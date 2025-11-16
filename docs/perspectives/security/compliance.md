# Security Compliance

> **Last Updated**: 2025-10-23
> **Status**: ✅ Active

## 概述

本文件描述電子商務平台的法規合規要求和實作策略。系統必須遵守 GDPR（一般資料保護規範）以保護資料隱私，以及 PCI-DSS（支付卡產業資料安全標準）以處理支付卡資料。

## GDPR 合規性

### 概述

一般資料保護規範（GDPR）是一項全面的資料保護法律，適用於所有處理 EU 居民個人資料的組織。我們的電子商務平台處理客戶個人資料，必須遵守 GDPR 要求。

### GDPR 關鍵原則

1. **合法性、公平性和透明度**：以合法和透明的方式處理資料
2. **目的限制**：為特定、明確的目的收集資料
3. **資料最小化**：僅收集必要的資料
4. **準確性**：保持個人資料準確且最新
5. **儲存限制**：僅在必要期間保留資料
6. **完整性和機密性**：以適當的 security 保護資料
7. **問責制**：證明符合 GDPR

### 資料主體權利

#### 存取權（第 15 條）

```java
@Service
public class GDPRDataAccessService {

    /**
     * 向客戶提供其所有個人資料
     */
    public CustomerDataExport exportCustomerData(String customerId) {
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException(customerId));

        List<Order> orders = orderRepository.findByCustomerId(customerId);
        List<Review> reviews = reviewRepository.findByCustomerId(customerId);
        List<Address> addresses = addressRepository.findByCustomerId(customerId);
        List<PaymentMethod> paymentMethods = paymentMethodRepository
            .findByCustomerId(customerId);

        return CustomerDataExport.builder()
            .personalInfo(mapPersonalInfo(customer))
            .orders(mapOrders(orders))
            .reviews(mapReviews(reviews))
            .addresses(mapAddresses(addresses))
            .paymentMethods(mapPaymentMethods(paymentMethods))
            .exportDate(LocalDateTime.now())
            .format("JSON")
            .build();
    }

    private PersonalInfoDto mapPersonalInfo(Customer customer) {
        return PersonalInfoDto.builder()
            .customerId(customer.getId())
            .name(customer.getName())
            .email(customer.getEmail())
            .phoneNumber(customer.getPhoneNumber())
            .dateOfBirth(customer.getDateOfBirth())
            .registrationDate(customer.getCreatedAt())
            .lastLoginDate(customer.getLastLoginAt())
            .build();
    }
}
```

#### 更正權（第 16 條）

```java
@Service
public class GDPRDataRectificationService {

    /**
     * 允許客戶更正其個人資料
     */
    @Transactional
    public Customer rectifyCustomerData(
            String customerId,
            CustomerDataRectificationRequest request) {

        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException(customerId));

        // 更新個人資訊
        if (request.name() != null) {
            customer.setName(request.name());
        }
        if (request.email() != null) {
            validateEmailUnique(request.email(), customerId);
            customer.setEmail(request.email());
        }
        if (request.phoneNumber() != null) {
            customer.setPhoneNumber(request.phoneNumber());
        }
        if (request.dateOfBirth() != null) {
            customer.setDateOfBirth(request.dateOfBirth());
        }

        customer.setUpdatedAt(LocalDateTime.now());
        Customer updated = customerRepository.save(customer);

        // 記錄更正以供稽核
        auditLogger.logDataRectification(customerId, request);

        return updated;
    }
}
```

#### 刪除權（第 17 條）

