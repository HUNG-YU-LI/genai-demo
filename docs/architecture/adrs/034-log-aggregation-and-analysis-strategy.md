---
adr_number: 034
title: "Log Aggregation 和 Analysis Strategy"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [8, 43]
affected_viewpoints: ["operational", "deployment"]
affected_perspectives: ["availability", "performance", "security"]
decision_makers: ["Architecture Team", "Operations Team", "Security Team"]
---

# ADR-034: Log Aggregation 和 Analysis Strategy

## 狀態

**Status**: Accepted

**Date**: 2025-10-25

**Decision Makers**: Architecture Team, Operations Team, Security Team

## 上下文

### 問題陳述

The Enterprise E-Commerce Platform generates logs from multiple sources 跨 distributed services, requiring a centralized log aggregation 和 analysis solution. We need to:

- Collect logs from all services (backend, frontend, infrastructure)
- 啟用 real-time log search 和 analysis
- 支援 security incident investigation
- 維持 compliance 與 data retention policies
- 提供 cost-effective long-term log storage
- 啟用 correlation 跨 multi-region deployments

### 業務上下文

**業務驅動因素**：

- Regulatory compliance requiring audit trails (GDPR, PCI-DSS, Taiwan Personal Data Protection Act)
- Security incident response requiring rapid log analysis
- Operational troubleshooting requiring centralized log access
- Performance optimization requiring log-based insights
- Cost optimization requiring efficient log storage

**Business Constraints**:

- 預算 constraints 用於 log storage 和 analysis tools
- Data residency requirements 用於 Taiwan 和 Japan regions
- 7-year retention requirement 用於 audit logs
- Real-time alerting requirements 用於 security events

**Business Requirements**:

- 支援 1TB+ daily log volume
- 啟用 sub-second search 回應時間
- 提供 99.9% log ingestion availability
- 支援 structured logging format
- 啟用 cross-region log correlation

### 技術上下文

**Current Architecture**:

- Services log to stdout/stderr (12-factor app pattern)
- Container logs collected 透過 Kubernetes
- No centralized log aggregation currently
- Limited log retention (7 天 local)
- Manual log analysis required

**Technical Constraints**:

- 必須 integrate 與 AWS EKS
- 必須 支援 structured JSON logging
- 必須 處理 high log volume (1TB+/day)
- 必須 支援 multi-region deployment
- 必須 integrate 與 existing monitoring (CloudWatch, Grafana)

**Dependencies**:

- ADR-008: Observability platform (CloudWatch + X-Ray + Grafana)
- ADR-043: Multi-region observability
- AWS services (CloudWatch Logs, S3, Athena, Kinesis)

## 決策驅動因素

- **Cost Efficiency**: Need cost-effective solution 用於 1TB+/day log volume 與 7-year retention
- **Search Performance**: Sub-second search response 用於 operational troubleshooting
- **Scalability**: 處理 growing log volume as system scales
- **Integration**: Seamless integration 與 existing AWS infrastructure
- **Compliance**: Meet regulatory retention 和 audit requirements
- **Multi-Region**: 支援 cross-region log correlation
- **Real-time Analysis**: 啟用 real-time alerting 和 anomaly detection

## 考慮的選項

### 選項 1： AWS CloudWatch Logs + S3 + Athena

**描述**：
Use AWS native services 用於 log aggregation. CloudWatch Logs 用於 real-time ingestion 和 short-term storage, S3 用於 long-term archival, 和 Athena 用於 ad-hoc analysis.

**Pros** ✅:

- Native AWS integration 與 EKS, Lambda, RDS
- No infrastructure management required
- Automatic scaling 和 高可用性
- Cost-effective long-term storage 與 S3
- Integrated 與 CloudWatch Alarms 和 Metrics
- Built-in encryption 和 access control
- Multi-region 支援 與 cross-region replication

**Cons** ❌:

- CloudWatch Logs expensive 用於 high volume ($0.50/GB ingestion + $0.03/GB storage)
- Athena query performance slower than dedicated search engines
- Limited real-time search capabilities
- Query costs 可以 be high 用於 frequent analysis
- Less flexible than dedicated log platforms

**成本**：

