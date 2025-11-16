---
adr_number: 040
title: "Network Partition Handling Strategy"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [037, 038, 039, 041]
affected_viewpoints: ["deployment", "operational", "concurrency"]
affected_perspectives: ["availability", "performance"]
---

# ADR-040: Network Partition Handling Strategy

## 狀態

**Accepted** - 2025-10-25

## 上下文

### 問題陳述

Active-active multi-region architecture faces the risk of network partitions where regions 可以not communicate:

**Network Partition Scenarios**:

- **Submarine Cable Cut**: Taiwan-Japan undersea cables damaged
- **DDoS Attack**: Cross-region connectivity disrupted
- **AWS Network Issue**: Inter-region VPC peering failure
- **BGP Routing Problem**: Internet routing issues
- **Geopolitical Event**: Deliberate network isolation

**Split-Brain Problem**:
When regions 可以not communicate 但 both remain operational, they may:

- Accept conflicting writes
- Diverge in data state
- Create irreconcilable conflicts
- Violate consistency guarantees
- Cause data corruption

**Business Impact**:

- Data inconsistency 和 conflicts
- Duplicate orders 或 payments
- Inventory overselling
- Customer confusion
- Financial losses
- Regulatory violations

### 業務上下文

**業務驅動因素**：

- Prevent split-brain scenarios
- 維持 資料一致性
- Ensure availability 期間 partitions
- Minimize data conflicts
- Quick recovery after partition heals
- Clear operational procedures

**限制條件**：

- CAP theorem: 可以not have all three (Consistency, Availability, Partition tolerance)
- Network latency: 40-60ms between Taiwan-Tokyo
- 預算: $50,000/year 用於 partition handling infrastructure
- 必須 支援 different consistency levels per bounded context
- Recovery 必須 be automated

### 技術上下文

**目前狀態**：

- Active-active multi-region deployed
- Basic health checks in place
- No partition detection mechanism
- No split-brain prevention
- No partition recovery procedures

**需求**：

- Detect network partitions quickly (< 30 seconds)
- Prevent split-brain scenarios
- 維持 availability where possible
- Minimize data conflicts
- Automated partition recovery
- Clear consistency guarantees per bounded context

## 決策驅動因素

1. **Consistency**: Prevent data corruption 和 conflicts
2. **Availability**: 維持 service 期間 partitions
3. **Detection**: Quickly detect network partitions
4. **Prevention**: Prevent split-brain scenarios
5. **Recovery**: Automated recovery when partition heals
6. **Flexibility**: Different strategies per bounded context
7. **成本**： Optimize infrastructure costs
8. **Simplicity**: Manageable operational 複雜的ity

## 考慮的選項

### 選項 1： Quorum-Based Consensus with Context-Specific CAP Trade-offs (Recommended)

**描述**： Use quorum-based consensus for critical data (CP), allow partition tolerance for non-critical data (AP)

**CAP Trade-offs 透過 Bounded Context**:

**CP (Consistency + Partition Tolerance) - Sacrifice Availability**:

- **Orders**: Require quorum (majority) 用於 writes
- **Payments**: Require quorum 用於 writes
- **Inventory**: Require quorum 用於 writes
- **Strategy**: During partition, minority partition becomes read-only

**AP (Availability + Partition Tolerance) - Sacrifice Consistency**:

- **Product Catalog**: Accept writes in both partitions
- **Customer Profiles**: Accept writes in both partitions
- **Reviews**: Accept writes in both partitions
- **Shopping Carts**: Regional isolation (no cross-region sync)
- **Strategy**: Resolve conflicts after partition heals

**Partition Detection**:

- Cross-region heartbeat (every 10 seconds)
- Multi-path health checks
- External monitoring (third-party service)
- Consensus on partition state

**Split-Brain Prevention**:

- Quorum requirement 用於 critical operations
- Fencing mechanism (disable minority partition)
- Third-party arbitrator (AWS service in neutral region)
- Automatic read-only mode 用於 minority

