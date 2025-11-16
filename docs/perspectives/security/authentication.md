# Authentication

> **Last Updated**: 2025-10-23
> **Status**: ✅ Active

## 概述

本文件描述電子商務平台中使用的 authentication 機制。Authentication 是在授予對資源的存取權限之前驗證使用者和系統身份的過程。系統使用 JWT（JSON Web Tokens）作為主要的 authentication 機制，在所有服務之間提供無狀態、可擴展的 authentication。

## Authentication 策略

### 基於 JWT 的 Authentication

系統實作基於 JWT 的 authentication，具有以下特性：

- **無狀態**：不需要伺服器端 session 儲存
- **可擴展**：在多個服務實例之間無縫工作
- **安全**：加密簽名的 token 防止竄改
- **高效**：快速的 token 驗證，開銷最小

### Token 類型

#### Access Token

- **目的**：用於 API 存取的短期 token
- **有效期**：1 小時
- **包含**：使用者 ID、角色、權限
- **使用方式**：包含在所有 API 請求的 Authorization header 中

#### Refresh Token

- **目的**：用於獲取新 access token 的長期 token
- **有效期**：24 小時
- **包含**：使用者 ID、token family ID
- **使用方式**：當目前 token 過期時用於獲取新的 access token

## Authentication 流程

### 初始登入

```text

1. 使用者提交憑證 (email + password)
2. 系統對照資料庫驗證憑證
3. 系統生成 access token 和 refresh token
4. Token 返回給客戶端
5. 客戶端安全地儲存 token

```

### 後續請求

```text

1. 客戶端在 Authorization header 中包含 access token
2. 系統驗證 token 簽名和過期時間
3. 系統提取使用者身份和權限
4. 使用使用者上下文處理請求

```

### Token 更新

```text

1. 客戶端偵測到 access token 過期
2. 客戶端將 refresh token 傳送到 /auth/refresh 端點
3. 系統驗證 refresh token
4. 系統生成新的 access token
5. 新的 access token 返回給客戶端

```

## JWT Token 結構

### Access Token Claims

```json
{
  "sub": "user-123",
  "userId": "user-123",
  "email": "customer@example.com",
  "roles": ["CUSTOMER"],
  "permissions": ["order:read", "order:create"],
  "iat": 1698765432,
  "exp": 1698769032,
  "iss": "ecommerce-platform",
  "aud": "ecommerce-api"
}
```

### Refresh Token Claims

```json
{
  "sub": "user-123",
  "userId": "user-123",
  "tokenFamily": "family-456",
  "iat": 1698765432,
  "exp": 1698851832,
  "iss": "ecommerce-platform",
  "type": "refresh"
}
```

## 實作

### JWT Token Provider

```java
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token-validity:3600}")
    private int accessTokenValidity; // 1 hour

    @Value("${jwt.refresh-token-validity:86400}")
    private int refreshTokenValidity; // 24 hours

    /**
     * 為已 authentication 的使用者生成 access token
     */
    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userDetails.getUsername());
        claims.put("email", userDetails.getEmail());
        claims.put("roles", userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList()));

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(userDetails.getUsername())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + accessTokenValidity * 1000))
            .setIssuer("ecommerce-platform")
            .setAudience("ecommerce-api")
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
    }

    /**
     * 為使用者生成 refresh token
     */
    public String generateRefreshToken(String userId) {
        String tokenFamily = UUID.randomUUID().toString();

        return Jwts.builder()
            .setSubject(userId)
            .claim("userId", userId)
            .claim("tokenFamily", tokenFamily)
            .claim("type", "refresh")
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + refreshTokenValidity * 1000))
            .setIssuer("ecommerce-platform")
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
    }

    /**
     * 驗證 token 簽名和過期時間
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            logger.debug("JWT token 已過期: {}", e.getMessage());
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            logger.warn("無效的 JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 從 token 提取使用者 ID
     */
    public String getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody();
        return claims.get("userId", String.class);
    }

    /**
     * 從 token 提取角色
     */
    public List<String> getRolesFromToken(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody();
        return claims.get("roles", List.class);
    }
}
```

