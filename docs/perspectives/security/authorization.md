# Authorization

> **Last Updated**: 2025-10-23
> **Status**: ✅ Active

## 概述

本文件描述電子商務平台中使用的 authorization 機制。Authorization 決定已 authentication 的使用者可以做什麼 - 他們可以存取哪些資源以及可以執行哪些操作。系統實作具有細粒度權限的基於角色的存取控制（RBAC）。

## Authorization 模型

### 基於角色的存取控制 (RBAC)

系統使用 RBAC，其中：

- **使用者** 被指派一個或多個 **角色**
- **角色** 具有關聯的 **權限**
- **權限** 定義對特定資源和操作的存取

### 角色

#### ADMIN

- **描述**：具有完整存取權的系統管理員
- **權限**：所有資源上的所有操作
- **使用案例**：系統管理、使用者管理、配置

#### CUSTOMER

- **描述**：購買產品的一般客戶
- **權限**：
  - 讀取自己的個人資料和訂單
  - 建立訂單和評論
  - 更新自己的個人資料
  - 無法存取其他客戶的資料

#### SELLER

- **描述**：列出和管理產品的賣家
- **權限**：
  - 管理自己的產品和庫存
  - 查看自己的銷售和訂單
  - 無法存取其他賣家的資料

#### GUEST

- **描述**：未 authentication 的使用者
- **權限**：
  - 瀏覽產品
  - 查看公開內容
  - 無法下訂單或存取使用者資料

## 權限模型

### 權限格式

權限遵循格式：`resource:operation`

範例：

- `customer:read` - 讀取客戶資料
- `order:create` - 建立訂單
- `product:update` - 更新產品
- `admin:*` - 所有管理員操作

### 權限矩陣

| Resource | ADMIN | CUSTOMER | SELLER | GUEST |
|----------|-------|----------|--------|-------|
| **Customer Profile** |
| Read Any | ✅ | ❌ (own only) | ❌ | ❌ |
| Read Own | ✅ | ✅ | ✅ | ❌ |
| Create | ✅ | ✅ (registration) | ✅ (registration) | ✅ (registration) |
| Update Own | ✅ | ✅ | ✅ | ❌ |
| Delete | ✅ | ❌ | ❌ | ❌ |
| **Orders** |
| Read Any | ✅ | ❌ | ❌ | ❌ |
| Read Own | ✅ | ✅ | ❌ | ❌ |
| Create | ✅ | ✅ | ❌ | ❌ |
| Update | ✅ | ❌ | ❌ | ❌ |
| Cancel Own | ✅ | ✅ (if pending) | ❌ | ❌ |
| **Products** |
| Read | ✅ | ✅ | ✅ | ✅ |
| Create | ✅ | ❌ | ✅ | ❌ |
| Update Any | ✅ | ❌ | ❌ | ❌ |
| Update Own | ✅ | ❌ | ✅ | ❌ |
| Delete | ✅ | ❌ | ❌ | ❌ |
| **Reviews** |
| Read | ✅ | ✅ | ✅ | ✅ |
| Create | ✅ | ✅ (purchased only) | ❌ | ❌ |
| Update Own | ✅ | ✅ | ❌ | ❌ |
| Delete Any | ✅ | ❌ | ❌ | ❌ |
| Delete Own | ✅ | ✅ | ❌ | ❌ |

## 實作

### 方法級 Security

```java
@Configuration
@EnableGlobalMethodSecurity(
    prePostEnabled = true,
    securedEnabled = true,
    jsr250Enabled = true
)
public class MethodSecurityConfiguration {

    @Bean
    public PermissionEvaluator permissionEvaluator() {
        return new CustomPermissionEvaluator();
    }
}
```

### 使用 @PreAuthorize

