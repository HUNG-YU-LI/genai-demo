---
adr_number: 026
title: "CQRS Pattern 用於 Read/Write Separation"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [020, 021, 025]
affected_viewpoints: ["information", "functional", "performance"]
affected_perspectives: ["performance", "scalability", "evolution"]
---

# ADR-026: CQRS Pattern 用於 Read/Write Separation

## 狀態

**Accepted** - 2025-10-25

## 上下文

### 問題陳述

The Enterprise E-Commerce Platform 需要optimized read 和 write operations 與 different characteristics:

**Business Requirements**:

- **Read Performance**: Fast product searches 和 catalog browsing
- **Write Consistency**: Strong consistency 用於 orders 和 payments
- **Scalability**: Scale reads independently from writes
- **複雜的 Queries**: 支援 複雜的 reporting 和 analytics
- **Real-time Updates**: Near real-time data 用於 customer-facing features
- **Flexibility**: Different data models 用於 reads 和 writes

**Technical Challenges**:

- Read-heavy workload (90% reads, 10% writes)
- 複雜的 queries spanning multiple aggregates
- Different performance requirements 用於 reads vs writes
- 需要 denormalized data 用於 queries
- Reporting requirements
- Search functionality

**Current Issues**:

- Single model 用於 reads 和 writes
- 複雜的 queries impact write performance
- 難以optimize 用於 both 使用案例s
- Denormalization creates data duplication
- Reporting queries slow down transactional operations

### 業務上下文

**業務驅動因素**：

- 改善 customer experience (fast searches)
- 支援 business intelligence 和 reporting
- 啟用 real-time dashboards
- Scale 用於 growth
- 降低 infrastructure costs

**限制條件**：

- 預算: $70,000 用於 implementation
- Timeline: 3 個月
- Team: 3 senior developers
- 必須 維持 資料一致性
- 可以not impact current operations
- Gradual adoption strategy

### 技術上下文

**Current Approach**:

- Single database model
- JPA entities 用於 all operations
- 複雜的 queries on write model
- Performance bottlenecks

**Target Approach**:

- Separate read 和 write models
- Optimized read models 用於 queries
- Event-driven synchronization
- Independent scaling

## 決策驅動因素

1. **Performance**: Optimize reads 和 writes independently
2. **Scalability**: Scale reads separately from writes
3. **Flexibility**: Different models 用於 different needs
4. **複雜的ity**: Manage additional 複雜的ity
5. **Consistency**: 維持 acceptable consistency
6. **維持ability**: Keep system 維持able
7. **成本**： Optimize infrastructure costs
8. **Team Skills**: Team capability to implement

## 考慮的選項

### 選項 1： CQRS with Event-Driven Synchronization (Recommended)

**描述**： Separate read and write models, synchronized through domain events

**Architecture Overview**:

```text
Write Side (Commands)          Read Side (Queries)
┌─────────────────┐           ┌─────────────────┐
│  Command API    │           │   Query API     │
└────────┬────────┘           └────────┬────────┘
         │                             │
         ▼                             ▼
┌─────────────────┐           ┌─────────────────┐
│ Domain Model    │           │  Read Models    │
│ (Aggregates)    │           │ (Projections)   │
└────────┬────────┘           └────────┬────────┘
         │                             │
         ▼                             ▼
┌─────────────────┐           ┌─────────────────┐
│  Write DB       │           │   Read DB       │
│  (PostgreSQL)   │           │ (PostgreSQL +   │
│                 │           │  ElasticSearch) │
└────────┬────────┘           └─────────────────┘
         │
         │ Domain Events
         └──────────────────────────────►
```

**Write Model (Command Side)**:

```java
// Command
public record CreateOrderCommand(
    OrderId orderId,
    CustomerId customerId,
    List<OrderItemDto> items,
    ShippingAddress shippingAddress
) {}

// Command Handler
@Service
@Transactional
public class OrderCommandService {
    
    private final OrderRepository orderRepository;
    private final DomainEventPublisher eventPublisher;
    
    public void createOrder(CreateOrderCommand command) {
        // Create aggregate
        Order order = new Order(
            command.orderId(),
            command.customerId(),
            command.items(),
            command.shippingAddress()
        );
        
        // Save to write database
        orderRepository.save(order);
        
        // Publish events for read model synchronization
        eventPublisher.publishEventsFromAggregate(order);
    }
    
    public void submitOrder(SubmitOrderCommand command) {
        Order order = orderRepository.findById(command.orderId())
            .orElseThrow();
        
        order.submit();
        orderRepository.save(order);
        
        eventPublisher.publishEventsFromAggregate(order);
    }
}

// Write Model (Domain Aggregate)
@Entity
@Table(name = "orders")
public class Order extends AggregateRoot {
    
    @Id
    private String orderId;
    
    @Column(name = "customer_id")
    private String customerId;
    
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    
    @OneToMany(cascade = CascadeType.ALL)
    private List<OrderItem> items;
    
    @Embedded
    private ShippingAddress shippingAddress;
    
    @Column(name = "total_amount")
    private BigDecimal totalAmount;
    
    // Business methods that collect events
    public void submit() {
        validateSubmission();
        status = OrderStatus.PENDING;
        collectEvent(OrderSubmittedEvent.create(orderId, customerId, totalAmount));
    }
}
```

**Read Model (Query Side)**:

```java
// Query
public record GetOrderQuery(OrderId orderId) {}

public record SearchOrdersQuery(
    CustomerId customerId,
    OrderStatus status,
    LocalDate startDate,
    LocalDate endDate,
    Pageable pageable
) {}

// Query Handler
@Service
@Transactional(readOnly = true)
public class OrderQueryService {
    
    private final OrderReadModelRepository readModelRepository;
    
    public OrderReadModel getOrder(GetOrderQuery query) {
        return readModelRepository.findById(query.orderId().getValue())
            .orElseThrow(() -> new OrderNotFoundException(query.orderId()));
    }
    
    public Page<OrderSummaryReadModel> searchOrders(SearchOrdersQuery query) {
        return readModelRepository.findByCustomerIdAndStatusAndDateRange(
            query.customerId().getValue(),
            query.status(),
            query.startDate(),
            query.endDate(),
            query.pageable()
        );
    }
}

// Read Model (Denormalized for Queries)
@Entity
@Table(name = "order_read_model")
public class OrderReadModel {
    
    @Id
    private String orderId;
    
    @Column(name = "customer_id")
    private String customerId;
    
    @Column(name = "customer_name")
    private String customerName;  // Denormalized
    
    @Column(name = "customer_email")
    private String customerEmail;  // Denormalized
    
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    
    @Column(name = "item_count")
    private Integer itemCount;
    
    @Column(name = "total_amount")
    private BigDecimal totalAmount;
    
    @Column(name = "shipping_address")
    private String shippingAddress;  // Denormalized as text
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    // Optimized for queries - no business logic
}

// Optimized repository for read model
@Repository
public interface OrderReadModelRepository 
    extends JpaRepository<OrderReadModel, String> {
    
    @Query("""
        SELECT o FROM OrderReadModel o
        WHERE o.customerId = :customerId
          AND (:status IS NULL OR o.status = :status)
          AND o.createdAt BETWEEN :startDate AND :endDate
        ORDER BY o.createdAt DESC
        """)
    Page<OrderReadModel> findByCustomerIdAndStatusAndDateRange(
        @Param("customerId") String customerId,
        @Param("status") OrderStatus status,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        Pageable pageable
    );
    
    @Query("""
        SELECT new OrderSummaryReadModel(
            o.orderId,
            o.customerName,
            o.status,
            o.totalAmount,
            o.createdAt
        )
        FROM OrderReadModel o
        WHERE o.status = :status
        ORDER BY o.createdAt DESC
        """)
    List<OrderSummaryReadModel> findRecentOrdersByStatus(
        @Param("status") OrderStatus status,
        Pageable pageable
    );
}
```

**Event-Driven Synchronization**:

