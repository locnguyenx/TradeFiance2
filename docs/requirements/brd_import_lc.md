---
Document ID: BRD-002
Version: 1.0
Module: Trade Finance
Feature: Letter of Credit (LC) - Import LC
Status: DRAFT
Last Updated: 2026-03-10
Author: [LocNX]
---

# Business Requirements Document: Trade Finance System - Import LC

## 1. Executive Summary
This document outlines the requirements for the Import Letter of Credit (LC) module, handling the full lifecycle of LCs issued by the bank for importers.

## 2. Standard Compliance
Refer to requirements in the main business requirements document.

### 2.1. MT700
Refer to file ./MT/MT700.md for details

## 3. UI/UX Requirements
- **Grouped Layout**: The detail screen MUST group fields into General, Parties, Shipment, and Docs/Payment blocks.
- **Visual Status Tracking**: Key statuses MUST be displayed using high-visibility colored chips ("Premium Status Chips") in the detail header.
- **Reusable UI Components**: Core actions (e.g., LC Creation, Amendment Initiation) MUST use standardized, reusable dialog templates to ensure a consistent user experience across different entry points.
- **Hierarchical Navigation**: The system MUST follow a `Find (List) -> Detail (Standardized Tabs)` navigation pattern.
- **Cross-Module Linkage**: When viewing linked entities (e.g., LC from an Amendment), the target entity MUST be displayed in a **Read-Only** mode to prevent accidental data corruption.
- **Data Integrity**: Fields with invalid characters must be flagged immediately upon submission.
- **Activity Log**: Immutable history of all status changes and internal comments.

## 4. User Roles & Permissions
Define who will use the system and what business actions they are allowed to perform.

| User Role | Description | Permitted Actions |
| :--- | :--- | :--- |
| **Applicant** | Corporate Customer | Create Draft, View Own LCs, Submit Application |
| **Branch Operator** | Bank Front-Office Staff| Create Draft, View Own LCs, Submit Application |
| **Branch Supervisor** | Bank Front-Office Manager| View All LCs, Review, Reject |
| **Trade Operator** | Bank Back-Office Staff| View All LCs, Review, Reject, Issue |
| **Trade Supervisor** | Bank Back-Office Manager| View All LCs, Review, Approve, Reject |
| **Trade Auditor** | Risk/Compliance | View All LCs, View History (Read-Only) |

## 5. Business Lifecycle (State Machine)
### 5.1 Transaction Workflow Requirements

Define the statuses a record (transaction) goes through from creation to closure.

The system MUST support the following Transaction lifecycle states:
    - Draft
    - Pending Review
    - Pending Processing
    - Returned
    - Pending Approval
    - Approved
    - Rejected
    - Cancelled


**The state flow:**

| Current Status | User Action / Trigger | Next Status | Business Condition |
| :--- | :--- | :--- | :--- |
| `Draft` | Applicant or Branch Operator clicks "Submit for Review" | `Pending Review` | All mandatory application fields must be complete. |
| `Pending Review`| Branch Supervisor clicks "Submit for Processing" | `Pending Processing` | Requires Branch Supervisor authority. |
| `Pending Processing`| Trade Operator clicks "Submit for Approval" | `Pending Approval` | Requires Trade Operator authority. |
| `Pending Approval`| Trade Supervisor clicks "Approve" | `Approved` | Requires Trade Supervisor authority. |
| `Pending Review`/`Pending Approval`/`Pending Processing`  | Authorized user clicks "Reject" | `Rejected` | User must provide a rejection reason. |
| `Pending Review`/`Pending Approval`/`Pending Processing`  | Authorized user clicks "Return" | `Returned` | User must provide a returnning reason. |


### 5.2 LC Status Requirements
* The system MUST support the following LC lifecycle states:
    - Draft
    - Applied
    - Issued
    - Advised
    - Amended
    - Negotiated
    - Revoked
    - Expired

* **When the LC Application transaction is not Approved:** LC status is Draft
* **The LC status flow when the LC Application transaction is Approved:**

| Current Status | User Action / Trigger | Next Status | Business Condition |
| :--- | :--- | :--- | :--- |
| `Draft` | Trade Supervisor do "Approve" the transaction | `Applied` | Transaction status must be `Approved` |
| `Applied` |  Authorized user (i.e Trade Operator) do "Issue" the LC | `Issued` | Funds must be successfully reserved. |
| `Issued` |  Authorized user (i.e Trade Operator) complete "Amendment" for the LC | `Amended` | The Amendment must be confirmed |


