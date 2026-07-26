package com.serenmeet.owner.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/** 店长端试点业务持久化 Mapper。 */
public interface OwnerPilotMapper {

  Map<String, Object> selectOwnerProfile(@Param("tenantId") Long tenantId);

  Map<String, Object> selectDashboard(@Param("tenantId") Long tenantId);

  Map<String, Object> selectOnboardingDraft(
      @Param("tenantId") Long tenantId, @Param("ownerIdentityId") Long ownerIdentityId);

  Map<String, Object> selectOnboardingDraftForUpdate(@Param("tenantId") Long tenantId);

  List<Map<String, Object>> selectStores(@Param("tenantId") Long tenantId);

  Long selectStoreId(@Param("tenantId") Long tenantId);

  Map<String, Object> selectStoreDetail(@Param("tenantId") Long tenantId);

  List<Map<String, Object>> selectAdministrativeCities();

  List<Map<String, Object>> selectAdministrativeDistricts(@Param("cityCode") String cityCode);

  Map<String, Object> selectAdministrativeRegion(
      @Param("cityCode") String cityCode, @Param("districtCode") String districtCode);

  @InterceptorIgnore(tenantLine = "true")
  int countStoreNameKeyExcludingTenant(
      @Param("nameKey") String nameKey, @Param("tenantId") Long tenantId);

  Long insertStore(
      @Param("tenantId") Long tenantId,
      @Param("name") String name,
      @Param("nameKey") String nameKey,
      @Param("serviceScopes") String serviceScopes,
      @Param("cityCode") String cityCode,
      @Param("districtCode") String districtCode,
      @Param("detailAddress") String detailAddress,
      @Param("address") String address,
      @Param("contactPhone") String contactPhone,
      @Param("businessHours") String businessHours);

  int updateTenantProfile(
      @Param("tenantId") Long tenantId, @Param("name") String name, @Param("city") String city);

  int updateStoreProfile(
      @Param("tenantId") Long tenantId,
      @Param("name") String name,
      @Param("nameKey") String nameKey,
      @Param("serviceScopes") String serviceScopes,
      @Param("cityCode") String cityCode,
      @Param("districtCode") String districtCode,
      @Param("detailAddress") String detailAddress,
      @Param("address") String address,
      @Param("contactPhone") String contactPhone);

  int updateStoreBusinessHours(
      @Param("tenantId") Long tenantId, @Param("businessHours") String businessHours);

  int markOnboardingConverted(@Param("tenantId") Long tenantId);

  int acknowledgeTrialNotice(
      @Param("tenantId") Long tenantId, @Param("ownerIdentityId") Long ownerIdentityId);

  int updateStoreProfileDraft(@Param("tenantId") Long tenantId, @Param("payload") String payload);

  int updateBusinessHoursDraft(@Param("tenantId") Long tenantId, @Param("payload") String payload);

  int updateResourcesDraft(@Param("tenantId") Long tenantId, @Param("payload") String payload);

  List<Map<String, Object>> selectResources(@Param("tenantId") Long tenantId);

  Long insertResource(
      @Param("tenantId") Long tenantId,
      @Param("storeId") Long storeId,
      @Param("name") String name,
      @Param("resourceType") String resourceType,
      @Param("capacity") int capacity,
      @Param("enabled") boolean enabled,
      @Param("sortOrder") int sortOrder);

  int updateResource(
      @Param("tenantId") Long tenantId,
      @Param("resourceId") Long resourceId,
      @Param("name") String name,
      @Param("resourceType") String resourceType,
      @Param("capacity") int capacity,
      @Param("enabled") boolean enabled,
      @Param("sortOrder") int sortOrder);

  int countEnabledResourcesExcluding(
      @Param("tenantId") Long tenantId, @Param("resourceId") Long resourceId);

  int countResourceServiceBindings(
      @Param("tenantId") Long tenantId, @Param("resourceId") Long resourceId);

  int countResourceFutureSlots(
      @Param("tenantId") Long tenantId, @Param("resourceId") Long resourceId);

