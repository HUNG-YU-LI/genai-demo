# Test Performance Standards and Monitoring

## 概覽

本文件提供在我們的 Spring Boot 應用程式中進行測試效能監控、資源管理和優化的專業參考。

> **📋 主要標準**：基本的測試效能標準請參考 Development Standards

> **🎯 用途**：本文件作為測試效能監控的深度技術參考，包含詳細的實作指南和故障排除

## Test Performance Framework

### 核心元件

#### 1. TestPerformanceExtension

基於 annotation 的效能監控，用於自動追蹤測試效能。

```java
@TestPerformanceExtension(maxExecutionTimeMs = 10000, maxMemoryIncreaseMB = 100)
@IntegrationTest
public class MyIntegrationTest extends BaseIntegrationTest {
    // Tests are automatically monitored for performance
}
```

**配置選項：**

- `maxExecutionTimeMs`：最大允許執行時間（預設：5000ms）
- `maxMemoryIncreaseMB`：最大允許記憶體增加（預設：50MB）
- `generateReports`：是否生成詳細報告（預設：true）
- `checkRegressions`：是否檢查效能衰退（預設：true）

**實作細節：**

- 使用 `@ExtendWith(TestPerformanceMonitor.class)` 實作為 JUnit 5 extension
- 提供自動測試執行時間監控和記憶體使用追蹤
- 在 `build/reports/test-performance/` 生成詳細的執行報告
- 支援 class 級別和 method 級別的應用

#### 2. TestPerformanceMonitor

JUnit 5 extension，提供全面的測試效能監控。

**功能：**

- 毫秒精度的測試執行時間追蹤
- 記憶體使用監控（每個測試前後的 heap memory）
- 可配置閾值的效能衰退偵測
- 詳細的文字報告（HTML 報告由 TestPerformanceReportGenerator 生成）
- 慢速測試識別（>5s 警告，>30s 錯誤）
- 使用執行緒安全資料結構的並發測試執行追蹤
- 在 `build/reports/test-performance/` 自動生成報告

**效能閾值：**

- Slow Test Warning：> 5 秒
- Very Slow Test Error：> 30 秒
- Memory Usage Warning：> 50MB 增加

#### 3. TestPerformanceResourceManager

用於監控和管理測試資源的元件。

```java
@TestComponent
public class TestPerformanceResourceManager {

    public ResourceUsageStats getResourceUsageStats() {
        // Returns current resource usage statistics including:
        // - Current memory usage and maximum available
        // - Memory usage percentage
        // - Active test resources count
    }

    public void forceCleanup() {
        // Forces cleanup of all test resources
        // Triggers System.gc() to free memory
    }
}
```

**ResourceUsageStats 包含：**

- 執行的測試總數
- 目前使用的記憶體 vs 最大可用記憶體
- 記憶體使用百分比計算
- 測試執行期間分配的總記憶體
- 活動測試資源數量

#### 4. TestPerformanceConfiguration

用於效能監控設定的 Spring Test 配置。

```java
@TestConfiguration
@Profile("test")
public class TestPerformanceConfiguration {

    @Bean
    public TestPerformanceListener testPerformanceListener() {
        return new TestPerformanceListener();
    }
}
```

**TestPerformanceListener 提供：**

- 每個測試方法前後自動清理
- 正確處理外鍵約束的資料庫清理
- 測試之間的快取清除
- Mock 重置功能
- 應用程式狀態重置
- 臨時資源清理
- 測試類別完成後的最終清理

## Gradle Test Task 配置

### 優化的測試任務

