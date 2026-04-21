# Behavior-Driven Development (BDD) Specification
**Project Name:** Digital Trade Finance Platform
**Module:** Import Letter of Credit (LC)
**Document Version:** 1.0 (Initial BDD Derivation)
**Date:** April 21, 2026

---

## 1. Traceability Matrix

| Scenario ID | Title | Requirement Map (BRD ID) | Type |
|---|---|---|---|
| BDD-IMP-ISS-01 | Successful LC Draft Submission | REQ-IMP-FLOW-02, REQ-IMP-SPEC-01 | Happy Path |
| BDD-IMP-ISS-02 | LC Draft Submission Fails - Limit Exceeded | REQ-IMP-SPEC-01 | Edge Case |
| BDD-IMP-ISS-03 | Checker Authorizes LC Issuance | REQ-IMP-FLOW-03, REQ-UI-IMP-05 | Happy Path |
| BDD-IMP-ISS-04 | Checker Rejects LC Issuance | REQ-IMP-FLOW-01, REQ-IMP-SPEC-01, REQ-UI-IMP-05 | Edge Case |
| BDD-IMP-AMD-01 | LC Amendment Request Submission | REQ-IMP-SPEC-02 | Happy Path |
| BDD-IMP-DOC-01 | Clean Document Presentation Examination | REQ-IMP-FLOW-06, REQ-IMP-SPEC-03, REQ-UI-IMP-04 | Happy Path |
| BDD-IMP-DOC-02 | Discrepant Document Examination Logs Issue | REQ-IMP-FLOW-05, REQ-IMP-SPEC-03, REQ-UI-IMP-04 | Edge Case |
| BDD-IMP-SET-01 | Sight LC Fully Settled | REQ-IMP-FLOW-07, REQ-IMP-SPEC-04 | Happy Path |

---

## 2. Detailed BDD Scenarios

### Feature: LC Issuance Lifecycle (Draft to Issued)

#### Scenario BDD-IMP-ISS-01: Successful LC Draft Submission
**Requirement ID:** REQ-IMP-FLOW-02, REQ-IMP-SPEC-01, REQ-IMP-DEF-01
**Type:** Happy Path

* **Given** an Import LC application is in the `Draft` state
* **And** the Maker has filled all mandatory MT700 fields
* **And** the `Base Equivalent Amount` is less than or equal to the Applicant's total available `Facility Limit`
* **When** the Maker clicks "Submit for Approval"
* **Then** the Transaction State changes to `Pending Approval`
* **And** the LC Business State remains `Draft`
* **And** the requested LC amount is temporarily earmarked against the Applicant's limit to prevent over-drawing

#### Scenario BDD-IMP-ISS-02: LC Draft Submission Fails - Limit Exceeded
**Requirement ID:** REQ-IMP-SPEC-01
**Type:** Edge Case

* **Given** an Import LC application is in the `Draft` state
* **And** the `Base Equivalent Amount` is greater than the Applicant's total available `Facility Limit`
* **When** the Maker clicks "Submit for Approval"
* **Then** the system prevents submission
* **And** the system displays an error message: "Limit Exceeded: Please request a facility limit increase or select a different facility"
* **And** the transaction remains strictly in the `Draft` state

#### Scenario BDD-IMP-ISS-03: Checker Authorizes LC Issuance
**Requirement ID:** REQ-IMP-FLOW-03, REQ-UI-IMP-05
**Type:** Happy Path

* **Given** an Import LC is in the `Pending Approval` transaction state
* **And** a logged-in Checker has an Authority Tier >= the LC's Base Equivalent Amount
* **When** the Checker reviews the application in the Global Checker Queue and clicks "AUTHORIZE"
* **Then** the LC Business State changes to `Issued`
* **And** the Transaction State becomes `Processed/Closed`
* **And** the system calculates and logs the SWIFT MT700 payload for dispatch
* **And** the earmarked facility limit is permanently converted to a firm "Contingent Liability"

#### Scenario BDD-IMP-ISS-04: Checker Rejects LC Issuance
**Requirement ID:** REQ-IMP-FLOW-01, REQ-IMP-SPEC-01, REQ-UI-IMP-05
**Type:** Edge Case

* **Given** an Import LC is in the `Pending Approval` transaction state
* **When** the Checker clicks "REJECT TO MAKER" and provides a Rejection Reason
* **Then** the Transaction State changes back to `Draft`
* **And** the earmarked facility limit is immediately released back to the Available Balance
* **And** the Maker's inbox is alerted with the rejection notes

---

### Feature: Document Presentation & Examination

#### Scenario BDD-IMP-DOC-01: Clean Document Presentation Examination
**Requirement ID:** REQ-IMP-FLOW-06, REQ-IMP-SPEC-03, REQ-UI-IMP-04
**Type:** Happy Path

* **Given** an Import LC is in the business state `Documents Received`
* **When** the Trade Operations user assesses the documents against the LC terms and selects the "Clean" toggle
* **And** submits the examination decision
* **Then** the LC Business State transitions to `Accepted / Clean`
* **And** the system journals the liability for settlement based on the payment terms (Sight vs. Usance)

#### Scenario BDD-IMP-DOC-02: Discrepant Document Examination Logs Issue
**Requirement ID:** REQ-IMP-FLOW-05, REQ-IMP-SPEC-03, REQ-UI-IMP-04
**Type:** Edge Case

* **Given** an Import LC is in the business state `Documents Received`
* **When** the Trade Operations user assesses the documents and selects the "Discrepant" toggle
* **And** flags at least one ISBP Discrepancy Code (e.g., Code 12 - Late Shipment)
* **And** submits the examination decision
* **Then** the LC Business State transitions to `Discrepant`
* **And** the system places the transaction in a holding state pending an "Applicant Waiver" action
* **And** the background tariff engine applies a Discrepancy Fee charge logically

---

### Feature: Settlement

#### Scenario BDD-IMP-SET-01: Sight LC Fully Settled
**Requirement ID:** REQ-IMP-FLOW-07, REQ-IMP-SPEC-04
**Type:** Happy Path

* **Given** an Import LC is in the business state `Accepted / Clean`
* **And** the LC payment term was issued as `Sight`
* **When** the operations team triggers the Settlement execution action
* **Then** the LC Business State transitions to `Settled`
* **And** the associated Contingent Liability limit is reduced by the settled amount
* **And** the system logs a ledger posting payload for downstream Core Banking synchronization
