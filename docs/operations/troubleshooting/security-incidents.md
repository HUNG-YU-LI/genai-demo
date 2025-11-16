# Security Incident Troubleshooting

## 概述

本文件提供 Enterprise E-Commerce Platform 中 security 相關 incidents 的疑難排解程序，包括 authentication 失敗、authorization 問題、network security 問題和可疑活動調查。

## Authentication 問題

### 問題：JWT Token 過期

**症狀**：

- 使用者意外登出
- API 回應顯示「Token expired」錯誤
- 閒置一段時間後出現 401 Unauthorized 錯誤

**快速診斷**：

```bash
# 檢查 logs 中的 token 過期時間
kubectl logs -l app=ecommerce-backend -n production | grep "JWT.*expired"

# 驗證 JWT 設定
kubectl get configmap jwt-config -n production -o yaml

# 檢查目前 token 有效性
curl -H "Authorization: Bearer ${TOKEN}" \
  http://localhost:8080/api/v1/auth/validate
```

**常見原因**：

1. Token TTL 太短 (< 1 小時)
2. Services 之間的時鐘偏移
3. Token refresh 機制無法運作
4. Session timeout 設定不匹配

**快速修復**：

```bash
# 增加 token TTL (暫時)
kubectl set env deployment/ecommerce-backend \
  JWT_EXPIRATION_TIME=3600 -n production

# 重啟 pods 以套用變更
kubectl rollout restart deployment/ecommerce-backend -n production
```

**永久解決方案**：

1. 檢視並調整設定中的 token TTL
2. 實作適當的 token refresh 機制
3. 新增 sliding session expiration
4. 設定 refresh token rotation

**Related Runbook**：[Authentication Failure Runbook](../runbooks/authentication-failure.md)

---

### 問題：Invalid Token Signature

**症狀**：

- 「Invalid signature」錯誤
- Deployment 後 authentication 失敗
- 間歇性 401 錯誤

**快速診斷**：

```bash
# 檢查 JWT secret 設定
kubectl get secret jwt-secret -n production -o jsonpath='{.data.secret}' | base64 -d

# 驗證 token signing algorithm
kubectl logs -l app=ecommerce-backend -n production | grep "JWT.*algorithm"

# 檢查多個 JWT secrets (rolling update 問題)
kubectl get pods -n production -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.spec.containers[0].env[?(@.name=="JWT_SECRET")].valueFrom.secretKeyRef.name}{"\n"}{end}'
```

**常見原因**：

1. JWT secret 變更但未使舊 tokens 失效
2. 多個 services 使用不同 secrets
3. Secret rotation 未同步
4. Signing algorithm 設定錯誤

**快速修復**：

```bash
# 強制所有使用者重新驗證
kubectl delete secret jwt-secret -n production
kubectl create secret generic jwt-secret \
  --from-literal=secret=$(openssl rand -base64 32) \
  -n production

# 重啟所有 services
kubectl rollout restart deployment/ecommerce-backend -n production
```

**永久解決方案**：

1. 實作適當的 secret rotation 策略
2. 使用集中式 secret 管理 (AWS Secrets Manager)
3. 協調所有 services 間的 secret 更新
4. 為 rotation 期間的舊 secrets 新增寬限期

---

### 問題：Authentication Service 無法使用

**症狀**：

- 所有登入嘗試失敗
- 「Service unavailable」錯誤
- Authentication endpoint timeout

**快速診斷**：

```bash
# 檢查 authentication service health
kubectl get pods -n production -l app=auth-service

# 測試 authentication endpoint
curl -X POST http://auth-service:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test"}'

# 檢查 service logs
kubectl logs -l app=auth-service -n production --tail=100
```

**快速修復**：

```bash
# 重啟 authentication service
kubectl rollout restart deployment/auth-service -n production

# 必要時 scale up
kubectl scale deployment/auth-service --replicas=3 -n production
```

**Related Runbook**：[Service Outage Runbook](../runbooks/service-outage.md)

---

## Authorization 問題

### 問題：Permission Denied (RBAC)

**症狀**：

- 403 Forbidden 錯誤
- 「Access denied」訊息
- 使用者無法存取應有權限的資源

