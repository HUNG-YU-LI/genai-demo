# 資料保護

> **最後更新**: 2025-10-23
> **狀態**: ✅ 啟用中

## 概述

本文件描述電子商務平台中使用的資料保護機制,以確保敏感資料的機密性、完整性和隱私性。資料保護包括靜態加密和傳輸加密、資料遮罩、安全資料處理以及遵守資料保護法規。

## 資料分類

### 敏感度等級

#### 高度敏感 (Level 1)

- **支付卡資料** (PAN, CVV, 到期日)
- **驗證憑證** (密碼, API keys)
- **個人識別號碼** (SSN, 護照號碼)
- **財務資訊** (銀行帳號)

**保護要求**:

- ✅ 靜態加密 (AES-256)
- ✅ 傳輸加密 (TLS 1.3)
- ✅ 存取記錄
- ✅ PCI-DSS 合規
- ✅ 嚴格存取控制

#### 敏感 (Level 2)

- **個人識別資訊 (PII)**: Email, 電話號碼, 地址
- **訂單歷史和購買資料**
- **使用者偏好和行為資料**
- **商業機密資料**

**保護要求**:

- ✅ 靜態加密
- ✅ 傳輸加密
- ✅ 非生產環境資料遮罩
- ✅ GDPR 合規
- ✅ 存取控制

#### 內部 (Level 3)

- **產品目錄資料**
- **公開評論和評分**
- **彙總分析資料**
- **系統日誌 (非敏感)**

**保護要求**:

- ✅ 傳輸加密
- ✅ 存取控制
- ⚠️ 靜態加密 (可選)

#### 公開 (Level 4)

- **產品描述和圖片**
- **公開行銷內容**
- **公開 API 文件**

**保護要求**:

- ✅ 完整性保護
- ⚠️ 加密 (可選)

## 靜態加密

### Database 加密

#### Field-Level Encryption

```java
/**
 * JPA Converter for encrypting sensitive fields
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final AESEncryptionService encryptionService;

    public EncryptedStringConverter(AESEncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return encryptionService.encrypt(attribute);
        } catch (Exception e) {
            throw new EncryptionException("Failed to encrypt data", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return encryptionService.decrypt(dbData);
        } catch (Exception e) {
            throw new DecryptionException("Failed to decrypt data", e);
        }
    }
}
```

#### Entity with Encrypted Fields

```java
@Entity
@Table(name = "customers")
public class Customer {

    @Id
    private String id;

    @Column(name = "name")
    private String name;

    @Column(name = "email")
    @Convert(converter = EncryptedStringConverter.class)
    private String email; // Encrypted in database

    @Column(name = "phone_number")
    @Convert(converter = EncryptedStringConverter.class)
    private String phoneNumber; // Encrypted in database

    @Column(name = "address")
    @Convert(converter = EncryptedStringConverter.class)
    private String address; // Encrypted in database

    @Column(name = "password_hash")
    private String passwordHash; // Already hashed with BCrypt

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
```

### AES Encryption Service

```java
@Service
public class AESEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final SecretKey secretKey;

    public AESEncryptionService(@Value("${encryption.key}") String base64Key) {
        byte[] decodedKey = Base64.getDecoder().decode(base64Key);
        this.secretKey = new SecretKeySpec(decodedKey, "AES");
    }

    /**
     * Encrypt data using AES-256-GCM
     */
    public String encrypt(String plaintext) throws Exception {
        byte[] iv = generateIV();

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        // Combine IV and ciphertext
        byte[] combined = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * Decrypt data using AES-256-GCM
     */
    public String decrypt(String encrypted) throws Exception {
        byte[] combined = Base64.getDecoder().decode(encrypted);

        // Extract IV and ciphertext
        byte[] iv = new byte[GCM_IV_LENGTH];
        byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, iv.length);
        System.arraycopy(combined, iv.length, ciphertext, 0, ciphertext.length);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

        byte[] plaintext = cipher.doFinal(ciphertext);
        return new String(plaintext, StandardCharsets.UTF_8);
    }

    private byte[] generateIV() {
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        return iv;
    }
}
```

### Key Management

#### AWS Secrets Manager Integration

