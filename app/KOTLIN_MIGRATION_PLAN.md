# Java → Kotlin 完整遷移計劃
## GenAI Demo Project - /app/ Module

> **目標**：將 `/app/` 模組從 Java 21 遷移至 Kotlin 2.x，並進行深度的 idiomatic Kotlin 優化，而非簡單的語言替換。

---

## 📊 專案規模分析

### 統計資料
- **總檔案數**: 694 個 Java 源碼檔案
- **架構**: Clean Architecture + DDD + Hexagonal Architecture
- **技術棧**: Java 21, Spring Boot 3.5.7, JPA, Redis, AWS SDK

### 層級分布
| 層級 | 檔案數 | 主要內容 |
|------|-------|---------|
| **Domain Layer** | 291 | Aggregates, Entities, Value Objects, Events, Services |
| **Infrastructure Layer** | 237 | Repositories, Adapters, JPA Entities, Configurations |
| **Application Layer** | 90 | Application Services, DTOs, Use Cases |
| **Interfaces Layer** | 41 | REST Controllers, API DTOs |
| **Config + Exceptions** | 35 | 配置類, 異常處理 |

### Bounded Contexts (14個)
Customer, Order, Payment, Inventory, Product, Delivery, Promotion, ShoppingCart, Notification, Pricing, Review, Seller, Observability, Shared

---

## 🎯 Kotlin 化策略

### 一、語言層級優化重點

#### 1. Data Models 優化
| Java 模式 | Kotlin 優化 | 好處 |
|-----------|-------------|-----|
| `record ProductId(String value)` | `data class ProductId(val value: String)` | 更自然的 Kotlin 語法 |
| Java POJO + Lombok | `data class` | 消除樣板代碼，自動生成 equals/hashCode/toString |
| JPA Entity (getter/setter) | `data class` + `all-open` plugin | 簡潔 60% 代碼量 |
| Builder Pattern | Named Arguments | 更簡潔，無需額外 builder 類 |

#### 2. 值對象 (Value Objects)
```kotlin
// Before (Java Record - 105 lines)
public record ProductId(String value) {
    public ProductId {
        Objects.requireNonNull(value, "Product ID cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Product ID cannot be empty");
        }
    }
    // ... 20+ methods
}

// After (Kotlin - 30 lines, 70% reduction)
@ValueObject(name = "ProductId", description = "產品唯一標識符")
@JvmInline
value class ProductId(val value: String) {
    init {
        require(value.isNotBlank()) { "Product ID cannot be empty" }
    }

    fun toUUID(): UUID = UUID.fromString(value)
    val isUUIDFormat: Boolean get() = runCatching { UUID.fromString(value) }.isSuccess

    companion object {
        fun generate() = ProductId(UUID.randomUUID().toString())
        fun of(id: String) = ProductId(id)
        fun of(uuid: UUID) = ProductId(uuid.toString())
    }
}
```

**優化點**:
- ✅ 使用 `value class` (inline class) 實現零開銷抽象
- ✅ `init` 區塊取代 compact constructor
- ✅ `require()` 取代冗長的 Objects.requireNonNull
- ✅ computed property (`val isUUIDFormat`) 取代方法
- ✅ `companion object` 取代 static methods

#### 3. DTOs 優化
```kotlin
// Before (Java Record)
public record PromotionDto(
    String id,
    String name,
    String description,
    PromotionType type,
    PromotionStatus status,
    LocalDateTime startDate,
    LocalDateTime endDate,
    int usageLimit,
    int usageCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}

// After (Kotlin data class with defaults & null safety)
data class PromotionDto(
    val id: String,
    val name: String,
    val description: String?,  // Nullable for optional field
    val type: PromotionType,
    val status: PromotionStatus,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val usageLimit: Int = Int.MAX_VALUE,  // Smart default
    val usageCount: Int = 0,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    val isActive: Boolean
        get() = status == PromotionStatus.ACTIVE &&
                LocalDateTime.now() in startDate..endDate

    val remainingUsage: Int
        get() = (usageLimit - usageCount).coerceAtLeast(0)
}
```

