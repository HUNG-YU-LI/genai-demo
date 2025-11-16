# REST API 文件

## 概述

企業級電子商務平台提供遵循 OpenAPI 3.0 規範的完整 RESTful API。API 採用領域驅動設計 (DDD) 原則設計,並依限界上下文組織。

## API 設計原則

### RESTful 架構

我們的 API 遵循 REST (Representational State Transfer) 架構原則:

- **資源導向 URL**: URL 代表資源,而非動作
- **標準 HTTP Method**: GET、POST、PUT、PATCH、DELETE
- **無狀態通訊**: 每個 request 包含所有必要資訊
- **標準 HTTP 狀態碼**: 一致使用狀態碼
- **JSON 格式**: 所有 request 和 response 使用 JSON

### 設計指南

**URL 命名慣例**:

- 使用名詞,而非動詞: `/customers` 而非 `/getCustomers`
- 使用複數名詞: `/customers` 而非 `/customer`
- 使用 kebab-case: `/order-items` 而非 `/orderItems`
- 合理地巢狀資源: `/customers/{id}/orders`
- 保持 URL 簡潔直觀

**HTTP Method**:

- `GET`: 檢索資源 - 安全且具冪等性
- `POST`: 建立新資源 - 非冪等性
- `PUT`: 更新整個資源 - 冪等性
- `PATCH`: 部分更新 - 冪等性
- `DELETE`: 刪除資源 - 冪等性

**HTTP 狀態碼**:

- `200 OK`: 成功的 GET、PUT、PATCH
- `201 Created`: 成功的 POST
- `204 No Content`: 成功的 DELETE
- `400 Bad Request`: 驗證錯誤、格式錯誤的 request
- `401 Unauthorized`: 需要身份驗證
- `403 Forbidden`: 授權失敗
- `404 Not Found`: 找不到資源
- `409 Conflict`: 違反業務規則
- `422 Unprocessable Entity`: 語義驗證錯誤
- `500 Internal Server Error`: 系統錯誤

## Base URL 和版本控制

### Base URL

**Production**:

```yaml
https://api.ecommerce.com
```

**Staging**:

```yaml
https://api-staging.ecommerce.com
```

**Development**:

```yaml
http://localhost:8080
```

### API 版本控制

API 使用基於 URL 的版本控制以確保向後相容性:

```text
/api/v1/customers
/api/v2/customers
```

**版本控制策略**:

- 在 URL 路徑中使用主版本 (`v1`、`v2`)
- 至少維持 2 個版本的向後相容性
- 移除前 6 個月提供廢棄警告
- 破壞性變更需要新的主版本

**廢棄標頭**:

```http
Deprecation: true
Sunset: 2026-12-31T23:59:59Z
Link: </api/v2/customers>; rel="successor-version"
```

## API 結構

### 限界上下文

API 依以下限界上下文組織:

| Context | Base Path | 描述 |
|---------|-----------|------|
| Customer | `/api/v1/customers` | 客戶管理和資料 |
| Order | `/api/v1/orders` | 訂單處理和管理 |
| Product | `/api/v1/products` | 產品目錄和庫存 |
| Shopping Cart | `/api/v1/carts` | 購物車操作 |
| Payment | `/api/v1/payments` | 付款處理 |
| Promotion | `/api/v1/promotions` | 促銷和折扣 |
| Inventory | `/api/v1/inventory` | 庫存管理 |
| Logistics | `/api/v1/logistics` | 運送和配送 |
| Notification | `/api/v1/notifications` | 通知和提醒 |

### 標準回應格式

所有 API response 遵循一致的格式:

**成功回應**:

```json
{
  "data": {
    "id": "cust-123",
    "name": "John Doe",
    "email": "john@example.com"
  },
  "metadata": {
    "requestId": "req-abc-123",
    "timestamp": "2025-10-25T10:30:00Z",
    "version": "v1"
  }
}
```

**錯誤回應**:

```json
{
  "errors": [
    {
      "code": "VALIDATION_ERROR",
      "message": "Email format is invalid",
      "field": "email",
      "rejectedValue": "invalid-email"
    }
  ],
  "metadata": {
    "requestId": "req-abc-123",
    "timestamp": "2025-10-25T10:30:00Z",
    "version": "v1"
  }
}
```

### 分頁

列表 endpoint 支援使用查詢參數進行分頁:

**Request**:

```http
GET /api/v1/customers?page=0&size=20&sort=createdAt,desc
```

**Response**:

```json
{
  "data": {
    "content": [
      {
        "id": "cust-123",
        "name": "John Doe"
      }
    ],
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 100,
      "totalPages": 5,
      "first": true,
      "last": false
    }
  },
  "metadata": {
    "requestId": "req-abc-123",
    "timestamp": "2025-10-25T10:30:00Z",
    "version": "v1"
  }
}
```

**分頁參數**:

- `page`: 頁碼 (從 0 開始,預設: 0)
- `size`: 頁面大小 (預設: 20,最大: 100)
- `sort`: 排序欄位和方向 (例如 `createdAt,desc`)

## 互動式文件

### Swagger UI

在以下位置存取互動式 API 文件:

**Development**:

```yaml
http://localhost:8080/swagger-ui.html
```

**Production**:

```yaml
https://api.ecommerce.com/swagger-ui.html
```

