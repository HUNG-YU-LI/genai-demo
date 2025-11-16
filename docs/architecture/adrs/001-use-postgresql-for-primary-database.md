---
adr_number: 001
title: "Use PostgreSQL for Primary Database"
date: 2025-10-24
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [002, 005]
affected_viewpoints: ["information", "deployment"]
affected_perspectives: ["performance", "availability"]
---

# ADR-001: Use PostgreSQL for Primary Database

## 狀態

**Accepted** - 2025-10-24

## 上下文

### 問題陳述

企業電子商務平台需要一個強健、可擴展且可靠的資料庫系統，用於儲存和管理關鍵業務資料，包括客戶、訂單、產品、庫存和交易。資料庫必須支援：

- 具有參照完整性的複雜關聯式資料模型
- 金融操作的 ACID 交易
- 高讀寫吞吐量
- Multi-region 複製
- 進階查詢功能
- 強一致性保證

### 業務上下文

**業務驅動因素**：

- 需要可靠的交易處理（訂單、付款）
- 法規合規要求（GDPR、PCI-DSS）
- 預期從 10K 增長到 1M+ 用戶
- 24/7 可用性要求（99.9% SLA）
- Multi-region 部署以服務全球用戶

**限制條件**：

- 預算：每月 $5,000 用於資料庫基礎設施
- 團隊專業知識：豐富的 Java/Spring Boot 經驗
- 時程：3 個月上線至生產環境
- 合規性：必須支援資料加密和稽核軌跡

### 技術上下文

**目前狀態**：

- 全新的 greenfield 專案
- 沒有現有資料庫基礎設施
- 已選擇 Spring Boot 應用程式框架
- AWS 雲端基礎設施

**需求**：

- 支援複雜 joins 和交易
- JSON 資料類型支援以實現靈活的 schemas
- 全文搜尋功能
- 地理空間資料支援（未來需求）
- Read replica 支援以實現擴展
- Point-in-time 恢復

## 決策驅動因素

1. **資料完整性**：金融交易需要強 ACID 保證
2. **可擴展性**：必須處理 10K → 1M 用戶增長
3. **團隊專業知識**：團隊具有 SQL 資料庫經驗
4. **生態系統**：豐富的工具和 Spring Boot 整合
5. **成本**：可預測的定價模式
6. **合規性**：內建安全和稽核功能
7. **效能**：查詢回應時間少於 100ms
8. **可用性**：Multi-AZ 和 read replica 支援

## 考慮的選項

### 選項 1：PostgreSQL

**描述**：具有進階功能的開源關聯式資料庫

**優點**：

- ✅ 強 ACID 合規性和資料完整性
- ✅ 優秀的 Spring Boot/JPA 整合
- ✅ 豐富的功能集（JSON、全文搜尋、陣列）
- ✅ 活躍的社群和廣泛的文檔
- ✅ 可用 AWS RDS 託管服務
- ✅ Read replicas 實現水平擴展
- ✅ 進階索引（B-tree、GiST、GIN、BRIN）
- ✅ 成熟的複製（streaming、logical）
- ✅ 在我們的規模下具成本效益

**缺點**：

- ⚠️ 寫入擴展需要 sharding（複雜）
- ⚠️ 垂直擴展有限制
- ⚠️ Read replicas 的複製延遲

**成本**：

- 開發環境：每月 $3,000（db.r5.xlarge Multi-AZ）
- 生產環境：每月 $5,000（db.r5.2xlarge Multi-AZ + 2 read replicas）

**風險**：**低** - 已驗證的技術，具有廣泛的生產使用

### 選項 2：MySQL

**描述**：流行的開源關聯式資料庫

**優點**：

- ✅ 廣泛採用和社群
- ✅ 良好的 Spring Boot 整合
- ✅ AWS RDS 託管服務
- ✅ Read replicas 支援
- ✅ 較低的資源使用

**缺點**：

- ❌ 比 PostgreSQL 功能較少
- ❌ 較弱的 JSON 支援
- ❌ 有限的全文搜尋
- ❌ 查詢優化器較不成熟
- ❌ 複製可能很複雜

**成本**：與 PostgreSQL 相似

**風險**：**低** - 已驗證的技術

### 選項 3：MongoDB

**描述**：面向文檔的 NoSQL 資料庫

**優點**：

- ✅ 靈活的 schema
- ✅ 內建水平擴展
- ✅ 適合快速開發
- ✅ 原生 JSON 支援

**缺點**：

