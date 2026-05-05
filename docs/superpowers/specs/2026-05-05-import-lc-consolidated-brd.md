# Consolidated Business Requirements Document (BRD)
**ABOUTME:**
Consolidated requirements for the Import Letter of Credit (LC) Module.
This document merges baseline requirements with SWIFT specs, transaction tracking, and TradeParty refactoring.

**Project Name:** Digital Trade Finance Platform
**Module:** Import Letter of Credit (LC)
**Document Version:** 1.0 (Consolidated)
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

**Acceptance Criteria:**
- System blocks transitions not defined in the State Transition Matrix.
- State changes are logged in the audit timeline.

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

#### D. Inputs Capture (Data Dictionary)

##### General
| Field Name | Req/Opt | Data Type | Validation Rules |
| :--- | :--- | :--- | :--- |
| `applicantPartyId` | Req | id | Via `TradeInstrumentParty` junction (role: TP_APPLICANT). KYC must be Active. |
| `beneficiaryPartyId` | Req | id | Via `TradeInstrumentParty` junction (role: TP_BENEFICIARY). |
| `advisingBankPartyId` | Opt | id | Via `TradeInstrumentParty` junction (role: TP_ADVISING_BANK). Must have BIC. |

##### Dates
| Field Name | Req/Opt | Data Type | Validation Rules |
| :--- | :--- | :--- | :--- |
| `issueDate` | Req | Date | Cannot be in the past. Defaults to current system business date. |
| `expiryDate` | Req | Date | Must be ≥ Issue Date. |
| `latestShipmentDate` | Opt | Date | Must be ≤ Expiry Date. Mutually exclusive with `shipmentPeriodText`. |
| `expiryPlace` | Req | text-medium | Max 29 chars, X charset. SWIFT Tag 31D. |

##### Financial
| Field Name | Req/Opt | Data Type | Validation Rules |
| :--- | :--- | :--- | :--- |
| `currencyUomId` | Req | id | 3-letter ISO Currency Code. |
| `amount` | Req | number-decimal | Must be > 0. Comma decimal separator for SWIFT. |
| `tolerancePositive` | Opt | number-integer | Max 100. Mutually exclusive with `maxCreditAmountFlag`. SWIFT Tag 39A. |
| `toleranceNegative` | Opt | number-integer | Max 100. Mutually exclusive with `maxCreditAmountFlag`. SWIFT Tag 39A. |
| `maxCreditAmountFlag` | Opt | text-indicator | Y/N. Mutually exclusive with tolerance. SWIFT Tag 39B. |
| `additionalAmountsText` | Opt | text-long | Max 4×35, X charset. SWIFT Tag 39C. |

##### Terms
| Field Name | Req/Opt | Data Type | Validation Rules |
| :--- | :--- | :--- | :--- |
| `lcTypeEnumId` | Req | id | IRREVOCABLE, IRREVOCABLE_TRANSFERABLE. SWIFT Tag 40A. |
| `tenorTypeEnumId` | Req | id | SIGHT, USANCE, ACCEPTANCE, NEGOTIATION, DEF_PAYMENT, MIXED. |
| `usanceDays` | Cond | number-integer | Required if tenor ≠ SIGHT. |
| `usanceBaseDate` | Cond | text-medium | Required if tenor ≠ SIGHT. Max 35 chars, X charset. SWIFT Tag 42C. |
| `availableWithEnumId` | Req | id | AVAIL_ANY_BANK or AVAIL_SPECIFIC_BANK. |
| `availableWithBic` | Cond | text-short | Required if AVAIL_SPECIFIC_BANK. Valid 8/11 char BIC. SWIFT Tag 41A. |
| `availableByEnumId` | Req | id | BY_PAYMENT, BY_ACCEPTANCE, BY_NEGOTIATION, BY_DEF_PAYMENT. SWIFT Tag 41a. |
| `mixedPaymentDetails` | Cond | text-long | Max 4×35. Required if tenor = MIXED. SWIFT Tag 42M. |
| `deferredPaymentDetails` | Cond | text-long | Max 4×35. Required if tenor = DEF_PAYMENT/NEGOTIATION. SWIFT Tag 42P. |
| `draweeBic` | Opt | text-short | Valid 8/11 char BIC. SWIFT Tag 42A. |

##### Shipping
| Field Name | Req/Opt | Data Type | Validation Rules |
| :--- | :--- | :--- | :--- |
| `partialShipmentEnumId` | Req | id | ALLOWED, NOT_ALLOWED, CONDITIONAL. SWIFT Tag 43P. |
| `transhipmentEnumId` | Req | id | ALLOWED, NOT_ALLOWED, CONDITIONAL. SWIFT Tag 43T. |
| `portOfLoading` | Opt | text-medium | Max 65 chars, X charset. SWIFT Tag 44E. |
| `portOfDischarge` | Opt | text-medium | Max 65 chars, X charset. SWIFT Tag 44F. |
| `receiptPlace` | Opt | text-medium | Max 65 chars, X charset. SWIFT Tag 44A. |
| `finalDeliveryPlace` | Opt | text-medium | Max 65 chars, X charset. SWIFT Tag 44B. |
| `shipmentPeriodText` | Opt | text-medium | Max 65 chars, X charset. Mutually exclusive with `latestShipmentDate`. SWIFT Tag 44D. |

