# SWIFT SRG 2024 Remaining Gaps Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the remaining SWIFT SRG 2024 compliance gaps: MT 700 field additions, MT 740/747 auto-generation, manual Nostro reconciliation, Tag 77J enforcement, and Tag 23S cancellation.

**Architecture:** Extends existing entity/service/SECA patterns. New services follow `generate#Mt700` pattern in `SwiftGenerationServices.xml`. New entity `NostroReconciliation` in `ImportLcEntities.xml`. Tag 77J validation added to `ImportLcValidationServices.xml`. All changes are additive — no existing service signatures change.

**Tech Stack:** Moqui XML entities, Moqui XML services, Groovy (Spock tests), SECA hooks

---

## File Structure

| Action | File | Responsibility |
| :--- | :--- | :--- |
| Modify | `entity/ImportLcEntities.xml` | Add fields to `ImportLetterOfCredit`, `ImportLcAmendment`; add `NostroReconciliation` entity |
| Modify | `service/trade/SwiftGenerationServices.xml` | Add `generate#Mt740`, `generate#Mt747`; modify `generate#Mt700` for Tags 49G/49H/40E; modify `generate#Mt707` for Tag 23S |
| Modify | `service/trade/importlc/ImportLcValidationServices.xml` | Add Tag 77J aggregate validation (PRE-SWV-08/10) |
| Modify | `service/ImportLc.secas.xml` | Add SECA hooks for MT 740 (on issuance) and MT 747 (on amendment) |
| Modify | `data/TradeFinanceDemoData.xml` | Add enum seed data for `REIMB_OUR`, `REIMB_BEN`, `RECON_*` statuses, `UCP_LATEST` etc. |
| Create | `src/test/groovy/trade/SwiftReimbursementSpec.groovy` | Tests for MT 740/747 generation and Nostro reconciliation |
| Modify | `src/test/groovy/trade/SwiftGenerationSpec.groovy` | Tests for Tags 49G/49H/40E in MT 700, Tag 23S in MT 707 |
| Modify | `src/test/groovy/trade/SwiftValidationSpec.groovy` | Tests for Tag 77J aggregate limit |

---

### Task 1: Add Entity Fields and Seed Data

**BDD Scenarios:** Prerequisite for all scenarios (S1-S13)
**BRD Requirements:** FR-SWG-20 (49G/49H/40E fields), FR-SWG-21 (MT 740 fields), FR-SWG-22 (MT 747 fields), FR-RMB-01 (NostroReconciliation entity), FR-CAN-04 (Tag 23S field)

**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/entity/ImportLcEntities.xml`
- Modify: `runtime/component/TradeFinance/data/TradeFinanceDemoData.xml`

- [ ] **Step 1: Add new fields to `ImportLetterOfCredit` entity**

Add after line 54 (`chargeAllocationText` field), before `marginType`:

```xml
<field name="paymentCondBeneText" type="text-long"/> <!-- Tag 49G: max 4x35, X charset -->
<field name="paymentCondBankText" type="text-long"/> <!-- Tag 49H: max 4x35, X charset -->
<field name="applicableRulesEnumId" type="id"/> <!-- Tag 40E: UCP_LATEST, EUCP_LATEST, etc. -->
<field name="authExpiryDate" type="date"/> <!-- Tag 31D (MT740): Reimbursement auth expiry -->
<field name="reimbursingChargesEnumId" type="id"/> <!-- Tag 71D (MT740): REIMB_OUR, REIMB_BEN -->
<field name="applicableReimbRulesText" type="text-medium"/> <!-- Tag 40F (MT740): default URR LATEST VERSION -->
```

- [ ] **Step 2: Add new fields to `ImportLcAmendment` entity**

Add after line 175 (`senderToReceiverInfo` field), before `beneficiaryConsentStatusId`:

```xml
<field name="isCancellationRequest" type="text-indicator" default="'N'"/> <!-- Tag 23S: Y triggers CANCEL -->
<field name="newAuthExpiryDate" type="date"/> <!-- Tag 31E (MT747): New reimbursement expiry -->
```

- [ ] **Step 3: Add `NostroReconciliation` entity**

Add after the `ImportLcSettlement` entity (after line 242), before the view-entity:

```xml
<entity entity-name="NostroReconciliation" package="trade.importlc">
    <field name="reconciliationId" type="id" is-pk="true"/>
    <field name="instrumentId" type="id"/>
    <field name="reimbursingBankPartyId" type="id"/>
    <field name="expectedCurrency" type="id"/>
    <field name="expectedAmount" type="number-decimal"/>
    <field name="nostroDebitDate" type="date"/>
    <field name="nostroDebitAmount" type="number-decimal"/>
    <field name="nostroStatementRef" type="text-short"/>
    <field name="matchStatusEnumId" type="id" default="'RECON_PENDING'"/>
    <field name="matchedByUserId" type="id"/>
    <field name="matchedDate" type="date"/>
    <field name="remarks" type="text-long"/>
    <relationship type="one" related="trade.importlc.ImportLetterOfCredit">
        <key-map field-name="instrumentId"/>
    </relationship>
