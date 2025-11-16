# Domain Events 設計與實作指南

本文件提供我們 DDD + Hexagonal Architecture 專案中 Domain Event 設計與實作的完整指南。

## 核心原則

### 1. Event Publishing 職責

- **Aggregate Roots** 負責在業務操作期間收集 domain events
- **Application Services** 負責發布收集的 events
- **Infrastructure Layer** 處理 event delivery 的技術面向

### 2. Transaction Boundary 管理

- Events 在業務邏輯執行期間收集，但不會立即發布
- Events 使用 `@TransactionalEventListener` 在 transaction 邊界內發布
- Events 僅在成功發布後標記為已提交

### 3. Event 不可變性

- 所有 domain events 必須實作為不可變的 Java Records
- Events 應包含所有 event handlers 獨立處理所需的資料
- Events 不應包含可變物件的參考

## 實作標準

### Event 定義

```java
// Domain Event as Record - following project style
public record CustomerCreatedEvent(
    CustomerId customerId,
    CustomerName customerName,
    Email email,
    MembershipLevel membershipLevel,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent {

    /**
     * Factory method with automatic eventId and occurredOn generation
     */
    public static CustomerCreatedEvent create(
        CustomerId customerId,
        CustomerName customerName,
        Email email,
        MembershipLevel membershipLevel
    ) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new CustomerCreatedEvent(
            customerId, customerName, email, membershipLevel,
            metadata.eventId(), metadata.occurredOn()
        );
    }

    @Override
    public UUID getEventId() { return eventId; }

    @Override
    public LocalDateTime getOccurredOn() { return occurredOn; }

    @Override
    public String getEventType() {
        return DomainEvent.getEventTypeFromClass(this.getClass());
    }

    @Override
    public String getAggregateId() { return customerId.getValue(); }
}
```

### Aggregate Roots 中的 Event 收集

```java
@AggregateRoot(name = "Customer", description = "客戶聚合根", boundedContext = "Customer", version = "2.0")
public class Customer implements AggregateRootInterface {

    public void updateProfile(CustomerName newName, Email newEmail, Phone newPhone) {
        // 1. Execute business logic first
        validateProfileUpdate(newName, newEmail, newPhone);

        // 2. Update state
        this.name = newName;
        this.email = newEmail;
        this.phone = newPhone;

        // 3. Collect domain event
        collectEvent(CustomerProfileUpdatedEvent.create(this.id, newName, newEmail, newPhone));
    }

    // Event management methods are provided by AggregateRootInterface:
    // - collectEvent(DomainEvent event)
    // - getUncommittedEvents()
    // - markEventsAsCommitted()
    // - hasUncommittedEvents()
}
```

### Application Services 中的 Event 發布

```java
@Service
@Transactional
public class CustomerApplicationService {
    private final CustomerRepository customerRepository;
    private final DomainEventApplicationService domainEventService;

    public void updateCustomerProfile(UpdateProfileCommand command) {
        // 1. Load aggregate
        Customer customer = customerRepository.findById(command.customerId())
            .orElseThrow(() -> new CustomerNotFoundException(command.customerId()));

        // 2. Execute business operation (events are collected)
        customer.updateProfile(command.name(), command.email(), command.phone());

        // 3. Save aggregate
        customerRepository.save(customer);

        // 4. Publish collected events
        domainEventService.publishEventsFromAggregate(customer);
    }
}
```

### Event 處理

