---
adr_number: 021
title: "Event Sourcing 用於 Critical Aggregates (Optional Pattern)"
date: 2025-10-25
status: "proposed"
supersedes: []
superseded_by: null
related_adrs: [020, 025, 026]
affected_viewpoints: ["information", "functional"]
affected_perspectives: ["availability", "evolution", "performance"]
---

# ADR-021: Event Sourcing 用於 Critical Aggregates (Optional Pattern)

## 狀態

**Proposed** - 2025-10-25

*Note: This is an optional pattern 用於 future consideration. Not required 用於 initial implementation.*

## 上下文

### 問題陳述

Certain critical aggregates in the E-Commerce Platform could benefit from event sourcing to:

**Business Requirements**:

- **Complete Audit Trail**: Full history of all state changes
- **Temporal Queries**: Query state at any point in time
- **Regulatory Compliance**: Meet audit 和 compliance requirements
- **Debugging**: Reproduce issues 透過 replaying events
- **Analytics**: Analyze business patterns from event history
- **Undo/Redo**: 支援 複雜的 business workflows

**Technical Challenges**:

- Current state-based persistence loses history
- 難以audit changes
- 可以not replay past states
- Limited debugging capabilities
- 複雜的 temporal queries
- Compliance requirements 用於 financial data

**可以didate Aggregates**:

- **Order**: Complete order lifecycle tracking
- **Payment**: Financial transaction history
- **Inventory**: Stock movement tracking
- **Pricing**: Price change history
- **Customer**: Account activity history

### 業務上下文

**業務驅動因素**：

- Regulatory compliance (financial auditing)
- Customer dispute resolution
- Business intelligence 和 analytics
- Fraud detection 和 prevention
- Customer service 改善ments

**限制條件**：

- 預算: $80,000 用於 implementation
- Timeline: 3 個月
- Team: 2 senior developers
- 必須 coexist 與 existing CRUD approach
- 可以not impact current performance
- Gradual adoption strategy

### 技術上下文

**Current Approach**:

- Traditional CRUD 與 JPA
- Domain events 用於 integration
- Limited audit logging
- No event history

**Target Approach**:

- Event sourcing 用於 critical aggregates
- Event store 用於 persistence
- Projections 用於 read models
- Hybrid approach (not all aggregates)

## 決策驅動因素

1. **Auditability**: Complete audit trail 用於 compliance
2. **Temporal Queries**: Query historical states
3. **Debugging**: Reproduce issues from events
4. **Analytics**: 豐富的 event data 用於 analysis
5. **複雜的ity**: Manage additional 複雜的ity
6. **Performance**: 維持 acceptable performance
7. **Team Skills**: Team capability to implement
8. **Gradual Adoption**: Start small, expand if successful

## 考慮的選項

### 選項 1： Selective Event Sourcing (Recommended)

**描述**： Apply event sourcing only to critical aggregates that benefit most

**Event Sourcing Architecture**:

```java
// Event-sourced aggregate
@AggregateRoot
public class Order extends EventSourcedAggregateRoot {
    
    private OrderId id;
    private CustomerId customerId;
    private OrderStatus status;
    private List<OrderItem> items;
    private Money totalAmount;
    
    // Constructor for new aggregate
    public Order(OrderId id, CustomerId customerId, List<OrderItem> items) {
        // Apply event (not save state directly)
        apply(OrderCreatedEvent.create(id, customerId, items));
    }
    
    // Constructor for reconstitution from events
    protected Order(OrderId id) {
        this.id = id;
    }
    
    // Business method
    public void submit() {
        if (status != OrderStatus.DRAFT) {
            throw new BusinessRuleViolationException("Order already submitted");
        }
        
        apply(OrderSubmittedEvent.create(id, LocalDateTime.now()));
    }
    
    public void cancel(String reason) {
        if (status == OrderStatus.CANCELLED) {
            throw new BusinessRuleViolationException("Order already cancelled");
        }
        
        apply(OrderCancelledEvent.create(id, reason, LocalDateTime.now()));
    }
    
    // Event handlers (update state)
    @EventHandler
    private void on(OrderCreatedEvent event) {
        this.id = event.orderId();
        this.customerId = event.customerId();
        this.items = event.items();
        this.status = OrderStatus.DRAFT;
        this.totalAmount = calculateTotal(items);
    }
    
    @EventHandler
    private void on(OrderSubmittedEvent event) {
        this.status = OrderStatus.PENDING;
    }
    
    @EventHandler
    private void on(OrderCancelledEvent event) {
        this.status = OrderStatus.CANCELLED;
    }
}
```

