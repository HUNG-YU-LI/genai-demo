---
adr_number: 030
title: "API Gateway Pattern"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [9, 23, 14, 50]
affected_viewpoints: ["functional", "deployment", "operational"]
affected_perspectives: ["security", "performance", "evolution"]
decision_makers: ["Architecture Team", "Backend Team", "DevOps Team"]
---

# ADR-030: API Gateway Pattern

## 狀態

**Status**: Accepted

**Date**: 2025-10-25

**Decision Makers**: Architecture Team, Backend Team, DevOps Team

## 上下文

### 問題陳述

企業電子商務平台由multiple microservices (13 bounded contexts) that 需要to be exposed to external clients (web frontends, mobile apps, third-party integrations). We need to decide on an API Gateway strategy：組成

- 提供 a unified entry point 用於 all client requests
- 處理 cross-cutting concerns (authentication, 速率限制, logging)
- Route requests to appropriate backend services
- Transform requests/responses as needed
- Protect backend services from direct exposure

This decision impacts:

- API security 和 authentication
- Request routing 和 load balancing
- Rate limiting 和 throttling
- API versioning 和 evolution
- Monitoring 和 observability
- Developer experience

### 業務上下文

**業務驅動因素**：

- Unified API experience 用於 frontend applications
- Centralized security 和 authentication
- Simplified client integration (single endpoint)
- Consistent 速率限制 和 throttling
- 降低d backend service 複雜的ity
- 支援 用於 API versioning 和 evolution

**Business Constraints**:

- Multiple client types (Next.js CMC, Angular Consumer, Mobile apps, Third-party integrations)
- High traffic volume (peak: 10,000 req/s)
- Low latency requirements (< 100ms gateway overhead)
- 99.9% availability requirement
- Cost optimization 用於 AWS services
- 支援 用於 gradual migration from monolith

**Business Requirements**:

- Single entry point 用於 all API requests
- JWT-based authentication at gateway level
- Rate limiting per client/IP/endpoint
- Request/response transformation
- API versioning 支援 (v1, v2)
- Comprehensive logging 和 monitoring

### 技術上下文

**Current Architecture**:

- Backend: Spring Boot microservices (13 bounded contexts)
- Frontend: Next.js (CMC), Angular (Consumer)
- Authentication: JWT tokens (ADR-014)
- Rate Limiting: Application-level (ADR-023, ADR-050)
- Deployment: AWS EKS (ADR-018)
- Observability: CloudWatch + X-Ray + Grafana (ADR-008)

**Technical Constraints**:

- 必須 integrate 與 existing JWT authentication
- 必須 支援 existing 速率限制 strategy
- 必須 work 與 AWS EKS deployment
- 必須 提供 low latency (< 100ms overhead)
- 必須 支援 high throughput (10,000 req/s)
- 必須 integrate 與 existing observability stack

**Dependencies**:

- ADR-009: RESTful API Design (API standards)
- ADR-023: API Rate Limiting Strategy (速率限制 implementation)
- ADR-014: JWT-Based Authentication (authentication mechanism)
- ADR-050: API Security 和 Rate Limiting Strategy (security requirements)

## 決策驅動因素

- **Performance**: Low latency overhead (< 100ms)
- **Scalability**: 支援 high throughput (10,000 req/s)
- **Security**: Centralized authentication 和 authorization
- **Flexibility**: 容易configure routing 和 transformations
- **成本**： Optimize AWS service costs
- **維持ability**: 簡單operate 和 troubleshoot
- **Integration**: Seamless integration 與 existing infrastructure

## 考慮的選項

### 選項 1： AWS API Gateway (Managed Service)

**描述**：
Use AWS API Gateway as a fully managed API gateway service 與 built-in features 用於 authentication, 速率限制, caching, 和 monitoring.

**Pros** ✅:

- **Fully Managed**: No infrastructure to manage, automatic scaling
- **Native AWS Integration**: Seamless integration 與 Lambda, Cognito, WAF, CloudWatch
- **Built-in Features**: Authentication, 速率限制, caching, request/response transformation
- **High Availability**: Multi-AZ deployment, 99.95% SLA
- **Security**: AWS WAF integration, API keys, usage plans
- **Monitoring**: CloudWatch metrics, X-Ray tracing, access logs
- **Cost-Effective 用於 Low Traffic**: Pay-per-request pricing
- **Easy Setup**: Quick to configure via AWS Console 或 CDK

