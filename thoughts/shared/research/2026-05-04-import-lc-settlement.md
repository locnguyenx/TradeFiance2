---
date: 2026-05-04T11:35:27+0700
researcher: locnguyenx
git_commit: 3c4c16be239f0b58c5b237e433e5bb7dbd99dc1a
branch: master
repository: TradeFiance2
topic: "REQ-IMP-PRC-04: Settlement & Payment implementation"
tags: [research, codebase, import-lc, settlement, payment, sight, usance, moqui, swift, mt103, mt202, mt732]
status: complete
last_updated: 2026-05-04
last_updated_by: locnguyenx
---

# Research: REQ-IMP-PRC-04 — Settlement & Payment Implementation

**Date**: 2026-05-04T11:35:27+0700
**Researcher**: locnguyenx
**Git Commit**: 3c4c16be239f0b58c5b237e433e5bb7dbd99dc1a
**Branch**: master
**Repository**: TradeFiance2

## Research Question
How is the Settlement & Payment process (REQ-IMP-PRC-04) implemented? This covers Sight LC settlement, Usance (Deferred Payment) LCs, and the SWIFT payment messages (MT103, MT202, MT732).

## Summary
Settlement is implemented via `settle#Presentation` and `create#ImportLcSettlement` services. The `ImportLcSettlement` entity supports both Sight and Usance LCs with `settlementTypeEnumId` and `maturityDate` fields. Accounting entries are posted via `post#TradeEntry`. However, SWIFT payment messages (MT103/MT202 for Sight, MT732 for Usance) are **not auto-triggered**. Usance auto-queuing on maturity date is **not implemented**.

## Detailed Findings

### 1. ImportLcSettlement Entity

**File:** `runtime/component/TradeFinance/entity/ImportLcEntities.xml:159-183`

| Field | Type | Description | Line |
|-------|------|-------------|------|
| `settlementId` | id (PK) | Unique identifier | 160 |
| `presentationId` | id | Links to presentation | 161 |
| `instrumentId` | id | Links to LC | 162 |
| `principalAmount` | number-decimal | Settlement amount | 163 |
| `remittanceCurrency` | id | Currency for payment | 164 |
| `valueDate` | date | Funds availability date | 165 |
| `fxRate` | number-decimal | Exchange rate | 166 |
| `localEquivalent` | number-decimal | Local currency equivalent | 167 |
| `debitAccountId` | text-medium | Applicant's account to debit | 168 |
| `appliedMarginAmount` | number-decimal | Cash collateral applied | 169 |
| `netDebitAmount` | number-decimal | Total debit after margin | 170 |
| `chargesDetailEnumId` | id | OUR/BEN/SHA (Tag 71A) | 171 |
| `maturityDate` | date | For Usance LCs | 172 |
| `settlementTypeEnumId` | id | SIGHT/DEFERRED/ACCEPTANCE | 173 |
| `forwardContractRef` | text-medium | Treasury FX contract | 174 |
| `settlementStatusId` | id | SETTLE_PENDING/EXECUTED/FAILED | 175 |

**Settlement Statuses** (defined in `TradeFinanceSeedData.xml:208-210`):
- `SETTLE_PENDING` — Default
- `SETTLE_EXECUTED`
- `SETTLE_FAILED`

**Note:** `create#ImportLcSettlement` service (TradeAccountingServices.xml:55) performs accounting and LC updates but does **NOT** persist an `ImportLcSettlement` entity record to the database.

### 2. Settlement Services

#### `settle#Presentation` (`ImportLcServices.xml:476-533`)

**Inputs:**
- `presentationId` (required) — line 478
- `principalAmount` (required) — line 479
- `settlementTypeEnumId` (required) — line 480
- `isPartialDraw` (default "Y") — line 481

**Implementation:**
1. **Resolve presentation and LC** (lines 486-489)
2. **Approve pending presentation transaction** (lines 492-502): Finds `IMP_PRESENTATION` transaction, sets `TX_APPROVED`
3. **Create IMP_SETTLEMENT transaction** (lines 505-519): `transactionTypeEnumId: 'IMP_SETTLEMENT'`, `transactionStatusId: 'TX_APPROVED'`
4. **Call create#ImportLcSettlement** (line 522-523): Delegates to accounting service
5. **Update presentation status** (lines 525-526): `presentationStatusId: 'PRES_SETTLED'`
6. **Handle partial vs full draw** (lines 528-531): If `isPartialDraw == 'N'`, transitions LC to `LC_CLOSED`

#### `create#ImportLcSettlement` (`TradeAccountingServices.xml:55-103`)

**Implementation:**
1. **Post accounting entries** (lines 69-75):
   - `LC_SETTLEMENT_PRINCIPAL` via `post#TradeEntry`
   - `LC_SETTLEMENT_FEES` via `post#TradeEntry` (if charges > 0)
