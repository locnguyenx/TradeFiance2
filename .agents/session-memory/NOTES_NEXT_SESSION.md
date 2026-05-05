# Notes for Next Session
**Date:** 2026-05-06

## Context
We have just stabilized the SWIFT generation workflows and hardened the core test suite. The system is in a clean state with all major specs passing.

## Instructions for Next Session
1.  **Continue Test Refactoring**: Follow the plan in `implementation_plan.md` to finish refactoring the remaining backend tests to use official services (e.g., `BddImportLcModuleSpec`, `ComplianceServicesSpec`).
2.  **Monitor UI Stability**: Verify that the "Issue LC" and "Amend LC" flows in the UI are also benefiting from the new `SwiftUtilsServices`.
3.  **Clean up redundant files**: If no further issues are found, consider removing the `runtime/log/*.log` files and `runtime/txlog/*.tlog` to keep the environment fresh.

## Critical Warnings
- **Database Locks**: If the whole test suite fails with `InitializationException` or `IOException`, it's likely a lock held by a background Gradle daemon or the `moqui.war` server. Run `./gradlew --stop` before retrying.
- **SWIFT Charset**: Always use the `cleanValue` logic in `SwiftUtilsServices` when adding new fields to ensure compliance with the SWIFT X Character Set.
- **Entity Fields**: Do not assume `applicableRulesEnumId` exists on the `ImportLetterOfCredit` entity unless it is explicitly added back to the schema; the current generation logic uses a hardcoded "UCP LATEST VERSION" to match test expectations.