##### Narratives
| Field Name | Req/Opt | Data Type | Validation Rules |
| :--- | :--- | :--- | :--- |
| `goodsDescription` | Req | text-very-long | Max 100×65, X charset. Auto-wrap at 65 chars/line. SWIFT Tag 45A. |
| `documentsRequired` | Req | text-very-long | Max 100×65, X charset. Auto-wrap at 65 chars/line. SWIFT Tag 46A. |
| `additionalConditions` | Opt | text-very-long | Max 100×65, X charset. Auto-wrap at 65 chars/line. SWIFT Tag 47A. |
| `chargeAllocation` | Req | Enum | ALL_APPLICANT, ALL_BENEFICIARY, SHARED. |
| `chargeAllocationText` | Opt | text-long | Max 6×35, X charset. Detailed charges text. SWIFT Tag 71D. |
| `bankToBankInstructions` | Opt | text-very-long | Max 12×65, X charset. SWIFT Tag 78. |
| `presentationPeriodDays` | Opt | number-integer | Positive integer. Days after shipment for doc presentation. SWIFT Tag 48. |
| `confirmationEnumId` | Req | id | CONFIRM, MAY_ADD, WITHOUT. SWIFT Tag 49. |

##### Administrative
| Field Name | Req/Opt | Data Type | Validation Rules |
| :--- | :--- | :--- | :--- |
| `preAdviceRef` | Opt | text-short | Max 16 chars, X charset, slash rules. SWIFT Tag 23. |
| `senderToReceiverInfo` | Opt | text-long | Max 6×35, X charset. SWIFT Tag 72Z. |

#### E. Display / Computed Data

| Field Name | Calculation Formula |
| :--- | :--- |
| **Base Equivalent Amount** | `amount × Current System Exchange Rate` (LC Currency → Local Currency). Recalculated before submit. |
| **Maximum Liability Amount** | `amount + (amount × tolerancePositive / 100)`. True risk exposure. |
| **Available Facility Limit** | Fetched live from Core Banking: `Total Limit - Currently Utilized`. |
| **Limit Check Status** | `true` if `Available Facility Limit ≥ Maximum Liability Amount`. Blocks submission if false. |

#### F. Post-Submit Processing
Upon Checker authorization:
1. **Facility Earmark:** Synchronous call to Core Banking to deduct Maximum Liability Amount.
2. **Cash Margin Hold:** If insufficient credit line, hold on deposit account for margin percentage.
3. **Fee Deduction:** Issuance commission per bank tariff matrix (e.g., 0.125% per quarter).
4. **Entity Creation:** `TradeInstrument` and `ImportLetterOfCredit` records committed.
5. **SWIFT Generation:** MT 700 generated and dispatched. `SwiftMessage` record saved with `SWIFT_MSG_ACTIVE`.

#### G. User Stories

### US-ISS-01: LC Issuance with Full Validation
**As a** Trade Operations Maker,
**I want** to enter all LC issuance data with inline SWIFT validation,
**So that** I can submit a compliant application that won't fail at SWIFT generation.

### US-ISS-02: Automatic Limit Earmarking on Issuance
**As a** Trade Operations system,
**I want** to automatically earmark the customer's facility limit upon Checker approval,
**So that** the bank's credit exposure is tracked in real-time.

#### H. Grounding Info
*   **Data Dictionary:** From **REQ-IMP-SPEC-01** (baseline), enhanced by **FR-ENT-01 to FR-ENT-11** (validation), **FR-ENT-21 to FR-ENT-27** (gaps).
*   **Party Junction:** From **FR-TP-03**, **FR-TP-07** (tradeparty-refactor) — replaces flat `applicantPartyId`, `beneficiaryPartyId`, `advisingBankBic`.
*   **Post-Submit:** From **REQ-IMP-SPEC-01 Section G** (baseline), with `SwiftMessage` persistence from **FR-SWG-04**.

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

#### D. Amendment Inputs (Delta Fields Only)

| Field Name | Req/Opt | Data Type | Validation Rules |
| :--- | :--- | :--- | :--- |
| `amendmentDate` | Req | Date | Cannot be in the past. SWIFT Tag 30. |
| `amountIncrease` | Opt | number-decimal | Mutually exclusive with `amountDecrease`. |
| `amountDecrease` | Opt | number-decimal | Cannot exceed current Available Balance. |
| `newTotalAmount` | Req (computed) | number-decimal | `original + increase - decrease`. SWIFT Tag 34B. |
| `newTolerancePositive` | Opt | number-integer | Overwrites previous tolerance. |
| `newToleranceNegative` | Opt | number-integer | Overwrites previous tolerance. |
| `newExpiryDate` | Opt | Date | Must be ≥ current Business Date. SWIFT Tag 31E. |
| `newLatestShipmentDate` | Opt | Date | Must be ≤ New Expiry or Original Expiry. |
| `amendmentNarrative` | Cond | text-very-long | Max 35×50, **Z charset**. Required if no standard fields changed. SWIFT Tag 79. |
| `amendmentCharges` | Req | Enum | APPLICANT, BENEFICIARY. |
| `beneficiaryDecision` | Opt | Enum | PENDING, ACCEPTED, REJECTED. Defaults to PENDING. |

#### E. Display / Computed Data

| Field Name | Calculation Formula |
| :--- | :--- |
| **Amendment Number** | Auto-incremented (1, 2, 3...) per parent LC. SWIFT Tag 26E. |
| **New Total LC Amount** | `Original Amount + Increase - Decrease`. |
| **New Maximum Liability** | `New Total Amount + (New Total Amount × New Tolerance Positive / 100)`. |
| **Limit Delta Required** | `New Maximum Liability - Original Maximum Liability`. Positive = earmark more; negative = release. |
| **Required Authority Tier** | Calculated by Maker/Checker Matrix using **New Maximum Liability** (not delta). |

