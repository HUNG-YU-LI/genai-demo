---
adr_number: 014
title: "JWT-Based Authentication Strategy"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [015, 052]
affected_viewpoints: ["functional", "deployment", "operational"]
affected_perspectives: ["security", "performance"]
---

# ADR-014: JWT-Based Authentication Strategy

## 狀態

**Accepted** - 2025-10-25

## 上下文

### 問題陳述

The Enterprise E-Commerce Platform 需要secure, scalable, 和 stateless authentication mechanism that 可以:

- 支援 both web 和 mobile clients
- Scale horizontally 沒有 session affinity
- 啟用 microservices architecture 與 distributed authentication
- 提供 fine-grained access control
- 支援 token refresh 沒有 re-authentication
- Minimize database lookups 用於 authentication
- 啟用 single sign-on (SSO) capabilities

### 業務上下文

**業務驅動因素**：

- Multi-channel access (web, mobile, API partners)
- 預期的 100K+ concurrent users at peak
- 24/7 availability requirement
- 需要 API access 透過 third-party integrations
- Regulatory compliance (GDPR, data protection)

**限制條件**：

- 必須 支援 stateless authentication 用於 horizontal scaling
- Token expiration 必須 balance security 和 用戶體驗
- 必須 integrate 與 existing Spring Security framework
- 預算: No additional licensing costs preferred

### 技術上下文

**目前狀態**：

- Spring Boot 3.4.5 與 Spring Security
- Microservices architecture 與 multiple services
- AWS EKS deployment 與 auto-scaling
- No existing authentication system (greenfield)

**需求**：

- Stateless authentication (no server-side sessions)
- 支援 用於 role-based access control (RBAC)
- Token expiration 和 refresh mechanism
- Secure token storage 和 transmission
- Audit trail 用於 authentication events
- 支援 用於 token revocation

## 決策驅動因素

1. **Scalability**: 必須 支援 horizontal scaling 沒有 session affinity
2. **Performance**: Minimize authentication overhead (< 10ms per request)
3. **Security**: Industry-standard security practices
4. **Statelessness**: 啟用 true stateless microservices
5. **Developer Experience**: 容易implement 和 test
6. **Standards Compliance**: Use widely adopted standards
7. **成本**： No additional licensing fees
8. **Flexibility**: 支援 multiple client types

## 考慮的選項

### 選項 1： JWT (JSON Web Tokens) with RS256

**描述**： Stateless tokens signed with RSA asymmetric keys

**優點**：

- ✅ Truly stateless - no database lookup needed
- ✅ Horizontal scaling friendly
- ✅ Industry standard (RFC 7519)
- ✅ Self-contained (includes user info 和 permissions)
- ✅ Asymmetric signing 啟用s distributed verification
- ✅ 優秀的Spring Security整合
- ✅ 支援s token refresh pattern
- ✅ 可以 include custom claims 用於 RBAC
- ✅ No licensing costs

**缺點**：

- ⚠️ Token revocation 需要additional mechanism
- ⚠️ 大型的r token size than session IDs
- ⚠️ 可以not update permissions until token expires
- ⚠️ Key management 複雜的ity

**成本**： $0 (open standard, built into Spring Security)

**風險**： **Low** - Proven technology with extensive production use

### 選項 2： Session-Based Authentication with Redis

**描述**： Traditional session cookies with centralized session store

**優點**：

- ✅ 容易implement
- ✅ Immediate session invalidation
- ✅ Smaller cookie size
- ✅ 可以 update permissions immediately
- ✅ Familiar pattern

**缺點**：

- ❌ Requires session affinity 或 shared session store
- ❌ Additional Redis dependency 用於 sessions
- ❌ Database lookup on every request
- ❌ Not truly stateless
- ❌ 更難scale horizontally
- ❌ Single point of failure (Redis)

**成本**： $500/month (Redis cluster for sessions)

**風險**： **Medium** - Scalability limitations

### 選項 3： OAuth 2.0 with External Provider (Auth0, Okta)

**描述**： Delegate authentication to third-party provider

**優點**：

- ✅ Managed service (less 營運開銷)
- ✅ Advanced features (MFA, social login)
- ✅ Compliance certifications
- ✅ Professional 支援

**缺點**：

- ❌ Monthly licensing costs ($500-2000/month)
- ❌ Vendor lock-in
- ❌ External dependency
- ❌ Data privacy concerns (user data 與 third party)
- ❌ Network latency 用於 authentication
- ❌ Less control over authentication flow

**成本**： $1,500/month (Auth0 Professional plan)

**風險**： **Medium** - Vendor dependency, cost escalation

### 選項 4： API Keys for Service-to-Service

**描述**： Simple API keys for authentication

**優點**：

