---
adr_number: 012
title: "BDD 與 Cucumber 用於 Requirements Specification"
date: 2025-10-24
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [006]
affected_viewpoints: ["development"]
affected_perspectives: ["development-resource", "evolution"]
---

# ADR-012: BDD 與 Cucumber 用於 Requirements Specification

## 狀態

**Accepted** - 2025-10-24

## 上下文

### 問題陳述

The Enterprise E-Commerce Platform 需要methodology that:

- Bridges the gap between business requirements 和 technical implementation
- 提供s executable specifications that serve as living documentation
- 啟用s collaboration between business stakeholders 和 developers
- Ensures requirements are testable 和 verifiable
- 支援s Test-Driven Development (TDD) workflow
- 維持s alignment between business goals 和 implementation
- 提供s clear acceptance criteria 用於 features
- 啟用s regression testing of business scenarios

### 業務上下文

**業務驅動因素**：

- 需要 clear, unambiguous requirements
- Requirement 用於 business stakeholder involvement in development
- Compliance requirements 用於 documented business rules
- 需要 living documentation that stays up-to-date
- Reduction of misunderstandings between business 和 technical teams
- 支援 用於 agile development 與 clear acceptance criteria

**限制條件**：

- Team has limited BDD experience
- Business stakeholders have limited technical knowledge
- 必須 integrate 與 existing testing strategy (ADR-006)
- Timeline: 3 個月 to establish BDD practice
- 預算: No additional tooling costs

### 技術上下文

**目前狀態**：

- Spring Boot 3.4.5 + Java 21
- JUnit 5 用於 testing (ADR-006)
- Domain-Driven Design approach (ADR-002)
- Hexagonal Architecture (ADR-002)
- Event-driven architecture (ADR-003)

**需求**：

- Business-readable specifications
- Executable specifications
- Integration 與 CI/CD pipeline
- 支援 用於 multiple languages (English, Chinese)
- Reusable step definitions
- Clear test reporting
- Version control 用於 specifications

## 決策驅動因素

1. **Business Collaboration**: 啟用 non-technical stakeholders to understand tests
2. **Living Documentation**: Specifications that stay current 與 code
3. **Executable Specifications**: Tests that verify business requirements
4. **Ubiquitous Language**: Use domain language in specifications
5. **Test Automation**: Automated execution of business scenarios
6. **維持ability**: 容易update as requirements change
7. **Integration**: Works 與 existing testing framework
8. **成本**： Free and open source

## 考慮的選項

### 選項 1： Cucumber with Gherkin (BDD Framework)

**描述**： BDD framework using Gherkin syntax for business-readable specifications

**優點**：

- ✅ Business-readable Gherkin syntax (Given-When-Then)
- ✅ 優秀的Java整合 (Cucumber-JVM)
- ✅ 支援s multiple languages
- ✅ 大型的 社群和生態系統
- ✅ Integration 與 Spring Boot
- ✅ Reusable step definitions
- ✅ Clear test reports
- ✅ IDE 支援 (IntelliJ, VS Code)
- ✅ CI/CD integration
- ✅ Living documentation generation

**缺點**：

- ⚠️ Learning curve 用於 Gherkin syntax
- ⚠️ 可以 be verbose 用於 簡單的 scenarios
- ⚠️ Requires discipline to 維持

**成本**： $0 (open source)

**風險**： **Low** - Mature, widely adopted

### 選項 2： JBehave

**描述**： Java BDD framework similar to Cucumber

**優點**：

- ✅ Java-native BDD framework
- ✅ Similar to Cucumber
- ✅ 良好的Spring整合

**缺點**：

- ❌ Smaller community than Cucumber
- ❌ Less active development
- ❌ Fewer IDE plugins
- ❌ Less documentation

**成本**： $0

**風險**： **Medium** - Smaller ecosystem

### 選項 3： Spock Framework

**描述**： Groovy-based testing framework with BDD-style syntax

**優點**：

- ✅ Expressive syntax
- ✅ 良好的 用於 unit tests
- ✅ Data-driven testing

**缺點**：

- ❌ Requires Groovy knowledge
- ❌ Not business-readable
- ❌ Less suitable 用於 collaboration 與 non-technical stakeholders
- ❌ Primarily 用於 developers

