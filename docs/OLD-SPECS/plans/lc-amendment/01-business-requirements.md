# LC Amendment Business Requirements Analysis

**Created**: 2026-03-16
**Phase**: 1 - Business Understanding
**Agent**: @reasoning
**Feature**: LC Amendment
**Source**: BRD Section 8.5 (Document ID: BRD-002, Version: 1.0)

---

## 1. Problem Statement

The Trade Finance System needs to support LC Amendment functionality to allow applicants to modify issued Letters of Credit after initial issuance. This addresses the business need to accommodate changes in trade terms (e.g., extended expiry dates, increased amounts, modified shipment terms) while maintaining strict compliance with international banking standards (UCP 600, SWIFT MT707).

**Core Business Problem**: Issued LCs are immutable documents, but business circumstances change. The system must provide a controlled, auditable process for amendment requests with proper approval workflows and beneficiary notification, while preserving the original LC terms if amendments are rejected.

---

## 2. Stakeholders

| Role | Description | Permitted Actions |
|------|-------------|-------------------|
| **Applicant (Importer)** | Corporate customer who originally requested the LC | Submit amendment requests, view amendment status, receive notifications |
| **CSR (Customer Service Representative)** | Bank front-office staff | Create amendment requests, submit for approval, work with customer on charges/provisions |
| **Branch Supervisor** | Bank front-office manager | Review amendment requests, approve/reject with authorization |
| **IPC (International Processing Center)** | Bank back-office operations | Final approval authority, process amendment execution, generate SWIFT MT707 |
| **Beneficiary (Exporter)** | Party receiving payment under the LC | Accept or reject amendment per UCP 600 Article 10 |
| **Advising Bank** | Bank facilitating communication with beneficiary | Receive SWIFT MT707, advise beneficiary of amendment |

---

## 3. Business Rules

| # | Rule | Constraint |
|---|------|------------|
| 1 | **Amendment Request Creation** | CSR must create amendment request linked to existing issued LC; LC status remains unchanged until approval |
| 2 | **Approval Workflow** | All amendment requests must follow CSR → Supervisor → IPC approval chain |
| 3 | **Immutable Fields** | LC Number, Applicant, Currency, Issuing Bank, Advising Bank cannot be changed via amendment |
| 4 | **Amendment Numbering** | Each approved amendment increments the amendment number (e.g., Amendment 1, Amendment 2) |
| 5 | **Status Transition** | Upon IPC approval, LC status transitions from "Issued" to "Amended" |
| 6 | **SWIFT MT707 Generation** | System must generate SWIFT MT707 (Amendment to Documentary Credit) upon approval |
| 7 | **Beneficiary Acceptance** | Per UCP 600 Article 10, beneficiary may accept or reject amendment |
| 8 | **Original Terms Preservation** | If amendment is rejected, original LC terms remain in effect |
| 9 | **Effective Terms Display** | Users can view: Original LC terms, Latest effective terms (original + last confirmed amendment), and full amendment history |
| 10 | **Read-Only Context** | When viewing LC from amendment context, display must be read-only to prevent data corruption |
| 11 | **Charge Assessment** | Amendment may incur fees; charges calculated based on product template |
| 12 | **Provision Impact** | Amendment approval may impact credit limits, collateral, and provisions |

---

## 4. Success Criteria

| Criteria | Validation Method |
|----------|-------------------|
| Amendment requests can be created and linked to existing issued LCs | System creates LcAmendment record with proper LC linkage |
| Approval workflow routes correctly through CSR → Supervisor → IPC | Workflow engine processes routing and authorization |
| Upon IPC approval, LC entity updates correctly with amendment number and status | LetterOfCredit entity shows incremented amendment number and "Amended" status |
| SWIFT MT707 message is generated with correct format and content | MT707 message stored as attachment and available for transmission |
| Beneficiary can accept or reject amendment through proper channels | System supports acceptance/rejection status tracking per UCP 600 |
| Original LC terms are preserved if amendment is rejected | System maintains original terms and only applies accepted amendments |
| Users can view amendment history and effective terms | UI provides clear display of original, effective, and amendment history |
| Read-only mode prevents data corruption when viewing LC from amendment context | Context-aware display enforces read-only mode |
| Charges are calculated and collected for amendment processing | System calculates fees and manages collection workflow |

---

## 5. Edge Cases

| Case | Handling |
|------|----------|
| **Multiple concurrent amendment requests** | System must handle sequential processing; prevent overlapping amendments that conflict |
| **Amendment request for expired LC** | Should be rejected or handled as special case per business policy |
| **Beneficiary rejection of amendment** | System must preserve original LC terms and notify all parties |
| **Partial approval of amendment fields** | System must process only approved changes; reject or return conflicting changes |
| **Amendment after LC drawing commenced** | May require special handling; check UCP 600 compliance |
| **System failure during MT707 generation** | Must rollback status changes and alert operator |
| **CBS integration failure during provision adjustment** | Must rollback entire amendment transaction |
| **Duplicate amendment requests** | System must detect and prevent duplicates |

---

## 6. Questions for Next Phase

1. What specific fields can be amended vs. immutable fields (beyond the listed exceptions)?
2. What is the exact SWIFT MT707 message structure and required fields?
3. How long is the beneficiary acceptance/rejection window per UCP 600 Article 10?
4. What charge amounts apply to amendment requests?
5. How are provisions recalculated upon amendment approval?
6. What happens to pending drawings when an amendment is approved?
7. Are there restrictions on amendment frequency or timing?
8. What is the escalation path if IPC rejects an amendment request?
9. How are amendments integrated with CBS for accounting entries?
10. What audit trail requirements apply to amendment transactions?

---

**Last Updated**: 2026-03-16
