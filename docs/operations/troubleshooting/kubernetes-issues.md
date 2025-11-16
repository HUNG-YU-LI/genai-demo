# Kubernetes 疑難排解指南

## 概述

本文件提供 Enterprise E-Commerce Platform 中 Kubernetes 特定問題的全面疑難排解程序。涵蓋 pod scheduling、persistent volumes、ConfigMaps、Secrets、service discovery、autoscaling、node 問題和 etcd 健康狀態。

**目標讀者**：DevOps 工程師、SRE、Kubernetes 管理員
**先決條件**：Kubernetes cluster 存取權限、kubectl、cluster admin 權限
**相關文件**：

- [常見問題](common-issues.md)
- [Application Debugging 指南](application-debugging.md)
- [Network 和 Connectivity 指南](network-connectivity.md)

---

## 目錄

1. [Pod Scheduling 失敗](#pod-scheduling-失敗)
2. [Persistent Volume Claim 問題](#persistent-volume-claim-問題)
3. [ConfigMap 和 Secret 掛載問題](#configmap-和-secret-掛載問題)
4. [Service Discovery 失敗](#service-discovery-失敗)
5. [Horizontal Pod Autoscaler 疑難排解](#horizontal-pod-autoscaler-疑難排解)
6. [Cluster Autoscaler 問題](#cluster-autoscaler-問題)
7. [Node NotReady 疑難排解](#node-notready-疑難排解)
8. [etcd Performance 和 Health 問題](#etcd-performance-和-health-問題)

---

## Pod Scheduling 失敗

### 概述

Pod scheduling 失敗發生在 Kubernetes scheduler 無法找到合適的 node 來執行 pod 時。這可能是由於資源限制、node affinity 規則、taints/tolerations 或其他 scheduling 策略。

### 症狀

- Pod 卡在 `Pending` 狀態
- Events 顯示 "FailedScheduling"
- "Insufficient cpu/memory" 訊息
- "No nodes available" 錯誤
- Deployment 後 Pod 無法啟動

### 診斷程序

#### 步驟 1：檢查 Pod Status 和 Events

```bash
# 取得 pod status
kubectl get pods -n production -l app=ecommerce-backend

# Describe pod 以查看 events
kubectl describe pod ${POD_NAME} -n production

# 取得 scheduling events
kubectl get events -n production --field-selector involvedObject.name=${POD_NAME} --sort-by='.lastTimestamp'

# 篩選 scheduling 失敗
kubectl get events -n production --field-selector reason=FailedScheduling
```

**常見 Event 訊息**：

- `0/3 nodes are available: 3 Insufficient cpu.`
- `0/3 nodes are available: 3 node(s) didn't match node selector.`
- `0/3 nodes are available: 3 node(s) had taint {key: value}, that the pod didn't tolerate.`
- `0/3 nodes are available: 3 node(s) didn't match pod affinity rules.`

#### 步驟 2：檢查 Resource Requests 和 Limits

```bash
# 檢查 pod resource requests
kubectl get pod ${POD_NAME} -n production -o jsonpath='{.spec.containers[*].resources}'

# 檢查 node 可用資源
kubectl describe nodes | grep -A 5 "Allocated resources"

# 取得 node capacity 和 allocatable 資源
kubectl get nodes -o custom-columns=NAME:.metadata.name,CPU-CAPACITY:.status.capacity.cpu,CPU-ALLOCATABLE:.status.allocatable.cpu,MEMORY-CAPACITY:.status.capacity.memory,MEMORY-ALLOCATABLE:.status.allocatable.memory
```

#### 步驟 3：檢查 Node Affinity 和 Selectors

```bash
# 檢查 pod node selector
kubectl get pod ${POD_NAME} -n production -o jsonpath='{.spec.nodeSelector}'

# 檢查 pod affinity 規則
kubectl get pod ${POD_NAME} -n production -o jsonpath='{.spec.affinity}'

# 列出帶有 labels 的 nodes
kubectl get nodes --show-labels

# 檢查 nodes 是否符合 selector
kubectl get nodes -l environment=production
```

#### 步驟 4：檢查 Taints 和 Tolerations

```bash
# 檢查 node taints
kubectl describe nodes | grep -A 3 "Taints:"

# 檢查 pod tolerations
kubectl get pod ${POD_NAME} -n production -o jsonpath='{.spec.tolerations}'

# 列出所有 node taints
kubectl get nodes -o custom-columns=NAME:.metadata.name,TAINTS:.spec.taints
```

#### 步驟 5：檢查 Pod Priority 和 Preemption

```bash
# 檢查 pod priority class
kubectl get pod ${POD_NAME} -n production -o jsonpath='{.spec.priorityClassName}'

# 列出 priority classes
kubectl get priorityclasses

# 檢查 pod 是否被 preempt
kubectl get events -n production --field-selector reason=Preempted
```

### 常見 Scheduling 問題和解決方案

#### 問題 1：CPU/Memory 不足

**問題**：Nodes 沒有足夠資源來 schedule pod

**診斷**：

```bash
# 檢查 cluster 總資源
kubectl top nodes

# 檢查 pod resource requests
kubectl describe pod ${POD_NAME} -n production | grep -A 5 "Requests:"

# 計算總 requested 資源
kubectl describe nodes | grep -A 5 "Allocated resources:" | grep -E "cpu|memory"
```

**解決方案**：

**選項 1：減少 Resource Requests**

```yaml
# deployment.yaml
resources:
  requests:
    cpu: 500m      # 從 1000m 減少
    memory: 512Mi  # 從 1Gi 減少
  limits:
    cpu: 1000m
    memory: 1Gi
```

**選項 2：新增更多 Nodes**

```bash
# 對於 EKS 與 cluster autoscaler
# 在 node group 配置中增加 max size
aws eks update-nodegroup-config \
  --cluster-name ecommerce-cluster \
  --nodegroup-name ecommerce-nodes \
  --scaling-config minSize=3,maxSize=10,desiredSize=5

# 手動 scaling
kubectl scale deployment cluster-autoscaler \
  --replicas=1 -n kube-system
```

**選項 3：使用 Cluster Autoscaler**

```yaml
# 啟用 cluster autoscaler
apiVersion: apps/v1
kind: Deployment
metadata:
  name: cluster-autoscaler
  namespace: kube-system
spec:
  template:
    spec:
      containers:
      - name: cluster-autoscaler
        image: k8s.gcr.io/autoscaling/cluster-autoscaler:v1.27.0
        command:
          - ./cluster-autoscaler
          - --cloud-provider=aws
          - --namespace=kube-system
          - --node-group-auto-discovery=asg:tag=k8s.io/cluster-autoscaler/enabled,k8s.io/cluster-autoscaler/ecommerce-cluster
```

#### 問題 2：Node Selector 不匹配

**問題**：Pod 需要不存在的特定 node labels

**診斷**：

```bash
# 檢查 pod node selector
kubectl get pod ${POD_NAME} -n production -o yaml | grep -A 5 nodeSelector

# 檢查可用的 node labels
kubectl get nodes --show-labels | grep -i "environment\|workload"
```

**解決方案**：

**選項 1：新增 Labels 到 Nodes**

```bash
# 新增缺少的 label 到 node
kubectl label nodes ${NODE_NAME} environment=production

# 新增多個 labels
kubectl label nodes ${NODE_NAME} workload-type=compute-intensive tier=backend
```

**選項 2：移除或修改 Node Selector**

```yaml
# deployment.yaml - 移除 node selector
spec:
  template:
    spec:
      # nodeSelector:  # 註解掉
      #   environment: production
      containers:
      - name: app
```

**選項 3：使用 Node Affinity（更靈活）**

```yaml
spec:
  template:
    spec:
      affinity:
        nodeAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:  # Soft requirement
          - weight: 100
            preference:
              matchExpressions:
              - key: environment
                operator: In
                values:
                - production
```

#### 問題 3：Taint/Toleration 不匹配

**問題**：Nodes 有 pod 不容忍的 taints

**診斷**：

```bash
# 檢查 node taints
kubectl describe node ${NODE_NAME} | grep -A 3 "Taints:"

# 檢查 pod tolerations
kubectl get pod ${POD_NAME} -n production -o yaml | grep -A 10 tolerations
```

**解決方案**：

**選項 1：新增 Toleration 到 Pod**

```yaml
# deployment.yaml
spec:
  template:
    spec:
      tolerations:
      - key: "dedicated"
        operator: "Equal"
        value: "backend"
        effect: "NoSchedule"
      - key: "node.kubernetes.io/not-ready"
        operator: "Exists"
        effect: "NoExecute"
        tolerationSeconds: 300
```

**選項 2：從 Node 移除 Taint**

```bash
# 移除特定 taint
kubectl taint nodes ${NODE_NAME} dedicated=backend:NoSchedule-

# 移除所有 taints
kubectl taint nodes ${NODE_NAME} dedicated-
```

**選項 3：使用 Taint-Based Eviction**

```yaml
# 用於 maintenance 期間的暫時性 taints
tolerations:
- key: "node.kubernetes.io/unreachable"
  operator: "Exists"
  effect: "NoExecute"
  tolerationSeconds: 30
```

#### 問題 4：Pod Affinity/Anti-Affinity 規則

**問題**：Pod affinity 規則阻止 scheduling

**診斷**：

```bash
# 檢查 pod affinity 規則
kubectl get pod ${POD_NAME} -n production -o yaml | grep -A 20 affinity

# 檢查現有 pod 分佈
kubectl get pods -n production -o wide --show-labels
```

**解決方案**：

**選項 1：使用 Preferred 而非 Required**

```yaml
# 從 required 改為 preferred
spec:
  template:
    spec:
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:  # Soft rule
          - weight: 100
            podAffinityTerm:
              labelSelector:
                matchExpressions:
                - key: app
                  operator: In
                  values:
                  - ecommerce-backend
              topologyKey: kubernetes.io/hostname
```

**選項 2：調整 Topology Key**

```yaml
# 使用 zone 而非 hostname 以獲得更多靈活性
affinity:
  podAntiAffinity:
    requiredDuringSchedulingIgnoredDuringExecution:
    - labelSelector:
        matchExpressions:
        - key: app
          operator: In
          values:
          - ecommerce-backend
      topologyKey: topology.kubernetes.io/zone  # 更靈活
```

**選項 3：增加 Replica Count**

```bash
# 如果 anti-affinity 需要比可用更多的 nodes
kubectl scale deployment ecommerce-backend --replicas=3 -n production
```

### 預防和監控

**設定 Scheduling Alerts**：

```yaml
# Prometheus alert rule
- alert: PodsPendingTooLong
  expr: kube_pod_status_phase{phase="Pending"} > 0
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "Pods pending for more than 5 minutes"
    description: "Pod {{ $labels.pod }} in namespace {{ $labels.namespace }} has been pending for more than 5 minutes"

- alert: FailedScheduling
  expr: increase(kube_pod_failed_scheduling_total[5m]) > 0
  labels:
    severity: critical
  annotations:
    summary: "Pod scheduling failures detected"
```

**Resource Quota 監控**：

```bash
# 檢查 namespace resource quotas
kubectl get resourcequota -n production

# 檢查 limit ranges
kubectl get limitrange -n production

# 監控資源使用情況
kubectl top pods -n production --sort-by=memory
```

---

## Persistent Volume Claim 問題

### 概述

PersistentVolumeClaim (PVC) 問題阻止 pods 存取 persistent storage，導致 application 失敗或資料遺失。

### 症狀

- Pods 卡在 `ContainerCreating` 或 `Pending` 狀態
- Events 顯示 "FailedMount" 或 "FailedAttachVolume"
- "Volume not found" 錯誤
- Volumes 的 "Multi-Attach error"
- Pod 啟動時間緩慢

### 診斷程序

#### 步驟 1：檢查 PVC Status

```bash
# 列出 PVCs
kubectl get pvc -n production

# Describe PVC
kubectl describe pvc ${PVC_NAME} -n production

# 檢查 PVC events
kubectl get events -n production --field-selector involvedObject.name=${PVC_NAME}

# 檢查綁定的 PV
kubectl get pv | grep ${PVC_NAME}
```

**PVC Status 狀態**：

- `Pending`：等待 PV 被創建或綁定
- `Bound`：成功綁定到 PV
- `Lost`：PV 不再存在

#### 步驟 2：檢查 Storage Class

```bash
# 列出 storage classes
kubectl get storageclass

# Describe storage class
kubectl describe storageclass ${STORAGE_CLASS_NAME}

# 檢查預設 storage class
kubectl get storageclass -o jsonpath='{.items[?(@.metadata.annotations.storageclass\.kubernetes\.io/is-default-class=="true")].metadata.name}'

# 檢查 provisioner
kubectl get storageclass ${STORAGE_CLASS_NAME} -o jsonpath='{.provisioner}'
```

#### 步驟 3：檢查 Volume Attachment

```bash
# 檢查 volume attachments
kubectl get volumeattachment

# Describe volume attachment
kubectl describe volumeattachment ${ATTACHMENT_NAME}

# 檢查 CSI driver
kubectl get csidrivers

# 檢查 CSI nodes
kubectl get csinodes
```

#### 步驟 4：檢查 Pod Volume Mounts

```bash
# 檢查 pod volume mounts
kubectl get pod ${POD_NAME} -n production -o jsonpath='{.spec.volumes}'

# 檢查 volume mount status
kubectl describe pod ${POD_NAME} -n production | grep -A 10 "Volumes:"

# 檢查 mount errors
kubectl get events -n production --field-selector involvedObject.name=${POD_NAME},reason=FailedMount
```

### 常見 PVC 問題和解決方案

#### 問題 1：PVC 卡在 Pending

**問題**：PVC 無法找到或創建合適的 PV

**診斷**：

```bash
# 檢查 PVC status
kubectl describe pvc ${PVC_NAME} -n production

# 檢查可用的 PVs
kubectl get pv

# 檢查 storage class
kubectl get storageclass
```

**常見原因和解決方案**：

**原因 1：沒有 Storage Class**

```yaml
# 新增 storage class 到 PVC
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: data-pvc
spec:
  accessModes:
    - ReadWriteOnce
  storageClassName: gp3  # 新增此項
  resources:
    requests:
      storage: 10Gi
```

**原因 2：沒有匹配的 PV**

```bash
# 手動創建 PV（如果不使用 dynamic provisioning）
kubectl apply -f - <<EOF
apiVersion: v1
kind: PersistentVolume
metadata:
  name: pv-manual
spec:
  capacity:
    storage: 10Gi
  accessModes:
    - ReadWriteOnce
  persistentVolumeReclaimPolicy: Retain
  storageClassName: manual
  hostPath:
    path: /mnt/data
EOF
```

**原因 3：Storage 不足**

```bash
# 檢查可用 storage
kubectl get pv -o custom-columns=NAME:.metadata.name,CAPACITY:.spec.capacity.storage,STATUS:.status.phase

# 增加 PVC 大小（如果 storage class 支援 expansion）
kubectl patch pvc ${PVC_NAME} -n production -p '{"spec":{"resources":{"requests":{"storage":"20Gi"}}}}'
```

#### 問題 2：Multi-Attach Error

**問題**：Volume 無法 attach 到多個 nodes（ReadWriteOnce）

**診斷**：

```bash
# 檢查 volume attachment
kubectl get volumeattachment | grep ${PV_NAME}

# 檢查哪個 node 有 volume
kubectl get volumeattachment -o jsonpath='{.items[?(@.spec.source.persistentVolumeName=="'${PV_NAME}'")].spec.nodeName}'

# 檢查 pod node placement
kubectl get pod ${POD_NAME} -n production -o jsonpath='{.spec.nodeName}'
```

**解決方案**：

**選項 1：使用 ReadWriteMany（如果支援）**

```yaml
# 更改 access mode
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: shared-data
spec:
  accessModes:
    - ReadWriteMany  # 從 ReadWriteOnce 更改
  storageClassName: efs  # 使用 EFS 以支援 RWX
  resources:
    requests:
      storage: 10Gi
```

**選項 2：強制 Detach 和 Reattach**

```bash
# 刪除 pod 以強制 detach
kubectl delete pod ${POD_NAME} -n production --grace-period=0 --force

# 等待 volume detach
kubectl get volumeattachment --watch

# Pod 將被 deployment 重新創建
```

**選項 3：使用 Pod Affinity 到相同 Node**

```yaml
# 確保使用相同 volume 的 pods 在同一個 node 上執行
spec:
  template:
    spec:
      affinity:
        podAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
          - labelSelector:
              matchExpressions:
              - key: app
                operator: In
                values:
                - ecommerce-backend
            topologyKey: kubernetes.io/hostname
```

#### 問題 3：Volume Mount 失敗

**問題**：Volume 無法 mount 到 pod

**診斷**：

```bash
# 檢查 mount errors
kubectl describe pod ${POD_NAME} -n production | grep -A 20 "Events:"

# 檢查 volume plugin
kubectl get pod ${POD_NAME} -n production -o jsonpath='{.spec.volumes[*].persistentVolumeClaim}'

# 檢查 CSI driver logs
kubectl logs -n kube-system -l app=ebs-csi-controller
```

**解決方案**：

**選項 1：修復權限**

```yaml
# 新增 security context
spec:
  template:
    spec:
      securityContext:
        fsGroup: 1000
        runAsUser: 1000
      containers:
      - name: app
        volumeMounts:
        - name: data
          mountPath: /data
```

**選項 2：重新創建 PVC**

```bash
# 如需要備份資料
kubectl exec ${POD_NAME} -n production -- tar czf /tmp/backup.tar.gz /data

# 刪除並重新創建 PVC
kubectl delete pvc ${PVC_NAME} -n production
kubectl apply -f pvc.yaml

# 還原資料
kubectl exec ${POD_NAME} -n production -- tar xzf /tmp/backup.tar.gz -C /
```

**選項 3：檢查 Node Kubelet**

```bash
# 檢查 node 上的 kubelet logs
ssh ${NODE_IP}
journalctl -u kubelet -f | grep -i volume

# 如需要重啟 kubelet
systemctl restart kubelet
```

### Volume Expansion

**啟用 Volume Expansion**：

```yaml
# 啟用 expansion 的 Storage class
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: gp3-expandable
provisioner: ebs.csi.aws.com
allowVolumeExpansion: true  # 啟用 expansion
parameters:
  type: gp3
  iops: "3000"
  throughput: "125"
```

**擴充 PVC**：

```bash
# 編輯 PVC 以增加大小
kubectl patch pvc ${PVC_NAME} -n production -p '{"spec":{"resources":{"requests":{"storage":"20Gi"}}}}'

# 檢查 expansion status
kubectl describe pvc ${PVC_NAME} -n production | grep -A 5 "Conditions:"

# 對於 file system expansion，重啟 pod
kubectl rollout restart deployment/${DEPLOYMENT_NAME} -n production
```

---

## ConfigMap 和 Secret 掛載問題

### 概述

ConfigMaps 和 Secrets 為 pods 提供配置和敏感資料。掛載問題可能導致 application 失敗或安全漏洞。

### 症狀

- Pods 卡在 `ContainerCreating` 狀態
- "ConfigMap not found" 或 "Secret not found" 錯誤
- Application 配置未載入
- 環境變數缺失
- 檔案掛載為空或不正確

### 診斷程序

#### 步驟 1：檢查 ConfigMap/Secret 存在性

```bash
# 列出 ConfigMaps
kubectl get configmap -n production

# Describe ConfigMap
kubectl describe configmap ${CONFIGMAP_NAME} -n production

# 查看 ConfigMap 資料
kubectl get configmap ${CONFIGMAP_NAME} -n production -o yaml

# 列出 Secrets
kubectl get secret -n production

# Describe Secret（資料為 base64 編碼）
kubectl describe secret ${SECRET_NAME} -n production

# 解碼 Secret 資料
kubectl get secret ${SECRET_NAME} -n production -o jsonpath='{.data.password}' | base64 -d
```

#### 步驟 2：檢查 Pod 配置

```bash
# 檢查 pod 中的 ConfigMap 引用
kubectl get pod ${POD_NAME} -n production -o yaml | grep -A 10 configMapRef

# 檢查 Secret 引用
kubectl get pod ${POD_NAME} -n production -o yaml | grep -A 10 secretRef

# 檢查 volume mounts
kubectl get pod ${POD_NAME} -n production -o yaml | grep -A 20 volumeMounts

# 檢查環境變數
kubectl exec ${POD_NAME} -n production -- env | grep -i config
```

#### 步驟 3：檢查 Mount Status

```bash
# 檢查檔案是否已掛載
kubectl exec ${POD_NAME} -n production -- ls -la /etc/config

# 檢查檔案內容
kubectl exec ${POD_NAME} -n production -- cat /etc/config/application.yml

# 檢查環境變數
kubectl exec ${POD_NAME} -n production -- printenv | sort
```

### 常見 ConfigMap/Secret 問題和解決方案

#### 問題 1：ConfigMap/Secret 未找到

**問題**：引用的 ConfigMap 或 Secret 不存在

**診斷**：

```bash
# 檢查 ConfigMap 是否存在
kubectl get configmap ${CONFIGMAP_NAME} -n production

# 檢查 pod events
kubectl describe pod ${POD_NAME} -n production | grep -i "configmap\|secret"
```

**解決方案**：

**選項 1：創建缺少的 ConfigMap**

```bash
# 從 literal values 創建
kubectl create configmap app-config -n production \
  --from-literal=database.host=postgres.production.svc.cluster.local \
  --from-literal=database.port=5432

# 從檔案創建
kubectl create configmap app-config -n production \
  --from-file=application.yml

# 從目錄創建
kubectl create configmap app-config -n production \
  --from-file=config/
```

**選項 2：創建缺少的 Secret**

```bash
# 從 literal values 創建
kubectl create secret generic db-credentials -n production \
  --from-literal=username=admin \
  --from-literal=password=secretpassword

# 從檔案創建
kubectl create secret generic tls-cert -n production \
  --from-file=tls.crt=cert.pem \
  --from-file=tls.key=key.pem

# 創建 docker registry secret
kubectl create secret docker-registry ecr-secret -n production \
  --docker-server=${ECR_REGISTRY} \
  --docker-username=AWS \
  --docker-password=$(aws ecr get-login-password)
```

**選項 3：修復 Deployment 中的引用**

```yaml
# 修正 ConfigMap 名稱
spec:
  template:
    spec:
      containers:
      - name: app
        envFrom:
        - configMapRef:
            name: app-config  # 確保這與實際 ConfigMap 名稱匹配
```

#### 問題 2：ConfigMap/Secret 未更新

**問題**：ConfigMap/Secret 的變更未反映在運行中的 pods

**診斷**：

```bash
# 檢查 ConfigMap 版本
kubectl get configmap ${CONFIGMAP_NAME} -n production -o yaml | grep resourceVersion

# 檢查 pod 啟動時間
kubectl get pod ${POD_NAME} -n production -o jsonpath='{.status.startTime}'

# 檢查是否使用 subPath（阻止更新）
kubectl get pod ${POD_NAME} -n production -o yaml | grep subPath
```

**解決方案**：

**選項 1：重啟 Pods**

```bash
# Rolling restart
kubectl rollout restart deployment/${DEPLOYMENT_NAME} -n production

# 強制刪除 pods
kubectl delete pod -l app=ecommerce-backend -n production

# Scale down 和 up
kubectl scale deployment/${DEPLOYMENT_NAME} --replicas=0 -n production
kubectl scale deployment/${DEPLOYMENT_NAME} --replicas=3 -n production
```

**選項 2：使用 Reloader（自動重啟）**

```bash
# 安裝 Reloader
kubectl apply -f https://raw.githubusercontent.com/stakater/Reloader/master/deployments/kubernetes/reloader.yaml

# 新增 annotation 到 deployment
kubectl annotate deployment ${DEPLOYMENT_NAME} -n production \
  reloader.stakater.com/auto="true"
```

**選項 3：避免 subPath**

```yaml
# 不使用 subPath，掛載整個 ConfigMap
volumeMounts:
- name: config
  mountPath: /etc/config
  # 不使用 subPath - 它會阻止更新
```

**選項 4：使用 Immutable ConfigMaps**

```yaml
# 創建帶有版本的新 ConfigMap
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config-v2  # 版本化名稱
immutable: true
data:
  application.yml: |
    ...

# 更新 deployment 使用新版本
spec:
  template:
    spec:
      volumes:
      - name: config
        configMap:
          name: app-config-v2  # 更新引用
```

#### 問題 3：掛載檔案權限被拒絕

**問題**：Application 無法讀取掛載的 ConfigMap/Secret 檔案

**診斷**：

```bash
# 檢查檔案權限
kubectl exec ${POD_NAME} -n production -- ls -la /etc/config

# 檢查 pod security context
kubectl get pod ${POD_NAME} -n production -o jsonpath='{.spec.securityContext}'

# 檢查 container user
kubectl exec ${POD_NAME} -n production -- id
```

**解決方案**：

**選項 1：設定 Default Mode**

```yaml
# 設定檔案權限
volumes:
- name: config
  configMap:
    name: app-config
    defaultMode: 0644  # rw-r--r--
- name: secret
  secret:
    secretName: db-credentials
    defaultMode: 0400  # r--------
```

**選項 2：設定 Security Context**

```yaml
# 以特定 user 執行
spec:
  template:
    spec:
      securityContext:
        runAsUser: 1000
        runAsGroup: 1000
        fsGroup: 1000
      containers:
      - name: app
```

**選項 3：使用 Init Container 修復權限**

```yaml
# 複製並修復權限
initContainers:
- name: fix-permissions
  image: busybox
  command: ['sh', '-c', 'cp /tmp/config/* /etc/config/ && chmod 644 /etc/config/*']
  volumeMounts:
  - name: config-source
    mountPath: /tmp/config
  - name: config-writable
    mountPath: /etc/config
```

### 最佳實踐

**ConfigMap 管理**：

```yaml
# 使用 labels 進行版本控制
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
  labels:
    app: ecommerce-backend
    version: "1.2.0"
    environment: production
data:
  application.yml: |
    ...
```

**Secret 管理**：

```bash
# 使用外部 secret 管理
# 安裝 External Secrets Operator
kubectl apply -f https://raw.githubusercontent.com/external-secrets/external-secrets/main/deploy/crds/bundle.yaml

# 創建 SecretStore
kubectl apply -f - <<EOF
apiVersion: external-secrets.io/v1beta1
kind: SecretStore
metadata:
  name: aws-secrets-manager
  namespace: production
spec:
  provider:
    aws:
      service: SecretsManager
      region: us-east-1
EOF

# 創建 ExternalSecret
kubectl apply -f - <<EOF
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: db-credentials
  namespace: production
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: aws-secrets-manager
    kind: SecretStore
  target:
    name: db-credentials
  data:
  - secretKey: password
    remoteRef:
      key: prod/database/password
EOF
```

---

## Service Discovery 失敗

### 概述

Service discovery 問題阻止 pods 透過 Kubernetes Services 相互通訊，導致 application 失敗和網路連線問題。

### 症狀

- Services 之間「Connection refused」錯誤
- DNS 解析失敗
- "Service not found" 錯誤
- 間歇性連線問題
- Load balancing 無法運作

### 診斷程序

#### 步驟 1：檢查 Service 配置

```bash
# 列出 services
kubectl get svc -n production

# Describe service
kubectl describe svc ${SERVICE_NAME} -n production

# 檢查 service endpoints
kubectl get endpoints ${SERVICE_NAME} -n production

# 檢查 service selector
kubectl get svc ${SERVICE_NAME} -n production -o jsonpath='{.spec.selector}'
```

#### 步驟 2：驗證 Pod Labels 匹配 Service Selector

```bash
# 檢查 pod labels
kubectl get pods -n production --show-labels

# 檢查 pods 是否匹配 service selector
kubectl get pods -n production -l app=ecommerce-backend

# 與 service selector 比較
kubectl get svc ecommerce-backend -n production -o jsonpath='{.spec.selector}'
```

#### 步驟 3：測試 DNS 解析

```bash
# 從 cluster 內測試 DNS
kubectl run -it --rm debug --image=busybox --restart=Never -- nslookup ${SERVICE_NAME}.production.svc.cluster.local

# 測試 service 連線
kubectl run -it --rm debug --image=curlimages/curl --restart=Never -- curl http://${SERVICE_NAME}.production.svc.cluster.local:8080/actuator/health

# 檢查 CoreDNS
kubectl get pods -n kube-system -l k8s-app=kube-dns
kubectl logs -n kube-system -l k8s-app=kube-dns
```

### 常見 Service Discovery 問題

#### 問題 1：沒有 Endpoints

**問題**：Service 沒有 endpoints（沒有 pods 匹配 selector）

**解決方案**：

```bash
# 修復 pod labels 以匹配 service selector
kubectl label pods -l app=backend app=ecommerce-backend -n production --overwrite

# 或更新 service selector
kubectl patch svc ${SERVICE_NAME} -n production -p '{"spec":{"selector":{"app":"backend"}}}'
```

#### 問題 2：DNS 解析失敗

**問題**：無法解析 service 名稱

**解決方案**：

```bash
# 重啟 CoreDNS
kubectl rollout restart deployment/coredns -n kube-system

# 檢查 CoreDNS ConfigMap
kubectl get configmap coredns -n kube-system -o yaml

# 測試 DNS
kubectl exec ${POD_NAME} -n production -- nslookup kubernetes.default
```

---

## Horizontal Pod Autoscaler 疑難排解

### 概述

HPA 根據 metrics 自動 scale pods。問題可能導致 under 或 over-provisioning。

### 症狀

- 儘管負載高但 Pods 未 scaling
- 過度 scaling（flapping）
- "unable to get metrics" 錯誤
- HPA 顯示「unknown」狀態

### 診斷程序

```bash
# 檢查 HPA status
kubectl get hpa -n production

# Describe HPA
kubectl describe hpa ${HPA_NAME} -n production

# 檢查 metrics server
kubectl get deployment metrics-server -n kube-system
kubectl logs -n kube-system -l k8s-app=metrics-server

# 檢查當前 metrics
kubectl top pods -n production
kubectl top nodes

# 檢查 HPA events
kubectl get events -n production --field-selector involvedObject.name=${HPA_NAME}
```

### 常見 HPA 問題

#### 問題 1：Metrics Server 不可用

**解決方案**：

```bash
# 安裝 metrics server
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml

# 用於開發（insecure）
kubectl patch deployment metrics-server -n kube-system --type='json' -p='[{"op": "add", "path": "/spec/template/spec/containers/0/args/-", "value": "--kubelet-insecure-tls"}]'
```

#### 問題 2：Resource Requests 未設定

**解決方案**：

```yaml
# 新增 resource requests（HPA 所需）
spec:
  template:
    spec:
      containers:
      - name: app
        resources:
          requests:
            cpu: 500m
            memory: 512Mi
          limits:
            cpu: 1000m
            memory: 1Gi
```

#### 問題 3：HPA Flapping

**解決方案**：

```yaml
# 調整 HPA behavior
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: ecommerce-backend
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: ecommerce-backend
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300  # 在 scale down 前等待 5 分鐘
      policies:
      - type: Percent
        value: 50
        periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 0
      policies:
      - type: Percent
        value: 100
        periodSeconds: 15
```

---

## Cluster Autoscaler 問題

### 概述

Cluster Autoscaler 自動調整 nodes 數量。問題可能導致資源短缺或浪費。

### 症狀

- 儘管啟用 autoscaler，pods 仍 pending
- Nodes 未 scale up
- 空 nodes 未 scale down
- "scale up failed" 錯誤

### 診斷程序

```bash
# 檢查 cluster autoscaler status
kubectl get deployment cluster-autoscaler -n kube-system

# 檢查 logs
kubectl logs -n kube-system -l app=cluster-autoscaler --tail=100

# 檢查 autoscaler ConfigMap
kubectl get configmap cluster-autoscaler-status -n kube-system -o yaml

# 檢查 node groups
kubectl get nodes -o custom-columns=NAME:.metadata.name,INSTANCE-TYPE:.metadata.labels.node\\.kubernetes\\.io/instance-type,ZONE:.metadata.labels.topology\\.kubernetes\\.io/zone
```

### 常見 Cluster Autoscaler 問題

#### 問題 1：Autoscaler 未 Scale Up

**診斷**：

```bash
# 檢查 autoscaler logs 以查看 scale-up 決策
kubectl logs -n kube-system -l app=cluster-autoscaler | grep -i "scale up"

# 檢查 node group 限制
aws autoscaling describe-auto-scaling-groups --auto-scaling-group-names ${ASG_NAME}
```

**解決方案**：

```bash
# 增加 max size
aws autoscaling update-auto-scaling-group \
  --auto-scaling-group-name ${ASG_NAME} \
  --max-size 20

# 檢查 autoscaler 配置
kubectl edit deployment cluster-autoscaler -n kube-system
# 確保 --max-nodes-total 足夠
```

#### 問題 2：Nodes 未 Scale Down

**診斷**：

```bash
# 檢查 nodes 為何未 scale down
kubectl logs -n kube-system -l app=cluster-autoscaler | grep -i "scale down"

# 檢查 node annotations
kubectl describe node ${NODE_NAME} | grep -i "scale-down"
```

**解決方案**：

```bash
# 移除 scale-down 預防 annotation
kubectl annotate node ${NODE_NAME} cluster-autoscaler.kubernetes.io/scale-down-disabled-

# 檢查阻止 scale-down 的 pods
kubectl get pods --all-namespaces -o wide --field-selector spec.nodeName=${NODE_NAME}

# 新增 PodDisruptionBudget 以允許 eviction
kubectl apply -f - <<EOF
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: ecommerce-backend-pdb
  namespace: production
spec:
  minAvailable: 2
  selector:
    matchLabels:
      app: ecommerce-backend
EOF
```

---

## Node NotReady 疑難排解

### 概述

NotReady 狀態的 Nodes 無法執行 pods，減少 cluster 容量並可能導致中斷。

### 症狀

- Nodes 顯示 NotReady 狀態
- Pods 從 nodes 被 evicted
- "node not found" 錯誤
- Kubelet 無回應

### 診斷程序

```bash
# 檢查 node status
kubectl get nodes

# Describe node
kubectl describe node ${NODE_NAME}

# 檢查 node conditions
kubectl get node ${NODE_NAME} -o jsonpath='{.status.conditions[*].type}{"\n"}{.status.conditions[*].status}'

# 檢查 kubelet status（SSH 到 node）
ssh ${NODE_IP}
systemctl status kubelet
journalctl -u kubelet -f

# 檢查 node 資源
kubectl top node ${NODE_NAME}
```

### 常見 Node 問題

#### 問題 1：Disk Pressure

**診斷**：

```bash
# 檢查 node 上的 disk 使用情況
ssh ${NODE_IP}
df -h
du -sh /var/lib/docker/* | sort -rh | head -10
du -sh /var/lib/kubelet/* | sort -rh | head -10
```

**解決方案**：

```bash
# 清理 Docker images
docker system prune -a -f

# 清理未使用的 volumes
docker volume prune -f

# 清理 kubelet
kubectl delete pod --field-selector=status.phase==Succeeded -A
kubectl delete pod --field-selector=status.phase==Failed -A

# 增加 disk 大小（AWS EBS）
aws ec2 modify-volume --volume-id ${VOLUME_ID} --size 100
```

#### 問題 2：Memory Pressure

**診斷**：

```bash
# 檢查 memory 使用情況
ssh ${NODE_IP}
free -h
top -o %MEM

# 檢查 OOM kills
dmesg | grep -i "out of memory"
```

**解決方案**：

```bash
# 新增更多 nodes
kubectl scale deployment cluster-autoscaler --replicas=1 -n kube-system

# 減少 pod resource requests
kubectl set resources deployment/${DEPLOYMENT_NAME} -n production --requests=memory=256Mi

# 從 node evict pods
kubectl drain ${NODE_NAME} --ignore-daemonsets --delete-emptydir-data
```

#### 問題 3：Kubelet 未執行

**診斷**：

```bash
# 檢查 kubelet status
ssh ${NODE_IP}
systemctl status kubelet
journalctl -u kubelet --no-pager | tail -100
```

**解決方案**：

```bash
# 重啟 kubelet
systemctl restart kubelet

# 檢查 kubelet 配置
cat /var/lib/kubelet/config.yaml

# 檢查 certificates
ls -la /var/lib/kubelet/pki/

# 如果 certificates 過期則重新生成
kubeadm alpha certs renew all
systemctl restart kubelet
```

---

## etcd Performance 和 Health 問題

### 概述

etcd 是 Kubernetes cluster 狀態的 key-value store。Performance 問題會影響整個 cluster。

### 症狀

- API server 回應緩慢
- "etcdserver: request timed out" 錯誤
- etcd 延遲高
- Cluster 狀態不一致
- Leader election 失敗

### 診斷程序

```bash
# 檢查 etcd pods
kubectl get pods -n kube-system -l component=etcd

# 檢查 etcd logs
kubectl logs -n kube-system etcd-${MASTER_NODE} --tail=100

# 檢查 etcd health
kubectl exec -n kube-system etcd-${MASTER_NODE} -- etcdctl \
  --endpoints=https://127.0.0.1:2379 \
  --cacert=/etc/kubernetes/pki/etcd/ca.crt \
  --cert=/etc/kubernetes/pki/etcd/server.crt \
  --key=/etc/kubernetes/pki/etcd/server.key \
  endpoint health

# 檢查 etcd metrics
kubectl exec -n kube-system etcd-${MASTER_NODE} -- etcdctl \
  --endpoints=https://127.0.0.1:2379 \
  --cacert=/etc/kubernetes/pki/etcd/ca.crt \
  --cert=/etc/kubernetes/pki/etcd/server.crt \
  --key=/etc/kubernetes/pki/etcd/server.key \
  endpoint status --write-out=table
```

### 常見 etcd 問題

#### 問題 1：高延遲

**診斷**：

```bash
# 檢查 etcd 延遲
kubectl exec -n kube-system etcd-${MASTER_NODE} -- etcdctl \
  --endpoints=https://127.0.0.1:2379 \
  --cacert=/etc/kubernetes/pki/etcd/ca.crt \
  --cert=/etc/kubernetes/pki/etcd/server.crt \
  --key=/etc/kubernetes/pki/etcd/server.key \
  check perf

# 檢查 disk I/O
ssh ${MASTER_IP}
iostat -x 1 10
```

**解決方案**：

```bash
# 使用更快的 disks（SSD/NVMe）
# 對於 AWS，使用 io2 或 gp3 volumes

# Defragment etcd
kubectl exec -n kube-system etcd-${MASTER_NODE} -- etcdctl \
  --endpoints=https://127.0.0.1:2379 \
  --cacert=/etc/kubernetes/pki/etcd/ca.crt \
  --cert=/etc/kubernetes/pki/etcd/server.crt \
  --key=/etc/kubernetes/pki/etcd/server.key \
  defrag

# Compact etcd history
kubectl exec -n kube-system etcd-${MASTER_NODE} -- etcdctl \
  --endpoints=https://127.0.0.1:2379 \
  --cacert=/etc/kubernetes/pki/etcd/ca.crt \
  --cert=/etc/kubernetes/pki/etcd/server.crt \
  --key=/etc/kubernetes/pki/etcd/server.key \
  compact $(kubectl exec -n kube-system etcd-${MASTER_NODE} -- etcdctl \
    --endpoints=https://127.0.0.1:2379 \
    --cacert=/etc/kubernetes/pki/etcd/ca.crt \
    --cert=/etc/kubernetes/pki/etcd/server.crt \
    --key=/etc/kubernetes/pki/etcd/server.key \
    endpoint status --write-out="json" | jq -r '.[0].Status.header.revision')
```

#### 問題 2：Database 大小過大

**診斷**：

```bash
# 檢查 etcd database 大小
kubectl exec -n kube-system etcd-${MASTER_NODE} -- etcdctl \
  --endpoints=https://127.0.0.1:2379 \
  --cacert=/etc/kubernetes/pki/etcd/ca.crt \
  --cert=/etc/kubernetes/pki/etcd/server.crt \
  --key=/etc/kubernetes/pki/etcd/server.key \
  endpoint status --write-out=table

# 檢查大型 keys
kubectl exec -n kube-system etcd-${MASTER_NODE} -- etcdctl \
  --endpoints=https://127.0.0.1:2379 \
  --cacert=/etc/kubernetes/pki/etcd/ca.crt \
  --cert=/etc/kubernetes/pki/etcd/server.crt \
  --key=/etc/kubernetes/pki/etcd/server.key \
  get --prefix --keys-only / | head -100
```

**解決方案**：

```bash
# 啟用自動 compaction
kubectl edit pod etcd-${MASTER_NODE} -n kube-system
# 新增：--auto-compaction-retention=1

# 清理舊 events
kubectl delete events --all -A

# 清理已完成的 pods
kubectl delete pod --field-selector=status.phase==Succeeded -A
kubectl delete pod --field-selector=status.phase==Failed -A

# Backup 和 restore 以減少大小
ETCDCTL_API=3 etcdctl snapshot save /tmp/etcd-backup.db
ETCDCTL_API=3 etcdctl snapshot restore /tmp/etcd-backup.db --data-dir=/var/lib/etcd-new
```

#### 問題 3：Split Brain / Quorum Loss

**診斷**：

```bash
# 檢查 member list
kubectl exec -n kube-system etcd-${MASTER_NODE} -- etcdctl \
  --endpoints=https://127.0.0.1:2379 \
  --cacert=/etc/kubernetes/pki/etcd/ca.crt \
  --cert=/etc/kubernetes/pki/etcd/server.crt \
  --key=/etc/kubernetes/pki/etcd/server.key \
  member list

# 檢查 cluster health
kubectl exec -n kube-system etcd-${MASTER_NODE} -- etcdctl \
  --endpoints=https://127.0.0.1:2379 \
  --cacert=/etc/kubernetes/pki/etcd/ca.crt \
  --cert=/etc/kubernetes/pki/etcd/server.crt \
  --key=/etc/kubernetes/pki/etcd/server.key \
  endpoint health --cluster
```

**解決方案**：

```bash
# 移除不健康的 member
kubectl exec -n kube-system etcd-${MASTER_NODE} -- etcdctl \
  --endpoints=https://127.0.0.1:2379 \
  --cacert=/etc/kubernetes/pki/etcd/ca.crt \
  --cert=/etc/kubernetes/pki/etcd/server.crt \
  --key=/etc/kubernetes/pki/etcd/server.key \
  member remove ${MEMBER_ID}

# 新增新 member
kubectl exec -n kube-system etcd-${MASTER_NODE} -- etcdctl \
  --endpoints=https://127.0.0.1:2379 \
  --cacert=/etc/kubernetes/pki/etcd/ca.crt \
  --cert=/etc/kubernetes/pki/etcd/server.crt \
  --key=/etc/kubernetes/pki/etcd/server.key \
  member add ${NEW_MEMBER_NAME} --peer-urls=https://${NEW_MEMBER_IP}:2380
```

### etcd Backup 和 Restore

**Backup**：

```bash
# 創建 snapshot
kubectl exec -n kube-system etcd-${MASTER_NODE} -- etcdctl \
  --endpoints=https://127.0.0.1:2379 \
  --cacert=/etc/kubernetes/pki/etcd/ca.crt \
  --cert=/etc/kubernetes/pki/etcd/server.crt \
  --key=/etc/kubernetes/pki/etcd/server.key \
  snapshot save /tmp/etcd-backup-$(date +%Y%m%d-%H%M%S).db

# 複製 backup
kubectl cp kube-system/etcd-${MASTER_NODE}:/tmp/etcd-backup-*.db ./etcd-backup.db
```

**Restore**：

```bash
# 停止 API server
systemctl stop kube-apiserver

# Restore snapshot
ETCDCTL_API=3 etcdctl snapshot restore etcd-backup.db \
  --data-dir=/var/lib/etcd-restored \
  --name=${MEMBER_NAME} \
  --initial-cluster=${INITIAL_CLUSTER} \
  --initial-advertise-peer-urls=https://${MEMBER_IP}:2380

# 更新 etcd data directory
mv /var/lib/etcd /var/lib/etcd-old
mv /var/lib/etcd-restored /var/lib/etcd

# 啟動 API server
systemctl start kube-apiserver
```

---

## 監控和 Alerts

### Kubernetes 特定 Alerts

```yaml
# Prometheus alert rules
groups:
- name: kubernetes-alerts
  rules:
  - alert: PodsPendingTooLong
    expr: kube_pod_status_phase{phase="Pending"} > 0
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "Pods pending for more than 5 minutes"

  - alert: NodeNotReady
    expr: kube_node_status_condition{condition="Ready",status="true"} == 0
    for: 5m
    labels:
      severity: critical
    annotations:
      summary: "Node {{ $labels.node }} is not ready"

  - alert: PVCPending
    expr: kube_persistentvolumeclaim_status_phase{phase="Pending"} > 0
    for: 10m
    labels:
      severity: warning
    annotations:
      summary: "PVC {{ $labels.persistentvolumeclaim }} pending for 10+ minutes"

  - alert: HPAMaxedOut
    expr: kube_horizontalpodautoscaler_status_current_replicas >= kube_horizontalpodautoscaler_spec_max_replicas
    for: 15m
    labels:
      severity: warning
    annotations:
      summary: "HPA {{ $labels.horizontalpodautoscaler }} at max replicas"

  - alert: etcdHighLatency
    expr: histogram_quantile(0.99, rate(etcd_disk_wal_fsync_duration_seconds_bucket[5m])) > 0.5
    for: 10m
    labels:
      severity: critical
    annotations:
      summary: "etcd high latency detected"
```

---

## 相關文件

- [常見問題](common-issues.md) - 常見問題的快速解決方案
- [Application Debugging 指南](application-debugging.md) - Application 層級除錯
- [Database 疑難排解](database-issues.md) - Database 特定問題
- [Network 和 Connectivity](network-connectivity.md) - Network 疑難排解
- [Deployment 流程](../deployment/deployment-process.md) - Deployment 程序
- [Monitoring 策略](../monitoring/monitoring-strategy.md) - Monitoring 設定

---

**Last Updated**：2025-10-25
**Owner**：DevOps Team
**Review Cycle**：Monthly
