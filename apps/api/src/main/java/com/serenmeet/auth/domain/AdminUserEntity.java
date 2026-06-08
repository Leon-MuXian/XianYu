package com.serenmeet.auth.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 平台后台账号实体。
 */
@Getter
@Setter
@TableName("admin_user")
public class AdminUserEntity {

  /** 后台账号主键。 */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 后台登录账号，通常为内部邮箱。 */
  private String username;

  /** PBKDF2 密码哈希，不保存明文密码。 */
  private String passwordHash;

  /** 后台操作人展示名称。 */
  private String displayName;

  /** 账号是否启用，停用后禁止登录。 */
  private Boolean enabled;

  /** 账号创建时间。 */
  private LocalDateTime createdAt;
}
