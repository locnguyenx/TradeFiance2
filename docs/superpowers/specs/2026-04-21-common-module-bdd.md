# Behavior-Driven Development (BDD) Specification
**Project Name:** Digital Trade Finance Platform
**Module:** Common Module (Foundation, Maker/Checker, Facilities)
**Document Version:** 3.0 (Aligned with Design Spec v3.0)
**Date:** April 23, 2026

---

## 1. Traceability Summary

| Requirement Map (BRD ID) | Scenario ID | Title | Type |
|---|---|---|---|
| REQ-COM-ENT-01 | BDD-CMN-ENT-01 | Trade Transaction Base Attributes and Maker Tracking | Happy Path |
| REQ-COM-ENT-01 | BDD-CMN-ENT-01B | Transaction Version Increment on Authorization | Happy Path |
| REQ-COM-ENT-02 | BDD-CMN-ENT-02 | Valid Party KYC Acceptance | Happy Path |
| REQ-COM-ENT-02 | BDD-CMN-ENT-03 | Expired Party KYC Rejection | Edge Case |
| REQ-COM-ENT-03 | BDD-CMN-ENT-04 | Facility Limit Availability Earmark | Happy Path |
| REQ-COM-ENT-03 | BDD-CMN-ENT-05 | Expired Facility Block | Edge Case |
| REQ-COM-WF-01 | BDD-CMN-WF-01 | Processing Flow: Draft to Submitted with Dual-Status | Happy Path |
| REQ-COM-FX-01 | BDD-CMN-FX-01 | Precision: Zero Decimal JPY Format | Edge Case |
| REQ-COM-FX-01 | BDD-CMN-FX-02 | Precision: 2 Decimals USD Format | Happy Path |
| REQ-COM-FX-02 | BDD-CMN-FX-03 | Daily Board Rate for Limit Consumption | Happy Path |
| REQ-COM-FX-02 | BDD-CMN-FX-04 | Live FX Spread for Financial Settlement | Happy Path |
| REQ-COM-SLA-01 | BDD-CMN-SLA-01 | SLA Timer Skips Head Office Holidays | Happy Path |
| REQ-COM-SLA-02 | BDD-CMN-SLA-02 | Timer Exhaustion Generates System Block | Edge Case |
| REQ-COM-NOT-01 | BDD-CMN-NOT-01 | Proactive Facility 95% threshold Warning | Happy Path |
| REQ-COM-NOT-02 | BDD-CMN-NOT-02 | Sanctions Check triggers Queue Alert | Edge Case |
| REQ-COM-VAL-01 | BDD-CMN-VAL-01 | Hard Stop on Limit Breach | Edge Case |
| REQ-COM-VAL-02 | BDD-CMN-VAL-02 | Segregation of Duties Active Prevention | Edge Case |
| REQ-COM-VAL-02 | BDD-CMN-VAL-03 | Immutability Rule Prevents Active Record Mod | Edge Case |
| REQ-COM-VAL-03 | BDD-CMN-VAL-04 | Logic Guard: Expiry prior to Issue Date | Edge Case |
| REQ-COM-VAL-03 | BDD-CMN-VAL-05 | SWIFT Data Capture Validation (Layer 1) | Edge Case |
| REQ-COM-AUTH-01 | BDD-CMN-AUTH-01 | Tier Enforcement Calculation by Equivalent Amount | Happy Path |
| REQ-COM-AUTH-02 | BDD-CMN-AUTH-02 | Tier 4 Dual Checker Enforcement | Edge Case |
| REQ-COM-AUTH-03 | BDD-CMN-AUTH-03 | Amendment Total Liability Route Determination | Happy Path |
| REQ-COM-AUTH-03 | BDD-CMN-AUTH-04 | Compliance Route overrides Financial Route | Edge Case |
| REQ-COM-AUTH-01 | BDD-CMN-AUTH-05 | Priority Queue Ordering by Transaction Priority | Happy Path |
| REQ-COM-MAS-01 | BDD-CMN-MAS-01 | Tariff Matrix Evaluates Priority Overrides | Happy Path |
| REQ-COM-MAS-01 | BDD-CMN-MAS-02 | Tariff Matrix Evaluates Minimum Floor Fee | Edge Case |
| REQ-COM-MAS-02 | BDD-CMN-MAS-03 | Suspended Account Active Exclusion | Edge Case |
| REQ-COM-MAS-03 | BDD-CMN-MAS-04 | Mandatory Transaction Delta JSON Audit Log | Happy Path |
| REQ-COM-PRD-01 | BDD-CMN-PRD-01 | Configuration: Active Component Verification | Happy Path |
| REQ-COM-PRD-01 | BDD-CMN-PRD-02 | Configuration: Allowed Tenor Sight Restriction | Edge Case |
| REQ-COM-PRD-01 | BDD-CMN-PRD-03 | Configuration: Tolerance Limit Ceiling Check | Edge Case |
| REQ-COM-PRD-01 | BDD-CMN-PRD-04 | Configuration: Display Revolving Fields Rule | Happy Path |
| REQ-COM-PRD-01 | BDD-CMN-PRD-05 | Configuration: Advance Payment Doc Avoidance | Happy Path |
| REQ-COM-PRD-01 | BDD-CMN-PRD-06 | Configuration: Standby Routing Path Rule | Happy Path |
| REQ-COM-PRD-01 | BDD-CMN-PRD-07 | Configuration: Transferable Instructions Render | Happy Path |
| REQ-COM-PRD-01 | BDD-CMN-PRD-08 | Configuration: Islamic Ledger Classification | Happy Path |
| REQ-COM-PRD-01 | BDD-CMN-PRD-09 | Configuration: Mandatory Margin Prerequisite | Edge Case |
| REQ-COM-PRD-01 | BDD-CMN-PRD-10 | Configuration: Custom SLA Deadline Formula | Happy Path |
| REQ-COM-PRD-01 | BDD-CMN-PRD-11 | Configuration: Default SWIFT Base MT Generation | Happy Path |

