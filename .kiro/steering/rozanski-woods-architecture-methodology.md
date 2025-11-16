---
inclusion: always
---

# Rozanski & Woods 架構方法論轉向規則

## 強制性架構視點檢查

> **🔗 相關標準**:
>
> - [Development Standards](development-standards.md) - 基本開發和架構約束
> - [Domain Events](domain-events.md) - DDD 事件架構實作
> - [Security Standards](security-standards.md) - 安全架構要求
> - [Performance Standards](performance-standards.md) - 效能架構要求

### 每個新功能必須完成以下視點檢查

#### Functional Viewpoint

- [ ] Aggregate 邊界清晰定義
- [ ] Domain service 職責明確
- [ ] Use case 實作遵循 DDD tactical patterns

#### Information Viewpoint

- [ ] Domain event 設計完整
- [ ] 資料一致性策略定義
- [ ] Event sourcing 考量已處理

#### Concurrency Viewpoint

- [ ] 非同步處理策略已文件化
- [ ] Transaction 邊界清晰定義
- [ ] 並行衝突處理機制

#### Development Viewpoint

- [ ] 模組依賴符合 hexagonal architecture
- [ ] 測試策略涵蓋所有層級
- [ ] Build scripts 已更新

#### 部署 Viewpoint

- [ ] CDK 基礎設施已更新
- [ ] 環境設定變更已記錄
- [ ] 部署策略影響已評估

#### Operational Viewpoint

- [ ] Monitoring metrics 已定義
- [ ] Log 結構已設計
- [ ] 失敗處理程序

#### Context Viewpoint

- [ ] 外部系統整合邊界已定義
- [ ] Stakeholder 互動模型已文件化
- [ ] 系統邊界和外部依賴已對應
- [ ] 整合協定和資料交換格式已指定
- [ ] 外部服務合約和 SLAs 已定義
- [ ] 組織和法規約束已識別

## Quality Attribute Scenario 要求

### 每個 user story 必須包含至少一個 quality attribute scenario

#### Scenario 格式：Source → Stimulus → Environment → Artifact → Response → Response Measure

### 依 Quality Attribute 的 QAS Templates

#### 效能 Scenarios

```text
Template:
Source: [User/System/Load Generator]
Stimulus: [具體請求/操作及負載特性]
Environment: [Normal/Peak/Stress 條件]
Artifact: [系統元件/服務]
Response: [系統處理請求]
Response Measure: [Response time ≤ X ms, Throughput ≥ Y req/s, CPU ≤ Z%]

Example:
Source: Web user
Stimulus: Submit order with 3 items during peak shopping hours
Environment: Normal operation with 1000 concurrent users
Artifact: Order processing service
Response: Order is processed and confirmation is returned
Response Measure: Response time ≤ 2000ms, Success rate ≥ 99.5%
```

#### 安全性 Scenarios

```text
Template:
Source: [Attacker/Malicious user/System]
Stimulus: [攻擊類型/未授權存取嘗試]
Environment: [Network/System 狀態]
Artifact: [受攻擊的系統元件]
Response: [系統安全回應]
Response Measure: [Detection time, Prevention success rate, Recovery time]

Example:
Source: Malicious user
Stimulus: Attempts SQL injection on customer search endpoint
Environment: Production system with normal load
Artifact: Customer API service
Response: System detects and blocks the attack, logs the incident
Response Measure: Attack blocked within 100ms, Incident logged, No data exposure
```

#### Availability Scenarios

```text
Template:
Source: [失敗來源]
Stimulus: [失敗類型]
Environment: [失敗期間的系統狀態]
Artifact: [受影響的元件]
Response: [系統恢復動作]
Response Measure: [RTO ≤ X minutes, RPO ≤ Y minutes, Availability ≥ Z%]

Example:
Source: Database server
Stimulus: Primary database server fails
Environment: Production system during business hours
Artifact: Customer data service
Response: System fails over to secondary database
Response Measure: RTO ≤ 5 minutes, RPO ≤ 1 minute, Availability ≥ 99.9%
```

#### Scalability Scenarios

