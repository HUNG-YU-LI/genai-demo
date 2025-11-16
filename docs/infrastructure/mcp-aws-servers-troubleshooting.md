# AWS MCP Servers 故障排除指南

> **最後更新**: 2025-11-07
> **問題**: IAM, Lambda, 與 Pricing MCP servers 連線 timeout

---

## 🔴 目前問題

以下 AWS MCP servers 遇到連線 timeouts：

- ❌ `awslabs.iam-mcp-server`
- ❌ `awslabs.lambda-mcp-server`
- ❌ `awslabs.aws-pricing-mcp-server`

---

## 🔍 診斷

### AWS Credentials 狀態

```bash
✅ AWS Profile: kim-sso
✅ Account: 584518143473
✅ Role: AWSAdministratorAccess
✅ Region: us-east-1 (default)
```

### 可能原因

1. **Server 啟動時間** ⏱️
   - AWS MCP servers 初始化可能需要較長時間
   - 首次透過 `uvx` 下載 package
   - 連到 AWS services 的網路延遲

2. **Region 不一致** 🌏
   - Config 指定：`ap-northeast-1`
   - AWS CLI 預設：`us-east-1`
   - 可能造成混淆或延遲

3. **Package 安裝問題** 📦
   - `uvx` 需要在首次執行時下載 packages
   - 下載期間的網路問題
   - Package 版本衝突

4. **Timeout 設定** ⏰
   - Kiro 的預設 MCP timeout 可能太短
   - AWS API 呼叫可能很慢

---

## 🔧 解決方案

### 解決方案 1：停用有問題的 Servers（快速修正）

如果您不立即需要這些 servers，請停用它們：

**編輯 `~/.kiro/settings/mcp.json`：**

```json
{
  "mcpServers": {
    "awslabs.lambda-mcp-server": {
      "disabled": true,  // 加入這一行
      // ... 其餘 config
    },
    "awslabs.iam-mcp-server": {
      "disabled": true,  // 加入這一行
      // ... 其餘 config
    },
    "awslabs.aws-pricing-mcp-server": {
      "disabled": true,  // 加入這一行
      // ... 其餘 config
    }
  }
}
```

**然後重新啟動 Kiro。**

---

### 解決方案 2：預先安裝 Packages（建議）

先手動安裝 packages 以避免 Kiro 啟動時 timeout：

```bash
# 安裝 Lambda MCP server
uvx awslabs.lambda-mcp-server@latest --help

# 安裝 IAM MCP server
uvx awslabs.iam-mcp-server@latest --help

# 安裝 Pricing MCP server
uvx awslabs.aws-pricing-mcp-server@latest --help
```

這將會：

- 下載並快取 packages
- 驗證它們使用您的 AWS credentials 運作
- 加速 Kiro 啟動

**然後重新啟動 Kiro。**

---

### 解決方案 3：修正 Region Configuration

確保一致的 region configuration：

**選項 A：使用 us-east-1（符合 AWS CLI 預設）**

編輯 `~/.kiro/settings/mcp.json`：

```json
{
  "mcpServers": {
    "awslabs.lambda-mcp-server": {
      "env": {
        "FASTMCP_LOG_LEVEL": "ERROR",
        "AWS_PROFILE": "kim-sso",
        "AWS_REGION": "us-east-1"  // 從 ap-northeast-1 變更
      }
    },
    "awslabs.iam-mcp-server": {
      "env": {
        "FASTMCP_LOG_LEVEL": "ERROR",
        "AWS_PROFILE": "kim-sso",
        "AWS_REGION": "us-east-1"  // 從 ap-northeast-1 變更
      }
    },
    "awslabs.aws-pricing-mcp-server": {
      "env": {
        "FASTMCP_LOG_LEVEL": "ERROR",
        "AWS_PROFILE": "kim-sso",
        "AWS_REGION": "us-east-1"  // 從 ap-northeast-1 變更
      }
    }
  }
}
```

**選項 B：保留 ap-northeast-1（如需要東京 region）**

保持 config 不變，但請注意：

- Lambda functions 必須存在於 ap-northeast-1
- IAM 是全域的，所以 region 影響不大
- Pricing API 是全域運作的

---

### 解決方案 4：增加 Logging 以除錯

