# 闲遇 MVP API 映射

文档日期：2026-07-21
状态：active architecture  
范围：完整 MVP

## 1. 总则

- 应用内部保持 `/auth`、`/owner`、`/staff`、`/member`、`/admin` 路径；Nginx 对外统一增加 `/api` 并在转发时剥离。
- 店长端、教练端、会员端、平台后台共用同一后端服务。
- OpenAPI `/v3/api-docs` 是唯一网络契约，前端类型由其生成。
- 所有响应统一为 `requestId/success/data/error`。
- 关键写操作必须提供 `Idempotency-Key`；同键不同请求返回冲突。
- 租户业务 API 的 `tenant_id` 来自登录上下文，不信任前端传值。
- 租户冻结后，店长、教练、会员端业务写操作全部阻断。
- 平台后台不得代门店创建会员、发卡、预约、到店或核销。

## 2. 统一响应

非分页成功：

```json
{
  "requestId": "req_20260607_8f3a91c2",
  "success": true,
  "data": {},
  "error": null
}
```

分页成功：

```json
{
  "requestId": "req_20260607_8f3a91c2",
  "success": true,
  "data": {
    "items": [],
    "total": 126,
    "page": 1,
    "pageSize": 20
  },
  "error": null
}
```

失败：

```json
{
  "requestId": "req_20260607_8f3a91c2",
  "success": false,
  "data": null,
  "error": {
    "code": "FIELD_ERROR",
    "message": "请检查表单内容",
    "fieldErrors": {
      "name": "会员姓名不能为空"
    }
  }
}
```

错误码：

| 错误码 | 说明 |
| --- | --- |
| `UNAUTHORIZED` | 未登录或登录过期。 |
| `FORBIDDEN` | 无权限。 |
| `TENANT_FROZEN` | 租户冻结。 |
| `FIELD_ERROR` | 字段错误。 |
| `FORM_ERROR` | 表单级错误。 |
| `NOT_FOUND` | 资源不存在。 |
| `CONFLICT` | 状态冲突。 |
| `REQUEST_DUPLICATED` | 重复提交。 |
| `INVITE_INVALID` | 邀请码无效。 |
| `CARD_UNAVAILABLE` | 会员卡不可用。 |
| `BOOKING_UNAVAILABLE` | 时段不可预约。 |
| `CANCEL_BLOCKED` | 不允许取消。 |

## 3. 认证 API

| 方法 | 路径 | 端 | 说明 |
| --- | --- | --- | --- |
| `POST` | `/auth/owner/wechat-login` | 店长 | 微信 code 登录，只换 openid；首次自动创建试用租户。 |
| `POST` | `/auth/member/wechat-login` | 会员 | 微信 code 登录，只换 openid。 |
| `POST` | `/auth/staff/login` | 教练 | 账号密码登录。 |
| `POST` | `/admin/auth/login` | 后台 | 后台账号密码登录。 |
| `POST` | `/auth/logout` | 四端 | 退出登录。 |
| `GET` | `/auth/session` | 四端 | 当前登录主体、角色、租户状态、下一页。 |

## 4. 店长端 API

### 4.1 工作台和我的

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/owner/dashboard` | 工作台创建后待办或运营态摘要。 |
| `GET` | `/owner/me` | 店长当前租户、门店、试用状态和客服微信。 |
| `GET` | `/owner/frozen` | 冻结页信息。 |

### 4.2 开店和门店资料

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/owner/onboarding/draft` | 获取开店草稿、完成状态、门店预览、新用户权益告示待确认状态和实际试用天数。 |
| `POST` | `/owner/onboarding/trial-notice/acknowledge` | 当前店长确认新用户权益告示；确认状态持久化到服务端且可重复调用。 |
| `GET` | `/owner/regions/cities` | 获取已启用城市字典。 |
| `GET` | `/owner/regions/cities/{cityCode}/districts` | 获取指定城市下已启用的区/县字典。 |
| `POST` | `/owner/store/name-availability` | 检查规范化后的门店名称是否已被其他已创建门店使用；草稿不占用名称。 |
| `PUT` | `/owner/onboarding/store-profile` | 保存门店资料草稿；提交城市代码、区/县代码和详细地址，由后端校验行政区划并合成展示地址。 |
| `PUT` | `/owner/onboarding/business-hours` | 保存营业时间草稿。 |
| `PUT` | `/owner/onboarding/resources` | 保存资源草稿。 |
| `POST` | `/owner/onboarding/complete` | 服务端最终校验并创建门店。 |
| `GET` | `/owner/store` | 查看门店资料。 |
| `PUT` | `/owner/store` | 修改门店资料和营业时间；门店名称全局唯一，联系电话不做格式或重复校验。 |

