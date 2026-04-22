# Participant Operating Guide: Digital Trade Finance

Welcome to the Digital Trade Finance platform. This comprehensive guide provides step-by-step instructions for Bank Participants (Makers and Checkers) to manage the lifecycles of Import Letters of Credit and Shipping Guarantees.

---

## 1. Unified Operational Dashboard
The dashboard is your central hub for high-density, priority-driven task management.

### KPI Metrics
- **Drafts Awaiting My Submission**: Real-time count of instruments in `Draft` state.
- **LCs Expiring within 7 Days**: Critical countdown for instruments requiring immediate extension.
- **SLA Alerts**: Highlights presentations nearing the 5-day UCP 600 response deadline.

### High-Density Data Grid
- **Global Search**: Instantly filter instruments by Ref, Applicant, or Amount.
- **Dynamic Status Chips**: Visual indicators for `Pending Authorisation`, `Discrepant`, and `Issued` states.
- **Row-Level Actions**: Direct access to Document Examination or Amendment forms.

---

## 2. Import LC Issuance (Maker Role)
The Issuance Stepper provides a premium, 5-step guided experience for complex data entry.

### workflow Steps
1.  **Parties & General**: Capture Applicant, Beneficiary, and Advising Bank details.
2.  **Financials & Tolerance**: Define Amount, Currency, and +/- tolerance percentages.
3.  **Terms & Dates**: Define Incoterms, Ports of Loading/Discharge, and Expiry logic.
4.  **Documents & Narratives**: map required documents to MT700 field blocks.
5.  **Review & Submit**: Full-width summary view with validation error highlighting.

---

## 3. Checker Authorization & Risk Matrix (Checker Role)
Accessed via the **Global Checker Queue**, this workspace provides a 30/70 split-pane risk analysis view.

### Risk & Limit Matrix (Left Pane)
- **Limit Compliance**: Displays real-time headroom for the selected Credit Facility (BDD-IMP-ISS-01).
- **Sanctions Check**: Automated status from global SDN screening.
- **Discrepancy Severity**: High-visibility alerts for Document Presentation findings.

### Transaction Comparison (Right Pane)
- **Read-Only Context**: Full instrument view for verification.
- **Delta Highlighting**: Amendments show `Old Value` vs `New Value` with status tags for rapid audit.

### Review Actions
- **Approve Transaction**: Triggers immutable audit log and MT700 dispatch.
- **Reject to Maker**: Opens a mandatory comments modal to specify rectification requirements.

---

## 4. Document Examination Workspace
A specialized 50/50 split-screen interface for comparing LC terms against digital document presentations.

- **Digital Document Viewer (Left)**: Scrollable PDF/Image preview of presented docs (Invoice, B/L, etc.).
- **Discrepancy Ledger (Right)**:
    - **Checklist**: Select discrepancies (e.g., Late Presentation, Overdrawn Amount).
    - **Compliance Decision**: Mark as `Clean` or `Discrepant` to trigger the next workflow step.

---

## 5. Shipping Guarantee (SG) Workflow
Used when goods arrive at the port before original documents.
- **Issuance**: Select an existing Issued LC to anchor the SG.
- **110% Earmarking**: The system automatically secures 110% of the invoice amount against the customer's limit to cover potential price variance and costs.

---

## 6. Support & Troubleshooting
- **System Errors**: If the "System Temporarily Unavailable" banner appears, check your network or contact Trade Support.
- **Self-Auth Error**: You will see a "Self-Authorization Forbidden" warning if you attempt to approve your own transaction.
