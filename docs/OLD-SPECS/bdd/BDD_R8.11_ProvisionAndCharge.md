---
Document ID: BDD-R8.11
Version: 0.1
System: Trade Finance
Module: Letter of Credit (LC)
BRD ID: BRD-002
BRD Document Version: 1.0
Status: REVIEWED
Last Updated: 2026-03-13
Author: Antigravity & reviewed by Loc
---

# BDD Scenarios: Manage LC Provision & Charge (R8.11)

**Requirement:** R8.11 (Manage LC Provision & Charge - Assessment and CBS Integration)

This document outlines the behavior for calculating and posting LC-related provisions and charges, including integration with the Core Banking System (CBS).

## Feature: Manage LC Provision & Charge

**As a** Trade Finance Specialist (IPC Operator/Supervisor)
**I want** the system to automatically assess provisions and charges and sync them with CBS
**So that** financial risks are mitigated and accounting entries are accurately recorded

### Background
The system is configured with LC Products, each having a defined `provisionPercentage` and a linked `LcProductCharge` template.

### 1. Scenario: Configure Provision Percentage & Charge Template in LC Product
**Given** LC Product is created
**When** users do configuration for the LC Product
**Then** Users should be able to configure:
  - Provision percentage
  - Each each LC Status have a list of applicable charge types, which comes together with amount, currency, mandatory or optional flag.

### 2. Scenario: Tracking multiple types of charges
**Given** LC transaction (Application, Amendment, Issuance, Drawing)
**When** CSR input LC transaction data
**Then** the system retrieves the charges linked to the LC transaction and display different charge types such as:
  - **Issuance Commission** (`LC_CHG_ISSUANCE`)
  - **SWIFT Message Fee** (`LC_CHG_SWIFT`)
    **And** each charge record must include the amount and currency.

### 3. Scenario: Automated Assessment of Charges and Provisions (Happy Path - UC1)
**Given** an LC transaction (Application, Amendment, Issuance, Drawing) has been created 
**When** the LC transaction is in **Draft** or **Pending Processing** status
**Then** the system automatically calculates and updates the **Provision Amount** if not available yet, based on the LC Amount and the product's percentage
    **And** the system calculates and updates all applicable & mandatory **Charges** if not available yet, based on the product configuration that is linked to the current LC transaction

### 4. Scenario: Manually modify Charges and Provisions (Happy Path - UC1)
**Given** an LC transaction (Application, Amendment, Issuance, Drawing) is in **Draft** or **Pending Processing** status
**When** the authorized user opens the application for modification and add more applicable charges, or change the charge type of existing charge or modify the LC amount
**Then** the system automatically calculates and updates the **Provision Amount** based on the LC Amount and the product's percentage
    **And** the system calculates and updates **Charges** of newly added or modified charge type using charge type configuration (e.g., Issuance, Advising)
    **And** the system displays these calculated amounts in the **Charges & Provisions** section of the transaction screen
    **And** the authorized user can manually adjust specific charges amount that is not mandatory type.
    **And** the provision amount is not allowed to be manually adjusted.

### 5. Scenario: CBS Accounting Integration on Issuance Approval (Happy Path - UC2)
**Given** an LC Issuance transaction is in **Pending Approval** status
    **And** all charges and provisions have been assessed
**When** the Trade Supervisor clicks **"Approve"**
**Then** the system should trigger a request to CBS to post actual ledger entries for the calculated charges
    **And** the system should place a formal **Provision Hold** on the Applicant's account in CBS
    **And** the system should post contingent entries (Debit Contingent Asset, Credit Contingent Liability) to track off-balance sheet exposure
    **And** the LC status should transition to **Issued** only if all CBS entries are successful

### 6. Scenario: Provision Reversal on LC Expiry (Happy Path - UC3)
**Given** an LC with status **Issued** has reached its expiry date
    **And** there are no pending drawings or disputes
**When** the automated Expiry Service runs
**Then** the system should transition the LC status to **Expired**
    **And** the system should trigger a **Provision Release** request to CBS to unlock the held funds for the Applicant
    **And** the system should post reversal accounting entries in the General Ledger to close the contingent liability

