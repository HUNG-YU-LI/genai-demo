---
adr_number: 002
title: "Adopt Hexagonal Architecture"
date: 2025-10-24
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [001, 003, 006]
affected_viewpoints: ["development", "functional", "information"]
affected_perspectives: ["evolution", "development-resource"]
---

# ADR-002: Adopt Hexagonal Architecture

## 狀態

**Accepted** - 2025-10-24

## 上下文

### 問題陳述

企業電子商務平台需要一個架構能夠：

- 將業務邏輯與技術關注點分離
- 支援獨立測試領域邏輯
- 允許技術變更而不影響業務規則
- 支援多種介面（REST API、CLI、messaging）
- 促進團隊協作和平行開發
- 維持層級之間清晰的邊界

### 業務上下文

**業務驅動因素**：

- 需要快速功能開發而不破壞現有功能
- 需要支援多種客戶端類型（web、mobile、API 消費者）
- 預期技術堆疊在 5 年以上的生命週期內持續演進
- 需要全面測試以確保品質
- 團隊從 5 人成長到 20 人以上

**限制條件**：

- 團隊具有 Spring Boot 經驗但 DDD 經驗有限
- 3 個月時程達成 MVP
- 必須整合現有的 AWS 基礎設施
- 隨著系統演進需要維持向後相容性

### 技術上下文

**目前狀態**：

- 全新的 greenfield 專案
- Spring Boot 3.4.5 + Java 21
- 已選擇 Domain-Driven Design (DDD) tactical patterns
- Event-driven architecture 用於 cross-context 通訊

**需求**：

- 清晰的關注點分離
- 可測試的業務邏輯
- 技術無關的領域層
- 支援多種 adapters（REST、messaging、CLI）
- 容易讓新開發人員上手
- 5 年以上可維護性

## 決策驅動因素

1. **可測試性**：需要在沒有基礎設施依賴的情況下測試業務邏輯
2. **可維護性**：清晰的邊界降低耦合並提升可維護性
3. **技術獨立性**：業務邏輯不應依賴框架
4. **團隊可擴展性**：清晰的結構幫助團隊獨立工作
5. **演進性**：容易新增介面和變更實作
6. **DDD 對齊**：架構應支援 DDD tactical patterns
7. **Spring Boot 整合**：必須與 Spring 生態系統良好配合
8. **學習曲線**：團隊需要快速採用

## 考慮的選項

### 選項 1：Hexagonal Architecture (Ports and Adapters)

**描述**：透過 ports（介面）和 adapters（實作）將核心業務邏輯與外部關注點分離的架構模式

**結構**：

```text
domain/          # 核心業務邏輯（無依賴）
├── model/       # Aggregates、entities、value objects
├── events/      # Domain events
├── repository/  # Repository 介面（ports）
└── service/     # Domain services

application/     # Use case 編排
└── {context}/   # Application services

infrastructure/  # 技術實作（adapters）
├── persistence/ # Repository 實作
├── messaging/   # Event publishers
└── external/    # External service adapters

interfaces/      # 外部介面（adapters）
├── rest/        # REST controllers
└── messaging/   # Message consumers
```

**優點**：

- ✅ 業務邏輯與基礎設施完全分離
- ✅ 領域層零外部依賴
- ✅ 容易獨立測試領域邏輯
- ✅ 技術變更不影響業務規則
- ✅ 多個 adapters 可以共存（REST、CLI、messaging）
- ✅ 清晰的依賴方向（全部依賴領域層）
- ✅ 非常適合 DDD 實作
- ✅ 自然支援 event-driven architecture

**缺點**：

- ⚠️ 更多初始設定和樣板代碼
- ⚠️ 團隊學習曲線
- ⚠️ 更多介面和抽象
- ⚠️ 簡單 CRUD 可能感覺過度工程化

**成本**：

- 初始：額外 2 週設定時間
- 持續：最小（在可維護性上獲得回報）

**風險**：**低** - 已建立的模式，具有驗證的效益

### 選項 2：Layered Architecture（傳統 N-Tier）

**描述**：具有展示層、業務層和資料層的傳統分層架構

**結構**：

```text
presentation/    # Controllers、views
business/        # 業務邏輯
data/            # 資料存取
```

**優點**：

- ✅ 大多數開發人員熟悉
- ✅ 容易理解
- ✅ 較少樣板代碼
- ✅ 快速實作

**缺點**：

