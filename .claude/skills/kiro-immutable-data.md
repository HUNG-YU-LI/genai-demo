# Kiro 不可變資料技能

## 描述
為所有資料結構強制實施不可變性，以確保可預測、執行緒安全且可追蹤的程式碼。基於函數式程式設計原則和 AWS Lambda 最佳實踐。

## 何時使用
- 定義領域模型（Domain Models）和資料傳輸物件（DTOs）
- 建立值物件（Value Objects）
- 設計 API 請求/回應物件
- 實作事件架構（Event Schemas）

## 核心原則

### 1. 預設不可變
除非有充分理由，否則所有資料結構應該是不可變的。

### 2. 寫時複製（Copy-on-Write）
要「修改」資料，應建立帶有已更改值的新實例。

### 3. 防守性複製（Defensive Copying）
防止外部修改。

## Java 記錄模式（Record Pattern）（推薦）

### ✅ 最佳實踐：使用記錄
```java
/**
 * Immutable order representation using Java Record
 */
public record Order(
    OrderId id,
    CustomerId customerId,
    List<OrderItem> items,
    Money totalAmount,
    OrderStatus status,
    LocalDateTime createdAt
) {
    // Compact constructor for validation
    public Order {
        Objects.requireNonNull(id, "Order ID cannot be null");
        Objects.requireNonNull(customerId, "Customer ID cannot be null");

        // Defensive copy of mutable collections
        items = List.copyOf(items);

        // Validation
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }
    }

    // Factory methods for creation
    public static Order create(CustomerId customerId, List<OrderItem> items) {
        OrderId id = OrderId.generate();
        Money totalAmount = calculateTotal(items);
        return new Order(
            id,
            customerId,
            items,
            totalAmount,
            OrderStatus.PENDING,
            LocalDateTime.now()
        );
    }

    // Immutable updates return new instances
    public Order withStatus(OrderStatus newStatus) {
        return new Order(
            this.id,
            this.customerId,
            this.items,
            this.totalAmount,
            newStatus,
            this.createdAt
        );
    }

    public Order addItem(OrderItem newItem) {
        List<OrderItem> updatedItems = new ArrayList<>(this.items);
        updatedItems.add(newItem);

        Money updatedTotal = calculateTotal(updatedItems);

        return new Order(
            this.id,
            this.customerId,
            List.copyOf(updatedItems),
            updatedTotal,
            this.status,
            this.createdAt
        );
    }

    private static Money calculateTotal(List<OrderItem> items) {
        return items.stream()
            .map(OrderItem::getSubtotal)
            .reduce(Money.ZERO, Money::add);
    }
}
```

## 值物件模式（Value Object Pattern）

```java
/**
 * Immutable value object for email addresses
 */
public record Email(String value) {

    public Email {
        Objects.requireNonNull(value, "Email cannot be null");

        if (!value.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email format: " + value);
        }

        // Normalize
        value = value.toLowerCase().trim();
    }

    @Override
    public String toString() {
        return value;
    }
}

/**
 * Immutable money value object
 */
public record Money(BigDecimal amount, Currency currency) {

    public static final Money ZERO = new Money(BigDecimal.ZERO, Currency.getInstance("USD"));

    public Money {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");

        if (amount.scale() > 2) {
            amount = amount.setScale(2, RoundingMode.HALF_UP);
        }
    }

    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add different currencies");
        }

        return new Money(
            this.amount.add(other.amount),
            this.currency
        );
    }

    public Money multiply(BigDecimal multiplier) {
        return new Money(
            this.amount.multiply(multiplier).setScale(2, RoundingMode.HALF_UP),
            this.currency
        );
    }
}
```

## 事件模式（Event Pattern）（本質上不可變）

```java
/**
 * Immutable domain event
 */
public record OrderCreatedEvent(
    UUID eventId,
    OrderId orderId,
    CustomerId customerId,
    List<OrderItem> items,
    Money totalAmount,
    LocalDateTime occurredAt
) implements DomainEvent {

    public OrderCreatedEvent {
        // Defensive copies
        items = List.copyOf(items);

        // Auto-set if not provided
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }

    // Factory method
    public static OrderCreatedEvent from(Order order) {
        return new OrderCreatedEvent(
            UUID.randomUUID(),
            order.id(),
            order.customerId(),
            order.items(),
            order.totalAmount(),
            LocalDateTime.now()
        );
    }

    @Override
    public String getEventType() {
        return "OrderCreated";
    }
}
```

## 建造者模式（Builder Pattern）用於複雜物件

當記錄變得太複雜時，使用具有不可變性的建造者：