```text
Template:
Source: [負載來源]
Stimulus: [負載增加模式]
Environment: [目前系統容量]
Artifact: [系統元件]
Response: [擴展動作]
Response Measure: [Capacity increase, Performance maintenance, Cost impact]

Example:
Source: Marketing campaign
Stimulus: User load increases from 100 to 1000 concurrent users over 1 hour
Environment: Current system running at 60% capacity
Artifact: Web application tier
Response: System automatically scales out additional instances
Response Measure: Maintains response time ≤ 2s, Scales to handle 1000 users, Cost increase ≤ 50%
```

#### Usability Scenarios

```text
Template:
Source: [使用者類型]
Stimulus: [使用者任務/目標]
Environment: [使用情境]
Artifact: [使用者介面/系統]
Response: [系統提供介面/回饋]
Response Measure: [Task completion time, Error rate, User satisfaction]

Example:
Source: New customer
Stimulus: Wants to complete first purchase
Environment: Using mobile device during commute
Artifact: Mobile checkout interface
Response: System guides user through streamlined checkout process
Response Measure: Checkout completion ≤ 3 minutes, Error rate ≤ 2%, Abandonment rate ≤ 10%
```

### 量化指標要求

#### 效能 Metrics

- **Response Time**: API endpoints ≤ 2s (95th percentile)
- **Throughput**: System handles ≥ 1000 req/s peak load
- **Resource Usage**: CPU ≤ 70%, Memory ≤ 80%, Disk I/O ≤ 80%
- **Database**: Query response ≤ 100ms (95th percentile)

#### 安全性 Metrics

- **Authentication**: Login success rate ≥ 99.9%
- **Authorization**: Access control violations = 0
- **Encryption**: All data encrypted in transit (TLS 1.3) and at rest (AES-256)
- **Vulnerability**: Zero critical/high severity vulnerabilities in production

#### Availability Metrics

- **Uptime**: System availability ≥ 99.9% (8.76 hours downtime/year)
- **RTO**: Recovery Time Objective ≤ 5 minutes
- **RPO**: Recovery Point Objective ≤ 1 minute
- **MTTR**: Mean Time To Recovery ≤ 15 minutes

#### Scalability Metrics

- **Horizontal Scaling**: Auto-scale from 2 to 20 instances
- **Load Handling**: Support 10x traffic increase within 10 minutes
- **Database**: Read replicas scale automatically based on load
- **Storage**: Auto-scaling storage with 99.999% durability

## 架構合規規則

### 強制性 ArchUnit Rules

```java
// Domain layer dependency restrictions
@ArchTest
static final ArchRule domainLayerRules = classes()
    .that().resideInAPackage("..domain..")
    .should().onlyDependOnClassesThat()
    .resideInAnyPackage("..domain..", "java..", "org.springframework..");

// Aggregate root rules
@ArchTest
static final ArchRule aggregateRootRules = classes()
    .that().areAnnotatedWith(AggregateRoot.class)
    .should().implement(AggregateRootInterface.class);

// Event handler rules
@ArchTest
static final ArchRule eventHandlerRules = classes()
    .that().areAnnotatedWith(Component.class)
    .and().haveSimpleNameEndingWith("EventHandler")
    .should().beAnnotatedWith(TransactionalEventListener.class);

// Value object rules
@ArchTest
static final ArchRule valueObjectRules = classes()
    .that().areAnnotatedWith(ValueObject.class)
    .should().beRecords();
```

## ADR 必要內容

### ADR Template 結構

