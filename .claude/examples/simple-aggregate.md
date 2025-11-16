# 簡單聚合根範例

**Kiro 模式**: Idempotency + Immutable Input + Event-Driven

---

## 範例：Product 聚合根

這是一個簡單的聚合根範例，展示 Kiro 技巧的基本應用。

### 聚合根類別

```java
package solid.humank.genaidemo.domain.product.model.aggregate;

import solid.humank.genaidemo.domain.common.aggregate.AggregateRoot;
import solid.humank.genaidemo.domain.common.annotations.AggregateRoot as AR;
import solid.humank.genaidemo.domain.product.model.valueobject.*;
import solid.humank.genaidemo.domain.product.events.*;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Product 聚合根
 *
 * Kiro 技巧應用：
 * 1. Idempotency: 使用不可變的值物件（ProductId, ProductName, Price）
 * 2. Immutable Input: 建構子參數都是不可變物件
 * 3. Event-Driven: 狀態變更時收集領域事件
 * 4. Boundary Control: 所有狀態變更透過業務方法
 * 5. Fail-Safe: 建構子驗證輸入，Fail-Fast
 */
@AR(
    name = "Product",
    description = "產品聚合根",
    boundedContext = "Product",
    version = "1.0"
)
public class Product extends AggregateRoot {

    // ===== 識別符（不可變）=====
    private final ProductId id;

    // ===== 狀態（透過業務方法變更）=====
    private ProductName name;
    private Price price;
    private ProductStatus status;
    private int stockQuantity;

    // ===== 時間戳（追蹤）=====
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 建構子 - 建立新產品
     *
     * Kiro Pattern: Immutable Input + Fail-Fast Validation
     */
    public Product(ProductId id, ProductName name, Price price) {
        // Fail-Fast 驗證
        Objects.requireNonNull(id, "Product ID 不能為空");
        Objects.requireNonNull(name, "Product Name 不能為空");
        Objects.requireNonNull(price, "Price 不能為空");

        // 初始化（Immutable Input）
        this.id = id;
        this.name = name;
        this.price = price;
        this.status = ProductStatus.DRAFT;
        this.stockQuantity = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;

        // 收集領域事件（Event-Driven）
        collectEvent(ProductCreatedEvent.create(this.id, this.name, this.price));
    }

    /**
     * 重建建構子 - 從持久化重建
     *
     * Kiro Pattern: Reconstruction 不觸發事件
     */
    @AggregateReconstruction.ReconstructionConstructor
    protected Product(
        ProductId id,
        ProductName name,
        Price price,
        ProductStatus status,
        int stockQuantity,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.status = status;
        this.stockQuantity = stockQuantity;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        // 注意：重建時不發布事件（Idempotency）
    }

    // ===== 業務方法（Boundary Control）=====

    /**
     * 發布產品
     *
     * Kiro Pattern: Event-Driven + Boundary Control
     */
    public void publish() {
        // 驗證業務規則（Fail-Safe）
        validatePublish();

        // 變更狀態（Boundary Control）
        this.status = ProductStatus.PUBLISHED;
        this.updatedAt = LocalDateTime.now();

        // 收集事件（Event-Driven）
        collectEvent(ProductPublishedEvent.create(this.id));
    }

    /**
     * 更新價格
     *
     * Kiro Pattern: Immutable Input + Event-Driven
     */
    public void updatePrice(Price newPrice) {
        // 驗證（Fail-Safe）
        Objects.requireNonNull(newPrice, "新價格不能為空");

        // 檢查是否變更（Idempotency）
        if (this.price.equals(newPrice)) {
            return;  // 無變更，不觸發事件
        }

        Price oldPrice = this.price;
        this.price = newPrice;
        this.updatedAt = LocalDateTime.now();

        // 收集事件（Event-Driven）
        collectEvent(ProductPriceUpdatedEvent.create(this.id, oldPrice, newPrice));
    }

    /**
     * 增加庫存
     *
     * Kiro Pattern: Boundary Control
     */
    public void increaseStock(int quantity) {
        // 驗證（Fail-Safe）
        if (quantity <= 0) {
            throw new IllegalArgumentException("增加數量必須大於 0");
        }

        this.stockQuantity += quantity;
        this.updatedAt = LocalDateTime.now();

        // 收集事件
        collectEvent(StockIncreasedEvent.create(this.id, quantity, this.stockQuantity));
    }

    // ===== 驗證方法（Fail-Safe Pattern）=====

    private void validatePublish() {
        if (this.status == ProductStatus.PUBLISHED) {
            throw new IllegalStateException("產品已經發布，無法重複發布");
        }
        if (this.price.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("產品價格必須大於 0 才能發布");
        }
    }

    // ===== Getters（只暴露不可變副本）=====

    public ProductId getId() { return id; }
    public ProductName getName() { return name; }
    public Price getPrice() { return price; }
    public ProductStatus getStatus() { return status; }
    public int getStockQuantity() { return stockQuantity; }

    // ===== Equals/HashCode（基於 ID）=====

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof Product other) {
            return Objects.equals(id, other.id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Product{" +
            "id=" + id +
            ", name=" + name +
            ", price=" + price +
            ", status=" + status +
            '}';
    }
}
```

