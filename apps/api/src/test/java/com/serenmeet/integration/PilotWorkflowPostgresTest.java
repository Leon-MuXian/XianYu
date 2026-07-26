package com.serenmeet.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import java.util.Base64;
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
import org.springframework.http.MediaType;
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
    registry.add("seren-meet.staff-credential.active-key-id", () -> "integration");
    registry.add(
      "seren-meet.staff-credential.keys",
      () -> "integration=" + Base64.getEncoder().encodeToString(
        "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8)
      )
    );
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
  void storeProfileUsesBackendRegionsGlobalNamesAndFreeTextPhones() throws Exception {
    BusinessLoginResponse regionLogin = authService.ownerWechatLogin("owner-region-route");
    mockMvc.perform(get("/owner/regions/cities")
        .header("Authorization", "Bearer " + regionLogin.token()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.data.length()").value(341))
      .andExpect(jsonPath("$.data[?(@.code == '3100')].name").value(hasItem("上海市")));
    mockMvc.perform(get("/owner/regions/cities/3100/districts")
        .header("Authorization", "Bearer " + regionLogin.token()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.data[?(@.code == '310104')].name").value(hasItem("徐汇区")));

    assertThat(ownerService.administrativeCities())
      .hasSize(341)
      .anySatisfy(city -> assertThat(city).containsEntry("code", "3100").containsEntry("name", "上海市"));
    assertThat(ownerService.administrativeDistricts("3100"))
      .anySatisfy(district ->
        assertThat(district).containsEntry("code", "310104").containsEntry("name", "徐汇区")
      );
    assertThat(jdbcTemplate.queryForObject(
      "select count(*) from administrative_district", Integer.class
    )).isEqualTo(2978);
    assertThat(jdbcTemplate.queryForObject(
      "select count(*) from pg_constraint where conname = 'uq_store_name_key'", Integer.class
    )).isEqualTo(1);

    SessionPrincipal firstOwner = ownerPrincipal("owner-store-profile-first");
    TenantContext.set(firstOwner.tenantId());
    assertThat(ownerService.storeNameAvailability(firstOwner, "闲遇 规则店"))
      .containsEntry("available", true);
    saveCompleteStoreDraft(firstOwner, "闲遇 规则店", "客服微信：seren-meet");
    ownerService.completeOnboarding(firstOwner);
    assertThat(ownerService.store(firstOwner))
      .containsEntry("cityCode", "3100")
      .containsEntry("city", "上海市")
      .containsEntry("districtCode", "310104")
      .containsEntry("district", "徐汇区")
      .containsEntry("detailAddress", "衡山路 88 号 2 层")
      .containsEntry("address", "上海市徐汇区衡山路 88 号 2 层")
      .containsEntry("serviceScopes", List.of("瑜伽", "小班课"))
      .containsEntry("contactPhone", "客服微信：seren-meet");
    assertThat(ownerService.me(firstOwner)).containsEntry("city", "上海市");

    SessionPrincipal secondOwner = ownerPrincipal("owner-store-profile-second");
    TenantContext.set(secondOwner.tenantId());
    assertThat(ownerService.storeNameAvailability(secondOwner, "闲遇　规则店"))
      .containsEntry("available", false);
    assertThatThrownBy(() -> ownerService.saveStoreProfile(
      secondOwner,
      "闲遇　规则店",
      "3100",
      "310104",
      "衡山路 99 号",
      List.of("瑜伽", "小班课"),
      "客服微信：seren-meet"
    )).isInstanceOfSatisfying(ApiException.class, exception ->
      assertThat(exception.code()).isEqualTo("STORE_NAME_TAKEN")
    );
    assertThatThrownBy(() -> ownerService.saveStoreProfile(
      secondOwner,
      "闲遇第二门店",
      "3301",
      "310104",
      "测试路 2 号",
      List.of("瑜伽", "小班课"),
      "客服微信：seren-meet"
    )).isInstanceOfSatisfying(ApiException.class, exception ->
      assertThat(exception.code()).isEqualTo("REGION_INVALID")
    );
    assertThatThrownBy(() -> ownerService.saveStoreProfile(
      secondOwner,
      "闲遇第二门店",
      "3100",
      "310104",
      "衡山路 100 号",
      List.of("瑜伽", "小班课", "康复理疗", "咨询评估", "运动训练", "护理", "体适能"),
      "客服微信：seren-meet"
    )).isInstanceOfSatisfying(ApiException.class, exception ->
      assertThat(exception.code()).isEqualTo("FIELD_ERROR")
    );

    saveCompleteStoreDraft(secondOwner, "闲遇第二门店", "客服微信：seren-meet");
    ownerService.completeOnboarding(secondOwner);
    assertThat(ownerService.store(secondOwner))
      .containsEntry("contactPhone", "客服微信：seren-meet");
  }

  @Test
  void fullPilotFlowUsesHashedSessionsFactsAndAtomicCapacity() throws Exception {
    BusinessLoginResponse ownerLogin = authService.ownerWechatLogin("owner-integration");
    SessionPrincipal owner = authService.requirePrincipal("Bearer " + ownerLogin.token());
    TenantContext.set(owner.tenantId());

    assertThat(jdbcTemplate.queryForObject(
      "select count(*) from information_schema.columns where table_name = 'store' and column_name = 'service_tags'",
      Integer.class
    )).isZero();

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

    ownerService.saveStoreProfile(
      owner,
      "闲遇集成测试店",
      "3301",
      "330106",
      "测试路 1 号",
      List.of("瑜伽", "小班课"),
      "0571-00000000"
    );
    ownerService.saveBusinessHours(owner, Map.of(
      "days", Map.of("monday", Map.of("open", true, "start", "09:00", "end", "21:00"))
    ));
    mockMvc.perform(put("/owner/onboarding/resources")
        .header("Authorization", "Bearer " + ownerLogin.token())
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {"resources":[{"name":"兼容教室","resourceType":"房间","capacity":1}]}
          """))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.data.resources[0].enabled").value(true))
      .andExpect(jsonPath("$.data.resources[0].sortOrder").value(1));
    TenantContext.set(owner.tenantId());
    ownerService.saveResourcesDraft(owner, List.of(Map.of(
      "name", "一号教室",
      "resourceType", " 静语舱 ",
      "capacity", 2,
      "enabled", true,
      "sortOrder", 4
    )));
    assertThat(ownerService.onboardingDraft(owner).get("completion"))
      .isInstanceOfSatisfying(Map.class, completion ->
        assertThat(completion).containsEntry("storeProfileDone", true)
      );
    ownerService.completeOnboarding(owner);
    assertThat(ownerService.me(owner).get("name")).isEqualTo("闲遇集成测试店");

    Map<String, Object> primaryResource = ownerService.resources(owner).getFirst();
    assertThat(primaryResource)
      .containsEntry("resourceType", "静语舱")
      .containsEntry("enabled", true)
      .containsEntry("sortOrder", 4);
    Long resourceId = id(primaryResource);
    assertThat(ownerService.createResource(owner, " 二号教室 ", " 设备 ", 1, false, 8))
      .containsEntry("name", "二号教室")
      .containsEntry("resourceType", "设备")
      .containsEntry("enabled", false)
      .containsEntry("sortOrder", 8);
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
    var tenantDetail = tenantAdminService.getTenant(owner.tenantId());
    assertThat(tenantDetail.snapshot()).satisfies(snapshot -> {
      assertThat(snapshot.deductions()).isEqualTo(1);
      assertThat(snapshot.lastActivityAt()).isNotNull();
    });
    var mondayHours = tenantDetail.store().businessHours().days().get("monday");
    assertThat(mondayHours).isNotNull();
    assertThat(mondayHours.open()).isTrue();
    assertThat(mondayHours.start()).isEqualTo("09:00");
    assertThat(mondayHours.end()).isEqualTo("21:00");

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

  @Test
  void resourceDraftRejectsCustomPlaceholderAndAllDisabledResources() {
    BusinessLoginResponse ownerLogin = authService.ownerWechatLogin("owner-resource-validation");
    SessionPrincipal owner = authService.requirePrincipal("Bearer " + ownerLogin.token());
    TenantContext.set(owner.tenantId());

    assertThatThrownBy(() -> ownerService.saveResourcesDraft(owner, List.of(Map.of(
      "name", "咨询空间",
      "resourceType", "自定义",
      "capacity", 1,
      "enabled", true,
      "sortOrder", 1
    ))))
      .isInstanceOf(ApiException.class)
      .hasMessageContaining("具体的自定义资源类型");

    assertThatThrownBy(() -> ownerService.saveResourcesDraft(owner, List.of(Map.of(
      "name", "咨询舱",
      "resourceType", "咨询舱",
      "capacity", 1,
      "enabled", false,
      "sortOrder", 1
    ))))
      .isInstanceOf(ApiException.class)
      .hasMessageContaining("至少保留 1 个启用资源");
  }

  @Test
  void serviceNamesAreNormalizedUniquePerTenantAndConcurrencySafe() throws Exception {
    SessionPrincipal firstOwner = ownerPrincipal("owner-service-name-first");
    TenantContext.set(firstOwner.tenantId());
    saveCompleteStoreDraft(firstOwner, "服务名称唯一门店一", "021-11000001");
    ownerService.completeOnboarding(firstOwner);
    Long firstResourceId = id(ownerService.resources(firstOwner).getFirst());

    Map<String, Object> firstService = ownerService.createService(
      firstOwner,
      "  Ｐose　 Care  ",
      "一对一服务",
      60,
      1,
      1,
      List.of(firstResourceId),
      List.of(),
      "draft"
    );
    Long firstServiceId = id(firstService);
    assertThat(firstService).containsEntry("name", "Pose Care");
    assertThatThrownBy(() -> ownerService.createService(
      firstOwner,
      "pose care",
      "一对一服务",
      60,
      1,
      1,
      List.of(firstResourceId),
      List.of(),
      "draft"
    )).isInstanceOfSatisfying(ApiException.class, exception ->
      assertThat(exception.code()).isEqualTo("SERVICE_NAME_TAKEN")
    );

    Long secondServiceId = id(ownerService.createService(
      firstOwner,
      "替代服务",
      "一对一服务",
      45,
      1,
      1,
      List.of(firstResourceId),
      List.of(),
      "draft"
    ));
    assertThatThrownBy(() -> ownerService.updateService(
      firstOwner,
      secondServiceId,
      "ＰＯＳＥ　ＣＡＲＥ",
      "一对一服务",
      45,
      1,
      1,
      List.of(firstResourceId),
      List.of(),
      "draft"
    )).isInstanceOfSatisfying(ApiException.class, exception ->
      assertThat(exception.code()).isEqualTo("SERVICE_NAME_TAKEN")
    );
    assertThat(ownerService.updateService(
      firstOwner,
      firstServiceId,
      " POSE CARE ",
      "一对一服务",
      60,
      1,
      1,
      List.of(firstResourceId),
      List.of(),
      "draft"
    )).containsEntry("name", "POSE CARE");

    SessionPrincipal secondOwner = ownerPrincipal("owner-service-name-second");
    TenantContext.set(secondOwner.tenantId());
    saveCompleteStoreDraft(secondOwner, "服务名称唯一门店二", "021-11000002");
    ownerService.completeOnboarding(secondOwner);
    Long secondResourceId = id(ownerService.resources(secondOwner).getFirst());
    assertThat(ownerService.createService(
      secondOwner,
      "pose care",
      "一对一服务",
      60,
      1,
      1,
      List.of(secondResourceId),
      List.of(),
      "draft"
    )).containsEntry("name", "pose care");

    TenantContext.set(firstOwner.tenantId());
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    AtomicInteger successes = new AtomicInteger();
    AtomicInteger conflicts = new AtomicInteger();
    CompletableFuture<Void> firstAttempt = serviceCreationAttempt(
      firstOwner, firstResourceId, " concurrent service ", ready, start, successes, conflicts
    );
    CompletableFuture<Void> secondAttempt = serviceCreationAttempt(
      firstOwner, firstResourceId, "ＣＯＮＣＵＲＲＥＮＴ　ＳＥＲＶＩＣＥ",
      ready, start, successes, conflicts
    );
    assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
    start.countDown();
    firstAttempt.get(10, TimeUnit.SECONDS);
    secondAttempt.get(10, TimeUnit.SECONDS);

    assertThat(successes).hasValue(1);
    assertThat(conflicts).hasValue(1);
    assertThat(jdbcTemplate.queryForObject(
      "select count(*) from service_item where tenant_id = ? and name_key = 'concurrent service'",
      Integer.class,
      firstOwner.tenantId()
    )).isEqualTo(1);
    assertThat(jdbcTemplate.queryForObject(
      "select count(*) from pg_constraint where conname = 'uq_service_tenant_name_key'",
      Integer.class
    )).isEqualTo(1);
  }

  @Test
  void cardTemplateNamesAreNormalizedUniquePerTenantAndConcurrencySafe() throws Exception {
    SessionPrincipal firstOwner = ownerPrincipal("owner-card-name-first");
    TenantContext.set(firstOwner.tenantId());
    saveCompleteStoreDraft(firstOwner, "会员卡名称唯一门店一", "021-12000001");
    ownerService.completeOnboarding(firstOwner);
    Long firstResourceId = id(ownerService.resources(firstOwner).getFirst());
    Long firstStaffId = id(ownerService.createStaff(
      firstOwner, "card-name-staff-first", "staff-pass-2026", "卡务一", "教练"
    ));
    Long firstServiceId = id(ownerService.createService(
      firstOwner,
      "会员卡名称服务一",
      "一对一服务",
      60,
      1,
      1,
      List.of(firstResourceId),
      List.of(firstStaffId),
      "active"
    ));

    Map<String, Object> firstCard = ownerService.createCardTemplate(
      firstOwner,
      "  Ｐose　 Card  ",
      "count",
      new BigDecimal("600.00"),
      10,
      365,
      2,
      List.of(firstServiceId),
      List.of(firstStaffId)
    );
    Long firstCardId = id(firstCard);
    assertThat(firstCard).containsEntry("name", "Pose Card");
    assertThatThrownBy(() -> ownerService.createCardTemplate(
      firstOwner,
      "pose card",
      "period",
      new BigDecimal("800.00"),
      null,
      30,
      null,
      List.of(firstServiceId),
      List.of(firstStaffId)
    )).isInstanceOfSatisfying(ApiException.class, exception ->
      assertThat(exception.code()).isEqualTo("CARD_TEMPLATE_NAME_TAKEN")
    );

    Long secondCardId = id(ownerService.createCardTemplate(
      firstOwner,
      "替代会员卡",
      "period",
      new BigDecimal("900.00"),
      null,
      30,
      null,
      List.of(firstServiceId),
      List.of(firstStaffId)
    ));
    assertThatThrownBy(() -> ownerService.updateCardTemplate(
      firstOwner,
      secondCardId,
      "ＰＯＳＥ　ＣＡＲＤ",
      "period",
      new BigDecimal("900.00"),
      null,
      30,
      null,
      List.of(firstServiceId),
      List.of(firstStaffId)
    )).isInstanceOfSatisfying(ApiException.class, exception ->
      assertThat(exception.code()).isEqualTo("CARD_TEMPLATE_NAME_TAKEN")
    );
    assertThat(ownerService.updateCardTemplate(
      firstOwner,
      firstCardId,
      " POSE CARD ",
      "count",
      new BigDecimal("600.00"),
      10,
      365,
      2,
      List.of(firstServiceId),
      List.of(firstStaffId)
    )).containsEntry("name", "POSE CARD");

    SessionPrincipal secondOwner = ownerPrincipal("owner-card-name-second");
    TenantContext.set(secondOwner.tenantId());
    saveCompleteStoreDraft(secondOwner, "会员卡名称唯一门店二", "021-12000002");
    ownerService.completeOnboarding(secondOwner);
    Long secondResourceId = id(ownerService.resources(secondOwner).getFirst());
    Long secondStaffId = id(ownerService.createStaff(
      secondOwner, "card-name-staff-second", "staff-pass-2026", "卡务二", "教练"
    ));
    Long secondServiceId = id(ownerService.createService(
      secondOwner,
      "会员卡名称服务二",
      "一对一服务",
      60,
      1,
      1,
      List.of(secondResourceId),
      List.of(secondStaffId),
      "active"
    ));
    assertThat(ownerService.createCardTemplate(
      secondOwner,
      "pose card",
      "count",
      new BigDecimal("500.00"),
      8,
      180,
      1,
      List.of(secondServiceId),
      List.of(secondStaffId)
    )).containsEntry("name", "pose card");

    TenantContext.set(firstOwner.tenantId());
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    AtomicInteger successes = new AtomicInteger();
    AtomicInteger conflicts = new AtomicInteger();
    CompletableFuture<Void> firstAttempt = cardTemplateCreationAttempt(
      firstOwner, firstServiceId, firstStaffId, " concurrent card ", ready, start, successes, conflicts
    );
    CompletableFuture<Void> secondAttempt = cardTemplateCreationAttempt(
      firstOwner, firstServiceId, firstStaffId, "ＣＯＮＣＵＲＲＥＮＴ　ＣＡＲＤ",
      ready, start, successes, conflicts
    );
    assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
    start.countDown();
    firstAttempt.get(10, TimeUnit.SECONDS);
    secondAttempt.get(10, TimeUnit.SECONDS);

    assertThat(successes).hasValue(1);
    assertThat(conflicts).hasValue(1);
    assertThat(jdbcTemplate.queryForObject(
      "select count(*) from card_template where tenant_id = ? and name_key = 'concurrent card'",
      Integer.class,
      firstOwner.tenantId()
    )).isEqualTo(1);
    assertThat(jdbcTemplate.queryForObject(
      "select count(*) from pg_constraint where conname = 'uq_card_template_tenant_name_key'",
      Integer.class
    )).isEqualTo(1);
  }

  @Test
  void serviceDeletionRequiresDisabledUnreferencedOwnedService() throws Exception {
    BusinessLoginResponse ownerLogin =
      authService.ownerWechatLogin("owner-service-delete-primary");
    SessionPrincipal owner = authService.requirePrincipal("Bearer " + ownerLogin.token());
    TenantContext.set(owner.tenantId());
    saveCompleteStoreDraft(owner, "服务删除校验门店", "021-11000003");
    ownerService.completeOnboarding(owner);
    Long resourceId = id(ownerService.resources(owner).getFirst());
    Long staffId = id(ownerService.createStaff(
      owner, "service-delete-staff", "service-delete-pass", "删除校验员工", "康复师"
    ));

    Long deletableServiceId = id(ownerService.createService(
      owner,
      "可安全删除服务",
      "一对一服务",
      60,
      1,
      1,
      List.of(resourceId),
      List.of(staffId),
      "active"
    ));
    assertThatThrownBy(() -> ownerService.deleteService(owner, deletableServiceId))
      .isInstanceOfSatisfying(ApiException.class, exception ->
        assertThat(exception.code()).isEqualTo("SERVICE_MUST_BE_DISABLED")
      );

    Long cardBoundServiceId = id(ownerService.createService(
      owner,
      "会员卡关联服务",
      "一对一服务",
      45,
      1,
      1,
      List.of(resourceId),
      List.of(staffId),
      "active"
    ));
    ownerService.createCardTemplate(
      owner,
      "删除校验次数卡",
      "count",
      new BigDecimal("300.00"),
      10,
      365,
      2,
      List.of(cardBoundServiceId),
      List.of(staffId)
    );
    ownerService.updateServiceStatus(owner, cardBoundServiceId, "disabled");
    assertThatThrownBy(() -> ownerService.deleteService(owner, cardBoundServiceId))
      .isInstanceOfSatisfying(ApiException.class, exception ->
        assertThat(exception.code()).isEqualTo("SERVICE_IN_USE")
      );

    Long scheduledServiceId = id(ownerService.createService(
      owner,
      "排期关联服务",
      "一对一服务",
      50,
      1,
      1,
      List.of(resourceId),
      List.of(staffId),
      "active"
    ));
    OffsetDateTime startAt = OffsetDateTime.now().plusDays(5);
    ownerService.publishSlot(
      owner,
      scheduledServiceId,
      staffId,
      resourceId,
      startAt,
      startAt.plusMinutes(50),
      1
    );
    ownerService.updateServiceStatus(owner, scheduledServiceId, "disabled");
    assertThatThrownBy(() -> ownerService.deleteService(owner, scheduledServiceId))
      .isInstanceOfSatisfying(ApiException.class, exception ->
        assertThat(exception.code()).isEqualTo("SERVICE_IN_USE")
      );

    SessionPrincipal otherOwner = ownerPrincipal("owner-service-delete-other");
    TenantContext.set(otherOwner.tenantId());
    assertThatThrownBy(() -> ownerService.deleteService(otherOwner, deletableServiceId))
      .isInstanceOfSatisfying(ApiException.class, exception ->
        assertThat(exception.code()).isEqualTo("NOT_FOUND")
      );

    TenantContext.set(owner.tenantId());
    ownerService.updateServiceStatus(owner, deletableServiceId, "disabled");
    mockMvc.perform(delete("/owner/services/{serviceId}", deletableServiceId)
        .header("Authorization", "Bearer " + ownerLogin.token())
        .header("Idempotency-Key", "service-delete-retry"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.data.id").value(deletableServiceId))
      .andExpect(jsonPath("$.data.deleted").value(true));
    mockMvc.perform(delete("/owner/services/{serviceId}", deletableServiceId)
        .header("Authorization", "Bearer " + ownerLogin.token())
        .header("Idempotency-Key", "service-delete-retry"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.data.deleted").value(true));

    TenantContext.set(owner.tenantId());
    assertThat(jdbcTemplate.queryForObject(
      "select count(*) from service_item where tenant_id = ? and id = ?",
      Integer.class,
      owner.tenantId(),
      deletableServiceId
    )).isZero();
    assertThat(jdbcTemplate.queryForObject(
      "select count(*) from service_resource_binding where tenant_id = ? and service_item_id = ?",
      Integer.class,
      owner.tenantId(),
      deletableServiceId
    )).isZero();
    assertThat(jdbcTemplate.queryForObject(
      "select count(*) from staff_service_binding where tenant_id = ? and service_item_id = ?",
      Integer.class,
      owner.tenantId(),
      deletableServiceId
    )).isZero();
    assertThat(jdbcTemplate.queryForObject(
      "select count(*) from audit_log where tenant_id = ? and action = 'delete_service'",
      Integer.class,
      owner.tenantId()
    )).isEqualTo(1);
    assertThat(ownerService.createService(
      owner,
      "可安全删除服务",
      "一对一服务",
      60,
      1,
      1,
      List.of(resourceId),
      List.of(staffId),
      "draft"
    )).containsEntry("name", "可安全删除服务");
  }

  @Test
  void staffLoginNamesAreNormalizedGloballyUniqueAndConcurrencySafe() throws Exception {
    SessionPrincipal firstOwner = ownerPrincipal("owner-staff-login-first");
    TenantContext.set(firstOwner.tenantId());
    saveCompleteStoreDraft(firstOwner, "员工账号唯一门店一", "021-10000001");
    ownerService.completeOnboarding(firstOwner);

    Map<String, Object> firstStaff =
      ownerService.createStaff(
        firstOwner, "　ＴＥＡＭ－Ａ　", "staff-pass-2026", "员工甲", "教练");
    Long firstStaffId = id(firstStaff);
    assertThat(firstStaff).containsEntry("loginName", "team-a");

    BusinessLoginResponse staffLogin =
      authService.staffLogin(new StaffLoginRequest(" ＴＥＡＭ－Ａ ", "staff-pass-2026"));
    assertThat(authService.requirePrincipal("Bearer " + staffLogin.token()).tenantId())
      .isEqualTo(firstOwner.tenantId());

    assertThat(ownerService.updateStaff(
      firstOwner, firstStaffId, " TEAM-A ", "员工甲", "康复师"
    )).containsEntry("loginName", "team-a");

    assertThatThrownBy(() -> ownerService.createStaff(
      firstOwner, " team-a ", "staff-pass-2026", "同租户员工", "教练"
    )).isInstanceOfSatisfying(ApiException.class, exception ->
      assertThat(exception.code()).isEqualTo("STAFF_LOGIN_NAME_TAKEN")
    );

    Long secondStaffId = id(ownerService.createStaff(
      firstOwner, "team-b", "staff-pass-2026", "员工乙", "教练"
    ));
    assertThatThrownBy(() -> ownerService.updateStaff(
      firstOwner, secondStaffId, "ＴＥＡＭ－Ａ", "员工乙", "教练"
    )).isInstanceOfSatisfying(ApiException.class, exception ->
      assertThat(exception.code()).isEqualTo("STAFF_LOGIN_NAME_TAKEN")
    );

    SessionPrincipal secondOwner = ownerPrincipal("owner-staff-login-second");
    TenantContext.set(secondOwner.tenantId());
    saveCompleteStoreDraft(secondOwner, "员工账号唯一门店二", "021-10000002");
    ownerService.completeOnboarding(secondOwner);
    assertThatThrownBy(() -> ownerService.createStaff(
      secondOwner, "Team-A", "staff-pass-2026", "跨租户员工", "教练"
    )).isInstanceOfSatisfying(ApiException.class, exception ->
      assertThat(exception.code()).isEqualTo("STAFF_LOGIN_NAME_TAKEN")
    );

    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    AtomicInteger successes = new AtomicInteger();
    AtomicInteger conflicts = new AtomicInteger();
    CompletableFuture<Void> firstAttempt = staffCreationAttempt(
      firstOwner, " concurrent-staff ", "并发员工一", ready, start, successes, conflicts
    );
    CompletableFuture<Void> secondAttempt = staffCreationAttempt(
      secondOwner, "ＣＯＮＣＵＲＲＥＮＴ－ＳＴＡＦＦ", "并发员工二",
      ready, start, successes, conflicts
    );
    assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
    start.countDown();
    firstAttempt.get(10, TimeUnit.SECONDS);
    secondAttempt.get(10, TimeUnit.SECONDS);

    assertThat(successes).hasValue(1);
    assertThat(conflicts).hasValue(1);
    assertThat(jdbcTemplate.queryForObject(
      "select count(*) from staff_account where login_name = 'concurrent-staff'", Integer.class
    )).isEqualTo(1);
    assertThat(jdbcTemplate.queryForObject(
      "select count(*) from pg_constraint where conname = 'uq_staff_login'", Integer.class
    )).isEqualTo(1);
  }

  @Test
  void staffPasswordResetAndDisablePermanentlyRevokeExistingSessions() {
    SessionPrincipal owner = ownerPrincipal("owner-staff-session-revocation");
    TenantContext.set(owner.tenantId());
    saveCompleteStoreDraft(owner, "员工会话失效门店", "021-10000003");
    ownerService.completeOnboarding(owner);

    Long staffId = id(ownerService.createStaff(
      owner, "session-staff", "old-staff-pass-2026", "会话员工", "教练"
    ));
    Long unaffectedStaffId = id(ownerService.createStaff(
      owner, "unaffected-session-staff", "other-staff-pass-2026", "其他员工", "教练"
    ));
    BusinessLoginResponse oldLogin =
      authService.staffLogin(new StaffLoginRequest("session-staff", "old-staff-pass-2026"));
    BusinessLoginResponse unaffectedLogin = authService.staffLogin(
      new StaffLoginRequest("unaffected-session-staff", "other-staff-pass-2026")
    );
    assertThat(authService.requirePrincipal("Bearer " + oldLogin.token()).subjectId())
      .isEqualTo(staffId.toString());

    ownerService.updateStaffPassword(owner, staffId, "new-staff-pass-2026");

    assertThat(jdbcTemplate.queryForObject(
      "select revoked_at is not null from auth_session where token_hash = ?",
      Boolean.class,
      tokenHasher.hash(oldLogin.token())
    )).isTrue();
    assertThatThrownBy(() -> authService.requirePrincipal("Bearer " + oldLogin.token()))
      .isInstanceOfSatisfying(ApiException.class, exception ->
        assertThat(exception.code()).isEqualTo("UNAUTHORIZED")
      );
    assertThatThrownBy(() -> authService.staffLogin(
      new StaffLoginRequest("session-staff", "old-staff-pass-2026")
    )).isInstanceOfSatisfying(ApiException.class, exception ->
      assertThat(exception.code()).isEqualTo("INVALID_CREDENTIALS")
    );
    assertThat(authService.requirePrincipal("Bearer " + unaffectedLogin.token()).subjectId())
      .isEqualTo(unaffectedStaffId.toString());

    BusinessLoginResponse resetLogin =
      authService.staffLogin(new StaffLoginRequest("session-staff", "new-staff-pass-2026"));
    assertThat(authService.requirePrincipal("Bearer " + resetLogin.token()).firstLogin()).isTrue();

    ownerService.updateStaffStatus(owner, staffId, "disabled");

    assertThatThrownBy(() -> authService.requirePrincipal("Bearer " + resetLogin.token()))
      .isInstanceOfSatisfying(ApiException.class, exception ->
        assertThat(exception.code()).isEqualTo("UNAUTHORIZED")
      );
    assertThatThrownBy(() -> authService.staffLogin(
      new StaffLoginRequest("session-staff", "new-staff-pass-2026")
    )).isInstanceOfSatisfying(ApiException.class, exception ->
      assertThat(exception.code()).isEqualTo("STAFF_DISABLED")
    );

    ownerService.updateStaffStatus(owner, staffId, "active");

    assertThatThrownBy(() -> authService.requirePrincipal("Bearer " + resetLogin.token()))
      .isInstanceOfSatisfying(ApiException.class, exception ->
        assertThat(exception.code()).isEqualTo("UNAUTHORIZED")
      );
    BusinessLoginResponse reenabledLogin =
      authService.staffLogin(new StaffLoginRequest("session-staff", "new-staff-pass-2026"));
    assertThat(authService.requirePrincipal("Bearer " + reenabledLogin.token()).subjectId())
      .isEqualTo(staffId.toString());
    assertThat(authService.requirePrincipal("Bearer " + unaffectedLogin.token()).subjectId())
      .isEqualTo(unaffectedStaffId.toString());
  }

  @Test
  void ownerRevealsEncryptedStaffCredentialWithoutCrossTenantOrCacheLeak() throws Exception {
    BusinessLoginResponse ownerLogin =
      authService.ownerWechatLogin("owner-staff-credential-primary");
    SessionPrincipal owner = authService.requirePrincipal("Bearer " + ownerLogin.token());
    TenantContext.set(owner.tenantId());
    saveCompleteStoreDraft(owner, "员工凭据保管门店", "021-10000004");
    ownerService.completeOnboarding(owner);

    String initialPassword = "initial-staff-pass-2026";
    Long staffId = id(ownerService.createStaff(
      owner, "credential-staff", initialPassword, "凭据员工", "教练"
    ));
    String idempotentPassword = "idempotent-staff-pass-2026";
    mockMvc.perform(post("/owner/staff")
        .header("Authorization", "Bearer " + ownerLogin.token())
        .header("Idempotency-Key", "staff-credential-create")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "loginName":"idempotent-credential-staff",
            "password":"idempotent-staff-pass-2026",
            "staffName":"幂等凭据员工",
            "roleLabel":"教练"
          }
          """))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.data.loginPassword").doesNotExist())
      .andExpect(jsonPath("$.data.password").doesNotExist());
    TenantContext.set(owner.tenantId());
    assertThat(jdbcTemplate.queryForObject(
      """
      select response_body::text from idempotency_request
      where actor_type = 'owner' and actor_id = ? and operation = 'owner.staff.create'
        and idempotency_key = 'staff-credential-create'
      """,
      String.class,
      owner.subjectId()
    )).doesNotContain(idempotentPassword);
    Map<String, Object> listedStaff = ownerService.staff(owner).getFirst();
    assertThat(listedStaff)
      .containsEntry("credentialAvailable", true)
      .doesNotContainKeys("loginPassword", "password", "ciphertext", "nonce");

    Map<String, Object> storedSecret = jdbcTemplate.queryForMap(
      """
      select account.password_hash as password_hash, secret.key_id as key_id,
        encode(secret.nonce, 'base64') as nonce, encode(secret.ciphertext, 'base64') as ciphertext
      from staff_account account
      join staff_credential_secret secret on secret.staff_account_id = account.id
      where account.tenant_id = ? and account.id = ?
      """,
      owner.tenantId(),
      staffId
    );
    assertThat(storedSecret.get("password_hash").toString()).doesNotContain(initialPassword);
    assertThat(storedSecret.get("ciphertext").toString()).doesNotContain(initialPassword);
    assertThat(storedSecret.get("nonce").toString()).isNotBlank();
    assertThat(storedSecret.get("key_id")).isEqualTo("integration");

    mockMvc.perform(post("/owner/staff/{staffId}/credential/reveal", staffId)
        .header("Authorization", "Bearer " + ownerLogin.token()))
      .andExpect(status().isOk())
      .andExpect(header().string("Cache-Control", "no-store, no-cache, must-revalidate"))
      .andExpect(header().string("Pragma", "no-cache"))
      .andExpect(jsonPath("$.data.staffId").value(staffId))
      .andExpect(jsonPath("$.data.staffName").value("凭据员工"))
      .andExpect(jsonPath("$.data.loginName").value("credential-staff"))
      .andExpect(jsonPath("$.data.loginPassword").value(initialPassword));
    TenantContext.set(owner.tenantId());
    assertThat(jdbcTemplate.queryForObject(
      """
      select count(*) from audit_log
      where tenant_id = ? and action = 'reveal_staff_credential'
        and concat_ws('|', target_name, old_value, new_value, reason) like ?
      """,
      Integer.class,
      owner.tenantId(),
      "%" + initialPassword + "%"
    )).isZero();

    String resetPassword = "reset-staff-pass-2026";
    mockMvc.perform(put("/owner/staff/{staffId}/password", staffId)
        .header("Authorization", "Bearer " + ownerLogin.token())
        .header("Idempotency-Key", "staff-credential-reset")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"password\":\"reset-staff-pass-2026\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.data.passwordReset").value(true))
      .andExpect(jsonPath("$.data.loginPassword").doesNotExist())
      .andExpect(jsonPath("$.data.password").doesNotExist());
    TenantContext.set(owner.tenantId());
    assertThat(jdbcTemplate.queryForObject(
      """
      select response_body::text from idempotency_request
      where actor_type = 'owner' and actor_id = ? and operation = ?
        and idempotency_key = 'staff-credential-reset'
      """,
      String.class,
      owner.subjectId(),
      "owner.staff.password." + staffId
    )).doesNotContain(resetPassword);
    assertThat(ownerService.revealStaffCredential(owner, staffId).loginPassword())
      .isEqualTo(resetPassword);
    assertThat(jdbcTemplate.queryForObject(
      "select encode(ciphertext, 'base64') from staff_credential_secret where staff_account_id = ?",
      String.class,
      staffId
    )).isNotEqualTo(storedSecret.get("ciphertext"));

    BusinessLoginResponse otherOwnerLogin =
      authService.ownerWechatLogin("owner-staff-credential-other");
    SessionPrincipal otherOwner =
      authService.requirePrincipal("Bearer " + otherOwnerLogin.token());
    TenantContext.set(otherOwner.tenantId());
    saveCompleteStoreDraft(otherOwner, "员工凭据隔离门店", "021-10000005");
    ownerService.completeOnboarding(otherOwner);
    mockMvc.perform(post("/owner/staff/{staffId}/credential/reveal", staffId)
        .header("Authorization", "Bearer " + otherOwnerLogin.token()))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));

    TenantContext.set(owner.tenantId());
    Long legacyStaffId = id(ownerService.createStaff(
      owner, "legacy-credential-staff", "legacy-staff-pass-2026", "历史员工", "教练"
    ));
    jdbcTemplate.update(
      "delete from staff_credential_secret where tenant_id = ? and staff_account_id = ?",
      owner.tenantId(),
      legacyStaffId
    );
    assertThat(ownerService.staff(owner))
      .filteredOn(item -> legacyStaffId.equals(id(item)))
      .singleElement()
      .satisfies(item -> assertThat(item).containsEntry("credentialAvailable", false));
    assertThatThrownBy(() -> ownerService.revealStaffCredential(owner, legacyStaffId))
      .isInstanceOfSatisfying(ApiException.class, exception ->
        assertThat(exception.code()).isEqualTo("STAFF_CREDENTIAL_UNAVAILABLE")
      );
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

  private SessionPrincipal ownerPrincipal(String loginCode) {
    BusinessLoginResponse login = authService.ownerWechatLogin(loginCode);
    return authService.requirePrincipal("Bearer " + login.token());
  }

  private void saveCompleteStoreDraft(
    SessionPrincipal owner,
    String name,
    String contactPhone
  ) {
    ownerService.saveStoreProfile(
      owner,
      name,
      "3100",
      "310104",
      "衡山路 88 号 2 层",
      List.of("瑜伽", "小班课"),
      contactPhone
    );
    ownerService.saveBusinessHours(owner, Map.of(
      "days", Map.of("monday", Map.of("open", true, "start", "09:00", "end", "21:00"))
    ));
    ownerService.saveResourcesDraft(owner, List.of(Map.of(
      "name", "一号教室", "resourceType", "房间", "capacity", 2
    )));
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

  private CompletableFuture<Void> staffCreationAttempt(
    SessionPrincipal owner,
    String loginName,
    String staffName,
    CountDownLatch ready,
    CountDownLatch start,
    AtomicInteger successes,
    AtomicInteger conflicts
  ) {
    return CompletableFuture.runAsync(() -> {
      TenantContext.set(owner.tenantId());
      ready.countDown();
      try {
        start.await(5, TimeUnit.SECONDS);
        ownerService.createStaff(owner, loginName, "staff-pass-2026", staffName, "教练");
        successes.incrementAndGet();
      } catch (ApiException exception) {
        if (!"STAFF_LOGIN_NAME_TAKEN".equals(exception.code())) {
          throw exception;
        }
        conflicts.incrementAndGet();
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(exception);
      } finally {
        TenantContext.clear();
      }
    });
  }

  private CompletableFuture<Void> serviceCreationAttempt(
    SessionPrincipal owner,
    Long resourceId,
    String serviceName,
    CountDownLatch ready,
    CountDownLatch start,
    AtomicInteger successes,
    AtomicInteger conflicts
  ) {
    return CompletableFuture.runAsync(() -> {
      TenantContext.set(owner.tenantId());
      ready.countDown();
      try {
        start.await(5, TimeUnit.SECONDS);
        ownerService.createService(
          owner,
          serviceName,
          "一对一服务",
          60,
          1,
          1,
          List.of(resourceId),
          List.of(),
          "draft"
        );
        successes.incrementAndGet();
      } catch (ApiException exception) {
        if (!"SERVICE_NAME_TAKEN".equals(exception.code())) {
          throw exception;
        }
        conflicts.incrementAndGet();
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(exception);
      } finally {
        TenantContext.clear();
      }
    });
  }

  private CompletableFuture<Void> cardTemplateCreationAttempt(
    SessionPrincipal owner,
    Long serviceId,
    Long staffId,
    String cardName,
    CountDownLatch ready,
    CountDownLatch start,
    AtomicInteger successes,
    AtomicInteger conflicts
  ) {
    return CompletableFuture.runAsync(() -> {
      TenantContext.set(owner.tenantId());
      ready.countDown();
      try {
        start.await(5, TimeUnit.SECONDS);
        ownerService.createCardTemplate(
          owner,
          cardName,
          "count",
          new BigDecimal("700.00"),
          10,
          365,
          2,
          List.of(serviceId),
          List.of(staffId)
        );
        successes.incrementAndGet();
      } catch (ApiException exception) {
        if (!"CARD_TEMPLATE_NAME_TAKEN".equals(exception.code())) {
          throw exception;
        }
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
