# Notification API

## 概述

Notification API 提供了管理系統通知的端點，包括電子郵件、簡訊和應用程式內通知。此 API 處理通知偏好設定、傳送狀態和通知歷史。

**Base Path**: `/api/v1/notifications`

**Authentication**: 所有端點皆需要驗證

## Endpoints

### Get User Notifications

取得已驗證使用者的通知。

**Endpoint**: `GET /api/v1/notifications/me`

**Authentication**: 需要

**Query Parameters**:

- `page`: 頁碼（預設：0）
- `size`: 每頁大小（預設：20）
- `type`: 依類型篩選（EMAIL, SMS, IN_APP, PUSH）
- `status`: 依狀態篩選（UNREAD, READ, ARCHIVED）
- `category`: 依分類篩選（ORDER, PROMOTION, SYSTEM, ACCOUNT）

**Success Response** (200 OK):

```json
{
  "data": {
    "content": [
      {
        "id": "notif-123",
        "type": "IN_APP",
        "category": "ORDER",
        "title": "Order Shipped",
        "message": "Your order #ORD-2025-001 has been shipped",
        "status": "UNREAD",
        "priority": "NORMAL",
        "data": {
          "orderId": "order-456",
          "trackingNumber": "TW1234567890"
        },
        "actionUrl": "/orders/order-456",
        "createdAt": "2025-10-25T14:00:00Z",
        "readAt": null
      },
      {
        "id": "notif-124",
        "type": "EMAIL",
        "category": "PROMOTION",
        "title": "Special Offer: 20% Off",
        "message": "Get 20% off on all electronics this weekend",
        "status": "READ",
        "priority": "LOW",
        "createdAt": "2025-10-24T10:00:00Z",
        "readAt": "2025-10-24T11:30:00Z"
      }
    ],
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 45,
      "totalPages": 3
    },
    "unreadCount": 12
  }
}
```

**curl Example**:

```bash
curl -X GET "https://api.ecommerce.com/api/v1/notifications/me?status=UNREAD" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Get Notification by ID

取得特定通知。

**Endpoint**: `GET /api/v1/notifications/{id}`

**Authentication**: 需要

**Authorization**: 使用者僅能存取自己的通知

**Path Parameters**:

- `id`: Notification ID

**Success Response** (200 OK):

```json
{
  "data": {
    "id": "notif-123",
    "type": "IN_APP",
    "category": "ORDER",
    "title": "Order Shipped",
    "message": "Your order #ORD-2025-001 has been shipped. Track your package using tracking number TW1234567890.",
    "status": "UNREAD",
    "priority": "NORMAL",
    "data": {
      "orderId": "order-456",
      "trackingNumber": "TW1234567890",
      "carrier": "CHUNGHWA_POST",
      "estimatedDelivery": "2025-10-28T00:00:00Z"
    },
    "actionUrl": "/orders/order-456",
    "actionLabel": "Track Order",
    "createdAt": "2025-10-25T14:00:00Z",
    "readAt": null,
    "expiresAt": "2025-11-25T14:00:00Z"
  }
}
```

**Error Responses**:

- `403 Forbidden`: 無法存取其他使用者的通知
- `404 Not Found`: 找不到通知

**curl Example**:

```bash
curl -X GET https://api.ecommerce.com/api/v1/notifications/notif-123 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Mark Notification as Read

將通知標記為已讀。

**Endpoint**: `PATCH /api/v1/notifications/{id}/read`

**Authentication**: 需要

**Path Parameters**:

- `id`: Notification ID

**Success Response** (200 OK):

```json
{
  "data": {
    "id": "notif-123",
    "status": "READ",
    "readAt": "2025-10-25T15:00:00Z"
  }
}
```

**curl Example**:

```bash
curl -X PATCH https://api.ecommerce.com/api/v1/notifications/notif-123/read \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Mark All Notifications as Read

將已驗證使用者的所有通知標記為已讀。

**Endpoint**: `POST /api/v1/notifications/me/read-all`

**Authentication**: 需要

**Success Response** (200 OK):

```json
{
  "data": {
    "markedCount": 12,
    "message": "All notifications marked as read"
  }
}
```

**curl Example**:

```bash
curl -X POST https://api.ecommerce.com/api/v1/notifications/me/read-all \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Delete Notification

刪除通知（軟刪除/封存）。

**Endpoint**: `DELETE /api/v1/notifications/{id}`

**Authentication**: 需要

**Path Parameters**:

- `id`: Notification ID

**Success Response** (204 No Content)

**curl Example**:

```bash
curl -X DELETE https://api.ecommerce.com/api/v1/notifications/notif-123 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Get Unread Count

取得未讀通知的數量。

**Endpoint**: `GET /api/v1/notifications/me/unread-count`

**Authentication**: 需要

**Success Response** (200 OK):

```json
{
  "data": {
    "total": 12,
    "byCategory": {
      "ORDER": 5,
      "PROMOTION": 3,
      "SYSTEM": 2,
      "ACCOUNT": 2
    }
  }
}
```

**curl Example**:

```bash
curl -X GET https://api.ecommerce.com/api/v1/notifications/me/unread-count \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Get Notification Preferences

取得已驗證使用者的通知偏好設定。

**Endpoint**: `GET /api/v1/notifications/me/preferences`

**Authentication**: 需要

**Success Response** (200 OK):

```json
{
  "data": {
    "email": {
      "enabled": true,
      "categories": {
        "ORDER": true,
        "PROMOTION": true,
        "SYSTEM": true,
        "ACCOUNT": true
      }
    },
    "sms": {
      "enabled": true,
      "categories": {
        "ORDER": true,
        "PROMOTION": false,
        "SYSTEM": true,
        "ACCOUNT": true
      }
    },
    "inApp": {
      "enabled": true,
      "categories": {
        "ORDER": true,
        "PROMOTION": true,
        "SYSTEM": true,
        "ACCOUNT": true
      }
    },
    "push": {
      "enabled": false,
      "categories": {
        "ORDER": false,
        "PROMOTION": false,
        "SYSTEM": false,
        "ACCOUNT": false
      }
    },
    "quietHours": {
      "enabled": true,
      "startTime": "22:00",
      "endTime": "08:00",
      "timezone": "Asia/Taipei"
    }
  }
}
```

**curl Example**:

```bash
curl -X GET https://api.ecommerce.com/api/v1/notifications/me/preferences \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Update Notification Preferences

更新通知偏好設定。

**Endpoint**: `PUT /api/v1/notifications/me/preferences`

**Authentication**: 需要

**Request Body**:

```json
{
  "email": {
    "enabled": true,
    "categories": {
      "ORDER": true,
      "PROMOTION": false,
      "SYSTEM": true,
      "ACCOUNT": true
    }
  },
  "sms": {
    "enabled": true,
    "categories": {
      "ORDER": true,
      "PROMOTION": false,
      "SYSTEM": true,
      "ACCOUNT": true
    }
  },
  "quietHours": {
    "enabled": true,
    "startTime": "23:00",
    "endTime": "07:00",
    "timezone": "Asia/Taipei"
  }
}
```

**Success Response** (200 OK):

```json
{
  "data": {
    "email": {
      "enabled": true,
      "categories": {
        "ORDER": true,
        "PROMOTION": false,
        "SYSTEM": true,
        "ACCOUNT": true
      }
    },
    "sms": {
      "enabled": true,
      "categories": {
        "ORDER": true,
        "PROMOTION": false,
        "SYSTEM": true,
        "ACCOUNT": true
      }
    },
    "quietHours": {
      "enabled": true,
      "startTime": "23:00",
      "endTime": "07:00",
      "timezone": "Asia/Taipei"
    },
    "updatedAt": "2025-10-25T16:00:00Z"
  }
}
```

**curl Example**:

```bash
curl -X PUT https://api.ecommerce.com/api/v1/notifications/me/preferences \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "email": {
      "enabled": true,
      "categories": {
        "PROMOTION": false
      }
    }
  }'
