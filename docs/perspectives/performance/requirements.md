# Performance 要求

> **最後更新**: 2025-10-23
> **狀態**: ✅ 啟用中

## 概述

本文件定義電子商務平台的具體效能要求。這些要求是可測量、可測試的,並且直接與業務目標和使用者體驗目標相關。

## 回應時間要求

### API 回應時間目標

所有 API endpoints 必須在第 95 百分位達到以下回應時間目標:

| API 類別 | 目標 (第 95 百分位) | 理由 |
|----------|-------------------|------|
| Critical APIs (auth, payment) | ≤ 500ms | 使用者信任和安全感知需要即時回饋 |
| Business APIs (orders, customers, products) | ≤ 1000ms | 標準電子商務使用者體驗期望 |
| Reporting APIs (analytics, reports) | ≤ 3000ms | 使用者預期複雜分析會有一些延遲 |
| Batch APIs (imports, exports) | ≤ 30000ms | 背景操作帶有進度指示器 |

**測量方法**: CloudWatch metrics 帶有 endpoint 和 method 的自訂維度

**驗證**:

- 生產環境持續監控
- Staging 環境負載測試
- CI/CD pipeline 中的效能回歸測試

### Database Query Performance 目標

| Query 類型 | 目標 (第 95 百分位) | 範例操作 |
|-----------|-------------------|----------|
| Simple queries (單表, 已建立索引) | ≤ 10ms | 依 ID 查詢客戶, 依 SKU 查詢產品 |
| Complex queries (joins, aggregations) | ≤ 100ms | 訂單含項目, 客戶含訂單 |
| Reporting queries (大資料集) | ≤ 1000ms | 銷售報表, 分析查詢 |

**測量方法**: RDS Performance Insights, slow query logs

**驗證**:

- Database 效能監控
- 每週 slow query log 分析
- Query execution plan 審查

### Frontend Performance 目標

基於 Google 的 Core Web Vitals 和業界最佳實務:

| 指標 | 目標 | 說明 |
|------|------|------|
| First Contentful Paint (FCP) | ≤ 1.5s | 首次內容出現的時間 |
| Largest Contentful Paint (LCP) | ≤ 2.5s | 主要內容可見的時間 |
| First Input Delay (FID) | ≤ 100ms | 頁面變為可互動的時間 |
| Cumulative Layout Shift (CLS) | ≤ 0.1 | 頁面載入期間的視覺穩定性 |
| Time to Interactive (TTI) | ≤ 3.5s | 頁面完全可互動的時間 |

**測量方法**:

- CloudWatch RUM 用於真實使用者監控
- Deployment pipeline 中的 Lighthouse CI
- CloudWatch Synthetics 進行合成監控

**驗證**:

- 生產環境真實使用者監控
- CI/CD 中的 Lighthouse scores (最低分數: 90)
- 每週效能稽核

## 吞吐量要求

### 系統容量目標

| 指標 | 目標 | 理由 |
|------|------|------|
| Peak load handling | 1000 requests/second | Black Friday 流量預測含 2x 安全餘裕 |
| Sustained load | 500 requests/second | 典型營業時間流量 |
| Concurrent users | 5000 active users | 尖峰同時使用者估計 |
| Database connections per instance | Max 20 connections | 平衡吞吐量與資源使用 |

**測量方法**: ALB metrics, application metrics

**驗證**:

- JMeter 負載測試 (每週在 staging)
- 生產流量監控
- 容量規劃審查 (每季)

### Transaction Processing 目標

| Transaction 類型 | 目標吞吐量 | 目標延遲 |
|-----------------|----------|---------|
| Order submission | 100 orders/second | ≤ 1000ms |
| Product search | 500 searches/second | ≤ 500ms |
| Cart operations | 200 operations/second | ≤ 300ms |
| Payment processing | 50 payments/second | ≤ 2000ms |

**測量方法**: 自訂 application metrics, business metrics

**驗證**:

- Transaction 監控儀表板
- Business metrics 相關性分析
- 負載測試情境

## 資源使用要求

### Compute Resources

| 資源 | 目標使用率 | 警報門檻 |
|------|----------|---------|
| CPU utilization | ≤ 70% average | > 80% for 5 minutes |
| Memory utilization | ≤ 80% average | > 90% for 5 minutes |
| JVM heap usage | ≤ 75% of max heap | > 85% for 5 minutes |
| Thread pool utilization | ≤ 80% | > 90% for 2 minutes |

**理由**: 維持流量激增的餘裕空間並確保系統穩定性

**測量方法**: CloudWatch metrics, JVM metrics via Micrometer

**驗證**:

- 持續監控
- 資源使用報告 (每週)
- 容量規劃審查

### Database Resources