**成本**： $0

**風險**： **Medium** - Not true BDD

### 選項 4： Plain JUnit with Descriptive Names

**描述**： Use JUnit with very descriptive test method names

**優點**：

- ✅ No additional framework
- ✅ Team already knows JUnit
- ✅ 簡單的

**缺點**：

- ❌ Not business-readable
- ❌ No living documentation
- ❌ No collaboration 與 business stakeholders
- ❌ Doesn't bridge business-technical gap

**成本**： $0

**風險**： **High** - Misses BDD benefits

## 決策結果

**選擇的選項**： **Cucumber with Gherkin (BDD Framework)**

### 理由

Cucumber 與 Gherkin被選擇的原因如下：

1. **Business Collaboration**: Gherkin syntax is readable 透過 non-technical stakeholders
2. **Ubiquitous Language**: Scenarios use domain language from DDD
3. **Living Documentation**: Feature files serve as up-to-date documentation
4. **Executable Specifications**: Scenarios are automated tests
5. **Mature Ecosystem**: 大型的 community, 優秀的 tooling, extensive documentation
6. **Spring Boot Integration**: Seamless integration 與 existing stack
7. **Reusability**: Step definitions 可以 be reused 跨 scenarios
8. **Reporting**: Clear, business-friendly test reports
9. **IDE 支援**: 優秀的 plugins 用於 IntelliJ IDEA 和 VS Code

**實作策略**：

**Gherkin Structure**:

```gherkin
Feature: Order Submission
  As a customer
  I want to submit an order
  So that I can purchase products

  Background:
    Given a customer with ID "CUST-001"
    And the customer has a valid payment method

  Scenario: Submit order successfully
    Given the customer has items in shopping cart:
      | Product ID | Quantity | Price |
      | PROD-001   | 2        | 10.00 |
      | PROD-002   | 1        | 20.00 |
    When the customer submits the order
    Then the order status should be "PENDING"
    And an order confirmation email should be sent
    And inventory should be reserved for:
      | Product ID | Quantity |
      | PROD-001   | 2        |
      | PROD-002   | 1        |

  Scenario: Submit order with insufficient inventory
    Given the customer has items in shopping cart:
      | Product ID | Quantity | Price |
      | PROD-001   | 100      | 10.00 |
    And product "PROD-001" has only 50 units in stock
    When the customer submits the order
    Then the order should be rejected
    And the customer should see error "Insufficient inventory"
```

**Step Definitions**:

```java
@SpringBootTest
@CucumberContextConfiguration
public class OrderStepDefinitions {
    
    @Autowired
    private OrderApplicationService orderService;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    private Customer customer;
    private Order order;
    private Exception thrownException;
    
    @Given("a customer with ID {string}")
    public void aCustomerWithId(String customerId) {
        customer = customerRepository.findById(CustomerId.of(customerId))
            .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }
    
    @Given("the customer has items in shopping cart:")
    public void theCustomerHasItemsInShoppingCart(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps();
        
        for (Map<String, String> row : rows) {
            String productId = row.get("Product ID");
            int quantity = Integer.parseInt(row.get("Quantity"));
            BigDecimal price = new BigDecimal(row.get("Price"));
            
            customer.addToCart(
                ProductId.of(productId),
                quantity,
                Money.of(price)
            );
        }
    }
    
    @When("the customer submits the order")
    public void theCustomerSubmitsTheOrder() {
        try {
            SubmitOrderCommand command = new SubmitOrderCommand(
                customer.getId(),
                customer.getCartItems()
            );
            order = orderService.submitOrder(command);
        } catch (Exception e) {
            thrownException = e;
        }
    }
    
    @Then("the order status should be {string}")
    public void theOrderStatusShouldBe(String expectedStatus) {
        assertThat(order.getStatus().name()).isEqualTo(expectedStatus);
    }
    
    @Then("an order confirmation email should be sent")
    public void anOrderConfirmationEmailShouldBeSent() {
        // Verify email was sent (mock verification or event check)
        verify(emailService).sendOrderConfirmation(
            eq(customer.getEmail()),
            eq(order.getId())
        );
    }
    
    @Then("inventory should be reserved for:")
    public void inventoryShouldBeReservedFor(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps();
        
        for (Map<String, String> row : rows) {
            String productId = row.get("Product ID");
            int quantity = Integer.parseInt(row.get("Quantity"));
            
            verify(inventoryService).reserveInventory(
                eq(ProductId.of(productId)),
                eq(quantity)
            );
        }
    }
    
    @Then("the order should be rejected")
    public void theOrderShouldBeRejected() {
        assertThat(thrownException).isNotNull();
        assertThat(order).isNull();
    }
    
    @Then("the customer should see error {string}")
    public void theCustomerShouldSeeError(String expectedError) {
        assertThat(thrownException.getMessage()).contains(expectedError);
    }
}
```