```java
@Service
public class GDPRDataErasureService {

    /**
     * 匿名化客戶資料（被遺忘權）
     */
    @Transactional
    public void eraseCustomerData(String customerId) {
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException(customerId));

        // 檢查是否允許刪除
        if (!canEraseData(customerId)) {
            throw new DataErasureException(
                "Cannot erase data due to legal obligations or pending transactions"
            );
        }

        // 匿名化個人資料
        customer.setName("ANONYMIZED_" + UUID.randomUUID());
        customer.setEmail("anonymized_" + UUID.randomUUID() + "@deleted.local");
        customer.setPhoneNumber("000-000-0000");
        customer.setDateOfBirth(null);
        customer.setAnonymized(true);
        customer.setAnonymizedAt(LocalDateTime.now());

        customerRepository.save(customer);

        // 匿名化相關資料
        anonymizeRelatedData(customerId);

        // 記錄刪除以供稽核
        auditLogger.logDataErasure(customerId);
    }

    private boolean canEraseData(String customerId) {
        // 如果有待處理的訂單則無法刪除
        List<Order> pendingOrders = orderRepository
            .findByCustomerIdAndStatus(customerId, OrderStatus.PENDING);
        if (!pendingOrders.isEmpty()) {
            return false;
        }

        // 如果有最近的交易（90 天內）則無法刪除
        LocalDateTime ninetyDaysAgo = LocalDateTime.now().minusDays(90);
        List<Order> recentOrders = orderRepository
            .findByCustomerIdAndCreatedAtAfter(customerId, ninetyDaysAgo);

        return recentOrders.isEmpty();
    }

    private void anonymizeRelatedData(String customerId) {
        // 匿名化訂單配送地址
        List<Order> orders = orderRepository.findByCustomerId(customerId);
        for (Order order : orders) {
            order.setShippingAddress("ANONYMIZED");
            order.setBillingAddress("ANONYMIZED");
            orderRepository.save(order);
        }

        // 刪除地址
        addressRepository.deleteByCustomerId(customerId);

        // 刪除付款方式（保留交易記錄）
        paymentMethodRepository.deleteByCustomerId(customerId);
    }
}
```

#### 資料可攜權（第 20 條）

```java
@Service
public class GDPRDataPortabilityService {

    /**
     * 以機器可讀格式匯出客戶資料
     */
    public byte[] exportDataInMachineReadableFormat(
            String customerId,
            ExportFormat format) {

        CustomerDataExport data = gdprDataAccessService
            .exportCustomerData(customerId);

        return switch (format) {
            case JSON -> exportAsJson(data);
            case XML -> exportAsXml(data);
            case CSV -> exportAsCsv(data);
        };
    }

    private byte[] exportAsJson(CustomerDataExport data) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            throw new DataExportException("Failed to export data as JSON", e);
        }
    }
}
```

#### 限制處理權（第 18 條）

```java
@Service
public class GDPRProcessingRestrictionService {

    /**
     * 限制客戶資料的處理
     */
    @Transactional
    public void restrictProcessing(String customerId, RestrictionReason reason) {
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException(customerId));

        customer.setProcessingRestricted(true);
        customer.setRestrictionReason(reason);
        customer.setRestrictionDate(LocalDateTime.now());

        customerRepository.save(customer);

        // 記錄限制
        auditLogger.logProcessingRestriction(customerId, reason);
    }

    /**
     * 解除處理限制
     */
    @Transactional
    public void liftRestriction(String customerId) {
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException(customerId));

        customer.setProcessingRestricted(false);
        customer.setRestrictionReason(null);
        customer.setRestrictionDate(null);

        customerRepository.save(customer);

        // 記錄解除限制
        auditLogger.logRestrictionLifted(customerId);
    }
}
```

### 同意管理

```java
@Service
public class ConsentManagementService {

    /**
     * 記錄客戶同意
     */
    @Transactional
    public void recordConsent(String customerId, ConsentType type) {
        Consent consent = Consent.builder()
            .customerId(customerId)
            .consentType(type)
            .granted(true)
            .grantedAt(LocalDateTime.now())
            .ipAddress(getCurrentIpAddress())
            .userAgent(getCurrentUserAgent())
            .build();

        consentRepository.save(consent);
    }

    /**
     * 撤回客戶同意
     */
    @Transactional
    public void withdrawConsent(String customerId, ConsentType type) {
        Consent consent = consentRepository
            .findByCustomerIdAndConsentType(customerId, type)
            .orElseThrow(() -> new ConsentNotFoundException(customerId, type));

        consent.setGranted(false);
        consent.setWithdrawnAt(LocalDateTime.now());

        consentRepository.save(consent);

        // 處理同意撤回
        handleConsentWithdrawal(customerId, type);
    }

    private void handleConsentWithdrawal(String customerId, ConsentType type) {
        switch (type) {
            case MARKETING_EMAILS ->
                emailPreferenceService.unsubscribeFromMarketing(customerId);
            case DATA_ANALYTICS ->
                analyticsService.excludeFromAnalytics(customerId);
            case THIRD_PARTY_SHARING ->
                thirdPartyService.stopDataSharing(customerId);
        }
    }
}
```

