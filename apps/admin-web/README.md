# 闲遇 Admin Web

`apps/admin-web` 是闲遇 MVP 的平台后台 Web 工程，面向平台内部运营人员使用。当前实现覆盖账号登录、租户管理、租户详情、冻结与延期处理、客服微信配置、平台配置和操作记录页面。

工程使用 Vue 3、Vite、TypeScript、Vue Router 和 Element Plus。接口请求集中在 `src/services/api.ts`，默认连接本地 API `http://127.0.0.1:8080`。

## 工程情况

- 应用入口：`src/main.ts`
- 路由：`src/router/index.ts`
- 页面：`src/pages/`
- 布局：`src/layouts/AdminLayout.vue`
- API 封装：`src/services/api.ts`
- 样式与 token：`src/styles/`

默认登录信息：

- 账号：`admin@serenmeet`
- 密码：`admin123`

后台不提供注册入口。账号由 API 的 Flyway 初始化数据创建。

## 本地运行

前置要求：

- Node.js 20+
- npm
- 已启动 `apps/api`

安装依赖：

```bash
npm install
```

启动开发服务：

```bash
npm run dev
```

也可以从仓库根目录运行：

```bash
./scripts/dev-admin.sh
```

默认访问地址为 `http://127.0.0.1:5173`。

如需连接非默认 API，启动或构建前设置：

```bash
VITE_API_BASE_URL=http://127.0.0.1:8080 npm run dev
```

常用命令：

```bash
npm run typecheck
npm run build
npm run preview
```

## Docker 打包和部署

在仓库根目录构建镜像：

```bash
docker build -f apps/admin-web/Dockerfile \
  --build-arg VITE_API_BASE_URL=/api \
  -t seren-meet-admin-web:local \
  apps/admin-web
```

运行镜像：

```bash
docker run --rm -p 8081:80 seren-meet-admin-web:local
```

镜像使用 Nginx 托管 Vite 构建后的静态资源，并对 Vue Router 做 `try_files` 回退。生产部署时推荐由外层 Nginx 或网关将 `/api/` 反向代理到 `apps/api` 服务；如果 API 使用独立域名，可在构建时把 `VITE_API_BASE_URL` 设置为完整 HTTPS 地址。
