---
adr_number: 024
title: "Monorepo vs Multi-Repo Strategy"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [2, 7, 18, 19]
affected_viewpoints: ["development", "deployment"]
affected_perspectives: ["evolution", "development-resource"]
decision_makers: ["Architecture Team", "Development Team", "DevOps Team"]
---

# ADR-024: Monorepo vs Multi-Repo Strategy

## 狀態

**Status**: Accepted

**Date**: 2025-10-25

**Decision Makers**: Architecture Team, Development Team, DevOps Team

## 上下文

### 問題陳述

企業電子商務平台由multiple components (backend services, frontend applications, infrastructure code, shared libraries) that need to be organized in a version control repository structure. We need to decide between:組成

- **Monorepo**: Single repository containing all code
- **Multi-Repo**: Separate repositories 用於 each component
- **Hybrid**: Combination of both approaches

This decision impacts:

- Code sharing 和 reusability
- Build 和 deployment pipelines
- Team collaboration 和 ownership
- Dependency management
- Release coordination
- Developer experience

### 業務上下文

**業務驅動因素**：

- Fast feature delivery requiring cross-component changes
- Code reuse 跨 multiple services 和 applications
- Consistent coding standards 和 tooling
- Simplified dependency management
- Atomic commits 跨 multiple components
- 降低d context switching 用於 developers

**Business Constraints**:

- Team size: 15-20 developers 跨 multiple teams
- Multiple programming languages (Java, TypeScript, Python)
- Multiple deployment targets (backend services, frontends, infrastructure)
- 需要 independent service deployment
- CI/CD pipeline performance requirements

**Business Requirements**:

- 支援 rapid feature development
- 啟用 code sharing 跨 components
- 維持 clear ownership boundaries
- 支援 independent service scaling
- 啟用 efficient code reviews

### 技術上下文

**Current Architecture**:

- Backend: Spring Boot microservices (Java 21)
- Frontend: Next.js (CMC), Angular (Consumer)
- Infrastructure: AWS CDK (TypeScript)
- Shared libraries: Domain models, utilities
- 13 bounded contexts 與 potential 用於 independent services

**Technical Constraints**:

- Multiple build tools (Gradle, npm, Maven)
- Multiple deployment pipelines
- 需要 selective builds (only changed components)
- Git repository size 和 performance
- CI/CD pipeline execution time
- Developer machine performance

**Dependencies**:

- ADR-002: Hexagonal Architecture (affects code organization)
- ADR-007: AWS CDK 用於 Infrastructure (infrastructure code location)
- ADR-018: Container Orchestration 與 EKS (deployment strategy)
- ADR-019: Progressive Deployment Strategy (deployment independence)

## 決策驅動因素

- **Code Sharing**: Maximize code reuse 跨 services 和 applications
- **Developer Experience**: Minimize context switching 和 repository management overhead
- **Build Performance**: Ensure fast CI/CD pipelines 與 selective builds
- **Team Autonomy**: 啟用 teams to work independently 沒有 blocking
- **Dependency Management**: Simplify dependency updates 和 version management
- **Atomic Changes**: 支援 cross-component changes in single commit
- **Tooling**: Leverage modern monorepo tools (Nx, Turborepo, Bazel)

## 考慮的選項

### 選項 1： Monorepo with Selective Builds

**描述**：
Single repository containing all code (backend, frontend, infrastructure, shared libraries) 與 build tools that 支援 selective builds based on changed files.

**Pros** ✅:

- **Atomic Changes**: Cross-component changes in single commit 和 PR
- **Code Sharing**: 容易share code 跨 services 沒有 versioning overhead
- **Consistent Tooling**: Single set of linters, formatters, 和 CI/CD configuration
- **Simplified Dependency Management**: All dependencies in one place, easier to update
- **更好的 Refactoring**: IDE 支援 用於 cross-component refactoring
- **Single Source of Truth**: All code in one place, easier to search 和 navigate
- **Simplified Onboarding**: New developers clone one repository
- **Unified CI/CD**: Single pipeline configuration 用於 all components

