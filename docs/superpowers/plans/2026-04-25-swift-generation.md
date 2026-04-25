# SWIFT Message Generation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace skeletal SWIFT generation stubs with a robust Groovy message builder and full tag-mapping services for all 10 MT message types — MT700, MT701, MT707, MT750, MT734, MT752, MT732, MT799, MT202, MT103.

**Architecture:** A reusable `SwiftMessageBuilder` Groovy class handles SWIFT block construction, tag formatting, and line wrapping. Each `generate#MtXXX` service reads entity data, runs Layer 2 validation via `validate#SwiftFields` (Spec A), assembles the message via the builder, and persists it as a `SwiftMessage` with DRAFT/ACTIVE lifecycle management. This plan depends on Spec A (entity fields + `validate#SwiftFields` service) being complete.

**Tech Stack:** Moqui XML services, Groovy utility classes, Spock framework for testing.

**Prerequisite:** Spec A — `docs/superpowers/plans/2026-04-25-swift-validation.md` must be fully implemented.

---

## File Structure

| Action | File | Responsibility |
|:---|:---|:---|
| Create | `classes/trade/swift/SwiftMessageBuilder.groovy` | Reusable SWIFT block construction, tag formatting, line wrapping |
| Modify | `service/trade/SwiftGenerationServices.xml` | Replace skeletal stubs with full implementations for all 10 MT services |
| Create | `src/test/groovy/SwiftGenerationSpec.groovy` | Spock tests for generation and message lifecycle |

All paths are relative to `runtime/component/TradeFinance/`.

---

### Task 1: Create SwiftMessageBuilder Utility Class

**BDD Scenarios:** BDD-SWG-700-04 (header blocks), BDD-SWG-700-01 (tag formatting)
**BRD Requirements:** FR-SWG-01 (Builder), FR-SWG-02 (Tag Formatting)

**User-Facing:** NO

**Files:**
- Create: `runtime/component/TradeFinance/classes/trade/swift/SwiftMessageBuilder.groovy`
- Create: `runtime/component/TradeFinance/src/test/groovy/SwiftGenerationSpec.groovy`

- [ ] **Step 1: Write failing tests for builder**

Create `SwiftGenerationSpec.groovy`:

```groovy
import spock.lang.*
import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityCondition

// ABOUTME: SwiftGenerationSpec tests MT message generation and message lifecycle.
// ABOUTME: Covers builder formatting, tag assembly, DRAFT/ACTIVE lifecycle, and Layer 2 validation.

class SwiftGenerationSpec extends Specification {
    protected ExecutionContext ec

    def getService() { ec.service }
    def getEntity() { ec.entity }

    def setupSpec() {
        ExecutionContext ec = Moqui.getExecutionContext()
        ec.artifactExecution.disableAuthz()
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrument", 6000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit", 6000000, 1000)
        // Cleanup leaked data
        ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "6000000").deleteAll()
        ec.entity.find("trade.importlc.ImportLcAmendment").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "6000000").deleteAll()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "6000000").deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "6000000").deleteAll()
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
        ec.user.logoutUser()
        ec.artifactExecution.enableAuthz()
        ec.destroy()
    }

    // === BDD-SWG-700-04: SWIFT Header Block Construction ===
    def "700-04: SWIFT header block construction"() {
        given: "A builder configured with LC data"
        def builder = new trade.swift.SwiftMessageBuilder(
            senderBic: "VIETBANK1XXX", receiverBic: "BNPAFRPPXXX", messageType: "700"
        )
        builder.addTag("27", "1/1")
        builder.addTag("20", "TF-IMP-26-0001")

        when: "build() is called"
        def msg = builder.build()

        then: "Message contains proper SWIFT blocks"
        msg.contains("{1:F01VIETBANK1XXX")
        msg.contains("{2:I700BNPAFRPPXXX")
        msg.contains(":27:1/1")
        msg.contains(":20:TF-IMP-26-0001")
        msg.contains("-}")
    }

    // === Formatting helpers ===
    def "Builder: formatDate produces YYMMDD"() {
        expect:
        trade.swift.SwiftMessageBuilder.formatDate(java.sql.Date.valueOf("2026-06-15")) == "260615"
    }

    def "Builder: formatAmount produces comma-decimal"() {
        expect:
        trade.swift.SwiftMessageBuilder.formatAmount("USD", 500000.0) == "USD500000,00"
    }

    def "Builder: wrapLines splits at specified width"() {
        given:
        def longText = "A" * 200 // 200 chars

        when:
        def lines = trade.swift.SwiftMessageBuilder.wrapLines(longText, 65)

        then:
        lines.size() == 4 // 65 + 65 + 65 + 5
        lines.every { it.length() <= 65 }
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd /Users/me/myprojects/moqui-trade && ./gradlew :runtime:component:TradeFinance:test --tests "SwiftGenerationSpec" 2>&1 | tail -20
```