- **Implementation Cost**: 2 person-weeks (configuration 和 setup)
- **Monthly Cost**:
  - CloudWatch Logs: $500/TB ingestion + $30/TB/month storage (30-day retention)
  - S3 Glacier: $4/TB/month (long-term archival)
  - Athena: $5/TB s可以ned
  - Total: ~$550/month 用於 1TB/day + $5-20/month Athena queries
- **Total Cost of Ownership (3 年)**: ~$20,000

**風險**： Low

**Risk Description**: Proven AWS service 與 high reliability, 但 cost 可以 escalate 與 volume

**Effort**: Low

**Effort Description**: Minimal setup, mostly configuration

### 選項 2： ELK Stack (Elasticsearch + Logstash + Kibana)

**描述**：
Deploy self-managed ELK stack on EKS 用於 full-featured log aggregation, search, 和 visualization.

**Pros** ✅:

- Powerful full-text search capabilities
- 豐富的 visualization 與 Kibana
- Flexible data transformation 與 Logstash
- Real-time search 和 analysis
- 大型的 ecosystem 和 community 支援
- Advanced analytics 和 machine learning features

**Cons** ❌:

- High 營運開銷 (cluster management, upgrades, scaling)
- Expensive infrastructure costs (EC2, EBS 用於 Elasticsearch cluster)
- Requires dedicated team expertise
- 複雜的 multi-region setup
- High memory 和 CPU requirements
- Elasticsearch licensing concerns (Elastic License vs Open Source)

**成本**：

- **Implementation Cost**: 6 person-weeks (cluster setup, configuration, integration)
- **Monthly Cost**:
  - EC2 instances: $2,000/month (3x r5.2x大型的 用於 Elasticsearch)
  - EBS storage: $500/month (5TB SSD)
  - Data transfer: $200/month
  - Total: ~$2,700/month
- **Total Cost of Ownership (3 年)**: ~$100,000 + 營運開銷

**風險**： High

**Risk Description**: Operational 複雜的ity, potential downtime 期間 upgrades, scaling challenges

**Effort**: High

**Effort Description**: Signifi可以t setup 和 ongoing maintenance effort

### 選項 3： Grafana Loki

**描述**：
Use Grafana Loki 用於 log aggregation 與 S3 backend storage, integrated 與 existing Grafana dashboards.

**Pros** ✅:

- Cost-effective (indexes only metadata, not full text)
- Native integration 與 Grafana
- Horizontally scalable
- S3 backend 用於 cost-effective storage
- 簡單的 query language (LogQL)
- Lower resource requirements than Elasticsearch
- Open source 與 active community

**Cons** ❌:

- Limited full-text search capabilities
- Less mature than ELK stack
- Requires label-based querying (learning curve)
- Limited advanced analytics features
- Smaller ecosystem compared to Elasticsearch

**成本**：

- **Implementation Cost**: 4 person-weeks (deployment, configuration, integration)
- **Monthly Cost**:
  - EC2 instances: $800/month (Loki components)
  - S3 storage: $23/TB/month
  - Data transfer: $100/month
  - Total: ~$950/month 用於 1TB/day
- **Total Cost of Ownership (3 年)**: ~$35,000

**風險**： Medium

**Risk Description**: Less mature platform, potential limitations 用於 複雜的 queries

**Effort**: Medium

**Effort Description**: Moderate setup effort, 簡單的r than ELK

## 決策結果

**選擇的選項**： Option 1 - AWS CloudWatch Logs + S3 + Athena

**Rationale**:
We chose AWS CloudWatch Logs 與 S3 archival 和 Athena 用於 analysis as the optimal solution 用於 our log aggregation needs. This decision balances cost, operational simplicity, 和 functionality:

1. **Cost Efficiency**: While CloudWatch Logs has higher ingestion costs, the overall TCO is lowest when considering 營運開銷. S3 Glacier 提供s extremely cost-effective long-term storage 用於 compliance.

2. **Operational Simplicity**: As a fully 託管服務, CloudWatch Logs 需要minimal 營運開銷, allowing our team to focus on application development rather than infrastructure management.

3. **AWS Integration**: Native integration 與 our existing AWS infrastructure (EKS, RDS, Lambda) 提供s seamless log collection 沒有 additional agents 或 configuration.

