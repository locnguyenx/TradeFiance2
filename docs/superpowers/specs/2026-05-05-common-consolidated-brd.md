# Consolidated Business Requirements Document (BRD)
**ABOUTME:** 
Consolidated requirements for the Common Trade Finance Module.
This document merges baseline requirements with refactoring and transaction tracking specs.

**Project Name:** Digital Trade Finance Platform
**Module:** Common Trade Finance (Cross-Functional Core)
**Document Version:** 2.0 (All Features, Full Detail)
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
| `TP_ADVISE_THROUGH_BANK` | Advise Through Bank | Bank | Tag 57a |
| `TP_CONFIRMING_BANK` | Confirming / Requested Confirming | Bank | Tag 58a |
| `TP_REIMBURSING_BANK` | Reimbursing Bank | Bank | Tag 53a |
| `TP_NEGOTIATING_BANK` | Negotiating / Available With Bank | Bank | Tag 41a |
| `TP_DRAWEE_BANK` | Drawee Bank | Bank | Tag 42a |
| `TP_PRESENTING_BANK` | Presenting Bank | Bank | MT750/734/752 |
| `TP_INTERMEDIARY_BANK` | Intermediary Bank (Settlement) | Bank | Tag 56a (MT202) |
| `TP_SENDERS_CORRESPONDENT` | Sender's Correspondent | Bank | Tag 53a (MT202) |
| `TP_RECEIVERS_CORRESPONDENT` | Receiver's Correspondent | Bank | Tag 54a (MT202) |

#### C. Tag Coverage Verification Matrix
| MT Message | Tag | Party Role Used | Account Number Rule |
| :--- | :--- | :--- | :--- |
| **MT 700** | 50 | `TP_APPLICANT` | Optional |
| **MT 700** | 59 | `TP_BENEFICIARY` | Optional |
| **MT 700** | 51a | `TP_APPLICANT_BANK` | Forbidden |
| **MT 700** | 53a | `TP_REIMBURSING_BANK` | Optional |
| **MT 700** | 41a | `TP_NEGOTIATING_BANK` or "ANY BANK" | **Forbidden** |
| **MT 700** | 42a | `TP_DRAWEE_BANK` | **Forbidden** |
| **MT 700** | 57a | `TP_ADVISE_THROUGH_BANK` | Optional |
| **MT 700** | 58a | `TP_CONFIRMING_BANK` | Optional |
| **MT 707** | 50 | `TP_APPLICANT` | **Forbidden** (flat text only in MT707) |
| **MT 707** | 59 | `TP_BENEFICIARY` (New) | Optional |
| **MT 734** | 57a | `TP_PRESENTING_BANK` | Optional |
| **MT 740** | 59/59A | `TP_BENEFICIARY` | Optional |
| **MT 740** | 58a | `TP_NEGOTIATING_BANK` | Optional |
| **MT 750/734/752** | 20/21 (References) | `TP_PRESENTING_BANK` (via `presentingBankRef`) | N/A |
| **MT 202** | 52a | `TP_ISSUING_BANK` | Optional |
| **MT 202** | 53a | `TP_SENDERS_CORRESPONDENT` | Highly Recommended |
| **MT 202** | 54a | `TP_RECEIVERS_CORRESPONDENT` | Optional |
| **MT 202** | 56a | `TP_INTERMEDIARY_BANK` | Optional |
| **MT 202** | 58a | `TP_PRESENTING_BANK` | Optional |
| **MT 103** | 50a | `TP_APPLICANT` | Mandatory |
| **MT 103** | 59a | `TP_BENEFICIARY` | **Mandatory** (IBAN/account required) |
| **MT 103** | 56a | `TP_INTERMEDIARY_BANK` | Optional |
| **MT 103** | 57a | `TP_ADVISING_BANK` | Optional |

