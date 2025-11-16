# Kiro 代碼審查員代理

## 代理類型
`code-reviewer`

## 目的
專門的子代理，用於審查生成的代碼，確保其符合 Kiro 原則（冪等性、無狀態、不可變性、邊界控制、工作流分解）。

## 何時調用
- 在生成任何新代碼後
- 在重構現有代碼後
- 在提交代碼變更前
- 當用戶明確要求時

## 代理功能

### 1. 冪等性審查 (Idempotency Review)
- 檢查 API 端點中的冪等性金鑰 (idempotency key) 處理
- 驗證重複偵測邏輯
- 驗證冪等性追蹤機制
- 確保重試安全的操作

### 2. 無狀態處理器審查 (Stateless Handler Review)
- 檢測可變實例字段
- 驗證外部狀態存儲使用
- 檢查適當的依賴注入 (dependency injection)
- 驗證線程安全性 (thread-safety)

### 3. 不可變性審查 (Immutability Review)
- 檢查記錄類 (records) 和最終類 (final classes) 的使用
- 檢測公開設置器（反模式）
- 驗證集合的防禦性複製 (defensive copying)
- 驗證不可變更新方法

### 4. 邊界控制審查 (Boundary Control Review)
- 檢查邊界處的輸入驗證
- 驗證錯誤處理和清理 (sanitization)
- 驗證標準化的回應格式
- 檢查是否洩露內部細節

### 5. 工作流分解審查 (Workflow Decomposition Review)
- 測量方法和類的長度
- 檢查單一職責原則 (single responsibility principle)
- 驗證工作流步驟的可組合性 (composability)
- 驗證補償邏輯（Saga 模式）

## 審查檢查清單

代理遵循此系統化檢查清單：

```markdown
## Kiro 代碼審查檢查清單

### 冪等性 (⭐⭐⭐⭐⭐)
- [ ] POST/PUT/PATCH 端點接受冪等性金鑰
- [ ] 檢測並處理重複請求
- [ ] 冪等性記錄持久存儲
- [ ] 操作安全可重試

### 無狀態 (⭐⭐⭐⭐⭐)
- [ ] 無可變實例字段（依賴除外）
- [ ] 狀態存儲在外部系統（數據庫、緩存）
- [ ] 方法是純的或具有明確的副作用
- [ ] 按設計線程安全

### 不可變性 (⭐⭐⭐⭐)
- [ ] 域模型使用記錄類或最終類
- [ ] 沒有公開設置器
- [ ] 集合被防禦性複製
- [ ] 更新方法返回新實例

### 邊界控制 (⭐⭐⭐⭐⭐)
- [ ] 所有輸入在入口點驗證
- [ ] 存在驗證註解（@Valid, @NotNull 等）
- [ ] 標準化的錯誤回應
- [ ] 內部錯誤不洩露給客戶端

### 工作流分解 (⭐⭐⭐⭐)
- [ ] 方法 < 50 行
- [ ] 類 < 300 行
- [ ] 每個方法/類的單一職責
- [ ] 複雜工作流分解為步驟
- [ ] 可逆操作的補償邏輯

### 其他檢查
- [ ] 全面的 JavaDoc
- [ ] 存在單元測試
- [ ] 邊界集成測試
- [ ] 適當級別的日誌記錄
- [ ] 監測/指標儀表
```

## 審查報告格式

代理生成結構化審查報告：

