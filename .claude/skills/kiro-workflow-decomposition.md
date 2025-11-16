# Kiro Workflow Decomposition Skill

## Description
Breaks down complex workflows into small, single-responsibility, composable units. Based on Amazon Kiro's Step Functions orchestration patterns.

## When to Use
- Designing complex business workflows
- Breaking down large classes or functions
- Creating microservices boundaries
- Implementing saga patterns

## Core Principles

### 1. Single Responsibility
Each workflow step does exactly one thing.

### 2. Composability
Steps can be combined in different orders to create different workflows.

### 3. Testability
Each step can be tested independently.

## Workflow Pattern

```java
// Bad: Monolithic workflow
public class OrderProcessor {
    public void processOrder(Order order) {
        // 300 lines of mixed responsibilities
        validateOrder(order);
        checkInventory(order);
        reserveInventory(order);
        calculatePricing(order);
        applyDiscounts(order);
        processPayment(order);
        updateInventory(order);
        sendNotification(order);
        // ... more logic
    }
}

// Good: Decomposed workflow
public interface WorkflowStep<I, O> {
    O execute(I input);
    void compensate(I input); // For saga pattern
}

@Component
public class ValidateOrderStep implements WorkflowStep<Order, ValidatedOrder> {
    @Override
    public ValidatedOrder execute(Order order) {
        // Only validation logic
        return validator.validate(order);
    }
}

@Component
public class ReserveInventoryStep implements WorkflowStep<ValidatedOrder, InventoryReservation> {
    @Override
    public InventoryReservation execute(ValidatedOrder order) {
        return inventoryService.reserve(order.getItems());
    }

    @Override
    public void compensate(ValidatedOrder order) {
        inventoryService.release(order.getItems());
    }
}

@Component
public class OrderWorkflow {
    private final List<WorkflowStep<?, ?>> steps;

    public OrderResult executeWorkflow(Order order) {
        try {
            ValidatedOrder validated = validateStep.execute(order);
            InventoryReservation reservation = reserveStep.execute(validated);
            PaymentResult payment = paymentStep.execute(reservation);
            return completeStep.execute(payment);
        } catch (Exception e) {
            // Compensate in reverse order
            compensateWorkflow();
            throw e;
        }
    }
}
```

## Step Decomposition Template

Each workflow step should follow this structure:

```java
@Component
@Slf4j
public class {StepName}Step implements WorkflowStep<{InputType}, {OutputType}> {

    // Dependencies
    private final {Service} service;

    @Override
    public {OutputType} execute({InputType} input) {
        log.info("Executing {StepName} with input: {}", input);

        // 1. Validate input
        validateInput(input);

        // 2. Execute single responsibility
        {OutputType} result = performOperation(input);

        // 3. Validate output
        validateOutput(result);

        log.info("Completed {StepName} with result: {}", result);
        return result;
    }

    @Override
    public void compensate({InputType} input) {
        log.warn("Compensating {StepName} for input: {}", input);
        // Undo operation if possible
    }

    private void validateInput({InputType} input) {
        // Input validation logic
    }

    private {OutputType} performOperation({InputType} input) {
        // Core operation logic (10-20 lines max)
    }

    private void validateOutput({OutputType} output) {
        // Output validation logic
    }
}
```

## State Machine Pattern

For complex workflows, use explicit state machines:

```java
public enum OrderState {
    PENDING,
    VALIDATED,
    INVENTORY_RESERVED,
    PAYMENT_PROCESSED,
    COMPLETED,
    FAILED,
    COMPENSATED
}

public record OrderWorkflowContext(
    Order order,
    OrderState currentState,
    Map<String, Object> stepResults,
    List<String> executedSteps,
    Optional<Exception> error
) {
    public OrderWorkflowContext transitionTo(OrderState newState, String stepName, Object result) {
        Map<String, Object> updatedResults = new HashMap<>(stepResults);
        updatedResults.put(stepName, result);

        List<String> updatedSteps = new ArrayList<>(executedSteps);
        updatedSteps.add(stepName);

        return new OrderWorkflowContext(
            order,
            newState,
            updatedResults,
            updatedSteps,
            error
        );
    }
}
```

## Orchestration vs Choreography

### Orchestration (Centralized Control)
```java
@Service
public class OrderOrchestrator {

    public OrderResult orchestrateOrderCreation(Order order) {
        // Orchestrator controls the flow
        var validated = validationStep.execute(order);
        var reserved = inventoryStep.execute(validated);
        var payment = paymentStep.execute(reserved);
        return completionStep.execute(payment);
    }
}
```

### Choreography (Event-Driven)
```java
// Each step listens to events and publishes new events
@Component
public class InventoryReservationHandler {

    @EventListener
    public void onOrderValidated(OrderValidatedEvent event) {
        InventoryReservation reservation = inventoryService.reserve(event.getOrder());
        eventPublisher.publish(new InventoryReservedEvent(reservation));
    }
}

@Component
public class PaymentProcessingHandler {

    @EventListener
    public void onInventoryReserved(InventoryReservedEvent event) {
        PaymentResult payment = paymentService.process(event.getReservation());
        eventPublisher.publish(new PaymentProcessedEvent(payment));
    }
}
```

## Benefits for Claude Code

1. **Modularity**: Easy to generate, test, and modify individual steps
2. **Reusability**: Steps can be reused across different workflows
3. **Clarity**: Each step has a clear purpose and interface
4. **Error Handling**: Compensation logic is explicit and localized
5. **AI-Friendly**: Smaller units are easier for AI to reason about

## Prompts for Claude

```
Decompose the order fulfillment process into workflow steps using the Kiro pattern.
Each step should implement WorkflowStep<I,O> with execute and compensate methods.
```

```
Create a state machine for user registration workflow with the following states:
EMAIL_SENT, EMAIL_VERIFIED, PROFILE_CREATED, COMPLETED. Include compensation logic.
```

## Validation Checklist

- [ ] Each step has single responsibility (< 50 lines of logic)
- [ ] Steps are composable and reusable
- [ ] Input/output types are clearly defined
- [ ] Compensation logic is implemented for reversible operations
- [ ] State transitions are explicit
- [ ] Error boundaries are defined
- [ ] Each step is independently testable
