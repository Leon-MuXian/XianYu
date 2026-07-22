package com.serenmeet.staff.application;

import com.serenmeet.auth.support.SessionPrincipal;
import com.serenmeet.common.ApiException;
import com.serenmeet.staff.mapper.StaffPilotMapper;
import java.sql.Date;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 员工到店确认、核销和服务记录的业务编排服务。 */
@Service
public class StaffPilotApplicationService {

    private final StaffPilotMapper staffMapper;
    private final Clock clock;

    public StaffPilotApplicationService(StaffPilotMapper staffMapper, Clock clock) {
        this.staffMapper = staffMapper;
        this.clock = clock;
    }

    public Map<String, Object> account(SessionPrincipal principal) {
        return requireOne(staffMapper.selectAccount(principal.tenantId(), staffId(principal)));
    }

    public Map<String, Object> profile(SessionPrincipal principal) {
        return requireOne(staffMapper.selectProfile(principal.tenantId(), staffId(principal)));
    }

    @Transactional
    public Map<String, Object> updateProfile(
            SessionPrincipal principal, String displayName, String specialtyText, String introText) {
        int updated = staffMapper.updateProfile(
                principal.tenantId(),
                staffId(principal),
                displayName,
                specialtyText == null ? "" : specialtyText,
                introText == null ? "" : introText);
        if (updated != 1) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "员工资料不存在");
        }
        staffMapper.finishFirstLogin(principal.tenantId(), staffId(principal));
        return profile(principal);
    }

    public List<Map<String, Object>> today(SessionPrincipal principal) {
        return staffMapper.selectToday(principal.tenantId(), staffId(principal));
    }

    public List<Map<String, Object>> roster(SessionPrincipal principal, Long slotId) {
        ensureAssignedSlot(principal, slotId);
        return staffMapper.selectRoster(principal.tenantId(), staffId(principal), slotId);
    }

    @Transactional
    public Map<String, Object> confirmAttendance(SessionPrincipal principal, Long bookingId) {
        Long tenantId = principal.tenantId();
        Map<String, Object> booking = requireOne(
                staffMapper.selectBookingForAttendance(tenantId, staffId(principal), bookingId));
        String status = booking.get("status").toString();
        if (List.of("arrived", "deducted", "note_done").contains(status)) {
            return requireOne(staffMapper.selectAttendance(tenantId, bookingId));
        }
        if (!"reserved".equals(status)) {
            throw new ApiException(HttpStatus.CONFLICT, "CONFLICT", "当前预约状态不能确认到店");
        }
        Long attendanceId = staffMapper.insertAttendance(
                tenantId,
                asLong(booking.get("storeId")),
                bookingId,
                asLong(booking.get("memberId")),
                asLong(booking.get("memberCardId")),
                staffId(principal));
        staffMapper.markBookingArrived(tenantId, bookingId);
        return Map.of("attendanceId", attendanceId, "bookingId", bookingId, "status", "arrived");
    }

    @Transactional
    public Map<String, Object> deduct(SessionPrincipal principal, Long bookingId) {
        Long tenantId = principal.tenantId();
        Map<String, Object> row = requireOne(
                staffMapper.selectDeductionContext(tenantId, staffId(principal), bookingId));
        String bookingStatus = row.get("status").toString();
        if (List.of("deducted", "note_done").contains(bookingStatus)) {
            return deductionByBooking(tenantId, bookingId);
        }
        if (!"arrived".equals(bookingStatus) || row.get("attendanceId") == null) {
            throw new ApiException(HttpStatus.CONFLICT, "DEDUCTION_BLOCKED", "请先确认会员到店");
        }
        Map<String, Object> card = requireOne(staffMapper.selectCardForDeductionForUpdate(
                tenantId, asLong(row.get("memberCardId"))));
        row.putAll(card);
        validateCard(row);
        int deductCount = asInteger(row.get("deductCount"));
        Integer beforeCount = asNullableInteger(row.get("remainCount"));
        Integer afterCount = deductCountCard(tenantId, row, deductCount, beforeCount);
        Long deductionId = insertDeduction(principal, bookingId, row, deductCount, beforeCount, afterCount);
        staffMapper.markBookingDeducted(tenantId, bookingId);
        return deductionResponse(deductionId, bookingId, deductCount, beforeCount, afterCount);
    }

    @Transactional
    public Map<String, Object> saveNote(
            SessionPrincipal principal, Long bookingId, String staffNote, String memberFeedback) {
        Long tenantId = principal.tenantId();
        Map<String, Object> booking = requireOne(
                staffMapper.selectBookingForNote(tenantId, staffId(principal), bookingId));
        boolean noteBlocked = !List.of("deducted", "note_done").contains(booking.get("status"))
                || booking.get("deductionId") == null;
        if (noteBlocked) {
            throw new ApiException(HttpStatus.CONFLICT, "CONFLICT", "核销后才能提交服务记录");
        }
        staffMapper.upsertServiceNote(
                tenantId,
                bookingId,
                asLong(booking.get("deductionId")),
                staffNote,
                memberFeedback);
        staffMapper.markBookingNoteDone(tenantId, bookingId);
        return Map.of("bookingId", bookingId, "status", "note_done");
    }

    public List<Map<String, Object>> records(SessionPrincipal principal) {
        return staffMapper.selectRecords(principal.tenantId(), staffId(principal));
    }

    private void validateCard(Map<String, Object> row) {
        LocalDate today = LocalDate.now(clock);
        LocalDate validFrom = asLocalDate(row.get("validFrom"));
        LocalDate validUntil = asLocalDate(row.get("validUntil"));
        boolean unavailable = !"active".equals(row.get("baseStatus"))
                || today.isBefore(validFrom)
                || today.isAfter(validUntil);
        if (unavailable) {
            throw new ApiException(HttpStatus.CONFLICT, "CARD_UNAVAILABLE", "会员卡已停用或不在有效期");
        }
    }

    private Integer deductCountCard(
            Long tenantId, Map<String, Object> row, int deductCount, Integer beforeCount) {
        if (!"count".equals(row.get("cardType"))) {
            return null;
        }
        if (beforeCount == null || beforeCount < deductCount) {
            throw new ApiException(HttpStatus.CONFLICT, "CARD_UNAVAILABLE", "会员卡余额不足");
        }
        int updated = staffMapper.deductCardBalance(
                tenantId, asLong(row.get("memberCardId")), deductCount);
        if (updated != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "CARD_UNAVAILABLE", "会员卡余额不足");
        }
        return beforeCount - deductCount;
    }

    private Long insertDeduction(
            SessionPrincipal principal,
            Long bookingId,
            Map<String, Object> row,
            int deductCount,
            Integer beforeCount,
            Integer afterCount) {
        try {
            return staffMapper.insertDeduction(
                    principal.tenantId(),
                    asLong(row.get("storeId")),
                    bookingId,
                    asLong(row.get("attendanceId")),
                    asLong(row.get("memberId")),
                    asLong(row.get("memberCardId")),
                    deductCount,
                    beforeCount,
                    afterCount,
                    staffId(principal));
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "REQUEST_DUPLICATED", "该预约已经核销");
        }
    }

    private Map<String, Object> deductionResponse(
            Long deductionId,
            Long bookingId,
            int deductCount,
            Integer beforeCount,
            Integer afterCount) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("deductionId", deductionId);
        response.put("bookingId", bookingId);
        response.put("deductCount", deductCount);
        response.put("beforeCount", beforeCount);
        response.put("afterCount", afterCount);
        response.put("status", "deducted");
        return response;
    }

    private Map<String, Object> deductionByBooking(Long tenantId, Long bookingId) {
        return requireOne(staffMapper.selectDeductionByBooking(tenantId, bookingId));
    }

    private void ensureAssignedSlot(SessionPrincipal principal, Long slotId) {
        int count = staffMapper.countAssignedSlot(principal.tenantId(), staffId(principal), slotId);
        if (count != 1) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "记录不存在或不在当前员工的服务范围");
        }
    }

    private Map<String, Object> requireOne(Map<String, Object> row) {
        if (row == null || row.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "记录不存在或不在当前员工的服务范围");
        }
        return row;
    }

    private Long staffId(SessionPrincipal principal) {
        return Long.valueOf(principal.subjectId());
    }

    private Long asLong(Object value) {
        return ((Number) value).longValue();
    }

    private int asInteger(Object value) {
        return ((Number) value).intValue();
    }

    private Integer asNullableInteger(Object value) {
        return value == null ? null : asInteger(value);
    }

    private LocalDate asLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        return ((Date) value).toLocalDate();
    }
}