```markdown
# ADR-{NUMBER}: {TITLE}

## Status
[Proposed | Accepted | Deprecated | Superseded by ADR-XXX]

## Context
### Problem Statement
[描述需要解決的問題]

### Business Context
[業務驅動因素、約束和需求]

### Technical Context
[目前架構、技術約束和依賴]

## Decision Drivers

- [Driver 1: e.g., Performance requirements]
- [Driver 2: e.g., Cost constraints]
- [Driver 3: e.g., Team expertise]
- [Driver 4: e.g., Time to market]

## Considered Options
### Option 1: [Name]
**Pros:**

- [Advantage 1]
- [Advantage 2]

**Cons:**

- [Disadvantage 1]
- [Disadvantage 2]

**Cost:** [Implementation cost, maintenance cost]
**Risk:** [High/Medium/Low] - [Risk description]

### Option 2: [Name]
[Same structure as Option 1]

### Option 3: [Name]
[Same structure as Option 1]

## Decision Outcome
**Chosen Option:** [Selected option with rationale]

**Rationale:**
[詳細說明為何選擇此方案]

## Impact Analysis

### Stakeholder Impact
| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | High | Need to learn new technology | Training plan, documentation |
| Operations Team | Medium | New monitoring requirements | Update runbooks, training |
| End Users | Low | No visible changes | N/A |
| Business | Medium | Cost increase | Budget approval obtained |

### Impact Radius Assessment

- **Local**: [Changes within single component/service]
- **Bounded Context**: [Changes across related services]
- **System**: [Changes across multiple bounded contexts]
- **Enterprise**: [Changes affecting multiple systems]

**Selected Impact Radius:** [Local/Bounded Context/System/Enterprise]

### Risk Assessment
| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|-------------------|
| Technology learning curve | Medium | High | Provide training, pair programming |
| Performance degradation | Low | High | Load testing, performance monitoring |
| Integration complexity | High | Medium | Proof of concept, incremental rollout |

**Overall Risk Level:** [High/Medium/Low]

## 實現 Plan

### Migration Path
**Phase 1: Preparation (Week 1-2)**

- [ ] Team training
- [ ] Environment setup
- [ ] Proof of concept

**Phase 2: Implementation (Week 3-6)**

- [ ] Core functionality implementation
- [ ] Unit and integration tests
- [ ] Documentation updates

**Phase 3: Deployment (Week 7-8)**

- [ ] Staging environment deployment
- [ ] Performance testing
- [ ] Production deployment
- [ ] Monitoring setup

### Rollback Strategy
**Trigger Conditions:**

- Performance degradation > 20%
- Error rate > 1%
- Critical functionality unavailable > 5 minutes

**Rollback Steps:**

1. [Immediate action - e.g., traffic routing]
2. [Database rollback if needed]
3. [Service rollback procedure]
4. [Verification steps]

**Rollback Time:** [Target time to complete rollback]

## 監控 and Success Criteria

### Success Metrics

- [Metric 1: e.g., Response time < 2s]
- [Metric 2: e.g., Error rate < 0.1%]
- [Metric 3: e.g., Cost reduction of 20%]

### 監控 Plan

- [Dashboard/Alert 1]
- [Dashboard/Alert 2]
- [Review schedule]

## Consequences

### Positive Consequences

- [Benefit 1]
- [Benefit 2]

### Negative Consequences

- [Trade-off 1]
- [Trade-off 2]

### Technical Debt

- [Any technical debt introduced]
- [Plan to address technical debt]

## Related Decisions

- [ADR-XXX: Related decision]
- [Link to relevant documentation]

## Notes
[Any additional notes, assumptions, or constraints]
```

### ADR Quality Checklist

在接受 ADR 之前，確保：

- [ ] Problem statement 清晰且具體
- [ ] 至少考慮了 3 個選項
- [ ] 每個選項包含優缺點、成本和風險
- [ ] Decision rationale 已充分文件化
- [ ] Stakeholder impact analysis 已完成
- [ ] Risk assessment 包含緩解策略
- [ ] Implementation plan 有清晰的階段和時間表
- [ ] Rollback strategy 詳細且可測試
- [ ] Success criteria 可衡量
- [ ] Monitoring plan 具體明確

### ADR Review 流程

1. **Author** 建立 ADR 並設為 "Proposed" 狀態
2. **Architecture Team** 審查技術面向
3. **Stakeholders** 審查影響分析
4. **Team Lead** 核准實作計畫
5. **ADR** 狀態變更為 "Accepted"
6. **Implementation** 依計畫開始
7. **Review** 實作後的成功標準

## Observability 要求

### 新功能強制要求

- 每個 aggregate root 必須有對應的業務指標
- 每個 use case 必須有執行追蹤和效能指標
- 每個 domain event 必須有發布和處理指標
- 關鍵路徑必須有監控和告警

### 監控 實作標準

#### Business Metrics (每個 Aggregate Root 必要)

