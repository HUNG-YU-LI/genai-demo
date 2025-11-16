---
adr_number: 031
title: "Inter-Service Communication Protocol"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [2, 3, 5, 9, 25, 30]
affected_viewpoints: ["functional", "concurrency", "deployment"]
affected_perspectives: ["performance", "availability", "evolution"]
decision_makers: ["Architecture Team", "Backend Team"]
---

# ADR-031: Inter-Service Communication Protocol

## 狀態

**Status**: Accepted

**Date**: 2025-10-25

**Decision Makers**: Architecture Team, Backend Team

## 上下文

### 問題陳述

企業電子商務平台由13 bounded contexts that may be deployed as separate microservices. We need to decide on inter-service communication protocols to 啟用:組成

- Synchronous request-response communication
- Asynchronous event-driven communication
- Service-to-service data exchange
- Cross-context business process coordination

This decision impacts:

- System performance 和 latency
- Service coupling 和 independence
- Data consistency 和 eventual consistency
- Fault tolerance 和 resilience
- Development 複雜的ity
- Operational overhead

### 業務上下文

**業務驅動因素**：

- Fast 回應時間 用於 user-facing operations
- Reliable order processing 跨 multiple services
- Scalable architecture 支援ing business growth
- Loose coupling between bounded contexts
- 支援 用於 eventual consistency where appropriate
- 啟用 independent service deployment

**Business Constraints**:

- Order processing 需要coordination 跨 multiple services (Order, Inventory, Payment, Shipping)
- Some operations 需要immediate consistency (payment processing)
- Some operations 可以 tolerate eventual consistency (email notifications, analytics)
- Peak traffic: 10,000 requests/second
- Response time requirement: < 2 seconds 用於 user-facing operations
- 99.9% availability requirement

**Business Requirements**:

- 支援 synchronous operations 用於 immediate responses
- 支援 asynchronous operations 用於 long-running processes
- 啟用 event-driven architecture 用於 loose coupling
- 維持 資料一致性 跨 services
- 支援 service independence 和 scalability

### 技術上下文

**Current Architecture**:

- Backend: Spring Boot microservices (13 bounded contexts)
- Architecture: Hexagonal Architecture 與 DDD (ADR-002)
- Event System: Domain Events (ADR-003)
- Message Broker: Apache Kafka (ADR-005)
- API Design: RESTful APIs (ADR-009)
- Transaction Management: Saga Pattern (ADR-025)
- API Gateway: Kong Gateway (ADR-030)

**Technical Constraints**:

- 必須 支援 Spring Boot ecosystem
- 必須 integrate 與 existing Kafka infrastructure
- 必須 work 與 Kong API Gateway
- 必須 支援 distributed transactions (Saga pattern)
- 必須 提供 low latency 用於 synchronous calls
- 必須 ensure message delivery 用於 asynchronous calls

**Dependencies**:

- ADR-002: Hexagonal Architecture (service boundaries)
- ADR-003: Domain Events (event-driven communication)
- ADR-005: Apache Kafka (message broker)
- ADR-009: RESTful API Design (API standards)
- ADR-025: Saga Pattern (distributed transactions)
- ADR-030: API Gateway Pattern (external API access)

## 決策驅動因素

- **Performance**: Low latency 用於 synchronous operations
- **Reliability**: Guaranteed message delivery 用於 asynchronous operations
- **Scalability**: 支援 high throughput 和 horizontal scaling
- **Coupling**: Minimize coupling between services
- **Consistency**: 支援 both strong 和 eventual consistency
- **複雜的ity**: Balance between functionality 和 simplicity
- **Developer Experience**: Familiar technologies 和 patterns

## 考慮的選項

### 選項 1： REST for Sync + Kafka for Async (Hybrid Approach)

**描述**：
Use RESTful HTTP APIs 用於 synchronous request-response communication 和 Apache Kafka 用於 asynchronous event-driven communication.

**Pros** ✅:

