# Authentication

## 概述

企業電子商務平台 API 使用基於 JWT (JSON Web Token) 的身份驗證來保護 API endpoints。本文件描述身份驗證流程、token 格式和最佳實踐。

## 身份驗證流程

### 1. 使用者登入

**Endpoint**: `POST /api/v1/auth/login`

**Request**:

```http
POST /api/v1/auth/login HTTP/1.1
Host: api.ecommerce.com
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePassword123!"
}
```

**Response**:

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwicm9sZXMiOlsiVVNFUiJdLCJpYXQiOjE2MzU3ODk2MDAsImV4cCI6MTYzNTc5MzIwMH0.signature",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwidHlwZSI6InJlZnJlc2giLCJpYXQiOjE2MzU3ODk2MDAsImV4cCI6MTYzNjM5NDQwMH0.signature",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": "user-123",
      "email": "user@example.com",
      "name": "John Doe",
      "roles": ["USER"]
    }
  },
  "metadata": {
    "requestId": "req-abc-123",
    "timestamp": "2025-10-25T10:30:00Z",
    "version": "v1"
  }
}
```

### 2. 使用 Access Token

在所有需要身份驗證的請求中，將 access token 包含在 `Authorization` header 中：

```http
GET /api/v1/customers/me HTTP/1.1
Host: api.ecommerce.com
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

### 3. Token 更新

當 access token 過期時，使用 refresh token 獲取新的 access token：

**Endpoint**: `POST /api/v1/auth/refresh`

**Request**:

```http
POST /api/v1/auth/refresh HTTP/1.1
Host: api.ecommerce.com
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
}
```

**Response**:

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9.new_token...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9.new_refresh_token...",
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
}
```

### 4. 登出

**Endpoint**: `POST /api/v1/auth/logout`

**Request**:

```http
POST /api/v1/auth/logout HTTP/1.1
Host: api.ecommerce.com
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

**Response**:

```http
HTTP/1.1 204 No Content
```

## JWT Token 格式

### Access Token 結構

access token 是一個 JWT，具有以下結構：

**Header**:

```json
{
  "alg": "HS512",
  "typ": "JWT"
}
```

**Payload**:

```json
{
  "sub": "user@example.com",
  "userId": "user-123",
  "roles": ["USER", "CUSTOMER"],
  "permissions": ["read:orders", "write:orders"],
  "iat": 1635789600,
  "exp": 1635793200,
  "jti": "token-unique-id"
}
```

**Claims 說明**:

- `sub`: 主體（使用者 email）
- `userId`: 使用者唯一識別碼
- `roles`: 使用者角色（例如：USER, ADMIN, CUSTOMER）
- `permissions`: 特定權限
- `iat`: 發行時間戳記
- `exp`: 過期時間戳記
- `jti`: JWT ID（唯一 token 識別碼）

### Refresh Token 結構

refresh token 具有較長的過期時間，僅用於獲取新的 access token：

**Payload**:

```json
{
  "sub": "user@example.com",
  "userId": "user-123",
  "type": "refresh",
  "iat": 1635789600,
  "exp": 1636394400,
  "jti": "refresh-token-unique-id"
}
```

### Token 過期時間

| Token 類型 | 過期時間 | 用途 |
|------------|------------|---------|
| Access Token | 1 小時 | API 身份驗證 |
| Refresh Token | 7 天 | Token 更新 |

## 授權

### 基於角色的存取控制 (RBAC)

API 使用基於角色的存取控制來管理權限：

**角色**:

- `USER`: 基本認證使用者
- `CUSTOMER`: 具有購物權限的客戶
- `ADMIN`: 管理員存取權限
- `SELLER`: 賣家/供應商存取權限
- `SUPPORT`: 客戶支援存取權限

**角色層級**:

```text
ADMIN > SELLER > SUPPORT > CUSTOMER > USER
```

### 權限檢查

Endpoints 可能需要特定角色或權限：

**範例 - 僅限管理員**:

```http
GET /api/v1/admin/users
Authorization: Bearer <admin-token>
```

**Response (Forbidden)**:

```http
HTTP/1.1 403 Forbidden

{
  "errors": [
    {
      "code": "FORBIDDEN",
      "message": "Insufficient permissions. Required role: ADMIN"
    }
  ]
}
```

### 資源層級授權

某些 endpoints 強制執行資源層級授權：

**範例 - 存取自有資源**:

```http
GET /api/v1/customers/cust-123
Authorization: Bearer <user-token>
```

- ✅ 如果 token 屬於 `cust-123` 或使用者具有 `ADMIN` 角色，則允許
- ❌ 如果 token 屬於不同使用者且沒有 `ADMIN` 角色，則禁止

## 身份驗證 Endpoints

### 登入

**Endpoint**: `POST /api/v1/auth/login`

**Request Body**:

```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!"
}
```

**驗證規則**:

- Email: 必填，有效的 email 格式
- Password: 必填，最少 8 個字元

**成功回應** (200 OK):

