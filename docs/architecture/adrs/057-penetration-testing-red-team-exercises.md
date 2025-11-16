---
adr_number: 057
title: "Penetration Testing 和 Red Team Exercises"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [053, 054, 055, 056]
affected_viewpoints: ["operational"]
affected_perspectives: ["security", "availability"]
---

# ADR-057: Penetration Testing 和 Red Team Exercises

## 狀態

**Accepted** - 2025-10-25

## 上下文

### 問題陳述

The Enterprise E-Commerce Platform 需要comprehensive security testing to:

- Identify security vulnerabilities before attackers do
- Validate security controls effectiveness
- Test incident response capabilities
- Comply 與 security standards (PCI-DSS, ISO 27001)
- 改善 security posture continuously
- Prepare 用於 real-world attacks

Taiwan's cyber security environment presents unique challenges:

- Sophisticated APT attacks from state-sponsored actors
- High-value e-commerce platform as attractive target
- Need to test defenses against advanced threats
- Regulatory requirements 用於 security testing
- Limited internal security expertise

### 業務上下文

**業務驅動因素**：

- Protect customer data 和 business operations
- 維持 platform reputation 和 trust
- Comply 與 PCI-DSS requirement 11.3 (penetration testing)
- Validate security investments
- 改善 incident response readiness

**限制條件**：

- 必須 not disrupt production services
- 可以not expose real customer data
- 必須 comply 與 legal 和 ethical guidelines
- 預算: $50,000/year 用於 external testing
- Internal team capacity: 2 security engineers

### 技術上下文

**目前狀態**：

- No regular penetration testing
- No red team exercises
- Ad-hoc security assessments
- No vulnerability validation process
- Limited security testing expertise
- No attack simulation framework

**需求**：

- Regular penetration testing (quarterly)
- Annual red team exercises
- Automated security testing
- Vulnerability validation
- Incident response testing
- Compliance-ready reporting

## 決策驅動因素

1. **Coverage**: Comprehensive testing of all attack vectors
2. **Frequency**: Regular testing to catch new vulnerabilities
3. **Realism**: Simulate real-world attack scenarios
4. **Compliance**: Meet PCI-DSS 和 ISO 27001 requirements
5. **成本**： Optimize testing budget
6. **Expertise**: Leverage external expertise
7. **Safety**: Minimize risk to production systems
8. **Learning**: 改善 team security skills

## 考慮的選項

### 選項 1： Hybrid Penetration Testing Program (Recommended)

**描述**： Combination of external penetration testing, internal security testing, and red team exercises

**Components**:

- **Quarterly External Penetration Testing**: Professional security firm
- **Semi-Annual Internal Testing**: Internal security team
- **Annual Red Team Exercise**: Simulate APT attack
- **Continuous Automated Testing**: DAST, SAST tools
- **Bug Bounty Program**: Crowdsourced security testing
- **Post-Major-Update Testing**: Test after signifi可以t changes

**優點**：

- ✅ Comprehensive coverage (external + internal + automated)
- ✅ Regular testing cadence
- ✅ Real-world attack simulation
- ✅ Cost-effective ($50K/year)
- ✅ Compliance-ready
- ✅ Continuous 改善ment
- ✅ Team skill development

**缺點**：

- ⚠️ Requires coordination
- ⚠️ Internal team capacity needed
- ⚠️ Potential 用於 service disruption

**成本**： $50,000/year ($30K external, $10K red team, $10K tools/bounty)

**風險**： **Low** - Industry best practices

### 選項 2： External Penetration Testing Only

**描述**： Rely solely on external security firms

**優點**：

- ✅ Professional expertise
- ✅ Compliance-ready reports
- ✅ No internal capacity needed

**缺點**：

- ❌ Limited testing frequency
- ❌ No continuous testing
- ❌ No team skill development
- ❌ Higher cost 用於 frequent testing

**成本**： $80,000/year (quarterly external testing)

**風險**： **Medium** - Gaps between tests

