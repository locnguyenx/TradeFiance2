# SWIFT Validation Infrastructure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add entity schema fields and a reusable `validate#SwiftFields` service that enforces SWIFT compliance rules at data capture time (Layer 1).

**Architecture:** Extend existing entities with ~20 new fields and `messageStatusId`. Implement a single Groovy-based validation service that accepts any SWIFT-bound entity and returns field-level error messages. No UI changes in this plan.

**Tech Stack:** Moqui XML entities, Moqui XML services with embedded Groovy scripts, Spock framework for testing.

---

## File Structure

| Action | File | Responsibility |
|:---|:---|:---|
| Modify | `entity/ImportLcEntities.xml` | Add fields to `ImportLetterOfCredit`, `SwiftMessage`, `TradeDocumentPresentation` |
| Modify | `entity/TradeCommonEntities.xml` | Add fields to `TradeInstrument` |
| Create | `service/trade/ImportLcValidationServices.xml` | `validate#SwiftFields` service definition |
| Create | `src/test/groovy/SwiftValidationSpec.groovy` | Spock test spec for SWIFT validation |
| Modify | `data/TradeSeedData.xml` | Add `SWIFT_MSG_DRAFT`/`SWIFT_MSG_ACTIVE` status enumerations, `HOLDING_DOCUMENTS`/`RETURNING_DOCUMENTS` enumerations |

All paths are relative to `runtime/component/TradeFinance/`.

---

### Task 1: Add Entity Schema Fields — ImportLetterOfCredit

**BDD Scenarios:** Provides data model for all BDD-SWV-* scenarios
**BRD Requirements:** FR-ENT-01 through FR-ENT-11

**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/entity/ImportLcEntities.xml`

- [ ] **Step 1: Add 11 new fields to ImportLetterOfCredit entity**

After the existing `confirmationEnumId` field (~line 41), add:

```xml
<field name="receiptPlace" type="text-medium"/> <!-- Tag 44A: max 65, X charset -->
<field name="finalDeliveryPlace" type="text-medium"/> <!-- Tag 44B: max 65, X charset -->
<field name="shipmentPeriodText" type="text-medium"/> <!-- Tag 44D: max 65, X charset. Mutually exclusive with latestShipmentDate -->
<field name="maxCreditAmountFlag" type="text-indicator"/> <!-- Tag 39B: Y/N. Mutually exclusive with tolerance -->
<field name="additionalAmountsText" type="text-long"/> <!-- Tag 39C: max 4x35, X charset -->
<field name="mixedPaymentDetails" type="text-long"/> <!-- Tag 42M: required if tenor=MIXED -->
<field name="deferredPaymentDetails" type="text-long"/> <!-- Tag 42P: required if tenor=DEF_PAYMENT/NEGOTIATION -->
<field name="usanceBaseDate" type="text-medium"/> <!-- Tag 42C: required if tenor!=SIGHT -->
<field name="bankToBankInstructions" type="text-very-long"/> <!-- Tag 78: max 12x65, X charset -->
<field name="presentationPeriodDays" type="number-integer"/> <!-- Tag 48 -->
<field name="chargeAllocationText" type="text-long"/> <!-- Tag 71D: max 6x35, X charset -->
```

- [ ] **Step 2: Add messageStatusId to SwiftMessage entity**

In the `SwiftMessage` entity (~line 65), add before the closing `</entity>`:

```xml
<field name="messageStatusId" type="id"/> <!-- SWIFT_MSG_DRAFT or SWIFT_MSG_ACTIVE -->
```

- [ ] **Step 3: Add documentDisposalEnumId to TradeDocumentPresentation entity**

In the `TradeDocumentPresentation` entity, add:

```xml
<field name="documentDisposalEnumId" type="id"/> <!-- Tag 77B: HOLDING_DOCUMENTS or RETURNING_DOCUMENTS -->
```

- [ ] **Step 4: Commit**

```bash
git add runtime/component/TradeFinance/entity/ImportLcEntities.xml
git commit -m "feat(entity): add SWIFT validation fields to ImportLetterOfCredit, SwiftMessage, TradeDocumentPresentation"
```

---

### Task 2: Add Entity Schema Fields — TradeInstrument

**BDD Scenarios:** Provides data model for BDD-SWV-REF-*, BDD-SWV-BIC-*, BDD-SWV-MEX-03
**BRD Requirements:** FR-ENT-12 through FR-ENT-18

**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/entity/TradeCommonEntities.xml`

