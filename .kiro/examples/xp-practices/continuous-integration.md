# Continuous Integration 指南

## 概述

在我們專案中實作持續整合（Continuous Integration, CI）的實用指南。

**相關標準**：[Development Standards](../../steering/development-standards.md)

---

## 什麼是 Continuous Integration？

**Continuous Integration (CI)** 是頻繁地將程式碼變更整合到共享儲存庫的實踐，伴隨自動化建置和測試。

### 核心原則

1. **頻繁整合** - 每天多次
2. **自動化建置** - 一個指令建置
3. **自動化測試** - 每次 commit 觸發測試
4. **立即修復損壞的建置** - 最高優先級
5. **保持建置快速** - < 10 分鐘

---

## CI 工作流程

```
Developer → Commit → Push → CI Server → Build → Test → Deploy
                                ↓
                            Notify Team
```

### 每日工作流程

```bash
# 1. Pull latest changes
git pull origin main

# 2. Create feature branch
git checkout -b feature/order-submission

# 3. Write test (Red)
# Write failing test

# 4. Implement (Green)
# Make test pass

# 5. Refactor
# Improve code quality

# 6. Run local tests
./gradlew test

# 7. Commit frequently
git add .
git commit -m "feat: add order submission validation"

# 8. Push to trigger CI
git push origin feature/order-submission

# 9. CI runs automatically
# - Build
# - Test
# - Code quality checks
# - Security scan

# 10. Create PR when ready
# CI runs again on PR

# 11. Merge after approval
# CI runs on main branch
```

---

## 建置自動化

### 單一指令建置

```bash
# ✅ 良好：一個指令建置所有內容
./gradlew clean build

# This should:
# - Compile code
# - Run all tests
# - Generate reports
# - Create artifacts
```

### Gradle 建置配置

```gradle
// build.gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.4.5'
    id 'jacoco'
    id 'pmd'
    id 'checkstyle'
}

// Fast feedback tasks
tasks.register('quickTest', Test) {
    description = 'Fast unit tests for daily development'
    useJUnitPlatform {
        excludeTags 'integration', 'slow'
    }
    maxHeapSize = '2g'
    maxParallelForks = Runtime.runtime.availableProcessors()
}

// Pre-commit verification
tasks.register('preCommit') {
    dependsOn 'quickTest', 'checkstyleMain', 'pmdMain'
    description = 'Run before committing code'
}

// Full CI build
tasks.register('ciBuild') {
    dependsOn 'clean', 'build', 'test', 'jacocoTestReport', 'archUnit'
    description = 'Complete CI build with all checks'
}
```

---

## 自動化測試

### CI 中的測試金字塔

```
┌─────────────────┐
│   E2E Tests     │  5%  - Slow, comprehensive
│   (< 3s each)   │
├─────────────────┤
│ Integration     │  15% - Medium speed
│ Tests           │
│ (< 500ms each)  │
├─────────────────┤
│   Unit Tests    │  80% - Fast, focused
│   (< 50ms each) │
└─────────────────┘
```

### CI 測試執行

```bash
# Stage 1: Fast feedback (< 2 min)
./gradlew quickTest

# Stage 2: Integration tests (< 5 min)
./gradlew integrationTest

# Stage 3: E2E tests (< 10 min)
./gradlew e2eTest

# Stage 4: Quality checks
./gradlew jacocoTestReport pmdMain checkstyleMain
```

---

## GitHub Actions CI Pipeline

### 基本 CI Workflow

```yaml
# .github/workflows/ci.yml
name: CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v3

    - name: Set up JDK 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
        cache: gradle

    - name: Grant execute permission for gradlew
      run: chmod +x gradlew

    - name: Build with Gradle
      run: ./gradlew build

    - name: Run tests
      run: ./gradlew test

    - name: Generate test report
      run: ./gradlew jacocoTestReport

    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v3
      with:
        files: ./build/reports/jacoco/test/jacocoTestReport.xml
```

### 多階段 CI Pipeline

```yaml
# .github/workflows/ci-advanced.yml
name: Advanced CI

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  # Stage 1: Fast feedback
  quick-tests:
    runs-on: ubuntu-latest
    timeout-minutes: 5
    steps:
    - uses: actions/checkout@v3
    - uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
        cache: gradle
    - name: Quick tests
      run: ./gradlew quickTest

  # Stage 2: Code quality
  code-quality:
    runs-on: ubuntu-latest
    needs: quick-tests
    steps:
    - uses: actions/checkout@v3
    - uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
        cache: gradle
    - name: Code quality checks
      run: ./gradlew checkstyleMain pmdMain spotbugsMain

  # Stage 3: Integration tests
  integration-tests:
    runs-on: ubuntu-latest
    needs: quick-tests
    timeout-minutes: 10
    steps:
    - uses: actions/checkout@v3
    - uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
        cache: gradle
    - name: Integration tests
      run: ./gradlew integrationTest

  # Stage 4: Security scan
  security-scan:
    runs-on: ubuntu-latest
    needs: quick-tests
    steps:
    - uses: actions/checkout@v3
    - uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
        cache: gradle
    - name: Security scan
      run: ./gradlew dependencyCheckAnalyze

  # Stage 5: E2E tests (only on main)
  e2e-tests:
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    needs: [integration-tests, code-quality]
    timeout-minutes: 15
    steps:
    - uses: actions/checkout@v3
    - uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
        cache: gradle
    - name: E2E tests
      run: ./gradlew e2eTest
```

