# Architecture Viewpoints

本節包含遵循 Rozanski & Woods 方法論的所有 7 個架構 viewpoints 的文件。每個 viewpoint 從不同角度描述系統結構，解決特定利害關係人的關注點。

## 什麼是 Viewpoints

Viewpoints 是系統結構的不同視角，幫助我們理解：

- **是什麼** 系統是什麼
- **如何** 系統如何組織
- **為什麼** 系統為何如此構建

## 7 個 Viewpoints

### 1. [Functional Viewpoint](functional/README.md)

**目的**：描述系統的功能能力以及它們如何組織

**主要關注點**：

- 系統做什麼？
- 主要功能元素是什麼？
- 它們如何互動？
- 系統暴露哪些介面？

**利害關係人**：業務分析師、產品經理、開發人員

---

### 2. [Information Viewpoint](information/README.md)

**目的**：描述系統如何儲存、管理和分發資訊

**主要關注點**：

- 系統管理哪些資料？
- 資料如何結構化？
- 誰擁有哪些資料？
- 資料如何在系統中流動？

**利害關係人**：資料架構師、資料庫管理員、開發人員

---

### 3. [Concurrency Viewpoint](concurrency/README.md)

**目的**：描述系統如何處理並行和平行操作

**主要關注點**：

- 哪些操作並行運行？
- 並行如何管理？
- 同步機制是什麼？
- 如何防止競態條件？

**利害關係人**：開發人員、效能工程師、架構師

---

### 4. [Development Viewpoint](development/README.md)

**目的**：描述程式碼組織、建置流程和開發環境

**主要關注點**：

- 程式碼如何組織？
- 模組相依性是什麼？
- 系統如何建置和測試？
- 開發人員需要哪些工具？

**利害關係人**：開發人員、建置工程師、DevOps

---

### 5. [Deployment Viewpoint](deployment/README.md)

**目的**：描述系統如何部署到基礎設施

**主要關注點**：

- 需要什麼基礎設施？
- 網路如何配置？
- 部署流程是什麼？
- 系統如何擴展？

**利害關係人**：DevOps 工程師、基礎設施架構師、維運人員

---

### 6. [Operational Viewpoint](operational/README.md)

**目的**：描述系統如何運行、監控和維護

**主要關注點**：

- 系統如何監控？
- 運維程序是什麼？
- 如何執行備份？
- 系統如何維護？

**利害關係人**：維運團隊、SRE、支援工程師

---

### 7. [Context Viewpoint](context/README.md)

**目的**：描述系統與其環境的關係

**主要關注點**：

- 系統邊界是什麼？
- 它與哪些外部系統互動？
- 利害關係人是誰？
- 外部約束是什麼？

**利害關係人**：業務分析師、架構師、合規官員

---

## 如何使用本文件

### 給新團隊成員

1. 從 [Context Viewpoint](context/README.md) 開始了解系統邊界
2. 閱讀 [Functional Viewpoint](functional/README.md) 了解系統功能
3. 查看 [Development Viewpoint](development/README.md) 了解程式碼組織

### 給架構師

- 查看所有 viewpoints 以獲得完整的系統理解
- 檢查 [Architecture Decisions](../architecture/adrs/README.md) 了解設計理由
- 查看 [Perspectives](../perspectives/README.md) 了解品質屬性

### 給開發人員

- 專注於 [Development Viewpoint](development/README.md) 了解程式碼組織
- 查看 [Functional Viewpoint](functional/README.md) 了解業務邏輯
- 檢查 [Information Viewpoint](information/README.md) 了解資料模型

### 給維運人員

- 專注於 [Deployment Viewpoint](deployment/README.md) 了解基礎設施
- 查看 [Operational Viewpoint](operational/README.md) 了解程序
- 檢查 [Operations Guide](../operations/README.md) 了解操作手冊

## Viewpoint 關係

Viewpoints 相互關聯並相互參照：

```text
Context ──────────> Functional ──────────> Information
   │                    │                       │
   │                    │                       │
   └──> Development <───┴──> Concurrency <──────┘
            │                    │
            │                    │
            └──> Deployment <────┘
                     │
                     │
                 Operational
```

## 跨領域關注點

影響多個 viewpoints 的品質屬性記錄在 [Perspectives](../perspectives/README.md) 中：

- [Security](../perspectives/security/README.md)
- [Performance](../perspectives/performance/README.md)
- [Availability](../perspectives/availability/README.md)
- [Evolution](../perspectives/evolution/README.md)

---

**最後更新**：2025-01-17
**狀態**：進行中
