#!/bin/bash
# Kiro Pre-Code-Generation Hook
# Runs before Claude generates any code to ensure Kiro principles are followed

set -e

echo "🔍 [Kiro Hook] Pre-code generation validation..."

# Check if we're in a code generation context
if [ -z "$CLAUDE_TASK_TYPE" ]; then
    echo "⚠️  [Kiro Hook] CLAUDE_TASK_TYPE not set, skipping validation"
    exit 0
fi

# Only run for code generation tasks
if [[ "$CLAUDE_TASK_TYPE" != "code-generation" && "$CLAUDE_TASK_TYPE" != "refactoring" ]]; then
    echo "ℹ️  [Kiro Hook] Not a code generation task, skipping"
    exit 0
fi

echo "✅ [Kiro Hook] Code generation task detected: $CLAUDE_TASK_TYPE"

# Validate Kiro principles checklist
echo ""
echo "📋 [Kiro Hook] Kiro Principles Checklist:"
echo "   [ ] Idempotency: Will generated code be idempotent?"
echo "   [ ] Stateless: Will handlers be stateless?"
echo "   [ ] Immutability: Will data structures be immutable?"
echo "   [ ] Boundary Control: Will inputs be validated?"
echo "   [ ] Workflow Decomposition: Will complex workflows be decomposed?"
echo ""

# Check if project has Kiro configuration
KIRO_CONFIG=".claude/kiro-config.json"
if [ ! -f "$KIRO_CONFIG" ]; then
    echo "⚠️  [Kiro Hook] Warning: $KIRO_CONFIG not found"
    echo "   Creating default configuration..."

    cat > "$KIRO_CONFIG" << 'EOF'
{
  "kiro_principles": {
    "idempotency": {
      "enabled": true,
      "require_idempotency_key": true,
      "tracking_table": "idempotency_records"
    },
    "stateless": {
      "enabled": true,
      "allow_instance_state": false,
      "require_external_storage": true
    },
    "immutability": {
      "enabled": true,
      "prefer_records": true,
      "defensive_copying": true
    },
    "boundary_control": {
      "enabled": true,
      "validate_inputs": true,
      "sanitize_outputs": true,
      "standardized_errors": true
    },
    "workflow_decomposition": {
      "enabled": true,
      "max_method_lines": 50,
      "single_responsibility": true
    }
  },
  "code_generation_constraints": {
    "max_class_length": 300,
    "max_method_length": 50,
    "require_documentation": true,
    "require_tests": true
  },
  "architectural_patterns": {
    "hexagonal_architecture": true,
    "ddd_tactical_patterns": true,
    "cqrs": false,
    "event_sourcing": false
  }
}
EOF

    echo "✅ [Kiro Hook] Created default Kiro configuration"
fi

# Load configuration
if command -v jq &> /dev/null; then
    IDEMPOTENCY_ENABLED=$(jq -r '.kiro_principles.idempotency.enabled' "$KIRO_CONFIG")
    STATELESS_ENABLED=$(jq -r '.kiro_principles.stateless.enabled' "$KIRO_CONFIG")

    echo "📖 [Kiro Hook] Loaded configuration:"
    echo "   Idempotency: $IDEMPOTENCY_ENABLED"
    echo "   Stateless: $STATELESS_ENABLED"
else
    echo "⚠️  [Kiro Hook] jq not installed, skipping config validation"
fi

# Remind Claude about Kiro principles
echo ""
echo "💡 [Kiro Hook] Reminder: Apply Kiro principles during code generation:"
echo "   1. Make all operations idempotent with idempotency keys"
echo "   2. Keep handlers stateless, store state externally"
echo "   3. Use immutable data structures (records, final classes)"
echo "   4. Validate inputs at boundaries, sanitize outputs"
echo "   5. Decompose complex workflows into single-responsibility steps"
echo ""

echo "✅ [Kiro Hook] Pre-code generation validation complete"
exit 0