**優點**：

- ✅ Prevents split-brain 用於 關鍵資料
- ✅ 維持s availability 用於 non-關鍵資料
- ✅ Clear consistency guarantees per context
- ✅ Automated partition handling
- ✅ Flexible 和 pragmatic
- ✅ Balances consistency 和 availability

**缺點**：

- ⚠️ 複雜的ity in managing different strategies
- ⚠️ Minority partition becomes read-only (CP contexts)
- ⚠️ Conflict resolution needed (AP contexts)

**成本**： $50,000/year

**風險**： **Low** - Industry best practice

### 選項 2： Full CP (Consistency + Partition Tolerance)

**描述**： All data requires quorum, minority partition becomes fully read-only

**優點**：

- ✅ Strong consistency everywhere
- ✅ No data conflicts
- ✅ 簡單understand

**缺點**：

- ❌ Minority partition completely unavailable 用於 writes
- ❌ Poor 用戶體驗 期間 partitions
- ❌ Unnecessary 用於 non-關鍵資料

**成本**： $40,000/year

**風險**： **Medium** - Availability impact

### 選項 3： Full AP (Availability + Partition Tolerance)

**描述**： All partitions accept writes, resolve conflicts later

**優點**：

- ✅ Full availability 期間 partitions
- ✅ 良好的 用戶體驗

**缺點**：

- ❌ Data conflicts 用於 關鍵資料
- ❌ 複雜的 conflict resolution
- ❌ Risk of data corruption
- ❌ Unacceptable 用於 orders/payments

**成本**： $45,000/year

**風險**： **High** - Data consistency issues

## 決策結果

**選擇的選項**： **Quorum-Based Consensus with Context-Specific CAP Trade-offs (Option 1)**

### 理由

Context-specific CAP trade-offs 提供 optimal balance:

1. **Critical Data Protection**: CP 用於 orders, payments, inventory prevents financial errors
2. **Availability**: AP 用於 product catalog, profiles 維持s 用戶體驗
3. **Pragmatic**: Different contexts have different consistency needs
4. **Proven**: Industry-standard approach (Cassandra, DynamoDB)
5. **Flexible**: 容易adjust per context
6. **Automated**: Partition detection 和 handling automated

### CAP Trade-off Matrix

| Bounded Context | CAP Choice | Partition Behavior | Rationale |
|-----------------|------------|-------------------|-----------|
| **Orders** | CP | Minority read-only | Financial accuracy critical |
| **Payments** | CP | Minority read-only | Payment integrity required |
| **Inventory** | CP | Minority read-only | Prevent overselling |
| **Product Catalog** | AP | Both accept writes | Stale data acceptable |
| **Customer Profiles** | AP | Both accept writes | Profile conflicts rare |
| **Reviews** | AP | Both accept writes | Review conflicts acceptable |
| **Promotions** | AP | Both accept writes | Promotion changes infrequent |
| **Shopping Carts** | AP | Regional isolation | Merge on checkout |
| **Sessions** | AP | Regional isolation | No cross-region sessions |

### Partition Detection Architecture

**Multi-Layer Partition Detection**:

