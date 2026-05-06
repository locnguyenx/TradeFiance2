# Consolidated Behavior-Driven Development (BDD) Specification
**ABOUTME:** Consolidated BDD scenarios for the Import Letter of Credit module.
This document merges all source BDD specs into one traceable document aligned with the consolidated BRD.

**Project Name:** Digital Trade Finance Platform
**Module:** Import Letter of Credit (LC)
**Document Version:** 2.0
**Date:** May 06, 2026

**Superseded BDDs:**
*   `2026-04-21-import-lc-bdd.md`

---

## 1. Traceability Matrix

| Feature (BRD) | Scenario ID | Title | Type | Source BRD Req | User Story |
|---|---|---|---|---|---|
| **2.1 Issuance** | BDD-IMP-ISS-01 | State transition: Save to Draft (effective values) | Happy | REQ-IMP-FLOW-01 | US-ISS-01 |
| **2.1 Issuance** | BDD-IMP-ISS-02 | State transition: Submit to Pending Approval | Happy | REQ-IMP-FLOW-02 | US-ISS-01 |
| **2.1 Issuance** | BDD-IMP-ISS-03 | State transition: Authorize to Issued (effective snapshot) | Happy | REQ-IMP-FLOW-03 | US-ISS-01 |
| **2.1 Issuance** | BDD-IMP-ISS-04 | Issuance: Facility earmark via effective amount | Happy | REQ-IMP-SPEC-01 | US-ISS-01 |
| **2.1 Issuance** | BDD-IMP-ISS-05 | Issuance: Mandatory cash margin block | Edge | REQ-IMP-SPEC-01 | US-ISS-01 |
| **2.1 Issuance** | BDD-IMP-ISS-06 | Issuance: Effective values initialized on LC | Happy | REQ-IMP-SPEC-01 | US-ISS-01 |
| **2.1 Issuance** | BDD-IMP-ISS-07 | State transition: Invalid transition rejected | Edge | REQ-IMP-STATE-02 | US-ISS-01 |
| **2.1 Issuance** | BDD-IMP-ISS-08 | Vietnam FX regulatory tagging | Happy | REQ-IMP-04 | US-ISS-01 |
| **2.1 Issuance** | BDD-IMP-SWT-01 | MT700: X-Character base validation | Edge | REQ-IMP-SWIFT-01 | US-ISS-01 |
| **2.1 Issuance** | BDD-IMP-SWT-02 | MT700: Mandatory block validation | Happy | REQ-IMP-SWIFT-02 | US-ISS-01 |
| **2.1 Issuance** | BDD-IMP-SWT-03 | MT700: Tolerance output formatter | Edge | REQ-IMP-SWIFT-03A | US-ISS-01 |
| **2.1 Issuance** | BDD-IMP-SWT-04 | MT700: 'A' Designation swap (59/59A) | Edge | REQ-IMP-SWIFT-04 | US-ISS-01 |
| **2.1 Issuance** | BDD-IMP-SWT-05 | MT700: 65-char splitting with MT 701 continuation | Edge | REQ-IMP-SWIFT-05 | US-ISS-01 |
| **2.2 Amendments** | BDD-IMP-AMD-01 | Amendment: Financial increase updates effective amount | Happy | REQ-IMP-SPEC-02 | US-AMD-01 |
| **2.2 Amendments** | BDD-IMP-AMD-02 | Amendment: Negative delta releases limits | Happy | REQ-IMP-SPEC-02 | US-AMD-01 |
| **2.2 Amendments** | BDD-IMP-AMD-03 | Amendment: Non-financial bypasses limits | Happy | REQ-IMP-SPEC-02 | US-AMD-01 |
| **2.2 Amendments** | BDD-IMP-AMD-04 | Amendment: Pending beneficiary consent | Edge | REQ-IMP-SPEC-02 | US-AMD-01 |
| **2.2 Amendments** | BDD-IMP-AMD-05 | Amendment: Version number incremented | Happy | REQ-IMP-SPEC-02 | US-AMD-01 |
| **2.2 Amendments** | BDD-IMP-AMD-06 | MT707: Amendment message generation | Happy | REQ-IMP-SWIFT-06 | US-AMD-01 |
| **2.3 Presentation** | BDD-IMP-DOC-01 | Presentation: Examination timer enforcement | Happy | REQ-IMP-SPEC-03 | US-PRE-01 |
| **2.3 Presentation** | BDD-IMP-DOC-02 | Presentation: Internal notice on discrepancy | Edge | REQ-IMP-SPEC-03 | US-PRE-01 |
| **2.3 Presentation** | BDD-IMP-DOC-03 | Presentation: Waiver generates MT 752 | Happy | REQ-IMP-SPEC-03 | US-PRE-01 |
| **2.3 Presentation** | BDD-IMP-DOC-04 | State transition: Receive docs | Happy | REQ-IMP-FLOW-04 | US-PRE-01 |
| **2.3 Presentation** | BDD-IMP-DOC-05 | State transition: Review to discrepant | Edge | REQ-IMP-FLOW-05 | US-PRE-01 |
| **2.3 Presentation** | BDD-IMP-DOC-06 | State transition: Review to clean/accepted | Happy | REQ-IMP-FLOW-06 | US-PRE-01 |
| **2.3 Presentation** | BDD-IMP-VAL-01 | Drawn tolerance over-draw block | Edge | REQ-IMP-04 | US-PRE-01 |
| **2.3 Presentation** | BDD-IMP-VAL-02 | Late presentation expiry block | Edge | REQ-IMP-04 | US-PRE-01 |
| **2.3 Presentation** | BDD-IMP-VAL-03 | Auto-reinstatement of revolving LC | Happy | REQ-IMP-04 | US-PRE-01 |
| **2.4 Settlement** | BDD-IMP-SET-01 | Settlement: Usance future queue mapping | Happy | REQ-IMP-SPEC-04 | US-STL-01 |
| **2.4 Settlement** | BDD-IMP-SET-02 | Settlement: Nostro entry posting | Happy | REQ-IMP-SPEC-04 | US-STL-01 |
| **2.4 Settlement** | BDD-IMP-SET-03 | Settlement: Partial draw updates effective outstanding | Happy | REQ-IMP-SPEC-04 | US-STL-01 |
| **2.4 Settlement** | BDD-IMP-SET-04 | State transition: Settled decreases effective outstanding | Happy | REQ-IMP-FLOW-07 | US-STL-01 |
| **2.5 Shipping Guarantee** | BDD-IMP-SG-01 | Ship Guar: 110% over-indemnity earmark | Edge | REQ-IMP-SPEC-05 | US-SG-01 |
| **2.5 Shipping Guarantee** | BDD-IMP-SG-02 | Ship Guar: B/L exchange waiver lock | Happy | REQ-IMP-SPEC-05 | US-SG-02 |
| **2.6 Cancellations** | BDD-IMP-CAN-01 | Cancellation: End of day auto-expiry flush | Happy | REQ-IMP-SPEC-06 | US-CAN-01 |
| **2.6 Cancellations** | BDD-IMP-CAN-02 | Cancellation: Active limit reversal | Happy | REQ-IMP-SPEC-06 | US-CAN-01 |
| **2.6 Cancellations** | BDD-IMP-CAN-03 | State transition: Closed terminates actions | Edge | REQ-IMP-FLOW-08 | US-CAN-01 |

