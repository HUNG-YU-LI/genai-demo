---
adr_number: 022
title: "Distributed Locking 與 Redis"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [004, 005, 025]
affected_viewpoints: ["concurrency", "deployment"]
affected_perspectives: ["performance", "availability", "scalability"]
---

# ADR-022: Distributed Locking 與 Redis

## 狀態

**Accepted** - 2025-10-25

## 上下文

### 問題陳述

The Enterprise E-Commerce Platform 需要分散式鎖定 to:

- Prevent race conditions in distributed systems (multiple 應用程式實例)
- Ensure 資料一致性 用於 critical operations (inventory updates, order processing)
- Coordinate access to shared resources 跨 services
- 處理 concurrent requests safely 沒有 database-level locking overhead
- 支援 timeout 和 automatic lock release 用於 fault tolerance
- 提供 high performance 與 minimal latency impact

### 業務上下文

**業務驅動因素**：

- Prevent overselling inventory (critical business requirement)
- Ensure order processing integrity (no duplicate charges)
- 支援 horizontal scaling (multiple 應用程式實例)
- 維持 資料一致性 in distributed environment
- 處理 high concurrency 期間 promotions (1000+ concurrent users)

**Business Constraints**:

- 必須 prevent inventory overselling (zero tolerance)
- Order processing 必須 be atomic 和 consistent
- Lock acquisition 必須 be fast (< 10ms)
- System 必須 處理 lock holder failures gracefully

### 技術上下文

**目前狀態**：

- Redis 分散式快取 (ADR-004)
- Kafka 用於 event streaming (ADR-005)
- Saga pattern 用於 distributed transactions (ADR-025)
- Multiple 應用程式實例 (horizontal scaling)
- PostgreSQL 與 optimistic locking

**需求**：

- Distributed lock 跨 multiple instances
- Lock timeout 和 automatic release
- Deadlock prevention
- High availability (99.9% uptime)
- Low latency (< 10ms lock acquisition)
- Fair lock acquisition (FIFO when possible)

## 決策驅動因素

1. **Consistency**: Prevent race conditions 和 data corruption
2. **Performance**: Lock acquisition < 10ms, minimal overhead
3. **Availability**: 99.9% uptime 與 自動容錯移轉
4. **Scalability**: 支援 1000+ concurrent lock requests
5. **Fault Tolerance**: Automatic lock release on failure
6. **Integration**: 無縫的Spring Boot整合
7. **成本**： Leverage existing Redis infrastructure
8. **Operations**: 簡單monitor 和 troubleshoot

## 考慮的選項

### 選項 1： Redis Distributed Locks (Redisson)

**描述**： Use Redisson library for Redis-based distributed locks with advanced features

**優點**：

- ✅ Built on existing Redis infrastructure (ADR-004)
- ✅ 優秀的Spring Boot整合
- ✅ Automatic lock renewal (watchdog)
- ✅ Fair locks (FIFO) 支援
- ✅ Read-write locks 支援
- ✅ Semaphore 和 CountDownLatch 支援
- ✅ Proven in production (Netflix, Alibaba)
- ✅ Low latency (< 5ms)
- ✅ Comprehensive documentation

**缺點**：

- ⚠️ Requires Redis cluster 用於 高可用性
- ⚠️ Network partition 可以 cause issues
- ⚠️ Additional library dependency

**成本**：

- Development: $0 (uses existing Redis)
- Production: $0 (included in Redis cost)
- Learning: 2-3 天

**風險**： **Low** - Industry-standard solution

### 選項 2： Database-Level Locking (PostgreSQL)

**描述**： Use PostgreSQL advisory locks or SELECT FOR UPDATE

**優點**：

- ✅ Strong consistency guarantees
- ✅ No additional infrastructure
- ✅ ACID transaction 支援
- ✅ 簡單implement

**缺點**：

- ❌ Higher latency (10-50ms)
- ❌ Increases 資料庫負載
- ❌ Doesn't scale well 與 high concurrency
- ❌ Lock contention affects database performance
- ❌ Limited to single database instance
- ❌ Deadlock risk 與 複雜的 transactions

