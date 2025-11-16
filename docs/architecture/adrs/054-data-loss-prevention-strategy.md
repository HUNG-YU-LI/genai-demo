---
adr_number: 054
title: "Data Loss Prevention (DLP) Strategy"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [015, 016, 053, 055]
affected_viewpoints: ["functional", "information", "operational"]
affected_perspectives: ["security", "availability"]
---

# ADR-054: Data Loss Prevention (DLP) Strategy

## 狀態

**Accepted** - 2025-10-25

## 上下文

### 問題陳述

The Enterprise E-Commerce Platform 處理s sensitive customer data that 需要comprehensive protection against:

- Data exfiltration 透過 external attackers
- Insider threats 和 unauthorized access
- Accidental data exposure
- Compliance violations (GDPR, PCI-DSS)
- Data breaches 和 leaks
- Unauthorized data sharing

Taiwan's regulatory environment 和 cyber threat landscape require:

- Compliance 與 Taiwan Personal Data Protection Act
- Protection against state-sponsored data theft
- Secure handling of payment card data (PCI-DSS)
- Audit trails 用於 data access 和 usage
- Data residency 和 sovereignty requirements

### 業務上下文

**業務驅動因素**：

- Protect customer trust 和 platform reputation
- Comply 與 data protection regulations
- Prevent financial losses from data breaches
- 維持 PCI-DSS compliance 用於 payment processing
- 啟用 secure business operations

**限制條件**：

- 必須 not impact system performance signifi可以tly
- 可以not block legitimate business operations
- 必須 支援 data analytics 和 reporting
- 預算: $3,000/month 用於 DLP tools 和 services

### 技術上下文

**目前狀態**：

- Basic encryption at rest 和 in transit (ADR-016)
- RBAC 用於 access control (ADR-015)
- No data classification system
- No data exfiltration detection
- No data masking 用於 non-production environments
- Manual data access auditing

**需求**：

- Sensitive data identification 和 classification
- Data exfiltration prevention
- Data masking 用於 non-production environments
- Access control 和 monitoring
- Audit trails 用於 compliance
- Automated policy enforcement

## 決策驅動因素

1. **Data Protection**: Prevent unauthorized data access 和 exfiltration
2. **Compliance**: Meet GDPR, PCI-DSS, Taiwan PDPA requirements
3. **Performance**: Minimal impact on system performance (< 5% overhead)
4. **Usability**: 啟用 legitimate business operations
5. **Auditability**: Complete audit trails 用於 compliance
6. **Scalability**: 處理 growing data volumes
7. **成本**： Optimize operational costs
8. **Automation**: Automated policy enforcement 和 monitoring

## 考慮的選項

### 選項 1： Comprehensive DLP Strategy (Recommended)

**描述**： Multi-layered data loss prevention with classification, monitoring, masking, and access control

**Components**:

- **Data Classification**: Identify 和 tag sensitive data (PII, PCI, confidential)
- **Database Activity Monitoring**: Monitor all database access 和 queries
- **API Call Auditing**: Track all API calls accessing sensitive data
- **Data Masking**: Mask sensitive data in non-production environments
- **Access Control**: Least privilege principle 與 periodic reviews
- **Anomaly Detection**: Detect unusual data access patterns
- **Audit Logging**: Comprehensive audit trails 用於 compliance

**優點**：

- ✅ Defense-in-depth data protection
- ✅ Compliance-ready (GDPR, PCI-DSS, Taiwan PDPA)
- ✅ Automated policy enforcement
- ✅ Comprehensive audit trails
- ✅ 支援s legitimate business operations
- ✅ Scalable architecture
- ✅ Cost-effective ($3,000/month)

**缺點**：

- ⚠️ Implementation 複雜的ity
- ⚠️ Requires data classification effort
- ⚠️ Potential false positives
- ⚠️ Operational overhead

**成本**： $3,000/month (AWS Macie $1,500, monitoring tools $1,500)

**風險**： **Low** - Proven DLP practices

