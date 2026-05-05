# Behavior Driven Development (BDD) Spec
**Project Name:** Digital Trade Finance Platform
**Module:** SWIFT Gaps Consolidation (Validation & Generation)
**Document Version:** 1.1
**Date:** April 26, 2026
**Traceability:** Validates requirements defined in `2026-04-26-swift-gaps-consolidation-brd.md`.

---

## 1. User Stories & Scenario Mapping

| User Story | Scenario Title | Requirement ID |
|:---|:---|:---|
| SWIFT Compliance | Validate mandatory Form of Credit (40A) | FR-ENT-21 |
| SWIFT Compliance | Validate mandatory Available By (41a) | FR-ENT-23 |
| SWIFT Compliance | Validate mandatory Confirmation (49) | FR-ENT-26 |
| SWIFT Compliance | Routing: Accept ANY BANK in Available With | FR-ENT-22, FR-SGC-08 |
| SWIFT Compliance | Routing: Validate Bank BIC in Available With | FR-ENT-22, FR-SGC-08 |
| SWIFT Compliance | Mutual Exclusion: Max Credit blocks Tolerance | FR-SGC-07 |
| Amendment Management | Validate Amendment Sequence (26E) | FR-ENT-28, FR-SGC-05 |
| Amendment Management | Validate New Total Amount (34B) | FR-ENT-29, FR-SGC-05 |
| Presentation Management | Validate Presentation Reference Format | FR-ENT-30 |
| Presentation Management | Validate Presentation Date | FR-ENT-31 |
| Message Generation | Assemble MT700 with Available With logic | FR-SGC-04 |
| Message Generation | Assemble MT734 Notice of Refusal Tag 32A | FR-SGC-06 |

---

## 2. BDD Scenarios

### Feature: SWIFT-Compliant Data Entry (Validation Layer 1)

#### Scenario: Validate mandatory Form of Credit (40A)
**Type:** Happy Path
**Story:** Issuance Validation
**Traceability:** FR-ENT-21
- **Given** a Maker is drafting a new Import LC
- **When** the Maker attempts to save the LC without specifying `lcTypeEnumId`
- **Then** the system must block the save
- **And** display error "Form of Documentary Credit (Tag 40A) is required"

#### Scenario: Validate mandatory Available By (41a)
**Type:** Happy Path
**Story:** Issuance Validation
**Traceability:** FR-ENT-23
- **Given** a Maker is drafting a new Import LC
- **When** the Maker attempts to save the LC without specifying `availableByEnumId`
- **Then** the system must block the save
- **And** display error "Availability (Tag 41a) is required"

#### Scenario: Validate mandatory Confirmation (49)
**Type:** Happy Path
**Story:** Issuance Validation
**Traceability:** FR-ENT-26
- **Given** a Maker is drafting a new Import LC
- **When** the Maker attempts to save the LC without specifying `confirmationEnumId`
- **Then** the system must block the save
- **And** display error "Confirmation Instructions (Tag 49) must be specified"

#### Scenario: Routing: Accept ANY BANK in Available With
**Type:** Happy Path
**Story:** Routing Validation
**Traceability:** FR-ENT-22, FR-SGC-08
- **Given** a Maker is drafting a new Import LC
- **When** the Maker enters "ANY BANK" in the `availableWithBic` field
- **Then** the system must accept the value

#### Scenario: Routing: Validate Bank BIC in Available With
**Type:** Edge Case
**Story:** Routing Validation
**Traceability:** FR-ENT-22, FR-SGC-08
- **Given** a Maker is drafting a new Import LC
- **When** the Maker enters a BIC belonging to a Trade Party without a Bank role in `availableWithBic`
- **Then** the system must display an error "BIC must belong to a Trade Party with Bank role"

#### Scenario: Mutual Exclusion: Max Credit blocks Tolerance
**Type:** Edge Case
**Story:** Financial Constraints
**Traceability:** FR-SGC-07
- **Given** a Maker is drafting a new Import LC
- **And** `maxCreditAmountFlag` is set to "Y"
- **When** the Maker attempts to enter a non-zero value in `tolerancePositive`
- **Then** the system must reset `tolerancePositive` to zero
- **And** display a warning "Tolerance (Tag 39A) is not allowed when 'Not Exceeding' (Tag 39B) is active"

#### Scenario: Validate Presentation Reference Format
**Type:** Edge Case
**Story:** Presentation Validation
**Traceability:** FR-ENT-30
- **Given** a Maker is recording a new Document Presentation
- **When** the Maker enters a `presentationRef` starting with "/"
- **Then** the system must display an error "Sender's Reference cannot start with a slash"

#### Scenario: Validate Presentation Date
**Type:** Happy Path
**Story:** Presentation Validation
**Traceability:** FR-ENT-31
- **Given** a Maker is recording a new Document Presentation
- **When** the Maker attempts to save without a `presentationDate`
- **Then** the system must block the save
- **And** display error "Presentation Date is required"

### Feature: SWIFT Message Generation (Layer 2)

#### Scenario: Assemble MT700 with Available With logic
**Type:** Happy Path
**Story:** MT700 Generation
**Traceability:** FR-SGC-04
- **Given** an authorized LC issuance with `availableWithBic` = "ANY BANK" and `availableByEnumId` = "BY_PAYMENT"
- **When** the system generates the MT700 message
- **Then** Tag 41a must contain "ANY BANK" followed by "BY PAYMENT"

#### Scenario: Validate Amendment Sequence Increment
**Type:** Happy Path
**Story:** Amendment Management
**Traceability:** FR-ENT-28, FR-SGC-05
- **Given** an LC with 1 existing amendment
- **When** a new amendment is drafted
- **Then** the system must auto-populate `amendmentNumber` with 2

#### Scenario: Assemble MT734 Notice of Refusal Tag 32A
**Type:** Happy Path
**Story:** MT734 Generation
**Traceability:** FR-SGC-06
- **Given** a refused presentation with `presentationDate` = "2026-04-26" and `claimAmount` = 1000.00
- **When** the system generates the MT734 message
- **Then** Tag 32A must start with "260426" (YYMMDD format)
- **And** contain the currency and amount with a comma decimal "1000,00"

---

## 3. Traceability Matrix

| Gherkin Scenario | BRD Req ID | User Story |
|:---|:---|:---|
| Validate mandatory Form of Credit | FR-ENT-21 | SWIFT Compliance |
| Validate mandatory Available By | FR-ENT-23 | SWIFT Compliance |
| Validate mandatory Confirmation | FR-ENT-26 | SWIFT Compliance |
| Accept ANY BANK in Available With | FR-ENT-22, FR-SGC-08 | SWIFT Compliance |
| Validate Bank BIC in Available With | FR-ENT-22, FR-SGC-08 | SWIFT Compliance |
| Max Credit blocks Tolerance | FR-SGC-07 | SWIFT Compliance |
| Validate Presentation Reference Format | FR-ENT-30 | Presentation Management |
| Validate Presentation Date | FR-ENT-31 | Presentation Management |
| Assemble MT700 with Available With | FR-SGC-04 | Message Generation |
| Validate Amendment Sequence | FR-ENT-28, FR-SGC-05 | Amendment Management |
| Assemble MT734 with Value Date | FR-SGC-06 | Message Generation |
