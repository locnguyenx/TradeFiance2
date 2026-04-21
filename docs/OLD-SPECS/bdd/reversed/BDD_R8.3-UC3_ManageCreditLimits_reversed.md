---
Document ID: BDD-R8.3-UC3
Version: 0.1
System: Trade Finance
Module: Letter of Credit (LC)
BRD ID: BRD-002
Status: DRAFT
Last Updated: 2026-03-12
Author: [Antigravity]
---

# BDD Scenarios: Manage Customer Credit Limits (R8.3-UC3) - Reversed

**Requirement:** R8.3-UC3 (Manage collateral info, retrieve approved credit limit from CBS)

This document contains BDD scenarios reversed from the implemented test cases in `TradeFinanceWorkflowSpec.groovy`.

## Feature: Customer Credit Limit Management

**As a** CSR or Branch Operator
**I want to** update collateral information and verify the applicant's credit limit with the CBS
**So that** the bank can assess the risk of the LC application

### Background
A Draft LC application has been created in the system.

### Scenario: Update Collateral and Retrieve Credit Limit (Happy Path)
**Given** an LC application is in **Draft** status (`LcTxDraft`)
**When** the user updates the application via the **"update#LcApplicationDetail"** service with:
  - **isSecured**: "Y"
  - **securedPercentage**: 10
  - **creditAgreementId**: "CA-MOCK-001"
  - **collateralDescription**: "Cash margin 10%"
**Then** the system should store these details on the LC record
**And** the system should automatically call the CBS to retrieve the **Available Credit Limit** for the applicant
**And** the **availableCreditLimit** on the LC record should be updated to a value greater than 0.

---

## Business Rules & Validation
1. **Security Details:** The system MUST store collateral type, percentage, and agreement reference.
2. **CBS Integration:** Updating security/collateral information triggers an automated real-time check against the CBS Credit Limit service.
3. **Draft Requirement:** These details are typically filled while the transaction is still in a draft or review phase to support the decision-making process.
4. **Data Integrity:** The `availableCreditLimit` retrieved from CBS is stored as a snapshot on the LC to provide context at the time of application.
