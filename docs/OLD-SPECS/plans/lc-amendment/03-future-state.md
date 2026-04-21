# LC Amendment Future State Definition

**Created**: 2026-03-16
**Phase**: 3 - Future State Definition
**Agent**: @plan + @reasoning
**Feature**: LC Amendment
**Source**: Business Requirements (01-business-requirements.md) and Current State Analysis (02-current-state.md)

---

## 1. Process Flow

### End-to-End Process

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         LC AMENDMENT WORKFLOW                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  [Applicant]                                                                │
│       │                                                                     │
│       ▼                                                                     │
│  [Request Amendment]                                                        │
│       │                                                                     │
│       ▼                                                                     │
│  ┌─────────────────┐                                                       │
│  │ CSR Creates     │                                                       │
│  │ Amendment Draft │                                                       │
│  └────────┬────────┘                                                       │
│           │                                                                 │
│           ▼                                                                 │
│  ┌─────────────────────────────────────┐                                  │
│  │ Supervisor Review                   │                                  │
│  │ (Approve/Reject)                    │                                  │
│  └────────┬────────────────────────────┘                                  │
│           │                                                                 │
│           ▼                                                                 │
│  ┌─────────────────────────────────────┐                                  │
│  │ IPC Final Approval                  │                                  │
│  │ (Approve/Reject)                    │                                  │
│  └────────┬────────────────────────────┘                                  │
│           │                                                                 │
│           ▼                                                                 │
│  ┌─────────────────────────────────────┐                                  │
│  │ Generate SWIFT MT707                │                                  │
│  │ Notify Advising Bank                │                                  │
│  └────────┬────────────────────────────┘                                  │
│           │                                                                 │
│           ▼                                                                 │
│  ┌─────────────────────────────────────┐                                  │
│  │ Advising Bank Notifies              │                                  │
│  │ Beneficiary                         │                                  │
│  └────────┬────────────────────────────┘                                  │
│           │                                                                 │
│           ▼                                                                 │
│  ┌─────────────────────────────────────┐                                  │
│  │ Beneficiary Response                │                                  │
│  │ (Accept/Reject)                     │                                  │
│  └────────┬────────────────────────────┘                                  │
│           │                                                                 │
│           ▼                                                                 │
│  ┌─────────────────────────────────────┐                                  │
│  │ System Updates LC Status            │                                  │
│  │ & Applies Amendment (if accepted)   │                                  │
│  └────────┬────────────────────────────┘                                  │
│           │                                                                 │
│           ▼                                                                 │
│  ┌─────────────────────────────────────┐                                  │
│  │ Update Charges & Provisions         │                                  │
│  │ Post Accounting Entries             │                                  │
│  └────────┬────────────────────────────┘                                  │
│           │                                                                 │
│           ▼                                                                 │
│  ┌─────────────────────────────────────┐                                  │
│  │ Notify All Parties                  │                                  │
│  │ (Applicant, CSR, Supervisor, IPC)   │                                  │
│  └────────────────────────────────────┘                                  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Step-by-Step Process

| Step | Actor | Action | System Response |
|------|-------|--------|-----------------|
| 1 | Applicant | Request LC amendment (specify changes) | - |
| 2 | CSR | Create amendment draft linked to issued LC | - Validate LC status is "Issued"<br>- Create LcAmendment record with shadow copy<br>- Generate Mantle Request (RqtLcAmendment)<br>- Set status to Draft |
| 3 | CSR | Submit amendment for review | - Transition to Submitted status<br>- Notify Branch Supervisor |
| 4 | Branch Supervisor | Review amendment request | - Validate impact on credit limits/collateral<br>- Approve or Reject with reason<br>- If approved: transition to Approved status<br>- If rejected: transition to Rejected status, notify CSR |
| 5 | IPC | Final approval of amendment | - Validate beneficiary acceptance window not expired<br>- Approve or Reject with reason<br>- If approved: transition to Approved status<br>- If rejected: transition to Rejected status, notify Supervisor |
| 6 | System | Generate SWIFT MT707 | - Create full MT707 message per SWIFT standards<br>- Attach to LC record<br>- Notify Advising Bank |
| 7 | Advising Bank | Notify beneficiary of amendment | - Forward MT707 to beneficiary<br>- Track notification timestamp |
| 8 | Beneficiary | Review and respond to amendment | - Accept or Reject per UCP 600 Article 10<br>- Provide reason if rejecting |
| 9 | System | Process beneficiary response | - If accepted: proceed to confirmation<br>- If rejected: preserve original LC terms, notify all parties |
| 10 | IPC | Confirm amendment application | - Apply shadow fields to LC (excluding immutable fields)<br>- Increment amendmentNumber<br>- Transition lcStatusId to "Amended"<br>- Update transaction status |
| 11 | System | Update financials | - Recalculate charges based on amendment<br>- Adjust provisions if needed<br>- Post accounting entries to CBS |
| 12 | System | Finalize and notify | - Transition to Confirmed status<br>- Notify Applicant, CSR, Supervisor, IPC<br>- Update LC effective terms display |

---

## 2. Feature Prioritization

