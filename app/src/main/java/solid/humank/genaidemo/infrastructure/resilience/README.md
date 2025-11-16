# Application Resilience Patterns - 應用韌性模式

本套件提供了全面的災難恢復和業務連續性的 Resilience 模式。

## 📋 概述

Resilience Infrastructure 實作了以下模式：
- **Circuit Breaker**: 防止級聯故障
- **Retry**: 使用指數退避處理暫時性故障
- **Time Limiter**: 防止長時間運行的操作
- **Fallback**: 提供降級功能
- **Bulkhead**: 隔離資源
- **Rate Limiter**: 控制請求速率
- **Business Continuity Metrics**: 追蹤 RTO/RPO 和業務交易

## 🚀 快速開始

### 範例服務

查看 `ExampleResilientService` 以瞭解所有 Resilience 模式的全面範例：
- Circuit Breaker 搭配 Fallback
- Retry 搭配指數退避
- 組合模式 (Circuit Breaker + Retry + Time Limiter)
- Circuit Breaker 搭配快取
- 直接使用 ResilientServiceWrapper
- 嚴格 Circuit Breaker 的關鍵服務
- 寬鬆 Circuit Breaker 的非關鍵服務

### 1. 使用註解（推薦）

```java
@Service
public class MyService {

    @CircuitBreaker(name = "myService", fallbackMethod = "fallbackMethod")
    @Retry(name = "database")
    @Timed(value = "myService.operation")
    public String performOperation() {
        // 你的業務邏輯
        return "result";
    }

    private String fallbackMethod(Exception e) {
        return "fallback-result";
    }
}
```

### 2. 使用 ResilientServiceWrapper

```java
@Service
public class MyService {

    private final ResilientServiceWrapper resilientWrapper;

    public String performOperation() {
        return resilientWrapper.executeWithResilience(
            "myService",
            () -> {
                // 你的業務邏輯
                return "result";
            },
            () -> "fallback-result"
        );
    }
}
```

## 🔧 配置

### Circuit Breaker 配置

位於 `application-resilience.yml`：

```yaml
resilience4j:
  circuitbreaker:
    instances:
      myService:
        failure-rate-threshold: 50        # 在 50% 故障時開啟 Circuit
        slow-call-rate-threshold: 50      # 在 50% 慢速呼叫時開啟 Circuit
        slow-call-duration-threshold: 2s  # 超過 2 秒的呼叫視為慢速
        wait-duration-in-open-state: 30s  # 在開啟狀態下等待 30 秒後進入半開
        minimum-number-of-calls: 10       # 計算前需要至少 10 次呼叫
```

### Retry 配置

```yaml
resilience4j:
  retry:
    instances:
      myService:
        max-attempts: 3                   # 最多重試 3 次
        wait-duration: 500ms              # 初始等待時間
        exponential-backoff-multiplier: 2 # 指數退避乘數
```

## 📊 監控

### Circuit Breaker 指標

```java
// 取得 Circuit Breaker 狀態
String state = resilientWrapper.getCircuitBreakerState("myService");

// 取得 Circuit Breaker 指標
CircuitBreakerMetrics metrics = resilientWrapper.getCircuitBreakerMetrics("myService");
System.out.println("故障率: " + metrics.failureRate());
System.out.println("成功呼叫數: " + metrics.successfulCalls());
System.out.println("失敗呼叫數: " + metrics.failedCalls());
```

### 業務連續性指標

```java
@Service
public class MyService {

    private final RecoveryMetricsTracker recoveryTracker;
    private final BusinessTransactionMetricsTracker transactionTracker;

    public void handleIncident() {
        // 記錄事件開始
        recoveryTracker.recordIncidentStart("incident-123", "database-failure");

        // 執行恢復
        performRecovery();

        // 記錄成功恢復
        recoveryTracker.recordRecoverySuccess("incident-123", "database-failure");
    }

    public void performTransaction() {
        Instant start = Instant.now();
        boolean success = false;

        try {
            // 業務邏輯
            success = true;
        } finally {
            Duration duration = Duration.between(start, Instant.now());
            transactionTracker.recordTransaction("order.create", success, duration);
        }
    }
}
```

## 🎯 使用案例

### 1. 數據庫操作

```java
@Service
public class CustomerService {

    @CircuitBreaker(name = "database", fallbackMethod = "findByIdFallback")
    @Retry(name = "database")
    @Cacheable("customers")
    public Optional<Customer> findById(String id) {
        return customerRepository.findById(id);
    }

    private Optional<Customer> findByIdFallback(String id, Exception e) {
        logger.warn("為客戶使用降級方案: {}", id);
        return Optional.empty();
    }
}
```

### 2. 外部 API 呼叫

```java
@Service
public class PaymentService {

    @CircuitBreaker(name = "paymentService", fallbackMethod = "processPaymentFallback")
    @Retry(name = "externalApi")
    @TimeLimiter(name = "externalApi")
    public PaymentResult processPayment(PaymentRequest request) {
        return paymentGateway.process(request);
    }

    private PaymentResult processPaymentFallback(PaymentRequest request, Exception e) {
        // 加入隊列稍後處理
        paymentQueue.add(request);
        return PaymentResult.pending();
    }
}
```

### 3. 含超時的非同步操作

