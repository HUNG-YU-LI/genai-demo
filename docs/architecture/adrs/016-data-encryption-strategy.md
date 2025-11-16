---
adr_number: 016
title: "Data Encryption Strategy (At Rest 和 In Transit)"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [001, 014, 054]
affected_viewpoints: ["information", "deployment", "operational"]
affected_perspectives: ["security", "performance", "availability"]
---

# ADR-016: Data Encryption Strategy (At Rest 和 In Transit)

## 狀態

**Accepted** - 2025-10-25

## 上下文

### 問題陳述

The Enterprise E-Commerce Platform 處理s sensitive data including:

- Customer personal information (PII)
- Payment card data (PCI-DSS compliance required)
- Authentication credentials
- Business confidential data

We 需要comprehensive encryption strategy：

- Protects data at rest in databases 和 file storage
- Secures data in transit between services 和 clients
- Meets regulatory compliance (GDPR, PCI-DSS, Taiwan Personal Data Protection Act)
- 維持s acceptable performance (< 10ms encryption overhead)
- 啟用s secure key management 和 rotation
- 支援s data residency requirements

### 業務上下文

**業務驅動因素**：

- PCI-DSS Level 1 compliance required 用於 payment processing
- GDPR compliance 用於 EU customers
- Taiwan Personal Data Protection Act compliance
- Customer trust 和 brand reputation
- Regulatory penalties avoidance (up to 4% of annual revenue)

**限制條件**：

- 必須 encrypt payment card data (PCI-DSS Requirement 3)
- 必須 encrypt PII (GDPR Article 32)
- Performance impact < 10ms per operation
- Key rotation 沒有 service downtime
- 預算: $2,000/month 用於 encryption infrastructure

### 技術上下文

**目前狀態**：

- PostgreSQL database on AWS RDS
- S3 用於 file storage
- Redis 用於 caching
- Kafka 用於 event streaming
- Spring Boot microservices

**需求**：

- Encrypt sensitive data at rest
- Encrypt all data in transit (TLS 1.3)
- Secure key management
- Key rotation capability
- Audit trail 用於 encryption operations
- Performance: < 10ms overhead

## 決策驅動因素

1. **Compliance**: Meet PCI-DSS, GDPR, local regulations
2. **Security**: Industry-standard encryption algorithms
3. **Performance**: Minimal performance impact
4. **Key Management**: Secure, auditable key lifecycle
5. **Scalability**: 支援 millions of encrypted records
6. **Operational**: Automated key rotation, monitoring
7. **成本**： Within budget constraints
8. **Flexibility**: 支援 multiple encryption methods

## 考慮的選項

### 選項 1： AWS KMS with Envelope Encryption

**描述**： Use AWS KMS for key management, envelope encryption for data

**優點**：

- ✅ Managed key lifecycle (rotation, auditing)
- ✅ FIPS 140-2 Level 2 validated
- ✅ Integrated 與 AWS services (RDS, S3, EBS)
- ✅ Automatic key rotation
- ✅ CloudTrail audit logging
- ✅ Envelope encryption 降低s KMS API calls
- ✅ 支援s customer-managed keys (CMK)
- ✅ Multi-region key replication

**缺點**：

- ⚠️ AWS vendor lock-in
- ⚠️ API rate limits (1200 req/sec per CMK)
- ⚠️ Cost per API call ($0.03 per 10K requests)
- ⚠️ Network latency 用於 KMS calls

**成本**： $1,500/month (KMS + API calls)

**風險**： **Low** - Proven AWS service

### 選項 2： HashiCorp Vault

**描述**： Self-managed encryption and key management

**優點**：

- ✅ Cloud-agnostic (no vendor lock-in)
- ✅ Advanced features (dynamic secrets, PKI)
- ✅ Fine-grained access control
- ✅ Audit logging
- ✅ Multi-cloud 支援

**缺點**：

- ❌ Self-managed infrastructure (營運開銷)
- ❌ High availability setup 複雜的
- ❌ Additional infrastructure costs
- ❌ Team learning curve
- ❌ Maintenance burden

**成本**： $3,000/month (infrastructure + operations)

**風險**： **Medium** - Operational complexity

### 選項 3： Application-Level Encryption Only

**描述**： Encrypt in application code, manage keys manually

**優點**：

