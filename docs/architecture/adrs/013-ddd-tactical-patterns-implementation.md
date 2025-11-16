---
adr_number: 013
title: "DDD Tactical Patterns Implementation"
date: 2025-10-24
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [002, 003, 012]
affected_viewpoints: ["development", "functional"]
affected_perspectives: ["evolution", "development-resource"]
---

# ADR-013: DDD Tactical Patterns Implementation

## 狀態

**Accepted** - 2025-10-24

## 上下文

### 問題陳述

The Enterprise E-Commerce Platform 需要structured approach to domain modeling that:

- Captures 複雜的 business logic in a 維持able way
- 提供s clear boundaries between different business concepts
- Ensures consistency within business transactions
- 啟用s 豐富的 domain models 與 behavior
- 支援s ubiquitous language from business domain
- Facilitates collaboration between domain experts 和 developers
- 提供s patterns 用於 common domain modeling challenges
- 啟用s evolution of the domain model over time

### 業務上下文

**業務驅動因素**：

- 複雜的 business rules 用於 e-commerce operations
- 需要 clear business logic encapsulation
- Requirement 用於 資料一致性 in transactions
- 支援 用於 evolving business requirements
- Collaboration between business 和 technical teams
- Long-term 維持ability of business logic

**限制條件**：

- Team has limited DDD experience
- Hexagonal Architecture already adopted (ADR-002)
- Domain events 用於 cross-context communication (ADR-003)
- 必須 integrate 與 existing Spring Boot stack
- Timeline: 3 個月 to establish DDD practices

### 技術上下文

**目前狀態**：

- Spring Boot 3.4.5 + Java 21
- Hexagonal Architecture (ADR-002)
- Domain events (ADR-003)
- BDD 與 Cucumber (ADR-012)
- 13 bounded contexts identified

**需求**：

- 豐富的 domain models 與 behavior
- Clear aggregate boundaries
- Consistency guarantees within aggregates
- Value objects 用於 domain concepts
- Repository pattern 用於 persistence
- Domain services 用於 cross-aggregate logic
- Factory pattern 用於 複雜的 object creation
- Specification pattern 用於 business rules

## 決策驅動因素

1. **Business Logic Encapsulation**: Keep business rules in domain layer
2. **Consistency Boundaries**: Clear transactional boundaries
3. **Ubiquitous Language**: Code reflects business language
4. **維持ability**: 容易understand 和 modify
5. **Testability**: Domain logic testable in isolation
6. **Evolution**: 支援 changing business requirements
7. **Team Collaboration**: Patterns facilitate communication
8. **Industry Standards**: Follow proven DDD patterns

## 考慮的選項

### 選項 1： Full DDD Tactical Patterns

**描述**： Implement complete set of DDD tactical patterns (Aggregates, Entities, Value Objects, Repositories, Domain Services, Factories, Specifications)

**優點**：

- ✅ 豐富的 domain models 與 behavior
- ✅ Clear aggregate boundaries
- ✅ Strong consistency guarantees
- ✅ Value objects 用於 type safety
- ✅ Testable domain logic
- ✅ Ubiquitous language in code
- ✅ Well-documented patterns
- ✅ 支援s 複雜的 business logic

**缺點**：

- ⚠️ Learning curve 用於 team
- ⚠️ More initial design effort
- ⚠️ 可以 be over-engineering 用於 簡單的 CRUD

**成本**： $0 (patterns, not tools)

**風險**： **Low** - Proven patterns

### 選項 2： Anemic Domain Model

**描述**： Simple data objects with getters/setters, business logic in services

**優點**：

- ✅ 簡單understand
- ✅ Familiar to most developers
- ✅ Quick to implement

**缺點**：

- ❌ Business logic scattered 跨 services
- ❌ No encapsulation
- ❌ 難以維持 複雜的 logic
- ❌ No consistency guarantees
- ❌ Poor testability

**成本**： $0

**風險**： **High** - Technical debt accumulates

### 選項 3： Transaction Script Pattern

**描述**： Procedural code organized by use cases

**優點**：

