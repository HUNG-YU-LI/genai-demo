---
adr_number: 046
title: "Third Region Disaster Recovery (Singapore/Seoul)"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [037, 038, 040, 044]
affected_viewpoints: ["deployment", "operational"]
affected_perspectives: ["availability", "evolution"]
---

# ADR-046: Third Region Disaster Recovery (Singapore/Seoul)

## 狀態

**Accepted** - 2025-10-25

## 上下文

### 問題陳述

Current active-active architecture in Taiwan 和 Tokyo 提供s 高可用性 但 lacks geographic diversity 用於 true disaster recovery:

**Current Limitations**:

- **Geographic Concentration**: Both regions in East Asia
- **Correlated Risks**: Regional disasters (earthquakes, typhoons) could affect both
- **Geopolitical Risks**: Taiwan Strait tensions could impact both regions
- **Limited Failover Options**: No third region 用於 complete primary region loss
- **Recovery Time**: Extended recovery if both regions unavailable
- **Data Sovereignty**: Need alternative region 用於 data residency compliance

**Disaster Scenarios Requiring Third Region**:

1. **Dual Region Failure**: Both Taiwan 和 Tokyo unavailable
2. **Regional Natural Disaster**: Major earthquake affecting East Asia
3. **Geopolitical Crisis**: Conflict impacting multiple regions
4. **Extended Outage**: Long-term unavailability requiring alternative region
5. **Data Migration**: Need to relocate data due to regulatory changes

**Business Impact Without Third Region**:

- Complete service outage if both primary regions fail
- Extended recovery time (> 4 hours)
- Potential data loss
- Customer trust erosion
- Revenue loss
- Regulatory non-compliance

### 業務上下文

**業務驅動因素**：

- Business continuity (mandatory)
- Geographic risk diversification
- Regulatory compliance (data residency)
- Customer trust 和 reliability
- Competitive advantage
- Market expansion opportunities

**限制條件**：

- 預算: $50,000/year 用於 third region
- 必須 維持 99.9% availability
- Data sovereignty requirements
- Minimize operational 複雜的ity
- Cost optimization

### 技術上下文

**目前狀態**：

- Active-active in Taiwan (ap-northeast-1) 和 Tokyo (ap-northeast-1)
- No third region
- Manual disaster recovery procedures
- Limited geographic diversity
- 4-hour RTO 用於 complete region loss

**需求**：

- Third region in different geographic area
- Automated failover capability
- < 2 hour RTO 用於 third region activation
- < 15 minute RPO 用於 關鍵資料
- Data sovereignty compliance
- Cost-effective implementation

## 決策驅動因素

1. **Geographic Diversity**: 降低 correlated regional risks
2. **Availability**: 維持 99.9% SLO even 期間 dual region failure
3. **Recovery Time**: < 2 hour RTO 用於 third region activation
4. **Data Protection**: < 15 minute RPO 用於 關鍵資料
5. **成本**： Optimize third region costs ($50K/year budget)
6. **Compliance**: Meet data sovereignty requirements
7. **Automation**: Automated failover 和 recovery
8. **Simplicity**: Minimize operational 複雜的ity

## 考慮的選項

### 選項 1： Singapore as Primary Third Region with Seoul Backup (Recommended)

**描述**： Deploy Singapore (ap-southeast-1) as warm standby third region with Seoul (ap-northeast-2) as cold standby backup

**架構**：

```text
Primary Regions (Active-Active):
├── Taiwan (ap-northeast-1) - 50% traffic
└── Tokyo (ap-northeast-1) - 50% traffic

Third Region (Warm Standby):
└── Singapore (ap-southeast-1)
    ├── Compute: 20% capacity (ready to scale)
    ├── Database: Read replica (5-minute lag)
    ├── Cache: Empty (ready to populate)
    └── Storage: Replicated from primary

Backup Region (Cold Standby):
└── Seoul (ap-northeast-2)
    ├── Compute: None (deploy from IaC)
    ├── Database: Backup snapshots only
    ├── Cache: None
    └── Storage: Backup snapshots only
```

**Singapore Warm Standby Configuration**:

