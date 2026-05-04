## Revise existing sections in BRD

**Suspressed:** Section 3: Core Business Processes (Detailed Specification) > 3.2. Process: Import LC Amendments (`Trade_Finance/BRD_ImportLC.md`)

Here is the fully revised and architecturally complete Business Requirements Document (BRD) section for Import LC Amendments. This incorporates the exhaustive MT 707 SWIFT mapping, the strict legal requirement for Beneficiary Consent on all UCP 600 changes, and the distinct workflow for Internal back-office amendments.

This process dictates how an active, issued Import LC is modified. The system must rigorously separate amendments into two distinct operational scopes: **External (UCP 600) Amendments** and **Internal Bank Amendments**.

### A. Amendment Scopes & Consent Rules

1.  **External (UCP 600) Amendments:** 
    *   **Definition:** Any change to the legal text of the LC (Financial, Logistics, Narrative, or Charges). 
    *   **Consent Rule:** Under UCP 600 Article 10, **ALL** external amendments (even a single typo correction) strictly require Beneficiary Consent to become legally binding.
    *   **System Action:** Generates an MT 707. Master LC is *not* updated until consent is logged.
2.  **Internal Bank Amendments:** 
    *   **Definition:** Changes to the bank's back-office tracking that do not alter the contract provided to the Beneficiary (e.g., re-linking the Facility Limit ID, changing the Applicant's fee debit account, altering internal margin requirements).
    *   **Consent Rule:** **None.** The Beneficiary is not a party to these changes.
    *   **System Action:** Does *not* generate an MT 707. Overwrites the Master LC internal data immediately upon Checker approval.

**Related States**
* **Transaction State (System):** Draft (Amendment) $\rightarrow$ Pending Approval $\rightarrow$ Active
* **LC Business State (Domain):** The parent LC remains in the **Issued** state throughout. The system creates a linked "Amendment Record" that transitions from Draft $\rightarrow$ Pending Approval $\rightarrow$ Dispatched $\rightarrow$ Accepted/Rejected (by Beneficiary).

---

### B. Business Process Workflow
1. **Initiation:** Operations locates the active LC and clicks "Initiate Amendment" / "Initiate Internal Amendment"
2. **System field populations** based on the selected amendment type ("External" or "Internal"), system will display dedicated UI and populate related fields with existing data (if any).
3. **Data Entry:** The Maker inputs only the fields that are changing (e.g., increasing the amount, changing the port).
4. **Submission:** Maker submits. Status changes to *Pending Approval*. The required Checker tier is dynamically calculated based on the *new total liability*.
5. **Checker Review & Authorization:** Checker reviews the amendment request against the customer's instructions and approves.
6. **Execution:** System deducts/releases limits (if financial), applies amendment fees, and generates the MT 707 message.
7. **Beneficiary Consent Tracking:** The amendment remains structurally pending until operations logs the Beneficiary's official acceptance or rejection.

### C. Initiation & Post Conditions
* **Initiation Criteria (Entry):** The parent LC must be in an *Issued* state and must not have reached its Expiry Date. 
* **Post Condition (Exit):** An MT 707 is dispatched. The bank's liability is updated (if financial). The new terms become legally binding *only* upon logging the Beneficiary's acceptance.

***

### D. Inputs Capture & UI Behavior (SRG 2024 Compliant)

Here is the completely reworked BRD section for the **LC Amendment Inputs Capture UI**. 

This design completely abstracts the rigid SRG 2024 rules away from the operations user. It implements a "Smart Delta" UI that automatically handles the mandatory historical fields and injects the required SWIFT codewords into the payload, ensuring zero formatting errors.

---

During an amendment, the Maker operates in a "Delta Only" UI. The system displays the original LC values as read-only context on the left, and provides input fields for the new/amended values on the right. 

To comply with the SRG 2024 overhaul, the UI is broken into four functional blocks.

#### 1. The "Track Changes" Narrative Engine (Tags 45B, 46B, 47B, 49M, 49N)
SWIFT SRG 2024 requires text amendments to use specific "B" tags with prefix codewords (e.g., `/ADD/`, `/DELETE/`, `/REPALL/`). **Users will not type these codewords.**

