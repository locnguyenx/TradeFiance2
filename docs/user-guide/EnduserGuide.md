# Participant Operating Guide: Digital Trade Finance

This guide provides exhaustive, step-by-step instructions for Bank Makers and Checkers. The platform enforces **"Clean at Capture"** principles through proactive validation, ensuring every field adheres to SWIFT MT7xx standards before submission.

---

## 1. Getting Started: Authentication
The platform uses role-based authentication to secure trade operations.

### Logging In
1.  Enter your **Username** and **Password** on the premium login screen.
2.  If your account is suspended or credentials expire, a toast notification will provide specific guidance.
3.  Upon successful login, you will be redirected to the **Operations Dashboard**.

### Session Management
- Sessions are strictly monitored. 
- Click the **Logout (LogOut)** icon in the sidebar footer to securely end your session.
- Always logout when leaving your workstation to prevent unauthorized transactions.

---

## 2. Navigation Route Map
The platform is organized into functional modules accessible via the **Sidebar**.

### OPERATIONS (Cross-Product Workflow)
- **Transaction Dashboard**: Unified view of all transactions (Maker/Checker) across all product lines.
- **My Tasks**: Checker queue for authorizing transactions (requires `TRADE_CHECKER` role).
- **Doc Examination**: Specialized workflow for verifying physical documents against LC terms.

#### IMPORT LC (Product-Specific Management)
- **Import LC Dashboard**: Central command for tracking current legal states of Import instruments.
- **New LC Issuance**: Entry point for creating fresh MT 700 draft instruments.
- **LC Amendments**: Browse and view the full history of both **External** (MT 707) and **Internal** (Bank-only) amendment records independent of the dashboard.
- **Presentations**: Direct access to historical document claim records and discrepancy status.
- **Settlements**: Portfolio view of all payment releases and remittance history.
- **Shipping Guarantees**: Independent record browsing for cargo release guarantees.
- **Cancellations**: Track formal termination requests and historical closures.
- **Nostro Reconciliation**: Manage the matching of MT 740/747 expectations with actual bank statement debits.

### MASTER DATA & ADMIN
- **Party & KYC Directory**: Manage corporate client records, BICs, and compliance status.
- **Credit Facilities**: Real-time monitoring of bank-wide limits and utilization.
- **User Authority Tiers**: Manage user approval limits and delegation tiers (requires `TRADE_ADMIN`).
- **Product & Tariff Config**: System-level pricing and logic templates.

### USER ACCOUNT
- **Profile**: Access your personal authority details and security settings.
- **Logout**: Securely terminate your current session.

---

## 3. Portfolio Record Browsing (REQ-NAV-01.2)
Unlike the operational Dashboards which show pending work, the **Portfolio Record Views** provide direct, non-blocking access to the underlying business artifacts (Amendments, Presentations, etc.).

### Accessing Portfolio Records
Navigate to the specific product group in the sidebar to view its dedicated master portfolio:
- **Amendment Portfolio**: `IMPORT_LC > LC Amendments`. View historical records for both **External** (MT 707) and **Internal** (Bank-only) adjustments, including specific delta changes and consent status.
- **Presentation Portfolio**: `IMPORT LC > Presentations`. Track claim amounts and exam results for all document submissions.
- **Settlement Portfolio**: `IMPORT LC > Settlements`. Monitor specific remittance dates, FX rates, and debit account details.

### Functional Features
- **Decoupled Navigation**: Browse historical records without first selecting an active instrument from the dashboard.
- **Entity-Specific Details**: Clicking a record provides a specialized view of its payload (e.g., viewing an Amendment Narrative) rather than a generic transaction log.
- **Audit Stability**: These views provide the permanent record of approved actions, ensuring full visibility into the lifecycle history of any trade asset.

---

## 4. Dashboard Actions & Workflows
The **Operations Dashboard** is the primary launchpad for instrument-level actions.

