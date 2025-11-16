---
adr_number: 051
title: "Input Validation 和 Sanitization Strategy"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [009, 014, 049, 050]
affected_viewpoints: ["functional", "development"]
affected_perspectives: ["security"]
---

# ADR-051: Input Validation 和 Sanitization Strategy

## 狀態

**Accepted** - 2025-10-25

## 上下文

### 問題陳述

The Enterprise E-Commerce Platform 必須 protect against injection attacks 和 malicious input through comprehensive validation 和 sanitization:

- **SQL Injection**: Malicious SQL queries targeting database
- **Cross-Site Scripting (XSS)**: Injection of malicious scripts
- **Cross-Site Request Forgery (CSRF)**: Unauthorized state-changing operations
- **Command Injection**: OS command execution attempts
- **Path Traversal**: Unauthorized file system access
- **XML/JSON Injection**: Malicious data in structured formats
- **Business Logic Bypass**: Invalid data causing un預期的 behavior

The platform 需要defense-in-depth validation strategy 與 multiple layers:

- Frontend validation 用於 用戶體驗
- API Gateway validation 用於 early rejection
- Application validation 用於 business rules
- Database validation as final defense

### 業務上下文

**業務驅動因素**：

- **Data Integrity**: Ensure data quality 和 consistency
- **Security Compliance**: PCI-DSS, GDPR requirements
- **User Trust**: Protect customer data from breaches
- **Regulatory Requirements**: 必須 prevent data manipulation

**Taiwan-Specific Context**:

- **High Attack Volume**: Frequent injection attack attempts
- **E-Commerce Targeting**: Payment 和 customer data are prime targets
- **Regulatory Scrutiny**: Taiwan Personal Data Protection Act compliance

**限制條件**：

- 必須 not impact 用戶體驗 (< 10ms validation overhead)
- 必須 支援 internationalization (Unicode, multi-language)
- 必須 integrate 與 existing Spring Boot application
- 預算: No additional infrastructure cost

### 技術上下文

**目前狀態**：

- Spring Boot 3.4.5 與 Spring Validation
- Basic @Valid annotation usage
- No comprehensive sanitization
- No CSRF protection
- Limited input validation

**Attack Vectors**:

- **Login Form**: SQL injection, XSS in username/password
- **Search**: SQL injection, XSS in search terms
- **Product Reviews**: XSS in review content
- **User Profile**: XSS in name, address, bio
- **File Upload**: Malicious file uploads
- **API Endpoints**: JSON injection, parameter tampering

## 決策驅動因素

1. **Security**: Prevent all injection attacks (SQL, XSS, CSRF)
2. **Performance**: < 10ms validation overhead
3. **User Experience**: Clear, helpful error messages
4. **維持ability**: Centralized validation logic
5. **Compliance**: Meet PCI-DSS 和 GDPR requirements
6. **Flexibility**: 支援 custom validation rules
7. **Integration**: 無縫的Spring Boot整合
8. **成本**： No additional infrastructure cost

## 考慮的選項

### 選項 1： Multi-Layer Validation (Frontend + API Gateway + Application + Database) - Recommended

**描述**： Defense-in-depth with validation at every layer

**Validation Layers**:

1. **Frontend**: UX validation (immediate feedback)
2. **API Gateway**: Schema validation (early rejection)
3. **Application**: Business logic validation (comprehensive)
4. **Database**: Final defense (constraints, triggers)

**優點**：

- ✅ **Defense in Depth**: Multiple layers of protection
- ✅ **Early Rejection**: Invalid requests rejected at gateway
- ✅ **User Experience**: Immediate frontend feedback
- ✅ **Security**: Comprehensive protection against all injection types
- ✅ **Performance**: < 10ms overhead per layer
- ✅ **維持ability**: Clear separation of concerns

**缺點**：

- ⚠️ **複雜的ity**: Multiple validation layers to 維持
- ⚠️ **Duplication**: Some validation logic duplicated 跨 layers