#### F. Post-Submit Processing
1. **Facility Delta Update:** Earmark additional funds or release excess.
2. **Fee Application:** Amendment flat fees + additional issuance commission if amount increased/expiry extended.
3. **Amendment Record Commit:** Delta saved to database, linked to parent LC.
4. **SWIFT Generation:** MT 707 generated with **delta-only tags** (only changed fields).

#### G. User Stories

### US-AMD-01: Financial vs Non-Financial Amendment Classification
**As a** Trade Operations system,
**I want** to automatically classify amendments as financial or non-financial,
**So that** the correct Checker authority tier is applied.

### US-AMD-02: Beneficiary Consent Tracking
**As a** Trade Operations Maker,
**I want** to log the Beneficiary's acceptance or rejection of an amendment,
**So that** the amendment terms become legally binding only with consent.

#### H. Grounding Info
*   **Process:** From **REQ-IMP-SPEC-02** (baseline).
*   **Delta-Only Tags:** From **FR-SWG-07** (generation spec).
*   **Amendment Number:** From **FR-ENT-28** (gaps spec).
*   **Authority on New Total:** From **REQ-COM-AUTH-03** (common module).

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

#### D. Presentation Inputs

| Field Name | Req/Opt | Data Type | Validation Rules |
| :--- | :--- | :--- | :--- |
| `presentingBankPartyId` | Req | id | Via `TradeInstrumentParty` junction (role: TP_PRESENTING_BANK). |
| `presentationDate` | Req | Date | Cannot be in the future. SWIFT Tag 30/32A. |
| `presentingBankRef` | Opt | text-short | Max 16 chars, X charset, slash rules. SWIFT Tag 21. |
| `presentationRef` | Req | text-short | Max 16 chars, X charset, no leading/trailing `/` or `//`. |
| `claimAmount` | Req | number-decimal | Must be > 0. Comma decimal for SWIFT. |
| `claimCurrency` | Req | id | Must match parent LC currency. ISO 4217. |
| `discrepancyFound` | Req | Boolean | Determines routing (Clean vs Discrepant). |
| `discrepancyDetails` | Cond | Array | Required if discrepancyFound = true. ISBP codes + free text. |
| `documentDisposalEnumId` | Cond | id | HOLDING_DOCUMENTS, RETURNING_DOCUMENTS. Required when refusing. SWIFT Tag 77B. |
| `applicantDecision` | Opt | Enum | PENDING, WAIVED, REFUSED. Used only if discrepant. |
| `chargesDeducted` | Opt | text-long | Max 6×35, X charset. Discrepancy fee text. SWIFT Tag 73. |

##### Document Type Grid (per document received)
| Field Name | Req/Opt | Data Type |
| :--- | :--- | :--- |
| `documentType` | Req | String | Bill of Lading, Commercial Invoice, Packing List, Origin Certificate, etc. |
| `originalCount` | Req | number-integer | Number of original copies. |
| `copyCount` | Req | number-integer | Number of photocopies. |

#### E. Display / Computed Data

| Field Name | Calculation Formula |
| :--- | :--- |
| **Presentation Reference** | Auto-generated unique ID (e.g., PR-IMP-2026-001). |
| **Regulatory Deadline** | `presentationDate + 5 Banking Days`. UCP 600 hard-stop. |
| **Remaining LC Balance** | `Parent LC Amount - Total Previously Accepted Claims`. |
| **Overdrawn Status** | `true` if `claimAmount > (Remaining Balance + Tolerance %)`. |

#### F. Post-Submit Processing
1. **Limit Update:** Contingent liability → firm liability (or acceptance liability for Usance).
2. **SLA Tracking:** 5-day countdown timer stops.
3. **Discrepancy Fee:** Auto-calculated and deducted per LC terms.
4. **SWIFT Generation:** MT 750 (discrepancy advice), MT 734 (refusal), or MT 752 (waiver accepted) as applicable.

#### G. User Stories

### US-PRE-01: Document Lodgement & Examination
**As a** Trade Operations Maker,
**I want** to log received documents and record examination results with discrepancy codes,
**So that** the bank meets its UCP 600 examination obligations.

### US-PRE-02: Regulatory Deadline Enforcement
**As a** Trade Operations system,
**I want** to enforce the 5-banking-day examination window,
**So that** the bank avoids UCP 600 violations.

#### H. Grounding Info
*   **Process:** From **REQ-IMP-SPEC-03** (baseline).
*   **Entity Fields:** From **FR-ENT-19** (validation), **FR-ENT-30 to FR-ENT-33** (gaps).
*   **Party Junction:** From **FR-TP-08** — `presentingBankBic` replaced by `TP_PRESENTING_BANK` junction.
*   **SWIFT Generation:** From **FR-SWG-08** (MT750), **FR-SWG-09** (MT734), **FR-SWG-10** (MT752).

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

#### D. Settlement Inputs

| Field Name | Req/Opt | Data Type | Validation Rules |
| :--- | :--- | :--- | :--- |
| `valueDate` | Req | Date | Cannot be in the past. SWIFT Tag 32A. |
| `principalAmount` | Req | number-decimal | Must equal Accepted Claim Amount. Comma decimal for SWIFT. |
| `remittanceCurrency` | Req | id | Must match LC/Claim currency. ISO 4217. |
| `applicantDebitAccount` | Req | String | Valid, active CASA account belonging to Applicant. |
| `appliedMarginAmount` | Opt | number-decimal | Cash collateral to utilize (if taken during issuance). |
| `fxExchangeRate` | Cond | number-decimal | Required if Debit Account currency ≠ Remittance Currency. |
| `forwardContractRef` | Opt | text-short | Reference to pre-booked Treasury FX contract. |
| `chargesDetailEnumId` | Req | Enum | OUR, BEN, SHA. Defaults to LC terms. SWIFT Tag 71A. |

