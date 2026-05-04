---
date: 2026-05-04T11:35:27+0700
researcher: locnguyenx
git_commit: 3c4c16be239f0b58c5b237e433e5bb7dbd99dc1a
branch: master
repository: TradeFiance2
topic: "REQ-IMP-PRC-02: Amendments implementation"
tags: [research, codebase, import-lc, amendments, moqui, swift, mt707]
status: complete
last_updated: 2026-05-04
last_updated_by: locnguyenx
---

# Research: REQ-IMP-PRC-02 — Amendments Implementation

**Date**: 2026-05-04T11:35:27+0700
**Researcher**: locnguyenx
**Git Commit**: 3c4c16be239f0b58c5b237e433e5bb7dbd99dc1a
**Branch**: master
**Repository**: TradeFiance2

## Research Question
How is the Amendments process (REQ-IMP-PRC-02) implemented? This covers Initiation, Financial vs Non-Financial distinction, Limit Recalculation, and Beneficiary Consent tracking.

## Summary
The Amendment process is implemented via `create#Amendment`, `authorize#Amendment`, and `update#Amendment` services. The `ImportLcAmendment` entity tracks amendments with `isFinancial` flag, `beneficiaryConsentStatusId`, and `amendmentNumber`. MT707 generation exists but is **not auto-triggered**. Beneficiary consent is tracked via `update#Amendment` but is not fully integrated into the UI flow.

## Detailed Findings

### 1. ImportLcAmendment Entity

**File:** `runtime/component/TradeFinance/entity/ImportLcEntities.xml:124-143`

| Field | Type | Description | Line |
|-------|------|-------------|------|
| `amendmentId` | id (PK) | Unique identifier | 125 |
| `instrumentId` | id | Links to ImportLetterOfCredit | 126 |
| `amendmentDate` | date | Defaults to `ec.user.nowTimestamp` | 127 |
| `amountAdjustment` | number-decimal | Delta amount (+ increase, - decrease) | 128 |
| `newExpiryDate` | date | New expiry if extended | 129 |
| `amendmentNarrative` | text-very-long | Free-text changes | 130 |
| `isFinancial` | text-indicator | 'Y' for financial amendments | 131 |
| `beneficiaryConsentStatusId` | id | PENDING/ACCEPTED/REJECTED | 132 |
| `amendmentBusinessStateId` | id | AMEND_DRAFT, AMEND_APPROVED, etc. | 133 |
| `amendmentTypeEnumId` | id | Type classification | 134 |
| `isBeneficiaryAcceptanceRequired` | text-indicator | Default 'Y' | 135 |
| `amendmentNumber` | number-integer | Auto-incremented | 136 |
| `newTolerance` | number-decimal | Overwrite tolerance | 137 |
| `chargeAllocationEnumId` | id | Who pays amendment fee | 138 |

**Relationship:** Links to `ImportLetterOfCredit` via `instrumentId` (line 140-142).

### 2. Amendment Services

#### `create#Amendment` (`ImportLcServices.xml:304-374`)

**Inputs:**
- `instrumentId` (required) — line 306
- `amendmentTypeEnumId` (required) — line 307
- `amendmentDate` (required) — line 308
- `amountAdjustment` — line 309
- `newExpiryDate` — line 310
- `amendmentNarrative` — line 311
- `isFinancial` — line 312
- `priorityEnumId` — line 313

**Implementation:**
1. **Validate LC state** (line 324): Cannot create amendment while `LC_AMENDMENT_PENDING`
2. **Calculate amendment number** (lines 329-334): Auto-increments from existing amendments
3. **Set initial state** (line 337): `amendmentBusinessStateId: 'AMEND_DRAFT'`
4. **Set beneficiary consent** (line 338): If `isFinancial == 'Y'`, set `beneficiaryConsentStatusId: 'PENDING'`
5. **Check concurrent amendments** (lines 340-344): Blocks if any amendment is in DRAFT/PENDING/APPROVED state
6. **Create TradeTransaction** (lines 349-362): `transactionTypeEnumId: 'IMP_AMENDMENT'`, links via `relatedRecordId`
7. **Transition LC state** (lines 367-372): If financial, transitions to `LC_AMENDMENT_PENDING`

