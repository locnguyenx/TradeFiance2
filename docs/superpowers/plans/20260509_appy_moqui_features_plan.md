# Apply Moqui Framework Best Practices to Trade Finance Entity Definitions

Comprehensive refactoring of `TradeCommonEntities.xml` and `ImportLcEntities.xml` to fully utilize Moqui's entity definition features. This goes beyond just adding relationships — we're applying all the framework best practices observed in `framework/entity/BasicEntities.xml`, `framework/entity/SecurityEntities.xml`, and `mantle-udm` (AccountingAccountEntities, OrderEntities).

## Reference Sources

| Source | Features Observed |
|---|---|
| `framework/BasicEntities.xml` | `cache`, `short-alias` on entities; `enable-audit-log` on status fields; `master` definitions; seed-data inline |
| `framework/SecurityEntities.xml` | `one-nofk` for cross-package refs; `short-alias` on relationships |
| `mantle-udm/AccountingAccountEntities.xml` | `optimistic-lock` on key entities; `enable-audit-log="true"` on status, `"update"` on mutable business fields; `short-alias` on every entity + relationship; `cache="never"` on transactional entities; comprehensive `master` definitions |
| `mantle-udm/OrderEntities.xml` | `master` with `use-master="default"` for detail expansion; `type="many"` reverse relationships |
| `Data+Model+Patterns.md` | `title` convention matching `enumTypeId`/`statusTypeId`; `enable-audit-log` for status history |

---

## Feature Gap Analysis

| Feature | Current State | Target State |
|---|---|---|
| **Relationships** | ~8 defined out of ~55 needed | All `type="id"` fields linked |
| **`enable-audit-log`** | None used anywhere | On all status/state fields and key mutable business fields |
| **`short-alias`** on entities | None | On all master entities (enables clean REST paths) |
| **`short-alias`** on relationships | None | On all relationships (enables nested REST expansion) |
| **`cache`** attribute | None | `cache="true"` on config entities (TradeProductCatalog, TradeStandardClause) |
| **`master`** definitions | None | On key master entities (TradeInstrument, ImportLetterOfCredit) |
| **`optimistic-lock`** | None | On entities where concurrent updates are likely (TradeInstrument, ImportLetterOfCredit) |
| **`type="many"` reverse relationships** | None | Key parent→child navigations |
| **Remove redundant field** | `guaranteeStatusId` duplicates `sgStatusId` | Remove `guaranteeStatusId`, update service + tests |

---

## Proposed Changes

### [MODIFY] [TradeCommonEntities.xml](file:///Users/me/myprojects/moqui-trade/runtime/component/TradeFinance/entity/TradeCommonEntities.xml)

---

#### Entity: `TradeInstrument`

**Entity-level changes:**
- Add `short-alias="tradeInstruments"`
- Add `optimistic-lock="true"` (concurrency protection for amount/status updates)

**Audit log:**
- `businessStateId` → `enable-audit-log="true"` (lifecycle transitions are critical audit trail)
- `amount` → `enable-audit-log="update"` (financial value changes)
- `outstandingAmount` → `enable-audit-log="update"`
- `expiryDate` → `enable-audit-log="update"`

**Add relationships (8 new `type="one"`):**

| Field | Rel Type | Related | Title | Short-Alias |
|---|---|---|---|---|
| `businessStateId` | `one` | `moqui.basic.StatusItem` | `TradeLcBusinessState` | `businessState` |
| `previousBusinessStateId` | `one-nofk` | `moqui.basic.StatusItem` | `PreviousBusinessState` | `previousState` |
| `instrumentTypeEnumId` | `one` | `moqui.basic.Enumeration` | `TradeInstrumentType` | `instrumentType` |
| `productEnumId` | `one` | `moqui.basic.Enumeration` | `TradeProductType` | `productType` |
| `currencyUomId` | `one` | `moqui.basic.Uom` | `Currency` | `currencyUom` |
| `customerFacilityId` | `one` | `trade.CustomerFacility` | — | `facility` |
| `makerUserId` | `one-nofk` | `moqui.security.UserAccount` | `Maker` | `maker` |
| `priorityEnumId` | `one` | `moqui.basic.Enumeration` | `TradePriority` | `priority` |

