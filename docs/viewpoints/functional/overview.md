---
title: "Functional Viewpoint"
type: "viewpoint"
category: "functional"
stakeholders: ["business-analysts", "product-managers", "developers", "architects"]
last_updated: "2025-10-22"
version: "1.0"
status: "active"
owner: "Architecture Team"
related_docs:

  - "viewpoints/information/overview.md"
  - "viewpoints/context/overview.md"
  - "perspectives/security/overview.md"

tags: ["ddd", "bounded-contexts", "use-cases", "functional-capabilities"]
---

# Functional Viewpoint

> **Status**: ✅ Active
> **Last Updated**: 2025-10-22
> **Owner**: Architecture Team

## Overview

Functional Viewpoint 描述**系統做什麼**——它的功能能力、業務邏輯以及如何向用戶交付價值。此 viewpoint 基於 **Domain-Driven Design (DDD)** 原則組織，將系統分解為 13 個 bounded contexts，每個代表一個獨特的業務能力。

Enterprise E-Commerce Platform 使用 **Hexagonal Architecture**（Ports and Adapters）結合 **Event-Driven Architecture** 構建，在 bounded contexts 之間實現鬆耦合，同時在每個 context 內保持強一致性。

## Purpose

此 viewpoint 回答以下關鍵問題：

- 系統的核心業務能力是什麼？
- 系統如何組織成 bounded contexts？
- 關鍵的 use cases 和用戶旅程是什麼？
- 系統的不同部分如何通訊？
- 功能介面（APIs 和 events）是什麼？

## Stakeholders

### Primary Stakeholders

- **Business Analysts**：理解業務能力和需求
- **Product Managers**：驗證功能完整性和業務價值
- **Developers**：實現業務邏輯和功能
- **Architects**：設計系統結構和整合模式

### Secondary Stakeholders

- **QA Engineers**：設計功能測試和驗證場景
- **Technical Writers**：記錄面向用戶的功能
- **Support Teams**：理解系統能力以提供客戶支援

## Contents

### 📄 Documents

- [Bounded Contexts](bounded-contexts.md) - 所有 13 個 bounded contexts 的詳細描述
- [Use Cases](use-cases.md) - 關鍵的用戶旅程和業務流程
- [Functional Interfaces](interfaces.md) - REST APIs 和 domain events

### 📊 Diagrams

- [Bounded Contexts Overview](../../diagrams/generated/functional/bounded-contexts-overview.png) - 高階 context map
- 每個 bounded context 的特定圖表（見 bounded-contexts.md）

## Key Concerns

### Concern 1: Business Capability Organization

**Description**：系統必須圍繞業務能力組織，以實現功能的獨立開發和部署。

**Why it matters**：業務能力之間的清晰邊界減少耦合，實現團隊自治，並允許系統在不同領域獨立演進。

**How it's addressed**：系統使用 DDD strategic design 分解為 13 個 bounded contexts：

- Customer Context
- Order Context
- Product Context
- Inventory Context
- Payment Context
- Delivery Context
- Promotion Context
- Notification Context
- Review Context
- Shopping Cart Context
- Pricing Context
- Seller Context
- Observability Context (cross-cutting)

### Concern 2: Context Integration

**Description**：Bounded contexts 必須有效通訊，同時保持鬆耦合。

**Why it matters**：contexts 之間的緊耦合會抵消 bounded context 分離的好處，並使系統更難演進。

**How it's addressed**：

- **Domain Events**：透過 domain events 進行跨 context 工作流的非同步通訊
- **REST APIs**：用於即時查詢的同步通訊
- **Shared Kernel**：在 `domain/shared/` 中的最小共享 value objects
- **Anti-Corruption Layer**：每個 context 將外部資料翻譯為自己的 domain model

### Concern 3: Business Rule Consistency

**Description**：業務規則必須在每個 bounded context 內一致執行。

**Why it matters**：不一致的業務規則執行會導致資料完整性問題和不可預測的系統行為。

**How it's addressed**：

- **Aggregate Roots**：執行 invariants 和業務規則
- **Domain Services**：實現跨多個 aggregates 的複雜業務邏輯
- **Validation**：多層驗證（value objects、aggregates、application services）
- **Event Sourcing**：維護所有業務狀態變更的稽核軌跡

## Architectural Models

### Model 1: Bounded Context Architecture

每個 bounded context 遵循 Hexagonal Architecture 模式：

```text
Bounded Context
├── Domain Layer (Core Business Logic)
│   ├── model/
│   │   ├── aggregate/     # Aggregate roots
│   │   ├── entity/        # Entities
│   │   └── valueobject/   # Value objects
│   ├── events/            # Domain events
│   ├── repository/        # Repository interfaces
│   ├── service/           # Domain services
│   └── validation/        # Business rules
│
├── Application Layer (Use Case Orchestration)
│   ├── {UseCase}ApplicationService.java
│   ├── command/           # Command objects
│   ├── query/             # Query objects
│   └── dto/               # Data transfer objects
│
├── Infrastructure Layer (Technical Implementation)
│   ├── persistence/       # Repository implementations
│   ├── messaging/         # Event publishers
│   └── external/          # External service adapters
│
└── Interfaces Layer (External Communication)
    └── rest/              # REST controllers
```