**快速診斷**：

```bash
# 檢查使用者 roles 和 permissions
kubectl logs -l app=ecommerce-backend -n production | grep "Access denied.*user=${USER_ID}"

# 驗證 RBAC 設定
kubectl get configmap rbac-config -n production -o yaml

# 檢查使用者被指派的 roles
curl -H "Authorization: Bearer ${TOKEN}" \
  http://localhost:8080/api/v1/users/${USER_ID}/roles
```

**常見原因**：

1. Role 未指派給使用者
2. Permission 未授予 role
3. Resource ownership 不匹配
4. RBAC policy cache 未更新

**快速修復**：

```bash
# 清除 RBAC cache
kubectl exec -it ${POD_NAME} -n production -- \
  redis-cli DEL "rbac:cache:*"

# 重啟 authorization service
kubectl rollout restart deployment/ecommerce-backend -n production
```

**調查步驟**：

1. 驗證 database 中的使用者 roles：

```sql
SELECT u.id, u.email, r.name as role
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
WHERE u.id = '${USER_ID}';
```

1. 檢查 role permissions：

```sql
SELECT r.name as role, p.resource, p.action
FROM roles r
JOIN role_permissions rp ON r.id = rp.role_id
JOIN permissions p ON rp.permission_id = p.id
WHERE r.name = '${ROLE_NAME}';
```

1. 檢視 audit logs：

```bash
kubectl logs -l app=ecommerce-backend -n production | \
  grep "Authorization.*${USER_ID}" | tail -50
```

**永久解決方案**：

1. 檢視並更新 RBAC policies
2. 實作適當的 role hierarchy
3. 新增 permission inheritance
4. 記錄 role-permission mappings

---

### 問題：Resource Ownership Validation 失敗

**症狀**：

- 使用者無法存取自己的資源
- 「Not authorized to access this resource」錯誤
- Ownership 檢查錯誤失敗

**快速診斷**：

```bash
# 檢查 database 中的 resource ownership
psql -c "SELECT id, owner_id, created_by FROM orders WHERE id = '${ORDER_ID}';"

# 驗證 logs 中的 ownership validation logic
kubectl logs -l app=ecommerce-backend -n production | \
  grep "Ownership validation.*${ORDER_ID}"
```

**常見原因**：

1. User ID 不匹配 (string vs UUID)
2. Ownership 欄位未填入
3. Delegation/sharing 無法運作
4. Cache 不一致

**快速修復**：

```bash
# 清除 ownership cache
kubectl exec -it ${POD_NAME} -n production -- \
  redis-cli DEL "ownership:*"
```

---

## Network Security 問題

### 問題：Security Group 阻擋流量

**症狀**：

- 特定 services 的連線 timeout
- 間歇性連線問題
- 「Connection refused」錯誤

**快速診斷**：

```bash
# 檢查 security group 規則
aws ec2 describe-security-groups \
  --group-ids ${SECURITY_GROUP_ID} \
  --query 'SecurityGroups[0].IpPermissions'

# 從 pod 測試連線
kubectl exec -it ${POD_NAME} -n production -- \
  nc -zv ${TARGET_HOST} ${TARGET_PORT}

# 檢查 VPC flow logs
aws ec2 describe-flow-logs \
  --filter "Name=resource-id,Values=${VPC_ID}"
```

**常見原因**：

1. 缺少 ingress/egress 規則
2. 錯誤的 CIDR blocks
3. Port 不匹配
4. Security group 未附加至資源

**快速修復**：

```bash
# 新增暫時規則 (請具體指定！)
aws ec2 authorize-security-group-ingress \
  --group-id ${SECURITY_GROUP_ID} \
  --protocol tcp \
  --port ${PORT} \
  --cidr ${SOURCE_CIDR}
```

**調查步驟**：

1. 檢視 VPC flow logs 中被拒絕的連線：

```bash
aws logs filter-log-events \
  --log-group-name /aws/vpc/flowlogs \
  --filter-pattern "[version, account, eni, source, destination, srcport, destport, protocol, packets, bytes, start, end, action=REJECT, status]" \
  --start-time $(date -u -d '1 hour ago' +%s)000
```

