# Development 文件

> **最後更新**: 2025-01-17

## 概述

本章節包含 GenAI Demo 電子商務平台的完整開發文件，包括設定指南、編碼標準、測試策略、工作流程和範例。

## 快速導航

### 🚀 入門指南

- [Development 設定](setup/README.md) - 環境設定和先決條件
- [本地開發](setup/local-development.md) - 在本地執行應用程式
- [IDE 配置](setup/ide-configuration.md) - IDE 設定和插件
- [新進人員指南](setup/onboarding.md) - 新進開發人員入職指南

### 📝 編碼標準

- [Java 標準](coding-standards/java-standards.md) - Java 編碼慣例
- [TypeScript 標準](coding-standards/typescript-standards.md) - TypeScript 慣例
- [Code Review 指南](coding-standards/code-review.md) - Code review 流程
- [Git Commit 標準](coding-standards/git-standards.md) - Commit 訊息格式

### 🧪 測試

- [測試策略](testing/testing-strategy.md) - 整體測試方法
- [Unit Testing](testing/unit-testing.md) - Unit test 指南
- [Integration Testing](testing/integration-testing.md) - Integration test 指南
- [BDD Testing](testing/bdd-testing.md) - Behavior-driven development
- [Architecture Testing](testing/architecture-testing.md) - ArchUnit 測試

### 🔄 工作流程

- [Git Workflow](workflows/git-workflow.md) - 分支和合併策略
- [Code Review 流程](workflows/code-review.md) - Review 程序
- [CI/CD Pipeline](workflows/ci-cd.md) - 持續整合和部署
- [發布流程](workflows/release-process.md) - Release 管理

### 💡 範例

- [建立 Aggregate](examples/creating-aggregate.md) - DDD aggregate 範例
- [實作 Events](examples/implementing-event.md) - Domain event 範例
- [撰寫測試](examples/writing-tests.md) - 測試範例
- [API 實作](examples/api-implementation.md) - REST API 範例

### 🔧 工具與 Hooks

- [開發工具](tools/README.md) - 推薦的開發工具
- [Kiro Hooks](hooks/README.md) - 自動化開發 hooks
- [圖表生成](hooks/diagram-hooks-design.md) - 圖表自動化

## 開發環境

### 先決條件

- **Java**: JDK 21 或更高版本
- **Node.js**: v18 或更高版本 (用於前端)
- **Docker**: 最新版本
- **Gradle**: 8.x (已包含 wrapper)
- **Git**: 最新版本
- **IDE**: IntelliJ IDEA 或 VS Code

### 快速開始

```bash
# Clone repository
git clone https://github.com/company/genai-demo.git
cd genai-demo

# Setup environment
./scripts/setup-dev-environment.sh

# Run application
./gradlew bootRun

# Run tests
./gradlew test
```

[詳細設定指南](setup/README.md)

## 架構概述

### Hexagonal Architecture

應用程式遵循 hexagonal architecture (ports and adapters):

```
src/
├── main/
│   └── java/
│       └── solid/humank/genaidemo/
│           ├── domain/          # Business logic (no dependencies)
│           ├── application/     # Use cases (depends on domain)
│           ├── infrastructure/  # Technical implementations
│           └── interfaces/      # API controllers
```

[架構指南](../viewpoints/development/README.md)

### Domain-Driven Design

我們遵循 DDD tactical patterns:

- **Aggregates**: Consistency boundaries
- **Entities**: Objects with identity
- **Value Objects**: Immutable objects
- **Domain Events**: Business events
- **Repositories**: Data access interfaces
- **Domain Services**: Cross-aggregate logic

[DDD Patterns 指南](../architecture/patterns/ddd-patterns.md)

## 編碼標準

### Java 編碼標準