**優化點**:
- ✅ Null safety (`description: String?`)
- ✅ Default values 減少建構子重載
- ✅ Computed properties 封裝業務邏輯
- ✅ Kotlin range operator (`in startDate..endDate`)

#### 4. JPA Entity 優化
```kotlin
// Before (Java - 179 lines with getters/setters)
@Entity
@Table(name = "product_reviews")
public class JpaProductReviewEntity {
    @Id
    private String id;

    @Column(name = "product_id", nullable = false)
    private String productId;

    // ... 15+ fields with getters/setters

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    // ... 30+ getter/setter methods
}

// After (Kotlin - 40 lines, 78% reduction)
@Entity
@Table(name = "product_reviews")
data class JpaProductReviewEntity(
    @Id
    var id: String,

    @Column(name = "product_id", nullable = false)
    var productId: String,

    @Column(name = "reviewer_id", nullable = false)
    var reviewerId: String,

    @Column(nullable = false)
    var rating: Int,

    @Column(columnDefinition = "TEXT")
    var comment: String? = null,

    @Column(nullable = false)
    var status: String,

    @Column(name = "submitted_at", nullable = false)
    var submittedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "last_modified_at")
    var lastModifiedAt: LocalDateTime? = null,

    @ElementCollection
    @CollectionTable(name = "review_images", joinColumns = [JoinColumn(name = "review_id")])
    @Column(name = "image_url")
    var images: List<String> = emptyList(),

    @Column(name = "moderator_comment")
    var moderatorComment: String? = null,

    @Column(name = "moderated_at")
    var moderatedAt: LocalDateTime? = null,

    @Column(name = "is_reported")
    var isReported: Boolean = false,

    @Column(name = "report_reason")
    var reportReason: String? = null,

    @Column(name = "reported_at")
    var reportedAt: LocalDateTime? = null
)
```

**優化點**:
- ✅ 消除 100+ 行 getter/setter 樣板代碼
- ✅ Default values 簡化初始化
- ✅ Null safety (`var comment: String?`)
- ✅ 使用 `var` (JPA 需要可變性)