**Cons** ❌:

- **Vendor Lock-in**: Tightly coupled to AWS ecosystem
- **Cost at Scale**: Expensive 用於 high traffic (>10M requests/month)
- **Limited Customization**: Restricted to AWS-提供d features
- **Cold Start**: REST API has 29-second timeout, HTTP API has 30-second timeout
- **複雜的ity**: Two types (REST API vs HTTP API) 與 different features
- **Performance**: Additional network hop, potential latency
- **Limited Transformation**: VTL (Velocity Template Language) 用於 transformations

**成本**：

- **REST API**: $3.50 per million requests + $0.09/GB data transfer
- **HTTP API**: $1.00 per million requests + $0.09/GB data transfer
- **Estimated Monthly Cost** (100M requests): $100-350 + data transfer
- **Total Cost of Ownership (3 年)**: ~$15,000-50,000

**風險**： Low

**Risk Description**: Proven AWS service 與 high reliability

**Effort**: Low

**Effort Description**: Quick setup 與 AWS CDK, minimal configuration

### 選項 2： Kong Gateway (Open Source / Enterprise)

**描述**：
Deploy Kong Gateway as a self-hosted API gateway on EKS 與 plugins 用於 authentication, 速率限制, logging, 和 transformations.

**Pros** ✅:

- **Open Source**: Free community edition, no vendor lock-in
- **Highly Customizable**: Extensive plugin ecosystem (50+ plugins)
- **Performance**: Low latency (< 10ms overhead), high throughput
- **Flexibility**: Custom plugins in Lua, 支援 用於 any backend
- **Multi-Cloud**: Works on any Kubernetes cluster, not AWS-specific
- **豐富的 Features**: Authentication, 速率限制, caching, transformations, circuit breaker
- **Developer Experience**: Declarative configuration, GitOps-friendly
- **Enterprise Option**: Kong Enterprise 用於 advanced features (RBAC, analytics)

**Cons** ❌:

- **Self-Hosted**: Need to manage infrastructure, scaling, updates
- **Operational Overhead**: Monitoring, logging, troubleshooting required
- **Database Dependency**: Requires PostgreSQL 用於 configuration storage
- **Learning Curve**: Need to learn Kong configuration 和 plugin system
- **High Availability**: Need to configure multi-replica deployment
- **成本**： Infrastructure costs (EC2, RDS) + operational overhead
- **Enterprise Features**: Advanced features 需要paid license

**成本**：

- **Infrastructure**: $500-1000/month (EKS nodes, RDS PostgreSQL)
- **Operational Overhead**: 2 person-days/month ($2,000/month)
- **Enterprise License** (optional): $3,000-10,000/month
- **Total Cost of Ownership (3 年)**: ~$100,000-150,000 (community) 或 $200,000-400,000 (enterprise)

**風險**： Medium

**Risk Description**: Requires operational expertise, potential downtime 期間 upgrades

**Effort**: High

**Effort Description**: Signifi可以t setup 和 configuration, ongoing maintenance

### 選項 3： Spring Cloud Gateway (Application-Level)

**描述**：
Deploy Spring Cloud Gateway as a Spring Boot application on EKS, leveraging Spring ecosystem 用於 routing, filtering, 和 integration.

**Pros** ✅:

- **Spring Ecosystem**: Native integration 與 Spring Boot, Spring Security, Spring Cloud
- **Java-Based**: Familiar technology 用於 Java developers, easy to customize
- **Reactive**: Built on Spring WebFlux 用於 high performance
- **Flexible Routing**: Powerful routing DSL, predicates, 和 filters
- **Custom Filters**: 容易write custom filters in Java
- **Observability**: Spring Boot Actuator, Micrometer integration
- **No Additional Infrastructure**: Runs as Spring Boot app on existing EKS
- **Cost-Effective**: No additional service costs, only compute resources

**Cons** ❌:

- **Self-Hosted**: Need to manage deployment, scaling, updates
- **Limited Features**: Fewer built-in features compared to Kong 或 AWS API Gateway
- **Operational Overhead**: Monitoring, logging, troubleshooting required
- **Performance**: Higher latency than Kong (Java overhead)
- **Scaling**: Need to configure auto-scaling, load balancing
- **High Availability**: Need to deploy multiple replicas
- **Learning Curve**: Need to learn Spring Cloud Gateway configuration

**成本**：

- **Infrastructure**: $300-500/month (EKS nodes)
- **Operational Overhead**: 1.5 person-days/month ($1,500/month)
- **Total Cost of Ownership (3 年)**: ~$70,000-90,000

**風險**： Medium

**Risk Description**: Requires Spring expertise, potential performance issues

**Effort**: Medium

**Effort Description**: Moderate setup, 需要Spring Cloud Gateway knowledge

## 決策結果

**選擇的選項**： Option 2 - Kong Gateway (Open Source)

**Rationale**:
We chose Kong Gateway as our API gateway solution. This decision prioritizes performance, flexibility, 和 long-term cost optimization over 託管服務 convenience:

1. **Performance**: Kong 提供s < 10ms latency overhead compared to AWS API Gateway's higher latency. For high-traffic e-commerce platform (10,000 req/s peak), this translates to signifi可以t performance 改善ment.

2. **Cost Optimization**: At our traffic volume (100M+ requests/month), Kong's infrastructure costs ($500-1000/month) are signifi可以tly lower than AWS API Gateway's pay-per-request pricing ($100-350/month + data transfer). Long-term savings justify 營運開銷.

3. **Flexibility 和 Customization**: Kong's extensive plugin ecosystem (50+ plugins) 和 ability to write custom Lua plugins 提供 flexibility 用於 future requirements (custom authentication, advanced 速率限制, request transformations).

4. **Multi-Cloud Strategy**: Kong works on any Kubernetes cluster, 支援ing potential future multi-cloud 或 hybrid cloud strategy. Not locked into AWS ecosystem.

5. **Developer Experience**: Declarative configuration 和 GitOps-friendly approach align 與 our infrastructure-as-code strategy (ADR-007). Configuration stored in Git, versioned, 和 reviewed.

6. **Enterprise Readiness**: Kong Community Edition 提供s all essential features. Option to upgrade to Kong Enterprise 用於 advanced features (RBAC, analytics, developer portal) if needed.

7. **Kubernetes Native**: Kong runs natively on Kubernetes (EKS), leveraging existing container orchestration infrastructure (ADR-018). No additional infrastructure required.

8. **Proven at Scale**: Kong is used 透過 major companies (Nasdaq, Expedia, Samsung) handling billions of requests per 天. Proven reliability 和 performance.

**Key Factors in Decision**:

1. **Performance**: < 10ms latency overhead critical 用於 用戶體驗
2. **成本**： Long-term cost savings at high traffic volume
3. **Flexibility**: Extensive plugin ecosystem 用於 future requirements
4. **Team Capability**: Team has Kubernetes expertise, 可以 manage Kong deployment

## 影響分析

### 利害關係人影響

| Stakeholder | Impact Level | Description | Mitigation Strategy |
|-------------|--------------|-------------|-------------------|
| Backend Team | Medium | Need to configure Kong routing 和 plugins | Training on Kong configuration, documentation |
| DevOps Team | High | Responsible 用於 Kong deployment 和 operations | Kong training, operational runbooks, monitoring setup |
| Frontend Team | Low | Transparent change, same API endpoints | Communication about gateway deployment |
| Security Team | Medium | Need to configure authentication 和 速率限制 | Security configuration review, penetration testing |
| Operations Team | High | New component to monitor 和 troubleshoot | Monitoring dashboards, alerting, runbooks |

### Impact Radius Assessment

**選擇的影響半徑**： System

**Impact Description**:

- **System**: Changes affect entire API infrastructure
  - All API requests routed through Kong Gateway
  - All services 必須 be configured in Kong
  - All authentication 處理d at gateway level
  - All 速率限制 enforced at gateway level
  - All API monitoring includes gateway metrics

### Affected Components

- **All Backend Services**: Configured as Kong upstreams
- **Frontend Applications**: API calls routed through Kong
- **Authentication**: JWT validation at Kong gateway
- **Rate Limiting**: Enforced at Kong gateway
- **Monitoring**: Kong metrics added to observability stack
- **CI/CD**: Kong configuration deployed via CDK

