# Business Requirements Document (BRD)

**Project Name:** Digital Trade Finance Platform
**Module:** Common Trade Finance (Cross-Functional Core)
**Document Version:** 2.2 (Full Detail Integration)
**Date:** April 21, 2026

## 1. Document Control & Scope
The Common Module serves as the foundational layer for all Trade Finance (TF) transactions. Rather than duplicating logic across Import LCs, Export LCs, and Collections, this module centralizes the core business entities, validation rules, currency precision, calendars, and processing flows that apply universally to every trade instrument.

## 2. Domain Business Entities & Key Attributes
These are the core business data structures that the system must capture and maintain. 

### REQ-COM-ENT-01: Trade Instrument (Base Transaction)
This entity represents the common data shared across all TF products.
* **Transaction Reference Number:** A unique, system-generated, human-readable identifier (e.g., TF-IMP-2026-0001).
* **Product Type:** The specific product category (e.g., Import LC, Export Collection).
* **Transaction Currency:** The three-letter ISO currency code of the instrument.
* **Transaction Amount:** The total monetary value of the instrument.
* **Base Equivalent Amount:** The value of the transaction converted to the bank's local operating currency for limit and reporting purposes.
* **Issue Date:** The business date the transaction is formally initiated.
* **Expiry/Maturity Date:** The date the instrument ceases to be valid or the date payment is due.
* **Lifecycle Status:** The current business state (e.g., *Draft, Pending Approval, Active, Hold, Closed*).

### REQ-COM-ENT-02: Trade Party (Customer & Bank Directory)
All entities involved in a transaction must be recorded and validated against this directory.
* **Party ID:** Unique identifier for the customer, correspondent bank, or corporate entity.
* **Legal Name & Registered Address:** The official legal details used for SWIFT messaging and document generation.
* **Role in Transaction:** The specific capacity of the party for a given transaction (e.g., Applicant, Beneficiary, Issuing Bank, Drawee).
* **KYC Status:** Indicator of whether the party's Know Your Customer vetting is *Active, Expired*, or *Pending*.
* **Sanctions Status:** Indicator of the party's screening results (*Clear, Suspended, Blocked*).
* **Country of Risk:** The primary jurisdiction of the party, used for country-limit exposure tracking.

### REQ-COM-ENT-03: Customer Facility (Credit Limits)
Required to ensure the bank does not take on unsecured risk beyond approved boundaries.
* **Facility ID:** Identifier for the approved credit line.
* **Total Approved Limit:** The maximum risk exposure allowed for this customer.
* **Utilized Amount:** The total value currently locked by active TF transactions.
* **Available Earmark:** The remaining balance available for new transactions.
* **Facility Expiry Date:** The date the credit line must be renewed.

## 3. Standardized Processing Flow
### REQ-COM-WF-01: Processing Flow Steps
Every trade finance transaction, regardless of the specific product, must progress through this standardized operational workflow.
1. **Initiation (Data Capture):** An operations user or a customer (via an external portal) inputs the transaction details. The record remains in a *Draft* state.
2. **Pre-Processing Validations:** The system automatically checks business rules (KYC, Limit, Data Completeness). If validations fail, the user is prompted to correct the errors.
3. **Authorization (Maker/Checker):** Once submitted by the initiator (Maker), the transaction enters a *Pending Approval* state. It is routed to an authorized supervisor (Checker).
4. **Execution:** Upon Checker approval, the system commits the transaction, updates the facility limits, and generates any required outward SWIFT messages or physical cover letters. The state shifts to *Active* or *Issued*.
5. **Lifecycle Events:** The system logs subsequent events tied to the parent instrument, such as Amendments, Document Presentations, or Tracers.
6. **Settlement & Closure:** Funds are moved, final accounting entries are generated, and the transaction state is moved to *Closed*.

## 4. Currency, FX Rules & SLA Calendars (New)
### REQ-COM-FX-01: Currency Precision
The system MUST respect ISO-standard decimal precision uniformly (e.g., USD = 2 decimals, JPY = 0 decimals) for all fields, limits, and settlement calculations.

