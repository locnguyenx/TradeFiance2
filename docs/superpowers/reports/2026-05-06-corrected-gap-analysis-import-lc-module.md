# Corrected Gap Analysis Report: Import LC Module
**Date:** May 06, 2026
**Analysis Type:** Comprehensive BRD-BDD-Test Traceability Validation

---

## 1. Executive Summary

### Coverage Overview
| Metric | Count |
|--------|-------|
| Total BRD Requirements (unique IDs) | 127 |
| Total BDD Scenarios | 41 |
| Total Test Methods (all files) | 85 |
| Requirements with BDD Coverage | 38 |
| Requirements without BDD Coverage | 89 |
| BDD Scenarios fully covered by tests | 27 |
| BDD Scenarios with partial test coverage | 8 |
| BDD Scenarios with no test coverage | 6 |

### Corrected Coverage Calculation
- **True Coverage:** 65.9% (27/41 BDD scenarios have passing tests)
- **False Coverage:** 19.5% (8/41 appear covered but tests validate wrong behavior or have quality issues)
- **Gap Coverage:** 14.6% (6/41 genuinely lack test coverage)

---

## 2. Invalid BDD→BRD References

### 2.1 Missing Requirement References
| BDD Scenario | Referenced BRD ID | Status | Notes |
|--------------|-------------------|--------|-------|
| BDD-IMP-ISS-08 | REQ-IMP-04 | NEAR MATCH | REQ-IMP-04 is about validation rules; scenario is about Vietnam FX tagging which is REQ-IMP-04 in baseline but moved to common module in v1.1 |
| BDD-IMP-SWT-01 | REQ-IMP-SWIFT-01, REQ-IMP-SWIFT-05 | EXACT MATCH | Valid reference |
| BDD-IMP-SWT-02 | REQ-IMP-SWIFT-02 | EXACT MATCH | Valid reference |
| BDD-IMP-SWT-03 | REQ-IMP-SWIFT-03A, REQ-IMP-SWIFT-05 | EXACT MATCH | Valid reference - note REQ-IMP-SWIFT-03A was split from REQ-IMP-SWIFT-03 |
| BDD-IMP-SWT-04 | REQ-IMP-SWIFT-04 | EXACT MATCH | Valid reference |
| BDD-IMP-SWT-05 | REQ-IMP-SWIFT-05 | EXACT MATCH | Valid reference |
| BDD-IMP-DOC-04 | REQ-IMP-FLOW-04 | EXACT MATCH | Valid reference |
| BDD-IMP-DOC-05 | REQ-IMP-FLOW-05 | EXACT MATCH | Valid reference |
| BDD-IMP-DOC-06 | REQ-IMP-FLOW-06 | EXACT MATCH | Valid reference |
| BDD-IMP-CAN-03 | REQ-IMP-FLOW-08 | EXACT MATCH | Valid reference |

### 2.2 Typos/Label Errors
| BDD Reference | Correct BRD ID | Issue |
|--------------|-----------------|-------|
| REQ-IMP-09 (archived) | REQ-IMP-STATE-02 | BDD-IMP-ISS-07 references REQ-IMP-STATE-02 but archived BDD shows REQ-IMP-09 |

### 2.3 Common Module Requirements Referenced
| BDD Scenario | Referenced BRD ID | Issue |
|--------------|-----------------|-------|
| ALL | REQ-COM-SLA-02 | This requirement is from common module, not Import LC-specific |

---

## 3. Contradictory BDD Scenarios

### 3.1 Behavior vs Requirement Mismatch

| BDD Scenario | Requirement Text | BDD Behavior | Issue |
|-------------|------------------|---------------|-------|
| BDD-IMP-ISS-01 | "Effective values initialized on LC" | Test creates LC and expects LC_DRAFT | The BRD states effective values initialize on "execute#IssuancePostAuth" (post-authorization), not on creation |
| BDD-IMP-ISS-03 | "Business State → LC_ISSUED, Transaction State → TRANS_APPROVED" | Test only updates businessStateId | Partial match - test doesn't verify transaction status change |
| BDD-IMP-DOC-01 | "Uses product-specific SLA days" | Test uses hardcoded 5 days | Test should fetch from TradeProductCatalog.documentExamSlaDays per BRD |
| BDD-IMP-SET-01 | "Uses usanceDays from LC" | Test uses hardcoded 14 days | Test should use actual usanceDays from LC entity |

### 3.2 Subset/Superset Issues
| BDD Scenario | Requirement Scope | Test Scope | Issue |
|-------------|------------------|-----------|-------|
| BDD-IMP-ISS-04 | "Facility earmark via effective amount" | Only tests calculation | Test doesn't verify actual facility update call |
| BDD-IMP-AMD-01 | "Financial increase updates effective amount" | Tests +20,000 increase | Should also test negative delta per BRD spec |
| BDD-IMP-SG-01 | "110% over-indemnity earmark" | Test uses 110 multiplier | BRD states 110%-150% range; test only validates 110% |

