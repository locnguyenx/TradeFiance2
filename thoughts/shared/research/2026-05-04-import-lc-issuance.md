---
date: 2026-05-04T11:35:27+0700
researcher: locnguyenx
git_commit: 3c4c16be239f0b58c5b237e433e5bb7dbd99dc1a
branch: master
repository: TradeFiance2
topic: "REQ-IMP-PRC-01: LC Issuance implementation"
tags: [research, codebase, import-lc, issuance, moqui, swift, mt700]
status: complete
last_updated: 2026-05-04
last_updated_by: locnguyenx
---

# Research: REQ-IMP-PRC-01 — LC Issuance Implementation

**Date**: 2026-05-04T11:35:27+0700
**Researcher**: locnguyenx
**Git Commit**: 3c4c16be239f0b58c5b237e433e5bb7dbd99dc1a
**Branch**: master
**Repository**: TradeFiance2

## Research Question
How is the LC Issuance process (REQ-IMP-PRC-01) implemented in the codebase? This covers Application Capture, Margin/Cash Collateral, and SWIFT MT700 Generation.

## Summary
The LC Issuance process is implemented via `create#ImportLetterOfCredit` and `approve#ImportLetterOfCredit` services in `ImportLcServices.xml`. All mandatory BRD fields are captured in the entity model. However, MT700 generation is **not auto-triggered** on approval, facility checks are missing, and fee calculation is a stub.

## Detailed Findings

### 1. Application Capture — Entity Fields

All mandatory BRD fields are implemented across two entities:

**`TradeInstrument`** (`runtime/component/TradeFinance/entity/TradeCommonEntities.xml:6-68`):
| BRD Field | Entity Field | Line |
|-----------|-------------|------|
| LC Amount | `amount` | 13 |
| Currency | `currencyUomId` | 14 |
| Expiry Date | `expiryDate` | 18 |
| Base Equivalent Amount | `baseEquivalentAmount` | 22 |

**`ImportLetterOfCredit`** (`runtime/component/TradeFinance/entity/ImportLcEntities.xml:6-61`):
| BRD Field | Entity Field | Line |
|-----------|-------------|------|
| Tolerance (+/-) | `tolerancePositive`, `toleranceNegative` | 9-10 |
| Tenor | `tenorTypeId`, `usanceDays` | 11-12 |
| Port of Loading | `portOfLoading` | 13 |
| Port of Discharge | `portOfDischarge` | 14 |
| Expiry Place | `expiryPlace` | 15 |
| Description of Goods | `goodsDescription` | 16 |
| Documents Required | `documentsRequired` | 17 |
| Additional Conditions | `additionalConditions` | 18 |
| Latest Shipment Date | `latestShipmentDate` | 24 |
| Charge Allocation | `chargeAllocationEnumId` | 21 |
| Partial Shipment | `partialShipmentEnumId` | 22 |
| Transhipment | `transhipmentEnumId` | 23 |
| LC Type | `lcTypeEnumId` | 26 |
| Available By/With | `availableByEnumId`, `availableWithEnumId` | 27-28 |

**Party assignment** uses `TradeInstrumentParty` junction entity (`TradeCommonEntities.xml:111-121`) with roles `TP_APPLICANT`, `TP_BENEFICIARY`, `TP_ADVISING_BANK`, `TP_NEGOTIATING_BANK`.

### 2. Issuance Service Flow

**`create#ImportLetterOfCredit`** (`ImportLcServices.xml:6-103`):

1. **Generate transaction ref** (lines 32-35): Calls `trade.TradeCommonServices.get#NextNumber` with sequence `IMP_LC_REF`
2. **Create TradeInstrument** (lines 37-48): `businessStateId: 'LC_DRAFT'`, `instrumentTypeEnumId: 'IMPORT_LC'`
3. **Create TradeTransaction** (lines 50-62): `transactionTypeEnumId: 'IMP_NEW'`, `transactionStatusId: 'TX_DRAFT'`
4. **Assign Parties** (lines 64-72): Iterates `instrumentParties` list, calls `assign#InstrumentParty`
5. **Mandatory Role Validation** (lines 74-84): Requires `TP_APPLICANT` and `TP_BENEFICIARY`; `TP_NEGOTIATING_BANK` required if `availableWithEnumId == 'AVAIL_SPECIFIC_BANK'`
6. **Create ImportLetterOfCredit** (lines 86-101): Sets `effectiveAmount`, `effectiveOutstandingAmount`, `effectiveExpiryDate`, `effectiveShipmentDate`