- ✅ Very 簡單的 to implement
- ✅ Low overhead
- ✅ 良好的 用於 service-to-service

**缺點**：

- ❌ Not suitable 用於 user authentication
- ❌ No expiration mechanism
- ❌ 難以rotate
- ❌ No fine-grained permissions
- ❌ Security risks if leaked

**成本**： $0

**風險**： **High** - Security limitations

## 決策結果

**選擇的選項**： **JWT with RS256 Asymmetric Signing**

### 理由

JWT 與 RS256被選擇的原因如下：

1. **Stateless Architecture**: 啟用s true stateless microservices, critical 用於 horizontal scaling
2. **Performance**: No database lookup needed 用於 authentication (< 5ms overhead)
3. **Scalability**: No session affinity required, perfect 用於 auto-scaling EKS
4. **Security**: Industry-standard 與 strong cryptographic signing
5. **Flexibility**: Self-contained tokens work 跨 all services
6. **Cost-Effective**: No licensing fees, built into Spring Security
7. **Developer Experience**: 優秀的 tooling 和 documentation
8. **Standards-Based**: RFC 7519 standard, widely 支援ed

**Token Structure**:

```json
{
  "header": {
    "alg": "RS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "user-123",
    "name": "John Doe",
    "email": "john@example.com",
    "roles": ["CUSTOMER", "PREMIUM"],
    "permissions": ["order:create", "order:read"],
    "iat": 1706356800,
    "exp": 1706357700,
    "iss": "ecommerce-platform",
    "aud": "ecommerce-api"
  },
  "signature": "..."
}
```

**為何不選 Session-Based**： Requires session affinity 或 shared session store, limiting horizontal scalability 和 adding Redis dependency.

**為何不選 OAuth 提供r**： High cost ($1,500/month) 和 vendor lock-in not justified 用於 our requirements. We 可以 implement OAuth 2.0 ourselves 與 JWT.

## 影響分析

### 利害關係人影響

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | Medium | Need to implement JWT handling | Training, code examples, libraries |
| Frontend Team | Medium | Need to store 和 send JWT tokens | Documentation, SDK 提供d |
| Operations Team | Low | Key management required | Automated key rotation, documentation |
| End Users | None | Transparent to users | N/A |
| Security Team | Positive | Industry-standard security | Regular security audits |
| API Partners | Positive | Standard token-based auth | API documentation |

### 影響半徑

**選擇的影響半徑**： **System**

影響：

- All API endpoints (authentication required)
- All microservices (token verification)
- Frontend applications (token storage 和 transmission)
- API Gateway (token validation)
- Infrastructure (key management)

### 風險評估

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| Token theft/leakage | Medium | High | Short expiration (15 min), HTTPS only, HttpOnly cookies |
| Key compromise | Low | Critical | Key rotation every 90 天, HSM storage, monitoring |
| Token revocation delay | Medium | Medium | Short expiration, blacklist 用於 critical cases |
| Clock skew issues | Low | Medium | NTP synchronization, clock skew tolerance (5 min) |
| Token size overhead | Low | Low | Minimize claims, compress if needed |

**整體風險等級**： **Low**

## 實作計畫

### 第 1 階段： Core JWT Infrastructure （第 1 週）

- [x] Generate RSA key pair (2048-bit) 用於 signing
- [x] Store private key in AWS Secrets Manager
- [x] Implement JWT generation service
- [x] Implement JWT validation filter
- [x] Configure Spring Security 與 JWT
- [x] Add JWT utilities (parse, validate, extract claims)

### 第 2 階段： Authentication Endpoints （第 2 週）

- [x] Implement `/api/v1/auth/login` endpoint
- [x] Implement `/api/v1/auth/refresh` endpoint
- [x] Implement `/api/v1/auth/logout` endpoint
- [x] Add password validation 和 hashing (BCrypt)
- [x] Implement 速率限制 用於 auth endpoints
- [x] Add authentication event logging

### 第 3 階段： Token Refresh Mechanism （第 3 週）

- [x] Implement refresh token generation
- [x] Store refresh tokens in database (hashed)
- [x] Implement refresh token rotation
- [x] Add refresh token expiration (7 天)
- [x] Implement refresh token revocation
- [x] Add refresh token cleanup job

### 第 4 階段： Integration and Testing （第 4 週）

- [x] Integrate 與 all microservices
- [x] Update API Gateway 用於 token validation
- [x] Frontend integration (token storage)
- [x] Security testing (penetration testing)
- [x] Load testing (10K concurrent users)
- [x] Documentation 和 examples

### 回滾策略

**觸發條件**：

- Critical security vulnerability discovered
- Performance degradation > 50ms per request
- Token validation failures > 1%
- Key management issues

**回滾步驟**：