---

## 2. Detailed BDD Scenarios

### Feature: Standard Master Data Entities & Constraints

#### Scenario BDD-CMN-ENT-01: Trade Transaction Base Attributes and Maker Tracking
**Requirement ID:** REQ-COM-ENT-01
**Type:** Happy Path

* **Given** a new trade transaction is initialized by the Maker
* **When** the service `create#ImportLetterOfCredit` executes
* **Then** the `TradeInstrument` record is created with transaction management fields populated:
  | Field | Expected Value |
  | `transactionRef` | Not Null (auto-generated `TF-IMP-YY-NNNN`) |
  | `lifecycleStatusId` | `INST_PRE_ISSUE` |
  | `transactionStatusId` | `TRANS_DRAFT` |
  | `makerUserId` | Current authenticated user ID |
  | `makerTimestamp` | Current system timestamp |
  | `versionNumber` | `1` |
  | `transactionDate` | Current business date |
  | `checkerUserId` | Null |
  | `checkerTimestamp` | Null |

#### Scenario BDD-CMN-ENT-01B: Transaction Version Increment on Authorization
**Requirement ID:** REQ-COM-ENT-01
**Type:** Happy Path

* **Given** a `TradeInstrument` with `versionNumber = 1` and `transactionStatusId = TRANS_SUBMITTED`
* **When** a Checker authorizes the transaction via `authorize#Instrument`
* **Then** the transaction management fields are updated:
  | Field | Expected Value |
  | `checkerUserId` | The Checker's authenticated user ID |
  | `checkerTimestamp` | Current system timestamp |
  | `transactionStatusId` | `TRANS_APPROVED` |
  | `versionNumber` | Unchanged (`1`) |
* **And** when a subsequent Amendment is authorized against this LC
* **Then** the `versionNumber` increments to `2`

#### Scenario BDD-CMN-ENT-02: Valid Party KYC Acceptance
**Requirement ID:** REQ-COM-ENT-02
**Type:** Happy Path

* **Given** a `TradeParty` Directory query against Applicant "Acme Corp"
* **And** the KYC Status logic evaluator returns "Active"
* **When** the Maker links the Applicant ID to the transaction
* **Then** the Entity mapper accepts the link without error flags:
  | Target Entity Link | System Response |
  | Party Role assigned Applicant | Success |

#### Scenario BDD-CMN-ENT-03: Expired Party KYC Rejection
**Requirement ID:** REQ-COM-ENT-02
**Type:** Edge Case

