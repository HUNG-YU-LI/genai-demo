---
adr_number: 037
title: "Active-Active Multi-Region Architecture (TPE-Tokyo)"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [017, 018, 035, 038, 039, 040, 041]
affected_viewpoints: ["deployment", "operational"]
affected_perspectives: ["availability", "performance", "location"]
---

# ADR-037: Active-Active Multi-Region Architecture (TPE-Tokyo)

## 狀態

**Accepted** - 2025-10-25

## 上下文

### 問題陳述

The Enterprise E-Commerce Platform faces critical geopolitical 和 operational risks:

**Geopolitical Risks**:

- **Taiwan-China Tensions**: Escalating military tensions 與 potential 用於 conflict
- **Missile Attack Risk**: Taiwan within range of Chinese missile systems
- **Submarine Cable Vulnerability**: 99% of Taiwan's internet traffic via undersea cables
- **Cyber Warfare**: Frequent DDoS attacks 和 APT campaigns from state actors
- **Economic Sanctions**: Potential 用於 trade restrictions affecting operations

**Operational Risks**:

- **Natural Disasters**: Taiwan in earthquake 和 typhoon zones
- **Single Point of Failure**: Single-region deployment creates business continuity risk
- **Latency**: Customers in Japan experience higher latency
- **Regulatory**: Data sovereignty requirements 用於 different markets

**Business Impact**:

- Revenue loss 期間 regional outages
- Customer trust erosion
- Regulatory non-compliance
- Competitive disadvantage
- Insurance 和 liability issues

### 業務上下文

**業務驅動因素**：

- Business continuity 期間 geopolitical crises
- Disaster recovery 用於 natural disasters
- Market expansion to Japan
- Regulatory compliance (data residency)
- Customer experience 改善ment (lower latency)
- Competitive advantage (99.99% availability)

**限制條件**：

- 預算: $500,000/year 用於 multi-region infrastructure
- Timeline: 6 個月 用於 initial deployment
- Existing Taiwan infrastructure 必須 remain operational
- Zero downtime migration required
- Data consistency 需求各異 透過 bounded context

### 技術上下文

**目前狀態**：

- Single region deployment in Taiwan (ap-northeast-3)
- No disaster recovery capability
- Manual failover procedures
- RTO: 4+ hours, RPO: 1+ hour
- No geographic load distribution

**需求**：

- Active-active deployment in two regions
- Automatic failover capability
- RTO: < 5 minutes
- RPO: < 1 minute
- Geographic load distribution
- Data consistency 跨 regions
- Cost-effective solution

## 決策驅動因素

1. **Geopolitical Resilience**: Survive Taiwan-China conflict scenarios
2. **Business Continuity**: 維持 operations 期間 regional disasters
3. **Performance**: 降低 latency 用於 Japanese customers
4. **Availability**: Achieve 99.99% uptime SLA
5. **成本**： Optimize infrastructure costs
6. **複雜的ity**: Manageable operational 複雜的ity
7. **Data Consistency**: Balance consistency 與 availability
8. **Regulatory**: Meet data residency requirements

## 考慮的選項

### 選項 1： Active-Active Multi-Region (TPE + Tokyo) - Recommended

**描述**： Deploy fully operational infrastructure in both Taipei (ap-northeast-3) and Tokyo (ap-northeast-1) with active traffic serving from both regions

**架構**：

```text
┌─────────────────────────────────────────────────────────────┐
│                     Route 53 Global DNS                      │
│              Geolocation + Health Check Routing              │
└────────────────┬────────────────────────────┬────────────────┘
                 │                            │
        ┌────────▼────────┐          ┌───────▼────────┐
        │  Taiwan Region  │          │  Tokyo Region  │
        │ (ap-northeast-3)│          │(ap-northeast-1)│
        └────────┬────────┘          └───────┬────────┘
                 │                            │
        ┌────────▼────────┐          ┌───────▼────────┐
        │   EKS Cluster   │          │  EKS Cluster   │
        │   RDS Primary   │◄────────►│  RDS Primary   │
        │  ElastiCache    │  Sync    │ ElastiCache    │
        │   MSK Kafka     │◄────────►│   MSK Kafka    │
        └─────────────────┘          └────────────────┘
```

