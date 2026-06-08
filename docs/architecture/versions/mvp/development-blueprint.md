# 闲遇 MVP 开发落地设计

文档日期：2026-06-07  
状态：active architecture  
范围：完整 MVP 开发骨架与代码规范

## 1. 定位

本文件定义 MVP 从文档仓库进入应用代码仓库时的落地结构和代码规范。它不是执行计划，不拆 active 任务，不记录实施过程。

本文件只回答：

- 仓库代码目录怎么组织。
- 后端包结构怎么分。
- 店长、教练、会员三个小程序端怎么放。
- 平台后台前端怎么放。
- 共享类型、API SDK、design token 放哪里。
- 本地启动、构建、测试、迁移命令怎么规划。
- 第一版工程骨架一次性创建哪些目录。
- Java、TypeScript、Vue、Taro 的基础代码规范。

## 2. 仓库目录结构

应用代码进入仓库后，推荐采用单仓多应用结构：

```text
.
├── apps/
│   ├── api/
│   ├── admin-web/
│   ├── miniprogram-owner/
│   ├── miniprogram-staff/
│   └── miniprogram-member/
├── packages/
│   ├── shared-types/
│   ├── api-client/
│   ├── design-tokens/
│   ├── mini-components/
│   └── admin-components/
├── infra/
│   ├── docker/
│   └── compose/
├── scripts/
├── docs/
└── README.md
```

目录职责：

| 路径 | 职责 |
| --- | --- |
| `apps/api/` | Spring Boot 后端。 |
| `apps/admin-web/` | 平台后台 Vue 应用。 |
| `apps/miniprogram-owner/` | 店长小程序。 |
| `apps/miniprogram-staff/` | 教练小程序。 |
| `apps/miniprogram-member/` | 会员小程序。 |
| `packages/shared-types/` | 前后端共享 DTO、枚举、OpenAPI 生成类型。 |
| `packages/api-client/` | 小程序端和后台前端共享 API SDK。 |
| `packages/design-tokens/` | 颜色、字号、间距、圆角、状态 token。 |
| `packages/mini-components/` | 小程序业务组件。 |
| `packages/admin-components/` | 后台业务组件。 |
| `infra/compose/` | Docker Compose 文件和环境样例。 |
| `infra/docker/` | Dockerfile、Nginx 配置、容器脚本。 |
| `scripts/` | 本地开发、构建、检查辅助脚本。 |

## 3. 后端工程结构

后端为单体 Spring Boot 应用，根目录为 `apps/api/`。

```text
apps/api/
├── pom.xml
├── src/main/java/com/serenmeet/
│   ├── SerenMeetApplication.java
│   ├── auth/
│   ├── tenant/
│   ├── admin/
│   ├── audit/
│   ├── onboarding/
│   ├── store/
│   ├── resource/
│   ├── staff/
│   ├── service/
│   ├── card/
│   ├── member/
│   ├── sale/
│   ├── schedule/
│   ├── booking/
│   ├── fulfillment/
│   ├── report/
│   └── common/
├── src/main/resources/
│   ├── application.yml
│   ├── application-local.yml
│   └── db/migration/
└── src/test/java/com/serenmeet/
```

每个业务包采用统一分层：

```text
<domain>/
├── controller/
├── application/
├── domain/
├── mapper/
├── dto/
└── support/
```

分层职责：

| 分层 | 职责 |
| --- | --- |
| `controller` | HTTP 入口、参数校验、响应转换。 |
| `application` | 用例编排、事务边界、权限和租户校验。 |
| `domain` | 状态流转、业务规则、领域对象。 |
| `mapper` | MyBatis-Plus Mapper 和复杂 SQL。 |
| `dto` | Request、Response、Command、View 对象。 |
| `support` | 当前业务域内部辅助类，不外溢为公共工具。 |
| `common` | 统一响应、错误码、租户上下文、鉴权、审计、OpenAPI 配置。 |

命名规则：

