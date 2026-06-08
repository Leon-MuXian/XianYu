package com.serenmeet.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.serenmeet.audit.domain.AuditLogEntity;
import com.serenmeet.audit.dto.AuditLogItem;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 平台操作审计 Mapper。
 */
public interface AuditLogMapper extends BaseMapper<AuditLogEntity> {

  List<AuditLogItem> selectAuditLogs(
    @Param("keyword") String keyword,
    @Param("action") String action,
    @Param("range") String range,
    @Param("pageSize") int pageSize,
    @Param("offset") int offset
  );

  long countAuditLogs(@Param("keyword") String keyword, @Param("action") String action, @Param("range") String range);
}
