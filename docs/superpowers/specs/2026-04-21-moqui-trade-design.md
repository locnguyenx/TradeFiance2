# Comprehensive Moqui Technical Design Specification
**Project Name:** Digital Trade Finance Platform
**Date:** April 23, 2026
**Version:** 3.0 (Comprehensive)
**Traceability:** Maps every requirement from the Common Module BRD, Import LC BRD, and UI Wireframes.

---

## 1. Architectural Overview & Integration

### 1.1 Architecture Pattern (Hybrid Headless)
* **Moqui Engine Backend:** Operates as a headless REST API (`/rest/s1/trade`). Executes state-machine governance, validation, limit manipulation, tariff logic, SWIFT generation, and database persistence.
* **Separated Frontend SPA:** A Next.js/React SPA consuming the REST APIs. Handles visual workflows (split-screens, document examination tables, dashboard queues) on the client.

### 1.2 Dual-Status Architecture
Every trade transaction uses two parallel status flows, corresponding to the BRD distinction between "Transaction State" and "LC Business State":

| Status Flow | Entity Field | Tracks | BRD Source |
|:---|:---|:---|:---|
| **Transaction State** (System) | `TradeInstrument.lifecycleStatusId` | Processing workflow: who has control | REQ-IMP-DEF-01 |
| **Business State** (Domain) | `ImportLetterOfCredit.businessStateId` | Instrument lifecycle: legal/operational reality | REQ-IMP-DEF-02 |

**Transaction States** (`lifecycleStatusId`):
* `INST_DRAFT` — Maker is editing
* `INST_PRE_ISSUE` — Pre-processing validations running
* `INST_PENDING_APPROVAL` — In Checker queue
* `INST_AUTHORIZED` — Checker approved, execution complete
* `INST_HOLD` — Compliance/Sanctions hold
* `INST_CANCELLED` — Permanently declined

**Rationale:** A Maker can submit an Amendment (Transaction State = `INST_PENDING_APPROVAL`) while the LC remains in Business State = `LC_ISSUED`. These are orthogonal dimensions.

### 1.3 Standardized Processing Flow (REQ-COM-WF-01)
Every transaction follows this 6-step orchestration, implemented as a service chain:

```
Step 1: Initiation (Data Capture)
  → Service: create#{ModuleName} (e.g., create#ImportLetterOfCredit)
  → Result: TradeInstrument created, lifecycleStatusId = INST_DRAFT

Step 2: Pre-Processing Validations
  → Service: validate#Submission
  → Calls: check#Sanctions, validate#Kyc, calculate#Earmark, check#DateLogic
  → Result: Pass/Fail with specific error messages

Step 3: Authorization (Maker/Checker)
  → Service: submit#ForApproval → sets lifecycleStatusId = INST_PENDING_APPROVAL
  → Service: authorize#Instrument → evaluates MakerCheckerMatrix
  → Result: Approved (INST_AUTHORIZED) or Rejected (INST_DRAFT)

Step 4: Execution (Post-Authorization)
  → Service: execute#PostAuthorization
  → Calls: update#Utilization, calculate#Fees, generate#Mt700
  → Result: Business state transitions (e.g., LC_DRAFT → LC_ISSUED)

Step 5: Lifecycle Events
  → Services: create#Amendment, create#Presentation, create#ShippingGuarantee
  → Each follows Steps 1-4 internally

Step 6: Settlement & Closure
  → Service: create#ImportLcSettlement, update#Cancellation
  → Result: Limits released, terminal state reached
```

---

## 2. COMMON MODULE: Base Framework & Master Configurations

### 2.1 Entity Schema

#### 2.1.1 TradeInstrument (REQ-COM-ENT-01)
The base trade transaction record shared across all TF products. Captures the **original issuance snapshot** — values at the time the instrument was first authorized. Subsequent processing (amendments, partial draws) updates effective values on the domain entity (e.g., `ImportLetterOfCredit`) while this record preserves the original terms for audit and reporting.

**Instrument & Financial Data (Original Snapshot):**

| Field | Type | BRD Mapping | Notes |
|:---|:---|:---|:---|
| `instrumentId` | `id` (PK) | — | Auto-sequenced |
| `transactionRef` | `text-short` | Transaction Reference Number | Format: `TF-IMP-YY-NNNN` via `NumberSequence` |
| `productEnumId` | `id` | Product Type | FK → `Enumeration` |
| `amount` | `number-decimal` | Original Issuance Amount | Frozen at authorization; never updated by amendments |
| `currencyUomId` | `id` | Transaction Currency | ISO 4217 3-letter code |
| `outstandingAmount` | `number-decimal` | — | Remaining drawable balance (updated by draws/settlements) |
| `baseEquivalentAmount` | `number-decimal` | Base Equivalent Amount | Computed: `amount × Daily Board Rate` at issuance |
| `applicantPartyId` | `id` | — | FK → `TradeParty` |
| `beneficiaryPartyId` | `id` | — | FK → `TradeParty` |
| `issueDate` | `date` | Original Issue Date | Cannot be in the past |
| `expiryDate` | `date` | Original Expiry Date | Must be ≥ `issueDate` |
| `customerFacilityId` | `id` | — | FK → `CustomerFacility` |

**Transaction Management Fields [NEW]:**

