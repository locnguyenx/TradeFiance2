# Business Requirements Document (BRD)
**Project Name:** Digital Trade Finance Platform
**Module:** SWIFT Gaps Consolidation (Validation & Generation)
**Document Version:** 1.0
**Date:** April 26, 2026
**Traceability:** Resolves gaps identified in Spec A (Validation) and Spec B (Generation) relative to `MT700-generation.md` and `MT-others.md`.

---

## REQ-SGC-01: Overview & Business Need

The initial SWIFT validation (Spec A) and generation (Spec B) documents omitted several critical tags and data fields required for full SWIFT compliance across the Import LC lifecycle. These gaps include mandatory MT700 fields, amendment sequencing, and comprehensive document presentation mapping.

**Goal:** Provide a single source of truth for the missing SWIFT-bound fields and their associated validation/generation logic to ensure 100% STP (Straight-Through Processing) for all 10 priority MT messages.

---

## REQ-SGC-02: Entity Schema & Validation — Missing Tags

The following fields must be added to the Moqui entity definitions and validated at data capture time.

### ImportLetterOfCredit — Enhanced Fields

| Req ID | Field | Type | SWIFT Tag | Constraint | MT Messages |
|:---|:---|:---|:---|:---|:---|
| FR-ENT-21 | `lcTypeEnumId` | `id` | 40A | Mandatory. Valid SWIFT Form of DC code: IRREVOCABLE, REVOCABLE, IRREVOCABLE_TRANSFERABLE | MT700 |
| FR-ENT-22 | `availableWithBic` | `text-short` | 41A | EITHER "ANY BANK" OR valid 8/11 character BIC of a TradeParty with Bank role | MT700 |
| FR-ENT-23 | `availableByEnumId` | `id` | 41a | Mandatory. Enum: BY_PAYMENT, BY_ACCEPTANCE, BY_NEGOTIATION, BY_DEF_PAYMENT | MT700 |
| FR-ENT-24 | `partialShipmentEnumId` | `id` | 43P | Mandatory. Enum: ALLOWED, NOT_ALLOWED, CONDITIONAL | MT700 |
| FR-ENT-25 | `transhipmentEnumId` | `id` | 43T | Mandatory. Enum: ALLOWED, NOT_ALLOWED, CONDITIONAL | MT700 |
| FR-ENT-26 | `confirmationEnumId` | `id` | 49 | Mandatory. Enum: CONFIRM, MAY_ADD, WITHOUT | MT700 |
| FR-ENT-27 | `draweeBic` | `text-short` | 42A | Valid 8/11 character BIC of a TradeParty with Bank role | MT700 |

### ImportLcAmendment — Enhanced Fields

| Req ID | Field | Type | SWIFT Tag | Constraint | MT Messages |
|:---|:---|:---|:---|:---|:---|
| FR-ENT-28 | `amendmentNumber` | `number-integer` | 26E | Positive integer, auto-incrementing | MT707 |
| FR-ENT-29 | `newTotalAmount` | `number-decimal` | 34B | Positive amount, 15 digits, comma decimal | MT707 |

### TradeDocumentPresentation — Enhanced Fields

| Req ID | Field | Type | SWIFT Tag | Constraint | MT Messages |
|:---|:---|:---|:---|:---|:---|
| FR-ENT-30 | `presentationRef` | `text-short` | 20 | Mandatory. Max 16 characters, X Character Set. No leading/trailing "/" or "//" | MT750, MT734 |
| FR-ENT-31 | `presentationDate` | `date` | 30 / 32A | Mandatory. Valid date. Formatted as YYMMDD in SWIFT tags | MT734, MT732 |
| FR-ENT-32 | `chargesDeducted` | `text-long` | 73 | Max 6 lines of 35 characters each. X Character Set | MT734 |
| FR-ENT-33 | `senderToReceiverPresentationInfo` | `text-long` | 72Z | Max 6 lines of 35 characters each. X Character Set | MT750, MT752 |

---

## REQ-SGC-03: Functional Requirements — Generation (Layer 2)

The `SwiftMessageBuilder` and associated services must be updated to handle the mapping and assembly of these tags.

### FR-SGC-04: MT700 Completion Logic
- **Tag 41a Alignment:** Assemble "Available With" by combining `availableWithBic` (or "ANY BANK") with the text representing `availableByEnumId`.
- **Tag 49 Implementation:** Always include based on `confirmationEnumId`. Default to `WITHOUT` if not specified.
- **Tag 42A Drawee:** If `draweeBic` is provided, generate Tag 42A.

### FR-SGC-05: MT707 Delta Logic Enhancement
- **Tag 26E Generation:** Always include `amendmentNumber`.
- **Tag 34B Calculation:** Generate only if the amendment results in a change to the total LC amount.

### FR-SGC-06: MT734 & MT732 Value Date (Tag 32A)
- Assemble Tag 32A using `presentationDate` (YYMMDD) + `claimCurrency` + `claimAmount`.
- Ensure the amount uses the comma `,` decimal separator.

## REQ-SGC-05: Mutual Exclusion & Conditional Guards

### FR-SGC-07: Tolerance vs Max Credit (MT700)
- If `maxCreditAmountFlag` = 'Y' (Tag 39B), then `tolerancePositive` and `toleranceNegative` (Tag 39A) must be null/zero.
- If `tolerancePositive` or `toleranceNegative` is set, `maxCreditAmountFlag` must be 'N'.

### FR-SGC-08: Available With Formats
- The system must support Option A (BIC) for `availableWithBic`.
- If `availableWithBic` is null, the system should default to "ANY BANK" or similar instruction as per bank policy.

---

## REQ-SGC-06: Verification Plan

### Automated Tests (Integration)
- **testMt700FullCompletion:** Verify generation of MT700 with all mandatory tags (40A, 41a, 49) and optional tags (43P/T).
- **testMt707Sequence:** Verify Tag 26E increments correctly across multiple amendments.
- **testMt734ValueDate:** Verify Tag 32A assembly using the presentation date.

### Manual Verification
- Verify that the UI data entry forms for Issuance, Amendment, and Presentation include the new fields.
- Verify that Layer 1 validation triggers error messages for invalid formats (e.g., non-SWIFT characters in `presentationRef`).