**Traffic Distribution**:

- Taiwan/Hong Kong/Southeast Asia → Taiwan region
- Japan/Korea/Pacific → Tokyo region
- Automatic failover on region failure
- Manual override capability

**Data Replication**:

- **PostgreSQL**: Logical replication (quasi-synchronous)
- **Redis**: Redis Cluster 與 cross-region replication
- **Kafka**: MirrorMaker 2.0 用於 event streaming
- **S3**: Cross-Region Replication (CRR)

**優點**：

- ✅ Survives complete Taiwan region failure
- ✅ Low latency 用於 both markets
- ✅ True 高可用性 (99.99%+)
- ✅ Automatic failover (< 5 minutes)
- ✅ Load distribution 降低s costs
- ✅ 支援s market expansion
- ✅ Meets data residency requirements

**缺點**：

- ⚠️ Higher infrastructure cost (2x compute)
- ⚠️ Data consistency 複雜的ity
- ⚠️ Cross-region data transfer costs
- ⚠️ Operational 複雜的ity

**成本**：

- Infrastructure: $400,000/year (2x compute, storage)
- Data Transfer: $50,000/year (cross-region)
- Operations: $50,000/year (additional staff)
- **Total**: $500,000/year

**風險**： **Low** - Proven architecture pattern

### 選項 2： Active-Passive (TPE Primary + Tokyo DR)

**描述**： Taiwan as primary region, Tokyo as cold/warm standby

**優點**：

- ✅ Lower cost ($250,000/year)
- ✅ 簡單的r operations
- ✅ Easier 資料一致性

**缺點**：

- ❌ Manual failover required (RTO: 30+ minutes)
- ❌ No latency 改善ment 用於 Japan
- ❌ Underutilized DR resources
- ❌ Higher RPO (5-15 minutes)
- ❌ No load distribution

**成本**： $250,000/year

**風險**： **Medium** - Manual failover unreliable

### 選項 3： Multi-Region with Third Region (TPE + Tokyo + Singapore)

**描述**： Three-region deployment for maximum resilience

**優點**：

- ✅ Survives dual-region failure
- ✅ Southeast Asia coverage
- ✅ Maximum resilience

**缺點**：

- ❌ Very high cost ($750,000/year)
- ❌ High 複雜的ity
- ❌ Overkill 用於 current needs
- ❌ Data consistency challenges

**成本**： $750,000/year

**風險**： **Low** - But unnecessary complexity

## 決策結果

**選擇的選項**： **Active-Active Multi-Region (TPE + Tokyo) - Option 1**

### 理由

Active-active multi-region architecture was selected 用於 the following critical reasons:

1. **Geopolitical Resilience**: Survives complete Taiwan region failure due to military conflict
2. **Business Continuity**: 維持s operations 期間 natural disasters (earthquakes, typhoons)
3. **Performance**: 50-70ms latency reduction 用於 Japanese customers
4. **Availability**: Achieves 99.99% uptime (52 minutes downtime/year)
5. **Cost-Effective**: Load distribution 降低s per-region costs vs single region
6. **Market Expansion**: 支援s Japan market growth
7. **Automatic Failover**: < 5 minute RTO 沒有 manual intervention

### Region Selection Rationale

**Taiwan (ap-northeast-3)**:

- Primary market (60% of customers)
- Existing infrastructure
- Lower latency 用於 Taiwan/Hong Kong/SEA customers
- Data residency 用於 Taiwan customers

**Tokyo (ap-northeast-1)**:

- Geographically separated from Taiwan (2,100 km)
- Politically stable
- 優秀的 AWS infrastructure
- Low latency 用於 Japan/Korea customers (20-30ms)
- Data residency 用於 Japanese customers
- Submarine cable diversity

**為何不選 Other Regions**：

- **Singapore**: Too far from Taiwan (3,300 km), higher latency
- **Seoul**: Too close to North Korea, geopolitical risk
- **Hong Kong**: Too close to China, similar risks as Taiwan
- **Sydney**: Too far (7,000 km), very high latency

