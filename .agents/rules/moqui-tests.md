---
trigger: always_on
---

# Moqui Testing Constraints

## Test Writing Standards
* **Framework:** All tests MUST be written using the Spock framework (Groovy).
* **Package Hygiene:** Ensure all Specs and Suites are placed in the correct package (e.g., `package trade`).

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