**成本**： $0 (using existing infrastructure)

**風險**： **Low** - Industry best practice

### 選項 2： Application-Only Validation (Basic)

**描述**： Validation only at application layer

**優點**：

- ✅ **簡單的**: Single validation layer
- ✅ **Low Maintenance**: One place to update

**缺點**：

- ❌ **No Early Rejection**: Invalid requests reach application
- ❌ **Performance**: Wasted processing on invalid requests
- ❌ **Security**: Single point of failure

**成本**： $0

**風險**： **Medium** - Insufficient defense in depth

### 選項 3： Third-Party Validation Service

**描述**： Use external validation service

**優點**：

- ✅ **Managed Service**: Less 營運開銷
- ✅ **Advanced Features**: ML-based validation

**缺點**：

- ❌ **High Cost**: $500-2,000/month
- ❌ **Latency**: Additional network hop
- ❌ **Vendor Lock-In**: 難以migrate

**成本**： $500-2,000/month

**風險**： **Medium** - Vendor dependency

## 決策結果

**選擇的選項**： **Multi-Layer Validation (Frontend + API Gateway + Application + Database)**

### 理由

Multi-layer validation被選擇的原因如下：

1. **Defense in Depth**: Multiple layers 提供 comprehensive protection
2. **Early Rejection**: Invalid requests rejected at gateway (saves resources)
3. **User Experience**: Immediate frontend feedback
4. **Security**: Protection against all injection types
5. **Performance**: < 10ms overhead per layer
6. **Cost-Effective**: Uses existing infrastructure
7. **Compliance**: Meets PCI-DSS 和 GDPR requirements

**Validation Strategy 透過 Layer**:

**Layer 1 - Frontend (UX Validation)**:

- **Purpose**: Immediate user feedback, 降低 server load
- **Validation**: Format, length, required fields
- **Technology**: React Hook Form, Angular Forms
- **Example**: Email format, password strength, required fields

**Layer 2 - API Gateway (Schema Validation)**:

- **Purpose**: Early rejection of malformed requests
- **Validation**: JSON schema, request structure
- **Technology**: OpenAPI 3.0 schema validation
- **Example**: Required fields, data types, enum values

**Layer 3 - Application (Business Logic Validation)**:

- **Purpose**: Comprehensive validation 和 sanitization
- **Validation**: Business rules, cross-field validation, sanitization
- **Technology**: Spring Validation, Hibernate Validator, OWASP Java Encoder
- **Example**: Business rules, SQL injection prevention, XSS prevention

**Layer 4 - Database (Final Defense)**:

- **Purpose**: Data integrity constraints
- **Validation**: Database constraints, triggers
- **Technology**: PostgreSQL constraints, check constraints
- **Example**: Unique constraints, foreign keys, check constraints

**SQL Injection Prevention**:

- **Mandatory**: Use parameterized queries (JPA, JDBC PreparedStatement)
- **Prohibited**: String concatenation 用於 SQL queries
- **ORM Usage**: Prefer JPA/Hibernate over native SQL
- **Code Review**: Automated checks 用於 SQL injection vulnerabilities

**XSS Prevention**:

- **Output Encoding**: Encode all user-generated content
- **HTML Sanitization**: Use OWASP Java HTML Sanitizer
- **CSP Headers**: Content Security Policy headers
- **HTTPOnly Cookies**: Prevent JavaScript access to cookies
- **Secure Cookies**: HTTPS-only cookies

**CSRF Prevention**:

- **CSRF Tokens**: Required 用於 all state-changing operations
- **SameSite Cookies**: SameSite=Strict 或 Lax
- **Double-Submit Cookie**: Additional CSRF protection
- **Referer Validation**: Validate Referer header

**為何不選 Application-Only**： Insufficient defense in depth, no early rejection of invalid requests.