```typescript
interface PartitionDetector {
  // Layer 1: Direct connectivity check
  checkDirectConnectivity(): Promise<boolean>;
  
  // Layer 2: Application-level heartbeat
  checkApplicationHeartbeat(): Promise<boolean>;
  
  // Layer 3: External monitoring
  checkExternalMonitoring(): Promise<boolean>;
  
  // Consensus: Agree on partition state
  reachConsensus(): Promise<PartitionState>;
}

class NetworkPartitionDetector implements PartitionDetector {
  
  private readonly HEARTBEAT_INTERVAL = 10000; // 10 seconds
  private readonly HEARTBEAT_TIMEOUT = 30000; // 30 seconds
  private readonly CONSENSUS_THRESHOLD = 2; // 2 out of 3 checks
  
  async detectPartition(): Promise<PartitionDetection> {
    const checks = await Promise.all([
      this.checkDirectConnectivity(),
      this.checkApplicationHeartbeat(),
      this.checkExternalMonitoring(),
    ]);
    
    const failedChecks = checks.filter(c => !c).length;
    
    if (failedChecks >= this.CONSENSUS_THRESHOLD) {
      return {
        partitioned: true,
        detectedAt: new Date(),
        failedChecks: failedChecks,
        confidence: failedChecks === 3 ? 'HIGH' : 'MEDIUM',
      };
    }
    
    return {
      partitioned: false,
      detectedAt: new Date(),
      failedChecks: 0,
      confidence: 'HIGH',
    };
  }
  
  async checkDirectConnectivity(): Promise<boolean> {
    try {
      // TCP connection to other region
      const response = await fetch('https://tokyo-internal.ecommerce.com/health', {
        timeout: 5000,
      });
      return response.ok;
    } catch (error) {
      return false;
    }
  }
  
  async checkApplicationHeartbeat(): Promise<boolean> {
    try {
      // Application-level heartbeat via database
      const lastHeartbeat = await this.getLastHeartbeat('tokyo');
      const age = Date.now() - lastHeartbeat.getTime();
      return age < this.HEARTBEAT_TIMEOUT;
    } catch (error) {
      return false;
    }
  }
  
  async checkExternalMonitoring(): Promise<boolean> {
    try {
      // Third-party monitoring service (e.g., Pingdom, StatusCake)
      const response = await fetch('https://api.monitoring-service.com/check', {
        method: 'POST',
        body: JSON.stringify({
          source: 'taiwan',
          target: 'tokyo',
        }),
      });
      const result = await response.json();
      return result.reachable;
    } catch (error) {
      return false;
    }
  }
}
```

**Heartbeat Implementation**:

```java
@Component
@Scheduled(fixedRate = 10000) // Every 10 seconds
public class CrossRegionHeartbeat {
    
    private final JdbcTemplate jdbcTemplate;
    private final String currentRegion;
    
    public void sendHeartbeat() {
        try {
            // Write heartbeat to shared database table
            jdbcTemplate.update(
                "INSERT INTO region_heartbeats (region, timestamp, status) " +
                "VALUES (?, ?, ?) " +
                "ON CONFLICT (region) DO UPDATE SET timestamp = ?, status = ?",
                currentRegion,
                Instant.now(),
                "HEALTHY",
                Instant.now(),
                "HEALTHY"
            );
            
            // Check other region's heartbeat
            Instant lastHeartbeat = jdbcTemplate.queryForObject(
                "SELECT timestamp FROM region_heartbeats WHERE region = ?",
                Instant.class,
                getOtherRegion()
            );
            
            Duration age = Duration.between(lastHeartbeat, Instant.now());
            
            if (age.getSeconds() > 30) {
                // Other region heartbeat stale - possible partition
                handlePossiblePartition(age);
            }
            
        } catch (Exception e) {
            logger.error("Heartbeat failed", e);
            handleHeartbeatFailure(e);
        }
    }
    
    private void handlePossiblePartition(Duration age) {
        logger.warn("Possible network partition detected. Last heartbeat: {} seconds ago", 
            age.getSeconds());
        
        // Trigger partition detection
        partitionDetector.detectPartition();
    }
}
```

### Quorum-Based Write Strategy

**Quorum Configuration**:

