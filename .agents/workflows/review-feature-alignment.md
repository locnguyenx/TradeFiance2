---
description: Use when requested for review a tasks, implementing features to verify work meets requirements
---

# Requesting Feature Review

**Core principle:** Comply the specs and implementation plan.

## How to Request

**1. Get git SHAs:**
```bash
BASE_SHA=$(git rev-parse HEAD~1)  # or origin/main
HEAD_SHA=$(git rev-parse HEAD)
```

**2. Dispatch the Code Review Agent:**

Use Task tool with code-reviewer type.

**Placeholders:**
- `{FEATURE_DESCRIPTION}` - feature requested by Human partner
- `{WHAT_WAS_IMPLEMENTED}` - What you just built related to {FEATURE_DESCRIPTION}
- `{PLAN_SPECS_OR_REQUIREMENTS}` - What it should do
- `{BASE_SHA}` - Starting commit
- `{HEAD_SHA}` - Ending commit

**3. Act on feedback:**
- Fix Critical issues immediately
- Fix Important issues before proceeding
- Note Minor issues for later
- Push back if reviewer is wrong (with reasoning)

## Red Flags

**Never:**
- Skip review because "it's simple"
- Ignore Critical issues
- Proceed with unfixed Important issues
- Argue with valid technical feedback

**If reviewer wrong:**
- Push back with technical reasoning
- Show code/tests that prove it works
- Request clarification

# Code Review Agent

You are reviewing code changes for production readiness.

**Your task:**
1. Review {WHAT_WAS_IMPLEMENTED} of {FEATURE_DESCRIPTION}
2. Compare against {PLAN_SPECS_OR_REQUIREMENTS}
3. Check design & plan alignment, code quality, architecture, testing
4. Categorize issues by severity
5. Assess production readiness

## What Was Implemented

{DESCRIPTION}

## Requirements/Specs/Plan

{PLAN_REQUIREMENTS}

## Git Range to Review

**Base:** {BASE_SHA}
**Head:** {HEAD_SHA}

```bash
git diff --stat {BASE_SHA}..{HEAD_SHA}
git diff {BASE_SHA}..{HEAD_SHA}
```

## Review Checklist
**Design & Plan alignment**
   1. The "Atomic Mapping" Prompt (Best for UI/Data Entry)
   "Perform an Atomic Mapping Review of the [Component Name] against the [BRD/Design Spec]. Audit every single field in the code for existence, type, and Enum alignment. Ignore the current test status, which may be a "Passing Test" Trap, and look for any missing fields or hardcoded shortcuts that don't match the spec."

   2. The "Traceability Audit" Prompt (Best for Feature Completeness)
   "Run a Traceability Audit on the [Implementation Plan]. For every 'completed' task in our checklist, prove that it exists in the codebase without hardcoding. If a field or logic branch from the plan is missing or was 'short-circuited,' flag it as a discrepancy."

   3. The "Production Hardening" Prompt (Best for removing shortcuts)
   "Perform a Hardcoding & Bias Audit on the [Package/Directory]. Search for any hardcoded IDs (like ACME_CORP_001), mock-like testing behaviors in production logic, or Enum mismatches. I need a list of every 'magic string' that should be dynamic."

**Code Quality:**
- Clean separation of concerns?
- Proper error handling?
- Type safety (if applicable)?
- DRY principle followed?
- Edge cases handled?
- ZERO hardcoding

**Architecture:**
- Sound design decisions?
- Scalability considerations?
- Performance implications?
- Security concerns?

**Testing:**
- Tests actually test logic (not mocks)?
- Edge cases covered?
- Integration tests where needed?
- All tests passing?
- Is there a "Passing Test" Trap? Avoid the heuristic that "passing tests = correct implementation", need the completeness of the BRD field mapping and BDD actual behavior testing.

**Requirements:**
- All plan requirements met?
- Implementation matches spec?
- No scope creep?
- Breaking changes documented?

**Production Readiness:**
- Migration strategy (if schema changes)?
- Backward compatibility considered?
- Documentation complete?
- No obvious bugs?

## Output Format

### Strengths
[What's well done? Be specific.]

### Issues

#### Critical (Must Fix)
[Bugs, security issues, data loss risks, broken functionality]

#### Important (Should Fix)
[Architecture problems, missing features/entity fields/UI elements/validations/processings, poor error handling, test gaps]

#### Minor (Nice to Have)
[Code style, optimization opportunities, documentation improvements]

**For each issue:**
- File:line reference
- What's wrong
- Why it matters
- How to fix (if not obvious)

### Recommendations
[Improvements for code quality, architecture, or process]

### Assessment

**Ready to merge?** [Yes/No/With fixes]

**Reasoning:** [Technical assessment in 1-2 sentences]

## Critical Rules

**DO:**
- Categorize by actual severity (not everything is Critical)
- Be specific (file:line, not vague)
- Explain WHY issues matter
- Acknowledge strengths
- Give clear verdict

**DON'T:**
- Say "looks good" without checking
- Mark nitpicks as Critical
- Give feedback on code you didn't review
- Be vague ("improve error handling")
- Avoid giving a clear verdict
- Surface-Level Verification

## Example Output

```
### Strengths
- Clean database schema with proper migrations (db.ts:15-42)
- Comprehensive test coverage (18 tests, all edge cases)
- Good error handling with fallbacks (summarizer.ts:85-92)

### Issues

#### Important
1. **Missing help text in CLI wrapper**
   - File: index-conversations:1-31
   - Issue: No --help flag, users won't discover --concurrency
   - Fix: Add --help case with usage examples

2. **Date validation missing**
   - File: search.ts:25-27
   - Issue: Invalid dates silently return no results
   - Fix: Validate ISO format, throw error with example

#### Minor
1. **Progress indicators**
   - File: indexer.ts:130
   - Issue: No "X of Y" counter for long operations
   - Impact: Users don't know how long to wait

### Recommendations
- Add progress reporting for user experience
- Consider config file for excluded projects (portability)

### Assessment

**Ready to merge: With fixes**

**Reasoning:** Core implementation is solid with good architecture and tests. Important issues (help text, date validation) are easily fixed and don't affect core functionality.
```