```java
@Configuration
public class KeyManagementConfiguration {

    @Bean
    public AESEncryptionService encryptionService(
            @Value("${aws.secretsmanager.secret-name}") String secretName) {

        AWSSecretsManager client = AWSSecretsManagerClientBuilder.standard()
            .withRegion(Regions.AP_NORTHEAST_1)
            .build();

        GetSecretValueRequest request = new GetSecretValueRequest()
            .withSecretId(secretName);

        GetSecretValueResult result = client.getSecretValue(request);
        String secret = result.getSecretString();

        // Parse JSON to get encryption key
        JsonNode secretJson = objectMapper.readTree(secret);
        String encryptionKey = secretJson.get("encryption-key").asText();

        return new AESEncryptionService(encryptionKey);
    }
}
```

#### Key Rotation Strategy

```java
@Service
public class KeyRotationService {

    private final AESEncryptionService currentKeyService;
    private final AESEncryptionService previousKeyService;
    private final CustomerRepository customerRepository;

    /**
     * Rotate encryption keys for all encrypted data
     */
    @Scheduled(cron = "0 0 2 1 */3 *") // Every 3 months at 2 AM
    public void rotateKeys() {
        logger.info("Starting key rotation process");

        List<Customer> customers = customerRepository.findAll();

        for (Customer customer : customers) {
            try {
                // Decrypt with old key
                String email = previousKeyService.decrypt(customer.getEmail());
                String phone = previousKeyService.decrypt(customer.getPhoneNumber());

                // Re-encrypt with new key
                customer.setEmail(currentKeyService.encrypt(email));
                customer.setPhoneNumber(currentKeyService.encrypt(phone));

                customerRepository.save(customer);
            } catch (Exception e) {
                logger.error("Failed to rotate keys for customer: {}",
                    customer.getId(), e);
            }
        }

        logger.info("Key rotation completed");
    }
}
```

## 傳輸加密

### TLS Configuration

```yaml
# application.yml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: ecommerce-platform
    protocol: TLS
    enabled-protocols: TLSv1.3
  port: 8443

# Force HTTPS redirect
security:
  require-ssl: true
```

### TLS Security Configuration

```java
@Configuration
public class TLSConfiguration {

    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
            @Override
            protected void postProcessContext(Context context) {
                SecurityConstraint securityConstraint = new SecurityConstraint();
                securityConstraint.setUserConstraint("CONFIDENTIAL");
                SecurityCollection collection = new SecurityCollection();
                collection.addPattern("/*");
                securityConstraint.addCollection(collection);
                context.addConstraint(securityConstraint);
            }
        };

        tomcat.addAdditionalTomcatConnectors(redirectConnector());
        return tomcat;
    }

    private Connector redirectConnector() {
        Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
        connector.setScheme("http");
        connector.setPort(8080);
        connector.setSecure(false);
        connector.setRedirectPort(8443);
        return connector;
    }
}
```

### Security Headers

```java
@Configuration
public class SecurityHeadersConfiguration {

    @Bean
    public SecurityFilterChain securityHeaders(HttpSecurity http) throws Exception {
        http.headers(headers -> headers
            .contentTypeOptions(contentTypeOptions -> contentTypeOptions.disable())
            .xssProtection(xss -> xss.disable())
            .cacheControl(cache -> cache.disable())
            .httpStrictTransportSecurity(hsts -> hsts
                .includeSubDomains(true)
                .maxAgeInSeconds(31536000)
                .preload(true))
            .frameOptions(frame -> frame.deny())
            .contentSecurityPolicy(csp -> csp
                .policyDirectives("default-src 'self'; " +
                    "script-src 'self' 'unsafe-inline'; " +
                    "style-src 'self' 'unsafe-inline'; " +
                    "img-src 'self' data: https:; " +
                    "font-src 'self' data:; " +
                    "connect-src 'self'"))
            .referrerPolicy(referrer -> referrer
                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            .permissionsPolicy(permissions -> permissions
                .policy("geolocation=(), microphone=(), camera=()"))
        );

        return http.build();
    }
}
```

## 資料遮罩

### Masking Service