## 7. Functionalities
  - Describe the functionalities of the system, including the business process flow, User Interface & Data Requirements.

### 7.1 Import LC - Overall Business Process Flow

Step 1: The exporter and importer sign a trade contract, clearly stating that the payment method is Letter of Credit.

Step 2: The importer requests the Issuing Bank to issue an LC with the exporter as the beneficiary.

Step 3: After checking and evaluating the documents, the Issuing Bank issues the LC and notifies the Advising Bank, usually designated by the beneficiary.

Step 4: The Advising Bank informs the beneficiary that the LC has been issued and commits to payment.

Step 5: The exporter delivers the goods to the importer and prepares a delivery document set, in which Bill of Lading is included.

Step 6: The exporter delivers the delivery document set to the Advising Bank, the Negotiating Bank, and the Presenting Bank, so that the Advising Bank can forward and present it to the Issuing Bank to claim payment.

Step 7: After checking the Documents, the receiving banks (Advising Bank, Negotiating Bank, and Presenting Bank) will notify the Beneficiary of the Documents for correction or discrepancy (if any). If it is correct, they will proceed with discounting (Negotiation) of the Documents or send a request for payment via SWIFT message, or send the Documents to request payment.

Step 8: The Issuing Bank receives the Documents, checks it, and if correct, will proceed with sight payment or acceptance of payment.

Step 9: If there is a discrepancy, the bank will send a SWIFT message to inform the advising bank and the applicant. If the applicant accepts the discrepancy, they will proceed with handing over the Documents to the applicant for payment.

Step 10: If the two parties don't have mutual agreement on the discrepancy, the bank will return the Documents according to the instructions of the presenting bank. If the LC allows for payment via SWIFT message, upon receiving the request for payment, the Issuing Bank will proceed with payment or authorize the reimbursing bank to make the payment.

### 8.2 Import LC Product
LC Pre Advice, LC Sight, LC Usance, LC Negotiation, LC Standby
Each product has its own configuration, such as:
- Available With / By
- Mixed Payment Details
- Negotiation/Deferred Payment Details
- Provision percentage
- Documents required
- Charge template
- SWIFT message type

### 8.3 LC Application
#### Business process flow

1. Step 1: The importer (Applicant) submits an LC Application form specifying all required LC terms, including parties, shipment details, and document requirements.
2. Step 2: The CSR (Customer Service Representative) inputs the data into the system, creating a Draft LC.
3. Step 3: The CSR attachs required documents according to the bank's current regulations for this LC product.
4. Step 4: The CRS inputs additional information for this LC application request:
- If the customer has already issued an LC 
- If 100% of the LC value requested is secured by a deposit/pledging of a savings account, securities, physical gold, or foreign currency
- If the customer (applicant) has signed a credit agreement in the form of a guarantee. If not, it's mandatory to sign one and attach it to the application. Retrieve the Credit Limit information which is sanctioned for this credit agreement in CBS and update the available credit limit for this LC application request.

5. Step 5: The CSR submits the application to the Supervisor for approval. After approval, the application is submitted to the IPC for processing.

6. Step 6: The IPC processes the application and updates the status of the application, calculate charge and provision, and notify CSR to work with customer for this.

7. Step 7: The CSR works with customer to collect the charge and provision:
- Check that the customer's account has sufficient funds, if not, notify customer to deposit the funds;
- Request the customer to sign confirmation of agreement to pay.
- Once the charge and provision are collected, update the status of the application and circulate to IPC for LC issuance.

8. Step 8: The IPC does the following:
- Check the application and documents.
- Proceed LC Issuance
9. Step 9: Once the LC issuance is completed, system notify related users. 
10. Step 10: The CSR print out the LC document, get signature and stamp from authorized person, and give the original LC to the customer. A scanned copy of signedLC document is attached to the LC record in the system.

#### System Use Cases
- **R8.3-UC1: Create Draft LC Application**: (Steps 1-2) CSR inputs data, system validates, and creates a Draft LC.
- **R8.3-UC2: Attach Document to LC Application**: (Step 3, 10) CSR attaches necessary documents (e.g. Applicant requests, Signed LC scan) to the LC record.
- **R8.3-UC3: Manage Customer Credit Limits**: (Step 4) CSR inputs collateral info, retrieves approved credit limit from CBS, updates available limit.
- **R8.3-UC4: Application Approval Routing**: (Step 5) Process flows from CSR -> Supervisor -> IPC.
- **R8.3-UC5: Provision & Charge Assessment**: (Steps 6-7) IPC calculates amounts, CSR validates customer funds and collects signs.
- **R8.3-UC6: System Notification for Application**: (Steps 6, 9) System notifies CSR and other related users upon processing/issuance events.
- **R8.3-UC7: Finalize Application**: (Steps 8, 10) IPC verifies and proceeds; CSR handles physical printing and signing.

