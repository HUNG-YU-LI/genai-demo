# 跨區域配置管理

## 概述

本文件說明在 Secrets Stack 中實作的增強跨區域配置管理系統。該系統為 Active-Active 部署場景提供自動同步 secrets、ConfigMaps 和配置參數的功能，跨越多個 AWS 區域。

## 功能

### 🔄 跨區域 Secret 同步

- AWS Secrets Manager secrets 跨區域自動複製
- 由 EventBridge 事件觸發的即時同步
- 支援多個複製區域
- 衝突解決和錯誤處理

### 🗺️ ConfigMap 同步

- Kubernetes ConfigMaps 自動同步
- 跨區域與 EKS 叢集整合
- 非敏感配置資料的選擇性同步
- 支援自訂命名空間和 ConfigMap 名稱

### 🔍 配置漂移偵測

- 跨區域自動偵測配置不一致
- 每小時漂移偵測掃描
- CloudWatch 指標和告警整合
- 詳細的漂移報告與修復指南

### 🚀 GitOps 多區域部署管線

- 多區域基礎設施的整合部署管線
- Blue-green 部署策略支援
- 失敗時自動回滾
- 健康檢查驗證

## 架構

```text
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Primary       │    │   Region 2      │    │   Region 3      │
│   Region        │    │                 │    │                 │
│                 │    │                 │    │                 │
│ ┌─────────────┐ │    │ ┌─────────────┐ │    │ ┌─────────────┐ │
│ │   Secrets   │◄┼────┼►│   Secrets   │ │    │ │   Secrets   │ │
│ │  Manager    │ │    │ │  Manager    │ │    │ │  Manager    │ │
│ └─────────────┘ │    │ └─────────────┘ │    │ └─────────────┘ │
│                 │    │                 │    │                 │
│ ┌─────────────┐ │    │ ┌─────────────┐ │    │ ┌─────────────┐ │
│ │ ConfigMaps  │◄┼────┼►│ ConfigMaps  │ │    │ │ ConfigMaps  │ │
│ │    (EKS)    │ │    │ │    (EKS)    │ │    │ │    (EKS)    │ │
│ └─────────────┘ │    │ └─────────────┘ │    │ └─────────────┘ │
│                 │    │                 │    │                 │
│ ┌─────────────┐ │    │                 │    │                 │
│ │EventBridge  │ │    │                 │    │                 │
│ │   Rules     │ │    │                 │    │                 │
│ └─────────────┘ │    │                 │    │                 │
│                 │    │                 │    │                 │
│ ┌─────────────┐ │    │                 │    │                 │
│ │   Drift     │ │    │                 │    │                 │
│ │ Detection   │ │    │                 │    │                 │
│ └─────────────┘ │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 組件

### Lambda Functions

#### 1. Cross-Region Sync Lambda

- **函式名稱**：`{project}-{environment}-cross-region-sync`
- **用途**：跨區域同步 secrets
- **觸發器**：EventBridge 事件、手動調用
- **逾時**：10 分鐘
- **記憶體**：512 MB

#### 2. ConfigMap Sync Lambda

- **函式名稱**：`{project}-{environment}-configmap-sync`
- **用途**：同步 Kubernetes 叢集中的 ConfigMaps
- **觸發器**：EventBridge 事件、排程執行
- **逾時**：5 分鐘
- **記憶體**：256 MB

#### 3. Drift Detection Lambda

- **函式名稱**：`{project}-{environment}-drift-detection`
- **用途**：偵測跨區域的配置漂移
- **觸發器**：排程（每小時）、手動調用
- **逾時**：15 分鐘
- **記憶體**：512 MB

### EventBridge Rules

#### Secrets Manager Event Rule

- **規則名稱**：`{project}-{environment}-secrets-events`
- **事件模式**：捕獲 Secrets Manager API 呼叫
- **目標**：Cross-region sync 和 ConfigMap sync Lambdas

### Parameter Store 配置

#### 全域配置參數

- `/genai-demo/{environment}/global/secrets/cross-region-config`
- `/genai-demo/{environment}/global/secrets/gitops-config`
- `/genai-demo/{environment}/global/secrets/configmap-sync-config`
- `/genai-demo/{environment}/global/secrets/drift-detection-config`

## 部署

### 前置條件

- 已安裝 AWS CDK v2.x
- 已配置 AWS CLI 與適當的權限
- 在 CDK context 中啟用多區域部署

### 基本部署

```bash
# 啟用多區域支援部署
./infrastructure/deploy-unified.sh full -e production --enable-multi-region

# 僅部署 secrets 和配置管理
./infrastructure/deploy-unified.sh security -e production --enable-multi-region
```

### 使用自訂區域的進階部署

```bash
# 設定自訂複製區域
export REPLICATION_REGIONS="ap-northeast-1,ap-southeast-1,us-west-2"

# 使用自訂配置部署
./infrastructure/deploy-unified.sh full -e production \
  --enable-multi-region \
  -r ap-east-2 \
  -a ops@company.com
