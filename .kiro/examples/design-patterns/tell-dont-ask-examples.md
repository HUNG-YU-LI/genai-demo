# Tell, Don't Ask - 詳細範例

## 原則概述

**Tell, Don't Ask** 意味著物件應該告訴其他物件做什麼，而不是詢問其狀態然後基於該狀態做決定。這個原則有助於維護封裝性，並將行為保持在它所操作的資料旁邊。

## 核心概念

- **Ask（詢問）**：從物件取得資料並做決定
- **Tell（告訴）**：指示物件執行操作

---

## 範例 1：訂單狀態管理

### ❌ 錯誤：詢問狀態

```java
// Controller 詢問狀態並做決定
@RestController
public class OrderController {

    @PostMapping("/orders/{id}/submit")
    public ResponseEntity<OrderResponse> submitOrder(@PathVariable String id) {
        Order order = orderRepository.findById(id).orElseThrow();

        // 詢問狀態並做決定
        if (order.getStatus() == OrderStatus.CREATED) {
            if (order.getItems().isEmpty()) {
                throw new BusinessRuleViolationException("Cannot submit empty order");
            }

            order.setStatus(OrderStatus.PENDING);
            order.setSubmittedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());

            orderRepository.save(order);

            // 更多外部邏輯
            emailService.sendOrderConfirmation(order.getCustomerId());

            return ResponseEntity.ok(OrderResponse.from(order));
        } else {
            throw new InvalidOrderStateException("Order cannot be submitted");
        }
    }
}
```

**問題：**
- Controller 知道太多 Order 的內部狀態
- 業務邏輯散落在領域模型之外
- 難以測試業務規則
- 違反封裝性

### ✅ 正確：告訴做什麼

```java
// Controller 告訴 order 做什麼
@RestController
public class OrderController {

    @PostMapping("/orders/{id}/submit")
    public ResponseEntity<OrderResponse> submitOrder(@PathVariable String id) {
        Order order = orderRepository.findById(id).orElseThrow();

        // 告訴 order 提交自己
        order.submit();

        orderRepository.save(order);

        // 發佈由 order 收集的事件
        domainEventService.publishEventsFromAggregate(order);

        return ResponseEntity.ok(OrderResponse.from(order));
    }
}

// Order 處理自己的狀態轉換
@AggregateRoot
public class Order {
    private OrderId id;
    private OrderStatus status;
    private List<OrderItem> items;
    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;

    public void submit() {
        // 業務規則被封裝
        validateCanBeSubmitted();

        // 狀態變更是內部的
        this.status = OrderStatus.PENDING;
        this.submittedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        // 收集領域事件
        collectEvent(OrderSubmittedEvent.create(id, customerId, calculateTotal()));
    }

    private void validateCanBeSubmitted() {
        if (status != OrderStatus.CREATED) {
            throw new InvalidOrderStateException(
                "Order must be in CREATED status to be submitted. Current status: " + status
            );
        }

        if (items.isEmpty()) {
            throw new BusinessRuleViolationException("Cannot submit empty order");
        }
    }
}
```

**優點：**
- 業務邏輯在領域模型中
- Order 控制自己的狀態轉換
- 易於測試業務規則
- 維護封裝性

---

## 範例 2：客戶折扣計算

### ❌ 錯誤：詢問狀態

```java
@Service
public class OrderService {

    public Money calculateOrderTotal(Order order, Customer customer) {
        Money subtotal = Money.ZERO;

        // 詢問 items 並在外部計算
        for (OrderItem item : order.getItems()) {
            Money itemPrice = item.getProduct().getPrice();
            int quantity = item.getQuantity();
            subtotal = subtotal.add(itemPrice.multiply(quantity));
        }

        // 詢問客戶類型並在外部計算折扣
        if (customer.getMembershipLevel() == MembershipLevel.PREMIUM) {
            Money discount = subtotal.multiply(0.10); // 10% discount
            return subtotal.subtract(discount);
        } else if (customer.getMembershipLevel() == MembershipLevel.GOLD) {
            Money discount = subtotal.multiply(0.05); // 5% discount
            return subtotal.subtract(discount);
        }

        return subtotal;
    }
}
```

**問題：**
- Service 知道折扣計算邏輯
- Customer 的行為在 Customer 外部實作
- 難以新增新的會員等級
- 折扣邏輯重複

### ✅ 正確：告訴做什麼

