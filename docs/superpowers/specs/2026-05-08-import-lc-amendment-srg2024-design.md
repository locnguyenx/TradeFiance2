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

## 2. Backend Service Layer (`ImportLcServices.xml`)

### 2.1 `create#ExternalAmendment`
- Maps UI inputs to `ImportLcAmendment`.
- Calculates total liability to dictate the required Maker/Checker tier.
- Validates that `amountDecrease` does not exceed the current available balance.
- Creates `TradeTransaction` with type `IMP_AMENDMENT_EXT`.

### 2.2 `create#InternalAmendment`
- Maps UI inputs to `ImportLcInternalAmendment`.
- Creates `TradeTransaction` with type `IMP_AMENDMENT_INT`.

### 2.3 `AuthorizationServices.authorize#Instrument` (Checker Approval)
The system's global Maker/Checker service will be updated to switch on the two new transaction types:

**Case: `IMP_AMEND_EXT` (External Amendment):**
- **Liability Check:** If `amountIncrease` > 0, executes limit earmark from Core Banking immediately.
- **SWIFT Gen:** Calls `SwiftMessageBuilder` to generate MT 707.
- **State Transition:** Updates TradeTransaction to `TX_DISPATCHED` (waiting for Beneficiary Consent) rather than `TX_APPROVED`. Updates Amendment state to `AMEND_DISPATCHED`.

**Case: `IMP_AMEND_INT` (Internal Amendment):**
- No SWIFT Generation.
- **Immediate Merge:** Overwrites internal fields on Master LC (`ImportLetterOfCredit`) immediately.
- **State Transition:** Updates TradeTransaction to `TX_APPROVED` and Amendment state to `AMEND_APPROVED`.

### 2.4 Consent Processing Workflow (External Only)
- **`process#IncomingSwiftConsent`**: System listens for MT 730, maps to the corresponding `ImportLcAmendment`, and updates `beneficiaryDecisionEnumId` to ACCEPTED/REJECTED.
- **`authorize#AmendmentConsent`**: Checker reviews the logged consent.
  - If ACCEPTED: Calls the "Merge" routine to overwrite Master LC fields with the Delta fields. If `amountDecrease` > 0, releases the facility limit.
  - If REJECTED: Amendment marked DEAD. If `amountIncrease` > 0, releases the previously earmarked facility limit.

## 3. REST API Design (`trade.rest.xml`)
- `POST /api/trade/v1/import-lc/{instrumentId}/amendments/external` - Maps to `create#ExternalAmendment`.
- `POST /api/trade/v1/import-lc/{instrumentId}/amendments/internal` - Maps to `create#InternalAmendment`.
- `GET /api/trade/v1/import-lc/{instrumentId}/amendments` - Lists both external and internal amendments for the timeline.

## 4. Frontend UI Design (React)

### 4.1 Amendment Scope Selector
- When clicking "Initiate Amendment", user is presented with a Scope Selector:
  - **External (UCP 600)** -> Renders `ExternalAmendmentForm.tsx`
  - **Internal Bank Use Only** -> Renders `InternalAmendmentForm.tsx`

### 4.2 External Amendment Form (`ExternalAmendmentForm.tsx`)
- **Split-Pane Layout:** 
  - Left pane: Read-only values from the current Master LC (Current Amount, Current Expiry, Current Goods Description).
  - Right pane: Active input fields for the Delta.
- **Locked Fields:** LC Number, Currency, and Applicant fields are rendered as disabled `Input` components.
- **Smart Delta Narrative Components:** 
  - For narrative fields (e.g., Goods Description), renders a `<SmartDeltaField />` component.
  - `<SmartDeltaField />` contains a Select dropdown (Add, Delete, Replace All) and a Textarea for the exact text payload.
- **Validation:** 
  - Amount Decrease cannot be greater than the Unutilized Balance (fetched via API).
  - Expiry Date must be $\ge$ current date.

### 4.3 Internal Amendment Form (`InternalAmendmentForm.tsx`)
- Renders only the internal fields: Facility ID, Fee Account, Margin Account, RM ID.
- Displays a prominent banner: *"These changes will not be transmitted via SWIFT."*

### 4.4 Dashboard & Consent UI
- **Timeline View:** Amendments are badged as "Pending Consent" if they are Dispatched.
- **Checker Consent Approval:** An Action button appears for Checkers to approve automatically-logged Beneficiary consents, triggering the final Master LC merge.
