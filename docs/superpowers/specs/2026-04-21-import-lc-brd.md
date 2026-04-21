# Business Requirements Document (BRD)

**Project Name:** Digital Trade Finance Platform
**Module:** Import Letter of Credit (LC)
**Document Version:** 2.0 (Refined & Formalization)
**Date:** April 21, 2026

---

## 1. Module Overview & Terminology
The Import LC module facilitates the issuance and entire lifecycle management of Documentary Credits on behalf of the bank's importing customers (Applicants), acting in accordance with UCP 600 guidelines.

### REQ-IMP-DEF-01: State Terminology Separation
The system MUST rigorously decouple the generic approval workflow from the instrument's legal lifecycle:
1. **Transaction State (System & Workflow Level):** Inherited from the Common Module, dictating the Maker/Checker flow (e.g., *Draft, Pending Approval, Active*). Controls system access routing.
2. **LC Business State (Domain & Product Level):** Dictates the operational reality of the specific trade instrument (e.g., *Issued, Documents Received, Discrepant, Settled, Closed*). Controls UCP 600 compliance boundaries.

---

## 2. Business State Machine & Transitions

### REQ-IMP-STATE-01: Permitted State Transitions
The system MUST strictly enforce the transition matrix for the LC Business State. A business object cannot bypass defined states.

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

### REQ-IMP-STATE-02: State Operational Criteria
The entry and exit thresholds for each state MUST adhere to the following logic:

* **Draft:** 
  * *Exit:* All mandatory fields (Applicant, Beneficiary, Amount, Currency, Expiry Date, Tenor, Required Docs) complete. Expiry Date > Past.
* **Pending Approval:** 
  * *Entry:* Maker submissions or Amendment triggers.
  * *Exit:* Checker authorization validates against customer facility blocks and screening.
* **Issued:** 
  * *Entry:* Checker authorized. MT700 dispatched. Liability locked on core books.
  * *Exit:* Documents presented, or unutilized expiration.
* **Documents Received:** 
  * *Exit:* Exam completed within strict maximum 5-banking-day UCP timer.
* **Discrepant:** 
  * *Entry:* Standard ISBP discrepancy codes logged. Applicant notice generation.
  * *Exit:* Applicant Waiver signed, or Bank Refusal logged.
* **Accepted / Clean:** 
  * *Entry:* Clean exam, or Waiver accepted by bank credit team.
  * *Exit:* Triggers final payment / scheduled Usance maturity.
* **Settled:** 
  * *Entry:* Applicant debited, MT202/103 generated, Nostro confirmed.
* **Closed / Cancelled:** 
  * *Entry:* Fully drawn, auto-expired, or mutually cancelled. Total limit reversed.

---

## 3. Core Business Processes

### REQ-IMP-PRC-01: Process - LC Issuance
Responsible for capturing the initial customer application and generating the legal undertaking (MT700).

**A. Related System Hooks**
* **System State:** Draft → Pending Approval → Active
* **Business State:** Draft → Issued

**B. Workflow & Execution Logic**
1. **Capture:** Operations / Portal inputs mandatory field data.
2. **Pre-Check:** System checks KYC, Sanctions, available Limits.
3. **Authorization:** Maker submits, Checker verifies source documents against system.
4. **Execution:** System deducts core limit (Maximum Liability Amount), holds required cash margins, applies tariffs, and broadcasts MT700.

