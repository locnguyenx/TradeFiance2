# Business Requirements Document: Import LC Amendment (SRG 2024 Enhancement)

## 1. Background & Objectives
The SWIFT Network overhauled Category 7 (Trade Finance) messages to enforce strict structured data over free text. The current Moqui-trade implementation of LC Amendments (`ImportLcAmendment`) uses generic text blocks and loose "amount adjustment" fields, which are no longer compliant with SRG 2024. Furthermore, UCP 600 makes no legal distinction between "Financial" and "Non-Financial" amendments—any external change requires Beneficiary Consent.

This enhancement restructures the system to:
1. Segregate amendments into **External** (UCP 600, generating MT 707) and **Internal** (Back-office tracking).
2. Implement a **"Smart Delta" UI** for tracking explicit narrative actions (Add, Delete, Replace All).
3. Ensure automated logging of Beneficiary Consent via incoming SWIFT (MT 730).

## 2. Scopes and Consent Rules
### 2.1 External (UCP 600) Amendments
- **Definition:** Any change to the legal text of the LC (Financial, Logistics, Narrative, or Charges).
- **Consent Rule:** ALL external amendments require Beneficiary Consent.
- **Workflow:** Draft -> Pending Approval -> Dispatched (MT 707) -> Consent Logged (MT 730) -> Consent Approved -> Merged.

### 2.2 Internal Bank Amendments
- **Definition:** Changes to bank back-office tracking (Facility ID, Fee Account, Margin Account).
- **Consent Rule:** None.
- **Workflow:** Draft -> Pending Approval -> Executed (Immediately overwrites Master LC).

## 3. Inputs Capture & Payload Builder (External)
### 3.1 Financial & Date Deltas
Replaces the old `amountAdjustment` with strictly separated `amountIncrease` and `amountDecrease`.
- **Decrease Rule:** Must not exceed Current Unutilized Balance. Earmarks are released ONLY upon Beneficiary Consent.
- **Increase Rule:** Limit earmarks must be blocked upon Dispatch (before consent).

### 3.2 Track Changes Narrative Engine (Smart Delta)
Instead of typing `/ADD/`, the Maker selects an action dropdown and types the text. The system stores:
- `ActionEnum` (ADD, DELETE, REPLACE_ALL)
- `DeltaText` (The raw text)

## 4. Traceability
- **FR-AMD-01**: Segregate External and Internal Amendment models.
- **FR-AMD-02**: Enforce "Smart Delta" structured fields for narrative tags.
- **FR-AMD-03**: Auto-log Beneficiary Consent via SWIFT MT 730.