```gradle
// Unit tests - fast feedback for daily development
tasks.register('unitTest', Test) {
    description = 'Fast unit tests (~5MB, ~50ms each)'
    useJUnitPlatform {
        excludeTags 'integration', 'end-to-end', 'slow'
        includeTags 'unit'
    }
    maxHeapSize = '2g'
    maxParallelForks = Runtime.runtime.availableProcessors()
    forkEvery = 0  // No JVM restart for speed
}

// Integration tests - pre-commit verification
tasks.register('integrationTest', Test) {
    description = 'Integration tests (~50MB, ~500ms each)'
    useJUnitPlatform {
        includeTags 'integration'
        excludeTags 'end-to-end', 'slow'
    }
    maxHeapSize = '6g'
    minHeapSize = '2g'
    maxParallelForks = 1
    forkEvery = 5
    timeout = Duration.ofMinutes(30)

    // HttpComponents optimization and JVM tuning
    jvmArgs += [
        '--enable-preview',
        '-XX:MaxMetaspaceSize=1g',
        '-XX:+UseG1GC',
        '-XX:+UseStringDeduplication',
        '-XX:G1HeapRegionSize=32m',
        '-XX:+UnlockExperimentalVMOptions',
        '-XX:G1NewSizePercent=20',
        '-XX:G1MaxNewSizePercent=30',
        '-Xshare:off',
        // HttpComponents specific JVM parameters
        '-Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.SimpleLog',
        '-Dorg.apache.commons.logging.simplelog.showdatetime=true',
        '-Dorg.apache.commons.logging.simplelog.log.org.apache.http=DEBUG',
        '-Dorg.apache.http.wire=DEBUG',
        // Network timeout configuration
        '-Dsun.net.useExclusiveBind=false',
        '-Djava.net.preferIPv4Stack=true'
    ]

    // Enhanced system properties for integration tests
    systemProperties = [
        'junit.jupiter.execution.timeout.default': '2m',
        'spring.profiles.active': 'test',
        'http.client.connection.timeout': '10000',
        'http.client.socket.timeout': '30000',
        'test.resource.cleanup.enabled': 'true',
        'test.memory.monitoring.enabled': 'true'
    ]
}

// End-to-end tests - pre-release verification
tasks.register('e2eTest', Test) {
    description = 'End-to-end tests (~500MB, ~3s each)'
    useJUnitPlatform {
        includeTags 'end-to-end'
    }
    maxHeapSize = '8g'
    minHeapSize = '3g'
    maxParallelForks = 1
    forkEvery = 2
    timeout = Duration.ofHours(1)

    // E2E test specific JVM parameters
    jvmArgs += [
        '--enable-preview',
        '-XX:MaxMetaspaceSize=2g',
        '-XX:+UseG1GC',
        '-XX:+UseStringDeduplication',
        '-XX:G1HeapRegionSize=32m',
        '-XX:+UnlockExperimentalVMOptions',
        '-XX:G1NewSizePercent=20',
        '-XX:G1MaxNewSizePercent=30',
        '-Xshare:off',
        '-Djava.security.egd=file:/dev/./urandom'
    ]

    // E2E test system properties
    systemProperties = [
        'junit.jupiter.execution.timeout.default': '5m',
        'spring.profiles.active': 'test',
        'spring.main.lazy-initialization': 'false',
        'http.client.connection.timeout': '30000',
        'http.client.socket.timeout': '60000',
        'test.performance.monitoring.enabled': 'true'
    ]
}
```

### 測試任務階層

```bash
# Development workflow
./gradlew quickTest              # Daily development (< 2 minutes)
./gradlew preCommitTest          # Pre-commit verification (< 5 minutes)
./gradlew fullTest               # Pre-release verification (< 30 minutes)

# Specific test types
./gradlew unitTest               # Unit tests only
./gradlew integrationTest        # Integration tests only
./gradlew e2eTest               # End-to-end tests only
./gradlew cucumber              # BDD Cucumber tests
```

## 效能閾值和監控

### 效能閾值

- **Slow Test Warning**：> 5 秒
- **Very Slow Test Error**：> 30 秒
- **Memory Usage Warning**：> 50MB 增加
- **Memory Usage Critical**：> 80% 可用 heap

### 自動效能監控

#### 測試執行監控

```java
// Automatic monitoring with TestPerformanceMonitor
public class TestPerformanceMonitor implements BeforeAllCallback, AfterAllCallback,
        BeforeEachCallback, AfterEachCallback, TestWatcher {

    // Automatically tracks:
    // - Test execution times
    // - Memory usage during tests
    // - Performance regressions
    // - Resource cleanup
}
```

#### 效能報告生成

- **HTML Reports**：互動式圖表和詳細分析
- **CSV Exports**：原始資料供進一步分析
- **Trend Analysis**：效能衰退偵測
- **Resource Usage**：記憶體和 CPU 使用率追蹤

### 效能報告結構

```text
build/reports/test-performance/
├── performance-report.html          # Interactive HTML report with charts (via TestPerformanceReportGenerator)
├── performance-data.csv             # Raw performance data (via TestPerformanceReportGenerator)
├── overall-performance-summary.txt  # Summary statistics (via TestPerformanceMonitor)
└── {TestClass}-performance-report.txt # Individual class reports (via TestPerformanceMonitor)
```

**報告內容：**

- **Individual Class Reports**：測試執行時間、記憶體使用、失敗原因
- **Overall Summary**：執行的測試總數、成功率、平均執行時間
- **Performance Analysis**：慢速測試識別、最慢的前 5 個測試
- **HTML Reports**：互動式圖表和詳細分析（單獨生成）
- **CSV Data**：原始效能資料供進一步分析