1. 驗證 network ACLs：

```bash
aws ec2 describe-network-acls \
  --filters "Name=vpc-id,Values=${VPC_ID}"
```

**永久解決方案**：

1. 記錄所需的 security group 規則
2. 使用 Infrastructure as Code (CDK) 管理 security groups
3. 實作最小權限存取
4. 定期 security group 稽核

---

### 問題：Network ACL 阻擋流量

**症狀**：

- 整個 subnet 連線問題
- Inbound 和 outbound 流量都受影響
- Security groups 正確但流量仍被阻擋

**快速診斷**：

```bash
# 檢查 Network ACL 規則
aws ec2 describe-network-acls \
  --filters "Name=association.subnet-id,Values=${SUBNET_ID}"

# 檢查規則評估順序
aws ec2 describe-network-acls \
  --network-acl-ids ${NACL_ID} \
  --query 'NetworkAcls[0].Entries' \
  --output table
```

**常見原因**：

1. 較低編號的 deny 規則 (較高優先級)
2. 缺少 ephemeral ports 的 allow 規則
3. 未考慮 NACLs 的 stateless 特性
4. 規則編號衝突

**快速修復**：

```bash
# 新增 allow 規則 (使用適當的規則編號)
aws ec2 create-network-acl-entry \
  --network-acl-id ${NACL_ID} \
  --rule-number 100 \
  --protocol tcp \
  --port-range From=${PORT},To=${PORT} \
  --cidr-block ${CIDR} \
  --egress \
  --rule-action allow
```

---

### 問題：WAF 阻擋合法流量

**症狀**：

- CloudFront/ALB 回應 403 Forbidden
- 合法請求被阻擋
- Logs 顯示「Request blocked by WAF」

**快速診斷**：

```bash
# 檢查 WAF logs
aws wafv2 get-sampled-requests \
  --web-acl-arn ${WEB_ACL_ARN} \
  --rule-metric-name ${RULE_NAME} \
  --scope REGIONAL \
  --time-window StartTime=$(date -u -d '1 hour ago' +%s),EndTime=$(date -u +%s) \
  --max-items 100

# 檢查哪個規則正在阻擋
aws logs filter-log-events \
  --log-group-name aws-waf-logs-${WEB_ACL_NAME} \
  --filter-pattern '{ $.action = "BLOCK" }' \
  --start-time $(date -u -d '1 hour ago' +%s)000
```

**常見原因**：

1. Rate limiting 太激進
2. Geo-blocking 合法使用者
3. SQL injection 規則誤判
4. XSS protection 阻擋有效輸入

**快速修復**：

```bash
# 暫時停用有問題的規則
aws wafv2 update-web-acl \
  --id ${WEB_ACL_ID} \
  --scope REGIONAL \
  --lock-token ${LOCK_TOKEN} \
  --rules file://updated-rules.json

# 將 IP 新增至 allowlist
aws wafv2 update-ip-set \
  --id ${IP_SET_ID} \
  --scope REGIONAL \
  --addresses ${IP_ADDRESS}/32 \
  --lock-token ${LOCK_TOKEN}
```

**調查步驟**：

1. 分析被阻擋的請求：

```bash
# 取得詳細 WAF logs
aws logs get-log-events \
  --log-group-name aws-waf-logs-${WEB_ACL_NAME} \
  --log-stream-name ${LOG_STREAM} \
  --start-time $(date -u -d '1 hour ago' +%s)000 | \
  jq '.events[] | select(.message | contains("BLOCK"))'
```

1. 檢視規則匹配詳情：

```bash
# 檢查哪個規則匹配
aws wafv2 get-sampled-requests \
  --web-acl-arn ${WEB_ACL_ARN} \
  --rule-metric-name ${RULE_NAME} \
  --scope REGIONAL \
  --time-window StartTime=$(date -u -d '1 hour ago' +%s),EndTime=$(date -u +%s) \
  --max-items 100 | \
  jq '.SampledRequests[] | {uri: .Request.URI, action: .Action, ruleWithinRuleGroup: .RuleNameWithinRuleGroup}'
```

**永久解決方案**：

