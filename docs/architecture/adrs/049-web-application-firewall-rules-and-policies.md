---
adr_number: 049
title: "Web Application Firewall (WAF) Rules 和 Policies"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [048, 050, 051, 053]
affected_viewpoints: ["deployment", "operational"]
affected_perspectives: ["security", "performance"]
---

# ADR-049: Web Application Firewall (WAF) Rules 和 Policies

## 狀態

**Accepted** - 2025-10-25

## 上下文

### 問題陳述

The Enterprise E-Commerce Platform 需要comprehensive application-layer (Layer 7) protection against:

- **SQL Injection Attacks**: Malicious SQL queries targeting database
- **Cross-Site Scripting (XSS)**: Injection of malicious scripts
- **HTTP Floods**: Overwhelming application 與 HTTP requests
- **Bot Attacks**: Automated scraping, credential stuffing, inventory hoarding
- **Known Vulnerabilities**: OWASP Top 10 exploits
- **Geo-Based Attacks**: Attacks from high-risk countries
- **Rate Abuse**: Excessive requests from single sources

The platform needs a Web Application Firewall (WAF) that 可以:

- Protect against OWASP Top 10 vulnerabilities
- Implement intelligent 速率限制
- Detect 和 block malicious bots
- 支援 custom rules 用於 application-specific threats
- 提供 real-time monitoring 和 logging
- Minimize false positives
- Scale 與 application traffic

### 業務上下文

**業務驅動因素**：

- **Data Protection**: Protect customer PII 和 payment information
- **Regulatory Compliance**: PCI-DSS, GDPR requirements
- **Brand Reputation**: Security breaches damage customer trust
- **Revenue Protection**: Prevent bot-driven inventory hoarding

**Taiwan-Specific Context**:

- **Frequent Web Attacks**: Taiwan experiences high volume of application-layer attacks
- **E-Commerce Targeting**: Online shopping platforms are prime targets
- **Bot Activity**: High bot traffic from automated scraping 和 fraud attempts
- **Credential Stuffing**: Frequent attempts using leaked credentials

**限制條件**：

- 預算: ~$500/month 用於 WAF (included in DDoS protection 預算)
- 必須 not impact legitimate 用戶體驗
- 必須 integrate 與 CloudFront 和 ALB
- 必須 支援 multi-region deployment

### 技術上下文

**目前狀態**：

- CloudFront distribution configured
- AWS Shield Advanced 啟用d
- No WAF rules configured
- No 速率限制 implemented
- No bot protection

**Attack Vectors**:

- **SQL Injection**: Login, search, product filters
- **XSS**: Product reviews, user profiles, comments
- **CSRF**: State-changing operations (checkout, profile updates)
- **HTTP Floods**: Login, search, checkout endpoints
- **Bot Scraping**: Product catalog, pricing information
- **Credential Stuffing**: Login endpoint 與 leaked credentials

## 決策驅動因素

1. **Security Coverage**: Protect against OWASP Top 10 和 common attacks
2. **False Positive Rate**: < 0.1% legitimate traffic blocked
3. **Performance**: < 5ms latency overhead
4. **Flexibility**: 支援 custom rules 用於 application-specific threats
5. **Visibility**: Comprehensive logging 和 monitoring
6. **Cost-Effectiveness**: Stay within $500/month 預算
7. **Ease of Management**: Manageable rule 複雜的ity
8. **Integration**: Seamless integration 與 CloudFront 和 ALB

## 考慮的選項

### 選項 1： AWS WAF with Managed Rules + Custom Rules (Recommended)

**描述**： AWS WAF with combination of AWS Managed Rules and custom rules

**Rule Sets**:

- AWS Managed Rules (Core Rule Set, Known Bad Inputs, SQL Database, OWASP Top 10)
- Custom 速率限制 rules
- Custom geo-blocking rules
- Custom bot detection rules
- Custom application-specific rules

**優點**：

- ✅ **Comprehensive Protection**: Covers OWASP Top 10 和 common attacks
- ✅ **Managed Rules**: AWS 維持s 和 updates rules automatically
- ✅ **Custom Flexibility**: 可以 add application-specific rules
- ✅ **AWS Integration**: Native integration 與 CloudFront 和 ALB
- ✅ **Real-Time Updates**: Managed rules updated automatically
- ✅ **Cost-Effective**: $5/month Web ACL + $1/million requests
- ✅ **Scalability**: 處理s high traffic volumes
- ✅ **Logging**: Comprehensive logging to S3 和 CloudWatch