### 8.4 LC Issuance
#### Business process flow

1. Step 1: IPC user reviews the LC application and documents, complete inputting the LC details, and save the LC record as in draft status.
2. Step 2: Once all LC details are completed, IPC user submits the LC application for approval.
   - The system validates the input against SWIFT MT700 rules and internal policies.
   - The system calculates the required provision and places a hold on the customer's account via the Core Banking System.
   - The system calculates upfront issuance charges if customer's deposit balance is sufficient. If not, notify CSR to work with customer for this.
3. Step 3: The IPC supervisor reviews the application and approves it. 
4. Step 4: The system transitions the LC Status from `Applied` to `Issued` and the Transaction Status to `Closed`.
   - The system generates the standard SWIFT MT700 message and attaches it to the LC record for transmission.
   - The system generate accounting entries in CBS for the contingent entries of LC issuance, and  charges and provision if any.

#### System Use Cases
- **R8.4-UC1: Draft & Review Issuance details**: (Step 1) IPC user reviews the approved application and saves LC record as draft.
- **R8.4-UC2: Submit Issuance & Automated CBS Hooks**: (Step 2) IPC user submits. System runs SWIFT rules, holds CBS provision accounts, and calculates upfront charges.
- **R8.4-UC3: Supervisor Final Approval**: (Step 3) Final review by IPC supervisor.
- **R8.4-UC4: Issue LC Instrument & MT700**: (Step 4) System transitions status to `Closed`, LC Status to `Issued`, generates MT700, and posts internal ledger (accounting) entries in CBS.

#### Automated System Processes
Define the chain of background processing the system must perform without direct human intervention:

##### Process 1: LC Issuance Finalization
* **Trigger:** The LC status changes to `Issued`.
* **Processing Steps:**
  1. Generate the official LC PDF document (MT700 SWIFT Message is generated during the issuance service).
  2. Activate Provisions: Transition all held funds for provisions (`LcPrvHeld`) to active status (`LcPrvActive`).
  3. Contingent Accounting: Post contra-entries in CBS (mocked) for the full LC amount to track off-balance sheet liability.
      - Debit: `CONTINGENT_ASSET_ACC`
      - Credit: `CONTINGENT_LIAB_ACC`
  4. Deduct issuance fees: Collect calculated charges and notify CBS for ledger updates.
  5. Send an email/system notification to the Applicant and related users.
* **Expected Result:** The LC is officially active, fees are collected, contingent liability is recorded, and all parties are notified. If any critical accounting step fails, the issuance service must roll back.

### 8.5 LC Amendment
#### Business process flow

1. Step 1: The importer (Applicant) submits an amendment request to the Issuing Bank, specifying the changes (e.g., extend expiry date, increase amount, change shipment terms). The RM reviews the request for completeness and feasibility.

2. Step 2: The CSR creates a new amendment `Request` in the system linked to the existing LC. The amendment details (old value vs. new value for each changed field) are recorded. The LC's `lcStatusId` remains unchanged until the amendment is finalized.

3. Step 3: The amendment `Request` follows the same approval workflow as issuance (CSR → Supervisor → IPC). The approval authority reviews the impact on credit limits, collateral, and provisions.

4. Step 4: Upon approval by IPC, the system updates the amended fields on the `LetterOfCredit` entity, increments the amendment number, transitions `lcStatusId` to `Amended`, and generates SWIFT MT707 (Amendment to Documentary Credit) to notify the Advising Bank.

5. Step 5: The Advising Bank advises the beneficiary of the amendment. Per UCP 600 Article 10, the beneficiary may accept or reject the amendment. If rejected, the original LC terms remain in effect.

6. Step 6: The CSR archives the amendment documentation and updates the LC file.

#### Supplemental Info

