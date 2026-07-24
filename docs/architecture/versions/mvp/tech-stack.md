# 闲遇 MVP 技术栈

文档日期：2026-06-07  
状态：active architecture  
范围：完整 MVP

## 1. 最终选型

| 层 | 技术 | 结论 |
| --- | --- | --- |
| 后端 | Java 21、Spring Boot 3.x、Maven | 稳定、生态成熟，适合承载多租户业务 API、事务和后台管理。 |
| ORM | MyBatis-Plus | 支持 Mapper、分页、条件构造、自动填充和多租户插件；贴合 Java 后端开发习惯。 |
| Java 代码简化 | Lombok | 用于减少 DTO、Entity、配置对象的样板代码；业务规则仍以显式方法表达。 |
| 数据库 | PostgreSQL | 作为唯一业务事实来源，保存租户、门店、排期、预约、会员卡、核销、报表和审计。 |
| 小程序端 | Taro + Vue 3 + TypeScript | 三个小程序端共享组件、类型、请求层和业务状态，编译到微信小程序。 |
| 小程序 UI 组件库 | NutUI for Taro | 与 Taro Vue 3 模板配合自然，作为表单、弹层、导航、反馈等基础组件来源。 |
| 平台后台前端 | Vue 3 + TypeScript + Vite + Element Plus | 适合内部后台的表格、筛选、表单、弹窗、抽屉、分页和桌面响应式。 |
| 后台 UI 组件库 | Element Plus | 作为后台基础组件库，配合自定义主题和业务组件封装落地当前视觉。 |
| 数据库迁移 | Flyway | Maven 集成简单，迁移脚本随代码版本提交。 |
| API 文档 | OpenAPI | 后端生成接口契约，小程序端和后台前端据此维护 DTO 类型。 |
| 部署 | Docker + Docker Compose | 后端、后台前端、PostgreSQL 和反向代理统一容器化。 |

当前最终版不增加额外基础设施。登录态、幂等、限流和热点查询先由后端、数据库约束和应用内机制承担；若后续出现明确瓶颈，再作为新版本能力评审。

## 2. 后端可落地性

后端使用单体分层架构，不拆微服务：

- `controller`：四端 API 入口。
- `application`：用例编排、事务边界、权限校验。
- `domain`：状态流转、业务规则、领域服务。
- `mapper`：MyBatis-Plus Mapper 和复杂 SQL。
- `infrastructure`：微信登录、鉴权、审计、配置、OpenAPI、迁移。

Spring Boot 承载参数校验、统一响应、鉴权过滤器、事务、配置和健康检查。Java 21 满足 Spring Boot 3.x 的运行要求，并为后续长期维护保留 LTS 余量。

MyBatis-Plus 多租户插件作为租户行级隔离默认方案。所有租户内业务表必须包含 `tenant_id`，平台后台跨租户查询只能走后台 API 和显式白名单，不允许业务端绕过租户上下文。

Lombok 用于减少 Java 样板代码，但不得削弱业务可读性：

- DTO、Entity、配置对象、简单数据承载类允许使用 `@Getter`、`@Setter`、`@NoArgsConstructor`、`@AllArgsConstructor`、`@Builder`。
- 业务服务类优先使用 `@RequiredArgsConstructor` 做构造器注入。
- 限制滥用 `@Data`；涉及状态流转、金额、权限、租户上下文的对象不得只靠 `@Data` 暴露全部 setter。
- 领域对象的关键行为必须使用显式业务方法表达，例如 `freezeTenant`、`reserveSlot`、`deductMemberCard`。
- 领域规则、事务边界和状态变更不能依靠 Lombok 注解隐藏意图。

PostgreSQL 是唯一强一致数据源。以下动作必须使用数据库事务和行级锁或等价并发控制：

- 创建门店并落资源。
- 发卡并写入线下售卡记录。
- 预约并占用名额。
- 取消预约并释放名额。
- 到店确认。
- 核销并更新会员卡余额。
- 冻结、解冻和调整期限。
- 平台配置修改和审计写入。

## 3. 小程序端可落地性

Taro + Vue 3 + TypeScript 可以落地当前店长端、教练端和会员端 UI，但实现必须按小程序约束设计，而不是按 Web 页面直接移植：