1. 根據流量模式微調 WAF 規則
2. 為已知誤判實作自訂規則
3. 在阻擋前使用 count mode
4. 定期檢視被阻擋的請求

---

## DDoS Attack 偵測與緩解

### 問題：疑似 DDoS Attack

**症狀**：

- 流量突然激增
- 來自特定 IPs/regions 的大量請求
- Service 降級或無法使用
- 異常流量模式

**快速診斷**：

```bash
# 檢查請求率
kubectl logs -l app=ecommerce-backend -n production | \
  grep "HTTP" | awk '{print $1}' | sort | uniq -c | sort -rn | head -20

# 檢查 CloudWatch metrics
aws cloudwatch get-metric-statistics \
  --namespace AWS/ApplicationELB \
  --metric-name RequestCount \
  --dimensions Name=LoadBalancer,Value=${ALB_ARN} \
  --start-time $(date -u -d '1 hour ago' --iso-8601=seconds) \
  --end-time $(date -u --iso-8601=seconds) \
  --period 60 \
  --statistics Sum

# 檢查 AWS Shield events
aws shield describe-attack \
  --attack-id ${ATTACK_ID}
```

**立即行動**：

1. **啟用 AWS Shield Advanced (如果尚未啟用)**：

```bash
# 檢查 Shield 狀態
aws shield describe-subscription

# 必要時啟用 DDoS Response Team (DRT) 存取
aws shield associate-drt-role --role-arn ${DRT_ROLE_ARN}
```

1. **啟用 rate limiting**：

```bash
# 更新 WAF rate limit 規則
aws wafv2 update-web-acl \
  --id ${WEB_ACL_ID} \
  --scope REGIONAL \
  --lock-token ${LOCK_TOKEN} \
  --rules file://rate-limit-rules.json
```

1. **阻擋惡意 IPs**：

```bash
# 將 IPs 新增至 block list
aws wafv2 update-ip-set \
  --id ${BLOCK_IP_SET_ID} \
  --scope REGIONAL \
  --addresses ${MALICIOUS_IP}/32 \
  --lock-token ${LOCK_TOKEN}
```

1. **啟用 CloudFront geo-blocking** (如適用)：

```bash
aws cloudfront update-distribution \
  --id ${DISTRIBUTION_ID} \
  --distribution-config file://geo-restriction-config.json
```

**調查步驟**：

1. 分析流量模式：

```bash
# 最多來源 IPs
aws logs filter-log-events \
  --log-group-name /aws/elasticloadbalancing/${ALB_NAME} \
  --start-time $(date -u -d '1 hour ago' +%s)000 | \
  jq -r '.events[].message' | \
  awk '{print $3}' | sort | uniq -c | sort -rn | head -20

# 依 endpoint 的請求分布
kubectl logs -l app=ecommerce-backend -n production | \
  grep "HTTP" | awk '{print $7}' | sort | uniq -c | sort -rn
```

1. 檢查 attack signatures：

```bash
# 尋找常見 DDoS 模式
kubectl logs -l app=ecommerce-backend -n production | \
  grep -E "(slowloris|RUDY|HTTP flood)" | wc -l

# 檢查 SYN flood
aws ec2 describe-flow-logs \
  --filter "Name=resource-id,Values=${VPC_ID}" | \
  grep "SYN"
```

**緩解策略**：

1. **Layer 7 (Application Layer)**：
   - 啟用 WAF rate limiting
   - 實作 CAPTCHA challenges
   - 使用 CloudFront 搭配 AWS Shield
   - 啟用 request throttling

2. **Layer 4 (Transport Layer)**：
   - 啟用 AWS Shield Advanced
   - 使用 Network Load Balancer 搭配 Shield
   - 實作 connection limits

3. **Layer 3 (Network Layer)**：
   - AWS Shield Standard (自動)
   - VPC flow logs 分析
   - Network ACL 規則

**Post-Incident**：

1. 檢視 attack 模式並更新防禦
2. 記錄 attack 特徵
3. 更新 incident response 程序
4. 進行 post-mortem 分析

---

## 可疑活動調查

### 問題：異常使用者行為

**症狀**：

- 多次登入失敗嘗試
- 從異常位置存取
- 異常 API 使用模式
- 權限提升嘗試

