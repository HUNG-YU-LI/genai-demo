---
title: "Bounded Contexts"
type: "functional-viewpoint"
category: "functional"
stakeholders: ["architects", "developers", "business-analysts"]
last_updated: "2025-10-22"
version: "1.0"
status: "active"
owner: "Architecture Team"
related_docs:
  - "viewpoints/functional/overview.md"
  - "viewpoints/information/overview.md"
  - "perspectives/evolution/overview.md"
tags: ["ddd", "bounded-contexts", "domain-model", "context-map"]
---

# Bounded Contexts

> **Status**: ✅ Active
> **Last Updated**: 2025-10-22
> **Owner**: Architecture Team

## 概述

本文件描述企業電子商務平台中的所有 13 個 bounded contexts。每個 bounded context 代表一個獨特的業務能力，具有明確的邊界、自己的 domain model 和特定的職責。

這些 contexts 遵循 **Domain-Driven Design (DDD)** 策略設計原則組織，每個 context 都可以獨立部署和維護。

## Context Map

![Bounded Contexts Overview](../../diagrams/generated/functional/bounded-contexts-overview.png)

### Context 關係

- **Customer ↔ Order**: Customer 下訂單（透過 events）
- **Order ↔ Inventory**: Order 預留庫存（透過 events）
- **Order ↔ Payment**: Order 觸發付款處理（透過 events）
- **Order ↔ Delivery**: Order 啟動配送（透過 events）
- **Order ↔ Pricing**: Order 計算價格（透過 service call）
- **Product ↔ Inventory**: Product 庫存由 inventory 管理（透過 events）
- **Shopping Cart ↔ Product**: Cart 包含 products（透過 service call）
- **Promotion ↔ Pricing**: Promotions 影響定價（透過 events）
- **Review ↔ Product**: Reviews 與 products 關聯（透過 events）
- **Notification**: 監聽所有 contexts 的 events

## Bounded Contexts

### 1. Customer Context

**職責**：管理客戶生命週期、個人檔案、偏好設定和會員資格

**核心 Aggregate**：`Customer`

**關鍵 Entities**：

- `Customer` (Aggregate Root)
- `CustomerPreferences`
- `DeliveryAddress`
- `PaymentMethod`

**關鍵 Value Objects**：

- `CustomerId`
- `CustomerName`
- `Email`
- `Phone`
- `Address`
- `MembershipLevel` (STANDARD, SILVER, GOLD, PLATINUM)
- `CustomerStatus` (ACTIVE, INACTIVE, SUSPENDED)
- `RewardPoints`
- `NotificationPreferences`

**發布的 Domain Events**：

- `CustomerCreatedEvent`
- `CustomerProfileUpdatedEvent`
- `CustomerStatusChangedEvent`
- `MembershipLevelUpgradedEvent`
- `CustomerVipUpgradedEvent`
- `DeliveryAddressAddedEvent`
- `DeliveryAddressRemovedEvent`
- `NotificationPreferencesUpdatedEvent`
- `RewardPointsEarnedEvent`
- `RewardPointsRedeemedEvent`
- `CustomerSpendingUpdatedEvent`

**消費的 Domain Events**：

- `OrderCompletedEvent` (from Order Context) → 更新消費、獎勵點數
- `PaymentCompletedEvent` (from Payment Context) → 更新客戶統計資料

**REST API Endpoints**：

- `POST /api/v1/customers` - 註冊新客戶
- `GET /api/v1/customers/{id}` - 取得客戶詳細資訊
- `PUT /api/v1/customers/{id}` - 更新客戶個人檔案
- `POST /api/v1/customers/{id}/addresses` - 新增配送地址
- `PUT /api/v1/customers/{id}/preferences` - 更新偏好設定
- `GET /api/v1/customers/{id}/reward-points` - 取得獎勵點數餘額

**業務規則**：

- Email 在所有客戶中必須唯一
- 會員等級升級基於消費門檻
- 獎勵點數賺取：每消費 $10 獲得 1 點
- VIP 狀態需要 PLATINUM 會員資格 + $10,000 年度消費

---

### 2. Order Context

**職責**：管理訂單生命週期，從建立到完成

**核心 Aggregate**：`Order`

**關鍵 Entities**：

- `Order` (Aggregate Root)
- `OrderItem`
- `OrderHistory`

**關鍵 Value Objects**：

- `OrderId`
- `OrderStatus` (CREATED, PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED)
- `OrderItem`
- `ShippingAddress`
- `Money`
- `Quantity`

