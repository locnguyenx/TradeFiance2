---
Document ID: BDD-R8.8-R8.10-Drawing
Version: 0.1
System: Trade Finance
Module: Letter of Credit (LC)
BRD ID: BRD-002
Status: DRAFT
Last Updated: 2026-03-12
Author: [Antigravity]
---

# BDD Scenarios: Drawing Lifecycle & Discrepancy (R8.8-R8.10) - Reversed

**Requirement:** R8.8 (Document Presentation), R8.9 (Discrepancy Handling), R8.10 (Acceptance & Payment)

This document contains BDD scenarios reversed from the implemented test cases in `TradeFinanceDrawingFlowSpec.groovy`.

## Feature: Drawing Lifecycle & Discrepancy Handling

**As a** Back-Office Trade Operator
**I want to** process drawings against an LC, identify discrepancies, and resolve them
**So that** payments can be made accurately according to the LC terms

### Background
An issued LC exists in the system.

### Scenario: Registering a New Drawing (Happy Path)
**Given** an active LC exists
**When** the Trade Operator registers a new document presentation via the **"create#LcDrawing"** service
**Then** the system should create an **LcDrawing** record
**And** the initial status should be **Received** (`LcDrReceived`).

### Scenario: Automated Examination & Discrepancy Detection (Edge Case)
**Given** an **LcDrawing** has been registered with a **drawingAmount** that exceeds the available LC amount
**When** the Operator executes the **"examine#LcDrawing"** automated service
**Then** the system should identify the discrepancy
**And** the drawing status should transition to **Discrepant** (`LcDrDiscrepant`)
**And** an **LcDiscrepancy** record should be created for the drawing
**And** the system should automatically generate a SWIFT **MT734** (Advice of Refusal) document.

### Scenario: Resolving Discrepancies by Acceptance (Happy Path)
**Given** a drawing is in **Discrepant** status
**And** the Applicant provides written acceptance of the discrepancies
**When** the Operator calls the **"resolve#LcDiscrepancy"** service with `resolutionEnumId = "LC_DISRES_ACCEPTED"`
**Then** the status of the **LcDrawing** should transition to **Accepted** (`LcDrAccepted`)
**And** the drawing should proceed to the payment stage.

---

## Business Rules & Validation
1. **Automated Validation:** The `examine#LcDrawing` service must automatically compare drawing details (like amount) against the parent LC terms.
2. **Mandatory Refusal Advise:** Every discrepant drawing MUST trigger the generation of an MT734 document for transmission to the presenting bank.
3. **Resolution Requirement:** A drawing cannot move to `Accepted` or `Paid` status if there are unresolved discrepancies.
4. **Traceability:** Drawings are identified by `drawingSeqId` and discrepancies by `discrepancySeqId`, both scoped per LC.
5. **Swift Compliance:** Refusal messages (MT734) must be logged as **LcDocument** records for audit purposes.
