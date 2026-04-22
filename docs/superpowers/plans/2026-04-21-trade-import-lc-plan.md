# Trade Import LC Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Establish the specialized Import Letter of Credit specific entities expanding identically upon the Common Limits checking explicitly against BDD constraints securely driving state validations natively mapped to exact MT700 parsing properties.

**Architecture:** Native Moqui backend `TradeFinance` module utilizing Moqui REST configurations cleanly separating SWIFT execution out of `ImportLcServices`.

**Tech Stack:** Moqui Framework, Groovy, Spock

---

#### Task 1: Moqui Import LC Extensibility Schema [DONE]

**BDD Scenarios:** BDD-IMP-FLOW-01: State Transition Default Mappings
**BRD Requirements:** REQ-IMP-DTL-01
**User-Facing:** NO

**Files:**
- Create: `component/TradeFinance/entity/ImportLcEntities.xml`
- Create: `component/TradeFinance/test/groovy/trade/finance/ImportLcEntitiesSpec.groovy`

- [x] **Step 1: Write the failing test**

```groovy
import spock.lang.Specification
import org.moqui.context.ExecutionContext

class ImportLcEntitiesSpec extends Specification {
    def "CREATE ImportLc validates relationships properly"() {
        given:
        ExecutionContext ec = ec
        ec.entity.makeValue("trade.finance.TradeInstrument").setAll([instrumentId: "LC-100", transactionRef: "LC-01", baseEquivalentAmount: 500.0]).create()
        when:
        def lc = ec.entity.makeValue("trade.finance.ImportLetterOfCredit")
        lc.setAll([instrumentId: "LC-100", tolerancePositive: 10.0, toleranceNegative: 10.0, businessStateId: "DRAFT"])
        lc.create()
        def found = ec.entity.find("trade.finance.ImportLetterOfCredit").condition("instrumentId", "LC-100").one()
        then:
        found.businessStateId == "DRAFT"
    }
}
```

- [x] **Step 2: Run test to verify it fails**

Run: `gradle clean test --tests *ImportLcEntitiesSpec*`
Expected: FAIL, Entity `trade.finance.ImportLetterOfCredit` missing

- [x] **Step 3: Write minimal implementation**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<entities xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="http://moqui.org/xsd/entity-definition-3.xsd">
    <entity entity-name="ImportLetterOfCredit" package="trade.finance">
        <field name="instrumentId" type="id" is-pk="true"/>
        <field name="businessStateId" type="text-short"/>
        <field name="beneficiaryPartyId" type="id"/>
        <field name="tolerancePositive" type="number-decimal"/>
        <field name="toleranceNegative" type="number-decimal"/>
        <field name="tenorTypeId" type="id"/>
        <field name="usanceDays" type="number-integer"/>
        <field name="chargeAllocationEnumId" type="id"/>
        <relationship type="one" related="trade.finance.TradeInstrument">
            <key-map field-name="instrumentId"/>
        </relationship>
    </entity>
</entities>
```

- [x] **Step 4: Run test to verify it passes**

Run: `gradle clean test --tests *ImportLcEntitiesSpec*`
Expected: PASS

- [x] **Step 5: Commit**

```bash
git add component/TradeFinance/entity/ImportLcEntities.xml component/TradeFinance/test/groovy/trade/finance/ImportLcEntitiesSpec.groovy
git commit -m "feat(import-lc): extend TradeInstrument securely with ImportLetterOfCredit logic"
```

### Task 2: Tolerance Validation Service Execution [DONE]

**BDD Scenarios:** BDD-IMP-VAL-01: Tolerance Limits Enforcement Matrix
**BRD Requirements:** REQ-IMP-04
**User-Facing:** NO

**Files:**
- Create: `component/TradeFinance/service/trade/finance/ImportLcValidationServices.xml`
- Create: `component/TradeFinance/test/groovy/trade/finance/ImportLcValidationServicesSpec.groovy`

- [x] **Step 1: Write the failing test**

```groovy
import spock.lang.Specification
import org.moqui.context.ExecutionContext

class ImportLcValidationServicesSpec extends Specification {
    def "BLOCK Claim amount exceeding Positive Tolerance strictly"() {
        given:
        ExecutionContext ec = ec
        def instrumentAmount = 10000.0
        def posTolerance = 10.0 // 10%
        def claimAmount = 11500.0 // Mathematically invalid claim
        
        when:
        ec.service.sync().name("trade.finance.ImportLcValidationServices.evaluateDrawing")
            .parameters([lcAmount: instrumentAmount, tolerancePercent: posTolerance, claimAmount: claimAmount]).call()
            
        then:
        thrown(IllegalArgumentException)
    }
}
```

- [x] **Step 2: Run test to verify it fails**

Run: `gradle clean test --tests *ImportLcValidationServicesSpec*`
Expected: FAIL, service evaluateDrawing not found

- [x] **Step 3: Write minimal implementation**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<services xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="http://moqui.org/xsd/service-definition-3.xsd">
    <service verb="evaluate" noun="Drawing">
        <in-parameters>
            <parameter name="lcAmount" type="BigDecimal" required="true"/>
            <parameter name="tolerancePercent" type="BigDecimal" required="true"/>
            <parameter name="claimAmount" type="BigDecimal" required="true"/>
        </in-parameters>
        <actions>
            <set field="maxValidDraw" from="lcAmount * (1 + (tolerancePercent/100))"/>
            <if condition="claimAmount > maxValidDraw">
                <script>throw new IllegalArgumentException("Claim exceeds Tolerance bounds")</script>
            </if>
            <return message="Drawing Verified"/>
        </actions>
    </service>
</services>
```

- [x] **Step 4: Run test to verify it passes**

Run: `gradle clean test --tests *ImportLcValidationServicesSpec*`
Expected: PASS

- [x] **Step 5: Commit**

```bash
git add component/TradeFinance/service/trade/finance/ImportLcValidationServices.xml component/TradeFinance/test/groovy/trade/finance/ImportLcValidationServicesSpec.groovy
git commit -m "feat(import-lc): validate drawing tolerance exactly natively enforcing bounds"
```
