---
inclusion: always
---

# 程式碼審查標準與指南

## 程式碼審查流程

### 審查工作流程

#### Pull Request 需求

- [ ] **標題**: 遵循格式的清晰、描述性標題：`[TYPE] Brief description`
  - 類型：`FEAT`、`FIX`、`REFACTOR`、`DOCS`、`TEST`、`CHORE`
- [ ] **描述**: 詳細解釋變更內容及原因
- [ ] **連結問題**: 參考相關的 issues 或 user stories
- [ ] **測試**: 測試證據（unit tests、手動測試結果）
- [ ] **破壞性變更**: 如有任何變更需清楚記錄
- [ ] **截圖**: 對於 UI 變更，包含變更前後的截圖

#### 審查分配規則

- **最少審查者**: 需要 2 位審查者
- **必需審查者**:
  - 至少 1 位資深開發人員
  - 受影響區域的領域專家
  - 安全性相關變更的安全審查者
- **審查時程**: 審查必須在 24 小時內完成
- **自我審查**: 作者必須先審查自己的 PR

### 審查檢查清單

#### 功能需求

- [ ] **業務邏輯**: 程式碼正確實作需求
- [ ] **邊緣案例**: 適當處理邊緣案例和錯誤條件
- [ ] **輸入驗證**: 所有輸入都經過適當驗證
- [ ] **輸出正確性**: 輸出符合預期格式和內容
- [ ] **整合**: 與現有系統的適當整合

#### 程式碼品質

- [ ] **可讀性**: 程式碼清晰且自我說明
- [ ] **可維護性**: 程式碼易於修改和擴展
- [ ] **複雜度**: 方法和類別不過於複雜
- [ ] **命名**: 變數、方法和類別具有有意義的名稱
- [ ] **註解**: 複雜邏輯有適當註解

#### 架構和設計

- [ ] **設計模式**: 使用適當的設計模式
- [ ] **SOLID 原則**: 程式碼遵循 SOLID 原則
- [ ] **DDD 合規性**: 遵循 domain-driven design 原則
- [ ] **層級分離**: 跨層級的適當關注點分離
- [ ] **依賴**: 依賴被適當管理和注入

## 程式碼品質標準

> **📋 基本標準**: 基礎的程式碼品質標準請參考 Development Standards

> **🎯 用途**: 本節提供詳細的程式碼審查檢查清單和範例

### 命名慣例

#### Java 命名標準

```java
// ✅ 好: 清晰、描述性名稱
public class CustomerRegistrationService {

    private final CustomerRepository customerRepository;
    private final EmailNotificationService emailNotificationService;

    public Customer registerNewCustomer(CustomerRegistrationRequest request) {
        validateRegistrationRequest(request);

        Customer customer = createCustomerFromRequest(request);
        Customer savedCustomer = customerRepository.save(customer);

        sendWelcomeEmail(savedCustomer);

        return savedCustomer;
    }

    private void validateRegistrationRequest(CustomerRegistrationRequest request) {
        if (isEmailAlreadyRegistered(request.getEmail())) {
            throw new EmailAlreadyRegisteredException(request.getEmail());
        }
    }
}

// ❌ 壞: 不清楚、縮寫名稱
public class CustRegSvc {
    private final CustRepo repo;
    private final EmailSvc emailSvc;

    public Cust regCust(CustRegReq req) {
        // Unclear what this method does
        validate(req);
        Cust c = create(req);
        Cust saved = repo.save(c);
        sendEmail(saved);
        return saved;
    }
}
```

#### 方法命名指南

```java
// ✅ 好: 動詞-名詞模式，意圖清晰
public boolean isCustomerEligibleForDiscount(Customer customer)
public void sendOrderConfirmationEmail(Order order)
public List<Product> findProductsByCategory(Category category)
public void validatePaymentInformation(PaymentInfo paymentInfo)

// ❌ 壞: 意圖不清楚，語法差
public boolean customerDiscount(Customer customer)
public void email(Order order)
public List<Product> products(Category category)
public void payment(PaymentInfo paymentInfo)
```

### 程式碼結構標準

#### 方法長度和複雜度