```java
@Service
public class OrderService {

    public Money calculateOrderTotal(Order order, Customer customer) {
        // 告訴 order 計算其小計
        Money subtotal = order.calculateSubtotal();

        // 告訴 customer 應用其折扣
        return customer.applyDiscount(subtotal);
    }
}

// Order 知道如何計算自己的小計
@AggregateRoot
public class Order {
    private List<OrderItem> items;

    public Money calculateSubtotal() {
        return items.stream()
            .map(OrderItem::calculateSubtotal)
            .reduce(Money.ZERO, Money::add);
    }
}

// OrderItem 知道如何計算自己的小計
public class OrderItem {
    private Product product;
    private int quantity;

    public Money calculateSubtotal() {
        return product.getPrice().multiply(quantity);
    }
}

// Customer 知道如何應用自己的折扣
@AggregateRoot
public class Customer {
    private MembershipLevel membershipLevel;

    public Money applyDiscount(Money amount) {
        return membershipLevel.applyDiscount(amount);
    }
}

// MembershipLevel 封裝折扣邏輯
public enum MembershipLevel {
    STANDARD(0.0),
    PREMIUM(0.10),
    GOLD(0.05);

    private final double discountRate;

    MembershipLevel(double discountRate) {
        this.discountRate = discountRate;
    }

    public Money applyDiscount(Money amount) {
        if (discountRate == 0.0) {
            return amount;
        }
        Money discount = amount.multiply(discountRate);
        return amount.subtract(discount);
    }
}
```

**優點：**
- 每個物件處理自己的計算
- 易於新增新的會員等級
- 折扣邏輯集中化
- 遵循單一責任原則

---

## 範例 3：庫存管理

### ❌ 錯誤：詢問狀態

```java
@Service
public class OrderProcessingService {

    public void processOrder(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            int requestedQuantity = item.getQuantity();

            // 詢問庫存狀態
            Inventory inventory = inventoryRepository.findByProductId(product.getId());
            int availableQuantity = inventory.getAvailableQuantity();

            // 基於狀態做決定
            if (availableQuantity >= requestedQuantity) {
                inventory.setAvailableQuantity(availableQuantity - requestedQuantity);
                inventory.setReservedQuantity(inventory.getReservedQuantity() + requestedQuantity);
                inventoryRepository.save(inventory);
            } else {
                throw new InsufficientInventoryException(
                    "Not enough inventory for product: " + product.getName()
                );
            }
        }
    }
}
```

**問題：**
- Service 直接操作庫存狀態
- 庫存業務規則在 Inventory 外部
- 沒有庫存邏輯的封裝

### ✅ 正確：告訴做什麼

```java
@Service
public class OrderProcessingService {

    public void processOrder(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            int requestedQuantity = item.getQuantity();

            // 告訴庫存預留項目
            Inventory inventory = inventoryRepository.findByProductId(product.getId());
            inventory.reserve(requestedQuantity);

            inventoryRepository.save(inventory);
        }
    }
}

// Inventory 處理自己的狀態
@AggregateRoot
public class Inventory {
    private ProductId productId;
    private int availableQuantity;
    private int reservedQuantity;

    public void reserve(int quantity) {
        validateCanReserve(quantity);

        this.availableQuantity -= quantity;
        this.reservedQuantity += quantity;

        collectEvent(InventoryReservedEvent.create(productId, quantity));
    }

    private void validateCanReserve(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        if (availableQuantity < quantity) {
            throw new InsufficientInventoryException(
                String.format("Cannot reserve %d items. Only %d available",
                    quantity, availableQuantity)
            );
        }
    }

    public void release(int quantity) {
        validateCanRelease(quantity);

        this.reservedQuantity -= quantity;
        this.availableQuantity += quantity;

        collectEvent(InventoryReleasedEvent.create(productId, quantity));
    }

    private void validateCanRelease(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        if (reservedQuantity < quantity) {
            throw new IllegalStateException(
                String.format("Cannot release %d items. Only %d reserved",
                    quantity, reservedQuantity)
            );
        }
    }
}
```

**優點：**
- Inventory 控制自己的狀態
- 業務規則被封裝
- 易於新增新的庫存操作
- 清楚的驗證邏輯

---

## 範例 4：付款處理

### ❌ 錯誤：詢問狀態