```json
{
  "data": {
    "accessToken": "...",
    "refreshToken": "...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": "user-123",
      "email": "user@example.com",
      "name": "John Doe",
      "roles": ["USER", "CUSTOMER"]
    }
  }
}
```

**錯誤回應**:

- `400 Bad Request`: 無效的 email 或密碼格式
- `401 Unauthorized`: 無效的憑證
- `429 Too Many Requests`: 登入失敗次數過多

### 註冊

**Endpoint**: `POST /api/v1/auth/register`

**Request Body**:

```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "SecurePassword123!",
  "confirmPassword": "SecurePassword123!"
}
```

**驗證規則**:

- Name: 必填，2-100 個字元
- Email: 必填，有效的 email 格式，唯一
- Password: 必填，最少 8 個字元，必須包含大寫、小寫和數字
- Confirm Password: 必須與密碼相符

**成功回應** (201 Created):

```json
{
  "data": {
    "accessToken": "...",
    "refreshToken": "...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": "user-123",
      "email": "john@example.com",
      "name": "John Doe",
      "roles": ["USER", "CUSTOMER"]
    }
  }
}
```

**錯誤回應**:

- `400 Bad Request`: 驗證錯誤
- `409 Conflict`: Email 已註冊

### 更新 Token

**Endpoint**: `POST /api/v1/auth/refresh`

**Request Body**:

```json
{
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
}
```

**成功回應** (200 OK):

```json
{
  "data": {
    "accessToken": "...",
    "refreshToken": "...",
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
}
```

**錯誤回應**:

- `400 Bad Request`: 缺少 refresh token
- `401 Unauthorized`: 無效或過期的 refresh token

### 登出

**Endpoint**: `POST /api/v1/auth/logout`

**Headers**:

```http
Authorization: Bearer <access-token>
```

**成功回應** (204 No Content)

**錯誤回應**:

- `401 Unauthorized`: 無效或缺少 token

### 密碼重設請求

**Endpoint**: `POST /api/v1/auth/password-reset/request`

**Request Body**:

```json
{
  "email": "user@example.com"
}
```

**成功回應** (200 OK):

```json
{
  "data": {
    "message": "Password reset email sent if account exists"
  }
}
```

**注意**: 總是回傳成功以防止 email 列舉攻擊

### 確認密碼重設

**Endpoint**: `POST /api/v1/auth/password-reset/confirm`

**Request Body**:

```json
{
  "token": "reset-token-from-email",
  "newPassword": "NewSecurePassword123!",
  "confirmPassword": "NewSecurePassword123!"
}
```

**成功回應** (200 OK):

```json
{
  "data": {
    "message": "Password reset successful"
  }
}
```

**錯誤回應**:

- `400 Bad Request`: 無效的 token 或密碼驗證失敗
- `401 Unauthorized`: 過期的重設 token

## 程式碼範例

### JavaScript (Fetch API)

```javascript
// Login
async function login(email, password) {
  const response = await fetch('https://api.ecommerce.com/api/v1/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ email, password })
  });

  if (!response.ok) {
    throw new Error('Login failed');
  }

  const data = await response.json();

  // Store tokens securely
  sessionStorage.setItem('accessToken', data.data.accessToken);
  sessionStorage.setItem('refreshToken', data.data.refreshToken);

  return data.data.user;
}

// Authenticated request
async function getProfile() {
  const accessToken = sessionStorage.getItem('accessToken');

  const response = await fetch('https://api.ecommerce.com/api/v1/customers/me', {
    headers: {
      'Authorization': `Bearer ${accessToken}`
    }
  });

  if (response.status === 401) {
    // Token expired, try refresh
    await refreshToken();
    return getProfile(); // Retry
  }

  return response.json();
}

// Refresh token
async function refreshToken() {
  const refreshToken = sessionStorage.getItem('refreshToken');

  const response = await fetch('https://api.ecommerce.com/api/v1/auth/refresh', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ refreshToken })
  });

  if (!response.ok) {
    // Refresh failed, redirect to login
    window.location.href = '/login';
    return;
  }

  const data = await response.json();
  sessionStorage.setItem('accessToken', data.data.accessToken);
  sessionStorage.setItem('refreshToken', data.data.refreshToken);
}
```

### Java (Spring RestTemplate)

```java
// Login
public class AuthService {
    private final RestTemplate restTemplate;
    private final String apiBaseUrl = "https://api.ecommerce.com";

    public LoginResponse login(String email, String password) {
        LoginRequest request = new LoginRequest(email, password);

        ResponseEntity<ApiResponse<LoginResponse>> response = restTemplate.postForEntity(
            apiBaseUrl + "/api/v1/auth/login",
            request,
            new ParameterizedTypeReference<ApiResponse<LoginResponse>>() {}
        );

        return response.getBody().getData();
    }

    // Authenticated request
    public CustomerResponse getProfile(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<ApiResponse<CustomerResponse>> response = restTemplate.exchange(
            apiBaseUrl + "/api/v1/customers/me",
            HttpMethod.GET,
            entity,
            new ParameterizedTypeReference<ApiResponse<CustomerResponse>>() {}
        );

        return response.getBody().getData();
    }
}
```