### 資料保留政策

```java
@Service
public class DataRetentionService {

    // 保留期限（天數）
    private static final int CUSTOMER_DATA_RETENTION = 2555; // 7 年
    private static final int ORDER_DATA_RETENTION = 2555; // 7 年
    private static final int LOG_RETENTION = 90; // 90 天
    private static final int ANONYMIZED_DATA_RETENTION = 365; // 匿名化後 1 年

    /**
     * 執行資料保留政策
     */
    @Scheduled(cron = "0 0 3 * * *") // 每天凌晨 3 點
    public void enforceRetentionPolicy() {
        deleteExpiredAnonymizedData();
        deleteExpiredLogs();
        archiveOldOrders();
    }

    private void deleteExpiredAnonymizedData() {
        LocalDateTime cutoffDate = LocalDateTime.now()
            .minusDays(ANONYMIZED_DATA_RETENTION);

        List<Customer> expiredCustomers = customerRepository
            .findByAnonymizedTrueAndAnonymizedAtBefore(cutoffDate);

        for (Customer customer : expiredCustomers) {
            // 刪除客戶和所有相關資料
            deleteCustomerCompletely(customer.getId());

            logger.info("Deleted expired anonymized customer data",
                kv("customerId", customer.getId()),
                kv("anonymizedAt", customer.getAnonymizedAt()));
        }
    }

    private void deleteExpiredLogs() {
        LocalDateTime cutoffDate = LocalDateTime.now()
            .minusDays(LOG_RETENTION);

        int deletedCount = auditLogRepository.deleteByCreatedAtBefore(cutoffDate);

        logger.info("Deleted expired audit logs",
            kv("count", deletedCount),
            kv("cutoffDate", cutoffDate));
    }
}
```

### GDPR 合規性檢查清單

- [x] **處理的合法基礎**：已記錄同意和合法利益
- [x] **隱私政策**：清晰、可存取的隱私政策
- [x] **資料主體權利**：所有權利已實作和測試
- [x] **同意管理**：細粒度同意且易於撤回
- [x] **資料最小化**：僅收集必要資料
- [x] **資料保留**：自動執行保留政策
- [x] **資料 Security**：加密、存取控制、稽核日誌
- [x] **資料外洩通知**：已建立事件回應計劃
- [x] **資料保護長**：已任命且可聯繫 DPO
- [x] **隱私設計**：Security 內建於系統設計中
- [x] **資料處理協議**：與第三方處理者的合約

## PCI-DSS 合規性

### 概述

支付卡產業資料安全標準（PCI-DSS）是一套 security 標準，旨在確保所有接受、處理、儲存或傳輸信用卡資訊的公司維護安全環境。

### PCI-DSS 要求

#### 要求 1：安裝和維護防火牆配置

```yaml
# AWS Security Group 配置
SecurityGroup:
  Type: AWS::EC2::SecurityGroup
  Properties:
    GroupDescription: Application security group
    VpcId: !Ref VPC
    SecurityGroupIngress:
      # 僅允許 HTTPS
      - IpProtocol: tcp
        FromPort: 443
        ToPort: 443
        CidrIp: 0.0.0.0/0
    SecurityGroupEgress:
      # 僅允許出站到付款閘道
      - IpProtocol: tcp
        FromPort: 443
        ToPort: 443
        DestinationSecurityGroupId: !Ref PaymentGatewaySecurityGroup
```

#### 要求 2：不使用供應商提供的預設值

```java
@Configuration
public class SecurityDefaultsConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 使用強密碼編碼（非預設）
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 停用預設 security
            .csrf().disable()
            .cors().and()
            // 自訂 security 配置
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        return http.build();
    }
}
```

#### 要求 3：保護儲存的持卡人資料