---

## 4. Obsolete BDD Scenarios

### 4.1 Scenarios Testing Superseded Requirements
| BDD ID | Source BRD | Current Status | Notes |
|-------|-----------|---------------|-------|
| BDD-IMP-VAL-04 (Vietnam FX Tagging) | 2026-04-21 BRD | MOVED | Requirement moved to common module but no formal BDD update |

### 4.2 Scenarios with Changed Scope
| BDD ID | Original Scope | Current Scope | Notes |
|-------|--------------|---------------|-------|
| BDD-IMP-ISS-08 | Must validate FX categorization code | No longer validates code | Behavior changed during consolidation |
| REQ-IMP-FLOW-09 | "Invalid transition rejected" | REMOVED | This scenario exists in archived BDD v3.0 but removed in consolidated BDD v2.0 |

---

## 5. BRD Requirements Without BDD Scenarios

### 5.1 User Stories Without BDD (Critical)
| User Story | Description | Status |
|-----------|-------------|--------|
| US-LC-01 | LC Lifecycle State Management | Covered by BDD-FLOW scenarios but no explicit US Story mapping |
| US-LC-02 | Dual-Status Visibility | NO BDD - UI requirement |
| US-LC-03 | UCP 600 SLA Countdown | Partially covered (BDD-DOC-01) |
| US-ISS-01 | LC Issuance with Full Validation | Covered by multiple scenarios |
| US-ISS-02 | Automatic Limit Earmarking | Covered by BDD-ISS-04 |
| US-ISS-03 | MT700 Generation on Authorization | Covered by BDD-SWT-* |
| US-AMD-01 | Financial vs Non-Financial Classification | Covered by AMD-* scenarios |
| US-AMD-02 | Beneficiary Consent Tracking | Covered by AMD-04 |
| US-AMD-03 | MT707 Delta-Only Generation | Covered by BDD-IMP-SWT-08 |
| US-PRE-01 | Document Lodgement & Examination | Covered by DOC-* scenarios |
| US-PRE-02 | Regulatory Deadline Enforcement | Covered by DOC-01, VAL-02 |
| US-STL-01 | Sight LC Settlement | Covered by SET-* scenarios |
| US-STL-02 | Usance LC Maturity Tracking | Covered by SET-01 |
| US-STL-03 | Live FX Rate for Settlement | NO BDD - Infrastructure validation |
| US-SG-01 | Shipping Guarantee Issuance | Covered by SG-* scenarios |
| US-SG-02 | Automatic Waiver Lock | Covered by SG-02 |
| US-CAN-01 | Auto-Expiry with Mail Days Grace | Covered by CAN-01 |
| US-CAN-02 | Early Mutual Cancellation | Covered by CAN-02 |

### 5.2 SWIFT Validation Rules Without BDD (87 Rules Total)
| Rule ID Set | Count | Coverage |
|-------------|-------|----------|
| ISS-SWV-01 to ISS-SWV-17 | 17 | 0% (covered by integration tests only) |
| AMD-SWV-01 to AMD-SWV-06 | 6 | 0% (unit tests pending) |
| PRE-SWV-01 to PRE-SWV-07 | 7 | 0% (unit tests pending) |
| STL-SWV-01 to STL-SWV-08 | 8 | 0% (unit tests pending) |
| CAN-SWV-01 to CAN-SWV-03 | 3 | 0% (unit tests pending) |
| SWV-01 to SWV-09 | 9 | Partially covered by X-character tests |

### 5.3 Common Module Requirements (Not Import LC-Specific)
| Requirement | Module | Notes |
|-------------|-------|-------|
| REQ-COM-SLA-02 | Common | SLA countdown rules |
| REQ-COM-FX-02 | Common | Live FX rates |
| REQ-COM-AUT-03 | Common | Authority tiers |
| REQ-COM-PRD-01 | Common | Product config |

---

## 6. Tests Validating Obsolete Requirements

### 6.1 Tests with Outdated Assertions
| Test File | Test Method | Issue |
|----------|-------------|-------|
| BddImportLcModuleSpec | BDD-IMP-ISS-07 | Tests LC_DRAFT→LC_SETTLED direct transition; BRD now requires 8-step path |
| BddImportLcModuleSpec | BDD-IMP-VAL-04 | Tests FX tagging but requirement moved to common module |
| SwiftValidationSpec | Various | Tests X-character handling but Z-character validation not covered |