* **Given** a `TradeParty` Directory query against Applicant "Bad Corp"
* **And** the Party's KYC Expiry Date is "2026-01-01" (in the past)
* **When** a user actively attempts to associate this party to a target transaction
* **Then** the transaction enforces the Directory constraints natively:
  | Constraint Target | System Action |
  | Maker Save Request | Blocked - Exception Thrown |
  | Exception Alert Value | 'Party KYC status is expired.' |

#### Scenario BDD-CMN-ENT-04: Facility Limit Availability Earmark
**Requirement ID:** REQ-COM-ENT-03
**Type:** Happy Path

* **Given** a `CustomerFacility` entity designated "FAC-ACME-001"
* **And** the recorded stats evaluate to:
  | Field | Value |
  | Total Approved Limit | 5,000,000 USD |
  | Previous Utilized Amount | 1,000,000 USD |
* **When** an application executes a synchronous limit earmark for exactly 50,000 USD
* **Then** the facility instantly resolves to the new metrics:
  | Field | Evaluated Result |
  | Available Earmark | 3,950,000 USD |
  | Utilized Amount | 1,050,000 USD |

#### Scenario BDD-CMN-ENT-05: Expired Facility Block
**Requirement ID:** REQ-COM-ENT-03
**Type:** Edge Case

* **Given** a `CustomerFacility` entity designated "FAC-OLD-001"
* **And** the Facility Expiry Date is recorded as "2023-12-01"
* **When** an application requests an earmark confirmation, regardless of Available Balance
* **Then** the Facility Manager rejects the request:
  | Target Limit Engine Response | Status |
  | Allocation Permitted | False |
  | Exception Detail | 'Credit Facility completely Expired' |

---

### Feature: Standard Processing Flows and Timings

#### Scenario BDD-CMN-WF-01: Processing Flow: Draft to Submitted with Dual-Status
**Requirement ID:** REQ-COM-WF-01
**Type:** Happy Path

* **Given** a transaction with `transactionStatusId = TRANS_DRAFT` and `lifecycleStatusId = INST_PRE_ISSUE`
* **And** all pre-processing Validations have sequentially evaluated to True
* **When** the Maker explicitly activates submission via `submit#ForApproval`
* **Then** the dual-status model updates independently:
  | Status Dimension | Before | After |
  | `transactionStatusId` (processing) | `TRANS_DRAFT` | `TRANS_SUBMITTED` |
  | `lifecycleStatusId` (system) | `INST_PRE_ISSUE` | `INST_PENDING_APPROVAL` |
  | `makerTimestamp` | Updated to current timestamp |
* **And** the `ImportLetterOfCredit.businessStateId` remains `LC_DRAFT` (unaffected by submission — only authorization changes business state)

#### Scenario BDD-CMN-FX-01: Precision: Zero Decimal JPY Format
**Requirement ID:** REQ-COM-FX-01
**Type:** Edge Case

* **Given** an amount evaluation sequence targeting `10050.50`
* **And** the mapped Currency base is designated as `JPY` (ISO standard 0 decimals)
* **When** the universal rounding routine is invoked over the transaction array
* **Then** the result strictly aligns with zero-decimal precision standards:
  | Raw Amount | Truncated Legal Value (or rounded per param) |
  | 10050.50 | 10051 (rounded standard) |

#### Scenario BDD-CMN-FX-02: Precision: 2 Decimals USD Format
**Requirement ID:** REQ-COM-FX-01
**Type:** Happy Path

* **Given** an amount evaluation sequence targeting `5200.125`
* **And** the mapped Currency base is designated as `USD` (ISO standard 2 decimals)
* **When** the standard rounding routine applies scaling parameters
* **Then** the system maintains exactly two decimal nodes unconditionally:
  | Raw Amount | Evaluated Legal Value |
  | 5200.125 | 5200.13 |

#### Scenario BDD-CMN-FX-03: Daily Board Rate for Limit Consumption
**Requirement ID:** REQ-COM-FX-02
**Type:** Happy Path

* **Given** a base equivalent query required to allocate limits in USD
* **And** the working instrument specifies an original payload in EUR
* **When** the core FX resolver asks for identical conversions throughout the working day
* **Then** the system locks consumption conversions to the cached Board Rate values:
  | Board Rate Param Context | Resolution Value Example |
  | Selected Source Table | DAILY_CACHE |
  | Result for 100 EUR Query 1 | 105 USD |
  | Result for 100 EUR Query 2 | 105 USD |

