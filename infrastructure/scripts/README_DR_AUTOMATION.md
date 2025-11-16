# Disaster Recovery 自動化腳本

本目錄包含用於管理和測試增強 Disaster Recovery 自動化系統的腳本和工具。

## 概述

DR 自動化系統提供完全自動化的災難復原能力，包括：

- **自動化容錯移轉**：自動提升 Aurora Global Database 和 DNS 路由更新
- **混沌工程**：自動化測試和驗證 DR 準備狀態
- **每月測試**：排程的完整 DR 測試
- **監控**：DR 組件的即時監控和告警

## 腳本

### dr-automation-test.js

DR 自動化的主要測試和驗證腳本。

#### 安裝

```bash
# 安裝依賴項
cd infrastructure
npm install

# 使腳本可執行
chmod +x scripts/dr-automation-test.js
```

#### 使用方式

##### 測試容錯移轉工作流程

```bash
# 基本容錯移轉測試
node scripts/dr-automation-test.js test-failover \
  --project genai-demo \
  --environment production

# 完整容錯移轉測試，包含所有參數
node scripts/dr-automation-test.js test-failover \
  --project genai-demo \
  --environment production \
  --primary-health-check-id ABCD1234567890 \
  --secondary-health-check-id EFGH1234567890 \
  --global-cluster-id genai-demo-global-cluster \
  --secondary-cluster-id genai-demo-secondary-cluster \
  --hosted-zone-id Z1234567890ABC \
  --domain-name api.kimkao.io \
  --secondary-alb-dns genai-demo-alb-dr-123456789.ap-northeast-1.elb.amazonaws.com \
  --notification-topic-arn arn:aws:sns:ap-northeast-1:123456789012:genai-demo-dr-alerts
```

##### 執行混沌工程測試

```bash
# 每月 DR 測試
node scripts/dr-automation-test.js test-chaos \
  --project genai-demo \
  --environment production \
  --test-type monthly_dr_test

# 健康檢查失敗模擬
node scripts/dr-automation-test.js test-chaos \
  --project genai-demo \
  --environment production \
  --test-type health_check_failure

# 網路分區模擬
node scripts/dr-automation-test.js test-chaos \
  --project genai-demo \
  --environment production \
  --test-type network_partition
```

##### 驗證 DR 準備狀態

```bash
# 全面的 DR 準備狀態驗證
node scripts/dr-automation-test.js validate \
  --project genai-demo \
  --environment production
```

##### 產生 DR 報告

```bash
# 產生全面的 DR 報告
node scripts/dr-automation-test.js report \
  --project genai-demo \
  --environment production \
  --output dr-report-$(date +%Y%m%d).json
```

#### NPM 腳本

為方便起見，提供以下 NPM 腳本：

```bash
# 測試容錯移轉工作流程
npm run dr:test-failover -- --project genai-demo --environment production

# 執行混沌工程測試
npm run dr:test-chaos -- --project genai-demo --environment production

# 驗證 DR 準備狀態
npm run dr:validate -- --project genai-demo --environment production

# 產生 DR 報告
npm run dr:report -- --project genai-demo --environment production --output report.json
```

## 配置

### 環境變數

在執行腳本前設定以下環境變數：

```bash
export AWS_REGION=ap-northeast-1
export AWS_PROFILE=your-aws-profile  # 選用
```

### AWS 憑證

確保已配置適當的 AWS 憑證，並具備以下權限：

- **Step Functions**：執行狀態機
- **Lambda**：調用函式
- **CloudWatch**：讀取指標和告警
- **Route 53**：讀取健康檢查
- **RDS**：讀取叢集資訊
- **Systems Manager**：讀取參數
- **SNS**：發布訊息（用於通知）

### 所需 IAM 權限

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "states:StartExecution",
                "states:DescribeExecution",
                "states:DescribeStateMachine",
                "lambda:InvokeFunction",
                "cloudwatch:GetMetricStatistics",
                "cloudwatch:DescribeAlarms",
                "route53:GetHealthCheck",
                "route53:ListHealthChecks",
                "rds:DescribeDBClusters",
                "rds:DescribeGlobalClusters",
                "ssm:GetParameter",
                "ssm:GetParameters",
                "sns:Publish"
            ],
            "Resource": "*"
        }
    ]
}
```

## 測試類型

### 容錯移轉測試

- **健康檢查驗證**：驗證健康檢查回應性
- **Aurora 提升**：測試 Aurora Global Database 容錯移轉
- **DNS 更新**：驗證 DNS 路由變更
- **端到端**：完整的容錯移轉工作流程

### 混沌工程測試

- **每月 DR 測試**：全面的每月驗證
- **健康檢查失敗**：模擬健康檢查失敗
- **網路分區**：測試網路連接問題
- **資料庫失敗**：模擬資料庫不可用性

### 驗證測試

- **健康檢查**：驗證健康檢查配置
- **Aurora Global Database**：檢查複製狀態
- **DNS 配置**：驗證 DNS 設定
- **自動化組件**：測試 Step Functions 和 Lambda
- **監控**：驗證 CloudWatch 和告警

## 輸出範例

### 成功的容錯移轉測試

```
🚀 Starting failover workflow test...
✅ DR automation configuration loaded
✅ Failover workflow started: arn:aws:states:ap-northeast-1:123456789012:execution:genai-demo-production-dr-failover:dr-failover-test-1640995200000
⏳ Monitoring execution...
   Status: RUNNING
   Status: RUNNING
   Status: SUCCEEDED
