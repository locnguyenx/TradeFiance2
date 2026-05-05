# Business Requirements Document (BRD)
**Project Name:** Digital Trade Finance Platform
**Module:** SWIFT Validation Infrastructure (Spec A)
**Document Version:** 1.0
**Date:** April 25, 2026
**Traceability:** Extends REQ-IMP-SWIFT-01 through REQ-IMP-SWIFT-05 from the Import LC BRD.

---

## REQ-SWV-01: Overview & Business Need

SWIFT messages have strict constraints on character sets, field lengths, line formats, and mutual exclusion rules. The current system stores SWIFT-bound data without any such constraints — a Maker can save invalid characters, exceed field lengths, or violate mutual exclusion rules, only discovering errors (if at all) at SWIFT message generation time.

**Business Impact:** In banking operations, SWIFT NACKs (network rejections) and generation-time errors cause:
- Transaction processing delays (re-routing back to Maker for correction)
- SLA breaches on the UCP 600 5-day examination window
- Operational rework and cost
- Reputational risk with correspondent banks

**Goal:** Enforce SWIFT compliance at data capture time (Layer 1) across all SWIFT-bound fields in the Import LC lifecycle. By the time a transaction reaches the generation stage, all text fields are guaranteed compliant.

---

## REQ-SWV-02: User Stories

### US-SWV-01: Inline SWIFT Character Validation
**As a** Trade Operations Maker,
**I want** the system to immediately flag invalid SWIFT characters when I enter data,
**So that** I can fix formatting issues during data entry instead of having transactions rejected at generation time.

### US-SWV-02: Field Length Enforcement
**As a** Trade Operations Maker,
**I want** the system to enforce SWIFT field length limits (e.g., max 16 chars for references, 4×35 for addresses, 65 chars per line for narratives),
**So that** I never submit data that will fail SWIFT tag assembly.

### US-SWV-03: Mutual Exclusion Guards
**As a** Trade Operations Maker,
**I want** the system to prevent me from filling in mutually exclusive fields (e.g., tolerance percentage AND "not exceeding" flag),
**So that** the generated SWIFT message is logically valid per SWIFT standards.

### US-SWV-04: Conditional Field Requirements
**As a** Trade Operations Maker,
**I want** the system to require conditional fields based on business context (e.g., usance days required when tenor ≠ SIGHT),
**So that** all required SWIFT tags have source data at generation time.

### US-SWV-05: Comprehensive Pre-Submission Validation
**As a** Trade Operations Maker,
**I want** a comprehensive SWIFT compliance check to run before I submit a transaction for approval,
**So that** I receive a complete list of all SWIFT-related issues to fix in one pass.

---

## REQ-SWV-03: Functional Requirements

### FR-SWV-01: SWIFT Character Set Validation
The system must validate all SWIFT-bound text fields against the appropriate character set:

**X Character Set** (majority of SWIFT fields):
- Allowed: `A-Z`, `a-z`, `0-9`, `/ - ? : ( ) . , ' +` and spaces
- Blocked: `@ & _ # ! % ^ * { } [ ] | \ " ; < > ~` and all other special characters

**Z Character Set** (Tags 79 in MT707, MT799):
- Extends X charset with: `@ # = ! " % & * ; < > _`
- Used only for free-format narrative fields

### FR-SWV-02: Reference Field Slash Rules
All reference fields (Tags 20, 21, 23) must not:
- Start or end with `/`
- Contain `//` (double slash)

### FR-SWV-03: SWIFT Amount Format
Financial amount fields must:
- Use comma `,` as decimal separator (not period)
- Not exceed 15 digits (including the comma)
- Be positive (> 0)

### FR-SWV-04: SWIFT Date Format
All date fields destined for SWIFT tags must be valid dates in `YYMMDD` format.

### FR-SWV-05: SWIFT BIC Validation
All SWIFT BIC fields must be exactly 8 or 11 alphanumeric characters.

### FR-SWV-06: Line-Format Validation
Multi-line text fields must conform to SWIFT line formatting rules:
- `NxM` format: maximum N lines of M characters each
- System must support validation for 4×35, 6×35, 12×65, 35×50, 70×50, and 100×65 formats

### FR-SWV-07: Mutual Exclusion Rules
The system must enforce these mutual exclusions at data capture:
- Tag 39A (Tolerance %) and Tag 39B (Max Credit Amount) — cannot both be set
- Tag 44C (Latest Shipment Date) and Tag 44D (Shipment Period Text) — cannot both be set
- Tag 53A (Reimbursing Bank BIC) and Tag 53D (Reimbursing Bank Name/Address) — use one format per party