**Key Elements**：

- **Domain Layer**：包含純業務邏輯，沒有基礎設施依賴
- **Application Layer**：編排 use cases、管理 transactions、發布 events
- **Infrastructure Layer**：實現技術關注點（database、messaging、external APIs）
- **Interfaces Layer**：透過 REST APIs 暴露功能

### Model 2: Event-Driven Communication

Bounded contexts 透過 domain events 進行非同步通訊：

```text
Order Context                    Inventory Context
     |                                 |
     | OrderSubmittedEvent             |
     |-------------------------------->|
     |                                 | Reserve items
     |                                 |
     | InventoryReservedEvent          |
     |<--------------------------------|
     |                                 |
```

**Event Flow**：

1. Aggregate root 在業務操作期間收集 domain events
2. Application service 在成功 transaction 後發布 events
3. 其他 contexts 中的 event handlers 對 events 做出反應
4. 每個 context 維護自己的資料一致性

## Design Decisions

### Decision 1: Domain-Driven Design with Bounded Contexts

**Context**：需要在具有多個業務能力的大型電子商務系統中管理複雜性

**Decision**：採用 DDD strategic design，包含 13 個 bounded contexts

**Rationale**：

- 將軟體結構與業務組織對齊
- 實現不同業務能力的獨立演進
- 通過建立清晰的邊界來減少認知負荷
- 支援團隊自治和並行開發

**Consequences**：

- 需要仔細定義 context 邊界
- 需要跨 context 通訊模式
- contexts 之間可能有資料重複
- contexts 之間的最終一致性

**Related ADR**：[ADR-002: Adopt Hexagonal Architecture](../../architecture/adrs/ADR-002-adopt-hexagonal-architecture.md)

### Decision 2: Event-Driven Architecture for Context Integration

**Context**：需要在 bounded contexts 之間保持鬆耦合，同時維護業務流程完整性

**Decision**：使用 domain events 進行跨 context 通訊

**Rationale**：

- 解耦 bounded contexts（無直接依賴）
- 實現非同步處理以獲得更好的可擴展性
- 提供業務 events 的稽核軌跡
- 支援最終一致性模型

**Consequences**：

- 增加系統複雜性（distributed transactions）
- 需要 event 版本控制和 schema 演進
- 需要健全的 event 交付保證
- 除錯分散式工作流更複雜

**Related ADR**：[ADR-003: Use Domain Events for Cross-Context Communication](../../architecture/adrs/ADR-003-domain-events-communication.md)

## Key Concepts

### Bounded Context

Bounded context 是一個邏輯邊界，在其中定義和適用特定的 domain model。每個 context 都有自己的 ubiquitous language，並負責特定的業務能力。

### Aggregate Root

Aggregate root 是必須作為單一單位處理資料變更的 domain objects 集群的入口點。它執行業務 invariants 並收集 domain events。

### Domain Event

Domain event 代表 business domain 中發生的重要事件。Events 是過去發生事件的不可變記錄，用於跨 context 通訊。

### Use Case

Use case 代表用戶與系統互動以實現目標的特定方式。每個 use case 實現為編排 domain objects 的 application service。

## Constraints and Assumptions

### Constraints

- 每個 bounded context 必須可獨立部署
- Domain layer 不得依賴 infrastructure
- 跨 context 通訊必須是非同步的（查詢除外）
- 每個 context 擁有自己的資料（無共享資料庫）

### Assumptions

- 業務能力相對穩定（context 邊界不會頻繁變更）
- 跨 context 工作流可接受最終一致性
- Domain events 可靠交付（at-least-once delivery）
- 每個 context 可以根據負載獨立擴展

## Related Documentation

### Related Viewpoints

- [Information Viewpoint](../information/overview.md) - 每個 context 內的資料模型和所有權
- [Development Viewpoint](../development/overview.md) - 程式碼組織和模組結構
- [Context Viewpoint](../context/overview.md) - 外部系統整合和邊界

### Related Perspectives

- [Security Perspective](../../perspectives/security/overview.md) - 跨 contexts 的驗證和授權
- [Performance Perspective](../../perspectives/performance/overview.md) - 每個 context 的效能考量
- [Evolution Perspective](../../perspectives/evolution/overview.md) - Contexts 如何獨立演進

### Related Architecture Decisions

- [ADR-002: Adopt Hexagonal Architecture](../../architecture/adrs/ADR-002-adopt-hexagonal-architecture.md)
- [ADR-003: Use Domain Events for Cross-Context Communication](../../architecture/adrs/ADR-003-domain-events-communication.md)