#### E. Display / Computed Data

| Field Name | Calculation Formula |
| :--- | :--- |
| **Total Debit Amount** | `(principalAmount × fxExchangeRate) - appliedMarginAmount + calculatedBankFees`. |
| **Nostro Account** | Auto-derived based on Remittance Currency (from `TradeConfig.NOSTRO_ACCOUNT_{CCY}`). |
| **Account Balance Check** | `true` if Applicant Debit Account has sufficient funds. Blocks submission if false. |

#### F. Post-Submit Processing
1. **Liability Reversal:** Reverses firm/acceptance liability for claim amount.
2. **Margin Release:** Releases margin block, transfers to settlement suspense.
3. **Core Debit:** Debits Total Debit Amount from Applicant's operating account.
4. **Parent LC Update:** Deducts settled amount. If remaining balance = 0 (within tolerance), LC → Closed.
5. **SWIFT Generation:** MT 202 (bank-to-bank) or MT 103 (customer direct).

#### G. User Stories

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

#### H. Grounding Info
*   **Process:** From **REQ-IMP-SPEC-04** (baseline).
*   **SWIFT Generation:** From **FR-SWG-13** (MT202), **FR-SWG-14** (MT103).
*   **Live FX Rate:** From **REQ-COM-FX-02** (common module) and common-consolidated-brd.md Feature 2.
*   **Settlement Roles:** From **FR-TP-04** — settlement-specific party roles added during this process.

---

### 2.5 Process — Shipping Guarantees (SG)

#### A. Related States
*   **Transaction State:** Draft (SG Issuance) → Pending Approval → Executed
*   **SG Sub-State:** Issued → Redeemed/Closed
*   **LC Business State Impact:** Parent LC remains Issued. Hard lock on utilized limit. Waiver lock flag set.

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

#### E. Display / Computed Data

| Field Name | Calculation Formula |
| :--- | :--- |
| **Mail Days Grace Period** | `LC Expiry Date + System Parameter (e.g., 15 Days)`. Hard-stop before auto-expiry. |
| **Total Limit to Release** | `cancelledAmount + (cancelledAmount × tolerancePositive / 100)`. |
| **Margin to Release** | Proportional cash collateral tied to cancelled balance. |

#### F. Post-Submit Processing
1. **Limit Reversal:** Restore limits to customer's Available Earmark via Core Banking.
2. **Margin Release:** Remove hold on deposit account if cash-backed.
3. **State Lock:** LC → Closed. Block new presentations, amendments, or fees.

#### G. User Stories

### US-CAN-01: Auto-Expiry with Mail Days Grace
**As a** Trade Operations system,
**I want** to auto-close LCs after Expiry Date plus a configurable grace period,
**So that** overseas documents in transit are accommodated before releasing limits.

### US-CAN-02: Early Mutual Cancellation with Beneficiary Consent
**As a** Trade Operations Maker,
**I want** to request early cancellation and track Beneficiary consent via SWIFT,
**So that** the LC is cancelled only with all parties' agreement.

#### H. Grounding Info
*   **Process:** From **REQ-IMP-SPEC-06** (baseline).
*   **Mail Days:** From product configuration `Auto-Expiry days` in REQ-COM-PRD-01 (common module).

---

## 3. Feature 3: SWIFT Validation Infrastructure

### 3.1 Final Requirements in Detail

#### A. SWIFT Character Sets
**X Character Set** (majority of SWIFT fields):
- Allowed: `A-Z a-z 0-9 / - ? : ( ) . , ' +` and spaces
- Blocked: `@ & _ # ! % ^ * { } [ ] | \ " ; < > ~` and all other special characters

**Z Character Set** (Tags 79 in MT707, MT799):
- Extends X charset with: `@ # = ! " % & * ; < > _`

#### B. Validation Rules Summary

| Rule ID | Rule | Applies To |
| :--- | :--- | :--- |
| FR-SWV-01 | X Character Set validation | All X charset fields |
| FR-SWV-02 | Reference slash rules (no leading/trailing `/`, no `//`) | Tags 20, 21, 23 |
| FR-SWV-03 | Amount format: comma decimal, max 15 digits, positive | Tags 32B, 33B, 34B |
| FR-SWV-04 | Date format: YYMMDD | All date tags |
| FR-SWV-05 | BIC format: exactly 8 or 11 alphanumeric characters | All BIC fields |
| FR-SWV-06 | Line format: NxM (4×35, 6×35, 12×65, 35×50, 70×50, 100×65) | Multi-line narrative fields |
| FR-SWV-07 | Mutual exclusion: 39A/39B, 44C/44D, 53A/53D, 57A/57D | Tolerance/Max Credit, Shipment Date/Period, Bank BIC/Name |
| FR-SWV-08 | Conditional requirements: tenor-dependent, discrepancy-dependent | Usance fields, Mixed Payment, Deferred Payment, Discrepancy details |

#### C. Validation Service API
**Service:** `validate#SwiftFields`
- Accepts entity type and entity ID
- Validates ALL SWIFT-bound fields on that entity
- Returns field-level error messages with specific violation details
- Can be called on save (incremental) and on submit (comprehensive)
- Supports all SWIFT-bound entities: `ImportLetterOfCredit`, `TradeInstrument`, `TradeParty`, `ImportLcAmendment`, `TradeDocumentPresentation`, `PresentationDiscrepancy`, `ImportLcSettlement`

