# Implementation Plan - Hardening Import LC Business Logic

This plan addresses identified gaps in the Import LC lifecycle, focusing on automation reliability, financial controls, and data integrity.

## User Review Required

> [!IMPORTANT]
> **SWIFT Automation Strategy**: I will align the SECA triggers with the existing `IMP_NEW` transaction type instead of creating a new `IMP_ISSUANCE` type. This maintains consistency with the current service layer.
> *Reference: [1. SWIFT Automation (Proven Discrepancies)](file:///Users/me/myprojects/moqui-trade/thoughts/shared/research/2026-05-04-lc-lifecycle-master.md#1-swift-automation-proven-discrepancies)*
>
> **GL Account Configuration**: I propose adding `GL_TRADE_DEBIT` and `GL_TRADE_CREDIT` to the `TradeConfig` entity to replace hardcoded values in `TradeAccountingServices.xml`.
> *Reference: [2. Accounting & Fee Gap (Proven Disconnection)](file:///Users/me/myprojects/moqui-trade/thoughts/shared/research/2026-05-04-lc-lifecycle-master.md#2-accounting--fee-gap-proven-disconnection)*

> [!IMPORTANT]
> **SWIFT Message Viewer**: I will implement a dedicated viewer within the project's frontend (Instrument Details) so business users can verify MTxxx messages directly in the application.

## Proposed Changes

---

### 1. SWIFT Automation & Lifecycle Triggers
Fix the disconnect between service-layer transaction updates and SECA automation rules.

#### [MODIFY] [TradeFinanceSeca.xml](file:///Users/me/myprojects/moqui-trade/runtime/component/TradeFinance/service/TradeFinanceSeca.xml)
- Change `IMP_ISSUANCE` to `IMP_NEW` in the MT700 trigger.
- Remove redundant `Mt750` trigger for `authorize#Presentation` or add a condition to check for discrepancies to avoid double-generation and clean-presentation noise.

#### [MODIFY] [ImportLcServices.xml](file:///Users/me/myprojects/moqui-trade/runtime/component/TradeFinance/service/trade/importlc/ImportLcServices.xml)
- Remove explicit `service-call` to `generate#Mt750` in `authorize#Presentation` to let the SECA handle it (or vice versa).
- Add `checkerUserId` and `checkerTimestamp` updates to all approval-related transaction updates.

---

### 2. Facility Limit Enforcement ("Earmarking")
Integrate limit checks into the LC approval workflow to prevent over-limit issuances.
*Reference: [3. Limits & Facility Integration (Proven Gap)](file:///Users/me/myprojects/moqui-trade/thoughts/shared/research/2026-05-04-lc-lifecycle-master.md#3-limits--facility-integration-proven-gap)*

#### [MODIFY] [ImportLcServices.xml](file:///Users/me/myprojects/moqui-trade/runtime/component/TradeFinance/service/trade/importlc/ImportLcServices.xml)
- Update `approve#ImportLetterOfCredit`: Call `trade.LimitServices.calculate#Earmark` before final approval.
- Add logic to block approval if the facility limit is exceeded.
- Add `LimitServices.update#Utilization` (positive delta) upon issuance approval.

---

### 3. Fee Calculation & Accounting Refactor
Replace stub implementations with configuration-driven logic.

#### [MODIFY] [TradeAccountingServices.xml](file:///Users/me/myprojects/moqui-trade/runtime/component/TradeFinance/service/trade/TradeAccountingServices.xml)
- Update `calculate#Fees`: Query `trade.common.FeeConfiguration` instead of using the current hardcoded logic.
- Update `post#TradeEntry`: Fetch GL account IDs from `trade.common.TradeConfig` instead of hardcoding `111000`/`411000`.

#### [MODIFY] [ImportLcServices.xml](file:///Users/me/myprojects/moqui-trade/runtime/component/TradeFinance/service/trade/importlc/ImportLcServices.xml)
- Call `calculate#Fees` in `create#ImportLetterOfCredit` to estimate fees for the initial draft.

---

### 4. Settlement Persistence
Ensure every settlement transaction is recorded in the domain-specific entity.
*Reference: [5. Settlement & Usance Gap (Proven Data Loss)](file:///Users/me/myprojects/moqui-trade/thoughts/shared/research/2026-05-04-lc-lifecycle-master.md#5-settlement--usance-gap-proven-data-loss)*

#### [MODIFY] [TradeAccountingServices.xml](file:///Users/me/myprojects/moqui-trade/runtime/component/TradeFinance/service/trade/TradeAccountingServices.xml)
- Update `create#ImportLcSettlement`: Add a `create#trade.importlc.ImportLcSettlement` call to persist the record.
- Populate `settlementDate`, `principalAmount`, `chargesAmount`, and `maturityDate` (for Usance LCs).

---

### 5. SWIFT Message Viewer
Implement a built-in SWIFT message viewer in the project frontend to allow business users to inspect generated messages without needing access to framework tools.

#### [NEW] Backend Service: `get#SwiftMessages`
- **File**: `SwiftGenerationServices.xml`
- **Logic**: Query `trade.importlc.SwiftMessage` for the given `instrumentId`. Return a list of messages with their types, statuses, and formatted content.

#### [MODIFY] REST API: `trade.rest.xml`
- Add `swift-messages` resource under `instrument/{instrumentId}`.

#### [MODIFY] Frontend: `InstrumentDetail` View
- Add a "SWIFT Messages" tab or button to the Instrument Detail page.
- Implement a `SwiftMessageViewer` component to display the list of messages and their raw SWIFT content in a formatted code block.

---

### 6. SLA & Business Date Logic
Improve the banking day calculation logic.

#### [MODIFY] [TradeCommonServices.xml](file:///Users/me/myprojects/moqui-trade/runtime/component/TradeCommon/service/trade/TradeCommonServices.xml)
- Update `calculate#BusinessDate`: Replace simple `+3` logic with a routine that skips weekends (Saturdays and Sundays).

## Verification Plan

### Automated Tests
- Run `TradeFinanceServicesSpec` to ensure existing flows remain green.
- Create a new Spock test case `TradeFinanceHardeningSpec.groovy` to verify:
    - MT700 generation after issuance approval.
    - Facility limit block (by setting a low limit on a customer facility).
    - Fee calculation against master data in `TradeFinanceMasterData.xml`.
    - Presence of `ImportLcSettlement` records after settlement.

### Manual Verification
- **Frontend SWIFT Viewer**: Navigate to the Instrument Detail page for an Issued LC and click the "SWIFT Messages" tab to confirm MT700/MT750/MT707 are generated correctly.
- **Facility Utilization**: Check the "Customer Exposure" or "Facility Detail" view in the frontend (if available) or DB to verify limit consumption after issuance.
- **Settlement Audit**: Verify that settlement records appear in the instrument's transaction history with correct amounts and dates.