#### D. Related Business Logic
*   **Advising Eligibility**: The system blocks any bank without `Active RMA = Yes` from being assigned to the Advising Bank role (Ref: `FR-TP-12`).
*   **Advise Through Bank**: Does NOT require RMA with Issuing Bank — the Advising Bank handles RMA relay via MT 710 (Ref: `FR-TP-12`).
*   **Reimbursing Eligibility**: Assignment as a Reimbursing Bank requires a non-null `Nostro Reference` (Ref: `FR-TP-12`).
*   **Confirming Eligibility**: Confirmation is only permitted if the bank's `FI Limit Available` is greater than or equal to the transaction's maximum liability (Ref: `FR-TP-12`).
*   **KYC/Sanctions on All Bank Roles**: All bank role assignments require `kycStatus = Active` and `sanctionsStatus = SANCTION_CLEAR` (Ref: `FR-TP-12`).
*   **Party Type Match**: All role assignments require `partyTypeEnumId` to match the role's applicable type (Commercial vs Bank) (Ref: `FR-TP-12`).
*   **Compliance Block**: The system prevents submission to Checker if any assigned party has a `KYC Status` of "Expired" or a `Sanctions Status` of "Blocked" (Ref: `REQ-COM-ENT-02`).
*   **Negotiation Rule**: If `TP_NEGOTIATING_BANK` is assigned a specific bank, the account number is strictly forbidden in Tag 41a. If "Any Bank" is selected, Tag 41a must use code `ANY BANK` (Ref: `FR-TP-09`).
*   **Available With — Explicit User Choice (FR-TP-09)**: Tag 41a requires explicit user selection of `AVAIL_ANY_BANK` or `AVAIL_SPECIFIC_BANK` via `availableWithEnumId` on `ImportLetterOfCredit`. Maker selects "ANY BANK" → no junction record; selects specific bank → creates `TP_NEGOTIATING_BANK` junction. Account numbers are forbidden on Tag 41a even if the negotiating bank has one.
*   **Mandatory Roles for Submission**: `TP_APPLICANT` and `TP_BENEFICIARY` must be assigned before `submit#ForApproval` (Ref: `FR-TP-17`).
*   **SWIFT Option D Fallback**: If a BIC is not available, the system must utilize the `Legal Name` and `Registered Address` (SWIFT Option D) for banking tags.
*   **SWIFT Tag Format Selection**: Determined at runtime from party data (Option A/B/C/D) (Ref: `FR-TP-17`).
*   **Role Uniqueness Enforcement (FR-TP-18)**: Each instrument role (e.g., TP_APPLICANT) can only be assigned to a single party per instrument. The system must enforce this at the database level (Primary Key: `instrumentId`, `roleEnumId`) and prevent duplicate role assignments.

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

### US-TP-07: Structured Party Review for Checker
**As a** Trade Operations Checker,
**I want** to see all parties and their roles in a structured view when reviewing an LC,
**So that** I can verify the party assignments are correct before authorization.

**Acceptance Criteria:**
- Parties displayed grouped by category: Commercial Parties, Banking Parties.
- Each party shows: role label, party name, BIC (if bank), address, eligibility status.
- Multi-role assignments clearly indicated (e.g., "JP Morgan — Advising, Confirming, Negotiating").

### US-TP-08: Party Directory Tabbed Layout
**As a** system administrator,
**I want** the Party Directory detail view to use a tabbed interface,
**So that** party information is organized and navigable.

**Acceptance Criteria:**
- Tab 1: General Info, Tab 2: Roles, Tab 3: Compliance (Ref: `REQ-UI-CMN-03`).

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

**Trade Instrument (Base Transaction — REQ-COM-ENT-01)**
| Field Name | Req/Opt | Data Type | Description |
| :--- | :--- | :--- | :--- |
| `Transaction Reference` | Req | String | Unique system-generated human-readable ID (e.g., TF-IMP-2026-0001). |
| `Product Type` | Req | Enum | Product category (e.g., Import LC, Export Collection). |
| `Transaction Currency` | Req | id | Three-letter ISO currency code. |
| `Transaction Amount` | Req | Decimal | Total monetary value of the instrument. |
| `Base Equivalent Amount` | Req | Decimal | Value converted to bank's local operating currency for limit and reporting purposes. |
| `Issue Date` | Req | Date | Business date the transaction is formally initiated. |
| `Expiry/Maturity Date` | Req | Date | Date the instrument ceases to be valid or payment is due. |
| `Lifecycle Status` | Req | Enum | Draft, Pending Approval, Active, Hold, Closed. |

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
*   **Standard Processing Flow (REQ-COM-WF-01)**: Every trade finance transaction progresses through:
    1. **Initiation (Data Capture):** User inputs details → record in *Draft* state.
    2. **Pre-Processing Validations:** System checks KYC, Limit, Data Completeness. Failures prompt correction.
    3. **Authorization (Maker/Checker):** Submitted → *Pending Approval* → routed to authorized Checker.
    4. **Execution:** Checker approval → commits transaction, updates facility limits, generates SWIFT → *Active/Issued*.
    5. **Lifecycle Events:** Subsequent events logged (Amendments, Presentations, Tracers).
    6. **Settlement & Closure:** Funds moved, accounting entries generated, state → *Closed*.