### 4.3 资源

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/owner/resources` | 资源列表。 |
| `POST` | `/owner/resources` | 新增资源。 |
| `PUT` | `/owner/resources/{resourceId}` | 修改资源。 |
| `DELETE` | `/owner/resources/{resourceId}` | 删除资源，先校验阻断原因。 |

### 4.4 员工

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/owner/staff` | 员工列表。 |
| `POST` | `/owner/staff` | 创建员工账号。 |
| `PUT` | `/owner/staff/{staffId}` | 修改员工账号资料。 |
| `PUT` | `/owner/staff/{staffId}/password` | 修改或重置密码。 |
| `PUT` | `/owner/staff/{staffId}/status` | 启用或停用。 |
| `DELETE` | `/owner/staff/{staffId}` | 物理删除，先校验服务、会员卡范围和未来排期。 |

### 4.5 服务项目

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/owner/services` | 服务项目列表。 |
| `POST` | `/owner/services` | 创建服务项目。 |
| `PUT` | `/owner/services/{serviceId}` | 修改服务项目。 |
| `PUT` | `/owner/services/{serviceId}/status` | 启用或停用。 |
| `GET` | `/owner/services/options` | 服务、资源、员工下拉选项。 |

### 4.6 会员卡

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/owner/card-templates` | 会员卡模板列表。 |
| `POST` | `/owner/card-templates` | 创建次数卡或期限卡。 |
| `PUT` | `/owner/card-templates/{templateId}` | 修改模板。 |
| `PUT` | `/owner/card-templates/{templateId}/status` | 启用或停用。 |

### 4.7 会员和发卡

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/owner/members` | 会员列表，支持绑定状态、低余额、到期筛选。 |
| `POST` | `/owner/members` | 创建会员。 |
| `GET` | `/owner/members/{memberId}` | 会员摘要。 |
| `PUT` | `/owner/members/{memberId}` | 修改会员资料。 |
| `POST` | `/owner/members/{memberId}/cards` | 发卡并写线下实收。 |
| `POST` | `/owner/members/{memberId}/invite-codes` | 重新生成邀请码。 |

发卡接口必须在同一事务内创建会员卡和线下售卡记录，不生成支付订单。

### 4.8 排期

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/owner/schedules` | 按日期查看排期首页。 |
| `POST` | `/owner/schedules/drafts` | 保存排期草稿。 |
| `POST` | `/owner/schedules/precheck` | 发布前预检。 |
| `POST` | `/owner/schedules/publish` | 发布时段。 |
| `POST` | `/owner/schedules/{slotId}/copy` | 复制时段。 |
| `PUT` | `/owner/schedules/{slotId}` | 修改时段。 |
| `PUT` | `/owner/schedules/{slotId}/status` | 停用、恢复或取消时段。 |

