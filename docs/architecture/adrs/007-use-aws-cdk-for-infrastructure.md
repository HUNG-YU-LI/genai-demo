---
adr_number: 007
title: "Use AWS CDK 用於 Infrastructure as Code"
date: 2025-10-24
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [001, 005]
affected_viewpoints: ["deployment", "operational"]
affected_perspectives: ["evolution", "development-resource"]
---

# ADR-007: Use AWS CDK 用於 Infrastructure as Code

## 狀態

**Accepted** - 2025-10-24

## 上下文

### 問題陳述

The Enterprise E-Commerce Platform 需要robust Infrastructure as Code (IaC) solution that:

- Provisions 和 manages AWS infrastructure reliably
- 支援s version control 和 code review 用於 infrastructure changes
- 啟用s automated deployment 和 rollback
- 提供s type safety 和 IDE 支援
- Integrates well 與 CI/CD pipelines
- 支援s multiple environments (dev, staging, production)
- Allows infrastructure testing before deployment
- 維持s consistency 跨 environments

### 業務上下文

**業務驅動因素**：

- 需要 repeatable, consistent infrastructure deployments
- Requirement 用於 disaster recovery 和 environment replication
- Compliance requirements 用於 infrastructure audit trails
- Team growth requiring infrastructure automation
- Multi-region deployment 用於 global users
- Cost optimization through infrastructure as code

**限制條件**：

- AWS cloud platform (already decided)
- Team has strong Java/TypeScript experience
- Limited DevOps/infrastructure experience
- 預算: Infrastructure management 應該 not 需要dedicated team
- Timeline: 3 個月 to production deployment

### 技術上下文

**目前狀態**：

- AWS 雲端基礎設施
- Spring Boot application (Java 21)
- PostgreSQL on RDS (ADR-001)
- Kafka on MSK (ADR-005)
- EKS 用於 container orchestration
- Multiple environments needed (dev, staging, production)

**需求**：

- Provision VPC, subnets, security groups
- Manage RDS PostgreSQL instances
- Configure EKS clusters
- Set up MSK Kafka clusters
- Configure ElastiCache Redis
- Manage IAM roles 和 policies
- Set up CloudWatch monitoring
- 支援 multi-region deployment

## 決策驅動因素

1. **Type Safety**: Catch infrastructure errors at compile time
2. **Developer Experience**: Familiar programming language (TypeScript/Java)
3. **AWS Integration**: Native AWS service 支援
4. **Testing**: Ability to test infrastructure code
5. **Reusability**: Create reusable infrastructure components
6. **Team Skills**: Leverage existing programming skills
7. **維持ability**: 容易understand 和 modify
8. **成本**： No additional licensing costs

## 考慮的選項

### 選項 1： AWS CDK (Cloud Development Kit)

**描述**： Infrastructure as Code using TypeScript/Python/Java with AWS constructs

**優點**：

- ✅ Type-safe infrastructure code (TypeScript/Java)
- ✅ Full IDE 支援 (autocomplete, refactoring)
- ✅ Familiar programming language 用於 team
- ✅ Reusable constructs 和 patterns
- ✅ Built-in testing framework
- ✅ Native AWS service 支援
- ✅ Synthesizes to CloudFormation
- ✅ Active AWS development 和 支援
- ✅ Higher-level abstractions (L2/L3 constructs)
- ✅ 可以 use npm packages 用於 extensions

**缺點**：

- ⚠️ Learning curve 用於 CDK concepts
- ⚠️ Generates CloudFormation (inherits CF limitations)
- ⚠️ Younger than Terraform (less mature ecosystem)
- ⚠️ AWS-specific (vendor lock-in)

**成本**： $0 (open source)

**風險**： **Low** - AWS-backed, growing adoption

### 選項 2： Terraform

**描述**： HashiCorp's infrastructure as code tool with HCL language

**優點**：

- ✅ Multi-cloud 支援 (not AWS-specific)
- ✅ Mature ecosystem 和 community
- ✅ 大型的 module library
- ✅ State management built-in
- ✅ Plan/apply workflow
- ✅ Wide industry adoption

**缺點**：