**Event Store Interface**:

```java
public interface EventStore {
    
    /**

     * Save events for an aggregate

     */
    void saveEvents(String aggregateId, 
                   List<DomainEvent> events, 
                   long expectedVersion);
    
    /**

     * Load all events for an aggregate

     */
    List<DomainEvent> getEvents(String aggregateId);
    
    /**

     * Load events after a specific version

     */
    List<DomainEvent> getEventsAfterVersion(String aggregateId, long version);
    
    /**

     * Load events in a time range

     */
    List<DomainEvent> getEventsBetween(String aggregateId, 
                                       LocalDateTime start, 
                                       LocalDateTime end);
}
```

**Event Store Implementation (PostgreSQL)**:

```sql
-- Event store table
CREATE TABLE event_store (
    event_id UUID PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    event_data JSONB NOT NULL,
    event_metadata JSONB,
    version BIGINT NOT NULL,
    occurred_on TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    
    CONSTRAINT uk_aggregate_version UNIQUE (aggregate_id, version)
);

-- Indexes for performance
CREATE INDEX idx_event_store_aggregate 
ON event_store(aggregate_id, version);

CREATE INDEX idx_event_store_type 
ON event_store(aggregate_type, occurred_on);

CREATE INDEX idx_event_store_occurred 
ON event_store(occurred_on);

-- Snapshots table for performance
CREATE TABLE aggregate_snapshots (
    snapshot_id UUID PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    snapshot_data JSONB NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    
    CONSTRAINT uk_aggregate_snapshot UNIQUE (aggregate_id, version)
);

CREATE INDEX idx_snapshots_aggregate 
ON aggregate_snapshots(aggregate_id, version DESC);
```

**Event Store Repository**:

```java
@Repository
public class PostgresEventStore implements EventStore {
    
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    
    @Override
    @Transactional
    public void saveEvents(String aggregateId, 
                          List<DomainEvent> events, 
                          long expectedVersion) {
        
        // Optimistic concurrency check
        Long currentVersion = getCurrentVersion(aggregateId);
        if (currentVersion != null && currentVersion != expectedVersion) {
            throw new ConcurrencyException(
                "Aggregate version mismatch. Expected: " + expectedVersion + 
                ", Current: " + currentVersion);
        }
        
        // Save events
        long version = expectedVersion;
        for (DomainEvent event : events) {
            version++;
            
            jdbcTemplate.update("""
                INSERT INTO event_store (
                    event_id, aggregate_id, aggregate_type, event_type,
                    event_data, event_metadata, version, occurred_on
                ) VALUES (?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?)
                """,
                event.getEventId(),
                aggregateId,
                getAggregateType(event),
                event.getEventType(),
                serializeEvent(event),
                serializeMetadata(event),
                version,
                event.getOccurredOn()
            );
        }
    }
    
    @Override
    public List<DomainEvent> getEvents(String aggregateId) {
        return jdbcTemplate.query("""
            SELECT event_data, event_type
            FROM event_store
            WHERE aggregate_id = ?
            ORDER BY version ASC
            """,
            (rs, rowNum) -> deserializeEvent(
                rs.getString("event_data"),
                rs.getString("event_type")
            ),
            aggregateId
        );
    }
    
    @Override
    public List<DomainEvent> getEventsBetween(String aggregateId,
                                              LocalDateTime start,
                                              LocalDateTime end) {
        return jdbcTemplate.query("""
            SELECT event_data, event_type
            FROM event_store
            WHERE aggregate_id = ?
              AND occurred_on BETWEEN ? AND ?
            ORDER BY version ASC
            """,
            (rs, rowNum) -> deserializeEvent(
                rs.getString("event_data"),
                rs.getString("event_type")
            ),
            aggregateId, start, end
        );
    }
    
    private Long getCurrentVersion(String aggregateId) {
        return jdbcTemplate.queryForObject("""
            SELECT MAX(version)
            FROM event_store
            WHERE aggregate_id = ?
            """,
            Long.class,
            aggregateId
        );
    }
}
```

