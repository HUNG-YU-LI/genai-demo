---
adr_number: 033
title: "Secrets Management Strategy"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [014, 016, 007]
affected_viewpoints: ["deployment", "operational"]
affected_perspectives: ["security", "availability"]
---

# ADR-033: Secrets Management Strategy

## 狀態

**Accepted** - 2025-10-25

## 上下文

### 問題陳述

The Enterprise E-Commerce Platform 需要secure management of sensitive configuration data including:

- Database credentials (PostgreSQL, Redis)
- API keys (payment gateways, email services, SMS 提供rs)
- Encryption keys (JWT signing keys, data encryption keys)
- Third-party service credentials
- OAuth client secrets
- Certificate private keys

We 需要secrets management solution：

- Stores secrets securely 與 encryption at rest
- 提供s fine-grained access control
- 啟用s automatic secret rotation
- 支援s audit logging 用於 secret access
- Integrates 與 AWS services 和 Kubernetes
- 啟用s secrets versioning 和 rollback
- 支援s disaster recovery 和 高可用性

### 業務上下文

**業務驅動因素**：

- Security compliance (PCI-DSS, GDPR, SOC 2)
- Prevent credential leakage 和 data breaches
- Regulatory audit requirements
- Zero-trust security model
- 預期的 growth from 10K to 1M+ users

**限制條件**：

- 必須 integrate 與 AWS infrastructure
- 必須 支援 Kubernetes (EKS) workloads
- Performance: Secret retrieval < 100ms
- 預算: $500/month 用於 secrets management
- 必須 支援 automatic rotation 沒有 downtime

### 技術上下文

**目前狀態**：

- AWS EKS 用於 container orchestration
- Spring Boot microservices
- AWS CDK 用於 infrastructure as code
- Multiple environments (dev, staging, production)

**需求**：

- Centralized secrets storage
- Encryption at rest 和 in transit
- Fine-grained IAM-based access control
- Automatic secret rotation
- Audit logging (CloudTrail integration)
- Versioning 和 rollback capability
- High availability (99.9%+)

## 決策驅動因素

1. **Security**: Industry-standard encryption 和 access control
2. **Integration**: 無縫的AWS 和 Kubernetes整合
3. **Automation**: Automatic rotation 沒有 manual intervention
4. **Auditability**: Complete audit trail 用於 compliance
5. **Performance**: Fast secret retrieval (< 100ms)
6. **成本**： Within budget constraints
7. **Operational**: Minimal 營運開銷
8. **Reliability**: High availability 和 disaster recovery

## 考慮的選項

### 選項 1： AWS Secrets Manager

**描述**： Fully managed secrets management service by AWS

**優點**：

- ✅ Fully managed (no infrastructure to 維持)
- ✅ Native AWS integration (RDS, EKS, Lambda)
- ✅ Automatic rotation 用於 RDS, Redshift, DocumentDB
- ✅ Encryption 與 AWS KMS
- ✅ Fine-grained IAM access control
- ✅ CloudTrail audit logging
- ✅ Versioning 和 rollback
- ✅ Cross-region replication
- ✅ High availability (99.99% SLA)
- ✅ Kubernetes integration via External Secrets Operator

**缺點**：

- ⚠️ Cost: $0.40 per secret per 月 + $0.05 per 10K API calls
- ⚠️ AWS vendor lock-in
- ⚠️ Limited to 65,536 bytes per secret

**成本**： $400/month (estimated for 1000 secrets + API calls)

**風險**： **Low** - Proven AWS service

### 選項 2： AWS Systems Manager Parameter Store

**描述**： Hierarchical storage for configuration data and secrets

**優點**：

- ✅ Free 用於 standard parameters (up to 10K)
- ✅ Native AWS integration
- ✅ KMS encryption 用於 SecureString
- ✅ IAM access control
- ✅ CloudTrail audit logging
- ✅ Versioning 支援
- ✅ Parameter hierarchies

**缺點**：

- ❌ No automatic rotation (manual implementation required)
- ❌ Limited to 8KB per parameter (standard) 或 4KB (advanced)
- ❌ Advanced parameters cost $0.05 per parameter per 月
- ❌ Higher throughput tier costs extra
- ❌ Less feature-豐富的 than Secrets Manager

