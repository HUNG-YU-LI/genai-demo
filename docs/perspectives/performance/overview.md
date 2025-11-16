---
title: "Performance & Scalability Perspective"
type: "perspective"
category: "performance"
affected_viewpoints: ["functional", "information", "concurrency", "deployment", "operational"]
last_updated: "2025-10-23"
version: "1.0"
status: "active"
owner: "Performance Engineering Team"
related_docs:
  - "../../viewpoints/functional/overview.md"
  - "../../viewpoints/deployment/overview.md"
  - "../../viewpoints/concurrency/overview.md"
tags: ["performance", "scalability", "optimization", "caching", "load-testing"]
---

# Performance & Scalability Perspective

> **狀態**: ✅ Active
> **最後更新**: 2025-10-23
> **負責人**: Performance Engineering Team

## 概述

Performance & Scalability Perspective 處理系統在不同負載下滿足回應時間需求的能力，以及隨著需求增長有效擴展的能力。對於電子商務平台，performance 直接影響使用者體驗、轉換率和業務成功。本觀點確保系統能夠處理銷售活動期間的尖峰負載，同時保持可接受的回應時間。

Performance 和 scalability 透過多種策略實現，包括高效演算法、caching、非同步處理、database 最佳化、horizontal scaling 和 load balancing。系統設計為可從數百個擴展到數千個並發使用者，而不會降低使用者體驗。

## 目的

本觀點確保：

- **回應性**: 所有使用者互動的快速回應時間
- **吞吐量**: 高交易處理容量
- **擴展性**: 處理不斷增長的使用者群和資料量的能力
- **效率**: 最佳資源利用
- **可預測性**: 在不同負載下的一致 performance
- **成本效益**: 高效擴展而不會產生過高的基礎設施成本

## 利害關係人

### 主要利害關係人

- **終端使用者**: 期望快速、回應迅速的應用程式
- **業務負責人**: 關注轉換率和客戶滿意度
- **開發團隊**: 負責實作 performance 最佳化
- **維運團隊**: 管理基礎設施擴展和監控

### 次要利害關係人

- **產品經理**: 根據業務需求定義 performance 需求
- **行銷團隊**: 規劃可能導致流量高峰的活動
- **財務團隊**: 關注基礎設施成本
- **客戶支援**: 處理關於 performance 緩慢的投訴

## 目錄

### 📄 文檔

- [Requirements](requirements.md) - Performance 目標和 quality attribute scenarios
- [Scalability](scalability.md) - Horizontal 和 vertical scaling 策略
- [Optimization](optimization.md) - Performance 最佳化技術
- [Verification](verification.md) - Load testing 和 performance 驗證

### 📊 圖表

- [Caching Architecture](../../diagrams/perspectives/performance/caching-architecture.puml) - 多層 caching 策略
- [Scaling Strategy](../../diagrams/perspectives/performance/scaling-strategy.puml) - Auto-scaling 配置
- [Database Optimization](../../diagrams/perspectives/performance/database-optimization.puml) - Query 最佳化和 indexing
- [Load Distribution](../../diagrams/perspectives/performance/load-distribution.puml) - Load balancing 架構

## 關鍵關注點

### 關注點 1: API 回應時間

**描述**: 確保所有 API endpoints 在可接受的時間限制內回應，以提供良好的使用者體驗。回應時間包括處理時間、database queries 和網路延遲。

**影響**: 緩慢的 API 回應導致不良的使用者體驗、放棄的購物車和收入損失。研究顯示，1 秒的延遲可使轉換率降低 7%。

**優先級**: 高

**影響的 Viewpoints**: Functional、Concurrency、Deployment

### 關注點 2: Database Performance

**描述**: 隨著資料量增長，維持快速的 database query performance。這包括 query 最佳化、適當的 indexing 和高效的資料存取模式。

**影響**: Database 瓶頸可能連鎖影響整個系統 performance，導致所有功能的 timeouts 和使用者體驗降級。

**優先級**: 高

**影響的 Viewpoints**: Information、Concurrency、Operational

### 關注點 3: Horizontal Scalability

**描述**: 無需程式碼變更或架構修改即可增加更多應用程式實例以處理增加的負載的能力。

**影響**: 沒有 horizontal scalability，系統無法處理銷售活動期間的流量高峰，導致中斷和收入損失。

**優先級**: 高

**影響的 Viewpoints**: Deployment、Concurrency、Operational

### 關注點 4: Caching 有效性