```java
@Component
public class CustomerProfileUpdatedEventHandler extends AbstractDomainEventHandler<CustomerProfileUpdatedEvent> {

    @Override
    @Transactional
    public void handle(CustomerProfileUpdatedEvent event) {
        // Implement idempotency check
        if (isEventAlreadyProcessed(event.getEventId())) {
            return;
        }

        try {
            // Execute cross-aggregate business logic
            updateCustomerSearchIndex(event);
            sendProfileUpdateNotification(event);

            // Mark event as processed
            markEventAsProcessed(event.getEventId());

        } catch (Exception e) {
            // Log error and potentially retry
            logEventProcessingError(event, e);
            throw new DomainEventProcessingException("Failed to process profile update", e);
        }
    }

    @Override
    public Class<CustomerProfileUpdatedEvent> getSupportedEventType() {
        return CustomerProfileUpdatedEvent.class;
    }

    private boolean isEventAlreadyProcessed(UUID eventId) {
        // Check if event was already processed (idempotency)
        return processedEventRepository.existsByEventId(eventId);
    }

    private void markEventAsProcessed(UUID eventId) {
        // Mark event as processed for idempotency
        processedEventRepository.save(new ProcessedEvent(eventId, Instant.now()));
    }
}
```

## 最佳實踐

### 1. Event 命名慣例

- 使用過去式動詞：`CustomerCreated`、`OrderSubmitted`、`PaymentCompleted`
- 包含 aggregate 名稱：`Customer*Event`、`Order*Event`
- 明確說明發生的事情：`CustomerProfileUpdated` 而非 `CustomerChanged`

### 2. Event 內容指南

- 包含 aggregate ID 用於 event routing
- 包含 event handlers 所需的所有資料
- 避免包含不應分享的敏感資訊
- 包含 event metadata (eventId, occurredOn, eventType)

### 3. Event Handler 設計

- **Single Responsibility**：每個 handler 應處理一個特定的 event type
- **Idempotency**：Handlers 應可安全地多次執行
- **Error Handling**：實作適當的錯誤處理和日誌記錄
- **Transaction Management**：使用適當的 transaction 邊界

### 4. Cross-Aggregate 通訊

```java
// Good: Use events for cross-aggregate communication
@Component
public class OrderCreatedEventHandler extends AbstractDomainEventHandler<OrderCreatedEvent> {

    @Override
    public void handle(OrderCreatedEvent event) {
        // Update customer statistics
        customerStatisticsService.updateOrderCount(event.customerId());

        // Reserve inventory
        inventoryService.reserveItems(event.orderItems());

        // Send confirmation email
        notificationService.sendOrderConfirmation(event);
    }
}
```

### 5. Event 排序和依賴

```java
// Use @Order annotation for event handler precedence
@Component
@Order(1) // Process first
public class InventoryReservationHandler extends AbstractDomainEventHandler<OrderCreatedEvent> {
    // Reserve inventory first
}

@Component
@Order(2) // Process after inventory reservation
public class OrderConfirmationHandler extends AbstractDomainEventHandler<OrderCreatedEvent> {
    // Send confirmation after inventory is reserved
}
```

## 進階模式

### 1. Event Versioning (Schema Evolution Pattern)

```java
// Recommended: Schema Evolution with Backward Compatibility
public record CustomerCreatedEvent(
    CustomerId customerId,
    CustomerName customerName,
    Email email,
    MembershipLevel membershipLevel,
    // V2 fields using Optional for backward compatibility
    Optional<LocalDate> birthDate,
    Optional<Address> address,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent {

    // Primary factory method - latest version
    public static CustomerCreatedEvent create(
        CustomerId customerId,
        CustomerName customerName,
        Email email,
        MembershipLevel membershipLevel,
        LocalDate birthDate,
        Address address
    ) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new CustomerCreatedEvent(
            customerId, customerName, email, membershipLevel,
            Optional.ofNullable(birthDate),
            Optional.ofNullable(address),
            metadata.eventId(), metadata.occurredOn()
        );
    }

    // Backward compatible factory method
    public static CustomerCreatedEvent createLegacy(
        CustomerId customerId,
        CustomerName customerName,
        Email email,
        MembershipLevel membershipLevel
    ) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new CustomerCreatedEvent(
            customerId, customerName, email, membershipLevel,
            Optional.empty(), // Legacy version has no birth date
            Optional.empty(), // Legacy version has no address
            metadata.eventId(), metadata.occurredOn()
        );
    }

    @Override
    public String getEventType() {
        return "CustomerCreated"; // Keep same event type across versions
    }

    @Override
    public String getAggregateId() { return customerId.getValue(); }
}
```

