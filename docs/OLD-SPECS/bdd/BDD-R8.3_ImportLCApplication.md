---
Document ID: BDD-R8.3
Version: 0.1
System: Trade Finance
Module: Letter of Credit (LC)
BRD ID: BRD-002
Status: DRAFT
Last Updated: 2026-03-14
Author: [AI Agent]
---

# BDD Scenarios: Import LC Application (R8.3)

This document contains BDD scenarios for the Import LC Application process (R8.3).
The workflow covers the end-to-end process from applicant submission to LC issuance.

---

## R8.3-UC1: Create Draft LC Application

### Feature: Create Draft LC Application

**As a** CSR (Customer Service Representative)
**I want to** input LC application details into the system
**So that** I can create a Draft LC record for further processing

**Background**
The CSR is logged into the Trade Finance system and has navigated to the "Create Import LC" screen.

#### 1. Scenario: Successful creation of a Draft LC Application (Happy Path)
**Given** the CSR has received a completed LC Application form from the Applicant
**And** the form includes all required details: Parties, Shipment, and Documents
**When** the CSR enters the valid data into all mandatory fields
**And** the CSR clicks the **"Create Draft"** button
**Then** the system should validate that all mandatory fields are present
**And** the system should create a unique LC record with status **Draft**
**And** the system should display a success message: "Draft LC Application [LC_ID] has been created successfully."
**And** the system should redirect the CSR to the **LC Detail** screen for this newly created record.

#### 2. Scenario: Creation fails due to missing mandatory data (Edge Case - Validation)
**Given** the CSR is on the "Create LC Application" screen
**When** the CSR leaves one or more mandatory fields (e.g., **Applicant**, **LC Amount**, or **Expiry Date**) empty
**And** the CSR clicks the **"Create Draft"** button
**Then** the system should NOT create a new LC record
**And** the system should highlight the empty mandatory fields
**And** the system should display an error message: "Please fill in all mandatory fields."

#### 3. Scenario: UI prevents invalid character input (Edge Case - Data Integrity)
**Given** the CSR is entering details in the **"Description of Goods"** field
**When** the CSR attempts to enter a character that is not in the SWIFT character set (e.g., `~` or `_`)
**Then** the system should display a real-time validation warning
**And** the system should prevent the record from being saved if the CSR proceeds to click **"Create Draft"**
**And** the system should display an error message: "Invalid characters detected. Please use the standard SWIFT character set only."

### Business Rules & Validation
1. **Mandatory Fields:** The following fields MUST be provided to create a Draft LC:
   - Applicant (Party)
   - Beneficiary (Party)
   - LC Amount & Currency
   - Expiry Date & Place of Expiry
   - Form of LC (e.g., Irrevocable)
2. **SWIFT Compliance:** All text data must conform to the SWIFT X-Character Set (A-Z, a-z, 0-9, `.`, `,`, `-`, `(`, `)`, `/`, `'`, `?`, `:`, `+`, space).
3. **State Transition:** Upon successful creation, the `LetterOfCredit.statusId` must be set to `LcDraft`.
4. **Ownership:** The record is initially owned by the branch/unit of the CSR who created it.

---

## R8.3-UC2: Attach Document to LC Application

### Feature: Attach Document to LC Application

**As a** CSR (Customer Service Representative)
**I want to** attach required documents to the LC Application
**So that** the application is complete and ready for approval

**Background**
The CSR has created a Draft LC Application and is on the LC Detail screen.

#### 1. Scenario: Successfully attach a document to LC Application (Happy Path)
**Given** the CSR is on the LC Detail screen for a Draft LC
**When** the CSR clicks the **"Attach Document"** button
**And** the CSR selects a valid file (PDF, JPG, PNG) under 10MB
**And** the CSR enters a document description
**And** the CSR clicks **"Upload"**
**Then** the system should store the document file
**And** the system should create a record in the `LcDocument` entity
**And** the system should display the document in the attachments list
**And** the system should display a success message: "Document uploaded successfully."

#### 2. Scenario: Upload fails due to invalid file type (Edge Case - Validation)
**Given** the CSR is on the document upload dialog
**When** the CSR selects a file with extension `.exe` or `.zip`
**And** the CSR clicks **"Upload"**
**Then** the system should reject the file upload
**And** the system should display an error message: "Invalid file type. Only PDF, JPG, and PNG files are allowed."

#### 3. Scenario: Upload fails due to file size exceeding limit (Edge Case - Validation)
**Given** the CSR is on the document upload dialog
**When** the CSR selects a file larger than 10MB
**And** the CSR clicks **"Upload"**
**Then** the system should reject the file upload
**And** the system should display an error message: "File size exceeds the maximum limit of 10MB."

