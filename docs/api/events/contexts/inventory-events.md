# Inventory Context Events

## 概述

庫存管理和庫存控制的 domain events。

**最後更新**: 2025-10-25

---

## 事件列表

| 事件名稱 | 觸發條件 | 優先級 |
|------------|---------|----------|
| `InventoryReservedEvent` | 提交訂單 | P0 |
| `InventoryReleasedEvent` | 取消訂單 | P0 |
| `InventoryAdjustedEvent` | 調整庫存 | P1 |
| `InventoryLowStockAlertEvent` | 偵測到低庫存 | P1 |

---

## InventoryReservedEvent

```java
public record InventoryReservedEvent(
    OrderId orderId,
    List<ReservedItemDto> reservedItems,
    LocalDateTime reservedUntil,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent { }
```

### 範例 JSON

```json
{
  "eventType": "InventoryReserved",
  "eventId": "400e8400-e29b-41d4-a716-446655440000",
  "occurredOn": "2025-10-25T10:06:30Z",
  "orderId": "ORD-2025-001",
  "reservedItems": [
    {
      "productId": "PROD-001",
      "quantity": 1,
      "warehouseId": "WH-TPE-001"
    }
  ],
  "reservedUntil": "2025-10-25T10:21:30Z"
}
```

---

## InventoryLowStockAlertEvent

```java
public record InventoryLowStockAlertEvent(
    ProductId productId,
    int currentStock,
    int threshold,
    String warehouseId,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent { }
```

### 範例 JSON

```json
{
  "eventType": "InventoryLowStockAlert",
  "eventId": "410e8400-e29b-41d4-a716-446655440001",
  "occurredOn": "2025-10-25T14:00:00Z",
  "productId": "PROD-001",
  "currentStock": 5,
  "threshold": 10,
  "warehouseId": "WH-TPE-001"
}
```

---

**文件版本**: 1.0
**最後更新**: 2025-10-25
**負責人**: Inventory Domain Team
