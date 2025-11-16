---
title: "Synchronization Mechanisms"
viewpoint: "Concurrency"
status: "active"
last_updated: "2025-10-23"
stakeholders: ["Architects", "Developers", "Operations Team"]
---

# Synchronization Mechanisms

> **Viewpoint**: Concurrency
> **Purpose**: 記錄用於協調並行操作的同步技術
> **Audience**: Architects, Developers, Operations Team

## 概述

本文件描述用於協調對共享資源的並行存取、維護資料一致性,以及防止 E-Commerce Platform 分散式系統中 race conditions 的同步機制。

![Distributed Locking Sequence](../../diagrams/generated/concurrency/distributed-locking.png)

*圖 1: Distributed locking sequence diagram showing lock acquisition, contention, timeout, and optimistic locking scenarios*

## 同步階層

我們的同步策略在多個層級運作:

```text
┌─────────────────────────────────────────────────────────────┐
│                    Distributed Level                        │
│  ├── Kafka Event Ordering (Partition-based)                │
│  ├── Distributed Locks (Redis-based)                       │
│  └── Database Cluster Coordination                         │
├─────────────────────────────────────────────────────────────┤
│                    Application Level                        │
│  ├── Database Transactions (ACID)                          │
│  ├── Optimistic Locking (JPA @Version)                     │
│  ├── Pessimistic Locking (SELECT FOR UPDATE)               │
│  └── Application-level Semaphores                          │
├─────────────────────────────────────────────────────────────┤
│                    JVM Level                                │
│  ├── Synchronized Methods/Blocks                           │
│  ├── ReentrantLock and ReadWriteLock                       │
│  ├── Atomic Classes (AtomicLong, etc.)                     │
│  └── Concurrent Collections                                 │
└─────────────────────────────────────────────────────────────┘
```

## 資料庫 Transaction 管理

### 1. Transaction 邊界

我們使用 Spring 的 `@Transactional` annotation 來定義明確的 transaction 邊界:

```java
@Service
@Transactional
public class OrderApplicationService {

    // Read-only transactions for queries
    @Transactional(readOnly = true)
    public OrderSummary getOrderSummary(String orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        return OrderSummary.from(order);
    }

    // Write transaction with proper isolation
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OrderResult submitOrder(SubmitOrderCommand command) {
        Order order = orderRepository.findById(command.orderId())
            .orElseThrow(() -> new OrderNotFoundException(command.orderId()));

        // Business logic within transaction boundary
        order.submit();
        Order savedOrder = orderRepository.save(order);

        // Events are published after transaction commit
        eventPublisher.publishEvent(new OrderSubmittedEvent(savedOrder));

        return OrderResult.success(savedOrder);
    }

    // New transaction for independent operations
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processPaymentCallback(PaymentCallbackEvent event) {
        // This runs in a separate transaction
        Payment payment = paymentRepository.findById(event.paymentId());
        payment.updateStatus(event.status());
        paymentRepository.save(payment);
    }
}
```

### 2. Optimistic Locking

處理並行更新的主要機制:

```java
@Entity
@Table(name = "orders")
public class Order {
    @Id
    private String id;

    @Version
    private Long version;                   // Optimistic locking

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private LocalDateTime lastModified;

    public void submit() {
        if (this.status != OrderStatus.CREATED) {
            throw new IllegalStateException("Order cannot be submitted in status: " + this.status);
        }
        this.status = OrderStatus.PENDING;
        this.lastModified = LocalDateTime.now();
        // Version will be automatically incremented by JPA
    }
}
```

**處理 Optimistic Lock Exceptions**:

```java
@Service
public class OrderService {

    @Retryable(
        value = {OptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public OrderResult submitOrderWithRetry(SubmitOrderCommand command) {
        try {
            return submitOrder(command);
        } catch (OptimisticLockingFailureException e) {
            log.warn("Optimistic lock failure for order {}, retrying...", command.orderId());
            throw e; // Trigger retry
        }
    }

    @Recover
    public OrderResult recoverFromOptimisticLockFailure(
            OptimisticLockingFailureException ex,
            SubmitOrderCommand command) {
        log.error("Failed to submit order {} after retries due to concurrent modification",
            command.orderId());
        return OrderResult.failure("Order is being modified by another process. Please try again.");
    }
}
```

