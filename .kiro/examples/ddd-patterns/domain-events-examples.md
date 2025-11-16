# Domain Events - 詳細範例

## 原則概述

**Domain Events** 是領域中發生某事的不可變記錄。它們代表聚合中狀態變更的事實，並實現 bounded contexts 之間的鬆耦合。

## 核心概念

- **不可變性（Immutability）**：事件建立後無法變更
- **過去式（Past Tense）**：事件名稱使用過去式（例如 `OrderSubmitted`，而非 `SubmitOrder`）
- **完整資訊（Complete Information）**：事件包含所有必要資料
- **後設資料（Metadata）**：包含 eventId、occurredOn、eventType、aggregateId

**相關標準**：[Domain Events Standards](../../steering/domain-events.md)

---

## 基本 Domain Event Pattern（正式程式碼）

### 使用 Java Records

這是我們正式程式碼庫中實際的 OrderSubmittedEvent：

```java
/**
 * Order submitted event
 * Using record for automatic immutability and basic functionality
 */
public record OrderSubmittedEvent(
        OrderId orderId,
        String customerId,
        Money totalAmount,
        int itemCount,
        UUID eventId,
        LocalDateTime occurredOn) implements DomainEvent {

    /**
     * Factory method with automatic eventId and occurredOn
     */
    public static OrderSubmittedEvent create(
            OrderId orderId, String customerId, Money totalAmount, int itemCount) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new OrderSubmittedEvent(orderId, customerId, totalAmount, itemCount,
                metadata.eventId(), metadata.occurredOn());
    }

    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public LocalDateTime getOccurredOn() {
        return occurredOn;
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

---

## 完整 Domain Event 範例（正式程式碼）

### 範例 1：CustomerCreatedEvent

這是我們正式程式碼庫中實際的 CustomerCreatedEvent：

```java
/**
 * Customer created event
 * Published when a new customer is created
 *
 * Using record for automatic immutability and basic functionality
 */
public record CustomerCreatedEvent(
        CustomerId customerId,
        CustomerName customerName,
        Email email,
        MembershipLevel membershipLevel,
        UUID eventId,
        LocalDateTime occurredOn) implements DomainEvent {

    /**
     * Factory method with automatic eventId and occurredOn
     */
    public static CustomerCreatedEvent create(
            CustomerId customerId,
            CustomerName customerName,
            Email email,
            MembershipLevel membershipLevel) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new CustomerCreatedEvent(customerId, customerName, email, membershipLevel,
                metadata.eventId(), metadata.occurredOn());
    }

    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }

    @Override
    public String getEventType() {
        return DomainEvent.getEventTypeFromClass(this.getClass());
    }

    @Override
    public String getAggregateId() {
        return customerId.getValue();
    }
}
```

### 範例 2：CustomerProfileUpdatedEvent

```java
/**
 * Customer profile updated event
 * Published when customer profile information is updated
 */
public record CustomerProfileUpdatedEvent(
        CustomerId customerId,
        CustomerName newName,
        Email newEmail,
        Phone newPhone,
        UUID eventId,
        LocalDateTime occurredOn) implements DomainEvent {

    /**
     * Factory method with automatic eventId and occurredOn
     */
    public static CustomerProfileUpdatedEvent create(
            CustomerId customerId,
            CustomerName newName,
            Email newEmail,
            Phone newPhone) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new CustomerProfileUpdatedEvent(customerId, newName, newEmail, newPhone,
                metadata.eventId(), metadata.occurredOn());
    }

    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }

    @Override
    public String getEventType() {
        return DomainEvent.getEventTypeFromClass(this.getClass());
    }

    @Override
    public String getAggregateId() {
        return customerId.getValue();
    }
}
```

### 範例 3：OrderCreatedEvent

```java
/**
 * Order created event
 * Published when a new order is created
 */
