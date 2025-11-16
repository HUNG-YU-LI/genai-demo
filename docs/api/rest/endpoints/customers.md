# Customer API

## 概述

Customer API 提供管理客戶資料的 endpoints，包括建立、檢索、更新和刪除。客戶是電子商務平台的主要使用者。

**Base Path**: `/api/v1/customers`

**Authentication**: 除註冊外，所有 endpoints 都需要身份驗證

## Endpoints

### Create Customer (Register)

建立新的客戶帳戶。

**Endpoint**: `POST /api/v1/customers`

**Authentication**: 不需要

**Request Body**:

```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "SecurePassword123!",
  "phone": "+886912345678",
  "address": {
    "street": "123 Main St",
    "city": "Taipei",
    "state": "Taiwan",
    "postalCode": "10001",
    "country": "TW"
  }
}
```

**驗證規則**:

- `name`: 必填，2-100 個字元
- `email`: 必填，有效的 email 格式，唯一
- `password`: 必填，最少 8 個字元，必須包含大寫、小寫和數字
- `phone`: 選填，有效的電話格式
- `address`: 必填物件，所有欄位皆必填

**成功回應** (201 Created):

```json
{
  "data": {
    "id": "cust-123",
    "name": "John Doe",
    "email": "john@example.com",
    "phone": "+886912345678",
    "address": {
      "street": "123 Main St",
      "city": "Taipei",
      "state": "Taiwan",
      "postalCode": "10001",
      "country": "TW"
    },
    "membershipLevel": "STANDARD",
    "createdAt": "2025-10-25T10:30:00Z",
    "updatedAt": "2025-10-25T10:30:00Z"
  },
  "metadata": {
    "requestId": "req-abc-123",
    "timestamp": "2025-10-25T10:30:00Z",
    "version": "v1"
  }
}
```

**錯誤回應**:

- `400 Bad Request`: 驗證錯誤
- `409 Conflict`: Email 已註冊

**curl 範例**:

```bash
curl -X POST https://api.ecommerce.com/api/v1/customers \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "password": "SecurePassword123!",
    "phone": "+886912345678",
    "address": {
      "street": "123 Main St",
      "city": "Taipei",
      "state": "Taiwan",
      "postalCode": "10001",
      "country": "TW"
    }
  }'
```

---

### Get Customer by ID

依 ID 檢索特定客戶。

**Endpoint**: `GET /api/v1/customers/{id}`

**Authentication**: 必填

**Authorization**: 使用者可存取自己的資料，或需要 ADMIN 角色

**Path Parameters**:

- `id`: Customer ID (例如：`cust-123`)

**成功回應** (200 OK):

```json
{
  "data": {
    "id": "cust-123",
    "name": "John Doe",
    "email": "john@example.com",
    "phone": "+886912345678",
    "address": {
      "street": "123 Main St",
      "city": "Taipei",
      "state": "Taiwan",
      "postalCode": "10001",
      "country": "TW"
    },
    "membershipLevel": "PREMIUM",
    "loyaltyPoints": 1500,
    "createdAt": "2025-10-25T10:30:00Z",
    "updatedAt": "2025-10-25T11:00:00Z"
  },
  "metadata": {
    "requestId": "req-abc-123",
    "timestamp": "2025-10-25T11:00:00Z",
    "version": "v1"
  }
}
```

**錯誤回應**:

- `401 Unauthorized`: 缺少或無效的 token
- `403 Forbidden`: 權限不足
- `404 Not Found`: 找不到客戶

**curl 範例**:

```bash
curl -X GET https://api.ecommerce.com/api/v1/customers/cust-123 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Get Current Customer Profile

檢索已驗證使用者的資料。

**Endpoint**: `GET /api/v1/customers/me`

**Authentication**: 必填

**成功回應** (200 OK):

```json
{
  "data": {
    "id": "cust-123",
    "name": "John Doe",
    "email": "john@example.com",
    "phone": "+886912345678",
    "address": {
      "street": "123 Main St",
      "city": "Taipei",
      "state": "Taiwan",
      "postalCode": "10001",
      "country": "TW"
    },
    "membershipLevel": "PREMIUM",
    "loyaltyPoints": 1500,
    "preferences": {
      "newsletter": true,
      "notifications": true,
      "language": "zh-TW"
    },
    "createdAt": "2025-10-25T10:30:00Z",
    "updatedAt": "2025-10-25T11:00:00Z"
  }
}
```

**錯誤回應**:

- `401 Unauthorized`: 缺少或無效的 token

**curl 範例**:

```bash
curl -X GET https://api.ecommerce.com/api/v1/customers/me \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### List Customers

檢索分頁的客戶列表。

**Endpoint**: `GET /api/v1/customers`

**Authentication**: 必填

**Authorization**: 需要 ADMIN 角色

**Query Parameters**:

- `page`: 頁碼（從 0 開始，預設值：0）
- `size`: 每頁大小（預設值：20，最大值：100）
- `sort`: 排序欄位和方向（預設值：`createdAt,desc`）
  - 範例：`name,asc`、`email,desc`、`createdAt,desc`
- `search`: 搜尋關鍵字（搜尋姓名和 email）
- `membershipLevel`: 依會員等級篩選（STANDARD、PREMIUM、VIP）
- `status`: 依狀態篩選（ACTIVE、INACTIVE、SUSPENDED）

**成功回應** (200 OK):

```json
{
  "data": {
    "content": [
      {
        "id": "cust-123",
        "name": "John Doe",
        "email": "john@example.com",
        "membershipLevel": "PREMIUM",
        "status": "ACTIVE",
        "createdAt": "2025-10-25T10:30:00Z"
      },
      {
        "id": "cust-124",
        "name": "Jane Smith",
        "email": "jane@example.com",
        "membershipLevel": "STANDARD",
        "status": "ACTIVE",
        "createdAt": "2025-10-24T09:15:00Z"
      }
    ],
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 150,
      "totalPages": 8,
      "first": true,
      "last": false
    }
  }
}
```

**錯誤回應**:

- `401 Unauthorized`: 缺少或無效的 token
- `403 Forbidden`: 權限不足（需要 ADMIN）

**curl 範例**:

```bash
# 基本列表
curl -X GET "https://api.ecommerce.com/api/v1/customers?page=0&size=20" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# 使用搜尋和篩選
curl -X GET "https://api.ecommerce.com/api/v1/customers?search=john&membershipLevel=PREMIUM&sort=name,asc" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Update Customer

更新客戶資訊。

**Endpoint**: `PUT /api/v1/customers/{id}`

**Authentication**: 必填

**Authorization**: 使用者可更新自己的資料，或需要 ADMIN 角色

**Path Parameters**:

- `id`: Customer ID

**Request Body**:

```json
{
  "name": "John Doe Updated",
  "phone": "+886987654321",
  "address": {
    "street": "456 New St",
    "city": "Taipei",
    "state": "Taiwan",
    "postalCode": "10002",
    "country": "TW"
  }
}
```

**驗證規則**:

- `name`: 選填，若提供則為 2-100 個字元
- `phone`: 選填，若提供則為有效的電話格式
- `address`: 選填，若提供則所有欄位皆必填
- `email`: 無法更新（使用另外的 endpoint）

**成功回應** (200 OK):

```json
{
  "data": {
    "id": "cust-123",
    "name": "John Doe Updated",
    "email": "john@example.com",
    "phone": "+886987654321",
    "address": {
      "street": "456 New St",
      "city": "Taipei",
      "state": "Taiwan",
      "postalCode": "10002",
      "country": "TW"
    },
    "membershipLevel": "PREMIUM",
    "updatedAt": "2025-10-25T12:00:00Z"
  }
}
```

**錯誤回應**:

- `400 Bad Request`: 驗證錯誤
- `401 Unauthorized`: 缺少或無效的 token
- `403 Forbidden`: 權限不足
- `404 Not Found`: 找不到客戶

**curl 範例**:

```bash
curl -X PUT https://api.ecommerce.com/api/v1/customers/cust-123 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe Updated",
    "phone": "+886987654321",
    "address": {
      "street": "456 New St",
      "city": "Taipei",
      "state": "Taiwan",
      "postalCode": "10002",
      "country": "TW"
    }
  }'
