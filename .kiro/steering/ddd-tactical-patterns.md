# DDD Tactical Patterns

## 概述

本文件定義了此專案中使用的 Domain-Driven Design tactical patterns。這些 patterns 對所有 domain model 實作都是強制性的。

**目的**: 提供 DDD pattern 實作的規則和快速檢查。
**詳細範例**: 請參閱 `.kiro/examples/ddd-patterns/` 以獲取完整的實作指南。

---

## Aggregate Root Pattern

### 必須遵循

- [ ] 繼承 `AggregateRoot` base class
- [ ] 使用帶有 metadata 的 `@AggregateRoot` annotation
- [ ] 使用 `collectEvent()` 方法收集 events
- [ ] 從 domain 不直接存取 repository
- [ ] Aggregates 是一致性邊界
- [ ] 只有 aggregate roots 可以從 repositories 取得

### 必須避免

- [ ] ❌ 直接暴露內部 collections
- [ ] ❌ 內部狀態的 setters
- [ ] ❌ 直接從 aggregate 發布 events
- [ ] ❌ 直接存取其他 aggregates

### 範例結構

```java
@AggregateRoot(name = "Order", boundedContext = "Order", version = "1.0")
public class Order extends AggregateRoot {
    private final OrderId id;
    private OrderStatus status;
    private List<OrderItem> items;

    public void submit() {
        // 1. Validate business rules
        validateOrderSubmission();

        // 2. Update state
        status = OrderStatus.PENDING;

        // 3. Collect domain event
        collectEvent(OrderSubmittedEvent.create(id, customerId, totalAmount));
    }

    private void validateOrderSubmission() {
        if (items.isEmpty()) {
            throw new BusinessRuleViolationException("Cannot submit empty order");
        }
    }
}
```

**完整範例**: #[[file:../examples/ddd-patterns/aggregate-root-examples.md]]

---

## Domain Events Pattern

### 必須遵循

- [ ] 使用 Record 實作以實現不可變性
- [ ] 實作 `DomainEvent` interface
- [ ] 使用帶有 `createEventMetadata()` 的 factory method
- [ ] Events 是不可變的並包含所有必要資料
- [ ] 使用過去式命名（例如，`OrderSubmitted`，而非 `SubmitOrder`）
- [ ] 在 event 中包含 aggregate ID

### 必須避免

- [ ] ❌ 可變的 event 欄位
- [ ] ❌ events 中的業務邏輯
- [ ] ❌ 對可變物件的引用
- [ ] ❌ 缺少 event metadata (eventId、occurredOn)

### 範例結構

```java
public record OrderSubmittedEvent(
    OrderId orderId,
    CustomerId customerId,
    Money totalAmount,
    int itemCount,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent {

    // Factory method with automatic metadata
    public static OrderSubmittedEvent create(
        OrderId orderId,
        CustomerId customerId,
        Money totalAmount,
        int itemCount
    ) {
        var metadata = DomainEvent.createEventMetadata();
        return new OrderSubmittedEvent(
            orderId, customerId, totalAmount, itemCount,
            metadata.eventId(), metadata.occurredOn()
        );
    }

    @Override
    public String getEventType() {
        return DomainEvent.getEventTypeFromClass(this.getClass());
    }

    @Override
    public String getAggregateId() {
        return orderId.getValue();
    }
}
```

**完整範例**: #[[file:../examples/ddd-patterns/domain-events-examples.md]]

---

## Value Objects Pattern

### 必須遵循

- [ ] 使用 Record 以實現不可變性
- [ ] 在 constructor 中驗證
- [ ] 沒有 setters
- [ ] 基於值實作 equals/hashCode
- [ ] 使驗證明確且快速失敗
- [ ] 使用描述性名稱（例如，`Email`，而非 `String`）

### 必須避免

- [ ] ❌ 可變欄位
- [ ] ❌ Setters 或修改方法
- [ ] ❌ 基於身份的相等性
- [ ] ❌ Primitive obsession（使用 String 而非 Email）

