# Pair Programming 指南

## 概覽

開發團隊有效 pair programming 的實用指南。

**相關標準**: [Design Principles](../../steering/design-principles.md)

---

## 什麼是 Pair Programming？

兩位開發者在同一個工作站上一起工作：
- **Driver**：撰寫程式碼
- **Navigator**：審查、提前思考、提供改進建議

---

## 優勢

### 程式碼品質
- 更少的錯誤（減少 15-20%）
- 更好的設計決策
- 即時的程式碼審查
- 知識分享

### 團隊優勢
- 更快的新人培訓
- 集體程式碼所有權
- 減少知識孤島
- 改善溝通

---

## Pairing 風格

### 1. Driver-Navigator (經典)

**何時使用**：一般開發、學習

```
Driver:    輸入程式碼，專注於語法
Navigator: 審查、策略性思考、提供改進建議

每 25-30 分鐘切換角色
```

### 2. Ping-Pong Pairing

**何時使用**：TDD 開發

```
Developer A: 撰寫失敗的測試 (Red)
Developer B: 讓測試通過 (Green)
Developer A: 重構 (Refactor)
Developer B: 撰寫下一個測試 (Red)
... 繼續切換
```

**範例**：
```java
// Developer A 撰寫測試
@Test
void should_calculate_total_with_discount() {
    Order order = anOrder().withTotal(100).build();
    Customer customer = aPremiumCustomer().build();

    BigDecimal total = calculator.calculate(order, customer);

    assertThat(total).isEqualByComparingTo("90.00");
}

// Developer B 實作
public BigDecimal calculate(Order order, Customer customer) {
    BigDecimal total = order.getTotal();
    if (customer.isPremium()) {
        return total.multiply(new BigDecimal("0.9"));
    }
    return total;
}

// Developer A 重構
public BigDecimal calculate(Order order, Customer customer) {
    return customer.applyDiscount(order.getTotal());
}
```

### 3. Strong-Style Pairing

**何時使用**：教學、知識轉移

```
規則："要讓想法從你的腦中進入電腦，
       必須經過其他人的手"

Expert:   導航、解釋概念
Learner:  駕駛、提問
```

---

## 最佳實踐

### Pairing 前

- [ ] 明確的會議目標
- [ ] 舒適的工作空間設置
- [ ] 商定的時間長度
- [ ] 減少干擾

### Pairing 期間

- [ ] 定期切換角色（25-30 分鐘）
- [ ] 休息（每小時 5-10 分鐘）
- [ ] 持續溝通
- [ ] 保持尊重和耐心
- [ ] 提出問題
- [ ] 解釋你的思考

### Pairing 後

- [ ] 反思有效的做法
- [ ] 討論改進
- [ ] 記錄決策
- [ ] 一起提交程式碼

---

## 溝通技巧

### 作為 Driver

```java
// ✅ GOOD: 大聲思考
"我打算將這個驗證邏輯提取到單獨的方法..."
"讓我為訂單為空的邊緣情況添加測試..."
"我認為這裡應該使用 value object 來處理 email..."

// ❌ BAD: 無聲編碼
// 只是打字而不解釋
```

### 作為 Navigator

```java
// ✅ GOOD: 建設性建議
"如果我們將其提取為 value object 會如何？"
"我們應該為 null 情況添加測試嗎？"
"我注意到這裡有些重複，也許我們可以重構？"

// ❌ BAD: 微觀管理
"不，使用不同的變數名稱"
"那是錯的，這樣做"
"你打字太慢了"
```

---

## 常見挑戰

### 挑戰 1：技能差距

**解決方案**：使用 Strong-Style Pairing
- 專家導航、解釋概念
- 新手駕駛、邊做邊學
- 專注於學習，而非速度

### 挑戰 2：個性衝突

**解決方案**：設定基本規則
- 尊重不同的方法
- 專注於程式碼，而非個人
- 如果緊張升級就休息
- 定期輪換配對

### 挑戰 3：遠端 Pairing

**解決方案**：使用適當的工具
- 螢幕共享（VS Code Live Share）
- 視訊通話（清晰的音訊/視訊）
- 共享終端機存取
- 定期休息（更頻繁）

---

## 遠端 Pairing 設置

### 工具