### 選項 2： Basic Data Protection (Encryption Only)

**描述**： Rely on encryption and basic access control

**優點**：

- ✅ 簡單implement
- ✅ Low 營運開銷
- ✅ Low cost

**缺點**：

- ❌ No data exfiltration detection
- ❌ No data masking
- ❌ Limited audit capabilities
- ❌ Compliance gaps
- ❌ No anomaly detection

**成本**： $0 (already implemented)

**風險**： **High** - Insufficient data protection

### 選項 3： Third-Party DLP Solution (Symantec, McAfee)

**描述**： Deploy enterprise DLP solution

**優點**：

- ✅ Advanced DLP features
- ✅ Proven enterprise solution
- ✅ Professional 支援

**缺點**：

- ❌ Very high cost ($10,000-20,000/month)
- ❌ 複雜的 deployment
- ❌ Performance overhead
- ❌ Vendor lock-in

**成本**： $15,000/month

**風險**： **Medium** - High cost and complexity

## 決策結果

**選擇的選項**： **Comprehensive DLP Strategy (Option 1)**

### 理由

Comprehensive DLP strategy被選擇的原因如下：

1. **Compliance**: Meets GDPR, PCI-DSS, 和 Taiwan PDPA requirements
2. **Cost-Effective**: $3,000/month vs $15,000+ 用於 enterprise DLP
3. **AWS Native**: Leverages AWS services (Macie, CloudTrail, GuardDuty)
4. **Scalable**: 處理s platform growth automatically
5. **Automated**: Policy enforcement 和 monitoring 與 minimal overhead
6. **Flexible**: 支援s legitimate business operations
7. **Comprehensive**: Multi-layered protection against data loss

### Data Classification

**Sensitivity Tiers**:

**Tier 1: Highly Sensitive (PII, PCI)**

- Customer names, addresses, phone numbers
- Email addresses
- Payment card numbers (PAN)
- Bank account information
- Government ID numbers
- Passwords 和 authentication credentials

**Tier 2: Sensitive (Business Data)**

- Order details 和 history
- Inventory levels 和 pricing
- Customer purchase patterns
- Business analytics data
- Internal communications

**Tier 3: Public (Non-Sensitive)**

- Product catalog
- Public marketing content
- General system information

**Classification Implementation**:

```java
@Entity
@Table(name = "customers")
public class Customer {
    
    @Id
    private String id;
    
    @Column
    @DataClassification(tier = DataTier.TIER1, type = DataType.PII)
    private String name;
    
    @Column
    @DataClassification(tier = DataTier.TIER1, type = DataType.PII)
    @Encrypted
    private String email;
    
    @Column
    @DataClassification(tier = DataTier.TIER1, type = DataType.PII)
    @Encrypted
    private String phoneNumber;
    
    @Column
    @DataClassification(tier = DataTier.TIER1, type = DataType.PCI)
    @Encrypted
    @Masked
    private String paymentCardLast4;
}
```

### Database Activity Monitoring

**Monitoring Approach**:

- 啟用 PostgreSQL audit logging
- Monitor all SELECT, INSERT, UPDATE, DELETE operations
- Track query patterns 和 data volumes
- Detect anomalous queries (大型的 result sets, unusual times)
- Alert on suspicious activity

**Implementation**:

```sql
-- Enable PostgreSQL audit logging
ALTER SYSTEM SET log_statement = 'all';
ALTER SYSTEM SET log_connections = 'on';
ALTER SYSTEM SET log_disconnections = 'on';
ALTER SYSTEM SET log_duration = 'on';

-- Create audit table
CREATE TABLE audit_log (
    id SERIAL PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
    user_id VARCHAR(255),
    session_id VARCHAR(255),
    query_text TEXT,
    rows_affected INTEGER,
    execution_time_ms INTEGER,
    source_ip VARCHAR(45),
    application_name VARCHAR(255)
);

-- Audit trigger for sensitive tables
CREATE OR REPLACE FUNCTION audit_sensitive_data()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO audit_log (user_id, query_text, rows_affected)
    VALUES (current_user, current_query(), 1);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER customer_audit_trigger
AFTER INSERT OR UPDATE OR DELETE ON customers
FOR EACH ROW EXECUTE FUNCTION audit_sensitive_data();
```