</entity>
```

- [ ] **Step 4: Add enum seed data**

Add to `TradeFinanceDemoData.xml`:

```xml
<!-- Reimbursing Charges -->
<moqui.basic.Enumeration enumId="REIMB_OUR" description="Charges on Applicant" enumTypeId="ReimbursingCharges"/>
<moqui.basic.Enumeration enumId="REIMB_BEN" description="Charges on Beneficiary" enumTypeId="ReimbursingCharges"/>

<!-- Applicable Rules -->
<moqui.basic.Enumeration enumId="UCP_LATEST" description="UCP LATEST VERSION" enumTypeId="ApplicableRules"/>
<moqui.basic.Enumeration enumId="EUCP_LATEST" description="EUCP LATEST VERSION" enumTypeId="ApplicableRules"/>
<moqui.basic.Enumeration enumId="UCPDC_600" description="UCP 600" enumTypeId="ApplicableRules"/>
<moqui.basic.Enumeration enumId="ISP_LATEST" description="ISP LATEST VERSION" enumTypeId="ApplicableRules"/>

<!-- Nostro Reconciliation Status -->
<moqui.basic.Enumeration enumId="RECON_PENDING" description="Pending Reconciliation" enumTypeId="NostroReconStatus"/>
<moqui.basic.Enumeration enumId="RECON_MATCHED" description="Matched" enumTypeId="NostroReconStatus"/>
<moqui.basic.Enumeration enumId="RECON_UNMATCHED" description="Unmatched" enumTypeId="NostroReconStatus"/>
<moqui.basic.Enumeration enumId="RECON_PARTIAL" description="Partial Match" enumTypeId="NostroReconStatus"/>
```

- [ ] **Step 5: Run `gradlew reloadSave` to verify entity compilation**

Run: `./gradlew reloadSave 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL (entities compile without XSD errors)

- [ ] **Step 6: Commit**

```bash
git add runtime/component/TradeFinance/entity/ImportLcEntities.xml runtime/component/TradeFinance/data/TradeFinanceDemoData.xml
git commit -m "feat(entity): add fields for MT700 49G/49H/40E, MT740/747 reimbursement, NostroReconciliation, Tag 23S"
```

---

### Task 2: MT 700 Tag Additions (49G/49H/40E) and MT 707 Tag 23S

**BDD Scenarios:** S1 (payment conditions in MT 700), S2 (charset rejection), S12 (Tag 23S cancel), S13 (mixed cancel blocked)
**BRD Requirements:** FR-SWG-20 (49G/49H/40E), FR-CAN-04 (23S), ISS-SWV-18/19/20, CAN-SWV-04/05

