# Comprehensive Moqui Technical Design Specification
**Project Name:** Digital Trade Finance Platform
**Date:** April 21, 2026
**Version:** 2.0 (Exhaustive Finalization)

## 1. Architectural Overview & Integration
This technical design fully maps the Business Requirements (BRD), Behavior Driven Development tests (BDD), and UI specifications directly into Moqui framework data structures, service logic, and persistence patterns. 

### 1.1 Architecture Pattern (Hybrid Headless)
* **Moqui Engine Backend:** Operates explicitly as a headless REST API (`/rest/s1/trade`). It serves as the secure core, strictly executing state-machine constraint validation, Limit/Facility manipulation, Tariff logic, SWIFT building, and database persistence.
* **Separated Frontend SPA:** A Single Page Application (e.g., Next.js/React) disconnected from standard Moqui XML Form rendering. It consumes the REST APIs and is primarily responsible for resolving the cognitive load requirements (visual split-screens, document examination tables, high-density dashboard queues) natively on the client.

### 1.2 Maker/Checker Authorization Engine Framework
* **OOTB Baseline Access Migration:** Tiers 1 through 4 correlate strictly to standard Moqui `UserGroup`s (e.g., `TRADE_APP_TIER_1` down to `TRADE_APP_TIER_4`). REST API endpoints are protected using standard `UserPermission` assignments natively attached to these groups, causing generic `403 Forbidden` limits dynamically.
* **Dual Approval & Compliance Suspensions:** Executed within the primary `trade.finance.AuthorizationServices.evaluateMakerCheckerMatrix`. Since specific logic (like requiring absolute verification natively resolving to TWO distinct Tier 4 user signatures for high-tier validations) exceeds simple boolean routing, this service directly parses `Shadow Records` determining validity natively.
* **Segregation By Identity:** Explicit Four-Eyes principle evaluates `Instrument.createdBy` directly prohibiting UI Action execution for identical user identities inherently.
* **Sanctions Interruptions:** If compliance calls output `True` during Pre-processing validations, standard structural Maker/Checker assignments logically disconnect routing targets directly toward an isolated internal `COMPLIANCE_REVIEW_QUEUE`.

---

## 2. COMMON MODULE A: Base Framework & Master Configurations
Covers the cross-cutting global operations universally applicable for all underlying Trade Operations (Letters of Credit, Shipping Guarantees, Collections).

### 2.1 Core Business Entity Relational Schema
Extending standard Moqui structures dynamically.

* **`TradeInstrument` (Base Extensibility Parent):**
  * `instrumentId` (`id`, PK, auto-sequential)
  * `transactionRef` (`text-short`) [TF-IMP-YY-0001 System generated standard utilizing NumberSequence logic].
  * `lifecycleStatusId` (`id`) [Mapped directly into `TradeTransactionFlow`]
  * `productEnumId` (`id`) [Derived via `TradeProductType`]
  * `baseEquivalentAmount` (`number-decimal`) [Pre-computed using designated local Daily Exchange Rates].
  * `issueDate` (`date`), `expiryDate` (`date`)
  * `customerFacilityId` (`id`) [FK mapping directly into Limits Engines API].

* **`CustomerFacility` (Core Limits):**
  * `facilityId` (`id`, PK)
  * `totalApprovedLimit` (`number-decimal`)
  * `utilizedAmount` (`number-decimal`)
  * `facilityExpiryDate` (`date`)

* **`TradePartyExtent` (KYC/AML Extrapolations):**
  * Extends standard Moqui `mantle.party.Party` inserting specific explicit logic arrays capturing formal fields:
  * `isKycCleared` (`boolean`), `kycExpirationDate` (`date`), `sanctionsWarningActive` (`boolean`), `partyRoleEnumId` (`id`).

* **`TradeInstrumentAmendment` (Shadow Versioning):**
  * Inherently buffers modified states explicitly avoiding physical writes over `TradeInstrument` limits until final formal Checker approval natively updates parameters securely.

