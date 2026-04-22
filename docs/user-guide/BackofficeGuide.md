# Backoffice & System Admin Manual: Digital Trade Finance

This guide provides exhaustive, step-by-step instructions for Administrative and Backoffice users. The platform uses a "Four-Eyes" principle ensuring no single user can complete a sensitive operation.

![Navigation Overview](file:///Users/me/.gemini/antigravity/brain/222f8be2-5040-4a62-83e6-546ec9bc13b0/modern_flat_light_trade_navigation_mockup_v2_1776866522690.png)

---

## 1. Party & KYC Management
**Goal**: Onboard a new corporate entity or verify compliance status.

1.  **Initiation**: Click **Party Directory** in the **MASTER DATA** section of the sidebar.
2.  **Selection**: Use the search bar in the left pane to find the entity by Name or Legal ID (e.g., `P001`). Click the row to load details.
3.  **KYC Verification (Step-by-Step)**:
    - Click the **KYC** tab in the main workspace.
    - Review **Ultimate Beneficial Owner (UBO)** details.
    - Check the **Verification Date**. If expired (red dot), click the **Renew Record** action (to be implemented).
4.  **Compliance Audit**:
    - Switch to the **Compliance** tab.
    - Review the **Narrative** for adverse media flags.
    - Verify that **UN Sanctions** and **OFAC** status are marked as `CLEAR`.
5.  **Role Assignment**:
    - Click the **Roles** tab.
    - Verify the entity is authorized as an `Applicant` or `Beneficiary`. Active roles are highlighted in green.

---

## 2. Credit Facility Monitoring
**Goal**: Review bank-wide exposure and drill down into specific utilization.

1.  **Access**: Click **Credit Facilities** in the sidebar.
2.  **Overview Analysis**:
    - Review the **Total Exposure** cards at the top. Note the **Available Headroom** (highlighted in Green).
3.  **Drill-Down Utilization (Step-by-Step)**:
    - In the **Exposure by Instrument** section (Right Pane), click on a specific segment (e.g., **Import LC**).
    - The **Utilization Breakdown** table will refresh below.
    - Review the **EAD (Exposure at Default)** for each transaction.
    - Click any **Transaction Ref** (underlined) to pull up the full instrument audit log.
4.  **Risk Metrics**:
    - Scroll to the bottom to view **Weighted Average Tenor** and **Concentration Metrics** for the selected segment.

---

## 3. System Administration & Audit
**Goal**: Enforce security policies and audit system activity.

### A. Managing Authority Tiers
1.  Navigate to **Administration > Authority Tiers**.
2.  Review the **Approval Matrices**.
3.  **Action**: Verify that `TRADE_CHECKER` users have sufficient limits for the current transaction volume.

### B. Analyzing Audit Logs
1.  Navigate to **Administration > System Audit Logs**.
2.  **Step-by-Step Audit**:
    - Use the search bar to filter by **User ID** or **Transaction Ref**.
    - Observe the `MAKER_COMMIT` vs `CHECKER_APPROVE` events.
    - Click **View Delta** to see the exact fields changed during an amendment.

### C. Product Configuration
1.  Navigate to **Master Data > Product Config**.
2.  Review standard **Incoterms** and **Document Checklist** templates.
3.  **Action**: Update mandatory document requirements for specific LC types if regulatory policies change.

---

## 4. Advanced Features
- **Send Message**: From any Party detail view, click **Communicate** to trigger an internal SWIFT-formatted message.
- **Print Template**: Use the **Print** icon on any Facility view to generate a PDF snapshot of current utilization for management reporting.