### 3. Distributed Locking

用於需要跨多個應用程式實例協調的操作:

```java
@Component
public class DistributedLockService {

    private final RedisTemplate<String, String> redisTemplate;

    public <T> T executeWithLock(String lockKey, Duration timeout, Supplier<T> operation) {
        String lockValue = UUID.randomUUID().toString();
        String fullLockKey = "lock:" + lockKey;

        try {
            // Attempt to acquire lock
            Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(fullLockKey, lockValue, timeout);

            if (!acquired) {
                throw new LockAcquisitionException("Failed to acquire lock: " + lockKey);
            }

            // Execute operation with lock held
            return operation.get();
        } finally {
            // Release lock atomically
            releaseLock(fullLockKey, lockValue);
        }
    }

    private void releaseLock(String lockKey, String lockValue) {
        // Lua script for atomic lock release
        String script =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "  return redis.call('del', KEYS[1]) " +
            "else " +
            "  return 0 " +
            "end";

        redisTemplate.execute(
            RedisScript.of(script, Boolean.class),
            Collections.singletonList(lockKey),
            lockValue
        );
    }
}
```

## 事件排序和一致性

### 1. Kafka Partition-based Ordering

相同 aggregate 的事件保證有序:

```java
@Component
public class OrderedEventPublisher {

    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;

    public void publishEvent(DomainEvent event) {
        // Use aggregate ID as partition key to ensure ordering
        String partitionKey = event.getAggregateId();

        ProducerRecord<String, DomainEvent> record = new ProducerRecord<>(
            "domain-events",
            partitionKey,  // Events for same aggregate go to same partition
            event
        );

        kafkaTemplate.send(record);
    }
}
```

### 2. Sequence Number Tracking

```java
@Component
public class OrderedEventProcessor {

    private final ConcurrentMap<String, Long> lastProcessedSequence = new ConcurrentHashMap<>();

    @KafkaListener(topics = "domain-events")
    public void processEvent(DomainEvent event) {
        String aggregateId = event.getAggregateId();
        long eventSequence = event.getSequenceNumber();

        synchronized (getLock(aggregateId)) {
            Long lastSequence = lastProcessedSequence.get(aggregateId);

            if (lastSequence == null || eventSequence == lastSequence + 1) {
                processEventInternal(event);
                lastProcessedSequence.put(aggregateId, eventSequence);
            } else {
                log.warn("Out-of-order event detected: {} for aggregate {}",
                    eventSequence, aggregateId);
            }
        }
    }

    private Object getLock(String aggregateId) {
        return aggregateId.intern();
    }
}
```

## Deadlock 預防

### 1. Lock Ordering

總是以一致的順序獲取 locks:

```java
@Service
public class TransferService {

    public void transferFunds(String fromAccountId, String toAccountId, Money amount) {
        // Always lock accounts in alphabetical order to prevent deadlock
        String firstLock = fromAccountId.compareTo(toAccountId) < 0 ? fromAccountId : toAccountId;
        String secondLock = fromAccountId.compareTo(toAccountId) < 0 ? toAccountId : fromAccountId;

        distributedLockService.executeWithLock(firstLock, Duration.ofSeconds(5), () -> {
            return distributedLockService.executeWithLock(secondLock, Duration.ofSeconds(5), () -> {
                // Both accounts locked in consistent order
                Account fromAccount = accountRepository.findById(fromAccountId).orElseThrow();
                Account toAccount = accountRepository.findById(toAccountId).orElseThrow();

                fromAccount.debit(amount);
                toAccount.credit(amount);

                accountRepository.save(fromAccount);
                accountRepository.save(toAccount);

                return null;
            });
        });
    }
}
```

### 2. Deadlock Detection

