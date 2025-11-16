# Repository Pattern - 詳細範例

## 原則概述

**Repository** 提供了資料存取的抽象，讓領域層可以使用領域物件而不需要知道持久化細節。Repository 介面存在於領域層，而實作則存在於基礎設施層。

## 核心概念

- **介面在領域層（Interface in Domain Layer）** - Repository 契約定義在領域層
- **實作在基礎設施層（Implementation in Infrastructure Layer）** - 實際的持久化邏輯
- **回傳領域物件（Return Domain Objects）** - 而非資料庫實體
- **每個 Aggregate Root 一個 Repository** - 只有聚合根有 repositories
- **使用 Optional 處理單一結果** - 明確處理找不到的情況

**相關標準**：[DDD Tactical Patterns](../../steering/ddd-tactical-patterns.md)

---

## Repository 介面（領域層 - 正式程式碼）

### OrderRepository 介面

這是我們正式程式碼庫中實際的 OrderRepository 介面：

```java
package solid.humank.genaidemo.domain.order.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.annotations.Repository;
import solid.humank.genaidemo.domain.common.repository.BaseRepository;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;

/**
 * Order repository interface
 */
@Repository(name = "OrderRepository", description = "訂單聚合根儲存庫")
public interface OrderRepository extends BaseRepository<Order, OrderId> {

    /**
     * Save order
     */
    @Override
    Order save(Order order);

    /**
     * Find order by ID
     */
    Optional<Order> findById(OrderId id);

    /**
     * Find orders by customer ID
     */
    List<Order> findByCustomerId(CustomerId customerId);

    /**
     * Find orders by customer ID (UUID version)
     */
    List<Order> findByCustomerId(UUID customerId);
}
```

### CustomerRepository 介面

```java
package solid.humank.genaidemo.domain.customer.repository;

import java.util.List;
import java.util.Optional;

import solid.humank.genaidemo.domain.common.annotations.Repository;
import solid.humank.genaidemo.domain.common.repository.BaseRepository;
import solid.humank.genaidemo.domain.customer.model.aggregate.Customer;
import solid.humank.genaidemo.domain.customer.model.valueobject.Email;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;

/**
 * Customer repository interface
 */
@Repository(name = "CustomerRepository", description = "客戶聚合根儲存庫")
public interface CustomerRepository extends BaseRepository<Customer, CustomerId> {

    /**
     * Save customer
     */
    @Override
    Customer save(Customer customer);

    /**
     * Find customer by ID
     */
    Optional<Customer> findById(CustomerId id);

    /**
     * Find customer by email
     */
    Optional<Customer> findByEmail(Email email);

    /**
     * Find all customers
     */
    List<Customer> findAll();

    /**
     * Delete customer
     */
    void delete(Customer customer);

    /**
     * Check if customer exists by email
     */
    boolean existsByEmail(Email email);
}
```

---

## Repository 實作（基礎設施層 - 正式程式碼）

### JPA Repository 介面

這是我們正式程式碼庫中實際的 JPA repository：

```java
package solid.humank.genaidemo.infrastructure.order.persistence.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import solid.humank.genaidemo.infrastructure.order.persistence.entity.JpaOrderEntity;

/**
 * JPA Order Repository
 * Used for database interaction with Spring Data JPA
 */
@Repository
public interface JpaOrderRepository extends JpaRepository<JpaOrderEntity, String> {

    /**
     * Find orders by customer ID
     */
    List<JpaOrderEntity> findByCustomerId(String customerId);

    /**
     * Find order by ID
     */
    Optional<JpaOrderEntity> findById(String id);

    // ========== Statistical Query Methods ==========

    /**
     * Count all order items
     */
    @Query("SELECT COUNT(oi) FROM JpaOrderEntity o JOIN o.items oi")
    long countAllOrderItems();

    /**
     * Sum total amount by status
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM JpaOrderEntity o WHERE o.status = :status")
    BigDecimal sumTotalAmountByStatus(
            @Param("status") solid.humank.genaidemo.domain.common.valueobject.OrderStatus status);

    /**
     * Count distinct customers
     */
    @Query("SELECT COUNT(DISTINCT o.customerId) FROM JpaOrderEntity o")
    long countDistinctCustomers();

    /**
     * Count orders grouped by status
     */
    @Query("SELECT o.status, COUNT(o) FROM JpaOrderEntity o GROUP BY o.status")
    List<Object[]> countByStatusGrouped();

    /**
     * Find distinct customer IDs
     */
    @Query("SELECT DISTINCT o.customerId FROM JpaOrderEntity o ORDER BY o.customerId")
    List<String> findDistinctCustomerIds();

    /**
     * Check if customer has orders
     */
    boolean existsByCustomerId(String customerId);

    /**
     * Count orders by customer ID
     */
    long countByCustomerId(String customerId);
}
```