#### `authorize#Amendment` (`ImportLcServices.xml:376-430`)

**Inputs:** `amendmentId` (required) — line 378

**Implementation:**
1. **Validate state** (line 384): Only amendments in `AMEND_DRAFT` can be authorized
2. **Update amendment state** (line 388-389): Sets `amendmentBusinessStateId: 'AMEND_APPROVED'`
3. **Apply effective values to LC** (lines 394-398):
   ```groovy
   lc.effectiveAmount += amendment.amountAdjustment
   lc.effectiveOutstandingAmount += amendment.amountAdjustment
   lc.totalAmendmentCount += 1
   ```
4. **Approve TradeTransaction** (lines 403-416): Finds `IMP_AMENDMENT` transaction, sets `TX_APPROVED`
5. **Transition LC state for financial** (lines 419-424): Sets `LC_AMENDMENT_PENDING` (waits beneficiary consent)
6. **Non-financial** (lines 426-428): LC remains `LC_ISSUED`

#### `update#Amendment` (`ImportLcServices.xml:432-474`)

**Purpose:** Handle beneficiary consent and apply changes to LC

**Implementation:**
1. **Detect consent change** (line 441): If `beneficiaryConsentStatusId` changes to `ACCEPTED` or `REJECTED`
2. **Transition LC back to Issued** (lines 443-445): `businessStateId: 'LC_ISSUED'`
3. **Apply effective values on acceptance** (lines 447-465):
   - `effectiveAmount += amountAdjustment`
   - `effectiveExpiryDate = newExpiryDate` (if provided)
   - `effectiveTolerancePositive = newTolerance` (if provided)
4. **Set amendment state** (line 467): `amendmentBusinessStateId: 'AMEND_COMMITTED'`

### 3. Financial vs Non-Financial Distinction

**Detection:** `isFinancial` flag set during `create#Amendment` (line 312, 338)

**Financial amendment triggers:**
- Amount change (`amountAdjustment != null`) — `authorize#Amendment:419-424`
- Expiry extension (`newExpiryDate` provided) — requires beneficiary consent

**Non-financial amendment:**
- Text changes (`amendmentNarrative`)
- Port changes
- LC remains `LC_ISSUED` during authorization (line 427)

**Limit recalculation:** The system updates `effectiveAmount` and `effectiveOutstandingAmount` on the LC (lines 395-396), but does **not** call `LimitServices.update#Utilization` to adjust facility utilization for financial amendments.

### 4. Beneficiary Consent Tracking

**Field:** `ImportLcAmendment.beneficiaryConsentStatusId` (line 132)
- Values: `PENDING` (default for financial), `ACCEPTED`, `REJECTED`

**Flow:**
1. Financial amendment created → `beneficiaryConsentStatusId: 'PENDING'`
2. LC transitions to `LC_AMENDMENT_PENDING` (line 369-371)
3. Authorization → `AMEND_APPROVED` (line 389)
4. Bank logs beneficiary response via `update#Amendment` (line 432)
5. On `ACCEPTED` → applies changes, transitions LC back to `LC_ISSUED` (lines 443-465)
6. On `REJECTED` → transitions LC back to `LC_ISSUED` without applying changes

**Gap:** MT707 is generated via `generate#Mt707` but is **not auto-triggered** when amendment is approved. The SWIFT message must be sent to the Advising Bank to notify the Beneficiary of the amendment.

### 5. MT707 Generation — NOT AUTO-TRIGGERED

**Service exists:** `generate#Mt707` (`SwiftGenerationServices.xml:195-264`)

**Implementation:**
- Reads amendment and instrument data (lines 207-210)
- Checks for existing active MT707 (lines 213-223)
- Sets `messageStatusId: 'SWIFT_MSG_ACTIVE'` if amendment is `AMEND_APPROVED` (line 225)
- Builds message using `SwiftMessageBuilder` (lines 230-242):
  - Tag 26E: Amendment number
  - Tag 31C: Amendment date
  - Tag 32B: Amount adjustment (if provided)
  - Tag 79Z: Amendment narrative

