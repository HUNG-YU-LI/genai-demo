# Shopping Cart API

## 概述

Shopping Cart API 提供管理客戶購物車的 endpoints，包括新增項目、更新數量、套用促銷活動和結帳準備。

**Base Path**: `/api/v1/carts`

**Authentication**: 所有 endpoints 都需要

## Endpoints

### 取得目前購物車

檢索已驗證使用者的購物車。

**Endpoint**: `GET /api/v1/carts/me`

**Authentication**: Required

**成功回應** (200 OK):

```json
{
  "data": {
    "id": "cart-123",
    "customerId": "cust-123",
    "items": [
      {
        "id": "item-1",
        "productId": "prod-456",
        "productName": "Wireless Mouse",
        "quantity": 2,
        "unitPrice": 599.00,
        "subtotal": 1198.00,
        "currency": "TWD"
      },
      {
        "id": "item-2",
        "productId": "prod-789",
        "productName": "USB Cable",
        "quantity": 1,
        "unitPrice": 199.00,
        "subtotal": 199.00,
        "currency": "TWD"
      }
    ],
    "subtotal": 1397.00,
    "discount": 139.70,
    "tax": 0.00,
    "total": 1257.30,
    "currency": "TWD",
    "appliedPromotions": [
      {
        "promotionId": "promo-001",
        "promotionName": "10% Off Electronics",
        "discountAmount": 139.70
      }
    ],
    "itemCount": 3,
    "createdAt": "2025-10-25T10:00:00Z",
    "updatedAt": "2025-10-25T11:30:00Z"
  }
}
```

**錯誤回應**:

- `401 Unauthorized`: 缺少或無效的 token
- `404 Not Found`: 找不到購物車（空購物車）

**curl 範例**:

```bash
curl -X GET https://api.ecommerce.com/api/v1/carts/me \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### 新增項目到購物車

將產品新增到購物車。

**Endpoint**: `POST /api/v1/carts/me/items`

**Authentication**: Required

**Request Body**:

```json
{
  "productId": "prod-456",
  "quantity": 2
}
```

**驗證規則**:

- `productId`: 必填，必須存在
- `quantity`: 必填，正整數，最大 99

**成功回應** (201 Created):

```json
{
  "data": {
    "id": "cart-123",
    "items": [
      {
        "id": "item-1",
        "productId": "prod-456",
        "productName": "Wireless Mouse",
        "quantity": 2,
        "unitPrice": 599.00,
        "subtotal": 1198.00,
        "currency": "TWD"
      }
    ],
    "subtotal": 1198.00,
    "total": 1198.00,
    "currency": "TWD",
    "itemCount": 2,
    "updatedAt": "2025-10-25T11:30:00Z"
  }
}
```

**錯誤回應**:

- `400 Bad Request`: 驗證錯誤
- `404 Not Found`: 找不到產品
- `409 Conflict`: 庫存不足

**curl 範例**:

```bash
curl -X POST https://api.ecommerce.com/api/v1/carts/me/items \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "prod-456",
    "quantity": 2
  }'
```

---

### 更新購物車項目數量

更新購物車中項目的數量。

**Endpoint**: `PATCH /api/v1/carts/me/items/{itemId}`

**Authentication**: Required

**Path Parameters**:

- `itemId`: 購物車項目 ID

**Request Body**:

```json
{
  "quantity": 3
}
```

**驗證規則**:

- `quantity`: 必填，正整數（0 表示移除），最大 99

**成功回應** (200 OK):

```json
{
  "data": {
    "id": "cart-123",
    "items": [
      {
        "id": "item-1",
        "productId": "prod-456",
        "productName": "Wireless Mouse",
        "quantity": 3,
        "unitPrice": 599.00,
        "subtotal": 1797.00,
        "currency": "TWD"
      }
    ],
    "subtotal": 1797.00,
    "total": 1797.00,
    "currency": "TWD",
    "itemCount": 3,
    "updatedAt": "2025-10-25T11:45:00Z"
  }
}
```

**錯誤回應**:

- `400 Bad Request`: 驗證錯誤
- `404 Not Found`: 找不到項目
- `409 Conflict`: 庫存不足

**curl 範例**:

```bash
curl -X PATCH https://api.ecommerce.com/api/v1/carts/me/items/item-1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "quantity": 3
  }'
```

---

### 從購物車移除項目

從購物車中移除項目。

**Endpoint**: `DELETE /api/v1/carts/me/items/{itemId}`

**Authentication**: Required

**Path Parameters**:

- `itemId`: 購物車項目 ID

**成功回應** (204 No Content)

**錯誤回應**:

- `401 Unauthorized`: 缺少或無效的 token
- `404 Not Found`: 找不到項目

**curl 範例**:

```bash
curl -X DELETE https://api.ecommerce.com/api/v1/carts/me/items/item-1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### 清空購物車

移除購物車中的所有項目。

**Endpoint**: `DELETE /api/v1/carts/me`

**Authentication**: Required

**成功回應** (204 No Content)

**錯誤回應**:

- `401 Unauthorized`: 缺少或無效的 token

**curl 範例**:

```bash
curl -X DELETE https://api.ecommerce.com/api/v1/carts/me \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### 套用促銷代碼

將促銷代碼套用到購物車。

**Endpoint**: `POST /api/v1/carts/me/promotions`

**Authentication**: Required

**Request Body**:

```json
{
  "promotionCode": "SAVE10"
}
```

**成功回應** (200 OK):

```json
{
  "data": {
    "id": "cart-123",
    "subtotal": 1797.00,
    "discount": 179.70,
    "total": 1617.30,
    "currency": "TWD",
    "appliedPromotions": [
      {
        "promotionId": "promo-001",
        "promotionCode": "SAVE10",
        "promotionName": "10% Off",
        "discountAmount": 179.70
      }
    ],
    "updatedAt": "2025-10-25T12:00:00Z"
  }
}
```

**錯誤回應**:

- `400 Bad Request`: 無效的促銷代碼
- `409 Conflict`: 促銷不適用或已過期

**curl 範例**:

```bash
curl -X POST https://api.ecommerce.com/api/v1/carts/me/promotions \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "promotionCode": "SAVE10"
  }'
