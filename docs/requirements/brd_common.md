Here is the detailed Business Requirements Document (BRD) for the Common Module. 

In strict alignment with standard business analysis practices, this document is entirely agnostic of the underlying technology stack, frameworks, or database architecture. It focuses purely on the business logic, data capture requirements, and operational rules necessary to support all Trade Finance products.

***

# Business Requirements Document (BRD)
**Project Name:** Digital Trade Finance Platform
**Module:** Common Trade Finance (Cross-Functional Core)
**Document Version:** 1.0
**Date:** April 21, 2026

## 1. Overview
The Common Module serves as the foundational layer for all Trade Finance (TF) transactions. Rather than duplicating logic across Import LCs, Export LCs, and Collections, this module centralizes the core business entities, validation rules, and processing flows that apply universally to every trade instrument.

## 2. Domain Business Entities & Key Attributes
These are the core business data structures that the system must capture and maintain. 

### 2.1. Trade Instrument (Base Transaction)
This entity represents the common data shared across all TF products.
* **Transaction Reference Number:** A unique, system-generated, human-readable identifier (e.g., TF-IMP-2026-0001).
* **Product Type:** The specific product category (e.g., Import LC, Export Collection).
* **Transaction Currency:** The three-letter ISO currency code of the instrument.
* **Transaction Amount:** The total monetary value of the instrument.
* **Base Equivalent Amount:** The value of the transaction converted to the bank's local operating currency for limit and reporting purposes.
* **Issue Date:** The business date the transaction is formally initiated.
* **Expiry/Maturity Date:** The date the instrument ceases to be valid or the date payment is due.
* **Lifecycle Status:** The current business state (e.g., *Draft, Pending Approval, Active, Hold, Closed*).

### 2.2. Trade Party (Customer & Bank Directory)
All entities involved in a transaction must be recorded and validated against this directory.
* **Party ID:** Unique identifier for the customer, correspondent bank, or corporate entity.
* **Legal Name & Registered Address:** The official legal details used for SWIFT messaging and document generation.
* **Role in Transaction:** The specific capacity of the party for a given transaction (e.g., Applicant, Beneficiary, Issuing Bank, Drawee).
* **KYC Status:** Indicator of whether the party's Know Your Customer vetting is *Active, Expired*, or *Pending*.
* **Sanctions Status:** Indicator of the party's screening results (*Clear, Suspended, Blocked*).
* **Country of Risk:** The primary jurisdiction of the party, used for country-limit exposure tracking.

### 2.3. Customer Facility (Credit Limits)
Required to ensure the bank does not take on unsecured risk beyond approved boundaries.
* **Facility ID:** Identifier for the approved credit line.
* **Total Approved Limit:** The maximum risk exposure allowed for this customer.
* **Utilized Amount:** The total value currently locked by active TF transactions.
* **Available Earmark:** The remaining balance available for new transactions.
* **Facility Expiry Date:** The date the credit line must be renewed.

## 3. Standardized Processing Flow
Every trade finance transaction, regardless of the specific product, must progress through this standardized operational workflow.

1. **Initiation (Data Capture):** An operations user or a customer (via an external portal) inputs the transaction details. The record remains in a *Draft* state.
2. **Pre-Processing Validations:** The system automatically checks business rules (KYC, Limit, Data Completeness). If validations fail, the user is prompted to correct the errors.
3. **Authorization (Maker/Checker):** Once submitted by the initiator (Maker), the transaction enters a *Pending Approval* state. It is routed to an authorized supervisor (Checker).
4. **Execution:** Upon Checker approval, the system commits the transaction, updates the facility limits, and generates any required outward SWIFT messages or physical cover letters. The state shifts to *Active* or *Issued*.
5. **Lifecycle Events:** The system logs subsequent events tied to the parent instrument, such as Amendments, Document Presentations, or Tracers.
6. **Settlement & Closure:** Funds are moved, final accounting entries are generated, and the transaction state is moved to *Closed*.

Refer to section "5. Maker/Checker Authorization Matrix & Authority Tiers" for Authorization details

## 4. Cross-Functional Validation Rules
These rules are mandatory and act as system "hard-stops" preventing a transaction from moving forward if violated.