*   **Decoupled Data Capture (REQ-TXN-02):** Frontend must distinguish between "proposed" data in an active transaction and the "current" state of the instrument.
*   **Four-Eyes Principle**: `Maker` cannot be the same as `Checker`.
*   **Transaction-Primary Authorization**: Checkers authorize based on the `Transaction ID`. Instrument is only updated after `Executed` (Ref: `FR-TT-01`).
*   **Dual-Status Header**: UI must display both **Transaction Status** and **Business State**.
*   **Historical Immutability (REQ-COM-VAL-02):** Once *Active/Issued*, core attributes cannot be changed or deleted. Modifications require a formal "Amendment" event creating a new version record while preserving the original.
*   **Date Sequence Logic (REQ-COM-VAL-03):** Expiry Date must always be strictly greater than or equal to the Issue Date.
*   **Back-Valuation Restriction (REQ-COM-VAL-03):** Users cannot set an Issue Date in the past without special administrative overrides.

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

**Delegated Authority Tiers (REQ-COM-AUTH-01)**
| Tier Level | Max Approval Limit (Base CCY) | Typical Business Role |
| :--- | :--- | :--- |
| `TIER_1` | 100,000 | Senior Trade Operations Officer |
| `TIER_2` | 1,000,000 | Trade Finance Team Lead |
| `TIER_3` | 5,000,000 | Head of Trade Operations |
| `TIER_4` | Above 5,000,000 (Joint Approval) | Credit Risk Committee / Board |

**User Profile (REQ-COM-MAS-02)**
| Field Name | Req/Opt | Data Type | Description |
| :--- | :--- | :--- | :--- |
| `User ID` | Req | String | Unique system identifier, linked to Active Directory. |
| `Functional Roles` | Req | Array | Specific modules/actions the user can access. |
| `Maker/Checker Flag` | Req | Enum | Values: Maker Only, Checker Only, Dual. |
| `Delegation Tier` | Cond | Enum | Values: Tier 1, Tier 2, Tier 3, Tier 4. |
| `Custom Limit` | Opt | Decimal | Specific override limit in the bank's base currency. |
| `Branch Access` | Req | Array | Restricts users to view/process transactions for specific branches. |

#### B. Related Business Logic
*   **Routing Rule**: Transactions routed to Checkers with Tier ≥ transaction value (Ref: `FR-UIA-02`).
*   **Dual-Checker Rule**: Transactions > 5,000,000 require **two** Tier 4 Checkers (Joint Approval, REQ-COM-AUTH-02).
*   **Self-Approval Block**: Makers are strictly prohibited from authorizing their own transactions (Ref: `FR-UIA-04`, REQ-COM-MAS-02).
*   **Branch Unit Lockdown**: Checkers only view/authorize within their own Organizational Unit.
*   **Downward Delegation Restriction (REQ-COM-AUTH-02)**: A Tier 1 user cannot approve a transaction requiring Tier 2 authority. Hard-stop with "Insufficient Authority" notice.
*   **Suspended Accounts (REQ-COM-MAS-02)**: If a user is on leave or suspended, the system must immediately remove them from the routing matrix so items do not get stuck in their queue.
*   **Admin Self-Disable Block**: An admin cannot disable their own account; the toggle is hidden or greyed out (Ref: `FR-ADM-10`).

**Special Authorization Scenarios (REQ-COM-AUTH-03)**
*   **A. Financial Amendments**: When an amendment increases the financial value of an existing instrument, the authorization tier is determined by the **new total liability**, not just the delta.
*   **B. Non-Financial Amendments**: Amendments that do not impact credit limit or transaction value default to a Tier 1 authorization requirement.
*   **C. Parallel Compliance Approvals**: Financial authority tiers are superseded by Compliance rules. If a transaction triggers a "Sanctions Hold" or "AML Anomaly" during validation, the standard Maker/Checker flow is suspended. It must be routed to a dedicated Compliance Officer role. The Compliance Officer must officially release the hold (with mandatory audit comments) before the standard operational Checker can finalize the financial approval.

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