- **Clear Separation**: Synchronous vs asynchronous communication clearly separated
- **Familiar Technologies**: REST 和 Kafka are well-known 和 widely adopted
- **Flexibility**: Choose appropriate protocol 用於 each 使用案例
- **Existing Infrastructure**: Leverage existing REST APIs 和 Kafka setup
- **Tooling**: 優秀的 tooling 和 ecosystem 支援
- **Debugging**: 容易debug 和 monitor both protocols
- **Spring Integration**: Native Spring 支援 用於 both REST 和 Kafka
- **Event-Driven**: Kafka 啟用s true event-driven architecture

**Cons** ❌:

- **Dual Protocol**: Need to manage two different communication protocols
- **複雜的ity**: More 複雜的 than single protocol approach
- **Latency**: REST has higher latency than gRPC 用於 sync calls
- **Payload Size**: REST JSON payloads 大型的r than binary protocols
- **Learning Curve**: Team needs expertise in both REST 和 Kafka

**成本**：

- **Implementation Cost**: 2 person-weeks (configure REST clients 和 Kafka producers/consumers)
- **Infrastructure Cost**: $500/month (Kafka MSK cluster already exists)
- **Maintenance Cost**: 1 person-day/month
- **Total Cost of Ownership (3 年)**: ~$25,000

**風險**： Low

**Risk Description**: Proven technologies 與 extensive production usage

**Effort**: Low

**Effort Description**: Leverage existing REST 和 Kafka infrastructure

### 選項 2： gRPC for Sync + Kafka for Async

**描述**：
Use gRPC 用於 synchronous request-response communication 和 Apache Kafka 用於 asynchronous event-driven communication.

**Pros** ✅:

- **Performance**: gRPC 提供s lower latency 和 higher throughput than REST
- **Type Safety**: Protocol Buffers 提供 strong typing 和 schema validation
- **Efficient**: Binary serialization 降低s payload size
- **Streaming**: Built-in 支援 用於 bidirectional streaming
- **Code Generation**: Automatic client/server code generation
- **HTTP/2**: Multiplexing, header compression, server push
- **Event-Driven**: Kafka 啟用s event-driven architecture

**Cons** ❌:

- **Learning Curve**: Team needs to learn gRPC 和 Protocol Buffers
- **Debugging**: Binary protocol harder to debug than JSON
- **Browser 支援**: Limited browser 支援 (需要gRPC-Web)
- **Tooling**: Less mature tooling compared to REST
- **API Gateway**: Kong Gateway has limited gRPC 支援
- **Migration**: Requires migrating existing REST APIs to gRPC
- **複雜的ity**: More 複雜的 than REST 用於 簡單的 使用案例s

**成本**：

- **Implementation Cost**: 6 person-weeks (migrate to gRPC, train team)
- **Infrastructure Cost**: $500/month (Kafka MSK)
- **Maintenance Cost**: 1.5 person-days/month
- **Total Cost of Ownership (3 年)**: ~$40,000

**風險**： Medium

**Risk Description**: Team unfamiliar 與 gRPC, migration 複雜的ity

**Effort**: High

**Effort Description**: Signifi可以t migration effort, team training required

### 選項 3： Kafka for All Communication (Event-Driven Only)

**描述**：
Use Apache Kafka 用於 all inter-service communication, including both synchronous 和 asynchronous patterns.

**Pros** ✅:

- **Single Protocol**: Unified communication protocol
- **Event-Driven**: True event-driven architecture
- **Decoupling**: Maximum decoupling between services
- **Scalability**: Kafka scales horizontally
- **Durability**: Messages persisted 和 replayed
- **Audit Trail**: Complete event history 用於 debugging

**Cons** ❌:

- **Latency**: Higher latency 用於 request-response patterns
- **複雜的ity**: Request-response over Kafka is 複雜的
- **Correlation**: Need to correlate requests 和 responses
- **Timeout Handling**: 複雜的 timeout 和 error handling
- **Debugging**: 更難debug than direct HTTP calls
- **Not Suitable**: Poor fit 用於 synchronous operations
- **Learning Curve**: Team needs deep Kafka expertise

