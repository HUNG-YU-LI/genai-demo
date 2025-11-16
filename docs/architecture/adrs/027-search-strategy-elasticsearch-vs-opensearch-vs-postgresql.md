---
adr_number: 027
title: "Search Strategy (Elasticsearch vs OpenSearch vs PostgreSQL)"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [001, 004]
affected_viewpoints: ["functional", "information", "deployment"]
affected_perspectives: ["performance", "scalability", "cost"]
---

# ADR-027: Search Strategy (Elasticsearch vs OpenSearch vs PostgreSQL)

## 狀態

**Accepted** - 2025-10-25

## 上下文

### 問題陳述

The Enterprise E-Commerce Platform 需要search functionality for:

- Product search 與 full-text search, filters, 和 facets
- Order search 透過 customer, date range, status, 和 order number
- Customer search 透過 name, email, phone, 和 membership level
- Fast search 回應時間 (< 200ms 用於 95th percentile)
- 支援 用於 typo tolerance 和 fuzzy matching
- Relevance ranking 和 scoring
- Autocomplete 和 search suggestions
- Scalability to 處理 millions of products 和 orders

### 業務上下文

**業務驅動因素**：

- Product discovery is critical 用於 conversion (60% of users use search)
- Fast search 改善s 用戶體驗 和 sales
- Advanced search features (filters, facets) increase engagement
- 支援 用於 100K+ products, 1M+ orders, 100K+ customers
- 預期的 search traffic: 1000+ searches/second 期間 peak

**Business Constraints**:

- 預算: $1000/month 用於 search infrastructure
- Search 回應時間 < 200ms (95th percentile)
- 必須 支援 Chinese 和 English full-text search
- Need real-time 或 near-real-time indexing
- High availability (99.9% uptime)

### 技術上下文

**目前狀態**：

- PostgreSQL 主要資料庫 (ADR-001)
- Redis 用於 caching (ADR-004)
- Spring Boot 3.4.5 application
- AWS 雲端基礎設施
- 13 bounded contexts 與 search needs

**需求**：

- Full-text search 與 relevance ranking
- Faceted search (filters 透過 category, price, brand, etc.)
- Autocomplete 和 suggestions
- Typo tolerance 和 fuzzy matching
- Multi-language 支援 (Chinese, English)
- Real-time 或 near-real-time indexing
- Scalability to millions of documents
- High availability 與 自動容錯移轉

## 決策驅動因素

1. **Performance**: Search 回應時間 < 200ms (95th percentile)
2. **Features**: Full-text search, facets, autocomplete, fuzzy matching
3. **Scalability**: 處理 millions of documents, 1000+ searches/second
4. **成本**： Within $1000/month budget
5. **Operational Overhead**: Managed service preferred
6. **Integration**: Spring Boot integration
7. **Multi-Language**: Chinese 和 English 支援
8. **Real-Time**: Near-real-time indexing (< 1 minute lag)

## 考慮的選項

### 選項 1： PostgreSQL Full-Text Search

**描述**： Use PostgreSQL's built-in full-text search capabilities

**優點**：

- ✅ No additional infrastructure
- ✅ ACID transactions 與 search
- ✅ 簡單implement
- ✅ No data synchronization needed
- ✅ Cost-effective ($0 additional)
- ✅ 良好的 用於 簡單的 searches

**缺點**：

- ❌ Limited full-text search features
- ❌ Poor performance at scale (> 100K documents)
- ❌ No faceted search 支援
- ❌ Limited relevance ranking
- ❌ No autocomplete 支援
- ❌ Increases 資料庫負載
- ❌ Limited multi-language 支援
- ❌ No distributed search

**成本**： $0 (included in database)

**風險**： **High** - Insufficient for requirements

**Performance**: 500-2000ms 用於 複雜的 searches

### 選項 2： Elasticsearch (AWS OpenSearch Service)

**描述**： Use Elasticsearch via AWS OpenSearch Service (Elasticsearch-compatible)

**優點**：

- ✅ 優秀的 full-text search capabilities
- ✅ 豐富的 faceted search 和 aggregations
- ✅ Fast search performance (< 50ms)
- ✅ Autocomplete 和 suggestions
- ✅ Typo tolerance 和 fuzzy matching
- ✅ Multi-language analyzers
- ✅ Scalable to billions of documents
- ✅ AWS 託管服務
- ✅ High availability 與 replicas
- ✅ 大型的 社群和生態系統
- ✅ Spring Data Elasticsearch integration

**缺點**：

- ⚠️ Additional infrastructure cost
- ⚠️ Data synchronization 複雜的ity
- ⚠️ Eventual consistency
- ⚠️ Operational overhead (indexing, monitoring)
- ⚠️ Learning curve

**成本**：

