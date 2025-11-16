# {資源名稱} API

> **基礎 URL**: `https://api.example.com/api/v1`
> **最後更新**: YYYY-MM-DD
> **API 版本**: v1

## 概述

[簡要描述此 API 資源的功能及其在系統中的用途]

## 驗證

除非另有說明，否則所有端點都需要驗證。

```http
Authorization: Bearer {access_token}
```

詳細資訊，請參閱[驗證指南](../authentication.md)以了解如何取得存取權杖。

## 端點

---

### 建立 {資源}

在系統中建立新的{資源}。

#### 要求

**方法**: `POST`

**端點**: `/api/v1/{resources}`

**標題**:

```http
Content-Type: application/json
Authorization: Bearer {access_token}
```

**要求主體**:

```json
{
  "field1": "string",
  "field2": "number",
  "field3": {
    "nestedField1": "string",
    "nestedField2": "boolean"
  },
  "field4": ["array", "of", "strings"]
}
```

**欄位描述**:

| 欄位 | 類型 | 必需 | 描述 | 約束 |
|-------|------|----------|-------------|-------------|
| field1 | string | 是 | field1 描述 | 最小: 2、最大: 100 個字元 |
| field2 | number | 是 | field2 描述 | 最小: 0、最大: 1000 |
| field3 | object | 否 | field3 描述 | - |
| field3.nestedField1 | string | 是 | 描述 | - |
| field3.nestedField2 | boolean | 否 | 描述 | 預設值: false |
| field4 | array | 否 | 描述 | 最多項: 10 |

#### 回應

**成功回應** (201 已建立):

```http
HTTP/1.1 201 Created
Content-Type: application/json
Location: /api/v1/{resources}/{id}
```

```json
{
  "id": "resource-123",
  "field1": "string",
  "field2": 42,
  "field3": {
    "nestedField1": "string",
    "nestedField2": true
  },
  "field4": ["array", "of", "strings"],
  "createdAt": "2025-01-17T10:00:00Z",
  "updatedAt": "2025-01-17T10:00:00Z",
  "status": "active"
}
```

**回應欄位**:

| 欄位 | 類型 | 描述 |
|-------|------|-------------|
| id | string | 資源的唯一識別碼 |
| field1 | string | 描述 |
| field2 | number | 描述 |
| createdAt | string (ISO 8601) | 建立資源時的時間戳 |
| updatedAt | string (ISO 8601) | 最後更新資源時的時間戳 |
| status | string | 資源的目前狀態 |

#### 錯誤回應

**400 不良要求** - 無效輸入:

```json
{
  "errorCode": "INVALID_INPUT",
  "message": "驗證失敗",
  "timestamp": "2025-01-17T10:00:00Z",
  "path": "/api/v1/{resources}",
  "fieldErrors": [
    {
      "field": "field1",
      "message": "Field1 必須介於 2 到 100 個字元",
      "rejectedValue": "a"
    }
  ]
}
```

**401 未授權** - 遺漏或無效的驗證:

```json
{
  "errorCode": "UNAUTHORIZED",
  "message": "需要驗證",
  "timestamp": "2025-01-17T10:00:00Z",
  "path": "/api/v1/{resources}"
}
```

**409 衝突** - 資源已存在:

```json
{
  "errorCode": "RESOURCE_EXISTS",
  "message": "具有此識別碼的資源已存在",
  "timestamp": "2025-01-17T10:00:00Z",
  "path": "/api/v1/{resources}",
  "context": {
    "existingResourceId": "resource-123"
  }
}
```

**429 要求過多** - 超過速率限制:

```json
{
  "errorCode": "RATE_LIMIT_EXCEEDED",
  "message": "要求過多。請稍後再試。",
  "timestamp": "2025-01-17T10:00:00Z",
  "path": "/api/v1/{resources}",
  "retryAfter": 60
}
```

**500 內部伺服器錯誤** - 伺服器錯誤:

```json
{
  "errorCode": "INTERNAL_ERROR",
  "message": "發生未預期的錯誤",
  "timestamp": "2025-01-17T10:00:00Z",
  "path": "/api/v1/{resources}"
}
```

#### 範例

**cURL**:

```bash
curl -X POST https://api.example.com/api/v1/{resources} \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "field1": "example value",
    "field2": 42,
    "field3": {
      "nestedField1": "nested value",
      "nestedField2": true
    }
  }'
```

**JavaScript (fetch)**:

```javascript
const response = await fetch('https://api.example.com/api/v1/{resources}', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${accessToken}`
  },
  body: JSON.stringify({
    field1: 'example value',
    field2: 42,
    field3: {
      nestedField1: 'nested value',
      nestedField2: true
    }
  })
});

const data = await response.json();
console.log(data);
```

**Java (Spring RestTemplate)**:

```java
RestTemplate restTemplate = new RestTemplate();
HttpHeaders headers = new HttpHeaders();
headers.setContentType(MediaType.APPLICATION_JSON);
headers.setBearerAuth(accessToken);

CreateResourceRequest request = new CreateResourceRequest(
    "example value",
    42,
    new NestedField("nested value", true)
);

HttpEntity<CreateResourceRequest> entity = new HttpEntity<>(request, headers);
ResponseEntity<ResourceResponse> response = restTemplate.postForEntity(
    "https://api.example.com/api/v1/{resources}",
    entity,
    ResourceResponse.class
);
```

**Python (requests)**:

```python
import requests

