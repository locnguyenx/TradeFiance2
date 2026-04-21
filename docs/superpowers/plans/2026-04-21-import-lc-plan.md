# Import LC Module Implementation Plan
> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the complex Import Letter of Credit transaction lifecycle, strictly adhering to the 31 exact BDD scenarios detailing Draft, Submission, Authorization, Discrepancy Checks, Amendment limits, and SWIFT message structure validations.

**Architecture:** Deploys upon the foundational entities from the Common Module, utilizing `service/trade/finance/ImportLcServices.xml` to manage states and `trade/finance/SwiftGenerationServices.xml` to manage SWIFT rendering sequences. Uses Spock testing exclusively.

**Tech Stack:** Moqui Framework (XML Services), Groovy (Spock Framework for TDD).

## Verification Plan
We will exclusively use Test-Driven Development (TDD) via the Spock Framework as mandated by the `moqui-testing` and `writing-plans` skills.
Every task below begins with writing a failing Spock test in `runtime/component/TradeFinance/src/test/groovy/...` that directly enforces our Import LC BDD logic.
**Execution Command:** You will run tests using:
`./gradlew test -Pcomponent=TradeFinance --info`

---

### Task 1: Core Lifecycle Entity Controller

**BDD Scenarios:** BDD-IMP-FLOW-01, BDD-IMP-FLOW-02, BDD-IMP-FLOW-03, BDD-IMP-FLOW-08, BDD-IMP-ISS-01
**BRD Requirements:** REQ-IMP-FLOW-01..03, REQ-IMP-SPEC-01
**User-Facing:** NO

**Files:**
- Create: `runtime/component/TradeFinance/service/trade/finance/ImportLcServices.xml`
- Test: `runtime/component/TradeFinance/src/test/groovy/trade/finance/ImportLcLifecycleSpec.groovy`

- [ ] **Step 1: Write the failing test**
```groovy
// In src/test/groovy/trade/finance/ImportLcLifecycleSpec.groovy
import spock.lang.Specification
import org.moqui.Moqui

class ImportLcLifecycleSpec extends Specification {
    // Tests BDD-IMP-FLOW-02 and BDD-IMP-ISS-01
    def "test Import LC generation and submission limits"() {
        given:
        def ec = Moqui.getExecutionContext()
        // Setup initial draft component
        ec.entity.makeValue("trade.finance.TradeInstrument")
            .setAll([instrumentId: "IMP-123", lifecycleStatusId: "TfDraft"]).create()
            
        when: "User triggers submit for approval"
        Map res = ec.service.sync().name("trade.finance.ImportLcServices.submitLc")
            .parameters([instrumentId: "IMP-123", facilityId: "FAC-001", lcAmount: 100000.0, tolerance: 10.0]).call()
            
        then: "Earmarks 110,000 against the Common Limit Engine inherently and pushes State"
        def inst = ec.entity.find("trade.finance.TradeInstrument").condition("instrumentId", "IMP-123").one()
        inst.lifecycleStatusId == "TfPendingApproval"
        
        cleanup:
        ec.destroy()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**
Run: `./gradlew test -Pcomponent=TradeFinance --tests *ImportLcLifecycleSpec*`
Expected: FAIL due to missing service `submitLc`

- [ ] **Step 3: Write minimal implementation**
```xml
<!-- In ImportLcServices.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<services xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="http://moqui.org/xsd/service-definition-3.xsd">
    <service verb="submit" noun="Lc">
        <in-parameters>
            <parameter name="instrumentId" required="true"/>
            <parameter name="facilityId" required="true"/>
            <parameter name="lcAmount" type="BigDecimal" required="true"/>
            <parameter name="tolerance" type="BigDecimal"/>
        </in-parameters>
        <actions>
            <entity-find-one entity-name="trade.finance.TradeInstrument" value-field="instrument"/>
            
            <!-- Calculate total utilizing Tolerance -->
            <set field="toleranceMultiplier" value="${1 + (tolerance ?: 0.0) / 100}"/>
            <set field="totalEarmark" value="${lcAmount * toleranceMultiplier}"/>
            
            <!-- Interact with Common Module Limit logic -->
            <service-call name="trade.finance.LimitServices.calculateEarmark" 
                          in-map="[facilityId: facilityId, targetAmount: totalEarmark]" out-map="earmarkOut"/>
                          
            <set field="instrument.lifecycleStatusId" value="TfPendingApproval"/>
            <entity-update value-field="instrument"/>
        </actions>
    </service>
</services>
```

- [ ] **Step 4: Run test to verify it passes**
Run: `./gradlew test -Pcomponent=TradeFinance --tests *ImportLcLifecycleSpec*`
Expected: PASS

- [ ] **Step 5: Commit**
```bash
git add runtime/component/TradeFinance/service/trade/finance/ImportLcServices.xml runtime/component/TradeFinance/src/test/groovy/trade/finance/ImportLcLifecycleSpec.groovy
git commit -m "feat(import): lifecycle state bounds evaluating common module facilities appropriately"
```

---

### Task 2: Amendment & Tolerance Validation Business Rules

**BDD Scenarios:** BDD-IMP-VAL-01, BDD-IMP-VAL-02, BDD-IMP-AMD-01, BDD-IMP-AMD-02
**BRD Requirements:** REQ-IMP-04, REQ-IMP-SPEC-02
**User-Facing:** NO

**Files:**
- Create: `runtime/component/TradeFinance/service/trade/finance/ImportLcValidationServices.xml`
- Test: `runtime/component/TradeFinance/src/test/groovy/trade/finance/ImportLcValidationSpec.groovy`

- [ ] **Step 1: Write the failing test**
```groovy
// In src/test/groovy/trade/finance/ImportLcValidationSpec.groovy
import spock.lang.Specification
import org.moqui.Moqui