#### Scenario BDD-CMN-FX-04: Live FX Spread for Financial Settlement
**Requirement ID:** REQ-COM-FX-02
**Type:** Happy Path

* **Given** the system is generating a Nostro Account funding sequence required to process an active cash settlement
* **And** the settlement bridges EUR into local USD equivalent physically
* **When** the FX resolver builds the remittance payload 
* **Then** the resolver forces a live Treasury REST look-up exclusively:
  | Live Spot Target Table | Evaluation Result Rules |
  | Selected Source Context | LIVE_API_TREASURY_PROXY |
  | Uses Daily Cache? | False |

#### Scenario BDD-CMN-SLA-01: SLA Timer Skips Head Office Holidays
**Requirement ID:** REQ-COM-SLA-01
**Type:** Happy Path

* **Given** the system generates an operational deadline set for purely `5 Banking Days`
* **And** the start time is a `Monday`, and a global Head-Office holiday strictly exists on the subsequent `Wednesday`
* **When** the global banking calendar formula applies
* **Then** the logic determines that `Wednesday`, `Saturday`, and `Sunday` do NOT compute against the total duration:
  | Day Additions Evaluated | M(1) T(2) W(0) Th(3) F(4) S(0) Su(0) M(5) |
  | Result Deadline Date | The following Monday |

#### Scenario BDD-CMN-SLA-02: Timer Exhaustion Generates System Block
**Requirement ID:** REQ-COM-SLA-02
**Type:** Edge Case

* **Given** a Document presentation is active and unassessed
* **And** the exact runtime spans past 5 Universal Banking Days since the start trigger
* **When** the continuous batch queue checks the document timer metrics
* **Then** the system forcibly applies an escalation label against the primary parent document to alert management:
  | Computed Day Difference | Enforced Evaluation Output Status |
  | 5 | Critical Blocking Exception Raised |

---

### Feature: Compliance, Validations, and Alerts

#### Scenario BDD-CMN-NOT-01: Proactive Facility 95% threshold Warning
**Requirement ID:** REQ-COM-NOT-01
**Type:** Happy Path

* **Given** a transaction actively deducts from `FAC-ACME-002` (Total Limit: `1,000,000 USD`)
* **When** the final limit offset places the `Utilized Amount` squarely at `960,000 USD`
* **Then** the system triggers the alert payload module logically for the risk manager group:
  | Condition (960k / 1M) | Active Trigger Result Metric |
  | Evaluated Coverage > 95% | True |
  | Alert Queued | Facility Overutilization Group Email |

#### Scenario BDD-CMN-NOT-02: Sanctions Check triggers Queue Alert
**Requirement ID:** REQ-COM-NOT-02
**Type:** Edge Case

* **Given** an operation applies a new Target Party to the instrument ("Banned Corp")
* **When** the validation gateway invokes instantaneous sanctions analysis and returns `Hit`
* **Then** standard execution routes are replaced immediately:
  | Event Hook Route | Outcome Target |
  | `lifecycleStatusId` | `INST_HOLD` |
  | `transactionStatusId` | Unchanged (remains current processing state) |
  | Compliance Queue Alert | Dispatched True |

#### Scenario BDD-CMN-VAL-01: Hard Stop on Limit Breach
**Requirement ID:** REQ-COM-VAL-01
**Type:** Edge Case

* **Given** an LC issuance `Base Equivalent Amount` calculation resolves to `5,000 USD`
* **And** the selected application Applicant Facility currently evaluates an Available Earmark limit of exactly `4,999 USD`
* **When** the initial pre-processing Limit Availability check resolves
* **Then** the system acts to enforce strict adherence to parameters by throwing a hard exception:
  | Action Trigger | Exception Handled Status |
  | State Change Application | Refused completely |
  | Message Emitted | Error: Exceeds specific Unsecured Facility Limit |

#### Scenario BDD-CMN-VAL-02: Segregation of Duties Active Prevention
**Requirement ID:** REQ-COM-VAL-02
**Type:** Edge Case

