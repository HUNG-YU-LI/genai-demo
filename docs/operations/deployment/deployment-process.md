# Deployment 流程

## 概述

本文件提供 Enterprise E-Commerce Platform 跨不同環境的逐步 deployment 程序。

## Deployment 環境

- **Local**：開發人員工作站
- **Staging**：Pre-production 測試環境
- **Production**：Live 客戶面向環境

## 先決條件

### 所需工具

- AWS CLI v2.x
- kubectl v1.28+
- Docker v24.x+
- Gradle 8.x
- Node.js 18.x+（用於 frontend deployments）

### 所需存取權限

- 具有適當權限的 AWS IAM credentials
- EKS cluster 存取權限（kubeconfig）
- Container registry 存取權限（ECR）
- Secrets Manager 存取權限

## Deployment 到 Staging

### 步驟 1：Pre-Deployment 檢查

```bash
# 驗證 AWS credentials
aws sts get-caller-identity

# 驗證 kubectl 存取權限
kubectl cluster-info

# 檢查當前 staging status
kubectl get pods -n staging
kubectl get services -n staging
```

### 步驟 2：建置 Application

```bash
# 建置 backend application
cd app
./gradlew clean build

# 執行測試
./gradlew test

# 建置 Docker image
docker build -t ecommerce-backend:${VERSION} .

# 為 ECR 標記
docker tag ecommerce-backend:${VERSION} \
  ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/ecommerce-backend:${VERSION}
```

### 步驟 3：推送到 Container Registry

```bash
# 登入 ECR
aws ecr get-login-password --region ${AWS_REGION} | \
  docker login --username AWS --password-stdin \
  ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com

# 推送 image
docker push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/ecommerce-backend:${VERSION}
```

### 步驟 4：更新 Kubernetes Manifests

```bash
# 使用新 image 版本更新 deployment manifest
cd infrastructure/k8s/overlays/staging

# 編輯 kustomization.yaml 以更新 image tag
kustomize edit set image \
  ecommerce-backend=${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/ecommerce-backend:${VERSION}
```

### 步驟 5：套用 Database Migrations

```bash
# 執行 Flyway migrations
kubectl exec -it deployment/ecommerce-backend -n staging -- \
  ./gradlew flywayMigrate -Dflyway.url=jdbc:postgresql://${DB_HOST}:5432/${DB_NAME}

# 驗證 migration status
kubectl exec -it deployment/ecommerce-backend -n staging -- \
  ./gradlew flywayInfo
```

### 步驟 6：Deploy 到 Staging

```bash
# 套用 Kubernetes manifests
kubectl apply -k infrastructure/k8s/overlays/staging

# 觀察 deployment 進度
kubectl rollout status deployment/ecommerce-backend -n staging

# 驗證 pods 正在運行
kubectl get pods -n staging -l app=ecommerce-backend
```

### 步驟 7：驗證 Deployment

```bash
# 檢查 pod logs
kubectl logs -f deployment/ecommerce-backend -n staging

# 檢查 service endpoints
kubectl get svc -n staging

# 執行 smoke tests
./scripts/run-smoke-tests.sh staging

# 驗證 health endpoints
curl https://staging.ecommerce.example.com/actuator/health
curl https://staging.ecommerce.example.com/actuator/info
```

### 步驟 8：Post-Deployment 驗證

- [ ] 所有 pods 處於 Running 狀態
- [ ] Health checks 正在通過
- [ ] Smoke tests 通過
- [ ] CloudWatch 中沒有 error logs
- [ ] Metrics 正在報告
- [ ] Database 連線健康

## Deployment 到 Production

### 步驟 1：Pre-Deployment 檢查

```bash
# 驗證 staging deployment 穩定
./scripts/verify-staging-health.sh

# 檢查 production status
kubectl get pods -n production
kubectl get services -n production

# 驗證沒有正在進行的 incidents
# 檢查 monitoring dashboards
# 審查最近的 error rates
```

### 步驟 2：創建 Deployment 計畫

記錄以下內容：

- Deployment 時間窗口（日期和時間）
- 預期的停機時間（如果有）
- Rollback 計畫
- 溝通計畫
- On-call 工程師

### 步驟 3：通知利益相關者

```bash
# 發送 deployment 通知
# - Engineering team
# - Product team
# - Customer support
# - Management

# 通知範例：
# Subject: Production Deployment - [DATE] [TIME]
# - Version: ${VERSION}
# - Changes: [CHANGELOG_URL]
# - Expected duration: 30 minutes
# - Rollback plan: Available
```

### 步驟 4：啟用 Maintenance Mode（如需要）

```bash
# 對於零停機時間的 deployments，跳過此步驟
# 對於需要停機時間的 deployments：

kubectl apply -f infrastructure/k8s/maintenance-mode.yaml
```

### 步驟 5：建置並推送 Production Image

```bash
# 建置 production image
docker build -t ecommerce-backend:${VERSION} \
  --build-arg ENV=production .

# 為 production ECR 標記
docker tag ecommerce-backend:${VERSION} \
  ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/ecommerce-backend:${VERSION}-prod

# 推送到 production registry
docker push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/ecommerce-backend:${VERSION}-prod
```

### 步驟 6：套用 Database Migrations

