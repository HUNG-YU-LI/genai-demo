# 錯誤處理

## 概述

企業級電子商務平台 API 使用標準 HTTP 狀態碼,並以一致的格式提供詳細的錯誤資訊。本文件描述錯誤回應格式、錯誤代碼和疑難排解指南。

## 錯誤回應格式

### 標準錯誤回應

所有錯誤回應遵循此一致的結構:

```json
{
  "errors": [
    {
      "code": "ERROR_CODE",
      "message": "人類可讀的錯誤訊息",
      "field": "fieldName",
      "rejectedValue": "invalid-value"
    }
  ],
  "metadata": {
    "requestId": "req-abc-123",
    "timestamp": "2025-10-25T10:30:00Z",
    "version": "v1"
  }
}
```

**欄位**:

- `errors`: 錯誤物件陣列
- `code`: 機器可讀的錯誤代碼
- `message`: 人類可讀的錯誤說明
- `field`: 欄位名稱 (用於驗證錯誤)
- `rejectedValue`: 被拒絕的無效值
- `metadata.requestId`: 唯一 request 識別碼用於追蹤
- `metadata.timestamp`: 錯誤發生時間戳
- `metadata.version`: API 版本

### 多個錯誤

當發生多個驗證錯誤時,會回傳所有錯誤:

```json
{
  "errors": [
    {
      "code": "VALIDATION_ERROR",
      "message": "Email format is invalid",
      "field": "email",
      "rejectedValue": "invalid-email"
    },
    {
      "code": "VALIDATION_ERROR",
      "message": "Password must be at least 8 characters",
      "field": "password",
      "rejectedValue": "short"
    }
  ],
  "metadata": {
    "requestId": "req-abc-123",
    "timestamp": "2025-10-25T10:30:00Z",
    "version": "v1"
  }
}
```

## HTTP 狀態碼

### 成功碼 (2xx)

| 代碼 | 狀態 | 描述 | 使用時機 |
|------|------|------|----------|
| 200 | OK | Request 成功 | GET、PUT、PATCH |
| 201 | Created | 資源已建立 | POST |
| 204 | No Content | 成功但無回應內容 | DELETE |

### 客戶端錯誤碼 (4xx)

| 代碼 | 狀態 | 描述 | 使用時機 |
|------|------|------|----------|
| 400 | Bad Request | 無效的 request 格式或驗證錯誤 | 格式錯誤的 JSON、驗證失敗 |
| 401 | Unauthorized | 需要身份驗證或身份驗證失敗 | 缺少/無效的 token |
| 403 | Forbidden | 權限不足 | 授權失敗 |
| 404 | Not Found | 找不到資源 | 無效的資源 ID |
| 405 | Method Not Allowed | 不支援的 HTTP method | 錯誤的 HTTP method |
| 409 | Conflict | 違反業務規則 | 重複資源、狀態衝突 |
| 422 | Unprocessable Entity | 語義驗證錯誤 | 業務邏輯驗證 |
| 429 | Too Many Requests | 超過流量限制 | 太多 request |

### 伺服器錯誤碼 (5xx)

| 代碼 | 狀態 | 描述 | 使用時機 |
|------|------|------|----------|
| 500 | Internal Server Error | 未預期的伺服器錯誤 | 系統故障 |
| 502 | Bad Gateway | 上游服務錯誤 | 外部服務故障 |
| 503 | Service Unavailable | 服務暫時無法使用 | 維護中、過載 |
| 504 | Gateway Timeout | 上游服務逾時 | 外部服務逾時 |

## 錯誤代碼

### 身份驗證錯誤 (AUTH_*)

| 代碼 | HTTP 狀態 | 描述 | 解決方案 |
|------|-----------|------|----------|
| `AUTH_INVALID_CREDENTIALS` | 401 | 無效的 email 或密碼 | 檢查憑證 |
| `AUTH_TOKEN_EXPIRED` | 401 | Access token 過期 | 更新 token |
| `AUTH_TOKEN_INVALID` | 401 | 格式錯誤或無效的 token | 重新驗證 |
| `AUTH_REFRESH_TOKEN_EXPIRED` | 401 | Refresh token 過期 | 重新驗證 |
| `AUTH_REFRESH_TOKEN_INVALID` | 401 | 無效的 refresh token | 重新驗證 |
| `AUTH_ACCOUNT_LOCKED` | 401 | 因失敗嘗試次數過多而鎖定帳號 | 等待或聯絡支援 |
| `AUTH_ACCOUNT_DISABLED` | 401 | 帳號已停用 | 聯絡支援 |
| `AUTH_EMAIL_NOT_VERIFIED` | 401 | Email 尚未驗證 | 驗證 email |

