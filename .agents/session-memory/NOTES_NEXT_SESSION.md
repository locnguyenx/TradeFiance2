# Notes for Next Session
**Date:** 2026-05-16

## Context
We have finalized the `feature/inbound-swift` branch. The backend testing suite (`TradeFinanceMoquiSuite`) is fully passing with a 100% success rate, the `InboundActionSpec.groovy` isolated tests for SWIFT messages have been finalized, and all documentation for inbound SWIFT processes has been updated.

## Next Steps
1.  **Merge & Deploy**: Merge `feature/inbound-swift` into the main branch, if not done already.
2.  **UI Implementation**: The `InboundActionServices` references a `DbResourceFile` with `parentResourceId='PRES_{id}'` for document presentation attachments. Ensure the frontend (`PresentationDetails.tsx`) implements the corresponding fetch and display logic.
3.  **Monitor E2E Tests**: Ensure no new regressions surface in E2E tests following the changes to SWIFT message generation and ingestion logic.

## Open Issues
- `TransactionIssuanceBugSpec` might still have an NPE at line 103 from a previous session.
- The UI does not currently support viewing presentation document attachments.
