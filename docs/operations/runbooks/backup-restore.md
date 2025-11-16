# Runbook: Backup 和 Restore 操作

## 概述

本 runbook 涵蓋 Enterprise E-Commerce Platform 的 database backup 和 restore 程序。

## Backup 程序

### 自動化 Backups

自動化 backups 根據排程執行：

- **Production**：每小時 snapshots，保留 30 天
- **Staging**：每日 snapshots，保留 7 天

### 手動 Backup

#### 建立手動 Snapshot

```bash
# Create RDS snapshot
aws rds create-db-snapshot \
  --db-instance-identifier ecommerce-production \
  --db-snapshot-identifier ecommerce-prod-manual-$(date +%Y%m%d-%H%M%S)

# Wait for snapshot to complete
aws rds wait db-snapshot-available \
  --db-snapshot-identifier ecommerce-prod-manual-$(date +%Y%m%d-%H%M%S)

# Verify snapshot
aws rds describe-db-snapshots \
  --db-snapshot-identifier ecommerce-prod-manual-$(date +%Y%m%d-%H%M%S)
```

#### 匯出 Database Dump

```bash
# Create logical backup
kubectl exec -it ${POD_NAME} -n production -- \
  pg_dump -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} \
  -F c -f /tmp/backup-$(date +%Y%m%d).dump

# Copy backup locally
kubectl cp production/${POD_NAME}:/tmp/backup-$(date +%Y%m%d).dump \
  ./backup-$(date +%Y%m%d).dump

# Upload to S3
aws s3 cp ./backup-$(date +%Y%m%d).dump \
  s3://ecommerce-backups/database/backup-$(date +%Y%m%d).dump
```

## Restore 程序

### 從 RDS Snapshot Restore

#### 步驟 1：列出可用的 Snapshots

```bash
# List recent snapshots
aws rds describe-db-snapshots \
  --db-instance-identifier ecommerce-production \
  --query 'DBSnapshots[*].[DBSnapshotIdentifier,SnapshotCreateTime]' \
  --output table | head -20
```

#### 步驟 2：停止 Application

```bash
# Scale down to prevent writes
kubectl scale deployment/ecommerce-backend --replicas=0 -n production
```

#### 步驟 3：Restore Snapshot

```bash
# Restore to new instance
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier ecommerce-prod-restored \
  --db-snapshot-identifier ${SNAPSHOT_ID} \
  --db-instance-class db.r5.xlarge \
  --multi-az

# Wait for restore to complete
aws rds wait db-instance-available \
  --db-instance-identifier ecommerce-prod-restored
```

#### 步驟 4：更新 Application 設定

```bash
# Update database endpoint in Secrets Manager
aws secretsmanager update-secret \
  --secret-id production/ecommerce/config \
  --secret-string '{
    "DB_HOST": "ecommerce-prod-restored.xxxxx.rds.amazonaws.com",
    "DB_NAME": "ecommerce_production",
    "DB_USERNAME": "ecommerce_app",
    "DB_PASSWORD": "..."
  }'
```

#### 步驟 5：重啟 Application

```bash
# Scale up application
kubectl scale deployment/ecommerce-backend --replicas=4 -n production

# Verify connectivity
kubectl logs -f deployment/ecommerce-backend -n production
```

### 從 Database Dump Restore

#### 步驟 1：下載 Backup

```bash
# Download from S3
aws s3 cp s3://ecommerce-backups/database/${BACKUP_FILE} \
  ./restore-backup.dump
```

#### 步驟 2：停止 Application

```bash
kubectl scale deployment/ecommerce-backend --replicas=0 -n production
```

#### 步驟 3：Restore Database

```bash
# Copy dump to pod
kubectl cp ./restore-backup.dump \
  production/${POD_NAME}:/tmp/restore-backup.dump

# Restore database
kubectl exec -it ${POD_NAME} -n production -- \
  pg_restore -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} \
  -c -F c /tmp/restore-backup.dump
```

#### 步驟 4：驗證 Restore

```bash
# Check table counts
kubectl exec -it ${POD_NAME} -n production -- \
  psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} \
  -c "SELECT schemaname, tablename, n_live_tup
      FROM pg_stat_user_tables
      ORDER BY n_live_tup DESC;"

# Verify data integrity
./scripts/verify-database-integrity.sh
```

#### 步驟 5：重啟 Application

```bash
kubectl scale deployment/ecommerce-backend --replicas=4 -n production
```

## Point-in-Time Recovery

### Restore 至特定時間

```bash
# Restore to specific timestamp
aws rds restore-db-instance-to-point-in-time \
  --source-db-instance-identifier ecommerce-production \
  --target-db-instance-identifier ecommerce-prod-pitr \
  --restore-time 2025-10-25T10:30:00Z \
  --db-instance-class db.r5.xlarge

# Wait for restore
aws rds wait db-instance-available \
  --db-instance-identifier ecommerce-prod-pitr
```

## 驗證檢查清單

### Restore 之後

- [ ] Database 可存取
- [ ] 所有 tables 都存在
- [ ] Row 計數符合預期
- [ ] Foreign key constraints 完整
- [ ] Application 可以連線
- [ ] 關鍵 queries 成功執行
- [ ] 業務邏輯正常運作
- [ ] 未偵測到資料損毀

### 資料完整性檢查

```sql
-- Check for orphaned records
SELECT COUNT(*) FROM orders WHERE customer_id NOT IN (SELECT id FROM customers);

-- Verify referential integrity
SELECT conname, conrelid::regclass, confrelid::regclass
FROM pg_constraint
WHERE contype = 'f' AND convalidated = false;

-- Check for duplicate primary keys
SELECT id, COUNT(*) FROM orders GROUP BY id HAVING COUNT(*) > 1;
```

## Backup 測試

### 每月 Backup 測試

1. Restore backup 至測試環境
2. 驗證資料完整性
3. 執行 application tests
4. 記錄結果
5. 必要時更新程序

## Backup 保留政策

| Environment | Frequency | Retention | Type |
|-------------|-----------|-----------|------|
| Production | 每小時 | 7 天 | Automated snapshot |
| Production | 每日 | 30 天 | Automated snapshot |
| Production | 每週 | 90 天 | Manual snapshot |
| Staging | 每日 | 7 天 | Automated snapshot |

## Disaster Recovery

### RTO 和 RPO 目標

- **RTO** (Recovery Time Objective)：< 1 小時
- **RPO** (Recovery Point Objective)：< 15 分鐘

### DR 程序

1. 評估情況和影響
2. 通知 stakeholders
3. 啟動 DR plan
4. 從最近的 backup restore
5. 驗證資料完整性
6. 恢復營運
7. 進行 post-mortem

## Escalation

- **L1 Support**：DevOps team
- **L2 Support**：Database administrator
- **AWS Support**：針對 RDS 特定問題

## 相關

- [Database Connection Issues](database-connection-issues.md)
- [Data Inconsistency](data-inconsistency.md)
- [Rollback Procedures](../deployment/rollback.md)

---

**Last Updated**：2025-10-25
**Owner**：DevOps Team
