# GenAI Demo Infrastructure

本專案包含 GenAI Demo 應用程式的**整合 AWS CDK 基礎設施**程式碼，具備統一的部署架構和完整測試。

## ✅ 專案狀態

- **CDK v2 合規**: 使用 `aws-cdk-lib ^2.208.0`
- **完整測試**: 11 個測試套件中通過 103 個測試
- **生產就緒**: 具備安全合規和監控
- **整合部署**: 單一指令部署所有基礎設施

## 🏗️ 架構概覽

基礎設施組織為 **18 個協調的 stacks**，具備統一部署：

### Foundation Layer
- **NetworkStack**: VPC、子網路、安全群組和網路元件
- **SecurityStack**: KMS 金鑰、IAM 角色和安全相關資源
- **IAMStack**: 細粒度存取控制、基於資源的政策
- **CertificateStack**: 安全通訊的 SSL/TLS 憑證

### Data Layer
- **RdsStack**: Aurora PostgreSQL cluster，支援 global database
- **ElastiCacheStack**: 用於分散式快取和鎖定的 Redis cluster
- **MSKStack**: 用於事件串流和資料流追蹤的 Kafka cluster

### Compute Layer
- **EKSStack**: Kubernetes cluster，具備自動擴展和安全性
- **EKSIRSAStack**: IAM Roles for Service Accounts 設定

### Security & Identity Layer
- **SSOStack**: AWS SSO 整合，具備權限集
- **SecurityStack**: 增強的安全監控和合規

### Observability Layer
- **AlertingStack**: SNS topics 和通知基礎設施
- **ObservabilityStack**: CloudWatch logs、dashboards、X-Ray tracing 和監控

### Analytics Layer *（選用）*
- **DataCatalogStack**: AWS Glue Data Catalog，具備自動化 schema 探索
- **AnalyticsStack**: Data lake、Kinesis、Glue 和分析管道

### Management Layer
- **CoreInfrastructureStack**: Application Load Balancer 和核心運算資源
- **CostOptimizationStack**: 成本監控和最佳化自動化

### Resilience Layer *（生產環境）*
- **DisasterRecoveryStack**: 多區域災難復原自動化
- **MultiRegionStack**: 跨區域複寫和容錯移轉

## Prerequisites

- Node.js 18.x 或更新版本
- AWS CLI，已設定適當的憑證
- 全域安裝 AWS CDK CLI: `npm install -g aws-cdk`
- TypeScript 5.6+（包含在相依套件中）

## 🚀 Quick Start

### Unified Deployment（建議）

新的統一部署腳本為所有基礎設施部署場景提供單一進入點：

```bash
# 部署完整開發環境
./deploy-unified.sh full -e development

# 僅部署基礎元件（network、security、IAM）
./deploy-unified.sh foundation -e staging

# 啟用 analytics 進行部署
./deploy-unified.sh full --enable-analytics -a ops@company.com

# 部署生產環境，具備 multi-region
./deploy-unified.sh full -e production --enable-multi-region

# 檢查部署狀態
./deploy-unified.sh --status

# 銷毀開發環境
./deploy-unified.sh --destroy -e development
```

### NPM Scripts（替代方案）

```bash
# 快速部署指令
npm run deploy:dev          # 開發環境
npm run deploy:staging      # Staging，具備 analytics
npm run deploy:prod         # 生產環境，具備 multi-region

# 元件特定部署
npm run deploy:foundation   # Network、security、IAM
npm run deploy:data         # RDS、ElastiCache、MSK
npm run deploy:compute      # EKS cluster
npm run deploy:security     # IAM、SSO、IRSA
npm run deploy:observability # Monitoring、alerting

# 狀態和清理
npm run status              # 檢查部署狀態
npm run destroy:dev         # 銷毀開發環境
```

## 📁 Project Structure