```

---

### Send Notification (Admin)

傳送通知給使用者（僅限管理員）。

**Endpoint**: `POST /api/v1/notifications/send`

**Authentication**: 需要

**Authorization**: 需要 ADMIN 或 MARKETING 角色

**Request Body**:

```json
{
  "recipients": {
    "type": "ALL_USERS",
    "filters": {
      "membershipLevel": ["PREMIUM", "VIP"],
      "registeredAfter": "2025-01-01T00:00:00Z"
    }
  },
  "notification": {
    "type": "EMAIL",
    "category": "PROMOTION",
    "title": "Exclusive Offer for Premium Members",
    "message": "Get 30% off on your next purchase",
    "priority": "NORMAL",
    "actionUrl": "/promotions/premium-offer",
    "actionLabel": "Shop Now",
    "expiresAt": "2025-11-01T00:00:00Z"
  },
  "schedule": {
    "sendAt": "2025-10-26T10:00:00Z",
    "timezone": "Asia/Taipei"
  }
}
```

**Recipient Types**:

- `ALL_USERS`: 所有註冊的使用者
- `SPECIFIC_USERS`: 特定使用者 ID 清單
- `SEGMENT`: 基於篩選條件的使用者區段

**Success Response** (202 Accepted):

```json
{
  "data": {
    "campaignId": "camp-123",
    "status": "SCHEDULED",
    "estimatedRecipients": 1250,
    "scheduledAt": "2025-10-26T10:00:00Z",
    "createdAt": "2025-10-25T16:30:00Z"
  }
}
```

**Error Responses**:

- `400 Bad Request`: 驗證錯誤
- `403 Forbidden`: 權限不足

**curl Example**:

```bash
curl -X POST https://api.ecommerce.com/api/v1/notifications/send \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "recipients": {
      "type": "ALL_USERS"
    },
    "notification": {
      "type": "EMAIL",
      "category": "PROMOTION",
      "title": "Special Offer",
      "message": "Get 20% off today"
    }
  }'
```

---

### Get Notification Campaign Status (Admin)

取得通知活動的狀態。

**Endpoint**: `GET /api/v1/notifications/campaigns/{campaignId}`

**Authentication**: 需要

**Authorization**: 需要 ADMIN 或 MARKETING 角色

**Path Parameters**:

- `campaignId`: Campaign ID

**Success Response** (200 OK):

```json
{
  "data": {
    "campaignId": "camp-123",
    "status": "COMPLETED",
    "notification": {
      "type": "EMAIL",
      "category": "PROMOTION",
      "title": "Exclusive Offer for Premium Members"
    },
    "statistics": {
      "totalRecipients": 1250,
      "sent": 1245,
      "delivered": 1230,
      "failed": 15,
      "opened": 850,
      "clicked": 320,
      "unsubscribed": 5
    },
    "scheduledAt": "2025-10-26T10:00:00Z",
    "startedAt": "2025-10-26T10:00:05Z",
    "completedAt": "2025-10-26T10:15:30Z"
  }
}
```

**curl Example**:

```bash
curl -X GET https://api.ecommerce.com/api/v1/notifications/campaigns/camp-123 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### Get Notification Templates (Admin)

取得可用的通知範本。

**Endpoint**: `GET /api/v1/notifications/templates`

**Authentication**: 需要

**Authorization**: 需要 ADMIN 或 MARKETING 角色

**Query Parameters**:

- `type`: 依類型篩選（EMAIL, SMS）
- `category`: 依分類篩選

**Success Response** (200 OK):

```json
{
  "data": [
    {
      "id": "tmpl-order-confirmation",
      "name": "Order Confirmation",
      "type": "EMAIL",
      "category": "ORDER",
      "subject": "Order Confirmation - {{orderNumber}}",
      "body": "Thank you for your order {{orderNumber}}...",
      "variables": ["orderNumber", "customerName", "totalAmount"],
      "active": true
    },
    {
      "id": "tmpl-shipping-notification",
      "name": "Shipping Notification",
      "type": "EMAIL",
      "category": "ORDER",
      "subject": "Your order has been shipped",
      "body": "Your order {{orderNumber}} has been shipped...",
      "variables": ["orderNumber", "trackingNumber", "carrier"],
      "active": true
    }
  ]
}
```

