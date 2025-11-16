# Context Viewpoint Overview

> **最後更新**：2025-10-23
> **狀態**：使用中
> **利害關係人**：業務分析師、架構師、產品經理

## 目的

Context Viewpoint 描述企業電子商務平台與其環境的關係。它定義系統邊界、識別與平台互動的外部系統和參與者，並釐清系統範圍內和範圍外的內容。

此 viewpoint 幫助利害關係人理解：

- **系統邊界**：系統的一部分和外部的內容
- **外部互動**：系統如何與外部實體通訊
- **利害關係人關注點**：誰對系統感興趣及其關注點
- **環境約束**：影響系統的外部因素

## 主要關注點

此 viewpoint 解決以下關注點：

1. **範圍定義**：系統與其環境之間的明確邊界
2. **外部相依性**：平台依賴的第三方系統和服務
3. **整合點**：系統如何與外部實體連接
4. **利害關係人識別**：誰與系統互動或對系統感興趣
5. **監管約束**：外部合規和監管要求
6. **地理約束**：區域和位置限制

## 系統脈絡

### 高階脈絡

企業電子商務平台是一個綜合線上零售系統，使客戶能夠瀏覽產品、下訂單和管理帳戶。該系統整合多個外部服務以提供完整的電子商務功能。

#### 系統脈絡圖

![System Context Diagram](../../diagrams/generated/context/system-context.png)

上圖顯示位於中心的電子商務平台，以及所有外部參與者（使用者和系統）及其互動。關鍵元素包括：

- **使用者**：客戶、賣家、管理員、支援人員
- **外部系統**：Payment Gateway、Email Service、SMS Service、Shipping Providers
- **雲端基礎設施**：AWS 託管和管理服務
- **整合模式**：同步 API 呼叫（實線箭頭）和非同步 webhooks（虛線箭頭）

### 系統邊界

#### 系統內部

企業電子商務平台包括：

1. **Customer Management**
   - 客戶註冊和驗證
   - 個人檔案管理
   - 客戶偏好設定和設定
   - 會員等級和忠誠計劃

2. **Product Catalog**
   - 產品資訊管理
   - 類別和分類管理
   - 產品搜尋和篩選
   - 產品評論和評分

3. **Order Management**
   - 購物車功能
   - 訂單建立和提交
   - 訂單追蹤和歷史記錄
   - 訂單取消和退貨

4. **Payment Processing**
   - 付款方式管理
   - 付款交易協調
   - 付款狀態追蹤
   - 退款處理

5. **Inventory Management**
   - 庫存水平追蹤
   - 庫存預留
   - 補貨通知
   - 多倉庫支援

6. **Promotion Engine**
   - 折扣規則管理
   - 優惠券代碼處理
   - 促銷活動
   - 動態定價

7. **Pricing Engine**
   - 價格計算
   - 稅金計算
   - 運費計算
   - 貨幣轉換

8. **Notification System**
   - 電子郵件通知協調
   - SMS 通知協調
   - 應用程式內通知管理
   - 通知偏好設定

#### 系統外部

以下是平台外部的內容：

1. **Payment Gateway** (Stripe)
   - 信用卡處理
   - 付款授權
   - 詐欺偵測
   - PCI 合規

2. **Shipping Providers** (FedEx, UPS, DHL)
   - 包裹取件
   - 配送追蹤
   - 運送標籤產生
   - 配送確認

3. **Email Service** (SendGrid)
   - 電子郵件傳送
   - 電子郵件範本渲染
   - 退信處理
   - 電子郵件分析

4. **SMS Service** (Twilio)
   - SMS 訊息傳送
   - 電話號碼驗證
   - 傳送回條

5. **Analytics Platform** (Google Analytics)
   - 使用者行為追蹤
   - 轉換追蹤
   - 流量分析
   - 行銷歸因

6. **Warehouse Management System**
   - 實體庫存管理
   - 倉庫營運
   - 揀貨和包裝
   - 出貨準備

7. **Accounting System**
   - 財務報表
   - 收入確認
   - 稅務申報
   - 應付/應收帳款

8. **Customer Support System** (Zendesk)
   - 工單管理
   - 客戶服務互動
   - 知識庫
   - 即時聊天

## 外部參與者

### 主要參與者

#### 1. Customers (終端使用者)

**描述**：瀏覽產品、下訂單和管理帳戶的個人。

**互動**：

- 瀏覽產品目錄
- 搜尋產品
- 將商品加入購物車
- 下訂單和追蹤訂單
- 管理帳戶和偏好設定
- 撰寫產品評論

**存取方式**：網頁瀏覽器、行動應用程式

**驗證**：使用者名稱/密碼、OAuth (Google, Facebook)

#### 2. Sellers/Merchants

**描述**：在平台上列出和銷售產品的第三方賣家。

**互動**：

- 管理產品列表
- 查看銷售報告
- 處理訂單
- 管理庫存
- 處理客戶諮詢

**存取方式**：賣家入口網站（網頁）

**驗證**：使用者名稱/密碼、2FA

#### 3. Administrators

**描述**：管理平台和支援營運的內部員工。

