---
adr_number: 009
title: "RESTful API Design 與 OpenAPI 3.0"
date: 2025-10-24
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [002, 003]
affected_viewpoints: ["functional", "development"]
affected_perspectives: ["evolution", "accessibility"]
---

# ADR-009: RESTful API Design 與 OpenAPI 3.0

## 狀態

**Accepted** - 2025-10-24

## 上下文

### 問題陳述

The Enterprise E-Commerce Platform 需要well-defined API strategy：

- 提供s consistent, intuitive interfaces 用於 clients
- 支援s multiple client types (web, mobile, third-party integrations)
- 啟用s API versioning 和 evolution
- 提供s comprehensive API documentation
- 支援s API testing 和 validation
- Follows industry best practices
- 啟用s API-first development
- 支援s contract testing

### 業務上下文

**業務驅動因素**：

- 需要 web 和 mobile client applications
- Future requirement 用於 third-party integrations
- API marketplace potential
- Developer experience 用於 internal 和 external developers
- Rapid feature development 沒有 breaking clients
- Compliance 與 industry standards

**限制條件**：

- Team has REST API experience
- Spring Boot framework (ADR-002)
- 需要 backward compatibility
- 必須 支援 API versioning
- 預算: No additional API gateway costs initially

### 技術上下文

**目前狀態**：

- Spring Boot 3.4.5 + Java 21
- Hexagonal Architecture (ADR-002)
- Multiple bounded contexts
- Event-driven architecture (ADR-003)
- Next.js 和 Angular frontends

**需求**：

- RESTful API design
- API documentation generation
- Request/response validation
- Error handling standards
- API versioning strategy
- Authentication 和 authorization
- Rate limiting 支援
- CORS configuration

## 決策驅動因素

1. **Industry Standards**: Follow widely-adopted REST principles
2. **Documentation**: Auto-generate comprehensive API docs
3. **Developer Experience**: 容易understand 和 use
4. **Versioning**: 支援 API evolution 沒有 breaking clients
5. **Validation**: Automatic request/response validation
6. **Testing**: 啟用 contract testing
7. **Tooling**: 良好的 IDE 和 testing tool 支援
8. **Team Skills**: Leverage existing REST API knowledge

## 考慮的選項

### 選項 1： RESTful API with OpenAPI 3.0 (SpringDoc)

**描述**： REST API following OpenAPI 3.0 specification with SpringDoc for documentation

**優點**：

- ✅ Industry-standard REST principles
- ✅ OpenAPI 3.0 widely 支援ed
- ✅ SpringDoc auto-generates documentation from code
- ✅ Swagger UI 用於 interactive testing
- ✅ Strong tooling ecosystem
- ✅ Team has REST experience
- ✅ 支援s API versioning
- ✅ Contract-first 或 code-first approach
- ✅ Free 和 open source

**缺點**：

- ⚠️ REST 可以 be verbose 用於 複雜的 operations
- ⚠️ Need to 維持 API versioning discipline
- ⚠️ Over-fetching/under-fetching possible

**成本**： $0 (open source)

**風險**： **Low** - Proven, widely adopted

### 選項 2： GraphQL

**描述**： GraphQL API with schema-first design

**優點**：

- ✅ Flexible querying (no over/under-fetching)
- ✅ Strong typing
- ✅ Single endpoint
- ✅ Real-time subscriptions
- ✅ Introspection

**缺點**：

- ❌ Team lacks GraphQL experience
- ❌ More 複雜的 to implement
- ❌ Caching more difficult
- ❌ N+1 query problems
- ❌ 更難version
- ❌ Security concerns (query 複雜的ity)

**成本**： $0 (open source)

**風險**： **Medium** - Learning curve, complexity

### 選項 3： gRPC

**描述**： gRPC with Protocol Buffers

**優點**：

- ✅ High performance (binary protocol)
- ✅ Strong typing
- ✅ Bi-directional streaming
- ✅ Code generation

**缺點**：

- ❌ Not browser-friendly (needs gRPC-Web)
- ❌ Team lacks gRPC experience
- ❌ Limited tooling 用於 debugging
- ❌ 更難test manually
- ❌ Not suitable 用於 public APIs

**成本**： $0 (open source)

**風險**： **High** - Not suitable for web/mobile clients

### 選項 4： REST without OpenAPI