```typescript
interface SingaporeWarmStandby {
  infrastructure: {
    compute: {
      eks: {
        nodeGroups: 2,
        instanceType: 'm5.large',
        capacity: '20% of primary',
        cost: '$3,000/month'
      }
    },
    
    database: {
      rds: {
        instanceType: 'db.r5.large',
        role: 'Read replica',
        replicationLag: '< 5 minutes',
        cost: '$2,500/month'
      }
    },
    
    cache: {
      elasticache: {
        nodeType: 'cache.r5.large',
        nodes: 2,
        status: 'Empty, ready to populate',
        cost: '$1,000/month'
      }
    },
    
    storage: {
      s3: {
        replication: 'Cross-region replication',
        consistency: 'Eventually consistent',
        cost: '$500/month'
      }
    },
    
    network: {
      vpc: 'Dedicated VPC',
      connectivity: 'VPC peering to Taiwan/Tokyo',
      bandwidth: '1 Gbps',
      cost: '$1,000/month'
    }
  },
  
  totalMonthlyCost: '$8,000',
  totalAnnualCost: '$96,000'
}
```

**Seoul Cold Standby Configuration**:

```typescript
interface SeoulColdStandby {
  infrastructure: {
    compute: {
      status: 'No running infrastructure',
      deployment: 'IaC templates ready',
      activationTime: '< 2 hours'
    },
    
    database: {
      snapshots: 'Daily automated snapshots',
      retention: '30 days',
      restoreTime: '< 1 hour'
    },
    
    storage: {
      s3: 'Backup snapshots only',
      replication: 'Weekly backup replication',
      cost: '$500/month'
    }
  },
  
  totalMonthlyCost: '$500',
  totalAnnualCost: '$6,000'
}
```

**Why Singapore?**:

- ✅ Geographic diversity (Southeast Asia vs East Asia)
- ✅ Low latency to Taiwan/Tokyo (~50-80ms)
- ✅ Stable political environment
- ✅ Strong data protection laws
- ✅ 優秀的 AWS infrastructure
- ✅ English-speaking 支援
- ✅ Regional hub 用於 APAC

**Why Seoul as Backup?**:

- ✅ Geographic proximity to Taiwan/Tokyo
- ✅ Lower cost than Singapore
- ✅ 良好的 AWS infrastructure
- ✅ Alternative if Singapore unavailable
- ✅ Cultural 和 business ties to region

**Activation Procedures**:

**Scenario 1: Activate Singapore (Dual Region Failure)**:

```typescript
interface SingaporeActivation {
  trigger: {
    condition: 'Both Taiwan and Tokyo unavailable',
    detection: 'Automated health checks',
    decision: 'Automatic or manual approval'
  },
  
  steps: [
    {
      phase: 'Immediate Response (0-5 minutes)',
      actions: [
        'Detect dual region failure',
        'Activate incident response team',
        'Notify stakeholders',
        'Initiate Singapore activation'
      ]
    },
    {
      phase: 'Infrastructure Scaling (5-30 minutes)',
      actions: [
        'Scale EKS nodes to 100% capacity',
        'Promote RDS read replica to primary',
        'Populate ElastiCache from database',
        'Update DNS routing to Singapore',
        'Verify health checks'
      ]
    },
    {
      phase: 'Service Validation (30-60 minutes)',
      actions: [
        'Run smoke tests',
        'Verify critical services',
        'Monitor error rates',
        'Validate data integrity',
        'Confirm customer access'
      ]
    },
    {
      phase: 'Stabilization (60-120 minutes)',
      actions: [
        'Monitor system performance',
        'Optimize resource allocation',
        'Update monitoring dashboards',
        'Communicate status to customers',
        'Plan recovery strategy'
      ]
    }
  ],
  
  rto: '< 2 hours',
  rpo: '< 15 minutes',
  
  rollback: {
    trigger: 'Singapore activation fails',
    action: 'Attempt Seoul activation',
    time: '< 1 hour'
  }
}
```

**Scenario 2: Activate Seoul (Singapore Also Unavailable)**:

```typescript
interface SeoulActivation {
  trigger: {
    condition: 'All three regions (Taiwan, Tokyo, Singapore) unavailable',
    detection: 'Manual assessment',
    decision: 'Executive approval required'
  },
  
  steps: [
    {
      phase: 'Infrastructure Deployment (0-60 minutes)',
      actions: [
        'Deploy EKS cluster from IaC',
        'Deploy RDS from latest snapshot',
        'Deploy ElastiCache cluster',
        'Configure networking',
        'Deploy application services'
      ]
    },
    {
      phase: 'Data Restoration (60-120 minutes)',
      actions: [
        'Restore database from snapshot',
        'Verify data integrity',
        'Restore S3 data from backups',
        'Populate caches',
        'Sync with any available region'
      ]
    },
    {
      phase: 'Service Activation (120-180 minutes)',
      actions: [
        'Update DNS routing',
        'Run comprehensive tests',
        'Verify all services',
        'Monitor error rates',
        'Confirm customer access'
      ]
    },
    {
      phase: 'Stabilization (180-240 minutes)',
      actions: [
        'Monitor system performance',
        'Optimize configuration',
        'Update monitoring',
        'Communicate with customers',
        'Plan long-term recovery'
      ]
    }
  ],
  
  rto: '< 4 hours',
  rpo: '< 1 hour',
  
  dataLoss: {
    risk: 'Potential data loss since last snapshot',
    mitigation: 'Hourly snapshots, transaction logs'
  }
}
```

**Data Replication Strategy**:

```typescript
interface DataReplicationStrategy {
  tier1_critical: {
    services: ['Order', 'Payment', 'Authentication'],
    replication: {
      singapore: 'Async replication (< 5 min lag)',
      seoul: 'Hourly snapshots'
    },
    rpo: '< 15 minutes'
  },
  
  tier2_important: {
    services: ['Customer', 'Inventory', 'Product'],
    replication: {
      singapore: 'Async replication (< 15 min lag)',
      seoul: 'Daily snapshots'
    },
    rpo: '< 1 hour'
  },
  
  tier3_standard: {
    services: ['Analytics', 'Reporting', 'Logs'],
    replication: {
      singapore: 'Async replication (< 1 hour lag)',
      seoul: 'Weekly snapshots'
    },
    rpo: '< 24 hours'
  }
}
```

**Cost Breakdown**:

```typescript
const thirdRegionCosts = {
  singapore: {
    compute: 36000,      // $3,000/month
    database: 30000,     // $2,500/month
    cache: 12000,        // $1,000/month
    storage: 6000,       // $500/month
    network: 12000,      // $1,000/month
    subtotal: 96000
  },
  
  seoul: {
    storage: 6000,       // $500/month (snapshots only)
    subtotal: 6000
  },
  
  dataTransfer: {
    taiwanToSingapore: 12000,  // $1,000/month
    tokyoToSingapore: 12000,   // $1,000/month
    subtotal: 24000
  },
  
  total: 126000,  // $126,000/year
  
  note: 'Exceeds $50K budget - requires optimization or budget increase'
}
```

**Cost Optimization Options**:

1. **降低 Singapore capacity to 10%**: Save $18,000/year
2. **Use Spot instances**: Save $15,000/year
3. **Optimize data transfer**: Save $10,000/year
4. **Use smaller database instance**: Save $12,000/year
5. **Total potential savings**: $55,000/year
6. **Optimized cost**: $71,000/year (still over 預算)

**優點**：

- ✅ True geographic diversity
- ✅ Fast activation (< 2 hours)
- ✅ Low data loss (< 15 minutes RPO)
- ✅ Automated failover capability
- ✅ Two backup options (Singapore + Seoul)
- ✅ 支援s data sovereignty
- ✅ Regional expansion opportunity

**缺點**：

- ⚠️ Cost exceeds 預算 ($126K vs $50K)
- ⚠️ Operational 複雜的ity
- ⚠️ Data transfer costs
- ⚠️ Requires cost optimization

**成本**： $126,000/year (requires optimization to $71K)

**風險**： **Low** - Comprehensive coverage

### 選項 2： Seoul Only (Cost-Optimized)

**描述**： Deploy Seoul as single third region (warm standby)

**優點**：

- ✅ Lower cost ($60K/year)
- ✅ 簡單的r operations
- ✅ Geographic proximity

**缺點**：