### Repository Adapter 實作

```java
package solid.humank.genaidemo.infrastructure.order.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;
import solid.humank.genaidemo.domain.order.repository.OrderRepository;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;
import solid.humank.genaidemo.infrastructure.order.persistence.entity.JpaOrderEntity;
import solid.humank.genaidemo.infrastructure.order.persistence.mapper.OrderMapper;
import solid.humank.genaidemo.infrastructure.order.persistence.repository.JpaOrderRepository;

/**
 * Order Repository Adapter
 * Implements domain repository interface using JPA
 */
@Component
public class OrderRepositoryAdapter implements OrderRepository {

    private final JpaOrderRepository jpaRepository;
    private final OrderMapper mapper;

    public OrderRepositoryAdapter(JpaOrderRepository jpaRepository, OrderMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Order save(Order order) {
        // Convert domain object to JPA entity
        JpaOrderEntity entity = mapper.toEntity(order);

        // Save to database
        JpaOrderEntity savedEntity = jpaRepository.save(entity);

        // Convert back to domain object
        Order savedOrder = mapper.toDomain(savedEntity);

        // Mark events as committed after successful save
        savedOrder.markEventsAsCommitted();

        return savedOrder;
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return jpaRepository.findById(id.getValue())
                .map(mapper::toDomain);
    }

    @Override
    public List<Order> findByCustomerId(CustomerId customerId) {
        return jpaRepository.findByCustomerId(customerId.getValue())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Order> findByCustomerId(UUID customerId) {
        return jpaRepository.findByCustomerId(customerId.toString())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
```

---

## 領域-實體映射（正式程式碼）

### OrderMapper

```java
package solid.humank.genaidemo.infrastructure.order.persistence.mapper;

import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.common.valueobject.OrderItem;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;
import solid.humank.genaidemo.infrastructure.order.persistence.entity.JpaOrderEntity;
import solid.humank.genaidemo.infrastructure.order.persistence.entity.JpaOrderItemEntity;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper between Order domain object and JPA entity
 */
@Component
public class OrderMapper {

    /**
     * Convert domain object to JPA entity
     */
    public JpaOrderEntity toEntity(Order order) {
        JpaOrderEntity entity = new JpaOrderEntity();
        entity.setId(order.getId().getValue());
        entity.setCustomerId(order.getCustomerId().getValue());
        entity.setShippingAddress(order.getShippingAddress());
        entity.setStatus(order.getStatus());
        entity.setTotalAmount(order.getTotalAmount().getAmount());
        entity.setEffectiveAmount(order.getEffectiveAmount().getAmount());
        entity.setCreatedAt(order.getCreatedAt());
        entity.setUpdatedAt(order.getUpdatedAt());

        // Convert order items
        List<JpaOrderItemEntity> itemEntities = order.getItems().stream()
                .map(item -> toItemEntity(item, entity))
                .collect(Collectors.toList());
        entity.setItems(itemEntities);

        return entity;
    }

    /**
     * Convert JPA entity to domain object
     */
    public Order toDomain(JpaOrderEntity entity) {
        // Convert order items
        List<OrderItem> items = entity.getItems().stream()
                .map(this::toItemDomain)
                .collect(Collectors.toList());

        // Use reconstruction constructor
        return new Order(
                OrderId.of(entity.getId()),
                CustomerId.of(entity.getCustomerId()),
                entity.getShippingAddress(),
                items,
                entity.getStatus(),
                Money.of(entity.getTotalAmount()),
                Money.of(entity.getEffectiveAmount()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private JpaOrderItemEntity toItemEntity(OrderItem item, JpaOrderEntity order) {
        JpaOrderItemEntity entity = new JpaOrderItemEntity();
        entity.setProductId(item.getProductId());
        entity.setProductName(item.getProductName());
        entity.setQuantity(item.getQuantity());
        entity.setPrice(item.getPrice().getAmount());
        entity.setOrder(order);
        return entity;
    }

    private OrderItem toItemDomain(JpaOrderItemEntity entity) {
        return new OrderItem(
                entity.getProductId(),
                entity.getProductName(),
                entity.getQuantity(),
                Money.of(entity.getPrice())
        );
    }
}
```