### Python (Requests)

```python
import requests

class AuthClient:
    def __init__(self, base_url="https://api.ecommerce.com"):
        self.base_url = base_url
        self.access_token = None
        self.refresh_token = None

    def login(self, email, password):
        response = requests.post(
            f"{self.base_url}/api/v1/auth/login",
            json={"email": email, "password": password}
        )
        response.raise_for_status()

        data = response.json()["data"]
        self.access_token = data["accessToken"]
        self.refresh_token = data["refreshToken"]

        return data["user"]

    def get_profile(self):
        headers = {"Authorization": f"Bearer {self.access_token}"}

        response = requests.get(
            f"{self.base_url}/api/v1/customers/me",
            headers=headers
        )

        if response.status_code == 401:
            # Token expired, refresh
            self.refresh_access_token()
            return self.get_profile()  # Retry

        response.raise_for_status()
        return response.json()["data"]

    def refresh_access_token(self):
        response = requests.post(
            f"{self.base_url}/api/v1/auth/refresh",
            json={"refreshToken": self.refresh_token}
        )
        response.raise_for_status()

        data = response.json()["data"]
        self.access_token = data["accessToken"]
        self.refresh_token = data["refreshToken"]
```

### curl

```bash
# Login
curl -X POST https://api.ecommerce.com/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePassword123!"
  }'

# Save token to variable
TOKEN="eyJhbGciOiJIUzUxMiJ9..."

# Authenticated request
curl -X GET https://api.ecommerce.com/api/v1/customers/me \
  -H "Authorization: Bearer $TOKEN"

# Refresh token
curl -X POST https://api.ecommerce.com/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
  }'
```

## 安全最佳實踐

### Token 儲存

**✅ 建議**:

- 將 token 儲存在記憶體中（JavaScript 變數）
- 對於 web 應用程式使用 HttpOnly cookies
- 在行動裝置上使用安全儲存 API（Keychain, KeyStore）
- 對於單分頁應用程式使用 session storage

**❌ 不建議**:

- localStorage（容易受到 XSS 攻擊）
- 沒有 HttpOnly 標誌的 Cookies
- 純文字檔案
- URL 參數

### Token 傳輸

**✅ 務必**:

- 在生產環境使用 HTTPS
- 在 Authorization header 中包含 token
- 驗證 SSL 憑證

**❌ 絕不**:

- 在 URL 參數中傳送 token
- 透過 HTTP 傳送 token
- 在應用程式日誌中記錄 token
- 在使用者之間共用 token

### Token 生命週期

**最佳實踐**:

1. **短期 Access Token**: 1 小時過期
2. **較長期的 Refresh Token**: 7 天過期
3. **Token 輪換**: 在更新時發行新的 refresh token
4. **撤銷**: 實施 token 黑名單用於登出
5. **監控**: 追蹤 token 使用情況和異常

### 密碼安全

**需求**:

- 最少 8 個字元
- 至少一個大寫字母
- 至少一個小寫字母
- 至少一個數字
- 至少一個特殊字元
- 不使用常見密碼（與字典檢查）

**最佳實踐**:

- 使用密碼管理器
- 啟用雙因素驗證 (2FA)
- 定期更改密碼
- 絕不重複使用密碼

## 疑難排解

### 常見問題

**401 Unauthorized**:

- **原因**: 無效或過期的 token
- **解決方案**: 更新 token 或重新驗證

**403 Forbidden**:

- **原因**: 權限不足
- **解決方案**: 檢查所需的角色/權限

**429 Too Many Requests**:

- **原因**: 超過速率限制
- **解決方案**: 實施指數退避

### 錯誤代碼

| 代碼 | 描述 | 動作 |
|------|-------------|--------|
| `INVALID_CREDENTIALS` | 錯誤的 email 或密碼 | 檢查憑證 |
| `TOKEN_EXPIRED` | Access token 過期 | 更新 token |
| `TOKEN_INVALID` | 格式錯誤或無效的 token | 重新驗證 |
| `REFRESH_TOKEN_EXPIRED` | Refresh token 過期 | 重新驗證 |
| `INSUFFICIENT_PERMISSIONS` | 缺少所需角色 | 聯絡管理員 |
| `ACCOUNT_LOCKED` | 失敗嘗試次數過多 | 等待或聯絡支援 |

## 相關文件

- [錯誤處理](error-handling.md) - 完整錯誤代碼參考
- [Customer API](endpoints/customers.md) - 客戶資料管理
- [Security Perspective](../../perspectives/security/) - 安全架構
- [ADR-014: JWT Authentication](../../architecture/adrs/014-jwt-authentication-strategy.md) - 身份驗證設計決策

---

**最後更新**: 2025-10-25
**API Version**: v1
