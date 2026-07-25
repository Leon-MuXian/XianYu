# Decisions Index

本目录保存产品决策记录。产品决策记录用于解释长期有效的产品判断，并帮助后续版本理解为什么这样设计。

## 当前决策

- `PDR-0001-rewrite-mvp-baseline.md`：无存量环境前提下重写完整 V1 基线。
- `PDR-0002-unified-opaque-session.md`：四端统一数据库支持的不透明会话。
- `PDR-0003-phase-one-minimum-booking.md`：阶段一前置最小会员预约以保持履约事实完整。
- `PDR-0004-owner-managed-staff-credentials.md`：店长按需查看并交付其门店员工登录凭据。

## 命名

使用 PDR 编号加主题命名，例如 `PDR-0001-topic.md`。

## 模板

```text
# PDR-0001 Title

状态：accepted
日期：YYYY-MM-DD
关联版本：mvp

## 背景

## 决策

## 影响

## 关联
```

决策记录可以作为解释和约束背景，但具体实现仍以当前 active 版本包为准。
