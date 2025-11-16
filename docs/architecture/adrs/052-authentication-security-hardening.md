---
adr_number: 052
title: "Authentication Security Hardening"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [014, 015, 053]
affected_viewpoints: ["functional", "operational"]
affected_perspectives: ["security", "availability"]
---

# ADR-052: Authentication Security Hardening

## 狀態

**Accepted** - 2025-10-25

## 上下文

### 問題陳述

The Enterprise E-Commerce Platform 需要comprehensive authentication security hardening to protect against:

- Brute force attacks 和 credential stuffing
- Weak password vulnerabilities
- Account takeover attempts
- Session hijacking 和 token theft
- Insider threats 和 privilege escalation
- Compliance violations (password policies)

Taiwan's cyber security environment presents additional challenges:

- Frequent targeted attacks from state-sponsored actors
- High-value e-commerce platform as attractive target
- 需要 defense-in-depth authentication security
- Regulatory compliance requirements (Taiwan Personal Data Protection Act)

### 業務上下文

**業務驅動因素**：

- Protect customer accounts 和 sensitive data
- 維持 customer trust 和 platform reputation
- Comply 與 security regulations 和 standards
- Prevent financial losses from account compromise
- 支援 100K+ user accounts 與 varying risk profiles

**限制條件**：

- 必須 balance security 與 用戶體驗
- 可以not 需要複雜的 authentication 用於 all users
- 必須 支援 legacy password migration
- 預算: $2,000/month 用於 MFA services

### 技術上下文

**目前狀態**：

- JWT-based authentication (ADR-014)
- Basic password hashing 與 BCrypt
- No multi-factor authentication
- No account lockout mechanism
- No anomalous login detection

**需求**：

- Strong password policy enforcement
- Multi-factor authentication (MFA) 支援
- Account protection mechanisms (lockout, anomaly detection)
- Secure password storage 和 rotation
- Session timeout 和 management
- Audit trail 用於 authentication events

## 決策驅動因素

1. **Security**: Prevent unauthorized access 和 account compromise
2. **Compliance**: Meet regulatory requirements (GDPR, Taiwan PDPA)
3. **User Experience**: Balance security 與 usability
4. **Scalability**: 支援 100K+ users 沒有 performance degradation
5. **成本**： Minimize operational costs while maintaining security
6. **Flexibility**: 支援 different security levels 用於 different user types
7. **Auditability**: Complete audit trail 用於 compliance
8. **Recovery**: 啟用 secure account recovery mechanisms

## 考慮的選項

### 選項 1： Comprehensive Security Hardening (Recommended)

**描述**： Multi-layered authentication security with password policy, MFA, account protection, and monitoring

**Components**:

- Strong password policy (12+ chars, 複雜的ity, history, rotation)
- Multi-factor authentication (TOTP, SMS backup)
- Account lockout (5 failed attempts = 15-min lockout)
- Anomalous login detection (location, device, time)
- Session timeout (30 minutes idle, 8 hours absolute)
- Password storage (BCrypt cost factor 12)
- Mandatory MFA 用於 admin accounts

**優點**：

- ✅ Defense-in-depth security approach
- ✅ Protects against multiple attack vectors
- ✅ Compliance 與 security standards
- ✅ Flexible security levels per user type
- ✅ Comprehensive audit trail
- ✅ Industry best practices
- ✅ 支援s risk-based authentication

**缺點**：

- ⚠️ Increased implementation 複雜的ity
- ⚠️ Higher 營運開銷
- ⚠️ Potential user friction (MFA, password requirements)
- ⚠️ SMS costs 用於 MFA backup

**成本**： $2,000/month (SMS MFA, monitoring tools)

**風險**： **Low** - Proven security practices

### 選項 2： Basic Security with Password Policy Only

**描述**： Minimal security hardening with password policy and basic lockout

**優點**：

- ✅ 簡單implement
- ✅ Low 營運開銷
- ✅ Minimal user friction
- ✅ Low cost

**缺點**：

- ❌ Insufficient protection against sophisticated attacks
- ❌ No MFA protection
- ❌ Limited anomaly detection
- ❌ Compliance gaps
- ❌ Higher risk of account compromise

**成本**： $0

