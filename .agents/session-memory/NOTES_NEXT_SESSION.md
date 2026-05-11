# Notes for Next Session
**Date:** 2026-05-10

## Context
We are mid-way through a comprehensive stabilization of the `TradeFinanceMoquiSuite`. Widespread PK collisions have been mitigated by assigning unique sequence ranges to 17 major specs.

## Next Steps
1.  **Migrate Remaining Specs**: Continue refactoring the ~15 remaining specs in `trade.TradeFinanceMoquiSuite` to use the `tempSetSequencedIdPrimary` range pattern.
    -   Target files: `ComplianceServicesSpec`, `NostroApiSpec`, `AuthorizationDataLossSpec`, `InstrumentDataIntegritySpec`, etc.
    -   Use non-overlapping ranges (e.g., 4,500,000, 4,000,000, etc. - check existing ranges to avoid overlaps).
2.  **Fix NPE in TransactionIssuanceBugSpec**: Resolve the `NullPointerException` at line 103 identified in the last suite run.
3.  **Final Verification**: Run `./gradlew reloadSave :runtime:component:TradeFinance:test --tests trade.TradeFinanceMoquiSuite` and aim for 0 failures.

## Open Issues
- `TransactionIssuanceBugSpec` is failing with NPE after refactoring.
- Minor specs still cause secondary collisions because they share the global ID sequence.