---

## 2. Detailed BDD Scenarios

### Feature 2.1: Issuance

#### Scenario BDD-IMP-ISS-01: State transition: Save to Draft (effective values initialized)
**US-ISS-01 | REQ-IMP-FLOW-01**
*Type: Happy Path*

* **Given** a user inputs generic parameter sets into a new Import LC component with `amount = 500,000 USD` and `tolerancePositive = 0.10`
* **When** the `create#ImportLetterOfCredit` service executes
* **Then** two entities are created with synchronized data:
  | Entity | Field | Expected Value |
  | `TradeInstrument` | `amount` | 500,000 (original snapshot) |
  | `TradeInstrument` | `transactionStatusId` | `TRANS_DRAFT` |
  | `TradeInstrument` | `lifecycleStatusId` | `INST_PRE_ISSUE` |
  | `TradeInstrument` | `versionNumber` | `1` |
  | `ImportLetterOfCredit` | `businessStateId` | `LC_DRAFT` |
  | `ImportLetterOfCredit` | `effectiveAmount` | 500,000 (initialized from TradeInstrument) |
  | `ImportLetterOfCredit` | `effectiveExpiryDate` | Same as `TradeInstrument.expiryDate` |
  | `ImportLetterOfCredit` | `effectiveTolerancePositive` | 0.10 |
  | `ImportLetterOfCredit` | `effectiveOutstandingAmount` | 500,000 |
  | `ImportLetterOfCredit` | `cumulativeDrawnAmount` | 0 |
  | `ImportLetterOfCredit` | `totalAmendmentCount` | 0 |