**為何不選 JBehave**： Smaller community 和 less active development compared to Cucumber.

**為何不選 Spock**： Not business-readable, primarily 用於 developers, doesn't 啟用 business collaboration.

**為何不選 Plain JUnit**： Misses the key benefit of BDD - collaboration 與 business stakeholders through executable specifications.

## 影響分析

### 利害關係人影響

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Business Stakeholders | High | 可以 read 和 validate requirements | Training on Gherkin, workshops |
| Product Owners | High | Write acceptance criteria in Gherkin | Training, templates, examples |
| Developers | High | Write step definitions 和 scenarios | Training, pair programming |
| QA Team | High | Use scenarios 用於 testing | Training, test automation guides |
| Architects | Medium | Ensure scenarios align 與 architecture | Review process, guidelines |

### 影響半徑

**選擇的影響半徑**： **System**

影響：

- Requirements gathering process
- Development workflow (TDD/BDD)
- Testing strategy
- Documentation approach
- Stakeholder collaboration
- CI/CD pipeline

### 風險評估

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| Gherkin learning curve | High | Medium | Training, templates, examples, workshops |
| Scenario maintenance overhead | Medium | Medium | Regular reviews, refactoring, reusable steps |
| Business stakeholder engagement | Medium | High | Demonstrate value, involve early, regular collaboration |
| Over-specification | Medium | Medium | Focus on business value, avoid technical details |
| Step definition duplication | Medium | Low | Reusable step library, code reviews |

**整體風險等級**： **Low**

## 實作計畫

### 第 1 階段： Setup and Training （第 1-2 週）

- [ ] Add Cucumber dependencies

  ```xml
  <dependency>
    <groupId>io.cucumber</groupId>
    <artifactId>cucumber-java</artifactId>
    <version>7.14.0</version>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>io.cucumber</groupId>
    <artifactId>cucumber-spring</artifactId>
    <version>7.14.0</version>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>io.cucumber</groupId>
    <artifactId>cucumber-junit-platform-engine</artifactId>
    <version>7.14.0</version>
    <scope>test</scope>
  </dependency>
  ```

- [ ] Configure Cucumber

  ```java
  @Suite
  @IncludeEngines("cucumber")
  @SelectClasspathResource("features")
  @ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, 
                          value = "pretty, html:target/cucumber-reports/cucumber.html")
  @ConfigurationParameter(key = GLUE_PROPERTY_NAME, 
                          value = "solid.humank.genaidemo.bdd")
  public class CucumberTestRunner {
  }
  ```

- [ ] Create project structure

  ```
  src/test/
  ├── java/
  │   └── solid/humank/genaidemo/bdd/
  │       ├── CucumberTestRunner.java
  │       ├── CucumberSpringConfiguration.java
  │       ├── steps/
  │       │   ├── CustomerSteps.java
  │       │   ├── OrderSteps.java
  │       │   └── CommonSteps.java
  │       └── support/
  │           ├── TestDataBuilder.java
  │           └── ScenarioContext.java
  └── resources/
      └── features/
          ├── customer/
          │   ├── customer-registration.feature
          │   └── customer-profile.feature
          ├── order/
          │   ├── order-submission.feature
          │   └── order-cancellation.feature
          └── product/
              └── product-search.feature
  ```

- [ ] Conduct team training
  - Gherkin syntax workshop
  - Writing effective scenarios
  - Step definition best practices
  - BDD workflow demonstration

### 第 2 階段： Core Step Definitions （第 2-3 週）

