# Law of Demeter - 詳細範例

## 原則概述

**Law of Demeter**（最少知識原則）指出，物件應該只與其直接朋友交流，而不是與陌生人交流。這減少耦合並使程式碼更易於維護。

## 「單點」規則

方法應該只呼叫以下物件的方法：
1. 物件本身（`this`）
2. 作為參數傳入的物件
3. 本地建立的物件
4. 直接元件物件（欄位）

**例外**：Fluent APIs 和 builders 是可接受的（例如 `builder.withName().withEmail().build()`）

---

## 範例 1：地址資訊

### ❌ 錯誤：與陌生人交談

```java
@RestController
public class OrderController {

    @GetMapping("/orders/{id}/shipping-city")
    public String getShippingCity(@PathVariable String id) {
        Order order = orderRepository.findById(id).orElseThrow();

        // 違反：透過多個物件鏈結
        return order.getCustomer().getAddress().getCity().getName();
        //     ↑ friend    ↑ stranger  ↑ stranger  ↑ stranger
    }
}
```

**問題：**
- Controller 知道 Order 的內部結構
- Customer 或 Address 的變更會破壞 controller
- 跨多個類別的高耦合
- 難以測試和模擬

### ✅ 正確：只詢問朋友

```java
@RestController
public class OrderController {

    @GetMapping("/orders/{id}/shipping-city")
    public String getShippingCity(@PathVariable String id) {
        Order order = orderRepository.findById(id).orElseThrow();

        // 告訴 order 取得配送城市
        return order.getShippingCityName();
    }
}

// Order 提供直接方法
@AggregateRoot
public class Order {
    private Customer customer;
    private Address shippingAddress;

    public String getShippingCityName() {
        return shippingAddress.getCityName();
    }
}

// Address 提供直接方法
public class Address {
    private City city;

    public String getCityName() {
        return city.getName();
    }
}
```

**優點：**
- Controller 只與 Order 交流
- Order 封裝如何取得城市名稱
- 易於變更內部結構
- 低耦合

---

## 範例 2：價格計算

### ❌ 錯誤：深層導航

```java
@Service
public class InvoiceService {

    public Money calculateInvoiceTotal(Invoice invoice) {
        Money total = Money.ZERO;

        // 違反：導航深入物件結構
        for (InvoiceLine line : invoice.getLines()) {
            Product product = line.getProduct();
            Money unitPrice = product.getPricing().getCurrentPrice().getAmount();
            int quantity = line.getQuantity();

            total = total.add(unitPrice.multiply(quantity));
        }

        return total;
    }
}
```

**問題：**
- Service 知道 Product 的定價結構
- 定價結構變更會破壞功能
- 計算邏輯在外部

### ✅ 正確：委派給物件

```java
@Service
public class InvoiceService {

    public Money calculateInvoiceTotal(Invoice invoice) {
        // 告訴 invoice 計算自己的總計
        return invoice.calculateTotal();
    }
}

// Invoice 計算自己的總計
public class Invoice {
    private List<InvoiceLine> lines;

    public Money calculateTotal() {
        return lines.stream()
            .map(InvoiceLine::calculateSubtotal)
            .reduce(Money.ZERO, Money::add);
    }
}

// InvoiceLine 計算自己的小計
public class InvoiceLine {
    private Product product;
    private int quantity;

    public Money calculateSubtotal() {
        Money unitPrice = product.getCurrentPrice();
        return unitPrice.multiply(quantity);
    }
}

// Product 提供簡單介面
public class Product {
    private Pricing pricing;

    public Money getCurrentPrice() {
        return pricing.getCurrentAmount();
    }
}

// Pricing 封裝其結構
public class Pricing {
    private Price currentPrice;

    public Money getCurrentAmount() {
        return currentPrice.getAmount();
    }
}
```

**優點：**
- 每個物件處理自己的計算
- 定價結構變更不影響呼叫者
- 清楚的責任鏈

---

## 範例 3：通知偏好設定

### ❌ 錯誤：穿透物件存取