- ❌ 業務邏輯經常洩漏到其他層
- ❌ 與框架和資料庫緊密耦合
- ❌ 難以獨立測試
- ❌ 技術變更影響業務邏輯
- ❌ 不能很好地支援 DDD
- ❌ 單一 adapter 類型（通常是 REST）

**成本**：較低的初始成本，較高的維護成本

**風險**：**中等** - 技術債務快速累積

### 選項 3：Clean Architecture (Uncle Bob)

**描述**：類似 Hexagonal 但具有更明確的層級定義

**結構**：

```text
entities/        # 企業業務規則
use-cases/       # 應用程式業務規則
interface-adapters/  # Controllers、presenters、gateways
frameworks/      # 外部框架和工具
```

**優點**：

- ✅ 與 Hexagonal 有類似效益
- ✅ 非常明確的層級邊界
- ✅ 強調依賴規則
- ✅ 適合複雜領域

**缺點**：

- ⚠️ 比 Hexagonal 更多層級
- ⚠️ 可能過度複雜
- ⚠️ 較少 Spring Boot 整合範例
- ⚠️ 更陡峭的學習曲線

**成本**：與 Hexagonal 類似，複雜度稍高

**風險**：**低** - 已驗證的模式，但更複雜

### 選項 4：Modular Monolith

**描述**：具有清晰模組邊界的單一可部署單元

**優點**：

- ✅ 更簡單的部署
- ✅ 清晰的模組邊界
- ✅ 可以演進為 microservices

**缺點**：

- ❌ 不處理層級分離
- ❌ 仍可能有緊密耦合
- ❌ 不解決可測試性問題
- ❌ 與 Hexagonal 不互斥

**成本**：與分層架構類似

**風險**：**中等** - 邊界可能隨時間侵蝕

## 決策結果

**選擇的選項**：**Hexagonal Architecture (Ports and Adapters)**

### 理由

選擇 Hexagonal Architecture 的原因如下：

1. **完美的 DDD 契合**：與我們的 DDD tactical patterns（aggregates、repositories、domain events）完美對齊
2. **可測試性**：領域邏輯可以在沒有任何基礎設施依賴的情況下測試
3. **技術獨立性**：業務規則完全與 Spring Boot、資料庫和 messaging 隔離
4. **多個 Adapters**：自然支援 REST API、messaging 和未來的 CLI/GraphQL 介面
5. **清晰的邊界**：明確的 ports（介面）和 adapters（實作）防止耦合
6. **Event-Driven 支援**：Domain events 自然融入架構
7. **團隊成長**：清晰的結構幫助新開發人員理解系統
8. **長期可維護性**：技術變更（例如資料庫、messaging）不影響業務邏輯

**為何不選 Layered**：分層架構導致緊密耦合並使測試困難。業務邏輯經常洩漏到 controllers 和 repositories。

**為何不選 Clean Architecture**：雖然類似，但 Hexagonal 更簡單且有更好的 Spring Boot 整合範例。Clean Architecture 中的額外層級增加了複雜性，但對我們的使用案例沒有顯著效益。

## 影響分析

### 利害關係人影響

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | High | 需要學習 Hexagonal Architecture | 培訓課程、程式碼範例、結對程式設計 |
| Architects | Positive | 清晰的架構邊界 | 架構文檔、ADRs |
| QA Team | Positive | 更容易測試業務邏輯 | 測試指南、範例測試 |
| Operations | Low | 無營運影響 | N/A |
| Business | Positive | 更快的功能開發 | N/A |

### 影響半徑

**選擇的影響半徑**：**System**

影響：

- 所有 bounded contexts（套件結構）
- 所有層級（domain、application、infrastructure、interfaces）
- 測試策略（領域的單元測試）
- 開發工作流程（程式碼放置位置）
- 入職流程（架構培訓）

### 風險評估

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| 團隊學習曲線 | High | Medium | 培訓、範例、結對程式設計、程式碼審查 |
| 過度工程化簡單功能 | Medium | Low | 務實的方法，允許 CRUD 使用更簡單的模式 |
| 樣板代碼 | Medium | Low | 程式碼產生器、模板、IDE 片段 |
| 實作不一致 | Medium | Medium | ArchUnit 測試、程式碼審查、架構指南 |
| 抗拒變更 | Low | Medium | 展示效益、讓團隊參與決策 |

**整體風險等級**：**低**

## 實作計畫

### 第 1 階段：基礎設定（第 1 週）

- [x] 建立套件結構（domain、application、infrastructure、interfaces）
- [x] 定義基礎介面（AggregateRoot、DomainEvent、Repository）
- [x] 設定 ArchUnit 測試以強制執行架構規則
- [x] 建立完整實作的範例 aggregate
- [x] 撰寫架構指南文檔

