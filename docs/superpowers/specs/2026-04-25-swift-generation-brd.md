# Business Requirements Document (BRD)
**Project Name:** Digital Trade Finance Platform
**Module:** SWIFT Message Generation Services (Spec B)
**Document Version:** 1.0
**Date:** April 25, 2026
**Prerequisite:** Spec A — SWIFT Validation Infrastructure (entity schema additions + Layer 1 validation)
**Traceability:** Implements Design Spec §4.1 through §4.9, REQ-IMP-SWIFT-01 through 05

---

## REQ-SWG-01: Overview & Business Need

The current SWIFT message generation services are skeletal stubs using raw string concatenation with hardcoded placeholder values. They do not read actual entity data, do not format tags per SWIFT standards, do not construct proper SWIFT header blocks, and skip the majority of mandatory and optional tags specified in the design.

**Business Impact:** Without fully implemented generation services:
- Issued LCs cannot be communicated to Advising Banks via the SWIFT network
- Amendment, discrepancy, and settlement messages cannot be transmitted
- The bank cannot participate in standard interbank trade finance messaging

**Goal:** Implement all 10 MT message generation services with proper entity data mapping, SWIFT tag formatting, header construction, Layer 2 validation, and `SwiftMessage` persistence.

---

## REQ-SWG-02: User Stories

### US-SWG-01: Correct MT700 Generation on LC Issuance
**As a** Trade Operations system,
**I want** the system to generate a standards-compliant MT700 message from entity data when a Checker authorizes an LC issuance,
**So that** the Advising Bank receives a correctly formatted documentary credit notification.

### US-SWG-02: MT701 Auto-Continuation for Long Narratives
**As a** Trade Operations system,
**I want** the system to automatically split narratives exceeding 100×65 characters and generate MT701 continuation messages,
**So that** long goods descriptions, document lists, and conditions are fully transmitted.

### US-SWG-03: MT707 for Amendments with Delta-Only Tags
**As a** Trade Operations system,
**I want** the system to generate an MT707 containing only changed tags when an amendment is authorized,
**So that** the Advising Bank accurately processes only the modified terms.

### US-SWG-04: Document Presentation Messages (MT750, MT734, MT752)
**As a** Trade Operations system,
**I want** the system to generate the correct message type based on the presentation outcome (discrepant, refused, waiver accepted),
**So that** the Presenting Bank is properly notified of the Issuing Bank's decision.

### US-SWG-05: Usance Acceptance Message (MT732)
**As a** Trade Operations system,
**I want** the system to generate an MT732 Advice of Discharge when a Usance presentation is accepted,
**So that** the Presenting Bank receives formal acknowledgment of the future maturity date.

### US-SWG-06: Cancellation Request Message (MT799)
**As a** Trade Operations system,
**I want** the system to generate an MT799 free-format message requesting beneficiary consent when initiating early LC cancellation,
**So that** the cancellation process follows standard interbank communication protocols.

### US-SWG-07: Settlement Payment Messages (MT202, MT103)
**As a** Trade Operations system,
**I want** the system to generate the appropriate settlement message (MT202 for bank-to-bank or MT103 for customer credit) when a payment is authorized,
**So that** funds are correctly routed to the Presenting Bank or Beneficiary.

### US-SWG-08: Layer 2 Safety Net Validation
**As a** Trade Operations system,
**I want** the system to re-validate all SWIFT-bound fields immediately before message assembly as a safety net,
**So that** any data that bypassed Layer 1 validation is caught before SWIFT transmission.

### US-SWG-09: Message Persistence and Auditability
**As a** Trade Operations system,
**I want** every generated SWIFT message to be persisted in the `SwiftMessage` entity with type, content, and timestamp,
**So that** the bank has a complete audit trail of all outbound SWIFT communications.

---

## REQ-SWG-03: Functional Requirements

