# Test Specification: Import LC Module

**Component:** TradeFinance  
**Module:** Import Letter of Credit  
**Version:** 1.0  
**Update Date:** 2026-03-12
**Status:** Active  

---

## 1. Overview

This document defines the test specification for the Import LC module of the Trade Finance component. All tests are implemented as Spock specifications using the Moqui framework's testing infrastructure (`ExecutionContext`, `ScreenTest`, `makeDataLoader`).

### 1.1 Test Suite Structure

| File | Type | Pattern | Tests |
|------|------|---------|-------|
| `TradeFinanceSuite.groovy` | JUnit5 Suite Runner | — | 190+ |
| `TradeFinanceServicesSpec.groovy` | Core Services | `@Stepwise` | 80 |
| `TradeFinanceAccountingSpec.groovy` | GL Integration | `@Stepwise` | 3 |
| `TradeFinanceScreensSpec.groovy` | Screen Render Tests | `@Unroll` | 60 |
| `TradeFinanceDrawingFlowSpec.groovy` | Drawing Lifecycle | `@Stepwise` | 2 |
| `TradeFinanceSwiftSpec.groovy` | SWIFT Validation | `@Stepwise` | 2 |
| `TradeFinanceAmendmentSpec.groovy` | LC Amendment | `@Stepwise` | 2 |
| Phase-based Specs (Phase2-4) | Regression Tests | — | 30+ |

**Location:** `runtime/component/TradeFinance/src/test/groovy/moqui/trade/finance/`

### 1.2 Test Data Strategy

Tests use a **hybrid approach** of pre-loaded demo data and dynamically created records:

| Strategy | Use Case | Rationale |
|----------|----------|-----------|
| **Demo data** (TradeFinanceDemoData.xml) | Data validation, invalid transition guards, screen rendering | Realistic dataset with known state; no test setup overhead |
| **Dynamic data** (created in test) | Create/delete service tests, full lifecycle transitions | Must test that services correctly create and modify records |

### 1.3 Execution

```bash
# Required environment
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home

# Full verification (Slow: 100+ tests)
./gradlew reloadSave :runtime:component:TradeFinance:test

# Targeted verification (Fast)
./gradlew reloadSave :runtime:component:TradeFinance:test --tests "moqui.trade.finance.TradeFinanceAccountingSpec"
```

---

## 2. Demo Data Summary

**File:** `data/TradeFinanceDemoData.xml` (type: demo)

### 2.1 LC Records (10 records)

| LC ID | LC Number | LC Status | Tx Status | Product | Amount (USD) | Purpose |
|-------|-----------|-----------|-----------|---------|-------------|---------|
| DEMO_LC_01 | ILC-2026-0001 | Closed | Closed | Sight | 500,000 | Fully settled, 2 drawings, amendment, charges |
| DEMO_LC_02 | ILC-2026-0002 | Advised | Closed | Usance | 1,200,000 | Active usance, 1 drawing under review |
| DEMO_LC_03 | ILC-2026-0003 | Amended | Closed | Negotiation | 2,500,000 | Amended (expiry extended), high-value |
| DEMO_LC_04 | ILC-2026-0004 | Applied | Submitted | Sight | 750,000 | Pending approval |
| DEMO_LC_05 | ILC-2026-0005 | Draft | Draft | Sight | 320,000 | Initial draft, not submitted |
| DEMO_LC_06 | ILC-2026-0006 | Expired | Closed | Usance | 180,000 | Expired without utilization |
| DEMO_LC_07 | ILC-2026-0007 | Issued | Closed | Standby | 3,000,000 | Active standby guarantee |
| DEMO_LC_08 | ILC-2026-0008 | Applied | Submitted | Sight | 890,000 | Transferable, pending |
| DEMO_LC_09 | ILC-2026-0009 | Draft | Draft | Sight | 445,000 | Rejected & reopened |
| DEMO_LC_10 | ILC-2026-0010 | Negotiated | Approved | Negotiation | 1,800,000 | 2 drawings, 1 discrepancy waived |

### 2.2 Related Entity Data

