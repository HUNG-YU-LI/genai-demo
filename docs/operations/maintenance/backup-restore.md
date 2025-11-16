# 備份與還原指南

## 概述

本指南涵蓋企業電商平台所有元件的備份和還原程序。

關於詳細操作程序,請參閱 [Backup and Restore Runbook](../runbooks/backup-restore.md)。

## 備份策略

### 備份類型

| 類型 | 頻率 | 保留期 | 目的 |
|------|-----------|-----------|---------|
| Full Backup | 每日 | 30 天 | 完整系統復原 |
| Incremental | 每小時 | 7 天 | 時間點復原 |
| Transaction Log | 持續 | 7 天 | 最小資料遺失 |
| Configuration | 變更時 | 90 天 | 系統配置 |

### 備份排程

**Production**:

- Database: 每小時自動 snapshot
- Application config: 每次部署時
- Log: 持續至 CloudWatch (90 天保留)
- Metric: CloudWatch 中保留 15 個月

**Staging**:

- Database: 每日 snapshot
- Application config: 部署時
- Log: 30 天保留

## 資料庫備份

### 自動 RDS Snapshot

Configured in RDS:

- Backup window: 02:00-04:00 UTC
- Retention: 30 days
- Multi-AZ: Enabled
- Point-in-time recovery: 7 days

### Manual Backup

See [Backup and Restore Runbook](../runbooks/backup-restore.md) for detailed procedures.

## Application Configuration Backups

### ConfigMaps and Secrets

```bash
# Backup all ConfigMaps
kubectl get configmap -n production -o yaml > configmaps-backup.yaml

# Backup all Secrets
kubectl get secret -n production -o yaml > secrets-backup.yaml

# Store in version control (secrets encrypted)
git add configmaps-backup.yaml
git commit -m "Backup: ConfigMaps $(date +%Y-%m-%d)"
```

### Infrastructure as Code

All infrastructure defined in CDK:

- Version controlled in Git
- Automated backups via GitHub
- Can recreate entire infrastructure from code

## Restore Procedures

### Database Restore

See [Backup and Restore Runbook](../runbooks/backup-restore.md) for step-by-step procedures.

### Application Configuration Restore

```bash
# Restore ConfigMaps
kubectl apply -f configmaps-backup.yaml

# Restore Secrets
kubectl apply -f secrets-backup.yaml

# Restart pods to pick up changes
kubectl rollout restart deployment/ecommerce-backend -n production
```

## Disaster Recovery

### RTO and RPO

- **RTO** (Recovery Time Objective): < 1 hour
- **RPO** (Recovery Point Objective): < 15 minutes

### DR Procedures

1. Assess situation
2. Activate DR team
3. Restore from most recent backup
4. Verify data integrity
5. Resume operations
6. Conduct post-mortem

## Testing

### Monthly Backup Tests

- Restore backup to test environment
- Verify data integrity
- Run application tests
- Document results

### Quarterly DR Drills

- Full disaster recovery simulation
- Test all procedures
- Update documentation
- Train team

## Related Documentation

- [Backup and Restore Runbook](../runbooks/backup-restore.md)
- [Database Maintenance](database-maintenance.md)
- [Deployment Process](../deployment/deployment-process.md)

---

**Last Updated**: 2025-10-25  
**Owner**: DevOps Team  
**Review Cycle**: Quarterly
