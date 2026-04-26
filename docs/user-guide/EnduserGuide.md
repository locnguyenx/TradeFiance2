# Participant Operating Guide: Digital Trade Finance

This guide provides exhaustive, step-by-step instructions for Bank Makers and Checkers to manage the trade finance lifecycle, from initial LC issuance to final settlement and closure. All data entry follows strict SWIFT standards to ensure "Clean at Capture" processing.

---

## 1. Dashboard & Navigation
The primary landing page provides a high-level view of your trade portfolio with real-time exposure monitoring.

| Section | Description | Action |
|---------|-------------|--------|
| **Operations Dashboard** | List of all active and pending instruments. | Click the **Reference Number** to view full instrument details. |
| **Global Search** | Persistent search bar in the top header. | Type a **Transaction Ref** (e.g., `TF-IMP-26-0001`) or **Applicant Name** to filter instantly. |
| **SLA Timer** | Visual indicator on instrument rows. | Green: >5 days; Yellow: 2-5 days; Red: <2 days for UCP compliance. |

---

## 2. Import LC Issuance (Maker Workflow)

**Action**: Click **New LC Issuance** in the **OPERATIONS** section of the sidebar.

### Step 1: Parties
This step defines the contractual entities and their banking relationships.

| Field | Requirement | Input Guideline | Validation Rule |
|-------|-------------|-----------------|-----------------|
| **Product Catalog** | Required | Select from: Standard, Standby, or Revolving. | Loads default SLA and tolerance templates. |
| **Applicant** | Required | Use the magnifying glass to search by Party ID or Name. | Must have an active KYC status and clear sanctions. |
| **Beneficiary** | Required | Select the exporter entity. | Must be different from the Applicant. |
| **Beneficiary Name** | Tag 59 | Enter full name and address. (Max 4 lines). | **X-Charset Only**: `A-Z 0-9 / - ? : ( ) . , ' + space`. |
| **Advising Bank BIC** | Tag 57A | Enter the 8 or 11 character SWIFT BIC. | Must be a valid BIC registered in the system. |
| **Issuing Bank BIC** | Tag 51A | Defaulted to your branch BIC. | Read-only for standard users. |

**To Proceed**: Click **Next** in the bottom right. The system auto-saves a draft.

---

### Step 2: Financials
Defines the value, currency, and drawing flexibility.

| Field | Requirement | Input Guideline | Validation Rule |
|-------|-------------|-----------------|-----------------|
| **LC Amount** | Required | Enter numeric value (e.g., `50000.00`). | Cannot exceed the Applicant's Treasury Limit. |
| **Currency** | Required | ISO Code (e.g., USD, EUR, SGD). | FX rates are fetched from the live Treasury feed. |
| **Tolerance (+)** | Tag 39A | Percentage allowed above the base (e.g. `5`). | System calculates Max Liability as `Amount * 1.05`. |
| **Tolerance (-)** | Tag 39A | Percentage allowed below the base (e.g. `5`). | Affects the minimum drawing validation. |
| **Max Credit** | Tag 39B | Check "NOT EXCEEDING" if no tolerance is allowed. | Mutually exclusive with Tolerance (+/-). |

---

### Step 3: Terms & Shipping
Defines the validity period and logistics constraints.

| Field | Requirement | Input Guideline | Validation Rule |
|-------|-------------|-----------------|-----------------|
| **Expiry Date** | Tag 31D | Select from date picker. | Must be in the future and match Expiry Place hours. |
| **Expiry Place** | Tag 31D | Free text (e.g., "At our counters"). | **X-Charset Only**. |
| **Tenor Type** | Required | *SIGHT* (immediate) or *USANCE* (deferred). | Determines drawing payment date calculation. |
| **Usance Days** | Conditional| Number of days (e.g., `90`). | Required if Tenor is *USANCE*. |
| **Port of Loading** | Tag 44E | City/Port name. | Max 65 characters. |
| **Port of Discharge**| Tag 44F | Destination City. | Max 65 characters. |
| **Latest Shipment** | Tag 44C | Final date for Bill of Lading. | Must be $\leq$ Expiry Date. |

---

### Step 4: Clauses & Documents
The legal "Narrative" fields that define the conditions of payment.

| Field | Requirement | Input Guideline | Validation Rule |
|-------|-------------|-----------------|-----------------|
| **Goods Description**| Tag 45A | Summary of merchandise. | **X-Charset Only**. Supports multiple lines. |
| **Documents Required**| Tag 46A | List of required evidence (e.g., Invoices). | **X-Charset Only**. Be specific about copies. |
| **Additional Cond.** | Tag 47A | Special banking instructions. | **X-Charset Only**. |
| **Confirmation** | Tag 49 | Select: CONFIRM, MAY ADD, or WITHOUT. | Controls confirmation fee calculations. |

---

### Step 5: Review & Submission
1.  **Validation Check**: Click **Run Audit**. Red markers will appear on steps with errors.
2.  **Swift Preview**: Click **Preview MT700** to see the draft SWIFT message.
3.  **Submit**: Click **Submit for Authorization**.
    - Status changes from `LC_DRAFT` to `LC_PENDING`.
    - Instrument appears in the **Checker Queue**.

---

## 3. LC Amendments
Used to modify terms after the LC is issued.

1.  **Narrative (Tag 79N)**: Enter the reason (e.g., "Price increase by 10%"). **Z-Charset Only**.
2.  **Financial Change**: If increasing, the system re-validates the facility limit.
3.  **Completion**: Amendments remain `PENDING_CONSENT` until the Beneficiary (Exporter) accepts.

---

## 4. Document Presentation & Settlement

### Recording Presentation
1.  Go to **Present Documents**.
2.  Enter **Total Claim Amount**. Must satisfy `Amount + Tolerance`.
3.  Verify **Document Checklist**. Mark "Received" for all stipulated documents.

### Settlement
1.  **Action**: Click **Initiate Payment**.
2.  **Effect**: The system posts GL entries, debits the applicant, and releases the facility limit.
3.  **Close**: If the drawing is "Final," the LC status moves to `LC_CLOSED`.

---

## 5. Checker Authorization
Checkers must verify the following before approval:
1.  **Sanctions Status**: Ensure "SANCTION_CLEAR" is visible.
2.  **Facility Headroom**: Verify the "Amount to be Earmarked" does not cause an over-limit.
3.  **Dual Approval**: If the transaction is Tier 4 (> $1,000,000), two Checkers are required.

---

## Conclusion
If you encounter a "SWIFT Character Mapping Error," check for special symbols like `@`, `#`, or `!`. Replace them with text or SWIFT-approved aliases (e.g., `/AT/` for `@`).