#### Event Schema Registry Pattern (Advanced)

```java
public interface DomainEvent extends Serializable {
    // Existing methods...

    default EventSchema getSchema() {
        return EventSchemaRegistry.getSchema(this.getClass());
    }

    default int getSchemaVersion() {
        return getSchema().getVersion();
    }
}

@Component
public class EventSchemaRegistry {
    private static final Map<Class<? extends DomainEvent>, EventSchema> schemas = new ConcurrentHashMap<>();

    static {
        // Register event schemas
        register(CustomerCreatedEvent.class, EventSchema.builder()
            .version(2)
            .compatibleWith(1) // Compatible with version 1
            .build());
    }

    public static EventSchema getSchema(Class<? extends DomainEvent> eventClass) {
        return schemas.get(eventClass);
    }

    private static void register(Class<? extends DomainEvent> eventClass, EventSchema schema) {
        schemas.put(eventClass, schema);
    }
}

public record EventSchema(int version, Set<Integer> compatibleVersions) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int version;
        private Set<Integer> compatibleVersions = new HashSet<>();

        public Builder version(int version) {
            this.version = version;
            return this;
        }

        public Builder compatibleWith(int... versions) {
            for (int v : versions) {
                compatibleVersions.add(v);
            }
            return this;
        }

        public EventSchema build() {
            return new EventSchema(version, compatibleVersions);
        }
    }
}
```

#### Event Upcasting Pattern

```java
@Component
public class EventUpcaster {

    public DomainEvent upcast(StoredEvent storedEvent) {
        return switch (storedEvent.eventType()) {
            case "CustomerCreated" -> upcastCustomerCreatedEvent(storedEvent);
            default -> deserializeEvent(storedEvent);
        };
    }

    private CustomerCreatedEvent upcastCustomerCreatedEvent(StoredEvent storedEvent) {
        JsonNode eventData = parseJson(storedEvent.eventData());

        // Extract existing fields
        CustomerId customerId = new CustomerId(eventData.get("customerId").asText());
        CustomerName customerName = new CustomerName(eventData.get("customerName").asText());
        Email email = new Email(eventData.get("email").asText());
        MembershipLevel membershipLevel = MembershipLevel.valueOf(eventData.get("membershipLevel").asText());

        // V2 fields with defaults for legacy events
        Optional<LocalDate> birthDate = eventData.has("birthDate")
            ? Optional.of(LocalDate.parse(eventData.get("birthDate").asText()))
            : Optional.empty();

        Optional<Address> address = eventData.has("address")
            ? Optional.of(deserializeAddress(eventData.get("address")))
            : Optional.empty();

        return new CustomerCreatedEvent(
            customerId, customerName, email, membershipLevel,
            birthDate, address,
            UUID.fromString(eventData.get("eventId").asText()),
            LocalDateTime.parse(eventData.get("occurredOn").asText())
        );
    }
}
```

### 2. Event Categories 和 Priorities

```java
public interface DomainEvent extends Serializable {
    // Existing methods...

    default EventCategory getCategory() {
        return EventCategory.BUSINESS; // BUSINESS, INTEGRATION, AUDIT
    }

    default EventPriority getPriority() {
        return EventPriority.NORMAL; // HIGH, NORMAL, LOW
    }
}
```

### 3. Saga Pattern for Complex Workflows