* **Given** a specific authenticated user `USER_MAKER_XX` was logged as the `makerUserId` of `TradeInstrument` for transaction `TF-IMP-001`
* **When** identical authenticated subject `USER_MAKER_XX` subsequently opens the Authorization view for `TF-IMP-001`
* **Then** the visual context disables manual progress vectors entirely to enforce Four-Eyes:
  | Target UI Logic Vector | Security Outcome Applied |
  | Authorization Interface Buttons | Read-only / disabled |
  | Endpoint Direct Auth Call Payload | Refused via Auth Middleware |

#### Scenario BDD-CMN-VAL-03: Immutability Rule Prevents Active Record Mod
**Requirement ID:** REQ-COM-VAL-02
**Type:** Edge Case

* **Given** an `ImportLetterOfCredit` evaluates `businessStateId = LC_ISSUED`
* **When** a user agent initiates a raw `PUT` standard core document update targeting a financial parameter (e.g., `effectiveAmount`)
* **Then** the system natively intercepts the action and demands an explicit event payload:
  | Update Evaluation Route | Exception Status |
  | Modification Method Evaluated | Bypassed. Formal Amendment Process Requested. |

#### Scenario BDD-CMN-VAL-04: Logic Guard: Expiry prior to Issue Date
**Requirement ID:** REQ-COM-VAL-03
**Type:** Edge Case

* **Given** a generic user enters `Issue Date` as `2026-06-01`
* **When** the generic user actively sets `Expiry Date` to strictly previous date `2026-05-01`
* **Then** the validation controller refuses progression without caveat:
  | Logical Comparison Target | Resolution |
  | Expiry Date < Issue Date? | True (Throws Business Rules Array Exception) |

#### Scenario BDD-CMN-VAL-05: SWIFT Data Capture Validation (Layer 1) [NEW]
**Requirement ID:** REQ-COM-VAL-03
**Type:** Edge Case

* **Given** a Maker enters Description of Goods text containing the character `@` at position 142
* **When** the Maker saves the draft via `create#ImportLetterOfCredit` or `update#ImportLetterOfCredit`
* **Then** Layer 1 validation service `validate#SwiftFields` rejects with a field-specific error:
  | Validation Check | Result |
  | Field | `goodsDescription` |
  | Error Message | "Description of Goods contains invalid SWIFT character '@' at position 142" |
  | Transaction Saved | No — blocked before persistence |
* **And** the same validation catches `transactionRef` exceeding 16 characters or containing `//`

---

### Feature: Maker/Checker Framework Execution

#### Scenario BDD-CMN-AUTH-01: Tier Enforcement Calculation by Equivalent Amount
**Requirement ID:** REQ-COM-AUTH-01
**Type:** Happy Path

* **Given** an input parameter targets an instrument transaction containing 70,000 USD Base Equivalent
* **When** the queue determines proper visibility logic
* **Then** the system enforces visibility solely to mapped Tier 1 Officers or strictly higher.
  | Required Matrix Limit Mapping | Condition Passed |
  | Tier 1 Limit Map ($100K) | Valid |

#### Scenario BDD-CMN-AUTH-02: Tier 4 Dual Checker Enforcement
**Requirement ID:** REQ-COM-AUTH-02
**Type:** Edge Case

* **Given** the `TradeInstrument.baseEquivalentAmount` computes at `8,000,000 USD` (mapped Tier 4 equivalent)
* **When** the Maker pushes to the Authorization gateway and one Tier 4 member executes `Approve`
* **Then** the workflow transitions to `INST_PARTIAL_APPROVAL` (not `INST_AUTHORIZED`)
* **And** a `TradeApprovalRecord` is created with `approvalSequence = 1`
* **And** the transaction reappears in the Checker queue for a second distinct Tier 4 user
* **And** the second Checker must be different from both the Maker (`makerUserId`) and the first Checker
* **And** only after the second approval does `lifecycleStatusId` transition to `INST_AUTHORIZED`

#### Scenario BDD-CMN-AUTH-03: Amendment Total Liability Route Determination
**Requirement ID:** REQ-COM-AUTH-03
**Type:** Happy Path

* **Given** an `ImportLetterOfCredit` with `effectiveAmount = 900,000 USD`
* **And** an Amendment increases the `effectiveAmount` by `150,000 USD` (new total: `1,050,000 USD`)
* **When** the system recalculates the limit bounds against the Maker-Checker matrix tier framework
* **Then** it uses the **new `effectiveAmount` (1,050,000 USD)** for tier routing — not the isolated $150k delta
* **And** this routes to Tier 3 (>$1M) instead of Tier 1