- 遵循 [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- 使用 Java 21 特性 (records, pattern matching 等)
- 撰寫自我說明的程式碼
- 使用有意義的變數名稱
- 保持方法簡短 (< 20 行)

[完整 Java 標準](coding-standards/java-standards.md)

### 程式碼品質工具

- **Checkstyle**: 程式碼風格檢查
- **PMD**: 程式碼品質分析
- **SpotBugs**: Bug 偵測
- **SonarQube**: 綜合程式碼分析
- **ArchUnit**: 架構測試

```bash
# Run code quality checks
./gradlew check
./gradlew pmdMain
./gradlew spotbugsMain
```

## 測試策略

### Test Pyramid

- **Unit Tests (80%)**: 快速、隔離的測試
- **Integration Tests (15%)**: 元件整合
- **E2E Tests (5%)**: 完整的使用者旅程

### 測試指令

```bash
# Run all tests
./gradlew test

# Run unit tests only
./gradlew unitTest

# Run integration tests
./gradlew integrationTest

# Run E2E tests
./gradlew e2eTest

# Run BDD tests
./gradlew cucumber

# Generate coverage report
./gradlew jacocoTestReport
```

[測試策略指南](testing/testing-strategy.md)

### 測試覆蓋率要求

- **最低覆蓋率**: 80% 行覆蓋率
- **關鍵路徑**: 100% 覆蓋率
- **新程式碼**: 必須包含測試
- **Bug 修復**: 必須包含 regression test

## 開發工作流程

### Git Workflow

我們使用 **Git Flow**，包含以下分支:

- `main`: 可用於生產環境的程式碼
- `develop`: 整合分支
- `feature/*`: 功能開發
- `bugfix/*`: Bug 修復
- `hotfix/*`: 生產環境緊急修復
- `release/*`: Release 準備

[Git Workflow 指南](workflows/git-workflow.md)

### Code Review 流程

1. **建立 PR**: 從 feature 分支到 develop
2. **自動化檢查**: CI/CD 執行測試
3. **Code Review**: 至少 2 位 reviewers
4. **處理回饋**: 進行要求的變更
5. **核准**: 獲得 reviewers 的核准
6. **合併**: Squash and merge 到 develop

[Code Review 指南](workflows/code-review.md)

### CI/CD Pipeline

我們的 CI/CD pipeline 包含:

1. **Build**: 編譯和打包
2. **Test**: 執行所有測試
3. **Quality**: 程式碼品質檢查
4. **Security**: 安全性掃描
5. **Deploy**: 部署到環境

[CI/CD 指南](workflows/ci-cd.md)

## 開發最佳實踐

### DDD 最佳實踐

1. **Ubiquitous Language**: 使用領域術語
2. **Bounded Contexts**: 清晰的上下文邊界
3. **Aggregate Design**: 小型、專注的 aggregates
4. **Event-Driven**: 使用 domain events 進行溝通
5. **Repository Pattern**: 抽象化資料存取

### Clean Code 原則

1. **SOLID Principles**: 遵循 SOLID 設計
2. **DRY**: Don't Repeat Yourself
3. **KISS**: Keep It Simple, Stupid
4. **YAGNI**: You Aren't Gonna Need It
5. **Boy Scout Rule**: 讓程式碼比你發現時更好

### 效能最佳實踐

1. **資料庫最佳化**: 使用索引，避免 N+1
2. **快取**: 快取經常存取的資料
3. **非同步處理**: 對長時間操作使用 async
4. **連線池**: 適當配置
5. **監控**: 監控效能指標

## 常見開發任務

### 建立新功能

1. **建立 Feature 分支**: `git checkout -b feature/my-feature`
2. **撰寫 BDD Scenarios**: 用 Gherkin 定義行為
3. **實作 Domain Logic**: 從 domain 層開始
4. **新增測試**: 撰寫 unit 和 integration tests
5. **實作 API**: 新增 REST endpoints
6. **更新文件**: 記錄變更
7. **建立 PR**: 提交審查

[功能開發指南](examples/creating-aggregate.md)

### 實作 Domain Events

1. **定義 Event**: 建立 event record
2. **在 Aggregate 中收集**: 使用 `collectEvent()`
3. **在 Service 中發布**: 使用 `DomainEventApplicationService`
4. **處理 Event**: 建立 event handler
5. **測試**: 撰寫 event 測試

[Event 實作指南](examples/implementing-event.md)

### 新增 API Endpoint

1. **設計 Endpoint**: 遵循 REST 原則
2. **建立 DTO**: Request/response objects
3. **實作 Controller**: REST controller
4. **新增驗證**: 輸入驗證
5. **撰寫測試**: API 測試
6. **文件**: 更新 API 文件

[API 實作指南](examples/api-implementation.md)

## 疑難排解

### 常見問題

#### Build 失敗

```bash
# Clean and rebuild
./gradlew clean build

# Clear Gradle cache
rm -rf ~/.gradle/caches
```

#### 測試失敗

```bash
# Run specific test
./gradlew test --tests "ClassName.testMethod"

# Run with debug logging
./gradlew test --debug
```

#### IDE 問題

- **IntelliJ**: File → Invalidate Caches / Restart
- **VS Code**: Reload window (Cmd+Shift+P → Reload Window)

[疑難排解指南](../operations/troubleshooting/README.md)

## 資源

### 文件

- [架構文件](../viewpoints/README.md)
- [API 文件](../api/README.md)
- [Operations 文件](../operations/README.md)

### 外部資源

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Domain-Driven Design](https://www.domainlanguage.com/ddd/)
- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)

### 培訓資料

- [DDD Workshop Materials](training/ddd-workshop/)
- [Testing Workshop](training/testing-workshop/)
- [Architecture Patterns](training/architecture-patterns/)

## 貢獻

### 貢獻指南

1. 遵循編碼標準
2. 為新程式碼撰寫測試
3. 更新文件
4. 提交 PR 並附上清楚的說明
5. 回應 review 回饋

### 文件更新

1. 遵循 [style guide](../STYLE-GUIDE.md)
2. 使用 [templates](../templates/) 中的範本
3. 保持文件最新
4. 在有幫助的地方新增範例

## 支援

### 取得協助

- **Slack**: #dev-support
- **Email**: dev-team@company.com
- **Wiki**: 內部開發 wiki
- **Office Hours**: 星期二 2-3 PM

### 回報問題

1. 檢查現有問題
2. 提供重現步驟
3. 包含錯誤訊息
4. 新增相關日誌

---

**文件負責人**: Development Team
**最後審查**: 2025-01-17
**下次審查**: 2025-04-17
**狀態**: Active
