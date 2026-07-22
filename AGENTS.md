# 闲遇 Agent 读取地图

当前仓库包含闲遇（`seren-meet`）MVP 的完整产品、设计和工程依据，以及阶段一可运行应用代码。阶段一技术基线已完成，当前等待真实 trial 和业务阶段门；阶段二尚未启动。

## 优先读取

1. `docs/README.md`：文档标准与读取顺序。
2. `docs/product-specs/product.manifest.json`：机器可读的 active 版本与当前工件路径。
3. `docs/product-specs/current.md`：当前实现依据短入口。
4. `docs/product-specs/versions/mvp/spec.md`：可执行的 MVP 产品规格。
5. `docs/design-docs/versions/mvp/prototype.html`：当前 MVP UI 原型。
6. `docs/architecture/index.md`：当前 MVP 工程实现入口。
7. `docs/architecture/versions/mvp/implementation-status.md`：产品范围与实际代码状态矩阵。

## 当前范围

- 当前产品：`seren-meet`
- 当前版本：`mvp`
- 产品依据：`docs/product-specs/versions/mvp/`
- 设计依据：`docs/design-docs/versions/mvp/`
- 工程映射：`docs/architecture/versions/mvp/`
- 执行记录：`docs/exec-plans/completed/`
- 当前执行计划：`docs/exec-plans/active/mvp-governance/`
- 决策和背景：`docs/decisions/`、`docs/references/`

## 工作规则

- 聊天记录不是正式依据，除非已经写入 `docs/`。
- `docs/exec-plans/completed/` 和 `docs/references/` 只作为过程或背景；只有同步到 active 版本包后，才成为实现要求。
- 新增设计决策应保持小而明确，并能从当前产品文档追溯。
- 保持 MVP 剔除项：不做线上支付、不接入微信授权手机号、不做文件上传、不做会员自助购卡、不做员工自助注册。
- 阶段一完成后必须经过 `pilot-wedge.md` 的真实业务阶段门；未达标不得进入完整预约阶段。
- PostgreSQL 生产访问必须通过 MyBatis-Plus Mapper；复杂 SQL 放在具名 Mapper XML 中。禁止在 `src/main` 使用 `JdbcTemplate` 或直接 JDBC，测试可用 `JdbcTemplate` 做黑盒数据库断言。
- Java 新增和修改代码遵循《阿里巴巴 Java 开发手册》：4 空格缩进、构造器注入、清晰常量和命名、禁止通配符导入与标准输出、异常保留原因；提交前执行 `npm run check:java`。
