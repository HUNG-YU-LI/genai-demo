---
adr_number: 025
title: "Saga Pattern 用於 Distributed Transactions"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [020, 021, 026]
affected_viewpoints: ["functional", "concurrency"]
affected_perspectives: ["availability", "performance"]
---

# ADR-025: Saga Pattern 用於 Distributed Transactions

## 狀態

**Accepted** - 2025-10-25

## 上下文

### 問題陳述

The Enterprise E-Commerce Platform 需要reliable way to 處理 distributed transactions 跨 multiple bounded contexts:

**Business Requirements**:

- **Data Consistency**: 維持 consistency 跨 microservices
- **Reliability**: Ensure business processes complete successfully
- **Compensation**: 處理 failures gracefully 與 rollback
- **Visibility**: Track long-running business processes
- **Performance**: Avoid blocking distributed transactions
- **Scalability**: 支援 high transaction volumes

**Technical Challenges**:

- 13 bounded contexts 與 separate databases
- No distributed transactions (2PC not suitable)
- 複雜的 business workflows spanning multiple services
- 需要 compensation logic
- Failure handling 和 recovery
- Monitoring long-running processes

**Example Workflows**:

1. **Order Submission**: Order → Inventory → Payment → Shipping
2. **Order 可以cellation**: Order → Payment Refund → Inventory Release
3. **Customer Registration**: Customer → Email → Loyalty Program

### 業務上下文

**業務驅動因素**：

- Ensure order processing reliability
- 處理 payment failures gracefully
- 維持 inventory accuracy
- 支援 複雜的 business workflows
- 啟用 business process monitoring

**限制條件**：

- 預算: $60,000 用於 implementation
- Timeline: 2 個月
- Team: 3 senior developers
- 必須 work 與 existing event-driven architecture
- 可以not use distributed transactions (2PC)
- 必須 支援 compensation

### 技術上下文

**Current Approach**:

- Domain events 用於 integration
- No coordination mechanism
- Manual compensation logic
- 難以track workflow state

**Target Approach**:

- Saga pattern 用於 orchestration
- Automated compensation
- Workflow state tracking
- Monitoring 和 observability

## 決策驅動因素

1. **Consistency**: 維持 eventual consistency 跨 services
2. **Reliability**: Ensure workflows complete 或 compensate
3. **Observability**: Track workflow progress 和 failures
4. **Simplicity**: 容易understand 和 implement
5. **Performance**: Non-blocking, asynchronous execution
6. **Scalability**: 處理 high transaction volumes
7. **維持ability**: 容易add new workflows
8. **Testability**: 容易test compensation logic

## 考慮的選項

### 選項 1： Choreography-Based Saga (Recommended)

**描述**： Services coordinate through domain events, no central orchestrator

**Order Submission Saga Example**:

```java
// Step 1: Order Service - Create Order
@Service
@Transactional
public class OrderApplicationService {
    
    public void submitOrder(SubmitOrderCommand command) {
        // Create order
        Order order = orderRepository.findById(command.orderId())
            .orElseThrow();
        
        order.submit();
        orderRepository.save(order);
        
        // Publish event (triggers next step)
        eventPublisher.publish(OrderSubmittedEvent.create(
            order.getId(),
            order.getCustomerId(),
            order.getItems(),
            order.getTotalAmount()
        ));
    }
}

// Step 2: Inventory Service - Reserve Items
@Component
public class OrderSubmittedEventHandler {
    
    @EventListener
    @Transactional
    public void handle(OrderSubmittedEvent event) {
        try {
            // Reserve inventory
            inventoryService.reserveItems(
                event.orderId(),
                event.items()
            );
            
            // Publish success event (triggers next step)
            eventPublisher.publish(InventoryReservedEvent.create(
                event.orderId(),
                event.items()
            ));
            
        } catch (InsufficientInventoryException e) {
            // Publish failure event (triggers compensation)
            eventPublisher.publish(InventoryReservationFailedEvent.create(
                event.orderId(),
                e.getMessage()
            ));
        }
    }
}

// Step 3: Payment Service - Process Payment
@Component
public class InventoryReservedEventHandler {
    
    @EventListener
    @Transactional
    public void handle(InventoryReservedEvent event) {
        try {
            // Process payment
            Payment payment = paymentService.processPayment(
                event.orderId(),
                event.amount()
            );
            
            // Publish success event (triggers next step)
            eventPublisher.publish(PaymentProcessedEvent.create(
                event.orderId(),
                payment.getId(),
                payment.getAmount()
            ));
            
        } catch (PaymentFailedException e) {
            // Publish failure event (triggers compensation)
            eventPublisher.publish(PaymentFailedEvent.create(
                event.orderId(),
                e.getMessage()
            ));
        }
    }
}

// Step 4: Order Service - Confirm Order
@Component
public class PaymentProcessedEventHandler {
    
    @EventListener
    @Transactional
    public void handle(PaymentProcessedEvent event) {
        Order order = orderRepository.findById(event.orderId())
            .orElseThrow();
        
        order.confirm();
        orderRepository.save(order);
        
        // Publish completion event
        eventPublisher.publish(OrderConfirmedEvent.create(
            event.orderId()
        ));
    }
}

// Compensation: Payment Failed - Release Inventory
@Component
public class PaymentFailedEventHandler {
    
    @EventListener
    @Transactional
    public void handle(PaymentFailedEvent event) {
        // Release reserved inventory
        inventoryService.releaseReservation(event.orderId());
        
        // Update order status
        Order order = orderRepository.findById(event.orderId())
            .orElseThrow();
        
        order.fail("Payment failed: " + event.reason());
        orderRepository.save(order);
        
        // Publish compensation event
        eventPublisher.publish(OrderFailedEvent.create(
            event.orderId(),
            "Payment failed"
        ));
    }
}

// Compensation: Inventory Failed - Cancel Order
@Component
public class InventoryReservationFailedEventHandler {
    
    @EventListener
    @Transactional
    public void handle(InventoryReservationFailedEvent event) {
        // Update order status
        Order order = orderRepository.findById(event.orderId())
            .orElseThrow();
        
        order.fail("Insufficient inventory: " + event.reason());
        orderRepository.save(order);
        
        // Publish compensation event
        eventPublisher.publish(OrderFailedEvent.create(
            event.orderId(),
            "Insufficient inventory"
        ));
    }
}
```

