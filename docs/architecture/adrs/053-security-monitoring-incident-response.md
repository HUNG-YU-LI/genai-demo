---
adr_number: 053
title: "Security Monitoring 和 Incident Response"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [008, 043, 052, 054]
affected_viewpoints: ["operational", "deployment"]
affected_perspectives: ["security", "availability"]
---

# ADR-053: Security Monitoring 和 Incident Response

## 狀態

**Accepted** - 2025-10-25

## 上下文

### 問題陳述

The Enterprise E-Commerce Platform 需要comprehensive security monitoring 和 incident response capabilities to:

- Detect security threats in real-time
- Respond to security incidents rapidly
- 維持 audit trails 用於 compliance
- Prevent data breaches 和 unauthorized access
- Meet regulatory requirements 用於 security monitoring
- Protect against sophisticated cyber attacks

Taiwan's cyber security environment presents unique challenges:

- Frequent DDoS attacks from state-sponsored actors
- Advanced Persistent Threat (APT) campaigns
- High-value e-commerce platform as attractive target
- 需要 24/7 security operations
- Regulatory compliance (Taiwan Cyber Security Management Act)

### 業務上下文

**業務驅動因素**：

- Protect customer data 和 business operations
- 維持 platform availability 和 reputation
- Comply 與 security regulations
- Minimize financial impact of security incidents
- 啟用 rapid incident response 和 recovery

**限制條件**：

- 必須 operate 24/7 與 minimal false positives
- 可以not impact system performance
- 必須 integrate 與 existing monitoring (ADR-008)
- 預算: $5,000/month 用於 security monitoring tools

### 技術上下文

**目前狀態**：

- Basic CloudWatch monitoring (ADR-008)
- No dedicated security monitoring
- No intrusion detection system
- No security incident response process
- Manual security log analysis

**需求**：

- Real-time threat detection
- Automated incident response
- Comprehensive audit logging
- Security event correlation
- Threat intelligence integration
- 24/7 security operations center (SOC)

## 決策驅動因素

1. **Detection Speed**: Detect threats in real-time (< 1 minute)
2. **Response Time**: Respond to incidents rapidly (< 15 minutes)
3. **Coverage**: Monitor all attack vectors comprehensively
4. **Accuracy**: Minimize false positives (< 5%)
5. **Compliance**: Meet regulatory audit requirements
6. **Scalability**: 處理 100K+ events per second
7. **成本**： Optimize operational costs
8. **Integration**: Integrate 與 existing AWS infrastructure

## 考慮的選項

### 選項 1： AWS Native Security Monitoring (Recommended)

**描述**： Comprehensive security monitoring using AWS native services

**Components**:

- **AWS GuardDuty**: Threat detection 用於 AWS accounts 和 workloads
- **AWS Security Hub**: Centralized security findings management
- **AWS CloudTrail**: API audit logging
- **VPC Flow Logs**: Network traffic monitoring
- **AWS WAF Logs**: Web application firewall logs
- **Amazon EventBridge**: Event-driven automation
- **AWS Lambda**: Automated incident response
- **Amazon SNS**: Alert notifications

**優點**：

- ✅ Native AWS integration (no agents required)
- ✅ Comprehensive threat detection (ML-powered)
- ✅ Automated response capabilities
- ✅ Centralized security management
- ✅ Compliance-ready audit trails
- ✅ Scalable 和 cost-effective
- ✅ Continuous threat intelligence updates
- ✅ Low 營運開銷

**缺點**：

- ⚠️ Limited customization compared to SIEM
- ⚠️ AWS-specific (not multi-cloud)
- ⚠️ Additional cost 用於 GuardDuty 和 Security Hub

**成本**： $3,000/month (GuardDuty $1,500, Security Hub $500, storage $1,000)

**風險**： **Low** - Proven AWS services

### 選項 2： Third-Party SIEM (Splunk, ELK Stack)

**描述**： Deploy dedicated Security Information and Event Management system

**優點**：

- ✅ Advanced correlation 和 analytics
- ✅ Customizable detection rules
- ✅ Multi-cloud 支援
- ✅ 豐富的 visualization 和 reporting
- ✅ Extensive integration ecosystem

**缺點**：

