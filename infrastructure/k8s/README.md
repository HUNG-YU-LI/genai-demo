# GenAI Demo 的 Kubernetes Manifests

本目錄包含用於將 GenAI Demo 應用程式部署到 Amazon EKS 的 Kubernetes manifests。

## 結構

```
k8s/
├── base/                    # 基本 Kubernetes manifests
│   ├── namespace.yaml       # 應用程式 namespaces
│   ├── deployment.yaml      # 應用程式 deployment
│   ├── service.yaml         # Kubernetes services
│   ├── ingress.yaml         # ALB ingress 配置
│   ├── hpa.yaml            # Horizontal Pod Autoscaler
│   └── serviceaccount.yaml  # Service accounts 和 RBAC
├── overlays/               # 環境特定配置
│   ├── development/        # Development 環境
│   │   ├── kustomization.yaml
│   │   ├── deployment-patch.yaml
│   │   ├── hpa-patch.yaml
│   │   └── ingress-patch.yaml
│   └── production/         # Production 環境
│       ├── kustomization.yaml
│       ├── deployment-patch.yaml
│       └── ingress-patch.yaml
└── README.md              # 本文件
```

## 功能

### 基本配置

- **Namespace**：應用程式和系統組件的獨立 namespaces
- **Deployment**：具備全面健康檢查的 Spring Boot 應用程式
- **Service**：ClusterIP service，具備用於服務探索的 headless service
- **Ingress**：AWS Load Balancer Controller 整合，具備 SSL 終止
- **HPA**：基於 CPU 和記憶體指標的 Horizontal Pod Autoscaler
- **ServiceAccount**：啟用 IRSA 的 service accounts，用於 AWS 整合

### 安全功能

- 非 root 容器執行
- 唯讀根檔案系統
- 具備已移除能力的安全 context
- Pod 安全標準合規性
- 網路政策（待新增）

### 可觀測性

- Prometheus 指標抓取註釋
- 全面的健康檢查（liveness、readiness、startup）
- 具備關聯 ID 的結構化日誌記錄
- 分散式追蹤整合

### 高可用性

- Pod 反親和性規則
- 生產環境的多個副本
- 滾動更新策略
- 資源請求和限制

## 環境差異

### Development

- 單一副本
- x86_64 架構（t3.medium 執行個體）
- 降低的資源需求
- Development Spring profile

### Production

- 多個副本（3）
- ARM64 架構（Graviton3 m6g.large 執行個體）
- 更高的資源配置
- Production Spring profile
- WAF 整合
- 增強的監控

## 部署

### 先決條件

1. 已安裝 AWS Load Balancer Controller 的 EKS cluster
2. 用於 HPA 的 Metrics Server
3. 用於節點擴展的 Cluster Autoscaler
4. 為 service accounts 配置的 IRSA roles

### 使用 Kustomize

```bash
# Development 部署
kubectl apply -k overlays/development/

# Production 部署
kubectl apply -k overlays/production/
```

### 使用 Helm（替代方案）

Manifests 也可以使用 Helm 進行範本化，以獲得更動態的配置。

## 配置

### 環境變數

- `SPRING_PROFILES_ACTIVE`：Spring Boot profile（dev/prod）
- `ENVIRONMENT`：部署環境
- `POD_NAME`、`POD_IP`、`NODE_NAME`：Kubernetes 元資料

### 資源需求

| 環境 | CPU Request | Memory Request | CPU Limit | Memory Limit |
|-------------|-------------|----------------|-----------|--------------|
| Development | 250m        | 512Mi          | 1         | 1Gi          |
| Production  | 500m        | 1Gi            | 2         | 2Gi          |

### 健康檢查

- **Startup Probe**：`/actuator/health`（30秒初始延遲，最多 30 次失敗）
- **Liveness Probe**：`/actuator/health/liveness`（60秒初始延遲）
- **Readiness Probe**：`/actuator/health/readiness`（30秒初始延遲）

## 監控和可觀測性

### 指標

- 在埠 8080 的 `/actuator/prometheus` 啟用 Prometheus 抓取
- 透過 Micrometer 公開的自訂業務指標
- 包含 JVM 和系統指標

### 日誌記錄

- 結構化 JSON 日誌記錄到 stdout
- 透過 Fluent Bit 到 CloudWatch 的日誌聚合
- 用於請求追蹤的關聯 ID

### 追蹤

- OpenTelemetry 整合
- 用於分散式追蹤的 AWS X-Ray
- 用於本地開發的 Jaeger

## 擴展

### Horizontal Pod Autoscaler

- CPU 目標：70% 利用率
- 記憶體目標：80% 利用率
- 擴展：每 15 秒增加 100%（最多）
- 縮減：每 60 秒減少 10%（最多）

### Cluster Autoscaler

- 基於 pod 資源請求的自動節點擴展
- 用於成本優化的 Graviton3 執行個體
- 用於開發環境的 Spot 執行個體

## 安全性

### RBAC

- 最小權限 service accounts
- 用於 AWS 服務存取的 IRSA
- 無 cluster-admin 權限

### 網路安全

- 用於 worker 節點的私有子網路
- 用於流量控制的安全群組
- 用於生產環境 ingress 的 WAF 保護

### 容器安全

- 非 root 使用者執行
- 唯讀根檔案系統
- 最小基本映像
- 定期安全掃描

## 疑難排解

### 常見問題

1. **Pod 未啟動**

   ```bash
   kubectl describe pod -l app=genai-demo -n genai-demo
   kubectl logs -l app=genai-demo -n genai-demo
   ```

2. **健康檢查失敗**

   ```bash
   kubectl get events -n genai-demo
   kubectl logs -l app=genai-demo -n genai-demo --previous
   ```

3. **Ingress 無法運作**

   ```bash
   kubectl describe ingress genai-demo-ingress -n genai-demo
   kubectl logs -n kube-system -l app.kubernetes.io/name=aws-load-balancer-controller
   ```

4. **HPA 未擴展**

   ```bash
   kubectl describe hpa genai-demo-hpa -n genai-demo
   kubectl top pods -n genai-demo
   ```

### 有用的指令

```bash
# 檢查應用程式狀態
kubectl get all -n genai-demo

# 檢視應用程式日誌
kubectl logs -f deployment/genai-demo-deployment -n genai-demo

# 本地存取的 port forward
kubectl port-forward service/genai-demo-service 8080:80 -n genai-demo

# 執行到 pod 中
kubectl exec -it deployment/genai-demo-deployment -n genai-demo -- /bin/sh

# 檢查資源使用情況
kubectl top pods -n genai-demo
kubectl top nodes
```

## 與 CI/CD 整合

Manifests 設計為與 GitOps 工作流程配合使用：

1. **映像更新**：Kustomize 處理映像標籤更新
2. **配置變更**：環境特定的 patches
3. **回滾**：Kubernetes 原生回滾能力
4. **Blue-Green 部署**：可以使用額外的 services 實作

## 未來增強

- [ ] 用於微分段的網路政策
- [ ] 用於可用性的 Pod Disruption Budgets
- [ ] Vertical Pod Autoscaler 整合
- [ ] Service mesh 整合（Istio/App Mesh）
- [ ] 使用 ArgoCD 的 GitOps
- [ ] 使用 Chaos Mesh 的混沌工程