```java
@Service
public class DataMaskingService {

    /**
     * Mask email address
     * Example: john.doe@example.com -> j*******e@example.com
     */
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

        return localPart.charAt(0)
            + "*".repeat(localPart.length() - 2)
            + localPart.charAt(localPart.length() - 1)
            + "@" + domain;
    }

    /**
     * Mask phone number
     * Example: +1-555-123-4567 -> +1-***-***-4567
     */
    public String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return phoneNumber;
        }

        String lastFour = phoneNumber.substring(phoneNumber.length() - 4);
        String masked = "*".repeat(phoneNumber.length() - 4);
        return masked + lastFour;
    }

    /**
     * Mask credit card number
     * Example: 4532-1234-5678-9010 -> ****-****-****-9010
     */
    public String maskCreditCard(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return cardNumber;
        }

        String lastFour = cardNumber.substring(cardNumber.length() - 4);
        String prefix = cardNumber.substring(0, cardNumber.length() - 4);
        String masked = prefix.replaceAll("[0-9]", "*");
        return masked + lastFour;
    }

    /**
     * Mask address
     * Example: 123 Main Street, Apt 4B -> *** Main Street, ***
     */
    public String maskAddress(String address) {
        if (address == null) {
            return null;
        }

        // Mask street number and apartment number
        return address.replaceAll("\\d+", "***");
    }
}
```

### Automatic Masking for Non-Production

```java
@Component
@Profile("!production")
public class DataMaskingAspect {

    private final DataMaskingService maskingService;

    /**
     * Automatically mask sensitive data in non-production environments
     */
    @Around("@annotation(MaskSensitiveData)")
    public Object maskData(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();

        if (result instanceof CustomerDto dto) {
            dto.setEmail(maskingService.maskEmail(dto.getEmail()));
            dto.setPhoneNumber(maskingService.maskPhoneNumber(dto.getPhoneNumber()));
            dto.setAddress(maskingService.maskAddress(dto.getAddress()));
        }

        return result;
    }
}
```

## 資料匿名化

### GDPR Right to Erasure

```java
@Service
public class DataAnonymizationService {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;

    /**
     * Anonymize customer data for GDPR compliance
     */
    @Transactional
    public void anonymizeCustomer(String customerId) {
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException(customerId));

        // Anonymize personal data
        customer.setName("ANONYMIZED_" + UUID.randomUUID());
        customer.setEmail("anonymized_" + UUID.randomUUID() + "@deleted.local");
        customer.setPhoneNumber("000-000-0000");
        customer.setAddress("ANONYMIZED");
        customer.setAnonymized(true);
        customer.setAnonymizedAt(LocalDateTime.now());

        customerRepository.save(customer);

        // Anonymize related orders
        List<Order> orders = orderRepository.findByCustomerId(customerId);
        for (Order order : orders) {
            order.setShippingAddress("ANONYMIZED");
            order.setBillingAddress("ANONYMIZED");
            orderRepository.save(order);
        }

        logger.info("Customer data anonymized",
            kv("customerId", customerId),
            kv("timestamp", LocalDateTime.now()));
    }

    /**
     * Check if customer can be anonymized
     */
    public boolean canAnonymize(String customerId) {
        // Cannot anonymize if there are pending orders
        List<Order> pendingOrders = orderRepository
            .findByCustomerIdAndStatus(customerId, OrderStatus.PENDING);

        if (!pendingOrders.isEmpty()) {
            return false;
        }

        // Cannot anonymize if there are recent transactions (within 90 days)
        LocalDateTime ninetyDaysAgo = LocalDateTime.now().minusDays(90);
        List<Order> recentOrders = orderRepository
            .findByCustomerIdAndCreatedAtAfter(customerId, ninetyDaysAgo);

        return recentOrders.isEmpty();
    }
}
```

## 安全資料處理

### Input Sanitization

```java
@Component
public class InputSanitizationService {

    private final PolicyFactory htmlPolicy;

    public InputSanitizationService() {
        this.htmlPolicy = Sanitizers.FORMATTING
            .and(Sanitizers.LINKS)
            .and(Sanitizers.BLOCKS);
    }

    /**
     * Sanitize HTML input to prevent XSS
     */
    public String sanitizeHtml(String input) {
        if (input == null) {
            return null;
        }
        return htmlPolicy.sanitize(input);
    }

    /**
     * Escape special characters for SQL
     */
    public String escapeSql(String input) {
        if (input == null) {
            return null;
        }
        return input.replace("'", "''")
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
    }

    /**
     * Remove potentially dangerous characters
     */
    public String sanitizeFilename(String filename) {
        if (filename == null) {
            return null;
        }
        // Remove path traversal attempts and special characters
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
```