**描述**: 實作多層 caching 以減少 database 負載並改善經常存取資料的回應時間。

**影響**: 無效的 caching 導致不必要的 database queries、增加的延遲和更高的基礎設施成本。

**優先級**: 高

**影響的 Viewpoints**: Functional、Information、Deployment

### 關注點 5: 非同步處理

**描述**: 對長時間執行的操作使用非同步處理，以避免阻塞使用者請求並改善感知 performance。

**影響**: 重型操作的同步處理會阻塞使用者請求，導致 timeouts 和不良的使用者體驗。

**優先級**: 中

**影響的 Viewpoints**: Functional、Concurrency

### 關注點 6: 資源利用

**描述**: 有效使用 CPU、memory 和網路資源，以最大化吞吐量同時最小化基礎設施成本。

**影響**: 低效的資源使用導致更高的成本並限制系統容量。

**優先級**: 中

**影響的 Viewpoints**: Deployment、Operational

## Quality Attribute 需求

### 需求 1: API 回應時間

**描述**: 所有 API endpoints 必須在正常和尖峰負載條件下的指定時間限制內回應。

**目標**:

- Critical APIs (產品搜尋、結帳): ≤ 500ms (95th percentile)
- Standard APIs (產品詳情、購物車操作): ≤ 1000ms (95th percentile)
- Background APIs (訂單歷史、analytics): ≤ 2000ms (95th percentile)

**理由**: 快速的回應時間對使用者體驗和轉換率至關重要。這些目標基於行業基準和使用者期望。

**驗證**: Load testing、APM 監控、performance benchmarks

### 需求 2: 吞吐量容量

**描述**: 系統必須處理指定數量的並發使用者和每秒交易數。

**目標**:

- 並發使用者: 10,000 個同時使用者
- 每秒交易數: 1,000 TPS 持續，2,000 TPS 尖峰
- 訂單處理: 每分鐘 500 個訂單
- 搜尋 queries: 每分鐘 5,000 個 queries

**理由**: 基於預計的使用者增長和銷售活動期間的尖峰流量。

**驗證**: Load testing、stress testing、production 監控

### 需求 3: Database Query Performance

**描述**: Database queries 必須在可接受的時間限制內執行，以避免瓶頸。

**目標**:

- 簡單 queries (單表、indexed): ≤ 10ms (95th percentile)
- 複雜 queries (joins、aggregations): ≤ 100ms (95th percentile)
- 報表 queries: ≤ 1000ms (95th percentile)
- Connection pool 利用率: ≤ 80%

**理由**: Database performance 直接影響整體系統 performance。這些目標確保 database 不是瓶頸。

**驗證**: Query profiling、slow query logs、database 監控

### 需求 4: Scalability

**描述**: 系統必須 horizontally scale 以在可接受的時間內處理 10 倍流量增長。

**目標**:

- Auto-scale 從 2 到 20 個實例
- Scale-up 時間: ≤ 5 分鐘
- Scale-down 時間: ≤ 10 分鐘
- Scaling 期間無 performance 降級
- 高達 20 個實例的線性 scalability

**理由**: 能夠在不需手動介入的情況下處理銷售活動期間的流量高峰。

**驗證**: Load testing with auto-scaling、production 監控

### 需求 5: Cache Hit Rate

**描述**: Caching 必須有效減少 database 負載並改善回應時間。

**目標**:

- 產品目錄 cache hit rate: ≥ 90%
- 使用者 session cache hit rate: ≥ 95%
- API 回應 cache hit rate: ≥ 80%
- Cache invalidation 時間: ≤ 1 秒

**理由**: 高 cache hit rates 顯著減少 database 負載並改善回應時間。

**驗證**: Cache 監控、hit rate 分析

## Quality Attribute Scenarios

### Scenario 1: Flash Sale 流量高峰

**來源**: 行銷活動公告

**刺激**: 流量在 10 分鐘內從 1,000 個並發使用者增加到 10,000 個

**環境**: Flash sale 活動期間的 production 系統

**產物**: Web 應用程式和 API services

**回應**: 系統 auto-scales 以處理增加的負載，維持回應時間

**回應衡量**:

- Auto-scaling 在 2 分鐘內觸發
- 在 5 分鐘內部署額外的實例
- API 回應時間保持 ≤ 1000ms (95th percentile)
- 零服務中斷
- 成功率 ≥ 99.5%

**優先級**: 高

**狀態**: ✅ 已實作

