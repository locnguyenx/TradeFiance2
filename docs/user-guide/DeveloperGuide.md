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
- `trade.importlc`: Import LC domain logic and state transitions.
- `trade.exportlc`: Export LC domain logic (Phase 5 reconstruction).

### Service Hardening
All state-changing services must adhere to the following guards:
1.  **Enforce Maker/Checker**: Implemented via `trade.AuthorizationServices`. Status transitions for `LC_ISSUED` are blocked until `INST_APPROVED`.
2.  **Validate SWIFT**: Strict regex validation for X/Z character sets and BIC codes via `trade.importlc.ImportLcValidationServices`.
3.  **Facility Utilization**: Atomic limit check-and-earmark logic in `LimitServices.xml` to prevent over-drawing.
4.  **Audit Persistence**: Automatic transaction logging in `TradeTransactionAudit` for every business state change.

---

## 2. Technical Stack
- **Backend**: Moqui Framework 3.0 (Groovy/Java).
- **Frontend**: Next.js 14 (TypeScript / Vanilla CSS / premium tokens).
- **Communication**: REST API (v1 / trade namespace).
- **Verification**: Playwright E2E and Spock Integration Tests.

---

## 3. Regression Suite
The backend suite is unified under `trade.TradeFinanceMoquiSuite` (runtime/component/TradeFinance).
The frontend suite uses Playwright (frontend/e2e).

### Execution (Backend)
```bash
./gradlew :runtime:component:TradeFinance:test --tests trade.TradeFinanceMoquiSuite
```

### Execution (Frontend)
```bash
cd frontend && npx playwright test
```

### Validation Strategy
- **Service Layer**: TDD-based unit tests for all domain math.
- **Workflow Layer**: Integration tests for multi-step approvals and compliance holds.
- **UI Layer**: E2E tests focusing on navigation integrity and "Clean at Capture" validation.

---

## 4. SWIFT Character Sets & Mappings
The platform validates input strictly against SWIFT MT7xx standards:
- **X-Charset**: `A-Z 0-9 / - ? : ( ) . , ' + space`. (Standard narratives).
- **Z-Charset**: Includes additional characters but excludes illegal symbols. (Narrative Tag 79N).
- **BIC-11/8**: Validated via centralized `trade.importlc.ImportLcValidationServices.check#BicCode`.

---

## 5. Troubleshooting & Maintenance
- **Data Seeding**: Use `./gradlew loadSave` to refresh the master data (Facilities, Products, Tiers).
- **Log Analysis**: Business rule failures are logged with `[TRADE-AUTH]` or `[TRADE-VAL]` prefixes.
- **E2E Stabilization**: When UI labels change, update `NavigationIntegrity.spec.ts` to match the "Blue Premium" tokens.

---

## Conclusion
Maintain transactional integrity above all. When refactoring common services, ensure no side effects on the `trade.common` facility utilization logic.