**Aggregate Repository 與 Event Sourcing**:

```java
@Repository
public class EventSourcedOrderRepository implements OrderRepository {
    
    private final EventStore eventStore;
    private final SnapshotStore snapshotStore;
    
    @Override
    public Optional<Order> findById(OrderId orderId) {
        String aggregateId = orderId.getValue();
        
        // Try to load from snapshot
        Optional<AggregateSnapshot> snapshot = 
            snapshotStore.getLatestSnapshot(aggregateId);
        
        Order order;
        long version;
        
        if (snapshot.isPresent()) {
            // Reconstitute from snapshot
            order = deserializeSnapshot(snapshot.get());
            version = snapshot.get().getVersion();
            
            // Load events after snapshot
            List<DomainEvent> events = 
                eventStore.getEventsAfterVersion(aggregateId, version);
            order.loadFromHistory(events);
        } else {
            // Load all events
            List<DomainEvent> events = eventStore.getEvents(aggregateId);
            if (events.isEmpty()) {
                return Optional.empty();
            }
            
            // Reconstitute from events
            order = new Order(orderId);
            order.loadFromHistory(events);
        }
        
        return Optional.of(order);
    }
    
    @Override
    @Transactional
    public Order save(Order order) {
        List<DomainEvent> uncommittedEvents = order.getUncommittedEvents();
        
        if (!uncommittedEvents.isEmpty()) {
            // Save events
            eventStore.saveEvents(
                order.getId().getValue(),
                uncommittedEvents,
                order.getVersion()
            );
            
            // Mark events as committed
            order.markEventsAsCommitted();
            
            // Create snapshot if needed
            if (shouldCreateSnapshot(order)) {
                snapshotStore.saveSnapshot(
                    order.getId().getValue(),
                    serializeAggregate(order),
                    order.getVersion()
                );
            }
        }
        
        return order;
    }
    
    private boolean shouldCreateSnapshot(Order order) {
        // Create snapshot every 50 events
        return order.getVersion() % 50 == 0;
    }
}
```

**Projection 用於 Read Model**:

```java
@Component
public class OrderProjection {
    
    private final OrderReadModelRepository readModelRepository;
    
    @EventListener
    @Transactional
    public void on(OrderCreatedEvent event) {
        OrderReadModel readModel = new OrderReadModel(
            event.orderId().getValue(),
            event.customerId().getValue(),
            OrderStatus.DRAFT,
            event.totalAmount(),
            event.occurredOn()
        );
        
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
    public void on(OrderCancelledEvent event) {
        OrderReadModel readModel = readModelRepository
            .findById(event.orderId().getValue())
            .orElseThrow();
        
        readModel.setStatus(OrderStatus.CANCELLED);
        readModel.setCancelledAt(event.occurredOn());
        readModel.setCancellationReason(event.reason());
        
        readModelRepository.save(readModel);
    }
}
```

**Temporal Queries**:

```java
@Service
public class OrderHistoryService {
    
    private final EventStore eventStore;
    
    /**

     * Get order state at specific point in time

     */
    public Order getOrderAtTime(OrderId orderId, LocalDateTime timestamp) {
        List<DomainEvent> events = eventStore.getEventsBetween(
            orderId.getValue(),
            LocalDateTime.MIN,
            timestamp
        );
        
        Order order = new Order(orderId);
        order.loadFromHistory(events);
        
        return order;
    }
    
    /**

     * Get all changes to order in time range

     */
    public List<OrderChange> getOrderChanges(OrderId orderId,
                                             LocalDateTime start,
                                             LocalDateTime end) {
        List<DomainEvent> events = eventStore.getEventsBetween(
            orderId.getValue(),
            start,
            end
        );
        
        return events.stream()
            .map(this::toOrderChange)
            .collect(Collectors.toList());
    }
    
    /**

     * Replay events for debugging

     */
    public void replayOrderEvents(OrderId orderId) {
        List<DomainEvent> events = eventStore.getEvents(orderId.getValue());
        
        Order order = new Order(orderId);
        
        for (DomainEvent event : events) {
            logger.info("Replaying event: {} at {}",
                event.getEventType(),
                event.getOccurredOn());
            
            order.loadFromHistory(List.of(event));
            
            logger.info("Order state after event: {}", order);
        }
    }
}
```