```java
// Projection Builder (Updates Read Model)
@Component
public class OrderReadModelProjection {
    
    private final OrderReadModelRepository readModelRepository;
    private final CustomerQueryService customerQueryService;
    
    @EventListener
    @Transactional
    public void on(OrderCreatedEvent event) {
        // Get customer details for denormalization
        CustomerReadModel customer = customerQueryService
            .getCustomer(event.customerId());
        
        // Create read model
        OrderReadModel readModel = new OrderReadModel();
        readModel.setOrderId(event.orderId().getValue());
        readModel.setCustomerId(event.customerId().getValue());
        readModel.setCustomerName(customer.getName());
        readModel.setCustomerEmail(customer.getEmail());
        readModel.setStatus(OrderStatus.DRAFT);
        readModel.setItemCount(event.items().size());
        readModel.setTotalAmount(event.totalAmount());
        readModel.setShippingAddress(formatAddress(event.shippingAddress()));
        readModel.setCreatedAt(event.occurredOn());
        
        readModelRepository.save(readModel);
    }
    
    @EventListener
    @Transactional
    public void on(OrderSubmittedEvent event) {
        OrderReadModel readModel = readModelRepository
            .findById(event.orderId().getValue())
            .orElseThrow();
        
        readModel.setStatus(OrderStatus.PENDING);
        readModel.setSubmittedAt(event.occurredOn());
        
        readModelRepository.save(readModel);
    }
    
    @EventListener
    @Transactional
    public void on(OrderCompletedEvent event) {
        OrderReadModel readModel = readModelRepository
            .findById(event.orderId().getValue())
            .orElseThrow();
        
        readModel.setStatus(OrderStatus.COMPLETED);
        readModel.setCompletedAt(event.occurredOn());
        
        readModelRepository.save(readModel);
    }
    
    @EventListener
    @Transactional
    public void on(CustomerUpdatedEvent event) {
        // Update denormalized customer data in all orders
        List<OrderReadModel> orders = readModelRepository
            .findByCustomerId(event.customerId().getValue());
        
        for (OrderReadModel order : orders) {
            order.setCustomerName(event.name());
            order.setCustomerEmail(event.email());
        }
        
        readModelRepository.saveAll(orders);
    }
}
```

**API Separation**:

```java
// Command API (Write Operations)
@RestController
@RequestMapping("/api/v1/commands/orders")
public class OrderCommandController {
    
    private final OrderCommandService commandService;
    
    @PostMapping
    public ResponseEntity<OrderCreatedResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        
        CreateOrderCommand command = toCommand(request);
        commandService.createOrder(command);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new OrderCreatedResponse(command.orderId()));
    }
    
    @PostMapping("/{orderId}/submit")
    public ResponseEntity<Void> submitOrder(@PathVariable String orderId) {
        SubmitOrderCommand command = new SubmitOrderCommand(
            OrderId.of(orderId)
        );
        commandService.submitOrder(command);
        
        return ResponseEntity.ok().build();
    }
}

// Query API (Read Operations)
@RestController
@RequestMapping("/api/v1/queries/orders")
public class OrderQueryController {
    
    private final OrderQueryService queryService;
    
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderReadModel> getOrder(
            @PathVariable String orderId) {
        
        GetOrderQuery query = new GetOrderQuery(OrderId.of(orderId));
        OrderReadModel order = queryService.getOrder(query);
        
        return ResponseEntity.ok(order);
    }
    
    @GetMapping
    public ResponseEntity<Page<OrderSummaryReadModel>> searchOrders(
            @RequestParam String customerId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            Pageable pageable) {
        
        SearchOrdersQuery query = new SearchOrdersQuery(
            CustomerId.of(customerId),
            status,
            startDate,
            endDate,
            pageable
        );
        
        Page<OrderSummaryReadModel> orders = queryService.searchOrders(query);
        
        return ResponseEntity.ok(orders);
    }
}
```

**ElasticSearch 用於 Advanced Queries**:

```java
// Product search read model in ElasticSearch
@Document(indexName = "products")
public class ProductSearchModel {
    
    @Id
    private String productId;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String name;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;
    
    @Field(type = FieldType.Keyword)
    private String category;
    
    @Field(type = FieldType.Keyword)
    private List<String> tags;
    
    @Field(type = FieldType.Double)
    private BigDecimal price;
    
    @Field(type = FieldType.Integer)
    private Integer stockQuantity;
    
    @Field(type = FieldType.Double)
    private Double averageRating;
    
    @Field(type = FieldType.Integer)
    private Integer reviewCount;
}

// ElasticSearch repository
@Repository
public interface ProductSearchRepository 
    extends ElasticsearchRepository<ProductSearchModel, String> {
    
    @Query("""
        {
          "bool": {
            "must": [
              {
                "multi_match": {
                  "query": "?0",
                  "fields": ["name^2", "description", "tags"]
                }
              }
            ],
            "filter": [
              { "term": { "category": "?1" } },
              { "range": { "price": { "gte": ?2, "lte": ?3 } } },
              { "range": { "stockQuantity": { "gt": 0 } } }
            ]
          }
        }
        """)
    Page<ProductSearchModel> searchProducts(
        String searchText,
        String category,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Pageable pageable
    );
}

// Product search projection
@Component
public class ProductSearchProjection {
    
    private final ProductSearchRepository searchRepository;
    
    @EventListener
    @Transactional
    public void on(ProductCreatedEvent event) {
        ProductSearchModel searchModel = new ProductSearchModel();
        searchModel.setProductId(event.productId().getValue());
        searchModel.setName(event.name());
        searchModel.setDescription(event.description());
        searchModel.setCategory(event.category());
        searchModel.setTags(event.tags());
        searchModel.setPrice(event.price());
        searchModel.setStockQuantity(event.initialStock());
        searchModel.setAverageRating(0.0);
        searchModel.setReviewCount(0);
        
        searchRepository.save(searchModel);
    }
    
    @EventListener
    @Transactional
    public void on(ProductReviewedEvent event) {
        ProductSearchModel searchModel = searchRepository
            .findById(event.productId().getValue())
            .orElseThrow();
        
        // Update rating
        searchModel.setAverageRating(event.newAverageRating());
        searchModel.setReviewCount(event.totalReviews());
        
        searchRepository.save(searchModel);
    }
}
```

**Eventual Consistency Handling**:

```java
// Read model consistency checker
@Component
public class ReadModelConsistencyChecker {
    
    private final OrderReadModelRepository readModelRepository;
    private final OrderRepository writeRepository;
    
    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    public void checkConsistency() {
        // Find orders with mismatched status
        List<String> inconsistentOrders = findInconsistentOrders();
        
        if (!inconsistentOrders.isEmpty()) {
            logger.warn("Found {} inconsistent orders", 
                inconsistentOrders.size());
            
            // Trigger rebuild for inconsistent orders
            for (String orderId : inconsistentOrders) {
                rebuildReadModel(orderId);
            }
        }
    }
    
    private void rebuildReadModel(String orderId) {
        // Load from write model
        Order order = writeRepository.findById(OrderId.of(orderId))
            .orElseThrow();
        
        // Rebuild read model
        OrderReadModel readModel = buildReadModelFromAggregate(order);
        readModelRepository.save(readModel);
        
        logger.info("Rebuilt read model for order {}", orderId);
    }
}
```

**Monitoring 和 Metrics**:

