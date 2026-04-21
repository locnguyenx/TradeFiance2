# Technical Specification Document - Import LC Provision Collection

**Created**: 2026-03-15
**Updated**: 2026-03-19
**Phase**: Implementation Complete
**Agent**: @plan + @tech-designer
**Feature**: Import LC Provision Collection

---

## 1. Implementation Summary

### Status: COMPLETED

All planned features have been implemented and tested. The implementation supports multi-account provision collection with currency conversion.

---

## 2. Entities Implemented

### 2.1 LcProvisionCollection (Pre-existing)

Location: `runtime/component/TradeFinance/entity/TradeFinanceEntities.xml`

```xml
<entity entity-name="LcProvisionCollection" package="moqui.trade.finance">
    <field name="collectionId" type="id" is-pk="true"/>
    <field name="lcId" type="id"/>
    <field name="targetProvisionAmount" type="currency-amount"/>
    <field name="targetCurrencyUomId" type="id" default="'USD'"/>
    <field name="collectedAmount" type="currency-amount" default="0.00"/>
    <field name="collectionStatusId" type="id"/>
    <field name="collectionDate" type="date-time"/>
    <field name="collectedByUserId" type="id"/>
    <field name="remarks" type="text-medium"/>
    
    <relationship type="one" related="moqui.trade.finance.LetterOfCredit"/>
    <relationship type="one" title="CollectionStatus" related="moqui.basic.StatusItem">
        <key-map field-name="collectionStatusId" related="statusId"/>
    </relationship>
    <relationship type="many" related="moqui.trade.finance.LcProvisionCollectionEntry">
        <key-map field-name="collectionId"/>
    </relationship>
</entity>
```

### 2.2 LcProvisionCollectionEntry (Pre-existing)

```xml
<entity entity-name="LcProvisionCollectionEntry" package="moqui.trade.finance">
    <field name="collectionId" type="id" is-pk="true"/>
    <field name="entrySeqId" type="id" is-pk="true"/>
    <field name="partyId" type="id"/>
    <field name="accountId" type="text-short"/>
    <field name="sourceCurrencyUomId" type="id"/>
    <field name="sourceAmount" type="currency-amount"/>
    <field name="exchangeRate" type="number-decimal" precision="6"/>
    <field name="convertedAmount" type="currency-amount"/>
    <field name="convertedCurrencyUomId" type="id" default="'USD'"/>
    <field name="rateFetchedDate" type="date-time"/>
    <field name="entryStatusId" type="id"/>
    <field name="cbsHoldReference" type="text-short"/>
    <field name="cbsHoldDate" type="date-time"/>
    <field name="remarks" type="text-medium"/>
    
    <relationship type="one" related="moqui.trade.finance.LcProvisionCollection"/>
    <relationship type="one" title="EntryStatus" related="moqui.basic.StatusItem">
        <key-map field-name="entryStatusId" related="statusId"/>
    </relationship>
</entity>
```

---

## 3. Services Implemented

### 3.1 ProvisionCollectionServices

Location: `runtime/component/TradeFinance/service/moqui/trade/finance/ProvisionCollectionServices.xml`

| Service | Description | Status |
|---------|-------------|--------|
| `create#LcProvisionCollection` | Initialize a new provision collection | ✅ Implemented |
| `add#CollectionEntry` | Add account entry with currency conversion | ✅ Implemented |
| `delete#CollectionEntry` | Delete entry and recalculate total | ✅ Implemented |
| `validate#CollectionTotal` | Validate total matches target (tolerance: ±$0.01) | ✅ Implemented |
| `collect#ProvisionFunds` | Execute CBS holds for all entries | ✅ Implemented |
| `release#ProvisionCollection` | Release all CBS holds | ✅ Implemented |

### 3.2 CbsIntegrationServices Fixes

Location: `runtime/component/TradeFinance/service/moqui/trade/finance/CbsIntegrationServices.xml`

| Service | Changes | Status |
|---------|---------|--------|
| `release#Funds` | Fixed parameter inheritance from `hold#Funds` | ✅ Fixed |

---

## 4. UI Screens

### 4.1 ProvisionCollection Screen (NEW)

Location: `runtime/component/TradeFinance/screen/TradeFinance/ImportLc/Lc/ProvisionCollection.xml`

**Features:**
- Collection summary card (target, collected, remaining, status)
- Add entry form with account selection and currency dropdown
- Collection entries table with exchange rates and CBS references
- Action buttons (Validate, Collect Funds, Release Funds)
- Real-time total calculation
- Delete entry functionality

**Navigation:**
- URL: `/trade-finance/ImportLc/Lc/ProvisionCollection?lcId={lcId}&collectionId={collectionId}`
- From Financials screen: Click "Collect Provision" or "View Collection"

### 4.2 Financials Screen Updates

Location: `runtime/component/TradeFinance/screen/TradeFinance/ImportLc/Lc/Financials.xml`

**Changes:**
- Added provision collection transitions (create, add, validate, collect, release)
- Added collection summary section
- Added collection entries table
- Added action buttons for provision collection