## 測試資源管理

### 資源清理策略

#### 自動清理

```java
// TestPerformanceConfiguration provides automatic cleanup
public static class TestPerformanceListener extends AbstractTestExecutionListener {

    @Override
    public void afterTestMethod(TestContext testContext) throws Exception {
        // Automatic cleanup after each test method:
        // - Database cleanup
        // - Cache clearing
        // - Mock resetting
        // - Temporary resource cleanup
    }
}
```

#### 手動資源管理

```java
// BaseIntegrationTest provides manual resource management
protected void forceResourceCleanup() {
    // Force cleanup when needed during tests
}

protected boolean isMemoryUsageAcceptable() {
    // Check memory usage within acceptable limits
}

protected void waitForCondition(BooleanSupplier condition, Duration timeout, String description) {
    // Wait for asynchronous operations with timeout
}
```

### 記憶體管理最佳實踐

#### 測試的 JVM 配置

```gradle
// Optimized JVM parameters for test execution
jvmArgs += [
    '--enable-preview',
    '-XX:MaxMetaspaceSize=1g',
    '-XX:+UseG1GC',
    '-XX:+UseStringDeduplication',
    '-XX:G1HeapRegionSize=32m',
    '-XX:+UnlockExperimentalVMOptions',
    '-XX:G1NewSizePercent=20',
    '-XX:G1MaxNewSizePercent=30',
    '-Xshare:off'
]
```

#### 記憶體監控

- **Warning Threshold**：80% 記憶體使用
- **Critical Threshold**：90% 記憶體使用
- **Automatic GC**：在關鍵使用時觸發
- **Periodic Cleanup**：每 5 個測試

#### 5. TestPerformanceReportGenerator

獨立的工具，用於生成全面的 HTML 和 CSV 效能報告。

```bash
# Generate performance reports
./gradlew generatePerformanceReport
```

**生成的報告：**

- **HTML Report**：互動式圖表和詳細的效能分析
- **CSV Report**：原始效能資料供進一步分析
- **Trend Analysis**：隨時間的效能衰退偵測
- **Resource Usage**：記憶體和執行時間的相關性

## 與現有工具的整合

### Allure 整合

```gradle
// Allure reporting with performance data
systemProperty 'allure.results.directory', layout.buildDirectory.dir("allure-results").get().asFile.absolutePath
systemProperty 'allure.epic', 'Performance Testing'
systemProperty 'allure.feature', 'Test Performance Monitoring'
```

### Cucumber 整合

```gradle
// Cucumber with performance monitoring
tasks.register('cucumber', JavaExec) {
    maxHeapSize = '4g'
    args = [
        '--plugin', 'io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm',
        '--glue', 'solid.humank.genaidemo.bdd',
        'src/test/resources/features'
    ]
}
```

## 最佳實踐

### 測試效能優化

1. **使用適當的測試類型**：
   - Unit tests 用於業務邏輯（快速、隔離）
   - Integration tests 用於元件互動（中等）
   - E2E tests 用於完整工作流程（慢速、全面）

2. **資源管理**：
   - 使用 `@TestPerformanceExtension` 啟用效能監控
   - 使用 `BaseIntegrationTest` 以獲得一致的設定
   - 在測試方法中實作適當的清理

3. **記憶體優化**：
   - 在測試期間監控記憶體使用
   - 當記憶體使用量高時強制清理
   - 為不同的測試類型使用適當的 heap 大小

4. **效能衰退偵測**：
   - 自動偵測慢速測試
   - 效能趨勢分析
   - 基於閾值的警報

### 測試執行策略

#### 開發階段

```bash
./gradlew quickTest    # Fast feedback during development
```

#### Pre-Commit 階段

```bash
./gradlew preCommitTest    # Comprehensive verification before commit
```

#### Pre-Release 階段

```bash
./gradlew fullTest    # Complete test suite including performance validation
```

## 監控和報告

### 效能指標

- **Test Execution Time**：每個測試和每個類別
- **Memory Usage**：每個測試前後
- **Resource Utilization**：CPU、記憶體、資料庫連接
- **Failure Rates**：成功/失敗統計

### 報告生成

```bash
# Generate performance reports
./gradlew generatePerformanceReport

# View reports
open build/reports/test-performance/performance-report.html
```

### 效能衰退偵測

- 自動偵測超過閾值的測試
- 歷史效能比較
- 趨勢分析和警報
- 與 CI/CD pipeline 整合

此框架確保整個應用程式的測試效能保持一致、受監控和優化。