```java
@Service
public class NotificationService {

    public void sendOrderConfirmation(Order order) {
        // 違反：穿透多個物件
        Customer customer = order.getCustomer();
        ContactInfo contactInfo = customer.getContactInfo();
        NotificationPreferences prefs = contactInfo.getNotificationPreferences();

        if (prefs.isEmailEnabled()) {
            String email = contactInfo.getEmail().getValue();
            emailService.send(email, "Order Confirmation", buildMessage(order));
        }

        if (prefs.isSmsEnabled()) {
            String phone = contactInfo.getPhone().getValue();
            smsService.send(phone, buildShortMessage(order));
        }
    }
}
```

**問題：**
- Service 導航深層物件圖
- 知道內部結構
- 難以變更通知邏輯

### ✅ 正確：使用 Facade 方法

```java
@Service
public class NotificationService {

    public void sendOrderConfirmation(Order order) {
        Customer customer = order.getCustomer();

        // 詢問 customer 通知管道
        if (customer.prefersEmailNotifications()) {
            String email = customer.getEmailAddress();
            emailService.send(email, "Order Confirmation", buildMessage(order));
        }

        if (customer.prefersSmsNotifications()) {
            String phone = customer.getPhoneNumber();
            smsService.send(phone, buildShortMessage(order));
        }
    }
}

// Customer 提供 facade 方法
@AggregateRoot
public class Customer {
    private ContactInfo contactInfo;

    public boolean prefersEmailNotifications() {
        return contactInfo.isEmailNotificationEnabled();
    }

    public boolean prefersSmsNotifications() {
        return contactInfo.isSmsNotificationEnabled();
    }

    public String getEmailAddress() {
        return contactInfo.getEmailValue();
    }

    public String getPhoneNumber() {
        return contactInfo.getPhoneValue();
    }
}

// ContactInfo 封裝其結構
public class ContactInfo {
    private Email email;
    private Phone phone;
    private NotificationPreferences preferences;

    public boolean isEmailNotificationEnabled() {
        return preferences.isEmailEnabled();
    }

    public boolean isSmsNotificationEnabled() {
        return preferences.isSmsEnabled();
    }

    public String getEmailValue() {
        return email.getValue();
    }

    public String getPhoneValue() {
        return phone.getValue();
    }
}
```

**優點：**
- Service 只與 Customer 交流
- Customer 封裝聯絡細節
- 易於變更通知邏輯

---

## 範例 4：訂單驗證

### ❌ 錯誤：檢查內部狀態

```java
@Service
public class OrderValidationService {

    public void validateOrder(Order order) {
        // 違反：檢查深層內部狀態
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            Inventory inventory = product.getInventory();

            if (inventory.getAvailableQuantity() < item.getQuantity()) {
                throw new InsufficientInventoryException(
                    "Not enough inventory for: " + product.getName()
                );
            }

            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw new InactiveProductException(
                    "Product is not active: " + product.getName()
                );
            }
        }
    }
}
```

**問題：**
- Service 知道 Product 和 Inventory 內部
- 驗證邏輯分散
- 難以維護

### ✅ 正確：委派驗證

```java
@Service
public class OrderValidationService {

    public void validateOrder(Order order) {
        // 告訴 order 驗證自己
        order.validate();
    }
}

// Order 驗證自己
@AggregateRoot
public class Order {
    private List<OrderItem> items;

    public void validate() {
        if (items.isEmpty()) {
            throw new BusinessRuleViolationException("Order must have at least one item");
        }

        items.forEach(OrderItem::validate);
    }
}

// OrderItem 驗證自己
public class OrderItem {
    private Product product;
    private int quantity;

    public void validate() {
        product.validateAvailability(quantity);
        product.validateActive();
    }
}

// Product 提供驗證方法
public class Product {
    private Inventory inventory;
    private ProductStatus status;

    public void validateAvailability(int requestedQuantity) {
        if (!inventory.hasAvailableQuantity(requestedQuantity)) {
            throw new InsufficientInventoryException(
                String.format("Not enough inventory for: %s. Requested: %d, Available: %d",
                    name, requestedQuantity, inventory.getAvailableQuantity())
            );
        }
    }

    public void validateActive() {
        if (status != ProductStatus.ACTIVE) {
            throw new InactiveProductException("Product is not active: " + name);
        }
    }
}

// Inventory 提供查詢方法
public class Inventory {
    private int availableQuantity;

    public boolean hasAvailableQuantity(int requested) {
        return availableQuantity >= requested;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }
}
```

