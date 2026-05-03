# UI/UX
## Import LC
### LC issuance
- The Save Draft button is visible only in the last step. When clicked, do checking required inputs validation for LC issuance creation. If there are any errors, show error messages and do not save the draft. If there are no errors, save the draft and show Success message.
- When click Next button, validate all fields in the current step. If there are any errors, show error messages and do not move to the next step. If there are no errors, move to the next step.

# Domain entity

## TradeInstrument (Transaction)

### Updated business rules for existing fields
* **transactionRef: **
auto generate when create new, Format: `TF-IMP-YY-NNNN` 
- `IMP`: Import LC, `EXP`: Export LC
- YY: 2 digits year
- NNNN: 4 digits number, using `NumberSequence`
* **transactionDate: **
auto generate when create new, Format: `YYYY-MM-DD`

### New fields
- beneficiaryName: free text, mapped to `Beneficiary` (field 59 of MT700/MT710/MT707...) , follow SWIFT MT700 data constraint. Use this field to store beneficiary name when beneficiary is not a party in the system. If beneficiary is a party in the system, use `TradeParty.partyName` instead.

## Import LC

Add all fields with values taken from TradeInstrument (Instrument & Financial Data):
- productEnumId
- applicantPartyId
- currencyUomId: LC currency
- amount: LC amount when issued (get from issuance transaction, not allowed to change after issuance)
- outstandingAmount: Remaining drawable balance (updated by draws/settlements)
- baseEquivalentAmount: Computed: `outstandingAmount × Daily Board Rate` at issuance
- issueDate
- expiryDate
- customerFacilityId

### Updated business rules for existing fields

#### Missing Fields of Block C: Financials & Terms

| SWIFT Tag | Description | M/O | System Source / Data Extraction Logic |
| :--- | :--- | :--- | :--- |
| **41a** | Available With ... By ... | M | Logic: Determine the "Available With" bank (Often `ANY BANK` or a specific Advising Bank BIC). Append the `ImportLc.tenorTypeEnumId` mapped to SWIFT codes: `BY PAYMENT`, `BY ACCEPTANCE`, `BY NEGOTIATION`, or `BY DEF PAYMENT`. |
| **42A** | Drawee | O | Typically the Issuing Bank. Extracted from the bank's own system profile. |