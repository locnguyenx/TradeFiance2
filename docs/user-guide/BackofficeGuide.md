# Backoffice & System Admin Manual: Digital Trade Finance

This guide provides exhaustive, step-by-step instructions for Administrative and Backoffice users. The platform runs on the **"Blue Premium"** enterprise design system, ensuring high visibility for audit and compliance workflows.

---

## 1. Party & KYC Management
**Goal**: Onboard a new corporate entity or verify compliance status.

1.  **Initiation**: Click **Party Directory** in the **MASTER DATA** section of the sidebar.
2.  **Selection**: The directory uses a master-detail split view. Use the search bar to find an entity by Name or Legal ID.
3.  **KYC & Compliance (Full-Screen View)**:
    - Click **View Details** to enter the full instrument view.
    - Review **Ultimate Beneficial Owner (UBO)** and **UN Sanctions/OFAC** status.
    - Status is visually indicated by color-coded badges (Green: Clear, Red: Action Required).
4.  **Role Assignment**:
    - Verify roles (Applicant/Beneficiary) in the **Identity Card**.

---

## 2. Credit Facility & Exposure Monitoring
**Goal**: Review bank-wide exposure and drill down into specific utilization.

1.  **Facility Dashboard**: Click **Credit Facilities** in the sidebar.
2.  **Exposure Breakdown**:
    - Review the "Financial Hero Cards" for Total Exposure, Utilized, and Available Headroom.
    - Use the **Utilization Breakdown** table to see individual transactions.
3.  **Audit Drill-Down**:
    - Click any underlined **Transaction Ref** to open the dedicated full-screen detail page.
    - Review the **Unified Narrative Timeline** for a merged view of financial transactions and system audit logs.
    - Every business action (Issuance, Amendment) is linked to a unique **Transaction ID**, allowing for granular backtracking.

---

## 3. System Administration & Audit

### A. Managing Authority Tiers
1.  Navigate to **Administration > Authority Tiers**.
2.  The platform enforces **Dual Checker** logic for Tier 4 transactions automatically based on the configured amount thresholds.

### B. Analyzing Audit Logs
1.  Navigate to **Administration > System Audit Logs**.
    - Observe the interleaved feed of business transactions and low-level entity changes.
    - View exactly which transaction triggered each lifecycle state transition.

### C. Product Configuration
1.  Navigate to **Master Data > Product Config**.
2.  Configure standard **Incoterms** and **Document Checklist** templates which are proactively loaded into the Issuance Stepper.

---

### D. Compliance Holds & Release
1.  **Enforcement**: Transactions matching SDN entities or sanctions lists are automatically placed on **Compliance Hold**.
2.  **Release**: A Compliance Officer must review the instrument and explicitly release the hold via the **Release Hold** action in the detail view.

---

## 4. Security & Risk Controls
- **Dual Checker Enforcement**: High-value transactions are locked to unique **Transaction IDs** that require multi-stage authorization before the legal instrument is updated.
- **Role Isolation**: Makers cannot authorize their own transactions.
- **Narrative Traceability**: The platform maintains a persistent, chronological record of every touchpoint on an instrument via the unified timeline.

---

## Conclusion
For operational support, contact the **Trade Finance Operations Helpdesk** at ext 9999 or email `trade-support@bank.com`.
