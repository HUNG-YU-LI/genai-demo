# 常見問題與解決方案

## 概述

本文件提供 Enterprise E-Commerce Platform 中常見問題的快速解決方案。

## Application 問題

### 問題：Application 無法啟動

**症狀**：Pod 處於 CrashLoopBackOff 狀態，application logs 顯示啟動錯誤

**快速檢查**：

```bash
kubectl logs ${POD_NAME} -n ${NAMESPACE}
kubectl describe pod ${POD_NAME} -n ${NAMESPACE}
```

**常見原因**：

1. **Database 連線失敗**
   - 檢查 database endpoint 和憑證
   - 驗證 security group 規則
   - 測試連線：`pg_isready -h ${DB_HOST}`

2. **缺少環境變數**
   - 檢查 ConfigMap 和 Secrets
   - 驗證所有必要變數已設定

3. **Port 已被使用**
   - 檢查 port 衝突
   - 驗證 service 設定

**解決方案**：參見 [Service Outage Runbook](../runbooks/service-outage.md)

### 問題：API 回應緩慢

**症狀**：回應時間 > 2s，timeout 錯誤

**快速檢查**：

```bash
curl http://localhost:8080/actuator/metrics/http.server.requests
kubectl top pods -n ${NAMESPACE}
```

**常見原因**：

1. CPU/Memory 使用率高
2. Database 查詢緩慢
3. Cache 未命中
4. 外部 API 延遲

**解決方案**：

