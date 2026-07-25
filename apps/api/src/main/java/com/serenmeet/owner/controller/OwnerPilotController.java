package com.serenmeet.owner.controller;

import com.serenmeet.auth.support.CurrentSession;
import com.serenmeet.auth.support.SessionPrincipal;
import com.serenmeet.common.ApiResponse;
import com.serenmeet.common.IdempotencyWorkflow;
import com.serenmeet.owner.application.OwnerPilotApplicationService;
import com.serenmeet.owner.dto.StaffCredentialResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/owner")
public class OwnerPilotController {

  private final OwnerPilotApplicationService ownerService;
  private final IdempotencyWorkflow idempotency;

  public OwnerPilotController(
      OwnerPilotApplicationService ownerService, IdempotencyWorkflow idempotency) {
    this.ownerService = ownerService;
    this.idempotency = idempotency;
  }

  @GetMapping("/me")
  public ApiResponse<Map<String, Object>> me(@CurrentSession SessionPrincipal principal) {
    return ApiResponse.ok(ownerService.me(principal));
  }

  @GetMapping("/frozen")
  public ApiResponse<Map<String, Object>> frozen(@CurrentSession SessionPrincipal principal) {
    return ApiResponse.ok(ownerService.me(principal));
  }

  @GetMapping("/dashboard")
  public ApiResponse<Map<String, Object>> dashboard(@CurrentSession SessionPrincipal principal) {
    return ApiResponse.ok(ownerService.dashboard(principal));
  }

  @GetMapping("/store")
  public ApiResponse<Map<String, Object>> store(@CurrentSession SessionPrincipal principal) {
    return ApiResponse.ok(ownerService.store(principal));
  }

  @GetMapping("/regions/cities")
  public ApiResponse<List<Map<String, Object>>> administrativeCities(
      @CurrentSession SessionPrincipal principal) {
    return ApiResponse.ok(ownerService.administrativeCities());
  }

  @GetMapping("/regions/cities/{cityCode}/districts")
  public ApiResponse<List<Map<String, Object>>> administrativeDistricts(
      @CurrentSession SessionPrincipal principal, @PathVariable String cityCode) {
    return ApiResponse.ok(ownerService.administrativeDistricts(cityCode));
  }

  @PostMapping("/store/name-availability")
  public ApiResponse<Map<String, Object>> storeNameAvailability(
      @CurrentSession SessionPrincipal principal,
      @Valid @RequestBody StoreNameAvailabilityRequest request) {
    return ApiResponse.ok(ownerService.storeNameAvailability(principal, request.name()));
  }

  @PutMapping("/store")
  public ApiResponse<Object> updateStore(
      @CurrentSession SessionPrincipal principal,
      @RequestHeader("Idempotency-Key") String key,
      @Valid @RequestBody UpdateStoreRequest request) {
    return ApiResponse.ok(
        idempotency.execute(
            principal,
            "owner.store.update",
            key,
            request,
            () ->
                request.days() == null
                    ? ownerService.updateStoreProfile(
                        principal,
                        request.name(),
                        request.cityCode(),
                        request.districtCode(),
                        request.detailAddress(),
                        request.serviceScopes(),
                        request.contactPhone())
                    : ownerService.updateStoreBusinessHours(
                        principal, Map.of("days", request.days()))));
  }

  @GetMapping("/onboarding/draft")
  public ApiResponse<Map<String, Object>> onboarding(@CurrentSession SessionPrincipal principal) {
    return ApiResponse.ok(ownerService.onboardingDraft(principal));
  }

  @PostMapping("/onboarding/trial-notice/acknowledge")
  public ApiResponse<Map<String, Object>> acknowledgeTrialNotice(
      @CurrentSession SessionPrincipal principal) {
    return ApiResponse.ok(ownerService.acknowledgeTrialNotice(principal));
  }