4. **Scalability**: Automatic scaling 處理s our growing log volume 沒有 manual intervention.

5. **Multi-Region 支援**: CloudWatch Logs 支援s our multi-region architecture (Taipei + Tokyo) 與 cross-region log aggregation.

6. **Compliance**: Built-in encryption, access control, 和 retention policies meet our regulatory requirements.

**Key Factors in Decision**:

1. **Total Cost of Ownership**: $20K over 3 年 vs $100K (ELK) 或 $35K (Loki)
2. **Operational Overhead**: Zero infrastructure management vs signifi可以t effort 用於 self-managed solutions
3. **Time to Value**: 2 週 implementation vs 4-6 週 用於 alternatives
4. **Risk Profile**: Low risk 與 proven AWS service vs higher risk 與 self-managed platforms

## 影響分析

### 利害關係人影響

| Stakeholder | Impact Level | Description | Mitigation Strategy |
|-------------|--------------|-------------|-------------------|
| Development Team | Medium | Need to adopt structured logging format | 提供 logging libraries 和 examples |
| Operations Team | Low | Simplified log access through CloudWatch console | Training on CloudWatch Logs Insights |
| Security Team | Low | Centralized security log analysis | Configure security-specific log groups 和 alarms |
| Business | Low | 改善d incident 回應時間 | Demonstrate value through metrics |
| Compliance Team | Low | Automated audit trail retention | Document retention policies 和 access controls |

### Impact Radius Assessment

**選擇的影響半徑**： System

**Impact Description**:

- **System**: Changes affect all services 跨 all bounded contexts
  - All services 必須 adopt structured JSON logging
  - All services 必須 configure CloudWatch Logs agent
  - All services 必須 follow log retention policies
  - Cross-region log correlation 需要unified log format

### Affected Components

- **All Microservices**: 必須 implement structured logging
- **EKS Cluster**: Configure Fluent Bit 用於 log forwarding
- **Lambda Functions**: Configure CloudWatch Logs integration
- **RDS Databases**: 啟用 CloudWatch Logs export
- **Monitoring Dashboards**: Add log-based metrics 和 alerts

### 風險評估

| Risk | Probability | Impact | Mitigation Strategy | Owner |
|------|-------------|--------|-------------------|-------|
| High log volume costs | Medium | High | Implement log sampling, optimize log levels, use S3 archival | Operations Team |
| CloudWatch Logs service limits | Low | Medium | Monitor usage, request limit increases proactively | Operations Team |
| Log data loss 期間 ingestion | Low | High | Configure retry logic, monitor ingestion metrics | Development Team |
| Slow Athena query performance | Medium | Medium | Optimize partitioning, use columnar format (Parquet) | Data Team |
| Cross-region log correlation 複雜的ity | Medium | Medium | Implement unified log format 與 region tags | Architecture Team |

**整體風險等級**： Low

**Risk Mitigation Plan**:

- Implement cost monitoring 和 alerting 用於 CloudWatch Logs usage
- Establish log retention policies (30 天 hot, 7 年 cold)
- Configure log sampling 用於 high-volume debug logs
- Use S3 Intelligent-Tiering 用於 cost optimization
- Implement structured logging standards 跨 all services

## 實作計畫

### 第 1 階段： Foundation Setup (Timeline: Week 1)

**Objectives**:

- Configure CloudWatch Logs infrastructure
- Establish logging standards
- Set up S3 archival

**Tasks**:

- [ ] Create CloudWatch Log Groups 用於 each service
- [ ] Configure log retention policies (30 天)
- [ ] Set up S3 bucket 用於 log archival 與 lifecycle policies
- [ ] Configure S3 Glacier transition (30 天 → Glacier)
- [ ] Create IAM roles 和 policies 用於 log access
- [ ] Document structured logging format (JSON schema)

**Deliverables**:

- CloudWatch Log Groups configured
- S3 archival bucket 與 lifecycle policies
- Logging standards documentation

**Success Criteria**:

- All log groups created 和 accessible
- S3 archival working 與 automatic transition
- IAM policies tested 和 validated

### 第 2 階段： Service Integration (Timeline: Week 2-3)

**Objectives**:

- Integrate all services 與 CloudWatch Logs
- Implement structured logging
- Configure log forwarding

