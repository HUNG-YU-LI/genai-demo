# CDK Infrastructure 測試指南

## 概述

本文件提供整合 CDK 基礎設施測試的完整指引。

## 測試結構

### 測試類別

1. **Unit Tests** (`test/unit/`) - 個別元件的快速、獨立測試（26 個測試）
2. **Integration Tests** (`test/integration/`) - Stack 互動和完整部署的測試（8 個測試）
3. **Consolidated Tests** (`test/consolidated-stack.test.ts`) - 所有 stacks 的主要測試套件（18 個測試）
4. **Compliance Tests** (`test/cdk-nag-suppressions.test.ts`) - CDK Nag 合規性驗證（4 個測試）
5. **Stack Tests** (`test/*-stack.test.ts`) - 個別 stack 驗證測試（47 個測試）

### 測試檔案

```
test/
├── unit/                           # Unit tests
│   ├── network-stack.test.ts      # Network stack unit tests
│   ├── network-stack.unit.test.ts # Additional network tests
│   └── security-stack.test.ts     # Security stack unit tests
├── integration/                    # Integration tests
│   └── deployment.test.ts         # Full deployment tests
├── consolidated-stack.test.ts      # Main consolidated test suite
├── cdk-nag-suppressions.test.ts   # CDK Nag compliance tests
├── certificate-stack.test.ts      # Certificate stack tests
├── msk-stack.test.ts              # MSK stack tests
├── network-stack.test.ts          # Network stack integration tests
├── rds-stack.test.ts              # RDS stack tests
├── security-stack.test.ts         # Security stack integration tests
└── setup.ts                       # Test configuration
```

## 執行測試

### 所有測試

```bash
npm test
```

### 特定測試類別

```bash
# Unit tests only
npm test -- --testPathPatterns="unit/"

# Integration tests only
npm test -- --testPathPatterns="integration/"

# Consolidated tests only
npm test -- --testPathPatterns="consolidated-stack.test.ts"

# CDK Nag compliance tests
npm test -- --testPathPatterns="cdk-nag-suppressions.test.ts"
```

### 個別測試檔案

```bash
# Network stack tests
npm test -- --testPathPatterns="network-stack.test.ts"

# Security stack tests
npm test -- --testPathPatterns="security-stack.test.ts"

# Full deployment tests
npm test -- --testPathPatterns="deployment.test.ts"
```

## 測試配置

### Jest 配置

- **Timeout**：CDK synthesis 操作 60 秒
- **Max Workers**：1（循序執行以避免 CDK 衝突）
- **Setup**：自動環境配置和警告抑制

### 環境變數

測試自動設定：

- `CDK_DEFAULT_REGION=us-east-1`
- `CDK_DEFAULT_ACCOUNT=123456789012`
- `JSII_DEPRECATED=quiet`

## CDK Synthesis 測試

### 不使用 CDK Nag（開發環境建議）

```bash
npx cdk synth --context enableCdkNag=false
```

### 使用 CDK Nag（安全合規性）

```bash
npx cdk synth
```

### 特定 Stack

```bash
npx cdk synth genai-demo-development-NetworkStack --context enableCdkNag=false
```

## 測試涵蓋範圍

### 目前測試涵蓋範圍

- **NetworkStack**：✅ VPC、Subnets、Security Groups、Route Tables、Outputs（15 個測試）
- **SecurityStack**：✅ KMS Key、IAM Role、Policies、Outputs（8 個測試）
- **CoreInfrastructureStack**：✅ ALB、Target Groups、Listeners（3 個測試）
- **AlertingStack**：✅ SNS Topics、Subscriptions（2 個測試）
- **ObservabilityStack**：✅ CloudWatch Logs、Dashboard（2 個測試）
- **AnalyticsStack**：✅ S3、Kinesis Firehose、Lambda、Glue（3 個測試）
- **Integration Tests**：✅ 完整部署、Cross-stack references（8 個測試）
- **Compliance Tests**：✅ CDK Nag suppressions、Security validation（4 個測試）