- [ ] **Step 1: Add 7 new fields to TradeInstrument entity**

After existing fields in `TradeInstrument`, add:

```xml
<field name="reimbursingBankBic" type="text-short"/> <!-- Tag 53A: valid 8/11 BIC -->
<field name="reimbursingBankName" type="text-long"/> <!-- Tag 53D: max 4x35, X charset. Alt to BIC -->
<field name="adviseThroughBankBic" type="text-short"/> <!-- Tag 57A: valid 8/11 BIC -->
<field name="adviseThroughBankName" type="text-long"/> <!-- Tag 57D: max 4x35, X charset. Alt to BIC -->
<field name="preAdviceRef" type="text-short"/> <!-- Tag 23: max 16, X charset, slash rules -->
<field name="senderToReceiverInfo" type="text-long"/> <!-- Tag 72Z: max 6x35, X charset -->
<field name="beneficiaryName" type="text-long"/> <!-- Tag 59: max 4x35, X charset -->
```

- [ ] **Step 2: Commit**

```bash
git add runtime/component/TradeFinance/entity/TradeCommonEntities.xml
git commit -m "feat(entity): add SWIFT routing and party fields to TradeInstrument"
```

---

### Task 3: Add Seed Data — Status Enumerations

**BDD Scenarios:** BDD-SWG-LCY-01 through LCY-05 (from Spec B, but entity foundation set here)
**BRD Requirements:** FR-ENT-19, FR-ENT-20, FR-CFG-01, FR-CFG-02

**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/data/TradeSeedData.xml`

- [ ] **Step 1: Add enumerations and config entries**

Add to `TradeSeedData.xml`:

```xml
<!-- SWIFT Message Status -->
<moqui.basic.Enumeration enumId="SWIFT_MSG_DRAFT" description="Draft" enumTypeId="SwiftMessageStatus"/>
<moqui.basic.Enumeration enumId="SWIFT_MSG_ACTIVE" description="Active" enumTypeId="SwiftMessageStatus"/>

<!-- Document Disposal (Tag 77B) -->
<moqui.basic.Enumeration enumId="HOLDING_DOCUMENTS" description="Holding Documents" enumTypeId="DocumentDisposal"/>
<moqui.basic.Enumeration enumId="RETURNING_DOCUMENTS" description="Returning Documents" enumTypeId="DocumentDisposal"/>

<!-- Bank Configuration -->
<trade.TradeConfig configId="ISSUING_BANK_BIC" configValue="VIETBANK1XXX" description="Issuing bank SWIFT BIC for message headers"/>
<trade.TradeConfig configId="NOSTRO_ACCOUNT_USD" configValue="001-234567-USD" description="USD Nostro account for MT202 routing"/>
```

- [ ] **Step 2: Commit**

```bash
git add runtime/component/TradeFinance/data/TradeSeedData.xml
git commit -m "feat(data): add SWIFT message status enums, document disposal enums, bank config"
```

---

### Task 4: Implement validate#SwiftFields — Core Validation Logic

**BDD Scenarios:** BDD-SWV-XCS-01, XCS-02, ZCS-01, ZCS-02, REF-01, REF-02, AMT-01, BIC-01, BIC-02, LIN-01, LIN-02, LIN-03, MEX-01, MEX-02, MEX-03, CND-01, CND-02, CND-03, SVC-01, SVC-02, SVC-03, SVC-04, SVC-05
**BRD Requirements:** FR-SWV-01 through FR-SWV-09

**User-Facing:** NO

**Files:**
- Create: `runtime/component/TradeFinance/service/trade/ImportLcValidationServices.xml`
- Create: `runtime/component/TradeFinance/src/test/groovy/SwiftValidationSpec.groovy`

- [ ] **Step 1: Write failing tests for X charset validation**

Create `SwiftValidationSpec.groovy` with initial test:

```groovy
import spock.lang.*
import org.moqui.Moqui
import org.moqui.context.ExecutionContext