**風險**： **High** - Inadequate security for e-commerce platform

### 選項 3： Third-Party Authentication Service (Auth0, Okta)

**描述**： Delegate authentication security to managed service

**優點**：

- ✅ Managed security features
- ✅ Professional 支援
- ✅ Advanced features (adaptive MFA, risk scoring)
- ✅ Compliance certifications

**缺點**：

- ❌ High cost ($3,000-5,000/month)
- ❌ Vendor lock-in
- ❌ Less control over security policies
- ❌ Data privacy concerns
- ❌ External dependency

**成本**： $4,000/month

**風險**： **Medium** - Vendor dependency

## 決策結果

**選擇的選項**： **Comprehensive Security Hardening (Option 1)**

### 理由

Comprehensive security hardening被選擇的原因如下：

1. **Defense-in-Depth**: Multiple security layers protect against various attack vectors
2. **Compliance**: Meets regulatory requirements 用於 password security 和 MFA
3. **Risk Mitigation**: Taiwan's cyber threat environment 需要robust authentication security
4. **Cost-Effective**: $2,000/month is reasonable 用於 100K+ users
5. **Flexibility**: Risk-based authentication adapts to user behavior
6. **Control**: Full control over security policies 和 implementation
7. **Scalability**: Designed to scale 與 user growth

### Password Policy

**需求**：

