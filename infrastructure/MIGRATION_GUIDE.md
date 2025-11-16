# 遷移指南：獨立 Stacks → 整合部署

## 概述

本指南協助您從先前的獨立 CDK 應用程式遷移到新的整合部署方法。

## 🎯 遷移優勢

### 之前：3 個獨立應用程式

- **Main Infrastructure**：`bin/infrastructure.ts` 中的 4 個 stacks
- **Analytics Pipeline**：`bin/analytics.ts` 中的 1 個 stack
- **Multi-Region Setup**：`bin/multi-region-deployment.ts` 中的 15+ 個 stacks

### 之後：1 個統一應用程式

- **Consolidated Main**：`bin/infrastructure.ts` 中的 5-6 個 stacks
- **Specialized Multi-Region**：針對複雜 DR 場景保持獨立
- **Deprecated Analytics**：功能移至主應用程式

## 📋 遷移前檢查清單

### 1. 備份目前基礎設施

```bash
# 匯出目前 stack 輸出
aws cloudformation describe-stacks --region us-east-1 > current-stacks-backup.json

# 列出所有現有 stacks
cdk list > current-cdk-stacks.txt
```

### 2. 驗證 CDK 版本

```bash
# 檢查目前 CDK 版本
cdk --version

# 應顯示：2.208.0 或更高版本
# 如果不是，請升級：npm install -g aws-cdk@latest
```

### 3. 檢視目前配置

```bash
# 檢查目前 context 設定
cat cdk.context.json

# 檢視目前部署參數
grep -r "context" bin/
```

## 🔄 遷移場景

### 場景 1：目前僅使用 `bin/infrastructure.ts`

**狀態**：✅ **無需遷移**

您的部署將繼續運作。選用改進：

```bash
# 新增分析功能
cdk deploy --all --context enableAnalytics=true

# 新增告警
cdk deploy --all --context alertEmail=your-email@company.com
```

### 場景 2：目前單獨使用 `bin/analytics.ts`

**需要遷移**：移至整合部署

#### 步驟 1：銷毀現有 Analytics Stack

```bash
# 如果您有現有的 analytics 部署
cdk destroy -a "npx ts-node bin/analytics.ts"
```

#### 步驟 2：部署整合基礎設施

```bash
# 啟用 analytics 部署
./deploy-consolidated.sh production us-east-1 true true
```

#### 步驟 3：驗證 Analytics 元件

```bash
# 檢查 analytics stack 部署
aws cloudformation describe-stacks --stack-name GenAIDemo-Prod-AnalyticsStack

# 驗證 S3 data lake
aws s3 ls | grep data-lake

# 檢查 Kinesis Firehose
aws firehose list-delivery-streams
```

### 場景 3：同時使用 Main + Analytics

**需要遷移**：整合為單一部署

#### 步驟 1：匯出目前配置

```bash
# 儲存目前輸出
aws cloudformation describe-stacks --stack-name YourAnalyticsStack > analytics-outputs.json
aws cloudformation describe-stacks --stack-name YourMainStack > main-outputs.json
```

#### 步驟 2：規劃遷移

```bash
# 首先在 development 中測試整合部署
cdk deploy --all \
  --context environment=development \
  --context enableAnalytics=true \
  --context region=us-east-1
```

#### 步驟 3：遷移 Production

```bash
# 銷毀獨立 analytics stack
cdk destroy YourAnalyticsStack

# 部署整合基礎設施
./deploy-consolidated.sh production us-east-1 true true
```

### 場景 4：使用 Multi-Region 部署

**狀態**：✅ **無需遷移**

Multi-region 部署針對專門的 DR 場景保持獨立：

```bash
# 繼續使用 multi-region 部署
cdk deploy --all -a "npx ts-node bin/multi-region-deployment.ts"
```

**選用**：對單一區域環境使用整合部署：

```bash
# 使用整合方法的 Development/staging
./deploy-consolidated.sh development us-east-1 true

# 使用 multi-region 方法的 Production
cdk deploy --all -a "npx ts-node bin/multi-region-deployment.ts"
```

## 🛠️ 遷移步驟

### 步驟 1：更新相依套件

```bash
cd infrastructure
npm install
npm run build
```

### 步驟 2：在 Development 中測試

```bash
# 首先部署到 development 環境
./deploy-consolidated.sh development us-east-1 true false
```

### 步驟 3：驗證功能

```bash
# 檢查所有 stacks 部署成功
cdk list

# 驗證 stack 輸出
aws cloudformation describe-stacks --stack-name GenAIDemo-Dev-CoreInfrastructureStack
```