### 第 2 階段：團隊培訓（第 1-2 週）

- [x] 進行架構培訓課程
- [x] 為常見模式建立程式碼範例
- [x] 設定結對程式設計課程
- [x] 建立架構決策流程圖
- [x] 撰寫「程式碼放置位置」指南

### 第 3 階段：實作（第 2-12 週）

- [x] 實作 Customer bounded context
- [x] 實作 Order bounded context
- [x] 實作 Product bounded context
- [ ] 實作剩餘的 bounded contexts
- [ ] 持續程式碼審查以確保架構合規
- [ ] 根據回饋精煉模式

### 第 4 階段：驗證（持續進行）

- [x] ArchUnit 測試在 CI/CD 中執行
- [x] 定期架構審查
- [x] 收集團隊回饋
- [x] 根據學習更新指南

### 回滾策略

**觸發條件**：

- 團隊在 4 週後無法採用
- 開發速度降低 > 30%
- 過多的樣板代碼減慢開發
- 架構違規 > 50% 的 PRs

**回滾步驟**：

1. 簡化為分層架構
2. 保留領域模型但允許直接依賴
3. 將基礎設施合併到應用程式層
4. 更新 ArchUnit 規則
5. 逐步重構現有程式碼

**回滾時間**：2 週

**注意**：回滾不太可能發生，因為在初始學習期後效益通常超過成本。

## 監控和成功標準

### 成功指標

- ✅ 100% 的領域層零基礎設施依賴（ArchUnit）
- ✅ 80% 以上的領域邏輯單元測試覆蓋率
- ✅ 程式碼審查中 < 5% 的架構違規
- ✅ 新開發人員在 1 週內具生產力
- ✅ 技術變更（例如資料庫）耗時 < 1 天
- ✅ 團隊滿意度分數 > 4/5

### 監控計畫

**ArchUnit Tests**：

```java
@ArchTest
static final ArchRule domainLayerRules = classes()
    .that().resideInAPackage("..domain..")
    .should().onlyDependOnClassesThat()
    .resideInAnyPackage("..domain..", "java..");

@ArchTest
static final ArchRule repositoryRules = classes()
    .that().haveSimpleNameEndingWith("Repository")
    .and().areInterfaces()
    .should().resideInAPackage("..domain..repository..");
```

**程式碼審查檢查清單**：

- [ ] 領域邏輯在領域層
- [ ] 領域中無基礎設施依賴
- [ ] Repositories 是領域中的介面
- [ ] Adapters 實作領域介面
- [ ] Application services 編排 use cases

**審查時程**：

- 每週：團隊會議中的架構審查
- 每月：ArchUnit 測試結果審查
- 每季：架構回顧

## 後果

### 正面後果

- ✅ **可測試性**：在沒有基礎設施的情況下測試領域邏輯（快速、可靠的測試）
- ✅ **技術獨立性**：可以變更資料庫、框架而不影響業務邏輯
- ✅ **清晰的邊界**：明確的 ports 和 adapters 防止耦合
- ✅ **多種介面**：容易新增 REST、GraphQL、CLI、messaging adapters
- ✅ **DDD 支援**：完美契合 aggregates、repositories、domain events
- ✅ **團隊可擴展性**：清晰的結構幫助團隊獨立工作
- ✅ **可維護性**：變更局限於特定層級
- ✅ **入職**：新開發人員快速理解結構

### 負面後果

- ⚠️ **初始開銷**：更多設定時間和樣板代碼
- ⚠️ **學習曲線**：團隊需要學習新模式
- ⚠️ **更多抽象**：更多介面和類別
- ⚠️ **潛在的過度工程化**：簡單 CRUD 可能感覺複雜

### 技術債務

**已識別債務**：

1. 一些簡單的 CRUD 操作有不必要的抽象（可接受的權衡）
2. 層級間 mappers 的樣板代碼（可使用 MapStruct 減少）
3. 新團隊成員的學習曲線（隨時間降低）

**債務償還計畫**：

- **2026 年 Q1**：引入 MapStruct 以減少 mapper 樣板代碼
- **2026 年 Q2**：為常見模式建立程式碼產生器
- **2026 年 Q3**：根據 6 個月經驗精煉模式

## 相關決策