```java
public class QuorumConfiguration {
    
    // Total number of regions
    private static final int N = 2; // Taiwan + Tokyo
    
    // Write quorum (majority)
    private static final int W = 2; // Both regions must acknowledge
    
    // Read quorum
    private static final int R = 1; // Read from any region
    
    // Quorum formula: W + R > N ensures consistency
    // 2 + 1 > 2 ✓
}

@Service
public class QuorumWriteService {
    
    private final OrderRepository taiwanRepo;
    private final OrderRepository tokyoRepo;
    private final PartitionDetector partitionDetector;
    
    public Order createOrder(CreateOrderCommand command) {
        // Check for partition
        if (partitionDetector.isPartitioned()) {
            return handlePartitionedWrite(command);
        }
        
        // Normal case: write to both regions
        return writeWithQuorum(command);
    }
    
    private Order writeWithQuorum(CreateOrderCommand command) {
        Order order = Order.create(command);
        
        // Write to both regions in parallel
        CompletableFuture<Order> taiwanWrite = CompletableFuture.supplyAsync(
            () -> taiwanRepo.save(order)
        );
        
        CompletableFuture<Order> tokyoWrite = CompletableFuture.supplyAsync(
            () -> tokyoRepo.save(order)
        );
        
        try {
            // Wait for both writes (quorum = 2)
            CompletableFuture.allOf(taiwanWrite, tokyoWrite)
                .get(5, TimeUnit.SECONDS);
            
            return order;
            
        } catch (TimeoutException e) {
            // One region didn't respond - possible partition
            throw new QuorumNotAchievedException(
                "Failed to achieve write quorum", e
            );
        }
    }
    
    private Order handlePartitionedWrite(CreateOrderCommand command) {
        // Determine if we're in majority partition
        if (partitionDetector.isInMajorityPartition()) {
            // Majority partition: allow writes
            return writeToLocalRegion(command);
        } else {
            // Minority partition: reject writes
            throw new PartitionedWriteException(
                "Cannot write to minority partition. System is read-only."
            );
        }
    }
}
```

### Split-Brain Prevention

**Fencing Mechanism**:

```java
@Component
public class PartitionFencingService {
    
    private final DynamoDbClient dynamoDb;
    private final String FENCING_TABLE = "region_fencing";
    
    public FencingToken acquireFencingToken(String region) {
        try {
            // Attempt to acquire fencing token with conditional write
            PutItemRequest request = PutItemRequest.builder()
                .tableName(FENCING_TABLE)
                .item(Map.of(
                    "partition_key", AttributeValue.builder().s("ACTIVE_REGION").build(),
                    "region", AttributeValue.builder().s(region).build(),
                    "timestamp", AttributeValue.builder().n(String.valueOf(System.currentTimeMillis())).build(),
                    "token", AttributeValue.builder().s(UUID.randomUUID().toString()).build()
                ))
                .conditionExpression("attribute_not_exists(partition_key) OR #ts < :expiry")
                .expressionAttributeNames(Map.of("#ts", "timestamp"))
                .expressionAttributeValues(Map.of(
                    ":expiry", AttributeValue.builder()
                        .n(String.valueOf(System.currentTimeMillis() - 60000)) // 1 minute expiry
                        .build()
                ))
                .build();
            
            dynamoDb.putItem(request);
            
            return new FencingToken(region, true);
            
        } catch (ConditionalCheckFailedException e) {
            // Another region holds the token
            return new FencingToken(region, false);
        }
    }
    
    public void enterReadOnlyMode(String reason) {
        logger.warn("Entering read-only mode: {}", reason);
        
        // Set global read-only flag
        applicationContext.setReadOnly(true);
        
        // Reject all write operations
        writeOperationGuard.blockWrites();
        
        // Send alerts
        alertService.sendAlert(
            "Region entered read-only mode",
            AlertSeverity.HIGH,
            Map.of("reason", reason)
        );
        
        // Update monitoring
        metricsService.recordReadOnlyMode(true);
    }
}
```

**Third-Party Arbitrator**:

```typescript
// Use AWS service in neutral region (Singapore) as arbitrator
class ThirdPartyArbitrator {
  
  private readonly dynamoDb: DynamoDB;
  private readonly ARBITRATOR_TABLE = 'partition_arbitrator';
  
  async determineActiveRegion(): Promise<string> {
    // Both regions attempt to register with arbitrator
    // First one to register becomes active
    
    try {
      await this.dynamoDb.putItem({
        TableName: this.ARBITRATOR_TABLE,
        Item: {
          partition_key: { S: 'ACTIVE_PARTITION' },
          region: { S: this.currentRegion },
          timestamp: { N: Date.now().toString() },
        },
        ConditionExpression: 'attribute_not_exists(partition_key)',
      });
      
      // Successfully registered - we are active
      return this.currentRegion;
      
    } catch (error) {
      if (error.code === 'ConditionalCheckFailedException') {
        // Another region already registered
        const item = await this.dynamoDb.getItem({
          TableName: this.ARBITRATOR_TABLE,
          Key: {
            partition_key: { S: 'ACTIVE_PARTITION' },
          },
        });
        
        return item.Item.region.S;
      }
      
      throw error;
    }
  }
}
```

### Conflict Resolution 用於 AP Contexts

**Conflict Detection**:

```java
public class ConflictDetector {
    
    public List<DataConflict> detectConflicts(String region1, String region2) {
        List<DataConflict> conflicts = new ArrayList<>();
        
        // Check product catalog conflicts
        conflicts.addAll(detectProductConflicts(region1, region2));
        
        // Check customer profile conflicts
        conflicts.addAll(detectCustomerConflicts(region1, region2));
        
        // Check review conflicts
        conflicts.addAll(detectReviewConflicts(region1, region2));
        
        return conflicts;
    }
    
    private List<DataConflict> detectProductConflicts(String r1, String r2) {
        List<Product> products1 = productRepository.findAll(r1);
        List<Product> products2 = productRepository.findAll(r2);
        
        List<DataConflict> conflicts = new ArrayList<>();
        
        for (Product p1 : products1) {
            Product p2 = findById(products2, p1.getId());
            
            if (p2 != null && !p1.equals(p2)) {
                // Same product, different versions
                if (p1.getVersion() != p2.getVersion()) {
                    conflicts.add(new DataConflict(
                        "Product",
                        p1.getId(),
                        p1,
                        p2,
                        ConflictType.VERSION_MISMATCH
                    ));
                }
            }
        }
        
        return conflicts;
    }
}
```

**Conflict Resolution Strategies**:

```java
public class ConflictResolver {
    
    public void resolveConflicts(List<DataConflict> conflicts) {
        for (DataConflict conflict : conflicts) {
            switch (conflict.getType()) {
                case VERSION_MISMATCH:
                    resolveVersionConflict(conflict);
                    break;
                case CONCURRENT_UPDATE:
                    resolveConcurrentUpdate(conflict);
                    break;
                case DELETE_UPDATE:
                    resolveDeleteUpdate(conflict);
                    break;
            }
        }
    }
    
    private void resolveVersionConflict(DataConflict conflict) {
        // Last-Write-Wins based on timestamp
        Object newer = conflict.getVersion1().getLastModified()
            .isAfter(conflict.getVersion2().getLastModified())
            ? conflict.getVersion1()
            : conflict.getVersion2();
        
        // Update both regions with newer version
        updateBothRegions(conflict.getEntityType(), conflict.getEntityId(), newer);
        
        // Log resolution
        logger.info("Resolved version conflict for {} {}: chose version from {}",
            conflict.getEntityType(),
            conflict.getEntityId(),
            newer.getLastModifiedRegion()
        );
    }
    
    private void resolveConcurrentUpdate(DataConflict conflict) {
        // Merge strategy for concurrent updates
        Object merged = mergeVersions(
            conflict.getVersion1(),
            conflict.getVersion2()
        );
        
        updateBothRegions(conflict.getEntityType(), conflict.getEntityId(), merged);
    }
    
    private Object mergeVersions(Object v1, Object v2) {
        // Field-level merge
        // Keep non-null values from both versions
        // For conflicts, use Last-Write-Wins per field
        
        if (v1 instanceof Product p1 && v2 instanceof Product p2) {
            return Product.builder()
                .id(p1.getId())
                .name(p1.getNameLastModified().isAfter(p2.getNameLastModified()) 
                    ? p1.getName() : p2.getName())
                .price(p1.getPriceLastModified().isAfter(p2.getPriceLastModified())
                    ? p1.getPrice() : p2.getPrice())
                .description(p1.getDescLastModified().isAfter(p2.getDescLastModified())
                    ? p1.getDescription() : p2.getDescription())
                .build();
        }
        
        throw new UnsupportedOperationException("Merge not supported for type");
    }
}
```

