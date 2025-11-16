# 錯誤處理模式

## 概述

我們 DDD + Hexagonal Architecture 專案中錯誤處理的全面指南。

**相關標準**: [Development Standards](../../steering/development-standards.md), [Code Quality Checklist](../../steering/code-quality-checklist.md)

---

## Exception 階層

### 自訂 Exception 設計

```java
// Base domain exception
public abstract class DomainException extends RuntimeException {
    private final String errorCode;
    private final Map<String, Object> context;

    protected DomainException(String errorCode, String message, Map<String, Object> context) {
        super(message);
        this.errorCode = errorCode;
        this.context = context != null ? context : Map.of();
    }

    public String getErrorCode() { return errorCode; }
    public Map<String, Object> getContext() { return context; }
}

// Business rule violations
public class BusinessRuleViolationException extends DomainException {
    public BusinessRuleViolationException(String rule, String message) {
        super("BUSINESS_RULE_VIOLATION", message, Map.of("rule", rule));
    }
}

// Resource not found
public class ResourceNotFoundException extends DomainException {
    public ResourceNotFoundException(String resourceType, String resourceId) {
        super("RESOURCE_NOT_FOUND",
              String.format("%s with id %s not found", resourceType, resourceId),
              Map.of("resourceType", resourceType, "resourceId", resourceId));
    }
}

// Validation errors
public class ValidationException extends DomainException {
    private final List<FieldError> fieldErrors;

    public ValidationException(String message, List<FieldError> fieldErrors) {
        super("VALIDATION_ERROR", message, Map.of("fieldCount", fieldErrors.size()));
        this.fieldErrors = fieldErrors;
    }

    public List<FieldError> getFieldErrors() { return fieldErrors; }
}

public record FieldError(String field, String message, Object rejectedValue) {}
```

---

## 全域 Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(DomainException ex) {
        logger.warn("Domain exception occurred",
            kv("errorCode", ex.getErrorCode()),
            kv("context", ex.getContext()),
            ex);

        ErrorResponse response = ErrorResponse.builder()
            .errorCode(ex.getErrorCode())
            .message(ex.getMessage())
            .context(ex.getContext())
            .timestamp(Instant.now())
            .build();

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        logger.warn("Validation failed",
            kv("fieldErrors", ex.getFieldErrors()),
            ex);

        ErrorResponse response = ErrorResponse.builder()
            .errorCode(ex.getErrorCode())
            .message(ex.getMessage())
            .fieldErrors(ex.getFieldErrors())
            .timestamp(Instant.now())
            .build();

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        logger.error("Unexpected error occurred", ex);

        ErrorResponse response = ErrorResponse.builder()
            .errorCode("INTERNAL_SERVER_ERROR")
            .message("An unexpected error occurred")
            .timestamp(Instant.now())
            .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}

@Builder
public record ErrorResponse(
    String errorCode,
    String message,
    Map<String, Object> context,
    List<FieldError> fieldErrors,
    Instant timestamp
) {}
```

---

## 驗證模式

### 輸入驗證

```java
// ✅ 好: Bean Validation
public record CreateCustomerRequest(
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    String name,

    @NotBlank(message = "Email is required")
    @Email(message = "Email format is invalid")
    String email,

    @Valid
    @NotNull(message = "Address is required")
    AddressDto address
) {}

@RestController
public class CustomerController {

    @PostMapping("/customers")
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request) {
        // Validation happens automatically
        Customer customer = customerService.createCustomer(request);
        return ResponseEntity.ok(CustomerResponse.from(customer));
    }
}
```

### 業務規則驗證

```java
// ✅ 好: Domain 驗證
@AggregateRoot
public class Order extends AggregateRoot {

    public void submit() {
        validateCanSubmit();

        this.status = OrderStatus.SUBMITTED;
        collectEvent(OrderSubmittedEvent.create(id));
    }

    private void validateCanSubmit() {
        if (items.isEmpty()) {
            throw new BusinessRuleViolationException(
                "ORDER_EMPTY",
                "Cannot submit empty order"
            );
        }

        if (status != OrderStatus.DRAFT) {
            throw new BusinessRuleViolationException(
                "ORDER_ALREADY_SUBMITTED",
                "Order has already been submitted"
            );
        }

        if (!hasValidShippingAddress()) {
            throw new BusinessRuleViolationException(
                "INVALID_SHIPPING_ADDRESS",
                "Order must have valid shipping address"
            );
        }
    }
}
```

---

## 錯誤恢復模式

### 重試與指數退避

```java
@Service
public class PaymentService {

    @Retryable(
        value = {TransientException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public PaymentResult processPayment(PaymentRequest request) {
        try {
            return paymentGateway.process(request);
        } catch (GatewayTimeoutException e) {
            throw new TransientException("Payment gateway timeout", e);
        }
    }

    @Recover
    public PaymentResult recover(TransientException ex, PaymentRequest request) {
        logger.error("Payment failed after retries",
            kv("paymentId", request.getPaymentId()),
            ex);
        return PaymentResult.failed("Payment processing failed");
    }
}
```

### Circuit Breaker

```java
@Service
public class InventoryService {

    @CircuitBreaker(name = "inventory", fallbackMethod = "getInventoryFallback")
    public InventoryStatus getInventoryStatus(ProductId productId) {
        return inventoryClient.getStatus(productId);
    }

    private InventoryStatus getInventoryFallback(ProductId productId, Exception ex) {
        logger.warn("Inventory service unavailable, using fallback",
            kv("productId", productId),
            ex);
        return InventoryStatus.unknown();
    }
}
```

---

## 日誌最佳實踐

### 結構化日誌

```java
// ✅ 好: 帶有上下文的結構化日誌
public class OrderService {
    private final Logger logger = LoggerFactory.getLogger(OrderService.class);

    public void processOrder(OrderId orderId) {
        logger.info("Processing order",
            kv("orderId", orderId),
            kv("timestamp", Instant.now()));

        try {
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

            order.submit();
            orderRepository.save(order);

            logger.info("Order processed successfully",
                kv("orderId", orderId),
                kv("total", order.getTotal()),
                kv("itemCount", order.getItems().size()));

        } catch (BusinessRuleViolationException e) {
            logger.warn("Business rule violation",
                kv("orderId", orderId),
                kv("rule", e.getErrorCode()),
                e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error processing order",
                kv("orderId", orderId),
                e);
            throw new OrderProcessingException("Failed to process order", e);
        }
    }
}
```

---

## 總結

錯誤處理的關鍵原則：

1. **使用具體的 exceptions** 帶有錯誤代碼和上下文
2. **及早驗證** 在 API 邊界和 domain 中
3. **適當地記錄** 使用結構化資料
4. **優雅地處理** 使用重試和降級
5. **絕不吞噬 exceptions** 不記錄的情況下

---

**相關文件**:
- [Development Standards](../../steering/development-standards.md)
- [Code Quality Checklist](../../steering/code-quality-checklist.md)
- [Security Standards](../../steering/security-standards.md)