### 6.2 Tests with Contradictory Behavior
| Test File | Test Method | Expected (BRD) | Actual (Test) |
|----------|-------------|-----------------|----------------|
| BddImportLcModuleSpec | BDD-IMP-ISS-01 | effectiveAmount init on auth | effectiveAmount init on create |
| ShippingGuaranteeSpec | Test 110% Earmarking | Multiplier 110,125,150 | Only 110 |

---

## 7. Orphan Tests

### 7.1 Tests Outside BRD Scope
| Test File | Test Method | Feature | Notes |
|----------|-------------|---------|--------|
| ImportLcValidationServicesSpec | validateParty* | Party validation | Not in BRD |
| ImportLcServicesSpec | create#Draft | Draft LC | Draft functionality expanded in implementation |
| SwiftValidationSpec | validateSwiftFormat | SWIFT format | Validation rules not formally in BRD |
| TradeSwiftAutoTriggerSpec | Auto-trigger SWIFT | Auto-generation | Not in BRD |

### 7.2 Defensive/Just-in-Case Tests
| Test File | Test Method | Rationale |
|----------|-------------|-----------|
| BddImportLcModuleSpec | BDD-IMP-AMD-06 | Concurrent amendment block - defensive |
| BddImportLcModuleSpec | BDD-IMP-AMD-07 | Beneficiary approval - defensive |

---

## 8. Corrected True Gap List

### 8.1 Scenarios Lacking ANY Test Coverage
| Gap ID | BDD Scenario | BRD Requirement | Severity |
|--------|---------------|------------------|----------|
| GAP-01 | US-LC-02 (Dual-Status Display) | Display both Transaction State + LC Business State | HIGH |
| GAP-02 | US-STL-03 (Live FX Rate) | REQ-COM-FX-02 | MEDIUM |
| GAP-03 | ISS-SWV-10 to ISS-SWV-17 | SWIFT validation issuance rules | MEDIUM |
| GAP-04 | AMD-SWV-04 to AMD-SWV-06 | Amendment validation rules | MEDIUM |
| GAP-05 | PRE-SWV-05 to PRE-SWV-07 | Presentation validation rules | MEDIUM |
| GAP-06 | CAN-SWV-01 to CAN-SWV-03 | Cancellation SWIFT rules | LOW |
| GAP-07 | BRD Feature 1.1.D (Dual-Status Display) | Both statuses on header | HIGH |
| GAP-08 | US-CAN-02 | Early Cancellation with Beneficiary Consent | LOW |

### 8.2 Backend Logic Gaps (Spock Tests Needed)
| Gap ID | Requirement | Process Area |
|--------|-------------|--------------|
| GAP-09 | ISS-SWV-10 (Advising Bank RMA check) | Issuance validation |
| GAP-10 | ISS-SWV-12 (Amount format) | Issuance validation |
| GAP-11 | STL-SWV-04 (Nostro reference check) | Settlement |
| GAP-12 | STL-SWV-05 (MT103 beneficiary account) | Settlement |

### 8.3 UI/Frontend Gaps (Playwright/Jest Tests Needed)
| Gap ID | Requirement | Screen |
|--------|-------------|--------|
| GAP-13 | Dual-status display | LC Detail Header |
| GAP-14 | UCP 600 countdown timer | Document Examination |
| GAP-15 | Amendment pending consent indicator | Amendment List |

---

## 9. Corrected False Coverage List

### 9.1 Tests Covering Wrong Behavior
| Test File | Test Method | Issue |
|----------|-------------|-------|
| BddImportLcModuleSpec | BDD-IMP-ISS-01 | Tests creation-time init, should be auth-time |
| BddImportLcModuleSpec | BDD-IMP-ISS-04 | Tests calculation only, not facility update call |
| BddImportLcModuleSpec | BDD-IMP-SWT-01 | Tests X-char handling, missing Z-char validation |
| ShippingGuaranteeSpec | 110% Earmarking | Only tests 110%, not range 110-150% |

### 9.2 Tests with Quality Issues
| Test File | Test Method | Issue |
|----------|-------------|-------|
| BddImportLcModuleSpec | BDD-IMP-DOC-01 | Uses hardcoded 5, should fetch from product config |
| BddImportLcModuleSpec | BDD-IMP-SET-01 | Uses hardcoded 14 days, should use LC.usanceDays |
| BddImportLcModuleSpec | BDD-IMP-SWT-02/03/04 | Data-driven test but limited assertions |

---

## 10. Recommendations (Prioritized by Severity)