✅ Execution completed successfully
📋 Execution output: {
  "statusCode": 200,
  "success": true,
  "operations": [
    {
      "step": "health_validation",
      "success": true,
      "details": { ... }
    },
    {
      "step": "aurora_promotion",
      "success": true,
      "details": { ... }
    },
    {
      "step": "dns_update",
      "success": true,
      "details": { ... }
    }
  ],
  "timestamp": "2024-01-01T12:00:00.000Z"
}
```

### DR 準備狀態驗證

```
🔍 Validating DR readiness...
✅ DR automation configuration loaded
  🔍 Validating health checks...
  🔍 Validating Aurora Global Database...
  🔍 Validating DNS configuration...
  🔍 Validating automation components...
  🔍 Validating monitoring and alerting...
✅ DR system is ready
📊 DR Readiness: 5/5 checks passed (100%)
```

### DR 報告

```
📊 Generating DR report...
✅ DR automation configuration loaded

📋 DR REPORT
==================================================
Project: genai-demo
Environment: production
Overall Status: READY
Checks Passed: 5/5

🔧 RECOMMENDATIONS:
1. DR system is fully operational. Consider running monthly chaos tests to maintain readiness.
```

## 疑難排解

### 常見問題

#### 1. 找不到配置

```
❌ Failed to load DR automation configuration: ParameterNotFound
```

**解決方案**：確保已部署 DR 自動化堆疊且配置參數存在。

```bash
# 檢查參數是否存在
aws ssm get-parameter --name "/genai-demo/production/dr/automation-config"
```

#### 2. 權限不足

```
❌ Failover workflow test failed: AccessDenied
```

**解決方案**：驗證您的 AWS 憑證具備上述所需權限。

#### 3. 找不到狀態機

```
❌ Execution failed: StateMachineDoesNotExist
```

**解決方案**：確保已部署 DR 自動化基礎設施。

```bash
# 列出狀態機
aws stepfunctions list-state-machines --query 'stateMachines[?contains(name, `genai-demo-production-dr`)]'
```

#### 4. 健康檢查問題

```
❌ Health check validation failed: HealthCheckNotFound
```

**解決方案**：驗證健康檢查存在且已正確配置。

```bash
# 列出健康檢查
aws route53 list-health-checks --query 'HealthChecks[?contains(CallerReference, `genai-demo`)]'
```

### 除錯模式

透過設定 LOG_LEVEL 環境變數啟用除錯日誌：

```bash
export LOG_LEVEL=DEBUG
node scripts/dr-automation-test.js validate --project genai-demo --environment production
```

### 手動驗證

您可以使用 AWS CLI 手動驗證 DR 組件：

```bash
# 檢查 Step Functions 狀態機
aws stepfunctions list-state-machines --query 'stateMachines[?contains(name, `genai-demo`)]'

# 檢查 Lambda 函式
aws lambda list-functions --query 'Functions[?contains(FunctionName, `genai-demo-dr`)]'

# 檢查 CloudWatch 儀表板
aws cloudwatch list-dashboards --query 'DashboardEntries[?contains(DashboardName, `genai-demo`)]'

# 檢查 EventBridge 規則
aws events list-rules --query 'Rules[?contains(Name, `genai-demo`)]'
```

## 與 CI/CD 整合

### GitHub Actions

將 DR 測試新增到您的 GitHub Actions 工作流程：

```yaml
- name: Test DR Automation
  run: |
    cd infrastructure
    npm install
    npm run dr:validate -- --project genai-demo --environment production
  env:
    AWS_REGION: ap-northeast-1
    AWS_ACCESS_KEY_ID: ${{ secrets.AWS_ACCESS_KEY_ID }}
    AWS_SECRET_ACCESS_KEY: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
```

### 排程測試

使用 cron 設定排程的 DR 測試：

```bash
# 新增到 crontab 以進行每月測試
0 2 1 * * cd /path/to/infrastructure && npm run dr:test-chaos -- --project genai-demo --environment production --test-type monthly_dr_test
```

## 監控和告警

### CloudWatch 指標

DR 自動化系統發布自訂指標：

- `DR/FailoverSuccess` - 成功的容錯移轉操作
- `DR/FailoverFailure` - 失敗的容錯移轉操作
- `DR/TestSuccess` - 成功的混沌測試
- `DR/TestFailure` - 失敗的混沌測試
- `DR/ReadinessScore` - 整體 DR 準備狀態評分（0-100）

### SNS 通知

為 DR 告警配置 SNS 訂閱：

```bash
# 訂閱 DR 告警
aws sns subscribe \
  --topic-arn arn:aws:sns:ap-northeast-1:123456789012:genai-demo-production-dr-alerts \
  --protocol email \
  --notification-endpoint your-email@example.com
```

## 最佳實踐

1. **定期測試**：每月或在重大基礎設施變更後執行 DR 測試
2. **監控指標**：為 DR 指標設定 CloudWatch 告警
3. **文件變更**：在進行基礎設施變更時更新 DR 文件
4. **驗證配置**：部署後始終驗證 DR 準備狀態
5. **檢視報告**：定期檢視 DR 報告並處理建議
6. **測試場景**：測試不同的失敗場景，不僅僅是正常路徑
7. **更新程序**：隨著基礎設施變更保持 DR 程序更新

## 支援

如有問題或疑問：

1. 檢查上述疑難排解部分
2. 檢視 CloudWatch 日誌以取得詳細的錯誤資訊
3. 參閱主要 DR 自動化文件
4. 聯絡 DevOps 團隊尋求協助

## 相關文件

- [DR Automation Implementation Guide](../docs/DR_AUTOMATION_IMPLEMENTATION.md)
- [Infrastructure Troubleshooting Guide](../TROUBLESHOOTING.md)
- [Multi-Region Architecture Documentation](../MULTI_REGION_ARCHITECTURE.md)
