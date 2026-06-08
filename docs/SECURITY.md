# Security

当前安全依据来自 MVP 产品规格。旧 MVP 工程设计内容已清空，安全工程细节等待重新输出。

## 当前依据

- 产品规格：`product-specs/versions/mvp/spec.md`
- 实现说明：`product-specs/versions/mvp/implementation-notes.md`
- 工程入口：`architecture/index.md`

MVP 必须保持多租户数据隔离、角色权限校验和冻结租户访问限制。当前不接入线上支付、微信授权手机号或文件上传。