- 使用小程序组件体系，不依赖浏览器 DOM。
- 以 `rpx`、百分比、Flex 和可迁移 Grid 思路处理响应式。
- 底部导航、底部操作区、抽屉和弹层必须适配安全区。
- 触控区域不小于 44px。
- 390px 只是设计稿基准，必须验证 320px、375px、390px、430px。
- 小程序端不得出现横向滚动。
- 门店名、服务名、会员卡名最多两行截断；详情页或展开态展示完整内容。
- 所有保存、发布、预约、取消、到店、核销等提交动作必须覆盖 loading、成功、字段错误、表单错误、页面阻断和禁用原因。

小程序 UI 组件库选择 NutUI for Taro：

- Taro 官方 Vue 3 文档提供 NutUI 模板入口，和当前 `Taro + Vue 3` 技术栈匹配。
- NutUI 用作基础组件库，覆盖按钮、表单、弹层、Toast、Loading、Tabs、列表、选择器等常用能力。
- 当前 UI 设计稿是业务工具风格，不能直接套组件库默认皮肤；必须用设计 token 覆盖颜色、圆角、字号、间距和状态。
- 复杂业务组件需要自建，例如开店清单、会员卡片、排期时段、预约名单、核销确认、冻结提示。
- 若未来放弃 Taro 改用微信小程序原生工程，可重新评估 TDesign Miniprogram；当前不作为主选，避免和 Taro Vue 组件体系混用增加复杂度。

三端建议在同一 Taro 工作区中按包或目录隔离：

```text
apps/miniprogram-owner
apps/miniprogram-staff
apps/miniprogram-member
packages/mini-components
packages/mini-api
packages/shared-types
```

这样可以共享设计 token、请求封装、错误处理和 DTO 类型，同时保持三端入口、路由和权限清晰。

## 4. 平台后台前端可落地性

Vue 3 + TypeScript + Vite + Element Plus 可以落地当前后台 UI。Element Plus 只作为基础组件库，不直接采用默认视觉：

- 用主题变量实现“企业经纬”视觉：深海军蓝侧栏、冷灰白工作区、商务蓝主操作、3-5px 容器圆角和紧凑表格。
- 自定义后台壳组件：侧栏、顶部栏、内容区、筛选栏、详情区、操作确认区。
- 表单页沿统一内容起始线左对齐；客服微信等字段较少的页面保持全宽业务面板，不单独居中收窄。
- 页面不使用渐变装饰、大圆角卡片、悬浮数据卡堆叠、开发说明式标签或营销页式大标题。
- 表格支持分页、筛选、加载、空态、错误态、横向滚动或关键列固定。
- 1024px、1280px、1440px、1920px 和宽表格必须验证。
- 窄桌面允许筛选换行或折叠次要筛选；主搜索、状态筛选和行操作必须可访问。
- 超宽屏内容区不无限拉宽，避免扫读困难。
- 弹窗、抽屉、表单错误、键盘焦点和确认操作必须符合后台可用性要求。

后台 UI 组件库选择 Element Plus：

- Element Plus 面向 Vue 3 和 TypeScript，覆盖后台需要的表格、分页、表单、日期、弹窗、抽屉、消息、加载态。
- 当前后台是内部运营工具，优先密度、可扫读、状态清晰和行操作效率。
- Element Plus 默认主题不能直接交付，需要封装 `AdminLayout`、`FilterBar`、`StatusTag`、`MetricGrid`、`ConfirmPanel`、`AuditTable` 等业务组件。
- 宽表格和筛选联动可以先用 Element Plus Table；若后续出现复杂列固定、虚拟滚动或超大数据量，再评估专用表格库。

后台建议独立工程：

```text
apps/admin-web
packages/shared-types
packages/admin-api
packages/design-tokens
```

## 5. 扩展性判断

该技术栈对后续扩展友好：

- 完整预约、候补、取消规则和报表下钻都可在现有 PostgreSQL + Spring Boot + MyBatis-Plus 下实现。
- Taro 三端可共享权益、预约、核销等业务组件和 API 类型。
- Vue 后台可以自然扩展更多平台运营模块。
- Docker Compose 可支撑本地、试点和早期生产；后续需要云平台、Kubernetes 或托管数据库时，可以平滑迁移。

不友好的边界也明确：

- 不适合一开始就做复杂营销、在线支付、集团多门店或高并发大规模 SaaS。
- 不适合把报表做成实时 BI 平台。
- 不适合把小程序端强行做成 H5/APP 多端统一发布。

这些都不在 MVP 当前范围内。
