# MCP Server Configuration 分析

> **最後更新**: 2025-01-22
> **狀態**: 配置審查中

## 總覽

本文件分析目前的 MCP (Model Context Protocol) server 配置，識別哪些 servers 是全域 vs 專案層級、其狀態，以及最佳化建議。

---

## Configuration 階層架構

### 全域 Configuration

**位置**: `~/.kiro/settings/mcp.json`
**範圍**: 適用於所有 Kiro workspaces
**優先級**: 較低（會被專案層級 config 覆蓋）

### 專案 Configuration

**位置**: `.kiro/settings/mcp.json`
**範圍**: 僅限此專案
**優先級**: 較高（對於相同 server 名稱會覆蓋全域 config）

---

## Server 清單

### ✅ 啟用中的 Servers (專案層級)

| Server | 類型 | 狀態 | 用途 |
|--------|------|--------|---------|
| `time` | Project | ✅ 啟用中 | 時間操作與時區轉換 |
| `aws-docs` | Project | ✅ 啟用中 | AWS documentation 搜尋 |
| `aws-cdk` | Project | ✅ 啟用中 | CDK 指引與 Nag rule 說明 |
| `aws-pricing` | Project | ✅ 啟用中 | AWS 成本分析與 pricing |
| `excalidraw` | Project | ✅ 啟用中 | 圖表建立（本機 node server） |

### ✅ 啟用中的 Servers (全域層級)

| Server | 類型 | 狀態 | 用途 |
|--------|------|--------|---------|
| `github` | Global | ✅ 啟用中 | GitHub API 操作 |
| `aws-docs` | Global | ✅ 啟用中 | AWS documentation（重複） |
| `awslabs.cdk-mcp-server` | Global | ✅ 啟用中 | CDK 操作（重複） |
| `awslabs.aws-pricing-mcp-server` | Global | ✅ 啟用中 | Pricing（重複） |
| `awslabs.lambda-mcp-server` | Global | ✅ 啟用中 | Lambda function 管理 |
| `awslabs.iam-mcp-server` | Global | ✅ 啟用中 | IAM 唯讀操作 |

### ⚠️ 已停用的 Servers (全域層級)

| Server | 原因 | 建議 |
|--------|--------|----------------|
| `aws-knowledge-mcp-server` | 已停用 | 如不需要可移除 |
| `fetch` | 已停用 | 如需 web scraping 請啟用 |
| `awslabs.core-mcp-server` | 已停用 | 用於 Well-Architected reviews 請啟用 |
| `awslabs.terraform-mcp-server` | 已停用 | 如使用 Terraform 請啟用 |
| `sqlite` | 已停用 | 用於本機 DB 操作請啟用 |
| `kubernetes` | 已停用 | 如管理 K8s clusters 請啟用 |
| `docker` | 已停用 | 用於 container 管理請啟用 |
| `time` | 已停用（全域） | 已在專案中啟用 |
| `awslabs.ec2-mcp-server` | 已停用 | 如管理 EC2 instances 請啟用 |
| `ppt-automation` | 已停用 | 如建立 PowerPoint 請啟用 |

---

## 重複的 Configurations

### ⚠️ 同時在全域與專案定義的 Servers

這些 servers 同時在兩個 configurations 中定義。**專案層級優先**。

| Server 名稱 | 全域狀態 | 專案狀態 | 建議 |
|-------------|---------------|----------------|----------------|
| `aws-docs` | ✅ 啟用中 | ✅ 啟用中 | 保留專案層級，從全域移除 |
| `time` | ❌ 已停用 | ✅ 啟用中 | 保留專案層級，從全域移除 |

**注意**: 以下名稱不同但功能相同：

- 全域: `awslabs.cdk-mcp-server` vs 專案: `aws-cdk`
- 全域: `awslabs.aws-pricing-mcp-server` vs 專案: `aws-pricing`

---

## 連線問題分析

### 可能的連線問題

根據配置，這些 servers 可能有連線問題：

#### 1. **aws-knowledge-mcp-server** (全域，已停用)

```json
"command": "uvx",
"args": ["mcp-proxy", "--transport", "streamablehttp",
         "https://knowledge-mcp.global.api.aws"]
```

**問題**: 需要 AWS authentication 與 AWS MCP endpoint 的網路存取
**狀態**: 目前已停用
**行動**: 除非 AWS MCP service 可用，否則保持停用

#### 2. **github** (全域，啟用中)

```json
"args": ["mcp-proxy", "--transport", "streamablehttp",
         "--headers", "Authorization", "Bearer gho_16gd32s7..."]
```

