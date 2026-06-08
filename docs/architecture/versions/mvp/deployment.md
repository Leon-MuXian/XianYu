# 闲遇 MVP 部署映射

文档日期：2026-06-07  
状态：active architecture  
范围：完整 MVP

## 1. 最终部署形态

MVP 部署分为两类：

- 微信小程序端：通过微信开发者工具和微信小程序后台上传、体验、审核、发布。
- 服务端和后台：使用 Docker + Docker Compose 部署 Spring Boot API、后台前端、PostgreSQL 和反向代理。

```mermaid
flowchart LR
  Mini["微信小程序<br/>店长/教练/会员"] --> Nginx["HTTPS 反向代理"]
  Admin["平台后台 Web"] --> Nginx
  Nginx --> API["Spring Boot API"]
  API --> PG["PostgreSQL"]
  API --> Wechat["微信 code2session"]
```

正式部署后的自动数据备份不纳入 MVP 当前范围。

## 2. 环境

| 环境 | 用途 | 要求 |
| --- | --- | --- |
| `local` | 本地开发 | Docker Compose 启动 PostgreSQL、API 和后台前端；小程序使用开发者工具。 |
| `trial` | 试点体验 | 给体验版小程序、内部后台和真实试点门店使用。 |
| `prod` | 正式版本 | 小程序审核发布后的线上 API 和后台。 |

如果资源有限，`trial` 和 `prod` 可以共用同一台服务器，但必须使用不同环境变量、数据库 schema 或数据库实例隔离。

## 3. Docker Compose 服务

| 服务 | 说明 |
| --- | --- |
| `api` | Spring Boot 后端。 |
| `admin-web` | Vue 后台构建产物，由 Nginx 或静态服务托管。 |
| `postgres` | PostgreSQL。 |
| `nginx` | HTTPS、静态资源、API 反向代理。 |

不包含独立缓存服务、消息队列、对象存储或文件服务。

## 4. 域名与 HTTPS

小程序正式请求必须使用 HTTPS 合法域名。推荐：

- `api.example.com`：后端 API。
- `admin.example.com`：平台后台。

Nginx 推荐规则：

- `/api/` 转发到 Spring Boot。
- `/` 服务后台前端。
- 静态资源启用合理缓存。
- API 响应透传或生成 `X-Request-Id`。

## 5. 后端环境变量

| 变量 | 说明 |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `local`、`trial`、`prod`。 |
| `SERVER_PORT` | API 端口。 |
| `DATABASE_URL` | PostgreSQL 地址。 |
| `DATABASE_USERNAME` | 数据库用户名。 |
| `DATABASE_PASSWORD` | 数据库密码。 |
| `JWT_SECRET` | token 密钥。 |
| `WECHAT_OWNER_APP_ID` | 店长端小程序 AppID。 |
| `WECHAT_OWNER_APP_SECRET` | 店长端小程序 AppSecret。 |
| `WECHAT_MEMBER_APP_ID` | 会员端小程序 AppID。 |
| `WECHAT_MEMBER_APP_SECRET` | 会员端小程序 AppSecret。 |

如果三端共用同一个微信小程序 AppID，工程创建时可合并为统一变量，但文档先按可独立配置设计。

## 6. 数据库迁移

- 使用 Flyway。
- 迁移脚本随代码提交。
- 应用启动时执行迁移或由部署命令显式执行迁移。
- 禁止在试点或正式环境手工改表后不提交迁移脚本。

## 7. 小程序发布

小程序不通过 Docker 发布。推荐流程：

1. 注册微信小程序并获取 AppID。
2. 在微信公众平台配置服务器域名。
3. 本地使用微信开发者工具调试。
4. 构建 Taro 微信小程序产物。
5. 上传体验版。
6. 体验成员验证店长、教练、会员三端主流程。
7. 提交审核。
8. 审核通过后发布。

后续可接入微信官方 `miniprogram-ci` 自动预览和上传，但 MVP 可以先手动上传。

## 8. 后台前端发布

后台前端构建为静态资源：

1. 安装依赖。
2. 执行类型检查和构建。
3. 将 `dist` 放入 `admin-web` 镜像或 Nginx 静态目录。
4. 配置 API base URL。
5. 验证 1024px、1280px、1440px、1920px 和宽表格。

## 9. 运维边界

MVP 必须具备：

- 健康检查。
- 应用日志。
- 请求 ID。
- 数据库迁移。
- 环境变量配置。
- 后台登录保护。

MVP 不包含：

- 自动数据备份。
- 多地域容灾。
- 自动扩缩容。
- Kubernetes。
- 蓝绿发布。
- 对象存储。
- 文件上传服务。