### Traffic Distribution Strategy

**Route 53 Geolocation Routing**:

```typescript
// CDK Configuration
const globalDNS = new route53.HostedZone(this, 'GlobalDNS', {
  zoneName: 'ecommerce-platform.com',
});

// Taiwan region record
new route53.ARecord(this, 'TaiwanRegion', {
  zone: globalDNS,
  recordName: 'api',
  target: route53.RecordTarget.fromAlias(
    new targets.LoadBalancerTarget(taiwanALB)
  ),
  geoLocation: route53.GeoLocation.country('TW'),
});

// Tokyo region record
new route53.ARecord(this, 'TokyoRegion', {
  zone: globalDNS,
  recordName: 'api',
  target: route53.RecordTarget.fromAlias(
    new targets.LoadBalancerTarget(tokyoALB)
  ),
  geoLocation: route53.GeoLocation.country('JP'),
});

// Default to closest region
new route53.ARecord(this, 'DefaultRegion', {
  zone: globalDNS,
  recordName: 'api',
  target: route53.RecordTarget.fromAlias(
    new targets.LoadBalancerTarget(taiwanALB)
  ),
  geoLocation: route53.GeoLocation.default(),
});
```

**Health Check Configuration**:

```typescript
// Health checks for automatic failover
const taiwanHealthCheck = new route53.CfnHealthCheck(this, 'TaiwanHealth', {
  healthCheckConfig: {
    type: 'HTTPS',
    resourcePath: '/health',
    fullyQualifiedDomainName: 'taiwan.ecommerce-platform.com',
    port: 443,
    requestInterval: 30,
    failureThreshold: 3,
  },
  healthCheckTags: [
    { key: 'Name', value: 'Taiwan Region Health' },
    { key: 'Region', value: 'ap-northeast-3' },
  ],
});

const tokyoHealthCheck = new route53.CfnHealthCheck(this, 'TokyoHealth', {
  healthCheckConfig: {
    type: 'HTTPS',
    resourcePath: '/health',
    fullyQualifiedDomainName: 'tokyo.ecommerce-platform.com',
    port: 443,
    requestInterval: 30,
    failureThreshold: 3,
  },
  healthCheckTags: [
    { key: 'Name', value: 'Tokyo Region Health' },
    { key: 'Region', value: 'ap-northeast-1' },
  ],
});
```

### Data Synchronization Strategy

**Bounded Context Classification**:

**Strong Consistency (CP - Consistency + Partition Tolerance)**:

- **Orders**: Quorum write (both regions 必須 acknowledge)
- **Payments**: Synchronous replication
- **Inventory**: Distributed locks + dual-write

**Eventual Consistency (AP - Availability + Partition Tolerance)**:

- **Product Catalog**: Asynchronous replication (5-10s lag acceptable)
- **Customer Profiles**: Asynchronous replication
- **Shopping Carts**: Regional isolation, merge on checkout
- **Reviews**: Asynchronous replication

**Replication Technologies**:

**PostgreSQL Logical Replication**:

```sql
-- Taiwan region (publisher)
CREATE PUBLICATION taiwan_pub FOR ALL TABLES;

-- Tokyo region (subscriber)
CREATE SUBSCRIPTION tokyo_sub
  CONNECTION 'host=taiwan-db.region.rds.amazonaws.com port=5432 dbname=ecommerce'
  PUBLICATION taiwan_pub
  WITH (copy_data = true, create_slot = true);

-- Bidirectional replication
CREATE PUBLICATION tokyo_pub FOR ALL TABLES;

CREATE SUBSCRIPTION taiwan_sub
  CONNECTION 'host=tokyo-db.region.rds.amazonaws.com port=5432 dbname=ecommerce'
  PUBLICATION tokyo_pub
  WITH (copy_data = true, create_slot = true);
```

**Redis Cross-Region Replication**:

```typescript
// Redis Global Datastore
const globalDatastore = new elasticache.CfnGlobalReplicationGroup(this, 'GlobalRedis', {
  globalReplicationGroupIdSuffix: 'ecommerce',
  primaryReplicationGroupId: taiwanRedisCluster.ref,
  globalReplicationGroupDescription: 'Cross-region Redis replication',
  
  // Add Tokyo as member
  members: [
    {
      replicationGroupId: tokyoRedisCluster.ref,
      replicationGroupRegion: 'ap-northeast-1',
      role: 'SECONDARY',
    }
  ],
});
```

**Kafka MirrorMaker 2.0**:

```yaml
# MirrorMaker 2.0 configuration
clusters:
  taiwan:
    bootstrap.servers: taiwan-kafka.region.amazonaws.com:9092
  tokyo:
    bootstrap.servers: tokyo-kafka.region.amazonaws.com:9092

mirrors:

  - source: taiwan

    target: tokyo
    topics: ".*"
    sync.topic.configs.enabled: true
    sync.topic.acls.enabled: false
    
  - source: tokyo

    target: taiwan
    topics: ".*"
    sync.topic.configs.enabled: true
    sync.topic.acls.enabled: false
```

### Conflict Resolution Strategy

**Last-Write-Wins (LWW)**:

```java
// For customer profile updates
public class CustomerProfile {
    private String id;
    private String name;
    private String email;
    private long version;  // Timestamp-based version
    private String lastModifiedRegion;
    
    public void merge(CustomerProfile other) {
        if (other.version > this.version) {
            // Other version is newer, accept it
            this.name = other.name;
            this.email = other.email;
            this.version = other.version;
            this.lastModifiedRegion = other.lastModifiedRegion;
        }
    }
}
```

**Application-Level Resolution**:

```java
// For inventory updates
public class InventoryConflictResolver {
    
    public Inventory resolve(Inventory taiwan, Inventory tokyo) {
        // Conservative approach: use minimum quantity
        int resolvedQuantity = Math.min(
            taiwan.getQuantity(),
            tokyo.getQuantity()
        );
        
        // Log conflict for investigation
        logger.warn("Inventory conflict detected for product {}: Taiwan={}, Tokyo={}",
            taiwan.getProductId(), taiwan.getQuantity(), tokyo.getQuantity());
        
        // Create resolved inventory
        return Inventory.builder()
            .productId(taiwan.getProductId())
            .quantity(resolvedQuantity)
            .lastModified(Instant.now())
            .resolvedFrom(List.of(taiwan, tokyo))
            .build();
    }
}
```

## 影響分析

### 利害關係人影響

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | High | Multi-region code 複雜的ity, 資料一致性 | Training, patterns, tools |
| Operations Team | High | Multi-region monitoring, incident response | Automation, runbooks, training |
| End Users | Low | 改善d latency, higher availability | Transparent migration |
| Business | Medium | Higher infrastructure cost, 更好的 resilience | ROI analysis, phased rollout |
| Security Team | Medium | Cross-region security, compliance | Security controls, audits |

### 影響半徑

**選擇的影響半徑**： **Enterprise**

影響：

- All application services
- All databases 和 caches
- All message queues
- All monitoring 和 logging
- All deployment pipelines
- All disaster recovery procedures

### 風險評估

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| Data inconsistency | Medium | High | Conflict resolution, monitoring, alerts |
| Cross-region latency | Low | Medium | Optimize replication, caching |
| Increased costs | High | Medium | Cost monitoring, optimization |
| Operational 複雜的ity | Medium | High | Automation, training, documentation |
| Split-brain scenario | Low | Critical | Quorum-based consensus, fencing |

**整體風險等級**： **Medium**

## 實作計畫

### 第 1 階段： Foundation (Month 1-2)

**Objectives**:

- Set up Tokyo region infrastructure
- Establish cross-region networking
- Deploy monitoring 和 observability

**Tasks**:

- [ ] Provision Tokyo VPC 和 networking
- [ ] Set up VPC peering between regions
- [ ] Deploy EKS cluster in Tokyo
- [ ] Set up cross-region monitoring
- [ ] Configure Route 53 global DNS
- [ ] Deploy health check endpoints

**Success Criteria**:

- Tokyo infrastructure operational
- Cross-region connectivity verified
- Monitoring dashboards functional