**C. Data Dictionary (Issuance)**
| Field Name | Req | Data Type | Validation Rules / Logic |
| :--- | :--- | :--- | :--- |
| `Applicant ID` | Req | String | Matches active Party. KYC = Clear. |
| `Beneficiary Name/Addr` | Req | Text | Max 4 lines of 35 chars. |
| `Advising Bank BIC` | Opt | String | Valid SWIFT BIC. |
| `Issue Date` | Req | Date | $\ge$ Current System Date. |
| `Expiry Date` | Req | Date | $\ge$ Issue Date. |
| `Expiry Place` | Req | String | Typically Beneficiary country. |
| `LC Currency & Amount` | Req | Decimal | Base currency ISO. Amount > 0. |
| `Tolerance (+/-) %` | Opt | Integer | e.g. 10 for 10% tolerance bounds. |
| `Tenor Type` | Req | Enum | Sight, Usance (Deferred), Acceptance, Mixed. |
| `Usance Days` | Cond | Integer | Required if Tenor $\ne$ Sight. |
| `Partial Ship / Tranship`| Req | Enum | Allowed, Not Allowed, Conditional. |
| `Loading/Discharge Port` | Opt | String | Origin and destination nodes. |
| `Description of Goods` | Req | Text | Free text (Max 65,000 chars). |
| `Documents Required` | Req | Text | Explicit list of required presentations. |
| `Charge Allocation` | Req | Enum | All Applicant, All Beneficiary, Shared. |

**D. Core Formulas**
* `Maximum Liability Amount` = `LC Amount` + (`LC Amount` $\times$ `Tolerance (+) %`).
* `Available Facility Limit` = Live fetch: `Total Customer Limit` - `Currently Utilized Amount`.

---

### REQ-IMP-PRC-02: Process - LC Amendments
Modifications to active LCs.

**A. Workflow & Constraint Logic**
1. System categorizes amendment as Financial vs Non-Financial.
2. User only inputs "Delta" fields. Blank fields inherit from Parent LC.
3. If Financial, Checker tier recalculates based on the **new total liability**, not just delta.
4. System dispatches MT707. Formal status change pends Beneficiary Consent tracking.

**B. Data Dictionary (Delta)**
| Field Name | Req | Data Type | Validation Rules / Logic |
| :--- | :--- | :--- | :--- |
| `Amount Increase/Decrease`| Opt | Decimal | Mutually exclusive. Cannot decrease > available balance. |
| `New Expiry Date` | Opt | Date | $\ge$ Current System Date. |
| `Narrative / Changes` | Cond | Text | Used for free-text changes. Requires Sanctions re-screening if populated! |

---

### REQ-IMP-PRC-03: Process - Document Presentation & Exam
Receipt and evaluation of shipping/financial documents from Presenting Bank.

**A. Workflow & UCP 600 Engine**
1. Operations generates new linked "Presentation Record". System sets status *Documents Received*.
2. Regulatory SLA Engine instantly begins 5-banking-day countdown.
3. Maker evaluates against ISBP codes. Logs as Clean or Discrepant.
4. Checker authorizes findings. If discrepant, internal waiver triggered. MT734 (Refusal) pre-flighted.

**B. Data Dictionary (Examination)**
| Field Name | Req | Data Type | Validation Rules / Logic |
| :--- | :--- | :--- | :--- |
| `Presenting Bank BIC` | Req | String | Valid SWIFT BIC. |
| `Presentation Date` | Req | Date | $\le$ Current System Date. Timer baseline. |
| `Claim Amount` | Req | Decimal | Must match LC CCY. |
| `Discrepancy Found?` | Req | Boolean | Drives workflow fork (Clean vs Waiver flow). |
| `Discrepancy Details` | Cond | Array | Standardized ISBP lookup codes + free text. |
| `Applicant Decision` | Opt | Enum | Pending, Waived, Refused. |

---

### REQ-IMP-PRC-04: Process - Settlement & Payment
Final cash movement extinguishing the bank's presentation liability.

**A. Workflow & Logic**
1. Sight LC: Clean authorization immediately queues Settlement Draft.
2. Usance LC: Clean authorization yields MT732. System auto-queues Settlement Draft strictly on `Maturity Date`.
3. Execution debits Applicant's operating account, consumes Cash Margin blocks via suspended suspense.
4. Nostro MT202/103 generated. Utilization bounds updated or released.