**curl Example**:

```bash
curl -X GET "https://api.ecommerce.com/api/v1/notifications/templates?category=ORDER" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## Data Models

### Notification Object

```json
{
  "id": "string",
  "userId": "string",
  "type": "EMAIL | SMS | IN_APP | PUSH",
  "category": "ORDER | PROMOTION | SYSTEM | ACCOUNT",
  "title": "string",
  "message": "string",
  "status": "UNREAD | READ | ARCHIVED",
  "priority": "HIGH | NORMAL | LOW",
  "data": "object",
  "actionUrl": "string",
  "actionLabel": "string",
  "createdAt": "string (ISO 8601)",
  "readAt": "string (ISO 8601)",
  "expiresAt": "string (ISO 8601)"
}
```

### Notification Types

| Type | Description | Delivery Method |
|------|-------------|-----------------|
| EMAIL | 電子郵件通知 | SMTP |
| SMS | 簡訊通知 | SMS Gateway |
| IN_APP | 應用程式內通知 | WebSocket/Polling |
| PUSH | 推播通知 | FCM/APNS |

### Notification Categories

| Category | Description | Examples |
|----------|-------------|----------|
| ORDER | 訂單相關 | 訂單確認、出貨更新 |
| PROMOTION | 行銷推廣 | 特別優惠、折扣 |
| SYSTEM | 系統訊息 | 維護、更新 |
| ACCOUNT | 帳戶相關 | 密碼重設、個人資料更新 |

## Business Rules

1. **Quiet Hours**: 通知會尊重使用者的勿擾時段設定
2. **Preference Override**: 重要通知（例如安全性）會忽略偏好設定
3. **Expiration**: 通知在 30 天後過期
4. **Delivery Retry**: 傳送失敗時最多重試 3 次
5. **Rate Limiting**: 每位使用者每小時最多 10 則通知
6. **Unsubscribe**: 使用者可以取消訂閱促銷通知
7. **Read Status**: 應用程式內通知在檢視時標記為已讀

## Error Codes

| Code | Description | Solution |
|------|-------------|----------|
| `NOTIFICATION_NOT_FOUND` | 找不到通知 | 檢查通知 ID |
| `NOTIFICATION_ACCESS_DENIED` | 無法存取通知 | 檢查擁有權 |
| `NOTIFICATION_RATE_LIMIT_EXCEEDED` | 通知過多 | 等待後再傳送更多 |
| `NOTIFICATION_INVALID_RECIPIENT` | 收件者無效 | 檢查使用者 ID 或電子郵件 |
| `NOTIFICATION_TEMPLATE_NOT_FOUND` | 找不到範本 | 檢查範本 ID |
| `NOTIFICATION_DELIVERY_FAILED` | 傳送失敗 | 檢查傳送設定 |

## Webhook Events

系統可以針對通知狀態更新傳送 webhook 事件：

```json
{
  "event": "notification.delivered",
  "timestamp": "2025-10-25T14:00:00Z",
  "data": {
    "notificationId": "notif-123",
    "userId": "cust-123",
    "type": "EMAIL",
    "status": "DELIVERED"
  }
}
```

**Event Types**:

- `notification.sent`: 通知已傳送
- `notification.delivered`: 通知已送達
- `notification.failed`: 傳送失敗
- `notification.opened`: 電子郵件/推播已開啟
- `notification.clicked`: 動作已點擊

## Related Documentation

- [Customer API](customers.md) - 顧客偏好設定
- [Order API](orders.md) - 訂單通知
- [Logistics API](logistics.md) - 出貨通知

---

**Last Updated**: 2025-10-25
**API Version**: v1
