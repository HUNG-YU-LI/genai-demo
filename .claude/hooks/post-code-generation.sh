#!/bin/bash
# Kiro Post-Code-Generation Hook
# Runs after Claude generates code to validate Kiro principles

set -e

echo "🔍 [Kiro Hook] Post-code generation validation..."

# Check if we're in a code generation context
if [ -z "$CLAUDE_GENERATED_FILES" ]; then
    echo "⚠️  [Kiro Hook] No generated files to validate"
    exit 0
fi

echo "📁 [Kiro Hook] Validating generated files: $CLAUDE_GENERATED_FILES"

# Function to check for Kiro violations
check_kiro_violations() {
    local file=$1
    local violations=()

    if [ ! -f "$file" ]; then
        return 0
    fi

    # Only check Java files
    if [[ ! "$file" =~ \.java$ ]]; then
        return 0
    fi

    echo "   Checking: $file"

    # Check for mutable instance fields (anti-pattern for stateless)
    if grep -q "^[[:space:]]*private.*=.*new.*;" "$file" 2>/dev/null; then
        violations+=("❌ Found mutable instance fields (violates stateless principle)")
    fi

    # Check for missing idempotency handling in API controllers
    if grep -q "@PostMapping\|@PutMapping\|@PatchMapping" "$file" 2>/dev/null; then
        if ! grep -q "idempotency\|Idempotency" "$file" 2>/dev/null; then
            violations+=("⚠️  POST/PUT/PATCH endpoint without idempotency handling")
        fi
    fi

    # Check for setter methods (anti-pattern for immutability)
    if grep -q "public void set[A-Z]" "$file" 2>/dev/null; then
        violations+=("⚠️  Found public setter methods (violates immutability)")
    fi

    # Check for missing input validation in controllers
    if grep -q "@RestController\|@Controller" "$file" 2>/dev/null; then
        if ! grep -q "@Valid\|@Validated" "$file" 2>/dev/null; then
            violations+=("⚠️  Controller without input validation annotations")
        fi
    fi

    # Check for large methods (anti-pattern for decomposition)
    local max_method_lines=50
    awk '
        /^\s*(public|private|protected).*\{/ {
            in_method=1;
            method_start=NR;
            method_name=$0;
        }
        in_method && /^\s*\}/ && NR - method_start > '"$max_method_lines"' {
            print "⚠️  Method too long (" NR - method_start " lines): " method_name;
            in_method=0;
        }
        in_method && /^\s*\}/ { in_method=0; }
    ' "$file" | while read -r line; do
        violations+=("$line")
    done

    # Report violations
    if [ ${#violations[@]} -gt 0 ]; then
        echo "   ⚠️  Kiro violations found in $file:"
        for violation in "${violations[@]}"; do
            echo "      $violation"
        done
        return 1
    else
        echo "   ✅ No Kiro violations found"
        return 0
    fi
}

# Validate each generated file
violation_count=0
IFS=',' read -ra FILES <<< "$CLAUDE_GENERATED_FILES"
for file in "${FILES[@]}"; do
    if ! check_kiro_violations "$file"; then
        ((violation_count++))
    fi
done

echo ""
if [ $violation_count -eq 0 ]; then
    echo "✅ [Kiro Hook] All generated files comply with Kiro principles"
    exit 0
else
    echo "⚠️  [Kiro Hook] Found Kiro violations in $violation_count file(s)"
    echo "   Please review and fix the violations above"
    echo ""
    echo "💡 Quick fixes:"
    echo "   - Add @Valid to controller parameters"
    echo "   - Add idempotency key handling to POST/PUT endpoints"
    echo "   - Replace setters with immutable update methods"
    echo "   - Break down large methods into smaller steps"
    echo ""
    # Don't fail the hook, just warn
    exit 0
fi
