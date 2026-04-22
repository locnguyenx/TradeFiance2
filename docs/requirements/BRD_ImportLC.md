Here is the detailed Business Requirements Document (BRD) for the Import Letter of Credit (LC) Module. Continuing our established standard, this section is strictly focused on business processes, lifecycle states, and operational rules, omitting any technical architecture to remain accessible for business stakeholder review.

***

# Business Requirements Document (BRD)
**Project Name:** Digital Trade Finance Platform
**Module:** Import Letter of Credit (LC)
**Document Version:** 1.0
**Date:** April 21, 2026

## Module: Import Letter of Credit (LC)

### 1. Overview
The Import LC module facilitates the issuance and entire lifecycle management of Documentary Credits on behalf of the bank's importing customers (Applicants). It ensures that the bank guarantees payment to a foreign supplier (Beneficiary) strictly upon the presentation of complying shipping and commercial documents, acting in accordance with UCP 600 guidelines.

### 2. LC Lifecycle States
An Import LC must transition through the following standard business states. State transitions are governed by the Maker/Checker authorization matrix defined in the Common Module.

* **Draft:** The initial data entry phase. The application is being prepared by the operations team or saved by the customer via the corporate portal.
* **Pending Approval:** The LC application has been submitted by a Maker and is awaiting authorization by a Checker with the appropriate financial tier limit.
* **Issued:** The LC has been formally approved, limits are earmarked, and the MT 700 (Issue of a Documentary Credit) message has been dispatched to the Advising Bank.
* **Amended:** A formal change to the terms of an Issued LC (e.g., value increase, expiry extension) has been processed and approved.
* **Documents Received:** Physical or electronic documents have been received from the Presenting Bank and are pending examination by the trade operations team.
* **Discrepant:** Operations has examined the documents and found them non-compliant with the LC terms. The transaction awaits Applicant waiver or formal refusal.
* **Accepted / Clean:** Documents are found strictly compliant (or discrepancies have been waived by the Applicant and accepted by the bank). The bank's undertaking to pay is now unconditional.
* **Settled:** Funds have been successfully debited from the Applicant and remitted to the Presenting Bank.
* **Closed / Cancelled:** The LC has expired unutilized, or has been fully drawn and settled, officially releasing any remaining facility earmarks.

**IMPORTANT NOTES:**
The term "LC Lifecycle States" here and the "Standardized Processing Flow" in the common module brd are not the same, though they are deeply interconnected. In business analysis and system design, it is crucial to distinguish between the two because they serve different purposes for different audiences (operations teams vs. software developers).

Here is the distinction:

#### 1. Standardized Processing Flow (The "How" & "Who")
The Processing Flow describes the **actions, human behaviors, and sequence of events**. It maps out *what people and the system are actually doing* step-by-step. The target (objective) of this flow is a Transaction.

* **Focus:** Operations, user journeys, and task hand-offs.
* **Examples:** "Maker inputs data," "System checks KYC," "Checker reviews documents."
* **Why it's in the BRD:** It helps the business operations team understand how their daily manual tasks will translate into the new digital system.

#### 2. LC Lifecycle States (The "What")
The Lifecycle State / or business state describes the **strict condition of the data record** at a specific moment in time. It is the static *result* of an action. A state dictates what can and cannot be done to the Business Object next.

* **Focus:** Data governance, system logic, and access control.
* **Examples:** *Draft, Pending Approval, Issued, Settled.*
* **Why it's in the BRD:** It tells the software architects (using frameworks like Moqui) exactly how to build the database logic, state machines, and API permissions. 

---

#### How They Work Together (The Mapping)
Think of the **Processing Flow** as the *vehicle* driving down a road, and the **Lifecycle States** as the *checkpoints* it must park at along the way. 

An action in the Processing Flow acts as the **trigger** that moves the Business Object from one Lifecycle State to the next.

| Standardized Processing Flow (Action) | Trigger | Resulting Lifecycle State |
| :--- | :--- | :--- |
| Maker inputs application data and saves. | Save Action | **Draft** |
| Maker clicks "Submit for Review." | Submit Action | **Pending Approval** |
| Checker reviews and clicks "Authorize." | Authorize Action | **Issued** |
| Presenting Bank sends physical documents. | Receive Action | **Documents Received** |
| Operations logs discrepancies in the system. | Log Action | **Discrepant** |
| System debits the Applicant's account. | Pay Action | **Settled** |

**Summary:** The Common Module BRD defines the **Processing Flow** because the *concept* of Maker/Checker actions applies to every product. The Import LC BRD defines specific **Lifecycle States** because the exact statuses (like *Documents Received* or *Discrepant*) are unique to the reality of processing Letters of Credit.

Adopting this precise terminology is an excellent move. In a Business Requirements Document, clarity is everything. Separating the generic system workflow from the domain-specific instrument lifecycle will eliminate confusion for the business stakeholders reading the document.

Going forward in the BRD, we will strictly use this nomenclature:

**1. Transaction State (System & Workflow Level)**
This refers strictly to the Maker/Checker processing flow defined in the **Common Module**. It dictates who has the current action and system control.
* **Examples:** *Draft, Pending Approval, Active/Executed, Hold.*
* **Focus:** System access, user routing, and authorization.

**2. LC Business State (Domain & Product Level)**
This refers strictly to the lifecycle of the specific financial instrument (the Import LC) as defined in the **Import LC Module**. It dictates the legal and operational reality of the trade.
* **Examples:** *Issued, Documents Received, Discrepant, Settled, Closed.*
* **Focus:** UCP 600 compliance, bank liability, and operational outcomes.

By making this separation, it becomes very easy to explain complex scenarios to stakeholders. For example: An Amendment can be in a **Transaction State** of *Pending Approval* while the underlying LC remains in an **LC Business State** of *Issued*. 

This updated terminology will be applied consistently across all subsequent modules we draft.

### 3. Core Business Processes