暫時提高 log level 以查看發生了什麼：

```json
{
  "mcpServers": {
    "awslabs.lambda-mcp-server": {
      "env": {
        "FASTMCP_LOG_LEVEL": "DEBUG",  // 從 ERROR 變更
        "AWS_PROFILE": "kim-sso",
        "AWS_REGION": "ap-northeast-1"
      }
    }
  }
}
```

檢查 Kiro logs 以查看詳細錯誤訊息。

---

### 解決方案 5：手動測試 Servers

獨立測試每個 server 以識別問題：

```bash
# 測試 Lambda server
AWS_PROFILE=kim-sso AWS_REGION=ap-northeast-1 uvx awslabs.lambda-mcp-server@latest

# 測試 IAM server
AWS_PROFILE=kim-sso AWS_REGION=ap-northeast-1 uvx awslabs.iam-mcp-server@latest --readonly

# 測試 Pricing server
AWS_PROFILE=kim-sso AWS_REGION=ap-northeast-1 uvx awslabs.aws-pricing-mcp-server@latest
```

如果任何失敗，您將看到實際的錯誤訊息。

---

## 📋 建議的行動計畫

### 步驟 1：快速修正（立即）

停用有問題的 servers 以解除您工作的阻礙：

```bash
# 編輯全域 config
code ~/.kiro/settings/mcp.json

# 設定 disabled: true 給：
# - awslabs.lambda-mcp-server
# - awslabs.iam-mcp-server
# - awslabs.aws-pricing-mcp-server

# 重新啟動 Kiro
```

### 步驟 2：調查（有時間時）

1. **預先安裝 packages**：

   ```bash
   uvx awslabs.lambda-mcp-server@latest --help
   uvx awslabs.iam-mcp-server@latest --help
   uvx awslabs.aws-pricing-mcp-server@latest --help
   ```

2. **手動測試** 以查看實際錯誤

3. **檢查您是否真的需要這些 servers**：
   - 您透過 Kiro 管理 Lambda functions 嗎？
   - 您在 Kiro 中需要 IAM 資訊嗎？
   - 您在 Kiro 中需要 AWS pricing 嗎？

### 步驟 3：重新啟用（如需要）

一旦 packages 預先安裝並測試過：

1. 在 config 中設定 `disabled: false`
2. 重新啟動 Kiro
3. 驗證連線

---

## 🎯 最小運作 Configuration

如果您不需要透過 MCP 管理 AWS 資源，這裡有一個最小 config：

**全域 Config**（`~/.kiro/settings/mcp.json`）：

```json
{
  "mcpServers": {
    "github": {
      "command": "uvx",
      "args": [
        "mcp-proxy",
        "--transport", "streamablehttp",
        "--headers", "Authorization", "Bearer YOUR_TOKEN",
        "https://api.githubcopilot.com/mcp/"
      ],
      "disabled": false,
      "autoApprove": ["list_issues", "get_issue", "create_pull_request"]
    }
  }
}
```

**專案 Config**（`.kiro/settings/mcp.json`）：

```json
{
  "mcpServers": {
    "time": {
      "command": "uvx",
      "args": ["mcp-server-time"],
      "disabled": false,
      "autoApprove": ["get_current_time", "convert_time"]
    },
    "aws-docs": {
      "command": "uvx",
      "args": ["awslabs.aws-documentation-mcp-server@latest"],
      "env": {"FASTMCP_LOG_LEVEL": "ERROR"},
      "disabled": false,
      "autoApprove": ["search_aws_documentation", "read_documentation"]
    },
    "aws-cdk": {
      "command": "uvx",
      "args": ["awslabs.cdk-mcp-server@latest"],
      "env": {"FASTMCP_LOG_LEVEL": "ERROR"},
      "disabled": false,
      "autoApprove": ["CDKGeneralGuidance", "ExplainCDKNagRule"]
    },
    "aws-pricing": {
      "command": "uvx",
      "args": ["awslabs.aws-pricing-mcp-server@latest"],
      "env": {"FASTMCP_LOG_LEVEL": "ERROR"},
      "disabled": false,
      "autoApprove": ["analyze_cdk_project", "get_pricing"]
    },
    "excalidraw": {
      "command": "node",
      "args": ["./node_modules/mcp-excalidraw-server/src/index.js"],
      "env": {"ENABLE_CANVAS_SYNC": "false"},
      "disabled": false,
      "autoApprove": ["create_element", "update_element"]
    }
  }
}
```

