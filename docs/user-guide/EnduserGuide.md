# Participant Operating Guide: Digital Trade Finance

This guide provides exhaustive, step-by-step instructions for Bank Makers and Checkers. The platform enforces **"Clean at Capture"** principles through proactive validation, ensuring every field adheres to SWIFT MT7xx standards before submission.

---

## 1. Navigation Route Map
The platform is organized into functional modules accessible via the **Sidebar**.

### OPERATIONS (Core Workflow)
- **Dashboard**: Central command for tracking all active and pending instruments. Key landing page for daily operations.
- **My Tasks**: Checker queue for authorizing transactions (requires `TRADE_CHECKER` role).
- **Doc Examination**: Specialized workflow for verifying physical documents against LC terms.

### LIFECYCLE MANAGEMENT (Transaction Actions)
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

## 2. Dashboard Actions & Workflows
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

## 3. Instrument Detail View (Workspace)
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

## 5. Import LC Issuance (Maker Workflow)

### Step 1: Parties & Limits
| Field | Tag | Requirement | Character Set | Input Guideline / Constraints |
|-------|-----|-------------|---------------|-------------------------------|
| **Product Catalog**| - | **Mandatory** | - | Select the generic LC type to load default SLA/Tenor templates. |
| **Applicant** | 50 | **Mandatory** | **X-Charset** | Select corporate client. Name/Address mapped to **Max 4 Lines**. |
| **Beneficiary** | 59 | **Mandatory** | **X-Charset** | Enter manually or select. Name/Address mapped to **Max 4 Lines**. |
| **Advising Bank** | 57A | **Mandatory** | **BIC-11/8** | Enter 8 or 11 char BIC. System auto-verifies Bank Name. |
| **Credit Facility**| - | **Mandatory** | - | Must have sufficient available balance. Blocks flow if exceeded. |

---

### Step 2: Main LC Information
| Field | Tag | Requirement | Character Set | Input Guideline / Constraints |
|-------|-----|-------------|---------------|-------------------------------|
| **LC Amount** | 32B | **Mandatory** | **Numeric** | Enter base value. **Currency** defaulted from facility. |
| **Tolerance +/-** | 39A | Optional | **Numeric** | Percentage deviation allowed. Disabled if "Max Credit Amount" checked. |
| **Available By** | 41a | **Mandatory** | - | Select Payment Type (e.g., SIGHT, NEGOTIATION). |
| **Available With** | 41A/D| **Mandatory** | **BIC or X** | **BIC (41A)** or **Narrative Name (41D)**. Name max **4 lines**. |
| **Expiry Date** | 31D | **Mandatory** | **Date** | Must be in the future. |
| **Latest Shipment**| 44E | Conditional | **Date** | Mandatory if "Shipment Period" is empty. Must be $\leq$ Expiry. |

---

### Step 3: Narratives & "Clean at Capture"
Proactive validation prevents the entry of invalid symbols.

| Field | Tag | Requirement | Character Set | Max Lines | Input Guideline |
|-------|-----|-------------|---------------|-----------|-----------------|
| **Goods Desc.** | 45A | **Mandatory** | **X-Charset** | 100 | Detailed list of merchandise and Incoterms. |
| **Docs Required** | 46A | **Mandatory** | **X-Charset** | 100 | List of required evidence (e.g., Bill of Lading, Invoice). |
| **Add. Conditions**| 47A | Optional | **X-Charset** | 100 | Special bank instructions or specific local regulations. |
| **Charges** | 71D | Optional | **X-Charset** | 6 | Define fee responsibility (e.g., All outside bank for Ben). |

---

## 3. Document Presentation Lodgement
Enforces strict 5-day UCP 600 examination rules.

| Field | Tag | Requirement | Character Set | Max Lines | Validation Rule |
|-------|-----|-------------|---------------|-----------|-----------------|
| **Claim Amount** | - | **Mandatory** | **Numeric** | 1 | Cannot exceed LC Balance + Tolerance. |
| **Examination Date**| - | **Mandatory** | **Date** | - | Sets the anchor for the 5-day SLA countdown. |
| **Charges Ded.** | 73 | Optional | **Z-Charset** | 6 | Fees taken from proceeds. Ex: `ADVISING FEES USD 50`. |
| **Sender/Receiver**| 72Z | Optional | **Z-Charset** | 6 | Special instructions to the bank. |

---

## 4. LC Amendment (MT 707)
Designed for mid-lifecycle adjustments.

| Field | Tag | Requirement | Character Set | Validation Rule |
|-------|-----|-------------|---------------|-----------------|
| **Amount Delta** | 32B | Optional | **Numeric** | Use `+` or `-` to adjust. Redefines Max Liability. |
| **Expiry Delta** | 31D | Optional | **Date** | New validity date. |
| **Narrative** | 77A | **Mandatory**| **Z-Charset** | Explicitly describe what was changed in the instrument. |

---

## 5. Character Set Quick-Reference

### X-Character Set (The "Strict" Set)
Forbidden symbols: `@`, `!`, `#`, `$`, `%`, `^`, `&`, `*`, `_`, `=`, `<`, `>`, `;`, `"`.
Allowed: `A-Z a-z 0-9 / - ? : ( ) . , ' + space`

### Z-Character Set (The "Extended" Set)
Allowed in **Narratives** (Tags 73, 72Z, 77A):
- `.` `,` `-` `(` `)` `/` `=` `'` `+` `:` `?` `!` `#` `&` `*` `<` `>` `;` `@` `space`

---

## Conclusion
For operational support, contact the **Trade Finance Operations Helpdesk** at ext 9999 or email `trade-support@bank.com`.
