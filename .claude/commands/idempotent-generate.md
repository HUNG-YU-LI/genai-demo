# Command: /idempotent-generate

使用 Kiro idempotency 原則生成程式碼。

## 用法

```bash
/idempotent-generate <type> <name> [options]
```

## 參數

- `<type>`: 生成類型（aggregate, valueobject, event, repository, service）
- `<name>`: 元件名稱（例如：Customer, Order, Payment）
- `[options]`: 可選參數

## 範例

### 生成 Aggregate Root
```bash
/idempotent-generate aggregate Customer
```

### 生成 Value Object
```bash
/idempotent-generate valueobject CustomerId
```

### 生成 Domain Event
```bash
/idempotent-generate event CustomerCreated
```

### 生成 Repository
```bash
/idempotent-generate repository Customer
```

## 執行流程

1. 驗證輸入
2. 計算輸入雜湊
3. 檢查快取
4. 如果快取命中，返回快取結果
5. 如果快取未命中，生成程式碼
6. 驗證輸出
7. 保存到快取
8. 返回結果

## 輸出

生成的檔案將被放置在正確的目錄：
- Aggregate: `domain/{context}/model/aggregate/`
- Value Object: `domain/{context}/model/valueobject/`
- Event: `domain/{context}/model/events/`
- Repository Interface: `domain/{context}/model/repository/`
- Repository Implementation: `infrastructure/{context}/persistence/adapter/`

## 質量保證

所有生成的程式碼將：
- ✅ 符合 DDD tactical patterns
- ✅ 通過 ArchUnit 驗證
- ✅ 通過 Checkstyle 驗證
- ✅ 包含完整的 JavaDoc
- ✅ 包含業務規則驗證
- ✅ 幂等性保證（相同輸入 = 相同輸出）
