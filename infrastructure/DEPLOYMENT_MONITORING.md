# 部署監控堆疊

## 概述

Deployment Monitoring Stack 為 AWS Code Services 部署提供全面的監控和告警，包括 CodePipeline 和 CodeDeploy。此堆疊滿足多區域 active-active 部署的監控需求。

## 功能

### 📊 部署指標收集

自動收集並發佈以下指標到 CloudWatch：

#### CodePipeline 指標
- **Pipeline Success Rate**：成功的管線執行百分比
- **Pipeline Execution Time**：管線執行完成的平均時間
- **Pipeline Failures**：失敗的管線執行次數

#### CodeDeploy 指標
- **Deployment Success Rate**：成功的部署百分比
- **Deployment Time**：部署完成的平均時間
- **Deployment Failures**：失敗的部署次數

### 🔔 即時告警

部署問題的自動化告警：

1. **Pipeline Failure Alert**：任何管線執行失敗時觸發
2. **Deployment Failure Alert**：任何部署失敗時觸發
3. **Low Success Rate Alert**：部署成功率低於 80% 時觸發
4. **Long Deployment Time Alert**：部署超過 30 分鐘時觸發
5. **Long Pipeline Execution Alert**：管線執行超過 60 分鐘時觸發

### 📈 CloudWatch 儀表板

互動式儀表板包含：
- 即時成功率追蹤
- 部署時間趨勢
- 失敗追蹤和分析
- 多區域部署狀態（啟用時）

### 🎯 EventBridge 整合

從以下來源捕獲部署事件：
- CodePipeline 狀態變更（SUCCESS、FAILED）
- CodeDeploy 狀態變更（SUCCESS、FAILURE、STOPPED）

## 架構

```
┌─────────────────────────────────────────────────────────────┐
│                  Deployment Monitoring Stack                 │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────┐      ┌──────────────────────────────┐    │
│  │ EventBridge  │─────▶│  Event Handler Lambdas       │    │
│  │   Rules      │      │  - Pipeline Events           │    │
│  └──────────────┘      │  - Deploy Events             │    │
│         │              └──────────────────────────────┘    │
│         │                          │                        │
│         │                          ▼                        │
│         │              ┌──────────────────────────────┐    │
│         └─────────────▶│   SNS Alert Topic            │    │
│                        └──────────────────────────────┘    │
│                                                               │
│  ┌──────────────┐      ┌──────────────────────────────┐    │
│  │  Scheduled   │─────▶│  Metrics Collection Lambda   │    │
│  │  Rule (5min) │      │  - Collect Pipeline Metrics  │    │
│  └──────────────┘      │  - Collect Deploy Metrics    │    │
│                        │  - Publish to CloudWatch     │    │
│                        └──────────────────────────────┘    │
│                                    │                        │
│                                    ▼                        │
│                        ┌──────────────────────────────┐    │
│                        │   CloudWatch Metrics         │    │
│                        │   - Success Rates            │    │
│                        │   - Execution Times          │    │
│                        │   - Failure Counts           │    │
│                        └──────────────────────────────┘    │
│                                    │                        │
│                                    ▼                        │
│                        ┌──────────────────────────────┐    │
│                        │   CloudWatch Dashboard       │    │
│                        │   - Real-time Visualization  │    │
│                        └──────────────────────────────┘    │
│                                    │                        │
│                                    ▼                        │
│                        ┌──────────────────────────────┐    │
│                        │   CloudWatch Alarms          │    │
│                        │   - Failure Alerts           │    │
│                        │   - Performance Alerts       │    │
│                        └──────────────────────────────┘    │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

## 使用方式

### 基本部署

```typescript
import { DeploymentMonitoringStack } from './stacks/deployment-monitoring-stack';

const deploymentMonitoring = new DeploymentMonitoringStack(app, 'DeploymentMonitoring', {
    projectName: 'genai-demo',
    environment: 'production',
});
```

### 使用現有告警主題

```typescript
import { DeploymentMonitoringStack } from './stacks/deployment-monitoring-stack';