**Cons** ❌:

- **Repository Size**: 大型的 repository 可以 slow down Git operations
- **Build 複雜的ity**: Requires sophisticated build tools 用於 selective builds
- **Access Control**: 更難restrict access to specific components
- **CI/CD Performance**: Risk of slow pipelines 沒有 proper optimization
- **Merge Conflicts**: Higher potential 用於 conflicts 與 many developers
- **Tooling Investment**: Requires investment in monorepo tooling (Nx, Turborepo)

**成本**：

- **Implementation Cost**: 4 person-weeks (setup monorepo tools, migrate code, configure CI/CD)
- **Tooling Cost**: $0 (open source tools)
- **Maintenance Cost**: 1 person-day/month (monorepo tool updates, optimization)
- **Total Cost of Ownership (3 年)**: ~$25,000 (labor)

**風險**： Medium

**Risk Description**: Build performance degradation, Git performance issues 與 大型的 repository

**Effort**: Medium

**Effort Description**: Moderate setup effort, 需要learning monorepo tools

### 選項 2： Multi-Repo with Shared Libraries

**描述**：
Separate repositories 用於 each major component (backend, CMC frontend, consumer frontend, infrastructure) 與 shared libraries published as packages.

**Pros** ✅:

- **Clear Ownership**: Each repository has clear team ownership
- **Independent Deployment**: Services 可以 be deployed independently
- **Smaller Repositories**: Faster Git operations
- **Access Control**: 容易restrict access per repository
- **簡單的r CI/CD**: Each repository has independent pipeline
- **Flexible Tooling**: Each repository 可以 use different tools
- **Proven Pattern**: Well-understood approach used 透過 many organizations

**Cons** ❌:

- **Cross-Repo Changes**: 難以make atomic changes 跨 repositories
- **Dependency Hell**: 複雜的 dependency management 與 versioned packages
- **Code Duplication**: Tendency to duplicate code instead of sharing
- **Inconsistent Tooling**: Different linters, formatters per repository
- **Context Switching**: Developers need to switch between repositories
- **Complicated Refactoring**: Cross-repo refactoring 需要multiple PRs
- **Version Coordination**: 難以coordinate versions 跨 repositories
- **Onboarding Overhead**: New developers need to clone multiple repositories

**成本**：

- **Implementation Cost**: 2 person-weeks (split existing code, setup shared libraries)
- **Publishing Cost**: $0 (use GitHub Packages 或 npm registry)
- **Maintenance Cost**: 2 person-days/month (coordinate versions, publish packages)
- **Total Cost of Ownership (3 年)**: ~$35,000 (labor + coordination overhead)

**風險**： Medium

**Risk Description**: Dependency management 複雜的ity, version coordination challenges

**Effort**: Low

**Effort Description**: 簡單implement, well-understood pattern

### 選項 3： Hybrid Approach (Monorepo for Backend, Separate Repos for Frontends)

**描述**：
Monorepo 用於 backend services 和 shared libraries, separate repositories 用於 frontend applications 和 infrastructure.

**Pros** ✅:

- **Backend Code Sharing**: Easy sharing 跨 backend services
- **Frontend Independence**: Frontends 可以 evolve independently
- **Balanced 複雜的ity**: 簡單的r than full monorepo, 更好的 than full multi-repo
- **Team Alignment**: Backend team uses monorepo, frontend teams use separate repos
- **Flexible Deployment**: Frontends deploy independently, backend services coordinated

**Cons** ❌:

- **Partial Benefits**: Doesn't get full benefits of either approach
- **Cross-Boundary Changes**: Still difficult to make changes 跨 backend/frontend
- **Inconsistent Experience**: Different workflows 用於 backend vs frontend developers
- **Shared Library 複雜的ity**: Still need to publish shared libraries 用於 frontends
- **Tooling Overhead**: Need to 維持 both monorepo 和 multi-repo tooling