Expected: FAIL — `trade.swift.SwiftMessageBuilder` class does not exist.

- [ ] **Step 3: Implement SwiftMessageBuilder**

Create `runtime/component/TradeFinance/classes/trade/swift/SwiftMessageBuilder.groovy`:

```groovy
package trade.swift

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat

// ABOUTME: SwiftMessageBuilder assembles SWIFT MT message blocks from structured tag data.
// ABOUTME: Handles header construction, tag formatting (dates, amounts, BICs), and line wrapping.

class SwiftMessageBuilder {
    String senderBic
    String receiverBic
    String messageType
    private List<Map> tags = []

    SwiftMessageBuilder(Map params) {
        this.senderBic = params.senderBic ?: ""
        this.receiverBic = params.receiverBic ?: ""
        this.messageType = params.messageType ?: ""
    }

    /** Add a tag to the text block. Value may be multi-line. */
    void addTag(String tagId, String value) {
        if (value != null && value.trim()) {
            tags.add([id: tagId, value: value])
        }
    }

    /** Add tag only if value is non-null and non-empty. */
    void addOptionalTag(String tagId, String value) {
        if (value != null && value.toString().trim()) {
            tags.add([id: tagId, value: value.toString()])
        }
    }

    /** Build the complete SWIFT message. */
    String build() {
        def sb = new StringBuilder()

        // Block 1: Basic Header
        def paddedSender = senderBic.padRight(12, 'X')
        sb.append("{1:F01${paddedSender}0000000000}")

        // Block 2: Application Header
        def paddedReceiver = receiverBic.padRight(12, 'X')
        sb.append("{2:I${messageType}${paddedReceiver}N}")

        // Block 4: Text Block
        sb.append("{4:\r\n")
        tags.each { tag ->
            def lines = tag.value.split(/\r?\n/)
            sb.append(":${tag.id}:${lines[0]}\r\n")
            for (int i = 1; i < lines.length; i++) {
                sb.append("${lines[i]}\r\n")
            }
        }
        sb.append("-}")

        return sb.toString()
    }

    /** Format a date to SWIFT YYMMDD format. */
    static String formatDate(java.util.Date date) {
        if (!date) return ""
        new SimpleDateFormat("yyMMdd").format(date)
    }

    /** Format currency + amount in SWIFT comma-decimal format. */
    static String formatAmount(String currency, BigDecimal amount) {
        if (!amount) return ""
        def symbols = new DecimalFormatSymbols(Locale.US)
        symbols.setDecimalSeparator(',' as char)
        def df = new DecimalFormat("#0.00", symbols)
        df.setGroupingUsed(false)
        "${currency ?: ''}${df.format(amount)}"
    }

    /** Format party block: name + address, each line max charsPerLine. */
    static String formatParty(String name, String address) {
        def parts = []
        if (name) parts.addAll(wrapLines(name, 35))
        if (address) parts.addAll(wrapLines(address, 35))
        // Max 4 lines per SWIFT Tag 50/59 rules
        parts.take(4).join("\r\n")
    }

    /** Wrap text into lines of specified max width. */
    static List<String> wrapLines(String text, int maxWidth) {
        if (!text) return []
        def result = []
        def remaining = text
        while (remaining.length() > maxWidth) {
            result.add(remaining.substring(0, maxWidth))
            remaining = remaining.substring(maxWidth)
        }
        if (remaining) result.add(remaining)
        return result
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd /Users/me/myprojects/moqui-trade && ./gradlew :runtime:component:TradeFinance:test --tests "SwiftGenerationSpec" 2>&1 | tail -20
```

Expected: All builder tests PASS.

- [ ] **Step 5: Commit**

```bash
git add runtime/component/TradeFinance/classes/trade/swift/SwiftMessageBuilder.groovy runtime/component/TradeFinance/src/test/groovy/SwiftGenerationSpec.groovy
git commit -m "feat: create SwiftMessageBuilder utility with header construction and tag formatting"
```

---

### Task 2: Implement generate#Mt700 — Full Tag Assembly

**BDD Scenarios:** BDD-SWG-700-01, BDD-SWG-700-02, BDD-SWG-700-03
**BRD Requirements:** FR-SWG-03 (Layer 2), FR-SWG-04 (Persistence), FR-SWG-05 (MT700), FR-SWG-15 (Lifecycle)