- ❌ High cost ($10,000-20,000/month)
- ❌ 複雜的 deployment 和 maintenance
- ❌ Requires dedicated security team
- ❌ Performance overhead
- ❌ Steep learning curve

**成本**： $15,000/month (Splunk Enterprise Security)

**風險**： **Medium** - High operational complexity

### 選項 3： Managed Security Service Provider (MSSP)

**描述**： Outsource security monitoring to third-party SOC

**優點**：

- ✅ 24/7 professional monitoring
- ✅ Expert incident response
- ✅ No internal SOC required
- ✅ Compliance 支援

**缺點**：

- ❌ Very high cost ($20,000-50,000/month)
- ❌ Less control over security operations
- ❌ Data privacy concerns
- ❌ Vendor dependency
- ❌ Communication overhead

**成本**： $30,000/month

**風險**： **Medium** - Vendor dependency

## 決策結果

**選擇的選項**： **AWS Native Security Monitoring (Option 1)**

### 理由

AWS native security monitoring被選擇的原因如下：

1. **Cost-Effective**: $3,000/month vs $15,000+ 用於 alternatives
2. **Native Integration**: Seamless integration 與 AWS infrastructure
3. **Automated Detection**: ML-powered threat detection 與 continuous updates
4. **Scalability**: 處理s platform growth 沒有 additional infrastructure
5. **Low Overhead**: No agents 或 additional infrastructure required
6. **Compliance-Ready**: Built-in compliance reporting 和 audit trails
7. **Rapid Deployment**: 可以 be 啟用d in hours, not 週
8. **Automated Response**: Event-driven automation 與 Lambda

### Architecture

```text
┌─────────────────────────────────────────────────────────────┐
│                    Security Monitoring                       │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │  GuardDuty   │  │ Security Hub │  │  CloudTrail  │     │
│  │ Threat       │  │ Centralized  │  │ API Audit    │     │
│  │ Detection    │  │ Findings     │  │ Logging      │     │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘     │
│         │                  │                  │              │
│         └──────────────────┼──────────────────┘              │
│                            │                                 │
│                    ┌───────▼────────┐                       │
│                    │  EventBridge   │                       │
│                    │  Event Router  │                       │
│                    └───────┬────────┘                       │
│                            │                                 │
│         ┌──────────────────┼──────────────────┐             │
│         │                  │                  │             │
│  ┌──────▼───────┐  ┌──────▼───────┐  ┌──────▼───────┐    │
│  │   Lambda     │  │     SNS      │  │   Lambda     │    │
│  │ Auto-Block   │  │   Alerts     │  │  Forensics   │    │
│  │   Malicious  │  │ Notification │  │  Collection  │    │
│  │     IPs      │  │              │  │              │    │
│  └──────────────┘  └──────────────┘  └──────────────┘    │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### AWS GuardDuty Configuration

**Threat Detection Categories**:

- **Reconnaissance**: Port s可以ning, unusual API calls
- **Instance Compromise**: Malware, cryptocurrency mining, backdoor communication
- **Account Compromise**: Credential theft, unusual API activity
- **Bucket Compromise**: S3 data exfiltration, policy changes
- **Kubernetes Threats**: Container compromise, privilege escalation

**Detection Mechanisms**:

- Machine learning models
- Threat intelligence feeds
- Anomaly detection
- Behavioral analysis

**Implementation**:

```python
# Enable GuardDuty via CDK
guardduty = aws_guardduty.CfnDetector(
    self, "GuardDuty",
    enable=True,
    finding_publishing_frequency="FIFTEEN_MINUTES",
    data_sources=aws_guardduty.CfnDetector.CFNDataSourceConfigurationsProperty(
        s3_logs=aws_guardduty.CfnDetector.CFNS3LogsConfigurationProperty(enable=True),
        kubernetes=aws_guardduty.CfnDetector.CFNKubernetesConfigurationProperty(
            audit_logs=aws_guardduty.CfnDetector.CFNKubernetesAuditLogsConfigurationProperty(enable=True)
        )
    )
)
```

### AWS Security Hub Configuration

**Security Standards**:

- AWS Foundational Security Best Practices
- CIS AWS Foundations Benchmark
- PCI DSS
- NIST Cybersecurity Framework

**Integration**:

- GuardDuty findings
- AWS Config compliance checks
- IAM Access Analyzer findings
- Macie sensitive data findings
- Inspector vulnerability findings

**Implementation**:

```python
# Enable Security Hub via CDK
security_hub = aws_securityhub.CfnHub(
    self, "SecurityHub",
    enable_default_standards=True,
    control_finding_generator="SECURITY_CONTROL"
)

