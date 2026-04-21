---
Document ID: BDD-<Req Id, or NEW for adhoc requirement>-<Feature>
Version: 0.1
System: Trade Finance
Module: Letter of Credit (LC)
BRD ID: (get from BRD if any, ADHOC for adhoc requirement)
Status: DRAFT
Last Updated: 2026-03-10
Author: [LocNX]
---

# BDD Scenarios

This document is a collection of BDD scenarios for a requirement (Req Id).
Each scenario is written in the Gherkin format and follows the Given-When-Then structure.

[If a requirement has multiple use cases / processing steps, create 1 section as following for each use case/processing step]

## R8.3-UC1 Create Draft LC Application 

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
**When** the CSR attempts to enter a character that is not in the SWIFT character set (follow SWIFT Compliance)
**Then** the system should display a real-time validation warning
**And** the system should prevent the record from being saved if the CSR proceeds to click **"Create Draft"**
**And** the system should display an error message: "Invalid characters detected. Please use the standard SWIFT character set only."

## Business Rules & Validation
(All Business Rules & Validation used in scenarios above. In scenario, we will refer to a rule/validation in this section)
1. **Mandatory Fields:** The following fields MUST be provided to create a Draft LC:
   - Applicant (Party)
   - Beneficiary (Party)
   - LC Amount & Currency
   - Expiry Date & Place of Expiry
   - Form of LC (e.g., Irrevocable)
2. **Compliance:** List all compliance requirements for the LC Application transaction.
* **SWIFT Compliance:** "Not Applicable" or "Compliant"
Example for "Compliant": All text data must conform to the SWIFT X-Character Set (A-Z, a-z, 0-9, `.`, `,`, `-`, `(`, `)`, `/`, `'`, `?`, `:`, `+`, space).
3. **State Transition:** Upon successful creation, the `LetterOfCredit.statusId` must be set to `LcDraft`.
4. **Ownership:** The record is initially owned by the branch/unit of the CSR who created it.
