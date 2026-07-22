package com.serenmeet.auth.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import java.time.OffsetDateTime;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/**
 * 业务端认证持久化 Mapper。
 *
 * <p>认证查询发生在租户上下文建立之前，因此涉及租户的数据必须在 SQL 中显式携带租户或主体条件。
 */
@InterceptorIgnore(tenantLine = "true")
public interface BusinessAuthMapper {

    Map<String, Object> selectOwnerIdentity(@Param("openid") String openid);

    Map<String, Object> selectMemberIdentity(@Param("openid") String openid);

    Map<String, Object> selectStaffCredential(@Param("loginName") String loginName);

    Map<String, Object> selectSessionPrincipal(@Param("tokenHash") String tokenHash);

    Integer selectEnabledIntegerConfig(@Param("configKey") String configKey);

    Long insertTenant(
            @Param("name") String name,
            @Param("trialStartAt") OffsetDateTime trialStartAt,
            @Param("trialEndAt") OffsetDateTime trialEndAt);

    Long insertOwnerIdentity(@Param("tenantId") Long tenantId, @Param("openid") String openid);

    Long insertMemberIdentity(@Param("openid") String openid);

    int insertOnboardingDraft(@Param("tenantId") Long tenantId, @Param("openid") String openid);

    int touchOwnerIdentity(@Param("identityId") Long identityId);

    int touchMemberIdentity(@Param("identityId") Long identityId);

    int countActiveStaff(@Param("staffId") Long staffId);

    int countStore(@Param("tenantId") Long tenantId);
}