**Anomaly Detection**:

```python
# Detect anomalous database queries
def detect_anomalous_queries():
    # Large result set (> 10,000 rows)
    large_queries = """
    SELECT user_id, query_text, rows_affected
    FROM audit_log
    WHERE rows_affected > 10000
    AND timestamp > NOW() - INTERVAL '1 hour'
    """
    
    # Unusual query time (3 AM - 6 AM)
    unusual_time_queries = """
    SELECT user_id, query_text, timestamp
    FROM audit_log
    WHERE EXTRACT(HOUR FROM timestamp) BETWEEN 3 AND 6
    AND timestamp > NOW() - INTERVAL '24 hours'
    """
    
    # Bulk data export
    bulk_export_queries = """
    SELECT user_id, COUNT(*) as query_count, SUM(rows_affected) as total_rows
    FROM audit_log
    WHERE timestamp > NOW() - INTERVAL '1 hour'
    GROUP BY user_id
    HAVING SUM(rows_affected) > 50000
    """
    
    # Send alerts for anomalies
    for query in execute_queries([large_queries, unusual_time_queries, bulk_export_queries]):
        send_security_alert(query)
```

### API Call Auditing

**Monitoring Approach**:

- Log all API calls accessing sensitive data
- Track request/response payloads (sanitized)
- Monitor data volume per user/IP
- Detect unusual access patterns
- Alert on suspicious activity

**Implementation**:

```java
@Aspect
@Component
public class DataAccessAuditAspect {
    
    @Around("@annotation(DataAccess)")
    public Object auditDataAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        DataAccess annotation = getAnnotation(joinPoint);
        String userId = getCurrentUserId();
        String ipAddress = getCurrentIpAddress();
        
        // Log access attempt
        AuditLog auditLog = AuditLog.builder()
            .timestamp(Instant.now())
            .userId(userId)
            .ipAddress(ipAddress)
            .dataType(annotation.dataType())
            .operation(annotation.operation())
            .resourceId(getResourceId(joinPoint))
            .build();
        
        try {
            Object result = joinPoint.proceed();
            
            // Log successful access
            auditLog.setStatus("SUCCESS");
            auditLog.setRowsAccessed(getRowCount(result));
            
            // Check for anomalous access
            if (isAnomalousAccess(auditLog)) {
                sendSecurityAlert(auditLog);
            }
            
            return result;
        } catch (Exception e) {
            // Log failed access
            auditLog.setStatus("FAILED");
            auditLog.setErrorMessage(e.getMessage());
            throw e;
        } finally {
            auditLogRepository.save(auditLog);
        }
    }
    
    private boolean isAnomalousAccess(AuditLog auditLog) {
        // Check for large data access
        if (auditLog.getRowsAccessed() > 1000) {
            return true;
        }
        
        // Check for unusual time
        int hour = LocalDateTime.now().getHour();
        if (hour >= 3 && hour <= 6) {
            return true;
        }
        
        // Check for velocity (multiple accesses in short time)
        long recentAccesses = countRecentAccesses(auditLog.getUserId(), Duration.ofMinutes(5));
        if (recentAccesses > 100) {
            return true;
        }
        
        return false;
    }
}
```

### Data Masking

**Masking Strategy**:

**Production Environment**: No masking (encrypted data)

**Staging Environment**: Partial masking

- Email: `j***@example.com`
- Phone: `+886-9**-***-123`
- Credit Card: `****-****-****-1234`
- Name: `John D***`

**Development Environment**: Full masking

- Email: `user{id}@example.com`
- Phone: `+886-900-000-{id}`
- Credit Card: `4111-1111-1111-{id}`
- Name: `Test User {id}`