**範例**:

```json
{
  "errors": [
    {
      "code": "AUTH_INVALID_CREDENTIALS",
      "message": "Invalid email or password"
    }
  ]
}
```

### 授權錯誤 (AUTHZ_*)

| 代碼 | HTTP 狀態 | 描述 | 解決方案 |
|------|-----------|------|----------|
| `AUTHZ_INSUFFICIENT_PERMISSIONS` | 403 | 缺少必要權限 | 檢查必要角色 |
| `AUTHZ_RESOURCE_ACCESS_DENIED` | 403 | 無法存取此資源 | 檢查資源擁有權 |
| `AUTHZ_ROLE_REQUIRED` | 403 | 需要特定角色 | 聯絡管理員 |

**範例**:

```json
{
  "errors": [
    {
      "code": "AUTHZ_INSUFFICIENT_PERMISSIONS",
      "message": "Insufficient permissions. Required role: ADMIN"
    }
  ]
}
```

### 驗證錯誤 (VALIDATION_*)

| 代碼 | HTTP 狀態 | 描述 | 解決方案 |
|------|-----------|------|----------|
| `VALIDATION_ERROR` | 400 | 欄位驗證失敗 | 檢查欄位需求 |
| `VALIDATION_REQUIRED_FIELD` | 400 | 缺少必填欄位 | 提供必填欄位 |
| `VALIDATION_INVALID_FORMAT` | 400 | 無效的欄位格式 | 檢查格式需求 |
| `VALIDATION_OUT_OF_RANGE` | 400 | 值超出可接受範圍 | 檢查最小/最大值 |
| `VALIDATION_INVALID_LENGTH` | 400 | 字串長度無效 | 檢查長度需求 |
| `VALIDATION_INVALID_EMAIL` | 400 | 無效的 email 格式 | 提供有效的 email |
| `VALIDATION_INVALID_PHONE` | 400 | 無效的電話格式 | 提供有效的電話 |
| `VALIDATION_INVALID_DATE` | 400 | 無效的日期格式 | 使用 ISO 8601 格式 |

**範例**:

```json
{
  "errors": [
    {
      "code": "VALIDATION_INVALID_EMAIL",
      "message": "Email format is invalid",
      "field": "email",
      "rejectedValue": "invalid-email"
    }
  ]
}
```

### 業務規則錯誤 (BUSINESS_*)

| 代碼 | HTTP 狀態 | 描述 | 解決方案 |
|------|-----------|------|----------|
| `BUSINESS_RULE_VIOLATION` | 409 | 違反業務規則 | 檢查業務約束 |
| `BUSINESS_DUPLICATE_RESOURCE` | 409 | 資源已存在 | 使用現有資源 |
| `BUSINESS_INVALID_STATE` | 409 | 無效的狀態轉換 | 檢查目前狀態 |
| `BUSINESS_INSUFFICIENT_INVENTORY` | 409 | 庫存不足 | 減少數量 |
| `BUSINESS_PAYMENT_FAILED` | 422 | 付款處理失敗 | 檢查付款詳細資訊 |
| `BUSINESS_ORDER_CANNOT_BE_CANCELLED` | 409 | 訂單無法取消 | 檢查訂單狀態 |

**範例**:

```json
{
  "errors": [
    {
      "code": "BUSINESS_INSUFFICIENT_INVENTORY",
      "message": "Insufficient inventory for product. Available: 5, Requested: 10",
      "field": "quantity",
      "rejectedValue": 10
    }
  ]
}
```

### 資源錯誤 (RESOURCE_*)

| 代碼 | HTTP 狀態 | 描述 | 解決方案 |
|------|-----------|------|----------|
| `RESOURCE_NOT_FOUND` | 404 | 找不到資源 | 檢查資源 ID |
| `RESOURCE_ALREADY_EXISTS` | 409 | 資源已存在 | 使用不同的識別碼 |
| `RESOURCE_DELETED` | 410 | 資源已被刪除 | 無法復原 |