  int deleteResource(@Param("tenantId") Long tenantId, @Param("resourceId") Long resourceId);

  List<Map<String, Object>> selectStaff(@Param("tenantId") Long tenantId);

  Map<String, Object> selectStaffCredential(
      @Param("tenantId") Long tenantId, @Param("staffId") Long staffId);

  @InterceptorIgnore(tenantLine = "true")
  List<String> selectStaffCredentialKeyIds();

  @InterceptorIgnore(tenantLine = "true")
  List<Map<String, Object>> selectStaffCredentialValidationSamples();

  @InterceptorIgnore(tenantLine = "true")
  int countStaffLoginName(
      @Param("loginName") String loginName, @Param("excludeStaffId") Long excludeStaffId);

  Long insertStaffAccount(
      @Param("tenantId") Long tenantId,
      @Param("storeId") Long storeId,
      @Param("loginName") String loginName,
      @Param("passwordHash") String passwordHash,
      @Param("staffName") String staffName,
      @Param("roleLabel") String roleLabel);

  int insertStaffProfile(
      @Param("tenantId") Long tenantId,
      @Param("staffId") Long staffId,
      @Param("displayName") String displayName);

  int insertStaffCredentialSecret(
      @Param("tenantId") Long tenantId,
      @Param("staffId") Long staffId,
      @Param("keyId") String keyId,
      @Param("cipherVersion") String cipherVersion,
      @Param("nonce") byte[] nonce,
      @Param("ciphertext") byte[] ciphertext);

  int updateStaffAccount(
      @Param("tenantId") Long tenantId,
      @Param("staffId") Long staffId,
      @Param("loginName") String loginName,
      @Param("staffName") String staffName,
      @Param("roleLabel") String roleLabel);

  int updateStaffProfileName(
      @Param("tenantId") Long tenantId,
      @Param("staffId") Long staffId,
      @Param("displayName") String displayName);

  int updateStaffPassword(
      @Param("tenantId") Long tenantId,
      @Param("staffId") Long staffId,
      @Param("passwordHash") String passwordHash);

  int upsertStaffCredentialSecret(
      @Param("tenantId") Long tenantId,
      @Param("staffId") Long staffId,
      @Param("keyId") String keyId,
      @Param("cipherVersion") String cipherVersion,
      @Param("nonce") byte[] nonce,
      @Param("ciphertext") byte[] ciphertext);

  int revokeStaffSessions(
      @Param("tenantId") Long tenantId, @Param("staffId") Long staffId);

  int updateStaffStatus(
      @Param("tenantId") Long tenantId,
      @Param("staffId") Long staffId,
      @Param("status") String status);

  int countStaffCardScopes(@Param("tenantId") Long tenantId, @Param("staffId") Long staffId);

  int countStaffServiceBindings(@Param("tenantId") Long tenantId, @Param("staffId") Long staffId);

  int countStaffFutureSlots(@Param("tenantId") Long tenantId, @Param("staffId") Long staffId);

  int deleteStaff(@Param("tenantId") Long tenantId, @Param("staffId") Long staffId);

  List<Map<String, Object>> selectServices(@Param("tenantId") Long tenantId);

  int countServiceNameKey(
      @Param("tenantId") Long tenantId,
      @Param("nameKey") String nameKey,
      @Param("excludeServiceId") Long excludeServiceId);

  Long insertService(
      @Param("tenantId") Long tenantId,
      @Param("storeId") Long storeId,
      @Param("name") String name,
      @Param("serviceType") String serviceType,
      @Param("durationMin") int durationMin,
      @Param("capacity") int capacity,
      @Param("deductCount") int deductCount,
      @Param("status") String status);

  int insertServiceResourceBinding(
      @Param("tenantId") Long tenantId,
      @Param("serviceId") Long serviceId,
      @Param("resourceId") Long resourceId);

  int insertStaffServiceBinding(
      @Param("tenantId") Long tenantId,
      @Param("staffId") Long staffId,
      @Param("serviceId") Long serviceId);