### 4.9 报表和预警

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/owner/reports/summary` | 销售额、预约、到店、核销和会员卡提醒摘要。 |
| `GET` | `/owner/reports/offline-sales` | 售卡明细。 |
| `GET` | `/owner/reports/bookings` | 预约、取消、候补、到店明细。 |
| `GET` | `/owner/reports/deductions` | 核销明细。 |
| `GET` | `/owner/reports/services` | 服务表现。 |
| `GET` | `/owner/warnings/cards` | 低余额、即将到期、已过期提醒。 |

## 5. 教练端 API

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/staff/account` | 账号状态和阻断信息。 |
| `GET` | `/staff/profile` | 个人资料和归属门店摘要。 |
| `PUT` | `/staff/profile` | 完善或修改文字资料。 |
| `GET` | `/staff/today` | 今日服务，含无服务空态信息。 |
| `GET` | `/staff/slots/{slotId}/roster` | 预约名单。 |
| `POST` | `/staff/attendance` | 到店确认。 |
| `POST` | `/staff/deductions` | 核销确认。 |
| `POST` | `/staff/service-notes` | 提交服务记录。 |
| `GET` | `/staff/records` | 自己的核销和服务记录。 |
| `GET` | `/staff/frozen` | 冻结提示。 |

核销前必须已有到店确认。过期、余额不足、未到店或租户冻结时返回阻断原因。

## 6. 会员端 API

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/member/invites/check` | 校验邀请码并返回门店、会员和可绑定权益，不绑定。 |
| `POST` | `/member/invites/bind` | 会员确认后绑定。 |
| `GET` | `/member/home` | 首页权益摘要、门店资料和最近核销。 |
| `GET` | `/member/cards` | 卡包。 |
| `GET` | `/member/services` | 约课首页筛选和服务时段。 |
| `GET` | `/member/services/{serviceId}/slots` | 服务详情和可用时段。 |
| `GET` | `/member/cards/available` | 某服务/时段可用卡。 |
| `POST` | `/member/bookings` | 确认预约。 |
| `POST` | `/member/waitlist` | 加入候补。 |
| `GET` | `/member/bookings` | 我的预约列表。 |
| `GET` | `/member/bookings/{bookingId}` | 预约详情。 |
| `POST` | `/member/bookings/{bookingId}/cancel-check` | 取消前校验。 |
| `POST` | `/member/bookings/{bookingId}/cancel` | 确认取消。 |
| `GET` | `/member/records` | 核销记录和服务记录。 |
| `GET` | `/member/me` | 会员资料和绑定门店摘要。 |
| `GET` | `/member/frozen` | 冻结提示和只读卡包摘要。 |

会员端无可用卡时只提示联系门店，不返回购卡或支付入口。

## 7. 平台后台 API

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/admin/tenants` | 租户列表，支持搜索、状态、到期筛选和分页。 |
| `GET` | `/admin/tenants/{tenantId}` | 租户详情、门店资料、业务摘要和试用状态。 |
| `POST` | `/admin/tenants/{tenantId}/freeze` | 冻结租户，必须填写原因并二次确认。 |
| `POST` | `/admin/tenants/{tenantId}/extend-trial` | 调整期限并解冻，作为冻结租户恢复入口。 |
| `GET` | `/admin/support-wechat` | 查看客服微信配置。 |
| `PUT` | `/admin/support-wechat` | 保存客服微信文本和展示文案。 |
| `GET` | `/admin/configs` | 平台配置列表。 |
| `PUT` | `/admin/configs/{configKey}` | 修改配置，必须填写原因。 |
| `GET` | `/admin/audit-logs` | 操作记录，支持租户、动作、时间筛选和分页。 |

后台无新建租户入口；租户只由店长首次微信登录自动开通。

## 8. 幂等与并发

以下接口必须支持重复提交保护：

- `/owner/onboarding/complete`
- `/owner/members/{memberId}/cards`
- `/owner/schedules/publish`
- `/member/bookings`
- `/member/waitlist`
- `/member/bookings/{bookingId}/cancel`
- `/staff/attendance`
- `/staff/deductions`
- `/admin/tenants/{tenantId}/freeze`
- `/admin/tenants/{tenantId}/extend-trial`
- `/admin/configs/{configKey}`

实现方式：

- 前端提交中禁用按钮。
- 请求携带幂等 key 或后端基于业务唯一约束防重。
- 预约、取消、核销等接口在事务内重新读取状态。
- 所有错误返回 `requestId`。

## 9. OpenAPI

后端工程必须生成 OpenAPI 文档。小程序端和后台前端的 DTO 类型应从 OpenAPI 或共享类型包生成/维护，避免四端字段漂移。