### REQ-COM-FX-02: Dual-Rate FX Integrations
The system MUST implement a dual-rate integration model to balance execution speed and financial risk:
* **Facility Blocking (Daily Board Rate):** For all limit availability and credit calculations, the system MUST utilize a "Daily Board Rate" synchronized once at End-Of-Day. This stabilizes checking workflows by ensuring pending requests don't randomly fail due to intraday FX swings.
* **Physical Settlement (Live Rate):** For actual accounting settlement (MT202/103 remittance), the system MUST require an active Live FX Treasury API call to guarantee the bank bears zero live market exposure on the cash movement.

### REQ-COM-SLA-01: Universal Banking Calendar
All duration calculations (e.g., the UCP 600 maximum 5-banking-day document checking window) MUST be evaluated systematically against a Single Global Head-Office Calendar, neutralizing differences in international regional holidays. Weekends and global bank holidays are systematically skipped.

### REQ-COM-SLA-02: Overdue Enforcement
If a presentation remains unchecked after 3 banking days, a warning flag MUST be elevated. On the 5th day, the item MUST become a critical blocking exception.

## 5. Required Notifications
### REQ-COM-NOT-01: SLA and Threshold Alerts
The system MUST generate automated email alerts routed to the respective group inbox when:
- An SLA timer breaches Day 3.
- An Applicant facility limit reaches 95% utilization.

### REQ-COM-NOT-02: Compliance & Risk Holds
If any transaction triggers a Sanctions block or KYC expiration during submission, the system MUST immediately send a prioritized alert to the designated Compliance Review queue/inbox.

## 6. Cross-Functional Validation Rules
These rules are mandatory and act as system "hard-stops" preventing a transaction from moving forward if violated.

### REQ-COM-VAL-01: Risk & Compliance Rules
* **KYC Validation:** The system must restrict the execution of any transaction if the primary customer (Applicant or Principal) has an "Expired" KYC status.
* **Sanctions Hold:** If any Trade Party, Vessel Name, or Port entered into the transaction matches a restricted entity on a global watch list, the transaction must immediately shift to a *Hold* status and route to the Compliance department. It cannot be authorized by standard operations staff.
* **Limit Availability:** The system must block the issuance of any funded or unfunded instrument if the *Base Equivalent Amount* exceeds the *Available Earmark* on the customer's facility.

### REQ-COM-VAL-02: Operational & Security Rules
* **Four-Eyes Principle (Segregation of Duties):** The user who initiates or modifies a transaction (Maker) cannot be the same user who approves it (Checker). The system must enforce this strictly at the user-identity level.
* **Approval Authority Limits:** A Checker can only approve transactions up to their assigned monetary authority tier. Transactions exceeding this tier must be automatically routed to higher management or a credit committee.
* **Historical Immutability:** Once a transaction reaches an *Active/Issued* state, none of its core attributes can be changed or deleted. Any modification must be processed as a formal "Amendment" event, which creates a new version record while preserving the original.

### REQ-COM-VAL-03: Business Logic Rules
* **Date Sequence Logic:** The Expiry Date must always be strictly greater than or equal to the Issue Date. 
* **Back-Valuation Restriction:** Users cannot set an Issue Date in the past without special administrative overrides.
* **Currency Verification:** Transactions can only be processed using active, bank-approved ISO currencies.

## 7. Maker/Checker Authorization Matrix & Authority Tiers

### REQ-COM-AUTH-01: Delegated Authority Tiers
Checkers are assigned to specific Delegated Authority Tiers based on Base Equivalent Amount.

| Tier Level | Typical Business Role | Maximum Approval Limit (Base Equivalent) | Routing Logic |
| :--- | :--- | :--- | :--- |
| **Tier 1** | Senior Trade Operations Officer | Up to 100,000 USD | Routine, low-value transactions. |
| **Tier 2** | Trade Finance Team Lead | Up to 1,000,000 USD | Standard commercial transactions. |
| **Tier 3** | Head of Trade Operations | Up to 5,000,000 USD | High-value, complex transactions. |
| **Tier 4** | Credit Risk Committee / Board | Above 5,000,000 USD | Exceptional value; requires specialized executive routing. |