**描述**： REST API without formal specification

**優點**：

- ✅ 簡單start
- ✅ Flexible

**缺點**：

- ❌ No auto-generated documentation
- ❌ Manual documentation maintenance
- ❌ No contract testing
- ❌ Inconsistent API design
- ❌ Poor developer experience

**成本**： $0

**風險**： **High** - Poor maintainability

## 決策結果

**選擇的選項**： **RESTful API with OpenAPI 3.0 (SpringDoc)**

### 理由

RESTful API 與 OpenAPI 3.0被選擇的原因如下：

1. **Industry Standard**: REST is widely understood 和 adopted
2. **Team Experience**: Team already knows REST principles
3. **OpenAPI Ecosystem**: 優秀的 tooling 用於 documentation, testing, 和 code generation
4. **SpringDoc Integration**: 無縫的Spring Boot整合 與 auto-generated docs
5. **Swagger UI**: Interactive API documentation 和 testing
6. **Versioning 支援**: URL-based versioning strategy
7. **Client 支援**: Works 與 all client types (web, mobile, third-party)
8. **Cost-Effective**: Free 和 open source

**實作策略**：

**API Design Principles**:

- RESTful resource-based URLs
- Standard HTTP methods (GET, POST, PUT, DELETE, PATCH)
- Consistent response formats
- Proper HTTP status codes
- HATEOAS 用於 discoverability (optional)

**OpenAPI Documentation**:

- SpringDoc annotations on controllers
- Auto-generated OpenAPI 3.0 specification
- Swagger UI 用於 interactive testing
- API documentation versioned 與 code

**Versioning Strategy**:

- URL-based versioning: `/api/v1/`, `/api/v2/`
- 維持 backward compatibility 用於 at least 2 versions
- Deprecation headers 用於 old versions

**為何不選 GraphQL**： While GraphQL offers flexibility, the team lacks experience 和 REST meets all current requirements. 可以 add GraphQL later if needed.

**為何不選 gRPC**： Not suitable 用於 browser-based clients. 更好的 用於 internal service-to-service communication, which we 處理 與 domain events.

## 影響分析

### 利害關係人影響

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Frontend Developers | High | Primary API consumers | Clear documentation, examples, Swagger UI |
| Mobile Developers | High | API consumers | SDK generation from OpenAPI spec |
| Third-Party Developers | Medium | Future API consumers | Public API documentation, sandbox environment |
| Backend Developers | Medium | API implementers | SpringDoc annotations, examples |
| QA Team | Medium | API testing | Postman collections, contract tests |

### 影響半徑

**選擇的影響半徑**： **System**

影響：

- All client applications
- API documentation
- Testing strategy
- Deployment process
- Monitoring 和 logging
- Security implementation

### 風險評估

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| Breaking API changes | Medium | High | Versioning strategy, deprecation policy |
| Inconsistent API design | Medium | Medium | API design guidelines, code reviews |
| Documentation drift | Medium | Medium | Auto-generation from code, CI/CD checks |
| Over-fetching data | Low | Low | Optimize endpoints, consider GraphQL later |
| API security issues | Low | High | Security best practices, regular audits |

**整體風險等級**： **Low**

## 實作計畫

### 第 1 階段： Setup and Standards （第 1 週）

- [x] Add SpringDoc dependency

  ```xml
  <dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
  </dependency>
  ```

- [x] Configure SpringDoc

  ```java
  @Configuration
  public class OpenApiConfiguration {
      @Bean
      public OpenAPI customOpenAPI() {
          return new OpenAPI()
              .info(new Info()
                  .title("E-Commerce Platform API")
                  .version("v1")
                  .description("Enterprise E-Commerce Platform REST API")
                  .contact(new Contact()
                      .name("API Support")
                      .email("api@ecommerce.com"))
                  .license(new License()
                      .name("Apache 2.0")
                      .url("https://www.apache.org/licenses/LICENSE-2.0")))
              .servers(List.of(
                  new Server().url("https://api.ecommerce.com").description("Production"),
                  new Server().url("https://api-staging.ecommerce.com").description("Staging"),
                  new Server().url("http://localhost:8080").description("Local")
              ));
      }
  }
  ```

- [x] Create API design guidelines document
- [x] Define standard response formats

### 第 2 階段： API Design Patterns （第 1-2 週）