- 快速修復：參見 [Slow API Responses Runbook](../runbooks/slow-api-responses.md)
- 詳細調查：參見 [Application Debugging Guide](application-debugging.md#performance-issues)

### 問題：Memory 洩漏

**症狀**：Memory 使用量持續增加，Pod 被 OOMKilled

**快速檢查**：

```bash
kubectl top pod ${POD_NAME} -n ${NAMESPACE}
jmap -histo:live 1
```

**常見原因**：

1. Database 連線未關閉
2. Cache 無上限
3. 靜態集合持續增長
4. ThreadLocal 未清理

**解決方案**：

- 快速修復：參見 [High Memory Usage Runbook](../runbooks/high-memory-usage.md)
- 詳細調查：參見 [Application Debugging Guide](application-debugging.md#memory-issues)

## Database 問題

### 問題：Connection Pool 耗盡

**症狀**：「Too many connections」錯誤

**快速修復**：

```bash
# Kill idle connections
psql -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE state = 'idle' AND query_start < NOW() - INTERVAL '10 minutes';"

# Increase pool size temporarily
kubectl set env deployment/ecommerce-backend HIKARI_MAX_POOL_SIZE=30
```

**解決方案**：參見 [Database Connection Issues Runbook](../runbooks/database-connection-issues.md)

### 問題：查詢緩慢

**症狀**：查詢時間 > 1s，database CPU 過高

**快速診斷**：

```sql
SELECT query, calls, mean_time, max_time
FROM pg_stat_statements
ORDER BY mean_time DESC LIMIT 10;
```

**快速修復**：

- 新增缺少的索引
- 優化查詢
- 使用查詢快取

**解決方案**：參見 [Slow API Responses Runbook](../runbooks/slow-api-responses.md)

## Deployment 問題

### 問題：Deployment 卡住

**症狀**：Rollout 未進行，Pod 處於 pending 狀態

**快速檢查**：

```bash
kubectl rollout status deployment/ecommerce-backend -n ${NAMESPACE}
kubectl get events -n ${NAMESPACE} --sort-by='.lastTimestamp'
```

**快速修復**：

```bash
# Rollback if needed
kubectl rollout undo deployment/ecommerce-backend -n ${NAMESPACE}
```

**解決方案**：參見 [Failed Deployment Runbook](../runbooks/failed-deployment.md)

### 問題：Image Pull 錯誤

**症狀**：ImagePullBackOff、ErrImagePull

**快速修復**：

```bash
# Update image pull secret
TOKEN=$(aws ecr get-login-password)
kubectl create secret docker-registry ecr-secret \
  --docker-server=${ECR_REGISTRY} \
  --docker-username=AWS \
  --docker-password=${TOKEN} \
  --dry-run=client -o yaml | kubectl apply -f -
```

## Network 問題

### 問題：Service 無法連線

**症狀**：Connection refused、timeout 錯誤

**快速檢查**：

```bash
kubectl get svc -n ${NAMESPACE}
kubectl get endpoints -n ${NAMESPACE}
kubectl describe svc ecommerce-backend -n ${NAMESPACE}
```

**常見原因**：

1. Service selector 不匹配
2. Pod 未就緒
3. Network policy 阻擋
4. Security group 規則

**解決方案**：參見 [Network and Connectivity Troubleshooting Guide](network-connectivity.md)

### 問題：DNS 解析失敗

**症狀**：「Name or service not known」錯誤

**快速修復**：

```bash
# Test DNS resolution
kubectl exec -it ${POD_NAME} -- nslookup kubernetes.default

# Restart CoreDNS
kubectl rollout restart deployment/coredns -n kube-system
```

**解決方案**：

- 快速修復：重啟 CoreDNS
- 詳細調查：參見 [Network and Connectivity Troubleshooting Guide](network-connectivity.md#dns-resolution-troubleshooting)

## Cache 問題

### 問題：Cache 命中率低

**症狀**：Cache 命中率 < 70%，回應緩慢

**快速診斷**：

```bash
redis-cli INFO stats | grep keyspace
```

**快速修復**：

- 增加 cache TTL
- 使用常用資料預熱 cache
- 檢視 cache key 策略

### 問題：Redis 連線失敗

**症狀**：「Connection refused」到 Redis

**快速檢查**：

```bash
redis-cli ping
kubectl get pods -n ${NAMESPACE} -l app=redis
```

**快速修復**：

```bash
# Restart Redis
kubectl rollout restart statefulset/redis -n ${NAMESPACE}
```

## Monitoring 問題

### 問題：Metrics 未顯示

**症狀**：Grafana dashboards 空白，沒有 metrics

**快速檢查**：

```bash
# Check metrics endpoint
curl http://localhost:8080/actuator/metrics

# Check Prometheus targets
curl http://prometheus:9090/api/v1/targets
```

**快速修復**：

- 驗證 Prometheus scrape 設定
- 檢查 service monitor
- 重啟 Prometheus

### 問題：Alerts 未觸發

**症狀**：儘管有問題但沒有 alerts

**快速檢查**：

```bash
# Check alert rules
curl http://prometheus:9090/api/v1/rules

# Check Alertmanager
curl http://alertmanager:9093/api/v1/alerts
```

## 快速參考指令

### Health Checks

```bash
# Application health
curl http://localhost:8080/actuator/health

# Database health
pg_isready -h ${DB_HOST}

# Redis health
redis-cli ping

# Kafka health
kafka-broker-api-versions --bootstrap-server ${KAFKA_BOOTSTRAP}
```

### 資源檢查

```bash
# Pod resources
kubectl top pods -n ${NAMESPACE}

# Node resources
kubectl top nodes

# Database connections
psql -c "SELECT count(*) FROM pg_stat_activity;"
```

### Log 檢查

```bash
# Application logs
kubectl logs -f deployment/ecommerce-backend -n ${NAMESPACE}

# Previous container logs
kubectl logs ${POD_NAME} --previous -n ${NAMESPACE}

# All pods logs
kubectl logs -l app=ecommerce-backend -n ${NAMESPACE} --tail=100
```

## 取得協助

### 內部資源

- **Runbooks**：`/docs/operations/runbooks/`
- **Monitoring**：Grafana dashboards
- **Logs**：CloudWatch Logs、Kibana

### 升級處理

- **L1 Support**：DevOps team
- **L2 Support**：Backend engineering team
- **On-Call**：查看 PagerDuty schedule

### 外部資源

- **AWS Support**：Premium support available
- **Kubernetes Docs**：<https://kubernetes.io/docs/>
- **Spring Boot Docs**：<https://spring.io/projects/spring-boot>

## 相關文件

- [Application Debugging Guide](application-debugging.md) - 詳細的除錯工作流程與分析技術
- [Database Issues Guide](database-issues.md) - 完整的 database 疑難排解
- [Network and Connectivity Guide](network-connectivity.md) - Network、DNS、TLS 和連線疑難排解
- [Security Incidents Guide](security-incidents.md) - Security 疑難排解與事件回應
- [Runbooks](../runbooks/README.md) - 逐步操作程序
- [Monitoring Strategy](../monitoring/monitoring-strategy.md) - Monitoring 和 alerting 設定
- [Deployment Process](../deployment/deployment-process.md) - Deployment 程序

---

**Last Updated**：2025-10-25
**Owner**：DevOps Team
**Review Cycle**：Monthly