#### 5. Domain Aggregate 優化
```kotlin
// Before (Java - 489 lines)
@AggregateRoot(...)
public class Order extends AggregateRoot {
    private final OrderId id;
    private final CustomerId customerId;
    private final List<OrderItem> items;
    private OrderStatus status;
    private Money totalAmount;
    // ... 400+ lines of methods
}

// After (Kotlin - 250 lines, 50% reduction)
@AggregateRoot(
    name = "Order",
    description = "訂單聚合根",
    boundedContext = "Order",
    version = "1.0"
)
@AggregateLifecycle.ManagedLifecycle
class Order(
    val id: OrderId,
    val customerId: CustomerId,
    val shippingAddress: String,
    private val items: MutableList<OrderItem> = mutableListOf(),
    private var status: OrderStatus = OrderStatus.CREATED,
    private var totalAmount: Money = Money.zero(),
    private var effectiveAmount: Money = totalAmount,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    private var updatedAt: LocalDateTime = createdAt
) : AggregateRoot() {

    private val stateTracker = AggregateStateTracker(this)

    // Public immutable view
    val itemsView: List<OrderItem> get() = items.toList()

    fun addItem(productId: String, productName: String, quantity: Int, price: Money) {
        require(status == OrderStatus.CREATED) {
            "Cannot add items to order in $status state"
        }

        val item = OrderItem(productId, productName, quantity, price)
        items += item  // Kotlin operator

        totalAmount += item.subtotal
        effectiveAmount = totalAmount
        updatedAt = LocalDateTime.now()

        collectEvent(OrderItemAddedEvent.create(id, productId, quantity, price))
    }

    fun submit() {
        validateOrderSubmission()

        val oldStatus = status
        stateTracker.trackChange("status", oldStatus, OrderStatus.PENDING) { _, _ ->
            OrderSubmittedEvent.create(id, customerId.toString(), totalAmount, items.size)
        }

        status = OrderStatus.PENDING
        updatedAt = LocalDateTime.now()

        CrossAggregateOperation.publishEvent(
            this,
            OrderInventoryReservationRequestedEvent(id, customerId, items)
        )
    }

    private fun validateOrderSubmission() {
        val violations = buildList {
            if (items.isEmpty()) {
                add("ORDER_ITEMS_REQUIRED" to "Cannot submit order with no items")
            }
            if (status != OrderStatus.CREATED) {
                add("ORDER_STATUS_INVALID" to "只有 CREATED 狀態可提交，當前：$status")
            }
            if (totalAmount.amount <= BigDecimal.ZERO) {
                add("ORDER_AMOUNT_INVALID" to "訂單金額必須大於零")
            }
        }

        if (violations.isNotEmpty()) {
            throw BusinessRuleViolationException("Order", id.value, violations)
        }
    }

    // ... other methods

    override fun equals(other: Any?) =
        this === other || (other is Order && id == other.id)

    override fun hashCode() = id.hashCode()

    override fun toString() =
        "Order(id=$id, customerId=$customerId, status=$status, totalAmount=$totalAmount, items=${items.size})"

    companion object {
        // Factory methods
        fun create(customerId: CustomerId, shippingAddress: String): Order {
            require(shippingAddress.isNotBlank()) { "Shipping address cannot be empty" }
            return Order(
                id = OrderId.generate(),
                customerId = customerId,
                shippingAddress = shippingAddress
            ).also {
                it.collectEvent(OrderCreatedEvent.create(it.id, customerId.toString(), Money.zero(), emptyList()))
            }
        }

        // Reconstruction for repository
        @AggregateReconstruction.ReconstructionConstructor("從持久化狀態重建")
        fun reconstruct(
            id: OrderId,
            customerId: CustomerId,
            shippingAddress: String,
            items: List<OrderItem>,
            status: OrderStatus,
            totalAmount: Money,
            effectiveAmount: Money,
            createdAt: LocalDateTime,
            updatedAt: LocalDateTime
        ) = Order(
            id, customerId, shippingAddress,
            items.toMutableList(), status, totalAmount, effectiveAmount, createdAt, updatedAt
        )
    }
}
```

**優化點**:
- ✅ Primary constructor 簡化初始化
- ✅ `require()` 取代冗長驗證邏輯
- ✅ Computed property (`val itemsView`) 提供不可變視圖
- ✅ `buildList` DSL 構建驗證錯誤
- ✅ `companion object` 封裝工廠方法
- ✅ Kotlin operators (`+=`, `in`)
- ✅ String templates (`$status`, `$totalAmount`)