```java
@Component
public class OrderProcessingSaga {

    @TransactionalEventListener
    @Order(1)
    public void on(OrderCreatedEvent event) {
        // Step 1: Reserve inventory
        inventoryService.reserveItems(event.orderItems());
    }

    @TransactionalEventListener
    @Order(2)
    public void on(InventoryReservedEvent event) {
        // Step 2: Process payment
        paymentService.processPayment(event.orderId(), event.amount());
    }

    @TransactionalEventListener
    @Order(3)
    public void on(PaymentProcessedEvent event) {
        // Step 3: Confirm order
        orderService.confirmOrder(event.orderId());
    }
}
```

### 4. Event Store Solutions

#### Event Store Interface

```java
public interface EventStore {
    void store(DomainEvent event);
    List<DomainEvent> getEventsForAggregate(String aggregateId);
    List<DomainEvent> getEventsByType(String eventType);
    List<DomainEvent> getAllEvents();
}
```

#### Option 1: EventStore DB (Recommended for Production)

```yaml
# docker-compose.yml
version: '3.8'
services:
  eventstore:
    image: eventstore/eventstore:23.10.0-bookworm-slim
    container_name: eventstore
    environment:
      - EVENTSTORE_CLUSTER_SIZE=1
      - EVENTSTORE_RUN_PROJECTIONS=All
      - EVENTSTORE_START_STANDARD_PROJECTIONS=true
      - EVENTSTORE_EXT_TCP_PORT=1113
      - EVENTSTORE_HTTP_PORT=2113
      - EVENTSTORE_INSECURE=true
      - EVENTSTORE_ENABLE_EXTERNAL_TCP=true
      - EVENTSTORE_ENABLE_ATOM_PUB_OVER_HTTP=true
    ports:
      - "1113:1113"
      - "2113:2113"
    volumes:
      - eventstore-volume-data:/var/lib/eventstore
      - eventstore-volume-logs:/var/log/eventstore

volumes:
  eventstore-volume-data:
  eventstore-volume-logs:
```

```java
@Component
@Profile("production")
public class EventStoreDbAdapter implements EventStore {
    private final EventStoreDBClient client;

    public EventStoreDbAdapter() {
        EventStoreDBConnectionString connectionString = EventStoreDBConnectionString
            .parseOrThrow("esdb://localhost:2113?tls=false");
        this.client = EventStoreDBClient.create(connectionString);
    }

    @Override
    public void store(DomainEvent event) {
        String streamName = "aggregate-" + event.getAggregateId();

        EventData eventData = EventData.builderAsJson(
            event.getEventId(),
            event.getEventType(),
            serializeEvent(event)
        ).build();

        client.appendToStream(streamName, eventData).join();
    }

    @Override
    public List<DomainEvent> getEventsForAggregate(String aggregateId) {
        String streamName = "aggregate-" + aggregateId;

        ReadResult result = client.readStream(streamName).join();

        return result.getEvents().stream()
            .map(this::deserializeEvent)
            .toList();
    }
}
```

#### Option 2: JPA Event Store (Recommended for Development)

```java
@Entity
@Table(name = "event_store")
public class StoredEvent {
    @Id
    private String eventId;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "aggregate_id")
    private String aggregateId;

    @Column(name = "aggregate_type")
    private String aggregateType;

    @Column(name = "event_data", columnDefinition = "TEXT")
    private String eventData;

    @Column(name = "occurred_on")
    private LocalDateTime occurredOn;

    @Column(name = "version")
    private Long version;

    // constructors, getters, setters
}

@Repository
public interface StoredEventRepository extends JpaRepository<StoredEvent, String> {
    List<StoredEvent> findByAggregateIdOrderByVersionAsc(String aggregateId);
    List<StoredEvent> findByEventTypeOrderByOccurredOnAsc(String eventType);
}

@Component
@Profile("development")
public class JpaEventStore implements EventStore {
    private final StoredEventRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void store(DomainEvent event) {
        StoredEvent storedEvent = new StoredEvent(
            event.getEventId().toString(),
            event.getEventType(),
            event.getAggregateId(),
            getAggregateType(event),
            serializeEvent(event),
            event.getOccurredOn(),
            getNextVersion(event.getAggregateId())
        );

        repository.save(storedEvent);
    }

    @Override
    public List<DomainEvent> getEventsForAggregate(String aggregateId) {
        return repository.findByAggregateIdOrderByVersionAsc(aggregateId)
            .stream()
            .map(this::deserializeEvent)
            .toList();
    }

    @Override
    public List<DomainEvent> getEventsByType(String eventType) {
        return repository.findByEventTypeOrderByOccurredOnAsc(eventType)
            .stream()
            .map(this::deserializeEvent)
            .toList();
    }

    private String serializeEvent(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new EventSerializationException("Failed to serialize event", e);
        }
    }

    private DomainEvent deserializeEvent(StoredEvent storedEvent) {
        try {
            Class<?> eventClass = Class.forName(getEventClassName(storedEvent.getEventType()));
            return (DomainEvent) objectMapper.readValue(storedEvent.getEventData(), eventClass);
        } catch (Exception e) {
            throw new EventDeserializationException("Failed to deserialize event", e);
        }
    }
}
```