| Entity | Count | Examples |
|--------|-------|---------|
| Mantle Request | 11 | 10 LC issuance + 1 amendment request |
| LcHistory | 13 | LC_01: 6 entries (full lifecycle), LC_09: 4 entries (reject/reopen) |
| LcAmendment | 2 | LC_01: amount increase, LC_03: expiry extension |
| LcDrawing | 5 | LC_01: 2 paid, LC_02: 1 received, LC_10: 1 accepted + 1 compliant |
| LcDrawingDocument | 7 | LC_01: full set (BL, invoice, packing list, CoO, insurance) |
| LcDiscrepancy | 1 | LC_10: amount discrepancy, waived |
| LcCharge | 11 | Issuance, SWIFT, amendment, courier, acceptance, discrepancy fees |
| LcProvision | 4 | LC_01: released, LC_02/03/07: active |
| LcDocument | 4 | MT700/MT707 SWIFT messages, application forms |

### 2.3 Accounting Configuration (TradeFinanceAccountingSpec)

| Entity / Configuration | Purpose |
|------------------------|---------|
| `GlAccount` | Revenue (Item Type map) and A/R (Default Type map) accounts |
| `GlAccountOrganization` | Links GL accounts to `DEMO_ORG_VIETCOMBANK` |
| `PartyRole` | `OrgInternal` role for issuing bank (required for auto-post) |
| `InvoiceTypeTransType` | Maps `InvoiceSales` to `AttSalesInvoice` |
| `TimePeriod` | Open `FiscalMonth` for current transaction dates |

---

## 3. Service Test Specification (TradeFinanceServicesSpec)

**Pattern:** `@Stepwise` (ordered execution), `@Shared` state  
**Total:** 80 test methods (including parameterized data-driven tests)

### 3.1 Demo Data Validation (10 tests)

Validates that pre-loaded demo data is present and correct in the database.

| # | Test Name | Data Source | Validates |
|---|-----------|------------|-----------|
| 1 | validate demo LC_01 (Closed Sight LC) data | DEMO_LC_01 | LC fields, statuses, SWIFT fields, amounts |
| 2 | validate demo LC_02 (Advised Usance LC) data | DEMO_LC_02 | Usance fields, draftsAt, product type |
| 3 | validate demo LC_05 (Draft LC) data | DEMO_LC_05 | Draft statuses, minimal fields |
| 4 | validate demo LC_07 (Standby LC) data | DEMO_LC_07 | Standby product type, amount |
| 5 | validate demo LC_10 (Negotiated LC) data | DEMO_LC_10 | Negotiated status, confirmation instructions |
| 6 | validate demo related entities | Multiple | Drawings (paid), amendments, charges, discrepancy, provisions |
| 7 | validate demo LC_01 history audit trail | DEMO_LC_01 | 6 history entries: Draft→Applied→Issued→Closed |
| 8 | validate demo LC_09 rejection/reopen trail | DEMO_LC_09 | 4 entries: Submitted→Rejected→Draft |
| 9 | validate demo drawing documents for LC_01 | DEMO_LC_01 | 5 document types in correct order |
| 10 | validate all 10 demo LCs exist | All | Count = 10 |

### 3.2 SWIFT Validation Service (7 tests)

Tests the `validate#LetterOfCredit` service for SWIFT MT700 field compliance.

| # | Test Name | Input | Expected |
|---|-----------|-------|----------|
| 1 | valid SWIFT Character Set X fields | lcNumber="ILC-2026-0001", valid names | Pass |
| 2 | invalid SWIFT characters | applicantName="Invalid@Name#Corp" | Error: invalid chars |
| 3 | LC Number exceeds 16 characters | lcNumber (17 chars) | Error: too long |
| 4 | Applicant Name exceeds 140 chars | 141 'A' chars | Error: too long |
| 5 | Expiry Date before Issue Date | issue=2026-06-01, expiry=2026-05-01 | Error: date order |
| 6 | invalid Amount Tolerance format | "ABC" | Error: format |
| 7 | valid Amount Tolerance format | "5/5" | Pass |

### 3.3 Create LC Service (3 tests)

