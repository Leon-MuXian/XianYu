package com.serenmeet.member.controller;

import com.serenmeet.auth.support.CurrentSession;
import com.serenmeet.auth.support.SessionPrincipal;
import com.serenmeet.common.ApiResponse;
import com.serenmeet.common.IdempotencyWorkflow;
import com.serenmeet.member.application.MemberPilotApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/member")
public class MemberPilotController {

  private final MemberPilotApplicationService memberService;
  private final IdempotencyWorkflow idempotency;

  public MemberPilotController(MemberPilotApplicationService memberService, IdempotencyWorkflow idempotency) {
    this.memberService = memberService;
    this.idempotency = idempotency;
  }

  @PostMapping("/invites/check")
  public ApiResponse<Map<String, Object>> checkInvite(
    @CurrentSession SessionPrincipal principal,
    @Valid @RequestBody InviteRequest request
  ) {
    return ApiResponse.ok(memberService.checkInvite(principal, request.code()));
  }

  @PostMapping("/invites/bind")
  public ApiResponse<Object> bindInvite(
    @CurrentSession SessionPrincipal principal,
    @RequestHeader("Idempotency-Key") String key,
    @Valid @RequestBody InviteRequest request
  ) {
    return ApiResponse.ok(idempotency.execute(
      principal, "member.invite.bind", key, request, () -> memberService.bindInvite(principal, request.code())
    ));
  }

  @GetMapping("/home")
  public ApiResponse<Map<String, Object>> home(@CurrentSession SessionPrincipal principal) {
    return ApiResponse.ok(memberService.home(principal));
  }

  @GetMapping("/me")
  public ApiResponse<Map<String, Object>> me(@CurrentSession SessionPrincipal principal) {
    return ApiResponse.ok(memberService.me(principal));
  }

  @GetMapping("/frozen")
  public ApiResponse<Map<String, Object>> frozen(@CurrentSession SessionPrincipal principal) {
    return ApiResponse.ok(memberService.me(principal));
  }

  @GetMapping("/cards")
  public ApiResponse<List<Map<String, Object>>> cards(@CurrentSession SessionPrincipal principal) {
    return ApiResponse.ok(memberService.cards(principal));
  }

  @GetMapping("/services")
  public ApiResponse<List<Map<String, Object>>> services(@CurrentSession SessionPrincipal principal) {
    return ApiResponse.ok(memberService.services(principal));
  }

  @GetMapping("/cards/available")
  public ApiResponse<List<Map<String, Object>>> availableCards(
    @CurrentSession SessionPrincipal principal,
    @RequestParam Long slotId
  ) {
    return ApiResponse.ok(memberService.availableCards(principal, slotId));
  }

  @PostMapping("/bookings")
  public ApiResponse<Object> createBooking(
    @CurrentSession SessionPrincipal principal,
    @RequestHeader("Idempotency-Key") String key,
    @Valid @RequestBody BookingRequest request
  ) {
    return ApiResponse.ok(idempotency.execute(
      principal,
      "member.booking.create",
      key,
      request,
      () -> memberService.createBooking(principal, request.slotId(), request.memberCardId())
    ));
  }

  @GetMapping("/bookings")
  public ApiResponse<List<Map<String, Object>>> bookings(@CurrentSession SessionPrincipal principal) {
    return ApiResponse.ok(memberService.bookings(principal));
  }

  @GetMapping("/records")
  public ApiResponse<List<Map<String, Object>>> records(@CurrentSession SessionPrincipal principal) {
    return ApiResponse.ok(memberService.records(principal));
  }

  public record InviteRequest(@NotBlank String code) {
  }

  public record BookingRequest(@NotNull Long slotId, @NotNull Long memberCardId) {
  }
}