#### D. User Stories

### US-SWV-01: Inline SWIFT Character Validation
**As a** Trade Operations Maker,
**I want** the system to immediately flag invalid SWIFT characters during data entry,
**So that** I can fix formatting issues before submission instead of having transactions rejected at generation time.

### US-SWV-02: Pre-Submission Comprehensive Validation
**As a** Trade Operations Maker,
**I want** a complete SWIFT compliance check before submitting for approval,
**So that** I receive all issues in one pass and can fix them efficiently.

#### E. Grounding Info
*   **Character Sets:** From **FR-SWV-01** (swift-validation-brd.md).
*   **Validation Rules:** From **FR-SWV-01 through FR-SWV-09** (swift-validation-brd.md).
*   **Validation Service:** From **FR-SWV-09** (swift-validation-brd.md).

---

## 4. Feature 4: SWIFT Message Generation

### 4.1 Final Requirements in Detail

#### A. SWIFT Message Builder
Reusable Groovy utility class (`SwiftMessageBuilder`) that:
- Constructs SWIFT Basic Header Block 1 (sender BIC from `TradeConfig.ISSUING_BANK_BIC`)
- Constructs Application Header Block 2 (message type, receiver BIC)
- Constructs User Header Block 3 (optional)
- Constructs Text Block 4 (tag content)
- Handles tag formatting: date→YYMMDD, amount→comma-decimal, multi-line→NxM wrapping
- `build()` method returns assembled SWIFT message text

#### B. Tag Formatting Services
| Service | Output Format |
| :--- | :--- |
| `format#SwiftDate(date)` | YYMMDD |
| `format#SwiftAmount(currency, amount)` | CCY + comma-decimal (e.g., `USD50000,00`) |
| `format#SwiftParty(name, address, accountNumber)` | 4×35 formatted party block |
| `format#SwiftBic(bic)` | Validated 8/11 char BIC |
| `format#SwiftNarrative(text, charsPerLine)` | Auto-wrapped lines at specified width |
| `format#SwiftReference(ref)` | Validated reference (slash rules, max 16 chars) |

#### C. Layer 2 Generation-Time Validation
Before assembling any message:
1. Call `validate#SwiftFields` on all source entities
2. If errors: abort generation, return errors, log warning
3. If data passes but contains unexpected characters: auto-convert (e.g., `&` → `AND`) and log warning
4. All Layer 2 conversions logged with field name, original value, and converted value

#### D. Message Persistence & Lifecycle
Every generated message persisted as `SwiftMessage` record:
- `instrumentId`, `messageType`, `messageContent`, `generatedDate`, `messageStatusId`

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
- Pre-approval: users may manually generate DRAFT messages for preview. Re-generation replaces existing DRAFT.
- Post-approval: system auto-generates ACTIVE message. Once ACTIVE exists, regeneration is **blocked** (immutable).
- If auto-generation fails, manual generation allowed only if no ACTIVE message exists.

#### E. MT Message Generation Matrix

| MT Type | Trigger | Calling Service |
| :--- | :--- | :--- |
| MT700 | LC Issuance authorized | `execute#IssuancePostAuth` |
| MT701 | Auto (from MT700 if overflow) | `generate#Mt700` (internal) |
| MT707 | Amendment authorized | `authorize#Amendment` (post-auth) |
| MT750 | Presentation marked Discrepant | `authorize#Presentation` |
| MT734 | Presentation formally Refused | `update#PresentationRefusal` |
| MT752 | Discrepancy Waived → Accepted | `update#PresentationWaiver` |
| MT732 | Usance presentation Accepted | `authorize#Presentation` (usance path) |
| MT799 | Early cancellation initiated | `update#Cancellation` (early mutual) |
| MT202 | Settlement authorized (bank-to-bank) | `settle#Presentation` |
| MT103 | Settlement authorized (customer direct) | `settle#Presentation` |

#### F. MT700 Tag Mapping (Complete)

| Block | Tags | M/O | Data Source |
| :--- | :--- | :--- | :--- |
| **A: Header** | 27 (Sequence), 40A (Form of DC), 20 (Credit No.), 31C (Issue Date), 31D (Expiry Date+Place) | All M | `TradeInstrument`, `ImportLetterOfCredit` |
| **B: Parties** | 50 (Applicant), 59/59A (Beneficiary), 51A (Applicant Bank) | 50/59 M, 51A O | `TradeInstrumentParty` junction (TP_APPLICANT, TP_BENEFICIARY, TP_APPLICANT_BANK) |
| **C: Financials** | 32B (Amount), 39A (Tolerance), 39B (Max Credit), 39C (Additional Amounts), 41a (Available With), 42C (Drafts at), 42A (Drawee), 42M (Mixed), 42P (Deferred) | 32B/41a M, rest O | `TradeInstrument`, `ImportLetterOfCredit` |
| **D: Shipping** | 43P (Partial), 43T (Transhipment), 44A (Receipt Place), 44B (Final Dest), 44C (Latest Shipment), 44D (Shipment Period), 44E (Port of Loading), 44F (Port of Discharge) | All O | `ImportLetterOfCredit` |
| **E: Narratives** | 45A (Goods), 46A (Documents), 47A (Conditions), 71D (Charges), 48 (Presentation Period), 49 (Confirmation) | 45A/46A/49 M, rest O | `ImportLetterOfCredit` |
| **F: Routing** | 53a (Reimbursing Bank), 57a (Advise Through Bank) | All O | `TradeInstrumentParty` junction (TP_REIMBURSING, TP_ADVISE_THRU) |
| **G: Admin** | 23 (Pre-Advice Ref), 72Z (Sender Info), 78 (Bank Instructions) | All O | `TradeInstrument`, `ImportLetterOfCredit` |