- ❌ 跨文檔沒有 ACID 交易（直到 v4.0）
- ❌ 預設最終一致性
- ❌ 不太適合複雜 joins
- ❌ 團隊缺乏 NoSQL 經驗
- ❌ 學習曲線較陡
- ❌ 在規模化時更昂貴

**成本**：每月 $6,000（MongoDB Atlas M30）

**風險**：**中等** - 團隊學習曲線，交易限制

### 選項 4：Amazon Aurora PostgreSQL

**描述**：AWS 原生 PostgreSQL 相容資料庫

**優點**：

- ✅ PostgreSQL 相容性
- ✅ 比標準 PostgreSQL 效能更好
- ✅ 自動擴展
- ✅ 快速容錯移轉（< 30 秒）
- ✅ 最多 15 個 read replicas

**缺點**：

- ❌ 廠商鎖定於 AWS
- ❌ 成本高於 RDS PostgreSQL
- ❌ 某些 PostgreSQL 擴展不支援
- ❌ 更複雜的定價模式

**成本**：每月 $7,000（db.r5.2xlarge + replicas）

**風險**：**低** - AWS 託管，但有廠商鎖定

## 決策結果

**選擇的選項**：**PostgreSQL on AWS RDS**

### 理由

選擇 PostgreSQL 作為主要資料庫的原因如下：

1. **強 ACID 保證**：對金融交易和訂單處理至關重要
2. **功能豐富**：JSON 支援、全文搜尋和進階索引滿足所有當前和近期需求
3. **優秀的生態系統**：與 Hibernate 的一流 Spring Boot/JPA 整合
4. **團隊專業知識**：團隊具有 SQL 經驗，學習曲線最小
5. **具成本效益**：在預算限制內滿足企業功能需求
6. **可擴展路徑**：Read replicas 提供水平讀取擴展；如需要可稍後新增 sharding
7. **AWS RDS**：託管服務減少營運負擔（備份、修補、監控）
8. **合規性**：內建加密、稽核日誌和安全功能

**為何不選 Aurora**：雖然 Aurora 提供更好的效能，但成本溢價（高 40%）對我們目前的規模來說不合理。如需要，我們可以稍後遷移到 Aurora，而無需變更應用程式。

**為何不選 MongoDB**：缺乏強 ACID 保證和團隊專業知識，使其不適合我們的交易密集型工作負載。

## 影響分析

### 利害關係人影響

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | Medium | 需要學習 PostgreSQL 特定功能 | 培訓課程、文檔 |
| Operations Team | Low | 熟悉 RDS 管理 | 標準 RDS runbooks |
| End Users | None | 對用戶透明 | N/A |
| Business | Positive | 可靠的資料儲存 | N/A |
| Compliance | Positive | 內建安全功能 | 定期稽核 |

### 影響半徑

**選擇的影響半徑**：**System**

影響：

- 所有 bounded contexts（資料儲存）
- Application 層（JPA 配置）
- Infrastructure 層（RDS 設定）
- 部署流程
- 備份和恢復程序

### 風險評估

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| 寫入擴展限制 | Medium | High | 實作 read replicas、快取、最終 sharding 計畫 |
| 複製延遲 | Medium | Medium | 監控延遲、實作重試邏輯、使用非同步處理 |
| 成本超支 | Low | Medium | 監控使用量、實作自動擴展政策 |
| 資料遷移複雜性 | Low | High | 徹底測試、分階段推出、回滾計畫 |
| 廠商鎖定（AWS RDS） | Low | Medium | 使用標準 PostgreSQL 功能、避免 AWS 特定擴展 |

**整體風險等級**：**低**

## 實作計畫

### 第 1 階段：設定和配置（第 1-2 週）

- [x] 佈建 RDS PostgreSQL 實例（db.r5.xlarge、Multi-AZ）
- [x] 配置 security groups 和網路存取
- [x] 設定 parameter groups 以進行優化
- [x] 啟用自動備份（7 天保留）
- [x] 配置 CloudWatch 監控和告警
- [x] 在次要區域設定 read replica

### 第 2 階段：應用程式整合（第 3-4 週）

- [x] 配置 Spring Boot data source
- [x] 使用 PostgreSQL dialect 設定 Hibernate/JPA
- [x] 使用 Flyway 實作資料庫遷移
- [x] 建立初始 schema 和索引
- [x] 實作連線池（HikariCP）
- [x] 新增資料庫健康檢查

### 第 3 階段：測試和優化（第 5-6 週）

- [x] 使用實際資料量進行負載測試
- [x] 查詢效能優化
- [x] 基於查詢模式調整索引
- [x] 複製延遲監控
- [x] 容錯移轉測試
- [x] 備份和恢復測試

