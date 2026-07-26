# Quality Score

当前仓库同时包含正式文档、原型和应用代码。质量门禁覆盖文档一致性、接口契约、数据库迁移、自动化测试、前端构建和容器构建。

## 当前质量门禁

- manifest JSON 合法且路径存在。
- Markdown 相对链接可解析。
- active version 能从 `AGENTS.md` 到 `product-specs/current.md` 被定位。
- `design-docs/versions/mvp/prototype.html` 能被 HTML parser 解析。
- 正式版本包不依赖过程记录或调研材料才能实现。
- OpenAPI 生成产物不得与后端契约漂移。
- PostgreSQL 空库迁移、后端关键路径测试、前端类型检查和构建必须通过。
- 关键写操作必须覆盖幂等、事务和并发场景。
- 生产代码与迁移中不得包含固定管理员密码或微信密钥。
- PostgreSQL 生产访问只能位于 MyBatis-Plus Mapper，`src/main` 禁止 `JdbcTemplate` 和直接 JDBC。
- Java 代码必须通过 Java 21 兼容的阿里规范 PMD 子集与仓库边界检查。

详细规则见 `checks/doc-rules.md` 和 `checks/prototype-checklist.md`。

当前自动化入口为根目录 `npm run check:docs`、`npm run check:java` 和 `.github/workflows/ci.yml`。文档检查会逐项校验 74 个原型标签与实现矩阵；Java 检查会阻止生产代码绕过 MyBatis-Plus，并执行命名、异常、集合、资源关闭和控制流规则；后端集成测试会比较运行时 `/v3/api-docs` 和已提交 OpenAPI 快照，前端检查会验证该快照生成 TypeScript 后无漂移。实现进度与剩余门禁见 `architecture/versions/mvp/implementation-status.md`。
