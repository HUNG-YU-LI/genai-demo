# Architecture Documentation

> **最後更新**：2025-01-17

## 概述

本節包含 GenAI Demo 電子商務平台的完整架構文檔，遵循 Rozanski & Woods 方法論，包含 viewpoints、perspectives 和 Architecture Decision Records (ADRs)。

## 快速導航

### 📐 Architectural Viewpoints

- [Viewpoints Overview](../viewpoints/README.md) - 所有架構 viewpoints
- [Functional Viewpoint](../viewpoints/functional/README.md) - 業務能力
- [Information Viewpoint](../viewpoints/information/README.md) - 資料結構
- [Concurrency Viewpoint](../viewpoints/concurrency/README.md) - 並發模型
- [Development Viewpoint](../viewpoints/development/README.md) - 開發結構
- [Deployment Viewpoint](../viewpoints/deployment/README.md) - 部署架構
- [Operational Viewpoint](../viewpoints/operational/README.md) - 運營關注點
- [Context Viewpoint](../viewpoints/context/README.md) - 系統上下文

### 🎯 Quality Perspectives

- [Perspectives Overview](../perspectives/README.md) - 所有品質 perspectives
- [Security Perspective](../perspectives/security/README.md) - 安全關注點
- [Performance Perspective](../perspectives/performance/README.md) - 效能優化
- [Availability Perspective](../perspectives/availability/README.md) - 高可用性
- [Evolution Perspective](../perspectives/evolution/README.md) - 系統演進
- [Accessibility Perspective](../perspectives/accessibility/README.md) - API 可存取性
- [Development Resource Perspective](../perspectives/development-resource/README.md) - 開發資源
- [Internationalization Perspective](../perspectives/internationalization/README.md) - I18n 支援
- [Location Perspective](../perspectives/location/README.md) - 地理分佈

### 📋 Architecture Decision Records

- [ADRs Overview](adrs/README.md) - 所有架構決策
- [ADR Roadmap](adrs/ADR-ROADMAP.md) - 已規劃和已完成的 ADRs
- [ADR Template](../templates/adr-template.md) - 新 ADRs 的範本

## 架構概述

### 系統架構

GenAI Demo 平台採用以下架構：

- **Hexagonal Architecture** (Ports and Adapters)
- **Domain-Driven Design** (DDD) tactical patterns
- **Event-Driven Architecture** 用於跨 context 通訊
- **Microservices** 部署在 AWS EKS 上
- **Multi-Region Active-Active** 實現高可用性

### 核心架構原則

1. **Domain-Centric Design**：業務邏輯位於 domain 層
2. **Dependency Inversion**：Domain 層沒有基礎設施依賴
3. **Event-Driven Communication**：Bounded contexts 透過事件通訊
4. **Infrastructure as Code**：使用 AWS CDK 管理基礎設施
5. **Cloud-Native**：專為雲端部署而設計

### 技術堆疊

#### Backend

- **Language**：Java 21
- **Framework**：Spring Boot 3.4.5
- **Build Tool**：Gradle 8.x
- **Database**：PostgreSQL (RDS)
- **Cache**：Redis (ElastiCache)
- **Messaging**：Apache Kafka (MSK)

#### Frontend

- **CMC Management**：Next.js 14 + React 18 + TypeScript
- **Consumer App**：Angular 18 + TypeScript
- **UI Components**：shadcn/ui + Radix UI

#### Infrastructure

- **Cloud Provider**：AWS
- **Container Orchestration**：Amazon EKS
- **Infrastructure as Code**：AWS CDK
- **Observability**：CloudWatch + X-Ray + Grafana

## Architectural Viewpoints

### Functional Viewpoint

描述系統的功能能力和職責。

**核心要素**：
- Bounded contexts 及其職責
- 使用案例和業務流程
- Domain 模型和 aggregates
- 外部介面

[完整 Functional Viewpoint](../viewpoints/functional/README.md)

### Information Viewpoint

描述系統如何儲存、操作和分發資訊。

**核心要素**：
- 資料模型和實體關係
- 組件之間的資料流
- 資料生命週期和持久化
- 事件資料結構

[完整 Information Viewpoint](../viewpoints/information/README.md)

### Concurrency Viewpoint

