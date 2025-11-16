# Runbook: 安全事件回應

## 症狀

- 異常流量模式
- 未授權存取嘗試
- 資料外洩指標
- Malware 偵測
- DDoS 攻擊
- 可疑使用者活動
- AWS GuardDuty 的安全警報
- 身分驗證失敗激增

## 影響

- **嚴重性**: P0 - Critical
- **受影響使用者**: 可能影響所有使用者
- **業務影響**: 資料外洩、服務中斷、法律責任、聲譽損害

## 偵測

- **Alert**: `SecurityIncidentDetected`、`UnauthorizedAccess`、`DDoSAttack`
- **Monitoring Dashboard**: Security Dashboard
- **Log Patterns**:
  - 多次登入失敗嘗試
  - 異常 API 存取模式
  - SQL injection 嘗試
  - XSS 嘗試
  - 權限提升嘗試

## 診斷

### 步驟 1: 評估事件嚴重性

```bash
# Check security alerts
aws guardduty list-findings \
  --detector-id ${DETECTOR_ID} \
  --finding-criteria '{"Criterion":{"severity":{"Gte":7}}}'

# Check WAF blocked requests
aws wafv2 get-sampled-requests \
  --web-acl-id ${WEB_ACL_ID} \
  --rule-metric-name ${RULE_NAME} \
  --scope REGIONAL \
  --time-window StartTime=$(date -u -d '1 hour ago' +%s),EndTime=$(date -u +%s) \
  --max-items 100

# Check CloudTrail for suspicious activity
aws cloudtrail lookup-events \
  --lookup-attributes AttributeKey=EventName,AttributeValue=ConsoleLogin \
  --start-time $(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%S) \
  --max-results 50
```

### 步驟 2: 識別攻擊向量

```bash
# Check application logs for attack patterns
kubectl logs deployment/ecommerce-backend -n production --tail=10000 | \
  grep -iE "sql.*injection|<script|union.*select|drop.*table|../../../"

# Check failed authentication attempts
kubectl logs deployment/ecommerce-backend -n production --tail=10000 | \
  grep -i "authentication.*failed\|unauthorized\|forbidden"

# Check for unusual API access
kubectl logs deployment/ecommerce-backend -n production --tail=10000 | \
  awk '{print $1, $7}' | sort | uniq -c | sort -rn | head -20

# Check for data exfiltration attempts
kubectl logs deployment/ecommerce-backend -n production --tail=10000 | \
  grep -iE "download|export|dump" | grep -v "normal_operation"
```

### 步驟 3: 識別受影響系統

```bash
# Check which pods/services accessed
kubectl logs deployment/ecommerce-backend -n production --tail=10000 | \
  grep ${SUSPICIOUS_IP} | awk '{print $1, $7}' | sort | uniq

# Check database access logs
kubectl exec -it ${POD_NAME} -n production -- \
  psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} -c \
  "SELECT usename, application_name, client_addr, query_start, query 
   FROM pg_stat_activity 
   WHERE client_addr = '${SUSPICIOUS_IP}';"

# Check S3 access logs
aws s3api get-bucket-logging --bucket ecommerce-production-assets
aws s3 cp s3://ecommerce-logs/s3-access/ . --recursive
grep ${SUSPICIOUS_IP} *.log
```

### 步驟 4: 判定資料暴露程度

```bash
# Check for data access
kubectl logs deployment/ecommerce-backend -n production --tail=10000 | \
  grep ${SUSPICIOUS_IP} | grep -iE "customer|order|payment|credit"

# Check database queries from suspicious source
kubectl exec -it ${POD_NAME} -n production -- \
  psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} -c \
  "SELECT query, calls, rows 
   FROM pg_stat_statements 
   WHERE query LIKE '%customers%' OR query LIKE '%payments%' 
   ORDER BY calls DESC 
   LIMIT 20;"

# Check for data downloads
aws cloudtrail lookup-events \
  --lookup-attributes AttributeKey=EventName,AttributeValue=GetObject \
  --start-time $(date -u -d '24 hours ago' +%Y-%m-%dT%H:%M:%S) \
  --query 'Events[?contains(CloudTrailEvent, `${SUSPICIOUS_IP}`)]'
```

## 解決方案

### 立即行動 (前 15 分鐘)

1. **啟動事件回應團隊**:

```bash
# Page security team
# Notify: Security Lead, Engineering Manager, Legal, PR

# Create incident channel
# Slack: #incident-security-YYYYMMDD
```

1. **控制威脅**:

**封鎖惡意 IP 位址**:

```bash
# Add IP to WAF block list
aws wafv2 update-ip-set \
  --name BlockedIPs \
  --scope REGIONAL \
  --id ${IP_SET_ID} \
  --addresses ${MALICIOUS_IP}/32

# Block at security group level
aws ec2 authorize-security-group-ingress \
  --group-id ${SG_ID} \
  --ip-permissions IpProtocol=-1,FromPort=-1,ToPort=-1,IpRanges="[{CidrIp=${MALICIOUS_IP}/32,Description='Blocked malicious IP'}]"
```

**撤銷已洩露的憑證**:

```bash
# Disable compromised IAM user
aws iam update-access-key \
  --access-key-id ${ACCESS_KEY_ID} \
  --status Inactive \
  --user-name ${USER_NAME}

# Force password reset
aws iam update-login-profile \
  --user-name ${USER_NAME} \
  --password-reset-required

# Revoke all sessions
aws iam delete-login-profile --user-name ${USER_NAME}
```

**隔離受影響系統**:

```bash
# Scale down affected pods
kubectl scale deployment/ecommerce-backend --replicas=0 -n production

# Or isolate specific pod
kubectl label pod ${POD_NAME} quarantine=true -n production
kubectl patch service ecommerce-backend -n production \
  -p '{"spec":{"selector":{"quarantine":"false"}}}'
```

1. **啟用增強日誌記錄**:

```bash
# Enable VPC Flow Logs
aws ec2 create-flow-logs \
  --resource-type VPC \
  --resource-ids ${VPC_ID} \
  --traffic-type ALL \
  --log-destination-type cloud-watch-logs \
  --log-group-name /aws/vpc/flowlogs

# Enable CloudTrail data events
aws cloudtrail put-event-selectors \
  --trail-name ecommerce-production \
  --event-selectors '[{"ReadWriteType":"All","IncludeManagementEvents":true,"DataResources":[{"Type":"AWS::S3::Object","Values":["arn:aws:s3:::*/*"]}]}]'
```

### 調查 (第一小時)

1. **收集證據**:

```bash
# Capture logs
kubectl logs deployment/ecommerce-backend -n production --since=24h > incident-logs.txt

# Export database logs
kubectl exec -it ${POD_NAME} -n production -- \
  psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} -c \
  "COPY (SELECT * FROM pg_stat_activity) TO '/tmp/db-activity.csv' CSV HEADER;"

# Capture network traffic
kubectl exec -it ${POD_NAME} -n production -- tcpdump -i any -w /tmp/capture.pcap

# Export CloudTrail events
aws cloudtrail lookup-events \
  --start-time $(date -u -d '24 hours ago' +%Y-%m-%dT%H:%M:%S) \
  --max-results 1000 > cloudtrail-events.json
```

1. **分析攻擊模式**:

```bash
# Analyze access patterns
cat incident-logs.txt | grep ${SUSPICIOUS_IP} | \
  awk '{print $4}' | sort | uniq -c | sort -rn

# Check for privilege escalation
cat incident-logs.txt | grep -iE "admin|root|sudo|privilege"

# Check for lateral movement
cat incident-logs.txt | grep -iE "ssh|rdp|smb|internal"
```

1. **評估資料外洩**:

```sql
-- Check accessed customer data
SELECT c.id, c.email, c.created_at, c.last_modified
FROM customers c
WHERE c.last_modified > NOW() - INTERVAL '24 hours'
ORDER BY c.last_modified DESC;

-- Check accessed payment data
SELECT p.id, p.order_id, p.amount, p.created_at
FROM payments p
WHERE p.created_at > NOW() - INTERVAL '24 hours'
ORDER BY p.created_at DESC;

-- Check for data exports
SELECT * FROM audit_log
WHERE action IN ('EXPORT', 'DOWNLOAD', 'BULK_READ')
  AND created_at > NOW() - INTERVAL '24 hours';
```

### 補救措施

1. **修補漏洞**:

```bash
# Update dependencies
./gradlew dependencyUpdates
./gradlew build

# Scan for vulnerabilities
trivy image ${ECR_REGISTRY}/ecommerce-backend:${VERSION}

# Apply security patches
kubectl set image deployment/ecommerce-backend \
  ecommerce-backend=${ECR_REGISTRY}/ecommerce-backend:${PATCHED_VERSION} \
  -n production
```

