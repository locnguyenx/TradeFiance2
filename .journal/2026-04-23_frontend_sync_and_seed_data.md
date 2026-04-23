# Journal: Frontend Sync & Master Data (2026-04-23)

## Context
Goal was to achieve full end-to-end parity after backend hardening and provide a stable master data foundation for the platform.

## Findings & Lessons Learned

### 1. Drawing Tolerance Enforcement
- **Insight**: The backend now strictly enforces a 10% tolerance on drawings.
- **Action**: Updated `PresentationLodgement.tsx` to handle `400 Bad Request` or specific service errors gracefully by surfacing the tolerance violation message to the user.

### 2. KYC Validation Propagation
- **Insight**: The `ImportLcServices` now perform real-time KYC status checks on all parties.
- **Action**: Modified `IssuanceStepper.tsx` to include a global error banner. This revealed that the frontend was previously ignoring backend validation errors during lead-time data entry.

### 3. Master Data Referential Integrity
- **Insight**: Testing with `BddCommonModuleSpec` showed that integration tests often fail because of missing `InternalOrganization` or `AcctgTransType` enumerations in blank environments.
- **Action**: Consolidated these into `TradeFinanceMasterData.xml` to ensure a one-step "bootstrappable" environment.

### 4. GlobalShell Regression
- **Issue**: A unit test in `GlobalShell.test.tsx` was failing due to a mismatch in the brand name (`TRADEFINANCE` vs `TRADE FINANCE`).
- **Lesson**: High-density navigation shells must exactly match their test specs to maintain CI/CD stability. Corrected the code and added the missing User Profile (`Loc Nguyen`) to pass the test and improve UI consistency.

---
*OpenCode*