```java
@Component
public class CQRSMetrics {
    
    private final MeterRegistry meterRegistry;
    
    @EventListener
    public void onCommandExecuted(CommandExecutedEvent event) {
        Counter.builder("cqrs.commands.executed")
            .tag("command", event.getCommandType())
            .tag("status", event.getStatus())
            .register(meterRegistry)
            .increment();
        
        Timer.builder("cqrs.commands.duration")
            .tag("command", event.getCommandType())
            .register(meterRegistry)
            .record(event.getDuration(), TimeUnit.MILLISECONDS);
    }
    
    @EventListener
    public void onQueryExecuted(QueryExecutedEvent event) {
        Counter.builder("cqrs.queries.executed")
            .tag("query", event.getQueryType())
            .register(meterRegistry)
            .increment();
        
        Timer.builder("cqrs.queries.duration")
            .tag("query", event.getQueryType())
            .register(meterRegistry)
            .record(event.getDuration(), TimeUnit.MILLISECONDS);
    }
    
    @EventListener
    public void onProjectionUpdated(ProjectionUpdatedEvent event) {
        Counter.builder("cqrs.projections.updated")
            .tag("projection", event.getProjectionType())
            .register(meterRegistry)
            .increment();
        
        Gauge.builder("cqrs.projections.lag")
            .tag("projection", event.getProjectionType())
            .register(meterRegistry, event, e -> e.getLagMillis());
    }
}
```

**優點**：

- ✅ Optimized read 和 write performance
- ✅ Independent scaling
- ✅ Flexible data models
- ✅ 複雜的 queries 沒有 impacting writes
- ✅ Natural fit 用於 event-driven architecture
- ✅ 支援s multiple read models (SQL + ElasticSearch)

**缺點**：

- ⚠️ Eventual consistency
- ⚠️ Increased 複雜的ity
- ⚠️ Data duplication
- ⚠️ Synchronization overhead
- ⚠️ More infrastructure to manage

**成本**： $70,000 implementation + $8,000/year operational

**風險**： **Medium** - Complexity and consistency challenges

### 選項 2： Simple Read Replicas

**描述**： Use database read replicas for read scaling

**優點**：

- ✅ 簡單implement
- ✅ Strong consistency
- ✅ No code changes

**缺點**：

- ❌ Same data model 用於 reads 和 writes
- ❌ Limited optimization
- ❌ Replication lag
- ❌ 可以not use different databases

**成本**： $20,000 implementation + $15,000/year (replicas)

**風險**： **Low** - But limited benefits

### 選項 3： Materialized Views

**描述**： Use database materialized views for complex queries

**優點**：

- ✅ Database-level optimization
- ✅ No application changes
- ✅ Familiar to DBAs

**缺點**：

- ❌ Database-specific
- ❌ Limited flexibility
- ❌ Refresh overhead
- ❌ 可以not use different databases

**成本**： $15,000 implementation

**風險**： **Low** - But limited scalability

## 決策結果

**選擇的選項**： **CQRS with Event-Driven Synchronization (Option 1)**

### 理由

CQRS 提供s the flexibility 和 performance optimization needed 用於 our read-heavy workload, allowing independent scaling 和 optimization of reads 和 writes while fitting naturally 與 our event-driven architecture.

## 影響分析

### 利害關係人影響

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | High | New pattern to learn | Training, examples, documentation |
| Database Team | Medium | New data models | Training, monitoring tools |
| QA Team | Medium | New testing approaches | Test frameworks, consistency checks |
| Operations Team | Medium | More infrastructure | Monitoring, automation |
| API Consumers | Low | Separate endpoints | Clear documentation |

### Impact Radius Assessment

**選擇的影響半徑**： **System**

影響：

- All bounded contexts
- API design
- Database architecture
- Event-driven architecture
- Monitoring systems

### 風險評估

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| Eventual consistency issues | Medium | Medium | Consistency checks, monitoring |
| Projection lag | Low | Medium | Performance optimization, alerts |
| Data synchronization failures | Low | High | Retry mechanism, manual tools |
| Increased 複雜的ity | High | Medium | Training, documentation, tooling |
| Storage costs | Low | Low | Optimize projections, archiving |

**整體風險等級**： **Medium**

## 實作計畫

### 第 1 階段： Framework Setup (Month 1)

**Tasks**:

- [ ] Design CQRS infrastructure
- [ ] Implement projection framework
- [ ] Set up monitoring
- [ ] Create testing utilities
- [ ] Document patterns

**Success Criteria**:

- Framework operational
- Monitoring working
- Documentation complete

