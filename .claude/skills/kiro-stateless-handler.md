# Kiro Stateless Handler Skill

## Description
Ensures all handlers and services are stateless, with state externalized to databases or caches. Based on AWS Lambda's stateless execution model.

## When to Use
- Creating API handlers
- Implementing event processors
- Designing service classes
- Building scalable microservices

## Core Principles

### 1. No Instance State
Never store state in class fields (except for injected dependencies).

### 2. Pure Functions
Given the same input, always produce the same output.

### 3. External State Storage
All state must be stored in external systems (DB, cache, message queue).

## Stateless Handler Pattern

### ❌ Anti-Pattern: Stateful Handler
```java
@RestController
public class OrderController {

    // BAD: Instance state
    private Map<String, Order> orderCache = new HashMap<>();
    private int requestCount = 0;

    @PostMapping("/orders")
    public Order createOrder(@RequestBody OrderRequest request) {
        requestCount++; // BAD: Stateful counter

        Order order = new Order(request);
        orderCache.put(order.getId(), order); // BAD: In-memory cache

        return order;
    }
}
```

### ✅ Best Practice: Stateless Handler
```java
@RestController
@RequiredArgsConstructor
public class OrderController {

    // GOOD: Only injected dependencies (stateless services)
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final CacheManager cacheManager;

    @PostMapping("/orders")
    public ResponseEntity<Order> createOrder(@RequestBody OrderRequest request) {
        // All state is passed as parameters or stored externally
        Order order = orderService.createOrder(request);

        // Store in external database
        orderRepository.save(order);

        // Store in external cache
        cacheManager.put("order:" + order.getId(), order);

        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable String id) {
        // Retrieve from external storage
        return orderRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
```

## Service Layer Stateless Pattern

```java
@Service
@RequiredArgsConstructor
public class OrderProcessingService {

    // GOOD: Only dependencies, no mutable state
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final PaymentGateway paymentGateway;
    private final EventPublisher eventPublisher;

    /**
     * Stateless method: All inputs as parameters, all outputs as return values
     */
    public ProcessingResult processOrder(OrderId orderId, ProcessingContext context) {
        // 1. Load state from external storage
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        // 2. Perform stateless processing
        InventoryCheckResult inventoryCheck = inventoryService.checkAvailability(order.getItems());

        if (!inventoryCheck.isAvailable()) {
            return ProcessingResult.failure("Insufficient inventory");
        }

        // 3. Execute side effects via external systems
        PaymentResult payment = paymentGateway.charge(order.getTotalAmount(), context.getPaymentMethod());

        if (!payment.isSuccessful()) {
            return ProcessingResult.failure("Payment failed");
        }

        // 4. Update state in external storage
        Order updatedOrder = order.markAsPaid(payment.getTransactionId());
        orderRepository.save(updatedOrder);

        // 5. Publish events to external message queue
        eventPublisher.publish(new OrderPaidEvent(orderId, payment.getTransactionId()));

        // 6. Return result (not stored in instance)
        return ProcessingResult.success(updatedOrder);
    }
}
```

## Request-Scoped State Pattern

For state that needs to live during a request, use context objects:

```java
public record RequestContext(
    String requestId,
    String userId,
    LocalDateTime requestTime,
    Map<String, String> metadata
) {
    // Immutable context passed through the call chain

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
        // Create request-scoped context
        RequestContext context = RequestContext.fromHttpRequest(httpRequest);

        // Pass context through the call chain
        Order order = orderService.createOrder(request, context);

        return ResponseEntity.ok(order);
    }
}
```

## External State Storage Patterns

### 1. Database State
```java
@Service
public class CustomerService {

    private final CustomerRepository repository;

    public Customer updateCustomer(CustomerId id, CustomerUpdate update) {
        // Load from DB
        Customer customer = repository.findById(id)
            .orElseThrow(() -> new CustomerNotFoundException(id));

        // Transform (stateless)
        Customer updated = customer.applyUpdate(update);

        // Save to DB
        return repository.save(updated);
    }
}
```

### 2. Cache State
```java
@Service
public class ProductService {

    private final ProductRepository repository;
    private final CacheManager cache;

    public Product getProduct(ProductId id) {
        // Try cache first
        String cacheKey = "product:" + id.getValue();
        Product cached = cache.get(cacheKey, Product.class);

        if (cached != null) {
            return cached;
        }

        // Load from DB
        Product product = repository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));

        // Store in cache
        cache.put(cacheKey, product, Duration.ofMinutes(10));

        return product;
    }
}
```

### 3. Session State (External Session Store)
```java
@Service
public class SessionService {

    private final RedisTemplate<String, UserSession> sessionStore;

    public UserSession getSession(String sessionId) {
        // Retrieve from Redis
        return sessionStore.opsForValue().get("session:" + sessionId);
    }

    public void updateSession(String sessionId, UserSession session) {
        // Store in Redis with TTL
        sessionStore.opsForValue().set(
            "session:" + sessionId,
            session,
            Duration.ofHours(24)
        );
    }
}
```

## Concurrency Safety

Stateless handlers are inherently thread-safe:

```java
@Service
public class ConcurrentOrderService {

    // Thread-safe: No mutable state
    private final OrderRepository repository;

    // This method can be called concurrently from multiple threads
    public Order processOrder(OrderRequest request) {
        // Each invocation is independent
        Order order = Order.from(request);

        // External storage handles concurrency
        return repository.save(order);
    }
}
```

## Benefits for Claude Code

1. **Scalability**: Handlers can be replicated without coordination
2. **Simplicity**: No need to manage instance lifecycle
3. **Testability**: Easy to test with mock dependencies
4. **Reliability**: No state corruption across requests
5. **Cloud-Native**: Fits perfectly with container/serverless models

## Prompts for Claude

```
Generate a stateless REST API handler for user registration.
All state must be stored in external database and cache.
No instance variables except injected dependencies.
```

```
Create a stateless event processor that handles order creation events.
Use external Redis for idempotency tracking and PostgreSQL for persistence.
```

## Validation Checklist

- [ ] No mutable instance fields (except injected dependencies)
- [ ] All state is passed as parameters or stored externally
- [ ] Methods are pure or have explicit side effects via external systems
- [ ] No static mutable state
- [ ] Thread-safe by design
- [ ] Request-scoped state uses context objects
- [ ] External storage is used for persistence