**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/service/trade/SwiftGenerationServices.xml`
- Modify: `runtime/component/TradeFinance/src/test/groovy/SwiftGenerationSpec.groovy`

- [ ] **Step 1: Add failing tests for MT700 generation**

Add to `SwiftGenerationSpec.groovy`:

```groovy
    // === Helper: create a full LC for generation tests ===
    def createFullLc() {
        def ref = "TF-GEN-" + System.currentTimeMillis()
        def res = service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 500000.0, lcCurrencyUomId: "USD",
                         beneficiaryPartyId: "BEN-GEN-01",
                         goodsDescription: "STEEL PIPES, GRADE A, 100MM DIAMETER",
                         documentsRequired: "FULL SET OF CLEAN ON BOARD BILL OF LADING",
                         portOfLoading: "HO CHI MINH CITY",
                         expiryPlace: "VIETNAM"]).call()
        return res.instrumentId
    }

    // === BDD-SWG-700-01: Full tag assembly ===
    def "700-01: MT700 full tag assembly from entity data"() {
        given: "A fully populated LC"
        def instrumentId = createFullLc()

        when: "generate#Mt700 is called"
        def result = service.sync().name("trade.SwiftGenerationServices.generate#Mt700")
            .parameters([instrumentId: instrumentId]).call()

        then: "SwiftMessage is created with properly formatted tags"
        result.swiftMessageId != null
        def msg = entity.find("trade.importlc.SwiftMessage")
            .condition("swiftMessageId", result.swiftMessageId).one()
        msg.messageType == "MT700"
        msg.messageContent.contains(":27:")
        msg.messageContent.contains(":20:")
        msg.messageContent.contains(":32B:USD500000,00")
        msg.messageContent.contains(":45A:")
        msg.messageContent.contains("STEEL PIPES")
    }

    // === BDD-SWG-700-02: Optional tags present when data exists ===
    def "700-02: MT700 optional tags populated when data exists"() {
        given: "An LC with optional fields"
        def instrumentId = createFullLc()
        entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId)
            .one().setAll([tolerancePositive: 0.10, toleranceNegative: 0.05,
                           portOfDischarge: "TOKYO, JAPAN",
                           additionalConditions: "ALL DOCUMENTS MUST BE IN ENGLISH",
                           bankToBankInstructions: "UPON CLEAN PRESENTATION CLAIM REIMBURSEMENT"]).update()

        when: "generate#Mt700 is called"
        def result = service.sync().name("trade.SwiftGenerationServices.generate#Mt700")
            .parameters([instrumentId: instrumentId]).call()

        then: "Optional tags are included"
        def msg = entity.find("trade.importlc.SwiftMessage")
            .condition("swiftMessageId", result.swiftMessageId).one()
        msg.messageContent.contains(":39A:")
        msg.messageContent.contains(":44F:TOKYO, JAPAN")
        msg.messageContent.contains(":47A:")
        msg.messageContent.contains(":78:")
    }

    // === BDD-SWG-700-03: BIC toggle 59A vs 59 ===
    def "700-03: MT700 BIC toggle - 59A when BIC available"() {
        given: "An LC with beneficiary BIC"
        def instrumentId = createFullLc()
        // Set beneficiary BIC on party
        if (!entity.find("trade.TradeParty").condition("partyId", "BEN-GEN-01").one()) {
            entity.makeValue("trade.TradeParty").setAll([
                partyId: "BEN-GEN-01", partyName: "GLOBAL STEEL LTD",
                registeredAddress: "456 HARBOUR RD, TOKYO",
                swiftBic: "BANKJPJT"]).create()
        } else {
            entity.find("trade.TradeParty").condition("partyId", "BEN-GEN-01")
                .one().set("swiftBic", "BANKJPJT").update()
        }

        when: "generate#Mt700 is called"
        def result = service.sync().name("trade.SwiftGenerationServices.generate#Mt700")
            .parameters([instrumentId: instrumentId]).call()

        then: "Tag 59A is used (BIC format)"
        def msg = entity.find("trade.importlc.SwiftMessage")
            .condition("swiftMessageId", result.swiftMessageId).one()
        msg.messageContent.contains(":59A:")
        !msg.messageContent.contains(":59:")  // plain 59 should not appear
    }
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd /Users/me/myprojects/moqui-trade && ./gradlew :runtime:component:TradeFinance:test --tests "SwiftGenerationSpec.700*" 2>&1 | tail -20
```

Expected: FAIL — skeletal implementation produces wrong content.

- [ ] **Step 3: Rewrite generate#Mt700 with full tag mapping**

Replace the `generate#Mt700` service in `SwiftGenerationServices.xml` with full implementation using `SwiftMessageBuilder`. The service must:
1. Load `TradeInstrument`, `ImportLetterOfCredit`, Applicant/Beneficiary `TradeParty`
2. Run Layer 2 validation (`validate#SwiftFields` on both entities)
3. Check lifecycle: if ACTIVE message exists, abort
4. Use `SwiftMessageBuilder` to assemble all tags from FR-SWG-05
5. Persist as `SwiftMessage` with lifecycle-aware status (DRAFT or ACTIVE)
6. Call `generate#Mt701` if overflow detected

