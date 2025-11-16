# Kiro Boundary Control Skill

## Description
Establishes clear input validation, output formatting, and error handling boundaries for all system interfaces. Based on defensive programming and fail-fast principles.

## When to Use
- Creating API endpoints
- Implementing event handlers
- Designing service interfaces
- Building system boundaries (controllers, adapters)

## Core Principles

### 1. Validate Early
Validate all inputs at the boundary before processing.

### 2. Fail Fast
Reject invalid inputs immediately with clear error messages.

### 3. Explicit Contracts
Define clear input/output contracts using types and validation.

### 4. Error Boundaries
Handle errors at appropriate boundaries, don't leak implementation details.

## API Boundary Pattern

```java
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {

    private final OrderService orderService;
    private final OrderMapper orderMapper;

    /**
     * Create order endpoint with comprehensive boundary control
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
        @Valid @RequestBody CreateOrderRequest request,
        @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
        @RequestHeader(value = "X-Request-ID", required = false) String requestId
    ) {
        // 1. BOUNDARY: Input validation (already done by @Valid)

        // 2. BOUNDARY: Additional business validation
        validateBusinessRules(request);

        // 3. BOUNDARY: Convert DTO to domain model
        CreateOrderCommand command = orderMapper.toCommand(request);

        // 4. Execute business logic (trusting validated input)
        Order order = orderService.createOrder(command, idempotencyKey);

        // 5. BOUNDARY: Convert domain model to DTO
        OrderResponse response = orderMapper.toResponse(order);

        // 6. BOUNDARY: Set appropriate HTTP status and headers
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .header("Location", "/api/v1/orders/" + order.getId())
            .header("X-Request-ID", requestId)
            .body(response);
    }

    /**
     * Business rule validation at boundary
     */
    private void validateBusinessRules(CreateOrderRequest request) {
        // Example: Business hours check
        if (!isWithinBusinessHours()) {
            throw new BusinessRuleViolationException(
                "Orders can only be placed during business hours (9 AM - 5 PM)"
            );
        }

        // Example: Order value limits
        BigDecimal totalValue = calculateTotalValue(request.getItems());
        if (totalValue.compareTo(new BigDecimal("10000")) > 0) {
            throw new BusinessRuleViolationException(
                "Order value cannot exceed $10,000. Please contact sales for large orders."
            );
        }
    }
}
```

## Input Validation Pattern

```java
/**
 * Request DTO with comprehensive validation
 */
public record CreateOrderRequest(
    @NotNull(message = "Customer ID is required")
    @Pattern(regexp = "^CUST-[0-9]{8}$", message = "Invalid customer ID format")
    String customerId,

    @NotEmpty(message = "Order must have at least one item")
    @Size(max = 100, message = "Cannot order more than 100 items at once")
    @Valid
    List<OrderItemRequest> items,

    @NotNull(message = "Shipping address is required")
    @Valid
    ShippingAddressRequest shippingAddress,

    @Pattern(regexp = "^(STANDARD|EXPRESS|OVERNIGHT)$", message = "Invalid shipping method")
    String shippingMethod
) {
    // Additional validation in compact constructor
    public CreateOrderRequest {
        // Defensive copy
        items = List.copyOf(items);

        // Cross-field validation
        if (shippingMethod.equals("OVERNIGHT") && items.size() > 10) {
            throw new IllegalArgumentException(
                "Overnight shipping is not available for orders with more than 10 items"
            );
        }
    }
}

/**
 * Nested validation
 */
public record OrderItemRequest(
    @NotBlank(message = "Product ID is required")
    String productId,

    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 100, message = "Quantity cannot exceed 100")
    int quantity,

    @DecimalMin(value = "0.01", message = "Unit price must be positive")
    @Digits(integer = 10, fraction = 2, message = "Invalid price format")
    BigDecimal unitPrice
) {}
```

## Output Formatting Pattern

```java
/**
 * Standardized API response wrapper
 */
public record ApiResponse<T>(
    boolean success,
    T data,
    ErrorDetails error,
    ResponseMetadata metadata
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
            true,
            data,
            null,
            new ResponseMetadata(
                LocalDateTime.now(),
                UUID.randomUUID().toString()
            )
        );
    }

    public static <T> ApiResponse<T> error(ErrorDetails error) {
        return new ApiResponse<>(
            false,
            null,
            error,
            new ResponseMetadata(
                LocalDateTime.now(),
                UUID.randomUUID().toString()
            )
        );
    }
}

public record ErrorDetails(
    String code,
    String message,
    Map<String, List<String>> fieldErrors,
    String timestamp
) {
    public static ErrorDetails from(Exception exception) {
        return new ErrorDetails(
            "INTERNAL_ERROR",
            "An unexpected error occurred",
            Map.of(),
            LocalDateTime.now().toString()
        );
    }

    public static ErrorDetails validationError(Map<String, List<String>> fieldErrors) {
        return new ErrorDetails(
            "VALIDATION_ERROR",
            "Request validation failed",
            fieldErrors,
            LocalDateTime.now().toString()
        );
    }
}
```

## Error Boundary Pattern

