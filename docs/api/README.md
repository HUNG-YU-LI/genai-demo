# API 文件

> **最後更新**: 2025-01-17

## 概述

本節包含 GenAI Demo 電子商務平台的完整 API 文件,包括 REST API endpoint、domain event、身份驗證和整合指南。

## 快速導覽

### 🌐 REST API

- [REST API 概述](rest/README.md) - API 設計原則和慣例
- [身份驗證](rest/authentication.md) - JWT 身份驗證和授權
- [錯誤處理](rest/error-handling.md) - 錯誤回應格式和代碼
- [版本控制](rest/versioning.md) - API 版本策略

### 📡 Domain Events

- [Event 概述](events/README.md) - Event-driven 架構
- [Event 目錄](events/event-catalog.md) - 完整的 domain event 清單
- [Event 模式](events/event-patterns.md) - Event 設計模式
- [Event Context](events/contexts/) - 依限界上下文分類的 event

### 🔌 整合

- [整合指南](integration/README.md) - 整合模式和最佳實踐
- [Webhook](integration/webhooks.md) - Webhook 配置
- [流量限制](integration/rate-limiting.md) - API 流量限制和配額

## 依 Context 分類的 API Endpoint

### Customer Context

**Base Path**: `/api/v1/customers`

| Method | Endpoint | 描述 |
|--------|----------|------|
| POST | `/customers` | 建立新客戶 |
| GET | `/customers/{id}` | 依 ID 取得客戶 |
| PUT | `/customers/{id}` | 更新客戶 |
| DELETE | `/customers/{id}` | 刪除客戶 |
| GET | `/customers/{id}/orders` | 取得客戶訂單 |
| GET | `/customers/{id}/profile` | 取得客戶資料 |
| PUT | `/customers/{id}/profile` | 更新客戶資料 |
| POST | `/customers/{id}/addresses` | 新增客戶地址 |

[完整 Customer API 文件](rest/endpoints/customers.md)

### Order Context

**Base Path**: `/api/v1/orders`

| Method | Endpoint | 描述 |
|--------|----------|------|
| POST | `/orders` | 建立新訂單 |
| GET | `/orders/{id}` | 依 ID 取得訂單 |
| PUT | `/orders/{id}` | 更新訂單 |
| POST | `/orders/{id}/submit` | 提交訂單 |
| POST | `/orders/{id}/confirm` | 確認訂單 |
| POST | `/orders/{id}/ship` | 出貨訂單 |
| POST | `/orders/{id}/deliver` | 配送訂單 |
| POST | `/orders/{id}/cancel` | 取消訂單 |
| GET | `/orders/{id}/items` | 取得訂單項目 |
| POST | `/orders/{id}/items` | 新增訂單項目 |

[完整 Order API 文件](rest/endpoints/orders.md)

### Product Context

**Base Path**: `/api/v1/products`

| Method | Endpoint | 描述 |
|--------|----------|------|
| POST | `/products` | 建立新產品 |
| GET | `/products/{id}` | 依 ID 取得產品 |
| PUT | `/products/{id}` | 更新產品 |
| DELETE | `/products/{id}` | 刪除產品 |
| GET | `/products` | 列出產品 |
| GET | `/products/search` | 搜尋產品 |
| GET | `/products/{id}/inventory` | 取得產品庫存 |

[完整 Product API 文件](rest/endpoints/products.md)

### Inventory Context

**Base Path**: `/api/v1/inventory`

| Method | Endpoint | 描述 |
|--------|----------|------|
| GET | `/inventory/{productId}` | 取得庫存數量 |
| POST | `/inventory/{productId}/reserve` | 保留庫存 |
| POST | `/inventory/{productId}/release` | 釋放庫存 |
| POST | `/inventory/{productId}/adjust` | 調整庫存 |
| GET | `/inventory/low-stock` | 取得低庫存項目 |
| POST | `/inventory/{productId}/restock` | 補貨 |

[完整 Inventory API 文件](rest/endpoints/inventory.md)

### Payment Context

**Base Path**: `/api/v1/payments`

| Method | Endpoint | 描述 |
|--------|----------|------|
| POST | `/payments` | 處理付款 |
| GET | `/payments/{id}` | 依 ID 取得付款 |
| POST | `/payments/{id}/refund` | 退款 |
| GET | `/payments/order/{orderId}` | 取得訂單的付款記錄 |
| POST | `/payments/{id}/verify` | 驗證付款 |

[完整 Payment API 文件](rest/endpoints/payments.md)

### Shipping Context

**Base Path**: `/api/v1/shipping`

| Method | Endpoint | 描述 |
|--------|----------|------|
| POST | `/shipping/shipments` | 建立出貨 |
| GET | `/shipping/shipments/{id}` | 依 ID 取得出貨 |
| PUT | `/shipping/shipments/{id}/track` | 更新追蹤 |
| GET | `/shipping/rates` | 計算運費 |

[完整 Shipping API 文件](rest/endpoints/shipping.md)

## 依 Context 分類的 Domain Event

### Customer Events

- `CustomerCreatedEvent`
- `CustomerUpdatedEvent`
- `CustomerDeletedEvent`
- `CustomerProfileUpdatedEvent`
- `CustomerAddressAddedEvent`

[完整 Customer Event](events/contexts/customer-events.md)

### Order Events

- `OrderCreatedEvent`
- `OrderSubmittedEvent`
- `OrderConfirmedEvent`
- `OrderShippedEvent`
- `OrderDeliveredEvent`
- `OrderCancelledEvent`
- `OrderItemAddedEvent`
- `OrderItemRemovedEvent`

[完整 Order Event](events/contexts/order-events.md)

### Product Events

