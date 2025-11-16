# Staging Test Framework 指南

## 概述

此綜合 staging test framework 為在 staging 環境中測試應用程式提供了堅實的基礎。其中包括：

- **基礎測試類別**，具備認證和重試邏輯
- **效能指標**收集和報告
- **AWS 服務整合**用於 CloudWatch、X-Ray 及其他服務
- **靈活設定**透過環境變數
- **Pytest 整合**搭配自訂 fixtures 和 markers

## 快速入門

### 1. 安裝

```bash
# 導航到 staging-tests 目錄
cd staging-tests

# 安裝依賴項
pip install -r requirements.txt

# 複製環境設定
cp .env.example .env

# 使用你的 staging 環境詳細資訊編輯 .env
nano .env
```

### 2. 設定

使用你的 staging 環境設定更新 `.env` 檔案：

```bash
# API 設定
STAGING_API_URL=https://api-staging.your-domain.com
AUTH_TOKEN=your_bearer_token_here

# AWS 設定
AWS_REGION=ap-northeast-1
AWS_PROFILE=staging  # 選用

# 資料庫設定
DB_HOST=your-aurora-endpoint.rds.amazonaws.com
DB_PORT=5432
DB_NAME=staging_db
DB_USER=staging_user
DB_PASSWORD=your_password
```

### 3. 執行測試

```bash
# 執行所有測試
pytest

# 執行特定測試套件
pytest examples/test_example_api.py

# 執行具有特定 markers 的測試
pytest -m smoke  # 只執行 smoke 測試
pytest -m "api and not slow"  # 執行 API 測試，排除緩慢的測試

# 執行測試並顯示詳細輸出
pytest -v

# 並行執行測試
pytest -n auto  # 使用所有可用的 CPU 核心

# 產生 HTML 報告
pytest --html=reports/test_report.html --self-contained-html
```

## Framework 元件

### 基礎測試類別

#### BaseStagingApiTest

用於 API 測試的基礎類別，具備內建功能：

- **認證**: 支援 Bearer、Basic 和 API Key 認證
- **重試邏輯**: 對暫時故障進行自動重試，具備指數退避
- **指標追蹤**: 收集所有 API 呼叫的效能指標
- **錯誤處理**: 區分可重試和不可重試的錯誤

**使用範例:**

```python
from base_staging_test import BaseStagingApiTest

class TestMyApi(BaseStagingApiTest):
    def test_create_resource(self):
        with self.track_test_metrics('test_create_resource'):
            # 發出 API 請求，自動重試
            response = self.post('/api/resources', data={'name': 'Test'})

            # 斷言回應
            self.assert_response_success(response, 201)
            self.assert_response_contains(response, 'id')
```

#### BaseStagingIntegrationTest

擴展 `BaseStagingApiTest` 以及 AWS 服務整合：

- **CloudWatch**: 發佈自訂指標和查詢日誌
- **X-Ray**: 分散式追蹤整合
- **RDS**: 資料庫服務整合
- **ElastiCache**: 快取服務整合
- **MSK**: Kafka 服務整合

**使用範例:**

```python
from base_staging_test import BaseStagingIntegrationTest

class TestAwsIntegration(BaseStagingIntegrationTest):
    def test_publish_metrics(self):
        with self.track_test_metrics('test_publish_metrics'):
            # 發佈指標到 CloudWatch
            self.publish_cloudwatch_metric(
                namespace='MyApp',
                metric_name='TestMetric',
                value=100.0,
                unit='Count'
            )
```

### 設定管理

#### StagingEnvironmentConfig

從環境變數載入設定的集中設定類別：

```python
from base_staging_test import StagingEnvironmentConfig

# 從環境載入設定
config = StagingEnvironmentConfig()

# 存取設定值
print(config.api_base_url)
print(config.aws_region)
print(config.db_host)
```

### 重試邏輯

#### @retry_on_failure Decorator

為任何函式新增重試邏輯的 decorator：

