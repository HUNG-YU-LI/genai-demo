# Order Context Events

## 概述

本文件描述由 Order bounded context 發布的所有 domain events。這些 events 捕捉從建立到交付的完整訂單生命週期，包括訂單修改、取消和退貨。

**最後更新**: 2025-10-25

---

## Event 清單

| Event Name | Trigger | Frequency | Priority |
|------------|---------|-----------|----------|
| `OrderCreatedEvent` | 結帳啟動 | Very High | P0 |
| `OrderSubmittedEvent` | 訂單提交 | Very High | P0 |
| `OrderConfirmedEvent` | 付款成功 | Very High | P0 |
| `OrderCancelledEvent` | 訂單取消 | Medium | P1 |
| `OrderShippedEvent` | 出貨派送 | High | P0 |
| `OrderDeliveredEvent` | 送達確認 | High | P0 |
| `OrderReturnedEvent` | 退貨請求 | Low | P1 |
| `OrderRefundedEvent` | 退款處理 | Low | P1 |
| `OrderItemAddedEvent` | 項目新增 | Medium | P2 |
| `OrderItemRemovedEvent` | 項目移除 | Medium | P2 |
| `OrderItemQuantityChangedEvent` | 數量更新 | Medium | P2 |

---

## OrderCreatedEvent

### 描述

當在結帳啟動期間從購物車建立新訂單時發布。

### Event 結構

```java
public record OrderCreatedEvent(
    OrderId orderId,
    CustomerId customerId,
    List<OrderItemDto> items,
    Money subtotal,
    ShippingAddress shippingAddress,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent {

    public static OrderCreatedEvent create(
        OrderId orderId,
        CustomerId customerId,
        List<OrderItem> items,
        Money subtotal,
        ShippingAddress shippingAddress
    ) {
        var metadata = DomainEvent.createEventMetadata();
        return new OrderCreatedEvent(
            orderId, customerId,
            items.stream().map(OrderItemDto::from).toList(),
            subtotal, shippingAddress,
            metadata.eventId(), metadata.occurredOn()
        );
    }
}
```

### Payload 欄位

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `orderId` | OrderId | Yes | 唯一訂單識別碼 |
| `customerId` | CustomerId | Yes | 建立訂單的客戶 |
| `items` | List<OrderItemDto> | Yes | 訂單項目清單 |
| `subtotal` | Money | Yes | 稅金和運費前的訂單小計 |
| `shippingAddress` | ShippingAddress | Yes | 配送地址 |
| `eventId` | UUID | Yes | 唯一 event 識別碼 |
| `occurredOn` | LocalDateTime | Yes | Event 時間戳記 |

### 範例 JSON

```json
{
  "eventType": "OrderCreated",
  "eventId": "100e8400-e29b-41d4-a716-446655440000",
  "occurredOn": "2025-10-25T10:00:00Z",
  "orderId": "ORD-2025-001",
  "customerId": "CUST-001",
  "items": [
    {
      "productId": "PROD-001",
      "productName": "iPhone 15 Pro",
      "quantity": 1,
      "unitPrice": {
        "amount": 35900,
        "currency": "TWD"
      }
    }
  ],
  "subtotal": {
    "amount": 35900,
    "currency": "TWD"
  },
  "shippingAddress": {
    "recipientName": "張小明",
    "street": "台北市信義區信義路五段7號",
    "city": "台北市",
    "postalCode": "110",
    "phone": "+886-912-345-678"
  }
}
```

### Event Handlers

| Handler | Action | Context |
|---------|--------|---------|
| `OrderValidationHandler` | 驗證訂單詳細資料 | Order |
| `InventoryCheckHandler` | 檢查產品可用性 | Inventory |
| `PricingCalculationHandler` | 計算最終價格 | Pricing |

### 相關 Events

- 觸發: `PriceCalculatedEvent`, `TaxCalculatedEvent`
- 接續: `CartCheckedOutEvent`
- 先於: `OrderSubmittedEvent`

---

## OrderSubmittedEvent

### 描述

當客戶在檢視訂單詳細資料後提交訂單進行處理時發布。

### Event 結構

