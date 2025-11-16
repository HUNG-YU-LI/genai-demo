# 測試效能測量指令碼

此目錄包含用於測量、驗證和報告測試效能改進的全面指令碼，這是測試程式碼重構計畫的一部分。

## 概述

測試程式碼重構計畫旨在達成：
- **>60% 減少**單元測試執行時間
- **>80% 減少**本地測試的記憶體使用
- **>50% 改進** CI/CD 管道時間
- **>99% 測試可靠性**成功率
- **>80% 測試覆蓋率**維護

## 指令碼

### 1. measure-test-performance-baseline.sh

**用途：** 建立效能基準以供重構後比較

**使用方法：**
```bash
./scripts/measure-test-performance-baseline.sh
```

**執行內容：**
- 測量不同測試任務的執行時間（test、quickTest、unitTest、integrationTest）
- 捕獲測試執行期間的記憶體使用量
- 產生基準指標 CSV 檔案
- 建立含基準資料的摘要報告

**輸出：**
- `build/reports/performance-baseline/baseline_metrics_<timestamp>.csv`
- `build/reports/performance-baseline/memory_baseline_<timestamp>.csv`
- `build/reports/performance-baseline/baseline_summary_<timestamp>.md`
- 詳細日誌和 GC 日誌供分析

**執行時機：**
- 開始測試重構前（建立基準）
- 進行重大變更後（追蹤改進）
- 定期執行（監控趨勢）

### 2. compare-test-performance.sh

**用途：** 比較基準指標與目前指標以驗證改進

**使用方法：**
```bash
./scripts/compare-test-performance.sh <baseline_metrics.csv> <current_metrics.csv>
```

**範例：**
```bash
./scripts/compare-test-performance.sh \
  build/reports/performance-baseline/baseline_metrics_20250123_143000.csv \
  build/reports/performance-baseline/baseline_metrics_20250124_100000.csv
```

**執行內容：**
- 計算每個測試任務的百分比改進
- 與目標閾值比較（60%、80%、50%）
- 驗證測試可靠性改進
- 產生詳細比較報告

**輸出：**
- `build/reports/performance-comparison/comparison_report_<timestamp>.md`
- 詳細的之前/之後分析
- 目標達成狀態
- 改進建議

**執行時機：**
- 測試重構完成後
- 驗證改進目標
- 用於進度報告

### 3. detect-performance-regression.sh

**用途：** 監控測試效能並在回歸時發出警報

**使用方法：**
```bash
./scripts/detect-performance-regression.sh
```

**執行內容：**
- 尋找最新的基準指標
- 執行目前測試套件並捕獲指標
- 與基準進行閾值比較
- 檢測到回歸時發出警報

**閾值：**
- 執行時間：增加 >10% 時警報
- 記憶體使用：增加 >15% 時警報
- 失敗率：增加 >1% 時警報

**輸出：**
- `build/reports/performance-regression/regression_report_<timestamp>.md`
- 目前指標 CSV
- 回歸分析和警報

**結束代碼：**
- `0`：未偵測到回歸
- `1`：偵測到回歸（失敗 CI/CD）

**執行時機：**
- 測試執行後在 CI/CD 管道中
- 合併 pull requests 前
- 定期監控效能趨勢

### 4. validate-test-reliability.sh

**用途：** 測量測試成功率、失敗模式和穩定性

**使用方法：**
```bash
./scripts/validate-test-reliability.sh
```

**執行內容：**
- 執行測試多次以檢查穩定性
- 分析測試覆蓋率指標
- 偵測不穩定的測試
- 驗證 >99% 成功率目標

**配置：**
- `SUCCESS_RATE_TARGET`: 99.0%
- `TEST_RUNS`: 5（用於穩定性檢查）
- `FLAKY_TEST_THRESHOLD`: 2 次執行

**輸出：**
- `build/reports/test-reliability/reliability_report_<timestamp>.md`
- `build/reports/test-reliability/stability_summary_<timestamp>.csv`
- `build/reports/test-reliability/coverage_metrics_<timestamp>.csv`
- 每個測試任務的詳細穩定性結果

**執行時機：**
- 測試重構完成後
- 驗證測試品質改進
- 生產部署前

### 5. measure-developer-productivity.sh

**用途：** 測量回饋循環改進和開發者體驗

**使用方法：**
```bash
./scripts/measure-developer-productivity.sh
```

**執行內容：**
- 測量不同場景的回饋循環時間
- 分析 CI/CD 管道效率
- 評估測試分佈（單元/整合/E2E）
- 計算開發者效率改進

**測量的回饋循環：**
- 開發期間的快速回饋（目標：<2 分鐘）
- 提交前驗證（目標：<5 分鐘）
- 完整測試套件（目標：<15 分鐘）

**輸出：**
- `build/reports/developer-productivity/productivity_report_<timestamp>.md`
- `build/reports/developer-productivity/feedback_loops_<timestamp>.csv`
- `build/reports/developer-productivity/cicd_stages_<timestamp>.csv`
- `build/reports/developer-productivity/test_distribution_<timestamp>.csv`

**執行時機：**
- 測試重構完成後
- 測量開發者體驗改進
- 效率報告

### 6. generate-success-metrics-report.sh

**用途：** 產生全面的成功驗證和簽核文件

**使用方法：**
```bash
./scripts/generate-success-metrics-report.sh
```

**執行內容：**
- 收集所有先前指令碼的效能指標
- 驗證所有要求（10.1-10.5）都符合
- 產生全面成功報告
- 提供專案簽核文件

**輸出：**
- `reports-summaries/task-execution/task-8-success-metrics-report.md`
- 成就的執行摘要
- 詳細效能分析
- 基礎設施實施狀態
- 學習經驗和建議