### 範例結構

```java
public record Email(String value) {

    public Email {
        // Validation in compact constructor
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        if (!value.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email format: " + value);
        }
    }

    // Factory method for clarity
    public static Email of(String value) {
        return new Email(value);
    }
}

public record Money(BigDecimal amount, Currency currency) {

    public Money {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null");
        }
    }

    // Business methods
    public Money add(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add different currencies");
        }
        return new Money(amount.add(other.amount), currency);
    }

    public static Money zero() {
        return new Money(BigDecimal.ZERO, Currency.getInstance("USD"));
    }
}
```

**完整範例**: #[[file:../examples/ddd-patterns/value-objects-examples.md]]

---

## Repository Pattern

### 必須遵循

- [ ] Interface 在 domain 層
- [ ] 實作在 infrastructure 層
- [ ] 回傳 domain objects，而非 entities
- [ ] 對單一結果使用 Optional
- [ ] 每個 aggregate root 一個 repository
- [ ] 只有 aggregate roots 有 repositories

### 必須避免

- [ ] ❌ 在 infrastructure 層的 repository 沒有 domain interface
- [ ] ❌ 暴露 JPA entities
- [ ] ❌ repository 中的業務邏輯
- [ ] ❌ 非 aggregate entities 的 repositories

### 範例結構

```java
// Domain layer: interface
package solid.humank.genaidemo.domain.order.repository;

public interface OrderRepository {
    Optional<Order> findById(OrderId orderId);
    List<Order> findByCustomerId(CustomerId customerId);
    Order save(Order order);
    void delete(OrderId orderId);
}

// Infrastructure layer: implementation
package solid.humank.genaidemo.infrastructure.order;

@Repository
public class JpaOrderRepository implements OrderRepository {

    private final OrderJpaRepository jpaRepository;
    private final OrderMapper mapper;

    @Override
    public Optional<Order> findById(OrderId orderId) {
        return jpaRepository.findById(orderId.getValue())
            .map(mapper::toDomain);
    }

    @Override
    public Order save(Order order) {
        OrderEntity entity = mapper.toEntity(order);
        OrderEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
```

**完整範例**: #[[file:../examples/ddd-patterns/repository-examples.md]]

---

## Domain Services Pattern

### 使用時機

- [ ] 不自然適合 aggregate 的業務邏輯
- [ ] 涉及多個 aggregates 的操作
- [ ] 複雜的計算或演算法
- [ ] 與外部 domain 概念的整合

### 必須遵循

- [ ] 無狀態 services
- [ ] Interface 在 domain 層
- [ ] 實作可以在 domain 或 infrastructure
- [ ] 方法名稱使用 domain 語言

### 範例結構

```java
// Domain layer
package solid.humank.genaidemo.domain.pricing.service;

public interface PricingService {
    Money calculateOrderTotal(Order order, Customer customer);
    Money applyPromotions(Money amount, List<Promotion> promotions);
}

// Domain layer implementation
@Service
public class PricingServiceImpl implements PricingService {

    @Override
    public Money calculateOrderTotal(Order order, Customer customer) {
        Money subtotal = order.calculateSubtotal();
        Money discount = customer.getDiscountRate().apply(subtotal);
        return subtotal.subtract(discount);
    }
}
```

---

## Application Services Pattern

### 職責

- [ ] 編排 use cases
- [ ] 從 repositories 載入 aggregates
- [ ] 呼叫 aggregate 方法
- [ ] 儲存 aggregates
- [ ] 發布 domain events
- [ ] Transaction 管理

### 必須遵循

- [ ] 精簡的 application services（僅編排）
- [ ] application services 中沒有業務邏輯
- [ ] 使用 `@Transactional` 以確保一致性
- [ ] 在成功的 transaction 之後發布 events

### 範例結構