```

## 配置

### Secrets Stack 配置

```typescript
const secretsStack = new SecretsStack(this, 'SecretsStack', {
  environment: 'production',
  projectName: 'genai-demo',
  region: 'ap-east-2',
  vpc: networkStack.vpc,
  secretsManagerKey: kmsStack.secretsManagerKey,
  enableMultiRegion: true,
  replicationRegions: ['ap-northeast-1', 'ap-southeast-1'],
  alertingTopic: alertingStack.alertTopic
});
```

### Parameter Store 配置

```json
{
  "primaryRegion": "ap-east-2",
  "replicationRegions": ["ap-northeast-1", "ap-southeast-1"],
  "syncEnabled": true,
  "driftDetectionEnabled": true,
  "configMapSyncEnabled": true,
  "lastSyncTimestamp": "2025-01-21T10:30:00Z"
}
```

### ConfigMap Sync 配置

```json
{
  "enabled": true,
  "namespace": "default",
  "configMapName": "genai-demo-production-config",
  "syncInterval": "5m",
  "excludeSensitiveKeys": true,
  "includeKeys": ["log_level", "cache_ttl", "max_connections", "session_timeout"],
  "kubernetesServiceAccount": "genai-demo-production-secrets-sync"
}
```

### Drift Detection 配置

```json
{
  "enabled": true,
  "checkInterval": "1h",
  "alertThreshold": 1,
  "autoRemediation": false,
  "includeSecrets": true,
  "includeParameters": true,
  "includeConfigMaps": true,
  "retentionDays": 30
}
```

## 監控和告警

### CloudWatch 指標

#### 配置漂移指標
- `genai-demo/ConfigurationDrift/DriftCount`：偵測到的配置漂移數量
- `genai-demo/ConfigurationDrift/HasDrift`：漂移存在的二元指示器

#### Lambda Function 指標
- `AWS/Lambda/Invocations`：函式調用次數
- `AWS/Lambda/Errors`：函式錯誤次數
- `AWS/Lambda/Duration`：函式執行時間

#### EventBridge 指標
- `AWS/Events/MatchedEvents`：規則匹配的事件數量

### CloudWatch 警報

#### 關鍵警報
```bash
# 偵測到配置漂移
aws cloudwatch put-metric-alarm \
  --alarm-name "ConfigurationDriftDetected" \
  --alarm-description "Configuration drift detected across regions" \
  --metric-name "DriftCount" \
  --namespace "genai-demo/ConfigurationDrift" \
  --statistic "Sum" \
  --period 300 \
  --threshold 1 \
  --comparison-operator "GreaterThanOrEqualToThreshold"

# 跨區域同步失敗
aws cloudwatch put-metric-alarm \
  --alarm-name "CrossRegionSyncFailures" \
  --alarm-description "Cross-region sync Lambda function failures" \
  --metric-name "Errors" \
  --namespace "AWS/Lambda" \
  --dimensions "Name=FunctionName,Value=genai-demo-production-cross-region-sync" \
  --statistic "Sum" \
  --period 300 \
  --threshold 1 \
  --comparison-operator "GreaterThanOrEqualToThreshold"
```

### 儀表板小工具

Observability Stack 會自動建立以下儀表板小工具：
- 配置漂移偵測狀態
- 跨區域同步效能
- ConfigMap 同步狀態
- Secrets Manager 活動
- 多區域健康概覽

## 測試

### 自動化測試
```bash
# 執行綜合測試套件
./infrastructure/test-cross-region-config.sh

# 測試特定組件
aws lambda invoke \
  --function-name genai-demo-production-cross-region-sync \
  --payload '{"action": "test_sync"}' \
  /tmp/sync-test-response.json
```

### 手動測試

#### 測試 Secret 同步
```bash
# 在主要區域更新 secret
aws secretsmanager update-secret \
  --secret-id "production/genai-demo/application" \
  --secret-string '{"test_key": "test_value"}' \
  --region ap-east-2

# 等待同步（30 秒）
sleep 30

# 在複製區域中驗證
aws secretsmanager get-secret-value \
  --secret-id "production/genai-demo/application" \
  --region ap-northeast-1
```

#### 測試漂移偵測
```bash
# 手動觸發漂移偵測
aws lambda invoke \
  --function-name genai-demo-production-drift-detection \
  --payload '{"action": "manual_check"}' \
  /tmp/drift-response.json

# 檢查結果
cat /tmp/drift-response.json
```

## 疑難排解

### 常見問題

#### 1. 跨區域同步失敗
**症狀**：Secrets 未跨區域同步
**原因**：
- IAM 權限問題
- 網路連接問題
- KMS 金鑰存取問題

**解決方案**：
```bash
# 檢查 Lambda 函式日誌
aws logs describe-log-groups --log-group-name-prefix "/aws/lambda/genai-demo"

