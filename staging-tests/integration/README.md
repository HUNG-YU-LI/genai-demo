# 整合測試 Framework

此目錄包含按服務類型和測試範圍組織的應用程式的綜合整合測試。

## 測試結構

```
staging-tests/integration/
├── database/                    # 資料庫整合測試
│   ├── test_database_integration.py
│   └── requirements.txt
├── cache/                       # 快取整合測試
│   ├── test_redis_integration.py
│   └── requirements.txt
├── messaging/                   # 訊息傳遞整合測試
│   ├── test_kafka_integration.py
│   └── requirements.txt
└── monitoring/                  # 監控整合測試
```

## 測試類別

### 資料庫整合測試
- **PostgreSQL 連線池效能測試**
- **Aurora 容錯移轉和復原場景測試**
- **資料庫健康驗證和監控測試**
- **跨區域資料庫複寫測試**
- **測試資料管理和清理程序**

**需求**: 2.1、2.2、2.4、6.1

### 快取整合測試
- **Redis cluster 效能和可擴展性測試**
- **Redis Sentinel 容錯移轉場景測試**
- **跨區域快取同步測試**
- **快取驅逐和記憶體管理測試**
- **快取效能基準測試和驗證**

**需求**: 2.1、2.2、2.4、6.1

### 訊息傳遞整合測試
- **Kafka producer 和 consumer 吞吐量測試**
- **Kafka partition 重新平衡和容錯移轉測試**
- **跨區域訊息複寫測試**
- **訊息順序和遞送保證測試**
- **訊息傳遞效能基準測試和驗證**

**需求**: 2.1、2.2、2.4、6.1

## 執行測試

### 前置需求

1. **安裝 Python 依賴項**:
   ```bash
   pip install -r requirements.txt
   ```

2. **啟動所需服務** (使用 Docker Compose):
   ```bash
   cd staging-tests/config
   docker-compose -f docker-compose-staging.yml up -d
   ```

3. **等待服務就緒**:
   ```bash
   cd staging-tests/scripts
   ./wait-for-services.sh
   ```

### 執行個別測試套件

#### 資料庫測試
```bash
cd staging-tests/integration/database
python -m pytest test_database_integration.py -v
```

#### 快取測試
```bash
cd staging-tests/integration/cache
python -m pytest test_redis_integration.py -v
```

#### 訊息傳遞測試
```bash
cd staging-tests/integration/messaging
python -m pytest test_kafka_integration.py -v
```

### 執行所有整合測試
```bash
cd staging-tests
python run_integration_tests.py
```

### 執行特定測試類型
```bash
# 只執行資料庫測試
python run_integration_tests.py --test-type database

# 只執行快取測試
python run_integration_tests.py --test-type cache

# 只執行訊息傳遞測試
python run_integration_tests.py --test-type messaging

# 使用詳細日誌
python run_integration_tests.py --verbose
```

## 測試設定

### 資料庫設定
- **主機**: localhost
- **連接埠**: 5432 (primary)、5433 (replica)、5434 (replica)
- **資料庫**: test_db
- **使用者名稱**: test_user
- **密碼**: test_password

### 快取設定
- **Redis 主機**: localhost
- **Redis 連接埠**: 6379 (primary)、6380 (replica)、6381 (replica)
- **Sentinel 連接埠**: 26379
- **服務名稱**: mymaster

### 訊息傳遞設定
- **Kafka Brokers**: localhost:9092、localhost:9093、localhost:9094
- **主題**: 在測試期間自動建立
- **Consumer Groups**: 測試特定的群組

## 效能期望

### 資料庫測試
- **Connection Pool 效能**: < 500ms 平均回應時間
- **Aurora 容錯移轉**: < 30 秒
- **健康驗證**: < 100ms 查詢回應時間
- **跨區域複寫**: < 1000ms lag

### 快取測試
- **快取操作**: < 10ms 平均延遲
- **Sentinel 容錯移轉**: < 30 秒
- **跨區域同步**: < 1000ms 延遲
- **記憶體管理**: 在記憶體壓力下適當驅逐

### 訊息傳遞測試
- **Producer 吞吐量**: > 1000 訊息/秒
- **Consumer 吞吐量**: > 1000 訊息/秒
- **端對端延遲**: < 100ms 平均值
- **訊息順序**: 在單個 partition 中保留

## 疑難排解

### 常見問題

1. **服務連線失敗**
   - 確保 Docker 服務正在執行
   - 檢查連接埠可用性
   - 驗證網路連線

2. **測試逾時**
   - 增加測試設定中的逾時值
   - 檢查系統資源可用性
   - 監控服務效能

3. **資料一致性問題**
   - 驗證複寫設定
   - 檢查區域間的網路延遲
   - 監控同步程序

### 除錯

啟用詳細日誌：
```bash
python run_integration_tests.py --verbose
```

檢查服務日誌：
```bash
docker-compose -f config/docker-compose-staging.yml logs [service-name]
```

監控資源使用：
```bash
docker stats
```

## 擴展測試

### 新增測試案例

1. **在適當的測試套件類別中建立測試方法**
2. **遵循命名慣例**: `test_[functionality]_[scenario]`
3. **包含基於需求的效能斷言**
4. **在 teardown 方法中新增適當的清理**
5. **使用新的測試描述更新文件**

### 新增測試套件

1. **在 `integration/` 下建立新目錄**
2. **依照現有模式實現測試套件類別**
3. **新增包含特定依賴項的 requirements.txt**
4. **更新主測試執行器以包含新套件**
5. **如果需要，新增新服務的設定**

## 持續整合

這些測試設計為在 CI/CD 管道中執行：

- **AWS CodeBuild**: 查閱 `aws-codebuild/buildspec-integration-tests.yml`
- **本機開發**: 使用 `scripts/run-integration-tests.sh`
- **Staging 環境**: 程式碼變更時自動執行

## 監控和報告

測試結果包括：
- **每個操作的效能指標**
- **按測試類別的成功/失敗率**
- **測試執行期間的資源利用率**
- **失敗測試的詳細錯誤日誌**
- **效能回歸檢測的趨勢分析**