### Data Interaction
1.  **Filter & Search**: Use the **Status Filter** (Draft, Pending, Issued, Doc Received) or the **Global Search** bar.
    - **Context Toggle**: Select **Inst** to search by **Instrument Ref** (legal asset) or **Txn** to search by **Transaction Ref** (active workflow).
2.  **View Detail Page**: Click any **Instrument Ref** (underlined blue text) to exit the dashboard and enter the **Dedicated Full-Screen View**.
3.  **Dual-Status Visibility**: The dashboard provides a combined view of the instrument's legal state (e.g., `ISSUED`) and its current active transaction status (e.g., `Draft Amendment`). This ensures high visibility for pending lifecycle actions.
4.  **Financial Clarity**:
    - **Effective Amount**: The current authorized face value of the instrument (includes amendments).
    - **Outstanding Amount**: The remaining undrawn balance available for presentation.
    - **Drawn Amount**: Total cumulative payments released to the beneficiary.
5.  **SLA Health**: High-priority transactions with <3 days remaining are highlighted in **Urgent Red** in the "SLA Timer" column.

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

## 5. Instrument Detail View (Workspace)
Clicking a reference opens the **Full-Screen Workspace**. This view is optimized for audit and control.

### Navigation within Details
The workspace features a high-fidelity layout:
- **Current State**: Static view of the instrument's active data (Parties, Financials, Terms).
- **Transaction Specific Details**: A dedicated payload section (visible when viewing a specific transaction) showing the exact data captured for that event (e.g., Amendment Narrative, Presentation Amounts).
- **Audit Narrative**: Chronological **Unified Timeline** merging financial transactions and technical audit logs.
- **SWIFT Messages**: Real-time view of all MT 7xx messages generated for this instrument.

### 5.1 SWIFT Message Preview (Maker Workflow)
To ensure accuracy before submission, Makers can preview the generated SWIFT payload:
1.  Navigate to the **SWIFT Messages** tab in the Instrument Detail View.
2.  Select the message record (e.g., MT 700 for Issuance).
3.  Click **View Payload** to see the draft message content.
4.  **Draft Status**: If the instrument is in `LC_DRAFT`, the message is considered a preview. Validation warnings (e.g., missing non-critical tags) are logged as warnings but do not block the view, allowing for iterative editing.
5.  **Final Validation**: Once the checker approves the transaction, the message status changes to **ACTIVE**, and strict SWIFT Layer 2 validation is enforced.

### Workspace Actions
The **Workspace Actions** sidebar card provides contextual buttons:
1.  **Continue Editing Draft**: Only visible for instruments in `LC_DRAFT`.
2.  **Export Audit Document**: Generates a high-fidelity print-ready report of the transaction.
3.  **Action Buttons**: For Issued LCs, buttons for **Amend**, **Present**, and **Settle** are located at the bottom or in the sidebar depending on screen size.
### 5.5 Previewing SWIFT Messages (Maker Preview)
The platform allows Makers to preview the exact SWIFT payload before the transaction is finalized.
- **Message Lifecycle**:
    - **DRAFT**: Messages for pending transactions (New LC, Amendment, etc.) are labeled as DRAFT. These represent the *proposed* message and can be updated by editing the transaction.
    - **ACTIVE**: Once a Checker authorizes the transaction, the message status changes to ACTIVE, and it is officially released for transmission.
- **How to Preview**:
    1.  Navigate to the **SWIFT Messages** tab within the workspace.
    2.  Click any row with status **DRAFT** to expand the raw message content.
    3.  Verify that all tags (e.g., 45A, 46A) are correctly formatted and fit within SWIFT line limits.
    4.  If errors are found, click **Edit Draft** to correct the source data.

---

