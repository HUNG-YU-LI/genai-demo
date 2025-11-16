# Test Generator Skill

**版本**: 1.0
**用途**: 自動生成符合測試金字塔的測試程式碼
**Kiro 映射**: Workflow Decomposition + Isolation Pattern + Single Responsibility

---

## Skill 描述

此 Skill 負責生成完整的測試套件，遵循測試金字塔原則（80% 單元測試、15% 整合測試、5% E2E 測試）。

**Kiro 技巧應用**:
1. **Workflow Decomposition**: 將測試生成分解為 Unit → Integration → E2E
2. **Isolation Pattern**: 每種測試類型獨立生成，互不干擾
3. **Single Responsibility**: 一個測試類別只測試一個職責

---

## 輸入規格

```yaml
input:
  targetClass: string          # 目標類別（例如：Order）
  targetType: enum             # 類別類型：aggregate | valueObject | service | repository
  testTypes: array             # 測試類型：unit | integration | e2e
  coverageTarget: number       # 覆蓋率目標（例如：80）
  mockStrategy: enum           # Mock 策略：minimal | moderate | extensive
```

---

## 生成模板

### 1. 單元測試（Unit Test）

```java
package solid.humank.genaidemo.domain.{context}.model.aggregate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*;

/**
 * {TargetClass} 單元測試
 *
 * Kiro Pattern: Isolation + Stateless
 * - 每個測試獨立執行（Isolation）
 * - 不依賴外部狀態（Stateless）
 */
@ExtendWith(MockitoExtension.class)
class {TargetClass}Test {

    /**
     * 測試命名: should_{ExpectedBehavior}_when_{StateUnderTest}
     * Kiro Pattern: Idempotency - 測試名稱清晰描述預期行為
     */
    @Test
    void should_create_aggregate_successfully_when_valid_input_provided() {
        // Given: 準備測試資料（Immutable Input）
        {TargetClass}Id id = {TargetClass}Id.generate();

        // When: 執行業務操作
        {TargetClass} aggregate = new {TargetClass}(id);

        // Then: 驗證結果（Boundary Control）
        assertThat(aggregate.getId()).isEqualTo(id);
        assertThat(aggregate.getStatus()).isEqualTo({TargetClass}Status.CREATED);
    }

    @Test
    void should_throw_exception_when_null_id_provided() {
        // Given: 無效輸入（Fail-Fast Pattern）
        {TargetClass}Id nullId = null;

        // When & Then: 驗證 Fail-Fast 行為
        assertThatThrownBy(() -> new {TargetClass}(nullId))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("不能為空");
    }

    @Test
    void should_collect_domain_event_when_aggregate_created() {
        // Given
        {TargetClass}Id id = {TargetClass}Id.generate();

        // When
        {TargetClass} aggregate = new {TargetClass}(id);

        // Then: 驗證事件驅動行為（Event-Driven Pattern）
        assertThat(aggregate.hasUncommittedEvents()).isTrue();
        assertThat(aggregate.getUncommittedEvents())
            .hasSize(1)
            .first()
            .isInstanceOf({TargetClass}CreatedEvent.class);
    }

    /**
     * 測試冪等性（Idempotency）
     * 相同輸入應產生相同結果
     */
    @Test
    void should_produce_same_result_when_same_input_provided_multiple_times() {
        // Given
        {TargetClass}Id id = {TargetClass}Id.of("TEST-123");

        // When: 多次執行
        {TargetClass} aggregate1 = new {TargetClass}(id);
        {TargetClass} aggregate2 = new {TargetClass}(id);

        // Then: 結果應相同（Idempotency）
        assertThat(aggregate1).isEqualTo(aggregate2);
    }
}
```

### 2. 整合測試（Integration Test）

```java
package solid.humank.genaidemo.infrastructure.{context}.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;

/**
 * {TargetClass}Repository 整合測試
 *
 * Kiro Pattern: Isolation + Boundary Control
 * - 使用 H2 資料庫隔離測試環境（Isolation）
 * - 測試資料存取邊界（Boundary Control）
 */
@DataJpaTest
@ActiveProfiles("test")
class {TargetClass}RepositoryIntegrationTest {

    @Autowired
    private {TargetClass}Repository repository;

    /**
     * 測試 CRUD 操作的冪等性
     */
    @Test
    void should_save_and_retrieve_aggregate_successfully() {
        // Given: 建立測試資料
        {TargetClass} aggregate = create{TargetClass}();

        // When: 儲存
        {TargetClass} saved = repository.save(aggregate);

        // Then: 驗證儲存結果（Boundary Control）
        assertThat(saved.getId()).isNotNull();

        // When: 查詢
        Optional<{TargetClass}> retrieved = repository.findById(saved.getId());

        // Then: 驗證查詢結果（Idempotency）
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getId()).isEqualTo(saved.getId());
    }

    /**
     * 測試交易邊界（Boundary Control）
     */
    @Test
    void should_rollback_transaction_when_exception_occurs() {
        // Given
        {TargetClass} aggregate = create{TargetClass}();

        // When: 執行會失敗的操作
        assertThatThrownBy(() -> {
            repository.save(aggregate);
            throw new RuntimeException("Simulated error");
        });

        // Then: 驗證交易回滾
        assertThat(repository.findAll()).isEmpty();
    }

    // Test Data Builder (Immutable Input Pattern)
    private {TargetClass} create{TargetClass}() {
        return new {TargetClass}({TargetClass}Id.generate());
    }
}
```

