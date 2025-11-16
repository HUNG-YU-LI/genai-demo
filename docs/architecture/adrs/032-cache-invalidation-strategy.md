---
adr_number: 032
title: "Cache Invalidation Strategy"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [003, 004, 005]
affected_viewpoints: ["information", "concurrency"]
affected_perspectives: ["performance", "consistency"]
---

# ADR-032: Cache Invalidation Strategy

## 狀態

**Accepted** - 2025-10-25

## 上下文

### 問題陳述

The Enterprise E-Commerce Platform uses Redis 用於 分散式快取 (ADR-004) 和 需要快取失效 strategy：

- 維持 資料一致性 between cache 和 database
- Minimize stale data exposure to users
- Balance performance (cache hits) 與 freshness (data accuracy)
- 處理 快取失效 跨 multiple 應用程式實例
- 支援 different consistency requirements 用於 different data types
- Prevent cache stampede 期間 invalidation
- 啟用 efficient bulk invalidation 用於 related data

### 業務上下文

**業務驅動因素**：

- Product prices 必須 be accurate (no stale pricing)
- Inventory levels 必須 be reasonably current (< 1 minute stale acceptable)
- Customer profiles 可以 tolerate some staleness (< 5 minutes acceptable)
- Order status 必須 be accurate 用於 customer inquiries
- Promotional pricing 必須 be immediately effective

**Business Constraints**:

- Critical data (prices, inventory) 必須 be fresh
- Non-關鍵資料 (product descriptions) 可以 be stale
- 必須 支援 high cache hit rates (> 80%)
- Cache invalidation 必須 not impact performance
- 必須 work 跨 multiple 應用程式實例

### 技術上下文

**目前狀態**：

- Redis 分散式快取 (ADR-004)
- Domain events 用於 cross-context communication (ADR-003)
- Kafka 用於 event streaming (ADR-005)
- Multiple 應用程式實例 (horizontal scaling)
- PostgreSQL 主要資料庫

**需求**：

- Consistent 快取失效 跨 instances
- 支援 用於 different TTL strategies per data type
- Event-driven invalidation 用於 關鍵資料
- Bulk invalidation 用於 related data
- Cache stampede prevention
- Low latency (< 5ms invalidation overhead)
- High availability (99.9% uptime)

## 決策驅動因素

1. **Consistency**: Balance between performance 和 data freshness
2. **Performance**: Minimal impact on application performance
3. **Scalability**: Work 跨 multiple instances
4. **Flexibility**: Different strategies 用於 different data types
5. **Reliability**: Prevent cache stampede 和 thundering herd
6. **Simplicity**: 容易implement 和 維持
7. **成本**： Leverage existing infrastructure
8. **Observability**: Monitor cache effectiveness

## 考慮的選項

### 選項 1： TTL-Based Expiration Only

**描述**： Rely solely on Time-To-Live (TTL) for cache expiration

**優點**：

- ✅ 簡單implement
- ✅ No additional infrastructure
- ✅ Automatic cleanup
- ✅ Low overhead

**缺點**：

- ❌ Stale data until TTL expires
- ❌ No immediate invalidation
- ❌ Inefficient 用於 frequently updated data
- ❌ Cache stampede risk on expiration
- ❌ Doesn't meet consistency requirements

**成本**： $0

**風險**： **High** - Insufficient for requirements

### 選項 2： Write-Through Cache

**描述**： Update cache synchronously on every write

**優點**：

- ✅ Always consistent
- ✅ No stale data
- ✅ 簡單的 logic

**缺點**：

- ❌ Increased write latency
- ❌ Cache write failures affect writes
- ❌ Doesn't scale well
- ❌ Tight coupling 與 cache

**成本**： $0

**風險**： **Medium** - Performance impact

### 選項 3： Event-Driven Invalidation

**描述**： Use domain events to trigger cache invalidation

**優點**：

- ✅ Near real-time consistency
- ✅ Decoupled from business logic
- ✅ Works 跨 instances (pub/sub)
- ✅ Flexible (可以 invalidate related data)
- ✅ Leverages existing event infrastructure

**缺點**：

- ⚠️ Eventual consistency (small lag)
- ⚠️ Requires event infrastructure
- ⚠️ More 複雜的 implementation

**成本**： $0 (uses existing Kafka/Redis)

**風險**： **Low** - Proven pattern

### 選項 4： Hybrid Approach (TTL + Event-Driven)

**描述**： Combine TTL for baseline expiration with event-driven invalidation for critical updates

**優點**：

- ✅ Best of both worlds
- ✅ TTL as safety net
- ✅ Event-driven 用於 immediate updates
- ✅ Flexible per data type
- ✅ Cache stampede prevention
- ✅ High cache hit rates

