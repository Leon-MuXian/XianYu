package com.serenmeet.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.serenmeet.auth.domain.AdminSessionEntity;
import com.serenmeet.auth.dto.AdminUserView;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Param;

/**
 * 平台后台会话 Mapper。
 */
public interface AdminSessionMapper extends BaseMapper<AdminSessionEntity> {

  AdminUserView findUserByToken(@Param("token") String token);

  int deleteExpiredSessions(@Param("now") LocalDateTime now);
}
