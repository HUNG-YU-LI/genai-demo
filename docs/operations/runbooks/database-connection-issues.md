# Runbook: Database Connection Issues

## Symptoms

- Logs 中出現 "Connection refused" 錯誤
- "Too many connections" 錯誤
- 存取 database 時發生 timeout 錯誤
- Application 無法啟動

## Impact

- **Severity**：P0 - Critical
- **Affected Users**：所有使用者
- **Business Impact**：服務無法使用，無法存取資料

## Detection

- **Alert**：`HighDatabaseConnectionUsage` 或 `DatabaseConnectionFailed`
- **Monitoring Dashboard**：Database Dashboard > Connections
- **Log Patterns**：`SQLException`、`Connection timeout`、`Too many connections`

## Diagnosis

### 步驟 1：檢查 Connection Pool 狀態

```bash
# Check application logs
kubectl logs deployment/ecommerce-backend -n production | grep -i "connection"

# Check connection pool metrics
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
```

### 步驟 2：檢查 Database 狀態

```bash
# Check RDS instance status
aws rds describe-db-instances \
  --db-instance-identifier ecommerce-production \
  --query 'DBInstances[0].DBInstanceStatus'

# Check current connections
kubectl exec -it ${POD_NAME} -n production -- \
  psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} \
  -c "SELECT count(*) FROM pg_stat_activity;"

# Check connection limit
kubectl exec -it ${POD_NAME} -n production -- \
  psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} \
  -c "SHOW max_connections;"
```

### 步驟 3：識別 Connection Leaks

```bash
# Check long-running connections
kubectl exec -it ${POD_NAME} -n production -- \
  psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} \
  -c "SELECT pid, usename, application_name, state, query_start, query
      FROM pg_stat_activity
      WHERE state != 'idle'
      ORDER BY query_start;"
```

## Resolution

### 立即行動

1. **終止閒置的 connections**（如果安全）：

```bash
kubectl exec -it ${POD_NAME} -n production -- \
  psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} \
  -c "SELECT pg_terminate_backend(pid)
      FROM pg_stat_activity
      WHERE state = 'idle'
      AND query_start < NOW() - INTERVAL '10 minutes';"
```

1. **重啟 application pods**：

```bash
kubectl rollout restart deployment/ecommerce-backend -n production
```

### 根本原因修復

#### 如果 connection pool 耗盡

1. 增加 connection pool 大小：

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 30  # Increase from 20
      minimum-idle: 15       # Increase from 10
```

1. 修復程式碼中的 connection leaks：
   - 確保正確使用 try-with-resources
   - 在 finally blocks 中關閉 connections
   - 正確使用 @Transactional

#### 如果達到 database max_connections

1. 增加 RDS max_connections：

```bash
aws rds modify-db-parameter-group \
  --db-parameter-group-name ecommerce-prod \
  --parameters "ParameterName=max_connections,ParameterValue=200,ApplyMethod=immediate"
```

1. 必要時重啟 RDS instance

## Verification

- [ ] Application 可以連線至 database
- [ ] Connection pool metrics 正常
- [ ] Logs 中無 connection 錯誤
- [ ] API endpoints 正常回應
- [ ] Database queries 成功執行

## Prevention

1. **適當的 connection 管理**：
   - 使用 connection pooling
   - 設定適當的 timeouts
   - 實作 connection validation

2. **Monitoring**：
   - 監控 connection pool 使用率
   - 在 connection 數量高時發出 alert
   - 追蹤 connection leaks

3. **Code review**：
   - 檢視 database 存取程式碼
   - 確保適當的資源清理
   - 使用 try-with-resources

## Escalation

- **L1 Support**：DevOps team
- **L2 Support**：Backend engineering team
- **Database DBA**：針對 RDS 特定問題

## 相關

- [Slow Database Queries](slow-queries.md)
- [High Memory Usage](high-memory-usage.md)
- [Service Outage](service-outage.md)

---

**Last Updated**：2025-10-25
**Owner**：DevOps Team
