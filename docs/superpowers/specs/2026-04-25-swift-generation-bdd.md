# Behavior-Driven Development (BDD) Specification
**Project Name:** Digital Trade Finance Platform
**Module:** SWIFT Message Generation Services (Spec B)
**Document Version:** 1.0
**Date:** April 25, 2026
**Prerequisite:** Spec A BDD — SWIFT Validation Infrastructure
**Traceability:** Maps to BRD `2026-04-25-swift-generation-brd.md` (REQ-SWG-01 through REQ-SWG-05)

---

## 1. Traceability Summary

| User Story | Functional Req | Scenario ID | Title | Type |
|:---|:---|:---|:---|:---|
| US-SWG-01 | FR-SWG-01, FR-SWG-05 | BDD-SWG-700-01 | MT700: Full Tag Assembly from Entity Data | Happy Path |
| US-SWG-01 | FR-SWG-02, FR-SWG-05 | BDD-SWG-700-02 | MT700: Optional Tags Populated When Data Exists | Happy Path |
| US-SWG-01 | FR-SWG-05 | BDD-SWG-700-03 | MT700: BIC Toggle — 59A vs 59 | Edge Case |
| US-SWG-01 | FR-SWG-05 | BDD-SWG-700-04 | MT700: SWIFT Header Block Construction | Happy Path |
| US-SWG-02 | FR-SWG-06 | BDD-SWG-701-01 | MT701: Auto-Continuation for Overflow Narratives | Edge Case |
| US-SWG-03 | FR-SWG-07 | BDD-SWG-707-01 | MT707: Financial Amendment — Increase with 32B/34B | Happy Path |
| US-SWG-03 | FR-SWG-07 | BDD-SWG-707-02 | MT707: Financial Amendment — Decrease with 33B/34B | Happy Path |
| US-SWG-03 | FR-SWG-07 | BDD-SWG-707-03 | MT707: Non-Financial Amendment — Narrative Only | Edge Case |
| US-SWG-04 | FR-SWG-08 | BDD-SWG-750-01 | MT750: Discrepancy Advice with Joined Discrepancy Records | Happy Path |
| US-SWG-04 | FR-SWG-09 | BDD-SWG-734-01 | MT734: Notice of Refusal with Document Disposal | Happy Path |
| US-SWG-04 | FR-SWG-10 | BDD-SWG-752-01 | MT752: Authorization to Pay after Waiver | Happy Path |
| US-SWG-05 | FR-SWG-11 | BDD-SWG-732-01 | MT732: Advice of Discharge for Usance LC | Happy Path |
| US-SWG-06 | FR-SWG-12 | BDD-SWG-799-01 | MT799: Cancellation Request Narrative | Happy Path |
| US-SWG-07 | FR-SWG-13 | BDD-SWG-202-01 | MT202: Bank-to-Bank Settlement Transfer | Happy Path |
| US-SWG-07 | FR-SWG-14 | BDD-SWG-103-01 | MT103: Customer Credit Transfer with Charges | Happy Path |
| US-SWG-08 | FR-SWG-03 | BDD-SWG-L2V-01 | Layer 2: Generation Aborted on Invalid Data | Edge Case |
| US-SWG-08 | FR-SWG-03 | BDD-SWG-L2V-02 | Layer 2: Auto-Conversion with Warning Log | Edge Case |
| US-SWG-09 | FR-SWG-04 | BDD-SWG-PER-01 | Persistence: SwiftMessage Record Created | Happy Path |

---

## 2. Detailed BDD Scenarios

### Feature: MT700 — Issue of a Documentary Credit (FR-SWG-05)

#### Scenario BDD-SWG-700-01: MT700 — Full Tag Assembly from Entity Data
**User Story:** US-SWG-01
**Functional Requirement:** FR-SWG-01, FR-SWG-05
**Type:** Happy Path

* **Given** a `TradeInstrument` with:
  | Field | Value |
  | `transactionRef` | `TF-IMP-26-0001` |
  | `currencyUomId` | `USD` |
  | `amount` | `500000` |
  | `issueDate` | `2026-06-15` |
  | `expiryDate` | `2026-12-31` |