### 第 2 階段： Data Replication (Month 3-4)

**Objectives**:

- Establish database replication
- Configure cache replication
- Set up event streaming

**Tasks**:

- [ ] Configure PostgreSQL logical replication
- [ ] Set up Redis Global Datastore
- [ ] Deploy Kafka MirrorMaker 2.0
- [ ] Configure S3 Cross-Region Replication
- [ ] Test data synchronization
- [ ] Implement conflict resolution

**Success Criteria**:

- Data replication lag < 5 seconds
- Conflict resolution working
- Zero data loss in tests

### 第 3 階段： Application Deployment (Month 5)

**Objectives**:

- Deploy applications to Tokyo
- Configure traffic routing
- Test failover scenarios

**Tasks**:

- [ ] Deploy application services to Tokyo
- [ ] Configure Route 53 geolocation routing
- [ ] Test traffic distribution
- [ ] Perform failover drills
- [ ] Validate 資料一致性
- [ ] Load testing

**Success Criteria**:

- All services operational in both regions
- Traffic routing working correctly
- Failover < 5 minutes

### 第 4 階段： Production Cutover (Month 6)

**Objectives**:

- Gradual traffic migration
- Production validation
- Full active-active operation

**Tasks**:

- [ ] Migrate 10% traffic to Tokyo
- [ ] Monitor 和 validate
- [ ] Migrate 50% traffic to Tokyo
- [ ] Monitor 和 validate
- [ ] 啟用 full active-active
- [ ] 24/7 monitoring 用於 2 週

**Success Criteria**:

- 99.99% availability achieved
- RTO < 5 minutes validated
- RPO < 1 minute validated
- No data inconsistencies

### 回滾策略

**觸發條件**：

- Data consistency issues
- Failover failures
- Performance degradation > 20%
- Critical bugs in multi-region code

**回滾步驟**：

1. **Immediate**: Route all traffic to Taiwan region
2. **Data**: Stop replication, validate Taiwan data integrity
3. **Services**: Scale down Tokyo services
4. **Verification**: Validate single-region operation
5. **Investigation**: Root cause analysis

**回滾時間**： < 30 minutes

## 監控和成功標準

### 成功指標

| Metric | Target | Measurement |
|--------|--------|-------------|
| Availability | 99.99% | Uptime monitoring |
| RTO | < 5 minutes | Failover drills |
| RPO | < 1 minute | Replication lag |
| Latency (Taiwan) | < 50ms | APM |
| Latency (Japan) | < 30ms | APM |
| Data sync lag | < 5 seconds | Replication monitoring |
| Failover success | 100% | Quarterly drills |

### 監控計畫

**Key Metrics**:

```typescript
// CloudWatch metrics
const metrics = {
  // Regional health
  'region.taiwan.health': 'Healthy/Unhealthy',
  'region.tokyo.health': 'Healthy/Unhealthy',
  
  // Traffic distribution
  'traffic.taiwan.requests_per_second': 'Count',
  'traffic.tokyo.requests_per_second': 'Count',
  
  // Replication lag
  'replication.postgres.lag_seconds': 'Seconds',
  'replication.redis.lag_seconds': 'Seconds',
  'replication.kafka.lag_messages': 'Count',
  
  // Failover
  'failover.last_event': 'Timestamp',
  'failover.duration_seconds': 'Seconds',
  
  // Data consistency
  'consistency.conflicts_per_hour': 'Count',
  'consistency.resolution_success_rate': 'Percentage',
};
```

**告警**：

- **P0 Critical**: Region failure, failover failure
- **P1 High**: Replication lag > 10s, data conflicts
- **P2 Medium**: Replication lag > 5s, latency increase
- **P3 Low**: Cost anomalies, capacity warnings

**Dashboards**:

- Multi-region overview dashboard
- Regional health dashboard
- Replication status dashboard
- Traffic distribution dashboard
- Cost analysis dashboard

### Review Schedule

- **Daily**: Health check review
- **Weekly**: Metrics review, cost analysis
- **Monthly**: Failover drill, capacity planning
- **Quarterly**: Architecture review, optimization

