# Moqui Technical Design Specification
**Project Name:** Digital Trade Finance Platform
**Date:** April 21, 2026
**Version:** 1.0 Finalized

## 1. Architectural Overview & UI Integration
This technical design bridges the Business Requirements (BRDs), Behavior Driven Development tests (BDDs), and UI Wireframes into explicit data structures, execution logic, and deployment structures within the Moqui framework. To maintain robust modularity and crash resistance, the platform is structured cleanly between common governance logics and domain-specific instruments.

### 1.1 Architecture Pattern (Hybrid Headless)
* **Moqui Engine Backend:** Operates explicitly as a headless REST API (`/rest/s1/trade`). It acts as securely centralized control executing SWIFT mapping, limits calculation (`LimitServices`), strict state-machine constraints, and maker/checker compliance rules globally.
* **Separated Frontend SPA:** Constructed securely separate from Moqui XML framework logic, allowing React/JSX DOM structures to process the cognitively heavy UX elements (such as split-screen visualization, document presentation examination tables, and dashboard queues) locally before firing standard JSON REST calls against Moqui's native validation structure.

### 1.2 Maker/Checker Authorization Engine
* **Integration mapping:** The 4 established Authorization Tiers (Tier 1 $\rightarrow$ 4) map strictly to standard Moqui out-of-the-box native groups: `TRADE_APP_TIER_1` down to `TRADE_APP_TIER_4`. Role-Based Execution endpoints (`submit#Lc`) securely lock utilizing `UserPermission` tags.
* **Joint/Dual Approval Matrix Engine:** Because standard `UserPermission` is boolean, Dual Check (meaning specifically two active users executing approvals for Tier 4 requirements organically) inherently executes computationally inside `trade.finance.AuthorizationServices.evaluateMakerCheckerMatrix`. Natively traversing the system's `Shadow Records`, ensuring distinct unique identity validations logic prior to instrument mutation.

---

## 2. COMMON MODULE A: Trade Framework Integrations
Houses universally inherited data entity properties, limits evaluations, and core static dictionaries logically extending across collections and explicit guarantees evenly.

### 2.1 Core Entities & Master Dictionary Enum Mapping
Defined logically explicitly mapping to `moqui.basic.Enumeration` attributes.

* **Product Type Enum (`TradeProductType`):** `LC_IMPORT`, `LC_EXPORT`, `COLLECTION_IMPORT`.
* **State Machine Frameworks (`TradeTransactionFlow`):** Represents operational execution phase natively: `Draft`, `PendingApproval`, `Authorized`, `Rejected`, `Closed`.
* **SLA Configuration Matrix:** Incorporating logic directly against standard Universal Date Calendar processing APIs native skipping weekend arrays successfully.

### 2.2 Framework Entities Schema

#### `TradeInstrument` (Primary Polymorphic Parent)
Captures unified logic mapped against FX calculations securely.
* `instrumentId` (`id`, PK, auto-sequential)
* `transactionRef` (`text-short`) [TF-IMP-YY-0001 System generated standard]
* `lifecycleStatusId` (`id`) [Points to TradeTransactionFlow]
* `productEnumId` (`id`) [Points to TradeProductType]
* `baseEquivalentAmount` (`number-decimal`) [Calculated via dual-FX constraints inherently]
* `issueDate` (`date`), `expiryDate` (`date`)
* `customerFacilityId` (`id`) [Points inherently against Limits Engines]

#### `CustomerFacility`
Holds the centralized master validation bounds guaranteeing absolute non-breach parameters natively.
* `facilityId` (`id`, PK)
* `totalApprovedLimit` (`number-decimal`)
* `utilizedAmount` (`number-decimal`)
* `facilityExpiryDate` (`date`)

#### `TradeInstrumentAmendment` (Shadow Versioning)
Safely retains Maker Delta parameters before Checker commitments effectively overriding base attributes securely isolating modifications.
* `amendmentId` (`id`, PK)
* `instrumentId` (`id`) [Points to Base TradeInstrument natively]
* `transactionStatusId` (`id`) [Draft/Pending Approval statuses exclusively]
* `amountDeltaNew` (`number-decimal`), `newExpiryDate` (`date`)

### 2.3 System Services
* **`LimitServices.xml`** `calculate#FacilityEarmark`. Directly performs boundary math logic internally. Throws explicit `IllegalArgumentException` completely blocking commits when utilization breaches defined max capacity limits natively.

---

## 3. MODULE B: Import Letter of Credit (LC) Domain
Expands base foundations utilizing strictly mapped parameters fulfilling BDD and UCP600 compliance models effectively generating accurate states natively.

### 3.1 Domain Product Entities Schema

#### `ImportLetterOfCredit` (Extends Base Instrument)
Maps explicit commercial fields driving SWIFT payload values functionally.
* `instrumentId` (`id`, PK) [Mapped entirely to `TradeInstrument`]
* `businessStateId` (`id`) [Tracks precise UCP status explicitly (e.g., Issued, DocsReceived, Settled, Closed)]
* `beneficiaryPartyId` (`id`) [Validates exclusively against `TradeParty` mapping]
* `tolerancePositive` (`number-decimal`), `toleranceNegative` (`number-decimal`)
* `tenorTypeEnumId` (`id`) [ Sight, Usance_Deferred ]
* `chargeAllocationEnumId` (`id`) [ Applicant, Beneficiary, Shared ]
* `partialShipmentEnumId` (`id`), `transhipmentEnumId` (`id`)

#### `TradeDocumentPresentation`
Safeguards the physical lodgement records specifically.
* `presentationId` (`id`, PK)
* `instrumentId` (`id`) [Link to ImportLetterOfCredit logic directly]
* `presentationDate` (`date`)
* `claimAmount` (`number-decimal`)
* `isDiscrepant` (`text-indicator`)

### 3.2 Key Services
* **`ImportLcServices.xml`** `submit#ImportLc`. Reads exact constraints combining mathematical tolerances ensuring absolute protection parameters internally. Checks `TradeParty` KYC variables strictly before triggering state transitions explicitly.
* **`ImportLcValidationServices.xml`** `evaluate#Drawing`. Forces hard-stops utilizing tolerance mathematical thresholds inherently restricting excessive drawn claims logically overriding UI bounds.

---

## 4. SWIFT Generative Mapping Specifications
Strict conversion of standard Moqui REST objects exactly mapping formatted messaging logic arrays efficiently avoiding string concatenations natively.

### 4.1 Base Swift Service
* **`SwiftGenerationServices.xml`** `generate#Mt700`. Executes direct generation calls cleanly extracting attributes converting structures appropriately.

### 4.2 Structural Overrides and Logic Formats
* **Dual Designator Tags (`59` vs `59A`):** The logic explicitly checks for User Interface toggles indicating standard Name/Address routing versus structured Swift BIC mapping natively appending accurate Alpha descriptors automatically.
* **Text Array Processing (`45A`, `46A`, `47A`):** The engine natively loops and chunks any standard free-text string payload converting entirely into the SWIFT required `65-Character Limited line arrays` perfectly complying with MT formatting securely.
* **Tolerance Splitting (`39A`):** Synthesizes numeric decimals logically into string formats internally (`5/5` positive/negative arrays) satisfying parsing structures completely overriding localized decimal formatting effectively.