---

## 5. Exchange Rates (CBS Mock)

| Currency Pair | Rate | Notes |
|---------------|------|-------|
| EUR/USD | 1.09 | 1 EUR = 1.09 USD |
| USD/EUR | 0.92 | 1 USD = 0.92 EUR |
| GBP/USD | 1.27 | 1 GBP = 1.27 USD |
| USD/GBP | 0.79 | 1 USD = 0.79 GBP |
| EUR/GBP | 0.86 | 1 EUR = 0.86 GBP |
| GBP/EUR | 1.16 | 1 GBP = 1.16 EUR |

---

## 6. Status Workflow

### Collection Status Flow

```
Draft → Complete → Collected → Released
  ↓         ↓          ↓
  └── (can return to Draft if edited)
```

### Entry Status Flow

```
Pending → Collected → Released
    ↓          ↓
  Failed (if CBS hold fails)
```

### Status Enumerations

| Status ID | Type | Description |
|-----------|------|-------------|
| LcPrvColDraft | Collection | Collection created, entries being added |
| LcPrvColComplete | Collection | Total matches target, ready for collection |
| LcPrvColCollected | Collection | Funds successfully held in CBS |
| LcPrvColReleased | Collection | Funds released |
| LcPrvEntryPending | Entry | Entry added, not yet collected |
| LcPrvEntryCollected | Entry | CBS hold placed |
| LcPrvEntryReleased | Entry | CBS hold released |
| LcPrvEntryFailed | Entry | CBS hold failed |

---

## 7. Validation Rules

### Tolerance
- **±$0.01 USD**: Total collected amount can differ from target by up to $0.01

### Business Rules
1. Only one active collection per LC at a time
2. Entries can only be added/deleted in Draft status
3. Collection must be Complete status before collecting
4. Only Collected collections can be released
5. Supported currencies: USD, EUR, GBP

---

## 8. Test Coverage

### Unit Tests

Location: `runtime/component/TradeFinance/src/test/groovy/moqui/trade/finance/TradeFinanceProvisionCollectionSpec.groovy`

| Test | Description | Status |
|------|-------------|--------|
| Create LcProvisionCollection | Initialize collection | ✅ Pass |
| Add collection entry with exchange rate | EUR conversion | ✅ Pass |
| Add multiple collection entries | Multi-currency total | ✅ Pass |
| Validate collection total | Tolerance validation | ✅ Pass |
| Collect provision funds | CBS holds | ✅ Pass |
| Release provision collection | CBS releases | ✅ Pass |
| Get exchange rate | CBS rate service | ✅ Pass |
| Handle unsupported currency | Error handling | ✅ Pass |

**Result**: 8/8 tests passing

---

## 9. Files Summary

| Type | Path | Action |
|------|------|--------|
| Entity | `entity/TradeFinanceEntities.xml` | No change (entities pre-existed) |
| Service | `service/ProvisionCollectionServices.xml` | Modified |
| Service | `service/CbsIntegrationServices.xml` | Modified |
| Screen | `screen/ProvisionCollection.xml` | Created |
| Screen | `screen/Financials.xml` | No change (pre-existed) |
| Test | `test/TradeFinanceProvisionCollectionSpec.groovy` | Created |

---

## 10. API Reference

### create#LcProvisionCollection

**Input:**
```json
{
  "lcId": "100123",
  "targetProvisionAmount": 10000.00,
  "targetCurrencyUomId": "USD"
}
```

**Output:**
```json
{
  "collectionId": "LCPC_abc123...",
  "success": true
}
```

### add#CollectionEntry

**Input:**
```json
{
  "collectionId": "LCPC_abc123...",
  "partyId": "DEMO_ORG_ABC",
  "accountId": "ACC_EUR_001",
  "sourceCurrencyUomId": "EUR",
  "sourceAmount": 5000.00
}
```

**Output:**
```json
{
  "entrySeqId": "LCE_xyz789...",
  "convertedAmount": 5450.00,
  "exchangeRate": 1.09,
  "success": true
}
```

### validate#CollectionTotal

**Input:**
```json
{
  "collectionId": "LCPC_abc123..."
}
```

**Output:**
```json
{
  "isValid": true,
  "status": "Complete",
  "collectedAmount": 10000.00,
  "targetAmount": 10000.00,
  "message": "Collection total is within tolerance of target"
}
```

### collect#ProvisionFunds

**Input:**
```json
{
  "collectionId": "LCPC_abc123..."
}
```

**Output:**
```json
{
  "success": true,
  "holdReferences": "SIM-HLD-123,SIM-HLD-124,SIM-HLD-125",
  "message": "Funds collected successfully"
}
```

---

## 11. Implementation Notes

### Known Limitations
- Only USD, EUR, GBP currencies supported
- Exchange rates are mocked (hardcoded)
- No account balance validation before hold

### Future Enhancements
- Additional currency support
- Real-time FX rate API integration
- Account balance validation
- Partial release functionality
- Collection history/reporting

---

**Document Status**: Complete
**Last Updated**: 2026-03-19