```java
@Service
public class PaymentService {

    public void processPayment(Order order, PaymentMethod paymentMethod) {
        Money amount = order.getTotalAmount();

        // 詢問付款方式細節
        if (paymentMethod.getType() == PaymentMethodType.CREDIT_CARD) {
            CreditCard card = paymentMethod.getCreditCard();

            // 外部驗證
            if (card.getExpiryDate().isBefore(LocalDate.now())) {
                throw new ExpiredCardException("Credit card has expired");
            }

            if (card.getAvailableCredit().isLessThan(amount)) {
                throw new InsufficientFundsException("Insufficient credit");
            }

            // 外部處理
            card.setAvailableCredit(card.getAvailableCredit().subtract(amount));
            paymentMethodRepository.save(paymentMethod);

        } else if (paymentMethod.getType() == PaymentMethodType.BANK_ACCOUNT) {
            BankAccount account = paymentMethod.getBankAccount();

            if (account.getBalance().isLessThan(amount)) {
                throw new InsufficientFundsException("Insufficient balance");
            }

            account.setBalance(account.getBalance().subtract(amount));
            paymentMethodRepository.save(paymentMethod);
        }
    }
}
```

**問題：**
- Service 知道付款方式內部
- 驗證邏輯在外部
- 難以新增新的付款方式
- 違反開放封閉原則

### ✅ 正確：告訴做什麼

```java
@Service
public class PaymentService {

    public void processPayment(Order order, PaymentMethod paymentMethod) {
        Money amount = order.getTotalAmount();

        // 告訴付款方式處理付款
        paymentMethod.processPayment(amount);

        paymentMethodRepository.save(paymentMethod);

        // 告訴 order 已付款
        order.markAsPaid();
        orderRepository.save(order);
    }
}

// PaymentMethod 是一個介面
public interface PaymentMethod {
    void processPayment(Money amount);
    PaymentMethodType getType();
}

// CreditCard 實作自己的邏輯
public class CreditCard implements PaymentMethod {
    private String cardNumber;
    private LocalDate expiryDate;
    private Money availableCredit;

    @Override
    public void processPayment(Money amount) {
        validatePayment(amount);

        this.availableCredit = availableCredit.subtract(amount);
    }

    private void validatePayment(Money amount) {
        if (expiryDate.isBefore(LocalDate.now())) {
            throw new ExpiredCardException("Credit card has expired");
        }

        if (availableCredit.isLessThan(amount)) {
            throw new InsufficientFundsException(
                String.format("Insufficient credit. Available: %s, Required: %s",
                    availableCredit, amount)
            );
        }
    }

    @Override
    public PaymentMethodType getType() {
        return PaymentMethodType.CREDIT_CARD;
    }
}

// BankAccount 實作自己的邏輯
public class BankAccount implements PaymentMethod {
    private String accountNumber;
    private Money balance;

    @Override
    public void processPayment(Money amount) {
        validatePayment(amount);

        this.balance = balance.subtract(amount);
    }

    private void validatePayment(Money amount) {
        if (balance.isLessThan(amount)) {
            throw new InsufficientFundsException(
                String.format("Insufficient balance. Available: %s, Required: %s",
                    balance, amount)
            );
        }
    }

    @Override
    public PaymentMethodType getType() {
        return PaymentMethodType.BANK_ACCOUNT;
    }
}
```

**優點：**
- 每個付款方式處理自己的邏輯
- 易於新增新的付款方式
- 遵循策略模式
- 多型行為

---

## 快速參考

### 何時使用 Tell, Don't Ask

✅ **使用時機：**
- 物件擁有做決定所需的資料
- 行為自然屬於該資料
- 您想要維護封裝性
- 您需要遵循單一責任原則

❌ **不使用時機：**
- 您需要協調多個物件
- 您在實作純演算法
- 您在 application service 中編排 use cases

### 常見違反

1. **Getter 鏈結**：`order.getCustomer().getAddress().getCity()`
2. **基於類型的 If-else**：`if (customer.getType() == CustomerType.PREMIUM)`
3. **外部計算**：在聚合外計算總計
4. **狀態操作**：從外部設定多個欄位

### 重構步驟

1. 識別基於物件狀態做決定的地方
2. 將決定邏輯移到擁有資料的物件中
3. 將 getters 替換為行為方法
4. 測試業務規則現在被封裝

---

## 相關原則

- **Law of Demeter**：只與直接朋友交流
- **Single Responsibility**：每個類別只有一個變更的理由
- **Encapsulation**：隱藏內部狀態並暴露行為

## 延伸閱讀

- [Law of Demeter Examples](law-of-demeter-examples.md)
- [Design Principles](../../steering/design-principles.md)