**調查程序**：

1. **收集使用者活動資料**：

```bash
# 檢查 authentication logs
kubectl logs -l app=ecommerce-backend -n production | \
  grep "Authentication.*${USER_ID}" | tail -100

# 檢查 authorization 失敗
kubectl logs -l app=ecommerce-backend -n production | \
  grep "Authorization failed.*${USER_ID}" | tail -100

# 檢查 API 存取模式
kubectl logs -l app=ecommerce-backend -n production | \
  grep "user=${USER_ID}" | \
  awk '{print $7}' | sort | uniq -c | sort -rn
```

1. **分析登入模式**：

```sql
-- 檢查最近的登入嘗試
SELECT
  user_id,
  ip_address,
  user_agent,
  success,
  created_at
FROM authentication_logs
WHERE user_id = '${USER_ID}'
  AND created_at > NOW() - INTERVAL '24 hours'
ORDER BY created_at DESC;

-- 檢查失敗的登入嘗試
SELECT
  ip_address,
  COUNT(*) as attempts,
  MAX(created_at) as last_attempt
FROM authentication_logs
WHERE user_id = '${USER_ID}'
  AND success = false
  AND created_at > NOW() - INTERVAL '1 hour'
GROUP BY ip_address
HAVING COUNT(*) > 5;
```

1. **檢查權限提升**：

```sql
-- 檢查 role 變更
SELECT
  user_id,
  old_role,
  new_role,
  changed_by,
  changed_at
FROM role_change_audit
WHERE user_id = '${USER_ID}'
  AND changed_at > NOW() - INTERVAL '7 days'
ORDER BY changed_at DESC;

-- 檢查 permission 授予
SELECT
  user_id,
  permission,
  granted_by,
  granted_at
FROM permission_audit
WHERE user_id = '${USER_ID}'
  AND granted_at > NOW() - INTERVAL '7 days'
ORDER BY granted_at DESC;
```

1. **分析資料存取模式**：

```sql
-- 檢查敏感資料存取
SELECT
  user_id,
  resource_type,
  resource_id,
  action,
  accessed_at
FROM data_access_audit
WHERE user_id = '${USER_ID}'
  AND resource_type IN ('customer_pii', 'payment_info', 'admin_panel')
  AND accessed_at > NOW() - INTERVAL '24 hours'
ORDER BY accessed_at DESC;
```

**回應行動**：

1. **如果帳號被入侵**：

```bash
# 立即停用帳號
curl -X POST http://localhost:8080/api/v1/admin/users/${USER_ID}/disable \
  -H "Authorization: Bearer ${ADMIN_TOKEN}"

# 使所有 sessions 失效
kubectl exec -it ${POD_NAME} -n production -- \
  redis-cli DEL "session:${USER_ID}:*"

# 強制密碼重設
curl -X POST http://localhost:8080/api/v1/admin/users/${USER_ID}/force-password-reset \
  -H "Authorization: Bearer ${ADMIN_TOKEN}"
```

1. **如果可疑但未確認**：

```bash
# 啟用增強監控
curl -X POST http://localhost:8080/api/v1/admin/users/${USER_ID}/enable-monitoring \
  -H "Authorization: Bearer ${ADMIN_TOKEN}"

# 要求下次登入使用 MFA
curl -X POST http://localhost:8080/api/v1/admin/users/${USER_ID}/require-mfa \
  -H "Authorization: Bearer ${ADMIN_TOKEN}"
```

---

### 問題：資料外洩嘗試

**症狀**：

- 大量 API 請求
- 大量資料下載
- 異常 database 查詢
- 高資料傳輸量

**調查程序**：

1. **檢查 API 使用**：

```bash
# 依使用者計算 API 請求
kubectl logs -l app=ecommerce-backend -n production | \
  grep "user=${USER_ID}" | wc -l

# 檢查傳輸的資料量
kubectl logs -l app=ecommerce-backend -n production | \
  grep "user=${USER_ID}" | \
  awk '{sum+=$10} END {print "Total bytes:", sum}'
```

1. **分析 database 查詢**：