```java
public record OrderSubmittedEvent(
    OrderId orderId,
    CustomerId customerId,
    Money totalAmount,
    int itemCount,
    PaymentMethod paymentMethod,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent {

    public static OrderSubmittedEvent create(
        OrderId orderId,
        CustomerId customerId,
        Money totalAmount,
        int itemCount,
        PaymentMethod paymentMethod
    ) {
        var metadata = DomainEvent.createEventMetadata();
        return new OrderSubmittedEvent(
            orderId, customerId, totalAmount, itemCount, paymentMethod,
            metadata.eventId(), metadata.occurredOn()
        );
    }
}
```

### Payload 欄位

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `orderId` | OrderId | Yes | 訂單識別碼 |
| `customerId` | CustomerId | Yes | 客戶識別碼 |
| `totalAmount` | Money | Yes | 包含稅金和運費的訂單總金額 |
| `itemCount` | int | Yes | 項目總數 |
| `paymentMethod` | PaymentMethod | Yes | 選擇的付款方式 |
| `eventId` | UUID | Yes | 唯一 event 識別碼 |
| `occurredOn` | LocalDateTime | Yes | Event 時間戳記 |

### 範例 JSON

```json
{
  "eventType": "OrderSubmitted",
  "eventId": "110e8400-e29b-41d4-a716-446655440001",
  "occurredOn": "2025-10-25T10:05:00Z",
  "orderId": "ORD-2025-001",
  "customerId": "CUST-001",
  "totalAmount": {
    "amount": 37695,
    "currency": "TWD"
  },
  "itemCount": 1,
  "paymentMethod": "CREDIT_CARD"
}
```

### Event Handlers

| Handler | Action | Context |
|---------|--------|---------|
| `PaymentInitiationHandler` | 啟動付款處理 | Payment |
| `InventoryReservationHandler` | 保留庫存 | Inventory |
| `OrderStatusHandler` | 更新訂單狀態為 PENDING | Order |

### 相關 Events

- 觸發: `PaymentInitiatedEvent`, `InventoryReservedEvent`
- 接續: `OrderCreatedEvent`
- 先於: `OrderConfirmedEvent`

---

## OrderConfirmedEvent

### 描述

當訂單在付款處理成功後確認時發布。

### Event 結構

```java
public record OrderConfirmedEvent(
    OrderId orderId,
    CustomerId customerId,
    PaymentId paymentId,
    Money paidAmount,
    LocalDateTime confirmedAt,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent {

    public static OrderConfirmedEvent create(
        OrderId orderId,
        CustomerId customerId,
        PaymentId paymentId,
        Money paidAmount
    ) {
        var metadata = DomainEvent.createEventMetadata();
        return new OrderConfirmedEvent(
            orderId, customerId, paymentId, paidAmount,
            LocalDateTime.now(),
            metadata.eventId(), metadata.occurredOn()
        );
    }
}
```

### Payload 欄位

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `orderId` | OrderId | Yes | 訂單識別碼 |
| `customerId` | CustomerId | Yes | 客戶識別碼 |
| `paymentId` | PaymentId | Yes | 付款交易識別碼 |
| `paidAmount` | Money | Yes | 已付金額 |
| `confirmedAt` | LocalDateTime | Yes | 確認時間戳記 |
| `eventId` | UUID | Yes | 唯一 event 識別碼 |
| `occurredOn` | LocalDateTime | Yes | Event 時間戳記 |

### 範例 JSON

```json
{
  "eventType": "OrderConfirmed",
  "eventId": "120e8400-e29b-41d4-a716-446655440002",
  "occurredOn": "2025-10-25T10:10:00Z",
  "orderId": "ORD-2025-001",
  "customerId": "CUST-001",
  "paymentId": "PAY-2025-001",
  "paidAmount": {
    "amount": 37695,
    "currency": "TWD"
  },
  "confirmedAt": "2025-10-25T10:10:00Z"
}
```

### Event Handlers

| Handler | Action | Context |
|---------|--------|---------|
| `ShippingScheduleHandler` | 排程出貨 | Shipping |
| `OrderConfirmationEmailHandler` | 發送確認 email | Notification |
| `InvoiceGenerationHandler` | 產生發票 | Billing |
| `LoyaltyPointsHandler` | 獎勵忠誠點數 | Promotion |

### 相關 Events

- 觸發: `ShippingScheduledEvent`, `NotificationSentEvent`
- 接續: `PaymentProcessedEvent`
- 先於: `OrderShippedEvent`