```java
@Component
public class DeadlockDetector {

    private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void detectDeadlocks() {
        long[] deadlockedThreads = threadBean.findDeadlockedThreads();

        if (deadlockedThreads != null && deadlockedThreads.length > 0) {
            ThreadInfo[] threadInfos = threadBean.getThreadInfo(deadlockedThreads, true, true);

            StringBuilder report = new StringBuilder("Deadlock detected:\n");
            for (ThreadInfo info : threadInfos) {
                report.append("Thread: ").append(info.getThreadName()).append("\n");
                report.append("  State: ").append(info.getThreadState()).append("\n");
                report.append("  Lock: ").append(info.getLockName()).append("\n");
            }

            log.error(report.toString());
            alertService.sendCriticalAlert("Deadlock Detected", report.toString());
        }
    }
}
```

## 最佳實踐

### 1. 最小化 Lock 範圍

```java
// ✅ GOOD: Lock only critical section
public void processOrder(Order order) {
    validateOrder(order);
    calculateTotal(order);

    synchronized(this) {
        updateInventory(order);     // Only this needs lock
    }

    sendNotification(order);
}
```

### 2. 使用適當的 Isolation Levels

```java
@Transactional(isolation = Isolation.READ_COMMITTED)  // Default
public void updateOrder(Order order) {
    // Standard update
}

@Transactional(isolation = Isolation.REPEATABLE_READ)  // For consistent reads
public OrderReport generateReport(String orderId) {
    // Multiple reads that must be consistent
}
```

## 監控和故障排除

### Lock Metrics

**要監控的關鍵指標**:

- Lock acquisition time
- Lock hold duration
- Lock contention rate
- Deadlock occurrences
- Lock timeout rate

**監控實作**:

```java
@Aspect
@Component
public class LockMonitoringAspect {

    private final MeterRegistry meterRegistry;

    @Around("@annotation(DistributedLock)")
    public Object monitorLock(ProceedingJoinPoint joinPoint) throws Throwable {
        Timer.Sample sample = Timer.start(meterRegistry);
        String lockName = getLockName(joinPoint);

        try {
            Object result = joinPoint.proceed();
            sample.stop(Timer.builder("distributed.lock.acquisition")
                .tag("lock", lockName)
                .tag("status", "success")
                .register(meterRegistry));
            return result;
        } catch (LockAcquisitionException e) {
            sample.stop(Timer.builder("distributed.lock.acquisition")
                .tag("lock", lockName)
                .tag("status", "failure")
                .register(meterRegistry));
            throw e;
        }
    }
}
```

### 常見問題故障排除

**問題 1: 高 Lock Contention**

- **症狀**: 回應時間慢,高 lock wait times
- **解決方案**: 減少 lock 範圍,使用 optimistic locking,分割資料

**問題 2: Deadlocks**

- **症狀**: Threads 無限期阻塞
- **解決方案**: 實作 lock ordering,使用 timeouts,檢視 lock acquisition patterns

**問題 3: Lock Timeouts**

- **症狀**: 頻繁的 lock acquisition 失敗
- **解決方案**: 增加 timeout,最佳化鎖定操作,水平擴展

## 效能考量

### Lock 效能比較

| 機制 | 開銷 | 可擴展性 | 使用案例 |
|-----------|----------|-------------|----------|
| Optimistic Locking | 低 | 高 | 低競爭場景 |
| Pessimistic Locking | 中 | 中 | 高競爭場景 |
| Distributed Locks | 高 | 低 | 跨實例協調 |
| Event Ordering | 低 | 高 | 非同步操作 |

### 最佳化策略

1. **最小化 Lock 範圍**: 只鎖定關鍵部分
2. **使用 Read-Write Locks**: 允許並行讀取
3. **實作 Lock Striping**: 依 key 分割 locks
4. **快取 Lock Status**: 減少 lock acquisition 嘗試
5. **優先使用 Optimistic Locking**: 只在需要時回退到 pessimistic

## 相關文件

- [Concurrency Viewpoint Overview](overview.md) - Overall concurrency model ←
- [Synchronous vs Asynchronous Operations](sync-async-operations.md) - Operation classification ←
- [State Management](state-management.md) - State handling strategies →
- [Performance Perspective](../../perspectives/performance/overview.md) - Performance requirements

---

**Document Status**: Active
**Last Review**: 2025-10-23
**Next Review**: 2025-11-23
**Owner**: Architecture Team