- `ProductCreatedEvent`
- `ProductUpdatedEvent`
- `ProductDeletedEvent`
- `ProductPriceChangedEvent`

[完整 Product Event](events/contexts/product-events.md)

### Inventory Events

- `InventoryReservedEvent`
- `InventoryReleasedEvent`
- `InventoryAdjustedEvent`
- `LowStockAlertEvent`

[完整 Inventory Event](events/contexts/inventory-events.md)

### Payment Events

- `PaymentProcessedEvent`
- `PaymentFailedEvent`
- `PaymentRefundedEvent`
- `PaymentVerifiedEvent`

[完整 Payment Event](events/contexts/payment-events.md)

### Shipping Events

- `ShipmentCreatedEvent`
- `ShipmentShippedEvent`
- `ShipmentDeliveredEvent`
- `TrackingUpdatedEvent`

[完整 Shipping Event](events/contexts/shipping-events.md)

## 身份驗證與授權

### JWT 身份驗證

所有 API request 都需要 JWT 身份驗證:

```http
Authorization: Bearer <jwt_token>
```

[身份驗證指南](rest/authentication.md)

### 角色型存取控制

- **Admin**: 完整存取所有 endpoint
- **Customer**: 僅存取自己的資料
- **Seller**: 存取自己的產品和訂單
- **Guest**: 僅讀取公開資料

[授權指南](rest/authentication.md#authorization)

## 錯誤處理

### 標準錯誤回應

```json
{
  "errorCode": "RESOURCE_NOT_FOUND",
  "message": "Customer with id 123 not found",
  "timestamp": "2025-01-17T10:30:00Z",
  "path": "/api/v1/customers/123",
  "fieldErrors": []
}
```

[錯誤處理指南](rest/error-handling.md)

### HTTP 狀態碼

- `200 OK`: 成功的 GET、PUT、PATCH
- `201 Created`: 成功的 POST
- `204 No Content`: 成功的 DELETE
- `400 Bad Request`: 驗證錯誤
- `401 Unauthorized`: 需要身份驗證
- `403 Forbidden`: 授權失敗
- `404 Not Found`: 找不到資源
- `409 Conflict`: 違反業務規則
- `500 Internal Server Error`: 系統錯誤

## API 版本控制

目前 API 版本: **v1**

- **URL 版本控制**: `/api/v1/`
- **向後相容**: 維持 2 個版本
- **廢棄通知**: 移除前 6 個月

[版本控制策略](rest/versioning.md)

## 流量限制

- **預設**: 每個 API key 每小時 1000 次 request
- **突發**: 每分鐘 100 次 request
- **標頭**: `X-RateLimit-Limit`、`X-RateLimit-Remaining`、`X-RateLimit-Reset`

[流量限制指南](integration/rate-limiting.md)

## 開始使用

### API 使用者

1. **取得 API Key**: 註冊 API 存取
2. **閱讀身份驗證指南**: 了解 JWT 身份驗證
3. **探索 Endpoint**: 查看 endpoint 文件
4. **使用 Postman 測試**: 使用提供的 Postman collection
5. **處理錯誤**: 實作適當的錯誤處理

### 開發人員

1. **了解架構**: 查看[功能視角](../viewpoints/functional/README.md)
2. **學習 Event 模式**: 研讀 [Domain Event](events/README.md)
3. **遵循標準**: 使用 [API 設計標準](rest/README.md)
4. **實作 Endpoint**: 遵循[開發指南](../development/README.md)

## 工具和資源

### Postman Collection

下載完整的 Postman collection:
- [Postman Collection](postman/genai-demo-api.postman_collection.json)
- [環境變數](postman/genai-demo-environment.postman_environment.json)

### OpenAPI Specification

- [OpenAPI 3.0 規格](openapi/genai-demo-api-v1.yaml)
- [Swagger UI](http://localhost:8080/swagger-ui.html) (本地執行時)

### 程式碼範例

- [Java Client 範例](examples/java/)
- [JavaScript Client 範例](examples/javascript/)
- [Python Client 範例](examples/python/)

## 相關文件

### 架構文件

- [功能視角](../viewpoints/functional/README.md) - 業務能力
- [Context 視角](../viewpoints/context/README.md) - 系統 context
- [資訊視角](../viewpoints/information/README.md) - 資料模型

### 開發文件

- [API 開發指南](../development/api-development.md)
- [測試 API Endpoint](../development/testing/api-testing.md)
- [API 安全性](../perspectives/security/api-security.md)

### 架構決策

- [ADR-014: JWT 身份驗證策略](../architecture/adrs/014-jwt-authentication-strategy.md)
- [ADR-015: RBAC 實作](../architecture/adrs/015-rbac-implementation.md)
- [ADR-050: API 安全性與流量限制](../architecture/adrs/050-api-security-rate-limiting.md)

## 支援

### API 支援

- **Email**: api-support@company.com
- **Slack**: #api-support
- **文件**: 本站
- **狀態頁**: https://status.company.com

### 回報問題

1. 查看[疑難排解指南](../operations/troubleshooting/README.md)
2. 搜尋現有問題
3. 建立新問題並提供詳細資訊
4. 包含 API request/response 範例

## 貢獻

### 更新 API 文件

1. 遵循 [API 文件標準](../STYLE-GUIDE.md#api-documentation)
2. 更新 OpenAPI specification
3. 新增程式碼範例
4. 提交 PR 進行審查

### 新增 Endpoint

1. 依照 REST 原則設計 endpoint
2. 在 OpenAPI spec 中記錄
3. 新增到相關 context 文件
4. 包含身份驗證/授權需求
5. 新增程式碼範例

---

**文件負責人**: API Team
**最後審查**: 2025-01-17
**下次審查**: 2025-04-17
**狀態**: Active
