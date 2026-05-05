# Consolidated Business Requirements Document (BRD)
**ABOUTME:** 
Consolidated requirements for the Common Trade Finance Module.
This document merges baseline requirements with refactoring and transaction tracking specs.

**Project Name:** Digital Trade Finance Platform
**Module:** Common Trade Finance (Cross-Functional Core)
**Document Version:** 1.3 (Verified & Scrutinized)
**Date:** May 05, 2026

**Superseded BRDs:**
*   [2026-04-21-common-module-brd.md](file:///Users/me/myprojects/moqui-trade/docs/superpowers/specs/2026-04-21-common-module-brd.md)
*   [2026-04-30-tradeparty-refactor-brd.md](file:///Users/me/myprojects/moqui-trade/docs/superpowers/specs/2026-04-30-tradeparty-refactor-brd.md)
*   [2026-04-28-trade-transaction-tracking-brd.md](file:///Users/me/myprojects/moqui-trade/docs/superpowers/specs/2026-04-28-trade-transaction-tracking-brd.md)
*   [2026-05-03-user-identity-access-brd.md](file:///Users/me/myprojects/moqui-trade/docs/superpowers/specs/2026-05-03-user-identity-access-brd.md)
*   [2026-04-22-gap_analysis.md](file:///Users/me/myprojects/moqui-trade/docs/superpowers/specs/2026-04-22-gap_analysis.md) (Common Sections)

---

## 1. Feature 1: Trade Party Management (Refactored)

### 1.1 Final Requirements in Detail

#### A. Domain Entity Model (Data Dictionary)

**Trade Party (Master Identity)**
| Field Name | Req/Opt | Data Type | Description |
| :--- | :--- | :--- | :--- |
| `Party ID` | Req | String | Unique system identifier. |
| `Party Type` | Req | Enum | Values: `PARTY_COMMERCIAL`, `PARTY_BANK`. |
| `Legal Name` | Req | String | Official name for SWIFT (Max 35 chars). |
| `Account Number` | Opt | String | Mandatory for MT103 (Beneficiary). |
| `Registered Address`| Req | Text | Max 4 lines of 35 characters. |
| `KYC Status` | Req | Enum | Active, Expired, Pending. |
| `KYC Expiry Date` | Req | Date | Mandatory if KYC is active. |
| `Sanctions Status` | Req | Enum | Clear, Suspended, Blocked. |
| `Country of Risk` | Req | String | ISO country code for limit tracking. |

**Trade Party Bank (Relationship Extension)**
| Field Name | Req/Opt | Data Type | Description |
| :--- | :--- | :--- | :--- |
| `SWIFT BIC` | Req | String | 8 or 11 character identifier. |
| `Clearing Code` | Opt | String | National routing code for SWIFT Option C (e.g. ABA). |
| `Active RMA` | Req | Boolean | Mandatory 'Yes' for Advising Bank role. |
| `Nostro Reference` | Cond | String | Required for Reimbursing Bank role. |
| `FI Limit Available`| Opt | Decimal | Credit line for bank-to-bank liability. |
| `FI Limit Currency` | Opt | String | Currency of the FI limit. |

**Transaction Role Assignment (Junction)**
| Field Name | Req/Opt | Data Type | Description |
| :--- | :--- | :--- | :--- |
| `Instrument ID` | Req | String | Link to the parent transaction. |
| `Role` | Req | Enum | See Role Matrix below. |
| `Party ID` | Req | String | The party filling this specific role. |

#### B. Role Enumeration (Supported Roles)
| Role ID | Description | Party Type | Primary SWIFT Tag |
| :--- | :--- | :--- | :--- |
| `TP_APPLICANT` | Applicant (Ordering Customer) | Commercial | Tag 50 |
| `TP_BENEFICIARY` | Beneficiary | Commercial | Tag 59 |
| `TP_ISSUING_BANK` | Issuing Bank (Our Bank) | Bank | Header (Sender) |
| `TP_APPLICANT_BANK` | Applicant Bank | Bank | Tag 51a |
| `TP_ADVISING_BANK` | Advising Bank | Bank | Header (Receiver) |
| `TP_ADVISE_THRU` | Advise Through Bank | Bank | Tag 57a |
| `TP_CONFIRMING` | Confirming / Requested Confirming | Bank | Tag 58a |
| `TP_REIMBURSING` | Reimbursing Bank | Bank | Tag 53a |
| `TP_NEGOTIATING` | Negotiating / Available With Bank | Bank | Tag 41a |
| `TP_DRAWEE` | Drawee Bank | Bank | Tag 42a |
| `TP_PRESENTING` | Presenting Bank | Bank | MT750/734/752 |
| `TP_INTERMEDIARY` | Intermediary Bank (Settlement) | Bank | Tag 56a (MT202) |
| `TP_SENDERS_CORR` | Sender's Correspondent | Bank | Tag 53a (MT202) |
| `TP_RECEIVERS_CORR` | Receiver's Correspondent | Bank | Tag 54a (MT202) |

#### C. Tag Coverage Verification Matrix
| MT Message | Tag | Party Role Used | Account Number Rule |
| :--- | :--- | :--- | :--- |
| **MT 700** | 50 | `TP_APPLICANT` | Optional |
| **MT 700** | 59 | `TP_BENEFICIARY` | Optional |
| **MT 700** | 51a | `TP_APPLICANT_BANK` | Forbidden |
| **MT 700** | 53a | `TP_REIMBURSING` | Forbidden |
| **MT 700** | 57a | `TP_ADVISE_THRU` | Forbidden |
| **MT 700** | 41a | `TP_NEGOTIATING` | Strictly Forbidden |
| **MT 700** | 42a | `TP_DRAWEE` | Strictly Forbidden |
| **MT 707** | 59 | `TP_BENEFICIARY` (New) | Optional |
| **MT 734** | 57a | `TP_PRESENTING` | Optional |
| **MT 202** | 53a | `TP_SENDERS_CORR` | Optional |
| **MT 202** | 54a | `TP_RECEIVERS_CORR` | Optional |
| **MT 202** | 56a | `TP_INTERMEDIARY` | Optional |

#### D. Related Business Logic
*   **Advising Eligibility**: The system blocks any bank without `Active RMA = Yes` from being assigned to the Advising Bank role (Ref: `FR-TP-02`).
*   **Reimbursing Eligibility**: Assignment as a Reimbursing Bank requires a non-null `Nostro Reference`.
*   **Confirming Eligibility**: Confirmation is only permitted if the bank's `FI Limit Available` is greater than or equal to the transaction's maximum liability.
*   **Compliance Block**: The system prevents submission to Checker if any assigned party has a `KYC Status` of "Expired" or a `Sanctions Status` of "Blocked" (Ref: `REQ-COM-ENT-02`).
*   **Negotiation Rule**: If `TP_NEGOTIATING` is assigned a specific bank, the account number is strictly forbidden in Tag 41a. If "Any Bank" is selected, Tag 41a must use code `ANY BANK`.
*   **SWIFT Option D Fallback**: If a BIC is not available, the system must utilize the `Legal Name` and `Registered Address` (SWIFT Option D) for banking tags.

#### E. User Stories

### US-TP-01: Party Master Data with Type-Specific Fields
**As a** system administrator,
**I want** to manage trade parties with dedicated field sets based on party type (Commercial vs Bank),
**So that** commercial parties store KYC/address data and bank parties additionally store relationship attributes (RMA, Nostro, FI Limits).

**Acceptance Criteria:**
- Commercial parties: partyName, registeredAddress, KYC status, sanctions status, country of risk.
- Bank parties: all commercial fields PLUS swiftBic, clearingCode, hasActiveRMA, nostroAccountRef, fiLimitAvailable.
- Party type (COMMERCIAL/BANK) is set at creation and determines which fields are applicable.

### US-TP-02: Instrument Party Role Assignment
**As a** Trade Operations Maker,
**I want** to assign parties to specific roles on an LC instrument,
**So that** each role (Applicant, Beneficiary, Advising Bank, etc.) is filled by exactly one party per instrument.

**Acceptance Criteria:**
- Each role on an instrument maps to exactly one party.
- A single party can be assigned to multiple different roles on the same instrument.
- Removing a role assignment deletes the junction record.

### US-TP-03: Bank Eligibility Validation per Role
**As a** Trade Operations system,
**I want** to validate that a bank meets the relationship requirements for its assigned role,
**So that** invalid banks are blocked from transaction roles.

**Acceptance Criteria:**
- Advising Bank assignment requires `hasActiveRMA = Y`.
- Reimbursing Bank assignment requires `nostroAccountRef` populated.

### US-TP-04: FI Limit Enforcement for Confirmation
**As a** Trade Operations system,
**I want** to verify the available FI credit limit before allowing a bank to be assigned as a Confirming Bank,
**So that** the bank's exposure remains within approved risk boundaries.

**Acceptance Criteria:**
- Confirming Bank assignment is blocked if `fiLimitAvailable` < transaction amount.

### US-TP-05: Multi-Role Assignment for Banks
**As a** Trade Operations Maker,
**I want** to assign a single bank to multiple roles on the same instrument (e.g., Advising and Confirming Bank),
**So that** I can accurately represent complex banking relationships.

**Acceptance Criteria:**
- System allows the same `partyId` to be used for multiple `roleEnumId` records on one instrument.

### US-TP-06: BIC-to-Party Auto-Population
**As a** Trade Operations Maker,
**I want** the system to automatically retrieve party details when I enter a valid SWIFT BIC,
**So that** data entry is faster and more accurate.

**Acceptance Criteria:**
- Entering a recognized BIC auto-fills the Name and Address fields.

#### F. Grounding Info
*   **Role Enumeration**: Proof from `2026-04-30-tradeparty-refactor-brd.md` Section **FR-TP-04**, defining all 14 roles.
*   **Junction Pattern**: Proved by Section **FR-TP-03** of the same document, replaces flat BIC fields with `TradeInstrumentParty`.
*   **Eligibility Rules**: Grounded in **US-TP-03** (RMA) and **US-TP-04** (FI Limit) of the refactor spec.
*   **KYC Compliance**: Retained from **REQ-COM-ENT-02** in `2026-04-21-common-module-brd.md`.

#### G. Dropped Requirements
*   **Flat BIC Fields**: Explicitly dropped by **FR-TP-01** entity refactor; BICs now reside in `TradePartyBank`.

---

## 2. Feature 2: Credit Facility & Limit Dashboard

### 2.1 Final Requirements in Detail

#### A. Domain Entity Model (Data Dictionary)

**Customer Facility**
| Field Name | Req/Opt | Data Type | Description |
| :--- | :--- | :--- | :--- |
| `Facility ID` | Req | String | Unique reference for the credit line. |
| `Total Limit` | Req | Decimal | Maximum risk exposure allowed. |
| `Utilized Amount` | Req | Decimal | Sum of all active transaction liabilities. |
| `Available Earmark`| Req | Decimal | `Total Limit` - `Utilized Amount`. |
| `Expiry Date` | Req | Date | Date when the facility must be renewed. |

#### B. Related Business Logic
*   **Dual-Rate FX Model**: (Ref: `REQ-COM-FX-02`)
    *   **Facility Checks**: Use the "Daily Board Rate" (synchronized at EOD) for stable availability.
    *   **Remittances**: Use the "Live Treasury Rate" at the moment of payment to eliminate market risk.
*   **Earmarking Rule**: Earmarks are calculated as `Transaction Amount` + `Positive Tolerance %`. (Ref: `3.1.2 Limit Management`).

#### C. User Stories

### US-LIM-01: Facility Utilization Drill-down
**As a** Credit Officer,
**I want** a live, hyperlinked breakdown of every transaction currently consuming a facility,
**So that** I can identify the specific sources of credit exposure.

**Acceptance Criteria:**
- Dashboard includes a "Utilization Breakdown" table with Transaction Ref, Counterparty, and Base Amount.
- Reference numbers are clickable, navigating to the instrument timeline.

### US-LIM-02: FX Rate Stability for Earmarking
**As a** Trade Maker,
**I want** my transactions to use a stable exchange rate for limit earmarking,
**So that** I don't face rejection due to intra-day market fluctuations.

**Acceptance Criteria:**
- System uses the "Daily Board Rate" for all earmark calculations.

### US-LIM-03: Threshold Notifications (95%)
**As a** Credit Officer,
**I want** to be alerted when a facility's utilization exceeds 95%,
**So that** I can proactively review the customer's credit line.

**Acceptance Criteria:**
- System sends an automated group alert when `Utilized Amount` / `Total Limit` $\ge$ 0.95.

#### D. Grounding Info
*   **FX Methodology**: Based on **REQ-COM-FX-02** in baseline spec.
*   **Utilization Table**: Proved by **GAP-UI-05** in `2026-04-22-gap_analysis.md`.
*   **Earmarking Logic**: Derived from **3.1.2 Limit Management** in baseline spec.

#### E. Dropped Requirements
*   **Chart-Only Views**: Dropped in favor of the actionable table requirement from Gap Analysis **Section 2.1**.

---

## 3. Feature 3: Unified Transaction Lifecycle (Maker/Checker)

### 3.1 Final Requirements in Detail

#### A. Domain Entity Model (Data Dictionary)

**Trade Transaction (Workflow Header)**
| Field Name | Req/Opt | Data Type | Description |
| :--- | :--- | :--- | :--- |
| `Transaction ID` | Req | String | Unique system-generated identifier. |
| `Parent Instrument ID`| Req | String | Link to the legal instrument (e.g., LC ID). |
| `Type` | Req | Enum | Values: Issuance, Amendment, Presentation, Settlement. |
| `Priority` | Req | Enum | Values: `URGENT`, `HIGH`, `MEDIUM`, `LOW`. |
| `Status` | Req | Enum | Draft, Pending Approval, Executed, Cancelled. |
| `Maker` | Req | User | User who initiated the action. |
| `Checker` | Opt | User | User who authorized the action. |

#### B. Related Business Logic
*   **Four-Eyes Principle**: `Maker` cannot be the same as `Checker`.
*   **Transaction-Primary Authorization**: Checkers authorize based on the `Transaction ID`. Instrument is only updated after `Executed` (Ref: `FR-TT-01`).
*   **Dual-Status Header**: UI must display both **Transaction Status** and **Business State**.

#### C. User Stories

### US-TXN-01: Unified Action Initiation via TradeTransaction
**As a** Trade Maker,
**I want** all instrument modifications (Issuance, Amendment, etc.) to start as a `Trade Transaction`,
**So that** there is a consistent workflow for all trade actions.

**Acceptance Criteria:**
- Clicking "New LC" or "Amend" creates a `TradeTransaction` record.
- The instrument is not modified until the transaction is "Executed."

### US-TXN-02: Checker Dashboard / Queue Management
**As a** Trade Checker,
**I want** a centralized queue of all transactions pending my authorization,
**So that** I can manage my workload and prioritize urgent requests.

**Acceptance Criteria:**
- Dashboard shows all `PENDING_APPROVAL` transactions assigned to the user's tier.

### US-TXN-03: Dual-Status Visibility
**As a** Trade Operator,
**I want** to see both the transaction workflow status and the legal instrument status,
**So that** I understand the total state of the trade asset.

#### D. Grounding Info
*   **Transaction Model**: Implementation of **FR-TT-01** from `2026-04-28-trade-transaction-tracking-brd.md`.
*   **Workflow States**: Aligned with **Transaction Status Enum** in tracking spec.

#### E. Dropped Requirements
*   **Direct Instrument Editing**: Superseded by the tracking spec requirement for `TradeTransaction` containers.

---

## 4. Feature 4: Authority Tiers & Data Access

### 4.1 Final Requirements in Detail

#### A. Domain Entity Model (Authority Matrix)

| Tier Level | Max Approval Limit (Base CCY) | Description |
| :--- | :--- | :--- |
| `TIER_1` | 100,000 | Operational Officer |
| `TIER_2` | 500,000 | Manager / Unit Head |
| `TIER_3` | 2,000,000 | Department Head / AVP |
| `TIER_4` | Unlimited | Credit Committee / VP |

#### B. Related Business Logic
*   **Routing Rule**: Transactions routed to Checkers with Tier $\ge$ transaction value (Ref: `FR-UIA-02`).
*   **Dual-Checker Rule**: Transactions > 5,000,000 require **two** Tier 4 Checkers.
*   **Self-Approval Block**: Makers are strictly prohibited from authorizing their own transactions (Ref: `FR-UIA-04`).
*   **Branch Unit Lockdown**: Checkers only view/authorize within their own Organizational Unit.

#### C. User Stories

### US-UIA-01: Tier-Based Authorization
**As a** system administrator,
**I want** to assign approval tiers to users,
**So that** high-value transactions are only authorized by senior management.

### US-UIA-02: Multi-Checker Approval for Board Limits
**As a** Credit Committee member,
**I want** extremely high-value transactions to require two independent authorizations,
**So that** we maintain rigorous oversight on major exposures.

**Acceptance Criteria:**
- Transactions > 5,000,000 remain in "Pending" until a second Tier 4 user approves.

### US-UIA-03: Maker/Checker Segregation (Four-Eyes)
**As a** Compliance Officer,
**I want** to ensure the Maker of a transaction can never be its Checker,
**So that** we maintain strict internal controls.

### US-UIA-04: Branch-Level Data Access Isolation
**As a** Branch Manager,
**I want** my users to only view transactions belonging to their own branch,
**So that** we maintain data privacy and organizational focus.

### US-UIA-05: Self-Approval Block
**As a** Compliance Officer,
**I want** to strictly block users from approving their own work, even if they have senior authority,
**So that** we prevent internal fraud and errors.

#### D. Grounding Info
*   **Tier Matrix**: Imported from **FR-UIA-02** in `2026-05-03-user-identity-access-brd.md`.
*   **Security Guards**: Based on **FR-UIA-04** and **Section 3.2 Branch Assignment** of the Access spec.

---

## 5. Feature 5: Product Configuration (The Matrix)

### 5.1 Final Requirements in Detail

#### A. Domain Entity Model (Product Definitions)

| Attribute | Logic / Description |
| :--- | :--- |
| `Product Name` | e.g., Import LC Conventional, Standby LC. |
| `Is Revolving` | If Yes, limit is reinstated upon settlement. |
| `Max Tolerance %` | Hard ceiling for issuance (default 10%). |
| `Mandatory Margin %`| Minimum cash collateral required for this product. |
| `Accounting Template`| GL mapping for Issuance, Commission, and Settlement entries. |
| `SLA Days` | Standard examination window (default 5 days). |
| `Auto-Expiry days` | Number of days after Expiry Date to trigger the closure batch. |

#### B. Related Business Logic
*   **Dynamic UI Validation**: UI hides fields (e.g., "Usance Days") based on product type (Ref: `REQ-COM-UI-01`).

#### C. User Stories

### US-PRD-01: Product-Driven UI Behavior
**As a** Trade Maker,
**I want** the input screens to adjust based on the selected product,
**So that** I only see fields relevant to the specific trade instrument.

**Acceptance Criteria:**
- Fields like "Usance Days" are hidden if a "Sight Only" product is selected.

### US-PRD-02: Dynamic Tolerance Enforcement
**As a** Trade Operations system,
**I want** to enforce product-specific tolerance limits (e.g., max 10% for conventional LCs),
**So that** issuance complies with product-line policies.

#### D. Grounding Info
*   **Dynamic UI**: Based on **REQ-COM-UI-01** in baseline spec.
*   **Margin/GL Rules**: Grounded in **Section 4.1 Accounting Integration** of baseline spec.

---

## 6. Feature 6: Audit Logs & Narrative Timeline

### 6.1 Final Requirements in Detail

#### A. Domain Entity Model (Audit Data)

| Field Name | Requirement |
| :--- | :--- |
| `Timestamp` | Millisecond-level precision. |
| `Event Type` | DATA_CHANGE, AUTH_DECISION, SWIFT_IN/OUT. |
| `Entity Field` | Name of attribute modified. |
| `Old/New Value` | Before and after state capture. |
| `Justification` | Mandatory for rejections. |

#### B. Related Business Logic
*   **Visual Hierarchy**: Timeline must highlight Active/Pending transactions with distinct colors.
*   **In-Timeline Actionability**: Checkers must be able to "Authorize" or "Reject" directly from a timeline node.
*   **Snapshot Integrity**: Every timeline node must link to the specific data snapshot of the instrument.

#### C. User Stories

### US-UTN-01: Chronological Event Narrative
**As a** Trade Operator,
**I want** to see a unified timeline of all business and technical events for an instrument,
**So that** I can understand the complete history of the transaction in one view.

### US-UTN-02: In-Timeline Actionability
**As a** Trade Checker,
**I want** to authorize/reject directly from a timeline node,
**So that** I can act within the context of the instrument's history.

### US-UTN-03: Version Delta Analysis
**As a** Trade Checker,
**I want** to see exactly what changed in an amendment before I approve it,
**So that** I can focus on the risk impact of the modification.

#### D. Grounding Info
*   **Narrative Interleaving**: Implementation of **FR-TT-03** from tracking spec.
*   **Delta Analysis**: Addresses **US-UTN-03** in tracking spec.
