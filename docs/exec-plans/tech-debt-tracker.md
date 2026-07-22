# Tech Debt Tracker

当前技术债由阶段一治理任务统一收敛。已关闭项保留用于证明六项治理的处理结果；未关闭项不得被完成状态掩盖。

| 编号 | 条目 | 优先级 | 状态 | 证据或下一步 |
| --- | --- | --- | --- | --- |
| TD-001 | 平台后台使用手写 DTO，缺少 OpenAPI 生成客户端 | P0 | 已关闭 | OpenAPI 快照生成 `shared-types`，四端网络请求统一使用共享客户端；后台领域类型从生成 schema 派生 |
| TD-002 | 后端仅有单元测试，缺少 PostgreSQL 集成与并发测试 | P0 | 已关闭 | Testcontainers PostgreSQL 覆盖空库、完整试点链路、租户隔离、冻结、幂等、容量竞争和并发核销，并进入 CI |
| TD-003 | Compose 仅包含 PostgreSQL | P1 | 已关闭 | PostgreSQL、API、Admin Web、Nginx 四服务隔离 smoke 通过 |
| TD-004 | 前端主包超过 Vite 默认告警阈值 | P1 | 已关闭 | Element Plus 按组件拆包，最大生产 JS chunk 约 107 KB |
| TD-005 | Taro 工具链存在已知高危间接依赖 | P1 | 阶段三待处理 | `npm audit --omit=dev` 报告 15 项（3 critical、2 high、10 moderate）；自动修复会降级到 Taro 3，阶段三结合上游版本升级并重跑四端回归 |

未来新增技术债时，记录：

- 编号和标题。
- 影响范围。
- 关联规格、架构或执行计划。
- 处理优先级。
- 关闭条件。
