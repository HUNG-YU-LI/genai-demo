# Product API

## 概述

Product API 提供管理產品目錄的 endpoints，包括產品資訊、庫存、定價和搜尋功能。

**Base Path**: `/api/v1/products`

**Authentication**:

- 讀取操作: 不需要（公開）
- 寫入操作: 需要 ADMIN 或 SELLER 角色

## Endpoints

### 列出產品

檢索具有篩選和搜尋功能的分頁產品清單。

**Endpoint**: `GET /api/v1/products`

**Authentication**: Not required

**Query Parameters**:

- `page`: 頁碼（從 0 開始，預設: 0）
- `size`: 頁面大小（預設: 20，最大: 100）
- `sort`: 排序欄位和方向（預設: `createdAt,desc`）
  - 選項: `name,asc`, `price,asc`, `price,desc`, `popularity,desc`
- `search`: 搜尋詞（搜尋名稱和描述）
- `category`: 依分類 ID 篩選
- `minPrice`: 最低價格篩選
- `maxPrice`: 最高價格篩選
- `inStock`: 依庫存可用性篩選（true/false）
- `brand`: 依品牌名稱篩選

**成功回應** (200 OK):

```json
{
  "data": {
    "content": [
      {
        "id": "prod-456",
        "name": "Wireless Mouse",
        "description": "Ergonomic wireless mouse with 2.4GHz connection",
        "price": 500.00,
        "currency": "TWD",
        "category": {
          "id": "cat-123",
          "name": "Computer Accessories"
        },
        "brand": "TechBrand",
        "images": [
          {
            "url": "https://cdn.ecommerce.com/products/prod-456-1.jpg",
            "alt": "Wireless Mouse - Front View",
            "isPrimary": true
          }
        ],
        "inStock": true,
        "stockQuantity": 150,
        "rating": 4.5,
        "reviewCount": 234,
        "createdAt": "2025-10-20T10:00:00Z"
      }
    ],
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 500,
      "totalPages": 25,
      "first": true,
      "last": false
    }
  }
}
```

**curl 範例**:

```bash
# 基本清單
curl -X GET "https://api.ecommerce.com/api/v1/products?page=0&size=20"

# 帶搜尋和篩選
curl -X GET "https://api.ecommerce.com/api/v1/products?search=mouse&category=cat-123&minPrice=100&maxPrice=1000&inStock=true"

# 依價格排序
curl -X GET "https://api.ecommerce.com/api/v1/products?sort=price,asc"
```

---

### 依 ID 取得產品

檢索特定產品的詳細資訊。

**Endpoint**: `GET /api/v1/products/{id}`

**Authentication**: Not required

**Path Parameters**:

- `id`: 產品 ID（例如 `prod-456`）

**成功回應** (200 OK):

```json
{
  "data": {
    "id": "prod-456",
    "sku": "WM-2025-001",
    "name": "Wireless Mouse",
    "description": "Ergonomic wireless mouse with 2.4GHz connection. Features include adjustable DPI, 6 programmable buttons, and long battery life.",
    "price": 500.00,
    "originalPrice": 600.00,
    "discount": 100.00,
    "discountPercentage": 16.67,
    "currency": "TWD",
    "category": {
      "id": "cat-123",
      "name": "Computer Accessories",
      "path": "Electronics > Computer Accessories"
    },
    "brand": "TechBrand",
    "images": [
      {
        "url": "https://cdn.ecommerce.com/products/prod-456-1.jpg",
        "alt": "Wireless Mouse - Front View",
        "isPrimary": true
      },
      {
        "url": "https://cdn.ecommerce.com/products/prod-456-2.jpg",
        "alt": "Wireless Mouse - Side View",
        "isPrimary": false
      }
    ],
    "specifications": {
      "color": "Black",
      "connectivity": "2.4GHz Wireless",
      "dpi": "800-3200",
      "buttons": "6",
      "battery": "AA x 1",
      "weight": "85g"
    },
    "inStock": true,
    "stockQuantity": 150,
    "lowStockThreshold": 20,
    "rating": 4.5,
    "reviewCount": 234,
    "tags": ["wireless", "ergonomic", "gaming"],
    "relatedProducts": ["prod-789", "prod-101"],
    "createdAt": "2025-10-20T10:00:00Z",
    "updatedAt": "2025-10-25T10:00:00Z"
  }
}
```

