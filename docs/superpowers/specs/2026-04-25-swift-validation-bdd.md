# Behavior-Driven Development (BDD) Specification
**Project Name:** Digital Trade Finance Platform
**Module:** SWIFT Validation Infrastructure (Spec A)
**Document Version:** 1.0
**Date:** April 25, 2026
**Traceability:** Maps to BRD `2026-04-25-swift-validation-brd.md` (REQ-SWV-01 through REQ-SWV-06)

---

## 1. Traceability Summary

| User Story (BRD) | Functional Req | Scenario ID | Title | Type |
|:---|:---|:---|:---|:---|
| US-SWV-01 | FR-SWV-01 | BDD-SWV-XCS-01 | X Character Set: Invalid Characters Blocked | Edge Case |
| US-SWV-01 | FR-SWV-01 | BDD-SWV-XCS-02 | X Character Set: Valid Characters Accepted | Happy Path |
| US-SWV-01 | FR-SWV-01 | BDD-SWV-ZCS-01 | Z Character Set: Extended Characters Accepted for Tag 79 | Happy Path |
| US-SWV-01 | FR-SWV-01 | BDD-SWV-ZCS-02 | Z Character Set: Invalid Characters Blocked Even in Z Fields | Edge Case |
| US-SWV-02 | FR-SWV-02 | BDD-SWV-REF-01 | Reference Fields: Slash Rule Violations Blocked | Edge Case |
| US-SWV-02 | FR-SWV-02 | BDD-SWV-REF-02 | Reference Fields: Valid References Accepted | Happy Path |
| US-SWV-02 | FR-SWV-03 | BDD-SWV-AMT-01 | Amount Fields: Exceed 15 Digit Limit Blocked | Edge Case |
| US-SWV-02 | FR-SWV-05 | BDD-SWV-BIC-01 | BIC Fields: Invalid Length Blocked | Edge Case |
| US-SWV-02 | FR-SWV-05 | BDD-SWV-BIC-02 | BIC Fields: Valid 8 and 11 Char BICs Accepted | Happy Path |
| US-SWV-02 | FR-SWV-06 | BDD-SWV-LIN-01 | Line Format: 4x35 Overflow Blocked | Edge Case |
| US-SWV-02 | FR-SWV-06 | BDD-SWV-LIN-02 | Line Format: 100x65 Narrative Within Limits Accepted | Happy Path |
| US-SWV-02 | FR-SWV-06 | BDD-SWV-LIN-03 | Line Format: 35x50 Z-Charset Narrative Within Limits | Happy Path |
| US-SWV-03 | FR-SWV-07 | BDD-SWV-MEX-01 | Mutual Exclusion: Tolerance and Max Credit Amount | Edge Case |
| US-SWV-03 | FR-SWV-07 | BDD-SWV-MEX-02 | Mutual Exclusion: Shipment Date and Shipment Period | Edge Case |
| US-SWV-03 | FR-SWV-07 | BDD-SWV-MEX-03 | Mutual Exclusion: Bank BIC and Bank Name/Address | Edge Case |
| US-SWV-04 | FR-SWV-08 | BDD-SWV-CND-01 | Conditional: Usance Fields Required When Tenor Not Sight | Edge Case |
| US-SWV-04 | FR-SWV-08 | BDD-SWV-CND-02 | Conditional: Mixed Payment Details Required for Mixed Tenor | Edge Case |
| US-SWV-04 | FR-SWV-08 | BDD-SWV-CND-03 | Conditional: Document Disposal Required on Refusal | Edge Case |
| US-SWV-05 | FR-SWV-09 | BDD-SWV-SVC-01 | Validation Service: Comprehensive Multi-Field Error Report | Happy Path |
| US-SWV-05 | FR-SWV-09 | BDD-SWV-SVC-02 | Validation Service: Clean Data Returns No Errors | Happy Path |
| US-SWV-05 | FR-SWV-09 | BDD-SWV-SVC-03 | Validation Service: Amendment Entity Z-Charset Validation | Happy Path |
| US-SWV-05 | FR-SWV-09 | BDD-SWV-SVC-04 | Validation Service: Presentation Entity Cross-MT Validation | Happy Path |
| US-SWV-05 | FR-SWV-09 | BDD-SWV-SVC-05 | Validation Service: Settlement Entity Validation | Happy Path |

---

## 2. Detailed BDD Scenarios

### Feature: SWIFT X Character Set Validation (FR-SWV-01)

