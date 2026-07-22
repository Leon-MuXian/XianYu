package com.serenmeet.member.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/** 会员端绑定、卡包和预约持久化 Mapper。 */
public interface MemberPilotMapper {

    @InterceptorIgnore(tenantLine = "true")
    List<Map<String, Object>> lockActiveInvite(@Param("codeHash") String codeHash);

    @InterceptorIgnore(tenantLine = "true")
    Map<String, Object> selectInviteDetails(@Param("codeHash") String codeHash);

    @InterceptorIgnore(tenantLine = "true")
    String selectIdentityOpenidForUpdate(@Param("identityId") Long identityId);

    @InterceptorIgnore(tenantLine = "true")
    int markInviteUsed(@Param("inviteId") Long inviteId, @Param("openid") String openid);

    @InterceptorIgnore(tenantLine = "true")
    int bindIdentity(
            @Param("identityId") Long identityId,
            @Param("tenantId") Long tenantId,
            @Param("memberId") Long memberId);

    @InterceptorIgnore(tenantLine = "true")
    int markMemberBound(@Param("tenantId") Long tenantId, @Param("memberId") Long memberId);

    @InterceptorIgnore(tenantLine = "true")
    int attachTenantToSessions(@Param("tenantId") Long tenantId, @Param("subjectId") String subjectId);

    Map<String, Object> selectMemberRef(@Param("identityId") Long identityId);

    int countActiveCards(@Param("tenantId") Long tenantId, @Param("memberId") Long memberId);

    int countFutureBookings(@Param("tenantId") Long tenantId, @Param("memberId") Long memberId);

    Map<String, Object> selectMemberProfile(
            @Param("tenantId") Long tenantId, @Param("memberId") Long memberId);

    List<Map<String, Object>> selectCards(
            @Param("tenantId") Long tenantId, @Param("memberId") Long memberId);

    List<Map<String, Object>> selectServices(@Param("tenantId") Long tenantId);

    List<Map<String, Object>> selectAvailableCards(
            @Param("tenantId") Long tenantId,
            @Param("memberId") Long memberId,
            @Param("slotId") Long slotId);

    int reserveSlot(@Param("tenantId") Long tenantId, @Param("slotId") Long slotId);

    Long insertBooking(
            @Param("tenantId") Long tenantId,
            @Param("memberId") Long memberId,
            @Param("memberCardId") Long memberCardId,
            @Param("slotId") Long slotId);

    List<Map<String, Object>> selectBookings(
            @Param("tenantId") Long tenantId, @Param("memberId") Long memberId);

    List<Map<String, Object>> selectRecords(
            @Param("tenantId") Long tenantId, @Param("memberId") Long memberId);

    Map<String, Object> selectBookingDetail(
            @Param("tenantId") Long tenantId,
            @Param("memberId") Long memberId,
            @Param("bookingId") Long bookingId);
}