### US-AUTH-01: Login & Session Management
**As a** platform user,
**I want** to log in with username and password and have my session persist across page refreshes,
**So that** I can securely access the platform.

**Acceptance Criteria:**
- All routes except `/login` require active Moqui session; unauthenticated → redirect (FR-AUTH-01).
- `POST /rest/s1/trade/login` establishes Moqui session cookie (FR-AUTH-02).
- `GET /rest/s1/trade/current-user` validates session on app mount; returns 401 if invalid (FR-AUTH-03).
- Failed login shows generic "Invalid username or password" — no information leakage (FR-AUTH-04).
- Account lockout: 3 failures → 5-minute disable (FR-AUTH-05).
- Logout invalidates server session, clears client state, redirects to `/login` (FR-AUTH-06).

### US-AUTH-02: User Self-Service Profile
**As a** platform user,
**I want** to view my profile and change my password,
**So that** I can maintain my account security.

**Acceptance Criteria:**
- Profile panel shows: full name, username, email, assigned roles, delegation tier (FR-PROF-01).
- Change password requires current + new password; calls `update#Password` (FR-PROF-02/03).
- Password policy: min 8 chars, 1 digit, 1 special char; errors displayed inline (FR-PROF-04).
- Logout button in profile panel (FR-PROF-05).

### US-AUTH-03: Admin User Management
**As a** system administrator,
**I want** a unified user administration page to create, edit, and manage user accounts,
**So that** I can control platform access.

**Acceptance Criteria:**
- Unified "User Administration" page at `/admin/users`; `TRADE_ADMIN` role required (FR-ADM-01).
- Left pane lists users (username, name, role badges) with "+ New User" button and search filter (FR-ADM-02/03).
- Create User: username (unique), name, email, initial password; calls `create#TradeUser` (FR-ADM-04).
- Edit User: name, email (username immutable); calls `update#TradeUser` (FR-ADM-05).
- Reset Password: admin sets new password without old password (FR-ADM-06).
- Role Assignment checkboxes: TRADE_MAKER, TRADE_CHECKER, TRADE_BACKOFFICE, TRADE_ADMIN (FR-ADM-07).
- Authority Tier Config: delegation tier, custom limit, currency (FR-ADM-08).
- Enable/Disable toggle; disabled accounts cannot log in (FR-ADM-09).
- Admin cannot disable own account (FR-ADM-10).

#### D. REST API Endpoints (FR-SVC)

| Endpoint | Method | Service | Access |
| :--- | :--- | :--- | :--- |
| `/current-user` | GET | `get#CurrentUser` | Authenticated |
| `/logout` | POST | wraps `ec.user.logoutUser()` | Authenticated |
| `/users` | GET | `get#UserList` | Admin only |
| `/users/{userId}` | GET | `get#UserDetail` | Admin only |
| `/users` | POST | `create#TradeUser` | Admin only |
| `/users/{userId}` | POST | `update#TradeUser` | Admin only |
| `/users/{userId}/reset-password` | POST | `reset#UserPassword` | Admin only |
| `/users/{userId}/roles` | POST | `update#UserRoles` | Admin only |
| `/users/{userId}/status` | POST | `update#UserStatus` | Admin only |
| `/change-password` | POST | `change#OwnPassword` | Authenticated |

#### E. Grounding Info
*   **Tier Matrix**: Imported from **FR-UIA-02** in `2026-05-03-user-identity-access-brd.md`.
*   **Security Guards**: Based on **FR-UIA-04** and **Section 3.2 Branch Assignment** of the Access spec.

---

## 5. Feature 5: Product Configuration (The Matrix)

### 5.1 Final Requirements in Detail

#### A. Domain Entity Model (Product Definitions — REQ-COM-PRD-01)

