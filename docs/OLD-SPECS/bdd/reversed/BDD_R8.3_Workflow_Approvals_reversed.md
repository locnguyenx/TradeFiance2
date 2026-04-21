---
Document ID: BDD-R8.3-Workflow
Version: 0.1
System: Trade Finance
Module: Letter of Credit (LC)
BRD ID: BRD-002
Status: DRAFT
Last Updated: 2026-03-12
Author: [Antigravity]
---

# BDD Scenarios: Workflow, Collateral & Approvals (R8.3) - Reversed

**Requirement:** R8.3-UC3 (Manage Customer Credit Limits), R8.3-UC4 (Application Approval Routing)

This document contains BDD scenarios reversed from the implemented test cases in `TradeFinanceWorkflowSpec.groovy`.

## Feature: Workflow, Collateral & Approvals

**As a** Trade Finance User
**I want to** manage collateral info and progress applications through a multi-level approval workflow
**So that** credit risk is assessed and the application is authorized by the correct bank roles

### Background
A Draft LC application has been created.

### Scenario: Update Collateral and Retrieve Credit Limit (Step 4)
**Given** an LC application is in **Draft** status
**When** the user updates the application detail with **Collateral Information** (e.g., Secured = "Y", Secured % = 10)
**Then** the system should record the **Credit Agreement ID** and **Collateral Description**
**And** the system should automatically retrieve the **Available Credit Limit** from the CBS
**And** store it on the LC record for review.

### Scenario: Multi-Level Approval Workflow (Happy Path)
**Given** a Draft LC has been submitted for review
**When** the application is approved by the **Branch Supervisor**
**Then** the status should transition from **Pending Review** to **Pending Processing**
**And When** the application is approved by the **Trade Operator**
**Then** the status should transition to **Pending Approval**
**And When** the application is approved by the **Trade Supervisor**
**Then** the transaction status should reach **Approved**
**And** the LC lifecycle status should transition to **Applied**.

### Scenario: Returning an Application for Correction (Edge Case)
**Given** an application is in the approval queue (e.g., **Pending Review**)
**When** a Supervisor identifies missing information and performs a **Return** action
**Then** the system should transition the status to **Returned**
**And** record the supervisor's comments in the LC History
**And** the application should be available to the CSR for correction and resubmission.

---

## Business Rules & Validation
1. **Role-Based Access:** Each stage of the workflow (Review, Processing, Approval) MUST be performed by the authorized user role.
2. **Sequential Logic:** Transitions must follow the mandated path: Draft -> Review -> Processing -> Approval -> Approved.
3. **CBS Integration (Limit):** Updating collateral details MUST trigger a call to the CBS to fetch the latest credit limit for the applicant.
4. **Transparency:** Every action in the workflow (Approval, Rejection, Return) MUST be logged with the user's ID and timestamp.
5. **Return Flow:** Applications in `Returned` status can be corrected and submitted again, restarting the workflow from the first review stage.
