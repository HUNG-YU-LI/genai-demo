# MCP Configuration 清理建議

> **最後更新**: 2025-01-22
> **狀態**: 準備執行

## 🎯 快速摘要

您的 MCP configuration 有：

- ✅ **5 個運作中的專案 servers**（time, aws-docs, aws-cdk, aws-pricing, excalidraw）
- ✅ **6 個運作中的全域 servers**（github, aws-docs, cdk, pricing, lambda, iam）
- ⚠️ **2 個重複項**（aws-docs, time）
- ⚠️ **10 個已停用的 servers** 在全域 config 中（占用空間）

---

## 📋 建議的行動

### 步驟 1：備份 Configurations ✅

```bash
# 備份全域 config
cp ~/.kiro/settings/mcp.json ~/.kiro/settings/mcp.json.backup.$(date +%Y%m%d)

# 備份專案 config
cp .kiro/settings/mcp.json .kiro/settings/mcp.json.backup.$(date +%Y%m%d)
```

### 步驟 2：從全域 Config 移除重複項

**從 `~/.kiro/settings/mcp.json` 移除的重複項：**

1. **`aws-docs`** - 已在專案 config 中啟用
2. **`time`** - 已在專案 config 中啟用（全域版本反正已停用）

**原因**：專案層級 config 優先，因此這些全域項目未被使用。

**關於 Time Server 的注意事項**：

- `time` MCP server 提供時區轉換與時間格式化
- 然而，Kiro 也可以透過系統命令（`date`）取得目前時間
- **建議**：在專案 config 中保留 `time` server 以進行進階時間操作
  - 時區轉換
  - 以不同格式格式化時間
  - 時間差異計算
  - 多時區支援

### 步驟 3：清理已停用的 Servers（選用）

**全域 config 中可移除的已停用 servers：**

| Server | 移除原因 |
|--------|------------------|
| `aws-knowledge-mcp-server` | 需要 AWS MCP service 存取（尚未可用） |
| `fetch` | 未使用 |
| `awslabs.core-mcp-server` | 未使用 |
| `awslabs.terraform-mcp-server` | 此專案未使用 Terraform |
| `sqlite` | 未使用 SQLite |
| `kubernetes` | 不從 MCP 管理 K8s |
| `docker` | 不從 MCP 管理 Docker |
| `awslabs.ec2-mcp-server` | 不從 MCP 管理 EC2 |
| `ppt-automation` | 依賴外部專案 |

**如果稍後可能使用，請保留這些已停用的 servers：**

- `kubernetes`, `docker`, `sqlite` - 對 infrastructure 管理有用
- `awslabs.ec2-mcp-server` - 對 AWS EC2 管理有用

### 步驟 4：修正 GitHub Token（如使用 GitHub MCP）

全域 config 中的 GitHub token 可能已過期：

```json
"Bearer ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
```

**行動**：

1. 產生新 token：<https://github.com/settings/tokens>
2. 在 `~/.kiro/settings/mcp.json` 中更新

**⚠️ 安全注意事項**：絕不將實際 tokens commit 到 repository。使用如上的佔位符。
3. 或如不需要請停用該 server

---

## 🔧 手動清理步驟

### 選項 A：保守清理（建議）

僅移除重複項，保留已停用的 servers 供未來使用。

**編輯 `~/.kiro/settings/mcp.json`：**

```bash
# 在編輯器中開啟
code ~/.kiro/settings/mcp.json

# 移除這兩個項目：
# 1. "aws-docs": { ... }
# 2. "time": { ... }
```

### 選項 B：積極清理

移除重複項與所有已停用的 servers。

**編輯 `~/.kiro/settings/mcp.json`：**

移除這些項目：

- `aws-docs`
- `time`
- `aws-knowledge-mcp-server`
- `fetch`
- `awslabs.core-mcp-server`
- `awslabs.terraform-mcp-server`
- `sqlite`
- `kubernetes`
- `docker`
- `awslabs.ec2-mcp-server`
- `ppt-automation`

---

## 📊 建議的最終 Configuration

### 全域 Config（`~/.kiro/settings/mcp.json`）

**僅保留這些啟用的 servers：**