  @PutMapping("/onboarding/store-profile")
  public ApiResponse<Map<String, Object>> saveStoreProfile(
      @CurrentSession SessionPrincipal principal, @Valid @RequestBody StoreProfileRequest request) {
    return ApiResponse.ok(
        ownerService.saveStoreProfile(
            principal,
            request.name(),
            request.cityCode(),
            request.districtCode(),
            request.detailAddress(),
            request.serviceScopes(),
            request.contactPhone()));
  }

  @PutMapping("/onboarding/business-hours")
  public ApiResponse<Map<String, Object>> saveBusinessHours(
      @CurrentSession SessionPrincipal principal,
      @Valid @RequestBody BusinessHoursRequest request) {
    return ApiResponse.ok(ownerService.saveBusinessHours(principal, request));
  }

  @PutMapping("/onboarding/resources")
  public ApiResponse<Map<String, Object>> saveResources(
      @CurrentSession SessionPrincipal principal,
      @Valid @RequestBody ResourcesDraftRequest request) {
    return ApiResponse.ok(ownerService.saveResourcesDraft(principal, request.resources()));
  }

  @PostMapping("/onboarding/complete")
  public ApiResponse<Object> completeOnboarding(
      @CurrentSession SessionPrincipal principal, @RequestHeader("Idempotency-Key") String key) {
    return ApiResponse.ok(
        idempotency.execute(
            principal,
            "owner.onboarding.complete",
            key,
            Map.of(),
            () -> ownerService.completeOnboarding(principal)));
  }

  @GetMapping("/resources")
  public ApiResponse<List<Map<String, Object>>> resources(
      @CurrentSession SessionPrincipal principal) {
    return ApiResponse.ok(ownerService.resources(principal));
  }

  @PostMapping("/resources")
  public ApiResponse<Object> createResource(
      @CurrentSession SessionPrincipal principal,
      @RequestHeader("Idempotency-Key") String key,
      @Valid @RequestBody ResourceRequest request) {
    return ApiResponse.ok(
        idempotency.execute(
            principal,
            "owner.resource.create",
            key,
            request,
            () ->
                ownerService.createResource(
                    principal,
                    request.name(),
                    request.resourceType(),
                    request.capacity(),
                    request.enabled(),
                    request.sortOrder())));
  }

  @PutMapping("/resources/{resourceId}")
  public ApiResponse<Object> updateResource(
      @CurrentSession SessionPrincipal principal,
      @PathVariable Long resourceId,
      @RequestHeader("Idempotency-Key") String key,
      @Valid @RequestBody UpdateResourceRequest request) {
    return ApiResponse.ok(
        idempotency.execute(
            principal,
            "owner.resource.update." + resourceId,
            key,
            request,
            () ->
                ownerService.updateResource(
                    principal,
                    resourceId,
                    request.name(),
                    request.resourceType(),
                    request.capacity(),
                    request.enabled(),
                    request.sortOrder())));
  }

  @DeleteMapping("/resources/{resourceId}")
  public ApiResponse<Object> deleteResource(
      @CurrentSession SessionPrincipal principal,
      @PathVariable Long resourceId,
      @RequestHeader("Idempotency-Key") String key) {
    return ApiResponse.ok(
        idempotency.execute(
            principal,
            "owner.resource.delete." + resourceId,
            key,
            Map.of("resourceId", resourceId),
            () -> ownerService.deleteResource(principal, resourceId)));
  }

  @GetMapping("/staff")
  public ApiResponse<List<Map<String, Object>>> staff(@CurrentSession SessionPrincipal principal) {
    return ApiResponse.ok(ownerService.staff(principal));
  }

  @PostMapping("/staff/{staffId}/credential/reveal")
  public ResponseEntity<ApiResponse<StaffCredentialResponse>> revealStaffCredential(
      @CurrentSession SessionPrincipal principal, @PathVariable Long staffId) {
    return ResponseEntity.ok()
        .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate")
        .header(HttpHeaders.PRAGMA, "no-cache")
        .body(ApiResponse.ok(ownerService.revealStaffCredential(principal, staffId)));
  }

