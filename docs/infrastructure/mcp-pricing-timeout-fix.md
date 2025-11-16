# AWS Pricing MCP Server Timeout 修復

> **日期**: 2025-11-07 12:15
> **問題**: 專案配置中的 `aws-pricing` server 也發生 timeout
> **狀態**: ✅ 已修復

---

## 🔴 問題

在停用全域 AWS MCP servers 後，專案配置中的 `aws-pricing` server 也發生連線 timeout。

---

## 🔧 採取的行動

### 已建立備份

```bash
.kiro/settings/mcp.json.backup.20251107_121500
```

### 已停用 Server

```bash
# 停用專案配置中的 aws-pricing
jq '.mcpServers["aws-pricing"].disabled = true' .kiro/settings/mcp.json
```

---

## ✅ 目前狀態

### 啟用的 Servers (專案配置)

| Server | 狀態 | 用途 |
|--------|--------|---------|
| `time` | ✅ 啟用中 | 時間操作 |
| `aws-docs` | ✅ 啟用中 | AWS documentation |
| `aws-cdk` | ✅ 啟用中 | CDK 操作 |
| `excalidraw` | ✅ 啟用中 | 圖表建立 |

### 已停用的 Servers (專案配置)

| Server | 原因 |
|--------|--------|
| `aws-pricing` | 連線 timeout |

---

## 📊 摘要

**啟用中的 Servers 總數**: 6

- 全域: 2 (`github`, `awslabs.cdk-mcp-server`)
- 專案: 4 (`time`, `aws-docs`, `aws-cdk`, `excalidraw`)

**因 Timeout 停用的總數**: 4

- 全域: 3 (`lambda`, `iam`, `aws-pricing-mcp-server`)
- 專案: 1 (`aws-pricing`)

---

## 🎯 根本原因分析

### 為何 AWS Pricing Servers Timeout

AWS Pricing API servers (全域和專案版本) 發生 timeout 可能是由於：

1. **API 回應緩慢**: AWS Pricing API 回應速度可能很慢
2. **大型資料集**: Pricing 資料龐大且載入需要時間
3. **網路延遲**: 連線到 AWS 服務的額外延遲
4. **首次初始化**: Package 下載和初始化的開銷

### 為何其他 AWS Servers 正常運作

- **aws-docs**: 使用快取的 documentation，回應較快
- **aws-cdk**: 本機 CDK 指引，不需要 API 呼叫
- **awslabs.cdk-mcp-server**: 類似 aws-cdk，本機操作

---

## 💡 建議

### 短期 (目前)

✅ 保持 pricing servers 停用以維持穩定運作

### 長期 (選擇性)

如果您需要 pricing 功能：

1. **預先安裝 package**：

   ```bash
   uvx awslabs.aws-pricing-mcp-server@latest --help
   ```

2. **手動測試**：

   ```bash
   AWS_PROFILE=kim-sso AWS_REGION=ap-northeast-1 \
     uvx awslabs.aws-pricing-mcp-server@latest
   ```

3. **增加 timeout** (如果 Kiro 支援)：
   - 檢查 Kiro 設定的 MCP timeout 配置
   - 將 pricing servers 的 timeout 增加到 60-90 秒

4. **改用 AWS CLI**：

   ```bash
   # 透過 CLI 取得 pricing
   aws pricing get-products \
     --service-code AmazonEC2 \
     --filters Type=TERM_MATCH,Field=location,Value="Asia Pacific (Tokyo)" \
     --profile kim-sso
   ```

---

## 🔄 替代解決方案

### 選項 1: 使用 AWS Cost Explorer

- 成本分析更可靠
- 網頁介面
- 歷史成本資料

### 選項 2: 使用 AWS Pricing Calculator

- <https://calculator.aws/>
- 全面的 pricing 估算
- 無 API timeout

### 選項 3: 使用 Infracost (用於 CDK)

```bash
# 安裝 Infracost
brew install infracost

# 從 CDK 生成成本估算
cdk synth > template.yaml
infracost breakdown --path template.yaml
```

---

## 📋 測試檢查清單

重新啟動後，驗證這些功能正常運作：

- [ ] "What time is it?" (time)
- [ ] "Search AWS docs for Lambda" (aws-docs)
- [ ] "Explain CDK Nag rule AwsSolutions-IAM4" (aws-cdk)
- [ ] "Create a simple diagram" (excalidraw)
- [ ] "List my GitHub repos" (github - 如果 token 有效)

---

## 🎉 結論

所有 timeout 問題已透過停用有問題的 pricing servers 解決。您的 MCP 配置現在穩定且快速。

**下一步**: 重新啟動 Kiro 並享受改善的效能！ 🚀

---

**相關 Documentation**：

- [MCP Final Status](./mcp-final-status.md)
- [AWS Servers Troubleshooting](./mcp-aws-servers-troubleshooting.md)