- ✅ 簡單的 用於 簡單的 使用案例s
- ✅ 容易understand

**缺點**：

- ❌ Doesn't scale to 複雜的 domains
- ❌ Code duplication
- ❌ No reusability
- ❌ 難以test

**成本**： $0

**風險**： **High** - Not suitable for complex domain

### 選項 4： Partial DDD (Aggregates Only)

**描述**： Use only aggregates and entities, skip value objects and other patterns

**優點**：

- ✅ Some benefits of DDD
- ✅ Lower learning curve

**缺點**：

- ❌ Missing type safety from value objects
- ❌ Incomplete pattern implementation
- ❌ Confusion about which patterns to use
- ❌ Less benefit than full DDD

**成本**： $0

**風險**： **Medium** - Incomplete benefits

## 決策結果

**選擇的選項**： **Full DDD Tactical Patterns**

### 理由

Full DDD tactical patterns被選擇的原因如下：

1. **複雜的 Domain**: E-commerce has 複雜的 business rules that benefit from 豐富的 domain models
2. **Consistency**: Aggregates 提供 clear transactional boundaries
3. **Type Safety**: Value objects prevent primitive obsession
4. **Testability**: Domain logic 可以 be tested 沒有 infrastructure
5. **維持ability**: Clear patterns make code easier to understand
6. **Evolution**: Patterns 支援 changing requirements
7. **Hexagonal Architecture Fit**: DDD patterns align perfectly 與 hexagonal architecture
8. **Industry Proven**: Patterns are well-documented 和 proven in production

**實作策略**：

**Core Patterns**:

1. **Aggregate Root**:

```java
@AggregateRoot(name = "Order", boundedContext = "Order", version = "1.0")
public class Order extends AggregateRootBase {
    private final OrderId id;
    private final CustomerId customerId;
    private OrderStatus status;
    private final List<OrderItem> items;
    private Money totalAmount;
    
    // Constructor enforces invariants
    public Order(OrderId id, CustomerId customerId, List<OrderItem> items) {
        this.id = Objects.requireNonNull(id, "Order ID cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.items = new ArrayList<>(Objects.requireNonNull(items, "Items cannot be null"));
        this.status = OrderStatus.DRAFT;
        
        validateOrderCreation();
        calculateTotal();
        
        collectEvent(OrderCreatedEvent.create(id, customerId, totalAmount));
    }
    
    // Business methods
    public void submit() {
        validateOrderSubmission();
        
        this.status = OrderStatus.PENDING;
        
        collectEvent(OrderSubmittedEvent.create(id, customerId, totalAmount, items.size()));
    }
    
    public void addItem(OrderItem item) {
        validateItemAddition(item);
        
        items.add(item);
        calculateTotal();
        
        collectEvent(OrderItemAddedEvent.create(id, item.getProductId(), item.getQuantity()));
    }
    
    // Invariant enforcement
    private void validateOrderCreation() {
        if (items.isEmpty()) {
            throw new BusinessRuleViolationException("Order must have at least one item");
        }
    }
    
    private void validateOrderSubmission() {
        if (status != OrderStatus.DRAFT) {
            throw new BusinessRuleViolationException(
                "Only draft orders can be submitted. Current status: " + status
            );
        }
        if (items.isEmpty()) {
            throw new BusinessRuleViolationException("Cannot submit empty order");
        }
    }
    
    private void calculateTotal() {
        this.totalAmount = items.stream()
            .map(OrderItem::getSubtotal)
            .reduce(Money.zero(), Money::add);
    }
    
    // Getters only, no setters
    public OrderId getId() { return id; }
    public OrderStatus getStatus() { return status; }
    public Money getTotalAmount() { return totalAmount; }
    public List<OrderItem> getItems() { return Collections.unmodifiableList(items); }
}
```

1. **Entity**:

```java
public class OrderItem {
    private final OrderItemId id;
    private final ProductId productId;
    private final String productName;
    private final Money unitPrice;
    private int quantity;
    private Money subtotal;
    
    public OrderItem(ProductId productId, String productName, Money unitPrice, int quantity) {
        this.id = OrderItemId.generate();
        this.productId = Objects.requireNonNull(productId);
        this.productName = Objects.requireNonNull(productName);
        this.unitPrice = Objects.requireNonNull(unitPrice);
        
        setQuantity(quantity);
    }
    
    public void setQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        this.quantity = quantity;
        this.subtotal = unitPrice.multiply(quantity);
    }
    
    public Money getSubtotal() { return subtotal; }
    public ProductId getProductId() { return productId; }
    public int getQuantity() { return quantity; }
}
```

1. **Value Object**:

```java
public record OrderId(String value) {
    public OrderId {
        Objects.requireNonNull(value, "Order ID cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Order ID cannot be blank");
        }
        if (!value.matches("ORD-\\d{10}")) {
            throw new IllegalArgumentException("Invalid order ID format: " + value);
        }
    }
    
    public static OrderId generate() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        return new OrderId("ORD-" + timestamp);
    }
    
    public static OrderId of(String value) {
        return new OrderId(value);
    }
}

public record Money(BigDecimal amount, Currency currency) {
    public Money {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
    }
    
    public static Money of(BigDecimal amount) {
        return new Money(amount, Currency.getInstance("USD"));
    }
    
    public static Money zero() {
        return new Money(BigDecimal.ZERO, Currency.getInstance("USD"));
    }
    
    public Money add(Money other) {
        validateSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }
    
    public Money multiply(int factor) {
        return new Money(amount.multiply(BigDecimal.valueOf(factor)), currency);
    }
    
    private void validateSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot operate on different currencies");
        }
    }
}
```

1. **Repository Interface** (in domain layer):

```java
public interface OrderRepository {
    Optional<Order> findById(OrderId orderId);
    List<Order> findByCustomerId(CustomerId customerId);
    Order save(Order order);
    void delete(OrderId orderId);
}
```

1. **Domain Service**:

```java
@Service
public class PricingService {
    
    public Money calculateOrderTotal(Order order, Customer customer) {
        Money subtotal = order.calculateSubtotal();
        Money discount = calculateDiscount(subtotal, customer);
        Money tax = calculateTax(subtotal.subtract(discount));
        
        return subtotal.subtract(discount).add(tax);
    }
    
    private Money calculateDiscount(Money subtotal, Customer customer) {
        DiscountRate rate = customer.getDiscountRate();
        return subtotal.multiply(rate.getValue());
    }
    
    private Money calculateTax(Money amount) {
        TaxRate taxRate = TaxRate.standard();
        return amount.multiply(taxRate.getValue());
    }
}
```

1. **Factory**:

```java
@Component
public class OrderFactory {
    
    private final ProductRepository productRepository;
    private final PricingService pricingService;
    
    public Order createOrder(CustomerId customerId, List<OrderItemRequest> itemRequests) {
        List<OrderItem> items = new ArrayList<>();
        
        for (OrderItemRequest request : itemRequests) {
            Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ProductNotFoundException(request.productId()));
            
            OrderItem item = new OrderItem(
                product.getId(),
                product.getName(),
                product.getPrice(),
                request.quantity()
            );
            
            items.add(item);
        }
        
        return new Order(OrderId.generate(), customerId, items);
    }
}
```

1. **Specification**:

```java
public interface Specification<T> {
    boolean isSatisfiedBy(T candidate);
    Specification<T> and(Specification<T> other);
    Specification<T> or(Specification<T> other);
    Specification<T> not();
}

public class OrderEligibleForDiscountSpecification implements Specification<Order> {
    
    private final Money minimumAmount;
    
    public OrderEligibleForDiscountSpecification(Money minimumAmount) {
        this.minimumAmount = minimumAmount;
    }
    
    @Override
    public boolean isSatisfiedBy(Order order) {
        return order.getTotalAmount().isGreaterThan(minimumAmount)
            && order.getStatus() == OrderStatus.DRAFT;
    }
    
    @Override
    public Specification<Order> and(Specification<Order> other) {
        return new AndSpecification<>(this, other);
    }
    
    // ... other methods
}

// Usage
Specification<Order> eligibleForDiscount = new OrderEligibleForDiscountSpecification(Money.of(100))
    .and(new CustomerIsPremiumSpecification());

if (eligibleForDiscount.isSatisfiedBy(order)) {
    order.applyDiscount(discount);
}
```