**Add `type="many"` reverse relationships:**

| Related | Short-Alias |
|---|---|
| `trade.TradeInstrumentParty` | `parties` |
| `trade.TradeTransaction` | `transactions` |

**Add `master` definition:**
```xml
<master>
    <detail relationship="businessState"/>
    <detail relationship="instrumentType"/>
    <detail relationship="currencyUom"/>
    <detail relationship="facility"/>
    <detail relationship="priority"/>
    <detail relationship="parties"><detail relationship="party"/></detail>
</master>
```

---

#### Entity: `CustomerFacility`

**Entity-level:** Add `short-alias="customerFacilities"`

**Relationships:**

| Field | Rel Type | Related | Title | Short-Alias |
|---|---|---|---|---|
| `ownerPartyId` | `one` | `trade.TradeParty` | `Owner` | `owner` |
| `currencyUomId` | `one` | `moqui.basic.Uom` | `Currency` | `currencyUom` |

---

#### Entity: `TradeTransactionAudit`

**Relationships:**

| Field | Rel Type | Related | Title | Short-Alias |
|---|---|---|---|---|
| `userId` | `one-nofk` | `moqui.security.UserAccount` | — | `user` |
| `actionEnumId` | `one` | `moqui.basic.Enumeration` | `TradeLaunchAction` | `action` |
| `transactionId` | `one` | `trade.TradeTransaction` | — | `transaction` |
| `instrumentId` | `one` | `trade.TradeInstrument` | — | `instrument` |

---

#### Entity: `TradeStandardClause`

**Entity-level:** Add `cache="true"` (configuration data, rarely changes)

**Relationships:**

| Field | Rel Type | Related | Title | Short-Alias |
|---|---|---|---|---|
| `clauseTypeEnumId` | `one-nofk` | `moqui.basic.Enumeration` | `ClauseType` | `clauseType` |

---

#### Entity: `TradeParty`

**Entity-level:** Add `short-alias="tradeParties"`

**Relationships:** Seed data for `TradePartyType` already exists in `TradeFinanceMasterData.xml` (line 54-56), so we use `type="one"` with FK enforcement.

| Field | Rel Type | Related | Title | Short-Alias |
|---|---|---|---|---|
| `partyTypeEnumId` | `one` | `moqui.basic.Enumeration` | `TradePartyType` | `partyType` |
| `countryOfRisk` | `one-nofk` | `moqui.basic.Geo` | `CountryOfRisk` | `country` |

---

#### Entity: `TradeInstrumentParty`

**Relationships:** Seed data for `TradePartyRole` already exists in `TradeFinanceMasterData.xml` (lines 59-73), so we use `type="one"` with FK enforcement.

| Field | Rel Type | Related | Title | Short-Alias |
|---|---|---|---|---|
| `roleEnumId` | `one` | `moqui.basic.Enumeration` | `TradePartyRole` | `role` |

---

#### Entity: `FeeConfiguration`

**Entity-level:** Add `short-alias="feeConfigurations"`

**Audit log:** `statusId` → `enable-audit-log="true"`

**Relationships:**

| Field | Rel Type | Related | Title | Short-Alias |
|---|---|---|---|---|
| `feeEventEnumId` | `one` | `moqui.basic.Enumeration` | `TradeFeeType` | `feeEvent` |
| `calculationTypeEnumId` | `one` | `moqui.basic.Enumeration` | `TradeFeeCalcMethod` | `calcMethod` |
| `currencyUomId` | `one` | `moqui.basic.Uom` | `Currency` | `currencyUom` |
| `frequencyEnumId` | `one` | `moqui.basic.Enumeration` | `TradeFeeFrequency` | `frequency` |
| `statusId` | `one` | `moqui.basic.StatusItem` | `TradeFeeStatus` | `status` |

---

