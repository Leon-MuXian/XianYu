package com.serenmeet.owner.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serenmeet.auth.support.PasswordHasher;
import com.serenmeet.auth.support.SessionPrincipal;
import com.serenmeet.auth.support.StaffCredentialCipher;
import com.serenmeet.auth.support.StaffLoginNameNormalizer;
import com.serenmeet.auth.support.TokenHasher;
import com.serenmeet.common.ApiException;
import com.serenmeet.owner.dto.StaffCredentialResponse;
import com.serenmeet.owner.mapper.OwnerPilotMapper;
import com.serenmeet.owner.support.ServiceNameNormalizer;
import com.serenmeet.owner.support.StaffCredentialRevealRateLimiter;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 店长端阶段一试点链路的业务编排与事务服务。 */
@Service
public class OwnerPilotApplicationService {

  private static final char[] INVITE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
  private static final int INVITE_CODE_LENGTH = 8;
  private static final String INVITE_VALID_DAYS_CONFIG = "member.invite.default_valid_days";
  private static final String CANCELLATION_DEADLINE_CONFIG = "booking.cancel.default_deadline_min";
  private static final int MAX_SERVICE_SCOPE_COUNT = 6;
  private static final int MAX_RESOURCE_TEXT_LENGTH = 80;
  private static final int MAX_SERVICE_NAME_LENGTH = 120;
  private static final int MAX_STAFF_LOGIN_NAME_LENGTH = 80;
  private static final String CUSTOM_RESOURCE_TYPE_PLACEHOLDER = "自定义";
  private static final Pattern STORE_NAME_WHITESPACE = Pattern.compile("[\\s\\p{Z}]+");

  private final OwnerPilotMapper ownerMapper;
  private final ObjectMapper objectMapper;
  private final PasswordHasher passwordHasher;
  private final StaffCredentialCipher staffCredentialCipher;
  private final StaffCredentialRevealRateLimiter staffCredentialRevealRateLimiter;
  private final TokenHasher tokenHasher;
  private final Clock clock;
  private final SecureRandom secureRandom = new SecureRandom();

  public OwnerPilotApplicationService(
      OwnerPilotMapper ownerMapper,
      ObjectMapper objectMapper,
      PasswordHasher passwordHasher,
      StaffCredentialCipher staffCredentialCipher,
      StaffCredentialRevealRateLimiter staffCredentialRevealRateLimiter,
      TokenHasher tokenHasher,
      Clock clock) {
    this.ownerMapper = ownerMapper;
    this.objectMapper = objectMapper;
    this.passwordHasher = passwordHasher;
    this.staffCredentialCipher = staffCredentialCipher;
    this.staffCredentialRevealRateLimiter = staffCredentialRevealRateLimiter;
    this.tokenHasher = tokenHasher;
    this.clock = clock;
  }

  public Map<String, Object> me(SessionPrincipal principal) {
    return requireOne(ownerMapper.selectOwnerProfile(principal.tenantId()));
  }

  public Map<String, Object> store(SessionPrincipal principal) {
    Map<String, Object> row =
        new LinkedHashMap<>(requireOne(ownerMapper.selectStoreDetail(principal.tenantId())));
    JsonNode businessHours = json(row.get("businessHours"));
    row.put("serviceScopes", jsonValue(row.get("serviceScopes")));
    row.put("businessHours", jsonValue(row.get("businessHours")));
    row.put("businessHoursSummary", summarizeBusinessHours(businessHours));
    return row;
  }

  public List<Map<String, Object>> administrativeCities() {
    return ownerMapper.selectAdministrativeCities();
  }

  public List<Map<String, Object>> administrativeDistricts(String cityCode) {
    List<Map<String, Object>> districts = ownerMapper.selectAdministrativeDistricts(cityCode);
    if (districts.isEmpty()) {
      throw new ApiException(HttpStatus.NOT_FOUND, "REGION_NOT_FOUND", "城市不存在或暂无可选区县");
    }
    return districts;
  }

  public Map<String, Object> storeNameAvailability(
      SessionPrincipal principal, String name) {
    String nameKey = normalizeStoreName(name).toLowerCase(Locale.ROOT);
    boolean available =
        ownerMapper.countStoreNameKeyExcludingTenant(nameKey, principal.tenantId()) == 0;
    return Map.of("available", available);
  }

  @Transactional
  public Map<String, Object> updateStoreProfile(
      SessionPrincipal principal,
      String name,
      String cityCode,
      String districtCode,
      String detailAddress,
      List<String> serviceScopes,
      String contactPhone) {
    Long tenantId = principal.tenantId();
    requireStoreId(tenantId);
    StoreProfileData profile =
        buildStoreProfile(
            tenantId,
            name,
            cityCode,
            districtCode,
            detailAddress,
            serviceScopes,
            contactPhone);
    try {
      ownerMapper.updateStoreProfile(
          tenantId,
          profile.name(),
          profile.nameKey(),
          writeJson(profile.serviceScopes()),
          profile.cityCode(),
          profile.districtCode(),
          profile.detailAddress(),
          profile.address(),
          profile.contactPhone());
      ownerMapper.updateTenantProfile(tenantId, profile.name(), profile.city());
      audit(
          tenantId, principal, "update_store_profile", profile.name(), null, "updated", null);
      return store(principal);
    } catch (DataIntegrityViolationException exception) {
      throw storeNameTaken(exception);
    }
  }

  @Transactional
  public Map<String, Object> updateStoreBusinessHours(SessionPrincipal principal, Object request) {
    Long tenantId = principal.tenantId();
    requireStoreId(tenantId);
    ownerMapper.updateStoreBusinessHours(tenantId, writeJson(request));
    audit(tenantId, principal, "update_store_business_hours", "营业时间", null, "updated", null);
    return store(principal);
  }

  public Map<String, Object> dashboard(SessionPrincipal principal) {
    return requireOne(ownerMapper.selectDashboard(principal.tenantId()));
  }

  public Map<String, Object> onboardingDraft(SessionPrincipal principal) {
    Map<String, Object> row =
        new LinkedHashMap<>(
            requireOne(
                ownerMapper.selectOnboardingDraft(
                    principal.tenantId(), Long.valueOf(principal.subjectId()))));
    row.put("storeProfile", jsonValue(row.get("storeProfile")));
    row.put("businessHours", jsonValue(row.get("businessHours")));
    row.put("resources", jsonValue(row.get("resources")));
    row.put("completion", jsonValue(row.get("completion")));
    return row;
  }

