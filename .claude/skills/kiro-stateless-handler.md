# Kiro 無狀態處理器技巧 (Kiro Stateless Handler Skill)

## 描述

確保所有處理器和服務都是無狀態的，狀態外部化到資料庫或快取。基於 AWS Lambda 的無狀態執行模型。

## 何時使用

- 建立 API 處理器
- 實作事件處理器
- 設計服務類別
- 建構可擴展的微服務

## 核心原則

### 1. 無實例狀態

絕不在類別欄位中儲存狀態（注入的依賴項除外）。

### 2. 純函數

給定相同的輸入，始終產生相同的輸出。

### 3. 外部狀態儲存

所有狀態都必須儲存在外部系統（資料庫、快取、訊息佇列）中。

## 無狀態處理器模式

### ❌ 反模式：有狀態處理器

```java
@RestController
public class OrderController {

    // 錯誤：實例狀態
    private Map<String, Order> orderCache = new HashMap<>();
    private int requestCount = 0;

    @PostMapping("/orders")
    public Order createOrder(@RequestBody OrderRequest request) {
        requestCount++; // 錯誤：有狀態計數器

        Order order = new Order(request);
        orderCache.put(order.getId(), order); // 錯誤：記憶體內快取

        return order;
    }
}
```

### ✅ 最佳實踐：無狀態處理器

```java
@RestController
@RequiredArgsConstructor
public class OrderController {

    // 正確：只有注入的依賴項（無狀態服務）
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final CacheManager cacheManager;

    @PostMapping("/orders")
    public ResponseEntity<Order> createOrder(@RequestBody OrderRequest request) {
        // 所有狀態都作為參數傳遞或儲存在外部
        Order order = orderService.createOrder(request);

        // 儲存到外部資料庫
        orderRepository.save(order);

        // 儲存到外部快取
        cacheManager.put("order:" + order.getId(), order);

        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable String id) {
        // 從外部儲存檢索
        return orderRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
```

## 服務層無狀態模式

```java
@Service
@RequiredArgsConstructor
public class OrderProcessingService {

    // 正確：只有依賴項，沒有可變狀態
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final PaymentGateway paymentGateway;
    private final EventPublisher eventPublisher;

    /**
     * 無狀態方法：所有輸入作為參數，所有輸出作為返回值
     */
    public ProcessingResult processOrder(OrderId orderId, ProcessingContext context) {
        // 1. 從外部儲存載入狀態
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        // 2. 執行無狀態處理
        InventoryCheckResult inventoryCheck = inventoryService.checkAvailability(order.getItems());

        if (!inventoryCheck.isAvailable()) {
            return ProcessingResult.failure("Insufficient inventory");
        }

        // 3. 透過外部系統執行副作用
        PaymentResult payment = paymentGateway.charge(order.getTotalAmount(), context.getPaymentMethod());

        if (!payment.isSuccessful()) {
            return ProcessingResult.failure("Payment failed");
        }

        // 4. 更新外部儲存中的狀態
        Order updatedOrder = order.markAsPaid(payment.getTransactionId());
        orderRepository.save(updatedOrder);

        // 5. 發布事件到外部訊息佇列
        eventPublisher.publish(new OrderPaidEvent(orderId, payment.getTransactionId()));

        // 6. 返回結果（不儲存在實例中）
        return ProcessingResult.success(updatedOrder);
    }
}
```

## 請求範圍狀態模式

對於需要在請求期間存活的狀態，使用上下文物件：

```java
public record RequestContext(
    String requestId,
    String userId,
    LocalDateTime requestTime,
    Map<String, String> metadata
) {
    // 不可變上下文在呼叫鏈中傳遞

    public static RequestContext fromHttpRequest(HttpServletRequest request) {
        return new RequestContext(
            UUID.randomUUID().toString(),
            extractUserId(request),
            LocalDateTime.now(),
            extractMetadata(request)
        );
    }
}

@RestController
public class OrderController {

    @PostMapping("/orders")
    public ResponseEntity<Order> createOrder(
        @RequestBody OrderRequest request,
        HttpServletRequest httpRequest
    ) {
        // 建立請求範圍上下文
        RequestContext context = RequestContext.fromHttpRequest(httpRequest);

        // 在呼叫鏈中傳遞上下文
        Order order = orderService.createOrder(request, context);

        return ResponseEntity.ok(order);
    }
}
```

## 外部狀態儲存模式

### 1. 資料庫狀態

```java
@Service
public class CustomerService {

    private final CustomerRepository repository;

    public Customer updateCustomer(CustomerId id, CustomerUpdate update) {
        // 從資料庫載入
        Customer customer = repository.findById(id)
            .orElseThrow(() -> new CustomerNotFoundException(id));

        // 轉換（無狀態）
        Customer updated = customer.applyUpdate(update);

        // 儲存到資料庫
        return repository.save(updated);
    }
}
```

### 2. 快取狀態

```java
@Service
public class ProductService {

    private final ProductRepository repository;
    private final CacheManager cache;

    public Product getProduct(ProductId id) {
        // 先嘗試快取
        String cacheKey = "product:" + id.getValue();
        Product cached = cache.get(cacheKey, Product.class);

        if (cached != null) {
            return cached;
        }

        // 從資料庫載入
        Product product = repository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));

        // 儲存到快取
        cache.put(cacheKey, product, Duration.ofMinutes(10));

        return product;
    }
}
```

### 3. 會話狀態（外部會話儲存）

```java
@Service
public class SessionService {

    private final RedisTemplate<String, UserSession> sessionStore;

    public UserSession getSession(String sessionId) {
        // 從 Redis 檢索
        return sessionStore.opsForValue().get("session:" + sessionId);
    }

    public void updateSession(String sessionId, UserSession session) {
        // 儲存到 Redis 並設定 TTL
        sessionStore.opsForValue().set(
            "session:" + sessionId,
            session,
            Duration.ofHours(24)
        );
    }
}
```

## 並行安全

無狀態處理器本質上是執行緒安全的：

```java
@Service
public class ConcurrentOrderService {

    // 執行緒安全：沒有可變狀態
    private final OrderRepository repository;

    // 此方法可以從多個執行緒同時呼叫
    public Order processOrder(OrderRequest request) {
        // 每次呼叫都是獨立的
        Order order = Order.from(request);

        // 外部儲存處理並行性
        return repository.save(order);
    }
}
```

## 對 Claude Code 的好處

1. **可擴展性**：處理器可以無需協調地複製
2. **簡單性**：不需要管理實例生命週期
3. **可測試性**：易於使用模擬依賴項進行測試
4. **可靠性**：請求之間沒有狀態損壞
5. **雲原生**：完美適配容器/無伺服器模型

## Claude 提示詞範例

```
生成一個用於使用者註冊的無狀態 REST API 處理器。
所有狀態必須儲存在外部資料庫和快取中。
除了注入的依賴項外，不得有實例變數。
```

```
建立一個處理訂單建立事件的無狀態事件處理器。
使用外部 Redis 進行冪等性追蹤，使用 PostgreSQL 進行持久化。
```

## 驗證檢查清單

- [ ] 沒有可變實例欄位（注入的依賴項除外）
- [ ] 所有狀態都作為參數傳遞或儲存在外部
- [ ] 方法是純粹的，或透過外部系統具有明確的副作用
- [ ] 沒有靜態可變狀態
- [ ] 設計上是執行緒安全的
- [ ] 請求範圍狀態使用上下文物件
- [ ] 外部儲存用於持久化