**Test Environment**: Synthetic data

- Generated fake data using Faker library
- Realistic 但 not real customer data

**Implementation**:

```java
@Service
public class DataMaskingService {
    
    public String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];
        
        if (localPart.length() <= 2) {
            return "*".repeat(localPart.length()) + "@" + domain;
        }
        
        return localPart.charAt(0) + "*".repeat(localPart.length() - 2) + 
               localPart.charAt(localPart.length() - 1) + "@" + domain;
    }
    
    public String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return phone;
        }
        
        String last4 = phone.substring(phone.length() - 4);
        return "*".repeat(phone.length() - 4) + last4;
    }
    
    public String maskCreditCard(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return cardNumber;
        }
        
        String last4 = cardNumber.substring(cardNumber.length() - 4);
        return "****-****-****-" + last4;
    }
    
    public Customer maskCustomer(Customer customer, Environment environment) {
        if (environment == Environment.PRODUCTION) {
            return customer; // No masking in production
        }
        
        Customer masked = customer.clone();
        
        if (environment == Environment.STAGING) {
            // Partial masking
            masked.setEmail(maskEmail(customer.getEmail()));
            masked.setPhone(maskPhone(customer.getPhone()));
        } else {
            // Full masking
            masked.setEmail("user" + customer.getId() + "@example.com");
            masked.setPhone("+886-900-000-" + customer.getId());
            masked.setName("Test User " + customer.getId());
        }
        
        return masked;
    }
}
```

### Access Control

**Least Privilege Principle**:

- Users have minimum permissions required 用於 their role
- Temporary elevated access 用於 specific tasks
- Automatic permission expiration
- Regular permission reviews (quarterly)

**Access Control Matrix**:

| Role | Customer PII | Payment Data | Order Data | Product Data |
|------|--------------|--------------|------------|--------------|
| Customer | Own data only | Own data only | Own data only | Read all |
| Customer 支援 | Read all | Read last 4 digits | Read all | Read all |
| Admin | Read/Write all | Read last 4 digits | Read/Write all | Read/Write all |
| Developer | No access | No access | Read staging | Read/Write all |
| Analyst | Masked data | No access | Read all | Read all |

**Implementation**:

```java
@Service
public class DataAccessControlService {
    
    public boolean canAccessCustomerData(String userId, String customerId, AccessType accessType) {
        User user = userRepository.findById(userId).orElseThrow();
        
        // Customer can only access own data
        if (user.hasRole("CUSTOMER")) {
            return userId.equals(customerId) && accessType == AccessType.READ;
        }
        
        // Customer support can read all customer data
        if (user.hasRole("CUSTOMER_SUPPORT")) {
            return accessType == AccessType.READ;
        }
        
        // Admin can read/write all customer data
        if (user.hasRole("ADMIN")) {
            return true;
        }
        
        // Developer has no access to production customer data
        if (user.hasRole("DEVELOPER")) {
            return false;
        }
        
        // Analyst can read masked data
        if (user.hasRole("ANALYST")) {
            return accessType == AccessType.READ_MASKED;
        }
        
        return false;
    }
    
    public void requestTemporaryAccess(String userId, String resourceType, Duration duration, String justification) {
        // Create temporary access request
        TemporaryAccessRequest request = TemporaryAccessRequest.builder()
            .userId(userId)
            .resourceType(resourceType)
            .duration(duration)
            .justification(justification)
            .status(RequestStatus.PENDING)
            .build();
        
        temporaryAccessRepository.save(request);
        
        // Notify approver
        notificationService.sendAccessRequestNotification(request);
    }
    
    public void reviewPermissions() {
        // Quarterly permission review
        List<User> users = userRepository.findAll();
        
        for (User user : users) {
            // Check for unused permissions
            List<Permission> unusedPermissions = findUnusedPermissions(user, Duration.ofDays(90));
            
            if (!unusedPermissions.isEmpty()) {
                // Notify manager for review
                notificationService.sendPermissionReviewNotification(user, unusedPermissions);
            }
            
            // Check for expired temporary access
            List<TemporaryAccess> expiredAccess = findExpiredTemporaryAccess(user);
            
            for (TemporaryAccess access : expiredAccess) {
                revokeTemporaryAccess(access);
            }
        }
    }
}
```