- [ ] Implement standard response wrapper

  ```java
  public record ApiResponse<T>(
      T data,
      ApiMetadata metadata,
      List<ApiError> errors
  ) {
      public static <T> ApiResponse<T> success(T data) {
          return new ApiResponse<>(data, ApiMetadata.create(), null);
      }
      
      public static <T> ApiResponse<T> error(List<ApiError> errors) {
          return new ApiResponse<>(null, ApiMetadata.create(), errors);
      }
  }
  
  public record ApiMetadata(
      String requestId,
      LocalDateTime timestamp,
      String version
  ) {
      public static ApiMetadata create() {
          return new ApiMetadata(
              UUID.randomUUID().toString(),
              LocalDateTime.now(),
              "v1"
          );
      }
  }
  ```

- [ ] Implement error response format

  ```java
  public record ApiError(
      String code,
      String message,
      String field,
      Object rejectedValue
  ) {}
  
  @RestControllerAdvice
  public class GlobalExceptionHandler {
      @ExceptionHandler(MethodArgumentNotValidException.class)
      public ResponseEntity<ApiResponse<Void>> handleValidation(
          MethodArgumentNotValidException ex) {
          
          List<ApiError> errors = ex.getBindingResult()
              .getFieldErrors()
              .stream()
              .map(error -> new ApiError(
                  "VALIDATION_ERROR",
                  error.getDefaultMessage(),
                  error.getField(),
                  error.getRejectedValue()
              ))
              .toList();
          
          return ResponseEntity
              .badRequest()
              .body(ApiResponse.error(errors));
      }
  }
  ```

- [ ] Create pagination 支援

  ```java
  public record PageResponse<T>(
      List<T> content,
      PageMetadata page
  ) {}
  
  public record PageMetadata(
      int number,
      int size,
      long totalElements,
      int totalPages,
      boolean first,
      boolean last
  ) {
      public static PageMetadata from(Page<?> page) {
          return new PageMetadata(
              page.getNumber(),
              page.getSize(),
              page.getTotalElements(),
              page.getTotalPages(),
              page.isFirst(),
              page.isLast()
          );
      }
  }
  ```

### 第 3 階段： Customer API Implementation （第 2-3 週）

- [ ] Implement Customer API endpoints

  ```java
  @RestController
  @RequestMapping("/api/v1/customers")
  @Tag(name = "Customer", description = "Customer management APIs")
  public class CustomerController {
      
      @Operation(
          summary = "Get customer by ID",
          description = "Returns a single customer by their unique identifier"
      )
      @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Customer found"),
          @ApiResponse(responseCode = "404", description = "Customer not found"),
          @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
      @GetMapping("/{id}")
      public ResponseEntity<ApiResponse<CustomerResponse>> getCustomer(
          @Parameter(description = "Customer ID", required = true)
          @PathVariable String id) {
          
          Customer customer = customerService.findById(id);
          CustomerResponse response = CustomerResponse.from(customer);
          return ResponseEntity.ok(ApiResponse.success(response));
      }
      
      @Operation(summary = "Create new customer")
      @PostMapping
      public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(
          @Valid @RequestBody CreateCustomerRequest request) {
          
          Customer customer = customerService.create(request);
          CustomerResponse response = CustomerResponse.from(customer);
          return ResponseEntity
              .status(HttpStatus.CREATED)
              .body(ApiResponse.success(response));
      }
      
      @Operation(summary = "Update customer")
      @PutMapping("/{id}")
      public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(
          @PathVariable String id,
          @Valid @RequestBody UpdateCustomerRequest request) {
          
          Customer customer = customerService.update(id, request);
          CustomerResponse response = CustomerResponse.from(customer);
          return ResponseEntity.ok(ApiResponse.success(response));
      }
      
      @Operation(summary = "Delete customer")
      @DeleteMapping("/{id}")
      public ResponseEntity<Void> deleteCustomer(@PathVariable String id) {
          customerService.delete(id);
          return ResponseEntity.noContent().build();
      }
      
      @Operation(summary = "List customers with pagination")
      @GetMapping
      public ResponseEntity<ApiResponse<PageResponse<CustomerResponse>>> listCustomers(
          @Parameter(description = "Page number (0-based)")
          @RequestParam(defaultValue = "0") int page,
          @Parameter(description = "Page size")
          @RequestParam(defaultValue = "20") int size,
          @Parameter(description = "Sort field")
          @RequestParam(defaultValue = "createdAt") String sort) {
          
          Page<Customer> customers = customerService.findAll(
              PageRequest.of(page, size, Sort.by(sort).descending())
          );
          
          PageResponse<CustomerResponse> response = new PageResponse<>(
              customers.getContent().stream()
                  .map(CustomerResponse::from)
                  .toList(),
              PageMetadata.from(customers)
          );
          
          return ResponseEntity.ok(ApiResponse.success(response));
      }
  }
  ```