- [ ] Create base configuration

  ```java
  @SpringBootTest
  @CucumberContextConfiguration
  @ActiveProfiles("test")
  public class CucumberSpringConfiguration {
      
      @Autowired
      private ApplicationContext applicationContext;
      
      @BeforeAll
      public static void setup() {
          // Global test setup
      }
      
      @AfterAll
      public static void teardown() {
          // Global test cleanup
      }
  }
  ```

- [ ] Create scenario context

  ```java
  @Component
  @Scope("cucumber-glue")
  public class ScenarioContext {
      
      private final Map<String, Object> context = new HashMap<>();
      
      public void set(String key, Object value) {
          context.put(key, value);
      }
      
      @SuppressWarnings("unchecked")
      public <T> T get(String key) {
          return (T) context.get(key);
      }
      
      public void clear() {
          context.clear();
      }
  }
  ```

- [ ] Create common step definitions

  ```java
  public class CommonSteps {
      
      @Autowired
      private ScenarioContext scenarioContext;
      
      @Before
      public void beforeScenario() {
          scenarioContext.clear();
      }
      
      @After
      public void afterScenario() {
          // Cleanup after each scenario
      }
      
      @Given("the system is ready")
      public void theSystemIsReady() {
          // Verify system is in ready state
      }
      
      @Given("the current date is {string}")
      public void theCurrentDateIs(String date) {
          LocalDate testDate = LocalDate.parse(date);
          scenarioContext.set("currentDate", testDate);
      }
  }
  ```

### 第 3 階段： Feature Implementation （第 3-6 週）

- [ ] Write Customer features

  ```gherkin
  # customer-registration.feature
  Feature: Customer Registration
    As a new user
    I want to register an account
    So that I can make purchases
    
    Scenario: Successful registration with valid information
      Given I am on the registration page
      When I fill in the registration form with:
        | Field    | Value              |
        | Name     | John Doe           |
        | Email    | john@example.com   |
        | Password | SecurePass123!     |
      And I submit the registration form
      Then I should see a success message "Registration successful"
      And a welcome email should be sent to "john@example.com"
      And my account should be created with status "ACTIVE"
    
    Scenario: Registration fails with duplicate email
      Given a customer exists with email "existing@example.com"
      When I try to register with email "existing@example.com"
      Then I should see an error "Email already registered"
      And no account should be created
    
    Scenario Outline: Registration fails with invalid data
      When I try to register with <field> as "<value>"
      Then I should see an error "<error_message>"
      
      Examples:
        | field    | value           | error_message                |
        | email    | invalid-email   | Invalid email format         |
        | password | short           | Password too short           |
        | name     |                 | Name is required             |
  ```

- [ ] Write Order features

  ```gherkin
  # order-submission.feature
  Feature: Order Submission
    As a customer
    I want to submit orders
    So that I can purchase products
    
    Background:
      Given a customer "John Doe" with ID "CUST-001"
      And the customer has a valid payment method
      And the following products are available:
        | Product ID | Name        | Price | Stock |
        | PROD-001   | Laptop      | 999   | 10    |
        | PROD-002   | Mouse       | 29    | 50    |
        | PROD-003   | Keyboard    | 79    | 30    |
    
    Scenario: Submit order with single item
      Given the customer adds to cart:
        | Product ID | Quantity |
        | PROD-001   | 1        |
      When the customer submits the order
      Then the order should be created with status "PENDING"
      And the order total should be 999.00
      And inventory should be reserved:
        | Product ID | Quantity |
        | PROD-001   | 1        |
      And an order confirmation should be sent
    
    Scenario: Submit order with multiple items
      Given the customer adds to cart:
        | Product ID | Quantity |
        | PROD-001   | 1        |
        | PROD-002   | 2        |
        | PROD-003   | 1        |
      When the customer submits the order
      Then the order should be created with status "PENDING"
      And the order total should be 1137.00
      And inventory should be reserved for all items
    
    Scenario: Order submission fails with insufficient inventory
      Given product "PROD-001" has only 5 units in stock
      And the customer adds to cart:
        | Product ID | Quantity |
        | PROD-001   | 10       |
      When the customer submits the order
      Then the order should be rejected
      And the error should be "Insufficient inventory for PROD-001"
      And no inventory should be reserved
    
    Scenario: Order submission fails with invalid payment
      Given the customer has an expired payment method
      And the customer adds to cart:
        | Product ID | Quantity |
        | PROD-001   | 1        |
      When the customer submits the order
      Then the order should be rejected
      And the error should be "Payment method invalid"
  ```

