# Current State - Inbound SWIFT Processing Engine
**Last Update:** 2026-05-16

## Goal
Finalize the Inbound SWIFT Processing Engine for production readiness by resolving test failures, integrating automated workflows, and verifying system-wide logic separation.

## Approach
1.  **Architecture Refactoring**: Decoupled `InboundCorrelationServices.xml` from inline business logic by redirecting message triggers (MT730, MT799, MT750, MT754, MT742) to specialized `<service-call>`s in `InboundActionServices.xml`.
2.  **Archiving vs Deletion**: Rewrote `InboundSwiftServices.xml` polling logic to utilize `TradeConfig` and archive SWIFT files instead of deleting them `file.delete()`, ensuring SOX compliance.
3.  **Test Migration**: Migrated the functional assertions from `InboundSwiftSpec.groovy` to the new `InboundActionSpec.groovy` and adjusted `sourceChannel` to test manual and batch ingestion.
4.  **Debugging MT 730 Failure**: Resolved the intermittent `[23506]` referential integrity test failure. Diagnosed that Moqui silently rolled back the `TradeInstrument` insertion because the `advisingBank` mock lacked an active RMA. Added `hasActiveRMA: true` to the test setup.
5.  **Documentation**: Updated the `docs/tcd/TestCoverageMatrix.md` and `docs/user-guide/EnduserGuide.md` to reflect new operations.

## Steps Completed
- [x] Refactored `InboundCorrelationServices.xml` and `InboundActionServices.xml`.
- [x] Updated polling/archiving in `InboundSwiftServices.xml`.
- [x] Debugged and fixed `InboundActionSpec.groovy`.
- [x] Full `TradeFinanceMoquiSuite` suite passed.
- [x] Created `TestCoverageMatrix.md` and documented missing UI elements (`DbResourceFile`).
- [x] Updated user guides to include automated workflows.

## Current Status
The Inbound SWIFT Processing Engine is now fully stable, verified against all edge cases, and documentation has been updated to reflect the new automated processing paradigms.

## Next Failure to Work On
No current failure. Feature branch `feature/inbound-swift` is complete and ready to be merged.