#### Entity: `TradeProductCatalog`

**Entity-level:** Add `short-alias="tradeProductCatalogs"`, `cache="true"` (configuration data)

**Audit log:** `statusId` → `enable-audit-log="true"`

**Relationships:**

| Field | Rel Type | Related | Title | Short-Alias |
|---|---|---|---|---|
| `productTypeEnumId` | `one` | `moqui.basic.Enumeration` | `TradeProductType` | `productType` |
| `statusId` | `one` | `moqui.basic.StatusItem` | `TradeProductStatus` | `status` |
| `allowedTenorEnumId` | `one-nofk` | `moqui.basic.Enumeration` | `AllowedTenor` | `allowedTenor` |
| `accountingFrameworkEnumId` | `one-nofk` | `moqui.basic.Enumeration` | `AccountingFramework` | `accountingFramework` |
| `defaultSwiftFormatEnumId` | `one-nofk` | `moqui.basic.Enumeration` | `DefaultSwiftFormat` | `defaultSwiftFormat` |

---

#### Entity: `UserAuthorityProfile`

**Relationships:**

| Field | Rel Type | Related | Title | Short-Alias |
|---|---|---|---|---|
| `userId` | `one-nofk` | `moqui.security.UserAccount` | — | `user` |
| `makerCheckerFlag` | `one` | `moqui.basic.Enumeration` | `TradeMakerCheckerFlag` | `makerCheckerType` |
| `delegationTierId` | `one` | `moqui.basic.Enumeration` | `TradeAuthorityTier` | `delegationTier` |
| `currencyUomId` | `one` | `moqui.basic.Uom` | `Currency` | `currencyUom` |

---

#### Entity: `TradeTransaction`

**Entity-level:** Add `short-alias="tradeTransactions"`

**Audit log:** `transactionStatusId` → `enable-audit-log="true"`

**Relationships:**

| Field | Rel Type | Related | Title | Short-Alias |
|---|---|---|---|---|
| `transactionTypeEnumId` | `one` | `moqui.basic.Enumeration` | `TradeTransactionType` | `transactionType` |
| `transactionStatusId` | `one` | `moqui.basic.StatusItem` | `TradeTransactionStatus` | `transactionStatus` |
| `makerUserId` | `one-nofk` | `moqui.security.UserAccount` | `Maker` | `maker` |
| `checkerUserId` | `one-nofk` | `moqui.security.UserAccount` | `Checker` | `checker` |
| `priorityEnumId` | `one` | `moqui.basic.Enumeration` | `TradePriority` | `priority` |

---

#### Entity: `TradeApprovalRecord`

**Relationships:**

| Field | Rel Type | Related | Title | Short-Alias |
|---|---|---|---|---|
| `approverUserId` | `one-nofk` | `moqui.security.UserAccount` | `Approver` | `approver` |

---

### [MODIFY] [ImportLcEntities.xml](file:///Users/me/myprojects/moqui-trade/runtime/component/TradeFinance/entity/ImportLcEntities.xml)

---

#### Entity: `ImportLetterOfCredit`

**Entity-level:**
- Add `short-alias="importLettersOfCredit"`
- Add `optimistic-lock="true"` (concurrent amendment/settlement risk)

**Audit log:**
- `businessStateId` → `enable-audit-log="true"`
- `effectiveAmount` → `enable-audit-log="update"`
- `effectiveExpiryDate` → `enable-audit-log="update"`
- `effectiveOutstandingAmount` → `enable-audit-log="update"`
- `totalAmendmentCount` → `enable-audit-log="update"`

**Relationships (11 new):**