Tests `create#LetterOfCredit` with dynamic data creation.

| # | Test Name | Validates |
|---|-----------|-----------|
| 1 | create LC sets default statuses | lcStatusId=LcLfDraft, statusId=LcTxDraft, lcId and requestId returned |
| 2 | create LC creates linked Mantle Request | Request in ReqDraft, type=RqtLcIssuance |
| 3 | create LC records initial history entry | LcHistory with changeType=StatusChange, fieldName=lcStatusId |

### 3.4 Update LC Service (2 tests)

| # | Test Name | Data Source | Validates |
|---|-----------|------------|-----------|
| 1 | update demo LC_05 modifies fields | DEMO_LC_05 | applicantName updated, LcHistory recorded |
| 2 | update fails for non-existent lcId | N/A | Error message returned |

### 3.5 Transaction Status Transitions (4 tests, dynamic)

Uses the newly created LC from section 3.3.

| # | Test Name | Transition | Validates |
|---|-----------|-----------|-----------|
| 1 | Draft → Submitted | LcTxDraft→LcTxSubmitted | Status updated |
| 2 | Submitted → Approved | LcTxSubmitted→LcTxApproved | Status updated |
| 3 | records history with old/new values | — | oldValue=LcTxSubmitted, newValue=LcTxApproved |
| 4 | Approved → Closed | LcTxApproved→LcTxClosed | Status updated |

### 3.6 LC Lifecycle Transitions (3 tests, dynamic)

Uses the newly created LC from section 3.3.

| # | Test Name | Transition | Validates |
|---|-----------|-----------|-----------|
| 1 | Draft → Applied | LcLfDraft→LcLfApplied | Status updated |
| 2 | Applied → Issued | LcLfApplied→LcLfIssued | Status updated |
| 3 | lifecycle transition records history | — | oldValue=LcLfApplied in history |

### 3.7 Invalid Transition Guards (3 tests, demo data)

| # | Test Name | Data Source | Invalid Transition | Expected |
|---|-----------|------------|-------------------|----------|
| 1 | invalid lifecycle transition | DEMO_LC_05 | Draft→Issued (skip Applied) | Error |
| 2 | invalid transaction transition | DEMO_LC_05 | Draft→Approved (skip Submitted) | Error |
| 3 | transition non-existent LC | N/A | NON_EXISTENT→Applied | Error |

### 3.8 Rejection & Reopen Flow (3 tests, demo data)

Uses DEMO_LC_09 (already in Draft after previous rejection cycle).

| # | Test Name | Transition | Validates |
|---|-----------|-----------|-----------|
| 1 | submit demo LC_09 | Draft→Submitted | statusId updated |
| 2 | reject demo LC_09 | Submitted→Rejected | statusId updated |
| 3 | reopen demo LC_09 | Rejected→Draft | statusId back to Draft |

### 3.9 Delete LC Service (4 tests)

| # | Test Name | Data Source | Validates |
|---|-----------|------------|-----------|
| 1 | create LC for delete test | Dynamic | deleteLcId returned |
| 2 | delete succeeds for Draft | Dynamic | Entity is null after delete |
| 3 | delete fails for Applied (LC_04) | DEMO_LC_04 | Error: non-Draft status |
| 4 | delete fails for Closed (LC_01) | DEMO_LC_01 | Error: non-Draft status |

### 3.10 Full Lifecycle Summary (2 tests)

| # | Test Name | Validates |
|---|-----------|-----------|
| 1 | validate full lifecycle for new LC | lcStatusId=LcLfIssued, statusId=LcTxClosed, ≥6 history entries |
| 2 | validate all 10 demo LCs exist | Total count = 10 |

---

## 4. Screen Test Specification (TradeFinanceScreensSpec)

**Pattern:** `@Unroll` data-driven, `ScreenTest` API  
**Total:** 17 test methods  
**Data:** All tests use pre-loaded demo data (no dynamic creation)

### 4.1 Data-Driven Screen Renders (9 @Unroll tests)