描述並發結構以及系統如何處理並發請求。

**核心要素**：
- Thread pools 和非同步處理
- 事件處理並發
- 分散式鎖定策略
- 交易管理

[完整 Concurrency Viewpoint](../viewpoints/concurrency/README.md)

### Development Viewpoint

從開發者角度描述架構。

**核心要素**：
- 模組結構和依賴關係
- 建置流程和工具
- 開發環境
- 程式碼組織

[完整 Development Viewpoint](../viewpoints/development/README.md)

### Deployment Viewpoint

描述系統如何部署到執行環境。

**核心要素**：
- 部署架構
- 基礎設施組件
- 網路拓撲
- 環境配置

[完整 Deployment Viewpoint](../viewpoints/deployment/README.md)

### Operational Viewpoint

描述系統如何運營、監控和維護。

**核心要素**：
- 監控和告警
- 備份和恢復
- 事件回應
- 維護程序

[完整 Operational Viewpoint](../viewpoints/operational/README.md)

### Context Viewpoint

描述系統與其環境的關係。

**核心要素**：
- 系統邊界
- 外部系統和整合
- 利害關係人
- 外部依賴

[完整 Context Viewpoint](../viewpoints/context/README.md)

## Quality Perspectives

### Security Perspective

處理所有 viewpoints 的安全關注點。

**核心關注點**：
- 認證和授權
- 資料保護和加密
- 網路安全
- 合規要求

[完整 Security Perspective](../perspectives/security/README.md)

### Performance Perspective

處理效能和可擴展性關注點。

**核心關注點**：
- 回應時間要求
- 吞吐量容量
- 資源利用率
- 可擴展性策略

[完整 Performance Perspective](../perspectives/performance/README.md)

### Availability Perspective

處理系統可用性和可靠性。

**核心關注點**：
- 高可用性架構
- 災難恢復
- 容錯能力
- 業務連續性

[完整 Availability Perspective](../perspectives/availability/README.md)

### Evolution Perspective

處理系統如何隨時間演進。

**核心關注點**：
- 可擴展性機制
- 版本管理
- 遷移策略
- 技術債務管理

[完整 Evolution Perspective](../perspectives/evolution/README.md)

## Architecture Decision Records

### 什麼是 ADRs？

Architecture Decision Records 記錄重要的架構決策，包括：
- 上下文和問題陳述
- 考慮的選項
- 決策理由
- 後果和權衡

### ADR 類別

#### Data Storage (8 ADRs)

- PostgreSQL 作為主要資料庫
- Redis 用於分散式快取
- Kafka 用於事件串流
- Event store 實作

