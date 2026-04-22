# Journal: Frontend Component Hardening (2026-04-22)

## Context
Goal was to expand the React unit test suite to cover critical business logic, form validation, and API error states in the `ImportLcDashboard` and `IssuanceStepper` components.

## Findings & Lessons Learned

### 1. Async State Leaks in Jest/React
- **Issue**: Tests that render components with `useEffect` (like `ImportLcDashboard`) can leak state updates if they finish before background promises resolve. This causes `Cannot log after tests are done` or "act" warnings.
- **Solution**: Always use `waitFor` or `await screen.findBy...` to ensure the component has fully "settled" before finishing a test case. Even simple rendering tests should wait for the settling event.

### 2. Jest/Playwright Conflict
- **Conflict**: Jest by default may pick up Playwright spec files (`*.spec.ts`) if they share the same project root. Since Playwright uses its own global `describe` and `test` functions, this causes runtime errors in Jest.
- **Decision**: Updated `jest.config.ts` with `testPathIgnorePatterns: ['<rootDir>/tests/']` to strictly isolate unit and E2E testing environments.

### 3. Component Hardening Patterns
- **Dynamic Table Mapping**: Moving from hardcoded dashboard placeholders to dynamic mapping (`lcs.map(...)`) revealed and resolved issues with empty list rendering.
- **Form State Persistence**: Verifying that `IssuanceStepper` preserves state between back/forth transitions ensured that local React state is robustly managed during the multi-step workflow.

### 4. Mocking tradeApi
- Initial tests were too shallow. Adding `jest.mock('../api/tradeApi')` with specific mocked values for each test case (loading vs. error vs. data) provided the necessary coverage for edge cases like system unavailability.

---
*OpenCode*