- ❌ HCL is new language to learn
- ❌ No type safety 或 IDE 支援
- ❌ Limited testing capabilities
- ❌ Verbose configuration
- ❌ State management 複雜的ity
- ❌ Slower AWS feature adoption
- ❌ Requires separate Terraform Cloud 用於 team collaboration

**成本**： $0 (open source), Terraform Cloud: $20/user/month

**風險**： **Low** - Mature, widely adopted

### 選項 3： AWS CloudFormation (YAML/JSON)

**描述**： AWS native IaC using YAML or JSON templates

**優點**：

- ✅ Native AWS service
- ✅ No additional tools needed
- ✅ Direct AWS integration
- ✅ Change sets 用於 preview
- ✅ Rollback 支援

**缺點**：

- ❌ YAML/JSON is verbose 和 error-prone
- ❌ No type safety
- ❌ Limited IDE 支援
- ❌ 難以test
- ❌ No reusable components
- ❌ Poor developer experience
- ❌ Hard to 維持 大型的 templates

**成本**： $0

**風險**： **Low** - AWS native, but poor DX

### 選項 4： Pulumi

**描述**： Infrastructure as Code using general-purpose programming languages

**優點**：

- ✅ Use TypeScript/Python/Go/C#
- ✅ Type safety 和 IDE 支援
- ✅ Multi-cloud 支援
- ✅ 良好的 testing 支援
- ✅ Familiar programming paradigms

**缺點**：

- ❌ Requires Pulumi Cloud 用於 state management
- ❌ Smaller community than Terraform
- ❌ Additional service dependency
- ❌ Less AWS-specific optimization
- ❌ Licensing costs 用於 teams

**成本**： Free tier limited, Team: $75/user/month

**風險**： **Medium** - Smaller ecosystem, cost concerns

## 決策結果

**選擇的選項**： **AWS CDK (Cloud Development Kit) with TypeScript**

### 理由

AWS CDK被選擇的原因如下：

1. **Type Safety**: TypeScript 提供s compile-time error checking 用於 infrastructure code
2. **Developer Experience**: Team already knows TypeScript (Next.js frontend)
3. **IDE 支援**: Full IntelliSense, refactoring, 和 debugging 支援
4. **AWS Native**: Best-in-class AWS service 支援 和 fastest feature adoption
5. **Testing**: Built-in testing framework 用於 infrastructure validation
6. **Reusability**: Create custom constructs 用於 common patterns
7. **Higher Abstractions**: L2/L3 constructs 降低 boilerplate
8. **No Additional Costs**: Open source 與 no licensing fees
9. **CloudFormation Backend**: Inherits CF's reliability 和 rollback capabilities

**實作策略**：

**Language Choice**: TypeScript

- Team has TypeScript experience (Next.js)
- Best CDK documentation 和 examples
- Strong type safety
- 優秀的 IDE 支援

**Project Structure**:

```text
infrastructure/
├── bin/
│   └── app.ts              # CDK app entry point
├── lib/
│   ├── stacks/
│   │   ├── network-stack.ts
│   │   ├── database-stack.ts
│   │   ├── compute-stack.ts
│   │   └── monitoring-stack.ts
│   └── constructs/
│       ├── vpc-construct.ts
│       └── rds-construct.ts
├── test/
│   └── infrastructure.test.ts
├── cdk.json
└── package.json
```

**為何不選 Terraform**： While Terraform is mature 和 multi-cloud, our commitment to AWS 和 team's TypeScript skills make CDK a 更好的 fit. HCL is another language to learn 與 no type safety.

**為何不選 CloudFormation**： YAML/JSON templates are too verbose 和 error-prone. CDK 提供s 更好的 developer experience while still using CloudFormation backend.

**為何不選 Pulumi**： Requires paid service 用於 team collaboration 和 state management. CDK is free 和 AWS-native.

## 影響分析

### 利害關係人影響

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | Medium | Need to learn CDK concepts | Training, examples, documentation |
| Operations Team | High | Infrastructure now in code | Training, runbooks, gradual adoption |
| Architects | Positive | Infrastructure versioned 和 reviewable | Architecture reviews 用於 infrastructure |
| Security Team | Positive | Infrastructure changes auditable | Security s可以ning in CI/CD |
| Business | Positive | Faster, more reliable deployments | N/A |

