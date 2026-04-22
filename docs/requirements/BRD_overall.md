# Business Requirements Document (BRD)
**Project Name:** Digital Trade Finance Platform
**Module:** System Overall
**Document Version:** 1.0
**Date:** April 21, 2026

## 1. Executive Summary
The purpose of this project is to develop a comprehensive digital platform to manage the end-to-end lifecycle of Trade Finance products. The system will serve as the central hub for processing global trade transactions, specifically focusing on Letters of Credit (LCs) and Documentary Collections. The goal is to standardize operations, reduce manual paper processing, improve turnaround times, and provide real-time visibility into transaction statuses for all involved parties.

## 2. Project Objectives
* **Digitize Workflows:** Transition from manual, paper-based tracking to a fully digital, state-driven operational flow.
* **Risk Mitigation:** Enforce strict approval hierarchies and compliance checks at key stages of the trade lifecycle.
* **Operational Efficiency:** Standardize the capture of trade data to reduce discrepancies and rework during document presentation.
* **Customer Visibility:** Provide a transparent, traceable history of all interactions, amendments, and settlements for each trade instrument.

## 3. Scope of Work
The initial release of the platform is restricted to four core trade finance modules. 

### In-Scope Modules:
1.  Import Letter of Credit (LC)
2.  Export Letter of Credit (LC)
3.  Import Documentary Collection
4.  Export Documentary Collection

### Out-of-Scope (Phase 1):
* Guarantees and Standby LCs (SBLC)
* Supply Chain Finance / Factoring
* Trade Loans / Financing Modules

## 4. Business Requirements by Module

### 4.1. Module: Import Letter of Credit
This module manages the issuance and lifecycle of LCs on behalf of the importer (Applicant).

* **BR-IMP-01: LC Application:** The system must capture all necessary LC issuance data, including applicant/beneficiary details, amount, currency, expiry date, tenor (Sight/Usance), and required shipping documents.
* **BR-IMP-02: Approval Workflow:** Applications must pass through an internal limit checking and authorization hierarchy before formal issuance.
* **BR-IMP-03: Amendments:** The system must allow users to initiate, review, and approve amendments to active LCs (e.g., extending expiry dates, increasing amounts).
* **BR-IMP-04: Document Presentation:** The system must record the receipt of physical/digital documents from the presenting bank and route them for discrepancy checking.
* **BR-IMP-05: Discrepancy Management:** The system must allow operations teams to log document discrepancies, notify the applicant, and record the applicant's waiver or refusal.
* **BR-IMP-06: Settlement:** The system must trigger the final payment process (for Sight LCs) or maturity date tracking (for Usance LCs) upon clean document presentation or accepted waivers.

### 4.2. Module: Export Letter of Credit
This module handles LCs received in favor of the exporter (Beneficiary).

* **BR-EXP-01: LC Advising:** The system must allow operations teams to record and authenticate incoming LCs from Issuing Banks and generate an advising notice for the beneficiary.
* **BR-EXP-02: Confirmation:** The system must support adding the bank's confirmation to an Export LC, including tracking confirmation fees and limits.
* **BR-EXP-03: Document Lodgement:** The system must provide a facility to log documents submitted by the exporter against the advised LC.
* **BR-EXP-04: Document Dispatch:** The system must generate cover letters and dispatch records when sending documents to the Issuing Bank.
* **BR-EXP-05: Claim and Reconciliation:** The system must track outstanding claims and reconcile funds received from the Issuing Bank against the specific Export LC.

### 4.3. Module: Import Documentary Collection
This module manages incoming collections where the bank acts as the Collecting/Presenting Bank.

* **BR-IMC-01: Collection Registration:** The system must log incoming collection instructions from a Remitting Bank, including draft amounts, tenor, and attached commercial documents.
* **BR-IMC-02: Drawee Notification:** The system must automatically generate an arrival notice to the importer (Drawee) outlining the terms of the collection.
* **BR-IMC-03: Release of Documents:** The system must enforce business rules to release documents only against Payment (D/P) or against Acceptance (D/A).
* **BR-IMC-04: Acceptance Tracking:** For D/A collections, the system must record the importer's acceptance and track the bill until its maturity date.
* **BR-IMC-05: Remittance:** Upon payment by the importer, the system must initiate the transfer of funds back to the Remitting Bank.

### 4.4. Module: Export Documentary Collection
This module handles outgoing collections where the bank acts as the Remitting Bank on behalf of the exporter.