這保留了：

- ✅ Documentation servers（aws-docs, aws-cdk）
- ✅ Pricing 分析（aws-pricing）
- ✅ 圖表建立（excalidraw）
- ✅ 時間工具（time）
- ✅ GitHub 整合（github）

移除了：

- ❌ Lambda 管理（IDE 中很少需要）
- ❌ IAM 管理（IDE 中很少需要）

---

## 🔍 除錯命令

### 檢查 Package 安裝

```bash
# 列出已安裝的 uvx packages
ls ~/.local/share/uv/tools/

# 檢查是否已安裝 AWS MCP servers
ls ~/.local/share/uv/tools/ | grep awslabs
```

### 測試 AWS 連線性

```bash
# 測試 AWS CLI 運作
aws sts get-caller-identity --profile kim-sso

# 測試 Lambda 存取
aws lambda list-functions --profile kim-sso --region ap-northeast-1 --max-items 1

# 測試 IAM 存取
aws iam list-users --profile kim-sso --max-items 1
```

### 檢查 Kiro Logs

在 Kiro 的 output panel 或 logs 中尋找 MCP 相關錯誤。

---

## 📊 Server 優先級評估

| Server | 優先級 | 使用情境 | 建議 |
|--------|----------|----------|----------------|
| `aws-docs` | 🔴 高 | Documentation 查詢 | ✅ 保持啟用 |
| `aws-cdk` | 🔴 高 | CDK 開發 | ✅ 保持啟用 |
| `aws-pricing` | 🟡 中 | 成本分析 | ✅ 保持啟用 |
| `excalidraw` | 🟡 中 | 圖表 | ✅ 保持啟用 |
| `time` | 🟡 中 | 時間操作 | ✅ 保持啟用 |
| `github` | 🟡 中 | GitHub 操作 | ✅ 保持啟用 |
| `lambda` | 🟢 低 | Lambda 管理 | ⚠️ 如 timeout 請停用 |
| `iam` | 🟢 低 | IAM 查詢 | ⚠️ 如 timeout 請停用 |

---

## ✅ 快速修正 Script

儲存為 `fix-aws-mcp-servers.sh`：

```bash
#!/bin/bash

echo "🔧 修正 AWS MCP Server 問題"
echo ""

# 備份
cp ~/.kiro/settings/mcp.json ~/.kiro/settings/mcp.json.backup.$(date +%Y%m%d_%H%M%S)
echo "✅ 已建立備份"

# 停用有問題的 servers
jq '.mcpServers["awslabs.lambda-mcp-server"].disabled = true |
    .mcpServers["awslabs.iam-mcp-server"].disabled = true |
    .mcpServers["awslabs.aws-pricing-mcp-server"].disabled = true' \
    ~/.kiro/settings/mcp.json > ~/.kiro/settings/mcp.json.tmp

mv ~/.kiro/settings/mcp.json.tmp ~/.kiro/settings/mcp.json

echo "✅ 已停用有問題的 AWS MCP servers"
echo ""
echo "📋 後續步驟："
echo "1. 重新啟動 Kiro"
echo "2. 驗證其他 servers 運作正常"
echo "3. 選擇性地預先安裝 packages 並重新啟用"
```

執行方式：

```bash
chmod +x fix-aws-mcp-servers.sh
./fix-aws-mcp-servers.sh
```

---

## 🎯 建議的解決方案

**立即提升生產力**：

1. 停用三個有問題的 servers
2. 保留運作中的 servers（專案中的 aws-docs, aws-cdk, aws-pricing，全域的 github）
3. 重新啟動 Kiro

**長期**：

1. 有時間時預先安裝 packages
2. 手動測試它們
3. 只有在真正需要時才重新啟用

大多數開發者不需要直接在 IDE 中管理 Lambda/IAM，因此停用它們完全沒問題。

---

**相關 Documentation**：

- [MCP Cleanup Report](./mcp-cleanup-report.md)
- [MCP Server Analysis](./mcp-server-analysis.md)