**成本**： $0 (existing database)

**風險**： **Medium** - Performance bottleneck

### 選項 3： ZooKeeper Distributed Locks

**描述**： Use Apache ZooKeeper for distributed coordination

**優點**：

- ✅ Strong consistency (CP system)
- ✅ Proven 用於 distributed coordination
- ✅ Automatic lock release on client failure
- ✅ Watch mechanism 用於 lock notifications

**缺點**：

- ❌ Additional infrastructure to manage
- ❌ Higher operational 複雜的ity
- ❌ Higher latency (20-50ms)
- ❌ Overkill 用於 our 使用案例
- ❌ Requires ZooKeeper cluster (3-5 nodes)
- ❌ Higher cost ($300-500/month)

**成本**： $400/month (managed ZooKeeper)

**風險**： **Medium** - Operational overhead

### 選項 4： Optimistic Locking Only

**描述**： Rely solely on database optimistic locking (version fields)

**優點**：

- ✅ 簡單implement
- ✅ No additional infrastructure
- ✅ Works well 用於 low contention

**缺點**：

- ❌ High retry rate under contention
- ❌ Poor 用戶體驗 (frequent failures)
- ❌ Doesn't prevent race conditions
- ❌ Inefficient 用於 high concurrency
- ❌ No coordination 跨 services

**成本**： $0

**風險**： **High** - Insufficient for high concurrency

## 決策結果

**選擇的選項**： **Redis Distributed Locks (Redisson)**

### 理由

Redisson被選擇的原因如下：

1. **Leverages Existing Infrastructure**: Uses Redis already deployed 用於 caching (ADR-004)
2. **Performance**: Sub-5ms lock acquisition meets our < 10ms requirement
3. **豐富的 Features**: Watchdog, fair locks, read-write locks, semaphores
4. **Proven**: Used 透過 Netflix, Alibaba, 和 other 大型的-scale systems
5. **Spring Integration**: 優秀的 Spring Boot 支援
6. **Cost-Effective**: No additional infrastructure cost
7. **Fault Tolerance**: Automatic lock release 和 renewal
8. **Scalability**: 處理s 1000+ concurrent requests

**Lock Strategy**:

- **Inventory Updates**: Exclusive locks 與 30-second timeout
- **Order Processing**: Fair locks (FIFO) 與 60-second timeout
- **Payment Processing**: Exclusive locks 與 30-second timeout
- **Cache Updates**: Read-write locks 用於 read-heavy operations
- **Rate Limiting**: Semaphores 用於 concurrent request limits

**為何不選 Database Locking**： Higher latency 和 資料庫負載, doesn't scale well.

**為何不選 ZooKeeper**： Overkill 用於 our 使用案例, higher cost 和 複雜的ity.

**為何不選 Optimistic Locking Only**： Insufficient 用於 high concurrency scenarios.

## 影響分析

### 利害關係人影響

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | Medium | Need to learn Redisson patterns | Training, code examples, documentation |
| Operations Team | Low | Monitor lock metrics | Add lock monitoring dashboards |
| End Users | Positive | Prevents overselling, 更好的 consistency | N/A |
| Business | Positive | Prevents revenue loss from overselling | N/A |
| Database Team | Positive | 降低d database lock contention | N/A |

### 影響半徑

**選擇的影響半徑**： **System**

影響：

- Inventory bounded context (critical)
- Order bounded context (critical)
- Payment bounded context (critical)
- Shopping cart bounded context (medium)
- Application services (lock annotations)
- Infrastructure layer (Redisson configuration)

### 風險評估

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| Lock not released | Low | High | Automatic timeout, watchdog |
| Redis unavailability | Low | High | Fallback to optimistic locking, circuit breaker |
| Deadlock | Low | Medium | Lock ordering, timeout |
| Lock contention | Medium | Medium | Fair locks, monitoring |
| Network partition | Low | High | Redis cluster, health checks |

