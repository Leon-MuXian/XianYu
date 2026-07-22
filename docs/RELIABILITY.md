# Reliability

当前可靠性依据来自完整 MVP 工程映射。阶段一优先建立可观测、可迁移、可重复部署的试点基线。

## 当前依据

- 工程入口：`architecture/index.md`
- 产品规格：`product-specs/versions/mvp/spec.md`
- 实现说明：`product-specs/versions/mvp/implementation-notes.md`

## 当前要求

- API 暴露 liveness 和 readiness，数据库不可用时 readiness 必须失败。
- 所有响应和日志携带同一 `requestId`，定时任务和后台变更写入审计。
- Flyway 必须能从空 PostgreSQL 数据库一次性完成迁移。
- Docker Compose 服务必须声明健康检查和依赖条件。
- 预约容量、发卡、取消和核销使用数据库事务、唯一约束和行级并发控制。
- trial 环境和未来 prod 环境使用独立配置与数据库；自动备份仍不进入 MVP。

阶段一已通过 PostgreSQL 16 Testcontainers 集成测试和隔离 Compose smoke。真实 trial 的 HTTPS、微信合法域名、真实凭据和业务观测仍需按 active 阶段门完成。
