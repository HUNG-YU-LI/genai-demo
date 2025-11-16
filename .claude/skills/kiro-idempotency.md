# Kiro 冪等性技巧 (Kiro Idempotency Skill)

## 描述

在程式碼生成中強制執行冪等性模式，以確保一致且可重複的結果。基於 Amazon Kiro 針對無伺服器架構的冪等性原則。

## 何時使用

- 生成需要處理重複請求的 API 處理器
- 建立必須可安全重試的資料庫操作
- 實作事件驅動工作流程
- 撰寫與外部服務互動的程式碼

## 核心原則

### 1. 冪等操作

每個生成的函數在使用相同輸入多次呼叫時，應該產生相同的結果。

### 2. 冪等鍵模式

```java
// 在請求處理中始終包含冪等鍵
public class OrderService {

    @Transactional
    public Order createOrder(CreateOrderRequest request, String idempotencyKey) {
        // 檢查是否已處理
        Optional<IdempotencyRecord> existing =
            idempotencyRepository.findByKey(idempotencyKey);

        if (existing.isPresent()) {
            return existing.get().getResult();
        }

        // 處理請求
        Order order = processOrder(request);

        // 儲存冪等記錄
        idempotencyRepository.save(new IdempotencyRecord(
            idempotencyKey,
            order.getId(),
            LocalDateTime.now()
        ));

        return order;
    }
}
```

### 3. 不可變狀態

```java
// 使用不可變物件和記錄
public record CreateOrderRequest(
    CustomerId customerId,
    List<OrderItem> items,
    ShippingAddress shippingAddress
) {
    // 沒有 setters - 完全不可變

    public CreateOrderRequest withCustomerId(CustomerId newCustomerId) {
        return new CreateOrderRequest(newCustomerId, items, shippingAddress);
    }
}
```

## 程式碼生成規則

生成程式碼時，始終應用以下模式：

1. **API 處理器**：包含冪等鍵驗證
2. **資料庫操作**：使用樂觀鎖定 + 冪等性追蹤
3. **外部 API 呼叫**：實作具有冪等性的重試機制
4. **事件處理器**：基於事件 ID 去重

## 範例

### REST API 處理器

```java
@PostMapping("/orders")
public ResponseEntity<Order> createOrder(
    @RequestBody CreateOrderRequest request,
    @RequestHeader("Idempotency-Key") String idempotencyKey
) {
    // 冪等鍵是必需的
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
        throw new MissingIdempotencyKeyException();
    }

    Order order = orderService.createOrder(request, idempotencyKey);
    return ResponseEntity.status(HttpStatus.CREATED).body(order);
}
```

### 事件處理器

```java
@KafkaListener(topics = "order-events")
public void handleOrderEvent(OrderEvent event) {
    String eventId = event.getEventId().toString();

    // 檢查事件是否已處理
    if (processedEventRepository.existsById(eventId)) {
        log.info("Event {} already processed, skipping", eventId);
        return;
    }

    // 處理事件
    processEvent(event);

    // 標記為已處理
    processedEventRepository.save(new ProcessedEvent(eventId, LocalDateTime.now()));
}
```

## 應避免的反模式

❌ **不要**：僅依賴時間戳記或計數器

```java
// 錯誤：不是真正的冪等
public void processOrder(Order order) {
    order.setProcessedAt(LocalDateTime.now()); // 每次都不同！
}
```

✅ **要**：使用確定性值

```java
// 正確：相同輸入產生相同輸出
public ProcessedOrder processOrder(Order order) {
    return ProcessedOrder.from(order, calculateChecksum(order));
}
```

## 對 Claude Code 的好處

1. **一致性**：生成的程式碼遵循可預測的模式
2. **安全性**：預設為重試安全
3. **可靠性**：優雅地處理網路故障
4. **可審計性**：清晰的冪等性追蹤

## Claude 提示詞範例

要求 Claude 使用此技巧生成程式碼時：

```
遵循 Kiro 冪等性模式生成訂單建立 API 端點。
包含冪等鍵處理和重複檢測。
```

```
建立一個使用事件 ID 追蹤完全冪等的 Kafka 事件處理器。
```

## 驗證檢查清單

在完成程式碼生成前，驗證：

- [ ] 在 API 處理器中驗證了冪等鍵
- [ ] 實作了重複檢測
- [ ] 請求使用了不可變物件
- [ ] 重試邏輯是冪等的
- [ ] 追蹤了狀態變更
