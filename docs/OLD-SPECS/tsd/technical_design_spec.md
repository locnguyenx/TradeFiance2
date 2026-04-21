---
Document ID: TSD-001
Version: 0.1
Related BRD Version: 0.1
Module: Trade Finance
Feature: Letter of Credit (LC)
Status: DRAFT
Last Updated: 2026-03-11
Author: [LocNX]
---
# Technical Design Specification (TDS) - Trade Finance System

> **Purpose**: This document is a complete technical blueprint for rebuilding the Trade Finance system from scratch on the Moqui Framework. Combined with the BRD (`docs/brd/`) and Implementation Plan (`.opencode/state/implementation_plan.md`), it provides all information needed.

---

## 1. Moqui Setup & Bootstrap

### 1.1 Prerequisites
- Java 21 (JDK)
- Git
- Gradle (provided by Moqui's `gradlew` wrapper)

### 1.2 Framework Setup
```bash
# Clone Moqui Framework
git clone https://github.com/moqui/moqui-framework.git moqui-trade-finance
cd moqui-trade-finance

# Get runtime and Mantle
./gradlew getRuntime
./gradlew getComponent -Pcomponent=mantle-udm
./gradlew getComponent -Pcomponent=mantle-usl
```

### 1.3 Component Creation
```bash
mkdir -p runtime/component/TradeFinance
```

Create `runtime/component/TradeFinance/component.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<component name="TradeFinance" version="1.0.0"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="http://moqui.org/xsd/component-3.0.xsd">
</component>
```

### 1.4 Directory Structure
```
runtime/component/TradeFinance/
├── component.xml
├── data/
│   ├── 10_TradeFinanceData.xml        (seed: statuses, enums, jobs)
│   ├── 20_TradeFinanceSecurityData.xml (seed: roles, authz)
│   ├── 30_TradeFinanceDemoData.xml     (demo: users, parties, LCs)
│   └── 40_TradeFinanceTestDemoData.xml (demo: test-specific data)
├── entity/
│   └── TradeFinanceEntities.xml
├── service/moqui/trade/finance/
│   ├── TradeFinanceServices.xml
│   ├── AmendmentServices.xml
│   ├── DrawingServices.xml
│   ├── FinancialServices.xml
│   ├── LifecycleServices.xml
│   ├── SwiftServices.xml
│   ├── CbsIntegrationServices.xml
│   ├── AccountingServices.xml
│   ├── NotificationServices.xml
│   ├── ScheduledServices.xml
│   └── DocumentServices.xml
├── screen/
│   └── TradeFinance.xml               (root screen)
│       └── TradeFinance/
│           ├── Home.xml
│           ├── ImportLc.xml
│           │   └── ImportLc/
│           │       ├── Dashboard.xml
│           │       ├── TaskQueue.xml
│           │       ├── Lc.xml         (wrapper)
│           │       │   └── Lc/
│           │       │       ├── FindLc.xml
│           │       │       ├── MainLC.xml
│           │       │       ├── Financials.xml
│           │       │       ├── Amendments.xml
│           │       │       ├── Drawings.xml
│           │       │       └── History.xml
│           │       ├── Amendment.xml   (wrapper)
│           │       │   └── Amendment/
│           │       │       ├── FindAmendment.xml
│           │       │       ├── AmendmentDetail.xml
│           │       │       ├── Financials.xml
│           │       │       └── History.xml
│           │       └── Drawing.xml     (wrapper)
│           │           └── Drawing/
│           │               ├── FindDrawing.xml
│           │               └── DrawingDetail.xml
│           ├── ExportLc.xml           (placeholder)
│           ├── ImportCollection.xml   (placeholder)
│           └── ExportCollection.xml   (placeholder)
└── src/test/groovy/
    ├── TradeFinanceServicesSpec.groovy
    ├── TradeFinanceScreensSpec.groovy
    ├── TradeFinanceDrawingFlowSpec.groovy  <!-- NEW: Exhaustive drawing tests -->
    ├── TradeFinanceCbsSpec.groovy          <!-- NEW: CBS Integration simulator tests -->
    ├── TradeFinancePhase2Spec.groovy
    ├── TradeFinancePhase3Spec.groovy
    └── TradeFinancePhase4Spec.groovy
```

### 1.5 Root Screen
`screen/TradeFinance.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<screen xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="http://moqui.org/xsd/xml-screen-3.0.xsd"
        require-authentication="false">
    <always-actions><set field="appRoot" value="trade-finance"/></always-actions>
    <subscreens default-item="Home" always-use-full-path="true"/>
    <widgets>
        <subscreens-panel id="TradeFinancePanel" type="popup" title="Trade Finance"/>
    </widgets>
</screen>
```

### 1.6 Build & Run Commands
```bash
# Load data and run
./gradlew load
./gradlew run

# Run tests
./gradlew cleanAll load runtime/component/TradeFinance:test

# Specific test
./gradlew cleanAll load runtime/component/TradeFinance:test --tests moqui.trade.finance.TradeFinanceScreensSpec

# NEVER use: java -jar moqui.war
```

---

## 2. Entity Specification

Package: `moqui.trade.finance`

### 2.1 LetterOfCredit (Master Entity)

| Field | Type | PK | Default | Notes |
| :--- | :--- | :---: | :--- | :--- |
| `lcId` | id | ✅ | auto | Primary key |
| `lcStatusId` | id | | | FK → StatusItem (LcLifecycle). Audit-logged. |
| `transactionStatusId` | id | | | FK → StatusItem (LcTransaction). Audit-logged. |
| `requestId` | id | | | FK → mantle.request.Request |
| `productId` | id | | | FK → LcProduct |
| `lcProductTypeEnumId` | id | | | **Deprecated**. Use `productId`. |
| `amendmentNumber` | number-integer | | 0 | Incremented on each amendment |
| `lcNumber` | text-short | | | SWIFT Tag 20 (16x max) |
| `isSecured`| text-indicator | | 'N' | 100% secured by deposit/pledge |
| `securedPercentage` | number-decimal | | | % of LC value secured |
| `creditAgreementId` | id | | | FK → Credit Agreement in CBS |
| `collateralDescription` | text-medium | | | Details of collateral (Gold, Cash, etc.) |
| `availableCreditLimit` | currency-amount | | | Retrieved from CBS |
| `sequenceTotal_27` | text-short | | | SWIFT Tag 27 (e.g., "1/1") |
| `formOfCredit_40A` | id | | | Enum: LcFormOfCredit |
| `applicableRules_40E` | id | | | Enum: LcApplicableRules |
| `issueDate` | date | | | SWIFT Tag 31C |
| `expiryDate` | date | | | SWIFT Tag 31D |
| `expiryPlace_31D` | text-medium | | | SWIFT Tag 31D place |
| `amount` | currency-amount | | | SWIFT Tag 32B amount |
| `amountCurrencyUomId` | id | | | FK → moqui.basic.Uom |
| `amountTolerance_39A` | text-short | | | Format: "n/n" (e.g., "10/10") |
| `maxCreditAmount_39B` | text-short | | | SWIFT Tag 39B |
| `additionalAmountsCovered_39C` | text-very-long | | | SWIFT Tag 39C (4×35) |
| `applicantPartyId` | id | | | FK → mantle.party.Party |
| `beneficiaryPartyId` | id | | | FK → mantle.party.Party |
| `issuingBankPartyId` | id | | | FK → mantle.party.Party |
| `advisingBankPartyId` | id | | | FK → mantle.party.Party |
| `advisingThroughBankPartyId` | id | | | FK → mantle.party.Party |
| `reimbursingBankPartyId` | id | | | FK → mantle.party.Party |
| `applicantName` | text-medium | | | SWIFT Tag 50 fallback (4×35) |
| `applicantAddress` | text-medium | | | |
| `beneficiaryName` | text-medium | | | SWIFT Tag 59 fallback (4×35) |
| `beneficiaryAddress` | text-medium | | | |
| `issuingBankName` | text-medium | | | SWIFT Tag 51a fallback |
| `issuingBankAddress` | text-medium | | | |
| `advisingBankName` | text-medium | | | SWIFT Tag 57a fallback |
| `requestedConfirmationParty_58a` | text-medium | | | SWIFT Tag 58a |
| `advisingThroughBank_57a` | text-medium | | | |
| `reimbursingBank_53a` | text-medium | | | SWIFT Tag 53a |
| `partialShipment_43P` | id | | | Enum: LcPartialShipment |
| `transhipment_43T` | id | | | Enum: LcTranshipment |
| `latestShipDate_44C` | date | | | SWIFT Tag 44C |
| `placeOfReceipt_44A` | text-medium | | | SWIFT Tag 44A (65x) |
| `portOfLoading_44E` | text-medium | | | SWIFT Tag 44E (65x) |
| `portOfDischarge_44F` | text-medium | | | SWIFT Tag 44F (65x) |
| `placeOfFinalDestination_44B` | text-medium | | | SWIFT Tag 44B (65x) |
| `descriptionOfGoods_45A` | text-very-long | | | SWIFT Tag 45A (100×65) |
| `shipmentPeriod_44D` | text-long | | | SWIFT Tag 44D (6×65) |
| `docsRequired_46A` | text-very-long | | | SWIFT Tag 46A (100×65) |
| `additionalConditions_47A` | text-very-long | | | SWIFT Tag 47A (100×65) |
| `instructionsToBank_78` | text-very-long | | | SWIFT Tag 78 (100×65) |
| `periodForPresentation_48` | text-short | | | SWIFT Tag 48 |
| `charges_71B` | text-long | | | SWIFT Tag 71B (6×35) |
| `senderToReceiverInfo_72Z` | text-long | | | SWIFT Tag 72Z (6×35) |
| `availableWithBy_41A` | id | | | Enum: LcAvailability |
| `availableWithBankName` | text-medium | | | |
| `draftsAt_42C` | text-medium | | | SWIFT Tag 42C (3×35) |
| `confirmationInstructions_49` | id | | | Enum: LcConfirmation |
| `comments` | text-very-long | | | Internal notes |

**Relationships:**
- `one` → `moqui.basic.StatusItem` as `lcStatus` (via `lcStatusId`)
- `one` → `moqui.basic.StatusItem` as `transactionStatus` (via `transactionStatusId`)
- `one` → `mantle.request.Request`
- `one` → `moqui.basic.Uom` as `currency` (via `amountCurrencyUomId`)
- `one` → `mantle.party.Party` (×4: Applicant, Beneficiary, IssuingBank, AdvisingBank)
- `one` → `moqui.trade.finance.LcProduct`
- `one` → `moqui.basic.Enumeration` (×7: FormOfCredit, ApplicableRules, Availability, PartialShipment, Transhipment, Confirmation, ProductType)
- `many` → LcHistory, LcAmendment, LcDrawing, LcCharge, LcProvision, LcDocument

### 2.2 LcHistory (Audit Trail)

| Field | Type | PK | Default | Notes |
| :--- | :--- | :---: | :--- | :--- |
| `lcHistoryId` | id | ✅ | auto | |
| `lcId` | id | | | FK → LetterOfCredit |
| `changeType` | text-short | | | "StatusChange", "Amendment", "Update" |
| `fieldName` | text-short | | | Which field changed |
| `oldValue` | text-medium | | | |
| `newValue` | text-medium | | | |
| `amendmentSeqId` | id | | | Optional FK → LcAmendment |
| `changedByUserId` | id | | | FK → UserAccount |
| `changeDate` | date-time | | `ec.user.nowTimestamp` | |
| `comments` | text-medium | | | |

### 2.3 LcAmendment (Shadow Record Model)

| Field | Type | PK | Notes |
| :--- | :--- | :---: | :--- |
| `lcId` | id | ✅ | FK → LetterOfCredit |
| `amendmentSeqId` | id | ✅ | Sequential ID |
| `amendmentNumber` | number-integer | | Display number |
| `requestId` | id | | FK → mantle.request.Request |
| `amendmentDate` | date | | |
| `amendmentStatusId` | id | | Uses LcTransaction StatusFlow. Audit-logged. |
| `confirmationStatusId` | id | | Uses LcAmendmentConfirmation StatusFlow. Audit-logged. |
| `confirmationDate` | date-time | | |
| *Shadow fields* | | | **All amendable fields from LetterOfCredit are cloned here** |
| `remarks` | text-medium | | |

**Shadow Fields** (complete clone of amendable LC fields):
`productId`, `lcProductTypeEnumId`, `formOfCredit_40A`, `applicableRules_40E`, `expiryDate`, `expiryPlace_31D`, `amount`, `amountCurrencyUomId`, `amountTolerance_39A`, `maxCreditAmount_39B`, `additionalAmountsCovered_39C`, `beneficiaryPartyId`, `beneficiaryName`, `beneficiaryAddress`, `advisingBankPartyId`, `advisingBankName`, `advisingThroughBankPartyId`, `advisingThroughBank_57a`, `reimbursingBankPartyId`, `reimbursingBank_53a`, `requestedConfirmationParty_58a`, `partialShipment_43P`, `transhipment_43T`, `latestShipDate_44C`, `placeOfReceipt_44A`, `portOfLoading_44E`, `portOfDischarge_44F`, `placeOfFinalDestination_44B`, `descriptionOfGoods_45A`, `shipmentPeriod_44D`, `docsRequired_46A`, `additionalConditions_47A`, `instructionsToBank_78`, `periodForPresentation_48`, `charges_71B`, `senderToReceiverInfo_72Z`, `availableWithBy_41A`, `availableWithBankName`, `draftsAt_42C`, `confirmationInstructions_49`, `isSecured`, `securedPercentage`, `collateralDescription`

> **Design Note**: Fields NOT cloned (immutable after issuance): `lcNumber`, `applicantPartyId`, `applicantName`, `issuingBankPartyId`, `issuingBankName`, `amountCurrencyUomId`

### 2.4 LcDrawing

| Field | Type | PK | Notes |
| :--- | :--- | :---: | :--- |
| `lcId` | id | ✅ | FK → LetterOfCredit |
| `drawingSeqId` | id | ✅ | |
| `requestId` | id | | FK → mantle.request.Request |
| `drawingDate` | date | | |
| `drawingAmount` | currency-amount | | |
| `drawingCurrencyUomId` | id | | FK → moqui.basic.Uom |
| `drawingStatusId` | id | | Uses LcDrawingStatus flow. Audit-logged. |
| `presentationDate` | date | | |
| `maturityDate` | date | | For usance/deferred LCs |
| `paymentDate` | date | | |
| `paymentAmount` | currency-amount | | |
| `paymentReference` | text-short | | |
| `documentCount` | number-integer | | |
| `remarks` | text-medium | | |

### 2.5 LcDrawingDocument

| Field | Type | PK | Notes |
| :--- | :--- | :---: | :--- |
| `lcId` | id | ✅ | |
| `drawingSeqId` | id | ✅ | |
| `documentSeqId` | id | ✅ | |
| `documentTypeEnumId` | id | | Enum: LcDocumentType |
| `documentReference` | text-short | | |
| `documentDate` | date | | |
| `originalCount` | number-integer | | |
| `copyCount` | number-integer | | |
| `remarks` | text-medium | | |

### 2.6 LcDiscrepancy

| Field | Type | PK | Notes |
| :--- | :--- | :---: | :--- |
| `lcId` | id | ✅ | |
| `drawingSeqId` | id | ✅ | |
| `discrepancySeqId` | id | ✅ | |
| `discrepancyTypeEnumId` | id | | Enum: LcDiscrepancyType |
| `description` | text-medium | | |
| `resolutionEnumId` | id | | Enum: LcDiscrepancyResolution |
| `resolutionDate` | date | | |
| `resolvedByUserId` | id | | FK → UserAccount |
| `remarks` | text-medium | | |

### 2.7 LcCharge

| Field | Type | PK | Notes |
| :--- | :--- | :---: | :--- |
| `lcId` | id | ✅ | |
| `chargeSeqId` | id | ✅ | |
| `chargeTypeEnumId` | id | | Enum: LcChargeType |
| `chargeAmount` | currency-amount | | |
| `chargeCurrencyUomId` | id | | FK → Uom |
| `chargeDate` | date | | |
| `amendmentSeqId` | id | | Optional FK → LcAmendment |
| `waived` | text-indicator | | Default 'N' |
| `cbsReference` | text-short | | CBS transaction ref |
| `remarks` | text-medium | | |

### 2.8 LcProvision

| Field | Type | PK | Notes |
| :--- | :--- | :---: | :--- |
| `lcId` | id | ✅ | |
| `provisionSeqId` | id | ✅ | |
| `provisionAmount` | currency-amount | | |
| `provisionCurrencyUomId` | id | | FK → Uom |
| `provisionRate` | number-decimal | | % of LC amount |
| `effectiveDate` | date | | |
| `releaseDate` | date | | |
| `amendmentSeqId` | id | | Optional FK → LcAmendment |
| `provisionStatusId` | id | | Uses LcProvisionStatus (Active/Released) |
| `cbsHoldReference` | text-short | | CBS hold ref |
| `remarks` | text-medium | | |

### 2.9 LcDocument (Attachments)

| Field | Type | PK | Notes |
| :--- | :--- | :---: | :--- |
| `lcId` | id | ✅ | |
| `documentSeqId` | id | ✅ | |
| `documentTypeEnumId` | id | | Enum: LcDocumentType |
| `documentReference` | text-short | | |
| `documentDate` | date | | |
| `contentLocation` | text-medium | | File path (e.g., `dbresource://...`) |
| `description` | text-medium | | |
| `uploadDate` | date-time | | Default `ec.user.nowTimestamp` |
| `uploadedByUserId` | id | | FK → UserAccount |

### 2.10 View Entities

**LcAmendmentDetailView**: Joins `LcAmendment` (alias AMND) + `LetterOfCredit` (alias LC) + `StatusItem` (alias ST)
- All fields from AMND
- `lcNumber` from LC
- `applicantPartyId` from LC
- `statusDescription` from ST (mapped from `amendmentStatusId`)

**LcDrawingDetailView**: Joins `LcDrawing` (alias DRWG) + `LetterOfCredit` (alias LC)
- All fields from DRWG
- `lcNumber` from LC
- `applicantPartyId` from LC

### 2.11 LcProduct (Settings)
| Field | Type | PK | Notes |
| :--- | :--- | :---: | :--- |
| `productId` | id | ✅ | |
| `productName` | text-medium | | e.g., "Sight LC - Standard" |
| `lcProductTypeEnumId`| id | | FK → Enumeration |
| `defaultProvisionRate`| number-decimal| | Default % for provisions |

### 2.12 LcProductCharge (Settings)
| Field | Type | PK | Notes |
| :--- | :--- | :---: | :--- |
| `productId` | id | ✅ | FK → LcProduct |
| `chargeTypeEnumId` | id | ✅ | FK → Enumeration |
| `defaultAmount` | currency-amount | | |
| `itemTypeEnumId` | id | | Mantle ItemType for GL Mapping |

### 2.13 CbsSimulatorState [NEW]
| Field | Type | PK | Default | Notes |
| :--- | :--- | :---: | :--- | :--- |
| `partyId` | id | ✅ | | FK → mantle.party.Party |
| `balanceAmount` | currency-amount | | 10000000.00 | Virtual balance |
| `holdAmount` | currency-amount | | 0.00 | Total funds currently on hold |
| `currencyUomId` | id | | 'USD' | |
| `lastUpdated` | date-time | | now | |

---

## 3. Service Specification

Package: `moqui.trade.finance`

### 3.1 TradeFinanceServices.xml

#### `validate#LetterOfCredit`
- **In**: All nonpk fields of LetterOfCredit
- **Logic**:
  1. Check SWIFT Character Set X on text fields: only `A-Za-z0-9/-?:().,'+ \r\n` allowed
  2. UCP 600: `expiryDate` must be after `issueDate`
  3. SWIFT length limits: `lcNumber` ≤ 16, `applicantName` ≤ 140, `beneficiaryName` ≤ 140, `descriptionOfGoods_45A` ≤ 6500
  4. Format validation: `amountTolerance_39A` must match `n/n` regex

#### `create#LetterOfCredit`
- **In**: All nonpk fields + `lcNumber` (required), defaults: `lcStatusId=LcLfDraft`, `transactionStatusId=LcTxDraft`
- **Out**: `lcId`, `requestId`
- **Logic**:
  1. Call `validate#LetterOfCredit`
  2. Create `mantle.request.Request` with type `RqtLcIssuance`, status `ReqDraft`
  3. Create `LetterOfCredit` entity with `requestId`
  4. **Automated Charges**: Lookup `LcProductCharge` for the selected `productId` and create `LcCharge` records.
  5. Create initial `LcHistory` entry (changeType=StatusChange)
  5. Send notification via `send#LcNotification`

#### `update#LetterOfCredit`
- **In**: PK + all nonpk fields + `comments`
- **Logic**: Validate → find entity → `entity-set` + `entity-update` → create LcHistory (changeType=Update)

#### `delete#LetterOfCredit`
- **In**: `lcId` (required)
- **Logic**: Guard: `transactionStatusId == LcTxDraft` only → cascade delete all children (LcDiscrepancy, LcDrawingDocument, LcDrawing, LcAmendment, LcCharge, LcProvision, LcDocument, LcHistory) → delete LetterOfCredit

#### `transition#LcStatus`
- **In**: `lcId`, `toStatusId`, `comments`
- **Logic**: Validate transition is allowed by `StatusFlowTransition` (flowId=LcLifecycle) → update `lcStatusId` → create LcHistory

#### `transition#TransactionStatus`
- **In**: `lcId`, `toStatusId`, `comments`
- **Logic**: Same pattern using flowId=LcTransaction → send notification on Submit/Approve

#### `update#LcApplicationDetail`
- **In**: `lcId`, `isSecured`, `securedPercentage`, `creditAgreementId`, `collateralDescription`
- **Logic**: Update LC record → automatically call `check#CustomerCreditLimit` if `creditAgreementId` supplied.

#### `check#CustomerCreditLimit`
- **In**: `lcId`, `creditAgreementId`
- **Logic**: Call `CbsIntegrationServices.check#CreditLimit` → update `availableCreditLimit` on LC → create history entry.

#### `submit#LetterOfCredit` (CR)
- **Logic**: Refined to transition to `LcTxPendingReview` (as per BRD lifecycle).

#### `approve#LcBySupervisor`
- **Logic**: Transitions to `LcTxPendingProcessing`.

#### `approve#LcByTradeOperator` (Phase 2 Enhancement)
- **Logic**: 
  1. Automated Calculation: Call `calculate#LcChargesAndProvisions`.
  2. CBS Provision Hold: Iterate through `LcProvision` (status=LcPrvDraft) and call `CbsIntegrationServices.hold#Funds`.
  3. Upfront Charge Collection: Iterate through `LcCharge` and call `CbsIntegrationServices.post#AccountingEntries`.
  4. Guard: Halt and return error if any CBS integration call fails.
  5. Transition transaction status → `LcTxPendingApproval`.

#### `calculate#LcChargesAndProvisions` [NEW]
- **In**: `lcId` (required)
- **Logic**: 
  - If no `LcProvision` exists, calculate based on `LcProduct.defaultProvisionRate` and create a `LcPrvDraft` record.
  - (Note: Initial `LcCharge` records are created during `create#LetterOfCredit` but can be refreshed here if needed).

#### `approve#LcByTradeSupervisor`
- **Logic**: Final business approval. Transitions transaction to `LcTxApproved` AND LC lifecycle to `LcLfApplied`.

#### `return#LetterOfCredit`
- **Logic**: Transitions back to `LcTxReturned` for correction. Allows applicant/CSR to edit and re-submit.

### 3.2 AmendmentServices.xml

#### `create#LcAmendment`
- **In**: `lcId` (required)
- **Out**: `amendmentSeqId`
- **Logic` (Shadow Record):
  1. Find the original `LetterOfCredit`
  2. Generate next `amendmentSeqId`
  3. **Clone all amendable fields** from LC into a new `LcAmendment` record
  4. Set `amendmentStatusId = LcTxDraft`, `amendmentDate = now`
  5. Create linked `mantle.request.Request` (type `RqtLcAmendment`)
  6. Create `LcHistory` entry

#### `confirm#LcAmendment`
- **In**: `lcId`, `amendmentSeqId`
- **Logic` (Apply Shadow → Master):
  1. Find the `LcAmendment` record
  2. **Write all shadow fields back** to primary `LetterOfCredit`
  3. Increment `LetterOfCredit.amendmentNumber`
  4. Transition LC lifecycle status → `LcLfAmended`
  5. Set `confirmationStatusId = LcAmndConfirmed`
  6. Generate SWIFT MT707

### 3.3 DrawingServices.xml

#### `register#LcDrawing`
- **In**: `lcId`, drawing fields
- **Logic`: Create `LcDrawing` with status `LcDrReceived` + linked Request

#### `examine#LcDrawing`
- **In**: `lcId`, `drawingSeqId`, `isCompliant` (boolean)
- **Logic`: Transition to `LcDrCompliant` or `LcDrDiscrepant` based on exam result

#### `record#LcDiscrepancy`
- **In**: `lcId`, `drawingSeqId`, discrepancy fields
- **Logic`: Create `LcDiscrepancy` record

#### `resolve#LcDiscrepancy`
- **In**: `lcId`, `drawingSeqId`, `discrepancySeqId`, `resolutionEnumId`
- **Logic`: Update resolution on LcDiscrepancy + transition drawing status accordingly

### 3.4 LifecycleServices.xml

#### `issue#LetterOfCredit` (Phase 3 Enhancement)
- **In**: `lcId`, `comments`
- **Logic**:
  1. Guard: `transactionStatusId == LcTxApproved` AND `lcStatusId in [LcLfDraft, LcLfApplied]`
  2. **Contingent Accounting**: Call `CbsIntegrationServices.post#AccountingEntries` for the full LC Amount.
     - Debit: `CONTINGENT_ASSET_ACC` (Contingent Asset Account)
     - Credit: `CONTINGENT_LIAB_ACC` (Contingent Liability Account)
  3. **Provision Activation**: Update all linked `LcProvision` records where `provisionStatusId == LcPrvHeld` to `LcPrvActive`.
  4. Generate SWIFT MT700 via `SwiftServices.generate#SwiftMt700`.
  5. Transition transaction status → `LcTxClosed`.
  6. Transition LC status → `LcLfIssued`.
  7. Post upfront charges to invoice/GL via `AccountingServices.post#LcChargesToInvoice`.
  8. Send system notification.

#### `revoke#LetterOfCredit`
- **In**: `lcId`, `comments`
- **Logic**:
  1. Guard: `formOfCredit_40A == LC_FORM_REVOCABLE`
  2. Transition LC status → `LcLfRevoked`
  3. Release all active provisions
  4. Generate SWIFT MT799 with revocation message
  5. Send notification

### 3.5 FinancialServices.xml

#### `calculate#LcCharge`
- **In**: `lcId`, charge fields
- **Logic`: Create `LcCharge` record

#### `calculate#LcProvision`
- **In**: `lcId`, `provisionRate`
- **Logic`: Calculate `provisionAmount = LC.amount × rate/100` → create `LcProvision` with status `LcPrvActive` → call CBS `hold#Funds`

#### `release#LcProvision`
- **In**: `lcId`, `provisionSeqId`
- **Logic`: Set `provisionStatusId = LcPrvReleased`, `releaseDate = now` → call CBS `release#Funds`

### 3.6 SwiftServices.xml

#### `generate#SwiftMt700`
- **In**: `lcId` | **Out**: `swiftMessageText`, `documentContentId`
- **Logic`: Build MT700 string from all 24 LC entity fields → save as `LcDocument` → store content at `dbresource://trade-finance/lc/swift/{lcId}_MT700.txt`

#### `generate#SwiftMt707`
- **In**: `lcId`, `amendmentSeqId`
- **Logic`: Build MT707 with amendment diff → save as LcDocument

#### `generate#SwiftMt734`
- **In**: `lcId`, `drawingSeqId`
- **Logic`: Build MT734 listing all discrepancies from `LcDiscrepancy` entity → save as LcDocument.

#### `generate#SwiftMt799`
- **In**: `lcId`, `subject`, `message`
- **Logic`: Build free-format MT799 → save as LcDocument

#### `parse#SwiftMt700`
- **In**: `swiftText` (String) | **Out**: `fieldMap` (Map)
- **Logic`: Regex parsing of SWIFT tags (:20:, :40A:, etc.) into a flat Map.

#### `parse#SwiftMt707`
- **In**: `swiftText` (String) | **Out**: `fieldMap` (Map)
- **Logic`: Regex parsing of amendment tags (:20:, :26E:, :79:, etc.)

### 3.7 Other Services

- **CbsIntegrationServices.xml**: `hold#Funds`, `release#Funds`, `post#AccountingEntries` — Interface stubs for CBS integration. Dynamic routing to `Simulator` via `cbs.integration.impl` system property.
- **CbsSimulatorServices.xml**: `hold#FundsSimulator`, `release#FundsSimulator`, `check#CreditLimitSimulator` — State-persisting services using `CbsSimulatorState`.
- **NotificationServices.xml**: `send#LcNotification` — Uses Moqui `NotificationMessage` API
- **ScheduledServices.xml**: `check#LcExpiry` — Daily cron job, finds LCs past `expiryDate` with no pending drawings → transition to `LcLfExpired`
- **DocumentServices.xml**: `attach#LcDocument` — Creates `LcDocument` record with file upload

### 3.8 AccountingServices.xml

#### `post#LcChargesToInvoice`
- **In**: `lcId`, `amendmentSeqId` (optional)
- **Logic**:
  1. Find all un-invoiced `LcCharge` records for the LC.
  2. Create Mantle `Invoice` (type `InvoiceSales`) from Issuing Bank to Applicant.
  3. Create `InvoiceItem` for each charge, mapping `chargeTypeEnumId` to `ItemType` via `LcProductCharge`.
  4. Link `LcCharge` to `InvoiceItem`.
  5. Call Mantle `post#Invoice` to generate GL entries (`AcctgTrans`).

---

## 4. Status Flows & Seed Data

### 4.1 Status Types & Flows

| StatusTypeId | StatusFlowId | Description |
| :--- | :--- | :--- |
| `LcLifecycle` | `LcLifecycle` | LC instrument lifecycle |
| `LcTransaction` | `LcTransaction` | Internal processing workflow |
| `LcDrawingStatus` | `LcDrawingStatus` | Drawing document examination |
| `LcProvisionStatus` | — | Provision hold state |
| `LcAmendmentConfirmation` | `LcAmendmentConfirmation` | External amendment confirmation |

### 4.2 LC Lifecycle Status Items

| statusId | description | seq |
| :--- | :--- | :---: |
| `LcLfDraft` | Draft | 1 |
| `LcLfApplied` | Applied | 2 |
| `LcLfIssued` | Issued | 3 |
| `LcLfAdvised` | Advised | 4 |
| `LcLfAmended` | Amended | 5 |
| `LcLfNegotiated` | Negotiated | 6 |
| `LcLfRevoked` | Revoked | 7 |
| `LcLfExpired` | Expired | 8 |
| `LcLfClosed` | Closed | 9 |

**Transitions**: Draft→Applied→Issued→{Advised,Amended,Negotiated,Revoked,Expired,Closed}, Advised→{Amended,Negotiated,Expired,Closed}, Amended→{Amended,Revoked,Negotiated,Expired,Closed}

### 4.3 Transaction Status Items

| statusId | description | seq |
| :--- | :--- | :---: |
| `LcTxDraft` | Draft | 1 |
| `LcTxSubmitted` | Submitted | 2 |
| `LcTxApproved` | Approved | 3 |
| `LcTxRejected` | Rejected | 4 |
| `LcTxCancelled` | Cancelled | 5 |
| `LcTxClosed` | Closed | 6 |

**Transitions**: Draft→{Submitted, Pending Review, Cancelled}, Submitted→{Approved, Rejected, Returned, Cancelled}, Pending Review→{Pending Processing, Returned}, Pending Processing→{Pending Approval, Returned}, Pending Approval→{Approved, Rejected, Returned}, Approved→Closed, Rejected→Draft, Returned→Draft

### 4.4 Drawing Status Items

| statusId | description | seq |
| :--- | :--- | :---: |
| `LcDrReceived` | Received | 1 |
| `LcDrCompliant` | Compliant | 2 |
| `LcDrDiscrepant` | Discrepant | 3 |
| `LcDrAccepted` | Accepted | 4 |
| `LcDrUnderTrust` | Under Trust | 5 |
| `LcDrPaid` | Paid | 6 |
| `LcDrRejected` | Rejected | 7 |

**Transitions**: Received→{Compliant,Discrepant}, Compliant→{Accepted,Paid}, Discrepant→{Accepted,Rejected,UnderTrust}, Accepted→Paid, UnderTrust→Paid

### 4.5 Provision Status
`LcPrvActive` (Active), `LcPrvReleased` (Released)

### 4.6 Amendment Confirmation Status
`LcAmndPending` → `LcAmndConfirmed` | `LcAmndRejected`

### 4.7 Enumerations

| EnumTypeId | Enum Values |
| :--- | :--- |
| `LcFormOfCredit` | Irrevocable, Revocable, Irrevocable Transferable |
| `LcApplicableRules` | UCP Latest, UCP600, eUCP Latest |
| `LcAvailability` | By Payment, By Acceptance, By Negotiation, By Deferred Payment |
| `LcPartialShipment` | ALLOWED, NOT ALLOWED, CONDITIONAL |
| `LcTranshipment` | ALLOWED, NOT ALLOWED, CONDITIONAL |
| `LcConfirmation` | CONFIRM, MAY ADD, WITHOUT |
| `LcProductType` | LC Pre Advice, LC Sight, LC Usance, LC Negotiation, LC Standby |
| `LcChargeType` | Issuance Commission, Advising Fee, Amendment Fee, Negotiation Commission, Acceptance Commission, Discrepancy Fee, Courier Charges, SWIFT Charges |
| `LcDocumentType` | Bill of Lading, Commercial Invoice, Packing List, Insurance Certificate, Certificate of Origin, Inspection Certificate, SWIFT Message, Application Form, Transport Document, Other |
| `LcDiscrepancyType` | Late Presentation, Inconsistent Documents, Missing Documents, Incorrect Amount, Stale Transport Documents, Goods Description Mismatch, Other |
| `LcDiscrepancyResolution` | Accepted, Rejected, Waived |
| `RequestType` (Mantle) | `RqtLcIssuance`, `RqtLcAmendment`, `RqtLcDrawing` |

### 4.8 Scheduled Jobs
```xml
<moqui.service.job.ServiceJob jobName="TradeFinance_checkLcExpiry"
    description="Auto-expires overdue Letters of Credit"
    serviceName="moqui.trade.finance.ScheduledServices.check#LcExpiry"
    cronExpression="0 0 1 * * ?"/>
```

---

## 5. Security Configuration

### 5.1 Artifact Groups

| artifactGroupId | Scope | Pattern |
| :--- | :--- | :--- |
| `TRADE_FINANCE_APP` | Screens + transitions + services + entities | `component://TradeFinance/.*`, `moqui\.trade\.finance\..*` |
| `TRADE_FINANCE_MANTLE` | Mantle Request entities + services | `mantle\.request\..*` |
| `TRADE_FINANCE_FRAMEWORK` | Framework entities for StatusFlow lookups | `moqui.basic.StatusItem`, `StatusFlowItem`, `StatusFlowTransition`, `Enumeration`, `EnumerationType`, `Uom` |

### 5.2 User Groups (Roles)

| userGroupId | Description | Access Level |
| :--- | :--- | :--- |
| `TF_ADMIN` | Trade Finance Administrator | ALL on all 3 groups |
| `TF_MAKER` | Create and Submit LCs | ALL on all 3 groups |
| `TF_CHECKER` | Approve and Reject LCs | ALL on all 3 groups |
| `TF_VIEWER` | Read Only Access | VIEW on all 3 groups |

### 5.3 Demo Users

| userId | username | password | Group |
| :--- | :--- | :--- | :--- |
| `TF_ADMIN_USER` | tf-admin | moqui | TF_ADMIN |
| `TF_MAKER_USER` | tf-maker | moqui | TF_MAKER |
| `TF_CHECKER_USER` | tf-checker | moqui | TF_CHECKER |
| `TF_VIEWER_USER` | tf-viewer | moqui | TF_VIEWER |

Password hash: `16ac58bbfa332c1c55bd98b53e60720bfa90d394` (SHA, value="moqui")

---

## 6. Screen Architecture Patterns

### 6.1 Module Root Pattern (ImportLc.xml)
```xml
<screen default-menu-title="Import LC Module">
    <subscreens default-item="Dashboard">
        <subscreens-item name="Dashboard" location="...Dashboard.xml"/>
        <subscreens-item name="Lc" menu-title="Letter of Credit" location="...Lc.xml"/>
        <subscreens-item name="Amendment" menu-title="Amendments" location="...Amendment.xml"/>
        <subscreens-item name="Drawing" menu-title="Drawings" location="...Drawing.xml"/>
    </subscreens>
    <widgets><subscreens-panel id="ImportLcPanel" type="popup"/></widgets>
</screen>
```

### 6.2 Wrapper Screen Pattern (Lc.xml)
Key features:
- Declares `<parameter name="lcId"/>` and `<parameter name="readOnly"/>`
- `<actions>` block: fetches LC data, resolves status descriptions, fetches valid StatusFlowTransitions
- Sets `isReadOnly` from `readOnly` parameter
- Uses `<subscreens default-item="FindLc">` with `menu-include="false"` for FindLc
- Main widget uses `<section condition="lcId && lc && targetScreen != 'FindLc'">` to show detail header only when viewing detail
- Detail header shows LC number, status chips, and action buttons (transition buttons)
- `<subscreens-panel type="tab"/>` for detail, `<subscreens-panel type="popup"/>` for list (fail-widgets)
- Safe subscreen detection: `sri.screenUrlInfo.targetScreen?.getScreenName() != 'FindLc'`

### 6.3 Find/List Screen Pattern (FindLc.xml)
```xml
<screen default-menu-title="Find LC">
    <transition name="lcDetail"><default-response url="../MainLC"/></transition>
    <actions>
        <entity-find entity-name="moqui.trade.finance.LetterOfCredit" list="lcList">
            <search-form-inputs/>
        </entity-find>
    </actions>
    <widgets>
        <form-list name="FindLcForm" list="lcList" transition="lcDetail">
            <field name="lcNumber"><default-field><link url="lcDetail" text="${lcNumber}" 
                parameter-map="[lcId:lcId]"/></default-field></field>
            <!-- More fields with display widgets -->
        </form-list>
    </widgets>
</screen>
```

### 6.4 Detail Form Pattern (MainLC.xml) — with Read-Only Support
Key pattern — use `conditional-field` to toggle between edit and display mode:
```xml
<field name="amount">
    <conditional-field condition="isReadOnly"><display currency-unit-field="amountCurrencyUomId"/></conditional-field>
    <default-field><text-line size="20"/></default-field>
</field>
```
Transition guarding:
```xml
<transition name="UpdateLc" condition="!isReadOnly">
    <service-call name="moqui.trade.finance.TradeFinanceServices.update#LetterOfCredit"/>
    <default-response url="."/>
</transition>
```

### 6.5 Cross-Module Navigation
- Use relative paths: `../../Lc/MainLC` from Amendment → LC
- Always use `url-type="screen"`
- Pass `readOnly=true` for cross-module view
- Pass `lastScreenUrl` for "Close View" back-navigation

### 6.6 Status Chip Pattern (in wrappers)
Use Quasar `q-chip` via CDATA for visual status:
```xml
<label text="${lcStatus?.description}" style="q-chip bg-green-8 text-white"/>
```
Color mapping: green-8 = active, blue-8 = info, orange-8 = warning, red-8 = error

### 6.7 Key Moqui XML Rules
1. **Never** place HTML/Quasar tags directly inside `<widgets>` — use `<text type="html"><![CDATA[...]]></text>`
2. All widget elements must be valid Moqui XSD elements
3. Use `<section name="..." condition="...">` for conditional rendering
4. Use `<section-iterate>` for looping over lists
5. Initialize all variables in `<actions>` before using in XML attributes
6. Use `<field-layout>` with `<field-group>` to organize form sections
7. Use `<field-ref>` to reference fields in `<field-layout>` sections

---

## 7. Demo Data Structure

10 LC records covering all lifecycle stages and product types:

| # | LC Number | Product | LC Status | Tx Status | Key Feature |
| :--- | :--- | :--- | :--- | :--- | :--- |
| 1 | ILC-2026-0001 | Sight | Closed | Closed | Fully settled, 2 drawings paid, 1 amendment |
| 2 | ILC-2026-0002 | Usance | Advised | Closed | Active, 1 drawing under review (90 days) |
| 3 | ILC-2026-0003 | Negotiation | Amended | Closed | 1 amendment (extend expiry), active |
| 4 | ILC-2026-0004 | Sight | Applied | Submitted | Pending approval |
| 5 | ILC-2026-0005 | Sight | Draft | Draft | Initial draft, not submitted |
| 6 | ILC-2026-0006 | Usance | Expired | Closed | No drawings, expired |
| 7 | ILC-2026-0007 | Standby | Issued | Closed | Active guarantee, no drawings |
| 8 | ILC-2026-0008 | Sight | Applied | Submitted | Transferable LC |
| 9 | ILC-2026-0009 | Sight | Draft | Draft | Rejected and reopened |
| 10 | ILC-2026-0010 | Negotiation | Negotiated | Approved | 2 drawings (1 with waived discrepancy) |

Related data: 10 applicants, 10 beneficiaries, ~15 banks, 4 users with role assignments.

---

**Document Generated:** 2026-03-08