---

## 在 Application Services 中使用（正式程式碼）

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
     * Submit order
     */
    public void submitOrder(SubmitOrderCommand command) {
        // 1. Load aggregate from repository
        Order order = orderRepository.findById(command.orderId())
                .orElseThrow(() -> new OrderNotFoundException(command.orderId()));

        // 2. Execute business operation
        order.submit();

        // 3. Save aggregate to repository
        orderRepository.save(order);

        // 4. Publish collected events
        eventService.publishEventsFromAggregate(order);
    }

    /**
     * Create order
     */
    public OrderId createOrder(CreateOrderCommand command) {
        // 1. Create new aggregate
        Order order = new Order(
                OrderId.generate(),
                CustomerId.of(command.customerId()),
                command.shippingAddress()
        );

        // 2. Add items
        for (var item : command.items()) {
            order.addItem(
                    item.productId(),
                    item.productName(),
                    item.quantity(),
                    Money.twd(item.price())
            );
        }

        // 3. Save aggregate to repository
        orderRepository.save(order);

        // 4. Publish collected events
        eventService.publishEventsFromAggregate(order);

        return order.getId();
    }

    /**
     * Find orders by customer
     */
    public List<OrderDto> findOrdersByCustomer(CustomerId customerId) {
        return orderRepository.findByCustomerId(customerId)
                .stream()
                .map(OrderDto::from)
                .toList();
    }
}
```

---

## 關鍵 模式

### 1. 介面在領域層，實作在基礎設施層

```java
// Domain layer - interface
package solid.humank.genaidemo.domain.order.repository;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(OrderId id);
}

// Infrastructure layer - implementation
package solid.humank.genaidemo.infrastructure.order.persistence.adapter;

@Component
public class OrderRepositoryAdapter implements OrderRepository {
    private final JpaOrderRepository jpaRepository;
    private final OrderMapper mapper;

