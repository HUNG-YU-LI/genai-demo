---
adr_number: 006
title: "Environment-Specific Testing Strategy"
date: 2025-10-24
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [002, 004]
affected_viewpoints: ["development"]
affected_perspectives: ["development-resource", "evolution"]
---

# ADR-006: Environment-Specific Testing Strategy

## 狀態

**Accepted** - 2025-10-24

## 上下文

### 問題陳述

The Enterprise E-Commerce Platform 需要comprehensive testing strategy that:

- Ensures high code quality 和 reliability
- 提供s fast feedback 期間 development
- 支援s Test-Driven Development (TDD) 和 Behavior-Driven Development (BDD)
- Balances test coverage 與 execution speed
- Works effectively 與 Hexagonal Architecture (ADR-002)
- 處理s different testing needs 跨 environments
- 維持s test performance standards
- 支援s continuous integration 和 deployment

### 業務上下文

**業務驅動因素**：

- 需要 high-quality, reliable software (99.9% uptime SLA)
- Rapid feature development 沒有 breaking existing functionality
- Regulatory compliance requiring thorough testing
- Team growth from 5 to 20+ developers
- 需要 fast feedback loops (< 5 minutes 用於 pre-commit tests)

**限制條件**：

- Development team has strong Java/JUnit experience
- Limited experience 與 BDD/Cucumber
- CI/CD pipeline 必須 complete in < 15 minutes
- Test execution 必須 not slow down development
- 預算: No additional testing infrastructure costs

### 技術上下文

**目前狀態**：

- Spring Boot 3.4.5 + Java 21
- Hexagonal Architecture 與 clear layer boundaries (ADR-002)
- Domain-Driven Design tactical patterns
- Event-driven architecture (ADR-003)
- PostgreSQL database (ADR-001)

**需求**：

- Test pyramid approach (80% unit, 15% integration, 5% E2E)
- Fast unit tests (< 50ms, < 5MB per test)
- Moderate integration tests (< 500ms, < 50MB per test)
- Comprehensive E2E tests (< 3s, < 500MB per test)
- BDD 支援 用於 business scenarios
- Test performance monitoring
- Environment-specific test configurations

## 決策驅動因素

1. **Fast Feedback**: Developers need quick feedback (< 5 minutes)
2. **Test Pyramid**: Majority of tests 應該 be fast unit tests
3. **Architecture Alignment**: Tests 應該 respect layer boundaries
4. **BDD 支援**: Business scenarios need executable specifications
5. **Performance Standards**: Tests 必須 meet performance requirements
6. **CI/CD Integration**: Tests 必須 run efficiently in pipeline
7. **Developer Experience**: 容易write 和 維持 tests
8. **Cost Efficiency**: No expensive testing infrastructure

## 考慮的選項

### 選項 1： Environment-Specific Testing Strategy with Test Pyramid

**描述**： Layered testing approach with different test types optimized for different purposes

**Test Types**:

```text
Unit Tests (80%):

- @ExtendWith(MockitoExtension.class)
- No Spring context
- Test domain logic in isolation
- < 50ms, < 5MB per test

Integration Tests (15%):

- @DataJpaTest, @WebMvcTest, @JsonTest
- Partial Spring context
- Test infrastructure components
- < 500ms, < 50MB per test

E2E Tests (5%):

- @SpringBootTest(webEnvironment = RANDOM_PORT)
- Full Spring context
- Test complete workflows
- < 3s, < 500MB per test

BDD Tests:

- Cucumber with Gherkin scenarios
- Business-readable specifications
- Executable documentation

```

**優點**：

- ✅ Fast feedback (unit tests run in seconds)
- ✅ Comprehensive coverage at all levels
- ✅ Aligns 與 test pyramid best practices
- ✅ 支援s TDD 和 BDD workflows
- ✅ Clear separation of test types
- ✅ Performance standards enforced
- ✅ Cost-effective (no additional infrastructure)
- ✅ Works well 與 Hexagonal Architecture

**缺點**：

- ⚠️ Multiple test frameworks to learn
- ⚠️ Need to 維持 test performance standards
- ⚠️ BDD learning curve 用於 team

