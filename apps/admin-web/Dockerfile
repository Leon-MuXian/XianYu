FROM node:20-alpine AS build

WORKDIR /workspace
ARG VITE_API_BASE_URL=/api
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

COPY package.json package-lock.json ./
COPY apps/admin-web/package.json apps/admin-web/package.json
COPY apps/miniprogram-owner/package.json apps/miniprogram-owner/package.json
COPY apps/miniprogram-staff/package.json apps/miniprogram-staff/package.json
COPY apps/miniprogram-member/package.json apps/miniprogram-member/package.json
COPY packages/api-client/package.json packages/api-client/package.json
COPY packages/business-components/package.json packages/business-components/package.json
COPY packages/design-tokens/package.json packages/design-tokens/package.json
COPY packages/shared-types/package.json packages/shared-types/package.json
RUN npm ci
COPY apps/admin-web apps/admin-web
COPY packages packages
RUN npm run build:packages && npm run build:web

FROM nginx:1.27-alpine

COPY apps/admin-web/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/apps/admin-web/dist /usr/share/nginx/html

EXPOSE 80