### 影響半徑

**選擇的影響半徑**： **System**

影響：

- All AWS infrastructure provisioning
- Deployment processes
- Environment management
- Disaster recovery procedures
- Cost management
- Security configuration

### 風險評估

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| CDK learning curve | High | Medium | Training, examples, pair programming |
| CloudFormation limitations | Medium | Medium | Understand CF limits, use escape hatches |
| Infrastructure drift | Medium | High | Regular drift detection, automated reconciliation |
| Breaking changes in CDK | Low | Medium | Pin CDK versions, test upgrades |
| Vendor lock-in to AWS | Low | Low | Acceptable trade-off 用於 AWS commitment |

**整體風險等級**： **Low**

## 實作計畫

### 第 1 階段： Setup and Training （第 1-2 週）

- [x] Install AWS CDK CLI

  ```bash
  npm install -g aws-cdk
  cdk --version
  ```

- [x] Create CDK project structure

  ```bash
  mkdir infrastructure && cd infrastructure
  cdk init app --language typescript
  ```

- [x] Configure AWS credentials 和 profiles
- [x] Set up CDK context 用於 environments
- [x] Conduct team training on CDK basics

### 第 2 階段： Network Infrastructure （第 2-3 週）

- [ ] Create VPC stack

  ```typescript
  export class NetworkStack extends Stack {
    public readonly vpc: ec2.Vpc;
    
    constructor(scope: Construct, id: string, props?: StackProps) {
      super(scope, id, props);
      
      this.vpc = new ec2.Vpc(this, 'VPC', {
        maxAzs: 3,
        natGateways: 2,
        subnetConfiguration: [
          {
            name: 'Public',
            subnetType: ec2.SubnetType.PUBLIC,
          },
          {
            name: 'Private',
            subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS,
          },
          {
            name: 'Isolated',
            subnetType: ec2.SubnetType.PRIVATE_ISOLATED,
          },
        ],
      });
    }
  }
  ```

- [ ] Configure security groups
- [ ] Set up VPC endpoints
- [ ] Deploy 和 test network stack

### 第 3 階段： Database Infrastructure （第 3-4 週）

- [ ] Create RDS stack

  ```typescript
  export class DatabaseStack extends Stack {
    public readonly database: rds.DatabaseInstance;
    
    constructor(scope: Construct, id: string, props: DatabaseStackProps) {
      super(scope, id, props);
      
      this.database = new rds.DatabaseInstance(this, 'Database', {
        engine: rds.DatabaseInstanceEngine.postgres({
          version: rds.PostgresEngineVersion.VER_15,
        }),
        instanceType: ec2.InstanceType.of(
          ec2.InstanceClass.R5,
          ec2.InstanceSize.XLARGE
        ),
        vpc: props.vpc,
        vpcSubnets: {
          subnetType: ec2.SubnetType.PRIVATE_ISOLATED,
        },
        multiAz: true,
        allocatedStorage: 100,
        storageEncrypted: true,
        backupRetention: Duration.days(7),
        deletionProtection: true,
      });
    }
  }
  ```

- [ ] Configure read replicas
- [ ] Set up automated backups
- [ ] Deploy 和 test database stack

### 第 4 階段： Compute Infrastructure （第 4-5 週）

- [ ] Create EKS cluster stack

  ```typescript
  export class ComputeStack extends Stack {
    public readonly cluster: eks.Cluster;
    
    constructor(scope: Construct, id: string, props: ComputeStackProps) {
      super(scope, id, props);
      
      this.cluster = new eks.Cluster(this, 'Cluster', {
        version: eks.KubernetesVersion.V1_28,
        vpc: props.vpc,
        defaultCapacity: 0,
      });
      
      this.cluster.addNodegroupCapacity('NodeGroup', {
        instanceTypes: [
          new ec2.InstanceType('t3.large'),
        ],
        minSize: 2,
        maxSize: 10,
        desiredSize: 3,
      });
    }
  }
  ```

- [ ] Configure auto-scaling
- [ ] Set up load balancers
- [ ] Deploy 和 test compute stack

### Phase 5: Messaging and Caching （第 5-6 週）