**成本**：

- **Implementation Cost**: 3 person-weeks
- **Maintenance Cost**: 1.5 person-days/month
- **Total Cost of Ownership (3 年)**: ~$30,000

**風險**： Medium

**Risk Description**: 複雜的ity of 維持ing both approaches

**Effort**: Medium

**Effort Description**: Moderate effort to set up 和 維持

## 決策結果

**選擇的選項**： Option 1 - Monorepo with Selective Builds

**Rationale**:
We chose monorepo 與 selective builds as our repository strategy. This decision prioritizes developer experience, code sharing, 和 atomic changes over repository independence:

1. **Atomic Cross-Component Changes**: E-commerce features often 需要changes 跨 backend services, frontends, 和 infrastructure. Monorepo 啟用s atomic commits 和 PRs, reducing coordination overhead 和 deployment risks.

2. **Code Sharing Without Versioning**: Shared domain models, utilities, 和 types 可以 be imported directly 沒有 publishing packages. This eliminates dependency hell 和 version coordination challenges.

3. **Consistent Tooling**: Single set of linters (ESLint, Checkstyle), formatters (Prettier, Google Java Format), 和 CI/CD configuration ensures consistent code quality 跨 all components.

4. **更好的 Refactoring**: IDE 支援 用於 cross-component refactoring 啟用s safe 大型的-scale changes. Renaming a domain model automatically updates all usages 跨 services 和 frontends.

5. **Simplified Dependency Management**: All dependencies in root package.json 和 build.gradle. Updating a dependency (e.g., Spring Boot version) is a single commit affecting all services.

6. **Developer Experience**: New developers clone one repository 和 have access to all code. No context switching between repositories, no need to manage multiple Git remotes.

7. **Modern Tooling**: Tools like Nx 和 Turborepo 提供 sophisticated build caching 和 selective builds, addressing traditional monorepo performance concerns.

8. **Team Size**: With 15-20 developers, monorepo is manageable. 大型的 companies (Google, Facebook, Microsoft) successfully use monorepos 與 thousands of developers.

**Key Factors in Decision**:

1. **Atomic Changes**: Cross-component features are common in e-commerce (e.g., new payment method 需要backend, frontend, 和 infrastructure changes)
2. **Code Sharing**: Domain models, types, 和 utilities shared 跨 13 bounded contexts
3. **Team Collaboration**: Small team benefits from unified codebase 和 tooling
4. **Modern Tooling**: Nx 和 Turborepo solve traditional monorepo performance issues

## 影響分析

### 利害關係人影響

| Stakeholder | Impact Level | Description | Mitigation Strategy |
|-------------|--------------|-------------|-------------------|
| Development Team | High | New workflow, learning monorepo tools | Training sessions, documentation, pair programming |
| DevOps Team | High | New CI/CD pipeline 與 selective builds | Gradual migration, performance monitoring |
| Frontend Team | Medium | Share repository 與 backend team | Clear ownership boundaries, CODEOWNERS file |
| Backend Team | Low | Similar to current workflow | Minimal changes needed |
| New Hires | Low | 簡單的r onboarding (one repository) | Updated onboarding documentation |

### Impact Radius Assessment

**選擇的影響半徑**： Enterprise

**Impact Description**:

- **Enterprise**: Changes affect entire development workflow
  - All developers 必須 adopt monorepo workflow
  - All CI/CD pipelines 必須 支援 selective builds
  - All teams 必須 follow monorepo conventions
  - All documentation 必須 be updated

### Affected Components

- **All Source Code**: Migrated to monorepo structure
- **CI/CD Pipelines**: Rewritten to 支援 selective builds
- **Build Tools**: Nx 或 Turborepo integrated
- **Developer Machines**: Monorepo tools installed
- **Documentation**: Updated 用於 monorepo workflow
- **Git Hooks**: Updated 用於 monorepo structure