**`approve#ImportLetterOfCredit`** (`ImportLcServices.xml:221-302`):
1. **Evaluate Maker/Checker Authority** (lines 243-247): Calls `trade.AuthorizationServices.evaluate#MakerCheckerMatrix`
2. **Dual Checker for Tier4** (lines 250-267): Requires two distinct approvers
3. **Create Approval Record** (lines 270-279): Persists to `TradeApprovalRecord`
4. **Transition to Issued** (lines 282-296): Sets `transactionStatusId: 'TX_APPROVED'`, `businessStateId: 'LC_ISSUED'`

**State Transitions:**
```
LC_DRAFT (create) → LC_PENDING (submit) → LC_ISSUED (approve)
```

### 3. Margin/Cash Collateral — PARTIALLY IMPLEMENTED

**What exists:**

`LimitServices.xml` has the building blocks but they are **not wired into issuance**:

| Service | File:Line | Purpose |
|---------|-----------|---------|
| `calculate#Earmark` | `LimitServices.xml:6-34` | Checks `totalApprovedLimit - utilizedAmount` |
| `update#Utilization` | `LimitServices.xml:36-52` | Adjusts facility `utilizedAmount` by delta |
| `reverse#Limit` | `LimitServices.xml:201-218` | Releases utilization on cancellation |

**What is missing:**
- `create#ImportLetterOfCredit` (line 6) does **not** accept `customerFacilityId`
- `approve#ImportLetterOfCredit` (line 221) does **not** call `calculate#Earmark` before issuing
- `TradeInstrument.customerFacilityId` field exists (`TradeCommonEntities.xml:19`) but is never populated during creation

**See also:**
- [2026-05-04-import-lc-amendments.md](./2026-05-04-import-lc-amendments.md) — Amendment process also affects facility utilization via `update#Utilization`

### 4. SWIFT MT700 Generation — NOT AUTO-TRIGGERED

**Service exists:** `generate#Mt700` (`SwiftGenerationServices.xml:6-158`)

**MT700 Tags implemented** (from `SwiftGenerationServices.xml:79-133`):

| Tag | Field | Line |
|-----|-------|------|
| 27 | Sequence of total | 79 |
| 40A | Form of Documentary Credit | 80 |
| 31C | Date of Issue | 81 |
| 40E | Applicable Rules | 82 |
| 31D | Date and Place of Expiry | 83 |
| 50 | Applicant | 86-87 |
| 59 | Beneficiary | 90-91 |
| 32B | Currency + Amount | 93-94 |
| 39A | Tolerance (+/-) | 96-98 |
| 39B | Max Credit Amount flag | 98-100 |
| 41A/41D | Available With...By... | 103-110 |
| 42C | Drafts at (usanceBaseDate) | 112-113 |
| 42a | Drawee (draweeBankBIC) | 115 |
| 43P | Partial Shipments | 116 |
| 43T | Transhipment | 117 |
| 44A | Place of Receipt | 119 |
| 44E | Port of Loading | 120 |
| 44F | Port of Discharge | 121 |
| 44B | Place of Final Delivery | 122 |
| 44C | Latest Shipment Date | 124 |
| 44D | Shipment Period | 125 |
| 45A | Description of Goods | 127 |
| 46A | Documents Required | 128 |
| 47A | Additional Conditions | 129 |
| 71D | Charges | 130 |
| 48 | Period for Presentation | 131 |
| 49 | Confirmation Instructions | 132 |
| 78 | Instructions to Bank | 133 |

**Builder:** `SwiftMessageBuilder.groovy` (`src/main/groovy/trade/SwiftMessageBuilder.groovy`):
- `formatValue(value, maxLines, maxChars)` — line 42-72: Enforces X charset, wraps lines
- `build()` — line 74-104: Assembles `{1:...}{2:...}{4:...}` structure

**Critical gap:** `approve#ImportLetterOfCredit` (line 221) does **not** call `generate#Mt700`. The service exists and works but is not wired into the approval flow.

**See also:**
- [2026-05-04-import-lc-amendments.md](./2026-05-04-import-lc-amendments.md) — MT707 generation has the same gap (service exists, not triggered)
- `SwiftMessageBuilder.groovy` is also used by MT707, MT750, MT734, MT732 generators

### 5. Fee Calculation — STUB ONLY

