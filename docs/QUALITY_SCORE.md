# Quality Score

当前仓库只包含文档和原型，质量评分以文档可读性、路径可解析性和原型可解析性为主。

## 当前质量门禁

- manifest JSON 合法且路径存在。
- Markdown 相对链接可解析。
- active version 能从 `AGENTS.md` 到 `product-specs/current.md` 被定位。
- `design-docs/versions/mvp/prototype.html` 能被 HTML parser 解析。
- 正式版本包不依赖过程记录或调研材料才能实现。

详细规则见 `checks/doc-rules.md` 和 `checks/prototype-checklist.md`。
