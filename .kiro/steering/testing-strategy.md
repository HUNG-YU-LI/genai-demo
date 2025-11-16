# 測試 Strategy

## 概覽

本文件定義了此專案的測試策略和標準，遵循測試金字塔方法和 BDD/TDD 實踐。

**目的**：提供明確的測試實作和執行規則。
**詳細指南**：參見 `.kiro/examples/testing/` 以取得完整的測試指南。

---

## Test Pyramid

### 分布

- **Unit Tests (80%)**：< 50ms，< 5MB per test
- **Integration Tests (15%)**：< 500ms，< 50MB per test
- **E2E Tests (5%)**：< 3s，< 500MB per test

### 必須遵循

- [ ] 大部分測試應為 unit tests
- [ ] Integration tests 用於基礎設施
- [ ] 最少的 E2E tests 用於關鍵路徑
- [ ] 所有測試都應自動化

---

## 測試分類

### Unit Tests（首選）

#### 何時使用

- 孤立測試 domain logic
- 驗證 business rules
- 測試 utility functions
- 驗證計算

#### 必須遵循

- [ ] 使用 `@ExtendWith(MockitoExtension.class)`
- [ ] 不使用 Spring context
- [ ] Mock 外部依賴
- [ ] 快速執行（< 50ms）

#### 範例

```java
@ExtendWith(MockitoExtension.class)
class OrderTest {

    @Test
    void should_throw_exception_when_submitting_empty_order() {
        // Given
        Order order = new Order(customerId, shippingAddress);

        // When & Then
        assertThatThrownBy(() -> order.submit())
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("Cannot submit empty order");
    }
}
```

**詳細指南**：#[[file:../examples/testing/unit-testing-guide.md]]

---

### 整合 Tests

#### 何時使用

- 測試 repository 實作
- 驗證資料庫查詢
- 測試 API endpoints
- 驗證序列化/反序列化

#### 必須遵循

- [ ] 使用 `@DataJpaTest`、`@WebMvcTest` 或 `@JsonTest`
- [ ] 僅使用部分 Spring context
- [ ] 使用測試資料庫（H2）
- [ ] 測試之間清理狀態

#### 範例

```java
@DataJpaTest
@ActiveProfiles("test")
class OrderRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrderRepository repository;

    @Test
    void should_find_orders_by_customer_id() {
        // Given
        Order order = createOrder(customerId);
        entityManager.persistAndFlush(order);

        // When
        List<Order> results = repository.findByCustomerId(customerId);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCustomerId()).isEqualTo(customerId);
    }
}
```

**詳細指南**：#[[file:../examples/testing/integration-testing-guide.md]]

---

### E2E Tests

#### 何時使用

- 測試完整的使用者旅程
- 驗證系統整合
- 關鍵路徑的煙霧測試

#### 必須遵循

- [ ] 使用 `@SpringBootTest(webEnvironment = RANDOM_PORT)`
- [ ] 完整的 Spring context
- [ ] 測試完整的工作流程
- [ ] 最少數量的 E2E tests

#### 範例

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OrderE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void should_complete_order_submission_flow() {
        // Given: Create customer and add items to cart
        // When: Submit order
        // Then: Verify order created, inventory reserved, email sent
    }
}
```

---

## 使用 Cucumber 進行 BDD 測試

### 必須遵循

- [ ] 在實作前編寫 Gherkin scenarios
- [ ] 使用 Given-When-Then 格式
- [ ] 使用通用語言
- [ ] 每個 business rule 一個 scenario

### Gherkin 結構

```gherkin
Feature: Order Submission

  Scenario: Submit order successfully
    Given a customer with ID "CUST-001"
    And the customer has items in shopping cart
    When the customer submits the order
    Then the order status should be "PENDING"
    And an order confirmation email should be sent
    And inventory should be reserved
```

### Step Definitions

```java
@Given("a customer with ID {string}")
public void aCustomerWithId(String customerId) {
    this.customerId = CustomerId.of(customerId);
    this.customer = createCustomer(customerId);
}

@When("the customer submits the order")
public void theCustomerSubmitsTheOrder() {
    submitOrderCommand = new SubmitOrderCommand(orderId);
    orderService.submitOrder(submitOrderCommand);
}