**`calculate#Fees`** (`TradeAccountingServices.xml:104-115`):
```groovy
totalFee = baseAmount < minCharge ? minCharge : baseAmount
```
- `minCharge` defaults to `50.0`
- Does **not** read from `FeeConfiguration` entity (`TradeCommonEntities.xml:123-138`)
- Does **not** apply `calculationTypeEnumId` (FLAT, PERCENTAGE, TIERED)

**`post#TradeEntry`** (`TradeAccountingServices.xml:6-54`): Posts GL entries (DR `111000`, CR `411000`) but is not called during issuance.

**Gap:** No fee deduction on issuance. The `approve#ImportLetterOfCredit` service does not call `calculate#Fees` or `post#TradeEntry`.

## Code References
- `runtime/component/TradeFinance/entity/ImportLcEntities.xml:6` - ImportLetterOfCredit entity
- `runtime/component/TradeFinance/entity/TradeCommonEntities.xml:6` - TradeInstrument entity
- `runtime/component/TradeFinance/entity/TradeCommonEntities.xml:111` - TradeInstrumentParty entity
- `runtime/component/TradeFinance/service/trade/importlc/ImportLcServices.xml:6` - create#ImportLetterOfCredit
- `runtime/component/TradeFinance/service/trade/importlc/ImportLcServices.xml:221` - approve#ImportLetterOfCredit
- `runtime/component/TradeFinance/service/trade/SwiftGenerationServices.xml:6` - generate#Mt700
- `runtime/component/TradeFinance/src/main/groovy/trade/SwiftMessageBuilder.groovy` - SWIFT message builder
- `runtime/component/TradeFinance/service/trade/LimitServices.xml:6` - calculate#Earmark
- `runtime/component/TradeFinance/service/trade/TradeAccountingServices.xml:104` - calculate#Fees (stub)
- `frontend/src/components/IssuanceStepper.tsx:26` - 4-step issuance UI wizard

## Architecture Documentation

### Issuance Flow (Current Implementation)
```
User fills IssuanceStepper (4 steps)
    ↓ POST /import-lc
create#ImportLetterOfCredit (ImportLcServices.xml:6)
    ├→ Create TradeInstrument (state: LC_DRAFT)
    ├→ Create TradeTransaction (type: IMP_NEW, status: TX_DRAFT)
    ├→ Assign Parties (applicant, beneficiary, etc.)
    └→ Create ImportLetterOfCredit record
    ↓ (Maker submits with businessStateId: LC_PENDING)
update#ImportLetterOfCredit (ImportLcServices.xml:105)
    └→ TradeTransaction.status → TX_PENDING
    ↓ (Checker approves)
approve#ImportLetterOfCredit (ImportLcServices.xml:221)
    ├→ Evaluate Maker/Checker authority matrix
    ├→ Create TradeApprovalRecord
    ├→ Transition: TX_APPROVED + LC_ISSUED
    └→ ⚠️ MT700 NOT auto-generated (service exists but not called)
```

### Key Patterns
- **Two-layer entity model**: `TradeInstrument` (common) + `ImportLetterOfCredit` (LC-specific SWIFT fields)
- **Effective value pattern**: `effectiveAmount`, `effectiveExpiryDate` — snapshot fields for amendment tracking
- **State machine**: LC_DRAFT → LC_PENDING → LC_ISSUED
- **Immutability guard**: Issued LCs cannot modify financial terms via update; must use Amendment workflow (`ImportLcServices.xml:140`)

## Related Research
- [2026-05-04-import-lc-amendments.md](./2026-05-04-import-lc-amendments.md) — Amendment process (financial changes post-issuance)
- [2026-05-04-import-lc-presentation.md](./2026-05-04-import-lc-presentation.md) — Document presentation (post-issuance)
- [2026-05-04-import-lc-settlement.md](./2026-05-04-import-lc-settlement.md) — Settlement & payment (final step)

## Open Questions
1. Should MT700 be auto-generated in `approve#ImportLetterOfCredit` (line 296) or handled via SECA rule in `TradeFinanceSeca.xml`?
2. Should facility check (`calculate#Earmark`) block approval if insufficient? Currently no check exists.
3. Should `customerFacilityId` be required during LC creation, or looked up from the Applicant's profile?
4. Fee calculation needs to read from `FeeConfiguration` entity — what is the correct fee event enum for issuance (e.g., `FEE_ISSUE_IMP_LC`)?
