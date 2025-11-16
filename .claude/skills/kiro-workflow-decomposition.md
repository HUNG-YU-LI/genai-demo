# Kiro 工作流程分解技巧 (Kiro Workflow Decomposition Skill)

## 描述

將複雜的工作流程分解為小型、單一職責、可組合的單元。基於 Amazon Kiro 的 Step Functions 編排模式。

## 何時使用

- 設計複雜的業務工作流程
- 分解大型類別或函數
- 建立微服務邊界
- 實作 Saga 模式

## 核心原則

### 1. 單一職責

每個工作流程步驟只做一件事。

### 2. 可組合性

步驟可以以不同的順序組合，創建不同的工作流程。

### 3. 可測試性

每個步驟都可以獨立測試。

## 工作流程模式

```java
// 錯誤：單體式工作流程
public class OrderProcessor {
    public void processOrder(Order order) {
        // 300 行混合職責的程式碼
        validateOrder(order);
        checkInventory(order);
        reserveInventory(order);
        calculatePricing(order);
        applyDiscounts(order);
        processPayment(order);
        updateInventory(order);
        sendNotification(order);
        // ... 更多邏輯
    }
}

// 正確：分解的工作流程
public interface WorkflowStep<I, O> {
    O execute(I input);
    void compensate(I input); // 用於 Saga 模式
}

@Component
public class ValidateOrderStep implements WorkflowStep<Order, ValidatedOrder> {
    @Override
    public ValidatedOrder execute(Order order) {
        // 只有驗證邏輯
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
            // 反向補償
            compensateWorkflow();
            throw e;
        }
    }
}
```

## 步驟分解範本

每個工作流程步驟應遵循此結構：

```java
@Component
@Slf4j
public class {StepName}Step implements WorkflowStep<{InputType}, {OutputType}> {

    // 依賴項
    private final {Service} service;

    @Override
    public {OutputType} execute({InputType} input) {
        log.info("Executing {StepName} with input: {}", input);

        // 1. 驗證輸入
        validateInput(input);

        // 2. 執行單一職責
        {OutputType} result = performOperation(input);

        // 3. 驗證輸出
        validateOutput(result);

        log.info("Completed {StepName} with result: {}", result);
        return result;
    }

    @Override
    public void compensate({InputType} input) {
        log.warn("Compensating {StepName} for input: {}", input);
        // 如果可能，撤銷操作
    }

    private void validateInput({InputType} input) {
        // 輸入驗證邏輯
    }

    private {OutputType} performOperation({InputType} input) {
        // 核心操作邏輯（最多 10-20 行）
    }

    private void validateOutput({OutputType} output) {
        // 輸出驗證邏輯
    }
}
```

## 狀態機模式

對於複雜的工作流程，使用明確的狀態機：

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

## 編排 vs 編舞

### 編排（集中式控制）

```java
@Service
public class OrderOrchestrator {

    public OrderResult orchestrateOrderCreation(Order order) {
        // 編排器控制流程
        var validated = validationStep.execute(order);
        var reserved = inventoryStep.execute(validated);
        var payment = paymentStep.execute(reserved);
        return completionStep.execute(payment);
    }
}
```

### 編舞（事件驅動）

```java
// 每個步驟監聽事件並發布新事件
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

## 對 Claude Code 的好處

1. **模組化**：易於生成、測試和修改個別步驟
2. **可重用性**：步驟可以在不同的工作流程中重用
3. **清晰性**：每個步驟都有明確的目的和介面
4. **錯誤處理**：補償邏輯是明確且局部化的
5. **AI 友善**：較小的單元更容易讓 AI 推理

## Claude 提示詞範例

```
使用 Kiro 模式將訂單履行流程分解為工作流程步驟。
每個步驟應實作 WorkflowStep<I,O>，並包含 execute 和 compensate 方法。
```

```
為使用者註冊工作流程建立狀態機，包含以下狀態：
EMAIL_SENT、EMAIL_VERIFIED、PROFILE_CREATED、COMPLETED。包含補償邏輯。
```

## 驗證檢查清單

- [ ] 每個步驟都有單一職責（< 50 行邏輯）
- [ ] 步驟是可組合和可重用的
- [ ] 輸入/輸出類型已明確定義
- [ ] 已為可逆操作實作補償邏輯
- [ ] 狀態轉換是明確的
- [ ] 已定義錯誤邊界
- [ ] 每個步驟都可獨立測試