### 選項 3： Internal Testing Only

**描述**： Build internal security testing capability

**優點**：

- ✅ Continuous testing capability
- ✅ Deep system knowledge
- ✅ Lower long-term cost

**缺點**：

- ❌ Limited expertise
- ❌ Potential bias
- ❌ Compliance concerns
- ❌ High initial investment

**成本**： $100,000/year (2 security engineers)

**風險**： **High** - Insufficient expertise

## 決策結果

**選擇的選項**： **Hybrid Penetration Testing Program (Option 1)**

### 理由

Hybrid penetration testing program被選擇的原因如下：

1. **Comprehensive**: Combines external expertise 與 internal capability
2. **Cost-Effective**: $50K/year vs $80K+ 用於 external-only
3. **Continuous**: Regular testing plus automated s可以ning
4. **Compliance**: Meets PCI-DSS 和 ISO 27001 requirements
5. **Realistic**: Red team exercises simulate real attacks
6. **Skill Development**: Internal team learns from external experts
7. **Flexible**: 可以 adjust testing focus based on threats

### Penetration Testing Scope

**Testing Categories**:

**1. Web Application Testing**

- OWASP Top 10 vulnerabilities
- Authentication 和 authorization
- Session management
- Input validation
- Business logic flaws
- API security

**2. API Endpoint Testing**

- REST API security
- GraphQL security (if applicable)
- Authentication bypass
- Authorization flaws
- Rate limiting bypass
- Data exposure

**3. Infrastructure Testing**

- Network segmentation
- Firewall rules
- Cloud configuration
- Container security
- Kubernetes security
- Database security

**4. Social Engineering**

- Phishing campaigns
- Pretexting
- Physical security (optional)
- Insider threat simulation

**Testing Methodology**:

- **Black Box**: No prior knowledge (external attacker perspective)
- **Gray Box**: Limited knowledge (compromised user perspective)
- **White Box**: Full knowledge (insider threat perspective)

### Quarterly External Penetration Testing

**Testing Schedule**:

- **Q1**: Web application 和 API testing
- **Q2**: Infrastructure 和 cloud security testing
- **Q3**: Full-scope testing (web + infrastructure)
- **Q4**: Post-major-update testing

**Testing Process**:

**Week 1: Planning 和 Scoping**

- Define testing scope 和 objectives
- Identify critical assets 和 systems
- Establish rules of engagement
- Set up testing environment
- Coordinate 與 operations team

**Week 2-3: Testing Execution**

- Reconnaissance 和 information gathering
- Vulnerability identification
- Exploitation attempts
- Privilege escalation testing
- Lateral movement testing
- Data exfiltration simulation

**Week 4: Reporting 和 Remediation**

- Detailed findings report
- Risk prioritization
- Remediation recommendations
- Executive summary
- Remediation validation (optional)

**Vendor Selection Criteria**:

- CREST 或 OSCP certified testers
- Experience 與 e-commerce platforms
- Understanding of Taiwan cyber threats
- PCI-DSS testing experience
- 良好的 communication 和 reporting

**Implementation**:

```yaml
# Penetration Testing Checklist
penetration_testing:
  frequency: quarterly
  vendor: "Professional Security Firm"
  scope:

    - web_applications
    - api_endpoints
    - infrastructure
    - cloud_configuration
  
  methodology:

    - black_box
    - gray_box
    - white_box
  
  deliverables:

    - detailed_findings_report
    - executive_summary
    - remediation_recommendations
    - retest_report
  
  cost_per_test: $7,500
  annual_cost: $30,000
```

### Semi-Annual Internal Testing

**Internal Testing Focus**:

- Validate external testing findings
- Test new features 和 changes
- Continuous security assessment
- Team skill development

**Testing Tools**:

- **Burp Suite Professional**: Web application testing
- **OWASP ZAP**: Automated s可以ning
- **Metasploit**: Exploitation framework
- **Nmap**: Network s可以ning
- **Nikto**: Web server s可以ning

**Internal Testing Process**:

```bash
# Automated security scanning
#!/bin/bash

# Web application scanning with OWASP ZAP
docker run -t owasp/zap2docker-stable zap-baseline.py \
  -t https://staging.example.com \
  -r zap-report.html

# API security testing
docker run -t owasp/zap2docker-stable zap-api-scan.py \
  -t https://api.staging.example.com/openapi.json \
  -f openapi \
  -r api-report.html

# Network scanning
nmap -sV -sC -oA nmap-scan staging.example.com

# SSL/TLS testing
testssl.sh --full staging.example.com

# Container security scanning
trivy image app:latest --severity HIGH,CRITICAL
```

### Annual Red Team Exercise

**Red Team Objectives**:

- Simulate Advanced Persistent Threat (APT) attack
- Test detection 和 response capabilities
- Identify security gaps
- Validate incident response procedures
- 改善 security awareness

**Attack Scenarios**:

**Scenario 1: External Breach**

- Initial access via phishing 或 vulnerability exploitation
- Establish persistence
- Privilege escalation
- Lateral movement
- Data exfiltration
- Cover tracks

**Scenario 2: Insider Threat**

- Compromised employee account
- Abuse of legitimate access
- Data theft
- Sabotage attempts

**Scenario 3: Supply Chain Attack**

- Compromised third-party dependency
- Malicious code injection
- Backdoor installation

**Red Team Exercise Process**:

**Phase 1: Planning （第 1-2 週）**

- Define objectives 和 scope
- Establish rules of engagement
- Identify target systems
- Coordinate 與 blue team (limited disclosure)
- Set up command 和 control infrastructure

**Phase 2: Execution （第 3-6 週）**

- Reconnaissance 和 intelligence gathering
- Initial access attempts
- Establish foothold
- Privilege escalation
- Lateral movement
- Achieve objectives (data exfiltration, etc.)
- 維持 persistence

**Phase 3: Reporting （第 7-8 週）**

- Detailed attack timeline
- Techniques, tactics, 和 procedures (TTPs)
- Detection gaps identified
- Blue team performance analysis
- Recommendations 用於 改善ment

**Implementation**:

```yaml
# Red Team Exercise Plan
red_team_exercise:
  frequency: annual
  duration: 6_weeks
  team: "External Red Team + Internal Security"
  
  objectives:

    - test_detection_capabilities
    - validate_incident_response
    - identify_security_gaps
    - improve_security_awareness
  
  scenarios:

    - external_breach
    - insider_threat
    - supply_chain_attack
  
  rules_of_engagement:

    - no_production_data_exfiltration
    - no_service_disruption
    - coordinate_with_operations
    - stop_on_critical_issues
  
  cost: $10,000
```

### Continuous Automated Testing

**DAST (Dynamic Application Security Testing)**:

```yaml
# GitHub Actions - DAST Scanning
name: DAST Scan

on:
  schedule:

    - cron: '0 2 * * *'  # Daily at 2 AM

  workflow_dispatch:

jobs:
  dast-scan:
    runs-on: ubuntu-latest
    steps:

      - name: OWASP ZAP Scan

        uses: zaproxy/action-full-scan@v0.4.0
        with:
          target: 'https://staging.example.com'
          rules_file_name: '.zap/rules.tsv'
          cmd_options: '-a'
      
      - name: Upload SARIF results

        uses: github/codeql-action/upload-sarif@v2
        with:
          sarif_file: results.sarif
```

**SAST (Static Application Security Testing)**:

```yaml
# GitHub Actions - SAST Scanning
name: SAST Scan

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  sast-scan:
    runs-on: ubuntu-latest
    steps:

      - uses: actions/checkout@v3
      
      - name: Run Semgrep

        uses: returntocorp/semgrep-action@v1
        with:
          config: >-
            p/security-audit
            p/owasp-top-ten
            p/java
      
      - name: Run SonarQube

        uses: sonarsource/sonarqube-scan-action@master
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
          SONAR_HOST_URL: ${{ secrets.SONAR_HOST_URL }}
```