**為何不選 Third-Party**： High cost ($500-2K/month) not justified when we 可以 implement comprehensive validation using existing infrastructure.

## 實作計畫

### 第 1 階段： Application-Level Validation （第 1 週）

- [x] Configure Spring Validation
- [x] Implement custom validators 用於 business rules
- [x] Add @Valid annotations to all DTOs
- [x] Implement validation error handling
- [x] Add validation unit tests

### 第 2 階段： SQL Injection Prevention （第 2 週）

- [x] Audit all SQL queries 用於 injection vulnerabilities
- [x] Convert string concatenation to parameterized queries
- [x] Implement JPA query validation
- [x] Add ArchUnit rules to prevent SQL injection
- [x] Conduct security testing

### 第 3 階段： XSS Prevention （第 3 週）

- [x] Implement output encoding 用於 all user-generated content
- [x] Configure OWASP Java HTML Sanitizer
- [x] Add CSP headers
- [x] Configure HTTPOnly 和 Secure cookies
- [x] Test XSS prevention 與 OWASP ZAP

### 第 4 階段： CSRF Protection （第 4 週）

- [x] 啟用 Spring Security CSRF protection
- [x] Configure CSRF tokens 用於 all state-changing operations
- [x] Implement SameSite cookie attribute
- [x] Add double-submit cookie pattern
- [x] Test CSRF protection

### Phase 5: API Gateway Validation （第 5 週）

- [x] Configure OpenAPI 3.0 schema validation
- [x] Implement request validation at API Gateway
- [x] Add validation error responses
- [x] Test 與 invalid requests
- [x] Monitor validation metrics

### 回滾策略

**觸發條件**：

- Validation causing service outage
- False positive rate > 1% (legitimate requests rejected)
- Performance degradation > 20ms

**回滾步驟**：

1. Disable specific validation rules causing issues
2. Revert to previous validation configuration
3. Investigate 和 fix issues
4. Re-deploy 與 corrections

**回滾時間**： < 15 minutes

## 監控和成功標準

### 成功指標

- ✅ **Injection Prevention**: 100% of injection attempts blocked
- ✅ **False Positive Rate**: < 0.1% legitimate requests rejected
- ✅ **Performance**: < 10ms validation overhead
- ✅ **User Experience**: Clear, helpful error messages
- ✅ **Compliance**: Pass PCI-DSS 和 GDPR audits
- ✅ **Security**: Zero successful injection attacks

### 監控計畫

**CloudWatch Metrics**:

- `validation.errors` (count 透過 field)
- `validation.sql_injection_attempts` (count)
- `validation.xss_attempts` (count)
- `validation.csrf_failures` (count)
- `validation.latency` (histogram)

**告警**：

- **P0 Critical**: SQL injection attempts > 100/min
- **P1 High**: XSS attempts > 50/min
- **P2 Medium**: Validation error rate > 10%
- **P3 Low**: Unusual validation patterns

**審查時程**：

- **Real-Time**: 24/7 monitoring dashboard
- **Daily**: Review validation errors
- **Weekly**: Analyze attack patterns
- **Monthly**: Security review 和 validation optimization
- **Quarterly**: Penetration testing

## 後果

### 正面後果

- ✅ **Enhanced Security**: Protection against all injection attacks
- ✅ **Data Integrity**: Ensure data quality 和 consistency
- ✅ **User Experience**: Clear, helpful error messages
- ✅ **Compliance**: Meet PCI-DSS 和 GDPR requirements
- ✅ **Performance**: < 10ms validation overhead
- ✅ **維持ability**: Centralized validation logic
- ✅ **Cost-Effective**: No additional infrastructure cost

### 負面後果

- ⚠️ **複雜的ity**: Multiple validation layers to 維持
- ⚠️ **Duplication**: Some validation logic duplicated 跨 layers
- ⚠️ **Development Overhead**: More code to write 和 test

### 技術債務

**已識別債務**：

