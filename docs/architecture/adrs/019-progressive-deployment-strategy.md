---
adr_number: 019
title: "Progressive Deployment Strategy (可以ary + Rolling Update)"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [007, 017, 018]
affected_viewpoints: ["deployment", "operational"]
affected_perspectives: ["availability", "evolution"]
---

# ADR-019: Progressive Deployment Strategy (可以ary + Rolling Update)

## 狀態

**Accepted** - 2025-10-25

## 上下文

### 問題陳述

The Enterprise E-Commerce Platform 需要safe, reliable deployment strategy to:

**Business Requirements**:

- **Zero Downtime**: 維持 99.9% availability 期間 deployments
- **Risk Mitigation**: Minimize impact of faulty releases
- **Fast Rollback**: Quick recovery from deployment failures (< 5 minutes)
- **Confidence**: Validate releases before full rollout
- **Frequency**: 支援 multiple deployments per 天
- **Compliance**: Meet audit 和 compliance requirements

**Technical Challenges**:

- 複雜的 microservices architecture (13 bounded contexts)
- High traffic volume (10,000+ requests/minute)
- Database schema changes
- API compatibility requirements
- Cross-service dependencies
- Multi-region deployment coordination
- Monitoring 和 validation 複雜的ity

**目前狀態**：

- All-at-once deployments
- Signifi可以t downtime 期間 releases
- High risk of widespread failures
- Manual rollback procedures
- Limited deployment validation
- Infrequent releases (weekly)

### 業務上下文

**業務驅動因素**：

- 啟用 continuous delivery
- 降低 deployment risk
- 改善 time-to-market
- Increase deployment frequency
- Minimize customer impact
- 支援 A/B testing

**限制條件**：

- 預算: $50,000 用於 implementation
- Timeline: 2 個月
- Team: 3 DevOps engineers
- 必須 work 與 existing EKS infrastructure
- 可以not disrupt current operations

### 技術上下文

**Current Deployment**:

- Manual kubectl apply
- All pods updated simultaneously
- No traffic shifting
- Manual validation
- Long deployment windows

**Target Deployment**:

- Automated progressive rollout
- Traffic-based validation
- Automatic rollback on failures
- Continuous monitoring
- Multiple deployments per 天

## 決策驅動因素

1. **Safety**: Minimize blast radius of faulty deployments
2. **Speed**: 啟用 rapid rollback when issues detected
3. **Confidence**: Validate releases 與 real traffic
4. **Automation**: 降低 manual intervention
5. **Observability**: Comprehensive monitoring 期間 rollout
6. **Flexibility**: 支援 different rollout strategies
7. **Simplicity**: 容易understand 和 operate
8. **成本**： Optimize infrastructure costs

## 考慮的選項

### 選項 1： Canary + Rolling Update (Recommended)

**描述**： Combine canary deployment for validation with rolling update for full rollout

**Strategy**:

```typescript
interface ProgressiveDeploymentStrategy {
  phases: {
    canary: {
      description: 'Deploy to small subset, validate with real traffic',
      trafficPercentage: [10, 25, 50],
      duration: [5, 10, 15], // minutes per stage
      validation: {
        metrics: [
          'Error rate < 0.1%',
          'Response time p95 < 2s',
          'Success rate > 99.9%'
        ],
        automated: true,
        rollbackOnFailure: true
      }
    },
    
    rollingUpdate: {
      description: 'Gradually replace all pods',
      strategy: 'RollingUpdate',
      maxSurge: '25%',
      maxUnavailable: '0%',
      validation: {
        healthChecks: true,
        readinessProbe: true,
        livenessProbe: true
      }
    }
  },
  
  rollback: {
    trigger: 'Automatic on metric violations',
    duration: '< 5 minutes',
    method: 'Instant traffic shift to stable version'
  }
}
```

**可以ary Deployment 與 Flagger**:

```yaml
# Flagger Canary Resource
apiVersion: flagger.app/v1beta1
kind: Canary
metadata:
  name: ecommerce-api
  namespace: production
spec:
  # Target deployment
  targetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: ecommerce-api
  
  # Progressive traffic shifting
  service:
    port: 80
    targetPort: 8080
    gateways:

    - public-gateway

    hosts:

    - api.ecommerce.example.com
  
  # Canary analysis
  analysis:
    # Schedule interval
    interval: 1m
    
    # Max traffic percentage routed to canary
    maxWeight: 50
    
    # Canary increment step
    stepWeight: 10
    
    # Number of checks before promotion
    threshold: 5
    
    # Metrics for validation
    metrics:

    - name: request-success-rate

      thresholdRange:
        min: 99
      interval: 1m
    
    - name: request-duration

      thresholdRange:
        max: 2000
      interval: 1m
    
    - name: error-rate

      thresholdRange:
        max: 0.1
      interval: 1m
    
    # Webhooks for custom validation
    webhooks:

    - name: load-test

      url: http://flagger-loadtester/
      timeout: 5s
      metadata:
        type: cmd
        cmd: "hey -z 1m -q 10 -c 2 http://ecommerce-api-canary/health"
    
    - name: acceptance-test

      url: http://flagger-loadtester/
      timeout: 30s
      metadata:
        type: bash
        cmd: "./run-acceptance-tests.sh"
  
  # Rollback configuration
  rollbackOnFailure: true
  
  # Skip analysis for specific conditions
  skipAnalysis: false

---
# Prometheus metrics for Flagger
apiVersion: v1
kind: ConfigMap
metadata:
  name: flagger-metrics
  namespace: production
data:
  metrics.yaml: |

    - name: request-success-rate

      query: |
        sum(
          rate(
            http_server_requests_seconds_count{
              namespace="production",
              pod=~"ecommerce-api-.*",
              status!~"5.."
            }[1m]
          )
        )
        /
        sum(
          rate(
            http_server_requests_seconds_count{
              namespace="production",
              pod=~"ecommerce-api-.*"
            }[1m]
          )
        )

        * 100
    
    - name: request-duration

      query: |
        histogram_quantile(
          0.95,
          sum(
            rate(
              http_server_requests_seconds_bucket{
                namespace="production",
                pod=~"ecommerce-api-.*"
              }[1m]
            )
          ) by (le)
        )

        * 1000
    
    - name: error-rate

      query: |
        sum(
          rate(
            http_server_requests_seconds_count{
              namespace="production",
              pod=~"ecommerce-api-.*",
              status=~"5.."
            }[1m]
          )
        )
        /
        sum(
          rate(
            http_server_requests_seconds_count{
              namespace="production",
              pod=~"ecommerce-api-.*"
            }[1m]
          )
        )

        * 100

```

**Rolling Update Configuration**:

```yaml
# Deployment with Rolling Update
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ecommerce-api
  namespace: production
spec:
  replicas: 10
  
  # Rolling update strategy
  strategy:
    type: RollingUpdate
    rollingUpdate:
      # Maximum number of pods that can be created over desired replicas
      maxSurge: 25%  # 2-3 extra pods during update
      
      # Maximum number of pods that can be unavailable
      maxUnavailable: 0%  # Zero downtime
  
  # Minimum time for pod to be ready before considered available
  minReadySeconds: 30
  
  # Number of old ReplicaSets to retain
  revisionHistoryLimit: 10
  
  # Progress deadline (rollback if not progressing)
  progressDeadlineSeconds: 600
  
  selector:
    matchLabels:
      app: ecommerce-api
  
  template:
    metadata:
      labels:
        app: ecommerce-api
        version: v1.2.0
    spec:
      containers:

      - name: api

        image: ecommerce-api:v1.2.0
        
        # Readiness probe - pod receives traffic only when ready
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
          timeoutSeconds: 3
          successThreshold: 1
          failureThreshold: 3
        
        # Liveness probe - pod restarted if unhealthy
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          successThreshold: 1
          failureThreshold: 3
        
        # Startup probe - gives app time to start
        startupProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 0
          periodSeconds: 5
          timeoutSeconds: 3
          successThreshold: 1
          failureThreshold: 30  # 150 seconds max startup time
        
        # Graceful shutdown
        lifecycle:
          preStop:
            exec:
              command: ["/bin/sh", "-c", "sleep 15"]
        
        # Termination grace period
        terminationGracePeriodSeconds: 30
```

