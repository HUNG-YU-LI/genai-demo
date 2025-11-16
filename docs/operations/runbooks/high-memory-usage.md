# Runbook: 高記憶體使用率

## 症狀

- 記憶體使用率持續超過 90%
- 應用程式日誌中出現 OutOfMemoryError
- Pod 被 OOMKiller 終止
- Garbage collection 緩慢
- 應用程式無回應
- Swap 使用量增加

## 影響

- **嚴重性**: P0 - Critical
- **受影響使用者**: 所有使用者可能會遇到服務中斷
- **業務影響**: 服務崩潰、資料遺失風險、完全中斷

## 偵測

- **Alert**: `HighMemoryUsage` alert 觸發
- **Monitoring Dashboard**: Operations Dashboard > Infrastructure > Memory Utilization
- **Log Patterns**:
  - `java.lang.OutOfMemoryError`
  - `GC overhead limit exceeded`
  - pod events 中出現 `OOMKilled`

## 診斷

### 步驟 1: 識別記憶體使用模式

```bash
# Check current memory usage
kubectl top pods -n production -l app=ecommerce-backend

# Get detailed pod resource usage
kubectl describe pod ${POD_NAME} -n production | grep -A 10 "Limits\|Requests"

# Check pod events for OOM kills
kubectl get events -n production --field-selector involvedObject.name=${POD_NAME} | grep OOM
```

### 步驟 2: 分析 Heap Memory

```bash
# Get heap dump (WARNING: This will pause the application briefly)
kubectl exec -it ${POD_NAME} -n production -- \
  jmap -dump:live,format=b,file=/tmp/heap-dump.hprof 1

# Copy heap dump locally for analysis
kubectl cp production/${POD_NAME}:/tmp/heap-dump.hprof \
  ./heap-dump-$(date +%Y%m%d-%H%M%S).hprof

# Get heap histogram (lighter weight)
kubectl exec -it ${POD_NAME} -n production -- \
  jmap -histo:live 1 | head -50
```

### 步驟 3: 檢查 Garbage Collection

```bash
# Check GC logs
kubectl logs ${POD_NAME} -n production | grep -i "gc\|garbage"

# Get GC statistics
kubectl exec -it ${POD_NAME} -n production -- \
  jstat -gcutil 1 1000 10

# Check for memory leaks
kubectl exec -it ${POD_NAME} -n production -- \
  jstat -gccause 1
```

### 步驟 4: 分析應用程式 Metrics

```bash
# Check JVM memory metrics
curl http://localhost:8080/actuator/metrics/jvm.memory.used
curl http://localhost:8080/actuator/metrics/jvm.memory.max

# Check heap memory by area
curl http://localhost:8080/actuator/metrics/jvm.memory.used?tag=area:heap
curl http://localhost:8080/actuator/metrics/jvm.memory.used?tag=area:nonheap

# Check memory pools
curl http://localhost:8080/actuator/metrics/jvm.memory.used?tag=id:PS\ Eden\ Space
curl http://localhost:8080/actuator/metrics/jvm.memory.used?tag=id:PS\ Old\ Gen
```

### 步驟 5: 識別記憶體洩漏

```bash
# Check for common leak patterns in logs
kubectl logs ${POD_NAME} -n production --tail=5000 | grep -i "leak\|retain\|cache"

# Check thread count (threads consume memory)
kubectl exec -it ${POD_NAME} -n production -- \
  jstack 1 | grep "^\"" | wc -l

# List all threads
kubectl exec -it ${POD_NAME} -n production -- \
  jstack 1 > thread-dump-$(date +%Y%m%d-%H%M%S).txt
```

## 解決方案

### 立即行動

1. **重啟受影響的 pod** (暫時緩解):

```bash
# Delete pod to trigger restart
kubectl delete pod ${POD_NAME} -n production

# Or restart entire deployment
kubectl rollout restart deployment/ecommerce-backend -n production
```

1. **水平擴展** 以分散負載:

```bash
# Increase replica count
kubectl scale deployment/ecommerce-backend --replicas=8 -n production
```

1. **強制 garbage collection** (如果 pod 仍有回應):

```bash
kubectl exec -it ${POD_NAME} -n production -- \
  jcmd 1 GC.run
```

### 根本原因修復

#### 如果是由記憶體配置不足引起

1. **增加記憶體限制**:

```yaml
# Update deployment
resources:
  requests:
    memory: "2Gi"    # Increase from 1Gi
  limits:
    memory: "4Gi"    # Increase from 2Gi
```

1. **調整 JVM heap 設定**:

```yaml
env:

  - name: JAVA_OPTS

    value: "-Xms2g -Xmx3g -XX:MaxMetaspaceSize=512m"
```

1. **套用變更**:

```bash
kubectl apply -f deployment.yaml
kubectl rollout status deployment/ecommerce-backend -n production
```

#### 如果是由記憶體洩漏引起

1. **分析 heap dump** 使用工具如:
   - Eclipse Memory Analyzer (MAT)
   - VisualVM
   - JProfiler

2. **常見的洩漏模式**:
   - 未關閉的資料庫連線
   - 無界限的 cache
   - 無限增長的 static collection
   - 未移除的 event listener
   - 未清理的 ThreadLocal 變數

3. **修復已識別的洩漏**:

```java
// Example: Fix cache leak
@Configuration
public class CacheConfiguration {
    @Bean
    public CacheManager cacheManager() {
        return CacheManagerBuilder.newCacheManagerBuilder()
            .withCache("products",
                CacheConfigurationBuilder.newCacheConfigurationBuilder(
                    String.class, Product.class,
                    ResourcePoolsBuilder.heap(1000)  // Limit cache size
                        .offheap(100, MemoryUnit.MB))
                .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofMinutes(30)))
            )
            .build(true);
    }
}
```

