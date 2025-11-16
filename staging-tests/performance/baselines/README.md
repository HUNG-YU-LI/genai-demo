# 效能基準

此目錄包含不同測試場景的效能基準。基準用於檢測效能回歸和追蹤效能趨勢。

## 概述

效能基準提供了參考點，用於將目前測試結果與歷史效能進行比較。它們可幫助識別：

- 效能回歸 (相對於基準的效能下降)
- 效能改進 (優於基準)
- 效能穩定性 (與基準一致)
- 長期效能趨勢

## 基準檔案

每個測試場景都有自己的基準檔案：

- `normal-load_baseline.json` - 常規負載測試基準
- `peak-load_baseline.json` - 尖峰負載測試基準
- `stress-test_baseline.json` - 壓力測試基準
- `endurance-test_baseline.json` - 耐久性測試基準
- `spike-test_baseline.json` - 尖峰測試基準

## 基準結構

每個基準檔案包含：

```json
{
  "test_scenario": "normal-load",
  "created_at": "2025-01-21T10:30:00Z",
  "updated_at": "2025-01-21T15:45:00Z",
  "version": "1.2",
  "data_points": 10,
  "baseline_metrics": {
    "mean_response_time": {
      "value": 850.5,
      "unit": "ms",
      "confidence_interval": [820.2, 880.8],
      "std_deviation": 45.3
    },
    "p95_response_time": {
      "value": 1250.0,
      "unit": "ms",
      "confidence_interval": [1200.0, 1300.0],
      "std_deviation": 75.5
    },
    "throughput": {
      "value": 125.8,
      "unit": "req/s",
      "confidence_interval": [120.0, 131.6],
      "std_deviation": 8.2
    },
    "success_rate": {
      "value": 99.2,
      "unit": "%",
      "confidence_interval": [98.8, 99.6],
      "std_deviation": 0.4
    }
  },
  "regression_thresholds": {
    "critical": {
      "response_time_increase_percent": 50,
      "throughput_decrease_percent": 30,
      "success_rate_decrease_percent": 5
    },
    "high": {
      "response_time_increase_percent": 30,
      "throughput_decrease_percent": 20,
      "success_rate_decrease_percent": 3
    }
  },
  "metadata": {
    "calculation_method": "percentile",
    "percentile": 75,
    "outliers_removed": 2,
    "source_files": [
      "results/20250121_103000/normal-load-results.json",
      "results/20250121_113000/normal-load-results.json"
    ]
  }
}
```

## 建立基準

### 自動建立

執行效能測試時會自動建立基準：

```bash
# 執行測試並建立基準
./scripts/run-performance-tests.sh --scenario normal --baseline

# 從現有結果建立基準
python scripts/performance-baseline.py create \
  --test-name normal-load \
  --results-file results/normal-load-results.json \
  --description "Initial baseline for normal load"
```

### 手動建立

你可以從多個測試結果手動建立基準：

```bash
# 從多個結果檔案建立基準
python scripts/performance-baseline.py create \
  --test-name normal-load \
  --results-files results/run1.json results/run2.json results/run3.json \
  --description "Baseline from 3 test runs"
```

## 更新基準

### 移動平均更新

使用最近結果的移動平均更新基準：

```bash
python scripts/performance-baseline.py update \
  --test-name normal-load \
  --results-file results/latest-results.json \
  --strategy rolling_average
```

### 加權平均更新

使用加權平均更新基準：

```bash
python scripts/performance-baseline.py update \
  --test-name normal-load \
  --results-file results/latest-results.json \
  --strategy weighted_average \
  --new-weight 0.3
```

### 取代基準

完全取代現有基準：

```bash
python scripts/performance-baseline.py update \
  --test-name normal-load \
  --results-file results/latest-results.json \
  --strategy replace
```

## 回歸檢測

### 與基準進行比較

```bash
# 將目前結果與基準進行比較
python scripts/performance-baseline.py compare \
  --test-name normal-load \
  --results-file results/current-results.json \
  --output regression-analysis.json
```

### 自動回歸檢測

回歸檢測在效能測試期間自動執行：

```bash
# 執行測試並進行回歸檢測
./scripts/run-performance-tests.sh --scenario normal --baseline --alerts
```

## 回歸閾值

效能回歸的不同嚴重程度：

### 嚴重回歸
- 回應時間增加 > 50%
- 吞吐量下降 > 30%
- 成功率下降 > 5%

### 高回歸
- 回應時間增加 > 30%
- 吞吐量下降 > 20%
- 成功率下降 > 3%

### 中等回歸
- 回應時間增加 > 20%
- 吞吐量下降 > 15%
- 成功率下降 > 2%