#### Scenario BDD-CMN-AUTH-04: Compliance Route overrides Financial Route
**Requirement ID:** REQ-COM-AUTH-03
**Type:** Edge Case

* **Given** an instrument has triggered `check#Sanctions` returning `isHit = true`
* **And** `lifecycleStatusId` has been set to `INST_HOLD`
* **When** the Checker matrix evaluates conditional checks
* **Then** standard Financial Queue authorization is blocked until `release#ComplianceHold` is invoked by a `TRADE_COMPLIANCE_OFFICER`

#### Scenario BDD-CMN-AUTH-05: Priority Queue Ordering by Transaction Priority [NEW]
**Requirement ID:** REQ-COM-AUTH-01
**Type:** Happy Path

* **Given** two transactions in the Checker queue: TX-A with `priorityEnumId = NORMAL` and TX-B with `priorityEnumId = URGENT`
* **When** the Checker queue list is rendered
* **Then** TX-B appears above TX-A regardless of submission timestamp

---

### Feature: Tariff Engines & Configuration Catalogs (The Product Matrix)

#### Scenario BDD-CMN-MAS-01: Tariff Matrix Evaluates Priority Overrides
**Requirement ID:** REQ-COM-MAS-01
**Type:** Happy Path

* **Given** a designated standard configuration calculates product issuance effectively at `0.20%`
* **And** the specialized customer explicitly possesses a Customer Tier rule assigning `0.10%` to Issuance
* **When** the Tariff engine collects fee structure data points upon action termination
* **Then** the engine overrides the default natively honoring specific customer tier mappings primarily before applying math.

#### Scenario BDD-CMN-MAS-02: Tariff Matrix Evaluates Minimum Floor Fee
**Requirement ID:** REQ-COM-MAS-01
**Type:** Edge Case

* **Given** the applied fee computes arithmetically via the rate formulas to entirely equal `$15 USD`
* **And** the `FeeConfiguration.minFloorAmount` rigidly states a Minimum Charge equivalent to `$50 USD`
* **When** the fee collector executes the final summation
* **Then** the output natively substitutes the minimum equivalent to eliminate undersized processing fees:
  | Target Value Applied Against Ledgers | 
  | $50 USD |

#### Scenario BDD-CMN-MAS-03: Suspended Account Active Exclusion
**Requirement ID:** REQ-COM-MAS-02
**Type:** Edge Case

* **Given** a user agent identity natively exists inside a Tier 3 authorization matrix in `UserAuthorityProfile`
* **When** an HR automation explicitly updates `UserAuthorityProfile.isSuspended` to `Y`
* **Then** global lists explicitly eliminate user visibility from Maker queues and authorization assignments to preempt workflow stalls.

#### Scenario BDD-CMN-MAS-04: Mandatory Transaction Delta JSON Audit Log
**Requirement ID:** REQ-COM-MAS-03
**Type:** Happy Path

* **Given** a save request commits a state-modifying delta against the physical DB
* **When** the save commit formally finalizes inside the application structure layer
* **Then** an explicit Append-Only `TradeTransactionAudit` record logs:
  | Target Audit Key Attributes | Existence |
  | `snapshotDeltaJSON` (Before/After) | Checked |
  | `userId` | Checked |
  | `timestamp` | Checked |
  | `ipAddress` | Checked |
  | `fieldChanged` | Checked |

---

### Feature: REQ-COM-PRD-01 (Product Configuration Matrix - Extensive Coverage)

#### Scenario BDD-CMN-PRD-01: Configuration: Active Component Verification
**Requirement ID:** REQ-COM-PRD-01
**Type:** Happy Path

* **Given** an administrator effectively configured `"SBLC_COMM"` `isActive` property to `N` in `TradeProductCatalog`
* **When** a user hits the New Application rendering sequence view
* **Then** the component completely ignores rendering the target definition options over dropdown inputs:
  | Application Value | Evaluates Missing |

#### Scenario BDD-CMN-PRD-02: Configuration: Allowed Tenor Sight Restriction
**Requirement ID:** REQ-COM-PRD-01
**Type:** Edge Case

