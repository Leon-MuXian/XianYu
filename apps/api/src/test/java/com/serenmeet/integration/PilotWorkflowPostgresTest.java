package com.serenmeet.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.serenmeet.admin.application.PlatformConfigApplicationService;
import com.serenmeet.admin.application.SupportWechatApplicationService;
import com.serenmeet.audit.application.AuditApplicationService;
import com.serenmeet.audit.domain.AuditAction;
import com.serenmeet.auth.application.BusinessAuthApplicationService;
import com.serenmeet.auth.dto.BusinessLoginResponse;
import com.serenmeet.auth.dto.StaffLoginRequest;
import com.serenmeet.auth.support.SessionPrincipal;
import com.serenmeet.auth.support.TenantContext;
import com.serenmeet.auth.support.TokenHasher;
import com.serenmeet.common.ApiException;
import com.serenmeet.common.IdempotencyWorkflow;
import com.serenmeet.member.application.MemberPilotApplicationService;
import com.serenmeet.owner.application.OwnerPilotApplicationService;
import com.serenmeet.staff.application.StaffPilotApplicationService;
import com.serenmeet.tenant.application.TenantAdminApplicationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureMockMvc
class PilotWorkflowPostgresTest {

  @Container
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired
  private BusinessAuthApplicationService authService;

  @Autowired
  private PlatformConfigApplicationService platformConfigService;

  @Autowired
  private SupportWechatApplicationService supportWechatService;

  @Autowired
  private AuditApplicationService auditService;

  @Autowired
  private OwnerPilotApplicationService ownerService;

  @Autowired
  private MemberPilotApplicationService memberService;

  @Autowired
  private StaffPilotApplicationService staffService;

  @Autowired
  private TenantAdminApplicationService tenantAdminService;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private TokenHasher tokenHasher;

  @Autowired
  private IdempotencyWorkflow idempotency;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @AfterEach
  void clearTenantContext() {
    TenantContext.clear();
  }

  @Test
  void runtimeOpenApiMatchesCommittedContract() throws Exception {
    String runtime = mockMvc.perform(get("/v3/api-docs"))
      .andExpect(status().isOk())
      .andReturn()
      .getResponse()
      .getContentAsString(StandardCharsets.UTF_8);
    Path workingDirectory = Path.of(System.getProperty("user.dir"));
    Path snapshot = workingDirectory.resolve("packages/shared-types/openapi.json");
    if (!Files.exists(snapshot)) {
      snapshot = workingDirectory.resolve("../../packages/shared-types/openapi.json").normalize();
    }
    assertThat(Files.exists(snapshot)).as("committed OpenAPI snapshot exists").isTrue();
    JsonNode runtimeContract = objectMapper.readTree(runtime);
    JsonNode committedContract = objectMapper.readTree(Files.readString(snapshot));
    ((ObjectNode) runtimeContract).remove("servers");
    ((ObjectNode) committedContract).remove("servers");
    assertThat(runtimeContract).isEqualTo(committedContract);
  }

  @Test
  void adminConfigurationAndAuditUsePostgresTimezoneTypes() {
    assertThat(supportWechatService.getDefaultSupportWechat().wechatId()).isEqualTo("SerenMeet-CS");
    assertThat(platformConfigService.listConfigs(null, false)).isNotEmpty();

    auditService.recordSystem(
      null,
      "postgres-timezone-test",
      AuditAction.UPDATE_CONFIG.code(),
      "timezone mapping",
      null,
      "verified",
      "integration test"
    );

    assertThat(auditService.list("postgres-timezone-test", AuditAction.UPDATE_CONFIG.code(), null, 1, 10).items())
      .singleElement()
      .satisfies(item -> {
        assertThat(item.createdAt()).isNotNull();
        assertThat(item.actorName()).isEqualTo("postgres-timezone-test");
      });
  }