### REQ-COM-AUTH-02: The Authorization Matrix & Workflows
When a Maker submits a transaction, the system must use the transaction's Base Equivalent Amount to route it to the appropriate Checker queue.
* **Single-Tier Approval:** For Tiers 1 through 3, only one Checker from the appropriate (or higher) tier is required to approve the transaction. 
* **Joint Approval (Dual-Checker):** For Tier 4 transactions, the system must enforce a "Joint Approval" workflow. After the Maker submits the transaction, it requires approval from **two distinct** Tier 4 Checkers before execution.
* **Downward Delegation Restriction:** A Tier 1 user cannot approve a transaction requiring Tier 2 authority. The system must hard-stop the approval attempt and display an "Insufficient Authority" notice.

### REQ-COM-AUTH-03: Special Authorization Scenarios
* **A. Financial Amendments:** When a Maker processes an amendment that increases the financial value of an existing instrument, the authorization tier is determined by the **new total liability**, not just the delta.
* **B. Non-Financial Amendments:** Amendments that do not impact the credit limit or transaction value default to a Tier 1 authorization requirement.
* **C. Parallel Compliance Approvals:** Financial authority tiers are superseded by Compliance rules. If a transaction triggers a "Sanctions Hold" or an "AML Anomaly" during system validation, the standard Maker/Checker flow is suspended. It must be routed to a dedicated Compliance Officer role. The Compliance Officer must officially release the hold (with mandatory audit comments) before the standard operational Checker can finalize the financial approval.

## 8. Other Core Master Data & Administrative Processes

### REQ-COM-MAS-01: Fee & Charge Configuration & Calculation

**Business Process Workflow:**
1. **Rule Definition (Maker):** An administrator creates or updates a fee rule.
2. **Approval (Checker):** A senior manager reviews and approves the new pricing rule.
3. **Execution (System Calculation):** During a transaction, the system identifies required fee types.
4. **Exception Pricing:** The system checks if the specific Applicant has a negotiated "Customer Exception Rate". If yes, it overrides the standard tariff.
5. **Collection:** The system calculates the final fee and prepares accounting entries.

**Inputs Capture (Data Dictionary - Tariff Matrix):**
| Field Name | Req/Opt | Data Type | Description |
| :--- | :--- | :--- | :--- |
| `Fee Event Trigger` | Req | Enum | e.g., LC Issuance, Amendment, Discrepancy, Payment. |
| `Calculation Type` | Req | Enum | Values: Flat Rate, Percentage, Tiered. |
| `Base Rate / Amount`| Req | Decimal | The standard price or percentage. |
| `Minimum Charge` | Opt | Decimal | Minimum floor for percentage fees. |
| `Maximum Charge` | Opt | Decimal | Maximum ceiling. |
| `Frequency / Period`| Cond | Enum | Values: One-Off, Per Month, Per Quarter. |
| `Effective Date` | Req | Date | Date the new pricing takes effect. Cannot backdate. |
| `Customer Tier` | Opt | String | Override for specific customer segments (e.g., VIP Corporate). |

**Display / Computed Data:**
* `Time Units (Quarters)`: Used for LC issuance: (LC Expiry Date - Issue Date) / 90 days, rounded up to the next whole quarter.
* `Calculated Base Fee`: `LC Amount` × `Base Rate %` × `Time Units`.
* `Final Applied Fee`: `Calculated Base Fee`, enforced against `Minimum Charge` and `Maximum Charge`.

### REQ-COM-MAS-02: User Authority Tiers & Access Management

**Inputs Capture (Data Dictionary - User Profile):**
| Field Name | Req/Opt | Data Type | Description |
| :--- | :--- | :--- | :--- |
| `User ID` | Req | String | Unique system identifier, usually linked to Active Directory. |
| `Functional Roles` | Req | Array | The specific modules/actions the user can access. |
| `Maker/Checker Flag`| Req | Enum | Values: Maker Only, Checker Only, Dual. |
| `Delegation Tier` | Cond | Enum | Values: Tier 1, Tier 2, Tier 3, Tier 4. |
| `Custom Limit` | Opt | Decimal | A specific override limit in the bank's base currency. |
| `Branch Access` | Req | Array | Restricts users to only view/process transactions for specific branches. |

**Specific Validation Rules:**
* **Self-Approval Block:** Hard-coded system logic: If `Transaction.CreatedBy == CurrentUser`, hide the `[Authorize]` button, regardless of the user's Tier limit.
* **Suspended Accounts:** If a user is on leave or suspended, the system must immediately remove them from the routing matrix so items do not get stuck in their queue.