**為何不選 Anemic Domain Model**： Scatters business logic 跨 services, making it hard to 維持 和 test.

**為何不選 Transaction Script**： Doesn't scale to 複雜的 domains, leads to code duplication.

**為何不選 Partial DDD**： Missing key benefits like type safety from value objects 和 clear patterns.

## 影響分析

### 利害關係人影響

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Developers | High | Need to learn DDD patterns | Training, examples, pair programming |
| Architects | Positive | Clear domain modeling approach | Architecture guidelines |
| Business Experts | Medium | Participate in domain modeling | Workshops, ubiquitous language sessions |
| QA Team | Medium | Test domain logic in isolation | Testing guides, examples |

### 影響半徑

**選擇的影響半徑**： **System**

影響：

- All bounded contexts
- Domain layer structure
- Testing approach
- Development workflow
- Code review process

### 風險評估

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| DDD learning curve | High | Medium | Training, examples, pair programming, code reviews |
| Over-engineering 簡單的 features | Medium | Low | Pragmatic approach, allow 簡單的r patterns 用於 CRUD |
| Aggregate boundary mistakes | Medium | High | Domain modeling workshops, architecture reviews |
| Performance concerns | Low | Medium | Performance testing, optimization where needed |
| Team resistance | Medium | Medium | Demonstrate benefits, involve team in decisions |

**整體風險等級**： **Low**

## 實作計畫

### 第 1 階段： Training and Setup （第 1-2 週）

- [ ] Conduct DDD training
  - Tactical patterns overview
  - Aggregate design workshop
  - Value object benefits
  - Repository pattern
  - Domain services vs application services

- [ ] Create base classes 和 interfaces

  ```java
  public abstract class AggregateRootBase implements AggregateRootInterface {
      private final List<DomainEvent> uncommittedEvents = new ArrayList<>();
      
      protected void collectEvent(DomainEvent event) {
          uncommittedEvents.add(event);
      }
      
      @Override
      public List<DomainEvent> getUncommittedEvents() {
          return Collections.unmodifiableList(uncommittedEvents);
      }
      
      @Override
      public void markEventsAsCommitted() {
          uncommittedEvents.clear();
      }
      
      @Override
      public boolean hasUncommittedEvents() {
          return !uncommittedEvents.isEmpty();
      }
  }
  ```

- [ ] Create annotations

  ```java
  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  public @interface AggregateRoot {
      String name();
      String boundedContext();
      String version() default "1.0";
      String description() default "";
  }
  
  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  public @interface ValueObject {
  }
  ```

### 第 2 階段： Customer Bounded Context （第 2-4 週）

- [ ] Design Customer aggregate

  ```java
  @AggregateRoot(name = "Customer", boundedContext = "Customer")
  public class Customer extends AggregateRootBase {
      private final CustomerId id;
      private CustomerName name;
      private Email email;
      private Address address;
      private MembershipLevel membershipLevel;
      private CustomerStatus status;
      
      public Customer(CustomerId id, CustomerName name, Email email, Address address) {
          this.id = Objects.requireNonNull(id);
          this.name = Objects.requireNonNull(name);
          this.email = Objects.requireNonNull(email);
          this.address = Objects.requireNonNull(address);
          this.membershipLevel = MembershipLevel.STANDARD;
          this.status = CustomerStatus.ACTIVE;
          
          collectEvent(CustomerCreatedEvent.create(id, name, email, membershipLevel));
      }
      
      public void updateProfile(CustomerName newName, Email newEmail, Address newAddress) {
          validateProfileUpdate(newName, newEmail, newAddress);
          
          this.name = newName;
          this.email = newEmail;
          this.address = newAddress;
          
          collectEvent(CustomerProfileUpdatedEvent.create(id, newName, newEmail, newAddress));
      }
      
      public void upgradeMembership(MembershipLevel newLevel) {
          if (!newLevel.isHigherThan(this.membershipLevel)) {
              throw new BusinessRuleViolationException(
                  "New membership level must be higher than current level"
              );
          }
          
          this.membershipLevel = newLevel;
          
          collectEvent(CustomerMembershipUpgradedEvent.create(id, newLevel));
      }
      
      private void validateProfileUpdate(CustomerName name, Email email, Address address) {
          Objects.requireNonNull(name, "Name cannot be null");
          Objects.requireNonNull(email, "Email cannot be null");
          Objects.requireNonNull(address, "Address cannot be null");
      }
  }
  ```

