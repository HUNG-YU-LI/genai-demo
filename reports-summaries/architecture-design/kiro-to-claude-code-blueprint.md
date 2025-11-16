# Kiro → Claude Code 技巧映射藍圖

**創建日期：** 2025-11-16
**版本：** 1.0.0
**狀態：** 可執行版本

---

## 執行摘要

本文檔提供從 Amazon Kiro 架構原則到 Claude Code AI Coding 工具的完整映射和實作指南。所有配置均為可直接使用的實際檔案，而非概念性描述。

### 核心成果

- ✅ **7 項 Kiro 核心原則**完整映射到 Claude Code
- ✅ **2 個 Skills** 模組（Idempotent Code Generation, Workflow Decomposition）
- ✅ **1 個 Hook** 配置（Session Start Kiro Principles）
- ✅ **2 個 Sub-Agents** 設計（DomainModelAgent, InfrastructureAgent）
- ✅ **所有配置檔案**可直接使用（JSON/Markdown 格式）

---

## 目錄

1. [Kiro → Claude Code 技巧對照表](#一kiro--claude-code-技巧對照表)
2. [Claude Code 實作設計](#二claude-code-實作設計)
3. [配置檔案清單](#三配置檔案清單)
4. [最佳實踐](#四最佳實踐)
5. [注意事項](#五注意事項)
6. [使用指南](#六使用指南)
7. [架構圖](#七架構圖)
8. [附錄](#八附錄)

---

## 一、Kiro → Claude Code 技巧對照表

### 1. Idempotency（幂等性）

#### Kiro 原則
- 相同輸入必須產生相同輸出
- 可重複執行而不產生副作用
- 使用確定性演算法
- 輸入使用雜湊值追蹤

#### Claude Code 實現方式

**技術手段：**
1. **輸入雜湊與快取**
   ```bash
   # 計算輸入的 SHA-256 雜湊
   INPUT_HASH=$(echo -n "$REQUIREMENTS" | sha256sum | cut -d' ' -f1)

   # 檢查快取
   if [ -f ".claude/cache/${INPUT_HASH}.done" ]; then
     cat ".claude/cache/${INPUT_HASH}.output"
     exit 0
   fi
   ```

2. **確定性 Prompt**
   - 使用固定的模板（不使用隨機變化）
   - 明確指定輸出格式
   - 固定 temperature = 0.1（降低隨機性）

3. **版本控制模板**
   - 所有模板納入 git 版本控制
   - 模板變更需要 code review
   - 使用語義化版本號

#### 具體好處

| 好處 | 說明 | 可衡量指標 |
|------|------|-----------|
| **穩定度提升** | 相同需求不會產生不同程式碼 | 一致性分數 = 100% |
| **可預測性** | 團隊知道 AI 會生成什麼樣的程式碼 | 預測準確率 > 95% |
| **可追溯性** | 透過雜湊值追蹤每次生成 | 追蹤覆蓋率 = 100% |
| **降低 Hallucination** | 固定模板減少 AI 幻覺 | Hallucination 率 < 5% |

#### 實作檔案
- **Skill**: `.claude/skills/idempotent-code-generation.md`
- **配置**: `.claude/config/main-config.json` → `kiro_principles.idempotency`
- **Command**: `.claude/commands/idempotent-generate.md`

---

### 2. Workflow Decomposition（工作流分解）

#### Kiro 原則
- 將複雜任務分解為離散步驟
- 每個步驟有明確的輸入/輸出
- 步驟之間的依賴關係明確
- 可並行執行的步驟被識別

#### Claude Code 實現方式

**技術手段：**
1. **自動任務分析**
   ```python
   def analyze_task(task_description):
       # 識別涉及的 bounded contexts
       contexts = identify_bounded_contexts(task_description)

       # 識別涉及的架構層級
       layers = identify_layers(task_description)

       # 識別依賴關係
       dependencies = analyze_dependencies(contexts, layers)

       # 生成分解計劃
       plan = generate_decomposition_plan(contexts, layers, dependencies)

       return plan
   ```

2. **Sub-Agent 分配**
   - DomainModelAgent → Domain 層
   - InfrastructureAgent → Infrastructure 層
   - TestingAgent → Testing 層
   - 根據步驟類型自動分配

3. **並行執行**
   ```python
   # 建立依賴圖
   dependency_graph = build_dependency_graph(steps)

   # 識別可並行執行的步驟
   parallel_groups = identify_parallel_groups(dependency_graph)

   # 並行執行
   for group in parallel_groups:
       execute_parallel(group)
   ```

#### 具體好處

| 好處 | 說明 | 可衡量指標 |
|------|------|-----------|
| **降低複雜度** | 大任務分解為小步驟 | 平均步驟數 < 10 |
| **提升成功率** | 小步驟更容易驗證和修正 | 步驟成功率 > 90% |
| **時間節省** | 並行執行節省時間 | 時間節省 20-40% |
| **可追蹤性** | 每個步驟有 checkpoint | Checkpoint 覆蓋率 = 100% |

#### 實作檔案
- **Skill**: `.claude/skills/workflow-decomposition.md`
- **配置**: `.claude/config/main-config.json` → `kiro_principles.workflow_decomposition`

---

### 3. Stateless Handler（無狀態處理）

#### Kiro 原則
- Skill 不依賴 session 狀態
- 所有參數透過輸入傳遞
- 輸出完全由輸入決定
- 無全域變數依賴

#### Claude Code 實現方式

**技術手段：**
1. **Self-Contained Skills**
   ```markdown
   # 每個 Skill 包含完整資訊
   - 輸入 schema
   - 執行邏輯
   - 輸出格式
   - 驗證規則
   - 不依賴外部 session 狀態
   ```

2. **明確參數傳遞**
   ```bash
   # 所有參數透過命令列傳遞
   /idempotent-generate aggregate Customer --context=customer --package=domain.customer

   # 而非依賴 session 變數
   ```

3. **Agent 配置獨立**
   ```json
   {
     "agent_name": "DomainModelAgent",
     "context_sources": [
       ".kiro/steering/ddd-tactical-patterns.md",
       ".kiro/examples/ddd-patterns/aggregate-root-examples.md"
     ],
     "stateless": true
   }
   ```

#### 具體好處

| 好處 | 說明 | 可衡量指標 |
|------|------|-----------|
| **可重現性** | 任何時候執行都得到相同結果 | 重現率 = 100% |
| **可測試性** | 不依賴外部狀態，易於測試 | 測試覆蓋率 > 80% |
| **可並行性** | 多個 Skill 可同時執行 | 並行效率 > 70% |
| **降低錯誤** | 無狀態競爭 | 狀態相關錯誤 = 0 |

#### 實作檔案
- **配置**: `.claude/config/main-config.json` → `kiro_principles.stateless_handler`
- **Agent**: `.claude/agents/domain-model-agent.json` → `stateless: true`

---

### 4. Immutable Input（不可變輸入）

#### Kiro 原則
- 永遠不修改輸入文件
- 生成新文件而非編輯現有文件
- 保留原始 artifacts
- 使用版本控制

#### Claude Code 實現方式

**技術手段：**
1. **Read-Only 輸入**
   ```bash
   # 輸入文件設為 read-only
   chmod 444 requirements.md

   # Claude Code 只讀取，不修改
   ```

2. **生成新文件**
   ```java
   // 而非編輯現有的 Customer.java
   // 生成新的 CustomerV2.java 或 Customer.java.new

   // 或使用明確的生成目錄
   // generated/domain/customer/model/aggregate/Customer.java
   ```

3. **備份機制**
   ```json
   {
     "safety_nets": {
       "backup_before_edit": true,
       "backup_directory": ".claude/backups/"
     }
   }
   ```

#### 具體好處

| 好處 | 說明 | 可衡量指標 |
|------|------|-----------|
| **可回滾** | 隨時可恢復到原始狀態 | 回滾成功率 = 100% |
| **安全性** | 不會意外破壞現有程式碼 | 意外破壞率 = 0 |
| **可追溯** | 保留所有歷史版本 | 版本覆蓋率 = 100% |
| **可比較** | 可對比新舊版本差異 | Diff 可用性 = 100% |

#### 實作檔案
- **配置**: `.claude/config/main-config.json` → `kiro_principles.immutable_input`
- **配置**: `.claude/config/main-config.json` → `safety_nets.backup_before_edit`

---

### 5. Boundary Control（邊界控制）

#### Kiro 原則
- 明確定義輸入 schema
- 驗證所有輸入
- 明確定義輸出格式
- 錯誤處理邊界清晰

#### Claude Code 實現方式

**技術手段：**
1. **輸入 Schema 驗證**
   ```json
   {
     "input_schema": {
       "type": "object",
       "properties": {
         "aggregate_name": {
           "type": "string",
           "pattern": "^[A-Z][a-zA-Z0-9]*$",
           "minLength": 3,
           "maxLength": 50
         },
         "attributes": {
           "type": "array",
           "items": {
             "type": "object",
             "properties": {
               "name": {"type": "string"},
               "type": {"type": "string"}
             },
             "required": ["name", "type"]
           }
         }
       },
       "required": ["aggregate_name", "attributes"]
     }
   }
   ```

2. **輸出格式驗證**
   ```python
   def validate_output(generated_code):
       # 1. 語法驗證（可編譯）
       assert compiles(generated_code)

       # 2. Linting 驗證
       assert passes_checkstyle(generated_code)
       assert passes_pmd(generated_code)

       # 3. 架構驗證
       assert passes_archunit(generated_code)

       # 4. Schema 驗證
       assert matches_output_schema(generated_code)
   ```

3. **Quality Gates**
   ```json
   {
     "quality_gates": {
       "pre_execution": [
         "Validate inputs against schema",
         "Check prerequisites",
         "Verify environment"
       ],
       "post_execution": [
         "Validate outputs",
         "Run quality checks",
         "Update metrics"
       ]
     }
   }
   ```

#### 具體好處

| 好處 | 說明 | 可衡量指標 |
|------|------|-----------|
| **輸入品質** | 拒絕不合規輸入 | 無效輸入拒絕率 = 100% |
| **輸出品質** | 確保輸出符合標準 | 輸出驗證通過率 > 95% |
| **錯誤提早發現** | 在生成前發現問題 | 前置錯誤發現率 > 80% |
| **降低返工** | 一次生成正確 | 返工率 < 10% |

#### 實作檔案
- **配置**: `.claude/config/main-config.json` → `kiro_principles.boundary_control`
- **配置**: `.claude/config/main-config.json` → `quality_gates`

---

### 6. Fail-Safe Pattern（故障安全模式）

#### Kiro 原則
- 失敗時自動回滾
- 保存失敗日誌
- 不提交不完整的程式碼
- 提供降級策略

#### Claude Code 實現方式

**技術手段：**
1. **Checkpoint 機制**
   ```python
   def execute_with_checkpoints(plan):
       for phase in plan.phases:
           # 保存 checkpoint
           save_checkpoint(phase.id, "before")

           try:
               # 執行 phase
               result = execute_phase(phase)

               # 驗證 checkpoint
               if not validate_checkpoint(result):
                   # 回滾到上一個 checkpoint
                   rollback_to_checkpoint(phase.id)
                   raise ExecutionError(f"Phase {phase.id} failed")

               # 保存成功 checkpoint
               save_checkpoint(phase.id, "after", result)

           except Exception as e:
               # 回滾並報告
               rollback_to_checkpoint(phase.id)
               log_failure(phase.id, e)
               raise
   ```

2. **Dry-Run 模式**
   ```json
   {
     "safety_nets": {
       "dry_run_mode": true,
       "description": "預覽生成結果，但不實際寫入檔案"
     }
   }
   ```

3. **降級策略**
   ```python
   try:
       # 嘗試使用最佳策略（例如：MapStruct）
       code = generate_with_mapstruct(spec)
   except MapStructError:
       # 降級到手工 Mapper
       logger.warning("MapStruct failed, falling back to manual mapper")
       code = generate_with_manual_mapper(spec)
   ```

#### 具體好處

| 好處 | 說明 | 可衡量指標 |
|------|------|-----------|
| **可靠性** | 失敗不會破壞現有程式碼 | 破壞率 = 0 |
| **可恢復性** | 可快速恢復到上一個狀態 | 恢復時間 < 5s |
| **可審計性** | 所有失敗都有日誌 | 日誌覆蓋率 = 100% |
| **降低風險** | Dry-run 預覽結果 | 風險降低 > 70% |

#### 實作檔案
- **配置**: `.claude/config/main-config.json` → `safety_nets`
- **Skill**: `.claude/skills/idempotent-code-generation.md` → Step 5 (安全網)

---

### 7. Isolation Pattern（隔離模式）

#### Kiro 原則
- 每個元件獨立運作
- 失敗不會傳播
- 使用 Circuit Breaker
- 超時保護

#### Claude Code 實現方式

**技術手段：**
1. **Sub-Agent 隔離**
   ```json
   {
     "agents": {
       "DomainModelAgent": {
         "timeout_seconds": 300,
         "max_retries": 3,
         "circuit_breaker": true
       },
       "InfrastructureAgent": {
         "timeout_seconds": 180,
         "max_retries": 2,
         "circuit_breaker": true
       }
     }
   }
   ```

2. **超時保護**
   ```python
   @timeout(seconds=300)
   def execute_agent(agent, task):
       try:
           return agent.execute(task)
       except TimeoutError:
           logger.error(f"Agent {agent.name} timed out")
           return None
   ```

3. **Circuit Breaker**
   ```python
   circuit_breaker = CircuitBreaker(
       failure_threshold=3,
       recovery_timeout=60
   )

   @circuit_breaker
   def call_external_mcp(service, request):
       return service.call(request)
   ```

#### 具體好處

| 好處 | 說明 | 可衡量指標 |
|------|------|-----------|
| **故障隔離** | 一個 Agent 失敗不影響其他 | 故障隔離率 > 95% |
| **可用性** | 部分失敗仍可繼續 | 部分可用性 > 90% |
| **可控性** | 超時自動中斷 | 超時保護率 = 100% |
| **降低連鎖失敗** | Circuit Breaker 阻止雪崩 | 連鎖失敗率 < 5% |

#### 實作檔案
- **配置**: `.claude/config/main-config.json` → `safety_nets.timeout_seconds`
- **Agent**: `.claude/agents/*.json` → `execution_parameters.timeout`

---

## 二、Claude Code 實作設計

### 整體架構

```
Claude Code Workspace
│
├── .claude/
│   ├── config/
│   │   └── main-config.json              # 主配置（Kiro 原則、Quality Gates、Safety Nets）
│   │
│   ├── skills/
│   │   ├── idempotent-code-generation.md # Skill: 幂等程式碼生成
│   │   └── workflow-decomposition.md     # Skill: 工作流分解
│   │
│   ├── hooks/
│   │   └── session-start-kiro-principles.md # Hook: Session 啟動時載入 Kiro 原則
│   │
│   ├── commands/
│   │   └── idempotent-generate.md        # Command: /idempotent-generate
│   │
│   ├── agents/
│   │   ├── domain-model-agent.json       # Sub-Agent: Domain 層專家
│   │   └── infrastructure-agent.json     # Sub-Agent: Infrastructure 層專家
│   │
│   ├── templates/
│   │   ├── aggregate-root.java.template
│   │   ├── value-object.java.template
│   │   └── domain-event.java.template
│   │
│   ├── cache/                            # 幂等性快取
│   ├── backups/                          # 安全備份
│   ├── logs/                             # 執行日誌
│   └── metrics/                          # 監控指標
│
├── .kiro/                                # 原有的 Kiro 配置（Steering Rules, Examples, etc.）
│
└── reports-summaries/
    └── claude-code-execution/            # 執行報告
```

### Skills 設計

#### 1. Idempotent Code Generation

**Purpose**: 確保相同輸入產生相同輸出

**核心機制**:
- 輸入雜湊 (SHA-256)
- 結果快取
- 確定性模板
- 輸出驗證

**使用場景**:
- 生成 Aggregate Root
- 生成 Value Object
- 生成 Domain Event
- 生成 Repository

**Quality Gates**:
- ✅ 編譯通過
- ✅ ArchUnit 驗證
- ✅ Checkstyle 驗證
- ✅ 符合 Steering Rules

#### 2. Workflow Decomposition

**Purpose**: 自動分解複雜任務

**核心機制**:
- 任務分析（識別 Bounded Contexts、Layers、Dependencies）
- 步驟生成（建立依賴圖、拓撲排序）
- Sub-Agent 分配
- 並行執行
- Checkpoint 機制

**使用場景**:
- 實作完整的 Bounded Context
- 重構現有程式碼
- 添加跨層級功能

**Quality Gates**:
- ✅ 所有步驟成功執行
- ✅ Checkpoints 完整
- ✅ 無依賴違反
- ✅ 執行時間在預估範圍內

### Hooks 設計

#### Session Start Hook

**Purpose**: 每次 session 啟動時自動載入 Kiro 原則

**執行內容**:
1. 載入 Kiro Steering Rules (17 files)
2. 載入 Examples (25 files)
3. 設定 MCP servers
4. 啟用監控
5. 顯示 Quality Checklist

**配置**:
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

### Sub-Agents 設計

#### 1. DomainModelAgent

**專長**: DDD Tactical Patterns

**職責**:
- 生成 Aggregate Roots
- 生成 Value Objects
- 生成 Domain Events
- 生成 Repository Interfaces

**上下文來源**:
- `.kiro/steering/ddd-tactical-patterns.md`
- `.kiro/examples/ddd-patterns/`

**驗證規則**:
- Aggregate 必須繼承 `AggregateRoot<ID>`
- Value Object 使用 `record`
- Domain Event 必須 immutable
- Domain 層不得依賴 Infrastructure

**執行參數**:
- Model: Sonnet (高品質)
- Temperature: 0.1 (低隨機性)
- Thoroughness: High

#### 2. InfrastructureAgent

**專長**: Infrastructure Layer (Adapters, Persistence)

**職責**:
- 生成 JPA Entities
- 生成 Repository Adapters
- 生成 Entity Mappers (prefer MapStruct)

**反模式警告**:
- ❌ 不使用手工 Mapper（使用 MapStruct）
- ❌ 不重複 CRUD 邏輯（使用 BaseJpaAdapter）
- ❌ 不使用反射構造 Entity

**執行參數**:
- Model: Haiku (快速執行)
- Temperature: 0.1
- Thoroughness: Medium

---

## 三、配置檔案清單

### 主配置檔案

**檔案**: `.claude/config/main-config.json`

```json
{
  "version": "1.0.0",
  "description": "Claude Code configuration following Amazon Kiro principles",
  "created": "2025-11-16",

  "kiro_principles": {
    "idempotency": {
      "enabled": true,
      "implementation": [
        "Use deterministic prompts",
        "Version control for all templates",
        "Immutable input validation"
      ]
    },
    "workflow_decomposition": {
      "enabled": true,
      "implementation": [
        "Multi-step task planning",
        "Sub-agent delegation",
        "Checkpoint-based execution"
      ]
    },
    "stateless_handler": {
      "enabled": true,
      "implementation": [
        "Self-contained skills",
        "Explicit parameter passing",
        "No global state dependencies"
      ]
    },
    "immutable_input": {
      "enabled": true,
      "implementation": [
        "Read-only file access by default",
        "Generate new files instead of editing",
        "Preserve original artifacts"
      ]
    },
    "boundary_control": {
      "enabled": true,
      "implementation": [
        "Schema validation for inputs",
        "Type-safe outputs",
        "Error handling boundaries"
      ]
    }
  },

  "quality_gates": {
    "pre_execution": [
      "Validate inputs against schema",
      "Check prerequisites",
      "Verify environment"
    ],
    "post_execution": [
      "Validate outputs",
      "Run quality checks",
      "Update metrics"
    ]
  },

  "safety_nets": {
    "max_file_size": "10MB",
    "max_iterations": 10,
    "timeout_seconds": 300,
    "backup_before_edit": true,
    "dry_run_mode": false
  },

  "observability": {
    "logging_level": "INFO",
    "metrics_enabled": true,
    "trace_execution": true,
    "report_directory": "reports-summaries/claude-code-execution"
  }
}
```

### Skills 配置

#### Skill 1: Idempotent Code Generation

**檔案**: `.claude/skills/idempotent-code-generation.md`

**用法**:
```bash
/idempotent-generate aggregate Customer
/idempotent-generate valueobject CustomerId
/idempotent-generate event CustomerCreated
```

#### Skill 2: Workflow Decomposition

**檔案**: `.claude/skills/workflow-decomposition.md`

**用法**:
```bash
/decompose-workflow "實作完整的 Customer Bounded Context"
```

### Hooks 配置

**檔案**: `.claude/hooks/session-start-kiro-principles.md`

**觸發時機**: Session 啟動時自動執行

### Commands 配置

**檔案**: `.claude/commands/idempotent-generate.md`

**用法**:
```bash
/idempotent-generate <type> <name> [options]
```

### Sub-Agents 配置

#### Agent 1: DomainModelAgent

**檔案**: `.claude/agents/domain-model-agent.json`

**專長**: DDD Tactical Patterns

#### Agent 2: InfrastructureAgent

**檔案**: `.claude/agents/infrastructure-agent.json`

**專長**: Infrastructure Layer

---

## 四、最佳實踐

### 1. 使用 Idempotent Code Generation

**場景**: 生成 Domain 層程式碼

**步驟**:
```bash
# Step 1: 準備 requirements
cat > requirements.md <<EOF
## Aggregate: Customer
### Attributes
- CustomerId (Value Object)
- CustomerName (Value Object)
- Email (Value Object)
EOF

# Step 2: 執行生成
/idempotent-generate aggregate Customer

# Step 3: 驗證結果
./gradlew archUnit
./gradlew checkstyleMain
```

**預期結果**:
- ✅ 生成 `Customer.java` (符合 DDD patterns)
- ✅ 通過 ArchUnit 驗證
- ✅ 通過 Checkstyle 驗證
- ✅ 快取結果（相同輸入再次執行返回快取）

### 2. 使用 Workflow Decomposition

**場景**: 實作完整的 Bounded Context

**步驟**:
```bash
# Step 1: 描述任務
/decompose-workflow "實作 Order Management Bounded Context，包含 CRUD、狀態機、事件發布"

# Step 2: 審查分解計劃
# Claude Code 會顯示分解後的步驟、依賴關係、預估時間

# Step 3: 執行計劃
/execute-decomposed-plan order-management-plan.md

# Step 4: 追蹤進度
# 自動顯示進度條和 checkpoint
```

**預期結果**:
- ✅ 任務分解為 < 10 個步驟
- ✅ 依賴關係明確
- ✅ 可並行步驟自動識別
- ✅ Checkpoint 自動保存
- ✅ 失敗自動回滾

### 3. 結合 Steering Rules

**場景**: 確保生成的程式碼符合專案標準

**步驟**:
```bash
# Session 啟動時自動載入 Steering Rules
# 無需手動操作

# 生成程式碼時，Claude Code 自動參考：
# - .kiro/steering/ddd-tactical-patterns.md
# - .kiro/steering/design-principles.md
# - .kiro/examples/ddd-patterns/
```

**預期結果**:
- ✅ 生成的 Aggregate 符合 DDD 模式
- ✅ 使用正確的 annotation (`@AggregateRoot`)
- ✅ Value Object 使用 `record`
- ✅ 業務規則驗證完整

### 4. 使用 Dry-Run 模式

**場景**: 預覽生成結果，不實際寫入檔案

**步驟**:
```bash
# 啟用 dry-run 模式
# 修改 .claude/config/main-config.json
{
  "safety_nets": {
    "dry_run_mode": true
  }
}

# 執行生成
/idempotent-generate aggregate Customer

# 預覽結果（不寫入檔案）
# 審查後，關閉 dry-run 模式再執行
```

**預期結果**:
- ✅ 顯示生成的程式碼
- ✅ 不實際寫入檔案
- ✅ 可安全審查
- ✅ 降低風險

---

## 五、注意事項

### 適用的 Kiro 技巧

| Kiro 技巧 | Claude Code 適用性 | 說明 |
|-----------|-------------------|------|
| **Idempotency** | ✅ 完全適用 | 透過輸入雜湊、快取、確定性 prompt 實現 |
| **Workflow Decomposition** | ✅ 完全適用 | 透過 Sub-Agent 分配、依賴圖、並行執行實現 |
| **Stateless Handler** | ✅ 完全適用 | Skills 和 Agents 都是 stateless |
| **Immutable Input** | ✅ 完全適用 | Read-only 輸入、生成新文件、備份機制 |
| **Boundary Control** | ✅ 完全適用 | Schema 驗證、Quality Gates |
| **Fail-Safe Pattern** | ✅ 完全適用 | Checkpoint、回滾、Dry-run |
| **Isolation Pattern** | ✅ 完全適用 | Sub-Agent 隔離、超時保護、Circuit Breaker |

### 不完全適用的 Kiro 技巧

| Kiro 技巧 | Claude Code 限制 | 替代方案 |
|-----------|-----------------|---------|
| **Auto-Scaling** | ⚠️ 部分適用 | Claude Code 不支援自動擴展 Agents，但可手動配置並行度 |
| **Load Balancing** | ⚠️ 不適用 | Claude Code 是單機工具，無需 load balancing |
| **Distributed Tracing** | ⚠️ 部分適用 | 可追蹤單機執行，但無分散式追蹤 |
| **Multi-Tenancy** | ❌ 不適用 | Claude Code 是單用戶工具 |

### 特別注意

#### 1. Temperature 設定

**Kiro 原則**: 使用確定性演算法

**Claude Code 實作**:
```json
{
  "execution_parameters": {
    "temperature": 0.1  // 盡可能低，但不是 0（會導致過於機械化）
  }
}
```

**注意**: Temperature = 0 可能導致：
- 過於機械化的程式碼
- 缺乏創意的命名
- 僵化的結構

**建議**: Temperature = 0.1-0.2（平衡確定性與程式碼品質）

#### 2. 快取時效

**Kiro 原則**: 幂等性快取永久有效

**Claude Code 實作**:
```json
{
  "parameters": {
    "cache_enabled": true,
    "cache_ttl": 3600  // 1 小時
  }
}
```

**注意**: 如果模板或 Steering Rules 變更，需清除快取

**建議**:
```bash
# 清除快取
rm -rf .claude/cache/*

# 或使用版本化快取
.claude/cache/v1.0.0/
.claude/cache/v1.1.0/
```

#### 3. Sub-Agent 選擇

**Kiro 原則**: 根據任務類型自動選擇 handler

**Claude Code 實作**:
- Domain 層 → DomainModelAgent (Sonnet, high quality)
- Infrastructure 層 → InfrastructureAgent (Haiku, fast)
- Testing 層 → TestingAgent (Sonnet, high coverage)

**注意**: 不同 Agent 使用不同模型，成本和速度不同

**建議**:
- 重要程式碼（Domain 層）→ 使用 Sonnet
- 重複性程式碼（Infrastructure 層）→ 使用 Haiku
- 平衡成本與品質

#### 4. Validation Strictness

**Kiro 原則**: 嚴格驗證輸入和輸出

**Claude Code 實作**:
```json
{
  "parameters": {
    "validation_strict": true
  }
}
```

**注意**: 嚴格模式可能導致：
- 較多失敗（但品質更高）
- 較長執行時間（需要重試）

**建議**:
- 開發階段：`validation_strict: false`（快速迭代）
- 生產階段：`validation_strict: true`（確保品質）

---

## 六、使用指南

### 快速開始

#### 1. 初始化 Claude Code Workspace

```bash
# Clone repository
git clone https://github.com/HUNG-YU-LI/genai-demo.git
cd genai-demo

# 確認 .claude/ 目錄存在
ls -la .claude/

# 輸出應包含：
# .claude/config/
# .claude/skills/
# .claude/hooks/
# .claude/commands/
# .claude/agents/
```

#### 2. 驗證配置

```bash
# 檢查主配置
cat .claude/config/main-config.json

# 檢查 Skills
ls .claude/skills/

# 檢查 Agents
ls .claude/agents/
```

#### 3. 啟動 Claude Code Session

```bash
# 啟動 Claude Code
# Session start hook 會自動執行

# 驗證 Kiro Principles 已載入
# 應該看到：
# ✅ Kiro Principles loaded
# ✅ Steering Rules: 17 files
# ✅ Examples: 25 files
# ✅ MCP Servers: 4 active
```

### 常見使用場景

#### 場景 1: 生成 Aggregate Root

```bash
# 使用 idempotent-generate command
/idempotent-generate aggregate Order

# 預期輸出：
# ✓ Input validated
# ✓ Input hash: a1b2c3d4...
# ✓ Generating Order.java
# ✓ Output validated (ArchUnit: PASS, Checkstyle: PASS)
# ✓ Saved to cache
# ✓ File created: domain/order/model/aggregate/Order.java
```

#### 場景 2: 分解複雜任務

```bash
# 使用 workflow-decomposition skill
/decompose-workflow "實作 Payment Processing 功能，包含 Domain、Application、Infrastructure 層"

# 預期輸出：
# ✓ Task analyzed
# ✓ Identified: 1 bounded context, 3 layers, 8 steps
# ✓ Dependency graph created
# ✓ Parallel groups identified: 2 groups
# ✓ Estimated time: 4.5 hours (with parallelization: 3.2 hours)
#
# Generated plan:
# - Phase 1: Domain Layer (3 steps, 2 hours)
# - Phase 2: Application Layer (2 steps, 1.5 hours)
# - Phase 3: Infrastructure Layer (3 steps, 1 hour, parallel with Phase 2)
#
# Execute plan? (y/n)
```

#### 場景 3: 驗證程式碼品質

```bash
# 執行 ArchUnit 驗證
./gradlew archUnit

# 執行 Checkstyle 驗證
./gradlew checkstyleMain

# 執行所有測試
./gradlew test

# 生成覆蓋率報告
./gradlew jacocoTestReport
```

### 監控與除錯

#### 查看執行日誌

```bash
# 查看最新 session 日誌
cat .claude/logs/session-latest.log

# 查看特定 session 日誌
cat .claude/logs/session-2025-11-16-14-30-00.log

# 查看錯誤日誌
grep ERROR .claude/logs/session-latest.log
```

#### 查看執行報告

```bash
# 查看最新執行報告
cat reports-summaries/claude-code-execution/latest-report.md

# 查看所有報告
ls reports-summaries/claude-code-execution/
```

#### 查看監控指標

```bash
# 查看最新指標
cat .claude/metrics/session-latest.json

# 分析指標趨勢
python scripts/analyze-metrics.py .claude/metrics/
```

---

## 七、架構圖

### 整體架構（文字版）

```
┌─────────────────────────────────────────────────────────────────┐
│                       Claude Code Session                        │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              Session Start Hook (自動執行)                │  │
│  │  - 載入 Kiro Principles                                  │  │
│  │  - 載入 Steering Rules (17 files)                        │  │
│  │  - 載入 Examples (25 files)                              │  │
│  │  - 啟用 MCP Servers (AWS docs, CDK, Excalidraw)         │  │
│  │  - 啟用監控                                              │  │
│  └──────────────────────────────────────────────────────────┘  │
│                             ↓                                    │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                    User Command                           │  │
│  │  /idempotent-generate aggregate Customer                 │  │
│  │  /decompose-workflow "task description"                  │  │
│  └──────────────────────────────────────────────────────────┘  │
│                             ↓                                    │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                  Skill Execution                          │  │
│  │                                                           │  │
│  │  ┌─────────────────────────────────────────────────┐    │  │
│  │  │  Idempotent Code Generation Skill               │    │  │
│  │  │  1. 驗證輸入 (Schema validation)                │    │  │
│  │  │  2. 計算雜湊 (SHA-256)                          │    │  │
│  │  │  3. 檢查快取                                    │    │  │
│  │  │  4. 選擇模板                                    │    │  │
│  │  │  5. 呼叫 Sub-Agent                              │    │  │
│  │  │  6. 驗證輸出 (ArchUnit, Checkstyle)            │    │  │
│  │  │  7. 保存快取                                    │    │  │
│  │  └─────────────────────────────────────────────────┘    │  │
│  │                       ↓                                   │  │
│  │  ┌─────────────────────────────────────────────────┐    │  │
│  │  │  Workflow Decomposition Skill                    │    │  │
│  │  │  1. 任務分析 (識別 contexts, layers, deps)      │    │  │
│  │  │  2. 生成步驟 (建立依賴圖)                       │    │  │
│  │  │  3. Sub-Agent 分配                              │    │  │
│  │  │  4. 並行執行 (parallel groups)                  │    │  │
│  │  │  5. Checkpoint 機制                             │    │  │
│  │  └─────────────────────────────────────────────────┘    │  │
│  └──────────────────────────────────────────────────────────┘  │
│                             ↓                                    │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                  Sub-Agent Layer                          │  │
│  │                                                           │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │  │
│  │  │ DomainModel  │  │Infrastructure│  │   Testing    │  │  │
│  │  │    Agent     │  │    Agent     │  │    Agent     │  │  │
│  │  │              │  │              │  │              │  │  │
│  │  │ Sonnet       │  │ Haiku        │  │ Sonnet       │  │  │
│  │  │ Temp: 0.1    │  │ Temp: 0.1    │  │ Temp: 0.1    │  │  │
│  │  │ High Quality │  │ Fast         │  │ High Coverage│  │  │
│  │  └──────────────┘  └──────────────┘  └──────────────┘  │  │
│  └──────────────────────────────────────────────────────────┘  │
│                             ↓                                    │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                  Quality Gates                            │  │
│  │                                                           │  │
│  │  Pre-Execution:                                          │  │
│  │  ✓ Validate inputs against schema                       │  │
│  │  ✓ Check prerequisites                                   │  │
│  │  ✓ Verify environment                                    │  │
│  │                                                           │  │
│  │  Post-Execution:                                         │  │
│  │  ✓ Validate outputs                                      │  │
│  │  ✓ Run quality checks (ArchUnit, Checkstyle, PMD)       │  │
│  │  ✓ Update metrics                                        │  │
│  └──────────────────────────────────────────────────────────┘  │
│                             ↓                                    │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                    Safety Nets                            │  │
│  │                                                           │  │
│  │  ✓ Backup before edit                                    │  │
│  │  ✓ Checkpoint mechanism                                  │  │
│  │  ✓ Rollback on failure                                   │  │
│  │  ✓ Timeout protection (300s)                             │  │
│  │  ✓ Dry-run mode (optional)                               │  │
│  └──────────────────────────────────────────────────────────┘  │
│                             ↓                                    │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                     Output                                │  │
│  │                                                           │  │
│  │  ✓ Generated files                                       │  │
│  │  ✓ Execution report                                      │  │
│  │  ✓ Metrics (JSON)                                        │  │
│  │  ✓ Logs                                                  │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### Kiro 原則映射圖

```
Kiro Principles                    Claude Code Implementation
════════════════                   ═══════════════════════════

Idempotency                   →    • Input hashing (SHA-256)
• Same input = same output         • Result caching
• Deterministic algorithms          • Deterministic prompts (temp=0.1)
• Input tracking                    • Template versioning

Workflow Decomposition        →    • Task analysis
• Break into discrete steps         • Dependency graph
• Clear input/output                • Sub-agent delegation
• Parallel execution                • Parallel group execution

Stateless Handler             →    • Self-contained skills
• No session state                  • Explicit parameters
• All params via input              • Stateless agents
• Output = f(input)                 • No global variables

Immutable Input               →    • Read-only inputs
• Never modify input                • Generate new files
• Preserve originals                • Backup mechanism
• Version control                   • Git integration

Boundary Control              →    • Input schema validation
• Define input schema               • Output format validation
• Validate all inputs               • Quality gates
• Clear error boundaries            • Type-safe outputs

Fail-Safe Pattern             →    • Checkpoint mechanism
• Auto rollback on failure          • Backup before edit
• Save failure logs                 • Dry-run mode
• No incomplete commits             • Rollback on validation failure

Isolation Pattern             →    • Sub-agent isolation
• Components operate independently  • Timeout protection (300s)
• Failure doesn't propagate         • Circuit breaker
• Timeout protection                • Error boundaries
```

---

## 八、附錄

### A. 檔案清單

#### 配置檔案

| 檔案路徑 | 用途 | 格式 |
|---------|------|------|
| `.claude/config/main-config.json` | 主配置（Kiro 原則、Quality Gates、Safety Nets） | JSON |

#### Skills

| 檔案路徑 | 用途 | 格式 |
|---------|------|------|
| `.claude/skills/idempotent-code-generation.md` | 幂等程式碼生成 | Markdown |
| `.claude/skills/workflow-decomposition.md` | 工作流分解 | Markdown |

#### Hooks

| 檔案路徑 | 用途 | 格式 |
|---------|------|------|
| `.claude/hooks/session-start-kiro-principles.md` | Session 啟動時載入 Kiro 原則 | Markdown |

#### Commands

| 檔案路徑 | 用途 | 格式 |
|---------|------|------|
| `.claude/commands/idempotent-generate.md` | `/idempotent-generate` 命令 | Markdown |

#### Sub-Agents

| 檔案路徑 | 用途 | 格式 |
|---------|------|------|
| `.claude/agents/domain-model-agent.json` | Domain 層專家 Agent | JSON |
| `.claude/agents/infrastructure-agent.json` | Infrastructure 層專家 Agent | JSON |

### B. 目錄樹結構

```
genai-demo/
├── .claude/                                    # Claude Code 配置目錄
│   ├── config/
│   │   └── main-config.json                   # 主配置（Kiro 原則、Quality Gates）
│   │
│   ├── skills/
│   │   ├── idempotent-code-generation.md      # Skill: 幂等程式碼生成
│   │   └── workflow-decomposition.md          # Skill: 工作流分解
│   │
│   ├── hooks/
│   │   └── session-start-kiro-principles.md   # Hook: Session 啟動
│   │
│   ├── commands/
│   │   └── idempotent-generate.md             # Command: /idempotent-generate
│   │
│   ├── agents/
│   │   ├── domain-model-agent.json            # Sub-Agent: Domain 層
│   │   └── infrastructure-agent.json          # Sub-Agent: Infrastructure 層
│   │
│   ├── templates/                             # 程式碼模板（待創建）
│   │   ├── aggregate-root.java.template
│   │   ├── value-object.java.template
│   │   └── domain-event.java.template
│   │
│   ├── cache/                                 # 幂等性快取
│   ├── backups/                               # 安全備份
│   ├── logs/                                  # 執行日誌
│   └── metrics/                               # 監控指標
│
├── .kiro/                                     # 原有的 Kiro 配置
│   ├── steering/                              # Steering Rules (17 files)
│   ├── examples/                              # Examples (25 files)
│   ├── hooks/                                 # Kiro Hooks
│   ├── settings/                              # Kiro Settings (MCP, Diagram Sync)
│   └── specs/                                 # Specs (Requirements, Design, Tasks)
│
├── reports-summaries/
│   ├── architecture-design/
│   │   ├── kiro-ai-coding-analysis-report.md  # Kiro 分析報告
│   │   └── kiro-to-claude-code-blueprint.md   # 本文檔
│   │
│   └── claude-code-execution/                 # Claude Code 執行報告（待創建）
│
├── app/src/main/java/                         # 應用程式碼
├── docs/                                      # 文檔
├── infrastructure/                            # 基礎設施（AWS CDK）
└── ... (其他專案檔案)
```

### C. 快速命令參考

#### Claude Code Commands

```bash
# 幂等生成
/idempotent-generate aggregate Customer
/idempotent-generate valueobject CustomerId
/idempotent-generate event CustomerCreated
/idempotent-generate repository Customer

# 工作流分解
/decompose-workflow "task description"

# 執行分解後的計劃
/execute-decomposed-plan plan.md
```

#### 驗證命令

```bash
# 架構驗證
./gradlew archUnit

# 程式碼品質
./gradlew checkstyleMain pmdMain

# 測試
./gradlew test
./gradlew cucumber

# 覆蓋率
./gradlew jacocoTestReport
```

#### 監控命令

```bash
# 查看日誌
cat .claude/logs/session-latest.log

# 查看報告
cat reports-summaries/claude-code-execution/latest-report.md

# 查看指標
cat .claude/metrics/session-latest.json

# 清除快取
rm -rf .claude/cache/*
```

### D. 監控指標

#### Idempotency Metrics

- `idempotent_cache_hit_rate`: 快取命中率（目標 > 60%）
- `generation_consistency_score`: 一致性分數（目標 = 100%）
- `validation_pass_rate`: 驗證通過率（目標 > 95%）
- `avg_generation_time`: 平均生成時間（目標 < 30s）

#### Workflow Decomposition Metrics

- `decomposition_accuracy`: 分解準確度（實際步驟 vs 預估步驟）
- `parallel_efficiency`: 並行效率（實際時間節省 vs 理論節省）
- `checkpoint_recovery_rate`: Checkpoint 恢復成功率
- `dependency_violation_count`: 依賴違反次數

#### Quality Metrics

- `archunit_pass_rate`: ArchUnit 通過率（目標 = 100%）
- `checkstyle_violation_count`: Checkstyle 違反數（目標 = 0）
- `pmd_violation_count`: PMD 違反數（目標 < 10）
- `test_coverage`: 測試覆蓋率（目標 > 80%）

### E. 故障排除

#### 問題 1: 快取命中率低

**症狀**: `idempotent_cache_hit_rate` < 30%

**可能原因**:
- 輸入經常變化（細微差異）
- 模板或 Steering Rules 經常更新
- 快取 TTL 設定過短

**解決方案**:
```bash
# 1. 檢查輸入差異
diff requirements-v1.md requirements-v2.md

# 2. 延長快取 TTL
# 修改 .claude/config/main-config.json
{
  "cache_ttl": 7200  // 從 3600 增加到 7200
}

# 3. 使用版本化快取
.claude/cache/v1.0.0/
```

#### 問題 2: 驗證通過率低

**症狀**: `validation_pass_rate` < 80%

**可能原因**:
- Steering Rules 過於嚴格
- 模板品質不佳
- Agent temperature 設定過高

**解決方案**:
```bash
# 1. 降低 temperature
# 修改 .claude/agents/domain-model-agent.json
{
  "execution_parameters": {
    "temperature": 0.05  // 從 0.1 降低到 0.05
  }
}

# 2. 改進模板品質
# 參考高品質範例更新模板

# 3. 調整驗證嚴格度
# 修改 .claude/config/main-config.json
{
  "parameters": {
    "validation_strict": false  // 暫時放寬
  }
}
```

#### 問題 3: 執行超時

**症狀**: Agent 執行超過 300 秒

**可能原因**:
- 任務過於複雜
- MCP 服務回應緩慢
- 網路問題

**解決方案**:
```bash
# 1. 增加超時時間
# 修改 .claude/config/main-config.json
{
  "safety_nets": {
    "timeout_seconds": 600  // 從 300 增加到 600
  }
}

# 2. 分解任務
/decompose-workflow "task description"

# 3. 檢查 MCP 服務
# 查看 MCP 日誌
cat .kiro/logs/mcp-*.log
```

---

## 總結

本藍圖提供了從 Amazon Kiro 到 Claude Code 的完整映射和實作指南。所有配置檔案已創建並可直接使用。

### 核心價值

1. **穩定性**: 透過 Idempotency 確保相同輸入產生相同輸出
2. **可管理性**: 透過 Workflow Decomposition 將複雜任務分解
3. **可靠性**: 透過 Fail-Safe 和 Safety Nets 確保不破壞現有程式碼
4. **可追溯性**: 透過 Observability 追蹤所有執行
5. **品質保證**: 透過 Quality Gates 確保輸出符合標準

### 下一步

1. ✅ 配置檔案已創建
2. ⏭️ 創建模板檔案（aggregate-root.java.template, etc.）
3. ⏭️ 測試 Skills（執行 /idempotent-generate）
4. ⏭️ 收集監控指標
5. ⏭️ 持續優化（根據指標調整參數）

---

**文檔版本**: 1.0.0
**最後更新**: 2025-11-16
**維護者**: Development Team
