# Corrected Gap Analysis Report: Common Module Validation Pipeline
**Date:** May 06, 2026
**Module:** Common Trade Finance (Cross-Functional Core)
**Document Version:** 1.2 (Finalized)

---

## Executive Summary

This report presents a corrected gap analysis comparing the Consolidated BRD, Consolidated BDD, and test files for the Common Trade Finance Module. The analysis identifies inconsistencies between requirements, test coverage, and traces all references to determine accurate coverage numbers.

**Key Findings:**
- Total BRD Requirements (US-*, FR-*, REQ-*): 104
- Total BDD Scenarios: 77
- Test Methods Covering BDD: 27
- Verified BRD→BDD Mismatches: 0
- True Gap Scenarios: 4 (UI-only remaining)
- Obsolete/Incorrect Tests: 0
- Orphan Tests: 0

---

## 1. BDD→BRD Traceability Validation

The report previously claimed 12 BDD scenarios referenced non-existent requirement IDs. **Validation proves this is incorrect.** The BDD file has already been updated to use User Story IDs (US-*) from the consolidated BRD.

### Verified BDD Traceability References

| BDD Scenario | Report Claimed Ref | Actual BDD Ref | Status |
|---|---|---|---|
| BDD-CMN-TXN-06 | REQ-TXN-01.1 | US-TXN-01 | ✓ Correct |
| BDD-CMN-TXN-07 | REQ-TXN-01.2 | US-TXN-02 | ✓ Correct |
| BDD-CMN-TXN-08 | REQ-TXN-02 | US-TXN-01 | ✓ Correct |
| BDD-CMN-TXN-09 | REQ-UTN-05 | US-TXN-03 | ✓ Correct |
| BDD-CMN-AUD-02 | REQ-UTN-02.1 | US-UTN-02 | ✓ Correct |
| BDD-CMN-AUD-03 | REQ-UTN-02.3 | US-UTN-02 | ✓ Correct |
| BDD-CMN-AUD-04 | REQ-UTN-03.1 | US-UTN-03 | ✓ Correct |
| BDD-CMN-AUTH-20 | REQ-COM-VAL-02 | REQ-COM-VAL-02 | ✓ Correct |
| BDD-CMN-FAC-01 | REQ-COM-ENT-03 | US-LIM-01 | ✓ Correct |
| BDD-CMN-FAC-03 | REQ-COM-NOT-01 | US-LIM-03 | ✓ Correct |

**Finding:** All BDD scenarios correctly reference either US-* or REQ-* IDs that exist in the consolidated BRD. The previous report's Section 1 findings are obsolete.

---

## 2. Contradictory BDD Scenarios

### Behavior vs Requirement Mismatch

**None identified.** The BDD scenarios generally align with the BRD requirements in terms of expected behavior.

---

## 3. Obsolete BDD Scenarios

These BDD scenarios test patterns that were explicitly changed or removed during consolidation:

| BDD Scenario | Superseded By | Notes |
|---|---|---|
| N/A | N/A | No obsolete BDD scenarios found. All current BDD scenarios correctly validate the junction pattern per consolidated BRD. |

**Finding:** The consolidated BRD replaced flat BIC fields with the junction pattern (see BRD Section 1 - Feature 1, FR-TP-03). The current BDD correctly validates the new junction pattern; no obsolete scenarios found.

---

## 4. BRD Requirements Without BDD Scenarios

The following BRD requirements have no corresponding BDD scenario:

| Requirement ID | Type | Description | Test Gap Status |
|---|---|---|---|
| US-TP-06 | User Story | BIC-to-Party Auto-Population | Frontend/UI - requires Playwright test |
| US-TP-07 | User Story | Structured Party Review for Checker | UI - requires Playwright test |
| US-TP-08 | User Story | Party Directory Tabbed Layout | UI - requires Playwright test |
| US-LIM-02 | User Story | FX Rate Stability for Earmarking | Backend - needs Spock test |
| US-AUTH-01 | User Story | Login & Session Management | Mixed - partial coverage in BDD-CMN-AUTH-08/09/10 |
| US-AUTH-02 | User Story | User Self-Service Profile | UI - partial coverage in BDD-CMN-AUTH-12/13/14 |
| US-AUTH-03 | User Story | Admin User Management | UI - partial coverage in BDD-CMN-AUTH-15/16/17/18/19 |
| US-PRD-01 | User Story | Product-Driven UI Behavior | UI - requires Playwright test |
| US-PRD-02 | User Story | Dynamic Tolerance Enforcement | Backend - partial PRD-03 covers |
| US-UTN-01 | User Story | Chronological Event Narrative | Backend - covered PRD-01 covers |
| US-UTN-02 | User Story | In-Timeline Actionability | UI - requires Playwright test |
| US-UTN-03 | User Story | Version Delta Analysis | Backend - covered PRD-04 covers |
| US-NAV-01 | User Story | KPI Dashboard | UI - requires Playwright test |
| US-NAV-02 | User Story | Global Transaction Log | UI - covered PRD-01 covers |
| US-SRH-01 | User Story | Contextual Search | UI - covered PRD-02 covers |
| US-FEE-01 | User Story | Fee Rule Management | Partial - covered PRD-12/13 |
| US-FEE-02 | User Story | Customer Exception Rates | Backend - missing explicit scenario |
| FR-TP-10 | Functional Req | SWIFT X Character Set Validation | Backend - covered by BDD-CMN-TP-03 |
| FR-TP-17 | Functional Req | SWIFT Tag Format Selection | Backend - needs test |
| REQ-COM-VAL-01 | Req | Risk & Compliance (KYC, Sanctions) | Covered by BDD-CMN-TXN-04, BDD-CMN-AUD-06 |
| REQ-COM-VAL-03 | Req | Date Sequence Logic | Backend - COVERED by BDD-CMN-VAL-04 and BDD-CMN-DATE-01 |
| REQ-COM-MAS-02 | Req | User Authority Tiers & Access | Backend - partial AUTH-01/02/03/07 covers |
| REQ-COM-NAV-01.1 | Req | KPI Dashboard | UI - missing |
| REQ-COM-NAV-01.2 | Req | Instrument Management | UI - missing |
| REQ-COM-NAV-01.3 | Req | Global Transaction Log | Backend - covered by NAV-01 |
| REQ-COM-SRH-01.2 | Req | Cross-Reference Indexing | Backend - missing explicit test |

**Summary:** 26 BRD requirements lack explicit BDD scenarios. Most are UI-focused (require Playwright tests) or infrastructure-level requirements that cannot be tested at the application level.

---

## 5. Tests Validating Obsolete Requirements

The following tests validate behavior from superseded BRDs that has been changed:

| Test File | Test Method | Obsolete BRD | Current BRD | Mismatch |
|---|---|---|---|---|
| N/A | N/A | No tests found validating obsolete patterns | Current tests correctly use junction pattern | N/A |

**Finding:** No tests were found validating the old flat BIC field pattern. All current tests validate the correct junction pattern per the consolidated BRD.

---

## 6. Orphan Tests

The following tests exist without clear BRD requirement mapping:

| Test File | Test Method | Issue |
|---|---|---|
| TradePartySpec.groovy | SC-09 "Advising bank must have RMA even if advise-through exists; Advise-through exempt" | Tests combined scenario but BDD-CMN-TP-08/09 labels don't exist in test files - test is valid but naming misalignment |
| ImportLcServicesSpec.groovy | Multiple tests using "ISSUING_BANK_001" | Tests internal system bank that is not parameterized; may be implementation detail |

---

## 7. Corrected True Gap List

Scenarios that genuinely have NO test coverage:

| Gap ID | BDD Scenario | BRD Requirement | Missing Test Type | Status |
|---|---|---|---|---|
| GAP-01 | BDD-CMN-TP-XX | US-TP-06: BIC-to-Party Auto-Population | Spock (backend) or UI test | PENDING (UI) |
| GAP-02 | BDD-CMN-FEE-01 | US-FEE-02: Customer Exception Rates | Spock | FIXED |
| GAP-04 | BDD-CMN-SRH-01 | REQ-COM-SRH-01.2: Cross-Reference Indexing | Spock | FIXED |
| GAP-05 | BDD-CMN-NAV-XX | US-NAV-01: KPI Dashboard | UI Playwright | PENDING (UI) |
| GAP-06 | BDD-CMN-AUTH-XX | US-AUTH-02: Self-service profile - change password confirmation | UI Playwright | PENDING (UI) |
| GAP-07 | BDD-CMN-UTN-XX | US-UTN-02: In-timeline authorization actions | UI Playwright | PENDING (UI) |

**Removed:** GAP-03 (Date Sequence Logic) - BDD-CMN-VAL-04 (line 333) and BDD-CMN-DATE-01 (line 350) provide coverage.

---

## 8. Corrected False Coverage List

Scenarios that appear covered but test wrong behavior or incorrect mappings:

| BDD Scenario | Test Coverage | Issue |
|---|---|---|
| BDD-CMN-TP-04 | Covered by TradePartySpec | ROLE_UNIQUENESS verified via PK enforcement test (BDD-CMN-TP-12) |
| BDD-CMN-TXN-06 | Partially covered | Validates LC creation; redirect logic is UI-specific |
| BDD-CMN-AUTH-07 | Covered by BddCommonModuleSpec | BDD-CMN-MAS-03 tests "suspended account exclusion" |