# Enable standards
aws_securityhub.CfnStandard(
    self, "CISStandard",
    standards_arn="arn:aws:securityhub:region::standards/cis-aws-foundations-benchmark/v/1.4.0"
)
```

### Intrusion Detection System (IDS)

**Approach**: Network-based IDS using Suricata on EC2

**Deployment**:

- Suricata instances in each VPC
- Mirror VPC traffic to IDS instances
- Analyze traffic 用於 malicious patterns
- Alert on suspicious activity

**Detection Rules**:

- OWASP Top 10 attack patterns
- Known malware signatures
- Anomalous traffic patterns
- Data exfiltration attempts
- Command 和 control communication

**Implementation**:

```yaml
# Suricata configuration
vars:
  address-groups:
    HOME_NET: "[10.0.0.0/8]"
    EXTERNAL_NET: "!$HOME_NET"

rule-files:

  - suricata.rules
  - emerging-threats.rules
  - custom-rules.rules

outputs:

  - eve-log:

      enabled: yes
      filetype: regular
      filename: eve.json
      types:

        - alert
        - http
        - dns
        - tls

```

### Automated Incident Response

**Response Actions**:

1. **Auto-Block Malicious IPs**: Update WAF 和 Security Groups
2. **Isolate Compromised Instances**: Remove from load balancer, restrict network
3. **Collect Forensics**: Snapshot volumes, capture memory, save logs
4. **Notify Security Team**: Send alerts via SNS, PagerDuty
5. **Create Incident Ticket**: Automatically create Jira ticket

**Implementation**:

```python
# Lambda function for automated response
def lambda_handler(event, context):
    finding = event['detail']['findings'][0]
    severity = finding['Severity']['Label']
    finding_type = finding['Type']
    
    if severity in ['HIGH', 'CRITICAL']:
        # Extract malicious IP
        if 'RemoteIpDetails' in finding['Service']['Action']:
            malicious_ip = finding['Service']['Action']['RemoteIpDetails']['IpAddressV4']
            
            # Block IP in WAF
            waf_client.update_ip_set(
                Name='BlockedIPs',
                Scope='REGIONAL',
                Addresses=[f"{malicious_ip}/32"]
            )
            
            # Block IP in Security Group
            ec2_client.revoke_security_group_ingress(
                GroupId=security_group_id,
                IpPermissions=[{
                    'IpProtocol': '-1',
                    'IpRanges': [{'CidrIp': f"{malicious_ip}/32"}]
                }]
            )
            
            # Send alert
            sns_client.publish(
                TopicArn=alert_topic_arn,
                Subject=f"Security Alert: {finding_type}",
                Message=f"Blocked malicious IP: {malicious_ip}"
            )
            
            # Create incident ticket
            create_incident_ticket(finding)
    
    return {'statusCode': 200}
```

### Security Event Correlation

**SIEM Capabilities**:

- Correlate events 跨 multiple sources
- Detect multi-stage attacks
- Identify attack patterns
- Generate security insights

**Implementation 與 CloudWatch Insights**:

```sql
-- Detect brute force attacks
fields @timestamp, userIdentity.principalId, sourceIPAddress, errorCode
| filter eventName = "ConsoleLogin" and errorCode = "Failed authentication"
| stats count() as failedAttempts by sourceIPAddress, userIdentity.principalId
| filter failedAttempts > 5
| sort failedAttempts desc

