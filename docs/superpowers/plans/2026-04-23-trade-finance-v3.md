# Trade Finance Platform v3.0 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement all entity, service, and data enhancements defined in the [Design Spec v3.0](file:///Users/me/myprojects/moqui-trade/docs/superpowers/specs/2026-04-21-moqui-trade-design.md) to achieve full BRD coverage for the Common Module and Import LC Module.

**Architecture:** Dual-entity model where `TradeInstrument` captures the original issuance snapshot and transaction management fields, while `ImportLetterOfCredit` holds live effective values updated by amendments and draws. All lifecycle services read effective values from `ImportLetterOfCredit`. SWIFT validation enforced at two layers: data capture (entity + service) and message generation.

**Tech Stack:** Moqui Framework 3.0, Groovy (Spock tests), XML service/entity definitions, H2 (dev DB with auto-table-creation)

**References:**
- Design Spec: [2026-04-21-moqui-trade-design.md](file:///Users/me/myprojects/moqui-trade/docs/superpowers/specs/2026-04-21-moqui-trade-design.md)
- Common BDD: [2026-04-21-common-module-bdd.md](file:///Users/me/myprojects/moqui-trade/docs/superpowers/specs/2026-04-21-common-module-bdd.md)
- Import LC BDD: [2026-04-21-import-lc-bdd.md](file:///Users/me/myprojects/moqui-trade/docs/superpowers/specs/2026-04-21-import-lc-bdd.md)
- MT Messages: [MT-others.md](file:///Users/me/myprojects/moqui-trade/docs/superpowers/specs/MT-message/MT-others.md)
- Framework Docs: [Framework+Features.md](file:///Users/me/myprojects/moqui-trade/moqui-documentation/moqui-framework/Framework+Features.md)

**Constraints:**
- All entity XML: `runtime/component/TradeFinance/entity/`
- All service XML: `runtime/component/TradeFinance/service/`
- All seed data: `runtime/component/TradeFinance/data/`
- All tests: `src/test/groovy/moqui/trade/finance/`
- Entity rules: [moqui-entities.md](file:///Users/me/myprojects/moqui-trade/.agents/rules/moqui-entities.md)
- Service rules: [moqui-services.md](file:///Users/me/myprojects/moqui-trade/.agents/rules/moqui-services.md)
- Testing rules: [testing-debugging.md](file:///Users/me/myprojects/moqui-trade/.agents/rules/testing-debugging.md)

---

## Phase 1: Foundation — Entity Schema & Seed Data

> Entities must be in place before any service or test work. Moqui auto-creates tables at runtime — no migration scripts needed.

---

### Task 1.1: Extend TradeInstrument with Transaction Management Fields

**BDD Scenarios:** BDD-CMN-ENT-01, BDD-CMN-ENT-01B
**BRD Requirements:** REQ-COM-ENT-01

**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/entity/TradeCommonEntities.xml` (TradeInstrument entity, lines 6-20)
- Test: `src/test/groovy/moqui/trade/finance/TradeCommonEntitiesSpec.groovy`

**Current state of TradeInstrument (13 fields):**
```xml
<entity entity-name="TradeInstrument" package="moqui.trade.instrument">
    <field name="instrumentId" type="id" is-pk="true"/>
    <field name="transactionRef" type="text-short"/>
    <field name="lifecycleStatusId" type="id"/>
    <field name="productEnumId" type="id"/>
    <field name="amount" type="number-decimal"/>
    <field name="currencyUomId" type="id"/>
    <field name="outstandingAmount" type="number-decimal"/>
    <field name="baseEquivalentAmount" type="number-decimal"/>
    <field name="applicantPartyId" type="id"/>
    <field name="beneficiaryPartyId" type="id"/>
    <field name="issueDate" type="date"/>
    <field name="expiryDate" type="date"/>
    <field name="customerFacilityId" type="id"/>
</entity>
```

- [ ] **Step 1: Write the failing test**

In `TradeCommonEntitiesSpec.groovy`, add a test that creates a TradeInstrument with the new transaction management fields and verifies they persist correctly:

```groovy
def "TradeInstrument persists transaction management fields"() {
    when:
    ec.service.sync().name("create#moqui.trade.instrument.TradeInstrument").parameters([
        transactionRef: "TF-IMP-26-TEST",
        lifecycleStatusId: "INST_PRE_ISSUE",
        transactionStatusId: "TRANS_DRAFT",
        transactionDate: ec.user.nowTimestamp,
        transactionTypeEnumId: "NEW_ISSUANCE",
        makerUserId: ec.user.userId,
        makerTimestamp: ec.user.nowTimestamp,
        versionNumber: 1,
        priorityEnumId: "NORMAL",
        productEnumId: "IMP_LC",
        amount: 100000,
        currencyUomId: "USD",
        outstandingAmount: 100000,
        applicantPartyId: "ACME_CORP_001",
        issueDate: "2026-06-01",
        expiryDate: "2026-12-31"
    ]).call()
    def instruments = ec.entity.find("moqui.trade.instrument.TradeInstrument")
            .condition("transactionRef", "TF-IMP-26-TEST").list()
    def inst = instruments[0]

    then:
    inst.transactionStatusId == "TRANS_DRAFT"
    inst.transactionTypeEnumId == "NEW_ISSUANCE"
    inst.makerUserId == ec.user.userId
    inst.makerTimestamp != null
    inst.versionNumber == 1
    inst.priorityEnumId == "NORMAL"
    inst.checkerUserId == null
    inst.checkerTimestamp == null
    inst.rejectionReason == null
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
cd runtime/component/TradeFinance && gradle test --tests "*TradeCommonEntitiesSpec*transaction management*" 2>&1 | tail -20
```
Expected: FAIL — fields do not exist yet.

- [ ] **Step 3: Add the 12 new transaction management fields to TradeInstrument**

Add these fields to the `TradeInstrument` entity in `TradeCommonEntities.xml`, after `customerFacilityId`:

```xml
<!-- Transaction Management Fields -->
<field name="transactionDate" type="date"/>
<field name="transactionTypeEnumId" type="id"/>
<field name="transactionStatusId" type="id"/>
<field name="makerUserId" type="id"/>
<field name="makerTimestamp" type="date-time"/>
<field name="checkerUserId" type="id"/>
<field name="checkerTimestamp" type="date-time"/>
<field name="rejectionReason" type="text-long"/>
<field name="versionNumber" type="number-integer"/>
<field name="lastUpdateTimestamp" type="date-time"/>
<field name="priorityEnumId" type="id"/>
```

Also update the entity comment to reflect the snapshot + transaction management dual purpose.

- [ ] **Step 4: Run the test to verify it passes**

```bash
cd runtime/component/TradeFinance && gradle test --tests "*TradeCommonEntitiesSpec*transaction management*" 2>&1 | tail -20
```
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add entity/TradeCommonEntities.xml src/test/groovy/moqui/trade/finance/TradeCommonEntitiesSpec.groovy
git commit -m "feat(entity): add transaction management fields to TradeInstrument

Adds transactionDate, transactionTypeEnumId, transactionStatusId,
makerUserId/makerTimestamp, checkerUserId/checkerTimestamp,
rejectionReason, versionNumber, lastUpdateTimestamp, priorityEnumId.
Ref: BDD-CMN-ENT-01, REQ-COM-ENT-01"
```

---

### Task 1.2: Extend TradeParty with SWIFT & Compliance Fields

**BDD Scenarios:** BDD-CMN-ENT-02, BDD-CMN-ENT-03
**BRD Requirements:** REQ-COM-ENT-02

**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/entity/TradeCommonEntities.xml` (TradeParty entity, lines 61-66)
- Test: `src/test/groovy/moqui/trade/finance/TradeCommonEntitiesSpec.groovy`

**Current state of TradeParty (4 fields):** `partyId`, `partyName`, `kycStatus`, `kycExpiryDate`

- [ ] **Step 1: Write the failing test**

```groovy
def "TradeParty persists SWIFT and compliance fields"() {
    when:
    ec.service.sync().name("create#moqui.trade.instrument.TradeParty").parameters([
        partyId: "TEST_BANK_SWT",
        partyName: "Test Banking Corp",
        kycStatus: "Active",
        kycExpiryDate: "2027-12-31",
        sanctionsStatus: "Clear",
        countryOfRisk: "US",
        swiftBic: "TESTUSXX",
        registeredAddress: "123 Main Street\nNew York NY 10001\nUSA",
        partyRoleEnumId: "ADVISING_BANK"
    ]).call()
    def party = ec.entity.find("moqui.trade.instrument.TradeParty")
            .condition("partyId", "TEST_BANK_SWT").one()

    then:
    party.sanctionsStatus == "Clear"
    party.countryOfRisk == "US"
    party.swiftBic == "TESTUSXX"
    party.registeredAddress.contains("123 Main Street")
    party.partyRoleEnumId == "ADVISING_BANK"
}
```

- [ ] **Step 2: Run test — expect FAIL**
- [ ] **Step 3: Add 5 new fields to TradeParty**

```xml
<field name="sanctionsStatus" type="text-short"/> <!-- Clear, Suspended, Blocked -->
<field name="countryOfRisk" type="id"/>
<field name="swiftBic" type="text-short"/>
<field name="registeredAddress" type="text-long"/> <!-- 4x35 SWIFT format -->
<field name="partyRoleEnumId" type="id"/>
```

- [ ] **Step 4: Run test — expect PASS**
- [ ] **Step 5: Commit**

```bash
git add entity/TradeCommonEntities.xml src/test/groovy/moqui/trade/finance/TradeCommonEntitiesSpec.groovy
git commit -m "feat(entity): add SWIFT/compliance fields to TradeParty

Adds sanctionsStatus, countryOfRisk, swiftBic, registeredAddress,
partyRoleEnumId. Ref: BDD-CMN-ENT-02, REQ-COM-ENT-02"
```

---

### Task 1.3: Extend CustomerFacility with Party Ownership

**BDD Scenarios:** BDD-CMN-ENT-04, BDD-CMN-ENT-05
**BRD Requirements:** REQ-COM-ENT-03

**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/entity/TradeCommonEntities.xml` (CustomerFacility, lines 22-27)
- Modify: `runtime/component/TradeFinance/data/TradeFinanceMasterData.xml`
- Test: `src/test/groovy/moqui/trade/finance/TradeCommonEntitiesSpec.groovy`

- [ ] **Step 1: Write the failing test** verifying `partyId` FK on CustomerFacility
- [ ] **Step 2: Run test — expect FAIL**
- [ ] **Step 3: Add `partyId` field to CustomerFacility, update seed data to include `partyId` on all existing facility records**

```xml
<field name="partyId" type="id"/> <!-- FK to owning customer TradeParty -->
```

Seed data update:
```xml
<moqui.trade.instrument.CustomerFacility facilityId="FAC-ACME-001" partyId="ACME_CORP_001" totalApprovedLimit="10000000.00" utilizedAmount="2500000.00" facilityExpiryDate="2027-12-31"/>
```

- [ ] **Step 4: Run test — expect PASS**
- [ ] **Step 5: Commit**

---

### Task 1.4: Extend TradeTransactionAudit with IP and Field Tracking

**BDD Scenarios:** BDD-CMN-MAS-04
**BRD Requirements:** REQ-COM-MAS-03

**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/entity/TradeCommonEntities.xml` (TradeTransactionAudit, lines 29-37)
- Test: `src/test/groovy/moqui/trade/finance/TradeCommonEntitiesSpec.groovy`

- [ ] **Step 1: Write the failing test** verifying `ipAddress` and `fieldChanged` persist
- [ ] **Step 2: Run test — expect FAIL**
- [ ] **Step 3: Add 2 new fields**

```xml
<field name="ipAddress" type="text-short"/>
<field name="fieldChanged" type="text-medium"/>
```

- [ ] **Step 4: Run test — expect PASS**
- [ ] **Step 5: Commit**

---

### Task 1.5: Create New Common Module Entities (FeeConfiguration, TradeProductCatalog, UserAuthorityProfile)

**BDD Scenarios:** BDD-CMN-MAS-01, BDD-CMN-MAS-02, BDD-CMN-PRD-01 to PRD-11, BDD-CMN-AUTH-01 to AUTH-05
**BRD Requirements:** REQ-COM-MAS-01, REQ-COM-PRD-01, REQ-COM-AUTH-01

**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/entity/TradeCommonEntities.xml`
- Create: `runtime/component/TradeFinance/data/TradeProductCatalogData.xml`
- Test: `src/test/groovy/moqui/trade/finance/TradeCommonEntitiesSpec.groovy`

- [ ] **Step 1: Write the failing tests** — one test per new entity verifying all fields persist

- [ ] **Step 2: Run tests — expect FAIL**

- [ ] **Step 3: Add three new entities to TradeCommonEntities.xml**

**FeeConfiguration:**
```xml
<!-- Fee/tariff rule definitions, managed via Maker/Checker admin -->
<entity entity-name="FeeConfiguration" package="moqui.trade.instrument">
    <field name="feeConfigId" type="id" is-pk="true"/>
    <field name="feeTypeEnumId" type="id"/> <!-- ISSUANCE_FEE, AMENDMENT_FEE, etc. -->
    <field name="calculationMethodEnumId" type="id"/> <!-- FLAT, PERCENTAGE, TIERED -->
    <field name="ratePercent" type="number-decimal"/>
    <field name="flatAmount" type="number-decimal"/>
    <field name="minFloorAmount" type="number-decimal"/>
    <field name="maxCeilingAmount" type="number-decimal"/>
    <field name="currencyUomId" type="id"/>
    <field name="isActive" type="text-indicator" default="'Y'"/>
</entity>
```

**TradeProductCatalog:**
```xml
<!-- Product configurations that drive LC behavior, validation rules, and feature flags -->
<entity entity-name="TradeProductCatalog" package="moqui.trade.instrument">
    <field name="productCatalogId" type="id" is-pk="true"/>
    <field name="productName" type="text-medium"/>
    <field name="productDescription" type="text-long"/>
    <field name="isActive" type="text-indicator" default="'Y'"/>
    <field name="allowedTenorEnumId" type="id"/>
    <field name="maxToleranceLimit" type="number-decimal"/>
    <field name="allowRevolving" type="text-indicator" default="'N'"/>
    <field name="allowAdvancePayment" type="text-indicator" default="'N'"/>
    <field name="isStandby" type="text-indicator" default="'N'"/>
    <field name="isTransferable" type="text-indicator" default="'N'"/>
    <field name="accountingFrameworkEnumId" type="id"/> <!-- CONVENTIONAL, ISLAMIC -->
    <field name="mandatoryMarginPercent" type="number-decimal"/>
    <field name="documentExamSlaDays" type="number-integer" default="5"/>
    <field name="defaultSwiftFormatEnumId" type="id"/> <!-- MT700, MT760 -->
</entity>
```

**UserAuthorityProfile:**
```xml
<!-- Authorization tier assignments for Maker/Checker governance -->
<entity entity-name="UserAuthorityProfile" package="moqui.trade.instrument">
    <field name="authorityProfileId" type="id" is-pk="true"/>
    <field name="userId" type="id"/>
    <field name="authorityTierEnumId" type="id"/> <!-- TIER_1, TIER_2, TIER_3, TIER_4 -->
    <field name="maxApprovalAmount" type="number-decimal"/>
    <field name="currencyUomId" type="id"/>
    <field name="isSuspended" type="text-indicator" default="'N'"/>
</entity>
```

- [ ] **Step 4: Create seed data file `TradeProductCatalogData.xml` with standard product configurations**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<entity-facade-xml>
    <!-- Standard Import LC Product -->
    <moqui.trade.instrument.TradeProductCatalog productCatalogId="IMP_LC_STANDARD"
        productName="Standard Import LC" isActive="Y"
        allowedTenorEnumId="ALL" maxToleranceLimit="0.10"
        allowRevolving="N" allowAdvancePayment="N" isStandby="N" isTransferable="N"
        accountingFrameworkEnumId="CONVENTIONAL" mandatoryMarginPercent="0"
        documentExamSlaDays="5" defaultSwiftFormatEnumId="MT700"/>

    <!-- Authority Tier Definitions -->
    <moqui.basic.EnumerationType enumTypeId="AuthorityTier" description="Maker/Checker Authority Tiers"/>
    <moqui.basic.Enumeration enumId="TIER_1" enumTypeId="AuthorityTier" description="Up to $100K"/>
    <moqui.basic.Enumeration enumId="TIER_2" enumTypeId="AuthorityTier" description="$100K - $1M"/>
    <moqui.basic.Enumeration enumId="TIER_3" enumTypeId="AuthorityTier" description="$1M - $5M"/>
    <moqui.basic.Enumeration enumId="TIER_4" enumTypeId="AuthorityTier" description="Over $5M (Dual Checker)"/>

    <!-- Transaction Processing Statuses -->
    <moqui.basic.StatusType statusTypeId="TransactionProcessing" description="Transaction Processing Status"/>
    <moqui.basic.StatusItem statusId="TRANS_DRAFT" statusTypeId="TransactionProcessing" description="Draft"/>
    <moqui.basic.StatusItem statusId="TRANS_SUBMITTED" statusTypeId="TransactionProcessing" description="Submitted"/>
    <moqui.basic.StatusItem statusId="TRANS_APPROVED" statusTypeId="TransactionProcessing" description="Approved"/>
    <moqui.basic.StatusItem statusId="TRANS_REJECTED" statusTypeId="TransactionProcessing" description="Rejected"/>
    <moqui.basic.StatusItem statusId="TRANS_CANCELLED" statusTypeId="TransactionProcessing" description="Cancelled"/>

    <!-- Transaction Priority -->
    <moqui.basic.EnumerationType enumTypeId="TransactionPriority" description="Transaction Priority Level"/>
    <moqui.basic.Enumeration enumId="NORMAL" enumTypeId="TransactionPriority" description="Normal"/>
    <moqui.basic.Enumeration enumId="URGENT" enumTypeId="TransactionPriority" description="Urgent"/>
    <moqui.basic.Enumeration enumId="EXPRESS" enumTypeId="TransactionPriority" description="Express"/>
</entity-facade-xml>
```

- [ ] **Step 5: Run tests — expect PASS**
- [ ] **Step 6: Commit**

```bash
git add entity/TradeCommonEntities.xml data/TradeProductCatalogData.xml src/test/groovy/moqui/trade/finance/TradeCommonEntitiesSpec.groovy
git commit -m "feat(entity): add FeeConfiguration, TradeProductCatalog, UserAuthorityProfile

Three new common module entities with seed data for product configs,
authority tiers, transaction statuses, and priority enumerations.
Ref: BDD-CMN-MAS-01, BDD-CMN-PRD-01, BDD-CMN-AUTH-01"
```

---

### Task 1.6: Extend ImportLetterOfCredit with Effective Values and New Fields

**BDD Scenarios:** BDD-IMP-FLOW-01, BDD-IMP-ISS-03, BDD-IMP-AMD-01
**BRD Requirements:** REQ-IMP-02

**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/entity/ImportLcEntities.xml` (ImportLetterOfCredit, lines 6-24)
- Test: `src/test/groovy/moqui/trade/finance/ImportLcEntitiesSpec.groovy`

**Current state (13 fields):** `instrumentId`, `businessStateId`, `beneficiaryPartyId`, `tolerancePositive/Negative`, `tenorTypeId`, `usanceDays`, `portOfLoading/Discharge`, `expiryPlace`, `goodsDescription`, `documentsRequired`, `additionalConditions`

- [ ] **Step 1: Write the failing test**

```groovy
def "ImportLetterOfCredit persists effective values and new fields"() {
    when:
    // First create parent TradeInstrument
    ec.service.sync().name("create#moqui.trade.instrument.TradeInstrument").parameters([
        transactionRef: "TF-EFF-TEST", lifecycleStatusId: "INST_PRE_ISSUE",
        transactionStatusId: "TRANS_DRAFT", amount: 500000,
        currencyUomId: "USD", outstandingAmount: 500000,
        issueDate: "2026-06-01", expiryDate: "2026-12-31",
        versionNumber: 1
    ]).call()
    def inst = ec.entity.find("moqui.trade.instrument.TradeInstrument")
            .condition("transactionRef", "TF-EFF-TEST").one()

    ec.service.sync().name("create#moqui.trade.importlc.ImportLetterOfCredit").parameters([
        instrumentId: inst.instrumentId,
        businessStateId: "LC_DRAFT",
        effectiveAmount: 500000,
        effectiveCurrencyUomId: "USD",
        effectiveExpiryDate: "2026-12-31",
        effectiveTolerancePositive: 0.10,
        effectiveToleranceNegative: 0.05,
        effectiveOutstandingAmount: 500000,
        cumulativeDrawnAmount: 0,
        totalAmendmentCount: 0,
        chargeAllocationEnumId: "APPLICANT",
        partialShipmentEnumId: "ALLOWED",
        transhipmentEnumId: "NOT_ALLOWED",
        confirmationEnumId: "WITHOUT",
        lcTypeEnumId: "IRREVOCABLE",
        productCatalogId: "IMP_LC_STANDARD"
    ]).call()
    def lc = ec.entity.find("moqui.trade.importlc.ImportLetterOfCredit")
            .condition("instrumentId", inst.instrumentId).one()

    then:
    lc.effectiveAmount == 500000
    lc.effectiveOutstandingAmount == 500000
    lc.cumulativeDrawnAmount == 0
    lc.totalAmendmentCount == 0
    lc.chargeAllocationEnumId == "APPLICANT"
    lc.lcTypeEnumId == "IRREVOCABLE"
    lc.productCatalogId == "IMP_LC_STANDARD"
}
```

- [ ] **Step 2: Run test — expect FAIL**

- [ ] **Step 3: Add 15 new fields to ImportLetterOfCredit**

```xml
<!-- LC-specific new fields -->
<field name="chargeAllocationEnumId" type="id"/>
<field name="partialShipmentEnumId" type="id"/>
<field name="transhipmentEnumId" type="id"/>
<field name="latestShipmentDate" type="date"/>
<field name="confirmationEnumId" type="id"/>
<field name="lcTypeEnumId" type="id"/>
<field name="productCatalogId" type="id"/>

<!-- Replicated effective values — updated by amendments, read by all lifecycle services -->
<field name="effectiveAmount" type="number-decimal"/>
<field name="effectiveCurrencyUomId" type="id"/>
<field name="effectiveExpiryDate" type="date"/>
<field name="effectiveTolerancePositive" type="number-decimal"/>
<field name="effectiveToleranceNegative" type="number-decimal"/>
<field name="effectiveOutstandingAmount" type="number-decimal"/>
<field name="cumulativeDrawnAmount" type="number-decimal" default="0"/>
<field name="totalAmendmentCount" type="number-integer" default="0"/>
```

Also add relationship to TradeProductCatalog:
```xml
<relationship type="one" related="moqui.trade.instrument.TradeProductCatalog">
    <key-map field-name="productCatalogId"/>
</relationship>
```

- [ ] **Step 4: Run test — expect PASS**
- [ ] **Step 5: Commit**

---

### Task 1.7: Create PresentationDiscrepancy and ImportLcSettlement Entities

**BDD Scenarios:** BDD-IMP-FLOW-05, BDD-IMP-DOC-02, BDD-IMP-SET-01, BDD-IMP-SET-02
**BRD Requirements:** REQ-IMP-SPEC-03, REQ-IMP-SPEC-04

**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/entity/ImportLcEntities.xml`
- Test: `src/test/groovy/moqui/trade/finance/ImportLcEntitiesSpec.groovy`

- [ ] **Step 1: Write the failing tests** — one per new entity

- [ ] **Step 2: Run tests — expect FAIL**

- [ ] **Step 3: Add two new entities**

**PresentationDiscrepancy:**
```xml
<!-- Records individual discrepancy items found during document examination -->
<entity entity-name="PresentationDiscrepancy" package="moqui.trade.importlc">
    <field name="discrepancyId" type="id" is-pk="true"/>
    <field name="presentationId" type="id"/>
    <field name="discrepancyCode" type="text-short"/> <!-- ISBP standard code -->
    <field name="discrepancyDescription" type="text-long"/>
    <field name="isWaived" type="text-indicator" default="'N'"/>
    <field name="waivedByUserId" type="id"/>
    <field name="waivedTimestamp" type="date-time"/>
    <relationship type="one" related="moqui.trade.importlc.TradeDocumentPresentation">
        <key-map field-name="presentationId"/>
    </relationship>
</entity>
```

**ImportLcSettlement:**
```xml
<!-- Settlement record for document presentation payments -->
<entity entity-name="ImportLcSettlement" package="moqui.trade.importlc">
    <field name="settlementId" type="id" is-pk="true"/>
    <field name="presentationId" type="id"/>
    <field name="instrumentId" type="id"/>
    <field name="principalAmount" type="number-decimal"/>
    <field name="settlementCurrencyUomId" type="id"/>
    <field name="fxRate" type="number-decimal"/>
    <field name="localEquivalent" type="number-decimal"/>
    <field name="valueDate" type="date"/>
    <field name="debitAccountId" type="text-medium"/>
    <field name="marginApplied" type="number-decimal" default="0"/>
    <field name="netDebitAmount" type="number-decimal"/>
    <field name="chargesDetailEnumId" type="id"/> <!-- OUR, BEN, SHA -->
    <field name="maturityDate" type="date"/> <!-- For Usance LCs -->
    <relationship type="one" related="moqui.trade.importlc.TradeDocumentPresentation">
        <key-map field-name="presentationId"/>
    </relationship>
    <relationship type="one" related="moqui.trade.importlc.ImportLetterOfCredit">
        <key-map field-name="instrumentId"/>
    </relationship>
</entity>
```

- [ ] **Step 4: Run tests — expect PASS**
- [ ] **Step 5: Commit**

---

### Task 1.8: Extend Existing ImportLc Sub-Entities (Amendment, Presentation, ShippingGuarantee)

**BDD Scenarios:** BDD-IMP-AMD-01, BDD-IMP-DOC-01, BDD-IMP-SG-01
**BRD Requirements:** REQ-IMP-SPEC-02, REQ-IMP-SPEC-03, REQ-IMP-SPEC-05

**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/entity/ImportLcEntities.xml`
- Test: `src/test/groovy/moqui/trade/finance/ImportLcEntitiesSpec.groovy`

- [ ] **Step 1: Write failing tests for new fields on each entity**

- [ ] **Step 2: Run tests — expect FAIL**

- [ ] **Step 3: Add fields**

**ImportLcAmendment — add 3 fields:**
```xml
<field name="amendmentNumber" type="number-integer"/>
<field name="newTolerance" type="number-decimal"/>
<field name="chargeAllocationEnumId" type="id"/>
```

**TradeDocumentPresentation — add 4 fields:**
```xml
<field name="presentingBankBic" type="text-short"/>
<field name="presentingBankRef" type="text-medium"/>
<field name="claimCurrency" type="id"/>
<field name="regulatoryDeadline" type="date"/>
```

**ImportLcShippingGuarantee — add 4 fields:**
```xml
<field name="sgStatusId" type="id"/> <!-- SG_ISSUED, SG_REDEEMED, SG_CANCELLED -->
<field name="waiverLockFlag" type="text-indicator" default="'Y'"/>
<field name="redemptionDate" type="date"/>
<field name="issuanceFee" type="number-decimal"/>
```

- [ ] **Step 4: Run tests — expect PASS**
- [ ] **Step 5: Commit**

---

### Task 1.9: Add Enumeration and Status Seed Data

**BDD Scenarios:** All lifecycle BDD scenarios depend on valid status values
**BRD Requirements:** REQ-IMP-FLOW-01 through 08

**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/data/TradeFinanceMasterData.xml`
- Test: `src/test/groovy/moqui/trade/finance/TradeCommonEntitiesSpec.groovy`

- [ ] **Step 1: Write a test that verifies all Business State StatusItems exist and valid transitions are defined**

- [ ] **Step 2: Run test — expect FAIL**

- [ ] **Step 3: Add seed data for LC Business States and valid transitions**

```xml
<!-- LC Business States -->
<moqui.basic.StatusType statusTypeId="LcBusinessState" description="LC Business Lifecycle State"/>
<moqui.basic.StatusItem statusId="LC_DRAFT" statusTypeId="LcBusinessState" description="Draft"/>
<moqui.basic.StatusItem statusId="LC_PENDING" statusTypeId="LcBusinessState" description="Pending Approval"/>
<moqui.basic.StatusItem statusId="LC_ISSUED" statusTypeId="LcBusinessState" description="Issued"/>
<moqui.basic.StatusItem statusId="LC_DOCS_RECEIVED" statusTypeId="LcBusinessState" description="Documents Received"/>
<moqui.basic.StatusItem statusId="LC_DISCREPANT" statusTypeId="LcBusinessState" description="Discrepant"/>
<moqui.basic.StatusItem statusId="LC_ACCEPTED" statusTypeId="LcBusinessState" description="Accepted/Clean"/>
<moqui.basic.StatusItem statusId="LC_SETTLED" statusTypeId="LcBusinessState" description="Settled"/>
<moqui.basic.StatusItem statusId="LC_CLOSED" statusTypeId="LcBusinessState" description="Closed"/>
<moqui.basic.StatusItem statusId="LC_CANCELLED" statusTypeId="LcBusinessState" description="Cancelled"/>

<!-- Valid State Transitions (Design Spec §3.2) -->
<moqui.basic.StatusValidChange statusId="LC_DRAFT" toStatusId="LC_PENDING"/>
<moqui.basic.StatusValidChange statusId="LC_PENDING" toStatusId="LC_ISSUED"/>
<moqui.basic.StatusValidChange statusId="LC_PENDING" toStatusId="LC_DRAFT"/> <!-- Rejection returns to Draft -->
<moqui.basic.StatusValidChange statusId="LC_ISSUED" toStatusId="LC_DOCS_RECEIVED"/>
<moqui.basic.StatusValidChange statusId="LC_ISSUED" toStatusId="LC_CANCELLED"/>
<moqui.basic.StatusValidChange statusId="LC_ISSUED" toStatusId="LC_CLOSED"/> <!-- Auto-expiry -->
<moqui.basic.StatusValidChange statusId="LC_DOCS_RECEIVED" toStatusId="LC_DISCREPANT"/>
<moqui.basic.StatusValidChange statusId="LC_DOCS_RECEIVED" toStatusId="LC_ACCEPTED"/>
<moqui.basic.StatusValidChange statusId="LC_DISCREPANT" toStatusId="LC_ACCEPTED"/> <!-- Waiver -->
<moqui.basic.StatusValidChange statusId="LC_DISCREPANT" toStatusId="LC_CLOSED"/> <!-- Refusal -->
<moqui.basic.StatusValidChange statusId="LC_ACCEPTED" toStatusId="LC_SETTLED"/>
<moqui.basic.StatusValidChange statusId="LC_SETTLED" toStatusId="LC_CLOSED"/>
<moqui.basic.StatusValidChange statusId="LC_SETTLED" toStatusId="LC_ISSUED"/> <!-- Partial draw -->

<!-- Lifecycle System Statuses -->
<moqui.basic.StatusType statusTypeId="InstrumentLifecycle" description="System Instrument Lifecycle Status"/>
<moqui.basic.StatusItem statusId="INST_PRE_ISSUE" statusTypeId="InstrumentLifecycle" description="Pre-Issue"/>
<moqui.basic.StatusItem statusId="INST_PENDING_APPROVAL" statusTypeId="InstrumentLifecycle" description="Pending Approval"/>
<moqui.basic.StatusItem statusId="INST_AUTHORIZED" statusTypeId="InstrumentLifecycle" description="Authorized"/>
<moqui.basic.StatusItem statusId="INST_HOLD" statusTypeId="InstrumentLifecycle" description="Compliance Hold"/>
<moqui.basic.StatusItem statusId="INST_ACTIVE" statusTypeId="InstrumentLifecycle" description="Active"/>
<moqui.basic.StatusItem statusId="INST_CLOSED" statusTypeId="InstrumentLifecycle" description="Closed"/>

<!-- Enumeration Types for LC domain -->
<moqui.basic.EnumerationType enumTypeId="ChargeAllocation" description="Fee Charge Allocation"/>
<moqui.basic.Enumeration enumId="APPLICANT" enumTypeId="ChargeAllocation" description="Applicant"/>
<moqui.basic.Enumeration enumId="BENEFICIARY" enumTypeId="ChargeAllocation" description="Beneficiary"/>
<moqui.basic.Enumeration enumId="SHARED" enumTypeId="ChargeAllocation" description="Shared"/>
```

- [ ] **Step 4: Run test — expect PASS**
- [ ] **Step 5: Commit**

---

## Phase 2: Core Services — Lifecycle Processing

> Services build on the entity schema from Phase 1.

---

### Task 2.1: Implement SWIFT Data Capture Validation Service (Layer 1)

**BDD Scenarios:** BDD-CMN-VAL-05
**BRD Requirements:** REQ-COM-VAL-03, Design Spec §4.0

**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/service/ImportLcValidationServices.xml`
- Test: `src/test/groovy/moqui/trade/finance/ImportLcValidationServicesSpec.groovy`

- [ ] **Step 1: Write the failing test**

```groovy
def "validate#SwiftFields rejects invalid X-Character in goodsDescription"() {
    when:
    def result = ec.service.sync().name("TradeFinance.ImportLcValidationServices.validate#SwiftFields")
        .parameters([
            goodsDescription: "Steel Rods @ 50mm diameter",
            transactionRef: "TF-IMP-26-0001",
            portOfLoading: "Ho Chi Minh City"
        ]).call()

    then:
    ec.message.hasError()
    ec.message.errorsString.contains("goodsDescription")
    ec.message.errorsString.contains("@")
}
```

- [ ] **Step 2: Run test — expect FAIL**

- [ ] **Step 3: Implement `validate#SwiftFields` service**

The service scans all SWIFT-mapped fields against the X Character Set (`A-Z`, `a-z`, `0-9`, `- / ? : ( ) . , ' +` and space). It returns field-specific errors with the offending character and position. Key checks:
- `transactionRef`: max 16 chars, no leading/trailing `/`, no `//`
- `goodsDescription`, `documentsRequired`, `additionalConditions`: X Character Set only
- `registeredAddress`: max 4 lines × 35 chars
- `portOfLoading`, `portOfDischarge`: max 65 chars

```xml
<service verb="validate" noun="SwiftFields">
    <in-parameters>
        <parameter name="transactionRef" type="String"/>
        <parameter name="goodsDescription" type="String"/>
        <parameter name="documentsRequired" type="String"/>
        <parameter name="additionalConditions" type="String"/>
        <parameter name="portOfLoading" type="String"/>
        <parameter name="portOfDischarge" type="String"/>
    </in-parameters>
    <actions><script><![CDATA[
        import java.util.regex.Pattern
        def SWIFT_X_PATTERN = Pattern.compile("^[A-Za-z0-9 \\-/?:().,'+]+\$")
        def errors = []

        def checkXCharSet = { String fieldName, String value ->
            if (!value) return
            value.eachWithIndex { ch, idx ->
                if (!SWIFT_X_PATTERN.matcher(ch.toString()).matches()) {
                    errors.add("${fieldName} contains invalid SWIFT character '${ch}' at position ${idx + 1}")
                }
            }
        }

        // Reference field checks
        if (transactionRef) {
            if (transactionRef.length() > 16) errors.add("transactionRef exceeds 16 character SWIFT limit")
            if (transactionRef.startsWith("/") || transactionRef.endsWith("/")) errors.add("transactionRef cannot start or end with '/'")
            if (transactionRef.contains("//")) errors.add("transactionRef cannot contain '//'")
        }

        // Narrative field checks
        checkXCharSet("goodsDescription", goodsDescription)
        checkXCharSet("documentsRequired", documentsRequired)
        checkXCharSet("additionalConditions", additionalConditions)

        // Length checks
        if (portOfLoading && portOfLoading.length() > 65) errors.add("portOfLoading exceeds 65 character SWIFT limit")
        if (portOfDischarge && portOfDischarge.length() > 65) errors.add("portOfDischarge exceeds 65 character SWIFT limit")

        errors.each { ec.message.addError(it) }
    ]]></script></actions>
</service>
```

- [ ] **Step 4: Run test — expect PASS**
- [ ] **Step 5: Commit**

---

### Task 2.2: Update create#ImportLetterOfCredit to Initialize Effective Values

**BDD Scenarios:** BDD-IMP-FLOW-01, BDD-IMP-ISS-03
**BRD Requirements:** REQ-IMP-SPEC-01

**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/service/ImportLcServices.xml` (create#ImportLetterOfCredit)
- Test: `src/test/groovy/moqui/trade/finance/BddImportLcModuleSpec.groovy`

- [ ] **Step 1: Write the failing test** — after calling `create#ImportLetterOfCredit`, verify that `effectiveAmount`, `effectiveExpiryDate`, `effectiveOutstandingAmount`, and `cumulativeDrawnAmount` are correctly initialized from the input values
- [ ] **Step 2: Run test — expect FAIL**
- [ ] **Step 3: Modify the existing create service to also set transaction management fields on TradeInstrument and initialize effective values on ImportLetterOfCredit**
- [ ] **Step 4: Run test — expect PASS**
- [ ] **Step 5: Commit**

---

### Task 2.3: Update create#Amendment to Maintain Effective Values

**BDD Scenarios:** BDD-IMP-AMD-01, BDD-IMP-AMD-02, BDD-IMP-AMD-05
**BRD Requirements:** REQ-IMP-SPEC-02

**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/service/ImportLcServices.xml` (create#Amendment)
- Test: `src/test/groovy/moqui/trade/finance/BddImportLcModuleSpec.groovy`

- [ ] **Step 1: Write the failing test**

```groovy
def "Amendment authorization updates ImportLetterOfCredit effective values"() {
    given: "An issued LC with effectiveAmount = 50000"
    // ... setup code ...

    when: "A financial amendment of +20000 is created and authorized"
    ec.service.sync().name("TradeFinance.ImportLcServices.create#Amendment").parameters([
        instrumentId: instrumentId,
        amountAdjustment: 20000,
        isFinancial: "Y"
    ]).call()
    // ... authorize amendment ...

    then: "Effective values on ImportLetterOfCredit are updated"
    def lc = ec.entity.find("moqui.trade.importlc.ImportLetterOfCredit")
            .condition("instrumentId", instrumentId).one()
    lc.effectiveAmount == 70000
    lc.effectiveOutstandingAmount == 70000
    lc.totalAmendmentCount == 1

    and: "TradeInstrument.amount remains unchanged (original snapshot)"
    def inst = ec.entity.find("moqui.trade.instrument.TradeInstrument")
            .condition("instrumentId", instrumentId).one()
    inst.amount == 50000
    inst.versionNumber == 2
}
```

- [ ] **Step 2: Run test — expect FAIL**
- [ ] **Step 3: Update `create#Amendment` to update effectiveAmount, effectiveOutstandingAmount, totalAmendmentCount, and versionNumber after authorization**
- [ ] **Step 4: Run test — expect PASS**
- [ ] **Step 5: Commit**

---

### Task 2.4: Update settle#Presentation to Track Effective Outstanding

**BDD Scenarios:** BDD-IMP-FLOW-07, BDD-IMP-SET-03
**BRD Requirements:** REQ-IMP-SPEC-04

**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/service/ImportLcServices.xml` (settle#Presentation)
- Modify: `runtime/component/TradeFinance/service/TradeAccountingServices.xml`
- Test: `src/test/groovy/moqui/trade/finance/BddImportLcModuleSpec.groovy`

- [ ] **Step 1: Write the failing test** for partial draw updating `effectiveOutstandingAmount` and `cumulativeDrawnAmount`
- [ ] **Step 2: Run test — expect FAIL**
- [ ] **Step 3: Update settle#Presentation — after settlement, update `effectiveOutstandingAmount -= claimAmount`, `cumulativeDrawnAmount += claimAmount`. If `effectiveOutstandingAmount == 0`, transition to LC_CLOSED. Otherwise, return to LC_ISSUED.**
- [ ] **Step 4: Run test — expect PASS**
- [ ] **Step 5: Write additional test for revolving LC reinstatement (BDD-IMP-VAL-03)**

```groovy
def "Revolving LC reinstates effectiveOutstandingAmount after full draw"() {
    given: "An LC with TradeProductCatalog.allowRevolving = Y and effectiveAmount = 10000"
    // ... setup revolving LC ...

    when: "Full draw of 10000 is settled"
    // ... settle full amount ...

    then: "effectiveOutstandingAmount is reinstated back to effectiveAmount"
    def lc = ec.entity.find("moqui.trade.importlc.ImportLetterOfCredit")
            .condition("instrumentId", instrumentId).one()
    lc.cumulativeDrawnAmount == 10000
    lc.effectiveOutstandingAmount == 10000  // Reinstated
    lc.businessStateId == "LC_ISSUED"       // Not closed
}
```

- [ ] **Step 6: Run test — expect FAIL**
- [ ] **Step 7: Add revolving logic to settle#Presentation — check `TradeProductCatalog.allowRevolving`. If `Y` and `effectiveOutstandingAmount == 0`, reinstate `effectiveOutstandingAmount = effectiveAmount` and return to LC_ISSUED instead of closing.**
- [ ] **Step 8: Run test — expect PASS**
- [ ] **Step 9: Commit**

---

### Task 2.5: Implement Discrepancy Waiver and MT 752 Trigger

**BDD Scenarios:** BDD-IMP-DOC-03
**BRD Requirements:** REQ-IMP-SPEC-03

**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/service/ImportLcServices.xml`
- Test: `src/test/groovy/moqui/trade/finance/BddImportLcModuleSpec.groovy`

- [ ] **Step 1: Write failing test** — after `update#PresentationWaiver` with `applicantDecisionEnumId = WAIVED`, verify `businessStateId` transitions from `LC_DISCREPANT` to `LC_ACCEPTED` and a SwiftMessage of type `MT752` is generated
- [ ] **Step 2: Run test — expect FAIL**
- [ ] **Step 3: Implement `update#PresentationWaiver` service**
- [ ] **Step 4: Run test — expect PASS**
- [ ] **Step 5: Commit**

---

### Task 2.6: Implement Business State Transition Guard

**BDD Scenarios:** BDD-IMP-FLOW-08, BDD-IMP-FLOW-09
**BRD Requirements:** REQ-IMP-FLOW-08

**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/service/ImportLcServices.xml`
- Test: `src/test/groovy/moqui/trade/finance/BddImportLcModuleSpec.groovy`

- [ ] **Step 1: Write failing test** — calling `transition#BusinessState(LC_DRAFT → LC_SETTLED)` should throw an error
- [ ] **Step 2: Run test — expect FAIL**
- [ ] **Step 3: Implement `transition#BusinessState` — validates proposed transition against `StatusValidChange` seed data before updating `businessStateId`**
- [ ] **Step 4: Run test — expect PASS**
- [ ] **Step 5: Commit**

---

## Phase 3: SWIFT Message Generation Services

> Services for generating MT messages from entity data.

---

### Task 3.1: Implement MT 701 Continuation Logic

**BDD Scenarios:** BDD-IMP-SWT-05
**BRD Requirements:** REQ-IMP-SWIFT-05, Design Spec §4.2

**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/service/SwiftGenerationServices.xml`
- Test: `src/test/groovy/moqui/trade/finance/BddImportLcModuleSpec.groovy`

- [ ] **Step 1: Write the failing test** — generate MT 700 with `goodsDescription` > 6500 chars → should produce TWO SwiftMessage records (MT700 with Tag 27 = `1/2` + MT701 with Tag 27 = `2/2`)
- [ ] **Step 2: Run test — expect FAIL**
- [ ] **Step 3: In `generate#Mt700`, add overflow detection for Tags 45A/46A/47A. If any exceeds 100 lines × 65 chars, call new `generate#Mt701` service to produce the continuation message. Update Tag 27 on both.**
- [ ] **Step 4: Run test — expect PASS**
- [ ] **Step 5: Commit**

---

### Task 3.2: Implement MT 707 Amendment Message

**BDD Scenarios:** BDD-IMP-SWT-06
**BRD Requirements:** Design Spec §4.3

**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/service/SwiftGenerationServices.xml`
- Test: `src/test/groovy/moqui/trade/finance/BddImportLcModuleSpec.groovy`

- [ ] **Step 1: Write failing test** — after amendment authorization, `generate#Mt707` produces a SwiftMessage with correct Tags 20, 30, 32B/33B, 34B, 31E
- [ ] **Step 2: Run test — expect FAIL**
- [ ] **Step 3: Implement `generate#Mt707` service reading from `ImportLcAmendment` and `TradeInstrument`**
- [ ] **Step 4: Run test — expect PASS**
- [ ] **Step 5: Commit**

---

### Task 3.3: Implement MT 750, 734, 752 Discrepancy Messages

**BDD Scenarios:** BDD-IMP-DOC-02, BDD-IMP-DOC-03
**BRD Requirements:** Design Spec §4.4, §4.5, §4.6

**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/service/SwiftGenerationServices.xml`
- Test: `src/test/groovy/moqui/trade/finance/BddImportLcModuleSpec.groovy`

- [ ] **Step 1: Write failing tests** for each message type
- [ ] **Step 2: Run tests — expect FAIL**
- [ ] **Step 3: Implement `generate#Mt750`, `generate#Mt734`, `generate#Mt752` services**
- [ ] **Step 4: Run tests — expect PASS**
- [ ] **Step 5: Commit**

---

### Task 3.4: Implement MT 732 Advice of Discharge

**BDD Scenarios:** BDD-IMP-SET-01
**BRD Requirements:** Design Spec §4.7

**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/service/SwiftGenerationServices.xml`
- Test: `src/test/groovy/moqui/trade/finance/BddImportLcModuleSpec.groovy`

- [ ] **Step 1: Write failing test** — after usance acceptance, `generate#Mt732` produces a SwiftMessage with Tags 20, 21, 32B, 71B
- [ ] **Step 2: Run test — expect FAIL**
- [ ] **Step 3: Implement `generate#Mt732` — reads maturity date from settlement or acceptance, generates Tag 32B with the accepted amount**
- [ ] **Step 4: Run test — expect PASS**
- [ ] **Step 5: Commit**

---

### Task 3.5: Implement MT 799 and Settlement Messages (MT 202, MT 103)

**BDD Scenarios:** BDD-IMP-CAN-01, BDD-IMP-SET-02
**BRD Requirements:** Design Spec §4.8, §4.9

**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/service/SwiftGenerationServices.xml`
- Test: `src/test/groovy/moqui/trade/finance/BddImportLcModuleSpec.groovy`

- [ ] **Step 1: Write failing tests**
- [ ] **Step 2: Run tests — expect FAIL**
- [ ] **Step 3: Implement `generate#Mt799`, `generate#Mt202`, `generate#Mt103`**
- [ ] **Step 4: Run tests — expect PASS**
- [ ] **Step 5: Commit**

---

## Phase 4: Authorization & Compliance Services

---

### Task 4.1: Implement Priority Queue Ordering and Tier Routing via Effective Amount

**BDD Scenarios:** BDD-CMN-AUTH-01, BDD-CMN-AUTH-03, BDD-CMN-AUTH-05
**BRD Requirements:** REQ-COM-AUTH-01, REQ-COM-AUTH-03

**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/service/AuthorizationServices.xml`
- Test: `src/test/groovy/moqui/trade/finance/AuthorizationServicesSpec.groovy`

- [ ] **Step 1: Write failing test** — amendment tier uses `effectiveAmount` (new total), not the delta
- [ ] **Step 2: Run test — expect FAIL**
- [ ] **Step 3: Update `evaluate#MakerCheckerMatrix` to read `ImportLetterOfCredit.effectiveAmount` for tier routing. Add `priorityEnumId` ordering to the Checker queue query.**
- [ ] **Step 4: Run test — expect PASS**
- [ ] **Step 5: Commit**

---

### Task 4.2: Implement Compliance Hold and Release

**BDD Scenarios:** BDD-CMN-AUTH-04, BDD-CMN-NOT-02
**BRD Requirements:** REQ-COM-AUTH-03

**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/service/AuthorizationServices.xml`
- Modify: `runtime/component/TradeFinance/service/TradeComplianceServices.xml`
- Test: `src/test/groovy/moqui/trade/finance/AuthorizationServicesSpec.groovy`

- [ ] **Step 1: Write failing test** — `check#Sanctions` returning `isHit=true` sets `lifecycleStatusId = INST_HOLD`. `release#ComplianceHold` returns to `INST_PENDING_APPROVAL`. Only users with `TRADE_COMPLIANCE_OFFICER` role can invoke release.
- [ ] **Step 2: Run test — expect FAIL**
- [ ] **Step 3: Implement `release#ComplianceHold` service**
- [ ] **Step 4: Run test — expect PASS**
- [ ] **Step 5: Commit**

---

### Task 4.3: Implement Dual Checker Enforcement for Tier 4

**BDD Scenarios:** BDD-CMN-AUTH-02
**BRD Requirements:** REQ-COM-AUTH-02

**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/service/AuthorizationServices.xml`
- Modify: `runtime/component/TradeFinance/entity/TradeCommonEntities.xml`
- Test: `src/test/groovy/moqui/trade/finance/AuthorizationServicesSpec.groovy`

- [ ] **Step 1: Write the failing test**

```groovy
def "Tier 4 requires two distinct Checkers for authorization"() {
    given: "A transaction with baseEquivalentAmount = 8,000,000 (Tier 4)"
    // ... setup TradeInstrument with amount 8M ...
    
    when: "First Checker approves"
    ec.service.sync().name("TradeFinance.AuthorizationServices.authorize#Instrument").parameters([
        instrumentId: instrumentId
    ]).call()
    def inst = ec.entity.find("moqui.trade.instrument.TradeInstrument")
            .condition("instrumentId", instrumentId).one()
    
    then: "Transaction is partially approved, not fully authorized"
    inst.lifecycleStatusId == "INST_PARTIAL_APPROVAL"
    
    and: "A TradeApprovalRecord is created"
    def approvals = ec.entity.find("moqui.trade.instrument.TradeApprovalRecord")
            .condition("instrumentId", instrumentId).list()
    approvals.size() == 1
    approvals[0].approvalSequence == 1
}
```

- [ ] **Step 2: Run test — expect FAIL**

- [ ] **Step 3: Add `TradeApprovalRecord` entity and `INST_PARTIAL_APPROVAL` status**

```xml
<!-- Records individual approval actions for multi-approval workflows -->
<entity entity-name="TradeApprovalRecord" package="moqui.trade.instrument">
    <field name="approvalRecordId" type="id" is-pk="true"/>
    <field name="instrumentId" type="id"/>
    <field name="approverUserId" type="id"/>
    <field name="approvalSequence" type="number-integer"/>
    <field name="approvalTimestamp" type="date-time"/>
    <field name="approvalDecisionEnumId" type="id"/> <!-- APPROVED, REJECTED -->
    <relationship type="one" related="moqui.trade.instrument.TradeInstrument">
        <key-map field-name="instrumentId"/>
    </relationship>
</entity>
```

Update `authorize#Instrument` to check if current tier is TIER_4. If so, check how many `TradeApprovalRecord` entries exist. On first approval, set `lifecycleStatusId = INST_PARTIAL_APPROVAL` and create a record. On second approval (different user from Maker and first Checker), complete authorization.

- [ ] **Step 4: Run test — expect PASS**
- [ ] **Step 5: Commit**

---

## Phase 5: Batch Processing & Settlement Edge Cases

---

### Task 5.1: Implement Auto-Expiry Batch Job

**BDD Scenarios:** BDD-IMP-CAN-01
**BRD Requirements:** REQ-IMP-SPEC-06

**User-Facing:** NO

**Files:**
- Create: `runtime/component/TradeFinance/service/ImportLcBatchServices.xml`
- Test: `src/test/groovy/moqui/trade/finance/BddImportLcModuleSpec.groovy`

- [ ] **Step 1: Write failing test** — create an LC with `effectiveExpiryDate` 16+ days ago, run `batch#AutoExpiry`, verify it transitions to `LC_CLOSED` and releases limits
- [ ] **Step 2: Run test — expect FAIL**
- [ ] **Step 3: Implement `batch#AutoExpiry` — finds LCs where `effectiveExpiryDate + mailDaysGracePeriod < today`, transitions to LC_CLOSED, releases facility earmarks**
- [ ] **Step 4: Run test — expect PASS**
- [ ] **Step 5: Commit**

---

## Phase 6: Full BDD Regression

---

### Task 6.1: Run Full BDD Test Suites and Fix Breakages

**BDD Scenarios:** All BDD scenarios
**BRD Requirements:** All requirements

**User-Facing:** NO

**Files:**
- Modify: `src/test/groovy/moqui/trade/finance/BddCommonModuleSpec.groovy`
- Modify: `src/test/groovy/moqui/trade/finance/BddImportLcModuleSpec.groovy`

- [ ] **Step 1: Run full test suite**

```bash
cd runtime/component/TradeFinance && gradle test 2>&1 | tail -50
```

- [ ] **Step 2: Parse the Spock HTML report for failures**

```bash
open build/reports/tests/test/index.html
```

- [ ] **Step 3: Fix any test failures caused by entity schema changes (new required fields, changed field names)**

- [ ] **Step 4: Update BDD spec tests to align with v3.0 BDD scenarios for any scenarios not yet covered in Phase 2-5**

- [ ] **Step 5: Run full suite again — expect 100% PASS**

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "test: align BDD specs with design v3.0, fix breakages from entity schema changes"
```

---

## Verification Plan

### Automated Tests

All tasks follow TDD (test-first). After all tasks are complete:

```bash
# Run the full Spock test suite
cd runtime/component/TradeFinance && gradle test

# View detailed HTML report
open build/reports/tests/test/index.html
```

**Expected:** 100% pass rate across all spec files:
- `TradeCommonEntitiesSpec.groovy` — entity persistence (Phase 1)
- `ImportLcEntitiesSpec.groovy` — entity persistence (Phase 1)
- `ImportLcValidationServicesSpec.groovy` — SWIFT Layer 1 validation (Task 2.1)
- `BddCommonModuleSpec.groovy` — all 42 common module BDD scenarios
- `BddImportLcModuleSpec.groovy` — all 37 import LC BDD scenarios
- `AuthorizationServicesSpec.groovy` — tier routing and compliance hold
- `ShippingGuaranteeSpec.groovy` — SG lifecycle
- `EndToEndImportLcSpec.groovy` — full E2E lifecycle

### Manual Verification

After automated tests pass, Loc should verify:

1. **Entity auto-creation:** Start the Moqui server (`gradle run`), check that all new/extended tables are created automatically in H2 without errors in the log
2. **Seed data loading:** Verify `TradeProductCatalogData.xml` loads without errors on startup (check `moqui.log` for any data import warnings)
3. **REST API smoke test:** Hit the REST API endpoints to verify the new fields are exposed (e.g., `GET /rest/s1/TradeFinance/ImportLetterOfCredit/{id}` should return `effectiveAmount` in the response)