**成本**：

- **Implementation Cost**: 8 person-weeks (implement request-response over Kafka)
- **Infrastructure Cost**: $500/month (Kafka MSK)
- **Maintenance Cost**: 2 person-days/month
- **Total Cost of Ownership (3 年)**: ~$50,000

**風險**： High

**Risk Description**: 複雜的 implementation, not suitable 用於 synchronous operations

**Effort**: High

**Effort Description**: Signifi可以t implementation 複雜的ity

## 決策結果

**選擇的選項**： Option 1 - REST for Sync + Kafka for Async (Hybrid Approach)

**Rationale**:
We chose a hybrid approach using REST 用於 synchronous communication 和 Kafka 用於 asynchronous communication. This decision balances performance, simplicity, 和 flexibility:

1. **Clear Use Case Separation**: REST 用於 immediate request-response (query customer, check inventory) 和 Kafka 用於 event-driven workflows (order processing, notifications). Each protocol optimized 用於 its 使用案例.

2. **Leverage Existing Infrastructure**: We already have RESTful APIs (ADR-009) 和 Kafka infrastructure (ADR-005). No need to migrate 或 introduce new technologies.

3. **Team Familiarity**: Team has strong expertise in REST 和 Kafka. No learning curve 用於 gRPC 或 複雜的 Kafka request-response patterns.

4. **Proven Pattern**: REST + Kafka is a proven pattern used 透過 major e-commerce platforms (Amazon, eBay, Shopify). Well-documented best practices 和 troubleshooting guides.

5. **Flexibility**: Choose appropriate protocol 用於 each 使用案例. Synchronous operations (get customer profile) use REST. Asynchronous workflows (order processing) use Kafka events.

6. **Debugging 和 Monitoring**: REST calls easy to debug 與 HTTP tools. Kafka messages easy to inspect 與 Kafka tools. Both integrate well 與 existing observability stack (ADR-008).

7. **API Gateway Integration**: Kong Gateway (ADR-030) has 優秀的 REST 支援. Frontend applications continue using REST APIs through gateway.

8. **Gradual Evolution**: 可以 introduce gRPC 用於 specific high-performance 使用案例s in the future 沒有 major architectural changes.

**Communication Pattern Guidelines**:

**Use REST (Synchronous) When**:

- Immediate response required (query operations)
- 簡單的 request-response pattern
- External API calls (third-party integrations)
- Frontend-to-backend communication
- Low latency requirement (< 100ms)
- Examples: Get customer profile, check product availability, validate coupon

**Use Kafka (Asynchronous) When**:

- Event notification (something happened)
- Long-running processes (order fulfillment)
- Cross-context workflows (saga pattern)
- Eventual consistency acceptable
- High throughput required
- Examples: Order submitted, payment processed, inventory updated, email sent

## 影響分析

### 利害關係人影響

| Stakeholder | Impact Level | Description | Mitigation Strategy |
|-------------|--------------|-------------|-------------------|
| Backend Team | Medium | Need to choose appropriate protocol 用於 each 使用案例 | Communication pattern guidelines, code examples |
| Frontend Team | Low | Continue using REST APIs through Kong Gateway | No changes required |
| DevOps Team | Low | Monitor both REST 和 Kafka communication | Unified observability dashboard |
| QA Team | Medium | Test both synchronous 和 asynchronous flows | Testing guidelines 用於 both protocols |

### Impact Radius Assessment

**選擇的影響半徑**： System

**Impact Description**:

- **System**: Communication patterns affect all services
  - All services implement REST endpoints 用於 synchronous operations
  - All services publish/consume Kafka events 用於 asynchronous operations
  - All services follow communication pattern guidelines
  - All services integrate 與 Kong Gateway 用於 external access

### Affected Components

- **All Backend Services**: Implement both REST 和 Kafka communication
- **Kong Gateway**: Routes REST API calls
- **Kafka Cluster**: 處理s asynchronous event communication
- **Observability Stack**: Monitors both REST 和 Kafka metrics
- **Testing Framework**: Tests both synchronous 和 asynchronous flows