**缺點**：

- ⚠️ **Rule Tuning**: Requires initial tuning to minimize false positives
- ⚠️ **複雜的ity**: Multiple rule sets to manage
- ⚠️ **Learning Curve**: Team needs training on WAF configuration

**成本**： ~$500/month (for 100M requests)

**風險**： **Low** - Proven AWS service with extensive production use

### 選項 2： AWS WAF with Managed Rules Only (Basic)

**描述**： Use only AWS Managed Rules without custom rules

**優點**：

- ✅ **簡單的 Setup**: Minimal configuration required
- ✅ **Automatic Updates**: AWS 維持s rules
- ✅ **Lower Cost**: Fewer rules = lower cost

**缺點**：

- ❌ **Limited Protection**: No application-specific rules
- ❌ **No Rate Limiting**: 可以not implement custom rate limits
- ❌ **No Geo-Blocking**: 可以not block specific countries
- ❌ **No Bot Protection**: Limited bot detection capabilities

**成本**： ~$300/month

**風險**： **Medium** - Insufficient for comprehensive protection

### 選項 3： Third-Party WAF (Cloudflare, Imperva)

**描述**： Use third-party WAF service

**優點**：

- ✅ **Advanced Features**: Bot management, DDoS protection, CDN
- ✅ **Specialized Protection**: WAF is core business
- ✅ **Global Network**: 大型的 edge network

**缺點**：

- ❌ **Higher Cost**: $1,000-5,000/month
- ❌ **Vendor Lock-In**: 難以migrate
- ❌ **Integration 複雜的ity**: Requires DNS changes
- ❌ **Data Privacy**: Traffic routed through third-party

**成本**： $1,000-5,000/month

**風險**： **Medium** - Vendor dependency and higher cost

### 選項 4： Open-Source WAF (ModSecurity)

**描述**： Self-hosted open-source WAF

**優點**：

- ✅ **No Licensing Cost**: Open-source
- ✅ **Full Control**: Complete customization

**缺點**：

- ❌ **Operational Overhead**: Requires dedicated team
- ❌ **Maintenance Burden**: Manual rule updates
- ❌ **Scalability Challenges**: 難以scale
- ❌ **No Managed Updates**: 必須 manually update rules

**成本**： $0 licensing + $5,000/month operational overhead

**風險**： **High** - Not suitable for cloud-native architecture

## 決策結果

**選擇的選項**： **AWS WAF with Managed Rules + Custom Rules**

### 理由

AWS WAF 與 managed 和 custom rules被選擇的原因如下：

1. **Comprehensive Protection**: Covers OWASP Top 10 和 application-specific threats
2. **Cost-Effective**: $500/month fits within 預算
3. **AWS Integration**: Seamless integration 與 CloudFront 和 Shield Advanced
4. **Automatic Updates**: Managed rules updated 透過 AWS automatically
5. **Flexibility**: 可以 add custom rules 用於 application-specific threats
6. **Scalability**: 處理s high traffic volumes 沒有 performance impact
7. **Visibility**: Comprehensive logging 和 monitoring

**WAF Rule Architecture**:

**Rule Priority Order** (evaluated in order):

1. **Allow List** (Priority 1-100): Whitelist known 良好的 IPs/User-Agents
2. **Block List** (Priority 101-200): Blacklist known malicious IPs
3. **Rate Limiting** (Priority 201-300): Prevent abuse
4. **AWS Managed Rules** (Priority 301-400): OWASP Top 10, SQL injection, XSS
5. **Custom Rules** (Priority 401-500): Application-specific rules
6. **Default Action**: Allow (after all rules evaluated)

**為何不選 Managed Rules Only**： Insufficient protection 用於 application-specific threats 和 no 速率限制 capabilities.

**為何不選 Third-Party WAF**： Higher cost ($1K-5K/month) 和 vendor lock-in not justified when AWS WAF 提供s comprehensive protection at lower cost.

## 影響分析

