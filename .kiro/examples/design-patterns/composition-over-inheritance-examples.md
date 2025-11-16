# Composition Over Inheritance - 詳細範例

## 原則概述

**「優先使用物件組合而非類別繼承」** 是物件導向設計中最重要的原則之一。這個原則建議您應該透過組合（has-a 關係）而非繼承（is-a 關係）來實現程式碼重用，除非存在真正的子類型關係。

## 目錄

1. [何時使用繼承 vs 組合](#何時使用繼承-vs-組合)
2. [範例 1：折扣系統](#範例-1折扣系統)
3. [範例 2：通知系統](#範例-2通知系統)
4. [範例 3：付款處理](#範例-3付款處理)
5. [使用組合的設計模式](#使用組合的設計模式)
6. [常見陷阱](#常見陷阱)
7. [從繼承重構到組合](#從繼承重構到組合)

---

## 何時使用繼承 vs 組合

### 使用繼承當：

1. **存在真正的 "is-a" 關係**
   - Car IS-A Vehicle
   - Manager IS-A Employee
   - SavingsAccount IS-A BankAccount

2. **適用 Liskov 替換原則**
   - 子類型可以替換父類型而不破壞功能
   - 子類型不違反父類的契約

3. **淺層次結構（最多 1-2 層）**
   - 深層次結構變得脆弱且難以維護

4. **行為是類型的基礎**
   - 不只是新增功能，而是定義類型的本質

### 使用組合當：

1. **需要程式碼重用但沒有 "is-a" 關係**
   - Car HAS-A Engine（而非 IS-A Engine）
   - Order HAS-A DiscountStrategy

2. **想在執行時變更行為**
   - 動態切換策略
   - 即時新增/移除功能

3. **需要組合多個行為**
   - 避免子類別的組合爆炸
   - 混合搭配功能

4. **想避免脆弱基底類別問題**
   - 基底類別的變更不會破壞組合物件
   - 更好的封裝性

---

## 範例 1：折扣系統

### 問題：電商折扣管理

我們需要支援各種折扣類型：百分比折扣、固定金額折扣、季節性折扣、會員折扣，以及它們的組合。

### ❌ 錯誤方法：繼承層次結構

```java
// 基底類別
public abstract class Order {
    protected OrderId id;
    protected List<OrderItem> items;

    public abstract Money calculateTotal();
}

// 第一層：基本折扣類型
public class StandardOrder extends Order {
    @Override
    public Money calculateTotal() {
        return calculateSubtotal();
    }
}

public class PercentageDiscountOrder extends Order {
    private final double discountPercentage;

    @Override
    public Money calculateTotal() {
        Money subtotal = calculateSubtotal();
        return subtotal.subtract(subtotal.multiply(discountPercentage));
    }
}

// 第二層：會員特定折扣
public class PremiumMemberOrder extends PercentageDiscountOrder {
    public PremiumMemberOrder() {
        super(0.10); // 10% 折扣
    }
}

// 組合爆炸！
// 需要：GoldMemberSeasonalOrder, PremiumMemberFixedDiscountOrder 等
// 有 N 種折扣類型和 M 個會員等級，需要 N × M 個類別！
```

**此方法的問題：**
1. **組合爆炸**：每個組合需要新類別
2. **無法在執行時變更**：一旦建立，折扣類型就固定了
3. **脆弱基底類別**：Order 的變更影響所有子類別
4. **違反開放封閉原則**：必須修改程式碼才能新增新折扣類型

### ✅ 正確方法：使用 Strategy Pattern 組合

```java
// 單一 Order 類別，組合折扣策略
@AggregateRoot
public class Order {
    private final OrderId id;
    private final CustomerId customerId;
    private final List<OrderItem> items;
    private DiscountStrategy discountStrategy;

    public Order(OrderId id, CustomerId customerId, DiscountStrategy discountStrategy) {
        this.id = id;
        this.customerId = customerId;
        this.items = new ArrayList<>();
        this.discountStrategy = discountStrategy != null ? discountStrategy : new NoDiscount();
    }

    public Money calculateTotal() {
        Money subtotal = calculateSubtotal();
        return discountStrategy.apply(subtotal);
    }

    // 可在執行時變更折扣策略
    public void applyDiscount(DiscountStrategy newStrategy) {
        this.discountStrategy = newStrategy;
    }

    private Money calculateSubtotal() {
        return items.stream()
            .map(OrderItem::getSubtotal)
            .reduce(Money.ZERO, Money::add);
    }
}

// 折扣策略介面
public interface DiscountStrategy {
    Money apply(Money amount);
}

// 具體策略
public class NoDiscount implements DiscountStrategy {
    @Override
    public Money apply(Money amount) {
        return amount;
    }
}

public class PercentageDiscount implements DiscountStrategy {
    private final double percentage;

    public PercentageDiscount(double percentage) {
        this.percentage = percentage;
    }

    @Override
    public Money apply(Money amount) {
        Money discount = amount.multiply(percentage);
        return amount.subtract(discount);
    }
}

public class FixedAmountDiscount implements DiscountStrategy {
    private final Money discountAmount;

    public FixedAmountDiscount(Money discountAmount) {
        this.discountAmount = discountAmount;
    }

    @Override
    public Money apply(Money amount) {
        return amount.subtract(discountAmount);
    }
}

// 組合多個折扣
public class CompositeDiscount implements DiscountStrategy {
    private final List<DiscountStrategy> strategies;

    public CompositeDiscount(DiscountStrategy... strategies) {
        this.strategies = Arrays.asList(strategies);
    }

    @Override
    public Money apply(Money amount) {
        Money result = amount;
        for (DiscountStrategy strategy : strategies) {
            result = strategy.apply(result);
        }
        return result;
    }
}

// 使用範例
Order order = new Order(orderId, customerId, new PercentageDiscount(0.10));

// 執行時變更策略
order.applyDiscount(new CompositeDiscount(
    new PercentageDiscount(0.10),  // 10% 會員折扣
    new FixedAmountDiscount(Money.twd(100))  // 額外 $100 折扣
));
```

**優點：**
- 單一 Order 類別
- 可在執行時變更行為
- 易於新增新折扣類型
- 可組合多個折扣
- 更好的測試性

---

## 範例 2：通知系統

### ❌ 錯誤：繼承

```java
public abstract class Notification {
    protected String message;
    public abstract void send();
}

public class EmailNotification extends Notification {
    public void send() { /* send email */ }
}

public class SmsNotification extends Notification {
    public void send() { /* send SMS */ }
}

// 需要兩者都發送？建立新類別！
public class EmailAndSmsNotification extends Notification {
    public void send() {
        // 重複程式碼
        sendEmail();
        sendSms();
    }
}
```

### ✅ 正確：組合

```java
public interface NotificationChannel {
    void send(String message);
}

public class EmailChannel implements NotificationChannel {
    @Override
    public void send(String message) { /* send email */ }
}

public class SmsChannel implements NotificationChannel {
    @Override
    public void send(String message) { /* send SMS */ }
}

public class NotificationService {
    private final List<NotificationChannel> channels;

    public NotificationService(NotificationChannel... channels) {
        this.channels = Arrays.asList(channels);
    }

    public void notify(String message) {
        channels.forEach(channel -> channel.send(message));
    }
}

// 使用
NotificationService service = new NotificationService(
    new EmailChannel(),
    new SmsChannel()
);
```

---

## 範例 3：付款處理

### ✅ 使用組合實現策略模式

```java
public interface PaymentMethod {
    PaymentResult process(Money amount);
}

public class CreditCardPayment implements PaymentMethod {
    private final String cardNumber;

    @Override
    public PaymentResult process(Money amount) {
        // 信用卡處理邏輯
        return PaymentResult.success();
    }
}

public class PayPalPayment implements PaymentMethod {
    private final String email;

    @Override
    public PaymentResult process(Money amount) {
        // PayPal 處理邏輯
        return PaymentResult.success();
    }
}

public class PaymentProcessor {
    private final PaymentMethod paymentMethod;

    public PaymentProcessor(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentResult processPayment(Money amount) {
        return paymentMethod.process(amount);
    }

    // 可在執行時切換付款方式
    public void switchPaymentMethod(PaymentMethod newMethod) {
        this.paymentMethod = newMethod;
    }
}
```

---

## 使用組合的設計模式

### 1. Strategy Pattern（策略模式）

```java
// 使用組合來變更演算法
public class Sorter {
    private SortingStrategy strategy;

    public void setStrategy(SortingStrategy strategy) {
        this.strategy = strategy;
    }

    public <T> List<T> sort(List<T> items) {
        return strategy.sort(items);
    }
}
```

### 2. Decorator Pattern（裝飾器模式）

```java
// 使用組合動態新增責任
public interface Coffee {
    Money getCost();
    String getDescription();
}

public class SimpleCoffee implements Coffee {
    @Override
    public Money getCost() { return Money.twd(50); }

    @Override
    public String getDescription() { return "Simple coffee"; }
}

public class MilkDecorator implements Coffee {
    private final Coffee coffee;

    public MilkDecorator(Coffee coffee) {
        this.coffee = coffee;
    }

    @Override
    public Money getCost() {
        return coffee.getCost().add(Money.twd(10));
    }

    @Override
    public String getDescription() {
        return coffee.getDescription() + ", milk";
    }
}

// 使用
Coffee coffee = new MilkDecorator(new SimpleCoffee());
```

### 3. Composite Pattern（組合模式）

```java
public interface Component {
    void operation();
}

public class Leaf implements Component {
    @Override
    public void operation() { /* leaf operation */ }
}

public class Composite implements Component {
    private final List<Component> children = new ArrayList<>();

    public void add(Component component) {
        children.add(component);
    }

    @Override
    public void operation() {
        children.forEach(Component::operation);
    }
}
```

---

## 常見陷阱

### 陷阱 1：過度使用繼承

```java
// ❌ 錯誤
public class Logger {
    public void log(String message) { /* log */ }
}

public class TimestampedLogger extends Logger {
    @Override
    public void log(String message) {
        super.log(timestamp() + ": " + message);
    }
}

// ✅ 正確：使用組合
public class Logger {
    private final LogFormatter formatter;

    public Logger(LogFormatter formatter) {
        this.formatter = formatter;
    }

    public void log(String message) {
        String formatted = formatter.format(message);
        writeToLog(formatted);
    }
}
```

### 陷阱 2：深層繼承層次

```java
// ❌ 錯誤：深層次結構
Vehicle -> MotorizedVehicle -> Car -> SportsCar -> FerrariF40

// ✅ 正確：淺層次結構 + 組合
public class Car {
    private final Engine engine;  // 組合
    private final Transmission transmission;  // 組合

    // 行為透過組合的元件
}
```

---

## 從繼承重構到組合

### 步驟 1：識別繼承的使用

找出使用繼承但不符合真正 "is-a" 關係的類別。

### 步驟 2：提取介面

```java
// Before
public abstract class Discount {
    public abstract Money apply(Money amount);
}

public class PercentageDiscount extends Discount {
    // ...
}

// After
public interface DiscountStrategy {
    Money apply(Money amount);
}

public class PercentageDiscount implements DiscountStrategy {
    // ...
}
```

### 步驟 3：使用組合替換繼承

```java
// Before
public class PremiumOrder extends PercentageDiscountOrder {
    // ...
}

// After
public class Order {
    private final DiscountStrategy discountStrategy;

    public Order(DiscountStrategy strategy) {
        this.discountStrategy = strategy;
    }
}
```

### 步驟 4：測試並驗證

確保重構沒有破壞現有功能。

---

## 最佳實踐

### ✅ DO

1. **優先使用組合**來實現程式碼重用
2. **使用介面**定義行為契約
3. **在執行時**注入依賴
4. **保持淺層繼承**（最多 1-2 層）
5. **使用設計模式**（Strategy, Decorator, Composite）

### ❌ DON'T

1. **不要為了程式碼重用而使用繼承**
2. **不要建立深層繼承層次**
3. **不要違反 Liskov 替換原則**
4. **不要建立組合爆炸的類別**
5. **不要使用繼承來新增功能**

---

## 總結

**Composition Over Inheritance** 提供：
- **靈活性** - 執行時變更行為
- **可重用性** - 混合搭配元件
- **可維護性** - 避免脆弱的層次結構
- **可測試性** - 易於模擬和測試
- **可擴展性** - 新增新功能而不修改現有程式碼

記住：**繼承代表 "is-a"，組合代表 "has-a"**。當有疑慮時，選擇組合。

---

## 相關原則

- **Single Responsibility Principle** - 每個類別一個責任
- **Open/Closed Principle** - 對擴展開放，對修改封閉
- **Dependency Inversion Principle** - 依賴抽象，而非具體實作
- **Interface Segregation Principle** - 小而專注的介面

## 延伸閱讀

- [Design Principles](../../steering/design-principles.md)
- [SOLID Principles](../../steering/design-principles.md#solid-principles)
- [Design Patterns](../../steering/design-patterns.md)