### 風險評估

| Risk | Probability | Impact | Mitigation Strategy | Owner |
|------|-------------|--------|-------------------|-------|
| Protocol misuse | Medium | Medium | Clear guidelines, code reviews | Architecture Team |
| REST performance issues | Low | Medium | Caching, optimization | Backend Team |
| Kafka message loss | Low | High | Kafka replication, monitoring | DevOps Team |
| Debugging 複雜的ity | Medium | Low | Unified observability, tracing | DevOps Team |
| Inconsistent patterns | Medium | Medium | Code templates, reviews | Architecture Team |

**整體風險等級**： Low

**Risk Mitigation Plan**:

- 提供 clear communication pattern guidelines
- Create code templates 用於 REST clients 和 Kafka producers/consumers
- Implement distributed tracing 跨 REST 和 Kafka
- Regular architecture reviews to ensure pattern compliance
- Automated tests 用於 both synchronous 和 asynchronous flows

## 實作計畫

### 第 1 階段： Communication Pattern Guidelines (Timeline: Week 1)

**Objectives**:

- Define when to use REST vs Kafka
- Create communication pattern guidelines
- Document best practices

**Tasks**:

- [ ] Document REST usage guidelines (synchronous operations)
- [ ] Document Kafka usage guidelines (asynchronous operations)
- [ ] Create decision tree 用於 protocol selection
- [ ] Define service-to-service REST API standards
- [ ] Define Kafka event naming conventions
- [ ] Document error handling 用於 both protocols
- [ ] Create code examples 用於 common patterns

**Deliverables**:

- Communication pattern guidelines document
- Protocol selection decision tree
- Code examples 和 templates

**Success Criteria**:

- Clear guidelines 用於 when to use each protocol
- Team understands protocol selection criteria

### 第 2 階段： REST Client Implementation (Timeline: Week 1-2)

**Objectives**:

- Implement REST clients 用於 service-to-service calls
- Configure timeouts 和 retries
- Integrate 與 observability

**Tasks**:

- [ ] Create RestTemplate/WebClient configuration
- [ ] Configure connection pooling 和 timeouts
- [ ] Implement circuit breaker 用於 REST calls (Resilience4j)
- [ ] Add distributed tracing (X-Ray) to REST clients
- [ ] Implement retry logic 與 exponential backoff
- [ ] Create REST client code templates
- [ ] Document REST client usage

**Deliverables**:

- Configured REST clients 與 resilience patterns
- Code templates 用於 REST communication
- Documentation

**Success Criteria**:

- REST clients working 與 circuit breaker 和 retries
- Distributed tracing working 跨 services

### 第 3 階段： Kafka Producer/Consumer Implementation (Timeline: Week 2-3)

**Objectives**:

- Implement Kafka producers 用於 event publishing
- Implement Kafka consumers 用於 event handling
- Ensure reliable message delivery

**Tasks**:

- [ ] Configure Kafka producers 與 idempotency
- [ ] Configure Kafka consumers 與 error handling
- [ ] Implement dead letter queue 用於 failed messages
- [ ] Add distributed tracing (X-Ray) to Kafka messages
- [ ] Implement event versioning strategy
- [ ] Create Kafka producer/consumer code templates
- [ ] Document Kafka usage patterns

**Deliverables**:

- Configured Kafka producers 和 consumers
- Dead letter queue implementation
- Code templates 和 documentation

**Success Criteria**:

- Kafka messages delivered reliably
- Failed messages routed to dead letter queue
- Distributed tracing working 用於 Kafka events

### 第 4 階段： Testing and Validation (Timeline: Week 3-4)

**Objectives**:

- Test synchronous 和 asynchronous flows
- Validate performance 和 reliability
- Create testing guidelines

**Tasks**:

- [ ] Create integration tests 用於 REST communication
- [ ] Create integration tests 用於 Kafka communication
- [ ] Test circuit breaker 和 retry behavior
- [ ] Test Kafka message delivery 和 error handling
- [ ] Perform load testing 用於 both protocols
- [ ] Validate distributed tracing end-to-end
- [ ] Create testing guidelines 和 examples
- [ ] Document troubleshooting procedures

**Deliverables**:

- Comprehensive integration tests
- Load testing results
- Testing guidelines

**Success Criteria**:

- All tests passing
- Performance targets met (REST < 100ms, Kafka throughput > 10,000 msg/s)
- Distributed tracing working end-to-end

### 回滾策略

**觸發條件**：

- Critical performance degradation (> 50% latency increase)
- Message loss 或 data inconsistency
- Service communication failures
- Team unable to implement patterns correctly

**回滾步驟**：

1. **Immediate Action**: Revert to previous communication patterns
2. **Service Rollback**: Deploy previous service versions
3. **Configuration Rollback**: Restore previous REST/Kafka configurations
4. **Validation**: Verify services communicating correctly
5. **Root Cause Analysis**: Investigate issues 和 plan remediation

**回滾時間**： 15-30 minutes

**Rollback Testing**: Test rollback procedure in staging environment

## 監控和成功標準

### 成功指標

| Metric | Target | Measurement Method | Review Frequency |
|--------|--------|-------------------|------------------|
| REST Latency (p95) | < 100ms | Spring Boot Actuator metrics | Real-time |
| REST Success Rate | > 99.5% | HTTP status codes | Daily |
| Kafka Throughput | > 10,000 msg/s | Kafka metrics | Real-time |
| Kafka Consumer Lag | < 1000 messages | Kafka consumer metrics | Real-time |
| Message Delivery Rate | 100% | Kafka producer metrics | Daily |

### 監控計畫

**Dashboards**:

- **REST Communication Dashboard**: Latency, success rate, circuit breaker status
- **Kafka Communication Dashboard**: Throughput, consumer lag, message delivery
- **Service Communication Map**: Visualize service dependencies 和 communication patterns

**告警**：

- **Critical**: REST success rate < 95% (PagerDuty)
- **Critical**: Kafka consumer lag > 10,000 messages (PagerDuty)
- **Warning**: REST latency > 200ms p95 (Slack)
- **Warning**: Kafka message delivery failure (Slack)

**審查時程**：

- **Real-time**: Automated monitoring 和 alerting
- **Daily**: Review communication metrics 和 errors
- **Weekly**: Analyze communication patterns 和 optimize
- **Monthly**: Comprehensive performance review

### Key Performance Indicators (KPIs)

- **Performance KPI**: REST latency < 100ms (p95), Kafka throughput > 10,000 msg/s
- **Reliability KPI**: REST success rate > 99.5%, Kafka delivery rate 100%
- **Scalability KPI**: 支援 10,000 req/s peak load
- **Observability KPI**: 100% distributed tracing coverage

## 後果

### Positive Consequences ✅

- **Flexibility**: Choose appropriate protocol 用於 each 使用案例
- **Performance**: REST 提供s low latency 用於 synchronous operations
- **Scalability**: Kafka 提供s high throughput 用於 asynchronous operations
- **Decoupling**: Kafka events 啟用 loose coupling between services
- **Familiarity**: Team already knows REST 和 Kafka
- **Tooling**: 優秀的 tooling 和 ecosystem 支援
- **Debugging**: 容易debug 和 monitor both protocols
- **Evolution**: 可以 introduce gRPC 用於 specific 使用案例s in future

### Negative Consequences ❌

- **Dual Protocol**: Need to manage two communication protocols (Mitigation: Clear guidelines 和 templates)
- **複雜的ity**: More 複雜的 than single protocol (Mitigation: Documentation 和 training)
- **Protocol Selection**: Need to choose correct protocol 用於 each 使用案例 (Mitigation: Decision tree 和 guidelines)

### 技術債務

**Debt Introduced**:

- **Protocol Expertise**: Team needs expertise in both REST 和 Kafka
- **Monitoring 複雜的ity**: Need to monitor two different protocols
- **Testing 複雜的ity**: Need to test both synchronous 和 asynchronous flows

