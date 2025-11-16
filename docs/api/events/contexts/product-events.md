# Product Context Events

## 概述

本文件描述 Product bounded context 發佈的所有 domain events。這些事件捕獲產品生命週期、目錄管理和庫存相關變更。

**最後更新**: 2025-10-25

---

## 事件列表

| 事件名稱 | 觸發條件 | 頻率 | 優先級 |
|------------|---------|-----------|----------|
| `ProductCreatedEvent` | 建立產品 | 中 | P1 |
| `ProductUpdatedEvent` | 編輯產品 | 高 | P1 |
| `ProductPriceChangedEvent` | 更新價格 | 中 | P0 |
| `ProductStockUpdatedEvent` | 調整庫存 | 高 | P0 |
| `ProductDeactivatedEvent` | 停用產品 | 低 | P1 |
| `ProductReactivatedEvent` | 重新啟用產品 | 低 | P1 |
| `ProductCategoryChangedEvent` | 更新類別 | 低 | P2 |

---

## ProductCreatedEvent

### 事件結構

```java
public record ProductCreatedEvent(
    ProductId productId,
    String productName,
    String description,
    CategoryId categoryId,
    Money price,
    int initialStock,
    SellerId sellerId,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent { }
```

### 範例 JSON

```json
{
  "eventType": "ProductCreated",
  "eventId": "200e8400-e29b-41d4-a716-446655440000",
  "occurredOn": "2025-10-25T09:00:00Z",
  "productId": "PROD-001",
  "productName": "iPhone 15 Pro 256GB",
  "description": "最新款 iPhone，配備 A17 Pro 晶片",
  "categoryId": "CAT-ELECTRONICS",
  "price": {
    "amount": 35900,
    "currency": "TWD"
  },
  "initialStock": 100,
  "sellerId": "SELLER-001"
}
```

### Event Handlers

- `ProductSearchIndexHandler`：加入搜尋索引
- `ProductCatalogHandler`：更新產品目錄
- `SellerProductCountHandler`：更新賣家的產品數量

---

## ProductPriceChangedEvent

### 事件結構

```java
public record ProductPriceChangedEvent(
    ProductId productId,
    Money oldPrice,
    Money newPrice,
    String changeReason,
    LocalDateTime effectiveDate,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent { }
```

### 範例 JSON

```json
{
  "eventType": "ProductPriceChanged",
  "eventId": "210e8400-e29b-41d4-a716-446655440001",
  "occurredOn": "2025-10-25T10:00:00Z",
  "productId": "PROD-001",
  "oldPrice": {
    "amount": 35900,
    "currency": "TWD"
  },
  "newPrice": {
    "amount": 32900,
    "currency": "TWD"
  },
  "changeReason": "促銷活動",
  "effectiveDate": "2025-10-26T00:00:00Z"
}
```

### Event Handlers

- `PricingCacheHandler`：更新價格快取
- `PriceHistoryHandler`：記錄價格歷史
- `PriceAlertHandler`：通知有設定價格提醒的客戶

---

## ProductStockUpdatedEvent

### 事件結構

```java
public record ProductStockUpdatedEvent(
    ProductId productId,
    int oldStock,
    int newStock,
    int changeAmount,
    String updateReason,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent { }
```

### 範例 JSON

```json
{
  "eventType": "ProductStockUpdated",
  "eventId": "220e8400-e29b-41d4-a716-446655440002",
  "occurredOn": "2025-10-25T11:00:00Z",
  "productId": "PROD-001",
  "oldStock": 100,
  "newStock": 99,
  "changeAmount": -1,
  "updateReason": "ORDER_PLACED"
}
```

### Event Handlers

- `LowStockAlertHandler`：檢查低庫存提醒
- `ProductAvailabilityHandler`：更新可用狀態
- `InventoryReportHandler`：更新庫存報表

---

**文件版本**: 1.0
**最後更新**: 2025-10-25
**負責人**: Product Domain Team
