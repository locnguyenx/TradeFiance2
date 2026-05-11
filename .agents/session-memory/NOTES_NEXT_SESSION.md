# Notes for Next Session
**Date:** 2026-05-11

## Context
We have successfully resolved the 400 Referential Integrity errors in the LC issuance workflow. The frontend and backend are now fully synchronized regarding enumeration constants.

## Next Steps
1.  **Monitor E2E Tests**: Ensure no new regressions surface in `IssuanceFlow.spec.ts` as more features are added.
2.  **Backend Spec Stabilization**: Resume the refactoring of the remaining ~15 backend specs to ensure a 100% pass rate in `TradeFinanceMoquiSuite`.
    -   Target: `ComplianceServicesSpec`, `NostroApiSpec`, etc.
    -   Address the NPE in `TransactionIssuanceBugSpec`.
3.  **UI Polish**: The `InstrumentDetails.tsx` display logic now handles prefixed enums, but consider adding a generic utility for enum-to-label transformation if more types are added.

## Open Issues
- `TransactionIssuanceBugSpec` still has an NPE at line 103.
- Backend suite still has logic-specific failures unrelated to ID collisions.
