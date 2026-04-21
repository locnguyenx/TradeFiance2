# Moqui Technical Design Specification
**Project Name:** Digital Trade Finance Platform
**Date:** April 21, 2026

## Architectural Overview
This specification bridges our Business Requirements (BRDs) to explicit data structures and execution logic within the Moqui framework. To ensure an easily maintainable and highly cohesive codebase, the architecture is strictly modularized into **Module A: Common Trade Framework** and **Module B: Import LC**.

All definitions conform strictly to `AGENTS.md` and the internal Moqui patterns (`moqui-entities.md`, `moqui-entity-patterns.md`, etc.).

---

# MODULE A: COMMON TRADE FINANCE FRAMEWORK
This module houses the global state machines, generalized trade entities, and master configuration matrices that apply indiscriminately to all trade products (Import, Export, Collections).

## A.1 Global System Workflows (Transaction States)
Defined as `moqui.basic.StatusItem` mapped to `StatusFlow = "TradeTransactionFlow"`. This governs the Maker/Checker processing flow independent of the domain product.
* **TxDraft:** Initiated but incomplete data entry.
* **TxPendingApproval:** Submitted, locked for maker, awaiting Checker.
* **TxAuthorized:** Officially approved.
* **TxRejected:** Checker returns to Maker for correction.
* **TxClosed:** Terminal system state.

## A.2 Global Enumerations (Configuration)
Defined as `moqui.basic.Enumeration` with `cache="true"`.
* **Product Type Enum (`TradeProductType`):** `LC_IMPORT`, `LC_EXPORT`, `COLLECTION_IMPORT`, `COLLECTION_EXPORT`.
* **Accounting Framework Enum (`AcctgFramework`):** `CONVENTIONAL` (Interest), `ISLAMIC` (Profit).
* **Allowed Tenor Configuration Enum (`ConfigTenorType`):** `SIGHT_ONLY`, `USANCE_ONLY`, `MIXED_ALLOWED`.
* **Party Role Enum (`TradePartyRole`):** `APPLICANT`, `BENEFICIARY`, `ISSUING_BANK`, `ADVISING_BANK`.
* **Limit Earmark Type Enum (`LimitTransactionType`):** `HOLD`, `DEDUCT`, `RELEASE`.

## A.3 Entity Data Model (Moqui XML Schema)

### `TradeInstrument` (Master Record)
The master log for common attributes.
* `instrumentId` (`id`, **PK**, `primary-key-sequence="true"`)
* `transactionRef` (`text-short`) [Required: System-generated format e.g. TF-IMP-001]
* `transactionStatusId` (`id`) [Relation -> `StatusItem` : Records System Workflow Status]
* `productEnumId` (`id`) [Relation -> `Enumeration` : TradeProductType]
* `baseEquivalentAmount` (`number-decimal`) [Calculated via FX rules]
* `issueDate` (`date`), `expiryDate` (`date`)
* `customerFacilityId` (`id`) [Relation -> `CustomerFacility`]

### `CustomerFacility`
* `facilityId` (`id`, **PK**)
* `totalApprovedLimit` (`number-decimal`)
* `utilizedAmount` (`number-decimal`)
* `facilityExpiryDate` (`date`)

### `TradeInstrumentAmendment` (Shadow)
Implements Moqui Shadow Record Pattern for Maker/Checker. Allows modifications without altering `TradeInstrument`.
* `amendmentId` (`id`, **PK**, `primary-key-sequence="true"`)
* `instrumentId` (`id`) [Relation -> `TradeInstrument`]
* `transactionStatusId` (`id`) [Draft / PendingApproval / Applied / Rejected]
* `amountDeltaNew` (`number-decimal`) [Proposed Financial shift]
* `expiryDateNew` (`date`) [Proposed Expiry shift]

## A.4 Service Contracts (Moqui XML Logic)

### `LimitServices.xml`
* **Verb/Noun:** `calculate#FacilityEarmark`
* **Input:** `facilityId`, `targetAmount`
* **Logic:** Rolls back aggressively via `ec.message.addError()` on breaches.

### `AuthorizationServices.xml`
* **Verb/Noun:** `evaluate#MakerCheckerMatrix`
* **Input:** `instrumentId`
* **Logic:** Evaluates user assignment levels vs payload financial `baseEquivalent`. Enforces strict **Segregation of Duties** by querying `createdBy` vs `ec.user.userId`.