* **And** an `ImportLetterOfCredit` with:
  | Field | Value |
  | `effectiveAmount` | `500000` |
  | `expiryPlace` | `VIETNAM` |
  | `lcTypeEnumId` | `IRREVOCABLE` |
  | `tenorTypeId` | `SIGHT` |
  | `goodsDescription` | `STEEL PIPES, GRADE A, 100MM DIAMETER` |
  | `documentsRequired` | `FULL SET OF CLEAN ON BOARD BILL OF LADING` |
  | `confirmationEnumId` | `WITHOUT` |
  | `partialShipmentEnumId` | `ALLOWED` |
  | `transhipmentEnumId` | `NOT_ALLOWED` |
* **And** an Applicant `TradeParty` with `partyName = ACME IMPORT CO` and `registeredAddress = 123 MAIN ST, HO CHI MINH CITY`
* **And** a Beneficiary `TradeParty` with `partyName = GLOBAL STEEL LTD` and `registeredAddress = 456 HARBOUR RD, TOKYO`
* **When** `generate#Mt700` is called with `instrumentId`
* **Then** the generated message contains all mandatory tags with correctly formatted data:
  | Tag | Expected Content |
  | 27 | `1/1` |
  | 40A | `IRREVOCABLE` |
  | 20 | `TF-IMP-26-0001` |
  | 31C | `260615` |
  | 31D | `261231VIETNAM` |
  | 50 | `ACME IMPORT CO\r\n123 MAIN ST, HO CHI MINH CITY` |
  | 59 | `GLOBAL STEEL LTD\r\n456 HARBOUR RD, TOKYO` |
  | 32B | `USD500000,00` |
  | 45A | `STEEL PIPES, GRADE A, 100MM DIAMETER` |
  | 46A | `FULL SET OF CLEAN ON BOARD BILL OF LADING` |
  | 49 | `WITHOUT` |
  | 43P | `ALLOWED` |
  | 43T | `NOT ALLOWED` |

#### Scenario BDD-SWG-700-02: MT700 — Optional Tags Populated When Data Exists
**User Story:** US-SWG-01
**Functional Requirement:** FR-SWG-02, FR-SWG-05
**Type:** Happy Path

* **Given** an `ImportLetterOfCredit` with optional fields populated:
  | Field | Value |
  | `tolerancePositive` | `0.10` |
  | `toleranceNegative` | `0.05` |
  | `portOfLoading` | `HO CHI MINH CITY` |
  | `portOfDischarge` | `TOKYO, JAPAN` |
  | `latestShipmentDate` | `2026-11-30` |
  | `additionalConditions` | `ALL DOCUMENTS MUST BE IN ENGLISH` |
  | `bankToBankInstructions` | `UPON CLEAN PRESENTATION CLAIM REIMBURSEMENT` |
* **And** a `TradeInstrument` with `reimbursingBankBic = CHASUS33`
* **When** `generate#Mt700` is called with `instrumentId`
* **Then** the generated message includes the optional tags:
  | Tag | Expected Content |
  | 39A | `10/05` |
  | 44E | `HO CHI MINH CITY` |
  | 44F | `TOKYO, JAPAN` |
  | 44C | `261130` |
  | 47A | `ALL DOCUMENTS MUST BE IN ENGLISH` |
  | 53A | `CHASUS33` |
  | 78 | `UPON CLEAN PRESENTATION CLAIM REIMBURSEMENT` |
* **And** tags for NULL optional fields (e.g., 44A, 44B, 42M) are NOT present in the message

#### Scenario BDD-SWG-700-03: MT700 — BIC Toggle: 59A vs 59
**User Story:** US-SWG-01
**Functional Requirement:** FR-SWG-05
**Type:** Edge Case

* **Given** a Beneficiary `TradeParty` with `swiftBic = BANKJPJT` and `partyName = GLOBAL STEEL LTD`
* **When** `generate#Mt700` processes Tag 59
* **Then** the output uses Tag 59A (BIC-based format):
  | Tag Used | Content |
  | 59A | `/BANKJPJT\r\nGLOBAL STEEL LTD` |