**錯誤回應**:

- `404 Not Found`: 找不到產品

**curl 範例**:

```bash
curl -X GET https://api.ecommerce.com/api/v1/products/prod-456
```

---

### 搜尋產品

具有全文搜尋功能的進階產品搜尋。

**Endpoint**: `GET /api/v1/products/search`

**Authentication**: Not required

**Query Parameters**:

- `q`: 搜尋查詢（必填）
- `page`: 頁碼（預設: 0）
- `size`: 頁面大小（預設: 20）
- `filters`: 篩選條件的 JSON 字串
  - 範例: `{"category": "cat-123", "brand": "TechBrand"}`

**成功回應** (200 OK):

```json
{
  "data": {
    "query": "wireless mouse",
    "results": [
      {
        "id": "prod-456",
        "name": "Wireless Mouse",
        "description": "Ergonomic wireless mouse...",
        "price": 500.00,
        "images": ["https://cdn.ecommerce.com/products/prod-456-1.jpg"],
        "rating": 4.5,
        "inStock": true,
        "relevanceScore": 0.95
      }
    ],
    "suggestions": ["wireless keyboard", "bluetooth mouse"],
    "facets": {
      "categories": [
        {"id": "cat-123", "name": "Computer Accessories", "count": 45}
      ],
      "brands": [
        {"name": "TechBrand", "count": 23},
        {"name": "OfficePro", "count": 15}
      ],
      "priceRanges": [
        {"min": 0, "max": 500, "count": 30},
        {"min": 500, "max": 1000, "count": 15}
      ]
    },
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 45
    }
  }
}
```

**curl 範例**:

```bash
curl -X GET "https://api.ecommerce.com/api/v1/products/search?q=wireless+mouse&page=0&size=20"
```

---

### 依分類取得產品

檢索特定分類中的產品。

**Endpoint**: `GET /api/v1/products/category/{categoryId}`

**Authentication**: Not required

**Path Parameters**:

- `categoryId`: 分類 ID

**Query Parameters**:

- `page`, `size`, `sort`: 標準分頁參數

**成功回應** (200 OK):

```json
{
  "data": {
    "category": {
      "id": "cat-123",
      "name": "Computer Accessories",
      "description": "Accessories for computers and laptops"
    },
    "products": {
      "content": [
        {
          "id": "prod-456",
          "name": "Wireless Mouse",
          "price": 500.00,
          "inStock": true
        }
      ],
      "page": {
        "number": 0,
        "size": 20,
        "totalElements": 45
      }
    }
  }
}
```

**curl 範例**:

```bash
curl -X GET "https://api.ecommerce.com/api/v1/products/category/cat-123?page=0&size=20"
```

---

### 建立產品

建立新產品（僅限 Admin/Seller）。

**Endpoint**: `POST /api/v1/products`

**Authentication**: Required

**Authorization**: 需要 ADMIN 或 SELLER 角色

**Request Body**:

```json
{
  "sku": "WM-2025-002",
  "name": "Gaming Mouse Pro",
  "description": "Professional gaming mouse with RGB lighting",
  "price": 1200.00,
  "categoryId": "cat-123",
  "brand": "GamerTech",
  "specifications": {
    "color": "Black",
    "connectivity": "USB Wired",
    "dpi": "100-16000",
    "buttons": "8"
  },
  "images": [
    {
      "url": "https://cdn.ecommerce.com/products/new-product-1.jpg",
      "alt": "Gaming Mouse Pro",
      "isPrimary": true
    }
  ],
  "stockQuantity": 100,
  "lowStockThreshold": 10,
  "tags": ["gaming", "rgb", "wired"]
}
```

**驗證規則**:

- `sku`: 必填，唯一
- `name`: 必填，2-200 個字元
- `description`: 必填，10-5000 個字元
- `price`: 必填，必須 > 0
- `categoryId`: 必填，必須存在
- `stockQuantity`: 必填，必須 >= 0

**成功回應** (201 Created):

