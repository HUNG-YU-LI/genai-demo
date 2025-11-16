# Promotion API

## 概述

Promotion API 提供了管理促銷活動、折扣碼和特別優惠的端點。促銷可以應用於產品、類別或整個訂單。

**Base Path**: `/api/v1/promotions`

**Authentication**: 管理端點需要驗證，公開查詢為選填

## Endpoints

### List Active Promotions

取得所有當前有效的促銷活動。

**Endpoint**: `GET /api/v1/promotions`

**Authentication**: 不需要

**Query Parameters**:

- `page`: 頁碼（預設：0）
- `size`: 每頁大小（預設：20）
- `type`: 依類型篩選（PERCENTAGE, FIXED_AMOUNT, BUY_X_GET_Y, FREE_SHIPPING）
- `category`: 依適用類別篩選
- `active`: 依有效狀態篩選（預設：true）

**Success Response** (200 OK):

```json
{
  "data": {
    "content": [
      {
        "id": "promo-001",
        "code": "SAVE10",
        "name": "10% Off Electronics",
        "description": "Get 10% off all electronics",
        "type": "PERCENTAGE",
        "discountValue": 10.0,
        "minPurchaseAmount": 1000.00,
        "maxDiscountAmount": 500.00,
        "applicableCategories": ["electronics"],
        "startDate": "2025-10-01T00:00:00Z",
        "endDate": "2025-10-31T23:59:59Z",
        "usageLimit": 1000,
        "usageCount": 245,
        "active": true
      },
      {
        "id": "promo-002",
        "code": "FREESHIP",
        "name": "Free Shipping",
        "description": "Free shipping on orders over $500",
        "type": "FREE_SHIPPING",
        "minPurchaseAmount": 500.00,
        "startDate": "2025-10-01T00:00:00Z",
        "endDate": "2025-12-31T23:59:59Z",
        "active": true
      }
    ],
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 15,
      "totalPages": 1
    }
  }
}
```

**curl Example**:

```bash
curl -X GET "https://api.ecommerce.com/api/v1/promotions?type=PERCENTAGE&active=true"
```

---

### Get Promotion by ID

根據 ID 取得特定促銷活動。

**Endpoint**: `GET /api/v1/promotions/{id}`

**Authentication**: 不需要

**Path Parameters**:

- `id`: Promotion ID

**Success Response** (200 OK):

```json
{
  "data": {
    "id": "promo-001",
    "code": "SAVE10",
    "name": "10% Off Electronics",
    "description": "Get 10% off all electronics",
    "type": "PERCENTAGE",
    "discountValue": 10.0,
    "minPurchaseAmount": 1000.00,
    "maxDiscountAmount": 500.00,
    "applicableCategories": ["electronics"],
    "applicableProducts": [],
    "excludedProducts": [],
    "startDate": "2025-10-01T00:00:00Z",
    "endDate": "2025-10-31T23:59:59Z",
    "usageLimit": 1000,
    "usageCount": 245,
    "usageLimitPerCustomer": 1,
    "stackable": false,
    "active": true,
    "createdAt": "2025-09-25T10:00:00Z",
    "updatedAt": "2025-10-25T12:00:00Z"
  }
}
```

**Error Responses**:

- `404 Not Found`: 找不到促銷活動

**curl Example**:

```bash
curl -X GET https://api.ecommerce.com/api/v1/promotions/promo-001
```

---

### Validate Promotion Code

驗證特定購物車或訂單的促銷碼。

**Endpoint**: `POST /api/v1/promotions/validate`

**Authentication**: 需要

**Request Body**:

```json
{
  "code": "SAVE10",
  "cartId": "cart-123",
  "subtotal": 1500.00,
  "items": [
    {
      "productId": "prod-456",
      "categoryId": "cat-electronics",
      "quantity": 2,
      "unitPrice": 750.00
    }
  ]
}
```

**Success Response** (200 OK):

```json
{
  "data": {
    "valid": true,
    "promotionId": "promo-001",
    "code": "SAVE10",
    "name": "10% Off Electronics",
    "type": "PERCENTAGE",
    "discountValue": 10.0,
    "calculatedDiscount": 150.00,
    "finalAmount": 1350.00,
    "message": "Promotion applied successfully"
  }
}
```

**Validation Failed Response** (200 OK):

```json
{
  "data": {
    "valid": false,
    "code": "SAVE10",
    "reason": "MINIMUM_PURCHASE_NOT_MET",
    "message": "Minimum purchase amount of $1000 required",
    "requiredAmount": 1000.00,
    "currentAmount": 800.00
  }
}
```

**Error Responses**:

- `400 Bad Request`: 請求格式無效
- `404 Not Found`: 找不到促銷碼

**curl Example**:

```bash
curl -X POST https://api.ecommerce.com/api/v1/promotions/validate \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "SAVE10",
    "cartId": "cart-123",
    "subtotal": 1500.00
  }'
```