### OpenAPI Specification

下載 OpenAPI 3.0 規格:

**JSON 格式**:

```yaml
http://localhost:8080/v3/api-docs
```

**YAML 格式**:

```yaml
http://localhost:8080/v3/api-docs.yaml
```

## API Endpoint

### 核心 API

- [身份驗證](authentication.md) - 基於 JWT 的身份驗證
- [錯誤處理](error-handling.md) - 錯誤代碼和疑難排解
- [Customer API](endpoints/customers.md) - 客戶管理
- [Order API](endpoints/orders.md) - 訂單處理
- [Product API](endpoints/products.md) - 產品目錄
- [Payment API](endpoints/payments.md) - 付款處理

### 其他 API

- [Shopping Cart API](endpoints/shopping-cart.md) - 購物車操作
- [Promotion API](endpoints/promotions.md) - 促銷和折扣
- [Inventory API](endpoints/inventory.md) - 庫存管理
- [Logistics API](endpoints/logistics.md) - 運送和配送
- [Notification API](endpoints/notifications.md) - 通知

## 開始使用

### 先決條件

- 有效的 API 憑證 (API key 或 JWT token)
- HTTP client (curl、Postman 或程式語言的 HTTP library)
- 了解 REST 原則

### 快速開始

1. **取得身份驗證 Token**:

   ```bash
   curl -X POST https://api.ecommerce.com/api/v1/auth/login \
     -H "Content-Type: application/json" \
     -d '{
       "email": "user@example.com",
       "password": "your-password"
     }'
   ```

2. **發送已驗證的 Request**:

   ```bash
   curl -X GET https://api.ecommerce.com/api/v1/customers/me \
     -H "Authorization: Bearer YOUR_JWT_TOKEN"
   ```

3. **建立資源**:

   ```bash
   curl -X POST https://api.ecommerce.com/api/v1/customers \
     -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{
       "name": "John Doe",
       "email": "john@example.com",
       "address": {
         "street": "123 Main St",
         "city": "Taipei",
         "postalCode": "10001"
       }
     }'
   ```

### 測試工具

**Postman Collection**:

- 下載: [Postman Collection](postman/ecommerce-api.json)
- 匯入 Postman 以便於測試

**curl 範例**:

- 參見各別 endpoint 文件的 curl 範例

**Client SDK**:

- 從 OpenAPI specification 產生 client SDK
- 支援的語言: Java、JavaScript、Python、Go

## 流量限制

API request 受流量限制以確保公平使用:

**限制**:

- 已驗證使用者: 每小時 1000 次 request
- 未驗證使用者: 每小時 100 次 request
- 突發限制: 每秒 20 次 request

**流量限制標頭**:

```http
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1635789600
```

**超過流量限制回應**:

```http
HTTP/1.1 429 Too Many Requests
Retry-After: 3600

{
  "errors": [
    {
      "code": "RATE_LIMIT_EXCEEDED",
      "message": "Rate limit exceeded. Please try again later."
    }
  ]
}
```

## CORS 配置

跨來源資源共用 (CORS) 配置為允許來自批准來源的 request:

**允許的來源**:

- `https://www.ecommerce.com`
- `https://admin.ecommerce.com`
- `http://localhost:3000` (development)

**允許的 Method**:

- GET、POST、PUT、PATCH、DELETE、OPTIONS

**允許的標頭**:

- Authorization、Content-Type、X-Request-ID

## 最佳實踐

### Request 最佳實踐

1. **使用適當的 HTTP Method**: 讀取使用 GET,建立使用 POST,更新使用 PUT/PATCH
2. **包含 Request ID**: 新增 `X-Request-ID` 標頭以進行追蹤
3. **設定 Content-Type**: 始終設定 `Content-Type: application/json`
4. **優雅地處理錯誤**: 檢查狀態碼並適當地處理錯誤
5. **實作重試邏輯**: 使用指數退避進行重試
6. **快取回應**: 適當時快取 GET response

### 安全性最佳實踐

1. **使用 HTTPS**: 在 production 中始終使用 HTTPS
2. **保護 Token**: 安全地儲存 JWT token (不要儲存在 localStorage)
3. **驗證輸入**: 在客戶端驗證所有輸入
4. **處理敏感資料**: 絕不記錄敏感資料
5. **實作逾時**: 設定合理的 request 逾時
6. **監控 API 使用**: 追蹤 API 使用和錯誤

## 支援和資源

### 文件

- [API 參考](endpoints/) - 詳細的 endpoint 文件
- [身份驗證指南](authentication.md) - 身份驗證和授權
- [錯誤處理](error-handling.md) - 錯誤代碼和疑難排解
- [Postman Collection](postman/) - 可直接使用的 API collection

### 支援管道

- **Email**: <api-support@ecommerce.com>
- **文件**: <https://docs.ecommerce.com>
- **狀態頁**: <https://status.ecommerce.com>
- **GitHub Issues**: <https://github.com/ecommerce/api/issues>

### 相關文件

- [架構決策記錄](../../architecture/adrs/) - API 設計決策
- [Domain Event](../events/) - Event-driven 架構
- [開發指南](../../development/) - 開發人員資源

---

**API 版本**: v1
**最後更新**: 2025-10-25
**OpenAPI Specification**: [下載](http://localhost:8080/v3/api-docs)
