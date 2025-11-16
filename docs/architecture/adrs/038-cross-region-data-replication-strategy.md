---
adr_number: 038
title: "Cross-Region Data Replication Strategy"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [001, 004, 005, 037, 039, 040]
affected_viewpoints: ["information", "deployment"]
affected_perspectives: ["availability", "performance", "evolution"]
---

# ADR-038: Cross-Region Data Replication Strategy

## 狀態

**Accepted** - 2025-10-25

## 上下文

### 問題陳述

Active-active multi-region architecture (ADR-037) 需要data synchronization between Taiwan 和 Tokyo regions. Different bounded contexts have different consistency requirements:

**Challenges**:

- **Consistency vs Availability**: CAP theorem trade-offs
- **Replication Lag**: Network latency between regions (40-60ms)
- **Data Conflicts**: Concurrent updates in different regions
- **成本**： Cross-region data transfer ($0.09/GB)
- **複雜的ity**: Multiple 資料儲存s (PostgreSQL, Redis, Kafka, S3)
- **Performance**: Replication impact on application performance

**Business Impact**:

- Order processing errors due to inconsistency
- Inventory overselling
- Customer data conflicts
- Payment processing failures
- Poor 用戶體驗

### 業務上下文

**業務驅動因素**：

- Data consistency 用於 critical operations (orders, payments)
- High availability 用於 all operations
- Low latency 用於 read operations
- Cost-effective replication
- Regulatory compliance (data residency)

**限制條件**：

- Network latency: 40-60ms between Taiwan-Tokyo
- 預算: $50,000/year 用於 data transfer
- Replication lag tolerance varies 透過 context
- 必須 支援 bidirectional replication
- Zero data loss 用於 關鍵資料

### 技術上下文

**目前狀態**：

- Single-region deployment
- No cross-region replication
- No conflict resolution mechanisms

**需求**：

- Bidirectional replication 用於 all 資料儲存s
- Bounded context-specific consistency levels
- Conflict detection 和 resolution
- Replication lag monitoring
- Cost optimization

## 決策驅動因素

1. **Consistency**: Critical data 必須 be consistent
2. **Availability**: System 必須 remain available 期間 failures
3. **Performance**: Minimal impact on application performance
4. **成本**： Optimize data transfer costs
5. **複雜的ity**: Manageable operational 複雜的ity
6. **Scalability**: 支援 growing data volumes
7. **Compliance**: Meet data residency requirements

## 考慮的選項

### 選項 1： Tiered Replication Strategy (Recommended)

**描述**： Different replication strategies based on bounded context requirements

**Tier 1 - Strong Consistency (CP)**:

- **Contexts**: Orders, Payments, Inventory
- **Strategy**: Quorum write (both regions acknowledge)
- **Technology**: PostgreSQL synchronous replication
- **Lag**: < 100ms
- **成本**： Higher latency, lower throughput

**Tier 2 - Eventual Consistency (AP)**:

- **Contexts**: Product Catalog, Customer Profiles, Reviews
- **Strategy**: Asynchronous replication
- **Technology**: PostgreSQL logical replication
- **Lag**: 1-5 seconds acceptable
- **成本**： Low latency, high throughput

**Tier 3 - Regional Isolation**:

- **Contexts**: Shopping Carts, Sessions
- **Strategy**: Regional data, merge on demand
- **Technology**: Redis 與 regional clusters
- **Lag**: N/A (no replication)
- **成本**： Lowest

**優點**：