```

---

### Partial Update Customer

部分更新客戶資訊（僅更新指定欄位）。

**Endpoint**: `PATCH /api/v1/customers/{id}`

**Authentication**: 必填

**Authorization**: 使用者可更新自己的資料，或需要 ADMIN 角色

**Path Parameters**:

- `id`: Customer ID

**Request Body** (所有欄位皆為選填):

```json
{
  "name": "John Doe Updated",
  "phone": "+886987654321"
}
```

**成功回應** (200 OK):

```json
{
  "data": {
    "id": "cust-123",
    "name": "John Doe Updated",
    "email": "john@example.com",
    "phone": "+886987654321",
    "address": {
      "street": "123 Main St",
      "city": "Taipei",
      "state": "Taiwan",
      "postalCode": "10001",
      "country": "TW"
    },
    "membershipLevel": "PREMIUM",
    "updatedAt": "2025-10-25T12:30:00Z"
  }
}
```

**錯誤回應**:

- `400 Bad Request`: 驗證錯誤
- `401 Unauthorized`: 缺少或無效的 token
- `403 Forbidden`: 權限不足
- `404 Not Found`: 找不到客戶

**curl 範例**:

```bash
curl -X PATCH https://api.ecommerce.com/api/v1/customers/cust-123 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "+886987654321"
  }'
```

---

### Delete Customer

刪除客戶帳戶。

**Endpoint**: `DELETE /api/v1/customers/{id}`

**Authentication**: 必填

**Authorization**: 使用者可刪除自己的帳戶，或需要 ADMIN 角色

**Path Parameters**:

- `id`: Customer ID

**成功回應** (204 No Content)

**錯誤回應**:

- `401 Unauthorized`: 缺少或無效的 token
- `403 Forbidden`: 權限不足
- `404 Not Found`: 找不到客戶
- `409 Conflict`: 無法刪除有活躍訂單的客戶

**curl 範例**:

```bash
curl -X DELETE https://api.ecommerce.com/api/v1/customers/cust-123 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Update Customer Email

更新客戶 email 地址（需要驗證）。

**Endpoint**: `POST /api/v1/customers/{id}/email`

**Authentication**: 必填

**Authorization**: 使用者可更新自己的 email，或需要 ADMIN 角色

**Path Parameters**:

- `id`: Customer ID

**Request Body**:

```json
{
  "newEmail": "newemail@example.com",
  "password": "CurrentPassword123!"
}
```

**驗證規則**:

- `newEmail`: 必填，有效的 email 格式，唯一
- `password`: 必填，用於驗證

**成功回應** (200 OK):

```json
{
  "data": {
    "message": "Verification email sent to newemail@example.com",
    "verificationRequired": true
  }
}
```

**錯誤回應**:

- `400 Bad Request`: 驗證錯誤
- `401 Unauthorized`: 無效的密碼
- `409 Conflict`: Email 已被使用

**curl 範例**:

```bash
curl -X POST https://api.ecommerce.com/api/v1/customers/cust-123/email \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "newEmail": "newemail@example.com",
    "password": "CurrentPassword123!"
  }'
```

---

### Get Customer Orders

檢索特定客戶的訂單。

**Endpoint**: `GET /api/v1/customers/{id}/orders`

**Authentication**: 必填

**Authorization**: 使用者可存取自己的訂單，或需要 ADMIN 角色

**Path Parameters**:

- `id`: Customer ID

**Query Parameters**:

- `page`: 頁碼（預設值：0）
- `size`: 每頁大小（預設值：20）
- `status`: 依訂單狀態篩選（PENDING、CONFIRMED、SHIPPED、DELIVERED、CANCELLED）

**成功回應** (200 OK):

```json
{
  "data": {
    "content": [
      {
        "id": "order-456",
        "orderNumber": "ORD-2025-001",
        "status": "DELIVERED",
        "totalAmount": 1500.00,
        "currency": "TWD",
        "itemCount": 3,
        "createdAt": "2025-10-20T10:00:00Z",
        "deliveredAt": "2025-10-23T14:30:00Z"
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

**curl 範例**:

```bash
curl -X GET "https://api.ecommerce.com/api/v1/customers/cust-123/orders?status=DELIVERED" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Update Customer Preferences

更新客戶偏好和設定。

**Endpoint**: `PATCH /api/v1/customers/{id}/preferences`

**Authentication**: 必填

**Authorization**: 使用者可更新自己的偏好設定

**Path Parameters**:

- `id`: Customer ID

**Request Body**:

```json
{
  "newsletter": true,
  "notifications": true,
  "language": "zh-TW",
  "currency": "TWD"
}
```

**成功回應** (200 OK):

```json
{
  "data": {
    "preferences": {
      "newsletter": true,
      "notifications": true,
      "language": "zh-TW",
      "currency": "TWD"
    },
    "updatedAt": "2025-10-25T13:00:00Z"
  }
}
```

**curl 範例**:

```bash
curl -X PATCH https://api.ecommerce.com/api/v1/customers/cust-123/preferences \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "newsletter": true,
    "language": "zh-TW"
  }'
```

---

## Data Models

### Customer Object

```json
{
  "id": "string",
  "name": "string",
  "email": "string",
  "phone": "string",
  "address": {
    "street": "string",
    "city": "string",
    "state": "string",
    "postalCode": "string",
    "country": "string"
  },
  "membershipLevel": "STANDARD | PREMIUM | VIP",
  "status": "ACTIVE | INACTIVE | SUSPENDED",
  "loyaltyPoints": "number",
  "preferences": {
    "newsletter": "boolean",
    "notifications": "boolean",
    "language": "string",
    "currency": "string"
  },
  "createdAt": "string (ISO 8601)",
  "updatedAt": "string (ISO 8601)"
}
```

### Membership Levels

| 等級 | 描述 | 權益 |
|-------|-------------|----------|
| STANDARD | 預設會員 | 基本功能 |
| PREMIUM | 升級會員 | 5% 折扣、優先客服 |
| VIP | 頂級會員 | 10% 折扣、免運費、專屬優惠 |

## 業務規則

1. **Email 唯一性**：每個 email 只能註冊一次
2. **會員升級**：根據購買記錄自動升級
3. **點數累積**：每消費 $10 獲得 1 點
4. **帳戶刪除**：無法刪除有活躍訂單的帳戶
5. **Email 變更**：需透過 email 驗證
6. **密碼變更**：需要當前密碼驗證

## 錯誤代碼

| 代碼 | 描述 | 解決方案 |
|------|-------------|----------|
| `CUSTOMER_EMAIL_EXISTS` | Email 已註冊 | 使用不同 email 或登入 |
| `CUSTOMER_NOT_FOUND` | 找不到 Customer ID | 檢查客戶 ID |
| `CUSTOMER_INVALID_PASSWORD` | 密碼驗證失敗 | 檢查當前密碼 |
| `CUSTOMER_HAS_ACTIVE_ORDERS` | 無法刪除有活躍訂單的客戶 | 先取消訂單 |
| `CUSTOMER_EMAIL_VERIFICATION_REQUIRED` | Email 未驗證 | 檢查 email 中的驗證連結 |

## 相關文件

- [Authentication](../authentication.md) - 身份驗證和授權
- [Order API](orders.md) - 訂單管理
- [Error Handling](../error-handling.md) - 錯誤代碼和疑難排解

---

**最後更新**: 2025-10-25
**API Version**: v1