### Scenario 2: 負載下的 Database Query Performance

**來源**: 多個並發使用者

**刺激**: 1,000 個並發產品搜尋 queries

**環境**: 具有完整產品目錄 (1M 產品) 的 production 系統

**產物**: Database 和搜尋服務

**回應**: 系統使用 indexes 和 caching 有效執行 queries

**回應衡量**:

- Query 回應時間 ≤ 100ms (95th percentile)
- Database CPU 利用率 ≤ 70%
- Cache hit rate ≥ 90%
- 零 query timeouts

**優先級**: 高

**狀態**: ✅ 已實作

### Scenario 3: 結帳流程 Performance

**來源**: 客戶

**刺激**: 完成購物車中有 5 個項目的結帳

**環境**: 尖峰流量 (5,000 並發使用者)

**產物**: 結帳服務、payment gateway 整合

**回應**: 系統快速處理結帳並進行所有驗證

**回應衡量**:

- 總結帳時間 ≤ 3 秒
- Payment 處理 ≤ 2 秒
- 訂單確認 ≤ 1 秒
- 成功率 ≥ 99.9%

**優先級**: 高

**狀態**: ✅ 已實作

### Scenario 4: Cache Invalidation

**來源**: 產品經理

**刺激**: 更新 100 個產品的價格

**環境**: 有活躍使用者瀏覽產品的 production 系統

**產物**: 產品服務和 cache 層

**回應**: 系統 invalidates cache 並更新所有實例

**回應衡量**:

- Cache invalidation 在 1 秒內傳播
- 2 秒後不提供過時資料
- 對正在進行的使用者 sessions 零影響
- Cache 重建時間 ≤ 5 秒

**優先級**: 中

**狀態**: ✅ 已實作

### Scenario 5: 長時間執行的報表生成

**來源**: 業務分析師

**刺激**: 請求過去 12 個月的銷售報表

**環境**: 營業時間的 production 系統

**產物**: 報表服務

**回應**: 系統非同步處理報表而不阻塞

**回應衡量**:

- 請求立即接受 (≤ 100ms)
- 在 5 分鐘內生成報表
- 完成時通知使用者
- 對其他操作零影響

**優先級**: 中

**狀態**: 🚧 進行中

## 設計決策

### 決策 1: Redis for Distributed Caching

**背景**: 需要快速、分散式 caching 以減少 database 負載並改善多個應用程式實例的回應時間。

**決策**: 實作 Redis 作為 session 資料、產品目錄和 API 回應的分散式 cache。

**理由**:

- In-memory performance (sub-millisecond latency)
- 分散式架構支援 horizontal scaling
- 豐富的資料結構 (strings、hashes、sets、sorted sets)
- 內建的 expiration 和 eviction policies
- 在電子商務應用程式中已證明可擴展

**取捨**:

- ✅ 獲得: 優秀的 performance、scalability、靈活性
- ❌ 犧牲: 額外的基礎設施元件、cache 一致性複雜性

**對 Quality Attribute 的影響**: 顯著改善回應時間並減少 database 負載，實現更高的吞吐量。

**相關 ADR**: ADR-004: Use Redis for Distributed Caching

### 決策 2: Horizontal Auto-Scaling with EKS

**背景**: 需要在不過度配置基礎設施的情況下有效處理可變流量負載。

**決策**: 在 AWS EKS 上部署應用程式，基於 CPU 和自訂 metrics 使用 Horizontal Pod Autoscaler (HPA)。

**理由**:

- 基於實際需求的自動 scaling
- 成本效益 (僅為使用的資源付費)
- 快速 scaling (幾分鐘內新增 pods)
- Kubernetes-native 解決方案
- 支援自訂 metrics (request rate、queue depth)

**取捨**:

- ✅ 獲得: 成本效率、自動 scaling、靈活性
- ❌ 犧牲: 配置複雜性、cold start 時間

**對 Quality Attribute 的影響**: 使系統能夠處理 10 倍流量高峰同時最佳化成本。

**相關 ADR**: ADR-016: Kubernetes Auto-Scaling Strategy

### 決策 3: Database Read Replicas

**背景**: 讀取密集的工作負載，90% 讀取和 10% 寫入導致 database 瓶頸。

**決策**: 在應用程式層實作 PostgreSQL read replicas 和讀寫分離。

**理由**:

- 在多個 database 實例之間分配讀取負載
- 減少 primary database 的負載
- 改善讀取 query performance
- 維持資料一致性 (eventual consistency 對讀取可接受)

