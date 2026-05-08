# BDD Scenarios: Import LC Amendment (SRG 2024 Enhancement)

## Feature: Import LC Amendments

### Scenario 1: External Amendment increases amount and requires consent
**Given** an Import LC is in the "Issued" state with an available limit of $100,000
**When** the Maker initiates an External Amendment to increase the amount by $50,000
**And** the Maker submits the amendment
**Then** the system calculates the required Checker tier based on the new total liability
**And** upon Checker approval, the facility limit is earmarked for the extra $50,000
**And** an MT 707 is generated and the amendment state becomes "Dispatched"
**But** the Master LC effective amount remains $100,000

### Scenario 2: Beneficiary consents to External Amendment via SWIFT
**Given** an External Amendment is in the "Dispatched" state
**When** the system receives an incoming MT 730 indicating Beneficiary Acceptance
**Then** the system auto-logs the consent as "ACCEPTED"
**And** the amendment transitions to "Pending Consent Approval"
**When** a Checker approves the logged consent
**Then** the system merges the $50,000 increase into the Master LC effective amount
**And** the amendment state becomes "Merged"

### Scenario 3: Internal Amendment execution bypasses SWIFT
**Given** an Import LC is in the "Issued" state
**When** the Maker initiates an Internal Amendment to change the Fee Debit Account
**And** the Checker approves the internal amendment
**Then** the system immediately updates the Master LC's fee debit account
**And** no MT 707 message is generated
**And** the amendment state becomes "Executed"

### Scenario 4: Smart Delta Narrative payload generation
**Given** an External Amendment draft
**When** the Maker selects "ADD" for the Goods Description and types "Certificate of Origin required"
**Then** the system saves `goodsActionEnumId` as "ADD" and `goodsDeltaText` as "Certificate of Origin required"
**And** upon approval, the generated MT 707 Tag 45B payload correctly formats as `/ADD/Certificate of Origin required`
