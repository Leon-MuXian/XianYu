# 闲遇文档标准

文档日期：2026-06-06

## 1. 标准目标

`docs/` 按 Harness Engineering 范式组织产品资料：把当前产品真相、正式版本包、设计稿、工程映射、任务记录、设计决策、调研背景和检查规则全部沉淀为仓库内可读、可审计、可验证的工件。

目标是在项目初期建立一套能支撑后续产品设计和工程实现迭代的记录系统。

## 2. 核心原则

- 当前真相必须短路径可达：Agent 从 `../product-specs/product.manifest.json` 和 `../product-specs/current.md` 即可定位当前产品、设计和架构依据。
- 正式产品规格必须自包含：当前单产品仓库使用 `product-specs/versions/<version>/` 独立描述产品范围、需求、验收、实现说明和原型说明。
- 顶层域职责清晰：产品规格、设计稿、架构、执行记录、决策、参考资料、生成物和检查规则分域放置。
- 计划和任务是一等工件：正在执行的任务进入 `../exec-plans/active/`，已完成任务进入 `../exec-plans/completed/`；最终要求必须同步到正式文档。
- 产品决策必须可追溯：跨版本有效的判断进入 `../decisions/`，不能散落在聊天或临时文档中。
- 调研不能直接变成需求：`../references/` 下的研究材料只提供背景，进入版本包后才成为正式产品要求。
- 检查规则必须显式：`../checks/` 描述文档和 UI 设计稿的自动化或人工检查标准。

## 3. 标准目录

```text
docs/
├── README.md
├── DESIGN.md
├── FRONTEND.md
├── PLANS.md
├── PRODUCT_SENSE.md
├── QUALITY_SCORE.md
├── RELIABILITY.md
├── SECURITY.md
├── architecture/
├── design-docs/
├── product-specs/
├── exec-plans/
├── decisions/
├── references/
├── generated/
└── checks/
```

## 4. 当前产品目录

```text
docs/product-specs/
├── index.md
├── current.md
├── product.manifest.json
└── versions/
    ├── index.md
    └── mvp/
        ├── version.manifest.json
        ├── README.md
        ├── spec.md
        ├── requirements-list.md
        ├── acceptance-criteria.md
        ├── implementation-notes.md
        └── prototype-notes.md
```

## 5. 文件职责

| 文件或目录 | 职责 | 是否可作为实现依据 |
| --- | --- | --- |
| `product-specs/product.manifest.json` | 机器可读入口，声明当前版本和跨域关键路径 | 是 |
| `product-specs/current.md` | 当前实现入口，链接产品、设计和架构当前依据 | 是 |
| `product-specs/versions/<version>/` | 单版本正式产品包 | 是 |
| `design-docs/versions/<version>/prototype.html` | 当前 UI 设计稿 | 是 |
| `design-docs/versions/<version>/notes.md` | 当前 UI 设计说明 | 是 |
| `architecture/versions/<version>/` | 当前版本工程映射 | 是 |
| `../exec-plans/` | active 任务和已完成任务记录 | 否，除非内容已进入版本包 |
| `../decisions/` | 产品决策记录 | 是，作为解释和约束背景 |
| `../references/` | 文档标准、调研和外部参考 | 否，除非内容已进入版本包 |
| `generated/` | 工具生成的派生文档 | 否，以源文档为准 |
| `../checks/` | 检查规则和验收辅助清单 | 是，作为质量门禁 |

## 6. 版本生命周期

1. `draft`：在 `exec-plans/active/` 中形成计划和草稿，不作为实现依据。
2. `candidate`：设计已收敛，开始同步到 `versions/<version>/` 并接受评审。
3. `active`：写入 `../product-specs/product.manifest.json` 的 `activeVersion`，成为当前实现依据。
4. `superseded`：被新版本替代，保留完整版本包用于追溯。
5. `retired`：明确不再维护，只保留必要决策或迁移说明。

当前阶段 `mvp` 为 `active`，且 `mode: full`。

## 7. 增量版本规范

未来 `v1.0` 使用 `mode: incremental`，在 `version.manifest.json` 中声明：

- `baseVersion: "mvp"`
- `dependsOn: ["mvp"]`
- `changes: [...]`
- `supersedes` 或 `overrides` 的文档路径

增量版本进入 active 前，必须能从 manifest 机械追溯到它依赖的完整基线版本。

## 8. 执行记录标准

正在执行的任务放入 `exec-plans/active/<task-id>/`。任务完成后，移动到 `exec-plans/completed/<task-id>/`，并在任务本体中标记完成状态。

`completed/` 只存放已标记完成的任务，不存放调研、评审纪要、变更日志、验证报告或其他过程材料。必要结论必须同步到 active 产品版本包、设计稿、架构文档或决策记录。

## 9. 检查标准

每次修改产品设计相关文档后，至少检查：

- `../product-specs/product.manifest.json` 指向的文件都存在。
- active `version.manifest.json` 指向的文件都存在。
- `../product-specs/current.md` 的入口链接都存在。
- `versions/<activeVersion>/` 包含规定文件。
- `design-docs/versions/<activeVersion>/prototype.html` 能被 HTML parser 解析。
- `architecture/versions/<activeVersion>/` 存在，并与 `architecture/index.md` 声明的当前状态一致。
- 正式版本包不把执行记录或研究材料当作实现前提。
- 页面 ID、需求清单、验收标准和 UI 设计稿没有明显冲突。

## 10. 禁止事项

- 禁止只在聊天中保存产品决策。
- 禁止把执行计划草稿当作正式实现依据。
- 禁止让 `product-specs/` 承载设计稿、架构、执行记录、决策、调研或检查规则。
- 禁止让 UI 设计稿和版本包描述不同业务范围。
- 禁止在没有更新 `../product-specs/product.manifest.json` 的情况下切换当前版本。