| Attribute | Data Type | Description & System Rules |
| :--- | :--- | :--- |
| `ProductID` | String | Unique Identifier (e.g., `IMP_LC_RED_CLAUSE`). |
| `Product Name` | String | Commercial name displayed to users. |
| `Is Active` | Boolean | Controls if the product is available for new issuances. |
| `Product Type` | Enum | `LC Import`, `LC Export`, `Collection Import`, `Collection Export`. |
| `Allowed Tenor` | Enum | Limits payment terms: `Sight Only`, `Usance Only`, or `Mixed`. |
| `Max Tolerance Limit` | Integer | Hard ceiling on the tolerance percentage (e.g., 10%). |
| `Allow Revolving` | Boolean | If True, enables UI fields for automatic reinstatements. |
| `Allow Advance Payment`| Boolean | Enables "Red/Green Clause" logic, allowing Beneficiary to draw funds before shipment. |
| `Is Standby (SBLC)` | Boolean | Flags the product as a Guarantee. Alters SLA and presentation rules. |
| `Is Transferable` | Boolean | Dictates if the LC can be reassigned to a second beneficiary. |
| `Accounting Framework`| Enum | Values: `Conventional` or `Islamic`. Dictates underlying GL posting rules (Interest vs. Profit Rate). |
| `Mandatory Cash Margin`| Integer | Specifies a minimum % of cash collateral required at issuance, bypassing standard facilities. |
| `Document Exam SLA Days`| Integer | Overrides the standard 5-day UCP 600 rule (e.g., SBLCs might require 1-day). |
| `Default SWIFT Format`| Enum | Forces the output message type (e.g., MT 700 for LC, MT 760 for Standbys). |
| `Auto-Expiry days` | Integer | Number of days after Expiry Date to trigger the closure batch. |

#### B. Related Business Logic (REQ-COM-PRD-02)
*   **Dynamic UI Rendering**: Adapts entry screen instantly (e.g., hiding "Usance Days" if `Sight Only`) (Ref: `REQ-COM-UI-01`).
*   **Pre-Processing Validations**: Forces exact checks (e.g., blocking Checker if `Mandatory Cash Margin` is not fully held).
*   **Message Generation**: Formats SWIFT dynamically (e.g., MT760 instead of MT700 for `Is Standby = True`).
*   **SLA Countdown Timer**: Uses the overridden `Document Exam SLA Days` for alerts and blocks.
*   **Advance Drawings**: Allows clean presentations against simple receipts if `Allow Advance Payment = True`, bypassing standard shipping document requirements.
*   **Accounting/Settlement**: Reinstates Revolving LCs automatically upon settlement if `Allow Revolving = True`. Posts to Islamic GLs if `Accounting Framework = Islamic`.

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

#### A. Domain Entity Model (Audit Data — REQ-COM-MAS-03)

| Field Name | Data Type | Description & System Rules |
| :--- | :--- | :--- |
| `Timestamp` | DateTime | Captured at exact millisecond of commit. System time, non-editable. |
| `User ID` | String | ID of the authenticated user performing the action. "SYSTEM" for automated agents. |
| `IP Address` | String | Network location from which the request originated. |
| `Transaction Ref` | String | The business reference number. |
| `Action Performed` | Enum | e.g., Submit for Approval, Authorize, Reject. |
| `Event Type` | Enum | DATA_CHANGE, AUTH_DECISION, SWIFT_IN/OUT. |
| `Entity Field` | String | Name of the specific data field modified. |
| `Old Value` | String | The data state before the action. |
| `New Value` | String | The data state after the action. |
| `Justification` | Text | Free-text reason (Mandatory for Rejections/Overrides). |

**Audit Rules:**
*   **Immutability (Append-Only)**: No user or admin should have permissions to delete/update a row in the audit log.
*   **Session Tracking**: If processed by an automated system agent, User ID must clearly reflect "SYSTEM".
*   **Report Generation**: System MUST support `Transaction History Report` (UI view) and `Compliance Extract` (cryptographically signed CSV/PDF export).