### 2.2 Currency, SLA Calendars & Notifications
* **Dual-Rate FX Mapping Strategies:**
  * `Daily Board Rate Cache:` Cached end-of-day variables natively retrieved during Pre-Processing calculations evaluating `CustomerFacility` earmarks reliably (protecting inputs uniformly).
  * `Live Spot Integration API:` Settlement components hitting explicit REST API (`TreasuryProxyServices`) natively evaluating accounting cash generation matrices explicitly mapping real-time numbers independently.
* **Single Global Banking Calendar Engine:**
  * Service explicit structure computing logic (`trade.finance.DateServices.computeSlaTime`). Inherently bypasses universal weekends and Head-Office recognized structured holiday arrays evaluating maximum SLA durations strictly returning `Target End Date` explicitly.
* **Proactive Threshold Alerts Engine (Notifiers):**
  * Configured listener batches firing upon physical Limit modifications evaluating calculations. Triggers native Moqui `EmailTemplate` sending payloads asynchronously if `(utilizedAmount / totalApprovedLimit) > 0.95` or if elapsed durations hit Document Examination 5-day deadlines unconditionally.

### 2.3 Comprehensive Catalog Master Data
Ensuring dynamic configurability structurally without system deployments.

#### 1. Tariff & Fee Mathematical Engine (`FeeConfiguration` Schema)
The platform establishes dynamic cost computation structures completely controlled manually via configuration menus mapping rules natively during Service calculation executions.
* **Fields:** `feeConfigurationId`, `targetEventEnumId` (e.g., LC Issuance), `calculationTypeEnumId` (Flat, Percentage), `baseValue` (`number-decimal`), `minFloorAmount` (`number-decimal`), `maxCeilingAmount` (`number-decimal`), `customerOverrideTierId` (`id`).
* **Service Executor (`trade.finance.TariffServices.calculateFee`)**: Uses logical extraction assessing standard `Base Value` metrics checking conditional outputs cleanly against `minFloorAmount`. Evaluates particular priority matching Applicant `Customer Tier` overriding generic formulas accurately returning absolute computed `Final Applied Fee`.

#### 2. The Product Configuration Catalog Matrix (`TradeProductCatalog`)
Business definitions establishing fundamental internal constraints modifying specific rendering and routing structurally.
* **Fields & Attributes:** `productId`, `isTransferable` (`boolean`), `allowRevolving` (`boolean`), `allowAdvancePayment` (`boolean`), `maxToleranceThreshold` (`number-decimal`), `documentExamSlaRuleDays` (`number-integer`), `defaultSwiftMtTypeEnumId` (`id`), `accountingFrameworkEnumId` (`CONVENTIONAL` / `ISLAMIC`), `mandatoryMarginPercent` (`integer`).
* **Execution Logic Integration:** Modifies structural behavior inherently across components conditionally bypassing hard rules universally (e.g., Islamic framework enforces differing independent localized GL codes logically routing interest fields perfectly away; `MandatoryMargin` denies Maker Commit attempts failing to find 100% equivalent blocked customer accounts concurrently; `DocumentExamRule` completely overrides baseline generic UCP 5 Day SLAs replacing mathematical duration computations dynamically).

#### 3. Delta JSON Transaction Audit Logging
* **`TradeTransactionAudit` (Immutable Layer):**
  Every modification specifically creating immutable records. Mapped inherently capturing variables: `timestamp`, `userId`, `actionEnumId` (e.g., Maker Update), `justificationRootText`, and `snapshotDeltaJSON`. Uses pure raw entity output configurations generating physical text strings verifying no system or superuser can edit past records maliciously.

---

## 3. IMPORT LC MODULE B: Structured Lifecycle Domain
Explicit application domains expanding upon the standardized internal architectures implementing exact BRD instrument conditions definitively.

### 3.1 Domain Extensibility Engine Data Schemas

