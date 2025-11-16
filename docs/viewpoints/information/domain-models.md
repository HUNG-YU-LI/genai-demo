---
title: "Domain Models"
viewpoint: "Information"
status: "Active"
last_updated: "2025-10-23"
related_documents:

  - "overview.md"
  - "data-ownership.md"
  - "data-flow.md"

---

# Domain Models

## 概述

本文件描述系統中每個 bounded context 的 domain models。每個 model 包含 entities、value objects 和 aggregates，代表核心業務概念及其關係。

## Model Organization

每個 bounded context 維護自己的 domain model，包含：

- **Aggregate Roots**: 定義一致性邊界的頂層 entities
- **Entities**: Aggregate 內具有唯一身分的物件
- **Value Objects**: 由其屬性定義的不可變物件
- **Domain Events**: 狀態變更通知

---

## Customer Context

### Purpose

管理 customer profiles、認證和偏好設定。

### Aggregate: Customer

**Aggregate Root**: `Customer`

#### Entities

- **Customer**（Root）
  - `CustomerId`（Identity）
  - `CustomerName`（Value Object）
  - `Email`（Value Object）
  - `Phone`（Value Object）
  - `MembershipLevel`（Enum: STANDARD、PREMIUM、VIP）
  - `RegistrationDate`（LocalDate）
  - `Status`（Enum: ACTIVE、SUSPENDED、DELETED）

- **CustomerAddress**
  - `AddressId`（Identity）
  - `Address`（Value Object）
  - `AddressType`（Enum: BILLING、SHIPPING）
  - `IsDefault`（Boolean）

#### Value Objects

- **CustomerName**
  - `firstName: String`
  - `lastName: String`
  - Validation: 非空，最多 100 個字元

- **Email**
  - `value: String`
  - Validation: 有效的 email 格式，唯一

- **Phone**
  - `value: String`
  - Validation: 有效的電話格式

- **Address**
  - `street: String`
  - `city: String`
  - `state: String`
  - `postalCode: String`
  - `country: String`

#### Domain Events

- `CustomerRegisteredEvent`
- `CustomerProfileUpdatedEvent`
- `CustomerAddressAddedEvent`
- `CustomerMembershipUpgradedEvent`
- `CustomerSuspendedEvent`

#### Relationships

```text
Customer (1) ----< (0..*) CustomerAddress
```

---

## Order Context

### Purpose

管理訂單從建立到履行的生命週期。

### Aggregate: Order

**Aggregate Root**: `Order`

#### Entities

- **Order**（Root）
  - `OrderId`（Identity）
  - `CustomerId`（Reference to Customer Context）
  - `OrderDate`（LocalDateTime）
  - `Status`（Enum: CREATED、PENDING、CONFIRMED、SHIPPED、DELIVERED、CANCELLED）
  - `TotalAmount`（Money Value Object）
  - `ShippingAddress`（Address Value Object）
  - `BillingAddress`（Address Value Object）

- **OrderItem**
  - `OrderItemId`（Identity）
  - `ProductId`（Reference to Product Context）
  - `ProductName`（String - snapshot）
  - `Quantity`（Integer）
  - `UnitPrice`（Money Value Object）
  - `Subtotal`（Money Value Object）

#### Value Objects

- **Money**
  - `amount: BigDecimal`
  - `currency: Currency`
  - Operations: add、subtract、multiply
  - Validation: 非負數，最多 2 位小數

- **OrderId**
  - `value: String`
  - Format: "ORD-{UUID}"

#### Domain Events

- `OrderCreatedEvent`
- `OrderSubmittedEvent`
- `OrderConfirmedEvent`
- `OrderShippedEvent`
- `OrderDeliveredEvent`
- `OrderCancelledEvent`
- `OrderItemAddedEvent`
- `OrderItemRemovedEvent`

#### Relationships

```text
Order (1) ----< (1..*) OrderItem
Order (1) ---- (1) ShippingAddress
Order (1) ---- (1) BillingAddress
```

#### Business Rules

