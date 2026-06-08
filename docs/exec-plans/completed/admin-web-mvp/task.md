# 平台后台 WEB 端 MVP 开发任务

状态：completed
创建日期：2026-06-07
完成日期：2026-06-08
范围：`seren-meet` MVP 平台后台 WEB 端

## 原始目标

按当前 MVP 产品规格、UI 原型和工程映射，完成可运行、可验收的平台后台 WEB 端。后台用于平台内部运营人员查看自动开通的租户、冻结/解冻、调整期限、配置客服微信、维护平台配置和查看操作记录。

## 输入依据

- `AGENTS.md`
- `docs/product-specs/product.manifest.json`
- `docs/product-specs/current.md`
- `docs/product-specs/versions/mvp/spec.md`
- `docs/product-specs/versions/mvp/requirements-list.md`
- `docs/product-specs/versions/mvp/acceptance-criteria.md`
- `docs/design-docs/versions/mvp/prototype.html`
- `docs/design-docs/versions/mvp/notes.md`
- `docs/DESIGN.md`
- `docs/FRONTEND.md`
- `docs/architecture/versions/mvp/tech-stack.md`
- `docs/architecture/versions/mvp/development-blueprint.md`
- `docs/architecture/versions/mvp/system-design.md`
- `docs/architecture/versions/mvp/api-map.md`

## 已完成范围

- 完成 `apps/admin-web/` Vue 3 + TypeScript + Vite 平台后台应用。
- 完成后台登录、租户列表、租户详情、冻结、延期并解冻、客服微信配置、平台配置和操作记录页面。
- 完成 `apps/api/` Spring Boot 平台后台 API，包括登录会话、租户查询、冻结、延期、客服微信、平台配置和审计记录。
- 完成 PostgreSQL + Flyway 初始化基线，初始化数据只保留 `admin@serenmeet` 默认账号、平台配置和默认客服微信。
- 完成 `packages/design-tokens/` 和 `packages/admin-components/` 基础入口。
- 完成本地开发脚本、Docker Compose PostgreSQL、前后端 Dockerfile 和子工程 README。

## 验证方式

- `mvn test`
- `npm run build`，执行目录为 `apps/admin-web/`
- `rg --files apps/api/src/main/resources/db/migration`，确认迁移目录只保留一个最终 SQL 文件。

## 收尾说明

- `docs/exec-plans/active/admin-web-mvp/` 已清理。
- 本记录只说明任务完成状态；正式产品、设计和工程要求仍以 active MVP 版本包、UI 原型和架构文档为准。