**取捨**:

- ✅ 獲得: 更好的讀取 performance、更高的吞吐量、fault tolerance
- ❌ 犧牲: Replication lag (通常 <1 秒)、增加的複雜性

**對 Quality Attribute 的影響**: 改善 database query performance 和整體系統吞吐量。

**相關 ADR**: ADR-017: Database Read Replica Strategy

### 決策 4: Asynchronous Event Processing with Kafka

**背景**: 需要處理事件 (訂單已下、庫存已更新) 而不阻塞使用者請求。

**決策**: 使用 Apache Kafka (AWS MSK) 進行非同步事件處理，配合 consumer groups。

**理由**:

- 解耦事件 producers 和 consumers
- 高吞吐量 (每秒數百萬個事件)
- 持久的 message 儲存
- 支援多個 consumers
- 實現 event-driven 架構

**取捨**:

- ✅ 獲得: 更好的回應性、scalability、resilience
- ❌ 犧牲: Eventual consistency、增加的複雜性

**對 Quality Attribute 的影響**: 透過將重型處理卸載到背景 workers 來改善 API 回應時間。

**相關 ADR**: ADR-005: Use Apache Kafka for Event Streaming

## 實作指南

### 架構模式

- **Caching Strategy**: 多層 caching (browser、CDN、application、database)
- **Database Optimization**: Query 最佳化、indexing、connection pooling
- **Asynchronous Processing**: 非關鍵操作的 event-driven 架構
- **Load Balancing**: 在多個實例之間分配流量
- **Circuit Breaker**: 防止來自緩慢依賴項的連鎖故障
- **Bulkhead**: 隔離資源以防止資源耗盡

### 最佳實務

1. **Cache 經常存取的資料**: 產品目錄、使用者 sessions、API 回應
2. **最佳化 Database Queries**: 使用 indexes、避免 N+1 queries、使用分頁
3. **使用 Connection Pooling**: 有效重用 database connections
4. **實作 Lazy Loading**: 僅在需要時載入資料
5. **壓縮回應**: 對 API 回應使用 gzip 壓縮
6. **最佳化圖片**: 使用 CDN 和適當的圖片格式
7. **監控 Performance**: 持續監控並發出警報
8. **定期 Load Test**: 在實際負載下驗證 performance

### 應避免的反模式

- ❌ **過早最佳化**: 基於實際瓶頸進行最佳化，而非假設
- ❌ **過度 Caching**: Caching 所有內容導致過時資料和 memory 問題
- ❌ **同步重型操作**: 對長時間執行的任務使用 async 處理
- ❌ **N+1 Query 問題**: 始終使用 eager loading 或 batch queries
- ❌ **忽略 Indexes**: 缺少 indexes 導致緩慢的 queries
- ❌ **大型 Transactions**: 保持 transactions 小而專注
- ❌ **阻塞 I/O**: 使用 non-blocking I/O 以獲得更好的並發性

### 程式碼範例

#### 範例 1: Caching with Redis

```java
@Service
@CacheConfig(cacheNames = "products")
public class ProductService {

    @Cacheable(key = "#productId", unless = "#result == null")
    public Product findById(String productId) {
        return productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    @Cacheable(key = "'search:' + #query + ':' + #pageable.pageNumber")
    public Page<Product> searchProducts(String query, Pageable pageable) {
        return productRepository.searchByName(query, pageable);
    }

    @CacheEvict(key = "#product.id")
    public Product updateProduct(Product product) {
        return productRepository.save(product);
    }
}
```

#### 範例 2: Async Processing

```java
@Service
public class OrderService {

    @Async("orderProcessingExecutor")
    public CompletableFuture<Void> processOrderAsync(String orderId) {
        return CompletableFuture.runAsync(() -> {
            Order order = orderRepository.findById(orderId).orElseThrow();

            // Heavy processing
            inventoryService.reserveItems(order);
            paymentService.processPayment(order);
            notificationService.sendConfirmation(order);

        }, orderProcessingExecutor);
    }
}
```

## 驗證和測試

### 驗證方法

- **Load Testing**: 使用 JMeter 或 Gatling 模擬實際使用者負載
- **Stress Testing**: 測試系統限制和臨界點
- **Endurance Testing**: 驗證長時間的 performance
- **Spike Testing**: 測試對突然流量增加的回應
- **APM Monitoring**: Production 中的持續 performance 監控

