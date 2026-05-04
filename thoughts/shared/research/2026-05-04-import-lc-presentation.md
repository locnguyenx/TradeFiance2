---
date: 2026-05-04T11:35:27+0700
researcher: locnguyenx
git_commit: 3c4c16be239f0b58c5b237e433e5bb7dbd99dc1a
branch: master
repository: TradeFiance2
topic: "REQ-IMP-PRC-03: Document Presentation & Examination implementation"
tags: [research, codebase, import-lc, presentation, examination, discrepancies, moqui, swift, mt734, mt750]
status: complete
last_updated: 2026-05-04
last_updated_by: locnguyenx
---

# Research: REQ-IMP-PRC-03 — Document Presentation & Examination Implementation

**Date**: 2026-05-04T11:35:27+0700
**Researcher**: locnguyenx
**Git Commit**: 3c4c16be239f0b58c5b237e433e5bb7dbd99dc1a
**Branch**: master
**Repository**: TradeFiance2

## Research Question
How is the Document Presentation & Examination process (REQ-IMP-PRC-03) implemented? This covers Lodgement, Examination Window (5-day SLA), Discrepancy Logging, and Communication (MT734/MT750).

## Summary
Document presentation is implemented via `create#Presentation`, `examine#Documents`, and `authorize#Presentation` services. The `TradeDocumentPresentation` and `PresentationDiscrepancy` entities capture all required data. MT750 is auto-generated on discrepancies, but MT734 is **not called** in the flow. The 5-day SLA timer is **not automatically calculated** — the `regulatoryDeadline` field exists but is never set.

## Detailed Findings

### 1. TradeDocumentPresentation Entity

**File:** `runtime/component/TradeFinance/entity/ImportLcEntities.xml:93-111`

| Field | Type | Description | Line |
|-------|------|-------------|------|
| `presentationId` | id (PK) | Unique identifier | 94 |
| `instrumentId` | id | Links to ImportLetterOfCredit | 95 |
| `presentationDate` | date | Defaults to `ec.user.nowTimestamp` | 96 |
| `claimAmount` | number-decimal | Presentation claim amount | 97 |
| `isDiscrepant` | text-indicator | Flag for discrepancies | 98 |
| `applicantDecisionEnumId` | id | PENDING/WAIVED/REFUSED | 99 |
| `presentationStatusId` | id | Status tracking | 100 |
| `presentingBankRef` | text-medium | Presenting bank reference | 101 |
| `claimCurrency` | id | Currency of claim | 102 |
| `regulatoryDeadline` | date | **NOT AUTO-CALCULATED** | 103 |
| `documentDisposalEnumId` | id | HOLDING/RETURNING_DOCUMENTS (Tag 77B) | 104 |
| `chargesDeducted` | text-long | Tag 73 | 105 |
| `senderToReceiverPresentationInfo` | text-long | Tag 72Z | 106 |

**Related entity:** `TradeDocumentPresentationItem` (lines 113-122) — Document type grid with `originalsCount` and `copiesCount`. **Note:** Entity exists but is **NOT used** in current services.

### 2. Presentation Services

#### `create#Presentation` (`TradeCommonServices.xml:32-71`)

**Implementation:**
1. **Validate presentation** (line 43): Calls `validate#Presentation` from `ImportLcValidationServices.xml`
2. **Create TradeDocumentPresentation** (lines 48-49): Status `PRES_RECEIVED`
3. **Create TradeTransaction** (lines 53-65): `transactionTypeEnumId: 'IMP_PRESENTATION'`
4. **Update LC state** (lines 67-69): `businessStateId: 'LC_DOC_RECEIVED'`

**Gap:** Does **not** calculate `regulatoryDeadline` (PresentationDate + 5 banking days).

#### `examine#Documents` (`ImportLcServices.xml:534-557`)

**Inputs:** `presentationId`, `discrepancyList` — line 536-537

**Implementation:**
1. **Iterate discrepancies** (lines 549-552): Creates `PresentationDiscrepancy` records for each
2. **Update presentation status** (line 554): `presentationStatusId: 'PRES_EXAMINED'`

**Note:** Does **not** auto-trigger MT750 — that happens in `authorize#Presentation`.

#### `authorize#Presentation` (`ImportLcServices.xml:559-584`)

**Implementation:**
1. **Check for discrepancies** (lines 568-570)
2. **Set target LC state** (line 572):
   - With discrepancies: `LC_DISCREPANT`
   - Clean: `LC_ACCEPTED`
