# Behavior-Driven Development (BDD) Specification
**Project Name:** Digital Trade Finance Platform
**Module:** Common Module (Foundation, Maker/Checker, Facilities)
**Document Version:** 1.1 (Exhaustive Traceability)
**Date:** April 21, 2026

---

## Traceability Summary
*(Note: Every scenario below links to specific REQ-COM IDs natively. This ensures 1:1 coverage.)*

---

## Feature: 1. Core Master Data Entities
*(Coverage: REQ-COM-ENT-01, REQ-COM-ENT-02, REQ-COM-ENT-03)*

### Scenario: BDD-COM-ENT-01: Base Trade Instrument Instantiation
**Requirement ID:** REQ-COM-ENT-01
**Type:** Happy Path

* **Given** that the modular framework initializes a new trade feature (e.g. Documentary Collection)
* **When** the developer persists the new root transaction
* **Then** the core system enforces inheritance from the base `Trade Instrument` payload:
  | Core Inherited Fields | Enforced Reality |
  | Transaction Reference ID | Required Unique |
  | Global Branch Routing Code | Required |
  | Instrument Status Pointer | Maintained |

### Scenario: BDD-COM-ENT-02: Party KYC Screening Validation
**Requirement ID:** REQ-COM-ENT-02
**Type:** Edge Case

* **Given** a target Corporate Applicant exists within the shared `Trade Party` Directory
* **And** the KYC Date has lapsed into "Expired" state
* **When** an Import LC Maker attempts to link this Party to a new transaction
* **Then** the unified Entity controller rejects the reference:
  | Check Field | Evaluated Status | Action Taken |
  | Customer KYC Validity | False | Hard Block / Warning thrown |

### Scenario: BDD-COM-ENT-03: Multi-Currency Facility Limits
**Requirement ID:** REQ-COM-ENT-03
**Type:** Happy Path

* **Given** an Applicant is registered with a Global `Customer Facility` limit of 10M EUR
* **When** a JPY based transaction attempts to query the active facility head-room
* **Then** the shared Data Entity automatically applies the system currency conversion protocols before answering the balance query.

---

## Feature: 2. Shared Workflows & Dual Rate FX Integrations
*(Coverage: REQ-COM-WF-01, REQ-COM-FX-01, REQ-COM-FX-02)*

### Scenario: BDD-COM-FX-01: Unified Board Rate Lookup for Risk Earmarks
**Requirement ID:** REQ-COM-WF-01, REQ-COM-FX-01
**Type:** Happy Path

* **Given** a cross-currency limit consumption event fires during any Maker drafting workflow
* **When** the workflow demands an equivalent base value for the limit ledger
* **Then** the FX Integration strictly guarantees the query uses static predictable variables:
  | Parameter Sent | Hard-Coded Condition |
  | Rate Source Context | `DAILY_BOARD_RATE` |
  | Target Ledger | Facility Allocation Table |

### Scenario: BDD-COM-FX-02: Real-time Live Spread Lookup for Financial Exits
**Requirement ID:** REQ-COM-FX-02
**Type:** Happy Path

* **Given** a transaction state triggers a financial Settlement
* **When** the payment demands currency conversion (e.g. paying USD from a GBP account)
* **Then** the FX Integration completely ignores the static daily rate and fetches a live spread:
  | Parameter Sent | Hard-Coded Condition |
  | Rate Source Context | `LIVE_SPOT_MARKET_RATE` |
  | Target Ledger | Nostro/Vostro Ledger |

---

## Feature: 3. SLA Universal Calendar & Validations
*(Coverage: REQ-COM-SLA-01, REQ-COM-SLA-02, REQ-COM-NOT-01, REQ-COM-NOT-02, REQ-COM-VAL-01, REQ-COM-VAL-02, REQ-COM-VAL-03)*

### Scenario: BDD-COM-SLA-01: Head-Office UCP600 Date Calculations
**Requirement ID:** REQ-COM-SLA-01, REQ-COM-SLA-02, REQ-COM-NOT-01
**Type:** Happy Path

