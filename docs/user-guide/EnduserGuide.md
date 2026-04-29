# Participant Operating Guide: Digital Trade Finance

This guide provides exhaustive, step-by-step instructions for Bank Makers and Checkers. The platform enforces **"Clean at Capture"** principles through proactive validation, ensuring every field adheres to SWIFT MT7xx standards before submission.

---

## 1. Navigation Route Map
The platform is organized into functional modules accessible via the **Sidebar**.

### OPERATIONS (Cross-Product Workflow)
- **Transaction Dashboard**: Unified view of all transactions (Maker/Checker) across all product lines.
- **My Tasks**: Checker queue for authorizing transactions (requires `TRADE_CHECKER` role).
- **Doc Examination**: Specialized workflow for verifying physical documents against LC terms.

### IMPORT LC (Product-Specific Management)
- **Import LC Dashboard**: Central command for tracking current legal states of Import instruments.
- **New LC Issuance**: Entry point for creating fresh MT 700 draft instruments.
- **LC Amendments**: Capture delta changes (Amount, Expiry, Terms) via MT 707.
- **Presentations**: Log beneficiary document claims and track 5-day examination SLAs.
- **Settlements**: Finalize payment and release credit facility earmarks.
- **Shipping Guarantees**: Issue guarantees for cargo release before docs arrival.
- **Cancellations**: Request formal termination of an active instrument.

### MASTER DATA & ADMIN
- **Party & KYC Directory**: Manage corporate client records, BICs, and compliance status.
- **Credit Facilities**: Real-time monitoring of bank-wide limits and utilization.
- **Product & Tariff Config**: System-level pricing and logic templates.

---

## 2. Vertical Asset Management (REQ-NAV-01.2)
Unlike the operational Dashboards which show pending work, the **Vertical Asset Lists** are designed for browsing the current legal state of trade products.

### Accessing Vertical Lists
Navigate to the specific product group in the sidebar to view its dedicated master list:
- **Import LCs**: Accessible via `IMPORT LC > Import LC Dashboard`. This list provides a filtered view of all active and historical Import Letters of Credit.
- **Shipping Guarantees**: Accessible via `IMPORT LC > Shipping Guarantees`.
- **Other Verticals**: Export LCs and Standby LCs are accessible via their respective sidebar groups (where implemented).

### Global Transaction Log (REQ-NAV-01.3)
Access the **Global Transaction Log** via `ADMINISTRATION > Audit Logs`. This view provides a chronologically sorted, priority-aware list of all operational actions. It allows officers to trace decisions back to specific Maker/Checker IDs and view the data delta for every change.

### Functional Features
- **Asset Snapshot**: Unlike transaction views, these lists always show the **Approved Version** of the instrument.
- **Exposure Tracking**: Integrated with **REQ-NAV-01.1**, these views provide the primary source of truth for bank exposure at the instrument level.
- **Deep Link Navigation**: Click any Reference Number to enter the **Unified Narrative Timeline** for that specific asset.

---

## 3. Dashboard Actions & Workflows
The **Operations Dashboard** is the primary launchpad for instrument-level actions.

### Data Interaction
1.  **Filter & Search**: Use the **Status Filter** (Draft, Pending, Issued, Doc Received) or the **Global Search** bar.
    - **Context Toggle**: Select **Inst** to search by Instrument ID (legal asset) or **Txn** to search by Transaction Ref (active workflow).
2.  **View Detail Page**: Click any **Ref No** (underlined blue text) to exit the dashboard and enter the **Dedicated Full-Screen View**.
3.  **SLA Health**: High-priority transactions with <3 days remaining are highlighted in **Urgent Red** in the "SLA Timer" column.

### Row-Level Context Actions (•••)
Click the triple-dot menu on any dashboard row to access status-specific actions:

| Action | Availability | Description |
|--------|--------------|-------------|
| **Edit Draft** | `LC_DRAFT` only | Resumes the Issuance Stepper. |
| **View Details** | All Statuses | Opens the high-fidelity audit view. |
| **New Amendment** | `LC_ISSUED` | Starts an MT 707 lifecycle change. |
| **Present Docs** | `LC_ISSUED` | Logs a document presentation. |
| **Initiate Settle** | `DOC_RECEIVED` | Starts the payment authorization. |
| **Req. Cancellation**| `LC_ISSUED` | Formally closes the LC before expiry. |

---

## 4. Instrument Detail View (Workspace)
Clicking a reference opens the **Full-Screen Workspace**. This view is optimized for audit and control.

### Navigation within Details
The workspace features a dual-mode layout:
- **Current State**: Static view of the instrument's active data (Parties, Financials, Terms).
- **Audit Narrative**: Chronological **Unified Timeline** merging financial transactions and technical audit logs.

### Workspace Actions
The **Workspace Actions** sidebar card provides contextual buttons:
1.  **Continue Editing Draft**: Only visible for instruments in `LC_DRAFT`.
2.  **Export Audit Document**: Generates a high-fidelity print-ready report of the transaction.
3.  **Action Buttons**: For Issued LCs, buttons for **Amend**, **Present**, and **Settle** are located at the bottom or in the sidebar depending on screen size.