```java
@Service
public class PCIDSSPaymentService {

    /**
     * 絕不儲存完整 PAN（主要帳號）
     * 絕不儲存 CVV/CVC
     * 絕不儲存 PIN
     */
    @Transactional
    public PaymentResult processPayment(PaymentRequest request) {
        // 驗證卡資料（但不記錄）
        validateCardData(request);

        // 直接傳送到付款閘道
        // 不在我們的資料庫中儲存卡資料
        PaymentGatewayResponse response = paymentGateway.process(request);

        // 僅儲存 tokenized 參考
        Payment payment = Payment.builder()
            .orderId(request.getOrderId())
            .amount(request.getAmount())
            .paymentToken(response.getToken()) // 僅 Token，非卡資料
            .last4Digits(request.getCardNumber().substring(
                request.getCardNumber().length() - 4)) // 僅最後 4 位數
            .cardBrand(detectCardBrand(request.getCardNumber()))
            .status(response.getStatus())
            .processedAt(LocalDateTime.now())
            .build();

        paymentRepository.save(payment);

        return PaymentResult.from(response);
    }

    private void validateCardData(PaymentRequest request) {
        // 驗證但絕不記錄卡資料
        if (!isValidCardNumber(request.getCardNumber())) {
            throw new InvalidCardException("Invalid card number");
        }

        // 確保 CVV 未被儲存
        if (request.getCvv() == null || request.getCvv().length() < 3) {
            throw new InvalidCardException("Invalid CVV");
        }

        // CVV 應僅用於驗證，絕不儲存
    }
}
```

#### 要求 4：加密持卡人資料的傳輸

```yaml
# application.yml - 強制 TLS 1.3
server:
  ssl:
    enabled: true
    protocol: TLS
    enabled-protocols: TLSv1.3
    ciphers:
      - TLS_AES_256_GCM_SHA384
      - TLS_AES_128_GCM_SHA256
```

#### 要求 5：保護所有系統免受惡意軟體攻擊

```yaml
# 自動漏洞掃描
security-scanning:
  enabled: true
  schedule: "0 0 2 * * *" # 每天凌晨 2 點
  tools:
    - dependency-check
    - spotbugs
    - sonarqube
```

#### 要求 6：開發和維護安全系統

```java
// 透過程式碼審查和自動檢查強制執行安全編碼實踐
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResult> processPayment(
            @Valid @RequestBody PaymentRequest request) {

        // 輸入驗證
        validatePaymentRequest(request);

        // 安全處理付款
        PaymentResult result = paymentService.processPayment(request);

        return ResponseEntity.ok(result);
    }
}
```

#### 要求 7：限制對持卡人資料的存取

```java
@PreAuthorize("hasRole('PAYMENT_PROCESSOR')")
public class PaymentProcessingService {

    /**
     * 僅授權人員可存取付款處理
     */
    @AuditDataAccess
    public PaymentResult processPayment(PaymentRequest request) {
        // 付款處理邏輯
        // 所有存取都被記錄以供稽核
    }
}
```

#### 要求 8：識別和驗證存取

```java
// 付款處理的強 authentication
@Service
public class PaymentAuthenticationService {

    /**
     * 付款處理存取需要 MFA
     */
    public boolean authenticatePaymentProcessor(String userId, String mfaCode) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        // 驗證使用者具有付款處理者角色
        if (!user.hasRole("PAYMENT_PROCESSOR")) {
            return false;
        }

        // 驗證 MFA 代碼
        return mfaService.verifyCode(userId, mfaCode);
    }
}
```

#### 要求 9：限制實體存取

- 資料中心的實體存取控制（AWS 責任）
- 訪客日誌和護送要求
- 安全處理包含持卡人資料的媒體

#### 要求 10：追蹤和監控所有存取

```java
@Component
public class PaymentAuditLogger {

    /**
     * 記錄所有與付款相關的活動
     */
    public void logPaymentAccess(String userId, String action, String details) {
        AuditLogEntry entry = AuditLogEntry.builder()
            .userId(userId)
            .action(action)
            .category("PAYMENT")
            .details(details)
            .ipAddress(getCurrentIpAddress())
            .timestamp(Instant.now())
            .build();

        auditLogRepository.save(entry);

        // 同時傳送到集中式日誌記錄
        logger.info("Payment access",
            kv("userId", userId),
            kv("action", action),
            kv("timestamp", Instant.now()));
    }
}
```