```sql
-- 檢查大量查詢
SELECT
  user_id,
  query,
  rows_returned,
  execution_time,
  executed_at
FROM query_audit
WHERE user_id = '${USER_ID}'
  AND rows_returned > 1000
  AND executed_at > NOW() - INTERVAL '1 hour'
ORDER BY rows_returned DESC;
```

1. **檢查匯出操作**：

```sql
-- 檢查資料匯出
SELECT
  user_id,
  export_type,
  record_count,
  file_size,
  created_at
FROM export_audit
WHERE user_id = '${USER_ID}'
  AND created_at > NOW() - INTERVAL '24 hours'
ORDER BY created_at DESC;
```

**回應行動**：

```bash
# 立即阻擋使用者
curl -X POST http://localhost:8080/api/v1/admin/users/${USER_ID}/block \
  -H "Authorization: Bearer ${ADMIN_TOKEN}"

# 撤銷 API keys
curl -X DELETE http://localhost:8080/api/v1/admin/users/${USER_ID}/api-keys \
  -H "Authorization: Bearer ${ADMIN_TOKEN}"

# 警示 security team
# 傳送包含證據的通知
```

---

## Audit Log 分析

### 分析 Security Events

**常見查詢**：

1. **失敗的 authentication 嘗試**：

```sql
SELECT
  ip_address,
  user_agent,
  COUNT(*) as attempts,
  MIN(created_at) as first_attempt,
  MAX(created_at) as last_attempt
FROM authentication_logs
WHERE success = false
  AND created_at > NOW() - INTERVAL '1 hour'
GROUP BY ip_address, user_agent
HAVING COUNT(*) > 10
ORDER BY attempts DESC;
```

1. **權限提升事件**：

```sql
SELECT
  user_id,
  action,
  resource,
  old_value,
  new_value,
  performed_by,
  created_at
FROM security_audit
WHERE action IN ('ROLE_CHANGE', 'PERMISSION_GRANT', 'ADMIN_ACCESS')
  AND created_at > NOW() - INTERVAL '24 hours'
ORDER BY created_at DESC;
```

1. **敏感資料存取**：

```sql
SELECT
  user_id,
  resource_type,
  COUNT(*) as access_count,
  MIN(accessed_at) as first_access,
  MAX(accessed_at) as last_access
FROM data_access_audit
WHERE resource_type IN ('customer_pii', 'payment_info', 'financial_data')
  AND accessed_at > NOW() - INTERVAL '24 hours'
GROUP BY user_id, resource_type
HAVING COUNT(*) > 100
ORDER BY access_count DESC;
```

1. **設定變更**：

```sql
SELECT
  user_id,
  config_key,
  old_value,
  new_value,
  changed_at
FROM config_change_audit
WHERE config_key LIKE '%security%'
  OR config_key LIKE '%auth%'
  OR config_key LIKE '%permission%'
ORDER BY changed_at DESC
LIMIT 50;
```

**CloudWatch Insights 查詢**：

1. **依 IP 的 authentication 失敗**：

```text
fields @timestamp, ip_address, user_id, error_message
| filter event_type = "authentication_failure"
| stats count() as failure_count by ip_address
| sort failure_count desc
| limit 20
```

1. **Authorization 失敗**：

```text
fields @timestamp, user_id, resource, action, reason
| filter event_type = "authorization_failure"
| stats count() as denial_count by user_id, resource
| sort denial_count desc
```

1. **可疑 API 模式**：

```text
fields @timestamp, user_id, endpoint, response_code
| filter response_code = 403 or response_code = 401
| stats count() as error_count by user_id, endpoint
| filter error_count > 50
| sort error_count desc
```

---

## Security Incident Response Checklist

### 立即回應 (0-15 分鐘)

- [ ] 識別並確認 security incident
- [ ] 評估嚴重性和影響
- [ ] 通知 security team 和 on-call engineer
- [ ] 開始收集證據
- [ ] 實施立即的遏制措施

### 調查 (15-60 分鐘)

- [ ] 收集並保存 logs
- [ ] 分析 attack vectors
- [ ] 識別受影響的系統和資料
- [ ] 確定根本原因
- [ ] 記錄事件時間軸

### 遏制 (30-120 分鐘)