- ✅ Full control over encryption
- ✅ No external dependencies
- ✅ Low cost

**缺點**：

- ❌ Manual key management (high risk)
- ❌ No automatic key rotation
- ❌ 難以audit
- ❌ Key storage security concerns
- ❌ Not compliant 與 PCI-DSS
- ❌ High operational risk

**成本**： $0

**風險**： **High** - Security and compliance risks

### 選項 4： Database-Level Encryption Only

**描述**： Rely on RDS encryption, no application-level encryption

**優點**：

- ✅ 容易啟用
- ✅ Transparent to application
- ✅ Low cost

**缺點**：

- ❌ Coarse-grained (encrypts entire database)
- ❌ No field-level encryption
- ❌ Keys accessible to DBAs
- ❌ Not sufficient 用於 PCI-DSS
- ❌ 可以not selectively encrypt fields

**成本**： Included in RDS

**風險**： **High** - Insufficient for compliance

## 決策結果

**選擇的選項**： **AWS KMS with Envelope Encryption + Multi-Layer Encryption**

### 理由

We 將 implement a multi-layer encryption strategy:

1. **Data in Transit**: TLS 1.3 用於 all network communication
2. **Data at Rest - Infrastructure Level**: AWS service encryption (RDS, S3, EBS)
3. **Data at Rest - Application Level**: Field-level encryption 用於 sensitive data using AWS KMS
4. **Key Management**: AWS KMS 用於 centralized key management

**Why AWS KMS**:

- Managed service 降低s 營運開銷
- FIPS 140-2 Level 2 compliance
- Integrated 與 AWS services
- Automatic key rotation
- Comprehensive audit logging
- Cost-effective 用於 our scale

**Why Multi-Layer**:

- Defense in depth
- Compliance requirements (PCI-DSS 需要field-level encryption)
- Granular control over sensitive data
- Performance optimization (encrypt only what's necessary)

**Encryption Strategy 透過 Data Type**:

| Data Type | Encryption Method | Key Type | Rotation |
|-----------|------------------|----------|----------|
| Payment Card Data | AES-256 field-level | AWS KMS CMK | 90 天 |
| PII (email, phone) | AES-256 field-level | AWS KMS CMK | 90 天 |
| Passwords | BCrypt (cost 12) | N/A | N/A |
| Session Tokens | N/A (short-lived) | N/A | N/A |
| Business Data | RDS encryption | AWS-managed | Annual |
| File Storage | S3 SSE-KMS | AWS KMS CMK | 90 天 |
| Backups | EBS/S3 encryption | AWS KMS CMK | 90 天 |

## 影響分析

### 利害關係人影響

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | High | Implement encryption logic | Training, libraries, code examples |
| Operations Team | Medium | Monitor encryption, key rotation | Automation, runbooks, alerts |
| Security Team | Positive | Enhanced security posture | Regular audits |
| End Users | None | Transparent to users | N/A |
| Compliance | Positive | Meet regulatory requirements | Compliance documentation |
| Performance | Low | < 10ms overhead | Caching, optimization |

### 影響半徑

**選擇的影響半徑**： **System**

影響：

- All services handling sensitive data
- Database schema (encrypted columns)
- File storage (S3 encryption)
- Infrastructure (KMS setup)
- Deployment (key management)
- Monitoring (encryption metrics)

### 風險評估

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| Key loss/corruption | Low | Critical | Multi-region key backup, AWS KMS durability |
| Performance degradation | Medium | Medium | Caching, selective encryption, optimization |
| KMS API rate limits | Low | Medium | Envelope encryption, caching, request batching |
| Key rotation downtime | Low | High | Zero-downtime rotation process, dual-key period |
| Compliance audit failure | Low | High | Regular audits, automated compliance checks |

**整體風險等級**： **Low**

## 實作計畫

### 第 1 階段： Infrastructure Setup （第 1 週）

- [x] Create AWS KMS Customer Managed Keys (CMKs)
- [x] Configure key policies 和 IAM roles
- [x] 啟用 CloudTrail logging 用於 KMS
- [x] Set up key rotation (90-day schedule)
- [x] Configure multi-region key replication
- [x] Create key aliases 用於 different data types

### 第 2 階段： Data in Transit Encryption （第 2 週）

- [x] 啟用 TLS 1.3 用於 all API endpoints
- [x] Configure HTTPS-only (HSTS headers)
- [x] 啟用 TLS 用於 database connections
- [x] 啟用 TLS 用於 Redis connections
- [x] 啟用 TLS 用於 Kafka connections
- [x] Configure certificate management (ACM)

### 第 3 階段： Data at Rest - Infrastructure Level （第 3 週）

- [x] 啟用 RDS encryption 與 KMS
- [x] 啟用 S3 bucket encryption (SSE-KMS)
- [x] 啟用 EBS volume encryption
- [x] 啟用 ElastiCache encryption
- [x] 啟用 MSK encryption
- [x] Verify encryption status

### 第 4 階段： Data at Rest - Application Level （第 4-5 週）

- [x] Implement encryption service using AWS KMS
- [x] Implement envelope encryption 用於 performance
- [x] Add JPA AttributeConverter 用於 encrypted fields
- [x] Encrypt payment card data fields
- [x] Encrypt PII fields (email, phone, address)
- [x] Implement key caching (5-minute TTL)
- [x] Add encryption audit logging

### Phase 5: Testing and Validation （第 6 週）

- [x] Unit tests 用於 encryption/decryption
- [x] Integration tests 與 KMS
- [x] Performance testing (< 10ms overhead)
- [x] Key rotation testing
- [x] Disaster recovery testing
- [x] Compliance validation (PCI-DSS, GDPR)

### 回滾策略

**觸發條件**：

- Performance degradation > 20ms per operation
- KMS availability issues
- Data corruption 期間 encryption
- Compliance audit failures

**回滾步驟**：

1. Disable application-level encryption
2. Revert to infrastructure-level encryption only
3. Investigate 和 fix issues
4. Re-啟用 與 fixes

**回滾時間**： < 2 hours

## 監控和成功標準

### 成功指標

- ✅ Encryption overhead < 10ms (95th percentile)
- ✅ Zero data breaches
- ✅ 100% sensitive data encrypted
- ✅ Key rotation success rate 100%
- ✅ PCI-DSS compliance achieved
- ✅ GDPR compliance achieved
- ✅ KMS API error rate < 0.1%

### 監控計畫

**CloudWatch Metrics**:

- `kms.encrypt.time` (histogram)
- `kms.decrypt.time` (histogram)
- `kms.api.errors` (count)
- `kms.api.throttles` (count)
- `encryption.cache.hit_rate` (gauge)
- `key.rotation.success` (count)

**告警**：

- KMS API error rate > 1% 用於 5 minutes
- Encryption latency > 20ms 用於 5 minutes
- Key rotation failures
- KMS API throttling
- Encryption cache hit rate < 80%

**Security Monitoring**:

- KMS key usage patterns
- Unauthorized key access attempts
- Key policy changes
- Encryption/decryption failures

**審查時程**：

- Daily: Check encryption metrics
- Weekly: Review KMS audit logs
- Monthly: Key rotation verification
- Quarterly: Compliance audit

## 後果

### 正面後果

- ✅ **Compliance**: Meet PCI-DSS, GDPR, local regulations
- ✅ **Security**: Defense in depth 與 multi-layer encryption
- ✅ **Key Management**: Automated, auditable key lifecycle
- ✅ **Performance**: < 10ms overhead 與 caching
- ✅ **Scalability**: Envelope encryption 降低s KMS calls
- ✅ **Operational**: Managed service 降低s overhead
- ✅ **Audit Trail**: Complete encryption operation history
- ✅ **Disaster Recovery**: Multi-region key replication

### 負面後果

- ⚠️ **複雜的ity**: Additional encryption layer to manage
- ⚠️ **成本**： $1,500/month for KMS and API calls
- ⚠️ **Vendor Lock-in**: AWS KMS dependency
- ⚠️ **Performance**: Small overhead 用於 encryption operations
- ⚠️ **Development**: Additional code 用於 encryption logic

### 技術債務

**已識別債務**：

1. No client-side encryption 用於 highly sensitive data (acceptable 用於 now)
2. Manual encryption key selection (acceptable 與 clear guidelines)
3. No homomorphic encryption 用於 searchable encrypted data (not needed yet)

**債務償還計畫**：

- **Q2 2026**: Evaluate client-side encryption 用於 payment data
- **Q3 2026**: Implement automated encryption key selection
- **Q4 2026**: Evaluate searchable encryption if needed

## 相關決策

- [ADR-001: Use PostgreSQL 用於 Primary Database](001-use-postgresql-for-primary-database.md) - Database encryption
- [ADR-014: JWT-Based Authentication Strategy](014-jwt-based-authentication-strategy.md) - Token security
- [ADR-054: Data Loss Prevention (DLP) Strategy](054-data-loss-prevention-strategy.md) - Data protection integration

## 備註

### Encryption Implementation

```java
@Service
public class EncryptionService {
    
    private final AWSKMS kmsClient;
    private final Cache<String, DataKey> keyCache;
    
    public String encrypt(String plaintext, String keyId) {
        // 1. Get data key from cache or generate new
        DataKey dataKey = keyCache.get(keyId, () -> generateDataKey(keyId));
        
        // 2. Encrypt plaintext with data key (AES-256-GCM)
        byte[] ciphertext = aesEncrypt(plaintext, dataKey.getPlaintext());
        
        // 3. Return encrypted data key + ciphertext
        return Base64.encode(dataKey.getEncrypted() + ciphertext);
    }
    
    public String decrypt(String encrypted, String keyId) {
        // 1. Extract encrypted data key and ciphertext
        byte[] encryptedDataKey = extractDataKey(encrypted);
        byte[] ciphertext = extractCiphertext(encrypted);
        
        // 2. Decrypt data key with KMS
        byte[] plaintextDataKey = kmsClient.decrypt(encryptedDataKey);
        
        // 3. Decrypt ciphertext with data key
        return aesDecrypt(ciphertext, plaintextDataKey);
    }
}

// JPA AttributeConverter for transparent encryption
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {
    
    @Autowired
    private EncryptionService encryptionService;
    
    @Override
    public String convertToDatabaseColumn(String attribute) {
        return encryptionService.encrypt(attribute, "customer-data-key");
    }
    
    @Override
    public String convertToEntityAttribute(String dbData) {
        return encryptionService.decrypt(dbData, "customer-data-key");
    }
}

// Usage in entities
@Entity
public class Customer {
    
    @Id
    private String id;
    
    @Column
    @Convert(converter = EncryptedStringConverter.class)
    private String email; // Encrypted at rest
    
    @Column
    @Convert(converter = EncryptedStringConverter.class)
    private String phone; // Encrypted at rest
    
    @Column
    private String hashedPassword; // BCrypt hashed, not encrypted
}
```

### TLS Configuration

```yaml
# application.yml
server:
  ssl:
    enabled: true
    protocol: TLS
    enabled-protocols: TLSv1.3
    ciphers:

      - TLS_AES_256_GCM_SHA384
      - TLS_AES_128_GCM_SHA256

    key-store: ${SSL_KEYSTORE_PATH}
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12

# Force HTTPS
security:
  require-ssl: true
  
# HSTS headers
spring:
  security:
    headers:
      hsts:
        enabled: true
        max-age: 31536000
        include-subdomains: true
```

### Key Rotation Process

1. **Automatic Rotation** (AWS KMS):
   - KMS automatically rotates CMKs every 90 天
   - Old key versions retained 用於 decryption
   - No application changes needed

2. **Manual Rotation** (if needed):
   - Create new CMK
   - Update application configuration
   - Re-encrypt data 與 new key (background job)
   - Deprecate old key after re-encryption complete

3. **Zero-Downtime Rotation**:
   - Dual-key period (7 天)
   - Both old 和 new keys active
   - Gradual migration to new key
   - Verify all data re-encrypted
   - Deactivate old key

### Compliance Mapping

**PCI-DSS Requirements**:

- Requirement 3.4: Render PAN unreadable ✅ (AES-256 encryption)
- Requirement 3.5: Protect keys ✅ (AWS KMS)
- Requirement 3.6: Key management ✅ (Automated rotation)
- Requirement 4.1: Encrypt transmission ✅ (TLS 1.3)

**GDPR Requirements**:

- Article 32: Security of processing ✅ (Encryption at rest 和 in transit)
- Article 33: Breach notification ✅ (Monitoring 和 alerting)

---

**文檔狀態**： ✅ Accepted  
**上次審查**： 2025-10-25  
**下次審查**： 2026-01-25 （每季）