```
infrastructure/
├── bin/
│   └── infrastructure.ts          # 主要 CDK app 進入點（NEW）
├── src/
│   ├── stacks/                    # CDK stack 定義（18 stacks）
│   │   ├── iam-stack.ts          # 細粒度存取控制
│   │   ├── eks-irsa-stack.ts     # IRSA 設定
│   │   ├── sso-stack.ts          # AWS SSO 整合
│   │   ├── msk-stack.ts          # Kafka messaging
│   │   ├── data-catalog-stack.ts # 資料治理
│   │   └── ...                   # 其他 stacks
│   ├── constructs/                # 可重用的 CDK constructs
│   ├── config/                    # 環境設定
│   └── utils/                     # 工具函數
├── test/
│   ├── unit/                      # 單元測試
│   ├── integration/               # 整合測試
│   └── ...                       # 測試套件
├── docs/                          # 文件
├── deploy-unified.sh              # NEW: 統一部署腳本
├── deploy-iam-security.sh         # IAM 安全部署
├── deploy-consolidated.sh         # 舊版部署腳本
└── package.json                   # 相依套件和腳本
```

## Getting Started

1. **安裝相依套件**:

   ```bash
   npm install
   ```

2. **建置專案**:

   ```bash
   npm run build
   ```

3. **執行測試**:

   ```bash
   npm test
   # 所有 103 個測試應在約 16 秒內通過
   ```

4. **合成 CloudFormation templates**:

   ```bash
   npm run synth
   # 或不顯示 CDK Nag 警告:
   npx cdk synth --context enableCdkNag=false
   ```

5. **部署到 AWS**:

   ```bash
   # 統一部署（建議）
   ./deploy-consolidated.sh

   # 或使用 npm scripts
   npm run deploy:consolidated

   # 部署到特定環境
   npm run deploy:dev      # 開發環境，具備 analytics
   npm run deploy:staging  # Staging，具備 CDK Nag
   npm run deploy:prod     # 生產環境部署
   ```

## 🧪 Testing

### Quick Test Commands

```bash
# 執行所有測試（103 tests）
npm test

# 執行特定測試類別
npm run test:unit          # 單元測試（26 tests）
npm run test:integration   # 整合測試（8 tests）
npm run test:consolidated  # 主要測試套件（18 tests）
npm run test:compliance    # CDK Nag 合規（4 tests）
npm run test:quick         # 開發用的快速子集
```

### Test Results

```
Test Suites: 11 passed, 11 total
Tests: 103 passed, 103 total
Time: 15.828 s
Coverage: 100% on core infrastructure
```

## Project Structure

```
infrastructure/
├── src/                          # 原始碼
│   ├── stacks/                   # CDK Stack 定義
│   ├── constructs/               # 自訂 CDK Constructs
│   ├── config/                   # 設定檔
│   └── utils/                    # 工具函數
├── test/                         # 測試檔案
│   ├── unit/                     # 單元測試
│   ├── integration/              # 整合測試
│   └── compliance/               # CDK Nag 合規測試
├── bin/                          # 進入點
├── docs/                         # 文件
└── k8s/                          # Kubernetes manifests
```

## Available Scripts

- `npm run build` - 編譯 TypeScript 為 JavaScript
- `npm run watch` - 監看變更並編譯
- `npm test` - 執行所有測試
- `npm run test:unit` - 僅執行單元測試
- `npm run test:integration` - 僅執行整合測試
- `npm run test:compliance` - 執行 CDK Nag 合規測試
- `npm run synth` - 合成 CloudFormation templates
- `npm run deploy` - 部署 stacks 到 AWS
- `npm run destroy` - 從 AWS 銷毀 stacks
- `npm run lint` - 執行 ESLint
- `npm run lint:fix` - 修復 ESLint 問題

## Environment Configuration

基礎設施支援多個環境（development、staging、production）。
使用 CDK context 設定環境：

```bash
# 部署到 development（預設）
cdk deploy

# 部署到 staging
cdk deploy -c environment=staging

# 部署到 production
cdk deploy -c environment=production
```

## Testing

專案包含完整的測試：

- **Unit Tests**: 測試個別 stack 元件
- **Integration Tests**: 測試 stack 互動
- **Compliance Tests**: CDK Nag 安全和最佳實踐檢查

執行所有測試：

```bash
npm test
```

## Security

基礎設施遵循 AWS 安全最佳實踐：

- 所有資源使用 KMS 加密
- 安全群組遵循最小權限原則
- IAM 角色具有最小所需權限
- CDK Nag 檢查確保符合 AWS Well-Architected Framework

## Monitoring

ObservabilityStack 提供：

- 應用程式日誌的 CloudWatch log groups
- 監控用的 CloudWatch dashboards
- 具加密的結構化日誌記錄

## Contributing

