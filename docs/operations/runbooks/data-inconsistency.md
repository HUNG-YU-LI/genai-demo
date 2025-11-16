# Runbook: Data Inconsistency

## Symptoms

- Services 之間的資料不匹配
- Database 中的重複記錄
- 應該存在的記錄遺失
- 不正確的計算值
- Foreign key constraint 違規
- Event 處理失敗
- 客戶回報資料不正確

## Impact

- **Severity**：P1 - High（如果影響 payments 或 orders 則為 P0）
- **Affected Users**：依資料類型而異
- **Business Impact**：客戶信任流失、不正確的帳單、訂單履行問題

## Detection

- **Alert**：`DataInconsistencyDetected` alert 觸發
- **Monitoring Dashboard**：Data Quality Dashboard
- **Log Patterns**：
  - `Foreign key constraint violation`
  - `Duplicate key value`
  - `Data mismatch detected`
  - `Reconciliation failed`

## Diagnosis

### 步驟 1：識別 Inconsistency 範圍

```bash
# Check application logs for data errors
kubectl logs deployment/ecommerce-backend -n production --tail=5000 | \
  grep -i "inconsisten\|mismatch\|duplicate\|constraint"

# Check database logs
kubectl exec -it ${POD_NAME} -n production -- \
  psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} -c \
  "SELECT * FROM pg_stat_database_conflicts WHERE datname = 'ecommerce_production';"
```

### 步驟 2：執行資料完整性檢查

```sql
-- Connect to database
kubectl exec -it ${POD_NAME} -n production -- \
  psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME}

-- Check for orphaned orders (orders without customers)
SELECT COUNT(*) as orphaned_orders
FROM orders o
LEFT JOIN customers c ON o.customer_id = c.id
WHERE c.id IS NULL;

-- Check for duplicate customer emails
SELECT email, COUNT(*) as count
FROM customers
GROUP BY email
HAVING COUNT(*) > 1;

-- Check for orders with invalid totals
SELECT o.id, o.total_amount, SUM(oi.quantity * oi.unit_price) as calculated_total
FROM orders o
JOIN order_items oi ON o.id = oi.order_id
GROUP BY o.id, o.total_amount
HAVING o.total_amount != SUM(oi.quantity * oi.unit_price);

-- Check for inventory inconsistencies
SELECT p.id, p.name, p.stock_quantity,
       COALESCE(SUM(i.quantity), 0) as actual_inventory
FROM products p
LEFT JOIN inventory i ON p.id = i.product_id
GROUP BY p.id, p.name, p.stock_quantity
HAVING p.stock_quantity != COALESCE(SUM(i.quantity), 0);

-- Check for payment-order mismatches
SELECT o.id as order_id, o.total_amount as order_total,
       p.amount as payment_amount, p.status as payment_status
FROM orders o
LEFT JOIN payments p ON o.id = p.order_id
WHERE o.status = 'COMPLETED'
  AND (p.id IS NULL OR p.status != 'COMPLETED' OR p.amount != o.total_amount);
```

### 步驟 3：檢查 Event 處理

```bash
# Check Kafka consumer lag
kubectl exec -it kafka-0 -n production -- \
  kafka-consumer-groups --bootstrap-server localhost:9092 \
  --describe --group ecommerce-consumer

# Check for failed events in dead letter queue
kubectl exec -it kafka-0 -n production -- \
  kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic ecommerce-dlq --from-beginning --max-messages 100

# Check event processing logs
kubectl logs deployment/ecommerce-backend -n production --tail=1000 | \
  grep -i "event.*failed\|event.*error"
```

### 步驟 4：檢查 Cache 一致性

```bash
# Check Redis for stale data
kubectl exec -it redis-0 -n production -- redis-cli

# Get cached customer data
GET customer:12345

# Compare with database
kubectl exec -it ${POD_NAME} -n production -- \
  psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} -c \
  "SELECT * FROM customers WHERE id = '12345';"

# Check cache TTL
TTL customer:12345

# Check for cache keys that should have expired
SCAN 0 MATCH customer:* COUNT 1000
```

### 步驟 5：分析最近的變更

```bash
# Check recent deployments
kubectl rollout history deployment/ecommerce-backend -n production

# Check recent database migrations
kubectl exec -it ${POD_NAME} -n production -- \
  psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} -c \
  "SELECT * FROM flyway_schema_history ORDER BY installed_on DESC LIMIT 10;"

# Check recent configuration changes
kubectl get configmap ecommerce-config -n production -o yaml | grep -A 5 "last-applied"
```

### 步驟 6：檢查 Race Conditions