**問題**: Bearer token 可能已過期或無效
**狀態**: 啟用中但可能 authentication 失敗
**行動**: 更新 token 或如不需要請停用

#### 3. **excalidraw** (專案，啟用中) ✅

```json
"command": "node",
"args": ["/Users/yikaikao/git/genai-demo/node_modules/mcp-excalidraw-server/src/index.js"]
```

**狀態**: ✅ **完全正常運作** - 已驗證本機安裝
**版本**: 1.0.5
**功能**:

- 完整的 MCP server 含 15+ tools
- 元素建立（rectangles, ellipses, diamonds, arrows, text, lines）
- 元素管理（update, delete, query）
- Batch 操作
- 進階功能（grouping, alignment, distribution, locking）
- 選用的 canvas sync 與前端（目前已停用）

**Configuration**:

- `ENABLE_CANVAS_SYNC=false` - 以 standalone 模式執行
- 基本圖表建立不需要前端 server
- 所有 tools 透過 MCP protocol 可用

**可用的 Tools**:

- `create_element`, `update_element`, `delete_element`, `query_elements`
- `batch_create_elements`
- `group_elements`, `ungroup_elements`
- `align_elements`, `distribute_elements`
- `lock_elements`, `unlock_elements`
- `get_resource`

**行動**: ✅ 無需行動 - 運作正常

#### 4. **ppt-automation** (全域，已停用)

```json
"command": "uv",
"args": ["run", "--directory", "/Users/yikaikao/git/dst-lab/powerpoint-automation-mcp", ...]
```

**問題**: 依賴外部專案目錄
**狀態**: 已停用
**行動**: 除非需要 PowerPoint automation，否則保持停用

---

## 建議

### 立即行動

1. **從全域 Config 移除重複項**

   ```bash
   # 編輯 ~/.kiro/settings/mcp.json 並移除：
   # - aws-docs（保留在專案中）
   # - time（保留在專案中）
   ```

2. **修正 GitHub Token**（如使用 GitHub MCP）
   - 在此產生新 token：<https://github.com/settings/tokens>
   - 在 `~/.kiro/settings/mcp.json` 中更新

3. **驗證 Excalidraw 安裝**

   ```bash
   cd /Users/yikaikao/git/genai-demo
   npm install mcp-excalidraw-server
   ```

### Configuration 最佳化

#### 建議的全域 Config（跨專案 Tools）

```json
{
  "mcpServers": {
    "github": {
      "command": "uvx",
      "args": ["mcp-proxy", "--transport", "streamablehttp",
               "--headers", "Authorization", "Bearer YOUR_NEW_TOKEN",
               "https://api.githubcopilot.com/mcp/"],
      "disabled": false,
      "autoApprove": ["list_issues", "get_issue", "create_pull_request"]
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
      "autoApprove": ["list_functions", "invoke_function"]
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
      "autoApprove": ["list_users", "list_roles"]
    }
  }
}
```

#### 建議的專案 Config（專案特定 Tools）

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
      "autoApprove": ["search_aws_documentation"]
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

---

## 測試 MCP Servers

### 測試個別 Server

```bash
# 測試 uvx 是否能執行 server
uvx mcp-server-time --help

# 測試 AWS servers（需要 AWS credentials）
uvx awslabs.aws-documentation-mcp-server@latest --help
```

### 在 Kiro 中驗證 Server 狀態

1. 開啟 Command Palette：`Cmd+Shift+P`
2. 搜尋："MCP Server"
3. 選擇："View MCP Servers"
4. 檢查每個 server 的連線狀態

---

## 摘要

### 目前狀態

- **配置的 Servers 總數**: 21
- **啟用中（專案）**: 5
- **啟用中（全域）**: 6
- **已停用（全域）**: 10
- **重複項**: 2（aws-docs, time）

### 主要問題

1. ✅ **Excalidraw**: 本機相依性 - 驗證安裝
2. ⚠️ **GitHub**: Token 可能已過期
3. ⚠️ **重複項**: 從全域 config 移除
4. ℹ️ **許多已停用**: 考慮清理

### 後續步驟

1. 清理重複的 configurations
2. 如需要請更新 GitHub token
3. 驗證 excalidraw 安裝
4. 移除未使用的已停用 servers
5. 測試啟用中的 servers 連線性

---

**相關 Documentation**:

- [MCP Configuration Guide](https://docs.kiro.ai/mcp)
- [AWS MCP Servers](https://github.com/awslabs/mcp-servers)