```python
from base_staging_test import retry_on_failure, RetryableError

@retry_on_failure(max_retries=3, delay=1.0, backoff=2.0)
def flaky_operation():
    # 這將以指數退避方式重試最多 3 次
    if random.random() < 0.5:
        raise RetryableError("Temporary failure")
    return "Success"
```

### 效能指標

#### TestMetrics

自動收集效能指標：

- 測試執行持續時間
- API 呼叫計數和持續時間
- 成功/失敗狀態
- 錯誤訊息

**範例:**

```python
class TestPerformance(BaseStagingApiTest):
    def test_with_metrics(self):
        with self.track_test_metrics('test_with_metrics'):
            # 所有 API 呼叫都自動被追蹤
            self.get('/api/resource/1')
            self.get('/api/resource/2')
            self.get('/api/resource/3')

        # 指標在測試清理時自動儲存
```

**產生的指標報告:**

```json
{
  "generated_at": "2025-10-09T10:30:00",
  "total_tests": 5,
  "successful_tests": 4,
  "failed_tests": 1,
  "total_duration_ms": 1234.56,
  "total_api_calls": 15,
  "tests": [
    {
      "test_name": "test_with_metrics",
      "duration_ms": 234.56,
      "success": true,
      "api_calls": 3,
      "avg_api_call_duration_ms": 78.19
    }
  ]
}
```

## 測試組織

### 測試 Markers

使用 pytest markers 對測試進行分類：

```python
@pytest.mark.smoke
def test_health_check():
    """快速 smoke 測試"""
    pass

@pytest.mark.integration
@pytest.mark.database
def test_database_connection():
    """資料庫整合測試"""
    pass

@pytest.mark.performance
@pytest.mark.slow
def test_load_handling():
    """效能測試 (緩慢)"""
    pass
```

### 測試結構

按功能組織測試：

```
staging-tests/
├── base_staging_test.py          # 基礎測試類別
├── examples/                      # 測試範例
│   └── test_example_api.py
├── integration/                   # 整合測試
│   ├── database/
│   ├── cache/
│   └── messaging/
├── performance/                   # 效能測試
├── security/                      # 安全測試
└── cross-region/                  # 跨區域測試
```

## 最佳實踐

### 1. 對指標使用 Context Managers

始終使用 `track_test_metrics` context manager：

```python
def test_example(self):
    with self.track_test_metrics('test_example'):
        # 測試程式碼在此
        pass
```

### 2. 清理測試資料

始終在 finally 區塊或 fixtures 中清理測試資料：

```python
def test_create_resource(self):
    resource_id = None
    try:
        response = self.post('/api/resources', data={'name': 'Test'})
        resource_id = response.json()['id']
        # 測試斷言
    finally:
        if resource_id:
            self.delete(f'/api/resources/{resource_id}')
```

### 3. 使用 Fixtures 進行常見設定

```python
@pytest.fixture(autouse=True)
def setup_test_data(self):
    """在每個測試前設定測試資料"""
    self.test_data = {'name': 'Test', 'value': 123}
    yield
    # 測試後清理
```

### 4. 處理可重試和不可重試的錯誤

```python
from base_staging_test import RetryableError, NonRetryableError

def make_request(self):
    response = self.get('/api/resource')

    if response.status_code >= 500:
        # 伺服器錯誤可重試
        raise RetryableError("Server error")
    elif response.status_code == 400:
        # 客戶端錯誤不可重試
        raise NonRetryableError("Bad request")
```

### 5. 使用斷言輔助函式

```python
def test_api_response(self):
    response = self.get('/api/resource')

    # 使用內建的斷言輔助函式
    self.assert_response_success(response, 200)
    self.assert_response_contains(response, 'id')
    self.assert_response_contains(response, 'name', 'Expected Name')
```

## 進階功能

### 並行測試執行

以並行方式執行測試以加快執行速度：

```bash
# 使用所有可用的 CPU 核心
pytest -n auto

# 使用特定數量的 workers
pytest -n 4
```

### 測試逾時

為緩慢的測試設定逾時：

