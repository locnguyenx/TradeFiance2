# Behavior-Driven Development (BDD) Specification
**Project Name:** Digital Trade Finance Platform
**Module:** Import Letter of Credit (LC)
**Document Version:** 1.1 (Exhaustive Traceability)
**Date:** April 21, 2026

---

## Traceability Summary
*(Note: Every scenario below links to specific REQ-IMP IDs natively. This ensures 1:1 coverage.)*

---

## Feature: 1. Overview & LC Lifecycle States
*(Coverage: REQ-IMP-01, REQ-IMP-02, REQ-IMP-NOTE-01/02/03, REQ-IMP-DEF-01/02, REQ-IMP-DTL-01/02, REQ-IMP-STATE-01/02, REQ-IMP-FLOW-01..08)*

### Scenario: BDD-IMP-STATE-01: Initialization of an Import LC
**Requirement ID:** REQ-IMP-FLOW-01, REQ-IMP-DEF-01, REQ-IMP-DEF-02
**Type:** Happy Path

* **Given** a Trade Operations user initiates a new Import LC via the standard issuance form
* **When** they manually save the record without submitting for authorization
* **Then** the system commits the record with the following state configuration:
  | Output Field | Expected Value |
  | Transaction State | Draft |
  | LC Business State | Draft |
  | Limit Blocked | None |

### Scenario: BDD-IMP-STATE-02: State Transition via Maker Submission
**Requirement ID:** REQ-IMP-FLOW-02
**Type:** Happy Path

* **Given** a drafted Import LC with the following validation state:
  | Field | Value |
  | Transaction Ref | LC-2026-0001 |
  | Transaction State | Draft |
* **When** the Maker executes the "Submit for Approval" action
* **Then** the workflow engine applies the following updates:
  | Output Field | Expected Value |
  | Transaction State | Pending Approval |
  | LC Business State | Draft |

### Scenario: BDD-IMP-STATE-03: State Transition via Checker Approval
**Requirement ID:** REQ-IMP-FLOW-03
**Type:** Happy Path

* **Given** a submitted Import LC with the following configuration:
  | Field | Value |
  | Transaction Ref | LC-2026-0001 |
  | Transaction State | Pending Approval |
  | LC Business State | Draft |
* **When** an authorized Checker executes the "Authorize" action
* **Then** the workflow engine applies the following state promotion:
  | Output Field | Expected Value |
  | Transaction State | Processed/Closed |
  | LC Business State | Issued |

### Scenario: BDD-IMP-STATE-04: State Transition upon Document Arrival
**Requirement ID:** REQ-IMP-FLOW-04
**Type:** Happy Path

* **Given** an actively issued Import LC
* **When** a document presentation payload is received from the Beneficiary's bank
* **Then** the target LC record state is updated as follows:
  | Output Field | Expected Value |
  | Transaction State | Processed/Closed |
  | LC Business State | Documents Received |

### Scenario: BDD-IMP-STATE-05: State Transition upon Discrepancy Assessment
**Requirement ID:** REQ-IMP-FLOW-05
**Type:** Edge Case

* **Given** an Import LC currently under Document Examination
* **When** the operations team saves the examination with the "Discrepant" flag enabled
* **Then** the transaction state is suspended for Applicant response:
  | Output Field | Expected Value |
  | LC Business State | Discrepant |
  | Required Next Action | Applicant Waiver Response |

### Scenario: BDD-IMP-STATE-06: State Transition upon Clean Assessment
**Requirement ID:** REQ-IMP-FLOW-06
**Type:** Happy Path

* **Given** an Import LC currently under Document Examination
* **When** the operations team submits a "Clean" validation assessment
* **Then** the system promotes the LC maturity flow:
  | Output Field | Expected Value |
  | LC Business State | Accepted / Clean |
  | Financial Flag | Liability Formally Recognized |

### Scenario: BDD-IMP-STATE-07: State Transition upon Full Settlement
**Requirement ID:** REQ-IMP-FLOW-07
**Type:** Happy Path

* **Given** an Import LC holding liability in the "Accepted / Clean" business state
* **When** the complete financial settlement action completes against the core ledger
* **Then** the lifecycle gracefully concludes:
  | Output Field | Expected Value |
  | LC Business State | Settled |
  | Active Liability | Zero |

### Scenario: BDD-IMP-STATE-08: State Transition upon Cancellation or Expiry
**Requirement ID:** REQ-IMP-FLOW-08
**Type:** Edge Case

