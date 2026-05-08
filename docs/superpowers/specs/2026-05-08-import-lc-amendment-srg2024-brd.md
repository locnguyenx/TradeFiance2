# Consolidated BRD Supplement: Revised Import LC Amendments (SRG 2024)

## 1. Revision Scope
This document fully replaces the previous "Import LC Amendments" section of the Core Business Processes. It aligns the system with the SWIFT SRG 2024 standards, enforcing strict structured data and a new "Smart Delta" UI, while formalizing the distinction between UCP 600 External Amendments and Internal Back-Office Amendments.

---

## 2. Amendment Scopes & Consent Rules

### A. External (UCP 600) Amendments
*   **Definition:** Any change to the legal text of the LC (Financial, Logistics, Narrative, or Charges). UCP 600 makes no distinction between "Financial" and "Non-Financial" - any change to the contract is an external amendment.
*   **Consent Rule:** Under UCP 600 Article 10, **ALL** external amendments strictly require Beneficiary Consent to become legally binding.
*   **System Action:** Generates an MT 707. The Master LC is *not* updated until consent is logged.

### B. Internal Bank Amendments
*   **Definition:** Changes to the bank's back-office tracking that do not alter the contract provided to the Beneficiary.
    *   *Cases:* Changing the Fee Settlement Account, Updating the Credit Facility ID, Changing Internal Margin, Re-assigning Relationship Manager.
*   **Consent Rule:** **None.** The Beneficiary is not a party to these changes.
*   **System Action:** Does *not* generate an MT 707. Overwrites the Master LC internal data immediately upon Checker approval.

---

## 3. Business Process Workflow (External Amendments)
1. **Initiation:** Operations locates the active LC and clicks "Initiate Amendment".
2. **System Population:** The system displays dedicated UI and populates related fields with existing data on the left.
3. **Data Entry ("Smart Delta" UI):** The Maker inputs only the fields that are changing on the right.
4. **Submission:** Maker submits. Status changes to *Pending Approval*. The required Checker tier is dynamically calculated based on the *new total liability*.
5. **Checker Review & Authorization:** Checker reviews and approves.
6. **Execution (Dispatched):** System deducts/releases limits (if increasing), applies amendment fees, and generates the MT 707 message.
7. **Beneficiary Consent Tracking:** The amendment remains structurally pending. The system auto-logs consent upon receiving an MT 730, or the Maker logs it manually.
8. **Final Merge:** Upon Checker approval of the logged consent, the amendment data officially overwrites the Master LC.

---

## 4. State Machine & Liability Impact Timing

**State 1: `Dispatched` (Checker Approves, MT 707 Sent)**
*   **SWIFT:** MT 707 generated and transmitted.
*   **Original LC Data:** **NO CHANGE.** Master LC remains in original state.
*   **Liability Rule (Increase):** If Amount/Tolerance *increases*, system **MUST** block/earmark the new limit from the core facility immediately. 
*   **Liability Rule (Decrease):** If Amount *decreases*, system **DOES NOT** release the limit yet.

**State 2: `Accepted` (Beneficiary Formally Agrees)**
*   **Trigger:** System receives MT 730, or Operations logs formal consent email. Checker approves the consent log.
*   **Original LC Data:** System executes "Merge." The amendment data officially overwrites the Master LC.
*   **Liability Rule (Decrease):** Only at this moment does the system call the Core Banking API to release excess limits.

**State 3: `Rejected` (Beneficiary Refuses)**
*   **Trigger:** Operations logs rejection notice.
*   **Original LC Data:** Remains untouched. Amendment record flagged as Dead.
*   **Liability Rule (Increase):** Extra limit blocked at `Dispatched` is immediately un-earmarked and released.

**State 4: `Internal Executed` (Internal Scope Amendments Only)**
*   **Trigger:** Checker approves an internal-only change.
*   **System Action:** Bypasses SWIFT. Master LC internal tags are overwritten immediately. Limits and routing update in real-time.

---

## 5. Inputs Capture & UI Behavior (Smart Delta)

The system displays the original LC values as read-only context on the left, and provides input fields for the new/amended values on the right.

### A. The "Track Changes" Narrative Engine
Users do not type SWIFT codewords. The UI provides a structured input block for every narrative field (Tags 45B, 46B, 47B, 49M, 49N):
*   **Action Selector:** Dropdown for `Add`, `Delete`, or `Replace All`.
*   **Input Area:** Text box for plain text.
*   **Payload Injection:** The system concatenates (e.g., `/ADD/Certificate required.`) during generation.