---

### Create Promotion (Admin)

建立新的促銷活動。

**Endpoint**: `POST /api/v1/promotions`

**Authentication**: 需要

**Authorization**: 需要 ADMIN 或 MARKETING 角色

**Request Body**:

```json
{
  "code": "SUMMER2025",
  "name": "Summer Sale 2025",
  "description": "20% off summer collection",
  "type": "PERCENTAGE",
  "discountValue": 20.0,
  "minPurchaseAmount": 500.00,
  "maxDiscountAmount": 1000.00,
  "applicableCategories": ["clothing", "accessories"],
  "startDate": "2025-06-01T00:00:00Z",
  "endDate": "2025-08-31T23:59:59Z",
  "usageLimit": 5000,
  "usageLimitPerCustomer": 3,
  "stackable": false,
  "active": true
}
```

**Validation Rules**:

- `code`: 必填，唯一，4-20 個字元，僅限英數字
- `name`: 必填，3-100 個字元
- `type`: 必填，必須為以下其中之一：PERCENTAGE, FIXED_AMOUNT, BUY_X_GET_Y, FREE_SHIPPING
- `discountValue`: PERCENTAGE 和 FIXED_AMOUNT 類型必填
- `startDate`: 必填，必須為未來日期
- `endDate`: 必填，必須晚於 startDate

**Success Response** (201 Created):

```json
{
  "data": {
    "id": "promo-003",
    "code": "SUMMER2025",
    "name": "Summer Sale 2025",
    "description": "20% off summer collection",
    "type": "PERCENTAGE",
    "discountValue": 20.0,
    "minPurchaseAmount": 500.00,
    "maxDiscountAmount": 1000.00,
    "applicableCategories": ["clothing", "accessories"],
    "startDate": "2025-06-01T00:00:00Z",
    "endDate": "2025-08-31T23:59:59Z",
    "usageLimit": 5000,
    "usageLimitPerCustomer": 3,
    "stackable": false,
    "active": true,
    "createdAt": "2025-10-25T13:00:00Z"
  }
}
```

**Error Responses**:

- `400 Bad Request`: 驗證錯誤
- `403 Forbidden`: 權限不足
- `409 Conflict`: 促銷碼已存在

**curl Example**:

```bash
curl -X POST https://api.ecommerce.com/api/v1/promotions \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "SUMMER2025",
    "name": "Summer Sale 2025",
    "type": "PERCENTAGE",
    "discountValue": 20.0,
    "startDate": "2025-06-01T00:00:00Z",
    "endDate": "2025-08-31T23:59:59Z"
  }'
```

---

### Update Promotion (Admin)

更新現有的促銷活動。

**Endpoint**: `PUT /api/v1/promotions/{id}`

**Authentication**: 需要

**Authorization**: 需要 ADMIN 或 MARKETING 角色

**Path Parameters**:

- `id`: Promotion ID

**Request Body**:

```json
{
  "name": "Summer Sale 2025 - Extended",
  "description": "20% off summer collection - Extended!",
  "endDate": "2025-09-30T23:59:59Z",
  "usageLimit": 10000,
  "active": true
}
```

**Success Response** (200 OK):

```json
{
  "data": {
    "id": "promo-003",
    "code": "SUMMER2025",
    "name": "Summer Sale 2025 - Extended",
    "description": "20% off summer collection - Extended!",
    "endDate": "2025-09-30T23:59:59Z",
    "usageLimit": 10000,
    "updatedAt": "2025-10-25T14:00:00Z"
  }
}
```

**Business Rules**:

- 建立後無法變更促銷碼
- 建立後無法變更類型
- 無法將使用次數限制降低至低於當前使用次數

**Error Responses**:

- `400 Bad Request`: 驗證錯誤
- `403 Forbidden`: 權限不足
- `404 Not Found`: 找不到促銷活動

**curl Example**:

```bash
curl -X PUT https://api.ecommerce.com/api/v1/promotions/promo-003 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "endDate": "2025-09-30T23:59:59Z",
    "usageLimit": 10000
  }'
```

---

### Deactivate Promotion (Admin)

停用促銷活動（軟刪除）。

**Endpoint**: `DELETE /api/v1/promotions/{id}`

**Authentication**: 需要

**Authorization**: 需要 ADMIN 或 MARKETING 角色

**Path Parameters**:

- `id`: Promotion ID

**Success Response** (204 No Content)

**Error Responses**:

- `403 Forbidden`: 權限不足
- `404 Not Found`: 找不到促銷活動

**curl Example**:

```bash
curl -X DELETE https://api.ecommerce.com/api/v1/promotions/promo-003 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Get Promotion Usage Statistics (Admin)

取得促銷活動的使用統計資料。

**Endpoint**: `GET /api/v1/promotions/{id}/statistics`

**Authentication**: 需要

**Authorization**: 需要 ADMIN 或 MARKETING 角色

**Path Parameters**:

- `id`: Promotion ID

**Success Response** (200 OK):

```json
{
  "data": {
    "promotionId": "promo-001",
    "code": "SAVE10",
    "usageCount": 245,
    "usageLimit": 1000,
    "usagePercentage": 24.5,
    "totalDiscountAmount": 12250.00,
    "totalOrderValue": 122500.00,
    "averageDiscountPerOrder": 50.00,
    "uniqueCustomers": 230,
    "topCustomers": [
      {
        "customerId": "cust-123",
        "usageCount": 3,
        "totalDiscount": 450.00
      }
    ],
    "usageByDate": [
      {
        "date": "2025-10-25",
        "usageCount": 15,
        "totalDiscount": 750.00
      }
    ]
  }
}
```

**curl Example**:

```bash
curl -X GET https://api.ecommerce.com/api/v1/promotions/promo-001/statistics \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Get Customer Promotion History

取得已驗證顧客的促銷使用歷史。

**Endpoint**: `GET /api/v1/promotions/me/history`

**Authentication**: 需要

**Query Parameters**:

- `page`: 頁碼（預設：0）
- `size`: 每頁大小（預設：20）

**Success Response** (200 OK):

```json
{
  "data": {
    "content": [
      {
        "promotionId": "promo-001",
        "code": "SAVE10",
        "name": "10% Off Electronics",
        "orderId": "order-456",
        "discountAmount": 150.00,
        "usedAt": "2025-10-20T14:30:00Z"
      }
    ],
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 5,
      "totalPages": 1
    }
  }
}
```

**curl Example**:

```bash
curl -X GET https://api.ecommerce.com/api/v1/promotions/me/history \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## Data Models

### Promotion Object

```json
{
  "id": "string",
  "code": "string",
  "name": "string",
  "description": "string",
  "type": "PERCENTAGE | FIXED_AMOUNT | BUY_X_GET_Y | FREE_SHIPPING",
  "discountValue": "number",
  "minPurchaseAmount": "number",
  "maxDiscountAmount": "number",
  "applicableCategories": ["string"],
  "applicableProducts": ["string"],
  "excludedProducts": ["string"],
  "startDate": "string (ISO 8601)",
  "endDate": "string (ISO 8601)",
  "usageLimit": "number",
  "usageCount": "number",
  "usageLimitPerCustomer": "number",
  "stackable": "boolean",
  "active": "boolean",
  "createdAt": "string (ISO 8601)",
  "updatedAt": "string (ISO 8601)"
}
```

### Promotion Types

| Type | Description | Example |
|------|-------------|---------|
| PERCENTAGE | 百分比折扣 | 10% off |
| FIXED_AMOUNT | 固定金額折扣 | $50 off |
| BUY_X_GET_Y | 買 X 送 Y | Buy 2 get 1 free |
| FREE_SHIPPING | 免運費 | Free shipping over $500 |

## Business Rules

1. **Code Uniqueness**: 促銷碼必須是唯一的
2. **Date Validation**: 結束日期必須晚於開始日期
3. **Usage Limits**: 不能超過全域或每位顧客的使用次數限制
4. **Stacking**: 不可堆疊的促銷活動無法合併使用
5. **Category/Product Rules**: 促銷僅適用於指定的商品
6. **Minimum Purchase**: 訂單必須達到最低金額
7. **Maximum Discount**: 折扣不能超過最大上限
8. **Expiration**: 促銷活動在結束日期後會自動停用

## Error Codes

| Code | Description | Solution |
|------|-------------|----------|
| `PROMOTION_NOT_FOUND` | 找不到促銷碼 | 檢查碼的拼寫 |
| `PROMOTION_EXPIRED` | 促銷已過期 | 使用當前有效的促銷 |
| `PROMOTION_NOT_STARTED` | 促銷尚未開始 | 等待開始日期 |
| `PROMOTION_USAGE_LIMIT_REACHED` | 已達使用次數限制 | 促銷不再可用 |
| `PROMOTION_CUSTOMER_LIMIT_REACHED` | 已達顧客使用次數限制 | 無法再次使用 |
| `PROMOTION_MINIMUM_NOT_MET` | 未達最低購買金額 | 新增更多商品 |
| `PROMOTION_NOT_APPLICABLE` | 不適用於購物車商品 | 檢查符合條件的產品 |
| `PROMOTION_NOT_STACKABLE` | 無法與其他促銷合併使用 | 移除其他促銷 |

## Related Documentation

- [Shopping Cart API](shopping-cart.md) - 將促銷套用至購物車
- [Order API](orders.md) - 訂單中的促銷
- [Product API](products.md) - 產品類別

---

**Last Updated**: 2025-10-25
**API Version**: v1