### FR-SWG-01: Groovy SWIFT Message Builder
The system must provide a reusable Groovy utility class (`SwiftMessageBuilder`) that:
- Constructs SWIFT Basic Header Block 1 (sender BIC from `TradeConfig.ISSUING_BANK_BIC`)
- Constructs Application Header Block 2 (message type, receiver BIC)
- Constructs User Header Block 3 (optional)
- Constructs Text Block 4 (tag content)
- Handles tag formatting: date→YYMMDD, amount→comma-decimal, multi-line→NxM wrapping
- Provides a `build()` method that returns the assembled SWIFT message text

### FR-SWG-02: Tag Formatting Services
The system must provide tag-specific formatting services:
- `format#SwiftDate(date)` → `YYMMDD` string
- `format#SwiftAmount(currency, amount)` → `CCY` + comma-decimal amount (e.g., `USD50000,00`)
- `format#SwiftParty(name, address, accountNumber)` → 4×35 formatted party block
- `format#SwiftBic(bic)` → validated 8/11 char BIC
- `format#SwiftNarrative(text, charsPerLine)` → auto-wrapped lines at specified width
- `format#SwiftReference(ref)` → validated reference (slash rules, max 16 chars)

### FR-SWG-03: Layer 2 Generation-Time Validation
Before assembling any message, the generation service must:
- Call `validate#SwiftFields` (from Spec A) on all source entities
- If any errors: abort generation, return errors, log warning
- If data passes but contains unexpected characters: auto-convert (e.g., `&` → `AND`) and log warning
- All Layer 2 conversions must be logged with field name, original value, and converted value

### FR-SWG-04: Message Persistence
Every generated message must be persisted:
- Create `SwiftMessage` record with `instrumentId`, `messageType`, `messageContent`, `generatedDate`
- Return `swiftMessageId` to caller for reference

### FR-SWG-05: MT700 Generation — Full Tag Mapping
Service: `SwiftGenerationServices.generate#Mt700`
Trigger: Checker authorizes LC Issuance

Must populate all tags from entity data:

| Block | Tags | M/O | Data Source |
|:---|:---|:---|:---|
| **A: Header** | 27 (Sequence), 40A (Form of DC), 20 (Credit No.), 31C (Issue Date), 31D (Expiry Date+Place) | All M | `TradeInstrument`, `ImportLetterOfCredit` |
| **B: Parties** | 50 (Applicant), 59/59A (Beneficiary — BIC toggle), 51A (Applicant Bank) | 50/59 M, 51A O | `TradeParty`, `TradeInstrument.beneficiaryName` |
| **C: Financials** | 32B (Amount), 39A (Tolerance), 39B (Max Credit), 39C (Additional Amounts), 41a (Available With), 42C (Drafts at), 42A (Drawee), 42M (Mixed), 42P (Deferred) | 32B/41a M, rest O | `TradeInstrument`, `ImportLetterOfCredit` |
| **D: Shipping** | 43P (Partial), 43T (Transhipment), 44A (Receipt Place), 44B (Final Dest), 44C (Latest Shipment), 44D (Shipment Period), 44E (Port of Loading), 44F (Port of Discharge) | All O | `ImportLetterOfCredit` |
| **E: Narratives** | 45A (Goods), 46A (Documents), 47A (Conditions), 71D (Charges), 48 (Presentation Period), 49 (Confirmation) | 45A/46A/49 M, rest O | `ImportLetterOfCredit` |
| **F: Routing** | 53a (Reimbursing Bank), 57a (Advise Through Bank) | All O | `TradeInstrument` |
| **G: Admin** | 23 (Pre-Advice Ref), 72Z (Sender Info), 78 (Bank Instructions) | All O | `TradeInstrument`, `ImportLetterOfCredit` |

### FR-SWG-06: MT701 Auto-Continuation
Service: `SwiftGenerationServices.generate#Mt701`
Triggered automatically during MT700 generation when Tags 45A, 46A, or 47A exceed 100 lines × 65 characters.