#### 3.1. LC Issuance
* **Application Capture:** The system must capture all mandatory fields for LC issuance, including Applicant/Beneficiary details, LC Amount, Currency, Tolerance percentage (e.g., +/- 10%), Tenor (Sight or Usance), Expiry Date/Place, Latest Shipment Date, Port of Loading/Discharge, Required Documents, and Description of Goods.
* **Margin/Cash Collateral:** The system must allow operations to block cash collateral (margin) in the Applicant's designated deposit account if the customer does not have a sufficient unsecured credit facility.
* **SWIFT Generation:** Upon final authorization, the system must automatically format and generate an outward SWIFT MT 700 message.

#### 3.2. Amendments
* **Initiation:** Users must be able to initiate an amendment to any active, unexpired LC. 
* **Financial vs. Non-Financial:** The system must distinguish between financial amendments (increasing the amount or extending the expiry date beyond the original facility expiry) and non-financial amendments (correcting typos, changing shipping ports).
* **Limit Recalculation:** Financial amendments must trigger a recalculation of the required credit limit and route through the Maker/Checker matrix based on the *new total liability*, not just the amendment delta.
* **Beneficiary Consent:** The system must track the Beneficiary's acceptance or rejection of the amendment (as an LC is irrevocable and amendments require all parties' consent).

#### 3.3. Document Presentation & Examination
* **Lodgement:** When documents arrive, the system must record the receipt date, presentation amount, and the number/type of physical documents received (e.g., 3 originals of Bill of Lading, 2 Invoices).
* **Examination Window:** The system must start a compliance countdown timer (maximum of 5 banking days following the day of presentation, per UCP 600) for operations to complete document examination.
* **Discrepancy Logging:** If discrepancies are found (e.g., late shipment, missing signatures), the checker must log standard ISBP discrepancy codes into the system.
* **Communication:** The system must generate an MT 734 (Notice of Refusal) to the Presenting Bank if discrepancies are severe, or an internal notice to the Applicant requesting a waiver.

#### 3.4. Settlement & Payment
* **Sight LCs:** Upon determining a clean presentation (or accepting a waiver), the system must immediately trigger the debit of the Applicant's account and generate the outward remittance (MT 202/MT 103) to the Presenting Bank.
* **Usance (Deferred Payment) LCs:** Upon clean presentation, the system must generate an MT 732 (Advice of Discharge) acknowledging the bank's commitment to pay on the future maturity date. The system must track this maturity date and automatically queue the settlement when due.

### 4. Specific Validation & Business Rules
* **Tolerance Limit Check:** The system must validate that the total drawn amount across all presentations does not exceed the LC amount plus the stated positive tolerance percentage.
* **Expiry Date Rule:** The system must prevent the logging of a new document presentation if the presentation date is strictly after the established LC Expiry Date.
* **Revolving LCs:** If the LC is designated as "Revolving," the system must automatically reinstate the original LC amount upon settlement of a drawing, without requiring a manual amendment, until the maximum cumulative limit is reached.
* **Regulatory Reporting:** For transactions processed within Vietnam, the system must automatically flag and categorize the import goods codes for subsequent foreign exchange (FX) outflow reporting to the State Bank.

---

This is a necessary enhancement. A robust Trade Finance system relies heavily on strict state machine governance. If the acceptable transitions, entry criteria, and exit criteria are not explicitly defined for business operations, the risk of non-compliant workflow execution increases significantly.

Here is the expanded and highly detailed **Section 2: Transaction Lifecycle States** for the Import LC BRD, designed strictly around business logic and operational controls.

***

## **REQUIREMENT DETAILS FOR SECTIONS ABOVE**

### Section 2 Details: LC Lifecycle States & Transition Matrix

The lifecycle of an Import LC is not strictly linear. LC may move backward (e.g., due to rejections during authorization) or loop through cyclic states (e.g., partial drawings or amendments). The system must strictly enforce the permitted transitions defined below.

#### 2.1. State Transition Overview

| Current State | Permitted Next States (Forward / Loop / Backward / Terminal) | Trigger Event (Business Action) |
| :--- | :--- | :--- |
| **1. Draft** | Pending Approval, Cancelled | Maker submits application. |
| **2. Pending Approval**| Issued, Draft (Return to Maker), Cancelled | Checker authorizes or rejects. |
| **3. Issued** | Pending Approval (for Amendment), Documents Received, Closed | Amendment initiated, docs arrive, or LC expires. |
| **4. Documents Received**| Accepted/Clean, Discrepant | Examination completed by Operations. |
| **5. Discrepant** | Accepted/Clean, Closed (Refused) | Applicant waives, or Bank formally refuses. |
| **6. Accepted/Clean** | Settled | Payment initiated or maturity reached. |
| **7. Settled** | Issued (Partial Draw), Closed (Fully Drawn) | Payment finalized. |
| **8. Closed / Cancelled**| *None (Terminal State)* | Limits released. |

---

#### 2.2. Detailed State Criteria & Flow Rules

##### State 1: Draft
The initial data entry phase. The application is being prepared by the operations team (on behalf of the customer) or saved by the customer via the corporate portal.
* **Entry Criteria:** A user initiates a new Import LC application or an existing application is "Returned to Maker" from a Checker.
* **Exit Criteria:** All mandatory fields (Applicant, Beneficiary, Amount, Currency, Expiry Date, Tenor, Required Documents) must be populated and pass basic system validation (e.g., Expiry Date is in the future).
* **Permitted Transitions:**
    * **Forward:** To `Pending Approval` (User submits for review).
    * **Terminal:** To `Cancelled` (User discards the draft).

##### State 2: Pending Approval
The application is locked for editing by the Maker and resides in the Checker's queue for authorization, adhering to the bank's Maker/Checker limit matrix.
* **Entry Criteria:** Maker successfully submits a Draft application, OR Maker submits a formal Amendment to an Issued LC.
* **Exit Criteria:** An authorized Checker reviews the application against the customer's facility limits and AML/Sanctions screening results.
* **Permitted Transitions:**
    * **Forward:** To `Issued` (Checker approves; system cuts facility limits and generates SWIFT MT 700).
    * **Backward:** To `Draft` (Checker rejects the application due to data errors and returns it to the Maker for correction).
    * **Terminal:** To `Cancelled` (Checker permanently declines the issuance, e.g., due to credit risk).

