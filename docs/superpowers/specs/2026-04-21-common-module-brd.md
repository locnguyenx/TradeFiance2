# Business Requirements Document (BRD)

**Project Name:** Digital Trade Finance Platform
**Module:** Common Trade Finance (Cross-Functional Core)
**Document Version:** 2.0 (Formalization)
**Date:** April 21, 2026

---

## 1. Document Control & Scope
The Common Module serves as the foundational layer for all Trade Finance (TF) transactions. Rather than duplicating logic across Import LCs, Export LCs, and Collections, this module centralizes the core business entities, validation rules, currency rules, calendars, and overarching system governance that applies universally to every trade instrument.

---

## 2. Core Master Data Entities
The following core domain entities are shared across all specific Trade modules.

*   **REQ-COM-ENT-01: Trade Instrument (Base)**
    All transaction records MUST inherit the following properties:
    *   `Transaction Reference Number`: Unique alphanumeric identifier (e.g., TF-IMP-2026-0001).
    *   `Base Equivalent Amount`: The live value of the transaction converted to the bank's local operating currency, used exclusively for evaluating credit risk exposure.

*   **REQ-COM-ENT-02: Trade Party Directory**
    All transaction parties MUST be vetted against a unified `TradeParty` directory.
    *   `KYC Status`: Active, Expired, Pending.
    *   `Sanctions Status`: Clear, Suspended, Blocked.

*   **REQ-COM-ENT-03: Customer Facility (Credit Limits)**
    The system MUST strictly link an Applicant to an approved `CustomerFacility` and track its `Available Earmark`. Any transaction action MUST be strictly blocked if the `Base Equivalent Amount` breaches the available facility limit.

---

## 3. Currency & FX Rules

*   **REQ-COM-FX-01: Currency Precision Constraints**
    The system MUST enforce strict ISO-standard decimal precision for all amounts globally across UI entry, calculation, and database commitment (e.g., USD = 2 decimals, JPY = 0 decimals).

*   **REQ-COM-FX-02: Dual-Rate FX Framework**
    The system MUST implement a dual-rate integration model to balance execution speed and financial risk:
    *   **Facility Blocking (Static Rate):** For all limit availability and credit calculations, the system MUST utilize a "Daily Board Rate" synchronized once at End-Of-Day. This stabilizes checking workflows by ensuring pending requests don't randomly fail due to intraday FX swings.
    *   **Physical Settlement (Live Rate):** For actual accounting settlement (MT202/103 remittance), the system MUST require an active Live FX Treasury API call to guarantee the bank bears zero live market exposure on the cash movement.

---

## 4. SLA Timers & Global Calendar

*   **REQ-COM-SLA-01: Single Global Banking Calendar**
    The system MUST use a Single Global Head-Office Calendar for standardizing all internal timeline calculations. When executing timebound checks (such as the UCP 600 maximum 5-banking-day window for document presentations), weekends and global designated bank holidays are systematically skipped based solely on this centralized calendar.

*   **REQ-COM-SLA-02: UCP Timer Enforcement**
    The system MUST track the time elapsed since a document was registered as "Received". 
    *   **Day 3:** System generates a warning.
    *   **Day 5:** System escalates to a hard-limit exception if examination is not complete.

---

## 5. Required Notifications & Escalations

*   **REQ-COM-NOT-01: SLA and Threshold Mail Alerts**
    The system MUST trigger automated email alerts to specific operational group inboxes when:
    *   An SLA timer (as defined in REQ-COM-SLA-02) breaches Day 3.
    *   An Applicant's aggregate facility utilization breaches 95%.

*   **REQ-COM-NOT-02: Compliance Holds**
    If the system's Pre-Processing rules detect an expired KYC or flag a new party/vessel via Sanctions integration, the system MUST immediately halt process flows and send a priority alert to the `Compliance Review` inbox/queue.

---

## 6. Maker / Checker Authorization Matrix

*   **REQ-COM-AUTH-01: Segregation of Duties (Four-Eyes Principle)**
    Under no circumstances may a User who initiated or modified a transaction (Maker) authorize that same transaction (Checker). The system MUST hide the `[Authorize]` button and forcefully block the action at the identity level.

*   **REQ-COM-AUTH-02: Authority Tier Routing logic**
    Transactions awaiting approval MUST dynamically route to a specific tier queue based entirely on the transaction's `Base Equivalent Amount`:
    *   **Tier 1 (Up to $100K USD):** Senior Operations Officer.
    *   **Tier 2 (Up to $1M USD):** Trade Team Lead.
    *   **Tier 3 (Up to $5M USD):** Head of Trade Operations.
    *   **Tier 4 (Above $5M USD):** Requires Joint Approval (The authorization of two independent Tier 4 Checkers).

---

## 7. Risk & Compliance Framework

*   **REQ-COM-RSK-01: KYC Pre-Processing Blocks**
    The core TF engine MUST hard-stop transition to any active or issued commercial state if the Applicant's KYC flag is `Expired`.

*   **REQ-COM-RSK-02: Sanctions Halt**
    Any transaction component (address, vessel, bank SWIFT BIC) matching a sanctioned entity MUST intercept the Maker/Checker workflow, creating a `Compliance Hold` state that only a designated Compliance Officer can release.

---
*End of Document*