| 資源 | 目標 | 警報門檻 |
|------|------|---------|
| Database CPU | ≤ 70% average | > 80% for 5 minutes |
| Database connections | ≤ 80% of max | > 90% for 2 minutes |
| Read IOPS | ≤ 70% of provisioned | > 85% for 5 minutes |
| Write IOPS | ≤ 70% of provisioned | > 85% for 5 minutes |
| Storage utilization | ≤ 80% | > 85% |

**測量方法**: RDS CloudWatch metrics, Performance Insights

**驗證**:

- Database 效能監控
- 容量規劃審查
- Storage 成長分析

### Cache Performance

| 指標 | 目標 | 警報門檻 |
|------|------|---------|
| Cache hit rate | ≥ 85% | < 80% for 10 minutes |
| Cache memory usage | ≤ 80% | > 90% for 5 minutes |
| Cache eviction rate | < 5% of requests | > 10% for 10 minutes |
| Cache response time | ≤ 5ms (第 95 百分位) | > 10ms for 5 minutes |

**測量方法**: Redis INFO stats, custom metrics

**驗證**:

- Cache 效能監控
- Cache hit rate 分析
- Cache sizing 審查

## 可擴展性要求

### Horizontal Scaling

| 要求 | 目標 | 驗證方法 |
|------|------|---------|
| Auto-scale range | 2 to 20 instances | Load testing |
| Scale-out time | < 5 minutes | Auto-scaling tests |
| Scale-in time | < 10 minutes (with connection draining) | Auto-scaling tests |
| Scaling trigger (CPU) | 70% average over 3 minutes | Production monitoring |
| Scaling trigger (Memory) | 80% average over 3 minutes | Production monitoring |

**測量方法**: Kubernetes metrics, auto-scaling events

**驗證**:

- Auto-scaling 負載測試 (每週)
- 生產 scaling events 分析
- Scaling policy 調整

### Database Scaling

| 要求 | 目標 | 實作方式 |
|------|------|---------|
| Read replica count | 1-3 replicas | Based on read load |
| Replication lag | < 2 seconds | RDS monitoring |
| Read/write split | 80% reads to replicas | Application routing |
| Failover time | < 2 minutes | RDS Multi-AZ |

**測量方法**: RDS metrics, replication lag monitoring

**驗證**:

- Replication lag 監控
- Failover 測試 (每季)
- Read/write 分佈分析

## Quality Attribute Scenarios

### Scenario 1: Peak Traffic Handling

**情境**: Black Friday 促銷,流量為正常的 10 倍

**刺激**: 流量在 1 小時內從 100 增加到 1000 同時使用者

**預期回應**:

- 系統在 5 分鐘內從 2 個 instances 自動擴展到 10 個
- 第 95 百分位回應時間保持 ≤ 1000ms
- 沒有因容量問題導致的失敗請求
- Database read replicas 處理增加的讀取負載

**驗收標準**:

- ✅ Auto-scaling 在目標時間內完成
- ✅ 回應時間在目標範圍內
- ✅ 錯誤率 < 0.1%
- ✅ 所有服務保持健康狀態

**測試方法**: 逐漸增加負載的負載測試

### Scenario 2: Database Query Performance

**情境**: 新功能需要複雜的分析查詢

**刺激**: 查詢 join 5 個表並進行彙總

**預期回應**:

- 查詢執行時間 ≤ 100ms (第 95 百分位)
- 為查詢優化建立適當的索引
- 查詢計畫已審查和優化
- 對其他 database 操作無影響

**驗收標準**:

- ✅ 查詢執行時間在目標範圍內
- ✅ Database CPU 保持 < 70%
- ✅ 沒有 connection pool 耗盡
- ✅ Query plan 有效使用索引

**測試方法**: Database 效能測試, query plan 分析

### Scenario 3: Cache Effectiveness

**情境**: 產品目錄經常被存取

**刺激**: 1000 requests/second 查詢產品詳情

**預期回應**:

- Cache hit rate ≥ 85%
- 快取資料的 API 回應時間 ≤ 200ms
- Database 負載減少 80%
- Cache memory 使用在限制內

**驗收標準**:

- ✅ Cache hit rate 達到目標
- ✅ 回應時間改善可測量
- ✅ Database query 減少已驗證
- ✅ 沒有 cache memory 問題

**測試方法**: 帶 cache 監控的負載測試

### Scenario 4: Frontend Performance on Mobile

**情境**: 使用者在行動裝置 4G 網路上存取產品目錄

**刺激**: 使用者導航到產品列表頁面

**預期回應**:

- LCP ≤ 2.5s on mobile 4G
- FID ≤ 100ms
- 頁面在 3.5s 內完全可互動
- 圖片適當的 lazy-loaded

**驗收標準**:

- ✅ Core Web Vitals 達到目標
- ✅ Lighthouse mobile score ≥ 90
- ✅ Bundle size 已優化
- ✅ Critical rendering path 已優化

**測試方法**: Lighthouse CI, 真實使用者監控

### Scenario 5: Resource Exhaustion Prevention

**情境**: Flash sale 期間突然流量激增

**刺激**: 流量在 30 秒內加倍

**預期回應**:

- Circuit breakers 防止級聯失敗
- Connection pools 不會耗盡
- 系統優雅降級
- Auto-scaling 立即觸發

**驗收標準**:

- ✅ 沒有 connection timeout 錯誤
- ✅ Circuit breakers 適當啟動
- ✅ 提供 fallback responses
- ✅ 系統在流量激增後恢復

**測試方法**: Stress testing, chaos engineering

## 效能預算

### API Performance Budget

每個 API endpoint 都有不得超過的效能預算:

| 組件 | 預算 | 測量方式 |
|------|------|---------|
| Database query | 50ms | Query 執行時間 |
| Business logic | 100ms | Application 處理 |
| External service calls | 200ms | Third-party API calls |
| Serialization | 50ms | JSON serialization |
| Network overhead | 100ms | Network 延遲 |
| **總預算** | **500ms** | End-to-end 回應時間 |

**強制執行**: CI/CD pipeline 中的效能回歸測試

### Frontend Performance Budget

| 資源類型 | 預算 | 目前 | 狀態 |
|---------|------|------|------|
| JavaScript bundle | 200 KB (gzipped) | 180 KB | ✅ 在預算內 |
| CSS bundle | 50 KB (gzipped) | 45 KB | ✅ 在預算內 |
| Images (above fold) | 500 KB | 450 KB | ✅ 在預算內 |
| Web fonts | 100 KB | 80 KB | ✅ 在預算內 |
| Third-party scripts | 100 KB | 120 KB | ⚠️ 超出預算 |

**強制執行**: CI/CD 中的 bundle size 檢查, Lighthouse CI

## 監控和警報要求

### 必要的指標

所有服務必須公開以下效能指標:

1. **Request Metrics**:
   - Request count (依 endpoint, method, status)
   - Request duration (histogram 含百分位數)
   - Error rate (依錯誤類型)

2. **Resource Metrics**:
   - CPU utilization
   - Memory utilization
   - Thread pool utilization
   - Connection pool utilization

3. **Business Metrics**:
   - Transaction throughput (orders/second)
   - Conversion rate
   - Cart abandonment rate

### Alert 設定

| Alert | 條件 | 嚴重性 | 行動 |
|-------|------|--------|------|
| High response time | 第 95 百分位 > 1500ms for 5 min | Warning | 調查效能問題 |
| Very high response time | 第 95 百分位 > 2000ms for 5 min | Critical | Page on-call engineer |
| High error rate | Error rate > 1% for 5 min | Critical | Page on-call engineer |
| Database slow queries | > 10 slow queries/min | Warning | 審查 query 效能 |
| Cache hit rate low | Hit rate < 80% for 10 min | Warning | 調查 cache 設定 |
| Auto-scaling failure | Scaling event fails | Critical | Page on-call engineer |

## 效能測試要求

### Load Testing

**頻率**: 每週在 staging, 主要版本發布前

**情境**:

1. Normal load: 500 req/s for 30 minutes
2. Peak load: 1000 req/s for 30 minutes
3. Spike test: 0 to 1000 req/s in 1 minute
4. Endurance test: 500 req/s for 24 hours

**成功標準**:

- 所有回應時間目標達成
- 錯誤率 < 0.1%
- Auto-scaling 正確運作
- 沒有資源耗盡

### Performance Regression Testing

**頻率**: 每次部署到 staging

**方法**: CI/CD pipeline 中的自動化效能測試

**基準**: 前一版本的效能指標

**門檻**: 任何指標不超過 10% 的降級

**行動**: 如果檢測到回歸則阻止部署

## 合規性和報告

### Performance SLO

**Service Level Objective**: 95% 的請求在目標回應時間內完成

**測量期間**: 滾動 30 天窗口

**報告**: 每月 SLO 報告給利害關係人

### Performance Review

**頻率**: 每月效能審查會議

**參與者**: 開發團隊, 營運團隊, 架構師

**議程**:

- 審查效能指標
- 識別效能問題
- 規劃優化工作
- 更新效能要求

## 相關文件

- [Performance Overview](overview.md) - 高階效能視角
- [Scalability Strategy](scalability.md) - Horizontal scaling 方法
- [Optimization Guidelines](optimization.md) - 效能優化技術
- [Verification](verification.md) - 測試和監控細節

---

**文件版本**: 1.0
**最後更新**: 2025-10-23
**負責人**: Architecture Team