---

## 建置通知

### Slack 整合

```yaml
# .github/workflows/ci.yml
jobs:
  build:
    # ... build steps ...

    - name: Notify Slack on failure
      if: failure()
      uses: slackapi/slack-github-action@v1
      with:
        payload: |
          {
            "text": "❌ Build failed on ${{ github.ref }}",
            "blocks": [
              {
                "type": "section",
                "text": {
                  "type": "mrkdwn",
                  "text": "*Build Failed*\nBranch: ${{ github.ref }}\nCommit: ${{ github.sha }}\nAuthor: ${{ github.actor }}"
                }
              }
            ]
          }
      env:
        SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK_URL }}
```

---

## 最佳實踐

### 1. 頻繁 Commit

```bash
# ✅ 良好：小的、頻繁的 commits
git commit -m "feat: add email validation"
git commit -m "test: add test for empty email"
git commit -m "refactor: extract validation to value object"

# ❌ 不良：大的、不頻繁的 commits
git commit -m "feat: complete entire order module"
# (100+ files changed, 5000+ lines)
```

### 2. 立即修復損壞的建置

```
Priority when build breaks:
1. Stop new work
2. Fix the build
3. Verify fix works
4. Resume normal work

If fix takes > 30 minutes:
- Revert the breaking commit
- Fix offline
- Re-commit when ready
```

### 3. 保持建置快速

```
Target build times:
- Quick tests: < 2 minutes
- Full build: < 10 minutes
- E2E tests: < 15 minutes

Strategies:
- Parallel test execution
- Test categorization
- Incremental builds
- Build caching
```

### 4. 永遠不要在損壞的建置上 Commit

```bash
# ✅ 良好：先檢查建置狀態
./gradlew test
# All tests pass
git commit -m "feat: add feature"

# ❌ 不良：未測試就 commit
git commit -m "feat: add feature"
# Build breaks for everyone
```

---

## Continuous Deployment

### Deployment Pipeline

```yaml
# .github/workflows/cd.yml
name: CD

on:
  push:
    branches: [ main ]

jobs:
  deploy-staging:
    runs-on: ubuntu-latest
    environment: staging
    steps:
    - uses: actions/checkout@v3
    - name: Deploy to staging
      run: |
        ./gradlew build
        ./deploy-staging.sh

    - name: Run smoke tests
      run: ./gradlew smokeTest -Denv=staging

  deploy-production:
    needs: deploy-staging
    runs-on: ubuntu-latest
    environment: production
    steps:
    - uses: actions/checkout@v3
    - name: Deploy to production
      run: |
        ./gradlew build
        ./deploy-production.sh

    - name: Run smoke tests
      run: ./gradlew smokeTest -Denv=production
```

---

## 監控 CI 健康狀況

### 關鍵指標

```
Build Success Rate:     > 95%
Average Build Time:     < 10 minutes
Time to Fix Build:      < 30 minutes
Test Coverage:          > 80%
Failed Test Rate:       < 1%
```

### CI Dashboard

```
┌─────────────────────────────────┐
│ CI Health Dashboard             │
├─────────────────────────────────┤
│ Build Status:        ✅ Passing │
│ Last Build:          2 min ago  │
│ Success Rate:        98%        │
│ Avg Build Time:      8m 32s     │
│ Test Coverage:       85%        │
│ Failed Tests:        0          │
└─────────────────────────────────┘
```

---

## 疑難排解

### 本地建置失敗但 CI 通過

```bash
# Check environment differences
# - Java version
# - Gradle version
# - Environment variables
# - File permissions

# Clean and rebuild
./gradlew clean build --no-build-cache
```

### Flaky Tests

```bash
# Identify flaky tests
./gradlew test --rerun-tasks

# Fix flaky tests:
# - Remove time dependencies
# - Fix race conditions
# - Improve test isolation
# - Use proper test data setup
```

### 建置緩慢

```bash
# Profile build
./gradlew build --profile

# Optimize:
# - Enable parallel execution
# - Use build cache
# - Optimize test execution
# - Split into stages
```

---

## 總結

有效的 CI 需要：

1. **頻繁整合** - 每天多次 commits
2. **自動化建置** - 一個指令建置和測試
3. **快速回饋** - 建置在 < 10 分鐘內完成
4. **立即修復** - 損壞的建置是最高優先級
5. **團隊紀律** - 每個人都遵循流程

記住：**CI 是一種實踐，而非僅是工具** - 它需要團隊承諾。

---

**相關文件**：
- [Development Standards](../../steering/development-standards.md)
- [Testing Strategy](../../steering/testing-strategy.md)
- [Simple Design Examples](simple-design-examples.md)
