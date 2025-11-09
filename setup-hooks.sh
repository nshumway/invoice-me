#!/bin/bash

# Setup Git Hooks for InvoiceMe
# Run this once after cloning the repository

set -e

GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}Setting up Git hooks...${NC}\n"

# Check if .git directory exists
if [ ! -d ".git" ]; then
    echo "Error: Not in a Git repository"
    exit 1
fi

# Copy pre-commit hook
if [ -f ".git/hooks/pre-commit" ]; then
    echo "Pre-commit hook already exists. Creating backup..."
    cp .git/hooks/pre-commit .git/hooks/pre-commit.backup
fi

cat > .git/hooks/pre-commit << 'EOF'
#!/bin/bash

# Pre-commit hook for InvoiceMe
# Validates backend compilation, frontend build, and code quality

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}🔍 Running pre-commit checks...${NC}\n"

# Track if any checks fail
FAILED=0

# Function to print status
print_status() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓${NC} $2"
    else
        echo -e "${RED}✗${NC} $2"
        FAILED=1
    fi
}

# 1. Check Backend Compilation
echo -e "${BLUE}Checking backend compilation...${NC}"
mvn compile -q > /dev/null 2>&1
print_status $? "Backend compilation"

# 2. Check Frontend Build
echo -e "${BLUE}Checking frontend build...${NC}"
cd invoice-me-frontend
npm run build > /dev/null 2>&1
BUILD_STATUS=$?
cd ..
print_status $BUILD_STATUS "Frontend build"

# 3. Check TypeScript Types
echo -e "${BLUE}Checking TypeScript types...${NC}"
cd invoice-me-frontend
npx tsc --noEmit > /dev/null 2>&1
TS_STATUS=$?
cd ..
print_status $TS_STATUS "TypeScript type check"

# 4. Run ESLint
echo -e "${BLUE}Running ESLint...${NC}"
cd invoice-me-frontend
npx eslint . --ext ts,tsx --max-warnings=0 > /dev/null 2>&1
ESLINT_STATUS=$?
cd ..
print_status $ESLINT_STATUS "ESLint validation"

# 5. Check Prettier Formatting
echo -e "${BLUE}Checking code formatting...${NC}"
cd invoice-me-frontend
npx prettier --check "src/**/*.{ts,tsx,css}" > /dev/null 2>&1
PRETTIER_STATUS=$?
cd ..
print_status $PRETTIER_STATUS "Prettier formatting"

# Final result
echo ""
if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All pre-commit checks passed!${NC}"
    exit 0
else
    echo -e "${RED}✗ Some checks failed. Please fix the errors before committing.${NC}"
    echo ""
    echo -e "${YELLOW}To fix formatting issues, run:${NC}"
    echo "  cd invoice-me-frontend && npm run format"
    echo ""
    echo -e "${YELLOW}To see detailed errors, run checks manually:${NC}"
    echo "  mvn compile"
    echo "  cd invoice-me-frontend && npm run build"
    echo "  cd invoice-me-frontend && npm run type-check"
    echo "  cd invoice-me-frontend && npm run lint"
    exit 1
fi
EOF

chmod +x .git/hooks/pre-commit

echo -e "${GREEN}✓${NC} Pre-commit hook installed successfully!"
echo ""
echo "The hook will run these checks before each commit:"
echo "  • Backend compilation"
echo "  • Frontend build"
echo "  • TypeScript type checking"
echo "  • ESLint validation"
echo "  • Prettier formatting"
echo ""
echo "To skip the hook (not recommended), use: git commit --no-verify"