### Partition Recovery

**Automatic Recovery Process**:

```java
@Component
public class PartitionRecoveryService {
    
    @Scheduled(fixedRate = 30000) // Check every 30 seconds
    public void checkPartitionRecovery() {
        if (partitionDetector.isPartitioned()) {
            // Still partitioned
            return;
        }
        
        if (wasPartitioned()) {
            // Partition healed - initiate recovery
            initiateRecovery();
        }
    }
    
    private void initiateRecovery() {
        logger.info("Network partition healed. Initiating recovery...");
        
        try {
            // Phase 1: Detect conflicts
            List<DataConflict> conflicts = conflictDetector.detectConflicts(
                "taiwan", "tokyo"
            );
            
            logger.info("Detected {} conflicts during partition", conflicts.size());
            
            // Phase 2: Resolve conflicts
            conflictResolver.resolveConflicts(conflicts);
            
            // Phase 3: Resume normal replication
            replicationService.resumeReplication();
            
            // Phase 4: Exit read-only mode (if applicable)
            if (applicationContext.isReadOnly()) {
                fencingService.exitReadOnlyMode();
            }
            
            // Phase 5: Verify consistency
            boolean consistent = consistencyChecker.verifyConsistency();
            
            if (!consistent) {
                throw new RecoveryException("Consistency check failed after recovery");
            }
            
            // Phase 6: Send notifications
            alertService.sendAlert(
                "Partition recovery completed successfully",
                AlertSeverity.INFO,
                Map.of(
                    "conflicts_resolved", conflicts.size(),
                    "duration", getPartitionDuration()
                )
            );
            
            // Phase 7: Clear partition state
            clearPartitionState();
            
        } catch (Exception e) {
            logger.error("Partition recovery failed", e);
            alertService.sendAlert(
                "Partition recovery failed - manual intervention required",
                AlertSeverity.CRITICAL,
                Map.of("error", e.getMessage())
            );
        }
    }
}
```

## 影響分析

### 利害關係人影響

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | High | Partition-aware code, CAP understanding | Training, patterns, libraries |
| Operations Team | High | Partition monitoring, recovery procedures | Automation, runbooks, alerts |
| End Users (CP contexts) | Medium | Write unavailability in minority partition | Clear error messages, retry logic |
| End Users (AP contexts) | Low | Transparent operation | Conflict resolution |
| Business | Medium | Potential write unavailability | Clear SLAs, monitoring |

### 影響半徑

**選擇的影響半徑**： **Enterprise**

影響：

- All application services
- All 資料儲存s
- Write operations (CP contexts)
- Conflict resolution (AP contexts)
- Monitoring 和 alerting
- Operational procedures

### 風險評估

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| False partition detection | Low | High | Multi-layer detection, consensus |
| Split-brain | Low | Critical | Quorum, fencing, arbitrator |
| Data conflicts (AP) | Medium | Medium | Automated conflict resolution |
| Write unavailability (CP) | Medium | High | Clear error messages, retry logic |
| Recovery failure | Low | High | Automated recovery, manual procedures |

**整體風險等級**： **Medium**

## 實作計畫

### 第 1 階段： Partition Detection (Month 1)

**Objectives**:

- Implement multi-layer partition detection
- Deploy heartbeat system
- Set up external monitoring

**Tasks**:

- [ ] Implement partition detector
- [ ] Deploy heartbeat system
- [ ] Configure external monitoring
- [ ] Set up monitoring dashboards
- [ ] Test partition detection

**Success Criteria**:

- Partition detection < 30 seconds
- False positive rate < 1%
- Monitoring operational

### 第 2 階段： Split-Brain Prevention (Month 2)

**Objectives**:

- Implement quorum-based writes
- Deploy fencing mechanism
- Set up third-party arbitrator

**Tasks**:

- [ ] Implement quorum write service
- [ ] Deploy fencing service
- [ ] Configure DynamoDB arbitrator
- [ ] Implement read-only mode
- [ ] Test split-brain prevention

**Success Criteria**:

- Quorum writes working
- Fencing mechanism operational
- No split-brain in tests

### 第 3 階段： Conflict Resolution (Month 3)

**Objectives**:

- Implement conflict detection
- Deploy conflict resolution
- Test AP context behavior

**Tasks**:

- [ ] Implement conflict detector
- [ ] Deploy conflict resolver
- [ ] Test Last-Write-Wins
- [ ] Test merge strategies
- [ ] Validate AP contexts

**Success Criteria**:

- Conflict detection working
- Resolution automated
- AP contexts operational

### 第 4 階段： Partition Recovery (Month 4)

**Objectives**:

- Implement automated recovery
- Test recovery procedures
- Validate end-to-end flow

**Tasks**:

- [ ] Implement recovery service
- [ ] Test automatic recovery
- [ ] Create manual procedures
- [ ] Conduct partition drills
- [ ] Document procedures

**Success Criteria**:

- Automated recovery working
- Manual procedures documented
- Drills successful

### Phase 5: Production Readiness (Month 5-6)

**Objectives**:

- Comprehensive testing
- Team training
- Production deployment

**Tasks**:

- [ ] Conduct partition simulation drills
- [ ] Train operations team
- [ ] Update monitoring
- [ ] Deploy to production
- [ ] Monitor 用於 30 天

**Success Criteria**:

- All tests passing
- Team trained
- Production stable

### 回滾策略

**觸發條件**：

- Excessive false positives
- Split-brain scenarios
- Data corruption
- Operational issues

**回滾步驟**：

1. **Immediate**: Disable partition detection
2. **Revert**: Return to basic health checks
3. **Manual**: Manual partition handling
4. **Investigation**: Root cause analysis

**回滾時間**： < 1 hour

## 監控和成功標準

### 成功指標

| Metric | Target | Measurement |
|--------|--------|-------------|
| Partition Detection Time | < 30 seconds | Monitoring logs |
| False Positive Rate | < 1% | Detection logs |
| Split-Brain Incidents | 0 | Incident reports |
| Conflict Resolution Rate | > 99% | Resolution logs |
| Recovery Time | < 5 minutes | Recovery logs |
| Data Consistency | 100% | Validation queries |

### Key Metrics

```typescript
const partitionMetrics = {
  // Partition detection
  'partition.detected.total': 'Count',
  'partition.detection.time_seconds': 'Seconds',
  'partition.false_positives': 'Count',
  
  // Split-brain prevention
  'partition.quorum.failures': 'Count',
  'partition.fencing.activations': 'Count',
  'partition.readonly_mode.duration': 'Seconds',
  
  // Conflict resolution
  'partition.conflicts.detected': 'Count',
  'partition.conflicts.resolved': 'Count',
  'partition.conflicts.manual': 'Count',
  
  // Recovery
  'partition.recovery.attempts': 'Count',
  'partition.recovery.successes': 'Count',
  'partition.recovery.duration_seconds': 'Seconds',
};
```

### Monitoring Dashboards

**Partition Status Dashboard**:

- Current partition state
- Heartbeat status
- Quorum status
- Read-only mode status
- Recent partition events

**Conflict Resolution Dashboard**:

- Conflicts detected
- Conflicts resolved
- Resolution success rate
- Manual interventions
- Conflict types

### Review Schedule

- **Daily**: Partition metrics review
- **Weekly**: Conflict resolution review
- **Monthly**: Partition drill
- **Quarterly**: Comprehensive review

## 後果

### 正面後果