```bash
# 在 migration 前備份 database
aws rds create-db-snapshot \
  --db-instance-identifier ecommerce-prod \
  --db-snapshot-identifier ecommerce-prod-pre-${VERSION}

# 執行 migrations
kubectl exec -it deployment/ecommerce-backend -n production -- \
  ./gradlew flywayMigrate -Dflyway.url=jdbc:postgresql://${PROD_DB_HOST}:5432/${PROD_DB_NAME}

# 驗證 migration
kubectl exec -it deployment/ecommerce-backend -n production -- \
  ./gradlew flywayInfo
```

### 步驟 7：使用 Canary 策略 Deploy

```bash
# Deploy canary（10% 流量）
kubectl apply -f infrastructure/k8s/overlays/production/canary.yaml

# 監控 canary metrics 15 分鐘
# - Error rate
# - Response time
# - CPU/Memory usage
# - Business metrics（訂單、付款）

# 如果 canary 健康，繼續進行完整 deployment
kubectl apply -k infrastructure/k8s/overlays/production

# 觀察 rollout
kubectl rollout status deployment/ecommerce-backend -n production
```

### 步驟 8：驗證 Production Deployment

```bash
# 檢查所有 pods 正在運行
kubectl get pods -n production -l app=ecommerce-backend

# 驗證 health endpoints
curl https://api.ecommerce.example.com/actuator/health

# 執行 production smoke tests
./scripts/run-smoke-tests.sh production

# 監控關鍵 metrics
# - API 回應時間
# - Error rates
# - Database 連線
# - Cache hit rates
```

### 步驟 9：Post-Deployment 監控

在 deployment 後監控 1 小時：

- [ ] Error rates 正常
- [ ] 回應時間在 SLA 內
- [ ] 客戶支援票券沒有增加
- [ ] Business metrics 正常（訂單、付款）
- [ ] 沒有觸發 alerts

### 步驟 10：停用 Maintenance Mode

```bash
# 如果啟用了 maintenance mode
kubectl delete -f infrastructure/k8s/maintenance-mode.yaml
```

### 步驟 11：Post-Deployment 溝通

```bash
# 發送 deployment 完成通知
# Subject: Production Deployment Complete - [VERSION]
# - Deployment status: Success
# - All systems operational
# - Monitoring continues for 24 hours
```

## Frontend Deployment

### CMC Frontend (Next.js)

```bash
# 建置 frontend
cd cmc-frontend
npm run build

# Deploy 到 S3 + CloudFront
aws s3 sync out/ s3://cmc-frontend-${ENV}/

# 使 CloudFront cache 失效
aws cloudfront create-invalidation \
  --distribution-id ${DISTRIBUTION_ID} \
  --paths "/*"
```

### Consumer Frontend (Angular)

```bash
# 建置 frontend
cd consumer-frontend
npm run build:${ENV}

# Deploy 到 S3 + CloudFront
aws s3 sync dist/ s3://consumer-frontend-${ENV}/

# 使 CloudFront cache 失效
aws cloudfront create-invalidation \
  --distribution-id ${DISTRIBUTION_ID} \
  --paths "/*"
```

## Deployment Checklist

### Pre-Deployment

- [ ] 所有測試在 CI/CD 中通過
- [ ] Code review 完成
- [ ] Security 掃描通過
- [ ] Performance 測試通過
- [ ] Staging deployment 成功
- [ ] Deployment 計畫已記錄
- [ ] Rollback 計畫已準備
- [ ] 利益相關者已通知

### During Deployment

- [ ] Database backup 已創建
- [ ] Migrations 成功套用
- [ ] Application 已部署
- [ ] Health checks 通過
- [ ] Smoke tests 通過
- [ ] Monitoring 活動中

### Post-Deployment

- [ ] 所有 services 健康
- [ ] Metrics 在正常範圍內
- [ ] 沒有 critical errors
- [ ] 客戶面向功能正常運作
- [ ] Deployment 已記錄
- [ ] 利益相關者已通知

## Deployment 自動化

### CI/CD Pipeline

```yaml
# .github/workflows/deploy.yml
name: Deploy to Production

on:
  push:
    tags:
      - 'v*'

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v3

      - name: Build application
        run: ./gradlew build

      - name: Build Docker image
        run: docker build -t ecommerce-backend:${GITHUB_REF_NAME} .

      - name: Push to ECR
        run: |
          aws ecr get-login-password | docker login --username AWS --password-stdin
          docker push ${ECR_REGISTRY}/ecommerce-backend:${GITHUB_REF_NAME}

      - name: Deploy to EKS
        run: |
          kubectl set image deployment/ecommerce-backend \
            ecommerce-backend=${ECR_REGISTRY}/ecommerce-backend:${GITHUB_REF_NAME}
```

## 疑難排解

### Deployment 失敗

```bash
# 檢查 pod status
kubectl describe pod ${POD_NAME} -n ${NAMESPACE}

# 檢查 logs
kubectl logs ${POD_NAME} -n ${NAMESPACE}

# 檢查 events
kubectl get events -n ${NAMESPACE} --sort-by='.lastTimestamp'
```

### 需要 Rollback

參見 [Rollback 程序](rollback.md) 以獲取詳細的 rollback 步驟。

## 相關文件

- [環境配置](environments.md)
- [Rollback 程序](rollback.md)
- [Monitoring 指南](../monitoring/monitoring-strategy.md)
- [疑難排解指南](../troubleshooting/common-issues.md)

---

**Last Updated**：2025-10-25
**Owner**：DevOps Team
**Review Cycle**：Quarterly