  List<Long> selectServiceResourceIds(
      @Param("tenantId") Long tenantId, @Param("serviceId") Long serviceId);

  List<Long> selectServiceStaffIds(
      @Param("tenantId") Long tenantId, @Param("serviceId") Long serviceId);

  int updateService(
      @Param("tenantId") Long tenantId,
      @Param("serviceId") Long serviceId,
      @Param("name") String name,
      @Param("serviceType") String serviceType,
      @Param("durationMin") int durationMin,
      @Param("capacity") int capacity,
      @Param("deductCount") int deductCount,
      @Param("status") String status);

  int updateServiceStatus(
      @Param("tenantId") Long tenantId,
      @Param("serviceId") Long serviceId,
      @Param("status") String status);

  Map<String, Object> selectServiceForUpdate(
      @Param("tenantId") Long tenantId, @Param("serviceId") Long serviceId);

  int countServiceCardScopes(
      @Param("tenantId") Long tenantId, @Param("serviceId") Long serviceId);

  int countServiceSlots(
      @Param("tenantId") Long tenantId, @Param("serviceId") Long serviceId);

  int deleteService(@Param("tenantId") Long tenantId, @Param("serviceId") Long serviceId);

  int deleteServiceResourceBindings(
      @Param("tenantId") Long tenantId, @Param("serviceId") Long serviceId);

  int deleteStaffServiceBindings(
      @Param("tenantId") Long tenantId, @Param("serviceId") Long serviceId);

  List<Map<String, Object>> selectCardTemplates(@Param("tenantId") Long tenantId);

  int countCardTemplateNameKey(
      @Param("tenantId") Long tenantId,
      @Param("nameKey") String nameKey,
      @Param("excludeTemplateId") Long excludeTemplateId);

  int countOwnedCardTemplate(
      @Param("tenantId") Long tenantId, @Param("templateId") Long templateId);

  Long insertCardTemplate(
      @Param("tenantId") Long tenantId,
      @Param("storeId") Long storeId,
      @Param("name") String name,
      @Param("cardType") String cardType,
      @Param("salePrice") BigDecimal salePrice,
      @Param("totalCount") Integer totalCount,
      @Param("validDays") int validDays,
      @Param("lowBalanceThreshold") Integer lowBalanceThreshold);

  int insertCardServiceScope(
      @Param("tenantId") Long tenantId,
      @Param("cardTemplateId") Long cardTemplateId,
      @Param("serviceId") Long serviceId);

  int insertCardStaffScope(
      @Param("tenantId") Long tenantId,
      @Param("cardTemplateId") Long cardTemplateId,
      @Param("staffId") Long staffId);

  List<Long> selectCardServiceIds(
      @Param("tenantId") Long tenantId, @Param("templateId") Long templateId);

  List<Long> selectCardStaffIds(
      @Param("tenantId") Long tenantId, @Param("templateId") Long templateId);

  int updateCardTemplate(
      @Param("tenantId") Long tenantId,
      @Param("templateId") Long templateId,
      @Param("name") String name,
      @Param("cardType") String cardType,
      @Param("salePrice") BigDecimal salePrice,
      @Param("totalCount") Integer totalCount,
      @Param("validDays") int validDays,
      @Param("lowBalanceThreshold") Integer lowBalanceThreshold);

  int updateCardTemplateStatus(
      @Param("tenantId") Long tenantId,
      @Param("templateId") Long templateId,
      @Param("status") String status);

  int deleteCardServiceScopes(
      @Param("tenantId") Long tenantId, @Param("templateId") Long templateId);

  int deleteCardStaffScopes(@Param("tenantId") Long tenantId, @Param("templateId") Long templateId);

  List<Map<String, Object>> selectMembers(@Param("tenantId") Long tenantId);

  Long insertMember(
      @Param("tenantId") Long tenantId,
      @Param("storeId") Long storeId,
      @Param("name") String name,
      @Param("memberNo") String memberNo,
      @Param("contactText") String contactText);

  Map<String, Object> selectOwnedMember(
      @Param("tenantId") Long tenantId, @Param("memberId") Long memberId);