---

## 9. Test→BRD Mapping

Complete mapping from test files to BRD requirements:

| Test File | Test Method(s) | BRD Requirement(s) |
|---|---|---|
| BddCommonModuleSpec.groovy | BDD-CMN-ENT-01 through BDD-CMN-VAL-04, BDD-CMN-DATE-01 | REQ-COM-ENT-01, REQ-COM-ENT-02, REQ-COM-WF-01, REQ-COM-FX-01/02, REQ-COM-SLA-01/02, REQ-COM-NOT-01/02 |
| BddCommonModuleSpec.groovy | BDD-CMN-AUTH-01 through BDD-CMN-AUTH-04 | REQ-COM-AUTH-01, REQ-COM-AUTH-02, REQ-COM-AUTH-03A, REQ-COM-AUTH-03C |
| BddCommonModuleSpec.groovy | BDD-CMN-MAS-01 through BDD-CMN-MAS-04 | REQ-COM-MAS-01, REQ-COM-MAS-02, REQ-COM-MAS-03 |
| BddCommonModuleSpec.groovy | BDD-CMN-PRD-#id (11 data rows) | REQ-COM-PRD-01 |
| TradePartySpec.groovy | BDD-CMN-TP-01 through BDD-CMN-TP-12 | FR-TP-01, FR-TP-02, FR-TP-03, FR-TP-11, FR-TP-12 |
| TradeSearchSpec.groovy | GAP-04 | REQ-COM-SRH-01.2 |
| ImportLcServicesSpec.groovy | All tests (Parameterized) | US-TXN-01, US-TXN-02, US-TXN-03, REQ-COM-ENT-01 |

---

## 10. Recommendations (Prioritized by Severity)

### Critical (Must Fix)
1. **Fill GAP-02:** Add Spock test for Customer Exception Rates (US-FEE-02) -> **FIXED**
2. **Fill GAP-04:** Add test for Cross-Reference Indexing (REQ-SRH-01.2) -> **FIXED**

### High (Should Fix)
3. **Add role uniqueness test:** Verify PK prevents duplicate role assignments per instrument -> **FIXED**
4. **Standardize test naming:** Align TradePartySpec SC-* names with BDD scenario IDs -> **FIXED**
5. **Fix hardcoded ISSUING_BANK_001:** Parameterize party IDs in ImportLcServicesSpec -> **FIXED**

### Medium (Consider)
6. **Add UI tests:** Most gaps are UI-related (Playwright) - PENDING
7. **Simplify SC-09:** Break combined "advising bank + advise through" test into separate scenarios -> **FIXED**
8. **Document parameterization:** Ensure tests use parameterized party IDs -> **FIXED**

### Low (Nice to Have)
9. **Add KPI Dashboard test coverage** (US-NAV-01)
10. **Add Self-Service Profile confirmation test** (US-AUTH-02)
11. **Add In-Timeline Actionability test** (US-UTN-02)

---

| Appendix A: Coverage Summary | Count | % of Total |
|---|---|---|
| Total BRD Requirements | 104 | 100% |
| Requirements with BDD Coverage | 78 | 75% |
| Requirements without BDD (Gap) | 26 | 25% |
| BDD Scenarios | 77 | 100% |
| BDD Mismatches (Reference ID) | 0 | 0% |
| True Gap Scenarios | 4 | 5% |
| Tests Implemented (BDD-named) | 40 | 52% |

---

## Appendix B: Data Sources

- **BRD:** `/Users/me/myprojects/moqui-trade/docs/superpowers/specs/2026-05-05-common-consolidated-brd.md`
- **BDD:** `/Users/me/myprojects/moqui-trade/docs/superpowers/specs/2026-05-06-common-consolidated-bdd.md`
- **Test Files:** `/Users/me/myprojects/moqui-trade/runtime/component/TradeFinance/src/test/groovy/trade/`
- **Archived BRDs:** `/Users/me/myprojects/moqui-trade/docs/superpowers/specs/archived/`

---

## Correction Notes (v1.1)

This version corrects the following errors from v1.0:
1. **BRD Requirements count:** Updated from 89 to 104 (34 US-* + 32 FR-* + 38 REQ-*)
2. **BDD Scenarios count:** Updated from 81 to 77
3. **Test Methods count:** Updated from 28 to 27
4. **BDD→BRD Mismatches:** Corrected from 12 to 0 (BDD already uses valid US-*/REQ-* IDs)
5. **True Gaps:** Removed GAP-03 (Date Sequence Logic has coverage via BDD-CMN-VAL-04 and BDD-CMN-DATE-01)
6. **Section 1:** Rewritten to reflect actual BDD traceability state
7. **Section 8:** Corrected BDD-CMN-TP-04 reference (no test with that label exists)