  @PostMapping("/staff")
  public ApiResponse<Object> createStaff(
      @CurrentSession SessionPrincipal principal,
      @RequestHeader("Idempotency-Key") String key,
      @Valid @RequestBody CreateStaffRequest request) {
    return ApiResponse.ok(
        idempotency.execute(
            principal,
            "owner.staff.create",
            key,
            request,
            () ->
                ownerService.createStaff(
                    principal,
                    request.loginName(),
                    request.password(),
                    request.staffName(),
                    request.roleLabel())));
  }

  @PutMapping("/staff/{staffId}")
  public ApiResponse<Object> updateStaff(
      @CurrentSession SessionPrincipal principal,
      @PathVariable Long staffId,
      @RequestHeader("Idempotency-Key") String key,
      @Valid @RequestBody UpdateStaffRequest request) {
    return ApiResponse.ok(
        idempotency.execute(
            principal,
            "owner.staff.update." + staffId,
            key,
            request,
            () ->
                ownerService.updateStaff(
                    principal,
                    staffId,
                    request.loginName(),
                    request.staffName(),
                    request.roleLabel())));
  }

  @PutMapping("/staff/{staffId}/password")
  public ApiResponse<Object> updateStaffPassword(
      @CurrentSession SessionPrincipal principal,
      @PathVariable Long staffId,
      @RequestHeader("Idempotency-Key") String key,
      @Valid @RequestBody UpdatePasswordRequest request) {
    return ApiResponse.ok(
        idempotency.execute(
            principal,
            "owner.staff.password." + staffId,
            key,
            request,
            () -> ownerService.updateStaffPassword(principal, staffId, request.password())));
  }

  @PutMapping("/staff/{staffId}/status")
  public ApiResponse<Object> updateStaffStatus(
      @CurrentSession SessionPrincipal principal,
      @PathVariable Long staffId,
      @RequestHeader("Idempotency-Key") String key,
      @Valid @RequestBody StatusRequest request) {
    return ApiResponse.ok(
        idempotency.execute(
            principal,
            "owner.staff.status." + staffId,
            key,
            request,
            () -> ownerService.updateStaffStatus(principal, staffId, request.status())));
  }

  @DeleteMapping("/staff/{staffId}")
  public ApiResponse<Object> deleteStaff(
      @CurrentSession SessionPrincipal principal,
      @PathVariable Long staffId,
      @RequestHeader("Idempotency-Key") String key) {
    return ApiResponse.ok(
        idempotency.execute(
            principal,
            "owner.staff.delete." + staffId,
            key,
            Map.of("staffId", staffId),
            () -> ownerService.deleteStaff(principal, staffId)));
  }

  @GetMapping("/services")
  public ApiResponse<List<Map<String, Object>>> services(
      @CurrentSession SessionPrincipal principal) {
    return ApiResponse.ok(ownerService.services(principal));
  }

  @PostMapping("/services")
  public ApiResponse<Object> createService(
      @CurrentSession SessionPrincipal principal,
      @RequestHeader("Idempotency-Key") String key,
      @Valid @RequestBody CreateServiceRequest request) {
    return ApiResponse.ok(
        idempotency.execute(
            principal,
            "owner.service.create",
            key,
            request,
            () ->
                ownerService.createService(
                    principal,
                    request.name(),
                    request.serviceType(),
                    request.durationMin(),
                    request.defaultCapacity(),
                    request.deductCount(),
                    request.resourceIds(),
                    request.staffIds(),
                    request.status())));
  }

  @PutMapping("/services/{serviceId}")
  public ApiResponse<Object> updateService(
      @CurrentSession SessionPrincipal principal,
      @PathVariable Long serviceId,
      @RequestHeader("Idempotency-Key") String key,
      @Valid @RequestBody CreateServiceRequest request) {
    return ApiResponse.ok(
        idempotency.execute(
            principal,
            "owner.service.update." + serviceId,
            key,
            request,
            () ->
                ownerService.updateService(
                    principal,
                    serviceId,
                    request.name(),
                    request.serviceType(),
                    request.durationMin(),
                    request.defaultCapacity(),
                    request.deductCount(),
                    request.resourceIds(),
                    request.staffIds(),
                    request.status())));
  }

