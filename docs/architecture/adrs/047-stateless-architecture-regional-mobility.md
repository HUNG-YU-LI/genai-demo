---
adr_number: 047
title: "Stateless Architecture 用於 Regional Mobility"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [037, 038, 040, 046]
affected_viewpoints: ["deployment", "development", "concurrency"]
affected_perspectives: ["availability", "performance", "evolution"]
---

# ADR-047: Stateless Architecture 用於 Regional Mobility

## 狀態

**Accepted** - 2025-10-25

## 上下文

### 問題陳述

Multi-region active-active architecture 需要seamless regional mobility, 但 stateful components create barriers:

**Stateful Architecture Challenges**:

- **Session Affinity**: Users tied to specific regions
- **In-Memory State**: Lost 期間 failover
- **Local Caching**: Inconsistent 跨 regions
- **File Storage**: Region-specific file systems
- **Connection Pooling**: Region-bound connections
- **Scheduled Jobs**: Duplicate execution 跨 regions

**Impact on Regional Mobility**:

- Slow failover (need to migrate state)
- User session loss 期間 failover
- Inconsistent 用戶體驗
- 複雜的 failover procedures
- Limited load balancing flexibility
- Difficult testing 和 deployment

**Current State Issues**:

- Some services 維持 記憶體內 session state
- Local file storage 用於 uploads
- Region-specific caching
- Stateful WebSocket connections
- Background jobs 與 local state

### 業務上下文

**業務驅動因素**：

- Seamless failover (< 5 minutes)
- Consistent 用戶體驗
- Global load balancing
- Simplified operations
- Cost optimization
- Scalability

**限制條件**：

- 可以not break existing functionality
- 必須 維持 performance
- Minimize refactoring effort
- 支援 gradual migration
- 預算: $30,000 用於 implementation

### 技術上下文

**目前狀態**：

- Mix of stateful 和 stateless services
- Session state in application memory
- Local file storage
- Region-specific caches
- Manual failover procedures

**需求**：

- All services 必須 be stateless
- Externalized 會話管理
- Distributed caching
- Shared file storage
- Idempotent operations
- Automated failover

## 決策驅動因素

1. **Regional Mobility**: 啟用 seamless cross-region failover
2. **Scalability**: 支援 horizontal scaling
3. **Availability**: 改善 failover speed (< 5 minutes)
4. **Consistency**: Ensure consistent 用戶體驗
5. **Simplicity**: Simplify operations 和 deployment
6. **Performance**: 維持 或 改善 performance
7. **成本**： Optimize infrastructure costs
8. **Evolution**: 支援 future architectural changes

## 考慮的選項

### 選項 1： Comprehensive Stateless Architecture (Recommended)

**描述**： Implement fully stateless architecture with externalized state management

**Architecture Principles**:

**1. Stateless Application Services**:

```typescript
// ❌ BAD: Stateful service with in-memory session
@Service
class OrderService {
  private userSessions: Map<string, UserSession> = new Map();
  
  processOrder(userId: string, order: Order) {
    const session = this.userSessions.get(userId);  // State in memory!
    // Process order using session
  }
}

// ✅ GOOD: Stateless service with externalized session
@Service
class OrderService {
  constructor(
    private sessionStore: RedisSessionStore,
    private orderRepository: OrderRepository
  ) {}
  
  async processOrder(sessionId: string, order: Order) {
    const session = await this.sessionStore.get(sessionId);  // External state
    // Process order using session
  }
}
```

**2. Externalized Session Management**:

