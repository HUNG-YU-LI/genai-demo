# Information Viewpoint

> **狀態**：📝 待撰寫
> **最後更新**：2025-01-17
> **負責人**：資料架構師

## 概述

Information Viewpoint 描述系統如何跨 bounded contexts 儲存、管理和分發資訊。

## 目的

此 viewpoint 回答：

- 系統管理哪些資料？
- 資料如何結構化和關聯？
- 誰擁有哪些資料？
- 資料如何在系統中流動？

## 利害關係人

- **主要**：資料架構師、資料庫管理員
- **次要**：開發人員、架構師

## 內容

### 📄 文件

- [Overview](overview.md) - 資料管理方法
- [Domain Models](domain-models.md) - 按 bounded context 的實體關係
- [Data Ownership](data-ownership.md) - 資料擁有權和邊界
- [Data Flow](data-flow.md) - 資料如何在 contexts 之間移動
- [Data Lifecycle](data-lifecycle.md) - 建立、更新、刪除政策

### 📊 圖表

- 每個 bounded context 的實體關係圖
- 資料流圖
- 事件流圖

## 關鍵概念

### 資料擁有權

- 每個 bounded context 擁有其資料
- 透過 domain events 進行跨 context 資料存取
- Contexts 之間的最終一致性

### 資料儲存

- **主要資料庫**：PostgreSQL（正式環境）
- **開發/測試**：H2 記憶體資料庫
- **快取**：Redis 用於分散式快取

## 相關文件

### 相關 Viewpoints

- [Functional Viewpoint](../functional/README.md) - 擁有資料的 bounded contexts
- [Concurrency Viewpoint](../concurrency/README.md) - 資料一致性策略

### 相關 Perspectives

- [Security Perspective](../../perspectives/security/README.md) - 資料加密和保護
- [Performance Perspective](../../perspectives/performance/README.md) - 資料庫最佳化

## 快速連結

- [返回所有 Viewpoints](../README.md)
- [主文件](../../README.md)

---

**注意**：此 viewpoint 目前正在撰寫中。
