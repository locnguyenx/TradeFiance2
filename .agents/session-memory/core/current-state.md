# Current State - Trade Finance Enum Synchronization
**Last Update:** 2026-05-11

## Goal
Resolve persistent 400 "record does not exist [23506]" referential integrity errors in the LC issuance workflow by synchronizing frontend enumeration constants with Moqui backend seed data.

## Approach
1.  **Enum Standardization**: Audit and refactor all frontend components (`IssuanceStepper.tsx`, `InstrumentDetails.tsx`, `SettlementForm.tsx`) to utilize correctly prefixed enum IDs as defined in the backend (e.g., `LCT_`, `CHG_`, `AVB_`, `AW_`, `APR_`, `RMB_`, `MARG_`).
2.  **Seed Data Enrichment**: Supplement missing enumeration definitions in `TradeFinanceSeedData.xml` (e.g., `MARG_NONE`) to ensure the backend can validate all valid frontend options.
3.  **Backend Synchronization**: Force-load updated seed data and restart Moqui to ensure the database state matches the code definitions.
4.  **E2E Validation**: Use Playwright (`IssuanceFlow.spec.ts`) to confirm successful end-to-end record persistence and submission.

## Steps Completed
- [x] Standardized all major LC field enums in frontend components.
- [x] Updated unit tests (`IssuanceStepper.test.tsx`, `ProductCatalogManager.test.tsx`) to align with new mappings.
- [x] Identified and added missing `MARG_NONE` and other `TradeMarginType` enums to backend seed data.
- [x] Verified full issuance lifecycle with 100% pass rate in Playwright E2E.

## Current Status
The LC issuance workflow is now stable and fully synchronized with the Moqui backend's referential integrity requirements. The 400 errors during draft saving and submission have been resolved.

## Next Failure to Work On
Resuming the broader `TradeFinanceMoquiSuite` stabilization if any logic-specific failures remain in backend Spock tests.