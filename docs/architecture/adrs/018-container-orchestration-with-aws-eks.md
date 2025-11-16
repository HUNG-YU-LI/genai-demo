---
adr_number: 018
title: "Container Orchestration 與 AWS EKS"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [007, 017, 019, 037]
affected_viewpoints: ["deployment", "development", "operational"]
affected_perspectives: ["availability", "performance", "evolution", "development-resource"]
---

# ADR-018: Container Orchestration 與 AWS EKS

## 狀態

**Accepted** - 2025-10-25

## 上下文

### 問題陳述

The Enterprise E-Commerce Platform 需要robust container orchestration solution to:

**Business Requirements**:

- **Scalability**: Auto-scale based on demand (10x 流量尖峰)
- **High Availability**: 99.9% uptime 與 zero-downtime deployments
- **Resource Efficiency**: Optimize infrastructure costs
- **Developer Productivity**: Simplify deployment 和 operations
- **Multi-Region**: 支援 deployment 跨 Taiwan 和 Tokyo
- **Portability**: Avoid vendor lock-in

**Technical Challenges**:

- 複雜的 microservices architecture (13 bounded contexts)
- Variable traffic patterns (peak hours, sales events)
- 需要 rolling updates 和 可以ary deployments
- Service discovery 和 load balancing
- Configuration management
- Monitoring 和 logging at scale
- Security 和 compliance requirements

**目前狀態**：

- Manual deployment to EC2 instances
- No auto-scaling
- 難以manage multiple services
- Long deployment times
- Limited resource utilization
- 複雜的 networking setup

### 業務上下文

**業務驅動因素**：

- 降低 deployment time from hours to minutes
- 支援 rapid feature releases
- Optimize infrastructure costs (30% reduction target)
- 啟用 self-service deployments 用於 developers
- 改善 system reliability 和 availability

**限制條件**：

- 預算: $150,000 用於 initial setup
- Timeline: 4 個月 用於 migration
- Team: 3 DevOps engineers, 8 developers
- 必須 維持 existing services 期間 migration
- Compliance 與 security standards

### 技術上下文

**Current Architecture**:

- Monolithic application on EC2
- Manual deployment scripts
- Static capacity provisioning
- Limited monitoring
- 複雜的 networking

**Target Architecture**:

- Microservices on Kubernetes
- Automated CI/CD pipeline
- Dynamic auto-scaling
- Comprehensive observability
- Service mesh 用於 advanced networking

## 決策驅動因素

1. **Kubernetes Ecosystem**: Industry standard 與 豐富的 ecosystem
2. **AWS Integration**: Native integration 與 AWS services
3. **Managed Service**: 降低 營運開銷
4. **Scalability**: Auto-scaling 用於 pods 和 nodes
5. **High Availability**: Multi-AZ deployment
6. **成本**： Optimize resource utilization
7. **Developer Experience**: Familiar tools 和 workflows
8. **Security**: Built-in security features 和 compliance

## 考慮的選項

### 選項 1： AWS EKS (Elastic Kubernetes Service) - Recommended

**描述**： Managed Kubernetes service with deep AWS integration

**架構**：

```typescript
interface EKSArchitecture {
  controlPlane: {
    management: 'Fully managed by AWS',
    availability: 'Multi-AZ by default',
    version: 'Kubernetes 1.28',
    upgrades: 'Managed with minimal downtime',
    cost: '$0.10/hour per cluster'
  },
  
  nodeGroups: {
    managed: {
      type: 'EKS Managed Node Groups',
      instanceTypes: ['t3.large', 't3.xlarge', 'r6g.large'],
      scaling: {
        min: 3,
        max: 20,
        desired: 5
      },
      updateStrategy: 'Rolling update',
      ami: 'EKS-optimized Amazon Linux 2'
    },
    
    fargate: {
      type: 'AWS Fargate',
      useCase: 'Batch jobs, scheduled tasks',
      billing: 'Per-pod, per-second',
      management: 'Serverless, no node management'
    }
  },
  
  networking: {
    vpc: 'Custom VPC per region',
    subnets: {
      public: 'For load balancers',
      private: 'For application pods',
      database: 'For data tier'
    },
    cni: 'AWS VPC CNI plugin',
    serviceMesh: 'AWS App Mesh (optional)'
  },
  
  storage: {
    ebs: 'For persistent volumes',
    efs: 'For shared file systems',
    storageClasses: [
      'gp3 (general purpose)',
      'io2 (high performance)',
      'efs (shared storage)'
    ]
  }
}
```

