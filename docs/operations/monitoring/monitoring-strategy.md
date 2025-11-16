# Monitoring 策略

## 概述

本文件描述 Enterprise E-Commerce Platform 的全面 monitoring 策略，包括關鍵 metrics、dashboards 和 monitoring 工具。

## Monitoring 目標

- **可用性**：確保 99.9% uptime SLA
- **效能**：維持回應時間 < 2s（95th percentile）
- **可靠性**：在影響客戶前偵測並解決問題
- **容量**：主動管理資源使用率
- **業務**：追蹤關鍵業務 metrics 和 KPIs

## Monitoring Stack

### 工具和服務

| 工具 | 用途 | 環境 |
|------|------|------|
| AWS CloudWatch | Metrics、logs、alarms | All |
| AWS X-Ray | Distributed tracing | All |
| Grafana | 視覺化和 dashboards | All |
| Prometheus | Metrics 收集 | Staging、Production |
| ELK Stack | Log aggregation 和分析 | Production |
| PagerDuty | Incident 管理和 alerting | Production |

## 關鍵 Metrics

### Application Metrics

#### API Performance Metrics

| Metric | 描述 | 目標 | Alert 閾值 |
|--------|------|------|-----------|
| Response Time (p50) | 中位數 API 回應時間 | < 500ms | > 1s |
| Response Time (p95) | 95th percentile 回應時間 | < 2s | > 3s |
| Response Time (p99) | 99th percentile 回應時間 | < 5s | > 10s |
| Request Rate | 每秒請求數 | N/A | Baseline ±50% |
| Error Rate | 失敗請求百分比 | < 0.1% | > 1% |
| Success Rate | 成功請求百分比 | > 99.9% | < 99% |

#### Business Metrics

| Metric | 描述 | 目標 | Alert 閾值 |
|--------|------|------|-----------|
| Orders per Minute | 訂單建立速率 | N/A | < 50% of baseline |
| Payment Success Rate | 成功付款百分比 | > 98% | < 95% |
| Cart Abandonment Rate | 放棄購物車百分比 | < 70% | > 80% |
| Average Order Value | 平均訂單金額 | N/A | < 80% of baseline |
| Customer Registration Rate | 新客戶註冊數 | N/A | < 50% of baseline |

### Infrastructure Metrics

#### Compute Metrics

| Metric | 描述 | 目標 | Alert 閾值 |
|--------|------|------|-----------|
| CPU Utilization | Pod CPU 使用率 | < 70% | > 80% |
| Memory Utilization | Pod memory 使用率 | < 80% | > 90% |
| Pod Count | 運行中的 pod 數量 | N/A | < min replicas |
| Pod Restart Count | Pod 重啟次數 | 0 | > 3 in 15 min |
| Node CPU | Node 層級 CPU 使用率 | < 70% | > 85% |
| Node Memory | Node 層級 memory 使用率 | < 80% | > 90% |

#### Database Metrics

| Metric | 描述 | 目標 | Alert 閾值 |
|--------|------|------|-----------|
| Connection Count | 活動 database 連線數 | < 80% of max | > 90% of max |
| Query Duration (p95) | 95th percentile 查詢時間 | < 100ms | > 500ms |
| Slow Query Count | 查詢 > 1s | 0 | > 10 per minute |
| Replication Lag | Read replica lag | < 1s | > 5s |
| Deadlock Count | Database deadlocks | 0 | > 1 per hour |
| Database CPU | RDS CPU 使用率 | < 70% | > 80% |
| Database Storage | Storage 使用率 | < 80% | > 90% |

#### Cache Metrics

| Metric | 描述 | 目標 | Alert 閾值 |
|--------|------|------|-----------|
| Cache Hit Rate | Cache hits 百分比 | > 80% | < 70% |
| Cache Miss Rate | Cache misses 百分比 | < 20% | > 30% |
| Eviction Rate | 每秒 cache evictions | < 100 | > 500 |
| Memory Usage | Redis memory 使用率 | < 80% | > 90% |
| Connection Count | 活動 Redis 連線數 | < 80% of max | > 90% of max |

#### Message Queue Metrics

