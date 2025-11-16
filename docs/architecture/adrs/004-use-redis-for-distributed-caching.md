---
adr_number: 004
title: "Use Redis 用於 Distributed Caching"
date: 2025-10-24
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [001, 002]
affected_viewpoints: ["deployment", "concurrency"]
affected_perspectives: ["performance", "availability", "scalability"]
---

# ADR-004: Use Redis 用於 Distributed Caching

## 狀態

**Accepted** - 2025-10-24

## 上下文

### 問題陳述

企業電子商務平台需要caching solution來：

- 降低 資料庫負載 和 改善 回應時間
- 支援 分散式快取 跨 multiple 應用程式實例
- 處理 高讀取量 (5000+ reads/second)
- 提供 sub-millisecond 回應時間 用於 cached data
- 支援 快取失效 和 TTL management
- 啟用 會話管理 用於 無狀態應用程式
- 支援 速率限制 和 分散式鎖定

### 業務上下文

**業務驅動因素**：

- 需要 sub-second API 回應時間 (< 500ms target)
- 預期的 流量尖峰 期間 promotions (10x normal load)
- Cost optimization 透過 reducing database queries
- 改善d 用戶體驗 與 faster 頁面載入
- 支援 用於 10K → 1M 用戶增長

**限制條件**：

- 預算: $500/month 用於 快取基礎設施
- 必須 integrate 與 AWS infrastructure
- 需要 高可用性 (99.9% uptime)
- Data consistency 需求各異 透過 使用案例

### 技術上下文

**目前狀態**：

- PostgreSQL 主要資料庫 (ADR-001)
- Hexagonal Architecture (ADR-002)
- Spring Boot 3.4.5 與 Spring Cache abstraction
- AWS 雲端基礎設施

**需求**：

- Distributed caching 跨 multiple instances
- Sub-millisecond 讀取延遲
- 支援 用於 複雜資料結構 (lists, sets, sorted sets)
- Pub/sub capabilities 用於 快取失效
- Persistence options 用於 關鍵資料
- Cluster mode 用於 高可用性

## 決策驅動因素

1. **Performance**: Sub-millisecond 回應時間
2. **Scalability**: 處理 5000+ reads/second
3. **Availability**: 99.9% uptime 與 自動容錯移轉
4. **成本**： Within $500/month budget
5. **Spring Integration**: 無縫的Spring Boot整合
6. **Data Structures**: 支援 用於 複雜的 data types
7. **Persistence**: Optional persistence 用於 關鍵資料
8. **Operations**: Managed service to 降低 營運開銷

## 考慮的選項

### 選項 1： Redis (AWS ElastiCache)

**描述**： In-memory data store with optional persistence, managed by AWS ElastiCache

**優點**：

- ✅ Sub-millisecond latency (< 1ms)
- ✅ 豐富的 data structures (strings, lists, sets, sorted sets, hashes)
- ✅ 優秀的Spring Boot整合 (Spring Data Redis)
- ✅ Pub/sub 用於 快取失效
- ✅ Cluster mode 用於 高可用性
- ✅ Optional persistence (RDB, AOF)
- ✅ AWS ElastiCache 託管服務
- ✅ 支援s 分散式鎖定
- ✅ 大型的 社群和生態系統

**缺點**：

- ⚠️ In-memory only (需要sufficient RAM)
- ⚠️ Persistence has 效能權衡
- ⚠️ Cluster mode 增加複雜性

**成本**：

- Development: $200/month (cache.t3.medium)
- Production: $500/month (cache.r6g.大型的 與 replica)

**風險**： **Low** - Industry-standard solution

### 選項 2： Memcached (AWS ElastiCache)

**描述**： Simple in-memory key-value store

**優點**：

- ✅ Very fast (sub-millisecond)
- ✅ 簡單use
- ✅ Multi-threaded architecture
- ✅ AWS ElastiCache 託管服務
- ✅ Lower memory overhead

**缺點**：

- ❌ Only 支援s 簡單的 key-value pairs
- ❌ No persistence
- ❌ No pub/sub
- ❌ No 複雜資料結構
- ❌ Limited Spring Boot integration
- ❌ No 分散式鎖定

**成本**： Similar to Redis

**風險**： **Low** - But limited features

### 選項 3： Hazelcast

**描述**： In-memory data grid with distributed caching

**優點**：

- ✅ Distributed caching built-in
- ✅ 豐富的 data structures
- ✅ Strong consistency options
- ✅ 良好的Spring Boot整合

**缺點**：

- ❌ Higher memory usage
- ❌ More 複雜的 setup
- ❌ No AWS 託管服務
- ❌ Smaller community than Redis
- ❌ Higher 營運開銷

**成本**： $800/month (self-managed on EC2)