**成本**： $50/month (for advanced parameters)

**風險**： **Medium** - Requires custom rotation logic

### 選項 3： HashiCorp Vault

**描述**： Self-managed secrets management platform

**優點**：

- ✅ Cloud-agnostic (no vendor lock-in)
- ✅ Advanced features (dynamic secrets, PKI, encryption as a service)
- ✅ Fine-grained policies
- ✅ Automatic rotation 支援
- ✅ Audit logging
- ✅ Multi-cloud 支援
- ✅ Active community

**缺點**：

- ❌ Self-managed infrastructure (high 營運開銷)
- ❌ High availability setup 複雜的
- ❌ Requires dedicated team 用於 operations
- ❌ Additional infrastructure costs (EC2, storage, backups)
- ❌ Learning curve 用於 team
- ❌ Maintenance burden (upgrades, patches)

**成本**： $2,000/month (infrastructure + operations)

**風險**： **High** - Operational complexity

### 選項 4： Kubernetes Secrets (Native)

**描述**： Built-in Kubernetes secrets management

**優點**：

- ✅ No additional cost
- ✅ Native Kubernetes integration
- ✅ 簡單use

**缺點**：

- ❌ Base64 encoding only (not encrypted 透過 default)
- ❌ No automatic rotation
- ❌ Limited access control
- ❌ No audit logging
- ❌ Secrets stored in etcd (security concerns)
- ❌ Not suitable 用於 production

**成本**： $0

**風險**： **High** - Insufficient security

## 決策結果

**選擇的選項**： **AWS Secrets Manager with External Secrets Operator for Kubernetes**

### 理由

AWS Secrets Manager被選擇的原因如下：

1. **Fully Managed**: No infrastructure to 維持, 降低s 營運開銷
2. **Automatic Rotation**: Built-in rotation 用於 RDS, custom rotation via Lambda
3. **Security**: KMS encryption, IAM access control, CloudTrail auditing
4. **Integration**: Native AWS integration, Kubernetes via External Secrets Operator
5. **Reliability**: 99.99% SLA, cross-region replication
6. **Compliance**: Meets PCI-DSS, GDPR, SOC 2 requirements
7. **Cost-Effective**: $400/month within 預算, no infrastructure costs
8. **Versioning**: Automatic versioning 和 rollback capability

**架構**：

```text
┌─────────────────────────────────────────────────────────────┐
│                     AWS Secrets Manager                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ DB Credentials│  │  API Keys    │  │ JWT Keys     │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
│         ▲                  ▲                  ▲              │
│         │ KMS Encryption   │                  │              │
│         └──────────────────┴──────────────────┘              │
└─────────────────────────────────────────────────────────────┘
                           │
          ┌────────────────┼────────────────┐
          │                │                │
          ▼                ▼                ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ Spring Boot  │  │   Lambda     │  │     EKS      │
│ (Direct SDK) │  │ (Rotation)   │  │ (Ext Secrets)│
└──────────────┘  └──────────────┘  └──────────────┘
```

**Secret Categories**:

| Category | Storage | Rotation | Access Method |
|----------|---------|----------|---------------|
| Database Credentials | Secrets Manager | Automatic (30 天) | Spring Boot SDK |
| API Keys | Secrets Manager | Manual/Lambda | Spring Boot SDK |
| JWT Signing Keys | Secrets Manager | Manual (90 天) | Spring Boot SDK |
| Kubernetes Secrets | Secrets Manager | Sync via External Secrets | K8s Secret |
| Certificates | Secrets Manager | Manual/ACM | Spring Boot SDK |

## 影響分析

### 利害關係人影響

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | Medium | Need to use Secrets Manager SDK | Training, code examples, libraries |
| Operations Team | Low | Managed service, minimal ops | Monitoring dashboards, runbooks |
| Security Team | Positive | Enhanced secrets security | Regular audits |
| Infrastructure Team | Medium | CDK integration required | CDK constructs, documentation |
| End Users | None | Transparent to users | N/A |

### 影響半徑

**選擇的影響半徑**： **System**

