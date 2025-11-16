# Workflow Decomposition Skill

**Kiro Principle**: Workflow Decomposition（工作流分解）
**Purpose**: 將複雜任務自動分解為可管理的子任務

## 技能描述

此技能實現 Amazon Kiro 的工作流分解原則，確保：
1. 複雜任務被分解為離散的步驟
2. 每個步驟有明確的輸入/輸出
3. 步驟之間的依賴關係明確
4. 可並行執行的步驟被識別

## 使用場景

- 實作完整的 Bounded Context
- 重構現有程式碼
- 添加新功能（跨多個層級）
- 生成完整的測試套件

## 分解策略

### 1. 垂直分解（By Layer）
```
任務：實作 Order Management 功能

分解為：
Step 1: Domain Layer
  - 1.1 Create Order Aggregate
  - 1.2 Create Order Value Objects
  - 1.3 Create Order Events
  - 1.4 Create Order Repository Interface

Step 2: Application Layer
  - 2.1 Create OrderService
  - 2.2 Create Command Handlers
  - 2.3 Create Event Handlers

Step 3: Infrastructure Layer
  - 3.1 Create JPA Entity
  - 3.2 Create Repository Implementation
  - 3.3 Create Mapper

Step 4: Interface Layer
  - 4.1 Create REST Controller
  - 4.2 Create DTOs
  - 4.3 Create API Documentation

Step 5: Testing
  - 5.1 Unit Tests (Domain)
  - 5.2 Integration Tests (Infrastructure)
  - 5.3 E2E Tests (API)
```

### 2. 水平分解（By Feature）
```
任務：實作訂單狀態機

分解為：
Step 1: DRAFT → SUBMITTED
Step 2: SUBMITTED → CONFIRMED
Step 3: CONFIRMED → SHIPPED
Step 4: SHIPPED → DELIVERED
Step 5: ANY → CANCELLED
```

### 3. 依賴分解（By Dependency）
```
任務：實作 Payment Processing

依賴圖：
PaymentGateway (external)
    ↓
PaymentAdapter (infrastructure)
    ↓
PaymentDomainService (domain)
    ↓
Order Aggregate (domain)

執行順序：
1. 先實作 external dependencies
2. 再實作 infrastructure adapters
3. 然後實作 domain services
4. 最後整合到 aggregates
```

## 執行流程

### Phase 1: 任務分析
```python
def analyze_task(task_description):
    """分析任務並生成分解計劃"""

    # 1. 識別涉及的 bounded contexts
    contexts = identify_bounded_contexts(task_description)

    # 2. 識別涉及的架構層級
    layers = identify_layers(task_description)

    # 3. 識別依賴關係
    dependencies = analyze_dependencies(contexts, layers)

    # 4. 生成分解計劃
    plan = generate_decomposition_plan(contexts, layers, dependencies)

    return plan
```

### Phase 2: 步驟生成
```python
def generate_steps(plan):
    """將計劃轉換為可執行步驟"""

    steps = []

    for phase in plan.phases:
        for task in phase.tasks:
            step = Step(
                id=f"{phase.id}.{task.id}",
                name=task.name,
                dependencies=task.dependencies,
                estimated_time=task.estimated_time,
                sub_agent=select_sub_agent(task),
                validation=task.validation_criteria
            )
            steps.append(step)

    return steps
```

### Phase 3: 步驟執行
```python
def execute_steps(steps):
    """執行步驟（支援並行）"""

    # 建立依賴圖
    dependency_graph = build_dependency_graph(steps)

    # 拓撲排序
    execution_order = topological_sort(dependency_graph)

    # 識別可並行執行的步驟
    parallel_groups = identify_parallel_groups(execution_order)

    # 執行
    results = []
    for group in parallel_groups:
        # 並行執行同一 group 的步驟
        group_results = execute_parallel(group)
        results.extend(group_results)

        # 驗證結果
        validate_group_results(group_results)

    return results
```

