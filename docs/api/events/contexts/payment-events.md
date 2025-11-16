# Payment Context Events

## 概述

本文件描述 Payment bounded context 發佈的所有 domain events。這些事件捕獲付款處理、退款和交易管理。

**最後更新**: 2025-10-25

---

## 事件列表

| 事件名稱 | 觸發條件 | 頻率 | 優先級 |
|------------|---------|-----------|----------|
| `PaymentInitiatedEvent` | 開始付款 | 極高 | P0 |
| `PaymentProcessedEvent` | 付款成功 | 極高 | P0 |
| `PaymentFailedEvent` | 付款失敗 | 中 | P0 |
| `PaymentRefundedEvent` | 處理退款 | 低 | P1 |
| `PaymentCancelledEvent` | 取消付款 | 低 | P1 |

---

## PaymentInitiatedEvent

### 事件結構

```java
public record PaymentInitiatedEvent(
    PaymentId paymentId,
    OrderId orderId,
    CustomerId customerId,
    Money amount,
    PaymentMethod paymentMethod,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent { }
```

### 範例 JSON

```json
{
  "eventType": "PaymentInitiated",
  "eventId": "300e8400-e29b-41d4-a716-446655440000",
  "occurredOn": "2025-10-25T10:05:30Z",
  "paymentId": "PAY-2025-001",
  "orderId": "ORD-2025-001",
  "customerId": "CUST-001",
  "amount": {
    "amount": 37695,
    "currency": "TWD"
  },
  "paymentMethod": "CREDIT_CARD"
}
```

### Event Handlers

- `PaymentGatewayHandler`：使用 gateway 處理付款
- `PaymentTimeoutHandler`：設定付款逾時
- `FraudDetectionHandler`：檢查詐欺

---

## PaymentProcessedEvent

### 事件結構

```java
public record PaymentProcessedEvent(
    PaymentId paymentId,
    OrderId orderId,
    CustomerId customerId,
    Money paidAmount,
    String transactionId,
    PaymentMethod paymentMethod,
    LocalDateTime processedAt,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent { }
```

### 範例 JSON

```json
{
  "eventType": "PaymentProcessed",
  "eventId": "310e8400-e29b-41d4-a716-446655440001",
  "occurredOn": "2025-10-25T10:06:00Z",
  "paymentId": "PAY-2025-001",
  "orderId": "ORD-2025-001",
  "customerId": "CUST-001",
  "paidAmount": {
    "amount": 37695,
    "currency": "TWD"
  },
  "transactionId": "TXN-20251025-001",
  "paymentMethod": "CREDIT_CARD",
  "processedAt": "2025-10-25T10:06:00Z"
}
```

### Event Handlers

- `OrderConfirmationHandler`：確認訂單
- `PaymentReceiptHandler`：產生收據
- `AccountingHandler`：記錄交易

---

## PaymentFailedEvent

### 事件結構

```java
public record PaymentFailedEvent(
    PaymentId paymentId,
    OrderId orderId,
    CustomerId customerId,
    Money attemptedAmount,
    String failureReason,
    String errorCode,
    LocalDateTime failedAt,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent { }
```

### 範例 JSON

```json
{
  "eventType": "PaymentFailed",
  "eventId": "320e8400-e29b-41d4-a716-446655440002",
  "occurredOn": "2025-10-25T10:06:00Z",
  "paymentId": "PAY-2025-002",
  "orderId": "ORD-2025-002",
  "customerId": "CUST-002",
  "attemptedAmount": {
    "amount": 15000,
    "currency": "TWD"
  },
  "failureReason": "信用卡餘額不足",
  "errorCode": "INSUFFICIENT_FUNDS",
  "failedAt": "2025-10-25T10:06:00Z"
}
```

### Event Handlers

- `OrderCancellationHandler`：取消訂單
- `PaymentRetryHandler`：安排重試（如果適用）
- `CustomerNotificationHandler`：通知客戶

---

## PaymentRefundedEvent

### 事件結構

```java
public record PaymentRefundedEvent(
    RefundId refundId,
    PaymentId originalPaymentId,
    OrderId orderId,
    CustomerId customerId,
    Money refundAmount,
    String refundReason,
    LocalDateTime refundedAt,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent { }
```

### 範例 JSON

```json
{
  "eventType": "PaymentRefunded",
  "eventId": "330e8400-e29b-41d4-a716-446655440003",
  "occurredOn": "2025-10-29T15:00:00Z",
  "refundId": "REF-2025-001",
  "originalPaymentId": "PAY-2025-001",
  "orderId": "ORD-2025-001",
  "customerId": "CUST-001",
  "refundAmount": {
    "amount": 37695,
    "currency": "TWD"
  },
  "refundReason": "產品瑕疵退貨",
  "refundedAt": "2025-10-29T15:00:00Z"
}
```

### Event Handlers

- `RefundNotificationHandler`：通知客戶
- `AccountingHandler`：記錄退款交易
- `OrderStatusHandler`：更新訂單狀態

---

**文件版本**: 1.0
**最後更新**: 2025-10-25
**負責人**: Payment Domain Team