**缺點**：

- ⚠️ More 複雜的 implementation
- ⚠️ Requires careful configuration

**成本**： $0 (uses existing infrastructure)

**風險**： **Low** - Industry best practice

## 決策結果

**選擇的選項**： **Hybrid Approach (TTL + Event-Driven Invalidation)**

### 理由

The hybrid approach被選擇的原因如下：

1. **Flexibility**: Different strategies 用於 different data types
2. **Consistency**: Event-driven invalidation 用於 關鍵資料
3. **Safety Net**: TTL prevents indefinite stale data
4. **Performance**: High cache hit rates 與 timely updates
5. **Scalability**: Works 跨 multiple instances
6. **Proven**: Industry best practice (Netflix, Amazon, Alibaba)
7. **Cost-Effective**: Uses existing infrastructure
8. **Reliability**: Cache stampede prevention built-in

**Cache Invalidation Strategy 透過 Data Type**:

**Critical Data (Immediate Invalidation)**:

- **Product Prices**: Event-driven + 5-minute TTL
- **Inventory Levels**: Event-driven + 1-minute TTL
- **Order Status**: Event-driven + 5-minute TTL
- **Payment Status**: Event-driven + 5-minute TTL

**Semi-Critical Data (Near Real-Time)**:

- **Customer Profiles**: Event-driven + 30-minute TTL
- **Shopping Cart**: Event-driven + 15-minute TTL
- **Promotions**: Event-driven + 10-minute TTL

**Non-Critical Data (Lazy Invalidation)**:

- **Product Descriptions**: 1-hour TTL only
- **Product Images**: 24-hour TTL only
- **Static Content**: 24-hour TTL only

**Invalidation Patterns**:

1. **Single Key**: Invalidate specific cache entry
2. **Pattern-Based**: Invalidate all keys matching pattern
3. **Tag-Based**: Invalidate all entries 與 specific tag
4. **Bulk**: Invalidate multiple related entries

**Cache Stampede Prevention**:

- **Probabilistic Early Expiration**: Refresh before TTL expires
- **Lock-Based Refresh**: Only one instance refreshes
- **Stale-While-Revalidate**: Serve stale while refreshing

**為何不選 TTL Only**： Doesn't meet consistency requirements 用於 關鍵資料.

**為何不選 Write-Through**： Performance impact, tight coupling.

**為何不選 Event-Driven Only**： No safety net 用於 missed events.

## 影響分析

### 利害關係人影響

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | Medium | Implement invalidation logic | Code examples, patterns, documentation |
| Operations Team | Low | Monitor cache metrics | Dashboards 和 alerts |
| End Users | Positive | More accurate data, 更好的 UX | N/A |
| Business | Positive | Accurate pricing, inventory | N/A |
| Database Team | Positive | 降低d 資料庫負載 | N/A |

### 影響半徑

**選擇的影響半徑**： **System**

影響：

- All bounded contexts (快取失效)
- Application services (invalidation logic)
- Infrastructure layer (Redis pub/sub)
- Event 處理rs (invalidation triggers)
- Monitoring 和 alerting

### 風險評估

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| Missed invalidation events | Low | Medium | TTL as safety net, monitoring |
| Cache stampede | Medium | High | Probabilistic expiration, locking |
| Over-invalidation | Medium | Low | Careful pattern design, monitoring |
| Event lag | Low | Low | Monitor event processing lag |
| Redis pub/sub failure | Low | Medium | Fallback to TTL, circuit breaker |

**整體風險等級**： **Low**

## 實作計畫

### 第 1 階段： Core Implementation （第 1-2 週）

- [x] Implement 快取失效 service
- [x] Add Redis pub/sub 用於 invalidation
- [x] Create invalidation event 處理rs
- [x] Implement TTL configuration per data type
- [x] Add cache key tagging

### 第 2 階段： Event-Driven Invalidation （第 3-4 週）

- [x] Implement product price invalidation
- [x] Add inventory level invalidation
- [x] Implement order status invalidation
- [x] Add customer profile invalidation
- [x] Implement shopping cart invalidation

### 第 3 階段： Advanced Features （第 5 週）

- [x] Implement pattern-based invalidation
- [x] Add tag-based invalidation
- [x] Implement bulk invalidation
- [x] Add cache stampede prevention
- [x] Implement stale-while-revalidate

### 第 4 階段： Monitoring & Optimization （第 6 週）

- [x] Add 快取失效 metrics
- [x] Create invalidation dashboards
- [x] Implement invalidation alerts
- [x] Performance testing
- [x] Documentation 和 training

### 回滾策略

**觸發條件**：

- Cache hit rate drops > 20%
- Invalidation errors > 5%
- Performance degradation > 10%
- Data consistency issues