```java
/**
 * Global exception handler - Error boundary for API layer
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, List<String>> fieldErrors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
            fieldErrors.computeIfAbsent(error.getField(), k -> new ArrayList<>())
                      .add(error.getDefaultMessage())
        );

        log.warn("Validation error: {}", fieldErrors);

        return ApiResponse.error(ErrorDetails.validationError(fieldErrors));
    }

    /**
     * Handle business rule violations
     */
    @ExceptionHandler(BusinessRuleViolationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiResponse<?> handleBusinessRuleViolation(BusinessRuleViolationException ex) {
        log.warn("Business rule violation: {}", ex.getMessage());

        return ApiResponse.error(new ErrorDetails(
            "BUSINESS_RULE_VIOLATION",
            ex.getMessage(),
            Map.of(),
            LocalDateTime.now().toString()
        ));
    }

    /**
     * Handle not found errors
     */
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<?> handleNotFound(EntityNotFoundException ex) {
        log.info("Entity not found: {}", ex.getMessage());

        return ApiResponse.error(new ErrorDetails(
            "NOT_FOUND",
            ex.getMessage(),
            Map.of(),
            LocalDateTime.now().toString()
        ));
    }

    /**
     * Handle unexpected errors (DON'T leak internal details)
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<?> handleUnexpectedError(Exception ex) {
        // Log full details internally
        log.error("Unexpected error", ex);

        // Return sanitized error to client
        return ApiResponse.error(new ErrorDetails(
            "INTERNAL_ERROR",
            "An unexpected error occurred. Please try again later.",
            Map.of(),
            LocalDateTime.now().toString()
        ));
    }
}
```

## Event Handler Boundary Pattern

```java
@Component
@Slf4j
public class OrderEventHandler {

    private final OrderService orderService;

    @KafkaListener(topics = "order-events", groupId = "order-processor")
    public void handleOrderEvent(
        @Payload @Valid OrderEventMessage message,
        @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String messageKey,
        @Header(KafkaHeaders.RECEIVED_TIMESTAMP) long timestamp
    ) {
        // 1. BOUNDARY: Log incoming event
        log.info("Received order event: key={}, timestamp={}, message={}",
            messageKey, timestamp, message);

        try {
            // 2. BOUNDARY: Validate event structure
            validateEvent(message);

            // 3. BOUNDARY: Check idempotency
            if (isAlreadyProcessed(message.getEventId())) {
                log.info("Event {} already processed, skipping", message.getEventId());
                return;
            }

            // 4. Process event (trusting validated input)
            processEvent(message);

            // 5. BOUNDARY: Mark as processed
            markAsProcessed(message.getEventId());

            // 6. BOUNDARY: Log success
            log.info("Successfully processed event {}", message.getEventId());

        } catch (ValidationException ex) {
            // 7. BOUNDARY: Handle validation errors
            log.error("Invalid event format: {}", ex.getMessage());
            sendToDeadLetterQueue(message, "VALIDATION_ERROR", ex.getMessage());

        } catch (BusinessException ex) {
            // 8. BOUNDARY: Handle business errors
            log.error("Business error processing event: {}", ex.getMessage());
            sendToDeadLetterQueue(message, "BUSINESS_ERROR", ex.getMessage());

        } catch (Exception ex) {
            // 9. BOUNDARY: Handle unexpected errors
            log.error("Unexpected error processing event", ex);
            sendToRetryQueue(message);
        }
    }

    private void validateEvent(OrderEventMessage message) {
        if (message.getEventType() == null || message.getEventType().isBlank()) {
            throw new ValidationException("Event type is required");
        }

        if (message.getPayload() == null) {
            throw new ValidationException("Event payload is required");
        }
    }
}
```

## Service Interface Boundary Pattern

```java
/**
 * Service interface with explicit contracts
 */
public interface OrderService {

    /**
     * Creates a new order
     *
     * @param command Validated order creation command
     * @param idempotencyKey Unique key for idempotency
     * @return Created order
     * @throws IllegalArgumentException if command is invalid
     * @throws BusinessRuleViolationException if business rules are violated
     * @throws DuplicateOrderException if idempotency key already used
     */
    Order createOrder(
        @Valid CreateOrderCommand command,
        @NotBlank String idempotencyKey
    ) throws IllegalArgumentException, BusinessRuleViolationException;

    /**
     * Retrieves order by ID
     *
     * @param orderId Order identifier
     * @return Order if found
     * @throws OrderNotFoundException if order doesn't exist
     */
    Order getOrder(@NotNull OrderId orderId) throws OrderNotFoundException;
}

@Service
@RequiredArgsConstructor
@Validated
public class OrderServiceImpl implements OrderService {

    @Override
    public Order createOrder(CreateOrderCommand command, String idempotencyKey) {
        // BOUNDARY: Validate preconditions
        Objects.requireNonNull(command, "Command cannot be null");
        Objects.requireNonNull(idempotencyKey, "Idempotency key cannot be null");

        // BOUNDARY: Check business rules
        if (!canCreateOrder(command)) {
            throw new BusinessRuleViolationException("Cannot create order");
        }

        // Execute business logic (inputs are trusted)
        Order order = executeOrderCreation(command);

        // BOUNDARY: Validate postconditions
        Objects.requireNonNull(order, "Order creation failed");
        assert order.getId() != null : "Order must have an ID";

        return order;
    }
}
```

## Benefits for Claude Code

1. **Robustness**: Early validation prevents invalid states
2. **Clarity**: Explicit boundaries make data flow clear
3. **Debuggability**: Easy to identify where errors occur
4. **Security**: Prevent injection and malformed data
5. **AI-Friendly**: Clear contracts help AI generate correct code

## Prompts for Claude

```
Generate a REST controller with complete boundary control including
input validation, error handling, and standardized response format.
```

```
Create a Kafka event handler with idempotency checking, validation,
and dead letter queue for failed events.
```

## Validation Checklist

- [ ] All inputs validated at entry points
- [ ] Validation annotations on request DTOs
- [ ] Business rule validation separated from technical validation
- [ ] Standardized error responses
- [ ] Error boundaries (exception handlers) in place
- [ ] No internal error details leaked to clients
- [ ] Idempotency keys validated for APIs
- [ ] Output DTOs hide internal implementation
