# Architecture Index

文档日期：2026-06-07
当前产品：`seren-meet`  
当前版本：`mvp`

本目录承载工程映射，不覆盖产品范围。当前 MVP 工程映射是最终版完整设计，覆盖店长小程序、教练小程序、会员小程序和平台后台全部 MVP 业务。

## 读取顺序

1. `../product-specs/product.manifest.json`：确认 active 版本与当前工件。
2. `../product-specs/versions/mvp/spec.md`：理解产品范围、角色、实体和页面契约。
3. `../design-docs/versions/mvp/prototype.html`：理解当前 UI 原型。
4. `versions/mvp/tech-stack.md`：确认技术栈。
5. `versions/mvp/development-blueprint.md`：确认仓库结构、代码规范、本地命令和工程骨架目录。
6. `versions/mvp/system-design.md`：确认系统边界、模块和四端页面映射。
7. `versions/mvp/data-model.md`：确认数据库设计。
8. `versions/mvp/api-map.md`：确认四端 API 映射。
9. `versions/mvp/deployment.md`：确认部署方式。
10. `versions/mvp/implementation-status.md`：确认正式范围与实际代码状态。

## 当前状态

- `versions/mvp/` 目录为 MVP 工程映射位置。
- 旧工程设计内容已移除，不作为后续开发依据。
- 当前工程映射覆盖完整 MVP。
- 若工程文档与产品规格冲突，以 `../product-specs/versions/mvp/` 为准，并同步更新工程映射。

## 工程文档

| 类型 | 路径 | 说明 |
| --- | --- | --- |
| 技术栈 | `versions/mvp/tech-stack.md` | 最终技术选型、可落地性和扩展性。 |
| 开发落地设计 | `versions/mvp/development-blueprint.md` | 仓库结构、代码规范、本地命令和工程骨架目录。 |
| 系统设计 | `versions/mvp/system-design.md` | 系统边界、业务域、核心流程和四端页面映射。 |
| 数据库设计 | `versions/mvp/data-model.md` | 全局数据模型、表设计、索引、事务和约束。 |
| API 映射 | `versions/mvp/api-map.md` | 店长端、教练端、会员端、平台后台完整 API。 |
| 部署映射 | `versions/mvp/deployment.md` | 最终部署拓扑、环境、发布流程和运维边界。 |
| 实现状态 | `versions/mvp/implementation-status.md` | 四端、业务域、质量门禁和阶段门的实际状态。 |
