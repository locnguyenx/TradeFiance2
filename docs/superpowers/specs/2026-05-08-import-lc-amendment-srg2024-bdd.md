# Consolidated BDD Supplement: Import LC Amendments (SRG 2024)

## Traceability Matrix
| Scenario | Maps to Requirement | Type |
| :--- | :--- | :--- |
| Scenario 1 | FR-AMD-01, FR-AMD-05 | Happy Path |
| Scenario 2 | FR-AMD-07, FR-AMD-05 | Happy Path |
| Scenario 3 | FR-AMD-01, FR-AMD-06 | Happy Path |
| Scenario 4 | FR-AMD-02, FR-AMD-03 | Happy Path |
| Scenario 5 | FR-AMD-04, FR-AMD-08 | Edge Case / Validation |

---

## Scenarios

### Scenario 1: External Amendment increases amount and blocks limits before consent
**Given** an Import LC is in the "Issued" state with an available limit of $100,000
**When** the Maker initiates an External Amendment to increase the amount by $50,000 using the Smart Delta UI
**And** the Maker submits the amendment
**Then** the system calculates the required Checker tier based on the new total liability
**And** upon Checker approval, the facility limit is immediately earmarked for the extra $50,000
**And** an MT 707 is generated and the amendment transaction state becomes "Dispatched"
**But** the Master LC effective amount remains $100,000

### Scenario 2: Beneficiary consents to External Amendment via incoming SWIFT
**Given** an External Amendment is in the "Dispatched" state (pending consent)
**When** the system receives an incoming MT 730 referencing the LC and Amendment Number indicating Beneficiary Acceptance
**Then** the system auto-logs the consent as "ACCEPTED"
**And** the amendment transitions to "Pending Consent Approval"
**When** a Checker reviews and approves the logged consent
**Then** the system executes the "Merge" routine
**And** the $50,000 increase is officially merged into the Master LC effective amount
**And** the amendment transaction state becomes "Merged"

### Scenario 3: Internal Amendment execution bypasses SWIFT and Consent
**Given** an Import LC is in the "Issued" state
**When** the Maker initiates an Internal Amendment to change the Fee Debit Account to a new account number
**And** the Checker approves the internal amendment
**Then** the system immediately updates the Master LC's internal fee debit account
**And** no MT 707 message is generated
**And** no Beneficiary Consent is required
**And** the amendment transaction state becomes "Executed"

### Scenario 4: Smart Delta Narrative payload generation
**Given** an External Amendment draft
**When** the Maker selects "ADD" for the Goods Description action dropdown and types "Certificate of Origin required" in the text input
**Then** the system saves `goodsActionEnumId` as "ADD" and `goodsDeltaText` as "Certificate of Origin required"
**And** upon approval, the generated MT 707 Tag 45B payload correctly formats as `/ADD/Certificate of Origin required`

### Scenario 5: System auto-injects mandatory SRG 2024 Header tags
**Given** an External Amendment is submitted for approval
**When** the SWIFT Generation service builds the MT 707 payload
**Then** the system automatically retrieves the original Date of Issue from the Master LC and populates Tag 31C
**And** the system automatically calculates the next amendment number and populates Tag 26E
**And** the system automatically injects `ACNF` into Tag 22A
**And** the UI strictly prevents the Maker from modifying the LC Currency or Applicant Name