const deploymentMonitoring = new DeploymentMonitoringStack(app, 'DeploymentMonitoring', {
    projectName: 'genai-demo',
    environment: 'production',
    alertingTopic: existingAlertingStack.alertTopic,
});
```

### 多區域配置

```typescript
const deploymentMonitoring = new DeploymentMonitoringStack(app, 'DeploymentMonitoring', {
    projectName: 'genai-demo',
    environment: 'production',
    multiRegionConfig: {
        enabled: true,
        regions: ['us-east-1', 'us-west-2', 'eu-west-1'],
        primaryRegion: 'us-east-1',
    },
});
```

## 指標參考

### 自訂指標命名空間

所有指標發佈到命名空間：`{PROJECT_NAME}/Deployment`

### 指標維度

- **Environment**：部署環境（development、staging、production）
- **Service**：AWS 服務（CodePipeline、CodeDeploy）

### 指標詳情

| 指標名稱 | 單位 | 說明 | 典型值 |
|-------------|------|-------------|---------------|
| PipelineSuccessRate | Percent | 成功的管線執行百分比 | > 95% |
| PipelineExecutionTime | Seconds | 平均管線執行時間 | 300-1800s |
| PipelineFailures | Count | 失敗的管線執行次數 | 0 |
| DeploymentSuccessRate | Percent | 成功的部署百分比 | > 95% |
| DeploymentTime | Seconds | 平均部署時間 | 180-1200s |
| DeploymentFailures | Count | 失敗的部署次數 | 0 |

## 警報配置

### Pipeline Failure Alarm

- **閾值**：≥ 1 次失敗
- **評估期間**：5 分鐘
- **動作**：發送 SNS 通知

### Deployment Failure Alarm

- **閾值**：≥ 1 次失敗
- **評估期間**：5 分鐘
- **動作**：發送 SNS 通知

### Low Success Rate Alarm

- **閾值**：< 80% 成功率
- **評估期間**：15 分鐘（2 個資料點）
- **動作**：發送 SNS 通知

### Long Deployment Time Alarm

- **閾值**：> 1800 秒（30 分鐘）
- **評估期間**：5 分鐘
- **動作**：發送 SNS 通知

### Long Pipeline Execution Alarm

- **閾值**：> 3600 秒（60 分鐘）
- **評估期間**：5 分鐘
- **動作**：發送 SNS 通知

## 儀表板存取

部署後，透過以下方式存取儀表板：

```bash
# 從堆疊輸出取得儀表板 URL
aws cloudformation describe-stacks \
    --stack-name DeploymentMonitoring \
    --query 'Stacks[0].Outputs[?OutputKey==`DeploymentDashboardUrl`].OutputValue' \
    --output text
```

或導航到：
```
https://{region}.console.aws.amazon.com/cloudwatch/home?region={region}#dashboards:name={project}-{environment}-deployment-monitoring
```

## 告警通知

### 訂閱告警

```bash
# 訂閱電子郵件以接收部署告警
aws sns subscribe \
    --topic-arn $(aws cloudformation describe-stacks \
        --stack-name DeploymentMonitoring \
        --query 'Stacks[0].Outputs[?OutputKey==`DeploymentAlertTopicArn`].OutputValue' \
        --output text) \
    --protocol email \
    --notification-endpoint your-email@example.com
```

### 告警訊息格式

#### Pipeline State Change Alert

```
Deployment Alert: CodePipeline State Change

Pipeline: genai-demo-production-multi-region-pipeline
State: FAILED
Execution ID: abc123-def456-ghi789
Time: 2025-01-22T10:30:00Z

⚠️ FAILURE DETECTED
```

#### Deployment State Change Alert

```
Deployment Alert: CodeDeploy State Change

Application: genai-demo-production-app
Deployment Group: production-us-east-1
Deployment ID: d-ABCDEF123
State: FAILURE
Region: us-east-1
Time: 2025-01-22T10:30:00Z

⚠️ FAILURE DETECTED
```

## 監控最佳實踐

### 1. 設定電子郵件通知

訂閱關鍵團隊成員到部署告警主題：

```bash
aws sns subscribe \
    --topic-arn <deployment-alert-topic-arn> \
    --protocol email \
    --notification-endpoint ops-team@example.com
```

### 2. 定期檢視儀表板

- 每日檢查成功率
- 監控部署時間趨勢
- 立即調查任何失敗

### 3. 調整警報閾值

根據部署模式調整閾值：

```typescript
// 範例：自訂長部署時間閾值
const customAlarm = new cloudwatch.Alarm(this, 'CustomDeploymentTimeAlarm', {
    metric: deploymentTimeMetric,
    threshold: 2400, // 40 分鐘而非預設的 30 分鐘
    evaluationPeriods: 2,
});
```

### 4. 與事件管理整合

將 SNS 通知轉發到事件管理工具：

```bash
# 範例：訂閱 PagerDuty 端點
aws sns subscribe \
    --topic-arn <deployment-alert-topic-arn> \
    --protocol https \
    --notification-endpoint https://events.pagerduty.com/integration/<key>/enqueue
