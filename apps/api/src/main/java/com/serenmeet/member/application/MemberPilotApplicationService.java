package com.serenmeet.member.application;

import com.serenmeet.auth.support.SessionPrincipal;
import com.serenmeet.auth.support.TokenHasher;
import com.serenmeet.common.ApiException;
import com.serenmeet.member.mapper.MemberPilotMapper;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 会员绑定、卡包查询和最小预约链路的业务编排服务。 */
@Service
public class MemberPilotApplicationService {

    private final MemberPilotMapper memberMapper;
    private final TokenHasher tokenHasher;

    public MemberPilotApplicationService(MemberPilotMapper memberMapper, TokenHasher tokenHasher) {
        this.memberMapper = memberMapper;
        this.tokenHasher = tokenHasher;
    }

    public Map<String, Object> checkInvite(SessionPrincipal principal, String code) {
        return inviteDetails(principal, code);
    }

    @Transactional
    public Map<String, Object> bindInvite(SessionPrincipal principal, String code) {
        String codeHash = tokenHasher.hash(normalizeCode(code));
        if (memberMapper.lockActiveInvite(codeHash).isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVITE_INVALID", "邀请码无效或已过期");
        }
        Map<String, Object> invite = inviteDetails(principal, code);
        Long identityId = Long.valueOf(principal.subjectId());
        Long tenantId = asLong(invite.get("tenantId"));
        Long memberId = asLong(invite.get("memberId"));
        String openid = memberMapper.selectIdentityOpenidForUpdate(identityId);
        if (openid == null) {
            throw new ApiException(HttpStatus.CONFLICT, "CONFLICT", "当前微信身份不存在");
        }
        int inviteUpdated = memberMapper.markInviteUsed(asLong(invite.get("inviteId")), openid);
        if (inviteUpdated != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "INVITE_INVALID", "邀请码已失效，请联系门店重新生成");
        }
        int identityUpdated = memberMapper.bindIdentity(identityId, tenantId, memberId);
        if (identityUpdated != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "CONFLICT", "当前微信已绑定会员");
        }
        memberMapper.markMemberBound(tenantId, memberId);
        memberMapper.attachTenantToSessions(tenantId, principal.subjectId());
        return Map.of(
                "tenantId", tenantId,
                "memberId", memberId,
                "bound", true,
                "nextPath", "/member/services");
    }

    public Map<String, Object> home(SessionPrincipal principal) {
        MemberRef member = requireMember(principal);
        Map<String, Object> profile = me(principal);
        profile.put("activeCardCount", memberMapper.countActiveCards(member.tenantId(), member.memberId()));
        profile.put("futureBookingCount", memberMapper.countFutureBookings(member.tenantId(), member.memberId()));
        return profile;
    }

    public Map<String, Object> me(SessionPrincipal principal) {
        MemberRef member = requireMember(principal);
        return requireOne(memberMapper.selectMemberProfile(member.tenantId(), member.memberId()));
    }

    public List<Map<String, Object>> cards(SessionPrincipal principal) {
        MemberRef member = requireMember(principal);
        return memberMapper.selectCards(member.tenantId(), member.memberId());
    }

    public List<Map<String, Object>> services(SessionPrincipal principal) {
        MemberRef member = requireMember(principal);
        return memberMapper.selectServices(member.tenantId());
    }

    public List<Map<String, Object>> availableCards(SessionPrincipal principal, Long slotId) {
        MemberRef member = requireMember(principal);
        return memberMapper.selectAvailableCards(member.tenantId(), member.memberId(), slotId);
    }

    @Transactional
    public Map<String, Object> createBooking(SessionPrincipal principal, Long slotId, Long memberCardId) {
        MemberRef member = requireMember(principal);
        List<Map<String, Object>> cards = memberMapper.selectAvailableCards(
                member.tenantId(), member.memberId(), slotId);
        boolean cardAvailable = cards.stream()
                .anyMatch(card -> asLong(card.get("id")).equals(memberCardId));
        if (!cardAvailable) {
            throw new ApiException(HttpStatus.CONFLICT, "CARD_UNAVAILABLE", "会员卡不可用于该时段");
        }
        int capacityUpdated = memberMapper.reserveSlot(member.tenantId(), slotId);
        if (capacityUpdated != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "BOOKING_UNAVAILABLE", "该时段名额已满或不可预约");
        }
        try {
            Long bookingId = memberMapper.insertBooking(
                    member.tenantId(), member.memberId(), memberCardId, slotId);
            Map<String, Object> booking = bookingDetail(member, bookingId);
            booking.put("status", "reserved");
            return booking;
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "REQUEST_DUPLICATED", "已经预约过该时段");
        }
    }

    public List<Map<String, Object>> bookings(SessionPrincipal principal) {
        MemberRef member = requireMember(principal);
        return memberMapper.selectBookings(member.tenantId(), member.memberId());
    }

    public List<Map<String, Object>> records(SessionPrincipal principal) {
        MemberRef member = requireMember(principal);
        return memberMapper.selectRecords(member.tenantId(), member.memberId());
    }

    private Map<String, Object> inviteDetails(SessionPrincipal principal, String code) {
        if (principal.bound()) {
            throw new ApiException(HttpStatus.CONFLICT, "CONFLICT", "当前微信已绑定会员");
        }
        Map<String, Object> invite = memberMapper.selectInviteDetails(tokenHasher.hash(normalizeCode(code)));
        if (invite == null || invite.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVITE_INVALID", "邀请码无效或已过期");
        }
        return invite;
    }

    private MemberRef requireMember(SessionPrincipal principal) {
        Map<String, Object> row = memberMapper.selectMemberRef(Long.valueOf(principal.subjectId()));
        if (row == null || row.isEmpty()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "MEMBER_NOT_BOUND", "请先使用邀请码绑定会员");
        }
        return new MemberRef(asLong(row.get("tenantId")), asLong(row.get("memberId")));
    }

    private Map<String, Object> bookingDetail(MemberRef member, Long bookingId) {
        return requireOne(memberMapper.selectBookingDetail(member.tenantId(), member.memberId(), bookingId));
    }

    private Map<String, Object> requireOne(Map<String, Object> row) {
        if (row == null || row.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "记录不存在");
        }
        return row;
    }

    private String normalizeCode(String code) {
        return code == null ? "" : code.trim().toUpperCase();
    }

    private Long asLong(Object value) {
        return ((Number) value).longValue();
    }

    private record MemberRef(Long tenantId, Long memberId) {
    }
}
