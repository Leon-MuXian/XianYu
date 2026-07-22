package com.serenmeet.auth.application;

import com.serenmeet.auth.domain.AdminSessionEntity;
import com.serenmeet.auth.dto.BusinessLoginResponse;
import com.serenmeet.auth.dto.SessionView;
import com.serenmeet.auth.dto.StaffLoginRequest;
import com.serenmeet.auth.mapper.AdminSessionMapper;
import com.serenmeet.auth.mapper.BusinessAuthMapper;
import com.serenmeet.auth.support.PasswordHasher;
import com.serenmeet.auth.support.SessionPrincipal;
import com.serenmeet.auth.support.TokenHasher;
import com.serenmeet.auth.support.WechatCodeSessionClient;
import com.serenmeet.common.ApiException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 业务端统一登录、会话校验和登录后路由编排服务。 */
@Service
public class BusinessAuthApplicationService {

    private static final String OWNER_ACTOR = "owner";
    private static final String MEMBER_ACTOR = "member";
    private static final String STAFF_ACTOR = "staff";
    private static final String ADMIN_ACTOR = "admin";
    private static final String DEFAULT_STORE_NAME = "待开通门店";
    private static final String TRIAL_DAYS_CONFIG = "tenant.trial.default_days";
    private static final int SESSION_VALID_HOURS = 12;

    private final BusinessAuthMapper businessAuthMapper;
    private final AdminSessionMapper sessionMapper;
    private final WechatCodeSessionClient wechatClient;
    private final PasswordHasher passwordHasher;
    private final TokenHasher tokenHasher;
    private final Clock clock;

    public BusinessAuthApplicationService(
            BusinessAuthMapper businessAuthMapper,
            AdminSessionMapper sessionMapper,
            WechatCodeSessionClient wechatClient,
            PasswordHasher passwordHasher,
            TokenHasher tokenHasher,
            Clock clock) {
        this.businessAuthMapper = businessAuthMapper;
        this.sessionMapper = sessionMapper;
        this.wechatClient = wechatClient;
        this.passwordHasher = passwordHasher;
        this.tokenHasher = tokenHasher;
        this.clock = clock;
    }

    @Transactional
    public BusinessLoginResponse ownerWechatLogin(String code) {
        String openid = wechatClient.exchange(OWNER_ACTOR, code);
        OwnerIdentity identity = findOwner(openid);
        if (identity == null) {
            identity = createOwner(openid);
        } else {
            businessAuthMapper.touchOwnerIdentity(identity.id());
        }
        return createSession(OWNER_ACTOR, identity.id().toString(), identity.tenantId());
    }

    @Transactional
    public BusinessLoginResponse memberWechatLogin(String code) {
        String openid = wechatClient.exchange(MEMBER_ACTOR, code);
        MemberIdentity identity = findMember(openid);
        if (identity == null) {
            identity = new MemberIdentity(businessAuthMapper.insertMemberIdentity(openid), null);
        } else {
            businessAuthMapper.touchMemberIdentity(identity.id());
        }
        return createSession(MEMBER_ACTOR, identity.id().toString(), identity.tenantId());
    }

