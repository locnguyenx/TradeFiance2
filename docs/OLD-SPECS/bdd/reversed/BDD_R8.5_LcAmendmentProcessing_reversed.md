---
Document ID: BDD-R8.5-Amendment
Version: 0.1
System: Trade Finance
Module: Letter of Credit (LC)
BRD ID: BRD-002
Status: DRAFT
Last Updated: 2026-03-12
Author: [Antigravity]
---

# BDD Scenarios: LC Amendment Processing (R8.5) - Reversed

**Requirement:** R8.5 (Create Amendment Request, Process Amendment Approval, Finalize Amendment)

This document contains BDD scenarios reversed from the implemented test cases in `TradeFinanceAmendmentSpec.groovy`.

## Feature: LC Amendment Processing

**As a** Trade Finance System
**I want to** manage the lifecycle of an LC amendment, from shadow field creation to final application
**So that** the master LC record accurately reflects the most recent agreed-upon terms

### Background
An LC has been issued and is in an active status (e.g., `LcLfIssued`).

### Scenario: Shadow Field Cloning on Amendment Creation (Happy Path)
**Given** an issued LC exists with specific terms (e.g., Amount = 10,000 USD)
**When** a new Amendment request is created via the **"create#LcAmendment"** service
**Then** the system should create an **LcAmendment** record
**And** it should clone the current values from the master LC into the amendment "shadow" fields (e.g., `amount`)
**And** the initial **amendmentStatusId** should be **Draft** (`LcTxDraft`)
**And** the **confirmationStatusId** should be **Pending** (`LcAmndPending`).

### Scenario: Applying Amendment Changes to Master LC (Happy Path)
**Given** an **LcAmendment** record exists and has been **Approved** (`LcTxApproved`)
**When** the authorized user confirms the amendment via the **"confirm#LcAmendment"** service
**Then** the system should update the master **LetterOfCredit** record with the values from the amendment shadow fields
**And** the system should increment the **amendmentNumber** on the master LC
**And** the master **lcStatusId** should transition to **Amended** (`LcLfAmended`)
**And** the **LcAmendment** record status should be updated to **Closed** (`LcTxClosed`) and **Confirmed** (`LcAmndConfirmed`).

### Scenario: Restricted Field Validation (Business Rule)
**Given** an LC is being amended
**When** a user attempts to create an amendment for a restricted field such as **LC Number** (`lcNumber`)
**Then** the system should block the request
**And** return an error message indicating that the field cannot be amended.

---

## Business Rules & Validation
1. **Shadow Mechanism:** Changes are always made to an amendment record first. The master LC remains unchanged until formal confirmation.
2. **Restricted Fields:** Fields that define the identity of the credit (e.g., `lcNumber`, `applicantPartyId`, `issuingBankPartyId`) are NOT allowed to be amended.
3. **Sequential numbering:** The `amendmentNumber` on the master LC must increment by exactly 1 for each confirmed amendment.
4. **Synchronization:** Confirmed amendments must close the transaction status (`LcTxClosed`) and mark the confirmation status as `LcAmndConfirmed`.
5. **Traceability:** Every amendment is uniquely identified by `amendmentSeqId` and linked to the parent `lcId`.
