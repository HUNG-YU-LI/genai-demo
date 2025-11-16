# Documentation Quality Check Scripts

此目錄包含 Viewpoints & Perspectives 文件結構的完整文件品質保證工具。

## 概覽

文件品質檢查系統實作了文件重組規格中的**需求6：文件維護的自動化**。它提供以下自動化驗證：

- ✅ **Markdown 語法檢查**，使用 markdownlint
- ✅ **連結有效性驗證**，針對內部和外部連結
- ✅ **圖表渲染驗證**，支援 Mermaid、PlantUML 和 Excalidraw
- ✅ **翻譯同步**，中英文版本之間
- ✅ **文件 metadata 驗證**，包含 YAML front matter 檢查
- ✅ **結構一致性**，針對 Viewpoints & Perspectives 組織

## Scripts

### Main Quality Check Script

**`check-documentation-quality.sh`** - 綜合品質檢查執行器
```bash
# 執行所有品質檢查
bash scripts/check-documentation-quality.sh

# 或使用 npm script
npm run docs:quality
```

**功能：**
- 依序執行所有品質檢查
- 在 `build/reports/documentation-quality/` 產生完整報告
- 提供彩色輸出及通過/失敗狀態
- 建立包含建議的摘要報告

### Individual Quality Check Tools

#### 1. Advanced Link Checker
**`check-links-advanced.js`** - 基於 Node.js 的連結驗證
```bash
# 僅檢查內部連結（快速）
node scripts/check-links-advanced.js

# 檢查內部和外部連結（較慢）
node scripts/check-links-advanced.js --external

# 詳細輸出及進度
node scripts/check-links-advanced.js --verbose

# 自訂輸出檔案
node scripts/check-links-advanced.js --output reports/links.json

# NPM scripts
npm run docs:links          # 僅內部連結
npm run docs:links:external # 包含外部連結
```

**功能：**
- 驗證 markdown `[]()`和 HTML `<a href="">` 連結
- 支援內部檔案連結和外部 HTTP/HTTPS 連結
- 產生 JSON 和 Markdown 報告
- 處理相對路徑和錨點連結
- 可設定逾時和 user agent
- 排除 localhost 和範例網域

#### 2. Diagram Validator
**`validate-diagrams.py`** - 基於 Python 的圖表語法驗證
```bash
# 驗證所有圖表
python3 scripts/validate-diagrams.py

# 詳細輸出
python3 scripts/validate-diagrams.py --verbose

# 自訂輸出目錄
python3 scripts/validate-diagrams.py --output reports/

# 驗證特定目錄
python3 scripts/validate-diagrams.py docs/diagrams/

# NPM script
npm run docs:diagrams
```

**支援的格式：**
- **Mermaid** (`.mmd`)：驗證圖表類型、語法結構
- **PlantUML** (`.puml`, `.plantuml`)：檢查 `@startuml`/`@enduml` 標籤、平衡性
- **Excalidraw** (`.excalidraw`)：驗證 JSON 結構、必要欄位

**功能：**
- 自動偵測圖表類型
- 驗證語法結構和常見錯誤
- 產生包含錯誤位置的詳細報告
- 支援從圖表中提取 metadata

#### 3. Metadata Validator
**`validate-metadata.py`** - YAML front matter 驗證
```bash
# 驗證文件 metadata
python3 scripts/validate-metadata.py

# 詳細輸出
python3 scripts/validate-metadata.py --verbose

# 產生 metadata templates
python3 scripts/validate-metadata.py --generate-templates

# NPM script
npm run docs:metadata
```

**驗證規則：**
- **Viewpoints**：需要 `title`、`viewpoint`、`description`、`stakeholders`
- **Perspectives**：需要 `title`、`perspective`、`description`、`quality_attributes`
- **Templates**：需要 `title`、`type`、`description`、`usage`
- **General**：需要 `title`、`description`

**功能：**
- 驗證 YAML front matter 語法
- 依文件類型檢查必要欄位
- 針對標準清單驗證 viewpoint/perspective 值
- 為缺少的文件產生 metadata templates
- 交叉參照相關文件

#### 4. Translation Quality Checker
**`check-translation-quality.sh`** - 增強的翻譯同步
```bash
# 檢查翻譯品質
bash scripts/check-translation-quality.sh

# NPM script
npm run docs:translation
```

**功能：**
- 驗證文件結構和內容
- 驗證 Viewpoints & Perspectives 結構
- 支援新的文件組織

### Test and Validation Scripts

#### System Test Runner
**`test-documentation-quality.sh`** - 綜合系統測試
```bash
# 測試所有品質檢查元件
bash scripts/test-documentation-quality.sh
```

**功能：**
- 測試所有品質檢查腳本
- 建立包含已知問題的測試檔案
- 驗證腳本功能
- 產生測試報告

## Configuration Files

### Markdown Linting
**`.markdownlint.json`** - Markdown 語法規則
- 行長度：120 字元
- 允許 HTML 元素：`br`、`sub`、`sup`、`kbd`、`details`、`summary`
- 有序清單樣式強制執行
- 標題結構驗證