#### Option 3: In-Memory Event Store (測試 Only)

```java
@Component
@Profile("test")
public class InMemoryEventStore implements EventStore {
    private final Map<String, List<DomainEvent>> eventsByAggregate = new ConcurrentHashMap<>();
    private final Map<String, List<DomainEvent>> eventsByType = new ConcurrentHashMap<>();
    private final List<DomainEvent> allEvents = new CopyOnWriteArrayList<>();

    @Override
    public void store(DomainEvent event) {
        allEvents.add(event);

        eventsByAggregate.computeIfAbsent(event.getAggregateId(), k -> new ArrayList<>())
            .add(event);

        eventsByType.computeIfAbsent(event.getEventType(), k -> new ArrayList<>())
            .add(event);
    }

    @Override
    public List<DomainEvent> getEventsForAggregate(String aggregateId) {
        return new ArrayList<>(eventsByAggregate.getOrDefault(aggregateId, List.of()));
    }

    @Override
    public List<DomainEvent> getEventsByType(String eventType) {
        return new ArrayList<>(eventsByType.getOrDefault(eventType, List.of()));
    }

    @Override
    public List<DomainEvent> getAllEvents() {
        return new ArrayList<>(allEvents);
    }

    public void clear() {
        eventsByAggregate.clear();
        eventsByType.clear();
        allEvents.clear();
    }
}
```

#### Event Store 設定

```java
@Configuration
public class EventStoreConfiguration {

    @Bean
    @Profile("development")
    public EventStore developmentEventStore(StoredEventRepository repository, ObjectMapper objectMapper) {
        return new JpaEventStore(repository, objectMapper);
    }

    @Bean
    @Profile("test")
    public EventStore testEventStore() {
        return new InMemoryEventStore();
    }

    @Bean
    @Profile("production")
    public EventStore productionEventStore() {
        return new EventStoreDbAdapter();
    }
}
```

#### Event Store 整合

```java
@Component
public class EventStoreIntegration {
    private final EventStore eventStore;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void storeEvent(DomainEventPublisherAdapter.DomainEventWrapper wrapper) {
        DomainEvent event = wrapper.getSource();
        eventStore.store(event);
    }
}
```

## 錯誤處理和韌性

### 1. Retry 機制

```java
@Component
public class ResilientEventHandler extends AbstractDomainEventHandler<CustomerCreatedEvent> {

    @Retryable(
        value = {TransientException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    @Override
    public void handle(CustomerCreatedEvent event) {
        // Event processing logic with retry capability
    }

    @Recover
    public void recover(TransientException ex, CustomerCreatedEvent event) {
        // Handle final failure after all retries
        deadLetterService.send(event, ex);
    }
}
```

### 2. Dead Letter Queue