### 風險評估

| Risk | Probability | Impact | Mitigation Strategy | Owner |
|------|-------------|--------|-------------------|-------|
| Git performance degradation | Medium | Medium | Use Git LFS 用於 大型的 files, shallow clones | DevOps Team |
| CI/CD pipeline slowdown | Medium | High | Implement build caching, selective builds | DevOps Team |
| Merge conflict increase | Medium | Medium | Clear ownership boundaries, frequent integration | Development Team |
| Learning curve 用於 monorepo tools | High | Low | Training sessions, documentation | Tech Lead |
| Repository size growth | Low | Medium | Regular cleanup, Git LFS 用於 binaries | DevOps Team |

**整體風險等級**： Medium

**Risk Mitigation Plan**:

- Implement Nx 或 Turborepo 用於 build caching 和 selective builds
- Use Git LFS 用於 大型的 binary files (images, fonts)
- Configure shallow clones 用於 CI/CD pipelines
- Establish clear ownership boundaries 與 CODEOWNERS file
- Monitor CI/CD pipeline performance 和 optimize
- 提供 comprehensive training on monorepo workflow

## 實作計畫

### 第 1 階段： Monorepo Setup (Timeline: Week 1)

**Objectives**:

- Set up monorepo structure
- Configure build tools
- Migrate existing code

**Tasks**:

- [ ] Create monorepo structure (apps/, libs/, tools/, docs/)
- [ ] Install 和 configure Nx 或 Turborepo
- [ ] Migrate backend services to monorepo
- [ ] Migrate frontend applications to monorepo
- [ ] Migrate infrastructure code to monorepo
- [ ] Configure workspace dependencies
- [ ] Set up CODEOWNERS file 用於 ownership boundaries

**Deliverables**:

- Monorepo structure 與 all code migrated
- Build tools configured
- CODEOWNERS file established

**Success Criteria**:

- All code accessible in single repository
- Build tools working 用於 all components
- Clear ownership boundaries defined

### 第 2 階段： CI/CD Pipeline (Timeline: Week 2)

**Objectives**:

- Configure selective builds
- Implement build caching
- Set up deployment pipelines

**Tasks**:

- [ ] Configure Nx/Turborepo affected commands
- [ ] Set up build caching (local 和 remote)
- [ ] Configure CI/CD pipeline 用於 selective builds
- [ ] Implement parallel builds 用於 independent components
- [ ] Set up deployment pipelines per component
- [ ] Configure branch protection rules
- [ ] Test pipeline performance 與 sample changes

**Deliverables**:

- CI/CD pipeline 與 selective builds
- Build caching configured
- Deployment pipelines operational

**Success Criteria**:

- Only affected components build on changes
- Build caching 降低s build time 透過 50%+
- Deployment pipelines working 用於 all components

### 第 3 階段： Developer Tooling (Timeline: Week 3)

**Objectives**:

- Configure developer tools
- Set up IDE integration
- Create documentation

**Tasks**:

- [ ] Configure ESLint, Prettier 用於 monorepo
- [ ] Set up IDE integration (IntelliJ, VS Code)
- [ ] Configure Git hooks (pre-commit, pre-push)
- [ ] Create developer documentation
- [ ] Set up local development scripts
- [ ] Configure debugging 用於 monorepo
- [ ] Create troubleshooting guide

**Deliverables**:

- Developer tools configured
- IDE integration working
- Comprehensive documentation

**Success Criteria**:

- Developers 可以 build 和 run all components locally
- IDE 提供s cross-component navigation 和 refactoring
- Documentation covers common workflows

### 第 4 階段： Training and Migration (Timeline: Week 4)

**Objectives**:

- Train development team
- Migrate active branches
- Validate workflow

**Tasks**:

- [ ] Conduct training sessions on monorepo workflow
- [ ] Migrate active feature branches to monorepo
- [ ] Validate CI/CD pipeline 與 real changes
- [ ] Conduct pair programming sessions
- [ ] Gather feedback 和 address issues
- [ ] Update onboarding documentation
- [ ] Archive old repositories

**Deliverables**:

- Trained development team
- All active work migrated
- Validated workflow

**Success Criteria**:

- All developers comfortable 與 monorepo workflow
- No blocking issues identified
- Positive feedback from team

### 回滾策略

**觸發條件**：

- Git performance degradation > 50% slower
- CI/CD pipeline performance degradation > 100% slower
- Critical blocking issues preventing development
- Team consensus that monorepo is not working

**回滾步驟**：

1. **Immediate Action**: Freeze monorepo changes, revert to old repositories
2. **Code Extraction**: Extract code back to separate repositories
3. **CI/CD Restoration**: Restore old CI/CD pipelines
4. **Team Communication**: Communicate rollback plan 和 timeline
5. **Verification**: Confirm all teams 可以 work 與 old repositories

**回滾時間**： 1-2 days

**Rollback Testing**: Test rollback procedure in staging environment before production

## 監控和成功標準

### 成功指標

| Metric | Target | Measurement Method | Review Frequency |
|--------|--------|-------------------|------------------|
| CI/CD Build Time | < 10 minutes | Pipeline execution time | Daily |
| Build Cache Hit Rate | > 80% | Nx/Turborepo cache metrics | Weekly |
| Git Clone Time | < 2 minutes | Developer feedback | Monthly |
| Cross-Component Changes | > 30% of PRs | PR analysis | Monthly |
| Developer Satisfaction | > 4/5 | Survey | Quarterly |

### 監控計畫

**Dashboards**:

- **CI/CD Performance Dashboard**: Build times, cache hit rates, pipeline success rates
- **Repository Health Dashboard**: Repository size, Git performance, active branches
- **Developer Experience Dashboard**: Clone times, build times, satisfaction scores

**告警**：

- **Warning**: CI/CD build time > 15 minutes (Slack)
- **Warning**: Build cache hit rate < 70% (Slack)
- **Info**: Repository size > 5GB (Email)

**審查時程**：

- **Daily**: Quick check of CI/CD performance
- **Weekly**: Detailed review of build metrics 和 cache performance
- **Monthly**: Developer experience survey 和 feedback session
- **Quarterly**: Comprehensive review of monorepo strategy

### Key Performance Indicators (KPIs)

- **Productivity KPI**: 20% reduction in time 用於 cross-component changes
- **Quality KPI**: 30% increase in code reuse 跨 components
- **Efficiency KPI**: 50% reduction in dependency management overhead
- **Experience KPI**: 4/5 developer satisfaction score

## 後果

### Positive Consequences ✅

- **Atomic Changes**: Cross-component features in single PR 降低s coordination
- **Code Sharing**: Easy sharing of domain models, utilities, types
- **Consistent Tooling**: Single linting, formatting, testing configuration
- **更好的 Refactoring**: IDE 支援 用於 cross-component refactoring
- **Simplified Dependencies**: All dependencies in one place
- **Unified CI/CD**: Single pipeline configuration
- **Easier Onboarding**: New developers clone one repository
- **更好的 Collaboration**: All code visible to all developers

### Negative Consequences ❌

- **Learning Curve**: Team needs to learn monorepo tools (Mitigation: Training 和 documentation)
- **Git Performance**: 大型的 repository may slow Git operations (Mitigation: Git LFS, shallow clones)
- **Build 複雜的ity**: Requires sophisticated build tools (Mitigation: Use proven tools like Nx)
- **Access Control**: 更難restrict access to components (Mitigation: Use CODEOWNERS 和 branch protection)

### 技術債務

**Debt Introduced**:

- **Monorepo Tooling Dependency**: Dependent on Nx 或 Turborepo 用於 build performance
- **Build Configuration 複雜的ity**: 複雜的 build configuration 需要maintenance
- **Migration Effort**: Future migration away from monorepo would be signifi可以t effort

**債務償還計畫**：

- **Tooling**: Regularly update monorepo tools to latest versions
- **Configuration**: Quarterly review 和 simplification of build configuration
- **Migration**: Document extraction procedures 用於 potential future migration

### Long-term Implications

This decision establishes monorepo as our standard repository strategy 用於 the next 3-5 年. As the platform evolves:

- Consider splitting monorepo if repository size exceeds 10GB
- Evaluate new monorepo tools as they emerge (Bazel, Rush, etc.)
- Monitor Git performance 和 optimize as needed
- Reassess strategy if team grows beyond 50 developers

The monorepo 提供s foundation 用於 future platform evolution, enabling seamless addition of new services 和 applications 沒有 repository management overhead.

## 相關決策

### Related ADRs

- [ADR-002: Hexagonal Architecture](20250117-002-adopt-hexagonal-architecture.md) - Affects code organization in monorepo
- [ADR-007: AWS CDK 用於 Infrastructure](20250117-007-aws-cdk-for-infrastructure.md) - Infrastructure code in monorepo
- [ADR-018: Container Orchestration 與 EKS](20250117-018-container-orchestration-eks.md) - Deployment from monorepo
- [ADR-019: Progressive Deployment Strategy](20250117-019-progressive-deployment-strategy.md) - Independent deployment from monorepo

### Affected Viewpoints

- [Development Viewpoint](../../viewpoints/development/README.md) - Repository structure 和 build process
- [Deployment Viewpoint](../../viewpoints/deployment/README.md) - CI/CD pipelines 和 deployment

### Affected Perspectives

- [Evolution Perspective](../../perspectives/evolution/README.md) - Code evolution 和 refactoring
- [Development Resource Perspective](../../perspectives/development-resource/README.md) - Developer workflow 和 tooling

## 備註

### Assumptions

- Team size remains under 50 developers
- Git performance acceptable 與 repository size < 10GB
- Monorepo tools (Nx, Turborepo) continue to be 維持ed
- Team 將ing to learn new workflow 和 tools
- CI/CD infrastructure 可以 處理 selective builds

### Constraints

- 必須 支援 multiple programming languages (Java, TypeScript, Python)
- 必須 支援 multiple build tools (Gradle, npm)
- 必須 啟用 independent service deployment
- 必須 維持 reasonable CI/CD pipeline performance
- 必須 work 與 existing Git hosting (GitHub)

### Open Questions

- 應該 we use Nx 或 Turborepo 用於 build orchestration?
- What is optimal repository size before considering split?
- 應該 we use Git LFS 用於 all binary files?
- How to 處理 very 大型的 files (videos, datasets)?

### Follow-up Actions

- [ ] Evaluate Nx vs Turborepo 和 make selection - Architecture Team
- [ ] Create monorepo migration plan - DevOps Team
- [ ] Develop training materials 用於 monorepo workflow - Tech Lead
- [ ] Set up build 快取基礎設施 - DevOps Team
- [ ] Configure CODEOWNERS file - Team Leads
- [ ] Monitor Git 和 CI/CD performance - DevOps Team

### References

- [Monorepo Tools Comparison](https://monorepo.tools/)
- [Nx Documentation](https://nx.dev/)
- [Turborepo Documentation](https://turbo.build/repo)
- [Google's Monorepo Approach](https://cacm.acm.org/magazines/2016/7/204032-why-google-stores-billions-of-lines-of-code-in-a-single-repository/fulltext)
- [Microsoft's Monorepo Journey](https://devblogs.microsoft.com/engineering-at-microsoft/monorepo-at-microsoft/)
- [Monorepo Best Practices](https://github.com/korfuri/awesome-monorepo)

---

**ADR Template Version**: 1.0  
**Last Template Update**: 2025-01-17
