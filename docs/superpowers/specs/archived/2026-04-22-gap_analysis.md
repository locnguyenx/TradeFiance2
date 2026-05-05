# Systematic UI Gap Analysis vs Production Code

**Date:** 2026-04-22
**Objective:** Compare existing frontend implementation against [2026-04-21-ui-wireframes.md](file:///Users/me/myprojects/moqui-trade/docs/superpowers/specs/2026-04-21-ui-wireframes.md) and [2026-04-21-common-module-bdd.md](file:///Users/me/myprojects/moqui-trade/docs/superpowers/specs/2026-04-21-common-module-bdd.md).

## Executive Summary
You were entirely correct to challenge the assumption of full implementation. While the *components* exist and pass E2E tests for the "Happy Path", a rigorous comparison against the UI wireframes reveals that many enterprise UX features and System Admin modules were bypassed with static placeholders or simplified layouts. 

Below is the definitive list of mismatches that must be resolved to achieve 100% specification parity.

---

### 1. Missing System Admin & Module Navigation (REQ-UI-CMN-01)
**Requirements Spec:**
- `Trade Modules`: Import LC, Export LC (Phase 2), Collections (Phase 2)
- `Master Data`: Party Directory, Credit Facilities, Tariff Config, *Product Configuration*
- `System Admin`: *User Authority Tiers*, *Audit Logs*

**Production Reality (Gap):**
- **Missing Product Configuration:** Completely absent from [GlobalShell.tsx](file:///Users/me/myprojects/moqui-trade/frontend/src/components/GlobalShell.tsx) and routing.
- **Missing System Admin:** `User Authority Tiers` and `Audit Logs` exist only as empty backend stubs ([SystemAdminSettings.tsx](file:///Users/me/myprojects/moqui-trade/frontend/src/components/SystemAdminSettings.tsx)); they are not wired to the shell.

### 2. Global Checker Queue Interactivity (REQ-UI-CMN-02)
**Requirements Spec:**
- "Clicking a row does not open a new tab. It opens a Full-Screen Overlay (Modal) displaying the Checker Authorization Screen"

**Production Reality (Gap):**
- The `Authorize` button in [CheckersQueue.tsx](file:///Users/me/myprojects/moqui-trade/frontend/src/components/CheckersQueue.tsx) does absolutely nothing. No modal is triggered.

### 3. Party & KYC Directory Layout (REQ-UI-CMN-03)
**Requirements Spec:**
- "Right Pane (Detail View - Tabbed): Tab 1: General Info, Tab 2: Roles, Tab 3: Compliance."

**Production Reality (Gap):**
- The [PartyDirectory.tsx](file:///Users/me/myprojects/moqui-trade/frontend/src/components/PartyDirectory.tsx) right pane renders static sections side-by-side. The Tabbed interface requirement was ignored.

### 4. Credit Facility & Limit Dashboard (REQ-UI-CMN-04)
**Requirements Spec:**
- "Bottom Data Table (Utilization Breakdown): A live list of every single active transaction currently consuming this specific facility. Columns: Transaction Ref... etc. Feature: Hyperlinked Reference Numbers."

**Production Reality (Gap):**
- [LimitsDashboard.tsx](file:///Users/me/myprojects/moqui-trade/frontend/src/components/LimitsDashboard.tsx) features a bar chart instead of the required drill-down data table with active hyperlinked transactions.

### 5. Import LC Dashboard Row Actions (REQ-UI-IMP-02)
**Requirements Spec:**
- "Row Action Menu (Three dots): Context-aware actions. For an Issued LC, options are Initiate Amendment, Log Presentation, or Cancel."

**Production Reality (Gap) - As you reported:**
- The three-dots menu (`•••`) in [ImportLcDashboard.tsx](file:///Users/me/myprojects/moqui-trade/frontend/src/components/ImportLcDashboard.tsx) is an inert `<button>` element. No dropdown menu exists.

### 6. Issuance Stepper Features (REQ-UI-IMP-03)
**Requirements Spec:**
- "Step 4: Narratives... A 'Standard Clauses' button next to each text area allowing users to insert pre-approved legal text blocks."

**Production Reality (Gap):**
- [IssuanceStepper.tsx](file:///Users/me/myprojects/moqui-trade/frontend/src/components/IssuanceStepper.tsx) (Step 4) only provides raw `<textarea>` inputs. The "Standard Clauses" helper button was not implemented.

---

## 7. Deep Architecture Gap: Core Business Processes (Lifecycle States)
The original UI wireframes only covered **LC Issuance** and basic Document Examination. However, the [import-lc-brd.md](file:///Users/me/myprojects/moqui-trade/docs/superpowers/specs/2026-04-21-import-lc-brd.md) and [import-lc-bdd.md](file:///Users/me/myprojects/moqui-trade/docs/superpowers/specs/2026-04-21-import-lc-bdd.md) define an exhaustive state machine. The frontend is currently missing the entire post-issuance lifecycle.

**Missing Processes & UI Screens:**
1. **Amendments (REQ-IMP-SPEC-02):** No screen exists to process Delta Entries (increasing amounts/extending dates), routing them through the Maker/Checker queue, or logging Beneficiary Consents.
2. **Document Presentation & Waivers (REQ-IMP-SPEC-03):** 
   - No `PresentationLodgement` screen exists to log physical documents arriving at the counter.
   - No interface exists for the Applicant to review discrepant documents and formally `Waive` or `Refuse`.
3. **Settlement & Payment (REQ-IMP-SPEC-04):** No UI exists to finalize the transaction. We need a `SettlementInitiation` screen to define Value Dates, FX Rates, and Applicant Debit Accounts.
4. **Shipping Guarantees (REQ-IMP-SPEC-05):** No capability exists for the Applicant to request an early release indemnity (SG) against the LC payload.
5. **Cancellations (REQ-IMP-SPEC-06):** No UI exists for Operations to process an `Early Mutual Cancellation` and formally release limits back to the facility.

---

## Remediation Plan
To fix this, I will update the [implementation_plan.md](file:///Users/me/.gemini/antigravity/brain/222f8be2-5040-4a62-83e6-546ec9bc13b0/implementation_plan.md) to formally execute the missing features in a structured, TDD-compliant manner rather than applying quick patches. This includes scaffolding dedicated routes and components for *Amendment*, *Presentation*, *Settlement*, *Shipping Guarantees*, and *Cancellations*.