```java
@Component
public class DeadLetterService {

    public void send(DomainEvent event, Exception cause) {
        DeadLetterEvent deadLetter = new DeadLetterEvent(
            event.getEventId(),
            event.getClass().getSimpleName(),
            serializeEvent(event),
            cause.getMessage(),
            Instant.now()
        );

        deadLetterRepository.save(deadLetter);

        // Optionally send to external dead letter queue
        messageQueue.send("dead-letter-queue", deadLetter);
    }
}
```

## 測試指南

### 1. Event 收集測試

```java
@Test
void should_collect_customer_created_event_when_customer_is_created() {
    // Given
    CustomerId customerId = CustomerId.generate();
    CustomerName name = new CustomerName("John Doe");
    Email email = new Email("john@example.com");

    // When
    Customer customer = new Customer(customerId, name, email, MembershipLevel.STANDARD);

    // Then
    assertThat(customer.hasUncommittedEvents()).isTrue();
    List<DomainEvent> events = customer.getUncommittedEvents();
    assertThat(events).hasSize(1);
    assertThat(events.get(0)).isInstanceOf(CustomerCreatedEvent.class);

    CustomerCreatedEvent event = (CustomerCreatedEvent) events.get(0);
    assertThat(event.customerId()).isEqualTo(customerId);
    assertThat(event.customerName()).isEqualTo(name);
    assertThat(event.email()).isEqualTo(email);
}
```

### Event Store 測試

```java
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class EventStoreTestBase {

    @Autowired
    protected EventStore eventStore;

    @BeforeEach
    void setUp() {
        if (eventStore instanceof InMemoryEventStore inMemoryStore) {
            inMemoryStore.clear();
        }
    }

    protected void givenEvents(DomainEvent... events) {
        for (DomainEvent event : events) {
            eventStore.store(event);
        }
    }

    protected void assertEventStored(Class<? extends DomainEvent> eventType, String aggregateId) {
        List<DomainEvent> events = eventStore.getEventsForAggregate(aggregateId);
        assertThat(events).anyMatch(event -> eventType.isInstance(event));
    }

    protected void assertEventCount(String aggregateId, int expectedCount) {
        List<DomainEvent> events = eventStore.getEventsForAggregate(aggregateId);
        assertThat(events).hasSize(expectedCount);
    }
}

@Test
void should_store_and_retrieve_events_by_aggregate() {
    // Given
    CustomerId customerId = CustomerId.generate();
    CustomerCreatedEvent event = CustomerCreatedEvent.create(
        customerId,
        new CustomerName("John Doe"),
        new Email("john@example.com"),
        MembershipLevel.STANDARD,
        LocalDate.of(1990, 1, 1),
        new Address("123 Main St", "City", "12345")
    );

    // When
    eventStore.store(event);

    // Then
    List<DomainEvent> retrievedEvents = eventStore.getEventsForAggregate(customerId.getValue());
    assertThat(retrievedEvents).hasSize(1);
    assertThat(retrievedEvents.get(0)).isInstanceOf(CustomerCreatedEvent.class);

    CustomerCreatedEvent retrievedEvent = (CustomerCreatedEvent) retrievedEvents.get(0);
    assertThat(retrievedEvent.customerId()).isEqualTo(customerId);
    assertThat(retrievedEvent.birthDate()).isPresent();
    assertThat(retrievedEvent.address()).isPresent();
}
```

### 2. Event Handler 測試

```java
@Test
void should_send_welcome_email_when_customer_created() {
    // Given
    CustomerCreatedEvent event = CustomerCreatedEvent.create(
        CustomerId.of("CUST-001"),
        new CustomerName("John Doe"),
        new Email("john@example.com"),
        MembershipLevel.STANDARD
    );

    // When
    customerCreatedEventHandler.handle(event);

    // Then
    verify(emailService).sendWelcomeEmail(event.email(), event.customerName());
    verify(customerStatsService).createStatsRecord(event.customerId());
}
```