**債務償還計畫**：

- **Training**: Regular training on REST 和 Kafka best practices
- **Documentation**: 維持 comprehensive communication pattern guidelines
- **Automation**: Automate testing 用於 both protocols
- **Monitoring**: Unified observability dashboard 用於 both protocols

### Long-term Implications

This decision establishes REST + Kafka as our inter-service communication strategy 用於 the next 3-5 年. As the platform evolves:

- Consider gRPC 用於 specific high-performance 使用案例s
- Evaluate GraphQL 用於 flexible client queries
- Monitor communication patterns 和 optimize
- Keep REST 和 Kafka libraries updated
- Reassess if performance requirements change signifi可以tly

The hybrid approach 提供s flexibility to evolve communication patterns while 維持ing simplicity 和 team productivity.

## 相關決策

### Related ADRs

- [ADR-002: Hexagonal Architecture](002-adopt-hexagonal-architecture.md) - Service boundaries
- [ADR-003: Domain Events](003-use-domain-events-for-cross-context-communication.md) - Event-driven communication
- [ADR-005: Apache Kafka](005-use-apache-kafka-for-event-streaming.md) - Message broker
- [ADR-009: RESTful API Design](009-restful-api-design-with-openapi.md) - API standards
- [ADR-025: Saga Pattern](025-saga-pattern-distributed-transactions.md) - Distributed transactions
- [ADR-030: API Gateway Pattern](030-api-gateway-pattern.md) - External API access

### Affected Viewpoints

- [Functional Viewpoint](../../viewpoints/functional/README.md) - Service interactions
- [Concurrency Viewpoint](../../viewpoints/concurrency/README.md) - Synchronous vs asynchronous
- [Deployment Viewpoint](../../viewpoints/deployment/README.md) - Service deployment

### Affected Perspectives

- [Performance Perspective](../../perspectives/performance/README.md) - Communication latency
- [Availability Perspective](../../perspectives/availability/README.md) - Fault tolerance
- [Evolution Perspective](../../perspectives/evolution/README.md) - Protocol evolution

## 備註

### Assumptions

- Team has REST 和 Kafka expertise
- Kong Gateway 支援s REST routing
- Kafka cluster available 和 reliable
- Services deployed on AWS EKS
- Distributed tracing infrastructure available

### Constraints

- 必須 支援 Spring Boot ecosystem
- 必須 integrate 與 Kong API Gateway
- 必須 work 與 existing Kafka infrastructure
- 必須 提供 low latency 用於 synchronous calls
- 必須 ensure message delivery 用於 asynchronous calls

### Open Questions

- 應該 we introduce gRPC 用於 specific high-performance 使用案例s?
- What is optimal timeout 用於 REST calls between services?
- How to 處理 Kafka schema evolution?
- 應該 we use Kafka Streams 用於 複雜的 event processing?

### Follow-up Actions

- [ ] Create communication pattern guidelines - Architecture Team
- [ ] Implement REST client templates - Backend Team
- [ ] Implement Kafka producer/consumer templates - Backend Team
- [ ] Create integration tests 用於 both protocols - QA Team
- [ ] Set up monitoring dashboards - DevOps Team
- [ ] Conduct training on communication patterns - Tech Lead

### References

- [Microservices Communication Patterns](https://microservices.io/patterns/communication-style/messaging.html)
- [REST vs gRPC Comparison](https://cloud.google.com/blog/products/api-management/understanding-grpc-openapi-and-rest)
- [Kafka 用於 Microservices](https://www.confluent.io/blog/apache-kafka-for-service-architectures/)
- [Spring Cloud OpenFeign](https://spring.io/projects/spring-cloud-openfeign)
- [Spring Kafka](https://spring.io/projects/spring-kafka)
- [Resilience4j Circuit Breaker](https://resilience4j.readme.io/docs/circuitbreaker)

---

**ADR Template Version**: 1.0  
**Last Template Update**: 2025-01-17