1. 建立功能分支
2. 進行變更
3. 執行測試: `npm test`
4. 執行 linting: `npm run lint`
5. 合成 templates: `npm run synth`
6. 建立 pull request

## Troubleshooting

如果遇到問題：

1. **清理並重建**:

   ```bash
   npm run clean
   npm install
   npm run build
   ```

2. **檢查 CDK 版本相容性**:

   ```bash
   cdk --version
   ```

3. **驗證 AWS 憑證**:

   ```bash
   aws sts get-caller-identity
   ```

## License

本專案採用 MIT License。

## 🚀 Deployment Options

### Environment Configuration

基礎設施支援具有不同設定的多個環境：

| 環境 | Analytics | CDK Nag | 使用案例 |
|------|-----------|---------|----------|
| Development | 選用 | 停用 | 日常開發 |
| Staging | 啟用 | 啟用 | 預生產測試 |
| Production | 啟用 | 啟用 | 生產工作負載 |

### Deployment Commands

```bash
# 快速開發部署
./deploy-consolidated.sh development us-east-1 false false

# 完整 staging 部署，具備合規
./deploy-consolidated.sh staging us-east-1 true true

# 生產環境部署
./deploy-consolidated.sh production us-east-1 true true
```

## 🔒 Security & Compliance

### CDK Nag Integration

專案包含 CDK Nag 用於安全合規，具備適當的抑制：

- **AwsSolutions-VPC7**: VPC Flow Logs（開發環境選用）
- **AwsSolutions-EC23**: ALB 網際網路存取（Web 應用程式所需）
- **AwsSolutions-IAM4**: AWS 託管政策（CloudWatch agent）
- **AwsSolutions-IAM5**: KMS 萬用字元權限（加密操作）

### Security Features

- ✅ 所有敏感資料的 KMS 加密
- ✅ 具最小權限原則的 IAM 角色
- ✅ 具最小所需存取的安全群組
- ✅ 傳輸中資料的 SSL/TLS 強制執行
- ✅ 稽核軌跡的 CloudTrail 日誌記錄

## 📊 Monitoring & Observability

### Built-in Monitoring

- **CloudWatch Dashboards**: 應用程式和基礎設施指標
- **CloudWatch Logs**: 集中式日誌記錄，具備保留政策
- **SNS Alerting**: 多層級告警（Critical、Warning、Info）
- **X-Ray Tracing**: 效能分析的分散式追蹤

### Metrics & Alarms

- 應用程式效能指標
- 基礎設施健康監控
- 成本最佳化告警
- 安全事件通知

## 🛠️ Development Workflow

### Daily Development

```bash
# 1. 對基礎設施程式碼進行變更
# 2. 執行測試以驗證變更
npm run test:quick

# 3. 合成以檢查 CloudFormation
npm run synth

# 4. 部署到開發環境
npm run deploy:dev
```

### Pre-commit Checklist

- [ ] 所有測試通過（`npm test`）
- [ ] CDK 合成成功（`npm run synth`）
- [ ] 程式碼遵循 TypeScript 標準
- [ ] 如需要，文件已更新

## 📚 Documentation

- [`TESTING_GUIDE.md`](./TESTING_GUIDE.md) - 完整測試文件
- [`CONSOLIDATED_DEPLOYMENT.md`](./CONSOLIDATED_DEPLOYMENT.md) - 部署指南
- [`CDK_COMPLETION_SUMMARY.md`](../reports-summaries/task-execution/CDK_COMPLETION_SUMMARY.md) - 專案完成狀態
- [`MIGRATION_GUIDE.md`](./MIGRATION_GUIDE.md) - 從舊結構遷移

## 🤝 Contributing

1. Fork 倉儲
2. 建立功能分支
3. 進行變更
4. 執行測試: `npm test`
5. 提交 pull request

## 📞 Support

如有問題或疑問：

1. 檢查 `docs/` 目錄中的文件
2. 檢視 `test/` 目錄中的測試範例
3. 執行 `npm run synth` 驗證您的變更
4. 使用 `npm run test:watch` 進行互動式開發

## 🏷️ Version Information

- **CDK Version**: 2.208.0+
- **Node.js**: 18.x+
- **TypeScript**: 5.6+
- **Test Framework**: Jest
- **Test Coverage**: 11 個套件中通過 103 個測試