## 6. Manual Verification & Quality Control
Before finalizing any transaction, follow these steps to ensure data integrity:
1.  **Audit the "Proposed State"**: During authorization, always compare the current values with the proposed changes.
2.  **SWIFT Preview (MANDATORY)**: For all issued or amended LCs, navigate to the **SWIFT Messages** tab. **Ensure the status is DRAFT** and the content perfectly matches the intended terms.
3.  **Verify Character Sets**: Ensure no forbidden characters (like `@`, `!`, `#`) have bypassed validation in narrative fields.
4.  **Confirm Timeline**: Verify that the **Audit Narrative** correctly reflects the Maker's action and timestamp.
Users with the `TRADE_CHECKER` role can authorize transactions via the **My Tasks** section.

### The Approvals Queue
1.  **Priority Sorting**: Items are sorted by **Urgent**, **High**, **Medium**, and **Low** status based on transaction weight.
2.  **SLA Alerts**: Items pending for >4 hours are flagged as **SLA Alerts**.
3.  **Details Review**: Click the **Eye (👁️)** icon to open the **Checker Authorization Workspace**.

### Authorizing an Instrument
1.  Click **Authorize** to enter the high-fidelity workspace.
2.  **Compare Proposed vs. Current**: The workspace displays the instrument data *as it will look* after the transaction (Proposed) alongside current recorded values. This is critical for verifyng Amendment deltas or Settlement amounts.
3.  **Submit Decision**: Actions are performed against a unique **Transaction Ref** (e.g., `TF-TXN-26-0001`), ensuring atomicity. Once approved, the system promotes the "Proposed State" to the "Master Record" and updates the instrument's `businessStateId`.

### Structured Party Review
When reviewing an LC, the Checker sees all assigned parties grouped by category:
- **Commercial Parties**: Applicant (Obligor), Beneficiary (Payee) — with KYC status badge.
- **Banking Parties**: Advising, Confirming, Reimbursing, Drawee banks — with BIC, RMA status, and FI Limit indicators.
- **Multi-Role Assignments**: If one bank holds multiple roles, this is clearly indicated (e.g., "JP Morgan — Advising, Confirming").
- **Eligibility Status**: Each bank party shows a compliance summary (KYC ✓, RMA ✓, FI Limit ✓).

---

## 7. Resuming Drafts
The platform ensures no data is lost during the issuance process.

1.  Navigate to the **Dashboard**.
2.  Select **Status: Draft** from the filter dropdown.
3.  Click the **Actions (•••)** menu on your draft record.
4.  Select **Edit Draft**.
5.  **Persistence**: The stepper saves your progress automatically. You can jump directly to the last active step (e.g., Step 4: Narratives).

---

## 8. User Profile & Security
Manage your operational identity via the **Profile** page (accessible by clicking your name/avatar in the sidebar).

### Authority Monitoring
The Profile page displays your **Active Authority Tier** and **Current Approval Limit**. 
- **Tier 1 (Maker)**: Draft and submit instruments.
- **Tier 2/3 (Checker)**: Authorize transactions up to your limit.
- **Tier 4 (Executive)**: Authorization for high-value transactions and dual-auth overrides.

### Password Management
1.  Navigate to the **Security** section of your profile.
2.  Enter your current password and define a new one (min 8 characters).
3.  Successful updates are confirmed via a green toast notification and logged for audit purposes.

---

## 9. Import LC Issuance (Maker Workflow)