**成本**： $0 (uses existing tools)

**風險**： **Low** - Industry-standard approach

### 選項 2： Integration Tests Only

**描述**： Focus primarily on integration tests with full Spring context

**優點**：

- ✅ Tests real system behavior
- ✅ 簡單的r test setup
- ✅ Catches integration issues

**缺點**：

- ❌ Slow feedback (minutes instead of seconds)
- ❌ 難以isolate failures
- ❌ High resource usage
- ❌ Violates test pyramid principles
- ❌ Poor developer experience

**成本**： $0

**風險**： **High** - Slow tests hurt productivity

### 選項 3： E2E Tests Only

**描述**： Focus on end-to-end tests through UI/API

**優點**：

- ✅ Tests complete user journeys
- ✅ High confidence in system behavior

**缺點**：

- ❌ Very slow feedback (minutes to hours)
- ❌ Brittle 和 hard to 維持
- ❌ 難以debug failures
- ❌ Expensive to run
- ❌ Poor test coverage

**成本**： High (requires test infrastructure)

**風險**： **Critical** - Unsustainable for development

### 選項 4： Manual Testing Only

**描述**： Rely primarily on manual QA testing

**優點**：

- ✅ No test automation effort
- ✅ Flexible exploratory testing

**缺點**：

- ❌ No fast feedback
- ❌ Not repeatable
- ❌ Expensive 和 slow
- ❌ 可以not 支援 CI/CD
- ❌ High risk of regressions

**成本**： High (QA team costs)

**風險**： **Critical** - Unacceptable for modern development

## 決策結果

**選擇的選項**： **Environment-Specific Testing Strategy with Test Pyramid**

### 理由

The environment-specific testing strategy 與 test pyramid被選擇的原因如下：

1. **Fast Feedback**: Unit tests 提供 feedback in seconds, enabling TDD workflow
2. **Comprehensive Coverage**: All layers tested appropriately (domain, infrastructure, API)
3. **Architecture Alignment**: Tests respect Hexagonal Architecture boundaries
4. **Performance Standards**: Clear performance requirements prevent slow tests
5. **BDD 支援**: Cucumber 啟用s business-readable specifications
6. **Cost-Effective**: Uses existing tools 和 infrastructure
7. **Scalable**: Test suite scales 與 codebase 沒有 slowing down
8. **CI/CD Ready**: Fast enough 用於 continuous integration pipeline

**實作策略**：

**Unit Tests (80% of tests)**:

- Test domain logic 沒有 any infrastructure
- Use Mockito 用於 dependencies
- Run in milliseconds
- No Spring context overhead

**Integration Tests (15% of tests)**:

- Test repository implementations 與 @DataJpaTest
- Test REST controllers 與 @WebMvcTest
- Test JSON serialization 與 @JsonTest
- Use partial Spring context 用於 efficiency

**E2E Tests (5% of tests)**:

- Test critical user journeys
- Use full Spring context
- Focus on smoke tests 和 happy paths
- Run less frequently (pre-release)

**BDD Tests (Cross-cutting)**:

- Cucumber scenarios 用於 business requirements
- Executable specifications
- Living documentation
- Run as part of integration test suite

**為何不選 Integration-Only**： Slow feedback kills productivity. Unit tests 提供 instant feedback 用於 TDD.

**為何不選 E2E-Only**： E2E tests are too slow 和 brittle 用於 primary testing strategy.

## 影響分析

### 利害關係人影響

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | High | Need to learn test pyramid 和 BDD | Training, examples, pair programming |
| QA Team | Medium | Shift from manual to automated testing | Training, collaboration 與 developers |
| Architects | Positive | Clear testing standards | Architecture documentation |
| Operations | Low | Automated tests 降低 production issues | N/A |
| Business | Positive | Faster, more reliable releases | N/A |

### 影響半徑

**選擇的影響半徑**： **System**

影響：

- All bounded contexts (testing approach)
- Development workflow (TDD/BDD)
- CI/CD pipeline (test execution)
- Code review process (test coverage requirements)
- Onboarding (testing training)