### B. Financials & Dates Delta UI
*   **New Expiry Date (31D):** Date $\ge$ Current Date.
*   **Amount Increase (32B):** Decimal $>$ 0.
*   **Amount Decrease (33B):** Decimal $>$ 0. Cannot exceed Current Unutilized Balance.
*   **New Tolerance (+/-) (39A):** Integer. Overwrites original tolerance.
*   **Additional Amounts (39C):** Text.

### C. Logistics, Routing & Payment Terms UI
*   **Beneficiary Details (59):** Text/Address (System strips Option A logic for amendment).
*   **Payment Terms (Drafts) (42C):** Text.
*   **Drawee Bank (42a):** BIC/Name. Unlocked if 42C populated.
*   **Partial Shipments (43P) / Transhipments (43T):** `ALLOWED`, `NOT ALLOWED`, `CONDITIONAL`.
*   **Ports (Take/Load/Disch) (44A, 44E, 44F):** String. Triggers mandatory Geo-Sanctions screening.
*   **Latest Shipment Date (44C):** Date $\le$ New Expiry Date.
*   **Presentation Period (48):** Integer.
*   **Confirmation Instr. (49):** `CONFIRM`, `MAY ADD`, `WITHOUT`.
*   **Confirming Bank (58a):** BIC. Unlocked if 49 is `CONFIRM`.
*   **Reimbursing Bank (53a) / Advise Through Bank (57a):** BIC.

### D. Charges & Settlement UI
*   **Amendment Paid By (71N):** `APPLICANT`, `BENEFICIARY`.
*   **Overall Charges (71D):** Text.
*   **Sender/Receiver Notes (72Z):** Text.

---

## 6. The "Hidden" System Payload (SRG 2024 Header Builder)

The system must automatically inject these Mandatory tags without Maker input:
*   **Sequence (27):** Auto-calculated. If > 10,000 chars, generates MT 708 (`1/2`, `2/2`).
*   **Sender's Ref (20):** Pulls Master LC Number.
*   **Receiver's Ref (21):** Pulls Advising Bank's reference from MT 730 Acknowledgment (or `NONREF`).
*   **Issuing Bank Ref (23):** Pulls Master LC Number.
*   **Date of Issue (31C):** Issue Date from Master LC record.
*   **Amendment Num (26E):** Count of accepted amendments + 1.
*   **Amend Date (30):** Current business date.
*   **Purpose (22A):** Injects `ACNF`.

**STRICTLY NON-AMENDABLE FIELDS (UI Locked)**
*   LC Number (20), Currency (32B), Applicant Name/Details (50), Issuing Bank (52a).

---

## 7. Exhaustive MT 707 Data Dictionary (SRG 2024)

### 7.1 Header & Message Control (Mandatory Block)
| Tag | Field Name | Status | System / Mapping Rules |
| :--- | :--- | :--- | :--- |
| **27** | Sequence of Total | **M** | Used for pagination (e.g., `1/1`). If > 10,000 chars, spills into MT 708. |
| **20** | Sender's Reference | **M** | The Issuing Bank's LC Number. |
| **21** | Receiver's Reference | **M** | The Advising Bank's Reference. (If unknown, must explicitly be `NONREF`). |
| **23** | Issuing Bank's Reference | **M** | *Made Mandatory.* Carried forward from the issuance event. |
| **52a** | Issuing Bank | O | Used if the amendment is being relayed by a third-party bank. |
| **50B** | Non-Bank Issuer | O | Used if the LC was issued by a non-bank entity. |
| **31C** | Date of Issue | **M** | *Made Mandatory.* The issue date of the original MT 700. |
| **26E** | Number of Amendment | **M** | *Made Mandatory.* Sequential integer. |
| **30** | Date of Amendment | **M** | *Made Mandatory.* The business date this amendment is executed. |
| **22A** | Purpose of Message | **M** | *Made Mandatory.* Must contain `ACNF`. |
| **23S** | Cancellation Request | O | If cancelling entirely, contains `CANCEL`. |