3. **Generate MT750** (lines 577-579): If discrepancies found, calls `generate#Mt750`
4. **Update presentation** (line 582): `presentationStatusId: 'PRES_AUTHORIZED'`

**Critical gap:** MT734 (Notice of Refusal) is **not called** here. Only MT750 (Advice of Discrepancy) is generated.

### 3. Examination Window / 5-Day SLA

**Configuration exists:**
- `TradeProductCatalog.documentExamSlaDays` field (`TradeCommonEntities.xml:157`) — default 5 days
- Master data: `DOC_EXAM_SLA_DAYS=5` in `TradeFinanceMasterData.xml:32`
- Product-specific: `PROD_IMP_LC` has 5 days (`TradeFinanceMasterData.xml:42-44`)

**SLA Calculation Service:** `calculate#BusinessDate` (`TradeCommonServices.xml:99-116`)
- **STUB implementation** (line 108 comment)
- Adds days + 3 for "holiday simulation" (line 112)
- **Not a real SLA calculator** — no holiday calendar integration

**SLA Exhaustion Check:** `evaluate#SlaExhaustion` (`TradeCommonServices.xml:221-225`)
- Simple check: `daysDiff > 5` (line 224)
- **Not connected** to `regulatoryDeadline` field or any automated process

**Key finding:** `regulatoryDeadline` field (line 103) exists on entity but is **NEVER automatically calculated** by `create#Presentation`. The SLA configuration exists in master data but is not implemented in the presentation flow.

### 4. Discrepancy Logging

**PresentationDiscrepancy Entity** (`ImportLcEntities.xml:145-157`):

| Field | Type | Description | Line |
|-------|------|-------------|------|
| `discrepancyId` | id (PK) | Unique identifier | 146 |
| `presentationId` | id | Links to presentation | 147 |
| `discrepancyCode` | text-short | ISBP standard code | 148 |
| `discrepancyDescription` | text-long | Description | 149 |
| `isWaived` | text-indicator | Default 'N' | 150 |
| `waivedByUserId` | id | Who waived it | 151 |
| `waivedTimestamp` | date-time | When waived | 152 |

**Creation flow:** `examine#Documents` (line 549-551) creates records via `create#trade.importlc.PresentationDiscrepancy`.

**MT750 reads discrepancies** (`SwiftGenerationServices.xml:291`): Queries and formats them into field 77J.

### 5. MT734 Generation — NOT CALLED

**Service exists:** `generate#Mt734` (`SwiftGenerationServices.xml:306-347`)

**Implementation:**
- Sets message type "734" (line 334)
- Adds field 32A with date, currency, amount (line 337)
- Adds field 77J with refusal reason (line 338)
- Adds field 77B: "DOCUMENTS HELD AT YOUR DISPOSAL" (line 339)
- Creates `SwiftMessage` record (line 344)

**Critical gap:** `authorize#Presentation` (line 559) only calls `generate#Mt750` (line 577-579). MT734 is **never called** in the presentation flow.

### 6. Applicant Waiver Handling

**Service:** `update#PresentationWaiver` (`ImportLcServices.xml:587-605`)

**Inputs:** `presentationId`, `applicantDecisionEnumId` (WAIVED, REJECTED)

**Implementation:**
1. **If WAIVED** (lines 597-599): Updates LC state to `LC_ACCEPTED`
2. **Generates MT752** (line 601): Calls `generate#Mt752` (Authorization to Pay/Accept/Negotiate)

**Gaps:**
- Does **not** update `PresentationDiscrepancy.isWaived` flag
- Does **not** set `waivedByUserId` or `waivedTimestamp`
- Does **not** update `TradeDocumentPresentation.applicantDecisionEnumId` (line 99) — field exists but is never set by any service

### 7. State Transitions

**Defined in:** `TradeFinanceSeedData.xml:220-227`

**LC States:**
```
LC_ISSUED → LC_DOC_RECEIVED (create#Presentation)
LC_DOC_RECEIVED → LC_DISCREPANT (discrepancies found)
LC_DOC_RECEIVED → LC_ACCEPTED (clean presentation)
LC_DISCREPANT → LC_ACCEPTED (applicant waives)
LC_ACCEPTED → LC_SETTLED (settle#Presentation)
```