- [ ] 阻擋惡意 IPs/users
- [ ] 撤銷被入侵的憑證
- [ ] 隔離受影響的系統
- [ ] 套用緊急修補
- [ ] 啟用增強監控

### 恢復 (1-24 小時)

- [ ] 恢復受影響的 services
- [ ] 驗證系統完整性
- [ ] 重設被入侵的憑證
- [ ] 更新 security 控制
- [ ] 執行 security 掃描

### Post-Incident (24-72 小時)

- [ ] 完成 incident 報告
- [ ] 進行 post-mortem 分析
- [ ] 更新 security 程序
- [ ] 實施預防措施
- [ ] 向利害關係人簡報

---

## 快速參考指令

### Authentication 檢查

```bash
# 驗證 JWT token
curl -H "Authorization: Bearer ${TOKEN}" \
  http://localhost:8080/api/v1/auth/validate

# 檢查使用者 sessions
kubectl exec -it ${POD_NAME} -n production -- \
  redis-cli KEYS "session:${USER_ID}:*"

# 檢視 authentication logs
kubectl logs -l app=ecommerce-backend -n production | \
  grep "Authentication"
```

### Authorization 檢查

```bash
# 檢查使用者 roles
curl -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  http://localhost:8080/api/v1/users/${USER_ID}/roles

# 驗證 permissions
curl -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  http://localhost:8080/api/v1/users/${USER_ID}/permissions

# 清除 RBAC cache
kubectl exec -it ${POD_NAME} -n production -- \
  redis-cli DEL "rbac:cache:*"
```

### Network Security 檢查

```bash
# 檢查 security groups
aws ec2 describe-security-groups --group-ids ${SG_ID}

# 檢查 Network ACLs
aws ec2 describe-network-acls --network-acl-ids ${NACL_ID}

# 檢查 WAF 規則
aws wafv2 get-web-acl --id ${WEB_ACL_ID} --scope REGIONAL

# 測試連線
kubectl exec -it ${POD_NAME} -n production -- \
  nc -zv ${HOST} ${PORT}
```

### Audit Log 查詢

```bash
# CloudWatch Logs
aws logs filter-log-events \
  --log-group-name /aws/application/security \
  --filter-pattern "{ $.event_type = \"security_incident\" }" \
  --start-time $(date -u -d '1 hour ago' +%s)000

# Application logs
kubectl logs -l app=ecommerce-backend -n production | \
  grep -E "(Authentication|Authorization|Security)" | tail -100
```

---

## 升級處理程序

### 嚴重性等級

**P0 - Critical**：

- 進行中的 security breach
- 資料外洩進行中
- 系統級入侵
- **回應時間**：立即
- **升級**：Security team + CTO

**P1 - High**：

- 疑似帳號入侵
- DDoS attack
- 多次 authentication 失敗
- **回應時間**：15 分鐘
- **升級**：Security team + Engineering manager

**P2 - Medium**：

- Authorization 問題
- WAF 阻擋合法流量
- Security 設定問題
- **回應時間**：1 小時
- **升級**：On-call engineer

**P3 - Low**：

- 輕微 security 警告
- Audit log 異常
- 非關鍵 security 更新
- **回應時間**：4 小時
- **升級**：Security team (下個工作日)

### 聯絡資訊

- **Security Team**：<security@company.com>
- **On-Call Engineer**：PagerDuty
- **AWS Support**：Premium support portal
- **Incident Commander**：查看 on-call schedule

---

## 相關文件

- [Security Perspective](../../perspectives/security/README.md) - Security 架構與設計
- [Authentication Guide](../../perspectives/security/authentication.md) - Authentication 機制
- [Authorization Guide](../../perspectives/security/authorization.md) - RBAC 和 permissions
- [Monitoring Strategy](../monitoring/monitoring-strategy.md) - Security monitoring 設定
- [Incident Response Plan](../runbooks/security-incident-response.md) - 詳細 incident response 程序
- [Audit Logging](../../perspectives/security/audit-logging.md) - Audit log 設定

---

**Last Updated**：2025-10-25
**Owner**：Security Team
**Review Cycle**：Quarterly
**Emergency Contact**：<security@company.com>
