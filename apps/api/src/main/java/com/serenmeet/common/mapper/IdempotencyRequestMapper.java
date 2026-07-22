package com.serenmeet.common.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import java.time.OffsetDateTime;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/** 幂等请求占位、结果保存和释放 Mapper。 */
@InterceptorIgnore(tenantLine = "true")
public interface IdempotencyRequestMapper {

    int insertClaim(
            @Param("actorType") String actorType,
            @Param("actorId") String actorId,
            @Param("operation") String operation,
            @Param("idempotencyKey") String idempotencyKey,
            @Param("requestHash") String requestHash,
            @Param("expiresAt") OffsetDateTime expiresAt);

    Map<String, Object> selectClaim(
            @Param("actorType") String actorType,
            @Param("actorId") String actorId,
            @Param("operation") String operation,
            @Param("idempotencyKey") String idempotencyKey);

    int completeClaim(
            @Param("actorType") String actorType,
            @Param("actorId") String actorId,
            @Param("operation") String operation,
            @Param("idempotencyKey") String idempotencyKey,
            @Param("responseBody") String responseBody);

    int releaseClaim(
            @Param("actorType") String actorType,
            @Param("actorId") String actorId,
            @Param("operation") String operation,
            @Param("idempotencyKey") String idempotencyKey);
}