**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/service/trade/SwiftGenerationServices.xml`
- Modify: `runtime/component/TradeFinance/src/test/groovy/trade/SwiftGenerationSpec.groovy`

- [ ] **Step 1: Write failing test for Tags 49G/49H/40E in MT 700**

Add to `SwiftGenerationSpec.groovy`:

```groovy
def "MT700 includes Tags 49G, 49H, and 40E when populated"() {
    given: "LC with payment conditions and applicable rules"
    ec.entity.find("trade.importlc.ImportLetterOfCredit")
        .condition("instrumentId", instrumentId)
        .updateAll([paymentCondBeneText: "Payment upon receipt of clean BL",
                    paymentCondBankText: "Reimburse via MT 202 only",
                    applicableRulesEnumId: "UCP_LATEST"])

    when:
    def result = ec.service.sync().name("trade.SwiftGenerationServices.generate#Mt700")
        .parameters([instrumentId: instrumentId, forceRegenerate: true]).call()

    then:
    result.messageContent.contains("49G:Payment upon receipt of clean BL")
    result.messageContent.contains("49H:Reimburse via MT 202 only")
    result.messageContent.contains("40E:UCP LATEST VERSION")
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :runtime:component:TradeFinance:test --tests "trade.SwiftGenerationSpec.MT700 includes Tags 49G*" 2>&1 | tail -30`
Expected: FAIL (fields not yet read in generate#Mt700)

- [ ] **Step 3: Add Tags 49G/49H/40E to generate#Mt700**

In `SwiftGenerationServices.xml`, inside the `generate#Mt700` script block (after line 153, after `fields["78"]`), add:

```groovy
// FR-SWG-20: Payment Conditions and Applicable Rules
if (lc.paymentCondBeneText) fields["49G"] = formatValue(lc.paymentCondBeneText, 4, 35)
if (lc.paymentCondBankText) fields["49H"] = formatValue(lc.paymentCondBankText, 4, 35)
if (lc.applicableRulesEnumId) {
    def rulesMap = [UCP_LATEST:"UCP LATEST VERSION", EUCP_LATEST:"EUCP LATEST VERSION",
                    UCPDC_600:"UCP 600", ISP_LATEST:"ISP LATEST VERSION"]
    fields["40E"] = rulesMap[lc.applicableRulesEnumId] ?: "UCP LATEST VERSION"
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :runtime:component:TradeFinance:test --tests "trade.SwiftGenerationSpec.MT700 includes Tags 49G*" 2>&1 | tail -30`
Expected: PASS

- [ ] **Step 5: Write failing test for Tag 23S in MT 707**

Add to `SwiftGenerationSpec.groovy`:

```groovy
def "MT707 includes Tag 23S CANCEL when isCancellationRequest is Y"() {
    given: "Amendment with cancellation request"
    ec.entity.find("trade.importlc.ImportLcAmendment")
        .condition("amendmentId", testAmendmentId)
        .updateAll([isCancellationRequest: "Y"])

    when:
    def result = ec.service.sync().name("trade.SwiftGenerationServices.generate#Mt707")
        .parameters([amendmentId: testAmendmentId, forceRegenerate: true]).call()

    then:
    result.messageContent.contains("23S:CANCEL")
}
```

- [ ] **Step 6: Run test to verify it fails**

Run: `./gradlew :runtime:component:TradeFinance:test --tests "trade.SwiftGenerationSpec.MT707 includes Tag 23S*" 2>&1 | tail -30`
Expected: FAIL

- [ ] **Step 7: Add Tag 23S to generate#Mt707**

In `SwiftGenerationServices.xml`, inside the `generate#Mt707` script block (after line 303, after `fields["22A"]`), add:

```groovy
// FR-CAN-04: Cancellation Request
if (amendment.isCancellationRequest == "Y") fields["23S"] = "CANCEL"
```

- [ ] **Step 8: Run test to verify it passes**

Run: `./gradlew :runtime:component:TradeFinance:test --tests "trade.SwiftGenerationSpec.MT707 includes Tag 23S*" 2>&1 | tail -30`
Expected: PASS

- [ ] **Step 9: Commit**

```bash
git add runtime/component/TradeFinance/service/trade/SwiftGenerationServices.xml runtime/component/TradeFinance/src/test/groovy/trade/SwiftGenerationSpec.groovy
git commit -m "feat(swift): add Tags 49G/49H/40E to MT700, Tag 23S to MT707"
```

---

### Task 3: MT 740 Generation Service

**BDD Scenarios:** S3 (MT 740 auto-gen), S4 (no reimbursing bank = no MT 740)
**BRD Requirements:** FR-SWG-21, RMB-SWV-01 through RMB-SWV-05

**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/service/trade/SwiftGenerationServices.xml`
- Create: `runtime/component/TradeFinance/src/test/groovy/trade/SwiftReimbursementSpec.groovy`

- [ ] **Step 1: Write failing test for MT 740 generation**

Create `SwiftReimbursementSpec.groovy`:

```groovy
package trade

import spock.lang.Specification
import spock.lang.Shared
import org.moqui.Moqui
import org.moqui.context.ExecutionContext

class SwiftReimbursementSpec extends Specification {
    @Shared ExecutionContext ec

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.user.loginUser("john.doe", "moqui")
        ec.artifactExecution.disableAuthz()

        // Create test instrument with reimbursing bank
        ec.service.sync().name("create#trade.TradeInstrument").parameters([
            instrumentId: "REIMB_TEST_01", instrumentRef: "RTEST001",
            instrumentTypeEnumId: "IMPORT_LC", amount: 100000.00,
            currencyUomId: "USD", issueDate: new java.sql.Date(System.currentTimeMillis()),
            expiryDate: java.sql.Date.valueOf("2026-09-30")
        ]).call()

        ec.service.sync().name("create#trade.importlc.ImportLetterOfCredit").parameters([
            instrumentId: "REIMB_TEST_01", businessStateId: "LC_ISSUED",
            tolerancePositive: 0.10, toleranceNegative: 0.10,
            availableWithEnumId: "AVAIL_ANY_BANK",
            authExpiryDate: java.sql.Date.valueOf("2026-10-30"),
            reimbursingChargesEnumId: "REIMB_OUR",
            applicableReimbRulesText: "URR LATEST VERSION"
        ]).call()

        // Create reimbursing bank party
        ec.service.sync().name("create#trade.TradeParty").parameters([
            partyId: "REIMB_BANK_01", partyName: "CITIBANK NEW YORK",
            partyTypeEnumId: "TP_TYPE_BANK"
        ]).call()
        ec.service.sync().name("create#trade.TradePartyBank").parameters([
            partyId: "REIMB_BANK_01", swiftBic: "CITIUS33",
            nostroAccountRef: "36112345"
        ]).call()
        ec.service.sync().name("create#trade.TradeInstrumentParty").parameters([
            instrumentId: "REIMB_TEST_01", partyId: "REIMB_BANK_01",
            roleEnumId: "TP_REIMBURSING_BANK"
        ]).call()

        // Create advising bank, beneficiary (required for MT 740 tags)
        ec.service.sync().name("create#trade.TradeParty").parameters([
            partyId: "ADV_BANK_R01", partyName: "HSBC HONG KONG",
            partyTypeEnumId: "TP_TYPE_BANK"
        ]).call()
        ec.service.sync().name("create#trade.TradePartyBank").parameters([
            partyId: "ADV_BANK_R01", swiftBic: "HSBCHKHH"
        ]).call()
        ec.service.sync().name("create#trade.TradeInstrumentParty").parameters([
            instrumentId: "REIMB_TEST_01", partyId: "ADV_BANK_R01",
            roleEnumId: "TP_ADVISING_BANK"
        ]).call()

        ec.service.sync().name("create#trade.TradeParty").parameters([
            partyId: "BEN_R01", partyName: "ACME EXPORTS LTD",
            partyTypeEnumId: "TP_TYPE_COMMERCIAL"
        ]).call()
        ec.service.sync().name("create#trade.TradeInstrumentParty").parameters([
            instrumentId: "REIMB_TEST_01", partyId: "BEN_R01",
            roleEnumId: "TP_BENEFICIARY"
        ]).call()
    }

    def cleanupSpec() {
        ec?.destroy()
    }

    def "MT740 generated with correct tags when reimbursing bank assigned"() {
        when:
        def result = ec.service.sync().name("trade.SwiftGenerationServices.generate#Mt740")
            .parameters([instrumentId: "REIMB_TEST_01"]).call()

        then:
        result.messageContent != null
        result.messageContent.contains("CITIUS33")  // Receiver BIC
        result.messageContent.contains("36112345")  // Tag 25: Nostro account
        result.messageContent.contains("USD100000,00")  // Tag 32B
        result.messageContent.contains("ANY BANK")  // Tag 58a
        result.messageContent.contains("URR LATEST VERSION")  // Tag 40F
        result.swiftMessageId != null
    }

    def "NostroReconciliation record created when MT740 generated"() {
        when:
        ec.service.sync().name("trade.SwiftGenerationServices.generate#Mt740")
            .parameters([instrumentId: "REIMB_TEST_01"]).call()
        def reconList = ec.entity.find("trade.importlc.NostroReconciliation")
            .condition("instrumentId", "REIMB_TEST_01").list()

        then:
        reconList.size() >= 1
        reconList[0].matchStatusEnumId == "RECON_PENDING"
        reconList[0].expectedAmount == 100000.00
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :runtime:component:TradeFinance:test --tests "trade.SwiftReimbursementSpec" 2>&1 | tail -30`
Expected: FAIL (generate#Mt740 service does not exist)

- [ ] **Step 3: Implement generate#Mt740 service**

Add to `SwiftGenerationServices.xml` (before the `</services>` closing tag):

```xml
<service verb="generate" noun="Mt740" authenticate="false">
    <description>Generates MT740 (Authorization to Reimburse). Auto-triggered alongside MT700 when TP_REIMBURSING_BANK assigned.</description>
    <in-parameters>
        <parameter name="instrumentId" required="true"/>
    </in-parameters>
    <out-parameters>
        <parameter name="swiftMessageId" type="id"/>
        <parameter name="messageContent"/>
    </out-parameters>
    <actions>
        <script>ec.artifactExecution.disableAuthz()</script>
        <entity-find-one entity-name="trade.TradeInstrument" value-field="instrument" use-cache="false"/>
        <entity-find-one entity-name="trade.importlc.ImportLetterOfCredit" value-field="lc" use-cache="false"/>

        <!-- RMB-SWV-01: Only generate if TP_REIMBURSING_BANK assigned -->
        <entity-find entity-name="trade.TradeInstrumentParty" list="instrumentParties">
            <econdition field-name="instrumentId"/>
        </entity-find>
        <set field="reimbJunc" from="instrumentParties.find { it.roleEnumId == 'TP_REIMBURSING_BANK' }"/>
        <if condition="!reimbJunc">
            <log level="info" message="No TP_REIMBURSING_BANK for instrument ${instrumentId}. Skipping MT740."/>
            <return/>
        </if>

        <entity-find-one entity-name="trade.common.TradeConfig" value-field="bicConfig">
            <field-map field-name="configId" value="ISSUING_BANK_BIC"/>
        </entity-find-one>
        <set field="issuerBic" from="bicConfig?.configValue ?: 'ISSUERXX'"/>

        <set field="messageStatusId" value="SWIFT_MSG_ACTIVE"/>

        <script><![CDATA[
            def getParty(role) {
                def junc = instrumentParties.find { it.roleEnumId == role }
                if (!junc) return [:]
                def tp = ec.entity.find("trade.TradeParty").condition("partyId", junc.partyId).one()
                def tpb = ec.entity.find("trade.TradePartyBank").condition("partyId", junc.partyId).one()
                return [partyId: junc.partyId, name: tp?.partyName, address: tp?.registeredAddress, bic: tpb?.swiftBic, nostro: tpb?.nostroAccountRef]
            }

            def formatValue = { val, lines, len ->
                ec.service.sync().name("trade.SwiftUtilsServices.format#Value")
                    .parameters([value:val, maxLines:lines, maxLineLength:len]).call().formattedValue
            }

            def reimbursingBank = getParty("TP_REIMBURSING_BANK")
            def beneficiary = getParty("TP_BENEFICIARY")

            def fields = [:]
            fields["31D"] = (lc.authExpiryDate?.format("yyMMdd") ?: instrument.expiryDate?.format("yyMMdd") ?: "")
            fields["32B"] = (instrument.currencyUomId ?: "USD") + String.format("%.2f", (instrument.amount ?: 0.0)).replace(".", ",")

            if (lc.tolerancePositive != null || lc.toleranceNegative != null) {
                int pos = (int)((lc.tolerancePositive ?: 0.0) * 100)
                int neg = (int)((lc.toleranceNegative ?: 0.0) * 100)
                fields["39A"] = "${pos}/${neg}"
            } else if (lc.maxCreditAmountFlag == "Y") {
                fields["39B"] = "MAXIMUM"
            }

            fields["40F"] = lc.applicableReimbRulesText ?: "URR LATEST VERSION"

            // Tag 25: Nostro account
            if (reimbursingBank.nostro) fields["25"] = reimbursingBank.nostro

            // Tag 58a: Negotiating Bank (who can claim)
            if (lc.availableWithEnumId == 'AVAIL_ANY_BANK') {
                fields["58D"] = "ANY BANK"
            } else {
                def negBank = getParty("TP_NEGOTIATING_BANK")
                fields["58A"] = negBank.bic ?: "NEGOTXXX"
            }

            // Tag 59: Beneficiary
            def benText = (beneficiary.name ?: "BENEFICIARY") + (beneficiary.address ? "\r\n" + beneficiary.address : "")
            fields["59"] = formatValue(benText, 4, 35)

            // Tag 71D: Charges
            if (lc.reimbursingChargesEnumId) {
                def chargeMap = [REIMB_OUR:"OUR", REIMB_BEN:"BEN"]
                fields["71D"] = chargeMap[lc.reimbursingChargesEnumId] ?: "OUR"
            }

            def renderRes = ec.service.sync().name("trade.SwiftUtilsServices.render#SwiftMessage")
                .parameters([messageType:"740", senderBic:issuerBic, receiverBic:reimbursingBank.bic ?: "REIMBXXX",
                             senderRef:instrument.instrumentRef, fields:fields]).call()
            messageContent = renderRes.messageContent

            // FR-RMB-01: Auto-create NostroReconciliation record
            ec.service.sync().name("create#trade.importlc.NostroReconciliation").parameters([
                instrumentId: instrumentId,
                reimbursingBankPartyId: reimbursingBank.partyId,
                expectedCurrency: instrument.currencyUomId ?: "USD",
                expectedAmount: instrument.amount ?: 0.0,
                matchStatusEnumId: "RECON_PENDING"
            ]).call()
        ]]></script>

        <service-call name="create#trade.importlc.SwiftMessage" out-map="createRes"
            in-map="[instrumentId:instrumentId, messageType:'MT740', messageContent:messageContent, messageStatusId:messageStatusId]"/>
        <set field="swiftMessageId" from="createRes.swiftMessageId"/>
    </actions>
</service>
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :runtime:component:TradeFinance:test --tests "trade.SwiftReimbursementSpec" 2>&1 | tail -30`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add runtime/component/TradeFinance/service/trade/SwiftGenerationServices.xml runtime/component/TradeFinance/src/test/groovy/trade/SwiftReimbursementSpec.groovy
git commit -m "feat(swift): add generate#Mt740 service with NostroReconciliation auto-creation"
```

---

### Task 4: MT 747 Generation Service and SECA Hooks

**BDD Scenarios:** S5 (MT 747 auto-gen on financial amendment), S6 (no MT 747 on narrative amendment)
**BRD Requirements:** FR-SWG-22, RMB-SWV-06/07

**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/service/trade/SwiftGenerationServices.xml`
- Modify: `runtime/component/TradeFinance/service/ImportLc.secas.xml`
- Modify: `runtime/component/TradeFinance/src/test/groovy/trade/SwiftReimbursementSpec.groovy`

- [ ] **Step 1: Write failing test for MT 747 generation**

Add to `SwiftReimbursementSpec.groovy`:

```groovy
def "MT747 generated when financial amendment authorized on LC with reimbursing bank"() {
    given: "Amendment with amount increase"
    ec.service.sync().name("create#trade.importlc.ImportLcAmendment").parameters([
        amendmentId: "AMD_REIMB_01", instrumentId: "REIMB_TEST_01",
        amendmentDate: new java.sql.Date(System.currentTimeMillis()),
        amountIncrease: 25000.00, amendmentNumber: 1,
        newAuthExpiryDate: java.sql.Date.valueOf("2026-12-31"),
        amendmentBusinessStateId: "AMEND_APPROVED"
    ]).call()

    when:
    def result = ec.service.sync().name("trade.SwiftGenerationServices.generate#Mt747")
        .parameters([amendmentId: "AMD_REIMB_01"]).call()

    then:
    result.messageContent != null
    result.messageContent.contains("USD25000,00")  // Tag 32B: increase
    result.messageContent.contains("USD125000,00")  // Tag 34B: new total
    result.messageContent.contains("261231")  // Tag 31E: new auth expiry
}

def "MT747 not generated when amendment has no financial changes"() {
    given: "Amendment with narrative-only changes"
    ec.service.sync().name("create#trade.importlc.ImportLcAmendment").parameters([
        amendmentId: "AMD_REIMB_02", instrumentId: "REIMB_TEST_01",
        amendmentDate: new java.sql.Date(System.currentTimeMillis()),
        goodsActionEnumId: "ADD", goodsDeltaText: "Certificate of Origin required",
        amendmentNumber: 2, amendmentBusinessStateId: "AMEND_APPROVED"
    ]).call()

    when:
    def result = ec.service.sync().name("trade.SwiftGenerationServices.generate#Mt747")
        .parameters([amendmentId: "AMD_REIMB_02"]).call()

    then:
    result.messageContent == null
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :runtime:component:TradeFinance:test --tests "trade.SwiftReimbursementSpec.MT747*" 2>&1 | tail -30`
Expected: FAIL (generate#Mt747 service does not exist)

- [ ] **Step 3: Implement generate#Mt747 service**

Add to `SwiftGenerationServices.xml` (after the `generate#Mt740` service):

```xml
<service verb="generate" noun="Mt747" authenticate="false">
    <description>Generates MT747 (Amendment to Authorization to Reimburse). Auto-triggered alongside MT707 when financials change and TP_REIMBURSING_BANK assigned.</description>
    <in-parameters>
        <parameter name="amendmentId" required="true"/>
    </in-parameters>
    <out-parameters>
        <parameter name="swiftMessageId" type="id"/>
        <parameter name="messageContent"/>
    </out-parameters>
    <actions>
        <script>ec.artifactExecution.disableAuthz()</script>
        <entity-find-one entity-name="trade.importlc.ImportLcAmendment" value-field="amendment" use-cache="false"/>
        <if condition="!amendment"><return/></if>

        <entity-find-one entity-name="trade.TradeInstrument" value-field="instrument" use-cache="false">
            <field-map field-name="instrumentId" from="amendment.instrumentId"/>
        </entity-find-one>

        <!-- RMB-SWV-06: Only generate if financial changes exist -->
        <if condition="!amendment.amountIncrease &amp;&amp; !amendment.amountDecrease &amp;&amp; !amendment.newAuthExpiryDate">
            <log level="info" message="No financial changes in amendment ${amendmentId}. Skipping MT747."/>
            <return/>
        </if>

        <!-- RMB-SWV-07: Only generate if TP_REIMBURSING_BANK assigned -->
        <entity-find entity-name="trade.TradeInstrumentParty" list="instrumentParties">
            <econdition field-name="instrumentId" from="amendment.instrumentId"/>
        </entity-find>
        <set field="reimbJunc" from="instrumentParties.find { it.roleEnumId == 'TP_REIMBURSING_BANK' }"/>
        <if condition="!reimbJunc">
            <log level="info" message="No TP_REIMBURSING_BANK. Skipping MT747."/>
            <return/>
        </if>

        <entity-find-one entity-name="trade.common.TradeConfig" value-field="bicConfig">
            <field-map field-name="configId" value="ISSUING_BANK_BIC"/>
        </entity-find-one>
        <set field="issuerBic" from="bicConfig?.configValue ?: 'ISSUERXX'"/>

        <script><![CDATA[
            def reimbBic = ec.entity.find("trade.TradePartyBank").condition("partyId", reimbJunc.partyId).one()?.swiftBic ?: "REIMBXXX"

            def fields = [:]
            fields["30"] = amendment.amendmentDate?.format("yyMMdd") ?: ec.user.nowTimestamp.format("yyMMdd")

            if (amendment.amountIncrease && amendment.amountIncrease > 0) {
                fields["32B"] = (instrument.currencyUomId ?: "USD") + String.format("%.2f", amendment.amountIncrease).replace(".", ",")
            }
            if (amendment.amountDecrease && amendment.amountDecrease > 0) {
                fields["33B"] = (instrument.currencyUomId ?: "USD") + String.format("%.2f", amendment.amountDecrease).replace(".", ",")
            }

            // Tag 34B: New total authorized amount
            if (amendment.amountIncrease || amendment.amountDecrease) {
                def delta = (amendment.amountIncrease ?: 0.0) - (amendment.amountDecrease ?: 0.0)
                def newTotal = (instrument.amount ?: 0.0) + delta
                fields["34B"] = (instrument.currencyUomId ?: "USD") + String.format("%.2f", newTotal).replace(".", ",")
            }

            if (amendment.newAuthExpiryDate) {
                fields["31E"] = amendment.newAuthExpiryDate.format("yyMMdd")
            }

            def renderRes = ec.service.sync().name("trade.SwiftUtilsServices.render#SwiftMessage")
                .parameters([messageType:"747", senderBic:issuerBic, receiverBic:reimbBic,
                             senderRef:instrument.instrumentRef, fields:fields]).call()
            messageContent = renderRes.messageContent
        ]]></script>

        <service-call name="create#trade.importlc.SwiftMessage" out-map="createRes"
            in-map="[instrumentId:amendment.instrumentId, messageType:'MT747', messageContent:messageContent, messageStatusId:'SWIFT_MSG_ACTIVE']"/>
        <set field="swiftMessageId" from="createRes.swiftMessageId"/>
    </actions>
</service>
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :runtime:component:TradeFinance:test --tests "trade.SwiftReimbursementSpec" 2>&1 | tail -30`
Expected: PASS

- [ ] **Step 5: Add SECA hooks for MT 740 and MT 747**

Add to `ImportLc.secas.xml`, alongside the existing MT 700/707 SECAs:

For MT 740 (alongside MT 700 on issuance authorization):
```xml
<!-- FR-SWG-21: Auto-generate MT740 alongside MT700 when Reimbursing Bank assigned -->
<seca id="GenerateMt740OnIssuance" service="trade.AuthorizationServices.authorize#Transaction" when="post-service">
    <condition><expression>transactionTypeEnumId == 'IMP_NEW'</expression></condition>
    <actions>
        <service-call name="trade.SwiftGenerationServices.generate#Mt740" in-map="[instrumentId: instrumentId]"/>
    </actions>
</seca>
```

For MT 747 (alongside MT 707 on amendment authorization):
```xml
<!-- FR-SWG-22: Auto-generate MT747 alongside MT707 when financials change -->
<seca id="GenerateMt747OnAmendment" service="trade.AuthorizationServices.authorize#Transaction" when="post-service">
    <condition><expression>transactionTypeEnumId == 'IMP_AMEND'</expression></condition>
    <actions>
        <service-call name="trade.SwiftGenerationServices.generate#Mt747" in-map="[amendmentId: amendmentId]"/>
    </actions>
</seca>
```

- [ ] **Step 6: Commit**

```bash
git add runtime/component/TradeFinance/service/trade/SwiftGenerationServices.xml runtime/component/TradeFinance/service/ImportLc.secas.xml runtime/component/TradeFinance/src/test/groovy/trade/SwiftReimbursementSpec.groovy
git commit -m "feat(swift): add generate#Mt747 service and SECA hooks for MT740/MT747 auto-trigger"
```

---

### Task 5: Tag 77J Aggregate Validation

**BDD Scenarios:** S10 (line count enforcement), S11 (generation-time safety net)
**BRD Requirements:** FR-PRE-08, PRE-SWV-08/09/10

**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/service/trade/importlc/ImportLcValidationServices.xml`
- Modify: `runtime/component/TradeFinance/src/test/groovy/trade/SwiftValidationSpec.groovy`

- [ ] **Step 1: Write failing test for Tag 77J aggregate validation**

Add to `SwiftValidationSpec.groovy`:

```groovy
def "Tag 77J aggregate validation blocks when total exceeds 70 lines"() {
    given: "Presentation with many discrepancies that exceed 70 lines"
    def presId = "PRES_77J_TEST"
    ec.service.sync().name("create#trade.importlc.TradeDocumentPresentation").parameters([
        presentationId: presId, instrumentId: instrumentId,
        claimAmount: 50000.00, claimCurrency: "USD",
        presentationStatusId: "PRES_DISCREPANT", isDiscrepant: "Y"
    ]).call()

    // Create enough discrepancies to exceed 70 lines (each ~6 lines)
    (1..13).each { i ->
        ec.service.sync().name("create#trade.importlc.PresentationDiscrepancy").parameters([
            presentationId: presId,
            discrepancyCode: "DISC${String.format('%02d', i)}",
            discrepancyDescription: "Discrepancy detail line that is intentionally long enough to wrap into multiple lines when formatted at 50 chars per line totaling about six lines"
        ]).call()
    }

    when:
    def result = ec.service.sync().name("trade.importlc.ImportLcValidationServices.validate#Tag77JAggregateLimit")
        .parameters([presentationId: presId]).call()

    then:
    result.valid == false
    result.errorMessage.contains("70-line")
    result.currentLineCount > 70
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :runtime:component:TradeFinance:test --tests "trade.SwiftValidationSpec.Tag 77J*" 2>&1 | tail -30`
Expected: FAIL (service does not exist)

- [ ] **Step 3: Implement validate#Tag77JAggregateLimit service**

Add to `ImportLcValidationServices.xml`:

```xml
<service verb="validate" noun="Tag77JAggregateLimit" authenticate="false">
    <description>Validates that total discrepancy text for a presentation does not exceed 70 lines of 50 characters (PRE-SWV-08).</description>
    <in-parameters>
        <parameter name="presentationId" required="true"/>
    </in-parameters>
    <out-parameters>
        <parameter name="valid" type="Boolean"/>
        <parameter name="currentLineCount" type="Integer"/>
        <parameter name="errorMessage"/>
    </out-parameters>
    <actions>
        <script>ec.artifactExecution.disableAuthz()</script>
        <entity-find entity-name="trade.importlc.PresentationDiscrepancy" list="discList">
            <econdition field-name="presentationId"/>
        </entity-find>
        <script><![CDATA[
            def discText = discList ? discList.collect { "${it.discrepancyCode}: ${it.discrepancyDescription}" }.join("\n") : ""
            def formatResult = ec.service.sync().name("trade.SwiftUtilsServices.format#Value")
                .parameters([value: discText, maxLines: 9999, maxLineLength: 50]).call()
            def formatted = formatResult.formattedValue ?: ""
            currentLineCount = formatted ? formatted.split("\r?\n").length : 0
            if (currentLineCount > 70) {
                valid = false
                errorMessage = "Tag 77J exceeds 70-line limit after formatting (${currentLineCount} lines)"
            } else {
                valid = true
            }
        ]]></script>
    </actions>
</service>
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :runtime:component:TradeFinance:test --tests "trade.SwiftValidationSpec.Tag 77J*" 2>&1 | tail -30`
Expected: PASS

- [ ] **Step 5: Add safety net call to generate#Mt750 and generate#Mt734**

In `SwiftGenerationServices.xml`, in `generate#Mt750` (after line 376, after the existing validation block), add:

```xml
<!-- PRE-SWV-10: Tag 77J aggregate safety net -->
<service-call name="trade.importlc.ImportLcValidationServices.validate#Tag77JAggregateLimit"
    in-map="[presentationId: presentationId]" out-map="tag77jResult"/>
<if condition="tag77jResult.valid == false">
    <return error="true" message="${tag77jResult.errorMessage}"/>
</if>
```

Add the same block in `generate#Mt734` (after line 437, after the existing validation block).

- [ ] **Step 6: Commit**

```bash
git add runtime/component/TradeFinance/service/trade/importlc/ImportLcValidationServices.xml runtime/component/TradeFinance/service/trade/SwiftGenerationServices.xml runtime/component/TradeFinance/src/test/groovy/trade/SwiftValidationSpec.groovy
git commit -m "feat(validation): add Tag 77J 70x50 aggregate limit enforcement (PRE-SWV-08/10)"
```

---

### Task 6: Full Test Suite Verification

**BDD Scenarios:** All (S1-S13)
**BRD Requirements:** All

**User-Facing:** NO

- [ ] **Step 1: Run the complete test suite**

Run: `./gradlew :runtime:component:TradeFinance:test 2>&1 | tail -40`
Expected: All tests PASS. No regressions in existing specs.

- [ ] **Step 2: Review test report for failures**

Check: `runtime/component/TradeFinance/build/reports/tests/test/index.html`
Expected: 0 failures across all specs

- [ ] **Step 3: Final commit with all changes**

If any fixes were needed during verification:

```bash
git add -A
git commit -m "fix: address test regressions from SWIFT 2024 remaining gaps implementation"
```

- [ ] **Step 4: Summary commit log**

```bash
git log --oneline -10
```

Expected commits (newest first):
1. `fix: address test regressions...` (if needed)
2. `feat(validation): add Tag 77J 70x50 aggregate limit enforcement`
3. `feat(swift): add generate#Mt747 service and SECA hooks`
4. `feat(swift): add generate#Mt740 service with NostroReconciliation auto-creation`
5. `feat(swift): add Tags 49G/49H/40E to MT700, Tag 23S to MT707`
6. `feat(entity): add fields for MT700 49G/49H/40E, MT740/747 reimbursement, NostroReconciliation, Tag 23S`