- [ ] Add request/response DTOs 與 validation

  ```java
  public record CreateCustomerRequest(
      @NotBlank(message = "Name is required")
      @Size(min = 2, max = 100)
      String name,
      
      @NotBlank(message = "Email is required")
      @Email(message = "Invalid email format")
      String email,
      
      @NotBlank(message = "Password is required")
      @Size(min = 8, max = 128)
      @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
               message = "Password must contain uppercase, lowercase, and number")
      String password,
      
      @Valid
      @NotNull(message = "Address is required")
      AddressDto address
  ) {}
  
  @Schema(description = "Customer response")
  public record CustomerResponse(
      @Schema(description = "Customer unique identifier")
      String id,
      
      @Schema(description = "Customer name")
      String name,
      
      @Schema(description = "Customer email")
      String email,
      
      @Schema(description = "Customer address")
      AddressDto address,
      
      @Schema(description = "Membership level")
      String membershipLevel,
      
      @Schema(description = "Creation timestamp")
      LocalDateTime createdAt,
      
      @Schema(description = "Last update timestamp")
      LocalDateTime updatedAt
  ) {
      public static CustomerResponse from(Customer customer) {
          return new CustomerResponse(
              customer.getId().getValue(),
              customer.getName().getValue(),
              customer.getEmail().getValue(),
              AddressDto.from(customer.getAddress()),
              customer.getMembershipLevel().name(),
              customer.getCreatedAt(),
              customer.getUpdatedAt()
          );
      }
  }
  ```

### 第 4 階段： Remaining APIs （第 3-6 週）

- [ ] Implement Order API
- [ ] Implement Product API
- [ ] Implement Shopping Cart API
- [ ] Implement Payment API
- [ ] Implement remaining bounded context APIs

### Phase 5: API Versioning （第 6-7 週）

- [ ] Implement versioning strategy

  ```java
  @RestController
  @RequestMapping("/api/v1/customers")
  public class CustomerV1Controller {
      // V1 implementation
  }
  
  @RestController
  @RequestMapping("/api/v2/customers")
  public class CustomerV2Controller {
      // V2 implementation with breaking changes
  }
  ```

- [ ] Add deprecation headers

  ```java
  @GetMapping("/{id}")
  public ResponseEntity<CustomerResponse> getCustomer(@PathVariable String id) {
      return ResponseEntity.ok()
          .header("Deprecation", "true")
          .header("Sunset", "2026-12-31T23:59:59Z")
          .header("Link", "</api/v2/customers>; rel=\"successor-version\"")
          .body(response);
  }
  ```

### Phase 6: Testing and Documentation （第 7-8 週）

- [ ] Generate OpenAPI specification

  ```bash
  # Access at http://localhost:8080/v3/api-docs
  # Swagger UI at http://localhost:8080/swagger-ui.html
  ```

- [ ] Create Postman collection from OpenAPI spec
- [ ] Implement contract tests

  ```java
  @SpringBootTest(webEnvironment = RANDOM_PORT)
  class CustomerApiContractTest {
      @Test
      void should_match_openapi_specification() {
          // Validate API responses against OpenAPI spec
      }
  }
  ```

- [ ] Create API usage examples
- [ ] Document authentication flow

### 回滾策略

**觸發條件**：

- API design inconsistencies causing client issues
- OpenAPI documentation drift > 20%
- Team unable to 維持 API standards
- Performance issues 與 SpringDoc

**回滾步驟**：

1. Remove SpringDoc dependency
2. Create manual API documentation
3. Simplify API design
4. Re-evaluate API strategy

**回滾時間**： 1 week

## 監控和成功標準

### 成功指標

