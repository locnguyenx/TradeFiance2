---
Document ID: BDD-R8.5_LCAmendment
Version: 0.1
System: Trade Finance
Module: Letter of Credit (LC)
BRD ID: BRD-002-R8.5
Status: DRAFT
Last Updated: 2026-03-17
Author: [LocNX]
---

# BDD Scenarios - LC Amendment Feature

This document contains comprehensive BDD scenarios for the LC Amendment feature based on:
- 12 Business Rules from 01-business-requirements.md
- 6 Stakeholders analysis
- 12-Step Process Flow from 03-future-state.md
- Feature Prioritization (P0-P1-P2)
- Technical Entities and Services from 04-technical-discovery.md
- Risk Assessment and Integration Points

## R8.5-UC1 Create LC Amendment Draft

### Feature: Create LC Amendment Draft

**As a** CSR (Customer Service Representative)
**I want to** create an amendment draft linked to an existing issued LC
**So that** I can initiate the amendment request for further processing

**Background**
The CSR is logged into the Trade Finance system with appropriate permissions. The LC to be amended must be in "Issued" status. This covers BR-1, BR-3, and the Step 1-2 of the 12-step process flow.

#### 1. Scenario: Successful creation of Amendment Draft (Happy Path)
**Given** the LC "LC-2026-001" exists with status "Issued"
**And** the LC is not currently locked for another amendment
**And** the CSR has received a completed amendment request from the Applicant
**When** the CSR initiates an amendment request for LC "LC-2026-001"
**And** the CSR enters valid amendment details (new expiry date, increased amount)
**And** the CSR clicks the **"Create Draft"** button
**Then** the system should validate that the LC status is "Issued" (BR-1)
**And** the system should create an LcAmendment record with amendment number "1"
**And** the system should create a shadow copy of all LC fields in the amendment (BR-8)
**And** the system should set amendment status to "Draft"
**And** the system should acquire an amendment lock on the LC (LcAmendmentLock entity)
**And** the system should generate Mantle Request (RqtLcAmendment) for workflow routing
**And** the system should display success message: "Amendment Draft [AMND-001] has been created successfully."
**And** the system should redirect the CSR to the Amendment Detail screen.

#### 2. Scenario: Creation fails when LC is not in Issued status (Edge Case - State Validation)
**Given** the LC "LC-2026-002" exists with status "Draft"
**When** the CSR attempts to create an amendment for LC "LC-2026-002"
**And** the CSR clicks the **"Create Draft"** button
**Then** the system should NOT create an amendment record
**And** the system should display error message: "Amendment cannot be created. LC must be in Issued status." (BR-1)
**And** the system should not acquire any lock on the LC.

#### 3. Scenario: Creation fails when LC is already locked for amendment (Edge Case - Concurrency)
**Given** the LC "LC-2026-003" has an active LcAmendmentLock record
**And** the lock is held by user "john.doe" with lock expiry "2026-03-17T15:00:00"
**When** the CSR attempts to create an amendment for LC "LC-2026-003"
**And** the CSR clicks the **"Create Draft"** button
**Then** the system should NOT create an amendment record
**And** the system should call acquire#AmendmentLock service to check lock status
**And** the system should display error message: "LC is currently locked for amendment by john.doe. Please try again later."
**And** the system should display lock expiry time: "Lock expires at 15:00 today."

#### 4. Scenario: Immutable fields cannot be modified in amendment (Edge Case - Data Integrity - BR-3)
**Given** the CSR is on the Amendment Edit screen for LC "LC-2026-001"
**When** the CSR attempts to modify immutable fields: LC Number, Applicant, Currency, Issuing Bank, or Advising Bank
**Then** the system should make these fields read-only
**And** the system should display a tooltip on each immutable field: "This field cannot be changed via amendment."
**And** the system should prevent saving any changes to these fields
**And** the service check#AmendmentLockStatus should validate immutable field attempts.

