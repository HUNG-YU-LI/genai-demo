# 快速部署指南 - GenAI Demo Infrastructure

**更新時間**：2025 年 9 月 29 日 12:49 PM（台北時間）
**Task 10 狀態**：✅ **已完成** - IAM 細粒度存取控制

## 🚀 快速開始指令

### 1. 完整基礎設施部署

```bash
# Development 環境（建議首次部署）
./deploy-unified.sh full -e development -r ap-east-2

# Staging 環境包含分析功能
./deploy-unified.sh full -e staging --enable-analytics -a your-email@company.com

# Production 環境包含多區域
./deploy-unified.sh full -e production --enable-multi-region --enable-analytics
```

### 2. 元件特定部署

```bash
# 僅基礎（網路、安全、IAM）
./deploy-unified.sh foundation -e development

# 安全元件（IAM、SSO、IRSA）- Task 10 ✅
./deploy-unified.sh security -e development

# 資料層（RDS、ElastiCache、MSK）
./deploy-unified.sh data -e development

# 運算層（EKS）
./deploy-unified.sh compute -e development

# 可觀測性（監控、告警）
./deploy-unified.sh observability -e development
```

### 3. IAM 安全部署（Task 10 特定）

```bash
# 部署 IAM 細粒度存取控制
./deploy-iam-security.sh development ap-east-2

# 部署包含 SSO 整合
./deploy-iam-security.sh production ap-east-2 arn:aws:sso:::instance/ssoins-xxxxxxxxx
```

## 📋 部署前檢查清單

### 先決條件 ✅
- [ ] AWS CLI 已配置（`aws sts get-caller-identity`）
- [ ] Node.js 18+ 已安裝（`node --version`）
- [ ] AWS CDK CLI 已安裝（`npm install -g aws-cdk`）
- [ ] 具備 CDK 部署所需的適當 AWS 權限

### 環境設定 ✅
```bash
# 1. 安裝相依套件
npm install

# 2. 建置專案
npm run build

# 3. 執行測試
npm test

# 4. 檢查狀態
./status-check.sh
```

## 🔍 部署狀態監控

### 檢查整體狀態
```bash
# 檢查所有 stacks 狀態
./deploy-unified.sh --status -e development -r ap-east-2

# 快速基礎設施健康檢查
./status-check.sh
```

### 驗證特定元件
```bash
# 檢查 IAM roles（Task 10）
aws iam list-roles --query 'Roles[?contains(RoleName, `genai-demo-development`)].RoleName' --output table

# 檢查 EKS service accounts
kubectl get serviceaccounts -A | grep genai-demo

# 檢查 CloudFormation stacks
aws cloudformation list-stacks --stack-status-filter CREATE_COMPLETE UPDATE_COMPLETE
```

## 🛠️ 疑難排解快速修復

### 常見問題與解決方案

#### 1. 需要 CDK Bootstrap
```bash
# 在您的區域 bootstrap CDK
cdk bootstrap --region ap-east-2
```

#### 2. 建置失敗
```bash
# 清理並重新建置
npm run clean
npm install
npm run build
```

#### 3. 測試失敗
```bash
# 執行特定測試套件
npm run test:unit
npm run test:integration
```

#### 4. Stack 相依性
```bash
# 按正確順序部署
./deploy-unified.sh foundation -e development
./deploy-unified.sh data -e development
./deploy-unified.sh compute -e development
./deploy-unified.sh security -e development
```

## 🔐 安全部署驗證

### Task 10 - IAM 細粒度存取控制 ✅

#### 1. 驗證 IAM Stacks
```bash
# 檢查 IAM stack
aws cloudformation describe-stacks --stack-name genai-demo-development-iam --region ap-east-2

# 檢查 SSO stack（如已部署）
aws cloudformation describe-stacks --stack-name genai-demo-development-sso --region ap-east-2

# 檢查 EKS IRSA stack
aws cloudformation describe-stacks --stack-name genai-demo-development-eks-irsa --region ap-east-2
```