### Phase 4: 結果整合
```python
def integrate_results(results):
    """整合所有步驟的結果"""

    # 1. 收集所有生成的檔案
    generated_files = collect_generated_files(results)

    # 2. 驗證檔案之間的一致性
    validate_consistency(generated_files)

    # 3. 執行整體測試
    run_integration_tests(generated_files)

    # 4. 生成報告
    report = generate_execution_report(results)

    return report
```

## Sub-Agent 分配策略

```json
{
  "domain_layer": {
    "sub_agent": "DomainModelAgent",
    "specialization": "DDD tactical patterns",
    "examples": ".kiro/examples/ddd-patterns/"
  },
  "application_layer": {
    "sub_agent": "ApplicationServiceAgent",
    "specialization": "Use case orchestration",
    "examples": ".kiro/examples/application/"
  },
  "infrastructure_layer": {
    "sub_agent": "InfrastructureAgent",
    "specialization": "Adapters and persistence",
    "examples": ".kiro/examples/infrastructure/"
  },
  "testing": {
    "sub_agent": "TestingAgent",
    "specialization": "BDD and unit testing",
    "examples": ".kiro/examples/testing/"
  }
}
```

## 使用範例

### 完整實作 Customer Bounded Context

**輸入**:
```bash
/decompose-workflow "實作完整的 Customer Bounded Context，包含 CRUD 操作、業務規則驗證、事件發布"
```

**生成的分解計劃**:
```markdown
# Customer Bounded Context Implementation Plan

## Phase 1: Domain Layer (Parallel: No, Estimated: 2 hours)

### Step 1.1: Customer Aggregate
- File: domain/customer/model/aggregate/Customer.java
- Sub-Agent: DomainModelAgent
- Dependencies: None
- Validation: ArchUnit rules, Steering Rules

### Step 1.2: Customer Value Objects
- Files:
  - domain/customer/model/valueobject/CustomerId.java
  - domain/customer/model/valueobject/CustomerName.java
  - domain/customer/model/valueobject/Email.java
- Sub-Agent: DomainModelAgent
- Dependencies: None
- Validation: ArchUnit rules

### Step 1.3: Customer Events
- Files:
  - domain/customer/model/events/CustomerCreatedEvent.java
  - domain/customer/model/events/CustomerUpdatedEvent.java
- Sub-Agent: DomainModelAgent
- Dependencies: Step 1.1, 1.2
- Validation: Event schema validation

### Step 1.4: Customer Repository Interface
- File: domain/customer/model/repository/CustomerRepository.java
- Sub-Agent: DomainModelAgent
- Dependencies: Step 1.1, 1.2
- Validation: Interface segregation

## Phase 2: Application Layer (Parallel: No, Estimated: 1.5 hours)

### Step 2.1: CustomerService
- File: application/customer/CustomerService.java
- Sub-Agent: ApplicationServiceAgent
- Dependencies: Phase 1 complete
- Validation: Service layer tests

### Step 2.2: Command Handlers
- Files:
  - application/customer/command/CreateCustomerCommandHandler.java
  - application/customer/command/UpdateCustomerCommandHandler.java
- Sub-Agent: ApplicationServiceAgent
- Dependencies: Step 2.1
- Validation: Command pattern compliance

## Phase 3: Infrastructure Layer (Parallel: Yes with Phase 2, Estimated: 1.5 hours)

### Step 3.1: JPA Entity
- File: infrastructure/customer/persistence/entity/CustomerJpaEntity.java
- Sub-Agent: InfrastructureAgent
- Dependencies: Phase 1 complete
- Validation: JPA annotations

### Step 3.2: Repository Implementation
- File: infrastructure/customer/persistence/adapter/CustomerRepositoryAdapter.java
- Sub-Agent: InfrastructureAgent
- Dependencies: Step 3.1, Phase 1
- Validation: Repository tests

### Step 3.3: Entity Mapper
- File: infrastructure/customer/persistence/mapper/CustomerMapper.java
- Sub-Agent: InfrastructureAgent
- Dependencies: Step 3.1, 3.2
- Validation: Mapping correctness

## Phase 4: Interface Layer (Parallel: Yes with Phase 3, Estimated: 1 hour)

### Step 4.1: REST Controller
- File: interfaces/rest/customer/CustomerController.java
- Sub-Agent: InterfaceAgent
- Dependencies: Phase 2 complete
- Validation: API tests

### Step 4.2: DTOs
- Files:
  - interfaces/rest/customer/dto/CustomerDTO.java
  - interfaces/rest/customer/dto/CreateCustomerRequest.java
- Sub-Agent: InterfaceAgent
- Dependencies: Step 4.1
- Validation: DTO validation

## Phase 5: Testing (Parallel: No, Estimated: 2 hours)

### Step 5.1: Domain Tests
- Directory: app/src/test/java/domain/customer/
- Sub-Agent: TestingAgent
- Dependencies: Phase 1 complete
- Validation: 80%+ coverage

### Step 5.2: Integration Tests
- Directory: app/src/test/java/integration/customer/
- Sub-Agent: TestingAgent
- Dependencies: Phase 3 complete
- Validation: Integration scenarios

### Step 5.3: BDD Tests
- File: app/src/test/resources/features/customer/customer-management.feature
- Sub-Agent: TestingAgent
- Dependencies: All phases complete
- Validation: Gherkin syntax, scenario coverage

## Parallel Execution Groups

Group 1 (Sequential):
  - Phase 1 (Domain Layer)

Group 2 (Sequential):
  - Phase 2 (Application Layer)

Group 3 (Parallel):
  - Phase 3 (Infrastructure Layer)
  - Phase 4 (Interface Layer)

Group 4 (Sequential):
  - Phase 5 (Testing)

## Total Estimated Time: 8 hours
## Parallelization Benefit: ~20% time saving
```

