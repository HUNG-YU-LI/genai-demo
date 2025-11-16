# AWS Code Services 多區域部署指南

## 概述

`deploy-unified.sh` 腳本已經增強，支援使用 AWS Code Services（CodePipeline、CodeBuild 和 CodeDeploy）進行多區域部署。這提供了自動化、可擴展且可靠的部署管線，並支援 canary 和 blue-green 部署策略。

## 新功能

### 🚀 AWS Code Services 整合

- **CodePipeline**：多區域部署編排
- **CodeBuild**：基礎設施和應用程式建置
- **CodeDeploy**：Blue-green 和 canary 部署策略
- **CloudWatch**：自動化監控和回滾觸發器

### 🌍 多區域部署

- 跨多個 AWS 區域的平行部署
- 跨區域同步和監控
- 自動容錯移轉和災難復原

### 📊 部署策略

- **Canary Deployment**：漸進式流量轉移（可配置百分比）
- **Blue-Green Deployment**：零停機時間部署
- **Automated Rollback**：基於 CloudWatch 警報

## 使用方式

### 基本多區域部署

```bash
# 啟用多區域和 CodePipeline 進行部署
./deploy-unified.sh full -e production --enable-multi-region --enable-code-pipeline

# 使用自訂 canary 百分比進行部署
./deploy-unified.sh full -e production --enable-multi-region --canary-percentage 20

# 使用 blue-green 策略進行部署
./deploy-unified.sh full -e production --enable-multi-region --blue-green
```

### 新的命令列選項

| 選項 | 說明 | 預設值 |
|--------|-------------|---------|
| `--enable-code-pipeline` | 啟用 AWS CodePipeline 多區域部署 | false |
| `--canary-percentage PCT` | Canary 部署百分比 | 10 |
| `--blue-green` | 啟用 blue-green 部署策略 | false |
| `--pipeline-status` | 顯示 CodePipeline 部署狀態 | - |

### 監控和狀態

```bash
# 檢查基礎設施部署狀態
./deploy-unified.sh --status -e production -r ap-east-2

# 檢查 CodePipeline 部署狀態
./deploy-unified.sh --pipeline-status -e production -r ap-east-2
```

## 架構

### 管線結構

```
Source (S3) → Build (CodeBuild) → Deploy (Multi-Region)
    ↓              ↓                    ↓
Artifacts    Infrastructure      Primary Region
Bucket       + Application       + Replication Regions
```

### 建立的組件

#### CodePipeline
- **管線名稱**：`{PROJECT_NAME}-{ENVIRONMENT}-multi-region-pipeline`
- **階段**：Source、Build、Deploy Infrastructure、Deploy Application
- **產物**：儲存在啟用版本控制的 S3 中

#### CodeBuild 專案
1. **Infrastructure Build**：`{PROJECT_NAME}-{ENVIRONMENT}-infrastructure-build`
   - CDK 合成和測試
   - CloudFormation 範本生成

2. **Application Build**：`{PROJECT_NAME}-{ENVIRONMENT}-application-build`
   - Spring Boot 應用程式編譯
   - Docker 映像建置並推送到 ECR

#### CodeDeploy 應用程式
- **應用程式名稱**：`{PROJECT_NAME}-{ENVIRONMENT}-app`
- **部署群組**：每個區域一個
- **部署配置**：
  - Canary：`{PROJECT_NAME}-{ENVIRONMENT}-canary-10-percent`
  - Blue-Green：`{PROJECT_NAME}-{ENVIRONMENT}-blue-green`

### 建立的 IAM 角色

腳本會自動建立以下 IAM 角色：

1. **CodePipelineServiceRole**：用於管線執行
2. **CodeBuildServiceRole**：用於建置專案執行
3. **CodeDeployServiceRole**：用於部署執行
4. **CloudFormationServiceRole**：用於基礎設施部署

## 部署流程

### 階段 1：基礎設施
- Network、Security、IAM、Certificates
- 先部署到主要區域，然後到複製區域

### 階段 2：資料層
- RDS Aurora Global Database
- ElastiCache、MSK 跨區域複製
- 按適當的依賴關係部署