### Authentication Filter

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            String token = extractTokenFromRequest(request);

            if (token != null && tokenProvider.validateToken(token)) {
                String userId = tokenProvider.getUserIdFromToken(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(userId);

                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );

                authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            logger.error("無法設定使用者 authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

### Authentication Controller

```java
@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(
            @Valid @RequestBody LoginRequest request) {

        AuthenticationResponse response = authenticationService.authenticate(
            request.email(),
            request.password()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refreshToken(
            @Valid @RequestBody TokenRefreshRequest request) {

        TokenRefreshResponse response = authenticationService.refreshToken(
            request.refreshToken()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> logout(
            @RequestHeader("Authorization") String token) {

        authenticationService.logout(token);
        return ResponseEntity.noContent().build();
    }
}
```

## 密碼 Security

### 密碼要求

- **最小長度**：8 個字元
- **複雜性**：必須包含：
  - 至少一個大寫字母 (A-Z)
  - 至少一個小寫字母 (a-z)
  - 至少一個數字 (0-9)
  - 至少一個特殊字元 (!@#$%^&*())

### 密碼雜湊

```java
@Component
public class PasswordEncoderService {

    private static final int BCRYPT_STRENGTH = 12;
    private final BCryptPasswordEncoder encoder =
        new BCryptPasswordEncoder(BCRYPT_STRENGTH);

    /**
     * 使用 BCrypt 和強度因子 12 進行密碼雜湊
     */
    public String encodePassword(String rawPassword) {
        validatePasswordStrength(rawPassword);
        return encoder.encode(rawPassword);
    }

    /**
     * 驗證密碼是否符合雜湊
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }

    /**
     * 驗證密碼符合強度要求
     */
    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new WeakPasswordException(
                "密碼長度必須至少 8 個字元"
            );
        }

        if (!password.matches(".*[A-Z].*")) {
            throw new WeakPasswordException(
                "密碼必須包含至少一個大寫字母"
            );
        }

        if (!password.matches(".*[a-z].*")) {
            throw new WeakPasswordException(
                "密碼必須包含至少一個小寫字母"
            );
        }

        if (!password.matches(".*[0-9].*")) {
            throw new WeakPasswordException(
                "密碼必須包含至少一個數字"
            );
        }

        if (!password.matches(".*[!@#$%^&*()].*")) {
            throw new WeakPasswordException(
                "密碼必須包含至少一個特殊字元"
            );
        }
    }
}
```

## Security 配置

### Spring Security 配置

```java
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfiguration {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .cors().and()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/public/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling()
                .authenticationEntryPoint(authenticationEntryPoint)
            .and()
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
```

## API 使用範例

### 登入請求

```bash
curl -X POST https://api.example.com/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "customer@example.com",
    "password": "SecurePass123!"
  }'
```

**回應:**

```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": "user-123",
    "email": "customer@example.com",
    "roles": ["CUSTOMER"]
  }
}
```

### 已驗證的請求

```bash
curl -X GET https://api.example.com/api/v1/customers/user-123 \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
```

### Token 更新請求

```bash
curl -X POST https://api.example.com/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
  }'
```

**回應:**

```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

## Security 考量

### Token 儲存

**客戶端儲存：**

- ✅ **推薦**：儲存在記憶體中（JavaScript 變數）
- ✅ **可接受**：HttpOnly、Secure cookies
- ❌ **不推薦**：localStorage（易受 XSS 攻擊）
- ❌ **絕不**：sessionStorage 沒有額外保護

### Token 傳輸

- 始終使用 HTTPS 進行 token 傳輸
- 在 Authorization header 中包含 token（不在 URL 參數中）
- 使用 Bearer token scheme: `Authorization: Bearer <token>`

### Token 驗證

- 在每個請求上驗證 token 簽名
- 檢查 token 過期時間
- 驗證 token 簽發者和受眾
- 驗證 token 尚未被撤銷（如果維護撤銷列表）

### 暴力破解保護

```java
@Component
public class LoginAttemptService {

    private final LoadingCache<String, Integer> attemptsCache;
    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 15;

    public LoginAttemptService() {
        attemptsCache = CacheBuilder.newBuilder()
            .expireAfterWrite(LOCKOUT_DURATION_MINUTES, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                @Override
                public Integer load(String key) {
                    return 0;
                }
            });
    }

    public void loginSucceeded(String email) {
        attemptsCache.invalidate(email);
    }

    public void loginFailed(String email) {
        int attempts = attemptsCache.getUnchecked(email);
        attemptsCache.put(email, attempts + 1);
    }

    public boolean isBlocked(String email) {
        return attemptsCache.getUnchecked(email) >= MAX_ATTEMPTS;
    }
}
```

## 監控和日誌記錄

### 要記錄的 Authentication 事件

- ✅ 成功的登入嘗試
- ✅ 失敗的登入嘗試
- ✅ 帳戶鎖定
- ✅ Token 更新請求
- ✅ 登出事件
- ✅ 密碼變更
- ✅ 可疑活動（多次失敗、異常位置）

### 日誌格式

```java
logger.info("Authentication 成功",
    kv("event", "AUTH_SUCCESS"),
    kv("userId", userId),
    kv("email", email),
    kv("ipAddress", ipAddress),
    kv("userAgent", userAgent),
    kv("timestamp", Instant.now()));

logger.warn("Authentication 失敗",
    kv("event", "AUTH_FAILURE"),
    kv("email", email),
    kv("reason", "INVALID_CREDENTIALS"),
    kv("ipAddress", ipAddress),
    kv("timestamp", Instant.now()));
```

## 測試

### Authentication 測試

```java
@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void should_return_tokens_when_valid_credentials_provided() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email": "customer@example.com",
                        "password": "ValidPass123!"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
            .andExpect(jsonPath("$.user.email").value("customer@example.com"));
    }

    @Test
    void should_return_401_when_invalid_credentials_provided() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email": "customer@example.com",
                        "password": "WrongPassword"
                    }
                    """))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void should_reject_request_without_token() throws Exception {
        mockMvc.perform(get("/api/v1/customers/user-123"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void should_accept_request_with_valid_token() throws Exception {
        String token = generateValidToken();

        mockMvc.perform(get("/api/v1/customers/user-123")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }
}
```

## 相關文件

- [Authorization](authorization.md) - 基於角色的存取控制
- [Security Overview](overview.md) - 整體 security 觀點
- [Security Standards](../../.kiro/steering/security-standards.md) - 詳細的 security 標準

## 參考資料

- JWT Specification: <https://tools.ietf.org/html/rfc7519>
- OWASP Authentication Cheat Sheet: <https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html>
- Spring Security Documentation: <https://spring.io/projects/spring-security>