**Cluster Configuration**:

```typescript
// CDK Stack for EKS
export class EKSStack extends Stack {
  constructor(scope: Construct, id: string, props: StackProps) {
    super(scope, id, props);
    
    // VPC for EKS
    const vpc = new ec2.Vpc(this, 'EKS-VPC', {
      maxAzs: 3,
      natGateways: 3,
      subnetConfiguration: [
        {
          name: 'Public',
          subnetType: ec2.SubnetType.PUBLIC,
          cidrMask: 24
        },
        {
          name: 'Private',
          subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS,
          cidrMask: 24
        },
        {
          name: 'Database',
          subnetType: ec2.SubnetType.PRIVATE_ISOLATED,
          cidrMask: 24
        }
      ]
    });
    
    // EKS Cluster
    const cluster = new eks.Cluster(this, 'EKS-Cluster', {
      version: eks.KubernetesVersion.V1_28,
      vpc,
      vpcSubnets: [{ subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS }],
      defaultCapacity: 0, // We'll add managed node groups
      endpointAccess: eks.EndpointAccess.PUBLIC_AND_PRIVATE,
      
      // Logging
      clusterLogging: [
        eks.ClusterLoggingTypes.API,
        eks.ClusterLoggingTypes.AUDIT,
        eks.ClusterLoggingTypes.AUTHENTICATOR,
        eks.ClusterLoggingTypes.CONTROLLER_MANAGER,
        eks.ClusterLoggingTypes.SCHEDULER
      ]
    });
    
    // Managed Node Group - General Purpose
    cluster.addNodegroupCapacity('general-purpose', {
      instanceTypes: [
        new ec2.InstanceType('t3.large'),
        new ec2.InstanceType('t3.xlarge')
      ],
      minSize: 3,
      maxSize: 20,
      desiredSize: 5,
      diskSize: 100,
      amiType: eks.NodegroupAmiType.AL2_X86_64,
      capacityType: eks.CapacityType.ON_DEMAND,
      
      labels: {
        'workload-type': 'general'
      },
      
      taints: []
    });
    
    // Managed Node Group - Memory Optimized
    cluster.addNodegroupCapacity('memory-optimized', {
      instanceTypes: [
        new ec2.InstanceType('r6g.large'),
        new ec2.InstanceType('r6g.xlarge')
      ],
      minSize: 2,
      maxSize: 10,
      desiredSize: 3,
      diskSize: 100,
      amiType: eks.NodegroupAmiType.AL2_ARM_64,
      capacityType: eks.CapacityType.SPOT, // Use Spot for cost savings
      
      labels: {
        'workload-type': 'memory-intensive'
      },
      
      taints: [{
        key: 'workload-type',
        value: 'memory-intensive',
        effect: eks.TaintEffect.NO_SCHEDULE
      }]
    });
    
    // Fargate Profile for batch jobs
    cluster.addFargateProfile('batch-jobs', {
      selectors: [
        { namespace: 'batch' },
        { namespace: 'jobs' }
      ]
    });
    
    // Install AWS Load Balancer Controller
    const albController = new eks.AlbController(this, 'ALBController', {
      cluster,
      version: eks.AlbControllerVersion.V2_6_2
    });
    
    // Install Cluster Autoscaler
    const clusterAutoscaler = cluster.addHelmChart('ClusterAutoscaler', {
      chart: 'cluster-autoscaler',
      repository: 'https://kubernetes.github.io/autoscaler',
      namespace: 'kube-system',
      values: {
        autoDiscovery: {
          clusterName: cluster.clusterName
        },
        awsRegion: this.region,
        rbac: {
          create: true
        }
      }
    });
    
    // Install Metrics Server
    cluster.addHelmChart('MetricsServer', {
      chart: 'metrics-server',
      repository: 'https://kubernetes-sigs.github.io/metrics-server/',
      namespace: 'kube-system'
    });
    
    // Install AWS EBS CSI Driver
    const ebsCsiDriver = cluster.addHelmChart('EBSCSIDriver', {
      chart: 'aws-ebs-csi-driver',
      repository: 'https://kubernetes-sigs.github.io/aws-ebs-csi-driver',
      namespace: 'kube-system',
      values: {
        enableVolumeScheduling: true,
        enableVolumeResizing: true,
        enableVolumeSnapshot: true
      }
    });
  }
}
```

