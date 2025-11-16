# Runbook: 擴展操作

## 概述

本 runbook 涵蓋企業電商平台的手動和自動擴展程序。

## 何時進行擴展

### 擴展觸發條件

- CPU 使用率超過 70% 持續 10 分鐘以上
- 記憶體使用率超過 80% 持續 5 分鐘以上
- 請求佇列長度持續增加
- 回應時間下降
- 預期的流量尖峰 (行銷活動、促銷活動)

### 縮減觸發條件

- CPU 使用率低於 30% 持續 30 分鐘以上
- 記憶體使用率低於 50% 持續 30 分鐘以上
- 離峰時段的低請求率
- 成本最佳化需求

## Horizontal Pod Autoscaling (HPA)

### 檢查當前 HPA 狀態

```bash
# Get HPA status
kubectl get hpa -n production

# Describe HPA for details
kubectl describe hpa ecommerce-backend-hpa -n production

# Check current metrics
kubectl get hpa ecommerce-backend-hpa -n production -o yaml

# View HPA events
kubectl get events -n production --field-selector involvedObject.name=ecommerce-backend-hpa
```

### 配置 HPA

```yaml
# hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: ecommerce-backend-hpa
  namespace: production
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: ecommerce-backend
  minReplicas: 4
  maxReplicas: 20
  metrics:

    - type: Resource

      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 60

    - type: Resource

      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 70

    - type: Pods

      pods:
        metric:
          name: http_requests_per_second
        target:
          type: AverageValue
          averageValue: "1000"
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:

        - type: Percent

          value: 50
          periodSeconds: 60

        - type: Pods

          value: 2
          periodSeconds: 60
      selectPolicy: Max
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:

        - type: Percent

          value: 10
          periodSeconds: 60

        - type: Pods

          value: 1
          periodSeconds: 60
      selectPolicy: Min
```

### 套用 HPA 配置

```bash
# Apply HPA
kubectl apply -f hpa.yaml

# Verify HPA is working
kubectl get hpa -n production -w

# Check HPA metrics
kubectl top pods -n production -l app=ecommerce-backend
```

## 手動擴展

### 擴展應用程式 Pod

#### 擴展

```bash
# Check current replica count
kubectl get deployment ecommerce-backend -n production

# Scale up to 8 replicas
kubectl scale deployment/ecommerce-backend --replicas=8 -n production

# Watch scaling progress
kubectl get pods -n production -l app=ecommerce-backend -w

# Verify all pods are running
kubectl get pods -n production -l app=ecommerce-backend | grep "1/1.*Running" | wc -l
```

#### 縮減

```bash
# Scale down to 4 replicas
kubectl scale deployment/ecommerce-backend --replicas=4 -n production

# Watch scaling progress
kubectl get pods -n production -l app=ecommerce-backend -w

# Verify pods terminated gracefully
kubectl get events -n production | grep "ecommerce-backend" | grep "Killing"
```

### 擴展資料庫

#### Read Replica

```bash
# Add read replica
aws rds create-db-instance-read-replica \
  --db-instance-identifier ecommerce-prod-replica-3 \
  --source-db-instance-identifier ecommerce-production \
  --db-instance-class db.r5.xlarge \
  --availability-zone ap-northeast-1c

# Wait for replica to be available
aws rds wait db-instance-available \
  --db-instance-identifier ecommerce-prod-replica-3

# Update application to use new replica
kubectl set env deployment/ecommerce-backend \
  DB_READ_REPLICAS="replica1.xxx.rds.amazonaws.com,replica2.xxx.rds.amazonaws.com,replica3.xxx.rds.amazonaws.com" \
  -n production
```

#### 垂直擴展 (Instance 大小)

```bash
# Modify RDS instance class
aws rds modify-db-instance \
  --db-instance-identifier ecommerce-production \
  --db-instance-class db.r5.2xlarge \
  --apply-immediately

# Monitor modification progress
aws rds describe-db-instances \
  --db-instance-identifier ecommerce-production \
  --query 'DBInstances[0].DBInstanceStatus'

# Wait for modification to complete
aws rds wait db-instance-available \
  --db-instance-identifier ecommerce-production
```

### 擴展 Cache (Redis)

#### 增加 Cache Node

```bash
# Increase number of cache nodes
aws elasticache modify-replication-group \
  --replication-group-id ecommerce-prod-redis \
  --num-cache-clusters 5 \
  --apply-immediately

# Monitor scaling progress
aws elasticache describe-replication-groups \
  --replication-group-id ecommerce-prod-redis \
  --query 'ReplicationGroups[0].Status'
```

#### 垂直擴展 (Node Type)

```bash
# Modify cache node type
aws elasticache modify-replication-group \
  --replication-group-id ecommerce-prod-redis \
  --cache-node-type cache.r5.xlarge \
  --apply-immediately
```

### 擴展 Kafka (MSK)

#### 增加 Broker