### Integration Tests

- **Full Deployment**：✅ 所有包含相依性的 stacks
- **Cross-Stack References**：✅ VPC、Security Groups、KMS Keys
- **Optional Components**：✅ Analytics stack 啟用/停用

## CDK Nag 合規性

### 已知抑制規則

以下 CDK Nag 規則已被抑制並有正當理由：

1. **AwsSolutions-VPC7**：VPC Flow Logs 在開發環境為選用
2. **AwsSolutions-EC23**：ALB 需要在埠 80/443 上的網際網路存取
3. **AwsSolutions-IAM4**：需要 CloudWatch 託管政策
4. **AwsSolutions-IAM5**：需要 KMS 萬用字元權限

### 執行合規性測試

```bash
npm test -- --testPathPatterns="cdk-nag-suppressions.test.ts"
```

## 疑難排解

### 常見問題

1. **Multiple Synthesis Error**
   - **原因**：CDK App 在同一測試中被多次 synthesized
   - **解決方案**：為每個測試建立新的 App 實例

2. **Resource Count Mismatch**
   - **原因**：CDK 自動建立額外的資源
   - **解決方案**：檢查實際範本並更新預期計數

3. **Template Property Mismatch**
   - **原因**：CDK 產生的屬性結構與預期不同
   - **解決方案**：使用 `Template.fromStack(stack).toJSON()` 進行檢查

### 測試除錯

```bash
# Run with verbose output
npm test -- --verbose

# Run specific test with debugging
npm test -- --testPathPatterns="network-stack.test.ts" --verbose

# Check CDK template output
npx cdk synth --context enableCdkNag=false > template.yaml
```

### 測試效能

- **Unit Tests**：每個 < 1 秒
- **Integration Tests**：每個 2-5 秒
- **Full Test Suite**：~16 秒（103 個測試）
- **Parallel Execution**：使用 maxWorkers=1 進行 CDK synthesis 最佳化

## 最佳實踐

### 撰寫測試

1. **使用描述性名稱**：測試名稱應清楚描述正在測試的內容
2. **測試單一項目**：每個測試應驗證單一面向
3. **使用適當的設定**：為每個測試套件建立新的 CDK App
4. **檢查資源**：驗證存在性和配置
5. **測試輸出**：確保 stack 輸出正確配置

### 測試組織

1. **分組相關測試**：使用 `describe` 區塊進行邏輯分組
2. **共享設定**：使用 `beforeEach` 進行共同測試設定
3. **清潔隔離**：避免測試相依性
4. **文件化期望**：註釋複雜的測試邏輯

### 效能最佳化

1. **最小化 Synthesis**：在測試套件中重複使用範本
2. **平行執行**：為獨立測試使用單獨的測試檔案
3. **Mock 外部相依性**：為外部服務使用 mocks
4. **選擇性測試**：開發期間僅執行相關測試

## 持續整合

### GitHub Actions 整合

```yaml
- name: Run CDK Tests
  run: |
    cd infrastructure
    npm ci
    npm test
    npm run synth -- --context enableCdkNag=false
```

### Pre-commit Hooks

```bash
# Run tests before commit
npm test -- --testPathPatterns="consolidated-stack.test.ts"
```

## 未來增強

### 規劃的測試新增

1. **Performance Tests**：資源建立時間基準測試
2. **Security Tests**：自動化安全掃描
3. **Cost Tests**：資源成本估算驗證
4. **Multi-Region Tests**：跨區域部署驗證

### 測試自動化

1. **Snapshot Testing**：範本變更偵測
2. **Property Testing**：隨機輸入驗證
3. **Load Testing**：大規模部署模擬
4. **Regression Testing**：自動化變更影響分析

## 資源

- AWS CDK Testing Guide
- CDK Assertions Library
- CDK Nag Documentation
- Jest Testing Framework