#### 5. Scenario: Amendment request for expired LC (Edge Case - P2 Priority)
**Given** the LC "LC-2026-004" exists with status "Issued" but expiry date "2026-01-01" has passed
**When** the CSR attempts to create an amendment for LC "LC-2026-004"
**Then** the system should display warning: "This LC has expired. Creating amendment may require special approval."
**And** the system should allow the CSR to proceed with a confirmation dialog
**And** the system should flag the amendment for additional verification.

#### 6. Scenario: Duplicate amendment request detected (Edge Case - Duplicate Prevention)
**Given** there is an existing amendment "AMND-001" for LC "LC-2026-001" with status "Draft"
**When** the CSR attempts to create another amendment for the same LC
**Then** the system should NOT create a duplicate amendment
**And** the system should display error message: "A pending amendment already exists for this LC. Please complete or cancel the existing amendment first."

---

## R8.5-UC2 Submit Amendment for Approval

### Feature: Submit Amendment for Approval

**As a** CSR (Customer Service Representative)
**I want to** submit the amendment draft for supervisory review
**So that** the amendment can progress through the approval chain (Step 3 of 12-step flow)

**Background**
The CSR has created an amendment draft and needs to submit it for the approval workflow (CSR → Supervisor → IPC).

#### 7. Scenario: Successful submission of amendment (Happy Path)
**Given** the CSR has created an amendment draft "AMND-001" with status "Draft"
**And** the amendment includes all required proposed changes
**And** the amendment contains valid data per SWIFT X-Character Set compliance
**When** the CSR clicks the **"Submit for Review"** button
**Then** the system should validate that all mandatory fields are complete
**And** the system should call route#AmendmentForApproval service
**And** the system should transition the amendment status to "Submitted"
**And** the system should create a Mantle Request (RqtLcAmendment) for routing
**And** the system should notify the Branch Supervisor via NotificationService
**And** the system should display success message: "Amendment submitted for review."
**And** the system should release the amendment lock (LcAmendmentLock released)

#### 8. Scenario: Submission fails due to incomplete amendment details (Edge Case - Validation)
**Given** the CSR has created an amendment draft "AMND-002" with missing required fields
**When** the CSR clicks the **"Submit for Review"** button
**Then** the system should NOT submit the amendment
**And** the system should highlight the missing mandatory fields
**And** the system should display error message: "Please complete all mandatory fields before submission."

#### 9. Scenario: Amendment with invalid SWIFT characters (Edge Case - SWIFT Compliance)
**Given** the CSR is entering amendment details
**When** the CSR enters characters not in the SWIFT X-Character Set (e.g., special symbols)
**Then** the system should display real-time validation warning
**And** the system should prevent submission with invalid characters
**And** the system should display error message: "Invalid characters detected. Please use the standard SWIFT character set only."

---

## R8.5-UC3 Supervisor Review

### Feature: Supervisor Review Amendment

**As a** Branch Supervisor
**I want to** review and approve or reject amendment requests
**So that** I can ensure proper authorization before forwarding to IPC (Step 4 of 12-step flow)

**Background**
The Branch Supervisor receives notification of pending amendment requests requiring review. This covers BR-2 (Approval Workflow).

#### 10. Scenario: Supervisor approves amendment (Happy Path)
**Given** there is a submitted amendment "AMND-001" awaiting Supervisor review
**And** the LC has sufficient credit limit for the proposed changes
**When** the Supervisor reviews the amendment details via AmendmentSupervisorReview.xml screen
**And** the Supervisor clicks the **"Approve"** button
**And** the Supervisor enters review comments: "Approved - amount increase within limit"
**Then** the system should call review#LcAmendmentBySupervisor service
**And** the system should validate the credit limit impact
**And** the system should transition amendment status to "Supervisor Approved" (LcTxSupervisorApproved)
**And** the system should create audit trail entry via record#AmendmentAction
**And** the system should notify the IPC for final approval
**And** the system should display success message: "Amendment approved and routed to IPC."