### Output Encoding

```java
@Component
public class OutputEncodingService {

    /**
     * Encode for HTML context
     */
    public String encodeForHtml(String input) {
        if (input == null) {
            return null;
        }
        return StringEscapeUtils.escapeHtml4(input);
    }

    /**
     * Encode for JavaScript context
     */
    public String encodeForJavaScript(String input) {
        if (input == null) {
            return null;
        }
        return StringEscapeUtils.escapeEcmaScript(input);
    }

    /**
     * Encode for JSON context
     */
    public String encodeForJson(String input) {
        if (input == null) {
            return null;
        }
        return StringEscapeUtils.escapeJson(input);
    }
}
```

## 資料保留和刪除

### Retention Policy

```java
@Service
public class DataRetentionService {

    private static final int CUSTOMER_DATA_RETENTION_DAYS = 2555; // 7 years
    private static final int ORDER_DATA_RETENTION_DAYS = 2555; // 7 years
    private static final int LOG_RETENTION_DAYS = 90; // 90 days

    /**
     * Delete data that exceeds retention period
     */
    @Scheduled(cron = "0 0 3 * * *") // Daily at 3 AM
    public void enforceRetentionPolicy() {
        deleteExpiredCustomerData();
        deleteExpiredOrderData();
        deleteExpiredLogs();
    }

    private void deleteExpiredCustomerData() {
        LocalDateTime cutoffDate = LocalDateTime.now()
            .minusDays(CUSTOMER_DATA_RETENTION_DAYS);

        List<Customer> expiredCustomers = customerRepository
            .findByAnonymizedTrueAndAnonymizedAtBefore(cutoffDate);

        for (Customer customer : expiredCustomers) {
            customerRepository.delete(customer);
            logger.info("Deleted expired customer data",
                kv("customerId", customer.getId()));
        }
    }

    private void deleteExpiredOrderData() {
        LocalDateTime cutoffDate = LocalDateTime.now()
            .minusDays(ORDER_DATA_RETENTION_DAYS);

        List<Order> expiredOrders = orderRepository
            .findByCompletedAtBeforeAndStatusIn(
                cutoffDate,
                List.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED)
            );

        orderRepository.deleteAll(expiredOrders);
    }

    private void deleteExpiredLogs() {
        LocalDateTime cutoffDate = LocalDateTime.now()
            .minusDays(LOG_RETENTION_DAYS);

        auditLogRepository.deleteByCreatedAtBefore(cutoffDate);
    }
}
```

## 合規性

### GDPR Compliance

```java
@Service
public class GDPRComplianceService {

    /**
     * Handle GDPR data subject access request
     */
    public CustomerDataExport exportCustomerData(String customerId) {
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException(customerId));

        List<Order> orders = orderRepository.findByCustomerId(customerId);
        List<Review> reviews = reviewRepository.findByCustomerId(customerId);

        return CustomerDataExport.builder()
            .personalInfo(customer)
            .orders(orders)
            .reviews(reviews)
            .exportDate(LocalDateTime.now())
            .build();
    }

    /**
     * Handle GDPR right to rectification
     */
    public void rectifyCustomerData(String customerId, CustomerDataUpdate update) {
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException(customerId));

        customer.updatePersonalInfo(update);
        customerRepository.save(customer);

        auditLogger.logDataRectification(customerId, update);
    }

    /**
     * Handle GDPR right to erasure
     */
    public void eraseCustomerData(String customerId) {
        if (!dataAnonymizationService.canAnonymize(customerId)) {
            throw new DataErasureException(
                "Cannot erase data due to pending obligations"
            );
        }

        dataAnonymizationService.anonymizeCustomer(customerId);
        auditLogger.logDataErasure(customerId);
    }
}
```

### PCI-DSS Compliance

