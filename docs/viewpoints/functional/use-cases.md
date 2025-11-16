---
title: "Use Cases"
type: "functional-viewpoint"
category: "functional"
stakeholders: ["business-analysts", "product-managers", "developers", "qa-engineers"]
last_updated: "2025-10-22"
version: "1.0"
status: "active"
owner: "Product Team"
related_docs:

  - "viewpoints/functional/overview.md"
  - "viewpoints/functional/bounded-contexts.md"
  - "development/testing/bdd-testing.md"

tags: ["use-cases", "user-journeys", "business-processes", "bdd"]
---

# Use Cases

> **Status**: ✅ Active
> **Last Updated**: 2025-10-22
> **Owner**: Product Team

## Overview

本文件描述 Enterprise E-Commerce Platform 中的關鍵 use cases 和用戶旅程。每個 use case 代表用戶與系統互動以實現其目標的特定方式。

Use cases 按用戶角色組織，並使用 **Behavior-Driven Development (BDD)** 和 Cucumber scenarios 實現於 `src/test/resources/features/`。

## User Roles

### Primary Roles

- **Customer**：瀏覽產品並下訂單的終端用戶
- **Seller**：列出和銷售產品的商家
- **Administrator**：管理平台的系統管理員
- **Guest**：未經驗證的訪客

### System Roles

- **Payment Gateway**：外部支付處理系統
- **Logistics Provider**：外部配送服務
- **Notification Service**：內部通知系統

---

## Customer Use Cases

### UC-001: Customer Registration

**Actor**：Guest

**Goal**：建立新的客戶帳戶

**Preconditions**：

- 用戶未登入
- Email 地址尚未註冊

**Main Flow**：

1. Guest 導航至註冊頁面
2. Guest 輸入個人資訊（姓名、email、密碼、電話）
3. 系統驗證輸入資料
4. 系統建立客戶帳戶
5. 系統發送歡迎 email
6. 系統重定向至客戶儀表板

**Postconditions**：

- 以 STANDARD 會員資格建立客戶帳戶
- 發送歡迎 email
- 客戶已登入

**Alternative Flows**：

- **3a. Invalid email format**：系統顯示驗證錯誤
- **3b. Email already exists**：系統顯示 "Email already registered" 錯誤
- **3c. Weak password**：系統顯示密碼要求

**Business Rules**：

- Email 必須唯一
- 密碼必須至少 8 個字元，包含大寫、小寫、數字和特殊字元
- 預設會員等級為 STANDARD
- 初始獎勵積分餘額為 0

**Related BDD Scenario**：`features/customer/registration.feature`

**Bounded Contexts Involved**：

- Customer Context (primary)
- Notification Context

---

### UC-002: Product Search and Browse

**Actor**：Customer、Guest

**Goal**：尋找符合搜尋條件的產品

**Preconditions**：None

**Main Flow**：

1. 用戶輸入搜尋查詢或選擇類別
2. 系統檢索符合的產品
3. 系統顯示帶有篩選器的產品列表
4. 用戶套用篩選器（價格範圍、品牌、評分）
5. 系統更新產品列表
6. 用戶查看產品詳情

**Postconditions**：

- 顯示產品列表
- 套用篩選器
- 記錄搜尋歷史（對於已登入的客戶）

**Alternative Flows**：

- **2a. No products found**：系統顯示 "No products found" 訊息與建議
- **6a. Product out of stock**：系統顯示 "Out of Stock" 標記

**Business Rules**：

- 搜尋結果預設按相關性排序
- 搜尋結果中不顯示已停產的產品
- 顯示缺貨產品但清楚標記

**Related BDD Scenario**：`features/product/search.feature`

**Bounded Contexts Involved**：

- Product Context (primary)
- Inventory Context

---

### UC-003: Add Product to Shopping Cart

**Actor**：Customer

**Goal**：將選定的產品新增到購物車以供稍後購買

**Preconditions**：

- 客戶已登入
- 產品可用

**Main Flow**：

1. 客戶查看產品詳情
2. 客戶選擇數量
3. 客戶點擊 "Add to Cart"
4. 系統驗證產品可用性
5. 系統將項目新增到購物車
6. 系統更新購物車總額
7. 系統顯示確認訊息

