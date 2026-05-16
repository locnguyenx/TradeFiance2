# Getting Started Guide: Digital Trade Finance

This guide provides the necessary steps to set up, run, and access the Digital Trade Finance platform for development and local evaluation.

---

## 1. Prerequisites
Ensure the following are installed:
- **OpenJDK 17+**
- **Node.js 18+**
- **Moqui Framework 3.0+**

---

## 2. Backend Setup
1.  **Seed Data**: `./gradlew load`
2.  **Run**: `./gradlew run`
3.  **Port**: `http://localhost:8080`

---

# Developer Guide: Digital Trade Finance

## 1. Architectural Principles
The platform is built on **Moqui Framework**, utilizing a headless service architecture for high-throughput transactional integrity.

### Namespace Convention
- `trade.common`: Shared entities (Facilities, FX Rates, Party Config).
- `trade.importlc`: Import LC domain logic and `NostroReconciliation` entity.
- **Junction-Based Party Model**: The platform uses `TradeInstrumentParty` to normalize all party role assignments (Applicant, Beneficiary, Advising Bank) across instruments, rather than flat fields.
- **Transaction-Centric Model**: The platform decouples "Proposed State" (`TradeTransaction`) from "Current State" (`TradeInstrument`). All workflow actions Target `transactionId` rather than `instrumentId`. To enable efficient "Dual-Status" reporting, the `TradeInstrument` entity maintains a `latestTransactionId` reference, allowing the UI to join both the legal state and the active transaction status in a single query.

### Service Hardening
All state-changing services must adhere to the following guards:
1.  **Enforce Maker/Checker**: Implemented via `trade.AuthorizationServices`. Status transitions for `LC_ISSUED` are blocked until `INST_APPROVED`.
2.  **Validate SWIFT**: Strict regex validation for X/Z character sets and BIC codes via `trade.importlc.ImportLcValidationServices`. Z-set validation (Tags 73, 72Z, 77A) allows extended characters like `@`, `#`, `<` and `>` using the `format#ZCharacter` service.
3.  **Facility Utilization**: Atomic limit check-and-earmark logic in `LimitServices.xml` to prevent over-drawing.
4.  **Transaction Audit Hub**: Automatic logging in `TradeTransaction` and `TradeTransactionAudit`. The frontend merges these into a **Unified Narrative Timeline** via the `getInstrumentTransactions` and `getAuditLogs` API end-points.
5.  **Party Eligibility Gateway**: `TradeCommonServices.assign#InstrumentParty` enforces KYC status, RMA requirements, FI Limits, and party-type matching before any role assignment is persisted. This is the single compliance checkpoint for all party operations.
6.  **Identity Hub**: Centralized authentication and role-based session management via `trade.UserAccountServices`. Enforces transactional auditing for all security events (password changes, profile updates).
7.  **Immutability Guard**: `trade.importlc.ImportLcServices.update#ImportLetterOfCredit` enforces strict immutability for issued LCs. Manual modification of financial terms (amount, expiry) is blocked. These fields must only be updated by the system-level `AuthorizationServices.authorize#Instrument` flow using the `skipImmutabilityGuard: true` bypass flag, ensuring all changes are grounded in an approved transaction.
8.  **Smart Delta Architecture (SRG 2024)**: To support UCP 600 compliant amendments, the platform uses a structured delta model (`ImportLcAmendment` entity). Narratives are not overwritten; instead, `goodsDeltaText`, `docsDeltaText`, and `conditionsDeltaText` capture precise `ADD`, `DELETE`, or `REPLACE` actions. These deltas are merged into the Master LC only upon recorded **Beneficiary Consent** (ACCEPTED).
9.  **Automated Reimbursement (SRG 2024)**: `trade.SwiftGenerationServices.generate#Mt740` and `generate#Mt747` are triggered by SECA hooks on issuance/amendment authorization. These services also auto-create `NostroReconciliation` records to ensure end-to-end tracking of bank debits.
10. **Tag 77J Aggregate Validation**: `trade.importlc.ImportLcValidationServices.validate#Tag77JAggregateLimit` enforces a hard limit of 70 lines (at 50 characters each) for the total discrepancy text in MT 750/734 messages.
11. **Inbound SWIFT Processing Engine**: Built with Prowide Core to parse inbound messages. The engine deduplicates via SHA-256 hash and performs Tag 21 correlation to route messages to the Trade Inbox, auto-spawning specific actions for MT 730, 799, 750, 754, and 742.