**Application Deployment**:

```yaml
# Kubernetes Deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ecommerce-api
  namespace: production
spec:
  replicas: 5
  selector:
    matchLabels:
      app: ecommerce-api
  template:
    metadata:
      labels:
        app: ecommerce-api
        version: v1.0.0
    spec:
      serviceAccountName: ecommerce-api-sa
      
      # Node affinity for general-purpose nodes
      affinity:
        nodeAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
            nodeSelectorTerms:

            - matchExpressions:
              - key: workload-type

                operator: In
                values:

                - general
      
      containers:

      - name: api

        image: 123456789012.dkr.ecr.ap-northeast-3.amazonaws.com/ecommerce-api:v1.0.0
        ports:

        - containerPort: 8080

          name: http
        
        # Resource requests and limits
        resources:
          requests:
            cpu: 500m
            memory: 1Gi
          limits:
            cpu: 2000m
            memory: 2Gi
        
        # Health checks
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 3
        
        # Environment variables
        env:

        - name: SPRING_PROFILES_ACTIVE

          value: production

        - name: DB_HOST

          valueFrom:
            secretKeyRef:
              name: database-credentials
              key: host

        - name: DB_PASSWORD

          valueFrom:
            secretKeyRef:
              name: database-credentials
              key: password

        - name: REDIS_HOST

          valueFrom:
            configMapKeyRef:
              name: redis-config
              key: host
        
        # Volume mounts
        volumeMounts:

        - name: config

          mountPath: /app/config
          readOnly: true
      
      volumes:

      - name: config

        configMap:
          name: ecommerce-api-config

---
# Horizontal Pod Autoscaler
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: ecommerce-api-hpa
  namespace: production
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: ecommerce-api
  minReplicas: 5
  maxReplicas: 50
  metrics:

  - type: Resource

    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70

  - type: Resource

    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:

      - type: Percent

        value: 50
        periodSeconds: 60

      - type: Pods

        value: 5
        periodSeconds: 60
      selectPolicy: Max
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:

      - type: Percent

        value: 10
        periodSeconds: 60
      selectPolicy: Min

---
# Service
apiVersion: v1
kind: Service
metadata:
  name: ecommerce-api
  namespace: production
spec:
  type: ClusterIP
  selector:
    app: ecommerce-api
  ports:

  - port: 80

    targetPort: 8080
    protocol: TCP
    name: http

---
# Ingress with ALB
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: ecommerce-api-ingress
  namespace: production
  annotations:
    kubernetes.io/ingress.class: alb
    alb.ingress.kubernetes.io/scheme: internet-facing
    alb.ingress.kubernetes.io/target-type: ip
    alb.ingress.kubernetes.io/healthcheck-path: /actuator/health
    alb.ingress.kubernetes.io/listen-ports: '[{"HTTP": 80}, {"HTTPS": 443}]'
    alb.ingress.kubernetes.io/ssl-redirect: '443'
    alb.ingress.kubernetes.io/certificate-arn: arn:aws:acm:ap-northeast-3:123456789012:certificate/xxx
spec:
  rules:

  - host: api.ecommerce.example.com

    http:
      paths:

      - path: /

        pathType: Prefix
        backend:
          service:
            name: ecommerce-api
            port:
              number: 80
```