```bash
# VS Code Live Share
# - 即時協作
# - 共享除錯
# - 共享終端機

# Tuple / Pop
# - 低延遲螢幕共享
# - 專為 pair programming 設計

# tmux + SSH
# - 基於終端機的 pairing
# - 輕量級、快速
```

### 最佳實踐

- [ ] 良好的麥克風和攝影機
- [ ] 穩定的網路連線
- [ ] 清晰的螢幕共享
- [ ] 更頻繁的休息
- [ ] 過度溝通

---

## Pairing 時程表

### 全職 Pairing

```
09:00 - 10:30  Pair Session 1
10:30 - 10:45  休息
10:45 - 12:15  Pair Session 2
12:15 - 13:15  午餐
13:15 - 14:45  Pair Session 3
14:45 - 15:00  休息
15:00 - 16:30  Pair Session 4
16:30 - 17:00  獨立時間（email、行政）
```

### 兼職 Pairing

```
上午：   獨立工作（研究、設計）
下午：   Pair programming（實作）

或輪換：
週一/三/五: Pairing
週二/四:     獨立工作
```

---

## 何時 Pair

### 總是 Pair
- [ ] 複雜功能
- [ ] 關鍵錯誤修復
- [ ] 新團隊成員入職
- [ ] 學習新技術
- [ ] 重構遺留程式碼

### 考慮 Pairing
- [ ] 中等複雜度功能
- [ ] 程式碼審查（即時）
- [ ] 架構決策
- [ ] 效能最佳化

### 獨立工作
- [ ] 簡單、理解清楚的任務
- [ ] 研究和探索
- [ ] 文件撰寫
- [ ] 行政任務

---

## 衡量成功

### 定性指標
- 團隊滿意度
- 知識分享
- 程式碼品質感知
- 新人培訓速度

### 定量指標
- 缺陷率
- 程式碼審查時間（減少）
- 上線時間
- 測試覆蓋率

---

## Pairing Session 範例

### Session：實作 Order Submission

**目標**：實作帶驗證的訂單提交

**時長**：2 小時

**方法**：Ping-Pong TDD

```java
// Round 1: Developer A 撰寫測試
@Test
void should_submit_order_successfully() {
    Order order = anOrder()
        .withCustomer(aCustomer())
        .withItem(aProduct(), quantity(1))
        .build();

    orderService.submitOrder(order.getId());

    Order submitted = orderRepository.findById(order.getId()).get();
    assertThat(submitted.getStatus()).isEqualTo(OrderStatus.SUBMITTED);
}

// Developer B 實作（最小化）
public void submitOrder(OrderId orderId) {
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new OrderNotFoundException(orderId));
    order.setStatus(OrderStatus.SUBMITTED);
    orderRepository.save(order);
}

// Round 2: Developer B 撰寫測試
@Test
void should_throw_exception_when_order_is_empty() {
    Order order = anOrder()
        .withCustomer(aCustomer())
        .withNoItems()
        .build();

    assertThatThrownBy(() -> orderService.submitOrder(order.getId()))
        .isInstanceOf(BusinessRuleViolationException.class)
        .hasMessageContaining("empty order");
}

// Developer A 實作
public void submitOrder(OrderId orderId) {
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new OrderNotFoundException(orderId));

    if (order.getItems().isEmpty()) {
        throw new BusinessRuleViolationException("Cannot submit empty order");
    }

    order.setStatus(OrderStatus.SUBMITTED);
    orderRepository.save(order);
}

// Developer A 重構
public void submitOrder(OrderId orderId) {
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new OrderNotFoundException(orderId));

    order.submit(); // Move logic to aggregate
    orderRepository.save(order);
}

// 繼續更多測試和實作...
```

---

## 總結

有效的 pair programming 需要：

1. **明確的角色** - Driver 和 Navigator
2. **定期切換** - 每 25-30 分鐘
3. **良好溝通** - 大聲思考、提問
4. **相互尊重** - 不同的方法都是有效的
5. **適當設置** - 舒適的工作空間、良好的工具

記住：**Pairing 是一種技能** - 隨著實踐而改進。

---

**相關文件**：
- [Simple Design Examples](simple-design-examples.md)
- [Refactoring Guide](refactoring-guide.md)
- [Design Principles](../../steering/design-principles.md)