**Tasks**:

- [ ] Deploy Fluent Bit DaemonSet on EKS clusters
- [ ] Configure Fluent Bit to forward logs to CloudWatch
- [ ] Update all services to use structured JSON logging
- [ ] Implement logging libraries 用於 Java/TypeScript
- [ ] Configure RDS CloudWatch Logs export
- [ ] Configure Lambda CloudWatch Logs integration
- [ ] Add correlation IDs 用於 request tracing

**Deliverables**:

- All services logging to CloudWatch
- Structured JSON log format implemented
- Correlation IDs 用於 distributed tracing

**Success Criteria**:

- 100% of services sending logs to CloudWatch
- All logs in structured JSON format
- Correlation IDs present in all service logs

### 第 3 階段： Analysis and Alerting (Timeline: Week 4)

**Objectives**:

- Configure log analysis 與 Athena
- Set up log-based metrics 和 alarms
- Create operational dashboards

**Tasks**:

- [ ] Configure Athena 用於 S3 log analysis
- [ ] Create Athena tables 與 partitioning
- [ ] Set up CloudWatch Logs Insights queries
- [ ] Configure log-based metrics (error rates, latency)
- [ ] Create CloudWatch Alarms 用於 critical errors
- [ ] Build Grafana dashboards 用於 log visualization
- [ ] Document common log queries 和 runbooks

**Deliverables**:

- Athena configured 用於 log analysis
- Log-based metrics 和 alarms
- Operational dashboards in Grafana

**Success Criteria**:

- Athena queries return results in < 10 seconds
- Critical error alarms triggering correctly
- Dashboards showing real-time log metrics

### 回滾策略

**觸發條件**：

- CloudWatch Logs ingestion failure rate > 5%
- Log storage costs exceed 預算 透過 50%
- Critical service performance degradation due to logging overhead

**回滾步驟**：

1. **Immediate Action**: Disable Fluent Bit log forwarding 用於 non-critical services
2. **Service Rollback**: Revert to local container logging
3. **Cost Control**: 降低 log retention to 7 天
4. **Verification**: Confirm services operating normally 沒有 CloudWatch Logs

**回滾時間**： < 30 minutes

**Rollback Testing**: Test rollback procedure in staging environment 月ly

## 監控和成功標準

### 成功指標

| Metric | Target | Measurement Method | Review Frequency |
|--------|--------|-------------------|------------------|
| Log Ingestion Success Rate | > 99.9% | CloudWatch Metrics | Daily |
| Log Search Response Time | < 5 seconds | CloudWatch Logs Insights | Weekly |
| Log Storage Cost | < $600/month | AWS Cost Explorer | Weekly |
| Log Retention Compliance | 100% | Automated audit | Monthly |
| Cross-Region Log Correlation | < 10 seconds lag | Custom metric | Daily |

### 監控計畫

**Dashboards**:

- **CloudWatch Logs Dashboard**: Ingestion rates, error rates, storage usage
- **Grafana Log Analytics Dashboard**: Log-based metrics, error trends, service health
- **Cost Dashboard**: Daily log costs, storage costs, Athena query costs

**告警**：

- **Critical**: Log ingestion failure rate > 5% (PagerDuty)
- **Warning**: Log storage cost > $500/month (Email)
- **Info**: Athena query cost > $20/day (Slack)

**審查時程**：

- **Daily**: Quick metrics check (ingestion rate, error rate)
- **Weekly**: Cost review 和 optimization opportunities
- **Monthly**: Retention policy compliance audit

### Key Performance Indicators (KPIs)

- **Operational KPI**: Mean Time To Detect (MTTD) incidents < 5 minutes
- **Cost KPI**: Log cost per GB < $0.60
- **Compliance KPI**: 100% audit log retention 用於 7 年
- **Performance KPI**: 95th percentile log search time < 10 seconds

## 後果

### Positive Consequences ✅

- **Simplified Operations**: No infrastructure to manage, automatic scaling
- **Cost Predictability**: Clear cost structure based on log volume
- **Fast Implementation**: 4 週 to full deployment vs 8-12 週 用於 self-managed
- **High Availability**: AWS SLA guarantees 99.9% uptime
- **Compliance Ready**: Built-in encryption, retention, 和 access controls
- **Multi-Region 支援**: Native cross-region log aggregation
- **Integration**: Seamless integration 與 existing AWS services