* **And** Tag 59 (name-only format) is NOT present

#### Scenario BDD-SWG-700-04: MT700 — SWIFT Header Block Construction
**User Story:** US-SWG-01
**Functional Requirement:** FR-SWG-01
**Type:** Happy Path

* **Given** `TradeConfig.ISSUING_BANK_BIC = VIETBANK1`
* **And** the Advising Bank BIC (recipient) is `BNPAFRPP`
* **When** `generate#Mt700` constructs the SWIFT message
* **Then** the message starts with proper SWIFT blocks:
  | Block | Content Pattern |
  | Block 1 (Basic Header) | `{1:F01VIETBANK1XXXX...}` |
  | Block 2 (Application Header) | `{2:I700BNPAFRPPXXXXN}` |
  | Block 4 (Text Block) | Contains all tags between `{4:\r\n` and `-}` |

---

### Feature: MT701 — Continuation Message (FR-SWG-06)

#### Scenario BDD-SWG-701-01: MT701 — Auto-Continuation for Overflow Narratives
**User Story:** US-SWG-02
**Functional Requirement:** FR-SWG-06
**Type:** Edge Case

* **Given** an `ImportLetterOfCredit` with `goodsDescription` exceeding 6,500 characters (100 lines × 65 chars)
* **When** `generate#Mt700` processes Tag 45A
* **Then** two SWIFT messages are generated:
  | Message | Tag 27 | Content |
  | MT700 | `1/2` | First 100 lines of `goodsDescription` in Tag 45A |
  | MT701 | `2/2` | Remaining text in Tag 45B |
* **And** MT701 Tag 20 exactly matches MT700 Tag 20 (`TF-IMP-26-0001`)

---

### Feature: MT707 — Amendment (FR-SWG-07)

#### Scenario BDD-SWG-707-01: MT707 — Financial Increase with 32B/34B
**User Story:** US-SWG-03
**Functional Requirement:** FR-SWG-07
**Type:** Happy Path

* **Given** an `ImportLcAmendment` with:
  | Field | Value |
  | `amendmentDate` | `2026-07-15` |
  | `amendmentNumber` | `1` |
  | `amountAdjustment` | `20000` |
* **And** `ImportLetterOfCredit.effectiveAmount = 520000` (after amendment)
* **And** `TradeInstrument.currencyUomId = USD`
* **When** `generate#Mt707` is called
* **Then** the message contains:
  | Tag | Content |
  | 30 | `260715` |
  | 26E | `1` |
  | 32B | `USD20000,00` (increase) |
  | 34B | `USD520000,00` (new total) |
* **And** Tag 33B is NOT present (no decrease)

#### Scenario BDD-SWG-707-02: MT707 — Financial Decrease with 33B/34B
**User Story:** US-SWG-03
**Functional Requirement:** FR-SWG-07
**Type:** Happy Path

* **Given** an `ImportLcAmendment` with `amountAdjustment = -15000`
* **And** `ImportLetterOfCredit.effectiveAmount = 485000` (after amendment)
* **When** `generate#Mt707` is called
* **Then** the message contains:
  | Tag | Content |
  | 33B | `USD15000,00` (decrease — absolute value) |
  | 34B | `USD485000,00` (new total) |
* **And** Tag 32B is NOT present (no increase)

#### Scenario BDD-SWG-707-03: MT707 — Non-Financial Narrative Only
**User Story:** US-SWG-03
**Functional Requirement:** FR-SWG-07
**Type:** Edge Case

* **Given** an `ImportLcAmendment` with `amountAdjustment = 0`, `newExpiryDate = null`
* **And** `amendmentNarrative = PORT OF LOADING CHANGED FROM SAIGON TO HO CHI MINH CITY`
* **When** `generate#Mt707` is called
* **Then** the message contains:
  | Tag | Content |
  | 79 | `PORT OF LOADING CHANGED FROM SAIGON TO HO CHI MINH CITY` |