-- Detect data exfiltration
fields @timestamp, sourceIPAddress, bytesOut
| filter bytesOut > 1000000000  -- 1GB
| stats sum(bytesOut) as totalBytes by sourceIPAddress
| filter totalBytes > 10000000000  -- 10GB
| sort totalBytes desc
```

### Threat Intelligence Integration

**Sources**:

- AWS Threat Intelligence
- AlienVault OTX
- Abuse.ch
- Taiwan CERT threat feeds
- Custom threat intelligence

**Integration**:

- Automatically update WAF IP blocklists
- En豐富的 GuardDuty findings
- Correlate 與 known threat actors
- Share intelligence 與 Taiwan CERT

**Implementation**:

```python
# Update threat intelligence feeds
def update_threat_feeds():
    # Fetch threat intelligence
    malicious_ips = fetch_threat_intelligence()
    
    # Update WAF IP set
    waf_client.update_ip_set(
        Name='ThreatIntelligenceIPs',
        Scope='REGIONAL',
        Addresses=[f"{ip}/32" for ip in malicious_ips]
    )
    
    # Update GuardDuty threat list
    guardduty_client.create_threat_intel_set(
        DetectorId=detector_id,
        Name='CustomThreatIntel',
        Format='TXT',
        Location=f"s3://{bucket}/threat-intel.txt",
        Activate=True
    )
