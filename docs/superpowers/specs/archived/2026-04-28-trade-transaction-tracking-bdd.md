# Behavior-Driven Development (BDD) Specification
**Project Name:** Digital Trade Finance Platform
**Module:** Common / Unified Traceability
**Topic:** Trade Transaction & Instrument Narrative
**Document Version:** 1.0
**Date:** April 28, 2026

---

## 1. Traceability Summary

| Requirement Map (BRD ID) | Scenario ID | Title | Type |
|---|---|---|---|
| REQ-TXN-01 | BDD-TXN-INI-01 | Transaction-Primary: New LC creates Issuance Transaction | Happy Path |
| REQ-TXN-01 | BDD-TXN-AUTH-01 | Transaction-Primary: Checker Authorizes by transactionId | Happy Path |
| REQ-TXN-02 | BDD-TXN-SNAP-01 | Decoupled Data: View Pending Amendment Proposed Snapshot | Happy Path |
| REQ-NAV-01 | BDD-NAV-LOG-01 | Global Audit: View Cross-Instrument Transaction Log | Happy Path |
| REQ-SRH-01 | BDD-SRH-TGL-01 | Contextual Search: Toggle between Asset and Workflow views | Happy Path |
| REQ-UTN-01 | BDD-UTN-TIM-01 | Full Narrative Rendering: Financial & System Events | Happy Path |
| REQ-UTN-02 | BDD-UTN-ACT-01 | Timeline Workflow: Checker Authorizes Pending Node | Happy Path |
| REQ-UTN-02 | BDD-UTN-ACT-02 | Timeline Workflow: Rejection with Reason Display | Edge Case |
| REQ-UTN-03 | BDD-UTN-DLT-01 | Delta Analysis: Visualizing Amendment Differences | Happy Path |
| REQ-UTN-05 | BDD-UTN-STA-01 | Dual-Status Clarity: Workflow vs Lifecycle States | Happy Path |

---

## 2. Detailed BDD Scenarios

### Feature: Transaction-Primary Lifecycle (REQ-TXN-01)

#### Scenario BDD-TXN-INI-01: Transaction-Primary: New LC creates Issuance Transaction
**Requirement ID:** REQ-TXN-01.1
**Type:** Happy Path

* **Given** a user is completing the "New Import LC" form
* **When** they click "Submit for Approval"
* **Then** the frontend makes a POST request to `/rest/s1/trade/transactions`
* **And** the request payload includes:
  | Field | Value |
  | `transactionTypeEnumId` | `TXNT_ISSUANCE` |
  | `instrumentTypeEnumId` | `INST_IMPORT_LC` |
  | `priorityEnumId` | (User selected priority) |
* **And** the backend returns a `transactionId` which is then used to redirect the user to the "Issuance Confirmation" page.

#### Scenario BDD-TXN-AUTH-01: Transaction-Primary: Checker Authorizes by transactionId
**Requirement ID:** REQ-TXN-01.2
**Type:** Happy Path

* **Given** a Checker is reviewing an approval item with `transactionId = TXN-001`
* **When** the Checker clicks "Authorize"
* **Then** the frontend calls `tradeApi.authorize(transactionId: "TXN-001")`
* **And** the call hits the backend service `authorize#TradeTransaction`
* **And** the `TradeInstrument` state is updated by the backend as a cross-entity side effect of the transaction authorization.

### Feature: Decoupled Data Capture (REQ-TXN-02)

#### Scenario BDD-TXN-SNAP-01: Decoupled Data: View Pending Amendment Proposed Snapshot
**Requirement ID:** REQ-TXN-02.1
**Type:** Happy Path

* **Given** an LC exists with `amount = 500,000`
* **And** a pending Amendment transaction (v.2) exists with `amount = 550,000`
* **When** a Checker views the transaction details for the Amendment
* **Then** the UI displays the "Proposed Snapshot":
  | Display Field | Value | Style |
  | `Current Amount` | `500,000.00` | Standard |
  | `Proposed Amount`| `550,000.00` | Highlighted (Blue) |
* **And** the navigation header displays "Instrument: Issued | Transaction: Pending Approval".

### Feature: Enhanced Navigation (REQ-NAV-01)

#### Scenario BDD-NAV-LOG-01: Global Audit: View Cross-Instrument Transaction Log
**Requirement ID:** REQ-NAV-01.3
**Type:** Happy Path

