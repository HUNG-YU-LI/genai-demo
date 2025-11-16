# Event Schemas

## 概述

此目錄包含系統中所有 domain events 的 JSON Schema 定義。這些 schemas 可用於：

- **事件驗證**：在發佈前驗證事件 payloads
- **程式碼生成**：從 schemas 生成事件類別
- **文件產生**：自動產生事件文件
- **Contract 測試**：驗證服務之間的事件 contracts

**最後更新**: 2025-10-25

---

## Schema 組織

Schemas 依 bounded context 組織：

```text
schemas/
├── README.md (本文件)
├── customer/
│   ├── CustomerCreatedEvent.json
│   ├── CustomerProfileUpdatedEvent.json
│   └── ...
├── order/
│   ├── OrderCreatedEvent.json
│   ├── OrderSubmittedEvent.json
│   └── ...
├── product/
│   ├── ProductCreatedEvent.json
│   └── ...
├── payment/
│   ├── PaymentProcessedEvent.json
│   └── ...
└── common/
    ├── DomainEvent.json (基礎 schema)
    ├── Money.json
    ├── Address.json
    └── ...
```

---

## Schema 標準

### JSON Schema 版本

所有 schemas 使用 **JSON Schema Draft 2020-12**。

### 共通欄位

所有 domain events 必須包含這些欄位：

```json
{
  "eventId": "UUID",
  "eventType": "string",
  "occurredOn": "ISO 8601 datetime",
  "aggregateId": "string"
}
```

### 命名慣例

- **Schema 檔案**：`{EventName}.json`（例如：`CustomerCreatedEvent.json`）
- **事件類型**：PascalCase（例如：`CustomerCreated`）
- **欄位名稱**：camelCase（例如：`customerId`、`orderTotal`）

---

## 使用 Schemas

### 驗證範例 (Java)

```java
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;

public class EventValidator {

    private final JsonSchemaFactory factory =
        JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

    public void validateEvent(String eventJson, String schemaPath) {
        JsonSchema schema = factory.getSchema(
            getClass().getResourceAsStream(schemaPath)
        );

        Set<ValidationMessage> errors = schema.validate(
            new ObjectMapper().readTree(eventJson)
        );

        if (!errors.isEmpty()) {
            throw new EventValidationException(errors);
        }
    }
}
```

### 程式碼生成範例

```bash
# 使用 quicktype 從 schemas 生成 Java 類別
quicktype \
  --src schemas/customer/CustomerCreatedEvent.json \
  --lang java \
  --out src/main/java/events/CustomerCreatedEvent.java
```

---

## Schema 範例

### 基礎 Domain Event Schema

參見 [common/DomainEvent.json](common/DomainEvent.json) 取得所有事件繼承的基礎 schema。

### Customer Event Schema

參見 [customer/CustomerCreatedEvent.json](customer/CustomerCreatedEvent.json) 取得完整範例。

### Order Event Schema

參見 [order/OrderSubmittedEvent.json](order/OrderSubmittedEvent.json) 取得包含巢狀物件的複雜事件範例。

---

## Schema 版本控制

### 版本策略

- **主要版本**：Breaking changes（欄位移除、類型變更）
- **次要版本**：累加性變更（新增選填欄位）
- **修訂版本**：文件更新、說明

### Schema 中的版本

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "https://api.example.com/schemas/v1/CustomerCreatedEvent.json",
  "version": "1.0.0",
  "title": "CustomerCreatedEvent"
}
```

### 向後相容性

- 新欄位必須是選填的
- 現有欄位不能被移除
- 欄位類型不能變更
- Breaking changes 需要新的事件類型

---

## 驗證工具

### 線上驗證工具

- [JSON Schema Validator](https://www.jsonschemavalidator.net/)
- [JSON Schema Lint](https://jsonschemalint.com/)

### CLI 工具

```bash
# 安裝 ajv-cli
npm install -g ajv-cli

# 驗證事件是否符合 schema
ajv validate \
  -s schemas/customer/CustomerCreatedEvent.json \
  -d examples/customer-created-example.json
```

### IDE 整合

- **VS Code**：安裝 "JSON Schema Validator" 擴充套件
- **IntelliJ IDEA**：內建 JSON Schema 支援
- **Eclipse**：安裝 "JSON Editor Plugin"

---

## Schema Registry 整合

### Kafka Schema Registry

```java
@Configuration
public class SchemaRegistryConfig {

    @Bean
    public SchemaRegistryClient schemaRegistryClient() {
        return new CachedSchemaRegistryClient(
            "http://schema-registry:8081",
            100
        );
    }

    @Bean
    public KafkaAvroSerializer avroSerializer() {
        return new KafkaAvroSerializer(schemaRegistryClient());
    }
}
```

### Schema 演進規則

1. **Forward Compatible**：新的 consumers 可以讀取舊資料
2. **Backward Compatible**：舊的 consumers 可以讀取新資料
3. **Full Compatible**：同時具備 forward 和 backward compatible

---

## 使用 Schemas 進行測試

### Contract 測試

```java
@Test
void customer_created_event_should_match_schema() {
    // Given
    CustomerCreatedEvent event = createTestEvent();
    String eventJson = objectMapper.writeValueAsString(event);

    // When
    Set<ValidationMessage> errors = validateAgainstSchema(
        eventJson,
        "schemas/customer/CustomerCreatedEvent.json"
    );

    // Then
    assertThat(errors).isEmpty();
}
```

### Property-Based 測試

```java
@Property
void all_customer_events_should_have_valid_structure(
    @ForAll("customerEvents") CustomerCreatedEvent event
) {
    String eventJson = objectMapper.writeValueAsString(event);
    assertThat(validateAgainstSchema(eventJson)).isEmpty();
}
```

---

## 相關文件

- **Event Catalog**：[../event-catalog.md](../event-catalog.md)
- **Event 文件**：[../README.md](../README.md)
- **Domain Events 指南**：`.kiro/steering/domain-events.md`

---

## 貢獻

### 新增 Schemas

1. 在適當的 context 目錄中建立 schema 檔案
2. 繼承基礎 `DomainEvent.json` schema
3. 在 `examples/` 目錄中新增範例
4. 更新此 README，加入新 schema 參考
5. 執行驗證測試

### Schema 審查檢查清單

- [ ] 遵循 JSON Schema Draft 2020-12
- [ ] 繼承基礎 DomainEvent schema
- [ ] 所有必填欄位都有文件
- [ ] 欄位描述清楚
- [ ] 提供範例
- [ ] 驗證測試通過
- [ ] 維持向後相容性

---

**文件版本**: 1.0
**最後更新**: 2025-10-25
**負責人**: 架構團隊