```java
// ✅ 好: 簡短、專注的方法
public void processOrder(Order order) {
    validateOrder(order);
    calculateOrderTotal(order);
    applyDiscounts(order);
    reserveInventory(order);
    processPayment(order);
    sendConfirmation(order);
}

private void validateOrder(Order order) {
    if (order == null) {
        throw new IllegalArgumentException("Order cannot be null");
    }
    if (order.getItems().isEmpty()) {
        throw new BusinessRuleViolationException("Order must contain at least one item");
    }
}

// ❌ 壞: 過長、複雜的方法做太多事
public void processOrder(Order order) {
    // 50+ lines of mixed validation, calculation, and processing logic
    if (order != null && !order.getItems().isEmpty()) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : order.getItems()) {
            if (item.getQuantity() > 0 && item.getProduct() != null) {
                BigDecimal itemTotal = item.getProduct().getPrice().multiply(new BigDecimal(item.getQuantity()));
                if (item.getProduct().getCategory().equals("PREMIUM")) {
                    // Complex discount calculation logic...
                }
                total = total.add(itemTotal);
            }
        }
        // More complex logic continues...
    }
}
```

#### 類別設計標準

```java
// ✅ 好: 單一職責，目的清晰
@Service
@Transactional
public class OrderProcessingService {

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final NotificationService notificationService;

    public Order processOrder(ProcessOrderCommand command) {
        Order order = createOrderFromCommand(command);

        reserveInventory(order);
        processPayment(order);
        saveOrder(order);
        sendNotifications(order);

        return order;
    }

    // Additional focused methods...
}

// ❌ 壞: 多重職責，目的不清楚
@Service
public class OrderService {
    // Handles orders, customers, products, payments, notifications, reports...
    // 500+ lines of mixed responsibilities
}
```

### 錯誤處理標準

#### Exception 處理最佳實踐

```java
// ✅ 好: 具體的 exceptions，適當的錯誤上下文
@Service
public class CustomerService {

    public Customer findCustomerById(String customerId) {
        try {
            return customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(
                    "Customer not found with ID: " + customerId));
        } catch (DataAccessException e) {
            logger.error("Database error while fetching customer: {}", customerId, e);
            throw new CustomerServiceException("Unable to retrieve customer data", e);
        }
    }

    public Customer updateCustomer(String customerId, UpdateCustomerRequest request) {
        Customer customer = findCustomerById(customerId);

        try {
            validateUpdateRequest(request);
            updateCustomerFields(customer, request);
            return customerRepository.save(customer);
        } catch (ValidationException e) {
            logger.warn("Invalid update request for customer {}: {}", customerId, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error updating customer {}", customerId, e);
            throw new CustomerServiceException("Failed to update customer", e);
        }
    }
}

// ❌ 壞: 泛型 exceptions，錯誤處理不佳
@Service
public class CustomerService {

    public Customer findCustomerById(String customerId) {
        try {
            return customerRepository.findById(customerId).get(); // Can throw NoSuchElementException
        } catch (Exception e) {
            throw new RuntimeException("Error"); // Too generic
        }
    }
}
```

## 安全性審查標準

### 安全性檢查清單

- [ ] **輸入驗證**: 所有使用者輸入都經過驗證和清理
- [ ] **SQL Injection**: 使用參數化查詢
- [ ] **XSS 預防**: 輸出經過適當編碼
- [ ] **認證**: 適當的認證機制已就位
- [ ] **授權**: 存取控制正確實作
- [ ] **敏感資料**: 日誌或錯誤訊息中沒有敏感資料
- [ ] **加密**: 敏感資料在靜態和傳輸中都已加密

#### 安全性程式碼範例

```java
// ✅ 好: 適當的輸入驗證和參數化查詢
@RestController
public class CustomerController {

    @PostMapping("/customers")
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request) {

        // Input validation is handled by @Valid annotation
        String sanitizedName = htmlSanitizer.sanitize(request.getName());

        CreateCustomerCommand command = new CreateCustomerCommand(
            sanitizedName,
            request.getEmail(),
            passwordEncoder.encode(request.getPassword())
        );

        Customer customer = customerService.createCustomer(command);
        return ResponseEntity.ok(CustomerResponse.from(customer));
    }
}

@Repository
public class CustomerRepository {

    // ✅ 好: 參數化查詢防止 SQL injection
    @Query("SELECT c FROM Customer c WHERE c.email = :email AND c.status = :status")
    Optional<Customer> findByEmailAndStatus(@Param("email") String email, @Param("status") String status);
}

// ❌ 壞: 沒有輸入驗證，潛在的 SQL injection
@RestController
public class CustomerController {

    @PostMapping("/customers")
    public ResponseEntity<CustomerResponse> createCustomer(@RequestBody Map<String, String> request) {
        // No validation, direct use of user input
        String query = "SELECT * FROM customers WHERE email = '" + request.get("email") + "'";
        // SQL injection vulnerability
    }
}
```

## 效能審查標準

### 效能檢查清單