---

## 5. Authorization (Checker Workflow)
Users with the `TRADE_CHECKER` role can authorize transactions via the **My Tasks** section.

### The Approvals Queue
1.  **Priority Sorting**: Items are sorted by **Urgent**, **High**, **Medium**, and **Low** status based on transaction weight.
2.  **SLA Alerts**: Items pending for >4 hours are flagged as **SLA Alerts**.
3.  **Details Review**: Click the **Eye (👁️)** icon to open the **Checker Authorization Workspace**.

### Authorizing an Instrument
1.  Click **Authorize** to enter the high-fidelity workspace.
2.  **Compare Proposed vs. Current**: The workspace displays the instrument data *as it will look* after the transaction (Proposed) alongside current recorded values.
3.  **Submit Decision**: Actions are performed against the unique **Transaction ID**, ensuring atomicity regardless of the instrument's future versioning.

---

## 6. Resuming Drafts
The platform ensures no data is lost during the issuance process.

1.  Navigate to the **Dashboard**.
2.  Select **Status: Draft** from the filter dropdown.
3.  Click the **Actions (•••)** menu on your draft record.
4.  Select **Edit Draft**.
5.  **Persistence**: The stepper saves your progress automatically. You can jump directly to the last active step (e.g., Step 4: Narratives).

---

## 7. Import LC Issuance (Maker Workflow)

### Step 1: Parties & Limits
| Field | Tag | Requirement | Character Set | Input Guideline / Constraints |
|-------|-----|-------------|---------------|-------------------------------|
| **LC Type** | 40A | **Mandatory** | - | Select SIGHT or USANCE. Determines if Usance Days are required. |
| **Confirmation** | 49 | Optional | - | Select CONFIRM, MAY ADD, or WITHOUT (Default). |
| **Product Catalog**| - | **Mandatory** | - | Select the generic LC type to load default SLA/Tenor templates. |
| **Transaction Ref**| 20 | Optional | **X-Charset** | Bank's internal reference. **Format**: `TF-IMP-YY-NNNN` (e.g., `TF-IMP-26-0001`). |
| **Applicant** | 50 | **Mandatory** | **X-Charset** | Select corporate client. Name/Address mapped to **Max 4 Lines**. |
| **Beneficiary** | 59 | **Mandatory** | **X-Charset** | Enter manually or select. Name/Address mapped to **Max 4 Lines**. |
| **Advising Bank** | 57A | **Mandatory** | **BIC-11/8** | Enter 8 or 11 char BIC. System auto-verifies Bank Name. |
| **Adv. Thru Bank** | 58A | Optional | **BIC-11/8** | Enter BIC for intermediary bank (Advising Through). |
| **Credit Facility**| - | **Mandatory** | - | Must have sufficient available balance. Blocks flow if exceeded. |

---

### Step 2: Main LC Information (Financials & Dates)
| Field | Tag | Requirement | Character Set | Input Guideline / Constraints |
|-------|-----|-------------|---------------|-------------------------------|
| **Currency** | 32B | **Mandatory** | - | Select base currency (USD, EUR, etc.). |
| **LC Amount** | 32B | **Mandatory** | **Numeric** | Enter face value of the instrument. |
| **Positive Tol. %**| 39A | Optional | **Numeric** | Percentage deviation above face value. |
| **Negative Tol. %**| 39A | Optional | **Numeric** | Percentage deviation below face value. Disabled if 39B checked. |
| **Max Credit Amt** | 39B | Optional | **Indicator**| If "Y", instrument allows amounts exceeding face value. |
| **Issue Date** | 31C | Optional | **Date** | Default is today. Determines effective date. |
| **Expiry Date** | 31D | **Mandatory** | **Date** | Date of instrument expiration. Must be > Issue Date. |
| **Expiry Place** | 31D | **Mandatory** | **X-Charset** | City/Country where the instrument expires. |
| **Latest Shipment**| 44C | Conditional | **Date** | Mandatory if 44D is empty. Must be $\leq$ Expiry. |
| **Shipment Period**| 44D | Conditional | **X-Charset** | Narrative shipment window. Mandatory if 44C is empty. |
| **Usance Days** | 42C | Conditional | **Numeric** | Required if LC Type is USANCE. |
| **Available By** | 41a | **Mandatory** | - | Select Payment Type (e.g., SIGHT, ACCEPTANCE, NEGOTIATION). |
| **Available With** | 41A/D| **Mandatory** | **BIC or X** | **BIC (41A)** or **Narrative Name (41D)**. Name max **4 lines**. |
| **Drawee Bank** | 42A | Optional | **BIC-11/8** | BIC of the bank that will accept/pay the drafts. |
| **Partial Ship.** | 43P | Optional | - | Select ALLOWED or NOT ALLOWED. |
| **Transhipment** | 43T | Optional | - | Select ALLOWED, NOT ALLOWED, or CONDITIONAL. |
| **Port of Loading**| 44E | Optional | **X-Charset** | Port/Airport of taking in charge/dispatch. |
| **Port of Disch.** | 44F | Optional | **X-Charset** | Port/Airport of destination. |
| **Goods Desc.** | 45A | **Mandatory** | **X-Charset** | Detailed list of merchandise and Incoterms. |
| **Docs Required** | 46A | **Mandatory** | **X-Charset** | List of required evidence (e.g., Bill of Lading, Invoice). |
| **Issuing Bank** | 51A | Optional | **BIC-11/8** | BIC of the bank issuing the instrument (if not self). |