### 階段 3：運算層
- 所有區域的 EKS 叢集
- 應用程式負載平衡器
- 自動擴展配置

### 階段 4：全域服務
- Route53 全域路由
- CloudFront CDN
- 跨區域同步

### 階段 5：Code Services 管線
- CodePipeline 建立
- CodeBuild 專案設定
- CodeDeploy 應用程式配置
- CloudWatch 警報用於自動回滾

## 監控和告警

### CloudWatch 警報

自動為每個區域建立的警報：

- **High Error Rate**：2 分鐘內超過 10 個錯誤
- **High Response Time**：平均超過 2 秒
- **Deployment Failures**：自動回滾觸發器

### 追蹤的指標

- 管線執行狀態
- 建置成功/失敗率
- 部署成功率
- 應用程式效能指標
- 跨區域同步健康狀況

## 回滾策略

### 自動回滾觸發器

1. **CloudWatch Alarms**：高錯誤率或回應時間
2. **Deployment Failures**：CodeDeploy 部署失敗
3. **Health Check Failures**：應用程式健康檢查失敗

### 手動回滾

```bash
# 透過 CodeDeploy 回滾
aws deploy stop-deployment --deployment-id <deployment-id> --auto-rollback-enabled

# 透過 Pipeline 回滾
aws codepipeline stop-pipeline-execution --pipeline-name <pipeline-name> --pipeline-execution-id <execution-id>
```

## 疑難排解

### 常見問題

1. **IAM 權限錯誤**
   - 確保 AWS 憑證具有足夠的權限
   - 檢查服務角色是否成功建立

2. **建置失敗**
   - 檢查 CloudWatch 中的 CodeBuild 日誌
   - 驗證原始碼在 S3 產物儲存桶中可用

3. **部署失敗**
   - 檢查 CodeDeploy 部署日誌
   - 驗證目標基礎設施是否健康

### 除錯指令

```bash
# 檢查管線執行詳情
aws codepipeline get-pipeline-execution --pipeline-name <pipeline-name> --pipeline-execution-id <execution-id>

# 檢查建置日誌
aws logs get-log-events --log-group-name /aws/codebuild/<project-name> --log-stream-name <stream-name>

# 檢查部署狀態
aws deploy get-deployment --deployment-id <deployment-id>
```

## 最佳實踐

### 安全性
- 使用最小權限 IAM 角色
- 啟用 CloudTrail 進行稽核日誌記錄
- 加密 S3 中的產物
- 使用 VPC 端點進行私有通訊

### 效能
- 使用建置快取加速建置
- 在可能的情況下實施平行部署
- 監控和優化建置時間
- 為建置使用適當的執行個體類型

### 成本優化
- 對非關鍵建置使用 spot 執行個體
- 實施建置產物生命週期政策
- 監控 CodeBuild 使用情況並優化
- 對可預測的工作負載使用保留容量

## 與現有基礎設施的整合

### CDK Stacks
管線與現有 CDK stacks 整合：
- 盡可能重用現有的 IAM 角色
- 維護現有的依賴關係
- 保留當前的配置模式

### 監控
與現有的可觀測性堆疊整合：
- CloudWatch 儀表板
- SNS 通知
- X-Ray 追蹤
- 自訂指標

## 測試

執行測試套件以驗證功能：

```bash
./test-deploy-script.sh
```

這會驗證：
- 腳本語法和函式
- 參數解析
- 說明訊息內容
- 預演功能
- AWS CLI 整合

## 支援

如有問題或疑問：
1. 檢查 CloudWatch 日誌以取得詳細的錯誤訊息
2. 使用 `--dry-run` 驗證配置
3. 使用 `--pipeline-status` 監控部署進度
4. 如果發生存取錯誤，請檢查 IAM 權限

## 未來增強功能

計劃的改進：
- 與 AWS CodeStar 整合進行專案管理
- 支援 AWS CodeGuru 進行程式碼品質分析
- 透過 AWS X-Ray 整合增強監控
- 支援多帳戶部署
