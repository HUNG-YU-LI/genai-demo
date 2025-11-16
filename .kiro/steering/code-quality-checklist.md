# Code Quality Checklist

## Overview

本文件提供程式碼品質標準的完整檢查清單。在開發和程式碼審查期間將此作為快速參考使用。

**Purpose**: 日常開發和程式碼審查的快速檢查清單。
**Detailed Examples**: 查看 `.kiro/examples/code-patterns/` 以獲取完整指南。

---

## Naming Conventions

### Must Follow

- [ ] Classes: PascalCase (e.g., `OrderService`, `CustomerRepository`)
- [ ] Methods: camelCase with verb-noun pattern (e.g., `findCustomerById`, `calculateTotal`)
- [ ] Variables: camelCase, descriptive names (e.g., `customerEmail`, `orderTotal`)
- [ ] Constants: UPPER_SNAKE_CASE (e.g., `MAX_RETRY_ATTEMPTS`, `DEFAULT_TIMEOUT`)
- [ ] Packages: lowercase, singular nouns (e.g., `domain.order.model`)
- [ ] Test methods: should_expectedBehavior_when_condition

### Must Avoid

- [ ] ❌ 縮寫 (e.g., `cust` instead of `customer`)
- [ ] ❌ 單字母變數（迴圈計數器除外）
- [ ] ❌ Hungarian notation (e.g., `strName`, `intCount`)
- [ ] ❌ 無意義的名稱 (e.g., `data`, `info`, `manager`)

---

## Error Handling

### Must Follow

- [ ] 使用特定的 exception types
- [ ] 在 exceptions 中包含錯誤上下文
- [ ] 使用結構化資料記錄錯誤
- [ ] 對 closeable resources 使用 try-with-resources
- [ ] 在適當層級處理錯誤

### Must Avoid

- [ ] ❌ 空的 catch blocks
- [ ] ❌ 通用的 `catch (Exception e)`
- [ ] ❌ 吞沒 exceptions
- [ ] ❌ 將 exceptions 用於控制流

### Quick Check

```java
// ✅ GOOD: Specific exception with context
throw new CustomerNotFoundException(
    "Customer not found",
    Map.of("customerId", customerId)
);

// ❌ BAD: Generic exception
throw new RuntimeException("Error");
```

**Detailed Guide**: #[[file:../examples/code-patterns/error-handling.md]]

---

## API Design

### Must Follow

- [ ] RESTful URL conventions (`/api/v1/customers`)
- [ ] Proper HTTP methods (GET, POST, PUT, DELETE)
- [ ] 一致的回應格式
- [ ] 使用 `@Valid` 進行輸入驗證
- [ ] Proper HTTP status codes

### HTTP Status Codes

- **200 OK**: Successful GET, PUT, PATCH
- **201 Created**: Successful POST
- **204 No Content**: Successful DELETE
- **400 Bad Request**: Validation errors
- **404 Not Found**: Resource not found
- **409 Conflict**: Business rule violation
- **500 Internal Server Error**: System errors

**Detailed Guide**: #[[file:../examples/code-patterns/api-design.md]]

---

## Security

### Must Follow

- [ ] 所有端點的輸入驗證
- [ ] Parameterized queries（無字串串接）
- [ ] 輸出編碼以防止 XSS
- [ ] 受保護端點的身份驗證
- [ ] 授權檢查
- [ ] 敏感資料加密

### Must Avoid

- [ ] ❌ SQL injection 漏洞
- [ ] ❌ XSS 漏洞
- [ ] ❌ 硬編碼的憑證
- [ ] ❌ 日誌中的敏感資料

### Quick Check

```java
// ✅ GOOD: Parameterized query
@Query("SELECT c FROM Customer c WHERE c.email = :email")
Optional<Customer> findByEmail(@Param("email") String email);

// ❌ BAD: String concatenation (SQL injection risk)
String query = "SELECT * FROM customers WHERE email = '" + email + "'";
```

**Detailed Guide**: #[[file:../examples/code-patterns/security-patterns.md]]

---

## Performance

### Must Follow

