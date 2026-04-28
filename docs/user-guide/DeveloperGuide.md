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
- `trade.importlc`: Import LC domain logic.
- **Transaction-Centric Model**: The platform decouples "Proposed State" (`TradeTransaction`) from "Current State" (`TradeInstrument`). All workflow actions Target `transactionId` rather than `instrumentId`.

### Service Hardening
All state-changing services must adhere to the following guards:
1.  **Enforce Maker/Checker**: Implemented via `trade.AuthorizationServices`. Status transitions for `LC_ISSUED` are blocked until `INST_APPROVED`.
2.  **Validate SWIFT**: Strict regex validation for X/Z character sets and BIC codes via `trade.importlc.ImportLcValidationServices`.
3.  **Facility Utilization**: Atomic limit check-and-earmark logic in `LimitServices.xml` to prevent over-drawing.
4.  **Transaction Audit Hub**: Automatic logging in `TradeTransaction` and `TradeTransactionAudit`. The frontend merges these into a **Unified Narrative Timeline** via the `getInstrumentTransactions` and `getAuditLogs` API end-points.

---

## 2. Technical Stack
- **Backend**: Moqui Framework 3.0 (Groovy/Java).
- **Frontend**: Next.js 14 (TypeScript / Vanilla CSS / premium tokens).
- **Communication**: REST API (v1 / trade namespace).
- **Verification**: Playwright E2E and Spock Integration Tests.

---

## 3. Regression Suite
The backend suite is unified under `trade.TradeFinanceMoquiSuite`.
The frontend suite uses Playwright (frontend/e2e) and Vitest for unit tests.

### Execution (Backend)
```bash
./gradlew :runtime:component:TradeFinance:test --tests trade.TradeFinanceMoquiSuite
```

### Execution (Frontend Unit Tests)
```bash
cd frontend && npm test src/components/SwiftValidation.test.tsx
```

---

## 4. SWIFT Character Sets & Mappings
The platform validates input strictly against SWIFT MT7xx standards:
- **X-Charset**: `A-Z a-z 0-9 / - ? : ( ) . , ' + space`. (Standard narratives: 45A, 46A).
- **Z-Charset**: Extended set allowed in Tag 73, 72Z, and 77A.
- **Line Enforcement**: Narrative fields are proactively checked for line-count compliance (e.g., Tag 73 max 6 lines).
- **Utilities**: Centralized validation in `frontend/src/utils/SwiftUtils.ts`.

---

## 5. Troubleshooting & Maintenance
- **Data Seeding**: Use `./gradlew load` to refresh master data.
- **Log Analysis**: Business rule failures are logged with `[TRADE-AUTH]` or `[TRADE-VAL]` prefixes.
- **UI Stabilization**: When UI labels change, update the "Blue Premium" tokens and verify navigation integrity.

---

## Conclusion
Maintain transactional integrity above all. Ensure all frontend changes mirror backend validation constraints to enable "Clean at Capture" processing.
