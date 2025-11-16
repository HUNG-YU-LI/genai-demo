# Runbook: Failed Deployment

## Symptoms

- Deployment 停滯在進行中
- 新的 pods 無法啟動
- ImagePullBackOff 錯誤
- CrashLoopBackOff 錯誤
- Rollout 狀態顯示失敗
- 新 pods 的 health checks 失敗

## Impact

- **Severity**：P1 - High（如果是 production deployment 則為 P0）
- **Affected Users**：如果 production deployment 失敗，可能影響所有使用者
- **Business Impact**：服務中斷、功能發布延遲

## Detection

- **Alert**：`DeploymentFailed` alert 觸發
- **Monitoring Dashboard**：Deployment Dashboard > Rollout Status
- **Log Patterns**：
  - `Failed to pull image`
  - `Back-off restarting failed container`
  - `Readiness probe failed`
  - `Liveness probe failed`

## Diagnosis

### 步驟 1：檢查 Deployment 狀態

```bash
# Check rollout status
kubectl rollout status deployment/ecommerce-backend -n ${NAMESPACE}

# Get deployment details
kubectl describe deployment ecommerce-backend -n ${NAMESPACE}

# Check replica sets
kubectl get rs -n ${NAMESPACE} -l app=ecommerce-backend

# Check pod status
kubectl get pods -n ${NAMESPACE} -l app=ecommerce-backend
```

### 步驟 2：識別失敗的 Pods

```bash
# Get pods with issues
kubectl get pods -n ${NAMESPACE} -l app=ecommerce-backend | grep -v "Running\|Completed"

# Describe problematic pod
kubectl describe pod ${POD_NAME} -n ${NAMESPACE}

# Check pod events
kubectl get events -n ${NAMESPACE} --field-selector involvedObject.name=${POD_NAME} --sort-by='.lastTimestamp'
```

### 步驟 3：分析 Pod Logs

```bash
# Check current container logs
kubectl logs ${POD_NAME} -n ${NAMESPACE}

# Check previous container logs (if pod restarted)
kubectl logs ${POD_NAME} -n ${NAMESPACE} --previous

# Check init container logs
kubectl logs ${POD_NAME} -n ${NAMESPACE} -c init-container-name

# Stream logs in real-time
kubectl logs -f ${POD_NAME} -n ${NAMESPACE}
```

### 步驟 4：檢查 Image 問題

```bash
# Verify image exists in ECR
aws ecr describe-images \
  --repository-name ecommerce-backend \
  --image-ids imageTag=${VERSION}

# Check image pull secrets
kubectl get secret -n ${NAMESPACE} | grep ecr

# Verify secret is valid
kubectl get secret ecr-registry-secret -n ${NAMESPACE} -o yaml

# Test image pull manually
docker pull ${ECR_REGISTRY}/ecommerce-backend:${VERSION}
```

### 步驟 5：檢查設定問題

```bash
# Check ConfigMap
kubectl get configmap ecommerce-config -n ${NAMESPACE} -o yaml

# Check Secrets
kubectl get secret ecommerce-secrets -n ${NAMESPACE} -o jsonpath='{.data}' | jq

# Verify environment variables
kubectl exec -it ${POD_NAME} -n ${NAMESPACE} -- env | grep -E "DB_|REDIS_|KAFKA_"

# Check mounted volumes
kubectl exec -it ${POD_NAME} -n ${NAMESPACE} -- ls -la /config
```

### 步驟 6：檢查資源限制

```bash
# Check node resources
kubectl describe nodes | grep -A 5 "Allocated resources"

# Check if pods are pending due to resources
kubectl get pods -n ${NAMESPACE} -o wide | grep Pending

# Check resource quotas
kubectl get resourcequota -n ${NAMESPACE}

# Check limit ranges
kubectl get limitrange -n ${NAMESPACE}
```

### 步驟 7：檢查 Health Probes

```bash
# Test readiness probe manually
kubectl exec -it ${POD_NAME} -n ${NAMESPACE} -- \
  curl -f http://localhost:8080/actuator/health/readiness

# Test liveness probe manually
kubectl exec -it ${POD_NAME} -n ${NAMESPACE} -- \
  curl -f http://localhost:8080/actuator/health/liveness

# Check probe configuration
kubectl get deployment ecommerce-backend -n ${NAMESPACE} -o yaml | grep -A 10 "livenessProbe\|readinessProbe"
```

## Resolution

### 立即行動

1. **暫停 rollout** 以防止進一步問題：

```bash
kubectl rollout pause deployment/ecommerce-backend -n ${NAMESPACE}
```

1. **評估影響**：