**優點：**
- 每個物件驗證自己的狀態
- 驗證邏輯被封裝
- 易於新增新的驗證規則

---

## 範例 5：Fluent API（可接受的例外）

### ✅ 可接受：Builder Pattern

```java
// 這是可接受的，因為它是 fluent API
Customer customer = Customer.builder()
    .withName("John Doe")
    .withEmail("john@example.com")
    .withAddress(Address.builder()
        .withStreet("123 Main St")
        .withCity("New York")
        .withZipCode("10001")
        .build())
    .build();

// 這也是可接受的
String result = stringBuilder
    .append("Hello")
    .append(" ")
    .append("World")
    .toString();
```

**為什麼可接受：**
- Builder 回傳自身以進行鏈結
- 這是刻意的 API 設計模式
- 不暴露內部結構
- 提高可讀性

---

## 重構策略

### 策略 1：新增 Facade 方法

```java
// 之前
String city = order.getCustomer().getAddress().getCity();

// 之後
String city = order.getCustomerCity();

// 實作
public class Order {
    public String getCustomerCity() {
        return customer.getAddressCity();
    }
}

public class Customer {
    public String getAddressCity() {
        return address.getCity();
    }
}
```

### 策略 2：將行為移到擁有者

```java
// 之前
if (order.getCustomer().getMembershipLevel() == MembershipLevel.PREMIUM) {
    applyPremiumDiscount();
}

// 之後
if (order.hasCustomerWithPremiumMembership()) {
    applyPremiumDiscount();
}

// 實作
public class Order {
    public boolean hasCustomerWithPremiumMembership() {
        return customer.isPremiumMember();
    }
}

public class Customer {
    public boolean isPremiumMember() {
        return membershipLevel == MembershipLevel.PREMIUM;
    }
}
```

### 策略 3：使用 Value Objects

```java
// 之前
String fullAddress = customer.getAddress().getStreet() + ", " +
                    customer.getAddress().getCity() + ", " +
                    customer.getAddress().getZipCode();

// 之後
String fullAddress = customer.getFormattedAddress();

// 實作
public class Customer {
    private Address address;

    public String getFormattedAddress() {
        return address.format();
    }
}

public class Address {
    public String format() {
        return String.format("%s, %s, %s", street, city, zipCode);
    }
}
```

---

## 常見違反與修正

### 違反 1：配置存取

```java
// ❌ 錯誤
int maxRetries = config.getRetrySettings().getMaxAttempts();

// ✅ 正確
int maxRetries = config.getMaxRetryAttempts();
```

### 違反 2：集合導航

```java
// ❌ 錯誤
Money total = order.getItems().stream()
    .map(item -> item.getPrice())
    .reduce(Money.ZERO, Money::add);

// ✅ 正確
Money total = order.calculateTotal();
```

### 違反 3：巢狀條件

```java
// ❌ 錯誤
if (order.getCustomer().getAddress().getCountry().equals("US")) {
    applyUSTax();
}

// ✅ 正確
if (order.isShippingToUS()) {
    applyUSTax();
}
```

---

## 快速參考

### 允許的方法呼叫

```java
public class Example {
    private Dependency dependency;  // Field

    public void method(Parameter param) {  // Parameter
        // ✅ 允許
        this.doSomething();           // this
        param.doSomething();          // parameter
        dependency.doSomething();     // field

        Local local = new Local();    // local
        local.doSomething();          // local

        // ❌ 不允許
        dependency.getOther().doSomething();  // stranger
        param.getChild().doSomething();       // stranger
    }
}
```

### 優點

- **低耦合**：物件不依賴內部結構
- **高內聚**：行為與資料在一起
- **易於重構**：內部變更不會破壞客戶端
- **更好的測試**：需要更少的 mocks

### 取捨

- **更多方法**：需要 facade 方法
- **潛在重複**：不同類別中的類似方法
- **學習曲線**：需要思考物件責任

---

## 相關原則

- **Tell, Don't Ask**：告訴物件做什麼
- **Information Hiding**：隱藏內部結構
- **Encapsulation**：保護物件狀態

## 延伸閱讀

- [Tell, Don't Ask Examples](tell-dont-ask-examples.md)
- [Design Principles](../../steering/design-principles.md)