- [ ] 資料庫查詢優化
- [ ] 對頻繁查詢的欄位進行適當索引
- [ ] 對大型結果集使用分頁
- [ ] 對頻繁訪問的資料實作快取
- [ ] 對長時間運行的操作使用非同步處理
- [ ] 避免 N+1 query 問題

### Quick Check

```java
// ✅ GOOD: Use JOIN FETCH to avoid N+1
@Query("SELECT o FROM Order o JOIN FETCH o.items WHERE o.id = :id")
Optional<Order> findByIdWithItems(@Param("id") String id);

// ❌ BAD: N+1 query problem
List<Order> orders = orderRepository.findAll();
orders.forEach(order -> order.getItems().size()); // N+1!
```

**Detailed Guide**: #[[file:../examples/code-patterns/performance-optimization.md]]

---

## Code Structure

### Method Length

- [ ] 方法 < 20 lines
- [ ] 每個方法一個抽象層級
- [ ] 將複雜邏輯提取到單獨的方法中

### Class Size

- [ ] 類別 < 200 lines
- [ ] 每個類別一個職責
- [ ] 將大型類別拆分為專注的小類別

### Parameter Lists

- [ ] 方法有 ≤ 3 個參數
- [ ] 對於 > 3 個參數使用 parameter objects
- [ ] 對複雜物件使用 builder pattern

---

## Documentation

### Must Follow

- [ ] Public APIs 有 Javadoc
- [ ] 複雜邏輯有行內註解
- [ ] 重大變更時更新 README
- [ ] API 文件已更新

### Javadoc Standards

```java
/**

 * Submits an order for processing.
 *
 * @param command the order submission command
 * @return the submitted order
 * @throws OrderNotFoundException if order not found
 * @throws BusinessRuleViolationException if business rules violated

 */
public Order submitOrder(SubmitOrderCommand command) {
    // Implementation
}
```

---

## Code Review Checklist

### Functionality

- [ ] 程式碼正確實作需求
- [ ] 邊界情況處理得當
- [ ] 錯誤條件已處理
- [ ] 業務規則已驗證

### Design

- [ ] 遵循 SOLID principles
- [ ] 遵循 Tell, Don't Ask
- [ ] 無 Law of Demeter 違規
- [ ] 適當使用 design patterns

### Testing

- [ ] 業務邏輯的 unit tests
- [ ] 基礎設施的 integration tests
- [ ] 測試覆蓋率 > 80%
- [ ] 測試清楚且可維護

### Security

- [ ] 已實作輸入驗證
- [ ] 無安全漏洞
- [ ] 敏感資料受保護
- [ ] 身份驗證/授權正確

### Performance

- [ ] 無明顯的效能問題
- [ ] 資料庫查詢已優化
- [ ] 適當使用快取
- [ ] 無記憶體洩漏

### Maintainability

- [ ] 程式碼可讀且清楚
- [ ] 命名具描述性
- [ ] 無程式碼重複
- [ ] 文件已更新

**Detailed Review Guide**: #[[file:../examples/process/code-review-guide.md]]

---

## Validation Commands

### Code Quality

```bash
./gradlew test jacocoTestReport  # Check test coverage
./gradlew pmdMain                # Check code smells
./gradlew checkstyleMain         # Check code style
./gradlew spotbugsMain           # Find bugs
```

### Architecture

```bash
./gradlew archUnit               # Verify architecture rules
```

### Security

```bash
./gradlew dependencyCheckAnalyze # Check dependencies
```

---

## Quick Reference

| Category | Key Check | Tool |
|----------|-----------|------|
| Naming | 描述性、一致性 | Code review |
| Error Handling | 具上下文的特定 exceptions | PMD |
| API Design | RESTful, proper status codes | Code review |
| Security | Input validation, no SQL injection | SpotBugs |
| Performance | No N+1, proper indexing | Code review |
| Testing | Coverage > 80% | JaCoCo |

---

## Related Documentation

- **Core Principles**: #[[file:core-principles.md]]
- **Design Principles**: #[[file:design-principles.md]]
- **DDD Patterns**: #[[file:ddd-tactical-patterns.md]]
- **Code Pattern Examples**: #[[file:../examples/code-patterns/]]

---

**Document Version**: 1.0
**Last Updated**: 2025-01-17
**Owner**: Development Team