### Business Rules & Validation
1. **Allowed File Types:** PDF, JPG, PNG only.
2. **Maximum File Size:** 10MB per document.
3. **Mandatory Documents:** The following documents are required before submission:
   - Application Form (signed)
   - Credit Agreement (if applicable)
4. **Document Status:** Each attached document should have a status: `Pending`, `Verified`, `Rejected`.

---

## R8.3-UC3: Manage Customer Credit Limits

### Feature: Manage Customer Credit Limits

**As a** CSR (Customer Service Representative)
**I want to** manage collateral information and verify credit limits
**So that** the LC application has sufficient backing before submission

**Background**
The CSR is processing an LC Application and needs to verify credit limits.

#### 1. Scenario: Successfully retrieve credit limit from CBS (Happy Path)
**Given** the CSR has entered the Applicant's information
**And** the Applicant has an existing Credit Agreement in the system
**When** the CSR clicks **"Check Credit Limit"**
**Then** the system should retrieve the credit limit from CBS (mocked)
**And** the system should display: Total Credit Limit, Available Limit, and Used Limit
**And** the system should calculate the available limit for this LC application

#### 2. Scenario: Customer has no existing credit agreement (Edge Case - Business Rule)
**Given** the CSR has entered the Applicant's information
**When** the CSR clicks **"Check Credit Limit"**
**And** the system finds no Credit Agreement for this customer
**Then** the system should display a warning: "No Credit Agreement found for this customer."
**And** the system should allow the CSR to proceed only if 100% cash collateral is provided.

#### 3. Scenario: Credit limit insufficient for LC amount (Edge Case - Business Rule)
**Given** the credit limit check has been performed
**When** the LC Amount exceeds the Available Credit Limit
**Then** the system should display an error: "Insufficient credit limit. LC Amount exceeds available limit by [AMOUNT]."
**And** the system should prevent the application from proceeding to approval

### Business Rules & Validation
1. **Credit Limit Check:** Must be performed before submission.
2. **Collateral Types Accepted:**
   - Cash Deposit
   - Pledge of Savings Account
   - Securities
   - Physical Gold
   - Credit Agreement
3. **100% Security:** If no Credit Agreement exists, 100% cash collateral is mandatory.

---

## R8.3-UC4: Application Approval Routing

### Feature: Application Approval Routing

**As a** CSR (Customer Service Representative)
**I want to** submit the LC Application for approval through the proper channels
**So that** the application is reviewed by the appropriate authorities

**Background**
The LC Application is complete with all documents and credit verification.

#### 1. Scenario: Successfully submit application to Supervisor (Happy Path)
**Given** the LC Application has all required documents attached
**And** the credit limit check has passed
**And** the CSR clicks **"Submit for Approval"**
**When** the system validates all required fields and checks pass
**Then** the system should create a workflow task for the Supervisor
**And** the system should change the application status to **"Pending Supervisor Approval"**
**And** the system should notify the Supervisor

#### 2. Scenario: Submit fails due to missing documents (Edge Case - Validation)
**Given** the LC Application is missing required documents
**When** the CSR clicks **"Submit for Approval"**
**Then** the system should display an error: "Cannot submit. Please attach all required documents."
**And** the system should list the missing documents

#### 3. Scenario: Supervisor approves and forwards to IPC (Edge Case - Workflow)
**Given** the Supervisor has reviewed the application
**And** all checks pass
**When** the Supervisor clicks **"Approve and Forward to IPC"**
**Then** the system should create a task for IPC
**And** the system should change the status to **"Pending IPC Approval"**
**And** the system should notify IPC

#### 4. Scenario: Supervisor rejects application (Edge Case - Workflow)
**Given** the Supervisor has reviewed the application
**When** the Supervisor clicks **"Reject"** and provides a reason
**Then** the system should change the status to **"Rejected"**
**And** the system should send notification to CSR
**And** the system should allow CSR to resubmit after corrections

### Business Rules & Validation
1. **Approval Levels:** CSR → Supervisor → IPC (3-level approval)
2. **Rejection Reason:** Mandatory when rejecting.
3. **Time Limit:** Each approval level should have a SLA (to be defined).

---

## R8.3-UC5: Provision & Charge Assessment

### Feature: Provision & Charge Assessment

**As an** IPC (International Processing Center) Officer
**I want to** calculate provision and charges for the LC Application
**So that** the customer can make the required payments before LC issuance