```java
@Component
public class CustomerMetrics {
    private final MeterRegistry meterRegistry;
    private final Counter customersCreated;
    private final Timer customerCreationTime;
    private final Gauge activeCustomers;

    public CustomerMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.customersCreated = Counter.builder("customers.created")
            .description("Total number of customers created")
            .register(meterRegistry);
        this.customerCreationTime = Timer.builder("customers.creation.time")
            .description("Time taken to create a customer")
            .register(meterRegistry);
        this.activeCustomers = Gauge.builder("customers.active")
            .description("Number of active customers")
            .register(meterRegistry, this, CustomerMetrics::getActiveCustomerCount);
    }

    public void recordCustomerCreated() {
        customersCreated.increment();
    }

    public Timer.Sample startCustomerCreation() {
        return Timer.start(meterRegistry);
    }

    private double getActiveCustomerCount() {
        // Implementation to get active customer count
        return customerRepository.countActiveCustomers();
    }
}
```

#### Use Case Tracing (每個 Application Service 必要)

```java
@Service
@Transactional
public class CustomerApplicationService {

    @TraceAsync
    @Timed(name = "customer.creation", description = "Time taken to create customer")
    public void createCustomer(CreateCustomerCommand command) {
        Span span = tracer.nextSpan()
            .name("create-customer")
            .tag("customer.type", command.getType())
            .tag("customer.source", command.getSource())
            .start();

        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            // Business logic implementation
            Customer customer = customerFactory.create(command);
            customerRepository.save(customer);

            // Add business context to trace
            span.tag("customer.id", customer.getId())
                .tag("customer.segment", customer.getSegment())
                .event("customer.created");

            domainEventService.publishEventsFromAggregate(customer);

        } catch (Exception e) {
            span.tag("error", e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}
```

#### Domain Event Metrics (每個 Event Type 必要)

```java
@Component
public class DomainEventMetrics {

    @EventListener
    public void recordEventPublished(DomainEventPublishedEvent event) {
        Counter.builder("domain.events.published")
            .tag("event.type", event.getEventType())
            .tag("aggregate.type", event.getAggregateType())
            .register(meterRegistry)
            .increment();
    }

    @EventListener
    public void recordEventProcessed(DomainEventProcessedEvent event) {
        Timer.builder("domain.events.processing.time")
            .tag("event.type", event.getEventType())
            .tag("handler", event.getHandlerName())
            .register(meterRegistry)
            .record(event.getProcessingTime(), TimeUnit.MILLISECONDS);
    }

    @EventListener
    public void recordEventFailed(DomainEventFailedEvent event) {
        Counter.builder("domain.events.failed")
            .tag("event.type", event.getEventType())
            .tag("error.type", event.getErrorType())
            .register(meterRegistry)
            .increment();
    }
}
```

### Logging 結構標準

#### Structured Logging Format

```java
// Use consistent structured logging
public class StructuredLogger {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public void logBusinessEvent(String event, Object... keyValues) {
        logger.info("Business event: {}", event,
            StructuredArguments.kv("timestamp", Instant.now()),
            StructuredArguments.kv("traceId", getCurrentTraceId()),
            StructuredArguments.kv("userId", getCurrentUserId()),
            keyValues);
    }

    public void logError(String message, Exception e, Object... keyValues) {
        logger.error("Error occurred: {}", message,
            StructuredArguments.kv("timestamp", Instant.now()),
            StructuredArguments.kv("traceId", getCurrentTraceId()),
            StructuredArguments.kv("errorType", e.getClass().getSimpleName()),
            keyValues,
            e);
    }
}

// Usage example
structuredLogger.logBusinessEvent("Customer created",
    kv("customerId", customer.getId()),
    kv("customerType", customer.getType()),
    kv("registrationSource", "web"));
```

#### Log Correlation 規範

```java
@Component
public class TraceContextFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        String traceId = extractOrGenerateTraceId(request);
        String sessionId = extractSessionId(request);

        try (MDCCloseable mdcCloseable = MDC.putCloseable("traceId", traceId)) {
            MDC.put("sessionId", sessionId);
            MDC.put("userId", getCurrentUserId());

            chain.doFilter(request, response);
        }
    }
}
```

### Alert 設定 規範

#### Critical Path Alerts (必要)

```yaml
# Prometheus Alert Rules
groups:

  - name: customer-service-alerts

    rules:

      - alert: HighErrorRate

        expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.01
        for: 2m
        labels:
          severity: critical
          service: customer-service
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value }} errors per second"

      - alert: HighResponseTime

        expr: histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m])) > 2
        for: 5m
        labels:
          severity: warning
          service: customer-service
        annotations:
          summary: "High response time detected"
          description: "95th percentile response time is {{ $value }} seconds"

      - alert: DatabaseConnectionPoolExhausted

        expr: hikaricp_connections_active / hikaricp_connections_max > 0.9
        for: 1m
        labels:
          severity: critical
          service: customer-service
        annotations:
          summary: "Database connection pool nearly exhausted"
          description: "Connection pool usage is {{ $value }}%"
```