```java
@Service
public class PCIDSSComplianceService {

    /**
     * Ensure payment card data is never stored
     */
    public PaymentResult processPayment(PaymentRequest request) {
        // Validate card data format
        validateCardData(request);

        // Send to payment gateway immediately
        // NEVER store card data in our database
        PaymentGatewayResponse response = paymentGateway.process(request);

        // Store only tokenized reference
        Payment payment = Payment.builder()
            .orderId(request.getOrderId())
            .amount(request.getAmount())
            .paymentToken(response.getToken()) // Tokenized reference only
            .status(response.getStatus())
            .build();

        paymentRepository.save(payment);

        return PaymentResult.from(response);
    }

    private void validateCardData(PaymentRequest request) {
        // Validate but never log card data
        if (!isValidCardNumber(request.getCardNumber())) {
            throw new InvalidCardException("Invalid card number");
        }

        // Ensure CVV is not stored
        if (request.getCvv() != null) {
            logger.warn("CVV should not be sent to backend");
        }
    }
}
```

## 監控和稽核

### Data Access Logging

```java
@Aspect
@Component
public class DataAccessAuditAspect {

    private final AuditLogRepository auditLogRepository;

    /**
     * Log all access to sensitive data
     */
    @Around("@annotation(AuditDataAccess)")
    public Object auditDataAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        String userId = SecurityContextHolder.getContext()
            .getAuthentication().getName();

        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        AuditLogEntry entry = AuditLogEntry.builder()
            .userId(userId)
            .action("DATA_ACCESS")
            .resource(methodName)
            .parameters(Arrays.toString(args))
            .timestamp(Instant.now())
            .build();

        try {
            Object result = joinPoint.proceed();
            entry.setStatus("SUCCESS");
            return result;
        } catch (Exception e) {
            entry.setStatus("FAILURE");
            entry.setErrorMessage(e.getMessage());
            throw e;
        } finally {
            auditLogRepository.save(entry);
        }
    }
}
```

## 測試

### Encryption Tests

```java
@SpringBootTest
class EncryptionTest {

    @Autowired
    private AESEncryptionService encryptionService;

    @Test
    void should_encrypt_and_decrypt_data_correctly() throws Exception {
        String plaintext = "sensitive-data@example.com";

        String encrypted = encryptionService.encrypt(plaintext);
        String decrypted = encryptionService.decrypt(encrypted);

        assertThat(encrypted).isNotEqualTo(plaintext);
        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    void should_produce_different_ciphertext_for_same_plaintext() throws Exception {
        String plaintext = "test@example.com";

        String encrypted1 = encryptionService.encrypt(plaintext);
        String encrypted2 = encryptionService.encrypt(plaintext);

        // Different IV should produce different ciphertext
        assertThat(encrypted1).isNotEqualTo(encrypted2);

        // But both should decrypt to same plaintext
        assertThat(encryptionService.decrypt(encrypted1)).isEqualTo(plaintext);
        assertThat(encryptionService.decrypt(encrypted2)).isEqualTo(plaintext);
    }
}
```

## 最佳實務

### ✅ 應該做

- 對所有 PII 和敏感資料進行靜態加密
- 對所有傳輸中的資料使用 TLS 1.3
- 對高度敏感資料實作欄位級加密
- 定期輪換加密金鑰 (每 90 天)
- 使用強加密演算法 (AES-256-GCM)
- 在日誌和錯誤訊息中遮罩敏感資料
- 實作資料保留和刪除政策
- 稽核所有敏感資料存取
- 遵守 GDPR 和 PCI-DSS 要求

### ❌ 不應該做

- 不要儲存支付卡資料 (使用 tokenization)
- 不要記錄敏感資料 (密碼, tokens, PII)
- 不要使用弱加密演算法 (DES, MD5)
- 不要將加密金鑰寫死在程式碼中
- 不要在錯誤訊息中暴露敏感資料
- 不要在非生產環境中跳過加密
- 不要忘記為 GDPR 合規匿名化資料

## 相關文件

- [Security Overview](overview.md) - 整體安全性視角
- [Authentication](authentication.md) - 驗證機制
- [Compliance](compliance.md) - 法規合規細節

## 參考資料

- GDPR: <https://gdpr.eu/>
- PCI-DSS: <https://www.pcisecuritystandards.org/>
- OWASP Cryptographic Storage: <https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html>
- AWS Encryption: <https://docs.aws.amazon.com/encryption-sdk/latest/developer-guide/>