**整體風險等級**： **Low**

## 實作計畫

### 第 1 階段： Setup （第 1 週）

- [x] Add Redisson dependency to project
- [x] Configure Redisson client 與 Redis cluster
- [x] Set up lock monitoring 和 metrics
- [x] Create lock utility classes
- [x] Document lock patterns

### 第 2 階段： Critical Operations （第 2-3 週）

- [x] Implement inventory reservation locks
- [x] Add order processing locks
- [x] Implement payment processing locks
- [x] Add lock timeout handling
- [x] Implement lock retry logic

### 第 3 階段： Advanced Features （第 4 週）

- [x] Implement fair locks 用於 order queue
- [x] Add read-write locks 用於 cache updates
- [x] Implement semaphores 用於 速率限制
- [x] Add lock metrics 和 monitoring
- [x] Performance testing under load

### 第 4 階段： Optimization （第 5 週）

- [x] Tune lock timeouts based on metrics
- [x] Optimize lock granularity
- [x] Add lock contention monitoring
- [x] Document best practices
- [x] Team training

### 回滾策略

**觸發條件**：

- Lock acquisition failure rate > 5%
- Lock timeout rate > 10%
- Redis unavailability > 1%
- Performance degradation > 20%

**回滾步驟**：

1. Disable distributed locks
2. Fall back to optimistic locking
3. 降低 concurrent request limits
4. Investigate 和 fix issues
5. Re-啟用 gradually

**回滾時間**： < 30 minutes

## 監控和成功標準

### 成功指標

- ✅ Lock acquisition time < 10ms (95th percentile)
- ✅ Lock acquisition success rate > 99%
- ✅ Zero inventory overselling incidents
- ✅ Lock timeout rate < 1%
- ✅ Lock contention < 5% of requests
- ✅ System availability > 99.9%

### 監控計畫

**Application Metrics**:

```java
@Component
public class LockMetrics {
    private final Timer lockAcquisitionTime;
    private final Counter lockAcquisitions;
    private final Counter lockFailures;
    private final Counter lockTimeouts;
    private final Gauge activeLocks;
    
    // Track lock performance
}
```

**CloudWatch Metrics**:

- Lock acquisition time (p50, p95, p99)
- Lock acquisition success rate
- Lock timeout rate
- Active locks count
- Lock contention rate
- Lock wait time

**告警**：

- Lock acquisition time > 50ms
- Lock failure rate > 5%
- Lock timeout rate > 10%
- Active locks > 1000
- Lock contention > 10%

**審查時程**：

- Daily: Check lock metrics
- Weekly: Review lock patterns 和 timeouts
- Monthly: Lock performance optimization
- Quarterly: Lock strategy review

## 後果

### 正面後果

- ✅ **Data Consistency**: Prevents race conditions 和 overselling
- ✅ **Performance**: Sub-10ms lock acquisition
- ✅ **Scalability**: 支援s horizontal scaling
- ✅ **Reliability**: Automatic lock release on failure
- ✅ **Cost-Effective**: Uses existing Redis infrastructure
- ✅ **Flexibility**: Multiple lock types (exclusive, fair, read-write)
- ✅ **Monitoring**: Comprehensive lock metrics

### 負面後果

- ⚠️ **複雜的ity**: Additional coordination layer
- ⚠️ **Dependency**: Relies on Redis availability
- ⚠️ **Network**: Network latency affects lock performance
- ⚠️ **Debugging**: 更難trace lock-related issues
- ⚠️ **Learning Curve**: Team needs to learn Redisson patterns

### 技術債務

**已識別債務**：

1. Manual lock timeout tuning (可以 be automated)
2. No automatic deadlock detection (future enhancement)
3. 簡單的 lock ordering (可以 add priority locks)

**債務償還計畫**：

- **Q2 2026**: Implement adaptive lock timeouts
- **Q3 2026**: Add deadlock detection 和 resolution
- **Q4 2026**: Implement priority-based lock queues

