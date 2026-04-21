---
Document ID: BDD-R8.3-UC4
Version: 0.2
System: Trade Finance
Module: Letter of Credit (LC)
BRD ID: BRD-002
Status: DRAFT
Last Updated: 2026-03-12
Author: [Antigravity]
---

# BDD Scenarios: Application Approval Routing (R8.3-UC4) - Reversed

**Requirement:** R8.3-UC4 (Workflow CSR -> Branch Supervisor -> Trade Operator -> Trade Supervisor)

This document contains BDD scenarios reversed from the implemented test cases in `TradeFinanceWorkflowSpec.groovy`.

## Feature: Multi-Level Approval Workflow

**As a** Trade Finance System
**I want to** route an LC application through specific bank roles for review and authorization
**So that** all financial and regulatory controls are strictly enforced

### Background
An LC application has been created and security details have been verified.

### Scenario: High-Level Approval Path (Happy Path)
**Given** an LC application is in **Draft** status (`LcTxDraft`)
**When** the CSR submits it for review via **"submit#LetterOfCredit"**
**Then** the status transitions to **Pending Review**
**And When** the **Branch Supervisor** approves via **"approve#LcBySupervisor"**
**Then** the status transitions to **Pending Processing**
**And When** the **Trade Operator** (Back Office) approves via **"approve#LcByTradeOperator"**
**Then** the status transitions to **Pending Approval**
**And When** the **Trade Supervisor** (Back Office) performs the final approval via **"approve#LcByTradeSupervisor"**
**Then** the **transactionStatusId** becomes **Approved** (`LcTxApproved`)
**And** the **lcStatusId** transitions to **Applied** (`LcLfApplied`).

### Scenario: Returning an Application for Correction (Edge Case)
**Given** an application is in the approval queue (e.g., **Pending Review**)
**When** a reviewer performs a **Return** action via the **"return#LetterOfCredit"** service
**Then** the system should transition the status to **Returned** (`LcTxReturned`)
**And** the reviewer must provide mandatory **comments** (e.g., "Missing document")
**And** the comments must be recorded in the **LcHistory**.

### Scenario: Rejecting an LC Application (Edge Case)
**Given** an LC application is at any stage of the workflow
**When** an authorized user calls the **"reject#LetterOfCredit"** service
**Then** the system should update the **transactionStatusId** to **Rejected** (`LcTxRejected`)
**And** the LC **lcStatusId** should remain unchanged (e.g., `LcLfDraft`).

---

## Business Rules & Validation
1. **Mandatory Routing:** The application must pass through each role in sequence. Skipping steps (e.g., Draft to Pending Approval) is prohibited.
2. **Authority Limits:** Each approval service (`approve#LcBySupervisor`, `approve#LcByTradeOperator`, etc.) corresponds to a specific bank role.
3. **Return to Source:** A "Return" action allows the application to be corrected by the initiator and then resubmitted, restarting the workflow.
4. **History Trail:** Every workflow step (Submit, Approve, Return, Reject) MUST create a unique entry in **LcHistory** with the actor's ID and timestamp.
5. **Finality:** Once the Trade Supervisor approves, the application is ready for the technical "Issuance" step.
6. **Comments:** Rejection and Return actions MUST require a comment/justification.