| Tag | M/O | Source |
|:---|:---|:---|
| 27 | M | Updated to `2/N` where N = total messages |
| 20 | M | Same `transactionRef` as parent MT700 |
| 45B | O | Overflow from 45A |
| 46B | O | Overflow from 46A |
| 47B | O | Overflow from 47A |

### FR-SWG-07: MT707 Amendment Generation
Service: `SwiftGenerationServices.generate#Mt707`
Trigger: Checker authorizes Amendment

Only changed fields populate corresponding tags. Conditional logic:
- If `amountAdjustment > 0` → Tag 32B (increase)
- If `amountAdjustment < 0` → Tag 33B (decrease, absolute value)
- If `amountAdjustment ≠ 0` → Tag 34B (new total amount)
- If `newExpiryDate` set → Tag 31E

| Tag | M/O | Source |
|:---|:---|:---|
| 20 | M | `TradeInstrument.transactionRef` |
| 21 | M | Advising Bank ref or `NONREF` |
| 31C | M | Original `issueDate` |
| 30 | M | `ImportLcAmendment.amendmentDate` |
| 26E | M | `ImportLcAmendment.amendmentNumber` |
| 32B | O | Amount increase (if positive) |
| 33B | O | Amount decrease (if negative) |
| 34B | O | New total effective amount |
| 31E | O | New expiry date |
| 79 | O | `amendmentNarrative` (Z charset, 35×50) |

### FR-SWG-08: MT750 Discrepancy Advice
Service: `SwiftGenerationServices.generate#Mt750`
Trigger: Presentation marked Discrepant

| Tag | M/O | Source |
|:---|:---|:---|
| 20 | M | `TradeInstrument.transactionRef` |
| 21 | M | `TradeDocumentPresentation.presentingBankRef` |
| 32B | M | `claimCurrency` + `claimAmount` |
| 77J | M | Concatenated from `PresentationDiscrepancy` records (code + description) |
| 72Z | O | `TradeInstrument.senderToReceiverInfo` or standard waiver text |

### FR-SWG-09: MT734 Notice of Refusal
Service: `SwiftGenerationServices.generate#Mt734`
Trigger: Applicant refuses to waive discrepancies

| Tag | M/O | Source |
|:---|:---|:---|
| 20 | M | `TradeInstrument.transactionRef` |
| 21 | M | `TradeDocumentPresentation.presentingBankRef` |
| 32A | M | Presentation date + `claimCurrency` + `claimAmount` |
| 73 | O | Calculated discrepancy fee text |
| 77J | M | Concatenated from `PresentationDiscrepancy` records |
| 77B | M | `documentDisposalEnumId` mapped to `HOLDING DOCUMENTS` or `RETURNING DOCUMENTS` |

### FR-SWG-10: MT752 Authorization to Pay
Service: `SwiftGenerationServices.generate#Mt752`
Trigger: Discrepancies waived, presentation accepted

| Tag | M/O | Source |
|:---|:---|:---|
| 20 | M | `TradeInstrument.transactionRef` |
| 21 | M | `TradeDocumentPresentation.presentingBankRef` |
| 30 | M | System business date |
| 32B | M | `claimCurrency` + `claimAmount` |
| 72Z | O | `"DISCREPANCIES WAIVED BY APPLICANT"` or configured text |

### FR-SWG-11: MT732 Advice of Discharge
Service: `SwiftGenerationServices.generate#Mt732`
Trigger: Usance LC presentation accepted (before money moves)

| Tag | M/O | Source |
|:---|:---|:---|
| 20 | M | `TradeInstrument.transactionRef` |
| 21 | M | `TradeDocumentPresentation.presentingBankRef` |
| 30 | M | System business date |
| 32A | M | Maturity date + `claimCurrency` + `claimAmount` (date must be future maturity) |

### FR-SWG-12: MT799 Free Format Message
Service: `SwiftGenerationServices.generate#Mt799`
Trigger: Early cancellation — requesting beneficiary consent