* **Given** an active Unutilized Import LC
* **When** the "Cancel Transaction" protocol is authorized by a tier-appropriate Checker
* **Then** the system permanently locks the transaction:
  | Output Field | Expected Value |
  | LC Business State | Closed / Cancelled |
  | Future Actions Allowed | None (Read-Only) |

---

## Feature: 3. Core Business Processes

### Process: 3.1 LC Issuance
*(Coverage: REQ-IMP-03, REQ-IMP-PRC-01, REQ-IMP-SPEC-01, REQ-IMP-04, REQ-IMP-DTL-00)*

### Scenario: BDD-IMP-PRC-ISS-01: Facility Limit Verification on Submission
**Requirement ID:** REQ-IMP-SPEC-01, REQ-IMP-04
**Type:** Happy Path

* **Given** a prepared Draft LC with the following inputs:
  | Request Field | Input Value |
  | LC Base Equivalent Amount | 350,000 USD |
  | Target Facility Available Balance | 500,000 USD |
* **When** the Maker attempts to submit the LC for approval
* **Then** the business validation engine responds:
  | Validation Check | Expected Result |
  | Facility Limit Check | pass |
  | Earmarked Amount | 350,000 USD |
* **And** the submission successfully routes to `Pending Approval`

### Scenario: BDD-IMP-PRC-ISS-02: Facility Limit Violation Rejection
**Requirement ID:** REQ-IMP-SPEC-01, REQ-IMP-04
**Type:** Edge Case

* **Given** a prepared Draft LC with the following inputs:
  | Request Field | Input Value |
  | LC Base Equivalent Amount | 600,000 USD |
  | Target Facility Available Balance | 500,000 USD |
* **When** the Maker attempts to submit the LC for approval
* **Then** the transaction is forcibly rejected:
  | Validation Check | Expected Result |
  | Facility Limit Check | fail |
  | Earmarked Amount | 0 USD |
* **And** the UI emits the explicit exception message "Insufficient Facility Limit"

### Scenario: BDD-IMP-PRC-ISS-03: LC Auto-Earmark Reversal on Rejection
**Requirement ID:** REQ-IMP-SPEC-01
**Type:** Edge Case

* **Given** an LC waiting in the `Pending Approval` queue holding an active Earmark block of 100,000 USD
* **When** the designated Checker rejects the issuance
* **Then** the limit facility engine restores the balance:
  | Action Executed | Expected State |
  | Earmark Block | Relinquished (0 USD) |
  | Transaction State | Draft |

---

### Process: 3.2 Amendments
*(Coverage: REQ-IMP-PRC-02, REQ-IMP-SPEC-02)*

### Scenario: BDD-IMP-PRC-AMD-01: Amendment Triggering Amount Increase Limit Earmark
**Requirement ID:** REQ-IMP-SPEC-02
**Type:** Happy Path

* **Given** an active `Issued` LC with an existing contingent liability of 50,000 USD
* **When** a Maker submits an Amendment strictly increasing the LC amount by 10,000 USD
* **Then** the Limit validation engine executes an incremental check:
  | Action | Value Checked | Expected Result |
  | Incremental Earmark Hold | +10,000 USD | Success |
  | Total Contingent | 60,000 USD (upon auth) | Calculated |

### Scenario: BDD-IMP-PRC-AMD-02: Non-Financial Amendment Processing
**Requirement ID:** REQ-IMP-SPEC-02
**Type:** Happy Path

* **Given** an active `Issued` LC 
* **When** a Maker submits an Amendment solely altering the "Port of Loading" (non-financial)
* **Then** the validation engine skips the Credit limit block sequence completely:
  | Action | Expected Result |
  | Limit Earmark Call | Ignored |
  | SWIFT Tag Modified | 44A |

---

### Process: 3.3 Document Presentation & Examination
*(Coverage: REQ-IMP-PRC-03, REQ-IMP-SPEC-03)*

### Scenario: BDD-IMP-PRC-DOC-01: Discrepancy Waiver Acceptance
**Requirement ID:** REQ-IMP-SPEC-03
**Type:** Happy Path

* **Given** an Import LC business state is `Discrepant`
* **When** the Maker receives a formal waiver from the Applicant and triggers the "Applicant Accepts Waiver" system action
* **And** the Checker authorizes the waiver
* **Then** the processing engine overrides the hold:
  | Output Field | Expected Result |
  | LC Business State | Accepted / Clean |
  | Discrepancy Fee | Assessed |

### Scenario: BDD-IMP-PRC-DOC-02: Discrepancy Refusal
**Requirement ID:** REQ-IMP-SPEC-03
**Type:** Edge Case