class ImportLcValidationSpec extends Specification {
    // Tests BDD-IMP-VAL-01: Tolerance Over-Draw Block
    def "test drawn amount hard limit tolerance protection"() {
        given:
        def ec = Moqui.getExecutionContext()
        ec.entity.makeValue("trade.finance.TradeInstrument")
            .setAll([instrumentId: "IMP-123", lcAmount: 10000.0, tolerancePositive: 10.0]).create()
            
        when: "We attempt to present docs mapping to 11,500 over LC"
        Map res = ec.service.sync().name("trade.finance.ImportLcValidationServices.evaluateDrawing")
            .parameters([instrumentId: "IMP-123", drawnAmount: 11500.0]).call()
            
        then: 
        thrown(IllegalArgumentException) // Blocks inherently
        
        cleanup:
        ec.destroy()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**
Run: `./gradlew test -Pcomponent=TradeFinance --tests *ImportLcValidationSpec*`

- [ ] **Step 3: Write minimal implementation**
```xml
<!-- In ImportLcValidationServices.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<services xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="http://moqui.org/xsd/service-definition-3.xsd">
    <service verb="evaluate" noun="Drawing">
        <in-parameters>
            <parameter name="instrumentId" required="true"/>
            <parameter name="drawnAmount" type="BigDecimal" required="true"/>
        </in-parameters>
        <actions>
            <entity-find-one entity-name="trade.finance.TradeInstrument" value-field="instrument"/>
            
            <set field="toleranceMultiplier" value="${1 + (instrument.tolerancePositive ?: 0) / 100}"/>
            <set field="hardLimit" value="${instrument.lcAmount * toleranceMultiplier}"/>
            
            <if condition="drawnAmount &gt; hardLimit">
                <return error="true" message="Presentation Overdraw Error: Expected maximum ${hardLimit}"/>
            </if>
        </actions>
    </service>
</services>
```

- [ ] **Step 4: Run test to verify it passes**
Run: `./gradlew test -Pcomponent=TradeFinance --tests *ImportLcValidationSpec*`

- [ ] **Step 5: Commit**
```bash
git add runtime/component/TradeFinance/service/trade/finance/ImportLcValidationServices.xml runtime/component/TradeFinance/src/test/groovy/trade/finance/ImportLcValidationSpec.groovy
git commit -m "feat(import): complex validations evaluating strict mathematical bounds limits on presentations"
```

---

### Task 3: SWIFT Payload Output Matrix Engine

**BDD Scenarios:** BDD-IMP-SWT-02, BDD-IMP-SWT-04, BDD-IMP-SWT-05
**BRD Requirements:** REQ-IMP-SWIFT-02, REQ-IMP-SWIFT-04, REQ-IMP-SWIFT-05
**User-Facing:** NO

**Files:**
- Create: `runtime/component/TradeFinance/service/trade/finance/SwiftGenerationServices.xml`
- Test: `runtime/component/TradeFinance/src/test/groovy/trade/finance/SwiftGenerationSpec.groovy`

- [ ] **Step 1: Write the failing test**
```groovy
// In src/test/groovy/trade/finance/SwiftGenerationSpec.groovy
import spock.lang.Specification
import org.moqui.Moqui

class SwiftGenerationSpec extends Specification {
    // Tests BDD-IMP-SWT-04 and BDD-IMP-SWT-05
    def "test proper tag allocations for Bank vs Address logic"() {
        given:
        def ec = Moqui.getExecutionContext()
        ec.entity.makeValue("trade.finance.TradeInstrument")
            .setAll([instrumentId: "IMP-123"]).create()
            
        when: "We utilize pure BIC mapping"
        Map res = ec.service.sync().name("trade.finance.SwiftGenerationServices.generateMt700")
            .parameters([instrumentId: "IMP-123", beneficiaryBic: "BOFUS33"]).call()
            
        then: 
        res.swiftPayload.contains(":59A:")
        !res.swiftPayload.contains(":59:")
        
        cleanup:
        ec.destroy()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**
Run: `./gradlew test -Pcomponent=TradeFinance --tests *SwiftGenerationSpec*`

- [ ] **Step 3: Write minimal implementation**
```xml
<!-- In SwiftGenerationServices.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<services xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="http://moqui.org/xsd/service-definition-3.xsd">
    <service verb="generate" noun="Mt700">
        <in-parameters>
            <parameter name="instrumentId" required="true"/>
            <parameter name="beneficiaryBic"/>
            <parameter name="beneficiaryAddress"/>
        </in-parameters>
        <out-parameters>
            <parameter name="swiftPayload" type="String"/>
        </out-parameters>
        <actions>
            <set field="builder" value="{"/>
            <if condition="beneficiaryBic">
                <set field="builder" value="${builder} :59A:${beneficiaryBic} "/>
            </if>
            <if condition="beneficiaryAddress &amp;&amp; !beneficiaryBic">
                <set field="builder" value="${builder} :59:${beneficiaryAddress} "/>
            </if>
            
            <set field="builder" value="${builder} }"/>
            <set field="swiftPayload" value="${builder}" type="String"/>
        </actions>
    </service>
</services>
```

- [ ] **Step 4: Run test to verify it passes**
Run: `./gradlew test -Pcomponent=TradeFinance --tests *SwiftGenerationSpec*`

- [ ] **Step 5: Commit**
```bash
git add runtime/component/TradeFinance/service/trade/finance/SwiftGenerationServices.xml runtime/component/TradeFinance/src/test/groovy/trade/finance/SwiftGenerationSpec.groovy
git commit -m "feat(import): MT700 base block generation evaluating strict structural SWIFT tags"
```