**發布的 Domain Events**：

- `OrderCreatedEvent`
- `OrderSubmittedEvent`
- `OrderConfirmedEvent`
- `OrderCancelledEvent`
- `OrderShippedEvent`
- `OrderDeliveredEvent`
- `OrderCompletedEvent`
- `OrderItemAddedEvent`
- `OrderItemRemovedEvent`

**消費的 Domain Events**：

- `InventoryReservedEvent` (from Inventory Context) → 確認訂單
- `PaymentCompletedEvent` (from Payment Context) → 繼續履行
- `PaymentFailedEvent` (from Payment Context) → 取消訂單
- `DeliveryScheduledEvent` (from Delivery Context) → 更新訂單狀態

**REST API Endpoints**：

- `POST /api/v1/orders` - 建立新訂單
- `GET /api/v1/orders/{id}` - 取得訂單詳細資訊
- `POST /api/v1/orders/{id}/submit` - 提交訂單進行處理
- `POST /api/v1/orders/{id}/cancel` - 取消訂單
- `GET /api/v1/orders?customerId={id}` - 列出客戶訂單
- `GET /api/v1/orders/{id}/history` - 取得訂單歷史記錄

**業務規則**：

- 訂單必須至少有一個項目
- 只有狀態為 CREATED 或 PENDING 的訂單才能取消
- 總金額必須符合項目價格總和加上運費
- 訂單確認需要成功的庫存預留和付款

---

### 3. Product Context

**職責**：管理產品目錄、類別和產品資訊

**核心 Aggregate**：`Product`

**關鍵 Entities**：

- `Product` (Aggregate Root)
- `ProductCategory`
- `ProductSpecification`

**關鍵 Value Objects**：

- `ProductId`
- `ProductName`
- `ProductDescription`
- `Price`
- `SKU`
- `ProductStatus` (ACTIVE, INACTIVE, DISCONTINUED)
- `CategoryId`

**發布的 Domain Events**：

- `ProductCreatedEvent`
- `ProductUpdatedEvent`
- `ProductPriceChangedEvent`
- `ProductStatusChangedEvent`
- `ProductDiscontinuedEvent`

**消費的 Domain Events**：

- `ReviewSubmittedEvent` (from Review Context) → 更新產品評分
- `InventoryDepletedEvent` (from Inventory Context) → 標記為缺貨

**REST API Endpoints**：

- `GET /api/v1/products` - 列出產品並篩選
- `GET /api/v1/products/{id}` - 取得產品詳細資訊
- `POST /api/v1/products` - 建立新產品（admin）
- `PUT /api/v1/products/{id}` - 更新產品（admin）
- `GET /api/v1/products/search?q={query}` - 搜尋產品
- `GET /api/v1/products/categories` - 列出類別

**業務規則**：

- SKU 在所有產品中必須唯一
- 價格必須為正數
- 如果在有效訂單中被引用，產品不能被刪除
- 已停產的產品不能加入新訂單

---

### 4. Inventory Context

**職責**：管理產品庫存水平和庫存操作

**核心 Aggregate**：`InventoryItem`

**關鍵 Entities**：

- `InventoryItem` (Aggregate Root)
- `StockMovement`
- `Reservation`

**關鍵 Value Objects**：

- `InventoryItemId`
- `ProductId`
- `Quantity`
- `ReservationId`
- `WarehouseLocation`

**發布的 Domain Events**：

- `InventoryReservedEvent`
- `InventoryReleasedEvent`
- `InventoryReplenishedEvent`
- `InventoryDepletedEvent`
- `StockLevelChangedEvent`

**消費的 Domain Events**：

- `OrderSubmittedEvent` (from Order Context) → 預留庫存
- `OrderCancelledEvent` (from Order Context) → 釋放預留
- `OrderDeliveredEvent` (from Order Context) → 提交預留

**REST API Endpoints**：

- `GET /api/v1/inventory/{productId}` - 取得庫存水平
- `POST /api/v1/inventory/{productId}/replenish` - 新增庫存（admin）
- `GET /api/v1/inventory/low-stock` - 列出低庫存項目（admin）

**業務規則**：

- 庫存水平不能為負數
- 如果訂單未確認，預留在 15 分鐘後過期
- 數量 < 10 時低庫存警示
- 數量 < 5 時自動重新訂購

---

### 5. Payment Context

**職責**：處理付款和管理付款交易

**核心 Aggregate**：`Payment`

**關鍵 Entities**：