```typescript
interface SessionManagementStrategy {
  storage: {
    primary: 'Redis (ElastiCache Global Datastore)',
    backup: 'DynamoDB Global Tables',
    ttl: '24 hours',
    replication: 'Cross-region automatic'
  },
  
  sessionData: {
    userId: 'string',
    authToken: 'JWT',
    preferences: 'object',
    cart: 'reference to cart service',
    lastActivity: 'timestamp'
  },
  
  implementation: {
    library: 'Spring Session with Redis',
    serialization: 'JSON',
    compression: 'gzip',
    encryption: 'AES-256'
  }
}

// Spring Session configuration
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 86400)
class SessionConfiguration {
  
  @Bean
  fun redisConnectionFactory(): RedisConnectionFactory {
    return LettuceConnectionFactory(
      RedisStandaloneConfiguration(
        redisHost,
        redisPort
      )
    )
  }
  
  @Bean
  fun cookieSerializer(): CookieSerializer {
    val serializer = DefaultCookieSerializer()
    serializer.setCookieName("SESSION")
    serializer.setCookiePath("/")
    serializer.setDomainNamePattern("^.+?\\.(\\w+\\.[a-z]+)$")
    serializer.setSameSite("Lax")
    serializer.setUseSecureCookie(true)
    serializer.setUseHttpOnlyCookie(true)
    return serializer
  }
}
```

**3. Distributed Caching Strategy**:

```typescript
interface DistributedCachingStrategy {
  architecture: {
    global: 'ElastiCache Global Datastore',
    regions: ['Taiwan', 'Tokyo', 'Singapore'],
    replication: 'Automatic cross-region',
    consistency: 'Eventually consistent (< 1 second)'
  },
  
  cachePatterns: {
    readThrough: {
      description: 'Cache miss loads from database',
      implementation: '@Cacheable annotation',
      ttl: 'Varies by data type'
    },
    
    writeThrough: {
      description: 'Write to cache and database',
      implementation: '@CachePut annotation',
      consistency: 'Strong'
    },
    
    cacheAside: {
      description: 'Application manages cache',
      implementation: 'Manual cache operations',
      flexibility: 'High'
    }
  },
  
  cacheKeys: {
    format: '{service}:{entity}:{id}',
    examples: [
      'product:item:12345',
      'customer:profile:user-789',
      'order:summary:order-456'
    ]
  }
}

// Cache configuration
@Configuration
@EnableCaching
class CacheConfiguration {
  
  @Bean
  fun cacheManager(
    connectionFactory: RedisConnectionFactory
  ): CacheManager {
    val config = RedisCacheConfiguration.defaultCacheConfig()
      .entryTtl(Duration.ofHours(1))
      .serializeKeysWith(
        RedisSerializationContext.SerializationPair
          .fromSerializer(StringRedisSerializer())
      )
      .serializeValuesWith(
        RedisSerializationContext.SerializationPair
          .fromSerializer(GenericJackson2JsonRedisSerializer())
      )
    
    return RedisCacheManager.builder(connectionFactory)
      .cacheDefaults(config)
      .withCacheConfiguration("products", 
        config.entryTtl(Duration.ofMinutes(30)))
      .withCacheConfiguration("customers",
        config.entryTtl(Duration.ofHours(2)))
      .build()
  }
}
```

**4. Shared File Storage**:

```typescript
interface SharedFileStorageStrategy {
  storage: {
    service: 'Amazon S3',
    replication: 'Cross-region replication',
    consistency: 'Strong read-after-write',
    availability: '99.99%'
  },
  
  structure: {
    buckets: {
      uploads: 's3://ecommerce-uploads-{region}',
      assets: 's3://ecommerce-assets-{region}',
      backups: 's3://ecommerce-backups-{region}'
    },
    
    replication: {
      source: 'Taiwan',
      destinations: ['Tokyo', 'Singapore'],
      mode: 'Automatic',
      time: '< 15 minutes'
    }
  },
  
  access: {
    method: 'Pre-signed URLs',
    expiration: '1 hour',
    permissions: 'Least privilege',
    cdn: 'CloudFront for public assets'
  }
}

// S3 service implementation
@Service
class FileStorageService(
  private val s3Client: S3Client,
  private val bucketName: String
) {
  
  fun uploadFile(
    key: String,
    file: MultipartFile
  ): String {
    val putRequest = PutObjectRequest.builder()
      .bucket(bucketName)
      .key(key)
      .contentType(file.contentType)
      .build()
    
    s3Client.putObject(
      putRequest,
      RequestBody.fromInputStream(
        file.inputStream,
        file.size
      )
    )
    
    return generatePresignedUrl(key)
  }
  
  fun generatePresignedUrl(key: String): String {
    val getRequest = GetObjectRequest.builder()
      .bucket(bucketName)
      .key(key)
      .build()
    
    val presignRequest = GetObjectPresignRequest.builder()
      .signatureDuration(Duration.ofHours(1))
      .getObjectRequest(getRequest)
      .build()
    
    return s3Presigner.presignGetObject(presignRequest)
      .url()
      .toString()
  }
}
```