```java
/**
 * Immutable customer with builder
 */
public final class Customer {

    private final CustomerId id;
    private final PersonalInfo personalInfo;
    private final ContactInfo contactInfo;
    private final MembershipInfo membershipInfo;
    private final List<Address> addresses;

    private Customer(Builder builder) {
        this.id = Objects.requireNonNull(builder.id);
        this.personalInfo = Objects.requireNonNull(builder.personalInfo);
        this.contactInfo = Objects.requireNonNull(builder.contactInfo);
        this.membershipInfo = Objects.requireNonNull(builder.membershipInfo);
        this.addresses = List.copyOf(builder.addresses);
    }

    // Getters only (no setters)
    public CustomerId getId() { return id; }
    public PersonalInfo getPersonalInfo() { return personalInfo; }
    // ... other getters

    // Immutable update methods
    public Customer withContactInfo(ContactInfo newContactInfo) {
        return new Builder()
            .id(this.id)
            .personalInfo(this.personalInfo)
            .contactInfo(newContactInfo)
            .membershipInfo(this.membershipInfo)
            .addresses(this.addresses)
            .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private CustomerId id;
        private PersonalInfo personalInfo;
        private ContactInfo contactInfo;
        private MembershipInfo membershipInfo;
        private List<Address> addresses = new ArrayList<>();

        public Builder id(CustomerId id) {
            this.id = id;
            return this;
        }

        public Builder personalInfo(PersonalInfo personalInfo) {
            this.personalInfo = personalInfo;
            return this;
        }

        public Builder contactInfo(ContactInfo contactInfo) {
            this.contactInfo = contactInfo;
            return this;
        }

        public Builder membershipInfo(MembershipInfo membershipInfo) {
            this.membershipInfo = membershipInfo;
            return this;
        }

        public Builder addresses(List<Address> addresses) {
            this.addresses = new ArrayList<>(addresses);
            return this;
        }

        public Builder addAddress(Address address) {
            this.addresses.add(address);
            return this;
        }

        public Customer build() {
            return new Customer(this);
        }
    }
}
```

## 集合不可變性（Collection Immutability）

```java
public class ImmutableCollectionExamples {

    // ✅ GOOD: Immutable list
    public record OrderSummary(
        OrderId orderId,
        List<String> productNames
    ) {
        public OrderSummary {
            productNames = List.copyOf(productNames);
        }
    }

    // ✅ GOOD: Immutable map
    public record ProductCatalog(
        Map<ProductId, Product> products
    ) {
        public ProductCatalog {
            products = Map.copyOf(products);
        }

        public Optional<Product> findProduct(ProductId id) {
            return Optional.ofNullable(products.get(id));
        }
    }

    // ✅ GOOD: Immutable set
    public record CustomerTags(
        Set<String> tags
    ) {
        public CustomerTags {
            tags = Set.copyOf(tags);
        }

        public CustomerTags addTag(String newTag) {
            Set<String> updatedTags = new HashSet<>(tags);
            updatedTags.add(newTag);
            return new CustomerTags(updatedTags);
        }
    }
}
```

## 應避免的反模式（Anti-Patterns）

### ❌ 可變欄位
```java
// BAD: Mutable class
public class Order {
    private OrderId id;
    private OrderStatus status;
    private List<OrderItem> items;

    public void setStatus(OrderStatus status) {
        this.status = status; // BAD: Mutation
    }

    public void addItem(OrderItem item) {
        this.items.add(item); // BAD: Mutation
    }
}
```

### ❌ 暴露可變集合
```java
// BAD: Exposing internal mutable state
public record Order(
    OrderId id,
    List<OrderItem> items
) {
    // BAD: Returns mutable reference
    public List<OrderItem> items() {
        return items; // Caller can modify!
    }
}
```

## JPA 實體的不可變性

特殊情況：JPA 需要可變性，但我們可以最小化它：

```java
@Entity
@Table(name = "orders")
public class JpaOrderEntity {

    @Id
    private String id;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @Version
    private Long version;

    // JPA requires default constructor
    protected JpaOrderEntity() {}

    // Factory method for creation
    public static JpaOrderEntity from(Order order) {
        JpaOrderEntity entity = new JpaOrderEntity();
        entity.id = order.id().getValue();
        entity.customerId = order.customerId().getValue();
        entity.totalAmount = order.totalAmount().amount();
        entity.status = order.status();
        return entity;
    }

    // Convert to immutable domain model
    public Order toDomainModel() {
        return new Order(
            new OrderId(id),
            new CustomerId(customerId),
            // ... map other fields
            status,
            // ...
        );
    }

    // Package-private setters (only for JPA)
    void setStatus(OrderStatus status) {
        this.status = status;
    }
}
```

## Claude Code 的好處

1. **可預測性（Predictability）**：不可變物件的行為始終一致
2. **執行緒安全性（Thread Safety）**：無需同步化
3. **偵錯（Debugging）**：更容易追蹤狀態變化
4. **測試（Testing）**：更簡單地建立測試夾具
5. **AI 友善（AI-Friendly）**：更清晰的資料流供 AI 推理

## Claude 的提示詞

```
Create an immutable Customer domain model using Java records.
Include validation in compact constructor and immutable update methods.
```

```
Generate an immutable event class for OrderShipped with defensive copying of collections.
```

## 驗證檢查清單

- [ ] 所有領域模型使用記錄或最終類別
- [ ] 無公開 setter
- [ ] 集合進行防守性複製
- [ ] 更新方法返回新實例
- [ ] 值物件在建構子中驗證
- [ ] 事件在設計上不可變
- [ ] JPA 實體具有最小可變性
