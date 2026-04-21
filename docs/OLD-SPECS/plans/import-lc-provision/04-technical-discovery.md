# Technical Discovery - Import LC Provision Collection

**Created**: 2026-03-15
**Phase**: 4 - Technical Discovery
**Agent**: @tech-designer + @plan
**Feature**: Import LC Provision Collection
**Input**: 01-business-requirements.md, 02-current-state.md, 03-future-state.md

---

## 1. New Entities

### 1.1 LcProvisionCollection

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

### 1.2 LcProvisionCollectionEntry

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

## 2. Modified Entities

| Entity | Changes |
|--------|---------|
| **LcProvision** | Add `collectionId` field (FK to LcProvisionCollection) |
| **CbsSimulatorState** | Add multi-currency support (accountId as PK) |

---

## 3. New Services

| Service | Purpose |
|---------|---------|
| create#LcProvisionCollection | Initialize collection |
| add#CollectionEntry | Add account entry with conversion |
| fetch#ExchangeRate | Get CBS exchange rate |
| validate#CollectionTotal | Validate total = X USD |
| collect#ProvisionFunds | Execute CBS holds |
| release#ProvisionCollection | Release all holds |

---

## 4. Modified Services

| Service | Changes |
|---------|---------|
| hold#LcProvision | Support collectionId parameter |
| release#LcProvision | Support collection release |
| get#ExchangeRate | Extend currency pairs |
| hold#Funds | Support accountId parameter |

---

## 5. Screen Changes

| Screen | Changes |
|--------|---------|
| **ProvisionCollection.xml** (NEW) | Main UI for multi-account collection |
| Financials.xml | Add collection link and summary |
| MainLC.xml | Add provision collection button |

---

## 6. Implementation Roadmap

### Phase 1: Core Infrastructure (Week 1-2)
1. Create LcProvisionCollection entity
2. Create LcProvisionCollectionEntry entity
3. Update CbsSimulatorState for multi-currency
4. Add status enumerations
5. Create create#LcProvisionCollection service

### Phase 2: Collection Entry Management (Week 2-3)
1. Extend get#ExchangeRate for more currencies
2. Create add#CollectionEntry service
3. Create validate#CollectionTotal service
4. Create recalculate#CollectionTotal service

### Phase 3: CBS Integration (Week 3-4)
1. Extend hold#Funds for accountId parameter
2. Create collect#ProvisionFunds service
3. Create release#ProvisionCollection service
4. Add rollback logic

### Phase 4: UI Implementation (Week 4-5)
1. Create ProvisionCollection screen
2. Add entry form with exchange rate display
3. Add real-time total calculation
4. Integrate collect button with validation

### Phase 5: Testing (Week 5-6)
1. Unit tests for collection services
2. Integration tests for CBS mock
3. UI tests for ProvisionCollection screen
4. End-to-end workflow test

---

## 7. Files to Create/Modify

| Type | Path | Action |
|------|------|--------|
| Entity | entity/TradeFinanceEntities.xml | Modify - Add new entities |
| Service | service/moqui/trade/finance/ProvisionCollectionServices.xml | Create |
| Service | service/moqui/trade/finance/FinancialServices.xml | Modify |
| Service | service/moqui/trade/finance/CbsIntegrationServices.xml | Modify |
| Screen | screen/TradeFinance/ImportLc/Lc/ProvisionCollection.xml | Create |
| Screen | screen/TradeFinance/ImportLc/Lc/Financials.xml | Modify |
| Data | data/10_TradeFinanceData.xml | Modify - Add status enumerations |

---

## 8. Status Enumerations

| Status ID | Status Type | Description |
|-----------|-------------|-------------|
| LcPrvColDraft | Collection | Collection created, entries being added |
| LcPrvColComplete | Collection | Total matches target, ready for collection |
| LcPrvColCollected | Collection | Funds successfully held in CBS |
| LcPrvColReleased | Collection | Funds released |
| LcPrvEntryPending | Entry | Entry added, not yet collected |
| LcPrvEntryCollected | Entry | CBS hold placed |
| LcPrvEntryReleased | Entry | CBS hold released |
| LcPrvEntryFailed | Entry | CBS hold failed |

---

**Last Updated**: 2026-03-15
