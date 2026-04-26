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
The platform is built on **Moqui Framework**, utilizing a headless service architecture.

### Namespace Convention
- `trade.common`: Shared entities (Facilities, FX, Config).
- `trade.importlc`: Import LC domain logic.
- `trade.exportlc`: Export LC domain logic (Phase 5).

### Service Hardening
All state-changing services must:
1.  **Enforce Maker/Checker**: Via `trade.AuthorizationServices`.
2.  **Validate SWIFT**: Via `trade.importlc.ImportLcValidationServices`.
3.  **Audit**: Create entries in `TradeTransactionAudit`.

---

## 2. Domain Logic & Entities

### Financial Integrity
- **Facility Management**: Handled by `LimitServices.xml`.
- **Earmarking**: 110% multiplier for Shipping Guarantees is enforced in `create#ShippingGuarantee`.
- **Settlement**: Atomic transaction that updates `utilizedAmount`, `outstandingAmount`, and posts to the GL.

### SWIFT Character Sets
The platform enforces validation at the service level:
- **X-Charset**: Used for addresses, names, and general fields.
- **Z-Charset**: Used for Tag 79N narratives and amendments.
- **BIC Format**: Strict 8/11 character validation.

---

## 3. Regression Suite
The suite is unified under `trade.TradeFinanceMoquiSuite`.

### Execution
```bash
./gradlew :runtime:component:TradeFinance:test --tests trade.TradeFinanceMoquiSuite
```

### Components (18)
1.  **BddCommonModuleSpec**: Base framework and business rules.
2.  **BddImportLcModuleSpec**: Core LC lifecycle.
3.  **ImportLcServicesSpec**: Hardened issuance flows.
4.  **ImportLcValidationServicesSpec**: SWIFT and BIC validation.
5.  **DraftLcSpec**: Automated reference generation.
6.  **ShippingGuaranteeSpec**: 110% limit verification.
7.  **AuthVerificationSpec**: Logic bypass guards.
8.  **AuthorizationServicesSpec**: Four-eyes matrix.
9.  **ComplianceServicesSpec**: Sanctions proxy.
10. **DualApprovalSpec**: Tier 4 thresholds.
11. **EndToEndImportLcSpec**: Integration journeys.
12. **ImportLcEntitiesSpec**: Persistent schema.
13. **LimitServicesSpec**: Facility math.
14. **RestApiEndpointsSpec**: API contracts.
15. **SwiftGenerationSpec**: MT700 construction.
16. **SwiftValidationSpec**: Regex and boundary checks.
17. **TradeCommonEntitiesSpec**: Common schema.
18. **TradeSeedDataSpec**: Enumeration integrity.

---

## 4. Troubleshooting
- **Referential Integrity**: Check `TradeFinanceMasterData.xml` for valid `productId` and `facilityId`.
- **Auth Exceptions**: Ensure `ec.artifactExecution.disableAuthz()` is used in setup for bypass tests.
- **Log Noise**: Business errors are logged as `ERROR` by `MessageFacadeImpl`; this is expected for auditability.

---

## 5. Deployment
Refer to the standard Moqui `Run+and+Deploy` documentation for production WAR generation.