[Data Storage ADRs](adrs/README.md#data-storage)

#### Architecture Patterns (12 ADRs)

- Hexagonal architecture 採用
- Domain events 通訊
- CQRS pattern 實作
- Saga pattern 用於分散式交易

[Architecture Patterns ADRs](adrs/README.md#architecture-patterns)

#### Infrastructure (15 ADRs)

- AWS 雲端基礎設施
- 使用 EKS 的容器編排
- Multi-region 部署
- Progressive deployment 策略

[Infrastructure ADRs](adrs/README.md#infrastructure)

#### Security (10 ADRs)

- JWT 認證策略
- RBAC 實作
- 資料加密標準
- WAF 規則和政策

[Security ADRs](adrs/README.md#security)

#### Observability (8 ADRs)

- Observability 平台選擇
- 分散式追蹤策略
- 日誌聚合方法
- Multi-region observability

[Observability ADRs](adrs/README.md#observability)

#### Multi-Region (7 ADRs)

- Active-active 架構
- 跨區域資料複製
- 區域容錯移轉策略
- 業務連續性規劃

[Multi-Region ADRs](adrs/README.md#multi-region)

### 最近的 ADRs

- [ADR-060: Cost Optimization Strategy](adrs/060-cost-optimization-strategy.md)
- [ADR-059: Compliance Automation](adrs/059-compliance-automation-strategy.md)
- [ADR-058: Security Compliance Audit](adrs/058-security-compliance-audit-strategy.md)
- [ADR-057: Data Retention Policy](adrs/057-data-retention-policy-implementation.md)
- [ADR-056: Network Segmentation](adrs/056-network-segmentation-isolation-strategy.md)

[所有 ADRs](adrs/README.md)

## 架構模式

### Domain-Driven Design

我們遵循 DDD tactical patterns：

- **Aggregates**：一致性邊界
- **Entities**：具有身份的物件
- **Value Objects**：不可變物件
- **Domain Events**：業務事件
- **Repositories**：資料存取介面
- **Domain Services**：跨 aggregate 邏輯
- **Application Services**：使用案例編排

### Hexagonal Architecture

層次和依賴關係：

```
interfaces/ (REST API, Web UI)
    ↓
application/ (Use Cases)
    ↓
domain/ (Business Logic) ← infrastructure/ (Technical Implementations)
```

### Event-Driven Architecture

- **Domain Events**：由 aggregates 發布
- **Event Handlers**：對事件作出反應
- **Event Store**：持久化事件
- **Event Sourcing**：從事件重建狀態

## 架構治理

### 架構審查流程

1. **提案**：為重大決策提交 ADR
2. **審查**：架構團隊審查
3. **討論**：利害關係人意見
4. **決策**：批准、拒絕或延期
5. **實作**：執行決策
6. **驗證**：驗證實作

### 架構合規性

- **ArchUnit Tests**：自動化架構測試
- **Code Reviews**：在 PRs 中進行架構審查
- **定期稽核**：每季度進行架構稽核
- **指標**：追蹤架構指標

### 架構演進

- **持續改進**：定期回顧
- **Technology Radar**：追蹤新興技術
- **Proof of Concepts**：驗證新方法
- **遷移計畫**：規劃架構演進

## 入門指南

### 對於架構師

1. **審查 Viewpoints**：理解所有 viewpoints
2. **研究 ADRs**：學習過去的決策
3. **審查 Perspectives**：理解品質關注點
4. **參與審查**：加入架構審查

### 對於開發者

1. **理解架構**：閱讀 viewpoints
2. **遵循模式**：使用已建立的模式
3. **參考 ADRs**：檢查相關決策
4. **提出問題**：釐清架構關注點

### 對於新團隊成員

1. **從概述開始**：閱讀本文檔
2. **研究 Functional Viewpoint**：理解業務
3. **審查 Development Viewpoint**：學習結構
4. **閱讀關鍵 ADRs**：理解主要決策

## 相關文檔

### 開發文檔

- [Development Guide](../development/README.md)
- [Coding Standards](../development/coding-standards/README.md)
- [Testing Strategy](../development/testing/README.md)

### 運營文檔

- [Operations Guide](../operations/README.md)
- [Deployment Procedures](../operations/deployment/README.md)
- [Runbooks](../operations/runbooks/README.md)

### API 文檔

- [API Overview](../api/README.md)
- [REST API](../api/rest/README.md)
- [Domain Events](../api/events/README.md)

## 工具和資源

### 架構工具

- **PlantUML**：圖表生成
- **Mermaid**：簡單圖表
- **ArchUnit**：架構測試
- **SonarQube**：程式碼品質分析

### 文檔工具

- **Markdown**：文檔格式
- **GitHub**：版本控制和協作
- **Kiro**：AI 輔助開發

### 外部資源

- [Rozanski & Woods Book](https://www.viewpoints-and-perspectives.info/)
- [Domain-Driven Design](https://www.domainlanguage.com/ddd/)
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [AWS Well-Architected Framework](https://aws.amazon.com/architecture/well-architected/)

## 貢獻

### 新增 ADRs

1. 使用 [ADR template](../templates/adr-template.md)
2. 遵循 ADR 編號慣例
3. 包含所有必需章節
4. 提交架構審查
5. 更新 [ADR index](adrs/README.md)

### 更新架構文檔

1. 遵循 [style guide](../STYLE-GUIDE.md)
2. 更新相關 viewpoints
3. 建立/更新圖表
4. 提交 PR 進行審查
5. 更新相關文檔

### 提出架構變更

1. 建立 ADR 提案
2. 向架構團隊呈現
3. 收集利害關係人回饋
4. 根據回饋修訂
5. 獲得批准並實作

---

**文檔負責人**：Architecture Team
**上次審查**：2025-01-17
**下次審查**：2025-04-17
**狀態**：Active
