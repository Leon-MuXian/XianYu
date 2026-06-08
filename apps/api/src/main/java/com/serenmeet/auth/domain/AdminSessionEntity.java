package com.serenmeet.auth.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 平台后台登录会话实体。
 */
@Getter
@Setter
@TableName("admin_session")
public class AdminSessionEntity {

  /** 会话令牌，前端以 Bearer token 形式携带。 */
  @TableId
  private String token;

  /** 对应后台账号 ID。 */
  private Long adminUserId;

  /** 会话创建时间。 */
  private LocalDateTime createdAt;

  /** 会话过期时间，过期后必须重新登录。 */
  private LocalDateTime expiresAt;
}