- Controller：`OwnerMemberController`、`StaffFulfillmentController`、`AdminTenantController`。
- Application Service：`MemberCardApplicationService`。
- Domain Service：`BookingDomainService`。
- Mapper：`MemberCardMapper`。
- 请求 DTO：`CreateMemberRequest`。
- 响应 DTO：`MemberCardResponse`。
- 用例命令：`IssueMemberCardCommand`。
- 页面视图：`OwnerDashboardView`。

## 4. 小程序三端结构

三个小程序端独立应用，避免路由、权限和发布配置混在一起：

```text
apps/miniprogram-owner/
├── project.config.json
├── config/
├── src/
│   ├── app.ts
│   ├── app.config.ts
│   ├── pages/
│   ├── routes/
│   ├── services/
│   ├── stores/
│   └── styles/
└── tests/
```

`miniprogram-staff` 和 `miniprogram-member` 使用同样结构。

页面目录按原型 ID 分组：

- 店长端：`pages/dashboard/`、`pages/onboarding/`、`pages/resource/`、`pages/staff/`、`pages/service/`、`pages/card/`、`pages/member/`、`pages/schedule/`、`pages/report/`、`pages/mine/`、`pages/frozen/`。
- 教练端：`pages/today/`、`pages/roster/`、`pages/attendance/`、`pages/deduction/`、`pages/note/`、`pages/mine/`、`pages/frozen/`。
- 会员端：`pages/bind/`、`pages/home/`、`pages/cards/`、`pages/services/`、`pages/bookings/`、`pages/records/`、`pages/mine/`、`pages/frozen/`。

小程序端共享能力放在：

```text
packages/mini-components/
├── BusinessCard/
├── StatusTag/
├── EmptyState/
├── SubmitBar/
├── FormField/
├── ConfirmPanel/
└── FrozenNotice/
```

NutUI for Taro 只作为基础组件库。业务组件必须基于设计 token 封装，不直接散落使用默认皮肤。

## 5. 平台后台前端结构

后台前端根目录为 `apps/admin-web/`：

```text
apps/admin-web/
├── index.html
├── package.json
├── vite.config.ts
├── src/
│   ├── main.ts
│   ├── app.vue
│   ├── router/
│   ├── layouts/
│   ├── pages/
│   ├── services/
│   ├── stores/
│   ├── components/
│   └── styles/
└── tests/
```

页面目录：

- `pages/login/`
- `pages/tenants/`
- `pages/tenant-detail/`
- `pages/freeze/`
- `pages/extend/`
- `pages/support-wechat/`
- `pages/config/`
- `pages/audit/`

后台共享业务组件放在 `packages/admin-components/`：

- `AdminLayout`
- `FilterBar`
- `StatusTag`
- `MetricGrid`
- `ConfirmPanel`
- `AuditTable`
- `ConfigEditor`
- `TenantStateBlock`

Element Plus 只作为基础组件库。后台必须通过主题变量和业务组件封装落地当前 UI，不直接使用默认主题交付。

## 6. 共享类型、API SDK 和 design token

### 6.1 `packages/shared-types`

用于保存前端共享类型：

```text
packages/shared-types/
├── src/
│   ├── generated/
│   ├── enums/
│   ├── dto/
│   └── index.ts
└── package.json
```

规则：

- `generated/` 保存 OpenAPI 生成类型。
- `enums/` 保存前端需要稳定引用的枚举类型。
- 手写类型必须和后端 OpenAPI 保持一致。

### 6.2 `packages/api-client`

用于保存统一请求封装：

```text
packages/api-client/
├── src/
│   ├── http/
│   ├── owner/
│   ├── staff/
│   ├── member/
│   ├── admin/
│   └── index.ts
└── package.json
```

规则：

- 所有响应必须读取并保留 `requestId`。
- 统一处理登录过期、租户冻结、字段错误、表单错误和页面阻断。
- 小程序端和后台端可以使用不同 HTTP adapter，但共享接口类型和错误模型。

### 6.3 `packages/design-tokens`

用于保存设计 token：

```text
packages/design-tokens/
├── src/
│   ├── colors.ts
│   ├── spacing.ts
│   ├── radius.ts
│   ├── typography.ts
│   └── status.ts
└── package.json
```

规则：

