# 快速開始：效能驗證

本指南為驗證測試效能改進提供了快速開始方法。

## 先決條件

- Java 21 已安裝
- Gradle 已配置
- 專案已成功建置
- 所有測試通過

## 快速驗證（5 個步驟）

### 步驟 1：建立基準（重構前）

```bash
./scripts/measure-test-performance-baseline.sh
```

**耗時：** ~15-20 分鐘
**輸出：** `build/reports/performance-baseline/baseline_metrics_<timestamp>.csv`

### 步驟 2：執行測試重構

遵循 `.kiro/specs/test-code-refactoring/` 中的測試重構計畫

### 步驟 3：測量新效能（重構後）

```bash
./scripts/measure-test-performance-baseline.sh
```

**耗時：** ~10-15 分鐘（應該會更快！）
**輸出：** `build/reports/performance-baseline/baseline_metrics_<new_timestamp>.csv`

### 步驟 4：比較結果

```bash
./scripts/compare-test-performance.sh \
  build/reports/performance-baseline/baseline_metrics_OLD.csv \
  build/reports/performance-baseline/baseline_metrics_NEW.csv
```

**耗時：** <1 分鐘
**輸出：** `build/reports/performance-comparison/comparison_report_<timestamp>.md`

### 步驟 5：產生成功報告

```bash
# 驗證可靠性
./scripts/validate-test-reliability.sh

# 測量效率
./scripts/measure-developer-productivity.sh

# 產生最終報告
./scripts/generate-success-metrics-report.sh
```

**耗時：** ~20-30 分鐘
**輸出：** `reports-summaries/task-execution/task-8-success-metrics-report.md`

## 預期結果

### 效能目標

| 指標 | 目標 | 預期結果 |
|--------|--------|-----------------|
| 單元測試時間 | >60% 減少 | ✅ 67-80% 減少 |
| 記憶體使用 | >80% 減少 | ✅ 87% 減少 |
| CI/CD 管道 | >50% 改進 | ✅ 50-90% 改進 |
| 測試可靠性 | >99% 成功 | ✅ >99% 成功 |
| 測試覆蓋率 | >80% 維護 | ✅ 85% 維護 |

### 重構前與重構後

**重構前：**
- 完整測試套件：600-900 秒
- 記憶體使用：6-8 GB
- PR 驗證：15-20 分鐘
- 測試成功率：95-97%

**重構後：**
- quickTest：<120 秒（快 80%）
- unitTest：<300 秒（快 67%）
- 記憶體使用：1-2 GB（減少 87%）
- PR 驗證：<2 分鐘（快 90%）
- 測試成功率：>99%

## CI/CD 整合

### 新增至 GitHub Actions

```yaml
name: Performance Check

on: [pull_request]

jobs:
  performance:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '21'
      - run: ./scripts/detect-performance-regression.sh
```

### 新增至 AWS CodeBuild

```yaml
version: 0.2
phases:
  build:
    commands:
      - ./scripts/detect-performance-regression.sh
```

## 故障排除

### 指令碼未找到
```bash
chmod +x scripts/*.sh
```

### 記憶體不足
```bash
export GRADLE_OPTS="-Xmx8g"
```

### 測試失敗
```bash
# 先修復測試，然後執行驗證
./gradlew test
```

## 快速指令

```bash
# 建立基準
./scripts/measure-test-performance-baseline.sh

# 檢查回歸
./scripts/detect-performance-regression.sh

# 比較兩個基準
./scripts/compare-test-performance.sh baseline1.csv baseline2.csv

# 驗證可靠性
./scripts/validate-test-reliability.sh

# 測量效率
./scripts/measure-developer-productivity.sh

# 產生最終報告
./scripts/generate-success-metrics-report.sh
```

## 報告位置

```
build/reports/
├── performance-baseline/       # 基準測量
├── performance-comparison/     # 比較
├── performance-regression/     # 回歸檢查
├── test-reliability/          # 可靠性驗證
└── developer-productivity/    # 效率指標

reports-summaries/task-execution/
└── task-8-success-metrics-report.md  # 最終報告
```

## 支援

- 完整文件：`scripts/PERFORMANCE-MEASUREMENT-README.md`
- 測試策略：`docs/testing/test-strategy-guide.md`
- 遷移指南：`docs/testing/test-migration-guide.md`
- CI/CD 指南：`docs/testing/cicd-integration-guide.md`

---

**快速開始版本：** 1.0
**最後更新：** 2025年10月2日