- [ ] Implement step definitions 用於 all features

### 第 4 階段： Business Stakeholder Collaboration （第 6-8 週）

- [ ] Conduct BDD workshops 與 business stakeholders
  - Explain Gherkin syntax
  - Demonstrate scenario writing
  - Practice writing scenarios together
  - Review existing scenarios

- [ ] Establish scenario review process
  - Business stakeholders review scenarios
  - Developers implement step definitions
  - QA validates scenarios
  - Regular refinement sessions

- [ ] Create scenario templates

  ```gherkin
  # Template for CRUD operations
  Feature: [Entity] Management
    As a [role]
    I want to [action]
    So that [benefit]
    
    Scenario: Create [entity] successfully
      Given [preconditions]
      When I create a [entity] with [data]
      Then the [entity] should be created
      And [expected outcomes]
    
    Scenario: Update [entity] successfully
      Given a [entity] exists with [identifier]
      When I update the [entity] with [data]
      Then the [entity] should be updated
      And [expected outcomes]
  ```

### Phase 5: CI/CD Integration （第 8-9 週）

- [ ] Configure Gradle task

  ```gradle
  tasks.register('cucumber', JavaExec) {
      dependsOn assemble, testClasses
      mainClass = 'io.cucumber.core.cli.Main'
      classpath = configurations.cucumberRuntime + sourceSets.main.output + sourceSets.test.output
      args = [
          '--plugin', 'pretty',
          '--plugin', 'html:build/reports/cucumber/cucumber.html',
          '--plugin', 'json:build/reports/cucumber/cucumber.json',
          '--plugin', 'io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm',
          '--glue', 'solid.humank.genaidemo.bdd',
          'src/test/resources/features'
      ]
  }
  ```

- [ ] Add to CI/CD pipeline

  ```yaml
  # .github/workflows/test.yml

  - name: Run BDD Tests

    run: ./gradlew cucumber
    
  - name: Generate Cucumber Report

    if: always()
    uses: actions/upload-artifact@v3
    with:
      name: cucumber-report
      path: build/reports/cucumber/
  ```

- [ ] Set up Allure reporting

  ```xml
  <dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-cucumber7-jvm</artifactId>
    <version>2.24.0</version>
  </dependency>
  ```

### Phase 6: Documentation and Best Practices （第 9-10 週）

- [ ] Create BDD guidelines document
  - Scenario writing best practices
  - Step definition patterns
  - Naming conventions
  - Common pitfalls to avoid

- [ ] Create living documentation

  ```bash
  # Generate living documentation from feature files
  ./gradlew cucumber
  # Open build/reports/cucumber/cucumber.html
  ```

- [ ] Establish maintenance process
  - Regular scenario reviews
  - Refactoring step definitions
  - Updating scenarios 與 requirements changes
  - Archiving obsolete scenarios

### 回滾策略

**觸發條件**：

- Business stakeholders not engaged after 3 個月
- Scenario maintenance overhead > 30% of development time
- Team unable to adopt BDD practices
- Scenarios become out of sync 與 implementation

**回滾步驟**：

1. Archive feature files 用於 reference
2. Convert critical scenarios to JUnit tests
3. Simplify to plain JUnit 與 descriptive names
4. Document lessons learned
5. Re-evaluate BDD approach

**回滾時間**： 2 weeks

## 監控和成功標準

### 成功指標

- ✅ 100% of user stories have Gherkin scenarios
- ✅ Business stakeholder participation in scenario reviews > 80%
- ✅ Scenario pass rate > 95%
- ✅ Scenario execution time < 10 minutes
- ✅ Living documentation generated automatically
- ✅ Developer satisfaction 與 BDD > 4/5
- ✅ Business stakeholder satisfaction > 4/5

### 監控計畫

**BDD Metrics**:

- Number of scenarios per feature
- Scenario pass/fail rates
- Scenario execution time
- Step definition reuse rate
- Business stakeholder engagement

**Quality Metrics**:

- Requirements coverage 透過 scenarios
- Defects found 透過 BDD tests
- Time to write scenarios vs implementation
- Scenario maintenance effort