#### Scenario BDD-SWV-XCS-01: X Character Set — Invalid Characters Blocked
**User Story:** US-SWV-01
**Functional Requirement:** FR-SWV-01
**Type:** Edge Case

* **Given** an `ImportLetterOfCredit` with the following SWIFT-bound field values:
  | Field | Value |
  | `goodsDescription` | `STEEL PIPES @100MM & FITTINGS` |
  | `portOfLoading` | `HO CHI MINH CITY #1 PORT` |
* **When** `validate#SwiftFields` is called with `entityType = ImportLetterOfCredit`
* **Then** the service returns field-level errors:
  | Field | Error |
  | `goodsDescription` | `Invalid X charset character '@' at position 13` |
  | `goodsDescription` | `Invalid X charset character '&' at position 19` |
  | `portOfLoading` | `Invalid X charset character '#' at position 19` |
* **And** no data is persisted until errors are corrected

#### Scenario BDD-SWV-XCS-02: X Character Set — Valid Characters Accepted
**User Story:** US-SWV-01
**Functional Requirement:** FR-SWV-01
**Type:** Happy Path

* **Given** an `ImportLetterOfCredit` with the following SWIFT-bound field values:
  | Field | Value |
  | `goodsDescription` | `STEEL PIPES 100MM, GRADE A (STANDARD)` |
  | `portOfLoading` | `HO CHI MINH CITY` |
  | `documentsRequired` | `1/3 ORIGINAL BILL OF LADING - CLEAN ON BOARD` |
* **When** `validate#SwiftFields` is called with `entityType = ImportLetterOfCredit`
* **Then** no character set validation errors are returned for these fields

---

### Feature: SWIFT Z Character Set Validation (FR-SWV-01)

#### Scenario BDD-SWV-ZCS-01: Z Character Set — Extended Characters Accepted for Tag 79
**User Story:** US-SWV-01
**Functional Requirement:** FR-SWV-01
**Type:** Happy Path

* **Given** an `ImportLcAmendment` with:
  | Field | Value |
  | `amendmentNarrative` | `PLEASE NOTE: AMOUNT INCREASED @ USD 20,000 = TOTAL 70,000` |
* **When** `validate#SwiftFields` is called with `entityType = ImportLcAmendment`
* **Then** no character set errors are returned (Z charset permits `@`, `=`)

#### Scenario BDD-SWV-ZCS-02: Z Character Set — Invalid Characters Blocked Even in Z Fields
**User Story:** US-SWV-01
**Functional Requirement:** FR-SWV-01
**Type:** Edge Case

* **Given** an `ImportLcAmendment` with:
  | Field | Value |
  | `amendmentNarrative` | `AMOUNT UPDATED {SEE ATTACHED}` |
* **When** `validate#SwiftFields` is called with `entityType = ImportLcAmendment`
* **Then** the service returns:
  | Field | Error |
  | `amendmentNarrative` | `Invalid Z charset character '{' at position 17` |
  | `amendmentNarrative` | `Invalid Z charset character '}' at position 30` |

---

### Feature: Reference Field Slash Rules (FR-SWV-02)

#### Scenario BDD-SWV-REF-01: Reference Fields — Slash Rule Violations Blocked
**User Story:** US-SWV-02
**Functional Requirement:** FR-SWV-02
**Type:** Edge Case

* **Given** a `TradeInstrument` with the following reference fields:
  | Field | Value | Violation |
  | `transactionRef` | `/TF-IMP-26-0001` | Starts with `/` |
  | `preAdviceRef` | `REF//2026/001` | Contains `//` |
* **When** `validate#SwiftFields` is called with `entityType = TradeInstrument`
* **Then** the service returns:
  | Field | Error |
  | `transactionRef` | `Reference field must not start with '/'` |
  | `preAdviceRef` | `Reference field must not contain '//'` |

#### Scenario BDD-SWV-REF-02: Reference Fields — Valid References Accepted
**User Story:** US-SWV-02
**Functional Requirement:** FR-SWV-02
**Type:** Happy Path

* **Given** a `TradeInstrument` with:
  | Field | Value |
  | `transactionRef` | `TF-IMP-26-0001` |
  | `preAdviceRef` | `PA-2026/001` |
* **When** `validate#SwiftFields` is called with `entityType = TradeInstrument`
* **Then** no slash rule errors are returned

---

### Feature: Amount Format Validation (FR-SWV-03)

