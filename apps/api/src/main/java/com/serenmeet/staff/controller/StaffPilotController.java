package com.serenmeet.staff.controller;

import com.serenmeet.auth.support.CurrentSession;
import com.serenmeet.auth.support.SessionPrincipal;
import com.serenmeet.common.ApiResponse;
import com.serenmeet.common.IdempotencyWorkflow;
import com.serenmeet.staff.application.StaffPilotApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/staff")
public class StaffPilotController {

  private final StaffPilotApplicationService staffService;
  private final IdempotencyWorkflow idempotency;

  public StaffPilotController(StaffPilotApplicationService staffService, IdempotencyWorkflow idempotency) {
    this.staffService = staffService;
    this.idempotency = idempotency;
  }

  @GetMapping("/account")
  public ApiResponse<Map<String, Object>> account(@CurrentSession SessionPrincipal principal) {
    return ApiResponse.ok(staffService.account(principal));
  }

  @GetMapping("/profile")
  public ApiResponse<Map<String, Object>> profile(@CurrentSession SessionPrincipal principal) {
    return ApiResponse.ok(staffService.profile(principal));
  }

  @PutMapping("/profile")
  public ApiResponse<Map<String, Object>> updateProfile(
    @CurrentSession SessionPrincipal principal,
    @Valid @RequestBody ProfileRequest request
  ) {
    return ApiResponse.ok(staffService.updateProfile(principal, request.displayName(), request.specialtyText(), request.introText()));
  }

  @GetMapping("/today")
  public ApiResponse<List<Map<String, Object>>> today(@CurrentSession SessionPrincipal principal) {
    return ApiResponse.ok(staffService.today(principal));
  }

  @GetMapping("/slots/{slotId}/roster")
  public ApiResponse<List<Map<String, Object>>> roster(
    @CurrentSession SessionPrincipal principal,
    @PathVariable Long slotId
  ) {
    return ApiResponse.ok(staffService.roster(principal, slotId));
  }

  @PostMapping("/attendance")
  public ApiResponse<Object> attendance(
    @CurrentSession SessionPrincipal principal,
    @RequestHeader("Idempotency-Key") String key,
    @Valid @RequestBody BookingActionRequest request
  ) {
    return ApiResponse.ok(idempotency.execute(
      principal,
      "staff.attendance." + request.bookingId(),
      key,
      request,
      () -> staffService.confirmAttendance(principal, request.bookingId())
    ));
  }

  @PostMapping("/deductions")
  public ApiResponse<Object> deduction(
    @CurrentSession SessionPrincipal principal,
    @RequestHeader("Idempotency-Key") String key,
    @Valid @RequestBody BookingActionRequest request
  ) {
    return ApiResponse.ok(idempotency.execute(
      principal,
      "staff.deduction." + request.bookingId(),
      key,
      request,
      () -> staffService.deduct(principal, request.bookingId())
    ));
  }

  @PostMapping("/service-notes")
  public ApiResponse<Object> note(
    @CurrentSession SessionPrincipal principal,
    @RequestHeader("Idempotency-Key") String key,
    @Valid @RequestBody ServiceNoteRequest request
  ) {
    return ApiResponse.ok(idempotency.execute(
      principal,
      "staff.service-note." + request.bookingId(),
      key,
      request,
      () -> staffService.saveNote(principal, request.bookingId(), request.staffNoteText(), request.memberFeedbackText())
    ));
  }

  @GetMapping("/records")
  public ApiResponse<List<Map<String, Object>>> records(@CurrentSession SessionPrincipal principal) {
    return ApiResponse.ok(staffService.records(principal));
  }

  @GetMapping("/frozen")
  public ApiResponse<Map<String, Object>> frozen(@CurrentSession SessionPrincipal principal) {
    return ApiResponse.ok(staffService.account(principal));
  }

  public record ProfileRequest(
    @NotBlank String displayName,
    @Size(max = 500) String specialtyText,
    @Size(max = 1000) String introText
  ) {
  }

  public record BookingActionRequest(@NotNull Long bookingId) {
  }

  public record ServiceNoteRequest(
    @NotNull Long bookingId,
    @Size(max = 1000) String staffNoteText,
    @Size(max = 1000) String memberFeedbackText
  ) {
  }
}
