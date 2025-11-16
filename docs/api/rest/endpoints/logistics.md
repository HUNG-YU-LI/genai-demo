# Logistics API

## 概述

Logistics API 提供了管理配送、物流追蹤和物流作業的端點。包括貨件建立、追蹤更新和配送管理。

**Base Path**: `/api/v1/logistics`

**Authentication**: 大部分端點需要驗證

## Endpoints

### Create Shipment

為訂單建立新的貨件。

**Endpoint**: `POST /api/v1/logistics/shipments`

**Authentication**: 需要

**Authorization**: 需要 ADMIN 或 WAREHOUSE 角色

**Request Body**:

```json
{
  "orderId": "order-456",
  "warehouseId": "wh-taipei",
  "shippingMethod": "STANDARD",
  "carrier": "CHUNGHWA_POST",
  "shippingAddress": {
    "recipientName": "John Doe",
    "phone": "+886912345678",
    "street": "123 Main St",
    "city": "Taipei",
    "state": "Taiwan",
    "postalCode": "10001",
    "country": "TW"
  },
  "items": [
    {
      "productId": "prod-456",
      "productName": "Wireless Mouse",
      "quantity": 2,
      "weight": 0.2,
      "dimensions": {
        "length": 15,
        "width": 10,
        "height": 5,
        "unit": "cm"
      }
    }
  ],
  "packageWeight": 0.5,
  "packageDimensions": {
    "length": 20,
    "width": 15,
    "height": 10,
    "unit": "cm"
  }
}
```

**Validation Rules**:

- `orderId`: 必填，必須存在
- `shippingMethod`: 必填，必須為以下其中之一：STANDARD, EXPRESS, SAME_DAY
- `carrier`: 必填，必須為以下其中之一：CHUNGHWA_POST, KERRY_TJ, SF_EXPRESS, BLACK_CAT
- `shippingAddress`: 必填，包含所有欄位
- `items`: 必填，至少一個項目

**Success Response** (201 Created):

```json
{
  "data": {
    "shipmentId": "ship-123",
    "orderId": "order-456",
    "trackingNumber": "TW1234567890",
    "status": "PENDING",
    "carrier": "CHUNGHWA_POST",
    "shippingMethod": "STANDARD",
    "estimatedDeliveryDate": "2025-10-28T00:00:00Z",
    "shippingAddress": {
      "recipientName": "John Doe",
      "phone": "+886912345678",
      "street": "123 Main St",
      "city": "Taipei",
      "state": "Taiwan",
      "postalCode": "10001",
      "country": "TW"
    },
    "packageWeight": 0.5,
    "packageDimensions": {
      "length": 20,
      "width": 15,
      "height": 10,
      "unit": "cm"
    },
    "shippingCost": 100.00,
    "currency": "TWD",
    "createdAt": "2025-10-25T14:00:00Z"
  }
}
```

**Error Responses**:

- `400 Bad Request`: 驗證錯誤
- `403 Forbidden`: 權限不足
- `404 Not Found`: 找不到訂單
- `409 Conflict`: 訂單的貨件已存在

**curl Example**:

```bash
curl -X POST https://api.ecommerce.com/api/v1/logistics/shipments \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "order-456",
    "warehouseId": "wh-taipei",
    "shippingMethod": "STANDARD",
    "carrier": "CHUNGHWA_POST"
  }'
```

---

### Get Shipment by ID

根據 ID 取得貨件詳細資料。

**Endpoint**: `GET /api/v1/logistics/shipments/{shipmentId}`

**Authentication**: 需要

**Authorization**: 使用者可查看自己的貨件，或需要 ADMIN/WAREHOUSE 角色

**Path Parameters**:

- `shipmentId`: Shipment ID

**Success Response** (200 OK):

```json
{
  "data": {
    "shipmentId": "ship-123",
    "orderId": "order-456",
    "trackingNumber": "TW1234567890",
    "status": "IN_TRANSIT",
    "carrier": "CHUNGHWA_POST",
    "shippingMethod": "STANDARD",
    "shippingAddress": {
      "recipientName": "John Doe",
      "phone": "+886912345678",
      "street": "123 Main St",
      "city": "Taipei",
      "state": "Taiwan",
      "postalCode": "10001",
      "country": "TW"
    },
    "packageWeight": 0.5,
    "shippingCost": 100.00,
    "currency": "TWD",
    "estimatedDeliveryDate": "2025-10-28T00:00:00Z",
    "actualDeliveryDate": null,
    "trackingEvents": [
      {
        "status": "PICKED_UP",
        "location": "Taipei Main Warehouse",
        "timestamp": "2025-10-25T15:00:00Z",
        "description": "Package picked up by carrier"
      },
      {
        "status": "IN_TRANSIT",
        "location": "Taipei Distribution Center",
        "timestamp": "2025-10-25T18:00:00Z",
        "description": "Package in transit"
      }
    ],
    "createdAt": "2025-10-25T14:00:00Z",
    "updatedAt": "2025-10-25T18:00:00Z"
  }
}
```

