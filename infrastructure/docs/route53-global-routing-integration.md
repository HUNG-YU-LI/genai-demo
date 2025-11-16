# Route53 Global Routing Stack 整合指南

## 概述

`Route53GlobalRoutingStack` 擴展了現有的 Route53 容錯移轉功能，提供真正的 Active-Active 多區域 DNS 路由，具有智能流量分配、即時健康監控和 A/B 測試功能。

## 主要功能

### 🌍 **基於地理位置的路由**

- 根據使用者的地理位置路由流量
- 將使用者導向最近的區域以優化效能
- 支援洲級路由（北美、歐洲、亞洲）
- 包含未匹配位置的預設後援

### ⚖️ **用於 A/B 測試的加權路由**

- 可配置的跨區域流量分配
- 預設分配：主要（70%）、次要（20%）、第三（10%）
- 啟用受控的推出和功能測試
- 即時流量分配監控

### 🚀 **基於延遲的路由**

- 路由到延遲最低的區域以獲得最佳效能
- 基於即時測量的自動效能優化
- 當區域變得不可用時的後援機制

### 🏥 **增強的健康監控**

- 30 秒的健康檢查間隔（可配置）
- `/actuator/health` 端點上的 HTTPS 健康檢查
- 多區域健康檢查執行以提高可靠性
- 延遲測量和 SNI 支援

## 與現有基礎設施的整合

### 基於現有的 Route53 Failover Stack

```typescript
// 擴展現有的 route53-failover-stack.ts 功能
// 維持與當前容錯移轉設定的向後相容性
// 在保留現有健康檢查的同時新增新的路由類型
```

### Certificate Stack 整合

```typescript
// 使用來自 certificate-stack.ts 的現有 SSL 憑證
// 利用現有的託管區域配置
// 維護憑證驗證和監控
```

### Core Infrastructure 整合

```typescript
// 與來自 core-infrastructure-stack.ts 的現有 ALB 整合
// 使用現有的負載平衡器健康檢查端點
// 維護現有的監控和告警系統
```

## 使用範例

```typescript
import { Route53GlobalRoutingStack } from '../src/stacks/route53-global-routing-stack';

// 使用現有基礎設施建立全域路由
const globalRouting = new Route53GlobalRoutingStack(this, 'GlobalRouting', {
    environment: 'production',
    projectName: 'genai-demo',
    domain: 'api.genai-demo.com',
    hostedZone: certificateStack.hostedZone,
    certificate: certificateStack.certificate,
    regions: {
        primary: {
            region: 'us-east-1',
            loadBalancer: primaryInfraStack.loadBalancer,
            weight: 70 // 70% 流量用於 A/B 測試
        },
        secondary: {
            region: 'eu-west-1',
            loadBalancer: secondaryInfraStack.loadBalancer,
            weight: 20 // 20% 流量用於 A/B 測試
        },
        tertiary: {
            region: 'ap-southeast-1',
            loadBalancer: tertiaryInfraStack.loadBalancer,
            weight: 10 // 10% 流量用於 A/B 測試
        }
    },
    monitoringConfig: {
        healthCheckInterval: 30, // 30 秒間隔
        failureThreshold: 3,     // 3 次失敗觸發容錯移轉
        enableABTesting: true,   // 啟用加權路由
        enableGeolocationRouting: true // 啟用地理路由
    }
});
```

## 建立的 DNS 端點

### 1. Geolocation Routing

- **端點**：`api-geo.{domain}`
- **用途**：根據使用者的地理位置進行路由
- **路由邏輯**：
  - 北美 → 主要區域
  - 歐洲 → 次要區域
  - 亞洲 → 第三區域
  - 預設 → 主要區域

### 2. Weighted Routing（A/B 測試）

- **端點**：`api-weighted.{domain}`
- **用途**：根據配置的權重分配流量
- **預設分配**：
  - 主要：70%
  - 次要：20%
  - 第三：10%

### 3. Latency-Based Routing

- **端點**：`api-latency.{domain}`
- **用途**：路由到延遲最低的區域
- **優勢**：為每個使用者提供最佳效能

## 監控和告警

### CloudWatch 儀表板

- 所有區域的健康檢查狀態
- 按路由類型的 DNS 查詢指標
- 跨區域的延遲比較
- 流量分配視覺化

### 自動化告警

- 健康檢查失敗通知
- 高延遲警告（> 2 秒）
- 全域系統健康複合警報
- DNS 查詢率監控（DDoS 偵測）

### SNS 整合

- 所有路由事件的集中告警主題
- 與現有通知系統整合
- 關鍵失敗的升級政策

## 配置選項

### 健康檢查配置

```typescript
monitoringConfig: {
    healthCheckInterval: 30,    // 檢查之間的秒數
    failureThreshold: 3,        // 容錯移轉前的失敗次數
    enableABTesting: true,      // 啟用加權路由
    enableGeolocationRouting: true // 啟用地理路由
}
```

### 流量分配

```typescript
regions: {
    primary: { weight: 70 },    // 70% 的流量
    secondary: { weight: 20 },  // 20% 的流量
    tertiary: { weight: 10 }    // 10% 的流量
}
```

## 滿足的需求

### ✅ 需求 4.1.3 - Global Routing

- ✅ 基於地理位置的智能路由
- ✅ 從現有 Certificate Stack 整合 SSL 憑證
- ✅ 具有 30 秒間隔的即時健康檢查
- ✅ 用於 A/B 測試支援的加權路由
- ✅ 與現有監控系統整合

## 部署考量

### 前置條件

1. 已部署帶有 SSL 憑證的現有 Certificate Stack
2. 具有 Application Load Balancers 的 Core Infrastructure Stack
3. 具有健康端點的多區域部署
4. 已配置並可存取的 Route53 託管區域

### 部署順序

1. 部署 Certificate Stack（現有）
2. 在所有區域部署 Core Infrastructure Stacks（現有）
3. 部署 Route53 Global Routing Stack（新）
4. 驗證健康檢查和 DNS 解析
5. 測試流量分配和容錯移轉場景

### 測試檢查清單

- [ ] 所有區域的健康檢查通過
- [ ] 所有端點類型的 DNS 解析正常
- [ ] 地理位置路由正確導向流量
- [ ] 加權路由按配置分配流量
- [ ] 延遲路由選擇最佳區域
- [ ] 當區域變得不健康時容錯移轉正常運作
- [ ] 監控儀表板顯示準確的指標
- [ ] 失敗場景正確觸發告警

## 疑難排解

### 常見問題

1. **健康檢查失敗**：驗證 `/actuator/health` 端點可存取性
2. **DNS 無法解析**：檢查託管區域配置和傳播
3. **路由不正確**：驗證區域配置和權重
4. **缺少指標**：確保配置了 CloudWatch 權限

### 除錯指令

```bash
# 測試 DNS 解析
dig api-geo.genai-demo.com
dig api-weighted.genai-demo.com
dig api-latency.genai-demo.com

# 檢查健康檢查狀態
aws route53 get-health-check --health-check-id <health-check-id>

# 監控 CloudWatch 指標
aws cloudwatch get-metric-statistics --namespace AWS/Route53 --metric-name HealthCheckStatus
```

## 效能目標

- **全域 P95 回應時間**：< 200ms
- **健康檢查間隔**：30 秒
- **容錯移轉時間**：< 2 分鐘（RTO）
- **資料遺失**：< 1 秒（RPO）
- **系統可用性**：≥ 99.99%

此實作為真正的 Active-Active 多區域架構提供了基礎，具有智能 DNS 路由、全面監控和自動化容錯移轉功能。