**MT701 Auto-Continuation:** Generated automatically when Tags 45A, 46A, or 47A exceed 100×65 chars. Contains overflow in Tags 45B, 46B, 47B. Sequence Tag 27 updated to `2/N`.

#### G. User Stories

### US-SWG-01: Correct MT700 Generation on LC Issuance
**As a** Trade Operations system,
**I want** to generate a standards-compliant MT700 from entity data upon Checker authorization,
**So that** the Advising Bank receives a correctly formatted documentary credit notification.

### US-SWG-02: Message Immutability After Dispatch
**As a** Trade Operations system,
**I want** ACTIVE SWIFT messages to be immutable after transaction approval,
**So that** the bank has an audit-proof record of dispatched communications.

#### H. Grounding Info
*   **Message Builder:** From **FR-SWG-01**, **FR-SWG-02** (swift-generation-brd.md).
*   **Layer 2 Validation:** From **FR-SWG-03** (swift-generation-brd.md).
*   **Message Lifecycle:** From **FR-SWG-15** (swift-generation-brd.md).
*   **MT700 Tag Mapping:** From **FR-SWG-05** (generation), **FR-SGC-04** (gaps), **REQ-IMP-SWIFT-02/03** (baseline).
*   **MT701 Continuation:** From **FR-SWG-06** (generation spec).
*   **Party Data Source:** From **FR-TP-03** — all party data read from `TradeInstrumentParty` junction, not flat fields.

---

## 5. Feature 5: Product Configuration & Validation

### 5.1 Final Requirements in Detail

#### A. Import LC Product-Specific Validation Rules

| Rule ID | Rule | Enforcement Point |
| :--- | :--- | :--- |
| IMP-VAL-01 | Tolerance Limit Check: total drawn amount ≤ LC amount + positive tolerance % | Document Presentation lodgement |
| IMP-VAL-02 | Expiry Date Rule: prevent new presentation if presentation date > LC Expiry Date | Document Presentation lodgement |
| IMP-VAL-03 | Date Sequence: Expiry Date ≥ Issue Date | Issuance data entry |
| IMP-VAL-04 | Latest Shipment Date ≤ Expiry Date | Issuance data entry |
| IMP-VAL-05 | Claim Currency must match LC currency | Document Presentation lodgement |
| IMP-VAL-06 | Available With Enum explicitly chosen (ANY BANK or SPECIFIC BANK) | Issuance data entry |
| IMP-VAL-07 | Tolerance (39A) and Max Credit (39B) mutually exclusive | Issuance data entry |
| IMP-VAL-08 | Shipment Date (44C) and Shipment Period Text (44D) mutually exclusive | Issuance data entry |

#### B. Revolving LC Rules
- If LC designated as "Revolving" (per `TradeProduct.Allow Revolving`), system automatically reinstates original LC amount upon settlement of a drawing.
- No manual amendment required.
- Continues until maximum cumulative limit reached.

#### C. Regulatory Reporting
- For transactions within Vietnam: system auto-flags and categorizes import goods codes for foreign exchange (FX) outflow reporting to State Bank.

#### D. Grounding Info
*   **Validation Rules:** From **REQ-IMP-04** (baseline).
*   **Revolving LC:** From **REQ-IMP-04** (baseline), `Allow Revolving` flag from **REQ-COM-PRD-01** (common module).
*   **Regulatory Reporting:** From **REQ-IMP-04** (baseline).

---

## 6. Feature 6: Party Management for Import LC

### 6.1 Final Requirements in Detail

#### A. Party-Role Junction Pattern (per tradeparty-refactor-brd.md)
Import LC uses the `TradeInstrumentParty` junction entity for all party assignments. Flat BIC/name fields have been removed from `ImportLetterOfCredit`, `TradeInstrument`, and `TradeDocumentPresentation`.

#### B. Import LC Party Roles

| Role ID | Description | Party Type | SWIFT Tag |
| :--- | :--- | :--- | :--- |
| `TP_APPLICANT` | Applicant (Ordering Customer) | Commercial | Tag 50 |
| `TP_BENEFICIARY` | Beneficiary | Commercial | Tag 59 |
| `TP_ADVISING_BANK` | Advising Bank | Bank | Header Block 2 (Receiver) |
| `TP_APPLICANT_BANK` | Applicant Bank | Bank | Tag 51a |
| `TP_REIMBURSING_BANK` | Reimbursing Bank | Bank | Tag 53a |
| `TP_ADVISE_THRU_BANK` | Advise Through Bank | Bank | Tag 57a |
| `TP_CONFIRMING_BANK` | Confirming Bank | Bank | Tag 58a |
| `TP_NEGOTIATING_BANK` | Negotiating / Available With Bank | Bank | Tag 41a |
| `TP_DRAWEE_BANK` | Drawee Bank | Bank | Tag 42a |
| `TP_PRESENTING_BANK` | Presenting Bank | Bank | MT750/734/752 |
| `TP_INTERMEDIARY_BANK` | Intermediary Bank (Settlement) | Bank | Tag 56a (MT202) |
| `TP_SENDERS_CORRESPONDENT` | Sender's Correspondent | Bank | Tag 53a (MT202) |
| `TP_RECEIVERS_CORRESPONDENT` | Receiver's Correspondent | Bank | Tag 54a (MT202) |

#### C. Bank Eligibility Rules for Import LC