---

## OrderCancelledEvent

### 描述

當訂單被客戶或系統取消時發布（例如付款失敗、缺貨）。

### Event 結構

```java
public record OrderCancelledEvent(
    OrderId orderId,
    CustomerId customerId,
    String cancellationReason,
    CancellationSource source,
    LocalDateTime cancelledAt,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent {

    public static OrderCancelledEvent create(
        OrderId orderId,
        CustomerId customerId,
        String cancellationReason,
        CancellationSource source
    ) {
        var metadata = DomainEvent.createEventMetadata();
        return new OrderCancelledEvent(
            orderId, customerId, cancellationReason, source,
            LocalDateTime.now(),
            metadata.eventId(), metadata.occurredOn()
        );
    }
}
```

### Payload 欄位

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `orderId` | OrderId | Yes | 訂單識別碼 |
| `customerId` | CustomerId | Yes | 客戶識別碼 |
| `cancellationReason` | String | Yes | 取消原因 |
| `source` | CancellationSource | Yes | 取消者 (CUSTOMER, SYSTEM, ADMIN) |
| `cancelledAt` | LocalDateTime | Yes | 取消時間戳記 |
| `eventId` | UUID | Yes | 唯一 event 識別碼 |
| `occurredOn` | LocalDateTime | Yes | Event 時間戳記 |

### 範例 JSON

```json
{
  "eventType": "OrderCancelled",
  "eventId": "130e8400-e29b-41d4-a716-446655440003",
  "occurredOn": "2025-10-25T11:00:00Z",
  "orderId": "ORD-2025-001",
  "customerId": "CUST-001",
  "cancellationReason": "Customer requested cancellation",
  "source": "CUSTOMER",
  "cancelledAt": "2025-10-25T11:00:00Z"
}
```

### Event Handlers

| Handler | Action | Context |
|---------|--------|---------|
| `InventoryReleaseHandler` | 釋放保留的庫存 | Inventory |
| `PaymentRefundHandler` | 如果已付款則處理退款 | Payment |
| `CancellationEmailHandler` | 發送取消確認 | Notification |

### 相關 Events

- 觸發: `InventoryReleasedEvent`, `PaymentRefundedEvent`
- 可能接續: `OrderCreatedEvent`, `OrderSubmittedEvent`, `OrderConfirmedEvent`

---

## OrderShippedEvent

### 描述

當訂單出貨並派送給客戶時發布。

### Event 結構

```java
public record OrderShippedEvent(
    OrderId orderId,
    CustomerId customerId,
    ShippingId shippingId,
    String trackingNumber,
    String carrier,
    LocalDateTime shippedAt,
    LocalDateTime estimatedDelivery,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent {

    public static OrderShippedEvent create(
        OrderId orderId,
        CustomerId customerId,
        ShippingId shippingId,
        String trackingNumber,
        String carrier,
        LocalDateTime estimatedDelivery
    ) {
        var metadata = DomainEvent.createEventMetadata();
        return new OrderShippedEvent(
            orderId, customerId, shippingId, trackingNumber, carrier,
            LocalDateTime.now(), estimatedDelivery,
            metadata.eventId(), metadata.occurredOn()
        );
    }
}
```

### Payload 欄位

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `orderId` | OrderId | Yes | 訂單識別碼 |
| `customerId` | CustomerId | Yes | 客戶識別碼 |
| `shippingId` | ShippingId | Yes | 出貨記錄識別碼 |
| `trackingNumber` | String | Yes | 物流追蹤號碼 |
| `carrier` | String | Yes | 物流業者名稱 |
| `shippedAt` | LocalDateTime | Yes | 出貨時間戳記 |
| `estimatedDelivery` | LocalDateTime | Yes | 預計送達日期 |
| `eventId` | UUID | Yes | 唯一 event 識別碼 |
| `occurredOn` | LocalDateTime | Yes | Event 時間戳記 |

### 範例 JSON

```json
{
  "eventType": "OrderShipped",
  "eventId": "140e8400-e29b-41d4-a716-446655440004",
  "occurredOn": "2025-10-26T09:00:00Z",
  "orderId": "ORD-2025-001",
  "customerId": "CUST-001",
  "shippingId": "SHIP-2025-001",
  "trackingNumber": "TW1234567890",
  "carrier": "黑貓宅急便",
  "shippedAt": "2025-10-26T09:00:00Z",
  "estimatedDelivery": "2025-10-27T18:00:00Z"
}
```

