---
Document ID: BDD-R8.8
Version: 0.1
System: Trade Finance
Module: Letter of Credit (LC)
BRD ID: BRD-002
Status: DRAFT
Last Updated: 2026-03-12
Author: [Antigravity]
---

# BDD Scenarios: Document Presentation (Drawings) (R8.8) - Reversed

**Requirement:** R8.8 (Register Document Presentation, Execute Document Examination)

This document contains BDD scenarios reversed from the implemented test cases in `TradeFinanceServicesSpec.groovy`, focusing on the registration and validation of drawing documents.

## Feature: Document Presentation (Drawings)

**As a** Back-Office Trade Operator
**I want to** register and verify documents presented under an LC
**So that** I can process drawing payments or identify discrepancies

### Background
An active, issued LC exists in the system.

### Scenario: Verify mandatory documents for a drawing (Happy Path)
**Given** a document presentation has been registered as an **LcDrawing**
**When** the system checks the linked **LcDrawingDocument** records
**Then** the system should verify the presence of core transport and commercial documents:
  - **Bill of Lading** (LC_DOC_BILL_LADING)
  - **Commercial Invoice** (LC_DOC_COMM_INVOICE)
  - **Packing List** (LC_DOC_PACKING_LIST)
  - **Certificate of Origin** (LC_DOC_CERT_ORIGIN)
  - **Insurance Certificate** (LC_DOC_INSURANCE)
**And** each document should be correctly sequenced via **documentSeqId**.

---

## Business Rules & Validation
1. **Document Completeness:** Every drawing under an LC must include the documents specified in the original LC terms.
2. **Standardized Types:** Documents must use the standard type enums (e.g., `LC_DOC_BILL_LADING`).
3. **Traceability:** Documents must be linked to both the **LetterOfCredit** (via `lcId`) and the specific **LcDrawing** (via `drawingSeqId`).