#### Scenario BDD-IMP-ISS-02: State transition: Submit to Pending Approval
**US-ISS-01 | REQ-IMP-FLOW-02**
*Type: Happy Path*

* **Given** an `ImportLetterOfCredit` with `businessStateId = LC_DRAFT`
* **And** `TradeInstrument.transactionStatusId = TRANS_DRAFT`
* **When** a user inputs mandatory components and fires `submit#ForApproval`
* **Then** the dual-status model updates:
  | Status Dimension | Before | After |
  | `TradeInstrument.lifecycleStatusId` | `INST_PRE_ISSUE` | `INST_PENDING_APPROVAL` |
  | `TradeInstrument.transactionStatusId` | `TRANS_DRAFT` | `TRANS_SUBMITTED` |
  | `ImportLetterOfCredit.businessStateId` | `LC_DRAFT` | `LC_PENDING` |

#### Scenario BDD-IMP-ISS-03: State transition: Authorize to Issued (effective snapshot)
**US-ISS-01 | REQ-IMP-FLOW-03**
*Type: Happy Path*

* **Given** a transaction with `lifecycleStatusId = INST_PENDING_APPROVAL`
* **When** a successfully authorized Checker clicks `Authorize` via `authorize#Instrument`
* **Then** both status dimensions finalize:
  | Status Dimension | End State |
  | `ImportLetterOfCredit.businessStateId` | `LC_ISSUED` |
  | `TradeInstrument.lifecycleStatusId` | `INST_AUTHORIZED` |
  | `TradeInstrument.transactionStatusId` | `TRANS_APPROVED` |
  | `TradeInstrument.checkerUserId` | Current Checker's user ID |
  | `TradeInstrument.checkerTimestamp` | Current timestamp |
* **And** the `TradeInstrument.amount` is frozen — subsequent amendments update only `ImportLetterOfCredit.effectiveAmount`

#### Scenario BDD-IMP-ISS-04: Issuance: Facility earmark via effective amount
**US-ISS-01 | REQ-IMP-SPEC-01**
*Type: Happy Path*

* **Given** a Maker creates an LC with `amount = 500,000` and `tolerancePositive = 0.10`
* **When** the limit module queries the facility structure bounds during `execute#IssuancePostAuth`
* **Then** the earmark calculation uses the effective maximum liability:
  | Computed Value | Result |
  | `effectiveAmount × (1 + effectiveTolerancePositive)` | 550,000 USD |
  | Facility earmark requested | 550,000 USD |

#### Scenario BDD-IMP-ISS-05: Issuance: Mandatory cash margin block
**US-ISS-01 | REQ-IMP-SPEC-01**
*Type: Edge Case*

* **Given** the Applicant possesses effectively `$0` Unsecured Facility bounds globally
* **And** `TradeProductCatalog.mandatoryMarginPercent = 100`
* **And** the Maker physically issues LC valuing exactly `$100,000 USD` equivalent
* **When** the Maker engages the authorization submission gateway framework
* **Then** the framework initiates explicit deposit locking natively:
  | Deposit Hold Condition Rules | Assigned Application Output |
  | Applicant Checking Equivalent | Debit Hold Generated |
  | Executed Earmark Total | 100,000 USD Equivalent |

#### Scenario BDD-IMP-ISS-06: Issuance: Effective values initialized on LC
**US-ISS-01 | REQ-IMP-SPEC-01**
*Type: Happy Path*

* **Given** the Checker authorizes an LC issuance with `TradeInstrument.amount = 500,000` and `expiryDate = 2026-12-31`
* **When** `execute#IssuancePostAuth` completes
* **Then** the effective values on `ImportLetterOfCredit` are initialized from `TradeInstrument`:
  | `ImportLetterOfCredit` Field | Value | Source |
  | `effectiveAmount` | 500,000 | `TradeInstrument.amount` |
  | `effectiveExpiryDate` | 2026-12-31 | `TradeInstrument.expiryDate` |
  | `effectiveOutstandingAmount` | 500,000 | `TradeInstrument.amount` |
  | `cumulativeDrawnAmount` | 0 | Initialized |
  | `totalAmendmentCount` | 0 | Initialized |