- `Payment` (Aggregate Root)
- `PaymentTransaction`
- `Refund`

**關鍵 Value Objects**：

- `PaymentId`
- `OrderId`
- `Money`
- `PaymentMethod` (CREDIT_CARD, DEBIT_CARD, PAYPAL, BANK_TRANSFER)
- `PaymentStatus` (PENDING, COMPLETED, FAILED, REFUNDED)
- `TransactionId`

**發布的 Domain Events**：

- `PaymentInitiatedEvent`
- `PaymentCompletedEvent`
- `PaymentFailedEvent`
- `RefundProcessedEvent`

**消費的 Domain Events**：

- `OrderSubmittedEvent` (from Order Context) → 啟動付款
- `OrderCancelledEvent` (from Order Context) → 處理退款

**REST API Endpoints**：

- `POST /api/v1/payments` - 啟動付款
- `GET /api/v1/payments/{id}` - 取得付款狀態
- `POST /api/v1/payments/{id}/refund` - 處理退款（admin）

**業務規則**：

- 付款金額必須符合訂單總額
- 失敗的付款觸發自動重試（最多 3 次嘗試）
- 退款只能對已完成的付款進行處理
- 允許對訂單取消進行部分退款

---

### 6. Delivery Context

**職責**：管理訂單配送和物流

**核心 Aggregate**：`Delivery`

**關鍵 Entities**：

- `Delivery` (Aggregate Root)
- `DeliveryRoute`
- `TrackingEvent`

**關鍵 Value Objects**：

- `DeliveryId`
- `OrderId`
- `TrackingNumber`
- `DeliveryStatus` (SCHEDULED, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED, FAILED)
- `DeliveryAddress`
- `EstimatedDeliveryDate`

**發布的 Domain Events**：

- `DeliveryScheduledEvent`
- `DeliveryDispatchedEvent`
- `DeliveryInTransitEvent`
- `DeliveryDeliveredEvent`
- `DeliveryFailedEvent`

**消費的 Domain Events**：

- `OrderConfirmedEvent` (from Order Context) → 安排配送
- `PaymentCompletedEvent` (from Payment Context) → 確認配送

**REST API Endpoints**：

- `GET /api/v1/deliveries/{orderId}` - 取得配送狀態
- `GET /api/v1/deliveries/track/{trackingNumber}` - 追蹤配送
- `POST /api/v1/deliveries/{id}/update-status` - 更新狀態（logistics）

**業務規則**：

- 配送只能為已確認的訂單安排
- 追蹤編號必須唯一
- 預計配送時間：標準運送 3-5 個工作天
- 配送失敗觸發客戶通知

---

### 7. Promotion Context

**職責**：管理促銷活動和折扣規則

**核心 Aggregate**：`Promotion`

**關鍵 Entities**：

- `Promotion` (Aggregate Root)
- `PromotionRule`
- `DiscountCoupon`

**關鍵 Value Objects**：

- `PromotionId`
- `PromotionType` (PERCENTAGE, FIXED_AMOUNT, BUY_X_GET_Y)
- `DiscountRate`
- `ValidityPeriod`
- `CouponCode`

**發布的 Domain Events**：

- `PromotionCreatedEvent`
- `PromotionActivatedEvent`
- `PromotionExpiredEvent`
- `CouponAppliedEvent`
- `CouponRedeemedEvent`

**消費的 Domain Events**：

- `OrderSubmittedEvent` (from Order Context) → 應用促銷

**REST API Endpoints**：

- `GET /api/v1/promotions/active` - 列出有效促銷
- `POST /api/v1/promotions/validate-coupon` - 驗證優惠券代碼
- `POST /api/v1/promotions` - 建立促銷（admin）

**業務規則**：

- 促銷有開始和結束日期
- 優惠券可以有使用限制（每位客戶或總計）
- 除非明確限制，否則可以合併多個促銷
- 過期的促銷不能應用

---

### 8. Notification Context

**職責**：透過多個管道向客戶發送通知

**核心 Aggregate**：`Notification`

**關鍵 Entities**：

- `Notification` (Aggregate Root)
- `NotificationTemplate`

**關鍵 Value Objects**：

- `NotificationId`
- `NotificationType` (EMAIL, SMS, PUSH)
- `NotificationStatus` (PENDING, SENT, FAILED)
- `RecipientId`

**發布的 Domain Events**：

- `NotificationSentEvent`
- `NotificationFailedEvent`

**消費的 Domain Events**：