**優點**：

- ✅ Complete audit trail 用於 critical aggregates
- ✅ Temporal queries 和 time travel
- ✅ 優秀的 debugging capabilities
- ✅ Natural fit 用於 event-driven architecture
- ✅ 支援s 複雜的 business workflows
- ✅ Regulatory compliance
- ✅ 可以 coexist 與 CRUD approach

**缺點**：

- ⚠️ Increased 複雜的ity
- ⚠️ Learning curve 用於 team
- ⚠️ More storage required
- ⚠️ Eventual consistency 用於 read models
- ⚠️ Snapshot management needed
- ⚠️ Event versioning challenges

**成本**： $80,000 implementation + $10,000/year operational

**風險**： **Medium** - Significant complexity increase

### 選項 2： Full Event Sourcing

**描述**： Apply event sourcing to all aggregates

**優點**：

- ✅ Consistent approach 跨 system
- ✅ Maximum auditability
- ✅ Simplified architecture (one pattern)

**缺點**：

- ❌ Very high 複雜的ity
- ❌ Signifi可以t performance overhead
- ❌ 大型的 storage requirements
- ❌ Steep learning curve
- ❌ Overkill 用於 簡單的 aggregates

**成本**： $200,000 implementation + $30,000/year

**風險**： **High** - Too complex for current needs

### 選項 3： Enhanced Audit Logging

**描述**： Keep CRUD but add comprehensive audit logging

**優點**：

- ✅ 簡單implement
- ✅ Low 複雜的ity
- ✅ Familiar to team

**缺點**：

- ❌ Limited temporal queries
- ❌ 可以not replay state
- ❌ Less powerful 用於 debugging
- ❌ Not true event sourcing

**成本**： $20,000 implementation

**風險**： **Low** - But limited capabilities

## 決策結果

**選擇的選項**： **Selective Event Sourcing (Option 1)** - Proposed for future implementation

### 理由

Selective event sourcing 用於 critical aggregates (Order, Payment) 提供s the best balance of benefits 和 複雜的ity, allowing gradual adoption 和 learning while delivering value 用於 compliance 和 debugging.

**Implementation Recommendation**: Start 與 Order aggregate as pilot, expand to Payment if successful.

## 影響分析

### 利害關係人影響

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | High | New programming model | Training, pair programming, examples |
| Database Team | Medium | New storage patterns | Training, monitoring tools |
| QA Team | Medium | New testing approaches | Test frameworks, examples |
| Operations Team | Medium | New monitoring needs | Dashboards, runbooks |
| Compliance Team | Low | 更好的 audit capabilities | Documentation, reports |

### Impact Radius Assessment

**選擇的影響半徑**： **Bounded Context**

影響：

- Order bounded context (initially)
- Payment bounded context (future)
- Event store infrastructure
- Read model projections
- Reporting systems

### 風險評估

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| 複雜的ity overwhelms team | Medium | High | Start small, extensive training |
| Performance issues | Low | Medium | Snapshots, caching, monitoring |
| Event versioning problems | Medium | Medium | Upcasting strategy, testing |
| Storage growth | Low | Low | Archiving strategy, compression |
| Eventual consistency issues | Medium | Medium | Clear documentation, monitoring |

**整體風險等級**： **Medium**

## 實作計畫

### 第 1 階段： Proof of Concept (Month 1)

**Tasks**:

- [ ] Implement event store
- [ ] Create Order aggregate 與 event sourcing
- [ ] Build 簡單的 projection
- [ ] Test temporal queries
- [ ] Measure performance
- [ ] Document learnings

**Success Criteria**:

- POC working
- Performance acceptable
- Team understands approach