```

---

### 移除促銷代碼

從購物車中移除促銷代碼。

**Endpoint**: `DELETE /api/v1/carts/me/promotions/{promotionId}`

**Authentication**: Required

**Path Parameters**:

- `promotionId`: 促銷 ID

**成功回應** (200 OK):

```json
{
  "data": {
    "id": "cart-123",
    "subtotal": 1797.00,
    "discount": 0.00,
    "total": 1797.00,
    "currency": "TWD",
    "appliedPromotions": [],
    "updatedAt": "2025-10-25T12:15:00Z"
  }
}
```

**curl 範例**:

```bash
curl -X DELETE https://api.ecommerce.com/api/v1/carts/me/promotions/promo-001 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### 合併購物車

將匿名購物車與已驗證使用者的購物車合併（登入後使用）。

**Endpoint**: `POST /api/v1/carts/me/merge`

**Authentication**: Required

**Request Body**:

```json
{
  "anonymousCartId": "cart-anon-456"
}
```

**成功回應** (200 OK):

```json
{
  "data": {
    "id": "cart-123",
    "items": [
      {
        "id": "item-1",
        "productId": "prod-456",
        "quantity": 5,
        "unitPrice": 599.00,
        "subtotal": 2995.00
      }
    ],
    "subtotal": 2995.00,
    "total": 2995.00,
    "currency": "TWD",
    "itemCount": 5,
    "mergedFrom": "cart-anon-456",
    "updatedAt": "2025-10-25T12:30:00Z"
  }
}
```

**業務規則**:

- 重複項目會合併（數量相加）
- 合併後匿名購物車會被刪除
- 來自匿名購物車的促銷會被驗證

**curl 範例**:

```bash
curl -X POST https://api.ecommerce.com/api/v1/carts/me/merge \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "anonymousCartId": "cart-anon-456"
  }'
```

---

### 驗證購物車以進行結帳

在結帳前驗證購物車項目並計算最終總額。

**Endpoint**: `POST /api/v1/carts/me/validate`

**Authentication**: Required

**成功回應** (200 OK):

```json
{
  "data": {
    "valid": true,
    "items": [
      {
        "productId": "prod-456",
        "available": true,
        "stockQuantity": 50,
        "requestedQuantity": 2
      }
    ],
    "subtotal": 1198.00,
    "discount": 119.80,
    "shippingFee": 100.00,
    "tax": 0.00,
    "total": 1178.20,
    "currency": "TWD",
    "warnings": [],
    "errors": []
  }
}
```

**有問題的驗證回應** (200 OK):

```json
{
  "data": {
    "valid": false,
    "items": [
      {
        "productId": "prod-456",
        "available": true,
        "stockQuantity": 1,
        "requestedQuantity": 2
      }
    ],
    "warnings": [
      {
        "code": "INSUFFICIENT_STOCK",
        "message": "Only 1 unit available for Wireless Mouse",
        "productId": "prod-456"
      }
    ],
    "errors": []
  }
}
```

**curl 範例**:

```bash
curl -X POST https://api.ecommerce.com/api/v1/carts/me/validate \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## 資料模型

### Cart Object

```json
{
  "id": "string",
  "customerId": "string",
  "items": [
    {
      "id": "string",
      "productId": "string",
      "productName": "string",
      "quantity": "number",
      "unitPrice": "number",
      "subtotal": "number",
      "currency": "string"
    }
  ],
  "subtotal": "number",
  "discount": "number",
  "shippingFee": "number",
  "tax": "number",
  "total": "number",
  "currency": "string",
  "appliedPromotions": [
    {
      "promotionId": "string",
      "promotionCode": "string",
      "promotionName": "string",
      "discountAmount": "number"
    }
  ],
  "itemCount": "number",
  "createdAt": "string (ISO 8601)",
  "updatedAt": "string (ISO 8601)"
}
```

## 業務規則

1. **購物車過期**: 匿名購物車在 30 天後過期
2. **已驗證購物車**: 持續保存直到明確清空
3. **數量限制**: 每個項目最多 99 單位
4. **庫存驗證**: 新增/更新時即時檢查庫存
5. **促銷堆疊**: 可根據規則套用多個促銷
6. **價格更新**: 每次檢索購物車時重新計算價格
7. **合併邏輯**: 重複項目的數量會相加

## 錯誤代碼

| Code | Description | Solution |
|------|-------------|----------|
| `CART_ITEM_NOT_FOUND` | 找不到購物車項目 | 檢查項目 ID |
| `CART_INSUFFICIENT_STOCK` | 庫存不足 | 減少數量 |
| `CART_INVALID_QUANTITY` | 無效的數量值 | 使用正整數 1-99 |
| `CART_PRODUCT_NOT_FOUND` | 找不到產品 | 檢查產品 ID |
| `CART_PROMOTION_INVALID` | 無效的促銷代碼 | 檢查代碼和有效期限 |
| `CART_PROMOTION_NOT_APPLICABLE` | 促銷不適用 | 檢查促銷條件 |

## 相關文件

- [Product API](products.md) - 產品資訊
- [Promotion API](promotions.md) - 促銷管理
- [Order API](orders.md) - 結帳和訂單建立
- [Inventory API](inventory.md) - 庫存可用性

---

**最後更新**: 2025-10-25
**API 版本**: v1