```

## 疑難排解

### 沒有指標出現

1. **檢查 Lambda 執行**：
   ```bash
   aws logs tail /aws/lambda/<metrics-function-name> --follow
   ```

2. **驗證 IAM 權限**：
   - 確保 Lambda 具有 CodePipeline 和 CodeDeploy 讀取權限
   - 確保 Lambda 具有 CloudWatch PutMetricData 權限

3. **檢查 EventBridge 規則**：
   ```bash
   aws events list-rules --name-prefix <project-name>
   ```

### 警報未觸發

1. **驗證警報狀態**：
   ```bash
   aws cloudwatch describe-alarms --alarm-names <alarm-name>
   ```

2. **檢查指標資料**：
   ```bash
   aws cloudwatch get-metric-statistics \
       --namespace <project-name>/Deployment \
       --metric-name DeploymentFailures \
       --start-time <start> \
       --end-time <end> \
       --period 300 \
       --statistics Sum
   ```

3. **驗證 SNS 訂閱**：
   ```bash
   aws sns list-subscriptions-by-topic --topic-arn <topic-arn>
   ```

### 儀表板無法載入

1. **檢查儀表板是否存在**：
   ```bash
   aws cloudwatch list-dashboards
   ```

2. **驗證儀表板內容**：
   ```bash
   aws cloudwatch get-dashboard --dashboard-name <dashboard-name>
   ```

## 與現有堆疊整合

### 與 Observability Stack 整合

```typescript
// 在主堆疊檔案中
const observability = new ObservabilityStack(app, 'Observability', {
    vpc,
    kmsKey,
    environment: 'production',
});

const deploymentMonitoring = new DeploymentMonitoringStack(app, 'DeploymentMonitoring', {
    projectName: 'genai-demo',
    environment: 'production',
    alertingTopic: observability.alertTopic, // 重用現有告警主題
});
```

### 與 Alerting Stack 整合

```typescript
const alerting = new AlertingStack(app, 'Alerting', {
    environment: 'production',
    applicationName: 'genai-demo',
});

const deploymentMonitoring = new DeploymentMonitoringStack(app, 'DeploymentMonitoring', {
    projectName: 'genai-demo',
    environment: 'production',
    alertingTopic: alerting.alertTopic,
});
```

## 成本考量

### 預估月費用

- **Lambda 執行**：~$0.20（288 次執行/天 × 30 天）
- **CloudWatch 指標**：~$3.00（6 個自訂指標）
- **CloudWatch 警報**：~$5.00（5 個警報）
- **CloudWatch 儀表板**：~$3.00（1 個儀表板）
- **SNS 通知**：~$0.50（假設 100 次通知/月）

**總預估成本**：每個環境約 $12/月

### 成本優化技巧

1. **調整收集頻率**：如果可以接受較少的粒度，從 5 分鐘改為 10 分鐘
2. **減少指標保留時間**：對非關鍵指標使用較短的保留期
3. **合併警報**：在可能的情況下合併相關警報

## 測試

執行測試套件：

```bash
cd infrastructure
npm test -- deployment-monitoring-stack.test.ts
```

## 相關文件

- [AWS Code Services Deployment Guide](./AWS_CODE_SERVICES_DEPLOYMENT.md)
- [Observability Stack Documentation](./docs/observability-stack.md)
- [Multi-Region Active-Active Spec](../.kiro/specs/multi-region-active-active/)

## 支援

如有問題或疑問：
1. 檢查 CloudWatch Logs 以取得 Lambda 函式錯誤
2. 檢視 EventBridge 規則配置
3. 驗證所有組件的 IAM 權限
4. 檢查 SNS 主題訂閱

## 未來增強功能

計劃的改進：
- 與 AWS X-Ray 整合進行詳細的追蹤分析
- 支援自訂部署指標
- 增強多區域關聯和分析
- 與 AWS Cost Explorer 整合進行部署成本追蹤
- 基於部署失敗的自動修復動作