### 風險評估

| Risk | Probability | Impact | Mitigation Strategy | Owner |
|------|-------------|--------|-------------------|-------|
| Kong deployment failure | Low | High | Blue-green deployment, rollback plan | DevOps Team |
| Performance degradation | Medium | High | Load testing, performance monitoring | DevOps Team |
| Configuration errors | Medium | Medium | Configuration validation, staging testing | Backend Team |
| Database failure (PostgreSQL) | Low | High | Multi-AZ RDS, automated backups | DevOps Team |
| Learning curve | High | Low | Training sessions, documentation | Tech Lead |
| Operational overhead | Medium | Medium | Automation, monitoring, runbooks | DevOps Team |

**整體風險等級**： Medium

**Risk Mitigation Plan**:

- Comprehensive load testing before production deployment
- Blue-green deployment strategy 用於 zero-downtime migration
- Multi-AZ RDS PostgreSQL 用於 Kong configuration database
- Automated monitoring 和 alerting 用於 Kong health
- Detailed operational runbooks 用於 common issues
- Training sessions 用於 DevOps 和 Backend teams

## 實作計畫

### 第 1 階段： Kong Setup and Configuration (Timeline: Week 1-2)

**Objectives**:

- Deploy Kong on EKS
- Configure PostgreSQL database
- Set up basic routing

**Tasks**:

- [ ] Deploy PostgreSQL RDS 用於 Kong configuration (Multi-AZ)
- [ ] Deploy Kong on EKS using Helm chart (2 replicas minimum)
- [ ] Configure Kong Ingress Controller
- [ ] Set up Kong Admin API access (secured)
- [ ] Configure basic routing 用於 one service (Customer API)
- [ ] Test basic request routing
- [ ] Configure health checks 和 readiness probes

**Deliverables**:

- Kong deployed on EKS 與 PostgreSQL backend
- Basic routing working 用於 one service
- Health checks configured

**Success Criteria**:

- Kong pods running 和 healthy
- PostgreSQL database accessible
- Basic routing working 與 < 10ms latency overhead

### 第 2 階段： Authentication and Security (Timeline: Week 2-3)

**Objectives**:

- Configure JWT authentication
- Set up 速率限制
- Integrate 與 AWS WAF

**Tasks**:

- [ ] Configure Kong JWT plugin 用於 authentication
- [ ] Integrate 與 existing JWT token validation (ADR-014)
- [ ] Configure 速率限制 plugin (ADR-023, ADR-050)
- [ ] Set up API key authentication 用於 third-party integrations
- [ ] Configure CORS plugin 用於 frontend applications
- [ ] Integrate Kong 與 AWS WAF (ADR-049)
- [ ] Configure request/response logging
- [ ] Test authentication 和 速率限制

**Deliverables**:

- JWT authentication working at gateway
- Rate limiting enforced
- Security plugins configured

**Success Criteria**:

- JWT tokens validated correctly
- Rate limiting working per client/IP/endpoint
- Unauthorized requests blocked

### 第 3 階段： Service Migration (Timeline: Week 3-5)

**Objectives**:

- Migrate all services to Kong
- Configure routing 用於 all endpoints
- Test end-to-end flows

**Tasks**:

- [ ] Configure Kong routes 用於 all 13 bounded contexts
- [ ] Set up service-specific rate limits
- [ ] Configure request/response transformations (if needed)
- [ ] Migrate Customer API endpoints
- [ ] Migrate Order API endpoints
- [ ] Migrate Product API endpoints
- [ ] Migrate remaining service endpoints
- [ ] Test all API endpoints through Kong
- [ ] Validate performance 和 latency

**Deliverables**:

- All services routed through Kong
- All endpoints tested 和 working
- Performance validated

**Success Criteria**:

- All API endpoints accessible through Kong
- Latency overhead < 10ms
- No functional regressions

### 第 4 階段： Monitoring and Operations (Timeline: Week 5-6)

**Objectives**:

- Set up monitoring 和 alerting
- Create operational runbooks
- Train operations team

**Tasks**:

- [ ] Configure Kong Prometheus plugin 用於 metrics
- [ ] Create Grafana dashboards 用於 Kong metrics
- [ ] Set up CloudWatch alarms 用於 Kong health
- [ ] Configure X-Ray tracing through Kong
- [ ] Create operational runbooks (deployment, troubleshooting, scaling)
- [ ] Document Kong configuration 和 plugins
- [ ] Conduct training sessions 用於 DevOps 和 Backend teams
- [ ] Perform load testing 和 capacity planning
- [ ] Create disaster recovery procedures

**Deliverables**:

- Monitoring dashboards operational
- Alerting configured
- Operational runbooks created
- Team trained

**Success Criteria**:

- All Kong metrics visible in Grafana
- Alerts triggering correctly
- Team comfortable 與 Kong operations

### 回滾策略

**觸發條件**：

- Kong gateway unavailable 用於 > 5 minutes
- Performance degradation > 50ms latency increase
- Critical security vulnerability discovered
- Database failure preventing Kong operation

**回滾步驟**：

1. **Immediate Action**: Route traffic directly to backend services (bypass Kong)
2. **DNS Update**: Update Route 53 to point to backend load balancers
3. **Service Validation**: Verify backend services accessible directly
4. **Communication**: Notify team of rollback 和 investigation plan
5. **Root Cause Analysis**: Investigate Kong failure 和 plan remediation

**回滾時間**： 5-10 minutes (DNS propagation)

**Rollback Testing**: Test rollback procedure in staging environment 月ly

## 監控和成功標準

### 成功指標

| Metric | Target | Measurement Method | Review Frequency |
|--------|--------|-------------------|------------------|
| Gateway Latency | < 10ms (p95) | Kong Prometheus metrics | Real-time |
| Gateway Availability | > 99.9% | Kong health checks | Real-time |
| Request Success Rate | > 99.5% | Kong access logs | Daily |
| Authentication Success Rate | > 99.9% | Kong JWT plugin metrics | Daily |
| Rate Limit Accuracy | 100% | Kong 速率限制 metrics | Weekly |

### 監控計畫

**Dashboards**:

- **Kong Performance Dashboard**: Latency, throughput, error rates
- **Kong Security Dashboard**: Authentication failures, rate limit hits, blocked requests
- **Kong Health Dashboard**: Pod health, database connections, resource usage

**告警**：

- **Critical**: Kong gateway unavailable (PagerDuty)
- **Critical**: Database connection failure (PagerDuty)
- **Warning**: Latency > 50ms p95 (Slack)
- **Warning**: Error rate > 1% (Slack)
- **Info**: Rate limit threshold reached (Slack)

**審查時程**：

- **Real-time**: Automated monitoring 和 alerting
- **Daily**: Review error logs 和 performance metrics
- **Weekly**: Capacity planning 和 optimization review
- **Monthly**: Comprehensive performance 和 cost review

### Key Performance Indicators (KPIs)

- **Performance KPI**: Gateway latency < 10ms (p95)
- **Reliability KPI**: Gateway availability > 99.9%
- **Security KPI**: Zero unauthorized access incidents
- **Cost KPI**: Gateway infrastructure cost < $1,000/month

## 後果

### Positive Consequences ✅

- **Centralized Security**: Authentication 和 authorization at gateway level
- **Simplified Clients**: Single entry point 用於 all API requests
- **Performance**: Low latency overhead (< 10ms)
- **Flexibility**: Extensive plugin ecosystem 用於 future requirements
- **Cost Optimization**: Lower cost at high traffic volume
- **Multi-Cloud Ready**: Not locked into AWS ecosystem
- **Developer Experience**: Declarative configuration, GitOps-friendly
- **Observability**: Centralized logging 和 monitoring

### Negative Consequences ❌

- **Operational Overhead**: Need to manage Kong deployment 和 operations (Mitigation: Automation, monitoring, runbooks)
- **Learning Curve**: Team needs to learn Kong configuration (Mitigation: Training 和 documentation)
- **Database Dependency**: Kong 需要PostgreSQL (Mitigation: Multi-AZ RDS 與 automated backups)
- **Single Point of Failure**: Gateway failure affects all services (Mitigation: Multi-replica deployment, health checks, rollback plan)

### 技術債務

**Debt Introduced**:

- **Kong Expertise**: Team needs to 維持 Kong expertise
- **Configuration Management**: Kong configuration needs to be versioned 和 managed
- **Database Maintenance**: PostgreSQL database 需要regular maintenance

**債務償還計畫**：

- **Training**: Quarterly Kong training sessions 用於 team
- **Documentation**: 維持 comprehensive Kong documentation
- **Automation**: Automate Kong configuration deployment 和 updates
- **Monitoring**: Continuous monitoring 和 optimization

### Long-term Implications

This decision establishes Kong Gateway as our API gateway 用於 the next 3-5 年. As the platform evolves:

- Consider Kong Enterprise 用於 advanced features (RBAC, analytics, developer portal)
- Evaluate Kong Mesh 用於 service mesh capabilities
- Monitor Kong performance 和 optimize configuration
- Keep Kong 和 plugins updated to latest versions
- Reassess if traffic patterns change signifi可以tly (> 100,000 req/s)

Kong Gateway 提供s foundation 用於 API management, enabling centralized security, 速率限制, 和 monitoring while 維持ing high performance 和 flexibility.

## 相關決策

### Related ADRs

- [ADR-009: RESTful API Design](009-restful-api-design-with-openapi.md) - API design standards
- [ADR-023: API Rate Limiting Strategy](023-api-rate-limiting-strategy.md) - Rate limiting implementation
- [ADR-014: JWT-Based Authentication](014-jwt-based-authentication-strategy.md) - Authentication mechanism
- [ADR-050: API Security 和 Rate Limiting Strategy](050-api-security-and-rate-limiting-strategy.md) - Security requirements

### Affected Viewpoints

- [Functional Viewpoint](../../viewpoints/functional/README.md) - API routing 和 functionality
- [Deployment Viewpoint](../../viewpoints/deployment/README.md) - Kong deployment on EKS
- [Operational Viewpoint](../../viewpoints/operational/README.md) - Kong operations 和 monitoring

### Affected Perspectives

- [Security Perspective](../../perspectives/security/README.md) - Centralized authentication 和 authorization
- [Performance Perspective](../../perspectives/performance/README.md) - Gateway latency 和 throughput
- [Evolution Perspective](../../perspectives/evolution/README.md) - API versioning 和 evolution

## 備註

### Assumptions

- Traffic volume: 100M+ requests/month
- Team has Kubernetes expertise
- PostgreSQL RDS available 用於 Kong configuration
- AWS EKS cluster available 用於 Kong deployment
- Team 將ing to learn Kong configuration

### Constraints

- 必須 integrate 與 existing JWT authentication (ADR-014)
- 必須 支援 existing 速率限制 strategy (ADR-023, ADR-050)
- 必須 work 與 AWS EKS deployment (ADR-018)
- 必須 提供 low latency (< 100ms overhead)
- 必須 支援 high throughput (10,000 req/s)

### Open Questions

- 應該 we use Kong Community Edition 或 Kong Enterprise?
- What is optimal number of Kong replicas 用於 高可用性?
- 應該 we use Kong DB-less mode 或 PostgreSQL mode?
- How to 處理 Kong configuration versioning 和 rollback?

### Follow-up Actions

- [ ] Deploy Kong on staging EKS cluster - DevOps Team
- [ ] Configure PostgreSQL RDS 用於 Kong - DevOps Team
- [ ] Create Kong configuration templates - Backend Team
- [ ] Develop Kong operational runbooks - DevOps Team
- [ ] Conduct Kong training sessions - Tech Lead
- [ ] Perform load testing 和 capacity planning - DevOps Team
- [ ] Set up monitoring dashboards 和 alerts - DevOps Team

### References

- [Kong Gateway Documentation](https://docs.konghq.com/gateway/latest/)
- [Kong on Kubernetes](https://docs.konghq.com/kubernetes-ingress-controller/latest/)
- [Kong Plugin Hub](https://docs.konghq.com/hub/)
- [Kong Performance Benchmarks](https://konghq.com/blog/kong-gateway-performance)
- [API Gateway Pattern](https://microservices.io/patterns/apigateway.html)
- [Kong vs AWS API Gateway Comparison](https://konghq.com/blog/kong-vs-aws-api-gateway)

---

**ADR Template Version**: 1.0  
**Last Template Update**: 2025-01-17
