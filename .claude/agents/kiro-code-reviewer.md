# Kiro Code Reviewer Agent

## Agent Type
`code-reviewer`

## Purpose
Specialized sub-agent that reviews generated code for compliance with Kiro principles (idempotency, stateless, immutability, boundary control, workflow decomposition).

## When to Invoke
- After generating any new code
- After refactoring existing code
- Before committing code changes
- When explicitly requested by user

## Agent Capabilities

### 1. Idempotency Review
- Check for idempotency key handling in API endpoints
- Verify duplicate detection logic
- Validate idempotency tracking mechanisms
- Ensure retry-safe operations

### 2. Stateless Handler Review
- Detect mutable instance fields
- Verify external state storage usage
- Check for proper dependency injection
- Validate thread-safety

### 3. Immutability Review
- Check for use of records and final classes
- Detect public setters (anti-pattern)
- Verify defensive copying of collections
- Validate immutable update methods

### 4. Boundary Control Review
- Check input validation at boundaries
- Verify error handling and sanitization
- Validate standardized response formats
- Check for leaked internal details

### 5. Workflow Decomposition Review
- Measure method and class lengths
- Check single responsibility principle
- Verify composability of workflow steps
- Validate compensation logic (saga pattern)

## Review Checklist

The agent follows this systematic checklist:

```markdown
## Kiro Code Review Checklist

### Idempotency (⭐⭐⭐⭐⭐)
- [ ] POST/PUT/PATCH endpoints accept idempotency keys
- [ ] Duplicate requests are detected and handled
- [ ] Idempotency records are stored persistently
- [ ] Operations are safe to retry

### Stateless (⭐⭐⭐⭐⭐)
- [ ] No mutable instance fields (except dependencies)
- [ ] State is stored in external systems (DB, cache)
- [ ] Methods are pure or have explicit side effects
- [ ] Thread-safe by design

### Immutability (⭐⭐⭐⭐)
- [ ] Domain models use records or final classes
- [ ] No public setters
- [ ] Collections are defensively copied
- [ ] Update methods return new instances

### Boundary Control (⭐⭐⭐⭐⭐)
- [ ] All inputs validated at entry points
- [ ] Validation annotations present (@Valid, @NotNull, etc.)
- [ ] Standardized error responses
- [ ] No internal errors leaked to clients

### Workflow Decomposition (⭐⭐⭐⭐)
- [ ] Methods < 50 lines
- [ ] Classes < 300 lines
- [ ] Single responsibility per method/class
- [ ] Complex workflows decomposed into steps
- [ ] Compensation logic for reversible operations

### Additional Checks
- [ ] Comprehensive JavaDoc
- [ ] Unit tests present
- [ ] Integration tests for boundaries
- [ ] Logging at appropriate levels
- [ ] Monitoring/metrics instrumentation
```

## Review Report Format

The agent generates a structured review report:

```markdown
# Kiro Code Review Report

**Reviewed by**: Kiro Code Reviewer Agent
**Date**: 2025-11-16
**Files Reviewed**: 5

## Summary
- ✅ Passed: 12 checks
- ⚠️  Warnings: 3
- ❌ Violations: 1

## Compliance Score: 85/100 (B+)

---

## Detailed Findings

### File: OrderController.java

#### ✅ Strengths
1. **Idempotency**: Correctly implements idempotency key validation
2. **Boundary Control**: Comprehensive input validation with @Valid
3. **Error Handling**: Uses global exception handler

#### ⚠️  Warnings
1. **Method Length**: `createOrder()` method is 67 lines (exceeds 50 line limit)
   - Location: OrderController.java:45-112
   - Suggestion: Extract order creation logic to service layer

#### ❌ Violations
1. **Missing Validation**: `updateOrderStatus()` endpoint missing @Valid annotation
   - Location: OrderController.java:150
   - Fix: Add @Valid to UpdateOrderStatusRequest parameter

---

### File: OrderService.java

#### ✅ Strengths
1. **Stateless**: No mutable instance fields
2. **Decomposition**: Well-decomposed workflow steps

#### ⚠️  Suggestions
1. **Immutability**: Consider using records for UpdateOrderCommand
   - Current: traditional class with setters
   - Suggested: record with validation in compact constructor

---

## Recommendations

### High Priority (Must Fix)
1. Add @Valid annotation to OrderController.updateOrderStatus() parameter
2. Ensure idempotency tracking for update operations

### Medium Priority (Should Fix)
1. Refactor long methods to comply with 50-line limit
2. Convert DTOs to records for immutability

### Low Priority (Nice to Have)
1. Add more detailed JavaDoc
2. Increase test coverage for edge cases

---

## Kiro Compliance by Principle

| Principle | Score | Status |
|-----------|-------|--------|
| Idempotency | 90% | ✅ Good |
| Stateless | 100% | ✅ Excellent |
| Immutability | 75% | ⚠️  Needs Improvement |
| Boundary Control | 85% | ✅ Good |
| Workflow Decomposition | 80% | ✅ Good |

**Overall Compliance**: 86% (B+)

---

## Next Steps

1. Fix the violations listed above
2. Address high-priority recommendations
3. Re-run Kiro code review
4. Consider adding ArchUnit tests to enforce these principles
```

## Usage Examples

### Invoke via CLI
```bash
claude-code review --agent=kiro-code-reviewer --files="app/src/main/java/**/*.java"
```

### Invoke via Prompt
```
Review the following code for Kiro compliance:
[paste code here]
```

### Invoke Automatically
```yaml
# .github/workflows/kiro-review.yml
on: [pull_request]
jobs:
  kiro-review:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Kiro Code Review
        run: |
          claude-code review \
            --agent=kiro-code-reviewer \
            --files="${{ github.event.pull_request.changed_files }}" \
            --output=review-report.md
      - name: Comment on PR
        uses: actions/github-script@v6
        with:
          script: |
            const fs = require('fs');
            const report = fs.readFileSync('review-report.md', 'utf8');
            github.rest.issues.createComment({
              issue_number: context.issue.number,
              owner: context.repo.owner,
              repo: context.repo.repo,
              body: report
            });
```

## Configuration

The agent can be configured via `.claude/agents/kiro-code-reviewer.json`:

```json
{
  "agent_type": "kiro-code-reviewer",
  "enabled": true,
  "auto_invoke_on": [
    "code-generation-complete",
    "before-commit"
  ],
  "review_scope": {
    "include_patterns": [
      "app/src/main/java/**/*.java",
      "app/src/test/java/**/*.java"
    ],
    "exclude_patterns": [
      "**/generated/**",
      "**/test/fixtures/**"
    ]
  },
  "severity_thresholds": {
    "fail_on_violations": false,
    "warn_on_score_below": 80,
    "fail_on_score_below": 60
  },
  "principle_weights": {
    "idempotency": 5,
    "stateless": 5,
    "immutability": 4,
    "boundary_control": 5,
    "workflow_decomposition": 4
  },
  "metrics": {
    "max_method_lines": 50,
    "max_class_lines": 300,
    "min_test_coverage": 80
  }
}
```

## Benefits

1. **Automated Quality Gates**: Catch Kiro violations before code review
2. **Consistent Standards**: Enforce same principles across codebase
3. **Learning Tool**: Helps developers understand Kiro principles
4. **Time Savings**: Reduces manual code review time by 40-60%
5. **Proactive**: Catches issues early in development cycle

## Integration with Other Agents

The Kiro Code Reviewer can work alongside:

- **Architecture Validator**: Ensures architectural patterns (DDD, Hexagonal)
- **Test Generator**: Generates tests for Kiro-compliant code
- **Documentation Generator**: Creates documentation highlighting Kiro patterns
- **Refactoring Agent**: Suggests refactorings to improve Kiro compliance