**Error Responses**:

- `403 Forbidden`: 權限不足
- `404 Not Found`: 找不到貨件

**curl Example**:

```bash
curl -X GET https://api.ecommerce.com/api/v1/logistics/shipments/ship-123 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Track Shipment

根據追蹤號碼追蹤貨件（公開端點）。

**Endpoint**: `GET /api/v1/logistics/track/{trackingNumber}`

**Authentication**: 不需要

**Path Parameters**:

- `trackingNumber`: 追蹤號碼

**Success Response** (200 OK):

```json
{
  "data": {
    "trackingNumber": "TW1234567890",
    "status": "IN_TRANSIT",
    "carrier": "CHUNGHWA_POST",
    "estimatedDeliveryDate": "2025-10-28T00:00:00Z",
    "trackingEvents": [
      {
        "status": "PICKED_UP",
        "location": "Taipei Main Warehouse",
        "timestamp": "2025-10-25T15:00:00Z",
        "description": "Package picked up by carrier"
      },
      {
        "status": "IN_TRANSIT",
        "location": "Taipei Distribution Center",
        "timestamp": "2025-10-25T18:00:00Z",
        "description": "Package in transit"
      },
      {
        "status": "OUT_FOR_DELIVERY",
        "location": "Taipei Delivery Station",
        "timestamp": "2025-10-26T08:00:00Z",
        "description": "Out for delivery"
      }
    ],
    "lastUpdate": "2025-10-26T08:00:00Z"
  }
}
```

**Error Responses**:

- `404 Not Found`: 找不到追蹤號碼

**curl Example**:

```bash
curl -X GET https://api.ecommerce.com/api/v1/logistics/track/TW1234567890
```

---

### Update Shipment Status

更新貨件狀態並新增追蹤事件。

**Endpoint**: `POST /api/v1/logistics/shipments/{shipmentId}/events`

**Authentication**: 需要

**Authorization**: 需要 ADMIN、WAREHOUSE 或 CARRIER 角色

**Path Parameters**:

- `shipmentId`: Shipment ID

**Request Body**:

```json
{
  "status": "DELIVERED",
  "location": "Customer Address",
  "description": "Package delivered successfully",
  "recipientName": "John Doe",
  "signature": "base64_encoded_signature_image"
}
```

**Status Values**:

- `PENDING`: 貨件已建立，等待取件
- `PICKED_UP`: 物流業者已取件
- `IN_TRANSIT`: 運送至目的地途中
- `OUT_FOR_DELIVERY`: 配送中
- `DELIVERED`: 成功送達
- `FAILED_DELIVERY`: 配送嘗試失敗
- `RETURNED`: 退回寄件人
- `CANCELLED`: 貨件已取消

**Success Response** (201 Created):

```json
{
  "data": {
    "shipmentId": "ship-123",
    "status": "DELIVERED",
    "trackingEvent": {
      "status": "DELIVERED",
      "location": "Customer Address",
      "timestamp": "2025-10-26T14:30:00Z",
      "description": "Package delivered successfully",
      "recipientName": "John Doe"
    },
    "actualDeliveryDate": "2025-10-26T14:30:00Z",
    "updatedAt": "2025-10-26T14:30:00Z"
  }
}
```

**Error Responses**:

- `400 Bad Request`: 無效的狀態轉換
- `403 Forbidden`: 權限不足
- `404 Not Found`: 找不到貨件

**curl Example**:

```bash
curl -X POST https://api.ecommerce.com/api/v1/logistics/shipments/ship-123/events \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "DELIVERED",
    "location": "Customer Address",
    "description": "Package delivered successfully"
  }'
```

---

### Get Shipments by Order

取得訂單的所有貨件。

**Endpoint**: `GET /api/v1/logistics/orders/{orderId}/shipments`

**Authentication**: 需要

**Authorization**: 使用者可查看自己的訂單貨件，或需要 ADMIN 角色

**Path Parameters**:

- `orderId`: Order ID

**Success Response** (200 OK):

```json
{
  "data": [
    {
      "shipmentId": "ship-123",
      "trackingNumber": "TW1234567890",
      "status": "DELIVERED",
      "carrier": "CHUNGHWA_POST",
      "shippingMethod": "STANDARD",
      "estimatedDeliveryDate": "2025-10-28T00:00:00Z",
      "actualDeliveryDate": "2025-10-26T14:30:00Z",
      "createdAt": "2025-10-25T14:00:00Z"
    }
  ]
}
```

**curl Example**:

```bash
curl -X GET https://api.ecommerce.com/api/v1/logistics/orders/order-456/shipments \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### List Shipments