#### 6. Application Service 優化
```kotlin
// Before (Java - 193 lines)
@Service
@Transactional
public class PromotionApplicationService {
    private final PromotionRepository promotionRepository;
    private final CartSummaryConverter cartSummaryConverter;

    public PromotionApplicationService(
        PromotionRepository promotionRepository,
        CartSummaryConverter cartSummaryConverter,
        DomainEventApplicationService domainEventApplicationService
    ) {
        this.promotionRepository = promotionRepository;
        this.cartSummaryConverter = cartSummaryConverter;
        this.domainEventApplicationService = domainEventApplicationService;
    }

    public PromotionDto createFlashSalePromotion(...) { ... }
    // ... many methods
}

// After (Kotlin - 120 lines, 38% reduction)
@Service
@Transactional
class PromotionApplicationService(
    private val promotionRepository: PromotionRepository,
    private val cartSummaryConverter: CartSummaryConverter,
    private val domainEventApplicationService: DomainEventApplicationService
) {

    fun createFlashSalePromotion(
        name: String,
        description: String,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        productId: String,
        specialPrice: Double,
        quantityLimit: Int
    ): PromotionDto =
        PromotionFactory.createFlashSalePromotion(
            name, description, startDate, endDate, productId, specialPrice, quantityLimit
        ).let { promotion ->
            promotionRepository.save(promotion).also { saved ->
                domainEventApplicationService.publishEventsFromAggregate(saved)
            }
        }.toDto()

    @Transactional(readOnly = true)
    fun getActivePromotions(): List<PromotionDto> =
        promotionRepository.findActivePromotions().map { it.toDto() }

    @Transactional(readOnly = true)
    fun getPromotionsByType(type: PromotionType): List<PromotionDto> =
        promotionRepository.findByType(type).map { it.toDto() }

    @Transactional(readOnly = true)
    fun getPromotionById(promotionId: String): PromotionDto? =
        promotionRepository.findById(PromotionId.of(promotionId))?.toDto()

    @Transactional(readOnly = true)
    fun calculatePromotionDiscount(shoppingCart: ShoppingCart, promotionId: String): Money {
        val cartSummary = cartSummaryConverter.toCartSummary(shoppingCart)
        val promotion = promotionRepository.findById(PromotionId.of(promotionId))
            ?: throw PromotionNotFoundException("促銷活動不存在: $promotionId")

        return promotion.calculateDiscount(cartSummary)
    }

    @Transactional(readOnly = true)
    fun calculateTotalDiscount(shoppingCart: ShoppingCart): Money =
        cartSummaryConverter.toCartSummary(shoppingCart).let { cartSummary ->
            promotionRepository.findActivePromotions()
                .filter { it.isApplicable(cartSummary) }
                .map { it.calculateDiscount(cartSummary) }
                .fold(Money.twd(0), Money::add)
        }

    // Extension function for mapping
    private fun Promotion.toDto() = PromotionDto(
        id = id.value,
        name = name,
        description = description,
        type = type,
        status = status,
        startDate = validPeriod.startDate,
        endDate = validPeriod.endDate,
        usageLimit = usageLimit,
        usageCount = usageCount,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
```

**優化點**:
- ✅ 建構子參數自動成為 fields (消除 field declarations)
- ✅ Expression body functions (`fun xxx() = ...`)
- ✅ Scope functions (`let`, `also`) 簡化流程
- ✅ Extension functions (`Promotion.toDto()`) 消除 mapper 類
- ✅ Null safety (`findById()?.toDto()`)
- ✅ `fold()` 取代 reduce
- ✅ Elvis operator (`?:`) 簡化 null 處理