```java
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    /**
     * Admin 可以存取任何客戶，使用者只能存取自己的資料
     */
    @GetMapping("/{customerId}")
    @PreAuthorize("hasRole('ADMIN') or #customerId == authentication.principal.customerId")
    public ResponseEntity<CustomerResponse> getCustomer(
            @PathVariable String customerId) {

        Customer customer = customerService.findById(customerId);
        return ResponseEntity.ok(CustomerResponse.from(customer));
    }

    /**
     * 只有管理員可以列出所有客戶
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<CustomerResponse>> listCustomers(
            Pageable pageable) {

        Page<Customer> customers = customerService.findAll(pageable);
        return ResponseEntity.ok(customers.map(CustomerResponse::from));
    }

    /**
     * 使用者可以更新自己的個人資料
     */
    @PutMapping("/{customerId}")
    @PreAuthorize("#customerId == authentication.principal.customerId")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable String customerId,
            @Valid @RequestBody UpdateCustomerRequest request) {

        Customer customer = customerService.updateCustomer(customerId, request);
        return ResponseEntity.ok(CustomerResponse.from(customer));
    }
}
```

### 自訂權限評估器

```java
@Component
public class CustomPermissionEvaluator implements PermissionEvaluator {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Override
    public boolean hasPermission(
            Authentication authentication,
            Object targetDomainObject,
            Object permission) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String permissionString = permission.toString();

        if (targetDomainObject instanceof Order order) {
            return evaluateOrderPermission(authentication, order, permissionString);
        }

        if (targetDomainObject instanceof Product product) {
            return evaluateProductPermission(authentication, product, permissionString);
        }

        return false;
    }

    @Override
    public boolean hasPermission(
            Authentication authentication,
            Serializable targetId,
            String targetType,
            Object permission) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return switch (targetType) {
            case "Order" -> evaluateOrderPermissionById(
                authentication, targetId.toString(), permission.toString()
            );
            case "Product" -> evaluateProductPermissionById(
                authentication, targetId.toString(), permission.toString()
            );
            default -> false;
        };
    }

    private boolean evaluateOrderPermission(
            Authentication auth,
            Order order,
            String permission) {

        String userId = auth.getName();

        // 管理員擁有所有權限
        if (hasRole(auth, "ADMIN")) {
            return true;
        }

        return switch (permission) {
            case "READ" -> order.getCustomerId().equals(userId);
            case "CANCEL" -> order.getCustomerId().equals(userId)
                && order.getStatus() == OrderStatus.PENDING;
            case "UPDATE", "DELETE" -> false; // 僅管理員
            default -> false;
        };
    }

    private boolean evaluateProductPermission(
            Authentication auth,
            Product product,
            String permission) {

        String userId = auth.getName();

        // 管理員擁有所有權限
        if (hasRole(auth, "ADMIN")) {
            return true;
        }

        // 賣家可以管理自己的產品
        if (hasRole(auth, "SELLER")) {
            return switch (permission) {
                case "READ" -> true;
                case "UPDATE", "DELETE" -> product.getSellerId().equals(userId);
                default -> false;
            };
        }

        // 客戶只能讀取
        return "READ".equals(permission);
    }

    private boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }
}
```

### 使用自訂權限

```java
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    /**
     * 檢查已載入領域物件的權限
     */
    @GetMapping("/{orderId}")
    @PreAuthorize("hasPermission(#orderId, 'Order', 'READ')")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId) {
        Order order = orderService.findById(orderId);
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    /**
     * 載入物件後檢查權限
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable String orderId) {
        Order order = orderService.findById(orderId);

        // 檢查實際物件的權限
        if (!hasPermission(order, "CANCEL")) {
            throw new AccessDeniedException("Cannot cancel this order");
        }

        orderService.cancelOrder(orderId);
        return ResponseEntity.noContent().build();
    }
}
```

### Service 級 Authorization

```java
@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final SecurityContext securityContext;

    /**
     * 在 service 級別強制執行 authorization
     */
    public Order findById(String orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        // 驗證使用者有權限存取此訂單
        String currentUserId = securityContext.getCurrentUserId();
        if (!order.getCustomerId().equals(currentUserId)
                && !securityContext.hasRole("ADMIN")) {
            throw new AccessDeniedException(
                "User does not have permission to access this order"
            );
        }

        return order;
    }

    /**
     * 根據使用者權限過濾結果
     */
    public Page<Order> findOrders(Pageable pageable) {
        String currentUserId = securityContext.getCurrentUserId();

        // 管理員查看所有訂單
        if (securityContext.hasRole("ADMIN")) {
            return orderRepository.findAll(pageable);
        }

        // 客戶只查看自己的訂單
        return orderRepository.findByCustomerId(currentUserId, pageable);
    }
}
```

