# Document Rules

文档日期：2026-06-06

## 必备结构

- 仓库根目录应包含 `AGENTS.md`，作为 Agent 读取当前产品设计的短入口。
- 仓库根目录应包含 `README.md` 和 `ARCHITECTURE.md`。
- `docs/` 必须包含 `README.md`、`../DESIGN.md`、`../FRONTEND.md`、`../PLANS.md`、`../PRODUCT_SENSE.md`、`../QUALITY_SCORE.md`、`../RELIABILITY.md`、`../SECURITY.md`。
- `docs/product-specs/` 只承载产品规格、版本入口和 manifest，不承载设计稿、工程映射、执行记录、决策、调研或检查规则。
- `docs/product-specs/` 必须包含 `../product-specs/index.md`、`../product-specs/current.md`、`../product-specs/product.manifest.json` 和 `../product-specs/versions/`。
- active 版本包必须包含 `../product-specs/versions/mvp/version.manifest.json`、`README.md`、`../product-specs/versions/mvp/spec.md`、`../product-specs/versions/mvp/requirements-list.md`、`../product-specs/versions/mvp/acceptance-criteria.md`、`../product-specs/versions/mvp/implementation-notes.md` 和 `../product-specs/versions/mvp/prototype-notes.md`。
- `docs/design-docs/versions/mvp/` 必须包含 `../design-docs/versions/mvp/notes.md` 和 `../design-docs/versions/mvp/prototype.html`。
- `docs/architecture/versions/mvp/` 是 MVP 工程映射存放位置；当前旧工程设计内容已清空，目录应包含 `../architecture/versions/mvp/README.md` 占位。
- `docs/exec-plans/active/` 当前只保留 `README.md`，新增 active 任务时按任务边界新增目录。
- `docs/exec-plans/completed/` 只存放已标记完成的任务；目录必须包含 `README.md`，不存放调研、评审纪要、变更日志或验证报告等过程材料。

## Manifest 规则

- `../product-specs/product.manifest.json` 必须 JSON 合法。
- `../product-specs/product.manifest.json` 的 `activeVersion` 必须能在 `../product-specs/versions/` 下找到同名目录。
- `../product-specs/product.manifest.json` 必须能机械定位 active 产品规格、设计文档、架构文档、执行记录、决策、参考资料、生成物和检查规则。
- 每个正式版本必须包含 `../product-specs/versions/mvp/version.manifest.json`。
- `mvp` 的 `../product-specs/versions/mvp/version.manifest.json` 必须声明 `mode: "full"`。
- 未来增量版本必须声明 `mode: "incremental"`、`baseVersion`、`dependsOn`、`changes`，并按需要声明 `supersedes` 或 `overrides`。

## 链接规则

- Markdown 中指向本仓库文件的相对路径必须可解析。
- `../product-specs/current.md` 中的当前入口必须与 `../product-specs/product.manifest.json` 一致。
- `../architecture/index.md` 中列出的工程入口和状态必须与 manifest 一致。
- 重新输出数据库设计后，数据库设计文档必须覆盖表级结构、关键约束、索引、关键事务和报表查询依据。
- 正式版本包不得要求先阅读执行记录或研究材料才可实现。

## 内容规则

- 正式版本包必须自包含。
- 需求、验收、实现说明和 UI 设计稿的业务范围必须一致。
- 正式版本包内 Markdown 标题编号应唯一、稳定，不得出现同级重复编号。
- 根目录 `AGENTS.md` 只能做目录和约束摘要，不得替代正式规格或验收标准。
- 调研结论只有写入 active 版本包后才成为正式要求。
- `completed/` 只保留已标记完成的任务，不保存其他过程记录。
- 已完成任务必须说明完成状态、完成时间、原始目标、实际交付物和验证方式。