| # | Screen Path | Expected Content | Demo Data |
|---|------------|-----------------|-----------|
| 1 | Home | (renders without error) | — |
| 2 | ImportLc/ImportLcDashboard | (renders without error) | — |
| 3 | ImportLc/ImportLcList | "Import Letters of Credit", "Create New LC" | All LCs |
| 4 | ImportLc/ImportLcList | "ILC-2026-0001", "ABC Trading Co" | DEMO_LC_01 |
| 5 | ImportLc/ImportLcList | "ILC-2026-0005", "Green Agriculture Co" | DEMO_LC_05 |
| 6 | ImportLc/LcDetail?lcId=DEMO_LC_01 | LC number, parties, Status Management, Activity Log | DEMO_LC_01 |
| 7 | ImportLc/LcDetail?lcId=DEMO_LC_05 | LC number, applicant | DEMO_LC_05 |
| 8 | ImportLc/LcDetail?lcId=DEMO_LC_07 | LC number, applicant | DEMO_LC_07 |
| 9 | ImportLc/LcDetail?lcId=DEMO_LC_10 | LC number, beneficiary | DEMO_LC_10 |

### 4.2 ImportLcList Targeted Tests (3 tests)

| # | Test Name | Validates |
|---|-----------|-----------|
| 1 | dual-status columns | "LC Status" and "Processing" column headers |
| 2 | action buttons | Edit and Delete action elements |
| 3 | Create dialog SWIFT fields | LC Number, Form of Credit, Applicable Rules fields |

### 4.3 LcDetail Targeted Tests (5 tests)

All use DEMO_LC_01 (fully populated) and DEMO_LC_04 (pending state).

| # | Test Name | Data Source | Validates |
|---|-----------|------------|-----------|
| 1 | renders all 4 form sections | DEMO_LC_01 | General Info, Parties, Shipment, Documents |
| 2 | renders dual-status in sidebar | DEMO_LC_01 | lcStatusId, statusId, lcProductTypeEnumId |
| 3 | renders party data | DEMO_LC_01 | Applicant, Beneficiary, Issuing Bank names |
| 4 | renders shipment data | DEMO_LC_01 | Port of Loading, Port of Discharge |
| 5 | renders Applied/Submitted LC | DEMO_LC_04 | LC number, applicant name |

---

## 5. Phase 3 Test Specification (TradeFinancePhase3Spec)

**Pattern:** `@Stepwise` (ordered execution), `@Shared` state  
**Total:** 5 test methods

### 5.1 CBS Integration Mock Tests
| # | Test Name | Validates |
|---|-----------|-----------|
| 1 | test CBS Funds Hold Mock | `CbsIntegrationServices.hold#Funds` returns success and a `HLD-` reference |
| 2 | test CBS Funds Release Mock | `CbsIntegrationServices.release#Funds` returns success |

### 5.2 Financial Integration Tests
| # | Test Name | Validates |
|---|-----------|-----------|
| 1 | hold and release provision using CBS integration | `FinancialServices.hold#LcProvision` links the CBS Hold Ref; `release#LcProvision` works |

### 5.3 SWIFT MT700 Generation
| # | Test Name | Validates |
|---|-----------|-----------|
| 1 | generate SWIFT MT700 for an LC | `SwiftServices.generate#SwiftMt700` formats the string correctly |

### 5.4 Lifecycle Issuance
| # | Test Name | Validates |
|---|-----------|-----------|
| 1 | issue Letter of Credit and verify MT700 document attachment | `LifecycleServices.issue#LetterOfCredit` marks status `LcLfIssued`/`LcTxClosed` and attaches document |

### 5.5 Scheduled Expiry
| # | Test Name | Validates |
|---|-----------|-----------|
| 1 | scheduled auto-expiry test | `ScheduledServices.check#LcExpiry` transitions past-due LCs to `LcLfExpired` |

---

## 6. Test Coverage Matrix

### 6.1 Service Coverage