### Negative Consequences ❌

- **Vendor Lock-in**: Tied to AWS CloudWatch Logs ecosystem (Mitigation: Use standard log formats 用於 portability)
- **Limited Search**: Less powerful than Elasticsearch 用於 複雜的 queries (Mitigation: Use Athena 用於 ad-hoc analysis)
- **Cost Scaling**: Costs increase linearly 與 log volume (Mitigation: Implement log sampling 和 retention policies)
- **Query Limitations**: CloudWatch Logs Insights has query 複雜的ity limits (Mitigation: Use Athena 用於 複雜的 analysis)

### 技術債務

**Debt Introduced**:

- **Log Format Migration**: Future migration to different platform 需要log format conversion
- **Query Optimization**: Athena queries may need optimization as data volume grows

**債務償還計畫**：

- **Log Format**: Use industry-standard JSON format to minimize migration effort
- **Query Optimization**: Quarterly review of Athena query performance 和 partitioning strategy

### Long-term Implications

This decision establishes CloudWatch Logs as our standard log aggregation platform 用於 the next 3-5 年. As log volume grows, we may need to:

- Implement more aggressive log sampling 用於 debug logs
- Optimize S3 storage 與 Intelligent-Tiering
- Consider hybrid approach 與 Loki 用於 specific high-volume services
- Evaluate AWS OpenSearch Service if advanced search becomes critical

The structured logging format 和 correlation IDs 提供 foundation 用於 future observability enhancements including distributed tracing 和 service mesh integration.

## 相關決策

### Related ADRs

- [ADR-008: Observability Platform](20250117-008-observability-platform.md) - CloudWatch Logs integrates 與 overall observability strategy
- [ADR-043: Multi-Region Observability](20250117-043-multi-region-observability.md) - Cross-region log correlation requirements

### Affected Viewpoints

- [Operational Viewpoint](../../viewpoints/operational/README.md) - Defines operational procedures using centralized logs
- [Deployment Viewpoint](../../viewpoints/deployment/README.md) - Log forwarding configuration in deployment

### Affected Perspectives

- [Availability Perspective](../../perspectives/availability/README.md) - Log-based monitoring 改善s availability
- [Performance Perspective](../../perspectives/performance/README.md) - Log analysis identifies performance issues
- [Security Perspective](../../perspectives/security/README.md) - Security incident investigation using logs

## 備註

### Assumptions

- Log volume 將 grow to 2TB/day within 2 年
- Structured JSON logging 可以 be adopted 跨 all services
- CloudWatch Logs service limits sufficient 用於 our scale
- S3 Glacier acceptable 用於 long-term archival access patterns

### Constraints

- 必須 comply 與 7-year retention requirement
- 必須 支援 data residency requirements (Taiwan, Japan)
- 必須 integrate 與 existing Grafana dashboards
- 必須 not impact service performance

### Open Questions

- 應該 we implement log sampling 用於 debug logs?
- What is the optimal CloudWatch Logs retention period (30 vs 90 天)?
- 應該 we use CloudWatch Logs Insights 或 Athena 用於 primary analysis?

### Follow-up Actions

- [ ] Create structured logging library 用於 Java services - Development Team
- [ ] Create structured logging library 用於 TypeScript services - Development Team
- [ ] Document log query examples 和 runbooks - Operations Team
- [ ] Set up cost monitoring 和 alerting - FinOps Team
- [ ] Conduct training on CloudWatch Logs Insights - Operations Team

### References

- [AWS CloudWatch Logs Documentation](https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/)
- [AWS CloudWatch Logs Insights Query Syntax](https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/CWL_QuerySyntax.html)
- [AWS Athena Best Practices](https://docs.aws.amazon.com/athena/latest/ug/performance-tuning.html)
- [Fluent Bit 用於 Kubernetes](https://docs.fluentbit.io/manual/installation/kubernetes)
- [Structured Logging Best Practices](https://www.loggly.com/ultimate-guide/json-logging-best-practices/)

---

**ADR Template Version**: 1.0  
**Last Template Update**: 2025-01-17