* **And** `TradeInstrument.amount` remains 500,000 — never updated by subsequent amendments

#### Scenario BDD-IMP-ISS-07: State transition: Invalid transition rejected
**US-ISS-01 | REQ-IMP-STATE-02**
*Type: Edge Case*

* **Given** an `ImportLetterOfCredit` with `businessStateId = LC_DRAFT`
* **When** a service attempts to transition directly to `LC_SETTLED` (skipping intermediate states)
* **Then** `transition#BusinessState` rejects the transition:
  | Attempted Transition | Result |
  | `LC_DRAFT` → `LC_SETTLED` | Blocked: "Invalid state transition" |

#### Scenario BDD-IMP-ISS-08: Vietnam FX regulatory tagging
**US-ISS-01 | REQ-IMP-04**
*Type: Happy Path*

* **Given** an LC Issued specifically by a Vietnam-based branch environment node
* **When** the standard generic component payload fires submission logic routines
* **Then** the module logically extracts specialized variables for the State Bank exclusively:
  | Database Extraction Target | Form Structure Appended |
  | Goods Categorization Code | Mandated / Validated |
  | FX Outflow Report Sequence | Row Instantiated |

#### Scenario BDD-IMP-SWT-01: MT700: X-Character base validation
**US-ISS-01 | REQ-IMP-SWIFT-01, REQ-IMP-SWIFT-05**
*Type: Edge Case*

* **Given** Applicant Details input text containing `@` character
* **When** the MT generator begins parsing via `format#XCharacter`
* **Then** the parser applies SWIFT 'X' character set rules:
  | Input | Output |
  | Contains `@` | Filtered / Replaced / Rejected Exception |
  | Contains `&` | Converted to `AND` |

#### Scenario BDD-IMP-SWT-02: MT700: Mandatory block validation
**US-ISS-01 | REQ-IMP-SWIFT-02**
*Type: Happy Path*

* **Given** a `TradeInstrument` and `ImportLetterOfCredit` with populated SWIFT-mapped fields
* **When** `generate#Mt700` compiles the message via Prowide WIFE
* **Then** mandatory tags are correctly populated from entity fields:
  | Tag | Source Field | Format |
  | 20 (Documentary Credit No.) | `TradeInstrument.transactionRef` | Max 16 chars |
  | 31C (Date of Issue) | `TradeInstrument.issueDate` | `YYMMDD` |
  | 32B (Currency and Amount) | `TradeInstrument.currencyUomId` + `ImportLetterOfCredit.effectiveAmount` | CCY + comma-decimal |

#### Scenario BDD-IMP-SWT-03: MT700: Tolerance output formatter
**US-ISS-01 | REQ-IMP-SWIFT-03A, REQ-IMP-SWIFT-05**
*Type: Edge Case*

* **Given** `ImportLetterOfCredit.effectiveTolerancePositive = 0.05` and `effectiveToleranceNegative = 0.05`
* **When** `format#Tag` processes Tag 39A
* **Then** the output combines tolerance values:
  | Computed Value | Expected |
  | Tag 39A Output | `5/5` |

#### Scenario BDD-IMP-SWT-04: MT700: 'A' Designation swap (59/59A)
**US-ISS-01 | REQ-IMP-SWIFT-04**
*Type: Edge Case*

* **Given** the beneficiary `TradeParty` has `swiftBic` populated (e.g., `BANKUSXX`)
* **When** `format#Tag` processes Tag 59
* **Then** logic switches to Tag 59A format:
  | Tag Used | Output |
  | Tag 59A | `/BANKUSXX` (BIC-based) |
  | Tag 59 | Excluded |

#### Scenario BDD-IMP-SWT-05: MT700: 65-Character splitting with MT 701 continuation
**US-ISS-01 | REQ-IMP-SWIFT-05**
*Type: Edge Case*