```json
{
  "mcpServers": {
    "github": {
      "command": "uvx",
      "args": [
        "mcp-proxy",
        "--transport", "streamablehttp",
        "--headers", "Authorization", "Bearer YOUR_NEW_TOKEN",
        "https://api.githubcopilot.com/mcp/"
      ],
      "disabled": false,
      "autoApprove": [
        "list_issues", "get_issue", "get_issue_comments",
        "create_pull_request", "get_me", "get_pull_request",
        "search_pull_requests", "update_pull_request",
        "search_repositories", "get_file_contents", "create_issue"
      ]
    },
    "awslabs.cdk-mcp-server": {
      "command": "uvx",
      "args": ["awslabs.cdk-mcp-server@latest"],
      "env": {"FASTMCP_LOG_LEVEL": "ERROR"},
      "disabled": false,
      "autoApprove": [
        "CDKGeneralGuidance",
        "ExplainCDKNagRule",
        "CheckCDKNagSuppressions"
      ]
    },
    "awslabs.aws-pricing-mcp-server": {
      "command": "uvx",
      "args": ["awslabs.aws-pricing-mcp-server@latest"],
      "env": {"FASTMCP_LOG_LEVEL": "ERROR"},
      "disabled": false,
      "autoApprove": [
        "analyze_cdk_project",
        "get_pricing",
        "generate_cost_report"
      ]
    },
    "awslabs.lambda-mcp-server": {
      "command": "uvx",
      "args": ["awslabs.lambda-mcp-server@latest"],
      "env": {
        "FASTMCP_LOG_LEVEL": "ERROR",
        "AWS_PROFILE": "kim-sso",
        "AWS_REGION": "ap-northeast-1"
      },
      "disabled": false,
      "autoApprove": [
        "list_functions",
        "invoke_function",
        "get_function_info",
        "update_function_code"
      ]
    },
    "awslabs.iam-mcp-server": {
      "command": "uvx",
      "args": ["awslabs.iam-mcp-server@latest", "--readonly"],
      "env": {
        "FASTMCP_LOG_LEVEL": "ERROR",
        "AWS_PROFILE": "kim-sso",
        "AWS_REGION": "ap-northeast-1"
      },
      "disabled": false,
      "autoApprove": [
        "list_users",
        "list_roles",
        "get_user_policies",
        "get_role_policies"
      ]
    }
  }
}
```

### 專案 Config（`.kiro/settings/mcp.json`）

**保持原樣 - 已是最佳狀態：**

```json
{
  "mcpServers": {
    "time": {
      "command": "uvx",
      "args": ["mcp-server-time"],
      "env": {},
      "disabled": false,
      "autoApprove": [
        "get_current_time",
        "get_timezone",
        "convert_time",
        "format_time",
        "calculate_time_difference"
      ]
    },
    "aws-docs": {
      "command": "uvx",
      "args": ["awslabs.aws-documentation-mcp-server@latest"],
      "env": {"FASTMCP_LOG_LEVEL": "ERROR"},
      "disabled": false,
      "autoApprove": [
        "search_aws_documentation",
        "get_aws_service_info",
        "search_documentation",
        "read_documentation"
      ]
    },
    "aws-cdk": {
      "command": "uvx",
      "args": ["awslabs.cdk-mcp-server@latest"],
      "env": {"FASTMCP_LOG_LEVEL": "ERROR"},
      "disabled": false,
      "autoApprove": [
        "CDKGeneralGuidance",
        "ExplainCDKNagRule",
        "CheckCDKNagSuppressions"
      ]
    },
    "aws-pricing": {
      "command": "uvx",
      "args": ["awslabs.aws-pricing-mcp-server@latest"],
      "env": {"FASTMCP_LOG_LEVEL": "ERROR"},
      "disabled": false,
      "autoApprove": [
        "analyze_cdk_project",
        "get_pricing",
        "generate_cost_report",
        "get_pricing_service_codes"
      ]
    },
    "excalidraw": {
      "command": "node",
      "args": [
        "/Users/yikaikao/git/genai-demo/node_modules/mcp-excalidraw-server/src/index.js"
      ],
      "env": {"ENABLE_CANVAS_SYNC": "false"},
      "disabled": false,
      "autoApprove": [
        "create_element", "update_element", "delete_element",
        "query_elements", "get_resource", "group_elements",
        "ungroup_elements", "align_elements", "distribute_elements",
        "lock_elements", "unlock_elements", "batch_create_elements"
      ]
    }
  }
}
```

---

## ✅ 驗證步驟

清理後：

1. **重新啟動 Kiro**
   - 關閉並重新開啟 Kiro 以重新載入 MCP configuration

2. **檢查 MCP Server 狀態**
   - 開啟 Command Palette：`Cmd+Shift+P`
   - 搜尋："MCP Server"
   - 選擇："View MCP Servers"
   - 驗證所有 servers 顯示 "Connected"

3. **測試關鍵 Servers**

   ```
   詢問 Kiro：

   - "現在幾點？"（測試 time server）
   - "搜尋 AWS docs 中關於 Lambda 的資訊"（測試 aws-docs）
   - "建立一個簡單的圖表"（測試 excalidraw）

   ```

---

## 📈 預期結果

**清理前：**

- Servers 總數：21
- 啟用中：11
- 已停用：10
- 重複項：2

**保守清理後：**

- Servers 總數：19
- 啟用中：11
- 已停用：8
- 重複項：0

**積極清理後：**

- Servers 總數：11
- 啟用中：11
- 已停用：0
- 重複項：0

---

## 🔄 復原計畫

如果出問題：

```bash
# 復原全域 config
cp ~/.kiro/settings/mcp.json.backup.YYYYMMDD ~/.kiro/settings/mcp.json

# 復原專案 config
cp .kiro/settings/mcp.json.backup.YYYYMMDD .kiro/settings/mcp.json

# 重新啟動 Kiro
```

---

## 📝 注意事項

- **Excalidraw**：✅ 完全正常運作，無需變更
- **GitHub**：⚠️ 如使用 GitHub 功能請更新 token
- **AWS Servers**：✅ 全部使用 kim-sso profile 運作正常
- **重複項**：從全域 config 移除是安全的

**建議**：從 **保守清理** 開始以降低風險。
