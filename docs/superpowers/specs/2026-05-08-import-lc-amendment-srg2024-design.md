# Architecture Design Spec: Import LC Amendment (SRG 2024)

## 1. Current Implementation Analysis
The current Moqui-trade implementation handles LC amendments via a single `ImportLcAmendment` entity and generic services in `ImportLcServices.xml` (`create#Amendment`, `update#Amendment`, `authorize#Amendment`).
- **Data Model Deficiencies:** It uses a generic `amountAdjustment` column instead of separating increases/decreases as required by SWIFT Tag 32B/33B. It stores narrative changes in single text blocks (`goodsDescription`, etc.) rather than distinguishing actions (Add/Delete/Replace), violating the SRG 2024 "Smart Delta" requirement.
- **Workflow Deficiencies:** It uses `isFinancial` to toggle Beneficiary Consent, which violates UCP 600 (all external amendments require consent). It lacks an isolated entity for purely internal back-office changes.

## 2. Proposed Data Model Enhancement

### 2.1 Refactoring `ImportLcAmendment` (External)
This entity will be modified to support SRG 2024 Tag 707 requirements.
- **Remove:** `amountAdjustment`, `amendmentNarrative`, `isFinancial`, `isBeneficiaryAcceptanceRequired`.
- **Add Financials:** `amountIncrease`, `amountDecrease`, `newTolerancePositive`, `newToleranceNegative`.
- **Add Smart Delta Narratives (Action/Text Pairs):** 
  - `goodsActionEnumId`, `goodsDeltaText`
  - `docsActionEnumId`, `docsDeltaText`
  - `conditionsActionEnumId`, `conditionsDeltaText`
  - `specialPaymentBeneActionEnumId`, `specialPaymentBeneText`
  - `specialPaymentBankActionEnumId`, `specialPaymentBankText`
- **Add Logistics & Terms:** `newBeneficiaryPartyId`, `newTenorTypeEnumId`, `newUsanceDays`, `newUsanceBaseDate`, `newDraweeBic`, `newPartialShipmentEnumId`, `newTranshipmentEnumId`, `newPortOfLoading`, `newPortOfDischarge`, `newReceiptPlace`, `newFinalDeliveryPlace`, `newLatestShipmentDate`, `newPresentationPeriodDays`, `newConfirmationEnumId`, `newConfirmingBankBic`, `newReimbursingBankBic`, `newAdviseThroughBankBic`.
- **Add Charges:** `amendmentPaidByEnumId`, `newBankToBankInstructions`, `senderToReceiverInfo`.

### 2.2 New Entity: `ImportLcInternalAmendment` (Internal)
- `internalAmendmentId` (PK)
- `instrumentId` (FK)
- `amendmentDate`
- `transactionRef`
- `newFeeDebitAccountId`
- `newFacilityId`
- `newMarginAccountId`
- `newMarginPercentage`
- `newRelationshipManagerId`

## 3. Service Layer Refactoring

### 3.1 `create#ExternalAmendment` (replaces `create#Amendment`)
- Captures all structured delta inputs.
- Calculates `newTotalAmount` (`effectiveAmount` + `increase` - `decrease`) strictly for Checker tier routing.
- Creates `ImportLcAmendment` and `TradeTransaction`.

### 3.2 `create#InternalAmendment`
- Captures internal fields.
- Creates `ImportLcInternalAmendment` and `TradeTransaction`.

### 3.3 `authorize#ExternalAmendment`
- Calculates facility earmarks: IF `amountIncrease` > 0, immediately earmarks limit from Core Banking. (Does NOT release on decrease until consent is logged).
- Generates the MT 707 via `SwiftMessageBuilder` utilizing the `ActionEnumId` prefix logic (e.g., `/ADD/`).
- Transitions state to `Dispatched` (waiting for consent).

### 3.4 `authorize#InternalAmendment`
- Bypasses SWIFT generation.
- Immediately overwrites the Master LC (`ImportLetterOfCredit`) with the internal field updates.
- Transitions state to `Executed`.

### 3.5 Consent Logging Workflow
- A new service `process#SwiftConsent` listens for incoming MT 730 messages.
- Updates `beneficiaryConsentStatusId` on the `ImportLcAmendment`.
- A new service `authorize#AmendmentConsent` allows the Checker to finalize the consent.
- Upon approval, the system executes the "Merge" routine: overwriting the Master LC effective values with the Amendment Delta values and releasing facility limits if the amount decreased.