- ❌ Still in East Asia (limited diversity)
- ❌ Single backup option
- ❌ Higher correlated risk

**成本**： $60,000/year

**風險**： **Medium** - Limited geographic diversity

### 選項 3： Mumbai as Third Region

**描述**： Deploy Mumbai (ap-south-1) as third region

**優點**：

- ✅ True geographic diversity
- ✅ Lower cost than Singapore
- ✅ Growing AWS region

**缺點**：

- ❌ Higher latency (~150ms)
- ❌ Less mature infrastructure
- ❌ Potential connectivity issues

**成本**： $80,000/year

**風險**： **Medium** - Latency and infrastructure concerns

## 決策結果

**選擇的選項**： **Singapore as Primary Third Region with Seoul Backup (Option 1)** with cost optimization

### 理由

Singapore 提供s optimal geographic diversity 與 acceptable latency, while Seoul offers cost-effective backup option.

**Cost Optimization Plan**:

1. 降低 Singapore capacity to 15% (save $12K)
2. Use Spot instances 用於 non-critical workloads (save $15K)
3. Optimize data transfer 與 caching (save $10K)
4. Use smaller database instance initially (save $12K)
5. Implement lifecycle policies 用於 storage (save $6K)

**Optimized Cost**: $71,000/year (需要$21K 預算 increase 或 further optimization)

## 影響分析

### 利害關係人影響

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Executive Team | High | 預算 increase required | ROI analysis, phased approach |
| Operations Team | High | Manage third region infrastructure | Training, automation, runbooks |
| Development Team | Medium | 支援 multi-region testing | Documentation, tooling |
| Finance Team | High | 預算 approval 用於 cost increase | Cost-benefit analysis |
| Customers | Low | Transparent failover | Communication plan |

### Impact Radius Assessment

**選擇的影響半徑**： **System**

影響：

- All services 和 data
- Infrastructure architecture
- Disaster recovery procedures
- Cost structure
- Operational processes

### 風險評估

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| Cost overruns | Medium | High | Strict cost monitoring, optimization |
| Activation failure | Low | Critical | Regular testing, automation |
| Data loss 期間 failover | Low | High | Frequent replication, testing |
| Latency increase | Low | Medium | Performance monitoring, optimization |
| Operational 複雜的ity | Medium | Medium | Automation, training, documentation |

**整體風險等級**： **Low**

## 實作計畫

### 第 1 階段： Singapore Infrastructure Setup (Month 1-2)

**Tasks**:

- [ ] Deploy EKS cluster in Singapore
- [ ] Set up RDS read replica
- [ ] Configure ElastiCache
- [ ] Set up S3 cross-region replication
- [ ] Configure VPC 和 networking
- [ ] Deploy monitoring infrastructure
- [ ] Test data replication
- [ ] Perform initial failover test

**Success Criteria**:

- Singapore infrastructure operational
- Data replication working (< 5 min lag)
- Initial failover test successful

### 第 2 階段： Seoul Backup Setup (Month 3)

**Tasks**:

- [ ] Create IaC templates 用於 Seoul
- [ ] Set up automated snapshots
- [ ] Configure backup replication
- [ ] Test infrastructure deployment
- [ ] Test data restoration
- [ ] Document activation procedures

**Success Criteria**:

- Seoul 可以 be deployed < 2 hours
- Backup restoration tested
- Procedures documented

### 第 3 階段： Automation & Testing (Month 4-5)

**Tasks**:

- [ ] Implement automated failover
- [ ] Create activation runbooks
- [ ] Conduct failover drills
- [ ] Test data integrity
- [ ] Optimize performance
- [ ] Train operations team

**Success Criteria**:

- Automated failover working
- RTO < 2 hours achieved
- RPO < 15 minutes achieved
- Team trained

### 第 4 階段： Cost Optimization (Month 6)

**Tasks**:

- [ ] Implement Spot instances
- [ ] Optimize data transfer
- [ ] Right-size resources
- [ ] Implement lifecycle policies
- [ ] Monitor 和 adjust

**Success Criteria**:

- Cost 降低d to $71K/year
- Performance 維持ed
- Availability 維持ed

### 回滾策略

**觸發條件**：