    @Transactional
    public BusinessLoginResponse staffLogin(StaffLoginRequest request) {
        StaffCredential credential = findStaffCredential(request.loginName());
        if (credential == null || !passwordHasher.matches(request.password(), credential.passwordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "账号或密码错误");
        }
        if (!"active".equals(credential.status())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "STAFF_DISABLED", "员工账号已停用");
        }
        return createSession(STAFF_ACTOR, credential.id().toString(), credential.tenantId());
    }

    public SessionPrincipal requirePrincipal(String authorizationHeader) {
        String token = requireToken(authorizationHeader);
        Map<String, Object> row = businessAuthMapper.selectSessionPrincipal(tokenHasher.hash(token));
        if (row == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "请先登录或重新登录");
        }
        Object tenantId = row.get("tenantId");
        SessionPrincipal principal = new SessionPrincipal(
                row.get("actorType").toString(),
                row.get("subjectId").toString(),
                tenantId == null ? null : asLong(tenantId),
                row.get("tenantStatus") == null ? null : row.get("tenantStatus").toString(),
                Boolean.TRUE.equals(row.get("bound")),
                Boolean.TRUE.equals(row.get("firstLogin")));
        if (STAFF_ACTOR.equals(principal.actorType()) && !isActiveStaff(principal.subjectId())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "员工账号已停用或登录已过期");
        }
        return principal;
    }

    public SessionView session(String authorizationHeader) {
        return toView(requirePrincipal(authorizationHeader));
    }

    @Transactional
    public void logout(String authorizationHeader) {
        sessionMapper.deleteById(tokenHasher.hash(requireToken(authorizationHeader)));
    }

    public String requireToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "请先登录或重新登录");
        }
        String token = authorizationHeader.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "请先登录或重新登录");
        }
        return token;
    }

    private OwnerIdentity createOwner(String openid) {
        Integer trialDays = businessAuthMapper.selectEnabledIntegerConfig(TRIAL_DAYS_CONFIG);
        if (trialDays == null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "TRIAL_CONFIG_MISSING", "新租户试用配置缺失");
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        Long tenantId = businessAuthMapper.insertTenant(DEFAULT_STORE_NAME, now, now.plusDays(trialDays));
        Long identityId = businessAuthMapper.insertOwnerIdentity(tenantId, openid);
        businessAuthMapper.insertOnboardingDraft(tenantId, openid);
        return new OwnerIdentity(identityId, tenantId);
    }

    private OwnerIdentity findOwner(String openid) {
        Map<String, Object> row = businessAuthMapper.selectOwnerIdentity(openid);
        if (row == null) {
            return null;
        }
        return new OwnerIdentity(asLong(row.get("id")), asLong(row.get("tenantId")));
    }

    private MemberIdentity findMember(String openid) {
        Map<String, Object> row = businessAuthMapper.selectMemberIdentity(openid);
        if (row == null) {
            return null;
        }
        Object tenantId = row.get("tenantId");
        return new MemberIdentity(asLong(row.get("id")), tenantId == null ? null : asLong(tenantId));
    }

    private StaffCredential findStaffCredential(String loginName) {
        Map<String, Object> row = businessAuthMapper.selectStaffCredential(loginName);
        if (row == null) {
            return null;
        }
        return new StaffCredential(
                asLong(row.get("id")),
                asLong(row.get("tenantId")),
                row.get("passwordHash").toString(),
                row.get("status").toString());
    }

    private BusinessLoginResponse createSession(String actorType, String subjectId, Long tenantId) {
        String rawToken = tokenHasher.generateToken();
        OffsetDateTime now = OffsetDateTime.now(clock);
        OffsetDateTime expiresAt = now.plusHours(SESSION_VALID_HOURS);
        AdminSessionEntity session = new AdminSessionEntity();
        session.setTokenHash(tokenHasher.hash(rawToken));
        session.setActorType(actorType);
        session.setSubjectId(subjectId);
        session.setTenantId(tenantId);
        session.setCreatedAt(now);
        session.setExpiresAt(expiresAt);
        sessionMapper.insert(session);
        SessionPrincipal principal = requirePrincipal("Bearer " + rawToken);
        return new BusinessLoginResponse(rawToken, expiresAt, toView(principal));
    }

    private boolean isActiveStaff(String staffId) {
        return businessAuthMapper.countActiveStaff(Long.valueOf(staffId)) == 1;
    }

    private SessionView toView(SessionPrincipal principal) {
        String nextPath = switch (principal.actorType()) {
            case OWNER_ACTOR -> hasStore(principal.tenantId()) ? "/owner/dashboard" : "/owner/onboarding";
            case MEMBER_ACTOR -> principal.bound() ? "/member/services" : "/member/bind";
            case STAFF_ACTOR -> principal.firstLogin() ? "/staff/profile" : "/staff/today";
            case ADMIN_ACTOR -> "/admin/tenants";
            default -> "/auth/login";
        };
        if ("frozen".equals(principal.tenantStatus()) && !ADMIN_ACTOR.equals(principal.actorType())) {
            nextPath = "/" + principal.actorType() + "/frozen";
        }
        return new SessionView(
                principal.actorType(),
                principal.subjectId(),
                principal.tenantId(),
                principal.tenantStatus(),
                principal.bound(),
                principal.firstLogin(),
                nextPath);
    }

    private boolean hasStore(Long tenantId) {
        return tenantId != null && businessAuthMapper.countStore(tenantId) > 0;
    }

    private Long asLong(Object value) {
        return ((Number) value).longValue();
    }

    private record OwnerIdentity(Long id, Long tenantId) {
    }

    private record MemberIdentity(Long id, Long tenantId) {
    }

    private record StaffCredential(Long id, Long tenantId, String passwordHash, String status) {
    }
}