### 風險評估

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| Team learning curve | High | Medium | Training, examples, pair programming |
| Test performance degradation | Medium | High | Automated performance monitoring, strict standards |
| BDD adoption resistance | Medium | Low | Demonstrate value, start 與 critical scenarios |
| Test maintenance overhead | Medium | Medium | 良好的 test design, regular refactoring |
| Flaky tests | Low | High | Strict isolation, proper cleanup, retry mechanisms |

**整體風險等級**： **Low**

## 實作計畫

### 第 1 階段： Foundation and Training （第 1-2 週）

- [x] Set up test performance monitoring framework
  - Implement @TestPerformanceExtension
  - Create TestPerformanceMonitor
  - Configure performance thresholds

- [x] Create base test classes
  - BaseIntegrationTest 與 common setup
  - Test data builders
  - Test utilities

- [x] Configure Gradle test tasks

  ```bash
  ./gradlew quickTest        # Unit tests only (< 2 min)
  ./gradlew preCommitTest    # Unit + Integration (< 5 min)
  ./gradlew fullTest         # All tests including E2E
  ```

- [x] Conduct team training
  - Test pyramid principles
  - TDD workflow
  - BDD 與 Cucumber
  - Test performance standards

### 第 2 階段： Unit Testing Implementation （第 2-4 週）

- [x] Implement unit tests 用於 domain layer
  - Aggregate root tests
  - Value object tests
  - Domain service tests
  - Event collection tests

- [x] Achieve 80%+ unit test coverage
- [x] Enforce performance standards (< 50ms, < 5MB)
- [x] Set up ArchUnit tests 用於 architecture compliance

### 第 3 階段： Integration Testing Implementation （第 4-6 週）

- [x] Implement repository integration tests
  - @DataJpaTest 用於 JPA repositories
  - Test database queries
  - Test data mapping

- [x] Implement API integration tests
  - @WebMvcTest 用於 controllers
  - Test request/response handling
  - Test validation

- [x] Enforce performance standards (< 500ms, < 50MB)

### 第 4 階段： E2E and BDD Testing （第 6-8 週）

- [ ] Set up Cucumber framework
  - Configure Cucumber 與 Spring Boot
  - Create step definition base classes
  - Set up test data management

- [ ] Implement BDD scenarios
  - Write Gherkin scenarios 用於 key features
  - Implement step definitions
  - Link to requirements

- [ ] Implement E2E tests
  - Critical user journeys
  - Smoke tests
  - Happy path scenarios

### Phase 5: CI/CD Integration （第 8 週）

- [x] Configure GitHub Actions workflows
  - Run quickTest on every push
  - Run preCommitTest on PR
  - Run fullTest before merge

- [x] Set up test reporting
  - JaCoCo coverage reports
  - Test performance reports
  - Cucumber reports

- [x] Configure quality gates
  - Minimum 80% coverage
  - All tests 必須 pass
  - Performance standards met

### 回滾策略

**觸發條件**：

- Test execution time > 15 minutes
- Developer productivity decreases > 30%
- Test maintenance overhead > 20% of development time
- Team unable to adopt after 8 週

**回滾步驟**：

1. Simplify to integration tests only 用於 critical paths
2. 降低 BDD scope to essential scenarios
3. Relax performance standards temporarily
4. 提供 additional training 和 支援
5. Re-evaluate strategy after addressing issues

**回滾時間**： 1 week

## 監控和成功標準

### 成功指標

- ✅ Unit test coverage > 80%
- ✅ Integration test coverage > 60%
- ✅ E2E test coverage 用於 critical paths
- ✅ Test execution time: quickTest < 2 min, preCommitTest < 5 min
- ✅ Test performance standards met (100% compliance)
- ✅ Zero flaky tests
- ✅ BDD scenarios 用於 all user stories
- ✅ Developer satisfaction > 4/5

### 監控計畫

**Test Performance Monitoring**:

```java
@TestPerformanceExtension(maxExecutionTimeMs = 50, maxMemoryIncreaseMB = 5)
@ExtendWith(MockitoExtension.class)
class CustomerUnitTest {
    // Automatically monitored for performance
}
```

**Metrics Tracked**:

- Test execution time per test
- Memory usage per test
- Test success/failure rates
- Coverage percentages
- Flaky test detection

**告警**：

- Test execution time exceeds threshold
- Coverage drops below 80%
- Flaky tests detected
- Performance regression detected

**審查時程**：

- Daily: Check test execution times in CI/CD
- Weekly: Review test coverage reports
- Monthly: Test strategy retrospective
- Quarterly: Performance optimization review

## 後果

### 正面後果

- ✅ **Fast Feedback**: Unit tests 提供 instant feedback 用於 TDD
- ✅ **High Coverage**: Comprehensive testing at all levels
- ✅ **維持able**: Clear test organization 和 standards
- ✅ **Architecture Compliance**: Tests enforce layer boundaries
- ✅ **BDD 支援**: Business-readable specifications
- ✅ **Performance Standards**: Tests remain fast as codebase grows
- ✅ **CI/CD Ready**: Fast enough 用於 continuous integration
- ✅ **Cost-Effective**: No additional infrastructure needed

### 負面後果

- ⚠️ **Learning Curve**: Team needs to learn multiple testing approaches
- ⚠️ **Initial Overhead**: Setting up test infrastructure takes time
- ⚠️ **Maintenance**: Need to 維持 tests alongside code
- ⚠️ **Discipline Required**: 必須 enforce test pyramid 和 performance standards

### 技術債務

**已識別債務**：

1. Some legacy code lacks unit tests (acceptable 期間 migration)
2. BDD scenarios not yet complete 用於 all features (ongoing work)
3. E2E test coverage limited to critical paths (acceptable trade-off)
4. Test data management could be 改善d (future enhancement)

**債務償還計畫**：

- **Q1 2026**: Achieve 90%+ unit test coverage
- **Q2 2026**: Complete BDD scenarios 用於 all user stories
- **Q3 2026**: 改善 test data management 與 builders
- **Q4 2026**: Add E2E tests 用於 additional user journeys

## 相關決策

- [ADR-002: Adopt Hexagonal Architecture](002-adopt-hexagonal-architecture.md) - Testing strategy aligns 與 architecture
- [ADR-004: Event Store Implementation](004-event-store-implementation.md) - In-memory event store 用於 testing

## 備註

### Test Classification

**Unit Tests**:

```java
@ExtendWith(MockitoExtension.class)
class OrderTest {
    
    @Test
    void should_calculate_total_correctly() {
        // Given
        Order order = OrderTestDataBuilder.anOrder()
            .withItem("PROD-1", 2, Money.of(10.00))
            .withItem("PROD-2", 1, Money.of(20.00))
            .build();
        
        // When
        Money total = order.calculateTotal();
        
        // Then
        assertThat(total).isEqualTo(Money.of(40.00));
    }
}
```

**Integration Tests**:

```java
@DataJpaTest
@ActiveProfiles("test")
@TestPerformanceExtension(maxExecutionTimeMs = 500, maxMemoryIncreaseMB = 50)
class OrderRepositoryTest extends BaseIntegrationTest {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Test
    void should_save_and_retrieve_order() {
        // Given
        Order order = createTestOrder();
        
        // When
        orderRepository.save(order);
        Order retrieved = orderRepository.findById(order.getId()).orElseThrow();
        
        // Then
        assertThat(retrieved).isEqualTo(order);
    }
}
```

**E2E Tests**:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPerformanceExtension(maxExecutionTimeMs = 3000, maxMemoryIncreaseMB = 500)
class OrderE2ETest extends BaseIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void should_complete_order_submission_flow() {
        // Given: Customer and products exist
        // When: Submit order through API
        // Then: Order created, inventory reserved, email sent
    }
}
```

**BDD Tests**:

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

### Gradle Test Configuration

```gradle
// Unit tests - fast feedback
tasks.register('unitTest', Test) {
    description = 'Fast unit tests (~5MB, ~50ms each)'
    useJUnitPlatform {
        excludeTags 'integration', 'end-to-end', 'slow'
        includeTags 'unit'
    }
    maxHeapSize = '2g'
    maxParallelForks = Runtime.runtime.availableProcessors()
    forkEvery = 0
}