### 值物件範例

```java
package solid.humank.genaidemo.domain.product.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

/**
 * Price 值物件
 *
 * Kiro Pattern: Immutable Input + Fail-Fast Validation
 */
@ValueObject(name = "Price", description = "產品價格")
public record Price(BigDecimal amount, Currency currency) {

    /**
     * 緊湊建構子 - Fail-Fast 驗證
     */
    public Price {
        Objects.requireNonNull(amount, "金額不能為空");
        Objects.requireNonNull(currency, "貨幣不能為空");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("價格不能為負數");
        }
    }

    /**
     * 工廠方法
     */
    public static Price of(BigDecimal amount, String currencyCode) {
        return new Price(amount, Currency.getInstance(currencyCode));
    }

    public static Price twd(double amount) {
        return new Price(BigDecimal.valueOf(amount), Currency.getInstance("TWD"));
    }

    /**
     * 業務方法（Immutable）
     */
    public Price multiply(int multiplier) {
        return new Price(amount.multiply(BigDecimal.valueOf(multiplier)), currency);
    }

    public boolean isGreaterThan(Price other) {
        requireSameCurrency(other);
        return amount.compareTo(other.amount) > 0;
    }

    private void requireSameCurrency(Price other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("不同貨幣無法比較");
        }
    }
}
```

### 領域事件範例

```java
package solid.humank.genaidemo.domain.product.events;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.product.model.valueobject.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ProductCreatedEvent 領域事件
 *
 * Kiro Pattern: Immutable Input + Event-Driven
 */
public record ProductCreatedEvent(
    ProductId productId,
    ProductName productName,
    Price price,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent {

    /**
     * 工廠方法 - Idempotency
     */
    public static ProductCreatedEvent create(
        ProductId productId,
        ProductName productName,
        Price price
    ) {
        var metadata = DomainEvent.createEventMetadata();
        return new ProductCreatedEvent(
            productId,
            productName,
            price,
            metadata.eventId(),
            metadata.occurredOn()
        );
    }

    @Override
    public String getEventType() {
        return DomainEvent.getEventTypeFromClass(this.getClass());
    }

    @Override
    public String getAggregateId() {
        return productId.value();
    }
}
```

---

## Kiro 技巧對照表

| Kiro 技巧 | 程式碼位置 | 實作方式 |
|----------|----------|---------|
| **Idempotency** | 全部 | 使用不可變值物件、相同輸入產生相同結果 |
| **Immutable Input** | 建構子參數 | 所有參數都是不可變物件（Record） |
| **Event-Driven** | 業務方法 | 狀態變更時收集領域事件 |
| **Boundary Control** | 業務方法 | 所有狀態變更透過業務方法，不暴露 setter |
| **Fail-Safe** | 驗證方法 | 建構子和業務方法都進行 Fail-Fast 驗證 |
| **Stateless Handler** | - | 聚合根本身不保留不必要的狀態 |

---

## 測試範例

```java
@ExtendWith(MockitoExtension.class)
class ProductTest {

    @Test
    void should_create_product_successfully_when_valid_input_provided() {
        // Given: Immutable Input
        ProductId id = ProductId.generate();
        ProductName name = ProductName.of("iPhone 15");
        Price price = Price.twd(30000);

        // When
        Product product = new Product(id, name, price);

        // Then: Verify Idempotency
        assertThat(product.getId()).isEqualTo(id);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.DRAFT);

        // Verify Event-Driven
        assertThat(product.hasUncommittedEvents()).isTrue();
        assertThat(product.getUncommittedEvents())
            .hasSize(1)
            .first()
            .isInstanceOf(ProductCreatedEvent.class);
    }

    @Test
    void should_throw_exception_when_null_id_provided() {
        // Given: Invalid Input (Fail-Safe)
        ProductId nullId = null;
        ProductName name = ProductName.of("iPhone 15");
        Price price = Price.twd(30000);

        // When & Then: Verify Fail-Fast
        assertThatThrownBy(() -> new Product(nullId, name, price))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("不能為空");
    }

    @Test
    void should_not_trigger_event_when_price_unchanged() {
        // Given
        Product product = createProduct();
        product.markEventsAsCommitted();  // 清除初始事件

        // When: 設定相同價格（Idempotency）
        Price samePrice = product.getPrice();
        product.updatePrice(samePrice);

        // Then: 不應觸發事件
        assertThat(product.hasUncommittedEvents()).isFalse();
    }
}
```

---

**Kiro 技巧總結**:
1. ✅ Idempotency - 測試可重複執行
2. ✅ Immutable Input - 使用不可變值物件
3. ✅ Event-Driven - 狀態變更觸發事件
4. ✅ Boundary Control - 透過業務方法變更狀態
5. ✅ Fail-Safe - Fail-Fast 驗證
6. ✅ Stateless Handler - 測試不保留狀態