#### 2. 測試 IAM Roles
```bash
# 列出應用程式 roles
aws iam get-role --role-name genai-demo-development-app-role

# 檢查 role policies
aws iam list-attached-role-policies --role-name genai-demo-development-app-role
```

#### 3. 驗證 EKS 整合
```bash
# 更新 kubeconfig
aws eks update-kubeconfig --region ap-east-2 --name genai-demo-development-cluster

# 檢查 service accounts
kubectl get serviceaccounts -n application
kubectl describe serviceaccount genai-demo-app-sa -n application
```

## 📊 部署選項矩陣

| Environment | Command | Features | Use Case |
|-------------|---------|----------|----------|
| Development | `./deploy-unified.sh full -e development` | 基本設定、快速部署 | 日常開發 |
| Staging | `./deploy-unified.sh full -e staging --enable-analytics` | 啟用分析、測試 | 生產前驗證 |
| Production | `./deploy-unified.sh full -e production --enable-multi-region --enable-analytics` | 完整功能、多區域 | 生產工作負載 |

## 🎯 NPM Script 快捷指令

```bash
# 快速部署指令
npm run deploy:dev          # Development 環境
npm run deploy:staging      # Staging 包含分析
npm run deploy:prod         # Production 包含多區域

# 元件部署
npm run deploy:foundation   # 網路、安全、IAM
npm run deploy:security     # IAM、SSO、IRSA（Task 10）
npm run deploy:data         # RDS、ElastiCache、MSK
npm run deploy:compute      # EKS cluster

# 工具指令
npm run status              # 檢查部署狀態
npm run destroy:dev         # 銷毀 development 環境
```

## 🔄 部署工作流程

### 標準部署流程
1. **準備** ✅
   ```bash
   ./status-check.sh
   npm run build
   npm test
   ```

2. **基礎部署** ✅
   ```bash
   ./deploy-unified.sh foundation -e development
   ```

3. **安全部署** ✅（Task 10）
   ```bash
   ./deploy-unified.sh security -e development
   ```

4. **資料層部署** ✅
   ```bash
   ./deploy-unified.sh data -e development
   ```

5. **運算部署** ✅
   ```bash
   ./deploy-unified.sh compute -e development
   ```

6. **可觀測性部署** ✅
   ```bash
   ./deploy-unified.sh observability -e development
   ```

7. **驗證** ✅
   ```bash
   ./deploy-unified.sh --status -e development
   ```

## 🚨 緊急程序

### 回滾部署
```bash
# 銷毀特定環境
./deploy-unified.sh --destroy -e development

# 銷毀特定元件
cdk destroy genai-demo-development-iam --region ap-east-2
```

### 快速復原
```bash
# 重新部署基礎
./deploy-unified.sh foundation -e development

# 重新部署安全（Task 10）
./deploy-iam-security.sh development ap-east-2
```

## 📞 支援資源

### 文件
- [完整基礎設施指南](README.md)
- [部署腳本摘要](DEPLOYMENT_SCRIPTS_SUMMARY.md)
- [安全實作](SECURITY_IMPLEMENTATION.md)
- [疑難排解指南](TROUBLESHOOTING.md)

### 快速協助
```bash
# 取得部署協助
./deploy-unified.sh --help

# 取得 IAM 安全協助
./deploy-iam-security.sh --help

# 檢查基礎設施狀態
./status-check.sh
```

### AWS Console 連結
- [CloudFormation Stacks](https://console.aws.amazon.com/cloudformation/)
- [IAM Roles](https://console.aws.amazon.com/iam/home#/roles)
- [EKS Clusters](https://console.aws.amazon.com/eks/home)
- [SSO Console](https://console.aws.amazon.com/singlesignon/)

---

**狀態**：✅ **Task 10 已完成 - IAM 細粒度存取控制**
**基礎設施**：✅ **已準備好生產部署**
**安全性**：✅ **已完整實作並測試**