* **Given** a `TradeProductCatalog` entry maps `allowedTenorEnumId` exclusively to `SIGHT_ONLY`
* **When** an application input passes a parameter payload asserting `tenorTypeId: USANCE`
* **Then** the core logic throws a validation assertion denying progress against the product configuration.

#### Scenario BDD-CMN-PRD-03: Configuration: Tolerance Limit Ceiling Check
**Requirement ID:** REQ-COM-PRD-01
**Type:** Edge Case

* **Given** a `TradeProductCatalog` specifies `maxToleranceLimit` explicitly to `10`
* **When** the user explicitly attempts to input a positive tolerance value of `25%`
* **Then** the system automatically generates an explicit UI exception denying values exceeding the product limit matrix configuration.

#### Scenario BDD-CMN-PRD-04: Configuration: Display Revolving Fields Rule
**Requirement ID:** REQ-COM-PRD-01
**Type:** Happy Path

* **Given** a `TradeProductCatalog` specifies `allowRevolving = Y`
* **When** the framework generates the application entry screen
* **Then** the module injects form objects supporting "Reinstatement parameters" directly to the client screen output dynamically.

#### Scenario BDD-CMN-PRD-05: Configuration: Advance Payment Doc Avoidance
**Requirement ID:** REQ-COM-PRD-01
**Type:** Happy Path

* **Given** the `TradeProductCatalog` records `allowAdvancePayment = Y` (typically Red Clause LCs)
* **When** the Beneficiary invokes a pre-shipment documentation presentation against the system workflow 
* **Then** the logic evaluator bypasses typical standard UCP transportation document validations natively to accept a simple receipt input matrix automatically.

#### Scenario BDD-CMN-PRD-06: Configuration: Standby Routing Path Rule
**Requirement ID:** REQ-COM-PRD-01
**Type:** Happy Path

* **Given** the `TradeProductCatalog` evaluates `isStandby = Y`
* **When** the core evaluates normal presentation behaviors and standard 5-day checks 
* **Then** the system natively switches workflow tracks utilizing local Guarantee processing mechanics to handle non-performance behaviors effectively.

#### Scenario BDD-CMN-PRD-07: Configuration: Transferable Instructions Render
**Requirement ID:** REQ-COM-PRD-01
**Type:** Happy Path

* **Given** a `TradeProductCatalog` maps `isTransferable = Y`
* **When** the user actively parses the data 
* **Then** the application triggers the inclusion of a specific "Transfer Instructions" tab inherently visible across Maker interface vectors.

#### Scenario BDD-CMN-PRD-08: Configuration: Islamic Ledger Classification
**Requirement ID:** REQ-COM-PRD-01
**Type:** Happy Path

* **Given** the `TradeProductCatalog.accountingFrameworkEnumId` is set to `ISLAMIC`
* **When** the backend prepares physical accounting vouchers effectively for fee allocations
* **Then** the system forcibly routes computations relying on "Profit Rates" bypassing "Interest Rates" entirely mapping to distinct Islamic GL arrays securely.

#### Scenario BDD-CMN-PRD-09: Configuration: Mandatory Margin Prerequisite
**Requirement ID:** REQ-COM-PRD-01
**Type:** Edge Case

* **Given** the `TradeProductCatalog.mandatoryMarginPercent` is set to `100`
* **When** the Maker pushes submission mechanics towards the system
* **Then** the framework absolutely denies validation assertions if evaluating standard unsecured credit facilities, natively forcing identical 100% equivalent holds over local user deposits inherently.

#### Scenario BDD-CMN-PRD-10: Configuration: Custom SLA Deadline Formula
**Requirement ID:** REQ-COM-PRD-01
**Type:** Happy Path

* **Given** the `TradeProductCatalog.documentExamSlaDays` is configured to `2` instead of global standard 5
* **When** a document logs to the system successfully  
* **Then** the background timer establishes the hard escalation limit calculated around the `2` custom mapped property days dynamically.

#### Scenario BDD-CMN-PRD-11: Configuration: Default SWIFT Base MT Generation
**Requirement ID:** REQ-COM-PRD-01
**Type:** Happy Path

* **Given** the `TradeProductCatalog.defaultSwiftFormatEnumId` is set to `MT760`
* **When** the standard process triggers the authorization routines dispatching automated message frameworks
* **Then** the engine automatically routes the payload data against the MT760 standard definitions completely ignoring default MT700 structures.