**Deployment Pipeline 與 Progressive Rollout**:

```yaml
# GitHub Actions workflow
name: Progressive Deployment

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:

      - uses: actions/checkout@v3
      
      - name: Build and Push Image

        run: |
          docker build -t ecommerce-api:${{ github.sha }} .
          docker push $ECR_REPO/ecommerce-api:${{ github.sha }}
      
      - name: Update Kubernetes Manifests

        run: |
          kubectl set image deployment/ecommerce-api \
            api=$ECR_REPO/ecommerce-api:${{ github.sha }} \
            --namespace=production
      
      - name: Monitor Canary Deployment

        run: |
          # Wait for Flagger to complete canary analysis
          kubectl wait canary/ecommerce-api \
            --for=condition=Promoted \
            --timeout=30m \
            --namespace=production
      
      - name: Verify Deployment

        run: |
          # Check rollout status
          kubectl rollout status deployment/ecommerce-api \
            --namespace=production \
            --timeout=10m
          
          # Run smoke tests
          ./scripts/smoke-tests.sh
      
      - name: Rollback on Failure

        if: failure()
        run: |
          # Automatic rollback
          kubectl rollout undo deployment/ecommerce-api \
            --namespace=production
          
          # Notify team
          curl -X POST $SLACK_WEBHOOK \
            -d '{"text":"Deployment failed and rolled back"}'
```

**Monitoring Dashboard**:

```typescript
interface DeploymentMonitoring {
  realTimeMetrics: {
    canaryMetrics: [
      'Traffic percentage to canary',
      'Request success rate (canary vs stable)',
      'Response time p95 (canary vs stable)',
      'Error rate (canary vs stable)',
      'Pod health status'
    ],
    
    rollingUpdateMetrics: [
      'Pods ready vs desired',
      'Pods updated',
      'Rollout progress percentage',
      'Time elapsed',
      'Estimated time remaining'
    ]
  },
  
  alerts: {
    critical: [
      {
        name: 'Canary Failure',
        condition: 'Error rate > 0.1% or success rate < 99.9%',
        action: 'Automatic rollback + PagerDuty'
      },
      {
        name: 'Rolling Update Stuck',
        condition: 'No progress for 10 minutes',
        action: 'Alert DevOps team'
      }
    ],
    
    warning: [
      {
        name: 'Slow Rollout',
        condition: 'Rollout taking > 30 minutes',
        action: 'Slack notification'
      }
    ]
  },
  
  dashboards: {
    grafana: 'Real-time deployment progress',
    datadog: 'Detailed metrics comparison',
    cloudwatch: 'AWS infrastructure metrics'
  }
}
```

**Database Migration Strategy**:

```typescript
interface DatabaseMigrationStrategy {
  approach: 'Backward Compatible Migrations',
  
  phases: {
    phase1: {
      name: 'Add new schema (backward compatible)',
      description: 'Add new columns/tables without removing old ones',
      deployment: 'Deploy with both old and new code paths',
      validation: 'Verify new schema works'
    },
    
    phase2: {
      name: 'Migrate data',
      description: 'Copy data from old to new schema',
      deployment: 'Background job, no code changes',
      validation: 'Verify data integrity'
    },
    
    phase3: {
      name: 'Switch to new schema',
      description: 'Update code to use new schema only',
      deployment: 'Progressive rollout with canary',
      validation: 'Monitor for issues'
    },
    
    phase4: {
      name: 'Remove old schema',
      description: 'Drop old columns/tables',
      deployment: 'After validation period (1 week)',
      validation: 'Confirm no usage of old schema'
    }
  },
  
  rollback: {
    phase1: 'Drop new schema',
    phase2: 'Stop migration job',
    phase3: 'Revert code to use old schema',
    phase4: 'Cannot rollback (old schema removed)'
  }
}
```

**優點**：

- ✅ Zero downtime deployments
- ✅ Automatic validation 與 real traffic
- ✅ Fast rollback (< 5 minutes)
- ✅ Gradual risk exposure
- ✅ Comprehensive monitoring
- ✅ 支援s A/B testing
- ✅ Database migration safety