### 第 2 階段： Pilot Implementation (Month 2)

**Tasks**:

- [ ] Implement Order CQRS
- [ ] Create read models
- [ ] Build projections
- [ ] Test in staging
- [ ] Gather feedback

**Success Criteria**:

- Order CQRS working
- Performance 改善d
- Team comfortable

### 第 3 階段： Expansion (Month 3)

**Tasks**:

- [ ] Implement Product CQRS 與 ElasticSearch
- [ ] Implement Customer CQRS
- [ ] Add consistency checks
- [ ] Optimize performance
- [ ] Update documentation

**Success Criteria**:

- All critical entities using CQRS
- Performance targets met
- Team trained

### 回滾策略

**觸發條件**：

- Unacceptable consistency issues
- Performance degradation
- Team 可以not 維持

**回滾步驟**：

1. Disable read models
2. Use write model 用於 queries
3. Fix issues
4. Re-啟用 CQRS

**回滾時間**： 1 day

## 監控和成功標準

### 成功指標

| Metric | Target | Measurement |
|--------|--------|-------------|
| Read Query Performance | < 100ms p95 | Application metrics |
| Write Performance | < 200ms p95 | Application metrics |
| Projection Lag | < 1s | Monitoring |
| Consistency Rate | > 99.9% | Consistency checks |
| Read Scalability | 10x 改善ment | Load testing |

### Review Schedule

- **Weekly**: Performance metrics review
- **Monthly**: Pattern optimization
- **Quarterly**: Strategy review

## 後果

### 正面後果

- ✅ **Performance**: Optimized reads 和 writes
- ✅ **Scalability**: Independent scaling
- ✅ **Flexibility**: Different models 用於 different needs
- ✅ **複雜的 Queries**: 支援 沒有 impacting writes
- ✅ **Multiple Stores**: SQL + ElasticSearch
- ✅ **Cost Optimization**: Scale only what's needed

### 負面後果

- ⚠️ **複雜的ity**: Signifi可以t increase
- ⚠️ **Eventual Consistency**: Not immediate
- ⚠️ **Data Duplication**: More storage
- ⚠️ **Synchronization**: Overhead 和 potential failures
- ⚠️ **Testing**: More 複雜的 scenarios

### 技術債務

**已識別債務**：

1. Manual projection rebuild
2. Limited consistency monitoring
3. Basic error handling
4. No projection versioning

**債務償還計畫**：

- **Q2 2026**: Automated projection rebuild
- **Q3 2026**: Comprehensive consistency monitoring
- **Q4 2026**: Advanced error handling
- **Q1 2027**: Projection versioning

## 相關決策

- [ADR-020: Database Migration Strategy 與 Flyway](020-database-migration-strategy-flyway.md)
- [ADR-021: Event Sourcing 用於 Critical Aggregates](021-event-sourcing-critical-aggregates.md)
- [ADR-025: Saga Pattern 用於 Distributed Transactions](025-saga-pattern-distributed-transactions.md)

---

**文檔狀態**： ✅ Accepted  
**上次審查**： 2025-10-25  
**下次審查**： 2026-01-25 （每季）

## 備註

### CQRS Best Practices

**DO**:

- ✅ Use 用於 read-heavy workloads
- ✅ Optimize read models 用於 queries
- ✅ Monitor projection lag
- ✅ Implement consistency checks
- ✅ Use different databases when beneficial
- ✅ Keep write model normalized

**DON'T**:

- ❌ Apply to all entities
- ❌ Ignore eventual consistency
- ❌ Over-denormalize
- ❌ Skip monitoring
- ❌ Forget about projection failures

### When to Use CQRS

**良好的 可以didates**:

- ✅ Read-heavy entities (products, catalog)
- ✅ 複雜的 query requirements
- ✅ Different scaling needs
- ✅ Multiple read models needed

**Poor 可以didates**:

- ❌ 簡單的 CRUD entities
- ❌ Low traffic entities
- ❌ Strong consistency required
- ❌ 簡單的 queries only