### Step 1: Parties & Limits
| Field | Tag | Requirement | Character Set | Input Guideline / Constraints |
|-------|-----|-------------|---------------|-------------------------------|
| **LC Type** | 40A | **Mandatory** | - | Select SIGHT or USANCE. Determines if Usance Days are required. |
| **Confirmation** | 49 | Optional | - | Select CONFIRM, MAY ADD, or WITHOUT (Default). |
| **Product Catalog**| - | **Mandatory** | - | Select the generic LC type to load default SLA/Tenor templates. |
| **Transaction Ref**| 20 | Optional | **X-Charset** | Bank's internal reference. **Format**: `TF-IMP-YY-NNNN` (e.g., `TF-IMP-26-0001`). |
| **Applicant** | 50 | **Mandatory** | - | Select commercial client from directory. **Must have 'Active' KYC Status.** |
| **Beneficiary** | 59 | **Mandatory** | - | Select commercial client from directory. **Must have 'Active' KYC Status.** |
| **Advising Bank** | Receiver | **Mandatory** | - | Select onboarded Bank from directory. **Must have Active RMA** with Issuing Bank. System auto-retrieves BIC for Block 2. |
| **Adv. Thru Bank** | 57A | Optional | - | Select intermediary Bank. **No RMA required** with Issuing Bank. Used when Issuing Bank has no RMA with the Advising Bank. |
| **Confirming Bank**| 58A | Conditional | - | Required if Confirmation is requested. Select onboarded Bank. **Must have sufficient FI Limit** to cover LC Amount. |
| **Credit Facility**| - | **Mandatory** | - | Select Applicant's facility. Must have sufficient available balance. Blocks flow if exceeded. |

> [!IMPORTANT]
> **Junction-Based Party Selection**: You **do not manually type** names or BICs for core parties. You select the legal entity from the master Party Directory. The system automatically retrieves the party's `swiftBic` and `registeredAddress` for SWIFT message generation.
>
> **Multi-Role Banks**: A single bank can serve multiple roles on the same LC (e.g., JP Morgan as Advising Bank + Confirming Bank + Negotiating Bank). Select the same bank from the dropdown for each applicable role. The system tracks each role assignment independently.

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
| **Available With** | 41A/D| **Mandatory** | - | Choose **"Any Bank"** or **"Specific Bank"** via radio toggle. If Specific, select the Negotiating Bank from directory. System generates Tag 41A (BIC) or 41D (Name/Address) automatically. |
| **Drawee Bank** | 42A | Optional | - | Select from Bank directory. System auto-retrieves BIC for Tag 42A. |
| **Partial Ship.** | 43P | Optional | - | Select ALLOWED or NOT ALLOWED. |
| **Transhipment** | 43T | Optional | - | Select ALLOWED, NOT ALLOWED, or CONDITIONAL. |
| **Port of Loading**| 44E | Optional | **X-Charset** | Port/Airport of taking in charge/dispatch. |
| **Port of Disch.** | 44F | Optional | **X-Charset** | Port/Airport of destination. |
| **Goods Desc.** | 45A | **Mandatory** | **X-Charset** | Detailed list of merchandise and Incoterms. |
| **Docs Required** | 46A | **Mandatory** | **X-Charset** | List of required evidence (e.g., Bill of Lading, Invoice). |
| **Applicant Bank** | 51A | Optional | - | Select from Bank directory (if not self). System auto-retrieves BIC. |

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
| **Claim Amount** | - | **Mandatory** | **Numeric** | 1 | Cannot exceed **LC Balance + Positive Tolerance**. |
| **Currency** | - | **Mandatory** | - | - | Mapped from instrument. |
| **Presenting Bank**| 54A | **Mandatory** | - | Select onboarded Bank from directory. System auto-retrieves BIC. |
| **Bank Reference** | 20 | **Mandatory** | **X-Charset** | 1 | The presenting bank's unique reference number. |
| **Examination Date**| - | **Mandatory** | **Date** | - | SLA anchor. **Rule**: Presentation Date + 5 Business Days (UCP 600). |
| **Document Matrix** | - | **Mandatory** | - | - | Count of Original/Copy for each document type. |
| **Doc Disposal** | 77B | Optional | - | 3 | Instructions if docs are refused (e.g., HOLDING). |
| **Charges Ded.** | 73 | Optional | **Z-Charset** | 6 | Fees taken from proceeds. Ex: `ADVISING FEES USD 50`. |
| **Sender/Receiver**| 72Z | Optional | **Z-Charset** | 6 | Special instructions to the bank. |
| **Discrepancy Tag** | 77J | **SRG 2024** | **X-Charset** | 70 | **Aggregate Line Limit**: The total text of all logged discrepancies must not exceed 70 lines of 50 characters each. |

