# 闲遇版本索引

当前 active 版本：`mvp`

| 版本 | 模式 | 状态 | 入口 | Manifest |
| --- | --- | --- | --- | --- |
| mvp | full | active | `mvp/README.md` | `mvp/version.manifest.json` |

## 版本规则

- `mvp` 是完整基线版本，产品设计、实现和验收均以 `mvp/` 下的正式产品文档为准。
- 设计和工程工件通过 `../product.manifest.json` 与 `mvp/version.manifest.json` 跨域定位。
- 未来新增 `v1.0` 时使用增量版本包，并声明 `baseVersion: "mvp"`、`dependsOn: ["mvp"]`、`changes`、`supersedes` 或 `overrides`。