**Saga State Tracking**:

```java
// Track saga execution state
@Entity
@Table(name = "saga_instances")
public class SagaInstance {
    
    @Id
    private String sagaId;
    
    @Column(name = "saga_type")
    private String sagaType;
    
    @Column(name = "aggregate_id")
    private String aggregateId;
    
    @Column(name = "current_step")
    private String currentStep;
    
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private SagaStatus status;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "error_message")
    private String errorMessage;
    
    @Column(name = "compensation_started_at")
    private LocalDateTime compensationStartedAt;
}

@Service
public class SagaTracker {
    
    private final SagaInstanceRepository repository;
    
    public void startSaga(String sagaType, String aggregateId) {
        SagaInstance saga = new SagaInstance(
            UUID.randomUUID().toString(),
            sagaType,
            aggregateId,
            "STARTED",
            SagaStatus.IN_PROGRESS,
            LocalDateTime.now()
        );
        
        repository.save(saga);
    }
    
    public void updateStep(String aggregateId, String step) {
        SagaInstance saga = repository.findByAggregateId(aggregateId)
            .orElseThrow();
        
        saga.setCurrentStep(step);
        repository.save(saga);
    }
    
    public void completeSaga(String aggregateId) {
        SagaInstance saga = repository.findByAggregateId(aggregateId)
            .orElseThrow();
        
        saga.setStatus(SagaStatus.COMPLETED);
        saga.setCompletedAt(LocalDateTime.now());
        repository.save(saga);
    }
    
    public void failSaga(String aggregateId, String error) {
        SagaInstance saga = repository.findByAggregateId(aggregateId)
            .orElseThrow();
        
        saga.setStatus(SagaStatus.COMPENSATING);
        saga.setErrorMessage(error);
        saga.setCompensationStartedAt(LocalDateTime.now());
        repository.save(saga);
    }
    
    public void compensationComplete(String aggregateId) {
        SagaInstance saga = repository.findByAggregateId(aggregateId)
            .orElseThrow();
        
        saga.setStatus(SagaStatus.COMPENSATED);
        saga.setCompletedAt(LocalDateTime.now());
        repository.save(saga);
    }
}
```

**Saga Monitoring**:

```java
@RestController
@RequestMapping("/api/v1/sagas")
public class SagaMonitoringController {
    
    private final SagaInstanceRepository repository;
    
    @GetMapping("/{sagaId}")
    public SagaInstanceDto getSagaStatus(@PathVariable String sagaId) {
        SagaInstance saga = repository.findById(sagaId)
            .orElseThrow(() -> new SagaNotFoundException(sagaId));
        
        return SagaInstanceDto.from(saga);
    }
    
    @GetMapping("/aggregate/{aggregateId}")
    public SagaInstanceDto getSagaByAggregate(@PathVariable String aggregateId) {
        SagaInstance saga = repository.findByAggregateId(aggregateId)
            .orElseThrow(() -> new SagaNotFoundException(aggregateId));
        
        return SagaInstanceDto.from(saga);
    }
    
    @GetMapping("/failed")
    public List<SagaInstanceDto> getFailedSagas() {
        return repository.findByStatus(SagaStatus.FAILED)
            .stream()
            .map(SagaInstanceDto::from)
            .collect(Collectors.toList());
    }
}
```