## 資料過濾

### 行級 Security

```java
@Component
public class DataFilteringAspect {

    /**
     * 根據使用者權限自動過濾查詢結果
     */
    @Around("@annotation(FilterByUser)")
    public Object filterResults(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();

        if (result instanceof List<?> list) {
            return filterList(list);
        }

        if (result instanceof Page<?> page) {
            return filterPage(page);
        }

        return result;
    }

    private List<?> filterList(List<?> list) {
        String currentUserId = SecurityContextHolder.getContext()
            .getAuthentication().getName();

        return list.stream()
            .filter(item -> canAccess(item, currentUserId))
            .collect(Collectors.toList());
    }

    private boolean canAccess(Object item, String userId) {
        if (item instanceof OwnedByUser ownedItem) {
            return ownedItem.getOwnerId().equals(userId);
        }
        return true;
    }
}
```

### 欄位級 Security

```java
@Component
public class FieldMaskingService {

    /**
     * 根據使用者角色遮罩敏感欄位
     */
    public CustomerDto maskSensitiveFields(Customer customer, String userRole) {
        CustomerDto dto = CustomerDto.from(customer);

        // 非管理員無法查看完整 email 和電話
        if (!"ADMIN".equals(userRole)) {
            dto.setEmail(maskEmail(dto.getEmail()));
            dto.setPhoneNumber(maskPhoneNumber(dto.getPhoneNumber()));
        }

        return dto;
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }

        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];

        if (localPart.length() <= 2) {
            return "*".repeat(localPart.length()) + "@" + domain;
        }

        return localPart.charAt(0)
            + "*".repeat(localPart.length() - 2)
            + localPart.charAt(localPart.length() - 1)
            + "@" + domain;
    }
}
```

## Authorization 模式

### 資源所有權

```java
public interface OwnedResource {
    String getOwnerId();

    default boolean isOwnedBy(String userId) {
        return getOwnerId().equals(userId);
    }
}

@Entity
public class Order implements OwnedResource {

    @Column(name = "customer_id")
    private String customerId;

    @Override
    public String getOwnerId() {
        return customerId;
    }
}
```

### 階層式權限

```java
public enum Permission {
    // Admin 權限（最高級別）
    ADMIN_ALL("admin:*"),

    // 客戶管理
    CUSTOMER_READ_ALL("customer:read:all"),
    CUSTOMER_READ_OWN("customer:read:own"),
    CUSTOMER_UPDATE_OWN("customer:update:own"),

    // 訂單管理
    ORDER_READ_ALL("order:read:all"),
    ORDER_READ_OWN("order:read:own"),
    ORDER_CREATE("order:create"),
    ORDER_CANCEL_OWN("order:cancel:own"),

    // 產品管理
    PRODUCT_READ("product:read"),
    PRODUCT_CREATE("product:create"),
    PRODUCT_UPDATE_OWN("product:update:own"),
    PRODUCT_DELETE_OWN("product:delete:own");

    private final String permission;

    Permission(String permission) {
        this.permission = permission;
    }

    public boolean implies(Permission other) {
        // ADMIN_ALL 包含所有其他權限
        if (this == ADMIN_ALL) {
            return true;
        }

        // 檢查此權限是否包含其他權限
        String thisBase = permission.split(":")[0];
        String otherBase = other.permission.split(":")[0];

        return thisBase.equals(otherBase) && permission.endsWith("*");
    }
}
```

## Security Context

### 存取目前使用者

```java
@Component
public class SecurityContextService {

    /**
     * 取得目前已 authentication 的使用者 ID
     */
    public String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext()
            .getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationException("User not authenticated");
        }

        return authentication.getName();
    }

    /**
     * 取得目前使用者詳細資訊
     */
    public UserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext()
            .getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationException("User not authenticated");
        }

        return (UserDetails) authentication.getPrincipal();
    }

    /**
     * 檢查目前使用者是否具有角色
     */
    public boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext()
            .getAuthentication();

        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    /**
     * 檢查目前使用者是否具有權限
     */
    public boolean hasPermission(String permission) {
        Authentication authentication = SecurityContextHolder.getContext()
            .getAuthentication();

        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals(permission));
    }
}
```