    @Override
    public Order save(Order order) {
        JpaOrderEntity entity = mapper.toEntity(order);
        JpaOrderEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
```

### 2. 回傳領域物件，而非實體

```java
// ✅ Good: Return domain object
@Override
public Optional<Order> findById(OrderId id) {
    return jpaRepository.findById(id.getValue())
            .map(mapper::toDomain); // Convert to domain object
}

// ❌ Bad: Return JPA entity
@Override
public Optional<JpaOrderEntity> findById(OrderId id) {
    return jpaRepository.findById(id.getValue()); // Wrong!
}
```

### 3. 使用 Optional 處理單一結果

```java
// ✅ Good: Use Optional
Optional<Order> findById(OrderId id);

// ❌ Bad: Return null
Order findById(OrderId id); // May return null
```

### 4. Mapper Pattern 進行領域-實體轉換

```java
@Component
public class OrderMapper {

    // Domain to Entity
    public JpaOrderEntity toEntity(Order order) {
        JpaOrderEntity entity = new JpaOrderEntity();
        entity.setId(order.getId().getValue());
        entity.setCustomerId(order.getCustomerId().getValue());
        // ... map other fields
        return entity;
    }

    // Entity to Domain
    public Order toDomain(JpaOrderEntity entity) {
        return new Order(
                OrderId.of(entity.getId()),
                CustomerId.of(entity.getCustomerId()),
                // ... map other fields
        );
    }
}
```

### 5. 儲存後標記事件已提交

```java
@Override
public Order save(Order order) {
    JpaOrderEntity entity = mapper.toEntity(order);
    JpaOrderEntity savedEntity = jpaRepository.save(entity);
    Order savedOrder = mapper.toDomain(savedEntity);

    // Mark events as committed after successful save
    savedOrder.markEventsAsCommitted();

    return savedOrder;
}
```

---

## 最佳實踐

### ✅ DO

1. **介面在領域層** - Repository 契約屬於領域
2. **實作在基礎設施層** - 隱藏持久化細節
3. **回傳領域物件** - 而非資料庫實體
4. **使用 Optional 處理單一結果** - 明確處理找不到的情況
5. **每個聚合根一個 repository** - 只有聚合根有 repositories
6. **使用 mapper pattern** - 領域與持久化的清楚分離
7. **標記事件已提交** - 在成功儲存後

### ❌ DON'T

1. **不要暴露 JPA 實體** - 總是回傳領域物件
2. **不要在 repository 中放入業務邏輯** - Repository 僅用於持久化
3. **不要為非聚合實體建立 repositories** - 只有聚合根
4. **不要回傳 null** - 使用 Optional 代替
5. **不要洩漏持久化細節** - 將 JPA annotations 保持在基礎設施層

---

## 測試 Repositories

### 整合 Test

```java
@DataJpaTest
@ActiveProfiles("test")
class OrderRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JpaOrderRepository jpaRepository;

    private OrderMapper mapper;
    private OrderRepositoryAdapter repository;

    @BeforeEach
    void setUp() {
        mapper = new OrderMapper();
        repository = new OrderRepositoryAdapter(jpaRepository, mapper);
    }

    @Test
    void should_save_and_retrieve_order() {
        // Given
        Order order = new Order(
                OrderId.generate(),
                CustomerId.of("CUST-001"),
                "台北市信義區"
        );
        order.addItem("PROD-001", "Product 1", 2, Money.twd(100));

        // When
        Order savedOrder = repository.save(order);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<Order> retrieved = repository.findById(savedOrder.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getId()).isEqualTo(savedOrder.getId());
        assertThat(retrieved.get().getItems()).hasSize(1);
    }

    @Test
    void should_find_orders_by_customer_id() {
        // Given
        CustomerId customerId = CustomerId.of("CUST-001");
        Order order1 = new Order(OrderId.generate(), customerId, "Address 1");
        Order order2 = new Order(OrderId.generate(), customerId, "Address 2");

        repository.save(order1);
        repository.save(order2);
        entityManager.flush();

        // When
        List<Order> orders = repository.findByCustomerId(customerId);

        // Then
        assertThat(orders).hasSize(2);
        assertThat(orders).allMatch(o -> o.getCustomerId().equals(customerId));
    }
}
```

---

## 總結

Repository Pattern 提供：
- **抽象化** - 領域不需要知道持久化細節
- **可測試性** - 易於模擬進行單元測試
- **靈活性** - 可以變更持久化技術
- **Clean Architecture** - 清楚的關注點分離
- **領域焦點** - 使用領域物件，而非實體

---

**相關文件**：
- [DDD Tactical Patterns](../../steering/ddd-tactical-patterns.md)
- [Aggregate Root Examples](aggregate-root-examples.md)
- [Domain Events Examples](domain-events-examples.md)
