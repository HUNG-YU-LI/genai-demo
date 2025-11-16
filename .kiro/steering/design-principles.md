# 設計 Principles

## 概覽

本文件定義指導我們編寫程式碼的設計原則，專注於 Extreme Programming (XP) 價值觀和物件導向設計原則。

**Purpose**: 提供設計決策的快速檢查和規則。
**Detailed Examples**: 查看 `.kiro/examples/design-patterns/` 和 `.kiro/examples/xp-practices/` 以獲取完整指南。

---

## Extreme Programming (XP) Core Values

### Simplicity ⭐

- [ ] 做可能有效的最簡單的事情
- [ ] YAGNI (You Aren't Gonna Need It)
- [ ] 移除重複
- [ ] 最小化類別和方法的數量

### Communication

- [ ] 程式碼清楚傳達意圖
- [ ] 使用領域的 ubiquitous language
- [ ] Pair programming 以分享知識
- [ ] Collective code ownership

### Feedback

- [ ] Test-first development
- [ ] Continuous integration
- [ ] 短迭代
- [ ] 客戶參與

### Courage

- [ ] 需要時進行 refactor
- [ ] 必要時丟棄程式碼
- [ ] 承認不知道的事情
- [ ] 接受反饋

**Detailed XP Practices**: #[[file:../examples/xp-practices/]]

---

## Tell, Don't Ask ⭐

**Principle**: 物件應該告訴其他物件做什麼，而不是詢問它們的狀態並做決策。

### Must Follow

- [ ] 行為應該在擁有資料的物件中
- [ ] 避免 getter 鏈
- [ ] 方法應該做工作，而不只是返回資料
- [ ] 將決策推入物件中

### Quick Check

```java
// ❌ BAD: Asking for state and making decisions
if (order.getStatus() == OrderStatus.CREATED) {
    order.setStatus(OrderStatus.PENDING);
    order.setUpdatedAt(LocalDateTime.now());
}

// ✅ GOOD: Telling the object what to do
order.submit();  // Object handles its own state transitions
```

**Full Examples**: #[[file:../examples/design-patterns/tell-dont-ask-examples.md]]

---

## Law of Demeter (Principle of Least Knowledge)

**Principle**: 只與你的直接朋友交談，不要與陌生人交談。

### Must Follow

- [ ] 只呼叫以下物件的方法：
  - 物件本身 (this)
  - 作為參數傳遞的物件
  - 在本地創建的物件
  - 直接組件物件
- [ ] 避免跨物件的方法鏈
- [ ] One dot rule（fluent APIs 除外）

### Quick Check

```java
// ❌ BAD: Violates Law of Demeter
customer.getAddress().getCity().getPostalCode();

// ✅ GOOD: Ask the object directly
customer.getPostalCode();  // Customer knows how to get it
```

**Full Examples**: #[[file:../examples/design-patterns/law-of-demeter-examples.md]]

---

## Composition Over Inheritance

**Principle**: 偏好物件組合而非類別繼承。

### Must Follow

- [ ] 使用 composition 處理 "has-a" 關係
- [ ] 只在 "is-a" 關係時使用 inheritance
- [ ] 優先使用 interfaces 而非 abstract classes
- [ ] 保持繼承層次結構淺（最多 2-3 層）

### Quick Check

```java
// ❌ BAD: Inheritance for code reuse
class OrderWithDiscount extends Order {
    // Inheriting just to add discount behavior
}

// ✅ GOOD: Composition
class Order {
    private DiscountStrategy discountStrategy;

    public Money calculateTotal() {
        Money subtotal = calculateSubtotal();
        return discountStrategy.apply(subtotal);
    }
}
```

**Full Examples**: #[[file:../examples/design-patterns/composition-over-inheritance-examples.md]]

---

## SOLID Principles

### Single Responsibility Principle (SRP)

**Principle**: 一個類別應該只有一個變更的理由。

#### Must Follow

- [ ] 每個類別只有一個清楚的職責
- [ ] 方法做好一件事
- [ ] 分離業務邏輯和基礎設施
- [ ] 將大型類別拆分為專注的小類別

#### Quick Check

```java
// ❌ BAD: Multiple responsibilities
class OrderService {
    void processOrder() { }
    void sendEmail() { }
    void updateInventory() { }
}

// ✅ GOOD: Single responsibility
class OrderProcessingService {
    void processOrder() { }
}
```

---

### Open/Closed Principle (OCP)

**Principle**: 對擴展開放，對修改封閉。

#### Must Follow

- [ ] 使用 interfaces 和 abstract classes
- [ ] Strategy pattern 處理變化的行為
- [ ] 避免修改現有程式碼
- [ ] 透過新實作進行擴展

---

### Liskov Substitution Principle (LSP)

**Principle**: 子類型必須可以替代其基礎類型。

#### Must Follow

- [ ] 子類別遵守父類別的契約
- [ ] 不要加強前置條件
- [ ] 不要弱化後置條件
- [ ] 保持不變量

---

### Interface Segregation Principle (ISP)

**Principle**: 客戶端不應該依賴它們不使用的介面。

#### Must Follow

- [ ] 保持介面小而專注
- [ ] 將大型介面拆分為較小的介面
- [ ] 基於角色的介面
- [ ] 避免 "fat" interfaces

#### Quick Check

```java
// ❌ BAD: Fat interface
interface OrderOperations {
    void create(); void update(); void delete();
    void approve(); void ship(); void cancel();
}

// ✅ GOOD: Segregated interfaces
interface OrderCreation { void create(); }
interface OrderManagement { void update(); void cancel(); }
interface OrderFulfillment { void approve(); void ship(); }
```

---

### Dependency Inversion Principle (DIP)

**Principle**: 依賴於抽象，而不是具體實作。

#### Must Follow

- [ ] 高層模組不依賴低層模組
- [ ] 兩者都依賴抽象 (interfaces)
- [ ] 使用 dependency injection
- [ ] 面向介面編程，而不是實作

#### Quick Check

```java
// ❌ BAD: Depends on concrete implementation
class OrderService {
    private PostgresOrderRepository repository = new PostgresOrderRepository();
}

// ✅ GOOD: Depends on abstraction
class OrderService {
    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }
}
```

**Full Examples**: #[[file:../examples/design-patterns/dependency-injection-examples.md]]

---

## Four Rules of Simple 設計 (Kent Beck)

### Priority Order

1. **Passes all tests** - 程式碼必須正確運作
2. **Reveals intention** - 程式碼清楚溝通
3. **No duplication** - DRY principle
4. **Fewest elements** - 最少的類別和方法

### Must Follow

- [ ] 測試通過（正確性優先）
- [ ] 清楚的命名和結構
- [ ] 提取重複
- [ ] 移除不必要的抽象

**Detailed Guide**: #[[file:../examples/xp-practices/simple-design-examples.md]]

---

## 設計 Smells to Avoid

### Code Smells

- [ ] ❌ 長方法 (> 20 lines)
- [ ] ❌ 大型類別 (> 200 lines)
- [ ] ❌ 長參數列表 (> 3 parameters)
- [ ] ❌ Primitive obsession
- [ ] ❌ Feature envy
- [ ] ❌ Data clumps

### 設計 Smells

- [ ] ❌ Rigidity（難以變更）
- [ ] ❌ Fragility（在許多地方損壞）
- [ ] ❌ Immobility（難以重用）
- [ ] ❌ Viscosity（做錯誤的事情更容易）
- [ ] ❌ Needless complexity
- [ ] ❌ Needless repetition

**Refactoring Guide**: #[[file:../examples/design-patterns/design-smells-refactoring.md]]

---

## Quick Reference Card

| Principle | Key Question | Red Flag |
|-----------|-------------|----------|
| Tell, Don't Ask | "我是否正在詢問資料來做決策？" | Getter chains, if-else on state |
| Law of Demeter | "我是否正在與陌生人交談？" | Multiple dots: `a.b().c().d()` |
| SRP | "這個類別是否只有一個變更的理由？" | Class does multiple things |
| OCP | "我能否在不修改的情況下擴展？" | Modifying existing code for new features |
| LSP | "我能否用子類別替代父類別？" | Subclass breaks parent contract |
| ISP | "客戶端是否使用所有介面方法？" | Large interfaces with unused methods |
| DIP | "我是否依賴於抽象？" | `new ConcreteClass()` in business logic |
| Composition | "這真的是 'is-a' 關係嗎？" | Inheritance for code reuse |

---

## Validation

### Code Review Checklist

- [ ] 程式碼是否遵循 Tell, Don't Ask？
- [ ] 是否有任何 Law of Demeter 違規？
- [ ] Composition 是否適當使用？
- [ ] 類別是否具有單一職責？
- [ ] 依賴是否正確注入？
- [ ] 介面是否小而專注？

### Automated Checks

```bash
# Check for design violations
./gradlew archUnit

# Check for code smells
./gradlew pmdMain

# Check complexity
./gradlew checkstyleMain
```

---

## Related Documentation

- **Core Principles**: #[[file:core-principles.md]]
- **DDD Patterns**: #[[file:ddd-tactical-patterns.md]]
- **Code Quality Checklist**: #[[file:code-quality-checklist.md]]
- **Design Pattern Examples**: #[[file:../examples/design-patterns/]]
- **XP Practices**: #[[file:../examples/xp-practices/]]

---

**Document Version**: 1.0
**Last Updated**: 2025-01-17
**Owner**: Architecture Team
