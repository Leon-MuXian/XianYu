package com.serenmeet.owner.dto;

/** 仅供所属店长按员工读取的登录凭据，不允许进入列表或写请求响应。 */
public record StaffCredentialResponse(
    Long staffId, String staffName, String loginName, String loginPassword) {}