### 回滾策略

**觸發條件**：

- 效能下降 > 50%
- 資料損壞或遺失
- 無法恢復的錯誤
- 成本超過預算 > 50%

**回滾步驟**：

1. 如果在開發環境：暫時切換到 H2 in-memory 資料庫
2. 如果在生產環境：從最新備份恢復
3. 調查根本原因
4. 如發現根本問題，重新評估資料庫選擇

**回滾時間**：開發環境 < 1 小時，生產環境 < 4 小時

## 監控和成功標準

### 成功指標

- ✅ 查詢回應時間 < 100ms（第 95 百分位）
- ✅ 寫入吞吐量 > 1000 TPS
- ✅ 讀取吞吐量 > 5000 TPS
- ✅ 複製延遲 < 1 秒
- ✅ 可用性 > 99.9%
- ✅ 零資料遺失事件
- ✅ 成本在預算內（每月 $5,000）

### 監控計畫

**CloudWatch Metrics**：

- DatabaseConnections
- CPUUtilization
- FreeableMemory
- ReadLatency / WriteLatency
- ReplicaLag
- DiskQueueDepth

**告警**：

- CPU > 80% 持續 5 分鐘
- 連線 > 最大值的 80%
- 複製延遲 > 5 秒
- 磁碟空間 < 20%
- 連線失敗 > 10 次/分鐘

**審查時程**：

- 每日：檢查 CloudWatch 儀表板
- 每週：審查慢查詢日誌
- 每月：容量規劃審查
- 每季：成本優化審查

## 後果

### 正面後果

- ✅ **強資料完整性**：ACID 保證保護金融資料
- ✅ **豐富功能集**：JSON、全文搜尋、陣列支援複雜使用案例
- ✅ **優秀工具**：pgAdmin、pg_stat_statements、廣泛監控
- ✅ **Spring Boot 整合**：無縫的 JPA/Hibernate 支援
- ✅ **可擴展性**：Read replicas 提供水平讀取擴展
- ✅ **社群支援**：大型社群、廣泛文檔
- ✅ **具成本效益**：在預算內滿足需求
- ✅ **合規就緒**：內建加密、稽核日誌

### 負面後果

- ⚠️ **寫入擴展**：僅垂直擴展，大規模需要 sharding
- ⚠️ **複製延遲**：Read replicas 具有最終一致性
- ⚠️ **營運負擔**：需要監控和調整效能
- ⚠️ **遷移複雜性**：未來遷移到 Aurora 或 sharding 將很複雜

### 技術債務

**已識別債務**：

1. 未實作 sharding 策略（對當前規模可接受）
2. 單區域寫入 master（對當前需求可接受）
3. 需要手動查詢優化（持續進行的過程）

**債務償還計畫**：

- **2026 年 Q2**：根據增長評估 sharding 需求
- **2026 年 Q3**：實作快取層以減少資料庫負載
- **2026 年 Q4**：如效能需求增加，考慮 Aurora 遷移

## 相關決策

- [ADR-002: Adopt Hexagonal Architecture](002-adopt-hexagonal-architecture.md) - Repository pattern 實作
- [ADR-004: Use Redis for Distributed Caching](004-use-redis-for-distributed-caching.md) - 快取策略以減少資料庫負載
- [ADR-005: Use Apache Kafka for Event Streaming](005-use-kafka-for-event-streaming.md) - Event sourcing 考量
- [ADR-007: Use AWS CDK for Infrastructure](007-use-aws-cdk-for-infrastructure.md) - 資料庫佈建

## 備註

### PostgreSQL 配置

```yaml
# 關鍵 RDS 參數
max_connections: 200
shared_buffers: 8GB
effective_cache_size: 24GB
maintenance_work_mem: 2GB
checkpoint_completion_target: 0.9
wal_buffers: 16MB
default_statistics_target: 100
random_page_cost: 1.1
effective_io_concurrency: 200
work_mem: 20MB
min_wal_size: 1GB
max_wal_size: 4GB
```

### 遷移到 Aurora 的路徑

如未來需求需要 Aurora：

1. 從 RDS 快照建立 Aurora cluster
2. 更新應用程式連線字串
3. 在 staging 環境徹底測試
4. Blue-green 部署到生產環境
5. 監控 48 小時
6. 停用 RDS 實例

**預估遷移時間**：1 週
**預估停機時間**：< 1 小時

---

**文檔狀態**：✅ Accepted
**上次審查**：2025-10-24
**下次審查**：2026-01-24（每季）