2. **Update LC state** (lines 77-96):
   - Sets `businessStateId: 'LC_SETTLED'` (line 78) — but this is immediately overridden by targetState logic
   - Deducts from `effectiveOutstandingAmount` (line 79)
   - Adds to `cumulativeDrawnAmount` (line 80)
   - Determines `targetState`: `LC_CLOSED` if zero outstanding, else `LC_ISSUED` (line 83)
3. **Handle revolving LC** (lines 85-90): Calls `evaluate#Reinstatement`
4. **Update facility utilization** (lines 98-101): Calls `LimitServices.update#Utilization` with negative delta

**Critical gap:** This service is named `create#ImportLcSettlement` but does **NOT** create an `ImportLcSettlement` entity record. It only performs accounting and LC state updates.

### 3. Sight LC Settlement

**Flow:**
1. Presentation → `LC_ACCEPTED` (via `authorize#Presentation`)
2. `settle#Presentation` called with `settlementTypeEnumId: "SIGHT"` (line 480)
3. Accounting entries posted via `post#TradeEntry` (DR `111000`, CR `411000`)
4. LC state: `LC_ACCEPTED` → `LC_SETTLED` → `LC_ISSUED` (partial) or `LC_CLOSED` (full)

**SWIFT Messages for Sight LCs:**
- **MT103** (Single Customer Credit Transfer): `SwiftGenerationServices.xml:443-468` — **NOT auto-triggered**
- **MT202** (Financial Institution Transfer): `SwiftGenerationServices.xml:470-493` — **NOT auto-triggered**

**Gap:** No SECA rule in `TradeFinanceSeca.xml` triggers MT103 or MT202 generation during settlement.

### 4. Usance (Deferred Payment) LC Handling

#### Usance Fields in ImportLetterOfCredit Entity

**File:** `runtime/component/TradeFinance/entity/ImportLcEntities.xml:11-50`

| Field | Line | Purpose |
|-------|------|---------|
| `tenorTypeId` | 11 | SIGHT/USANCE/ACCEPTANCE/MIXED |
| `usanceDays` | 12 | Number of days for usance |
| `usanceBaseDate` | 50 | Tag 42C, required if tenor != SIGHT |
| `deferredPaymentDetails` | 49 | Tag 42P, for DEF_PAYMENT/NEGOTIATION |
| `mixedPaymentDetails` | 48 | Tag 42M, required if tenor=MIXED |

#### MT732 (Advice of Discharge) Generation

**Service exists:** `generate#Mt732` (`SwiftGenerationServices.xml:391-415`)

**Implementation:**
- Sets message type "732" (line 410)
- Adds field 30: Date (line 411)
- Adds field 32B: Currency and Amount (line 412)
- Adds field 72Z: Sender to Receiver Info (line 413)
- Creates `SwiftMessage` record (line 414)

**Usage:** Called during clean presentation of Usance LCs to acknowledge the bank's commitment to pay on maturity date.

**Gap:** MT732 is **not auto-triggered** when a Usance presentation is accepted.

#### Maturity Date Tracking

**Field:** `ImportLcSettlement.maturityDate` (line 172) — defined but **not populated** during settlement.

**`evaluate#Tenor` service** (`TradeCommonServices.xml:118-124`):
```groovy
settlementState = maturityDays > 0 ? 'Suspended' : 'Sight'
```
- Only returns a logical state string
- Does **not** persist anything or schedule future settlement

### 5. Auto-Queuing of Settlement on Maturity Date — NOT IMPLEMENTED

**BRD Requirement:** System should track maturity date and automatically queue settlement when due.

**Current state:**
- No `JobSandbox` entries or scheduled job definitions found
- `ImportLcBatchServices.xml` only implements `batch#AutoExpiry` (lines 7-49) for LC expiry — **not settlement**
- `evaluate#Tenor` (TradeCommonServices.xml:118-124) returns "Suspended" for Usance LCs but does not create any scheduled job
- No references to Moqui's `JobSandbox` or scheduling mechanisms in TradeFinance component

**BDD Test:** `BDD-IMP-SET-01` (`BddImportLcModuleSpec.groovy:444-452`) only tests that `evaluate#Tenor` returns "Suspended" — does not verify actual queuing.

### 6. Accounting Entries Creation

**`post#TradeEntry` Service** (`TradeAccountingServices.xml:6-53`):

**Inputs:** `instrumentId`, `entryTypeEnumId`, `amount`, `currencyUomId`, `useLiveRate`

**Implementation:**
1. **Find instrument and applicant** (lines 16-30)
2. **Apply FX spread** (lines 20-24): Optional 0.5% spread if `useLiveRate=true`
3. **Create AcctgTrans** (lines 35-38): Via `mantle.ledger.transaction.AcctgTrans`
4. **Create AcctgTransEntry records** (lines 41-46):
   - **Debit:** glAccountId `111000` (DR)
   - **Credit:** glAccountId `411000` (CR)