### Event Handlers

| Handler | Action | Context |
|---------|--------|---------|
| `ShippingNotificationHandler` | 發送出貨通知 | Notification |
| `TrackingUpdateHandler` | 啟用追蹤更新 | Shipping |
| `DeliveryScheduleHandler` | 排程送達 | Delivery |

### 相關 Events

- 觸發: `ShippingDispatchedEvent`, `NotificationSentEvent`
- 接續: `OrderConfirmedEvent`
- 先於: `OrderDeliveredEvent`

---

## OrderDeliveredEvent

### 描述

當訂單成功送達給客戶時發布。

### Event 結構

```java
public record OrderDeliveredEvent(
    OrderId orderId,
    CustomerId customerId,
    ShippingId shippingId,
    LocalDateTime deliveredAt,
    String deliverySignature,
    DeliveryStatus deliveryStatus,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent {

    public static OrderDeliveredEvent create(
        OrderId orderId,
        CustomerId customerId,
        ShippingId shippingId,
        String deliverySignature,
        DeliveryStatus deliveryStatus
    ) {
        var metadata = DomainEvent.createEventMetadata();
        return new OrderDeliveredEvent(
            orderId, customerId, shippingId,
            LocalDateTime.now(), deliverySignature, deliveryStatus,
            metadata.eventId(), metadata.occurredOn()
        );
    }
}
```

### Payload 欄位

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `orderId` | OrderId | Yes | 訂單識別碼 |
| `customerId` | CustomerId | Yes | 客戶識別碼 |
| `shippingId` | ShippingId | Yes | 出貨記錄識別碼 |
| `deliveredAt` | LocalDateTime | Yes | 送達時間戳記 |
| `deliverySignature` | String | No | 送達簽名/證明 |
| `deliveryStatus` | DeliveryStatus | Yes | 送達狀態 (DELIVERED, LEFT_AT_DOOR, etc.) |
| `eventId` | UUID | Yes | 唯一 event 識別碼 |
| `occurredOn` | LocalDateTime | Yes | Event 時間戳記 |

### 範例 JSON

```json
{
  "eventType": "OrderDelivered",
  "eventId": "150e8400-e29b-41d4-a716-446655440005",
  "occurredOn": "2025-10-27T14:30:00Z",
  "orderId": "ORD-2025-001",
  "customerId": "CUST-001",
  "shippingId": "SHIP-2025-001",
  "deliveredAt": "2025-10-27T14:30:00Z",
  "deliverySignature": "張小明",
  "deliveryStatus": "DELIVERED"
}
```

### Event Handlers

| Handler | Action | Context |
|---------|--------|---------|
| `DeliveryConfirmationHandler` | 發送送達確認 | Notification |
| `ReviewRequestHandler` | 請求產品評價 | Review |
| `OrderCompletionHandler` | 標記訂單為已完成 | Order |

### 相關 Events

- 觸發: `NotificationSentEvent`, `ReviewRequestedEvent`
- 接續: `OrderShippedEvent`

---

## OrderReturnedEvent

### 描述

當客戶對已送達的訂單發起退貨時發布。

### Event 結構

```java
public record OrderReturnedEvent(
    OrderId orderId,
    CustomerId customerId,
    ReturnId returnId,
    List<ReturnItemDto> returnItems,
    String returnReason,
    LocalDateTime returnRequestedAt,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent {

    public static OrderReturnedEvent create(
        OrderId orderId,
        CustomerId customerId,
        ReturnId returnId,
        List<ReturnItem> returnItems,
        String returnReason
    ) {
        var metadata = DomainEvent.createEventMetadata();
        return new OrderReturnedEvent(
            orderId, customerId, returnId,
            returnItems.stream().map(ReturnItemDto::from).toList(),
            returnReason, LocalDateTime.now(),
            metadata.eventId(), metadata.occurredOn()
        );
    }
}
```

