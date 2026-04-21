---
Document ID: BDD-R8.5
Version: 0.1
System: Trade Finance
Module: Letter of Credit (LC)
BRD ID: BRD-002
Status: DRAFT
Last Updated: 2026-03-12
Author: [Antigravity]
---

# BDD Scenarios: LC Amendment (R8.5) - Reversed

**Requirement:** R8.5 (Create Amendment Request, Process Amendment Approval, Finalize Amendment)

This document contains BDD scenarios reversed from the implemented test cases and data validation in `TradeFinanceServicesSpec.groovy`.

## Feature: LC Amendment

**As a** Trade Finance User
**I want to** track changes and amendments made to an issued LC
**So that** the system always reflects the latest effective terms of the credit

### Background
An LC has been issued and is in an active status (e.g., `LcLfIssued`).

### Scenario: Tracking an approved LC Amendment
**Given** an LC has undergone an amendment process (e.g., `DEMO_LC_01`)
**When** the amendment is approved by the Trade Supervisor
**Then** an **LcAmendment** record should exist with:
  - **amendmentNumber** correctly incremented (e.g., 1)
  - **amendmentStatusId** set to **LcTxApproved**
**And** the record should be uniquely identified by an **amendmentSeqId**.

---

## Business Rules & Validation
1. **Sequential Numbering:** Amendments must be numbered sequentially (`amendmentNumber`).
2. **Status Tracking:** Each amendment follows its own transaction lifecycle (Draft -> Submitted -> Approved/Rejected).
3. **Traceability:** Amendments MUST be linked to the parent **LetterOfCredit** via `lcId`.
4. **Audit Trail:** History of amendments must be visible and traceable back to the master LC record.