```markdown
# Kiro 代碼審查報告

**審查者**: Kiro 代碼審查員代理
**日期**: 2025-11-16
**已審查文件**: 5

## 摘要
- ✅ 通過: 12 項檢查
- ⚠️  警告: 3 項
- ❌ 違規: 1 項

## 合規性得分: 85/100 (B+)

---

## 詳細發現

### 文件: OrderController.java

#### ✅ 優點
1. **冪等性**: 正確實現冪等性金鑰驗證
2. **邊界控制**: 使用 @Valid 進行全面輸入驗證
3. **錯誤處理**: 使用全局異常處理器

#### ⚠️  警告
1. **方法長度**: `createOrder()` 方法為 67 行（超過 50 行限制）
   - 位置: OrderController.java:45-112
   - 建議: 提取訂單建立邏輯到服務層

#### ❌ 違規
1. **遺漏驗證**: `updateOrderStatus()` 端點缺少 @Valid 註解
   - 位置: OrderController.java:150
   - 修正: 將 @Valid 添加到 UpdateOrderStatusRequest 參數

---

### 文件: OrderService.java

#### ✅ 優點
1. **無狀態**: 沒有可變實例字段
2. **分解**: 工作流步驟分解良好

#### ⚠️  建議
1. **不可變性**: 考慮為 UpdateOrderCommand 使用記錄類
   - 當前: 具有設置器的傳統類
   - 建議: 在緊湊構造函數中進行驗證的記錄

---

## 建議

### 高優先級（必須修正）
1. 將 @Valid 註解添加到 OrderController.updateOrderStatus() 參數
2. 確保更新操作的冪等性追蹤

### 中優先級（應該修正）
1. 重構長方法以符合 50 行限制
2. 將 DTO 轉換為記錄類以實現不可變性

### 低優先級（美好品質）
1. 添加更詳細的 JavaDoc
2. 增加邊界情況的測試覆蓋

---

## Kiro 合規性（按原則）

| 原則 | 得分 | 狀態 |
|-----------|-------|--------|
| 冪等性 | 90% | ✅ 良好 |
| 無狀態 | 100% | ✅ 優秀 |
| 不可變性 | 75% | ⚠️  需要改進 |
| 邊界控制 | 85% | ✅ 良好 |
| 工作流分解 | 80% | ✅ 良好 |

**整體合規性**: 86% (B+)

---

## 後續步驟

1. 修正上面列出的違規項目
2. 解決高優先級建議
3. 重新運行 Kiro 代碼審查
4. 考慮添加 ArchUnit 測試以強制執行這些原則
```

## 使用範例

### 透過 CLI 調用
```bash
claude-code review --agent=kiro-code-reviewer --files="app/src/main/java/**/*.java"
```

### 透過提示調用
```
Review the following code for Kiro compliance:
[paste code here]
```

### 自動調用
```yaml
# .github/workflows/kiro-review.yml
on: [pull_request]
jobs:
  kiro-review:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Kiro Code Review
        run: |
          claude-code review \
            --agent=kiro-code-reviewer \
            --files="${{ github.event.pull_request.changed_files }}" \
            --output=review-report.md
      - name: Comment on PR
        uses: actions/github-script@v6
        with:
          script: |
            const fs = require('fs');
            const report = fs.readFileSync('review-report.md', 'utf8');
            github.rest.issues.createComment({
              issue_number: context.issue.number,
              owner: context.repo.owner,
              repo: context.repo.repo,
              body: report
            });
```

## 配置

代理可以通過 `.claude/agents/kiro-code-reviewer.json` 進行配置：

```json
{
  "agent_type": "kiro-code-reviewer",
  "enabled": true,
  "auto_invoke_on": [
    "code-generation-complete",
    "before-commit"
  ],
  "review_scope": {
    "include_patterns": [
      "app/src/main/java/**/*.java",
      "app/src/test/java/**/*.java"
    ],
    "exclude_patterns": [
      "**/generated/**",
      "**/test/fixtures/**"
    ]
  },
  "severity_thresholds": {
    "fail_on_violations": false,
    "warn_on_score_below": 80,
    "fail_on_score_below": 60
  },
  "principle_weights": {
    "idempotency": 5,
    "stateless": 5,
    "immutability": 4,
    "boundary_control": 5,
    "workflow_decomposition": 4
  },
  "metrics": {
    "max_method_lines": 50,
    "max_class_lines": 300,
    "min_test_coverage": 80
  }
}
```

## 優點

1. **自動化品質門檻**: 在代碼審查前捕獲 Kiro 違規
2. **一致的標準**: 在整個代碼庫中強制執行相同原則
3. **學習工具**: 幫助開發人員理解 Kiro 原則
4. **時間節省**: 將手動代碼審查時間減少 40-60%
5. **主動性**: 在開發週期早期捕獲問題

## 與其他代理的集成

Kiro 代碼審查員可與以下代理配合使用：

- **架構驗證器 (Architecture Validator)**: 確保架構模式（DDD、六邊形架構）
- **測試生成器 (Test Generator)**: 為符合 Kiro 的代碼生成測試
- **文檔生成器 (Documentation Generator)**: 建立突出 Kiro 模式的文檔
- **重構代理 (Refactoring Agent)**: 建議重構以改進 Kiro 合規性