#### B. Related Business Logic
*   **Visual Hierarchy**: Timeline must highlight Active/Pending transactions with distinct colors (REQ-UTN-01.3).
*   **In-Timeline Actionability**: Checkers must be able to "Authorize" or "Reject" directly from a timeline node (REQ-UTN-02.1).
*   **Draft Resumption**: Users must be able to "Resume" draft transactions directly from the timeline (REQ-UTN-02.2).
*   **Rejection Feedback**: If a transaction was rejected, the "Rejection Reason" must be visible as a child-event under the rejected node (REQ-UTN-02.3).
*   **Snapshot Integrity**: Every timeline node must link to the specific data snapshot of the instrument (REQ-UTN-03.2).
*   **Technical Audit Events**: SWIFT message dispatch (MT700, MT707), Network ACKs, Email notifications, and Compliance screening results must be interleaved chronologically (REQ-UTN-01.2).
*   **Module Tagging**: Each event must be tagged with its origin (e.g., [ISSUANCE], [AMENDMENT], [SWIFT-OUT]) (REQ-UTN-04.1).
*   **Reference Integrity**: Events must be linked via `transactionId` to ensure that even if multiple amendments are pending, they are tracked independently (REQ-UTN-04.2).

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
*   **Audit Payload**: Imported from **REQ-COM-MAS-03** in baseline spec.
*   **Technical Events**: From **REQ-UTN-01.2** and **REQ-UTN-04** in tracking spec.

---

## 7. Feature 7: FX, SLA & Notifications

### 7.1 Final Requirements in Detail

#### A. Currency & FX Rules (REQ-COM-FX-01, REQ-COM-FX-02)
*   **Currency Precision (REQ-COM-FX-01)**: The system MUST respect ISO-standard decimal precision uniformly (e.g., USD = 2 decimals, JPY = 0 decimals) for all fields, limits, and settlement calculations.
*   **Currency Verification (REQ-COM-VAL-03)**: Transactions can only be processed using active, bank-approved ISO currencies.
*   **Dual-Rate FX Model (REQ-COM-FX-02)**:
    *   **Facility Blocking**: Use the "Daily Board Rate" (synchronized at EOD) for stable availability checks.
    *   **Physical Settlement**: Use the "Live Treasury Rate" at the moment of payment to eliminate market risk.
    *   **Earmarking Rule**: Earmarks are calculated as `Transaction Amount` + `Positive Tolerance %`.

#### B. Banking Calendar & SLA (REQ-COM-SLA-01, REQ-COM-SLA-02)
*   **Universal Banking Calendar (REQ-COM-SLA-01)**: All duration calculations (e.g., UCP 600 maximum 5-banking-day document checking window) MUST be evaluated against a Single Global Head-Office Calendar, neutralizing differences in international regional holidays. Weekends and global bank holidays are systematically skipped.
*   **Overdue Enforcement (REQ-COM-SLA-02)**: If a presentation remains unchecked after 3 banking days, a warning flag MUST be elevated. On the 5th day, the item MUST become a critical blocking exception.

#### C. Notifications (REQ-COM-NOT-01, REQ-COM-NOT-02)
*   **SLA and Threshold Alerts (REQ-COM-NOT-01)**: The system MUST generate automated email alerts routed to the respective group inbox when:
    *   An SLA timer breaches Day 3.
    *   An Applicant facility limit reaches 95% utilization.
*   **Compliance & Risk Holds (REQ-COM-NOT-02)**: If any transaction triggers a Sanctions block or KYC expiration during submission, the system MUST immediately send a prioritized alert to the designated Compliance Review queue/inbox.

#### D. User Stories

### US-FX-01: Currency Precision
**As a** Trade Operations system,
**I want** to enforce ISO-standard decimal precision for all currency fields,
**So that** calculations are accurate across all currencies.

### US-SLA-01: Banking Calendar Compliance
**As a** Trade Operations system,
**I want** to evaluate all duration calculations against a single global banking calendar,
**So that** UCP 600 deadlines are computed correctly regardless of local holidays.

### US-SLA-02: Overdue Presentation Enforcement
**As a** Trade Operations system,
**I want** to escalate unchecked presentations at 3 days and block at 5 banking days,
**So that** the bank avoids UCP 600 violations.

#### E. Grounding Info
*   **FX Methodology**: Based on **REQ-COM-FX-02** in baseline spec.
*   **SLA Calendar**: From **REQ-COM-SLA-01** in baseline spec.
*   **Overdue Rules**: From **REQ-COM-SLA-02** in baseline spec.
*   **Notifications**: From **REQ-COM-NOT-01/02** in baseline spec.

---

## 8. Feature 8: Fee & Charge Configuration

### 8.1 Final Requirements in Detail

#### A. Domain Entity Model (Tariff Matrix — REQ-COM-MAS-01)