## 架構規則

### 1. Event Publishing 規則

- 只有 Aggregate Roots 可以使用 `collectEvent()` 收集 domain events
- Application Services 負責透過 `DomainEventApplicationService` 發布 events
- Event handlers 必須在 Infrastructure Layer
- Events 必須在 transaction 邊界內發布

### 2. Event Handler 規則

- Event handlers 必須繼承 `AbstractDomainEventHandler<T>`
- Event handlers 必須標註 `@Component`
- Event handlers 必須實作 idempotency checks
- Event handlers 必須使用 `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`

### 3. Event 設計規則

- Events 必須實作為不可變的 Java Records
- Events 必須實作 `DomainEvent` interface
- Events 必須包含 eventId、occurredOn、eventType 和 aggregateId
- Events 必須使用 static factory methods 建立
- Event versioning 必須使用 Schema Evolution pattern，對於向後相容使用 Optional fields
- 避免使用明確的 version methods (createV1, createV2) - 使用描述性的 factory methods

## 監控 和 Observability

### 1. Event Metrics

```java
@Component
public class EventMetricsCollector {
    private final MeterRegistry meterRegistry;

    @TransactionalEventListener
    public void collectMetrics(DomainEventPublisherAdapter.DomainEventWrapper wrapper) {
        DomainEvent event = wrapper.getSource();

        // Count events by type
        Counter.builder("domain.events.published")
            .tag("event.type", event.getEventType())
            .tag("aggregate.type", getAggregateType(event))
            .register(meterRegistry)
            .increment();
    }
}
```

### 2. Event Tracing

```java
@Component
public class EventTracingHandler {

    @TransactionalEventListener
    public void trace(DomainEventPublisherAdapter.DomainEventWrapper wrapper) {
        DomainEvent event = wrapper.getSource();

        Span span = tracer.nextSpan()
            .name("domain-event-processing")
            .tag("event.type", event.getEventType())
            .tag("event.id", event.getEventId().toString())
            .tag("aggregate.id", event.getAggregateId())
            .start();

        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            // Event processing is traced
        } finally {
            span.end();
        }
    }
}
```

這份完整指南確保整個專案中一致、可靠且可維護的 domain event 實作。

## 環境特定設定

### Development 設定

```yaml
# application-development.yml
spring:
  profiles:
    active: development
  datasource:
    url: jdbc:h2:file:./data/eventstore
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

event-store:
  type: jpa

logging:
  level:
    solid.humank.genaidemo.infrastructure.event: DEBUG
```

### Test 設定

```yaml
# application-test.yml
spring:
  profiles:
    active: test
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop

event-store:
  type: in-memory
```

### Production 設定

```yaml
# application-production.yml
spring:
  profiles:
    active: production

event-store:
  type: eventstore-db
  connection-string: "esdb://eventstore:2113?tls=false"

logging:
  level:
    solid.humank.genaidemo.infrastructure.event: INFO
```

## Event Store 環境建議

### Development Stage

- **Recommended**: JPA Event Store with H2 file database
- **Benefits**: Persistent across restarts, easy to inspect data, SQL queries for debugging
- **Setup**: No additional containers needed

### 測試 Stage

- **Recommended**: In-Memory Event Store
- **Benefits**: Fast, clean state between tests, no external dependencies
- **Setup**: Automatic cleanup, perfect isolation

### Production Stage

- **Recommended**: EventStore DB
- **Benefits**: Purpose-built for event sourcing, high performance, built-in projections
- **Alternative**: Axon Framework Event Store for Java ecosystem integration

## Migration 策略

When moving between event store implementations:

1. **Development to Production**: Export events from JPA store, import to EventStore DB
2. **Schema Changes**: Use Event Upcasting to handle version differences
3. **Testing**: Always test migration with realistic data volumes

這個綜合性的方法確保在所有開發階段都能可靠地處理 events，同時保持未來變更的彈性。