**回滾步驟**：

1. Disable event-driven invalidation
2. Rely on TTL only
3. Investigate 和 fix issues
4. Re-啟用 gradually
5. Monitor metrics

**回滾時間**： < 30 minutes

## 監控和成功標準

### 成功指標

- ✅ Cache hit rate > 80%
- ✅ Cache invalidation latency < 5ms
- ✅ Stale data exposure < 1% of requests
- ✅ Cache stampede incidents = 0
- ✅ Invalidation success rate > 99.9%
- ✅ Event processing lag < 100ms

### 監控計畫

**Application Metrics**:

```java
@Component
public class CacheInvalidationMetrics {
    private final Counter invalidations;
    private final Counter invalidationErrors;
    private final Timer invalidationTime;
    private final Gauge cacheHitRate;
    private final Counter cacheStampede;
    
    // Track invalidation performance
}
```

**CloudWatch Metrics**:

- Cache invalidations per second
- Invalidation latency (p50, p95, p99)
- Cache hit rate 透過 data type
- Stale data rate
- Cache stampede incidents
- Event processing lag

**告警**：

- Cache hit rate < 70%
- Invalidation latency > 10ms
- Invalidation error rate > 1%
- Cache stampede detected
- Event lag > 1 second

**審查時程**：

- Daily: Check cache metrics
- Weekly: Review invalidation patterns
- Monthly: Optimize TTL 和 strategies
- Quarterly: Cache strategy review

## 後果

### 正面後果

- ✅ **Consistency**: Near real-time data freshness
- ✅ **Performance**: High cache hit rates (> 80%)
- ✅ **Flexibility**: Different strategies per data type
- ✅ **Reliability**: Cache stampede prevention
- ✅ **Scalability**: Works 跨 multiple instances
- ✅ **Safety**: TTL as fallback 用於 missed events
- ✅ **Observability**: Comprehensive metrics

### 負面後果

- ⚠️ **複雜的ity**: More 複雜的 than 簡單的 TTL
- ⚠️ **Eventual Consistency**: Small lag (< 100ms)
- ⚠️ **Monitoring**: Need to track invalidation metrics
- ⚠️ **Configuration**: Requires careful TTL tuning
- ⚠️ **Debugging**: 更難trace invalidation issues

### 技術債務

**已識別債務**：

1. Manual TTL configuration (可以 be adaptive)
2. 簡單的 pattern matching (可以 use advanced patterns)
3. No automatic invalidation optimization (future enhancement)

**債務償還計畫**：

- **Q2 2026**: Implement adaptive TTL based on access patterns
- **Q3 2026**: Add ML-based invalidation optimization
- **Q4 2026**: Implement automatic pattern optimization

## 相關決策

- [ADR-003: Use Domain Events 用於 Cross-Context Communication](003-use-domain-events-for-cross-context-communication.md) - Events trigger invalidation
- [ADR-004: Use Redis 用於 Distributed Caching](004-use-redis-for-distributed-caching.md) - Cache infrastructure
- [ADR-005: Use Apache Kafka 用於 Event Streaming](005-use-kafka-for-event-streaming.md) - Event infrastructure

## 備註

### Cache Invalidation Service

```java
@Service
public class CacheInvalidationService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private RedissonClient redissonClient;
    
    // Single key invalidation
    public void invalidate(String key) {
        redisTemplate.delete(key);
        publishInvalidation(key);
    }
    
    // Pattern-based invalidation
    public void invalidatePattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            publishInvalidation(pattern);
        }
    }
    
    // Tag-based invalidation
    public void invalidateByTag(String tag) {
        Set<String> keys = getKeysByTag(tag);
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
            publishInvalidation("tag:" + tag);
        }
    }
    
    // Bulk invalidation
    public void invalidateBulk(Collection<String> keys) {
        redisTemplate.delete(keys);
        keys.forEach(this::publishInvalidation);
    }
    
    // Publish invalidation to other instances
    private void publishInvalidation(String key) {
        RTopic topic = redissonClient.getTopic("cache:invalidation");
        topic.publish(new InvalidationMessage(key, Instant.now()));
    }
    
    // Subscribe to invalidation messages
    @PostConstruct
    public void subscribeToInvalidations() {
        RTopic topic = redissonClient.getTopic("cache:invalidation");
        topic.addListener(InvalidationMessage.class, (channel, msg) -> {
            // Invalidate local cache if needed
            localCache.invalidate(msg.getKey());
        });
    }
}
```

### Event-Driven Invalidation