| Role | Validation Rule | Error Message |
| :--- | :--- | :--- |
| `TP_ADVISING_BANK` | `hasActiveRMA = Y` (mandatory, no exceptions) | "Advising Bank must have active RMA with the Issuing Bank." |
| `TP_REIMBURSING_BANK` | `nostroAccountRef` is not null | "Cannot designate as Reimbursing Bank: No active Nostro account found." |
| `TP_CONFIRMING_BANK` | `fiLimitAvailable ≥ instrument max liability` | "Confirming Bank's FI limit is insufficient for instrument liability." |
| All bank roles | `kycStatus = Active`, `sanctionsStatus = SANCTION_CLEAR` | Per compliance rules |

#### D. "Available With" — Explicit User Choice
- Maker explicitly selects "ANY BANK" or a specific negotiating bank
- If specific bank: `availableWithEnumId = AVAIL_SPECIFIC_BANK` + `TP_NEGOTIATING_BANK` junction record
- If ANY BANK: `availableWithEnumId = AVAIL_ANY_BANK` + remove `TP_NEGOTIATING_BANK` junction record
- **Account numbers forbidden on Tag 41a** — SWIFT builder must suppress even if negotiating bank has `accountNumber`

#### E. User Stories
(Refer to US-TP-01 through US-TP-06 in common-consolidated-brd.md Feature 1)

#### F. Grounding Info
*   **Junction Pattern:** From **FR-TP-03** (tradeparty-refactor-brd.md).
*   **Role Enumeration:** From **FR-TP-04** (tradeparty-refactor-brd.md).
*   **Field Removals:** From **FR-TP-07**, **FR-TP-08** (tradeparty-refactor-brd.md).
*   **Available With Choice:** From **FR-TP-09** (tradeparty-refactor-brd.md).

---

## 7. Feature 7: SWIFT Field — Entity Schema Summary

### 7.1 ImportLetterOfCredit — Complete Field Set

| Field | Type | SWIFT Tag | Notes |
| :--- | :--- | :--- | :--- |
| `lcTypeEnumId` | id | 40A | IRREVOCABLE, IRREVOCABLE_TRANSFERABLE |
| `availableWithEnumId` | id | 41a | AVAIL_ANY_BANK or AVAIL_SPECIFIC_BANK |
| `availableWithBic` | text-short | 41A | Valid 8/11 char BIC (if specific bank) |
| `availableByEnumId` | id | 41a | BY_PAYMENT, BY_ACCEPTANCE, BY_NEGOTIATION, BY_DEF_PAYMENT |
| `partialShipmentEnumId` | id | 43P | ALLOWED, NOT_ALLOWED, CONDITIONAL |
| `transhipmentEnumId` | id | 43T | ALLOWED, NOT_ALLOWED, CONDITIONAL |
| `confirmationEnumId` | id | 49 | CONFIRM, MAY_ADD, WITHOUT |
| `tenorTypeEnumId` | id | 42C | SIGHT, USANCE, ACCEPTANCE, NEGOTIATION, DEF_PAYMENT, MIXED |
| `usanceDays` | number-integer | 42C | Required if tenor ≠ SIGHT |
| `usanceBaseDate` | text-medium | 42C | Required if tenor ≠ SIGHT |
| `draweeBic` | text-short | 42A | Valid 8/11 char BIC |
| `mixedPaymentDetails` | text-long | 42M | Max 4×35. Required if tenor = MIXED |
| `deferredPaymentDetails` | text-long | 42P | Max 4×35. Required if tenor = DEF_PAYMENT/NEGOTIATION |
| `goodsDescription` | text-very-long | 45A | Max 100×65, X charset |
| `documentsRequired` | text-very-long | 46A | Max 100×65, X charset |
| `additionalConditions` | text-very-long | 47A | Max 100×65, X charset |
| `chargeAllocationText` | text-long | 71D | Max 6×35, X charset |
| `bankToBankInstructions` | text-very-long | 78 | Max 12×65, X charset |
| `presentationPeriodDays` | number-integer | 48 | Positive integer |
| `portOfLoading` | text-medium | 44E | Max 65 chars, X charset |
| `portOfDischarge` | text-medium | 44F | Max 65 chars, X charset |
| `receiptPlace` | text-medium | 44A | Max 65 chars, X charset |
| `finalDeliveryPlace` | text-medium | 44B | Max 65 chars, X charset |
| `shipmentPeriodText` | text-medium | 44D | Max 65 chars, X charset. Mutually exclusive with latestShipmentDate |
| `tolerancePositive` | number-integer | 39A | Mutually exclusive with maxCreditAmountFlag |
| `toleranceNegative` | number-integer | 39A | Mutually exclusive with maxCreditAmountFlag |
| `maxCreditAmountFlag` | text-indicator | 39B | Y/N. Mutually exclusive with tolerance |
| `additionalAmountsText` | text-long | 39C | Max 4×35, X charset |
| `expiryPlace` | text-medium | 31D | Max 29 chars, X charset |
| `chargeAllocation` | Enum | 71D | ALL_APPLICANT, ALL_BENEFICIARY, SHARED |

### 7.2 TradeInstrument — Complete Field Set

| Field | Type | SWIFT Tag | Notes |
| :--- | :--- | :--- | :--- |
| `transactionRef` | text-short | 20 | Max 16 chars, X charset, slash rules |
| `currencyUomId` | id | 32B | ISO 4217 |
| `amount` | number-decimal | 32B | Comma decimal for SWIFT |
| `issueDate` | date | 31C | Formatted YYMMDD |
| `expiryDate` | date | 31D | Formatted YYMMDD |
| `preAdviceRef` | text-short | 23 | Max 16 chars, X charset, slash rules |
| `senderToReceiverInfo` | text-long | 72Z | Max 6×35, X charset |

