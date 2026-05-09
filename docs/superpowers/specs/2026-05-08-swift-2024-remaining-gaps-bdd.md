# BDD Supplement: Remaining SWIFT SRG 2024 Gaps
**ABOUTME:**
BDD scenarios for the SWIFT SRG 2024 remaining gaps supplement BRD.
Covers MT 700 enhancements, MT 740/747 reimbursement, Nostro reconciliation, Tag 77J, Tag 23S.

**Related BRD:** [2026-05-08-swift-2024-remaining-gaps-brd.md](file:///Users/me/myprojects/moqui-trade/docs/superpowers/specs/2026-05-08-swift-2024-remaining-gaps-brd.md)

## Traceability Matrix

| Scenario | US ID | FR ID(s) | Type |
| :--- | :--- | :--- | :--- |
| Scenario 1 | US-ISS-04 | FR-SWG-20 | Happy Path |
| Scenario 2 | US-ISS-04 | FR-SWG-20, ISS-SWV-18/19 | Edge Case |
| Scenario 3 | US-RMB-01 | FR-SWG-21, RMB-SWV-01/02 | Happy Path |
| Scenario 4 | US-RMB-01 | FR-SWG-21, RMB-SWV-01 | Edge Case |
| Scenario 5 | US-RMB-02 | FR-SWG-22, RMB-SWV-06/07 | Happy Path |
| Scenario 6 | US-RMB-02 | FR-SWG-22, RMB-SWV-07 | Edge Case |
| Scenario 7 | US-RMB-03 | FR-RMB-01, RMB-REC-01/02 | Happy Path |
| Scenario 8 | US-RMB-03 | FR-RMB-01, RMB-REC-03 | Edge Case |
| Scenario 9 | US-RMB-03 | FR-RMB-02, RMB-REC-04 | Edge Case |
| Scenario 10 | US-PRE-03 | FR-PRE-08, PRE-SWV-08/09 | Happy Path |
| Scenario 11 | US-PRE-03 | FR-PRE-08, PRE-SWV-10 | Edge Case |
| Scenario 12 | US-CAN-03 | FR-CAN-04, CAN-SWV-04 | Happy Path |
| Scenario 13 | US-CAN-03 | FR-CAN-04, CAN-SWV-05 | Edge Case |

---

## Feature 1 Scenarios: MT 700 Issuance Enhancements

### Scenario 1: Payment Condition fields included in MT 700 generation
**Given** an Import LC draft with `paymentCondBeneText` set to "Payment upon receipt of clean B/L" and `paymentCondBankText` set to "Reimburse via MT 202 only"
**And** `applicableRulesEnumId` set to `UCP_LATEST`
**When** the Checker authorizes the issuance
**Then** the generated MT 700 includes Tag 49G with content "Payment upon receipt of clean B/L"
**And** the generated MT 700 includes Tag 49H with content "Reimburse via MT 202 only"
**And** the generated MT 700 includes Tag 40E with content "UCP LATEST VERSION"

### Scenario 2: Payment Condition fields rejected with invalid characters
**Given** an Import LC draft with `paymentCondBeneText` containing the character `@`
**When** the Maker submits for approval
**Then** the system rejects submission with error "Payment Conditions (Beneficiary) contains invalid character '@' — X charset only"
**And** the transaction remains in Draft state

---

## Feature 2 Scenarios: Reimbursement Authorization & Nostro Reconciliation

### Scenario 3: MT 740 auto-generated alongside MT 700 when Reimbursing Bank assigned
**Given** an Import LC draft with amount USD 100,000
**And** a `TP_REIMBURSING_BANK` party assigned with `nostroAccountRef` = "36112345"
**And** `authExpiryDate` set to 30 days after LC expiry
**And** `reimbursingChargesEnumId` set to `REIMB_OUR`
**And** `availableWithEnumId` set to `AVAIL_ANY_BANK`
**When** the Checker authorizes the issuance
**Then** the system generates an MT 700 to the Advising Bank
**And** the system generates an MT 740 to the Reimbursing Bank
**And** the MT 740 Tag 20 matches the MT 700 Tag 20 exactly
**And** the MT 740 Tag 25 contains "36112345"
**And** the MT 740 Tag 32B contains "USD100000,00"
**And** the MT 740 Tag 58a contains "ANY BANK"
**And** the MT 740 Tag 40F contains "URR LATEST VERSION"
**And** a `NostroReconciliation` record is created with status `RECON_PENDING`

### Scenario 4: MT 740 not generated when no Reimbursing Bank assigned
**Given** an Import LC draft with no `TP_REIMBURSING_BANK` party assigned
**When** the Checker authorizes the issuance
**Then** the system generates an MT 700 to the Advising Bank
**But** no MT 740 message is generated
**And** no `NostroReconciliation` record is created

### Scenario 5: MT 747 auto-generated when financial amendment authorized
**Given** an Issued LC with a `TP_REIMBURSING_BANK` assigned
**And** `authExpiryDate` = 2026-09-30
**When** the Maker initiates an External Amendment with `amountIncrease` = USD 25,000 and `newAuthExpiryDate` = 2026-12-31
**And** the Checker authorizes the amendment
**Then** the system generates an MT 707 to the Advising Bank
**And** the system generates an MT 747 to the Reimbursing Bank
**And** the MT 747 Tag 32B contains "USD25000,00"
**And** the MT 747 Tag 34B contains "USD125000,00"
**And** the MT 747 Tag 31E contains "261231"

### Scenario 6: MT 747 not generated for non-financial amendment
**Given** an Issued LC with a `TP_REIMBURSING_BANK` assigned
**When** the Maker initiates an External Amendment changing only `goodsDeltaText` (narrative change)
**And** the Checker authorizes the amendment
**Then** the system generates an MT 707 to the Advising Bank
**But** no MT 747 message is generated (no financial change)

### Scenario 7: Manual Nostro reconciliation matching (happy path)
**Given** an LC with a `NostroReconciliation` record in status `RECON_PENDING` with `expectedAmount` = USD 100,000
**When** the Maker enters `nostroDebitDate` = 2026-06-15, `nostroDebitAmount` = USD 100,000, and `nostroStatementRef` = "CITI-STM-98765"
**And** the Maker submits the match
**And** the Checker approves the reconciliation
**Then** the `NostroReconciliation` status transitions to `RECON_MATCHED`
**And** `matchedByUserId` is populated with the Checker's user ID
**And** `matchedDate` is populated with the current business date

### Scenario 8: Nostro reconciliation flags amount mismatch
**Given** an LC with a `NostroReconciliation` record in status `RECON_PENDING` with `expectedAmount` = USD 100,000
**When** the Maker enters `nostroDebitAmount` = USD 99,500
**And** the Maker submits the match
**Then** the `NostroReconciliation` status transitions to `RECON_UNMATCHED`
**And** the system displays a warning "Nostro debit amount (99,500.00) does not match expected amount (100,000.00)"

### Scenario 9: LC closure blocked while Nostro reconciliation pending
**Given** an LC that is fully drawn and settled
**And** a `NostroReconciliation` record in status `RECON_PENDING`
**When** the system attempts to transition the LC to "Closed"
**Then** the transition is blocked with message "Cannot close LC: Nostro reconciliation pending for Reimbursing Bank"
**And** the LC remains in its current state

---

## Feature 3 Scenarios: Presentation Validation Tightening (Tag 77J)

### Scenario 10: Discrepancy line count enforced at submission
**Given** a discrepant presentation with 12 discrepancy entries totaling 65 lines of text
**When** the Maker attempts to add a 13th discrepancy entry containing 8 lines
**Then** the system blocks the addition with message "Adding this discrepancy would exceed the 70-line SWIFT limit (current: 65/70, adding: 8)"
**And** the discrepancy entry form shows "65/70 lines used"

### Scenario 11: Tag 77J generation-time safety net catches overflow
**Given** a discrepant presentation that passed UI validation with exactly 70 lines
**When** the SWIFT generation service assembles the MT 750 Tag 77J content
**And** line wrapping at 50 characters causes the content to expand to 72 lines
**Then** the generation is aborted with error "Tag 77J exceeds 70-line limit after formatting (72 lines)"
**And** no MT 750 message is saved
**And** the error is logged in the audit trail

---

## Feature 4 Scenarios: MT 707 Cancellation Request (Tag 23S)

### Scenario 12: Structured cancellation via MT 707 with Tag 23S
**Given** an Issued LC
**When** the Maker initiates an External Amendment with `isCancellationRequest` = Y
**And** the Checker authorizes the amendment
**Then** the generated MT 707 includes Tag 23S with value "CANCEL"
**And** the amendment follows the standard Beneficiary Consent workflow
**And** upon Beneficiary acceptance, the LC transitions to "Closed / Cancelled"

### Scenario 13: Mixed cancellation and amendment blocked
**Given** an Issued LC
**When** the Maker initiates an External Amendment with `isCancellationRequest` = Y
**And** the Maker also enters an `amountIncrease` of USD 10,000
**Then** the system blocks submission with error "Cancellation request cannot be combined with other amendment changes"
**And** the transaction remains in Draft state