The full service XML is large. Key structure:

```xml
<service verb="generate" noun="Mt700">
    <in-parameters>
        <parameter name="instrumentId" required="true"/>
    </in-parameters>
    <out-parameters>
        <parameter name="swiftMessageId" type="id"/>
    </out-parameters>
    <actions>
        <!-- Load entities -->
        <entity-find-one entity-name="trade.TradeInstrument" value-field="instrument">
            <field-map field-name="instrumentId"/>
        </entity-find-one>
        <entity-find-one entity-name="trade.importlc.ImportLetterOfCredit" value-field="lc">
            <field-map field-name="instrumentId"/>
        </entity-find-one>
        <if condition="!lc || !instrument"><return error="true" message="LC or Instrument not found"/></if>

        <!-- Layer 2 validation -->
        <service-call name="trade.ImportLcValidationServices.validate#SwiftFields"
                      in-map="[entityType:'ImportLetterOfCredit', entityId:instrumentId]" out-map="valLc"/>
        <service-call name="trade.ImportLcValidationServices.validate#SwiftFields"
                      in-map="[entityType:'TradeInstrument', entityId:instrumentId]" out-map="valInst"/>
        <if condition="valLc.errors || valInst.errors">
            <return error="true" message="Layer 2 validation failed: ${(valLc.errors + valInst.errors).collect{it.message}.join(', ')}"/>
        </if>

        <!-- Lifecycle check -->
        <entity-find entity-name="trade.importlc.SwiftMessage" list="existingMsgs">
            <econdition field-name="instrumentId"/>
            <econdition field-name="messageType" value="MT700"/>
            <econdition field-name="messageStatusId" value="SWIFT_MSG_ACTIVE"/>
        </entity-find>
        <if condition="existingMsgs">
            <return error="true" message="Cannot regenerate: ACTIVE message already exists for MT700 on this instrument"/>
        </if>

        <!-- Determine status -->
        <set field="msgStatus" from="instrument.transactionStatusId == 'TRANS_APPROVED' ? 'SWIFT_MSG_ACTIVE' : 'SWIFT_MSG_DRAFT'"/>

        <script><![CDATA[
            import trade.swift.SwiftMessageBuilder

            // Load party data
            def beneficiary = ec.entity.find("trade.TradeParty").condition("partyId", lc.beneficiaryPartyId).one()

            // Load bank config
            def bankBic = ec.entity.find("trade.TradeConfig").condition("configId", "ISSUING_BANK_BIC").one()?.configValue ?: "BANKXXXX"
            def advisingBic = instrument.adviseThroughBankBic ?: "ADVBANKX"

            def builder = new SwiftMessageBuilder(senderBic: bankBic, receiverBic: advisingBic, messageType: "700")

            // Check overflow for MT701
            def goodsLen = lc.goodsDescription?.length() ?: 0
            def docsLen = lc.documentsRequired?.length() ?: 0
            def condsLen = lc.additionalConditions?.length() ?: 0
            def needs701 = goodsLen > 6500 || docsLen > 6500 || condsLen > 6500
            def tag27 = needs701 ? "1/2" : "1/1"

            // Block A: Header
            builder.addTag("27", tag27)
            builder.addTag("40A", lc.lcTypeEnumId ?: "IRREVOCABLE")
            builder.addTag("20", instrument.transactionRef)
            builder.addTag("31C", SwiftMessageBuilder.formatDate(instrument.issueDate))
            builder.addTag("31D", SwiftMessageBuilder.formatDate(instrument.expiryDate) + (lc.expiryPlace ?: ""))
            builder.addOptionalTag("51A", bankBic)

            // Block B: Parties
            def applicantText = SwiftMessageBuilder.formatParty(lc.applicantName ?: "", lc.applicantAddress ?: "")
            builder.addTag("50", applicantText ?: "APPLICANT")
            if (beneficiary?.swiftBic) {
                builder.addTag("59A", "/${beneficiary.swiftBic}\r\n${beneficiary.partyName ?: ''}")
            } else {
                def benText = SwiftMessageBuilder.formatParty(beneficiary?.partyName ?: instrument.beneficiaryName ?: "", beneficiary?.registeredAddress ?: "")
                builder.addTag("59", benText ?: "BENEFICIARY")
            }

            // Block C: Financials
            builder.addTag("32B", SwiftMessageBuilder.formatAmount(instrument.currencyUomId, instrument.amount))
            if (lc.tolerancePositive || lc.toleranceNegative) {
                int pos = ((lc.tolerancePositive ?: 0.0) * 100) as int
                int neg = ((lc.toleranceNegative ?: 0.0) * 100) as int
                builder.addTag("39A", "${pos}/${neg}")
            }
            builder.addOptionalTag("39B", lc.maxCreditAmountFlag == "Y" ? "NOT EXCEEDING" : null)
            builder.addOptionalTag("39C", lc.additionalAmountsText)
            builder.addTag("41A", bankBic) // Available with issuing bank
            builder.addOptionalTag("42C", lc.usanceBaseDate)
            builder.addOptionalTag("42M", lc.mixedPaymentDetails)
            builder.addOptionalTag("42P", lc.deferredPaymentDetails)

            // Block D: Shipping
            builder.addOptionalTag("43P", lc.partialShipmentEnumId)
            builder.addOptionalTag("43T", lc.transhipmentEnumId)
            builder.addOptionalTag("44A", lc.receiptPlace)
            builder.addOptionalTag("44B", lc.finalDeliveryPlace)
            if (lc.latestShipmentDate) builder.addTag("44C", SwiftMessageBuilder.formatDate(lc.latestShipmentDate))
            builder.addOptionalTag("44D", lc.shipmentPeriodText)
            builder.addOptionalTag("44E", lc.portOfLoading)
            builder.addOptionalTag("44F", lc.portOfDischarge)

            // Block E: Narratives (truncate to 100 lines for MT700; overflow goes to MT701)
            def goodsText = lc.goodsDescription ?: ""
            if (goodsLen > 6500) goodsText = goodsText.substring(0, 6500)
            builder.addOptionalTag("45A", goodsText)
            def docsText = lc.documentsRequired ?: ""
            if (docsLen > 6500) docsText = docsText.substring(0, 6500)
            builder.addOptionalTag("46A", docsText)
            def condsText = lc.additionalConditions ?: ""
            if (condsLen > 6500) condsText = condsText.substring(0, 6500)
            builder.addOptionalTag("47A", condsText)
            builder.addOptionalTag("71D", lc.chargeAllocationText)
            if (lc.presentationPeriodDays) builder.addTag("48", "${lc.presentationPeriodDays} DAYS AFTER DATE OF SHIPMENT")
            builder.addTag("49", lc.confirmationEnumId ?: "WITHOUT")

            // Block F: Routing
            builder.addOptionalTag("53A", instrument.reimbursingBankBic)
            builder.addOptionalTag("57A", instrument.adviseThroughBankBic)

            // Block G: Admin
            builder.addOptionalTag("23", instrument.preAdviceRef)
            builder.addOptionalTag("72Z", instrument.senderToReceiverInfo)
            builder.addOptionalTag("78", lc.bankToBankInstructions)

            msgText = builder.build()
        ]]></script>

        <!-- Persist with lifecycle management -->
        <entity-find entity-name="trade.importlc.SwiftMessage" list="draftMsgs">
            <econdition field-name="instrumentId"/>
            <econdition field-name="messageType" value="MT700"/>
            <econdition field-name="messageStatusId" value="SWIFT_MSG_DRAFT"/>
        </entity-find>
        <if condition="draftMsgs">
            <!-- Update existing DRAFT -->
            <service-call name="update#trade.importlc.SwiftMessage"
                          in-map="[swiftMessageId:draftMsgs[0].swiftMessageId, messageContent:msgText, messageStatusId:msgStatus, generatedDate:ec.user.nowTimestamp]"/>
            <set field="swiftMessageId" from="draftMsgs[0].swiftMessageId"/>
        <else>
            <!-- Create new -->
            <service-call name="create#trade.importlc.SwiftMessage"
                          in-map="[instrumentId:instrumentId, messageType:'MT700', messageContent:msgText, messageStatusId:msgStatus]"
                          out-map="createOut"/>
            <set field="swiftMessageId" from="createOut.swiftMessageId"/>
        </else>
        </if>

        <!-- Generate MT701 if overflow -->
        <if condition="needs701">
            <service-call name="trade.SwiftGenerationServices.generate#Mt701" in-map="[instrumentId:instrumentId]"/>
        </if>
    </actions>
</service>
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd /Users/me/myprojects/moqui-trade && ./gradlew :runtime:component:TradeFinance:test --tests "SwiftGenerationSpec" 2>&1 | tail -20
```