1. Manual validation rule updates (acceptable initially)
2. Limited internationalization 支援 用於 error messages
3. No automated validation testing

**債務償還計畫**：

- **Q2 2026**: Implement automated validation rule generation
- **Q3 2026**: Enhance internationalization 支援
- **Q4 2026**: Implement automated validation testing

## 相關決策

- [ADR-009: RESTful API Design 與 OpenAPI 3.0](009-restful-api-design-with-openapi.md) - API schema validation
- [ADR-014: JWT-Based Authentication Strategy](014-jwt-based-authentication-strategy.md) - Authentication
- [ADR-049: Web Application Firewall (WAF) Rules 和 Policies](049-web-application-firewall-rules-and-policies.md) - WAF protection
- [ADR-050: API Security 和 Rate Limiting Strategy](050-api-security-and-rate-limiting-strategy.md) - API security

## 備註

### Validation Examples

**Spring Validation (Application Layer)**:

```java
public record CreateCustomerRequest(
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "Name can only contain letters and spaces")
    String name,
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email format is invalid")
    @Size(max = 255, message = "Email is too long")
    String email,
    
    @NotBlank(message = "Password is required")
    @Size(min = 12, max = 128, message = "Password must be between 12 and 128 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$", 
             message = "Password must contain uppercase, lowercase, number and special character")
    String password
) {}
```

**SQL Injection Prevention**:

```java
// ✅ GOOD: Parameterized query
@Query("SELECT c FROM Customer c WHERE c.email = :email AND c.status = :status")
Optional<Customer> findByEmailAndStatus(@Param("email") String email, @Param("status") String status);

// ❌ BAD: String concatenation (SQL injection risk)
String query = "SELECT * FROM customers WHERE email = '" + email + "'";
```

**XSS Prevention**:

```java
@Component
public class XSSProtectionService {
    
    private final PolicyFactory policy = Sanitizers.FORMATTING
        .and(Sanitizers.LINKS)
        .and(Sanitizers.BLOCKS)
        .and(Sanitizers.IMAGES);
    
    public String sanitizeHtml(String input) {
        if (input == null) return null;
        return policy.sanitize(input);
    }
    
    public String escapeHtml(String input) {
        if (input == null) return null;
        return StringEscapeUtils.escapeHtml4(input);
    }
}
```

**CSRF Protection**:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            .ignoringRequestMatchers("/api/v1/public/**")
        );
        
        return http.build();
    }
}
```

### Database Constraints

**PostgreSQL Constraints**:

```sql
-- Unique constraint
ALTER TABLE customers ADD CONSTRAINT uk_customer_email UNIQUE (email);

-- Check constraint
ALTER TABLE orders ADD CONSTRAINT chk_order_total_positive CHECK (total_amount > 0);

-- Foreign key constraint
ALTER TABLE order_items ADD CONSTRAINT fk_order_items_order 
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE;

-- Not null constraint
ALTER TABLE customers ALTER COLUMN email SET NOT NULL;
```

### Security Headers

**Spring Security Headers**:

```java
@Configuration
public class SecurityHeadersConfiguration {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.headers(headers -> headers
            .contentSecurityPolicy("default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'")
            .frameOptions().deny()
            .xssProtection().and()
            .contentTypeOptions().and()
            .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                .maxAgeInSeconds(31536000)
                .includeSubdomains(true)
                .preload(true))
        );
        
        return http.build();
    }
}
```

### Cost Breakdown

**Monthly Costs**:

- Spring Validation: $0 (included in Spring Boot)
- OWASP Java HTML Sanitizer: $0 (open source)
- Database Constraints: $0 (included in PostgreSQL)
- API Gateway Validation: $0 (included in API Gateway)
- **Total**: $0 (no additional cost)

---

**文檔狀態**： ✅ Accepted  
**上次審查**： 2025-10-25  
**下次審查**： 2026-01-25 （每季）