**Idempotency 用於 Saga Steps**:

```java
@Component
public class IdempotentEventHandler {
    
    private final ProcessedEventRepository processedEventRepository;
    
    @EventListener
    @Transactional
    public void handle(OrderSubmittedEvent event) {
        // Check if already processed
        if (isEventProcessed(event.getEventId())) {
            logger.info("Event {} already processed, skipping", 
                event.getEventId());
            return;
        }
        
        try {
            // Process event
            processOrderSubmission(event);
            
            // Mark as processed
            markEventAsProcessed(event.getEventId());
            
        } catch (Exception e) {
            logger.error("Failed to process event {}", 
                event.getEventId(), e);
            throw e;
        }
    }
    
    private boolean isEventProcessed(UUID eventId) {
        return processedEventRepository.existsByEventId(eventId);
    }
    
    private void markEventAsProcessed(UUID eventId) {
        ProcessedEvent processed = new ProcessedEvent(
            eventId,
            LocalDateTime.now()
        );
        processedEventRepository.save(processed);
    }
}
```

**Saga Timeout Handling**:

```java
@Component
@Scheduled(fixedDelay = 60000) // Every minute
public class SagaTimeoutMonitor {
    
    private final SagaInstanceRepository repository;
    private final SagaCompensationService compensationService;
    
    public void checkTimeouts() {
        LocalDateTime timeout = LocalDateTime.now().minusMinutes(30);
        
        List<SagaInstance> timedOutSagas = repository
            .findByStatusAndStartedAtBefore(
                SagaStatus.IN_PROGRESS,
                timeout
            );
        
        for (SagaInstance saga : timedOutSagas) {
            logger.warn("Saga {} timed out after 30 minutes", 
                saga.getSagaId());
            
            // Trigger compensation
            compensationService.compensate(saga);
        }
    }
}
```

**優點**：

- ✅ Decentralized, no single point of failure
- ✅ Services remain loosely coupled
- ✅ Natural fit 用於 event-driven architecture
- ✅ 容易add new participants
- ✅ Scales well
- ✅ 簡單understand

**缺點**：

- ⚠️ 難以track overall workflow
- ⚠️ 複雜的 to debug
- ⚠️ Cyclic dependencies possible
- ⚠️ Requires careful event design

**成本**： $60,000 implementation + $5,000/year operational

**風險**： **Low** - Fits existing architecture

### 選項 2： Orchestration-Based Saga

**描述**： Central orchestrator coordinates saga execution

**優點**：

- ✅ Centralized workflow logic
- ✅ 容易track progress
- ✅ 簡單的r debugging
- ✅ Clear compensation flow

**缺點**：

- ❌ Single point of failure
- ❌ Tight coupling to orchestrator
- ❌ More 複雜的 infrastructure
- ❌ Orchestrator becomes bottleneck

**成本**： $90,000 implementation + $10,000/year

**風險**： **Medium** - Additional complexity

### 選項 3： Two-Phase Commit (2PC)

**描述**： Distributed transaction protocol

**優點**：

- ✅ Strong consistency
- ✅ ACID guarantees

**缺點**：

- ❌ Blocking protocol
- ❌ Poor performance
- ❌ Scalability issues
- ❌ Single point of failure (coordinator)
- ❌ Not suitable 用於 microservices

**成本**： $40,000 implementation

**風險**： **High** - Not recommended for microservices

## 決策結果

**選擇的選項**： **Choreography-Based Saga (Option 1)**

### 理由

Choreography-based saga fits naturally 與 our existing event-driven architecture, 維持s loose coupling between services, 和 提供s the flexibility 和 scalability needed 用於 our distributed system.

## 影響分析

### 利害關係人影響

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | High | New pattern to learn | Training, examples, documentation |
| QA Team | Medium | New testing approaches | Test frameworks, saga testing tools |
| Operations Team | Medium | New monitoring needs | Dashboards, alerts, runbooks |
| Business Team | Low | 更好的 process visibility | Monitoring dashboards |

### Impact Radius Assessment

**選擇的影響半徑**： **System**

影響：

- All bounded contexts
- Event-driven architecture
- Business workflows
- Monitoring systems
- Testing approach

### 風險評估

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| 複雜的 debugging | Medium | Medium | Saga tracking, correlation IDs |
| Event ordering issues | Low | High | Idempotency, event versioning |
| Compensation failures | Low | High | Retry mechanism, manual intervention |
| Saga timeouts | Medium | Medium | Timeout monitoring, alerts |
| Cyclic dependencies | Low | Medium | Careful event design, reviews |

**整體風險等級**： **Low**

## 實作計畫

### 第 1 階段： Framework Setup （第 1-2 週）