**執行**:
```bash
# 自動執行分解後的計劃
/execute-decomposed-plan customer-bounded-context-plan.md

# 進度追蹤
Phase 1: ████████████████████ 100% (2/2 hours)
Phase 2: ████████░░░░░░░░░░░░  45% (0.7/1.5 hours)
Phase 3: ███████████████░░░░░  75% (1.1/1.5 hours)
Phase 4: ████████████████████ 100% (1/1 hours)
Phase 5: ░░░░░░░░░░░░░░░░░░░░   0% (0/2 hours)

Overall: ██████████░░░░░░░░░░  52% (4.8/8 hours)
```

## Checkpoint 機制

```python
def execute_with_checkpoints(plan):
    """執行計劃並保存 checkpoints"""

    for phase in plan.phases:
        # 執行 phase
        result = execute_phase(phase)

        # 保存 checkpoint
        save_checkpoint(phase.id, result)

        # 驗證 checkpoint
        if not validate_checkpoint(result):
            # 回滾到上一個 checkpoint
            rollback_to_checkpoint(phase.id - 1)
            raise ExecutionError(f"Phase {phase.id} failed validation")

    return "All phases completed successfully"
```

## 配置參數

```json
{
  "skill": "workflow-decomposition",
  "parameters": {
    "max_parallel_tasks": 3,
    "checkpoint_enabled": true,
    "auto_rollback": true,
    "sub_agent_selection": "auto",
    "estimated_time_buffer": 1.2,
    "dependency_validation": "strict"
  }
}
```

## 成功標準

1. ✅ 所有步驟成功執行
2. ✅ 所有驗證通過
3. ✅ Checkpoints 完整
4. ✅ 無依賴違反
5. ✅ 執行時間在預估範圍內

## 監控指標

- `decomposition_accuracy`: 分解準確度（實際步驟 vs 預估步驟）
- `parallel_efficiency`: 並行效率（實際時間節省 vs 理論節省）
- `checkpoint_recovery_rate`: Checkpoint 恢復成功率
- `dependency_violation_count`: 依賴違反次數