### 低回歸
- 回應時間增加 > 10%
- 吞吐量下降 > 10%
- 成功率下降 > 1%

## 基準管理

### 列出基準

```bash
# 列出所有基準
python scripts/performance-baseline.py list

# 列出特定場景的基準
python scripts/performance-baseline.py list --test-name normal-load
```

### 基準資訊

```bash
# 取得基準詳細資訊
python scripts/performance-baseline.py info --test-name normal-load
```

### 備份基準

```bash
# 備份所有基準
python scripts/performance-baseline.py backup

# 備份特定基準
python scripts/performance-baseline.py backup --test-name normal-load
```

### 還原基準

```bash
# 從備份還原
python scripts/performance-baseline.py restore --backup-file backup/normal-load_baseline_20250121.json
```

## 設定

基準行為在 `baseline-config.yml` 中設定：

### 主要設定選項

- **計算方法**: mean、median、percentile
- **異常值偵測**: IQR、Z-score、isolation forest
- **更新策略**: rolling_average、weighted_average、replace
- **回歸閾值**: 按嚴重程度
- **保留策略**: 基準保留多久

### 測試場景特定設定

每個測試場景都可以有自訂設定：

```yaml
test_scenarios:
  normal_load:
    baseline_creation:
      min_test_runs: 3
      calculation_method: "median"
    regression_detection:
      thresholds:
        critical:
          response_time_increase_percent: 40
```

## 最佳實踐

### 基準建立

1. **充分的資料**: 用於基準建立至少使用 3-5 次測試執行
2. **穩定的環境**: 在一致的測試環境中建立基準
3. **代表性負載**: 確保測試場景代表實際使用情況
4. **清潔資料**: 移除異常值和失敗的測試執行

### 基準維護

1. **定期更新**: 在重大改進後更新基準
2. **版本控制**: 保留基準版本以便進行回復
3. **文件**: 記錄基準變更和原因
4. **驗證**: 在重大系統變更後驗證基準

### 回歸檢測

1. **適當的閾值**: 根據業務需求設定閾值
2. **統計顯著性**: 使用統計測試進行回歸確認
3. **情境感知**: 考慮部署和環境變更
4. **趨勢分析**: 查看趨勢，而不僅是個別回歸

## 疑難排解

### 常見問題

#### 基準建立失敗

```bash
# 檢查是否有足夠的資料點
python scripts/performance-baseline.py info --test-name normal-load

# 驗證結果檔案格式
python scripts/performance-baseline.py validate --results-file results/test-results.json
```

#### 基準變異性高

```bash
# 檢查變異係數
python scripts/performance-baseline.py analyze --test-name normal-load

# 檢視 baseline-config.yml 中的異常值偵測設定
```

#### 假陽性回歸

```bash
# 在 baseline-config.yml 中調整回歸閾值
# 啟用統計顯著性測試
# 增加連續回歸要求
```

### 除錯模式

啟用除錯模式以取得詳細日誌：

```bash
python scripts/performance-baseline.py --debug compare \
  --test-name normal-load \
  --results-file results/current-results.json
```

## 整合

### CI/CD 管道

```yaml
# GitHub Actions 範例
- name: Performance Test with Baseline
  run: |
    ./scripts/run-performance-tests.sh --scenario normal --baseline

- name: Check for Regressions
  run: |
    python scripts/performance-baseline.py compare \
      --test-name normal-load \
      --results-file results/normal-load-results.json \
      --fail-on-regression
```

### 監控整合

```bash
# 將基準指標傳送到監控系統
python scripts/performance-baseline.py export \
  --test-name normal-load \
  --format prometheus \
  --output metrics.txt
```

## 檔案命名慣例

- 基準檔案: `{test_scenario}_baseline.json`
- 備份檔案: `{test_scenario}_baseline_{timestamp}.json`
- 版本檔案: `{test_scenario}_baseline_v{version}.json`

## 目錄結構

```
baselines/
├── README.md                           # 本檔案
├── normal-load_baseline.json           # 常規負載基準
├── peak-load_baseline.json             # 尖峰負載基準
├── stress-test_baseline.json           # 壓力測試基準
├── endurance-test_baseline.json        # 耐久性測試基準
├── spike-test_baseline.json            # 尖峰測試基準
├── backup/                             # 基準備份
│   ├── normal-load_baseline_20250121.json
│   └── peak-load_baseline_20250120.json
└── versions/                           # 基準版本
    ├── normal-load_baseline_v1.0.json
    └── normal-load_baseline_v1.1.json
```

此基準系統確保一致的效能監控，並幫助維持應用程式效能品質。