**風險**： **Medium** - More operational complexity

### 選項 4： Application-Level Caching (Caffeine)

**描述**： In-process caching with Caffeine library

**優點**：

- ✅ Zero network latency
- ✅ 簡單implement
- ✅ No additional infrastructure
- ✅ 優秀的Spring Boot整合

**缺點**：

- ❌ Not distributed (each instance has own cache)
- ❌ Cache invalidation 跨 instances is 複雜的
- ❌ Limited 透過 JVM heap size
- ❌ No session sharing
- ❌ No 分散式鎖定

**成本**： $0 (included in application)

**風險**： **Medium** - Doesn't scale well

## 決策結果

**選擇的選項**： **Redis (AWS ElastiCache)**

### 理由

Redis被選擇的原因如下：

1. **Performance**: Sub-millisecond latency meets our < 500ms API response target
2. **豐富的 Features**: 複雜的 data structures 支援 various caching patterns
3. **Spring Integration**: 優秀的 Spring Data Redis 和 Spring Cache 支援
4. **Distributed**: Works 跨 multiple 應用程式實例
5. **Pub/Sub**: 啟用s 快取失效 跨 instances
6. **Managed Service**: AWS ElastiCache 降低s 營運開銷
7. **High Availability**: Cluster mode 與 自動容錯移轉
8. **Cost-Effective**: Meets requirements within 預算
9. **Proven**: Industry-standard solution 與 大型的 community

**快取策略**：

- **Read-Through**: Cache customer profiles, product catalog
- **Write-Through**: Update cache on data changes
- **Cache-Aside**: For 複雜的 queries 和 aggregations
- **TTL-Based**: Automatic expiration 用於 time-sensitive data
- **Pub/Sub**: Cache invalidation 跨 instances

**為何不選 Memcached**： Lacks 複雜資料結構, pub/sub, 和 persistence needed 用於 our 使用案例s.

**為何不選 Hazelcast**： Higher 營運開銷 和 cost 沒有 signifi可以t benefits over Redis.

**為何不選 Caffeine**： Doesn't 支援 分散式快取 needed 用於 multiple instances.

## 影響分析

### 利害關係人影響

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | Medium | Need to learn Redis patterns | Training, examples, documentation |
| Operations Team | Low | Managed service 降低s overhead | ElastiCache runbooks |
| End Users | Positive | Faster 回應時間 | N/A |
| Business | Positive | Cost savings, 更好的 UX | N/A |
| Database Team | Positive | 降低d 資料庫負載 | Monitor cache hit rates |

### 影響半徑

**選擇的影響半徑**： **System**

影響：

- All bounded contexts (caching layer)
- Application services (cache annotations)
- Infrastructure layer (Redis configuration)
- Performance characteristics
- Monitoring 和 alerting

### 風險評估

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| Cache stampede | Medium | High | Use locking, staggered TTLs |
| Stale data | Medium | Medium | Proper 快取失效, short TTLs |
| Memory exhaustion | Low | High | Eviction policies, monitoring |
| Cache unavailability | Low | Medium | Fallback to database, circuit breaker |
| Cost overrun | Low | Low | Monitor usage, set alarms |

**整體風險等級**： **Low**

## 實作計畫

### 第 1 階段： Setup （第 1 週）

- [x] Provision ElastiCache Redis cluster (cache.r6g.大型的)
- [x] Configure security groups 和 VPC
- [x] Set up Redis cluster mode 與 replica
- [x] Configure CloudWatch monitoring
- [x] Set up backup schedule

### 第 2 階段： Integration （第 2-3 週）

- [x] Add Spring Data Redis dependencies
- [x] Configure Redis connection (Lettuce client)
- [x] Implement cache configuration
- [x] Add cache annotations to services
- [x] Implement cache key strategies

### 第 3 階段： Caching Patterns （第 4-5 週）

- [x] Implement read-through caching 用於 customer profiles
- [x] Implement cache-aside 用於 product catalog
- [x] Add 快取失效 on updates
- [x] Implement 分散式鎖定 用於 critical sections
- [x] Add 會話管理 與 Redis

### 第 4 階段： Optimization （第 6 週）

- [x] Tune cache TTLs based on usage patterns
- [x] Implement cache warming 用於 關鍵資料
- [x] Add cache metrics 和 monitoring
- [x] Load testing 和 performance tuning
- [x] Document caching patterns

### 回滾策略

**觸發條件**：

- Cache hit rate < 50%
- Cache-related errors > 1%
- Cost exceeds 預算 透過 > 50%
- Performance degradation 與 cache

**回滾步驟**：

1. Disable caching annotations
2. Route all requests to database
3. Investigate root cause
4. Fix issues 和 re-啟用 gradually

**回滾時間**： < 1 hour

