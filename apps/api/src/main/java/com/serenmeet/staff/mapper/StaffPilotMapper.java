package com.serenmeet.staff.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/** 员工端到店、核销和服务记录持久化 Mapper。 */
public interface StaffPilotMapper {

    Map<String, Object> selectAccount(@Param("tenantId") Long tenantId, @Param("staffId") Long staffId);

    Map<String, Object> selectProfile(@Param("tenantId") Long tenantId, @Param("staffId") Long staffId);

    int updateProfile(
            @Param("tenantId") Long tenantId,
            @Param("staffId") Long staffId,
            @Param("displayName") String displayName,
            @Param("specialtyText") String specialtyText,
            @Param("introText") String introText);

    int finishFirstLogin(@Param("tenantId") Long tenantId, @Param("staffId") Long staffId);

    List<Map<String, Object>> selectToday(@Param("tenantId") Long tenantId, @Param("staffId") Long staffId);

    int countAssignedSlot(
            @Param("tenantId") Long tenantId,
            @Param("staffId") Long staffId,
            @Param("slotId") Long slotId);

    List<Map<String, Object>> selectRoster(
            @Param("tenantId") Long tenantId,
            @Param("staffId") Long staffId,
            @Param("slotId") Long slotId);

    Map<String, Object> selectBookingForAttendance(
            @Param("tenantId") Long tenantId,
            @Param("staffId") Long staffId,
            @Param("bookingId") Long bookingId);

    Map<String, Object> selectAttendance(
            @Param("tenantId") Long tenantId, @Param("bookingId") Long bookingId);

    Long insertAttendance(
            @Param("tenantId") Long tenantId,
            @Param("storeId") Long storeId,
            @Param("bookingId") Long bookingId,
            @Param("memberId") Long memberId,
            @Param("memberCardId") Long memberCardId,
            @Param("staffId") Long staffId);

    int markBookingArrived(@Param("tenantId") Long tenantId, @Param("bookingId") Long bookingId);

    Map<String, Object> selectDeductionContext(
            @Param("tenantId") Long tenantId,
            @Param("staffId") Long staffId,
            @Param("bookingId") Long bookingId);

    Map<String, Object> selectCardForDeductionForUpdate(
            @Param("tenantId") Long tenantId, @Param("memberCardId") Long memberCardId);

    int deductCardBalance(
            @Param("tenantId") Long tenantId,
            @Param("memberCardId") Long memberCardId,
            @Param("deductCount") int deductCount);

    Long insertDeduction(
            @Param("tenantId") Long tenantId,
            @Param("storeId") Long storeId,
            @Param("bookingId") Long bookingId,
            @Param("attendanceId") Long attendanceId,
            @Param("memberId") Long memberId,
            @Param("memberCardId") Long memberCardId,
            @Param("deductCount") int deductCount,
            @Param("beforeCount") Integer beforeCount,
            @Param("afterCount") Integer afterCount,
            @Param("staffId") Long staffId);

    int markBookingDeducted(@Param("tenantId") Long tenantId, @Param("bookingId") Long bookingId);

    Map<String, Object> selectBookingForNote(
            @Param("tenantId") Long tenantId,
            @Param("staffId") Long staffId,
            @Param("bookingId") Long bookingId);

    int upsertServiceNote(
            @Param("tenantId") Long tenantId,
            @Param("bookingId") Long bookingId,
            @Param("deductionId") Long deductionId,
            @Param("staffNote") String staffNote,
            @Param("memberFeedback") String memberFeedback);

    int markBookingNoteDone(@Param("tenantId") Long tenantId, @Param("bookingId") Long bookingId);

    List<Map<String, Object>> selectRecords(
            @Param("tenantId") Long tenantId, @Param("staffId") Long staffId);

    Map<String, Object> selectDeductionByBooking(
            @Param("tenantId") Long tenantId, @Param("bookingId") Long bookingId);
}