### 第 2 階段： Production Implementation (Month 2)

**Tasks**:

- [ ] Production-ready event store
- [ ] Snapshot mechanism
- [ ] Complete Order projections
- [ ] Migration strategy
- [ ] Monitoring 和 alerting
- [ ] Documentation

**Success Criteria**:

- Production ready
- All features working
- Monitoring in place

### 第 3 階段： Rollout and Validation (Month 3)

**Tasks**:

- [ ] Deploy to production
- [ ] Monitor performance
- [ ] Validate audit capabilities
- [ ] Gather team feedback
- [ ] Decide on expansion

**Success Criteria**:

- Production stable
- Benefits realized
- Team comfortable

### 回滾策略

**觸發條件**：

- Unacceptable performance
- Team 可以not 維持
- Benefits not realized

**回滾步驟**：

1. Stop using event-sourced aggregates
2. Migrate to CRUD approach
3. Archive event store
4. Update documentation

**回滾時間**： 2 weeks

## 監控和成功標準

### 成功指標

| Metric | Target | Measurement |
|--------|--------|-------------|
| Audit Query Time | < 1s | Event store metrics |
| Aggregate Load Time | < 100ms | Application metrics |
| Storage Growth | < 10GB/month | Database metrics |
| Projection Lag | < 1s | Monitoring |
| Team Satisfaction | > 7/10 | Survey |

### Review Schedule

- **Monthly**: Performance 和 usage review
- **Quarterly**: Value assessment
- **Annually**: Expansion decision

## 後果

### 正面後果

- ✅ **Complete Audit Trail**: Full history 用於 compliance
- ✅ **Temporal Queries**: Query any historical state
- ✅ **Debugging**: Replay events to reproduce issues
- ✅ **Analytics**: 豐富的 event data 用於 analysis
- ✅ **Compliance**: Meet regulatory requirements
- ✅ **Flexibility**: 支援 複雜的 workflows

### 負面後果

- ⚠️ **複雜的ity**: Signifi可以t increase in 複雜的ity
- ⚠️ **Learning Curve**: Team needs training
- ⚠️ **Storage**: More storage required
- ⚠️ **Eventual Consistency**: Read models lag behind
- ⚠️ **Maintenance**: More code to 維持

### 技術債務

**已識別債務**：

1. Event versioning strategy needed
2. Snapshot optimization required
3. Projection rebuild mechanism
4. Event archiving strategy

**債務償還計畫**：

- **Q2 2026**: Event versioning framework
- **Q3 2026**: Snapshot optimization
- **Q4 2026**: Projection rebuild tools
- **Q1 2027**: Event archiving

## 相關決策

- [ADR-020: Database Migration Strategy 與 Flyway](020-database-migration-strategy-flyway.md)
- [ADR-025: Saga Pattern 用於 Distributed Transactions](025-saga-pattern-distributed-transactions.md)
- [ADR-026: CQRS Pattern 用於 Read/Write Separation](026-cqrs-pattern-read-write-separation.md)

---

**文檔狀態**： 📋 Proposed (Optional Pattern)  
**上次審查**： 2025-10-25  
**下次審查**： 2026-04-25 (After evaluation period)

## 備註

### When to Use Event Sourcing

**良好的 可以didates**:

- ✅ Aggregates requiring complete audit trail
- ✅ Financial transactions
- ✅ Regulatory compliance requirements
- ✅ 複雜的 business workflows
- ✅ Temporal queries needed

**Poor 可以didates**:

- ❌ 簡單的 CRUD entities
- ❌ Reference data
- ❌ High-volume, low-value data
- ❌ Frequently changing schemas

### Event Sourcing Best Practices

**DO**:

- ✅ Start 與 one aggregate
- ✅ Use snapshots 用於 performance
- ✅ Version events properly
- ✅ Keep events immutable
- ✅ Use projections 用於 queries
- ✅ Monitor projection lag

**DON'T**:

- ❌ Apply to all aggregates
- ❌ Store 大型的 payloads in events
- ❌ Modify past events
- ❌ Query event store directly
- ❌ Ignore event versioning
- ❌ Skip snapshot strategy