* **And** Tags 32B, 33B, 34B, 31E are NOT present

---

### Feature: MT750 — Advice of Discrepancy (FR-SWG-08)

#### Scenario BDD-SWG-750-01: MT750 — Discrepancy Advice with Joined Records
**User Story:** US-SWG-04
**Functional Requirement:** FR-SWG-08
**Type:** Happy Path

* **Given** a `TradeDocumentPresentation` with `presentingBankRef = PRES-2026-042`, `claimAmount = 100000`, `claimCurrency = USD`
* **And** two `PresentationDiscrepancy` records:
  | Code | Description |
  | `D001` | `LATE SHIPMENT - SHIPPED AFTER LATEST DATE` |
  | `D002` | `BILL OF LADING NOT ENDORSED IN BLANK` |
* **When** `generate#Mt750` is called
* **Then** the message contains:
  | Tag | Content |
  | 20 | `TF-IMP-26-0001` |
  | 21 | `PRES-2026-042` |
  | 32B | `USD100000,00` |
  | 77J | Contains both discrepancy codes and descriptions, auto-wrapped at 50 chars/line |

---

### Feature: MT734 — Notice of Refusal (FR-SWG-09)

#### Scenario BDD-SWG-734-01: MT734 — Notice of Refusal with Document Disposal
**User Story:** US-SWG-04
**Functional Requirement:** FR-SWG-09
**Type:** Happy Path

* **Given** a `TradeDocumentPresentation` with `applicantDecisionEnumId = REFUSED`, `documentDisposalEnumId = HOLDING_DOCUMENTS`
* **And** `PresentationDiscrepancy` records exist
* **When** `generate#Mt734` is called
* **Then** the message contains:
  | Tag | Content |
  | 32A | `260715USD100000,00` (presentation date + amount) |
  | 77J | Concatenated discrepancy details |
  | 77B | `HOLDING DOCUMENTS AT YOUR DISPOSAL` |

---

### Feature: MT752 — Authorization to Pay (FR-SWG-10)

#### Scenario BDD-SWG-752-01: MT752 — Authorization after Waiver
**User Story:** US-SWG-04
**Functional Requirement:** FR-SWG-10
**Type:** Happy Path

* **Given** a waived presentation with `claimAmount = 100000`, `claimCurrency = USD`
* **When** `generate#Mt752` is called
* **Then** the message contains:
  | Tag | Content |
  | 20 | `TF-IMP-26-0001` |
  | 21 | `PRES-2026-042` |
  | 30 | System business date in YYMMDD |
  | 32B | `USD100000,00` |

---

### Feature: MT732 — Advice of Discharge (FR-SWG-11)

#### Scenario BDD-SWG-732-01: MT732 — Advice of Discharge for Usance LC
**User Story:** US-SWG-05
**Functional Requirement:** FR-SWG-11
**Type:** Happy Path

* **Given** a Usance LC presentation accepted with maturity date `2026-09-15`
* **And** `claimAmount = 100000`, `claimCurrency = USD`
* **When** `generate#Mt732` is called
* **Then** the message contains:
  | Tag | Content |
  | 20 | `TF-IMP-26-0001` |
  | 30 | System business date in YYMMDD |
  | 32A | `260915USD100000,00` (maturity date — future date) |
* **And** Tag 32A date is the future maturity date, NOT the current date

---

### Feature: MT799 — Free Format Message (FR-SWG-12)

#### Scenario BDD-SWG-799-01: MT799 — Cancellation Request Narrative
**User Story:** US-SWG-06
**Functional Requirement:** FR-SWG-12
**Type:** Happy Path

* **Given** an early cancellation request for LC `TF-IMP-26-0001`
* **When** `generate#Mt799` is called
* **Then** the message contains:
  | Tag | Content |
  | 20 | `TF-IMP-26-0001` |
  | 21 | `NONREF` (or advising bank ref if available) |
  | 79 | System-generated narrative requesting beneficiary consent, auto-wrapped at 50 chars/line, Z charset |