### 步驟 4：遷移 Staging

```bash
# 部署到 staging
./deploy-consolidated.sh staging us-east-1 true true
```

### 步驟 5：遷移 Production

```bash
# 部署到 production（需要核准）
cdk deploy --all \
  --context environment=production \
  --context enableAnalytics=true \
  --context enableCdkNag=true \
  --context alertEmail=ops@company.com \
  --require-approval broadening
```

## 🔧 配置對應

### 舊 Analytics 配置 → 新整合

| 舊參數 | 新參數 | 備註 |
|---------------|---------------|-------|
| `--context vpc-id=vpc-xxx` | Automatic | 由 NetworkStack 建立的 VPC |
| `--context kms-key-id=key-xxx` | Automatic | 由 SecurityStack 建立的 KMS key |
| `--context msk-cluster-arn=arn:xxx` | Mock cluster | 可用真實 MSK 整合 |
| `--context alerting-topic-arn=arn:xxx` | Automatic | 由 AlertingStack 建立的 SNS topics |

### Context 參數遷移

```bash
# 舊 analytics 部署
cdk deploy -a "npx ts-node bin/analytics.ts" \
  --context vpc-id=vpc-12345 \
  --context kms-key-id=key-67890

# 新整合部署
cdk deploy --all \
  --context environment=production \
  --context enableAnalytics=true
```

## 🚨 疑難排解

### 問題：Stack 相依性

**問題**：遷移期間的相依性衝突

**解決方案**：

```bash
# 按正確順序部署 stacks
cdk deploy GenAIDemo-Prod-NetworkStack
cdk deploy GenAIDemo-Prod-SecurityStack
cdk deploy GenAIDemo-Prod-AlertingStack
cdk deploy GenAIDemo-Prod-CoreInfrastructureStack
cdk deploy GenAIDemo-Prod-ObservabilityStack
cdk deploy GenAIDemo-Prod-AnalyticsStack
```

### 問題：資源名稱衝突

**問題**：新舊 stacks 之間的資源名稱衝突

**解決方案**：

```bash
# 使用不同的 stack 前綴
cdk deploy --all --context stackPrefix=GenAIDemoV2-
```

### 問題：缺少權限

**問題**：遷移後缺少 IAM 權限

**解決方案**：

```bash
# 首先重新部署 security stack
cdk deploy GenAIDemo-Prod-SecurityStack
cdk deploy --all
```

## ✅ 遷移後驗證

### 1. 驗證所有 Stacks

```bash
# 列出已部署的 stacks
cdk list

# 預期輸出：
# GenAIDemo-Prod-NetworkStack
# GenAIDemo-Prod-SecurityStack
# GenAIDemo-Prod-AlertingStack
# GenAIDemo-Prod-CoreInfrastructureStack
# GenAIDemo-Prod-ObservabilityStack
# GenAIDemo-Prod-AnalyticsStack (if enabled)
```

### 2. 測試 Analytics Pipeline（如已啟用）

```bash
# 檢查 S3 data lake
aws s3 ls s3://genai-demo-production-data-lake-ACCOUNT/

# 測試 Kinesis Firehose
aws firehose put-record \
  --delivery-stream-name genai-demo-production-domain-events-firehose \
  --record '{"Data": "{\"test\": \"data\"}"}'
```

### 3. 驗證監控

```bash
# 檢查 CloudWatch dashboards
aws cloudwatch list-dashboards

# 驗證 SNS topics
aws sns list-topics | grep genai-demo
```

## 📚 後續步驟

### 1. 更新 CI/CD Pipelines

```yaml
# 更新您的 CI/CD 以使用整合部署
deploy:
  script:
    - cd infrastructure
    - npm run deploy:prod
```

### 2. 更新文件

- 更新部署 runbooks
- 更新團隊文件
- 更新監控程序

### 3. 清理舊資源

```bash
# 移除已棄用的 analytics 部署檔案（選用）
# git rm bin/analytics.ts

# 如需要更新 .gitignore
echo "# Deprecated deployment files" >> .gitignore
echo "bin/analytics.js" >> .gitignore
```

## 🎉 遷移完成

成功遷移後，您將擁有：

✅ **統一基礎設施**：單一部署指令
✅ **更好的相依性**：適當的 stack 相依性管理
✅ **共享資源**：高效的資源利用
✅ **一致的配置**：單一配置系統
✅ **增強的監控**：整合的告警和可觀測性

您的基礎設施現已整合並準備好進入生產環境！🚀