#### 7. Controller 優化
```kotlin
// Before (Java - 236 lines)
@RestController
@RequestMapping("/api/consumer/products")
@Tag(name = "消費者商品")
public class ConsumerProductController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> browseProducts(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String category
    ) {
        if (page < 0 || size <= 0) {
            return ResponseEntity.badRequest().build();
        }
        // ... 80 lines
    }
}

// After (Kotlin - 140 lines, 40% reduction)
@RestController
@RequestMapping("/api/consumer/products")
@Tag(name = "消費者商品", description = "商品瀏覽和搜索")
class ConsumerProductController {

    @GetMapping
    @Operation(summary = "瀏覽商品列表", description = "分頁瀏覽商品")
    fun browseProducts(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam category: String? = null,
        @RequestParam minPrice: BigDecimal? = null,
        @RequestParam maxPrice: BigDecimal? = null,
        @RequestParam minRating: Int? = null,
        @RequestParam sort: String? = null
    ): ResponseEntity<ProductPageResponse> {
        require(page >= 0 && size > 0) { "Invalid pagination parameters" }

        val products = createMockProducts()
            .applyFilters(category, minPrice, maxPrice, minRating)
            .sortBy(sort)
            .paginate(page, size)

        return ResponseEntity.ok(products)
    }

    @GetMapping("/search")
    @Operation(summary = "搜尋商品")
    fun searchProducts(
        @RequestParam keyword: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<ProductPageResponse> {
        require(keyword.isNotBlank()) { "Keyword cannot be empty" }

        val results = createMockProducts()
            .filter { it.name.contains(keyword, ignoreCase = true) }
            .paginate(page, size)

        return ResponseEntity.ok(results)
    }

    @GetMapping("/{productId}")
    @Operation(summary = "獲取商品詳情")
    fun getProductDetail(@PathVariable productId: String): ResponseEntity<ProductDto> =
        createMockProducts()
            .find { it.id == productId }
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()

    @GetMapping("/categories")
    @Operation(summary = "獲取商品分類")
    fun getProductCategories(): ResponseEntity<List<String>> =
        ResponseEntity.ok(listOf("ELECTRONICS", "CLOTHING", "BOOKS", "HOME", "SPORTS"))

    // Extension functions for cleaner code
    private fun List<ProductDto>.applyFilters(
        category: String?,
        minPrice: BigDecimal?,
        maxPrice: BigDecimal?,
        minRating: Int?
    ) = this
        .let { if (category != null) it.filter { p -> p.category == category } else it }
        .let { if (minPrice != null) it.filter { p -> p.price >= minPrice } else it }
        .let { if (maxPrice != null) it.filter { p -> p.price <= maxPrice } else it }
        .let { if (minRating != null) it.filter { p -> p.rating >= minRating } else it }

    private fun List<ProductDto>.paginate(page: Int, size: Int): ProductPageResponse {
        val start = page * size
        val end = minOf(start + size, this.size)

        return ProductPageResponse(
            content = subList(start, end),
            totalElements = size.toLong(),
            totalPages = (size + size - 1) / size,
            pageNumber = page,
            pageSize = size
        )
    }

    private fun createMockProducts() = listOf(
        ProductDto("PROD-001", "iPhone 15 Pro", "最新款iPhone", 35900.toBigDecimal(), "ELECTRONICS", true, 50),
        ProductDto("PROD-002", "MacBook Pro", "專業筆記型電腦", 58000.toBigDecimal(), "ELECTRONICS", true, 20),
        ProductDto("PROD-003", "AirPods Pro", "無線耳機", 8990.toBigDecimal(), "ELECTRONICS", true, 100)
    )
}

// Response data class
data class ProductPageResponse(
    val content: List<ProductDto>,
    val totalElements: Long,
    val totalPages: Int,
    val pageNumber: Int,
    val pageSize: Int
)
```

**優化點**:
- ✅ Nullable parameters (`category: String? = null`)
- ✅ Extension functions (`List<ProductDto>.applyFilters()`) 增強可讀性
- ✅ `let` chains 處理條件邏輯
- ✅ Expression body functions
- ✅ `find()`, `filter()` 取代 stream API
- ✅ `listOf()` 取代 Arrays.asList
- ✅ Named arguments 提升可讀性