---

### Feature: MT202 — Bank-to-Bank Transfer (FR-SWG-13)

#### Scenario BDD-SWG-202-01: MT202 — Bank-to-Bank Settlement Transfer
**User Story:** US-SWG-07
**Functional Requirement:** FR-SWG-13
**Type:** Happy Path

* **Given** an `ImportLcSettlement` with `valueDate = 2026-07-20`, `principalAmount = 100000`, `remittanceCurrency = USD`
* **And** `TradeDocumentPresentation.presentingBankBic = BNPAFRPP`
* **And** `TradeConfig.NOSTRO_ACCOUNT_USD = 001-234567-USD`
* **When** `generate#Mt202` is called
* **Then** the message contains:
  | Tag | Content |
  | 20 | `TF-IMP-26-0001` |
  | 32A | `260720USD100000,00` |
  | 53A | `001-234567-USD` |
  | 58A | `BNPAFRPP` |

---

### Feature: MT103 — Customer Credit Transfer (FR-SWG-14)

#### Scenario BDD-SWG-103-01: MT103 — Customer Credit Transfer with Charges
**User Story:** US-SWG-07
**Functional Requirement:** FR-SWG-14
**Type:** Happy Path

* **Given** an `ImportLcSettlement` with `valueDate = 2026-07-20`, `principalAmount = 100000`, `remittanceCurrency = USD`, `chargesDetailEnumId = SHA`
* **And** Applicant party: `ACME IMPORT CO, 123 MAIN ST, HO CHI MINH CITY`
* **And** Beneficiary party: `GLOBAL STEEL LTD, 456 HARBOUR RD, TOKYO`
* **When** `generate#Mt103` is called
* **Then** the message contains:
  | Tag | Content |
  | 20 | `TF-IMP-26-0001` |
  | 32A | `260720USD100000,00` |
  | 50K | `ACME IMPORT CO\r\n123 MAIN ST, HO CHI MINH CITY` |
  | 59 | `GLOBAL STEEL LTD\r\n456 HARBOUR RD, TOKYO` |
  | 71A | `SHA` |

---

### Feature: Layer 2 Generation-Time Validation (FR-SWG-03)

#### Scenario BDD-SWG-L2V-01: Layer 2 — Generation Aborted on Invalid Data
**User Story:** US-SWG-08
**Functional Requirement:** FR-SWG-03
**Type:** Edge Case

* **Given** a `TradeInstrument` with `transactionRef` containing `//` (which bypassed Layer 1)
* **When** `generate#Mt700` is called
* **Then** Layer 2 validation runs `validate#SwiftFields` before assembly
* **And** the service aborts with error: `Layer 2 validation failed: transactionRef contains '//'`
* **And** no `SwiftMessage` record is created

#### Scenario BDD-SWG-L2V-02: Layer 2 — Auto-Conversion with Warning Log
**User Story:** US-SWG-08
**Functional Requirement:** FR-SWG-03
**Type:** Edge Case

* **Given** an `ImportLetterOfCredit` where `goodsDescription` contains `&` character (which bypassed Layer 1)
* **When** `generate#Mt700` processes Tag 45A at Layer 2
* **Then** the `&` is auto-converted to `AND` in the generated message
* **And** a warning is logged: `Layer 2 auto-conversion: goodsDescription, '&' → 'AND' at position XX`

---

### Feature: Message Persistence (FR-SWG-04)

#### Scenario BDD-SWG-PER-01: Persistence — SwiftMessage Record Created
**User Story:** US-SWG-09
**Functional Requirement:** FR-SWG-04
**Type:** Happy Path

* **Given** a successful `generate#Mt700` execution
* **When** the service completes
* **Then** a `SwiftMessage` record exists with:
  | Field | Value |
  | `instrumentId` | Matches the source LC |
  | `messageType` | `MT700` |
  | `messageContent` | The full assembled SWIFT message text |
  | `generatedDate` | Current system timestamp |
* **And** the service returns the `swiftMessageId` to the caller