Expected: All 700-* tests PASS.

- [ ] **Step 5: Commit**

```bash
git add runtime/component/TradeFinance/service/trade/SwiftGenerationServices.xml runtime/component/TradeFinance/src/test/groovy/SwiftGenerationSpec.groovy
git commit -m "feat: implement generate#Mt700 with full tag mapping and lifecycle management"
```

---

### Task 3: Implement Message Lifecycle Tests

**BDD Scenarios:** BDD-SWG-LCY-01, LCY-02, LCY-03, LCY-04, LCY-05, BDD-SWG-PER-01
**BRD Requirements:** FR-SWG-04 (Persistence), FR-SWG-15 (Lifecycle)

**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/src/test/groovy/SwiftGenerationSpec.groovy`

- [ ] **Step 1: Add lifecycle tests**

Add to `SwiftGenerationSpec.groovy`:

```groovy
    // === BDD-SWG-LCY-01: DRAFT generation pre-approval ===
    def "LCY-01: DRAFT message generated when transaction not approved"() {
        given: "An unapproved LC"
        def instrumentId = createFullLc()
        // Verify instrument is not TRANS_APPROVED
        def inst = entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId).one()
        assert inst.transactionStatusId != "TRANS_APPROVED"

        when: "generate#Mt700 is called"
        def result = service.sync().name("trade.SwiftGenerationServices.generate#Mt700")
            .parameters([instrumentId: instrumentId]).call()

        then: "SwiftMessage has DRAFT status"
        def msg = entity.find("trade.importlc.SwiftMessage")
            .condition("swiftMessageId", result.swiftMessageId).one()
        msg.messageStatusId == "SWIFT_MSG_DRAFT"
    }

    // === BDD-SWG-LCY-02: DRAFT replacement ===
    def "LCY-02: DRAFT replaced on regeneration"() {
        given: "An LC with an existing DRAFT message"
        def instrumentId = createFullLc()
        def result1 = service.sync().name("trade.SwiftGenerationServices.generate#Mt700")
            .parameters([instrumentId: instrumentId]).call()
        def firstMsgId = result1.swiftMessageId

        // Update goods description
        entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId)
            .one().set("goodsDescription", "UPDATED GOODS DESCRIPTION").update()

        when: "generate#Mt700 is called again"
        def result2 = service.sync().name("trade.SwiftGenerationServices.generate#Mt700")
            .parameters([instrumentId: instrumentId]).call()

        then: "Same swiftMessageId, updated content"
        result2.swiftMessageId == firstMsgId
        def msg = entity.find("trade.importlc.SwiftMessage")
            .condition("swiftMessageId", firstMsgId).one()
        msg.messageContent.contains("UPDATED GOODS DESCRIPTION")
        msg.messageStatusId == "SWIFT_MSG_DRAFT"

        and: "Only one MT700 message exists"
        def allMt700 = entity.find("trade.importlc.SwiftMessage")
            .condition("instrumentId", instrumentId).condition("messageType", "MT700").list()
        allMt700.size() == 1
    }

    // === BDD-SWG-LCY-04: ACTIVE regeneration blocked ===
    def "LCY-04: ACTIVE message blocks regeneration"() {
        given: "An LC with an ACTIVE MT700 message"
        def instrumentId = createFullLc()
        // Set transaction as approved
        entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId)
            .one().set("transactionStatusId", "TRANS_APPROVED").update()
        // Generate ACTIVE message
        service.sync().name("trade.SwiftGenerationServices.generate#Mt700")
            .parameters([instrumentId: instrumentId]).call()

        when: "generate#Mt700 is called again"
        service.sync().name("trade.SwiftGenerationServices.generate#Mt700")
            .parameters([instrumentId: instrumentId]).call()

        then: "Error is returned"
        ec.message.hasError()
        ec.message.getErrorsString().contains("ACTIVE message already exists")
    }