**Tasks**:

- [ ] Create saga tracking infrastructure
- [ ] Implement idempotency framework
- [ ] Set up monitoring dashboards
- [ ] Create saga testing utilities
- [ ] Document patterns

**Success Criteria**:

- Framework operational
- Monitoring working
- Documentation complete

### 第 2 階段： Pilot Saga （第 3-4 週）

**Tasks**:

- [ ] Implement Order Submission saga
- [ ] Add compensation logic
- [ ] Test failure scenarios
- [ ] Monitor in staging
- [ ] Gather feedback

**Success Criteria**:

- Saga working end-to-end
- Compensation tested
- Team comfortable

### 第 3 階段： Additional Sagas （第 5-6 週）

**Tasks**:

- [ ] Implement Order 可以cellation saga
- [ ] Implement Customer Registration saga
- [ ] Add monitoring 用於 all sagas
- [ ] Update documentation
- [ ] Train team

**Success Criteria**:

- All critical sagas implemented
- Monitoring comprehensive
- Team trained

### 第 4 階段： Production Rollout （第 7-8 週）

**Tasks**:

- [ ] Deploy to production
- [ ] Monitor saga execution
- [ ] 處理 edge cases
- [ ] Optimize performance
- [ ] Document lessons learned

**Success Criteria**:

- Production stable
- Sagas executing reliably
- Team confident

### 回滾策略

**觸發條件**：

- Unacceptable failure rate
- Performance issues
- Team 可以not 維持

**回滾步驟**：

1. Disable saga pattern
2. Use synchronous calls
3. Fix issues
4. Re-啟用 sagas

**回滾時間**： 1 day

## 監控和成功標準

### 成功指標

| Metric | Target | Measurement |
|--------|--------|-------------|
| Saga Success Rate | > 99% | Saga tracking |
| Compensation Success Rate | > 99.9% | Saga tracking |
| Average Saga Duration | < 5s | Monitoring |
| Failed Saga Detection | < 1 minute | Alerts |
| Manual Intervention Rate | < 0.1% | Operations metrics |

### Review Schedule

- **Weekly**: Saga metrics review
- **Monthly**: Pattern optimization
- **Quarterly**: Strategy review

## 後果

### 正面後果

- ✅ **Consistency**: Eventual consistency 跨 services
- ✅ **Reliability**: Workflows complete 或 compensate
- ✅ **Observability**: Track workflow progress
- ✅ **Scalability**: Non-blocking, asynchronous
- ✅ **Flexibility**: 容易add new workflows
- ✅ **Loose Coupling**: Services remain independent

### 負面後果

- ⚠️ **複雜的ity**: More 複雜的 than synchronous calls
- ⚠️ **Debugging**: 更難debug distributed workflows
- ⚠️ **Testing**: More 複雜的 testing scenarios
- ⚠️ **Eventual Consistency**: Not immediate consistency

### 技術債務

**已識別債務**：

1. Manual saga timeout handling
2. Limited saga visualization
3. Basic compensation testing
4. No saga replay capability

**債務償還計畫**：

- **Q2 2026**: Automated timeout handling
- **Q3 2026**: Saga visualization dashboard
- **Q4 2026**: Comprehensive compensation testing
- **Q1 2027**: Saga replay 用於 debugging

## 相關決策

- [ADR-020: Database Migration Strategy 與 Flyway](020-database-migration-strategy-flyway.md)
- [ADR-021: Event Sourcing 用於 Critical Aggregates](021-event-sourcing-critical-aggregates.md)
- [ADR-026: CQRS Pattern 用於 Read/Write Separation](026-cqrs-pattern-read-write-separation.md)

---

**文檔狀態**： ✅ Accepted  
**上次審查**： 2025-10-25  
**下次審查**： 2026-01-25 （每季）

## 備註

### Saga Design Best Practices

**DO**:

- ✅ Design idempotent operations
- ✅ Use correlation IDs
- ✅ Implement compensation logic
- ✅ Monitor saga execution
- ✅ 處理 timeouts
- ✅ Test failure scenarios

**DON'T**:

- ❌ Create cyclic dependencies
- ❌ Ignore idempotency
- ❌ Skip compensation logic
- ❌ Forget timeout handling
- ❌ Ignore monitoring

### Common Saga Patterns

**Order Processing**:

1. Create Order → Reserve Inventory → Process Payment → Confirm Order
2. Compensation: Release Inventory ← Refund Payment ← 可以cel Order

**Order 可以cellation**:

1. 可以cel Order → Refund Payment → Release Inventory → Notify Customer
2. Compensation: Restore Order ← Reverse Refund

**Customer Registration**:

1. Create Customer → Send Email → Create Loyalty Account → Send Welcome Gift
2. Compensation: Delete Customer ← Delete Loyalty Account
