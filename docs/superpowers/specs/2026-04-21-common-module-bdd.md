# Behavior-Driven Development (BDD) Specification
**Project Name:** Digital Trade Finance Platform
**Module:** Common Module (Foundation, Maker/Checker, Facilities)
**Document Version:** 1.0 (Initial BDD Derivation)
**Date:** April 21, 2026

---

## 1. Traceability Matrix

| Scenario ID | Title | Requirement Map | Type |
|---|---|---|---|
| BDD-CMN-MC-01 | Segregation of Duties - Maker cannot Check | REQ-CMN-AUTH-01 (implied Maker/Checker Segregation) | Edge Case |
| BDD-CMN-MC-02 | Tiered Authority Enforcement - Under Limit | REQ-CMN-AUTH-02 (Tiered Limit) | Happy Path |
| BDD-CMN-MC-03 | Tiered Authority Enforcement - Over Limit | REQ-CMN-AUTH-02 (Tiered Limit) | Edge Case |
| BDD-CMN-FX-01 | Facility Limit Block via Board Rate | REQ-CMN-FX-01 | Happy Path |
| BDD-CMN-FX-02 | Settlement via Live FX Rate | REQ-CMN-FX-02 | Happy Path |

---

## 2. Detailed BDD Scenarios

### Feature: Maker/Checker Framework & Authorization Segregation
*(Note: These scenarios apply globally across all Trade Modules referencing the Common library)*

#### Scenario BDD-CMN-MC-01: Segregation of Duties - Maker cannot Check
**Type:** Edge Case
**Purpose:** Ensure strict four-eyes principle where the individual who drafted a document cannot authorize it.

* **Given** User A submits an Import LC application, acting as the Maker
* **And** User A also holds the role of Checker with a sufficient Tier Limit
* **When** User A accesses the Global Checker Queue
* **Then** the submitted application does not appear in User A's queue
* **And** if User A attempts to access the transaction via direct URL, the "AUTHORIZE" button is disabled
* **And** the system flags an error: "Authorization Error: Maker cannot authorize their own transaction"

#### Scenario BDD-CMN-MC-02: Tiered Authority Enforcement - Under Limit
**Type:** Happy Path
**Purpose:** Ensure a Checker with high enough privileges can authorize.

* **Given** a transaction is waiting in `Pending Approval` state with a Base Equivalent Amount of $5,000,000 USD
* **And** User B is logged in as a Tier 3 Checker (Global limit: $10,000,000 USD)
* **When** User B opens the Authorization screen
* **Then** the system visually confirms their Tier mapping 
* **And** allows the user to click the "AUTHORIZE" button successfully

#### Scenario BDD-CMN-MC-03: Tiered Authority Enforcement - Over Limit
**Type:** Edge Case
**Purpose:** Prevent Checkers from approving transactions above their tier threshold.

* **Given** a transaction is waiting in `Pending Approval` state with a Base Equivalent Amount of $5,000,000 USD
* **And** User C is logged in as a Tier 1 Checker (Global limit: $1,000,000 USD)
* **When** User C navigates to the Global Checker Queue
* **Then** the transaction may be visible in read-only mode but not actionable
* **And** if User C accesses the specific Authorization screen, the "AUTHORIZE" button is disabled
* **And** the UI displays an alert: "Insufficient Authority. Requires Tier 2 (or higher) to authorize."

---

### Feature: FX Rate Conversions

#### Scenario BDD-CMN-FX-01: Facility Limit Block via Board Rate
**Type:** Happy Path
**Purpose:** Risk controls require static daily Board Rates to calculate limit consumption predictability.

* **Given** an Import LC is drafted in EUR currency
* **And** the system identifies that the Applicant's Facility Limit is maintained in USD
* **When** the `Base Equivalent Amount` calculation is triggered during Draft Submission
* **Then** the system fetches the `Daily Static Board Rate` for EUR/USD for the current Business Date
* **And** calculates the earmark block amount using this Board Rate, logging the rate ID in the transaction payload

#### Scenario BDD-CMN-FX-02: Settlement via Live FX Rate
**Type:** Happy Path
**Purpose:** Financial settlement requires real-time FX tracking.

* **Given** a Sight Import LC in EUR currency is being settled against a USD customer account
* **When** the operations team triggers the Settlement action
* **Then** the system queries the FX Gateway for a `Live FX Rate` for EUR/USD
* **And** applies the Live Rate to the financial ledger debit request
* **And** recalculates and releases the Contingent Liability limit offset using the original `Board Rate` that it was booked at to ensure the facility balances accurately