| Metric | 描述 | 目標 | Alert 閾值 |
|--------|------|------|-----------|
| Message Lag | Consumer lag（訊息數） | < 1000 | > 10000 |
| Consumer Lag Time | 時間 lag（秒） | < 10s | > 60s |
| Message Rate | 每秒訊息數 | N/A | Baseline ±50% |
| Error Rate | 失敗的訊息處理 | < 0.1% | > 1% |
| Partition Count | Partition 數量 | N/A | Changes detected |

## Monitoring Dashboards

### Executive Dashboard

**用途**：管理層的高階系統健康狀態

**Metrics**：

- 系統可用性（uptime 百分比）
- 每分鐘總請求數
- 錯誤率
- 活動使用者
- 每小時訂單數
- 每小時收入

**更新率**：1 分鐘

### Operations Dashboard

**用途**：即時營運監控

**Panels**：

1. **系統健康狀態**
   - Service 狀態（up/down）
   - Pod 健康狀態
   - Database 狀態
   - Cache 狀態

2. **效能**
   - API 回應時間（p50、p95、p99）
   - 請求速率
   - 錯誤率
   - 成功率

3. **基礎設施**
   - CPU 使用率
   - Memory 使用率
   - Network I/O
   - Disk I/O

4. **Alerts**
   - 活動 alerts
   - 最近 incidents
   - Alert 歷史

**更新率**：30 秒

### Application Dashboard

**用途**：Application 特定 metrics

**Panels**：

1. **API Endpoints**
   - 依 endpoint 的回應時間
   - 依 endpoint 的請求數
   - 依 endpoint 的錯誤率

2. **Business Metrics**
   - 每分鐘訂單數
   - 付款成功率
   - 購物車操作
   - 客戶註冊

3. **依賴關係**
   - Database 查詢效能
   - Cache hit rate
   - 外部 API 回應時間

**更新率**：1 分鐘

### Database Dashboard

**用途**：Database performance 監控

**Panels**：

1. **連線**
   - 活動連線
   - 閒置連線
   - Connection pool 使用率

2. **效能**
   - 查詢時間（p50、p95、p99）
   - 慢速查詢
   - Deadlocks
   - Lock waits

3. **資源**
   - CPU 使用率
   - Memory 使用率
   - Storage 使用率
   - IOPS

4. **Replication**
   - Replication lag
   - Replica 狀態

**更新率**：1 分鐘

## Monitoring 實施

### CloudWatch Metrics

```java
@Component
public class MetricsPublisher {

    private final MeterRegistry meterRegistry;

    @Autowired
    public MetricsPublisher(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordApiRequest(String endpoint, long duration, int statusCode) {
        Timer.builder("api.request.duration")
            .tag("endpoint", endpoint)
            .tag("status", String.valueOf(statusCode))
            .register(meterRegistry)
            .record(duration, TimeUnit.MILLISECONDS);

        Counter.builder("api.request.count")
            .tag("endpoint", endpoint)
            .tag("status", String.valueOf(statusCode))
            .register(meterRegistry)
            .increment();
    }

    public void recordBusinessMetric(String metricName, double value) {
        Gauge.builder("business." + metricName, () -> value)
            .register(meterRegistry);
    }
}
```

### X-Ray Tracing

```java
@Component
public class TracingInterceptor implements HandlerInterceptor {

    private final Tracer tracer;

    @Override
    public boolean preHandle(HttpServletRequest request,
                            HttpServletResponse response,
                            Object handler) {
        Span span = tracer.nextSpan()
            .name(request.getMethod() + " " + request.getRequestURI())
            .tag("http.method", request.getMethod())
            .tag("http.url", request.getRequestURI())
            .start();

        request.setAttribute("span", span);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                               HttpServletResponse response,
                               Object handler,
                               Exception ex) {
        Span span = (Span) request.getAttribute("span");
        if (span != null) {
            span.tag("http.status_code", String.valueOf(response.getStatus()));
            if (ex != null) {
                span.tag("error", ex.getMessage());
            }
            span.end();
        }
    }
}
```

### Prometheus Metrics

```yaml
# prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'ecommerce-backend'
    kubernetes_sd_configs:
      - role: pod
        namespaces:
          names:
            - production
    relabel_configs:
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
        action: keep
        regex: true
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
        action: replace
        target_label: __metrics_path__
        regex: (.+)
      - source_labels: [__address__, __meta_kubernetes_pod_annotation_prometheus_io_port]
        action: replace
        regex: ([^:]+)(?::\d+)?;(\d+)
        replacement: $1:$2
        target_label: __address__
```