  @PutMapping("/services/{serviceId}/status")
  public ApiResponse<Object> updateServiceStatus(
      @CurrentSession SessionPrincipal principal,
      @PathVariable Long serviceId,
      @RequestHeader("Idempotency-Key") String key,
      @Valid @RequestBody StatusRequest request) {
    return ApiResponse.ok(
        idempotency.execute(
            principal,
            "owner.service.status." + serviceId,
            key,
            request,
            () -> ownerService.updateServiceStatus(principal, serviceId, request.status())));
  }

  @GetMapping("/services/options")
  public ApiResponse<Map<String, Object>> serviceOptions(
      @CurrentSession SessionPrincipal principal) {
    return ApiResponse.ok(
        Map.of(
            "services", ownerService.services(principal),
            "resources", ownerService.resources(principal),
            "staff", ownerService.staff(principal)));
  }

  @GetMapping("/card-templates")
  public ApiResponse<List<Map<String, Object>>> cardTemplates(
      @CurrentSession SessionPrincipal principal) {
    return ApiResponse.ok(ownerService.cardTemplates(principal));
  }

  @PostMapping("/card-templates")
  public ApiResponse<Object> createCardTemplate(
      @CurrentSession SessionPrincipal principal,
      @RequestHeader("Idempotency-Key") String key,
      @Valid @RequestBody CreateCardTemplateRequest request) {
    return ApiResponse.ok(
        idempotency.execute(
            principal,
            "owner.card-template.create",
            key,
            request,
            () ->
                ownerService.createCardTemplate(
                    principal,
                    request.name(),
                    request.cardType(),
                    request.salePriceYuan(),
                    request.totalCount(),
                    request.validDays(),
                    request.lowBalanceThreshold(),
                    request.serviceIds(),
                    request.staffIds())));
  }

  @PutMapping("/card-templates/{templateId}")
  public ApiResponse<Object> updateCardTemplate(
      @CurrentSession SessionPrincipal principal,
      @PathVariable Long templateId,
      @RequestHeader("Idempotency-Key") String key,
      @Valid @RequestBody CreateCardTemplateRequest request) {
    return ApiResponse.ok(
        idempotency.execute(
            principal,
            "owner.card-template.update." + templateId,
            key,
            request,
            () ->
                ownerService.updateCardTemplate(
                    principal,
                    templateId,
                    request.name(),
                    request.cardType(),
                    request.salePriceYuan(),
                    request.totalCount(),
                    request.validDays(),
                    request.lowBalanceThreshold(),
                    request.serviceIds(),
                    request.staffIds())));
  }

  @PutMapping("/card-templates/{templateId}/status")
  public ApiResponse<Object> updateCardTemplateStatus(
      @CurrentSession SessionPrincipal principal,
      @PathVariable Long templateId,
      @RequestHeader("Idempotency-Key") String key,
      @Valid @RequestBody StatusRequest request) {
    return ApiResponse.ok(
        idempotency.execute(
            principal,
            "owner.card-template.status." + templateId,
            key,
            request,
            () -> ownerService.updateCardTemplateStatus(principal, templateId, request.status())));
  }

  @GetMapping("/members")
  public ApiResponse<List<Map<String, Object>>> members(
      @CurrentSession SessionPrincipal principal) {
    return ApiResponse.ok(ownerService.members(principal));
  }

  @PostMapping("/members")
  public ApiResponse<Object> createMember(
      @CurrentSession SessionPrincipal principal,
      @RequestHeader("Idempotency-Key") String key,
      @Valid @RequestBody CreateMemberRequest request) {
    return ApiResponse.ok(
        idempotency.execute(
            principal,
            "owner.member.create",
            key,
            request,
            () ->
                ownerService.createMember(
                    principal, request.name(), request.memberNo(), request.contactText())));
  }