**Postconditions**：

- 產品新增到購物車
- 更新購物車總額
- 更新購物車項目數量

**Alternative Flows**：

- **4a. Insufficient inventory**：系統顯示 "Only X items available" 訊息
- **4b. Product discontinued**：系統顯示 "Product no longer available" 錯誤
- **5a. Item already in cart**：系統更新數量而非新增重複項目

**Business Rules**：

- 數量不能超過可用庫存
- 購物車項目 24 小時後過期
- 每個產品在購物車中最多 99 件

**Related BDD Scenario**：`features/cart/add-to-cart.feature`

**Bounded Contexts Involved**：

- Shopping Cart Context (primary)
- Product Context
- Inventory Context

---

### UC-004: Checkout and Place Order

**Actor**：Customer

**Goal**：完成購物車中項目的購買

**Preconditions**：

- 客戶已登入
- 購物車至少有一件商品
- 客戶有配送地址

**Main Flow**：

1. 客戶導航至結帳
2. 系統顯示訂單摘要
3. 客戶選擇配送地址
4. 客戶選擇支付方式
5. 客戶套用優惠券代碼（可選）
6. 系統計算最終價格（商品 + 運費 - 折扣 + 稅金）
7. 客戶確認訂單
8. 系統建立訂單
9. 系統保留庫存
10. 系統處理支付
11. 系統確認訂單
12. 系統發送訂單確認 email
13. 系統清空購物車

**Postconditions**：

- 以 CONFIRMED 狀態建立訂單
- 保留庫存
- 完成支付
- 購物車為空
- 發送訂單確認 email

**Alternative Flows**：

- **5a. Invalid coupon**：系統顯示 "Invalid or expired coupon" 錯誤
- **9a. Insufficient inventory**：系統顯示 "Some items are no longer available" 並移除它們
- **10a. Payment failed**：系統取消訂單、釋放庫存、顯示支付錯誤
- **10b. Payment timeout**：系統重試支付最多 3 次

**Business Rules**：

- 訂單金額 > $100 免運費
- 根據配送地址計算稅金
- 促銷和優惠券可以組合使用，除非有限制
- 支付必須在 15 分鐘內完成，否則訂單被取消

**Related BDD Scenario**：`features/order/checkout.feature`

**Bounded Contexts Involved**：

- Order Context (primary)
- Shopping Cart Context
- Inventory Context
- Payment Context
- Pricing Context
- Promotion Context
- Notification Context

---

### UC-005: Track Order Delivery

**Actor**：Customer

**Goal**：檢查訂購商品的狀態和位置

**Preconditions**：

- 客戶已登入
- 客戶至少有一筆訂單

**Main Flow**：

1. 客戶導航至訂單歷史
2. 系統顯示訂單列表
3. 客戶選擇一筆訂單
4. 系統顯示訂單詳情和配送狀態
5. 客戶點擊 "Track Delivery"
6. 系統顯示帶有時間軸的追蹤資訊

**Postconditions**：

- 顯示配送狀態
- 顯示追蹤時間軸

**Alternative Flows**：

- **4a. Order not yet shipped**：系統顯示 "Order is being prepared"
- **6a. Delivery failed**：系統顯示失敗原因和後續步驟

**Business Rules**：

- 追蹤僅適用於已確認的訂單
- 追蹤每 4 小時更新一次
- 預計配送時間：3-5 個工作天

**Related BDD Scenario**：`features/delivery/tracking.feature`

**Bounded Contexts Involved**：

- Order Context (primary)
- Delivery Context

---

### UC-006: Submit Product Review

**Actor**：Customer

**Goal**：分享已購買產品的反饋

**Preconditions**：

- 客戶已登入
- 客戶已收到產品
- 客戶尚未評論此產品

**Main Flow**：

1. 客戶導航至訂單歷史
2. 客戶選擇已配送的訂單
3. 客戶點擊產品的 "Write Review"
4. 客戶輸入評分（1-5 星）和評論文字
5. 客戶提交評論
6. 系統驗證評論
7. 系統以 PENDING 狀態建立評論
8. 系統通知管理員進行審核
9. 系統顯示 "Review submitted for moderation" 訊息

