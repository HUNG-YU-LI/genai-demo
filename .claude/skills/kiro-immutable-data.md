# Kiro Immutable Data Skill

## Description
Enforces immutability for all data structures to ensure predictable, thread-safe, and traceable code. Based on functional programming principles and AWS Lambda best practices.

## When to Use
- Defining domain models and DTOs
- Creating value objects
- Designing API request/response objects
- Implementing event schemas

## Core Principles

### 1. Immutability by Default
All data structures should be immutable unless there's a compelling reason otherwise.

### 2. Copy-on-Write
To "modify" data, create a new instance with the changed values.

### 3. Defensive Copying
Protect against external mutations.

## Java Record Pattern (Preferred)

### ✅ Best Practice: Use Records
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

## Value Object Pattern

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

## Event Pattern (Immutable by Nature)

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

## Builder Pattern for Complex Objects

When records become too complex, use builders with immutability:

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

## Collection Immutability

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

## Anti-Patterns to Avoid

### ❌ Mutable Fields
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

### ❌ Exposing Mutable Collections
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

## Immutability with JPA Entities

Special case: JPA requires mutability, but we can minimize it:

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

## Benefits for Claude Code

1. **Predictability**: Immutable objects behave consistently
2. **Thread Safety**: No synchronization needed
3. **Debugging**: Easier to track state changes
4. **Testing**: Simpler to create test fixtures
5. **AI-Friendly**: Clearer data flow for AI to reason about

## Prompts for Claude

```
Create an immutable Customer domain model using Java records.
Include validation in compact constructor and immutable update methods.
```

```
Generate an immutable event class for OrderShipped with defensive copying of collections.
```

## Validation Checklist

- [ ] All domain models use records or final classes
- [ ] No public setters
- [ ] Collections are defensively copied
- [ ] Update methods return new instances
- [ ] Value objects validate in constructor
- [ ] Events are immutable by design
- [ ] JPA entities have minimal mutability
