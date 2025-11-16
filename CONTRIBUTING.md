# Contributing to Enterprise E-Commerce Platform

感謝您有興趣為本專案做出貢獻！本文件提供了為本專案做出貢獻的指南和說明。

## 📑 目錄

- [行為準則](#code-of-conduct)
- [開始使用](#getting-started)
- [開發工作流程](#development-workflow)
- [編碼標準](#coding-standards)
- [測試要求](#testing-requirements)
- [文件](#documentation)
- [Pull Request 流程](#pull-request-process)
- [社群](#community)

---

## Code of Conduct

### Our Pledge

我們致力於為所有人提供一個友善且鼓舞人心的社群。請在互動中保持尊重和建設性。

### Expected Behavior

- 尊重和包容
- 歡迎新成員並幫助他們入門
- 專注於對社群最有利的事情
- 對其他社群成員展現同理心

### Unacceptable Behavior

- 騷擾、歧視或冒犯性評論
- 惡意挑釁、侮辱或貶損性評論
- 公開或私下騷擾
- 發布他人的私人資訊

**回報問題**: yikaikao@gmail.com

---

## Getting Started

### Prerequisites

在開始之前，請確保您擁有：

- Java 21 或更高版本
- Gradle 8.x（透過 wrapper 包含）
- Docker 和 Docker Compose
- Node.js 18+（用於 CDK）
- Git

### Fork and Clone

1. **Fork the repository** 在 GitHub 上
2. **Clone your fork** 到本地：

```bash
git clone https://github.com/YOUR-USERNAME/genai-demo.git
cd genai-demo
```

3. **Add upstream remote**：

```bash
git remote add upstream https://github.com/ORIGINAL-OWNER/genai-demo.git
```

### Set Up Development Environment

```bash
# 執行一鍵設定
make dev-setup

# 或手動執行：
docker-compose up -d              # 啟動相依服務
./gradlew :app:build              # 建置應用程式
make setup-hooks                  # 設定 Git hooks
```

**詳細設定**: 參見 [Development Setup Guide](docs/development/setup/README.md)

---

## Development Workflow

### 1. Create a Branch

從 `main` 建立功能分支：

```bash
git checkout main
git pull upstream main
git checkout -b feature/your-feature-name
```

**Branch Naming Convention**:
- `feature/` - 新功能
- `fix/` - 錯誤修復
- `docs/` - 文件變更
- `refactor/` - 程式碼重構
- `test/` - 測試新增或修復
- `chore/` - 維護任務

### 2. Make Changes

遵循我們的編碼標準和最佳實踐：

- 撰寫簡潔、可讀的程式碼
- 遵循 [Coding Standards](docs/development/coding-standards/README.md)
- 為新功能新增測試
- 根據需要更新文件

### 3. Test Your Changes

```bash
# 執行單元測試
./gradlew :app:test

# 執行 BDD 測試
./gradlew :app:cucumber

# 檢查覆蓋率
./gradlew :app:jacocoTestReport

# 執行架構測試
./gradlew :app:test --tests "*ArchitectureTest"

# 執行所有 pre-commit 檢查
make pre-commit
```

### 4. Commit Your Changes

遵循 [Conventional Commits](https://www.conventionalcommits.org/) 格式：

```bash
git add .
git commit -m "feat(context): add new feature"
```

**Commit Message Format**:
```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

**Types**:
- `feat`: 新功能
- `fix`: 錯誤修復
- `docs`: 文件變更
- `style`: 程式碼樣式變更（格式化等）
- `refactor`: 程式碼重構
- `test`: 測試新增或修復
- `chore`: 維護任務
- `perf`: 效能改進

**Examples**:
```
feat(auth): add JWT authentication
fix(api): resolve timeout issue in order endpoint
docs(architecture): update deployment viewpoint
test(customer): add unit tests for customer service
```

### 5. Push and Create Pull Request

```bash
git push origin feature/your-feature-name
```

然後在 GitHub 上建立 pull request。

---

## Coding Standards

### Java Code Style

我們遵循 **Google Java Style Guide** 並進行一些修改：

#### Formatting

- **Indentation**: 4 個空格（不使用 tabs）
- **Line Length**: 最多 120 個字元
- **Braces**: K&R 樣式（左大括號在同一行）

#### Naming Conventions

```java
// Classes: PascalCase
public class CustomerService { }

// Methods: camelCase with verb-noun pattern
public Customer findCustomerById(String id) { }

// Variables: camelCase, descriptive names
private String customerEmail;

// Constants: UPPER_SNAKE_CASE
private static final int MAX_RETRY_ATTEMPTS = 3;

// Packages: lowercase, singular nouns
package solid.humank.genaidemo.domain.customer;
```

#### Code Organization

```java
// Order: static fields, instance fields, constructors, methods
public class Order {
    // 1. Static fields
    private static final Logger logger = LoggerFactory.getLogger(Order.class);

    // 2. Instance fields
    private final OrderId id;
    private OrderStatus status;

    // 3. Constructors
    public Order(OrderId id) {
        this.id = id;
    }

    // 4. Public methods
    public void submit() { }

    // 5. Private methods
    private void validate() { }
}
```

### Architecture Patterns

遵循 **Domain-Driven Design** 和 **Hexagonal Architecture** 原則：

#### Domain Layer

```java
// Aggregate Root
@AggregateRoot
public class Customer extends AggregateRoot {
    // Business logic here
    public void updateProfile(CustomerName name, Email email) {
        // Validate
        // Update state
        // Collect domain event
        collectEvent(CustomerProfileUpdatedEvent.create(id, name, email));
    }
}

// Value Object (use Records)
public record Email(String value) {
    public Email {
        if (value == null || !value.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }
}

// Domain Event (use Records)
public record CustomerCreatedEvent(
    CustomerId customerId,
    CustomerName name,
    Email email,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent { }
```

#### Application Layer

```java
@Service
@Transactional
public class CustomerApplicationService {
    private final CustomerRepository customerRepository;
    private final DomainEventApplicationService eventService;

    public void createCustomer(CreateCustomerCommand command) {
        // 1. Create aggregate
        Customer customer = new Customer(command.name(), command.email());

        // 2. Save aggregate
        customerRepository.save(customer);

        // 3. Publish events
        eventService.publishEventsFromAggregate(customer);
    }
}
```

**詳細標準**: [Coding Standards](docs/development/coding-standards/README.md)

---

## Testing Requirements

### Test Coverage

- **最低覆蓋率**: 80% 行覆蓋率
- **重點**: 業務邏輯和領域模型
- **工具**: JaCoCo 用於覆蓋率報告

### Test Types

#### Unit Tests (80% of tests)

```java
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void should_create_customer_when_valid_data_provided() {
        // Given
        CreateCustomerCommand command = new CreateCustomerCommand("John", "john@example.com");

        // When
        Customer customer = customerService.createCustomer(command);

        // Then
        assertThat(customer).isNotNull();
        assertThat(customer.getName()).isEqualTo("John");
        verify(customerRepository).save(any(Customer.class));
    }
}
```

#### BDD Tests (Cucumber)

```gherkin
Feature: Customer Registration

  Scenario: Successful customer registration
    Given a new customer with valid information
    When they submit the registration form
    Then their account should be created
    And they should receive a welcome email
```

#### Architecture Tests (ArchUnit)

```java
@ArchTest
static final ArchRule domainLayerRules = classes()
    .that().resideInAPackage("..domain..")
    .should().onlyDependOnClassesThat()
    .resideInAnyPackage("..domain..", "java..");
```

### Running Tests

```bash
# 單元測試
./gradlew :app:test

# BDD 測試
./gradlew :app:cucumber

# 覆蓋率報告
./gradlew :app:jacocoTestReport
# 檢視: build/reports/jacoco/test/html/index.html

# 架構測試
./gradlew :app:test --tests "*ArchitectureTest"
```

**詳細測試指南**: [Testing Strategy](docs/development/testing/testing-strategy.md)

---

## Documentation

### Documentation Requirements

進行變更時，請更新相關文件：

- **程式碼變更**: 更新行內註解和 JavaDoc
- **API 變更**: 更新 OpenAPI 規格
- **架構變更**: 更新相關 viewpoint 文件
- **新功能**: 新增到 functional viewpoint 和使用者指南

### Documentation Structure

```text
docs/
├── viewpoints/              # Architecture viewpoints
├── perspectives/            # Quality perspectives
├── architecture/            # ADRs and patterns
├── api/                     # API documentation
├── development/             # Developer guides
└── operations/              # Operational procedures
```

### Writing Documentation

遵循我們的 [Documentation Style Guide](docs/STYLE-GUIDE.md)：

#### Markdown Standards

- 使用 ATX 樣式標題（`#` 而非 `===`）
- 每行一個句子以獲得更好的差異比較
- 內部參考使用相對連結
- 適當時包含程式碼範例

#### Diagrams

- 使用 PlantUML 繪製架構圖
- 使用 Mermaid 繪製簡單流程圖
- 將圖表儲存在 `docs/diagrams/`
- 產生圖表: `make diagrams`

#### Examples

```markdown
# Good Documentation

## Overview

此元件使用 JWT tokens 處理客戶驗證。

## Usage

```java
CustomerService service = new CustomerService(repository);
Customer customer = service.findById("123");
```

## Related Documentation

- [Security Perspective](../perspectives/security/README.md)
- [Authentication Guide](./authentication.md)
```

**詳細指南**: [Documentation Style Guide](docs/STYLE-GUIDE.md)

---

## Pull Request Process

### Before Submitting

1. **執行所有檢查**:
   ```bash
   make pre-commit
   ```

2. **確保測試通過**:
   ```bash
   ./gradlew :app:test
   ./gradlew :app:cucumber
   ```

3. **檢查覆蓋率**:
   ```bash
   ./gradlew :app:jacocoTestReport
   # 確保覆蓋率高於 80%
   ```

4. **更新文件**:
   - 更新相關 viewpoint 文件
   - 新增/更新 API 文件
   - 更新 CHANGELOG.md

### Pull Request Template

建立 pull request 時，請包含：

```markdown
## Description

變更的簡要說明

## Type of Change

- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Related Issues

Closes #123

## Testing

- [ ] Unit tests added/updated
- [ ] BDD tests added/updated
- [ ] Manual testing performed

## Documentation

- [ ] Code comments updated
- [ ] API documentation updated
- [ ] Architecture documentation updated

## Checklist

- [ ] Code follows style guidelines
- [ ] Tests pass locally
- [ ] Coverage is above 80%
- [ ] Documentation is updated
- [ ] No breaking changes (or documented)
```

### Review Process

1. **Automated Checks**: CI/CD 自動執行
2. **Code Review**: 至少需要一個核准
3. **Architecture Review**: 用於重大變更
4. **Merge**: Squash and merge 到 main

### After Merge

- 刪除您的功能分支
- 更新您的本地 main 分支
- 關閉相關 issues

---

## Community

### Communication Channels

- **GitHub Issues**: 錯誤回報和功能請求
- **GitHub Discussions**: 問題和一般討論
- **Email**: yikaikao@gmail.com

### Getting Help

- 檢查 [FAQ](README.md#-faq)
- 搜尋 [existing issues](https://github.com/yourusername/genai-demo/issues)
- 在 [Discussions](https://github.com/yourusername/genai-demo/discussions) 提問
- Email 維護者: yikaikao@gmail.com

### Recognition

貢獻者將被認可於：
- CONTRIBUTORS.md 文件
- Release notes
- 專案文件

---

## Additional Resources

### Documentation

- [Development Guide](docs/development/README.md)
- [Architecture Guide](docs/rozanski-woods-methodology-guide.md)
- [Testing Guide](docs/development/testing/testing-strategy.md)
- [API Documentation](docs/api/README.md)

### External Resources

- [Domain-Driven Design](https://www.domainlanguage.com/ddd/)
- [Rozanski & Woods Methodology](https://www.viewpoints-and-perspectives.info/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [AWS CDK Documentation](https://docs.aws.amazon.com/cdk/)

---

## Questions?

如果您對貢獻有任何疑問：

- 檢查本指南和連結的文件
- 搜尋 [existing issues](https://github.com/yourusername/genai-demo/issues)
- 在 [Discussions](https://github.com/yourusername/genai-demo/discussions) 提問
- Email: yikaikao@gmail.com

**感謝您的貢獻！**

---

**最後更新**: 2024-11-09
