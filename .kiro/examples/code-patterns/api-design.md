# API 設計模式

## 概述

我們專案的 RESTful API 設計模式和最佳實踐。

**相關標準**: [Development Standards](../../steering/development-standards.md)

---

## RESTful URL 設計

### 資源命名

```java
// ✅ 好: RESTful 慣例
GET    /api/v1/customers                    // List customers
GET    /api/v1/customers/{id}               // Get customer
POST   /api/v1/customers                    // Create customer
PUT    /api/v1/customers/{id}               // Update customer (full)
PATCH  /api/v1/customers/{id}               // Update customer (partial)
DELETE /api/v1/customers/{id}               // Delete customer

// Nested resources
GET    /api/v1/customers/{id}/orders        // Get customer's orders
POST   /api/v1/customers/{id}/orders        // Create order for customer

// Actions (non-CRUD)
POST   /api/v1/orders/{id}/submit           // Submit order
POST   /api/v1/orders/{id}/cancel           // Cancel order
POST   /api/v1/orders/{id}/ship             // Ship order

// ❌ 壞: 非 RESTful
GET    /api/v1/getCustomer?id=123
POST   /api/v1/createCustomer
POST   /api/v1/customer/delete
```

---

## Request/Response 設計

### DTOs 和驗證

```java
// Request DTO
public record CreateCustomerRequest(
    @NotBlank String name,
    @Email String email,
    @Valid AddressDto address
) {}

// Response DTO
public record CustomerResponse(
    String id,
    String name,
    String email,
    AddressDto address,
    Instant createdAt,
    Instant updatedAt
) {
    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
            customer.getId().getValue(),
            customer.getName().getValue(),
            customer.getEmail().getValue(),
            AddressDto.from(customer.getAddress()),
            customer.getCreatedAt(),
            customer.getUpdatedAt()
        );
    }
}
```

### HTTP 狀態碼

```java
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    @GetMapping
    public ResponseEntity<Page<CustomerResponse>> listCustomers(Pageable pageable) {
        // 200 OK for successful GET
        return ResponseEntity.ok(customerService.findAll(pageable));
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request) {
        Customer customer = customerService.createCustomer(request);
        // 201 Created for successful POST
        return ResponseEntity
            .created(URI.create("/api/v1/customers/" + customer.getId()))
            .body(CustomerResponse.from(customer));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable String id,
            @Valid @RequestBody UpdateCustomerRequest request) {
        Customer customer = customerService.updateCustomer(id, request);
        // 200 OK for successful PUT
        return ResponseEntity.ok(CustomerResponse.from(customer));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable String id) {
        customerService.deleteCustomer(id);
        // 204 No Content for successful DELETE
        return ResponseEntity.noContent().build();
    }
}
```

---

## 分頁和過濾

```java
@RestController
public class ProductController {

    @GetMapping("/api/v1/products")
    public ResponseEntity<Page<ProductResponse>> searchProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {

        ProductSearchCriteria criteria = ProductSearchCriteria.builder()
            .category(category)
            .minPrice(minPrice)
            .maxPrice(maxPrice)
            .build();

        Page<Product> products = productService.search(criteria, pageable);
        Page<ProductResponse> response = products.map(ProductResponse::from);

        return ResponseEntity.ok(response);
    }
}

// Response includes pagination metadata
{
  "content": [...],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {"sorted": true, "unsorted": false}
  },
  "totalElements": 150,
  "totalPages": 8,
  "last": false,
  "first": true
}
```

---

## API 版本控制

```java
// URL versioning (recommended)
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerControllerV1 { }

@RestController
@RequestMapping("/api/v2/customers")
public class CustomerControllerV2 { }

// Header versioning (alternative)
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    @GetMapping(headers = "API-Version=1")
    public ResponseEntity<CustomerResponseV1> getCustomerV1(@PathVariable String id) { }

    @GetMapping(headers = "API-Version=2")
    public ResponseEntity<CustomerResponseV2> getCustomerV2(@PathVariable String id) { }
}
```

---

## 錯誤回應

```java
// Consistent error format
public record ErrorResponse(
    String errorCode,
    String message,
    Map<String, Object> context,
    List<FieldError> fieldErrors,
    Instant timestamp
) {}

// Example responses
// 400 Bad Request - Validation error
{
  "errorCode": "VALIDATION_ERROR",
  "message": "Invalid request data",
  "fieldErrors": [
    {"field": "email", "message": "Email format is invalid", "rejectedValue": "invalid-email"}
  ],
  "timestamp": "2025-01-17T10:30:00Z"
}

// 404 Not Found
{
  "errorCode": "RESOURCE_NOT_FOUND",
  "message": "Customer with id CUST-001 not found",
  "context": {"resourceType": "Customer", "resourceId": "CUST-001"},
  "timestamp": "2025-01-17T10:30:00Z"
}

// 409 Conflict - Business rule violation
{
  "errorCode": "BUSINESS_RULE_VIOLATION",
  "message": "Cannot submit empty order",
  "context": {"rule": "ORDER_EMPTY"},
  "timestamp": "2025-01-17T10:30:00Z"
}
```

---

## API 文件

```java
@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customer Management", description = "APIs for managing customers")
public class CustomerController {

    @Operation(
        summary = "Create a new customer",
        description = "Creates a new customer with the provided information"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Customer created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "409", description = "Email already exists")
    })
    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Customer creation request",
                required = true
            )
            @Valid @RequestBody CreateCustomerRequest request) {
        // Implementation
    }
}
```

---

## 總結

關鍵 API 設計原則：

1. **使用 RESTful 慣例**用於 URLs 和 HTTP 方法
2. **回傳適當的狀態碼**（200、201、204、400、404、409、500）
3. **使用 Bean Validation 驗證輸入**
4. **提供一致的錯誤回應**帶有錯誤代碼
5. **支援分頁和過濾**用於清單端點
6. **版本控制您的 APIs**以實現向後相容性
7. **使用 OpenAPI 記錄**（Swagger）

---

**相關文件**:
- [Development Standards](../../steering/development-standards.md)
- [Error Handling](error-handling.md)
- [Code Quality Checklist](../../steering/code-quality-checklist.md)