### CRITICAL (Must Fix Before Code Freeze)
| ID | Recommendation | Action | Owner |
|----|---------------|--------|-------|
| REC-01 | Add dual-status display tests | Create Playwright test for LC header with both states | Frontend |
| REC-02 | Fix BDD-IMP-ISS-01 test timing | Update test to verify on authorize, not create | Backend |
| REC-03 | Add MT700/MT707 immutability test | Add to SwiftGenerationSpec | Backend |
| REC-04 | Add Z-character validation test | Add to SwiftValidationSpec | Backend |

### HIGH (Should Fix in Current Sprint)
| ID | Recommendation | Action | Owner |
|----|---------------|--------|-------|
| REC-05 | Expand SG multiplier test range | Test 110%, 125%, 150% | Backend |
| REC-06 | Add product-config SLA test | Fetch from TradeProductCatalog | Backend |
| REC-07 | Add Live FX rate test | Use actual treasury API | Backend |
| REC-08 | Add Nostro reference validation | Test STL-SWV-04 | Backend |

### MEDIUM (Next Sprint)
| ID | Recommendation | Action | Owner |
|----|---------------|--------|-------|
| REC-09 | Add all issuance SWIFT validation tests | 17 rule coverage | Backend |
| REC-10 | Add amendment validation tests | 6 rule coverage | Backend |
| REC-11 | Add presentation validation tests | 7 rule coverage | Backend |
| REC-12 | Add settlement validation tests | 8 rule coverage | Backend |
| REC-13 | Add cancellation SWIFT tests | 3 rule coverage | Backend |

### LOW (Backlog)
| ID | Recommendation | Action | Owner |
|----|---------------|--------|-------|
| REC-14 | Document common module requirements | Create tracking doc | BA |
| REC-15 | Add auto-trigger SWIFT tests | TradeSwiftAutoTriggerSpec | Backend |
| REC-16 | Add party validation tests | ImportLcValidationServicesSpec | Backend |

---

## Appendix A: Requirement ID Cross-Reference

### Consolidated BRD IDs
```
REQ-IMP-01 to REQ-IMP-06: Module/process specs
REQ-IMP-DTL-00 to REQ-IMP-DTL-02: Detail requirements
REQ-IMP-FLOW-01 to REQ-IMP-FLOW-09: Flow transitions
REQ-IMP-STATE-01 to REQ-IMP-STATE-02: State definitions
REQ-IMP-PRC-01 to REQ-IMP-PRC-03: Process requirements
REQ-IMP-SPEC-01 to REQ-IMP-SPEC-06: Spec definitions
REQ-IMP-SWIFT-01 to REQ-IMP-SWIFT-06: SWIFT messages
US-LC-01 to US-LC-03: User stories (lifecycle)
US-ISS-01 to US-ISS-03: User stories (issuance)
US-AMD-01 to US-AMD-03: User stories (amendment)
US-PRE-01 to US-PRE-02: User stories (presentation)
US-STL-01 to US-STL-03: User stories (settlement)
US-SG-01 to US-SG-02: User stories (shipping guarantee)
US-CAN-01 to US-CAN-02: User stories (cancellation)
FR-ENT-01 to FR-ENT-33: Entity requirements
FR-TP-03 to FR-TP-12: TradeParty requirements
FR-SWV-01 to FR-SWV-09: SWIFT validation
FR-SGC-04 to FR-SGC-08: SWIFT gaps consolidation
FR-SWG-05 to FR-SWG-15: SWIFT generation
ISS-SWV-01 to ISS-SWV-17: Issuance validation rules
AMD-SWV-01 to AMD-SWV-06: Amendment validation rules
PRE-SWV-01 to PRE-SWV-07: Presentation validation rules
STL-SWV-01 to STL-SWV-08: Settlement validation rules
CAN-SWV-01 to CAN-SWV-03: Cancellation validation rules
```

### Common Module Requirements (Referenced)
```
REQ-COM-SLA-02: SLA countdown rules
REQ-COM-FX-02: Live FX rates
REQ-COM-AUT-03: Authority tiers
REQ-COM-PRD-01: Product configuration
```

---

## Appendix B: Test-to-BRD Mapping Summary

| Test File | BDD Coverage | Gap Coverage |
|----------|-------------|------------|
| BddImportLcModuleSpec.groovy | 22 scenarios | 8 gaps |
| EndToEndImportLcSpec.groovy | 1 scenario | 2 gaps |
| SwiftGenerationSpec.groovy | 5 scenarios | 4 gaps |
| SwiftValidationSpec.groovy | Partial SWIFT rules | 15 gaps |
| ShippingGuaranteeSpec.groovy | 1 scenario | 0 gaps |
| ImportLcServicesSpec.groovy | Multiple | 0 gaps - orphaned |
| ImportLcValidationServicesSpec.groovy | 0 | 0 gaps - orphaned |

---

*Report Generated: May 06, 2026*
*Analysis Version: 1.0*