* **Given** an Import LC business state is `Discrepant`
* **When** the Applicant instructs the bank to refuse the presentation
* **And** the Trade Operator enters "Applicant Refused"
* **Then** generating the standard rejection SWIFT message is queued:
  | Output Field | Expected Result |
  | MT Message Triggered | MT734 (Notice of Refusal) |
  | LC Business State | Issued (Documents returned) |

---

### Process: 3.4 Settlement & Payment
*(Coverage: REQ-IMP-PRC-04, REQ-IMP-SPEC-04)*

### Scenario: BDD-IMP-PRC-SET-01: Limit Reconciliation Post-Settlement
**Requirement ID:** REQ-IMP-SPEC-04
**Type:** Happy Path

* **Given** an `Accepted` Sight presentation valued at 50,000 USD
* **When** the Settlement is executed by Operations
* **Then** the liability reconciliation executes precise limit restoration:
  | Output Metrics | Result |
  | Contingent Liability Offset | - 50,000 USD |
  | Nostro Account Entry | Debited 50,000 |

---

### Process: 3.5 & 3.6 Shipping Guarantees and Cancellations
*(Coverage: REQ-IMP-SPEC-05, REQ-IMP-SPEC-06)*

### Scenario: BDD-IMP-PRC-SG-01: SG Liability Allocation (100% Minimum)
**Requirement ID:** REQ-IMP-SPEC-05
**Type:** Happy Path

* **Given** an unutilized `Issued` LC for 500,000 USD
* **When** the Maker generates a Shipping Guarantee linked to this LC
* **Then** the system automatically consumes the applicant limit structure based on SG indemnity rules:
  | Target Metric | Formula Expected Value |
  | SG Limit Consumption | 500,000 USD * 100% Minimum Margin |

### Scenario: BDD-IMP-PRC-CAN-01: Expiry Hard Closure
**Requirement ID:** REQ-IMP-SPEC-06
**Type:** Edge Case

* **Given** an Unutilized `Issued` LC with an Expiry Date of "2026-03-01"
* **And** the current global system time evaluates to "2026-03-31" 
* **When** the nightly End of Day (EOD) batch validation sequence executes
* **Then** the batch job auto-closes the expired record:
  | Target Metric | Expected Update |
  | LC Business State | Closed / Cancelled |
  | Contingent Limit Reversal | 100% released |

---

## Feature: 5. MT700 Generation Mapping
*(Coverage: REQ-IMP-05, REQ-IMP-SWIFT-01, REQ-IMP-SWIFT-02, REQ-IMP-SWIFT-03, REQ-IMP-SWIFT-03A-03D, REQ-IMP-SWIFT-04, REQ-IMP-SWIFT-05)*

### Scenario: BDD-IMP-SWIFT-01: Mandatory Tag Integrity Validation
**Requirement ID:** REQ-IMP-SWIFT-01, REQ-IMP-SWIFT-02
**Type:** Happy Path

* **Given** a successfully `Issued` Import LC
* **When** the system invokes the internal MT700 payload constructor
* **Then** the generator strictly builds and populates the required sequences:
  | Database Field | Expected MT Tag Output |
  | LC Number | 20: Documentary Credit Number |
  | Expiry Date | 31D: Date and Place of Expiry |
  | Base Amount / CCY | 32B: Currency Code, Amount |
  | Applicant Name | 50: Applicant |

### Scenario: BDD-IMP-SWIFT-02: Dynamic 'a' Identifier Extension (59 vs 59A)
**Requirement ID:** REQ-IMP-SWIFT-04
**Type:** Edge Case

* **Given** an Import LC record pending payload generation
* **And** the target Beneficiary Party record directly contains a definitive `SWIFT BIC` code
* **When** the MT generator evaluates the target Beneficiary tagging
* **Then** the schema engine applies the structured `A` variant instead of the free-text payload:
  | Logic Case | Expected Evaluated Tag |
  | Beneficiary Has Valid BIC | 59A (Routing Code) |

### Scenario: BDD-IMP-SWIFT-03: Formatting Validation and Line Wrap Enforcements
**Requirement ID:** REQ-IMP-SWIFT-05
**Type:** Edge Case

* **Given** a Maker inputs 800 characters of 'Goods Description'
* **When** the system generates the SWIFT payload
* **Then** the SWIFT rules engine applies the physical payload limitations:
  | Validation Property | Standard Constraint |
  | Character Set Allowed | SWIFT X (Alpha, Number, SpecChars) |
  | Maximum Field Length | 65 columns |
  | Tag Line Wrapping Limit | Forced carriage return per ISBP structure |
