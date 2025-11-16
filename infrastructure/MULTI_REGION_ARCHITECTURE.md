# Multi-Region 基礎設施架構

本文件說明 GenAI Demo 專案的多區域災難復原基礎設施實作。

## 概述

多區域基礎設施提供跨台灣（ap-east-2）和東京（ap-northeast-1）區域的主動-主動災難復原能力，確保業務連續性，並最小化停機時間和零資料遺失。

## 架構元件

### 1. Multi-Region Stack（`MultiRegionStack`）

**目的**：協調跨區域基礎設施和容錯移轉機制。

**主要功能**：

- 台灣-東京連線的跨區域 VPC peering
- 多區域容錯移轉的 Route 53 health checks
- 跨區域 certificate 複寫策略
- 區域之間的 CloudFormation stack 相依性

**配置**：

```typescript
// 僅針對 production 環境啟用
if (environment === 'production' && multiRegionConfig['enable-dr']) {
  // 部署多區域協調
}
```

### 2. Disaster Recovery Stack（`DisasterRecoveryStack`）

**目的**：在次要區域（東京）部署完整基礎設施。

**主要功能**：

- 完整網路基礎設施（VPC、subnets、security groups）
- 具有 SSL 終止的 Application Load Balancer
- DR 網域的 ACM certificates（`dr.kimkao.io`）
- CloudWatch 監控儀表板
- 配置用的 Systems Manager parameter store
- 跨區域複寫設定

**資源規模**：

- DR 區域使用主要區域最小節點數的 50%
- DR 區域使用主要區域最大節點數的 80%
- DR 區域的 RDS 始終啟用 Multi-AZ

### 3. Route 53 Failover Stack（`Route53FailoverStack`）

**目的**：管理 DNS 容錯移轉和 health checks。

**主要功能**：

- 主要和次要區域的 health checks
- Failover DNS records（PRIMARY/SECONDARY）
- 基於延遲的路由以獲得最佳效能
- Health check 失敗的 CloudWatch alarms
- 容錯移轉事件的 SNS 通知

**Health Check 配置**：

- Endpoint：`/actuator/health`
- Protocol：HTTPS（埠 443）
- Interval：30 秒（可配置）
- Failure threshold：連續 3 次失敗（可配置）

## 配置

### CDK Context 配置

```json
{
  "genai-demo:regions": {
    "primary": "ap-east-2",
    "secondary": "ap-northeast-1",
    "regions": {
      "ap-east-2": {
        "name": "Taiwan",
        "type": "primary",
        "cost-optimization": {
          "spot-instances": false,
          "reserved-instances": true
        },
        "backup-retention": {
          "rds": 30,
          "logs": 90
        }
      },
      "ap-northeast-1": {
        "name": "Tokyo",
        "type": "secondary",
        "cost-optimization": {
          "spot-instances": false,
          "reserved-instances": true
        },
        "backup-retention": {
          "rds": 30,
          "logs": 90
        }
      }
    }
  },
  "genai-demo:multi-region": {
    "enable-dr": true,
    "enable-cross-region-peering": true,
    "enable-cross-region-replication": true,
    "failover-rto-minutes": 1,
    "failover-rpo-minutes": 0,
    "health-check-interval": 30,
    "health-check-failure-threshold": 3
  }
}
```

### 環境特定配置

```json
{
  "genai-demo:environments": {
    "production": {
      "vpc-cidr": "10.2.0.0/16",
      "nat-gateways": 3,
      "eks-node-type": "m6g.large",
      "eks-min-nodes": 2,
      "eks-max-nodes": 10
    },
    "production-dr": {
      "vpc-cidr": "10.3.0.0/16",
      "nat-gateways": 3,
      "eks-node-type": "m6g.large",
      "eks-min-nodes": 1,
      "eks-max-nodes": 8
    }
  }
}
```

## 部署策略

### 條件式部署

多區域基礎設施僅在以下情況部署：

1. 環境為 `production`
2. `genai-demo:multi-region.enable-dr` 為 `true`

### Stack 相依性

```
Primary Stack (ap-east-2)
    ↓
DR Stack (ap-northeast-1)
    ↓
Multi-Region Stack (ap-east-2)
```

### 部署指令

```bash
# 部署具有 multi-region DR 的 production
cdk deploy --context genai-demo:environment=production \
           --context genai-demo:domain=kimkao.io \
           --all
```

## DNS 和容錯移轉配置

### 主要 Endpoints

- `api.kimkao.io` - Failover endpoint（PRIMARY）
- `api-latency.kimkao.io` - 基於延遲的路由

### DR Endpoints

- `api.kimkao.io` - Failover endpoint（SECONDARY）
- `dr.kimkao.io` - 直接 DR 存取
- `api-dr.kimkao.io` - DR 特定 endpoint

### 容錯移轉行為

1. **正常運作**：流量路由到主要區域（台灣）
2. **主要失敗**：Route 53 偵測到 health check 失敗
3. **自動容錯移轉**：流量自動路由到次要區域（東京）
4. **復原**：當主要區域復原時，流量自動路由回主要區域

