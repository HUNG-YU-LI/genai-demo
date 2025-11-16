# Staging Tests

此目錄包含在 staging 環境中使用真實服務執行的綜合整合、效能和跨區域測試。

## 目錄結構

```
staging-tests/
├── integration/           # 與真實服務的整合測試
│   ├── database/         # PostgreSQL 和 Aurora 整合測試
│   ├── cache/           # Redis cluster 整合測試
│   ├── messaging/       # Kafka 整合測試
│   └── monitoring/      # 可觀測性和監控測試
├── performance/         # Gatling 效能測試
│   ├── simulations/     # Gatling 模擬腳本
│   └── scenarios/       # 測試場景設定
├── cross-region/        # 跨區域和災難復原測試
│   ├── disaster-recovery/  # DR 場景測試
│   └── replication/     # 跨區域複寫測試
├── config/              # Docker Compose 和服務設定
│   ├── docker-compose-staging.yml
│   └── service-configs/ # 個別服務設定
└── scripts/             # 自動化和執行腳本
    ├── run-integration-tests.sh
    ├── run-performance-tests.sh
    └── run-cross-region-tests.sh
```

## 測試類別

### Integration Tests
- **Database**: 連線池、容錯移轉、效能驗證
- **Cache**: Redis cluster 操作、Sentinel 容錯移轉場景
- **Messaging**: Kafka 吞吐量、分區重新平衡、跨區域複寫
- **Monitoring**: 健康檢查、指標收集、告警驗證

### Performance Tests
- **Load Testing**: 使用 Gatling 的正常和尖峰負載場景
- **Stress Testing**: 系統臨界點識別
- **Endurance Testing**: 長時間執行穩定性驗證
- **Baseline Testing**: 效能回歸檢測

### Cross-Region Tests
- **Data Consistency**: 跨區域資料複寫和一致性驗證
- **Failover Scenarios**: 完整和部分區域故障測試
- **Load Balancing**: 地理、加權、基於健康和基於容量的路由
- **Business Flows**: 跨多個區域的端對端業務工作流程
- **Disaster Recovery**: 多區域容錯移轉場景
- **Network Partitioning**: Split-brain 和網路故障場景

## 執行

### Prerequisites
- 已安裝 Docker 和 Docker Compose
- Python 3.11+ 及必要套件
- 已安裝 Gatling 用於效能測試
- 已設定 AWS CLI 用於跨區域測試

### Running Tests

```bash
# 執行所有整合測試
./scripts/run-integration-tests.sh

# 執行效能測試
./scripts/run-performance-tests.sh

# 執行跨區域測試
./scripts/run-cross-region-tests.sh

# 執行特定測試類別
./scripts/run-integration-tests.sh --category database
./scripts/run-performance-tests.sh --scenario normal-load

# 執行特定跨區域測試
python3 cross-region/test_cross_region_data_consistency.py
python3 cross-region/test_failover_scenarios.py
python3 cross-region/test_load_balancing.py
python3 cross-region/test_end_to_end_business_flow.py
```

### Cross-Region Test Details

#### Data Consistency Tests (`test_cross_region_data_consistency.py`)
測試多個 AWS 區域之間的資料複寫和一致性：
- **Write and Replicate**: 驗證寫入一個區域的資料在 100ms 內複寫到其他區域（P99）
- **Concurrent Writes**: 測試多個區域的並發寫入，具備最終一致性
- **Conflict Resolution**: 驗證 Last-Write-Wins (LWW) 衝突解決策略

**Configuration**:
```python
config = TestConfig(
    primary_region="us-east-1",
    secondary_regions=["us-west-2", "eu-west-1"],
    max_replication_delay_ms=100,  # P99 目標
    api_endpoints={
        "us-east-1": "https://api-us-east-1.example.com",
        "us-west-2": "https://api-us-west-2.example.com",
        "eu-west-1": "https://api-eu-west-1.example.com"
    }
)
```

#### Failover Scenarios Tests (`test_failover_scenarios.py`)
測試容錯移轉機制和系統韌性：
- **Complete Region Failure**: 驗證 RTO < 2 分鐘的自動容錯移轉
- **Partial Service Failure**: 測試優雅降級和流量路由
- **Network Partition**: 驗證 split-brain 預防和一致性
- **Automatic Recovery**: 測試區域復原時的流量重新分配

**Success Criteria**:
- 容錯移轉時間 ≤ 120 秒（RTO 目標）
- 容錯移轉期間無資料遺失（RPO < 1 秒）
- 容錯移轉期間可用性 ≥ 99%
- 自動復原和重新平衡

#### Load Balancing Tests (`test_load_balancing.py`)
測試跨區域的流量分配：
- **Geographic Routing**: 使用者路由到最近的區域（>95% 準確度）
- **Weighted Routing**: 依據設定的權重分配流量
- **Health-Based Routing**: 流量避開不健康的區域
- **Capacity-Based Routing**: 區域達到容量時流量轉移

**Performance Targets**:
- P95 延遲 < 200ms
- 路由準確度 > 95%
- 錯誤率 < 1%
- 流量分配在目標權重的 5% 內

#### End-to-End Business Flow Tests (`test_end_to_end_business_flow.py`)
測試跨區域的完整業務工作流程：
- **Customer Registration and Order**: 跨區域的完整客戶生命週期
- **Cross-Region Order Fulfillment**: 庫存分配和履行
- **Payment Processing**: 多區域支付協調

**Workflow Validation**:
- 所有工作流程步驟成功完成
- 跨區域維持資料一致性
- 工作流程完成時間 < 30 秒
- 無資料遺失或損毀

### Test Reports

測試結果和報告產生於：
- `reports/integration/` - 整合測試結果
- `reports/performance/` - Gatling 效能報告
- `reports/cross-region/` - 跨區域測試結果

## Configuration

### Environment Variables
- `TARGET_HOST` - 目標應用程式主機（預設：localhost:8080）
- `TEST_ENVIRONMENT` - 測試環境（staging、production）
- `AWS_REGION` - 測試的主要 AWS 區域
- `SECONDARY_REGION` - 跨區域測試的次要區域

### Service Configuration
服務透過 Docker Compose 以類生產環境設定進行配置：
- PostgreSQL，具備連線池和複寫
- Redis cluster，具備 Sentinel 以實現高可用性
- Kafka cluster，具備跨區域複寫
- 監控堆疊，包含 Prometheus 和 Grafana

## Development Guidelines

### Adding New Tests
1. 選擇適當的測試類別（integration/performance/cross-region）
2. 遵循現有測試結構和命名慣例
3. 包含適當的清理和資源管理
4. 新增測試文件和預期結果
5. 更新相關執行腳本

### Test Data Management
- 使用反映生產模式的真實測試資料
- 實作適當的清理程序
- 避免硬編碼值，使用設定檔
- 考慮資料隱私和安全要求

### Performance Considerations
- 測試應盡可能設計為並行執行
- 包含適當的資源限制和逾時
- 監控測試執行的資源使用
- 實作效能回歸檢測
