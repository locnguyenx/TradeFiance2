---
command: "doc-generate"
description: "Generate test report, release notes, and user guide after TDD completion"
---
# Documentation Generation Command

## Overview
Generate comprehensive documentation (test report, release notes, user guide) after TDD implementation completes.

## Usage
```
/doc-generate
```

## Pre-requisites
- TDD workflow must have completed
- Test results must be available in `build/test-results/test/`
- Feature plan files must exist in `.opencode/plans/[feature-name]/`

## Execution Steps

### Step 1: Create Reports Directory
Create the reports directory for the feature:
```bash
mkdir -p .opencode/plans/[feature-name]/reports
```

### Step 2: Generate Test Report
Execute using @general agent:

```
@general Generate a test report for [FEATURE NAME] based on test results in build/test-results/test/

Include:
- Test suite name and description
- Total tests run
- Passed/Failed counts
- Execution time
- Any failing tests with error details
- Coverage summary if available

Reference:
- BDD scenarios: .opencode/plans/[feature-name]/bdd-scenarios.md

Output: .opencode/plans/[feature-name]/reports/test-report.md
```

### Step 3: Generate Release Notes
Execute using @general agent:

```
@general Generate release notes for [FEATURE NAME] based on:

1. Technical Discovery: .opencode/plans/[feature-name]/04-technical-discovery.md
2. Implementation changes made during TDD
3. New entities, services, screens created/modified

Include:
- Feature summary
- New entities (with field descriptions)
- New services (with verb#noun naming)
- Modified services
- New screens
- Configuration changes
- Breaking changes (if any)
- Migration steps (if needed)

Output: .opencode/plans/[feature-name]/reports/release-notes.md
```

### Step 4: Generate User Guide
Execute using @general agent:

```
@general Generate user guide for [FEATURE NAME] based on:

1. Business Requirements: .opencode/plans/[feature-name]/01-business-requirements.md
2. Technical Discovery: .opencode/plans/[feature-name]/04-technical-discovery.md
3. Process Flow: .opencode/plans/[feature-name]/03-future-state.md

Include:
- Feature overview
- User interactions (step-by-step)
- Screen descriptions
- Input/Output field definitions
- Business rules applied
- Edge case handling
- Examples (happy path)
- Configuration steps

Output: .opencode/plans/[feature-name]/reports/user-guide.md
```

## Alternative: Single Command Execution

You can also invoke all three sequentially in one session:

```
@general Generate documentation for [FEATURE NAME]:
1. Test report from build/test-results/test/ → .opencode/plans/[feature-name]/reports/test-report.md
2. Release notes from 04-technical-discovery.md → .opencode/plans/[feature-name]/reports/release-notes.md  
3. User guide from requirements and technical discovery → .opencode/plans/[feature-name]/reports/user-guide.md
```

## Output Files

After execution, the following files will be created:

```
.opencode/plans/[feature-name]/reports/
├── test-report.md      # Test execution results
├── release-notes.md    # Changes and versioning
└── user-guide.md      # End-user documentation
```

## Agent Recommendation

Use **@general** agent for documentation generation because:
- Full tool access (write, edit, bash)
- Can orchestrate multiple tasks
- Best for complex multi-step documentation

## Notes

- Adjust `[feature-name]` to match your actual feature directory name
- Test report generation requires tests to have been run
- User guide requires both business requirements and technical discovery to be complete
- Release notes should be generated after all implementation is done
