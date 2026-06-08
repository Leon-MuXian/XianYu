# 闲遇 Agent 读取地图

当前仓库只包含闲遇（`seren-meet`）MVP 产品设计资料，尚无应用代码。

## 优先读取

1. `docs/README.md`：文档标准与读取顺序。
2. `docs/product-specs/product.manifest.json`：机器可读的 active 版本与当前工件路径。
3. `docs/product-specs/current.md`：当前实现依据短入口。
4. `docs/product-specs/versions/mvp/spec.md`：可执行的 MVP 产品规格。
5. `docs/design-docs/versions/mvp/prototype.html`：当前 MVP UI 原型。
6. `docs/architecture/index.md`：当前 MVP 工程实现入口。

## 当前范围

- 当前产品：`seren-meet`
- 当前版本：`mvp`
- 产品依据：`docs/product-specs/versions/mvp/`
- 设计依据：`docs/design-docs/versions/mvp/`
- 工程映射：`docs/architecture/versions/mvp/`
- 执行记录：`docs/exec-plans/completed/`
- 决策和背景：`docs/decisions/`、`docs/references/`

## 工作规则

- 聊天记录不是正式依据，除非已经写入 `docs/`。
- `docs/exec-plans/completed/` 和 `docs/references/` 只作为过程或背景；只有同步到 active 版本包后，才成为实现要求。
- 新增设计决策应保持小而明确，并能从当前产品文档追溯。
- 保持 MVP 剔除项：不做线上支付、不接入微信授权手机号、不做文件上传、不做会员自助购卡、不做员工自助注册。