```

### 24/7 Security Operations

**SOC Structure**:

- **Tier 1**: Alert triage 和 initial response (outsourced to Taiwan SOC)
- **Tier 2**: Incident investigation 和 remediation (internal team)
- **Tier 3**: Advanced threat hunting 和 forensics (internal team)

**On-Call Rotation**:

- 24/7 on-call coverage
- Primary 和 secondary on-call engineers
- Escalation to security lead 用於 critical incidents
- PagerDuty integration 用於 alerting

**Incident Classification**:

- **P0 (Critical)**: Active data breach, system compromise
- **P1 (High)**: Attempted breach, high-severity vulnerability
- **P2 (Medium)**: Security policy violation, medium-severity finding
- **P3 (Low)**: Informational finding, low-severity issue

**Response SLAs**:

- P0: 15 minutes response, 1 hour resolution
- P1: 1 hour response, 4 hours resolution
- P2: 4 hours response, 24 hours resolution
- P3: 24 hours response, 1 週 resolution

## 影響分析

### 利害關係人影響

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Security Team | High | New monitoring tools 和 processes | Training, runbooks, automation |
| Operations Team | Medium | Additional alerts 和 incidents | Alert tuning, automation |
| Development Team | Low | Security findings to address | Security training, tools |
| End Users | None | Transparent security monitoring | N/A |
| Compliance Team | Positive | Enhanced audit capabilities | Regular compliance reports |

### 影響半徑

**選擇的影響半徑**： **System**

影響：

- All AWS accounts 和 resources
- All network traffic
- All API calls
- All security events
- Incident response processes

### 風險評估

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| False positive alerts | High | Medium | Alert tuning, ML training, feedback loop |
| Alert fatigue | Medium | Medium | Prioritization, automation, aggregation |
| Missed threats | Low | Critical | Multiple detection layers, threat intelligence |
| Performance impact | Low | Low | Serverless architecture, efficient queries |
| Cost overrun | Medium | Medium | 預算 alerts, cost optimization |

**整體風險等級**： **Low**

## 實作計畫

### 第 1 階段： Core Monitoring （第 1-2 週）

- [ ] 啟用 AWS GuardDuty in all regions
- [ ] 啟用 AWS Security Hub
- [ ] Configure CloudTrail logging
- [ ] 啟用 VPC Flow Logs
- [ ] Set up S3 bucket 用於 log storage
- [ ] Configure log retention policies

### 第 2 階段： Automated Response （第 3-4 週）

- [ ] Create EventBridge rules 用於 security events
- [ ] Implement Lambda functions 用於 auto-response
- [ ] Configure SNS topics 用於 alerts
- [ ] Integrate 與 PagerDuty
- [ ] Create incident response runbooks
- [ ] Test automated response workflows

### 第 3 階段： IDS Deployment （第 5-6 週）

- [ ] Deploy Suricata instances
- [ ] Configure VPC traffic mirroring
- [ ] Set up detection rules
- [ ] Integrate 與 Security Hub
- [ ] Test detection capabilities
- [ ] Tune false positive rates

### 第 4 階段： SOC Operations （第 7-8 週）

- [ ] Establish on-call rotation
- [ ] Create security dashboards
- [ ] Implement threat intelligence feeds
- [ ] Set up security event correlation
- [ ] Conduct tabletop exercises
- [ ] Document incident response procedures

### 回滾策略

**觸發條件**：

- Excessive false positives (> 10% of alerts)
- Performance degradation (> 5% overhead)
- Cost overrun (> 150% of 預算)
- Operational issues

**回滾步驟**：

1. Disable automated response actions
2. 降低 GuardDuty sensitivity
3. Pause IDS deployment
4. Revert to manual monitoring
5. Investigate 和 fix issues

**回滾時間**： < 2 hours

## 監控和成功標準

### 成功指標

- ✅ Threat detection time: < 1 minute
- ✅ Incident 回應時間: < 15 minutes (P0)
- ✅ False positive rate: < 5%
- ✅ Security event coverage: > 95%
- ✅ Compliance audit pass rate: 100%
- ✅ Mean time to detect (MTTD): < 5 minutes
- ✅ Mean time to respond (MTTR): < 30 minutes

### 監控計畫

**CloudWatch Metrics**:

- `security.threats.detected` (count 透過 severity)
- `security.incidents.created` (count 透過 priority)
- `security.response.time` (histogram)
- `security.false.positives` (count)
- `security.compliance.score` (gauge)

**告警**：

- Critical security finding detected
- Incident response SLA breach
- False positive rate > 10%
- Compliance score < 90%
- Threat intelligence feed update failure

**審查時程**：

- Daily: Review security alerts 和 incidents
- Weekly: Analyze threat trends 和 patterns
- Monthly: Compliance 和 audit review
- Quarterly: Security posture assessment

## 後果

### 正面後果

- ✅ **Real-Time Detection**: Detect threats within minutes
- ✅ **Automated Response**: Rapid incident response 與 automation
- ✅ **Comprehensive Coverage**: Monitor all attack vectors
- ✅ **Compliance-Ready**: Built-in audit trails 和 reporting
- ✅ **Cost-Effective**: $3,000/month 用於 enterprise-grade monitoring
- ✅ **Scalable**: 處理s platform growth automatically
- ✅ **Low Overhead**: No agents 或 additional infrastructure

### 負面後果

- ⚠️ **Alert Fatigue**: Potential 用於 too many alerts initially
- ⚠️ **False Positives**: ML models 需要tuning
- ⚠️ **Operational Overhead**: 24/7 SOC operations required
- ⚠️ **Learning Curve**: Team needs training on new tools
- ⚠️ **成本**： $3,000/month ongoing operational cost

### 技術債務

**已識別債務**：

1. Manual threat intelligence integration (acceptable initially)
2. Basic security event correlation (acceptable 用於 MVP)
3. No advanced threat hunting capabilities
4. Limited forensics automation

**債務償還計畫**：

- **Q2 2026**: Implement advanced threat hunting 與 Athena
- **Q3 2026**: Automate forensics collection 和 analysis
- **Q4 2026**: Integrate 與 SOAR platform 用於 orchestration
- **2027**: Implement AI-powered threat detection

## 相關決策

- [ADR-008: Use CloudWatch + X-Ray + Grafana 用於 Observability](008-use-cloudwatch-xray-grafana-for-observability.md) - Base monitoring infrastructure
- [ADR-043: Observability 用於 Multi-Region Operations](043-observability-multi-region-operations.md) - Multi-region monitoring
- [ADR-052: Authentication Security Hardening](052-authentication-security-hardening.md) - Authentication security
- [ADR-054: Data Loss Prevention (DLP) Strategy](054-data-loss-prevention-strategy.md) - Data protection monitoring

## 備註

### GuardDuty Pricing

- VPC Flow Logs: $1.00 per GB analyzed
- CloudTrail Events: $4.40 per million events
- S3 Data Events: $0.80 per million events
- EKS Audit Logs: $0.40 per GB analyzed

**Estimated Monthly Cost**: $1,500 用於 100K users

### Security Hub Pricing

- Security checks: $0.0010 per check
- Finding ingestion: $0.00003 per finding
- Estimated: $500/month

### Incident Response Runbooks

1. **Data Breach Response**
2. **DDoS Attack Response**
3. **Account Compromise Response**
4. **Malware Infection Response**
5. **Data Exfiltration Response**

---

**文檔狀態**： ✅ Accepted  
**上次審查**： 2025-10-25  
**下次審查**： 2026-01-25 （每季）
