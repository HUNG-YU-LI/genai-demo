# Claude Code 配置目錄

本目錄包含將 Amazon Kiro 架構技巧映射到 Claude Code 的完整配置。

## 目錄結構

```
.claude/
├── skills/                          # Skills 技能模組（2個）
│   ├── ddd-aggregate-generator.md   # DDD 聚合根生成器
│   └── test-generator.md            # 測試生成器
│
├── hooks/                           # Hooks 自動化（2個）
│   ├── pre-commit-validation.json   # 提交前驗證
│   └── post-edit-sync.json          # 編輯後同步
│
├── agents/                          # Sub-Agents 專門代理（2個）
│   ├── domain-modeler.json          # 領域建模代理
│   └── test-architect.json          # 測試架構代理
│
├── settings/                        # 全域設定（2個）
│   ├── mcp.json                     # MCP 服務器配置
│   └── constraints.json             # 開發約束
│
├── examples/                        # 範例程式碼（1個）
│   └── simple-aggregate.md          # 簡單聚合根範例
│
└── README.md                        # 本文件
```

## 快速開始

### 1. 使用 Skills

```
/skill ddd-aggregate-generator

請生成以下聚合根：
- 名稱: Product
- 有界上下文: Product
- 值物件: ProductId, ProductName, Price
- 業務方法: publish(), updatePrice(Price newPrice)
```

### 2. 使用 Sub-Agents

```
/agent domain-modeler

請協助設計訂單管理的領域模型，包含：
- Order 聚合根
- OrderItem 實體
- 相關領域事件
```

### 3. 啟用 Hooks

```bash
# Pre-Commit Hook
cp hooks/pre-commit-validation.json ../.git/hooks/pre-commit
chmod +x ../.git/hooks/pre-commit

# Post-Edit Hook (自動啟用)
# Claude Code 會自動讀取 hooks/*.json
```

## Kiro 技巧映射

本配置應用了以下 Amazon Kiro 技巧：

| Kiro 技巧 | Claude Code 實作 | 檔案 |
|----------|----------------|------|
| Idempotency | 相同輸入產生相同程式碼 | skills/*.md |
| Stateless Handler | Skills 無狀態執行 | agents/*.json |
| Immutable Input | Record 值物件 | examples/*.md |
| Event-Driven | Hooks 事件觸發 | hooks/*.json |
| Boundary Control | 架構約束驗證 | settings/constraints.json |
| Fail-Safe Pattern | Pre-Commit 驗證 | hooks/pre-commit-validation.json |
| Workflow Decomposition | Sub-Agents 分工 | agents/*.json |
| Isolation Pattern | Agent 隔離執行 | agents/*.json |
| Single Responsibility | 一個 Skill 一個功能 | skills/*.md |
| Configuration as Code | 所有配置版本控制 | .claude/ 整個目錄 |

## 檔案說明

### Skills（技能模組）

- **ddd-aggregate-generator.md**: 自動生成 DDD 聚合根、值物件、領域事件
- **test-generator.md**: 自動生成測試套件（Unit/Integration/E2E）

### Hooks（自動化鉤子）

- **pre-commit-validation.json**: 提交前執行 ArchUnit、Checkstyle、測試覆蓋率檢查
- **post-edit-sync.json**: 程式碼編輯後自動同步文件和圖表

### Agents（專門代理）

- **domain-modeler.json**: 領域建模專家，協助設計聚合、識別有界上下文
- **test-architect.json**: 測試架構專家，設計測試策略和測試金字塔

### Settings（全域設定）

- **mcp.json**: MCP 服務器配置（time, aws-docs, aws-cdk, filesystem, git, database-schema）
- **constraints.json**: 開發約束（命名規範、架構限制、DDD 約束、測試要求）

### Examples（範例）

- **simple-aggregate.md**: 完整的 Product 聚合根實作範例，展示所有 Kiro 技巧

## 配置自訂

### 修改 MCP 服務器

編輯 `settings/mcp.json`:

```json
{
  "mcpServers": {
    "your-server": {
      "command": "uvx",
      "args": ["your-mcp-server"],
      "autoApprove": ["your-tool"]
    }
  }
}
```

### 修改開發約束

編輯 `settings/constraints.json`:

```json
{
  "codeStyleConstraints": {
    "complexity": {
      "maxMethodLength": 20,  // 調整方法長度限制
      "maxClassLength": 500   // 調整類別長度限制
    }
  }
}
```

### 新增 Skill

1. 在 `skills/` 目錄建立新的 `.md` 檔案
2. 參考現有 Skills 的格式
3. 說明 Kiro 技巧映射
4. 提供輸入規格和生成模板

### 新增 Hook

1. 在 `hooks/` 目錄建立新的 `.json` 檔案
2. 定義觸發條件 (`trigger`)
3. 定義執行動作 (`actions`)
4. 說明 Kiro 技巧映射

## 最佳實踐

### 1. Skills 使用

- ✅ 提供清晰的領域模型定義
- ✅ 生成後進行人工審查
- ✅ 執行 ArchUnit 驗證架構合規性
- ❌ 不要直接使用生成的程式碼而不審查

### 2. Hooks 使用

- ✅ 定期執行 Pre-Commit Hook
- ✅ 注意 Post-Edit Hook 的提示訊息
- ✅ 及時更新文件和圖表
- ❌ 不要忽略 Hook 的警告訊息

### 3. Agents 使用

- ✅ 提供詳細的業務需求
- ✅ 與 Agent 進行多輪對話精煉設計
- ✅ 驗證 Agent 產出的設計
- ❌ 不要盲目接受 Agent 的第一版設計

## 疑難排解

### MCP 服務器無法啟動

```bash
# 檢查安裝
uvx --version

# 重新安裝
pip install --upgrade mcp-server-time
```

### ArchUnit 驗證失敗

```bash
# 查看詳細錯誤
./gradlew archUnit --info

# 常見問題：
# - Domain 層依賴 Infrastructure → 移除不當依賴
# - 命名不符合規範 → 重新命名
```

### 測試覆蓋率不足

```bash
# 生成覆蓋率報告
./gradlew test jacocoTestReport

# 查看報告
open build/reports/jacoco/test/html/index.html

# 使用 Test Generator Skill 補充測試
/skill test-generator
```

## 詳細文件

完整的技巧映射和實施指南請參閱：
**[Kiro → Claude Code Blueprint](../kiro-to-claude-code-blueprint.md)**

## 版本

- **版本**: 1.0
- **建立日期**: 2025-11-16
- **維護者**: Architecture Team

## 授權

MIT License