```java
@Service
@Transactional
public class OrderApplicationService {

    private final OrderRepository orderRepository;
    private final DomainEventApplicationService eventService;

    public void submitOrder(SubmitOrderCommand command) {
        // 1. Load aggregate
        Order order = orderRepository.findById(command.orderId())
            .orElseThrow(() -> new OrderNotFoundException(command.orderId()));

        // 2. Execute business operation (aggregate handles logic)
        order.submit();

        // 3. Save aggregate
        orderRepository.save(order);

        // 4. Publish collected events
        eventService.publishEventsFromAggregate(order);
    }
}
```

---

## Bounded Context Pattern

### 必須遵循

- [ ] 每個 context 是獨立的
- [ ] 透過 domain events 通訊
- [ ] contexts 之間沒有直接依賴
- [ ] Shared kernel 在 `domain/shared/`
- [ ] Context map 已記錄

### Package 結構

```text
domain/
├── customer/          # Customer bounded context
│   ├── model/
│   ├── events/
│   ├── repository/
│   └── service/
├── order/             # Order bounded context
│   ├── model/
│   ├── events/
│   ├── repository/
│   └── service/
└── shared/            # Shared kernel
    └── valueobject/
```

---

## Event Handlers Pattern

### 必須遵循

- [ ] 繼承 `AbstractDomainEventHandler<T>`
- [ ] 使用 `@Component` 標註
- [ ] 實作冪等性檢查
- [ ] 使用 `@TransactionalEventListener(phase = AFTER_COMMIT)`
- [ ] 優雅地處理錯誤

### 範例結構

```java
@Component
public class OrderSubmittedEventHandler
    extends AbstractDomainEventHandler<OrderSubmittedEvent> {

    @Override
    @Transactional
    public void handle(OrderSubmittedEvent event) {
        // 1. Check idempotency
        if (isEventAlreadyProcessed(event.getEventId())) {
            return;
        }

        // 2. Execute cross-aggregate logic
        inventoryService.reserveItems(event.orderId());
        notificationService.sendOrderConfirmation(event.customerId());

        // 3. Mark as processed
        markEventAsProcessed(event.getEventId());
    }

    @Override
    public Class<OrderSubmittedEvent> getSupportedEventType() {
        return OrderSubmittedEvent.class;
    }
}
```

---

## 驗證

### 架構合規性

```bash
./gradlew archUnit  # Verify DDD patterns compliance
```

### ArchUnit 規則

```java
@ArchTest
static final ArchRule aggregateRootRules = classes()
    .that().areAnnotatedWith(AggregateRoot.class)
    .should().resideInAPackage("..domain..model.aggregate..");

@ArchTest
static final ArchRule domainEventRules = classes()
    .that().implement(DomainEvent.class)
    .should().beRecords();

@ArchTest
static final ArchRule repositoryRules = classes()
    .that().haveSimpleNameEndingWith("Repository")
    .and().areInterfaces()
    .should().resideInAPackage("..domain..repository..");
```

---

## 快速參考

| Pattern | 關鍵規則 | 位置 |
|---------|----------|----------|
| Aggregate Root | 收集 events，不發布 | `domain/{context}/model/aggregate/` |
| Domain Event | 不可變 Record | `domain/{context}/events/` |
| Value Object | 帶驗證的不可變 Record | `domain/{context}/model/valueobject/` |
| Repository | Interface 在 domain，impl 在 infra | Interface: `domain/{context}/repository/`<br>Impl: `infrastructure/{context}/` |
| Domain Service | 無狀態，domain 邏輯 | `domain/{context}/service/` |
| Application Service | 僅編排 | `application/{context}/` |

---

## 相關文件

- **核心原則**: #[[file:core-principles.md]]
- **設計原則**: #[[file:design-principles.md]]
- **架構約束**: #[[file:architecture-constraints.md]]
- **DDD 範例**: #[[file:../examples/ddd-patterns/]]

---

**文件版本**: 1.0
**最後更新**: 2025-01-17
**負責人**: Architecture Team