- [ ] **資料庫查詢**: 有效率的查詢並有適當索引
- [ ] **N+1 問題**: 沒有 N+1 查詢問題
- [ ] **快取**: 適當使用快取機制
- [ ] **延遲載入**: 對大型資料集適當使用延遲載入
- [ ] **記憶體使用**: 沒有記憶體洩漏或過度記憶體使用
- [ ] **非同步處理**: 長時間執行的操作是非同步的

#### 效能程式碼範例

```java
// ✅ 好: 有效率的查詢使用 JOIN FETCH
@Repository
public class OrderRepository {

    @Query("SELECT o FROM Order o JOIN FETCH o.items JOIN FETCH o.customer WHERE o.id = :orderId")
    Optional<Order> findByIdWithDetails(@Param("orderId") String orderId);

    @Query("SELECT o FROM Order o WHERE o.customerId = :customerId ORDER BY o.orderDate DESC")
    Page<Order> findByCustomerId(@Param("customerId") String customerId, Pageable pageable);
}

@Service
@CacheConfig(cacheNames = "customers")
public class CustomerService {

    @Cacheable(key = "#customerId")
    public Customer findById(String customerId) {
        return customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }
}

// ❌ 壞: N+1 查詢問題
@Service
public class OrderService {

    public List<OrderSummary> getOrderSummaries(String customerId) {
        List<Order> orders = orderRepository.findByCustomerId(customerId);

        return orders.stream()
            .map(order -> {
                // This causes N+1 queries - one for each order's items
                List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
                return new OrderSummary(order, items);
            })
            .collect(Collectors.toList());
    }
}
```

## 測試審查標準

### 測試品質檢查清單

- [ ] **測試覆蓋率**: 新程式碼有足夠的測試覆蓋率（>80%）
- [ ] **測試類型**: 適當混合 unit、integration 和 E2E tests
- [ ] **測試命名**: 清晰、描述性的測試方法名稱
- [ ] **測試結構**: 測試遵循 Given-When-Then 結構
- [ ] **測試資料**: 測試使用適當的測試資料和 builders
- [ ] **斷言**: 有意義的斷言驗證預期行為
- [ ] **邊緣案例**: 測試涵蓋邊緣案例和錯誤條件

#### 測試程式碼範例

```java
// ✅ 好: 清晰的測試結構和有意義的斷言
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void should_create_customer_and_send_welcome_email_when_valid_request_provided() {
        // Given
        CreateCustomerCommand command = new CreateCustomerCommand(
            "John Doe",
            "john@example.com",
            "encodedPassword"
        );

        Customer expectedCustomer = Customer.builder()
            .id("customer-123")
            .name("John Doe")
            .email("john@example.com")
            .build();

        when(customerRepository.save(any(Customer.class))).thenReturn(expectedCustomer);

        // When
        Customer result = customerService.createCustomer(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("John Doe");
        assertThat(result.getEmail()).isEqualTo("john@example.com");

        verify(customerRepository).save(any(Customer.class));
        verify(emailService).sendWelcomeEmail("john@example.com", "John Doe");
    }

    @Test
    void should_throw_exception_when_email_already_exists() {
        // Given
        CreateCustomerCommand command = new CreateCustomerCommand(
            "John Doe",
            "existing@example.com",
            "encodedPassword"
        );

        when(customerRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> customerService.createCustomer(command))
            .isInstanceOf(EmailAlreadyExistsException.class)
            .hasMessage("Email already exists: existing@example.com");

        verify(customerRepository, never()).save(any(Customer.class));
        verify(emailService, never()).sendWelcomeEmail(anyString(), anyString());
    }
}

// ❌ 壞: 不清楚的測試結構和弱斷言
@Test
void testCreateCustomer() {
    Customer customer = customerService.createCustomer(command);
    assertThat(customer).isNotNull(); // Too weak assertion
}
```

## 文件審查標準

### 文件檢查清單

- [ ] **API 文件**: 公開 APIs 有適當文件
- [ ] **程式碼註解**: 複雜邏輯用註解解釋
- [ ] **README 更新**: 重大變更時更新 README
- [ ] **架構文件**: 架構決策已記錄
- [ ] **遷移指南**: 破壞性變更包含遷移指南

#### 文件範例

