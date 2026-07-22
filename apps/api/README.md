# 闲遇 API

`apps/api` 是闲遇 MVP 的 Spring Boot 模块化单体，承载统一认证、租户业务、权益账本、最小预约履约链路和平台后台接口。工程使用 Java 21、Spring Boot 3.3、MyBatis-Plus、Flyway 和 PostgreSQL。

## 安全初始化

仓库不包含默认管理员账号或密码。管理员表为空时，只有以下三个变量同时存在才会创建首个管理员：

- `SEREN_MEET_BOOTSTRAP_ADMIN_USERNAME`
- `SEREN_MEET_BOOTSTRAP_ADMIN_PASSWORD`，至少 12 位
- `SEREN_MEET_BOOTSTRAP_ADMIN_DISPLAY_NAME`

初始化只执行一次，明文密码不会写入数据库或日志。平台配置和默认客服展示信息不是登录凭据。

## 本地运行

需要 JDK 21、Maven 3.9+、Docker 和 Node.js 20+。先按 `infra/compose/.env.example` 创建本机环境文件，再从仓库根目录运行：

```bash
./scripts/compose-up.sh
```

也可以只准备 PostgreSQL，然后从 `apps/api/.env.example` 创建不提交的 `apps/api/.env`，再启动 API：

```bash
./scripts/dev-api.sh
```

开发者工具模拟器使用 `SPRING_PROFILES_ACTIVE=local`，此时微信登录使用本地替身。手机真机调试真实微信登录时改为 `trial`，并在 `.env` 注入店长端 AppID/AppSecret；YAML 不保存任何凭据默认值。

常用变量：

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `SEREN_MEET_SERVER_PORT` | `8080` | API 监听端口 |
| `SEREN_MEET_DB_URL` | `jdbc:postgresql://127.0.0.1:5432/seren_meet` | PostgreSQL JDBC 地址 |
| `SEREN_MEET_DB_USER` | `seren_meet` | 数据库用户名 |
| `SEREN_MEET_DB_PASSWORD` | 无，必填 | 数据库密码 |
| `SEREN_MEET_ADMIN_ORIGIN` | `http://127.0.0.1:5173` | 后台 Web 跨域来源 |
| `SEREN_MEET_FLYWAY_ENABLED` | `true` | 是否启用 Flyway |

运行真实 PostgreSQL 集成测试：

```bash
mvn test
```

macOS Docker Desktop 使用非默认 socket 时可显式设置 `DOCKER_HOST`。

## 运维入口

- OpenAPI：`/v3/api-docs`
- Liveness：`/actuator/health/liveness`
- Readiness：`/actuator/health/readiness`
- 对外部署统一由 Nginx 增加 `/api`，转发时剥离此前缀。

完整编排、环境变量和显式数据库重建步骤见 `docs/architecture/versions/mvp/deployment.md`。