- [ADR-001: Use PostgreSQL for Primary Database](001-use-postgresql-for-primary-database.md) - Repository 實作
- [ADR-003: Use Domain Events for Cross-Context Communication](003-use-domain-events-for-cross-context-communication.md) - Event-driven architecture
- [ADR-006: Environment-Specific Testing Strategy](006-environment-specific-testing-strategy.md) - 測試方法

## 備註

### 套件結構

```text
solid.humank.genaidemo/
├── domain/
│   ├── customer/
│   │   ├── model/
│   │   │   ├── aggregate/
│   │   │   │   └── Customer.java
│   │   │   ├── entity/
│   │   │   └── valueobject/
│   │   │       ├── CustomerId.java
│   │   │       └── Email.java
│   │   ├── events/
│   │   │   └── CustomerCreatedEvent.java
│   │   ├── repository/
│   │   │   └── CustomerRepository.java (interface)
│   │   └── service/
│   │       └── CustomerDomainService.java
│   └── shared/
│       └── valueobject/
│           └── Money.java
├── application/
│   └── customer/
│       ├── CustomerApplicationService.java
│       ├── command/
│       │   └── CreateCustomerCommand.java
│       └── dto/
│           └── CustomerDto.java
├── infrastructure/
│   └── customer/
│       ├── persistence/
│       │   ├── entity/
│       │   │   └── CustomerEntity.java
│       │   ├── mapper/
│       │   │   └── CustomerMapper.java
│       │   └── repository/
│       │       ├── CustomerJpaRepository.java
│       │       └── JpaCustomerRepository.java (implements CustomerRepository)
│       ├── messaging/
│       │   └── CustomerEventPublisher.java
│       └── external/
│           └── EmailServiceAdapter.java
└── interfaces/
    └── rest/
        └── customer/
            ├── controller/
            │   └── CustomerController.java
            ├── dto/
            │   ├── CreateCustomerRequest.java
            │   └── CustomerResponse.java
            └── mapper/
                └── CustomerDtoMapper.java
```

### 依賴規則

1. **Domain Layer**：不依賴任何其他層級
2. **Application Layer**：僅依賴領域層
3. **Infrastructure Layer**：依賴領域層（實作介面）
4. **Interfaces Layer**：依賴應用程式和領域層

### 範例：新增功能

**情境**：新增「更新客戶電子郵件」功能

1. **Domain Layer**：將方法新增到 Customer aggregate

   ```java
   public void updateEmail(Email newEmail) {
       validateEmail(newEmail);
       this.email = newEmail;
       collectEvent(CustomerEmailUpdatedEvent.create(id, newEmail));
   }
   ```

2. **Application Layer**：建立 command 和 service 方法

   ```java
   public void updateCustomerEmail(UpdateEmailCommand command) {
       Customer customer = customerRepository.findById(command.customerId());
       customer.updateEmail(command.newEmail());
       customerRepository.save(customer);
       eventService.publishEventsFromAggregate(customer);
   }
   ```

3. **Infrastructure Layer**：無需變更（repository 已存在）

4. **Interfaces Layer**：新增 REST endpoint

   ```java
   @PutMapping("/{id}/email")
   public ResponseEntity<Void> updateEmail(@PathVariable String id, @RequestBody UpdateEmailRequest request) {
       applicationService.updateCustomerEmail(new UpdateEmailCommand(id, request.email()));
       return ResponseEntity.ok().build();
   }
   ```

### 測試策略

**單元測試**（Domain Layer）：

```java
@Test
void should_update_email_when_valid_email_provided() {
    // Given
    Customer customer = createCustomer();
    Email newEmail = new Email("new@example.com");
    
    // When
    customer.updateEmail(newEmail);
    
    // Then
    assertThat(customer.getEmail()).isEqualTo(newEmail);
    assertThat(customer.getUncommittedEvents()).hasSize(1);
}
```

**整合測試**（Infrastructure Layer）：

```java
@DataJpaTest
@Test
void should_save_and_retrieve_customer() {
    // Given
    Customer customer = createCustomer();

    // When
    customerRepository.save(customer);
    Customer retrieved = customerRepository.findById(customer.getId());

    // Then
    assertThat(retrieved).isEqualTo(customer);
}
```

**API 測試**（Interfaces Layer）：

```java
@WebMvcTest(CustomerController.class)
@Test
void should_update_customer_email() throws Exception {
    // When & Then
    mockMvc.perform(put("/api/v1/customers/123/email")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"new@example.com\"}"))
        .andExpect(status().isOk());
}
```

---

**文檔狀態**：✅ Accepted
**上次審查**：2025-10-24
**下次審查**：2026-01-24（每季）