#### `ImportLetterOfCredit` (Product Direct Inherit)
Expands precise properties structurally extending `TradeInstrument` bounds definitively capturing SWIFT specific fields.
* `instrumentId` (`id`, PK) [Relational binding]
* `businessStateId` (`id`) [StatusFlow tracking UCP logic directly: `LcDraft`, `LcIssued`, `LcDocsReceived`, `LcSettled`].
* `beneficiaryPartyId` (`id`) [Foreign Link mapping `TradePartyExtent`]
* `tolerancePositive` (`number-decimal`), `toleranceNegative` (`number-decimal`) [Evaluated against constraint rules enforcing explicit restrictions inherently against max configuration levels].
* `tenorTypeId` (`id`), `usanceDays` (`number-integer`), `partialShipmentId` (`id`), `transhipmentId` (`id`), `portOfLoading` (`text`), `portOfDischarge` (`text`).
* `chargeAllocationEnumId` (`id`) [Evaluates structural Fee assignment responsibility natively `APPLICANT`, `BENEFICIARY`].

#### `ImportLcShippingGuarantee`
Tracks 110% over-earmarked independent legal claims implicitly locking applicant capabilities inherently overriding discrepancy workflows actively.
* `guaranteeId` (`id`, PK), `instrumentId` (`id`, FK LC), `invoiceAmount` (`decimal`), `liabilityMultiplierRequired` (`integer` default 110%), `transportDocReference` (`string`).

#### `TradeDocumentPresentation`
Logs examination statuses uniquely evaluating independent actions.
* `presentationId` (`id`, PK), `instrumentId` (`id`), `presentationDate` (`date`).
* `claimAmount` (`number-decimal`), `isDiscrepant` (`boolean`), `applicantDecisionEnumId` (`id` [PENDING, WAIVED, REFUSED]).

### 3.2 Key Process Operations Enforcement
* **`ImportLcValidationServices.xml` `evaluateDrawingLimits`**: Restricts mathematical output parameters implicitly demanding `ClaimAmount` evaluates mathematically `<` `(LC Amount * (1 + PositiveTolerance))`. Denies any execution resolving logic false.
* **`ImportLcSettlementTracking.xml` `processUsanceFutureQueue`**: Native logic checking accepted states enforcing automatic logical execution loops evaluating current Business Dates natively compared specifically against defined explicit `Usance Maturity Document Date` targets generating required Settlement Triggers automatically.
* **End Of Day Auto-Expiry Engine (`ImportLcBatchJobs.xml`)**: Invokes native logic assessing generic active instrument pools natively applying `System Mail Grace Days` immediately determining Expiry closure bounds dynamically returning unutilized limits logically to `Customerfacility` mappings implicitly.

---

## 4. SWIFT Messaging Formatter Validations
Executes strict conversion parsing ensuring data fields properly mutate out of standard generic entities flawlessly directly executing SWIFT parameters without logic overlaps natively.

### 4.1 Native Output Format Services
* **`trade.finance.SwiftGenerationServices.generateMt700`:** Binds output parameters actively executing generation logics logically resolving raw entities definitively inside Prowide WIFE mapping structures natively. Incorporates strict X-Character filters logically preempting generation processes ensuring inputs lack invalid logical definitions completely.

### 4.2 Structural Overrides and Logic Vectors
* **Option Designation Dynamic Filtering:** Natively executes logic detecting variable states automatically replacing Tag references systematically ensuring Bank profiles natively switch format outputs directly into `59A` tags versus explicit Name/Address arrays targeting standard `59` definitions cleanly based entirely upon UI variables mapped seamlessly.
* **Tolerance Reformat Matrix (`Tag 39A`):** Aggregates numeric variables concatenating internal decimal definitions into valid text strings definitively executing combinations generating valid target formats specifically structured as `Positive/Negative` string logic natively.
* **Native Chunked Array Formatting (`Tag 45A/46A`):** The logic loops over `Description of Goods` string payload variables securely invoking chunk array division logic strictly capping line limitations at max `65 characters` wrapping dynamically ensuring final MT block execution perfectly bounds structure inherently protecting external systems correctly.