### 測試策略

#### Load Testing

**目的**: 驗證系統在預期負載下滿足 performance 需求

**方法**:

- 模擬 10,000 個並發使用者
- 混合操作 (瀏覽、搜尋、結帳)
- 10 分鐘的 ramp-up 期
- 持續負載 30 分鐘

**成功標準**:

- 回應時間 ≤ 1000ms (95th percentile)
- 吞吐量 ≥ 1000 TPS
- 錯誤率 < 0.1%
- 資源利用率 < 80%

**頻率**: 每週 + 主要發布前

#### Stress Testing

**目的**: 識別系統臨界點和故障模式

**方法**:

- 逐漸增加負載超過容量
- 監控系統行為和故障點
- 識別瓶頸和限制

**成功標準**:

- 在極端負載下優雅降級
- 無資料損壞
- 負載減少後快速恢復

**頻率**: 每月

### Metrics 和監控

| Metric | 目標 | 測量方法 | 警報閾值 |
|--------|--------|-------------------|-----------------|
| API 回應時間 (p95) | ≤ 1000ms | APM (New Relic/DataDog) | > 2000ms |
| API 回應時間 (p99) | ≤ 2000ms | APM | > 3000ms |
| 吞吐量 | ≥ 1000 TPS | Application metrics | < 500 TPS |
| 錯誤率 | < 0.1% | Application logs | > 1% |
| Database Query 時間 (p95) | ≤ 100ms | Database 監控 | > 200ms |
| Cache Hit Rate | ≥ 90% | Redis metrics | < 80% |
| CPU 利用率 | ≤ 70% | CloudWatch | > 85% |
| Memory 利用率 | ≤ 80% | CloudWatch | > 90% |
| Auto-Scaling 事件 | 根據需要 | Kubernetes metrics | N/A |

## 受影響的 Viewpoints

### [Functional Viewpoint](../../viewpoints/functional/overview.md)

**此觀點的應用方式**:
所有功能能力都必須考慮 performance，使用高效演算法和適當的 caching 策略。

**具體關注點**:

- API endpoint performance
- 搜尋功能 performance
- 結帳流程速度
- 產品目錄瀏覽

**實作指導**:

- 對經常存取的資料使用 caching
- 對大型結果集實作分頁
- 使用適當的 indexes 最佳化 database queries
- 對非關鍵操作使用 async 處理

### [Information Viewpoint](../../viewpoints/information/overview.md)

**此觀點的應用方式**:
資料模型和存取模式必須針對 performance 進行最佳化，使用適當的 indexing 和高效的 queries。

**具體關注點**:

- Database query performance
- 資料存取模式
- Index 策略
- 資料量增長

**實作指導**:

- 在經常查詢的欄位上建立 indexes
- 使用 database query 最佳化技術
- 對讀取密集的工作負載實作 read replicas
- 監控緩慢的 queries 並最佳化

### [Concurrency Viewpoint](../../viewpoints/concurrency/overview.md)

**此觀點的應用方式**:
並發操作必須有效處理，而不會阻塞或資源競爭。

**具體關注點**:

- Thread pool 配置
- Async 處理
- Lock 競爭
- 資源 pooling

**實作指導**:

- 使用適當的 thread pool 大小
- 對長時間操作實作 async 處理
- 最小化 lock 競爭
- 使用 connection pooling

### [Deployment Viewpoint](../../viewpoints/deployment/overview.md)

**此觀點的應用方式**:
基礎設施必須配置以實現最佳 performance 和 scalability。

**具體關注點**:

- Auto-scaling 配置
- Load balancing
- 資源分配
- 網路 performance

**實作指導**:

- 使用適當的 metrics 配置 HPA
- 使用 Application Load Balancer
- 為每個 pod 分配足夠的資源
- 最佳化網路配置

### [Operational Viewpoint](../../viewpoints/operational/overview.md)

**此觀點的應用方式**:
營運必須包括 performance 監控、警報和最佳化程序。

**具體關注點**:

- Performance 監控
- 警報配置
- Performance 故障排除
- 容量規劃

**實作指導**:

- 實作全面的 APM 監控
- 配置 performance 降級的警報
- 建立 performance 故障排除程序
- 定期容量規劃審查

## 相關文檔

### 相關 Perspectives

- [Availability Perspective](../availability/overview.md) - Performance 影響 availability
- [Cost Perspective](../cost/overview.md) - Performance 最佳化影響成本
- [Scalability Perspective](scalability.md) - 詳細的 scaling 策略

