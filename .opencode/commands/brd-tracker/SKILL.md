# Skill: brd-tracker
**Description**: Generates an up-to-date Project Management Progress Report by cross-referencing the BRDs against the living codebase and test suites.

## Execution Steps
### 1. The "Stateless Audit" Rule (CRITICAL)
- **Do not rely on chat history, memory, or recent actions to determine progress.**
- You MUST perform a fresh, raw text search of the actual `/runtime/component/TradeFinance/` files (XML, Groovy, and Spock tests) to verify if a BRD requirement currently exists in the codebase.
- Update the Technical Design Spec (TSD) to reflex latest development if any

### 2. Read the Business Baseline
- Read BRDs in `docs/brd/`.
- Parse Requirements: Extract all items, **DON'T MISS ANY OR GROUP THEM** → auto-ID as R1.1, R1.2-[sub-id 1], R1.2-[sub-id 2], R1.3, R2.1 (handles lists/tables/free text)

### 3. Scan the Codebase & Tests
- **UI:** Check if the required Moqui XML screens exist.
- **Backend:** Check if the Groovy/XML services enforcing the business rules exist.
- **Verification:** Check if Spock test cases exist and cover the BRD rules.

### 4. Generate the Progress Matrix
Create or update the master tracking document at `docs/reports/ImplementationProgress.md`. Format the report using the following structure:

```markdown
# 📊 Implementation Progress Report
* **Project:** [This project]
* **Report Date:** [Current Date]
* **Overall Completion:** [Estimate % based on implemented features vs total features]

## BDD - Implementation Mapping

### <Document ID N> - <Document Feature N> (for each BRD document)

**Implementation Status:**

| Req ID | Description | Required Component | Implementation Status | Test Coverage | Todo / Notes |
| :--- | :--- | :--- | :--- | :--- | :--- |
| R1-BR01 |Amount > 0 | `LcServices.xml` | ✅ Implemented | ✅ `TC-SRV-02` | Validation logic verified. |
| R1.1-UIx.x |Find LC screen | `FindLc.xml` | 🚧 In Progress | ❌ Missing | Screen scaffolded, filters pending. |
| R1.2-UCx.x |Immutability| Groovy Logic | ❌ Not Started | ❌ Missing | Pending backend architecture. |

**Progress since last update:**
[describe:
- New features have been implemented
- which pending items have been completed
- which bug/issues have been resolved]

**GAP Analysis:**

[After analysing what System Would Need vs. What We Have, list all the gaps you find. For each gap, provide the following information:]

| # | Req ID |Gap | Why It's Needed | Severity |
| :--- | :--- | :--- | :---: |
| 1 | R1.1 |**Technical Design Spec (TDS)** — Complete entity definitions with ALL field names, types, and relationships | The implementation plan says "30+ SWIFT fields" but doesn't list every one. Another AI cannot create `TradeFinanceEntities.xml` without the exact field spec. | 🔴 Critical |
| 2 | R1.2 |**Service Logic Spec** — Service signatures, parameters, AND business logic pseudocode | Services like `create#LcAmendment` (shadow record cloning) have complex Groovy logic. The plan describes WHAT but not HOW. | 🔴 Critical |
| 3 | R1.3 |**Screen Architecture Spec** — Moqui XML screen patterns and widget hierarchy for each screen | Another AI needs to know wrapper vs. subscreen patterns, `conditional-field` usage, `section-iterate` for status buttons, etc. | 🟡 High |
| 4 | R1.4 |**Moqui Setup Guide** — How to clone framework, create component, configure `MoquiConf.xml` | Basic bootstrapping instructions are not documented. | 🟡 High |
| 5 | R1.5 |**Security Configuration Spec** — Roles, artifact groups, authorization mappings | The actual XML exists in `20_TradeFinanceSecurityData.xml` but is not described in any doc. | 🟡 High |

### Proposed Solution

[propose solution to close the updated GAP, e.g if the TSD is missing, propose to create it. If the implementation plan is missing, propose to create it.]

```