  @Transactional
  public Map<String, Object> acknowledgeTrialNotice(SessionPrincipal principal) {
    requireOwned(
        ownerMapper.acknowledgeTrialNotice(
            principal.tenantId(), Long.valueOf(principal.subjectId())));
    return Map.of("trialNoticeRequired", false);
  }

  @Transactional
  public Map<String, Object> saveStoreProfile(
      SessionPrincipal principal,
      String name,
      String cityCode,
      String districtCode,
      String detailAddress,
      List<String> serviceScopes,
      String contactPhone) {
    StoreProfileData profile =
        buildStoreProfile(
            principal.tenantId(),
            name,
            cityCode,
            districtCode,
            detailAddress,
            serviceScopes,
            contactPhone);
    updateDraft(
        principal.tenantId(), DraftSection.STORE_PROFILE, storeProfileDraftPayload(profile));
    return onboardingDraft(principal);
  }

  @Transactional
  public Map<String, Object> saveBusinessHours(SessionPrincipal principal, Object request) {
    updateDraft(principal.tenantId(), DraftSection.BUSINESS_HOURS, request);
    return onboardingDraft(principal);
  }

  @Transactional
  public Map<String, Object> saveResourcesDraft(SessionPrincipal principal, Object request) {
    updateDraft(principal.tenantId(), DraftSection.RESOURCES, normalizeResourcesDraft(request));
    return onboardingDraft(principal);
  }

  @Transactional
  public Map<String, Object> completeOnboarding(SessionPrincipal principal) {
    Long tenantId = principal.tenantId();
    List<Map<String, Object>> existing = ownerMapper.selectStores(tenantId);
    if (!existing.isEmpty()) {
      return existing.getFirst();
    }
    Map<String, Object> draft = requireOne(ownerMapper.selectOnboardingDraftForUpdate(tenantId));
    JsonNode completion = json(draft.get("completion_status"));
    boolean incomplete =
        !completion.path("storeProfileDone").asBoolean()
            || !completion.path("businessHoursDone").asBoolean()
            || !completion.path("resourceDone").asBoolean();
    if (incomplete) {
      throw new ApiException(HttpStatus.CONFLICT, "FORM_ERROR", "请先完成门店资料、营业时间和履约资源");
    }

    JsonNode profile = json(draft.get("store_profile_draft"));
    JsonNode hours = json(draft.get("business_hours_draft"));
    JsonNode resources = json(draft.get("resources_draft"));
    validateOnboardingCollections(profile, resources);
    StoreProfileData storeProfile =
        buildStoreProfile(
            tenantId,
            requiredText(profile, "name", "门店名称不能为空"),
            requiredText(profile, "cityCode", "请选择城市"),
            requiredText(profile, "districtCode", "请选择区县"),
            requiredText(profile, "detailAddress", "请填写详细地址"),
            stringList(profile.path("serviceScopes")),
            requiredText(profile, "contactPhone", "联系电话不能为空"));

    Long storeId;
    try {
      storeId =
          ownerMapper.insertStore(
              tenantId,
              storeProfile.name(),
              storeProfile.nameKey(),
              writeJson(storeProfile.serviceScopes()),
              storeProfile.cityCode(),
              storeProfile.districtCode(),
              storeProfile.detailAddress(),
              storeProfile.address(),
              storeProfile.contactPhone(),
              hours.toString());
    } catch (DataIntegrityViolationException exception) {
      throw storeNameTaken(exception);
    }
    int sortOrder = 0;
    for (JsonNode resource : resources) {
      insertResource(tenantId, storeId, resource, sortOrder);
      sortOrder++;
    }
    ownerMapper.updateTenantProfile(tenantId, storeProfile.name(), storeProfile.city());
    ownerMapper.markOnboardingConverted(tenantId);
    audit(
        tenantId,
        principal,
        "complete_onboarding",
        storeProfile.name(),
        null,
        "created",
        null);
    return Map.of("id", storeId, "name", storeProfile.name());
  }

  public List<Map<String, Object>> resources(SessionPrincipal principal) {
    return ownerMapper.selectResources(principal.tenantId());
  }

  @Transactional
  public Map<String, Object> createResource(
      SessionPrincipal principal,
      String name,
      String type,
      int capacity,
      Boolean enabled,
      Integer sortOrder) {
    Long storeId = requireStoreId(principal.tenantId());
    String normalizedName = normalizeResourceText(name, "资源名称不能为空");
    String normalizedType = normalizeResourceType(type);
    boolean normalizedEnabled = enabled == null || enabled;
    int normalizedSortOrder = sortOrder == null ? 0 : Math.max(0, sortOrder);
    try {
      Long id =
          ownerMapper.insertResource(
              principal.tenantId(),
              storeId,
              normalizedName,
              normalizedType,
              capacity,
              normalizedEnabled,
              normalizedSortOrder);
      return Map.of(
          "id", id,
          "name", normalizedName,
          "resourceType", normalizedType,
          "capacity", capacity,
          "enabled", normalizedEnabled,
          "sortOrder", normalizedSortOrder);
    } catch (DataIntegrityViolationException exception) {
      throw new ApiException(HttpStatus.CONFLICT, "CONFLICT", "资源名称已存在");
    }
  }

  @Transactional
  public Map<String, Object> updateResource(
      SessionPrincipal principal,
      Long resourceId,
      String name,
      String type,
      int capacity,
      boolean enabled,
      int sortOrder) {
    Long tenantId = principal.tenantId();
    String normalizedName = normalizeResourceText(name, "资源名称不能为空");
    String normalizedType = normalizeResourceType(type);
    requireOwned(ownerMapper.countOwnedResource(tenantId, resourceId));
    if (!enabled && ownerMapper.countEnabledResourcesExcluding(tenantId, resourceId) == 0) {
      throw new ApiException(HttpStatus.CONFLICT, "RESOURCE_REQUIRED", "至少保留 1 个启用资源");
    }
    try {
      requireOwned(
          ownerMapper.updateResource(
              tenantId,
              resourceId,
              normalizedName,
              normalizedType,
              capacity,
              enabled,
              Math.max(0, sortOrder)));
      audit(tenantId, principal, "update_resource", normalizedName, null, "updated", null);
      return Map.of(
          "id", resourceId,
          "name", normalizedName,
          "resourceType", normalizedType,
          "capacity", capacity,
          "enabled", enabled,
          "sortOrder", Math.max(0, sortOrder));
    } catch (DataIntegrityViolationException exception) {
      throw new ApiException(HttpStatus.CONFLICT, "CONFLICT", "资源名称已存在");
    }
  }