**Postconditions**：

- 以 PENDING 狀態建立評論
- 通知管理員進行審核
- 客戶看到確認訊息

**Alternative Flows**：

- **6a. Review contains inappropriate content**：系統標記以進行人工審核
- **6b. Customer already reviewed**：系統顯示 "You have already reviewed this product"

**Business Rules**：

- 只有購買產品的客戶才能評論
- 每個客戶每個產品一則評論
- 評論需要管理員批准才能發布
- 評分必須是 1-5 星

**Related BDD Scenario**：`features/review/submit-review.feature`

**Bounded Contexts Involved**：

- Review Context (primary)
- Order Context
- Product Context

---

### UC-007: Manage Delivery Addresses

**Actor**：Customer

**Goal**：新增、更新或移除配送地址

**Preconditions**：

- 客戶已登入

**Main Flow**：

1. 客戶導航至帳戶設定
2. 客戶選擇 "Delivery Addresses"
3. 系統顯示已儲存的地址列表
4. 客戶點擊 "Add New Address"
5. 客戶輸入地址詳情
6. 客戶標記為預設（可選）
7. 客戶儲存地址
8. 系統驗證地址
9. 系統儲存地址
10. 系統顯示確認訊息

**Postconditions**：

- 儲存新地址
- 地址標記為預設（如果已選擇）

**Alternative Flows**：

- **8a. Invalid address**：系統顯示驗證錯誤
- **3a. Update existing address**：客戶編輯並儲存
- **3b. Delete address**：客戶確認刪除，系統移除地址

**Business Rules**：

- 客戶可以有多個地址
- 至少一個地址必須標記為預設
- 不能刪除預設地址，除非將另一個設為預設

**Related BDD Scenario**：`features/customer/manage-addresses.feature`

**Bounded Contexts Involved**：

- Customer Context (primary)

---

## Seller Use Cases

### UC-101: Seller Registration

**Actor**：Guest

**Goal**：註冊為賣家以列出產品

**Preconditions**：

- 用戶未登入
- 可提供企業資訊

**Main Flow**：

1. Guest 導航至賣家註冊頁面
2. Guest 輸入企業資訊（公司名稱、稅號、聯絡詳情）
3. Guest 上傳企業文件
4. Guest 提交註冊
5. 系統驗證資訊
6. 系統以 PENDING 狀態建立賣家帳戶
7. 系統通知管理員進行批准
8. 系統發送確認 email

**Postconditions**：

- 以 PENDING 狀態建立賣家帳戶
- 通知管理員進行批准
- 發送確認 email

**Alternative Flows**：

- **5a. Invalid tax ID**：系統顯示驗證錯誤
- **5b. Missing documents**：系統顯示所需文件列表

**Business Rules**：

- 賣家必須提供有效的企業註冊
- 列出產品前需要管理員批准
- 預設佣金率：15%

**Related BDD Scenario**：`features/seller/registration.feature`

**Bounded Contexts Involved**：

- Seller Context (primary)
- Notification Context

---

### UC-102: List New Product

**Actor**：Seller

**Goal**：將新產品新增到目錄

**Preconditions**：

- 賣家已登入
- 賣家帳戶為 APPROVED

**Main Flow**：

1. 賣家導航至產品管理
2. 賣家點擊 "Add New Product"
3. 賣家輸入產品詳情（名稱、描述、價格、類別）
4. 賣家上傳產品圖片
5. 賣家設定初始庫存數量
6. 賣家提交產品
7. 系統驗證產品資訊
8. 系統以 ACTIVE 狀態建立產品
9. 系統建立庫存記錄
10. 系統顯示確認訊息

**Postconditions**：

- 建立產品並在目錄中可見
- 建立庫存記錄
- 產品可搜尋

**Alternative Flows**：

- **7a. Invalid price**：系統顯示 "Price must be positive" 錯誤
- **7b. Missing required fields**：系統突出顯示缺少的欄位
- **7c. Duplicate SKU**：系統顯示 "SKU already exists" 錯誤

**Business Rules**：