### Payload 欄位

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `orderId` | OrderId | Yes | 訂單識別碼 |
| `customerId` | CustomerId | Yes | 客戶識別碼 |
| `returnId` | ReturnId | Yes | 退貨請求識別碼 |
| `returnItems` | List<ReturnItemDto> | Yes | 退貨項目 |
| `returnReason` | String | Yes | 退貨原因 |
| `returnRequestedAt` | LocalDateTime | Yes | 退貨請求時間戳記 |
| `eventId` | UUID | Yes | 唯一 event 識別碼 |
| `occurredOn` | LocalDateTime | Yes | Event 時間戳記 |

### 範例 JSON

```json
{
  "eventType": "OrderReturned",
  "eventId": "160e8400-e29b-41d4-a716-446655440006",
  "occurredOn": "2025-10-28T10:00:00Z",
  "orderId": "ORD-2025-001",
  "customerId": "CUST-001",
  "returnId": "RET-2025-001",
  "returnItems": [
    {
      "productId": "PROD-001",
      "quantity": 1,
      "reason": "產品瑕疵"
    }
  ],
  "returnReason": "產品有瑕疵，要求退貨",
  "returnRequestedAt": "2025-10-28T10:00:00Z"
}
```

### Event Handlers

| Handler | Action | Context |
|---------|--------|---------|
| `ReturnProcessingHandler` | 處理退貨請求 | Order |
| `ReturnLabelHandler` | 產生退貨物流標籤 | Shipping |
| `ReturnNotificationHandler` | 發送退貨確認 | Notification |

### 相關 Events

- 觸發: `ReturnLabelCreatedEvent`
- 接續: `OrderDeliveredEvent`
- 先於: `OrderRefundedEvent`

---

## OrderRefundedEvent

### 描述

當針對退貨或取消的訂單處理退款時發布。

### Event 結構

```java
public record OrderRefundedEvent(
    OrderId orderId,
    CustomerId customerId,
    RefundId refundId,
    Money refundAmount,
    String refundReason,
    RefundMethod refundMethod,
    LocalDateTime refundedAt,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent {

    public static OrderRefundedEvent create(
        OrderId orderId,
        CustomerId customerId,
        RefundId refundId,
        Money refundAmount,
        String refundReason,
        RefundMethod refundMethod
    ) {
        var metadata = DomainEvent.createEventMetadata();
        return new OrderRefundedEvent(
            orderId, customerId, refundId, refundAmount,
            refundReason, refundMethod, LocalDateTime.now(),
            metadata.eventId(), metadata.occurredOn()
        );
    }
}
```

### Payload 欄位

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `orderId` | OrderId | Yes | 訂單識別碼 |
| `customerId` | CustomerId | Yes | 客戶識別碼 |
| `refundId` | RefundId | Yes | 退款交易識別碼 |
| `refundAmount` | Money | Yes | 退款金額 |
| `refundReason` | String | Yes | 退款原因 |
| `refundMethod` | RefundMethod | Yes | 退款方式 (ORIGINAL_PAYMENT, STORE_CREDIT) |
| `refundedAt` | LocalDateTime | Yes | 退款時間戳記 |
| `eventId` | UUID | Yes | 唯一 event 識別碼 |
| `occurredOn` | LocalDateTime | Yes | Event 時間戳記 |

### 範例 JSON

```json
{
  "eventType": "OrderRefunded",
  "eventId": "170e8400-e29b-41d4-a716-446655440007",
  "occurredOn": "2025-10-29T15:00:00Z",
  "orderId": "ORD-2025-001",
  "customerId": "CUST-001",
  "refundId": "REF-2025-001",
  "refundAmount": {
    "amount": 37695,
    "currency": "TWD"
  },
  "refundReason": "產品瑕疵退貨",
  "refundMethod": "ORIGINAL_PAYMENT",
  "refundedAt": "2025-10-29T15:00:00Z"
}
```

### Event Handlers

| Handler | Action | Context |
|---------|--------|---------|
| `PaymentRefundHandler` | 處理付款退款 | Payment |
| `InventoryRestockHandler` | 重新入庫退回項目 | Inventory |
| `RefundNotificationHandler` | 發送退款確認 | Notification |

### 相關 Events

- 觸發: `PaymentRefundedEvent`, `InventoryAdjustedEvent`
- 接續: `OrderReturnedEvent` 或 `OrderCancelledEvent`

---

## OrderItemAddedEvent

### 描述

當項目被新增到現有訂單時發布（提交前）。