- Order 必須至少有一個 item
- Order 總額必須等於 item subtotals 的總和
- 只有當狀態為 CREATED 或 PENDING 時，Order 才能取消
- Order 確認後（CONFIRMED），order items 不能修改

---

## Product Context

### Purpose

管理產品目錄、規格和庫存資訊。

### Aggregate: Product

**Aggregate Root**: `Product`

#### Entities

- **Product**（Root）
  - `ProductId`（Identity）
  - `ProductName`（String）
  - `Description`（String）
  - `Category`（Category Value Object）
  - `Price`（Money Value Object）
  - `Status`（Enum: ACTIVE、DISCONTINUED、OUT_OF_STOCK）
  - `SellerId`（Reference to Seller Context）

- **ProductSpecification**
  - `SpecificationId`（Identity）
  - `AttributeName`（String）
  - `AttributeValue`（String）

- **ProductImage**
  - `ImageId`（Identity）
  - `ImageUrl`（String）
  - `IsPrimary`（Boolean）
  - `DisplayOrder`（Integer）

#### Value Objects

- **Category**
  - `categoryId: String`
  - `categoryName: String`
  - `parentCategoryId: String`（optional）

- **ProductId**
  - `value: String`
  - Format: "PRD-{UUID}"

#### Domain Events

- `ProductCreatedEvent`
- `ProductUpdatedEvent`
- `ProductPriceChangedEvent`
- `ProductDiscontinuedEvent`
- `ProductImageAddedEvent`

#### Relationships

```text
Product (1) ----< (0..*) ProductSpecification
Product (1) ----< (0..*) ProductImage
Product (1) ---- (1) Category
```

---

## Inventory Context

### Purpose

管理庫存水準、保留和庫存移動。

### Aggregate: InventoryItem

**Aggregate Root**: `InventoryItem`

#### Entities

- **InventoryItem**（Root）
  - `InventoryItemId`（Identity）
  - `ProductId`（Reference to Product Context）
  - `WarehouseId`（String）
  - `QuantityOnHand`（Integer）
  - `QuantityReserved`（Integer）
  - `QuantityAvailable`（Integer - calculated）
  - `ReorderPoint`（Integer）
  - `ReorderQuantity`（Integer）

- **InventoryReservation**
  - `ReservationId`（Identity）
  - `OrderId`（Reference to Order Context）
  - `Quantity`（Integer）
  - `ReservedAt`（LocalDateTime）
  - `ExpiresAt`（LocalDateTime）
  - `Status`（Enum: ACTIVE、FULFILLED、EXPIRED、CANCELLED）

#### Value Objects

- **StockLevel**
  - `onHand: Integer`
  - `reserved: Integer`
  - `available: Integer`（calculated: onHand - reserved）
  - Validation: onHand >= 0、reserved >= 0

#### Domain Events

- `InventoryReceivedEvent`
- `InventoryReservedEvent`
- `InventoryReservationExpiredEvent`
- `InventoryReservationCancelledEvent`
- `InventoryFulfilledEvent`
- `LowStockAlertEvent`

#### Relationships

```text
InventoryItem (1) ----< (0..*) InventoryReservation
```

#### Business Rules

- 可用數量 = 現有數量 - 保留數量
- 如果未履行，保留在 15 分鐘後過期
- 不能保留超過可用數量
- 當可用 < 重新訂購點時發出低庫存警告

---

## Payment Context

### Purpose

管理付款處理、交易和付款方式。

### Aggregate: Payment

**Aggregate Root**: `Payment`

#### Entities

- **Payment**（Root）
  - `PaymentId`（Identity）
  - `OrderId`（Reference to Order Context）
  - `CustomerId`（Reference to Customer Context）
  - `Amount`（Money Value Object）
  - `PaymentMethod`（PaymentMethod Value Object）
  - `Status`（Enum: PENDING、AUTHORIZED、CAPTURED、FAILED、REFUNDED）
  - `TransactionId`（String - from payment gateway）
  - `CreatedAt`（LocalDateTime）
  - `ProcessedAt`（LocalDateTime）

