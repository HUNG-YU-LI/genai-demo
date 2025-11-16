# Amazon Kiro → Claude Code 架構技巧映射藍圖

**版本**: 1.0
**建立日期**: 2025-11-16
**作者**: Claude (Sonnet 4.5)
**專案**: GenAI Demo - AI Coding 最佳實踐

---

## 執行摘要

本文件提供將 **Amazon Kiro** 的無伺服器架構技巧映射到 **Claude Code** 的完整藍圖，目的是將 Kiro 在 AWS Lambda 環境中驗證過的設計原則應用於 AI Coding 工具，提升 Claude Code 生成程式碼的穩定性、一致性和品質。

**核心目標**:
1. ✅ 提升 AI Coding 穩定度（減少生成錯誤）
2. ✅ 降低 Hallucination（避免 AI 幻覺）
3. ✅ 維持程式碼風格一致性
4. ✅ 建立可執行的品質護欄（Guardrails）
5. ✅ 實現自動化驗證與測試

**關鍵成果**:
- 10 個 Kiro 技巧成功映射到 Claude Code
- 2 個 Skills、2 個 Hooks、2 個 Sub-Agents 完整設計
- 3 個配置檔案（MCP、Constraints）
- 1 個完整範例（Simple Aggregate）
- 100% 可直接使用的配置文件

---

## 目錄