| Service | Create | Read | Update | Delete | Validation | Status Transitions |
|---------|--------|------|--------|--------|------------|-------------------|
| `create#LetterOfCredit` | ✅ | — | — | — | — | — |
| `update#LetterOfCredit` | — | — | ✅ | — | — | — |
| `delete#LetterOfCredit` | — | — | — | ✅ (+ guards) | — | — |
| `validate#LetterOfCredit` | — | — | — | — | ✅ (7 cases) | — |
| `transition#LcStatus` | — | — | — | — | — | ✅ (valid + invalid) |
| `transition#TransactionStatus` | — | — | — | — | — | ✅ (valid + invalid + reject/reopen) |

### 5.2 Entity Coverage

| Entity | Validated In |
|--------|-------------|
| LetterOfCredit | Demo data checks, create/update/delete, transitions |
| LcHistory | Audit trail validation, history recording after transitions |
| LcAmendment | Demo data check (LC_01 amount, LC_03 expiry) |
| LcDrawing | Demo data check (5 drawings across LC_01, LC_02, LC_10) |
| LcDrawingDocument | Demo data check (5 docs for LC_01 Drawing 01) |
| LcDiscrepancy | Demo data check (LC_10 amount discrepancy, waived) |
| LcCharge | Demo data check (issuance, SWIFT, amendment, courier, acceptance) |
| LcProvision | Demo data check (active + released provisions) |
| LcDocument | Not directly tested (attachment references only) |
| mantle.request.Request | Validated in create LC tests |

### 5.3 Screen Coverage

| Screen | Render | Content | Fields | Actions |
|--------|--------|---------|--------|---------|
| Home | ✅ | — | — | — |
| ImportLcDashboard | ✅ | — | — | — |
| ImportLcList | ✅ | ✅ (demo data) | ✅ (status columns, SWIFT fields) | ✅ (Edit, Delete, Create) |
| LcDetail | ✅ | ✅ (4 LC variants) | ✅ (4 sections, sidebar, parties, shipment) | — |

### 5.4 StatusFlow Coverage

| StatusFlow | Transitions Tested |
|-----------|-------------------|
| LcLifecycle | Draft→Applied→Issued (valid), Draft→Issued (invalid) |
| LcTransaction | Draft→Submitted→Approved→Closed (valid), Draft→Approved (invalid), Submitted→Rejected→Draft (reopen) |
| LcDrawingStatus | Demo data only (no transition service yet) |

---

## 7. Requirements Traceability Matrix (RTM)

This section maps the Business Requirements Document (`brd_import_lc.md`) functional requirements to their corresponding automated test implementations.

| BRD Section | Requirement Description (Use Case) | Covered By (Test Suite) | Coverage Level |
|-------------|------------------------------------|--------------------------|----------------|
| **4. UI/UX** | Grouped Layout (General, Parties, Shipment, Docs) | `ScreensSpec` | High |
| **5. Integration** | Generate SWIFT MT700 message | `Phase3Spec` | High |
| **5. Integration** | CBS: Accounting (Mantle GL) | `AccountingSpec` | High |
| **5. Integration** | CBS: Funds Hold/Release | `Phase3Spec`, `CbsIntegration` | High |
| **6. Workflow** | Transaction Lifecycle (Draft → Closed) | `ServicesSpec` | High |
| **6. Workflow** | LC Lifecycle (Draft → Issued) | `ServicesSpec` | High |
| **8.3 LC App** | Create Draft LC Application | `ServicesSpec` | High |
| **8.3 LC App** | Provision & Charge Assessment | `AccountingSpec` | High |
| **8.5 Manage** | LC Amendment Lifecycle | `AmendmentSpec`, `Phase4Spec` | High |
| **8.6 Payment** | Drawing Registry & Examination | `DrawingFlowSpec` | High |
| **8.6 Payment** | Discrepancy Record & Resolve | `DrawingFlowSpec` | High |
| **8.6 Payment** | MT734 Generation | `DrawingFlowSpec` | High |
| **8.7 Core** | Manage LC Provision (Mantle Integration) | `AccountingSpec` | High |

---

## 8. Known Limitations

1. **Screen tests** validate rendered HTML content only — no JavaScript/UI interaction testing
2. **CBS integration** is mocked using stubs for financial impacts
3. **Multi-user workflow** (different roles submitting/approving) is not tested — all tests run as `tf-admin`
