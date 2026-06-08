# 闲遇 API

`apps/api` 是闲遇 MVP 的 Spring Boot 后端工程，当前主要承载平台后台 Web 所需的后台登录、租户查询、冻结/延期、平台配置、客服微信配置和审计记录接口。工程使用 Java 21、Spring Boot 3.3、MyBatis-Plus、Flyway 和 PostgreSQL。

## 工程情况

- 应用入口：`src/main/java/com/serenmeet/SerenMeetApplication.java`
- 配置文件：`src/main/resources/application.yml`
- 数据库迁移：`src/main/resources/db/migration/`
- MyBatis XML：`src/main/resources/mapper/`
- 测试目录：`src/test/java/com/serenmeet/`

当前初始化数据只保留：

- 平台后台默认账号：`admin@serenmeet`
- 默认密码：`admin123`
- 平台配置：试用天数、到期提醒、邀请码有效期、取消截止、会员卡到期提醒
- 默认客服微信配置：`SerenMeet-CS`

初始化数据不再写入 mock 租户、门店、业务快照或演示审计记录。租户数据应由后续真实业务流程产生。

## 本地运行

前置要求：

- JDK 21
- Maven 3.9+
- Docker Desktop 或可用的 Docker Compose

从仓库根目录启动 PostgreSQL：

```bash
./scripts/compose-up.sh
```

启动 API：

```bash
./scripts/dev-api.sh
```

也可以直接在本目录运行：

```bash
mvn spring-boot:run
```

默认服务地址为 `http://127.0.0.1:8080`。应用启动时 Flyway 会自动执行迁移。

常用环境变量：

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `SEREN_MEET_SERVER_PORT` | `8080` | API 监听端口 |
| `SEREN_MEET_DB_URL` | `jdbc:postgresql://127.0.0.1:5432/seren_meet` | PostgreSQL JDBC 地址 |
| `SEREN_MEET_DB_USER` | `seren_meet` | 数据库用户名 |
| `SEREN_MEET_DB_PASSWORD` | `seren_meet` | 数据库密码 |
| `SEREN_MEET_ADMIN_ORIGIN` | `http://127.0.0.1:5173` | 后台 Web 跨域来源 |
| `SEREN_MEET_FLYWAY_ENABLED` | `true` | 是否启用 Flyway |

运行测试：

```bash
mvn test
```

## Docker 打包和部署

在仓库根目录构建镜像：

```bash
docker build -f apps/api/Dockerfile -t seren-meet-api:local apps/api
```

运行容器时需要连接 PostgreSQL。示例使用仓库 Compose 创建的数据库容器：

```bash
docker run --rm -p 8080:8080 \
  --network compose_default \
  -e SEREN_MEET_DB_URL=jdbc:postgresql://postgres:5432/seren_meet \
  -e SEREN_MEET_DB_USER=seren_meet \
  -e SEREN_MEET_DB_PASSWORD=seren_meet \
  seren-meet-api:local
```

生产或试点部署建议用 Docker Compose 编排 `postgres`、`api`、`admin-web` 和反向代理。API 容器必须通过环境变量注入数据库连接、CORS 来源和运行端口；数据库迁移随应用启动执行。