**5. Idempotent Operations**:

```typescript
interface IdempotencyStrategy {
  implementation: {
    method: 'Idempotency keys',
    storage: 'Redis with 24-hour TTL',
    header: 'Idempotency-Key',
    format: 'UUID v4'
  },
  
  operations: {
    payments: 'Required',
    orders: 'Required',
    inventory: 'Required',
    notifications: 'Recommended',
    queries: 'Not needed'
  }
}

// Idempotency implementation
@Service
class IdempotentOrderService(
  private val orderRepository: OrderRepository,
  private val idempotencyStore: RedisTemplate<String, String>
) {
  
  @Transactional
  fun createOrder(
    idempotencyKey: String,
    orderRequest: CreateOrderRequest
  ): Order {
    // Check if already processed
    val existingOrderId = idempotencyStore
      .opsForValue()
      .get("idempotency:$idempotencyKey")
    
    if (existingOrderId != null) {
      return orderRepository.findById(existingOrderId)
        .orElseThrow()
    }
    
    // Process order
    val order = Order.create(orderRequest)
    val savedOrder = orderRepository.save(order)
    
    // Store idempotency key
    idempotencyStore.opsForValue().set(
      "idempotency:$idempotencyKey",
      savedOrder.id,
      24,
      TimeUnit.HOURS
    )
    
    return savedOrder
  }
}

// API endpoint with idempotency
@PostMapping("/orders")
fun createOrder(
  @RequestHeader("Idempotency-Key") idempotencyKey: String,
  @RequestBody request: CreateOrderRequest
): ResponseEntity<OrderResponse> {
  val order = orderService.createOrder(idempotencyKey, request)
  return ResponseEntity.ok(OrderResponse.from(order))
}
```

**6. Stateless Background Jobs**:

```typescript
interface StatelessJobStrategy {
  scheduler: {
    service: 'AWS EventBridge',
    distribution: 'Single execution per schedule',
    failover: 'Automatic region failover'
  },
  
  execution: {
    method: 'Lambda or ECS Fargate',
    state: 'No local state',
    coordination: 'DynamoDB for distributed locks',
    idempotency: 'Required for all jobs'
  },
  
  examples: {
    dailyReport: {
      schedule: 'cron(0 2 * * ? *)',
      execution: 'Lambda',
      state: 'Query from database',
      output: 'S3'
    },
    
    orderCleanup: {
      schedule: 'rate(1 hour)',
      execution: 'ECS Fargate',
      state: 'Query from database',
      lock: 'DynamoDB distributed lock'
    }
  }
}

// Distributed lock for job coordination
@Service
class DistributedLockService(
  private val dynamoDb: DynamoDbClient
) {
  
  fun acquireLock(
    lockName: String,
    ttl: Duration
  ): Boolean {
    try {
      val item = mapOf(
        "lockName" to AttributeValue.builder().s(lockName).build(),
        "owner" to AttributeValue.builder().s(instanceId).build(),
        "expiresAt" to AttributeValue.builder()
          .n(Instant.now().plus(ttl).epochSecond.toString())
          .build()
      )
      
      dynamoDb.putItem(
        PutItemRequest.builder()
          .tableName("distributed-locks")
          .item(item)
          .conditionExpression("attribute_not_exists(lockName)")
          .build()
      )
      
      return true
    } catch (e: ConditionalCheckFailedException) {
      return false
    }
  }
  
  fun releaseLock(lockName: String) {
    dynamoDb.deleteItem(
      DeleteItemRequest.builder()
        .tableName("distributed-locks")
        .key(mapOf(
          "lockName" to AttributeValue.builder().s(lockName).build()
        ))
        .conditionExpression("owner = :owner")
        .expressionAttributeValues(mapOf(
          ":owner" to AttributeValue.builder().s(instanceId).build()
        ))
        .build()
    )
  }
}
```