## 相關決策

- [ADR-004: Use Redis 用於 Distributed Caching](004-use-redis-for-distributed-caching.md) - Redisson uses same Redis infrastructure
- [ADR-005: Use Apache Kafka 用於 Event Streaming](005-use-kafka-for-event-streaming.md) - Events after lock release
- [ADR-025: Saga Pattern 用於 Distributed Transactions](025-saga-pattern-distributed-transactions.md) - Locks coordinate saga steps

## 備註

### Redisson Configuration

```java
@Configuration
public class RedissonConfiguration {
    
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useClusterServers()
            .addNodeAddress("redis://redis-cluster:6379")
            .setPassword("your-password")
            .setConnectTimeout(10000)
            .setTimeout(3000)
            .setRetryAttempts(3)
            .setRetryInterval(1500);
        
        return Redisson.create(config);
    }
}
```

### Lock Usage Patterns

**Exclusive Lock**:

```java
@Service
public class InventoryService {
    
    @Autowired
    private RedissonClient redissonClient;
    
    public void reserveInventory(String productId, int quantity) {
        RLock lock = redissonClient.getLock("inventory:" + productId);
        
        try {
            // Wait up to 10 seconds, auto-unlock after 30 seconds
            if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
                // Critical section: update inventory
                Inventory inventory = inventoryRepository.findById(productId);
                inventory.reserve(quantity);
                inventoryRepository.save(inventory);
            } else {
                throw new LockAcquisitionException("Failed to acquire lock");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockInterruptedException("Lock interrupted", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

**Fair Lock (FIFO)**:

```java
@Service
public class OrderService {
    
    public void processOrder(String orderId) {
        RLock fairLock = redissonClient.getFairLock("order:queue");
        
        try {
            if (fairLock.tryLock(30, 60, TimeUnit.SECONDS)) {
                // Process order in FIFO order
                processOrderInternal(orderId);
            }
        } finally {
            if (fairLock.isHeldByCurrentThread()) {
                fairLock.unlock();
            }
        }
    }
}
```

**Read-Write Lock**:

```java
@Service
public class ProductCatalogService {
    
    public Product getProduct(String productId) {
        RReadWriteLock rwLock = redissonClient.getReadWriteLock("product:" + productId);
        RLock readLock = rwLock.readLock();
        
        try {
            readLock.lock(10, TimeUnit.SECONDS);
            return productRepository.findById(productId);
        } finally {
            readLock.unlock();
        }
    }
    
    public void updateProduct(Product product) {
        RReadWriteLock rwLock = redissonClient.getReadWriteLock("product:" + product.getId());
        RLock writeLock = rwLock.writeLock();
        
        try {
            writeLock.lock(30, TimeUnit.SECONDS);
            productRepository.save(product);
            cacheService.invalidate("product:" + product.getId());
        } finally {
            writeLock.unlock();
        }
    }
}
```

**Semaphore (Rate Limiting)**:

```java
@Service
public class ApiRateLimiter {
    
    public boolean tryAcquire(String userId) {
        RSemaphore semaphore = redissonClient.getSemaphore("rate:limit:" + userId);
        semaphore.trySetPermits(100); // 100 requests per minute
        
        try {
            return semaphore.tryAcquire(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
```

### Lock Naming Convention

```text
Pattern: {context}:{operation}:{resource-id}

Examples:

- inventory:reserve:PROD-123
- order:process:ORD-456
- payment:charge:PAY-789
- cache:update:customer:CUST-001

```

### Lock Timeout Guidelines

| Operation | Timeout | Rationale |
|-----------|---------|-----------|
| Inventory Reserve | 30s | Database update + validation |
| Order Processing | 60s | Multiple service calls |
| Payment Processing | 30s | External API call |
| Cache Update | 10s | Fast 記憶體內 operation |
| Batch Processing | 300s | Long-running operation |

---

**文檔狀態**： ✅ Accepted  
**上次審查**： 2025-10-25  
**下次審查**： 2026-01-25 （每季）