**審查時程**：

- Weekly: Scenario review sessions
- Monthly: BDD practice retrospective
- Quarterly: Business stakeholder feedback

## 後果

### 正面後果

- ✅ **Business Collaboration**: Non-technical stakeholders 可以 read 和 validate requirements
- ✅ **Living Documentation**: Scenarios serve as up-to-date documentation
- ✅ **Executable Specifications**: Requirements are automatically tested
- ✅ **Ubiquitous Language**: Scenarios use domain language
- ✅ **Clear Acceptance Criteria**: Each scenario defines "done"
- ✅ **Regression Testing**: Scenarios prevent regression
- ✅ **Requirements Traceability**: Clear link from requirement to test
- ✅ **降低d Misunderstandings**: Shared understanding of requirements

### 負面後果

- ⚠️ **Learning Curve**: Team needs to learn Gherkin 和 BDD practices
- ⚠️ **Initial Overhead**: Writing scenarios takes time initially
- ⚠️ **Maintenance**: Scenarios need to be kept up-to-date
- ⚠️ **Engagement Required**: Requires active business stakeholder participation
- ⚠️ **可以 Be Verbose**: Some scenarios 可以 become lengthy

### 技術債務

**已識別債務**：

1. Not all features have BDD scenarios yet (gradual adoption)
2. Some step definitions have duplication (needs refactoring)
3. Limited Chinese language scenarios (future requirement)
4. No automated scenario generation from requirements (future enhancement)

**債務償還計畫**：

- **Q1 2026**: Achieve 100% scenario coverage 用於 all features
- **Q2 2026**: Refactor step definitions to eliminate duplication
- **Q3 2026**: Add Chinese language scenario 支援
- **Q4 2026**: Explore automated scenario generation tools

## 相關決策

- [ADR-006: Environment-Specific Testing Strategy](006-environment-specific-testing-strategy.md) - BDD as part of testing strategy
- [ADR-002: Adopt Hexagonal Architecture](002-adopt-hexagonal-architecture.md) - Scenarios test through application layer

## 備註

### Gherkin Best Practices

**DO**:

- Use business language, not technical jargon
- Keep scenarios focused on business value
- Use Background 用於 common setup
- Use Scenario Outline 用於 similar scenarios 與 different data
- Make scenarios independent 和 isolated
- Use descriptive scenario names

**DON'T**:

- Include technical implementation details
- Make scenarios dependent on each other
- Use UI-specific language (unless testing UI)
- Write overly long scenarios (> 10 steps)
- Duplicate step definitions

### Step Definition Patterns

**Parameter Types**:

```java
@ParameterType("CUST-\\d+")
public CustomerId customerId(String id) {
    return CustomerId.of(id);
}

@ParameterType("\\d+\\.\\d{2}")
public Money money(String amount) {
    return Money.of(new BigDecimal(amount));
}
```

**Data Tables**:

```java
@Given("the following products exist:")
public void theFollowingProductsExist(List<Product> products) {
    products.forEach(productRepository::save);
}

// With custom transformer
@DataTableType
public Product productEntry(Map<String, String> entry) {
    return new Product(
        ProductId.of(entry.get("Product ID")),
        entry.get("Name"),
        Money.of(new BigDecimal(entry.get("Price")))
    );
}
```

### Scenario Organization

```text
features/
├── customer/
│   ├── registration.feature
│   ├── authentication.feature
│   └── profile-management.feature
├── order/
│   ├── order-submission.feature
│   ├── order-cancellation.feature
│   └── order-tracking.feature
├── product/
│   ├── product-search.feature
│   ├── product-details.feature
│   └── product-reviews.feature
└── payment/
    ├── payment-processing.feature
    └── refund-processing.feature
```

### Example Report Output

```text
Feature: Order Submission
  ✓ Submit order with single item (1.2s)
  ✓ Submit order with multiple items (1.5s)
  ✓ Order submission fails with insufficient inventory (0.8s)
  ✓ Order submission fails with invalid payment (0.9s)

4 scenarios (4 passed)
16 steps (16 passed)
0m4.4s
```

---

**文檔狀態**： ✅ Accepted  
**上次審查**： 2025-10-24  
**下次審查**： 2026-01-24 （每季）
