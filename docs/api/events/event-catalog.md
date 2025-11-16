# Domain Events Catalog

## 概述

本文件提供電子商務平台中所有 domain events 的完整目錄。事件依 bounded context 組織，並包括描述、payload 資訊和相關事件。

**最後更新**: 2025-10-25

---

## 事件命名慣例

所有事件遵循以下命名慣例：

- **過去式**：事件使用過去式動詞（例如：`CustomerCreated`，而非 `CreateCustomer`）
- **Context 前綴**：事件包含 aggregate 名稱（例如：`OrderSubmitted`、`PaymentProcessed`）
- **描述性**：事件名稱清楚描述發生了什麼

---

## Customer Context Events

| 事件名稱 | 描述 | Aggregate | 觸發條件 |
|------------|-------------|-----------|----------|
| `CustomerCreatedEvent` | 建立了新的客戶帳戶 | Customer | 客戶註冊 |
| `CustomerProfileUpdatedEvent` | 更新了客戶資料 | Customer | 編輯個人資料 |
| `CustomerEmailVerifiedEvent` | 驗證了客戶 email 地址 | Customer | Email 驗證 |
| `CustomerPasswordChangedEvent` | 變更了客戶密碼 | Customer | 變更密碼 |
| `CustomerDeactivatedEvent` | 停用了客戶帳戶 | Customer | 關閉帳戶 |
| `CustomerReactivatedEvent` | 重新啟用了客戶帳戶 | Customer | 重新啟用帳戶 |
| `CustomerMembershipUpgradedEvent` | 升級了客戶會員等級 | Customer | 會員升級 |

**總計**：7 個事件

**詳細資訊**：參見 [Customer Events 文件](contexts/customer-events.md)

---

## Order Context Events

| 事件名稱 | 描述 | Aggregate | 觸發條件 |
|------------|-------------|-----------|----------|
| `OrderCreatedEvent` | 從購物車建立了新訂單 | Order | 結帳啟動 |
| `OrderSubmittedEvent` | 提交訂單進行處理 | Order | 提交訂單 |
| `OrderConfirmedEvent` | 付款後確認訂單 | Order | 付款成功 |
| `OrderCancelledEvent` | 客戶或系統取消了訂單 | Order | 取消訂單 |
| `OrderShippedEvent` | 訂單已出貨給客戶 | Order | 配送發貨 |
| `OrderDeliveredEvent` | 訂單已送達客戶 | Order | 確認送達 |
| `OrderReturnedEvent` | 客戶退回了訂單 | Order | 退貨請求 |
| `OrderRefundedEvent` | 訂單已退款給客戶 | Order | 處理退款 |
| `OrderItemAddedEvent` | 項目已加入訂單 | Order | 加入項目 |
| `OrderItemRemovedEvent` | 項目已從訂單移除 | Order | 移除項目 |
| `OrderItemQuantityChangedEvent` | 訂單中的項目數量已變更 | Order | 更新數量 |

**總計**：11 個事件

**詳細資訊**：參見 [Order Events 文件](contexts/order-events.md)

---

## Product Context Events

| 事件名稱 | 描述 | Aggregate | 觸發條件 |
|------------|-------------|-----------|----------|
| `ProductCreatedEvent` | 新產品已加入目錄 | Product | 建立產品 |
| `ProductUpdatedEvent` | 產品資訊已更新 | Product | 編輯產品 |
| `ProductPriceChangedEvent` | 產品價格已變更 | Product | 更新價格 |
| `ProductStockUpdatedEvent` | 產品庫存量已更新 | Product | 調整庫存 |
| `ProductDeactivatedEvent` | 產品已從目錄移除 | Product | 停用產品 |
| `ProductReactivatedEvent` | 產品已恢復到目錄 | Product | 重新啟用產品 |
| `ProductCategoryChangedEvent` | 產品類別已變更 | Product | 更新類別 |

**總計**：7 個事件

**詳細資訊**：參見 [Product Events 文件](contexts/product-events.md)

---

## Payment Context Events

| 事件名稱 | 描述 | Aggregate | 觸發條件 |
|------------|-------------|-----------|----------|
| `PaymentInitiatedEvent` | 付款流程已啟動 | Payment | 開始付款 |
| `PaymentProcessedEvent` | 付款已成功處理 | Payment | 付款成功 |
| `PaymentFailedEvent` | 付款處理失敗 | Payment | 付款失敗 |
| `PaymentRefundedEvent` | 付款已退款給客戶 | Payment | 處理退款 |
| `PaymentCancelledEvent` | 付款已取消 | Payment | 取消付款 |

**總計**：5 個事件

**詳細資訊**：參見 [Payment Events 文件](contexts/payment-events.md)

---

## Inventory Context Events

