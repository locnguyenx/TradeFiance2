---
trigger: always_on
globs: **/src/test/**/*
---

# Testing, Issue tracking & Debugging Rules
## Testing

- ALL TEST FAILURES ARE YOUR RESPONSIBILITY, even if they're not your fault. The Broken Windows theory is real.
- Never delete a test because it's failing. Instead, raise the issue with Loc. 
- Tests MUST comprehensively cover ALL functionality. 
- YOU MUST NEVER write tests that "test" mocked behavior, including hardcoding. If you notice tests that test mocked behavior instead of real logic, you MUST stop and warn Loc about them.
- YOU MUST NEVER implement mocks in end to end tests. We always use real data and real APIs.
- YOU MUST NEVER ignore system or test output - logs and messages often contain CRITICAL information.
- Test output MUST BE PRISTINE TO PASS. If logs are expected to contain errors, these MUST be captured and tested. If a test is intentionally triggering an error, we *must* capture and validate that the error output is as we expect

## Issue tracking

- You MUST use your TodoWrite tool to keep track of what you're doing 
- You MUST NEVER discard tasks from your TodoWrite todo list without Loc's explicit approval

## Systematic Debugging Process

YOU MUST ALWAYS find the root cause of any issue you are debugging
YOU MUST NEVER fix a symptom or add a workaround instead of finding a root cause, even if it is faster or I seem like I'm in a hurry.

YOU MUST follow this debugging framework for ANY technical issue:

### Phase 1: Root Cause Investigation (BEFORE attempting fixes)
- **Read Error Messages Carefully**: Don't skip past errors or warnings - they often contain the exact solution
- **Reproduce Consistently**: Ensure you can reliably reproduce the issue before investigating
- **Check Recent Changes**: What changed that could have caused this? Git diff, recent commits, etc.

### Phase 2: Pattern Analysis
- **Find Working Examples**: Locate similar working code in the same codebase
- **Compare Against References**: If implementing a pattern, read the reference implementation completely
- **Identify Differences**: What's different between working and broken code?
- **Understand Dependencies**: What other components/settings does this pattern require?

### Phase 3: Hypothesis and Testing
1. **Form Single Hypothesis**: What do you think is the root cause? State it clearly
2. **Test Minimally**: Make the smallest possible change to test your hypothesis
3. **Verify Before Continuing**: Did your test work? If not, form new hypothesis - don't add more fixes
4. **When You Don't Know**: Say "I don't understand X" rather than pretending to know

### Phase 4: Implementation Rules
- ALWAYS have the simplest possible failing test case. If there's no test framework, it's ok to write a one-off test script.
- NEVER add multiple fixes at once
- NEVER claim to implement a pattern without reading it completely first
- ALWAYS test after each change
- IF your first fix doesn't work, STOP and re-analyze rather than adding more fixes

# Moqui Testing Constraints

## Test Writing Standards
* **Framework:** All tests MUST be written using the Spock framework (Groovy).
* **Package Hygiene:** Ensure all Specs and Suites are placed in the correct package (e.g., `package moqui.trade.finance`).

## The Knowledge Base Pointer (CRITICAL)
* Before writing or modifying any test assertions, UI screen tests, or mock data setups, you MUST read the exact testing patterns and traps defined in `.agents/knowledge/moqui-testing.md`.
* Pay special attention to the rules regarding **Type Coercion Traps**, **Resilient Assertions** (avoiding exact size checks), and **Sequence Collisions** documented in that file.


---

## Test Structure

```
src/test/groovy/
��── moqui/trade/finance/
    ├── TradeFinanceSuite.groovy           # Aggregator
    ├── TradeFinanceServicesSpec.groovy    # Service tests
    ├── TradeFinanceScreensSpec.groovy    # Screen tests
    ├── TradeFinanceLifecycleSpec.groovy # Status transitions
    └── ...
```