```

- [ ] **Step 2: Run tests to verify they pass**

```bash
cd /Users/me/myprojects/moqui-trade && ./gradlew :runtime:component:TradeFinance:test --tests "SwiftGenerationSpec.LCY*" 2>&1 | tail -20
```

- [ ] **Step 3: Commit**

```bash
git add runtime/component/TradeFinance/src/test/groovy/SwiftGenerationSpec.groovy
git commit -m "test: add message lifecycle tests (DRAFT/ACTIVE/blocked regeneration)"
```

---

### Task 4: Implement generate#Mt701 — Auto-Continuation

**BDD Scenarios:** BDD-SWG-701-01
**BRD Requirements:** FR-SWG-06 (MT701)

**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/service/trade/SwiftGenerationServices.xml`
- Modify: `runtime/component/TradeFinance/src/test/groovy/SwiftGenerationSpec.groovy`

- [ ] **Step 1: Write failing test for MT701 overflow**
- [ ] **Step 2: Run test to verify it fails**
- [ ] **Step 3: Implement generate#Mt701 using SwiftMessageBuilder — reads overflow from LC narrative fields beyond 6500 chars, sets Tags 45B/46B/47B, updates parent Tag 27**
- [ ] **Step 4: Run test to verify it passes**
- [ ] **Step 5: Commit**

