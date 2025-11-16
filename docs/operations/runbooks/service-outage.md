# Runbook: Service Outage

## Symptoms

- Service health check 失敗
- Load balancer 回傳 503/504 錯誤
- 所有 API endpoints 回傳錯誤
- 零成功請求

## Impact

- **Severity**：P0 - Critical
- **Affected Users**：所有使用者
- **Business Impact**：服務完全無法使用、營收損失

## Detection

- **Alert**：`APIEndpointDown` alert 觸發
- **Monitoring Dashboard**：Operations Dashboard 顯示服務停止
- **Log Patterns**：Connection refused、service unavailable

## Diagnosis

### 步驟 1：檢查 Service 狀態

```bash
# Check pod status
kubectl get pods -n production -l app=ecommerce-backend

# Check service endpoints
kubectl get endpoints ecommerce-backend -n production

# Check load balancer
kubectl get svc ecommerce-backend -n production
```

### 步驟 2：檢查最近的變更

```bash
# Check recent deployments
kubectl rollout history deployment/ecommerce-backend -n production

# Check recent events
kubectl get events -n production --sort-by='.lastTimestamp' | head -20
```

### 步驟 3：檢查相依服務

```bash
# Check database connectivity
kubectl exec -it ${POD_NAME} -n production -- \
  pg_isready -h ${DB_HOST} -p 5432

# Check Redis connectivity
kubectl exec -it ${POD_NAME} -n production -- \
  redis-cli -h ${REDIS_HOST} ping

# Check Kafka connectivity
kubectl exec -it ${POD_NAME} -n production -- \
  kafka-broker-api-versions --bootstrap-server ${KAFKA_BOOTSTRAP}
```

## Resolution

### 立即行動

1. **通知 stakeholders**：

```bash
# Send critical incident notification
# Subject: P0 - Service Outage
# All services are currently unavailable. Team is investigating.
```

1. **檢查 pod logs**：

```bash
kubectl logs -f deployment/ecommerce-backend -n production --tail=100
```

1. **必要時重啟 pods**：

```bash
kubectl rollout restart deployment/ecommerce-backend -n production
```

### 根本原因修復

#### 如果由失敗的 deployment 造成

```bash
# Rollback to previous version
kubectl rollout undo deployment/ecommerce-backend -n production
```

#### 如果由設定問題造成

```bash
# Restore previous configuration
kubectl apply -f previous-config.yaml
kubectl rollout restart deployment/ecommerce-backend -n production
```

#### 如果由相依服務失敗造成

```bash
# Check and fix database/Redis/Kafka
# See respective runbooks
```

## Verification

- [ ] 所有 pods 都在執行
- [ ] Health checks 通過
- [ ] API endpoints 正常回應
- [ ] Smoke tests 通過
- [ ] 錯誤率 < 1%
- [ ] 回應時間正常

## Prevention

1. **Deployment 安全性**：
   - 使用 canary deployments
   - 實作適當的 health checks
   - 先在 staging 測試

2. **相依服務韌性**：
   - 實作 circuit breakers
   - 新增 retry 邏輯
   - 使用 fallback 機制

3. **Monitoring**：
   - 全面的 health checks
   - 相依服務 monitoring
   - 失敗時自動 rollback

## Escalation

- **立即**：透過 PagerDuty 通知 on-call engineer
- **5 分鐘**：升級至 team lead
- **15 分鐘**：升級至 engineering manager

## 相關

- [Failed Deployment](failed-deployment.md)
- [Database Connection Issues](database-connection-issues.md)
- [Rollback Procedures](../deployment/rollback.md)

---

**Last Updated**：2025-10-25
**Owner**：DevOps Team