```bash
# Check how many pods are healthy
kubectl get pods -n ${NAMESPACE} -l app=ecommerce-backend | grep "Running.*1/1" | wc -l

# Check if service is still available
curl https://${ENVIRONMENT}.ecommerce.example.com/actuator/health
```

1. **決定行動**：
   - 如果有足夠的健康 pods：修復並恢復
   - 如果服務降級：立即 rollback

### 根本原因修復

#### 如果由 image pull 失敗造成

1. **驗證 image 存在**：

```bash
aws ecr describe-images \
  --repository-name ecommerce-backend \
  --image-ids imageTag=${VERSION}
```

1. **更新過期的 image pull secret**：

```bash
# Get new ECR token
TOKEN=$(aws ecr get-login-password --region ${AWS_REGION})

# Update secret
kubectl create secret docker-registry ecr-registry-secret \
  --docker-server=${ECR_REGISTRY} \
  --docker-username=AWS \
  --docker-password=${TOKEN} \
  --namespace=${NAMESPACE} \
  --dry-run=client -o yaml | kubectl apply -f -

# Restart deployment
kubectl rollout restart deployment/ecommerce-backend -n ${NAMESPACE}
```

1. **修復不正確的 image tag**：

```bash
kubectl set image deployment/ecommerce-backend \
  ecommerce-backend=${ECR_REGISTRY}/ecommerce-backend:${CORRECT_VERSION} \
  -n ${NAMESPACE}
```

#### 如果由 application 啟動失敗造成

1. **檢查 application logs** 查找啟動錯誤：

```bash
kubectl logs ${POD_NAME} -n ${NAMESPACE} | grep -i "error\|exception\|failed"
```

1. **常見啟動問題**：

**Database connection 失敗**：

```bash
# Verify database connectivity
kubectl exec -it ${POD_NAME} -n ${NAMESPACE} -- \
  pg_isready -h ${DB_HOST} -p 5432

# Check database credentials
kubectl get secret ecommerce-secrets -n ${NAMESPACE} -o jsonpath='{.data.DB_PASSWORD}' | base64 -d

# Test connection
kubectl exec -it ${POD_NAME} -n ${NAMESPACE} -- \
  psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} -c "SELECT 1"
```

**遺失的 environment variables**：

```bash
# Check required env vars
kubectl exec -it ${POD_NAME} -n ${NAMESPACE} -- env | sort

# Add missing env vars
kubectl set env deployment/ecommerce-backend \
  MISSING_VAR=value \
  -n ${NAMESPACE}
```

**設定檔問題**：

```bash
# Check ConfigMap
kubectl get configmap ecommerce-config -n ${NAMESPACE} -o yaml

# Update ConfigMap
kubectl create configmap ecommerce-config \
  --from-file=application.yml \
  --namespace=${NAMESPACE} \
  --dry-run=client -o yaml | kubectl apply -f -

# Restart to pick up changes
kubectl rollout restart deployment/ecommerce-backend -n ${NAMESPACE}
```

#### 如果由 health check 失敗造成

1. **調整 health check 時間**：

```yaml
# Increase initialDelaySeconds if app needs more time to start
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 60  # Increase from 30
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 30  # Increase from 10
  periodSeconds: 5
  timeoutSeconds: 3
  failureThreshold: 3
```

1. **修復 health check endpoint**：

```java
@Component
public class CustomHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        try {
            // Check critical dependencies
            checkDatabase();
            checkRedis();

            return Health.up()
                .withDetail("database", "UP")
                .withDetail("redis", "UP")
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

#### 如果由資源限制造成

1. **檢查 node 容量**：

```bash
kubectl describe nodes | grep -A 5 "Allocated resources"
```

1. **必要時擴展 cluster**：

```bash
# Add more nodes
eksctl scale nodegroup \
  --cluster=ecommerce-${ENV} \
  --name=ng-1 \
  --nodes=6 \
  --nodes-min=3 \
  --nodes-max=10
```

1. **調整資源 requests**：

```yaml
resources:
  requests:
    memory: "1Gi"    # Reduce if too high
    cpu: "250m"      # Reduce if too high
  limits:
    memory: "2Gi"
    cpu: "500m"
```

#### 如果由 database migration 失敗造成

1. **檢查 migration 狀態**：

```bash
kubectl logs ${POD_NAME} -n ${NAMESPACE} | grep -i "flyway\|migration"
```

1. **修復 migration**：

```bash
# Connect to database
kubectl exec -it ${POD_NAME} -n ${NAMESPACE} -- \
  psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME}

# Check migration history
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 10;