@Then("the order status should be {string}")
public void theOrderStatusShouldBe(String expectedStatus) {
    Order order = orderRepository.findById(orderId).orElseThrow();
    assertThat(order.getStatus().name()).isEqualTo(expectedStatus);
}
```

**詳細指南**：#[[file:../examples/testing/bdd-cucumber-guide.md]]

---

## Test 效能

### 必須遵循

- [ ] 使用 `@TestPerformanceExtension` 進行監控
- [ ] Unit tests：< 50ms，< 5MB
- [ ] Integration tests：< 500ms，< 50MB
- [ ] E2E tests：< 3s，< 500MB
- [ ] 測試後清理資源

### 效能監控

```java
@TestPerformanceExtension(maxExecutionTimeMs = 500, maxMemoryIncreaseMB = 50)
@DataJpaTest
class OrderRepositoryTest {
    // Tests are automatically monitored
}
```

**詳細指南**：#[[file:../examples/testing/test-performance-guide.md]]

---

## 測試資料管理

### 必須遵循

- [ ] 使用 test data builders
- [ ] 創建可重用的 test fixtures
- [ ] 使用有意義的測試資料
- [ ] 測試之間清理狀態

### Test Data Builder

```java
public class OrderTestDataBuilder {
    private OrderId orderId = OrderId.generate();
    private CustomerId customerId = CustomerId.of("CUST-001");

    public static OrderTestDataBuilder anOrder() {
        return new OrderTestDataBuilder();
    }

    public OrderTestDataBuilder withCustomerId(CustomerId customerId) {
        this.customerId = customerId;
        return this;
    }

    public Order build() {
        return new Order(orderId, customerId, "Test Address");
    }
}

// Usage
Order order = anOrder()
    .withCustomerId(customerId)
    .build();
```

---

## 測試組織

### 測試套件結構

```text
test/
├── java/
│   └── solid/humank/genaidemo/
│       ├── domain/          # Unit tests
│       ├── application/     # Application service tests
│       ├── infrastructure/  # Integration tests
│       └── bdd/             # BDD step definitions
└── resources/
    ├── features/            # Gherkin scenarios
    └── application-test.yml # Test configuration
```

### 測試命名

```java
// Unit test
class OrderTest { }

// Integration test
@DataJpaTest
class OrderRepositoryTest { }

// E2E test
@SpringBootTest
class OrderE2ETest { }
```

---

## Gradle 測試指令

### 日常開發

```bash
./gradlew quickTest          # Unit tests only (< 2 min)
./gradlew preCommitTest      # Unit + Integration (< 5 min)
./gradlew fullTest           # All tests including E2E
```

### 特定測試類型

```bash
./gradlew unitTest           # Fast unit tests
./gradlew integrationTest    # Integration tests
./gradlew e2eTest           # End-to-end tests
./gradlew cucumber          # BDD Cucumber tests
```

### 測試報告

```bash
./gradlew test jacocoTestReport              # Coverage report
./gradlew generatePerformanceReport          # Performance report
```

---

## 測試品質標準

### 必須達成

- [ ] 程式碼覆蓋率 > 80%
- [ ] 所有測試通過
- [ ] 無不穩定的測試
- [ ] 測試執行時間在限制內
- [ ] CI/CD 中無跳過的測試

### 測試特性

- [ ] **Fast**：快速回饋
- [ ] **Isolated**：獨立的測試
- [ ] **Repeatable**：每次結果相同
- [ ] **Self-validating**：明確的通過/失敗
- [ ] **Timely**：與程式碼一起編寫

---

## 驗證

### 覆蓋率檢查

```bash
./gradlew test jacocoTestReport
# Check: build/reports/jacoco/test/html/index.html
# Target: > 80% line coverage
```

### 效能檢查

```bash
./gradlew generatePerformanceReport
# Check: build/reports/test-performance/performance-report.html
```

---

## 快速參考

| Test Type | Annotation | Speed | Memory | Use Case |
|-----------|-----------|-------|--------|----------|
| Unit | `@ExtendWith(MockitoExtension.class)` | < 50ms | < 5MB | Domain logic |
| Integration | `@DataJpaTest`, `@WebMvcTest` | < 500ms | < 50MB | Infrastructure |
| E2E | `@SpringBootTest` | < 3s | < 500MB | Complete flows |
| BDD | Cucumber | Varies | Varies | Business scenarios |

---

## 相關文件

- **Core Principles**：#[[file:core-principles.md]]
- **Code Quality Checklist**：#[[file:code-quality-checklist.md]]
- **Testing Examples**：#[[file:../examples/testing/]]

---

**Document Version**：1.0
**Last Updated**：2025-01-17
**Owner**：QA Team