## Log Monitoring

### Log Levels

| Level | 使用 | 範例 |
|-------|------|------|
| ERROR | 需要立即關注的系統錯誤 | Exceptions、失敗的操作 |
| WARN | 潛在問題或效能下降 | 慢速查詢、高 memory 使用率 |
| INFO | 重要的業務事件 | 訂單已創建、付款已處理 |
| DEBUG | 詳細的執行流程 | 方法進入/退出、變數值 |
| TRACE | 非常詳細的除錯 | SQL 查詢、HTTP 請求 |

### Structured Logging

```java
@Slf4j
@Component
public class StructuredLogger {

    public void logBusinessEvent(String event, Map<String, Object> context) {
        log.info("Business event: {}",
            kv("event", event),
            kv("timestamp", Instant.now()),
            kv("traceId", getCurrentTraceId()),
            kv("context", context));
    }

    public void logError(String message, Exception ex, Map<String, Object> context) {
        log.error("Error occurred: {}",
            kv("message", message),
            kv("timestamp", Instant.now()),
            kv("traceId", getCurrentTraceId()),
            kv("errorType", ex.getClass().getSimpleName()),
            kv("context", context),
            ex);
    }
}
```

### Log Aggregation

```yaml
# Filebeat configuration
filebeat.inputs:
  - type: container
    paths:
      - '/var/log/containers/*.log'
    processors:
      - add_kubernetes_metadata:
          host: ${NODE_NAME}
          matchers:
            - logs_path:
                logs_path: "/var/log/containers/"

output.elasticsearch:
  hosts: ["${ELASTICSEARCH_HOST}:9200"]
  index: "ecommerce-logs-%{+yyyy.MM.dd}"
```

## Health Checks

### Application Health Checks

```java
@Component
public class CustomHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        try {
            // 檢查 database
            checkDatabase();

            // 檢查 Redis
            checkRedis();

            // 檢查 Kafka
            checkKafka();

            return Health.up()
                .withDetail("database", "UP")
                .withDetail("redis", "UP")
                .withDetail("kafka", "UP")
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

### Kubernetes Health Checks

```yaml
# Liveness probe
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3

# Readiness probe
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 5
  timeoutSeconds: 3
  failureThreshold: 3
```

## Monitoring 最佳實踐

### Metric 收集

- **使用一致的命名**：遵循命名慣例
- **使用 tags 增加上下文**：包含 environment、service、endpoint
- **避免高基數**：不要使用 user IDs 或 timestamps 作為 tags
- **設定適當的間隔**：平衡詳細程度與開銷

### Dashboard 設計

- **保持簡單**：專注於關鍵 metrics
- **使用適當的視覺化**：選擇正確的圖表類型
- **設定有意義的閾值**：使用紅/黃/綠指標
- **分組相關 metrics**：邏輯組織

### Alert 配置

- **避免 alert fatigue**：僅對可操作的問題發出 alert
- **使用適當的閾值**：基於歷史資料
- **設定升級政策**：定義何時通知誰
- **包含上下文**：提供足夠的資訊以採取行動

## Monitoring Checklist

### 每日 Monitoring

- [ ] 檢查系統健康狀態 dashboard
- [ ] 審查錯誤率
- [ ] 檢查資源使用率
- [ ] 審查最近的 alerts
- [ ] 檢查業務 metrics

### 每週 Monitoring

- [ ] 審查效能趨勢
- [ ] 分析慢速查詢
- [ ] 檢查容量規劃 metrics
- [ ] 審查 alert 有效性
- [ ] 根據需要更新 dashboards

### 每月 Monitoring

- [ ] 審查 SLA 合規性
- [ ] 分析長期趨勢
- [ ] 更新 alert 閾值
- [ ] 審查 monitoring 覆蓋範圍
- [ ] 進行 monitoring 回顧

## 相關文件

- [Alert 配置](alerts.md)
- [疑難排解指南](../troubleshooting/common-issues.md)
- [Runbooks](../runbooks/README.md)
- [Deployment 流程](../deployment/deployment-process.md)

---

**Last Updated**：2025-10-25
**Owner**：DevOps Team
**Review Cycle**：Quarterly