- 小程序端生成可用的 SCSS/CSS 变量或 TypeScript token。
- 后台端生成 Element Plus theme override 和业务组件 token。
- token 必须覆盖主色、风险色、信息色、提示色、圆角、字号、间距和状态。

## 7. 代码规范

### 7.1 Java 注释规范

必须使用中文注释说明业务意图：

- public 类必须有中文 Javadoc，说明该类承担的业务职责。
- public 方法必须有中文 Javadoc，说明业务语义、关键前置条件和返回含义。
- 关键字段必须有中文注释，尤其是状态、金额、时间、租户隔离、外部 ID。
- 复杂规则块必须用短注释说明为什么这样处理。

不需要注释：

- 明显的 getter/setter。
- 和类名、方法名完全重复的机械描述。
- 无业务含义的临时变量。

### 7.2 Java 命名规范

- 包名使用小写业务域。
- 类名用业务语义，不使用 `Manager`、`Helper` 兜底命名。
- DTO 后缀固定为 `Request`、`Response`、`Command`、`View`。
- 状态枚举使用 `Enum` 后缀，例如 `TenantStatusEnum`。
- 金额字段明确单位，例如 `saleAmountYuan`。
- 时间字段明确语义，例如 `trialEndAt`、`validUntil`、`deductedAt`。

### 7.3 Lombok 使用规范

Lombok 是技术栈选择，具体允许范围以 `tech-stack.md` 为准。本文件只约束落地使用：

- DTO、Entity、配置对象可以使用 Lombok 减少样板代码。
- 业务服务类使用构造器注入，不手写无意义 setter。
- 领域规则方法必须显式命名，不用 Lombok 隐藏业务行为。
- 关键状态变更方法必须保留可读的业务方法名，例如 `freezeTenant`、`deductMemberCard`。

### 7.4 TypeScript 和 Vue/Taro 规范

- 组件使用 PascalCase。
- 页面目录使用 kebab-case。
- 组合式逻辑以 `use` 开头，例如 `useFrozenGuard`。
- API 方法按端和业务域命名，例如 `ownerMemberApi.issueCard`。
- 表单字段错误必须和字段绑定，不只弹 toast。
- 页面不得直接写散落颜色值，必须使用 design token。
- 小程序页面不得固定 390px 宽度。
- 后台宽表格必须处理横向滚动或关键列固定。

## 8. 本地命令规划

根目录保留统一命令入口，具体脚本在工程创建时落地。

| 命令 | 说明 |
| --- | --- |
| `./scripts/dev-api.sh` | 启动后端 API。 |
| `./scripts/dev-admin.sh` | 启动后台前端。 |
| `./scripts/dev-owner.sh` | 启动店长小程序开发构建。 |
| `./scripts/dev-staff.sh` | 启动教练小程序开发构建。 |
| `./scripts/dev-member.sh` | 启动会员小程序开发构建。 |
| `./scripts/compose-up.sh` | 启动本地 PostgreSQL、API、后台前端和反向代理。 |
| `./scripts/test-api.sh` | 后端测试。 |
| `./scripts/test-web.sh` | 前端测试和类型检查。 |
| `./scripts/migrate.sh` | 执行数据库迁移。 |
| `./scripts/openapi-generate.sh` | 生成 OpenAPI 类型和 API SDK 输入。 |

命令必须保持幂等，失败时输出明确错误原因。

## 9. 第一版工程骨架目录清单

第一版工程骨架只创建目录、基础配置、空应用入口和本地脚本，不实现业务功能。

必须一次性创建：

- `apps/api/`
- `apps/admin-web/`
- `apps/miniprogram-owner/`
- `apps/miniprogram-staff/`
- `apps/miniprogram-member/`
- `packages/shared-types/`
- `packages/api-client/`
- `packages/design-tokens/`
- `packages/mini-components/`
- `packages/admin-components/`
- `infra/compose/`
- `infra/docker/`
- `scripts/`

不得在骨架阶段创建：

- active 开发任务。
- 业务页面实现。
- 数据库业务迁移内容之外的临时 SQL。
- 线上支付、微信授权手机号、文件上传、会员自助购卡或员工自助注册相关目录。