- [ ] Create MSK Kafka cluster
- [ ] Create ElastiCache Redis cluster
- [ ] Configure security 和 networking
- [ ] Deploy 和 test

### Phase 6: Monitoring and Observability （第 6-7 週）

- [ ] Create monitoring stack

  ```typescript
  export class MonitoringStack extends Stack {
    constructor(scope: Construct, id: string, props: MonitoringStackProps) {
      super(scope, id, props);
      
      // CloudWatch dashboards
      const dashboard = new cloudwatch.Dashboard(this, 'Dashboard', {
        dashboardName: 'ECommercePlatform',
      });
      
      // Alarms
      new cloudwatch.Alarm(this, 'HighCPU', {
        metric: props.cluster.metricCpuUtilization(),
        threshold: 80,
        evaluationPeriods: 2,
      });
    }
  }
  ```

- [ ] Set up CloudWatch dashboards
- [ ] Configure alarms 和 notifications
- [ ] Deploy 和 test

### Phase 7: CI/CD Integration （第 7-8 週）

- [ ] Create CDK pipeline

  ```typescript
  export class PipelineStack extends Stack {
    constructor(scope: Construct, id: string, props?: StackProps) {
      super(scope, id, props);
      
      const pipeline = new pipelines.CodePipeline(this, 'Pipeline', {
        synth: new pipelines.ShellStep('Synth', {
          input: pipelines.CodePipelineSource.gitHub('org/repo', 'main'),
          commands: [
            'cd infrastructure',
            'npm ci',
            'npm run build',
            'npx cdk synth',
          ],
        }),
      });
      
      pipeline.addStage(new ApplicationStage(this, 'Dev'));
      pipeline.addStage(new ApplicationStage(this, 'Prod'));
    }
  }
  ```

- [ ] Configure automated deployments
- [ ] Set up approval gates
- [ ] Test deployment pipeline

### 回滾策略

**觸發條件**：

- CDK deployment failures > 50%
- Team unable to manage infrastructure
- CloudFormation limitations blocking progress
- Infrastructure drift issues

**回滾步驟**：

1. Export current infrastructure to Terraform
2. Migrate state management
3. Update CI/CD pipelines
4. Train team on Terraform
5. Decommission CDK infrastructure

**回滾時間**： 2-3 weeks

**Note**: Rollback is 複雜的 due to CloudFormation backend. Prevention through testing is key.

## 監控和成功標準

### 成功指標

- ✅ 100% of infrastructure defined in CDK
- ✅ Zero manual infrastructure changes
- ✅ Deployment success rate > 95%
- ✅ Infrastructure drift detection automated
- ✅ All environments consistent
- ✅ Deployment time < 30 minutes
- ✅ Rollback time < 15 minutes

### 監控計畫

**CDK Deployment Metrics**:

- Deployment success/failure rates
- Deployment duration
- Stack drift detection
- Resource creation/update/delete counts

**CloudFormation Monitoring**:

```typescript
// Add custom metrics
new cloudwatch.Metric({
  namespace: 'CDK/Deployments',
  metricName: 'DeploymentDuration',
  statistic: 'Average',
});
```

**告警**：

- Deployment failures
- Stack drift detected
- CloudFormation stack in ROLLBACK state
- Resource creation failures

**審查時程**：

- Daily: Check deployment status
- Weekly: Review infrastructure changes
- Monthly: Infrastructure cost optimization
- Quarterly: CDK version upgrades

## 後果

### 正面後果

- ✅ **Type Safety**: Catch errors at compile time
- ✅ **IDE 支援**: Full IntelliSense 和 refactoring
- ✅ **Testability**: Unit test infrastructure code
- ✅ **Reusability**: Create custom constructs
- ✅ **Version Control**: Infrastructure changes tracked in Git
- ✅ **Code Review**: Infrastructure changes reviewed like code
- ✅ **Consistency**: Same infrastructure 跨 environments
- ✅ **Automation**: Automated deployments 和 rollbacks

### 負面後果

- ⚠️ **Learning Curve**: Team needs to learn CDK concepts
- ⚠️ **CloudFormation Limitations**: Inherits CF constraints
- ⚠️ **AWS Lock-in**: CDK is AWS-specific
- ⚠️ **複雜的ity**: More 複雜的 than 簡單的 YAML templates
- ⚠️ **Debugging**: CloudFormation errors 可以 be cryptic