使用篩選條件列出貨件（管理員）。

**Endpoint**: `GET /api/v1/logistics/shipments`

**Authentication**: 需要

**Authorization**: 需要 ADMIN 或 WAREHOUSE 角色

**Query Parameters**:

- `page`: 頁碼（預設：0）
- `size`: 每頁大小（預設：20）
- `status`: 依狀態篩選
- `carrier`: 依物流業者篩選
- `warehouseId`: 依倉庫篩選
- `startDate`: 從日期篩選（ISO 8601）
- `endDate`: 至日期篩選（ISO 8601）

**Success Response** (200 OK):

```json
{
  "data": {
    "content": [
      {
        "shipmentId": "ship-123",
        "orderId": "order-456",
        "trackingNumber": "TW1234567890",
        "status": "DELIVERED",
        "carrier": "CHUNGHWA_POST",
        "recipientName": "John Doe",
        "estimatedDeliveryDate": "2025-10-28T00:00:00Z",
        "actualDeliveryDate": "2025-10-26T14:30:00Z",
        "createdAt": "2025-10-25T14:00:00Z"
      }
    ],
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 150,
      "totalPages": 8
    }
  }
}
```

**curl Example**:

```bash
curl -X GET "https://api.ecommerce.com/api/v1/logistics/shipments?status=IN_TRANSIT&carrier=CHUNGHWA_POST" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Calculate Shipping Cost

計算訂單的運費。

**Endpoint**: `POST /api/v1/logistics/calculate-shipping`

**Authentication**: 需要

**Request Body**:

```json
{
  "shippingMethod": "STANDARD",
  "carrier": "CHUNGHWA_POST",
  "destinationAddress": {
    "city": "Taipei",
    "state": "Taiwan",
    "postalCode": "10001",
    "country": "TW"
  },
  "packageWeight": 0.5,
  "packageDimensions": {
    "length": 20,
    "width": 15,
    "height": 10,
    "unit": "cm"
  },
  "declaredValue": 1500.00
}
```

**Success Response** (200 OK):

```json
{
  "data": {
    "shippingMethod": "STANDARD",
    "carrier": "CHUNGHWA_POST",
    "baseCost": 80.00,
    "weightSurcharge": 10.00,
    "dimensionSurcharge": 5.00,
    "insuranceFee": 5.00,
    "totalCost": 100.00,
    "currency": "TWD",
    "estimatedDeliveryDays": 3,
    "estimatedDeliveryDate": "2025-10-28T00:00:00Z"
  }
}
```

**curl Example**:

```bash
curl -X POST https://api.ecommerce.com/api/v1/logistics/calculate-shipping \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "shippingMethod": "STANDARD",
    "carrier": "CHUNGHWA_POST",
    "packageWeight": 0.5
  }'
```

---

### Get Available Carriers

取得目的地可用的物流業者。

**Endpoint**: `GET /api/v1/logistics/carriers`

**Authentication**: 不需要

**Query Parameters**:

- `country`: 目的地國家代碼（例如：TW）
- `city`: 目的地城市（選填）

**Success Response** (200 OK):

```json
{
  "data": [
    {
      "code": "CHUNGHWA_POST",
      "name": "Chunghwa Post",
      "description": "Taiwan national postal service",
      "shippingMethods": [
        {
          "code": "STANDARD",
          "name": "Standard Shipping",
          "estimatedDays": 3,
          "baseCost": 80.00
        },
        {
          "code": "EXPRESS",
          "name": "Express Shipping",
          "estimatedDays": 1,
          "baseCost": 150.00
        }
      ],
      "trackingSupported": true,
      "insuranceAvailable": true
    },
    {
      "code": "BLACK_CAT",
      "name": "Black Cat Delivery",
      "description": "Taiwan courier service",
      "shippingMethods": [
        {
          "code": "SAME_DAY",
          "name": "Same Day Delivery",
          "estimatedDays": 0,
          "baseCost": 200.00
        }
      ],
      "trackingSupported": true,
      "insuranceAvailable": true
    }
  ]
}
```

**curl Example**:

```bash
curl -X GET "https://api.ecommerce.com/api/v1/logistics/carriers?country=TW"
```

---

### Request Return Shipment

為訂單請求退貨貨件。

**Endpoint**: `POST /api/v1/logistics/returns`

**Authentication**: 需要

**Request Body**:

```json
{
  "orderId": "order-456",
  "originalShipmentId": "ship-123",
  "reason": "DEFECTIVE",
  "description": "Product not working properly",
  "items": [
    {
      "productId": "prod-456",
      "quantity": 1
    }
  ]
}
```

**Reason Codes**:

- `DEFECTIVE`: 產品瑕疵
- `WRONG_ITEM`: 收到錯誤商品
- `NOT_AS_DESCRIBED`: 與描述不符
- `CHANGED_MIND`: 顧客改變主意
- `OTHER`: 其他原因

**Success Response** (201 Created):

```json
{
  "data": {
    "returnId": "ret-123",
    "orderId": "order-456",
    "status": "PENDING_APPROVAL",
    "reason": "DEFECTIVE",
    "description": "Product not working properly",
    "returnShipment": {
      "shipmentId": "ship-ret-123",
      "trackingNumber": "TW-RET-1234567890",
      "carrier": "CHUNGHWA_POST",
      "pickupAddress": {
        "recipientName": "John Doe",
        "phone": "+886912345678",
        "street": "123 Main St",
        "city": "Taipei",
        "postalCode": "10001"
      },
      "estimatedPickupDate": "2025-10-27T00:00:00Z"
    },
    "createdAt": "2025-10-26T15:00:00Z"
  }
}
```

**curl Example**:

```bash
curl -X POST https://api.ecommerce.com/api/v1/logistics/returns \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "order-456",
    "reason": "DEFECTIVE",
    "description": "Product not working properly"
  }'