| 事件名稱 | 描述 | Aggregate | 觸發條件 |
|------------|-------------|-----------|----------|
| `InventoryReservedEvent` | 為訂單保留了庫存 | Inventory | 提交訂單 |
| `InventoryReleasedEvent` | 釋放了保留的庫存 | Inventory | 取消訂單 |
| `InventoryAdjustedEvent` | 手動調整了庫存量 | Inventory | 調整庫存 |
| `InventoryLowStockAlertEvent` | 庫存低於閾值 | Inventory | 偵測到低庫存 |

**總計**：4 個事件

**詳細資訊**：參見 [Inventory Events 文件](contexts/inventory-events.md)

---

## Shipping Context Events

| 事件名稱 | 描述 | Aggregate | 觸發條件 |
|------------|-------------|-----------|----------|
| `ShippingScheduledEvent` | 為訂單安排了配送 | Shipping | 確認訂單 |
| `ShippingLabelCreatedEvent` | 產生了配送標籤 | Shipping | 產生標籤 |
| `ShippingDispatchedEvent` | 包裹已發貨 | Shipping | 包裹取件 |
| `ShippingInTransitEvent` | 包裹運送中 | Shipping | 運送更新 |
| `ShippingDeliveredEvent` | 包裹已送達 | Shipping | 確認送達 |
| `ShippingFailedEvent` | 配送嘗試失敗 | Shipping | 配送失敗 |

**總計**：6 個事件

**詳細資訊**：參見 [Shipping Events 文件](contexts/shipping-events.md)

---

## Promotion Context Events

| 事件名稱 | 描述 | Aggregate | 觸發條件 |
|------------|-------------|-----------|----------|
| `PromotionCreatedEvent` | 建立了新促銷活動 | Promotion | 設定促銷 |
| `PromotionActivatedEvent` | 促銷活動已啟動 | Promotion | 促銷開始 |
| `PromotionDeactivatedEvent` | 促銷活動已停用 | Promotion | 促銷結束 |
| `PromotionAppliedEvent` | 促銷活動已套用至訂單 | Promotion | 套用折扣 |

**總計**：4 個事件

**詳細資訊**：參見 [Promotion Events 文件](contexts/promotion-events.md)

---

## Notification Context Events

| 事件名稱 | 描述 | Aggregate | 觸發條件 |
|------------|-------------|-----------|----------|
| `NotificationSentEvent` | 通知已發送給客戶 | Notification | 事件觸發 |
| `NotificationFailedEvent` | 通知發送失敗 | Notification | 發送失敗 |
| `NotificationReadEvent` | 客戶已讀取通知 | Notification | 讀取動作 |

**總計**：3 個事件

**詳細資訊**：參見 [Notification Events 文件](contexts/notification-events.md)

---

## Review Context Events

| 事件名稱 | 描述 | Aggregate | 觸發條件 |
|------------|-------------|-----------|----------|
| `ReviewCreatedEvent` | 客戶建立了產品評價 | Review | 提交評價 |
| `ReviewUpdatedEvent` | 客戶更新了評價 | Review | 編輯評價 |
| `ReviewDeletedEvent` | 評價已刪除 | Review | 刪除評價 |
| `ReviewApprovedEvent` | 評價已由審核員核准 | Review | 審核核准 |

**總計**：4 個事件

**詳細資訊**：參見 [Review Events 文件](contexts/review-events.md)

---

## Shopping Cart Context Events

| 事件名稱 | 描述 | Aggregate | 觸發條件 |
|------------|-------------|-----------|----------|
| `CartCreatedEvent` | 建立了新購物車 | ShoppingCart | 購物車初始化 |
| `ItemAddedToCartEvent` | 項目已加入購物車 | ShoppingCart | 加入購物車 |
| `ItemRemovedFromCartEvent` | 項目已從購物車移除 | ShoppingCart | 從購物車移除 |
| `CartItemQuantityChangedEvent` | 購物車中的項目數量已變更 | ShoppingCart | 更新數量 |
| `CartCheckedOutEvent` | 購物車已結帳 | ShoppingCart | 結帳 |

**總計**：5 個事件

**詳細資訊**：參見 [Shopping Cart Events 文件](contexts/shopping-cart-events.md)

---

## Pricing Context Events

| 事件名稱 | 描述 | Aggregate | 觸發條件 |
|------------|-------------|-----------|----------|
| `PriceCalculatedEvent` | 為訂單計算了價格 | Pricing | 價格計算 |
| `DiscountAppliedEvent` | 訂單已套用折扣 | Pricing | 套用折扣 |
| `TaxCalculatedEvent` | 為訂單計算了稅金 | Pricing | 稅金計算 |

**總計**：3 個事件