- ✅ Optimal balance of consistency 和 availability
- ✅ Cost-effective (only replicate what's needed)
- ✅ Performance optimized per context
- ✅ Clear consistency guarantees

**缺點**：

- ⚠️ 複雜的ity in managing different strategies
- ⚠️ Requires careful context classification

**成本**： $40,000/year

**風險**： **Low**

### 選項 2： Full Synchronous Replication

**描述**： All data synchronously replicated

**優點**：

- ✅ Strong consistency everywhere
- ✅ 簡單understand

**缺點**：

- ❌ High latency (100ms+ 用於 all writes)
- ❌ Lower throughput
- ❌ Availability impact
- ❌ High cost ($80,000/year)

**風險**： **High** - Performance impact

### 選項 3： Full Asynchronous Replication

**描述**： All data asynchronously replicated

**優點**：

- ✅ Low latency
- ✅ High throughput
- ✅ Low cost ($30,000/year)

**缺點**：

- ❌ Consistency issues 用於 關鍵資料
- ❌ Risk of data loss
- ❌ 複雜的 conflict resolution

**風險**： **High** - Data consistency issues

## 決策結果

**選擇的選項**： **Tiered Replication Strategy (Option 1)**

### 理由

Tiered replication 提供s optimal balance:

1. **Strong Consistency**: Where needed (orders, payments)
2. **High Availability**: Where acceptable (product catalog)
3. **Performance**: Optimized per context
4. **成本**： Only pay for what's needed
5. **Flexibility**: 容易adjust per context

### Bounded Context Classification

**Tier 1 - Strong Consistency (CP)**:

| Context | Replication | Lag Target | Rationale |
|---------|-------------|------------|-----------|
| Orders | Quorum Write | < 100ms | Financial accuracy critical |
| Payments | Synchronous | < 100ms | Payment integrity required |
| Inventory | Distributed Lock | < 100ms | Prevent overselling |

**Tier 2 - Eventual Consistency (AP)**:

| Context | Replication | Lag Target | Rationale |
|---------|-------------|------------|-----------|
| Product Catalog | Async | < 5s | Stale data acceptable |
| Customer Profiles | Async | < 5s | Profile updates infrequent |
| Reviews | Async | < 10s | Review display 可以 lag |
| Promotions | Async | < 5s | Promotion changes infrequent |
| Sellers | Async | < 5s | Seller data changes rare |

**Tier 3 - Regional Isolation**:

| Context | Strategy | Rationale |
|---------|----------|-----------|
| Shopping Carts | Regional | Merge on checkout |
| Sessions | Regional | No cross-region sessions |
| Notifications | Regional | Region-specific notifications |

### PostgreSQL Replication Configuration

**Tier 1 - Synchronous Replication**:

```sql
-- postgresql.conf
synchronous_commit = remote_apply
synchronous_standby_names = 'tokyo_standby'

-- On Taiwan primary
ALTER SYSTEM SET synchronous_standby_names = 'tokyo_standby';

-- On Tokyo primary  
ALTER SYSTEM SET synchronous_standby_names = 'taiwan_standby';
```

**Tier 2 - Logical Replication**:

```sql
-- Taiwan publisher
CREATE PUBLICATION taiwan_catalog FOR TABLE 
  products, categories, product_images, customer_profiles;

-- Tokyo subscriber
CREATE SUBSCRIPTION tokyo_catalog
  CONNECTION 'host=taiwan-db port=5432 dbname=ecommerce'
  PUBLICATION taiwan_catalog
  WITH (
    copy_data = true,
    create_slot = true,
    streaming = true
  );
```

### Redis Replication Strategy

**Global Datastore 用於 Critical Data**:

```typescript
const globalRedis = new elasticache.CfnGlobalReplicationGroup(this, 'GlobalRedis', {
  globalReplicationGroupIdSuffix: 'critical',
  primaryReplicationGroupId: taiwanRedis.ref,
  members: [{
    replicationGroupId: tokyoRedis.ref,
    replicationGroupRegion: 'ap-northeast-1',
    role: 'SECONDARY',
  }],
});
```

**Regional Clusters 用於 Sessions**:

```typescript
// Separate clusters, no replication
const taiwanSessionRedis = new elasticache.CfnReplicationGroup(this, 'TaiwanSessions', {
  replicationGroupDescription: 'Taiwan session storage',
  cacheNodeType: 'cache.r6g.large',
  numCacheClusters: 2,
});

const tokyoSessionRedis = new elasticache.CfnReplicationGroup(this, 'TokyoSessions', {
  replicationGroupDescription: 'Tokyo session storage',
  cacheNodeType: 'cache.r6g.large',
  numCacheClusters: 2,
});
```

### Kafka Cross-Region Replication

**MirrorMaker 2.0 Configuration**:

```yaml
clusters:
  taiwan:
    bootstrap.servers: taiwan-kafka:9092
    security.protocol: SSL
  tokyo:
    bootstrap.servers: tokyo-kafka:9092
    security.protocol: SSL

mirrors:

  - source: taiwan

    target: tokyo
    topics: "orders.*, payments.*, inventory.*"
    sync.topic.configs.enabled: true
    replication.factor: 3
    
  - source: tokyo

    target: taiwan
    topics: "orders.*, payments.*, inventory.*"
    sync.topic.configs.enabled: true
    replication.factor: 3
```

### Conflict Resolution Mechanisms

**Last-Write-Wins (LWW)**:

```java
@Entity
public class CustomerProfile {
    @Id
    private String id;
    
    @Version
    private Long version;  // Optimistic locking
    
    @Column(name = "last_modified_timestamp")
    private Instant lastModified;
    
    @Column(name = "last_modified_region")
    private String lastModifiedRegion;
    
    public void mergeWith(CustomerProfile other) {
        if (other.lastModified.isAfter(this.lastModified)) {
            // Accept newer version
            this.name = other.name;
            this.email = other.email;
            this.lastModified = other.lastModified;
            this.lastModifiedRegion = other.lastModifiedRegion;
        }
    }
}
```

**Application-Level Resolution**:

```java
public class InventoryConflictResolver {
    
    public void resolveConflict(InventoryUpdate taiwan, InventoryUpdate tokyo) {
        // Conservative: use minimum quantity
        int resolved = Math.min(taiwan.getQuantity(), tokyo.getQuantity());
        
        // Log for investigation
        conflictLogger.warn("Inventory conflict: product={}, taiwan={}, tokyo={}, resolved={}",
            taiwan.getProductId(), taiwan.getQuantity(), tokyo.getQuantity(), resolved);
        
        // Update both regions
        inventoryService.updateQuantity(taiwan.getProductId(), resolved);
        
        // Alert if significant discrepancy
        if (Math.abs(taiwan.getQuantity() - tokyo.getQuantity()) > 10) {
            alertService.sendAlert("Significant inventory conflict detected");
        }
    }
}
```

## 影響分析

### 利害關係人影響

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | High | Multi-region data handling | Patterns, libraries, training |
| Operations Team | High | Replication monitoring | Automation, dashboards |
| End Users | Low | Transparent replication | Proper conflict resolution |
| Business | Medium | Data consistency guarantees | Clear SLAs per context |

### 風險評估

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Data conflicts | Medium | High | Conflict resolution, monitoring |
| Replication lag | Low | Medium | Monitoring, alerts, capacity |
| Split-brain | Low | Critical | Quorum, fencing (ADR-040) |
| Cost overrun | Medium | Medium | Monitoring, optimization |

**整體風險等級**： **Medium**

## 實作計畫

### 第 1 階段： Foundation (Month 1)

- [ ] Set up PostgreSQL logical replication
- [ ] Configure Redis Global Datastore
- [ ] Deploy Kafka MirrorMaker 2.0
- [ ] Implement replication monitoring

### 第 2 階段： Tier 1 Implementation (Month 2)

- [ ] Configure synchronous replication 用於 orders
- [ ] Implement 分散式鎖定 用於 inventory
- [ ] Test quorum writes
- [ ] Validate consistency

### 第 3 階段： Tier 2 Implementation (Month 3)

- [ ] Configure async replication 用於 catalog
- [ ] Implement conflict resolution
- [ ] Test replication lag
- [ ] Validate eventual consistency

### 第 4 階段： Production (Month 4)

- [ ] Gradual rollout
- [ ] Monitor replication metrics
- [ ] Tune performance
- [ ] Document procedures

## 監控和成功標準

### 成功指標

| Metric | Target | Measurement |
|--------|--------|-------------|
| Tier 1 replication lag | < 100ms | Replication monitoring |
| Tier 2 replication lag | < 5s | Replication monitoring |
| Conflict rate | < 0.1% | Conflict logs |
| Data consistency | 100% | Validation queries |
| Replication uptime | 99.99% | Monitoring |

### Key Metrics

```typescript
const metrics = {
  'replication.postgres.tier1.lag_ms': 'Milliseconds',
  'replication.postgres.tier2.lag_seconds': 'Seconds',
  'replication.redis.lag_seconds': 'Seconds',
  'replication.kafka.lag_messages': 'Count',
  'conflicts.detected_per_hour': 'Count',
  'conflicts.resolved_per_hour': 'Count',
  'replication.data_transfer_gb': 'Gigabytes',
};
```

## 後果

### 正面後果

- ✅ **Optimal Consistency**: Strong where needed, eventual where acceptable
- ✅ **High Performance**: Minimal latency impact
- ✅ **Cost-Effective**: $40K/year vs $80K 用於 full sync
- ✅ **Flexibility**: 容易adjust per context
- ✅ **Clear Guarantees**: Well-defined consistency per context

### 負面後果

- ⚠️ **複雜的ity**: Multiple replication strategies
- ⚠️ **Monitoring**: Need comprehensive monitoring
- ⚠️ **Conflicts**: Require resolution mechanisms
- ⚠️ **Training**: Team needs to understand strategies

## 相關決策

- [ADR-001: Use PostgreSQL 用於 Primary Database](001-postgresql-primary-database.md)
- [ADR-004: Use Redis 用於 Distributed Caching](004-redis-distributed-caching.md)
- [ADR-005: Use Apache Kafka (MSK) 用於 Event Streaming](005-kafka-event-streaming.md)
- [ADR-037: Active-Active Multi-Region Architecture](037-active-active-multi-region-architecture.md)
- [ADR-039: Regional Failover 和 Failback Strategy](039-regional-failover-failback-strategy.md)
- [ADR-040: Network Partition Handling Strategy](040-network-partition-handling-strategy.md)

---

**文檔狀態**： ✅ Accepted  
**上次審查**： 2025-10-25  
**下次審查**： 2026-01-25 （每季）
