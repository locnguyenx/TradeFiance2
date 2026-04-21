# Moqui Technical Design Specification
**Project Name:** Digital Trade Finance Platform
**Module:** Common Module & Import LC
**Date:** April 21, 2026

## 1. Architectural Overview
This specification bridges our Business Requirements (BRDs) and Behavior-Driven Scenarios (BDDs) to explicit data structures and execution logic within the Moqui framework. All definitions conform strictly to `AGENTS.md` and the internal Moqui patterns (`moqui-entities.md`, `moqui-entity-patterns.md`, etc.).

---

## 2. Global Enums and State Machines (`StatusItem`)

As per Moqui patterns, static lifecycle configurations leverage `moqui.basic.StatusItem` and cacheable queries.

### 2.1 Trade Instrument Lifecycle (`StatusFlow`)
The global state flow defining exactly where an instrument sits operationally.
* **TfDraft:** Initiated but incomplete.
* **TfPendingAppr:** Awaiting authorization (Tier checks apply).
* **TfIssued:** Active formal liability.
* **TfDocsReceived:** Presentation active, examination SLA running.
* **TfDiscrepant:** Operational pause requiring waivers.
* **TfAccepted:** Clean presentation, maturity calculation active.
* **TfSettled:** Final Nostro offset completed.
* **TfClosed:** Extinguished completely.

### 2.2 Trade Product Configuration Matrix (`Enumeration` cacheable)
Configuration identifiers mapped from the Common Module, representing product templates.
* **PrdSightLc:** Sight Letter of Credit Template
* **PrdUsanceLc:** Usance Letter of Credit Template
* **PrdStandbyLcG** Standby Guarantee Template
* **PrdIslamicLc** Islamic Profit-Ledger Template

---

## 3. Entity Data Model (Moqui XML Schema)

### 3.1 Common Trade Framework
#### `TradeInstrument` (Master Record)
* `instrumentId` (`id`, **PK**, `primary-key-sequence="true"`)
* `transactionRef` (`text-short`) [Required: System-generated format e.g. TF-IMP-001]
* `lifecycleStatusId` (`id`) [Relation -> `StatusItem`]
* `productEnumId` (`id`) [Relation -> `Enumeration`]
* `baseEquivalentAmount` (`number-decimal`) [Calculated via FX rules]
* `issueDate` (`date`), `expiryDate` (`date`)
* `customerFacilityId` (`id`) [Relation -> `CustomerFacility`]

#### `CustomerFacility`
* `facilityId` (`id`, **PK**)
* `totalApprovedLimit` (`number-decimal`)
* `utilizedAmount` (`number-decimal`)
* `facilityExpiryDate` (`date`)
* `isSecured` (`text-indicator`) [Y/N]

### 3.2 Amendment Shadow Record Pattern (CRITICAL PATTERN)
To support Maker/Checker authorization correctly on post-issuance updates without mutating the master record during the "Pending" phase.
#### `TradeInstrumentAmendment` (Shadow)
* `amendmentId` (`id`, **PK**, `primary-key-sequence="true"`)
* `instrumentId` (`id`) [Relation -> `TradeInstrument`]
* `lifecycleStatusId` (`id`) [Draft / PendingAppr / Applied / Rejected]
* `amountDeltaNew` (`number-decimal`) [Proposed Financial shift]
* `expiryDateNew` (`date`) [Proposed Expiry shift]
* `isBeneficiaryConsentRequired` (`text-indicator`) [Y/N]

### 3.3 Import LC Sub-Domain
#### `ImportLetterOfCredit` (Extends TradeInstrument)
* `instrumentId` (`id`, **PK**) [Direct relation mapping]
* `beneficiaryPartyId` (`id`) [Relation -> `Party`]
* `tolerancePositive` (`number-decimal`), `toleranceNegative` (`number-decimal`)
* `tenorTypeEnumId` (`id`) [Sight vs Usance]
* `usanceDays` (`number-integer`)
* `shippingGuaranteeCount` (`number-integer`)

---

## 4. Service Contracts (Moqui XML Logic)

### 4.1 Security and Idempotency Standard
All services assume `<sec-require>` execution securely. Write-methods (`create`, `update`) utilize Idempotency checks to prevent duplicate payloads. Validations utilize `<script>ec.message.addError()</script>` rather than pure XML assertions to ensure graceful `Exception` unwinding globally.

### 4.2 Standard Service Endpoints
*(Path: `runtime/component/TradeFinance/service/trade/finance`)*

#### `LimitServices.xml`
* **Verb/Noun:** `calculate#FacilityEarmark`
* **Input:** `facilityId` (Required), `targetAmount` (Required, BigDecimal)
* **Output:** `success` (Boolean)
* **Logic:** Applies `Status Guard`. Interrogates `available = totalApprovedLimit - utilizedAmount`. Adjusts utilized metric. Rolls back aggressively via ExecutionContext message errors on breaches rather than quiet return matrices.

#### `AuthorizationServices.xml`
* **Verb/Noun:** `evaluate#MakerCheckerMatrix`
* **Input:** `instrumentId` (Required)
* **Logic:** Evaluates user assignment levels vs payload financial `baseEquivalent`. Enforces strict **Segregation of Duties** by querying `createdBy` vs `ec.user.userId`. Validates shadow records (Amendments) explicitly prior to master update replication.

#### `ImportLcServices.xml`
* **Verb/Noun:** `submit#ImportLc`
* **Input:** `ImportLetterOfCredit` Entity map fields.
* **Output:** Transitioned `lifecycleStatusId`
* **Logic:** Implements "Read-Refresh-Update" pattern natively to sync parent/child validations. Performs date validations safely. Calls `calculateFacilityEarmark` globally.

#### `SwiftGenerationServices.xml`
* **Verb/Noun:** `generate#Mt700`
* **Logic:** Strict parsing using `<script>` CDATA encapsulation to avoid raw XML breakdown. Maps variables correctly against Option A (`:59A:`) or standard string text natively.

---

## 5. Test Coverage Alignment
Per standard execution rules, **all components** within these definitions will be built via pure **Test-Driven Development (TDD)** using **Spock**. 
Test suites (`*Spec.groovy`) will be constructed to fail natively against empty Moqui architectures prior to XML implementations for these entities and service paths.