### FR-SWV-08: Conditional Field Requirements
The system must enforce these conditional requirements:
- If `tenorTypeId` ≠ SIGHT: `usanceDays` and `usanceBaseDate` are required (Tags 42C)
- If `tenorTypeId` = MIXED: `mixedPaymentDetails` is required (Tag 42M)
- If `tenorTypeId` = DEF_PAYMENT or NEGOTIATION: `deferredPaymentDetails` is required (Tag 42P)
- If `isDiscrepant` = Y: at least one `PresentationDiscrepancy` record required (Tag 77J source)
- If `applicantDecisionEnumId` = REFUSED: `documentDisposalEnumId` is required (Tag 77B)

### FR-SWV-09: Validation Service API
The system must provide a service `validate#SwiftFields` that:
- Accepts an entity type and entity ID
- Validates ALL SWIFT-bound fields on that entity
- Returns field-level error messages with specific violation details (e.g., "Description of Goods contains invalid character '@' at position 142")
- Can be called on save (incremental) and on submit (comprehensive)
- Supports all SWIFT-bound entities: `ImportLetterOfCredit`, `TradeInstrument`, `TradeParty`, `ImportLcAmendment`, `TradeDocumentPresentation`, `PresentationDiscrepancy`, `ImportLcSettlement`

---

## REQ-SWV-04: Entity Schema — New Fields Required

Cross-referencing the MT700 BRD (REQ-IMP-SWIFT-02/03), the MT-others spec, and the design spec (Section 4) against existing entities, the following fields are missing:

### ImportLetterOfCredit — New Fields

| Req ID | Field | Type | SWIFT Tag | Constraint | MT Messages |
|:---|:---|:---|:---|:---|:---|
| FR-ENT-01 | `receiptPlace` | `text-medium` | 44A | Max 65 chars, X charset | MT700 |
| FR-ENT-02 | `finalDeliveryPlace` | `text-medium` | 44B | Max 65 chars, X charset | MT700 |
| FR-ENT-03 | `shipmentPeriodText` | `text-medium` | 44D | Max 65 chars, X charset. Mutually exclusive with `latestShipmentDate` | MT700 |
| FR-ENT-04 | `maxCreditAmountFlag` | `text-indicator` | 39B | Y/N. Mutually exclusive with `tolerancePositive`/`toleranceNegative` | MT700 |
| FR-ENT-05 | `additionalAmountsText` | `text-long` | 39C | Max 4×35 chars, X charset | MT700 |
| FR-ENT-06 | `mixedPaymentDetails` | `text-long` | 42M | Max 4×35 chars, X charset. Required if tenor = MIXED | MT700 |
| FR-ENT-07 | `deferredPaymentDetails` | `text-long` | 42P | Max 4×35 chars, X charset. Required if tenor = DEF_PAYMENT/NEGOTIATION | MT700 |
| FR-ENT-08 | `usanceBaseDate` | `text-medium` | 42C | X charset, max 35 chars. Required if tenor ≠ SIGHT | MT700 |
| FR-ENT-09 | `bankToBankInstructions` | `text-very-long` | 78 | Max 12×65 chars, X charset | MT700 |
| FR-ENT-10 | `presentationPeriodDays` | `number-integer` | 48 | Positive integer. Days after shipment for doc presentation | MT700 |
| FR-ENT-11 | `chargeAllocationText` | `text-long` | 71D | Max 6×35 chars, X charset. Detailed charges text | MT700 |

### TradeInstrument — New Fields

| Req ID | Field | Type | SWIFT Tag | Constraint | MT Messages |
|:---|:---|:---|:---|:---|:---|
| FR-ENT-12 | `reimbursingBankBic` | `text-short` | 53A | Valid 8/11 char BIC | MT700, MT202 |
| FR-ENT-13 | `reimbursingBankName` | `text-long` | 53D | Max 4×35 chars, X charset. Alternative to BIC | MT700 |
| FR-ENT-14 | `adviseThroughBankBic` | `text-short` | 57A | Valid 8/11 char BIC | MT700 |
| FR-ENT-15 | `adviseThroughBankName` | `text-long` | 57D | Max 4×35 chars, X charset. Alternative to BIC | MT700 |
| FR-ENT-16 | `preAdviceRef` | `text-short` | 23 | Max 16 chars, X charset, slash rules | MT700 |
| FR-ENT-17 | `senderToReceiverInfo` | `text-long` | 72Z | Max 6×35 chars, X charset | MT700, MT750, MT752 |
| FR-ENT-18 | `beneficiaryName` | `text-long` | 59 | Max 4×35 chars, X charset. Per 20250425 improvement | MT700, MT707 |