```java
/**
 * Service for managing customer lifecycle operations.
 *
 * This service handles customer registration, profile updates, and account management.
 * It integrates with the email service for notifications and maintains audit trails
 * for all customer operations.
 *
 * @author Development Team
 * @since 1.0
 */
@Service
@Transactional
public class CustomerService {

    /**
     * Creates a new customer account with the provided information.
     *
     * This method performs the following operations:
     * 1. Validates the customer information
     * 2. Checks for duplicate email addresses
     * 3. Creates the customer record
     * 4. Sends a welcome email
     * 5. Records the registration event
     *
     * @param command the customer creation command containing all required information
     * @return the created customer with generated ID and timestamps
     * @throws EmailAlreadyExistsException if the email is already registered
     * @throws ValidationException if the customer information is invalid
     */
    public Customer createCustomer(CreateCustomerCommand command) {
        // Implementation with inline comments for complex logic

        // Complex business rule that needs explanation
        if (isHighRiskRegistration(command)) {
            // High-risk registrations require additional verification
            // This includes customers from certain regions or with specific patterns
            scheduleAdditionalVerification(command);
        }

        return customer;
    }
}
```

## 審查回饋指南

### 提供建設性回饋

#### 回饋類別

- **必須修正**: 阻擋合併的關鍵問題
- **應該修正**: 應該處理的重要問題
- **考慮**: 改進建議
- **細節**: 次要的風格或偏好問題
- **讚賞**: 對良好實踐的正面回饋

#### 回饋範例

```markdown
## 必須修正

- **安全問題**: 第 45 行有 SQL injection 漏洞。使用參數化查詢。
- **Bug**: 第 23 行可能出現 null pointer exception。新增 null 檢查。

## 應該修正

- **效能**: `getOrderSummaries()` 中有 N+1 查詢問題。考慮使用 JOIN FETCH。
- **錯誤處理**: 第 67 行的泛型 exception 處理。使用特定 exceptions。

## 考慮

- **設計**: 考慮將此邏輯提取到單獨的服務中以獲得更好的關注點分離。
- **可讀性**: 此方法相當長。考慮將其拆分為較小的方法。

## 細節

- **風格**: 考慮使用更具描述性的變數名稱（例如，`customerList` 而非 `list`）。

## 讚賞

- **良好實踐**: 出色地使用 builder pattern 建立測試資料。
- **乾淨程式碼**: 結構良好的方法，具有清晰的單一職責。
```

### 回應回饋

#### 作者回應指南

- **確認**: 確認所有回饋，即使您不同意
- **解釋**: 必要時提供決策的上下文
- **提問**: 如果回饋不清楚，請要求澄清
- **開放**: 對建議和替代方法保持開放態度
- **更新**: 根據有效回饋更新程式碼

#### 回應範例

```markdown
## 回應回饋

**@reviewer1 關於 SQL injection 問題:**
好發現！我已更新查詢以使用參數化查詢。請參閱 commit abc123。

**@reviewer2 關於方法長度:**
我理解對方法長度的擔憂。我已將驗證邏輯提取到單獨的方法中。
但是，我將主要流程保持在一起，因為它代表單一業務交易。您覺得如何？

**@reviewer3 關於變數命名:**
感謝建議。我已更新變數名稱以更具描述性。
```

## 審查指標和品質關卡

### 品質關卡

- **程式碼覆蓋率**: 新程式碼最低 80% 行覆蓋率
- **複雜度**: 每個方法的循環複雜度 ≤ 10
- **重複**: 沒有超過 5 行的程式碼重複
- **安全性**: 沒有高或嚴重的安全漏洞
- **效能**: 沒有效能衰退

### 審查指標

- **審查時間**: 完成審查的平均時間
- **回饋品質**: 每次審查發現的問題數量
- **返工率**: 需要大量返工的 PRs 百分比
- **批准率**: 首次審查即批准的 PRs 百分比

## 審查工具和自動化

### 自動化檢查

```yaml
# GitHub Actions workflow for automated checks
name: Code Review Automation

on:
  pull_request:
    branches: [ main, develop ]

jobs:
  code-quality:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Run SonarQube Analysis
        uses: sonarqube-quality-gate-action@master

      - name: Run Security Scan
        uses: securecodewarrior/github-action-add-sarif@v1

      - name: Check Test Coverage
        run: ./gradlew jacocoTestReport

      - name: Verify Performance Benchmarks
        run: ./gradlew performanceTest
```

### 審查檢查清單自動化

```markdown
## 自動化 PR 檢查清單

- [ ] 所有測試通過
- [ ] 程式碼覆蓋率 ≥ 80%
- [ ] 沒有安全漏洞
- [ ] 沒有效能衰退
- [ ] 文件已更新
- [ ] 破壞性變更已記錄
- [ ] 提供遷移指南（如需要）
```

這個全面的程式碼審查標準確保一致、高品質的程式碼審查，在維護我們的開發標準的同時，培養協作和學習導向的環境。
