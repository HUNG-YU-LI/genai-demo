# Quality Perspectives

本章節包含遵循 Rozanski & Woods 方法論的全部 8 個品質觀點的文檔。每個觀點描述了跨越多個視角的品質屬性。

## 什麼是 Perspectives

Perspectives 是影響整個系統的品質屬性：

- **跨領域關注點**：跨越多個視角
- **品質需求**：如 security、performance、availability
- **非功能性需求**：對設計產生限制

## 8 個 Perspectives

### 1. [Security Perspective](security/README.md)

**關注點**：Authentication、authorization、資料保護、合規性

**關鍵問題**：

- 系統如何防範攻擊？
- 敏感資料如何受到保護？
- 使用者如何進行 authentication 和 authorization？

**影響的視角**：所有視角

---

### 2. [Performance & Scalability Perspective](performance/README.md)

**關注點**：回應時間、吞吐量、資源使用、擴展

**關鍵問題**：

- 系統回應速度有多快？
- 能支援多少使用者？
- 在負載下如何擴展？

**影響的視角**：Functional、Information、Concurrency、Deployment

---

### 3. [Availability & Resilience Perspective](availability/README.md)

**關注點**：正常運行時間、容錯、災難恢復

**關鍵問題**：

- 系統的正常運行時間是多少？
- 如何處理故障？
- 恢復速度有多快？

**影響的視角**：Deployment、Operational、Concurrency

---

### 4. [Evolution Perspective](evolution/README.md)

**關注點**：可擴展性、可維護性、技術演進

**關鍵問題**：

- 添加新功能有多容易？
- 技術如何升級？
- 如何維護向後相容性？

**影響的視角**：Development、Functional、Information

---

### 5. [Accessibility Perspective](accessibility/README.md)

**關注點**：UI accessibility、API 可用性、文檔清晰度

**關鍵問題**：

- 身心障礙使用者能否使用系統？
- API 是否易於使用？
- 文檔是否清晰？

**影響的視角**：Functional、Operational

---

### 6. [Development Resource Perspective](development-resource/README.md)

**關注點**：團隊結構、技能、工具、生產力

**關鍵問題**：

- 需要哪些技能？
- 需要哪些工具？
- 知識如何傳遞？

**影響的視角**：Development、Operational

---

### 7. [Internationalization Perspective](internationalization/README.md)

**關注點**：多語言支援、在地化、文化適應

**關鍵問題**：

- 支援哪些語言？
- 內容如何在地化？
- 文化考量有哪些？

**影響的視角**：Functional、Information、Deployment

---

### 8. [Location Perspective](location/README.md)

**關注點**：地理分佈、資料駐留、延遲

**關鍵問題**：

- 使用者位於何處？
- 資料儲存在哪裡？
- 如何最小化延遲？

**影響的視角**：Deployment、Information、Operational

---

## 如何使用本文檔

### 對於品質保證

- 檢視所有觀點以了解品質需求
- 使用品質屬性場景進行測試
- 根據需求驗證實作

### 對於架構師

- 在設計期間將觀點應用於視角
- 確保品質屬性得到解決
- 記錄權衡和決策

### 對於開發人員

- 了解實作的品質限制
- 遵循實作指南
- 根據品質需求驗證程式碼

## Perspective-Viewpoint 矩陣

| Perspective | Functional | Information | Concurrency | Development | Deployment | Operational | Context |
|-------------|-----------|-------------|-------------|-------------|------------|-------------|---------|
| Security | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Performance | ✓ | ✓ | ✓ | - | ✓ | ✓ | - |
| Availability | ✓ | ✓ | ✓ | - | ✓ | ✓ | - |
| Evolution | ✓ | ✓ | - | ✓ | ✓ | - | - |
| Accessibility | ✓ | - | - | - | - | ✓ | - |
| Dev Resource | - | - | - | ✓ | - | ✓ | - |
| i18n | ✓ | ✓ | - | - | ✓ | - | ✓ |
| Location | - | ✓ | - | - | ✓ | ✓ | ✓ |

## Quality Attribute Scenarios

每個觀點都包含以下格式的品質屬性場景：

- **Source**：誰／什麼產生刺激
- **Stimulus**：影響系統的條件
- **Environment**：刺激發生時的系統狀態
- **Artifact**：受影響的系統部分
- **Response**：系統如何回應
- **Response Measure**：如何衡量回應

範例：

```text
Source: Web user
Stimulus: Submit order during peak hours
Environment: 1000 concurrent users
Artifact: Order processing service
Response: Order processed and confirmed
Response Measure: Response time ≤ 2000ms, Success rate ≥ 99.5%
```

---

**Last Updated**: 2025-01-17
**Status**: In Progress