---

## 9. Import LC Amendment (MT 707) - SRG 2024 Revised
The platform supports a dual-track amendment process aligned with SRG 2024 "Smart Delta" requirements, separating customer-facing (External) changes from operational (Internal) bank adjustments.

### 9.1 Amendment Tracks
1.  **External Amendment (MT 707)**: Used for changes requiring beneficiary consent (e.g., amount, expiry, narrative terms). Generates a SWIFT MT 707 message.
2.  **Internal Amendment**: Used for bank-only adjustments (e.g., RM assignment, margin account change, fee debit account) that do not require beneficiary notification or consent.

### 9.2 Structured Cancellation (Tag 23S)
SRG 2024 introduces formal cancellation requests via Tag 23S in the MT 707:
- **Toggle**: Use the "Request Full Cancellation" toggle in the Amendment Stepper.
- **Mixed-Change Guard**: A cancellation request **cannot** be combined with any other changes (e.g., amount adjustment or expiry extension). Selecting cancellation will disable other financial fields.
- **Outcome**: Generates an MT 707 with Tag 23S set to `CANCEL`.

### 9.3 Smart Delta Narrative Updates
To prevent data loss and ensure precise audit trails, narrative fields (Goods, Documents, Conditions) are updated using structured **Delta Actions**:
-   **REPLACE**: Overwrites the entire existing field with new text.
-   **ADD**: Appends new text to the end of the existing narrative.
-   **DELETE**: Marks specific text for removal (captured in the delta log).

### 9.3 Amendment Workflow (Maker/Checker)
| Field | Tag | Requirement | Character Set | Input Guideline / Validation Rule |
|-------|-----|-------------|---------------|-----------------------------------|
| **Amendment Type**| - | **Mandatory** | - | Select `AMD_TYPE_GEN` (External) or `AMD_TYPE_INTERNAL`. |
| **Amount Increase**| 32B | Optional | **Numeric** | Incremental increase to the face value. |
| **Amount Decrease**| 32B | Optional | **Numeric** | Incremental decrease to the face value. |
| **New Expiry Date**| 31D | Optional | **Date** | Updates the instrument validity. |
| **Goods Delta** | 45B | Optional | **X-Charset** | Structured update to Goods Description using REPLACE/ADD/DELETE. |
| **Docs Delta** | 46B | Optional | **X-Charset** | Structured update to Documents Required. |
| **Beneficiary Consent**| - | **Mandatory** | **Boolean** | Required for all External Amendments. |

### 9.4 Beneficiary Consent (MT 730)
1.  **Draft/Pending**: Upon authorization, an External Amendment enters `AMEND_DRAFT` (Pending Consent). The Master LC is **not updated** yet.
2.  **Recording Consent**: When the beneficiary's decision (MT 730) is received:
    -   **Accepted**: The system automatically merges the Smart Delta changes into the Master LC and updates the `businessStateId` to `LC_AMENDED`.
    -   **Rejected**: The amendment is archived as `REJECTED`, and the Master LC remains unchanged.
3.  **Historical Traceability**: The **Amendment Portfolio** maintains the full history of every proposed delta, regardless of consent outcome.

### 9.5 Browsing and Viewing Amendments
Due to the distinct operational and regulatory nature of amendments, **External** and **Internal** amendments are managed in separate portfolios:

1.  **Access**: Use the sidebar navigation under the `IMPORT LC` section:
    -   Click **External Amendments** to view adjustments requiring SWIFT MT 707 generation and Beneficiary Consent (e.g., amount changes, expiry updates, narrative deltas).
    -   Click **Internal Amendments** to view bank-only operational adjustments (e.g., credit facility changes, margin account reassignments, RM updates).