| Priority | Feature | Description | Phase |
|----------|---------|-------------|-------|
| **P0 - MVP** | Amendment request creation | CSR can create draft amendment from issued LC | 1 |
| **P0 - MVP** | Basic approval workflow | CSR → Supervisor → IPC routing | 1 |
| **P0 - MVP** | Immutable field protection | Prevent changes to LC Number, Applicant, Currency, etc. | 1 |
| **P0 - MVP** | Amendment numbering | Auto-increment amendment number on confirmation | 1 |
| **P0 - MVP** | SWIFT MT707 generation | Generate basic MT707 message | 1 |
| **P1** | Beneficiary acceptance workflow | Accept/reject per UCP 600 Article 10 | 2 |
| **P1** | Read-only context enforcement | Prevent editing LC when viewing from amendment | 2 |
| **P1** | Concurrent amendment prevention | Detect and prevent overlapping amendments | 2 |
| **P2** | Enhanced MT707 format | Full SWIFT MT707 compliance | 3 |
| **P2** | Partial approval support | Selective field approval/rejection | 3 |
| **P2** | Amendment history view | Display original, effective, and full history | 3 |
| **P2** | Expired LC validation | Prevent amendments on expired LCs | 3 |

---

## 3. Data Requirements

| Data | Type | Source | Notes |
|------|------|--------|-------|
| LC Identifier | ID | LetterOfCredit.lcId | Links amendment to LC |
| Amendment Sequence | ID | LcAmendment.amendmentSeqId | Unique per LC |
| Amendment Number | Integer | LetterOfCredit.amendmentNumber | Displayed to users |
| Original LC Terms | Shadow Copy | LcAmendment.* | Preserved for rejected amendments |
| Amended Values | Shadow Copy | LcAmendment.* | Editable fields in amendment |
| Exchange Rate | Decimal | CBS get#ExchangeRate | For multi-currency amendments |
| CBS Hold Reference | Text | CBS hold#Funds | For provision adjustments |
| Amendment Status | Status ID | LcAmendment.amendmentStatusId | Draft/Submitted/Approved/Rejected |
| Confirmation Status | Status ID | LcAmendment.confirmationStatusId | Pending/Confirmed/Rejected (beneficiary) |
| Transaction Status | Status ID | LcTransaction.transactionStatusId | Draft/Submitted/Approved/etc. |
| LC Status | Status ID | LetterOfCredit.lcStatusId | Issued/Amended/etc. |
| Charges | Currency-Amount | LC product charge template | Calculated per amendment |
| Provision Amount | Currency-Amount | LC product provision percentage | Recalculated if amount changed |
| MT707 Message | Text | SwiftServices.generate#SwiftMt707 | Attached to LC record |
| Timestamp | Date-Time | System generated | Audit trail for all actions |
| User ID | ID | Security.getUserId() | Track who performed each action |
| Reason/Text | Text-Medium | User input | For approvals/rejections |

---

## 4. Integration Points

| System | Integration | Method | Details |
|--------|-------------|--------|---------|
| CBS (Core Banking) | Exchange rate lookup | get#ExchangeRate | For currency conversion in amendments |
| CBS (Core Banking) | Fund hold/release | hold#Funds / release#Funds | For provision adjustments |
| CBS (Core Banking) | Accounting entries | GL posting | Charge and provision adjustments |
| SWIFT Network | Message transmission | MT707 | Amendment notification to Advising Bank |
| Mantle Workflow | Request processing | Workflow engine | CSR → Supervisor → IPC routing |
| Notification System | Email/System alerts | Notification service | Status updates to stakeholders |
| Audit Logging | History tracking | LcEntityHistory | Complete amendment audit trail |
| Reporting System | Analytics | Reporting framework | Amendment metrics and trends |

---

## 5. Risk Assessment

| Risk | Severity | Mitigation |
|------|----------|------------|
| **Beneficiary rejection after system processing** | High | Implement two-phase confirmation: system holds changes until beneficiary accepts |
| **SWIFT MT707 format non-compliance** | Medium | Use validated SWIFT library or strict template matching |
| **Concurrent amendment conflicts** | High | Implement amendment locking mechanism (optimistic/pessimistic) |
| **CBS integration failure during provision adjustment** | High | Wrap entire confirmation in transaction with rollback on failure |
| **Incorrect immutable field enforcement** | Medium | Centralized validation service for immutable fields |
| **Beneficiary acceptance window expiration** | Low | Track and enforce UCP 600 Article 10 time limits |
| **Charge calculation errors** | Medium | Unit tests for charge calculation logic |
| **Audit trail gaps** | Low | Comprehensive history tracking for all amendment actions |
| **Notification delivery failures** | Low | Retry mechanism and delivery confirmation tracking |
| **Role-based authorization bypass** | Medium | Centralized security checks with unit tests |

---

## 6. Open Issues for Technical Design

1. **Locking Mechanism**: What type of locking (optimistic vs pessimistic) should be used for amendment concurrency control?
2. **Beneficiary Notification**: Should beneficiary notification be system-generated email or rely on external SWIFT MT707?
3. **Time Windows**: What is the exact beneficiary acceptance period per UCP 600 Article 10 implementation?
4. **Partial Approvals**: Should the system support approving some amendment fields while rejecting others?
5. **Charge Variability**: How are amendment charges determined (fixed, percentage, product-specific)?
6. **History Display**: What level of detail should be shown in the amendment history view?
7. **Integration Testing**: How will CBS integration be tested for amendment scenarios?
8. **Performance Impact**: What is the expected performance impact of shadow copy pattern on LC query performance?

---

**Last Updated**: 2026-03-16