1. **輪換憑證**:

```bash
# Rotate database passwords
aws secretsmanager rotate-secret \
  --secret-id production/ecommerce/db-password

# Rotate API keys
aws secretsmanager rotate-secret \
  --secret-id production/ecommerce/api-keys

# Rotate JWT secrets
aws secretsmanager rotate-secret \
  --secret-id production/ecommerce/jwt-secret

# Restart pods to pick up new secrets
kubectl rollout restart deployment/ecommerce-backend -n production
```

1. **強化安全**:

```bash
# Enable MFA for all users
aws iam update-account-password-policy \
  --require-symbols \
  --require-numbers \
  --require-uppercase-characters \
  --require-lowercase-characters \
  --minimum-password-length 14 \
  --max-password-age 90

# Enable AWS Config rules
aws configservice put-config-rule \
  --config-rule file://security-rules.json

# Update WAF rules
aws wafv2 update-web-acl \
  --name ecommerce-production \
  --scope REGIONAL \
  --id ${WEB_ACL_ID} \
  --rules file://enhanced-waf-rules.json
```

### 溝通

1. **內部溝通**:

```text
Subject: Security Incident - [SEVERITY] - [DATE]

Status: [Contained/Under Investigation/Resolved]

Summary:

- Incident Type: [Type]
- Detection Time: [Time]
- Affected Systems: [Systems]
- Data Exposure: [Yes/No/Unknown]

Actions Taken:

- [Action 1]
- [Action 2]

Next Steps:

- [Step 1]
- [Step 2]

Contact: security-team@ecommerce.example.com
```

1. **客戶溝通** (如果有資料外洩):

```text
Subject: Important Security Notice

Dear Customer,

We are writing to inform you of a security incident that may have affected your account.

What Happened:
[Brief description]

What Information Was Involved:
[Specific data types]

What We Are Doing:
[Actions taken]

What You Should Do:

- Change your password immediately
- Monitor your account for suspicious activity
- Enable two-factor authentication

We sincerely apologize for this incident.

Contact: support@ecommerce.example.com
```

1. **監管通報** (如果需要):

```bash
# GDPR breach notification (within 72 hours)
# Notify relevant data protection authority

# PCI DSS breach notification
# Notify payment card brands and acquiring bank

# Document all notifications
```

## 驗證

- [ ] 威脅已控制
- [ ] 惡意存取已封鎖
- [ ] 已洩露憑證已撤銷
- [ ] 漏洞已修補
- [ ] 無持續可疑活動
- [ ] 系統已恢復正常運作
- [ ] 證據已保存
- [ ] 事件已記錄

## 預防措施

### 1. 安全加固

```yaml
# Implement security best practices

- Enable MFA for all accounts
- Use least privilege access
- Encrypt data at rest and in transit
- Regular security audits
- Penetration testing
- Security training for team

```

### 2. 增強監控

```yaml
# Set up security monitoring

- alert: SuspiciousLoginActivity

  expr: rate(authentication_failures[5m]) > 10
  
- alert: UnusualAPIAccess

  expr: rate(api_requests{status="403"}[5m]) > 5
  
- alert: DataExfiltrationAttempt

  expr: rate(data_export_requests[5m]) > 1
```

### 3. 定期安全評估

```bash
# Schedule regular security scans
./scripts/security-scan.sh

# Conduct penetration testing
./scripts/pentest.sh

# Review access logs
./scripts/review-access-logs.sh
```

## 升級程序

- **立即**: Security Lead、Engineering Manager
- **1 小時內**: CTO、Legal Counsel
- **4 小時內**: CEO、董事會 (如果是重大外洩)
- **外部**: 執法機關 (如果有犯罪活動)

## 相關文件

- [DDoS Attack Response](ddos-attack.md)
- [Backup and Restore](backup-restore.md)
- [Service Outage](service-outage.md)

## 法律與合規

- 記錄所有採取的行動
- 保存證據以供調查
- 依法通知當局
- 遵守外洩通報要求
- 諮詢法律顧問

---

**Last Updated**: 2025-10-25
**Owner**: Security Team
**Review Cycle**: Quarterly
**Classification**: CONFIDENTIAL