#### 如果是由大型物件配置引起

1. **從 heap histogram 識別大型物件**
2. **最佳化資料結構**:

```java
// Example: Stream large datasets instead of loading all
public Stream<Order> findOrdersStream(LocalDate date) {
    return orderRepository.findByDateStream(date);
}

// Process in batches
public void processOrders(LocalDate date) {
    try (Stream<Order> orders = findOrdersStream(date)) {
        orders.forEach(order -> {
            processOrder(order);
            // Object eligible for GC after processing
        });
    }
}
```

#### 如果是由 garbage collection 效率不佳引起

1. **調整 GC 參數**:

```yaml
env:

  - name: JAVA_OPTS

    value: >-
      -Xms2g -Xmx3g
      -XX:+UseG1GC
      -XX:MaxGCPauseMillis=200
      -XX:ParallelGCThreads=4
      -XX:ConcGCThreads=2
      -XX:InitiatingHeapOccupancyPercent=45
      -XX:+PrintGCDetails
      -XX:+PrintGCDateStamps
      -Xloggc:/var/log/gc.log
```

1. **監控 GC 效能**:

```bash
# Analyze GC logs
kubectl logs ${POD_NAME} -n production | grep "GC" > gc.log
# Use GCViewer or similar tool to analyze
```

## 驗證

- [ ] 記憶體使用率降至 80% 以下
- [ ] 無 OOMKilled 事件
- [ ] GC 暫停時間可接受 (< 200ms)
- [ ] 應用程式正常回應
- [ ] 日誌中無記憶體相關錯誤
- [ ] Heap 使用率隨時間穩定
- [ ] 監控中未偵測到記憶體洩漏

### 驗證指令

```bash
# Monitor memory usage over time
watch -n 5 'kubectl top pod ${POD_NAME} -n production'

# Check for OOM events
kubectl get events -n production | grep OOM

# Verify GC is working properly
kubectl logs ${POD_NAME} -n production | grep "GC" | tail -20

# Check application health
curl http://localhost:8080/actuator/health
```

## 預防措施

### 1. 正確的記憶體配置

```yaml
# Set appropriate resource limits
resources:
  requests:
    memory: "2Gi"
    cpu: "500m"
  limits:
    memory: "4Gi"
    cpu: "1000m"

# Configure JVM heap
env:

  - name: JAVA_OPTS

    value: >-
      -Xms2g
      -Xmx3g
      -XX:MaxMetaspaceSize=512m
      -XX:+UseG1GC
      -XX:MaxGCPauseMillis=200
```

### 2. 程式碼最佳實踐

```java
// Use try-with-resources for auto-cleanup
try (Connection conn = dataSource.getConnection();
     PreparedStatement stmt = conn.prepareStatement(sql)) {
    // Use connection
} // Automatically closed

// Limit cache sizes
@Cacheable(value = "products", unless = "#result == null")
public Product findById(String id) {
    return productRepository.findById(id);
}

// Configure cache eviction
@CacheEvict(value = "products", allEntries = true)
@Scheduled(fixedRate = 3600000) // Every hour
public void evictAllCaches() {
    // Cache cleared automatically
}

// Stream large datasets
public void processLargeDataset() {
    try (Stream<Order> orders = orderRepository.streamAll()) {
        orders.forEach(this::processOrder);
    }
}
```

### 3. 監控與告警

```yaml
# Set up memory monitoring alerts

- alert: MemoryUsageIncreasing

  expr: rate(jvm_memory_used_bytes{area="heap"}[5m]) > 0
  for: 30m
  annotations:
    summary: "Memory usage is steadily increasing"

- alert: HighGCTime

  expr: rate(jvm_gc_pause_seconds_sum[5m]) > 0.1
  for: 10m
  annotations:
    summary: "High GC time detected"
```

### 4. 定期記憶體分析

```bash
# Schedule regular heap dumps during low traffic
# Analyze for memory leaks
# Compare heap dumps over time

# Weekly memory analysis
./scripts/analyze-memory-usage.sh
```

### 5. 負載測試

```bash
# Regular load testing to identify memory issues
./scripts/load-test.sh --duration=1h --users=1000

# Monitor memory during load test
watch -n 10 'kubectl top pods -n staging'
```

## 升級程序

- **L1 Support**: DevOps team (立即重啟)
- **L2 Support**: Backend engineering team (記憶體洩漏調查)
- **L3 Support**: Senior architect (JVM 調校、架構檢視)
- **On-Call Engineer**: 查看 PagerDuty

## 相關文件

- [High CPU Usage](high-cpu-usage.md)
- [Pod Restart Loop](pod-restart-loop.md)
- [Slow API Responses](slow-api-responses.md)
- [Service Outage](service-outage.md)

## 額外資源

- [Java Memory Management Guide](https://docs.oracle.com/javase/8/docs/technotes/guides/vm/gctuning/)
- [G1GC Tuning Guide](https://www.oracle.com/technical-resources/articles/java/g1gc.html)
- [Eclipse Memory Analyzer](https://www.eclipse.org/mat/)
- [JVM Memory Analysis Tools](https://docs.oracle.com/javase/8/docs/technotes/guides/troubleshoot/memleaks.html)

---

**Last Updated**: 2025-10-25
**Owner**: DevOps Team
**Review Cycle**: Monthly