```json
{
  "data": {
    "id": "prod-999",
    "sku": "WM-2025-002",
    "name": "Gaming Mouse Pro",
    "description": "Professional gaming mouse with RGB lighting",
    "price": 1200.00,
    "currency": "TWD",
    "category": {
      "id": "cat-123",
      "name": "Computer Accessories"
    },
    "brand": "GamerTech",
    "inStock": true,
    "stockQuantity": 100,
    "createdAt": "2025-10-25T14:00:00Z"
  }
}
```

**錯誤回應**:

- `400 Bad Request`: 驗證錯誤
- `401 Unauthorized`: 缺少或無效的 token
- `403 Forbidden`: 權限不足
- `409 Conflict`: SKU 已存在

**curl 範例**:

```bash
curl -X POST https://api.ecommerce.com/api/v1/products \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "sku": "WM-2025-002",
    "name": "Gaming Mouse Pro",
    "description": "Professional gaming mouse with RGB lighting",
    "price": 1200.00,
    "categoryId": "cat-123",
    "brand": "GamerTech",
    "stockQuantity": 100
  }'
```

---

### 更新產品

更新產品資訊（僅限 Admin/Seller）。

**Endpoint**: `PUT /api/v1/products/{id}`

**Authentication**: Required

**Authorization**: 需要 ADMIN 或 SELLER 角色

**Path Parameters**:

- `id`: 產品 ID

**Request Body**: 與建立產品相同

**成功回應** (200 OK):

```json
{
  "data": {
    "id": "prod-456",
    "name": "Wireless Mouse Updated",
    "price": 550.00,
    "updatedAt": "2025-10-25T15:00:00Z"
  }
}
```

**curl 範例**:

```bash
curl -X PUT https://api.ecommerce.com/api/v1/products/prod-456 \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Wireless Mouse Updated",
    "price": 550.00
  }'
```

---

### 更新產品庫存

更新產品庫存（僅限 Admin/Seller）。

**Endpoint**: `PATCH /api/v1/products/{id}/stock`

**Authentication**: Required

**Authorization**: 需要 ADMIN 或 SELLER 角色

**Path Parameters**:

- `id`: 產品 ID

**Request Body**:

```json
{
  "quantity": 200,
  "operation": "SET"
}
```

**Operations**:

- `SET`: 設定絕對數量
- `ADD`: 加到目前數量
- `SUBTRACT`: 從目前數量減去

**成功回應** (200 OK):

```json
{
  "data": {
    "id": "prod-456",
    "stockQuantity": 200,
    "inStock": true,
    "updatedAt": "2025-10-25T15:30:00Z"
  }
}
```

**curl 範例**:

```bash
curl -X PATCH https://api.ecommerce.com/api/v1/products/prod-456/stock \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "quantity": 200,
    "operation": "SET"
  }'
```

---

### 刪除產品

刪除產品（僅限 Admin）。

**Endpoint**: `DELETE /api/v1/products/{id}`

**Authentication**: Required

**Authorization**: 需要 ADMIN 角色

**Path Parameters**:

- `id`: 產品 ID

**成功回應** (204 No Content)

**錯誤回應**:

- `401 Unauthorized`: 缺少或無效的 token
- `403 Forbidden`: 權限不足
- `404 Not Found`: 找不到產品
- `409 Conflict`: 產品有使用中的訂單

**curl 範例**:

```bash
curl -X DELETE https://api.ecommerce.com/api/v1/products/prod-456 \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN"
```

---

### 取得產品評價

檢索產品的評價。

**Endpoint**: `GET /api/v1/products/{id}/reviews`

**Authentication**: Not required

**Path Parameters**:

- `id`: 產品 ID

**Query Parameters**:

- `page`, `size`: 標準分頁
- `sort`: 依 `rating,desc`、`createdAt,desc`、`helpful,desc` 排序
- `rating`: 依評分篩選（1-5）

**成功回應** (200 OK):

```json
{
  "data": {
    "productId": "prod-456",
    "averageRating": 4.5,
    "totalReviews": 234,
    "ratingDistribution": {
      "5": 150,
      "4": 60,
      "3": 15,
      "2": 5,
      "1": 4
    },
    "reviews": {
      "content": [
        {
          "id": "review-123",
          "customerId": "cust-789",
          "customerName": "John D.",
          "rating": 5,
          "title": "Excellent mouse!",
          "comment": "Very comfortable and responsive. Highly recommended!",
          "verified": true,
          "helpful": 45,
          "createdAt": "2025-10-20T10:00:00Z"
        }
      ],
      "page": {
        "number": 0,
        "size": 20,
        "totalElements": 234
      }
    }
  }
}
```