### 利害關係人影響

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | Low | Minimal code changes | Documentation 和 training |
| Operations Team | Medium | Need to monitor 和 tune WAF rules | Training, runbooks, dashboards |
| Security Team | Positive | Enhanced security posture | Regular security reviews |
| End Users | None | Transparent protection | N/A |
| Finance Team | Low | $500/month within 預算 | N/A |
| Business Team | Positive | 降低d security risk | N/A |

### 影響半徑

**選擇的影響半徑**： **System**

影響：

- All public-facing endpoints (web, mobile, API)
- CloudFront distribution
- Application Load Balancer
- Logging 和 monitoring systems
- Incident response procedures

### 風險評估

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| False positives blocking legitimate traffic | Medium | High | Careful rule tuning, whitelist 用於 known IPs, gradual rollout |
| Performance degradation | Low | Medium | Monitor latency, optimize rules |
| Rule 複雜的ity | Medium | Medium | Document rules, regular reviews |
| Bypass attacks | Low | High | Regular security audits, penetration testing |
| Cost overrun | Low | Low | Monitor request volume, set 預算 alerts |

**整體風險等級**： **Low**

## 實作計畫

### 第 1 階段： AWS Managed Rules Setup （第 1 週）

- [x] Create WAF Web ACL
- [x] 啟用 AWS Managed Rules:
  - Core Rule Set (CRS)
  - Known Bad Inputs
  - SQL Database Protection
  - Linux Operating System
  - POSIX Operating System
  - Windows Operating System
  - PHP Application
  - WordPress Application
- [x] Configure rule actions (Block, Count, Allow)
- [x] Associate WAF 與 CloudFront distribution
- [x] Test 與 simulated attacks

### 第 2 階段： Rate Limiting Rules （第 2 週）

- [x] Implement global 速率限制 (10,000 req/min)
- [x] Implement per-IP 速率限制 (2,000 req/min)
- [x] Implement per-user 速率限制 (100 req/min)
- [x] Implement endpoint-specific 速率限制:
  - Login: 10 req/min per IP
  - Search: 100 req/min per IP
  - Checkout: 20 req/min per IP
  - API: 1,000 req/min per API key
- [x] Test 速率限制 與 load testing

### 第 3 階段： Geo-Blocking and Custom Rules （第 3 週）

- [x] Configure geo-blocking (optional):
  - Block high-risk countries (if business allows)
  - Allow Taiwan, Japan, US, EU
- [x] Implement IP reputation rules
- [x] Implement User-Agent filtering (block known bad bots)
- [x] Implement custom rules 用於 application-specific threats
- [x] Test custom rules 與 simulated attacks

### 第 4 階段： Logging and Monitoring （第 4 週）

- [x] Configure WAF logging to S3
- [x] Set up CloudWatch metrics 和 alarms
- [x] Create CloudWatch dashboard 用於 WAF monitoring
- [x] Configure SNS notifications 用於 security team
- [x] Set up log analysis (Athena queries)
- [x] Configure automated response (Lambda + EventBridge)

### Phase 5: Tuning and Optimization （第 5 週）

- [x] Analyze false positives
- [x] Tune rule sensitivity
- [x] Add whitelist 用於 known 良好的 IPs
- [x] Optimize rule order 用於 performance
- [x] Document runbooks 和 procedures
- [x] Train operations team

### 回滾策略

**觸發條件**：

- False positive rate > 1% (legitimate traffic blocked)
- Performance degradation > 10ms latency
- Service outage caused 透過 WAF rules
- Cost overrun > 50% of 預算

**回滾步驟**：

1. Change rule actions from Block to Count (monitoring mode)
2. Disable specific rules causing issues
3. Remove WAF association from CloudFront
4. Investigate 和 fix issues
5. Re-deploy 與 corrections

**回滾時間**： < 15 minutes

## 監控和成功標準

### 成功指標

- ✅ **Attack Blocking**: 100% of known attacks blocked
- ✅ **False Positive Rate**: < 0.1% legitimate traffic blocked
- ✅ **Performance**: < 5ms latency overhead
- ✅ **Availability**: No service degradation from WAF
- ✅ **成本**： Stay within $500/month budget
- ✅ **Coverage**: All OWASP Top 10 vulnerabilities protected