1. 啟用 temporary session-based authentication
2. Investigate 和 fix JWT implementation
3. Re-deploy 與 fixes
4. Gradually migrate users back to JWT

**回滾時間**： < 2 hours

## 監控和成功標準

### 成功指標

- ✅ Authentication latency < 10ms (95th percentile)
- ✅ Token validation latency < 5ms (95th percentile)
- ✅ Zero token forgery incidents
- ✅ Token refresh success rate > 99.9%
- ✅ Authentication failure rate < 0.1%
- ✅ No unauthorized access incidents

### 監控計畫

**CloudWatch Metrics**:

- `auth.login.success` (count)
- `auth.login.failure` (count)
- `auth.token.validation.time` (histogram)
- `auth.token.expired` (count)
- `auth.token.invalid` (count)
- `auth.refresh.success` (count)

**告警**：

- Authentication failure rate > 5% 用於 5 minutes
- Token validation latency > 20ms 用於 5 minutes
- Suspicious authentication patterns (brute force)
- Key rotation failures

**Security Monitoring**:

- Failed login attempts per IP
- Token validation failures
- Refresh token usage patterns
- Anomalous authentication times/locations

**審查時程**：

- Daily: Check authentication metrics
- Weekly: Review failed authentication logs
- Monthly: Security audit of JWT implementation
- Quarterly: Key rotation 和 security review

## 後果

### 正面後果

- ✅ **Horizontal Scalability**: No session affinity required
- ✅ **Performance**: Fast authentication (< 5ms overhead)
- ✅ **Stateless**: True stateless microservices
- ✅ **Security**: Industry-standard cryptographic signing
- ✅ **Flexibility**: Works 跨 all client types
- ✅ **Cost-Effective**: No licensing fees
- ✅ **Developer-Friendly**: 優秀的 tooling 和 libraries
- ✅ **Standards-Based**: RFC 7519 compliance

### 負面後果

- ⚠️ **Token Revocation**: Requires additional blacklist mechanism 用於 immediate revocation
- ⚠️ **Token Size**: 大型的r than session IDs (typically 500-1000 bytes)
- ⚠️ **Permission Updates**: 可以not update permissions until token expires
- ⚠️ **Key Management**: Need secure key storage 和 rotation process
- ⚠️ **Clock Synchronization**: Requires NTP 用於 accurate expiration

### 技術債務

**已識別債務**：

1. No token blacklist implemented (acceptable 用於 15-min expiration)
2. Manual key rotation process (acceptable initially)
3. No token compression (acceptable 用於 current size)

**債務償還計畫**：

- **Q2 2026**: Implement Redis-based token blacklist 用於 critical revocations
- **Q3 2026**: Automate key rotation 與 AWS KMS
- **Q4 2026**: Implement token compression if size becomes issue

## 相關決策

- [ADR-015: Role-Based Access Control (RBAC) Implementation](015-role-based-access-control-implementation.md) - Authorization model
- [ADR-052: Authentication Security Hardening](052-authentication-security-hardening.md) - Additional security measures
- [ADR-009: RESTful API Design 與 OpenAPI 3.0](009-restful-api-design-with-openapi.md) - API authentication integration
- [ADR-007: Use AWS CDK 用於 Infrastructure](007-use-aws-cdk-for-infrastructure.md) - Key management infrastructure

## 備註

### JWT Configuration

```yaml
# application.yml
jwt:
  secret-key: ${JWT_SECRET_KEY} # Stored in AWS Secrets Manager
  expiration: 900000 # 15 minutes in milliseconds
  refresh-expiration: 604800000 # 7 days in milliseconds
  issuer: "ecommerce-platform"
  audience: "ecommerce-api"
  algorithm: "RS256"
```

### Token Expiration Strategy

- **Access Token**: 15 minutes (short-lived 用於 security)
- **Refresh Token**: 7 天 (balance between security 和 UX)
- **Remember Me**: 30 天 (optional, 與 additional security)

### Key Rotation Schedule

- **Frequency**: Every 90 天
- **Process**: Generate new key pair, dual-signing period (7 天), deprecate old key
- **Emergency Rotation**: < 1 hour if compromise suspected

### Security Best Practices

1. **HTTPS Only**: All JWT transmission over HTTPS
2. **HttpOnly Cookies**: Store tokens in HttpOnly cookies (web)
3. **Secure Storage**: Use Keychain/Keystore 用於 mobile apps
4. **Short Expiration**: 15-minute access tokens
5. **Token Refresh**: Implement refresh token rotation
6. **Rate Limiting**: Limit authentication attempts
7. **Audit Logging**: Log all authentication events

---

**文檔狀態**： ✅ Accepted  
**上次審查**： 2025-10-25  
**下次審查**： 2026-01-25 （每季）
