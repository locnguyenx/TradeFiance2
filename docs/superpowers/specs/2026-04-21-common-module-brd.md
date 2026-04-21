# Business Requirements Document (BRD)

**Project Name:** Digital Trade Finance Platform
**Module:** Common Trade Finance (Cross-Functional Core)
**Document Version:** 2.1 (Full Detail Integration)
**Date:** April 21, 2026

---

## 1. Document Control & Scope
The Common Module serves as the foundational layer for all Trade Finance (TF) transactions. Rather than duplicating logic across Import LCs, Export LCs, and Collections, this module centralizes the core business entities, validation rules, user authority workflows, fee engines, and product matrix that apply universally to every trade instrument.

---

## 2. Core Master Data Entities
The following core domain entities represent data structures that MUST be supported universally.

### REQ-COM-ENT-01: Trade Instrument (Base Transaction)
All transaction records MUST inherit the following properties:
- `Transaction Reference Number`: Unique, system-generated identifier (e.g., TF-IMP-2026-0001).
- `Product Type`: Specific product category mapping to the Product Matrix.
- `Transaction Currency`: 3-letter ISO code.
- `Transaction Amount`: Total monetary value in the transaction currency.
- `Base Equivalent Amount`: Transaction value converted to local operating currency for limit tracking.
- `Issue Date`: Business date the transaction is initiated.
- `Expiry/Maturity Date`: Date the instrument ceases to be valid or payment is due.
- `Lifecycle Status`: Current business state (Draft, Pending Approval, Active, Hold, Closed).

### REQ-COM-ENT-02: Trade Party Directory
All involved parties (Applicants, Beneficiaries, Issuing Banks) MUST exist and be validated against this directory.
- `Party ID`: Unique identifier for the customer or bank.
- `Legal Name & Registered Address`: Used for SWIFT messaging.
- `Role in Transaction`: Applicant, Beneficiary, etc.
- `KYC Status`: Active, Expired, Pending.
- `Sanctions Status`: Clear, Suspended, Blocked.
- `Country of Risk`: Primary jurisdiction for country-limit exposures.

### REQ-COM-ENT-03: Customer Facility (Credit Limits)
- `Facility ID`: Identifier for the approved credit line.
- `Total Approved Limit`: Maximum risk exposure allowed.
- `Utilized Amount`: Value currently locked by active TF transactions.
- `Available Earmark`: Remaining balance available.
- `Facility Expiry Date`: Date the credit line must be renewed.

---

## 3. Standard Processing Workflow

### REQ-COM-WF-01: Universal States
All transactions MUST progress through this operational flow:
1. **Initiation (Data Capture):** Entry by operations or via portal. Status = *Draft*.
2. **Pre-Processing Validations:** Core check of KYC, Limits, Completeness. 
3. **Authorization:** Submitted by Maker. Status = *Pending Approval*. Routed to Checker.
4. **Execution:** Checker approves. Limits updated. Status = *Active / Issued*.
5. **Lifecycle Events:** Subsequent amendments or presentations.
6. **Settlement & Closure:** Funds move, accounting entries pass. Status = *Closed*.

---

## 4. Currency, FX & Accounting

### REQ-COM-FX-01: Currency Precision
The system MUST respect ISO-standard decimal precision uniformly globally (e.g., USD = 2 decimals, JPY = 0 decimals).

### REQ-COM-FX-02: Dual-Rate Framework
To resolve the conflict between predictable limits and market risk, a dual integration is required:
- **Facility Blocking (Static Board Rate):** Facility Limit (`Base Equivalent Amount`) consumption MUST be calculated using a static "Daily Board Rate" updated strictly once daily. This prevents intraday Maker/Checker approvals from failing purely due to live FX volatility.
- **Settlement (Live Treasury Rate):** Actual accounting transfers (MT202/MT103) where money moves MUST fetch and lock a Live FX rate from the Treasury API to prevent spot FX reporting risk.

---

## 5. Maker / Checker Authorization Matrix

### REQ-COM-AUTH-01: Segregation of Duties
The system MUST definitively prevent the same individual from acting as both Maker and Checker on a single lifecycle event. The system MUST hide the authorization capabilities from the user who originally submitted the draft.

### REQ-COM-AUTH-02: Authority Tiers & Limits
Pending Approval transactions MUST map to the appropriate Checker tier based on the `Base Equivalent Amount`:
- **Tier 1:** Up to $100,000 USD (Routine items).
- **Tier 2:** Up to $1,000,000 USD (Standard commercial).
- **Tier 3:** Up to $5,000,000 USD (High-value).
- **Tier 4:** Above $5,000,000 USD (Exceptional value).

### REQ-COM-AUTH-03: Joint Approval & Delegation
- **Joint Approval:** Tier 4 limits MUST require routing to *two distinct* Tier 4 Checkers for joint approval.
- **Downward Delegation Block:** A Tier 1 Checker MUST NOT be permitted to authorize a Tier 2+ transaction. System must display "Insufficient Authority".

### REQ-COM-AUTH-04: Amendment Authorization Behavior
- **Financial Amendments:** If an amendment logically increases liability (amount up, expiry extended), the Tier required MUST be selected based on the *new total liability amount*, not just the amended delta.
- **Non-Financial Amendments:** Text changes, condition additions, or port updates default safely to a Tier 1 requirement regardless of total transaction value.