#### Scenario BDD-SWV-AMT-01: Amount Fields — Exceed 15 Digit Limit Blocked
**User Story:** US-SWV-02
**Functional Requirement:** FR-SWV-03
**Type:** Edge Case

* **Given** a `TradeInstrument` with `amount = 9999999999999.99` (16 digits when formatted as `9999999999999,99`)
* **When** `validate#SwiftFields` is called with `entityType = TradeInstrument`
* **Then** the service returns:
  | Field | Error |
  | `amount` | `SWIFT amount exceeds 15-digit maximum when formatted` |

---

### Feature: BIC Validation (FR-SWV-05)

#### Scenario BDD-SWV-BIC-01: BIC Fields — Invalid Length Blocked
**User Story:** US-SWV-02
**Functional Requirement:** FR-SWV-05
**Type:** Edge Case

* **Given** a `TradeInstrument` with:
  | Field | Value | Issue |
  | `reimbursingBankBic` | `JPMC` | Too short (4 chars) |
* **And** a `TradeDocumentPresentation` with:
  | Field | Value | Issue |
  | `presentingBankBic` | `BNPAFRPPXX99Z` | Too long (13 chars) |
* **When** `validate#SwiftFields` is called on each entity
* **Then** the service returns for each:
  | Entity | Field | Error |
  | `TradeInstrument` | `reimbursingBankBic` | `SWIFT BIC must be exactly 8 or 11 alphanumeric characters` |
  | `TradeDocumentPresentation` | `presentingBankBic` | `SWIFT BIC must be exactly 8 or 11 alphanumeric characters` |

#### Scenario BDD-SWV-BIC-02: BIC Fields — Valid 8 and 11 Char BICs Accepted
**User Story:** US-SWV-02
**Functional Requirement:** FR-SWV-05
**Type:** Happy Path

* **Given** a `TradeInstrument` with:
  | Field | Value | Length |
  | `reimbursingBankBic` | `CHASUS33` | 8 chars |
  | `adviseThroughBankBic` | `BNPAFRPPXXX` | 11 chars |
* **When** `validate#SwiftFields` is called with `entityType = TradeInstrument`
* **Then** no BIC validation errors are returned

---

### Feature: Line Format Validation (FR-SWV-06)

#### Scenario BDD-SWV-LIN-01: Line Format — 4x35 Overflow Blocked
**User Story:** US-SWV-02
**Functional Requirement:** FR-SWV-06
**Type:** Edge Case

* **Given** a `TradeParty` (Applicant) with:
  | Field | Value |
  | `registeredAddress` | 5 lines of 35 characters each (175 chars with line breaks) |
* **When** `validate#SwiftFields` is called with `entityType = TradeParty`
* **Then** the service returns:
  | Field | Error |
  | `registeredAddress` | `Exceeds SWIFT 4x35 format: maximum 4 lines of 35 characters` |

#### Scenario BDD-SWV-LIN-02: Line Format — 100x65 Narrative Within Limits Accepted
**User Story:** US-SWV-02
**Functional Requirement:** FR-SWV-06
**Type:** Happy Path

* **Given** an `ImportLetterOfCredit` with:
  | Field | Value |
  | `goodsDescription` | 50 lines × 65 characters of valid X charset text (3,250 chars) |
* **When** `validate#SwiftFields` is called with `entityType = ImportLetterOfCredit`
* **Then** no line format errors are returned for `goodsDescription`

#### Scenario BDD-SWV-LIN-03: Line Format — 35x50 Z-Charset Narrative Within Limits
**User Story:** US-SWV-02
**Functional Requirement:** FR-SWV-06
**Type:** Happy Path

* **Given** an `ImportLcAmendment` with:
  | Field | Value |
  | `amendmentNarrative` | 10 lines × 50 characters of valid Z charset text (500 chars) |
* **When** `validate#SwiftFields` is called with `entityType = ImportLcAmendment`
* **Then** no line format errors are returned for `amendmentNarrative`

---

### Feature: Mutual Exclusion Rules (FR-SWV-07)

#### Scenario BDD-SWV-MEX-01: Mutual Exclusion — Tolerance and Max Credit Amount
**User Story:** US-SWV-03
**Functional Requirement:** FR-SWV-07
**Type:** Edge Case

* **Given** an `ImportLetterOfCredit` with:
  | Field | Value |
  | `tolerancePositive` | `0.10` |
  | `toleranceNegative` | `0.05` |
  | `maxCreditAmountFlag` | `Y` |