```bash
# Get current broker count
aws kafka describe-cluster \
  --cluster-arn ${CLUSTER_ARN} \
  --query 'ClusterInfo.NumberOfBrokerNodes'

# Update broker count
aws kafka update-broker-count \
  --cluster-arn ${CLUSTER_ARN} \
  --current-version ${CURRENT_VERSION} \
  --target-number-of-broker-nodes 6

# Monitor scaling operation
aws kafka describe-cluster-operation \
  --cluster-operation-arn ${OPERATION_ARN}
```

#### 垂直擴展 (Broker Type)

```bash
# Update broker type
aws kafka update-broker-type \
  --cluster-arn ${CLUSTER_ARN} \
  --current-version ${CURRENT_VERSION} \
  --target-instance-type kafka.m5.2xlarge
```

## Cluster 擴展 (EKS Node)

### 檢查當前 Node 狀態

```bash
# Get node count and status
kubectl get nodes

# Check node resource usage
kubectl top nodes

# Check pod distribution
kubectl get pods -n production -o wide | awk '{print $7}' | sort | uniq -c
```

### 擴展 Node Group

#### 使用 eksctl

```bash
# Scale node group
eksctl scale nodegroup \
  --cluster=ecommerce-production \
  --name=ng-1 \
  --nodes=8 \
  --nodes-min=4 \
  --nodes-max=12

# Verify scaling
kubectl get nodes -w
```

#### 使用 AWS CLI

```bash
# Update Auto Scaling Group
aws autoscaling update-auto-scaling-group \
  --auto-scaling-group-name eks-ng-1-xxxxx \
  --min-size 4 \
  --max-size 12 \
  --desired-capacity 8

# Wait for nodes to be ready
kubectl wait --for=condition=Ready nodes --all --timeout=600s
```

### 新增 Node Group

```bash
# Create new node group with larger instances
eksctl create nodegroup \
  --cluster=ecommerce-production \
  --name=ng-large \
  --node-type=t3.xlarge \
  --nodes=3 \
  --nodes-min=2 \
  --nodes-max=6 \
  --node-labels="workload=compute-intensive"

# Migrate pods to new node group
kubectl cordon -l node-group=ng-1
kubectl drain -l node-group=ng-1 --ignore-daemonsets --delete-emptydir-data

# Verify pods running on new nodes
kubectl get pods -n production -o wide
```

## 特定事件的擴展

### 事件前擴展 (行銷活動)

```bash
# 1 hour before event
# Scale application pods
kubectl scale deployment/ecommerce-backend --replicas=12 -n production

# Add database read replicas
aws rds create-db-instance-read-replica \
  --db-instance-identifier ecommerce-prod-replica-temp \
  --source-db-instance-identifier ecommerce-production

# Increase cache capacity
aws elasticache modify-replication-group \
  --replication-group-id ecommerce-prod-redis \
  --num-cache-clusters 6 \
  --apply-immediately

# Scale EKS nodes
eksctl scale nodegroup \
  --cluster=ecommerce-production \
  --name=ng-1 \
  --nodes=10

# Warm up cache
./scripts/warm-cache.sh

# Pre-load frequently accessed data
./scripts/preload-data.sh
```

### 事件後縮減

```bash
# Wait 2 hours after event ends
# Scale down application pods
kubectl scale deployment/ecommerce-backend --replicas=4 -n production

# Remove temporary read replica
aws rds delete-db-instance \
  --db-instance-identifier ecommerce-prod-replica-temp \
  --skip-final-snapshot

# Reduce cache capacity
aws elasticache modify-replication-group \
  --replication-group-id ecommerce-prod-redis \
  --num-cache-clusters 3 \
  --apply-immediately

# Scale down EKS nodes
eksctl scale nodegroup \
  --cluster=ecommerce-production \
  --name=ng-1 \
  --nodes=6
```

## 擴展期間的監控

### 關鍵 Metric

```bash
# Monitor pod CPU/Memory
watch -n 5 'kubectl top pods -n production -l app=ecommerce-backend'

# Monitor node resources
watch -n 5 'kubectl top nodes'

# Monitor API response times
watch -n 5 'curl -s http://localhost:8080/actuator/metrics/http.server.requests | jq ".measurements[0].value"'

# Monitor request rate
watch -n 5 'kubectl logs deployment/ecommerce-backend -n production --tail=100 | grep "HTTP" | wc -l'

# Monitor error rate
watch -n 5 'kubectl logs deployment/ecommerce-backend -n production --tail=100 | grep "ERROR" | wc -l'
```

### Grafana Dashboard

- **Scaling Dashboard**: 即時監控擴展 metric
- **Resource Utilization**: 追蹤 CPU、記憶體、網路
- **Application Performance**: 回應時間、吞吐量、錯誤
- **Cost Dashboard**: 追蹤擴展成本

## 驗證

### 擴展後

- [ ] 所有新 pod 正在執行 (1/1 Ready)
- [ ] 新 pod 通過健康檢查
- [ ] 負載分散到所有 pod
- [ ] 回應時間改善或穩定
- [ ] 錯誤率正常 (< 1%)
- [ ] 無資源限制
- [ ] 資料庫連線健康
- [ ] Cache 命中率維持

