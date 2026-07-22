# 闲遇 seren-meet

## 项目介绍

闲遇是面向多业态门店的通用约课 SaaS。MVP 版本聚焦线下门店从建档、服务配置、资源配置、员工账号、会员卡售价、排期发布，到会员预约、员工到店确认、核销和经营报表的基础闭环。

当前产品适用于健身工作室、康复理疗、儿童体适能、咨询评估、场地预约等满足“服务项目 + 履约资源 + 员工 + 会员卡权益 + 可预约时段”模型的门店场景。

MVP 覆盖以下端与角色：

- 店长小程序：建店、配置服务/资源/员工/会员卡/会员/排期、录入线下售卡、查看报表、联系客服续期。
- 员工小程序：账号密码登录、查看自己的服务时段和名单、到店确认、核销、填写服务记录。
- 会员小程序：微信登录、邀请码绑定、查看卡包、筛选服务、预约、取消、候补、查看记录。
- 平台后台：租户试用、冻结/解冻、人工续期、客服微信配置。

当前阶段维护 MVP 版本设计与应用实现。开发、评审和验收从 `docs/product-specs/product.manifest.json` 和 `docs/product-specs/current.md` 进入，再读取产品、设计、架构和实现状态四类当前依据。

## 项目导航

### 产品设计指引

- [文档系统入口](docs/README.md)：顶层文档域、读取顺序和质量门禁。
- [产品规格入口](docs/product-specs/index.md)：产品文档总入口，说明当前版本范围和读取入口。
- [当前实现入口](docs/product-specs/current.md)：开发、评审和验收时优先读取的导航页。
- [MVP 产品设计](docs/product-specs/versions/mvp/spec.md)：当前唯一正式产品规格，包含定位、范围、角色权限、核心实体和业务故事。
- [MVP 需求清单](docs/product-specs/versions/mvp/requirements-list.md)：按功能模块拆解的需求列表。
- [MVP 验收标准](docs/product-specs/versions/mvp/acceptance-criteria.md)：用于开发完成后的验收依据。
- [MVP 实现说明](docs/product-specs/versions/mvp/implementation-notes.md)：实现时需要遵循的产品和工程说明。
- [MVP 原型说明](docs/product-specs/versions/mvp/prototype-notes.md)：UI 原型的页面、状态和交互说明。
- [当前 UI 设计稿](docs/design-docs/versions/mvp/prototype.html)：当前 MVP 对应的可视化原型。
- [当前设计说明](docs/design-docs/versions/mvp/notes.md)：UI 原型覆盖范围和设计边界。

### 工程实现指引

- [根工程入口](ARCHITECTURE.md)：仓库根目录的工程短入口。
- [MVP 工程入口](docs/architecture/index.md)：工程文档读取顺序和当前状态。
- [MVP 工程映射目录](docs/architecture/versions/mvp/README.md)：完整 MVP 技术栈、系统、数据、接口和部署设计。
- [实现状态矩阵](docs/architecture/versions/mvp/implementation-status.md)：正式范围与当前代码交付状态。

### 记录和规则

- [已完成任务](docs/exec-plans/completed/README.md)：仅存放已标记完成的任务。
- [产品决策](docs/decisions/index.md)：PDR 决策记录入口。
- [参考资料](docs/references/index.md)：文档标准和调研材料入口。
- [检查规则](docs/checks/doc-rules.md)：可机械校验的文档规则。

## 当前代码状态

- `apps/api/`：Spring Boot 模块化单体，阶段一统一认证、权益事实账本、最小预约与平台后台 API 已实现。
- `apps/admin-web/`：平台内部后台已实现并使用生成契约与共享客户端。
- `apps/miniprogram-owner/`、`apps/miniprogram-staff/`、`apps/miniprogram-member/`：阶段一试点链路已实现并可构建为微信小程序。
- `packages/`：共享类型、API 客户端、设计 token 和业务组件。
- `infra/compose/`：本地与试点容器编排。

根目录运行 `npm run check:docs` 检查文档入口、manifest、相对链接和原型覆盖。

当前阶段一技术基线已经通过本地隔离部署验证；真实 trial 仍需外部微信凭据、HTTPS 域名、试点门店和业务证据。达到 `docs/exec-plans/active/mvp-governance/trial-gate.md` 前不得启动阶段二。