### 4.1. Risk & Compliance Rules
* **KYC Validation:** The system must restrict the execution of any transaction if the primary customer (Applicant or Principal) has an "Expired" KYC status.
* **Sanctions Hold:** If any Trade Party, Vessel Name, or Port entered into the transaction matches a restricted entity on a global watch list, the transaction must immediately shift to a *Hold* status and route to the Compliance department. It cannot be authorized by standard operations staff.
* **Limit Availability:** The system must block the issuance of any funded or unfunded instrument if the *Base Equivalent Amount* exceeds the *Available Earmark* on the customer's facility.

### 4.2. Operational & Security Rules
* **Four-Eyes Principle (Segregation of Duties):** The user who initiates or modifies a transaction (Maker) cannot be the same user who approves it (Checker). The system must enforce this strictly at the user-identity level.
* **Approval Authority Limits:** A Checker can only approve transactions up to their assigned monetary authority tier. Transactions exceeding this tier must be automatically routed to higher management or a credit committee.
* **Historical Immutability:** Once a transaction reaches an *Active/Issued* state, none of its core attributes can be changed or deleted. Any modification must be processed as a formal "Amendment" event, which creates a new version record while preserving the original.

### 4.3. Business Logic Rules
* **Date Sequence Logic:** The Expiry Date must always be strictly greater than or equal to the Issue Date. 
* **Back-Valuation Restriction:** Users cannot set an Issue Date in the past without special administrative overrides.
* **Currency Verification:** Transactions can only be processed using active, bank-approved ISO currencies.

***

## 5. Maker/Checker Authorization Matrix & Authority Tiers
Here is the detailed continuation of the Common Module BRD, focusing specifically on the authorization matrix and authority tiers. 

This section defines the business risk controls and approval routing without prescribing how the underlying user access management or database roles will be technically implemented.

***

### 5.1. The Segregation of Duties Principle
The platform must strictly enforce the "Four-Eyes Principle" across all financial and legally binding actions. 
* **The Maker:** The operational user who performs data entry, initiates a new transaction, or proposes an amendment. The Maker verifies that documents and data match the customer's request.
* **The Checker:** The authorized supervisor who independently reviews the captured data against the source documents and internal credit policies before executing the transaction.
* **System Enforcement:** The system must definitively prevent the same individual from acting as both Maker and Checker on a single transaction lifecycle event, regardless of their seniority or assigned authority tier.