影響：

- All microservices (secret retrieval)
- Infrastructure code (CDK)
- Kubernetes deployments (External Secrets)
- CI/CD pipelines (secret injection)
- Monitoring 和 alerting

### 風險評估

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| Secret retrieval latency | Low | Medium | Caching, connection pooling, retry logic |
| AWS Secrets Manager outage | Very Low | High | Cross-region replication, fallback to cached secrets |
| Cost overrun | Medium | Low | Monitor API calls, implement caching, set 預算 alerts |
| Rotation failures | Low | High | Automated testing, rollback capability, alerts |
| Misconfigured IAM policies | Medium | High | Least privilege, automated policy validation, regular audits |

**整體風險等級**： **Low**

## 實作計畫

### 第 1 階段： Infrastructure Setup （第 1 週）

- [x] Create AWS Secrets Manager secrets 用於 each environment
- [x] Configure KMS encryption keys
- [x] Set up IAM roles 和 policies (least privilege)
- [x] 啟用 CloudTrail logging 用於 Secrets Manager
- [x] Configure cross-region replication (Tokyo backup)
- [x] Set up 預算 alerts

### 第 2 階段： Application Integration （第 2 週）

- [x] Add AWS Secrets Manager SDK to Spring Boot
- [x] Implement secrets retrieval service
- [x] Add caching layer (5-minute TTL)
- [x] Update application configuration
- [x] Implement graceful degradation (cached secrets)
- [x] Add health checks 用於 secrets access

### 第 3 階段： Kubernetes Integration （第 3 週）

- [x] Install External Secrets Operator on EKS
- [x] Configure IRSA (IAM Roles 用於 Service Accounts)
- [x] Create SecretStore 和 ExternalSecret resources
- [x] Migrate existing K8s secrets to Secrets Manager
- [x] Test secret synchronization
- [x] Update deployment manifests

### 第 4 階段： Automatic Rotation （第 4 週）

- [x] 啟用 automatic rotation 用於 RDS credentials
- [x] Create Lambda functions 用於 custom rotation
- [x] Implement rotation 用於 API keys
- [x] Implement rotation 用於 JWT signing keys
- [x] Test rotation 沒有 downtime
- [x] Set up rotation monitoring 和 alerts

### Phase 5: Migration and Testing （第 5 週）

- [x] Migrate secrets from environment variables
- [x] Migrate secrets from config files
- [x] Remove hardcoded secrets from code
- [x] Integration testing
- [x] Security testing (penetration testing)
- [x] Disaster recovery testing

### 回滾策略

**觸發條件**：

- Secret retrieval failures > 5%
- Performance degradation > 200ms
- Rotation failures causing outages
- Cost exceeding 預算 透過 > 50%

**回滾步驟**：

1. Revert to environment variables (temporary)
2. Investigate 和 fix Secrets Manager integration
3. Re-deploy 與 fixes
4. Gradually migrate back to Secrets Manager

**回滾時間**： < 1 hour

## 監控和成功標準

### 成功指標

- ✅ Secret retrieval latency < 100ms (95th percentile)
- ✅ Secret retrieval success rate > 99.9%
- ✅ Zero secret leakage incidents
- ✅ Automatic rotation success rate 100%
- ✅ Audit log completeness 100%
- ✅ Cost within 預算 ($500/month)

### 監控計畫

**CloudWatch Metrics**:

- `secrets.retrieval.time` (histogram)
- `secrets.retrieval.success` (count)
- `secrets.retrieval.failure` (count)
- `secrets.rotation.success` (count)
- `secrets.rotation.failure` (count)
- `secrets.cache.hit_rate` (gauge)

**告警**：

- Secret retrieval failure rate > 1% 用於 5 minutes
- Secret retrieval latency > 200ms 用於 5 minutes
- Rotation failures
- Unauthorized secret access attempts
- Cost exceeding 預算

**Security Monitoring**:

- Secret access patterns (CloudTrail)
- Unauthorized access attempts
- Secret version changes
- IAM policy changes
- Rotation failures

**審查時程**：