---

### Task 5: Implement generate#Mt707 — Amendment Messages

**BDD Scenarios:** BDD-SWG-707-01 (increase), BDD-SWG-707-02 (decrease), BDD-SWG-707-03 (narrative only)
**BRD Requirements:** FR-SWG-07 (MT707)

**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/service/trade/SwiftGenerationServices.xml`
- Modify: `runtime/component/TradeFinance/src/test/groovy/SwiftGenerationSpec.groovy`

- [ ] **Step 1: Write failing tests for MT707 increase and decrease**

```groovy
    def "707-01: MT707 financial increase with 32B/34B"() {
        given: "An issued LC with a financial amendment"
        def instrumentId = createFullLc()
        def amdRes = service.sync().name("trade.importlc.ImportLcServices.create#Amendment")
            .parameters([instrumentId: instrumentId, amountAdjustment: 20000.0,
                         amendmentTypeEnumId: 'AMEND_FINANCIAL',
                         amendmentDate: new java.sql.Date(ec.user.nowTimestamp.time)]).call()

        when: "generate#Mt707 is called"
        def result = service.sync().name("trade.SwiftGenerationServices.generate#Mt707")
            .parameters([instrumentId: instrumentId, amendmentId: amdRes.amendmentId]).call()

        then: "Contains Tag 32B (increase) and 34B (new total), not 33B"
        def msg = entity.find("trade.importlc.SwiftMessage")
            .condition("swiftMessageId", result.swiftMessageId).one()
        msg.messageContent.contains(":32B:USD20000,00")
        msg.messageContent.contains(":34B:")
        !msg.messageContent.contains(":33B:")
    }
```

- [ ] **Step 2: Run test to verify it fails**
- [ ] **Step 3: Implement generate#Mt707 — load amendment, compute delta tags (32B/33B/34B conditional), include only changed fields**
- [ ] **Step 4: Run test to verify it passes**
- [ ] **Step 5: Commit**

---

### Task 6: Implement Presentation Messages — MT750, MT734, MT752

**BDD Scenarios:** BDD-SWG-750-01, BDD-SWG-734-01, BDD-SWG-752-01
**BRD Requirements:** FR-SWG-08 (MT750), FR-SWG-09 (MT734), FR-SWG-10 (MT752)

**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/service/trade/SwiftGenerationServices.xml`
- Modify: `runtime/component/TradeFinance/src/test/groovy/SwiftGenerationSpec.groovy`

- [ ] **Step 1: Write failing tests for MT750, MT734, MT752**

```groovy
    def "750-01: MT750 discrepancy advice with joined records"() {
        given: "A presentation with discrepancy records"
        def instrumentId = createFullLc()
        // Create presentation
        def presRes = service.sync().name("trade.TradeCommonServices.create#Presentation")
            .parameters([instrumentId: instrumentId, claimAmount: 100000.0]).call()
        // Add discrepancies
        service.sync().name("trade.importlc.ImportLcServices.examine#Documents")
            .parameters([presentationId: presRes.presentationId,
                         discrepancyList: [
                             [discrepancyCode: "D001", discrepancyDescription: "LATE SHIPMENT"],
                             [discrepancyCode: "D002", discrepancyDescription: "B/L NOT ENDORSED"]]]).call()

        when: "generate#Mt750 is called"
        def result = service.sync().name("trade.SwiftGenerationServices.generate#Mt750")
            .parameters([instrumentId: instrumentId, presentationId: presRes.presentationId]).call()

        then: "Message contains joined discrepancy records in Tag 77J"
        def msg = entity.find("trade.importlc.SwiftMessage")
            .condition("swiftMessageId", result.swiftMessageId).one()
        msg.messageContent.contains(":77J:")
        msg.messageContent.contains("D001")
        msg.messageContent.contains("D002")
        msg.messageContent.contains(":32B:USD100000,00")
    }
```

- [ ] **Step 2: Run tests to verify they fail**
- [ ] **Step 3: Implement generate#Mt750 — joins PresentationDiscrepancy records into Tag 77J**
- [ ] **Step 4: Implement generate#Mt734 — maps documentDisposalEnumId to Tag 77B text**
- [ ] **Step 5: Implement generate#Mt752 — simple message after waiver**
- [ ] **Step 6: Run all tests to verify they pass**
- [ ] **Step 7: Commit**