### SWIFT Generation Data Flow
SWIFT message builders (MT700, MT707) read party data exclusively from the `TradeInstrumentParty` junction:
1. Query junction by `instrumentId` and `roleEnumId` (e.g., `TP_ADVISING_BANK`).
2. Join to `TradePartyView` to resolve `swiftBic`, `partyName`, `registeredAddress`.
3. Determine SWIFT tag format at runtime: Option A (BIC available), Option D (Name/Address fallback), or Option C (Clearing Code).
4. **Account Number rules**: Forbidden on Tags 41a and 42a. Optional on most MT700 tags. Mandatory on MT103 Tag 59.
5. **Available With (Tag 41a)**: Reads `availableWithEnumId` from `ImportLetterOfCredit`. If `AVAIL_ANY_BANK`, outputs literal "ANY BANK". If `AVAIL_SPECIFIC_BANK`, reads from `TP_NEGOTIATING_BANK` junction.

---

## 2. Frontend State & Feedback
The frontend uses a provider-based architecture for core services:
- **`AuthContext`**: Manages user sessions, profile hydration, and granular RBAC (`hasRole` guard).
- **`ToastContext`**: Provides a non-intrusive, global notification system for operational feedback.
- **`GlobalShell`**: Implements contextual navigation and role-based route guards (e.g. `/admin` restriction).

---

## 3. Technical Stack
- **Backend**: Moqui Framework 3.0 (Groovy/Java).
- **Frontend**: Next.js 14 (TypeScript / Vanilla CSS / premium tokens).
- **Communication**: REST API (v1 / trade namespace).
    - **Amendment Creation**: `POST /import-lc/{id}/amendment/external` (Customer-facing MT 707) and `POST /import-lc/{id}/amendment/internal` (Bank-only adjustments).
    - **Authorization**: `POST /import-lc/{id}/amendment/{amdId}/authorize` (Enforces Maker/Checker).
- **Verification**: Playwright E2E and Spock Integration Tests.

---

## 3. Regression Suite
The backend suite is unified under `trade.TradeFinanceMoquiSuite`.
The frontend suite uses Playwright (frontend/e2e) and Vitest for unit tests.

### Execution (Backend)
```bash
# Run the core functional suite
./gradlew :runtime:component:TradeFinance:test --tests trade.TradeFinanceMoquiSuite

# Run the hardened business process suite (Facilities, Fees, Settlements)
./gradlew :runtime:component:TradeFinance:test --tests trade.TradeFinanceHardeningSpec
```

### Execution (Frontend Unit Tests)
```bash
cd frontend && npm test src/context/AuthContext.test.tsx
cd frontend && npm test src/components/GlobalShell.test.tsx
```

### Execution (Frontend E2E Tests)
```bash
cd frontend && npx playwright test
```

---

## 4. SWIFT Character Sets & Mappings
The platform validates input strictly against SWIFT MT7xx standards:
- **X-Charset**: `A-Z a-z 0-9 / - ? : ( ) . , ' + space`. (Standard narratives: 45A, 46A).
- **Z-Charset**: Extended set (`@ # = ! " % & * ; < > _`) allowed in Tag 73, 72Z, and 77A. Implemented via the `trade.SwiftGenerationServices.format#ZCharacter` service.
- **Line Enforcement**: Narrative fields are proactively checked for line-count compliance. **Tag 77J** (Discrepancies) is strictly limited to an **aggregate of 70 lines** (50 characters per line) across all logged discrepancies.
- **Utilities**: Centralized validation in `frontend/src/utils/SwiftUtils.ts` and backend `SwiftUtilsServices.xml`.

---

## 5. Troubleshooting & Maintenance
- **Data Seeding**: Use `./gradlew load` to refresh master data.
- **Log Analysis**: Business rule failures are logged with `[TRADE-AUTH]` or `[TRADE-VAL]` prefixes.
- **UI Stabilization**: When UI labels change, update the "Blue Premium" tokens and verify navigation integrity.

---

## Conclusion
Maintain transactional integrity above all. Ensure all frontend changes mirror backend validation constraints to enable "Clean at Capture" processing.