**Monitoring 和 Observability**:

```yaml
# Prometheus ServiceMonitor
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: ecommerce-api-metrics
  namespace: production
spec:
  selector:
    matchLabels:
      app: ecommerce-api
  endpoints:

  - port: http

    path: /actuator/prometheus
    interval: 30s

---
# Grafana Dashboard ConfigMap
apiVersion: v1
kind: ConfigMap
metadata:
  name: ecommerce-dashboard
  namespace: monitoring
data:
  dashboard.json: |
    {
      "dashboard": {
        "title": "E-Commerce API Metrics",
        "panels": [
          {
            "title": "Request Rate",
            "targets": [
              {
                "expr": "rate(http_server_requests_seconds_count[5m])"
              }
            ]
          },
          {
            "title": "Error Rate",
            "targets": [
              {
                "expr": "rate(http_server_requests_seconds_count{status=~\"5..\"}[5m])"
              }
            ]
          },
          {
            "title": "Response Time (P95)",
            "targets": [
              {
                "expr": "histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))"
              }
            ]
          }
        ]
      }
    }
```

**Security Configuration**:

```yaml
# Pod Security Policy
apiVersion: policy/v1beta1
kind: PodSecurityPolicy
metadata:
  name: restricted
spec:
  privileged: false
  allowPrivilegeEscalation: false
  requiredDropCapabilities:

    - ALL

  volumes:

    - 'configMap'
    - 'emptyDir'
    - 'projected'
    - 'secret'
    - 'downwardAPI'
    - 'persistentVolumeClaim'

  hostNetwork: false
  hostIPC: false
  hostPID: false
  runAsUser:
    rule: 'MustRunAsNonRoot'
  seLinux:
    rule: 'RunAsAny'
  fsGroup:
    rule: 'RunAsAny'
  readOnlyRootFilesystem: true

---
# Network Policy
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: ecommerce-api-netpol
  namespace: production
spec:
  podSelector:
    matchLabels:
      app: ecommerce-api
  policyTypes:

  - Ingress
  - Egress

  ingress:

  - from:
    - namespaceSelector:

        matchLabels:
          name: ingress-nginx
    ports:

    - protocol: TCP

      port: 8080
  egress:

  - to:
    - namespaceSelector:

        matchLabels:
          name: database
    ports:

    - protocol: TCP

      port: 5432

  - to:
    - namespaceSelector:

        matchLabels:
          name: cache
    ports:

    - protocol: TCP

      port: 6379
```

**優點**：

- ✅ Fully managed control plane
- ✅ Deep AWS integration (IAM, VPC, ELB, etc.)
- ✅ Auto-scaling 用於 pods 和 nodes
- ✅ Multi-AZ 高可用性
- ✅ 豐富的 ecosystem (Helm, operators)
- ✅ Security features (IAM roles, encryption)
- ✅ Cost optimization (Spot instances, Fargate)
- ✅ Familiar Kubernetes API

**缺點**：

- ⚠️ Control plane cost ($0.10/hour = $73/month per cluster)
- ⚠️ Learning curve 用於 Kubernetes
- ⚠️ 複雜的 networking (VPC CNI)
- ⚠️ AWS-specific features may cause lock-in

**成本**： $150,000 setup + $60,000/year operational (2 clusters)

**風險**： **Low** - Mature, widely adopted solution

### 選項 2： Amazon ECS (Elastic Container Service)

**描述**： AWS-native container orchestration service

**優點**：

- ✅ 簡單的r than Kubernetes
- ✅ Deep AWS integration
- ✅ No control plane cost
- ✅ Fargate 支援

**缺點**：

