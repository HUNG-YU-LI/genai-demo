# Kiro Idempotency Skill

## Description
Enforces idempotency patterns in code generation to ensure consistent, repeatable results. Based on Amazon Kiro's idempotency principles for serverless architectures.

## When to Use
- Generating API handlers that need to handle duplicate requests
- Creating database operations that must be safely retryable
- Implementing event-driven workflows
- Writing code that interacts with external services

## Core Principles

### 1. Idempotent Operations
Every generated function should produce the same result when called multiple times with the same input.

### 2. Idempotency Key Pattern
```java
// Always include idempotency key in request handling
public class OrderService {

    @Transactional
    public Order createOrder(CreateOrderRequest request, String idempotencyKey) {
        // Check if already processed
        Optional<IdempotencyRecord> existing =
            idempotencyRepository.findByKey(idempotencyKey);

        if (existing.isPresent()) {
            return existing.get().getResult();
        }

        // Process request
        Order order = processOrder(request);

        // Store idempotency record
        idempotencyRepository.save(new IdempotencyRecord(
            idempotencyKey,
            order.getId(),
            LocalDateTime.now()
        ));

        return order;
    }
}
```

### 3. Immutable State
```java
// Use immutable objects and records
public record CreateOrderRequest(
    CustomerId customerId,
    List<OrderItem> items,
    ShippingAddress shippingAddress
) {
    // No setters - completely immutable

    public CreateOrderRequest withCustomerId(CustomerId newCustomerId) {
        return new CreateOrderRequest(newCustomerId, items, shippingAddress);
    }
}
```

## Code Generation Rules

When generating code, always apply these patterns:

1. **API Handlers**: Include idempotency key validation
2. **Database Operations**: Use optimistic locking + idempotency tracking
3. **External API Calls**: Implement retry with idempotency
4. **Event Handlers**: De-duplicate based on event ID

## Examples

### REST API Handler
```java
@PostMapping("/orders")
public ResponseEntity<Order> createOrder(
    @RequestBody CreateOrderRequest request,
    @RequestHeader("Idempotency-Key") String idempotencyKey
) {
    // Idempotency key is required
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
        throw new MissingIdempotencyKeyException();
    }

    Order order = orderService.createOrder(request, idempotencyKey);
    return ResponseEntity.status(HttpStatus.CREATED).body(order);
}
```

### Event Handler
```java
@KafkaListener(topics = "order-events")
public void handleOrderEvent(OrderEvent event) {
    String eventId = event.getEventId().toString();

    // Check if event already processed
    if (processedEventRepository.existsById(eventId)) {
        log.info("Event {} already processed, skipping", eventId);
        return;
    }

    // Process event
    processEvent(event);

    // Mark as processed
    processedEventRepository.save(new ProcessedEvent(eventId, LocalDateTime.now()));
}
```

## Anti-Patterns to Avoid

❌ **Don't**: Rely on timestamps or counters alone
```java
// BAD: Not truly idempotent
public void processOrder(Order order) {
    order.setProcessedAt(LocalDateTime.now()); // Different each time!
}
```

✅ **Do**: Use deterministic values
```java
// GOOD: Same input produces same output
public ProcessedOrder processOrder(Order order) {
    return ProcessedOrder.from(order, calculateChecksum(order));
}
```

## Benefits for Claude Code

1. **Consistency**: Generated code follows predictable patterns
2. **Safety**: Retry-safe by default
3. **Reliability**: Handles network failures gracefully
4. **Auditability**: Clear idempotency tracking

## Prompts for Claude

When asking Claude to generate code with this skill:

```
Generate an order creation API endpoint following the Kiro idempotency pattern.
Include idempotency key handling and duplicate detection.
```

```
Create a Kafka event handler that is fully idempotent using event ID tracking.
```

## Validation Checklist

Before completing code generation, verify:
- [ ] Idempotency key is validated in API handlers
- [ ] Duplicate detection is implemented
- [ ] Immutable objects are used for requests
- [ ] Retry logic is idempotent
- [ ] State changes are tracked