## 監控和成功標準

### 成功指標

- ✅ Cache hit rate > 80%
- ✅ Cache 回應時間 < 1ms (95th percentile)
- ✅ Database query reduction > 60%
- ✅ API 回應時間 改善ment > 40%
- ✅ Cache availability > 99.9%
- ✅ Cost within 預算 ($500/month)

### 監控計畫

**CloudWatch Metrics**:

- CacheHits / CacheMisses
- CPUUtilization
- NetworkBytesIn / NetworkBytesOut
- CurrConnections
- Evictions
- ReplicationLag

**Application Metrics**:

```java
@Component
public class CacheMetrics {
    private final Counter cacheHits;
    private final Counter cacheMisses;
    private final Timer cacheOperationTime;
    
    // Track cache performance
}
```

**告警**：

- Cache hit rate < 70%
- CPU utilization > 80%
- Evictions > 100/minute
- Replication lag > 5 seconds
- Connection count > 80% of max

**審查時程**：

- Daily: Check cache hit rates
- Weekly: Review cache patterns 和 TTLs
- Monthly: Cost optimization review
- Quarterly: Caching strategy review

## 後果

### 正面後果

- ✅ **Performance**: 40-60% reduction in API 回應時間
- ✅ **Scalability**: 處理s 10x traffic 與 same database capacity
- ✅ **Cost Savings**: 降低d database instance size needs
- ✅ **User Experience**: Faster 頁面載入 和 API responses
- ✅ **Database Health**: 降低d load on PostgreSQL
- ✅ **Flexibility**: 豐富的 data structures 支援 various patterns
- ✅ **Reliability**: High availability 與 自動容錯移轉

### 負面後果

- ⚠️ **複雜的ity**: Additional layer to manage
- ⚠️ **Consistency**: Potential 用於 stale data
- ⚠️ **成本**： Additional infrastructure cost ($500/month)
- ⚠️ **Debugging**: 更難trace cache-related issues
- ⚠️ **Memory Management**: Need to monitor 和 tune

### 技術債務

**已識別債務**：

1. Manual 快取失效 (可以 be 改善d 與 CDC)
2. No cache warming on deployment (future enhancement)
3. 簡單的 TTL-based expiration (可以 add smarter policies)

**債務償還計畫**：

- **Q2 2026**: Implement CDC-based 快取失效
- **Q3 2026**: Add cache warming on deployment
- **Q4 2026**: Implement adaptive TTL based on access patterns

## 相關決策

- [ADR-001: Use PostgreSQL 用於 Primary Database](001-use-postgresql-for-primary-database.md) - Cache 降低s 資料庫負載
- [ADR-002: Adopt Hexagonal Architecture](002-adopt-hexagonal-architecture.md) - Cache in infrastructure layer
- [ADR-005: Use Apache Kafka 用於 Event Streaming](005-use-kafka-for-event-streaming.md) - Event-based 快取失效

## 備註

### Cache Configuration

```java
@Configuration
@EnableCaching
public class CacheConfiguration {
    
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .withCacheConfiguration("customers", 
                config.entryTtl(Duration.ofHours(1)))
            .withCacheConfiguration("products", 
                config.entryTtl(Duration.ofMinutes(15)))
            .withCacheConfiguration("orders", 
                config.entryTtl(Duration.ofMinutes(5)))
            .build();
    }
}
```

### Caching Patterns

**Read-Through Caching**:

```java
@Service
@CacheConfig(cacheNames = "customers")
public class CustomerService {
    
    @Cacheable(key = "#customerId")
    public Customer findById(String customerId) {
        return customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }
    
    @CacheEvict(key = "#customer.id")
    public Customer updateCustomer(Customer customer) {
        return customerRepository.save(customer);
    }
}
```

**Distributed Locking**:

```java
@Service
public class OrderService {
    
    @Autowired
    private RedissonClient redissonClient;
    
    public void processOrder(String orderId) {
        RLock lock = redissonClient.getLock("order:" + orderId);
        
        try {
            if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
                // Process order with exclusive lock
            }
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

### Cache Key Strategy

```text
Pattern: {context}:{entity}:{id}:{version}

Examples:

- customer:profile:CUST-123:v1
- product:details:PROD-456:v2
- order:summary:ORD-789:v1

```

### ElastiCache Configuration

```yaml
# Production Configuration
Node Type: cache.r6g.large
Engine: Redis 7.0
Cluster Mode: Enabled
Replicas: 1 per shard
Shards: 2
Multi-AZ: Enabled
Backup: Daily, 7-day retention
Maintenance Window: Sun 03:00-04:00 UTC
```

---

**文檔狀態**： ✅ Accepted  
**上次審查**： 2025-10-24  
**下次審查**： 2026-01-24 （每季）
