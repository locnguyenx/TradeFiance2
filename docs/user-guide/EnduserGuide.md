# Participant Operating Guide: Digital Trade Finance

This guide provides exhaustive, step-by-step instructions for Bank Makers and Checkers to manage the trade finance lifecycle.

![Dashboard Overview](file:///Users/me/.gemini/antigravity/brain/222f8be2-5040-4a62-83e6-546ec9bc13b0/verify_icons_rendering_1776868261650.webp)

---

## 1. Import LC Issuance (Maker Workflow)
**Goal**: Create and submit a new LC for authorization.

1.  **Initiation**: Click **New LC Issuance** in the **OPERATIONS** section.
2.  **Step 1: Parties**:
    - Enter **Applicant** and **Beneficiary** names.
    - Select **Advising Bank** from the lookup.
3.  **Step 2: Financials**:
    - Enter **Amount** (e.g., `500000`) and select **Currency**.
    - Define **Tolerance** (e.g., `5/5`).
4.  **Step 3: Terms**:
    - Select **Incoterms** (e.g., `CIF`).
    - Define **Expiry Date** and **Place of Expiry**.
5.  **Step 4: Clauses**:
    - Use the **Clause Selector** to add standard legal text for MT700 field 47A.
6.  **Step 5: Submission**:
    - Review the **Validation Summary**.
    - Click **Submit for Authorization**. The status will change to `PENDING_CHECKER`.

---

## 2. Checker Authorization (Checker Workflow)
**Goal**: Audit and approve/reject transaction requests.

1.  **Access**: Click **My Tasks (Approvals)** in the sidebar.
2.  **Selection**: Click the **Authorize** button on the relevant task row.
3.  **Verification (Step-by-Step)**:
    - Review the **Risk Matrix** (Left Pane) for limit headroom and sanctions status.
    - Check the **Highlighting Deltas** section to see exactly what changed (for Amendments).
    - Click **Verify Documents** to open the side-by-side viewer.
4.  **Decision**:
    - Click **Approve** to finalize and dispatch the instrument.
    - Click **Reject** to send back to the Maker with comments.

---

## 3. LC Amendments & Lifecycle Management
**Goal**: Modify terms or manage events for an issued LC.

### A. Initiating an Amendment
1.  From the **Operations Dashboard**, click the **•••** action on an Issued LC row.
2.  Select **New Amendment**.
3.  **Step-by-Step Change**:
    - Modify the restricted fields (e.g., increase Amount or extend Expiry).
    - The system calculates the **Delta** automatically.
    - Click **Submit Amendment**.

### B. Document Presentation & Examination
1.  Navigate to **Operations Dashboard > Row Actions > Present Documents**.
2.  **Input**: Enter Invoice Value and transport document references.
3.  **Examination**:
    - Go to **Document Examination** in the sidebar.
    - Use the checklist to mark discrepancies (e.g., "Late Shipment").
    - **Submit Decision**: If discrepant, it triggers a waiver request to the Applicant.

### C. Settlement & Closure
1.  Navigate to **Lifecycle > Settlements**.
2.  **Step-by-Step Payment**:
    - Select the LC and associated Presentation.
    - Review **Charges & Fees** (pre-calculated from Tariff Matrix).
    - Click **Initiate Payment** to trigger the accounting entries and MT740.

---

## 4. Advanced Operational Features
- **Global Search**: Search by full Reference Number or Applicant Name in the top header.
- **SLA Tracking**: Observe the color-coded **SLA Timer** in the dashboard. Red indicates < 2 days for UCP 600 compliance.
- **View Message**: On any instrument, click the **Message** icon to preview the SWIFT MT7xx template before dispatch.
