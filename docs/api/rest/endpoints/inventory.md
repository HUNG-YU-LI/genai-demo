# Inventory API

## 概述

Inventory API 提供了管理產品庫存水平、保留和倉庫作業的端點。此 API 主要供內部系統和管理員使用。

**Base Path**: `/api/v1/inventory`

**Authentication**: 所有端點皆需要驗證

**Authorization**: 大部分端點需要 ADMIN 或 WAREHOUSE 角色

## Endpoints

### Check Product Availability

檢查產品的庫存可用性。

**Endpoint**: `GET /api/v1/inventory/products/{productId}/availability`

**Authentication**: 不需要（公開端點）

**Path Parameters**:

- `productId`: Product ID

**Query Parameters**:

- `quantity`: 請求數量（預設：1）
- `warehouseId`: 特定倉庫（選填）

**Success Response** (200 OK):

```json
{
  "data": {
    "productId": "prod-456",
    "available": true,
    "totalStock": 150,
    "availableStock": 145,
    "reservedStock": 5,
    "requestedQuantity": 2,
    "warehouses": [
      {
        "warehouseId": "wh-taipei",
        "warehouseName": "Taipei Main Warehouse",
        "stock": 100,
        "available": 95
      },
      {
        "warehouseId": "wh-taichung",
        "warehouseName": "Taichung Warehouse",
        "stock": 50,
        "available": 50
      }
    ],
    "estimatedRestockDate": null
  }
}
```

**Out of Stock Response** (200 OK):

```json
{
  "data": {
    "productId": "prod-789",
    "available": false,
    "totalStock": 0,
    "availableStock": 0,
    "reservedStock": 0,
    "requestedQuantity": 1,
    "estimatedRestockDate": "2025-11-01T00:00:00Z"
  }
}
```

**Error Responses**:

- `404 Not Found`: 找不到產品

**curl Example**:

```bash
curl -X GET "https://api.ecommerce.com/api/v1/inventory/products/prod-456/availability?quantity=2"
```

---

### Get Inventory by Product

取得產品的詳細庫存資訊。

**Endpoint**: `GET /api/v1/inventory/products/{productId}`

**Authentication**: 需要

**Authorization**: 需要 ADMIN 或 WAREHOUSE 角色

**Path Parameters**:

- `productId`: Product ID

**Success Response** (200 OK):

```json
{
  "data": {
    "productId": "prod-456",
    "productName": "Wireless Mouse",
    "sku": "WM-001",
    "totalStock": 150,
    "availableStock": 145,
    "reservedStock": 5,
    "inTransitStock": 50,
    "reorderPoint": 20,
    "reorderQuantity": 100,
    "warehouses": [
      {
        "warehouseId": "wh-taipei",
        "warehouseName": "Taipei Main Warehouse",
        "location": "A-12-03",
        "stock": 100,
        "reserved": 3,
        "available": 97
      },
      {
        "warehouseId": "wh-taichung",
        "warehouseName": "Taichung Warehouse",
        "location": "B-05-12",
        "stock": 50,
        "reserved": 2,
        "available": 48
      }
    ],
    "lastStockUpdate": "2025-10-25T10:00:00Z",
    "lastRestockDate": "2025-10-20T14:00:00Z",
    "nextRestockDate": "2025-11-05T00:00:00Z"
  }
}
```

**Error Responses**:

- `403 Forbidden`: 權限不足
- `404 Not Found`: 找不到產品

**curl Example**:

```bash
curl -X GET https://api.ecommerce.com/api/v1/inventory/products/prod-456 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Reserve Inventory

為訂單保留庫存（內部使用）。

**Endpoint**: `POST /api/v1/inventory/reservations`

**Authentication**: 需要

**Authorization**: 需要 SYSTEM 或 ADMIN 角色

**Request Body**:

```json
{
  "orderId": "order-456",
  "items": [
    {
      "productId": "prod-456",
      "quantity": 2,
      "warehouseId": "wh-taipei"
    },
    {
      "productId": "prod-789",
      "quantity": 1,
      "warehouseId": "wh-taipei"
    }
  ],
  "expiresAt": "2025-10-25T15:00:00Z"
}
```

**Validation Rules**:

- `orderId`: 必填，唯一
- `items`: 必填，至少一個項目
- `quantity`: 必填，正整數
- `expiresAt`: 選填，預設為現在起 15 分鐘

**Success Response** (201 Created):

```json
{
  "data": {
    "reservationId": "res-123",
    "orderId": "order-456",
    "status": "RESERVED",
    "items": [
      {
        "productId": "prod-456",
        "quantity": 2,
        "warehouseId": "wh-taipei",
        "reserved": true
      },
      {
        "productId": "prod-789",
        "quantity": 1,
        "warehouseId": "wh-taipei",
        "reserved": true
      }
    ],
    "createdAt": "2025-10-25T14:00:00Z",
    "expiresAt": "2025-10-25T15:00:00Z"
  }
}
```

**Partial Reservation Response** (207 Multi-Status):

```json
{
  "data": {
    "reservationId": "res-123",
    "orderId": "order-456",
    "status": "PARTIAL",
    "items": [
      {
        "productId": "prod-456",
        "quantity": 2,
        "warehouseId": "wh-taipei",
        "reserved": true
      },
      {
        "productId": "prod-789",
        "quantity": 1,
        "warehouseId": "wh-taipei",
        "reserved": false,
        "reason": "INSUFFICIENT_STOCK",
        "availableQuantity": 0
      }
    ],
    "createdAt": "2025-10-25T14:00:00Z"
  }
}
```

**Error Responses**:

- `400 Bad Request`: 驗證錯誤
- `403 Forbidden`: 權限不足
- `409 Conflict`: 所有項目庫存不足

**curl Example**:

```bash
curl -X POST https://api.ecommerce.com/api/v1/inventory/reservations \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "order-456",
    "items": [
      {
        "productId": "prod-456",
        "quantity": 2,
        "warehouseId": "wh-taipei"
      }
    ]
  }'
```

---

### Confirm Reservation

確認保留並從可用庫存扣除。

**Endpoint**: `POST /api/v1/inventory/reservations/{reservationId}/confirm`

**Authentication**: 需要

**Authorization**: 需要 SYSTEM 或 ADMIN 角色

**Path Parameters**:

- `reservationId`: Reservation ID

**Success Response** (200 OK):

```json
{
  "data": {
    "reservationId": "res-123",
    "orderId": "order-456",
    "status": "CONFIRMED",
    "items": [
      {
        "productId": "prod-456",
        "quantity": 2,
        "warehouseId": "wh-taipei",
        "deducted": true
      }
    ],
    "confirmedAt": "2025-10-25T14:30:00Z"
  }
}
```

**Error Responses**:

- `403 Forbidden`: 權限不足
- `404 Not Found`: 找不到保留
- `409 Conflict`: 保留已過期或已確認

**curl Example**:

```bash
curl -X POST https://api.ecommerce.com/api/v1/inventory/reservations/res-123/confirm \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Cancel Reservation

取消保留並釋放已保留的庫存。

**Endpoint**: `POST /api/v1/inventory/reservations/{reservationId}/cancel`

**Authentication**: 需要

**Authorization**: 需要 SYSTEM 或 ADMIN 角色

**Path Parameters**:

- `reservationId`: Reservation ID

**Success Response** (200 OK):

```json
{
  "data": {
    "reservationId": "res-123",
    "orderId": "order-456",
    "status": "CANCELLED",
    "items": [
      {
        "productId": "prod-456",
        "quantity": 2,
        "warehouseId": "wh-taipei",
        "released": true
      }
    ],
    "cancelledAt": "2025-10-25T14:45:00Z"
  }
}
```

**curl Example**:

```bash
curl -X POST https://api.ecommerce.com/api/v1/inventory/reservations/res-123/cancel \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Update Stock Level

更新倉庫中產品的庫存水平。

**Endpoint**: `PUT /api/v1/inventory/products/{productId}/warehouses/{warehouseId}`

**Authentication**: 需要

**Authorization**: 需要 ADMIN 或 WAREHOUSE 角色

**Path Parameters**:

- `productId`: Product ID
- `warehouseId`: Warehouse ID

**Request Body**:

```json
{
  "quantity": 150,
  "operation": "SET",
  "reason": "RESTOCK",
  "notes": "Received shipment from supplier"
}
```

**Operation Types**:

- `SET`: 設定絕對數量
- `ADD`: 加到當前數量
- `SUBTRACT`: 從當前數量減去

**Reason Codes**:

- `RESTOCK`: 庫存補貨
- `ADJUSTMENT`: 庫存調整
- `DAMAGE`: 損壞商品
- `RETURN`: 顧客退貨
- `TRANSFER`: 倉庫轉移

**Success Response** (200 OK):

```json
{
  "data": {
    "productId": "prod-456",
    "warehouseId": "wh-taipei",
    "previousQuantity": 100,
    "newQuantity": 150,
    "operation": "SET",
    "reason": "RESTOCK",
    "updatedBy": "admin-user-123",
    "updatedAt": "2025-10-25T15:00:00Z"
  }
}
```

**Error Responses**:

- `400 Bad Request`: 無效的操作或數量
- `403 Forbidden`: 權限不足
- `404 Not Found`: 找不到產品或倉庫

**curl Example**:

```bash
curl -X PUT https://api.ecommerce.com/api/v1/inventory/products/prod-456/warehouses/wh-taipei \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "quantity": 150,
    "operation": "SET",
    "reason": "RESTOCK"
  }'
```

---

### Get Low Stock Products

取得庫存低於補貨點的產品。

**Endpoint**: `GET /api/v1/inventory/low-stock`

**Authentication**: 需要

**Authorization**: 需要 ADMIN 或 WAREHOUSE 角色

**Query Parameters**:

- `page`: 頁碼（預設：0）
- `size`: 每頁大小（預設：20）
- `warehouseId`: 依倉庫篩選（選填）

**Success Response** (200 OK):

```json
{
  "data": {
    "content": [
      {
        "productId": "prod-789",
        "productName": "USB Cable",
        "sku": "UC-001",
        "currentStock": 15,
        "reorderPoint": 20,
        "reorderQuantity": 100,
        "status": "LOW_STOCK",
        "warehouseId": "wh-taipei",
        "lastRestockDate": "2025-10-15T00:00:00Z"
      },
      {
        "productId": "prod-890",
        "productName": "HDMI Cable",
        "sku": "HC-001",
        "currentStock": 0,
        "reorderPoint": 10,
        "reorderQuantity": 50,
        "status": "OUT_OF_STOCK",
        "warehouseId": "wh-taipei",
        "lastRestockDate": "2025-10-10T00:00:00Z"
      }
    ],
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 12,
      "totalPages": 1
    }
  }
}
```

**curl Example**:

```bash
curl -X GET "https://api.ecommerce.com/api/v1/inventory/low-stock?warehouseId=wh-taipei" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Get Inventory Movement History

取得產品的庫存異動歷史。

**Endpoint**: `GET /api/v1/inventory/products/{productId}/movements`

**Authentication**: 需要

**Authorization**: 需要 ADMIN 或 WAREHOUSE 角色

**Path Parameters**:

- `productId`: Product ID

**Query Parameters**:

- `page`: 頁碼（預設：0）
- `size`: 每頁大小（預設：20）
- `startDate`: 從日期篩選（ISO 8601）
- `endDate`: 至日期篩選（ISO 8601）
- `warehouseId`: 依倉庫篩選（選填）
- `type`: 依異動類型篩選（選填）

**Success Response** (200 OK):