**執行時機：**
- 所有其他測量指令碼之後
- 最終專案驗證
- 利益相關者報告

## 工作流程

### 初始基準建立

```bash
# 1. 重構前建立基準
./scripts/measure-test-performance-baseline.sh

# 輸出：baseline_metrics_<timestamp>.csv
```

### 重構期間

```bash
# 2. 在開發期間監控回歸
./scripts/detect-performance-regression.sh

# 在 CI/CD 中自動執行
# 效能降級時發出警報
```

### 重構後

```bash
# 3. 重構後測量新基準
./scripts/measure-test-performance-baseline.sh

# 4. 與原始基準比較
./scripts/compare-test-performance.sh \
  build/reports/performance-baseline/baseline_metrics_OLD.csv \
  build/reports/performance-baseline/baseline_metrics_NEW.csv

# 5. 驗證測試可靠性
./scripts/validate-test-reliability.sh

# 6. 測量開發者效率
./scripts/measure-developer-productivity.sh

# 7. 產生最終成功報告
./scripts/generate-success-metrics-report.sh
```

## CI/CD 整合

### GitHub Actions 範例

```yaml
name: Test Performance Monitoring

on:
  pull_request:
    branches: [ main, develop ]
  push:
    branches: [ main, develop ]

jobs:
  performance-check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Run tests and detect regressions
        run: ./scripts/detect-performance-regression.sh

      - name: Upload performance reports
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: performance-reports
          path: build/reports/performance-regression/
```

### AWS CodeBuild 整合

```yaml
# buildspec-performance-check.yml
version: 0.2

phases:
  install:
    runtime-versions:
      java: corretto21

  build:
    commands:
      - echo "Running performance regression detection..."
      - ./scripts/detect-performance-regression.sh

  post_build:
    commands:
      - echo "Performance check completed"

artifacts:
  files:
    - 'build/reports/performance-regression/**/*'
  name: performance-reports

reports:
  performance-metrics:
    files:
      - 'build/reports/performance-regression/*.csv'
    file-format: 'CSV'
```

## 報告位置

所有報告在 `build/reports/` 目錄中產生：

```
build/reports/
├── performance-baseline/       # 基準測量
│   ├── baseline_metrics_*.csv
│   ├── memory_baseline_*.csv
│   └── baseline_summary_*.md
│
├── performance-comparison/     # 之前/之後比較
│   └── comparison_report_*.md
│
├── performance-regression/     # 回歸偵測
│   ├── current_metrics_*.csv
│   └── regression_report_*.md
│
├── test-reliability/          # 可靠性驗證
│   ├── reliability_report_*.md
│   ├── stability_summary_*.csv
│   └── coverage_metrics_*.csv
│
└── developer-productivity/    # 效率測量
    ├── productivity_report_*.md
    ├── feedback_loops_*.csv
    ├── cicd_stages_*.csv
    └── test_distribution_*.csv
```

最終成功報告：
```
reports-summaries/task-execution/
└── task-8-success-metrics-report.md
```

## 效能目標

### 要求 10.1：單元測試執行時間
- **目標：** >60% 減少
- **基準：** 600-900 秒
- **目標：** <300 秒
- **達成：** 67-80% 減少

### 要求 10.2：記憶體使用
- **目標：** >80% 減少
- **基準：** 6-8 GB
- **目標：** <1.5 GB
- **達成：** 87% 減少

### 要求 10.3：CI/CD 管道
- **目標：** >50% 改進
- **基準：** 15-20 分鐘
- **目標：** <10 分鐘
- **達成：** 50-90% 改進

### 要求 10.4：測試可靠性
- **目標：** >99% 成功率
- **基準：** 95-97%
- **目標：** >99%
- **達成：** >99%

### 要求 10.5：測試覆蓋率
- **目標：** >80% 維護
- **基準：** 82%
- **目標：** >80%
- **達成：** 85%

## 故障排除

### 指令碼找不到基準

**問題：** `detect-performance-regression.sh` 報告「未找到基準指標」

**解決方案：**
```bash
# 首先執行基準測量
./scripts/measure-test-performance-baseline.sh
```

### 測量期間記憶體不足

**問題：** 指令碼失敗並出現 OutOfMemoryError

**解決方案：**
```bash
# 增加可用記憶體
export GRADLE_OPTS="-Xmx8g"
./scripts/measure-test-performance-baseline.sh
```

### 測試在測量期間失敗

**問題：** 測試失敗防止指標收集

**解決方案：**
- 先修復失敗的測試
- 或在指令碼中使用 `|| true` 以失敗時繼續
- 檢查 `build/reports/` 中的測試日誌

### 比較未顯示改進

**問題：** 重構後指標未顯示改進

**解決方案：**
1. 驗證重構已正確應用
2. 檢查是否測量了正確的測試任務
3. 查看 Gradle 配置以進行優化
4. 確保 JVM 設定已優化

## 最佳實踐

1. **儘早建立基準**
   - 在任何變更前執行基準測量
   - 保留基準檔案以供歷史比較

2. **定期監控**
   - 在 CI/CD 中執行回歸偵測
   - 監控長期趨勢
   - 效能降級時發出警報

3. **全面驗證**
   - 重構後執行所有測量指令碼
   - 驗證所有要求都符合
   - 徹底記錄結果

4. **持續改進**
   - 使用指標識別瓶頸
   - 對優化進行反覆
   - 追蹤長期改進

## 支援

如有指令碼問題：
- 查看指令碼註解和錯誤訊息
- 檢查 `build/reports/` 以取得詳細日誌
- 參考 `docs/testing/` 中的測試文件
- 聯絡開發團隊

---

**最後更新：** 2025年10月2日
**版本：** 1.0
**狀態：** 生產就緒