### TradeDocumentPresentation — New Field

| Req ID | Field | Type | SWIFT Tag | Constraint | MT Messages |
|:---|:---|:---|:---|:---|:---|
| FR-ENT-19 | `documentDisposalEnumId` | `id` | 77B | HOLDING_DOCUMENTS, RETURNING_DOCUMENTS. Required when refusing discrepant docs | MT734 |

### SwiftMessage — Enhanced Fields

| Req ID | Field | Type | Purpose | Constraint |
|:---|:---|:---|:---|:---|
| FR-ENT-20 | `messageStatusId` | `id` | Message lifecycle status | `SWIFT_MSG_DRAFT` or `SWIFT_MSG_ACTIVE`. See Spec B FR-SWG-15 for lifecycle rules |

### Bank Configuration — New TradeConfig entries

| Req ID | Config Key | Purpose | MT Messages |
|:---|:---|:---|:---|
| FR-CFG-01 | `ISSUING_BANK_BIC` | Issuing bank's SWIFT BIC for message headers | All MT messages |
| FR-CFG-02 | `NOSTRO_ACCOUNT_{CCY}` | Nostro account per currency for settlement routing | MT202 |

---

## REQ-SWV-05: SWIFT Validation Rules — Complete Field Matrix

### ImportLetterOfCredit Fields

| Field | SWIFT Tag | Charset | Max Length | Format | Conditional Rule |
|:---|:---|:---|:---|:---|:---|
| `goodsDescription` | 45A | X | 100×65 | Auto-wrap at 65 chars/line | Mandatory for MT700 |
| `documentsRequired` | 46A | X | 100×65 | Auto-wrap at 65 chars/line | Mandatory for MT700 |
| `additionalConditions` | 47A | X | 100×65 | Auto-wrap at 65 chars/line | Optional |
| `portOfLoading` | 44E | X | 65 | Single line | — |
| `portOfDischarge` | 44F | X | 65 | Single line | — |
| `expiryPlace` | 31D | X | 29 | Single line (appended to date) | Mandatory |
| `tolerancePositive` | 39A | — | — | Integer, ×100 scaling | Mutually exclusive with `maxCreditAmountFlag` |
| `toleranceNegative` | 39A | — | — | Integer, ×100 scaling | Mutually exclusive with `maxCreditAmountFlag` |
| `latestShipmentDate` | 44C | — | 6 | YYMMDD | Mutually exclusive with `shipmentPeriodText` |
| `receiptPlace` | 44A | X | 65 | Single line | — |
| `finalDeliveryPlace` | 44B | X | 65 | Single line | — |
| `shipmentPeriodText` | 44D | X | 65 | Single line | Mutually exclusive with `latestShipmentDate` |
| `maxCreditAmountFlag` | 39B | — | 1 | Y/N indicator | Mutually exclusive with tolerance (39A) |
| `additionalAmountsText` | 39C | X | 4×35 | Multi-line | — |
| `mixedPaymentDetails` | 42M | X | 4×35 | Multi-line | Required if tenor = MIXED |
| `deferredPaymentDetails` | 42P | X | 4×35 | Multi-line | Required if tenor = DEF_PAYMENT/NEGOTIATION |
| `usanceBaseDate` | 42C | X | 35 | Single line | Required if tenor ≠ SIGHT |
| `bankToBankInstructions` | 78 | X | 12×65 | Auto-wrap at 65 chars/line | — |
| `presentationPeriodDays` | 48 | — | — | Positive integer | — |
| `chargeAllocationText` | 71D | X | 6×35 | Multi-line | — |

### TradeInstrument Fields

| Field | SWIFT Tag | Charset | Max Length | Format | Conditional Rule |
|:---|:---|:---|:---|:---|:---|
| `transactionRef` | 20 | X | 16 | Single line, slash rules | Mandatory, all MT messages |
| `currencyUomId` | 32B | — | 3 | ISO 4217 | Mandatory |
| `amount` | 32B | — | 15 digits | Comma decimal separator | Mandatory, positive |
| `issueDate` | 31C | — | 6 | YYMMDD | Mandatory |
| `expiryDate` | 31D | — | 6 | YYMMDD | Mandatory |
| `beneficiaryName` | 59 | X | 4×35 | Multi-line | — |
| `reimbursingBankBic` | 53A | — | 8 or 11 | Alphanumeric BIC | Mutually exclusive with `reimbursingBankName` |
| `reimbursingBankName` | 53D | X | 4×35 | Multi-line | Mutually exclusive with `reimbursingBankBic` |
| `adviseThroughBankBic` | 57A | — | 8 or 11 | Alphanumeric BIC | Mutually exclusive with `adviseThroughBankName` |
| `adviseThroughBankName` | 57D | X | 4×35 | Multi-line | Mutually exclusive with `adviseThroughBankBic` |
| `preAdviceRef` | 23 | X | 16 | Single line, slash rules | — |
| `senderToReceiverInfo` | 72Z | X | 6×35 | Multi-line | — |

