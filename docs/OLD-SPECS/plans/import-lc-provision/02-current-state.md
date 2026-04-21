# Current State Analysis - Import LC Provision Collection

**Created**: 2026-03-15
**Phase**: 2 - Current State Analysis
**Agent**: @explore + @reasoning
**Feature**: Import LC Provision Collection
**Input**: 01-business-requirements.md

---

## 1. Related Entities Found

| Entity | Path | Description |
|--------|------|-------------|
| LetterOfCredit | moqui.trade.finance.LetterOfCredit | Master LC entity with amount, currency, applicantPartyId |
| LcProvision | moqui.trade.finance.LcProvision | Detail entity with provisionAmount, provisionCurrencyUomId, provisionRate, cbsHoldReference, provisionStatusId |
| LcProduct | moqui.trade.finance.LcProduct | Product configuration with defaultProvisionRate |
| LcProductCharge | moqui.trade.finance.LcProductCharge | Default charges per product |
| LcCharge | moqui.trade.finance.LcCharge | LC charges with chargeAmount, chargeCurrencyUomId, cbsReference |
| LcDrawing | moqui.trade.finance.LcDrawing | Drawing entity with drawingAmount, drawingCurrencyUomId |
| CbsSimulatorState | moqui.trade.finance.CbsSimulatorState | Virtual CBS account state tracking |

---

## 2. Related Services Found

| Service | Path | Description |
|---------|------|-------------|
| hold#LcProvision | moqui.trade.finance.FinancialServices | Places hold on single applicant account, calculates amount from LC amount and provision rate |
| release#LcProvision | moqui.trade.finance.FinancialServices | Releases provisions via CBS |
| calculate#LcChargesAndProvisions | moqui.trade.finance.FinancialServices | Calculates charges and provisions based on product defaults |
| get#ExchangeRate | moqui.trade.finance.CbsIntegrationServices | Gets exchange rates (currently hardcoded USD/EUR only) |
| hold#Funds | moqui.trade.finance.CbsIntegrationServices | Places hold on customer account (interface) |
| release#Funds | moqui.trade.finance.CbsIntegrationServices | Releases fund holds (interface) |
| hold#FundsMock | moqui.trade.finance.CbsMockServices | Mock implementation for CBS funds hold |
| release#FundsMock | moqui.trade.finance.CbsMockServices | Mock implementation for CBS funds release |

---

## 3. Related Screens Found

| Screen | Path | Description |
|--------|------|-------------|
| Financials | TradeFinance/ImportLc/Lc/Financials | Shows Provisions list (amount, rate, status, CBS ref), Charges list |
| MainLC | TradeFinance/ImportLc/Lc/MainLC | LC detail with CBS reference |
| ProductConfig | TradeFinance/ImportLc/ProductConfig | Product configuration with Default Provision Rate |

---

## 4. Gap Analysis

| Requirement | Current State | Gap |
|-------------|---------------|-----|
| **1. Multi-account collection** - support multiple source accounts | Current implementation: Single LcProvision entity per LC with single provisionAmount and provisionCurrencyUomId. `hold#LcProvision` service calculates single amount from LC.amount * provisionRate. | **MAJOR GAP**: No support for multiple source accounts. Need new entity structure to track individual collection entries with source account details (accountId, currency, amount, exchange rate). |
| **2. Currency conversion** - convert non-USD to USD using CBS exchange rate | Current: `get#ExchangeRate` service exists but only hardcoded for USD/EUR (lines 80-81 in CbsIntegrationServices.xml). No conversion logic in provision services. | **GAP**: Exchange rate service limited to USD/EUR only. No conversion logic in `hold#LcProvision`. Need to fetch rate per entry and store in entity. |
| **3. Total validation** - validate total collected = X USD | Current: Single provision amount stored in LcProvision.provisionAmount. No validation of sum vs. LC amount. | **GAP**: No validation logic. Need aggregation of multiple collection entries and comparison with target provision amount. |
| **4. Exchange rate per entry** - fetch/store rate per collection | Current: LcProvision.provisionRate stores percentage, not exchange rate. No exchange rate field in LcProvision. | **GAP**: No exchange rate storage per collection entry. Need new field in collection entity to store fetched exchange rate. |
| **5. Individual tracking** - track each collection separately | Current: Single LcProvision record per LC. No individual tracking of multiple contributions. | **GAP**: Need new entity structure to support multiple collection entries per LC. |

---

## 5. Reuse Opportunities

| Component | Can Extend | Notes |
|-----------|------------|-------|
| **LcProvision entity** | Add new fields or create child entity | Current entity is too granular for single-provision model. Can add collectionEntrySeqId to support multiple entries, or create LcProvisionCollectionEntry child entity. |
| **FinancialServices.hold#LcProvision** | Refactor to support multi-account logic | Current service calculates single amount. Can extend to accept list of collection entries and aggregate them. |
| **CbsIntegrationServices.get#ExchangeRate** | Extend currency pairs | Current hardcoded to USD/EUR. Can extend to support multiple currency pairs or connect to real FX API. |
| **CbsIntegrationServices.hold#Funds** | Reuse for multiple holds | Interface already supports single account hold. Can be called multiple times for multiple accounts. |
| **Financials screen** | Add collection entry UI | Current screen shows single provision list. Can extend to show breakdown of multiple collection entries with currency conversion. |
| **CbsSimulatorState** | Extend for multi-currency tracking | Current tracks single currency per party. Can extend to support multiple currency balances per party. |

---

## 6. Potential Conflicts & Considerations

1. **Entity Structure Conflict**: Current LcProvision entity is designed for single provision per LC. Adding multi-account support requires either:
   - Modifying LcProvision to support multiple entries (breaking change)
   - Creating new child entity LcProvisionCollectionEntry (cleaner approach)

2. **Exchange Rate Timing**: Business requirement #5 (Edge Cases) mentions exchange rate fluctuation. Current `get#ExchangeRate` fetches rate at call time. Need to decide:
   - Lock rate at LC creation (simpler)
   - Fetch dynamically per collection entry (more accurate but complex)

3. **Status Management Conflict**: Current LcProvision has single status (LcPrvHeld, LcPrvReleased). Business requires per-entry status (Pending, Collected, Verified) and overall provision status aggregation.

4. **CBS Integration Complexity**: Current `hold#Funds` service places single hold. Multi-account collection requires multiple CBS calls, which increases:
   - Transaction complexity
   - Error handling complexity
   - Rollback requirements

5. **Data Migration**: Existing LcProvision records may need migration to new structure if entity changes are made.

---

## 7. Questions for Next Phase

- Should we create a new entity `LcProvisionCollectionEntry` or modify existing `LcProvision` to support multiple entries?
- What is the tolerance for rounding discrepancies in USD conversion (per requirement #5 in business rules)?
- Should exchange rates be locked at LC creation or fetched dynamically per collection entry?
- How should the system handle partial collections that don't meet the full provision amount?
- Are there specific compliance rules (AML checks) required for multi-account provision collections?
- What is the process for handling failed CBS exchange rate fetches?

---

**Last Updated**: 2026-03-15