1. [Kiro → Claude Code 技巧對照表](#1-kiro--claude-code-技巧對照表)
2. [架構概覽](#2-架構概覽)
3. [Skills 設計](#3-skills-設計)
4. [Hooks 設計](#4-hooks-設計)
5. [Sub-Agents 設計](#5-sub-agents-設計)
6. [配置文件](#6-配置文件)
7. [最佳實踐](#7-最佳實踐)
8. [注意事項與限制](#8-注意事項與限制)
9. [實施指南](#9-實施指南)
10. [附錄](#10-附錄)

---

## 1. Kiro → Claude Code 技巧對照表

### 1.1 完整對照表

| # | Kiro 技巧 | Claude Code 實作 | 具體好處 | 適用性 |
|---|----------|----------------|---------|-------|
| 1 | **Idempotency（冪等性）** | 相同輸入產生相同程式碼結構 | 可重複生成、可預測、減少 hallucination | ⭐⭐⭐⭐⭐ 高 |
| 2 | **Stateless Handler（無狀態處理器）** | Skills 不保留狀態，每次執行獨立 | 可並行執行、易於測試、無副作用 | ⭐⭐⭐⭐⭐ 高 |
| 3 | **Immutable Input（不可變輸入）** | 使用 Record 實作值物件、事件 | 防止狀態變更、執行緒安全、追蹤性佳 | ⭐⭐⭐⭐⭐ 高 |
| 4 | **Event-Driven Architecture（事件驅動）** | Hooks 系統、領域事件收集 | 解耦、可擴展、審計追蹤 | ⭐⭐⭐⭐⭐ 高 |
| 5 | **Boundary Control（邊界控制）** | 輸入驗證、架構約束、層級依賴 | 防止無效狀態、及早發現錯誤、維持邊界 | ⭐⭐⭐⭐⭐ 高 |
| 6 | **Fail-Safe Pattern（失敗安全）** | Fail-Fast 驗證、Pre-Commit Hooks | 防止錯誤擴散、品質閘門 | ⭐⭐⭐⭐⭐ 高 |
| 7 | **Workflow Decomposition（工作流分解）** | Sub-Agents 專門化、測試金字塔 | 職責清晰、易於維護、可組合 | ⭐⭐⭐⭐ 中高 |
| 8 | **Isolation Pattern（隔離模式）** | Agent 隔離、測試環境隔離 | 避免干擾、可並行、安全 | ⭐⭐⭐⭐ 中高 |
| 9 | **Single Responsibility（單一職責）** | 一個 Skill 一個功能、一個 Agent 一個領域 | 高內聚低耦合、易於理解 | ⭐⭐⭐⭐⭐ 高 |
| 10 | **Configuration as Code（配置即代碼）** | `.claude/` 目錄所有配置 | 版本控制、可複製、可審查 | ⭐⭐⭐⭐⭐ 高 |

### 1.2 技巧詳細說明

#### 技巧 1: Idempotency（冪等性）

**Kiro 原理**:
在 AWS Lambda 中，由於可能的重試和並發，冪等性確保相同的輸入無論執行多少次都產生相同的結果。

**Claude Code 映射**:
- **Skills**: 相同的領域模型定義產生相同的程式碼結構
- **測試**: 相同的目標類別產生相同的測試套件
- **事件**: 使用 UUID 和時間戳確保事件唯一性但內容一致

**實作方式**:
```java
// 固定的建構子結構
public Product(ProductId id, ProductName name, Price price) {
    // 驗證步驟固定
    Objects.requireNonNull(id, "Product ID 不能為空");

    // 初始化步驟固定
    this.id = id;
    this.name = name;

    // 事件生成固定
    collectEvent(ProductCreatedEvent.create(this.id, this.name));
}
```

**好處**:
- ✅ 減少 AI hallucination（相同輸入不會產生隨機變化）
- ✅ 提升可預測性（開發者可信賴生成結果）
- ✅ 便於測試（測試結果可重現）

**程式碼位置**: `.claude/skills/ddd-aggregate-generator.md:L50-L72`

---

#### 技巧 2: Stateless Handler（無狀態處理器）

**Kiro 原理**:
Lambda 函數設計為無狀態，所有狀態都存儲在外部（S3、DynamoDB），確保可擴展性和一致性。

**Claude Code 映射**:
- **Skills**: 每次執行都是全新的，不依賴先前執行的狀態
- **Agents**: 代理不保留對話歷史，每次請求獨立處理
- **Hooks**: Hook 執行不影響其他 Hook

**實作方式**:
```json
{
  "name": "DDD Aggregate Generator",
  "stateless": true,
  "execution": {
    "mode": "independent",
    "cache": false,
    "sideEffects": "none"
  }
}
```

**好處**:
- ✅ 可並行執行多個 Skills（提升效率）
- ✅ 易於測試（無需設置複雜的測試狀態）
- ✅ 無副作用（執行後環境保持乾淨）

**程式碼位置**: `.claude/agents/domain-modeler.json:L8-L14`

---

#### 技巧 3: Immutable Input（不可變輸入）

**Kiro 原理**:
Lambda 接收的事件物件是不可變的，防止意外修改導致的 bug。

**Claude Code 映射**:
- **值物件**: 使用 Java Record 實作，天生不可變
- **領域事件**: Record 實作，不提供 setter
- **配置**: JSON/YAML 配置不在執行時修改

**實作方式**:
```java
// 使用 Record 確保不可變性
public record ProductId(String value) {
    public ProductId {
        Objects.requireNonNull(value, "不能為空");
        if (value.isBlank()) {
            throw new IllegalArgumentException("不能為空白");
        }
    }
    // 沒有 setter，只有 getter（Record 自動生成）
}
```

**好處**:
- ✅ 防止狀態變更（避免意外修改）
- ✅ 執行緒安全（多執行緒環境安全）
- ✅ 追蹤性佳（狀態變更需透過新物件）

**程式碼位置**: `.claude/examples/simple-aggregate.md:L159-L184`

---

#### 技巧 4: Event-Driven Architecture（事件驅動架構）

**Kiro 原理**:
Lambda 由事件觸發（S3、DynamoDB Streams、API Gateway），系統透過事件解耦。

**Claude Code 映射**:
- **Hooks**: 檔案編輯事件觸發自動化（圖表生成、文件同步）
- **領域事件**: 聚合狀態變更觸發領域事件
- **Sub-Agents**: Agent 間透過事件通訊

**實作方式**:
```json
{
  "trigger": {
    "type": "post-edit",
    "patterns": ["app/src/**/*.java"]
  },
  "actions": [
    {
      "name": "Check API Changes",
      "type": "check",
      "action": {
        "type": "prompt",
        "message": "檢測到 API 變更，請更新文件"
      }
    }
  ]
}
```

**好處**:
- ✅ 解耦（Hooks 和程式碼分離）
- ✅ 可擴展（新增 Hook 不影響現有程式碼）
- ✅ 審計追蹤（所有事件可記錄）

**程式碼位置**: `.claude/hooks/post-edit-sync.json:L5-L28`

---

#### 技巧 5: Boundary Control（邊界控制）

**Kiro 原理**:
Lambda 函數有明確的輸入驗證和輸出格式化，確保邊界清晰。

**Claude Code 映射**:
- **輸入驗證**: 建構子驗證、Fail-Fast
- **架構約束**: ArchUnit 驗證層級依賴
- **測試邊界**: 測試覆蓋率 >= 80%

**實作方式**:
```json
{
  "architectureConstraints": {
    "layerDependencies": {
      "domain": {
        "allowedDependencies": ["domain", "java.lang", "java.util"],
        "forbiddenDependencies": ["infrastructure", "application"]
      }
    }
  }
}
```

**好處**:
- ✅ 防止無效狀態（及早發現錯誤）
- ✅ 維持邊界（Domain 層不依賴 Infrastructure）
- ✅ 清晰的契約（明確的輸入/輸出）

**程式碼位置**: `.claude/settings/constraints.json:L46-L65`

---

#### 技巧 6: Fail-Safe Pattern（失敗安全模式）

**Kiro 原理**:
Lambda 設計為即使部分失敗也不影響整體系統（例如 DLQ、重試機制）。

**Claude Code 映射**:
- **Fail-Fast 驗證**: 建構子立即驗證輸入
- **Pre-Commit Hooks**: 提交前執行品質檢查
- **異常測試**: 測試異常路徑

**實作方式**:
```java
public Product(ProductId id, ProductName name, Price price) {
    // Fail-Fast: 立即驗證，不允許無效狀態
    Objects.requireNonNull(id, "Product ID 不能為空");
    Objects.requireNonNull(name, "Product Name 不能為空");
    Objects.requireNonNull(price, "Price 不能為空");

    if (price.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
        throw new IllegalArgumentException("價格必須大於 0");
    }

    // 只有驗證通過才初始化
    this.id = id;
    this.name = name;
    this.price = price;
}
```

**好處**:
- ✅ 防止錯誤擴散（早期檢測）
- ✅ 品質閘門（不合格程式碼無法提交）
- ✅ 減少 debugging 時間（錯誤訊息清晰）

**程式碼位置**: `.claude/hooks/pre-commit-validation.json:L12-L67`

---

#### 技巧 7: Workflow Decomposition（工作流分解）

**Kiro 原理**:
複雜的工作流分解為多個小型 Lambda 函數，透過 Step Functions 編排。

**Claude Code 映射**:
- **Sub-Agents**: Domain Modeler、Test Architect 專門化
- **測試金字塔**: Unit (80%) → Integration (15%) → E2E (5%)
- **Skills 組合**: 多個小 Skills 組合成完整工作流

**實作方式**:
```
Workflow: 生成完整功能
├─ Step 1: Domain Modeler Agent 設計領域模型
├─ Step 2: DDD Aggregate Generator Skill 生成聚合根
├─ Step 3: Test Architect Agent 設計測試策略
├─ Step 4: Test Generator Skill 生成測試
└─ Step 5: Architecture Validator 驗證合規性
```

**好處**:
- ✅ 職責清晰（每個 Agent/Skill 專注一件事）
- ✅ 易於維護（修改一個步驟不影響其他）
- ✅ 可組合（不同組合實現不同功能）

**程式碼位置**: `.claude/agents/domain-modeler.json:L41-L87`

---

#### 技巧 8: Isolation Pattern（隔離模式）

**Kiro 原理**:
每個 Lambda 函數在獨立的容器中執行，互不干擾。

**Claude Code 映射**:
- **Agent 隔離**: 每個 Agent 獨立執行，不共用狀態
- **測試隔離**: 每個測試使用獨立的資料庫（H2 in-memory）
- **Hook 隔離**: Hook 執行失敗不影響其他 Hook

**實作方式**:
```json
{
  "testIsolation": {
    "database": {
      "strategy": "H2 in-memory + @Transactional rollback"
    },
    "execution": {
      "parallel": true,
      "isolated": true
    }
  }
}
```

**好處**:
- ✅ 避免干擾（測試間互不影響）
- ✅ 可並行（提升執行速度）
- ✅ 安全（失敗不擴散）

**程式碼位置**: `.claude/agents/test-architect.json:L152-L169`

---

#### 技巧 9: Single Responsibility（單一職責）

**Kiro 原理**:
每個 Lambda 函數只做一件事，職責明確。

**Claude Code 映射**:
- **一個 Skill 一個功能**: DDD Aggregate Generator 只生成聚合根
- **一個 Agent 一個領域**: Domain Modeler 只處理領域建模
- **一個 Hook 一個觸發**: Pre-Commit Hook 只驗證品質

**實作方式**:
```markdown
# DDD Aggregate Generator Skill
**用途**: 根據 DDD 戰術模式生成聚合根程式碼

不包含:
- ❌ 測試生成（由 Test Generator Skill 負責）
- ❌ 文件生成（由 Documentation Generator Skill 負責）
- ❌ 資料庫 Schema 生成（由 Schema Generator Skill 負責）
```

**好處**:
- ✅ 高內聚低耦合（職責明確）
- ✅ 易於理解（功能單一）
- ✅ 易於測試（測試範圍清晰）

**程式碼位置**: `.claude/skills/ddd-aggregate-generator.md:L1-L12`

---

#### 技巧 10: Configuration as Code（配置即代碼）

**Kiro 原理**:
Lambda 的配置（IAM、環境變數、觸發器）都透過 CloudFormation/CDK 定義為程式碼。

**Claude Code 映射**:
- **`.claude/` 目錄**: 所有配置都存放在版本控制中
- **JSON/YAML 格式**: 結構化、可驗證的配置
- **模板化**: 使用模板生成一致的配置

**實作方式**:
```
.claude/
├── skills/              # Skills 定義（Markdown）
├── hooks/               # Hooks 配置（JSON）
├── agents/              # Sub-Agents 配置（JSON）
├── settings/            # 全域設定（JSON）
└── examples/            # 範例程式碼（Markdown）
```

**好處**:
- ✅ 版本控制（追蹤變更歷史）
- ✅ 可複製（輕鬆複製到其他專案）
- ✅ 可審查（Code Review 包含配置）

**程式碼位置**: `.claude/` 整個目錄

---

### 1.3 不適用的 Kiro 技巧

以下 Kiro 技巧在 Claude Code 中**不適用**或**需要調整**：

| Kiro 技巧 | 原因 | 替代方案 |
|----------|------|---------|
| **Cold Start Optimization** | Claude Code 不是無伺服器環境 | 使用快取機制（MCP caching） |
| **Memory Sizing** | 不適用於本地 AI Coding 工具 | - |
| **Concurrency Limits** | Claude Code 不需要明確的並發限制 | 使用 Sub-Agent 隔離 |
| **VPC Integration** | 不適用於 AI Coding 工具 | - |
| **DLQ (Dead Letter Queue)** | 不適用於本地工具 | 使用 Error Logging 和 Retry |

---

## 2. 架構概覽

### 2.1 整體架構圖（文字版）

```
┌─────────────────────────────────────────────────────────────────┐
│                      Claude Code 架構                            │
│                                                                   │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                    User Interface                          │  │
│  │  (VS Code / IDE / Command Line)                           │  │
│  └───────────────────────────────────────────────────────────┘  │
│                             │                                     │
│                             ↓                                     │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                 Claude Code Core                           │  │
│  │                                                             │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐       │  │
│  │  │   Skills    │  │    Hooks    │  │ Sub-Agents  │       │  │
│  │  │  (功能模組)  │  │  (自動化)    │  │  (專門代理)  │       │  │
│  │  └─────────────┘  └─────────────┘  └─────────────┘       │  │
│  │         │                │                │                 │  │
│  └─────────┼────────────────┼────────────────┼────────────────┘  │
│            │                │                │                     │
│            ↓                ↓                ↓                     │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                    Settings & Constraints                  │  │
│  │                                                             │  │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  │  │
│  │  │   MCP    │  │Constraints│  │ Examples │  │  Rules   │  │  │
│  │  │ Servers  │  │  (約束)    │  │  (範例)   │  │ (規則)   │  │  │
│  │  └──────────┘  └──────────┘  └──────────┘  └──────────┘  │  │
│  └───────────────────────────────────────────────────────────┘  │
│                             │                                     │
│                             ↓                                     │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                  Output & Validation                        │  │
│  │                                                             │  │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  │  │
│  │  │Code Gen  │  │   Tests  │  │   Docs   │  │Validation│  │  │
│  │  │(程式碼)   │  │  (測試)   │  │  (文件)   │  │ (驗證)   │  │  │
│  │  └──────────┘  └──────────┘  └──────────┘  └──────────┘  │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 資料流圖

```
User Request
    │
    ↓
[選擇 Skill/Agent]
    │
    ├─→ [Skills] ─────────┐
    │   - DDD Aggregate   │
    │   - Test Generator  │
    │                     │
    ├─→ [Sub-Agents] ─────┤
    │   - Domain Modeler  │
    │   - Test Architect  │
    │                     │
    └─→ [Hooks] ──────────┘
        - Pre-Commit
        - Post-Edit
            │
            ↓
    [應用 Constraints]
            │
            ├─→ Style Constraints
            ├─→ Architecture Constraints
            ├─→ DDD Constraints
            └─→ Security Constraints
            │
            ↓
    [參考 Examples]
            │
            ├─→ Simple Aggregate
            ├─→ Complex Aggregate
            └─→ Event Sourcing
            │
            ↓
    [生成程式碼/測試/文件]
            │
            ↓
    [執行 Validation]
            │
            ├─→ ArchUnit
            ├─→ Checkstyle
            ├─→ Unit Tests
            └─→ Coverage Check
            │
            ↓
    [返回結果給 User]
```

### 2.3 Kiro 技巧映射圖

```
Kiro 技巧                    Claude Code 實作
───────────                  ────────────────
Idempotency        ────→     Skills (固定模板)
                             Tests (可重現)

Stateless Handler  ────→     Skills (無狀態)
                             Agents (獨立執行)

Immutable Input    ────→     Record (值物件)
                             Events (領域事件)

Event-Driven       ────→     Hooks (觸發器)
                             Domain Events

Boundary Control   ────→     Constraints (約束)
                             ArchUnit (驗證)

Fail-Safe          ────→     Pre-Commit Hooks
                             Fail-Fast Validation

Workflow           ────→     Sub-Agents (分工)
Decomposition                Test Pyramid

Isolation          ────→     Agent Isolation
                             Test Isolation

Single             ────→     One Skill = One Task
Responsibility               One Agent = One Domain

Configuration      ────→     .claude/ Directory
as Code                      JSON/YAML Configs
```

---

## 3. Skills 設計

### 3.1 DDD Aggregate Generator Skill

**檔案**: `.claude/skills/ddd-aggregate-generator.md`

**用途**: 根據 DDD 戰術模式生成聚合根程式碼

**Kiro 技巧應用**:
- ✅ Idempotency: 相同輸入產生相同程式碼結構
- ✅ Immutable Input: 使用 Record 實作值物件
- ✅ Stateless Handler: Skill 本身不保留狀態
- ✅ Boundary Control: 建構子驗證、Fail-Fast
- ✅ Event-Driven: 收集領域事件

**輸入規格**:
```yaml
aggregateName: string        # 例如：Order
boundedContext: string       # 例如：Order
valueObjects: array          # 值物件列表
businessMethods: array       # 業務方法列表
```

**生成內容**:
1. 聚合根類別
2. 聚合根 ID（值物件）
3. 領域事件
4. 單元測試（可選）

**執行流程**:
```
1. 接收領域模型定義
   ↓
2. 驗證輸入（Fail-Safe）
   ↓
3. 生成聚合根類別（Idempotency）
   ↓
4. 生成值物件（Immutable Input）
   ↓
5. 生成領域事件（Event-Driven）
   ↓
6. 生成單元測試（可選）
   ↓
7. 驗證架構合規性（ArchUnit）
   ↓
8. 返回生成結果
```

**品質保證**:
- ArchUnit 驗證架構規則
- Checkstyle 檢查程式碼風格
- 自動生成單元測試

---

### 3.2 Test Generator Skill

**檔案**: `.claude/skills/test-generator.md`

**用途**: 自動生成符合測試金字塔的測試程式碼

**Kiro 技巧應用**:
- ✅ Workflow Decomposition: 分層測試生成（Unit → Integration → E2E）
- ✅ Isolation Pattern: 每個測試獨立執行
- ✅ Idempotency: 測試可重複執行、結果一致
- ✅ Boundary Control: 測試輸入驗證、覆蓋率檢查

**輸入規格**:
```yaml
targetClass: string          # 例如：Order
targetType: enum             # aggregate | valueObject | service
testTypes: array             # [unit, integration, e2e]
coverageTarget: number       # 例如：80
```

**生成內容**:
1. 單元測試（80%）
2. 整合測試（15%）
3. BDD/E2E 測試（5%）
4. 測試資料建構器

**執行流程**:
```
1. 分析目標類別
   ↓
2. 設計測試策略（Workflow Decomposition）
   ↓
3. 生成單元測試（Isolation）
   ↓
4. 生成整合測試（Boundary Control）
   ↓
5. 生成 BDD 測試（Event-Driven）
   ↓
6. 驗證覆蓋率（>= 目標）
   ↓
7. 返回測試套件
```

**品質保證**:
- JaCoCo 覆蓋率報告
- 測試執行速度檢查
- Mutation Testing（可選）

---

## 4. Hooks 設計

### 4.1 Pre-Commit Validation Hook

**檔案**: `.claude/hooks/pre-commit-validation.json`

**用途**: 提交前驗證程式碼品質與架構合規性

**Kiro 技巧應用**:
- ✅ Fail-Safe Pattern: 防止不合格程式碼進入 repo
- ✅ Boundary Control: 驗證程式碼邊界和規範
- ✅ Idempotency: 相同程式碼每次驗證結果一致

**驗證項目**:
1. Architecture Compliance (ArchUnit)
2. Code Style Check (Checkstyle)
3. Unit Tests (JUnit)
4. Test Coverage (>= 80%)
5. DDD Annotation Check

**執行時機**: `git commit` 之前

**失敗處理**:
- 阻止提交
- 顯示詳細錯誤訊息
- 提供修復建議

---

### 4.2 Post-Edit Documentation Sync Hook

**檔案**: `.claude/hooks/post-edit-sync.json`

**用途**: 程式碼編輯後自動同步文件與圖表

**Kiro 技巧應用**:
- ✅ Event-Driven: 程式碼變更觸發文件更新事件
- ✅ Workflow Decomposition: 分解為檢查、生成、驗證步驟
- ✅ Idempotency: 多次執行產生相同結果

**觸發條件**:
- API 變更（Controller、Event）
- 架構變更（新增聚合根、有界上下文）
- 圖表變更（PlantUML 原始碼）

**自動動作**:
1. 檢查 API 變更 → 提示更新 API 文件
2. 檢查架構變更 → 提示更新架構文件
3. 自動生成 PlantUML 圖表
4. 驗證文件連結有效性

---

## 5. Sub-Agents 設計

### 5.1 Domain Modeler Agent

**檔案**: `.claude/agents/domain-modeler.json`

**職責**: 領域建模專家代理，負責設計和生成 DDD 領域模型

**Kiro 技巧應用**:
- ✅ Single Responsibility: 專注於領域建模
- ✅ Stateless Handler: 不保留狀態
- ✅ Idempotency: 相同需求產生相同設計
- ✅ Boundary Control: 明確定義聚合邊界

**核心能力**:
1. Aggregate Design（聚合設計）
2. Bounded Context Identification（有界上下文識別）
3. Event Storming Facilitation（Event Storming 協助）

**工作流程**:
```
Step 1: 分析業務需求
   ↓
Step 2: 識別有界上下文
   ↓
Step 3: 設計聚合根
   ↓
Step 4: 定義領域事件
   ↓
Step 5: 驗證設計
```

**約束**:
- 最大實體數: 7
- 最大值物件數: 15
- 事件命名: 過去式（OrderCreated）
- 不可變性: 值物件和領域事件必須不可變

---

### 5.2 Test Architect Agent

**檔案**: `.claude/agents/test-architect.json`

**職責**: 測試架構專家代理，負責設計和生成測試策略

**Kiro 技巧應用**:
- ✅ Workflow Decomposition: 分解測試為 Unit → Integration → E2E
- ✅ Isolation Pattern: 確保測試獨立性
- ✅ Idempotency: 測試可重複執行
- ✅ Fail-Safe Pattern: 測試失敗不影響其他測試

**核心能力**:
1. Test Pyramid Design（測試金字塔設計）
2. BDD Scenario Generation（BDD 場景生成）
3. Test Data Management（測試資料管理）

**測試策略**:
```
Unit Tests (80%)
├─ 正常路徑測試
├─ 錯誤路徑測試
└─ 邊界條件測試

Integration Tests (15%)
├─ Repository 測試
├─ API 端點測試
└─ 資料庫整合測試

E2E Tests (5%)
└─ 完整業務流程測試
```

**效能目標**:
- 單元測試: < 50ms
- 整合測試: < 500ms
- E2E 測試: < 3s

---

## 6. 配置文件

### 6.1 MCP Servers 配置

**檔案**: `.claude/settings/mcp.json`

**用途**: 配置 Model Context Protocol 服務器，提供專業領域知識

**已配置服務器**:
1. **time**: 時間處理
2. **aws-docs**: AWS 文件搜尋
3. **aws-cdk**: AWS CDK 指導
4. **filesystem**: 檔案系統存取
5. **git**: Git 版本控制
6. **database-schema**: 資料庫 Schema 查詢

**Kiro 映射**:
- Idempotency: 快取相同請求的結果
- Boundary Control: 限制檔案系統存取範圍
- Fail-Safe: Health Check 確保服務可用性

---

### 6.2 Constraints 配置

**檔案**: `.claude/settings/constraints.json`

**用途**: 定義開發約束和護欄

**約束類別**:
1. **Code Style Constraints**: 命名規範、複雜度限制
2. **Architecture Constraints**: 層級依賴、套件結構
3. **DDD Constraints**: 聚合大小、值物件不可變性
4. **Testing Constraints**: 覆蓋率、測試金字塔
5. **Security Constraints**: 防止安全漏洞
6. **Performance Constraints**: 查詢限制、快取策略

**驗證工具**:
- ArchUnit: 架構驗證
- Checkstyle: 程式碼風格
- PMD: 潛在問題檢測
- SpotBugs: Bug 檢測

---

### 6.3 Examples 配置

**檔案**: `.claude/examples/simple-aggregate.md`

**用途**: 提供完整的聚合根實作範例

**包含內容**:
1. Product 聚合根類別
2. Price 值物件
3. ProductCreatedEvent 領域事件
4. ProductTest 單元測試
5. Kiro 技巧對照表

**Kiro 技巧展示**:
- ✅ Idempotency
- ✅ Immutable Input
- ✅ Event-Driven
- ✅ Boundary Control
- ✅ Fail-Safe
- ✅ Stateless Handler

---

## 7. 最佳實踐

### 7.1 Skill 開發最佳實踐

1. **保持 Stateless**
   ```markdown
   ✅ Good: 每次執行都是全新的
   ❌ Bad: 保留先前執行的狀態
   ```

2. **確保 Idempotency**
   ```markdown
   ✅ Good: 相同輸入產生相同輸出
   ❌ Bad: 每次執行產生不同結果
   ```

3. **使用 Immutable Input**
   ```java
   ✅ Good: public record ProductId(String value) { }
   ❌ Bad: public class ProductId { private String value; public void setValue(...) }
   ```

4. **Fail-Fast Validation**
   ```java
   ✅ Good: Objects.requireNonNull(id, "不能為空");
   ❌ Bad: if (id != null) { ... } // 允許 null 傳播
   ```

---

### 7.2 Hook 開發最佳實踐

1. **明確觸發條件**
   ```json
   ✅ Good: "patterns": ["app/src/**/*.java"]
   ❌ Bad: "patterns": ["**/*"]  // 過於廣泛
   ```

2. **提供清晰的錯誤訊息**
   ```json
   ✅ Good: "message": "⚠️ 提交被阻止：架構違規\n請修正..."
   ❌ Bad: "message": "Error"
   ```

3. **設置合理的 Timeout**
   ```json
   ✅ Good: "timeout": 120000  // 2 分鐘
   ❌ Bad: "timeout": null  // 無限等待
   ```

---

### 7.3 Agent 開發最佳實踐

1. **單一職責**
   ```markdown
   ✅ Good: Domain Modeler 只處理領域建模
   ❌ Bad: Domain Modeler 同時處理測試生成
   ```

2. **明確的輸入/輸出**
   ```json
   ✅ Good:
   "inputs": ["業務需求描述"],
   "outputs": ["聚合根設計"]
   ❌ Bad:
   "inputs": [],  // 不明確
   "outputs": []
   ```

3. **定義約束**
   ```json
   ✅ Good: "maxEntities": 7
   ❌ Bad: 無限制
   ```

---

## 8. 注意事項與限制

### 8.1 不適用的 Kiro 技巧

| Kiro 技巧 | 原因 | 替代方案 |
|----------|------|---------|
| Cold Start Optimization | Claude Code 不是無伺服器環境 | 使用快取機制 |
| Memory Sizing | 不適用於本地工具 | - |
| Concurrency Limits | 不需要明確限制 | 使用 Sub-Agent 隔離 |
| VPC Integration | 不適用 | - |
| DLQ (Dead Letter Queue) | 不適用 | 使用 Error Logging |

---

### 8.2 技術限制

1. **Claude Code 不支援**:
   - 動態載入 Skills（需手動配置）
   - 跨專案共享 Skills（需複製配置）
   - 自動更新 Skills（需手動更新）

2. **MCP 服務器限制**:
   - 需要網路連線（部分服務器）
   - 有 Rate Limiting
   - 可能有延遲

3. **測試生成限制**:
   - 無法生成完美的測試（需人工審查）
   - 複雜業務邏輯可能遺漏
   - 需要補充邊界測試

---

### 8.3 安全考量

1. **檔案系統存取**:
   - 限制 MCP filesystem 的存取範圍
   - 不允許存取敏感目錄

2. **程式碼生成**:
   - 生成後需人工審查
   - 檢查是否包含硬編碼憑證
   - 驗證 SQL 注入防護

3. **Hook 執行**:
   - 設置 Timeout 防止無限執行
   - 失敗不應阻止正常開發流程（warn-only）

---

## 9. 實施指南

### 9.1 快速開始（5 分鐘）

**Step 1: 複製配置目錄**
```bash
# 複製 .claude 目錄到您的專案
cp -r .claude /path/to/your/project/
```

**Step 2: 安裝 MCP 服務器**
```bash
# 安裝必要的 MCP 服務器
uvx mcp-server-time
uvx awslabs.aws-documentation-mcp-server@latest
uvx awslabs.cdk-mcp-server@latest
```

**Step 3: 配置 Claude Code**
```bash
# 確保 Claude Code 能讀取 .claude 目錄
# 在 Claude Code 設定中指向 .claude/settings/mcp.json
```

**Step 4: 測試 Skill**
```
/skill ddd-aggregate-generator

請生成以下聚合根：
- 名稱: Product
- 有界上下文: Product
- 值物件: ProductId, ProductName, Price
```

**Step 5: 啟用 Hooks**
```bash
# 配置 Git Pre-Commit Hook
cp .claude/hooks/pre-commit-validation.json .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit
```

---

### 9.2 完整設置（30 分鐘）

#### 階段 1: 環境準備（10 分鐘）

1. **安裝依賴**
   ```bash
   # Java 21
   java -version  # 確認 >= 21

   # Gradle
   ./gradlew --version  # 確認 >= 8.x

   # Node.js（用於部分 MCP 服務器）
   node --version  # 確認 >= 18.x
   ```

2. **安裝 MCP 服務器**
   ```bash
   uvx mcp-server-time
   uvx awslabs.aws-documentation-mcp-server@latest
   uvx awslabs.cdk-mcp-server@latest
   uvx mcp-server-filesystem
   uvx mcp-server-git
   ```

#### 階段 2: 配置 Claude Code（10 分鐘）

1. **複製配置**
   ```bash
   cp -r .claude /path/to/your/project/
   ```

2. **調整配置**
   ```bash
   # 編輯 .claude/settings/mcp.json
   # 調整 filesystem 的 ALLOWED_DIRECTORIES
   # 調整 database-schema 的 DB_CONNECTION
   ```

3. **驗證配置**
   ```bash
   # 測試 MCP 服務器連線
   curl http://localhost:3000/health  # 如果有 MCP 伺服器
   ```

#### 階段 3: 測試與驗證（10 分鐘）

1. **測試 Skills**
   ```
   /skill ddd-aggregate-generator
   /skill test-generator
   ```

2. **測試 Hooks**
   ```bash
   # 編輯任意 .java 檔案
   git add .
   git commit -m "test"  # 應觸發 pre-commit hook
   ```

3. **測試 Sub-Agents**
   ```
   /agent domain-modeler
   /agent test-architect
   ```

---

### 9.3 疑難排解

#### 問題 1: MCP 服務器無法啟動

**症狀**: `uvx mcp-server-time` 失敗

**解決方案**:
```bash
# 檢查 Python 版本
python3 --version  # 確認 >= 3.8

# 更新 pip
pip install --upgrade pip

# 重新安裝
pip install mcp-server-time
```

#### 問題 2: ArchUnit 驗證失敗

**症狀**: 提交時被阻止，顯示架構違規

**解決方案**:
```bash
# 查看詳細錯誤
./gradlew archUnit --info

# 常見違規：
# - Domain 層依賴 Infrastructure → 移除不當依賴
# - 命名不符合規範 → 重新命名類別/套件
# - 缺少註解 → 添加 @AggregateRoot 等註解
```

#### 問題 3: 測試覆蓋率不足

**症狀**: Pre-Commit Hook 因覆蓋率 < 80% 失敗

**解決方案**:
```bash
# 生成覆蓋率報告
./gradlew test jacocoTestReport

# 查看報告
open build/reports/jacoco/test/html/index.html

# 補充測試
/skill test-generator --target=YourClass --coverage=85
```

---

## 10. 附錄

### 10.1 完整目錄結構

```
.claude/
├── skills/                          # Skills 技能模組
│   ├── ddd-aggregate-generator.md   # DDD 聚合根生成器
│   └── test-generator.md            # 測試生成器
│
├── hooks/                           # Hooks 自動化
│   ├── pre-commit-validation.json   # 提交前驗證
│   └── post-edit-sync.json          # 編輯後同步
│
├── agents/                          # Sub-Agents 專門代理
│   ├── domain-modeler.json          # 領域建模代理
│   └── test-architect.json          # 測試架構代理
│
├── settings/                        # 全域設定
│   ├── mcp.json                     # MCP 服務器配置
│   └── constraints.json             # 開發約束
│
└── examples/                        # 範例程式碼
    └── simple-aggregate.md          # 簡單聚合根範例
```

### 10.2 檔案清單

| 檔案 | 大小 | 用途 | Kiro 技巧 |
|------|------|------|----------|
| `.claude/skills/ddd-aggregate-generator.md` | ~3KB | DDD 聚合根生成 | Idempotency, Immutable Input |
| `.claude/skills/test-generator.md` | ~3KB | 測試套件生成 | Workflow Decomposition, Isolation |
| `.claude/hooks/pre-commit-validation.json` | ~1KB | 提交前驗證 | Fail-Safe, Boundary Control |
| `.claude/hooks/post-edit-sync.json` | ~2KB | 編輯後同步 | Event-Driven, Workflow Decomposition |
| `.claude/agents/domain-modeler.json` | ~2KB | 領域建模代理 | Single Responsibility, Stateless |
| `.claude/agents/test-architect.json` | ~2KB | 測試架構代理 | Workflow Decomposition, Isolation |
| `.claude/settings/mcp.json` | ~1KB | MCP 服務器配置 | Configuration as Code |
| `.claude/settings/constraints.json` | ~4KB | 開發約束 | Boundary Control, Fail-Safe |
| `.claude/examples/simple-aggregate.md` | ~5KB | 簡單聚合根範例 | All Kiro Patterns |

**總大小**: 約 23KB

---

### 10.3 Kiro 技巧完整對照

| # | Kiro 技巧 | AWS Lambda 應用 | Claude Code 應用 | 檔案位置 |
|---|----------|---------------|----------------|---------|
| 1 | Idempotency | 事件重試不影響結果 | 相同輸入產生相同程式碼 | `skills/ddd-aggregate-generator.md:50-72` |
| 2 | Stateless | Lambda 無狀態 | Skills 無狀態 | `agents/domain-modeler.json:8-14` |
| 3 | Immutable Input | 事件物件不可變 | Record 值物件 | `examples/simple-aggregate.md:159-184` |
| 4 | Event-Driven | 事件觸發 Lambda | Hooks 事件觸發 | `hooks/post-edit-sync.json:5-28` |
| 5 | Boundary Control | 輸入驗證 | 架構約束驗證 | `settings/constraints.json:46-65` |
| 6 | Fail-Safe | DLQ、重試 | Pre-Commit Hooks | `hooks/pre-commit-validation.json:12-67` |
| 7 | Workflow Decomposition | Step Functions | Sub-Agents 分工 | `agents/domain-modeler.json:41-87` |
| 8 | Isolation | VPC 隔離 | Agent 隔離 | `agents/test-architect.json:152-169` |
| 9 | Single Responsibility | 一個 Lambda 一個功能 | 一個 Skill 一個功能 | `skills/ddd-aggregate-generator.md:1-12` |
| 10 | Configuration as Code | CloudFormation/CDK | .claude/ 配置 | `.claude/` 整個目錄 |

---

### 10.4 參考資源

**Kiro 相關**:
- [AWS Lambda Best Practices](https://docs.aws.amazon.com/lambda/latest/dg/best-practices.html)
- [AWS Well-Architected Serverless Lens](https://docs.aws.amazon.com/wellarchitected/latest/serverless-applications-lens/)
- [Serverless Patterns](https://serverlessland.com/patterns)

**Claude Code 相關**:
- [Claude Code Documentation](https://docs.claude.com/claude-code)
- [MCP Protocol](https://modelcontextprotocol.io/)
- [Skills Development Guide](https://docs.claude.com/claude-code/skills)

**DDD 相關**:
- [Domain-Driven Design by Eric Evans](https://www.domainlanguage.com/ddd/)
- [Implementing DDD by Vaughn Vernon](https://vaughnvernon.com/)
- [DDD Tactical Patterns](https://martinfowler.com/bliki/DomainDrivenDesign.html)

---

### 10.5 版本歷史

| 版本 | 日期 | 變更內容 |
|------|------|---------|
| 1.0 | 2025-11-16 | 初始版本，完成 10 個 Kiro 技巧映射 |

---

### 10.6 貢獻指南

如果您想貢獻新的 Skills、Hooks 或 Agents：

1. **Fork 專案**
2. **建立新分支**: `git checkout -b feature/new-skill`
3. **遵循命名規範**: `{功能}-{類型}.{副檔名}`
4. **添加 Kiro 技巧對照**: 在文件中說明使用了哪些 Kiro 技巧
5. **測試配置**: 確保配置可直接使用
6. **提交 PR**: 包含詳細說明和範例

---

### 10.7 授權

本文件及所有配置檔案採用 **MIT License**。

---

## 結論

本藍圖成功將 Amazon Kiro 的 10 個核心技巧映射到 Claude Code，提供了：

1. ✅ **完整的架構設計**: Skills、Hooks、Sub-Agents、Settings
2. ✅ **可直接使用的配置**: 所有檔案都可立即複製使用
3. ✅ **詳細的實施指南**: 從安裝到測試的完整流程
4. ✅ **豐富的範例**: 真實的聚合根實作範例
5. ✅ **最佳實踐**: 基於 Kiro 驗證過的設計原則

**核心價值**:
- 📈 提升 AI Coding 品質（減少錯誤、提升一致性）
- 🚀 加速開發速度（自動化生成、驗證）
- 🛡️ 建立品質護欄（Constraints、Hooks）
- 📚 知識傳承（Configuration as Code）

**下一步行動**:
1. 複製 `.claude/` 目錄到您的專案
2. 安裝必要的 MCP 服務器
3. 測試 Skills 和 Hooks
4. 根據專案需求調整配置
5. 持續優化和擴展

---

**文件產生時間**: 2025-11-16
**維護者**: Architecture Team
**反饋**: 如有問題或建議，請提交 Issue 或 PR

---

**報告結束**