- Development: $800/month (t3.medium.search x 2)
- Production: $1200/month (r6g.大型的.search x 2)

**風險**： **Low** - Industry-standard solution

**Performance**: 20-100ms 用於 複雜的 searches

### 選項 3： OpenSearch (AWS OpenSearch Service)

**描述**： Use OpenSearch (AWS's open-source fork of Elasticsearch)

**優點**：

- ✅ All Elasticsearch features (fork from ES 7.10)
- ✅ 優秀的 full-text search capabilities
- ✅ 豐富的 faceted search 和 aggregations
- ✅ Fast search performance (< 50ms)
- ✅ AWS 託管服務
- ✅ Lower cost than Elasticsearch
- ✅ Open-source (Apache 2.0 license)
- ✅ AWS native integration
- ✅ Spring Data Elasticsearch compatible

**缺點**：

- ⚠️ Smaller community than Elasticsearch
- ⚠️ Data synchronization 複雜的ity
- ⚠️ Eventual consistency
- ⚠️ Operational overhead

**成本**：

- Development: $600/month (t3.medium.search x 2)
- Production: $1000/month (r6g.大型的.search x 2)

**風險**： **Low** - AWS-backed solution

**Performance**: 20-100ms 用於 複雜的 searches

### 選項 4： Hybrid Approach (PostgreSQL + Redis)

**描述**： Use PostgreSQL for simple searches, Redis for autocomplete

**優點**：

- ✅ Leverages existing infrastructure
- ✅ Cost-effective
- ✅ 簡單implement
- ✅ 良好的 用於 basic 使用案例s

**缺點**：

- ❌ Limited search features
- ❌ Poor performance at scale
- ❌ No faceted search
- ❌ 複雜的 to 維持
- ❌ Doesn't meet requirements

**成本**： $0 (existing infrastructure)

**風險**： **High** - Insufficient for requirements

**Performance**: 200-1000ms 用於 複雜的 searches

## 決策結果

**選擇的選項**： **OpenSearch (AWS OpenSearch Service)**

### 理由

OpenSearch被選擇的原因如下：

1. **Cost-Effective**: Meets requirements within $1000/month 預算
2. **Performance**: Sub-100ms search 回應時間
3. **Features**: All required search features (full-text, facets, autocomplete, fuzzy)
4. **AWS Native**: 無縫的AWS整合, 託管服務
5. **Open-Source**: Apache 2.0 license, no vendor lock-in
6. **Scalability**: 處理s millions of documents easily
7. **Spring Integration**: Compatible 與 Spring Data Elasticsearch
8. **Multi-Language**: 優秀的 Chinese 和 English 支援
9. **Proven**: Used 透過 many 大型的-scale e-commerce platforms

**Search Architecture**:

**Primary Use Cases**:

- **Product Search**: Full-text search 與 facets (category, price, brand, rating)
- **Order Search**: Search 透過 order number, customer, date range, status
- **Customer Search**: Search 透過 name, email, phone (admin only)

**Indexing Strategy**:

- **Real-Time**: Product updates indexed within 1 minute
- **Batch**: Bulk indexing 用於 historical data
- **CDC**: Change Data Capture from PostgreSQL to OpenSearch
- **Event-Driven**: Domain events trigger index updates

**Search Features**:

- Full-text search 與 relevance ranking
- Faceted search (filters 和 aggregations)
- Autocomplete 和 search suggestions
- Typo tolerance (fuzzy matching, edit distance)
- Multi-language analyzers (Chinese, English)
- Highlighting of search terms
- Pagination 和 sorting

**為何不選 PostgreSQL**： Insufficient features, poor performance at scale.

**為何不選 Elasticsearch**： Higher cost, licensing concerns (Elastic License).

**為何不選 Hybrid**： Doesn't meet requirements, 複雜的 to 維持.

## 影響分析

### 利害關係人影響

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | High | Need to learn OpenSearch | Training, documentation, examples |
| Operations Team | Medium | Monitor OpenSearch cluster | AWS 託管服務, runbooks |
| End Users | Positive | Faster, 更好的 search experience | N/A |
| Business | Positive | 改善d conversion, user satisfaction | N/A |
| Database Team | Low | 降低d database search load | N/A |

### 影響半徑

**選擇的影響半徑**： **System**

影響：

- Product bounded context (primary)
- Order bounded context (secondary)
- Customer bounded context (admin search)
- Application services (search integration)
- Infrastructure layer (OpenSearch cluster)
- Data synchronization (CDC pipeline)

### 風險評估

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| Data sync lag | Medium | Medium | Monitor lag, alerting, CDC optimization |
| OpenSearch unavailability | Low | High | Fallback to database, circuit breaker |
| Index corruption | Low | Medium | Regular backups, automated recovery |
| Cost overrun | Low | Medium | Monitor usage, set alarms, optimize queries |
| Search relevance issues | Medium | Medium | Tuning, A/B testing, user feedback |

**整體風險等級**： **Low**

## 實作計畫

### 第 1 階段： Setup （第 1-2 週）

- [x] Provision OpenSearch cluster (r6g.大型的.search x 2)
- [x] Configure security (VPC, security groups, IAM)
- [x] Set up monitoring 和 alerting
- [x] Create index templates 和 mappings
- [x] Configure analyzers (Chinese, English)

### 第 2 階段： Product Search （第 3-4 週）

- [x] Implement product index mapping
- [x] Bulk index existing products
- [x] Implement CDC 用於 product updates
- [x] Create product search API
- [x] Implement faceted search
- [x] Add autocomplete

### 第 3 階段： Order & Customer Search （第 5-6 週）

- [x] Implement order index mapping
- [x] Bulk index existing orders
- [x] Implement CDC 用於 order updates
- [x] Create order search API
- [x] Implement customer search (admin)
- [x] Add search suggestions

### 第 4 階段： Optimization （第 7-8 週）

- [x] Performance tuning 和 optimization
- [x] Relevance tuning (scoring, boosting)
- [x] Load testing (1000+ searches/second)
- [x] Implement search analytics
- [x] Documentation 和 training

### 回滾策略

**觸發條件**：

- Search 回應時間 > 500ms
- OpenSearch availability < 99%
- Data sync lag > 5 minutes
- Cost exceeds 預算 透過 > 50%

**回滾步驟**：

1. Disable OpenSearch search
2. Fall back to PostgreSQL 簡單的 search
3. Investigate 和 fix issues
4. Re-啟用 OpenSearch gradually
5. Monitor performance

**回滾時間**： < 1 hour

## 監控和成功標準

### 成功指標

- ✅ Search 回應時間 < 200ms (95th percentile)
- ✅ Search availability > 99.9%
- ✅ Data sync lag < 1 minute
- ✅ Search relevance score > 80% (user feedback)
- ✅ Cost within 預算 ($1000/month)
- ✅ Zero data loss 期間 sync

### 監控計畫

**CloudWatch Metrics**:

- SearchRate (searches per second)
- SearchLatency (p50, p95, p99)
- IndexingRate (documents per second)
- IndexingLatency
- ClusterStatus (green, yellow, red)
- CPUUtilization
- JVMMemoryPressure
- DiskQueueDepth

**Application Metrics**:

```java
@Component
public class SearchMetrics {
    private final Timer searchTime;
    private final Counter searchRequests;
    private final Counter searchErrors;
    private final Gauge indexLag;
    
    // Track search performance
}
```

**告警**：

- Search latency > 500ms
- Cluster status not green
- CPU utilization > 80%
- JVM memory pressure > 80%
- Index lag > 5 minutes
- Search error rate > 1%

**審查時程**：

- Daily: Check search metrics
- Weekly: Review search relevance
- Monthly: Optimize search performance
- Quarterly: Search strategy review

## 後果

### 正面後果

- ✅ **Performance**: Sub-200ms search 回應時間
- ✅ **Features**: 豐富的 search capabilities (facets, autocomplete, fuzzy)
- ✅ **Scalability**: 處理s millions of documents
- ✅ **User Experience**: Fast, relevant search results
- ✅ **Conversion**: 改善d product discovery
- ✅ **Database Health**: 降低d database search load
- ✅ **Flexibility**: 容易add new search features

### 負面後果

- ⚠️ **複雜的ity**: Additional infrastructure to manage
- ⚠️ **成本**： $1000/month additional cost
- ⚠️ **Consistency**: Eventual consistency (1-minute lag)
- ⚠️ **Synchronization**: Need to keep data in sync
- ⚠️ **Operational Overhead**: Monitoring, tuning, maintenance

### 技術債務

**已識別債務**：

1. Manual relevance tuning (可以 be automated 與 ML)
2. 簡單的 CDC (可以 use Debezium 用於 robust CDC)
3. No search analytics (future enhancement)

**債務償還計畫**：

- **Q2 2026**: Implement Debezium 用於 robust CDC
- **Q3 2026**: Add ML-based relevance tuning
- **Q4 2026**: Implement search analytics 和 A/B testing

## 相關決策

- [ADR-001: Use PostgreSQL 用於 Primary Database](001-use-postgresql-for-primary-database.md) - Source of truth 用於 data
- [ADR-004: Use Redis 用於 Distributed Caching](004-use-redis-for-distributed-caching.md) - Cache search results

## 備註

### OpenSearch Index Mapping

```json
{
  "mappings": {
    "properties": {
      "productId": { "type": "keyword" },
      "name": {
        "type": "text",
        "analyzer": "ik_max_word",
        "search_analyzer": "ik_smart",
        "fields": {
          "keyword": { "type": "keyword" },
          "english": {
            "type": "text",
            "analyzer": "english"
          }
        }
      },
      "description": {
        "type": "text",
        "analyzer": "ik_max_word"
      },
      "category": { "type": "keyword" },
      "brand": { "type": "keyword" },
      "price": { "type": "double" },
      "rating": { "type": "float" },
      "reviewCount": { "type": "integer" },
      "inStock": { "type": "boolean" },
      "tags": { "type": "keyword" },
      "createdAt": { "type": "date" },
      "updatedAt": { "type": "date" }
    }
  }
}
```

### Search Query Example

```java
@Service
public class ProductSearchService {
    
    @Autowired
    private ElasticsearchOperations elasticsearchOperations;
    
    public SearchPage<Product> searchProducts(ProductSearchRequest request) {
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        
        // Full-text search
        if (StringUtils.hasText(request.getQuery())) {
            queryBuilder.withQuery(
                QueryBuilders.multiMatchQuery(request.getQuery())
                    .field("name", 3.0f)  // Boost name
                    .field("description", 1.0f)
                    .field("tags", 2.0f)
                    .fuzziness(Fuzziness.AUTO)
                    .prefixLength(2)
            );
        }
        
        // Filters
        BoolQueryBuilder filterQuery = QueryBuilders.boolQuery();
        
        if (request.getCategory() != null) {
            filterQuery.filter(QueryBuilders.termQuery("category", request.getCategory()));
        }
        
        if (request.getBrand() != null) {
            filterQuery.filter(QueryBuilders.termQuery("brand", request.getBrand()));
        }
        
        if (request.getMinPrice() != null || request.getMaxPrice() != null) {
            RangeQueryBuilder priceRange = QueryBuilders.rangeQuery("price");
            if (request.getMinPrice() != null) {
                priceRange.gte(request.getMinPrice());
            }
            if (request.getMaxPrice() != null) {
                priceRange.lte(request.getMaxPrice());
            }
            filterQuery.filter(priceRange);
        }
        
        if (request.isInStockOnly()) {
            filterQuery.filter(QueryBuilders.termQuery("inStock", true));
        }
        
        queryBuilder.withFilter(filterQuery);
        
        // Facets (Aggregations)
        queryBuilder.addAggregation(
            AggregationBuilders.terms("categories").field("category").size(20)
        );
        queryBuilder.addAggregation(
            AggregationBuilders.terms("brands").field("brand").size(20)
        );
        queryBuilder.addAggregation(
            AggregationBuilders.range("priceRanges").field("price")
                .addRange(0, 100)
                .addRange(100, 500)
                .addRange(500, 1000)
                .addRange(1000, Double.MAX_VALUE)
        );
        
        // Sorting
        if (request.getSortBy() != null) {
            SortOrder order = request.getSortOrder() == SortOrder.DESC ? 
                SortOrder.DESC : SortOrder.ASC;
            queryBuilder.withSort(SortBuilders.fieldSort(request.getSortBy()).order(order));
        }
        
        // Pagination
        queryBuilder.withPageable(
            PageRequest.of(request.getPage(), request.getSize())
        );
        
        // Highlighting
        queryBuilder.withHighlightFields(
            new HighlightBuilder.Field("name"),
            new HighlightBuilder.Field("description")
        );
        
        return elasticsearchOperations.searchForPage(
            queryBuilder.build(), 
            Product.class
        );
    }
}
```

### CDC Implementation

```java
@Component
public class ProductEventHandler {
    
    @Autowired
    private ElasticsearchOperations elasticsearchOperations;
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductCreated(ProductCreatedEvent event) {
        Product product = convertToSearchDocument(event);
        elasticsearchOperations.save(product);
    }
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductUpdated(ProductUpdatedEvent event) {
        Product product = convertToSearchDocument(event);
        elasticsearchOperations.save(product);
    }
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductDeleted(ProductDeletedEvent event) {
        elasticsearchOperations.delete(event.getProductId(), Product.class);
    }
}
```

### OpenSearch Configuration

```yaml
# AWS OpenSearch Service Configuration
Domain: ecommerce-search-prod
Version: OpenSearch 2.11
Instance Type: r6g.large.search
Instance Count: 2
Dedicated Master: Enabled (3 x r6g.large.search)
EBS Volume: 100 GB GP3 per node
Multi-AZ: Enabled
Encryption: At rest and in transit
VPC: Private subnets
Security: IAM + VPC security groups
Backup: Automated daily snapshots, 7-day retention
```

---

**文檔狀態**： ✅ Accepted  
**上次審查**： 2025-10-25  
**下次審查**： 2026-01-25 （每季）