#### 8. Enum → Sealed Class 優化
```kotlin
// Before (Java Enum)
public enum OrderStatus {
    CREATED, PENDING, CONFIRMED, PAID, SHIPPING, DELIVERED, CANCELLED;

    public boolean canTransitionTo(OrderStatus newStatus) {
        return switch (this) {
            case CREATED -> newStatus == PENDING;
            case PENDING -> newStatus == CONFIRMED || newStatus == CANCELLED;
            case CONFIRMED -> newStatus == PAID || newStatus == CANCELLED;
            // ...
        };
    }
}

// After (Kotlin Sealed Class - 更強大的狀態模型)
sealed class OrderStatus {
    abstract val displayName: String
    abstract fun canTransitionTo(newStatus: OrderStatus): Boolean

    data object Created : OrderStatus() {
        override val displayName = "已創建"
        override fun canTransitionTo(newStatus: OrderStatus) = newStatus is Pending
    }

    data object Pending : OrderStatus() {
        override val displayName = "待確認"
        override fun canTransitionTo(newStatus: OrderStatus) =
            newStatus is Confirmed || newStatus is Cancelled
    }

    data object Confirmed : OrderStatus() {
        override val displayName = "已確認"
        override fun canTransitionTo(newStatus: OrderStatus) =
            newStatus is Paid || newStatus is Cancelled
    }

    data class Paid(val paymentId: String, val paidAt: LocalDateTime) : OrderStatus() {
        override val displayName = "已付款"
        override fun canTransitionTo(newStatus: OrderStatus) =
            newStatus is Shipping || newStatus is Cancelled
    }

    data class Shipping(val trackingNumber: String) : OrderStatus() {
        override val displayName = "配送中"
        override fun canTransitionTo(newStatus: OrderStatus) = newStatus is Delivered
    }

    data class Delivered(val deliveredAt: LocalDateTime) : OrderStatus() {
        override val displayName = "已送達"
        override fun canTransitionTo(newStatus: OrderStatus) = false
    }

    data class Cancelled(val reason: String, val cancelledAt: LocalDateTime) : OrderStatus() {
        override val displayName = "已取消"
        override fun canTransitionTo(newStatus: OrderStatus) = false
    }
}
```

**優化點**:
- ✅ Sealed class 提供類型安全的狀態模型
- ✅ 每個狀態可攜帶不同數據 (如 Paid 的 paymentId)
- ✅ Exhaustive when expressions (編譯時檢查)
- ✅ data object 用於無狀態的狀態

---

## 📂 檔案對應表（關鍵類別）

### Domain Layer
| Java 檔案 | Kotlin 檔案 | 優化類型 |
|-----------|-------------|----------|
| `ProductId.java` (105 lines) | `ProductId.kt` (30 lines) | value class, -70% |
| `CustomerId.java` (105 lines) | `CustomerId.kt` (30 lines) | value class, -70% |
| `Order.java` (489 lines) | `Order.kt` (250 lines) | data class, -50% |
| `Money.java` (150 lines) | `Money.kt` (80 lines) | value class + operators, -47% |
| `OrderStatus.java` (enum) | `OrderStatus.kt` (sealed class) | 狀態模型優化 |

### Application Layer
| Java 檔案 | Kotlin 檔案 | 優化類型 |
|-----------|-------------|----------|
| `PromotionApplicationService.java` (193 lines) | `PromotionApplicationService.kt` (120 lines) | -38% |
| `PromotionDto.java` (record) | `PromotionDto.kt` (data class) | -20% |
| `OrderApplicationService.java` | `OrderApplicationService.kt` | Extension functions |

### Infrastructure Layer
| Java 檔案 | Kotlin 檔案 | 優化類型 |
|-----------|-------------|----------|
| `JpaProductReviewEntity.java` (179 lines) | `JpaProductReviewEntity.kt` (40 lines) | data class, -78% |
| `JpaPromotionRepository.java` | `JpaPromotionRepository.kt` | Interface unchanged |
| `OrderRepositoryImpl.java` (300+ lines) | `OrderRepositoryImpl.kt` (180 lines) | -40% |

### Interfaces Layer
| Java 檔案 | Kotlin 檔案 | 優化類型 |
|-----------|-------------|----------|
| `ConsumerProductController.java` (236 lines) | `ConsumerProductController.kt` (140 lines) | -40% |
| `OrderController.java` | `OrderController.kt` | Extension functions |

### Config Layer
| Java 檔案 | Kotlin 檔案 | 優化類型 |
|-----------|-------------|----------|
| `DomainServiceConfig.java` | `DomainServiceConfig.kt` | DSL configuration |
| `RedisConfiguration.java` | `RedisConfiguration.kt` | -30% |

**預估總代碼減少**: **40-50%** (從 694 個檔案約 100,000 行縮減至 50,000-60,000 行)

---

## ⚙️ Build 配置變更

### build.gradle → build.gradle.kts