- Minimum 12 characters (industry best practice)
- 複雜的ity requirements:
  - At least 1 uppercase letter
  - At least 1 lowercase letter
  - At least 1 number
  - At least 1 special character (!@#$%^&*()_+-=[]{}|;:,.<>?)
- Password history: No reuse of last 5 passwords
- Mandatory password change every 90 天
- No common passwords (check against breach database)
- No personal information in password (name, email, birthdate)

**Implementation**:

```java
public class PasswordPolicy {
    private static final int MIN_LENGTH = 12;
    private static final int PASSWORD_HISTORY_SIZE = 5;
    private static final int PASSWORD_EXPIRY_DAYS = 90;
    
    public ValidationResult validate(String password, User user) {
        // Length check
        if (password.length() < MIN_LENGTH) {
            return ValidationResult.fail("Password must be at least 12 characters");
        }
        
        // Complexity checks
        if (!hasUppercase(password)) {
            return ValidationResult.fail("Password must contain uppercase letter");
        }
        if (!hasLowercase(password)) {
            return ValidationResult.fail("Password must contain lowercase letter");
        }
        if (!hasDigit(password)) {
            return ValidationResult.fail("Password must contain number");
        }
        if (!hasSpecialChar(password)) {
            return ValidationResult.fail("Password must contain special character");
        }
        
        // History check
        if (isInPasswordHistory(password, user)) {
            return ValidationResult.fail("Cannot reuse last 5 passwords");
        }
        
        // Common password check
        if (isCommonPassword(password)) {
            return ValidationResult.fail("Password is too common");
        }
        
        return ValidationResult.success();
    }
}
```

### Multi-Factor Authentication (MFA)

**Primary Method**: Time-based One-Time Password (TOTP)

- Standard: RFC 6238
- Apps: Google Authenticator, Authy, Microsoft Authenticator
- Code validity: 30 seconds
- Code length: 6 digits

**Backup Method**: SMS One-Time Password

- Used when TOTP unavailable
- Code validity: 5 minutes
- Rate limited: 3 codes per hour
- Cost: $0.01 per SMS

**MFA Requirements**:

- Mandatory 用於 admin accounts
- Optional 但 recommended 用於 customer accounts
- Mandatory 用於 high-value transactions (> $1,000)
- Mandatory after password reset
- Remember device 用於 30 天 (optional)

**Implementation**:

```java
@Service
public class MfaService {
    
    public boolean verifyTotp(String userId, String code) {
        String secret = getUserTotpSecret(userId);
        TimeBasedOneTimePasswordGenerator totp = new TimeBasedOneTimePasswordGenerator();
        
        // Check current code and adjacent time windows (±1)
        long currentTime = System.currentTimeMillis() / 30000;
        for (long time = currentTime - 1; time <= currentTime + 1; time++) {
            String expectedCode = totp.generateOneTimePasswordString(secret, time);
            if (code.equals(expectedCode)) {
                return true;
            }
        }
        return false;
    }
    
    public void sendSmsCode(String userId, String phoneNumber) {
        // Rate limiting check
        if (hasExceededSmsRateLimit(userId)) {
            throw new RateLimitExceededException("Too many SMS requests");
        }
        
        // Generate 6-digit code
        String code = generateRandomCode(6);
        
        // Store code with 5-minute expiration
        storeVerificationCode(userId, code, Duration.ofMinutes(5));
        
        // Send SMS
        smsService.send(phoneNumber, "Your verification code: " + code);
    }
}
```

### Account Protection

**Account Lockout**:

- Trigger: 5 failed login attempts within 15 minutes
- Lockout duration: 15 minutes
- Unlock methods: Time expiration, email verification, admin unlock
- Notification: Email sent to user on lockout

**Anomalous Login Detection**:

- Location-based: Login from new country/city
- Device-based: Login from new device/browser
- Time-based: Login at unusual time (3 AM local time)
- Velocity-based: Multiple logins from different locations
- Action: Require MFA verification, send notification email

**Session Management**:

- Idle timeout: 30 minutes of inactivity
- Absolute timeout: 8 hours maximum session
- Concurrent sessions: Maximum 3 active sessions per user
- Session termination: Logout all sessions on password change

**Implementation**:

```java
@Service
public class AccountProtectionService {
    
    public void recordFailedLogin(String userId, String ipAddress) {
        FailedLoginAttempt attempt = new FailedLoginAttempt(userId, ipAddress, Instant.now());
        failedLoginRepository.save(attempt);
        
        // Check if account should be locked
        long recentFailures = countRecentFailures(userId, Duration.ofMinutes(15));
        if (recentFailures >= 5) {
            lockAccount(userId, Duration.ofMinutes(15));
            notificationService.sendAccountLockoutEmail(userId);
        }
    }
    
    public boolean isAnomalousLogin(String userId, LoginContext context) {
        UserLoginHistory history = getUserLoginHistory(userId);
        
        // Check location
        if (!history.hasLoginFromCountry(context.getCountry())) {
            return true; // New country
        }
        
        // Check device
        if (!history.hasLoginFromDevice(context.getDeviceFingerprint())) {
            return true; // New device
        }
        
        // Check time
        if (isUnusualLoginTime(context.getLocalTime())) {
            return true; // Unusual time (3-6 AM)
        }
        
        // Check velocity
        if (hasRecentLoginFromDifferentLocation(userId, context, Duration.ofMinutes(30))) {
            return true; // Impossible travel
        }
        
        return false;
    }
}
```

### Password Storage

**Hashing Algorithm**: BCrypt 與 cost factor 12

- Industry standard 用於 password hashing
- Adaptive cost factor (increases 與 hardware 改善ments)
- Built-in salt generation
- Resistant to rainbow table attacks

**Alternative**: Argon2 (future consideration)

- Winner of Password Hashing Competition (2015)
- Memory-hard algorithm (resistant to GPU attacks)
- Configurable memory, time, 和 parallelism parameters

**Implementation**:

```java
@Service
public class PasswordService {
    
    private static final int BCRYPT_COST_FACTOR = 12;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(BCRYPT_COST_FACTOR);
    
    public String hashPassword(String plainPassword) {
        return encoder.encode(plainPassword);
    }
    
    public boolean verifyPassword(String plainPassword, String hashedPassword) {
        return encoder.matches(plainPassword, hashedPassword);
    }
    
    public void rotatePassword(String userId, String newPassword) {
        // Validate new password
        ValidationResult validation = passwordPolicy.validate(newPassword, getUser(userId));
        if (!validation.isValid()) {
            throw new InvalidPasswordException(validation.getMessage());
        }
        
        // Hash new password
        String hashedPassword = hashPassword(newPassword);
        
        // Update password and history
        updateUserPassword(userId, hashedPassword);
        addToPasswordHistory(userId, hashedPassword);
        
        // Invalidate all sessions
        sessionService.invalidateAllSessions(userId);
        
        // Send notification
        notificationService.sendPasswordChangedEmail(userId);
    }
}
```

## 影響分析

### 利害關係人影響

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| End Users | Medium | Stronger password requirements, MFA setup | Clear instructions, gradual rollout |
| Admin Users | High | Mandatory MFA, stricter policies | Training, 支援 documentation |
| Development Team | High | Implement security features | Training, code examples, testing tools |
| Operations Team | Medium | Monitor security events, 處理 lockouts | Runbooks, automated alerts |
| Security Team | Positive | Enhanced security posture | Regular audits, penetration testing |
| Customer 支援 | Medium | 處理 lockout 和 MFA issues | Training, 支援 scripts |

### 影響半徑

**選擇的影響半徑**： **System**

影響：

- All authentication endpoints
- User registration 和 login flows
- Password reset functionality
- Session management
- Admin interfaces
- Mobile applications
- API authentication

### 風險評估

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| User friction from MFA | High | Medium | Optional 用於 customers, clear benefits communication |
| Account lockout false positives | Medium | Medium | Email unlock, admin override, reasonable thresholds |
| SMS delivery failures | Medium | Low | TOTP as primary, SMS as backup, retry mechanism |
| Password policy too strict | Medium | Low | User education, password strength meter |
| Implementation bugs | Low | High | Comprehensive testing, gradual rollout, monitoring |

**整體風險等級**： **Low**

## 實作計畫

### 第 1 階段： Password Policy （第 1-2 週）

- [ ] Implement password validation service
- [ ] Add password 複雜的ity checks
- [ ] Implement password history tracking
- [ ] Add password expiration mechanism
- [ ] Create password strength meter (frontend)
- [ ] Add common password blacklist
- [ ] Implement password rotation reminders

### 第 2 階段： Account Protection （第 3-4 週）

- [ ] Implement failed login tracking
- [ ] Add account lockout mechanism
- [ ] Create anomalous login detection
- [ ] Implement session timeout
- [ ] Add concurrent 會話管理
- [ ] Create account lockout notifications
- [ ] Add admin unlock functionality

### 第 3 階段： Multi-Factor Authentication （第 5-6 週）

- [ ] Implement TOTP generation 和 verification
- [ ] Add MFA enrollment flow
- [ ] Implement SMS OTP backup
- [ ] Add device remember functionality
- [ ] Create MFA recovery codes
- [ ] Implement MFA enforcement 用於 admins
- [ ] Add MFA status to user profile

### 第 4 階段： Integration and Testing （第 7-8 週）

- [ ] Integrate 與 existing authentication (ADR-014)
- [ ] Update frontend 用於 password policy
- [ ] Add MFA setup UI
- [ ] Security testing (penetration testing)
- [ ] Load testing (account lockout scenarios)
- [ ] User acceptance testing
- [ ] Documentation 和 training

### 回滾策略

**觸發條件**：

- Critical security vulnerability in implementation
- Excessive user lockouts (> 5% of login attempts)
- MFA service failures (> 1% failure rate)
- Performance degradation (> 100ms authentication overhead)

**回滾步驟**：

1. Disable MFA enforcement (make optional)
2. Relax password policy temporarily
3. Disable account lockout
4. Investigate 和 fix issues
5. Gradually re-啟用 features

**回滾時間**： < 1 hour

## 監控和成功標準

### 成功指標

- ✅ Password policy compliance: 100% of new passwords
- ✅ MFA adoption: > 30% of customers, 100% of admins
- ✅ Account lockout rate: < 1% of login attempts
- ✅ False positive lockouts: < 0.1%
- ✅ Anomalous login detection: > 95% accuracy
- ✅ Authentication overhead: < 50ms
- ✅ Zero account compromise incidents

### 監控計畫

**CloudWatch Metrics**:

- `auth.password.validation.failure` (count 透過 reason)
- `auth.account.lockout` (count)
- `auth.mfa.enrollment` (count)
- `auth.mfa.verification.success` (count)
- `auth.mfa.verification.failure` (count)
- `auth.anomalous.login.detected` (count)
- `auth.session.timeout` (count)

**告警**：

- Account lockout rate > 2% 用於 10 minutes
- MFA verification failure rate > 5% 用於 10 minutes
- Anomalous login detection spike (> 100 in 5 minutes)
- Password policy bypass attempts
- Suspicious authentication patterns

**Security Monitoring**:

- Failed login attempts per IP/user
- Account lockout patterns
- MFA bypass attempts
- Password reset abuse
- Session hijacking attempts

**審查時程**：

- Daily: Check authentication security metrics
- Weekly: Review anomalous login detections
- Monthly: Password policy effectiveness review
- Quarterly: Security audit 和 penetration testing

## 後果

### 正面後果

- ✅ **Enhanced Security**: Multi-layered protection against account compromise
- ✅ **Compliance**: Meets regulatory password 和 MFA requirements
- ✅ **Auditability**: Complete audit trail 用於 authentication events
- ✅ **Flexibility**: Risk-based authentication adapts to user behavior
- ✅ **User Protection**: Proactive detection 和 prevention of account takeover
- ✅ **Reputation**: Demonstrates commitment to security
- ✅ **Cost-Effective**: $2,000/month 用於 comprehensive security

### 負面後果

- ⚠️ **User Friction**: Stronger password requirements may frustrate some users
- ⚠️ **支援 Overhead**: Increased 支援 requests 用於 lockouts 和 MFA issues
- ⚠️ **Implementation 複雜的ity**: Multiple security features to implement 和 維持
- ⚠️ **Operational Overhead**: Monitoring 和 responding to security events
- ⚠️ **SMS Costs**: $0.01 per SMS 用於 MFA backup

### 技術債務

**已識別債務**：

1. BCrypt cost factor may need increase in future (currently 12)
2. SMS MFA is less secure than TOTP (acceptable as backup)
3. No biometric authentication 支援 (future enhancement)
4. No adaptive authentication (risk scoring)

**債務償還計畫**：

- **Q2 2026**: Implement adaptive authentication 與 risk scoring
- **Q3 2026**: Add biometric authentication 支援 (WebAuthn)
- **Q4 2026**: Migrate to Argon2 用於 password hashing
- **2027**: Implement passwordless authentication options

## 相關決策

- [ADR-014: JWT-Based Authentication Strategy](014-jwt-based-authentication-strategy.md) - Base authentication mechanism
- [ADR-015: Role-Based Access Control (RBAC) Implementation](015-role-based-access-control-implementation.md) - Authorization integration
- [ADR-053: Security Monitoring 和 Incident Response](053-security-monitoring-incident-response.md) - Security monitoring integration
- [ADR-054: Data Loss Prevention (DLP) Strategy](054-data-loss-prevention-strategy.md) - Data protection integration

## 備註

### Password Policy Configuration

```yaml
# application.yml
security:
  password:
    min-length: 12
    require-uppercase: true
    require-lowercase: true
    require-digit: true
    require-special-char: true
    history-size: 5
    expiry-days: 90
    common-password-check: true
```

### MFA Configuration

```yaml
# application.yml
security:
  mfa:
    totp:
      enabled: true
      issuer: "E-Commerce Platform"
      period: 30
      digits: 6
    sms:
      enabled: true
      validity-minutes: 5
      rate-limit: 3
      cost-per-sms: 0.01
    enforcement:
      admin-mandatory: true
      customer-optional: true
      high-value-transaction: true
```

### Account Protection Configuration

```yaml
# application.yml
security:
  account-protection:
    lockout:
      max-attempts: 5
      window-minutes: 15
      duration-minutes: 15
    session:
      idle-timeout-minutes: 30
      absolute-timeout-hours: 8
      max-concurrent-sessions: 3
    anomaly-detection:
      enabled: true
      check-location: true
      check-device: true
      check-time: true
      check-velocity: true
```

### Gradual Rollout Plan

**Phase 1 （第 1-2 週）**: Password policy for new users
**Phase 2 （第 3-4 週）**: Password policy for existing users (grace period)
**Phase 3 （第 5-6 週）**: Account lockout enabled
**Phase 4 （第 7-8 週）**: MFA optional for customers
**Phase 5 （第 9-10 週）**: MFA mandatory for admins
**Phase 6 （第 11-12 週）**: Anomalous login detection enabled

---

**文檔狀態**： ✅ Accepted  
**上次審查**： 2025-10-25  
**下次審查**： 2026-01-25 （每季）