```bash
# Check for concurrent updates
kubectl exec -it ${POD_NAME} -n production -- \
  psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} -c \
  "SELECT pid, usename, query_start, state, query
   FROM pg_stat_activity
   WHERE query LIKE '%UPDATE orders%' OR query LIKE '%UPDATE inventory%';"

# Check for lock conflicts
kubectl exec -it ${POD_NAME} -n production -- \
  psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} -c \
  "SELECT blocked_locks.pid AS blocked_pid,
          blocked_activity.usename AS blocked_user,
          blocking_locks.pid AS blocking_pid,
          blocking_activity.usename AS blocking_user,
          blocked_activity.query AS blocked_statement,
          blocking_activity.query AS blocking_statement
   FROM pg_catalog.pg_locks blocked_locks
   JOIN pg_catalog.pg_stat_activity blocked_activity ON blocked_activity.pid = blocked_locks.pid
   JOIN pg_catalog.pg_locks blocking_locks ON blocking_locks.locktype = blocked_locks.locktype
   JOIN pg_catalog.pg_stat_activity blocking_activity ON blocking_activity.pid = blocking_locks.pid
   WHERE NOT blocked_locks.granted;"
```

## Resolution

### 立即行動

1. **停止寫入**受影響的資料（如果關鍵）：

```bash
# Scale down to prevent further inconsistency
kubectl scale deployment/ecommerce-backend --replicas=0 -n production

# Or enable read-only mode
kubectl set env deployment/ecommerce-backend \
  READ_ONLY_MODE=true \
  -n production
```

1. **通知 stakeholders**：

```bash
# Send incident notification
# Subject: P1 - Data Inconsistency Detected
# Data inconsistency detected in [affected area].
# Team is investigating and will provide updates.
```

### 根本原因修復

#### 如果由失敗的 event 處理造成

1. **重播失敗的 events**：

```bash
# Get failed events from DLQ
kubectl exec -it kafka-0 -n production -- \
  kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic ecommerce-dlq --from-beginning > failed-events.json

# Fix event handler code
# Redeploy application

# Replay events
kubectl exec -it kafka-0 -n production -- \
  kafka-console-producer --bootstrap-server localhost:9092 \
  --topic ecommerce-events < failed-events.json
```

1. **實作 idempotency**：

```java
@Component
public class OrderEventHandler {

    @TransactionalEventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        // Check if already processed
        if (processedEventRepository.existsByEventId(event.getEventId())) {
            log.info("Event already processed: {}", event.getEventId());
            return;
        }

        try {
            // Process event
            inventoryService.reserveItems(event.getOrderId());

            // Mark as processed
            processedEventRepository.save(
                new ProcessedEvent(event.getEventId(), Instant.now())
            );
        } catch (Exception e) {
            log.error("Failed to process event: {}", event.getEventId(), e);
            throw e;
        }
    }
}
```

#### 如果由 race conditions 造成

1. **實作 optimistic locking**：

```java
@Entity
public class Product {
    @Id
    private String id;

    @Version
    private Long version;

    private Integer stockQuantity;

    public void decreaseStock(int quantity) {
        if (stockQuantity < quantity) {
            throw new InsufficientStockException();
        }
        stockQuantity -= quantity;
    }
}

@Service
@Transactional
public class InventoryService {

    @Retryable(
        value = {OptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100)
    )
    public void reserveStock(String productId, int quantity) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));

        product.decreaseStock(quantity);
        productRepository.save(product);
    }
}
```

1. **使用 distributed locks** 處理關鍵區段：

```java
@Service
public class OrderService {

    private final RedissonClient redissonClient;

    public void processOrder(String orderId) {
        RLock lock = redissonClient.getLock("order:" + orderId);

        try {
            // Wait up to 10 seconds, lock for up to 30 seconds
            if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
                try {
                    // Process order with exclusive lock
                    Order order = orderRepository.findById(orderId).orElseThrow();
                    order.process();
                    orderRepository.save(order);
                } finally {
                    lock.unlock();
                }
            } else {
                throw new LockAcquisitionException("Could not acquire lock for order: " + orderId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OrderProcessingException("Interrupted while acquiring lock", e);
        }
    }
}
```

#### 如果由 cache staleness 造成

1. **清除過時的 cache**：

```bash
# Clear specific cache entries
kubectl exec -it redis-0 -n production -- redis-cli DEL customer:12345

# Clear entire cache (use with caution)
kubectl exec -it redis-0 -n production -- redis-cli FLUSHDB
```

1. **實作 cache invalidation**：

```java
@Service
public class CustomerService {

    @CacheEvict(value = "customers", key = "#customer.id")
    public Customer updateCustomer(Customer customer) {
        Customer updated = customerRepository.save(customer);

        // Publish event for other services
        eventPublisher.publish(new CustomerUpdatedEvent(updated.getId()));

        return updated;
    }

    @EventListener
    @CacheEvict(value = "customers", key = "#event.customerId")
    public void handleCustomerUpdated(CustomerUpdatedEvent event) {
        // Cache automatically evicted
    }
}
```

#### 如果由 database 損毀造成

1. **執行資料 reconciliation**：

```sql
-- Fix orphaned orders
DELETE FROM orders
WHERE customer_id NOT IN (SELECT id FROM customers);

-- Fix duplicate emails (keep oldest)
DELETE FROM customers c1
USING customers c2
WHERE c1.email = c2.email
  AND c1.created_at > c2.created_at;

-- Recalculate order totals
UPDATE orders o
SET total_amount = (
    SELECT SUM(oi.quantity * oi.unit_price)
    FROM order_items oi
    WHERE oi.order_id = o.id
)
WHERE EXISTS (
    SELECT 1 FROM order_items oi WHERE oi.order_id = o.id
);

-- Fix inventory counts
UPDATE products p
SET stock_quantity = (
    SELECT COALESCE(SUM(i.quantity), 0)
    FROM inventory i
    WHERE i.product_id = p.id
);
```