#### 11. Scenario: Supervisor rejects amendment (Edge Case - Business Decision)
**Given** there is a submitted amendment "AMND-001" awaiting Supervisor review
**When** the Supervisor clicks the **"Reject"** button
**And** the Supervisor enters rejection reason: "Amount increase exceeds credit limit"
**Then** the system should transition amendment status to "Rejected" (LcTxRejected)
**And** the system should preserve the original LC terms (BR-8)
**And** the system should call record#AmendmentAction for audit
**And** the system should notify the CSR of the rejection
**And** the system should display message: "Amendment rejected. Applicant has been notified."

#### 12. Scenario: Supervisor validates collateral and provision impact (Edge Case - Financial Validation - BR-12)
**Given** there is a submitted amendment "AMND-001" that increases LC amount by 50%
**When** the Supervisor reviews the amendment
**Then** the system should display collateral impact assessment
**And** the system should calculate required provision adjustment via adjust#ProvisionsForAmendment service
**And** the system should show CBS fund hold status (hold#Funds integration)
**And** the system should display warning if provisions need increase: "Additional provision of [AMOUNT] required."

#### 13. Scenario: Supervisor reviews amendment field changes (Edge Case - P2 Priority)
**Given** there is a submitted amendment "AMND-001"
**When** the Supervisor views the amendment details
**Then** the system should call get#AmendmentFieldChanges service
**And** the system should display a comparison: Original Value vs. Proposed Value for each changed field
**And** the system should highlight fields that have been modified from original LC.

---

## R8.5-UC4 IPC Final Approval

### Feature: IPC Final Approval of Amendment

**As an** IPC (International Processing Center) Officer
**I want to** provide final approval for amendment requests
**So that** the amendment can be executed and beneficiary notified (Step 5-6 of 12-step flow)

**Background**
The IPC has final authority for amendment approval and SWIFT MT707 generation. This covers BR-5, BR-6.

#### 14. Scenario: IPC approves amendment and generates MT707 (Happy Path - BR-5, BR-6)
**Given** there is a Supervisor-approved amendment "AMND-001" awaiting IPC review
**And** the beneficiary acceptance window has not expired (tracked via track#BeneficiaryAcceptanceWindow)
**When** the IPC officer reviews the amendment
**And** the IPC officer clicks the **"Approve"** button
**And** the IPC enters approval comments: "Approved for execution"
**Then** the system should call approve#LcAmendmentByIpc service
**And** the system should validate beneficiary acceptance window not expired
**And** the system should transition amendment status to "IPC Approved" (LcTxIpcApproved)
**And** the system should generate SWIFT MT707 message via SwiftServices.generate#SwiftMt707
**And** the system should store MT707 as attachment to LC record
**And** the system should notify the Advising Bank
**And** the system should display success message: "Amendment approved. SWIFT MT707 generated."

#### 15. Scenario: IPC rejects amendment (Edge Case - Final Rejection)
**Given** there is a Supervisor-approved amendment "AMND-001" awaiting IPC review
**When** the IPC officer clicks the **"Reject"** button
**And** the IPC enters rejection reason: "Non-compliant amendment terms"
**Then** the system should transition amendment status to "Rejected"
**And** the system should preserve the original LC terms
**And** the system should notify the Supervisor and CSR
**And** the system should display message: "Amendment rejected at IPC level."

#### 16. Scenario: MT707 generation fails during approval (Edge Case - Integration Failure - Risk Mitigation)
**Given** the IPC officer clicks the **"Approve"** button
**When** the SWIFT integration fails during MT707 generation
**Then** the system should rollback the approval transaction
**And** the amendment status should remain unchanged
**And** the system should display error message: "MT707 generation failed. Transaction rolled back. Please retry or contact system administrator."
**And** the system should log the failure for troubleshooting
**And** the system should alert the operator via NotificationService.

#### 17. Scenario: Beneficiary acceptance window expires before IPC approval (Edge Case - Time Boundary)
**Given** the amendment "AMND-001" has beneficiaryAcceptanceExpiry set to "2026-03-15T23:59:59"
**And** the current date is "2026-03-16"
**When** the IPC attempts to approve the amendment
**Then** the system should prevent approval
**And** the system should display error message: "Beneficiary acceptance window has expired. Amendment cannot be approved."
**And** the system should transition amendment status to "Expired".

---

## R8.5-UC5 Beneficiary Acceptance

### Feature: Beneficiary Acceptance of Amendment

**As a** Beneficiary (Exporter)
**I want to** accept or reject the amendment per UCP 600 Article 10
**So that** the amended terms take effect or original terms are preserved (Step 7-9 of 12-step flow)

**Background**
The Advising Bank has notified the Beneficiary of the amendment. The Beneficiary has a defined acceptance window. This covers BR-7, BR-8.

#### 18. Scenario: Beneficiary accepts amendment (Happy Path - BR-7)
**Given** the Beneficiary has received notification of amendment "AMND-001" via SWIFT MT707
**And** the acceptance window is still open (beneficiaryAcceptanceExpiry not passed)
**When** the Beneficiary submits acceptance through the Advising Bank
**And** the Bank records the beneficiary response via record#BeneficiaryResponse service
**Then** the system should create LcAmendmentBeneficiaryResponse record with responseTypeEnumId "LcAmndAccept"
**And** the system should set confirmation status to "Beneficiary Accepted" (LcAmndAccept)
**And** the system should notify IPC for confirmation
**And** the system should display message: "Beneficiary acceptance recorded. Amendment will be applied upon confirmation."

#### 19. Scenario: Beneficiary rejects amendment (Edge Case - BR-8)
**Given** the Beneficiary has received notification of amendment "AMND-001"
**When** the Beneficiary submits rejection with reason: "Terms not acceptable to us"
**Then** the system should create LcAmendmentBeneficiaryResponse record with responseTypeEnumId "LcAmndReject"
**And** the system should set confirmation status to "Beneficiary Rejected" (LcAmndReject)
**And** the system should preserve all original LC terms (BR-8)
**And** the system should notify all parties (Applicant, CSR, Supervisor, IPC) via NotificationService
**And** the system should display message: "Amendment rejected by Beneficiary. Original LC terms remain in effect."
**And** the amendment should NOT be applied to the LC.

#### 20. Scenario: Beneficiary acceptance window expires (Edge Case - Time Boundary)
**Given** the beneficiary acceptance window has expired for amendment "AMND-001"
**And** the system runs track#BeneficiaryAcceptanceWindow scheduled job
**When** the system processes the expired window
**Then** the system should mark the confirmation status as "LcAmndPendingBen" expired
**And** the system should preserve original LC terms
**And** the system should notify IPC and Supervisor
**And** the system should display message: "Beneficiary acceptance window expired. Amendment not applied."

#### 21. Scenario: Multiple beneficiary responses tracked (Edge Case - P2 Priority)
**Given** the Beneficiary has submitted multiple responses to the same amendment
**When** the system records a new response
**Then** the system should maintain all responses in LcAmendmentBeneficiaryResponse
**And** the system should consider only the latest response as effective
**And** the system should display full response history in AmendmentDetail.xml.

---

## R8.5-UC6 Confirm Amendment Application

### Feature: Confirm Amendment Application

**As an** IPC (International Processing Center) Officer
**I want to** confirm the amendment application after beneficiary acceptance
**So that** the amended terms become effective on the LC (Step 10-11 of 12-step flow)

**Background**
The beneficiary has accepted the amendment. IPC now confirms application of changes to the master LC record. This covers BR-4, BR-5, BR-11, BR-12.

#### 22. Scenario: Successful amendment confirmation (Happy Path - BR-4, BR-5)
**Given** the beneficiary has accepted amendment "AMND-001"
**When** the IPC officer clicks the **"Confirm Amendment"** button
**Then** the system should call confirm#AmendmentApplication service
**And** the system should apply the approved amendment fields to the LC (excluding immutable fields - BR-3)
**And** the system should increment the amendment number (from 0 to 1) (BR-4)
**And** the system should transition LC status from "Issued" to "Amended" (BR-5)
**And** the system should update the effective terms display via LcAmendmentFullDetailView (BR-9)
**And** the system should recalculate charges based on amendment via FinancialServices.calculate#LcCharges (BR-11)
**And** the system should adjust provisions if amount changed via adjust#ProvisionsForAmendment (BR-12)
**And** the system should post accounting entries to CBS via GL posting integration
**And** the system should create audit trail entries via record#AmendmentAction
**And** the system should notify all parties (Applicant, CSR, Supervisor) (Step 12)
**And** the system should display success message: "Amendment confirmed. LC effective terms updated."

#### 23. Scenario: CBS integration fails during confirmation (Edge Case - Integration Failure - Risk Mitigation)
**Given** the IPC officer clicks the **"Confirm Amendment"** button
**When** the CBS integration fails during provision adjustment (adjust#ProvisionsForAmendment)
**Then** the system should rollback the entire confirmation transaction
**And** the LC should retain its original status and terms
**And** the amendment status should remain "Awaiting Confirmation"
**And** the system should display error message: "CBS integration failed. Transaction rolled back. Please contact system administrator."
**And** the system should log the failure for reconciliation.

#### 24. Scenario: Partial approval of amendment fields (Edge Case - P2 Priority)
**Given** the amendment "AMND-001" contains multiple proposed changes
**When** the IPC confirms the amendment with selective field approval
**Then** the system should apply only the approved fields
**And** the system should reject conflicting changes
**And** the system should document which fields were applied vs. rejected
**And** the system should notify the Applicant of partial application.

#### 25. Scenario: Concurrent amendment prevented during confirmation (Edge Case - Concurrency)
**Given** the beneficiary has accepted amendment "AMND-001"
**And** another user attempts to create a new amendment for the same LC
**When** the system checks the amendment lock status via check#AmendmentLockStatus
**Then** the system should prevent creation of the new amendment
**And** the system should display message: "LC is being processed for amendment confirmation. Please try again later."

---

## R8.5-UC7 View Amendment History and Effective Terms

### Feature: View Amendment History and Effective Terms

**As a** User (CSR, Supervisor, IPC, Applicant, Beneficiary)
**I want to** view amendment history and current effective terms
**So that** I can understand the complete amendment timeline and current LC state (BR-9, BR-10)

**Background**
Users need visibility into amendment history, original terms, and effective terms (original + applied amendments).

#### 26. Scenario: View complete amendment history (Happy Path - BR-9)
**Given** the LC "LC-2026-001" has undergone multiple amendments
**When** the user navigates to the AmendmentHistory.xml screen
**Then** the system should call get#AmendmentHistory service
**And** the system should display a chronological list of all amendments
**And** for each amendment, show: Amendment number, Status, Created date, Approved date, Beneficiary response
**And** the system should show field-level changes via get#AmendmentFieldChanges
**And** the system should allow filtering by date range and status.

#### 27. Scenario: View effective LC terms (Happy Path - BR-9)
**Given** the LC "LC-2026-001" has an accepted amendment applied
**When** the user views the LC effective terms
**Then** the system should query LcAmendmentFullDetailView
**And** the system should display the current effective LC terms
**And** the system should highlight fields that have been amended
**And** the system should show the original value vs. amended value for changed fields
**And** the system should display the amendment number of the last applied amendment.

#### 28. Scenario: View original LC terms (Happy Path - BR-9)
**Given** the LC "LC-2026-001" has amendments applied
**When** the user clicks on **"View Original Terms"**
**Then** the system should display the original LC terms as issued
**And** the system should indicate these are the original terms
**And** the system should show the issuance date.

#### 29. Scenario: Read-only context when viewing LC from amendment (Edge Case - BR-10)
**Given** the user is viewing the LC Detail screen from an Amendment context
**When** the user attempts to edit LC fields
**Then** the system should enforce read-only mode via AmendmentDetail.xml context flag
**And** the system should disable all edit buttons and form fields
**And** the system should display a banner: "Viewing LC in amendment context - edits disabled" (BR-10).

---

## R8.5-UC8 Amendment Charges and Provisions

### Feature: Amendment Charges and Provisions

**As an** IPC Officer
**I want to** view and process amendment-related charges and provisions
**So that** proper fees are collected and provisions are adjusted (BR-11, BR-12)

**Background**
Amendments may incur fees and impact credit provisions, especially when LC amount changes.

#### 30. Scenario: Charges calculated for amendment (Happy Path - BR-11)
**Given** an amendment "AMND-001" has been approved with changes to amount and expiry
**When** the system calculates amendment charges via FinancialServices.calculate#LcCharges
**Then** the system should apply the product-specific charge template
**And** the system should calculate: Amendment fee, Additional commission, Cable charges
**And** the system should display the total charges in Financials.xml screen
**And** the system should create accounting entries for charge collection via CBS integration.

#### 31. Scenario: Provisions adjusted when amount increased (Edge Case - BR-12)
**Given** amendment "AMND-001" increases the LC amount from USD 100,000 to USD 120,000
**When** the system calls adjust#ProvisionsForAmendment service
**Then** the system should calculate new provision requirement at product percentage
**And** the system should initiate CBS fund hold for additional amount via hold#Funds
**And** the system should release the old hold and create new hold
**And** the system should display provision adjustment summary
**And** the system should log the provision change for audit via record#AmendmentAction.

#### 32. Scenario: No provision adjustment when amount unchanged (Edge Case - Optimization)
**Given** amendment "AMND-001" only changes expiry date (no amount change)
**When** the system processes the amendment confirmation
**Then** the system should NOT initiate any CBS fund hold adjustment
**And** the system should skip provision recalculation
**And** the system should proceed directly to confirmation
**And** the system should log: "No provision adjustment required - amount unchanged."

#### 33. Scenario: Exchange rate lookup for multi-currency amendments (Edge Case - CBS Integration)
**Given** the amendment "AMND-001" changes LC currency from USD to EUR
**When** the system processes the amendment
**Then** the system should call get#ExchangeRate service from CBS
**And** the system should validate the exchange rate is available
**And** the system should display the converted amount for confirmation.

---

## R8.5-UC9 Amendment Lock Management

### Feature: Amendment Lock Management

**As a** System Administrator
**I want to** manage amendment locks in case of system failures
**So that** LCs can be unlocked for processing when locks are stale

**Background**
Amendment locks prevent concurrent amendments but may become stale due to system failures or user abandonment.

#### 34. Scenario: Force release stale lock (Edge Case - Administrative)
**Given** the LC "LC-2026-001" has a stale lock held by disconnected user "john.doe"
**And** the lock was acquired at "2026-03-17T10:00:00" with 30-minute timeout
**When** the Administrator clicks the **"Force Release Lock"** button
**And** the Administrator enters reason: "User session expired - stale lock"
**Then** the system should call forceRelease#AmendmentLock service
**And** the system should release the lock immediately
**And** the system should create audit trail entry for administrative force release via record#AmendmentAction
**And** the system should notify the original lock holder if they reconnect.

#### 35. Scenario: Lock automatically expires (Edge Case - Timeout)
**Given** a user acquired an amendment lock with 30-minute timeout at "10:00"
**When** 30 minutes pass without lock release
**Then** the system should automatically expire the lock via lockExpiry check
**And** the lock record (LcAmendmentLock) should be cleaned up
**And** the lock should be available for other users
**And** the system should log the automatic expiration.

#### 36. Scenario: Lock acquisition timeout (Edge Case - Concurrency)
**Given** the LC "LC-2026-001" has no active lock
**When** the CSR attempts to create an amendment
**Then** the system should call acquire#AmendmentLock service
**And** the system should acquire lock with configurable timeout
**And** the lock should be associated with the CSR's userId.

---

## R8.5-UC10 Amendment Workflow Notifications

### Feature: Amendment Workflow Notifications

**As a** Stakeholder (Applicant, CSR, Supervisor, IPC)
**I want to** receive notifications about amendment status changes
**So that** I am informed of progress and required actions (Step 12 of 12-step flow)

**Background**
Multiple stakeholders need to be notified at different stages of the amendment workflow.

#### 37. Scenario: Notification sent to Supervisor on submission (Happy Path)
**Given** the CSR submits an amendment for review
**When** the system processes the submission
**Then** the system should call NotificationService
**And** the notification should be sent to the Branch Supervisor
**And** the notification should include: LC number, Amendment number, Summary of changes, Link to review screen.

#### 38. Scenario: Notification sent to Applicant on confirmation (Happy Path - Step 12)
**Given** the IPC confirms the amendment application
**When** the system finalizes the amendment
**Then** the system should send notification to the Applicant via NotificationService
**And** the system should send notification to the Beneficiary via Advising Bank
**And** the notifications should include: Confirmation message, Summary of changes applied, New effective terms summary.

#### 39. Scenario: Notification on beneficiary rejection (Edge Case)
**Given** the Beneficiary rejects the amendment
**When** the system processes the rejection
**Then** the system should notify the Applicant: "Your amendment request has been rejected by the Beneficiary."
**And** the system should notify the CSR and Supervisor
**And** the notifications should include the beneficiary's reason for rejection.

---

## Business Rules & Validation

All Business Rules from 01-business-requirements.md implemented in scenarios above:

1. **BR-1 Amendment Request Creation:** CSR must create amendment request linked to existing issued LC; LC status remains unchanged until approval - **Scenarios 1, 2**
2. **BR-2 Approval Workflow:** All amendment requests must follow CSR → Supervisor → IPC approval chain - **Scenarios 7, 10, 14**
3. **BR-3 Immutable Fields:** LC Number, Applicant, Currency, Issuing Bank, Advising Bank cannot be changed via amendment - **Scenario 4**
4. **BR-4 Amendment Numbering:** Each approved amendment increments the amendment number - **Scenarios 1, 22**
5. **BR-5 Status Transition:** Upon IPC approval, LC status transitions from "Issued" to "Amended" - **Scenarios 14, 22**
6. **BR-6 SWIFT MT707 Generation:** System must generate SWIFT MT707 upon approval - **Scenarios 14, 16**
7. **BR-7 Beneficiary Acceptance:** Per UCP 600 Article 10, beneficiary may accept or reject amendment - **Scenarios 18, 19**
8. **BR-8 Original Terms Preservation:** If amendment is rejected, original LC terms remain in effect - **Scenarios 11, 19, 20**
9. **BR-9 Effective Terms Display:** Users can view Original LC terms, Latest effective terms, and full amendment history - **Scenarios 26, 27, 28**
10. **BR-10 Read-Only Context:** When viewing LC from amendment context, display must be read-only - **Scenario 29**
11. **BR-11 Charge Assessment:** Amendment may incur fees; charges calculated based on product template - **Scenarios 30, 32**
12. **BR-12 Provision Impact:** Amendment approval may impact credit limits, collateral, and provisions - **Scenarios 12, 22, 31**

---

## Stakeholder Coverage

| Stakeholder | Scenarios | Role in Workflow |
|-------------|-----------|-----------------|
| Applicant (Importer) | 5, 19, 31, 38, 39 | Requests amendment, receives notifications, affected by charges/provisions |
| CSR (Customer Service Representative) | 1-9, 37 | Creates amendment drafts, submits for approval |
| Branch Supervisor | 10-13, 37 | Reviews and approves/rejects amendments |
| IPC (International Processing Center) | 14-17, 22-25, 30-32, 38 | Final approval, confirms application, manages financials |
| Beneficiary (Exporter) | 18-21, 38 | Accepts or rejects amendment per UCP 600 |
| Advising Bank | 14, 18 | Receives MT707, notifies beneficiary |

---

## Priority-Based Scenario Coverage

| Priority | Feature | Scenario Count | Scenarios |
|----------|---------|----------------|-----------|
| **P0 - MVP** | Amendment request creation | 5 | 1-5 |
| **P0 - MVP** | Basic approval workflow | 7 | 6-12 |
| **P0 - MVP** | Immutable field protection | 1 | 4 |
| **P0 - MVP** | Amendment numbering | 1 | 1 |
| **P0 - MVP** | SWIFT MT707 generation | 3 | 14, 16 |
| **P1** | Beneficiary acceptance workflow | 4 | 18-21 |
| **P1** | Read-only context enforcement | 1 | 29 |
| **P1** | Concurrent amendment prevention | 3 | 3, 25, 35-36 |
| **P2** | Enhanced MT707 format | Covered in | 14 |
| **P2** | Partial approval support | 1 | 24 |
| **P2** | Amendment history view | 3 | 26-28 |
| **P2** | Expired LC validation | 1 | 5 |

---

## Risk-Based Scenario Coverage

| Risk | Severity | Mitigation Scenario |
|------|----------|---------------------|
| Beneficiary rejection after system processing | High | Scenario 19 - preserves original terms |
| SWIFT MT707 format non-compliance | Medium | Scenario 14 - MT707 generation with validation |
| Concurrent amendment conflicts | High | Scenarios 3, 25 - Lock acquisition and conflict detection |
| CBS integration failure during provision adjustment | High | Scenario 23 - Transaction rollback on CBS failure |
| Incorrect immutable field enforcement | Medium | Scenario 4 - Immutable field protection |
| Beneficiary acceptance window expiration | Low | Scenario 20 - Expiry handling |
| Charge calculation errors | Medium | Scenario 30 - Charge calculation validation |
| Audit trail gaps | Low | All approval scenarios - Comprehensive audit trail |
| Notification delivery failures | Low | Scenarios 37-39 - Notification system |
| Role-based authorization bypass | Medium | All scenarios - Role-based access via service auth |

---

## Integration Point Coverage

| Integration | Service/Entity | Scenarios |
|-------------|----------------|-----------|
| CBS - Exchange rate lookup | get#ExchangeRate | 33 |
| CBS - Fund hold/release | hold#Funds / release#Funds | 12, 31 |
| CBS - Accounting entries | GL posting | 22, 30 |
| SWIFT - MT707 transmission | SwiftServices.generate#SwiftMt707 | 14, 16 |
| Mantle Workflow - Request routing | route#AmendmentForApproval | 7 |
| Notification System | NotificationService | 10, 19, 37-39 |
| Lock Management | LcAmendmentLock entity | 1, 3, 34-36 |
| Audit Trail | record#AmendmentAction | 10, 11, 22, 34 |

---

## Technical Entity References

| Entity | Purpose | Used In Scenarios |
|--------|---------|-------------------|
| LcAmendment | Main amendment record | All scenarios |
| LcAmendmentBeneficiaryResponse | Beneficiary responses | 18, 19, 20, 21 |
| LcAmendmentLock | Concurrency control | 1, 3, 34, 35, 36 |
| LcAmendmentFullDetailView | Effective terms display | 27 |
| LcAmendmentChangesView | Field change tracking | 13, 26 |
| LetterOfCredit | Main LC entity | 1, 2, 22, 27 |

## Technical Service References

| Service | Purpose | Used In Scenarios |
|---------|---------|-------------------|
| create#LcAmendmentDraft | Creates amendment draft | 1 |
| route#AmendmentForApproval | Routes amendment workflow | 7 |
| review#LcAmendmentBySupervisor | Supervisor review | 10 |
| approve#LcAmendmentByIpc | IPC approval | 14 |
| record#BeneficiaryResponse | Records beneficiary response | 18, 19 |
| confirm#AmendmentApplication | Applies amendment to LC | 22 |
| track#BeneficiaryAcceptanceWindow | Tracks acceptance window | 17, 20 |
| adjust#ProvisionsForAmendment | Adjusts provisions | 12, 22, 31 |
| acquire#AmendmentLock | Acquires lock | 1, 36 |
| release#AmendmentLock | Releases lock | 7 |
| check#AmendmentLockStatus | Checks lock status | 3, 25 |
| forceRelease#AmendmentLock | Force releases lock | 34 |
| record#AmendmentAction | Audit trail | 10, 11, 22, 34 |
| get#AmendmentHistory | Retrieves history | 26 |
| get#AmendmentFieldChanges | Gets field changes | 13, 26 |
| SwiftServices.generate#SwiftMt707 | Generates MT707 | 14, 16 |
| FinancialServices.calculate#LcCharges | Calculates charges | 30 |

---

**Last Updated**: 2026-03-17