- ✅ 100% of endpoints documented in OpenAPI
- ✅ API 回應時間 < 2 seconds (95th percentile)
- ✅ API error rate < 1%
- ✅ Zero breaking changes 沒有 version bump
- ✅ API documentation accuracy > 95%
- ✅ Developer satisfaction > 4/5

### 監控計畫

**API Metrics**:

- Request rate per endpoint
- Response time per endpoint
- Error rate per endpoint
- API version usage distribution

**Documentation Metrics**:

- OpenAPI spec generation success
- Documentation page views
- API usage examples accessed

**審查時程**：

- Weekly: API design review
- Monthly: API versioning review
- Quarterly: API strategy review

## 後果

### 正面後果

- ✅ **Consistent API Design**: RESTful principles ensure consistency
- ✅ **Auto-Generated Docs**: SpringDoc generates docs from code
- ✅ **Interactive Testing**: Swagger UI 啟用s easy API testing
- ✅ **Contract Testing**: OpenAPI spec 啟用s contract tests
- ✅ **Client Generation**: 可以 generate client SDKs from spec
- ✅ **Versioning 支援**: URL-based versioning is straightforward
- ✅ **Industry Standard**: REST 和 OpenAPI are widely adopted
- ✅ **Team Familiarity**: Team already knows REST

### 負面後果

- ⚠️ **Verbosity**: REST 可以 be verbose 用於 複雜的 operations
- ⚠️ **Over/Under-Fetching**: May need multiple requests 或 get extra data
- ⚠️ **Versioning Discipline**: Need to 維持 backward compatibility
- ⚠️ **Documentation Maintenance**: Need to keep annotations up to date

### 技術債務

**已識別債務**：

1. No HATEOAS implementation yet (acceptable 用於 MVP)
2. Limited API 速率限制 (future enhancement)
3. No API gateway yet (future requirement)
4. Manual Postman collection creation (可以 be automated)

**債務償還計畫**：

- **Q1 2026**: Implement API 速率限制
- **Q2 2026**: Add HATEOAS 用於 discoverability
- **Q3 2026**: Evaluate API gateway (Kong, AWS API Gateway)
- **Q4 2026**: Automate client SDK generation

## 相關決策

- [ADR-002: Adopt Hexagonal Architecture](002-adopt-hexagonal-architecture.md) - API in interfaces layer
- [ADR-003: Use Domain Events 用於 Cross-Context Communication](003-use-domain-events-for-cross-context-communication.md) - Internal vs external communication

## 備註

### API Design Guidelines

**URL Naming**:

- Use nouns, not verbs: `/customers` not `/getCustomers`
- Use plural nouns: `/customers` not `/customer`
- Use kebab-case: `/order-items` not `/orderItems`
- Nest resources: `/customers/{id}/orders`

**HTTP Methods**:

- GET: Retrieve resource(s)
- POST: Create new resource
- PUT: Update entire resource
- PATCH: Partial update
- DELETE: Remove resource

**HTTP Status Codes**:

- 200 OK: Successful GET, PUT, PATCH
- 201 Created: Successful POST
- 204 No Content: Successful DELETE
- 400 Bad Request: Validation error
- 401 Unauthorized: Authentication required
- 403 Forbidden: Authorization failed
- 404 Not Found: Resource not found
- 409 Conflict: Business rule violation
- 500 Internal Server Error: System error

### OpenAPI Annotations

```java
@Tag(name = "Customer", description = "Customer management APIs")
@Operation(summary = "Get customer", description = "Returns customer by ID")
@ApiResponse(responseCode = "200", description = "Success")
@Parameter(description = "Customer ID", required = true)
@Schema(description = "Customer data")
@Valid
```

### Sample OpenAPI Specification

```yaml
openapi: 3.0.1
info:
  title: E-Commerce Platform API
  version: v1
  description: Enterprise E-Commerce Platform REST API
paths:
  /api/v1/customers:
    get:
      tags:

        - Customer

      summary: List customers
      parameters:

        - name: page

          in: query
          schema:
            type: integer
            default: 0

        - name: size

          in: query
          schema:
            type: integer
            default: 20
      responses:
        '200':
          description: Success
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PageResponseCustomerResponse'
```

---

**文檔狀態**： ✅ Accepted  
**上次審查**： 2025-10-24  
**下次審查**： 2026-01-24 （每季）