### TradeParty Fields

| Field | SWIFT Tag | Charset | Max Length | Format | Conditional Rule |
|:---|:---|:---|:---|:---|:---|
| `partyName` | 50/59 | X | 4×35 | Multi-line (part of party block) | Mandatory |
| `registeredAddress` | 50/59 | X | 4×35 | Multi-line (part of party block) | — |
| `swiftBic` | 51A/53A/57A | — | 8 or 11 | Alphanumeric BIC | — |

### ImportLcAmendment Fields

| Field | SWIFT Tag | Charset | Max Length | Format | Conditional Rule |
|:---|:---|:---|:---|:---|:---|
| `amendmentNarrative` | 79 | **Z** | 35×50 | Auto-wrap at 50 chars/line | — |
| `amendmentDate` | 30 | — | 6 | YYMMDD | Mandatory for MT707 |
| `amountAdjustment` | 32B/33B | — | 15 digits | Comma decimal, conditional positive/negative | Determines 32B (increase) vs 33B (decrease) |
| `newExpiryDate` | 31E | — | 6 | YYMMDD | — |

### TradeDocumentPresentation Fields

| Field | SWIFT Tag | Charset | Max Length | Format | Conditional Rule |
|:---|:---|:---|:---|:---|:---|
| `presentingBankBic` | — | — | 8 or 11 | Alphanumeric BIC | — |
| `presentingBankRef` | 21 | X | 16 | Single line, slash rules | Used in MT750, MT734, MT752, MT732 |
| `claimAmount` | 32B | — | 15 digits | Comma decimal separator | Mandatory, positive |
| `claimCurrency` | 32B | — | 3 | ISO 4217 | Must match parent LC currency |
| `documentDisposalEnumId` | 77B | — | — | Enum: HOLDING_DOCUMENTS, RETURNING_DOCUMENTS | Required when refusing (MT734) |

### PresentationDiscrepancy Fields

| Field | SWIFT Tag | Charset | Max Length | Format | Conditional Rule |
|:---|:---|:---|:---|:---|:---|
| `discrepancyCode` | 77J | X | 35 | Single line | Mandatory per record |
| `discrepancyDescription` | 77J | X | 50 | Single line (wrapped into 77J block) | — |

### ImportLcSettlement Fields

| Field | SWIFT Tag | Charset | Max Length | Format | Conditional Rule |
|:---|:---|:---|:---|:---|:---|
| `principalAmount` | 32A | — | 15 digits | Comma decimal separator | Mandatory, positive |
| `remittanceCurrency` | 32A | — | 3 | ISO 4217 | Must match LC currency |
| `valueDate` | 32A | — | 6 | YYMMDD | Mandatory |
| `maturityDate` | 32A(MT732) | — | 6 | YYMMDD | Required for Usance LCs |
| `chargesDetailEnumId` | 71A | — | — | Enum: OUR, BEN, SHA | Mandatory for MT103 |
| `forwardContractRef` | — | X | 16 | Single line, slash rules | — |

---

## REQ-SWV-06: Traceability Matrix

| User Story | Functional Requirement(s) | Entity Fields Affected |
|:---|:---|:---|
| US-SWV-01 | FR-SWV-01 (Character Set) | All X/Z charset fields listed above |
| US-SWV-02 | FR-SWV-02 (Slash Rules), FR-SWV-03 (Amount), FR-SWV-05 (BIC), FR-SWV-06 (Line Format) | All reference, amount, BIC, multi-line fields |
| US-SWV-03 | FR-SWV-07 (Mutual Exclusion) | 39A/39B, 44C/44D, 53A/53D, 57A/57D |
| US-SWV-04 | FR-SWV-08 (Conditional Requirements) | Tenor-dependent fields, discrepancy-dependent fields |
| US-SWV-05 | FR-SWV-09 (Validation Service) | All SWIFT-bound entities |