- **PaymentTransaction**
  - `TransactionId`（Identity）
  - `TransactionType`（Enum: AUTHORIZATION、CAPTURE、REFUND）
  - `Amount`（Money Value Object）
  - `Status`（Enum: SUCCESS、FAILED）
  - `GatewayResponse`（String）
  - `ProcessedAt`（LocalDateTime）

#### Value Objects

- **PaymentMethod**
  - `type: PaymentMethodType`（Enum: CREDIT_CARD、DEBIT_CARD、PAYPAL、BANK_TRANSFER）
  - `last4Digits: String`（for cards）
  - `expiryDate: YearMonth`（for cards）
  - `cardBrand: String`（for cards）

- **PaymentId**
  - `value: String`
  - Format: "PAY-{UUID}"

#### Domain Events

- `PaymentInitiatedEvent`
- `PaymentAuthorizedEvent`
- `PaymentCapturedEvent`
- `PaymentFailedEvent`
- `PaymentRefundedEvent`

#### Relationships

```text
Payment (1) ----< (1..*) PaymentTransaction
Payment (1) ---- (1) PaymentMethod
```

#### Business Rules

- Payment 必須在 capture 前先授權
- Refund 金額不能超過 captured 金額
- 失敗的 payments 最多可重試 3 次
- 如果未 captured，Payment 在 24 小時後過期

---

## Shopping Cart Context

### Purpose

在訂單建立前管理活躍的購物車和購物車項目。

### Aggregate: ShoppingCart

**Aggregate Root**: `ShoppingCart`

#### Entities

- **ShoppingCart**（Root）
  - `CartId`（Identity）
  - `CustomerId`（Reference to Customer Context）
  - `Status`（Enum: ACTIVE、ABANDONED、CONVERTED）
  - `CreatedAt`（LocalDateTime）
  - `UpdatedAt`（LocalDateTime）
  - `ExpiresAt`（LocalDateTime）

- **CartItem**
  - `CartItemId`（Identity）
  - `ProductId`（Reference to Product Context）
  - `ProductName`（String - snapshot）
  - `Quantity`（Integer）
  - `UnitPrice`（Money Value Object）
  - `Subtotal`（Money Value Object）
  - `AddedAt`（LocalDateTime）

#### Value Objects

- **CartId**
  - `value: String`
  - Format: "CART-{UUID}"

#### Domain Events

- `CartCreatedEvent`
- `ItemAddedToCartEvent`
- `ItemRemovedFromCartEvent`
- `ItemQuantityUpdatedEvent`
- `CartAbandonedEvent`
- `CartConvertedToOrderEvent`

#### Relationships

```text
ShoppingCart (1) ----< (0..*) CartItem
```

#### Business Rules

- Cart 在 7 天不活動後過期
- Cart item 數量必須 > 0
- Cart 總額 = 所有 item subtotals 的總和
- 只有當 Cart 有 items 時才能轉換為訂單

---

## Promotion Context

### Purpose

管理折扣規則、促銷活動和優惠券代碼。

### Aggregate: Promotion

**Aggregate Root**: `Promotion`

#### Entities

- **Promotion**（Root）
  - `PromotionId`（Identity）
  - `PromotionName`（String）
  - `Description`（String）
  - `DiscountType`（Enum: PERCENTAGE、FIXED_AMOUNT、BUY_X_GET_Y）
  - `DiscountValue`（BigDecimal）
  - `MinimumPurchaseAmount`（Money Value Object）
  - `StartDate`（LocalDateTime）
  - `EndDate`（LocalDateTime）
  - `Status`（Enum: DRAFT、ACTIVE、EXPIRED、CANCELLED）
  - `UsageLimit`（Integer）
  - `UsageCount`（Integer）

- **PromotionRule**
  - `RuleId`（Identity）
  - `RuleType`（Enum: PRODUCT_CATEGORY、CUSTOMER_SEGMENT、ORDER_AMOUNT）
  - `RuleCondition`（String - JSON）

#### Value Objects

- **CouponCode**
  - `code: String`
  - `promotionId: PromotionId`
  - `usageLimit: Integer`
  - `usageCount: Integer`
  - Validation: 唯一、英數字元

