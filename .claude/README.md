# Claude Code 配置（基於 Kiro 原則）

本目錄包含基於 Amazon Kiro 架構原則的 Claude Code 配置。

## 目錄結構

```
.claude/
├── config/
│   └── main-config.json              # 主配置（Kiro 原則、Quality Gates、Safety Nets）
├── skills/
│   ├── idempotent-code-generation.md # Skill: 幂等程式碼生成
│   └── workflow-decomposition.md     # Skill: 工作流分解
├── hooks/
│   └── session-start-kiro-principles.md # Hook: Session 啟動時載入 Kiro 原則
├── commands/
│   └── idempotent-generate.md        # Command: /idempotent-generate
├── agents/
│   ├── domain-model-agent.json       # Sub-Agent: Domain 層專家
│   └── infrastructure-agent.json     # Sub-Agent: Infrastructure 層專家
├── templates/
│   └── aggregate-root.java.template  # Template: Aggregate Root
├── cache/                            # 幂等性快取（自動生成）
├── backups/                          # 安全備份（自動生成）
├── logs/                             # 執行日誌（自動生成）
└── metrics/                          # 監控指標（自動生成）
```

## 快速開始

### 1. 驗證配置

```bash
# 檢查所有配置檔案是否存在
ls -la .claude/config/
ls -la .claude/skills/
ls -la .claude/hooks/
ls -la .claude/agents/
```

### 2. 啟動 Claude Code Session

啟動後，Session Start Hook 會自動執行，載入：
- Kiro Principles
- Steering Rules (17 files)
- Examples (25 files)
- MCP Servers (AWS docs, CDK, Excalidraw)

### 3. 使用 Skills

```bash
# 幂等生成 Aggregate Root
/idempotent-generate aggregate Customer

# 分解複雜任務
/decompose-workflow "實作 Order Management Bounded Context"
```

## Kiro 原則映射

| Kiro 原則 | Claude Code 實現 |
|----------|----------------|
| Idempotency | 輸入雜湊 + 快取 + 確定性 prompt |
| Workflow Decomposition | 任務分析 + 依賴圖 + Sub-Agent 分配 |
| Stateless Handler | Self-contained Skills + 明確參數傳遞 |
| Immutable Input | Read-only 輸入 + 生成新文件 + 備份 |
| Boundary Control | Schema 驗證 + Quality Gates |
| Fail-Safe Pattern | Checkpoint + 回滾 + Dry-run |
| Isolation Pattern | Sub-Agent 隔離 + 超時保護 |

## 完整文檔

詳細的映射說明和使用指南請參考：
- **完整藍圖**: `reports-summaries/architecture-design/kiro-to-claude-code-blueprint.md`
- **Kiro 分析**: `reports-summaries/architecture-design/kiro-ai-coding-analysis-report.md`

## 監控與除錯

### 查看日誌
```bash
cat .claude/logs/session-latest.log
```

### 查看報告
```bash
cat reports-summaries/claude-code-execution/latest-report.md
```

### 查看指標
```bash
cat .claude/metrics/session-latest.json
```

## 維護

- **配置版本**: 1.0.0
- **最後更新**: 2025-11-16
- **維護者**: Development Team