- Daily: Check secret access metrics
- Weekly: Review CloudTrail audit logs
- Monthly: Secret rotation verification
- Quarterly: Security audit 和 IAM policy review

## 後果

### 正面後果

- ✅ **Enhanced Security**: KMS encryption, IAM access control
- ✅ **Automatic Rotation**: 降低s manual effort 和 human error
- ✅ **Auditability**: Complete audit trail 用於 compliance
- ✅ **Reliability**: 99.99% SLA, cross-region replication
- ✅ **Integration**: 無縫的AWS 和 Kubernetes整合
- ✅ **Versioning**: Easy rollback to previous secret versions
- ✅ **Operational**: Fully managed, minimal overhead
- ✅ **Compliance**: Meets PCI-DSS, GDPR, SOC 2

### 負面後果

- ⚠️ **成本**： $400/month for secrets storage and API calls
- ⚠️ **Vendor Lock-in**: AWS dependency
- ⚠️ **Latency**: Small overhead 用於 secret retrieval (mitigated 透過 caching)
- ⚠️ **複雜的ity**: Additional integration code required

### 技術債務

**已識別債務**：

1. No multi-cloud secrets management (acceptable 用於 AWS-only deployment)
2. Manual rotation 用於 some secrets (acceptable initially)
3. No secrets s可以ning in CI/CD (應該 be added)

**債務償還計畫**：

- **Q2 2026**: Implement secrets s可以ning in CI/CD pipeline
- **Q3 2026**: Automate rotation 用於 all secrets
- **Q4 2026**: Evaluate multi-cloud secrets management if needed

## 相關決策

- [ADR-014: JWT-Based Authentication Strategy](014-jwt-based-authentication-strategy.md) - JWT key management
- [ADR-016: Data Encryption Strategy](016-data-encryption-strategy.md) - Encryption key management
- [ADR-007: Use AWS CDK 用於 Infrastructure](007-use-aws-cdk-for-infrastructure.md) - Infrastructure provisioning

## 備註

### Secrets Manager Implementation

```java
@Configuration
public class SecretsManagerConfiguration {
    
    @Bean
    public SecretsManagerClient secretsManagerClient() {
        return SecretsManagerClient.builder()
            .region(Region.AP_NORTHEAST_1)
            .build();
    }
    
    @Bean
    public SecretsService secretsService(SecretsManagerClient client) {
        return new SecretsService(client);
    }
}

@Service
public class SecretsService {
    
    private final SecretsManagerClient client;
    private final Cache<String, String> secretsCache;
    
    public SecretsService(SecretsManagerClient client) {
        this.client = client;
        this.secretsCache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(100)
            .build();
    }
    
    public String getSecret(String secretName) {
        return secretsCache.get(secretName, key -> {
            GetSecretValueRequest request = GetSecretValueRequest.builder()
                .secretId(secretName)
                .build();
            
            GetSecretValueResponse response = client.getSecretValue(request);
            return response.secretString();
        });
    }
    
    public <T> T getSecretAsObject(String secretName, Class<T> clazz) {
        String secretJson = getSecret(secretName);
        return objectMapper.readValue(secretJson, clazz);
    }
}

// Usage in application
@Configuration
public class DatabaseConfiguration {
    
    @Autowired
    private SecretsService secretsService;
    
    @Bean
    public DataSource dataSource() {
        DatabaseCredentials creds = secretsService.getSecretAsObject(
            "prod/database/credentials",
            DatabaseCredentials.class
        );
        
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(creds.getJdbcUrl());
        config.setUsername(creds.getUsername());
        config.setPassword(creds.getPassword());
        
        return new HikariDataSource(config);
    }
}
```

### External Secrets Operator Configuration

```yaml
# SecretStore - connects to AWS Secrets Manager
apiVersion: external-secrets.io/v1beta1
kind: SecretStore
metadata:
  name: aws-secrets-manager
  namespace: production
spec:
  provider:
    aws:
      service: SecretsManager
      region: ap-northeast-1
      auth:
        jwt:
          serviceAccountRef:
            name: external-secrets-sa

---
# ExternalSecret - syncs secret from AWS to K8s
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: database-credentials
  namespace: production
spec:
  refreshInterval: 5m
  secretStoreRef:
    name: aws-secrets-manager
    kind: SecretStore
  target:
    name: database-credentials
    creationPolicy: Owner
  data:

    - secretKey: username

      remoteRef:
        key: prod/database/credentials
        property: username

    - secretKey: password

      remoteRef:
        key: prod/database/credentials
        property: password
```