# 驗證 IAM 權限
aws iam simulate-principal-policy \
  --policy-source-arn "arn:aws:iam::ACCOUNT:role/CrossRegionSyncLambdaRole" \
  --action-names "secretsmanager:GetSecretValue" \
  --resource-arns "*"

# 測試網路連接
aws lambda invoke \
  --function-name genai-demo-production-cross-region-sync \
  --payload '{"action": "connectivity_test"}' \
  /tmp/connectivity-test.json
```

#### 2. ConfigMap 同步問題
**症狀**：ConfigMaps 在 Kubernetes 中未更新
**原因**：
- EKS 叢集存取問題
- Service account 權限
- Kubernetes API 連接

**解決方案**：
```bash
# 檢查 EKS 叢集狀態
aws eks describe-cluster --name genai-demo-production-cluster

# 驗證 service account
kubectl get serviceaccount genai-demo-production-secrets-sync

# 檢查 ConfigMap
kubectl get configmap genai-demo-production-config -o yaml
```

#### 3. 漂移偵測誤報
**症狀**：預期差異的漂移警報
**原因**：
- 同步期間的時間問題
- 預期的區域差異
- 配置雜湊不匹配

**解決方案**：
```bash
# 檢查漂移偵測配置
aws ssm get-parameter \
  --name "/genai-demo/production/global/secrets/drift-detection-config"

# 檢視漂移偵測日誌
aws logs filter-log-events \
  --log-group-name "/aws/lambda/genai-demo-production-drift-detection" \
  --start-time $(date -d '1 hour ago' +%s)000
```

### 日誌分析

#### Lambda Function 日誌
```bash
# Cross-region sync 日誌
aws logs tail /aws/lambda/genai-demo-production-cross-region-sync --follow

# ConfigMap sync 日誌
aws logs tail /aws/lambda/genai-demo-production-configmap-sync --follow

# Drift detection 日誌
aws logs tail /aws/lambda/genai-demo-production-drift-detection --follow
```

#### EventBridge 事件追蹤
```bash
# 檢查 EventBridge 規則指標
aws cloudwatch get-metric-statistics \
  --namespace "AWS/Events" \
  --metric-name "MatchedEvents" \
  --dimensions "Name=RuleName,Value=genai-demo-production-secrets-events" \
  --start-time $(date -d '1 hour ago' --iso-8601) \
  --end-time $(date --iso-8601) \
  --period 300 \
  --statistics Sum
```

## 安全性考量

### IAM 權限
- Lambda 函式使用最小權限 IAM 角色
- 明確授予跨區域存取
- KMS 金鑰權限是區域特定的

### 加密
- 所有 secrets 使用客戶管理的 KMS 金鑰加密
- 跨區域複製維持加密
- ConfigMaps 排除敏感資料

### 網路安全
- Lambda 函式在私有子網路中執行
- 使用 VPC 端點存取 AWS 服務
- 安全群組限制出站流量

## 最佳實踐

### 1. Secret 管理
- 使用帶環境前綴的描述性 secret 名稱
- 實施適當的 secret 輪換排程
- 監控 secret 存取模式

### 2. 配置漂移
- 設定適當的漂移偵測閾值
- 為關鍵漂移實施自動修復
- 定期檢視漂移偵測報告

### 3. 多區域部署
- 定期測試容錯移轉場景
- 監控跨區域延遲
- 實施適當的健康檢查

### 4. 監控和告警
- 設定綜合 CloudWatch 警報
- 使用 SNS 進行關鍵告警通知
- 實施升級程序

## 效能優化

### Lambda Function 優化
- 使用適當的記憶體配置
- 為 AWS 客戶端實施連接池
- 快取經常存取的資料

### EventBridge 優化
- 使用特定事件模式減少雜訊
- 實施適當的錯誤處理和重試
- 監控規則效能指標

### 跨區域優化
- 根據延遲需求選擇複製區域
- 為讀取操作實施智能路由
- 在適當的地方使用區域快取

## 維護

### 定期任務
- 檢視和更新 IAM 政策
- 監控 Lambda 函式效能
- 更新漂移偵測閾值
- 測試災難復原程序

### 季度檢視
- 分析跨區域同步效能
- 檢視安全配置
- 更新文件
- 進行容錯移轉測試

### 年度任務
- 跨區域存取的安全稽核
- 效能優化檢視
- 成本分析和優化
- 架構檢視和更新

## 支援和文件

### 其他資源
- [AWS Secrets Manager Documentation](https://docs.aws.amazon.com/secretsmanager/)
- [AWS EventBridge Documentation](https://docs.aws.amazon.com/eventbridge/)
- [Kubernetes ConfigMap Documentation](https://kubernetes.io/docs/concepts/configuration/configmap/)

### 取得協助
- 檢查 CloudWatch 日誌以取得詳細的錯誤資訊
- 使用測試腳本進行自動化診斷
- 檢視 Parameter Store 配置設定
- 對於複雜問題請聯絡開發團隊

---

**最後更新**：2025 年 1 月 21 日
**版本**：1.0
**維護者**：開發團隊