---

# MODULE B: IMPORT LETTER OF CREDIT (LC)
This module acts as a specialized extension of the Common Framework, natively implementing UCP 600 logic, LC-specific states, tolerances, and presentation matrices.

## B.1 LC Lifecycle States (Domain Business States)
Defined as `moqui.basic.StatusItem` mapped to `StatusFlow = "ImportLcBusinessFlow"`. Dictates the exact business truth of the LC liability.
* **LcDraft:** Initial initiation before first authorization.
* **LcIssued:** Active formal liability towards Beneficiary.
* **LcDocsReceived:** Presentation active, examination timer running.
* **LcDiscrepant:** Operational pause requiring waivers.
* **LcAccepted:** Clean presentation, payment maturity calculation active.
* **LcSettled:** Final financial obligations cleared.
* **LcCancelled:** Instrument expired or mutually cancelled without full drawing.

## B.2 Import LC Enumerations
Defined as `moqui.basic.Enumeration` with `cache="true"`.
* **LC Tenor Type Enum (`LcTenorType`):** `SIGHT`, `USANCE_DEFERRED`, `ACCEPTANCE`, `MIXED`.
* **Partial Shipments Enum (`LcPartialShipment`):** `ALLOWED`, `NOT_ALLOWED`, `CONDITIONAL`.
* **Transhipment Enum (`LcTranshipment`):** `ALLOWED`, `NOT_ALLOWED`, `CONDITIONAL`.
* **Charge Allocation Enum (`LcChargeAlloc`):** `ALL_APPLICANT`, `ALL_BENEFICIARY`, `SHARED`.
* **Confirmation Instructions Enum (`LcConfirmInst`):** `CONFIRM`, `MAY_ADD`, `WITHOUT`.
* **Applicant Decision Enum (`LcApplicantDecision`):** `PENDING`, `WAIVED`, `REFUSED`.
* **Closure Type Enum (`LcClosureType`):** `FULLY_DRAWN`, `EXPIRED`, `EARLY_CANCELLATION`.
* **Details of Charges Tag 71A (`LcDetailedCharges`):** `OUR`, `BEN`, `SHA`.

## B.3 Entity Data Model (Moqui XML Schema)

### `ImportLetterOfCredit` (Extends TradeInstrument)
* `instrumentId` (`id`, **PK**) [Direct relation mapping]
* `businessStateId` (`id`) [Relation -> `StatusItem` : Records Domain Product Status]
* `beneficiaryPartyId` (`id`) [Relation -> `Party`]
* `tolerancePositive` (`number-decimal`), `toleranceNegative` (`number-decimal`)
* `tenorTypeEnumId` (`id`) [Relation -> `Enumeration` : LcTenorType]
* `usanceDays` (`number-integer`)
* `chargeAllocationEnumId` (`id`) [Relation -> `Enumeration` : LcChargeAlloc]
* `shippingGuaranteeCount` (`number-integer`)

### `TradeDocumentPresentation`
Tracks the exact lifecycle of physical documents claimed against the LC.
* `presentationId` (`id`, **PK**)
* `instrumentId` (`id`) [Relation -> `ImportLetterOfCredit`]
* `businessStateId` (`id`) [Tracks specifically DocsRecv -> Discrepant -> Accepted]
* `presentationDate` (`date`)
* `claimAmount` (`number-decimal`)
* `isDiscrepant` (`text-indicator`)
* `applicantDecisionEnumId` (`id`) [Relation -> `Enumeration` : LcApplicantDecision]

## B.4 Service Contracts (Moqui XML Logic)

### `ImportLcServices.xml`
* **Verb/Noun:** `submit#ImportLc`
* **Input:** `ImportLetterOfCredit` Entity map fields.
* **Output:** Transitioned `lifecycleStatusId`
* **Logic:** Implements "Read-Refresh-Update" pattern natively to sync parent/child validations. Performs date validations safely. Calls `calculateFacilityEarmark` globally.

### `SwiftGenerationServices.xml`
* **Verb/Noun:** `generate#Mt700`
* **Input:** `instrumentId`
* **Logic:** Strict parsing using `<script>` CDATA encapsulation. Overrides pure string concatenation to generate proper array-based SWIFT blocks adhering to UCP norms.