### Related Guides

- [Development Guide](../../development/README.md) - 如何在 bounded contexts 中實現功能
- [API Documentation](../../api/README.md) - 每個 context 的 REST API 參考

## Implementation Guidelines

### For Developers

1. **Identify the Bounded Context**：確定您的功能屬於哪個 context
2. **Follow DDD Tactical Patterns**：使用 aggregates、value objects 和 domain events
3. **Respect Context Boundaries**：永遠不要直接存取另一個 context 的 database 或 domain objects
4. **Use Domain Events**：透過 events 與其他 contexts 通訊，而非直接呼叫
5. **Test in Isolation**：每個 context 應該可以獨立測試

### For Architects

1. **Define Clear Boundaries**：確保每個 context 都有明確定義的責任
2. **Minimize Context Coupling**：限制 contexts 之間的依賴
3. **Design Event Contracts**：為跨 context 通訊定義穩定的 event schemas
4. **Plan for Evolution**：設計 contexts 以獨立演進
5. **Monitor Context Health**：分別追蹤每個 context 的指標

### For Operations

1. **Deploy Independently**：每個 context 可以在不影響其他 context 的情況下部署
2. **Monitor Separately**：追蹤每個 context 的指標和日誌
3. **Scale Independently**：根據各自的負載模式擴展 contexts
4. **Handle Failures**：為跨 context 呼叫實現 circuit breakers 和 fallbacks

## Verification and Validation

### How to Verify

- 與 domain experts 審查 bounded context 邊界
- 驗證每個 context 都有清晰、單一的責任
- 確保 domain events 捕獲所有重要的業務事件
- 驗證 use cases 在 application services 中正確實現

### Validation Criteria

- 每個 bounded context 都可獨立部署
- Domain layer 沒有 infrastructure 依賴（由 ArchUnit 驗證）
- 所有跨 context 通訊都使用 domain events
- 業務規則在 aggregate roots 中執行

### Testing Approach

- **Unit Tests**：單獨測試 domain 邏輯（aggregates、value objects、domain services）
- **Integration Tests**：測試 repository 實現和 event 發布
- **BDD Tests**：使用 Cucumber scenarios 驗證 use cases
- **Contract Tests**：驗證 contexts 之間的 event schemas

## Known Issues and Limitations

### Current Limitations

- **Eventual Consistency**：跨 context 工作流可能有暫時的不一致
  - *Mitigation*：設計 UX 以處理最終一致性，提供狀態更新
- **Distributed Transactions**：contexts 之間沒有 ACID transactions
  - *Mitigation*：對複雜工作流使用 Saga pattern，實現補償 transactions

### Technical Debt

- **Context Boundary Refinement**：隨著 domain 理解的演進，某些 contexts 可能需要拆分或合併
  - *Plan*：每季度與 domain experts 進行 context 邊界審查
- **Event Versioning**：需要更好的 event schema 演進工具
  - *Plan*：在 2025 年 Q2 實現 event upcasting 機制

## Future Considerations

### Planned Improvements

- **Context Map Visualization**：顯示 context 關係和依賴的互動式圖表（2025 年 Q1）
- **Event Catalog**：包含 schemas 的所有 domain events 的可搜尋目錄（2025 年 Q2）
- **Context Health Dashboard**：每個 context 的健康指標即時監控（2025 年 Q2）

### Evolution Strategy

- Contexts 將根據業務需求獨立演進
- 隨著業務增長，可能從現有 contexts 中提取新的 contexts
- 每季度與 domain experts 審查 context 邊界
- Event schemas 將進行版本控制以支援向後相容

## Quick Links

- [Back to All Viewpoints](../README.md)
- [Bounded Contexts Details](bounded-contexts.md)
- [Use Cases](use-cases.md)
- [Functional Interfaces](interfaces.md)
- [Architecture Overview](../../architecture/README.md)
- [Main Documentation](../../README.md)

## Appendix

### Glossary

- **Bounded Context**：定義 domain model 的邏輯邊界
- **Aggregate Root**：domain objects 集群的入口點
- **Domain Event**：重要業務事件的不可變記錄
- **Ubiquitous Language**：開發人員和 domain experts 之間的共享詞彙
- **Anti-Corruption Layer**：保護 domain model 免受外部系統影響的翻譯層
- **Hexagonal Architecture**：將業務邏輯與技術關注點分離的架構模式

### References

- [Domain-Driven Design by Eric Evans](https://www.domainlanguage.com/ddd/)
- [Implementing Domain-Driven Design by Vaughn Vernon](https://vaughnvernon.com/)
- [Rozanski & Woods Software Systems Architecture](https://www.viewpoints-and-perspectives.info/)

### Change History

| Date | Version | Author | Changes |
|------|---------|--------|---------|
| 2025-10-22 | 1.0 | Architecture Team | Initial version |

---

**Template Version**: 1.0
**Last Template Update**: 2025-01-17
