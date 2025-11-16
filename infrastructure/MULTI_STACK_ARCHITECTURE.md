# Multi-Stack 架構實作

## 概述

本文件說明 GenAI Demo 基礎設施的 multi-stack 架構實作，從單一整體 stack 重構為模組化、可維護的架構。

## 架構設計

### Stack 結構

基礎設施現在組織成四個獨立的 stacks：

1. **Main Stack**（`genai-demo-development-primary`）
   - 協調所有其他 stacks
   - 管理 cross-stack 相依性
   - 提供彙總的輸出和參考

2. **Network Stack**（`genai-demo-development-primary/NetworkStack`）
   - 具有公有、私有和資料庫 subnets 的 VPC
   - 所有服務的 security groups（ALB、EKS、RDS、MSK）
   - VPC Flow Logs 和網路監控
   - EKS subnet 標記用於服務探索

3. **Certificate Stack**（`genai-demo-development-primary/CertificateStack`）
   - 具有 DNS 驗證的 ACM certificates
   - Route 53 hosted zone 查找
   - Certificate 監控和告警
   - 萬用字元 certificate 支援

4. **Core Infrastructure Stack**（`genai-demo-development-primary/CoreInfrastructureStack`）
   - 具有 SSL 終止的 Application Load Balancer
   - 用於網域路由的 DNS A records
   - Target groups 和 health checks
   - Kubernetes Ingress 配置助手

### Multi-Stack 架構的優勢

#### 1. **關注點分離**

- 每個 stack 具有單一、明確定義的職責
- 網路基礎設施與應用程式基礎設施隔離
- Certificate 管理與 load balancing 分離

#### 2. **獨立部署**

- 當變更不影響相依性時，stacks 可以獨立部署
- 獨立變更的部署時間更快
- 基礎設施變更的爆炸半徑減少

#### 3. **改善的可維護性**

- 較小、專注的程式碼庫更易於理解和修改
- 不同基礎設施元件之間的界限清晰
- 更容易獨立測試個別元件

#### 4. **增強的可重用性**

- 個別 stacks 可以在不同環境中重複使用
- 可以提取和共享常見模式
- 更容易建立環境特定的變體

#### 5. **更好的資源組織**

- 資源按功能邏輯分組
- 更容易追蹤每個元件的成本和資源使用情況
- 簡化疑難排解和除錯

## 實作細節

### 檔案結構

```
infrastructure/
├── lib/
│   ├── stacks/
│   │   ├── network-stack.ts           # VPC 和網路資源
│   │   ├── certificate-stack.ts       # ACM certificates 和 DNS
│   │   ├── core-infrastructure-stack.ts # ALB 和共享資源
│   │   └── index.ts                   # Stack 匯出
│   ├── genai-demo-infrastructure-stack.ts # 主要協調 stack
│   └── infrastructure-stack.ts        # 舊版 stack（已棄用）
├── test/
│   ├── network-stack.test.ts          # Network stack 測試
│   ├── certificate-stack.test.ts      # Certificate stack 測試
│   ├── core-infrastructure-stack.test.ts # Core infrastructure 測試
│   └── infrastructure.test.ts         # Main stack 協調測試
└── bin/
    └── infrastructure.ts               # CDK app 進入點
```

### Cross-Stack 相依性

Stacks 具有以下相依性關係：

```
Main Stack
├── Network Stack（獨立）
├── Certificate Stack（獨立）
└── Core Infrastructure Stack
    ├── depends on → Network Stack（VPC、Security Groups）
    └── depends on → Certificate Stack（Certificates、Hosted Zone）
```

### Stack 命名慣例

- **基於環境的命名**：`{projectName}-{environment}-{region}`
- **巢狀 stack 命名**：`{mainStack}/{StackType}Stack`
- **資源命名**：`{projectName}-{environment}-{resourceType}`

### 標記策略

所有 stacks 實作一致的標記：

```typescript
const commonTags = {
  Project: projectName,
  Environment: environment,
  ManagedBy: 'AWS-CDK',
  Component: stackType, // 'Network', 'Certificate', 'CoreInfrastructure'
  StackType: stackType
};
```

### Cross-Stack 參考

Main stack 提供協助方法來存取巢狀 stacks 的資源：

```typescript
// 從 Network Stack 存取 VPC
const vpc = mainStack.getVpc();

// 存取 security groups
const securityGroups = mainStack.getSecurityGroups();

// 存取 certificates
const certificates = mainStack.getCertificates();

// 存取 load balancer
const loadBalancer = mainStack.getLoadBalancer();
```

## 部署

### CDK 指令

```bash
# 列出所有 stacks
npx cdk list

# 部署所有 stacks
npx cdk deploy --all

# 部署特定 stack
npx cdk deploy genai-demo-development-primary/NetworkStack

# 合成範本
npx cdk synth

# 檢視差異
npx cdk diff
```

### Stack 部署順序

CDK 根據相依性自動處理部署順序：

1. Network Stack（無相依性）
2. Certificate Stack（無相依性）
3. Core Infrastructure Stack（相依於 Network 和 Certificate）
4. Main Stack（協調）

## 測試策略

### 測試組織

每個 stack 都有自己的測試檔案，專注於：

- **Network Stack Tests**：VPC 配置、security groups、subnet 標記
- **Certificate Stack Tests**：ACM certificates、DNS 驗證、監控
- **Core Infrastructure Tests**：ALB 配置、DNS records、SSL 終止
- **Main Stack Tests**：Cross-stack 整合、相依性管理

### 測試執行

```bash
# 執行所有測試
npm test

# 執行特定 stack 測試
npm test -- --testPathPattern="network-stack.test.ts"
npm test -- --testPathPattern="certificate-stack.test.ts"
npm test -- --testPathPattern="core-infrastructure-stack.test.ts"
npm test -- --testPathPattern="infrastructure.test.ts"
```

## 從整體 Stack 的遷移

### 變更內容

1. **單一 Stack → 多個 Stacks**：將整體 `infrastructure-stack.ts` 重構為模組化 stacks
2. **改善的組織**：資源按邏輯功能分組而非部署單元
3. **增強的測試**：具有專注測試場景的個別 stack 測試
4. **更好的相依性管理**：使用 CDK 相依性追蹤的明確 cross-stack 相依性

### 向後相容性

- 所有現有輸出都保留在 main stack 中
- 資源命名慣例保持一致
- 環境配置繼續如前運作
- Cross-stack 參考透明處理

## 未來增強

### 規劃的改進

1. **環境特定 Stacks**：每個環境不同的 stack 配置
2. **區域 Stacks**：多區域部署支援
3. **服務特定 Stacks**：EKS、RDS 和 MSK 作為獨立 stacks
4. **共享資源 Stacks**：跨應用程式共享的通用資源

### 可擴展性

模組化架構使以下操作變得容易：

- 將新的基礎設施元件新增為獨立 stacks
- 實作環境特定的變體
- 建立可重用的基礎設施模式
- 支援多區域部署

## 結論

Multi-stack 架構為可擴展、可維護的基礎設施管理提供堅實的基礎。關注點分離、改善的可測試性和增強的部署靈活性使此架構非常適合生產環境和未來成長。