### 3. BDD 測試（Cucumber）

```gherkin
# src/test/resources/features/{context}/{target_class}.feature

Feature: {TargetClass} 業務行為測試

  Kiro Pattern: Event-Driven + Boundary Control
  - 使用業務語言描述行為（Ubiquitous Language）
  - 測試完整業務流程（End-to-End）

  Scenario: 成功建立聚合根
    Given 系統準備就緒
    When 使用有效的 {TargetClass}Id 建立聚合根
    Then 聚合根狀態應為 CREATED
    And 應該發布 {TargetClass}CreatedEvent 事件

  Scenario: 驗證輸入邊界（Boundary Control）
    Given 系統準備就緒
    When 使用 null 作為 {TargetClass}Id 建立聚合根
    Then 應該拋出 NullPointerException 異常
    And 異常訊息應包含 "不能為空"
```

---

## 使用方式

### 呼叫 Skill

```
/skill test-generator

請為以下類別生成測試：
- 目標類別: Order
- 類別類型: aggregate
- 測試類型: [unit, integration, e2e]
- 覆蓋率目標: 85%
- Mock 策略: minimal
```

### Skill 執行流程

```
1. 分析目標類別（反射/靜態分析）
   ↓
2. 識別公開方法和業務邏輯
   ↓
3. 生成單元測試（80%）
   - 正常路徑測試
   - 錯誤路徑測試
   - 邊界條件測試
   ↓
4. 生成整合測試（15%）
   - Repository 測試
   - API 端點測試
   ↓
5. 生成 E2E/BDD 測試（5%）
   - Cucumber 場景
   - 完整業務流程
   ↓
6. 執行測試並生成覆蓋率報告
```

---

## Kiro 技巧映射

| Kiro 技巧 | Claude Code 實作 | 具體好處 |
|----------|----------------|---------|
| **Workflow Decomposition** | 分層測試生成（Unit → Integration → E2E） | 職責清晰、易於維護 |
| **Isolation Pattern** | 每個測試獨立執行、使用 H2 資料庫 | 避免測試間干擾、可並行執行 |
| **Idempotency** | 測試可重複執行、結果一致 | 可靠的測試套件 |
| **Boundary Control** | 測試輸入驗證、交易邊界 | 防止無效狀態、確保資料一致性 |
| **Fail-Safe Pattern** | 異常測試、回滾測試 | 驗證錯誤處理邏輯 |
| **Stateless Handler** | 測試不保留狀態、每次重建資料 | 測試結果可預測 |

---

## 測試品質指標

### 自動檢查

1. **覆蓋率**: >= 目標覆蓋率（預設 80%）
2. **執行速度**:
   - 單元測試 < 50ms
   - 整合測試 < 500ms
   - E2E 測試 < 3s
3. **成功率**: >= 99%

### 驗證命令

```bash
./gradlew test jacocoTestReport  # 執行測試並生成覆蓋率報告
./gradlew cucumber               # 執行 BDD 測試
```

---

## 最佳實踐

### 1. 測試命名
```java
// ✅ Good: 清晰描述預期行為
should_create_order_successfully_when_valid_input_provided()

// ❌ Bad: 不清楚的命名
testOrder()
```

### 2. 測試隔離
```java
// ✅ Good: 每個測試建立自己的資料
@Test
void test1() {
    Order order = createOrder();
    // ...
}

// ❌ Bad: 共用狀態
private Order sharedOrder;  // 不同測試會互相影響
```

### 3. 測試資料建構
```java
// ✅ Good: 使用 Builder Pattern
Order order = OrderTestDataBuilder.anOrder()
    .withCustomerId(customerId)
    .withItems(3)
    .build();

// ❌ Bad: 硬編碼測試資料
Order order = new Order("HARD-CODED-ID", ...);
```

---

**版本歷史**:
- v1.0 (2025-11-16): 初始版本，支援 Unit/Integration/E2E 測試生成

**維護者**: QA Team
