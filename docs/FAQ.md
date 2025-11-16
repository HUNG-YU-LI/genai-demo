# 常見問題 (FAQ)

本文件回答關於 Enterprise E-Commerce Platform 專案的常見問題。

## 📑 目錄

- [一般問題](#一般問題)
- [Architecture 與設計](#architecture-與設計)
- [開發](#開發)
- [測試](#測試)
- [部署與維運](#部署與維運)
- [疑難排解](#疑難排解)

---

## 一般問題

### 這個專案是關於什麼？

本專案透過完整的電商平台展示企業級軟體架構。它展示了：

- **Rozanski & Woods** 方法論 (7 個 Viewpoints + 8 個 Perspectives)
- **Domain-Driven Design** 包含 13 個 bounded contexts
- **Behavior-Driven Development** 使用 Cucumber
- **Cloud-native architecture** 在 AWS 上

**了解更多**: [專案總覽](../README.md#-project-overview)

---

### 這個專案適合誰？

本專案專為以下對象設計：

- **軟體架構師**：學習系統化的架構設計
- **開發者**：研究企業 patterns 和最佳實務
- **學生**：理解真實世界的軟體架構
- **團隊**：作為電商系統的參考架構

---

### 這個專案有何不同之處？

關鍵差異：

1. **完整的 Architecture 文件**：完整實作 Rozanski & Woods 方法論
2. **Production-Ready**：不只是 demo，而是生產級程式碼
3. **完整測試**：80%+ 涵蓋率，包含 unit、integration 和 BDD 測試
4. **Cloud-Native**：完整 AWS 基礎設施使用 CDK
5. **活文件**：測試作為可執行的規格

---

## Architecture 與設計

### 為什麼使用 Rozanski & Woods 方法論？

**優點**：

- **系統化分析**：7 個 viewpoints 提供完整系統理解
- **品質焦點**：8 個 perspectives 確保品質屬性得到處理
- **利害關係人溝通**：討論架構的共同語言
- **產業標準**：在企業架構中廣泛採用

**Viewpoints** 描述系統結構 (是什麼和如何)：
- Context、Functional、Information、Concurrency
- Development、Deployment、Operational

**Perspectives** 描述品質屬性 (跨領域關注點)：
- Security、Performance、Availability、Evolution
- Accessibility、Development Resource、i18n、Location

**了解更多**: [Rozanski & Woods 指南](rozanski-woods-methodology-guide.md)

---

### Viewpoints 和 Perspectives 有什麼不同？

**Viewpoints** = 系統結構
- 描述系統**是什麼**以及**如何**組織
- 範例：Functional Viewpoint 顯示業務能力

**Perspectives** = 品質屬性
- 描述影響整個系統的**品質關注點**
- 範例：Security Perspective 顯示跨所有 viewpoints 的認證

**類比**：
- Viewpoints = 建築物的不同相機角度
- Perspectives = 品質鏡頭 (安全性、能源效率、無障礙性)

**了解更多**: [Architecture 方法論](rozanski-woods-methodology-guide.md)

---

### 為什麼使用 Domain-Driven Design (DDD)？

**優點**：

- **業務對齊**：程式碼反映業務領域
- **Bounded Contexts**：明確邊界降低複雜度
- **Ubiquitous Language**：業務與技術間的共同術語
- **Strategic Design**：協助管理大型複雜系統

**我們的實作**：
- 13 個 bounded contexts (Customer、Order、Product 等)
- 完整戰術 patterns (Aggregates、Entities、Value Objects)
- Contexts 間的 event-driven 通訊

**了解更多**: [Functional Viewpoint](viewpoints/functional/README.md)

---

### 為什麼使用 Hexagonal Architecture？

**優點**：

- **可測試性**：業務邏輯與基礎設施隔離
- **彈性**：易於替換基礎設施元件
- **可維護性**：清楚的關注點分離
- **Domain 焦點**：業務邏輯為中心

**結構**：
```
Domain (核心) ← Application ← Infrastructure
                            ← Interfaces
```

**了解更多**: [Development Viewpoint](viewpoints/development/README.md)

---

## 開發

### 我可以不用 AWS 執行嗎？

**可以！** 使用 `local` profile：

```bash
./gradlew :app:bootRun --args='--spring.profiles.active=local'
```

**Local Profile 使用**：
- H2 in-memory database (取代 PostgreSQL)
- In-memory cache (取代 Redis)
- In-memory message broker (取代 Kafka)

**完美適用於**：
- 開發
- 單元測試
- 快速實驗

**了解更多**: [本機開發設定](development/setup/local-environment.md)

---

### 如何新增一個 bounded context？

**步驟**：

1. **定義邊界**：識別業務能力
2. **建立 Domain Model**：Aggregates、entities、value objects
3. **實作 Repository**：資料存取介面
4. **新增 Application Service**：Use case 編排
5. **建立 Infrastructure**：Repository 實作
6. **新增 Domain Events**：用於跨 context 通訊
7. **撰寫測試**：Unit、integration 和 BDD 測試
8. **更新文件**：Functional viewpoint

**了解更多**: [DDD Implementation Guide](development/ddd-implementation-guide.md)

---

### 我應該遵循哪些編碼標準？

**關鍵標準**：

- **風格**：Google Java Style Guide
- **命名**：PascalCase 用於類別、camelCase 用於方法
- **Architecture**：Hexagonal + DDD patterns
- **測試**：需要 80%+ 涵蓋率
- **文件**：公開 APIs 需要 JavaDoc

**工具**：
- Checkstyle 用於風格強制執行
- ArchUnit 用於架構規則
- JaCoCo 用於涵蓋率

**了解更多**: [編碼標準](development/coding-standards/README.md)

---

### 如何設定我的 IDE？

**支援的 IDEs**：
- IntelliJ IDEA (推薦)
- Eclipse
- VS Code

**設定步驟**：

1. 匯入為 Gradle 專案
2. 安裝需要的 plugins
3. 配置程式碼風格
4. 設定執行配置

**了解更多**: [IDE 配置](development/setup/ide-configuration.md)

---

## 測試

### 測試策略是什麼？

**Test Pyramid**：

```
     /\
    /E2E\     5% - Production 環境
   /____\
  /Integ.\   15% - Staging 環境
 /________\
/   Unit   \ 80% - Local 環境
/___________\
```

**環境特定**：
- **Local**：僅 unit tests (快速回饋)
- **Staging**：與真實 AWS 服務的 integration tests
- **Production**：E2E tests 和監控

**了解更多**: [測試策略](development/testing/testing-strategy.md)

---

### 如何執行測試？

**Unit Tests**：
```bash
./gradlew :app:test
```

**BDD Tests**：
```bash
./gradlew :app:cucumber
```

**Coverage Report**：
```bash
./gradlew :app:jacocoTestReport
# 檢視: build/reports/jacoco/test/html/index.html
```

**Architecture Tests**：
```bash
./gradlew :app:test --tests "*ArchitectureTest"
```

**所有 Pre-commit 檢查**：
```bash
make pre-commit
```

**了解更多**: [測試指南](development/testing/README.md)

---

### 如何撰寫 BDD tests？

**步驟**：

1. **撰寫 Gherkin Scenario**：
```gherkin
Feature: Customer Registration

  Scenario: Successful registration
    Given a new customer with valid information
    When they submit the registration form
    Then their account should be created
    And they should receive a welcome email
```

2. **實作 Step Definitions**：
```java
@Given("a new customer with valid information")
public void aNewCustomerWithValidInformation() {
    // Setup test data
}
```

3. **執行 Tests**：
```bash
./gradlew :app:cucumber
```

**了解更多**: [BDD 測試指南](development/testing/bdd-testing.md)

---

### 為什麼測試覆蓋率很重要？

**優點**：

- **信心**：安全重構
- **文件**：測試顯示程式碼如何運作
- **品質**：及早捕捉錯誤
- **可維護性**：更容易變更程式碼

**我們的目標**：80%+ line coverage

**重點領域**：
- 業務邏輯 (domain layer)
- Application services
- 關鍵路徑

**了解更多**: [測試策略](development/testing/testing-strategy.md)

---

## 部署與維運

### 如何部署到 AWS？

**前置要求**：
- AWS 帳號
- AWS CLI 已配置
- Node.js 18+ (用於 CDK)

**步驟**：

1. **安裝 Dependencies**：
```bash
cd infrastructure
npm install
```

2. **Bootstrap CDK** (僅首次)：
```bash
npx cdk bootstrap aws://ACCOUNT-ID/REGION
```

3. **部署到 Staging**：
```bash
npm run deploy:staging
```

4. **部署到 Production**：
```bash
npm run deploy:production
```

**了解更多**: [部署指南](operations/deployment/README.md)

---

### 使用了哪些 AWS 服務？

**核心服務**：
- **EKS**：Kubernetes 編排
- **RDS**：PostgreSQL 資料庫
- **MSK**：Managed Kafka
- **ElastiCache**：Redis cache
- **CloudWatch**：監控和日誌
- **X-Ray**：分散式追蹤

**支援服務**：
- VPC、Security Groups、IAM
- Secrets Manager、Certificate Manager
- Route 53、CloudFront、S3

**了解更多**: [Deployment Viewpoint](viewpoints/deployment/README.md)

---

### 如何監控應用程式？

**監控堆疊**：

- **Metrics**：CloudWatch + Prometheus
- **Logging**：CloudWatch 中的結構化日誌
- **Tracing**：AWS X-Ray 用於分散式追蹤
- **Dashboards**：Amazon Managed Grafana
- **Alerts**：CloudWatch Alarms + SNS

**關鍵指標**：
- API 回應時間 (p50, p95, p99)
- 依 endpoint 的錯誤率
- 資料庫查詢效能
- 快取命中率
- 業務指標 (訂單、收入)

**了解更多**: [監控指南](operations/monitoring/monitoring-strategy.md)

---

### 如果 production 發生問題怎麼辦？

**事件回應**：

1. **檢查 Runbooks**: [Operations Runbooks](operations/runbooks/README.md)
2. **審閱 Dashboards**: Grafana dashboards
3. **檢查 Logs**: CloudWatch Logs
4. **遵循程序**: Incident response runbook

**常見問題**：
- [Troubleshooting Guide](operations/troubleshooting/common-issues.md)
- [Debugging Guide](operations/troubleshooting/debugging-guide.md)

**取得協助**: yikaikao@gmail.com

---

## 疑難排解

### 應用程式無法啟動

**常見原因**：

1. **Port 已被使用**：
```bash
# 檢查是什麼在使用 port 8080
lsof -i :8080
# 終止 process
kill -9 <PID>
```

2. **資料庫未執行**：
```bash
# 啟動 Docker 服務
docker-compose up -d
```

3. **缺少 Dependencies**：
```bash
# 清除並重新建置
./gradlew clean build
```

**了解更多**: [Troubleshooting Guide](operations/troubleshooting/common-issues.md)

---

### 測試失敗

**常見原因**：

1. **過期的 Dependencies**：
```bash
./gradlew clean build --refresh-dependencies
```

2. **資料庫狀態問題**：
```bash
# 重置資料庫
docker-compose down -v
docker-compose up -d
```

3. **Architecture 規則違反**：
```bash
# 檢查 ArchUnit tests
./gradlew :app:test --tests "*ArchitectureTest"
```

**了解更多**: [測試 Troubleshooting](development/testing/troubleshooting.md)

---

### 圖表無法生成

**常見原因**：

1. **PlantUML 語法錯誤**：
```bash
# 驗證圖表
make validate
```

2. **缺少 PlantUML**：
```bash
# 安裝 PlantUML
brew install plantuml  # macOS
# 或從 https://plantuml.com/ 下載
```

3. **損壞的參考**：
```bash
# 檢查交叉參考
./scripts/validate-cross-references.py
```

**了解更多**: [圖表生成指南](diagrams/README.md)

---

### 如何取得協助？

**資源**：

1. **文件**: 查看 [docs/](../README.md#-documentation)
2. **此 FAQ**: 搜尋本文件
3. **GitHub Issues**: [搜尋現有 issues](https://github.com/yourusername/genai-demo/issues)
4. **Discussions**: [在 Discussions 中提問](https://github.com/yourusername/genai-demo/discussions)
5. **Email**: yikaikao@gmail.com

**提問前**：
- 搜尋文件
- 檢查現有 issues
- 嘗試疑難排解指南
- 提供錯誤訊息和日誌

---

## 額外問題

### 在哪裡可以找到 API 文件？

**多種格式**：

- **互動式**: Swagger UI 在 http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/api-docs
- **文件**: [API Documentation](api/README.md)
- **Postman**: Collection (即將推出)

---

### 如何貢獻？

**快速步驟**：

1. Fork repository
2. 建立 feature branch
3. 遵循[編碼標準](development/coding-standards/README.md)進行變更
4. 撰寫測試 (80%+ coverage)
5. 執行 `make pre-commit`
6. 提交 pull request

**了解更多**: [Contributing Guide](../CONTRIBUTING.md)

---

### 這是 production-ready 的嗎？

**是的！** 本專案包含：

- ✅ 完整測試 (80%+ coverage)
- ✅ 安全最佳實務
- ✅ 監控和可觀察性
- ✅ 災難復原程序
- ✅ 維運 runbooks
- ✅ CI/CD pipeline
- ✅ Infrastructure as Code

**然而**：在 production 使用前，請審閱並根據您的特定需求調整。

---

### 我可以用於我的專案嗎？

**可以！** 本專案採用 MIT 授權。

**您可以**：
- 作為參考架構使用
- 複製和修改程式碼
- 用於商業專案
- 從實作中學習

**請**：
- 適當給予致謝
- 分享改進 (選擇性)
- 遵循授權條款

**了解更多**: [LICENSE](../LICENSE)

---

### 如何保持更新？

**保持資訊的方式**：

- ⭐ 在 GitHub 上 star repository
- 👀 關注 releases
- 📧 訂閱 discussions
- 📰 查看 [CHANGELOG.md](../CHANGELOG.md)

---

## 還有問題？

如果您的問題未在此回答：

1. **搜尋文件**: [docs/README.md](README.md)
2. **檢查 Issues**: [GitHub Issues](https://github.com/yourusername/genai-demo/issues)
3. **詢問社群**: [GitHub Discussions](https://github.com/yourusername/genai-demo/discussions)
4. **Email 維護者**: yikaikao@gmail.com

**我們樂意協助！** 🤝

---

**最後更新**: 2024-11-09
**維護者**: yikaikao@gmail.com