* **When** `validate#SwiftFields` is called with `entityType = ImportLetterOfCredit`
* **Then** the service returns:
  | Field | Error |
  | `maxCreditAmountFlag` | `Tag 39B (Max Credit Amount) is mutually exclusive with Tag 39A (Tolerance). Clear tolerance fields or set maxCreditAmountFlag to N` |

#### Scenario BDD-SWV-MEX-02: Mutual Exclusion — Shipment Date and Shipment Period
**User Story:** US-SWV-03
**Functional Requirement:** FR-SWV-07
**Type:** Edge Case

* **Given** an `ImportLetterOfCredit` with:
  | Field | Value |
  | `latestShipmentDate` | `2026-12-15` |
  | `shipmentPeriodText` | `WITHIN 30 DAYS AFTER ISSUANCE` |
* **When** `validate#SwiftFields` is called with `entityType = ImportLetterOfCredit`
* **Then** the service returns:
  | Field | Error |
  | `shipmentPeriodText` | `Tag 44D (Shipment Period) is mutually exclusive with Tag 44C (Latest Shipment Date). Use one or the other` |

#### Scenario BDD-SWV-MEX-03: Mutual Exclusion — Bank BIC and Bank Name/Address
**User Story:** US-SWV-03
**Functional Requirement:** FR-SWV-07
**Type:** Edge Case

* **Given** a `TradeInstrument` with:
  | Field | Value |
  | `reimbursingBankBic` | `CHASUS33` |
  | `reimbursingBankName` | `JP MORGAN CHASE NEW YORK` |
* **When** `validate#SwiftFields` is called with `entityType = TradeInstrument`
* **Then** the service returns:
  | Field | Error |
  | `reimbursingBankName` | `Tag 53D (Name/Address) is mutually exclusive with Tag 53A (BIC). Use BIC format or Name/Address, not both` |

---

### Feature: Conditional Field Requirements (FR-SWV-08)

#### Scenario BDD-SWV-CND-01: Conditional — Usance Fields Required When Tenor Not Sight
**User Story:** US-SWV-04
**Functional Requirement:** FR-SWV-08
**Type:** Edge Case

* **Given** an `ImportLetterOfCredit` with:
  | Field | Value |
  | `tenorTypeId` | `USANCE` |
  | `usanceDays` | (empty) |
  | `usanceBaseDate` | (empty) |
* **When** `validate#SwiftFields` is called with `entityType = ImportLetterOfCredit`
* **Then** the service returns:
  | Field | Error |
  | `usanceDays` | `Required when tenor is not SIGHT (Tag 42C source)` |
  | `usanceBaseDate` | `Required when tenor is not SIGHT (Tag 42C source)` |

#### Scenario BDD-SWV-CND-02: Conditional — Mixed Payment Details Required for Mixed Tenor
**User Story:** US-SWV-04
**Functional Requirement:** FR-SWV-08
**Type:** Edge Case

* **Given** an `ImportLetterOfCredit` with:
  | Field | Value |
  | `tenorTypeId` | `MIXED` |
  | `mixedPaymentDetails` | (empty) |
* **When** `validate#SwiftFields` is called with `entityType = ImportLetterOfCredit`
* **Then** the service returns:
  | Field | Error |
  | `mixedPaymentDetails` | `Required when tenor is MIXED (Tag 42M source)` |

#### Scenario BDD-SWV-CND-03: Conditional — Document Disposal Required on Refusal
**User Story:** US-SWV-04
**Functional Requirement:** FR-SWV-08
**Type:** Edge Case

* **Given** a `TradeDocumentPresentation` with:
  | Field | Value |
  | `applicantDecisionEnumId` | `REFUSED` |
  | `documentDisposalEnumId` | (empty) |
* **When** `validate#SwiftFields` is called with `entityType = TradeDocumentPresentation`
* **Then** the service returns:
  | Field | Error |
  | `documentDisposalEnumId` | `Required when applicant refuses documents (Tag 77B source for MT734)` |

---

### Feature: Comprehensive Validation Service (FR-SWV-09)

#### Scenario BDD-SWV-SVC-01: Validation Service — Comprehensive Multi-Field Error Report
**User Story:** US-SWV-05
**Functional Requirement:** FR-SWV-09
**Type:** Happy Path