##### State 3: Issued
The bank has formally committed its undertaking to pay. The LC is active, and the liability is recorded on the bank's books.
* **Entry Criteria:** Checker approval is completed. SWIFT MT 700 is dispatched.
* **Exit Criteria:** The LC is acted upon by an external party (documents presented) or an internal party (amendment requested).
* **Permitted Transitions:**
    * **Forward:** To `Documents Received` (Presenting Bank sends shipping documents).
    * **Loop (Backward):** To `Pending Approval` (Maker initiates an Amendment, locking the record for a new approval cycle).
    * **Terminal:** To `Closed` (The LC reaches its expiry date unutilized, plus standard mail processing days, and limits are released).

##### State 4: Documents Received
Physical or digital documents have arrived at the bank's trade counter.
* **Entry Criteria:** Operations team logs the receipt of the presentation, entering the claimed amount and document counts.
* **Exit Criteria:** Operations team completes the document examination within the strict regulatory window (maximum of 5 banking days following the day of presentation, per UCP 600).
* **Permitted Transitions:**
    * **Forward:** To `Accepted/Clean` (Documents are fully compliant with LC terms).
    * **Alternative Forward:** To `Discrepant` (Operations identifies one or more ISBP rule violations).

##### State 5: Discrepant
The presented documents failed strict compliance checks. The bank's undertaking to pay is suspended until the discrepancies are resolved.
* **Entry Criteria:** Checker authorizes the examination results containing standard discrepancy codes. An internal notice is sent to the Applicant requesting a waiver; optionally, a SWIFT MT 734 (Notice of Refusal) is sent to the Presenting Bank.
* **Exit Criteria:** The Applicant provides a formal decision (waive or refuse), and the bank agrees with the decision.
* **Permitted Transitions:**
    * **Forward:** To `Accepted/Clean` (Applicant formally waives the discrepancies, AND the bank's credit risk team accepts the waiver).
    * **Terminal:** To `Closed` (Applicant refuses the documents, bank formally refuses presentation, and documents are returned to the Presenter. If no other drawings remain, LC is closed).

##### State 6: Accepted / Clean
The bank is now unconditionally bound to honor the drawing, either immediately (Sight) or at a future date (Usance).
* **Entry Criteria:** Documents are deemed clean during examination, OR discrepant documents are fully waived and accepted.
* **Exit Criteria:** The payment process is triggered by the system.
* **Permitted Transitions:**
    * **Forward:** To `Settled` (For Sight LCs: payment is processed immediately. For Usance LCs: system tracks the maturity date and initiates payment on the due date).

##### State 7: Settled
Funds have been successfully moved.
* **Entry Criteria:** The Applicant's account is debited, and the SWIFT MT 202/103 remittance is generated to pay the Presenting Bank. Nostro/Vostro accounting entries are confirmed.
* **Exit Criteria:** System determines if the LC has remaining available balance.
* **Permitted Transitions:**
    * **Loop (Backward):** To `Issued` (This was a partial drawing, and the LC still has an available balance and unexpired validity for future presentations).
    * **Terminal:** To `Closed` (The LC is fully drawn, or within accepted tolerance limits. Final facility earmarks are released).

##### State 8: Closed / Cancelled
The terminal state. No further financial or operational actions can be taken on this specific transaction record.
* **Entry Criteria:** LC is fully drawn and settled, expires unutilized, is formally cancelled by all parties before expiry, or a draft is discarded.
* **Exit Criteria:** None. This is a terminal state.
* **Permitted Transitions:** None. (Note: Any historical queries or audits are performed on the record in this state, but business state transitions are locked).

***

### Section 3: Core Business Processes (Detailed Specification)

#### 3.1. Process: Import LC Issuance
This process covers the end-to-end flow from capturing the customer's application to legally issuing the Documentary Credit via SWIFT.

**A. Related States**
* **Transaction State (System):** Draft $\rightarrow$ Pending Approval $\rightarrow$ Active
* **LC Business State (Domain):** Draft $\rightarrow$ Issued

**B. Business Process Workflow**
1. **Maker Entry:** Operations receives the LC application (via branch, email, or corporate portal) and enters the data into the system.
2. **System Validation:** System cross-checks data against KYC, Sanctions, and Facility Limits.
3. **Submission:** Maker submits. Status changes to *Pending Approval*.
4. **Checker Review:** Checker accesses the queue, reviews system validations, and compares physical/digital source documents against system data.
5. **Authorization:** Checker approves.
6. **Execution:** System executes post-submit processing (deducts limits, generates MT 700). Status changes to *Issued*.

**C. Initiation & Post Conditions**
* **Initiation Criteria (Entry):** A valid customer mandate/application form is received. The Applicant has an active Customer Profile and KYC status.
* **Post Condition (Exit):** The bank is irrevocably bound to the LC terms. The LC liability is booked in the core banking system, and the MT 700 is dispatched to the Advising Bank.

**D. Inputs Capture (Data Dictionary)**

| Data Group | Field Name | Req/Opt | Data Type | Validation Rules / Data Constraints |
| :--- | :--- | :--- | :--- | :--- |
| **General** | Applicant ID | Req | String | Must match an active Customer in the Party Directory. KYC must be 'Clear'. |
| **General** | Beneficiary Name & Addr. | Req | Text | Max 4 lines of 35 characters (SWIFT standard). |
| **General** | Advising Bank BIC | Opt | String | Must be a valid SWIFT BIC in the Bank Directory. |
| **Dates** | Issue Date | Req | Date | Cannot be in the past. Defaults to current system business date. |
| **Dates** | Expiry Date | Req | Date | Must be $\ge$ Issue Date. |
| **Dates** | Latest Shipment Date | Opt | Date | Must be $\le$ Expiry Date. |
| **Dates** | Expiry Place | Req | String | Usually the country of the Beneficiary or Issuing Bank. |
| **Financial** | LC Currency | Req | String | Must be a 3-letter ISO Currency Code active in the system. |
| **Financial** | LC Amount | Req | Decimal | Must be $>$ 0. |
| **Financial** | Tolerance (+) % | Opt | Integer | e.g., 10 for 10%. Max limit typically 100. |
| **Financial** | Tolerance (-) % | Opt | Integer | e.g., 10 for 10%. Max limit typically 100. |
| **Terms** | Tenor Type | Req | Enum | Values: `Sight`, `Usance (Deferred)`, `Acceptance`, `Mixed`. |
| **Terms** | Usance Days | Cond | Integer | Required if Tenor is not `Sight`. |
| **Terms** | Partial Shipments | Req | Enum | Values: `Allowed`, `Not Allowed`, `Conditional`. |
| **Terms** | Transhipment | Req | Enum | Values: `Allowed`, `Not Allowed`, `Conditional`. |
| **Shipping** | Port of Loading | Opt | String | Name or code of the departure port. |
| **Shipping** | Port of Discharge | Opt | String | Name or code of the destination port. |
| **Text** | Description of Goods | Req | Text | Max 65,000 characters. Defines what is being bought. |
| **Text** | Documents Required | Req | Text | Defines exact documents Beneficiary must present (e.g., Bill of Lading, Invoice). |
| **Text** | Additional Conditions | Opt | Text | Custom clauses (e.g., "All documents must be in English"). |
| **Charges** | Charge Allocation | Req | Enum | Values: `All Applicant`, `All Beneficiary`, `Shared`. |

**E. Display / Computed Data**

| Field Name | Data Type | Calculation Formula / Processing Rules |
| :--- | :--- | :--- |
| **Base Equivalent Amount** | Decimal | `LC Amount` $\times$ `Current System Exchange Rate (LC Currency to Local Currency)`. Recalculated dynamically before submit. |
| **Maximum Liability Amount** | Decimal | `LC Amount` + (`LC Amount` $\times$ `Tolerance (+) %`). This is the true risk exposure amount. |
| **Available Facility Limit** | Decimal | Fetched live from Core Banking/Limit module: `Total Limit` - `Currently Utilized Amount`. |
| **Limit Check Status** | Boolean | `True` if `Available Facility Limit` $\ge$ `Maximum Liability Amount`. Otherwise `False` (blocks submission). |

**F. Additional Requirements (Inputs/Attachments)**
* **Scanned Application:** Mandatory upload of the signed physical application form or digital equivalent.
* **Proforma Invoice / Contract:** Optional upload of the underlying commercial contract between buyer and seller.

**G. Post-Submit Processing & Related Entities**
Upon Checker authorization:
1. **Facility Earmark:** System triggers a synchronous call to the Core Banking limit module to deduct the `Maximum Liability Amount` from the Applicant's trade facility.
2. **Cash Margin Hold:** If the Applicant does not have a sufficient credit line, the system triggers a hold on the Applicant's deposit account for the equivalent cash margin percentage.
3. **Fee Deduction:** System calculates issuance commission based on the bank's tariff matrix (e.g., 0.125% per quarter) and debits the Applicant's operating account.
4. **Entity Creation:** The core `TradeInstrument` and `ImportLc` records are formally committed to the database.

**H. Report / File Generation Requirements**
* **Customer Advice of Issuance:** System automatically generates a PDF advice detailing the issued LC terms, fees charged, and the official Transaction Reference Number. Sent via email/portal to the Applicant.
* **Internal GL Voucher:** A digital accounting voucher detailing the debit/credit legs for fees and contingent liabilities.

**I. Inbound / Outbound Integration**
* **Inbound:** * Sanctions Screening Engine (API call to verify all named parties and text fields).
    * Core Banking System (API call to fetch live exchange rates and available facility limits).
* **Outbound:** * SWIFT Gateway: Generation and transmission of the **MT 700 (Issue of a Documentary Credit)** message.
    * Core Banking System: API call to post accounting entries (fees, margin blocks, contingent liability booking).

***

#### 3.2. Process: Import LC Amendments
This process dictates how an active, issued Import LC is modified at the request of the Applicant, covering both financial changes (amount increases, expiry extensions) and non-financial changes (shipping details, text corrections).

**A. Related States**
* **Transaction State (System):** Draft (Amendment) $\rightarrow$ Pending Approval $\rightarrow$ Active
* **LC Business State (Domain):** The parent LC remains in the **Issued** state throughout. The system creates a linked "Amendment Record" that transitions from Draft $\rightarrow$ Pending Approval $\rightarrow$ Dispatched $\rightarrow$ Accepted/Rejected (by Beneficiary).

**B. Business Process Workflow**
1. **Initiation:** Operations locates the active LC and clicks "Initiate Amendment." 
2. **Delta Entry:** The Maker inputs only the fields that are changing (e.g., increasing the amount, changing the port).
3. **System Categorization:** The system automatically determines if the amendment is "Financial" or "Non-Financial" based on the edited fields.
4. **Submission:** Maker submits. Status changes to *Pending Approval*. The required Checker tier is dynamically calculated based on the *new total liability*.
5. **Checker Review & Authorization:** Checker reviews the amendment request against the customer's instructions and approves.
6. **Execution:** System deducts/releases limits (if financial), applies amendment fees, and generates the MT 707 message.
7. **Beneficiary Consent Tracking:** The amendment remains structurally pending until operations logs the Beneficiary's official acceptance or rejection.

**C. Initiation & Post Conditions**
* **Initiation Criteria (Entry):** The parent LC must be in an *Issued* state and must not have reached its Expiry Date. 
* **Post Condition (Exit):** An MT 707 is dispatched. The bank's liability is updated (if financial). The new terms become legally binding *only* upon logging the Beneficiary's acceptance.

**D. Inputs Capture (Data Dictionary)**
*Note: During an amendment, users only interact with "Delta" fields. If a field is left blank, the system assumes the original LC term remains unchanged.*

| Data Group | Field Name | Req/Opt | Data Type | Validation Rules / Data Constraints |
| :--- | :--- | :--- | :--- | :--- |
| **Control** | Amendment Date | Req | Date | Cannot be in the past. |
| **Financial** | Amount Increase | Opt | Decimal | Must be $>$ 0. Mutually exclusive with Decrease. |
| **Financial** | Amount Decrease | Opt | Decimal | Must be $>$ 0. Cannot exceed current Available Balance. |
| **Financial** | New Tolerance (+/-) | Opt | Integer | Overwrites previous tolerance percentage. |
| **Dates** | New Expiry Date | Opt | Date | Must be $\ge$ current Business Date. |
| **Dates** | New Latest Shipment | Opt | Date | Must be $\le$ New Expiry Date (if provided) or Original Expiry. |
| **Text** | Narrative / Changes | Cond | Text | Max 65,000 chars. Used for free-text changes to goods or documents. Required if no standard fields are changed. |
| **Charges** | Amendment Charges | Req | Enum | Values: `Applicant`, `Beneficiary`. Determines who pays the fee for this specific change. |
| **Consent** | Beneficiary Decision | Opt | Enum | Values: `Pending`, `Accepted`, `Rejected`. Initially defaults to `Pending`. |

**E. Display / Computed Data**

| Field Name | Data Type | Calculation Formula / Processing Rules |
| :--- | :--- | :--- |
| **Amendment Number** | Integer | Auto-incremented (e.g., Amendment 1, Amendment 2) linked to the parent LC. |
| **Original LC Amount** | Decimal | Fetched from the parent LC record. |
| **New Total LC Amount** | Decimal | `Original LC Amount` + `Amount Increase` - `Amount Decrease`. |
| **New Maximum Liability** | Decimal | (`New Total LC Amount`) + (`New Total LC Amount` $\times$ `New Tolerance (+) %`). |
| **Limit Delta Required** | Decimal | The difference between the original liability and the `New Maximum Liability`. If negative, limits will be released. |
| **Required Authority Tier**| String | Dynamically calculated by the Maker/Checker Matrix using the `New Maximum Liability` (not just the delta). |

**F. Additional Requirements (Inputs/Attachments)**
* **Customer Amendment Request:** Mandatory upload of the signed physical instruction or portal request from the Applicant.

**G. Post-Submit Processing & Related Entities**
Upon Checker authorization:
1. **Facility Delta Update:** If `Limit Delta Required` is positive, the system earmarks additional funds from the Core Banking facility. If negative, the system releases the excess earmark back to the facility.
2. **Fee Application:** System calculates standard amendment flat fees, plus any additional issuance commission (if the amount was increased or expiry extended into a new quarter), and debits the designated party.
3. **Amendment Record Commit:** The amendment delta is saved to the database, linked to the parent LC.

**H. Report / File Generation Requirements**
* **Customer Amendment Advice:** A PDF document detailing exactly what changed, what remained the same, and the fees charged.

**I. Inbound / Outbound Integration**
* **Inbound:** * Sanctions Screening Engine: Re-screening is mandatory if the `Narrative` text changes, or if shipping ports are amended.
* **Outbound:** * SWIFT Gateway: Generation and transmission of the **MT 707 (Amendment to a Documentary Credit)** message.
    * Core Banking System: API call to post accounting entries for limit adjustments and fee collection.

***

#### 3.3. Process: Document Presentation & Examination
This process manages the receipt of shipping and financial documents from the foreign Presenting Bank, the formal examination of those documents against the LC terms, and the logging of any discrepancies.

**A. Related States**
* **Transaction State (System):** Draft (Examination) $\rightarrow$ Pending Approval $\rightarrow$ Executed
* **LC Business State (Domain):** Issued $\rightarrow$ Documents Received $\rightarrow$ Discrepant *OR* Accepted/Clean

**B. Business Process Workflow**
1. **Lodgement:** The mailroom or digital gateway receives the documents. Operations creates a new "Presentation Record" linked to the parent LC and inputs the basic cover letter details. Status becomes *Documents Received*.
2. **Examination:** A Maker reviews the physical/scanned documents against the original LC terms and standard ISBP 745 rules. 
3. **Discrepancy Logging:** If the documents do not strictly comply, the Maker logs specific discrepancy codes. If compliant, the Maker marks the presentation as clean.
4. **Submission:** Maker submits the examination results. Status changes to *Pending Approval*.
5. **Checker Review:** Checker independently verifies the documents and the Maker's findings.
6. **Execution/Decision:**
    * **If Clean:** Checker authorizes. State shifts to *Accepted/Clean*.
    * **If Discrepant:** Checker authorizes the findings. State shifts to *Discrepant*. The system generates a notice to the Applicant requesting a waiver, and optionally prepares a SWIFT MT 734 (Notice of Refusal) for the Presenting Bank.

**C. Initiation & Post Conditions**
* **Initiation Criteria (Entry):** The parent LC must be in an *Issued* state and have an available unutilized balance. 
* **Post Condition (Exit):** The bank either formally accepts the documents (binding them to pay) or formally refuses the presentation due to unresolved discrepancies.

**D. Inputs Capture (Data Dictionary)**

| Data Group | Field Name | Req/Opt | Data Type | Validation Rules / Data Constraints |
| :--- | :--- | :--- | :--- | :--- |
| **Header** | Presenting Bank BIC | Req | String | Must be a valid SWIFT BIC. Defaults to the Advising Bank but can be overridden. |
| **Header** | Presentation Date | Req | Date | The exact date documents arrived at the bank's counter. Cannot be in the future. |
| **Header** | Presenting Bank Ref | Opt | String | The reference number from the foreign bank's cover letter. |
| **Financial** | Claim Amount | Req | Decimal | Must be $>$ 0. |
| **Financial** | Claim Currency | Req | String | Must match the parent LC currency. |
| **Documents** | Document Type Grid | Req | Array | Standard list (e.g., Bill of Lading, Commercial Invoice, Packing List, Origin Certificate). |
| **Documents** | Original Count | Req | Integer | Number of original copies received per document type. |
| **Documents** | Copy Count | Req | Integer | Number of photocopies received per document type. |
| **Compliance**| Discrepancy Found? | Req | Boolean | Determines the routing path (Clean vs. Discrepant). |
| **Compliance**| Discrepancy Details | Cond | Array | Required if Discrepancy Found = `True`. Must use standardized ISBP codes + free text description. |
| **Resolution**| Applicant Decision | Opt | Enum | Values: `Pending`, `Waived`, `Refused`. Used only if discrepant. |

**E. Display / Computed Data**

| Field Name | Data Type | Calculation Formula / Processing Rules |
| :--- | :--- | :--- |
| **Presentation Reference** | String | Auto-generated unique ID (e.g., PR-IMP-2026-001). |
| **Regulatory Deadline** | Date | `Presentation Date` + 5 Banking Days. *Critical UCP 600 hard-stop for examination.* |
| **Remaining LC Balance** | Decimal | `Parent LC Amount` - `Total Previously Accepted Claims`. |
| **Overdrawn Status** | Boolean | `True` if `Claim Amount` > (`Remaining LC Balance` + `Tolerance %`). |

**F. Additional Requirements (Inputs/Attachments)**
* **Document Scans:** Mandatory high-resolution upload of all presented documents (if received physically) or automated ingestion of digital documents (eUCP).
* **Cover Letter Upload:** Scan of the Presenting Bank's instruction letter.

**G. Post-Submit Processing & Related Entities**
Upon Checker authorization of the examination:
1. **Limit Update:** The system moves the utilized limit from a "contingent liability" to a "firm liability" (or "acceptance liability" for Usance LCs).
2. **SLA Tracking:** The system stops the 5-day regulatory countdown timer.
3. **Discrepancy Fee Application:** If discrepancies are found, the system automatically calculates and deducts a standard discrepancy fee (e.g., $100 USD equivalent) from the settlement proceeds or bills the Presenting Bank, per the LC terms.

**H. Report / File Generation Requirements**
* **Discrepancy Advice / Waiver Request:** A system-generated PDF or portal notification sent to the Applicant detailing the exact discrepancies and requesting their formal decision to waive or refuse.
* **Internal Examination Checklist:** A generated audit report showing who checked the documents and which rules were applied.

**I. Inbound / Outbound Integration**
* **Outbound:** * **SWIFT MT 734 (Notice of Refusal):** Automatically generated if the bank or applicant formally refuses the discrepant documents.
    * **SWIFT MT 750 (Advice of Discrepancy):** Generated to seek the Presenting Bank's instructions if the Applicant delays their waiver decision.

***

#### 3.4. Process: Settlement & Payment
This process governs the final movement of funds from the Applicant's accounts (or utilizing existing cash margins) to the foreign Presenting Bank, officially extinguishing the bank's liability for that specific presentation.

**A. Related States**
* **Transaction State (System):** Draft (Settlement) $\rightarrow$ Pending Approval $\rightarrow$ Executed
* **LC Business State (Domain):** Accepted/Clean $\rightarrow$ Settled

**B. Business Process Workflow**
1. **Initiation (Sight LCs):** Immediately upon authorizing a clean presentation (or an accepted waiver), operations initiates the settlement process.
2. **Initiation (Usance LCs):** Upon clean presentation, the system generates an MT 732 (Advice of Discharge) acknowledging the maturity date. On the actual maturity date, the system automatically queues the settlement transaction for operations review.
3. **Data Entry:** The Maker inputs/verifies the exchange rates, debit accounts, and exact remittance routing details.
4. **Submission:** Maker submits. Status changes to *Pending Approval*.
5. **Checker Review:** Checker verifies the payment details against the presentation claim and available applicant funds.
6. **Execution:** Checker authorizes. The system debits the Applicant, releases the contingent/acceptance liability, and generates the outward payment SWIFT.

**C. Initiation & Post Conditions**
* **Initiation Criteria (Entry):** The Presentation record must be in an *Accepted/Clean* state. For Usance LCs, the current system business date must equal the agreed Maturity Date.
* **Post Condition (Exit):** Bank funds are irrevocably transferred. The presentation claim is closed. If the parent LC is fully drawn, it transitions to a *Closed* state.

**D. Inputs Capture (Data Dictionary)**

| Data Group | Field Name | Req/Opt | Data Type | Validation Rules / Data Constraints |
| :--- | :--- | :--- | :--- | :--- |
| **Control** | Value Date | Req | Date | The date funds are to be made available to the receiver. Cannot be in the past. |
| **Financial** | Principal Payment Amount| Req | Decimal | Must equal the Accepted Claim Amount. |
| **Financial** | Remittance Currency | Req | String | Must match the LC/Claim currency. |
| **Financial** | Applicant Debit Account | Req | String | Must be a valid, active CASA (Current/Savings) account belonging to the Applicant. |
| **Financial** | Applied Margin Amount | Opt | Decimal | Amount of cash collateral to be utilized for this payment (if taken during issuance). |
| **Treasury** | FX Exchange Rate | Cond | Decimal | Required if the Debit Account currency differs from the Remittance Currency. |
| **Treasury** | Forward Contract Ref | Opt | String | Reference to a pre-booked Treasury FX contract to lock in the rate. |
| **Routing** | Receiver's Correspondent | Opt | String | SWIFT BIC of the bank where the Presenting Bank holds its account. |
| **Routing** | Intermediary Bank | Opt | String | SWIFT BIC for routed payments (often required for USD/EUR clearing). |
| **Charges** | Details of Charges (71A)| Req | Enum | Values: `OUR` (Applicant pays all), `BEN` (Beneficiary pays all), `SHA` (Shared). Defaults to LC terms. |

**E. Display / Computed Data**

| Field Name | Data Type | Calculation Formula / Processing Rules |
| :--- | :--- | :--- |
| **Total Debit Amount** | Decimal | (`Principal Payment Amount` $\times$ `FX Exchange Rate`) - `Applied Margin Amount` + `Calculated Bank Fees`. |
| **Nostro Account** | String | The bank's own foreign currency account used for the outbound remittance, auto-derived based on the Remittance Currency. |
| **Account Balance Check** | Boolean | `True` if `Applicant Debit Account` has sufficient funds to cover the `Total Debit Amount`. Prevents submission if `False`. |

**F. Additional Requirements (Inputs/Attachments)**
* **Applicant Payment Instruction:** If required by local bank policy, a scanned authorization from the customer confirming the debit of their account (particularly for sight waivers).

**G. Post-Submit Processing & Related Entities**
Upon Checker authorization:
1. **Liability Reversal:** The system completely reverses the firm/acceptance liability associated with this specific claim amount.
2. **Margin Release:** If cash margin was utilized, the system releases the block on the margin account and transfers the funds to the settlement suspense account.
3. **Core Debit:** The system debits the `Total Debit Amount` from the Applicant's operating account.
4. **Parent LC Update:** The system deducts the settled amount from the parent LC. If the remaining balance is zero (within tolerance), the parent LC state shifts to *Closed*.

**H. Report / File Generation Requirements**
* **Customer Debit Advice:** A formal PDF receipt generated for the Applicant, detailing the exchange rate applied, total local currency debited, and breakdown of final settlement fees.
* **Payment GL Voucher:** Internal accounting record capturing the Nostro credit, Customer debit, and Fee income entries.

**I. Inbound / Outbound Integration**
* **Inbound:** * Core Banking System: Real-time balance check on the Applicant's CASA account and validation of live FX board rates.
* **Outbound:** * **SWIFT MT 202 (General Financial Institution Transfer):** Generated if paying bank-to-bank where the Presenting Bank is the direct beneficiary of the funds.
    * **SWIFT MT 103 (Single Customer Credit Transfer):** Generated if the payment is routed directly to the Beneficiary's account.
    * **SWIFT MT 732 (Advice of Discharge):** Generated automatically upon *Acceptance* of a Usance draft, acknowledging the future payment date.

---

#### 3.5. Process: Shipping Guarantees (SG) & Air Waybill (AWB) Endorsements
This process allows the Applicant to take possession of imported goods before the official shipping documents arrive at the bank. The bank issues a formal indemnity (SG) to the shipping line (for sea freight) or officially endorses the transport document (for air freight), thereby taking on the liability for the goods.

**A. Related States**
* **Transaction State (System):** Draft (SG Issuance) $\rightarrow$ Pending Approval $\rightarrow$ Executed
* **SG Sub-State:** Issued $\rightarrow$ Redeemed/Closed
* **LC Business State Impact:** The parent LC remains *Issued*, but the system places a hard lock on the utilized limit, and future discrepancy waivers are pre-empted.

**B. Business Process Workflow**
1. **Application:** The Applicant submits an SG request along with a copy of the commercial invoice and non-negotiable transport document.
2. **Data Entry:** The Maker links the SG request to the active parent LC and inputs the shipping details.
3. **Limit Assessment:** The system calculates the SG Liability (often 110% to 150% of the invoice value to cover potential shipping line claims).
4. **Submission & Approval:** Maker submits; Checker authorizes based on the elevated risk tier.
5. **Issuance:** System earmarks the facility limits and generates the physical SG indemnity form (or AWB endorsement letter) to be signed and stamped by the bank.
6. **Redemption (Future Event):** When the official documents finally arrive (triggering Process 3.3), the bank must accept them. The bank then hands the original Bill of Lading to the Applicant in exchange for the return of the physical SG document. The SG is then marked *Redeemed* in the system.

**C. Initiation & Post Conditions**
* **Initiation Criteria (Entry):** The parent LC must be active (*Issued*). No physical documents for this specific shipment can have been logged in the system yet.
* **Post Condition (Exit):** The bank is legally indemnifying the carrier. The Applicant legally forfeits the right to refuse the eventual document presentation, even if discrepancies are later found.

**D. Inputs Capture (Data Dictionary)**

| Data Group | Field Name | Req/Opt | Data Type | Validation Rules / Data Constraints |
| :--- | :--- | :--- | :--- | :--- |
| **Header** | SG Type | Req | Enum | Values: `Shipping Guarantee (Sea)`, `AWB Endorsement (Air)`. |
| **Header** | SG Issue Date | Req | Date | Defaults to current business date. |
| **Financial** | Invoice Amount | Req | Decimal | The value of the goods being claimed. |
| **Financial** | SG Liability % | Req | Integer | Standard is 100%. For sea freight, risk policy often dictates 110%, 125%, or 150%. |
| **Shipping** | Transport Doc Ref | Req | String | The Bill of Lading (B/L) or Air Waybill (AWB) number. |
| **Shipping** | Carrier / Agent Name | Req | String | The shipping line to whom the guarantee is addressed. |
| **Shipping** | Vessel Name & Voyage | Cond | String | Required if SG Type is `Shipping Guarantee (Sea)`. |
| **Shipping** | Goods Description | Req | Text | Brief description of the goods being released. |

**E. Display / Computed Data**

| Field Name | Data Type | Calculation Formula / Processing Rules |
| :--- | :--- | :--- |
| **SG Liability Amount** | Decimal | `Invoice Amount` $\times$ (`SG Liability %` / 100). |
| **Required Facility Earmark** | Decimal | The system checks if `SG Liability Amount` > `Parent LC Available Balance`. If yes, the difference must be earmarked from the customer's facility or taken as cash margin. |
| **Waiver Lock Flag** | Boolean | Set to `True`. Systematically prevents the Applicant from refusing the subsequent document presentation for this specific invoice amount. |

**F. Additional Requirements (Inputs/Attachments)**
* **Applicant Indemnity:** Mandatory upload of the Applicant’s signed counter-indemnity (protecting the bank).
* **Copy of Transport Document:** Upload of the copy B/L or AWB provided by the Applicant.

**G. Post-Submit Processing & Related Entities**
Upon Checker authorization:
1. **Facility Update:** The system locks the required `SG Liability Amount`. This limit *cannot* be released when the parent LC expires; it can only be released when the physical SG is returned.
2. **Commission Application:** The system calculates and deducts the SG issuance commission (often a flat fee plus a monthly recurring percentage until the SG is redeemed).
3. **Cross-Process Rule Enforcement:** The system updates the parent LC. When Process 3.3 (Document Presentation) is later initiated for this specific Transport Doc Ref, the system must automatically bypass the "Applicant Decision" state and force the presentation to *Accepted*.

**H. Report / File Generation Requirements**
* **Shipping Guarantee Instrument:** A rigidly formatted, legally binding PDF document addressed to the Carrier, containing standard ICC/local banking indemnity clauses.
* **Customer Advice:** Receipt of SG issuance and fees charged.

**I. Inbound / Outbound Integration**
* **Outbound:** * Core Banking System: API call to book the SG contingent liability and collect fees. (No SWIFT messages are generated for SGs, as they are local indemnities given to local shipping agents).

***

#### 3.6. Process: Cancellations, Expiry, and Limit Reversal

This is the final lifecycle event for an Import LC. It is critical from a risk and capital perspective because until an LC is formally closed in the system, the bank must hold capital against the contingent liability, and the customer cannot use those facility limits for other business.
This process dictates the strict business rules under which an active Import LC can be formally closed, and its associated credit limits and cash margins can be released back to the customer.

**A. Related States**
* **Transaction State (System):** Draft (Cancellation) $\rightarrow$ Pending Approval $\rightarrow$ Executed 
* **LC Business State (Domain):** Issued $\rightarrow$ Closed / Cancelled

**B. Business Process Workflow**
The system must handle three distinct closure scenarios:

1. **Auto-Closure (Fully Drawn):** * When a settlement process (Process 3.4) brings the unutilized LC balance to zero (accounting for tolerances), the system automatically transitions the parent LC state to *Closed* without requiring a separate Maker/Checker action.
2. **Auto-Expiry (Unutilized / Partially Utilized):**
   * The system runs an End-of-Day (EOD) batch process to identify LCs that have passed their Expiry Date. 
   * The system adds a configurable "Mail Days" grace period (e.g., Expiry Date + 15 calendar days) to account for documents that were presented overseas on the expiry date but are still in transit via courier.
   * After the grace period expires with no presentations logged, the system auto-closes the LC and releases the remaining limits.
3. **Early Mutual Cancellation:**
   * **Initiation:** The Applicant requests to cancel the LC before expiry (e.g., the underlying commercial contract fell through).
   * **Consent Request:** Operations logs the request and generates a SWIFT message to the Advising Bank requesting the Beneficiary's formal consent. (An LC is irrevocable; it cannot be cancelled unilaterally by the buyer).
   * **Execution:** Once the Advising Bank returns an authenticated SWIFT confirming the Beneficiary's consent, operations submits the cancellation for Checker authorization. Limits are then released.

**C. Initiation & Post Conditions**
* **Initiation Criteria (Entry):** The LC must be *Issued*. There cannot be any pending, un-examined, or discrepant presentations currently active against the LC.
* **Post Condition (Exit):** The LC reaches its terminal state (*Closed/Cancelled*). Core banking credit limits are restored.

**D. Inputs Capture (Data Dictionary)**
*(Note: These inputs primarily apply to the manual "Early Mutual Cancellation" scenario).*

| Data Group | Field Name | Req/Opt | Data Type | Validation Rules / Data Constraints |
| :--- | :--- | :--- | :--- | :--- |
| **Control** | Closure Type | Req | Enum | Values: `Fully Drawn`, `Expired`, `Early Cancellation`. |
| **Control** | Closure Date | Req | Date | Defaults to current business date. |
| **Financial** | Cancelled Amount | Req | Decimal | The exact unutilized balance being cancelled. |
| **Consent** | Beneficiary Consent | Cond | Enum | Values: `Pending`, `Consented`, `Refused`. Required for `Early Cancellation`. |
| **Consent** | Consent SWIFT Ref | Cond | String | The reference number of the incoming SWIFT MT 799/730 confirming Beneficiary agreement. |
| **Charges** | Cancellation Fee | Opt | Decimal | Flat fee charged for early processing. |

**E. Display / Computed Data**

| Field Name | Data Type | Calculation Formula / Processing Rules |
| :--- | :--- | :--- |
| **Mail Days Grace Period** | Date | `LC Expiry Date` + `System Parameter (e.g., 15 Days)`. Hard-stop date before auto-expiry runs. |
| **Total Limit to Release** | Decimal | `Cancelled Amount` + (`Cancelled Amount` $\times$ `Tolerance (+) %`). |
| **Margin to Release** | Decimal | The proportional amount of cash collateral tied to the cancelled balance. |

**F. Additional Requirements (Inputs/Attachments)**
* **Applicant Instruction:** Mandatory upload of the formal request to cancel early.
* **Beneficiary Consent:** Mandatory linkage to the incoming authenticated SWIFT message agreeing to the cancellation.

**G. Post-Submit Processing & Related Entities**
Upon Checker authorization (or Batch execution):
1. **Limit Reversal:** The system communicates with the Core Banking module to restore the `Total Limit to Release` back to the customer's Available Earmark.
2. **Margin Release:** If the LC was cash-backed, the system removes the hold on the Applicant's deposit account.
3. **State Lock:** The LC state changes to *Closed*. The system must strictly block any new document presentations, amendments, or fee additions against this Transaction Reference Number.

**H. Report / File Generation Requirements**
* **Cancellation Advice:** A formal PDF notice sent to the Applicant confirming the LC is permanently closed and detailing the exact monetary limits released back to their facility.

**I. Inbound / Outbound Integration**
* **Inbound:** * Core Banking System: EOD triggers for the auto-expiry batch.
    * SWIFT Gateway: Parsing incoming MT 799 (Free Format Message) or MT 730 (Acknowledgment) for Beneficiary consent.
* **Outbound:** * SWIFT Gateway: Generating an MT 799 (or sometimes an MT 707 Amendment to decrease the amount to zero) requesting the Beneficiary's agreement to cancel.
    * Core Banking System: API calls to execute limit reversals and margin releases.

***