- ❌ Less flexible than Kubernetes
- ❌ Smaller ecosystem
- ❌ AWS vendor lock-in
- ❌ Limited multi-cloud portability

**成本**： $100,000 setup + $45,000/year operational

**風險**： **Medium** - Vendor lock-in concerns

### 選項 3： Self-Managed Kubernetes on EC2

**描述**： Deploy and manage Kubernetes clusters on EC2 instances

**優點**：

- ✅ Full control over cluster
- ✅ No control plane cost
- ✅ Maximum flexibility

**缺點**：

- ❌ High 營運開銷
- ❌ Manual upgrades 和 patching
- ❌ 複雜的 setup 和 maintenance
- ❌ Requires deep Kubernetes expertise

**成本**： $80,000 setup + $100,000/year operational (DevOps team)

**風險**： **High** - Significant operational burden

## 決策結果

**選擇的選項**： **AWS EKS (Option 1)**

### 理由

AWS EKS 提供s the best balance of Kubernetes ecosystem benefits 與 託管服務 convenience, justifying the control plane cost 用於 降低d 營運開銷 和 改善d reliability.

## 影響分析

### 利害關係人影響

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | High | New deployment model 和 tools | Training, documentation, gradual migration |
| DevOps Team | High | New platform to manage | Training, AWS 支援, automation |
| QA Team | Medium | New testing environments | Staging clusters, test automation |
| Operations Team | Medium | New monitoring 和 troubleshooting | Training, runbooks, dashboards |
| Management | Medium | Investment 和 timeline | ROI analysis, phased approach |

### Impact Radius Assessment

**選擇的影響半徑**： **System**

影響：

- All application services
- Deployment processes
- Infrastructure management
- Monitoring 和 logging
- Security 和 compliance
- Cost structure

### 風險評估

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| Migration 複雜的ity | High | Medium | Phased migration, thorough testing |
| Learning curve | High | Medium | Training, documentation, 支援 |
| Performance issues | Low | High | Load testing, optimization |
| Cost overruns | Medium | Medium | 預算 monitoring, cost optimization |
| Security vulnerabilities | Low | Critical | Security s可以ning, best practices |

**整體風險等級**： **Low**

## 實作計畫

### 第 1 階段： Infrastructure Setup (Month 1)

**Tasks**:

- [ ] Deploy EKS clusters in Taiwan 和 Tokyo
- [ ] Configure VPC 和 networking
- [ ] Set up IAM roles 和 policies
- [ ] Install cluster add-ons (ALB controller, autoscaler)
- [ ] Configure monitoring 和 logging
- [ ] Set up CI/CD pipeline integration

**Success Criteria**:

- EKS clusters operational
- Networking configured
- Add-ons installed 和 tested

### 第 2 階段： Application Containerization (Month 2)

**Tasks**:

- [ ] Containerize all services
- [ ] Create Kubernetes manifests
- [ ] Set up Helm charts
- [ ] Configure secrets management
- [ ] Implement health checks
- [ ] Test in development environment

**Success Criteria**:

- All services containerized
- Manifests validated
- Development deployment successful

### 第 3 階段： Staging Migration (Month 3)

**Tasks**:

- [ ] Deploy to staging EKS cluster
- [ ] Configure auto-scaling
- [ ] Set up monitoring 和 alerting
- [ ] Run integration tests
- [ ] Performance testing
- [ ] Security s可以ning

**Success Criteria**:

- Staging environment operational
- All tests passing
- Performance acceptable

### 第 4 階段： Production Migration (Month 4)

**Tasks**:

- [ ] Gradual production migration
- [ ] Blue-green deployment
- [ ] Monitor performance
- [ ] Validate functionality
- [ ] Optimize configuration
- [ ] Complete documentation

**Success Criteria**:

- 100% traffic on EKS
- All metrics within targets
- Team trained on operations

### 回滾策略

**觸發條件**：

- Critical failures in EKS
- Performance degradation
- Data loss 或 corruption

**回滾步驟**：