完整的 Kotlin DSL 配置將在後續提供，包括：
- Kotlin JVM plugin + version 2.0.21
- kotlin-spring plugin (all-open, no-arg)
- kotlin-jpa plugin
- Kapt 配置 (for annotation processing)
- Kotlin compiler options (JVM target 21, -Xjsr305=strict)
- 保留所有現有依賴（Spring Boot, JPA, Redis, AWS SDK 等）

---

## 🔄 遷移階段規劃

### Phase 1: 基礎設施準備 (1-2 天)
1. ✅ 更新 `build.gradle` → `build.gradle.kts`
2. ✅ 配置 Kotlin plugins (kotlin-jvm, kotlin-spring, kotlin-jpa)
3. ✅ 配置 source sets (`src/main/kotlin`, `src/test/kotlin`)
4. ✅ 驗證編譯環境

### Phase 2: Shared Kernel 遷移 (2-3 天)
1. ✅ Value Objects (ProductId, CustomerId, Money, etc.)
2. ✅ Common Domain Types
3. ✅ 這些是最基礎的類型，優先遷移確保其他模組可依賴

### Phase 3: Domain Layer 遷移 (5-7 天)
按 Bounded Context 順序遷移：
1. **Order Domain** (核心領域)
   - Aggregates: Order
   - Events: OrderCreated, OrderSubmitted, etc.
   - Services: OrderDomainService
2. **Customer Domain**
3. **Product Domain**
4. **Inventory Domain**
5. ... (其他 10 個 contexts)

### Phase 4: Application Layer 遷移 (3-5 天)
1. Application Services
2. DTOs
3. Use Case handlers

### Phase 5: Infrastructure Layer 遷移 (4-6 天)
1. JPA Entities (使用 data class + all-open)
2. Repository Implementations
3. Adapters
4. Configurations

### Phase 6: Interfaces Layer 遷移 (2-3 天)
1. REST Controllers
2. API DTOs
3. Exception Handlers

### Phase 7: Tests 遷移 (5-7 天)
1. Unit Tests → Kotest
2. Integration Tests
3. Cucumber/BDD Tests (Kotlin support)

### Phase 8: 驗證與優化 (2-3 天)
1. 編譯驗證
2. 測試執行
3. 性能基準測試
4. 代碼審查

**總預估時間**: 25-35 工作天 (5-7 週)

---

## ⚠️ 潛在問題與解決方案

### 1. JPA Entity 可變性問題
**問題**: JPA 需要可變 entities (var properties)，但 data class 通常用 val
**解決方案**:
- 使用 `var` properties for JPA entities
- 使用 `kotlin-jpa` plugin (自動添加 no-arg constructor)
- 使用 `all-open` plugin (讓 class/methods non-final)

### 2. Spring AOP 代理問題
**問題**: Spring AOP 需要 non-final classes
**解決方案**:
- 使用 `kotlin-spring` plugin (自動 open @Service, @Repository, @Controller)

### 3. Lombok 移除
**問題**: Java 代碼大量使用 Lombok (@Data, @Builder, etc.)
**解決方案**:
- 移除 Lombok 依賴
- @Data → data class
- @Builder → named arguments
- @Slf4j → companion object logger

### 4. Null Safety 遷移
**問題**: Java 代碼沒有明確的 null safety
**解決方案**:
- 分析每個 field，決定 nullable (`?`) or non-null
- 使用 `@NotNull` annotations 作為參考
- Optional<T> → T?

### 5. Stream API → Kotlin Collections
**問題**: Java Stream API 與 Kotlin collections API 不同
**解決方案**:
- .stream().map() → .map()
- .stream().filter() → .filter()
- .collect(Collectors.toList()) → 直接返回 List
- .reduce() → .fold()

### 6. 編譯時間增加
**問題**: Kotlin 編譯可能較慢
**解決方案**:
- 使用 Gradle build cache
- 增加 compiler heap size
- 考慮使用 kapt → KSP (未來)