* **Given** `ImportLetterOfCredit.goodsDescription` exceeds 100 lines × 65 characters (>6,500 chars)
* **When** `generate#Mt700` processes Tag 45A
* **Then** the service auto-generates an MT 701 continuation:
  | MT 700 Tag 27 | `1/2` |
  | MT 700 Tag 45A | First 100 lines × 65 chars |
  | MT 701 Tag 27 | `2/2` |
  | MT 701 Tag 45B | Overflow content |
  | MT 701 Tag 20 | Must exactly match MT 700 Tag 20 |

---

### Feature 2.2: Amendments

#### Scenario BDD-IMP-AMD-01: Amendment: Financial increase updates effective amount
**US-AMD-01 | REQ-IMP-SPEC-02**
*Type: Happy Path*

* **Given** an `ImportLetterOfCredit` with `effectiveAmount = 50,000`
* **When** operations engages `create#Amendment` with `amountAdjustment = +20,000`
* **And** the amendment is authorized
* **Then** the effective values on `ImportLetterOfCredit` are updated:
  | Field | Before | After |
  | `effectiveAmount` | 50,000 | 70,000 |
  | `effectiveOutstandingAmount` | 50,000 | 70,000 |
  | `totalAmendmentCount` | 0 | 1 |
* **And** `TradeInstrument.amount` remains unchanged at 50,000 (original snapshot)
* **And** Maker/Checker tier routing uses the **new effective liability** of 70,000 (not the 20k delta)

#### Scenario BDD-IMP-AMD-02: Amendment: Negative delta releases limits
**US-AMD-01 | REQ-IMP-SPEC-02**
*Type: Happy Path*

* **Given** an `ImportLetterOfCredit` with `effectiveAmount = 100,000` on `CustomerFacility` "FAC-ACME-001"
* **When** an authorized Checker accepts `amountAdjustment = -15,000`
* **Then** both effective values and facility are updated:
  | Target | Before | After |
  | `ImportLetterOfCredit.effectiveAmount` | 100,000 | 85,000 |
  | `CustomerFacility.utilizedAmount` | Reduced by 15,000 |
  | `TradeInstrument.amount` | 100,000 | 100,000 (unchanged) |

#### Scenario BDD-IMP-AMD-03: Amendment: Non-financial bypasses limits
**US-AMD-01 | REQ-IMP-SPEC-02**
*Type: Happy Path*

* **Given** a Maker only alters `portOfLoading` (non-financial field, `amountAdjustment = 0`)
* **When** `create#Amendment` executes
* **Then** `ImportLcAmendment.isFinancial = N`
* **And** no `LimitServices.update#Utilization` call is made
* **And** `effectiveAmount` and `effectiveOutstandingAmount` remain unchanged
* **And** `totalAmendmentCount` increments by 1

#### Scenario BDD-IMP-AMD-04: Amendment: Pending beneficiary consent
**US-AMD-01 | REQ-IMP-SPEC-02**
*Type: Edge Case*

* **Given** a newly authorized Amendment with `ImportLcAmendment.beneficiaryConsentStatusId = PENDING`
* **And** `SwiftGenerationServices.generate#Mt707` has been dispatched
* **When** subsequent requests attempt to settle drawings against the amended terms
* **Then** the system checks Beneficiary Acknowledgement:
  | Expected Status | Decision |
  | `beneficiaryConsentStatusId` | `PENDING` |
  | Amendment Legally Enforced | False — original terms still govern |

#### Scenario BDD-IMP-AMD-05: Amendment: Version number incremented
**US-AMD-01 | REQ-IMP-SPEC-02**
*Type: Happy Path*

* **Given** an `ImportLetterOfCredit` with `totalAmendmentCount = 0` and `TradeInstrument.versionNumber = 1`
* **When** a financial amendment is authorized
* **Then** versioning fields update:
  | Field | Before | After |
  | `TradeInstrument.versionNumber` | 1 | 2 |
  | `ImportLetterOfCredit.totalAmendmentCount` | 0 | 1 |
  | `ImportLcAmendment.amendmentNumber` | — | 1 (auto-incremented) |

#### Scenario BDD-IMP-AMD-06: MT707: Amendment message generation
**US-AMD-01 | REQ-IMP-SWIFT-06**
*Type: Happy Path*