**範例**:

```json
{
  "errors": [
    {
      "code": "RESOURCE_NOT_FOUND",
      "message": "Customer with ID 'cust-999' not found"
    }
  ]
}
```

### 流量限制錯誤 (RATE_LIMIT_*)

| 代碼 | HTTP 狀態 | 描述 | 解決方案 |
|------|-----------|------|----------|
| `RATE_LIMIT_EXCEEDED` | 429 | 太多 request | 等待並重試 |
| `RATE_LIMIT_QUOTA_EXCEEDED` | 429 | 超過每日配額 | 等到重置 |

**範例**:

```json
{
  "errors": [
    {
      "code": "RATE_LIMIT_EXCEEDED",
      "message": "Rate limit exceeded. Please try again in 3600 seconds."
    }
  ]
}
```

**回應標頭**:

```http
HTTP/1.1 429 Too Many Requests
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1635793200
Retry-After: 3600
```

### 系統錯誤 (SYSTEM_*)

| 代碼 | HTTP 狀態 | 描述 | 解決方案 |
|------|-----------|------|----------|
| `SYSTEM_INTERNAL_ERROR` | 500 | 未預期的系統錯誤 | 聯絡支援 |
| `SYSTEM_SERVICE_UNAVAILABLE` | 503 | 服務暫時無法使用 | 稍後重試 |
| `SYSTEM_DATABASE_ERROR` | 500 | 資料庫操作失敗 | 聯絡支援 |
| `SYSTEM_EXTERNAL_SERVICE_ERROR` | 502 | 外部服務錯誤 | 重試或聯絡支援 |
| `SYSTEM_TIMEOUT` | 504 | Request 逾時 | 使用較小的 payload 重試 |

**範例**:

```json
{
  "errors": [
    {
      "code": "SYSTEM_INTERNAL_ERROR",
      "message": "An unexpected error occurred. Please contact support with request ID: req-abc-123"
    }
  ],
  "metadata": {
    "requestId": "req-abc-123",
    "timestamp": "2025-10-25T10:30:00Z"
  }
}
```

## 錯誤處理最佳實踐

### 客戶端錯誤處理

**1. 檢查 HTTP 狀態碼**:

```javascript
async function makeRequest() {
  const response = await fetch('/api/v1/customers');

  if (!response.ok) {
    const error = await response.json();
    handleError(response.status, error);
    return;
  }

  return response.json();
}

function handleError(status, error) {
  switch (status) {
    case 400:
      // 驗證錯誤 - 顯示欄位錯誤
      showValidationErrors(error.errors);
      break;
    case 401:
      // 身份驗證錯誤 - 重新導向到登入
      redirectToLogin();
      break;
    case 403:
      // 授權錯誤 - 顯示拒絕存取
      showAccessDenied();
      break;
    case 404:
      // 找不到 - 顯示找不到頁面
      showNotFound();
      break;
    case 429:
      // 流量限制 - 顯示重試訊息
      showRateLimitError(error);
      break;
    case 500:
      // 伺服器錯誤 - 顯示錯誤訊息和 request ID
      showServerError(error.metadata.requestId);
      break;
    default:
      showGenericError();
  }
}
```

**2. 顯示使用者友善的訊息**:

```javascript
function showValidationErrors(errors) {
  errors.forEach(error => {
    const field = document.querySelector(`[name="${error.field}"]`);
    if (field) {
      field.classList.add('error');
      const errorMessage = document.createElement('span');
      errorMessage.className = 'error-message';
      errorMessage.textContent = error.message;
      field.parentNode.appendChild(errorMessage);
    }
  });
}
```

**3. 實作重試邏輯**:

```javascript
async function fetchWithRetry(url, options, maxRetries = 3) {
  for (let i = 0; i < maxRetries; i++) {
    try {
      const response = await fetch(url, options);

      if (response.status === 429) {
        // 流量限制 - 等待並重試
        const retryAfter = response.headers.get('Retry-After') || 60;
        await sleep(retryAfter * 1000);
        continue;
      }

      if (response.status >= 500) {
        // 伺服器錯誤 - 指數退避
        await sleep(Math.pow(2, i) * 1000);
        continue;
      }

      return response;
    } catch (error) {
      if (i === maxRetries - 1) throw error;
      await sleep(Math.pow(2, i) * 1000);
    }
  }
}
```