**7. Stateless WebSocket Connections**:

```typescript
interface StatelessWebSocketStrategy {
  architecture: {
    gateway: 'AWS API Gateway WebSocket',
    backend: 'Lambda or ECS',
    state: 'DynamoDB for connection tracking',
    messaging: 'SNS/SQS for pub/sub'
  },
  
  connectionManagement: {
    storage: 'DynamoDB',
    schema: {
      connectionId: 'string (partition key)',
      userId: 'string (GSI)',
      region: 'string',
      connectedAt: 'timestamp',
      lastActivity: 'timestamp'
    }
  },
  
  messageRouting: {
    method: 'SNS topic per message type',
    fanout: 'All regions receive messages',
    delivery: 'At-least-once',
    deduplication: 'Message ID tracking'
  }
}

// WebSocket connection handler
@Service
class WebSocketConnectionService(
  private val dynamoDb: DynamoDbClient,
  private val apiGatewayClient: ApiGatewayManagementApiClient
) {
  
  fun handleConnect(connectionId: String, userId: String) {
    val item = mapOf(
      "connectionId" to AttributeValue.builder().s(connectionId).build(),
      "userId" to AttributeValue.builder().s(userId).build(),
      "region" to AttributeValue.builder().s(currentRegion).build(),
      "connectedAt" to AttributeValue.builder()
        .n(Instant.now().epochSecond.toString())
        .build()
    )
    
    dynamoDb.putItem(
      PutItemRequest.builder()
        .tableName("websocket-connections")
        .item(item)
        .build()
    )
  }
  
  fun sendMessage(userId: String, message: String) {
    // Query all connections for user
    val connections = queryConnectionsByUserId(userId)
    
    // Send to all connections (may be in different regions)
    connections.forEach { connection ->
      try {
        apiGatewayClient.postToConnection(
          PostToConnectionRequest.builder()
            .connectionId(connection.connectionId)
            .data(SdkBytes.fromUtf8String(message))
            .build()
        )
      } catch (e: GoneException) {
        // Connection closed, remove from DynamoDB
        removeConnection(connection.connectionId)
      }
    }
  }
}
```

**Benefits of Stateless Architecture**:

```typescript
const statelessBenefits = {
  regionalMobility: {
    failoverTime: '< 5 minutes (vs 30 minutes stateful)',
    userImpact: 'Transparent (vs session loss)',
    complexity: 'Low (vs high)',
    automation: 'Fully automated (vs manual)'
  },
  
  scalability: {
    horizontal: 'Unlimited (vs limited)',
    autoScaling: 'Instant (vs slow)',
    loadBalancing: 'Any instance (vs sticky sessions)',
    efficiency: 'High (vs low)'
  },
  
  operations: {
    deployment: 'Rolling updates (vs blue-green)',
    testing: 'Simple (vs complex)',
    debugging: 'Easier (vs harder)',
    monitoring: 'Straightforward (vs complex)'
  },
  
  cost: {
    infrastructure: 'Optimized (vs over-provisioned)',
    operations: 'Lower (vs higher)',
    development: 'Faster (vs slower)'
  }
}
```

**Migration Strategy**:

```typescript
interface MigrationStrategy {
  phases: {
    phase1: {
      name: 'Assessment',
      duration: '2 weeks',
      activities: [
        'Identify stateful components',
        'Analyze dependencies',
        'Plan migration order',
        'Estimate effort'
      ]
    },
    
    phase2: {
      name: 'Infrastructure Setup',
      duration: '2 weeks',
      activities: [
        'Deploy Redis Global Datastore',
        'Configure S3 replication',
        'Set up DynamoDB Global Tables',
        'Test connectivity'
      ]
    },
    
    phase3: {
      name: 'Service Migration',
      duration: '8 weeks',
      activities: [
        'Migrate session management',
        'Externalize caching',
        'Move file storage to S3',
        'Implement idempotency',
        'Refactor background jobs',
        'Update WebSocket handling'
      ]
    },
    
    phase4: {
      name: 'Testing & Validation',
      duration: '2 weeks',
      activities: [
        'Integration testing',
        'Failover testing',
        'Performance testing',
        'Load testing'
      ]
    },
    
    phase5: {
      name: 'Production Rollout',
      duration: '2 weeks',
      activities: [
        'Gradual rollout',
        'Monitor performance',
        'Validate failover',
        'Optimize configuration'
      ]
    }
  },
  
  totalDuration: '16 weeks',
  
  prioritization: {
    high: ['Session management', 'File storage'],
    medium: ['Caching', 'Background jobs'],
    low: ['WebSocket', 'Optimization']
  }
}
```

**優點**：

- ✅ Seamless regional failover (< 5 minutes)
- ✅ Unlimited horizontal scalability
- ✅ Simplified operations
- ✅ Consistent 用戶體驗
- ✅ Automated failover
- ✅ Cost optimization
- ✅ Easier testing 和 deployment
- ✅ 更好的 resource utilization

**缺點**：

- ⚠️ Refactoring effort (16 週)
- ⚠️ External dependencies (Redis, S3, DynamoDB)
- ⚠️ Network latency 用於 state access
- ⚠️ 複雜的ity in distributed state management

**成本**： $30,000 implementation + $15,000/year operational

**風險**： **Low** - Proven architecture pattern

### 選項 2： Hybrid Stateful/Stateless

**描述**： Keep some stateful components, externalize critical state only

**優點**：

- ✅ Lower refactoring effort
- ✅ Faster implementation

**缺點**：

- ❌ Limited regional mobility
- ❌ 複雜的 failover
- ❌ Inconsistent architecture

**成本**： $15,000 implementation

**風險**： **Medium** - Partial solution

### 選項 3： Sticky Sessions with State Replication

**描述**： Use sticky sessions with background state replication

**優點**：

- ✅ Minimal refactoring
- ✅ Familiar pattern

**缺點**：

- ❌ Slow failover
- ❌ Session loss on failure
- ❌ Limited scalability
- ❌ 複雜的 operations

**成本**： $10,000 implementation

**風險**： **High** - Does not solve core problems

## 決策結果

**選擇的選項**： **Comprehensive Stateless Architecture (Option 1)**

### 理由

Fully stateless architecture 提供s optimal regional mobility, scalability, 和 operational simplicity, justifying the refactoring investment.

## 影響分析

### 利害關係人影響

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | High | Signifi可以t refactoring required | Training, documentation, phased approach |
| Operations Team | Medium | New infrastructure to manage | Automation, training, runbooks |
| QA Team | High | Extensive testing required | Test automation, clear test plans |
| Customers | Low | Transparent changes | Gradual rollout, monitoring |
| Management | Medium | Investment approval | ROI analysis, phased approach |

### Impact Radius Assessment

**選擇的影響半徑**： **System**

影響：

- All application services
- Session management
- Caching layer
- File storage
- Background jobs
- WebSocket connections
- Deployment processes