```java
@Service
public class ReportService {

    public CompletableFuture<Report> generateReport(String reportId) {
        return resilientWrapper.executeWithTimeout(
            "reportService",
            () -> CompletableFuture.supplyAsync(() -> {
                // 長時間運行的報告生成
                return reportGenerator.generate(reportId);
            }),
            Duration.ofSeconds(30)
        );
    }
}
```

## 📈 可用指標

### Resilience 指標

- `resilience.operation.success` - 成功操作數
- `resilience.operation.failure` - 失敗操作數
- `resilience.operation.duration` - 操作耗時
- `resilience.fallback.success` - 成功的降級執行數
- `resilience.fallback.failure` - 失敗的降級執行數

### 業務連續性指標

- `business.continuity.rto.target.seconds` - 目標 RTO (120 秒)
- `business.continuity.rpo.target.seconds` - 目標 RPO (1 秒)
- `business.continuity.rto.actual` - 實際恢復時間
- `business.continuity.rpo.actual` - 實際數據損失時間
- `business.continuity.incidents.total` - 總事件數
- `business.continuity.recoveries.successful` - 成功恢復數
- `business.continuity.recovery.success.rate` - 恢復成功率

### 業務交易指標

- `business.transactions.total` - 交易總數
- `business.transactions.success` - 成功交易數
- `business.transactions.failure` - 失敗交易數
- `business.transactions.duration` - 交易耗時
- `business.value.*` - 業務價值指標（收入、訂單等）

## 🔍 健康檢查

Circuit Breaker 和 Rate Limiter 會自動公開為健康指標：

```bash
# 檢查應用健康狀態
curl http://localhost:8080/actuator/health

# 回應包含 Circuit Breaker 狀態
{
  "status": "UP",
  "components": {
    "circuitBreakers": {
      "status": "UP",
      "details": {
        "customerService": "CLOSED",
        "database": "CLOSED",
        "externalApi": "CLOSED"
      }
    }
  }
}
```

## 📊 Prometheus 指標

所有指標都會自動匯出到 Prometheus：

```bash
# 取得 Prometheus 指標
curl http://localhost:8080/actuator/prometheus

# 範例指標
resilience4j_circuitbreaker_state{name="customerService",state="closed"} 1.0
resilience4j_circuitbreaker_failure_rate{name="customerService"} 0.0
resilience4j_retry_calls_total{name="database",kind="successful_without_retry"} 100.0
business_continuity_rto_actual_seconds_max{type="database-failure"} 45.0
business_transactions_total{type="order.create",status="success"} 1000.0
```

## 🎨 最佳實踐

### 1. 選擇適當的配置

- **關鍵服務**: 使用 `critical` 配置（更積極）
- **非關鍵服務**: 使用 `lenient` 配置（更寬鬆）
- **預設**: 大多數服務使用 `default` 配置

### 2. 實現有意義的降級方案

```java
// ✅ 正確: 提供降級功能
private List<Product> findProductsFallback(Exception e) {
    return cachedProducts.getRecentProducts();
}

// ❌ 錯誤: 直接回傳空集合
private List<Product> findProductsFallback(Exception e) {
    return Collections.emptyList();
}
```

### 3. 不要重試非冪等操作

```java
// ✅ 正確: 建立操作無重試
@CircuitBreaker(name = "orderService")
public Order createOrder(OrderRequest request) {
    return orderRepository.save(request);
}

// ❌ 錯誤: 重試可能建立重複資料
@Retry(name = "orderService")
public Order createOrder(OrderRequest request) {
    return orderRepository.save(request);
}
```

### 4. 搭配快取使用 Circuit Breaker

```java
@CircuitBreaker(name = "productService", fallbackMethod = "findByIdFallback")
@Cacheable("products")
public Product findById(String id) {
    return productRepository.findById(id);
}

private Product findByIdFallback(String id, Exception e) {
    // 可用時使用快取
    return null;
}
```

### 5. 監控和告警

設置以下項目的告警：
- Circuit Breaker 狀態變化
- 高故障率
- RTO/RPO 目標未達成
- 降級使用率突增

## 🧪 測試

### 單元測試

```java
@Test
void should_use_fallback_when_service_fails() {
    // 給定
    when(externalService.call()).thenThrow(new RuntimeException());

    // 當
    String result = myService.performOperation();

    // 那麼
    assertThat(result).isEqualTo("fallback-result");
}
```

### 整合測試

```java
@SpringBootTest
@TestPerformanceExtension
class ResilientServiceIntegrationTest {

    @Test
    void should_recover_from_database_failure() {
        // 模擬數據庫故障
        // 驗證 Circuit Breaker 開啟
        // 驗證降級方案被使用
        // 模擬數據庫恢復
        // 驗證 Circuit Breaker 關閉
    }
}
```

## 📚 參考資料

- [Resilience4j 文檔](https://resilience4j.readme.io/)
- [Micrometer 文檔](https://micrometer.io/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Circuit Breaker 模式](https://martinfowler.com/bliki/CircuitBreaker.html)

## 🤝 貢獻

添加新的 Resilience 模式時：
1. 將配置添加到 `application-resilience.yml`
2. 在本 README 中記錄使用方法
3. 添加單元測試
4. 添加整合測試
5. 更新指標文檔