  @Transactional
  public Map<String, Object> deleteResource(SessionPrincipal principal, Long resourceId) {
    Long tenantId = principal.tenantId();
    requireOwned(ownerMapper.countOwnedResource(tenantId, resourceId));
    boolean blocked =
        ownerMapper.countEnabledResourcesExcluding(tenantId, resourceId) == 0
            || ownerMapper.countResourceServiceBindings(tenantId, resourceId) > 0
            || ownerMapper.countResourceFutureSlots(tenantId, resourceId) > 0;
    if (blocked) {
      throw new ApiException(HttpStatus.CONFLICT, "RESOURCE_IN_USE", "资源仍被服务项目或排期使用，或删除后将没有启用资源");
    }
    requireOwned(ownerMapper.deleteResource(tenantId, resourceId));
    audit(tenantId, principal, "delete_resource", resourceId.toString(), null, "deleted", null);
    return Map.of("id", resourceId, "deleted", true);
  }

  public List<Map<String, Object>> staff(SessionPrincipal principal) {
    return ownerMapper.selectStaff(principal.tenantId());
  }

  @Transactional
  public StaffCredentialResponse revealStaffCredential(
      SessionPrincipal principal, Long staffId) {
    Long tenantId = principal.tenantId();
    requireOwned(ownerMapper.countOwnedStaff(tenantId, staffId));
    staffCredentialRevealRateLimiter.acquire(principal.subjectId(), staffId);
    Map<String, Object> row = ownerMapper.selectStaffCredential(tenantId, staffId);
    if (row == null || row.isEmpty()) {
      throw new ApiException(
          HttpStatus.CONFLICT,
          "STAFF_CREDENTIAL_UNAVAILABLE",
          "该员工密码创建于凭据保管启用前，请先重置密码");
    }
    String password =
        staffCredentialCipher.decrypt(
            tenantId,
            staffId,
            row.get("keyId").toString(),
            row.get("cipherVersion").toString(),
            (byte[]) row.get("nonce"),
            (byte[]) row.get("ciphertext"));
    audit(
        tenantId,
        principal,
        "reveal_staff_credential",
        staffId.toString(),
        null,
        "revealed",
        null);
    return new StaffCredentialResponse(
        staffId, row.get("staffName").toString(), row.get("loginName").toString(), password);
  }

