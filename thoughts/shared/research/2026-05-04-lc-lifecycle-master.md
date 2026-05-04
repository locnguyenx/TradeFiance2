---
date: 2026-05-04T12:43:00+0700
researcher: Antigravity
git_commit: 3c4c16be239f0b58c5b237e433e5bb7dbd99dc1a
branch: master
repository: moqui-trade
topic: "Master Research: LC Business Processes Lifecycle"
tags: [research, codebase, import-lc, lifecycle, swift, accounting, limits]
status: complete
last_updated: 2026-05-04
last_updated_by: Antigravity
---

# Research: LC Business Processes Lifecycle Master

**Date**: 2026-05-04T12:43:00+0700
**Researcher**: Antigravity
**Git Commit**: 3c4c16be239f0b58c5b237e433e5bb7dbd99dc1a
**Branch**: master
**Repository**: moqui-trade

## Research Question
Provide a unified view of the LC business processes (Issuance, Amendment, Presentation, Settlement) based on the existing research documents, correcting any inaccuracies regarding automation and identifying cross-process gaps.

## Summary
The Import LC lifecycle is fully mapped across four specialized research documents. While core entities and services exist for all stages, the system currently suffers from "disconnection" between business logic and automation. **Correction**: SWIFT auto-triggers ARE defined in `TradeFinanceSeca.xml` but may fail due to specific status conditions in the services. Accounting is implemented via conventional GL entries but uses hardcoded accounts. Fee calculation and Facility checks (Earmarking) are the primary architectural gaps.

## Unified Lifecycle Map

| Stage | Triggering Service | Main Entity | State Transition | SWIFT Message |
|-------|-------------------|-------------|------------------|---------------|
| **Issuance** | `create#ImportLetterOfCredit` | `ImportLetterOfCredit` | `LC_DRAFT` → `LC_ISSUED` | MT700 (Auto) |
| **Amendment**| `create#Amendment` | `ImportLcAmendment` | `LC_ISSUED` → `LC_AMENDMENT_PENDING` | MT707 (Auto) |
| **Presentation**| `create#Presentation` | `TradeDocumentPresentation` | `LC_ISSUED` → `LC_DOC_RECEIVED` | MT750/752 (Auto) |
| **Settlement**| `settle#Presentation` | `ImportLcSettlement` | `LC_ACCEPTED` → `LC_CLOSED` | MT103/202 (Manual) |

## Detailed Findings & Corrections

### 1. SWIFT Automation (Proven Discrepancies)
I have verified several critical failures in the automation layer:
- **MT700 Trigger Failure**: `TradeFinanceSeca.xml:28` expects `IMP_ISSUANCE`, but `ImportLcServices.xml:54` uses `IMP_NEW`. The SECA never fires.
- **MT750 Redundancy/Bug**: 
    - `authorize#Presentation` (ImportLcServices.xml:579) calls `generate#Mt750` explicitly if discrepancies exist.
    - `TradeFinanceSeca.xml:51` calls it **again** via SECA regardless of whether discrepancies exist.
    - Result: Clean presentations trigger a "NO DISCREPANCIES RECORDED" MT750, and discrepant ones trigger two messages.
- **MT707 Coverage**: The SECA is correctly configured for `AMEND_APPROVED`, but the `authorize#Amendment` service is not integrated into any multi-level approval flow (Maker/Checker).

### 2. Accounting & Fee Gap (Proven Disconnection)
- **Evidence**: `calculate#Fees` in `TradeAccountingServices.xml:113` is a stub.
- **Proof**: A `grep` search confirms `calculate#Fees` is **never called** by any business service in the component; it is only referenced in a single unit test (`BddCommonModuleSpec.groovy:411`).
- **GL Integration**: `post#TradeEntry` (lines 41-46) hardcodes GL accounts `111000` (Debit) and `411000` (Credit).

### 3. Limits & Facility Integration (Proven Gap)
- **Evidence**: `approve#ImportLetterOfCredit` (lines 221-302) performs authority checks but **no limit checks**.
- **Proof**: `ImportLcServices.xml` only calls `LimitServices.calculate#Earmark` within the `create#ShippingGuarantee` service (line 718). It is completely missing from the LC Issuance and Amendment flows.
- **Impact**: Banks can issue LCs that exceed the approved customer facility.

### 4. Presentation & SLA Gap
- **SLA**: 5-day window configuration exists in `TradeProductCatalog` and `TradeConfig`.
- **Gap**: `regulatoryDeadline` is not auto-populated in `create#Presentation`. The "holiday calendar" logic in `calculate#BusinessDate` is a simulation (`+ 3` days).

### 5. Settlement & Usance Gap (Proven Data Loss)
- **Evidence**: `ImportLcSettlement` entity exists (`ImportLcEntities.xml:159`).
- **Proof**: `create#ImportLcSettlement` in `TradeAccountingServices.xml:55` performs LC updates and accounting but **never calls `create#trade.importlc.ImportLcSettlement`**.
- **Impact**: No persistent record of individual settlement transactions exists in the domain-specific entity; only GL entries and LC totals are updated.
- **Usance**: `maturityDate` is defined on the entity but never populated. Auto-queuing of settlement on maturity is entirely missing.

## Code References
- `runtime/component/TradeFinance/service/TradeFinanceSeca.xml:1` - Central automation rules
- `runtime/component/TradeFinance/service/trade/TradeAccountingServices.xml:104` - Fee stub
- `runtime/component/TradeFinance/data/TradeFinanceMasterData.xml:37` - Fee configuration data
- `runtime/component/TradeFinance/service/trade/LimitServices.xml:6` - Limit calculation logic

## Architecture Documentation
The system follows a **Transaction-Linked Pattern**. Every major lifecycle event creates a `TradeTransaction` record which acts as the audit trail and state transition trigger. Business-specific fields are stored in "extension" entities like `ImportLetterOfCredit` or `ImportLcAmendment`, while common fields (amount, currency, dates) live in `TradeInstrument`.

## Historical Context (from thoughts/)
- `thoughts/shared/research/2026-05-04-import-lc-issuance.md` - Detailed issuance fields and service flow.
- `thoughts/shared/research/2026-05-04-import-lc-amendments.md` - Amendment logic and beneficiary consent.
- `thoughts/shared/research/2026-05-04-import-lc-presentation.md` - Document examination and discrepancy handling.
- `thoughts/shared/research/2026-05-04-import-lc-settlement.md` - Payment and accounting entries.

## Related Research
- [Issuance Research](file:///Users/me/myprojects/moqui-trade/thoughts/shared/research/2026-05-04-import-lc-issuance.md)
- [Amendments Research](file:///Users/me/myprojects/moqui-trade/thoughts/shared/research/2026-05-04-import-lc-amendments.md)
- [Presentation Research](file:///Users/me/myprojects/moqui-trade/thoughts/shared/research/2026-05-04-import-lc-presentation.md)
- [Settlement Research](file:///Users/me/myprojects/moqui-trade/thoughts/shared/research/2026-05-04-import-lc-settlement.md)

## Open Questions
1. Why did the previous research miss the SECAs? Are they not triggering in tests?
2. Should the hardcoded GL accounts be moved to `TradeConfig` or a new `AccountingConfiguration` entity?
3. How should the "Banking Day" calendar be implemented for SLA calculation? Is there a Mantle dependency we can use?