## NPM Scripts

新增至您的工作流程：
```bash
# 個別檢查
npm run docs:quality      # 綜合品質檢查
npm run docs:links        # 連結驗證（內部）
npm run docs:links:external # 連結驗證（外部）
npm run docs:diagrams     # 圖表驗證
npm run docs:metadata     # Metadata 驗證
npm run docs:translation  # 翻譯品質

# 組合驗證
npm run docs:validate     # 執行所有檢查
```

## GitHub Actions Integration

**`.github/workflows/documentation-quality.yml`** - 自動化 CI/CD 檢查

**觸發條件：**
- 推送至 `main`/`develop` 分支
- 影響文件的 pull requests
- 手動 workflow dispatch

**功能：**
- 自動執行所有品質檢查
- 上傳報告作為 artifacts
- 在 PRs 上留言品質摘要
- 支援外部連結檢查選項

## Reports and Output

### Report Structure
```
build/reports/documentation-quality/
├── reports-summaries/frontend/documentation-quality-summary.md     # 主要摘要報告
├── markdown-lint-report.txt             # Markdown 語法問題
├── link-check-report.txt                # 連結驗證結果
├── advanced-link-check.json             # 詳細連結分析
├── advanced-link-check.md               # 連結檢查摘要
├── diagram-validation-report.json       # 圖表驗證資料
├── reports-summaries/diagrams/diagram-validation-report.md         # 圖表驗證摘要
├── metadata-validation-report.json      # Metadata 驗證資料
├── metadata-validation-report.md        # Metadata 驗證摘要
└── translation-sync-report.txt          # 翻譯品質結果
```

### Report Contents
- **通過/失敗狀態**：每個檢查的清楚指示
- **錯誤詳情**：特定檔案位置和錯誤訊息
- **建議**：修復問題的可行步驟
- **統計**：覆蓋率百分比和成功率
- **趨勢**：可用時的歷史比較

## Prerequisites

### Required Tools
- **Node.js 18+**：用於連結檢查和 npm scripts
- **Python 3.8+**：用於圖表和 metadata 驗證
- **Bash**：用於 shell scripts (macOS/Linux)

### Python Dependencies
```bash
# 安裝 PyYAML 用於 metadata 驗證
pip3 install pyyaml

# 或使用系統套件管理器
brew install python-yq  # macOS
```

### Node.js Dependencies
```bash
# 全域安裝 markdownlint
npm install -g markdownlint-cli

# 或安裝專案相依套件
npm ci
```

## Usage Examples

### Daily Development Workflow
```bash
# commit 前的快速品質檢查
npm run docs:quality

# 修復發現的任何問題
# 重新執行特定檢查
npm run docs:links
npm run docs:metadata
```

### Pre-Release Validation
```bash
# 包含外部連結的綜合驗證
npm run docs:links:external
npm run docs:validate

# 檢視報告
open build/reports/documentation-quality/reports-summaries/frontend/documentation-quality-summary.md
```

### Continuous Integration
```bash
# 在 CI/CD pipeline 中
npm ci
npm run docs:validate

# 上傳報告以供檢視
# 如果發現關鍵問題則建置失敗
```

## Troubleshooting

### Common Issues

**1. PyYAML Not Available**
```bash
# 安裝 PyYAML
pip3 install --user pyyaml
# 或使用系統套件管理器
brew install python-yq
```

**2. Markdownlint Not Found**
```bash
# 全域安裝
npm install -g markdownlint-cli
# 或使用 npx
npx markdownlint docs/**/*.md
```

**3. Permission Denied**
```bash
# 使腳本可執行
chmod +x scripts/*.sh scripts/*.js scripts/*.py
```

**4. External Link Timeouts**
```bash
# 跳過外部連結以加快檢查速度
node scripts/check-links-advanced.js  # 僅內部
```

### Performance Optimization

**對於大型文件集：**
- 日常開發使用僅內部連結檢查
- 僅在 CI/CD 中執行外部連結檢查
- 使用 `--verbose` 旗標監控進度
- 考慮對多個目錄進行並行執行

**記憶體使用：**
- 圖表驗證：100+ 圖表約 50MB
- 連結檢查：300+ 文件約 100MB
- Metadata 驗證：典型文件約 20MB

## Integration with Development Workflow

### Pre-Commit Hooks
```bash
# 新增至 .git/hooks/pre-commit
#!/bin/bash
npm run docs:quality
if [ $? -ne 0 ]; then
    echo "Documentation quality checks failed. Please fix issues before committing."
    exit 1
fi
```

### IDE Integration
- 設定 markdownlint 擴充套件以進行即時語法檢查
- 設定檔案監看器以進行自動驗證
- 使用任務執行器進行整合的品質檢查

### Documentation Review Process
1. **作者**：建立 PR 前執行 `npm run docs:quality`
2. **審查者**：檢查 PR 留言中的品質報告
3. **維護者**：確保所有品質檢查在合併前通過
4. **發布**：執行包含外部連結的綜合驗證

此綜合品質檢查系統確保一致、高品質的文件，符合 Viewpoints & Perspectives 重組規格的要求。
