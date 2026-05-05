# Consolidated Business Requirements Document (BRD)
**ABOUTME:**
Consolidated requirements for the Import Letter of Credit (LC) Module.
This document merges baseline requirements with SWIFT specs, transaction tracking, and TradeParty refactoring.
SWIFT validation and generation rules are integrated into the business processes where data is captured and messages are triggered.

**Project Name:** Digital Trade Finance Platform
**Module:** Import Letter of Credit (LC)
**Document Version:** 1.1 (Restructured — SWIFT rules integrated into business processes)
**Date:** May 05, 2026

**Superseded BRDs:**
*   [2026-04-21-import-lc-brd.md](file:///Users/me/myprojects/moqui-trade/docs/superpowers/specs/2026-04-21-import-lc-brd.md)
*   [2026-04-25-swift-validation-brd.md](file:///Users/me/myprojects/moqui-trade/docs/superpowers/specs/2026-04-25-swift-validation-brd.md)
*   [2026-04-25-swift-generation-brd.md](file:///Users/me/myprojects/moqui-trade/docs/superpowers/specs/2026-04-25-swift-generation-brd.md)
*   [2026-04-26-swift-gaps-consolidation-brd.md](file:///Users/me/myprojects/moqui-trade/docs/superpowers/specs/2026-04-26-swift-gaps-consolidation-brd.md)

**Related (requirements incorporated but not superseded — see common-consolidated-brd.md):**
*   [2026-04-28-trade-transaction-tracking-brd.md](file:///Users/me/myprojects/moqui-trade/docs/superpowers/specs/2026-04-28-trade-transaction-tracking-brd.md)
*   [2026-04-30-tradeparty-refactor-brd.md](file:///Users/me/myprojects/moqui-trade/docs/superpowers/specs/2026-04-30-tradeparty-refactor-brd.md)
*   [2026-05-05-common-consolidated-brd.md](file:///Users/me/myprojects/moqui-trade/docs/superpowers/specs/2026-05-05-common-consolidated-brd.md)

---

## 1. Feature 1: LC Lifecycle States & State Machine

### 1.1 Final Requirements in Detail

#### A. LC Business States (Domain Level)
The Import LC transitions through the following business states. State transitions are governed by the Maker/Checker authorization matrix defined in the Common Module.

| State | Description |
| :--- | :--- |
| **Draft** | Initial data entry phase. Application is being prepared by operations or saved by customer via portal. |
| **Pending Approval** | Submitted by Maker, awaiting Checker authorization per financial tier limit. |
| **Issued** | Formally approved, limits earmarked, MT 700 dispatched to Advising Bank. |
| **Amended** | A formal change to an Issued LC has been processed and approved. |
| **Documents Received** | Physical or electronic documents received, pending examination. |
| **Discrepant** | Documents found non-compliant. Awaiting Applicant waiver or formal refusal. |
| **Accepted / Clean** | Documents compliant (or discrepancies waived). Bank's undertaking to pay is unconditional. |
| **Settled** | Funds debited from Applicant and remitted to Presenting Bank. |
| **Closed / Cancelled** | LC expired unutilized, or fully drawn and settled, releasing facility earmarks. |

#### B. State Transition Matrix

| Current State | Permitted Next States | Trigger Event |
| :--- | :--- | :--- |
| **Draft** | Pending Approval, Cancelled | Maker submits or discards. |
| **Pending Approval** | Issued, Draft (Return), Cancelled | Checker authorizes, rejects, or declines. |
| **Issued** | Pending Approval (Amendment), Documents Received, Closed | Amendment initiated, docs arrive, or LC expires. |
| **Documents Received** | Accepted/Clean, Discrepant | Examination completed. |
| **Discrepant** | Accepted/Clean, Closed (Refused) | Applicant waives or bank refuses. |
| **Accepted/Clean** | Settled | Payment initiated or maturity reached. |
| **Settled** | Issued (Partial Draw), Closed (Fully Drawn) | Payment finalized. |
| **Closed / Cancelled** | *None (Terminal)* | Limits released. |

#### C. Detailed State Criteria

**Draft:**
- Entry: New application initiated or "Returned to Maker" from Checker.
- Exit: All mandatory fields populated (Applicant, Beneficiary, Amount, Currency, Expiry Date, Tenor, Required Documents) and pass basic validation.
- Transitions: Forward → Pending Approval; Terminal → Cancelled.

**Pending Approval:**
- Entry: Maker submits Draft or Amendment.
- Exit: Checker reviews against facility limits and AML/Sanctions results.
- Transitions: Forward → Issued (generates MT 700); Backward → Draft; Terminal → Cancelled.

**Issued:**
- Entry: Checker approval completed. MT 700 dispatched.
- Exit: External party acts (docs presented) or internal party acts (amendment requested).
- Transitions: Forward → Documents Received; Loop → Pending Approval (Amendment); Terminal → Closed (expiry + mail days).

**Documents Received:**
- Entry: Operations logs presentation with amount and document counts.
- Exit: Examination completed within 5 banking days (UCP 600).
- Transitions: Forward → Accepted/Clean; Alternative → Discrepant.

**Discrepant:**
- Entry: Checker authorizes examination results with discrepancy codes. MT 734 optionally sent.
- Exit: Applicant provides formal decision (waive/refuse) and bank agrees.
- Transitions: Forward → Accepted/Clean (waiver accepted); Terminal → Closed (refused).

**Accepted / Clean:**
- Entry: Documents deemed clean or discrepancies fully waived.
- Exit: Payment process triggered.
- Transitions: Forward → Settled (Sight: immediate; Usance: on maturity date).

**Settled:**
- Entry: Applicant debited, SWIFT remittance generated, Nostro/Vostro entries confirmed.
- Exit: System determines remaining balance.
- Transitions: Loop → Issued (partial draw); Terminal → Closed (fully drawn).

**Closed / Cancelled:**
- Entry: Fully drawn, expired unutilized, or formally cancelled by all parties.
- Exit: None. Terminal state.

#### D. Transaction State vs LC Business State (Dual-Status)
Per trade-transaction-tracking-brd.md, the system maintains TWO status concepts:
- **Transaction State** (System/Workflow): Draft → Pending Approval → Executed → Cancelled. Dictates who has current action.
- **LC Business State** (Domain/Product): Issued, Documents Received, Discrepant, Settled, Closed. Dictates legal/operational reality.

Example: An Amendment can be in Transaction State "Pending Approval" while the underlying LC Business State remains "Issued". The UI must display both statuses simultaneously (e.g., "Instrument: Issued | Action: Pending Amendment Approval").

#### E. User Stories

### US-LC-01: LC Lifecycle State Management
**As a** Trade Operations Maker,
**I want** the system to enforce strict LC state transitions,
**So that** the LC lifecycle follows UCP 600 compliance and bank policies.

### US-LC-02: Dual-Status Visibility
**As a** Trade Operator,
**I want** to see both Transaction State and LC Business State on the instrument header,
**So that** I understand both the workflow progress and legal status.

### US-LC-03: UCP 600 SLA Countdown
**As a** Trade Operations Checker,
**I want** the system to track the 5-banking-day examination countdown,
**So that** we comply with UCP 600 document examination deadlines.

**Acceptance Criteria:**
- Warning at Day 3, critical block at Day 5 (per REQ-COM-SLA-02 from common module).
- Countdown uses the Global Head-Office Calendar (weekends/holidays skipped).

#### F. Grounding Info
*   **LC States & Matrix:** From **REQ-IMP-02**, **REQ-IMP-DTL-00**, **REQ-IMP-STATE-01** in baseline spec.
*   **Dual-Status:** From **REQ-TXN-01**, **REQ-UTN-05** in trade-transaction-tracking-brd.md.
*   **SLA Countdown:** From **REQ-IMP-PRC-03** (baseline) and **REQ-COM-SLA-02** (common module).

---

## 2. Feature 2: Core Business Processes

### 2.1 Process — LC Issuance

#### A. Related States
*   **Transaction State:** Draft → Pending Approval → Executed
*   **LC Business State:** Draft → Issued

#### B. Business Process Workflow
1. **Maker Entry:** Operations enters LC application data (via branch, email, or corporate portal).
2. **System Validation:** Cross-checks KYC, Sanctions, Facility Limits, and SWIFT compliance (Layer 1).
3. **Submission:** Maker submits. Transaction State → Pending Approval.
4. **Checker Review:** Reviews validations, compares documents against system data.
5. **Authorization:** Checker approves.
6. **Execution:** System deducts limits, generates MT 700, issues Customer Advice. LC Business State → Issued.

#### C. Initiation & Post Conditions
*   **Entry:** Valid customer mandate received. Applicant has active Customer Profile and KYC = Clear.
*   **Exit:** Bank irrevocably bound to LC terms. Liability booked. MT 700 dispatched.

#### D. Inputs Capture (Data Dictionary with SWIFT Tag Mapping)

##### General (Party assignments via TradeInstrumentParty junction — see Feature 6)
| Field Name | Req/Opt | Data Type | SWIFT Tag | Validation Rules |
| :--- | :--- | :--- | :--- | :--- |
| `applicantPartyId` | Req | id | Tag 50 | Via `TradeInstrumentParty` junction (role: TP_APPLICANT). KYC must be Active. Party name: max 4×35, X charset. |
| `beneficiaryPartyId` | Req | id | Tag 59 | Via `TradeInstrumentParty` junction (role: TP_BENEFICIARY). Party name: max 4×35, X charset. Account number optional (prepended with `/`). |
| `advisingBankPartyId` | Opt | id | Header Block 2 | Via `TradeInstrumentParty` junction (role: TP_ADVISING_BANK). Must have valid BIC (8/11 chars). Must have `hasActiveRMA = Y`. |

##### Dates
| Field Name | Req/Opt | Data Type | SWIFT Tag | Validation Rules |
| :--- | :--- | :--- | :--- | :--- |
| `issueDate` | Req | Date | Tag 31C | Cannot be in the past. Defaults to current system business date. Formatted YYMMDD for SWIFT. |
| `expiryDate` | Req | Date | Tag 31D | Must be ≥ Issue Date. Formatted YYMMDD for SWIFT. |
| `latestShipmentDate` | Opt | Date | Tag 44C | Must be ≤ Expiry Date. Formatted YYMMDD. **Mutually exclusive with `shipmentPeriodText`**. |
| `expiryPlace` | Req | text-medium | Tag 31D | Max 29 chars, X charset. Concatenated with expiryDate in SWIFT output. |

##### Financial
| Field Name | Req/Opt | Data Type | SWIFT Tag | Validation Rules |
| :--- | :--- | :--- | :--- | :--- |
| `currencyUomId` | Req | id | Tag 32B | 3-letter ISO Currency Code. |
| `amount` | Req | number-decimal | Tag 32B | Must be > 0. Max 15 digits. Comma decimal separator for SWIFT. |
| `tolerancePositive` | Opt | number-integer | Tag 39A | Max 100. **Mutually exclusive with `maxCreditAmountFlag`**. |
| `toleranceNegative` | Opt | number-integer | Tag 39A | Max 100. **Mutually exclusive with `maxCreditAmountFlag`**. |
| `maxCreditAmountFlag` | Opt | text-indicator | Tag 39B | Y/N. **Mutually exclusive with tolerance fields (39A)**. |
| `additionalAmountsText` | Opt | text-long | Tag 39C | Max 4×35, X charset. |

##### Terms
| Field Name | Req/Opt | Data Type | SWIFT Tag | Validation Rules |
| :--- | :--- | :--- | :--- | :--- |
| `lcTypeEnumId` | Req | id | Tag 40A | IRREVOCABLE, IRREVOCABLE_TRANSFERABLE. Mandatory for MT700. |
| `tenorTypeEnumId` | Req | id | Tag 41a | SIGHT, USANCE, ACCEPTANCE, NEGOTIATION, DEF_PAYMENT, MIXED. |
| `usanceDays` | Cond | number-integer | Tag 42C | **Required if tenor ≠ SIGHT**. |
| `usanceBaseDate` | Cond | text-medium | Tag 42C | **Required if tenor ≠ SIGHT**. Max 35 chars, X charset. |
| `availableWithEnumId` | Req | id | Tag 41a | AVAIL_ANY_BANK or AVAIL_SPECIFIC_BANK. Explicit user choice. |
| `availableWithBic` | Cond | text-short | Tag 41A | Required if AVAIL_SPECIFIC_BANK. Valid 8/11 char BIC. |
| `availableByEnumId` | Req | id | Tag 41a | BY_PAYMENT, BY_ACCEPTANCE, BY_NEGOTIATION, BY_DEF_PAYMENT. Combined with availableWithBic for Tag 41a assembly. |
| `mixedPaymentDetails` | Cond | text-long | Tag 42M | Max 4×35, X charset. **Required if tenor = MIXED**. |
| `deferredPaymentDetails` | Cond | text-long | Tag 42P | Max 4×35, X charset. **Required if tenor = DEF_PAYMENT/NEGOTIATION**. |
| `draweeBic` | Opt | text-short | Tag 42A | Valid 8/11 char BIC. |

##### Shipping
| Field Name | Req/Opt | Data Type | SWIFT Tag | Validation Rules |
| :--- | :--- | :--- | :--- | :--- |
| `partialShipmentEnumId` | Req | id | Tag 43P | ALLOWED, NOT_ALLOWED, CONDITIONAL. |
| `transhipmentEnumId` | Req | id | Tag 43T | ALLOWED, NOT_ALLOWED, CONDITIONAL. |
| `portOfLoading` | Opt | text-medium | Tag 44E | Max 65 chars, X charset. |
| `portOfDischarge` | Opt | text-medium | Tag 44F | Max 65 chars, X charset. |
| `receiptPlace` | Opt | text-medium | Tag 44A | Max 65 chars, X charset. |
| `finalDeliveryPlace` | Opt | text-medium | Tag 44B | Max 65 chars, X charset. |
| `shipmentPeriodText` | Opt | text-medium | Tag 44D | Max 65 chars, X charset. **Mutually exclusive with `latestShipmentDate`**. |

##### Narratives
| Field Name | Req/Opt | Data Type | SWIFT Tag | Validation Rules |
| :--- | :--- | :--- | :--- | :--- |
| `goodsDescription` | Req | text-very-long | Tag 45A | Max 100×65, X charset. Auto-wrap at 65 chars/line for SWIFT. Mandatory for MT700. |
| `documentsRequired` | Req | text-very-long | Tag 46A | Max 100×65, X charset. Auto-wrap at 65 chars/line. Mandatory for MT700. |
| `additionalConditions` | Opt | text-very-long | Tag 47A | Max 100×65, X charset. Auto-wrap at 65 chars/line. |
| `chargeAllocation` | Req | Enum | Tag 71D | ALL_APPLICANT, ALL_BENEFICIARY, SHARED. |
| `chargeAllocationText` | Opt | text-long | Tag 71D | Max 6×35, X charset. Detailed charges text. |
| `bankToBankInstructions` | Opt | text-very-long | Tag 78 | Max 12×65, X charset. |
| `presentationPeriodDays` | Opt | number-integer | Tag 48 | Positive integer. Days after shipment for doc presentation. |
| `confirmationEnumId` | Req | id | Tag 49 | CONFIRM, MAY_ADD, WITHOUT. Mandatory for MT700. Default WITHOUT. |

##### Administrative
| Field Name | Req/Opt | Data Type | SWIFT Tag | Validation Rules |
| :--- | :--- | :--- | :--- | :--- |
| `preAdviceRef` | Opt | text-short | Tag 23 | Max 16 chars, X charset. No leading/trailing `/`, no `//`. |
| `senderToReceiverInfo` | Opt | text-long | Tag 72Z | Max 6×35, X charset. |

#### E. SWIFT Validation Rules Specific to Issuance

| Rule ID | Rule | SWIFT Tags Affected |
| :--- | :--- | :--- |
| ISS-SWV-01 | `lcTypeEnumId` must be a valid Form of DC code (IRREVOCABLE, IRREVOCABLE_TRANSFERABLE) | Tag 40A (Mandatory) |
| ISS-SWV-02 | `confirmationEnumId` must be specified (default WITHOUT) | Tag 49 (Mandatory) |
| ISS-SWV-03 | Tolerance (39A) and Max Credit (39B) mutually exclusive | Tags 39A, 39B |
| ISS-SWV-04 | Shipment Date (44C) and Shipment Period Text (44D) mutually exclusive | Tags 44C, 44D |
| ISS-SWV-05 | If tenor ≠ SIGHT: `usanceDays` and `usanceBaseDate` required | Tag 42C |
| ISS-SWV-06 | If tenor = MIXED: `mixedPaymentDetails` required | Tag 42M |
| ISS-SWV-07 | If tenor = DEF_PAYMENT/NEGOTIATION: `deferredPaymentDetails` required | Tag 42P |
| ISS-SWV-08 | `availableWithEnumId` must be explicitly chosen (ANY BANK or SPECIFIC BANK) | Tag 41a |
| ISS-SWV-09 | Party names (Applicant, Beneficiary) validated: X charset, max 4×35 lines | Tags 50, 59 |
| ISS-SWV-10 | Advising Bank must have `hasActiveRMA = Y` | Header Block 2 (Receiver) |
| ISS-SWV-11 | Reimbursing Bank (if assigned) must have `nostroAccountRef` populated | Tag 53a |
| ISS-SWV-12 | Amount formatted with comma decimal separator, max 15 digits, positive | Tag 32B |
| ISS-SWV-13 | All dates formatted as YYMMDD | Tags 31C, 31D, 44C |
| ISS-SWV-14 | All reference fields: no leading/trailing `/`, no `//` | Tags 20, 23 |
| ISS-SWV-15 | Narrative fields auto-wrapped at 65 chars/line for SWIFT output | Tags 45A, 46A, 47A, 78 |
| ISS-SWV-16 | BIC fields: exactly 8 or 11 alphanumeric characters | Tags 41A, 42A, 53a, 57a |
| ISS-SWV-17 | `transactionRef`: max 16 chars, X charset, slash rules | Tag 20 |

##### Source Traceability

| Rule ID | Source BRD | Source Requirement ID | Notes |
| :--- | :--- | :--- | :--- |
| ISS-SWV-01 | swift-gaps-consolidation-brd.md | FR-ENT-21 | Mandatory MT700 field added in gaps consolidation |
| ISS-SWV-02 | swift-gaps-consolidation-brd.md | FR-ENT-26, FR-SGC-04 | Mandatory Tag 49, default WITHOUT |
| ISS-SWV-03 | swift-validation-brd.md | FR-SWV-07 | Mutual exclusion: 39A/39B |
| ISS-SWV-04 | swift-validation-brd.md | FR-SWV-07 | Mutual exclusion: 44C/44D |
| ISS-SWV-05 | swift-validation-brd.md | FR-SWV-08 | Conditional: tenor ≠ SIGHT requires usance fields |
| ISS-SWV-06 | swift-validation-brd.md | FR-SWV-08 | Conditional: tenor = MIXED requires mixedPaymentDetails |
| ISS-SWV-07 | swift-validation-brd.md | FR-SWV-08 | Conditional: tenor = DEF_PAYMENT/NEGOTIATION requires deferredPaymentDetails |
| ISS-SWV-08 | tradeparty-refactor-brd.md | FR-TP-09 | Explicit user choice: AVAIL_ANY_BANK or AVAIL_SPECIFIC_BANK |
| ISS-SWV-09 | swift-validation-brd.md | FR-SWV-01, FR-SWV-06 | X charset + 4×35 line format for party blocks |
| ISS-SWV-10 | tradeparty-refactor-brd.md | FR-TP-12 | Bank eligibility: Advising Bank requires active RMA |
| ISS-SWV-11 | tradeparty-refactor-brd.md | FR-TP-12 | Bank eligibility: Reimbursing Bank requires Nostro reference |
| ISS-SWV-12 | swift-validation-brd.md | FR-SWV-03 | Amount format: comma decimal, max 15 digits |
| ISS-SWV-13 | swift-validation-brd.md | FR-SWV-04 | Date format: YYMMDD |
| ISS-SWV-14 | swift-validation-brd.md | FR-SWV-02 | Reference slash rules: no leading/trailing `/`, no `//` |
| ISS-SWV-15 | swift-validation-brd.md | FR-SWV-06 | Line format: 100×65 narrative auto-wrap at 65 chars |
| ISS-SWV-16 | swift-validation-brd.md | FR-SWV-05 | BIC format: 8 or 11 alphanumeric characters |
| ISS-SWV-17 | swift-validation-brd.md | FR-SWV-02, FR-SWV-01 | Reference rules + X charset |

#### F. Display / Computed Data

| Field Name | Calculation Formula |
| :--- | :--- |
| **Base Equivalent Amount** | `amount × Current System Exchange Rate` (LC Currency → Local Currency). Recalculated before submit. Uses Daily Board Rate (per REQ-COM-FX-02). |
| **Maximum Liability Amount** | `amount + (amount × tolerancePositive / 100)`. True risk exposure. |
| **Available Facility Limit** | Fetched live from Core Banking: `Total Limit - Currently Utilized`. |
| **Limit Check Status** | `true` if `Available Facility Limit ≥ Maximum Liability Amount`. Blocks submission if false. |

#### G. Post-Submit Processing & SWIFT Generation
Upon Checker authorization:
1. **Facility Earmark:** Synchronous call to Core Banking to deduct Maximum Liability Amount.
2. **Cash Margin Hold:** If insufficient credit line, hold on deposit account for margin percentage.
3. **Fee Deduction:** Issuance commission per bank tariff matrix (e.g., 0.125% per quarter).
4. **Entity Creation:** `TradeInstrument` and `ImportLetterOfCredit` records committed.
5. **SWIFT MT 700 Generation:**
   - Call `validate#SwiftFields` (Layer 2 safety net) on all source entities.
   - Assemble MT 700 using `SwiftMessageBuilder` with entity data.
   - **MT 701 Auto-Continuation:** If Tags 45A, 46A, or 47A exceed 100×65 chars, auto-generate MT 701 with overflow in Tags 45B, 46B, 47B. Sequence Tag 27 updated to `2/N`.
   - Save as `SwiftMessage` with `messageStatusId = SWIFT_MSG_ACTIVE` (post-approval, immutable).
   - Log dispatch in `TradeTransactionAudit`.
6. **Customer Advice:** Generate PDF advice with LC terms, fees, Transaction Reference Number.

#### H. User Stories

### US-ISS-01: LC Issuance with Full Validation
**As a** Trade Operations Maker,
**I want** to enter all LC issuance data with inline SWIFT validation,
**So that** I can submit a compliant application that won't fail at SWIFT generation.

### US-ISS-02: Automatic Limit Earmarking on Issuance
**As a** Trade Operations system,
**I want** to automatically earmark the customer's facility limit upon Checker approval,
**So that** the bank's credit exposure is tracked in real-time.

### US-ISS-03: MT700 Generation on Authorization
**As a** Trade Operations system,
**I want** to generate a standards-compliant MT700 from entity data upon Checker authorization,
**So that** the Advising Bank receives a correctly formatted documentary credit notification.

#### I. Grounding Info
*   **Data Dictionary:** From **REQ-IMP-SPEC-01** (baseline), enhanced by **FR-ENT-01 to FR-ENT-11** (validation), **FR-ENT-21 to FR-ENT-27** (gaps).
*   **Party Junction:** From **FR-TP-03**, **FR-TP-07** (tradeparty-refactor).
*   **SWIFT Validation:** From **FR-SWV-01 to FR-SWV-09** (swift-validation-brd.md), **FR-SGC-07, FR-SGC-08** (gaps).
*   **MT700/MT701 Generation:** From **FR-SWG-05, FR-SWG-06** (swift-generation-brd.md), **FR-SGC-04** (gaps), **REQ-IMP-SWIFT-02/03** (baseline).
*   **Message Lifecycle:** From **FR-SWG-15** — post-approval → ACTIVE, immutable.

---

### 2.2 Process — Amendments

#### A. Related States
*   **Transaction State:** Draft (Amendment) → Pending Approval → Executed
*   **LC Business State:** Parent LC remains **Issued**. Amendment record transitions: Draft → Pending Approval → Dispatched → Accepted/Rejected (by Beneficiary).

#### B. Business Process Workflow
1. **Initiation:** Operations locates active LC and clicks "Initiate Amendment."
2. **Delta Entry:** Maker inputs only changed fields.
3. **System Categorization:** Auto-determines Financial vs Non-Financial.
4. **Submission:** Maker submits. Checker tier calculated on **new total liability**.
5. **Checker Review & Authorization:** Checker approves.
6. **Execution:** System deducts/releases limits, applies fees, generates MT 707.
7. **Beneficiary Consent Tracking:** Amendment pending until Beneficiary acceptance/rejection logged.

#### C. Initiation & Post Conditions
*   **Entry:** Parent LC in Issued state, not expired. No pending un-examined presentations.
*   **Exit:** MT 707 dispatched. New terms binding only upon Beneficiary acceptance logging.

#### D. Amendment Inputs (Delta Fields with SWIFT Tag Mapping)

| Field Name | Req/Opt | Data Type | SWIFT Tag | Validation Rules |
| :--- | :--- | :--- | :--- | :--- |
| `amendmentDate` | Req | Date | Tag 30 | Cannot be in the past. Formatted YYMMDD for SWIFT. |
| `amountIncrease` | Opt | number-decimal | Tag 32B | Mutually exclusive with `amountDecrease`. Comma decimal, max 15 digits. |
| `amountDecrease` | Opt | number-decimal | Tag 33B | Cannot exceed current Available Balance. Comma decimal. |
| `newTotalAmount` | Req (computed) | number-decimal | Tag 34B | `original + increase - decrease`. Comma decimal, max 15 digits. |
| `newTolerancePositive` | Opt | number-integer | Tag 39A | Overwrites previous tolerance. Mutually exclusive with `maxCreditAmountFlag`. |
| `newToleranceNegative` | Opt | number-integer | Tag 39A | Overwrites previous tolerance. Mutually exclusive with `maxCreditAmountFlag`. |
| `newExpiryDate` | Opt | Date | Tag 31E | Must be ≥ current Business Date. Formatted YYMMDD. |
| `newLatestShipmentDate` | Opt | Date | Tag 44C | Must be ≤ New Expiry or Original Expiry. Mutually exclusive with `shipmentPeriodText`. |
| `amendmentNarrative` | Cond | text-very-long | Tag 79 | Max 35×50. **Z charset** (extends X with `@ # = ! " % & * ; < > _`). Required if no standard fields changed. |
| `amendmentCharges` | Req | Enum | — | APPLICANT, BENEFICIARY. Determines fee payer. |
| `beneficiaryDecision` | Opt | Enum | — | PENDING, ACCEPTED, REJECTED. Defaults to PENDING. |

#### E. SWIFT Validation Rules Specific to Amendments

| Rule ID | Rule | SWIFT Tags Affected |
| :--- | :--- | :--- |
| AMD-SWV-01 | `amendmentNarrative` uses Z charset (not X) | Tag 79 |
| AMD-SWV-02 | Amount increase → Tag 32B; decrease → Tag 33B; always include Tag 34B if amount changed | Tags 32B, 33B, 34B |
| AMD-SWV-03 | `amendmentNumber` auto-incremented (1, 2, 3...) per parent LC. Always included. | Tag 26E (Mandatory) |
| AMD-SWV-04 | If `amendmentNarrative` changed, or shipping ports amended: re-screen against Sanctions | — |
| AMD-SWV-05 | Tolerance (39A) and Max Credit (39B) mutual exclusion still applies if modified | Tags 39A, 39B |
| AMD-SWV-06 | Authority tier calculated on **New Maximum Liability** (not delta) | — |

##### Source Traceability

| Rule ID | Source BRD | Source Requirement ID | Notes |
| :--- | :--- | :--- | :--- |
| AMD-SWV-01 | swift-validation-brd.md | FR-SWV-01 | Z charset definition for Tag 79 (MT707 narrative) |
| AMD-SWV-02 | swift-generation-brd.md | FR-SWG-07 | MT707 delta logic: 32B increase, 33B decrease, 34B new total |
| AMD-SWV-03 | swift-gaps-consolidation-brd.md | FR-ENT-28 | Amendment number auto-increment, always included in MT707 |
| AMD-SWV-04 | import-lc-brd.md | REQ-IMP-SPEC-02 Section I | Sanctions re-screening if narrative or shipping ports change |
| AMD-SWV-05 | swift-validation-brd.md | FR-SWV-07 | Mutual exclusion 39A/39B inherited from issuance |
| AMD-SWV-06 | common-module-brd.md | REQ-COM-AUTH-03 Section A | Financial amendments tiered on new total liability, not delta |

#### F. Display / Computed Data

| Field Name | Calculation Formula |
| :--- | :--- |
| **Amendment Number** | Auto-incremented (1, 2, 3...) per parent LC. |
| **New Total LC Amount** | `Original Amount + Increase - Decrease`. |
| **New Maximum Liability** | `New Total Amount + (New Total Amount × New Tolerance Positive / 100)`. |
| **Limit Delta Required** | `New Maximum Liability - Original Maximum Liability`. Positive = earmark more; negative = release. |
| **Required Authority Tier** | Calculated by Maker/Checker Matrix using **New Maximum Liability** (not delta). |

#### G. Post-Submit Processing & SWIFT Generation
1. **Facility Delta Update:** Earmark additional funds or release excess.
2. **Fee Application:** Amendment flat fees + additional issuance commission if amount increased/expiry extended.
3. **Amendment Record Commit:** Delta saved to database, linked to parent LC.
4. **SWIFT MT 707 Generation:**
   - Call `validate#SwiftFields` (Layer 2) on amendment and parent LC entities.
   - Assemble MT 707 using `SwiftMessageBuilder` with **delta-only tags** — only changed fields populate corresponding tags.
   - Tag 26E (amendment number) always included. Tag 34B (new total) only if amount changed.
   - Save as `SwiftMessage` with `messageStatusId = SWIFT_MSG_ACTIVE` (immutable).

#### H. User Stories

### US-AMD-01: Financial vs Non-Financial Amendment Classification
**As a** Trade Operations system,
**I want** to automatically classify amendments as financial or non-financial,
**So that** the correct Checker authority tier is applied.

### US-AMD-02: Beneficiary Consent Tracking
**As a** Trade Operations Maker,
**I want** to log the Beneficiary's acceptance or rejection of an amendment,
**So that** the amendment terms become legally binding only with consent.

### US-AMD-03: MT707 Delta-Only Generation
**As a** Trade Operations system,
**I want** to generate MT707 containing only changed tags when an amendment is authorized,
**So that** the Advising Bank accurately processes only the modified terms.

#### I. Grounding Info
*   **Process:** From **REQ-IMP-SPEC-02** (baseline).
*   **Delta-Only Tags:** From **FR-SWG-07** (generation spec).
*   **Amendment Number:** From **FR-ENT-28** (gaps spec).
*   **Z Charset:** From **FR-SWV-01** (validation spec) — Tag 79 uses Z charset.
*   **Authority on New Total:** From **REQ-COM-AUTH-03** (common module).
*   **Sanctions Re-screening:** From **REQ-IMP-SPEC-02 Section I** (baseline).

---

### 2.3 Process — Document Presentation & Examination

#### A. Related States
*   **Transaction State:** Draft (Examination) → Pending Approval → Executed
*   **LC Business State:** Issued → Documents Received → Discrepant OR Accepted/Clean

#### B. Business Process Workflow
1. **Lodgement:** Operations creates Presentation Record, inputs presentation details. State → Documents Received.
2. **Examination:** Maker reviews documents against LC terms and ISBP 745 rules.
3. **Discrepancy Logging:** If non-compliant, Maker logs discrepancy codes. If compliant, marks clean.
4. **Submission:** Maker submits examination results. State → Pending Approval.
5. **Checker Review:** Checker independently verifies.
6. **Execution:**
   - **Clean:** State → Accepted/Clean.
   - **Discrepant:** State → Discrepant. MT 734 optionally prepared.

#### C. Initiation & Post Conditions
*   **Entry:** Parent LC in Issued state with available unutilized balance.
*   **Exit:** Bank formally accepts or refuses presentation.

#### D. Presentation Inputs (with SWIFT Tag Mapping)

| Field Name | Req/Opt | Data Type | SWIFT Tag | Validation Rules |
| :--- | :--- | :--- | :--- | :--- |
| `presentingBankPartyId` | Req | id | — | Via `TradeInstrumentParty` junction (role: TP_PRESENTING_BANK). Added when presentation created. |
| `presentationDate` | Req | Date | Tag 30/32A | Cannot be in the future. Formatted YYMMDD for SWIFT. |
| `presentingBankRef` | Opt | text-short | Tag 21 | Max 16 chars, X charset. Slash rules: no leading/trailing `/`, no `//`. |
| `presentationRef` | Req | text-short | Tag 20 | Max 16 chars, X charset. No leading/trailing `/`, no `//`. |
| `claimAmount` | Req | number-decimal | Tag 32B | Must be > 0. Comma decimal, max 15 digits. |
| `claimCurrency` | Req | id | Tag 32B | Must match parent LC currency. ISO 4217. |
| `discrepancyFound` | Req | Boolean | — | Determines routing (Clean vs Discrepant). |
| `discrepancyDetails` | Cond | Array | Tag 77J | Required if discrepancyFound = true. Each entry: `discrepancyCode` (max 35 chars, X charset) + `discrepancyDescription` (max 50 chars, X charset). |
| `documentDisposalEnumId` | Cond | id | Tag 77B | HOLDING_DOCUMENTS, RETURNING_DOCUMENTS. **Required when refusing discrepant documents**. |
| `applicantDecision` | Opt | Enum | — | PENDING, WAIVED, REFUSED. Used only if discrepant. |
| `chargesDeducted` | Opt | text-long | Tag 73 | Max 6×35, X charset. Discrepancy fee text. |

##### Document Type Grid (per document received)
| Field Name | Req/Opt | Data Type |
| :--- | :--- | :--- |
| `documentType` | Req | String | Bill of Lading, Commercial Invoice, Packing List, Origin Certificate, etc. |
| `originalCount` | Req | number-integer | Number of original copies. |
| `copyCount` | Req | number-integer | Number of photocopies. |

#### E. SWIFT Validation Rules Specific to Presentation

| Rule ID | Rule | SWIFT Tags Affected |
| :--- | :--- | :--- |
| PRE-SWV-01 | `presentationRef`: max 16 chars, X charset, slash rules | Tag 20 |
| PRE-SWV-02 | `claimAmount`: comma decimal, max 15 digits, positive | Tag 32B |
| PRE-SWV-03 | `claimCurrency` must match parent LC currency | Tag 32B |
| PRE-SWV-04 | If `discrepancyFound = true`: at least one `PresentationDiscrepancy` record required | Tag 77J |
| PRE-SWV-05 | If `applicantDecision = REFUSED`: `documentDisposalEnumId` required | Tag 77B |
| PRE-SWV-06 | Discrepancy codes and descriptions: X charset, max 35/50 chars per entry | Tag 77J |
| PRE-SWV-07 | Claim amount must not exceed remaining LC balance + tolerance % | — |

##### Source Traceability

| Rule ID | Source BRD | Source Requirement ID | Notes |
| :--- | :--- | :--- | :--- |
| PRE-SWV-01 | swift-gaps-consolidation-brd.md | FR-ENT-30 | presentationRef: mandatory, max 16 chars, X charset, slash rules |
| PRE-SWV-02 | swift-validation-brd.md | FR-SWV-03 | Amount format: comma decimal, max 15 digits, positive |
| PRE-SWV-03 | import-lc-brd.md | REQ-IMP-SPEC-03 Section D | Claim currency must match parent LC currency |
| PRE-SWV-04 | swift-validation-brd.md | FR-SWV-08 | Conditional: isDiscrepant = Y requires at least one discrepancy record |
| PRE-SWV-05 | swift-validation-brd.md | FR-SWV-08 | Conditional: applicantDecision = REFUSED requires documentDisposalEnumId |
| PRE-SWV-06 | swift-validation-brd.md | FR-SWV-01, FR-SWV-06 | Discrepancy code: max 35 chars (77J), description: max 50 chars, X charset |
| PRE-SWV-07 | import-lc-brd.md | REQ-IMP-04 | Tolerance limit check from baseline business rules |

#### F. Display / Computed Data

| Field Name | Calculation Formula |
| :--- | :--- |
| **Presentation Reference** | Auto-generated unique ID (e.g., PR-IMP-2026-001). |
| **Regulatory Deadline** | `presentationDate + 5 Banking Days`. UCP 600 hard-stop. |
| **Remaining LC Balance** | `Parent LC Amount - Total Previously Accepted Claims`. |
| **Overdrawn Status** | `true` if `claimAmount > (Remaining Balance + Tolerance %)`. |

#### G. Post-Submit Processing & SWIFT Generation
1. **Limit Update:** Contingent liability → firm liability (or acceptance liability for Usance).
2. **SLA Tracking:** 5-day countdown timer stops.
3. **Discrepancy Fee:** Auto-calculated and deducted per LC terms.
4. **SWIFT Generation** (based on outcome):
   - **Discrepant → MT 750:** Assemble from `TradeInstrument.transactionRef`, `presentingBankRef`, `claimCurrency` + `claimAmount`, concatenated `PresentationDiscrepancy` records (Tag 77J), optional `senderToReceiverInfo` (Tag 72Z). Save as `SWIFT_MSG_ACTIVE`.
   - **Refused → MT 734:** Assemble from `transactionRef`, `presentingBankRef`, `presentationDate` + `claimCurrency` + `claimAmount` (Tag 32A), concatenated discrepancies (Tag 77J), `documentDisposalEnumId` mapped to `HOLDING DOCUMENTS` or `RETURNING DOCUMENTS` (Tag 77B), optional discrepancy fee text (Tag 73). Save as `SWIFT_MSG_ACTIVE`.
   - **Waived → MT 752:** Assemble from `transactionRef`, `presentingBankRef`, system business date (Tag 30), `claimCurrency` + `claimAmount` (Tag 32B), optional `"DISCREPANCIES WAIVED BY APPLICANT"` (Tag 72Z). Save as `SWIFT_MSG_ACTIVE`.
   - **Usance Accepted → MT 732:** Assemble from `transactionRef`, `presentingBankRef`, system business date (Tag 30), **maturity date** + `claimCurrency` + `claimAmount` (Tag 32A — date is future maturity). Save as `SWIFT_MSG_ACTIVE`.

#### H. User Stories

### US-PRE-01: Document Lodgement & Examination
**As a** Trade Operations Maker,
**I want** to log received documents and record examination results with discrepancy codes,
**So that** the bank meets its UCP 600 examination obligations.

### US-PRE-02: Regulatory Deadline Enforcement
**As a** Trade Operations system,
**I want** to enforce the 5-banking-day examination window,
**So that** the bank avoids UCP 600 violations.

#### I. Grounding Info
*   **Process:** From **REQ-IMP-SPEC-03** (baseline).
*   **Entity Fields:** From **FR-ENT-19** (validation), **FR-ENT-30 to FR-ENT-33** (gaps).
*   **Party Junction:** From **FR-TP-08** — `presentingBankBic` replaced by `TP_PRESENTING_BANK` junction.
*   **SWIFT Generation:** From **FR-SWG-08** (MT750), **FR-SWG-09** (MT734), **FR-SWG-10** (MT752), **FR-SWG-11** (MT732).
*   **Conditional Validation:** From **FR-SWV-08** (validation spec) — discrepancy-dependent fields.

---

### 2.4 Process — Settlement & Payment

#### A. Related States
*   **Transaction State:** Draft (Settlement) → Pending Approval → Executed
*   **LC Business State:** Accepted/Clean → Settled

#### B. Business Process Workflow
1. **Initiation (Sight):** Immediately upon clean presentation or accepted waiver.
2. **Initiation (Usance):** MT 732 generated upon acceptance. On maturity date, system auto-queues settlement.
3. **Data Entry:** Maker inputs exchange rates, debit accounts, remittance routing.
4. **Submission:** Maker submits. State → Pending Approval.
5. **Checker Review:** Verifies payment details against presentation and available funds.
6. **Execution:** System debits Applicant, releases liability, generates SWIFT.

#### C. Initiation & Post Conditions
*   **Entry:** Presentation in Accepted/Clean state. For Usance: current date = Maturity Date.
*   **Exit:** Funds transferred. Presentation claim closed. LC → Closed if fully drawn.

#### D. Settlement Inputs (with SWIFT Tag Mapping)

| Field Name | Req/Opt | Data Type | SWIFT Tag | Validation Rules |
| :--- | :--- | :--- | :--- | :--- |
| `valueDate` | Req | Date | Tag 32A | Cannot be in the past. Formatted YYMMDD for SWIFT. |
| `principalAmount` | Req | number-decimal | Tag 32A | Must equal Accepted Claim Amount. Comma decimal, max 15 digits, positive. |
| `remittanceCurrency` | Req | id | Tag 32A | Must match LC/Claim currency. ISO 4217. |
| `applicantDebitAccount` | Req | String | — | Valid, active CASA account belonging to Applicant. |
| `appliedMarginAmount` | Opt | number-decimal | — | Cash collateral to utilize (if taken during issuance). |
| `fxExchangeRate` | Cond | number-decimal | — | Required if Debit Account currency ≠ Remittance Currency. Uses Live Treasury Rate (per REQ-COM-FX-02). |
| `forwardContractRef` | Opt | text-short | — | Max 16 chars, X charset, slash rules. Reference to pre-booked Treasury FX contract. |
| `chargesDetailEnumId` | Req | Enum | Tag 71A | OUR, BEN, SHA. Defaults to LC terms. Mandatory for MT103. |

##### Settlement Party Roles (added during settlement process)
| Role | SWIFT Tag | Notes |
| :--- | :--- | :--- |
| `TP_PRESENTING_BANK` | Tag 58a (MT202) | Beneficiary institution for bank-to-bank transfer. |
| `TP_INTERMEDIARY_BANK` | Tag 56a | Intermediate routing bank (if needed). |
| `TP_SENDERS_CORRESPONDENT` | Tag 53a (MT202) | Issuing bank's correspondent. Requires `nostroAccountRef`. |
| `TP_RECEIVERS_CORRESPONDENT` | Tag 54a (MT202) | Presenting bank's correspondent. |

#### E. SWIFT Validation Rules Specific to Settlement

| Rule ID | Rule | SWIFT Tags Affected |
| :--- | :--- | :--- |
| STL-SWV-01 | `principalAmount`: comma decimal, max 15 digits, positive | Tag 32A |
| STL-SWV-02 | `remittanceCurrency` must match LC/Claim currency | Tag 32A |
| STL-SWV-03 | `valueDate` formatted as YYMMDD | Tag 32A |
| STL-SWV-04 | For MT202: `TP_SENDERS_CORRESPONDENT` must have `nostroAccountRef` populated | Tag 53a |
| STL-SWV-05 | For MT103: Beneficiary (`TP_BENEFICIARY`) `accountNumber` is **mandatory** | Tag 59a |
| STL-SWV-06 | For MT103: `chargesDetailEnumId` required (OUR, BEN, SHA) | Tag 71A |
| STL-SWV-07 | For MT202: Presenting Bank BIC required | Tag 58a |
| STL-SWV-08 | Nostro account auto-derived from `TradeConfig.NOSTRO_ACCOUNT_{CCY}` | Tag 53a (MT202) |

##### Source Traceability

| Rule ID | Source BRD | Source Requirement ID | Notes |
| :--- | :--- | :--- | :--- |
| STL-SWV-01 | swift-validation-brd.md | FR-SWV-03 | Amount format: comma decimal, max 15 digits, positive (Tag 32A) |
| STL-SWV-02 | swift-validation-brd.md | FR-SWV-04 | Date format: YYMMDD for valueDate |
| STL-SWV-03 | import-lc-brd.md | REQ-IMP-SPEC-04 Section D | Remittance currency must match LC/Claim currency |
| STL-SWV-04 | tradeparty-refactor-brd.md | FR-TP-12 | Bank eligibility: Reimbursing/Sender's Correspondent requires Nostro reference |
| STL-SWV-05 | tradeparty-refactor-brd.md | FR-TP-04 Tag Coverage | MT103 Tag 59a: beneficiary accountNumber is mandatory |
| STL-SWV-06 | swift-generation-brd.md | FR-SWG-14 | MT103 Tag 71A: mandatory charges detail (OUR/BEN/SHA) |
| STL-SWV-07 | swift-generation-brd.md | FR-SWG-13 | MT202 Tag 58a: beneficiary institution (Presenting Bank) mandatory |
| STL-SWV-08 | swift-validation-brd.md | FR-CFG-02 | NOSTRO_ACCOUNT_{CCY} config key per currency for settlement routing |

#### F. Display / Computed Data

| Field Name | Calculation Formula |
| :--- | :--- |
| **Total Debit Amount** | `(principalAmount × fxExchangeRate) - appliedMarginAmount + calculatedBankFees`. |
| **Nostro Account** | Auto-derived based on Remittance Currency (from `TradeConfig.NOSTRO_ACCOUNT_{CCY}`). |
| **Account Balance Check** | `true` if Applicant Debit Account has sufficient funds. Blocks submission if false. |

#### G. Post-Submit Processing & SWIFT Generation
1. **Liability Reversal:** Reverses firm/acceptance liability for claim amount.
2. **Margin Release:** Releases margin block, transfers to settlement suspense.
3. **Core Debit:** Debits Total Debit Amount from Applicant's operating account.
4. **Parent LC Update:** Deducts settled amount. If remaining balance = 0 (within tolerance), LC → Closed.
5. **SWIFT Generation** (based on routing):
   - **Bank-to-Bank → MT 202:** Assemble from `transactionRef`, `presentingBankRef`, `valueDate` + `remittanceCurrency` + `principalAmount` (Tag 32A), Issuing bank's Nostro (Tag 53a from config), Presenting Bank BIC (Tag 58a). Save as `SWIFT_MSG_ACTIVE`.
   - **Customer Direct → MT 103:** Assemble from `transactionRef`, `valueDate` + `remittanceCurrency` + `principalAmount` (Tag 32A), Applicant details (Tag 50a), Beneficiary details with **mandatory account number** (Tag 59a), charges (Tag 71A). Save as `SWIFT_MSG_ACTIVE`.

#### H. User Stories

### US-STL-01: Sight LC Settlement
**As a** Trade Operations system,
**I want** to trigger immediate settlement upon clean presentation acceptance for Sight LCs,
**So that** the beneficiary receives payment promptly per UCP 600.

### US-STL-02: Usance LC Maturity Tracking
**As a** Trade Operations system,
**I want** to track Usance maturity dates and auto-queue settlements on the due date,
**So that** deferred payments are not missed.

### US-STL-03: Live FX Rate for Settlement
**As a** Trade Operations system,
**I want** to use Live Treasury FX rates at settlement time,
**So that** the bank bears zero market risk on cash movement (per REQ-COM-FX-02).

#### I. Grounding Info
*   **Process:** From **REQ-IMP-SPEC-04** (baseline).
*   **SWIFT Generation:** From **FR-SWG-13** (MT202), **FR-SWG-14** (MT103).
*   **Live FX Rate:** From **REQ-COM-FX-02** (common module) and common-consolidated-brd.md Feature 2.
*   **Settlement Roles:** From **FR-TP-04** — settlement-specific party roles added during this process.
*   **MT103 Account Number Rule:** From **FR-TP-04 Tag Coverage Verification** — beneficiary account mandatory for MT103.

---

### 2.5 Process — Shipping Guarantees (SG)

#### A. Related States
*   **Transaction State:** Draft (SG Issuance) → Pending Approval → Executed
*   **SG Sub-State:** Issued → Redeemed/Closed
*   **LC Business State Impact:** Parent LC remains Issued. Hard lock on utilized limit. Waiver lock flag set.

**Note:** No SWIFT messages are generated for Shipping Guarantees — they are local indemnities given to local shipping agents.

#### B. Business Process Workflow
1. **Application:** Applicant submits SG request with invoice copy and non-negotiable transport document.
2. **Data Entry:** Maker links SG to active parent LC, inputs shipping details.
3. **Limit Assessment:** System calculates SG Liability (110%-150% of invoice value).
4. **Submission & Approval:** Checker authorizes on elevated risk tier.
5. **Issuance:** System earmarks facility, generates physical SG indemnity form.
6. **Redemption:** When official documents arrive, bank accepts them automatically. Original B/L exchanged for returned SG. SG marked Redeemed.

#### C. Initiation & Post Conditions
*   **Entry:** Parent LC in Issued state. No physical documents logged for this shipment.
*   **Exit:** Bank legally indemnifies carrier. Applicant forfeits right to refuse eventual document presentation.

#### D. SG Inputs

| Field Name | Req/Opt | Data Type | Validation Rules |
| :--- | :--- | :--- | :--- |
| `sgTypeEnumId` | Req | id | SHIPPING_GUARANTEE_SEA, AWB_ENDORSEMENT_AIR. |
| `sgIssueDate` | Req | Date | Defaults to current business date. |
| `invoiceAmount` | Req | number-decimal | Value of goods being claimed. |
| `sgLiabilityPercent` | Req | number-integer | Standard 100%. Sea freight: 110%, 125%, or 150%. |
| `transportDocRef` | Req | String | Bill of Lading or Air Waybill number. |
| `carrierAgentName` | Req | String | Shipping line to whom guarantee is addressed. |
| `vesselNameVoyage` | Cond | String | Required if SG Type is Sea. |
| `goodsDescription` | Req | Text | Brief description of goods. |

#### E. Display / Computed Data

| Field Name | Calculation Formula |
| :--- | :--- |
| **SG Liability Amount** | `invoiceAmount × (sgLiabilityPercent / 100)`. |
| **Required Facility Earmark** | If `SG Liability Amount > Parent LC Available Balance`, difference must be earmarked or cash margined. |
| **Waiver Lock Flag** | Set to `true`. Prevents Applicant from refusing subsequent document presentation. |

#### F. Post-Submit Processing
1. **Facility Update:** Locks SG Liability Amount. Cannot be released when LC expires — only when SG returned.
2. **Commission Application:** SG issuance commission (flat fee + monthly recurring until redeemed).
3. **Cross-Process Rule:** When Document Presentation later initiated for this Transport Doc Ref, system auto-bypasses "Applicant Decision" and forces → Accepted.

#### G. User Stories

### US-SG-01: Shipping Guarantee Issuance
**As a** Trade Operations Maker,
**I want** to issue a Shipping Guarantee against an active LC,
**So that** the Applicant can take possession of goods before documents arrive.

### US-SG-02: Automatic Waiver Lock
**As a** Trade Operations system,
**I want** to prevent the Applicant from refusing documents after an SG is issued,
**So that** the bank's indemnity to the carrier is protected.

#### H. Grounding Info
*   **Process:** From **REQ-IMP-SPEC-05** (baseline).

---

### 2.6 Process — Cancellations

#### A. Related States
*   **Transaction State:** Draft (Cancellation) → Pending Approval → Executed
*   **LC Business State:** Issued → Closed / Cancelled

#### B. Closure Scenarios

1. **Auto-Closure (Fully Drawn):** When settlement brings unutilized balance to zero, system auto-closes LC. No Maker/Checker action required.
2. **Auto-Expiry (Unutilized):** EOD batch identifies LCs past Expiry Date + configurable "Mail Days" grace period (e.g., +15 days). After grace period with no presentations, system auto-closes and releases limits.
3. **Early Mutual Cancellation:**
   - Applicant requests cancellation (underlying contract fell through).
   - Operations generates SWIFT to Advising Bank requesting Beneficiary consent.
   - Upon receiving authenticated SWIFT confirming consent, operations submits for Checker authorization. Limits released.

#### C. Initiation & Post Conditions
*   **Entry:** LC in Issued state. No pending, un-examined, or discrepant presentations active.
*   **Exit:** LC in terminal state. Credit limits restored.

#### D. Cancellation Inputs (Manual Early Cancellation Only)

| Field Name | Req/Opt | Data Type | Validation Rules |
| :--- | :--- | :--- | :--- |
| `closureTypeEnumId` | Req | id | FULLY_DRAWN, EXPIRED, EARLY_CANCELLATION. |
| `closureDate` | Req | Date | Defaults to current business date. |
| `cancelledAmount` | Req | number-decimal | Exact unutilized balance being cancelled. |
| `beneficiaryConsent` | Cond | Enum | PENDING, CONSENTED, REFUSED. Required for Early Cancellation. |
| `consentSwiftRef` | Cond | text-short | Reference of incoming SWIFT MT 799/730 confirming agreement. |
| `cancellationFee` | Opt | number-decimal | Flat fee for early processing. |

#### E. SWIFT Validation Rules Specific to Cancellation

| Rule ID | Rule | SWIFT Tags Affected |
| :--- | :--- | :--- |
| CAN-SWV-01 | Early cancellation generates MT 799 free-format message | Tag 79 |
| CAN-SWV-02 | MT 799 narrative uses **Z charset** (Tag 79), max 35×50 | Tag 79 |
| CAN-SWV-03 | MT 799 includes `transactionRef` (Tag 20) and Advising Bank ref or NONREF (Tag 21) | Tags 20, 21 |

##### Source Traceability

| Rule ID | Source BRD | Source Requirement ID | Notes |
| :--- | :--- | :--- | :--- |
| CAN-SWV-01 | swift-generation-brd.md | FR-SWG-12 | MT799 free-format message for early cancellation consent request |
| CAN-SWV-02 | swift-validation-brd.md | FR-SWV-01 | Tag 79 uses Z charset (extends X with `@ # = ! " % & * ; < > _`), max 35×50 |
| CAN-SWV-03 | swift-generation-brd.md | FR-SWG-12 | MT799 structure: mandatory Tag 20 (credit no.), Tag 21 (ref or NONREF), Tag 79 (narrative) |

#### F. Display / Computed Data

| Field Name | Calculation Formula |
| :--- | :--- |
| **Mail Days Grace Period** | `LC Expiry Date + System Parameter (e.g., 15 Days)`. Hard-stop before auto-expiry. |
| **Total Limit to Release** | `cancelledAmount + (cancelledAmount × tolerancePositive / 100)`. |
| **Margin to Release** | Proportional cash collateral tied to cancelled balance. |

#### G. Post-Submit Processing & SWIFT Generation
1. **Early Cancellation → MT 799:** System-generated cancellation request narrative (Z charset, 35×50). Includes `transactionRef` and Advising Bank ref. Save as `SWIFT_MSG_ACTIVE`.
2. **Upon Consent Received → Checker Authorization:** Limits released, LC → Closed.
3. **Auto-Closure/Auto-Expiry:** No SWIFT generated. Limits restored via Core Banking. State Lock applied — block new presentations, amendments, or fees.

#### H. User Stories

### US-CAN-01: Auto-Expiry with Mail Days Grace
**As a** Trade Operations system,
**I want** to auto-close LCs after Expiry Date plus a configurable grace period,
**So that** overseas documents in transit are accommodated before releasing limits.

### US-CAN-02: Early Mutual Cancellation with Beneficiary Consent
**As a** Trade Operations Maker,
**I want** to request early cancellation and track Beneficiary consent via SWIFT,
**So that** the LC is cancelled only with all parties' agreement.

#### I. Grounding Info
*   **Process:** From **REQ-IMP-SPEC-06** (baseline).
*   **Mail Days:** From product configuration `Auto-Expiry days` in REQ-COM-PRD-01 (common module).
*   **MT799 Generation:** From **FR-SWG-12** (swift-generation-brd.md).

---

## 3. Feature 3: SWIFT Validation Infrastructure (Common)

### 3.1 Shared Character Sets

**X Character Set** (majority of SWIFT fields):
- Allowed: `A-Z a-z 0-9 / - ? : ( ) . , ' +` and spaces
- Blocked: `@ & _ # ! % ^ * { } [ ] | \ " ; < > ~` and all other special characters

**Z Character Set** (Tags 79 in MT707, MT799):
- Extends X charset with: `@ # = ! " % & * ; < > _`

### 3.2 Common Validation Rules (Apply Across All Processes)

| Rule ID | Rule | Applies To |
| :--- | :--- | :--- |
| SWV-01 | X Character Set validation | All X charset fields (narratives, names, addresses, references, descriptions) |
| SWV-02 | Z Character Set validation | Tag 79 only (amendment narrative, cancellation request) |
| SWV-03 | Reference slash rules: no leading/trailing `/`, no `//` | Tags 20, 21, 23 (all reference fields) |
| SWV-04 | Amount format: comma decimal, max 15 digits, positive | Tags 32A, 32B, 33B, 34B (all financial amount fields) |
| SWV-05 | Date format: YYMMDD | All date fields in SWIFT tags |
| SWV-06 | BIC format: exactly 8 or 11 alphanumeric characters | All BIC fields (Tags 41A, 42A, 53a, 57a, party BICs) |
| SWV-07 | Line format: NxM (4×35, 6×35, 12×65, 35×50, 70×50, 100×65) | Multi-line narrative fields — validated at data capture, auto-wrapped at generation |
| SWV-08 | Mutual exclusion: 39A/39B, 44C/44D, 53A/53D, 57A/57D | Tolerance vs Max Credit, Shipment Date vs Period, Bank BIC vs Name |
| SWV-09 | Conditional requirements enforcement | Tenor-dependent fields, discrepancy-dependent fields (see process-specific tables) |

### 3.3 Validation Service API

**Service:** `validate#SwiftFields`
- Accepts entity type and entity ID
- Validates ALL SWIFT-bound fields on that entity
- Returns field-level error messages with specific violation details (e.g., "Description of Goods contains invalid character '@' at position 142")
- Can be called on save (incremental) and on submit (comprehensive)
- Supports all SWIFT-bound entities: `ImportLetterOfCredit`, `TradeInstrument`, `TradeParty`, `TradePartyBank`, `ImportLcAmendment`, `TradeDocumentPresentation`, `PresentationDiscrepancy`, `ImportLcSettlement`

### 3.4 Layer 2 Generation-Time Validation (Safety Net)
Before assembling any SWIFT message:
1. Call `validate#SwiftFields` on all source entities
2. If errors: abort generation, return errors, log warning
3. If data passes but contains unexpected characters: auto-convert (e.g., `&` → `AND`) and log warning
4. All Layer 2 conversions logged with field name, original value, and converted value

### 3.5 User Stories

### US-SWV-01: Inline SWIFT Character Validation
**As a** Trade Operations Maker,
**I want** the system to immediately flag invalid SWIFT characters during data entry,
**So that** I can fix formatting issues before submission instead of having transactions rejected at generation time.

### US-SWV-02: Pre-Submission Comprehensive Validation
**As a** Trade Operations Maker,
**I want** a complete SWIFT compliance check before submitting for approval,
**So that** I receive all issues in one pass and can fix them efficiently.

#### 3.6 Grounding Info
*   **Character Sets:** From **FR-SWV-01** (swift-validation-brd.md).
*   **Validation Rules:** From **FR-SWV-01 through FR-SWV-08** (swift-validation-brd.md).
*   **Validation Service:** From **FR-SWV-09** (swift-validation-brd.md).
*   **Layer 2 Validation:** From **FR-SWG-03** (swift-generation-brd.md).

---

## 4. Feature 4: SWIFT Message Generation (Common Infrastructure)

### 4.1 SWIFT Message Builder
Reusable Groovy utility class (`SwiftMessageBuilder`) that:
- Constructs SWIFT Basic Header Block 1 (sender BIC from `TradeConfig.ISSUING_BANK_BIC`)
- Constructs Application Header Block 2 (message type, receiver BIC)
- Constructs User Header Block 3 (optional)
- Constructs Text Block 4 (tag content)
- Handles tag formatting: date→YYMMDD, amount→comma-decimal, multi-line→NxM wrapping
- `build()` method returns assembled SWIFT message text

### 4.2 Tag Formatting Services
| Service | Output Format |
| :--- | :--- |
| `format#SwiftDate(date)` | YYMMDD |
| `format#SwiftAmount(currency, amount)` | CCY + comma-decimal (e.g., `USD50000,00`) |
| `format#SwiftParty(name, address, accountNumber)` | 4×35 formatted party block |
| `format#SwiftBic(bic)` | Validated 8/11 char BIC |
| `format#SwiftNarrative(text, charsPerLine)` | Auto-wrapped lines at specified width |
| `format#SwiftReference(ref)` | Validated reference (slash rules, max 16 chars) |

### 4.3 Message Persistence & Lifecycle
Every generated message persisted as `SwiftMessage` record:
- Fields: `instrumentId`, `messageType`, `messageContent`, `generatedDate`, `messageStatusId`

**DRAFT/ACTIVE Lifecycle:**

| From Status | To Status | Trigger | Allowed? |
| :--- | :--- | :--- | :--- |
| (none) | DRAFT | Manual generation pre-approval | Yes |
| DRAFT | DRAFT | Re-generation pre-approval (replace) | Yes |
| DRAFT | ACTIVE | Transaction approved (auto-generate replaces DRAFT) | Yes |
| (none) | ACTIVE | Transaction approved (auto-generate, no prior DRAFT) | Yes |
| ACTIVE | ACTIVE | Re-generation attempt | **Blocked** |
| ACTIVE | (any) | Any modification | **Blocked** |

**Rules:**
- Pre-approval (`transactionStatusId ≠ TRANS_APPROVED`): users may manually generate DRAFT messages for preview. Re-generation replaces existing DRAFT.
- Post-approval (`transactionStatusId = TRANS_APPROVED`): system auto-generates ACTIVE message. Once ACTIVE exists, regeneration is **blocked** (immutable).
- If auto-generation fails, manual generation allowed only if no ACTIVE message exists for this `instrumentId` + `messageType` combination.

### 4.4 MT Message Generation Summary Matrix

| MT Type | Lifecycle Trigger | Process Feature | Primary Entity Data Source |
| :--- | :--- | :--- | :--- |
| MT700 | LC Issuance authorized | Feature 2.1 (Issuance) | `ImportLetterOfCredit`, `TradeInstrument`, `TradeInstrumentParty` |
| MT701 | Auto (from MT700 if overflow) | Feature 2.1 (Issuance) | Same as MT700 — overflow from Tags 45A/46A/47A |
| MT707 | Amendment authorized | Feature 2.2 (Amendments) | `ImportLcAmendment`, parent `ImportLetterOfCredit` |
| MT750 | Presentation marked Discrepant | Feature 2.3 (Presentation) | `TradeDocumentPresentation`, `PresentationDiscrepancy` |
| MT734 | Presentation formally Refused | Feature 2.3 (Presentation) | `TradeDocumentPresentation`, `PresentationDiscrepancy` |
| MT752 | Discrepancy Waived → Accepted | Feature 2.3 (Presentation) | `TradeDocumentPresentation` |
| MT732 | Usance presentation Accepted | Feature 2.3 (Presentation) | `TradeDocumentPresentation`, parent LC maturity |
| MT799 | Early cancellation initiated | Feature 2.6 (Cancellations) | `TradeInstrument`, Advising Bank ref |
| MT202 | Settlement (bank-to-bank) | Feature 2.4 (Settlement) | `ImportLcSettlement`, `TradeInstrumentParty` |
| MT103 | Settlement (customer direct) | Feature 2.4 (Settlement) | `ImportLcSettlement`, `TradeParty` |

### 4.5 User Stories

### US-SWG-01: Message Immutability After Dispatch
**As a** Trade Operations system,
**I want** ACTIVE SWIFT messages to be immutable after transaction approval,
**So that** the bank has an audit-proof record of dispatched communications.

### US-SWG-02: Draft Message Preview
**As a** Trade Operations Maker,
**I want** to preview and generate SWIFT messages as DRAFT before transaction approval,
**So that** I can verify message content during data entry.

#### 4.6 Grounding Info
*   **Message Builder:** From **FR-SWG-01**, **FR-SWG-02** (swift-generation-brd.md).
*   **Layer 2 Validation:** From **FR-SWG-03** (swift-generation-brd.md).
*   **Message Lifecycle:** From **FR-SWG-15** (swift-generation-brd.md).
*   **Message Persistence:** From **FR-SWG-04** (swift-generation-brd.md).

---

## 5. Feature 5: Party Management for Import LC

### 5.1 Party-Role Junction Pattern
Import LC uses the `TradeInstrumentParty` junction entity for all party assignments. Flat BIC/name fields have been removed from `ImportLetterOfCredit`, `TradeInstrument`, and `TradeDocumentPresentation`.

### 5.2 Import LC Party Roles

| Role ID | Description | Party Type | Primary SWIFT Tag | Account Number Rule |
| :--- | :--- | :--- | :--- | :--- |
| `TP_APPLICANT` | Applicant (Ordering Customer) | Commercial | Tag 50 | Optional |
| `TP_BENEFICIARY` | Beneficiary | Commercial | Tag 59 | Optional (MT700), **Mandatory** (MT103 Tag 59a) |
| `TP_ADVISING_BANK` | Advising Bank | Bank | Header Block 2 (Receiver) | N/A |
| `TP_APPLICANT_BANK` | Applicant Bank | Bank | Tag 51a | Forbidden in MT707 (flat text only) |
| `TP_REIMBURSING_BANK` | Reimbursing Bank | Bank | Tag 53a | Optional |
| `TP_ADVISE_THRU_BANK` | Advise Through Bank | Bank | Tag 57a | Optional |
| `TP_CONFIRMING_BANK` | Confirming Bank | Bank | Tag 58a | Optional |
| `TP_NEGOTIATING_BANK` | Negotiating / Available With Bank | Bank | Tag 41a | **Strictly Forbidden** |
| `TP_DRAWEE_BANK` | Drawee Bank | Bank | Tag 42a | **Strictly Forbidden** |
| `TP_PRESENTING_BANK` | Presenting Bank | Bank | MT750/734/752, MT202 Tag 58a | Optional |
| `TP_INTERMEDIARY_BANK` | Intermediary Bank (Settlement) | Bank | Tag 56a (MT202/MT103) | Optional |
| `TP_SENDERS_CORRESPONDENT` | Sender's Correspondent | Bank | Tag 53a (MT202) | Highly Recommended |
| `TP_RECEIVERS_CORRESPONDENT` | Receiver's Correspondent | Bank | Tag 54a (MT202) | Optional |

### 5.3 SWIFT Party Validation Rules

| Rule ID | Rule | Applies To |
| :--- | :--- | :--- |
| PTY-SWV-01 | Party name (`partyName`): X charset, max 4×35 lines | All party tags (50, 59, 51a, 53a, 57a, etc.) |
| PTY-SWV-02 | Party address (`registeredAddress`): X charset, max 4×35 lines | All party tags |
| PTY-SWV-03 | BIC format: exactly 8 or 11 alphanumeric characters | All bank party BICs |
| PTY-SWV-04 | Account number format: prefixed with `/` when included in party block | Tags 50, 59, 59a |
| PTY-SWV-05 | Account number **forbidden** on Tag 41a (Available With) — even if bank has `accountNumber` | Tag 41a / `TP_NEGOTIATING_BANK` |
| PTY-SWV-06 | Account number **forbidden** on Tag 42a (Drawee) | Tag 42a / `TP_DRAWEE_BANK` |
| PTY-SWV-07 | Account number **mandatory** on Tag 59a (MT103 Beneficiary) | Tag 59a / `TP_BENEFICIARY` |
| PTY-SWV-08 | If BIC unavailable, use Option D (Name + Address 4×35) for bank tags | Tags 53D, 57D, 58D |

### 5.4 Bank Eligibility Rules

| Role | Validation Rule | Error Message |
| :--- | :--- | :--- |
| `TP_ADVISING_BANK` | `hasActiveRMA = Y` (mandatory, no exceptions) | "Advising Bank must have active RMA with the Issuing Bank." |
| `TP_ADVISE_THRU_BANK` | No RMA check required | N/A — RMA between Advising and Advise Through is outside Issuing Bank scope |
| `TP_REIMBURSING_BANK` | `nostroAccountRef` is not null | "Cannot designate as Reimbursing Bank: No active Nostro account found." |
| `TP_CONFIRMING_BANK` | `fiLimitAvailable ≥ instrument max liability` | "Confirming Bank's FI limit is insufficient for instrument liability." |
| All bank roles | `kycStatus = Active`, `sanctionsStatus = SANCTION_CLEAR` | Per compliance rules |

### 5.5 "Available With" — Explicit User Choice
- Maker explicitly selects "ANY BANK" or a specific negotiating bank
- If specific bank: `availableWithEnumId = AVAIL_SPECIFIC_BANK` + `TP_NEGOTIATING_BANK` junction record
- If ANY BANK: `availableWithEnumId = AVAIL_ANY_BANK` + remove any existing `TP_NEGOTIATING_BANK` junction record
- The Maker can switch between these choices at any time before submission
- **Account numbers forbidden on Tag 41a** — SWIFT builder must suppress even if negotiating bank has `accountNumber`

### 5.6 User Stories
(Refer to US-TP-01 through US-TP-06 in common-consolidated-brd.md Feature 1)

#### 5.7 Grounding Info
*   **Junction Pattern:** From **FR-TP-03** (tradeparty-refactor-brd.md).
*   **Role Enumeration:** From **FR-TP-04** (tradeparty-refactor-brd.md).
*   **Field Removals:** From **FR-TP-06**, **FR-TP-07**, **FR-TP-08** (tradeparty-refactor-brd.md).
*   **Available With Choice:** From **FR-TP-09** (tradeparty-refactor-brd.md).
*   **Account Number Rules:** From **FR-TP-04 Tag Coverage Verification** — tag-specific account number requirements.
*   **SWIFT Party Validation:** From **FR-SWV-05** (BIC), **FR-SWV-06** (4×35 lines), **FR-TP-04** (account number per tag).

---

## 6. Feature 6: Product Configuration & Validation

### 6.1 Import LC Product-Specific Validation Rules

| Rule ID | Rule | Enforcement Point |
| :--- | :--- | :--- |
| IMP-VAL-01 | Tolerance Limit Check: total drawn amount ≤ LC amount + positive tolerance % | Document Presentation lodgement (Feature 2.3) |
| IMP-VAL-02 | Expiry Date Rule: prevent new presentation if presentation date > LC Expiry Date | Document Presentation lodgement (Feature 2.3) |
| IMP-VAL-03 | Date Sequence: Expiry Date ≥ Issue Date | Issuance data entry (Feature 2.1) |
| IMP-VAL-04 | Latest Shipment Date ≤ Expiry Date | Issuance data entry (Feature 2.1) |
| IMP-VAL-05 | Claim Currency must match LC currency | Document Presentation lodgement (Feature 2.3) |
| IMP-VAL-06 | Available With Enum explicitly chosen (ANY BANK or SPECIFIC BANK) | Issuance data entry (Feature 2.1) |

### 6.2 Revolving LC Rules
- If LC designated as "Revolving" (per `TradeProduct.Allow Revolving`), system automatically reinstates original LC amount upon settlement of a drawing.
- No manual amendment required.
- Continues until maximum cumulative limit reached.

### 6.3 Regulatory Reporting
- For transactions within Vietnam: system auto-flags and categorizes import goods codes for foreign exchange (FX) outflow reporting to State Bank.

#### 6.4 Grounding Info
*   **Validation Rules:** From **REQ-IMP-04** (baseline).
*   **Revolving LC:** From **REQ-IMP-04** (baseline), `Allow Revolving` flag from **REQ-COM-PRD-01** (common module).
*   **Regulatory Reporting:** From **REQ-IMP-04** (baseline).

---

## 7. Traceability Matrix

### User Stories → Features

| User Story | Feature(s) |
| :--- | :--- |
| US-LC-01, US-LC-02, US-LC-03 | Feature 1: LC Lifecycle States |
| US-ISS-01, US-ISS-02, US-ISS-03 | Feature 2.1: LC Issuance |
| US-AMD-01, US-AMD-02, US-AMD-03 | Feature 2.2: Amendments |
| US-PRE-01, US-PRE-02 | Feature 2.3: Document Presentation |
| US-STL-01, US-STL-02, US-STL-03 | Feature 2.4: Settlement |
| US-SG-01, US-SG-02 | Feature 2.5: Shipping Guarantees |
| US-CAN-01, US-CAN-02 | Feature 2.6: Cancellations |
| US-SWV-01, US-SWV-02 | Feature 3: SWIFT Validation (Common) |
| US-SWG-01, US-SWG-02 | Feature 4: SWIFT Generation (Common) |
| US-TP-01 to US-TP-06 | Feature 5: Party Management (see common-consolidated-brd.md) |

### SWIFT Message → Process Feature & Lifecycle Trigger

| MT Message | Lifecycle Trigger | Process Feature | Key Validation Rules |
| :--- | :--- | :--- | :--- |
| MT700 | LC Issuance authorized | Feature 2.1 | ISS-SWV-01 to ISS-SWV-17 |
| MT701 | Auto (from MT700 overflow) | Feature 2.1 | Same as MT700 + auto-continuation logic |
| MT707 | Amendment authorized | Feature 2.2 | AMD-SWV-01 to AMD-SWV-06, Z charset |
| MT750 | Presentation marked Discrepant | Feature 2.3 | PRE-SWV-01 to PRE-SWV-07 |
| MT734 | Presentation formally Refused | Feature 2.3 | PRE-SWV-01 to PRE-SWV-07, Tag 77B required |
| MT752 | Discrepancy Waived → Accepted | Feature 2.3 | PRE-SWV-01 to PRE-SWV-07 |
| MT732 | Usance presentation Accepted | Feature 2.3 | Maturity date in Tag 32A (future date) |
| MT799 | Early cancellation initiated | Feature 2.6 | CAN-SWV-01 to CAN-SWV-03, Z charset |
| MT202 | Settlement (bank-to-bank) | Feature 2.4 | STL-SWV-01 to STL-SWV-08 |
| MT103 | Settlement (customer direct) | Feature 2.4 | STL-SWV-01 to STL-SWV-08, beneficiary account mandatory |

### BRD Sections → Source Documents

| Section | Primary Source | Modified By |
| :--- | :--- | :--- |
| Feature 1: LC Lifecycle States | import-lc-brd.md (REQ-IMP-02, DTL-00) | trade-transaction-tracking-brd.md |
| Feature 2.1: Issuance + SWIFT | import-lc-brd.md (REQ-IMP-SPEC-01) | swift-validation-brd.md, swift-gaps-brd.md, tradeparty-refactor-brd.md, swift-generation-brd.md |
| Feature 2.2: Amendments + SWIFT | import-lc-brd.md (REQ-IMP-SPEC-02) | swift-generation-brd.md, swift-gaps-brd.md |
| Feature 2.3: Presentation + SWIFT | import-lc-brd.md (REQ-IMP-SPEC-03) | swift-validation-brd.md, swift-gaps-brd.md, tradeparty-refactor-brd.md, swift-generation-brd.md |
| Feature 2.4: Settlement + SWIFT | import-lc-brd.md (REQ-IMP-SPEC-04) | swift-generation-brd.md |
| Feature 2.5: Shipping Guarantees | import-lc-brd.md (REQ-IMP-SPEC-05) | — |
| Feature 2.6: Cancellations + SWIFT | import-lc-brd.md (REQ-IMP-SPEC-06) | swift-generation-brd.md |
| Feature 3: SWIFT Validation (Common) | swift-validation-brd.md | swift-gaps-brd.md |
| Feature 4: SWIFT Generation (Common) | swift-generation-brd.md | swift-gaps-brd.md |
| Feature 5: Party Management | tradeparty-refactor-brd.md | swift-validation-brd.md (party field validation) |
| Feature 6: Product Config | import-lc-brd.md (REQ-IMP-04) | common-module-brd.md (REQ-COM-PRD-01) |

---

## 8. Dropped / Superseded Requirements

| Original Requirement | Status | Replacement |
| :--- | :--- | :--- |
| Flat `applicantPartyId`, `beneficiaryPartyId` on ImportLetterOfCredit | **Dropped** | `TradeInstrumentParty` junction (TP_APPLICANT, TP_BENEFICIARY) |
| Flat `advisingBankBic`, `availableWithBic`, `draweeBankBic` on ImportLetterOfCredit | **Dropped** | `TradeInstrumentParty` junction per role |
| Flat `reimbursingBankBic`, `reimbursingBankName`, `adviseThroughBankBic` on TradeInstrument | **Dropped** | `TradeInstrumentParty` junction (TP_REIMBURSING, TP_ADVISE_THRU) |
| Flat `beneficiaryName` on TradeInstrument | **Dropped** | `TradeParty.partyName` via TP_BENEFICIARY junction |
| Flat `presentingBankBic` on TradeDocumentPresentation | **Dropped** | `TradeInstrumentParty` junction (TP_PRESENTING_BANK) |
| Flat `partyRoleEnumId` on TradeParty | **Dropped** | Roles are per-instrument via junction |
| Flat `swiftBic` on TradeParty | **Dropped** | Moves to `TradePartyBank` extension entity |
| Manual SWIFT string concatenation | **Dropped** | `SwiftMessageBuilder` Groovy utility class |
| Direct instrument editing for authorized actions | **Dropped** | All modifications via `TradeTransaction` container |