### 7.3 TradeDocumentPresentation — Complete Field Set

| Field | Type | SWIFT Tag | Notes |
| :--- | :--- | :--- | :--- |
| `presentationRef` | text-short | 20 | Max 16 chars, X charset, no leading/trailing `/` or `//` |
| `presentationDate` | date | 30/32A | Formatted YYMMDD |
| `presentingBankRef` | text-short | 21 | Max 16 chars, X charset, slash rules |
| `claimAmount` | number-decimal | 32B | Comma decimal for SWIFT |
| `claimCurrency` | id | 32B | Must match LC currency |
| `documentDisposalEnumId` | id | 77B | HOLDING_DOCUMENTS, RETURNING_DOCUMENTS |
| `chargesDeducted` | text-long | 73 | Max 6×35, X charset |

### 7.4 ImportLcAmendment — Complete Field Set

| Field | Type | SWIFT Tag | Notes |
| :--- | :--- | :--- | :--- |
| `amendmentNumber` | number-integer | 26E | Auto-incrementing |
| `amendmentDate` | date | 30 | Formatted YYMMDD |
| `amountIncrease` | number-decimal | 32B/33B | Positive or negative |
| `amountDecrease` | number-decimal | 32B/33B | Positive only |
| `newTotalAmount` | number-decimal | 34B | Comma decimal, 15 digits max |
| `newExpiryDate` | date | 31E | Formatted YYMMDD |
| `amendmentNarrative` | text-very-long | 79 | Max 35×50, **Z charset** |

### 7.5 Grounding Info
*   **All fields:** Consolidated from **REQ-IMP-SPEC-01 to 06** (baseline), **FR-ENT-01 to FR-ENT-19** (validation), **FR-ENT-21 to FR-ENT-33** (gaps), **FR-TP-06 to FR-TP-08** (party refactor).

---

## 8. Traceability Matrix

### User Stories → Features

| User Story | Feature(s) |
| :--- | :--- |
| US-LC-01, US-LC-02, US-LC-03 | Feature 1: LC Lifecycle States |
| US-ISS-01, US-ISS-02 | Feature 2.1: LC Issuance |
| US-AMD-01, US-AMD-02 | Feature 2.2: Amendments |
| US-PRE-01, US-PRE-02 | Feature 2.3: Document Presentation |
| US-STL-01, US-STL-02, US-STL-03 | Feature 2.4: Settlement |
| US-SG-01, US-SG-02 | Feature 2.5: Shipping Guarantees |
| US-CAN-01, US-CAN-02 | Feature 2.6: Cancellations |
| US-SWV-01, US-SWV-02 | Feature 3: SWIFT Validation |
| US-SWG-01, US-SWG-02 | Feature 4: SWIFT Generation |

### SWIFT Message → Lifecycle Trigger

| MT Message | Lifecycle Trigger | Feature Reference |
| :--- | :--- | :--- |
| MT700 | LC Issuance authorized | Feature 2.1 + Feature 4 |
| MT701 | Auto (from MT700 overflow) | Feature 4 |
| MT707 | Amendment authorized | Feature 2.2 + Feature 4 |
| MT750 | Presentation marked Discrepant | Feature 2.3 + Feature 4 |
| MT734 | Presentation formally Refused | Feature 2.3 + Feature 4 |
| MT752 | Discrepancy Waived → Accepted | Feature 2.3 + Feature 4 |
| MT732 | Usance presentation Accepted | Feature 2.4 + Feature 4 |
| MT799 | Early cancellation initiated | Feature 2.6 + Feature 4 |
| MT202 | Settlement authorized (bank-to-bank) | Feature 2.4 + Feature 4 |
| MT103 | Settlement authorized (customer direct) | Feature 2.4 + Feature 4 |

### BRD Sections → Source Documents

| Section | Primary Source | Modified By |
| :--- | :--- | :--- |
| Feature 1: LC Lifecycle States | import-lc-brd.md (REQ-IMP-02, DTL-00) | trade-transaction-tracking-brd.md |
| Feature 2.1: Issuance | import-lc-brd.md (REQ-IMP-SPEC-01) | swift-validation-brd.md, swift-gaps-brd.md, tradeparty-refactor-brd.md |
| Feature 2.2: Amendments | import-lc-brd.md (REQ-IMP-SPEC-02) | swift-generation-brd.md, swift-gaps-brd.md |
| Feature 2.3: Presentation | import-lc-brd.md (REQ-IMP-SPEC-03) | swift-validation-brd.md, swift-gaps-brd.md, tradeparty-refactor-brd.md |
| Feature 2.4: Settlement | import-lc-brd.md (REQ-IMP-SPEC-04) | swift-generation-brd.md |
| Feature 2.5: Shipping Guarantees | import-lc-brd.md (REQ-IMP-SPEC-05) | — |
| Feature 2.6: Cancellations | import-lc-brd.md (REQ-IMP-SPEC-06) | — |
| Feature 3: SWIFT Validation | swift-validation-brd.md | swift-gaps-brd.md |
| Feature 4: SWIFT Generation | swift-generation-brd.md | swift-gaps-brd.md |
| Feature 5: Product Config | import-lc-brd.md (REQ-IMP-04) | common-module-brd.md (REQ-COM-PRD-01) |
| Feature 6: Party Management | tradeparty-refactor-brd.md | — |
| Feature 7: Entity Schema | All BRDs combined | — |

---

## 9. Dropped / Superseded Requirements

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