1. Route traffic back to EC2
2. Investigate 和 fix issues
3. Retry migration

**回滾時間**： < 2 hours

## 監控和成功標準

### 成功指標

| Metric | Target | Measurement |
|--------|--------|-------------|
| Deployment Time | < 10 minutes | CI/CD metrics |
| Pod Startup Time | < 30 seconds | Kubernetes metrics |
| Auto-scaling Response | < 2 minutes | Cluster autoscaler logs |
| Resource Utilization | > 70% | CloudWatch |
| Availability | > 99.9% | CloudWatch |
| Cost Reduction | 30% | AWS Cost Explorer |

### Review Schedule

- **Weekly**: Cluster health 和 performance
- **Monthly**: Cost 和 resource optimization
- **Quarterly**: Architecture 和 security review

## 後果

### 正面後果

- ✅ **Faster Deployments**: 10-minute deployments vs hours
- ✅ **Auto-Scaling**: Automatic response to load changes
- ✅ **High Availability**: Multi-AZ 與 自動容錯移轉
- ✅ **Cost Optimization**: 30% reduction through 更好的 utilization
- ✅ **Developer Productivity**: Self-service deployments
- ✅ **Portability**: Standard Kubernetes API
- ✅ **豐富的 Ecosystem**: Access to Kubernetes tools 和 operators

### 負面後果

- ⚠️ **Learning Curve**: Team needs Kubernetes training
- ⚠️ **複雜的ity**: More 複雜的 than EC2 deployments
- ⚠️ **Control Plane Cost**: $73/month per cluster
- ⚠️ **Networking 複雜的ity**: VPC CNI learning curve

### 技術債務

**已識別債務**：

1. Some services not fully containerized
2. Manual configuration 用於 some resources
3. Basic monitoring dashboards
4. Limited automation

**債務償還計畫**：

- **Q2 2026**: Complete containerization
- **Q3 2026**: Full GitOps implementation
- **Q4 2026**: Advanced monitoring 和 automation

## 相關決策

- [ADR-007: Use AWS CDK 用於 Infrastructure as Code](007-use-aws-cdk-for-infrastructure.md)
- [ADR-017: Multi-Region Deployment Strategy](017-multi-region-deployment-strategy.md)
- [ADR-019: Progressive Deployment Strategy](019-progressive-deployment-strategy.md)
- [ADR-037: Active-Active Multi-Region Architecture](037-active-active-multi-region-architecture.md)

---

**文檔狀態**： ✅ Accepted  
**上次審查**： 2025-10-25  
**下次審查**： 2026-01-25 （每季）

## 備註

### EKS vs ECS Comparison

| Feature | EKS | ECS |
|---------|-----|-----|
| Kubernetes API | ✅ Yes | ❌ No |
| Ecosystem | 豐富的 | Limited |
| Learning Curve | Steep | Moderate |
| Control Plane Cost | $73/month | Free |
| Multi-Cloud | ✅ Yes | ❌ No |
| AWS Integration | 良好的 | 優秀的 |

### Cost Breakdown (Per Region)

| Component | Monthly Cost |
|-----------|--------------|
| EKS Control Plane | $73 |
| EC2 Nodes (5 x t3.大型的) | $365 |
| EBS Volumes | $50 |
| Data Transfer | $100 |
| Load Balancer | $25 |
| **Total** | **$613/month** |

### Best Practices

**Cluster Management**:

- Use managed node groups
- 啟用 cluster logging
- Regular version upgrades
- Implement pod security policies

**Application Deployment**:

- Use Helm 用於 package management
- Implement proper health checks
- Set resource requests 和 limits
- Use horizontal pod autoscaling

**Security**:

- Use IAM roles 用於 service accounts
- Implement network policies
- 啟用 encryption at rest
- Regular security s可以ning

**Cost Optimization**:

- Use Spot instances 用於 non-critical workloads
- Implement cluster autoscaler
- Right-size pod resources
- Use Fargate 用於 batch jobs