| Field Name | Req/Opt | Data Type | Description |
| :--- | :--- | :--- | :--- |
| `Fee Event Trigger` | Req | Enum | e.g., LC Issuance, Amendment, Discrepancy, Payment. |
| `Calculation Type` | Req | Enum | Values: Flat Rate, Percentage, Tiered. |
| `Base Rate / Amount` | Req | Decimal | The standard price or percentage. |
| `Minimum Charge` | Opt | Decimal | Minimum floor for percentage fees. |
| `Maximum Charge` | Opt | Decimal | Maximum ceiling. |
| `Frequency / Period` | Cond | Enum | Values: One-Off, Per Month, Per Quarter. |
| `Effective Date` | Req | Date | Date the new pricing takes effect. Cannot backdate. |
| `Customer Tier` | Opt | String | Override for specific customer segments (e.g., VIP Corporate). |

#### B. Related Business Logic
*   **Fee Workflow**: Rule Definition (Maker) → Approval (Checker) → Execution (System Calculation).
*   **Exception Pricing**: System checks if the specific Applicant has a negotiated "Customer Exception Rate". If yes, it overrides the standard tariff.
*   **LC Issuance Fee Calculation**:
    *   `Time Units (Quarters)`: `(LC Expiry Date - Issue Date) / 90 days`, rounded up to the next whole quarter.
    *   `Calculated Base Fee`: `LC Amount × Base Rate % × Time Units`.
    *   `Final Applied Fee`: `Calculated Base Fee`, enforced against `Minimum Charge` and `Maximum Charge`.

#### C. User Stories

### US-FEE-01: Fee Rule Management
**As a** system administrator,
**I want** to define and approve fee rules with configurable calculation types and triggers,
**So that** the system can automatically calculate charges during transactions.

**Acceptance Criteria:**
- Fee rules require Maker/Checker approval before activation.
- Cannot backdate effective dates.

### US-FEE-02: Customer Exception Rates
**As a** Trade Operations system,
**I want** to apply negotiated customer-specific fee overrides,
**So that** VIP clients receive their agreed pricing.

#### D. Grounding Info
*   **Fee Configuration**: Imported from **REQ-COM-MAS-01** in baseline spec.

---

## 9. Feature 9: Navigation & Search

### 9.1 Final Requirements in Detail

#### A. Navigation Structure (REQ-NAV-01)
*   **Dashboard (REQ-NAV-01.1)**: KPI-driven summary of current exposure and queue health.
*   **Instrument Management (REQ-NAV-01.2)**: Vertical-specific lists (Import LCs, SGs, etc.) for browsing the current legal state of all trade assets.
*   **Global Transaction Log (REQ-NAV-01.3)**: A cross-instrument audit view of all Maker/Checker activity, sorted by priority and date.

#### B. Contextual Global Search (REQ-SRH-01)
*   **Context Toggling (REQ-SRH-01.1)**: Users must be able to toggle search results between "Instruments" (linking to the legal state) and "Transactions" (linking to the workflow/timeline).
*   **Cross-Reference Indexing (REQ-SRH-01.2)**: Searching by an `instrumentId` must also surface all associated `transactionId`s in the transaction context.

#### C. Checker Queue Interactivity (REQ-UI-CMN-02)
*   Clicking a row in the Checker Queue opens a **Full-Screen Overlay (Modal)** displaying the Checker Authorization Screen — not a new tab.

#### D. User Stories

### US-NAV-01: KPI Dashboard
**As a** Trade Operations user,
**I want** a dashboard showing current exposure and queue health,
**So that** I can quickly assess operational status.

### US-NAV-02: Global Transaction Log
**As a** Trade Manager,
**I want** a cross-instrument view of all Maker/Checker activity,
**So that** I can monitor workflow progress across all trade types.

### US-SRH-01: Contextual Search
**As a** Trade Operator,
**I want** to search for instruments or transactions with context toggling,
**So that** I can quickly find what I need regardless of whether I'm thinking about the legal instrument or the workflow action.

#### E. Grounding Info
*   **Navigation**: From **REQ-NAV-01** in tracking spec.
*   **Search**: From **REQ-SRH-01** in tracking spec.
*   **Checker Queue Modal**: From **REQ-UI-CMN-02** in gap analysis.