**缺點**：

- ⚠️ Longer deployment time (30-45 minutes)
- ⚠️ Requires additional infrastructure (Flagger)
- ⚠️ 複雜的 configuration
- ⚠️ Need comprehensive metrics

**成本**： $50,000 implementation + $5,000/year operational

**風險**： **Low** - Industry-proven approach

### 選項 2： Blue-Green Deployment

**描述**： Maintain two identical environments, switch traffic between them

**優點**：

- ✅ Instant rollback
- ✅ Full environment testing
- ✅ 簡單的 concept

**缺點**：

- ❌ Double infrastructure cost
- ❌ No gradual validation
- ❌ 複雜的 用於 databases
- ❌ All-or-nothing switch

**成本**： $80,000 implementation + $40,000/year operational (2x infrastructure)

**風險**： **Medium** - Higher cost and complexity

### 選項 3： Recreate Deployment

**描述**： Stop all old pods, then start new pods

**優點**：

- ✅ 簡單的st approach
- ✅ No version overlap
- ✅ 容易understand

**缺點**：

- ❌ Downtime 期間 deployment
- ❌ High risk
- ❌ No gradual validation
- ❌ Slow rollback

**成本**： $10,000 implementation

**風險**： **High** - Unacceptable downtime

## 決策結果

**選擇的選項**： **Canary + Rolling Update (Option 1)**

### 理由

Combining 可以ary deployment 用於 validation 與 rolling update 用於 full rollout 提供s the optimal balance of safety, speed, 和 zero downtime, justifying the implementation 複雜的ity.

## 影響分析

### 利害關係人影響

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | Medium | New deployment workflow | Training, documentation, automation |
| DevOps Team | High | New tools 和 processes | Training, runbooks, 支援 |
| QA Team | Low | Automated validation | Test automation integration |
| Operations Team | Medium | New monitoring requirements | Dashboards, alerts, training |
| Customers | Low | 改善d availability | Transparent deployments |

### Impact Radius Assessment

**選擇的影響半徑**： **System**

影響：

- All application deployments
- CI/CD pipeline
- Monitoring systems
- Rollback procedures
- Database migrations
- Team workflows

### 風險評估

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| 可以ary false positives | Medium | Low | Tune metrics thresholds, manual override |
| Slow rollouts | Low | Medium | Optimize validation duration |
| Metric collection failures | Low | High | Redundant monitoring, fallback to manual |
| Database migration issues | Medium | Critical | Backward compatible migrations, testing |
| Team adoption | Medium | Medium | Training, documentation, 支援 |

**整體風險等級**： **Low**

## 實作計畫

### 第 1 階段： Infrastructure Setup （第 1-2 週）

**Tasks**:

- [ ] Install Flagger in EKS clusters
- [ ] Configure Prometheus metrics
- [ ] Set up Grafana dashboards
- [ ] Configure alerting
- [ ] Test in development environment

**Success Criteria**:

- Flagger operational
- Metrics collection working
- Dashboards configured

### 第 2 階段： Pilot Service （第 3-4 週）

**Tasks**:

- [ ] Select pilot service
- [ ] Create 可以ary resource
- [ ] Configure metrics 和 thresholds
- [ ] Test 可以ary deployment
- [ ] Validate rollback
- [ ] Document process

**Success Criteria**:

- Successful 可以ary deployment
- Automatic rollback tested
- Team comfortable 與 process

### 第 3 階段： Rollout to All Services （第 5-6 週）

**Tasks**:

- [ ] Create 可以ary resources 用於 all services
- [ ] Update CI/CD pipelines
- [ ] Configure service-specific metrics
- [ ] Train team on new process
- [ ] Update documentation

**Success Criteria**:

- All services using progressive deployment
- CI/CD pipelines updated
- Team trained

### 第 4 階段： Database Migration Strategy （第 7-8 週）

**Tasks**:

- [ ] Document migration process
- [ ] Create migration templates
- [ ] Test backward compatible migrations
- [ ] Update deployment guidelines
- [ ] Train team

**Success Criteria**:

- Migration process documented
- Templates available
- Team trained

### 回滾策略

**觸發條件**：