### AWS CDK Infrastructure

```typescript
// CDK construct for secrets
export class SecretsStack extends Stack {
  constructor(scope: Construct, id: string, props?: StackProps) {
    super(scope, id, props);
    
    // Database credentials with automatic rotation
    const dbSecret = new secretsmanager.Secret(this, 'DatabaseCredentials', {
      secretName: 'prod/database/credentials',
      description: 'PostgreSQL database credentials',
      generateSecretString: {
        secretStringTemplate: JSON.stringify({ username: 'admin' }),
        generateStringKey: 'password',
        excludePunctuation: true,
        passwordLength: 32,
      },
    });
    
    // Enable automatic rotation
    dbSecret.addRotationSchedule('RotationSchedule', {
      automaticallyAfter: Duration.days(30),
      rotationLambda: new lambda.Function(this, 'RotationFunction', {
        runtime: lambda.Runtime.PYTHON_3_11,
        handler: 'index.handler',
        code: lambda.Code.fromAsset('lambda/rotation'),
      }),
    });
    
    // JWT signing key
    const jwtSecret = new secretsmanager.Secret(this, 'JWTSigningKey', {
      secretName: 'prod/jwt/signing-key',
      description: 'JWT signing key (RS256)',
      generateSecretString: {
        excludePunctuation: true,
        passwordLength: 64,
      },
    });
    
    // Cross-region replication
    new secretsmanager.CfnReplicaSecret(this, 'JWTSecretReplica', {
      secretId: jwtSecret.secretArn,
      replicaRegions: [
        { region: 'ap-northeast-1' }, // Tokyo backup
      ],
    });
  }
}
```

### Secret Rotation Lambda

```python
import boto3
import json

def handler(event, context):
    """
    Lambda function for rotating API keys
    """
    service_client = boto3.client('secretsmanager')
    
    # Get secret metadata
    token = event['Token']
    step = event['Step']
    secret_arn = event['SecretId']
    
    if step == 'createSecret':
        # Generate new secret
        new_secret = generate_new_api_key()
        service_client.put_secret_value(
            SecretId=secret_arn,
            ClientRequestToken=token,
            SecretString=json.dumps(new_secret),
            VersionStages=['AWSPENDING']
        )
        
    elif step == 'setSecret':
        # Update external service with new key
        update_external_service(new_secret)
        
    elif step == 'testSecret':
        # Test new secret
        test_api_key(new_secret)
        
    elif step == 'finishSecret':
        # Mark new version as current
        service_client.update_secret_version_stage(
            SecretId=secret_arn,
            VersionStage='AWSCURRENT',
            MoveToVersionId=token,
            RemoveFromVersionId=get_current_version(secret_arn)
        )
```

### Secret Naming Convention

Format: `{environment}/{service}/{secret-type}`

Examples:

- `prod/database/credentials` - Production database credentials
- `prod/jwt/signing-key` - JWT signing key
- `prod/api/payment-gateway` - Payment gateway API key
- `prod/api/email-service` - Email service API key
- `staging/database/credentials` - Staging database credentials

### IAM Policy Example

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue",
        "secretsmanager:DescribeSecret"
      ],
      "Resource": [
        "arn:aws:secretsmanager:ap-northeast-1:*:secret:prod/database/*",
        "arn:aws:secretsmanager:ap-northeast-1:*:secret:prod/jwt/*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "kms:Decrypt"
      ],
      "Resource": "arn:aws:kms:ap-northeast-1:*:key/*",
      "Condition": {
        "StringEquals": {
          "kms:ViaService": "secretsmanager.ap-northeast-1.amazonaws.com"
        }
      }
    }
  ]
}
```

---

**文檔狀態**： ✅ Accepted  
**上次審查**： 2025-10-25  
**下次審查**： 2026-01-25 （每季）
