# Deployment Monitoring - 快速開始指南

## 5 分鐘設定

### 步驟 1：部署 Stack（2 分鐘）

```bash
cd infrastructure
cdk deploy DeploymentMonitoring --require-approval never
```

### 步驟 2：訂閱告警（1 分鐘）

```bash
# 取得 topic ARN
TOPIC_ARN=$(aws cloudformation describe-stacks \
    --stack-name DeploymentMonitoring \
    --query 'Stacks[0].Outputs[?OutputKey==`DeploymentAlertTopicArn`].OutputValue' \
    --output text)

# 訂閱您的電子郵件
aws sns subscribe \
    --topic-arn $TOPIC_ARN \
    --protocol email \
    --notification-endpoint your-email@example.com

# 在您的電子郵件中確認訂閱
```

### 步驟 3：存取 Dashboard（1 分鐘）

```bash
# 取得 dashboard URL
aws cloudformation describe-stacks \
    --stack-name DeploymentMonitoring \
    --query 'Stacks[0].Outputs[?OutputKey==`DeploymentDashboardUrl`].OutputValue' \
    --output text

# 或直接開啟
open "https://$(aws configure get region).console.aws.amazon.com/cloudwatch/home?region=$(aws configure get region)#dashboards:name=genai-demo-production-deployment-monitoring"
```

### 步驟 4：驗證 Metrics（1 分鐘）

```bash
# 等待 5 分鐘進行首次指標收集，然後檢查
aws cloudwatch get-metric-statistics \
    --namespace genai-demo/Deployment \
    --metric-name PipelineSuccessRate \
    --start-time $(date -u -d '10 minutes ago' +%Y-%m-%dT%H:%M:%S) \
    --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
    --period 300 \
    --statistics Average
```

## 您將獲得什麼

### 📊 即時 Dashboard

- Pipeline 成功率
- 部署成功率
- 執行時間趨勢
- 失敗追蹤

### 🔔 即時告警

您將收到以下電子郵件通知：
- 任何 pipeline 失敗
- 任何部署失敗
- 成功率低於 80%
- 部署時間超過 30 分鐘
- Pipeline 執行時間超過 60 分鐘

### 📈 歷史資料

- 追蹤隨時間變化的部署趨勢
- 識別失敗模式
- 監控效能改進
- 分析部署頻率

## 常見使用案例

### 監控 Production 部署

```bash
# 使用 production 配置進行部署
cdk deploy DeploymentMonitoring \
    -c environment=production \
    -c projectName=genai-demo
```

### Multi-Region 監控

```typescript
new DeploymentMonitoringStack(app, 'DeploymentMonitoring', {
    projectName: 'genai-demo',
    environment: 'production',
    multiRegionConfig: {
        enabled: true,
        regions: ['us-east-1', 'us-west-2', 'eu-west-1'],
        primaryRegion: 'us-east-1',
    },
});
```

### 與現有告警整合

```typescript
new DeploymentMonitoringStack(app, 'DeploymentMonitoring', {
    projectName: 'genai-demo',
    environment: 'production',
    alertingTopic: existingAlertingStack.alertTopic,
});
```

## 疑難排解

### 未顯示 Metrics？

```bash
# 檢查 Lambda logs
aws logs tail /aws/lambda/DeploymentMonitoring-DeploymentMetricsFunction --follow

# 驗證 Lambda 正在執行
aws lambda list-functions --query 'Functions[?contains(FunctionName, `DeploymentMetrics`)]'
```

### 未收到告警？

```bash
# 檢查 SNS subscriptions
aws sns list-subscriptions-by-topic --topic-arn $TOPIC_ARN

# 驗證訂閱已確認
aws sns get-subscription-attributes --subscription-arn <subscription-arn>
```

### Dashboard 未載入？

```bash
# 驗證 dashboard 存在
aws cloudwatch list-dashboards | grep deployment-monitoring

# 檢查 dashboard 內容
aws cloudwatch get-dashboard --dashboard-name genai-demo-production-deployment-monitoring
```

## 後續步驟

1. **自訂閾值**：根據您的部署模式調整告警閾值
2. **新增更多訂閱者**：訂閱更多團隊成員或工具
3. **與工具整合**：連接到 PagerDuty、Slack 或其他事件管理工具
4. **定期檢視**：每週檢查 dashboard 以識別改進機會

## 成本

**每個環境每月約 $12**

- 對於提供的可見性而言非常具有成本效益
- 隨部署頻率擴展
- 查看 dashboard 或接收告警無需額外費用

## 支援

詳細文件請參閱：
- [完整文件](./DEPLOYMENT_MONITORING.md)
- [AWS Code Services 指南](./AWS_CODE_SERVICES_DEPLOYMENT.md)
- [整合範例](./examples/deployment-monitoring-integration.ts)

---

**準備好部署了嗎？** 現在執行 `cdk deploy DeploymentMonitoring`！