### 縮減後

- [ ] 剩餘 pod 正在執行
- [ ] 無服務中斷
- [ ] 回應時間穩定
- [ ] 錯誤率正常
- [ ] 資源使用率適當
- [ ] 成本如預期降低

### 驗證指令

```bash
# Check pod status
kubectl get pods -n production -l app=ecommerce-backend

# Check HPA status
kubectl get hpa -n production

# Check resource usage
kubectl top pods -n production -l app=ecommerce-backend

# Test API endpoints
./scripts/smoke-test.sh production

# Check metrics
curl http://localhost:8080/actuator/metrics/http.server.requests
curl http://localhost:8080/actuator/metrics/jvm.memory.used
```

## 疑難排解

### Pod 無法擴展

```bash
# Check HPA status
kubectl describe hpa ecommerce-backend-hpa -n production

# Check metrics server
kubectl get apiservice v1beta1.metrics.k8s.io -o yaml

# Check resource limits
kubectl describe deployment ecommerce-backend -n production | grep -A 5 "Limits\|Requests"

# Check events
kubectl get events -n production --sort-by='.lastTimestamp' | grep -i scale
```

### Node 無法擴展

```bash
# Check Auto Scaling Group
aws autoscaling describe-auto-scaling-groups \
  --auto-scaling-group-names eks-ng-1-xxxxx

# Check scaling activities
aws autoscaling describe-scaling-activities \
  --auto-scaling-group-name eks-ng-1-xxxxx \
  --max-records 10

# Check node capacity
kubectl describe nodes | grep -A 5 "Allocatable"
```

### 效能未改善

```bash
# Check if pods are actually receiving traffic
kubectl logs deployment/ecommerce-backend -n production --tail=100 | grep "HTTP"

# Check service endpoints
kubectl get endpoints ecommerce-backend -n production

# Check load balancer
kubectl describe service ecommerce-backend -n production

# Check for bottlenecks
kubectl top pods -n production
kubectl top nodes
```

## 成本最佳化

### 適當調整大小

```bash
# Analyze resource usage over time
kubectl top pods -n production -l app=ecommerce-backend --containers

# Adjust resource requests/limits based on actual usage
kubectl set resources deployment/ecommerce-backend \
  --requests=cpu=400m,memory=1.5Gi \
  --limits=cpu=800m,memory=3Gi \
  -n production
```

### Spot Instance

```bash
# Create spot instance node group
eksctl create nodegroup \
  --cluster=ecommerce-production \
  --name=ng-spot \
  --node-type=t3.large \
  --nodes=3 \
  --nodes-min=2 \
  --nodes-max=8 \
  --spot \
  --instance-types=t3.large,t3a.large,t2.large

# Label pods for spot instances
kubectl label deployment ecommerce-backend-worker \
  workload=spot-compatible \
  -n production
```

### 排程擴展

```bash
# Scale down during off-peak hours (2 AM - 6 AM)
# Create CronJob for scheduled scaling
kubectl apply -f - <<EOF
apiVersion: batch/v1
kind: CronJob
metadata:
  name: scale-down-offpeak
  namespace: production
spec:
  schedule: "0 2 * * *"  # 2 AM daily
  jobTemplate:
    spec:
      template:
        spec:
          serviceAccountName: scaler
          containers:

          - name: kubectl

            image: bitnami/kubectl:latest
            command:

            - /bin/sh
            - -c
            - kubectl scale deployment/ecommerce-backend --replicas=2 -n production

          restartPolicy: OnFailure
---
apiVersion: batch/v1
kind: CronJob
metadata:
  name: scale-up-peak
  namespace: production
spec:
  schedule: "0 6 * * *"  # 6 AM daily
  jobTemplate:
    spec:
      template:
        spec:
          serviceAccountName: scaler
          containers:

          - name: kubectl

            image: bitnami/kubectl:latest
            command:

            - /bin/sh
            - -c
            - kubectl scale deployment/ecommerce-backend --replicas=6 -n production

          restartPolicy: OnFailure
EOF
```

## 最佳實踐

### 1. 漸進式擴展

- 以小幅度擴展 (一次 1-2 個 pod)
- 在進一步擴展前監控影響
- 允許 pod 有時間預熱

### 2. 預測性擴展

- 分析歷史流量模式
- 在已知流量尖峰前預先擴展
- 對可預測模式使用排程擴展

### 3. 測試

- 在 staging 環境測試擴展程序
- 進行負載測試以驗證擴展行為
- 在低流量時段練習擴展

### 4. 文件記錄

- 記錄擴展決策
- 追蹤擴展事件和結果
- 根據經驗更新 runbook

## 相關文件

- [High CPU Usage](high-cpu-usage.md)
- [High Memory Usage](high-memory-usage.md)
- [Slow API Responses](slow-api-responses.md)
- [Service Outage](service-outage.md)

---

**Last Updated**: 2025-10-25
**Owner**: DevOps Team
**Review Cycle**: Quarterly