### REQ-COM-AUTH-05: User Authority Profile Structure
The system MUST support an identity structure including:
- `User ID` & `Branch Access` (restrict views to specific physical branches).
- `Functional Roles` (e.g., Create LC, Discrepancy Edit).
- `Maker/Checker Flag` (Maker Only, Checker Only, Dual).
- `Delegation Tier` (1 through 4) and `Custom Limit` (specific numeric override).

---

## 6. Risk, Compliance & Governance

### REQ-COM-RSK-01: KYC Validation
Transactions MUST NOT proceed beyond Draft if the primary Applicant's KYC status is `Expired`.

### REQ-COM-RSK-02: Sanctions Hold (Pre-processing)
If any Trade Party, Vessel, or Port matches a restricted entity on a global watch list during submission, the system MUST immediately shift the transaction to `Compliance Hold`. It cannot be authorized by standard operations and MUST route to a designated Compliance Officer.

### REQ-COM-RSK-03: System Date Logic
- **Date Sequence:** Expiry Date MUST be $\ge$ Issue Date.
- **Back-Valuation:** The system MUST strictly restrict users from setting an `Issue Date` in the past.

### REQ-COM-RSK-04: Historical Immutability 
Once a transaction is Active/Issued, core domain attributes CANNOT be silently changed, backdated, or deleted. Any modification MUST be logged as a formal "Amendment", creating a persistent version history.

---

## 7. Audit Logging & Non-Repudiation

### REQ-COM-AUD-01: The Audit Payload
Every state mutation MUST systematically write an immutable record containing:
1. `Timestamp`: System millisecond.
2. `User ID`: Authenticated user or SYSTEM.
3. `IP Address`: Originating network location.
4. `Transaction Ref`: Associated instrument.
5. `Action Performed`: e.g., Submit, Authorize, Reject.
6. `Field Changed`: Data updated.
7. `Old Value` & `New Value`.
8. `Justification`: Free text (mandatory for overrides/rejections).

### REQ-COM-AUD-02: Immutability Constraint
The audit log MUST be strictly append-only at the database layer. Application-level deletes or updates are absolutely forbidden.

---

## 8. Calendars, SLAs & Notifications

### REQ-COM-SLA-01: Global Banking Calendar
To resolve timezone and regional discrepancies, all formal UCP 600 timers (e.g., the 5-day examination rule) MUST be exclusively calculated against a Single Global Head-Office Holiday Calendar (weekends and global holidays are skipped).

### REQ-COM-SLA-02: Document Presentation Timers
Upon logging Document Lodgement:
- **Day 3 Warning:** System generates an SLA Warning in the Checker Dashboard.
- **Day 5 Escalation:** System elevates the presentation to a critical blocker exception.

### REQ-COM-NOT-01: Event Notifications
The system MUST generate automated email alerts via an integration service when:
- SLA timers breach Day 3.
- Combined Applicant facility utilization reaches $\ge$ 95%.
- A transaction enters `Compliance Hold` (alerting the Risk/Compliance group).

---

## 9. Fee & Tariff Configuration Engine

### REQ-COM-FEE-01: Dynamic Tariff Matrix
Administrators MUST be able to define fee logic mapping without development code changes. The configuration MUST support:
- `Fee Event Trigger`: Issuance, Amendment, Payment.
- `Calculation Type`: Flat Rate, Percentage, Tiered.
- `Base Rate / Amount` & `Min/Max Constraints`.
- `Time Period`: One-off, Per Month, Per Quarter.
- `Customer Tier Override`: Apply exceptions for VIP clients.

### REQ-COM-FEE-02: Workflow Approvals for Pricing
Any creation or modification to the Tariff Matrix MUST traverse a Maker/Checker `Pending Approval` flow. Pricing updates carry significant risk and cannot be applied unilaterally by an admin.

---

## 10. Product Configuration Matrix (The Catalog)
Instead of rigidly hardcoding business logic into code, specific instruments (e.g., Islamic LC, Standby LC, Red Clause, Sight LC) MUST be driven by a Parameterized Catalog managed by business admins.

### REQ-COM-PRD-01: Product Matrix Fields
The internal `TradeProduct` mapping entity MUST support the following toggles/rules:
- `ProductID` & `Product Name`.
- `Allowed Tenor`: Sight Only, Usance Only, Mixed.
- `Max Tolerance Limit`: Hard ceiling % allowed during entry.
- `Allow Revolving`: Enables automated reinstatement schedules.
- `Allow Advance Payment`: Bypasses strict presentation rules for Red/Green clause LCs.
- `Is Standby (SBLC)`: Alters SLA constraints and UCP presentation logic (defaults output to MT760).
- `Accounting Framework`: Defaults to Conventional (Interest) or Islamic Trade (Profit Rate GL routing).
- `Mandatory Cash Margin`: Forces 100% cash deposit blocking ignoring standard limit facilities.

### REQ-COM-PRD-02: Cross-Functional Impact
When a Maker selects a `ProductID` during Application Capture, the system MUST dynamically override default behavior (e.g., hiding Usance inputs for Sight LCs, forcing 100% cash margins before authorization, mapping the correct Swift message format).

---
*End of Document*