2.  **View Details**:
    -   Click on any record row in either portfolio to view its full details.
    -   The **External Amendment Details** view highlights the Smart Delta actions (Add/Delete/Replace) for narrative fields and clearly displays the current Beneficiary Consent status.
    -   The **Internal Amendment Details** view focuses entirely on the new operational, accounting, and credit parameters.

---

## 10. Settlement & Drawings
Captures payment release and liability reduction. To ensure audit integrity, settlements follow the **Maker-Checker** pattern:
1.  **Initiation (Maker)**: Select an LC in `DOC_RECEIVED` or `LC_ISSUED` status and initiate settlement.
2.  **Validation**: The system validates that the **Drawing Amount** does not exceed the current **Outstanding Amount** (including tolerance).
3.  **Authorization (Checker)**: Upon Checker approval, the system automatically:
    -   Reduces the **Outstanding Amount** of the LC.
    -   Increases the **Cumulative Drawn Amount**.
    -   Releases the corresponding portion of the **Credit Facility** limit.
    -   Transitions the instrument state (e.g., to `LC_CLOSED` if fully drawn).

| Field | Tag | Requirement | Character Set | Input Guideline |
|-------|-----|-------------|---------------|-----------------|
| **Drawing Amount** | - | **Mandatory** | **Numeric** | Amount to be paid to beneficiary. |
| **Value Date** | - | **Mandatory** | **Date** | Effective date of payment and account debit. |
| **Charge Earmark** | - | Optional | - | Select if charges should be settled now or deferred. |
| **Settlement Type**| - | **Mandatory** | - | Select Sight Payment, Acceptance, or Deferred. |

---

## 11. Character Set Quick-Reference

### X-Character Set (The "Strict" Set)
Forbidden symbols: `@`, `!`, `#`, `$`, `%`, `^`, `&`, `*`, `_`, `=`, `<`, `>`, `;`, `"`.
Allowed: `A-Z a-z 0-9 / - ? : ( ) . , ' + space`

### Z-Character Set (The "Extended" Set)
Allowed in **Narratives** (Tags 73, 72Z, 77A):
### SWIFT Formatting Constraints
To ensure 100% STP (Straight Through Processing), all narrative and address fields are subject to:
- **Character Set Validation**: Automatic rejection of non-compliant symbols during the Maker phase.
- **Line Length Enforcement**: Addresses are limited to **35 characters per line** across max 4 lines. Narrative fields (Tag 45A/46A) are wrapped automatically at 65 characters.
- **Reference Uniqueness**: **Instrument Ref** must be unique bank-wide. **Transaction Ref** is auto-generated to ensure traceability across the lifecycle.

---

## 12. Nostro Reconciliation (MT 740/747)
The system automates the tracking of reimbursement expectations to comply with SRG 2024 standards.

### 12.1 Auto-Generation
- **MT 740**: Automatically generated when an LC is issued with a Reimbursing Bank assigned.
- **MT 747**: Automatically generated when a financial amendment increases or decreases the LC amount on an instrument with an active reimbursement authorization.

### 12.2 Manual Matching Workflow
1.  Navigate to **IMPORT LC > Nostro Reconciliation**.
2.  **Portfolio View**: Review the list of "Pending" reconciliation records. Each record represents an MT 740/747 message sent to a reimbursing bank.
3.  **Manual Match**: Click **Manual Match** on a pending record when the actual bank debit statement is received.
4.  **Input Details**:
    - **Debit Date**: The actual date the account was debited.
    - **Debit Amount**: The actual amount debited by the bank.
    - **Statement Ref**: The bank's statement reference number.
5.  **Amount Mismatch Guard**: If the `Debit Amount` differs from the `Expected Amount`, the system will display a high-visibility warning. You must provide a reason in the **Remarks** field before confirming the match.

---