| Field | Type | Notes |
|:---|:---|:---|
| `transactionDate` | `date` | **[NEW]** Business date of the transaction (may differ from system timestamp) |
| `transactionTypeEnumId` | `id` | **[NEW]** `NEW_ISSUANCE`, `AMENDMENT`, `PRESENTATION`, `SETTLEMENT`, `CANCELLATION` |
| `lifecycleStatusId` | `id` | Transaction State (System): FK → `StatusItem` |
| `transactionStatusId` | `id` | **[NEW]** Processing status: `TRANS_DRAFT`, `TRANS_SUBMITTED`, `TRANS_APPROVED`, `TRANS_REJECTED`, `TRANS_CANCELLED` |
| `makerUserId` | `id` | **[NEW]** User who created/submitted the transaction |
| `makerTimestamp` | `date-time` | **[NEW]** When the Maker submitted |
| `checkerUserId` | `id` | **[NEW]** User who authorized/rejected |
| `checkerTimestamp` | `date-time` | **[NEW]** When the Checker acted |
| `rejectionReason` | `text-long` | **[NEW]** Mandatory when `transactionStatusId = TRANS_REJECTED` |
| `versionNumber` | `number-integer` | **[NEW]** Incremented on each authorized change (issuance=1, 1st amendment=2, etc.) |
| `lastUpdateTimestamp` | `date-time` | **[NEW]** Last modification timestamp (complements Moqui's `lastUpdatedStamp`) |
| `priorityEnumId` | `id` | **[NEW]** `NORMAL`, `URGENT`, `EXPRESS` — affects SLA timers and queue ordering |

**Existing implementation:** [TradeCommonEntities.xml](file:///Users/me/myprojects/moqui-trade/runtime/component/TradeFinance/entity/TradeCommonEntities.xml)

#### 2.1.2 TradeParty (REQ-COM-ENT-02)
Party directory capturing KYC/AML data. Extends Moqui's `mantle.party.Party`.

| Field | Type | BRD Mapping | Notes |
|:---|:---|:---|:---|
| `partyId` | `id` (PK) | Party ID | Links to Mantle Party |
| `partyName` | `text-medium` | Legal Name | Official legal name for SWIFT |
| `kycStatus` | `text-short` | KYC Status | Values: `Active`, `Expired`, `Pending` |
| `kycExpiryDate` | `date` | — | Next review date |
| `sanctionsStatus` | `text-short` | Sanctions Status | **[NEW]** `Clear`, `Suspended`, `Blocked` |
| `countryOfRisk` | `id` | Country of Risk | **[NEW]** ISO country code |
| `swiftBic` | `text-short` | — | **[NEW]** SWIFT BIC for banks |
| `registeredAddress` | `text-long` | Registered Address | **[NEW]** 4x35 SWIFT format |
| `partyRoleEnumId` | `id` | Role in Transaction | **[NEW]** Applicant, Beneficiary, etc. |

**Gap closed:** G1 — Full Party Directory fields now mapped.

#### 2.1.3 CustomerFacility (REQ-COM-ENT-03)

| Field | Type | BRD Mapping |
|:---|:---|:---|
| `facilityId` | `id` (PK) | Facility ID |
| `partyId` | `id` | **[NEW]** FK to the customer who owns this facility |
| `totalApprovedLimit` | `number-decimal` | Total Approved Limit |
| `utilizedAmount` | `number-decimal` | Utilized Amount |
| `facilityExpiryDate` | `date` | Facility Expiry Date |

**Computed field** (service-level, not stored): `availableEarmark = totalApprovedLimit - utilizedAmount`

#### 2.1.4 TradeTransactionAudit (REQ-COM-MAS-03)
Immutable, append-only audit log.

| Field | Type | BRD Mapping |
|:---|:---|:---|
| `instrumentId` | `id` (PK) | Transaction Ref |
| `auditId` | `id` (PK) | — (secondary sequenced) |
| `timestamp` | `date-time` | Timestamp (millisecond precision) |
| `userId` | `id` | User ID (or `SYSTEM` for batch) |
| `actionEnumId` | `id` | Action Performed |
| `justificationRootText` | `text-long` | Justification (mandatory for rejections) |
| `snapshotDeltaJSON` | `text-very-long` | Old/New value delta |
| `ipAddress` | `text-short` | **[NEW]** Request origin IP |
| `fieldChanged` | `text-medium` | **[NEW]** Specific field modified |

**Validation rules:**
* No `UPDATE` or `DELETE` operations on this entity — append only.
* If `actionEnumId` in (`REJECT`, `OVERRIDE`), `justificationRootText` is mandatory.

**Gap closed:** G16 (partially) — IP address and field-level tracking added.

#### 2.1.5 FeeConfiguration [NEW] (REQ-COM-MAS-01)
Dynamic tariff rule definitions, managed via Maker/Checker.

| Field | Type | BRD Mapping |
|:---|:---|:---|
| `feeConfigurationId` | `id` (PK) | — |
| `feeEventEnumId` | `id` | Fee Event Trigger (LC_ISSUANCE, AMENDMENT, etc.) |
| `calculationTypeEnumId` | `id` | Calculation Type (FLAT, PERCENTAGE, TIERED) |
| `baseValue` | `number-decimal` | Base Rate / Amount |
| `minFloorAmount` | `number-decimal` | Minimum Charge |
| `maxCeilingAmount` | `number-decimal` | Maximum Charge |
| `frequencyEnumId` | `id` | Frequency (ONE_OFF, PER_MONTH, PER_QUARTER) |
| `effectiveDate` | `date` | Effective Date (cannot backdate) |
| `customerTierOverride` | `text-short` | Customer Tier override |
| `statusId` | `id` | DRAFT / ACTIVE (Maker/Checker governed) |

**Gap closed:** G17 — Fee workflow with Maker/Checker and effective date logic.

#### 2.1.6 TradeProductCatalog [NEW] (REQ-COM-PRD-01)
Product configuration matrix for business administrators.

| Field | Type | BRD Mapping |
|:---|:---|:---|
| `productId` | `id` (PK) | ProductID |
| `productName` | `text-medium` | Product Name |
| `isActive` | `text-indicator` | Is Active |
| `productTypeEnumId` | `id` | Product Type |
| `allowedTenorEnumId` | `id` | Allowed Tenor (SIGHT_ONLY, USANCE_ONLY, MIXED) |
| `maxToleranceLimit` | `number-integer` | Max Tolerance Limit (%) |
| `allowRevolving` | `text-indicator` | Allow Revolving |
| `allowAdvancePayment` | `text-indicator` | Allow Advance Payment |
| `isStandby` | `text-indicator` | Is Standby (SBLC) |
| `isTransferable` | `text-indicator` | Is Transferable |
| `accountingFrameworkEnumId` | `id` | Accounting Framework (CONVENTIONAL, ISLAMIC) |
| `mandatoryMarginPercent` | `number-integer` | Mandatory Cash Margin (%) |
| `documentExamSlaDays` | `number-integer` | Document Exam SLA Days |
| `defaultSwiftFormatEnumId` | `id` | Default SWIFT Format (MT700, MT760) |

**Impact on processes** (REQ-COM-PRD-02):
* `allowedTenorEnumId` → hides/shows Usance Days in UI stepper
* `mandatoryMarginPercent` → blocks Maker Commit if cash margin not held
* `documentExamSlaDays` → overrides default 5-day countdown
* `isStandby` → routes to MT760 instead of MT700
* `allowRevolving` → enables auto-reinstatement after settlement
* `accountingFrameworkEnumId` → selects Conventional vs Islamic GL codes

**Gap closed:** G18 — Full product impact on processes.

#### 2.1.7 UserAuthorityProfile [NEW] (REQ-COM-MAS-02)
User profile for authority tier management.

| Field | Type | BRD Mapping |
|:---|:---|:---|
| `userAuthorityId` | `id` (PK) | — |
| `userId` | `id` | User ID (links to Moqui UserAccount) |
| `makerCheckerFlag` | `id` | Maker Only / Checker Only / Dual |
| `delegationTierId` | `id` | Tier 1–4 |
| `customLimit` | `number-decimal` | Custom override limit (base currency) |
| `branchAccessList` | `text-long` | JSON array of branch IDs |
| `isSuspended` | `text-indicator` | If Y, removed from routing matrix |

**Gap closed:** G15 — Suspended user removal, branch access, custom limits.

### 2.2 Currency, FX & Calendar Services

#### 2.2.1 Currency Precision (REQ-COM-FX-01)
**Service:** `TradeCommonServices.round#Amount`
* Accepts `currencyUomId`, looks up ISO decimal places (USD=2, JPY=0, BHD=3)
* ALL monetary fields pass through this service before persistence
* **Existing implementation** already handles this via `setScale`.

**Gap closed:** G3.

#### 2.2.2 Dual-Rate FX (REQ-COM-FX-02)
**Service:** `TradeCommonServices.get#ExchangeRate`
* `rateType` parameter: `BOARD` (daily cached) or `LIVE` (real-time API)
* Board Rate used for: pre-processing limit checks, `baseEquivalentAmount` calculation
* Live Rate used for: settlement services (MT202/103 generation)

**Existing implementation** has a stub returning hardcoded rates — will need a `rateType` parameter added.

#### 2.2.3 Banking Calendar (REQ-COM-SLA-01)
**Service:** `TradeCommonServices.calculate#BusinessDate`
* Skips weekends + holidays defined in a `BankingHoliday` entity (currently hardcoded list)
* Used by: SLA countdown, auto-expiry batch, usance maturity calculation

#### 2.2.4 Overdue Enforcement (REQ-COM-SLA-02)
**Service [NEW]:** `TradeCommonServices.evaluate#SlaOverdue`
* Input: `presentationDate`, `productId`
* Calculates business days elapsed since presentation
* Returns: `{ daysElapsed, isWarning (>=3), isBlocked (>=5) }`
* On Day 3: sets `TradeInstrument.slaWarningFlag = true` + triggers email
* On Day 5: sets `TradeInstrument.slaBlockedFlag = true` — hard-stops any further action

**Gap closed:** G4.

### 2.3 Notification Services

#### 2.3.1 SLA & Threshold Alerts (REQ-COM-NOT-01)
**Service:** `TradeCommonServices.evaluate#Threshold`
* Fires when `(utilizedAmount / totalApprovedLimit) > 0.95`
* Triggers Moqui `EmailTemplate` to group inbox

**Service [NEW]:** `TradeCommonServices.send#SlaAlert`
* Fired by `evaluate#SlaOverdue` when Day 3 breached
* Routes to operations group inbox

#### 2.3.2 Compliance Holds (REQ-COM-NOT-02)
**Service [NEW]:** `TradeCommonServices.send#ComplianceAlert`
* Fired when `check#Sanctions` returns `isHit = true` OR `validate#Kyc` finds expired status during submission
* Routes to `COMPLIANCE_REVIEW_QUEUE` inbox
* Sets `lifecycleStatusId = INST_HOLD`

**Gap closed:** G5.

### 2.4 Cross-Functional Validation Rules

All validations are invoked during Step 2 (Pre-Processing) of the processing flow via a composite service `validate#Submission`.

#### 2.4.1 Risk & Compliance Rules (REQ-COM-VAL-01)

| Rule | Service | Logic | Error |
|:---|:---|:---|:---|
| KYC Validation | `validate#Kyc` | If `TradeParty.kycStatus == 'Expired'` → block | "Party KYC status is expired" |
| Sanctions Hold | `check#Sanctions` | Screen party names, vessel names, ports against watchlist. If `isHit` → set `INST_HOLD` + route to Compliance | "Transaction held for compliance review" |
| Limit Availability | `LimitServices.calculate#Earmark` | If `baseEquivalentAmount > availableEarmark` → block | "Insufficient limit" |

**Sanctions screening scope (REQ-COM-VAL-01 detail):**
* All `TradeParty` names (applicant, beneficiary, advising bank)
* Vessel names (from `ImportLcShippingGuarantee.vesselName`)
* Ports of loading/discharge
* Free-text fields (goods description) via keyword scan

**Gap closed:** G6, G7, G8.

#### 2.4.2 Operational & Security Rules (REQ-COM-VAL-02)

| Rule | Service | Logic |
|:---|:---|:---|
| Four-Eyes Principle | `evaluate#DutySegregation` | `maker != checker` at user identity level |
| Approval Authority | `evaluate#MakerCheckerMatrix` | Compare `baseEquivalentAmount` against user's delegation tier max |
| Downward Delegation Block | `evaluate#MakerCheckerMatrix` | If Checker's tier < required tier → return `isAuthorized = false` with "Insufficient Authority" |
| Historical Immutability | `validate#AmendmentRequired` **[NEW]** | If `businessStateId` in (`LC_ISSUED`, `LC_SETTLED`) → block direct field updates; require Amendment workflow |

**Gap closed:** G9, G10.

#### 2.4.3 Business Logic Rules (REQ-COM-VAL-03)

| Rule | Service | Logic |
|:---|:---|:---|
| Date Sequence | Inline in `create#ImportLetterOfCredit` | `expiryDate >= issueDate` |
| Back-Valuation | `validate#Submission` **[NEW]** | `issueDate` cannot be in past without admin override |
| Currency Verification | `validate#Submission` **[NEW]** | `currencyUomId` must exist in active `Uom` table |

**Gap closed:** G11.

### 2.5 Maker/Checker Authorization Engine

#### 2.5.1 Delegated Authority Tiers (REQ-COM-AUTH-01)

| Tier | UserGroup | Max Approval Limit (Base Equivalent) |
|:---|:---|:---|
| Tier 1 | `TRADE_APP_TIER_1` | Up to $100,000 |
| Tier 2 | `TRADE_APP_TIER_2` | Up to $1,000,000 |
| Tier 3 | `TRADE_APP_TIER_3` | Up to $5,000,000 |
| Tier 4 | `TRADE_APP_TIER_4` | Above $5,000,000 |

**Service:** `AuthorizationServices.evaluate#MakerCheckerMatrix`
* Inputs: `instrumentId`, `userId` (Checker)
* Logic:
  1. Fetch `baseEquivalentAmount` from `TradeInstrument`
  2. Determine required tier from amount ranges
  3. Fetch Checker's `delegationTierId` from `UserAuthorityProfile`
  4. If Checker's tier < required tier → `isAuthorized = false` + "Insufficient Authority"
  5. If Checker's `userId` == Maker's `userId` (from audit) → `isAuthorized = false` + "Four-Eyes violation"
  6. If Checker `isSuspended = Y` → `isAuthorized = false`

**Gap closed:** G12.

#### 2.5.2 Dual-Checker / Joint Approval (REQ-COM-AUTH-02)
For Tier 4 transactions requiring two distinct Checker signatures:

**Service [NEW]:** `AuthorizationServices.evaluate#JointApproval`
* After first Checker approves, `lifecycleStatusId` moves to `INST_PARTIAL_APPROVAL`
* A `TradeTransactionAudit` record captures first Checker's approval
* Transaction reappears in Checker queue for second approval
* Second Checker must be a different user from both the Maker and the first Checker
* Only on second approval does `lifecycleStatusId` move to `INST_AUTHORIZED`

**Tracking entity [NEW]:** `TradeApprovalRecord`
| Field | Type | Notes |
|:---|:---|:---|
| `approvalId` | `id` (PK) | — |
| `instrumentId` | `id` | FK to TradeInstrument |
| `checkerUserId` | `id` | Who approved |
| `approvalSequence` | `number-integer` | 1st or 2nd approval |
| `approvalTimestamp` | `date-time` | When |
| `approvalDecision` | `id` | APPROVED, REJECTED |

**Gap closed:** G13.

#### 2.5.3 Special Authorization Scenarios (REQ-COM-AUTH-03)

| Scenario | Rule | Implementation |
|:---|:---|:---|
| Financial Amendments | Tier calculated on **new total liability**, not delta | `create#Amendment` computes `newTotalAmount` → calls `evaluate#MakerCheckerMatrix` with that value |
| Non-Financial Amendments | Default Tier 1 | `create#Amendment` checks `isFinancial`; if `N` → `requiredTier = 1` |
| Parallel Compliance | Compliance approval precedes financial approval | If `check#Sanctions` returns `isHit`, route to Compliance queue. Compliance Officer must `release#ComplianceHold` before standard Checker can authorize |

**Service [NEW]:** `AuthorizationServices.release#ComplianceHold`
* Sets `lifecycleStatusId` from `INST_HOLD` → `INST_PENDING_APPROVAL`
* Requires mandatory audit comments
* Only users with `TRADE_COMPLIANCE_OFFICER` role can invoke

**Gap closed:** G14.

### 2.6 Report Generation (REQ-COM-MAS-03)

**Service [NEW]:** `TradeReportServices.generate#TransactionHistory`
* Queries `TradeTransactionAudit` for a given `instrumentId`
* Returns structured JSON for UI rendering

**Service [NEW]:** `TradeReportServices.generate#ComplianceExtract`
* Queries all audit records for a date range
* Produces CSV output suitable for regulatory submission

**Gap closed:** G16.

---

## 3. IMPORT LC MODULE: Structured Lifecycle Domain

### 3.1 Entity Schema

#### 3.1.1 ImportLetterOfCredit (REQ-IMP-02)

> [!IMPORTANT]
> **Snapshot vs. Effective Values:** `TradeInstrument` preserves the **original issuance snapshot** (amount, expiry, tolerance at authorization). `ImportLetterOfCredit` holds the **latest effective values** — updated by amendments, partial draws, and extensions. All lifecycle processing (drawing validation, tolerance checks, SLA countdown, SWIFT generation) reads from the effective values on this entity, not from `TradeInstrument`.

**LC-Specific Domain Fields:**

| Field | Type | BRD Mapping |
|:---|:---|:---|
| `instrumentId` | `id` (PK) | Relational binding to TradeInstrument |
| `businessStateId` | `id` | LC Business State |
| `beneficiaryPartyId` | `id` | Beneficiary |
| `tolerancePositive` | `number-decimal` | Tolerance (+) % |
| `toleranceNegative` | `number-decimal` | Tolerance (-) % |
| `tenorTypeId` | `id` | Tenor Type (SIGHT, USANCE, ACCEPTANCE, MIXED) |
| `usanceDays` | `number-integer` | Usance Days |
| `portOfLoading` | `text-medium` | Port of Loading |
| `portOfDischarge` | `text-medium` | Port of Discharge |
| `expiryPlace` | `text-medium` | Expiry Place |
| `goodsDescription` | `text-very-long` | Description of Goods |
| `documentsRequired` | `text-very-long` | Documents Required |
| `additionalConditions` | `text-very-long` | Additional Conditions |
| `chargeAllocationEnumId` | `id` | **[NEW]** Charge Allocation (APPLICANT, BENEFICIARY, SHARED) |
| `partialShipmentEnumId` | `id` | **[NEW]** Partial Shipments (ALLOWED, NOT_ALLOWED, CONDITIONAL) |
| `transhipmentEnumId` | `id` | **[NEW]** Transhipment (ALLOWED, NOT_ALLOWED, CONDITIONAL) |
| `latestShipmentDate` | `date` | **[NEW]** Must be ≤ effectiveExpiryDate |
| `confirmationEnumId` | `id` | **[NEW]** Confirmation Instructions (CONFIRM, MAY_ADD, WITHOUT) |
| `lcTypeEnumId` | `id` | **[NEW]** Form of Doc Credit (IRREVOCABLE, IRREVOCABLE_TRANSFERABLE) |
| `productCatalogId` | `id` | **[NEW]** FK to TradeProductCatalog |

**Replicated Effective Values [NEW] — Updated by Amendments:**

| Field | Type | Notes |
|:---|:---|:---|
| `effectiveAmount` | `number-decimal` | **[NEW]** Current LC amount after all amendments. Initialized = `TradeInstrument.amount`. Amendment service updates this: `effectiveAmount += amountAdjustment` |
| `effectiveCurrencyUomId` | `id` | **[NEW]** Current currency (normally immutable, but kept for completeness) |
| `effectiveExpiryDate` | `date` | **[NEW]** Current expiry after extensions. Initialized = `TradeInstrument.expiryDate`. Amendment service updates when `newExpiryDate` is specified |
| `effectiveTolerancePositive` | `number-decimal` | **[NEW]** Current positive tolerance. Amendment service updates when `newTolerance` specified |
| `effectiveToleranceNegative` | `number-decimal` | **[NEW]** Current negative tolerance |
| `effectiveOutstandingAmount` | `number-decimal` | **[NEW]** Current drawable balance = `effectiveAmount - totalDrawn`. Updated by presentations and settlements |
| `totalAmendmentCount` | `number-integer` | **[NEW]** Count of authorized amendments (drives `amendmentNumber` on child records) |
| `cumulativeDrawnAmount` | `number-decimal` | **[NEW]** Total drawn across all presentations. Used for tolerance limit checks: `cumulativeDrawnAmount + newClaim ≤ effectiveAmount × (1 + effectiveTolerancePositive)` |

**Amendment processing rule:** When an amendment is authorized, the service updates these effective fields on `ImportLetterOfCredit` while `TradeInstrument` retains the original issuance values. This ensures:
* Drawing validation, SLA timers, and SWIFT generation always use the latest effective values
* The original terms are preserved for audit, reporting, and regulatory comparison
* No ambiguity about which "amount" to use in any given context

**Gap closed:** G22 — Complete data dictionary now mapped.

#### 3.1.2 ImportLcAmendment (REQ-IMP-SPEC-02)
Already implemented. Additional fields needed:

| Field | Type | Notes |
|:---|:---|:---|
| `amendmentNumber` | `number-integer` | **[NEW]** Auto-incremented per parent LC |
| `newTolerance` | `number-decimal` | **[NEW]** New tolerance override |
| `chargeAllocationEnumId` | `id` | **[NEW]** Who pays amendment fee |

#### 3.1.3 TradeDocumentPresentation (REQ-IMP-SPEC-03)
Already implemented. Additional fields needed:

| Field | Type | Notes |
|:---|:---|:---|
| `presentingBankBic` | `text-short` | **[NEW]** Presenting Bank SWIFT BIC |
| `presentingBankRef` | `text-medium` | **[NEW]** Foreign bank's cover letter ref |
| `claimCurrency` | `id` | **[NEW]** Must match parent LC currency |
| `regulatoryDeadline` | `date` | **[NEW]** Computed: presentationDate + 5 banking days |
| `slaWarningFlag` | `text-indicator` | **[NEW]** Set on Day 3 |
| `slaBlockedFlag` | `text-indicator` | **[NEW]** Set on Day 5 |

#### 3.1.4 TradeDocumentPresentationItem
Already implemented — captures per-document type counts.

#### 3.1.5 PresentationDiscrepancy [NEW] (REQ-IMP-SPEC-03)

| Field | Type | Notes |
|:---|:---|:---|
| `discrepancyId` | `id` (PK) | — |
| `presentationId` | `id` | FK to TradeDocumentPresentation |
| `isbpCode` | `text-short` | Standard ISBP discrepancy code |
| `description` | `text-long` | Free text detail |

**Gap closed:** G25 (discrepancy logging).

#### 3.1.6 ImportLcShippingGuarantee (REQ-IMP-SPEC-05)
Already implemented. Additional fields needed:

| Field | Type | Notes |
|:---|:---|:---|
| `sgTypeEnumId` | `id` | **[NEW]** `SEA_GUARANTEE` or `AIR_ENDORSEMENT` |
| `sgIssueDate` | `date` | **[NEW]** Defaults to business date |
| `carrierName` | `text-medium` | **[NEW]** Shipping line/agent name |
| `vesselNameVoyage` | `text-medium` | **[NEW]** Vessel + voyage (sea only) |
| `goodsDescriptionBrief` | `text-long` | **[NEW]** Brief goods description |
| `waiverLockFlag` | `text-indicator` | **[NEW]** `Y` prevents applicant refusal |
| `redemptionDate` | `date` | **[NEW]** When physical SG returned |
| `sgStatusId` | `id` | **[NEW]** `SG_ISSUED`, `SG_REDEEMED` |

#### 3.1.7 ImportLcSettlement [NEW] (REQ-IMP-SPEC-04)

| Field | Type | Notes |
|:---|:---|:---|
| `settlementId` | `id` (PK) | — |
| `instrumentId` | `id` | FK to ImportLetterOfCredit |
| `presentationId` | `id` | FK to TradeDocumentPresentation |
| `valueDate` | `date` | Funds availability date |
| `principalAmount` | `number-decimal` | Must equal accepted claim amount |
| `remittanceCurrency` | `id` | Must match LC currency |
| `debitAccountId` | `text-medium` | Applicant's CASA account |
| `appliedMarginAmount` | `number-decimal` | Cash collateral utilized |
| `fxRate` | `number-decimal` | If cross-currency |
| `forwardContractRef` | `text-medium` | Pre-booked FX contract ref |
| `chargesDetailEnumId` | `id` | OUR, BEN, SHA (Tag 71A) |
| `settlementStatusId` | `id` | PENDING, EXECUTED |

**Gap closed:** G26 — Full settlement data dictionary.

### 3.2 LC Business State Machine (REQ-IMP-STATE-01, REQ-IMP-STATE-02)

**All 9 states from the BRD:**

```
businessStateId values:
  LC_DRAFT           → State 1
  LC_PENDING         → State 2 (Pending Approval)
  LC_ISSUED          → State 3
  LC_DOCS_RECEIVED   → State 4
  LC_DISCREPANT      → State 5
  LC_ACCEPTED        → State 6 (Accepted/Clean)
  LC_SETTLED         → State 7
  LC_CLOSED          → State 8 (Closed)
  LC_CANCELLED       → State 8 (Cancelled)
  LC_AMENDMENT_PENDING       → State 9 (Amendment Pending)
```

**Note:** when LC is in `LC_AMENDMENT_PENDING`, not allow to create new amendment, but LC is still active same as `LC_ISSUED` status.

**Permitted Transitions:**

| From | To | Trigger Service | Entry Criteria |
|:---|:---|:---|:---|
| `LC_DRAFT` | `LC_PENDING` | `submit#ForApproval` | All mandatory fields populated, validations pass |
| `LC_DRAFT` | `LC_CANCELLED` | `update#Cancellation` | User discards draft |
| `LC_PENDING` | `LC_ISSUED` | `authorize#Instrument` | Checker approves; MT 700 generated |
| `LC_PENDING` | `LC_DRAFT` | `reject#ToMaker` | Checker returns with rejection reason |
| `LC_PENDING` | `LC_CANCELLED` | `update#Cancellation` | Checker permanently declines |
| `LC_ISSUED` | `LC_PENDING` | `create#Amendment` | Amendment initiated, new approval cycle |
| `LC_ISSUED` | `LC_DOCS_RECEIVED` | `create#Presentation` | Documents arrive |
| `LC_ISSUED` | `LC_CLOSED` | `batch#AutoExpiry` | Expiry + mail days grace elapsed |
| `LC_DOCS_RECEIVED` | `LC_ACCEPTED` | `authorize#Presentation` | Clean examination authorized |
| `LC_DOCS_RECEIVED` | `LC_DISCREPANT` | `authorize#Presentation` | Discrepant examination authorized |
| `LC_DISCREPANT` | `LC_ACCEPTED` | `update#PresentationWaiver` | Applicant waives, bank accepts |
| `LC_DISCREPANT` | `LC_CLOSED` | `update#PresentationRefusal` | Applicant refuses; docs returned |
| `LC_ACCEPTED` | `LC_SETTLED` | `create#ImportLcSettlement` | Sight: immediate. Usance: maturity date |
| `LC_SETTLED` | `LC_ISSUED` | `return#ToIssued` | Partial draw — available balance remains |
| `LC_SETTLED` | `LC_CLOSED` | Auto (triggered by settlement) | Fully drawn / within tolerance |
| `LC_CLOSED` | *(none)* | — | Terminal state |
| `LC_CANCELLED` | *(none)* | — | Terminal state |
| `LC_AMENDMENT_PENDING` | `LC_ISSUED` | business Consent approved/rejected (beneficiaryConsentStatusId changed from PENDING to ACCEPTED/REJECTED)| Checker approves; MT xxx (to be defined) received |

**State enforcement service [NEW]:** `ImportLcLifecycleServices.transition#BusinessState`
* Input: `instrumentId`, `targetStateId`, `triggeredBy`
* Validates that the transition is permitted per the matrix above
* Returns error if transition is not allowed
* Creates `TradeTransactionAudit` record for every transition

**Gap closed:** G19, G20 — All 9 states + full transition matrix.

### 3.3 Process Service Designs

#### 3.3.1 Issuance (REQ-IMP-SPEC-01)
**Existing services:** `ImportLcServices.create#ImportLetterOfCredit`

**Post-Authorization Processing [NEW]:** `ImportLcServices.execute#IssuancePostAuth`
1. `LimitServices.update#Utilization` — earmark `maxLiabilityAmount`
2. `evaluate#CashMargin` — if `mandatoryMarginPercent > 0`, hold on deposit account
3. `TradeAccountingServices.calculate#Fees` → `post#TradeEntry` — issuance commission
4. `SwiftGenerationServices.generate#Mt700` — dispatch MT 700
5. Transition `businessStateId` → `LC_ISSUED`

**Computed fields in response:**
* `maxLiabilityAmount = amount × (1 + tolerancePositive)`
* `limitCheckStatus = availableEarmark >= maxLiabilityAmount`

**Report generation:**
* `TradeReportServices.generate#CustomerAdvice` — PDF advice
* `TradeReportServices.generate#GlVoucher` — accounting voucher

**Gap closed:** G21, G23.

#### 3.3.2 Amendments (REQ-IMP-SPEC-02)
**Existing service:** `ImportLcServices.create#Amendment`

**Enhancements needed:**
1. Auto-increment `amendmentNumber` per parent LC
2. Auto-categorize `isFinancial` based on fields changed (amount, tolerance, expiry)
3. `newTotalAmount = originalAmount + amountAdjustment`
4. `newMaxLiability = newTotalAmount × (1 + newTolerance)`
5. If financial: 
  - route through `evaluate#MakerCheckerMatrix` using `newMaxLiability`
  - Transition `businessStateId` → LC_AMENDMENT_PENDING
6. If non-financial: default to Tier 1
7. Post-authorization: update facility delta, apply amendment fees
8. `SwiftGenerationServices.generate#Mt707` **[NEW]** — dispatch MT 707
9. Track `beneficiaryConsentStatusId` (PENDING → ACCEPTED/REJECTED): 
  - transition `businessStateId`: LC_AMENDMENT_PENDING → LC_ISSUED
  - if ACCEPTED: update ImportLetterOfCredit values with amendment values from `TradeInstrument`

**Gap closed:** G24.

#### 3.3.3 Document Presentation (REQ-IMP-SPEC-03)
**Existing service:** `ImportLcServices.create#Presentation`

**Enhanced workflow:**
1. **Lodgement:** Create `TradeDocumentPresentation` + items (existing)
2. **SLA Timer:** Compute `regulatoryDeadline = presentationDate + 5 banking days` via `calculate#BusinessDate`
3. **Examination:** Maker logs `PresentationDiscrepancy` records with ISBP codes
4. **Submission:** Maker calls `submit#PresentationExam` — sets presentation `lifecycleStatusId = INST_PENDING_APPROVAL`
5. **Checker Review:** Checker independently verifies
6. **Decision:**
   * Clean → `businessStateId = LC_ACCEPTED`
   * Discrepant → `businessStateId = LC_DISCREPANT` + generate applicant waiver notice
7. **SWIFT messages [NEW]:**
   * `generate#Mt734` — Notice of Refusal (if bank formally refuses)
   * `generate#Mt750` — Advice of Discrepancy (if applicant delays waiver)
   * `generate#Mt752` — Authorization to Pay (if discrepancies waived, confirming acceptance)
8. **Post-authorization:** Limit reclassification from contingent → firm liability
9. **Discrepancy fee:** Auto-calculate and deduct standard fee

**Gap closed:** G25.

#### 3.3.4 Settlement & Payment (REQ-IMP-SPEC-04)
**Existing service:** `TradeAccountingServices.create#ImportLcSettlement`

**Enhanced settlement service:** `ImportLcServices.settle#Presentation`
1. **Sight LCs:** Triggered immediately upon `LC_ACCEPTED`
2. **Usance LCs:** Tracked by maturity queue
   * On acceptance: `generate#Mt732` **[NEW]** — Advice of Discharge
   * On maturity date: `batch#UsanceMaturityQueue` auto-queues settlement
3. **Settlement data entry:** Maker inputs FX rate, debit account, routing (stored in `ImportLcSettlement`)
4. **Post-authorization:**
   * Liability reversal
   * Margin release (if `appliedMarginAmount > 0`)
   * Core debit of `totalDebitAmount`
   * Parent LC update: if remaining balance = 0 → `transition#BusinessState(LC_CLOSED)`
   * If partial draw → `transition#BusinessState(LC_ISSUED)`
5. **SWIFT generation [NEW]:**
   * `generate#Mt202` — Bank-to-bank transfer
   * `generate#Mt103` — Customer credit transfer
6. **Reports:**
   * Customer Debit Advice (PDF)
   * Payment GL Voucher

**Gap closed:** G26.

#### 3.3.5 Shipping Guarantees (REQ-IMP-SPEC-05)
**Existing service:** `ImportLcServices.create#ShippingGuarantee`

**Enhanced workflow:**
1. **Limit assessment:** Calculate `sgLiabilityAmount = invoiceAmount × (liabilityMultiplier / 100)` (existing)
2. **Waiver lock:** Set `waiverLockFlag = Y` on parent LC — prevents applicant refusal for this invoice amount
3. **Commission:** Calculate SG issuance fee (flat + recurring monthly %)
4. **Redemption [NEW]:** `ImportLcServices.redeem#ShippingGuarantee`
   * When subsequent document presentation arrives for this `transportDocReference`:
     * Auto-set `applicantDecisionEnumId = WAIVED` (bypass applicant decision)
     * Set `sgStatusId = SG_REDEEMED`, record `redemptionDate`
     * Release SG-specific limit earmark
5. **Report generation:** SG Indemnity Document (PDF)

**Gap closed:** G27.

#### 3.3.6 Cancellations (REQ-IMP-SPEC-06)
**Existing service:** `ImportLcServices.update#Cancellation`

**Three closure scenarios:**
1. **Auto-Closure (Fully Drawn):** Triggered by `settle#Presentation` when `remainingBalance = 0`
2. **Auto-Expiry (Unutilized):**
   * **Service [NEW]:** `ImportLcBatchServices.batch#AutoExpiry`
   * EOD job: finds LCs where `expiryDate + mailDaysGracePeriod < today`
   * `mailDaysGracePeriod` from `TradeConfig` (default 15 calendar days)
   * Auto-transitions to `LC_CLOSED`, releases limits
3. **Early Mutual Cancellation:**
   * Requires `beneficiaryConsentRef` — incoming SWIFT MT 799/730 reference
   * Until consent received, cancellation stays in `PENDING` state
   * Post-authorization: release limits, margins, generate Cancellation Advice PDF

**SWIFT generation [NEW]:**
* `generate#Mt799` — Request for beneficiary consent (or MT 707 with amount decreased to zero)

**Gap closed:** G28.

### 3.4 Validation Rules (REQ-IMP-04)

| Rule | Service | Logic |
|:---|:---|:---|
| Tolerance Limit Check | `evaluate#Drawing` | `totalDrawn + claimAmount ≤ amount × (1 + tolerancePositive)` |
| Expiry Date Block | `create#Presentation` | `presentationDate > expiryDate` → error |
| Revolving Auto-Reinstatement | `settle#Presentation` **[ENHANCED]** | If `TradeProductCatalog.allowRevolving = Y` → reinstate `outstandingAmount` after settlement, until cumulative max |
| Regulatory Reporting | `flag#RegulatoryExport` **[NEW]** | Vietnam-processed imports → auto-flag goods codes for SBV FX outflow report |

**Gap closed:** G29.

---

## 4. SWIFT Messaging Services

**Reference:** Detailed BRD-level tag mappings in [MT700-generation.md](file:///Users/me/myprojects/moqui-trade/docs/superpowers/specs/MT-message/MT700-generation.md) and [MT-others.md](file:///Users/me/myprojects/moqui-trade/docs/superpowers/specs/MT-message/MT-others.md).

All message outputs are stored in the `SwiftMessage` entity with `messageType` and `messageContent`.

### 4.0 SWIFT Compliance: Dual-Layer Validation Strategy

SWIFT has strict constraints on character sets, field lengths, and line breaks. Rather than catching violations only at message generation time (when the user has already submitted and the transaction is in the Checker queue), the system enforces SWIFT compliance at **two layers**:

#### Layer 1: Data Capture Validation (Entity + UI)
Validates fields at the moment the Maker enters data, providing **immediate inline feedback**:

* **Entity-level constraints:** SWIFT-bound fields on `ImportLetterOfCredit` and `TradeParty` use Moqui validation rules to enforce character set and length limits at persistence time. For example:
  - `transactionRef`: max 16 chars, X Character Set, no leading/trailing `/`, no `//`
  - `goodsDescription`, `documentsRequired`, `additionalConditions`: X Character Set only
  - `beneficiaryName`/`registeredAddress`: max 4 lines × 35 chars (SWIFT `4x35` format)
  - `portOfLoading`, `portOfDischarge`: max 65 chars
* **Service-level validation during save:** `ImportLcValidationServices.validate#SwiftFields` — called on every Maker save/submit. Scans all SWIFT-mapped text fields and returns specific, field-level error messages (e.g., "Description of Goods contains invalid character '@' at position 142"). This gives Makers the opportunity to fix issues before the transaction enters the approval queue.
* **Frontend enforcement:** The SPA applies character filters and length counters on SWIFT-bound inputs in real time, stripping or warning on invalid characters as the user types.

#### Layer 2: Generation-Time Validation (Safety Net)
Re-validates all fields immediately before assembling the SWIFT message block:

* **Service:** `SwiftGenerationServices.format#XCharacter` — strips/converts any remaining invalid characters as a defense-in-depth measure.
* **Final field-length checks** — the Prowide WIFE library enforces tag-level constraints during assembly and throws errors on non-compliant data.
* **Logging:** Any character that is stripped or converted at this layer is logged as a warning to aid operational investigation, since it indicates data that bypassed Layer 1.

#### Validation Rules (Both Layers)

* **X Character Set:** `A-Z`, `a-z`, `0-9`, `- / ? : ( ) . , ' +` and spaces. Invalid characters (`@`, `&`, `_`, `#`) are blocked (Layer 1) or auto-converted (Layer 2: `&` → `AND`).
* **Z Character Set:** Used only for Tags 79 (MT 707, MT 799). Wider range: allows `@`, `#`, `=` in addition to X chars.
* **Slash Rules:** No reference field may start/end with `/` or contain `//`.
* **Decimal Format:** Financial amounts use comma `,` as decimal separator (Tag 32B: `USD50000,00`).
* **Line Wrapping:** Narrative fields auto-wrap at 65 chars/line (Tags 45A/46A/47A) or 50 chars/line (Tag 79).

### 4.1 MT 700: Issue of a Documentary Credit (REQ-IMP-SWIFT-01 through 05)
**Service:** `SwiftGenerationServices.generate#Mt700`
**Trigger:** Checker authorizes LC Issuance (Process §3.3.1)

The existing implementation produces a skeletal message. The comprehensive design uses the **Prowide WIFE** library.

**Tag mapping** covers all blocks from the BRD:
* **Block A (Header):** Tags 27, 40A, 20, 31C, 31D
* **Block B (Parties):** Tags 50, 59/59A, 51A — with BIC vs Name/Address toggle
* **Block C (Financials):** Tags 32B, 39A, 39B, 39C, 41a, 42C, 42A, 42M, 42P
* **Block D (Shipping):** Tags 43P, 43T, 44A, 44B, 44C, 44D, 44E, 44F
* **Block E (Narratives):** Tags 45A, 46A, 47A, 71D, 48, 49
* **Block F (Routing):** Tags 53a, 57a
* **Block G (Admin):** Tags 23, 72Z, 78

**Supporting services:**
* `format#Tag` — handles 32B (comma decimal), 39A (tolerance concatenation), 59/59A (BIC toggle)
* `split#Rows` — 65-character line wrapping for Tags 45A, 46A, 47A

### 4.2 MT 701: Issue of a Documentary Credit (Continuation) [NEW]
**Service:** `SwiftGenerationServices.generate#Mt701`
**Trigger:** Auto-generated during MT 700 generation when Tags 45A, 46A, or 47A exceed 100 lines of 65 characters.

| Tag | Description | M/O | Data Source | Validation |
|:---|:---|:---|:---|:---|
| **27** | Sequence of Total | M | `2/2` or `2/3` | Format: `Digit/Digit`. Max 5 chars. |
| **20** | Documentary Credit No. | M | `TradeInstrument.transactionRef` | Max 16 chars. Must exactly match Tag 20 of parent MT 700. |
| **45B** | Description of Goods | O | Overflow from MT 700 Tag 45A | Max 100 lines × 65 chars. X Character Set. |
| **46B** | Documents Required | O | Overflow from MT 700 Tag 46A | Max 100 lines × 65 chars. X Character Set. |
| **47B** | Additional Conditions | O | Overflow from MT 700 Tag 47A | Max 100 lines × 65 chars. X Character Set. |

**Logic:** During `generate#Mt700`, if any narrative exceeds `100 × 65` characters, the service splits the content and auto-generates an MT 701 continuation. The Tag 27 of the original MT 700 becomes `1/2`.

### 4.3 MT 707: Amendment to a Documentary Credit [NEW]
**Service:** `SwiftGenerationServices.generate#Mt707`
**Trigger:** Checker authorizes an Amendment (Process §3.3.2)

Only fields that are changing are populated in the message.

| Tag | Description | M/O | Data Source | Validation |
|:---|:---|:---|:---|:---|
| **20** | Sender's Reference | M | `TradeInstrument.transactionRef` | Max 16 chars. No invalid slashes. |
| **21** | Receiver's Reference | M | Advising Bank's ref or `NONREF` | Max 16 chars. No invalid slashes. |
| **31C** | Date of Issue | M | Original `TradeInstrument.issueDate` | Format: `YYMMDD`. Cannot be future date. |
| **30** | Date of Amendment | M | `ImportLcAmendment.amendmentDate` | Format: `YYMMDD`. |
| **32B** | Increase of LC Amount | O | `ImportLcAmendment.amountAdjustment` (if positive) | 3-letter CCY + Amount (max 15 digits incl. comma). |
| **33B** | Decrease of LC Amount | O | `ImportLcAmendment.amountAdjustment` (if negative) | 3-letter CCY + Amount. |
| **34B** | New LC Amount | O | Computed: `originalAmount + amountAdjustment` | 3-letter CCY + Amount. Must equal Original +/- delta. |
| **31E** | New Expiry Date | O | `ImportLcAmendment.newExpiryDate` | Format: `YYMMDD`. |
| **79** | Narrative (Amendment Details) | O | `ImportLcAmendment.amendmentNarrative` | Max 35 lines × 50 chars. **Z Character Set** (wider range: allows `@`, `#`, `=`). |

### 4.4 MT 750: Advice of Discrepancy [NEW]
**Service:** `SwiftGenerationServices.generate#Mt750`
**Trigger:** Checker marks presentation as Discrepant, bank seeks Applicant instructions (Process §3.3.3)

| Tag | Description | M/O | Data Source | Validation |
|:---|:---|:---|:---|:---|
| **20** | Sender's Reference | M | `TradeDocumentPresentation.presentationId` (formatted as ref) | Max 16 chars. No invalid slashes. |
| **21** | Related Reference | M | `TradeDocumentPresentation.presentingBankRef` | Max 16 chars. |
| **32B** | Principal Amount Claimed | M | `TradeDocumentPresentation.claimAmount` | 3-letter CCY + Amount (max 15 digits). |
| **77J** | Discrepancies | M | `PresentationDiscrepancy` records joined | Max 70 lines × 50 chars. X Character Set. Auto-wrap. |
| **72Z** | Sender to Receiver Info | O | Standard waiver status text | Max 6 lines × 35 chars. May include SWIFT code words (`/TELEMAC/`). |

### 4.5 MT 734: Notice of Refusal [NEW]
**Service:** `SwiftGenerationServices.generate#Mt734`
**Trigger:** Applicant refuses to waive discrepancies, or bank refuses due to compliance violations (Process §3.3.3)

| Tag | Description | M/O | Data Source | Validation |
|:---|:---|:---|:---|:---|
| **20** | Sender's Reference | M | `TradeInstrument.transactionRef` | Max 16 chars. |
| **21** | Presenting Bank's Ref | M | `TradeDocumentPresentation.presentingBankRef` | Max 16 chars. |
| **32A** | Value Date & Amount | M | Presentation date + `claimAmount` | Format: `YYMMDD` + 3-letter CCY + Amount. |
| **73** | Charges Deducted | O | Calculated discrepancy fee | Max 6 lines × 35 chars. X Character Set. |
| **77J** | Discrepancies | M | `PresentationDiscrepancy` records joined | Max 70 lines × 50 chars. X Character Set. |
| **77B** | Disposal of Documents | M | Enum: `HOLDING DOCUMENTS`, `RETURNING DOCUMENTS` | Max 3 lines × 35 chars. Must clearly state legal status of physical docs. |

### 4.6 MT 752: Authorization to Pay, Accept or Negotiate [NEW]
**Service:** `SwiftGenerationServices.generate#Mt752`
**Trigger:** Discrepant presentation transitions to Accepted/Clean after authorized waiver (Process §3.3.3)

| Tag | Description | M/O | Data Source | Validation |
|:---|:---|:---|:---|:---|
| **20** | Documentary Credit No. | M | `TradeInstrument.transactionRef` | Max 16 chars. |
| **21** | Presenting Bank's Ref | M | `TradeDocumentPresentation.presentingBankRef` | Max 16 chars. |
| **30** | Date of Advice | M | System Business Date | Format: `YYMMDD`. |
| **32B** | Amount Advised | M | `TradeDocumentPresentation.claimAmount` | 3-letter CCY + Amount (max 15 digits). |
| **72Z** | Sender to Receiver Info | O | `"DISCREPANCIES WAIVED..."` | Max 6 lines × 35 chars. |

### 4.7 MT 732: Advice of Discharge [NEW]
**Service:** `SwiftGenerationServices.generate#Mt732`
**Trigger:** Usance LC presentation enters Accepted state (Process §3.3.4). Generated before money moves — acknowledges future maturity date commitment.

| Tag | Description | M/O | Data Source | Validation |
|:---|:---|:---|:---|:---|
| **20** | Sender's Reference | M | `TradeInstrument.transactionRef` | Max 16 chars. |
| **21** | Presenting Bank's Ref | M | `TradeDocumentPresentation.presentingBankRef` | Max 16 chars. |
| **30** | Date of Advice | M | System Business Date | Format: `YYMMDD`. |
| **32A** | Value Date & Amount | M | Maturity date + accepted amount | Format: `YYMMDD` + 3-letter CCY + Amount. The date **must** be the future maturity date. |

### 4.8 MT 799: Free Format Message [NEW]
**Service:** `SwiftGenerationServices.generate#Mt799`
**Trigger:** Early cancellation — requesting Beneficiary's mutual consent (Process §3.3.6)

| Tag | Description | M/O | Data Source | Validation |
|:---|:---|:---|:---|:---|
| **20** | Transaction Reference | M | `TradeInstrument.transactionRef` | Max 16 chars. |
| **21** | Related Reference | M | Advising Bank's ref or `NONREF` | Max 16 chars. |
| **79** | Narrative | M | System-generated cancellation request text | Max 35 lines × 50 chars. **Z Character Set**. System must enforce line wrapping. |

### 4.9 Settlement Messages (Category 1 & 2) [NEW]
Actual money movement uses Category 1/2 messages, generated during Process §3.3.4 (Settlement).

#### MT 202: General Financial Institution Transfer
**Service:** `SwiftGenerationServices.generate#Mt202`
**Trigger:** Settlement — bank-to-bank reimbursement via correspondent Nostro/Vostro accounts.

| Tag | Description | M/O | Data Source |
|:---|:---|:---|:---|
| **20** | Transaction Reference | M | `TradeInstrument.transactionRef` |
| **21** | Related Reference | M | `TradeDocumentPresentation.presentingBankRef` |
| **32A** | Value Date, Currency, Amount | M | Settlement `valueDate` + `principalAmount` |
| **53a** | Sender's Correspondent | O | Bank's Nostro account (derived from `remittanceCurrency`) |
| **58a** | Beneficiary Institution | M | Presenting Bank's SWIFT BIC |

#### MT 103: Single Customer Credit Transfer
**Service:** `SwiftGenerationServices.generate#Mt103`
**Trigger:** Settlement — direct payment to Beneficiary's retail bank account (less common in TF).

| Tag | Description | M/O | Data Source |
|:---|:---|:---|:---|
| **20** | Transaction Reference | M | `TradeInstrument.transactionRef` |
| **32A** | Value Date, Currency, Amount | M | Settlement `valueDate` + `principalAmount` |
| **50a** | Ordering Customer | M | Applicant details from `TradeParty` |
| **59a** | Beneficiary Customer | M | Beneficiary details from `TradeParty` |
| **71A** | Details of Charges | M | `ImportLcSettlement.chargesDetailEnumId` (OUR/BEN/SHA) |

---

## 5. FRONTEND SPA APPLICATION DESIGN

### 5.1 Global UI Shell (REQ-UI-IMP-01)

**Top Navigation Bar:**
* System Context: Current Business Date, Logged-in User Profile, Active Role badge
* Global Search: Omni-search (LC refs, SWIFT refs, Applicant names) → calls `get#ImportLetterOfCreditList` with filter params

**Left Navigation Menu (REQ-UI-CMN-01):**
```
Dashboard
My Approvals (badge count)
Trade Modules
  ├── Import LC
  │     ├── New Application
  │     ├── Active LCs
  │     ├── Presentations
  │     └── Settled/Closed
  ├── Export LC (Phase 2 - locked)
  └── Collections (Phase 2 - locked)
Master Data
  ├── Party & KYC Directory
  ├── Credit Facilities (Limits)
  ├── Tariff & Fee Configuration
  └── Product Configuration
System Admin
  ├── User Authority Tiers
  └── Audit Logs
```

**Gap closed:** U1, U4.

### 5.2 Common Module UI

#### 5.2.1 Global Checker Queue (REQ-UI-CMN-02)
**Pattern:** High-Density Data Grid with Quick Filters.
**API:** `GET /rest/s1/trade/approvals?tier={}&productType={}&actionType={}`

* **Top KPI Banner:** SLA warning count, Tier indicator
* **Data Table Columns:** Module badge, Reference No, Action, Maker, Base Equiv Amount, Time in Queue
* **Row Behavior:** Full-screen overlay modal — no page navigation
* **Filters:** Product Type, Action Type, Priority

#### 5.2.2 Party & KYC Directory (REQ-UI-CMN-03)
**Pattern:** Master-Detail Split View.
* **Left Pane:** Searchable party list with KYC status color dots
* **Right Pane (Tabbed):** General Info, Roles, Compliance/AML

#### 5.2.3 Credit Facility Dashboard (REQ-UI-CMN-04)
**Pattern:** Analytics Dashboard with Drill-Down.
**API:** `GET /rest/s1/trade/facilities/{facilityId}`

* **Exposure Widget:** Horizontal progress bar (Firm/Contingent/Reserved/Available)
* **Utilization Breakdown:** All active transactions consuming the facility, hyperlinked refs

#### 5.2.4 Tariff & Fee Configuration (REQ-UI-CMN-05)
**Pattern:** Matrix / Rules Grid.
**API:** CRUD on `/rest/s1/trade/feeConfigurations`

* **Left Nav:** Fee Types list
* **Main Area:** Base rule set + Exception/Tier pricing grid
* **Action Bar:** Save Draft / Publish (Maker/Checker controlled)

**Gap closed:** U2.

#### 5.2.5 Product Configuration (REQ-UI-CMN-06)
**Pattern:** Matrix / Settings Grid.
**API:** CRUD on `/rest/s1/trade/productCatalog`

* **Left Nav:** Product Types list
* **Main Area:** Configuration flags per REQ-COM-PRD-01
* **Action Bar:** Save Draft / Publish (Maker/Checker controlled)

**Gap closed:** U3.

### 5.3 Import LC UI

#### 5.3.1 Import LC Dashboard (REQ-UI-IMP-02)
**Pattern:** KPI Widgets + Active Transaction Grid.
**API:** `GET /rest/s1/trade/importLc/kpis`, `GET /rest/s1/trade/importLcs`

* **KPI Cards:** Drafts awaiting submission, LCs expiring within 7 days, Discrepant presentations awaiting waiver
* **Data Table:** Filterable by status, date range, applicant. Columns: Ref, Applicant, Beneficiary, CCY, Amount, Expiry, Status, SLA Timer
* **Row Action Menu:** Context-aware (Initiate Amendment, Log Presentation, Cancel)

**Gap closed:** U5.

#### 5.3.2 LC Issuance Data Entry Stepper (REQ-UI-IMP-03)
**Pattern:** Horizontal Stepper (4 steps).

* **Header Banner (Sticky):** Draft Ref, Status badge, dynamic Base Equivalent Amount

* **Step 1: Basic Information**
  * LC Info: Type, Number (auto), Reference (auto), Status (auto), Product (dropdown linked to `TradeProductCatalog`)
  * Parties: Applicant (autocomplete with facility/KYC widget), Beneficiary (4x35 text)

* **Step 2: Main LC Information**
  * Financials & Dates: Currency, Amount, Tolerance +/-, Issue Date, Expiry Date
  * Terms & Shipping: Partial Shipments, Transhipment (radio), Ports, Latest Shipment Date
  * Narratives: Description of Goods, Documents Required, Additional Conditions (with "Standard Clauses" insert button)
  * **Right Navigation:** Section anchors for in-step scrolling

* **Step 3: Margin & Charges**
  * Margin: Type, Percentage, Amount (auto-calculated), Debit Account
  * Charges: Type, Rate (from FeeConfiguration), Amount (auto-calculated), Debit Account

* **Step 4: Review & Submit**
  * Read-only summary + Validation panel (Limit ✓, Sanctions ✓, KYC ✓)
  * Primary Action: **[Submit for Approval]**

**Gap closed:** U6, U7.

#### 5.3.3 Document Examination (REQ-UI-IMP-04)
**Pattern:** 50/50 Vertical Split-Pane.

* **Left Pane:** Read-only issued LC terms (accordion: Financials, Shipping, Required Documents)
* **Right Pane:** Presentation header, Document Grid (type + counts), Discrepancy Logger (ISBP code dropdown + free text)
* **Footer:** Save Draft / Submit Examination

#### 5.3.4 Checker Authorization Screen (REQ-UI-IMP-05)
**Pattern:** Action bar + Risk summary (30%) + Transaction data (70%).

* **Action Bar:** AUTHORIZE (green), REJECT TO MAKER (red), SEND TO COMPLIANCE (yellow)
* **Left Column:** Maker details, Limit progress bar (showing this transaction's impact), Screening badges
* **Right Column:** Read-only transaction data. **Amendment delta highlighting:** struck-through old values, green-highlighted new values
* **Rejection Modal:** Mandatory reason text before routing back to Draft

---

## 6. Verification Plan

### 6.1 Existing Test Infrastructure
* Framework: Spock (Groovy) under `src/test/groovy/moqui/trade/finance/`
* Existing specs: `BddCommonModuleSpec.groovy`, `BddImportLcModuleSpec.groovy`
* Run: `./gradlew test` from project root

### 6.2 Service-Layer Verification
Each new/modified service will be verified via Spock BDD tests:
* State machine transitions: test every permitted + rejected transition
* Authorization tiers: test Tier 1-4 routing, dual-checker, compliance hold
* Validation rules: KYC block, sanctions hold, limit block, date logic
* SWIFT generation: verify tag formatting for all message types
* Settlement: sight + usance + partial draw + full draw closure

### 6.3 Frontend Verification
* Browser-based smoke tests for each screen wireframe
* All API endpoints exercised via REST calls
* UI stepper: verify step navigation, validation inline errors, dynamic field hiding based on product config

---

## Appendix A: BRD Traceability Matrix

| Gap ID | BRD Requirement | Design Section | Status |
|:---|:---|:---|:---|
| G1 | REQ-COM-ENT-02 (Trade Party) | §2.1.2 | ✅ Closed |
| G2 | REQ-COM-WF-01 (Processing Flow) | §1.3 | ✅ Closed |
| G3 | REQ-COM-FX-01 (Currency Precision) | §2.2.1 | ✅ Closed |
| G4 | REQ-COM-SLA-02 (Overdue Enforcement) | §2.2.4 | ✅ Closed |
| G5 | REQ-COM-NOT-02 (Compliance Holds) | §2.3.2 | ✅ Closed |
| G6 | REQ-COM-VAL-01 (KYC Validation) | §2.4.1 | ✅ Closed |
| G7 | REQ-COM-VAL-01 (Sanctions Hold) | §2.4.1 | ✅ Closed |
| G8 | REQ-COM-VAL-01 (Limit Availability) | §2.4.1 | ✅ Closed |
| G9 | REQ-COM-VAL-02 (Four-Eyes) | §2.4.2 | ✅ Closed |
| G10 | REQ-COM-VAL-02 (Immutability) | §2.4.2 | ✅ Closed |
| G11 | REQ-COM-VAL-03 (Date/Currency Logic) | §2.4.3 | ✅ Closed |
| G12 | REQ-COM-AUTH-01 (Tier Limits) | §2.5.1 | ✅ Closed |
| G13 | REQ-COM-AUTH-02 (Dual-Checker) | §2.5.2 | ✅ Closed |
| G14 | REQ-COM-AUTH-03 (Special Auth) | §2.5.3 | ✅ Closed |
| G15 | REQ-COM-MAS-02 (User Authority) | §2.1.7 | ✅ Closed |
| G16 | REQ-COM-MAS-03 (Reports) | §2.6 | ✅ Closed |
| G17 | REQ-COM-MAS-01 (Fee Workflow) | §2.1.5 | ✅ Closed |
| G18 | REQ-COM-PRD-02 (Product Impact) | §2.1.6 | ✅ Closed |
| G19 | REQ-IMP-02 (Missing States) | §3.2 | ✅ Closed |
| G20 | REQ-IMP-STATE-01/02 (Transition Matrix) | §3.2 | ✅ Closed |
| G21 | REQ-IMP-SPEC-01 (Issuance Workflow) | §3.3.1 | ✅ Closed |
| G22 | REQ-IMP-SPEC-01 (Data Dictionary) | §3.1.1 | ✅ Closed |
| G23 | REQ-IMP-SPEC-01 (Post-Submit) | §3.3.1 | ✅ Closed |
| G24 | REQ-IMP-SPEC-02 (Amendments) | §3.3.2 | ✅ Closed |
| G25 | REQ-IMP-SPEC-03 (Doc Presentation) | §3.3.3 | ✅ Closed |
| G26 | REQ-IMP-SPEC-04 (Settlement) | §3.3.4 | ✅ Closed |
| G27 | REQ-IMP-SPEC-05 (Shipping Guarantees) | §3.3.5 | ✅ Closed |
| G28 | REQ-IMP-SPEC-06 (Cancellations) | §3.3.6 | ✅ Closed |
| G29 | REQ-IMP-04 (Validation Rules) | §3.4 | ✅ Closed |
| U1 | REQ-UI-CMN-01 (Left Nav) | §5.1 | ✅ Closed |
| U2 | REQ-UI-CMN-05 (Tariff Config UI) | §5.2.4 | ✅ Closed |
| U3 | REQ-UI-CMN-06 (Product Config UI) | §5.2.5 | ✅ Closed |
| U4 | REQ-UI-IMP-01 (Global Shell) | §5.1 | ✅ Closed |
| U5 | REQ-UI-IMP-02 (Dashboard) | §5.3.1 | ✅ Closed |
| U6 | REQ-UI-IMP-03 Step 3 (Margin) | §5.3.2 | ✅ Closed |
| U7 | REQ-UI-IMP-03 Right Nav | §5.3.2 | ✅ Closed |