### RTO/RPO 目標

- **RTO（Recovery Time Objective）**：1 分鐘
- **RPO（Recovery Point Objective）**：0 分鐘（零資料遺失）

## 監控與告警

### CloudWatch Dashboards

1. **Multi-Region Monitoring Dashboard**
   - 兩個區域的 health check 狀態
   - VPC peering 連線狀態
   - 跨區域複寫指標

2. **Failover Monitoring Dashboard**
   - Health check 回應時間
   - 容錯移轉事件時間軸
   - DNS 查詢模式

3. **DR Monitoring Dashboard**
   - DR 區域基礎設施健康狀態
   - Application Load Balancer 指標
   - Certificate 驗證狀態

### CloudWatch Alarms

1. **Primary Health Check Failure**
   - 觸發容錯移轉到次要區域
   - 發送 SNS 通知

2. **Secondary Health Check Failure**
   - 表示兩個區域都停機
   - 發送重要告警

3. **VPC Peering Connection Status**
   - 監控跨區域連線
   - 連線失敗時告警

### SNS Topics

1. **Multi-Region Alerts**：`genai-demo-production-primary-multi-region-alerts`
2. **Failover Alerts**：`genai-demo-production-failover-alerts`
3. **DR Replication Notifications**：`genai-demo-production-dr-replication-notifications`

## 安全考量

### 網路安全

- **VPC Peering**：安全的跨區域通訊
- **Security Groups**：區域之間的最小權限存取
- **Private Subnets**：資料庫和應用程式層隔離

### Certificate 管理

- **ACM Certificates**：自動續約和驗證
- **跨區域複寫**：兩個區域都可用 certificates
- **DNS 驗證**：安全的 certificate 驗證程序

### 存取控制

- **IAM Roles**：區域特定的存取控制
- **Systems Manager**：安全的參數儲存
- **CloudTrail**：所有操作的稽核軌跡

## 成本最佳化

### 資源規模

- **DR 區域**：主要區域容量的 50-80%
- **Reserved Instances**：生產工作負載的成本最佳化
- **Spot Instances**：針對生產可靠性停用

### 資料傳輸

- **VPC Peering**：降低資料傳輸成本
- **區域最佳化**：最小化跨區域資料傳輸

## 營運程序

### 部署

1. 部署主要區域基礎設施
2. 部署 DR 區域基礎設施
3. 配置多區域協調
4. 驗證 health checks 和容錯移轉

### 監控

1. 監控 health check 狀態
2. 檢視容錯移轉指標
3. 驗證跨區域複寫
4. 測試災難復原程序

### 維護

1. 協調區域之間的維護時段
2. 每月測試容錯移轉程序
3. 在過期前更新 certificates
4. 檢視並更新 RTO/RPO 目標

## 測試與驗證

### Health Check 測試

```bash
# 測試主要區域 health check
curl -k https://api.kimkao.io/actuator/health

# 測試 DR 區域 health check
curl -k https://api-dr.kimkao.io/actuator/health
```

### 容錯移轉測試

1. **計劃性容錯移轉**：停用主要區域 health check
2. **驗證容錯移轉**：確認流量路由到 DR 區域
3. **復原測試**：重新啟用主要區域
4. **驗證復原**：確認流量路由回主要區域

### DNS 解析測試

```bash
# 測試 failover DNS 解析
dig api.kimkao.io

# 測試基於延遲的 DNS 解析
dig api-latency.kimkao.io
```

## 疑難排解

### 常見問題

1. **Health Check 失敗**
   - 驗證應用程式 health endpoint
   - 檢查 security group 規則
   - 驗證 certificate 配置

2. **DNS 解析問題**
   - 檢查 Route 53 health check 狀態
   - 驗證 DNS record 配置
   - 驗證 TTL 設定

3. **跨區域連線**
   - 驗證 VPC peering 連線狀態
   - 檢查 route table 配置
   - 驗證 security group 規則

### 監控指令

```bash
# 檢查 health check 狀態
aws route53 get-health-check --health-check-id <health-check-id>

# 檢查 VPC peering 狀態
aws ec2 describe-vpc-peering-connections --vpc-peering-connection-ids <peering-id>

# 檢查 CloudWatch alarms
aws cloudwatch describe-alarms --alarm-names <alarm-name>
```

## 未來增強

### 規劃的改進

1. **自動化容錯移轉測試**：每月自動化 DR 測試
2. **增強監控**：額外的指標和儀表板
3. **成本最佳化**：基於需求的動態資源擴展
4. **安全增強**：額外的安全控制和監控

### 可擴展性考量

1. **額外區域**：支援兩個以上區域
2. **全球負載平衡**：增強的流量分配
3. **邊緣位置**：CloudFront 整合以獲得全球效能
4. **資料庫複寫**：增強的跨區域資料庫複寫

此多區域架構為災難復原提供堅實的基礎，同時在地理區域之間維持高可用性和效能。