- [ ] Create value objects

  ```java
  @ValueObject
  public record CustomerName(String value) {
      public CustomerName {
          Objects.requireNonNull(value, "Customer name cannot be null");
          if (value.isBlank()) {
              throw new IllegalArgumentException("Customer name cannot be blank");
          }
          if (value.length() < 2 || value.length() > 100) {
              throw new IllegalArgumentException("Customer name must be between 2 and 100 characters");
          }
      }
      
      public static CustomerName of(String value) {
          return new CustomerName(value);
      }
  }
  
  @ValueObject
  public record Email(String value) {
      private static final Pattern EMAIL_PATTERN = 
          Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
      
      public Email {
          Objects.requireNonNull(value, "Email cannot be null");
          if (!EMAIL_PATTERN.matcher(value).matches()) {
              throw new IllegalArgumentException("Invalid email format: " + value);
          }
      }
      
      public static Email of(String value) {
          return new Email(value);
      }
  }
  
  @ValueObject
  public record Address(
      String street,
      String city,
      String state,
      String postalCode,
      String country
  ) {
      public Address {
          Objects.requireNonNull(street, "Street cannot be null");
          Objects.requireNonNull(city, "City cannot be null");
          Objects.requireNonNull(postalCode, "Postal code cannot be null");
          Objects.requireNonNull(country, "Country cannot be null");
      }
  }
  ```

- [ ] Implement repository
- [ ] Write unit tests

### 第 3 階段： Order Bounded Context （第 4-6 週）

- [ ] Design Order aggregate (as shown in examples above)
- [ ] Create value objects (OrderId, Money, etc.)
- [ ] Implement domain services (PricingService)
- [ ] Create factory (OrderFactory)
- [ ] Implement specifications
- [ ] Write unit tests

### 第 4 階段： Product Bounded Context （第 6-8 週）

- [ ] Design Product aggregate
- [ ] Create value objects
- [ ] Implement repository
- [ ] Write unit tests

### Phase 5: Remaining Bounded Contexts （第 8-12 週）

- [ ] Implement remaining 10 bounded contexts
- [ ] Follow established patterns
- [ ] Conduct code reviews
- [ ] Write comprehensive tests

### Phase 6: ArchUnit Validation （第 12 週）

- [ ] Create ArchUnit tests

  ```java
  @ArchTest
  static final ArchRule aggregateRootRules = classes()
      .that().areAnnotatedWith(AggregateRoot.class)
      .should().resideInAPackage("..domain..model.aggregate..")
      .andShould().implement(AggregateRootInterface.class);
  
  @ArchTest
  static final ArchRule valueObjectRules = classes()
      .that().areAnnotatedWith(ValueObject.class)
      .should().beRecords()
      .andShould().resideInAPackage("..domain..model.valueobject..");
  
  @ArchTest
  static final ArchRule repositoryRules = classes()
      .that().haveSimpleNameEndingWith("Repository")
      .and().areInterfaces()
      .should().resideInAPackage("..domain..repository..");
  
  @ArchTest
  static final ArchRule domainLayerRules = classes()
      .that().resideInAPackage("..domain..")
      .should().onlyDependOnClassesThat()
      .resideInAnyPackage("..domain..", "java..", "org.springframework..");
  ```

### 回滾策略

**觸發條件**：

- Team unable to adopt DDD after 6 個月
- Development velocity decreases > 40%
- Aggregate boundaries causing major issues
- Over-engineering becomes problematic

**回滾步驟**：

