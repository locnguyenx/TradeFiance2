---
Document ID: BDD-R8.9
Version: 0.1
System: Trade Finance
Module: Letter of Credit (LC)
BRD ID: BRD-002
Status: DRAFT
Last Updated: 2026-03-12
Author: [Antigravity]
---

# BDD Scenarios: LC Discrepancy Handling (R8.9) - Reversed

**Requirement:** R8.9 (Record Discrepancies, Resolve Discrepancies)

This document contains BDD scenarios reversed from the implemented test cases and data validation in `TradeFinanceServicesSpec.groovy`.

## Feature: LC Discrepancy Handling

**As a** Trade Operator
**I want to** record and resolve discrepancies found during document examination
**So that** I can proceed with the drawing if the applicant or bank accepts the issues

### Background
A document presentation (`LcDrawing`) has been registered, and discrepancies have been identified.

### Scenario: Recording and Waiving a Discrepancy
**Given** an `LcDrawing` has a recorded discrepancy (e.g., `DEMO_LC_10`)
**When** the Trade Operator or Applicant decides to waive the discrepancy
**Then** the record in **LcDiscrepancy** should have its **resolutionEnumId** set to **LC_DISRES_WAIVED**
**And** the system should track the specific discrepancy type (e.g., `LC_DISC_AMOUNT`) and sequence.

---

## Business Rules & Validation
1. **Resolution Tracking:** Every identified discrepancy must have a resolution (e.g., Waived, Accepted by Applicant, Rejected).
2. **Standardized Types:** Discrepancies must be categorized using standard enums (e.g., `LC_DISC_AMOUNT`, `LC_DISC_LATE_PRES`).
3. **Traceability:** Discrepancies are linked to a specific **LcDrawing** and tracked by **discrepancySeqId**.
