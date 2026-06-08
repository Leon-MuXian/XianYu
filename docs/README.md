# Documentation Map

文档日期：2026-06-04

`docs/` 是闲遇的记录系统：产品规格、设计稿、工程映射、执行记录、决策、参考资料、生成物和检查规则都在这里。当前只维护 `seren-meet` MVP。

## 读取顺序

0. 读取根目录 `AGENTS.md`，确认当前产品、当前版本和正式入口。
1. 读取 `product-specs/product.manifest.json`，用机器入口确认 active 版本和跨域当前工件。
2. 读取 `product-specs/current.md`，进入当前产品、设计和架构依据。
3. 读取 `product-specs/versions/mvp/` 下的正式产品版本包。
4. 读取 `design-docs/versions/mvp/` 下的当前设计稿和设计说明。
5. 读取 `architecture/versions/mvp/` 下的工程映射。
6. 只有需要理解过程、背景或取舍时，才读取 `exec-plans/`、`decisions/` 和 `references/`。

## 顶层目录

| 路径 | 职责 |
| --- | --- |
| `product-specs/` | 产品规格、版本入口和 manifest |
| `design-docs/` | UI 设计稿和设计说明 |
| `architecture/` | 工程映射：架构、接口、数据库、技术栈、部署 |
| `exec-plans/` | active 任务和已完成任务 |
| `decisions/` | 产品决策记录 |
| `references/` | 文档标准、调研和外部参考 |
| `generated/` | 未来由工具生成的派生文档 |
| `checks/` | 可机械校验或人工复核的规则 |

## 顶层短入口

- `DESIGN.md`：设计系统和 UI 依据入口。
- `FRONTEND.md`：前端实现关注点入口。
- `PLANS.md`：执行计划和验证记录入口。
- `PRODUCT_SENSE.md`：产品判断与范围边界入口。
- `QUALITY_SCORE.md`：质量评分与检查状态入口。
- `RELIABILITY.md`：可靠性和运维关注点入口。
- `SECURITY.md`：安全和权限关注点入口。

## 版本模型

- `mvp` 是 `mode: full` 的完整基线版本。
- 未来正式新增 `v1.0` 时，使用 `mode: incremental`，并在对应 `product-specs/versions/mvp/version.manifest.json` 中声明 `baseVersion: "mvp"`、`dependsOn: ["mvp"]`、`changes`、`supersedes` 或 `overrides`。
- `product-specs/current.md` 不复制规格内容，只解析 active version 并链接到产品、设计、架构三类当前依据。

## 检查规则

- `product-specs/product.manifest.json` 和各版本 `product-specs/versions/mvp/version.manifest.json` 必须 JSON 合法，且声明的路径存在。
- Markdown 中指向本仓库文件的相对链接必须可解析。
- `design-docs/versions/mvp/prototype.html` 必须能被 HTML parser 解析。
- 正式版本包不应依赖执行记录或调研材料才能实现。
