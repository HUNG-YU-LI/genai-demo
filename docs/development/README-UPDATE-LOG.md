# README.md 更新日誌

## 2025-01-20: 新增 Make Commands 文件

### 變更內容

在根目錄的 `README.md` 檔案中新增了完整的 Make commands 文件。

### 新增的章節

#### 1. Development Commands (在 Quick Start 中)

- 常用 make commands 的快速參考
- 依類別組織 (Diagrams, Development Setup, Pre-commit)
- 包含實用範例

#### 2. Development Workflow (新章節)

- **Make Commands Reference**: 所有指令的詳細說明
  - Diagram Management commands
  - Development Setup commands
  - Pre-commit Workflow
  - Maintenance Commands

- **Git Hooks**: 如何設定和使用 Git hooks
  - Pre-commit hook
  - Commit message hook
  - Pre-push hook
  - Commit message 格式指南
  - 如何在必要時繞過 hooks

- **Automated Validation**: CI/CD 整合
  - GitHub Actions workflow
  - 自動驗證的項目

### README 中的位置

新內容插入在:

- **之前**: "Quick Start" 章節
- **之後**: "Testing Strategy" 章節

### 快速參考

使用者現在可以找到:

```bash
# In Quick Start section
make help           # See all commands
make dev-setup      # First-time setup
make pre-commit     # Before committing

# In Development Workflow section
# Detailed explanations of:
# - All make commands
# - Git hooks setup
# - Commit message format
# - CI/CD validation
```

### 優點

1. **可發現性**: 開發人員可以輕鬆找到可用的指令
2. **新進人員引導**: 新團隊成員有清楚的設定說明
3. **一致性**: 團隊之間的標準化工作流程
4. **文件**: 所有指令都記錄在一個地方

### 相關檔案

- `README.md` - 已更新 make commands 文件
- `Makefile` - 包含所有指令實作
- `scripts/setup-git-hooks.sh` - Git hooks 設定腳本
- `.github/workflows/validate-documentation.yml` - CI/CD 驗證

---

**更新者**: Development Team
**日期**: 2025-01-20
**相關**: Hooks Cleanup Initiative