  @Test
  void missingStaticResourceReturnsNotFoundEnvelope() throws Exception {
    mockMvc.perform(get("/favicon.ico"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.success").value(false))
      .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  void fullPilotFlowUsesHashedSessionsFactsAndAtomicCapacity() throws Exception {
    BusinessLoginResponse ownerLogin = authService.ownerWechatLogin("owner-integration");
    SessionPrincipal owner = authService.requirePrincipal("Bearer " + ownerLogin.token());
    TenantContext.set(owner.tenantId());

    assertThat(jdbcTemplate.queryForObject(
      "select count(*) from auth_session where token_hash = ?", Integer.class, ownerLogin.token()
    )).isZero();
    assertThat(jdbcTemplate.queryForObject(
      "select count(*) from auth_session where token_hash = ?", Integer.class, tokenHasher.hash(ownerLogin.token())
    )).isEqualTo(1);

    Map<String, Object> initialDraft = ownerService.onboardingDraft(owner);
    assertThat(initialDraft)
      .containsEntry("trialNoticeRequired", true)
      .containsEntry("trialDays", 30);
    assertThat(ownerService.acknowledgeTrialNotice(owner))
      .containsEntry("trialNoticeRequired", false);
    assertThat(ownerService.acknowledgeTrialNotice(owner))
      .containsEntry("trialNoticeRequired", false);
    assertThat(ownerService.onboardingDraft(owner))
      .containsEntry("trialNoticeRequired", false);

    ownerService.saveStoreProfile(owner, Map.of(
      "name", "闲遇集成测试店",
      "city", "杭州",
      "businessCategories", List.of("瑜伽"),
      "serviceTags", List.of("小班课"),
      "address", "测试路 1 号",
      "contactPhone", "0571-00000000"
    ));
    ownerService.saveBusinessHours(owner, Map.of(
      "days", Map.of("monday", Map.of("open", true, "start", "09:00", "end", "21:00"))
    ));
    ownerService.saveResourcesDraft(owner, List.of(Map.of(
      "name", "一号教室", "resourceType", "房间", "capacity", 2
    )));
    assertThat(ownerService.onboardingDraft(owner).get("completion"))
      .isInstanceOfSatisfying(Map.class, completion ->
        assertThat(completion).containsEntry("storeProfileDone", true)
      );
    ownerService.completeOnboarding(owner);
    assertThat(ownerService.me(owner).get("name")).isEqualTo("闲遇集成测试店");

    Long resourceId = id(ownerService.resources(owner).getFirst());
    Long staffId = id(ownerService.createStaff(owner, "pilot-staff", "staff-pass-2026", "小林", "教练"));
    Long serviceId = id(ownerService.createService(
      owner, "舒缓瑜伽", "瑜伽", 60, 1, 1, List.of(resourceId), List.of(staffId), "active"
    ));
    Long templateId = id(ownerService.createCardTemplate(
      owner, "五次体验卡", "count", new BigDecimal("499.00"), 5, 90, 1, List.of(serviceId), List.of(staffId)
    ));

    MemberLogin firstMember = createAndBindMember(owner, templateId, "M-001", "member-one");
    OffsetDateTime startAt = OffsetDateTime.now().plusDays(1).withMinute(0).withSecond(0).withNano(0);
    Long firstSlotId = id(ownerService.publishSlot(
      owner, serviceId, staffId, resourceId, startAt, startAt.plusHours(1), 1
    ));
    assertThat(ownerService.dashboard(owner).get("futureSlotCount")).isEqualTo(1);
    assertThat(ownerService.schedules(owner, startAt.toLocalDate())).hasSize(1);
    assertThat(ownerService.cardWarnings(owner)).hasSize(1);
    assertThat(memberService.home(firstMember.principal()).get("activeCardCount")).isEqualTo(1);
    assertThat(memberService.cards(firstMember.principal())).hasSize(1);
    assertThat(memberService.services(firstMember.principal())).hasSize(1);
    assertThat(memberService.availableCards(firstMember.principal(), firstSlotId)).hasSize(1);
    Long bookingId = id(memberService.createBooking(firstMember.principal(), firstSlotId, firstMember.cardId()));
    assertThat(memberService.bookings(firstMember.principal())).hasSize(1);

    BusinessLoginResponse staffLogin = authService.staffLogin(new StaffLoginRequest("pilot-staff", "staff-pass-2026"));
    SessionPrincipal staff = authService.requirePrincipal("Bearer " + staffLogin.token());
    assertThat(staffService.account(staff).get("staffName")).isEqualTo("小林");
    assertThat(staffService.profile(staff).get("displayName")).isEqualTo("小林");
    assertThat(staffService.roster(staff, firstSlotId)).hasSize(1);
    staffService.confirmAttendance(staff, bookingId);
    Map<String, Object> deduction = staffService.deduct(staff, bookingId);
    staffService.saveNote(staff, bookingId, "状态稳定", "体验良好");

    assertThat(deduction.get("beforeCount")).isEqualTo(5);
    assertThat(deduction.get("afterCount")).isEqualTo(4);
    assertThat(staffService.records(staff)).hasSize(1);
    assertThat(memberService.records(firstMember.principal())).hasSize(1);
    assertThat(jdbcTemplate.queryForObject(
      "select remain_count from member_card where id = ?", Integer.class, firstMember.cardId()
    )).isEqualTo(4);
    assertThat(ownerService.reportSummary(owner).get("deductions")).isEqualTo(1);
    assertThat(tenantAdminService.getTenant(owner.tenantId()).snapshot()).satisfies(snapshot -> {
      assertThat(snapshot.deductions()).isEqualTo(1);
      assertThat(snapshot.lastActivityAt()).isNotNull();
    });

    MemberLogin secondMember = createAndBindMember(owner, templateId, "M-002", "member-two");
    Long contestedSlotId = id(ownerService.publishSlot(
      owner, serviceId, staffId, resourceId, startAt.plusDays(1), startAt.plusDays(1).plusHours(1), 1
    ));
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    AtomicInteger successes = new AtomicInteger();
    AtomicInteger conflicts = new AtomicInteger();

    CompletableFuture<Void> firstAttempt = bookingAttempt(firstMember, contestedSlotId, ready, start, successes, conflicts);
    CompletableFuture<Void> secondAttempt = bookingAttempt(secondMember, contestedSlotId, ready, start, successes, conflicts);
    assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
    start.countDown();
    CompletableFuture.allOf(firstAttempt, secondAttempt).get(10, TimeUnit.SECONDS);

    assertThat(successes).hasValue(1);
    assertThat(conflicts).hasValue(1);
    assertThat(jdbcTemplate.queryForObject(
      "select reserved_count from bookable_slot where id = ?", Integer.class, contestedSlotId
    )).isEqualTo(1);
    assertThat(jdbcTemplate.queryForObject(
      "select count(*) from booking where bookable_slot_id = ?", Integer.class, contestedSlotId
    )).isEqualTo(1);

    verifyIdempotentIssuance(owner, templateId);
    verifyConcurrentDeduction(owner, staff, serviceId, staffId, resourceId, templateId, startAt.plusDays(2));

    BusinessLoginResponse secondOwnerLogin = authService.ownerWechatLogin("owner-isolation");
    SessionPrincipal secondOwner = authService.requirePrincipal("Bearer " + secondOwnerLogin.token());
    TenantContext.set(secondOwner.tenantId());
    assertThat(ownerService.resources(secondOwner)).isEmpty();
    TenantContext.set(owner.tenantId());
    assertThat(ownerService.resources(owner)).extracting(row -> id(row)).contains(resourceId);

    jdbcTemplate.update("update tenant set status = 'frozen', freeze_reason = 'integration gate' where id = ?", owner.tenantId());
    mockMvc.perform(get("/owner/me").header("Authorization", "Bearer " + ownerLogin.token()))
      .andExpect(status().isForbidden())
      .andExpect(jsonPath("$.success").value(false))
      .andExpect(jsonPath("$.error.code").value("TENANT_FROZEN"));
  }

  private void verifyIdempotentIssuance(SessionPrincipal owner, Long templateId) {
    Long memberId = id(ownerService.createMember(owner, "幂等会员", "M-IDEMPOTENT", "微信联系"));
    Map<String, Object> request = Map.of("memberId", memberId, "templateId", templateId, "paidAmount", "499.00");
    String key = "integration-issue-card";

    idempotency.execute(owner, "test.issue-card", key, request,
      () -> ownerService.issueCard(owner, memberId, templateId, new BigDecimal("499.00"), LocalDate.now(), "线下收款"));
    Object replay = idempotency.execute(owner, "test.issue-card", key, request,
      () -> { throw new AssertionError("replayed request must not execute the action"); });

    assertThat(replay).isNotNull();
    assertThat(jdbcTemplate.queryForObject(
      "select count(*) from offline_sale_record where tenant_id = ? and member_id = ?", Integer.class, owner.tenantId(), memberId
    )).isEqualTo(1);
    assertThatThrownBy(() -> idempotency.execute(
      owner,
      "test.issue-card",
      key,
      Map.of("memberId", memberId, "templateId", templateId, "paidAmount", "399.00"),
      () -> Map.of()
    )).isInstanceOfSatisfying(ApiException.class, exception ->
      assertThat(exception.code()).isEqualTo("IDEMPOTENCY_CONFLICT")
    );
  }

  private void verifyConcurrentDeduction(
    SessionPrincipal owner,
    SessionPrincipal staff,
    Long serviceId,
    Long staffId,
    Long resourceId,
    Long templateId,
    OffsetDateTime startAt
  ) throws Exception {
    MemberLogin member = createAndBindMember(owner, templateId, "M-DEDUCT", "member-deduction");
    Long slotId = id(ownerService.publishSlot(owner, serviceId, staffId, resourceId, startAt, startAt.plusHours(1), 1));
    Long bookingId = id(memberService.createBooking(member.principal(), slotId, member.cardId()));
    staffService.confirmAttendance(staff, bookingId);

    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    CompletableFuture<Map<String, Object>> first = deductionAttempt(staff, bookingId, ready, start);
    CompletableFuture<Map<String, Object>> second = deductionAttempt(staff, bookingId, ready, start);
    assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
    start.countDown();
    assertThat(first.get(10, TimeUnit.SECONDS).get("afterCount")).isEqualTo(4);
    assertThat(second.get(10, TimeUnit.SECONDS).get("afterCount")).isEqualTo(4);
    assertThat(jdbcTemplate.queryForObject(
      "select count(*) from deduction_record where booking_id = ?", Integer.class, bookingId
    )).isEqualTo(1);
    assertThat(jdbcTemplate.queryForObject(
      "select remain_count from member_card where id = ?", Integer.class, member.cardId()
    )).isEqualTo(4);
  }

  private CompletableFuture<Map<String, Object>> deductionAttempt(
    SessionPrincipal staff,
    Long bookingId,
    CountDownLatch ready,
    CountDownLatch start
  ) {
    return CompletableFuture.supplyAsync(() -> {
      TenantContext.set(staff.tenantId());
      ready.countDown();
      try {
        start.await(5, TimeUnit.SECONDS);
        return staffService.deduct(staff, bookingId);
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(exception);
      } finally {
        TenantContext.clear();
      }
    });
  }

  private MemberLogin createAndBindMember(SessionPrincipal owner, Long templateId, String memberNo, String loginCode) {
    Long memberId = id(ownerService.createMember(owner, "会员" + memberNo, memberNo, "微信联系"));
    Long cardId = ((Number) ownerService.issueCard(
      owner, memberId, templateId, new BigDecimal("499.00"), LocalDate.now(), "线下收款"
    ).get("memberCardId")).longValue();
    String inviteCode = ownerService.createInvite(owner, memberId).get("code").toString();
    BusinessLoginResponse login = authService.memberWechatLogin(loginCode);
    SessionPrincipal unbound = authService.requirePrincipal("Bearer " + login.token());
    assertThat(memberService.checkInvite(unbound, inviteCode).get("memberId")).isEqualTo(memberId);
    memberService.bindInvite(unbound, inviteCode);
    SessionPrincipal bound = authService.requirePrincipal("Bearer " + login.token());
    assertThat(bound.bound()).isTrue();
    return new MemberLogin(bound, cardId);
  }

  private CompletableFuture<Void> bookingAttempt(
    MemberLogin member,
    Long slotId,
    CountDownLatch ready,
    CountDownLatch start,
    AtomicInteger successes,
    AtomicInteger conflicts
  ) {
    return CompletableFuture.runAsync(() -> {
      TenantContext.set(member.principal().tenantId());
      ready.countDown();
      try {
        start.await(5, TimeUnit.SECONDS);
        memberService.createBooking(member.principal(), slotId, member.cardId());
        successes.incrementAndGet();
      } catch (ApiException exception) {
        conflicts.incrementAndGet();
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(exception);
      } finally {
        TenantContext.clear();
      }
    });
  }

  private Long id(Map<String, Object> value) {
    return ((Number) value.get("id")).longValue();
  }

  private record MemberLogin(SessionPrincipal principal, Long cardId) {
  }
}
