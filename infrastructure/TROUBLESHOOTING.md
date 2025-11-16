# AWS CDK Infrastructure 疑難排解指南

本指南說明在 AWS CDK 基礎設施部署和開發過程中常見的問題。

## 🔧 常見問題與解決方案

### 1. Route53 Health Check 配置問題

**問題**：Route53 health checks 因缺少 API 屬性或配置不正確而失敗。

**症狀**：

- Health checks 在 Route53 控制台顯示為「Failure」
- Failover 未按預期運作
- 缺少警報整合

**解決方案**：

```bash
# The Route53 health check configuration has been updated with:
# - searchString: '"status":"UP"' - looks for Spring Boot Actuator health response
# - insufficientDataHealthStatus: 'Failure' - treats missing data as failure
# - alarmIdentifier - integrates with CloudWatch alarms
# - Additional tags for better resource management
```

**驗證**：

1. 檢查 Route53 控制台的 health check 狀態
2. 驗證 health endpoint 回傳 `{"status":"UP"}`
3. 監控 CloudWatch alarms 的 health check 指標

### 2. OpenSearch Multi-AZ 配置問題

**問題**：使用 t3.small.search 實例時，OpenSearch domain 無法以 Multi-AZ 部署。

**症狀**：

- CDK 部署失敗並出現「UnsupportedOperation」錯誤
- 錯誤訊息提示實例類型不支援 Multi-AZ

**解決方案**：
配置已更新為使用環境特定的實例類型：

```typescript
// Development: Single-AZ with t3.small.search
"development": {
  "instance-type": "t3.small.search",
  "instance-count": 1,
  "multi-az": false,
  "volume-size": 20
}

// Staging/Production: Multi-AZ with m6g instances
"production": {
  "instance-type": "m6g.large.search",
  "instance-count": 3,
  "multi-az": true,
  "volume-size": 100
}
```

**驗證**：

```bash
# Check OpenSearch domain configuration
aws opensearch describe-domain --domain-name genai-demo-logs-production
```

### 3. TypeScript/ts-node Cache 問題

**問題**：CDK synthesis 因過時的 TypeScript 編譯快取或 ts-node 快取而失敗。

**症狀**：

- 「Cannot find module」錯誤
- 使用過時的類型定義
- 編譯結果不一致

**解決方案**：

#### 快速清除快取

```bash
npm run clean:cache
npm run build
npm run synth
```

#### 深度清理

```bash
./scripts/cleanup-cache.sh --deep
npm run build
```

#### 完整重置

```bash
./scripts/cleanup-cache.sh --reinstall
npm run build
```

#### 手動清理

```bash
# Remove TypeScript build info
rm -f tsconfig.tsbuildinfo

# Remove ts-node cache
rm -rf node_modules/.cache/ts-node/

# Remove Jest cache
rm -rf .jest-cache/

# Remove CDK output
rm -rf cdk.out/

# Remove compiled files
find . -name "*.js" -not -path "./node_modules/*" -not -path "./scripts/*" -delete
find . -name "*.d.ts" -not -path "./node_modules/*" -delete
```

## 🚀 部署最佳實踐

### 部署前檢查清單

1. **清理建置環境**：

   ```bash
   npm run clean:cache
   npm run build
   ```

2. **驗證配置**：

   ```bash
   npm run synth:validate
   ```

3. **執行測試**：

   ```bash
   npm run test:ci
   ```

4. **安全掃描**：

   ```bash
   npm run security:scan
   ```

### 環境特定部署

#### Development 環境

```bash
# Clean deployment for development
npm run deploy:clean -- --context environment=development
```

#### Production 環境

```bash
# Validate before production deployment
npm run validate:comprehensive
npm run deploy -- --context environment=production --require-approval broadening
```

## 🔍 除錯指令

### CDK 除錯

```bash
# Synthesize with verbose output
cdk synth --verbose

# Show differences
cdk diff --context environment=production

# List all stacks
cdk list

# Show stack dependencies
cdk synth --json | jq '.[] | select(.type=="aws:cdk:tree") | .metadata'
```

### 基礎設施驗證

```bash
# Validate all templates
npm run validate:templates

# Check for drift
npm run drift:detect

# Analyze performance
npm run performance:analyze
```

## 📊 監控與告警

### Health Check 監控

- **Primary Health Check**：透過 CloudWatch alarm `genai-demo-production-primary-health-alarm` 監控
- **Secondary Health Check**：透過 CloudWatch alarm `genai-demo-production-secondary-health-alarm` 監控
- **Dashboard**：透過 CDK 輸出 URL 存取 failover 監控儀表板

### OpenSearch 監控

- **Domain Health**：在 AWS 控制台檢查 OpenSearch domain 狀態
- **Cluster Metrics**：監控 CPU、記憶體和儲存使用率
- **Index Health**：驗證日誌收集和搜尋效能

### 日誌分析

```bash
# Check application logs
aws logs describe-log-groups --log-group-name-prefix "/aws/containerinsights/genai-demo-cluster"

# Query recent errors
aws logs filter-log-events --log-group-name "/aws/containerinsights/genai-demo-cluster/application" --filter-pattern "ERROR"
```

## 🛠️ 進階疑難排解

### CDK Context 問題

```bash
# Clear CDK context cache
cdk context --clear

# Reset specific context values
cdk context --reset "availability-zones:account=ACCOUNT:region=REGION"
```

### Node.js 記憶體問題

```bash
# Increase Node.js memory limit
export NODE_OPTIONS="--max-old-space-size=4096"
npm run build
```

### AWS Credentials 問題

```bash
# Verify AWS credentials
aws sts get-caller-identity

# Check CDK bootstrap status
cdk bootstrap --show-template
```

## 📞 取得協助

### 收集日誌以供支援

```bash
# Collect comprehensive logs
npm run troubleshoot > troubleshoot-output.log 2>&1

# Generate deployment report
npm run docs:generate
npm run cost:estimate
```

### 常見錯誤模式

1. **「Cannot assume role」**：檢查 IAM 權限和信任關係
2. **「Resource already exists」**：檢查命名衝突或未完成的清理
3. **「Insufficient capacity」**：驗證目標 AZs 中可用的實例類型
4. **「Invalid parameter」**：驗證 cdk.context.json 中的配置值

### 支援資源

- AWS CDK Documentation
- AWS CDK GitHub Issues
- AWS Support Center

## 🔄 復原程序

### 回滾部署

```bash
# Rollback to previous version
cdk deploy --rollback

# Destroy and redeploy
cdk destroy --force
npm run deploy:clean
```

### 緊急程序

1. **Route53 Failover**：如果自動 failover 失敗，手動更新 DNS 記錄
2. **OpenSearch Recovery**：從自動快照還原
3. **完整基礎設施重置**：使用 DR 文件中的災難復原程序