- Once LC is issed, it can't be changed. In business, we usually call the issued LC as original LC, or LC document
- To amend a issued LC, we make an admendment request. Basically, we can amend most of LC document details, except some info like LC Number, Applicant, Currency, Issuing Bank, Advising Bank... are not allowed to change
- When admendment request is approved (in step 4), we mark the original LC as Amended. Amendment details of each time (tracked by sequence number) will be maintained separatedly
- In step 5, when the amendment is confirmed by Advising bank, this amendment take effect for the LC in further processing i.e accouting entries, payment... If the amendment is rejected, the original LC terms plus last confirmed ammendment remain in effect.
- As a result, when user view a LC record in the system, user will have serveral option:
  - Original LC (term at issuance time)
  - Latest effective term: the original LC terms plus last confirmed ammendment
  - and a list of amendment for this LC

#### System Use Cases
- **R8.5-UC1: Create Amendment Request**: (Steps 1-2) Create `LcAmendment` record and linked `Request`.
- **R8.5-UC2: Process Amendment Approval**: (Step 3) Workflow from CSR -> Supervisor -> IPC.
- **R8.5-UC3: Finalize Amendment & MT707**: (Step 4) Apply shadow record field changes, update amendment number, and generate SWIFT MT707.
- **R8.5-UC3a: Manage Read-Only LC Access**: Ensure users can view the original LC context from an amendment in a robust, non-editable mode.

- **R8.5-UC4: Standardized Amendment View**: The system MUST provide a consistent list view of Amendments (both in the main Find screen and the LC Detail tab) including Request ID, Amendment Number, Date, and Status tracking for both internal processing and counterparty confirmation.
- **R8.5-UC5: Unified Amendment Initiation**: "Initiate New Request" MUST use a shared, context-aware dialog that automatically handles the LC link when initiated from within a specific LC record.

### 8.6 LC Expiry
#### Business process flow

1. Step 1: A scheduled service runs daily to identify LCs where `dateOfExpiry` has passed and there are no pending drawings (`LcDrawing` with status not in `Paid`, `Accepted`).

2. Step 2: For each qualifying LC, the system transitions `lcStatusId` from `Issued` (or `Amended`) to `Expired`.

3. Step 3: The system generates a notification to the RM and CSR, and optionally releases any remaining provisions/collateral holds via CBS integration.

#### System Use Cases
- **R8.6-UC1: Automated Expiry Processing**: (Steps 1-2) Periodic job to transition qualifying LCs to `Expired`.
- **R8.6-UC2: Notify Expiry & Release Provisions**: (Step 3) Notify stakeholders and release CBS funds hold.

#### Automated System Processes
Define the chain of background processing the system must perform without direct human intervention:

##### Process 1: LC Expiration
* **Trigger:** LC is on expiry date
* **Processing Steps:**
  1. Change LC Status to `Expired`
  2. Release the provision
  3. Send an email notification to the Applicant and the Beneficiary.
  4. Log the transaction in the bank's general ledger.
* **Expected Result:** The LC is expired, provision amount is paid back to customer account, the GL is balanced, and all parties are notified. If step 2 (Release the provision) fails, the entire process must halt and alert the Operator.

### 8.7 LC Revocation
#### Business process flow

1. Step 1: For revocable LCs (rare under UCP 600), the Issuing Bank may revoke the LC at any time before documents are presented. The operator initiates a revocation request.

2. Step 2: The revocation `Request` follows the standard approval workflow. Upon approval, `lcStatusId` transitions to `Revoked`.

3. Step 3: SWIFT MT799 (Free Format Message) is sent to the Advising Bank to notify of revocation. Provisions are released.

#### System Use Cases
- **R8.7-UC1: Initiate LC Revocation**: (Steps 1-2) Operator creates revocation request, approved via standard workflow.
- **R8.7-UC2: Execute Revocation & MT799**: (Step 3) Transition to `Revoked`, generate SWIFT MT799, release provisions.


### 8.8 Document Presentation (Drawings)

This function is for LC Payment & Drawings

#### Business process flow

1. Step 1: The Advising/Presenting Bank sends the document set (Bill of Lading, Commercial Invoice, Packing List, Insurance Certificate, Certificate of Origin, etc.) to the Issuing Bank, usually accompanied by a SWIFT MT750 (Advice of Discrepancy) or MT754 (Advice of Payment/Acceptance/Negotiation).

2. Step 2: The IPC operator registers the incoming documents as an `LcDrawing` record in the system, linking it to the corresponding LC. The drawing is assigned a `drawingSeqId` and status `Received`.

3. Step 3: The IPC officer examines the documents against the LC terms within 5 banking days (per UCP 600 Article 14). Each document is checked for:
   - Compliance with LC conditions (goods description, quantity, amount)
   - Consistency across documents (e.g., invoice amount matches LC amount within tolerance)
   - Timeliness (presentation within the presentation period, typically 21 days after shipment)
   - Authenticity of transport documents