### 風險評估

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| Performance degradation | Medium | High | Performance testing, optimization |
| Migration 複雜的ity | High | Medium | Phased approach, thorough testing |
| External dependency failures | Low | High | Redundancy, fallback mechanisms |
| Data consistency issues | Low | Critical | Strong consistency guarantees, testing |
| Cost overruns | Medium | Medium | 預算 monitoring, phased approach |

**整體風險等級**： **Low**

## 實作計畫

### 第 1 階段： Assessment & Planning （第 1-2 週）

**Tasks**:

- [ ] Audit all services 用於 stateful components
- [ ] Identify dependencies 和 migration order
- [ ] Create detailed migration plan
- [ ] Set up project tracking
- [ ] Allocate resources

**Success Criteria**:

- Complete inventory of stateful components
- Migration plan approved
- Resources allocated

### 第 2 階段： Infrastructure Setup （第 3-4 週）

**Tasks**:

- [ ] Deploy Redis Global Datastore
- [ ] Configure S3 cross-region replication
- [ ] Set up DynamoDB Global Tables
- [ ] Configure networking
- [ ] Test connectivity 和 replication
- [ ] Set up monitoring

**Success Criteria**:

- All infrastructure operational
- Replication working
- Monitoring configured

### 第 3 階段： Session Management Migration （第 5-6 週）

**Tasks**:

- [ ] Implement Spring Session 與 Redis
- [ ] Migrate session data structure
- [ ] Update authentication flow
- [ ] Test session persistence
- [ ] Deploy to staging
- [ ] Gradual production rollout

**Success Criteria**:

- Sessions externalized
- No session loss 期間 failover
- Performance 維持ed

### 第 4 階段： Caching Migration （第 7-8 週）

**Tasks**:

- [ ] Configure 分散式快取
- [ ] Migrate cache keys
- [ ] Update cache access patterns
- [ ] Test cache consistency
- [ ] Deploy to production

**Success Criteria**:

- Distributed caching operational
- Cache hit rates 維持ed
- Cross-region consistency

### Phase 5: File Storage Migration （第 9-10 週）

**Tasks**:

- [ ] Set up S3 buckets 與 replication
- [ ] Migrate existing files
- [ ] Update file upload/download logic
- [ ] Implement pre-signed URLs
- [ ] Test file access
- [ ] Deploy to production

**Success Criteria**:

- All files in S3
- Cross-region replication working
- File access performance acceptable

### Phase 6: Idempotency Implementation （第 11-12 週）

**Tasks**:

- [ ] Implement idempotency framework
- [ ] Add idempotency to critical operations
- [ ] Test duplicate request handling
- [ ] Update API documentation
- [ ] Deploy to production

**Success Criteria**:

- Idempotency working 用於 all critical operations
- No duplicate processing
- API clients updated

### Phase 7: Background Jobs Migration （第 13-14 週）

**Tasks**:

- [ ] Migrate to EventBridge scheduling
- [ ] Implement distributed locks
- [ ] Update job implementations
- [ ] Test job execution
- [ ] Deploy to production

**Success Criteria**:

- Jobs running stateless
- No duplicate execution
- Proper failover

### Phase 8: Testing & Validation （第 15-16 週）

**Tasks**:

- [ ] Integration testing
- [ ] Failover testing
- [ ] Performance testing
- [ ] Load testing
- [ ] Security testing
- [ ] Documentation updates

**Success Criteria**:

- All tests passing
- Failover < 5 minutes
- Performance 維持ed
- Documentation complete

### 回滾策略

**觸發條件**：

- Critical performance degradation
- Data consistency issues
- Failover failures

**回滾步驟**：

1. Revert to previous version
2. Restore stateful components
3. Investigate issues
4. Fix 和 retry

**回滾時間**： < 4 hours

## 監控和成功標準

### 成功指標