// ABOUTME: SwiftValidationSpec tests Layer 1 SWIFT field validation.
// ABOUTME: Covers character sets, slash rules, BIC, line format, mutual exclusion, and conditional rules.

class SwiftValidationSpec extends Specification {
    protected ExecutionContext ec

    def setupSpec() {
        ExecutionContext ec = Moqui.getExecutionContext()
        ec.artifactExecution.disableAuthz()
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrument", 5000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit", 5000000, 1000)
    }

    def setup() {
        ec = Moqui.getExecutionContext()
        ec.user.loginUser("trade.admin", "trade123")
        ec.artifactExecution.enableAuthz()
        ec.message.clearAll()
    }

    def cleanup() {
        ec.user.popUser()
        ec.message.clearAll()
    }

    def cleanupSpec() {
        ExecutionContext ec = Moqui.getExecutionContext()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", org.moqui.entity.EntityCondition.GREATER_THAN_EQUAL_TO, "5000000").deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", org.moqui.entity.EntityCondition.GREATER_THAN_EQUAL_TO, "5000000").deleteAll()
        ec.user.logoutUser()
        ec.artifactExecution.enableAuthz()
        ec.destroy()
    }

    // BDD-SWV-XCS-01: X Character Set — Invalid Characters Blocked
    def "XCS-01: X charset - invalid characters blocked"() {
        given: "An LC with invalid X charset characters"
        def ref = "TF-XCS-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD",
                         goodsDescription: "STEEL PIPES @100MM & FITTINGS",
                         portOfLoading: "HO CHI MINH CITY #1 PORT"]).call()

        when: "validate#SwiftFields is called"
        def result = ec.service.sync().name("trade.ImportLcValidationServices.validate#SwiftFields")
            .parameters([entityType: "ImportLetterOfCredit", entityId: res.instrumentId]).call()

        then: "Errors are returned for invalid characters"
        result.errors != null
        result.errors.size() >= 2
        result.errors.any { it.fieldName == "goodsDescription" && it.message.contains("@") }
        result.errors.any { it.fieldName == "portOfLoading" && it.message.contains("#") }
    }

    // BDD-SWV-XCS-02: X Character Set — Valid Characters Accepted
    def "XCS-02: X charset - valid characters accepted"() {
        given: "An LC with valid X charset characters"
        def ref = "TF-XCS-V-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD",
                         goodsDescription: "STEEL PIPES 100MM, GRADE A (STANDARD)",
                         portOfLoading: "HO CHI MINH CITY"]).call()

        when: "validate#SwiftFields is called"
        def result = ec.service.sync().name("trade.ImportLcValidationServices.validate#SwiftFields")
            .parameters([entityType: "ImportLetterOfCredit", entityId: res.instrumentId]).call()

        then: "No charset errors for these fields"
        def charsetErrors = result.errors?.findAll { it.fieldName in ["goodsDescription", "portOfLoading"] && it.message.contains("charset") }
        charsetErrors == null || charsetErrors.size() == 0
    }

    // BDD-SWV-REF-01: Slash Rule Violations Blocked
    def "REF-01: Reference slash rules - violations blocked"() {
        given: "A TradeInstrument with slash rule violations"
        def ref = "/TF-REF-" + System.currentTimeMillis()  // starts with /
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD"]).call()

        when: "validate#SwiftFields is called on the TradeInstrument"
        def result = ec.service.sync().name("trade.ImportLcValidationServices.validate#SwiftFields")
            .parameters([entityType: "TradeInstrument", entityId: res.instrumentId]).call()

        then: "Error for slash rule violation"
        result.errors.any { it.fieldName == "transactionRef" && it.message.contains("/") }
    }

    // BDD-SWV-BIC-01: Invalid BIC Length Blocked
    def "BIC-01: BIC validation - invalid length blocked"() {
        given: "A TradeInstrument with invalid BIC"
        def ref = "TF-BIC-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD"]).call()
        // Set invalid BIC directly on entity
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", res.instrumentId)
            .one().set("reimbursingBankBic", "JPMC").update()

        when: "validate#SwiftFields is called"
        def result = ec.service.sync().name("trade.ImportLcValidationServices.validate#SwiftFields")
            .parameters([entityType: "TradeInstrument", entityId: res.instrumentId]).call()

        then: "Error for BIC length"
        result.errors.any { it.fieldName == "reimbursingBankBic" && it.message.contains("8 or 11") }
    }

    // BDD-SWV-MEX-01: Tolerance and Max Credit Amount mutual exclusion
    def "MEX-01: Mutual exclusion - tolerance vs max credit amount"() {
        given: "An LC with both tolerance and maxCreditAmountFlag set"
        def ref = "TF-MEX-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD",
                         tolerancePositive: 0.10, toleranceNegative: 0.05]).call()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", res.instrumentId)
            .one().set("maxCreditAmountFlag", "Y").update()

        when: "validate#SwiftFields is called"
        def result = ec.service.sync().name("trade.ImportLcValidationServices.validate#SwiftFields")
            .parameters([entityType: "ImportLetterOfCredit", entityId: res.instrumentId]).call()

        then: "Error for mutual exclusion"
        result.errors.any { it.fieldName == "maxCreditAmountFlag" && it.message.contains("mutually exclusive") }
    }

    // BDD-SWV-CND-01: Usance fields required when tenor != SIGHT
    def "CND-01: Conditional - usance fields required"() {
        given: "An LC with tenor USANCE but missing usanceDays/usanceBaseDate"
        def ref = "TF-CND-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD"]).call()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", res.instrumentId)
            .one().setAll([tenorTypeId: "USANCE", usanceDays: null, usanceBaseDate: null]).update()

        when: "validate#SwiftFields is called"
        def result = ec.service.sync().name("trade.ImportLcValidationServices.validate#SwiftFields")
            .parameters([entityType: "ImportLetterOfCredit", entityId: res.instrumentId]).call()

        then: "Errors for missing conditional fields"
        result.errors.any { it.fieldName == "usanceDays" && it.message.contains("Required") }
        result.errors.any { it.fieldName == "usanceBaseDate" && it.message.contains("Required") }
    }

    // BDD-SWV-SVC-01: Comprehensive multi-field error report
    def "SVC-01: Validation returns ALL errors, not fail-fast"() {
        given: "An LC with multiple violations"
        def ref = "TF-SVC-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD",
                         goodsDescription: "PIPES @100MM"]).call()
        def lcEntity = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", res.instrumentId).one()
        lcEntity.setAll([maxCreditAmountFlag: "Y", tolerancePositive: 0.10, tenorTypeId: "USANCE", usanceDays: null]).update()

        when: "validate#SwiftFields is called"
        def result = ec.service.sync().name("trade.ImportLcValidationServices.validate#SwiftFields")
            .parameters([entityType: "ImportLetterOfCredit", entityId: res.instrumentId]).call()

        then: "At least 3 distinct field-level errors returned"
        result.errors.size() >= 3
    }

    // BDD-SWV-SVC-02: Clean data returns no errors
    def "SVC-02: Clean data returns no errors"() {
        given: "An LC with valid SWIFT-compliant data"
        def ref = "TF-CLEAN-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 500000.0, lcCurrencyUomId: "USD",
                         goodsDescription: "STEEL PIPES, GRADE A, AS PER PROFORMA INV 2026-001",
                         documentsRequired: "FULL SET OF CLEAN ON BOARD BILL OF LADING",
                         portOfLoading: "HO CHI MINH CITY",
                         portOfDischarge: "TOKYO, JAPAN",
                         expiryPlace: "VIETNAM"]).call()

        when: "validate#SwiftFields is called"
        def result = ec.service.sync().name("trade.ImportLcValidationServices.validate#SwiftFields")
            .parameters([entityType: "ImportLetterOfCredit", entityId: res.instrumentId]).call()

        then: "No errors"
        result.errors == null || result.errors.size() == 0
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd /Users/me/myprojects/moqui-trade && ./gradlew :runtime:component:TradeFinance:test --tests "SwiftValidationSpec" 2>&1 | tail -30
```

Expected: FAIL — `ImportLcValidationServices.validate#SwiftFields` service does not exist yet.