| Field | Rel Type | Related | Title | Short-Alias |
|---|---|---|---|---|
| `businessStateId` | `one` | `moqui.basic.StatusItem` | `TradeLcBusinessState` | `businessState` |
| `chargeAllocationEnumId` | `one-nofk` | `moqui.basic.Enumeration` | `ChargeAllocation` | `chargeAllocation` |
| `partialShipmentEnumId` | `one` | `moqui.basic.Enumeration` | `TradeShipmentOption` | `partialShipment` |
| `transhipmentEnumId` | `one` | `moqui.basic.Enumeration` | `TradeShipmentOption` | `transhipment` |
| `confirmationEnumId` | `one` | `moqui.basic.Enumeration` | `TradeConfirmation` | `confirmation` |
| `lcTypeEnumId` | `one` | `moqui.basic.Enumeration` | `TradeLcType` | `lcType` |
| `availableByEnumId` | `one` | `moqui.basic.Enumeration` | `TradeAvailableBy` | `availableBy` |
| `availableWithEnumId` | `one-nofk` | `moqui.basic.Enumeration` | `AvailableWith` | `availableWith` |
| `effectiveCurrencyUomId` | `one` | `moqui.basic.Uom` | `EffectiveCurrency` | `effectiveCurrency` |
| `applicableRulesEnumId` | `one` | `moqui.basic.Enumeration` | `ApplicableRules` | `applicableRules` |
| `reimbursingChargesEnumId` | `one` | `moqui.basic.Enumeration` | `ReimbursingCharges` | `reimbursingCharges` |

**Add `type="many"` reverse relationships:**

| Related | Short-Alias |
|---|---|
| `trade.importlc.ImportLcAmendment` | `amendments` |
| `trade.importlc.TradeDocumentPresentation` | `presentations` |
| `trade.importlc.ImportLcShippingGuarantee` | `shippingGuarantees` |
| `trade.importlc.ImportLcSettlement` | `settlements` |
| `trade.importlc.SwiftMessage` | `swiftMessages` |

**Add `master` definition:**
```xml
<master>
    <detail relationship="businessState"/>
    <detail relationship="lcType"/>
    <detail relationship="confirmation"/>
    <detail relationship="instrument"/>
    <detail relationship="amendments"/>
    <detail relationship="presentations"/>
    <detail relationship="settlements"/>
</master>
```

---

#### Entity: `SwiftMessage`

| Field | Rel Type | Related | Title | Short-Alias |
|---|---|---|---|---|
| `messageStatusId` | `one` | `moqui.basic.Enumeration` | `SwiftMessageStatus` | `messageStatus` |

---

#### Entity: `ImportLcShippingGuarantee`

**Field removal:** Remove `guaranteeStatusId` (redundant with `sgStatusId`).