1. **建立 reconciliation job**：

```java
@Component
public class DataReconciliationJob {

    @Scheduled(cron = "0 0 2 * * *")  // Run at 2 AM daily
    public void reconcileData() {
        log.info("Starting data reconciliation");

        // Reconcile orders
        reconcileOrders();

        // Reconcile inventory
        reconcileInventory();

        // Reconcile payments
        reconcilePayments();

        log.info("Data reconciliation completed");
    }

    private void reconcileOrders() {
        List<Order> orders = orderRepository.findAll();
        for (Order order : orders) {
            BigDecimal calculatedTotal = order.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (!order.getTotalAmount().equals(calculatedTotal)) {
                log.warn("Order total mismatch: orderId={}, stored={}, calculated={}",
                    order.getId(), order.getTotalAmount(), calculatedTotal);

                order.setTotalAmount(calculatedTotal);
                orderRepository.save(order);
            }
        }
    }
}
```

### 資料恢復

如果發生資料遺失：

1. **從 backup restore**：

```bash
# See backup-restore.md for detailed procedures
./scripts/restore-from-backup.sh --date=2025-10-25 --time=10:00
```

1. **從 event store 重播 events**：

```java
@Service
public class EventReplayService {

    public void replayEvents(String aggregateId, LocalDateTime from, LocalDateTime to) {
        List<DomainEvent> events = eventStore.getEvents(aggregateId, from, to);

        for (DomainEvent event : events) {
            try {
                eventPublisher.publish(event);
                log.info("Replayed event: {}", event.getEventId());
            } catch (Exception e) {
                log.error("Failed to replay event: {}", event.getEventId(), e);
            }
        }
    }
}
```

## Verification

- [ ] 資料完整性檢查通過
- [ ] 無 orphaned records
- [ ] 無重複記錄
- [ ] 計算值正確
- [ ] Foreign key constraints 完整
- [ ] Cache 與 database 一致
- [ ] Event 處理正常運作
- [ ] 無 data inconsistency alerts

### 驗證 Queries

```sql
-- Verify no orphaned orders
SELECT COUNT(*) FROM orders o
LEFT JOIN customers c ON o.customer_id = c.id
WHERE c.id IS NULL;
-- Expected: 0

-- Verify no duplicate emails
SELECT email, COUNT(*) FROM customers
GROUP BY email HAVING COUNT(*) > 1;
-- Expected: 0 rows

-- Verify order totals
SELECT COUNT(*) FROM orders o
WHERE o.total_amount != (
    SELECT SUM(oi.quantity * oi.unit_price)
    FROM order_items oi WHERE oi.order_id = o.id
);
-- Expected: 0

-- Verify inventory consistency
SELECT COUNT(*) FROM products p
WHERE p.stock_quantity != (
    SELECT COALESCE(SUM(i.quantity), 0)
    FROM inventory i WHERE i.product_id = p.id
);
-- Expected: 0
```

## Prevention

### 1. 實作資料驗證

```java
@Entity
public class Order {

    @PrePersist
    @PreUpdate
    public void validate() {
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidOrderException("Total amount must be positive");
        }

        BigDecimal calculatedTotal = items.stream()
            .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (!totalAmount.equals(calculatedTotal)) {
            throw new InvalidOrderException("Total amount does not match items");
        }
    }
}
```

### 2. 實作 Idempotency

```java
@Component
public class IdempotentEventHandler {

    private final ProcessedEventRepository processedEventRepository;

    @TransactionalEventListener
    public void handleEvent(DomainEvent event) {
        // Check if already processed
        if (isProcessed(event.getEventId())) {
            return;
        }

        // Process event
        processEvent(event);

        // Mark as processed
        markAsProcessed(event.getEventId());
    }
}
```

### 3. 定期資料稽核

```bash
# Schedule daily data integrity checks
./scripts/data-integrity-check.sh

# Generate data quality report
./scripts/generate-data-quality-report.sh
```

### 4. Monitoring 和 Alerting

```yaml
# Set up data quality alerts

- alert: DataInconsistencyDetected

  expr: data_integrity_check_failures > 0
  for: 5m

- alert: HighEventProcessingLag

  expr: kafka_consumer_lag > 10000
  for: 10m
```

## Escalation

- **L1 Support**：DevOps team（立即回應）
- **L2 Support**：Backend engineering team（資料修復）
- **L3 Support**：Database administrator（複雜 queries）
- **Data Team**：負責資料分析和恢復

## 相關

- [Backup and Restore](backup-restore.md)
- [Database Connection Issues](database-connection-issues.md)
- [Failed Deployment](failed-deployment.md)

---

**Last Updated**：2025-10-25
**Owner**：DevOps Team
**Review Cycle**：Monthly