---

### Task 7: Implement MT732, MT799, MT202, MT103

**BDD Scenarios:** BDD-SWG-732-01, BDD-SWG-799-01, BDD-SWG-202-01, BDD-SWG-103-01
**BRD Requirements:** FR-SWG-11 (MT732), FR-SWG-12 (MT799), FR-SWG-13 (MT202), FR-SWG-14 (MT103)

**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/service/trade/SwiftGenerationServices.xml`
- Modify: `runtime/component/TradeFinance/src/test/groovy/SwiftGenerationSpec.groovy`

- [ ] **Step 1: Write failing tests for MT732, MT799, MT202, MT103**

For each MT type, write a test that:
1. Creates the prerequisite entity data (LC + presentation/settlement as needed)
2. Calls the appropriate `generate#MtXXX` service
3. Asserts the `SwiftMessage` contains the expected mandatory tags

```groovy
    def "799-01: MT799 cancellation request narrative"() {
        given: "An LC with early cancellation request"
        def instrumentId = createFullLc()

        when: "generate#Mt799 is called"
        def result = service.sync().name("trade.SwiftGenerationServices.generate#Mt799")
            .parameters([instrumentId: instrumentId]).call()

        then: "Message contains Tag 79 with cancellation narrative"
        def msg = entity.find("trade.importlc.SwiftMessage")
            .condition("swiftMessageId", result.swiftMessageId).one()
        msg.messageContent.contains(":20:")
        msg.messageContent.contains(":79:")
    }
```

- [ ] **Step 2: Run tests to verify they fail**
- [ ] **Step 3: Implement generate#Mt732 — uses maturity date in Tag 32A**
- [ ] **Step 4: Implement generate#Mt799 — generates cancellation narrative in Z charset**
- [ ] **Step 5: Implement generate#Mt202 — bank-to-bank with Nostro lookup**
- [ ] **Step 6: Implement generate#Mt103 — customer credit with party details and charges**
- [ ] **Step 7: Run all tests to verify they pass**
- [ ] **Step 8: Commit**

---

### Task 8: Layer 2 Validation Integration Tests

**BDD Scenarios:** BDD-SWG-L2V-01 (abort on invalid), BDD-SWG-L2V-02 (auto-convert)
**BRD Requirements:** FR-SWG-03 (Layer 2)

**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/src/test/groovy/SwiftGenerationSpec.groovy`

- [ ] **Step 1: Write Layer 2 validation tests**

```groovy
    def "L2V-01: Generation aborted on invalid data"() {
        given: "An LC with slash rule violation in transactionRef"
        def ref = "//TF-BAD-" + System.currentTimeMillis()
        def res = service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD"]).call()

        when: "generate#Mt700 is called"
        service.sync().name("trade.SwiftGenerationServices.generate#Mt700")
            .parameters([instrumentId: res.instrumentId]).call()

        then: "Error about Layer 2 validation failure"
        ec.message.hasError()
        ec.message.getErrorsString().contains("Layer 2 validation failed")

        and: "No SwiftMessage record created"
        def msgs = entity.find("trade.importlc.SwiftMessage")
            .condition("instrumentId", res.instrumentId).list()
        msgs.size() == 0
    }
```

- [ ] **Step 2: Run test to verify it passes (Layer 2 is already wired in Task 2)**
- [ ] **Step 3: Commit**

---

### Task 9: Full Regression Run

**BDD Scenarios:** All
**BRD Requirements:** All

**User-Facing:** NO

- [ ] **Step 1: Run full test suite**

```bash
cd /Users/me/myprojects/moqui-trade && ./gradlew :runtime:component:TradeFinance:test 2>&1 | tail -50
```

Expected: All tests pass, including the existing `BddImportLcModuleSpec` tests.

- [ ] **Step 2: Commit any fixes**

---

## Verification Plan

### Automated Tests

The `SwiftGenerationSpec` spec provides comprehensive automated coverage:

```bash
# Run just the generation spec
cd /Users/me/myprojects/moqui-trade && ./gradlew :runtime:component:TradeFinance:test --tests "SwiftGenerationSpec"

# Run full suite (regression)
cd /Users/me/myprojects/moqui-trade && ./gradlew :runtime:component:TradeFinance:test
```

After tests complete, review the HTML report:
`runtime/component/TradeFinance/build/reports/tests/test/index.html`

### Manual Verification

No manual verification needed — all generation logic is backend-only and fully covered by automated tests.
