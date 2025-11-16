# Shipping Context Events

## 概述

物流配送作業的 domain events。

**最後更新**: 2025-10-25

---

## 事件列表

| 事件名稱 | 觸發條件 | 優先級 |
|------------|---------|----------|
| `ShippingScheduledEvent` | 確認訂單 | P0 |
| `ShippingLabelCreatedEvent` | 產生標籤 | P0 |
| `ShippingDispatchedEvent` | 包裹取件 | P0 |
| `ShippingInTransitEvent` | 運送更新 | P1 |
| `ShippingDeliveredEvent` | 確認送達 | P0 |
| `ShippingFailedEvent` | 配送失敗 | P1 |

---

## ShippingScheduledEvent

```java
public record ShippingScheduledEvent(
    ShippingId shippingId,
    OrderId orderId,
    ShippingAddress address,
    LocalDateTime scheduledDate,
    String carrier,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent { }
```

### 範例 JSON

```json
{
  "eventType": "ShippingScheduled",
  "eventId": "500e8400-e29b-41d4-a716-446655440000",
  "occurredOn": "2025-10-25T10:10:00Z",
  "shippingId": "SHIP-2025-001",
  "orderId": "ORD-2025-001",
  "address": {
    "recipientName": "張小明",
    "street": "台北市信義區信義路五段7號",
    "city": "台北市",
    "postalCode": "110"
  },
  "scheduledDate": "2025-10-26T09:00:00Z",
  "carrier": "黑貓宅急便"
}
```

---

## ShippingDispatchedEvent

```java
public record ShippingDispatchedEvent(
    ShippingId shippingId,
    OrderId orderId,
    String trackingNumber,
    String carrier,
    LocalDateTime dispatchedAt,
    LocalDateTime estimatedDelivery,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent { }
```

### 範例 JSON

```json
{
  "eventType": "ShippingDispatched",
  "eventId": "510e8400-e29b-41d4-a716-446655440001",
  "occurredOn": "2025-10-26T09:00:00Z",
  "shippingId": "SHIP-2025-001",
  "orderId": "ORD-2025-001",
  "trackingNumber": "TW1234567890",
  "carrier": "黑貓宅急便",
  "dispatchedAt": "2025-10-26T09:00:00Z",
  "estimatedDelivery": "2025-10-27T18:00:00Z"
}
```

---

**文件版本**: 1.0
**最後更新**: 2025-10-25
**負責人**: Shipping Domain Team