* **Given** an operation that allows "5 Banking Days" for execution (e.g. Document Examination)
* **When** the SLA Engine computes the deadline
* **Then** the system strictly relies on the Global Calendar regardless of regional login:
  | Constraint Condition | Evaluation |
  | Weekends | Skipped in calculation |
  | Local Branch Holidays | Ignored |
  | Head-Office Holidays | Skipped in calculation |

### Scenario: BDD-COM-SLA-02: Watchlist Freeze Override
**Requirement ID:** REQ-COM-NOT-02, REQ-COM-VAL-01, REQ-COM-VAL-02, REQ-COM-VAL-03
**Type:** Edge Case

* **Given** a pending transaction triggers an active `Compliance Risk Hold` due to an OFAC sanction flag
* **When** the backend SLA engine evaluates current metrics 
* **Then** the SLA timer is immediately suspended:
  | Evaluation Condition | Response Action |
  | Risk Hold Active? | True |
  | SLA Timer State | Paused / Frozen |

---

## Feature: 4. The Global Authorization Engine (Maker/Checker)
*(Coverage: REQ-COM-AUTH-01, REQ-COM-AUTH-02, REQ-COM-AUTH-03)*

### Scenario: BDD-COM-AUTH-01: Structural Independence (Segregation)
**Requirement ID:** REQ-COM-AUTH-02, REQ-COM-AUTH-03
**Type:** Edge Case

* **Given** an active Application Context for a specific Transaction UUID
* **When** the Application evaluates the active user credential attempting to hit `/authorize`
* **Then** the Authentication system runs a strict user comparison logic check:
  | Evaluated Payload | Validation Condition | Decision |
  | Current Logged User ID | == Maker User ID? | True |
  | Framework Response | Force Abort Request | 403 Forbidden |

### Scenario: BDD-COM-AUTH-02: Global Matrix Traversal based on Tier Maximum
**Requirement ID:** REQ-COM-AUTH-01
**Type:** Happy Path

* **Given** an incoming authorization request valued at 12M USD
* **When** the routing engine evaluates the Global Checker Queue visibility
* **Then** the system explicitly filters out users without adequate mapping:
  | Target User Tier Max | Visibility Outcome | Actionable Status |
  | Tier 1 (1M max) | Filtered / Hidden | False |
  | Tier 3 (20M max) | Target Recipient | True |

---

## Feature: 5. Tariff Engine & Product Attributes
*(Coverage: REQ-COM-MAS-01, REQ-COM-MAS-02, REQ-COM-MAS-03, REQ-COM-PRD-01, REQ-COM-PRD-02)*

### Scenario: BDD-COM-MAS-01: Auto-Calculation of Commission Percentages
**Requirement ID:** REQ-COM-MAS-01, REQ-COM-MAS-02, REQ-COM-PRD-01, REQ-COM-PRD-02
**Type:** Happy Path

* **Given** a fee request to calculate an "Issuance Commission"
* **When** the Tariff engine maps the active Customer Profile
* **Then** the Fee Calculator evaluates the hierarchy natively without hardcoded formulas:
  | Matrix Read Order | Metric Extracted |
  | Tier 1: Customer Special Override | Miss |
  | Tier 2: Product Geographic Default | Found (0.125%) |
* **And** computes 0.125% against the Base Equivalent Amount, routing output to the Ledger Module.

### Scenario: BDD-COM-MAS-02: Event Sourcing Immutable Audit Payload
**Requirement ID:** REQ-COM-MAS-03
**Type:** Happy Path

* **Given** an end-user applies a state-altering modification to any modular entity
* **When** the Transaction Save instruction completes 
* **Then** the shared Common platform automatically persists a non-repudiation delta payload containing:
  | Required JSON Audit Properties |
  | Timestamp (UTC) |
  | User Principal Target |
  | Before State Payload Snapshot |
  | After State Payload Snapshot |