- Flagger failures
- Metric collection issues
- Team concerns

**回滾步驟**：

1. Disable Flagger
2. Use standard rolling update
3. Fix issues
4. Re-啟用 Flagger

**回滾時間**： < 1 hour

## 監控和成功標準

### 成功指標

| Metric | Target | Measurement |
|--------|--------|-------------|
| Deployment Success Rate | > 95% | CI/CD metrics |
| Rollback Time | < 5 minutes | Flagger metrics |
| Deployment Frequency | > 5/day | CI/CD metrics |
| Zero Downtime | 100% | CloudWatch |
| Failed Deployment Detection | < 5 minutes | Flagger metrics |
| Mean Time to Recovery | < 10 minutes | Incident metrics |

### Review Schedule

- **Weekly**: Deployment metrics review
- **Monthly**: Process optimization
- **Quarterly**: Strategy review

## 後果

### 正面後果

- ✅ **Zero Downtime**: 100% availability 期間 deployments
- ✅ **Risk Mitigation**: Gradual rollout limits blast radius
- ✅ **Fast Rollback**: < 5 minute recovery from failures
- ✅ **Confidence**: Real traffic validation before full rollout
- ✅ **Frequency**: Multiple deployments per 天
- ✅ **Automation**: 降低d manual intervention
- ✅ **Observability**: Comprehensive deployment monitoring

### 負面後果

- ⚠️ **Deployment Time**: 30-45 minutes vs 10 minutes
- ⚠️ **複雜的ity**: More 複雜的 than 簡單的 rolling update
- ⚠️ **Infrastructure**: Additional components (Flagger)
- ⚠️ **Metrics Dependency**: Requires reliable metrics collection

### 技術債務

**已識別債務**：

1. Some services lack comprehensive metrics
2. Manual metric threshold tuning
3. Basic rollback procedures
4. Limited A/B testing capabilities

**債務償還計畫**：

- **Q2 2026**: Comprehensive metrics 用於 all services
- **Q3 2026**: Automated threshold tuning
- **Q4 2026**: Advanced A/B testing features

## 相關決策

- [ADR-007: Use AWS CDK 用於 Infrastructure as Code](007-use-aws-cdk-for-infrastructure.md)
- [ADR-017: Multi-Region Deployment Strategy](017-multi-region-deployment-strategy.md)
- [ADR-018: Container Orchestration 與 AWS EKS](018-container-orchestration-with-aws-eks.md)

---

**文檔狀態**： ✅ Accepted  
**上次審查**： 2025-10-25  
**下次審查**： 2026-01-25 （每季）

## 備註

### Deployment Strategy Comparison

| Strategy | Downtime | Rollback Speed | Risk | 複雜的ity | Cost |
|----------|----------|----------------|------|------------|------|
| 可以ary + Rolling | None | < 5 min | Low | High | Medium |
| Blue-Green | None | Instant | Medium | Medium | High |
| Rolling Update | None | 10-15 min | Medium | Low | Low |
| Recreate | Yes | 15-30 min | High | Very Low | Very Low |

### 可以ary Stages

| Stage | Traffic % | Duration | Validation |
|-------|-----------|----------|------------|
| Initial | 10% | 5 min | Error rate, latency |
| Intermediate | 25% | 10 min | Success rate, throughput |
| Advanced | 50% | 15 min | All metrics, load test |
| Full Rollout | 100% | Rolling | Health checks |

### Best Practices

**可以ary Deployment**:

- Start 與 small traffic percentage (10%)
- Monitor comprehensive metrics
- Use automated validation
- Have clear rollback criteria
- Test rollback procedures regularly

**Rolling Update**:

- Set maxUnavailable to 0 用於 zero downtime
- Use appropriate maxSurge 用於 faster rollout
- Implement proper health checks
- Set reasonable timeout values
- Monitor pod startup times

**Database Migrations**:

- Always use backward compatible changes
- Test migrations in staging
- Have rollback plan
- Monitor data integrity
- Use feature flags 用於 code changes

**Monitoring**:

- Track deployment progress
- Compare 可以ary vs stable metrics
- Alert on anomalies
- Log all deployment events
- Create deployment dashboards