## 後果

### 正面後果

- ✅ **Resilience**: Survives Taiwan region failure (war, disaster)
- ✅ **Availability**: 99.99% uptime (52 min downtime/year)
- ✅ **Performance**: 50-70ms latency reduction 用於 Japan
- ✅ **Business Continuity**: Operations continue 期間 crises
- ✅ **Market Expansion**: 支援s Japan market growth
- ✅ **Competitive Advantage**: Superior availability vs competitors
- ✅ **Customer Trust**: Demonstrates commitment to reliability

### 負面後果

- ⚠️ **成本**： $500,000/year infrastructure cost (2x single region)
- ⚠️ **複雜的ity**: Multi-region operations 複雜的ity
- ⚠️ **Data Consistency**: Eventual consistency challenges
- ⚠️ **Cross-Region Costs**: $50,000/year data transfer
- ⚠️ **Operational Overhead**: 24/7 monitoring required
- ⚠️ **Development Effort**: Multi-region aware code

### 技術債務

**已識別債務**：

1. Manual conflict resolution 用於 some scenarios
2. Basic traffic distribution (no intelligent routing)
3. Limited automated failover testing
4. Manual capacity planning

**債務償還計畫**：

- **Q2 2026**: Automated conflict resolution 用於 all scenarios
- **Q3 2026**: Intelligent traffic routing based on load
- **Q4 2026**: Automated failover testing (chaos engineering)
- **2027**: ML-powered capacity prediction

## 相關決策

- [ADR-017: Multi-Region Deployment Strategy](017-multi-region-deployment-strategy.md) - Superseded 透過 this ADR
- [ADR-018: Container Orchestration 與 AWS EKS](018-container-orchestration-eks.md) - EKS in both regions
- [ADR-035: Disaster Recovery Strategy](035-disaster-recovery-strategy.md) - DR integrated 與 multi-region
- [ADR-038: Cross-Region Data Replication Strategy](038-cross-region-data-replication-strategy.md) - Detailed replication design
- [ADR-039: Regional Failover 和 Failback Strategy](039-regional-failover-failback-strategy.md) - Failover procedures
- [ADR-040: Network Partition Handling Strategy](040-network-partition-handling-strategy.md) - Split-brain prevention
- [ADR-041: Data Residency 和 Sovereignty Strategy](041-data-residency-sovereignty-strategy.md) - Compliance requirements

## 備註

### Geopolitical Risk Assessment

**Taiwan-China Conflict Scenarios**:

1. **Missile Attack**: Taiwan region destroyed → Tokyo continues operations
2. **Submarine Cable Cut**: Taiwan isolated → Tokyo serves all traffic
3. **Cyber Attack**: DDoS on Taiwan → Automatic failover to Tokyo
4. **Economic Sanctions**: Taiwan access restricted → Tokyo primary region

**Mitigation**: Active-active architecture ensures business continuity in all scenarios

### Cost Breakdown

**Infrastructure Costs** ($400,000/year):

- EKS: $100,000/year (2 clusters)
- RDS: $120,000/year (2 主要資料庫s)
- ElastiCache: $60,000/year (2 clusters)
- MSK: $80,000/year (2 clusters)
- Load Balancers: $20,000/year
- Storage: $20,000/year

**Data Transfer** ($50,000/year):

- Cross-region replication: $0.09/GB
- Estimated: 50TB/month = $4,500/month

**Operations** ($50,000/year):

- Additional DevOps staff
- Training 和 tools
- Monitoring 和 alerting

### Performance Benchmarks

**Latency 改善ments**:

- Taiwan customers: 20-30ms (no change)
- Japan customers: 150ms → 30ms (80% 改善ment)
- Korea customers: 180ms → 50ms (72% 改善ment)

**Availability Calculation**:

- Single region: 99.9% (8.76 hours downtime/year)
- Active-active: 99.99% (52 minutes downtime/year)
- 改善ment: 10x reduction in downtime

---

**文檔狀態**： ✅ Accepted  
**上次審查**： 2025-10-25  
**下次審查**： 2026-01-25 （每季）
