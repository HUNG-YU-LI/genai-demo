# Runbook: API 回應緩慢

## 症狀

- API 回應時間 > 2s（95th percentile）
- 客戶端 timeout 錯誤
- 請求佇列長度增加
- 使用者抱怨頁面載入緩慢
- Distributed traces 中的高延遲

## 影響

- **嚴重程度**：P1 - High
- **受影響使用者**：所有使用者體驗效能下降
- **業務影響**：不良的使用者體驗、可能的購物車放棄、收入損失

## 偵測

- **Alert**：`SlowAPIResponseTime` alert 觸發
- **Monitoring Dashboard**：Operations Dashboard > Performance > API Response Times
- **Log 模式**：
  - `Request took longer than expected`
  - `Slow query detected`
  - `Timeout waiting for response`

## 診斷

### 步驟 1：識別慢速 Endpoints

```bash
# 檢查各 endpoint 的回應時間
curl http://localhost:8080/actuator/metrics/http.server.requests | jq '.measurements'

# 取得特定 endpoint 的詳細 metrics
curl "http://localhost:8080/actuator/metrics/http.server.requests?tag=uri:/api/v1/orders" | jq

# 檢查 Prometheus metrics
curl -g 'http://prometheus:9090/api/v1/query?query=histogram_quantile(0.95,rate(http_request_duration_seconds_bucket[5m]))' | jq
```

### 步驟 2：分析請求模式

```bash
# 檢查請求速率
kubectl logs deployment/ecommerce-backend -n production --tail=1000 | \
  grep "HTTP" | awk '{print $1}' | uniq -c | sort -rn

# 在 logs 中識別慢速請求
kubectl logs deployment/ecommerce-backend -n production --tail=5000 | \
  grep -i "slow\|timeout" | head -50

# 檢查特定慢速 endpoints
kubectl logs deployment/ecommerce-backend -n production --tail=5000 | \
  grep "duration" | awk '$NF > 2000' | head -20
```

### 步驟 3：檢查 Database Performance

```bash
# 檢查活動的 database 查詢
kubectl exec -it ${POD_NAME} -n production -- \
  psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} -c \
  "SELECT pid, now() - query_start as duration, state, query
   FROM pg_stat_activity
   WHERE state != 'idle'
   ORDER BY duration DESC
   LIMIT 20;"

# 檢查慢速查詢
kubectl exec -it ${POD_NAME} -n production -- \
  psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} -c \
  "SELECT query, calls, total_time, mean_time, max_time
   FROM pg_stat_statements
   ORDER BY mean_time DESC
   LIMIT 20;"

# 檢查 table locks
kubectl exec -it ${POD_NAME} -n production -- \
  psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} -c \
  "SELECT locktype, relation::regclass, mode, granted, pid
   FROM pg_locks
   WHERE NOT granted
   ORDER BY pid;"

# 檢查 database connection pool
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
curl http://localhost:8080/actuator/metrics/hikaricp.connections.pending
```

### 步驟 4：檢查 Cache Performance

```bash
# 檢查 cache hit rate
curl http://localhost:8080/actuator/metrics/cache.gets?tag=result:hit
curl http://localhost:8080/actuator/metrics/cache.gets?tag=result:miss

# 計算 hit rate
HIT=$(curl -s http://localhost:8080/actuator/metrics/cache.gets?tag=result:hit | jq '.measurements[0].value')
MISS=$(curl -s http://localhost:8080/actuator/metrics/cache.gets?tag=result:miss | jq '.measurements[0].value')
echo "Cache hit rate: $(echo "scale=2; $HIT / ($HIT + $MISS) * 100" | bc)%"

# 檢查 Redis performance
kubectl exec -it redis-0 -n production -- redis-cli INFO stats | grep -E "keyspace_hits|keyspace_misses"
kubectl exec -it redis-0 -n production -- redis-cli INFO stats | grep instantaneous_ops_per_sec
kubectl exec -it redis-0 -n production -- redis-cli SLOWLOG GET 10
```

### 步驟 5：分析 Distributed Traces

```bash
# 取得慢速請求的 trace IDs
kubectl logs deployment/ecommerce-backend -n production --tail=1000 | \
  grep "duration.*[3-9][0-9][0-9][0-9]" | \
  grep -oP 'traceId=\K[a-f0-9]+'

# 在 X-Ray console 中查看 trace 或使用 AWS CLI
aws xray get-trace-summaries \
  --start-time $(date -u -d '1 hour ago' +%s) \
  --end-time $(date -u +%s) \
  --filter-expression 'duration > 2'

# 取得詳細 trace
aws xray batch-get-traces --trace-ids ${TRACE_ID}
```

### 步驟 6：檢查外部依賴