### 5.2. Delegated Authority Tiers
To balance operational efficiency with risk management, Checkers are assigned to specific Delegated Authority Tiers. These tiers dictate the maximum transaction value (calculated in the bank's local Base Equivalent Amount) they are authorized to approve independently.

| Tier Level | Typical Business Role | Maximum Approval Limit (Base Equivalent) | Routing Logic |
| :--- | :--- | :--- | :--- |
| **Tier 1** | Senior Trade Operations Officer | Up to 100,000 USD | Routine, low-value transactions. |
| **Tier 2** | Trade Finance Team Lead | Up to 1,000,000 USD | Standard commercial transactions. |
| **Tier 3** | Head of Trade Operations | Up to 5,000,000 USD | High-value, complex transactions. |
| **Tier 4** | Credit Risk Committee / Board | Above 5,000,000 USD | Exceptional value; requires specialized executive routing. |

*(Note: The actual monetary values and currencies are configurable parameters managed by the bank's central risk administration.)*

### 5.3. The Authorization Matrix & Workflows
When a Maker submits a transaction, the system must use the transaction's Base Equivalent Amount to route it to the appropriate Checker queue.

* **Single-Tier Approval:** For Tiers 1 through 3, only one Checker from the appropriate (or higher) tier is required to approve the transaction. 
    * *Example:* A $500,000 Import LC can be approved by a Tier 2 or Tier 3 user.
* **Joint Approval (Dual-Checker):** For Tier 4 transactions, the system must enforce a "Joint Approval" workflow. After the Maker submits the transaction, it requires approval from **two distinct** Tier 4 Checkers before execution.
* **Downward Delegation Restriction:** A Tier 1 user cannot approve a transaction requiring Tier 2 authority. The system must hard-stop the approval attempt and display an "Insufficient Authority" notice.

### 5.4. Special Authorization Scenarios

**A. Financial Amendments**
When a Maker processes an amendment that increases the financial value of an existing instrument, the authorization tier is determined by the **new total liability**, not just the delta.
* *Example:* An existing LC of $800,000 (Tier 2) is amended to add $300,000. The new total is $1,100,000. The amendment must be routed to a Tier 3 Checker for approval.

**B. Non-Financial Amendments**
Amendments that do not impact the credit limit or transaction value (e.g., changing a port of loading, correcting a typographical error in goods description) default to a Tier 1 authorization requirement, regardless of the instrument's total financial value.

**C. Parallel Compliance Approvals**
Financial authority tiers are superseded by Compliance rules. If a transaction triggers a "Sanctions Hold" or an "AML Anomaly" during system validation, the standard Maker/Checker flow is suspended.
* The transaction must be routed to a dedicated **Compliance Officer** role.
* The Compliance Officer must officially release the hold (with mandatory audit comments) before the standard operational Checker can finalize the financial approval.

***

This is a critical catch. In a Tier-1 banking environment, the operational workflows (like issuing an LC) are only as secure and profitable as the underlying governance and fee structures. 

Here is the detailed Business Requirements Document (BRD) continuation for the **Common Module**, focusing on the master data and administrative processes you highlighted.

***

### Section 6: Other Core Master Data & Administrative Processes

#### 5.1. Process: Fee & Charge Configuration & Calculation
Trade Finance generates significant non-funded revenue through complex fee structures. This process governs how business administrators define tariff rules, and how the system automatically applies them to transactions.

**A. Related States (For Configuration Changes)**
* **Tariff State:** Draft $\rightarrow$ Pending Approval $\rightarrow$ Active 
*(Note: Changing bank pricing is a high-risk event and must follow a Maker/Checker workflow, just like a financial transaction).*

**B. Business Process Workflow**
1. **Rule Definition (Maker):** An administrator creates or updates a fee rule in the tariff matrix (e.g., updating the SWIFT cable charge, or changing the LC Issuance percentage).
2. **Approval (Checker):** A senior manager reviews and approves the new pricing rule.
3. **Execution (System Calculation):** During a transaction (e.g., LC Issuance), the system identifies the required fee types.
4. **Exception Pricing:** The system checks if the specific Applicant has a negotiated "Customer Exception Rate". If yes, it overrides the standard tariff.
5. **Collection:** The system calculates the final fee amount and prepares the accounting entries to debit the customer and credit the bank's income GL.

**C. Inputs Capture (Data Dictionary - Tariff Matrix)**

| Data Group | Field Name | Req/Opt | Data Type | Validation Rules / Data Constraints |
| :--- | :--- | :--- | :--- | :--- |
| **Control** | Fee Event Trigger | Req | Enum | e.g., `LC Issuance`, `Amendment`, `Discrepancy`, `Payment`. |
| **Control** | Calculation Type | Req | Enum | Values: `Flat Rate`, `Percentage`, `Tiered`. |
| **Financial**| Base Rate / Amount | Req | Decimal | The standard price or percentage (e.g., 0.125 for %). |
| **Financial**| Minimum Charge | Opt | Decimal | e.g., "0.125%, minimum $50.00". |
| **Financial**| Maximum Charge | Opt | Decimal | e.g., "Flat fee up to a maximum of $5,000". |
| **Time** | Frequency / Period | Cond | Enum | Values: `One-Off`, `Per Month`, `Per Quarter`. (Required for % fees). |
| **Time** | Effective Date | Req | Date | Date the new pricing takes effect. Cannot backdate. |
| **Overrides**| Customer Tier | Opt | String | Apply this specific rule only to a defined segment (e.g., "VIP Corporate"). |

**D. Display / Computed Data (During Transaction Execution)**

| Field Name | Data Type | Calculation Formula / Processing Rules |
| :--- | :--- | :--- |
| **Time Units (Quarters)** | Integer | Used for LC issuance: `(LC Expiry Date - Issue Date) / 90 days`, rounded up to the next whole quarter. |
| **Calculated Base Fee** | Decimal | `LC Amount` $\times$ `Base Rate %` $\times$ `Time Units`. |
| **Final Applied Fee** | Decimal | `Calculated Base Fee`, enforced against the `Minimum Charge` and `Maximum Charge` boundaries. |

**E. Inbound / Outbound Integration**
* **Outbound:** Core Banking System (API to post fee income to the bank's Profit & Loss GL accounts, and debit the customer's CASA account).

---

#### 5.2. Process: User Authority Tiers & Access Management
This process defines how the system enforces the "Four-Eyes Principle" and controls which users have the financial authority to bind the bank to a liability.

**A. Related States (User Profile)**
* **Profile State:** Active, Suspended, Locked, Terminated.

**B. Business Process Workflow**
1. **Provisioning:** IT Security or Risk Management sets up a new user profile.
2. **Role Assignment:** The user is assigned functional roles (e.g., "Import LC Maker", "Collections Checker").
3. **Limit Assignment:** The user is assigned to a specific Approval Tier (e.g., Tier 2 - Up to $1M).
4. **Enforcement:** When a transaction is submitted, the system routes it strictly to users who hold both the correct Functional Role and a sufficient Approval Tier.

**C. Inputs Capture (Data Dictionary - User Profile)**

| Data Group | Field Name | Req/Opt | Data Type | Validation Rules / Data Constraints |
| :--- | :--- | :--- | :--- | :--- |
| **Identity** | User ID | Req | String | Unique system identifier, usually linked to Active Directory. |
| **Access** | Functional Roles | Req | Array | The specific modules/actions the user can access (e.g., `Create LC`, `Authorize Payment`). |
| **Access** | Maker/Checker Flag | Req | Enum | Values: `Maker Only`, `Checker Only`, `Dual`. *(Note: Even if 'Dual', system must prevent user from checking their own maker work).* |
| **Authority**| Delegation Tier | Cond | Enum | Values: `Tier 1`, `Tier 2`, `Tier 3`, `Tier 4`. Required if Checker flag is enabled. |
| **Authority**| Custom Limit | Opt | Decimal | A specific override limit in the bank's base currency (e.g., exactly $750,000). |
| **Control** | Branch Access | Req | Array | Restricts users to only view/process transactions for specific physical bank branches. |

**D. Specific Validation Rules**
* **Self-Approval Block:** Hard-coded system logic: If `Transaction.CreatedBy == CurrentUser`, hide the `[Authorize]` button, regardless of the user's Tier limit.
* **Suspended Accounts:** If a user is on leave or suspended, the system must immediately remove them from the routing matrix so items do not get stuck in their queue.

**E. Inbound / Outbound Integration**
* **Inbound:** Active Directory / Single Sign-On (SSO) for authentication (SAML/OAuth2). The BRD defines *authorization* (what they can do), but SSO handles *authentication* (who they are).

---

#### 5.3. Process: Audit Logs & Non-Repudiation
A Trade Finance system is highly regulated. Internal auditors, external regulators (like the State Bank of Vietnam), and compliance teams require immutable proof of exactly who performed what action, and when.

**A. Business Process Workflow**
This is a background system process. Operations users do not "interact" with the audit log; the system passively records all state mutations. Administrators and Auditors access a read-only query interface to investigate issues.

**B. System-Generated Inputs (Audit Payload)**
Every single time a record is created, updated, authorized, or deleted, the system must automatically capture the following payload in a write-only database table:

| Field Name | Data Type | Description & System Rules |
| :--- | :--- | :--- |
| **Timestamp** | DateTime | Captured at the exact millisecond of the database commit. System time, non-editable. |
| **User ID** | String | The ID of the authenticated user performing the action. |
| **IP Address** | String | The network location from which the request originated. |
| **Transaction Ref** | String | The business reference number (e.g., LC-2026-001) tied to the action. |
| **Action Performed** | Enum | e.g., `Submit for Approval`, `Authorize`, `Reject`, `Change Password`. |
| **Field Changed** | String | Name of the specific data field modified (e.g., "Expiry Date"). |
| **Old Value** | String | The data state before the action. |
| **New Value** | String | The data state after the action. |
| **Justification** | Text | Free-text reason provided by the user (Mandatory for actions like Rejections or Overrides). |

**C. Specific Validation & Business Rules**
* **Immutability (Append-Only):** The audit log must be strictly append-only. No user, not even a Super Administrator or Database Admin, should have application-level permissions to update or delete a row in the audit log.
* **Session Tracking:** If a transaction is processed by an automated system agent (e.g., "End of Day Batch"), the `User ID` must clearly reflect "SYSTEM" rather than a human identity.

**D. Report / File Generation Requirements**
* **Transaction History Report:** A view within the UI that allows a user to open an LC and see its entire lifecycle timeline (e.g., "Draft created by User A on Monday $\rightarrow$ Approved by User B on Tuesday $\rightarrow$ Amended by User A on Friday").
* **Compliance Extract:** The ability for administrators to export a cryptographically signed CSV/PDF of the audit logs for a specific date range or user, tailored for external auditor hand-offs.

***

# Product Configuration

## Overview
In the industry, this architectural pattern is known as **Product Parameterization** or the **Product Catalog Approach**. 

Instead of hardcoding the business logic, UI screens, and accounting entries for every possible variation of a Letter of Credit, modern systems build a core "LC Engine" (which we defined in our Common Module) and then use a configuration layer to define **Specific LC Products**.

### 1. The "Product Catalog" Concept
When an operations user or a corporate client initiates a new transaction, they do not just select "Import LC." They select a specific product from a configured catalog (e.g., *Import LC - Sight*, *Import LC - Back-to-Back*, *Import Islamic LC*). 

Selecting that specific product dynamically dictates how the system behaves.

### 2. How Product Selection Drives Specific Processing

By tying the transaction to a specific Product ID, the system automatically alters the workflow in several critical ways:

* **Dynamic UI & Data Validation:** * *Standard Sight LC:* The "Tenor Days" field is disabled and hidden.
    * *Usance LC:* The "Tenor Days" field becomes mandatory, and the system requires the user to specify whether the tenor is calculated from the "Bill of Lading Date" or "Sight".
* **Workflow & State Overrides:**
    * *Red Clause LC:* This is a special LC that allows the Beneficiary to draw an advance payment before shipping the goods. Selecting this product adds a mandatory "Advance Payment Authorization" state to the workflow before the standard document presentation phase.
    * *Standby LC (SBLC):* SBLCs act as guarantees. The system will bypass the standard 5-day document examination SLA, as documents are only presented in the rare event of a default.
* **Accounting & GL Routing:**
    * *Islamic Trade Finance (e.g., Murabaha LC):* Conventional LCs calculate interest on deferred payments. Islamic LCs strictly forbid interest. Selecting a Sharia-compliant product alters the accounting engine to post entries to "Profit Rate" GLs instead of "Interest" GLs, and alters the SWIFT narrative to ensure compliant terminology is used.
* **Limit Utilization:**
    * *Back-to-Back LC:* This involves issuing an Import LC backed by the security of a master Export LC. The system must process this differently by linking the two reference numbers and blocking the limits against the Export LC's value, rather than utilizing the standard corporate credit facility.

### 3. Implementation in the Data Model (Moqui Context)
To support this in a system like Moqui, you would decouple the product definition from the transaction record.

1.  **`TradeProduct` Entity:** A setup table managed by Business Admins containing records like `PROD_IMP_SIGHT`, `PROD_IMP_USANCE`, `PROD_IMP_REDCLAUSE`. This table contains flags (e.g., `allowAdvances = Y/N`, `requiresUsance = Y/N`).
2.  **`ImportLc` Entity:** The transaction table. It contains a foreign key pointing to `productId`.
3.  **The Rule Engine:** When a user creates an LC and selects `PROD_IMP_REDCLAUSE`, the UI reads the flags from the `TradeProduct` table and dynamically renders the "Advance Payment" fields, while the backend services apply the specific fee tariffs linked to that product.

***

## Product Configuration Design

Designing a robust Product Configuration Matrix is the secret to building a scalable Trade Finance platform. By externalizing the business rules into a parameterized catalog, the bank can launch new Trade products in days rather than waiting months for developers to write new code.

Here is the comprehensive **Product Configuration Matrix** and a detailed analysis of how these parameters dynamically alter the core business processes we previously defined.

***

### 1. The Product Configuration Matrix (The "Rulebook")

This matrix sits within the Common Module (`TradeProduct` entity in Moqui). Business Administrators configure these parameters to define a specific product (e.g., "Islamic Usance Import LC" or "Conventional Sight LC"). 

| Parameter Category | Configuration Flag / Field | Data Type | Description & Business Purpose |
| :--- | :--- | :--- | :--- |
| **General Control** | `ProductID` | String | Unique Identifier (e.g., `IMP_LC_RED_CLAUSE`). |
| **General Control** | `Product Name` | String | Commercial name displayed to users. |
| **General Control** | `Is Active` | Boolean | Controls if the product is available for new issuances. |
| **General Control** | `Product Type` | Enum | `LC Import`, `LC Export`, `Collection Import`, `Collection Export` |
| **Financial Terms** | `Allowed Tenor` | Enum | Limits the payment terms: `Sight Only`, `Usance Only`, or `Mixed`. |
| **Financial Terms** | `Max Tolerance Limit` | Integer | Hard ceiling on the tolerance percentage (e.g., 10%). Prevents users from inputting 50% tolerance. |
| **Financial Terms** | `Allow Revolving` | Boolean | If `True`, enables the UI fields to configure time-based or value-based automatic reinstatements. |
| **Specialized Features**| `Allow Advance Payment` | Boolean | Enables "Red Clause" or "Green Clause" logic, allowing the Beneficiary to draw funds before shipment. |
| **Specialized Features**| `Is Standby (SBLC)` | Boolean | Flags the product as a Guarantee rather than a Commercial LC. Alters SLA and presentation rules. |
| **Specialized Features**| `Is Transferable` | Boolean | Relevant mostly for Export LCs, but dictates if the LC can be reassigned to a second beneficiary. |
| **Accounting & GL** | `Accounting Framework` | Enum | Values: `Conventional` or `Islamic (Sharia)`. Dictates the underlying GL posting rules (Interest vs. Profit Rate). |
| **Accounting & GL** | `Mandatory Cash Margin` | Integer | Specifies a minimum % of cash collateral required at issuance, regardless of the customer's facility limit. |
| **Workflow / SLA** | `Document Exam SLA Days`| Integer | Overrides the standard 5-day UCP 600 rule (e.g., SBLCs might require immediate 1-day processing). |
| **Messaging** | `Default SWIFT Format` | Enum | Forces the output message type (e.g., MT 700 for Commercial LCs, MT 760 for Guarantees/Standbys). |

---

### 2. Impact on Core Business Processes

When a Maker initiates a transaction and selects a specific `ProductID`, the system reads the matrix above and dynamically alters the execution of the processes we defined in Section 3 of the BRD.

#### Impact on Process 3.1: LC Issuance
* **Dynamic UI Rendering:** The Maker's data entry screen adapts instantly. If `Allowed Tenor` is set to `Sight Only`, the "Usance Days" field is completely hidden, eliminating the chance of data entry error. 
* **Pre-Processing Validations:** If the product has `Mandatory Cash Margin = 100` (e.g., for high-risk customers or specific promotional products), the system will block the Checker from authorizing the issuance unless exactly 100% of the LC value is successfully held in the Applicant's deposit account, bypassing the standard credit facility check.
* **Message Generation:** If `Is Standby (SBLC)` is `True`, the system automatically formats the output as an MT 760 instead of an MT 700, changing the mandatory SWIFT tags required during data entry.

#### Impact on Process 3.2: Amendments
* **Constraint Enforcement:** An Applicant cannot amend an LC in a way that violates its Product Configuration. For example, if a "Sight Only LC" is issued, an amendment to change it to "90 Days Usance" will be systematically blocked. The user would have to cancel the LC and issue a new one under a different product code.
* **Revolving Logic:** If `Allow Revolving` is `True`, the amendment process includes special rules to alter the frequency or maximum cumulative limits of the revolving schedule, which are hidden for standard products.

#### Impact on Process 3.3: Document Presentation & Examination
* **SLA Countdown Timer:** The standard system timer for examining documents is 5 banking days. However, if the `Document Exam SLA Days` is set to `2` for a specialized product, the system dashboard will flag the presentation as "Overdue" much faster, altering the operational priority queue.
* **Advance Drawings (Red Clause):** If `Allow Advance Payment` is `True`, the Presentation screen enables a special workflow. Operations can log a "Clean Presentation" for a cash advance against merely a simple receipt (without any shipping documents like a Bill of Lading), which is strictly forbidden under standard LC rules.

#### Impact on Process 3.4: Settlement & Payment
* **Revolving Reinstatement:** Upon successful settlement, the system checks the `Allow Revolving` flag. If `True`, the system automatically triggers a background process to reinstate the LC amount back to its original value, bypassing the need for a manual amendment.

***

By building the architecture this way, your Trade Finance system becomes incredibly resilient and adaptable. The operations team is guided by the system, ensuring compliance, while the business side can invent new trade products simply by toggling configuration flags.