1. Simplify to anemic domain model
2. Move business logic to application services
3. Keep value objects 用於 type safety
4. Simplify aggregate boundaries
5. Document lessons learned

**回滾時間**： 4 weeks

## 監控和成功標準

### 成功指標

- ✅ 100% of aggregates follow DDD patterns
- ✅ Value objects used instead of primitives > 90%
- ✅ Domain logic testable 沒有 infrastructure
- ✅ ArchUnit tests pass 100%
- ✅ Code review compliance > 95%
- ✅ Developer satisfaction > 4/5
- ✅ Business logic bugs decrease 透過 50%

### 監控計畫

**Code Quality Metrics**:

- Number of aggregates per bounded context
- Value object usage rate
- Domain logic test coverage
- ArchUnit test pass rate
- Code review findings

**審查時程**：

- Weekly: Domain modeling sessions
- Monthly: Architecture review
- Quarterly: DDD practice retrospective

## 後果

### 正面後果

- ✅ **豐富的 Domain Models**: Business logic encapsulated in domain objects
- ✅ **Type Safety**: Value objects prevent primitive obsession
- ✅ **Consistency**: Aggregates enforce invariants
- ✅ **Testability**: Domain logic testable in isolation
- ✅ **維持ability**: Clear patterns make code easier to understand
- ✅ **Ubiquitous Language**: Code reflects business language
- ✅ **Evolution**: Patterns 支援 changing requirements
- ✅ **Clear Boundaries**: Aggregates define transactional boundaries

### 負面後果

- ⚠️ **Learning Curve**: Team needs to learn DDD patterns
- ⚠️ **Initial Overhead**: More design effort upfront
- ⚠️ **Verbosity**: More classes than anemic model
- ⚠️ **可以 Over-Engineer**: Risk of over-engineering 簡單的 features

### 技術債務

**已識別債務**：

1. Some aggregates may have incorrect boundaries initially (將 refine)
2. Not all value objects implemented yet (gradual adoption)
3. Some domain services may be in wrong layer (將 refactor)
4. Limited specification pattern usage (future enhancement)

**債務償還計畫**：

- **Q1 2026**: Refine aggregate boundaries based on experience
- **Q2 2026**: Complete value object adoption
- **Q3 2026**: Review 和 refactor domain services
- **Q4 2026**: Expand specification pattern usage

## 相關決策

- [ADR-002: Adopt Hexagonal Architecture](002-adopt-hexagonal-architecture.md) - DDD in domain layer
- [ADR-003: Use Domain Events 用於 Cross-Context Communication](003-use-domain-events-for-cross-context-communication.md) - Events from aggregates
- [ADR-012: BDD 與 Cucumber](012-bdd-with-cucumber-for-requirements.md) - BDD scenarios test aggregates

## 備註

### Aggregate Design Guidelines

**DO**:

- Keep aggregates small
- Design around business invariants
- Use value objects 用於 concepts
- Enforce invariants in constructor
- Use domain events 用於 side effects
- Make aggregates testable

**DON'T**:

- Create 大型的 aggregates
- Reference other aggregates directly
- Expose internal collections
- Use setters 用於 state changes
- Put infrastructure concerns in domain

### Value Object Benefits

1. **Type Safety**: Compile-time checking
2. **Validation**: Centralized validation logic
3. **Immutability**: Thread-safe, no side effects
4. **Expressiveness**: Clear intent in code
5. **Reusability**: Used 跨 aggregates

### Common Patterns

**Aggregate Root Pattern**:

- Single entry point to aggregate
- Enforces invariants
- Collects domain events
- Controls access to entities

**Repository Pattern**:

- Interface in domain layer
- Implementation in infrastructure
- One repository per aggregate root
- Returns domain objects

**Factory Pattern**:

- 複雜的 object creation
- Enforces creation rules
- Hides construction 複雜的ity

**Specification Pattern**:

- Encapsulates business rules
- Reusable 和 composable
- Testable in isolation

---

**文檔狀態**： ✅ Accepted  
**上次審查**： 2025-10-24  
**下次審查**： 2026-01-24 （每季）
