# Dependency Injection - 詳細範例

## 原則概述

**Dependency Injection (DI)** 是一種設計模式，實作控制反轉（Inversion of Control，IoC）來解析依賴關係。物件不自己建立依賴，而是從外部提供（注入）依賴。

**主要優點：**
- 元件之間的鬆耦合
- 使用 mock 物件更容易測試
- 更好的程式碼可重用性
- 更清楚的依賴關係和契約

---

## 範例 1：訂單處理服務

### ❌ 錯誤：硬編碼依賴

```java
public class OrderService {
    // 直接建立依賴 - 緊密耦合！
    private final OrderValidator validator = new OrderValidator();
    private final PriceCalculator calculator = new PriceCalculator();
    private final PaymentProcessor paymentProcessor = new CreditCardPaymentProcessor();
    private final EmailService emailService = new SmtpEmailService();

    public OrderResult processOrder(OrderRequest request) {
        // 無法使用 mocks 測試
        // 無法變更實作
        // 難以配置
        return null;
    }
}
```

### ✅ 正確：建構子注入

```java
@Service
public class OrderService {
    private final OrderValidator validator;
    private final PriceCalculator calculator;
    private final PaymentProcessor paymentProcessor;
    private final EmailService emailService;

    // 透過建構子注入依賴
    public OrderService(
        OrderValidator validator,
        PriceCalculator calculator,
        PaymentProcessor paymentProcessor,
        EmailService emailService
    ) {
        this.validator = validator;
        this.calculator = calculator;
        this.paymentProcessor = paymentProcessor;
        this.emailService = emailService;
    }

    public OrderResult processOrder(OrderRequest request) {
        // 容易使用 mocks 測試
        // 可以交換實作
        // 配置由外部處理
        validator.validate(request);
        Money total = calculator.calculateTotal(request);
        PaymentResult payment = paymentProcessor.process(total);
        emailService.sendConfirmation(request.getCustomerEmail());
        return OrderResult.success();
    }
}
```

---

## 範例 2：使用 Dependency Injection 進行測試

### 使用 Mocks 輕鬆測試

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderValidator validator;

    @Mock
    private PriceCalculator calculator;

    @Mock
    private PaymentProcessor paymentProcessor;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void should_process_order_successfully() {
        // Given
        OrderRequest request = createOrderRequest();
        when(calculator.calculateTotal(request)).thenReturn(Money.of(100, "USD"));
        when(paymentProcessor.process(any())).thenReturn(PaymentResult.success());

        // When
        OrderResult result = orderService.processOrder(request);

        // Then
        assertThat(result.isSuccessful()).isTrue();
        verify(validator).validate(request);
        verify(emailService).sendConfirmation(request.getCustomerEmail());
    }
}
```

---

## 範例 3：使用 Spring 進行配置

### 基於介面的注入

```java
// 定義介面
public interface PaymentProcessor {
    PaymentResult process(Money amount);
}

public interface EmailService {
    void sendConfirmation(String email);
}

// 多個實作
@Component
public class CreditCardPaymentProcessor implements PaymentProcessor {
    @Override
    public PaymentResult process(Money amount) {
        // Credit card processing
        return PaymentResult.success();
    }
}

@Component
public class PayPalPaymentProcessor implements PaymentProcessor {
    @Override
    public PaymentResult process(Money amount) {
        // PayPal processing
        return PaymentResult.success();
    }
}

// 配置
@Configuration
public class PaymentConfiguration {

    @Bean
    @Primary
    public PaymentProcessor paymentProcessor() {
        return new CreditCardPaymentProcessor();
    }

    @Bean
    @Qualifier("paypal")
    public PaymentProcessor paypalProcessor() {
        return new PayPalPaymentProcessor();
    }
}

// 使用 qualifier
@Service
public class OrderService {
    private final PaymentProcessor paymentProcessor;

    public OrderService(@Qualifier("paypal") PaymentProcessor paymentProcessor) {
        this.paymentProcessor = paymentProcessor;
    }
}
```

---

## 關鍵原則

### 1. 依賴抽象

```java
// ✅ 正確：依賴介面
public class OrderService {
    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }
}

// ❌ 錯誤：依賴具體類別
public class OrderService {
    private final JpaOrderRepository repository;

    public OrderService(JpaOrderRepository repository) {
        this.repository = repository;
    }
}
```

### 2. 使用建構子注入

```java
// ✅ 正確：建構子注入
@Service
public class CustomerService {
    private final CustomerRepository repository;

    public CustomerService(CustomerRepository repository) {
        this.repository = repository;
    }
}

// ❌ 錯誤：欄位注入
@Service
public class CustomerService {
    @Autowired
    private CustomerRepository repository;
}
```

### 3. 明確依賴關係

```java
// ✅ 正確：清楚的依賴
public class OrderProcessor {
    private final OrderValidator validator;
    private final PriceCalculator calculator;
    private final InventoryService inventory;

    public OrderProcessor(
        OrderValidator validator,
        PriceCalculator calculator,
        InventoryService inventory
    ) {
        this.validator = validator;
        this.calculator = calculator;
        this.inventory = inventory;
    }
}
```

---

## 相關 模式

- **Dependency Inversion Principle (DIP)**：依賴抽象
- **Inversion of Control (IoC)**：框架控制物件建立
- **Service Locator**：DI 的替代方案（不建議）

## 延伸閱讀

- [Design Principles](../../steering/design-principles.md)
- [SOLID Principles](../../steering/design-principles.md#solid-principles)