### 監控計畫

**CloudWatch Metrics**:

- `AllowedRequests` (count)
- `BlockedRequests` (count)
- `CountedRequests` (count)
- `PassedRequests` (count)
- `SampledRequests` (sample of blocked requests)

**Custom Metrics**:

- Block rate 透過 rule
- Block rate 透過 country
- Block rate 透過 IP
- Block rate 透過 User-Agent
- Rate limiting triggers

**告警**：

- **P0 Critical**: Block rate > 50% (potential false positives 或 major attack)
- **P1 High**: Block rate > 10% (investigate 用於 false positives)
- **P2 Medium**: Unusual traffic patterns
- **P3 Low**: New attack patterns detected

**Security Monitoring**:

- Real-time WAF log analysis (Kinesis + Lambda)
- Attack pattern analysis (Athena queries on S3 logs)
- Geo-location analysis of blocked requests
- Bot detection 和 analysis
- SQL injection attempt analysis
- XSS attempt analysis

**審查時程**：

- **Real-Time**: 24/7 monitoring dashboard
- **Daily**: Review blocked requests 和 false positives
- **Weekly**: Analyze attack patterns 和 tune rules
- **Monthly**: Security review 和 rule optimization
- **Quarterly**: Comprehensive security audit

## 後果

### 正面後果

- ✅ **Enhanced Security**: Protection against OWASP Top 10 和 common attacks
- ✅ **Automatic Updates**: Managed rules updated 透過 AWS
- ✅ **Flexibility**: Custom rules 用於 application-specific threats
- ✅ **Visibility**: Comprehensive logging 和 monitoring
- ✅ **Cost-Effective**: $500/month within 預算
- ✅ **Performance**: < 5ms latency overhead
- ✅ **Scalability**: 處理s high traffic volumes
- ✅ **Compliance**: Helps meet PCI-DSS 和 GDPR requirements

### 負面後果

- ⚠️ **False Positive Risk**: Potential 用於 blocking legitimate traffic
- ⚠️ **Operational Overhead**: Requires monitoring 和 tuning
- ⚠️ **Rule 複雜的ity**: Multiple rule sets to manage
- ⚠️ **Learning Curve**: Team needs training on WAF configuration

### 技術債務

**已識別債務**：

1. Manual rule tuning (acceptable initially)
2. No automated false positive detection
3. Limited bot detection (basic User-Agent filtering)

**債務償還計畫**：

- **Q2 2026**: Implement automated rule tuning based on traffic patterns
- **Q3 2026**: Implement advanced bot detection (AWS WAF Bot Control)
- **Q4 2026**: Implement machine learning-based anomaly detection

## 相關決策

- [ADR-048: DDoS Protection Strategy (Multi-Layer Defense)](048-ddos-protection-strategy.md) - Overall DDoS protection
- [ADR-050: API Security 和 Rate Limiting Strategy](050-api-security-and-rate-limiting-strategy.md) - Application-level protection
- [ADR-051: Input Validation 和 Sanitization Strategy](051-input-validation-and-sanitization-strategy.md) - Application-level validation
- [ADR-053: Security Monitoring 和 Incident Response](053-security-monitoring-and-incident-response.md) - Security operations

## 備註

### AWS Managed Rule Groups

**Core Rule Set (CRS)**:

- General web application protection
- Covers common vulnerabilities
- Recommended 用於 all applications

**Known Bad Inputs**:

- Blocks requests 與 known malicious patterns
- Protects against common exploits

**SQL Database Protection**:

- Protects against SQL injection attacks
- Covers MySQL, PostgreSQL, Oracle, SQL Server

**Linux/Windows Operating System**:

- Protects against OS-specific exploits
- Blocks command injection attempts

**PHP/WordPress Application**:

- Protects against PHP 和 WordPress vulnerabilities
- Blocks common CMS exploits

### Rate Limiting Configuration

**Global Rate Limiting**:

```json
{
  "Name": "GlobalRateLimit",
  "Priority": 201,
  "Statement": {
    "RateBasedStatement": {
      "Limit": 10000,
      "AggregateKeyType": "IP"
    }
  },
  "Action": {
    "Block": {}
  }
}
```

**Per-IP Rate Limiting**:

```json
{
  "Name": "PerIPRateLimit",
  "Priority": 202,
  "Statement": {
    "RateBasedStatement": {
      "Limit": 2000,
      "AggregateKeyType": "IP"
    }
  },
  "Action": {
    "Block": {}
  }
}
```

**Endpoint-Specific Rate Limiting (Login)**:

```json
{
  "Name": "LoginRateLimit",
  "Priority": 203,
  "Statement": {
    "RateBasedStatement": {
      "Limit": 10,
      "AggregateKeyType": "IP",
      "ScopeDownStatement": {
        "ByteMatchStatement": {
          "FieldToMatch": {
            "UriPath": {}
          },
          "PositionalConstraint": "STARTS_WITH",
          "SearchString": "/api/v1/auth/login"
        }
      }
    }
  },
  "Action": {
    "Block": {}
  }
}
```

### Geo-Blocking Configuration

**Block High-Risk Countries** (Optional):

```json
{
  "Name": "GeoBlockHighRisk",
  "Priority": 101,
  "Statement": {
    "GeoMatchStatement": {
      "CountryCodes": ["CN", "RU", "KP"]
    }
  },
  "Action": {
    "Block": {}
  }
}
```

**Note**: Geo-blocking 應該 be used carefully as it may block legitimate users. Consider business impact before implementing.

### Custom Rule Examples

**Block Known Malicious IPs**:

```json
{
  "Name": "BlockMaliciousIPs",
  "Priority": 102,
  "Statement": {
    "IPSetReferenceStatement": {
      "Arn": "arn:aws:wafv2:region:account:regional/ipset/malicious-ips/id"
    }
  },
  "Action": {
    "Block": {}
  }
}
```

**Block Bad User-Agents**:

```json
{
  "Name": "BlockBadUserAgents",
  "Priority": 401,
  "Statement": {
    "ByteMatchStatement": {
      "FieldToMatch": {
        "SingleHeader": {
          "Name": "user-agent"
        }
      },
      "PositionalConstraint": "CONTAINS",
      "SearchString": "bot|crawler|scraper"
    }
  },
  "Action": {
    "Block": {}
  }
}
```

### WAF Logging Configuration

**S3 Logging**:

- Bucket: `waf-logs-{account-id}-{region}`
- Prefix: `AWSLogs/{account-id}/WAFLogs/{region}/`
- Retention: 90 天
- Format: JSON

**CloudWatch Logs**:

- Log Group: `/aws/waf/cloudfront`
- Retention: 30 天
- Metrics: Extracted from logs

**Kinesis Data Firehose**:

- Real-time log streaming
- Lambda processing 用於 alerts
- S3 backup

### Cost Breakdown

**Monthly Costs** (for 100M requests):

- Web ACL: $5/month
- Rules: $1/rule/month × 20 rules = $20/month
- Requests: $0.60/million requests × 100M = $60/month
- Managed Rule Groups: $10/rule group/month × 5 groups = $50/month
- Logging: $0.50/GB × 100GB = $50/month
- **Total**: ~$185/month (well within $500 預算)

**Cost Optimization**:

- Use Count mode 用於 non-critical rules (no charge)
- Optimize rule order (evaluate cheaper rules first)
- Use sampling 用於 logging (降低 log volume)

### Emergency Procedures

**During Active Attack**:

1. **Immediate**: Verify WAF is blocking malicious requests
2. **5 minutes**: Analyze attack patterns in WAF logs
3. **10 minutes**: Adjust rate limits if needed
4. **15 minutes**: Add custom rules 用於 attack-specific patterns
5. **30 minutes**: Communicate 與 stakeholders
6. **Post-Attack**: Conduct post-mortem 和 update rules

**False Positive Response**:

1. **Immediate**: Identify affected rule
2. **5 minutes**: Change rule action from Block to Count
3. **10 minutes**: Analyze blocked requests
4. **15 minutes**: Add whitelist 用於 legitimate traffic
5. **30 minutes**: Re-啟用 rule 與 whitelist
6. **Post-Incident**: Document 和 update runbooks

---

**文檔狀態**： ✅ Accepted  
**上次審查**： 2025-10-25  
**下次審查**： 2026-01-25 （每季）