**curl 範例**:

```bash
curl -X GET "https://api.ecommerce.com/api/v1/products/prod-456/reviews?page=0&size=20&sort=helpful,desc"
```

---

### 取得相關產品

取得與特定產品相關的產品。

**Endpoint**: `GET /api/v1/products/{id}/related`

**Authentication**: Not required

**Path Parameters**:

- `id`: 產品 ID

**Query Parameters**:

- `limit`: 相關產品數量（預設: 10，最大: 20）

**成功回應** (200 OK):

```json
{
  "data": {
    "products": [
      {
        "id": "prod-789",
        "name": "Mechanical Keyboard",
        "price": 1500.00,
        "images": ["https://cdn.ecommerce.com/products/prod-789-1.jpg"],
        "rating": 4.7,
        "inStock": true
      }
    ]
  }
}
```

**curl 範例**:

```bash
curl -X GET "https://api.ecommerce.com/api/v1/products/prod-456/related?limit=10"
```

---

## 資料模型

### Product Object

```json
{
  "id": "string",
  "sku": "string",
  "name": "string",
  "description": "string",
  "price": "number",
  "originalPrice": "number",
  "discount": "number",
  "discountPercentage": "number",
  "currency": "string",
  "category": {
    "id": "string",
    "name": "string",
    "path": "string"
  },
  "brand": "string",
  "images": [
    {
      "url": "string",
      "alt": "string",
      "isPrimary": "boolean"
    }
  ],
  "specifications": "object",
  "inStock": "boolean",
  "stockQuantity": "number",
  "lowStockThreshold": "number",
  "rating": "number",
  "reviewCount": "number",
  "tags": ["string"],
  "relatedProducts": ["string"],
  "createdAt": "string (ISO 8601)",
  "updatedAt": "string (ISO 8601)"
}
```

### 產品狀態

| Status | Description |
|--------|-------------|
| In Stock | 可供購買 |
| Low Stock | 庫存低於臨界值 |
| Out of Stock | 無庫存 |
| Discontinued | 不再販售 |

## 搜尋和篩選

### 搜尋功能

1. **全文搜尋**: 搜尋產品名稱和描述
2. **多面向搜尋**: 依分類、品牌、價格範圍篩選
3. **自動建議**: 即時搜尋建議
4. **相關性排名**: 結果依相關性分數排序

### 篩選選項

- **分類**: 依產品分類篩選
- **品牌**: 依品牌名稱篩選
- **價格範圍**: 最低和最高價格篩選
- **評分**: 依最低評分篩選
- **可用性**: 有庫存/無庫存
- **標籤**: 依產品標籤篩選

## 業務規則

1. **庫存管理**: 自動低庫存警示
2. **價格更新**: 追蹤價格歷史
3. **產品可見性**: 無庫存產品仍然可見
4. **評價驗證**: 僅已驗證的購買可以評價
5. **圖片需求**: 至少需要一張主要圖片
6. **SKU 唯一性**: SKU 必須在所有產品中唯一

## 錯誤代碼

| Code | Description | Solution |
|------|-------------|----------|
| `PRODUCT_NOT_FOUND` | 找不到產品 ID | 檢查產品 ID |
| `PRODUCT_SKU_EXISTS` | SKU 已存在 | 使用不同的 SKU |
| `PRODUCT_OUT_OF_STOCK` | 產品無庫存 | 檢查庫存或等待 |
| `PRODUCT_INVALID_PRICE` | 價格必須為正數 | 提供有效價格 |
| `PRODUCT_INVALID_CATEGORY` | 找不到分類 | 檢查分類 ID |

## 相關文件

- [Order API](orders.md) - 訂單管理
- [Inventory API](inventory.md) - 庫存管理
- [Shopping Cart API](shopping-cart.md) - 購物車操作
- [Authentication](../authentication.md) - 驗證和授權

---

**最後更新**: 2025-10-25
**API 版本**: v1