* **Given** an `ImportLetterOfCredit` with multiple violations:
  | Field | Value | Violation Type |
  | `goodsDescription` | `PIPES @100MM` | X charset |
  | `portOfLoading` | (101 characters) | Exceeds 65-char max |
  | `tolerancePositive` | `0.10` | — |
  | `maxCreditAmountFlag` | `Y` | Mutual exclusion with tolerance |
  | `tenorTypeId` | `USANCE` | — |
  | `usanceDays` | (empty) | Conditional required |
* **When** `validate#SwiftFields` is called with `entityType = ImportLetterOfCredit`
* **Then** the service returns ALL errors in a single response (not fail-fast):
  | Field | Error Type |
  | `goodsDescription` | X charset violation |
  | `portOfLoading` | Length violation |
  | `maxCreditAmountFlag` | Mutual exclusion |
  | `usanceDays` | Conditional required |
* **And** the response contains at least 4 distinct field-level error entries

#### Scenario BDD-SWV-SVC-02: Validation Service — Clean Data Returns No Errors
**User Story:** US-SWV-05
**Functional Requirement:** FR-SWV-09
**Type:** Happy Path

* **Given** an `ImportLetterOfCredit` with all SWIFT-bound fields populated with valid data:
  | Field | Value |
  | `goodsDescription` | `STEEL PIPES, GRADE A, AS PER PROFORMA INV 2026-001` |
  | `documentsRequired` | `FULL SET OF CLEAN ON BOARD BILL OF LADING` |
  | `portOfLoading` | `HO CHI MINH CITY` |
  | `portOfDischarge` | `TOKYO, JAPAN` |
  | `expiryPlace` | `VIETNAM` |
  | `tenorTypeId` | `SIGHT` |
* **And** a `TradeInstrument` with:
  | Field | Value |
  | `transactionRef` | `TF-IMP-26-0001` |
  | `currencyUomId` | `USD` |
  | `amount` | `500000` |
* **When** `validate#SwiftFields` is called for both entities
* **Then** no validation errors are returned for either entity

#### Scenario BDD-SWV-SVC-03: Validation Service — Amendment Entity Z-Charset Validation
**User Story:** US-SWV-05
**Functional Requirement:** FR-SWV-09
**Type:** Happy Path

* **Given** an `ImportLcAmendment` with valid fields:
  | Field | Value | Constraint Applied |
  | `amendmentNarrative` | `INCREASE BY USD 20,000 @ BENEFICIARY REQUEST` | Z charset (35×50) |
  | `amendmentDate` | `2026-06-15` | YYMMDD |
  | `amountAdjustment` | `20000` | 15-digit limit |
* **When** `validate#SwiftFields` is called with `entityType = ImportLcAmendment`
* **Then** no validation errors are returned

#### Scenario BDD-SWV-SVC-04: Validation Service — Presentation Entity Cross-MT Validation
**User Story:** US-SWV-05
**Functional Requirement:** FR-SWV-09
**Type:** Happy Path

* **Given** a `TradeDocumentPresentation` with:
  | Field | Value | Used In |
  | `presentingBankBic` | `BNPAFRPP` | MT750/MT734/MT752/MT732 headers |
  | `presentingBankRef` | `PRES-2026-042` | MT750 Tag 21, MT734 Tag 21, MT752 Tag 21, MT732 Tag 21 |
  | `claimAmount` | `100000` | MT750 Tag 32B, MT734 Tag 32A |
  | `claimCurrency` | `USD` | MT750 Tag 32B, MT734 Tag 32A |
* **When** `validate#SwiftFields` is called with `entityType = TradeDocumentPresentation`
* **Then** no validation errors are returned
* **And** all fields are confirmed compliant for MT750, MT734, MT752, and MT732 generation

#### Scenario BDD-SWV-SVC-05: Validation Service — Settlement Entity Validation
**User Story:** US-SWV-05
**Functional Requirement:** FR-SWV-09
**Type:** Happy Path

* **Given** an `ImportLcSettlement` with:
  | Field | Value | Used In |
  | `principalAmount` | `100000` | MT202 Tag 32A, MT103 Tag 32A |
  | `remittanceCurrency` | `USD` | MT202 Tag 32A, MT103 Tag 32A |
  | `valueDate` | `2026-07-15` | MT202 Tag 32A, MT103 Tag 32A |
  | `chargesDetailEnumId` | `SHA` | MT103 Tag 71A |
  | `maturityDate` | `2026-08-15` | MT732 Tag 32A |
* **When** `validate#SwiftFields` is called with `entityType = ImportLcSettlement`
* **Then** no validation errors are returned
* **And** all fields are confirmed compliant for MT202, MT103, and MT732 generation