# Mark failed migration as resolved if needed
UPDATE flyway_schema_history
SET success = true
WHERE version = '1.2.3' AND success = false;

# Or repair Flyway
kubectl exec -it ${POD_NAME} -n ${NAMESPACE} -- \
  ./gradlew flywayRepair
```

### Rollback 程序

如果修復無法快速完成，請 rollback：

```bash
# Rollback to previous version
kubectl rollout undo deployment/ecommerce-backend -n ${NAMESPACE}

# Or rollback to specific revision
kubectl rollout history deployment/ecommerce-backend -n ${NAMESPACE}
kubectl rollout undo deployment/ecommerce-backend -n ${NAMESPACE} --to-revision=3

# Monitor rollback
kubectl rollout status deployment/ecommerce-backend -n ${NAMESPACE}

# Verify rollback
kubectl get pods -n ${NAMESPACE} -l app=ecommerce-backend
curl https://${ENVIRONMENT}.ecommerce.example.com/actuator/health
```

## Verification

- [ ] 所有 pods 都在執行（1/1 Ready）
- [ ] Deployment rollout 成功完成
- [ ] Health checks 通過
- [ ] 最近的 logs 中無錯誤
- [ ] API endpoints 正確回應
- [ ] Smoke tests 通過
- [ ] Metrics 顯示正常行為

### 驗證指令

```bash
# Check deployment status
kubectl rollout status deployment/ecommerce-backend -n ${NAMESPACE}

# Verify all pods healthy
kubectl get pods -n ${NAMESPACE} -l app=ecommerce-backend

# Check health endpoints
curl https://${ENVIRONMENT}.ecommerce.example.com/actuator/health
curl https://${ENVIRONMENT}.ecommerce.example.com/actuator/info

# Run smoke tests
./scripts/run-smoke-tests.sh ${ENVIRONMENT}

# Check metrics
curl https://${ENVIRONMENT}.ecommerce.example.com/actuator/metrics
```

## Prevention

### 1. Pre-Deployment 驗證

```bash
# Validate deployment manifest
kubectl apply --dry-run=client -f deployment.yaml

# Validate with kubeval
kubeval deployment.yaml

# Run deployment tests in staging
./scripts/test-deployment.sh staging
```

### 2. Deployment 最佳實踐

```yaml
# Use deployment strategy
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxSurge: 1
    maxUnavailable: 0  # Ensure zero downtime

# Set appropriate resource limits
resources:
  requests:
    memory: "1Gi"
    cpu: "500m"
  limits:
    memory: "2Gi"
    cpu: "1000m"

# Configure health checks properly
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 10
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 5
  failureThreshold: 3
```

### 3. 自動化測試

```yaml
# CI/CD pipeline checks

- name: Build and test

  run: ./gradlew build test

- name: Build Docker image

  run: docker build -t ${IMAGE}:${TAG} .

- name: Scan image for vulnerabilities

  run: trivy image ${IMAGE}:${TAG}

- name: Deploy to staging

  run: kubectl apply -f k8s/staging/

- name: Run smoke tests

  run: ./scripts/smoke-test.sh staging

- name: Run integration tests

  run: ./scripts/integration-test.sh staging
```

### 4. Monitoring 和 Alerts

```yaml
# Set up deployment monitoring

- alert: DeploymentStuck

  expr: kube_deployment_status_replicas_updated{deployment="ecommerce-backend"} < kube_deployment_spec_replicas{deployment="ecommerce-backend"}
  for: 10m

- alert: PodCrashLooping

  expr: rate(kube_pod_container_status_restarts_total{pod=~"ecommerce-backend.*"}[15m]) > 0
  for: 5m
```

### 5. Deployment 檢查清單

- [ ] 所有測試在 CI/CD 中通過
- [ ] Staging deployment 成功
- [ ] Database migrations 已測試
- [ ] 設定已驗證
- [ ] 資源限制適當
- [ ] Health checks 已設定
- [ ] Rollback plan 準備就緒
- [ ] Monitoring 已就位
- [ ] 團隊已通知

## Escalation

- **L1 Support**：DevOps team（立即回應）
- **L2 Support**：Backend engineering team（程式碼/設定問題）
- **L3 Support**：Platform team（基礎設施問題）
- **On-Call Engineer**：查看 PagerDuty

## 相關

- [Rollback Procedures](../deployment/rollback.md)
- [Service Outage](service-outage.md)
- [Database Connection Issues](database-connection-issues.md)
- [Pod Restart Loop](pod-restart-loop.md)

---

**Last Updated**：2025-10-25
**Owner**：DevOps Team
**Review Cycle**：Monthly