- Cost exceeds 預算 signifi可以tly
- Performance degradation
- Activation failures

**回滾步驟**：

1. Decommission Singapore infrastructure
2. Return to two-region architecture
3. 維持 Seoul snapshots only
4. Re-evaluate strategy

**回滾時間**： < 1 week

## 監控和成功標準

### 成功指標

| Metric | Target | Measurement |
|--------|--------|-------------|
| Singapore Activation Time | < 2 hours | Drill results |
| Seoul Activation Time | < 4 hours | Drill results |
| Data Replication Lag | < 5 minutes | Monitoring |
| RPO Achievement | < 15 minutes | Actual failover |
| System Availability | > 99.9% | CloudWatch |
| Cost | < $75K/year | Cost reports |
| Drill Success Rate | > 95% | Drill results |

### Review Schedule

- **Monthly**: Cost review, performance monitoring
- **Quarterly**: Failover drill, procedure review
- **Annually**: Comprehensive strategy review

## 後果

### 正面後果

- ✅ **True Geographic Diversity**: Singapore in Southeast Asia
- ✅ **Fast Recovery**: < 2 hour RTO
- ✅ **Low Data Loss**: < 15 minute RPO
- ✅ **Dual Backup Options**: Singapore + Seoul
- ✅ **Automated Failover**: 降低s manual effort
- ✅ **Regional Expansion**: Foundation 用於 APAC growth
- ✅ **Customer Confidence**: Demonstrated resilience

### 負面後果

- ⚠️ **Cost Increase**: $71K/year (42% over 預算)
- ⚠️ **Operational 複雜的ity**: More regions to manage
- ⚠️ **Data Transfer Costs**: Cross-region replication
- ⚠️ **Testing Overhead**: Regular drills required
- ⚠️ **Latency Considerations**: Singapore slightly higher latency

### 技術債務

**已識別債務**：

1. Manual cost optimization
2. Basic failover automation
3. Limited testing scenarios
4. Manual capacity planning

**債務償還計畫**：

- **Q2 2026**: Advanced automation
- **Q3 2026**: AI-powered capacity planning
- **Q4 2026**: Predictive failover

## 相關決策

- [ADR-037: Active-Active Multi-Region Architecture](037-active-active-multi-region-architecture.md)
- [ADR-038: Cross-Region Data Replication Strategy](038-cross-region-data-replication-strategy.md)
- [ADR-040: Network Partition Handling Strategy](040-network-partition-handling-strategy.md)
- [ADR-044: Business Continuity Plan](044-business-continuity-plan-geopolitical-risks.md)

---

**文檔狀態**： ✅ Accepted  
**上次審查**： 2025-10-25  
**下次審查**： 2026-01-25 （每季）

## 備註

### Geographic Diversity Analysis

**Current Risk (Taiwan + Tokyo)**:

- Both in East Asia
- Similar seismic risk
- Similar typhoon risk
- Correlated geopolitical risk
- Limited diversity

**With Singapore**:

- Southeast Asia vs East Asia
- Different seismic zones
- Different weather patterns
- Different geopolitical context
- True geographic diversity

### Latency Comparison

| Route | Latency | Impact |
|-------|---------|--------|
| Taiwan ↔ Tokyo | ~30ms | 優秀的 |
| Taiwan ↔ Singapore | ~50ms | 良好的 |
| Tokyo ↔ Singapore | ~80ms | Acceptable |
| Taiwan ↔ Seoul | ~40ms | 良好的 |
| Tokyo ↔ Seoul | ~30ms | 優秀的 |

### Alternative Regions Considered

**Hong Kong**: Rejected due to geopolitical concerns
**Sydney**: Rejected due to high latency (~150ms)
**Mumbai**: Considered 但 higher latency (~150ms)
**Osaka**: Rejected due to proximity to Tokyo

### 預算 Considerations

**Original 預算**: $50,000/year
**Actual Cost**: $126,000/year
**Optimized Cost**: $71,000/year
**預算 Gap**: $21,000/year

**Options**:

1. Request 預算 increase
2. Further cost optimization
3. Phased implementation
4. 降低 scope (Seoul only)

**Recommendation**: Request $25K 預算 increase 與 commitment to optimize to $71K