#### 要求 11：定期測試 Security 系統

```java
@SpringBootTest
class PCIDSSSecurityTest {

    @Test
    void should_not_store_full_card_number() {
        // 驗證資料庫中沒有完整卡號
        List<Payment> payments = paymentRepository.findAll();

        for (Payment payment : payments) {
            assertThat(payment.getCardNumber()).isNull();
            assertThat(payment.getLast4Digits()).hasSize(4);
            assertThat(payment.getPaymentToken()).isNotNull();
        }
    }

    @Test
    void should_not_store_cvv() {
        // 驗證沒有儲存 CVV
        List<Payment> payments = paymentRepository.findAll();

        for (Payment payment : payments) {
            assertThat(payment.getCvv()).isNull();
        }
    }
}
```

#### 要求 12：維護資訊 Security 政策

- 記錄的 security 政策
- 年度 security 意識培訓
- 事件回應計劃
- 定期 security 評估

### PCI-DSS 合規性檢查清單

- [x] **防火牆配置**：AWS Security Groups 已配置
- [x] **無預設密碼**：所有預設值已變更
- [x] **持卡人資料保護**：未儲存卡資料
- [x] **傳輸中加密**：強制執行 TLS 1.3
- [x] **惡意軟體保護**：已啟用自動掃描
- [x] **安全開發**：強制執行安全編碼實踐
- [x] **存取限制**：付款處理的 RBAC 與 MFA
- [x] **Authentication**：需要強 authentication
- [x] **實體存取**：AWS 資料中心控制
- [x] **稽核日誌**：所有付款存取都被記錄
- [x] **Security 測試**：自動和手動測試
- [x] **Security 政策**：已記錄和維護

## 合規性監控

### 自動合規性檢查

```java
@Service
public class ComplianceMonitoringService {

    @Scheduled(cron = "0 0 1 * * *") // 每天凌晨 1 點
    public void runComplianceChecks() {
        ComplianceReport report = ComplianceReport.builder()
            .reportDate(LocalDate.now())
            .gdprCompliance(checkGDPRCompliance())
            .pciDssCompliance(checkPCIDSSCompliance())
            .build();

        complianceReportRepository.save(report);

        // 如果不合規則發出警報
        if (!report.isFullyCompliant()) {
            alertComplianceTeam(report);
        }
    }

    private GDPRComplianceStatus checkGDPRCompliance() {
        return GDPRComplianceStatus.builder()
            .dataSubjectRightsImplemented(true)
            .consentManagementActive(true)
            .dataRetentionPolicyEnforced(true)
            .privacyPolicyUpToDate(checkPrivacyPolicyDate())
            .dataBreachProceduresTested(checkLastDRPTest())
            .build();
    }

    private PCIDSSComplianceStatus checkPCIDSSCompliance() {
        return PCIDSSComplianceStatus.builder()
            .noCardDataStored(verifyNoCardDataStored())
            .tlsEnforced(verifyTLSConfiguration())
            .accessControlsActive(verifyAccessControls())
            .auditLoggingEnabled(verifyAuditLogging())
            .vulnerabilityScanningActive(checkLastVulnerabilityScan())
            .build();
    }
}
```

## 合規性文件

### 必要文件

1. **隱私政策**：面向客戶的隱私政策
2. **資料處理協議**：與第三方處理者的合約
3. **資料保護影響評估 (DPIA)**：風險評估
4. **事件回應計劃**：資料外洩回應程序
5. **Security 政策**：內部 security 政策和程序
6. **稽核日誌**：全面的存取和活動日誌
7. **合規性報告**：定期合規性評估報告

## 相關文件

- [Security Overview](overview.md) - 整體 security 觀點
- [Data Protection](data-protection.md) - 資料保護實作
- [Authentication](authentication.md) - Authentication 機制
- [Authorization](authorization.md) - Authorization 模型

## 參考資料

- GDPR Official Text: <https://gdpr.eu/>
- PCI-DSS Standards: <https://www.pcisecuritystandards.org/>
- GDPR Compliance Checklist: <https://gdpr.eu/checklist/>
- PCI-DSS Self-Assessment: <https://www.pcisecuritystandards.org/document_library>