url = "https://api.example.com/api/v1/{resources}"
headers = {
    "Content-Type": "application/json",
    "Authorization": f"Bearer {access_token}"
}
payload = {
    "field1": "example value",
    "field2": 42,
    "field3": {
        "nestedField1": "nested value",
        "nestedField2": True
    }
}

response = requests.post(url, json=payload, headers=headers)
data = response.json()
print(data)
```

---

### 取得 {資源}

按 ID 取得特定的{資源}。

#### 要求

**方法**: `GET`

**端點**: `/api/v1/{resources}/{id}`

**標題**:

```http
Authorization: Bearer {access_token}
```

**路徑參數**:

| 參數 | 類型 | 必需 | 描述 |
|-----------|------|----------|-------------|
| id | string | 是 | 資源的唯一識別碼 |

#### 回應

**成功回應** (200 OK):

```http
HTTP/1.1 200 OK
Content-Type: application/json
```

```json
{
  "id": "resource-123",
  "field1": "string",
  "field2": 42,
  "createdAt": "2025-01-17T10:00:00Z",
  "updatedAt": "2025-01-17T10:00:00Z"
}
```

#### 錯誤回應

**404 找不到** - 資源找不到:

```json
{
  "errorCode": "RESOURCE_NOT_FOUND",
  "message": "找不到 ID 為 'resource-123' 的資源",
  "timestamp": "2025-01-17T10:00:00Z",
  "path": "/api/v1/{resources}/resource-123"
}
```

#### 範例

**cURL**:

```bash
curl -X GET https://api.example.com/api/v1/{resources}/resource-123 \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

---

### 列出 {資源}

取得分頁的{資源}清單。

#### 要求

**方法**: `GET`

**端點**: `/api/v1/{resources}`

**標題**:

```http
Authorization: Bearer {access_token}
```

**查詢參數**:

| 參數 | 類型 | 必需 | 描述 | 預設值 |
|-----------|------|----------|-------------|---------|
| page | number | 否 | 頁碼（從 0 開始） | 0 |
| size | number | 否 | 每頁項數 | 20 |
| sort | string | 否 | 排序欄位和方向（例如 "createdAt,desc"） | "createdAt,desc" |
| filter | string | 否 | 篩選條件 | - |

#### 回應

**成功回應** (200 OK):

```json
{
  "content": [
    {
      "id": "resource-123",
      "field1": "string",
      "field2": 42
    }
  ],
  "page": {
    "size": 20,
    "number": 0,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

#### 範例

**cURL**:

```bash
curl -X GET "https://api.example.com/api/v1/{resources}?page=0&size=20&sort=createdAt,desc" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

---

### 更新 {資源}

更新現有的{資源}。

#### 要求

**方法**: `PUT`

**端點**: `/api/v1/{resources}/{id}`

**標題**:

```http
Content-Type: application/json
Authorization: Bearer {access_token}
```

**要求主體**: [與建立相同]

#### 回應

**成功回應** (200 OK): [與取得相同結構]

#### 錯誤回應

[與建立相同，加上 404 找不到]

---

### 部分更新 {資源}

部分更新現有的{資源}。

#### 要求

**方法**: `PATCH`

**端點**: `/api/v1/{resources}/{id}`

**標題**:

```http
Content-Type: application/json
Authorization: Bearer {access_token}
```

**要求主體**:

```json
{
  "field1": "updated value"
}
```

#### 回應

**成功回應** (200 OK): [與取得相同結構]

---

### 刪除 {資源}

刪除一個{資源}。

#### 要求

**方法**: `DELETE`

**端點**: `/api/v1/{resources}/{id}`

**標題**:

```http
Authorization: Bearer {access_token}
```

#### 回應

**成功回應** (204 無內容):

```http
HTTP/1.1 204 No Content
```

#### 錯誤回應

**404 找不到**: 資源找不到

**409 衝突**: 無法刪除資源（例如有依賴項）

---

## 速率限制

- **速率限制**: 每位使用者每小時 1000 個要求
- **標題**:
  - `X-RateLimit-Limit`: 允許的總要求數
  - `X-RateLimit-Remaining`: 剩餘要求
  - `X-RateLimit-Reset`: 限制重設的時間（Unix 時間戳）

## 分頁

所有清單端點都支援分頁：

- 預設頁面大小: 20
- 最大頁面大小: 100
- 頁碼從 0 開始

## 篩選和排序

### 排序

使用 `sort` 參數: `sort=field,direction`

- 範例: `sort=createdAt,desc`
- 多個排序: `sort=field1,asc&sort=field2,desc`

### 篩選

使用 `filter` 參數，格式: `field:operator:value`

- 運算子: `eq`、`ne`、`gt`、`lt`、`gte`、`lte`、`like`、`in`
- 範例: `filter=status:eq:active`
- 多個篩選: `filter=status:eq:active&filter=createdAt:gt:2025-01-01`

## 版本控制

此 API 使用 URL 版本控制。目前版本是 `v1`。

當引入重大變更時，將發行新版本（例如 `v2`）。

## 相關文件

- [驗證指南](../authentication.md)
- [錯誤處理](../error-handling.md)
- [速率限制](../rate-limiting.md)
- [API 版本控制](../versioning.md)

## 變更日誌

| 日期 | 版本 | 變更 |
|------|---------|---------|
| YYYY-MM-DD | 1.0 | 初始 API 版本 |

---

**API 模板版本**: 1.0
**最後模板更新**: 2025-01-17