```bash
# 檢查外部 API 回應時間
kubectl logs deployment/ecommerce-backend -n production --tail=1000 | \
  grep "external" | grep "duration"

# 測試 payment gateway
curl -w "@curl-format.txt" -o /dev/null -s https://payment-gateway.example.com/health

# 測試 email service
curl -w "@curl-format.txt" -o /dev/null -s https://email-service.example.com/health

# 檢查 Kafka lag
kubectl exec -it kafka-0 -n production -- \
  kafka-consumer-groups --bootstrap-server localhost:9092 \
  --describe --group ecommerce-consumer
```

### 步驟 7：檢查資源使用率

```bash
# 檢查 CPU 和 memory
kubectl top pods -n production -l app=ecommerce-backend

# 檢查 network I/O
kubectl exec -it ${POD_NAME} -n production -- \
  cat /proc/net/dev

# 檢查 disk I/O
kubectl exec -it ${POD_NAME} -n production -- \
  iostat -x 1 5
```

## 解決方案

### 即時行動

1. **水平擴充**（如果資源受限）：

```bash
kubectl scale deployment/ecommerce-backend --replicas=8 -n production
```

2. **清除 cache**（如果過時資料導致問題）：

```bash
# 清除 application cache
curl -X POST http://localhost:8080/actuator/caches/products -d '{"action":"clear"}'

# 清除 Redis cache
kubectl exec -it redis-0 -n production -- redis-cli FLUSHDB
```

3. **終止慢速查詢**（如果阻塞其他查詢）：

```bash
kubectl exec -it ${POD_NAME} -n production -- \
  psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} -c \
  "SELECT pg_terminate_backend(pid)
   FROM pg_stat_activity
   WHERE state = 'active'
   AND now() - query_start > interval '30 seconds';"
```

### 根本原因修復

#### 如果由慢速 database 查詢引起

1. **識別慢速查詢**：

```sql
-- 取得最慢的查詢
SELECT query, calls, total_time, mean_time, max_time,
       stddev_time, rows
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 20;
```

2. **分析查詢執行計畫**：

```sql
EXPLAIN ANALYZE
SELECT o.*, c.name, c.email
FROM orders o
JOIN customers c ON o.customer_id = c.id
WHERE o.created_at > NOW() - INTERVAL '7 days';
```

3. **新增缺少的索引**：

```sql
-- 在常查詢的欄位上創建索引
CREATE INDEX CONCURRENTLY idx_orders_created_at
ON orders(created_at);

CREATE INDEX CONCURRENTLY idx_orders_customer_id_created_at
ON orders(customer_id, created_at);

-- 驗證索引使用情況
EXPLAIN ANALYZE [your query];
```

4. **優化查詢**：

```java
// 之前：N+1 query 問題
public List<OrderDTO> getOrders() {
    List<Order> orders = orderRepository.findAll();
    return orders.stream()
        .map(order -> {
            Customer customer = customerRepository.findById(order.getCustomerId());
            return new OrderDTO(order, customer);
        })
        .collect(Collectors.toList());
}

// 之後：使用 JOIN FETCH
@Query("SELECT o FROM Order o JOIN FETCH o.customer WHERE o.createdAt > :date")
List<Order> findRecentOrdersWithCustomer(@Param("date") LocalDateTime date);
```

#### 如果由無效率的 caching 引起

1. **對頻繁存取的資料實施 caching**：

```java
@Service
public class ProductService {

    @Cacheable(value = "products", key = "#id", unless = "#result == null")
    public Product findById(String id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Cacheable(value = "productList", key = "#category")
    public List<Product> findByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    @CacheEvict(value = "products", key = "#product.id")
    public Product updateProduct(Product product) {
        return productRepository.save(product);
    }
}
```

2. **配置 cache TTL**：

```yaml
spring:
  cache:
    redis:
      time-to-live: 1800000  # 30 分鐘
    cache-names:
      - products
      - customers
      - categories
```

3. **實施 cache warming**：

```java
@Component
public class CacheWarmer {

    @Scheduled(cron = "0 0 * * * *")  // 每小時
    public void warmCache() {
        // 預載入頻繁存取的資料
        List<Product> popularProducts = productRepository.findPopularProducts();
        popularProducts.forEach(product ->
            cacheManager.getCache("products").put(product.getId(), product)
        );
    }
}
```

#### 如果由外部 API 緩慢引起

1. **實施 timeout 和 circuit breaker**：

```java
@Service
public class PaymentService {

    private final CircuitBreaker circuitBreaker;

    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "paymentFallback")
    @Retry(name = "paymentGateway", fallbackMethod = "paymentFallback")
    @TimeLimiter(name = "paymentGateway")
    public CompletableFuture<PaymentResult> processPayment(PaymentRequest request) {
        return CompletableFuture.supplyAsync(() ->
            paymentGatewayClient.process(request)
        );
    }

    public CompletableFuture<PaymentResult> paymentFallback(PaymentRequest request, Exception ex) {
        // 返回 cached 結果或將其排隊以供稍後處理
        return CompletableFuture.completedFuture(
            PaymentResult.pending("Payment queued for processing")
        );
    }
}
```

