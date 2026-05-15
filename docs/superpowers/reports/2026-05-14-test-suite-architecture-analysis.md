# Trade Finance Test Suite: As-Is Analysis & To-Be Architecture

## Overview
This document provides a detailed analysis of the current test suite fragmentation and proposes a consolidated, BRD-aligned architecture to eliminate redundancy, fix sequence ID collisions, and ensure 100% test reliability. It also includes an audit of violations against the project's testing rules (`.agents/rules/testing-debugging.md`).

---

## Part 1: Testing Rules Violation Audit

I performed a codebase-wide audit against the strict rules defined in `testing-debugging.md` and `AGENTS.md`.

### 1. Resilient Assertions (Violated)
**Rule**: Avoid exact size checks (e.g., `.size() == 2`). Tests must be resilient to background data changes.
**Findings**: Multiple specs are violating this rule by asserting exact list sizes instead of verifying the presence of specific expected data or using `>=`.
- `TradePartyLcIntegrationSpec.groovy`: `juncs.size() == 4`
- `DualApprovalSpec.groovy`: `approvals1.size() == 1`
- `TradePartySpec.groovy`: `ec.entity.find(...).list().size() == 2`
- `InstrumentDataIntegritySpec.groovy`: `outA.parties.size() == 2`
- `TradeTransactionViewSpec.groovy`: `result.transactionList.size() == 2`
**Fix**: During refactoring, all exact size checks will be replaced with assertions that extract and match the actual expected IDs/values (e.g., `list.any { it.partyId == expectedId }`).

### 2. ID Isolation Constraints (Violated)
**Rule**: Every spec MUST use `tempSetSequencedIdPrimary()` to assign itself a unique, non-overlapping range (500k increments) to prevent PK collisions.
**Findings**: While the method *is* used across the suite, the 500k increment and non-overlapping rules are broken. For example:
- `TradePartySpec` uses `10,000,000`
- `NostroApiSpec` uses `10,400,000` (Only 400k apart - Violation!)
- Overlapping arbitrary ranges across the 34 files have directly caused the dirty state PK collisions (like the `23505` error for ID `53000000` seen during issuance).
**Fix**: The new 8-spec architecture will enforce strict, widely spaced ranges (10M, 20M, 30M, etc.) per module, completely eliminating this class of failure.

### 3. Hardcoded IDs & Mocking (Compliant)
- **Mocks**: No `ec.service.registerMock()`, `Stub`, or `Spy` usage was found. Tests correctly use real data.
- **Dynamic Prefix**: All specs correctly use `System.currentTimeMillis()` for `testPrefix` generation.
- **Hardcoded IDs**: No hardcoded IDs were passed to service calls; they correctly capture values like `res.instrumentId`.

---

## Part 2: As-Is Architecture Analysis (Current State)

The current suite consists of **34 separate specification files** located in `runtime/component/TradeFinance/src/test/groovy/trade/`. Many were created ad-hoc during TDD, bug fixing, or feature additions, leading to significant overlap, sequence ID collisions, and maintenance overhead.

### 1. Import LC Core Lifecycle (High Redundancy)
*   **`BddImportLcModuleSpec`**: Covers Draft, Issued, Presentation, Settlement, SG, and Cancellation (BDD Scenarios). *(Keep as base)*
*   **`ImportLcServicesSpec`**: Tests the exact same transitions as the BDD spec, but adds Maker/Checker and Internal Amendment logic. *(Redundant)*
*   **`DraftLcSpec`**: Only tests saving a draft. *(Redundant - covered by BDD-IMP-ISS-01)*
*   **`ShippingGuaranteeSpec`**: Tests SG creation. *(Redundant - covered by BDD-IMP-SG-01)*
*   **`TransactionIssuanceBugSpec`**: Tests negative guards for duplicate issuance. *(Should be merged as negative cases)*
*   **`EndToEndImportLcSpec`**: Tests a full flow. *(Keep for full integration testing)*
*   **`ImportLcValidationServicesSpec`**: Tests SWIFT character validation. *(Can be merged into SWIFT/Compliance)*

### 2. Common Module: Parties & Limits (Fragmented)
*   **`TradePartySpec`**: Covers BDD scenarios for party creation and roles. *(Keep as base)*
*   **`BddCommonModuleSpec`**: Covers common limits and charges. *(Overlap with others)*
*   **`TradePartyLcIntegrationSpec`**: Tests party assignment to LC. *(Redundant)*
*   **`LimitServicesSpec`**: Tests Facility creation/limits. *(Can be merged into Common)*
*   **`UserAccountServicesSpec`**: Tests user creation. *(Can be merged into Common)*

### 3. SWIFT & Compliance (Highly Fragmented)
*   **`SwiftGenerationSpec`**: MT700/701/707 generation logic.
*   **`SwiftValidationSpec`**: Rules for tags.
*   **`SwiftPartyGenerationSpec`**: specific tag 50/59 generation.
*   **`SwiftReimbursementSpec`**: MT740/747 logic.
*   **`TradeSwiftTriggerSpec`** / **`TradeSwiftAutoTriggerSpec`**: SECAS trigger testing.
*   **`ComplianceServicesSpec`**: Sanctions screening.
*(Conclusion: These 7 files should be unified into a single SWIFT Compliance spec)*