### 技術債務

**已識別債務**：

1. No infrastructure testing yet (acceptable 用於 MVP)
2. Manual drift detection (可以 be automated)
3. Limited custom constructs (將 grow over time)
4. No multi-region deployment yet (future requirement)

**債務償還計畫**：

- **Q1 2026**: Implement comprehensive infrastructure testing
- **Q2 2026**: Automate drift detection 和 remediation
- **Q3 2026**: Create library of custom constructs
- **Q4 2026**: Implement multi-region deployment

## 相關決策

- [ADR-001: Use PostgreSQL 用於 Primary Database](001-use-postgresql-for-primary-database.md) - RDS provisioning
- [ADR-005: Use Apache Kafka 用於 Event Streaming](005-use-kafka-for-event-streaming.md) - MSK provisioning

## 備註

### CDK Best Practices

**1. Use L2/L3 Constructs**:

```typescript
// ✅ Good: Use L2 construct
const vpc = new ec2.Vpc(this, 'VPC', {
  maxAzs: 3,
});

// ❌ Avoid: Use L1 construct (CloudFormation)
const vpc = new ec2.CfnVPC(this, 'VPC', {
  cidrBlock: '10.0.0.0/16',
});
```

**2. Create Reusable Constructs**:

```typescript
export class DatabaseConstruct extends Construct {
  public readonly database: rds.DatabaseInstance;
  
  constructor(scope: Construct, id: string, props: DatabaseConstructProps) {
    super(scope, id);
    
    this.database = new rds.DatabaseInstance(this, 'Database', {
      // Common configuration
      engine: rds.DatabaseInstanceEngine.postgres(),
      vpc: props.vpc,
      multiAz: true,
      storageEncrypted: true,
      ...props.customConfig,
    });
  }
}
```

**3. Use Environment-Specific Configuration**:

```typescript
const app = new cdk.App();

new MyStack(app, 'Dev', {
  env: { account: '111111111111', region: 'us-east-1' },
  instanceType: 't3.small',
});

new MyStack(app, 'Prod', {
  env: { account: '222222222222', region: 'us-east-1' },
  instanceType: 'r5.xlarge',
});
```

**4. Test Infrastructure Code**:

```typescript
import { Template } from 'aws-cdk-lib/assertions';

test('VPC Created', () => {
  const app = new cdk.App();
  const stack = new NetworkStack(app, 'TestStack');
  const template = Template.fromStack(stack);
  
  template.hasResourceProperties('AWS::EC2::VPC', {
    CidrBlock: '10.0.0.0/16',
  });
});
```

### Common CDK Commands

```bash
# Initialize new project
cdk init app --language typescript

# List all stacks
cdk list

# Synthesize CloudFormation template
cdk synth

# Show differences
cdk diff

# Deploy stack
cdk deploy

# Deploy all stacks
cdk deploy --all

# Destroy stack
cdk destroy

# Check for drift
cdk diff --no-version-reporting
```

### Multi-Environment Setup

```typescript
// bin/app.ts
const app = new cdk.App();

const devEnv = {
  account: process.env.CDK_DEV_ACCOUNT,
  region: 'us-east-1',
};

const prodEnv = {
  account: process.env.CDK_PROD_ACCOUNT,
  region: 'us-east-1',
};

new NetworkStack(app, 'Dev-Network', { env: devEnv });
new DatabaseStack(app, 'Dev-Database', { env: devEnv });

new NetworkStack(app, 'Prod-Network', { env: prodEnv });
new DatabaseStack(app, 'Prod-Database', { env: prodEnv });
```

### Cost Optimization

```typescript
// Use tagging for cost allocation
cdk.Tags.of(stack).add('Environment', 'Production');
cdk.Tags.of(stack).add('Project', 'ECommerce');
cdk.Tags.of(stack).add('CostCenter', 'Engineering');

// Use removal policies
database.applyRemovalPolicy(cdk.RemovalPolicy.RETAIN);
```

---

**文檔狀態**： ✅ Accepted  
**上次審查**： 2025-10-24  
**下次審查**： 2026-01-24 （每季）