```java
@Component
public class ProductEventHandler {
    
    @Autowired
    private CacheInvalidationService cacheInvalidationService;
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductPriceUpdated(ProductPriceUpdatedEvent event) {
        // Invalidate product cache
        cacheInvalidationService.invalidate("product:" + event.getProductId());
        
        // Invalidate related caches
        cacheInvalidationService.invalidatePattern("product:list:*");
        cacheInvalidationService.invalidateByTag("category:" + event.getCategoryId());
    }
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleInventoryUpdated(InventoryUpdatedEvent event) {
        // Invalidate inventory cache
        cacheInvalidationService.invalidate("inventory:" + event.getProductId());
        
        // Invalidate product cache (includes stock status)
        cacheInvalidationService.invalidate("product:" + event.getProductId());
    }
}
```

### Cache Stampede Prevention

```java
@Service
public class CacheService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private RedissonClient redissonClient;
    
    // Probabilistic early expiration
    public <T> T getWithProbabilisticRefresh(String key, 
                                             long ttl, 
                                             Supplier<T> loader) {
        T value = (T) redisTemplate.opsForValue().get(key);
        
        if (value != null) {
            // Calculate probability of early refresh
            long timeLeft = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            double refreshProbability = 1.0 - (double) timeLeft / ttl;
            
            // Probabilistically refresh before expiration
            if (Math.random() < refreshProbability) {
                refreshAsync(key, ttl, loader);
            }
            
            return value;
        }
        
        // Cache miss - load with lock
        return loadWithLock(key, ttl, loader);
    }
    
    // Lock-based refresh (prevent stampede)
    private <T> T loadWithLock(String key, long ttl, Supplier<T> loader) {
        RLock lock = redissonClient.getLock("lock:" + key);
        
        try {
            if (lock.tryLock(5, 30, TimeUnit.SECONDS)) {
                // Double-check cache
                T value = (T) redisTemplate.opsForValue().get(key);
                if (value != null) {
                    return value;
                }
                
                // Load from source
                value = loader.get();
                
                // Cache with TTL
                redisTemplate.opsForValue().set(key, value, ttl, TimeUnit.SECONDS);
                
                return value;
            } else {
                // Failed to acquire lock, wait and retry
                Thread.sleep(100);
                return (T) redisTemplate.opsForValue().get(key);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CacheException("Lock interrupted", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
    
    // Stale-while-revalidate
    public <T> T getWithStaleWhileRevalidate(String key, 
                                             long ttl, 
                                             Supplier<T> loader) {
        T value = (T) redisTemplate.opsForValue().get(key);
        
        if (value != null) {
            long timeLeft = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            
            // If close to expiration, refresh async
            if (timeLeft < ttl * 0.1) {
                refreshAsync(key, ttl, loader);
            }
            
            return value;
        }
        
        // Cache miss - load synchronously
        return loadWithLock(key, ttl, loader);
    }
    
    private <T> void refreshAsync(String key, long ttl, Supplier<T> loader) {
        CompletableFuture.runAsync(() -> {
            T value = loader.get();
            redisTemplate.opsForValue().set(key, value, ttl, TimeUnit.SECONDS);
        });
    }
}
```

### Cache Configuration

```yaml
# application.yml
cache:
  ttl:
    # Critical data (event-driven + short TTL)
    product-price: 300  # 5 minutes
    inventory: 60  # 1 minute
    order-status: 300  # 5 minutes
    payment-status: 300  # 5 minutes
    
    # Semi-critical data (event-driven + medium TTL)
    customer-profile: 1800  # 30 minutes
    shopping-cart: 900  # 15 minutes
    promotions: 600  # 10 minutes
    
    # Non-critical data (TTL only)
    product-description: 3600  # 1 hour
    product-images: 86400  # 24 hours
    static-content: 86400  # 24 hours
  
  stampede-prevention:
    enabled: true
    probabilistic-refresh: true
    stale-while-revalidate: true
```

### Cache Key Tagging

```java
@Service
public class CacheTagService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    // Add tag to cache key
    public void tagKey(String key, String... tags) {
        for (String tag : tags) {
            redisTemplate.opsForSet().add("tag:" + tag, key);
        }
    }
    
    // Get keys by tag
    public Set<String> getKeysByTag(String tag) {
        return redisTemplate.opsForSet().members("tag:" + tag);
    }
    
    // Remove tag
    public void removeTag(String key, String tag) {
        redisTemplate.opsForSet().remove("tag:" + tag, key);
    }
}

// Usage
cacheService.set("product:123", product, 3600);
cacheTagService.tagKey("product:123", "category:electronics", "brand:apple");

// Invalidate all products in category
cacheInvalidationService.invalidateByTag("category:electronics");
```

---

**文檔狀態**： ✅ Accepted  
**上次審查**： 2025-10-25  
**下次審查**： 2026-01-25 （每季）