## 錯誤處理

### Authorization 例外

```java
@RestControllerAdvice
public class AuthorizationExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex) {

        ErrorResponse error = ErrorResponse.builder()
            .errorCode("ACCESS_DENIED")
            .message("You do not have permission to access this resource")
            .timestamp(Instant.now())
            .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(InsufficientAuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientAuthentication(
            InsufficientAuthenticationException ex) {

        ErrorResponse error = ErrorResponse.builder()
            .errorCode("AUTHENTICATION_REQUIRED")
            .message("Authentication is required to access this resource")
            .timestamp(Instant.now())
            .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }
}
```

## 測試

### Authorization 測試

```java
@SpringBootTest
@AutoConfigureMockMvc
class AuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "user-123", roles = "CUSTOMER")
    void customer_can_access_own_data() throws Exception {
        mockMvc.perform(get("/api/v1/customers/user-123"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user-123", roles = "CUSTOMER")
    void customer_cannot_access_other_customer_data() throws Exception {
        mockMvc.perform(get("/api/v1/customers/user-456"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin-1", roles = "ADMIN")
    void admin_can_access_any_customer_data() throws Exception {
        mockMvc.perform(get("/api/v1/customers/user-123"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user-123", roles = "CUSTOMER")
    void customer_can_create_order() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "items": [{"productId": "prod-1", "quantity": 2}]
                    }
                    """))
            .andExpect(status().isCreated());
    }

    @Test
    @WithAnonymousUser
    void anonymous_user_cannot_create_order() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "items": [{"productId": "prod-1", "quantity": 2}]
                    }
                    """))
            .andExpect(status().isUnauthorized());
    }
}
```

## 監控和日誌記錄

### 要記錄的 Authorization 事件

```java
@Component
public class AuthorizationEventLogger {

    private final Logger logger = LoggerFactory.getLogger("SECURITY");

    @EventListener
    public void handleAuthorizationFailure(
            AuthorizationFailureEvent event) {

        logger.warn("Authorization failure",
            kv("event", "AUTHZ_FAILURE"),
            kv("userId", event.getUserId()),
            kv("resource", event.getResource()),
            kv("action", event.getAction()),
            kv("reason", event.getReason()),
            kv("timestamp", Instant.now()));
    }

    @EventListener
    public void handleAccessDenied(AccessDeniedEvent event) {
        logger.warn("Access denied",
            kv("event", "ACCESS_DENIED"),
            kv("userId", event.getUserId()),
            kv("requestUri", event.getRequestUri()),
            kv("method", event.getHttpMethod()),
            kv("timestamp", Instant.now()));
    }
}
```

## 最佳實踐

### ✅ 應該

- 始終在多個層級檢查 authorization（controller、service、data）
- 使用方法級 security 註解以提高清晰度
- 為複雜邏輯實作自訂權限評估器
- 記錄所有 authorization 失敗以供 security 監控
- 徹底測試 authorization 規則
- 使用最小權限原則
- 根據使用者權限過濾資料

### ❌ 不應該

- 不要僅依賴客戶端 authorization
- 不要在錯誤訊息中暴露未授權的資料
- 不要僅在 controller 中實作 authorization 邏輯
- 不要忘記檢查所有端點的 authorization
- 不要到處使用硬編碼的角色檢查
- 不要跳過 authorization 測試

## 相關文件

- [Authentication](authentication.md) - Authentication 機制
- [Security Overview](overview.md) - 整體 security 觀點
- [Security Standards](../../.kiro/steering/security-standards.md) - 詳細的 security 標準

## 參考資料

- Spring Security Authorization: <https://docs.spring.io/spring-security/reference/servlet/authorization/index.html>
- OWASP Authorization Cheat Sheet: <https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html>
- RBAC: <https://en.wikipedia.org/wiki/Role-based_access_control>