4. Step 4: If documents are compliant, the drawing status transitions to `Compliant` and proceeds to payment/acceptance. If discrepancies are found, the drawing status transitions to `Discrepant`.

#### System Use Cases
- **R8.8-UC1: Register Document Presentation**: (Steps 1-2) Log `LcDrawing` and attach incoming documents.
- **R8.8-UC2: Execute Document Examination**: (Steps 3-4) Compare docs against LC terms, transition to `Compliant` or `Discrepant`.


### 8.9 LC Discrepancy Handling

This function is for LC Payment & Drawings, after document presentation.

#### Business process flow

1. Step 1: When discrepancies are identified during document examination, the IPC officer records each discrepancy against the `LcDrawing`. Common discrepancies include: late presentation, inconsistent documents, missing documents, incorrect amounts, stale transport documents.

2. Step 2: The Issuing Bank sends SWIFT MT734 (Advice of Refusal) to the Presenting Bank within 5 banking days, listing all discrepancies found.

3. Step 3: The Presenting Bank and Beneficiary are given the opportunity to correct the documents and re-present, or the Applicant may waive the discrepancies.

4. Step 4: **Accept Discrepancy** — The Applicant authorizes payment despite discrepancies. The `LcDrawing` status transitions from `Discrepant` to `Accepted`. A `Request` is created for the applicant's written acceptance.

5. Step 5: **Reject Discrepancy** — The Applicant refuses. SWIFT MT734 is sent. The documents are held pending further instructions from the Presenting Bank (return, hold, or re-present).

6. Step 6: **Waive Discrepancy** — The Issuing Bank itself may waive minor discrepancies with the Applicant's consent. The drawing proceeds to payment.

7. Step 7: **Document Under Trust** — In some cases, the Issuing Bank may release discrepant documents to the Applicant under a Trust Receipt, pending formal acceptance or payment. The `LcDrawing` status transitions to `UnderTrust`.

#### System Use Cases
- **R8.9-UC1: Record Discrepancies & MT734**: (Steps 1-2) Log findings, generate Refusal SWIFT (MT734).
- **R8.9-UC2: Resolve Discrepancies**: (Steps 4-7) Process Applicant acceptance, rejection, or waiver.


### 8.10 LC Acceptance & Payment

This function is for LC Payment & Drawings, after document presentation and discrepancy handling if any.

#### Business process flow

1. Step 1: **Sight Payment** — For LCs available by sight payment (`availableWithBy_41A` = `By Payment`), upon compliant document presentation (or accepted discrepancies), the Issuing Bank debits the Applicant's account and remits payment to the Presenting Bank. SWIFT MT756 (Advice of Reimbursement or Payment) is generated.

2. Step 2: **Acceptance (Usance LC)** — For LCs available by acceptance (`availableWithBy_41A` = `By Acceptance`), the Issuing Bank accepts the draft (bill of exchange) drawn by the Beneficiary. The `LcDrawing` status transitions to `Accepted` with a `maturityDate` calculated from the `draftsAt_42C` terms (e.g., "90 days after Bill of Lading date"). Payment is made on the maturity date.

3. Step 3: **Deferred Payment** — For LCs available by deferred payment (`availableWithBy_41A` = `By Deferred Payment`), similar to acceptance but without a draft. The bank issues a deferred payment undertaking. Payment is made on the calculated `maturityDate`.

4. Step 4: **Negotiation** — For LCs available by negotiation (`availableWithBy_41A` = `By Negotiation`), the Negotiating Bank (which may be the Advising Bank) purchases the draft and/or documents from the Beneficiary. The Issuing Bank reimburses the Negotiating Bank.

5. Step 5: Upon payment completion, the `LcDrawing` status transitions to `Paid`. If the total drawings equal or exceed the LC amount (within tolerance), the `lcStatusId` transitions to `Closed` (fully utilized). Provisions are released via CBS.

#### System Use Cases
- **R8.10-UC1: Process Sight Payment & MT756**: (Step 1) Debit applicant, remit to beneficiary bank, generate MT756.
- **R8.10-UC2: Manage Acceptance/Deferred Maturity**: (Step 2) Accept drafts, calculate `maturityDate`, handle usance payments.
- **R8.10-UC3: Finalize Drawing & LC Closure**: (Step 5) Transition Statuses to `Paid`/`Closed`, release provisions.

### 8.11 Core Infrastructure

#### Manage LC Provision & Charge
##### Business process flow