#### Business Metrics Alerts

```yaml

  - name: business-metrics-alerts

    rules:

      - alert: CustomerCreationRateDropped

        expr: rate(customers_created_total[10m]) < 0.1
        for: 5m
        labels:
          severity: warning
          team: product
        annotations:
          summary: "Customer creation rate has dropped significantly"

      - alert: HighCustomerChurnRate

        expr: rate(customers_churned_total[1h]) / rate(customers_created_total[1h]) > 0.1
        for: 10m
        labels:
          severity: critical
          team: product
        annotations:
          summary: "Customer churn rate is unusually high"
```

### Dashboard 要求

#### Technical Dashboard (每個服務必要)

- **Response Time**: 95th percentile over time
- **Error Rate**: 4xx and 5xx errors per minute
- **Throughput**: Requests per second
- **Resource Usage**: CPU, Memory, Database connections
- **Dependency Health**: External service response times

#### Business Dashboard (每個 bounded context 必要)

- **Key Business Metrics**: Orders, customers, revenue
- **Conversion Rates**: Funnel analysis
- **User Behavior**: Page views, session duration
- **Business Process Health**: Success rates, completion times

### Health Check 規範

#### Application Health Checks

```java
@Component
public class CustomerServiceHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        try {
            // Check database connectivity
            customerRepository.count();

            // Check external dependencies
            paymentService.healthCheck();

            // Check business logic health
            validateBusinessRules();

            return Health.up()
                .withDetail("database", "UP")
                .withDetail("payment-service", "UP")
                .withDetail("business-rules", "VALID")
                .build();

        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

#### Infrastructure Health Checks

```yaml
# Kubernetes Liveness and Readiness Probes
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 5
  timeoutSeconds: 3
  failureThreshold: 3
```

## Four Perspectives Checklist

### 安全性 Perspective

- [ ] API endpoints pass CDK Nag checks
- [ ] 敏感資料在儲存和傳輸中加密
- [ ] Authentication 和 authorization 機制
- [ ] Security event logging 和 monitoring

### 效能 & Scalability Perspective

- [ ] 關鍵路徑效能基準 (< 2s)
- [ ] Database query 最佳化和索引策略
- [ ] Caching 策略實作
- [ ] Horizontal scaling 能力驗證

### Availability & Resilience Perspective

- [ ] Health check endpoints 已實作
- [ ] 失敗恢復和重試機制
- [ ] Circuit breaker pattern 實作
- [ ] Disaster recovery 計畫和測試

### Evolution Perspective

- [ ] Interface 向後相容性保證
- [ ] Version 管理策略實作
- [ ] 模組化和鬆散耦合設計
- [ ] Refactoring 安全性保證（測試覆蓋率）

## Concurrency Strategy 要求

### 非同步處理設計必須指定

- Event 處理順序依賴
- Transaction 邊界和一致性保證
- 並行衝突偵測和處理機制
- Deadlock 預防和偵測策略

## 強制性 Resilience 模式

### 外部服務呼叫必須實作

- Circuit breaker pattern
- Retry 機制（最多 3 次嘗試，exponential backoff）
- Fallback 策略
- Dead letter queue 處理

### 關鍵業務流程必須具備

- 失敗恢復時間測試
- Monitoring 和 alerting 設定
- Operations manual 更新

## Technology Evolution 規範

### 新技術引入必須滿足

- [ ] 技術成熟度達到 "Growth" 階段或以上
- [ ] 完整文件和社群支援
- [ ] 團隊學習和維護能力
- [ ] Migration 風險可控且有 rollback plan

### Version 升級要求

- Critical dependency 升級必須有自動化測試覆蓋
- Major version 升級必須在測試環境驗證
- Legacy technology 淘汰必須有明確時間表

## Compliance 監控 Metrics

- Viewpoint 覆蓋率：100%
- Quality attribute scenario 覆蓋率：100%
- ArchUnit test 通過率：100%
- Architecture debt 趨勢：持續下降