- SKU 必須唯一
- 價格必須為正數
- 至少需要一張產品圖片
- 產品類別必須有效

**Related BDD Scenario**：`features/seller/list-product.feature`

**Bounded Contexts Involved**：

- Product Context (primary)
- Inventory Context
- Seller Context

---

## Administrator Use Cases

### UC-201: Approve Seller Registration

**Actor**：Administrator

**Goal**：審核和批准賣家註冊

**Preconditions**：

- 管理員已登入
- 賣家註冊為 PENDING

**Main Flow**：

1. 管理員導航至待審核賣家註冊
2. 系統顯示待審核賣家列表
3. 管理員選擇一位賣家
4. 系統顯示賣家詳情和文件
5. 管理員審核資訊
6. 管理員點擊 "Approve"
7. 系統將賣家狀態更新為 APPROVED
8. 系統發送批准 email 給賣家
9. 系統顯示確認訊息

**Postconditions**：

- 賣家狀態為 APPROVED
- 賣家現在可以列出產品
- 發送批准 email

**Alternative Flows**：

- **6a. Reject seller**：管理員提供原因，系統發送拒絕 email

**Business Rules**：

- 只有管理員可以批准賣家
- 批准原因為可選
- 拒絕原因為必填

**Related BDD Scenario**：`features/admin/approve-seller.feature`

**Bounded Contexts Involved**：

- Seller Context (primary)
- Notification Context

---

### UC-202: Moderate Product Review

**Actor**：Administrator

**Goal**：審核和批准/拒絕產品評論

**Preconditions**：

- 管理員已登入
- 評論為 PENDING 狀態

**Main Flow**：

1. 管理員導航至待審核評論
2. 系統顯示待審核評論列表
3. 管理員選擇一則評論
4. 系統顯示評論詳情和產品上下文
5. 管理員審核內容
6. 管理員點擊 "Approve"
7. 系統將評論狀態更新為 APPROVED
8. 系統在產品頁面發布評論
9. 系統更新產品評分
10. 系統顯示確認訊息

**Postconditions**：

- 評論狀態為 APPROVED
- 評論在產品頁面可見
- 更新產品評分

**Alternative Flows**：

- **6a. Reject review**：管理員提供原因，系統通知客戶

**Business Rules**：

- 只有管理員可以審核評論
- 批准的評論立即可見
- 拒絕的評論對客戶不可見

**Related BDD Scenario**：`features/admin/moderate-review.feature`

**Bounded Contexts Involved**：

- Review Context (primary)
- Product Context
- Notification Context

---

### UC-203: Create Promotion Campaign

**Actor**：Administrator

**Goal**：建立新的促銷活動

**Preconditions**：

- 管理員已登入

**Main Flow**：

1. 管理員導航至促銷管理
2. 管理員點擊 "Create Promotion"
3. 管理員輸入促銷詳情（名稱、類型、折扣率、有效期）
4. 管理員設定促銷規則（最低訂單、適用類別）
5. 管理員產生優惠券代碼（可選）
6. 管理員啟用促銷
7. 系統驗證促銷
8. 系統建立促銷
9. 系統啟用促銷
10. 系統顯示確認訊息

**Postconditions**：

- 建立並啟用促銷
- 產生優惠券代碼（如適用）
- 促銷適用於訂單

**Alternative Flows**：

- **7a. Invalid date range**：系統顯示 "End date must be after start date" 錯誤
- **7b. Conflicting promotion**：系統警告有重疊的促銷

**Business Rules**：

- 促銷必須有有效的日期範圍
- 折扣率：1-90%
- 優惠券代碼必須唯一
- 可以同時有多個活動促銷

**Related BDD Scenario**：`features/admin/create-promotion.feature`

**Bounded Contexts Involved**：

- Promotion Context (primary)
- Pricing Context

---

## Cross-Cutting Use Cases

### UC-301: System Health Monitoring

**Actor**：System、Administrator

**Goal**：監控系統健康和效能

**Preconditions**：None

**Main Flow**：

1. 系統持續收集指標
2. 系統根據閾值評估指標
3. 系統檢測異常（高 CPU、慢回應時間）
4. 系統觸發警報
5. 系統發送通知給管理員
6. 管理員調查問題
7. 管理員採取糾正措施