- [ ] **Step 3: Create the validate#SwiftFields service**

Create `runtime/component/TradeFinance/service/trade/ImportLcValidationServices.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!-- ABOUTME: ImportLcValidationServices provides Layer 1 SWIFT field validation. -->
<!-- ABOUTME: Validates character sets, lengths, slash rules, BIC, mutual exclusions, and conditional requirements. -->
<services xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:noNamespaceSchemaLocation="http://moqui.org/xsd/service-definition-3.0.xsd">

    <service verb="validate" noun="SwiftFields">
        <description>Validates all SWIFT-bound fields on a given entity. Returns field-level errors.</description>
        <in-parameters>
            <parameter name="entityType" required="true"><description>Entity type: ImportLetterOfCredit, TradeInstrument, TradeParty, ImportLcAmendment, TradeDocumentPresentation, PresentationDiscrepancy, ImportLcSettlement</description></parameter>
            <parameter name="entityId" required="true"/>
        </in-parameters>
        <out-parameters>
            <parameter name="errors" type="List"><description>List of maps with fieldName, message, violationType</description></parameter>
        </out-parameters>
        <actions>
            <set field="errors" from="[]"/>
            <script><![CDATA[
                // X charset regex pattern
                def X_PATTERN = /^[A-Za-z0-9\/ \-\?\:\(\)\.\,\'\+\r\n]*$/
                // Z charset extends X with @ # = ! " % & * ; < > _
                def Z_PATTERN = /^[A-Za-z0-9\/ \-\?\:\(\)\.\,\'\+\@\#\=\!\"\%\&\*\;\<\>\_\r\n]*$/
                def BIC_PATTERN = /^[A-Za-z0-9]{8}$|^[A-Za-z0-9]{11}$/

                def addError = { fieldName, message, type ->
                    errors.add([fieldName: fieldName, message: message, violationType: type])
                }

                def validateXCharset = { fieldName, value ->
                    if (!value) return
                    value.toString().eachWithIndex { ch, idx ->
                        if (!(ch ==~ /[A-Za-z0-9\/ \-\?\:\(\)\.\,\'\+\r\n]/)) {
                            addError(fieldName, "Invalid X charset character '${ch}' at position ${idx + 1}", "CHARSET")
                        }
                    }
                }

                def validateZCharset = { fieldName, value ->
                    if (!value) return
                    value.toString().eachWithIndex { ch, idx ->
                        if (!(ch ==~ /[A-Za-z0-9\/ \-\?\:\(\)\.\,\'\+\@\#\=\!\"\%\&\*\;\<\>\_\r\n]/)) {
                            addError(fieldName, "Invalid Z charset character '${ch}' at position ${idx + 1}", "CHARSET")
                        }
                    }
                }

                def validateReference = { fieldName, value ->
                    if (!value) return
                    def v = value.toString()
                    if (v.startsWith("/")) addError(fieldName, "Reference field must not start with '/'", "SLASH_RULE")
                    if (v.endsWith("/")) addError(fieldName, "Reference field must not end with '/'", "SLASH_RULE")
                    if (v.contains("//")) addError(fieldName, "Reference field must not contain '//'", "SLASH_RULE")
                    if (v.length() > 16) addError(fieldName, "Reference field exceeds 16-character maximum", "LENGTH")
                }

                def validateBic = { fieldName, value ->
                    if (!value) return
                    if (!(value.toString() ==~ BIC_PATTERN)) {
                        addError(fieldName, "SWIFT BIC must be exactly 8 or 11 alphanumeric characters", "BIC")
                    }
                }

                def validateLineFormat = { fieldName, value, maxLines, maxCharsPerLine ->
                    if (!value) return
                    def lines = value.toString().split(/\r?\n/)
                    if (lines.length > maxLines) {
                        addError(fieldName, "Exceeds SWIFT ${maxLines}x${maxCharsPerLine} format: maximum ${maxLines} lines of ${maxCharsPerLine} characters", "LINE_FORMAT")
                    }
                    lines.eachWithIndex { line, idx ->
                        if (line.length() > maxCharsPerLine) {
                            addError(fieldName, "Line ${idx + 1} exceeds ${maxCharsPerLine}-character maximum (${line.length()} chars)", "LINE_FORMAT")
                        }
                    }
                }

                // Dispatch by entity type
                if (entityType == "ImportLetterOfCredit") {
                    def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", entityId).one()
                    if (!lc) { addError("entityId", "ImportLetterOfCredit not found", "NOT_FOUND"); return }

                    // X charset fields
                    validateXCharset("goodsDescription", lc.goodsDescription)
                    validateXCharset("documentsRequired", lc.documentsRequired)
                    validateXCharset("additionalConditions", lc.additionalConditions)
                    validateXCharset("portOfLoading", lc.portOfLoading)
                    validateXCharset("portOfDischarge", lc.portOfDischarge)
                    validateXCharset("expiryPlace", lc.expiryPlace)
                    validateXCharset("receiptPlace", lc.receiptPlace)
                    validateXCharset("finalDeliveryPlace", lc.finalDeliveryPlace)
                    validateXCharset("shipmentPeriodText", lc.shipmentPeriodText)
                    validateXCharset("additionalAmountsText", lc.additionalAmountsText)
                    validateXCharset("mixedPaymentDetails", lc.mixedPaymentDetails)
                    validateXCharset("deferredPaymentDetails", lc.deferredPaymentDetails)
                    validateXCharset("usanceBaseDate", lc.usanceBaseDate)
                    validateXCharset("bankToBankInstructions", lc.bankToBankInstructions)
                    validateXCharset("chargeAllocationText", lc.chargeAllocationText)

                    // Length checks
                    if (lc.portOfLoading && lc.portOfLoading.length() > 65)
                        addError("portOfLoading", "Exceeds 65-character maximum", "LENGTH")
                    if (lc.portOfDischarge && lc.portOfDischarge.length() > 65)
                        addError("portOfDischarge", "Exceeds 65-character maximum", "LENGTH")
                    if (lc.expiryPlace && lc.expiryPlace.length() > 29)
                        addError("expiryPlace", "Exceeds 29-character maximum", "LENGTH")
                    if (lc.receiptPlace && lc.receiptPlace.length() > 65)
                        addError("receiptPlace", "Exceeds 65-character maximum", "LENGTH")
                    if (lc.finalDeliveryPlace && lc.finalDeliveryPlace.length() > 65)
                        addError("finalDeliveryPlace", "Exceeds 65-character maximum", "LENGTH")
                    if (lc.shipmentPeriodText && lc.shipmentPeriodText.length() > 65)
                        addError("shipmentPeriodText", "Exceeds 65-character maximum", "LENGTH")
                    if (lc.usanceBaseDate && lc.usanceBaseDate.length() > 35)
                        addError("usanceBaseDate", "Exceeds 35-character maximum", "LENGTH")

                    // Line format checks
                    validateLineFormat("goodsDescription", lc.goodsDescription, 100, 65)
                    validateLineFormat("documentsRequired", lc.documentsRequired, 100, 65)
                    validateLineFormat("additionalConditions", lc.additionalConditions, 100, 65)
                    validateLineFormat("additionalAmountsText", lc.additionalAmountsText, 4, 35)
                    validateLineFormat("mixedPaymentDetails", lc.mixedPaymentDetails, 4, 35)
                    validateLineFormat("deferredPaymentDetails", lc.deferredPaymentDetails, 4, 35)
                    validateLineFormat("bankToBankInstructions", lc.bankToBankInstructions, 12, 65)
                    validateLineFormat("chargeAllocationText", lc.chargeAllocationText, 6, 35)

                    // Mutual exclusions (FR-SWV-07)
                    if (lc.maxCreditAmountFlag == "Y" && (lc.tolerancePositive || lc.toleranceNegative))
                        addError("maxCreditAmountFlag", "Tag 39B (Max Credit Amount) is mutually exclusive with Tag 39A (Tolerance). Clear tolerance fields or set maxCreditAmountFlag to N", "MUTUAL_EXCLUSION")
                    if (lc.latestShipmentDate && lc.shipmentPeriodText)
                        addError("shipmentPeriodText", "Tag 44D (Shipment Period) is mutually exclusive with Tag 44C (Latest Shipment Date). Use one or the other", "MUTUAL_EXCLUSION")

                    // Conditional requirements (FR-SWV-08)
                    if (lc.tenorTypeId && lc.tenorTypeId != "SIGHT") {
                        if (!lc.usanceDays) addError("usanceDays", "Required when tenor is not SIGHT (Tag 42C source)", "CONDITIONAL")
                        if (!lc.usanceBaseDate) addError("usanceBaseDate", "Required when tenor is not SIGHT (Tag 42C source)", "CONDITIONAL")
                    }
                    if (lc.tenorTypeId == "MIXED" && !lc.mixedPaymentDetails)
                        addError("mixedPaymentDetails", "Required when tenor is MIXED (Tag 42M source)", "CONDITIONAL")
                    if (lc.tenorTypeId in ["DEF_PAYMENT", "NEGOTIATION"] && !lc.deferredPaymentDetails)
                        addError("deferredPaymentDetails", "Required when tenor is DEF_PAYMENT or NEGOTIATION (Tag 42P source)", "CONDITIONAL")

                } else if (entityType == "TradeInstrument") {
                    def inst = ec.entity.find("trade.TradeInstrument").condition("instrumentId", entityId).one()
                    if (!inst) { addError("entityId", "TradeInstrument not found", "NOT_FOUND"); return }

                    validateReference("transactionRef", inst.transactionRef)
                    validateReference("preAdviceRef", inst.preAdviceRef)
                    validateBic("reimbursingBankBic", inst.reimbursingBankBic)
                    validateBic("adviseThroughBankBic", inst.adviseThroughBankBic)
                    validateXCharset("beneficiaryName", inst.beneficiaryName)
                    validateXCharset("senderToReceiverInfo", inst.senderToReceiverInfo)
                    validateXCharset("reimbursingBankName", inst.reimbursingBankName)
                    validateXCharset("adviseThroughBankName", inst.adviseThroughBankName)
                    validateLineFormat("beneficiaryName", inst.beneficiaryName, 4, 35)
                    validateLineFormat("senderToReceiverInfo", inst.senderToReceiverInfo, 6, 35)
                    validateLineFormat("reimbursingBankName", inst.reimbursingBankName, 4, 35)
                    validateLineFormat("adviseThroughBankName", inst.adviseThroughBankName, 4, 35)

                    // Mutual exclusions
                    if (inst.reimbursingBankBic && inst.reimbursingBankName)
                        addError("reimbursingBankName", "Tag 53D (Name/Address) is mutually exclusive with Tag 53A (BIC). Use BIC format or Name/Address, not both", "MUTUAL_EXCLUSION")
                    if (inst.adviseThroughBankBic && inst.adviseThroughBankName)
                        addError("adviseThroughBankName", "Tag 57D (Name/Address) is mutually exclusive with Tag 57A (BIC). Use BIC format or Name/Address, not both", "MUTUAL_EXCLUSION")

                    // Amount validation
                    if (inst.amount != null) {
                        def formatted = String.format("%.2f", inst.amount).replace(".", ",")
                        if (formatted.length() > 15) addError("amount", "SWIFT amount exceeds 15-digit maximum when formatted", "AMOUNT")
                    }

                } else if (entityType == "ImportLcAmendment") {
                    def amd = ec.entity.find("trade.importlc.ImportLcAmendment").condition("amendmentId", entityId).one()
                    if (!amd) { addError("entityId", "ImportLcAmendment not found", "NOT_FOUND"); return }

                    validateZCharset("amendmentNarrative", amd.amendmentNarrative)
                    validateLineFormat("amendmentNarrative", amd.amendmentNarrative, 35, 50)

                } else if (entityType == "TradeDocumentPresentation") {
                    def pres = ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("presentationId", entityId).one()
                    if (!pres) { addError("entityId", "TradeDocumentPresentation not found", "NOT_FOUND"); return }

                    validateBic("presentingBankBic", pres.presentingBankBic)
                    validateReference("presentingBankRef", pres.presentingBankRef)

                    if (pres.applicantDecisionEnumId == "REFUSED" && !pres.documentDisposalEnumId)
                        addError("documentDisposalEnumId", "Required when applicant refuses documents (Tag 77B source for MT734)", "CONDITIONAL")

                } else if (entityType == "TradeParty") {
                    def party = ec.entity.find("trade.TradeParty").condition("partyId", entityId).one()
                    if (!party) { addError("entityId", "TradeParty not found", "NOT_FOUND"); return }

                    validateXCharset("partyName", party.partyName)
                    validateXCharset("registeredAddress", party.registeredAddress)
                    validateBic("swiftBic", party.swiftBic)
                    validateLineFormat("partyName", party.partyName, 4, 35)
                    validateLineFormat("registeredAddress", party.registeredAddress, 4, 35)
                }
            ]]></script>
        </actions>
    </service>
</services>
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd /Users/me/myprojects/moqui-trade && ./gradlew :runtime:component:TradeFinance:test --tests "SwiftValidationSpec" 2>&1 | tail -30
```

Expected: All tests PASS.

- [ ] **Step 5: Commit**

```bash
git add runtime/component/TradeFinance/service/trade/ImportLcValidationServices.xml runtime/component/TradeFinance/src/test/groovy/SwiftValidationSpec.groovy
git commit -m "feat: implement validate#SwiftFields with Layer 1 SWIFT validation"
```

---

## Verification Plan

### Automated Tests

Run the full SWIFT validation test suite:

```bash
cd /Users/me/myprojects/moqui-trade && ./gradlew :runtime:component:TradeFinance:test --tests "SwiftValidationSpec" 2>&1 | tail -50
```

Also run the existing test suite to confirm no regressions:

```bash
cd /Users/me/myprojects/moqui-trade && ./gradlew :runtime:component:TradeFinance:test 2>&1 | tail -50
```

### Manual Verification

After tests pass, review the HTML test report at:
`runtime/component/TradeFinance/build/reports/tests/test/index.html`

Confirm all SwiftValidationSpec tests show as PASSED alongside existing tests.