public record OrderCreatedEvent(
        OrderId orderId,
        String customerId,
        Money totalAmount,
        List<OrderItem> items,
        UUID eventId,
        LocalDateTime occurredOn) implements DomainEvent {

    /**
     * Factory method with automatic eventId and occurredOn
     */
    public static OrderCreatedEvent create(
            OrderId orderId,
            String customerId,
            Money totalAmount,
            List<OrderItem> items) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new OrderCreatedEvent(orderId, customerId, totalAmount,
                new ArrayList<>(items), // Defensive copy
                metadata.eventId(), metadata.occurredOn());
    }

    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public LocalDateTime getOccurredOn() {
        return occurredOn;
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

### 範例 4：MembershipLevelUpgradedEvent

```java
/**
 * Membership level upgraded event
 * Published when customer's membership level is upgraded
 */
public record MembershipLevelUpgradedEvent(
        CustomerId customerId,
        MembershipLevel oldLevel,
        MembershipLevel newLevel,
        UUID eventId,
        LocalDateTime occurredOn) implements DomainEvent {

    /**
     * Factory method with automatic eventId and occurredOn
     */
    public static MembershipLevelUpgradedEvent create(
            CustomerId customerId,
            MembershipLevel oldLevel,
            MembershipLevel newLevel) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new MembershipLevelUpgradedEvent(customerId, oldLevel, newLevel,
                metadata.eventId(), metadata.occurredOn());
    }

    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }

    @Override
    public String getEventType() {
        return DomainEvent.getEventTypeFromClass(this.getClass());
    }

    @Override
    public String getAggregateId() {
        return customerId.getValue();
    }
}
```

---

## 在 Aggregates 中收集事件（正式程式碼）

### Order Aggregate

```java
@AggregateRoot(name = "Order", boundedContext = "Order", version = "1.0")
public class Order extends solid.humank.genaidemo.domain.common.aggregate.AggregateRoot {

    private final OrderId id;
    private final CustomerId customerId;
    private OrderStatus status;
    private final List<OrderItem> items;
    private Money totalAmount;

    /**
     * Constructor - collects OrderCreatedEvent
     */
    public Order(OrderId orderId, CustomerId customerId, String shippingAddress) {
        this.id = orderId;
        this.customerId = customerId;
        this.items = new ArrayList<>();
        this.status = OrderStatus.CREATED;
        this.totalAmount = Money.zero();

        // Collect domain event
        collectEvent(OrderCreatedEvent.create(
            this.id, this.customerId.toString(), Money.zero(), List.of()));
    }

    /**
     * Submit order - collects OrderSubmittedEvent
     */
    public void submit() {
        validateOrderSubmission();

        // Update state
        status = OrderStatus.PENDING;

        // Collect domain event
        collectEvent(OrderSubmittedEvent.create(
            this.id, this.customerId.toString(), this.totalAmount, this.items.size()));
    }

    /**
     * Add item - collects OrderItemAddedEvent
     */
    public void addItem(String productId, String productName, int quantity, Money price) {
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException(
                "Cannot add items to an order that is not in CREATED state");
        }

        OrderItem item = new OrderItem(productId, productName, quantity, price);
        items.add(item);
        totalAmount = totalAmount.add(item.getSubtotal());

        // Collect domain event
        collectEvent(OrderItemAddedEvent.create(this.id, productId, quantity, price));
    }
}
```

### Customer Aggregate

```java
@AggregateRoot(name = "Customer", boundedContext = "Customer", version = "2.0")
public class Customer implements AggregateRootInterface {

    private final CustomerId id;
    private CustomerName name;
    private Email email;
    private Phone phone;
    private MembershipLevel membershipLevel;

    /**
     * Constructor - collects CustomerCreatedEvent
     */
    public Customer(
            CustomerId id,
            CustomerName name,
            Email email,
            Phone phone,
            Address address,
            MembershipLevel membershipLevel,
            LocalDate birthDate,
            LocalDateTime registrationDate) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.membershipLevel = membershipLevel;

        // Collect domain event
        collectEvent(CustomerCreatedEvent.create(id, name, email, membershipLevel));
    }

    /**
     * Update profile - collects CustomerProfileUpdatedEvent
     */
    public void updateProfile(CustomerName newName, Email newEmail, Phone newPhone) {
        validateProfileUpdate(newName, newEmail, newPhone);

        boolean hasChanges = !Objects.equals(this.name, newName) ||
                !Objects.equals(this.email, newEmail) ||
                !Objects.equals(this.phone, newPhone);

        if (hasChanges) {
            this.name = newName;
            this.email = newEmail;
            this.phone = newPhone;

            // Collect domain event
            collectEvent(CustomerProfileUpdatedEvent.create(this.id, newName, newEmail, newPhone));
        }
    }

    /**
     * Upgrade membership - collects MembershipLevelUpgradedEvent
     */
    public void upgradeMembershipLevel(MembershipLevel newLevel) {
        validateMembershipUpgrade(newLevel);

        MembershipLevel oldLevel = this.membershipLevel;
        this.membershipLevel = newLevel;

        // Collect domain event
        collectEvent(MembershipLevelUpgradedEvent.create(this.id, oldLevel, newLevel));
    }
}
```

---

## 在 Application Services 中發佈事件（正式程式碼）

```java
@Service
@Transactional
public class OrderApplicationService {

    private final OrderRepository orderRepository;
    private final DomainEventApplicationService eventService;

    public OrderApplicationService(
            OrderRepository orderRepository,
            DomainEventApplicationService eventService) {
        this.orderRepository = orderRepository;
        this.eventService = eventService;
    }

    /**
     * Submit order - publishes collected events
     */
    public void submitOrder(SubmitOrderCommand command) {
        // 1. Load aggregate
        Order order = orderRepository.findById(command.orderId())
            .orElseThrow(() -> new OrderNotFoundException(command.orderId()));

        // 2. Execute business operation (aggregate collects events)
        order.submit();

        // 3. Save aggregate
        orderRepository.save(order);

        // 4. Publish collected events
        eventService.publishEventsFromAggregate(order);
    }
}
```

---

## Event Handling（正式程式碼）

### Event Handler 實作

```java
@Component
public class OrderSubmittedEventHandler
    extends AbstractDomainEventHandler<OrderSubmittedEvent> {

    private final InventoryService inventoryService;
    private final NotificationService notificationService;
    private final ProcessedEventRepository processedEventRepository;

    @Override
    @Transactional
    public void handle(OrderSubmittedEvent event) {
        // 1. Check idempotency
        if (isEventAlreadyProcessed(event.getEventId())) {
            return;
        }

        try {
            // 2. Execute cross-aggregate logic
            inventoryService.reserveItems(event.orderId());
            notificationService.sendOrderConfirmation(event.customerId());

            // 3. Mark event as processed
            markEventAsProcessed(event.getEventId());

        } catch (Exception e) {
            logger.error("Failed to process OrderSubmittedEvent", e);
            throw new DomainEventProcessingException("Failed to process order submission", e);
        }
    }

    @Override
    public Class<OrderSubmittedEvent> getSupportedEventType() {
        return OrderSubmittedEvent.class;
    }

    private boolean isEventAlreadyProcessed(UUID eventId) {
        return processedEventRepository.existsByEventId(eventId);
    }

    private void markEventAsProcessed(UUID eventId) {
        processedEventRepository.save(new ProcessedEvent(eventId, Instant.now()));
    }
}
```

---

## 關鍵 Patterns

### 1. Factory Method Pattern

```java
public record OrderSubmittedEvent(...) implements DomainEvent {

    // Factory method automatically generates metadata
    public static OrderSubmittedEvent create(
            OrderId orderId, String customerId, Money totalAmount, int itemCount) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new OrderSubmittedEvent(orderId, customerId, totalAmount, itemCount,
                metadata.eventId(), metadata.occurredOn());
    }
}
```

### 2. Event Collection（收集事件，而非發佈）

```java
// ✅ Good: Collect events in aggregate
public void submit() {
    validateOrderSubmission();
    status = OrderStatus.PENDING;
    collectEvent(OrderSubmittedEvent.create(id, customerId, totalAmount, items.size()));
}

// ❌ Bad: Publishing directly from aggregate
public void submit() {
    validateOrderSubmission();
    status = OrderStatus.PENDING;
    eventPublisher.publish(new OrderSubmittedEvent(...)); // Wrong!
}
```

### 3. Event Publishing in Application Service

```java
@Service
@Transactional
public class OrderApplicationService {

    public void submitOrder(SubmitOrderCommand command) {
        // 1. Load aggregate
        Order order = orderRepository.findById(command.orderId())
            .orElseThrow(() -> new OrderNotFoundException(command.orderId()));

        // 2. Execute business operation (events collected)
        order.submit();

        // 3. Save aggregate
        orderRepository.save(order);

        // 4. Publish collected events
        eventService.publishEventsFromAggregate(order);
    }
}
```

### 4. Idempotent Event Handling（冪等事件處理）

```java
@Override
@Transactional
public void handle(OrderSubmittedEvent event) {
    // Check if event was already processed
    if (isEventAlreadyProcessed(event.getEventId())) {
        return; // Skip processing
    }

    // Process event
    inventoryService.reserveItems(event.orderId());

    // Mark as processed
    markEventAsProcessed(event.getEventId());
}
```

---

## 最佳實踐

### ✅ DO

1. **使用過去式** - `OrderSubmitted`，而非 `SubmitOrder`
2. **使用 Records** - 自動不可變性
3. **包含所有必要資料** - 事件應該是自包含的
4. **使用 factory methods** - 自動產生後設資料
5. **收集，不發佈** - Aggregates 收集，services 發佈
6. **實作冪等性** - 事件可能被處理多次
7. **使用 @TransactionalEventListener** - 在 commit 後發佈

### ❌ DON'T

1. **不要從 aggregates 發佈** - 只收集事件
2. **不要讓事件可變** - 使用 Records 確保不可變性
3. **不要跳過後設資料** - 總是包含 eventId 和 occurredOn
4. **不要使用現在式** - 事件是事實，不是命令
5. **不要引用可變物件** - 使用防禦性複本
6. **不要跳過冪等性檢查** - 事件可能被重播

---

## 測試 Domain Events

```java
@ExtendWith(MockitoExtension.class)
class OrderTest {

    @Test
    void should_collect_order_submitted_event_when_order_is_submitted() {
        // Given
        Order order = new Order(OrderId.generate(), CustomerId.of("CUST-001"), "台北市信義區");
        order.addItem("PROD-001", "Product 1", 2, Money.twd(100));

        // When
        order.submit();

        // Then
        assertThat(order.hasUncommittedEvents()).isTrue();
        List<DomainEvent> events = order.getUncommittedEvents();
        assertThat(events).anyMatch(e -> e instanceof OrderSubmittedEvent);

        OrderSubmittedEvent event = (OrderSubmittedEvent) events.stream()
            .filter(e -> e instanceof OrderSubmittedEvent)
            .findFirst()
            .orElseThrow();

        assertThat(event.orderId()).isEqualTo(order.getId());
        assertThat(event.customerId()).isEqualTo("CUST-001");
        assertThat(event.itemCount()).isEqualTo(1);
    }

    @Test
    void should_not_collect_event_when_no_changes() {
        // Given
        Customer customer = new Customer(
            CustomerId.generate(),
            new CustomerName("John Doe"),
            new Email("john@example.com"),
            new Phone("1234567890"),
            new Address("Street", "City", "12345"),
            MembershipLevel.STANDARD,
            null,
            LocalDateTime.now()
        );
        customer.markEventsAsCommitted(); // Clear creation event

        // When - update with same values
        customer.updateProfile(
            new CustomerName("John Doe"),
            new Email("john@example.com"),
            new Phone("1234567890")
        );

        // Then - no event collected
        assertThat(customer.hasUncommittedEvents()).isFalse();
    }
}
```

---

## 總結

Domain Events 提供：
- **鬆耦合** - Bounded contexts 透過事件溝通
- **稽核軌跡** - 完整的歷史記錄
- **Event sourcing** - 可從事件重建狀態
- **整合** - 易於與外部系統整合
- **可擴展性** - 非同步處理

---

**相關文件**：
- [Domain Events Standards](../../steering/domain-events.md)
- [Aggregate Root Examples](aggregate-root-examples.md)
- [DDD Tactical Patterns](../../steering/ddd-tactical-patterns.md)