**詳細資訊**：參見 [Pricing Events 文件](contexts/pricing-events.md)

---

## Seller Context Events

| 事件名稱 | 描述 | Aggregate | 觸發條件 |
|------------|-------------|-----------|----------|
| `SellerRegisteredEvent` | 註冊了新賣家 | Seller | 賣家註冊 |
| `SellerVerifiedEvent` | 賣家已驗證 | Seller | 完成驗證 |
| `SellerSuspendedEvent` | 賣家帳戶已暫停 | Seller | 暫停帳戶 |
| `SellerReactivatedEvent` | 賣家帳戶已重新啟用 | Seller | 重新啟用帳戶 |
| `SellerProductListedEvent` | 賣家上架了新產品 | Seller | 產品上架 |

**總計**：5 個事件

**詳細資訊**：參見 [Seller Events 文件](contexts/seller-events.md)

---

## Delivery Context Events

| 事件名稱 | 描述 | Aggregate | 觸發條件 |
|------------|-------------|-----------|----------|
| `DeliveryScheduledEvent` | 配送已安排 | Delivery | 安排配送 |
| `DeliveryAssignedEvent` | 配送已指派給司機 | Delivery | 指派司機 |
| `DeliveryInProgressEvent` | 配送進行中 | Delivery | 配送開始 |
| `DeliveryCompletedEvent` | 配送已完成 | Delivery | 完成配送 |
| `DeliveryFailedEvent` | 配送嘗試失敗 | Delivery | 配送失敗 |

**總計**：5 個事件

**詳細資訊**：參見 [Delivery Events 文件](contexts/delivery-events.md)

---

## 事件統計

### 依 Context

| Context | 事件數量 | 百分比 |
|---------|-------------|------------|
| Order | 11 | 16.9% |
| Customer | 7 | 10.8% |
| Product | 7 | 10.8% |
| Shipping | 6 | 9.2% |
| Payment | 5 | 7.7% |
| Shopping Cart | 5 | 7.7% |
| Seller | 5 | 7.7% |
| Delivery | 5 | 7.7% |
| Promotion | 4 | 6.2% |
| Inventory | 4 | 6.2% |
| Review | 4 | 6.2% |
| Notification | 3 | 4.6% |
| Pricing | 3 | 4.6% |
| **總計** | **65** | **100%** |

### 依類別

| 類別 | 事件數量 | 描述 |
|----------|-------------|-------------|
| Lifecycle | 25 | Created、Updated、Deleted 事件 |
| State Transition | 20 | 狀態變更事件 |
| Business Action | 15 | 業務操作事件 |
| System Event | 5 | 系統產生的事件 |

---

## 事件依賴關係

### 常見事件流程

#### 訂單處理流程

```text
CartCheckedOutEvent
    → OrderCreatedEvent
    → OrderSubmittedEvent
    → PaymentInitiatedEvent
    → PaymentProcessedEvent
    → OrderConfirmedEvent
    → InventoryReservedEvent
    → ShippingScheduledEvent
    → ShippingDispatchedEvent
    → ShippingDeliveredEvent
    → OrderDeliveredEvent
```

#### 客戶註冊流程

```text
CustomerCreatedEvent
    → NotificationSentEvent (歡迎 Email)
    → CustomerEmailVerifiedEvent
```

#### 產品購買流程

```text
ItemAddedToCartEvent
    → CartCheckedOutEvent
    → OrderCreatedEvent
    → ProductStockUpdatedEvent
    → InventoryReservedEvent
```

---

## 事件版本控制

### 當前版本

所有事件目前都在版本 1.0。未來版本將在此處記錄。

### 已棄用事件

目前沒有已棄用的事件。

---

## 相關文件

- **事件 Schemas**：參見 [schemas/](schemas/) 目錄
- **事件處理**：參見 `.kiro/steering/domain-events.md`
- **架構**：參見 `docs/viewpoints/information/data-flow.md`
- **API 文件**：參見 [README.md](README.md)

---

## 快速導覽

- [Customer Events](contexts/customer-events.md)
- [Order Events](contexts/order-events.md)
- [Product Events](contexts/product-events.md)
- [Payment Events](contexts/payment-events.md)
- [Inventory Events](contexts/inventory-events.md)
- [Shipping Events](contexts/shipping-events.md)
- [Promotion Events](contexts/promotion-events.md)
- [Notification Events](contexts/notification-events.md)
- [Review Events](contexts/review-events.md)
- [Shopping Cart Events](contexts/shopping-cart-events.md)
- [Pricing Events](contexts/pricing-events.md)
- [Seller Events](contexts/seller-events.md)
- [Delivery Events](contexts/delivery-events.md)

---

**文件版本**: 1.0
**最後更新**: 2025-10-25
**負責人**: 架構團隊