  @PostMapping("/members/{memberId}/cards")
  public ApiResponse<Object> issueCard(
      @CurrentSession SessionPrincipal principal,
      @PathVariable Long memberId,
      @RequestHeader("Idempotency-Key") String key,
      @Valid @RequestBody IssueCardRequest request) {
    return ApiResponse.ok(
        idempotency.execute(
            principal,
            "owner.member.issue-card." + memberId,
            key,
            request,
            () ->
                ownerService.issueCard(
                    principal,
                    memberId,
                    request.templateId(),
                    request.receivedAmountYuan(),
                    request.saleDate(),
                    request.payMethodLabel())));
  }

  @PostMapping("/members/{memberId}/invite-codes")
  public ApiResponse<Object> createInvite(
      @CurrentSession SessionPrincipal principal,
      @RequestHeader("Idempotency-Key") String key,
      @PathVariable Long memberId) {
    return ApiResponse.ok(
        idempotency.execute(
            principal,
            "owner.member.invite." + memberId,
            key,
            Map.of("memberId", memberId),
            () -> ownerService.createInvite(principal, memberId)));
  }

  @GetMapping("/schedules")
  public ApiResponse<List<Map<String, Object>>> schedules(
      @CurrentSession SessionPrincipal principal,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return ApiResponse.ok(ownerService.schedules(principal, date));
  }

  @PostMapping("/schedules/publish")
  public ApiResponse<Object> publishSchedule(
      @CurrentSession SessionPrincipal principal,
      @RequestHeader("Idempotency-Key") String key,
      @Valid @RequestBody PublishSlotRequest request) {
    return ApiResponse.ok(
        idempotency.execute(
            principal,
            "owner.schedule.publish",
            key,
            request,
            () ->
                ownerService.saveSlot(
                    principal,
                    request.serviceId(),
                    request.staffId(),
                    request.resourceId(),
                    request.startAt(),
                    request.endAt(),
                    request.capacity(),
                    "published")));
  }

  @PostMapping("/schedules/drafts")
  public ApiResponse<Object> saveScheduleDraft(
      @CurrentSession SessionPrincipal principal,
      @RequestHeader("Idempotency-Key") String key,
      @Valid @RequestBody PublishSlotRequest request) {
    return ApiResponse.ok(
        idempotency.execute(
            principal,
            "owner.schedule.draft",
            key,
            request,
            () ->
                ownerService.saveSlot(
                    principal,
                    request.serviceId(),
                    request.staffId(),
                    request.resourceId(),
                    request.startAt(),
                    request.endAt(),
                    request.capacity(),
                    "draft")));
  }

  @GetMapping("/reports/summary")
  public ApiResponse<Map<String, Object>> reportSummary(
      @CurrentSession SessionPrincipal principal) {
    return ApiResponse.ok(ownerService.reportSummary(principal));
  }

  @GetMapping("/reports/offline-sales")
  public ApiResponse<List<Map<String, Object>>> offlineSales(
      @CurrentSession SessionPrincipal principal) {
    return ApiResponse.ok(ownerService.offlineSales(principal));
  }

  @GetMapping("/reports/bookings")
  public ApiResponse<List<Map<String, Object>>> bookingReport(
      @CurrentSession SessionPrincipal principal) {
    return ApiResponse.ok(ownerService.bookingReport(principal));
  }

  @GetMapping("/reports/deductions")
  public ApiResponse<List<Map<String, Object>>> deductionReport(
      @CurrentSession SessionPrincipal principal) {
    return ApiResponse.ok(ownerService.deductionReport(principal));
  }

  @GetMapping("/reports/services")
  public ApiResponse<List<Map<String, Object>>> serviceReport(
      @CurrentSession SessionPrincipal principal) {
    return ApiResponse.ok(ownerService.serviceReport(principal));
  }