**互動**：

- 管理使用者和權限
- 配置系統設定
- 監控系統健康狀況
- 處理客戶支援升級
- 產生報告

**存取方式**：管理控制台（網頁）

**驗證**：使用者名稱/密碼、2FA、SSO

### 次要參與者

#### 4. Payment Gateway (Stripe)

**描述**：處理付款交易的外部服務。

**互動**：

- 處理信用卡付款
- 處理付款授權
- 管理付款方式
- 處理退款
- 提供詐欺偵測

**整合方式**：REST API、Webhooks

**驗證**：API keys、OAuth

#### 5. Shipping Providers (FedEx, UPS, DHL)

**描述**：處理包裹配送的第三方物流公司。

**互動**：

- 產生運送標籤
- 計算運費
- 追蹤包裹配送
- 提供配送確認
- 處理退貨

**整合方式**：REST API、SOAP API

**驗證**：API keys、OAuth

#### 6. Email Service (SendGrid)

**描述**：用於交易和行銷電子郵件的郵件傳送服務。

**互動**：

- 發送交易郵件（訂單確認、密碼重設）
- 發送行銷郵件（促銷、電子報）
- 追蹤電子郵件傳送和開啟
- 處理退信和取消訂閱

**整合方式**：REST API、SMTP

**驗證**：API keys

#### 7. SMS Service (Twilio)

**描述**：用於通知和警示的 SMS 傳送服務。

**互動**：

- 發送訂單狀態更新
- 發送配送通知
- 發送驗證碼
- 提供客戶支援

**整合方式**：REST API

**驗證**：API keys

#### 8. Analytics Platform (Google Analytics)

**描述**：用於追蹤使用者行為和轉換的網頁分析服務。

**互動**：

- 追蹤頁面瀏覽和使用者會話
- 追蹤電子商務交易
- 追蹤轉換漏斗
- 提供行銷歸因

**整合方式**：JavaScript SDK、Measurement Protocol API

**驗證**：Tracking ID

## 利害關係人群組

### 利害關係人圖譜

![Stakeholder Map](../../diagrams/generated/context/stakeholder-map.png)

上方的利害關係人圖譜視覺化所有利害關係人群組及其與電子商務平台的關係。它顯示：

- **利害關係人類別**：業務、技術、終端使用者、外部
- **參與程度**：每日、每週、每季、視需要
- **影響程度**：高、中、低
- **關係**：直接互動（實線）和協作（虛線）

### 業務利害關係人

#### Product Owners

**關注點**：

- 功能優先順序
- 使用者體驗
- 業務價值交付
- 市場競爭力

**資訊需求**：

- 產品路線圖
- 功能規格
- 使用者回饋
- 業務指標

#### Marketing Team

**關注點**：

- 客戶獲取
- 轉換優化
- 活動效果
- 品牌一致性

**資訊需求**：

- 分析資料
- 活動效能
- 客戶分群
- A/B 測試結果

#### Finance Team

**關注點**：

- 收入追蹤
- 成本管理
- 財務報表
- 合規

**資訊需求**：

- 交易資料
- 收入報告
- 成本分析
- 稽核追蹤

#### Customer Support Team

**關注點**：

- 客戶滿意度
- 問題解決
- 支援效率
- 知識管理

**資訊需求**：

- 客戶資料
- 訂單歷史
- 問題追蹤
- 支援指標

### 技術利害關係人

#### Development Team

**關注點**：

- 程式碼品質
- 技術債務
- 開發速度
- 系統可維護性

**資訊需求**：

- 架構文件
- API 規格
- 開發指南
- 技術路線圖

#### Operations Team (SRE/DevOps)

**關注點**：

- 系統可靠性
- 效能
- 可擴展性
- 事件管理

**資訊需求**：

- 基礎設施文件
- 監控儀表板
- 操作手冊
- 部署程序

#### Security Team

**關注點**：

- 資料保護
- 漏洞管理
- 合規
- 事件回應

**資訊需求**：

- 安全架構
- 威脅模型
- 合規報告
- 安全政策

#### Quality Assurance Team

**關注點**：

- 軟體品質
- 測試覆蓋率
- 錯誤追蹤
- 發布就緒度

**資訊需求**：

- 測試計劃
- 測試結果
- 錯誤報告
- 品質指標

### 外部利害關係人

#### Customers

**關注點**：

- 產品可用性
- 訂單履行
- 付款安全
- 客戶服務

**資訊需求**：

- 產品資訊
- 訂單狀態
- 配送追蹤
- 支援聯絡

#### Partners/Sellers

**關注點**：

- 銷售表現
- 佣金結構
- 平台可靠性
- 支援品質

**資訊需求**：

- 銷售報告
- 佣金報表
- 平台狀態
- 整合文件

#### Regulators

**關注點**：

- 資料隱私（GDPR、CCPA）
- 消費者保護
- 稅務合規
- 無障礙存取

**資訊需求**：

- 合規報告
- 隱私政策
- 稽核追蹤
- 無障礙聲明

## 環境約束

### 監管約束

#### 資料隱私法規