### Bug Bounty Program

**Program Structure**:

- **Platform**: HackerOne 或 Bugcrowd
- **Scope**: Production web application 和 APIs
- **預算**: $10,000/year
- **Rewards**: $100-$5,000 per vulnerability

**Reward Tiers**:

| Severity | Reward Range | Examples |
|----------|--------------|----------|
| Critical | $2,000-$5,000 | RCE, SQL Injection, Authentication bypass |
| High | $500-$2,000 | XSS, CSRF, Authorization bypass |
| Medium | $200-$500 | Information disclosure, IDOR |
| Low | $100-$200 | Security misconfiguration |

**Program Rules**:

```markdown
# Bug Bounty Program Rules

## In Scope

- *.example.com (production)
- api.example.com
- admin.example.com

## Out of Scope

- staging.example.com
- dev.example.com
- Third-party services

## Prohibited Activities

- DDoS attacks
- Social engineering
- Physical attacks
- Spam or phishing
- Accessing other users' data

## Reporting Requirements

- Detailed vulnerability description
- Steps to reproduce
- Proof of concept
- Impact assessment

```

### Post-Major-Update Testing

**Testing Triggers**:

- Major feature releases
- Infrastructure changes
- Security control updates
- Third-party integration changes

**Testing Process**:

1. Identify changes 和 potential security impacts
2. Conduct focused penetration testing
3. Validate security controls
4. Test 用於 regression vulnerabilities
5. Document findings 和 remediation

## 影響分析

### 利害關係人影響

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | Medium | Vulnerability remediation work | Prioritization, 支援 |
| Operations Team | Medium | Testing coordination, potential disruption | Scheduling, communication |
| Security Team | High | Testing execution, remediation validation | Training, tools |
| End Users | None | Transparent testing (staging environment) | N/A |
| Compliance Team | Positive | PCI-DSS compliance evidence | Regular reporting |

### 影響半徑

**選擇的影響半徑**： **System**

影響：

- All applications 和 services
- Infrastructure 和 cloud configuration
- Security controls 和 policies
- Incident response procedures
- Team security awareness

### 風險評估

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| Service disruption 期間 testing | Low | High | Test in staging, coordinate 與 ops |
| False sense of security | Medium | Medium | Regular testing, multiple methodologies |
| Vulnerability disclosure | Low | Critical | Responsible disclosure policy, NDA |
| Testing cost overrun | Medium | Low | Fixed-price contracts, 預算 monitoring |
| Internal team capacity | Medium | Medium | External 支援, training |

**整體風險等級**： **Low**

## 實作計畫

### 第 1 階段： Program Setup (Month 1)

- [ ] Define testing scope 和 objectives
- [ ] Select external penetration testing vendor
- [ ] Procure testing tools
- [ ] Set up bug bounty program
- [ ] Create testing procedures 和 runbooks

### 第 2 階段： Initial Testing (Month 2-3)

- [ ] Conduct first external penetration test
- [ ] Set up automated security s可以ning
- [ ] Train internal security team
- [ ] Launch bug bounty program
- [ ] Document findings 和 remediation

### 第 3 階段： Regular Testing (Month 4-12)

- [ ] Quarterly external penetration testing
- [ ] Semi-annual internal testing
- [ ] Continuous automated s可以ning
- [ ] Bug bounty program management
- [ ] Remediation tracking

### 第 4 階段： Red Team Exercise (Month 12)

- [ ] Plan red team exercise
- [ ] Execute attack scenarios
- [ ] Analyze blue team response
- [ ] Document lessons learned
- [ ] Implement 改善ments

### 回滾策略

**Not Applicable** - Testing is non-destructive 和 conducted in controlled manner

**Safety Measures**:

- Test in staging environment first
- Coordinate 與 operations team
- Have rollback plan 用於 any changes
- Monitor systems 期間 testing
- Stop testing if issues detected

## 監控和成功標準

### 成功指標

