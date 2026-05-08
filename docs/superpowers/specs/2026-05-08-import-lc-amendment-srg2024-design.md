# Architecture Design Spec: Import LC Amendment (SRG 2024)

## 1. Entity Model Design

As per the BRD, we are splitting the amendment capabilities into two distinct entities to reflect the strict boundary between UCP 600 External Amendments and Internal Back-Office Amendments. We are abandoning the one-to-many relationship for narrative deltas in favor of a flat, structured schema.

### 1.1 `ImportLcAmendment` Entity (External/UCP 600)
*Replaces the old legacy amendment fields. If a field is not being amended, it remains null.*

**Linkage & Header**
- `amendmentId` (PK)
- `instrumentId` (FK)
- `amendmentNumber` (Integer, auto-incremented)
- `amendmentDate` (Date)
- `transactionRef` (String)

**Consent Tracking**
- `beneficiaryDecisionEnumId` (PENDING, ACCEPTED, REJECTED)
- `consentSwiftRef` (String, MT 730 incoming ref)

**Financials & Dates Delta**
- `newExpiryDate` (Date)
- `amountIncrease` (Decimal)
- `amountDecrease` (Decimal)
- `newTolerancePositive` (Integer)
- `newToleranceNegative` (Integer)
- `additionalAmountsText` (String)

**Narrative Engine (Smart Delta Flat Columns)**
- `goodsActionEnumId` (Enum: ADD, DELETE, REPALL) & `goodsDeltaText` (String)
- `docsActionEnumId` & `docsDeltaText`
- `conditionsActionEnumId` & `conditionsDeltaText`
- `specialPaymentBeneActionEnumId` & `specialPaymentBeneText`
- `specialPaymentBankActionEnumId` & `specialPaymentBankText`

**Logistics & Routing Delta**
- `newBeneficiaryPartyId`
- `newTenorTypeEnumId`, `newUsanceDays`, `newUsanceBaseDate`, `newDraweeBic`
- `newPartialShipmentEnumId`, `newTranshipmentEnumId`
- `newPortOfLoading`, `newPortOfDischarge`, `newReceiptPlace`, `newFinalDeliveryPlace`
- `newLatestShipmentDate`, `newPresentationPeriodDays`
- `newConfirmationEnumId`, `newConfirmingBankBic`, `newReimbursingBankBic`, `newAdviseThroughBankBic`

**Charges & Admin**
- `amendmentPaidByEnumId`
- `newChargeAllocationText`
- `newBankToBankInstructions`
- `senderToReceiverInfo`

### 1.2 `ImportLcInternalAmendment` Entity (Internal/Back-Office)
- `internalAmendmentId` (PK)
- `instrumentId` (FK)
- `amendmentDate` (Date)
- `transactionRef` (String)
- `newFeeDebitAccountId` (String)
- `newFacilityId` (String)
- `newMarginAccountId` (String)
- `newMarginPercentage` (Decimal)
- `newRelationshipManagerId` (String)

## 2. Service Layer Refactoring (`ImportLcServices.xml`)

### 2.1 `create#ExternalAmendment`
- Maps UI inputs to `ImportLcAmendment`.
- Calculates total liability to dictate the required Maker/Checker tier.
- Validates that `amountDecrease` does not exceed the current available balance.
- Creates `TradeTransaction` with type `IMP_AMENDMENT_EXT`.

### 2.2 `create#InternalAmendment`
- Maps UI inputs to `ImportLcInternalAmendment`.
- Creates `TradeTransaction` with type `IMP_AMENDMENT_INT`.

### 2.3 `authorize#ExternalAmendment`
- **Liability Check:** If `amountIncrease` > 0, executes limit earmark from Core Banking immediately.
- **SWIFT Gen:** Calls `SwiftMessageBuilder` to generate MT 707.
  - Automatically queries Master LC for Issue Date (Tag 31C), Parent LC Ref (Tag 23).
  - Uses the ActionEnumIds to format B-Tags (e.g., `/ADD/`).
- Transitions TradeTransaction to `TX_DISPATCHED`.

### 2.4 `authorize#InternalAmendment`
- No SWIFT Generation.
- Executes merge immediately: Overwrites internal fields on Master LC (`ImportLetterOfCredit`).
- Transitions TradeTransaction to `TX_APPROVED`.

### 2.5 Consent Processing Workflow
- **`process#IncomingSwiftConsent`**: System listens for MT 730, maps to the corresponding `ImportLcAmendment`, and updates `beneficiaryDecisionEnumId` to ACCEPTED/REJECTED.
- **`authorize#AmendmentConsent`**: Checker reviews the logged consent.
  - If ACCEPTED: Calls the "Merge" routine to overwrite Master LC fields with the Delta fields. If `amountDecrease` > 0, releases the facility limit.
  - If REJECTED: Amendment marked DEAD. If `amountIncrease` > 0, releases the previously earmarked facility limit.
