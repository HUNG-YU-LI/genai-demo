# Glossary (中英對照表)

本文件提供專案中使用的技術名詞中英對照,幫助團隊成員快速理解各項技術術語的含義。

## DDD & Domain Layer

| English Term | 中文說明 | Explanation |
|--------------|---------|-------------|
| Aggregate Root | 聚合根 | The main entity within an aggregate that ensures consistency boundaries and controls access to other entities |
| Entity | 實體 | Domain object with unique identity that persists over time |
| Value Object | 值物件 | Immutable domain object defined by its attributes rather than identity |
| Domain Event | 領域事件 | Significant occurrence within the domain that domain experts care about |
| Domain Service | 領域服務 | Service containing domain logic that doesn't naturally belong to an entity or value object |
| Repository | 儲存庫 | Interface for accessing and persisting aggregates |
| Bounded Context | 限界上下文 | Explicit boundary within which a domain model is defined and applicable |

## Architecture Patterns

| English Term | 中文說明 | Explanation |
|--------------|---------|-------------|
| Hexagonal Architecture | 六角架構 | Architecture pattern that isolates business logic from external concerns through ports and adapters |
| CQRS | 命令查詢職責分離 | Pattern separating read and write operations for different data models |
| Event Sourcing | 事件溯源 | Pattern storing state changes as a sequence of events |
| Event-Driven Architecture | 事件驅動架構 | Architecture style where events trigger and communicate between decoupled services |

## Application & Infrastructure

| English Term | 中文說明 | Explanation |
|--------------|---------|-------------|
| Application Service | 應用服務 | Service orchestrating use cases and coordinating domain objects |
| Infrastructure Layer | 基礎設施層 | Layer providing technical capabilities like persistence, messaging, and external integrations |
| Distributed Lock | 分散式鎖 | Synchronization mechanism to coordinate access to shared resources across distributed systems |
| Optimistic Locking | 樂觀鎖 | Concurrency control mechanism assuming conflicts are rare and validating before commit |
| Pessimistic Locking | 悲觀鎖 | Concurrency control mechanism that locks resources preemptively to prevent conflicts |

## Spring Framework

| English Term | 中文說明 | Explanation |
|--------------|---------|-------------|
| Spring Boot | Spring Boot 框架 | Framework simplifying Spring application development with auto-configuration and conventions |
| Dependency Injection | 依賴注入 | Design pattern where dependencies are provided to objects rather than created internally |
| Annotation | 註解 | Metadata marker providing configuration and behavior instructions to frameworks |
| Bean | Bean 組件 | Object managed by Spring's IoC container |
| Transaction Management | 事務管理 | Mechanism ensuring data consistency across multiple operations |

## Testing & Quality

| English Term | 中文說明 | Explanation |
|--------------|---------|-------------|
| Unit Test | 單元測試 | Testing individual components in isolation |
| Integration Test | 整合測試 | Testing interaction between multiple components or systems |
| BDD | 行為驅動開發 | Development approach focusing on system behavior from user perspective |
| TDD | 測試驅動開發 | Development approach where tests are written before implementation code |
| Mock Object | 模擬物件 | Simulated object replacing real dependencies in tests |
| Test Coverage | 測試覆蓋率 | Metric measuring percentage of code executed by tests |
| AAA Pattern | AAA 模式 | Test structure: Arrange-Act-Assert |
| Given-When-Then | 給定-當-則模式 | BDD-style test structure pattern |
| Assertion | 斷言 | Statement verifying expected test outcomes |

## API & Web

| English Term | 中文說明 | Explanation |
|--------------|---------|-------------|
| REST API | REST API | Web service following Representational State Transfer principles |
| HTTP Method | HTTP 方法 | Standard operations (GET, POST, PUT, DELETE, PATCH) in HTTP protocol |
| JSON | JSON 格式 | Lightweight data interchange format |
| OpenAPI | OpenAPI 規範 | Standard specification for describing REST APIs |
| Endpoint | 端點 | Specific URL path where API operations are accessible |
| Pagination | 分頁 | Technique dividing large result sets into manageable pages |
| Rate Limiting | 流量限制 | Mechanism controlling request frequency to prevent abuse |
| CORS | 跨來源資源共用 | Security mechanism allowing controlled access to resources from different origins |

## Resilience & Reliability

| English Term | 中文說明 | Explanation |
|--------------|---------|-------------|
| Circuit Breaker | 斷路器 | Pattern preventing cascading failures by temporarily blocking failing operations |
| Retry Policy | 重試策略 | Strategy defining how and when failed operations should be retried |
| Timeout | 逾時 | Maximum time allowed for an operation to complete |
| Backpressure | 背壓機制 | Flow control mechanism preventing system overload |
| Resilience | 韌性 | System's ability to handle and recover from failures |

## AWS Services

| English Term | 中文說明 | Explanation |
|--------------|---------|-------------|
| EKS | 彈性 Kubernetes 服務 | AWS managed Kubernetes service for container orchestration |
| RDS | 關聯式資料庫服務 | AWS managed relational database service |
| Aurora | Aurora 資料庫 | AWS high-performance managed relational database |
| CloudWatch | CloudWatch 監控 | AWS monitoring and observability service |
| MSK | 託管 Kafka 服務 | AWS managed Apache Kafka service for streaming data |
| ElastiCache | ElastiCache 快取 | AWS managed in-memory caching service |
| VPC | 虛擬私有雲 | Isolated virtual network within AWS cloud |

## Data & Messaging

| English Term | 中文說明 | Explanation |
|--------------|---------|-------------|
| Redis | Redis 快取 | In-memory data store used for caching and messaging |
| Kafka | Kafka 訊息佇列 | Distributed streaming platform for building event-driven systems |
| Message Queue | 訊息佇列 | Asynchronous communication pattern using queued messages |
| PostgreSQL | PostgreSQL 資料庫 | Open-source relational database system |

## Development Practices

| English Term | 中文說明 | Explanation |
|--------------|---------|-------------|
| CI/CD | 持續整合/持續部署 | Automated pipeline for building, testing, and deploying code |
| Refactoring | 重構 | Improving code structure without changing external behavior |
| Code Review | 程式碼審查 | Systematic examination of code changes by peers |
| Version Control | 版本控制 | System tracking and managing changes to code over time |

## Architecture Viewpoints (Rozanski & Woods)

| English Term | 中文說明 | Explanation |
|--------------|---------|-------------|
| Viewpoint | 視點 | Perspective for analyzing specific aspects of system architecture |
| Perspective | 觀點 | Quality attribute concern that cuts across multiple viewpoints |
| Context Viewpoint | 情境視點 | Describes system's relationships with its environment |
| Functional Viewpoint | 功能視點 | Describes system's functional elements and their responsibilities |
| Information Viewpoint | 資訊視點 | Describes how information is stored, managed, and distributed |
| Concurrency Viewpoint | 並行視點 | Describes system's concurrency and state management structure |
| Development Viewpoint | 開發視點 | Describes architecture supporting software development process |
| Deployment Viewpoint | 部署視點 | Describes environment into which system will be deployed |
| Operational Viewpoint | 營運視點 | Describes how system will be operated, administered, and supported |

---

**總計**: 56 個核心技術術語

**最後更新**: 2025-11-16