- ✅ Quarterly penetration testing: 100% completion
- ✅ Critical vulnerabilities: Remediated within 24 hours
- ✅ High vulnerabilities: Remediated within 7 天
- ✅ Red team exercise: Annual completion
- ✅ Bug bounty submissions: > 10 per 年
- ✅ PCI-DSS compliance: 100%
- ✅ Zero successful real attacks

### 監控計畫

**Tracking Metrics**:

- `security.testing.completed` (count 透過 type)
- `security.vulnerabilities.found` (count 透過 severity)
- `security.vulnerabilities.remediated` (count 透過 severity)
- `security.remediation.time` (histogram)
- `security.testing.cost` (dollars)

**Reporting**:

- Monthly: Vulnerability remediation status
- Quarterly: Penetration testing results
- Annual: Red team exercise report
- Annual: Security posture assessment

**審查時程**：

- Monthly: Vulnerability remediation review
- Quarterly: Testing program effectiveness
- Annual: Program 預算 和 scope review

## 後果

### 正面後果

- ✅ **Proactive Security**: Find vulnerabilities before attackers
- ✅ **Compliance**: Meet PCI-DSS requirement 11.3
- ✅ **Validation**: Validate security controls effectiveness
- ✅ **改善ment**: Continuous security 改善ment
- ✅ **Readiness**: 改善d incident response capabilities
- ✅ **Awareness**: Enhanced team security awareness

### 負面後果

- ⚠️ **成本**： $50,000/year ongoing expense
- ⚠️ **Effort**: Remediation work 用於 development team
- ⚠️ **Coordination**: Requires coordination 與 multiple teams
- ⚠️ **Potential Disruption**: Risk of service disruption 期間 testing
- ⚠️ **False Positives**: Some findings may not be exploitable

### 技術債務

**已識別債務**：

1. Manual testing coordination (acceptable initially)
2. Limited internal testing capability
3. No automated remediation validation
4. Basic bug bounty program

**債務償還計畫**：

- **Q2 2026**: Automate testing coordination 和 scheduling
- **Q3 2026**: Build internal red team capability
- **Q4 2026**: Implement automated remediation validation
- **2027**: Expand bug bounty program 與 private researchers

## 相關決策

- [ADR-053: Security Monitoring 和 Incident Response](053-security-monitoring-incident-response.md) - Incident response testing
- [ADR-054: Data Loss Prevention (DLP) Strategy](054-data-loss-prevention-strategy.md) - Data protection testing
- [ADR-055: Vulnerability Management 和 Patching Strategy](055-vulnerability-management-patching-strategy.md) - Vulnerability remediation
- [ADR-056: Network Segmentation 和 Isolation Strategy](056-network-segmentation-isolation-strategy.md) - Network security testing

## 備註

### PCI-DSS Requirement 11.3

**Requirement**: Implement a methodology 用於 penetration testing that includes:

- External 和 internal penetration testing at least annually
- Testing after signifi可以t infrastructure 或 application upgrades
- Segmentation 和 scope-reduction controls testing
- Application-layer 和 network-layer penetration tests
- Testing to validate any segmentation 和 scope-reduction controls

### OWASP Testing Guide

Follow OWASP Testing Guide v4.2 methodology:

1. Information Gathering
2. Configuration 和 Deployment Management Testing
3. Identity Management Testing
4. Authentication Testing
5. Authorization Testing
6. Session Management Testing
7. Input Validation Testing
8. Error Handling Testing
9. Cryptography Testing
10. Business Logic Testing
11. Client-Side Testing

### Red Team vs Penetration Testing

**Penetration Testing**:

- Focused on finding vulnerabilities
- Time-boxed engagement
- Comprehensive reporting
- Known to blue team

**Red Team Exercise**:

- Focused on achieving objectives
- Extended engagement
- Simulates real attack
- Limited blue team knowledge

---

**文檔狀態**： ✅ Accepted  
**上次審查**： 2025-10-25  
**下次審查**： 2026-01-25 （每季）