Instead, the UI provides a structured input block for every narrative field:
*   **Action Selector (Dropdown):** User selects `Add`, `Delete`, or `Replace All`.
*   **Input Area (Text Box):** User types the plain text of the amendment.
*   **Payload Injection Rule:** The system concatenates the selection and the text to build the exact SWIFT payload. 
    *   *User Action:* Selects `Add` and types "Certificate of Origin required."
    *   *System Payload:* Generates Tag 47B as `/ADD/Certificate of Origin required.`

#### 2. Financials & Dates Delta UI
*Note: Tags 34B (New Total) and 31E (New Expiry) are obsolete. The UI captures the delta and recalculates liability internally.*

| UI Field Name | Maps to SWIFT Tag | Data Type | UI Behavior & Validation |
| :--- | :--- | :--- | :--- |
| **New Expiry Date** | `31D` | Date | Must be $\ge$ Current Date. Modifies the existing 31D payload. |
| **Amount Increase** | `32B` | Decimal | Must be $>$ 0. Mutually exclusive with Decrease. |
| **Amount Decrease** | `33B` | Decimal | Must be $>$ 0. **Strict Validation:** Cannot exceed Current Unutilized Balance. |
| **New Total Amount** | *None (Display Only)* | Decimal | System calculates: `Original Amount` + `Increase` - `Decrease`. |
| **New Tolerance (+/-)** | `39A` | Integer | Overwrites original tolerance. |
| **Additional Amounts** | `39C` | Text | E.g., "Insurance and freight." Overwrites previous value. |

#### 3. Logistics, Routing & Payment Terms UI
Users only interact with these fields if they are actively changing the routing or logistics of the shipment.

| UI Field Name | Maps to SWIFT Tag | Data Type | UI Behavior & Validation |
| :--- | :--- | :--- | :--- |
| **Beneficiary Details** | `59` | Text/Address | Used to amend the address. (System strips Option A/Account logic). |
| **Payment Terms (Drafts)**| `42C` | Text | Changes the Draft Tenor (e.g., "90 Days After Sight"). |
| **Drawee Bank** | `42a` | BIC / Name | Only unlocked if `42C` is populated. |
| **Partial Shipments** | `43P` | Dropdown | Values: `ALLOWED`, `NOT ALLOWED`, `CONDITIONAL`. |
| **Transhipments** | `43T` | Dropdown | Values: `ALLOWED`, `NOT ALLOWED`, `CONDITIONAL`. |
| **Ports (Take/Load/Disch)**| `44A, 44E, 44F` | String | Overwrites original. **Trigger:** Mandatory Geo-Sanctions screening. |
| **Latest Shipment Date** | `44C` | Date | Must be $\le$ New Expiry Date. |
| **Presentation Period** | `48` | Integer | Changes days allowed for presentation. |
| **Confirmation Instr.** | `49` | Dropdown | Values: `CONFIRM`, `MAY ADD`, `WITHOUT`. |
| **Confirming Bank** | `58a` | BIC | Unlocked if `49` is amended to `CONFIRM`. |
| **Reimbursing Bank** | `53a` | BIC | Changes the settlement routing. |
| **Advise Through Bank** | `57a` | BIC | Changes the Second Advising Bank routing. |

#### 4. Charges & Settlement UI
| UI Field Name | Maps to SWIFT Tag | Data Type | UI Behavior & Validation |
| :--- | :--- | :--- | :--- |
| **Amendment Paid By** | `71N` | Dropdown | Values: `APPLICANT`, `BENEFICIARY`. Dictates fee deduction for this action. |
| **Overall Charges** | `71D` | Text | Overwrites original fee structure. |
| **Sender/Receiver Notes** | `72Z` | Text | Private interbank instructions (Not seen by Beneficiary). |

---

### E. The "Hidden" System Payload (SRG 2024 Header Builder)

Because the SRG 2024 requires strict historical linkage, the Moqui SWIFT generation service must automatically inject the following Mandatory (`M`) tags into the MT 707 without any Maker input.