### 7. 與 Java 代碼混合編譯
**問題**: 過渡期間 Java/Kotlin 混合
**解決方案**:
- Gradle 支援 mixed source sets
- @JvmStatic, @JvmOverloads 確保 Java interop
- 逐步遷移，模組間可並存

---

## 📋 人工決策清單

以下項目需要架構師/技術負責人決策：

### 1. Sealed Class vs Enum
**決策點**: OrderStatus, PaymentStatus 等狀態類型
**選項**:
- A. 保持 enum (簡單，但擴展性差)
- B. 改為 sealed class (強大，可攜帶狀態)
**建議**: sealed class (現代 Kotlin 最佳實踐)

### 2. Value Class 使用範圍
**決策點**: 哪些 Value Objects 使用 value class (inline class)
**考量**: value class 有零開銷，但限制較多 (必須單一 property)
**建議**: ProductId, CustomerId, OrderId 等單一屬性的 ID 類型

### 3. Repository 模式
**決策點**: 是否重構 Repository pattern
**選項**:
- A. 保持 interface + implementation 分離
- B. 直接使用 Spring Data JPA 生成
**建議**: 保持現有架構，確保 Hexagonal Architecture

### 4. DTO Null Safety
**決策點**: API DTOs 的 null safety 策略
**選項**:
- A. 嚴格 non-null (require all fields)
- B. 寬鬆 nullable (允許 optional fields)
**建議**: 根據 API 規範，required fields 用 non-null，optional 用 nullable

### 5. Extension Functions vs Util Classes
**決策點**: 工具方法的組織方式
**建議**:
- Domain-specific utils → extension functions
- Generic utils → top-level functions in XxxUtils.kt

### 6. Coroutines 引入
**決策點**: 是否引入 Kotlin Coroutines 進行異步處理
**考量**: 當前使用 Spring WebMVC (blocking)，未來可遷移至 WebFlux
**建議**: Phase 1 不引入，保持與現有架構一致；Phase 2 評估 Coroutines + WebFlux

---

## 🎓 團隊學習資源

### Kotlin 學習路徑
1. **基礎語法** (1-2 週)
   - Kotlin Koans
   - Kotlin官方文檔
2. **Spring + Kotlin** (1-2 週)
   - Spring Boot with Kotlin guide
   - Kotlin Spring examples
3. **進階主題** (2-3 週)
   - Coroutines
   - DSL design
   - Advanced generics

### Code Review 檢查點
- [ ] 是否消除了不必要的 null checks
- [ ] 是否使用 data class 取代 POJO
- [ ] 是否使用 expression body functions
- [ ] 是否使用 scope functions 簡化代碼
- [ ] 是否避免過度使用 `!!` (force unwrap)
- [ ] 是否正確使用 sealed class
- [ ] 是否使用 extension functions 提升可讀性

---

## 📊 成功指標

### 定量指標
- ✅ 代碼行數減少 40-50%
- ✅ 編譯時間增加 <20%
- ✅ 測試覆蓋率維持 >80%
- ✅ 性能回歸測試通過率 100%
- ✅ 所有 CI/CD pipeline 綠燈

### 定性指標
- ✅ 代碼可讀性提升 (透過 code review 評估)
- ✅ Null safety 提升 (runtime NPE 減少)
- ✅ 維護成本降低 (樣板代碼減少)
- ✅ 團隊滿意度 (調查)

---

## 🚀 下一步行動

1. ✅ 審查本遷移計劃
2. ✅ 技術負責人決策上述「人工決策清單」
3. ✅ 開始 Phase 1: 更新 build.gradle.kts
4. ✅ 選擇 1-2 個 Bounded Context 作為 pilot（建議：Order + Customer）
5. ✅ 執行 pilot migration
6. ✅ Code review + 經驗總結
7. ✅ 全面遷移

---

**文件版本**: v1.0
**最後更新**: 2025-11-16
**負責人**: Claude AI Assistant
**審核者**: [待填寫]
