---
Document ID: BDD-R8.4
Version: 0.1
System: Trade Finance
Module: Letter of Credit (LC)
BRD ID: BRD-002
Status: DRAFT
Last Updated: 2026-03-12
Author: [Antigravity]
---

# BDD Scenarios: LC Issuance & CBS Integration (R8.4) - Reversed

**Requirement:** R8.4 (Draft & Review Issuance details, Submit Issuance & Automated CBS Hooks, Supervisor Final Approval, Issue LC Instrument & MT700)

This document contains BDD scenarios reversed from the implemented test cases in `TradeFinanceIssuanceSpec.groovy` and `TradeFinanceCbsSpec.groovy`.

## Feature: LC Issuance & CBS Integration

**As a** Trade Finance System
**I want to** manage the final approval and issuance of an LC with automated CBS integration
**So that** funds are secured, accounting entries are posted, and SWIFT messages are generated correctly

### Background
A Letter of Credit application has been created and is undergoing the approval workflow.

### Scenario: Automated Provision Calculation and Hold during IPC Processing (Happy Path)
**Given** an LC application is in **Pending Processing** status
**When** the Trade Operator approves the application via the **"approve#LcByTradeOperator"** service
**Then** the system should automatically calculate the required provision (e.g., 10% of LC amount)
**And** the system should initiate a funds hold request to the CBS
**And** an **LcProvision** record should be created with status **Held** (`LcPrvHeld`) and a **cbsHoldReference** assigned
**And** the LC **transactionStatusId** should transition to **Pending Approval**.

### Scenario: Funds Hold Failure Blocks Approval (Edge Case)
**Given** an LC application is in **Pending Processing** status
**And** the customer has insufficient funds for the required provision
**When** the Trade Operator attempts to approve the application
**Then** the CBS integration should return an **"Insufficient funds"** error
**And** the system should block the approval process
**And** the LC **transactionStatusId** should remain in **Pending Processing**.

### Scenario: Final LC Issuance and Provision Activation (Happy Path)
**Given** an LC application has received final approval and is in **Approved** status (`LcTxApproved`)
**And** a provision hold exists in **Held** status (`LcPrvHeld`)
**When** the **"issue#LetterOfCredit"** service is executed
**Then** the system should transition the **lcStatusId** to **Issued** (`LcLfIssued`)
**And** the system should transition the **transactionStatusId** to **Closed** (`LcTxClosed`)
**And** the linked **LcProvision** record should be updated to **Active** (`LcPrvActive`)
**And** a SWIFT **MT700** message should be automatically generated and attached as an **LcDocument**.

### Scenario: CBS Credit Limit Verification
**Given** a Party exists with a credit limit in the CBS Simulator
**When** a funds hold is performed for an LC provision
**Then** the system should verify that the available credit limit for that Party is reduced by the hold amount accurately.

---

## Business Rules & Validation
1. **Mandatory Hold:** Approval by the Trade Operator (IPC step) is strictly blocked if the CBS cannot secure a funds hold for the required provision.
2. **Provision Rate:** The provision amount is calculated based on the `defaultProvisionRate` defined in the **LcProduct** template.
3. **Status Synchronization:** Issuance triggers a dual status update: `lcStatusId` becomes `LcLfIssued` and `transactionStatusId` becomes `LcTxClosed`.
4. **SWIFT Generation:** Successful issuance MUST generate an MT700 document. Failure to generate the SWIFT message should prevent the status transition.
5. **Auditing:** All CBS interactions (Hold/Release) and status transitions MUST be logged in the system for reconciliation.