**Critical gap:** Neither `authorize#Amendment` (line 376) nor `update#Amendment` (line 432) calls `generate#Mt707`. The service exists and works but is not wired into the amendment flow.

**See also:**
- [2026-05-04-import-lc-issuance.md](./2026-05-04-import-lc-issuance.md) — MT700 has the same auto-trigger gap
- `SwiftMessageBuilder.groovy` is shared across MT700, MT707, MT750, MT734, MT732 generators

### 6. State Transitions

**Amendment Business States** (defined in `ImportLcAmendment.amendmentBusinessStateId`):
```
AMEND_DRAFT → AMEND_APPROVED → AMEND_COMMITTED
```

**LC States during Amendment:**
```
LC_ISSUED
    ↓ create#Amendment (isFinancial=Y)
LC_AMENDMENT_PENDING (awaiting beneficiary consent)
    ↓ update#Amendment (beneficiaryConsentStatusId=ACCEPTED)
LC_ISSUED (changes applied)
```

**Note:** The parent LC remains in `LC_ISSUED` throughout the amendment process per BRD REQ-IMP-SPEC-02 (section A), but the implementation transitions to `LC_AMENDMENT_PENDING` when waiting for beneficiary consent. This is a **deviation from the BRD**.

## Code References
- `runtime/component/TradeFinance/entity/ImportLcEntities.xml:124` - ImportLcAmendment entity
- `runtime/component/TradeFinance/service/trade/importlc/ImportLcServices.xml:304` - create#Amendment
- `runtime/component/TradeFinance/service/trade/importlc/ImportLcServices.xml:376` - authorize#Amendment
- `runtime/component/TradeFinance/service/trade/importlc/ImportLcServices.xml:432` - update#Amendment
- `runtime/component/TradeFinance/service/trade/SwiftGenerationServices.xml:195` - generate#Mt707
- `runtime/component/TradeFinance/service/trade/LimitServices.xml:36` - update#Utilization (not called for amendments)

## Architecture Documentation

### Amendment Flow (Current Implementation)
```
LC_ISSUED
    ↓ create#Amendment (ImportLcServices.xml:304)
    ├→ Create ImportLcAmendment (AMEND_DRAFT)
    ├→ Create TradeTransaction (IMP_AMENDMENT)
    └→ If financial: LC → LC_AMENDMENT_PENDING
    ↓ authorize#Amendment (ImportLcServices.xml:376)
    ├→ Update amendment to AMEND_APPROVED
    ├→ Apply effectiveAmount adjustment to LC
    └→ ⚠️ MT707 NOT auto-generated
    ↓ update#Amendment (beneficiary consent)
    ├→ If ACCEPTED: Apply effectiveExpiryDate, effectiveTolerance
    └→ LC → LC_ISSUED (amendment committed)
```

### Key Patterns
- **Delta-based amendments**: `amountAdjustment` (not absolute amounts) — supports both increases and decreases
- **Effective value pattern**: Changes stored in amendment, applied to `effective*` fields on LC
- **Beneficiary consent**: Required for financial amendments, tracked via `beneficiaryConsentStatusId`
- **Amendment numbering**: Auto-incremented per LC via `amendmentNumber`

## Related Research
- [2026-05-04-import-lc-issuance.md](./2026-05-04-import-lc-issuance.md) — Issuance process (amendments modify issued LCs)
- [2026-05-04-import-lc-presentation.md](./2026-05-04-import-lc-presentation.md) — Document presentation (can be amended)
- [2026-05-04-import-lc-settlement.md](./2026-05-04-import-lc-settlement.md) — Settlement (settles based on effective amounts)

## Open Questions
1. Should MT707 be auto-generated in `authorize#Amendment` (line 389) or when beneficiary accepts (line 443)?
2. Should `LimitServices.update#Utilization` be called for financial amendments to adjust facility earmarks?
3. The BRD says parent LC should remain `Issued` during amendment — should `LC_AMENDMENT_PENDING` state be removed?
4. Is there a SWIFT message for requesting beneficiary consent (e.g., MT799)? Not currently implemented.