| SWIFT Tag | System Action / Source |
| :--- | :--- |
| **Sequence (27)** | System calculates automatically. If message > 10,000 chars, generates MT 708 and sets to `1/2`, `2/2`. |
| **Sender's Ref (20)** | System pulls the Master LC Number. |
| **Receiver's Ref (21)** | System pulls the Advising Bank's reference from the MT 730 Acknowledgment. If null, injects `NONREF`. |
| **Issuing Bank Ref (23)**| System pulls the Master LC Number (redundant, but mandated by SRG). |
| **Date of Issue (31C)** | System pulls the `Issue Date` from the Master LC database record. |
| **Amendment Num (26E)**| System queries the database for the count of accepted amendments and increments by +1. |
| **Amend Date (30)** | System injects the current business date. |
| **Purpose (22A)** | System injects `ACNF` (Advice and confirmation of amendment). |


**STRICTLY NON-AMENDABLE FIELDS**
Your system UI must physically lock these fields. If the client wants to change these, they must cancel the existing LC and issue a brand new one.
*   **LC Number (Tag 20)**
*   **Currency (Tag 32B)** - Changing currency creates a legally distinct contract.
*   **Applicant Name & Details (Tag 50)** - Credit risk was assessed on the specific Applicant; it cannot be swapped.
*   **Issuing Bank (Tag 52a)**

---

### F. State Machine & Liability Impact Timing (The "When" & "What")

Because External Amendments require consent, the Moqui system must maintain the **Master LC Record** and a floating **Pending Amendment Record**. The system executes logic at specific lifecycle statuses:

**State 1: `Dispatched` (Checker Approves, MT 707 Sent)**
*   **SWIFT:** MT 707 generated and transmitted.
*   **Original LC Data:** **NO CHANGE.** Master LC remains in original state.
*   **Liability Rule (Increase):** If Amount/Tolerance *increases*, system **MUST** block/earmark the new limit from the core facility immediately. 
*   **Liability Rule (Decrease):** If Amount *decreases*, system **DOES NOT** release the limit yet, as the Beneficiary may reject the decrease.

**State 2: `Accepted` (Beneficiary Formally Agrees)**
*   **Trigger:** Operations logs receipt of MT 730 or formal consent email.
*   **Original LC Data:** System executes "Merge." The amendment data officially overwrites the Master LC.
*   **Liability Rule (Decrease):** Only at this moment does the system call the Core Banking API to release excess limits back to the Applicant's facility.

**State 3: `Rejected` (Beneficiary Refuses)**
*   **Trigger:** Operations logs rejection notice.
*   **Original LC Data:** Remains untouched. Amendment record flagged as Dead.
*   **Liability Rule (Increase):** The extra limit that was blocked at `Dispatched` is immediately un-earmarked and released back to the facility.

**State 4: `Internal Executed` (For Internal Scope Amendments Only)**
*   **Trigger:** Checker approves an internal-only change.
*   **System Action:** Bypasses SWIFT. Master LC internal tags (Facility ID, Fee Account) are overwritten immediately. Limits and routing update in real-time.

***

## **Revised MT 707 (LC Amendments)**

Here is the **complete, exhaustive list of every single tag permitted in an MT 707 message** according to the latest 2024 SWIFT Standards. 

---

### 1. The Exhaustive MT 707 Data Dictionary (SRG 2024)

The most critical system change for your developers to note is the **Header & Control Block**. Previously, only tags 20 and 21 were mandatory. Now, SWIFT forces your system to maintain state and carry forward data from the original MT 700 to validate the amendment. 

#### 1. Header & Message Control (The Mandatory Block)
*If your Moqui system fails to populate any of the 'M' fields, the SWIFT network will instantly NACK (reject) the message.*