#### Domain Events

- `PromotionCreatedEvent`
- `PromotionActivatedEvent`
- `PromotionExpiredEvent`
- `PromotionAppliedEvent`
- `CouponCodeGeneratedEvent`

#### Relationships

```text
Promotion (1) ----< (0..*) PromotionRule
Promotion (1) ----< (0..*) CouponCode
```

---

## Review Context

### Purpose

管理來自 customers 的產品評論和評分。

### Aggregate: Review

**Aggregate Root**: `Review`

#### Entities

- **Review**（Root）
  - `ReviewId`（Identity）
  - `ProductId`（Reference to Product Context）
  - `CustomerId`（Reference to Customer Context）
  - `OrderId`（Reference to Order Context）
  - `Rating`（Integer: 1-5）
  - `Title`（String）
  - `Content`（String）
  - `Status`（Enum: PENDING、APPROVED、REJECTED）
  - `CreatedAt`（LocalDateTime）
  - `UpdatedAt`（LocalDateTime）

- **ReviewImage**
  - `ImageId`（Identity）
  - `ImageUrl`（String）
  - `DisplayOrder`（Integer）

- **ReviewComment**
  - `CommentId`（Identity）
  - `UserId`（String）
  - `Content`（String）
  - `CreatedAt`（LocalDateTime）

#### Domain Events

- `ReviewSubmittedEvent`
- `ReviewApprovedEvent`
- `ReviewRejectedEvent`
- `ReviewUpdatedEvent`
- `ReviewCommentAddedEvent`

#### Relationships

```text
Review (1) ----< (0..*) ReviewImage
Review (1) ----< (0..*) ReviewComment
```

#### Business Rules

- Customer 只能評論他們已購買的產品
- 每個 customer 每個產品只能一個評論
- Rating 必須在 1 到 5 之間
- Review 在發布前需要審核

---

## Shared Value Objects

這些 value objects 在多個 bounded contexts 中使用：

### Money

```java
public record Money(BigDecimal amount, Currency currency) {
    public Money {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be non-negative");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency is required");
        }
    }
    
    public Money add(Money other) {
        validateSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }
    
    public Money multiply(int factor) {
        return new Money(amount.multiply(BigDecimal.valueOf(factor)), currency);
    }
}
```

### Address

```java
public record Address(
    String street,
    String city,
    String state,
    String postalCode,
    String country
) {
    public Address {
        if (street == null || street.isBlank()) {
            throw new IllegalArgumentException("Street is required");
        }
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("City is required");
        }
        if (postalCode == null || postalCode.isBlank()) {
            throw new IllegalArgumentException("Postal code is required");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Country is required");
        }
    }
}
```

### Email

```java
public record Email(String value) {
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    
    public Email {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + value);
        }
    }
}
```

## Model Diagrams

### Entity Relationship Diagrams

- [Customer Context ER Diagram](../../diagrams/generated/information/customer-context-er.png)
- [Order Context ER Diagram](../../diagrams/generated/information/order-context-er.png)
- [Product Context ER Diagram](../../diagrams/generated/information/product-context-er.png)
- [Inventory Context ER Diagram](../../diagrams/generated/information/inventory-context-er.png)
- [Payment Context ER Diagram](../../diagrams/generated/information/payment-context-er.png)
- [Shopping Cart Context ER Diagram](../../diagrams/generated/information/shopping-cart-context-er.png)

### Aggregate Diagrams

- [Order Aggregate](../../diagrams/generated/information/order-aggregate.png)
- [Customer Aggregate](../../diagrams/generated/information/customer-aggregate.png)
- [Product Aggregate](../../diagrams/generated/information/product-aggregate.png)

## Related Documentation

- [Information Viewpoint Overview](overview.md)
- [Data Ownership](data-ownership.md)
- [Data Flow](data-flow.md)
- [Domain Events Catalog](../../api/events/README.md)

---

**Document Status**: Active
**Last Review**: 2025-10-23
**Next Review**: 2026-01-23
**Owner**: Architecture Team