### 相關 Architecture Decisions

- [ADR-004: Use Redis for Distributed Caching](../../architecture/adrs/ADR-004-redis-caching.md)
- [ADR-005: Use Apache Kafka for Event Streaming](../../architecture/adrs/ADR-005-kafka-messaging.md)
- [ADR-016: Kubernetes Auto-Scaling Strategy](../../architecture/adrs/ADR-016-k8s-autoscaling.md)
- [ADR-017: Database Read Replica Strategy](../../architecture/adrs/ADR-017-db-read-replicas.md)

### 相關標準和指南

- [Performance Standards](../../.kiro/steering/performance-standards.md) - 詳細的 performance 標準
- [Test Performance Standards](../../.kiro/steering/test-performance-standards.md) - 測試 performance 指南

### 相關工具

- JMeter: Load testing 工具
- Gatling: Performance testing framework
- New Relic / DataDog: APM 監控
- Redis: Distributed caching
- Prometheus + Grafana: Metrics 和監控

## 已知問題和限制

### 目前限制

- **Cold Start 時間**: 新的 pod 實例需要 30-60 秒才能就緒
- **Cache Warm-up**: 重啟後 cache 需要時間才能達到最佳 hit rate
- **Database Connection 限制**: 每個 database 實例最多 100 個 connections

### 技術債務

- **Query 最佳化**: 某些複雜的報表 queries 需要最佳化
- **Cache 策略**: 需要實作關鍵資料的 cache warming
- **監控缺口**: 某些自訂 metrics 尚未實作

### 風險

| 風險 | 機率 | 影響 | 緩解策略 |
|------|-------------|--------|-------------------|
| Database 成為瓶頸 | 中 | 高 | 實作 read replicas、最佳化 queries、增加 caching |
| Cache 故障影響 performance | 低 | 高 | 實作 cache fallback、監控 cache 健康狀態 |
| Auto-scaling 太慢 | 中 | 中 | 調整 HPA 參數、實作預測性 scaling |
| 第三方 API 緩慢 | 中 | 中 | 實作 circuit breaker、caching、timeouts |

## 未來考量

### 計劃改進

- **預測性 Auto-Scaling**: 使用 ML 預測流量並主動 scale (Q2 2025)
- **Edge Caching**: 對靜態內容實作 CloudFront (Q2 2025)
- **Database Sharding**: 大規模的 horizontal database 分區 (Q3 2025)
- **GraphQL**: 使用 GraphQL 最佳化 API queries (Q4 2025)

### 演化策略

Performance perspective 將演化以應對不斷增長的規模和新興技術:

- 基於 production metrics 的持續 performance 最佳化
- 採用新的 caching 技術 (例如 Memcached、Hazelcast)
- 實作進階 scaling 策略
- 整合 AI/ML 用於 performance 預測和最佳化

### 新興技術

- **Serverless Computing**: 特定工作負載的 AWS Lambda
- **Edge Computing**: 邊緣處理的 CloudFront Functions
- **In-Memory Databases**: 超低延遲的 Redis Enterprise
- **Service Mesh**: 進階流量管理的 Istio

## 快速連結

- [返回所有 Perspectives](../README.md)
- [Architecture Overview](../../architecture/README.md)
- [主要文檔](../../README.md)
- [Performance Standards](../../.kiro/steering/performance-standards.md)

## 附錄

### 詞彙表

- **Response Time**: 從請求到回應的時間
- **Throughput**: 每秒交易數
- **Latency**: 處理延遲
- **TPS**: Transactions Per Second
- **p95/p99**: 95th/99th percentile (95%/99% 的請求比這更快)
- **Cache Hit Rate**: 從 cache 提供的請求百分比
- **APM**: Application Performance Monitoring
- **HPA**: Horizontal Pod Autoscaler

### 參考資料

- Performance Testing Guide: <https://martinfowler.com/articles/performance-testing.html>
- AWS Performance Best Practices: <https://aws.amazon.com/architecture/performance-efficiency/>
- Redis Best Practices: <https://redis.io/docs/manual/patterns/>
- Kubernetes HPA: <https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/>

### 變更歷史

| 日期 | 版本 | 作者 | 變更 |
|------|---------|--------|---------|
| 2025-10-23 | 1.0 | Performance Engineering Team | 初始版本 |

---

**模板版本**: 1.0
**最後模板更新**: 2025-01-17
