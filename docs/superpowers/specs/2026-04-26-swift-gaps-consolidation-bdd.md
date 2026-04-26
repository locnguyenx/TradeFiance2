# Behavior Driven Development (BDD) Spec
**Project Name:** Digital Trade Finance Platform
**Module:** SWIFT Gaps Consolidation (Validation & Generation)
**Document Version:** 1.0
**Date:** April 26, 2026
**Traceability:** Validates requirements defined in `2026-04-26-swift-gaps-consolidation-brd.md`.

---

## 1. User Stories & Scenario Mapping

| User Story | Scenario Title | Requirement ID |
|:---|:---|:---|
| SWIFT Compliance | Mandatory MT700 Field Validation | FR-ENT-21, FR-ENT-23, FR-ENT-26 |
| SWIFT Compliance | Available With BIC Logic | FR-ENT-22, FR-SGC-08 |
| SWIFT Compliance | Mutual Exclusion: Tolerance vs Max Credit | FR-SGC-07 |
| Amendment Management | Amendment Sequencing and Totals | FR-ENT-28, FR-ENT-29, FR-SGC-05 |
| Presentation Management | Presentation Reference and Date Validation | FR-ENT-30, FR-ENT-31 |
| Message Generation | MT734 Notice of Refusal Assembly | FR-SGC-06 |

---

## 2. BDD Scenarios

### Feature: SWIFT-Compliant Data Entry (Validation Layer 1)

#### Scenario: Validate mandatory MT700 fields during issuance
**Type:** Happy Path
**Story:** Issuance Validation
**Traceability:** FR-ENT-21, FR-ENT-23, FR-ENT-26
- **Given** a Maker is drafting a new Import LC
- **When** the Maker attempts to save the LC without specifying `lcTypeEnumId`, `availableByEnumId`, or `confirmationEnumId`
- **Then** the system must block the save
- **And** display error messages for the missing mandatory fields

#### Scenario: Validate Available With BIC logic
**Type:** Happy Path
**Story:** Routing Validation
**Traceability:** FR-ENT-22, FR-SGC-08
- **Given** a Maker is drafting a new Import LC
- **When** the Maker enters "ANY BANK" in the `availableWithBic` field
- **Then** the system must accept the value
- **When** the Maker enters a BIC belonging to a non-bank party
- **Then** the system must display an error "BIC must belong to a Trade Party with Bank role"

#### Scenario: Enforce mutual exclusion between Tolerance and Max Credit
**Type:** Edge Case
**Story:** Financial Constraints
**Traceability:** FR-SGC-07
- **Given** a Maker is drafting a new Import LC
- **When** the Maker sets `maxCreditAmountFlag` to "Y"
- **And** attempts to enter a non-zero value in `tolerancePositive`
- **Then** the system must reset `tolerancePositive` to zero
- **And** display a warning "Tolerance is not allowed when 'Not Exceeding' is selected"

#### Scenario: Validate Presentation Reference and Date
**Type:** Happy Path
**Story:** Presentation Validation
**Traceability:** FR-ENT-30, FR-ENT-31
- **Given** a Maker is recording a new Document Presentation
- **When** the Maker enters a `presentationRef` starting with "/"
- **Then** the system must display an error "Reference cannot start with a slash"
- **When** the Maker enters a valid reference and a presentation date
- **Then** the system must accept the record

### Feature: SWIFT Message Generation (Layer 2)

#### Scenario: Assemble MT700 with all consolidated tags
**Type:** Happy Path
**Story:** MT700 Generation
**Traceability:** FR-SGC-04
- **Given** an authorized LC issuance with `availableWithBic` = "ANY BANK" and `availableByEnumId` = "BY_PAYMENT"
- **When** the system generates the MT700 message
- **Then** Tag 41a must contain "ANY BANK" followed by "BY PAYMENT"
- **And** Tag 49 must match the `confirmationEnumId` value

#### Scenario: Generate MT707 with amendment sequence
**Type:** Happy Path
**Story:** MT707 Generation
**Traceability:** FR-SGC-05
- **Given** an authorized amendment with `amendmentNumber` = 2
- **When** the system generates the MT707 message
- **Then** Tag 26E must contain "2"
- **And** Tag 34B must reflect the `newTotalAmount` if the LC amount changed

#### Scenario: Assemble MT734 Notice of Refusal with Value Date
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
| Validate mandatory MT700 fields | FR-ENT-21, FR-ENT-23, FR-ENT-26 | SWIFT Compliance |
| Validate Available With BIC logic | FR-ENT-22, FR-SGC-08 | SWIFT Compliance |
| Enforce mutual exclusion | FR-SGC-07 | SWIFT Compliance |
| Validate Presentation Ref and Date | FR-ENT-30, FR-ENT-31 | Presentation Management |
| Assemble MT700 with consolidated tags | FR-SGC-04 | Message Generation |
| Generate MT707 with sequence | FR-SGC-05 | Amendment Management |
| Assemble MT734 with Value Date | FR-SGC-06 | Message Generation |