- ✅ **Prevents Split-Brain**: Quorum 和 fencing prevent data corruption
- ✅ **維持s Availability**: AP contexts remain available
- ✅ **Automated**: Detection 和 recovery automated
- ✅ **Flexible**: Different strategies per context
- ✅ **Clear Guarantees**: Well-defined consistency per context
- ✅ **Quick Detection**: < 30 second partition detection
- ✅ **Quick Recovery**: < 5 minute automated recovery

### 負面後果

- ⚠️ **Write Unavailability**: CP contexts unavailable in minority partition
- ⚠️ **複雜的ity**: 複雜的 partition handling logic
- ⚠️ **Conflicts**: AP contexts 需要conflict resolution
- ⚠️ **成本**： $50,000/year for infrastructure
- ⚠️ **Monitoring**: Comprehensive monitoring required
- ⚠️ **Training**: Team needs CAP theorem understanding

### 技術債務

**已識別債務**：

1. Basic conflict resolution (Last-Write-Wins only)
2. Manual intervention 用於 複雜的 conflicts
3. Limited partition simulation testing
4. Basic arbitrator (DynamoDB only)

**債務償還計畫**：

- **Q2 2026**: Advanced conflict resolution (CRDTs, vector clocks)
- **Q3 2026**: Automated 複雜的 conflict resolution
- **Q4 2026**: Chaos engineering 用於 partition testing
- **2027**: Multi-arbitrator consensus (Raft/Paxos)

## 相關決策

- [ADR-037: Active-Active Multi-Region Architecture](037-active-active-multi-region-architecture.md) - Multi-region foundation
- [ADR-038: Cross-Region Data Replication Strategy](038-cross-region-data-replication-strategy.md) - Replication 期間 partitions
- [ADR-039: Regional Failover 和 Failback Strategy](039-regional-failover-failback-strategy.md) - Failover vs partition handling
- [ADR-041: Data Residency 和 Sovereignty Strategy](041-data-residency-sovereignty-strategy.md) - Compliance 期間 partitions

## 備註

### CAP Theorem Refresher

**CAP Theorem**: In a distributed system, you 可以 only guarantee 2 out of 3:

- **C**onsistency: All nodes see the same data
- **A**vailability: Every request receives a response
- **P**artition Tolerance: System continues despite network partitions

**Our Choices**:

- **CP (Orders, Payments, Inventory)**: Sacrifice availability 用於 consistency
- **AP (Catalog, Profiles, Reviews)**: Sacrifice consistency 用於 availability

### Partition vs Failover

**Network Partition**:

- Both regions operational
- 可以not communicate
- Risk of split-brain
- Requires quorum/fencing

**Regional Failure**:

- One region down
- Clear failure
- No split-brain risk
- Failover to healthy region

### Submarine Cable Risks

**Taiwan's Internet Connectivity**:

- 99% via undersea cables
- 14 cables connecting Taiwan
- Vulnerable to:
  - Earthquakes
  - Ship anchors
  - Deliberate sabotage
  - Military action

**Historical Incidents**:

- 2006: Hengchun earthquake cut 8 cables
- 2008: Multiple cable cuts
- Regular maintenance disruptions

**Mitigation**: Multi-path detection, satellite backup (future)

### Testing Partition Scenarios

**Partition Simulation**:

```bash
# Simulate partition by blocking cross-region traffic
# Taiwan region
sudo iptables -A OUTPUT -d tokyo-region-ip -j DROP
sudo iptables -A INPUT -s tokyo-region-ip -j DROP

# Wait for partition detection (< 30 seconds)

# Verify behavior:
# - CP contexts: writes fail in minority
# - AP contexts: writes succeed
# - Monitoring: partition detected

# Heal partition
sudo iptables -D OUTPUT -d tokyo-region-ip -j DROP
sudo iptables -D INPUT -s tokyo-region-ip -j DROP

# Verify recovery:
# - Conflicts detected and resolved
# - Replication resumed
# - Normal operation restored
```

---

**文檔狀態**： ✅ Accepted  
**上次審查**： 2025-10-25  
**下次審查**： 2026-01-25 （每季）
