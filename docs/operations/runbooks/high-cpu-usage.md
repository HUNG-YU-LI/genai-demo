# Runbook: High CPU Usage

## Symptoms

- CPU 使用率 > 80% 持續一段時間
- API 回應時間緩慢
- 請求延遲增加
- Pod throttling 警告

## Impact

- **Severity**：P1 - High
- **Affected Users**：所有使用者可能體驗到較慢的回應時間
- **Business Impact**：使用者體驗降級、可能發生 timeout 錯誤

## Detection

- **Alert**：`HighCPUUsage` alert 觸發
- **Monitoring Dashboard**：Operations Dashboard > Infrastructure > CPU Utilization
- **Log Patterns**：關於處理緩慢的 `WARN` 訊息

## Diagnosis

### 步驟 1：識別受影響的 Pods

```bash
# Check CPU usage across all pods
kubectl top pods -n production -l app=ecommerce-backend

# Get detailed pod metrics
kubectl describe pod ${POD_NAME} -n production
```

### 步驟 2：檢查最近的變更

```bash
# Check recent deployments
kubectl rollout history deployment/ecommerce-backend -n production

# Check recent configuration changes
kubectl get configmap ecommerce-config -n production -o yaml
```

### 步驟 3：分析 Application Logs

```bash
# Check for CPU-intensive operations
kubectl logs ${POD_NAME} -n production --tail=500 | grep -i "slow\|timeout\|processing"

# Check for infinite loops or stuck processes
kubectl logs ${POD_NAME} -n production --tail=1000 | grep -i "error\|exception"
```

### 步驟 4：剖析 Application

```bash
# Get thread dump
kubectl exec -it ${POD_NAME} -n production -- jstack 1 > thread-dump.txt

# Check for CPU-intensive threads
grep "runnable" thread-dump.txt | sort | uniq -c | sort -rn
```

## Resolution

### 立即行動

1. **水平擴展**以分散負載：

```bash
kubectl scale deployment/ecommerce-backend --replicas=8 -n production
```

1. **監控影響**：

```bash
# Watch CPU usage
watch kubectl top pods -n production -l app=ecommerce-backend
```

### 根本原因修復

#### 如果由低效率的程式碼造成

1. 從 profiling 中識別 CPU 密集的程式碼
2. 優化演算法或 query
3. 透過正常 deployment 流程部署修復

#### 如果由負載增加造成

1. 驗證負載是否合法（非攻擊）
2. 調整 HPA 設定：

```bash
kubectl edit hpa ecommerce-backend-hpa -n production
# Increase maxReplicas if needed
```

#### 如果由資源限制造成

1. 檢視並調整資源 requests/limits：

```yaml
resources:
  requests:
    cpu: "500m"
  limits:
    cpu: "1000m"  # Increase if needed
```

## Verification

- [ ] CPU 使用率降至 70% 以下
- [ ] API 回應時間恢復正常（< 2s）
- [ ] Logs 中無 throttling 警告
- [ ] Application metrics 穩定
- [ ] 無新的 alerts 觸發

## Prevention

1. **實作適當的資源限制**：
   - 設定適當的 CPU requests 和 limits
   - 使用 HPA 進行自動擴展

2. **程式碼優化**：
   - 定期 profile 程式碼
   - 優化 database queries
   - 在適當的地方實作 caching

3. **Load testing**：
   - 定期 load testing 以識別瓶頸
   - 根據成長預測進行容量規劃

4. **Monitoring**：
   - 設定 CPU 逐漸增加的 alerts
   - 監控 CPU 趨勢隨時間變化

## Escalation

- **L1 Support**：DevOps team
- **L2 Support**：Backend engineering team
- **On-Call Engineer**：查看 PagerDuty

## 相關

- [High Memory Usage](high-memory-usage.md)
- [Slow API Responses](slow-api-responses.md)
- [Scaling Operations](scaling-operations.md)

---

**Last Updated**：2025-10-25
**Owner**：DevOps Team