  Map<String, Object> selectActiveCardTemplate(
      @Param("tenantId") Long tenantId, @Param("templateId") Long templateId);

  Long insertMemberCard(
      @Param("tenantId") Long tenantId,
      @Param("storeId") Long storeId,
      @Param("memberId") Long memberId,
      @Param("templateId") Long templateId,
      @Param("cardType") String cardType,
      @Param("salePrice") BigDecimal salePrice,
      @Param("remainCount") Integer remainCount,
      @Param("validFrom") LocalDate validFrom,
      @Param("validUntil") LocalDate validUntil);

  Long insertOfflineSale(
      @Param("tenantId") Long tenantId,
      @Param("storeId") Long storeId,
      @Param("memberId") Long memberId,
      @Param("memberCardId") Long memberCardId,
      @Param("saleAmount") BigDecimal saleAmount,
      @Param("saleDate") LocalDate saleDate,
      @Param("payMethodLabel") String payMethodLabel,
      @Param("operatorId") String operatorId);

  Integer selectEnabledIntegerConfig(@Param("configKey") String configKey);

  int revokeActiveInvites(@Param("tenantId") Long tenantId, @Param("memberId") Long memberId);

  int insertInvite(
      @Param("tenantId") Long tenantId,
      @Param("storeId") Long storeId,
      @Param("memberId") Long memberId,
      @Param("codeHash") String codeHash,
      @Param("displayCodeTail") String displayCodeTail,
      @Param("expiresAt") OffsetDateTime expiresAt);

  List<Map<String, Object>> selectSchedules(
      @Param("tenantId") Long tenantId, @Param("scheduleDate") LocalDate scheduleDate);

  Map<String, Object> selectActiveService(
      @Param("tenantId") Long tenantId, @Param("serviceId") Long serviceId);

  int countActiveStaff(@Param("tenantId") Long tenantId, @Param("staffId") Long staffId);

  int countEnabledResource(@Param("tenantId") Long tenantId, @Param("resourceId") Long resourceId);

  int countStaffServiceBinding(
      @Param("tenantId") Long tenantId,
      @Param("serviceId") Long serviceId,
      @Param("staffId") Long staffId);

  int countServiceResourceBinding(
      @Param("tenantId") Long tenantId,
      @Param("serviceId") Long serviceId,
      @Param("resourceId") Long resourceId);

  Long insertBookableSlot(
      @Param("tenantId") Long tenantId,
      @Param("storeId") Long storeId,
      @Param("serviceId") Long serviceId,
      @Param("staffId") Long staffId,
      @Param("resourceId") Long resourceId,
      @Param("startAt") OffsetDateTime startAt,
      @Param("endAt") OffsetDateTime endAt,
      @Param("capacity") int capacity,
      @Param("status") String status,
      @Param("cancelDeadlineMin") int cancelDeadlineMin);

  Map<String, Object> selectReportSummary(@Param("tenantId") Long tenantId);

  List<Map<String, Object>> selectCardWarnings(@Param("tenantId") Long tenantId);

  List<Map<String, Object>> selectOfflineSales(@Param("tenantId") Long tenantId);

  List<Map<String, Object>> selectBookingReport(@Param("tenantId") Long tenantId);

  List<Map<String, Object>> selectDeductionReport(@Param("tenantId") Long tenantId);

  List<Map<String, Object>> selectServiceReport(@Param("tenantId") Long tenantId);

  int countOwnedResource(@Param("tenantId") Long tenantId, @Param("id") Long id);

  int countOwnedStaff(@Param("tenantId") Long tenantId, @Param("id") Long id);

  int countOwnedService(@Param("tenantId") Long tenantId, @Param("id") Long id);

  int insertAudit(
      @Param("tenantId") Long tenantId,
      @Param("actorType") String actorType,
      @Param("actorName") String actorName,
      @Param("action") String action,
      @Param("targetName") String targetName,
      @Param("oldValue") String oldValue,
      @Param("newValue") String newValue,
      @Param("reason") String reason);
}
