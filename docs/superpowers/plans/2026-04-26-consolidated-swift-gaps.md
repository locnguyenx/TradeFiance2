# SWIFT Gaps Consolidation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Achieve 100% SWIFT compliance by implementing missing mandatory and optional tags in the entity schema, Layer 1 validation, and Layer 2 generation.

**Architecture:** 
1.  **Entity Layer**: Enhance `TradeDocumentPresentation` with missing SWIFT tags.
2.  **Validation Layer (Layer 1)**: Update `ImportLcValidationServices` to enforce mandatory fields and mutual exclusion rules.
3.  **Generation Layer (Layer 2)**: Update `SwiftGenerationServices` to map new fields to standard SWIFT MT tags (40A, 41a, 49, 26E, 34B, 32A).

**Tech Stack:** Moqui Framework, Groovy, Spock, XML Entities/Services.

---

### Task 1: Enhance TradeDocumentPresentation Entity

**BDD Scenarios:** N/A (Schema support for FR-ENT-30, FR-ENT-31, FR-ENT-32, FR-ENT-33)
**BRD Requirements:** FR-ENT-30, FR-ENT-31, FR-ENT-32, FR-ENT-33
**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/entity/ImportLcEntities.xml`
- Test: `runtime/component/TradeFinance/src/test/groovy/trade/ImportLcEntitiesSpec.groovy`

- [ ] **Step 1: Write failing test for new entity fields**
```groovy
def "Entity has missing presentation fields"() {
    given:
    def entityDef = ec.entity.getEntityDefinition("trade.importlc.TradeDocumentPresentation")
    
    expect:
    entityDef.getField("chargesDeducted") != null
    entityDef.getField("senderToReceiverPresentationInfo") != null
}
```
- [ ] **Step 2: Run test to verify it fails**
Run: `./gradlew test --tests trade.ImportLcEntitiesSpec`
- [ ] **Step 3: Add fields to ImportLcEntities.xml**
```xml
<entity entity-name="TradeDocumentPresentation" package="trade.importlc">
    <!-- existing fields ... -->
    <field name="chargesDeducted" type="text-long"/> <!-- Tag 73 -->
    <field name="senderToReceiverPresentationInfo" type="text-long"/> <!-- Tag 72Z -->
</entity>
```
- [ ] **Step 4: Run test to verify it passes**
- [ ] **Step 5: Commit**

---

### Task 2: Implement Mandatory Field Validation (40A, 41a, 49)

**BDD Scenarios:** Validate mandatory Form of Credit, Validate mandatory Available By, Validate mandatory Confirmation
**BRD Requirements:** FR-ENT-21, FR-ENT-23, FR-ENT-26
**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/service/trade/importlc/ImportLcValidationServices.xml`
- Test: `runtime/component/TradeFinance/src/test/groovy/trade/SwiftValidationSpec.groovy`

- [ ] **Step 1: Write failing tests for mandatory fields**
```groovy
def "Validate mandatory Form of Credit (40A)"() {
    given: "An LC missing lcTypeEnumId"
    def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
        .parameters([transactionRef: "TF-40A", lcAmount: 1000.0, lcCurrencyUomId: "USD"]).call()
    
    when: "validate#SwiftFields is called"
    def result = ec.service.sync().name("trade.importlc.ImportLcValidationServices.validate#SwiftFields")
        .parameters([entityType: "ImportLetterOfCredit", entityId: res.instrumentId]).call()

    then: "Error for 40A"
    result.errors.any { it.fieldName == "lcTypeEnumId" }
}
```
- [ ] **Step 2: Run test to verify failure**
- [ ] **Step 3: Update ImportLcValidationServices.xml with mandatory checks**
- [ ] **Step 4: Run test to verify success**
- [ ] **Step 5: Commit**

---

### Task 3: Implement Mutual Exclusion (Tolerance vs Max Credit & Shipment Period vs Latest Shipment)

**BDD Scenarios:** Mutual Exclusion: Max Credit blocks Tolerance, Mutual Exclusion: Shipment Period blocks Latest Shipment
**BRD Requirements:** FR-SGC-07, FR-SGC-08
**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/service/trade/importlc/ImportLcValidationServices.xml`
- Test: `runtime/component/TradeFinance/src/test/groovy/trade/SwiftValidationSpec.groovy`

- [x] **Step 1: Write failing test for mutual exclusion**
- [x] **Step 2: Run test to verify failure**
- [x] **Step 3: Implement mutual exclusion logic**
- [x] **Step 4: Run test to verify success**
- [x] **Step 5: Commit**

---

### Task 4: Update MT700 Generation (40A, 41a, 49)

**BDD Scenarios:** Assemble MT700 with Available With logic
**BRD Requirements:** FR-SGC-04
**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/service/trade/SwiftGenerationServices.xml`
- Test: `runtime/component/TradeFinance/src/test/groovy/trade/SwiftGenerationSpec.groovy`

- [/] **Step 1: Write failing test for tag mappings**
```groovy
def "MT700 contains Tags 40A, 41a, 49"() {
    given: "An approved LC with 40A, 41A, 49 fields"
    // setup LC ...
    when: "Message generated"
    def res = ec.service.sync().name("trade.SwiftGenerationServices.generate#Mt700").parameters(...).call()
    then:
    res.messageContent.contains(":40A:IRREVOCABLE")
    res.messageContent.contains(":41A:ANY BANK")
}
```
- [ ] **Step 2: Run test to verify failure**
- [ ] **Step 3: Update generate#Mt700 with new mappings**
- [ ] **Step 4: Run test to verify success**
- [ ] **Step 5: Commit**

---

### Task 5: Implement MT734 Value Date (Tag 32A)

**BDD Scenarios:** Assemble MT734 Notice of Refusal Tag 32A
**BRD Requirements:** FR-SGC-06
**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/service/trade/SwiftGenerationServices.xml`
- Test: `runtime/component/TradeFinance/src/test/groovy/trade/SwiftGenerationSpec.groovy`

- [ ] **Step 1: Write failing test for Tag 32A assembly**
```groovy
def "MT734 contains Tag 32A with YYMMDD"() {
    given: "A presentation with date 2026-04-26"
    // setup presentation ...
    when: "MT734 generated"
    def res = ec.service.sync().name("trade.SwiftGenerationServices.generate#Mt734").parameters(...).call()
    then:
    res.messageContent.contains(":32A:260426")
}
```
- [ ] **Step 2: Run test to verify failure**
- [ ] **Step 3: Implement Tag 32A assembly logic**
- [ ] **Step 4: Run test to verify success**
- [ ] **Step 5: Commit**