// Integration tests - pre-commit verification
tasks.register('integrationTest', Test) {
    description = 'Integration tests (~50MB, ~500ms each)'
    useJUnitPlatform {
        includeTags 'integration'
        excludeTags 'end-to-end', 'slow'
    }
    maxHeapSize = '6g'
    maxParallelForks = 1
    forkEvery = 5
    timeout = Duration.ofMinutes(30)
}

// E2E tests - pre-release verification
tasks.register('e2eTest', Test) {
    description = 'End-to-end tests (~500MB, ~3s each)'
    useJUnitPlatform {
        includeTags 'end-to-end'
    }
    maxHeapSize = '8g'
    maxParallelForks = 1
    forkEvery = 2
    timeout = Duration.ofHours(1)
}

// Quick test - daily development
tasks.register('quickTest', Test) {
    dependsOn 'unitTest'
}

// Pre-commit test - before committing
tasks.register('preCommitTest', Test) {
    dependsOn 'unitTest', 'integrationTest'
}

// Full test - before release
tasks.register('fullTest', Test) {
    dependsOn 'unitTest', 'integrationTest', 'e2eTest', 'cucumber'
}
```

### Test Performance Standards

| Test Type | Max Execution Time | Max Memory | Success Rate |
|-----------|-------------------|------------|--------------|
| Unit | 50ms | 5MB | > 99% |
| Integration | 500ms | 50MB | > 95% |
| E2E | 3s | 500MB | > 90% |

### Test Data Builders

```java
public class OrderTestDataBuilder {
    private OrderId orderId = OrderId.generate();
    private CustomerId customerId = CustomerId.of("CUST-001");
    private List<OrderItem> items = new ArrayList<>();
    
    public static OrderTestDataBuilder anOrder() {
        return new OrderTestDataBuilder();
    }
    
    public OrderTestDataBuilder withCustomerId(CustomerId customerId) {
        this.customerId = customerId;
        return this;
    }
    
    public OrderTestDataBuilder withItem(String productId, int quantity, Money price) {
        items.add(new OrderItem(ProductId.of(productId), quantity, price));
        return this;
    }
    
    public Order build() {
        Order order = new Order(orderId, customerId, "Test Address");
        items.forEach(order::addItem);
        return order;
    }
}
```

### CI/CD Integration

```yaml
# .github/workflows/test.yml
name: Test Suite

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  quick-test:
    runs-on: ubuntu-latest
    steps:

      - uses: actions/checkout@v3
      - name: Set up JDK 21

        uses: actions/setup-java@v3
        with:
          java-version: '21'

      - name: Run quick tests

        run: ./gradlew quickTest

      - name: Upload test results

        uses: actions/upload-artifact@v3
        with:
          name: test-results
          path: build/reports/tests/

  pre-commit-test:
    runs-on: ubuntu-latest
    if: github.event_name == 'pull_request'
    steps:

      - uses: actions/checkout@v3
      - name: Set up JDK 21

        uses: actions/setup-java@v3
        with:
          java-version: '21'

      - name: Run pre-commit tests

        run: ./gradlew preCommitTest

      - name: Generate coverage report

        run: ./gradlew jacocoTestReport

      - name: Upload coverage to Codecov

        uses: codecov/codecov-action@v3

  full-test:
    runs-on: ubuntu-latest
    if: github.event_name == 'push' && github.ref == 'refs/heads/main'
    steps:

      - uses: actions/checkout@v3
      - name: Set up JDK 21

        uses: actions/setup-java@v3
        with:
          java-version: '21'

      - name: Run full test suite

        run: ./gradlew fullTest

      - name: Generate performance report

        run: ./gradlew generatePerformanceReport
```

---

**文檔狀態**： ✅ Accepted  
**上次審查**： 2025-10-24  
**下次審查**： 2026-01-24 （每季）