```python
@pytest.mark.timeout(60)  # 60 秒逾時
def test_slow_operation(self):
    pass
```

### 自訂 Fixtures

為常見的測試場景建立自訂 fixtures：

```python
@pytest.fixture(scope='session')
def test_customer():
    """為整個測試工作階段建立測試客戶"""
    client = BaseStagingApiTest()
    response = client.post('/api/customers', data={'name': 'Test Customer'})
    customer_id = response.json()['id']

    yield customer_id

    # 在所有測試後清理
    client.delete(f'/api/customers/{customer_id}')
```

### 條件式測試執行

根據環境跳過測試：

```python
@pytest.mark.skipif(
    os.getenv('ENABLE_CROSS_REGION_TESTS') != 'true',
    reason="Cross-region tests disabled"
)
def test_cross_region_replication(self):
    pass
```

## 疑難排解

### 常見問題

#### 1. 認證失敗

```bash
# 驗證認證設定
echo $AUTH_TOKEN

# 手動測試認證
curl -H "Authorization: Bearer $AUTH_TOKEN" $STAGING_API_URL/health
```

#### 2. 連線逾時

```bash
# 在 .env 中增加逾時
API_TIMEOUT=60

# 或在測試程式碼中
response = self.get('/api/resource', timeout=60)
```

#### 3. AWS 認證

```bash
# 驗證 AWS 認證
aws sts get-caller-identity --profile staging

# 或使用環境變數
export AWS_ACCESS_KEY_ID=your_key
export AWS_SECRET_ACCESS_KEY=your_secret
```

### 除錯模式

啟用詳細日誌：

```bash
# 執行測試並啟用除錯日誌
pytest -v --log-cli-level=DEBUG

# 或在環境中設定
export LOG_LEVEL=DEBUG
```

### 測試報告

產生詳細的測試報告：

```bash
# HTML 報告
pytest --html=reports/test_report.html --self-contained-html

# JUnit XML 報告 (用於 CI/CD)
pytest --junitxml=reports/junit.xml

# 覆蓋範圍報告
pytest --cov=. --cov-report=html
```

## 與 CI/CD 整合

### GitHub Actions 範例

```yaml
name: Staging Tests

on:
  push:
    branches: [main, develop]
  schedule:
    - cron: '0 2 * * *'  # 每日上午 2 點

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3

      - name: 設定 Python
        uses: actions/setup-python@v4
        with:
          python-version: '3.11'

      - name: 安裝依賴項
        run: |
          cd staging-tests
          pip install -r requirements.txt

      - name: 執行測試
        env:
          STAGING_API_URL: ${{ secrets.STAGING_API_URL }}
          AUTH_TOKEN: ${{ secrets.AUTH_TOKEN }}
          AWS_REGION: ap-northeast-1
        run: |
          cd staging-tests
          pytest -v --html=reports/test_report.html

      - name: 上傳測試結果
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: test-results
          path: staging-tests/reports/
```

## 貢獻

### 新增測試套件

1. 按命名慣例建立新的測試檔案: `test_*.py`
2. 繼承自 `BaseStagingApiTest` 或 `BaseStagingIntegrationTest`
3. 新增適當的 pytest markers
4. 為所有測試方法包含 docstrings
5. 在 teardown 中清理測試資料

### 新增新功能

1. 在 `base_staging_test.py` 中以新功能更新
2. 在 `examples/` 中新增新功能的測試
3. 在本指南中更新使用範例
4. 如果需要新依賴項，更新 `requirements.txt`

## 支援

如有疑問或問題：

1. 查閱本指南和測試範例
2. 檢視測試執行日誌
3. 檢查環境設定
4. 聯絡開發團隊

## 參考資源

- [Pytest 文件](https://docs.pytest.org/)
- [Requests 函式庫](https://requests.readthedocs.io/)
- [Boto3 文件](https://boto3.amazonaws.com/v1/documentation/api/latest/index.html)
- [AWS SDK for Python](https://aws.amazon.com/sdk-for-python/)