- `CustomerCreatedEvent` → 發送歡迎郵件
- `OrderConfirmedEvent` → 發送訂單確認
- `OrderShippedEvent` → 發送運送通知
- `PaymentCompletedEvent` → 發送付款收據
- `DeliveryDeliveredEvent` → 發送配送確認

**REST API Endpoints**：

- `GET /api/v1/notifications/{customerId}` - 取得客戶通知
- `POST /api/v1/notifications/send` - 發送通知（內部）

**業務規則**：

- 尊重客戶通知偏好設定
- 失敗的通知重試最多 3 次
- 郵件通知包含取消訂閱連結
- 關鍵通知（付款、安全）不能停用

---

### 9. Review Context

**職責**：管理產品評論和評分

**核心 Aggregate**：`Review`

**關鍵 Entities**：

- `Review` (Aggregate Root)
- `ReviewComment`

**關鍵 Value Objects**：

- `ReviewId`
- `ProductId`
- `CustomerId`
- `Rating` (1-5 stars)
- `ReviewStatus` (PENDING, APPROVED, REJECTED)

**發布的 Domain Events**：

- `ReviewSubmittedEvent`
- `ReviewApprovedEvent`
- `ReviewRejectedEvent`
- `ReviewUpdatedEvent`

**消費的 Domain Events**：

- `OrderDeliveredEvent` (from Order Context) → 啟用評論提交

**REST API Endpoints**：

- `GET /api/v1/reviews?productId={id}` - 取得產品評論
- `POST /api/v1/reviews` - 提交評論
- `PUT /api/v1/reviews/{id}` - 更新評論
- `POST /api/v1/reviews/{id}/approve` - 批准評論（admin）

**業務規則**：

- 客戶只能評論他們購買的產品
- 每位客戶每個產品一個評論
- 評論在發布前需要審核
- 評分必須在 1 到 5 星之間

---

### 10. Shopping Cart Context

**職責**：管理客戶購物車和購物車操作

**核心 Aggregate**：`ShoppingCart`

**關鍵 Entities**：

- `ShoppingCart` (Aggregate Root)
- `CartItem`

**關鍵 Value Objects**：

- `CartId`
- `CustomerId`
- `ProductId`
- `Quantity`
- `CartStatus` (ACTIVE, ABANDONED, CONVERTED)

**發布的 Domain Events**：

- `CartCreatedEvent`
- `ItemAddedToCartEvent`
- `ItemRemovedFromCartEvent`
- `CartAbandonedEvent`
- `CartConvertedToOrderEvent`

**消費的 Domain Events**：

- `ProductPriceChangedEvent` (from Product Context) → 更新購物車價格
- `InventoryDepletedEvent` (from Inventory Context) → 移除不可用項目

**REST API Endpoints**：

- `GET /api/v1/carts/{customerId}` - 取得客戶購物車
- `POST /api/v1/carts/{customerId}/items` - 將項目加入購物車
- `DELETE /api/v1/carts/{customerId}/items/{productId}` - 移除項目
- `PUT /api/v1/carts/{customerId}/items/{productId}` - 更新數量
- `POST /api/v1/carts/{customerId}/checkout` - 將購物車轉換為訂單

**業務規則**：

- 購物車項目在 24 小時後過期
- 數量不能超過可用庫存
- 價格變更時重新計算購物車總額
- 放棄的購物車在 1 小時後觸發提醒郵件

---

### 11. Pricing Context

**職責**：計算包含折扣、稅金和運費的價格

**核心 Aggregate**：`PriceCalculation`

**關鍵 Entities**：

- `PriceCalculation` (Aggregate Root)
- `PricingRule`

**關鍵 Value Objects**：

- `BasePrice`
- `DiscountAmount`
- `TaxAmount`
- `ShippingCost`
- `FinalPrice`
- `PricingStrategy`

**發布的 Domain Events**：

- `PriceCalculatedEvent`
- `PricingRuleAppliedEvent`

**消費的 Domain Events**：

- `PromotionActivatedEvent` (from Promotion Context) → 更新定價規則
- `OrderSubmittedEvent` (from Order Context) → 計算最終價格

**REST API Endpoints**：

- `POST /api/v1/pricing/calculate` - 計算訂單價格
- `GET /api/v1/pricing/shipping-cost` - 取得運費估算

**業務規則**：

- 稅率因配送地點而異
- 訂單 > $100 免運費
- 大量購買的數量折扣
- 基於會員等級的會員折扣

---

### 12. Seller Context