### 7.2 Contract Base & Financials
| Tag | Field Name | Status | System / Mapping Rules |
| :--- | :--- | :--- | :--- |
| **40A** | Form of Doc. Credit | O | E.g., `IRREVOCABLE`. |
| **40E** | Applicable Rules | O | E.g., `UCP LATEST VERSION`. |
| **31D** | Date and Place of Expiry | O | Replaces the old expiry date/place. |
| **50** | Changed Applicant Details | O | Used only if the Applicant's name/address changes. |
| **59** | Beneficiary | O | Used only if the Beneficiary details change. |
| **32B** | Increase of Amount | O | Exact delta amount added. Mutually exclusive with 33B. |
| **33B** | Decrease of Amount | O | Exact delta amount subtracted. Mutually exclusive with 32B. |
| **39A** | Percentage Tolerance | O | Overwrites original `+/-` tolerance. |
| **39C** | Additional Amounts Covered | O | Overwrites original. |

### 7.3 Payment Terms, Drafts & Routing
| Tag | Field Name | Status | System / Mapping Rules |
| :--- | :--- | :--- | :--- |
| **41a** | Available With... By... | O | Changes Negotiating Bank or payment method. |
| **42C** | Drafts at... | O | Changes Tenor of draft. |
| **42a** | Drawee | O | Changes Bank draft is drawn upon. |
| **42M** | Mixed Payment Details | O | Text describing complex payment schedules. |
| **42P** | Deferred Payment Details | O | Text describing maturity dates for deferred payments. |
| **48** | Period for Presentation | O | Changes required days to present documents. |
| **49** | Confirmation Instructions | O | Amends to `CONFIRM`, `MAY ADD`, or `WITHOUT`. |
| **58a** | Requested Confirming Party| O | Added/Amended if Tag 49 changes to require confirmation. |
| **53a** | Reimbursing Bank | O | Added/Amended if settlement routing changes. |
| **57a** | 'Advise Through' Bank | O | Changes routing of Second Advising Bank. |

### 7.4 Logistics & Shipment
| Tag | Field Name | Status | System / Mapping Rules |
| :--- | :--- | :--- | :--- |
| **43P** | Partial Shipments | O | Amends to `ALLOWED`, `NOT ALLOWED`, `CONDITIONAL`. |
| **43T** | Transhipment | O | Amends to `ALLOWED`, `NOT ALLOWED`, `CONDITIONAL`. |
| **44A** | Place of Taking in Charge | O | Overwrites previous value. |
| **44E** | Port of Loading | O | Overwrites previous value. |
| **44F** | Port of Discharge | O | Overwrites previous value. |
| **44B** | Place of Final Destination | O | Overwrites previous value. |
| **44C** | Latest Date of Shipment | O | Mutually exclusive with 44D. |
| **44D** | Shipment Period | O | Mutually exclusive with 44C. |

### 7.5 Narrative Text (The "B" Tags) & Charges
*Uses Action Prefixes (`/ADD/`, `/DELETE/`, `/REPALL/`)*
| Tag | Field Name | Status | System / Mapping Rules |
| :--- | :--- | :--- | :--- |
| **45B** | Description of Goods | O | Modifies Tag 45A. Max 100 lines of 65 chars. |
| **46B** | Documents Required | O | Modifies Tag 46A. |
| **47B** | Additional Conditions | O | Modifies Tag 47A. |
| **49M** | Special Payment Cond. (Bene)| O | Modifies Tag 49G. |
| **49N** | Special Payment Cond. (Bank)| O | Modifies Tag 49H. |
| **71D** | Charges | O | Modifies overarching fee structure. |
| **71N** | Amendment Charge Payable By| O | Dictates who pays for *this specific MT 707*. |
| **78** | Instructions to Paying Bank| O | Modifies Interbank payment instructions. |
| **72Z** | Sender to Receiver Info | O | Private notes not meant for Beneficiary. |

---

## 8. Traceability Requirements

| Requirement ID | Description | Component |
| :--- | :--- | :--- |
| **FR-AMD-01** | Segregate External (UCP 600) and Internal Bank Amendment workflows. | Business Logic |
| **FR-AMD-02** | Implement Smart Delta UI: Left side read-only, right side input fields. | UI |
| **FR-AMD-03** | Implement Narrative Track Changes: Dropdown (Add/Del/Rep) + Text. | UI & DB |
| **FR-AMD-04** | Auto-inject Mandatory SRG 2024 Header tags (20, 21, 23, 31C, 26E, 30, 22A). | SWIFT Gen |
| **FR-AMD-05** | State Machine: Delay limit release on decrease until Consent Accepted. | Business Logic |
| **FR-AMD-06** | State Machine: Execute Internal amendments immediately without MT 707. | Business Logic |
| **FR-AMD-07** | Auto-log Beneficiary Consent upon receipt of incoming MT 730. | Integration |
| **FR-AMD-08** | Lock Currency, Applicant, and LC Number from being amended. | UI |