### Event 結構

```java
public record OrderItemAddedEvent(
    OrderId orderId,
    ProductId productId,
    int quantity,
    Money unitPrice,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent { }
```

### 範例 JSON

```json
{
  "eventType": "OrderItemAdded",
  "eventId": "180e8400-e29b-41d4-a716-446655440008",
  "occurredOn": "2025-10-25T10:02:00Z",
  "orderId": "ORD-2025-001",
  "productId": "PROD-002",
  "quantity": 1,
  "unitPrice": {
    "amount": 1990,
    "currency": "TWD"
  }
}
```

---

## OrderItemRemovedEvent

### 描述

當項目從現有訂單中移除時發布（提交前）。

### Event 結構

```java
public record OrderItemRemovedEvent(
    OrderId orderId,
    ProductId productId,
    int removedQuantity,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent { }
```

---

## OrderItemQuantityChangedEvent

### 描述

當訂單中項目的數量變更時發布（提交前）。

### Event 結構

```java
public record OrderItemQuantityChangedEvent(
    OrderId orderId,
    ProductId productId,
    int oldQuantity,
    int newQuantity,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent { }
```

---

## Event Flow 圖表

### 完整訂單處理流程

```mermaid
sequenceDiagram
    participant C as Customer
    participant O as Order
    participant P as Payment
    participant I as Inventory
    participant S as Shipping
    participant N as Notification

    C->>O: Checkout
    O-->>N: OrderCreatedEvent
    C->>O: Submit Order
    O-->>P: OrderSubmittedEvent
    P->>P: Process Payment
    P-->>O: PaymentProcessedEvent
    O-->>I: OrderConfirmedEvent
    I->>I: Reserve Inventory
    I-->>S: InventoryReservedEvent
    S->>S: Schedule Shipping
    S-->>N: ShippingScheduledEvent
    N->>C: Order Confirmation Email
    S->>S: Dispatch Package
    S-->>N: OrderShippedEvent
    N->>C: Shipping Notification
    S->>S: Deliver Package
    S-->>O: OrderDeliveredEvent
    O-->>N: OrderDeliveredEvent
    N->>C: Delivery Confirmation
```

### 訂單取消流程

```mermaid
sequenceDiagram
    participant C as Customer
    participant O as Order
    participant I as Inventory
    participant P as Payment
    participant N as Notification

    C->>O: Cancel Order
    O-->>I: OrderCancelledEvent
    I->>I: Release Inventory
    O-->>P: OrderCancelledEvent
    P->>P: Process Refund
    P-->>N: PaymentRefundedEvent
    N->>C: Cancellation Confirmation
```

---

## 測試指南

### 單元測試

```java
@Test
void should_create_order_submitted_event_with_correct_data() {
    // Given
    OrderId orderId = OrderId.of("ORD-2025-001");
    CustomerId customerId = CustomerId.of("CUST-001");
    Money totalAmount = Money.of(37695, "TWD");

    // When
    OrderSubmittedEvent event = OrderSubmittedEvent.create(
        orderId, customerId, totalAmount, 1, PaymentMethod.CREDIT_CARD
    );

    // Then
    assertThat(event.orderId()).isEqualTo(orderId);
    assertThat(event.totalAmount()).isEqualTo(totalAmount);
    assertThat(event.eventId()).isNotNull();
}
```

### 整合測試

```java
@SpringBootTest
@ActiveProfiles("test")
class OrderEventIntegrationTest {

    @Test
    void should_publish_order_confirmed_event_after_payment_success() {
        // Given
        Order order = createTestOrder();
        Payment payment = processPayment(order);

        // When
        orderService.confirmOrder(order.getId(), payment.getId());

        // Then
        verify(eventPublisher).publish(any(OrderConfirmedEvent.class));
    }
}
```

---

## 相關文件

- **Event Catalog**: [event-catalog.md](../event-catalog.md)
- **Order API**: `docs/api/rest/endpoints/orders.md`
- **Payment Events**: [payment-events.md](payment-events.md)
- **Shipping Events**: [shipping-events.md](shipping-events.md)
- **Order Aggregate**: `docs/viewpoints/functional/bounded-contexts.md#order-context`

---

**文件版本**: 1.0
**最後更新**: 2025-10-25
**負責人**: Order Domain Team
