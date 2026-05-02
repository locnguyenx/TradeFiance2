# Backoffice & System Admin Manual: Digital Trade Finance

This guide provides exhaustive, step-by-step instructions for Administrative and Backoffice users. The platform runs on the **"Blue Premium"** enterprise design system, ensuring high visibility for audit and compliance workflows.

---

## 1. Party & KYC Management
**Goal**: Onboard a new corporate entity or verify compliance status.

1.  **Initiation**: Click **Party Directory** in the **MASTER DATA** section of the sidebar.
2.  **Creating a New Party**:
    - Click the **+ New Party** button.
    - Fill out the required **Identity Details** (Name, Address, Type).
    - If creating a Bank, provide the **SWIFT BIC** and **Active RMA** status.
3.  **Viewing & Editing (Full-Screen View)**:
    - Use the search bar to find an entity by Name or Legal ID.
    - Click **View Details** to enter the full profile.
    - Click **Edit Profile** to update expiring KYC documents, change the Sanctions status, or adjust FI Limits.
4.  **Role Assignment & Bank Settings**:
    - Verify commercial roles (Applicant/Beneficiary) in the **Identity Card**.
    - **Bank Parties**: Ensure correct configuration of **RMA (Relationship Management Application)** status and **Financial Institution (FI) Limits**. 
    - *Note*: Advising and Confirming banks require an active RMA. Confirming banks also require sufficient FI Limits to support LC issuance.

### Party Attributes Reference

| Field | Entity Scope | Requirement | Description / Validation Rule |
|-------|--------------|-------------|-------------------------------|
| **Party ID** | All Parties | **Mandatory** | Unique identifier for the corporate or bank entity. |
| **Party Type** | All Parties | **Mandatory** | Select `PARTY_COMMERCIAL` or `PARTY_BANK`. |
| **Name** | All Parties | **Mandatory** | Full legal name of the entity. |
| **Account No** | All Parties | Optional | Default IBAN or domestic account for settlements. |
| **Address** | All Parties | **Mandatory** | Formatted to max 4 lines, 35 chars per line (SWIFT standard). |
| **KYC Status** | All Parties | **Mandatory** | Must be `Active` to participate in new LC issuances. |
| **KYC Expiry** | All Parties | **Mandatory** | Date the KYC profile requires renewal. |
| **Sanctions** | All Parties | **Mandatory** | `SANCTION_CLEAR` required. `PENDING` or `BLOCKED` trigger Compliance Holds. |
| **Country Risk**| All Parties | Optional | ISO Country Code for risk aggregation. |
| **SWIFT BIC** | Bank Only | **Mandatory** | 8 or 11 character standard Bank Identifier Code. |
| **Clearing Code**| Bank Only | Optional | National routing code (used in SWIFT Option C). |
| **Active RMA** | Bank Only | **Mandatory** | `Y/N`. Required for Advising/Confirming roles. |
| **Nostro Ref** | Bank Only | Conditional | Our Nostro account identifier (required for Reimbursing/Drawee roles). |
| **FI Limit** | Bank Only | Conditional | Approved limit amount for counterparty risk (required for Confirming). |
| **FI Limit CCY**| Bank Only | Conditional | Currency of the FI Limit. |

> [!IMPORTANT]
> **SWIFT Character Validation**: Party `Name` and `Address` fields are validated against the **X-Character Set** (see End User Guide §11). Forbidden characters (`@`, `!`, `#`, `$`, etc.) are rejected at save time. These fields are injected directly into SWIFT MT 700/707 tags, so compliance is critical.
>
> **How Party Data Flows to SWIFT Messages**: When an LC is submitted, the system reads all party assignments from the `TradeInstrumentParty` junction. For each bank role, it retrieves the party's `swiftBic`, `partyName`, and `registeredAddress` from the master directory and injects them into the correct SWIFT tags (e.g., Advising Bank → Tag 57A). Backoffice users must ensure all bank party records are complete and accurate before Makers can assign them to instruments.
>
> **Multi-Role Support**: A single bank can hold multiple roles on the same instrument (e.g., Advising + Confirming + Negotiating). The system validates eligibility independently for each role.

---

## 2. Credit Facility & Exposure Monitoring
**Goal**: Review bank-wide exposure and drill down into specific utilization.

1.  **Facility Dashboard**: Click **Credit Facilities** in the sidebar.
2.  **Creating a New Facility**:
    - Click **+ New Facility**.
    - Select the **Owner Party** (Corporate).
    - Define the **Total Limit**, **Currency**, and **Expiry Date**.
3.  **Editing & Managing Exposure**:
    - Click on any facility row to view details.
    - Review the "Financial Hero Cards" for Total Exposure, Utilized, and Available Headroom.
    - Click **Edit Facility** to increase/decrease the Limit or extend the Expiry.
    - Use the **Utilization Breakdown** table to see individual transactions.
4.  **Audit Drill-Down**:
    - Click any underlined **Transaction Ref** in the breakdown to open the specific instrument.
    - Review the **Unified Narrative Timeline** for a merged view of financial transactions and system audit logs.
    - Every business action (Issuance, Amendment) is linked to a unique **Transaction ID**, allowing for granular backtracking.

### Facility Attributes Reference

| Field | Requirement | Description / Validation Rule |
|-------|-------------|-------------------------------|
| **Facility ID** | **Mandatory** | Unique system identifier for the credit line. |
| **Owner Party** | **Mandatory** | The Corporate Party (`PARTY_COMMERCIAL`) that owns this limit. |
| **Description** | Optional | Internal notes or facility tranche details. |
| **Total Limit** | **Mandatory** | The maximum aggregate exposure permitted. |
| **Utilized** | System | Auto-calculated sum of all active instrument liabilities. |
| **Currency** | **Mandatory** | Base currency of the limit. Cross-currency LCs use `baseEquivalentAmount`. |
| **Expiry Date** | **Mandatory** | Facility expiration. LCs cannot expire after this date. |

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
1.  **Access**: Navigate to **Master Data > Product Config**.
2.  **Creating a New Product**:
    - Click **+ New Product** to define a new operational template (e.g., Export LC).
    - Define structural flags (Tenors, Revolving options, Advance payments).
3.  **Editing an Existing Product**:
    - Select an existing product to open the Configuration View.
    - Click **Edit** to adjust **Max Tolerances**, standard **Incoterms**, or the default **Document Exam SLA Days**.
    - *Note*: Changes to products automatically propagate as defaults into the Issuance Stepper for new transactions.

### Product Catalog Reference

| Field | Requirement | Description / Validation Rule |
|-------|-------------|-------------------------------|
| **Product ID** | **Mandatory** | Unique identifier (e.g., `PROD_IMP_LC`). |
| **Type** | **Mandatory** | Product category (e.g., `IMP_LC`, `EXP_LC`). |
| **Name / Desc** | **Mandatory** | Human-readable marketing name and long description. |
| **Status** | **Mandatory** | Must be `PROD_ACTIVE` to appear in Issuance Stepper. |
| **Tenor** | Optional | Restricts instrument to `SIGHT` or `USANCE`. |
| **Max Tolerance**| Optional | Hard cap on positive tolerance % (e.g., `10` for 10%). |
| **Exam SLA** | **Mandatory** | Default SLA days for document examination (standard is `5`). |
| **Flags** | Optional | Toggles for `allowRevolving`, `allowAdvancePayment`, `isTransferable`. |
| **Margin %** | Optional | Default cash margin percentage required at issuance. |

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