**GDPR (General Data Protection Regulation)**

- 適用於：歐盟客戶
- 要求：
  - 存取個人資料的權利
  - 刪除權（「被遺忘權」）
  - 資料可攜性
  - 同意管理
  - 資料外洩通知

**CCPA (California Consumer Privacy Act)**

- 適用於：加州居民
- 要求：
  - 知道收集哪些資料的權利
  - 刪除個人資料的權利
  - 選擇退出資料銷售的權利
  - 不歧視

#### Payment Card Industry (PCI DSS)

- 適用於：所有付款處理
- 要求：
  - 安全的網路基礎設施
  - 保護持卡人資料
  - 漏洞管理
  - 存取控制
  - 定期監控和測試

#### 無障礙標準

**WCAG 2.1 (Web Content Accessibility Guidelines)**

- 等級：需符合 AA 級
- 要求：
  - 可感知的內容
  - 可操作的介面
  - 可理解的資訊
  - 穩健的實作

### 地理約束

#### 區域可用性

- **主要市場**：北美、歐洲、亞太地區
- **語言支援**：英文、西班牙文、法文、德文、中文、日文
- **貨幣支援**：USD、EUR、GBP、JPY、CNY

#### 資料駐留要求

- **歐盟**：客戶資料必須儲存在歐盟境內
- **中國**：客戶資料必須儲存在中國境內
- **其他地區**：無特定要求

#### 區域限制

- **運送**：某些產品無法運送到特定國家
- **付款方式**：可用性因地區而異
- **內容**：某些內容可能在特定地區受到限制

### 技術約束

#### 外部服務相依性

- **Payment Gateway**：99.9% 正常運行時間 SLA
- **Email Service**：適用速率限制
- **SMS Service**：每則訊息費用
- **Shipping APIs**：速率限制和配額

#### 整合協定

- **REST APIs**：主要整合方式
- **Webhooks**：用於非同步通知
- **Message Queues**：用於內部事件處理
- **Batch Processing**：用於大量資料傳輸

#### 效能要求

- **API 回應時間**：< 2 秒（第 95 百分位）
- **頁面載入時間**：< 3 秒
- **並行使用者**：支援 10,000+ 並行使用者
- **交易量**：每小時處理 1,000+ 訂單

## 整合模式

### 外部整合概覽

![External Integrations Diagram](../../diagrams/generated/context/external-integrations.png)

上圖顯示所有外部系統整合及其關鍵性等級、整合模式和關鍵特性。每個整合包括：

- **關鍵性等級**：關鍵（紅色）、高（橙色）、中（黃色）
- **整合模式**：同步 API、非同步 webhooks 或兩者
- **關鍵細節**：驗證、速率限制、SLA、重試邏輯、備援策略

### 同步整合

用於：

- 付款處理
- 即時庫存檢查
- 運費計算
- 地址驗證

**模式**：透過 REST API 的請求-回應

**特性**：

- 需要立即回應
- 強一致性
- 逾時處理
- 重試邏輯

### 非同步整合

用於：

- 電子郵件通知
- SMS 通知
- 分析追蹤
- 倉庫更新

**模式**：透過 Message Queue (Kafka) 的事件驅動

**特性**：

- 最終一致性
- 解耦系統
- 對故障具韌性
- 可擴展

### 批次整合

用於：

- 每日銷售報告
- 庫存同步
- 財務對帳
- 資料倉儲更新

**模式**：排程批次作業

**特性**：

- 定期執行
- 大量資料
- 離峰處理
- 錯誤復原

## 系統介面

### 入站介面

1. **Web Application** (HTTPS)
   - 面向客戶的網站
   - 賣家入口網站
   - 管理控制台

2. **Mobile Application** (HTTPS/REST API)
   - iOS 應用程式
   - Android 應用程式

3. **Partner APIs** (REST API)
   - 第三方整合
   - 市場整合

### 出站介面

1. **Payment Gateway API** (REST)
   - 付款處理
   - 退款處理

2. **Shipping Provider APIs** (REST/SOAP)
   - 標籤產生
   - 追蹤更新

3. **Email Service API** (REST/SMTP)
   - 交易郵件
   - 行銷郵件

4. **SMS Service API** (REST)
   - 通知訊息
   - 驗證碼

5. **Analytics API** (JavaScript/REST)
   - 事件追蹤
   - 轉換追蹤

## 導覽

### 相關文件

- [Scope & Boundaries](scope-and-boundaries.md) - 詳細範圍定義 →
- [External Systems](external-systems.md) - 整合細節 →
- [Stakeholders](stakeholders.md) - 利害關係人分析 →

### 相關 Viewpoints

- [Functional Viewpoint](../functional/README.md) - 內部能力
- [Deployment Viewpoint](../deployment/README.md) - 基礎設施
- [Operational Viewpoint](../operational/README.md) - 營運

### 相關 Perspectives

- [Security Perspective](../../perspectives/security/README.md) - 外部安全
- [Location Perspective](../../perspectives/location/README.md) - 地理分布

---

**下一步**：[Scope & Boundaries →](scope-and-boundaries.md)