### REQ-COM-MAS-03: Audit Logs & Non-Repudiation

**System-Generated Inputs (Audit Payload):**
| Field Name | Data Type | Description & System Rules |
| :--- | :--- | :--- |
| `Timestamp` | DateTime | Captured at exact millisecond of commit. System time, non-editable. |
| `User ID` | String | The ID of the authenticated user performing the action. |
| `IP Address` | String | The network location from which the request originated. |
| `Transaction Ref` | String | The business reference number. |
| `Action Performed` | Enum | e.g., Submit for Approval, Authorize, Reject. |
| `Field Changed` | String | Name of the specific data field modified. |
| `Old Value` | String | The data state before the action. |
| `New Value` | String | The data state after the action. |
| `Justification` | Text | Free-text reason (Mandatory for Rejections/Overrides). |

**Specific Validation & Generation Rules:**
* **Immutability (Append-Only):** No user or admin should have permissions to delete/update a row in the audit log.
* **Session Tracking:** If processed by an automated system agent, User ID must clearly reflect "SYSTEM".
* **Report Generation:** System MUST support `Transaction History Report` (UI view) and `Compliance Extract` (cryptographically signed CSV/PDF export).

## 9. Product Configuration (The Catalog Approach)

### REQ-COM-PRD-01: Product Configuration Matrix
This matrix sits within the Common Module (`TradeProduct` entity). Business Administrators configure these parameters to define a specific product (e.g., "Conventional Sight Import LC").

| Configuration Flag / Field | Data Type | Description & System Rules |
| :--- | :--- | :--- |
| `ProductID` | String | Unique Identifier (e.g., `IMP_LC_RED_CLAUSE`). |
| `Product Name` | String | Commercial name displayed to users. |
| `Is Active` | Boolean | Controls if the product is available for new issuances. |
| `Product Type` | Enum | `LC Import`, `LC Export`, `Collection Import`, `Collection Export` |
| `Allowed Tenor` | Enum | Limits the payment terms: `Sight Only`, `Usance Only`, or `Mixed`. |
| `Max Tolerance Limit` | Integer | Hard ceiling on the tolerance percentage (e.g., 10%). |
| `Allow Revolving` | Boolean | If True, enables UI fields for automatic reinstatements. |
| `Allow Advance Payment`| Boolean | Enables "Red/Green Clause" logic, allowing Beneficiary to draw funds before shipment. |
| `Is Standby (SBLC)` | Boolean | Flags the product as a Guarantee. Alters SLA and presentation rules. |
| `Is Transferable` | Boolean | Dictates if the LC can be reassigned to a second beneficiary. |
| `Accounting Framework`| Enum | Values: `Conventional` or `Islamic`. Dictates underlying GL posting rules (Interest vs. Profit Rate). |
| `Mandatory Cash Margin`| Integer | Specifies a minimum % of cash collateral required at issuance, bypassing standard facilities. |
| `Document Exam SLA Days`| Integer | Overrides the standard 5-day UCP 600 rule (e.g., SBLCs might require 1-day). |
| `Default SWIFT Format`| Enum | Forces the output message type (e.g., MT 700 for LC, MT 760 for Standbys). |

### REQ-COM-PRD-02: Impact on Core Business Processes
When a Maker selects a specific `ProductID`, the system reads the matrix and dynamically alters processes:
* **Dynamic UI Rendering:** Adapts entry screen instantly (e.g., hiding "Usance Days" if `Sight Only`).
* **Pre-Processing Validations:** Forces exact checks (e.g., blocking Checker if `Mandatory Cash Margin` is not fully held).
* **Message Generation:** Formats SWIFT dynamically (e.g., MT760 instead of MT700 for `Is Standby = True`).
* **SLA Countdown Timer:** Uses the overridden `Document Exam SLA Days` for alerts and blocks.
* **Advance Drawings:** Allows clean presentations against simple receipts if `Allow Advance Payment = True`, bypassing standard shipping document requirements.
* **Accounting/Settlement:** Reinstates Revolving LCs automatically upon settlement if `Allow Revolving = True`. Posts to Islamic GLs if `Accounting Framework = Islamic`.