| Tag | Field Name | Status | System / Mapping Rules |
| :--- | :--- | :--- | :--- |
| **27** | Sequence of Total | **M** | Used for pagination (e.g., `1/1`). If the text exceeds SWIFT's limit, the message spills into an MT 708. |
| **20** | Sender's Reference | **M** | The Issuing Bank's LC Number. |
| **21** | Receiver's Reference | **M** | The Advising Bank's Reference. (If unknown, must explicitly be `NONREF`). |
| **23** | Issuing Bank's Reference | **M** | *Made Mandatory.* Carried forward from the issuance event. |
| **52a** | Issuing Bank | O | Used if the amendment is being relayed by a third-party bank. |
| **50B** | Non-Bank Issuer | O | Used if the LC was issued by a non-bank entity. |
| **31C** | Date of Issue | **M** | *Made Mandatory.* The issue date of the original MT 700. |
| **26E** | Number of Amendment | **M** | *Made Mandatory.* Sequential integer (e.g., `1`, `2`). Allows up to 999. |
| **30** | Date of Amendment | **M** | *Made Mandatory.* The business date this amendment is executed. |
| **22A** | Purpose of Message | **M** | *Made Mandatory.* Must contain a SWIFT code like `ACNF` (Advice and confirmation of amendment). |
| **23S** | Cancellation Request | O | If the purpose of the MT 707 is to cancel the LC entirely, this tag contains the code `CANCEL`. |

#### 2. Contract Base & Financials
*Note: Tags 34B (New Total Amount) and 31E (New Expiry) were completely removed from the network. Your system must imply the new total mathematically and use 31D for expiry changes.*

| Tag | Field Name | Status | System / Mapping Rules |
| :--- | :--- | :--- | :--- |
| **40A** | Form of Doc. Credit | O | E.g., `IRREVOCABLE`. |
| **40E** | Applicable Rules | O | E.g., `UCP LATEST VERSION`. |
| **31D** | Date and Place of Expiry | O | Replaces the old expiry date/place. |
| **50** | Changed Applicant Details | O | Used only if the Applicant's name/address changes. |
| **59** | Beneficiary | O | *Made Optional.* Used only if the Beneficiary details change. |
| **32B** | Increase of Amount | O | The exact delta amount added. Mutually exclusive with 33B. |
| **33B** | Decrease of Amount | O | The exact delta amount subtracted. Mutually exclusive with 32B. |
| **39A** | Percentage Tolerance | O | Overwrites the original `+/-` tolerance (e.g., `10/10`). |
| **39C** | Additional Amounts Covered | O | E.g., "Insurance and freight costs covered." |

#### 3. Payment Terms, Drafts & Routing
| Tag | Field Name | Status | System / Mapping Rules |
| :--- | :--- | :--- | :--- |
| **41a** | Available With... By... | O | Changes the Negotiating Bank or payment method. |
| **42C** | Drafts at... | O | Changes the Tenor of the draft (e.g., "90 Days After Sight"). |
| **42a** | Drawee | O | Changes the Bank the draft is drawn upon. |
| **42M** | Mixed Payment Details | O | Text describing complex payment schedules. |
| **42P** | Deferred Payment Details | O | Text describing maturity dates for deferred payments. |
| **48** | Period for Presentation | O | Changes the required days to present documents (e.g., `21 DAYS`). |
| **49** | Confirmation Instructions | O | Amends to `CONFIRM`, `MAY ADD`, or `WITHOUT`. |
| **58a** | Requested Confirming Party| O | Added/Amended if Tag 49 changes to require confirmation. |
| **53a** | Reimbursing Bank | O | Added/Amended if the settlement routing changes. |
| **57a** | 'Advise Through' Bank | O | Changes the routing of the Second Advising Bank. |

#### 4. Logistics & Shipment
| Tag | Field Name | Status | System / Mapping Rules |
| :--- | :--- | :--- | :--- |
| **43P** | Partial Shipments | O | Amends to `ALLOWED`, `NOT ALLOWED`, or `CONDITIONAL`. |
| **43T** | Transhipment | O | Amends to `ALLOWED`, `NOT ALLOWED`, or `CONDITIONAL`. |
| **44A** | Place of Taking in Charge | O | Overwrites previous value. |
| **44E** | Port of Loading | O | Overwrites previous value. |
| **44F** | Port of Discharge | O | Overwrites previous value. |
| **44B** | Place of Final Destination | O | Overwrites previous value. |
| **44C** | Latest Date of Shipment | O | Mutually exclusive with 44D. |
| **44D** | Shipment Period | O | Mutually exclusive with 44C. |

