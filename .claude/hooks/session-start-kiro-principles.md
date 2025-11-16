# Session Start Hook: Kiro Principles Enforcement

**Purpose**: 在每次 Claude Code session 開始時，自動載入 Kiro 原則和約束

## Hook 觸發時機

- Session 啟動時
- Workspace 切換時
- 重新連接時

## 執行內容

### 1. 載入 Kiro Steering Rules

```markdown
# 載入 Kiro Steering Rules

以下是本專案的核心開發原則（基於 Amazon Kiro 架構）：

## 核心原則

### 1. Idempotency（幂等性）
✅ **必須遵守**:
- 相同輸入必須產生相同輸出
- 所有操作可安全重複執行
- 使用確定性演算法
- 輸入使用雜湊值追蹤

❌ **禁止行為**:
- 依賴隨機數（除非有固定 seed）
- 依賴當前時間（使用參數化時間）
- 依賴外部可變狀態

### 2. Workflow Decomposition（工作流分解）
✅ **必須遵守**:
- 將複雜任務分解為 < 10 個步驟
- 每個步驟有明確的輸入/輸出
- 步驟之間的依賴關係明確
- 可並行的步驟標記為 parallel

❌ **禁止行為**:
- 單一巨大的 monolithic 任務
- 隱式依賴（未聲明的前置條件）
- 循環依賴

### 3. Stateless Handler（無狀態處理）
✅ **必須遵守**:
- Skill 不依賴 session 狀態
- 所有參數透過輸入傳遞
- 輸出完全由輸入決定
- 無全域變數依賴

❌ **禁止行為**:
- 使用全域可變狀態
- 依賴 session-specific 資料
- 依賴執行順序

### 4. Immutable Input（不可變輸入）
✅ **必須遵守**:
- 永遠不修改輸入文件
- 生成新文件而非編輯現有文件
- 保留原始 artifacts
- 使用版本控制

❌ **禁止行為**:
- 直接修改輸入文件
- 覆蓋原始資料
- 刪除源文件

### 5. Boundary Control（邊界控制）
✅ **必須遵守**:
- 明確定義輸入 schema
- 驗證所有輸入
- 明確定義輸出格式
- 錯誤處理邊界清晰

❌ **禁止行為**:
- 接受任意格式輸入
- 返回不確定格式輸出
- 錯誤訊息洩漏內部細節

## 程式碼生成約束

### DDD Tactical Patterns
- 參考：`.kiro/steering/ddd-tactical-patterns.md`
- Aggregate Root 必須繼承 `AggregateRoot<ID>`
- Value Object 使用 `record`
- Domain Event 必須 immutable

### Architecture Constraints
- 參考：`.kiro/steering/architecture-constraints.md`
- Domain 層不得依賴 Infrastructure
- 使用 Hexagonal Architecture
- Ports and Adapters pattern

### Code Quality Standards
- 參考：`.kiro/steering/code-review-standards.md`
- 80%+ test coverage
- No `return null;` (use Optional or throw exception)
- No commented-out code > 10 lines

## 自動載入資源

以下資源已自動載入到 context：

1. **Steering Rules** (17 files):
   - core-principles.md
   - ddd-tactical-patterns.md
   - design-principles.md
   - development-standards.md
   - rozanski-woods-architecture-methodology.md
   - ... (完整列表見 `.kiro/steering/README.md`)

2. **Examples** (25 files):
   - DDD Patterns examples
   - Design Patterns examples
   - Testing examples
   - XP Practices examples

3. **Kiro Configuration**:
   - MCP servers: AWS docs, CDK, Excalidraw
   - Diagram sync rules
   - Quality gates

## 質量檢查清單

在生成任何程式碼前，請確認：

- [ ] 輸入已驗證（schema validation）
- [ ] 選擇正確的模板（參考 Examples）
- [ ] 遵循 Steering Rules
- [ ] 生成的程式碼符合 Architecture Constraints
- [ ] 包含適當的測試
- [ ] 通過 linting（Checkstyle, PMD, ArchUnit）
- [ ] 文檔同步（如果修改 API 或 Domain Model）

## 快速參考

### 生成 Aggregate Root
```bash
# 使用 idempotent-code-generation skill
/idempotent-generate aggregate <name>
```

### 分解複雜任務
```bash
# 使用 workflow-decomposition skill
/decompose-workflow "<task description>"
```

### 驗證架構合規性
```bash
./gradlew archUnit
```

### 檢查程式碼品質
```bash
./gradlew pmdMain checkstyleMain
```

## Session 設定

- **模式**: Kiro-compliant AI Coding
- **品質標準**: High（80%+ coverage, all linting rules enforced）
- **安全網**: Enabled（backup before edit, validation gates）
- **Observability**: Enabled（trace execution, generate reports）

---

**Kiro Principles 已載入並啟用**

現在你可以開始編碼。所有生成的程式碼將自動遵循上述原則。

如果你不確定如何實作某個功能，請：
1. 查閱 `.kiro/steering/` 相關規則
2. 參考 `.kiro/examples/` 範例程式碼
3. 使用 `/decompose-workflow` 分解任務
4. 使用 `/idempotent-generate` 生成程式碼
```

### 2. 設定專案上下文

```markdown
## 專案資訊

- **名稱**: Enterprise E-Commerce Platform
- **架構**: DDD + Hexagonal Architecture + Event-Driven
- **技術棧**: Spring Boot 3.3.13, Java 21, PostgreSQL, Redis, Kafka
- **Bounded Contexts**: 13 個（Customer, Order, Product, Payment, Inventory, etc.）
- **測試策略**: Test Pyramid (80% unit, 15% integration, 5% E2E)

## 當前專案狀態

- **主要分支**: `main`
- **工作分支**: 自動生成（`claude/...`）
- **建構工具**: Gradle 8.x
- **CI/CD**: GitHub Actions + ArgoCD

## 快速命令

```bash
# 執行所有測試
./gradlew test

# 執行 BDD 測試
./gradlew cucumber

# 檢查架構合規性
./gradlew archUnit

# 啟動應用
./gradlew :app:bootRun

# 生成圖表
./scripts/generate-diagrams.sh

# 驗證文檔
./scripts/validate-diagrams.sh
```

## 環境配置

- **Local**: H2 database, in-memory cache
- **Staging**: Aurora PostgreSQL, ElastiCache Redis, MSK Kafka
- **Production**: Multi-AZ Aurora, Redis Cluster, MSK Cluster
```

### 3. 啟用監控

```markdown
## Execution Monitoring

Session 執行將被追蹤：
- 所有工具呼叫記錄到 `.claude/logs/session-{timestamp}.log`
- 執行報告生成到 `reports-summaries/claude-code-execution/`
- 指標收集到 `.claude/metrics/session-{timestamp}.json`

監控指標：
- Tool call count
- Execution time
- Error rate
- Validation pass rate
- Code quality score
```

## 執行

此 hook 將在每次 session 開始時自動執行，無需手動觸發。

## 驗證

檢查 hook 是否正常運作：
```bash
# 檢查 log
cat .claude/logs/session-latest.log | grep "Kiro Principles loaded"

# 輸出應包含：
# [INFO] Kiro Principles loaded successfully
# [INFO] Steering Rules: 17 files
# [INFO] Examples: 25 files
# [INFO] MCP Servers: 4 active
```

## 配置

```json
{
  "hook": "session-start-kiro-principles",
  "enabled": true,
  "priority": 1,
  "parameters": {
    "load_steering_rules": true,
    "load_examples": true,
    "enable_monitoring": true,
    "strict_mode": true
  }
}
```