  @GetMapping("/warnings/cards")
  public ApiResponse<List<Map<String, Object>>> cardWarnings(
      @CurrentSession SessionPrincipal principal) {
    return ApiResponse.ok(ownerService.cardWarnings(principal));
  }

  public record StoreProfileRequest(
      @NotBlank @Size(max = 120) String name,
      @NotBlank @Size(max = 12) String cityCode,
      @NotBlank @Size(max = 12) String districtCode,
      @NotBlank @Size(max = 180) String detailAddress,
      @NotEmpty @Size(max = 6) List<@NotBlank String> serviceScopes,
      @NotBlank @Size(max = 40) String contactPhone) {}

  public record UpdateStoreRequest(
      @Size(max = 120) String name,
      @Size(max = 12) String cityCode,
      @Size(max = 12) String districtCode,
      @Size(max = 180) String detailAddress,
      @Size(max = 6) List<@NotBlank String> serviceScopes,
      @Size(max = 40) String contactPhone,
      Map<String, @Valid DayHours> days) {}

  public record StoreNameAvailabilityRequest(
      @NotBlank @Size(max = 120) String name) {}

  public record BusinessHoursRequest(@NotNull Map<String, @Valid DayHours> days) {}

  public record DayHours(boolean open, String start, String end) {}

  public record ResourcesDraftRequest(@NotEmpty List<@Valid ResourceRequest> resources) {}

  public record ResourceRequest(
      @NotBlank @Size(max = 80) String name,
      @NotBlank @Size(max = 80) String resourceType,
      @Min(1) int capacity,
      Boolean enabled,
      @Min(0) Integer sortOrder) {}

  public record UpdateResourceRequest(
      @NotBlank @Size(max = 80) String name,
      @NotBlank @Size(max = 80) String resourceType,
      @Min(1) int capacity,
      boolean enabled,
      @Min(0) int sortOrder) {}

  public record CreateStaffRequest(
      @NotBlank @Size(max = 80) String loginName,
      @NotBlank @Size(min = 8, max = 120) String password,
      @NotBlank @Size(max = 80) String staffName,
      @NotBlank @Size(max = 80) String roleLabel) {}

  public record UpdateStaffRequest(
      @NotBlank @Size(max = 80) String loginName,
      @NotBlank @Size(max = 80) String staffName,
      @NotBlank @Size(max = 80) String roleLabel) {}

  public record UpdatePasswordRequest(@NotBlank @Size(min = 8, max = 120) String password) {}

  public record StatusRequest(@NotBlank String status) {}

  public record CreateServiceRequest(
      @NotBlank String name,
      @NotBlank String serviceType,
      @Min(1) @Max(1440) int durationMin,
      @Min(1) int defaultCapacity,
      @Min(1) int deductCount,
      @NotEmpty List<Long> resourceIds,
      List<Long> staffIds,
      @NotBlank String status) {}

  public record CreateCardTemplateRequest(
      @NotBlank String name,
      @NotBlank String cardType,
      @NotNull @DecimalMin("0.00") BigDecimal salePriceYuan,
      Integer totalCount,
      @Min(1) int validDays,
      Integer lowBalanceThreshold,
      @NotEmpty List<Long> serviceIds,
      @NotEmpty List<Long> staffIds) {}

  public record CreateMemberRequest(
      @NotBlank String name, @NotBlank String memberNo, String contactText) {}

  public record IssueCardRequest(
      @NotNull Long templateId,
      @NotNull @DecimalMin("0.00") BigDecimal receivedAmountYuan,
      @NotNull LocalDate saleDate,
      @NotBlank String payMethodLabel) {}

  public record PublishSlotRequest(
      @NotNull Long serviceId,
      @NotNull Long staffId,
      @NotNull Long resourceId,
      @NotNull OffsetDateTime startAt,
      @NotNull OffsetDateTime endAt,
      @Min(1) int capacity) {}
}