**4. 記錄錯誤並包含 Request ID**:

```javascript
function logError(error, requestId) {
  console.error('API Error:', {
    requestId,
    code: error.code,
    message: error.message,
    timestamp: new Date().toISOString()
  });

  // 發送到錯誤追蹤服務
  errorTracker.captureException(error, {
    extra: { requestId }
  });
}
```

### 伺服器端錯誤處理

**Java 範例**:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
        ValidationException ex) {

        List<ApiError> errors = ex.getErrors().stream()
            .map(error -> new ApiError(
                "VALIDATION_ERROR",
                error.getMessage(),
                error.getField(),
                error.getRejectedValue()
            ))
            .toList();

        return ResponseEntity
            .badRequest()
            .body(ApiResponse.error(errors));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(
        ResourceNotFoundException ex) {

        ApiError error = new ApiError(
            "RESOURCE_NOT_FOUND",
            ex.getMessage(),
            null,
            null
        );

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(List.of(error)));
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessRule(
        BusinessRuleViolationException ex) {

        ApiError error = new ApiError(
            "BUSINESS_RULE_VIOLATION",
            ex.getMessage(),
            null,
            null
        );

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ApiResponse.error(List.of(error)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(
        Exception ex, HttpServletRequest request) {

        String requestId = request.getHeader("X-Request-ID");

        logger.error("Unexpected error for request {}", requestId, ex);

        ApiError error = new ApiError(
            "SYSTEM_INTERNAL_ERROR",
            "An unexpected error occurred. Request ID: " + requestId,
            null,
            null
        );

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(List.of(error)));
    }
}
```

## 疑難排解指南

### 常見情境

#### 情境 1: 401 Unauthorized

**症狀**:

- API 回傳 401 狀態碼
- 錯誤代碼: `AUTH_TOKEN_EXPIRED` 或 `AUTH_TOKEN_INVALID`

**診斷**:

1. 檢查 Authorization 標頭中是否包含 token
2. 驗證 token 格式: `Bearer <token>`
3. 檢查 token 過期時間
4. 驗證 token 簽章

**解決方案**:

```javascript
// 檢查 token 是否過期
function isTokenExpired(token) {
  const payload = JSON.parse(atob(token.split('.')[1]));
  return payload.exp * 1000 < Date.now();
}

// 如果過期則更新
if (isTokenExpired(accessToken)) {
  await refreshToken();
}
```

#### 情境 2: 400 Bad Request 包含驗證錯誤

**症狀**:

- API 回傳 400 狀態碼
- Response 中有多個驗證錯誤

**診斷**:

1. 檢查所有必填欄位是否已提供
2. 驗證欄位格式 (email、電話、日期)
3. 檢查欄位長度限制
4. 驗證資料類型

**解決方案**:

```javascript
// 發送前驗證
function validateCustomer(customer) {
  const errors = [];

  if (!customer.email || !isValidEmail(customer.email)) {
    errors.push({ field: 'email', message: 'Invalid email format' });
  }

  if (!customer.name || customer.name.length < 2) {
    errors.push({ field: 'name', message: 'Name must be at least 2 characters' });
  }

  return errors;
}
```

#### 情境 3: 409 Conflict - 違反業務規則

**症狀**:

- API 回傳 409 狀態碼
- 錯誤代碼: `BUSINESS_RULE_VIOLATION`

**診斷**:

1. 檢查目前資源狀態
2. 驗證業務規則限制
3. 檢查重複資源

**解決方案**:

- 閱讀錯誤訊息以了解特定限制
- 調整 request 以滿足業務規則
- 在操作前檢查資源狀態

#### 情境 4: 429 Too Many Requests

**症狀**:

- API 回傳 429 狀態碼
- 錯誤代碼: `RATE_LIMIT_EXCEEDED`

**診斷**:

1. 檢查流量限制標頭
2. 計算最近的 request 數量
3. 驗證流量限制層級

**解決方案**:

```javascript
// 實作指數退避
async function retryWithBackoff(fn, maxRetries = 3) {
  for (let i = 0; i < maxRetries; i++) {
    try {
      return await fn();
    } catch (error) {
      if (error.status === 429 && i < maxRetries - 1) {
        const delay = Math.pow(2, i) * 1000;
        await sleep(delay);
        continue;
      }
      throw error;
    }
  }
}
```

#### 情境 5: 500 Internal Server Error

**症狀**:

- API 回傳 500 狀態碼
- 錯誤代碼: `SYSTEM_INTERNAL_ERROR`

**診斷**:

1. 記下錯誤回應中的 request ID
2. 檢查錯誤是否可重現
3. 驗證 request payload

**解決方案**:

- 使用 request ID 聯絡支援
- 一段時間後重試 request
- 檢查 API 狀態頁

### 除錯技巧

**1. 啟用 Request/Response 記錄**:

```javascript
// 記錄所有 API request
fetch = new Proxy(fetch, {
  apply(target, thisArg, args) {
    const [url, options] = args;
    console.log('API Request:', { url, options });

    return Reflect.apply(target, thisArg, args)
      .then(response => {
        console.log('API Response:', {
          url,
          status: response.status,
          headers: Object.fromEntries(response.headers)
        });
        return response;
      });
  }
});
```

**2. 在所有日誌中包含 Request ID**:

```javascript
function generateRequestId() {
  return `req-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
}

async function apiCall(url, options = {}) {
  const requestId = generateRequestId();

  options.headers = {
    ...options.headers,
    'X-Request-ID': requestId
  };

  console.log(`[${requestId}] Request:`, url);

  try {
    const response = await fetch(url, options);
    console.log(`[${requestId}] Response:`, response.status);
    return response;
  } catch (error) {
    console.error(`[${requestId}] Error:`, error);
    throw error;
  }
}
```

**3. 使用瀏覽器開發者工具**:

- Network 標籤: 檢查 request/response
- Console: 檢查 JavaScript 錯誤
- Application 標籤: 驗證 token 儲存

**4. 使用 curl 測試**:

```bash
# 新增詳細輸出
curl -v -X GET https://api.ecommerce.com/api/v1/customers/me \
  -H "Authorization: Bearer $TOKEN"

# 儲存 response 到檔案
curl -X GET https://api.ecommerce.com/api/v1/customers/me \
  -H "Authorization: Bearer $TOKEN" \
  -o response.json

# 僅顯示 HTTP 狀態碼
curl -s -o /dev/null -w "%{http_code}" \
  https://api.ecommerce.com/api/v1/customers/me
```

## 支援

### 取得協助

**1. 查看文件**:

- [API 參考](endpoints/) - Endpoint 文件
- [身份驗證指南](authentication.md) - 身份驗證問題
- [常見問題](#frequently-asked-questions) - 常見問題

**2. 檢查 API 狀態**:

- 狀態頁: <https://status.ecommerce.com>
- 事件歷史: <https://status.ecommerce.com/history>

**3. 聯絡支援**:

- Email: <api-support@ecommerce.com>
- 包含: Request ID、時間戳、錯誤代碼
- 回應時間: 24 小時

### 常見問題

**問: 為什麼我收到 401 Unauthorized?**
答: 檢查您的 token 是否有效且未過期。嘗試更新您的 token。

**問: 如何處理流量限制?**
答: 實作指數退避並遵守 `Retry-After` 標頭。

**問: 收到 500 錯誤該怎麼辦?**
答: 記下 request ID 並聯絡支援。問題出在我們這邊。

**問: 可以重試失敗的 request 嗎?**
答: 可以,但僅限冪等操作 (GET、PUT、DELETE)。使用指數退避。

**問: 錯誤日誌保留多久?**
答: 錯誤日誌保留 30 天用於疑難排解。

## 相關文件

- [API 概述](README.md) - API 設計原則
- [身份驗證](authentication.md) - 身份驗證和授權
- [Customer API](endpoints/customers.md) - Customer endpoint
- [ADR-009: RESTful API 設計](../../architecture/adrs/009-restful-api-design-with-openapi.md) - API 設計決策

---

**最後更新**: 2025-10-25
**API 版本**: v1