### 7. Scenario: Handling Insufficient Funds for Provision (Edge Case)
**Given** an LC Issuance is being approved
**When** the system attempts to place a provision hold on the Applicant's account in CBS
    **But** the account has **insufficient balance** to cover the required provision
**Then** the system should NOT approve the transaction
    **And** the system should display a high-visibility error: "CBS Error: Insufficient funds for provision hold."
    **And** the system should notify the CSR to contact the Applicant for fund deposit

### 8. Scenario: CBS Integration Timeout During Posting (Edge Case)
**Given** a transaction approval is in progress
**When** the system calls the CBS API to post accounting entries
    **And** the connection times out or CBS returns a **503 Service Unavailable**
**Then** the system MUST **rollback** any local status changes (e.g., keep the transaction in Pending Approval)
    **And** the system should log a detailed integration error
    **And** the system should display an alert: "CBS Integration Failed. Transaction has not been updated. Please retry later."

### 9. Scenario: Automatic Invoice Generation on LC Issuance (Happy Path)
**Given** an LC has been approved and moved to **Approved** status
    **And** automated charges (e.g., **Issuance Fee**, **SWIFT Fee**) have been calculated
**When** the LC is issued via the **"issue#LetterOfCredit"** service
**Then** the system should automatically group the charges and post them to a new **Invoice**
    **And** the Invoice status should be **Finalized**
    **And** the Invoice should be linked to the Applicant as the payer and the Issuing Bank as the payee.

### 10. Scenario: Automatic General Ledger Posting (Happy Path)
**Given** an Invoice has been finalized for LC charges
**When** the system processes the invoice for accounting
**Then** the system should generate an **Accounting Transaction**
    **And** the transaction should be automatically **Posted** to the correct General Ledger
    **And** it should contain double-entry items:
        - **Debit**: Accounts Receivable
        - **Credit**: Service Revenue.

### 11. Scenario: Provision Management with CBS Integration
**Given** an active LC record
**When** a provision hold is requested
**Then** the system must communicate with the CBS (Simulator) to secure the funds
    **And** the record in **LcProvision** must reflect the **cbsHoldReference** returned by the CBS
    **And** when the provision is later released, the system must update the status to **Released** and record the **releaseDate**.

---

## Business Rules & Validation
1.  **Provision Calculation**:
    *   `Provision Amount = (LC Amount * Product.provisionPercentage) / 100`.
    *   Provision must be held in the currency of the LC unless configured otherwise.
2. **Product-Driven Charges:** Charge types (e.g., `ItemCommission`, `ItemMiscCharge`) and amounts are determined on the product's charge configuration and LC Status (i.e Issued, etc...)
3. **Charge Collection & Automated Calculation:** 
    * Charges must be triggered automatically during key lifecycle events (e.g., Creation, Amendment, Issuance)
    * Applicable Charge types are calculated based on the product's charge configuration and LC Status (i.e Issued, etc...)
        * Mandatory charge types must be collected in the LC transaction
        * Optional charge types are collected if the user manually select the charge type and can manually adjust the charge amount
    * Charge amount is calculated based on the charge type configuration
4.  **CBS Synchronization (Zero-Touch Policy)**:
    *   The Moqui system acts as the orchestrator; CBS (mocked or real) is the source of truth for balances and ledger state.
    *   Any failure in CBS communication must prevent the Moqui transaction from finalized state changes.
5. **State Integrity**:
    *   Status transitions that involve money movement (LC Issuance, Payment, Expiry) are considered **Atomic Operations**.
6. **Double-Entry Integrity:** Every provision and charge posting MUST result in a balanced accounting transaction (Debits = Credits).
7. **Currency Consistency:** Charges and provisions should typically be recorded in the same currency as the LC, or as defined by the bank's accounting preferences.
8. **Traceability:** 
    * Invoices and Accounting Transactions MUST be linked to the parent LC record for easy auditing and reconciliation.
    * All financial adjustments must be linked to the parent LC and have a unique sequence ID (`chargeSeqId`, `provisionSeqId`).
9. **Audit Trail:** Any change in provision status must be reflected in the LC's history and recorded with the CBS reference number for reconciliation.
10. **Status Prerequisite:** Accounting transactions are typically only posted when an LC reaches the **Issued** status or an Invoice is **Finalized**.