**職責**：管理賣家帳戶和賣家操作

**核心 Aggregate**：`Seller`

**關鍵 Entities**：

- `Seller` (Aggregate Root)
- `SellerProfile`
- `SellerRating`

**關鍵 Value Objects**：

- `SellerId`
- `SellerName`
- `SellerStatus` (ACTIVE, SUSPENDED, INACTIVE)
- `CommissionRate`

**發布的 Domain Events**：

- `SellerRegisteredEvent`
- `SellerApprovedEvent`
- `SellerSuspendedEvent`
- `SellerRatingUpdatedEvent`

**消費的 Domain Events**：

- `OrderCompletedEvent` (from Order Context) → 更新賣家統計資料
- `ReviewSubmittedEvent` (from Review Context) → 更新賣家評分

**REST API Endpoints**：

- `POST /api/v1/sellers` - 註冊賣家
- `GET /api/v1/sellers/{id}` - 取得賣家個人檔案
- `PUT /api/v1/sellers/{id}` - 更新賣家個人檔案
- `GET /api/v1/sellers/{id}/products` - 列出賣家產品

**業務規則**：

- 賣家必須在列出產品前獲得批准
- 佣金率：基於賣家等級 10-20%
- 暫停的賣家不能列出新產品
- 賣家評分基於客戶評論和訂單履行

---

### 13. Observability Context (Cross-Cutting)

**職責**：收集和彙總系統指標、日誌和追蹤

**核心 Aggregate**：`MetricRecord`

**關鍵 Entities**：

- `MetricRecord` (Aggregate Root)
- `LogEntry`
- `TraceSpan`

**關鍵 Value Objects**：

- `MetricName`
- `MetricValue`
- `Timestamp`
- `TraceId`
- `SpanId`

**發布的 Domain Events**：

- `MetricRecordedEvent`
- `AlertTriggeredEvent`

**消費的 Domain Events**：

- 所有 contexts 的所有 domain events → 記錄指標

**REST API Endpoints**：

- `GET /api/v1/metrics` - 取得系統指標（admin）
- `GET /api/v1/health` - Health check endpoint

**業務規則**：

- 指標保留 90 天
- 日誌保留 30 天
- 追蹤保留 7 天
- 警示基於閾值規則觸發

---

## Context 整合模式

### 同步通訊 (REST API)

用於需要立即回應的即時查詢：

- Shopping Cart → Product（取得產品詳細資訊）
- Order → Pricing（計算訂單總額）
- Customer → Order（查詢訂單狀態）

### 非同步通訊 (Domain Events)

用於跨 context 工作流程和最終一致性：

- Order → Inventory（預留庫存）
- Order → Payment（處理付款）
- Order → Delivery（安排配送）
- 所有 contexts → Notification（發送通知）

### Shared Kernel

`domain/shared/` 中的最小共享 value objects：

- `Money`
- `CustomerId`
- `ProductId`
- `OrderId`

## Context 邊界

### 明確所有權

- 每個 context 專屬擁有其資料
- Contexts 之間不直接存取資料庫
- 每個 context 都有自己的資料庫 schema/tables

### Anti-Corruption Layer

- 每個 context 將外部資料轉換為自己的 domain model
- 外部 IDs 包裝在 context 特定的 value objects 中
- 外部 events 轉換為內部 domain events

## 演進策略

### 新增新 Contexts

1. 識別新業務能力
2. 定義 context 邊界和職責
3. 設計 domain model 和 events
4. 實作基礎設施
5. 透過 events 與現有 contexts 整合

### 拆分 Contexts

1. 識別現有 context 中的子領域
2. 提取 domain model 和 events
3. 建立新的 bounded context
4. 遷移資料並更新整合
5. 逐步棄用舊 context

### 合併 Contexts

1. 識別重疊的職責
2. 設計統一的 domain model
3. 合併 events 和 APIs
4. 從兩個 contexts 遷移資料
5. 更新所有整合

## 快速連結

- [回到 Functional Viewpoint](overview.md)
- [Use Cases](use-cases.md)
- [Functional Interfaces](interfaces.md)
- [Information Viewpoint](../information/overview.md)
- [主文件](../../README.md)

## 變更歷史

| 日期 | 版本 | 作者 | 變更 |
|------|---------|--------|---------|
| 2025-10-22 | 1.0 | Architecture Team | 包含所有 13 個 bounded contexts 的初始版本 |

---

**Document Version**: 1.0
**Last Updated**: 2025-10-22