```

---

## Data Models

### Shipment Object

```json
{
  "shipmentId": "string",
  "orderId": "string",
  "trackingNumber": "string",
  "status": "PENDING | PICKED_UP | IN_TRANSIT | OUT_FOR_DELIVERY | DELIVERED | FAILED_DELIVERY | RETURNED | CANCELLED",
  "carrier": "string",
  "shippingMethod": "STANDARD | EXPRESS | SAME_DAY",
  "shippingAddress": {
    "recipientName": "string",
    "phone": "string",
    "street": "string",
    "city": "string",
    "state": "string",
    "postalCode": "string",
    "country": "string"
  },
  "packageWeight": "number",
  "packageDimensions": {
    "length": "number",
    "width": "number",
    "height": "number",
    "unit": "string"
  },
  "shippingCost": "number",
  "currency": "string",
  "estimatedDeliveryDate": "string (ISO 8601)",
  "actualDeliveryDate": "string (ISO 8601)",
  "trackingEvents": [
    {
      "status": "string",
      "location": "string",
      "timestamp": "string (ISO 8601)",
      "description": "string"
    }
  ],
  "createdAt": "string (ISO 8601)",
  "updatedAt": "string (ISO 8601)"
}
```

### Supported Carriers

| Code | Name | Coverage | Tracking |
|------|------|----------|----------|
| CHUNGHWA_POST | Chunghwa Post | 台灣全境 | 是 |
| BLACK_CAT | Black Cat Delivery | 台灣全境 | 是 |
| KERRY_TJ | Kerry TJ Logistics | 台灣全境 | 是 |
| SF_EXPRESS | SF Express | 台灣 + 國際 | 是 |

## Business Rules

1. **Shipment Creation**: 每筆訂單一個貨件（或多個以進行部分配送）
2. **Tracking Updates**: 從物流業者 API 即時更新
3. **Delivery Confirmation**: 需要收件人簽名或照片證明
4. **Failed Delivery**: 最多 3 次配送嘗試
5. **Return Window**: 送達日期起 7 天內
6. **Shipping Cost**: 根據重量、尺寸和目的地計算
7. **Insurance**: 選填，基於申報價值

## Error Codes

| Code | Description | Solution |
|------|-------------|----------|
| `LOGISTICS_SHIPMENT_NOT_FOUND` | 找不到貨件 | 檢查貨件 ID |
| `LOGISTICS_TRACKING_NOT_FOUND` | 找不到追蹤號碼 | 檢查追蹤號碼 |
| `LOGISTICS_INVALID_STATUS_TRANSITION` | 無效的狀態變更 | 檢查目前狀態 |
| `LOGISTICS_CARRIER_NOT_AVAILABLE` | 物流業者不支援該目的地 | 選擇其他物流業者 |
| `LOGISTICS_SHIPMENT_ALREADY_EXISTS` | 訂單的貨件已存在 | 使用現有貨件 |
| `LOGISTICS_RETURN_WINDOW_EXPIRED` | 退貨期限已過 | 聯絡客服 |

## Related Documentation

- [Order API](orders.md) - 訂單管理
- [Customer API](customers.md) - 顧客地址
- [Notification API](notifications.md) - 配送通知

---

**Last Updated**: 2025-10-25
**API Version**: v1
