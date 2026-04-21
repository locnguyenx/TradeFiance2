---
Document ID: BDD-R8.3-UC1
Version: 0.1
System: Trade Finance
Module: Letter of Credit (LC)
BRD ID: BRD-002
Status: DRAFT
Last Updated: 2026-03-13
Author: [LocNX]
---

# BDD Scenarios: Create Draft LC Application (R8.3-UC1)

**Requirement:** R8.3-UC1 (Importer submits LC Application, CSR inputs data and creates Draft LC)

This document is a collection of BDD scenarios for this requirement.
Each scenario is written in the Gherkin format and follows the Given-When-Then structure.

## Feature: Create Draft LC Application

**As a** CSR (Customer Service Representative)
**I want to** input LC application details into the system
**So that** I can create a Draft LC record for further processing

### Background
The CSR is logged into the Trade Finance system and has navigated to the "Create Import LC" screen.

### Scenario: Successful creation of a Draft LC Application (Happy Path)
**Given** the CSR has received a completed LC Application form from the Applicant
**And** the form includes all required details: Parties, Shipment, Documents, Amount, and Product selection
**When** the CSR enters the valid data into all mandatory fields
   **And** the CSR clicks the **"Create Draft"** button
**Then** the system should validate that all mandatory fields are present
   **And** the system should create a unique LC record with status **Draft**
   **And** the system should set the **transactionStatusId** to **Draft**
   **And** the system should automatically generate an **Issuance Charge** (LC_CHG_ISSUANCE)
   **And** the system should record an initial history entry in **LcHistory** tracking the **Draft** status.
   **And** the system should display a success message: "Draft LC Application [LC_ID] has been created successfully."
   **And** the system should redirect the CSR to the **LC Detail** screen for this newly created record.

### Scenario: Creation fails due to missing mandatory data (Edge Case - Validation)
**Given** the CSR is on the "Create LC Application" screen
**When** the CSR leaves one or more mandatory fields (e.g., **Applicant**, **LC Amount**, or **Expiry Date**) empty
   **And** the CSR clicks the **"Create Draft"** button
**Then** the system should NOT create a new LC record
   **And** the system should highlight the empty mandatory fields
   **And** the system should display an error message: "Please fill in all mandatory fields."

### Scenario: Creation fails due to invalid SWIFT characters (Edge Case - Data Integrity)
**Given** the CSR is entering details for a new LC application
**When** the CSR attempts to input a character outside the SWIFT X-Character Set (e.g., `@` or `#` in names)
   **And** the CSR triggers the **"validate#LetterOfCredit"** or **"create#LetterOfCredit"** service
**Then** the system should display an error message: "Invalid characters detected. Please use the standard SWIFT character set only."
   **And** NO LC record should be created.

### Scenario: Creation fails due to length or date constraints (Edge Case - Validation)
**Given** the CSR is entering data for a new LC application
**When** the CSR enters an **LC Number** exceeding 16 characters
   **Or** the CSR enters an **Applicant Name** exceeding 140 characters
   **Or** the CSR enters an **Expiry Date** that is before the **Issue Date**
**Then** the system should return a validation error for the specific field
   **And** NO LC record should be created.

### Scenario: Successful validation of Amount Tolerance format
**Given** the CSR is entering data for a new LC application
**When** the CSR enters a valid **Amount Tolerance (39A)** format such as "5/5"
**Then** the system should accept the input as valid during validation.

### Scenario: Update Draft LC Application (Management)
**Given** a Draft LC has been successfully created
**When** the CSR updates a field (e.g., **Applicant Name**) using the **"update#LetterOfCredit"** service
**Then** the system should update the field in the database
   **And** the system should record a history entry in **LcHistory** with the **Update** change type.

### Scenario: Delete Draft LC Application (Maintenance)
**Given** a Draft LC exists with **transactionStatusId** set to **LcTxDraft**
**When** the CSR calls the **"delete#LetterOfCredit"** service
**Then** the system should permanently remove the LC record
   **And** the system should return a success status.

### Scenario: Delete fails for non-Draft LC (Service Rule)
**Given** an LC exists with a status other than **Draft** (e.g., **Applied** or **Closed**)
**When** a user attempts to call the **"delete#LetterOfCredit"** service
**Then** the system should return an error message
   **And** the LC record should NOT be deleted.

---

## Business Rules & Validation
1. **Mandatory Fields:** The following fields MUST be provided to create a Draft LC:
   - productId (e.g., PROD_ILC_SIGHT)
   - Applicant (Party)
   - Beneficiary (Party)
   - LC Amount & Currency
   - Expiry Date & Place of Expiry
   - Form of LC (e.g., Irrevocable)
2. **Compliance:** 
* **SWIFT Compliance:** All text data that binds to SWIFT messages must conform to the SWIFT X-Character Set (A-Z, a-z, 0-9, `.`, `,`, `-`, `(`, `)`, `/`, `'`, `?`, `:`, `+`, space).
3. **State Transition:** 
* Initial LC Status: `Draft`.
* Initial Transaction Status: `Draft`.
4. **Automation:**
   * **Automated Charges:** `LC_CHG_ISSUANCE` is calculated and recorded upon LC creation based on the configuration.
5. **Auditing:** Every status change, creation, and update event must be logged in the `LcHistory` entity.
6. **Deletion Rule:** Only LCs in **Draft** status can be deleted. This prevents data loss for processed records.
7. **Ownership:** The record is initially owned by the branch/unit of the CSR who created it, linked via the user's login context.