* **BR-EXC-01: Collection Initiation:** The system must capture the exporter's instructions, draft details, and commercial documents to be sent abroad.
* **BR-EXC-02: Dispatch to Collecting Bank:** The system must generate the outgoing collection schedule/cover letter addressed to the foreign Collecting Bank.
* **BR-EXC-03: Tracer Management:** The system must automatically schedule and generate tracers (follow-up notices) for unpaid or unaccepted collections after a predefined number of days.
* **BR-EXC-04: Status Updates:** The system must allow operations to log status updates received from the Collecting Bank (e.g., "Accepted", "Refused").
* **BR-EXC-05: Liquidation:** The system must process incoming funds and credit the exporter's account, officially closing the collection record.

## 5. General Business Rules & Cross-Functional Requirements

This Section incorporates the global trade standards and compliance frameworks. It is kept strictly focused on business logic and regulatory rules, avoiding any underlying technical or architectural specifications to ensure it is fully accessible to your business stakeholders.

### 5.1. Core Operational Rules
* **State Management:** Every transaction (LC or Collection) must have a strictly defined status (e.g., *Draft, Pending Approval, Issued, Advised, Closed, Cancelled*). Transactions cannot bypass mandatory approval statuses.
* **Audit Trail:** The system must maintain an immutable log of who created, approved, or modified any record, along with timestamp data and user identity.
* **Party Management:** The system must maintain a unified directory of all trade parties (Applicants, Beneficiaries, Issuing Banks, Advising Banks) to ensure consistent data entry and risk assessment.
* **Fee & Charge Calculation:** The system must support the configuration and application of standard trade tariffs (e.g., issuance commissions, advising fees, swift charges) based on the transaction type, tenor, and amount.

### 5.2. Industry Standards & Compliance Frameworks
To operate within the global financial ecosystem, the platform's workflows and data capture must strictly adhere to the following regulatory and standardized frameworks:

#### 1. International Chamber of Commerce (ICC) Rules
These are the universally accepted rules that govern how trade finance instruments are interpreted and handled globally. The system workflows must align with these rulebooks.
* **UCP 600 (Uniform Customs and Practice for Documentary Credits):** This is the mandatory standard governing all Letters of Credit (Import and Export). The system's rules for expiry dates, presentation periods, and discrepancy handling must adhere to UCP 600 guidelines.
* **URC 522 (Uniform Rules for Collections):** This governs Documentary Collections (Import and Export). It dictates how banks must handle commercial and financial documents, the responsibilities of the remitting and collecting banks, and protest procedures.
* **ISBP 745 (International Standard Banking Practice):** This acts as a companion to UCP 600 and provides strict guidelines on how documents should be examined for discrepancies. 
* **eUCP & eURC:** If the platform intends to handle purely electronic records rather than physical paper documents, it must comply with these digital supplements to the UCP and URC.

#### 2. Financial Messaging Standards (SWIFT)
To communicate with other banks globally, the system must generate and parse messages according to strict standardized formats.
* **SWIFT Category 7 (Documentary Credits):** The system must support generating and receiving standardized messages for LCs (e.g., MT 700 for issuance, MT 707 for amendments, MT 734 for refusal).
* **SWIFT Category 4 (Collections):** The system must support standard messages for collections (e.g., MT 400 for payment advice, MT 412 for acceptance advice).
* **ISO 20022 Migration:** The financial industry is currently migrating from legacy SWIFT MT messages to the data-rich ISO 20022 XML standard. The platform's data model must be robust enough to support this newer, more structured messaging standard for trade data.

#### 3. Regulatory & Financial Crime Compliance (AML/CFT)
Trade finance is a high-risk area for financial crime, meaning the system must integrate with or support strict compliance checks.
* **Sanctions Screening:** The system must interface with screening engines to check all parties (Applicant, Beneficiary, Banks) and vessels/ports against global sanction lists (e.g., OFAC, UN, EU).
* **Dual-Use Goods Screening:** Trade finance systems often require workflows to flag goods that could have both civilian and military applications.
* **AML (Anti-Money Laundering) & Trade-Based Money Laundering (TBML):** The system should capture granular data (pricing, vessel info, routing) to allow compliance teams to detect anomalies like over-invoicing or under-invoicing.
* **KYC / KYB (Know Your Customer / Business):** The party management module must ensure that transactions are only processed for entities that have an active, fully vetted KYC status.

#### 4. Data Privacy & Security
* **Data Protection Regulations:** Depending on where the bank and its clients operate, the system must comply with data privacy laws (like GDPR, PDPA, or local equivalents), particularly concerning the handling of personal data for company directors or individual applicants.
* **Audit & Non-Repudiation:** As outlined in your general rules, the system must ensure that no action can be deleted and that every state change is cryptographically or systematically tied to an authorized user.
