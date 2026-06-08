# 闲遇 MVP 版本包

MVP 是当前 active 版本包，模式为 `full`。当前版本声明见 `../../product.manifest.json` 和 `version.manifest.json`。

## 正式产品文档

- `version.manifest.json`
- `spec.md`
- `requirements-list.md`
- `acceptance-criteria.md`
- `implementation-notes.md`
- `prototype-notes.md`
- `pilot-wedge.md`

## 跨域当前依据

- UI 设计稿：`../../../design-docs/versions/mvp/prototype.html`
- 设计说明：`../../../design-docs/versions/mvp/notes.md`
- 工程映射：`../../../architecture/versions/mvp/`

## 版本边界

阅读 MVP 时，产品范围以本目录文档为准；设计稿和工程映射只解释如何呈现和实现当前产品范围。执行记录、研究材料和聊天记录不作为实现前提。

## MVP 核心定位

闲遇 MVP 是面向多业态门店的通用约课平台。只要业务符合“服务项目/课程 + 履约人员 + 时间资源 + 会员预约 + 权益核销”的模型，即可使用本版本，不限定健身、瑜伽、普拉提、舞蹈、康复训练等行业。

MVP 明确不做微信授权手机号、线上支付和任何文件上传。门店营收数据只记录店长线下售卡录入的实收金额和收款方式文本；平台营收采用试用到期后添加客服微信人工续费。

## 试点楔子

`pilot-wedge.md` 是当前 MVP 的分阶段实现补充，不替代完整 `spec.md`。当前完整 MVP 基线保留；第一阶段优先验证“会员卡权益台账 + 员工核销 + 低余额 / 到期预警”，后续设计和工程映射应先读取该文件再重排 UI 与模块交付顺序。
