# Participant Operating Guide: Digital Trade Finance

This guide provides exhaustive, step-by-step instructions for Bank Makers and Checkers to manage the trade finance lifecycle, from initial LC issuance to final settlement and closure. All data entry follows strict SWIFT standards to ensure "Clean at Capture" processing.

---

## 1. Dashboard & Navigation
The primary landing page provide a high-level view of your trade portfolio with real-time exposure monitoring.

| Section | Description | Action |
|---------|-------------|--------|
| **Operations Dashboard** | List of all active and pending instruments. | Click the **Reference Number** to view full instrument details. |
| **Global Search** | Persistent search bar in the top header. | Type a **Transaction Ref** (e.g., `TF-IMP-26-0001`) or **Applicant Name** to filter instantly. |
| **SLA Timer** | Visual indicator on instrument rows. | Green: >5 days; Yellow: 2-5 days; Red: <2 days for UCP compliance. |

---

## 2. Import LC Issuance (Maker Workflow)

**Action**: Click **New LC Issuance** in the **OPERATIONS** section of the sidebar.

### Step 1: Parties & Limits
This step defines the contractual entities and their banking relationships.

| Field | Requirement | Input Guideline | Validation Rule |
|-------|-------------|-----------------|-----------------|
| **Product Catalog** | **Mandatory** | Select the product type (e.g., *Import Letter of Credit*). | Loads default SLA, tolerance, and Fee templates. |
| **Applicant** | **Mandatory** | Search for the corporate client applying for the LC. | Must have an active KYC status and sufficient Limit Headroom. |
| **Beneficiary** | **Mandatory** | Select the exporter party from the directory. | Must be a different legal entity than the Applicant. |
| **Beneficiary Address**| **Mandatory** | Enter full postal address for MT700 Tag 59. | **X-Charset Only**: `A-Z 0-9 / - ? : ( ) . , ' + space`. Multiple lines allowed. |
| **Advising Bank BIC** | **Mandatory** | Enter the 8 or 11 character SWIFT BIC. | Must be a valid BIC. system validates bank name upon entry. |
| **Credit Facility** | **Mandatory** | Select the specific facility to be utilized. | Must have `firm` limit available. Displays current exposure alert if >90%. |

**To Proceed**: All mandatory fields must be filled. The "Next" button will remain disabled if validation fails.

---

### Step 2: Financial Terms
Defines the value, currency, and drawing flexibility.

| Field | Requirement | Input Guideline | Validation Rule |
|-------|-------------|-----------------|-----------------|
| **LC Amount** | **Mandatory** | Enter numeric value (e.g., `125000.00`). | Cannot exceed the selected Facility's available limit. |
| **Currency** | **Mandatory** | ISO Code (e.g., USD, EUR, SGD). | Defaulted based on Product/Facility settings. |
| **Tolerance (+)** | Tag 39A | Percentage allowed above base (e.g., `5`). | System calculates Max Liability as `Amount * (1 + Tol/100)`. |
| **Tolerance (-)** | Tag 39A | Percentage allowed below base (e.g., `5`). | Affects the minimum drawing validation. |
| **Partial Shipment** | Tag 43P | *ALLOWED* or *NOT ALLOWED*. | Defaults to *ALLOWED*. |
| **Transhipment** | Tag 43T | *ALLOWED* or *NOT ALLOWED*. | Defaults to *ALLOWED*. |

---

### Step 3: Terms & Shipping
Defines the validity period and logistics constraints.

| Field | Requirement | Input Guideline | Validation Rule |
|-------|-------------|-----------------|-----------------|
| **Expiry Date** | **Mandatory** | Select the final date of LC validity. | Must be in the future. |
| **Expiry Place** | **Mandatory** | City/Country where docs must be presented. | **X-Charset Only**. |
| **Latest Shipment**| **Mandatory** | Final date for port departure/loading. | Must be $\leq$ Expiry Date. |
| **Tenor Type** | **Mandatory** | *SIGHT* (immediate) or *USANCE* (deferred). | Required for payment scheduling. |
| **Usance Days** | Conditional | Number of days for deferred payment. | **Required** if Tenor is *USANCE*. |
| **Incoterms** | Optional | E.g., FOB, CIF, EXW. | Helps define insurance responsibility. |

---
### Step 4: Narrative & SWIFT Tags
The contractual conditions rendered in MT700.

| Field | Requirement | Input Guideline | Validation Rule |
|-------|-------------|-----------------|-----------------|
| **Goods Description**| **Mandatory** | Tag 45A - Details of merchandise. | **X-Charset Only**. Max 100 lines of 65 chars. |
| **Docs Required** | **Mandatory** | Tag 46A - List of required evidence. | **X-Charset Only**. E.g., "Full set clean on board Bill of Lading". |
| **Additional Cond.** | Optional | Tag 47A - Special banking instructions. | **X-Charset Only**. |
| **Charges** | Optional | Tag 71B - Who pays which fees? | E.g., "All banking charges outside issuing bank are for beneficiary account". |

---

### Step 5: Final Review
1.  **Draft Preview**: Scroll through the summary to verify all data.
2.  **Submit**: Click **Submit for Authorization**.
3.  **Result**: Status moves to `LC_PENDING_APPROVAL`. The record disappears from your draft list and moves to the Checker queue.

---

## 3. Checker Workflow (Authorization)

**Action**: Go to **My Tasks** in the sidebar.

1.  **Select Task**: Click on an instrument in `INST_PENDING_APPROVAL` status.
2.  **Verify Data**:
    - Ensure **Sanctions Check** is green.
    - Verify **Facility Utilization** bar (no red overflow).
3.  **Approve**: Click **Authorize**.
    - For Tier 1-3: Status moves to `LC_ISSUED`.
    - For Tier 4 (Dual): Requires a second checker to approve.
4.  **Reject**: Click **Send Back to Maker**. You **must** provide a reason in the comments.

### E2E Regression Stability
Achieved a 100% pass rate (20/20) across all functional flows.

- **Import LC Issuance**: Validated all 5 mandatory fields (Product, Party, Facility, Dates).
- **Navigation Integrity**: Verified all sidebar links and premium dashboard headings.
- **Admin Panels**: Synchronized Product, Tariff, and Party configuration Master-Detail views.
- **Authorization Queue**: Confirmed Checker-Maker visibility and state transitions.

### 📚 Documentation Enhancements
- **Enduser Guide**: Rewritten to provide field-level validation rules and SWIFT character set guidance.
- **Developer Guide**: Restored architectural details and regression suite execution commands.

---

## 4. Document Presentation & Payment

**Action**: Locate the Issued LC in the **Operations Dashboard** and click **Present Documents**.

1.  **Claim Amount**: Enter the amount presented by the beneficiary.
2.  **Discrepancy Check**: Record any deviations from LC terms (e.g., "Late Shipment").
3.  **Final Settlement**: Once docs are accepted, click **Settle Payment**.
    - System debits Applicant account.
    - System releases Facility earmark.
    - Status moves to `LC_CLOSED`.

---

## 5. Troubleshooting "SWIFT Character Block"
If you see a red border around a text field with the message "Invalid Characters":
- Check for symbols like `@`, `!`, `#`, `$`, `%`, `^`, `&`, `*`, `_`, `\`, `|`, `~`.
- SWIFT MT700 only allows: `A-Z a-z 0-9 / - ? : ( ) . , ' + space`.
- **Note**: Lowercase `a-z` is internally converted to uppercase in many bank gateways, so we recommend using CAPS for MT700 narratives.

---

## Conclusion
For support, contact the **Trade Finance Operations Helpdesk** at ext 9999 or email `trade-support@bank.com`.