**Postconditions**：

- 記錄警報
- 通知管理員
- 追蹤問題

**Alternative Flows**：

- **3a. No anomaly detected**：系統繼續監控

**Business Rules**：

- 每 60 秒收集指標
- 根據閾值規則觸發警報
- 立即發送關鍵警報
- 每 5 分鐘批次發送警告警報

**Related BDD Scenario**：`features/observability/monitoring.feature`

**Bounded Contexts Involved**：

- Observability Context (primary)
- Notification Context

---

## Use Case Relationships

### Primary User Journeys

#### Journey 1: First-Time Purchase

```text
UC-001 (Register)
  → UC-002 (Search Products)
  → UC-003 (Add to Cart)
  → UC-004 (Checkout)
  → UC-005 (Track Delivery)
  → UC-006 (Submit Review)
```

#### Journey 2: Repeat Purchase

```text
UC-002 (Search Products)
  → UC-003 (Add to Cart)
  → UC-004 (Checkout)
  → UC-005 (Track Delivery)
```

#### Journey 3: Seller Onboarding

```text
UC-101 (Seller Registration)
  → UC-201 (Admin Approval)
  → UC-102 (List Product)
```

### Use Case Dependencies

- **UC-004** 依賴 **UC-003**（購物車中必須有項目）
- **UC-005** 依賴 **UC-004**（必須已下訂單）
- **UC-006** 依賴 **UC-005**（必須已收到產品）
- **UC-102** 依賴 **UC-201**（賣家必須已批准）

## Implementation Status

| Use Case | Status | BDD Scenario | Implementation |
|----------|--------|--------------|----------------|
| UC-001 | ✅ Complete | ✅ Implemented | ✅ Complete |
| UC-002 | ✅ Complete | ✅ Implemented | ✅ Complete |
| UC-003 | ✅ Complete | ✅ Implemented | ✅ Complete |
| UC-004 | ✅ Complete | ✅ Implemented | ✅ Complete |
| UC-005 | ✅ Complete | ✅ Implemented | ✅ Complete |
| UC-006 | ✅ Complete | ✅ Implemented | ✅ Complete |
| UC-007 | ✅ Complete | ✅ Implemented | ✅ Complete |
| UC-101 | ✅ Complete | ✅ Implemented | ✅ Complete |
| UC-102 | ✅ Complete | ✅ Implemented | ✅ Complete |
| UC-201 | ✅ Complete | ✅ Implemented | ✅ Complete |
| UC-202 | ✅ Complete | ✅ Implemented | ✅ Complete |
| UC-203 | ✅ Complete | ✅ Implemented | ✅ Complete |
| UC-301 | ✅ Complete | ✅ Implemented | ✅ Complete |

## Testing Strategy

### BDD Testing with Cucumber

所有 use cases 都使用 Cucumber 實現為 BDD scenarios：

**Location**：`app/src/test/resources/features/`

**Example Scenario Structure**：

```gherkin
Feature: Customer Registration
  As a guest user
  I want to register for an account
  So that I can place orders

  Scenario: Successful customer registration
    Given I am on the registration page
    When I enter valid registration details
    And I submit the registration form
    Then my account should be created
    And I should receive a welcome email
    And I should be redirected to the dashboard
```

### Test Coverage

- **Unit Tests**：每個 bounded context 內的 Domain 邏輯
- **Integration Tests**：透過 events 的跨 context 工作流
- **BDD Tests**：端到端的用戶旅程
- **API Tests**：REST endpoint 驗證

## Quick Links

- [Back to Functional Viewpoint](overview.md)
- [Bounded Contexts](bounded-contexts.md)
- [Functional Interfaces](interfaces.md)
- [BDD Testing Guide](../../development/testing/bdd-testing.md)
- [Main Documentation](../../README.md)

## Change History

| Date | Version | Author | Changes |
|------|---------|--------|---------|
| 2025-10-22 | 1.0 | Product Team | Initial version with 13 use cases |

---

**Document Version**: 1.0
**Last Updated**: 2025-10-22
