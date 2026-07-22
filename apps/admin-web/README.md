# 闲遇 Admin Web

`apps/admin-web` 是平台内部运营后台，覆盖管理员登录、租户列表与真实业务信号、租户详情、冻结/延期、客服微信、平台配置和审计记录。

后台不提供注册入口，也不预填或展示任何固定登录凭据。首个管理员由 API 的一次性环境变量引导创建。

## 本地运行

依赖由根 npm workspace 统一安装和锁定：

```bash
npm ci
npm run typecheck
npm run test
npm run build:web
npm run dev --workspace @serenmeet/admin-web
```

默认开发地址为 `http://127.0.0.1:5173`，默认直连 `http://127.0.0.1:8080`。连接其他 API 时设置 `VITE_API_BASE_URL`。

后台请求复用 `@serenmeet/api-client`，网络响应和错误结构来自共享 OpenAPI 契约；冻结、延期和平台配置等关键写操作携带 `Idempotency-Key`。

## 容器

Compose 从仓库根 workspace 构建后台镜像，并以 `/api` 作为浏览器端 API 基址：

```bash
docker compose --env-file infra/compose/.env \
  -f infra/compose/docker-compose.yml build admin-web
```

镜像内 Nginx 提供 SPA 回退和静态资源缓存，外层 Nginx 负责 `/api` 反向代理。