**Background**
The LC Application has been approved by Supervisor and is pending IPC processing.

#### 1. Scenario: Successfully calculate provision and charges (Happy Path)
**Given** the LC Application is in "Pending IPC Approval" status
**When** the IPC Officer clicks **"Calculate Provision & Charges"**
**Then** the system should calculate the provision amount based on LC amount and product type
**And** the system should calculate the issuance charges based on the charge template
**And** the system should display: Provision Amount, Charge Amount, Total Amount Due
**And** the system should hold the provision amount in CBS (mocked)

#### 2. Scenario: Customer requests to view charge breakdown (Edge Case - UI)
**Given** the calculation has been performed
**When** the CSR clicks **"View Charge Breakdown"**
**Then** the system should display a detailed breakdown:
   - Processing Fee
   - SWIFT Message Fee
   - Commission
   - Other applicable fees

#### 3. Scenario: Provision calculation fails due to CBS connection error (Edge Case - Integration)
**Given** the IPC Officer initiates calculation
**When** the CBS system is unavailable
**Then** the system should display an error: "Unable to connect to CBS. Please try again later."
**And** the system should log the error for troubleshooting

### Business Rules & Validation
1. **Provision Rate:** Typically 0-100% of LC amount based on customer risk rating.
2. **Charge Template:** Each LC product has a predefined charge structure.
3. **CBS Hold:** Provision must be held in CBS before proceeding to issuance.

---

## R8.3-UC6: System Notification for Application

### Feature: System Notification for Application

**As a** System Administrator
**I want to** ensure stakeholders receive timely notifications
**So that** the LC Application process flows smoothly

**Background**
Notifications are sent at key stages of the LC Application lifecycle.

#### 1. Scenario: Notification sent to CSR when application is approved by IPC (Happy Path)
**Given** the IPC has approved the LC Application
**When** the approval is processed
**Then** the system should send a notification to the CSR
**And** the notification should contain: LC Number, Status Update, Next Steps

#### 2. Scenario: Notification sent to Applicant when LC is ready for pickup (Edge Case - Handoff)
**Given** the LC has been issued
**When** the issuance is complete
**Then** the system should send a notification to the Applicant
**And** the notification should instruct the Applicant to collect the LC document

### Business Rules & Validation
1. **Notification Channels:** Email, System In-App Notification.
2. **Notification Recipients:** CSR, Supervisor, IPC, Applicant (as appropriate).
3. **Notification Content:** Must include LC Number, Status, and Action Required (if any).

---

## R8.3-UC7: Finalize Application

### Feature: Finalize Application

**As a** CSR (Customer Service Representative)
**I want to** finalize the LC Application after all approvals and payments
**So that** the LC is ready for issuance

**Background**
The IPC has approved the application and charges have been collected.

#### 1. Scenario: CSR prints and signs LC document (Happy Path)
**Given** the LC Application is approved and charges are paid
**When** the CSR clicks **"Print LC Document"**
**Then** the system should generate the LC document in PDF format
**And** the CSR should be able to print the document

#### 2. Scenario: Attach signed LC document after physical signing (Edge Case - Handoff)
**Given** the LC document has been printed
**And** the authorized person has signed and stamped the document
**When** the CSR scans and uploads the signed document
**Then** the system should attach the signed document to the LC record
**And** the system should mark the LC as **"Ready for Issuance"**

#### 3. Scenario: CSR marks application as finalized and ready for IPC issuance (Edge Case - Workflow)
**Given** the signed LC document is attached
**When** the CSR clicks **"Mark Ready for Issuance"**
**Then** the system should change the status to **"Ready for Issuance"**
**And** the system should notify IPC that the LC is ready to be issued

### Business Rules & Validation
1. **Physical Process:** LC must be printed, signed, and stamped by authorized personnel.
2. **Signed Document:** Must be scanned and attached before finalization.
3. **Final Status:** After finalization, the application moves to LC Issuance (R8.4).

---

## Summary

| User Case | Feature | Scenarios |
|-----------|---------|-----------|
| R8.3-UC1 | Create Draft LC Application | 3 |
| R8.3-UC2 | Attach Document to LC Application | 3 |
| R8.3-UC3 | Manage Customer Credit Limits | 3 |
| R8.3-UC4 | Application Approval Routing | 4 |
| R8.3-UC5 | Provision & Charge Assessment | 3 |
| R8.3-UC6 | System Notification for Application | 2 |
| R8.3-UC7 | Finalize Application | 3 |

**Total Scenarios:** 21

---

*End of Document*