* **Given** an authorized amendment with `amountAdjustment = +20,000` and `newExpiryDate = 2027-06-30`
* **When** `generate#Mt707` executes
* **Then** only changed fields are populated in the message:
  | Tag | Value | Source |
  | 20 (Sender's Ref) | `TradeInstrument.transactionRef` | Same as parent LC |
  | 30 (Date of Amendment) | `ImportLcAmendment.amendmentDate` | `YYMMDD` format |
  | 32B (Increase) | `USD20000,00` | From `amountAdjustment` |
  | 34B (New Amount) | New `effectiveAmount` | Computed |
  | 31E (New Expiry) | `270630` | From `newExpiryDate` |

---

### Feature 2.3: Document Presentation

#### Scenario BDD-IMP-DOC-01: Presentation: Examination timer enforcement
**US-PRE-01 | REQ-IMP-SPEC-03**
*Type: Happy Path*

* **Given** a `TradeDocumentPresentation` with `presentationDate` captured
* **And** `TradeProductCatalog.documentExamSlaDays = 5` for the parent LC's product
* **When** document controllers generate examination bounds via `calculate#BusinessDate`
* **Then** the computed deadline uses the product-specific SLA days:
  | Field | Value |
  | `TradeDocumentPresentation.regulatoryDeadline` | `presentationDate + 5 banking days` |

#### Scenario BDD-IMP-DOC-02: Presentation: Internal notice on discrepancy
**US-PRE-01 | REQ-IMP-SPEC-03**
*Type: Edge Case*

* **Given** Operations evaluates examination with `PresentationDiscrepancy` records containing ISBP codes
* **When** the Checker authorizes the examination as discrepant
* **Then** the system generates SWIFT messaging and updates state:
  | Action | Output |
  | `generate#Mt750` dispatched | Advice of Discrepancy to Presenting Bank |
  | `ImportLetterOfCredit.businessStateId` | `LC_DISCREPANT` |
  | Applicant Waiver Notice | Sent internally |

#### Scenario BDD-IMP-DOC-03: Presentation: Waiver generates MT 752
**US-PRE-01 | REQ-IMP-SPEC-03**
*Type: Happy Path*

* **Given** an `ImportLetterOfCredit` with `businessStateId = LC_DISCREPANT`
* **And** a `TradeDocumentPresentation` with `applicantDecisionEnumId = PENDING`
* **When** the applicant waives discrepancies via `update#PresentationWaiver` with `applicantDecisionEnumId = WAIVED`
* **Then** the system:
  | Action | Output |
  | `businessStateId` transitions | `LC_DISCREPANT` → `LC_ACCEPTED` |
  | `generate#Mt752` dispatched | Authorization to Pay to Presenting Bank |
  | `TradeDocumentPresentation.applicantDecisionEnumId` | `WAIVED` |

#### Scenario BDD-IMP-DOC-04: State transition: Receive docs
**US-PRE-01 | REQ-IMP-FLOW-04**
*Type: Happy Path*

* **Given** an `ImportLetterOfCredit` with `businessStateId = LC_ISSUED`
* **When** a document packet receipt activates via `create#Presentation`
* **Then** the instrument transitions its business state:
  | Target Domain Metric | Assigned Value |
  | `ImportLetterOfCredit.businessStateId` | `LC_DOCS_RECEIVED` |

#### Scenario BDD-IMP-DOC-05: State transition: Review to discrepant
**US-PRE-01 | REQ-IMP-FLOW-05**
*Type: Edge Case*

* **Given** an `ImportLetterOfCredit` with `businessStateId = LC_DOCS_RECEIVED`
* **When** operations users tag fields with valid ISBP standard codes via `PresentationDiscrepancy` records
* **And** the Checker authorizes the examination with discrepancies flagged
* **Then** the state transitions:
  | Evaluated Flow Node | Resolution Path |
  | `ImportLetterOfCredit.businessStateId` | `LC_DISCREPANT` |
  | Hold Notification Rules | Triggered — applicant waiver notice sent |

#### Scenario BDD-IMP-DOC-06: State transition: Review to clean/accepted
**US-PRE-01 | REQ-IMP-FLOW-06**
*Type: Happy Path*

* **Given** an `ImportLetterOfCredit` with `businessStateId = LC_DOCS_RECEIVED`
* **And** no `PresentationDiscrepancy` records exist for the current presentation
* **When** the Checker submits formal approval
* **Then** the acceptance metrics resolve:
  | Evaluation Vector | Result State Mapping |
  | `ImportLetterOfCredit.businessStateId` | `LC_ACCEPTED` |

#### Scenario BDD-IMP-VAL-01: Drawn tolerance over-draw block
**US-PRE-01 | REQ-IMP-04**
*Type: Edge Case*

* **Given** an `ImportLetterOfCredit` with `effectiveAmount = 10,000` and `effectiveTolerancePositive = 0.10` (max drawable = $11,000)
* **And** `cumulativeDrawnAmount = 0`
* **When** a Presenting Bank claims a drawing of `$11,500`
* **Then** `evaluate#Drawing` validates against effective values:
  | Limit Logic | Computation |
  | Max allowed | `effectiveAmount × (1 + effectiveTolerancePositive)` = 11,000 |
  | `cumulativeDrawnAmount + claimAmount` | 0 + 11,500 = 11,500 |
  | Exceeds Tolerance | True (11,500 > 11,000) |
  | Presentation Saved | False (Blocked) |

#### Scenario BDD-IMP-VAL-02: Late presentation expiry block
**US-PRE-01 | REQ-IMP-04**
*Type: Edge Case*

* **Given** an `ImportLetterOfCredit` with `effectiveExpiryDate = 2026-11-01` (may have been extended by amendment from original `TradeInstrument.expiryDate`)
* **When** operations forces a presentation lodgement with `presentationDate = 2026-11-02`
* **Then** the strict logical evaluation validates against the **effective** expiry date:
  | Date Rules Evaluator | Assertion |
  | `presentationDate > effectiveExpiryDate` | True → Blocked |

#### Scenario BDD-IMP-VAL-03: Auto-reinstatement of revolving LC
**US-PRE-01 | REQ-IMP-04**
*Type: Happy Path*

* **Given** an `ImportLetterOfCredit` with `effectiveAmount = 10,000` and linked `TradeProductCatalog.allowRevolving = Y`
* **And** `effectiveOutstandingAmount = 10,000` and `cumulativeDrawnAmount = 0`
* **When** Operations settles a valid drawing valued at `$10,000`
* **Then** the effective values auto-reinstate:
  | Field | After Settlement |
  | `cumulativeDrawnAmount` | 10,000 |
  | `effectiveOutstandingAmount` | 10,000 (reinstated) |
  | `businessStateId` | `LC_ISSUED` (returned, not closed) |

---

### Feature 2.4: Settlement

#### Scenario BDD-IMP-SET-01: Settlement: Usance future queue mapping
**US-STL-01 | REQ-IMP-SPEC-04**
*Type: Happy Path*

* **Given** a presentation resolves clean with `ImportLetterOfCredit.tenorTypeId = USANCE`
* **And** `usanceDays = 14`
* **When** the initial acceptance phase completes
* **Then** the system generates suspense records:
  | Action | Result |
  | Auto-Pay Execution Queue | Suspended / Inactive Next 14 Days |
  | `generate#Mt732` dispatched | Advice of Discharge (maturity date commitment) |

#### Scenario BDD-IMP-SET-02: Settlement: Nostro entry posting
**US-STL-01 | REQ-IMP-SPEC-04**
*Type: Happy Path*

* **Given** operations executes final settlement via `settle#Presentation` for a Sight LC
* **When** payment evaluation completes
* **Then** the core logic pushes accounting entries:
  | Accounting Node | Assertion |
  | `post#TradeEntry` (LC_SETTLEMENT_PRINCIPAL) | Debit Nostro / Credit Customer |
  | `generate#Mt202` or `generate#Mt103` | Settlement SWIFT dispatched |

#### Scenario BDD-IMP-SET-03: Settlement: Partial draw updates effective outstanding
**US-STL-01 | REQ-IMP-SPEC-04**
*Type: Happy Path*

* **Given** an `ImportLetterOfCredit` with `effectiveAmount = 100,000` and `effectiveOutstandingAmount = 100,000`
* **When** a presentation for `claimAmount = 40,000` is settled
* **Then** the effective values update correctly:
  | Field | Before | After |
  | `effectiveOutstandingAmount` | 100,000 | 60,000 |
  | `cumulativeDrawnAmount` | 0 | 40,000 |
  | `businessStateId` | `LC_ACCEPTED` | `LC_SETTLED` → `LC_ISSUED` (partial — balance remains) |
* **And** `TradeInstrument.outstandingAmount` also reflects 60,000

#### Scenario BDD-IMP-SET-04: State transition: Settled decreases effective outstanding
**US-STL-01 | REQ-IMP-FLOW-07**
*Type: Happy Path*

* **Given** an LC with `businessStateId = LC_ACCEPTED` and `effectiveOutstandingAmount = 500,000`
* **When** settlement executes for the full `claimAmount = 500,000`
* **Then** the effective values update:
  | Field | Before | After |
  | `ImportLetterOfCredit.effectiveOutstandingAmount` | 500,000 | 0 |
  | `ImportLetterOfCredit.cumulativeDrawnAmount` | 0 | 500,000 |
  | `ImportLetterOfCredit.businessStateId` | `LC_ACCEPTED` | `LC_SETTLED` → `LC_CLOSED` (fully drawn) |

---

### Feature 2.5: Shipping Guarantee

#### Scenario BDD-IMP-SG-01: Ship Guar: 110% over-indemnity earmark
**US-SG-01 | REQ-IMP-SPEC-05**
*Type: Edge Case*

* **Given** an SG module targets `invoiceAmount = 50,000 USD`
* **And** `ImportLcShippingGuarantee.liabilityMultiplierRequired = 110`
* **When** the system evaluates limits via `LimitServices.calculate#Earmark`
* **Then** the demand is inflated:
  | Consumed Limit Value | Earmarked Calculation |
  | Required Facility Earmark | $55,000 USD (110% of 50,000) |

#### Scenario BDD-IMP-SG-02: Ship Guar: B/L exchange waiver lock
**US-SG-02 | REQ-IMP-SPEC-05**
*Type: Happy Path*

* **Given** an actively issued `ImportLcShippingGuarantee` with `waiverLockFlag = Y`
* **When** presentation documents arrive with matching `transportDocReference`
* **Then** the system bypasses standard applicant waiver workflow:
  | Evaluation Process | Action |
  | `applicantDecisionEnumId` | Auto-set to `WAIVED` |
  | `ImportLcShippingGuarantee.sgStatusId` | `SG_REDEEMED` |

---

### Feature 2.6: Cancellations

#### Scenario BDD-IMP-CAN-01: Cancellation: End of day auto-expiry flush
**US-CAN-01 | REQ-IMP-SPEC-06**
*Type: Happy Path*

* **Given** an unutilized `ImportLetterOfCredit` with `effectiveExpiryDate = 2026-01-01`
* **And** the system batch operates on `2026-01-16` (expiry + 15 grace days from `TradeConfig.mailDaysGracePeriod`)
* **When** `batch#AutoExpiry` sweeps outstanding instruments
* **Then** the lifecycle transitions and limits release:
  | Output | Value |
  | `ImportLetterOfCredit.businessStateId` | `LC_CLOSED` |
  | Facility earmark | 100% released |

#### Scenario BDD-IMP-CAN-02: Cancellation: Active limit reversal
**US-CAN-01 | REQ-IMP-SPEC-06**
*Type: Happy Path*

* **Given** a Mutual Early Cancellation clears Checker workflows for an LC with `effectiveOutstandingAmount = 500,000`
* **When** `update#Cancellation` completes
* **Then** the limits and effective values update:
  | Field | After |
  | `ImportLetterOfCredit.businessStateId` | `LC_CANCELLED` |
  | `ImportLetterOfCredit.effectiveOutstandingAmount` | 0 |
  | `CustomerFacility.utilizedAmount` | Reduced by 500,000 |

#### Scenario BDD-IMP-CAN-03: State transition: Closed terminates actions
**US-CAN-01 | REQ-IMP-FLOW-08**
*Type: Edge Case*

* **Given** an `ImportLetterOfCredit` with `businessStateId = LC_CLOSED`
* **When** any lifecycle service (amendment, presentation, settlement) is invoked
* **Then** `transition#BusinessState` rejects the transition:
  | Terminal Object Field | Result |
  | `businessStateId` | `LC_CLOSED` (terminal — no outgoing transitions) |
  | Action | Blocked with error: "No transitions permitted from terminal state" |