#### 5. Narrative Text (The "B" Tags) & Charges
*System Note: To make parsing easier for bank systems, MT 707 amendments use "B" tags for text. Users should use specific prefix codes like `/ADD/`, `/DELETE/`, or `/REPALL/` within these text blocks.*

| Tag | Field Name | Status | System / Mapping Rules |
| :--- | :--- | :--- | :--- |
| **45B** | Description of Goods | O | Modifies original Tag 45A. Max 100 lines of 65 chars. |
| **46B** | Documents Required | O | Modifies original Tag 46A. |
| **47B** | Additional Conditions | O | Modifies original Tag 47A. |
| **49M** | Special Payment Cond. (Bene)| O | Modifies original Tag 49G. |
| **49N** | Special Payment Cond. (Bank)| O | Modifies original Tag 49H. |
| **71D** | Charges | O | Modifies the overarching fee structure text. |
| **71N** | Amendment Charge Payable By | O | Dictates who pays for *this specific MT 707 message*. |
| **78** | Instructions to Paying Bank | O | Modifies Interbank payment instructions. |
| **72Z** | Sender to Receiver Info | O | Private bank-to-bank notes not meant for the Beneficiary. |

---
#### Why this matters for our system

Because SWIFT made fields like **31C (Date of Issue)** and **23 (Issuing Bank's Reference)** mandatory for amendments, your System database *must* retain strict relational links between the pending `Amendment Record` and the `Master LC Record`. 

When your Java/Groovy SWIFT generation service builds the MT 707, it cannot just look at the "Delta" screen the Maker filled out. It must query the Master LC database entity to pull tags 31C, 20, and 23 to successfully construct the mandatory message header.

---

### 2. Do we need Beneficiary Consent for BOTH types of amendment?

**Yes, absolutely.**

Under **UCP 600 Article 10(a):** *"A credit can neither be amended nor cancelled without the agreement of the issuing bank, the confirming bank, if any, and the beneficiary."*

UCP 600 makes zero distinction between a "Financial" and a "Non-Financial" amendment. If you change a single comma in the Port of Loading, it is legally an amendment to the contract. The Beneficiary must agree to it, because that new term might be impossible for them to fulfill (e.g., the ship already left the original port). 

If the Beneficiary does not explicitly accept the MT 707, they are legally permitted to present shipping documents that comply exactly with the *original* MT 700. If they do, your bank is legally obligated to pay them. 

*(System Implementation Note: This is why your Moqui system must keep the "Original" data active, and the "Amendment" data in a Pending state until consent is logged.)*

---

### 3. Is there any case where an amendment is strictly "Internal Scope" (No Consent Needed)?

**Yes.** While UCP 600 requires consent for changes to the *contractual* terms, banks process "Internal Amendments" constantly. 

An internal amendment occurs when a data point changes that affects the bank's back-office accounting, but does **not** alter the UCP 600 text provided to the Beneficiary. 

Because the legal contract isn't changing, **no MT 707 is generated, and no Beneficiary Consent is required.**

**Cases for an Internal Scope Amendment in Moqui:**
1.  **Changing the Fee Settlement Account:** The Applicant calls and says, "Please debit my EUR Account instead of my USD Account for the monthly LC commission fees going forward."
2.  **Updating the Credit Facility ID (Limit Re-allocation):** The corporate relationship manager restructures the Applicant's credit lines. The LC's liability needs to be un-linked from "Facility 123" and re-linked to "Facility 456".
3.  **Changing Internal Margin/Collateral:** The bank decides they want the client to deposit 10% cash margin against the LC due to a drop in the client's credit score.
4.  **Re-assigning the Relationship Manager:** Changing the internal tracking codes or profit-center mapping for the LC.

**How to handle this in your Moqui State Machine:**
You need to build a distinct UI action called `Initiate Internal Amendment`. 
*   This UI will *only* allow the Maker to edit internal fields (Limit IDs, Fee Accounts, Margin Accounts). 
*   It routes to a Checker for internal dual-control.
*   Upon approval, it updates the Master LC Record immediately. 
*   **Crucial Rule:** It completely bypasses the MT 707 SWIFT generation service and the Beneficiary Consent workflow.