**Entry types used in settlement** (lines 69-75):
- `LC_SETTLEMENT_PRINCIPAL` — principal settlement amount
- `LC_SETTLEMENT_FEES` — settlement charges (default 150.0)

**Note:** GL account IDs (`111000`, `411000`) are hardcoded. Error handling (lines 48-51) silently swallows exceptions.

### 7. State Transitions

**Defined in:** `TradeFinanceSeedData.xml:226-229`

**LC States:**
```
LC_ACCEPTED → LC_SETTLED (settle#Presentation)
LC_SETTLED → LC_ISSUED (partial settlement)
LC_SETTLED → LC_CLOSED (full settlement)
```

**Note:** `LC_SETTLED` state is transient — `create#ImportLcSettlement` (line 77) sets it, but the target state logic (line 82-95) immediately overrides it to `LC_ISSUED` or `LC_CLOSED`.

**Settlement Statuses** (defined but not fully used):
- `SETTLE_PENDING` (default on entity)
- `SETTLE_EXECUTED`
- `SETTLE_FAILED`

## Code References
- `runtime/component/TradeFinance/entity/ImportLcEntities.xml:159` - ImportLcSettlement entity
- `runtime/component/TradeFinance/entity/ImportLcEntities.xml:11` - ImportLetterOfCredit (usance fields)
- `runtime/component/TradeFinance/service/trade/importlc/ImportLcServices.xml:476` - settle#Presentation
- `runtime/component/TradeFinance/service/trade/TradeAccountingServices.xml:55` - create#ImportLcSettlement
- `runtime/component/TradeFinance/service/trade/TradeAccountingServices.xml:6` - post#TradeEntry
- `runtime/component/TradeFinance/service/trade/SwiftGenerationServices.xml:391` - generate#Mt732
- `runtime/component/TradeFinance/service/trade/SwiftGenerationServices.xml:443` - generate#Mt103
- `runtime/component/TradeFinance/service/trade/SwiftGenerationServices.xml:470` - generate#Mt202
- `runtime/component/TradeFinance/service/trade/TradeCommonServices.xml:118` - evaluate#Tenor
- `runtime/component/TradeFinance/service/ImportLcBatchServices.xml:7` - batch#AutoExpiry (no settlement scheduling)
- `runtime/component/TradeFinance/data/TradeFinanceSeedData.xml:208` - Settlement statuses
- `runtime/component/TradeFinance/data/TradeFinanceSeedData.xml:226` - State transitions

## Architecture Documentation

### Settlement Flow (Current Implementation)
```
LC_ACCEPTED (Sight LC)
    ↓ settle#Presentation (ImportLcServices.xml:476)
    ├→ Approve IMP_PRESENTATION transaction
    ├→ Create IMP_SETTLEMENT transaction
    ├→ Post accounting entries (DR 111000, CR 411000)
    ├→ Update LC: deduct outstanding, add to cumulative drawn
    ├→ Release facility utilization (negative delta)
    └→ LC → LC_SETTLED → LC_ISSUED (partial) OR LC_CLOSED (full)
    ⚠️ MT103/MT202 NOT auto-generated

LC_ACCEPTED (Usance LC)
    ↓ (on clean presentation)
    ├→ Generate MT732 (acknowledges future payment) — NOT AUTO-TRIGGERED
    └→ Maturity date tracking — NO AUTO-QUEUING IMPLEMENTED
```

### Key Patterns
- **Presentation-driven settlement**: Settlement is always triggered by an accepted presentation
- **Partial vs full draw**: `isPartialDraw` flag determines if LC closes or returns to Issued state
- **Revolving LC support**: `evaluate#Reinstatement` (called at line 85) reinstates amount for revolving LCs
- **Accounting integration**: GL entries posted via `post#TradeEntry`, but GL accounts are hardcoded
- **Settlement entity gap**: `create#ImportLcSettlement` service doesn't persist the entity it's named after

## Related Research
- [2026-05-04-import-lc-issuance.md](./2026-05-04-import-lc-issuance.md) — Issuance creates the LC that gets settled
- [2026-05-04-import-lc-amendments.md](./2026-05-04-import-lc-amendments.md) — Amendments affect effective amounts that are settled
- [2026-05-04-import-lc-presentation.md](./2026-05-04-import-lc-presentation.md) — Presentation must be accepted before settlement

## Open Questions
1. Should MT103 (or MT202) be auto-generated in `settle#Presentation`? What determines which message to use?
2. Should MT732 be auto-generated when a Usance presentation is accepted (in `authorize#Presentation`)?
3. How should Usance auto-queuing be implemented? Via Moqui's `JobSandbox`? What should trigger the initial scheduling?
4. Should `create#ImportLcSettlement` actually persist the `ImportLcSettlement` entity record?
5. Should GL account IDs (`111000`, `411000`) be configurable via an entity rather than hardcoded?
6. The `ImportLcSettlement.maturityDate` field is never populated — should `settle#Presentation` set this based on `usanceDays`?