* **Given** three different instruments (LC-1, LC-2, SG-1) have had recent activity
* **When** a user navigates to the "Global Transaction Log"
* **Then** they see a table containing:
  | Row | Transaction ID | Instrument Ref | User | Status |
  | 1 | TXN-999 | LC-1 | Maker-A | `Pending` |
  | 2 | TXN-998 | SG-1 | Checker-B| `Executed` |
  | 3 | TXN-997 | LC-2 | Maker-A | `Draft` |

### Feature: Contextual Global Search (REQ-SRH-01)

#### Scenario BDD-SRH-TGL-01: Contextual Search: Toggle between Asset and Workflow views
**Requirement ID:** REQ-SRH-01.1
**Type:** Happy Path

* **Given** an LC exists with reference `TF-IMP-26-0001`
* **And** it has an active amendment `TXN-5561`
* **When** a user searches for `TF-IMP-26` in the "Instruments" context
* **Then** the search results show the `TF-IMP-26-0001` asset record.
* **When** the user clicks "Switch to Transactions"
* **Then** the search results show `TXN-5561` (Amendment).

### Feature: Unified Narrative Timeline (REQ-UTN-01)

#### Scenario BDD-UTN-TIM-01: Full Narrative Rendering: Financial & System Events
**Requirement ID:** REQ-UTN-01
**Type:** Happy Path

* **Given** a Trade Instrument (Import LC) has the following history:
  1. Issuance Transaction (Authorized)
  2. SWIFT MT700 (ACKed by Network)
  3. Amendment Transaction (Pending Approval)
* **When** a user views the "Unified Narrative" page for this instrument
* **Then** the timeline renders 4 nodes in reverse chronological order:
  | Node Type | Source | Status |
  | `Transaction` | Amendment | `Pending Approval` (Highlighted) |
  | `Audit` | SWIFT MT700 | `ACKed` |
  | `System` | Issuance Advice | `Sent` |
  | `Transaction` | Issuance | `Authorized` |

---

### Feature: In-Timeline Actionability (REQ-UTN-02)

#### Scenario BDD-UTN-ACT-01: Timeline Workflow: Checker Authorizes Pending Node
**Requirement ID:** REQ-UTN-02.1
**Type:** Happy Path

* **Given** an Amendment transaction exists in `Pending Approval`
* **And** the current user has the `Checker` role with a sufficient financial tier
* **When** the Checker clicks the "Authorize" button directly on the timeline node
* **Then** the `authorize#TradeTransaction` service is invoked with the `transactionId`
* **And** the timeline node updates its status to `Authorized` (Green)
* **And** a new `System` event for "SWIFT MT707 Generation" appears above it

#### Scenario BDD-UTN-ACT-02: Timeline Workflow: Rejection with Reason Display
**Requirement ID:** REQ-UTN-02.3
**Type:** Edge Case

* **Given** an Amendment transaction in `Pending Approval`
* **When** a Checker clicks "Reject" on the timeline node and enters "Invalid Expiry Date"
* **Then** the transaction state shifts to `Draft` (or `Rejected`)
* **And** the timeline node displays a sub-item: "Rejection Reason: Invalid Expiry Date"
* **And** an "Edit" button becomes available on the node for the Maker

---

### Feature: Delta & Version Analysis (REQ-UTN-03)

#### Scenario BDD-UTN-DLT-01: Delta Analysis: Visualizing Amendment Differences
**Requirement ID:** REQ-UTN-03.1
**Type:** Happy Path

* **Given** an Amendment (v.2) exists for an LC where the `amount` was increased from `500,000` to `550,000`
* **When** a user clicks "View Diff" on the Amendment node in the timeline
* **Then** a side-panel or modal appears showing:
  | Field | Old Value | New Value | Change |
  | `LC Amount` | `500,000.00` | `550,000.00` | `+ 50,000.00` |

---

### Feature: Status Convergence (REQ-UTN-05)

#### Scenario BDD-UTN-STA-01: Dual-Status Clarity: Workflow vs Lifecycle States
**Requirement ID:** REQ-UTN-05
**Type:** Happy Path

* **Given** an Import LC is already `Issued` (Lifecycle State)
* **And** a partial Drawing presentation is currently `Pending Approval` (Workflow State)
* **When** a user views the Instrument Header
* **Then** the header displays both states:
  | UI Element | Value |
  | `Business State` | `Issued` |
  | `Action Status` | `Pending Approval` |
* **And** the visual "Global Status" badge uses the color for `Pending Approval` to signal urgency.