### 4. Security, Authorization & Data Integrity (Scattered)
*   **`AuthorizationServicesSpec`**, **`DualApprovalSpec`**, **`TradeFinanceHardeningSpec`**, **`AuthorizationDataLossSpec`**, **`InstrumentDataIntegritySpec`**
*(Conclusion: Security and state-machine guards are scattered across 5 files. They should be unified.)*

### 5. API, UI & Accounting (Valid but Isolated)
*   **`RestApiEndpointsSpec`**, **`TradeListServicesSpec`**, **`TradeSearchSpec`**, **`TradeTransactionViewSpec`**. *(API & Dashboard)*
*   **`NostroApiSpec`**. *(Accounting)*
*   **`TradeCommonEntitiesSpec`**, **`ImportLcEntitiesSpec`**, **`TradeSeedDataSpec`**, **`TradeTransactionSpec`**. *(Schema & DB)*

---

## Part 3: To-Be Specification (Proposed Architecture)

We will consolidate the 34 files into **8 core, module-aligned specifications**. Each spec will use a dedicated `tempSetSequencedIdPrimary` range (10M, 20M, 30M, etc.) to absolutely guarantee no primary key collisions when the suite runs.

### 1. `TradeCommonSpec.groovy` (Common Module)
*   **Scope**: Party Management (Commercial vs Bank, RMA logic), Customer Facilities, Global Limits, User Accounts.
*   **Consolidates**: `TradePartySpec`, `BddCommonModuleSpec`, `LimitServicesSpec`, `UserAccountServicesSpec`, `TradePartyLcIntegrationSpec`.
*   **Sequence ID Range**: 10,000,000

### 2. `ImportLcLifecycleSpec.groovy` (Import LC Module)
*   **Scope**: The complete LC lifecycle (Draft -> Issue -> Amend -> Cancel). Includes External (UCP 600) and Internal Amendments, Smart Delta logic, and Negative Guards (Duplicate transaction blocks).
*   **Consolidates**: `BddImportLcModuleSpec`, `ImportLcServicesSpec`, `DraftLcSpec`, `TransactionIssuanceBugSpec`.
*   **Sequence ID Range**: 20,000,000

### 3. `TradeDrawingSpec.groovy` (Drawing & Settlement)
*   **Scope**: Document Presentation, Discrepancies, Acceptance, Sight Payment, and Shipping Guarantees.
*   **Consolidates**: Presentation logic from `BddImportLcModuleSpec` and `ShippingGuaranteeSpec`.
*   **Sequence ID Range**: 30,000,000

### 4. `SwiftComplianceSpec.groovy` (SWIFT & Sanctions)
*   **Scope**: All MT message generation (700, 701, 707, 740, 747), SECAS automated triggers, SWIFT character validation (X-Char), and Compliance Hold/Release logic.
*   **Consolidates**: `SwiftGenerationSpec`, `SwiftValidationSpec`, `SwiftPartyGenerationSpec`, `SwiftReimbursementSpec`, `TradeSwiftTriggerSpec`, `TradeSwiftAutoTriggerSpec`, `ComplianceServicesSpec`, `ImportLcValidationServicesSpec`.
*   **Sequence ID Range**: 40,000,000

### 5. `TradeSecuritySpec.groovy` (Security & Integrity)
*   **Scope**: Maker/Checker enforcement, Dual Approval bypass rules, Data loss prevention during state transitions, and Concurrency transaction locking.
*   **Consolidates**: `AuthorizationServicesSpec`, `TradeFinanceHardeningSpec`, `DualApprovalSpec`, `AuthorizationDataLossSpec`, `InstrumentDataIntegritySpec`.
*   **Sequence ID Range**: 50,000,000

### 6. `NostroReconciliationSpec.groovy` (Accounting)
*   **Scope**: Nostro reconciliation workflows, statement matching.
*   **Consolidates**: `NostroApiSpec` (Retained/Renamed for clarity).
*   **Sequence ID Range**: 60,000,000

### 7. `TradeApiAndSearchSpec.groovy` (Frontend Integration)
*   **Scope**: REST API payload validation, Dashboard List View data (Status filters), and ElasticSearch cross-reference indexing.
*   **Consolidates**: `RestApiEndpointsSpec`, `TradeListServicesSpec`, `TradeSearchSpec`, `TradeTransactionViewSpec`.
*   **Sequence ID Range**: 70,000,000

### 8. `TradeCoreDataSpec.groovy` (Infrastructure)
*   **Scope**: Entity definition validation, Seed Data loading, basic DB CRUD integrity.
*   **Consolidates**: `ImportLcEntitiesSpec`, `TradeCommonEntitiesSpec`, `TradeSeedDataSpec`, `TradeTransactionSpec`.
*   **Sequence ID Range**: 80,000,000

*(Note: `EndToEndImportLcSpec` will be retained as `EndToEndIntegrationSpec` for a full cross-module smoketest).*
