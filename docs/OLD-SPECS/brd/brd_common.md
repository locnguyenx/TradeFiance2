---
Document ID: BRD-003
Version: 1.0
Module: Trade Finance
Feature: Common Functions
Status: DRAFT
Last Updated: 2026-04-21
Author: [LocNX]
---

# Business Requirements Document: Trade Finance System - Common Functions

## 1. Executive Summary
This document outlines the requirements for the Common Functions module.

## 2. Standard Compliance
N/A

## 3. UI/UX Requirements
- **Grouped Layout**: The detail screen MUST group fields into General, Parties, Shipment, and Docs/Payment blocks.
- **Visual Status Tracking**: Key statuses MUST be displayed using high-visibility colored chips ("Premium Status Chips") in the detail header.
- **Reusable UI Components**: Core actions (e.g., LC Creation, Amendment Initiation) MUST use standardized, reusable dialog templates to ensure a consistent user experience across different entry points.
- **Hierarchical Navigation**: The system MUST follow a `Find (List) -> Detail (Standardized Tabs)` navigation pattern.
- **Cross-Module Linkage**: When viewing linked entities (e.g., LC from an Amendment), the target entity MUST be displayed in a **Read-Only** mode to prevent accidental data corruption.
- **Data Integrity**: Fields with invalid characters must be flagged immediately upon submission.
- **Activity Log**: Immutable history of all status changes and internal comments.

## 4. User Roles & Permissions
Define who will use the system and what business actions they are allowed to perform.

| User Role | Description | Permitted Actions |
| :--- | :--- | :--- |
| **Applicant** | Corporate Customer | Create Draft, View Own LCs, Submit Application |
| **Branch Operator** | Bank Front-Office Staff| Create Draft, View Own LCs, Submit Application |
| **Branch Supervisor** | Bank Front-Office Manager| View All LCs, Review, Reject |
| **Trade Operator** | Bank Back-Office Staff| View All LCs, Review, Reject, Issue |
| **Trade Supervisor** | Bank Back-Office Manager| View All LCs, Review, Approve, Reject |
| **Trade Auditor** | Risk/Compliance | View All LCs, View History (Read-Only) |

## 5. Business Lifecycle (State Machine)
### 5.1 Transaction Workflow Requirements

Define the statuses a record (transaction) goes through from creation to closure.

The system MUST support the following Transaction lifecycle states:
    - Draft
    - Pending Review
    - Pending Processing
    - Returned
    - Pending Approval
    - Approved
    - Rejected
    - Cancelled


**The state flow:**

| Current Status | User Action / Trigger | Next Status | Business Condition |
| :--- | :--- | :--- | :--- |
| `Draft` | Applicant or Branch Operator clicks "Submit for Review" | `Pending Review` | All mandatory application fields must be complete. |
| `Pending Review`| Branch Supervisor clicks "Submit for Processing" | `Pending Processing` | Requires Branch Supervisor authority. |
| `Pending Processing`| Trade Operator clicks "Submit for Approval" | `Pending Approval` | Requires Trade Operator authority. |
| `Pending Approval`| Trade Supervisor clicks "Approve" | `Approved` | Requires Trade Supervisor authority. |
| `Pending Review`/`Pending Approval`/`Pending Processing`  | Authorized user clicks "Reject" | `Rejected` | User must provide a rejection reason. |
| `Pending Review`/`Pending Approval`/`Pending Processing`  | Authorized user clicks "Return" | `Returned` | User must provide a returnning reason. |



## 7. Functionalities
  - Describe the functionalities of the system, including the business process flow, User Interface & Data Requirements.

### Manage LC Provision & Charge
#### Business process flow

1. Step 1: Upon LC issuance, the system calculates provisions based on the LC product configuration (`provisionPercentage`). A provision hold is placed on the Applicant's account via CBS integration.

2. Step 2: Charges are calculated based on the LC product's charge template. Common charges include: issuance commission, advising fee, amendment fee, negotiation/acceptance commission, discrepancy fee, courier charges.

3. Step 3: Charges are collected upfront or deducted during payment, depending on the `charges_71B` field instructions (e.g., "ALL CHARGES OUTSIDE [COUNTRY] ARE FOR ACCOUNT OF BENEFICIARY").

4. Step 4: Upon LC expiry, closure, or revocation, remaining provisions are released.