1. Step 1: Upon LC issuance, the system calculates provisions based on the LC product configuration (`provisionPercentage`). A provision hold is placed on the Applicant's account via CBS integration.

2. Step 2: Charges are calculated based on the LC product's charge template. Common charges include: issuance commission, advising fee, amendment fee, negotiation/acceptance commission, discrepancy fee, courier charges.

3. Step 3: Charges are collected upfront or deducted during payment, depending on the `charges_71B` field instructions (e.g., "ALL CHARGES OUTSIDE [COUNTRY] ARE FOR ACCOUNT OF BENEFICIARY").

4. Step 4: Upon LC expiry, closure, or revocation, remaining provisions are released.

##### System Use Cases
- **R8.11-UC1: Assessment of Charges & Provisions**: (Steps 1-2) 
  - Collect charges and provisions inputted in related transaction i.e LC Application, Amendment, Drawing...
  - Automated calculation based on product templates, show infomation on the transaction
- **R8.11-UC2: CBS Accounting Integration**: (Step 3) (Integration Requirement) 
  When the transaction is Approved and transaction is Issuance, Amendment, Payment, Drawing: osting actual ledger entries of charges and provisions.
- **R8.11-UC3: CBS Accounting Integration**: (Step 4) (Integration Requirement) 
  When the transaction is Approved and transaction is Expiry, Closure, or Revocation: Posting reversal  ledger entries of charges and provisions 


### 8.12 LC Provision Collection

#### Business process flow

1. Step 1: When an LC requires provision (collateral/guarantee), the system creates a Provision Collection record linked to the LC with a target provision amount.

2. Step 2: The applicant selects multiple accounts (in different currencies) to contribute to the provision collection. The system fetches exchange rates from CBS for each currency conversion.

3. Step 3: The system calculates the converted amount for each entry and maintains a running total. The collection status is "Draft" until the total matches the target.

4. Step 4: When the total collected amount matches the target provision (within ±0.01 USD tolerance), the collection status transitions to "Complete".

5. Step 5: Upon collection completion, the system executes CBS holds for all accounts. If any hold fails, all holds are rolled back and the collection status returns to "Draft".

6. Step 6: Once funds are successfully held, the collection status transitions to "Collected". The provision is now active for the LC.

7. Step 7: Upon LC expiry, closure, or revocation, the system releases all holds and transitions the collection status to "Released".

#### System Use Cases
- **R8.12-UC1: Initialize Provision Collection**: (Steps 1-2) Create collection record, select accounts, fetch exchange rates.
- **R8.12-UC2: Add Collection Entries**: (Step 3) Add account entries with currency conversion, maintain running total.
- **R8.12-UC3: Validate Collection Total**: (Step 4) Compare total with target, apply tolerance, update status.
- **R8.12-UC4: Collect Funds from Multiple Accounts**: (Step 5) Execute CBS holds with rollback on partial failure.
- **R8.12-UC5: Release Provision Collection**: (Step 7) Release all CBS holds upon LC closure.

#### Business Rules
- **Multi-Account Support**: Applicants can contribute provisions from multiple accounts in different currencies.
- **Currency Conversion**: Exchange rates are fetched from CBS in real-time for accurate conversion.
- **Tolerance**: Collection is considered complete if total is within ±0.01 USD of target.
- **Atomic Collection**: All CBS holds must succeed, or all are rolled back (no partial holds).
- **Account Eligibility**: Only accounts owned by the LC applicant can be used for provision collection.

#### Manage LC Documents
##### Business process flow

1. Step 1: Throughout the LC lifecycle, various physical and electronic documents are generated or received: LC application form, SWIFT messages (MT700, MT707, MT750, MT756), transport documents, invoices, insurance policies.

2. Step 2: Each document is scanned and attached to the LC or Drawing record. The system tracks document type, reference number, date, and original/copy count.

3. Step 3: SWIFT messages generated by the system are stored as read-only attachments. Incoming SWIFT messages received via the messaging gateway are parsed and linked automatically.

## 9. Non-Functional Requirements

### Integration Requirements
- **SWIFT MT7**: The system MUST be able to generate SWIFT MT700 messages for LC issuance.
- **CBS**: The system MUST be able to integrate with Core Banking system to: 
    - generate accounting entries for LC issuance.
    - do payment for LC transaction.
    - retrieve customer information.
    - retrieve and update collateral information.
    - retrieve and update limit information.
    - retrieve account information and balance
    - retrieve currency exchange rate

**Last Updated:** 2026-03-16