| Metric | Target | Measurement |
|--------|--------|-------------|
| Failover Time | < 5 minutes | Drill results |
| Session Persistence | 100% | Monitoring |
| Cache Hit Rate | > 80% | Redis metrics |
| File Access Latency | < 200ms | CloudWatch |
| Idempotency Success | 100% | Application logs |
| System Availability | > 99.9% | CloudWatch |

### Review Schedule

- **Weekly**: Progress review 期間 implementation
- **Monthly**: Performance 和 cost review
- **Quarterly**: Architecture review

## 後果

### 正面後果

- ✅ **Seamless Failover**: < 5 minute regional failover
- ✅ **Unlimited Scalability**: Horizontal scaling 沒有 limits
- ✅ **Simplified Operations**: Easier deployment 和 management
- ✅ **Consistent Experience**: No session loss 期間 failover
- ✅ **Cost Optimization**: 更好的 resource utilization
- ✅ **Easier Testing**: Simplified testing 和 debugging
- ✅ **Future-Proof**: Foundation 用於 further evolution

### 負面後果

- ⚠️ **Refactoring Effort**: 16 週 of development
- ⚠️ **External Dependencies**: Reliance on Redis, S3, DynamoDB
- ⚠️ **Network Latency**: Slight increase 用於 state access
- ⚠️ **Operational Cost**: $15K/year 用於 external services
- ⚠️ **複雜的ity**: Distributed state management 複雜的ity

### 技術債務

**已識別債務**：

1. Some legacy components still stateful
2. Manual state migration scripts
3. Basic monitoring
4. Limited automation

**債務償還計畫**：

- **Q2 2026**: Complete legacy migration
- **Q3 2026**: Advanced monitoring
- **Q4 2026**: Full automation

## 相關決策

- [ADR-037: Active-Active Multi-Region Architecture](037-active-active-multi-region-architecture.md)
- [ADR-038: Cross-Region Data Replication Strategy](038-cross-region-data-replication-strategy.md)
- [ADR-040: Network Partition Handling Strategy](040-network-partition-handling-strategy.md)
- [ADR-046: Third Region Disaster Recovery](046-third-region-disaster-recovery-singapore-seoul.md)

---

**文檔狀態**： ✅ Accepted  
**上次審查**： 2025-10-25  
**下次審查**： 2026-01-25 （每季）

## 備註

### Stateless vs Stateful Comparison

| Aspect | Stateful | Stateless |
|--------|----------|-----------|
| Failover Time | 30+ minutes | < 5 minutes |
| Session Loss | Yes | No |
| Scalability | Limited | Unlimited |
| Load Balancing | Sticky sessions | Any instance |
| Deployment | 複雜的 | 簡單的 |
| Testing | Difficult | Easy |
| Cost | Higher | Lower |

### External State Storage Comparison

| Service | Use Case | Latency | Cost | Availability |
|---------|----------|---------|------|--------------|
| Redis | Sessions, Cache | < 1ms | Medium | 99.99% |
| DynamoDB | Locks, Connections | < 10ms | Low | 99.99% |
| S3 | Files, Assets | < 50ms | Very Low | 99.99% |

### Best Practices

**Session Management**:

- Use short TTL (24 hours)
- Compress session data
- Encrypt sensitive data
- Monitor session count

**Caching**:

- Use appropriate TTL
- Implement cache warming
- Monitor hit rates
- 處理 cache failures gracefully

**File Storage**:

- Use pre-signed URLs
- Implement CDN 用於 public files
- Monitor storage costs
- Implement lifecycle policies

**Idempotency**:

- Use UUID v4 用於 keys
- Store 用於 24 hours
- Return same result 用於 duplicate requests
- Log duplicate attempts

### Migration Checklist

- [ ] All services stateless
- [ ] Sessions externalized
- [ ] Caching distributed
- [ ] Files in S3
- [ ] Idempotency implemented
- [ ] Background jobs stateless
- [ ] WebSockets stateless
- [ ] Failover tested
- [ ] Performance validated
- [ ] Documentation updated