##### System Use Cases
- **1. Assessment of Charges & Provisions**: (Steps 1-2) 
  - Collect charges and provisions inputted in related transaction i.e LC Application, Amendment, Drawing...
  - Automated calculation based on product templates, show infomation on the transaction
- **2. CBS Accounting Integration**: (Step 3) (Integration Requirement) 
  When the transaction is Approved and transaction is Issuance, Amendment, Payment, Drawing: posting actual ledger entries of charges and provisions.
- **3. CBS Accounting Integration**: (Step 4) (Integration Requirement) 
  When the transaction is Approved and transaction is Expiry, Closure, or Revocation: Posting reversal  ledger entries of charges and provisions 

### Manage LC Provision
#### Business process flow

1. Step 1: Upon LC issuance, the system calculates provisions based on the LC product configuration (`provisionPercentage`). A provision hold is placed on the Applicant's account via CBS integration.

2. Step 2: Upon LC expiry, closure, or revocation, remaining provisions are released.

##### System Use Cases
- **1. Assessment of Provisions**: (Steps 1-2) 
  - Collect provisions inputted in related transaction i.e LC Application, Amendment, Drawing...
  - Automated calculation based on product templates, show infomation on the transaction
- **2. CBS Accounting Integration**: (Step 3) (Integration Requirement) 
  When the transaction is Approved and transaction is Issuance, Amendment, Payment, Drawing: posting actual ledger entries of charges and provisions.
- **3. CBS Accounting Integration**: (Step 4) (Integration Requirement) 
  When the transaction is Approved and transaction is Expiry, Closure, or Revocation: Posting reversal  ledger entries of charges and provisions 

### LC Provision Collection

#### Business process flow

1. Step 1: When an LC requires provision (collateral/guarantee), the system creates a Provision Collection record linked to the LC with a target provision amount.

2. Step 2: The applicant selects multiple accounts (in different currencies) to contribute to the provision collection. The system fetches exchange rates from CBS for each currency conversion.

3. Step 3: The system calculates the converted amount for each entry and maintains a running total. The collection status is "Draft" until the total matches the target.

4. Step 4: When the total collected amount matches the target provision (within ±0.01 USD tolerance), the collection status transitions to "Complete".

5. Step 5: Upon collection completion, the system executes CBS holds for all accounts. If any hold fails, all holds are rolled back and the collection status returns to "Draft".

6. Step 6: Once funds are successfully held, the collection status transitions to "Collected". The provision is now active for the LC.

7. Step 7: Upon LC expiry, closure, or revocation, the system releases all holds and transitions the collection status to "Released".

#### System Use Cases
- **R8.12-UC1: Initialize Provision Collection**: (Steps 1-2) Create collection record, select accounts, fetch exchange rates.
- **R8.12-UC2: Add Collection Entries**: (Step 3) Add account entries with currency conversion, maintain running total.
- **R8.12-UC3: Validate Collection Total**: (Step 4) Compare total with target, apply tolerance, update status.
- **R8.12-UC4: Collect Funds from Multiple Accounts**: (Step 5) Execute CBS holds with rollback on partial failure.
- **R8.12-UC5: Release Provision Collection**: (Step 7) Release all CBS holds upon LC closure.

#### Business Rules
- **Multi-Account Support**: Applicants can contribute provisions from multiple accounts in different currencies.
- **Currency Conversion**: Exchange rates are fetched from CBS in real-time for accurate conversion.
- **Tolerance**: Collection is considered complete if total is within ±0.01 USD of target.
- **Atomic Collection**: All CBS holds must succeed, or all are rolled back (no partial holds).
- **Account Eligibility**: Only accounts owned by the LC applicant can be used for provision collection.

### Manage LC Documents
#### Business process flow

1. Step 1: Throughout the LC lifecycle, various physical and electronic documents are generated or received: LC application form, SWIFT messages (MT700, MT707, MT750, MT756), transport documents, invoices, insurance policies.

2. Step 2: Each document is scanned and attached to the LC or Drawing record. The system tracks document type, reference number, date, and original/copy count.

3. Step 3: SWIFT messages generated by the system are stored as read-only attachments. Incoming SWIFT messages received via the messaging gateway are parsed and linked automatically.

## 9. Non-Functional Requirements

**Last Updated:** 2026-03-16