| Tag | M/O | Source |
|:---|:---|:---|
| 20 | M | `TradeInstrument.transactionRef` |
| 21 | M | Advising Bank ref or `NONREF` |
| 79 | M | System-generated cancellation request narrative (Z charset, 35×50) |

### FR-SWG-13: MT202 Bank-to-Bank Transfer
Service: `SwiftGenerationServices.generate#Mt202`
Trigger: Settlement — bank-to-bank reimbursement

| Tag | M/O | Source |
|:---|:---|:---|
| 20 | M | `TradeInstrument.transactionRef` |
| 21 | M | `TradeDocumentPresentation.presentingBankRef` |
| 32A | M | `valueDate` + `remittanceCurrency` + `principalAmount` |
| 53a | O | Issuing bank's Nostro account (from `TradeConfig.NOSTRO_ACCOUNT_{CCY}`) |
| 58a | M | Presenting Bank BIC (`TradeDocumentPresentation.presentingBankBic`) |

### FR-SWG-14: MT103 Customer Credit Transfer
Service: `SwiftGenerationServices.generate#Mt103`
Trigger: Settlement — direct payment to beneficiary account

| Tag | M/O | Source |
|:---|:---|:---|
| 20 | M | `TradeInstrument.transactionRef` |
| 32A | M | `valueDate` + `remittanceCurrency` + `principalAmount` |
| 50a | M | Applicant party details (name + address from `TradeParty`) |
| 59a | M | Beneficiary party details (name + address from `TradeParty`) |
| 71A | M | `chargesDetailEnumId` → `OUR`, `BEN`, or `SHA` |

---

## REQ-SWG-04: Lifecycle Integration Points

Each generation service is triggered by a specific lifecycle event. The calling service must:
1. Call `validate#SwiftFields` (Layer 2) on all source entities
2. Call the appropriate `generate#MtXXX` service
3. Receive the `swiftMessageId` confirming persistence
4. Log the message dispatch in `TradeTransactionAudit`

| MT Type | Lifecycle Trigger | Calling Service |
|:---|:---|:---|
| MT700 | LC Issuance authorized | `execute#IssuancePostAuth` |
| MT701 | Auto (from MT700 if overflow) | `generate#Mt700` (internal) |
| MT707 | Amendment authorized | `authorize#Amendment` (post-auth) |
| MT750 | Presentation marked Discrepant | `authorize#Presentation` |
| MT734 | Presentation formally Refused | `update#PresentationRefusal` |
| MT752 | Discrepancy Waived → Accepted | `update#PresentationWaiver` |
| MT732 | Usance presentation Accepted | `authorize#Presentation` (usance path) |
| MT799 | Early cancellation initiated | `update#Cancellation` (early mutual) |
| MT202 | Settlement authorized (bank-to-bank) | `settle#Presentation` |
| MT103 | Settlement authorized (customer direct) | `settle#Presentation` |

---

## REQ-SWG-05: Traceability Matrix

| User Story | Functional Requirement(s) | MT Message(s) |
|:---|:---|:---|
| US-SWG-01 | FR-SWG-01 (Builder), FR-SWG-02 (Formatting), FR-SWG-05 (MT700) | MT700 |
| US-SWG-02 | FR-SWG-06 (MT701) | MT701 |
| US-SWG-03 | FR-SWG-07 (MT707) | MT707 |
| US-SWG-04 | FR-SWG-08 (MT750), FR-SWG-09 (MT734), FR-SWG-10 (MT752) | MT750, MT734, MT752 |
| US-SWG-05 | FR-SWG-11 (MT732) | MT732 |
| US-SWG-06 | FR-SWG-12 (MT799) | MT799 |
| US-SWG-07 | FR-SWG-13 (MT202), FR-SWG-14 (MT103) | MT202, MT103 |
| US-SWG-08 | FR-SWG-03 (Layer 2) | All |
| US-SWG-09 | FR-SWG-04 (Persistence) | All |