> [!IMPORTANT]
> **Service + Test Impact of removing `guaranteeStatusId`:**
> - [ImportLcServices.xml:1067](file:///Users/me/myprojects/moqui-trade/runtime/component/TradeFinance/service/trade/importlc/ImportLcServices.xml#L1067): `create#ShippingGuarantee` passes `guaranteeStatusId: 'SG_ISSUED'` → change to `sgStatusId: 'SG_ISSUED'`
> - [ImportLcEntitiesSpec.groovy:255](file:///Users/me/myprojects/moqui-trade/runtime/component/TradeFinance/src/test/groovy/trade/ImportLcEntitiesSpec.groovy#L255): test sets `guaranteeStatusId: "SG_DRAFT"` → change to `sgStatusId: "SG_DRAFT"`
> - [ImportLcEntitiesSpec.groovy:263](file:///Users/me/myprojects/moqui-trade/runtime/component/TradeFinance/src/test/groovy/trade/ImportLcEntitiesSpec.groovy#L263): test asserts `sg.guaranteeStatusId == "SG_DRAFT"` → change to `sg.sgStatusId == "SG_DRAFT"`

**Audit log:** `sgStatusId` → `enable-audit-log="true"`

| Field | Rel Type | Related | Title | Short-Alias |
|---|---|---|---|---|
| `sgStatusId` | `one` | `moqui.basic.StatusItem` | `TradeGuaranteeStatus` | `sgStatus` |

---

#### Entity: `TradeDocumentPresentation`

**Audit log:** `presentationStatusId` → `enable-audit-log="true"`

| Field | Rel Type | Related | Title | Short-Alias |
|---|---|---|---|---|
| `presentationStatusId` | `one` | `moqui.basic.StatusItem` | `TradePresentationStatus` | `presentationStatus` |
| `applicantDecisionEnumId` | `one-nofk` | `moqui.basic.Enumeration` | `ApplicantDecision` | `applicantDecision` |
| `documentDisposalEnumId` | `one` | `moqui.basic.Enumeration` | `DocumentDisposal` | `documentDisposal` |
| `claimCurrency` | `one` | `moqui.basic.Uom` | `ClaimCurrency` | `claimCurrencyUom` |

---

#### Entity: `TradeDocumentPresentationItem`

| Field | Rel Type | Related | Title | Short-Alias |
|---|---|---|---|---|
| `documentTypeEnumId` | `one-nofk` | `moqui.basic.Enumeration` | `DocumentType` | `documentType` |

---

#### Entity: `ImportLcAmendment`

**Audit log:** `amendmentBusinessStateId` → `enable-audit-log="true"`

**Relationships (15 new):**

| Field | Rel Type | Related | Title | Short-Alias |
|---|---|---|---|---|
| `amendmentBusinessStateId` | `one` | `moqui.basic.StatusItem` | `TradeAmendmentBusinessState` | `amendmentBusinessState` |
| `amendmentTypeEnumId` | `one` | `moqui.basic.Enumeration` | `TradeAmendmentType` | `amendmentType` |
| `beneficiaryConsentStatusId` | `one` | `moqui.basic.Enumeration` | `BeneficiaryConsentStatus` | `consentStatus` |
| `beneficiaryDecisionEnumId` | `one-nofk` | `moqui.basic.Enumeration` | `BeneficiaryDecision` | `beneficiaryDecision` |
| `chargeAllocationEnumId` | `one-nofk` | `moqui.basic.Enumeration` | `ChargeAllocation` | `chargeAllocation` |
| `newPartialShipmentEnumId` | `one-nofk` | `moqui.basic.Enumeration` | `NewPartialShipment` | `newPartialShipment` |
| `newTranshipmentEnumId` | `one-nofk` | `moqui.basic.Enumeration` | `NewTranshipment` | `newTranshipment` |
| `newConfirmationEnumId` | `one-nofk` | `moqui.basic.Enumeration` | `NewConfirmation` | `newConfirmation` |
| `newBeneficiaryPartyId` | `one-nofk` | `trade.TradeParty` | `NewBeneficiary` | `newBeneficiary` |
| `goodsActionEnumId` | `one-nofk` | `moqui.basic.Enumeration` | `DeltaAction` | `goodsAction` |
| `docsActionEnumId` | `one-nofk` | `moqui.basic.Enumeration` | `DeltaAction` | `docsAction` |
| `conditionsActionEnumId` | `one-nofk` | `moqui.basic.Enumeration` | `DeltaAction` | `conditionsAction` |
| `specialPaymentBeneActionEnumId` | `one-nofk` | `moqui.basic.Enumeration` | `DeltaAction` | `specialPaymentBeneAction` |
| `specialPaymentBankActionEnumId` | `one-nofk` | `moqui.basic.Enumeration` | `DeltaAction` | `specialPaymentBankAction` |
| `amendmentPaidByEnumId` | `one-nofk` | `moqui.basic.Enumeration` | `AmendmentPaidBy` | `amendmentPaidBy` |

---

#### Entity: `PresentationDiscrepancy`

| Field | Rel Type | Related | Title | Short-Alias |
|---|---|---|---|---|
| `waivedByUserId` | `one-nofk` | `moqui.security.UserAccount` | `WaivedBy` | `waivedBy` |

---

#### Entity: `ImportLcSettlement`

**Audit log:** `settlementStatusId` → `enable-audit-log="true"`

| Field | Rel Type | Related | Title | Short-Alias |
|---|---|---|---|---|
| `remittanceCurrency` | `one` | `moqui.basic.Uom` | `RemittanceCurrency` | `remittanceCurrencyUom` |
| `settlementTypeEnumId` | `one` | `moqui.basic.Enumeration` | `TradeSettlementType` | `settlementType` |
| `settlementStatusId` | `one` | `moqui.basic.StatusItem` | `TradeSettlementStatus` | `settlementStatus` |
| `chargesDetailEnumId` | `one-nofk` | `moqui.basic.Enumeration` | `ChargesDetail` | `chargesDetail` |

---

#### Entity: `NostroReconciliation`

**Audit log:** `matchStatusEnumId` → `enable-audit-log="true"`

| Field | Rel Type | Related | Title | Short-Alias |
|---|---|---|---|---|
| `reimbursingBankPartyId` | `one` | `trade.TradeParty` | `ReimbursingBank` | `reimbursingBank` |
| `expectedCurrency` | `one` | `moqui.basic.Uom` | `ExpectedCurrency` | `expectedCurrencyUom` |
| `matchStatusEnumId` | `one` | `moqui.basic.Enumeration` | `NostroReconStatus` | `matchStatus` |
| `matchedByUserId` | `one-nofk` | `moqui.security.UserAccount` | `MatchedBy` | `matchedBy` |

---

### Service + Test Impact Summary

#### [MODIFY] [ImportLcServices.xml](file:///Users/me/myprojects/moqui-trade/runtime/component/TradeFinance/service/trade/importlc/ImportLcServices.xml)

| Line | Change | Reason |
|---|---|---|
| 1067 | `guaranteeStatusId: 'SG_ISSUED'` → `sgStatusId: 'SG_ISSUED'` | Removed redundant `guaranteeStatusId` field |

#### [MODIFY] [ImportLcEntitiesSpec.groovy](file:///Users/me/myprojects/moqui-trade/runtime/component/TradeFinance/src/test/groovy/trade/ImportLcEntitiesSpec.groovy)

| Line | Change | Reason |
|---|---|---|
| 255 | `guaranteeStatusId: "SG_DRAFT"` → `sgStatusId: "SG_DRAFT"` | Removed redundant field |
| 263 | `sg.guaranteeStatusId == "SG_DRAFT"` → `sg.sgStatusId == "SG_DRAFT"` | Removed redundant field |

> [!WARNING]
> **Pre-existing issue found:** [SwiftReimbursementSpec.groovy](file:///Users/me/myprojects/moqui-trade/runtime/component/TradeFinance/src/test/groovy/trade/SwiftReimbursementSpec.groovy) uses `partyTypeEnumId: "TP_TYPE_BANK"` and `"TP_TYPE_COMMERCIAL"` (lines 42, 56, 68) instead of the correct seed data values `PARTY_BANK` and `PARTY_COMMERCIAL`. Once we add the FK-enforced relationship for `partyTypeEnumId`, this test will fail on FK constraint. We must fix these values.
> **enumId naming convention**: in this factoring, we've changed the way of naming enumId to use a Prefix which is abbr of enumtype to prevent id duplication. As a result, we must change the way of usaging enum in printing/displaying values that use enum's description instead of enum's id: for example, in  runtime/component/TradeFinance/service/trade/SwiftGenerationServices.xml (line 99), change from`lc.lcTypeEnumId ?: "IRREVOCABLE"` -> `lc.lcType?.description ?: "IRREVOCABLE"`

---

## Verification Plan

### Automated Tests

1. **Moqui entity validation**: `./gradlew load` — validates all entity definitions, relationships, key-maps at startup
2. **TradeFinance component tests**:
   ```bash
   ./gradlew cleanTest reloadSave :runtime:component:TradeFinance:test --tests trade.TradeFinanceMoquiSuite
   ```
3. **View-entity compilation**: Startup validation confirms all existing view-entities still work with the new relationship structure

### Manual Verification

- Inspect Moqui's REST API for expanded entities (e.g., `GET /rest/s1/trade/tradeInstruments/{id}` should now show nested `businessState`, `instrumentType` etc.)
- Verify `enable-audit-log` writes to `EntityAuditLog` on status transitions