**B. Data Dictionary (Settlement)**
| Field Name | Req | Data Type | Validation Rules / Logic |
| :--- | :--- | :--- | :--- |
| `Value Date` | Req | Date | $\ge$ System Date. |
| `Principal Payment Amount`| Req | Decimal | Exactly equals Accepted Claim Amount. |
| `Applicant Debit Account` | Req | String | Active CASA account validation. |
| `FX Exchange Rate` | Cond | Decimal | Required if Debit CCY $\ne$ Remittance CCY. Evaluated against Core Board. |
| `Details of Charges` | Req | Enum | OUR, BEN, SHA (SWIFT 71A limits). |

---

### REQ-IMP-PRC-05: Process - Shipping Guarantees (SG)
Enables Applicant to take goods before formal documents arrive via Bank Indemnity.

**A. Logic Constraints**
1. Evaluates parent LC (must be un-drawn). SG Liability calculates at 110% to 150% of invoice.
2. Hard earmarks the SG liability independently against the customer facility limit.
3. **Hard-Stop Rule:** Locks the parent LC presentation logic: Applicant formally forfeits discrepancy waiver rights. Future linked document presentation must be systematically forced to `Accepted/Clean`.

---

### REQ-IMP-PRC-06: Cancellations & Expiry Reversals
Closure rules releasing final contingent capital.

**A. System Constraints**
* **Auto-Closure:** Triggered internally if parent LC unutilized balance hits exactly 0 post-settlement.
* **Auto-Expiry:** System EOD batch sweeps LCs passing `Expiry Date` + `Configurable Mail Grace Days`. Fully closes and releases Limits automatically.
* **Mutual Cancellation:** Requires validated Swift MT799/730 Beneficiary Consent before limits can be restored.

---

## 4. SWIFT MT 700 Generation Mapping

### REQ-IMP-SWIFT-01: Issuance Tag Data Map
To guarantee straight-through processing, the system MUST transform the internally captured LC Issuance data into the rigid SWIFT X format using the following mapping.

| SWIFT Tag | Description | M/O | System Source / Transformation Logic |
| :--- | :--- | :--- | :--- |
| **20** | Documentary Credit No. | M | `TradeInstrument.transactionRef`. System-generated ID. |
| **27** | Sequence of Total | M | Default `1/1`. Unless 45A/46A text exceeds limits requiring MT701. |
| **31C** | Date of Issue | M | `TradeInstrument.issueDate`. Format explicitly `YYMMDD`. |
| **31D** | Date and Place of Expiry | M | Concatenate: `Expiry Date` (YYMMDD) + `Expiry Place`. |
| **32B** | Currency Code, Amount | M | Add base ISO code. Ensure comma `,` used as decimal separator. |
| **39A** | Percentage Tolerance | O | `Tolerance (+) %` + `/` + `Tolerance (-) %`. |
| **40A** | Form of Doc Credit | M | Product Type logic map (e.g., `IRREVOCABLE`). |
| **41a** | Available With ... By ... | M | Maps Advising Bank lookup + Tenor (e.g., `BY PAYMENT`). |
| **42C** | Drafts at (Tenor) | O | `Usance Days` + " DAYS AFTER " + base date logic. |
| **43P** | Partial Shipments | O | Map Enum directly to `ALLOWED`, `NOT ALLOWED`. |
| **43T** | Transhipment | O | Map Enum directly to `ALLOWED`, `NOT ALLOWED`. |
| **44E** | Port of Loading | O | `Port of Loading` (Max 65 chars). |
| **44F** | Port of Discharge | O | `Port of Discharge` (Max 65 chars). |
| **45A** | Description of Goods | M | Hardwrap text at exactly 65 characters per line (`\r\n`). |
| **46A** | Documents Required | M | Format as list. Hardwrap at 65 characters per line. |
| **47A** | Additional Conditions | O | Hardwrap at 65 characters per line. |
| **49** | Confirmation Instructions| M | Enum map to `CONFIRM`, `MAY ADD`, `WITHOUT`. |
| **50** | Applicant | M | `Applicant.legalName` + Address. Wrap to `4x35` lines. |
| **59** | Beneficiary | M | `Beneficiary.legalName` + Address. Wrap to `4x35` lines. |

---
*End of Document*