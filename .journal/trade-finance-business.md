# Trade Finance Business Knowledge

## 1. Lifecycle & Workflows

### 1.1 Import Letter of Credit (LC)
- **Draft**: Initial entry by the bank or applicant.
- **Pending/Issued**: The LC has been sent to the beneficiary via the advising bank.
- **Amendment**: Formal process to change financial or non-financial terms (Amount, Expiry, Parties).
- **Presentation/Drawing**: Beneficiary presents documents for payment.

### 1.2 LC Amendment (Shadow Record Model)
- **Concept**: Amendments do not modify the master LC directly while pending.
- **Shadow Record**: `LcAmendment` stores proposed changes.
- **Confirmation**: Upon approval, shadow fields are merged into the master `LetterOfCredit`.
- **Audit**: Every amendment creates a `LcHistory` record.

## 2. Drawing & Presentation
- **Presentation**: Documents registered as `LcDrawing` (Status: `LcDrReceived`).
- **Examination**: Bank checks documents against LC terms.
- **Compliance**:
    - **Compliant**: Status moves to `LcDrCompliant`.
    - **Discrepant**: Discrepancies recorded as `LcDiscrepancy`. SWIFT MT734 Refusal advice required if not waived.

## 3. SWIFT Standards
- **MT700**: Issuance of a Documentary Credit.
- **MT707**: Amendment of a Documentary Credit.
- **MT734**: Advice of Refusal.
- **MT756**: Advice of Reimbursement or Payment.

### Character Set Validation (X-Set)
All alphanumeric fields must only allow: `A-Z`, `a-z`, `0-9`, `/`, `-`, `?`, `:`, `(`, `)`, `.`, `,`, `'`, `+`, and `space`.

## 4. Financial Rules (UCP 600)
- **Expiry Date**: Must be after Issue Date.
- **Shipment Date**: Must be before or on Expiry Date.
- **Currency**: Must be a valid ISO 4217 code.
