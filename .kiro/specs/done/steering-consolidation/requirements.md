# Steering Rules 整合需求

## 簡介

本文件概述整合和簡化當前 steering rules 的需求,以減少冗餘、提高可維護性並增強可用性,同時保留基本指導。

## 需求

### 需求 1: 消除內容重複

**用戶故事:** 作為開發者,我希望避免在多個檔案中閱讀相同的資訊,以便能夠更有效地找到指導。

#### 驗收標準

1. WHEN 審查測試效能內容 THEN 詳細實作應該 (SHALL) 僅存在於 test-performance-standards.md 中
2. WHEN development-standards.md 引用測試效能 THEN 它應該 (SHALL) 僅提供簡短概述和詳細文件的連結
3. WHEN performance-standards.md 討論測試效能 THEN 它應該 (SHALL) 引用 test-performance-standards.md 而非重複內容
4. WHEN 任何 steering 檔案包含重複內容 THEN 重複部分應該 (SHALL) 被移除並替換為交叉引用

### 需求 2: 建立清晰的內容層次結構

**用戶故事:** 作為開發者,我希望了解應該先閱讀哪個文件,以便能夠快速找到所需的資訊。

#### 驗收標準

1. WHEN 開發者需要基本標準 THEN development-standards.md 應該 (SHALL) 是主要入口點
2. WHEN 開發者需要專業指導 THEN 專業文件應該 (SHALL) 明確標記為 "深入探討" 或 "參考"
3. WHEN 文件引用另一個文件 THEN 它應該 (SHALL) 使用一致的交叉引用格式並提供清晰的上下文
4. WHEN README.md 列出文件 THEN 它應該 (SHALL) 清楚指示每個文件的層次結構和目的

### 需求 3: 整合相關內容

**用戶故事:** 作為開發者,我希望相關資訊組織在一起,以便我不必在多個檔案之間跳轉。

#### 驗收標準

1. WHEN 測試效能內容存在於多個檔案中 THEN 它應該 (SHALL) 被整合到 test-performance-standards.md 中
2. WHEN 語言使用規則存在 THEN 它們應該 (SHALL) 被整合到單一文件中
3. WHEN 文件標準存在 THEN 它們應該 (SHALL) 被合併到統一的文件指南中
4. WHEN 整合發生 THEN 不應該 (SHALL) 遺失任何基本資訊

### 需求 4: 簡化交叉引用

**用戶故事:** 作為開發者,我希望有清晰和最少的交叉引用,以便能夠輕鬆瀏覽文件。

#### 驗收標準

1. WHEN 文件引用另一個文件 THEN 它應該 (SHALL) 使用標準格式: "> **📋 主題**: 簡短描述 - 請參閱 [文件名稱](連結)"
2. WHEN 交叉引用存在 THEN 它們應該 (SHALL) 放置在相關部分的開頭
3. WHEN 多個交叉引用指向同一個文件 THEN 它們應該 (SHALL) 被整合成一個引用
4. WHEN 添加交叉引用 THEN 它應該 (SHALL) 包含目標文件中可用資訊的上下文

### 需求 5: 減少檔案數量

**用戶故事:** 作為開發者,我希望管理更少的 steering 檔案,以便文件更容易維護。

#### 驗收標準

1. WHEN 語言使用規則存在於多個檔案中 THEN 它們應該 (SHALL) 被合併到一個檔案中
2. WHEN 文件標準存在於多個檔案中 THEN 它們應該 (SHALL) 被合併到一個檔案中
3. WHEN 整合完成 THEN steering 檔案總數應該 (SHALL) 至少減少 20%
4. WHEN 檔案被合併 THEN 合併後的內容應該 (SHALL) 以清晰的部分進行邏輯組織

### 需求 6: 保持基本指導

**用戶故事:** 作為開發者,我希望所有基本指導都被保留,以便在整合過程中不會遺失重要資訊。

#### 驗收標準

1. WHEN 內容被移除 THEN 它應該 (SHALL) 僅是重複或冗餘的內容
2. WHEN 內容被整合 THEN 所有獨特資訊應該 (SHALL) 被保留
3. WHEN 專業指導存在 THEN 它應該 (SHALL) 保留在專用檔案中以供深入參考
4. WHEN 整合完成 THEN 應該 (SHALL) 進行審查以確認沒有遺失基本指導

### 需求 7: 改善 README 組織

**用戶故事:** 作為開發者,我希望有清晰的 README 幫助我快速找到正確的文件,以便提高生產力。

#### 驗收標準

1. WHEN README.md 被更新 THEN 它應該 (SHALL) 有清晰的 "快速開始" 部分用於常見情境
2. WHEN README.md 列出文件 THEN 它應該 (SHALL) 按目的分組 (核心、專業、參考)
3. WHEN README.md 提供指導 THEN 它應該 (SHALL) 包含找到正確文件的決策樹
4. WHEN README.md 完成 THEN 它應該 (SHALL) 不超過 200 行

### 需求 8: 標準化文件結構

**用戶故事:** 作為開發者,我希望有一致的文件結構,以便能夠在任何 steering 檔案中快速找到資訊。

#### 驗收標準

1. WHEN 建立或更新 steering 文件 THEN 它應該 (SHALL) 遵循標準結構: 概述、快速參考、詳細內容、相關文件
2. WHEN 文件有專業內容 THEN 它應該 (SHALL) 清楚標記部分為 "基礎" 或 "進階"
3. WHEN 文件很長 THEN 它應該 (SHALL) 包含目錄
4. WHEN 文件引用程式碼範例 THEN 它們應該 (SHALL) 被清楚標記和正確格式化

## 提議的整合計劃

### 要合併的檔案

1. **語言和文件標準** (合併到一個檔案):
   - chinese-conversation-english-documentation.md
   - english-documentation-standards.md
   - datetime-accuracy-standards.md
   → 新檔案: `documentation-language-standards.md`

2. **測試效能** (保持分離但移除重複):
   - 保留: test-performance-standards.md (詳細參考)
   - 更新: development-standards.md (移除重複的測試效能內容,添加簡短概述和連結)
   - 更新: performance-standards.md (移除重複的測試效能內容,添加交叉引用)

3. **BDD/TDD** (合併到 development-standards.md):
   - bdd-tdd-principles.md → 移動到 development-standards.md 作為一個部分

### 要保持原樣的檔案

- development-standards.md (核心,已移除重複)
- rozanski-woods-architecture-methodology.md (專業)
- code-review-standards.md (專業)
- security-standards.md (專業)
- performance-standards.md (專業,已移除重複)
- test-performance-standards.md (參考)
- domain-events.md (專業)
- diagram-generation-standards.md (專業)
- reports-organization-standards.md (專業)

### 預期結果

- **之前**: 13 個 steering 檔案
- **之後**: 10 個 steering 檔案 (減少 23%)
- **重複內容**: 減少約 40%
- **交叉引用**: 標準化和簡化
- **README**: 重新組織並提供清晰導航

## 成功標準

1. 所有重複內容被消除
2. 交叉引用清晰且最少
3. 檔案數量至少減少 20%
4. 沒有遺失基本指導
5. README 提供清晰導航
6. 文件結構一致
7. 開發者能更快找到資訊 (透過回饋測量)