### AWS Macie Integration

**Sensitive Data Discovery**:

- Automatically s可以 S3 buckets 用於 sensitive data
- Identify PII, PCI, 和 confidential data
- Generate data classification reports
- Alert on unencrypted sensitive data

**Implementation**:

```python
# Enable Macie via CDK
macie = aws_macie.CfnSession(
    self, "Macie",
    status="ENABLED",
    finding_publishing_frequency="FIFTEEN_MINUTES"
)

# Create classification job
classification_job = aws_macie.CfnClassificationJob(
    self, "ClassificationJob",
    job_type="SCHEDULED",
    name="SensitiveDataDiscovery",
    s3_job_definition=aws_macie.CfnClassificationJob.S3JobDefinitionProperty(
        bucket_definitions=[
            aws_macie.CfnClassificationJob.S3BucketDefinitionProperty(
                account_id=account_id,
                buckets=[bucket_name]
            )
        ]
    ),
    schedule_frequency=aws_macie.CfnClassificationJob.JobScheduleFrequencyProperty(
        daily_schedule={}
    )
)
```

## 影響分析

### 利害關係人影響

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | Medium | Data masking in non-prod environments | Automated masking, synthetic data |
| Data Analysts | Medium | Access to masked data only | 提供 aggregated data, masked datasets |
| Customer 支援 | Low | Audit logging of data access | Clear policies, training |
| Security Team | Positive | Enhanced data protection | Regular reviews, automated monitoring |
| Compliance Team | Positive | Audit trails 用於 compliance | Automated reporting |

### 影響半徑

**選擇的影響半徑**： **System**

影響：

- All databases 和 資料儲存s
- All API endpoints accessing sensitive data
- All non-production environments
- Data analytics 和 reporting
- Access control policies

### 風險評估

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| False positive alerts | Medium | Low | Alert tuning, feedback loop |
| Performance overhead | Low | Medium | Optimize queries, caching |
| Data masking errors | Low | High | Comprehensive testing, validation |
| Access control bypass | Low | Critical | Regular audits, penetration testing |
| Compliance violations | Low | Critical | Automated compliance checks, regular reviews |

**整體風險等級**： **Low**

## 實作計畫

### 第 1 階段： Data Classification （第 1-2 週）

- [ ] Define data classification tiers
- [ ] Annotate database schemas
- [ ] Implement classification metadata
- [ ] Create data inventory
- [ ] Document data flows

### 第 2 階段： Monitoring and Auditing （第 3-4 週）

- [ ] 啟用 database audit logging
- [ ] Implement API call auditing
- [ ] Create audit log storage
- [ ] Set up anomaly detection
- [ ] Configure alerts

### 第 3 階段： Data Masking （第 5-6 週）

- [ ] Implement masking service
- [ ] Create masked datasets 用於 staging
- [ ] Generate synthetic data 用於 dev/test
- [ ] Test masking accuracy
- [ ] Deploy to non-production environments

### 第 4 階段： Access Control （第 7-8 週）

- [ ] Implement access control service
- [ ] Create access control matrix
- [ ] Set up temporary access workflow
- [ ] Implement permission reviews
- [ ] 啟用 AWS Macie
- [ ] Test 和 validate

### 回滾策略

**觸發條件**：

- Critical performance degradation (> 10% overhead)
- Data masking errors exposing real data
- Access control blocking legitimate operations
- Excessive false positive alerts

**回滾步驟**：

1. Disable anomaly detection alerts
2. Relax access control policies
3. Disable data masking temporarily
4. Investigate 和 fix issues
5. Gradually re-啟用 features

**回滾時間**： < 2 hours

