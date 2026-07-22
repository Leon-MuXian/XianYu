package com.serenmeet.auth.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 平台后台登录会话实体。
 */
@Getter
@Setter
@TableName("auth_session")
public class AdminSessionEntity {

  /** 原始令牌的 SHA-256，不保存客户端持有的明文令牌。 */
  @TableId
  private String tokenHash;

  /** 会话角色：admin、owner、staff、member。 */
  private String actorType;

  /** 登录主体 ID。 */
  private String subjectId;

  /** 租户 ID；平台管理员为空。 */
  private Long tenantId;

  /** 会话创建时间。 */
  private OffsetDateTime createdAt;

  /** 会话过期时间，过期后必须重新登录。 */
  private OffsetDateTime expiresAt;

  /** 主动退出或安全撤销时间。 */
  private OffsetDateTime revokedAt;
}