2. **配置 resilience4j**：

```yaml
resilience4j:
  circuitbreaker:
    instances:
      paymentGateway:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10000
        permittedNumberOfCallsInHalfOpenState: 3

  retry:
    instances:
      paymentGateway:
        maxAttempts: 3
        waitDuration: 1000
        exponentialBackoffMultiplier: 2

  timelimiter:
    instances:
      paymentGateway:
        timeoutDuration: 5s
```

#### 如果由無效率的程式碼引起

1. **分析 application**：

```bash
# 取得 CPU profile
kubectl exec -it ${POD_NAME} -n production -- \
  async-profiler.sh -d 60 -f /tmp/profile.html 1

# 本地複製 profile
kubectl cp production/${POD_NAME}:/tmp/profile.html ./profile.html
```

2. **優化 hot paths**：

```java
// 之前：無效率
public List<OrderDTO> getOrders() {
    return orderRepository.findAll().stream()
        .map(this::convertToDTO)
        .collect(Collectors.toList());
}

// 之後：使用 projection
@Query("SELECT new com.example.OrderDTO(o.id, o.total, c.name) " +
       "FROM Order o JOIN o.customer c")
List<OrderDTO> findAllOrderDTOs();

// 或使用 database view
@Query(value = "SELECT * FROM order_summary_view", nativeQuery = true)
List<OrderSummaryDTO> findOrderSummaries();
```

3. **實施 pagination**：

```java
@GetMapping("/orders")
public Page<OrderDTO> getOrders(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size) {

    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
    return orderService.findOrders(pageable);
}
```

#### 如果由 connection pool 耗盡引起

1. **增加 connection pool 大小**：

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 30
      minimum-idle: 15
      connection-timeout: 20000
      idle-timeout: 300000
```

2. **修復 connection 洩漏**：

```java
// 確保適當的資源清理
@Transactional
public void processOrder(Order order) {
    try {
        // 處理訂單
        orderRepository.save(order);
    } catch (Exception e) {
        // 處理 exception
        throw new OrderProcessingException("Failed to process order", e);
    }
    // Transaction 自動關閉
}
```

## 驗證

- [ ] API 回應時間 < 2s（95th percentile）
- [ ] 沒有 timeout 錯誤
- [ ] Database 查詢時間正常
- [ ] Cache hit rate > 80%
- [ ] 沒有 slow query alerts
- [ ] 外部 API 呼叫在 SLA 內
- [ ] 資源使用率正常
- [ ] 使用者體驗改善

### 驗證指令

```bash
# 監控回應時間
watch -n 5 'curl -s http://localhost:8080/actuator/metrics/http.server.requests | jq ".measurements[0].value"'

# 檢查 database performance
kubectl exec -it ${POD_NAME} -n production -- \
  psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} -c \
  "SELECT max(now() - query_start) as max_duration FROM pg_stat_activity WHERE state = 'active';"

# 驗證 cache hit rate
curl -s http://localhost:8080/actuator/metrics/cache.gets | jq
```

## 預防

### 1. Performance 測試

```bash
# 定期 load 測試
./scripts/load-test.sh --duration=30m --users=500

# Performance regression 測試
./scripts/performance-test.sh --baseline=v1.0.0 --current=v1.1.0
```

### 2. Database 優化

```sql
-- 定期維護
VACUUM ANALYZE;
REINDEX DATABASE ecommerce_production;

-- 監控索引使用情況
SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read, idx_tup_fetch
FROM pg_stat_user_indexes
WHERE idx_scan = 0
ORDER BY schemaname, tablename;
```

### 3. Monitoring 和 Alerting

```yaml
# 設定 performance alerts
- alert: APIResponseTimeDegrading
  expr: rate(http_request_duration_seconds_sum[5m]) / rate(http_request_duration_seconds_count[5m]) > 1
  for: 10m

- alert: DatabaseQuerySlow
  expr: rate(pg_stat_statements_mean_exec_time_seconds[5m]) > 0.5
  for: 5m
```

### 4. Code Review Checklist

- [ ] Database 查詢已優化
- [ ] 適當的索引已建立
- [ ] 在適當位置實施 caching
- [ ] 沒有 N+1 query 問題
- [ ] 大型資料集的 pagination
- [ ] Connection pooling 已配置
- [ ] 外部呼叫設定 timeouts
- [ ] Circuit breakers 已實施

## 升級處理

- **L1 Support**：DevOps team（scaling、cache 清除）
- **L2 Support**：Backend engineering team（query 優化、程式碼修復）
- **L3 Support**：Database administrator（database 調校）
- **On-Call Engineer**：查看 PagerDuty

## 相關

- [High CPU Usage](high-cpu-usage.md)
- [Database Connection Issues](database-connection-issues.md)
- [Cache Issues](cache-issues.md)
- [Service Outage](service-outage.md)

---

**Last Updated**：2025-10-25
**Owner**：DevOps Team
**Review Cycle**：Monthly