```json
{
  "data": {
    "content": [
      {
        "id": "mov-123",
        "productId": "prod-456",
        "warehouseId": "wh-taipei",
        "type": "RESTOCK",
        "quantity": 50,
        "previousQuantity": 100,
        "newQuantity": 150,
        "reason": "RESTOCK",
        "notes": "Received shipment from supplier",
        "performedBy": "admin-user-123",
        "createdAt": "2025-10-25T15:00:00Z"
      },
      {
        "id": "mov-124",
        "productId": "prod-456",
        "warehouseId": "wh-taipei",
        "type": "SALE",
        "quantity": -2,
        "previousQuantity": 150,
        "newQuantity": 148,
        "orderId": "order-456",
        "createdAt": "2025-10-25T14:30:00Z"
      }
    ],
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 45,
      "totalPages": 3
    }
  }
}
```

**Movement Types**:

- `RESTOCK`: 庫存補貨
- `SALE`: 訂單履行
- `RETURN`: 顧客退貨
- `ADJUSTMENT`: 手動調整
- `DAMAGE`: 損壞商品報廢
- `TRANSFER_IN`: 從其他倉庫轉入
- `TRANSFER_OUT`: 轉出至其他倉庫

**curl Example**:

```bash
curl -X GET "https://api.ecommerce.com/api/v1/inventory/products/prod-456/movements?startDate=2025-10-01T00:00:00Z" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## Data Models

### Inventory Object

```json
{
  "productId": "string",
  "productName": "string",
  "sku": "string",
  "totalStock": "number",
  "availableStock": "number",
  "reservedStock": "number",
  "inTransitStock": "number",
  "reorderPoint": "number",
  "reorderQuantity": "number",
  "warehouses": [
    {
      "warehouseId": "string",
      "warehouseName": "string",
      "location": "string",
      "stock": "number",
      "reserved": "number",
      "available": "number"
    }
  ],
  "lastStockUpdate": "string (ISO 8601)",
  "lastRestockDate": "string (ISO 8601)",
  "nextRestockDate": "string (ISO 8601)"
}
```

### Reservation Object

```json
{
  "reservationId": "string",
  "orderId": "string",
  "status": "RESERVED | CONFIRMED | CANCELLED | EXPIRED",
  "items": [
    {
      "productId": "string",
      "quantity": "number",
      "warehouseId": "string",
      "reserved": "boolean"
    }
  ],
  "createdAt": "string (ISO 8601)",
  "expiresAt": "string (ISO 8601)",
  "confirmedAt": "string (ISO 8601)",
  "cancelledAt": "string (ISO 8601)"
}
```

## Business Rules

1. **Stock Calculation**: 可用庫存 = 總庫存 - 已保留庫存
2. **Reservation Expiry**: 保留在未確認的情況下 15 分鐘後過期
3. **Reorder Point**: 當庫存低於補貨點時自動提醒
4. **Multi-Warehouse**: 每個倉庫分別追蹤庫存
5. **Negative Stock**: 不允許，若結果為負數則操作失敗
6. **Concurrent Updates**: 樂觀鎖定防止競爭條件
7. **Audit Trail**: 所有庫存異動皆記錄在案

## Error Codes

| Code | Description | Solution |
|------|-------------|----------|
| `INVENTORY_INSUFFICIENT_STOCK` | 可用庫存不足 | 減少數量或等待補貨 |
| `INVENTORY_RESERVATION_EXPIRED` | 保留已過期 | 建立新的保留 |
| `INVENTORY_RESERVATION_NOT_FOUND` | 找不到保留 | 檢查保留 ID |
| `INVENTORY_NEGATIVE_STOCK` | 操作會導致負庫存 | 檢查當前庫存水平 |
| `INVENTORY_PRODUCT_NOT_FOUND` | 在庫存中找不到產品 | 檢查產品 ID |
| `INVENTORY_WAREHOUSE_NOT_FOUND` | 找不到倉庫 | 檢查倉庫 ID |

## Related Documentation

- [Product API](products.md) - 產品資訊
- [Order API](orders.md) - 訂單處理
- [Shopping Cart API](shopping-cart.md) - 購物車庫存驗證

---

**Last Updated**: 2025-10-25
**API Version**: v1