**Presentation States** (defined in seed data lines 185-190):
- `PRES_DRAFT` — Draft
- `PRES_RECEIVED` — Set by `create#Presentation` (line 48)
- `PRES_EXAMINED` — Set by `examine#Documents` (line 554)
- `PRES_AUTHORIZED` — Set by `authorize#Presentation` (line 582)
- `PRES_SETTLED` — Set by `settle#Presentation` (ImportLcServices.xml:525)

**Note:** `PRES_EXAMINING`, `PRES_DISCREPANT`, `PRES_COMPLIANT` are defined in seed data but **NOT USED** in services.

## Code References
- `runtime/component/TradeFinance/entity/ImportLcEntities.xml:93` - TradeDocumentPresentation entity
- `runtime/component/TradeFinance/entity/ImportLcEntities.xml:145` - PresentationDiscrepancy entity
- `runtime/component/TradeFinance/service/trade/TradeCommonServices.xml:32` - create#Presentation
- `runtime/component/TradeFinance/service/trade/importlc/ImportLcServices.xml:534` - examine#Documents
- `runtime/component/TradeFinance/service/trade/importlc/ImportLcServices.xml:559` - authorize#Presentation
- `runtime/component/TradeFinance/service/trade/importlc/ImportLcServices.xml:587` - update#PresentationWaiver
- `runtime/component/TradeFinance/service/trade/importlc/ImportLcValidationServices.xml` - validate#Presentation
- `runtime/component/TradeFinance/service/trade/SwiftGenerationServices.xml:280` - generate#Mt750
- `runtime/component/TradeFinance/service/trade/SwiftGenerationServices.xml:306` - generate#Mt734 (not called)
- `runtime/component/TradeFinance/service/trade/SwiftGenerationServices.xml:349` - generate#Mt752
- `runtime/component/TradeFinance/service/trade/TradeCommonServices.xml:99` - calculate#BusinessDate (stub)
- `runtime/component/TradeFinance/data/TradeFinanceSeedData.xml:220` - State transitions

## Architecture Documentation

### Presentation Flow (Current Implementation)
```
LC_ISSUED
    ↓ create#Presentation (TradeCommonServices.xml:32)
    ├→ Create TradeDocumentPresentation (PRES_RECEIVED)
    ├→ Create TradeTransaction (IMP_PRESENTATION)
    └→ LC → LC_DOC_RECEIVED
    ↓ examine#Documents (ImportLcServices.xml:534)
    ├→ Create PresentationDiscrepancy records
    └→ Presentation → PRES_EXAMINED
    ↓ authorize#Presentation (ImportLcServices.xml:559)
    ├→ If discrepancies: LC → LC_DISCREPANT, generate MT750
    └→ If clean: LC → LC_ACCEPTED
    ↓ update#PresentationWaiver (if discrepant, ImportLcServices.xml:587)
    ├→ If WAIVED: LC → LC_ACCEPTED, generate MT752
    └→ If REFUSED: LC → LC_CLOSED (documents returned)
```

### Key Patterns
- **Presentation-centric model**: Documents are logged against the LC via `TradeDocumentPresentation`
- **Discrepancy tracking**: ISBP codes stored in `PresentationDiscrepancy` with waiver workflow
- **SLA tracking**: Config exists (`documentExamSlaDays`) but auto-calculation is not implemented
- **SWIFT messages**: MT750 auto-generated on discrepancies, MT752 on waiver — but MT734 is missing

## Related Research
- [2026-05-04-import-lc-issuance.md](./2026-05-04-import-lc-issuance.md) — Issuance creates the LC that presentations are made against
- [2026-05-04-import-lc-amendments.md](./2026-05-04-import-lc-amendments.md) — Amendments can affect presentation terms
- [2026-05-04-import-lc-settlement.md](./2026-05-04-import-lc-settlement.md) — Settlement processes accepted presentations

## Open Questions
1. Should `regulatoryDeadline` be auto-calculated in `create#Presentation`? What calendar should be used for "banking days"?
2. Should MT734 be generated in `authorize#Presentation` when discrepancies are found (instead of, or in addition to, MT750)?
3. Should `applicantDecisionEnumId` on `TradeDocumentPresentation` be updated by `update#PresentationWaiver`?
4. Should `PresentationDiscrepancy.isWaived` be set when applicant waives discrepancies?
5. The `TradeDocumentPresentationItem` entity exists but is unused — should it be populated during `create#Presentation`?