---

### Step 3: Margin & Charges
| Field | Tag | Requirement | Character Set | Max Lines | Input Guideline |
|-------|-----|-------------|---------------|-----------|-----------------|
| **Margin Type** | - | Optional | - | - | Select None, Cash, or Lombard. |
| **Margin %** | - | Optional | **Numeric** | - | Percentage of amount to be earmarked as cash margin. |
| **Charge Alloc.** | 71D | **Mandatory** | - | - | Select who pays fees (Applicant, Beneficiary, or Shared). |
| **Cust. Facility** | - | **Mandatory** | - | - | Linked credit limit for liability tracking. |
| **Add. Conditions**| 47A | Optional | **X-Charset** | 100 | Special bank instructions or specific local regulations. |
| **Bank to Bank** | 78 | Optional | **X-Charset** | 12 | Instructions to the paying/accepting/negotiating bank. |
| **Pres. Period** | 48 | Optional | **Numeric** | 1 | Days after shipment to present docs (Default 21). |
| **Charges** | 71D | Optional | **X-Charset** | 6 | Define fee responsibility (e.g., All outside bank for Ben). |

---

## 8. Document Presentation Lodgement
Enforces strict 5-day UCP 600 examination rules.

| Field | Tag | Requirement | Character Set | Max Lines | Validation Rule |
|-------|-----|-------------|---------------|-----------|-----------------|
| **Pres. Date** | - | **Mandatory** | **Date** | - | Date documents were received at bank counter. |
| **Claim Amount** | - | **Mandatory** | **Numeric** | 1 | Cannot exceed LC Balance + Tolerance. |
| **Currency** | - | **Mandatory** | - | - | Mapped from instrument. |
| **Presenting Bank**| 54A | **Mandatory** | **BIC-11/8** | 1 | BIC of the bank presenting the documents. |
| **Bank Reference** | 20 | **Mandatory** | **X-Charset** | 1 | The presenting bank's unique reference number. |
| **Examination Date**| - | **Mandatory** | **Date** | - | SLA anchor. **Rule**: Presentation Date + 5 Business Days (UCP 600). |
| **Document Matrix** | - | **Mandatory** | - | - | Count of Original/Copy for each document type. |
| **Doc Disposal** | 77B | Optional | - | 3 | Instructions if docs are refused (e.g., HOLDING). |
| **Charges Ded.** | 73 | Optional | **Z-Charset** | 6 | Fees taken from proceeds. Ex: `ADVISING FEES USD 50`. |
| **Sender/Receiver**| 72Z | Optional | **Z-Charset** | 6 | Special instructions to the bank. |

---

## 9. LC Amendment (MT 707)
Designed for mid-lifecycle adjustments.

| Field | Tag | Requirement | Character Set | Validation Rule |
|-------|-----|-------------|---------------|-----------------|
| **Amendment Type** | - | **Mandatory** | - | Select FINANCIAL or NARRATIVE. |
| **Amount Delta** | 32B | Optional | **Numeric** | Use `+` or `-` to adjust. Redefines Max Liability. |
| **Expiry Delta** | 31D | Optional | **Date** | New validity date. |
| **Narrative** | 77A | **Mandatory**| **Z-Charset** | Explicitly describe what was changed in the instrument. |
| **Ben. Consent** | - | Optional | **Boolean** | If "Y", amendment is pending until beneficiary accepts. |

---

## 10. Settlement & Drawings
Captures payment release and liability reduction.

| Field | Tag | Requirement | Character Set | Input Guideline |
|-------|-----|-------------|---------------|-----------------|
| **Drawing Amount** | - | **Mandatory** | **Numeric** | Amount to be paid to beneficiary. |
| **Value Date** | - | **Mandatory** | **Date** | Effective date of payment and account debit. |
| **Charge Earmark** | - | Optional | - | Select if charges should be settled now or deferred. |

---

## 11. Character Set Quick-Reference

### X-Character Set (The "Strict" Set)
Forbidden symbols: `@`, `!`, `#`, `$`, `%`, `^`, `&`, `*`, `_`, `=`, `<`, `>`, `;`, `"`.
Allowed: `A-Z a-z 0-9 / - ? : ( ) . , ' + space`

### Z-Character Set (The "Extended" Set)
Allowed in **Narratives** (Tags 73, 72Z, 77A):
- `.` `,` `-` `(` `)` `/` `=` `'` `+` `:` `?` `!` `#` `&` `*` `<` `>` `;` `@` `space`

---

## 12. Support & Helpdesk
For operational support, contact the **Trade Finance Operations Helpdesk** at ext 9999 or email `trade-support@bank.com`.
