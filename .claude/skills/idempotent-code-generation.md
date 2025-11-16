# Idempotent Code Generation Skill

**Kiro Principle**: Idempotency（幂等性）
**Purpose**: 確保相同的需求輸入產生一致的程式碼輸出

## 技能描述

此技能實現 Amazon Kiro 的幂等性原則，確保：
1. 相同的輸入產生相同的輸出
2. 可重複執行而不產生副作用
3. 結果可預測且可驗證

## 使用場景

- 生成 DDD Aggregate Root
- 生成 Repository 實作
- 生成 Value Object
- 生成 Domain Event

## 執行流程

### Step 1: 輸入驗證（Immutable Input）
```
1. 讀取需求規格（requirements.md）
2. 驗證輸入 schema
3. 生成輸入雜湊值（用於幂等性檢查）
4. 鎖定輸入為 read-only
```

### Step 2: 模板選擇（Boundary Control）
```
1. 根據需求類型選擇對應模板
2. 載入 Steering Rules
3. 載入 Examples
4. 驗證模板完整性
```

### Step 3: 程式碼生成（Stateless Handler）
```
1. 使用確定性演算法生成程式碼
2. 不依賴 session state
3. 所有參數透過輸入傳遞
4. 使用固定的格式化規則
```

### Step 4: 輸出驗證（Quality Gate）
```
1. 驗證生成的程式碼符合 schema
2. 執行 linting（Checkstyle, PMD）
3. 驗證符合 Steering Rules
4. 生成幂等性報告
```

### Step 5: 安全網（Fail-Safe）
```
1. 如果驗證失敗，回滾所有變更
2. 保存失敗日誌
3. 不提交不完整的程式碼
```

## 幂等性保證機制

### 輸入雜湊
```bash
# 計算輸入的 SHA-256 雜湊
INPUT_HASH=$(echo -n "$REQUIREMENTS" | sha256sum | cut -d' ' -f1)

# 檢查是否已經處理過相同輸入
if [ -f ".claude/cache/${INPUT_HASH}.done" ]; then
  echo "Idempotent: Same input already processed"
  cat ".claude/cache/${INPUT_HASH}.output"
  exit 0
fi
```

### 輸出快取
```bash
# 儲存輸出到快取
echo "$OUTPUT" > ".claude/cache/${INPUT_HASH}.output"
touch ".claude/cache/${INPUT_HASH}.done"
```

## 使用範例

### 生成 Customer Aggregate

**輸入**（requirements.md）:
```markdown
## Aggregate: Customer

### Attributes
- CustomerId (Value Object)
- CustomerName (Value Object)
- Email (Value Object)
- Phone (String)
- Address (String)

### Commands
- CreateCustomer
- UpdateProfile
- SuspendCustomer

### Events
- CustomerCreated
- CustomerProfileUpdated
- CustomerSuspended
```

**執行**:
```bash
# 使用此 skill
/idempotent-generate aggregate Customer
```

**輸出**:
```java
// domain/customer/model/aggregate/Customer.java
@AggregateRoot
public class Customer extends AggregateRoot<CustomerId> {

    private CustomerName name;
    private Email email;
    private Phone phone;
    private Address address;
    private CustomerStatus status;

    // 建構子（private，透過 factory method 創建）
    private Customer(CustomerId id, CustomerName name, Email email) {
        super(id);
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.email = Objects.requireNonNull(email, "Email cannot be null");
        this.status = CustomerStatus.ACTIVE;

        registerEvent(new CustomerCreatedEvent(id, name, email, LocalDateTime.now()));
    }

    // Factory method
    public static Customer create(CustomerId id, CustomerName name, Email email) {
        return new Customer(id, name, email);
    }

    // 業務方法
    public void updateProfile(CustomerName name, Phone phone, Address address) {
        if (this.status == CustomerStatus.SUSPENDED) {
            throw new BusinessRuleViolationException("Cannot update suspended customer");
        }

        this.name = name;
        this.phone = phone;
        this.address = address;

        registerEvent(new CustomerProfileUpdatedEvent(
            this.getId(), name, phone, address, LocalDateTime.now()));
    }

    public void suspend(String reason) {
        this.status = CustomerStatus.SUSPENDED;
        registerEvent(new CustomerSuspendedEvent(this.getId(), reason, LocalDateTime.now()));
    }

    // Getters (immutable)
    public CustomerName getName() { return name; }
    public Email getEmail() { return email; }
    public Phone getPhone() { return phone; }
    public Address getAddress() { return address; }
    public CustomerStatus getStatus() { return status; }
}
```

**幂等性驗證**:
```bash
# 第一次執行
$ /idempotent-generate aggregate Customer
✓ Generated Customer.java (SHA-256: a1b2c3...)

# 第二次執行（相同輸入）
$ /idempotent-generate aggregate Customer
✓ Idempotent: Using cached result (SHA-256: a1b2c3...)
✓ Output identical to previous run
```

## 配置參數

```json
{
  "skill": "idempotent-code-generation",
  "parameters": {
    "cache_enabled": true,
    "cache_ttl": 3600,
    "validation_strict": true,
    "template_source": ".kiro/examples/ddd-patterns/",
    "output_verification": true,
    "hash_algorithm": "SHA-256"
  }
}
```

## 依賴

- Steering Rules: `.kiro/steering/ddd-tactical-patterns.md`
- Examples: `.kiro/examples/ddd-patterns/aggregate-root-examples.md`
- Templates: `.claude/templates/aggregate-root.java.template`
- Validation: ArchUnit rules, Checkstyle

## 輸出位置

- 生成的程式碼: `app/src/main/java/`
- 幂等性快取: `.claude/cache/`
- 執行報告: `reports-summaries/claude-code-execution/idempotent-generation-{timestamp}.md`

## 成功標準

1. ✅ 相同輸入產生完全相同的輸出（byte-level identical）
2. ✅ 輸出通過所有驗證（ArchUnit, Checkstyle, PMD）
3. ✅ 符合 Steering Rules
4. ✅ 可編譯（無語法錯誤）
5. ✅ 包含完整的 JavaDoc 和業務規則驗證

## 錯誤處理

```python
try:
    # Validate input
    validate_requirements(requirements)

    # Generate code
    code = generate_from_template(requirements, template)

    # Validate output
    validate_output(code)

    # Save to cache
    save_to_cache(input_hash, code)

    return code

except ValidationError as e:
    # Rollback and report
    logger.error(f"Validation failed: {e}")
    return None

except TemplateError as e:
    # Template issue - fix template
    logger.error(f"Template error: {e}")
    return None
```

## 監控指標

- `idempotent_cache_hit_rate`: 快取命中率（目標 > 60%）
- `generation_consistency_score`: 一致性分數（目標 = 100%）
- `validation_pass_rate`: 驗證通過率（目標 > 95%）
- `avg_generation_time`: 平均生成時間（目標 < 30s）