  @Transactional
  public Map<String, Object> createStaff(
      SessionPrincipal principal,
      String loginName,
      String password,
      String staffName,
      String roleLabel) {
    if (password.length() < 8) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "FIELD_ERROR", "员工密码至少 8 位");
    }
    Long tenantId = principal.tenantId();
    String normalizedLoginName = normalizeStaffLoginName(loginName);
    if (ownerMapper.countStaffLoginName(normalizedLoginName, null) > 0) {
      throw staffLoginNameTaken();
    }
    Long id;
    try {
      id =
        ownerMapper.insertStaffAccount(
          tenantId,
          requireStoreId(tenantId),
          normalizedLoginName,
          passwordHasher.encode(password),
          staffName,
          roleLabel);
    } catch (DuplicateKeyException exception) {
      throw staffLoginNameTaken(exception);
    }
    StaffCredentialCipher.EncryptedCredential credential =
        staffCredentialCipher.encrypt(tenantId, id, password);
    requireOwned(
        ownerMapper.insertStaffCredentialSecret(
            tenantId,
            id,
            credential.keyId(),
            credential.cipherVersion(),
            credential.nonce(),
            credential.ciphertext()));
    ownerMapper.insertStaffProfile(tenantId, id, staffName);
    return Map.of(
      "id", id,
      "loginName", normalizedLoginName,
      "staffName", staffName,
      "roleLabel", roleLabel,
      "status", "active");
  }

  @Transactional
  public Map<String, Object> updateStaff(
      SessionPrincipal principal,
      Long staffId,
      String loginName,
      String staffName,
      String roleLabel) {
    Long tenantId = principal.tenantId();
    requireOwned(ownerMapper.countOwnedStaff(tenantId, staffId));
    String normalizedLoginName = normalizeStaffLoginName(loginName);
    if (ownerMapper.countStaffLoginName(normalizedLoginName, staffId) > 0) {
      throw staffLoginNameTaken();
    }
    int updated;
    try {
      updated =
        ownerMapper.updateStaffAccount(
          tenantId, staffId, normalizedLoginName, staffName, roleLabel);
    } catch (DuplicateKeyException exception) {
      throw staffLoginNameTaken(exception);
    }
    requireOwned(updated);
    requireOwned(ownerMapper.updateStaffProfileName(tenantId, staffId, staffName));
    audit(tenantId, principal, "update_staff", staffName, null, "updated", null);
    return Map.of(
      "id", staffId,
      "loginName", normalizedLoginName,
      "staffName", staffName,
      "roleLabel", roleLabel);
  }

  @Transactional
  public Map<String, Object> updateStaffPassword(
      SessionPrincipal principal, Long staffId, String password) {
    if (password.length() < 8) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "FIELD_ERROR", "员工密码至少 8 位");
    }
    Long tenantId = principal.tenantId();
    requireOwned(ownerMapper.countOwnedStaff(tenantId, staffId));
    StaffCredentialCipher.EncryptedCredential credential =
        staffCredentialCipher.encrypt(tenantId, staffId, password);
    requireOwned(
        ownerMapper.updateStaffPassword(tenantId, staffId, passwordHasher.encode(password)));
    requireOwned(
        ownerMapper.upsertStaffCredentialSecret(
            tenantId,
            staffId,
            credential.keyId(),
            credential.cipherVersion(),
            credential.nonce(),
            credential.ciphertext()));
    ownerMapper.revokeStaffSessions(tenantId, staffId);
    audit(tenantId, principal, "update_staff_password", staffId.toString(), null, "reset", null);
    return Map.of("id", staffId, "passwordReset", true);
  }

  @Transactional
  public Map<String, Object> updateStaffStatus(
      SessionPrincipal principal, Long staffId, String status) {
    validateStatus(status, List.of("active", "disabled"), "员工状态无效");
    Long tenantId = principal.tenantId();
    requireOwned(ownerMapper.updateStaffStatus(tenantId, staffId, status));
    if ("disabled".equals(status)) {
      ownerMapper.revokeStaffSessions(tenantId, staffId);
    }
    audit(tenantId, principal, "update_staff_status", staffId.toString(), null, status, null);
    return Map.of("id", staffId, "status", status);
  }

  @Transactional
  public Map<String, Object> deleteStaff(SessionPrincipal principal, Long staffId) {
    Long tenantId = principal.tenantId();
    requireOwned(ownerMapper.countOwnedStaff(tenantId, staffId));
    boolean blocked =
        ownerMapper.countStaffServiceBindings(tenantId, staffId) > 0
            || ownerMapper.countStaffCardScopes(tenantId, staffId) > 0
            || ownerMapper.countStaffFutureSlots(tenantId, staffId) > 0;
    if (blocked) {
      throw new ApiException(HttpStatus.CONFLICT, "STAFF_IN_USE", "员工仍关联服务项目、会员卡范围或排期，请先解除关联");
    }
    requireOwned(ownerMapper.deleteStaff(tenantId, staffId));
    audit(tenantId, principal, "delete_staff", staffId.toString(), null, "deleted", null);
    return Map.of("id", staffId, "deleted", true);
  }

  public List<Map<String, Object>> services(SessionPrincipal principal) {
    Long tenantId = principal.tenantId();
    List<Map<String, Object>> result = new ArrayList<>();
    for (Map<String, Object> source : ownerMapper.selectServices(tenantId)) {
      Map<String, Object> row = new LinkedHashMap<>(source);
      Long serviceId = asLong(row.get("id"));
      row.put("resourceIds", ownerMapper.selectServiceResourceIds(tenantId, serviceId));
      row.put("staffIds", ownerMapper.selectServiceStaffIds(tenantId, serviceId));
      result.add(row);
    }
    return result;
  }

  @Transactional
  public Map<String, Object> createService(
      SessionPrincipal principal,
      String name,
      String type,
      int durationMin,
      int capacity,
      int deductCount,
      List<Long> resourceIds,
      List<Long> staffIds,
      String requestedStatus) {
    Long tenantId = principal.tenantId();
    String normalizedName = normalizeServiceName(name);
    requireServiceNameAvailable(tenantId, normalizedName, null);
    List<Long> selectedStaffIds = staffIds == null ? List.of() : staffIds;
    validateStatus(requestedStatus, List.of("draft", "active"), "服务状态无效");
    validateSelectableIds(OwnedEntity.RESOURCE, resourceIds, tenantId);
    validateSelectableIds(OwnedEntity.STAFF, selectedStaffIds, tenantId);
    String status =
        "active".equals(requestedStatus) && !selectedStaffIds.isEmpty() ? "active" : "draft";
    Long id;
    try {
      id =
          ownerMapper.insertService(
              tenantId,
              requireStoreId(tenantId),
              normalizedName,
              type,
              durationMin,
              capacity,
              deductCount,
              status);
    } catch (DuplicateKeyException exception) {
      throw serviceNameTaken(exception);
    }
    for (Long resourceId : resourceIds) {
      ownerMapper.insertServiceResourceBinding(tenantId, id, resourceId);
    }
    for (Long staffId : selectedStaffIds) {
      ownerMapper.insertStaffServiceBinding(tenantId, staffId, id);
    }
    return Map.of("id", id, "name", normalizedName, "status", status);
  }

  @Transactional
  public Map<String, Object> updateService(
      SessionPrincipal principal,
      Long serviceId,
      String name,
      String type,
      int durationMin,
      int capacity,
      int deductCount,
      List<Long> resourceIds,
      List<Long> staffIds,
      String requestedStatus) {
    Long tenantId = principal.tenantId();
    String normalizedName = normalizeServiceName(name);
    List<Long> selectedStaffIds = staffIds == null ? List.of() : staffIds;
    requireOwned(ownerMapper.countOwnedService(tenantId, serviceId));
    requireServiceNameAvailable(tenantId, normalizedName, serviceId);
    validateStatus(requestedStatus, List.of("draft", "active"), "服务状态无效");
    validateSelectableIds(OwnedEntity.RESOURCE, resourceIds, tenantId);
    validateSelectableIds(OwnedEntity.STAFF, selectedStaffIds, tenantId);
    String status =
        "active".equals(requestedStatus) && !selectedStaffIds.isEmpty() ? "active" : "draft";
    int updated;
    try {
      updated =
          ownerMapper.updateService(
              tenantId,
              serviceId,
              normalizedName,
              type,
              durationMin,
              capacity,
              deductCount,
              status);
    } catch (DuplicateKeyException exception) {
      throw serviceNameTaken(exception);
    }
    requireOwned(updated);
    ownerMapper.deleteServiceResourceBindings(tenantId, serviceId);
    ownerMapper.deleteStaffServiceBindings(tenantId, serviceId);
    for (Long resourceId : resourceIds) {
      ownerMapper.insertServiceResourceBinding(tenantId, serviceId, resourceId);
    }
    for (Long staffId : selectedStaffIds) {
      ownerMapper.insertStaffServiceBinding(tenantId, staffId, serviceId);
    }
    audit(tenantId, principal, "update_service", normalizedName, null, status, null);
    return Map.of("id", serviceId, "name", normalizedName, "status", status);
  }

  @Transactional
  public Map<String, Object> updateServiceStatus(
      SessionPrincipal principal, Long serviceId, String status) {
    validateStatus(status, List.of("active", "disabled"), "服务状态无效");
    Long tenantId = principal.tenantId();
    requireOwned(ownerMapper.countOwnedService(tenantId, serviceId));
    boolean missingScope =
        ownerMapper.selectServiceResourceIds(tenantId, serviceId).isEmpty()
            || ownerMapper.selectServiceStaffIds(tenantId, serviceId).isEmpty();
    if ("active".equals(status) && missingScope) {
      throw new ApiException(HttpStatus.CONFLICT, "FORM_ERROR", "服务缺少适用资源或可履约员工");
    }
    requireOwned(ownerMapper.updateServiceStatus(tenantId, serviceId, status));
    audit(tenantId, principal, "update_service_status", serviceId.toString(), null, status, null);
    return Map.of("id", serviceId, "status", status);
  }

  @Transactional
  public Map<String, Object> deleteService(SessionPrincipal principal, Long serviceId) {
    Long tenantId = principal.tenantId();
    Map<String, Object> service =
        requireOne(ownerMapper.selectServiceForUpdate(tenantId, serviceId));
    String status = service.get("status").toString();
    if ("active".equals(status)) {
      throw new ApiException(
          HttpStatus.CONFLICT,
          "SERVICE_MUST_BE_DISABLED",
          "启用中的服务不能删除，请先停用服务");
    }
    boolean blocked =
        ownerMapper.countServiceCardScopes(tenantId, serviceId) > 0
            || ownerMapper.countServiceSlots(tenantId, serviceId) > 0;
    if (blocked) {
      throw new ApiException(
          HttpStatus.CONFLICT,
          "SERVICE_IN_USE",
          "服务仍关联会员卡适用范围或排期，不能删除，请保留停用状态");
    }
    try {
      requireOwned(ownerMapper.deleteService(tenantId, serviceId));
    } catch (DataIntegrityViolationException exception) {
      throw new ApiException(
          HttpStatus.CONFLICT,
          "SERVICE_IN_USE",
          "服务已产生新的业务关联，不能删除，请保留停用状态",
          exception);
    }
    String serviceName = service.get("name").toString();
    audit(
        tenantId,
        principal,
        "delete_service",
        serviceName,
        "id=" + serviceId + ",status=" + status,
        "deleted",
        null);
    return Map.of("id", serviceId, "deleted", true);
  }

  public List<Map<String, Object>> cardTemplates(SessionPrincipal principal) {
    Long tenantId = principal.tenantId();
    List<Map<String, Object>> result = new ArrayList<>();
    for (Map<String, Object> source : ownerMapper.selectCardTemplates(tenantId)) {
      Map<String, Object> row = new LinkedHashMap<>(source);
      Long templateId = asLong(row.get("id"));
      row.put("serviceIds", ownerMapper.selectCardServiceIds(tenantId, templateId));
      row.put("staffIds", ownerMapper.selectCardStaffIds(tenantId, templateId));
      result.add(row);
    }
    return result;
  }

  @Transactional
  public Map<String, Object> createCardTemplate(
      SessionPrincipal principal,
      String name,
      String cardType,
      BigDecimal salePrice,
      Integer totalCount,
      int validDays,
      Integer lowBalanceThreshold,
      List<Long> serviceIds,
      List<Long> staffIds) {
    Long tenantId = principal.tenantId();
    validateSelectableIds(OwnedEntity.SERVICE, serviceIds, tenantId);
    validateSelectableIds(OwnedEntity.STAFF, staffIds, tenantId);
    validateCardTemplate(cardType, totalCount, lowBalanceThreshold);
    boolean countCard = "count".equals(cardType);
    Long id =
        ownerMapper.insertCardTemplate(
            tenantId,
            requireStoreId(tenantId),
            name,
            cardType,
            salePrice,
            countCard ? totalCount : null,
            validDays,
            countCard ? lowBalanceThreshold : null);
    for (Long serviceId : serviceIds) {
      ownerMapper.insertCardServiceScope(tenantId, id, serviceId);
    }
    for (Long staffId : staffIds) {
      ownerMapper.insertCardStaffScope(tenantId, id, staffId);
    }
    return Map.of("id", id, "name", name, "cardType", cardType, "status", "active");
  }

  @Transactional
  public Map<String, Object> updateCardTemplate(
      SessionPrincipal principal,
      Long templateId,
      String name,
      String cardType,
      BigDecimal salePrice,
      Integer totalCount,
      int validDays,
      Integer lowBalanceThreshold,
      List<Long> serviceIds,
      List<Long> staffIds) {
    Long tenantId = principal.tenantId();
    validateSelectableIds(OwnedEntity.SERVICE, serviceIds, tenantId);
    validateSelectableIds(OwnedEntity.STAFF, staffIds, tenantId);
    validateCardTemplate(cardType, totalCount, lowBalanceThreshold);
    boolean countCard = "count".equals(cardType);
    requireOwned(
        ownerMapper.updateCardTemplate(
            tenantId,
            templateId,
            name,
            cardType,
            salePrice,
            countCard ? totalCount : null,
            validDays,
            countCard ? lowBalanceThreshold : null));
    ownerMapper.deleteCardServiceScopes(tenantId, templateId);
    ownerMapper.deleteCardStaffScopes(tenantId, templateId);
    for (Long serviceId : serviceIds) {
      ownerMapper.insertCardServiceScope(tenantId, templateId, serviceId);
    }
    for (Long staffId : staffIds) {
      ownerMapper.insertCardStaffScope(tenantId, templateId, staffId);
    }
    audit(tenantId, principal, "update_card_template", name, null, "updated", null);
    return Map.of("id", templateId, "name", name, "cardType", cardType);
  }

  @Transactional
  public Map<String, Object> updateCardTemplateStatus(
      SessionPrincipal principal, Long templateId, String status) {
    validateStatus(status, List.of("active", "disabled"), "会员卡模板状态无效");
    Long tenantId = principal.tenantId();
    requireOwned(ownerMapper.updateCardTemplateStatus(tenantId, templateId, status));
    audit(
        tenantId,
        principal,
        "update_card_template_status",
        templateId.toString(),
        null,
        status,
        null);
    return Map.of("id", templateId, "status", status);
  }

  public List<Map<String, Object>> members(SessionPrincipal principal) {
    return ownerMapper.selectMembers(principal.tenantId());
  }

  @Transactional
  public Map<String, Object> createMember(
      SessionPrincipal principal, String name, String memberNo, String contactText) {
    Long tenantId = principal.tenantId();
    try {
      Long id =
          ownerMapper.insertMember(
              tenantId,
              requireStoreId(tenantId),
              name,
              memberNo,
              contactText == null ? "" : contactText);
      return Map.of("id", id, "name", name, "memberNo", memberNo, "bindStatus", "unbound");
    } catch (DataIntegrityViolationException exception) {
      throw new ApiException(HttpStatus.CONFLICT, "CONFLICT", "会员编号已存在");
    }
  }

  @Transactional
  public Map<String, Object> issueCard(
      SessionPrincipal principal,
      Long memberId,
      Long templateId,
      BigDecimal receivedAmount,
      LocalDate saleDate,
      String payMethodLabel) {
    Long tenantId = principal.tenantId();
    Map<String, Object> member = requireOne(ownerMapper.selectOwnedMember(tenantId, memberId));
    Map<String, Object> template =
        requireOne(ownerMapper.selectActiveCardTemplate(tenantId, templateId));
    String cardType = template.get("cardType").toString();
    Integer totalCount = asInteger(template.get("totalCount"));
    int validDays = asInteger(template.get("validDays"));
    LocalDate validUntil = saleDate.plusDays(validDays - 1L);
    Long storeId = asLong(member.get("storeId"));
    Long cardId =
        ownerMapper.insertMemberCard(
            tenantId,
            storeId,
            memberId,
            templateId,
            cardType,
            receivedAmount,
            "count".equals(cardType) ? totalCount : null,
            saleDate,
            validUntil);
    Long saleId =
        ownerMapper.insertOfflineSale(
            tenantId,
            storeId,
            memberId,
            cardId,
            receivedAmount,
            saleDate,
            payMethodLabel,
            principal.subjectId());
    audit(
        tenantId,
        principal,
        "issue_member_card",
        member.get("name").toString(),
        null,
        cardId.toString(),
        "线下实收 " + receivedAmount);
    return Map.of(
        "memberCardId", cardId,
        "saleRecordId", saleId,
        "cardType", cardType,
        "receivedAmountYuan", receivedAmount,
        "validFrom", saleDate,
        "validUntil", validUntil);
  }

  @Transactional
  public Map<String, Object> createInvite(SessionPrincipal principal, Long memberId) {
    Long tenantId = principal.tenantId();
    Map<String, Object> member = requireOne(ownerMapper.selectOwnedMember(tenantId, memberId));
    Integer validDays = requireConfig(INVITE_VALID_DAYS_CONFIG);
    ownerMapper.revokeActiveInvites(tenantId, memberId);
    String code = generateInviteCode();
    OffsetDateTime expiresAt = OffsetDateTime.now(clock).plusDays(validDays);
    ownerMapper.insertInvite(
        tenantId,
        asLong(member.get("storeId")),
        memberId,
        tokenHasher.hash(code),
        code.substring(code.length() - 4),
        expiresAt);
    return Map.of("code", code, "expiresAt", expiresAt, "status", "active");
  }

  public List<Map<String, Object>> schedules(SessionPrincipal principal, LocalDate date) {
    return ownerMapper.selectSchedules(principal.tenantId(), date);
  }

  @Transactional
  public Map<String, Object> publishSlot(
      SessionPrincipal principal,
      Long serviceId,
      Long staffId,
      Long resourceId,
      OffsetDateTime startAt,
      OffsetDateTime endAt,
      int capacity) {
    return saveSlot(
        principal, serviceId, staffId, resourceId, startAt, endAt, capacity, "published");
  }

  @Transactional
  public Map<String, Object> saveSlot(
      SessionPrincipal principal,
      Long serviceId,
      Long staffId,
      Long resourceId,
      OffsetDateTime startAt,
      OffsetDateTime endAt,
      int capacity,
      String status) {
    validateStatus(status, List.of("draft", "published"), "排期状态无效");
    Long tenantId = principal.tenantId();
    Map<String, Object> service = requireOne(ownerMapper.selectActiveService(tenantId, serviceId));
    requireOwned(ownerMapper.countActiveStaff(tenantId, staffId));
    requireOwned(ownerMapper.countEnabledResource(tenantId, resourceId));
    validateSlotTime(startAt, endAt);
    int staffBound = ownerMapper.countStaffServiceBinding(tenantId, serviceId, staffId);
    int resourceBound = ownerMapper.countServiceResourceBinding(tenantId, serviceId, resourceId);
    if (staffBound == 0 || resourceBound == 0) {
      throw new ApiException(HttpStatus.CONFLICT, "FORM_ERROR", "员工或资源不在该服务的适用范围内");
    }
    Long id =
        ownerMapper.insertBookableSlot(
            tenantId,
            asLong(service.get("storeId")),
            serviceId,
            staffId,
            resourceId,
            startAt,
            endAt,
            capacity,
            status,
            requireConfig(CANCELLATION_DEADLINE_CONFIG));
    return Map.of(
        "id", id,
        "status", status,
        "startAt", startAt,
        "endAt", endAt,
        "capacity", capacity);
  }

  public Map<String, Object> reportSummary(SessionPrincipal principal) {
    return requireOne(ownerMapper.selectReportSummary(principal.tenantId()));
  }

  public List<Map<String, Object>> cardWarnings(SessionPrincipal principal) {
    return ownerMapper.selectCardWarnings(principal.tenantId());
  }

  public List<Map<String, Object>> offlineSales(SessionPrincipal principal) {
    return ownerMapper.selectOfflineSales(principal.tenantId());
  }

  public List<Map<String, Object>> bookingReport(SessionPrincipal principal) {
    return ownerMapper.selectBookingReport(principal.tenantId());
  }

  public List<Map<String, Object>> deductionReport(SessionPrincipal principal) {
    return ownerMapper.selectDeductionReport(principal.tenantId());
  }

  public List<Map<String, Object>> serviceReport(SessionPrincipal principal) {
    return ownerMapper.selectServiceReport(principal.tenantId());
  }

  private void validateOnboardingCollections(JsonNode profile, JsonNode resources) {
    boolean profileListsMissing =
        !profile.path("serviceScopes").isArray()
            || profile.path("serviceScopes").isEmpty();
    if (profileListsMissing) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "FIELD_ERROR", "服务范围不能为空");
    }
    if (!resources.isArray() || resources.isEmpty()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "FIELD_ERROR", "至少需要一个履约资源");
    }
    boolean hasEnabledResource = false;
    for (JsonNode resource : resources) {
      if (!resource.hasNonNull("enabled") || resource.path("enabled").asBoolean()) {
        hasEnabledResource = true;
        break;
      }
    }
    if (!hasEnabledResource) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "RESOURCE_REQUIRED", "至少保留 1 个启用资源");
    }
  }

  private StoreProfileData buildStoreProfile(
      Long tenantId,
      String name,
      String cityCode,
      String districtCode,
      String detailAddress,
      List<String> serviceScopes,
      String contactPhone) {
    validateStoreProfile(
        name,
        cityCode,
        districtCode,
        detailAddress,
        contactPhone);
    List<String> normalizedServiceScopes = normalizeServiceScopes(serviceScopes);
    String normalizedName = normalizeStoreName(name);
    String nameKey = normalizedName.toLowerCase(Locale.ROOT);
    if (ownerMapper.countStoreNameKeyExcludingTenant(nameKey, tenantId) > 0) {
      throw storeNameTaken(null);
    }
    Map<String, Object> region = ownerMapper.selectAdministrativeRegion(cityCode, districtCode);
    if (region == null || region.isEmpty()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "REGION_INVALID", "所选城市与区县不匹配，请重新选择");
    }
    String normalizedDetailAddress = detailAddress.trim();
    String city = region.get("city").toString();
    String district = region.get("district").toString();
    String address = city + district + normalizedDetailAddress;
    if (address.length() > 255) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "FIELD_ERROR", "门店地址过长，请精简详细地址");
    }
    return new StoreProfileData(
        normalizedName,
        nameKey,
        cityCode,
        city,
        districtCode,
        district,
        normalizedDetailAddress,
        address,
        contactPhone.trim(),
        normalizedServiceScopes);
  }

  private void validateStoreProfile(
      String name,
      String cityCode,
      String districtCode,
      String detailAddress,
      String contactPhone) {
    boolean textMissing =
        name == null
            || name.isBlank()
            || cityCode == null
            || cityCode.isBlank()
            || districtCode == null
            || districtCode.isBlank()
            || detailAddress == null
            || detailAddress.isBlank()
            || contactPhone == null
            || contactPhone.isBlank();
    if (textMissing) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "FIELD_ERROR", "请完成门店资料必填项");
    }
  }

  private List<String> normalizeServiceScopes(List<String> serviceScopes) {
    if (serviceScopes == null || serviceScopes.isEmpty()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "FIELD_ERROR", "服务范围不能为空");
    }
    List<String> normalized =
        serviceScopes.stream()
            .filter(value -> value != null && !value.isBlank())
            .map(String::trim)
            .distinct()
            .toList();
    if (normalized.isEmpty()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "FIELD_ERROR", "服务范围不能为空");
    }
    if (normalized.size() > MAX_SERVICE_SCOPE_COUNT) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "FIELD_ERROR", "服务范围最多填写 6 项");
    }
    return normalized;
  }

  private String normalizeStoreName(String name) {
    if (name == null || name.isBlank()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "FIELD_ERROR", "门店名称不能为空");
    }
    String normalized =
        STORE_NAME_WHITESPACE
            .matcher(Normalizer.normalize(name, Normalizer.Form.NFKC).trim())
            .replaceAll(" ");
    if (normalized.length() > 120) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "FIELD_ERROR", "门店名称不能超过 120 个字符");
    }
    return normalized;
  }

  private Map<String, Object> storeProfileDraftPayload(StoreProfileData profile) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("name", profile.name());
    payload.put("nameKey", profile.nameKey());
    payload.put("cityCode", profile.cityCode());
    payload.put("city", profile.city());
    payload.put("districtCode", profile.districtCode());
    payload.put("district", profile.district());
    payload.put("detailAddress", profile.detailAddress());
    payload.put("address", profile.address());
    payload.put("contactPhone", profile.contactPhone());
    payload.put("serviceScopes", profile.serviceScopes());
    return payload;
  }

  private List<String> stringList(JsonNode node) {
    List<String> values = new ArrayList<>();
    if (!node.isArray()) {
      return values;
    }
    for (JsonNode item : node) {
      values.add(item.asText());
    }
    return values;
  }

  private ApiException storeNameTaken(Throwable cause) {
    return new ApiException(
        HttpStatus.CONFLICT, "STORE_NAME_TAKEN", "该门店名称已被使用，请更换名称", cause);
  }

  private String normalizeStaffLoginName(String loginName) {
    String normalized = StaffLoginNameNormalizer.normalize(loginName);
    if (normalized.isEmpty()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "FIELD_ERROR", "员工登录账号不能为空");
    }
    if (normalized.length() > MAX_STAFF_LOGIN_NAME_LENGTH) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "FIELD_ERROR", "员工登录账号不能超过 80 个字符");
    }
    return normalized;
  }

  private ApiException staffLoginNameTaken() {
    return new ApiException(
        HttpStatus.CONFLICT, "STAFF_LOGIN_NAME_TAKEN", "该登录账号已被使用，请更换账号");
  }

  private ApiException staffLoginNameTaken(Throwable cause) {
    return new ApiException(
        HttpStatus.CONFLICT,
        "STAFF_LOGIN_NAME_TAKEN",
        "该登录账号已被使用，请更换账号",
        cause);
  }

  private String normalizeServiceName(String name) {
    String normalized = ServiceNameNormalizer.normalizeDisplayName(name);
    if (normalized.isEmpty()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "FIELD_ERROR", "服务项目名称不能为空");
    }
    if (normalized.length() > MAX_SERVICE_NAME_LENGTH) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "FIELD_ERROR", "服务项目名称不能超过 120 个字符");
    }
    return normalized;
  }

  private void requireServiceNameAvailable(
      Long tenantId, String normalizedName, Long excludeServiceId) {
    String nameKey = ServiceNameNormalizer.keyOf(normalizedName);
    if (ownerMapper.countServiceNameKey(tenantId, nameKey, excludeServiceId) > 0) {
      throw serviceNameTaken(null);
    }
  }

  private ApiException serviceNameTaken(Throwable cause) {
    return new ApiException(
        HttpStatus.CONFLICT,
        "SERVICE_NAME_TAKEN",
        "当前租户已存在同名服务项目，请更换名称",
        cause);
  }

  private void validateCardTemplate(
      String cardType, Integer totalCount, Integer lowBalanceThreshold) {
    if (!List.of("count", "period").contains(cardType)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "FIELD_ERROR", "会员卡类型无效");
    }
    boolean invalidCountCard =
        "count".equals(cardType)
            && (totalCount == null
                || totalCount <= 0
                || lowBalanceThreshold == null
                || lowBalanceThreshold < 0);
    if (invalidCountCard) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "FIELD_ERROR", "次数卡需填写有效总次数和低余额阈值");
    }
  }

  private void validateSlotTime(OffsetDateTime startAt, OffsetDateTime endAt) {
    if (endAt == null || startAt == null || !endAt.isAfter(startAt)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "FIELD_ERROR", "结束时间必须晚于开始时间");
    }
    if (startAt.isBefore(OffsetDateTime.now(clock))) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "FIELD_ERROR", "只能发布未来时段");
    }
  }

  private void updateDraft(Long tenantId, DraftSection section, Object request) {
    try {
      String payload = objectMapper.writeValueAsString(request);
      int updated =
          switch (section) {
            case STORE_PROFILE -> ownerMapper.updateStoreProfileDraft(tenantId, payload);
            case BUSINESS_HOURS -> ownerMapper.updateBusinessHoursDraft(tenantId, payload);
            case RESOURCES -> ownerMapper.updateResourcesDraft(tenantId, payload);
          };
      if (updated != 1) {
        throw new ApiException(HttpStatus.CONFLICT, "CONFLICT", "开店草稿已完成或不存在");
      }
    } catch (JsonProcessingException exception) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "草稿内容无法处理", exception);
    }
  }

  private void insertResource(Long tenantId, Long storeId, JsonNode resource, int fallbackSortOrder) {
    String name = normalizeResourceText(requiredText(resource, "name", "资源名称不能为空"), "资源名称不能为空");
    String type = normalizeResourceType(requiredText(resource, "resourceType", "资源类型不能为空"));
    int capacity = resource.path("capacity").asInt(0);
    if (capacity < 1) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "FIELD_ERROR", "资源容量至少为 1");
    }
    boolean enabled = !resource.hasNonNull("enabled") || resource.path("enabled").asBoolean();
    int sortOrder =
        resource.hasNonNull("sortOrder")
            ? Math.max(0, resource.path("sortOrder").asInt(fallbackSortOrder))
            : fallbackSortOrder;
    ownerMapper.insertResource(tenantId, storeId, name, type, capacity, enabled, sortOrder);
  }

  private List<Map<String, Object>> normalizeResourcesDraft(Object request) {
    JsonNode resources = json(request);
    if (!resources.isArray() || resources.isEmpty()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "FIELD_ERROR", "至少需要一个履约资源");
    }
    List<Map<String, Object>> normalized = new ArrayList<>();
    boolean hasEnabledResource = false;
    int defaultSortOrder = 1;
    for (JsonNode resource : resources) {
      String name =
          normalizeResourceText(
              requiredText(resource, "name", "资源名称不能为空"), "资源名称不能为空");
      String resourceType =
          normalizeResourceType(requiredText(resource, "resourceType", "资源类型不能为空"));
      int capacity = resource.path("capacity").asInt(0);
      if (capacity < 1) {
        throw new ApiException(HttpStatus.BAD_REQUEST, "FIELD_ERROR", "资源容量至少为 1");
      }
      boolean enabled = !resource.hasNonNull("enabled") || resource.path("enabled").asBoolean();
      int sortOrder =
          resource.hasNonNull("sortOrder")
              ? resource.path("sortOrder").asInt(-1)
              : defaultSortOrder;
      if (sortOrder < 0) {
        throw new ApiException(HttpStatus.BAD_REQUEST, "FIELD_ERROR", "资源排序不能小于 0");
      }
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("name", name);
      item.put("resourceType", resourceType);
      item.put("capacity", capacity);
      item.put("enabled", enabled);
      item.put("sortOrder", sortOrder);
      normalized.add(item);
      hasEnabledResource = hasEnabledResource || enabled;
      defaultSortOrder++;
    }
    if (!hasEnabledResource) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "RESOURCE_REQUIRED", "至少保留 1 个启用资源");
    }
    return normalized;
  }

  private String normalizeResourceType(String value) {
    String normalized = normalizeResourceText(value, "资源类型不能为空");
    if (CUSTOM_RESOURCE_TYPE_PLACEHOLDER.equals(normalized)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "FIELD_ERROR", "请填写具体的自定义资源类型");
    }
    return normalized;
  }

  private String normalizeResourceText(String value, String emptyMessage) {
    String normalized = value == null ? "" : value.trim();
    if (normalized.isEmpty()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "FIELD_ERROR", emptyMessage);
    }
    if (normalized.length() > MAX_RESOURCE_TEXT_LENGTH) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "FIELD_ERROR", "资源名称和类型不能超过 80 个字符");
    }
    return normalized;
  }

  private JsonNode json(Object databaseValue) {
    if (databaseValue == null) {
      return objectMapper.createObjectNode();
    }
    if (databaseValue instanceof JsonNode node) {
      return node;
    }
    if (databaseValue instanceof Map<?, ?> || databaseValue instanceof List<?>) {
      return objectMapper.valueToTree(databaseValue);
    }
    try {
      return objectMapper.readTree(databaseValue.toString());
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Database JSON is invalid", exception);
    }
  }

  private Object jsonValue(Object databaseValue) {
    return objectMapper.convertValue(json(databaseValue), Object.class);
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "提交内容无法处理", exception);
    }
  }

  private String summarizeBusinessHours(JsonNode businessHours) {
    JsonNode days = businessHours.path("days");
    int openDays = 0;
    String firstPeriod = "";
    for (JsonNode day : days) {
      if (day.path("open").asBoolean()) {
        openDays++;
        if (firstPeriod.isEmpty()) {
          firstPeriod = day.path("start").asText() + "-" + day.path("end").asText();
        }
      }
    }
    return openDays == 0 ? "未设置营业日" : "每周 " + openDays + " 天 · " + firstPeriod;
  }

  private void validateStatus(String status, List<String> allowed, String message) {
    if (!allowed.contains(status)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "FIELD_ERROR", message);
    }
  }

  private String requiredText(JsonNode node, String field, String message) {
    String value = node.path(field).asText("").trim();
    if (value.isEmpty()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "FIELD_ERROR", message);
    }
    return value;
  }

  private Long requireStoreId(Long tenantId) {
    Long storeId = ownerMapper.selectStoreId(tenantId);
    if (storeId == null) {
      throw new ApiException(HttpStatus.CONFLICT, "STORE_REQUIRED", "请先完成开店");
    }
    return storeId;
  }

  private Integer requireConfig(String configKey) {
    Integer value = ownerMapper.selectEnabledIntegerConfig(configKey);
    if (value == null) {
      throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "CONFIG_MISSING", "平台配置缺失");
    }
    return value;
  }

  private Map<String, Object> requireOne(Map<String, Object> row) {
    if (row == null || row.isEmpty()) {
      throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "记录不存在或不属于当前门店");
    }
    return row;
  }

  private void requireOwned(int count) {
    if (count != 1) {
      throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "记录不存在或不属于当前门店");
    }
  }

  private void validateSelectableIds(OwnedEntity entity, List<Long> ids, Long tenantId) {
    if (ids == null || ids.isEmpty()) {
      return;
    }
    for (Long id : ids) {
      int count =
          switch (entity) {
            case RESOURCE -> ownerMapper.countEnabledResource(tenantId, id);
            case STAFF -> ownerMapper.countActiveStaff(tenantId, id);
            case SERVICE -> ownerMapper.selectActiveService(tenantId, id) == null ? 0 : 1;
          };
      if (count != 1) {
        throw new ApiException(HttpStatus.CONFLICT, "FORM_ERROR", "所选服务、员工或资源已停用，请重新选择");
      }
    }
  }

  private String generateInviteCode() {
    StringBuilder code = new StringBuilder(INVITE_CODE_LENGTH);
    for (int index = 0; index < INVITE_CODE_LENGTH; index++) {
      code.append(INVITE_ALPHABET[secureRandom.nextInt(INVITE_ALPHABET.length)]);
    }
    return code.toString();
  }

  private void audit(
      Long tenantId,
      SessionPrincipal principal,
      String action,
      String target,
      String oldValue,
      String newValue,
      String reason) {
    ownerMapper.insertAudit(
        tenantId,
        principal.actorType(),
        principal.subjectId(),
        action,
        target,
        oldValue,
        newValue,
        reason);
  }

  private Long asLong(Object value) {
    return ((Number) value).longValue();
  }

  private Integer asInteger(Object value) {
    return value == null ? null : ((Number) value).intValue();
  }

  private enum DraftSection {
    STORE_PROFILE,
    BUSINESS_HOURS,
    RESOURCES
  }

  private enum OwnedEntity {
    RESOURCE,
    STAFF,
    SERVICE
  }

  private record StoreProfileData(
      String name,
      String nameKey,
      String cityCode,
      String city,
      String districtCode,
      String district,
      String detailAddress,
      String address,
      String contactPhone,
      List<String> serviceScopes) {}
}