## 監控和成功標準

### 成功指標

- ✅ Data classification coverage: 100% of sensitive data
- ✅ Audit log coverage: 100% of sensitive data access
- ✅ Data masking accuracy: 100% (no real data in non-prod)
- ✅ Access control compliance: 100%
- ✅ Anomaly detection accuracy: > 90%
- ✅ Performance overhead: < 5%
- ✅ Zero data breach incidents

### 監控計畫

**CloudWatch Metrics**:

- `dlp.data.access.count` (count 透過 data tier)
- `dlp.anomaly.detected` (count 透過 type)
- `dlp.access.denied` (count 透過 reason)
- `dlp.audit.log.size` (bytes)
- `dlp.masking.errors` (count)

**告警**：

- Anomalous data access detected
- 大型的 data export (> 10,000 rows)
- Unusual access time (3-6 AM)
- Access control violation
- Data masking error

**審查時程**：

- Daily: Review anomalous access alerts
- Weekly: Analyze data access patterns
- Monthly: Permission review
- Quarterly: Compliance audit

## 後果

### 正面後果

- ✅ **Data Protection**: Comprehensive protection against data loss
- ✅ **Compliance**: Meets GDPR, PCI-DSS, Taiwan PDPA requirements
- ✅ **Auditability**: Complete audit trails 用於 compliance
- ✅ **Insider Threat Protection**: Detect 和 prevent insider threats
- ✅ **Cost-Effective**: $3,000/month 用於 enterprise-grade DLP
- ✅ **Automated**: Policy enforcement 與 minimal overhead

### 負面後果

- ⚠️ **Implementation 複雜的ity**: Multiple components to implement
- ⚠️ **Operational Overhead**: Monitoring 和 alert management
- ⚠️ **Performance Impact**: 3-5% overhead 用於 auditing
- ⚠️ **False Positives**: Potential 用於 legitimate operations to trigger alerts
- ⚠️ **Data Masking Effort**: Initial effort to create masked datasets

### 技術債務

**已識別債務**：

1. Manual data classification (acceptable initially)
2. Basic anomaly detection (rule-based)
3. No ML-powered threat detection
4. Limited data lineage tracking

**債務償還計畫**：

- **Q2 2026**: Implement ML-powered anomaly detection
- **Q3 2026**: Automate data classification 與 Macie
- **Q4 2026**: Implement data lineage tracking
- **2027**: Integrate 與 CASB 用於 cloud data protection

## 相關決策

- [ADR-015: Role-Based Access Control (RBAC) Implementation](015-role-based-access-control-implementation.md) - Access control foundation
- [ADR-016: Data Encryption Strategy](016-data-encryption-strategy.md) - Data encryption at rest 和 in transit
- [ADR-053: Security Monitoring 和 Incident Response](053-security-monitoring-incident-response.md) - Security monitoring integration
- [ADR-055: Vulnerability Management 和 Patching Strategy](055-vulnerability-management-patching-strategy.md) - Vulnerability management

## 備註

### Data Classification Tags

```java
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface DataClassification {
    DataTier tier();
    DataType type();
    boolean encrypted() default false;
    boolean masked() default false;
}

public enum DataTier {
    TIER1, // Highly Sensitive (PII, PCI)
    TIER2, // Sensitive (Business Data)
    TIER3  // Public (Non-Sensitive)
}

public enum DataType {
    PII,           // Personally Identifiable Information
    PCI,           // Payment Card Industry data
    CONFIDENTIAL,  // Business confidential
    PUBLIC         // Public information
}
```

### Compliance Mapping

- **GDPR Article 32**: Security of processing (encryption, audit logs)
- **PCI-DSS Requirement 10**: Track 和 monitor all access to network resources 和 cardholder data
- **Taiwan PDPA Article 27**: Security measures 用於 personal data protection

---

**文檔狀態**： ✅ Accepted  
**上次審查**： 2025-10-25  
**下次審查**： 2026-01-25 （每季）
