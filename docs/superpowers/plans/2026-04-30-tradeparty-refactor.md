# TradeParty Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace flat party BIC fields with a structured Party-Role junction pattern across entity, service, data, and view layers.

**Architecture:** TradeParty (base) + TradePartyBank (extension) entities with a TradeInstrumentParty junction keyed by (instrumentId, roleEnumId). All services refactored to read party data from the junction. Flat BIC fields removed from TradeInstrument and ImportLetterOfCredit.

**Tech Stack:** Moqui Framework (XML entities, XML services, Groovy Spock tests)

**Specs:**
- BRD: `docs/superpowers/specs/2026-04-30-tradeparty-refactor-brd.md`
- BDD: `docs/superpowers/specs/2026-04-30-tradeparty-refactor-bdd.md`
- Source Req: `docs/requirements/20260429-MT-special-tags-TradeParty.md`

---

## File Map

| File | Action | Responsibility |
|:---|:---|:---|
| `entity/TradeCommonEntities.xml` | Modify | Add TradePartyBank, TradeInstrumentParty entities. Modify TradeParty, TradeInstrument |
| `entity/ImportLcEntities.xml` | Modify | Remove flat BIC fields from ImportLetterOfCredit. Add availableWithEnumId. Refactor view entity |
| `data/TradeFinanceMasterData.xml` | Modify | Add enumerations, partyTypeEnumId, TradePartyBank records, junction records |
| `data/TradeFinanceUsers.xml` | Modify | Add TradePartyBank and TradeInstrumentParty to security artifact group |
| `service/trade/TradeCommonServices.xml` | Modify | Add party CRUD, assignment, and eligibility services |
| `service/trade/importlc/ImportLcServices.xml` | Modify | Refactor create/update LC to use junction |
| `service/trade/importlc/ImportLcValidationServices.xml` | Modify | Update SWIFT validation to read from junction |
| `service/trade/SwiftGenerationServices.xml` | Modify | Refactor to read party data from junction |
| `service/trade.rest.xml` | Modify | Update REST API for party assignments |
| `src/test/groovy/trade/TradePartySpec.groovy` | Create | New spec for party CRUD and assignment (SC-01 through SC-11) |
| `src/test/groovy/trade/TradePartyLcIntegrationSpec.groovy` | Create | LC integration tests (SC-12 through SC-17) |

---

## Task 1: Entity Schema — TradeParty, TradePartyBank, TradeInstrumentParty

**BDD Scenarios:** SC-01, SC-02 (entity structure prerequisite)
**BRD Requirements:** FR-TP-01, FR-TP-02, FR-TP-03, FR-TP-04, FR-TP-05
**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/entity/TradeCommonEntities.xml`
- Modify: `runtime/component/TradeFinance/data/TradeFinanceMasterData.xml`

- [ ] **Step 1: Modify TradeParty entity**

In `entity/TradeCommonEntities.xml`, replace the TradeParty entity (lines 79-89):

```xml
    <entity entity-name="TradeParty" package="trade">
        <field name="partyId" type="id" is-pk="true"/>
        <field name="partyTypeEnumId" type="id"/> <!-- PARTY_COMMERCIAL or PARTY_BANK -->
        <field name="partyName" type="text-medium"/>
        <field name="accountNumber" type="text-short"/> <!-- IBAN or domestic account. See requirements §accountNumber -->
        <field name="registeredAddress" type="text-long"/> <!-- 4x35 SWIFT format -->
        <field name="kycStatus" type="text-short"/> <!-- Active, Expired -->
        <field name="kycExpiryDate" type="date"/>
        <field name="sanctionsStatus" type="text-short"/> <!-- SANCTION_CLEAR, SANCTION_PENDING, SANCTION_BLOCKED -->
        <field name="countryOfRisk" type="id"/> <!-- ISO Country Code -->
    </entity>
```

Fields removed: `swiftBic` (→ TradePartyBank), `partyRoleEnumId` (→ junction).
Fields added: `partyTypeEnumId`, `accountNumber`.

- [ ] **Step 2: Add TradePartyBank entity**

Add after the TradeParty entity in `entity/TradeCommonEntities.xml`:

```xml
    <entity entity-name="TradePartyBank" package="trade">
        <field name="partyId" type="id" is-pk="true"/>
        <field name="swiftBic" type="text-short"/> <!-- Valid 8/11 char SWIFT BIC -->
        <field name="clearingCode" type="text-short"/> <!-- National routing code for Option C -->
        <field name="hasActiveRMA" type="text-indicator"/> <!-- Y/N -->
        <field name="nostroAccountRef" type="text-medium"/> <!-- Our Nostro account at this bank -->
        <field name="fiLimitAvailable" type="number-decimal"/>
        <field name="fiLimitCurrencyUomId" type="id"/>
        <relationship type="one" related="trade.TradeParty">
            <key-map field-name="partyId"/>
        </relationship>
    </entity>
```

- [ ] **Step 3: Add TradeInstrumentParty junction entity**

Add after TradePartyBank in `entity/TradeCommonEntities.xml`:

```xml
    <entity entity-name="TradeInstrumentParty" package="trade">
        <field name="instrumentId" type="id" is-pk="true"/>
        <field name="roleEnumId" type="id" is-pk="true"/>
        <field name="partyId" type="id"/>
        <relationship type="one" related="trade.TradeInstrument">
            <key-map field-name="instrumentId"/>
        </relationship>
        <relationship type="one" related="trade.TradeParty">
            <key-map field-name="partyId"/>
        </relationship>
    </entity>
```

- [ ] **Step 4: Remove flat party fields from TradeInstrument**

Remove these fields from the TradeInstrument entity (lines 17-18, 23-29):

```xml
<!-- REMOVE these lines -->
<field name="applicantPartyId" type="id"/>
<field name="beneficiaryPartyId" type="id"/>
<field name="reimbursingBankBic" type="text-short"/>
<field name="reimbursingBankName" type="text-long"/>
<field name="adviseThroughBankBic" type="text-short"/>
<field name="adviseThroughBankName" type="text-long"/>
<field name="beneficiaryName" type="text-long"/>
```

Keep: `preAdviceRef`, `senderToReceiverInfo` (these are instrument attributes, not party attributes).

- [ ] **Step 5: Add enumeration seed data**

In `data/TradeFinanceMasterData.xml`, add enumeration types and values (after the existing `AcctgTransType` block):

```xml
    <!-- Party Type Enumerations -->
    <moqui.basic.EnumerationType enumTypeId="TradePartyType" description="Trade Party Type"/>
    <moqui.basic.Enumeration enumId="PARTY_COMMERCIAL" enumTypeId="TradePartyType" description="Commercial Entity"/>
    <moqui.basic.Enumeration enumId="PARTY_BANK" enumTypeId="TradePartyType" description="Financial Institution"/>

    <!-- Party Role Enumerations -->
    <moqui.basic.EnumerationType enumTypeId="TradePartyRole" description="Trade Party Role on Instrument"/>
    <moqui.basic.Enumeration enumId="TP_APPLICANT" enumTypeId="TradePartyRole" description="Applicant" sequenceNum="1"/>
    <moqui.basic.Enumeration enumId="TP_BENEFICIARY" enumTypeId="TradePartyRole" description="Beneficiary" sequenceNum="2"/>
    <moqui.basic.Enumeration enumId="TP_ISSUING_BANK" enumTypeId="TradePartyRole" description="Issuing Bank" sequenceNum="10"/>
    <moqui.basic.Enumeration enumId="TP_APPLICANT_BANK" enumTypeId="TradePartyRole" description="Applicant Bank" sequenceNum="11"/>
    <moqui.basic.Enumeration enumId="TP_ADVISING_BANK" enumTypeId="TradePartyRole" description="Advising Bank" sequenceNum="12"/>
    <moqui.basic.Enumeration enumId="TP_ADVISE_THROUGH_BANK" enumTypeId="TradePartyRole" description="Advise Through Bank" sequenceNum="13"/>
    <moqui.basic.Enumeration enumId="TP_CONFIRMING_BANK" enumTypeId="TradePartyRole" description="Confirming Bank" sequenceNum="14"/>
    <moqui.basic.Enumeration enumId="TP_REIMBURSING_BANK" enumTypeId="TradePartyRole" description="Reimbursing Bank" sequenceNum="15"/>
    <moqui.basic.Enumeration enumId="TP_NEGOTIATING_BANK" enumTypeId="TradePartyRole" description="Negotiating Bank" sequenceNum="16"/>
    <moqui.basic.Enumeration enumId="TP_DRAWEE_BANK" enumTypeId="TradePartyRole" description="Drawee Bank" sequenceNum="17"/>
    <moqui.basic.Enumeration enumId="TP_PRESENTING_BANK" enumTypeId="TradePartyRole" description="Presenting Bank" sequenceNum="20"/>
    <moqui.basic.Enumeration enumId="TP_INTERMEDIARY_BANK" enumTypeId="TradePartyRole" description="Intermediary Bank" sequenceNum="30"/>
    <moqui.basic.Enumeration enumId="TP_SENDERS_CORRESPONDENT" enumTypeId="TradePartyRole" description="Sender Correspondent" sequenceNum="31"/>
    <moqui.basic.Enumeration enumId="TP_RECEIVERS_CORRESPONDENT" enumTypeId="TradePartyRole" description="Receiver Correspondent" sequenceNum="32"/>

    <!-- Available With Enumerations -->
    <moqui.basic.EnumerationType enumTypeId="AvailableWithType" description="Available With Bank Selection"/>
    <moqui.basic.Enumeration enumId="AVAIL_ANY_BANK" enumTypeId="AvailableWithType" description="Any Bank"/>
    <moqui.basic.Enumeration enumId="AVAIL_SPECIFIC_BANK" enumTypeId="AvailableWithType" description="Specific Bank"/>
```

- [ ] **Step 6: Update existing party master data**

Update existing TradeParty records with `partyTypeEnumId`. Add `TradePartyBank` records for bank parties. Add `swiftBic` to bank extension:

```xml
    <!-- Commercial Parties -->
    <trade.TradeParty partyId="ACME_CORP_001" partyTypeEnumId="PARTY_COMMERCIAL" partyName="Acme Corporation Ltd" kycStatus="Active" kycExpiryDate="2027-12-31" sanctionsStatus="SANCTION_CLEAR" countryOfRisk="USA" registeredAddress="123 Acme Way, New York, NY"/>
    <trade.TradeParty partyId="GLOBAL_EXP_002" partyTypeEnumId="PARTY_COMMERCIAL" partyName="Global Exports Inc" kycStatus="Active" kycExpiryDate="2027-06-30" sanctionsStatus="SANCTION_CLEAR" countryOfRisk="GBR" registeredAddress="45 Export St, London, UK"/>
    <trade.TradeParty partyId="RISKY_BIZ_003" partyTypeEnumId="PARTY_COMMERCIAL" partyName="Risky Business Trading" kycStatus="Expired" kycExpiryDate="2024-01-01" sanctionsStatus="SANCTION_PENDING" countryOfRisk="PAN" registeredAddress="99 Shadow Alley, Panama City"/>
    <trade.TradeParty partyId="BANNED_ENTITY_004" partyTypeEnumId="PARTY_COMMERCIAL" partyName="Banned Corp" kycStatus="Active" kycExpiryDate="2028-01-01" sanctionsStatus="SANCTION_BLOCKED" countryOfRisk="NK" registeredAddress="1 Terminal Rd, Pyongyang"/>
    <trade.TradeParty partyId="ORG_ZIZI_CORP" partyTypeEnumId="PARTY_COMMERCIAL" partyName="Zizi Corp" kycStatus="Active" sanctionsStatus="SANCTION_CLEAR" countryOfRisk="VNM"/>

    <!-- Bank Parties -->
    <trade.TradeParty partyId="ISSUING_BANK_001" partyTypeEnumId="PARTY_BANK" partyName="Trade Finance Bank" kycStatus="Active" sanctionsStatus="SANCTION_CLEAR" countryOfRisk="VNM"/>
    <trade.TradePartyBank partyId="ISSUING_BANK_001" swiftBic="TFBKVNVX" hasActiveRMA="Y" nostroAccountRef="NOSTRO-USD-TFB-001"/>

    <trade.TradeParty partyId="ADVISING_BANK_001" partyTypeEnumId="PARTY_BANK" partyName="Overseas Banking Corp" kycStatus="Active" sanctionsStatus="SANCTION_CLEAR" countryOfRisk="SGP"/>
    <trade.TradePartyBank partyId="ADVISING_BANK_001" swiftBic="OBCSGSGX" hasActiveRMA="Y" nostroAccountRef="NOSTRO-USD-OBC-001" fiLimitAvailable="10000000.00" fiLimitCurrencyUomId="USD"/>
```

- [ ] **Step 7: Add junction records for existing sample instruments**

Add TradeInstrumentParty records for LC240001, LC240002, LC240003. Also remove `applicantPartyId` and `beneficiaryPartyId` from TradeInstrument sample data:

```xml
    <!-- Sample instruments (remove applicantPartyId/beneficiaryPartyId from these) -->
    <trade.TradeInstrument instrumentId="LC240001" transactionRef="TF-ACME-100" baseEquivalentAmount="2000000.00" customerFacilityId="FAC-ACME-001" instrumentTypeEnumId="INST_LC" issueDate="2026-04-20"/>

    <!-- Junction records for LC240001 -->
    <trade.TradeInstrumentParty instrumentId="LC240001" roleEnumId="TP_APPLICANT" partyId="ACME_CORP_001"/>
    <trade.TradeInstrumentParty instrumentId="LC240001" roleEnumId="TP_BENEFICIARY" partyId="GLOBAL_EXP_002"/>
    <trade.TradeInstrumentParty instrumentId="LC240001" roleEnumId="TP_ISSUING_BANK" partyId="ISSUING_BANK_001"/>
    <trade.TradeInstrumentParty instrumentId="LC240001" roleEnumId="TP_ADVISING_BANK" partyId="ADVISING_BANK_001"/>
```

Repeat pattern for LC240002 and LC240003.

- [ ] **Step 8: Update security artifact group**

In `data/TradeFinanceUsers.xml`, add `TradePartyBank` to the `TRADE_FINANCE_ENTITIES` group (TradeInstrumentParty is already there):

```xml
    <moqui.security.ArtifactGroupMember artifactGroupId="TRADE_FINANCE_ENTITIES" artifactName="trade.TradePartyBank" artifactTypeEnumId="AT_ENTITY" inheritAuthz="Y"/>
```

- [ ] **Step 9: Commit**

```bash
git add runtime/component/TradeFinance/entity/TradeCommonEntities.xml runtime/component/TradeFinance/data/TradeFinanceMasterData.xml runtime/component/TradeFinance/data/TradeFinanceUsers.xml
git commit -m "feat: add TradePartyBank, TradeInstrumentParty entities and enumerations

- TradeParty: add partyTypeEnumId, accountNumber; remove swiftBic, partyRoleEnumId
- TradePartyBank: new extension entity for bank-specific fields
- TradeInstrumentParty: new junction entity PK=(instrumentId, roleEnumId)
- TradeInstrument: remove flat party BIC fields
- Add TradePartyRole, TradePartyType, AvailableWithType enumerations
- Migrate master data to new structure"
```

---

## Task 2: ImportLetterOfCredit Entity — Remove Flat Fields, Refactor View

**BDD Scenarios:** SC-12, SC-13, SC-17 (entity prerequisites)
**BRD Requirements:** FR-TP-07, FR-TP-09, FR-TP-14
**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/entity/ImportLcEntities.xml`

- [ ] **Step 1: Remove flat BIC/party fields from ImportLetterOfCredit**

In `entity/ImportLcEntities.xml`, remove these fields from the `ImportLetterOfCredit` entity:

```xml
<!-- REMOVE these lines (9, 17-18, 35, 68-74) -->
<field name="beneficiaryPartyId" type="id"/>
<field name="applicantName" type="text-long"/>
<field name="beneficiaryName" type="text-long"/>
<field name="applicantPartyId" type="id"/>
<field name="advisingBankBic" type="text-short"/>
<field name="advisingThroughBankBic" type="text-short"/>
<field name="issuingBankBic" type="text-short"/>
<field name="availableWithBic" type="text-short"/>
<field name="availableWithName" type="text-medium"/>
<field name="draweeBankBic" type="text-short"/>
```

- [ ] **Step 2: Add availableWithEnumId field**

Add in place of the removed BIC addressing block:

```xml
        <!-- Available With selection -->
        <field name="availableWithEnumId" type="id"/> <!-- AVAIL_ANY_BANK or AVAIL_SPECIFIC_BANK -->
```

- [ ] **Step 3: Remove presentingBankBic from TradeDocumentPresentation**

Remove line 122 from the `TradeDocumentPresentation` entity:

```xml
<!-- REMOVE -->
<field name="presentingBankBic" type="text-short"/>
```

Keep `presentingBankRef` — it's a presentation attribute.

- [ ] **Step 4: Refactor ImportLetterOfCreditView**

Replace the view entity (lines 207-242) to join through `TradeInstrumentParty`:

```xml
    <view-entity entity-name="ImportLetterOfCreditView" package="trade.importlc">
        <member-entity entity-alias="lc" entity-name="trade.importlc.ImportLetterOfCredit"/>
        <member-entity entity-alias="inst" entity-name="trade.TradeInstrument" join-from-alias="lc">
            <key-map field-name="instrumentId"/>
        </member-entity>
        <member-entity entity-alias="tx" entity-name="trade.TradeTransaction" join-from-alias="lc" join-optional="true">
            <key-map field-name="instrumentId"/>
        </member-entity>
        <member-entity entity-alias="prio" entity-name="moqui.basic.Enumeration" join-from-alias="tx" join-optional="true">
            <key-map field-name="priorityEnumId" related="enumId"/>
        </member-entity>
        <!-- Applicant via junction -->
        <member-entity entity-alias="app_role" entity-name="trade.TradeInstrumentParty" join-from-alias="lc" join-optional="true">
            <key-map field-name="instrumentId"/>
            <entity-condition><econdition field-name="roleEnumId" value="TP_APPLICANT"/></entity-condition>
        </member-entity>
        <member-entity entity-alias="aparty" entity-name="trade.TradeParty" join-from-alias="app_role" join-optional="true">
            <key-map field-name="partyId"/>
        </member-entity>
        <!-- Beneficiary via junction -->
        <member-entity entity-alias="ben_role" entity-name="trade.TradeInstrumentParty" join-from-alias="lc" join-optional="true">
            <key-map field-name="instrumentId"/>
            <entity-condition><econdition field-name="roleEnumId" value="TP_BENEFICIARY"/></entity-condition>
        </member-entity>
        <member-entity entity-alias="bparty" entity-name="trade.TradeParty" join-from-alias="ben_role" join-optional="true">
            <key-map field-name="partyId"/>
        </member-entity>
        <alias-all entity-alias="lc"/>
        <alias-all entity-alias="inst">
            <exclude field="instrumentId"/>
            <exclude field="businessStateId"/>
            <exclude field="amount"/>
            <exclude field="currencyUomId"/>
            <exclude field="outstandingAmount"/>
            <exclude field="baseEquivalentAmount"/>
            <exclude field="issueDate"/>
            <exclude field="expiryDate"/>
            <exclude field="customerFacilityId"/>
            <exclude field="productEnumId"/>
        </alias-all>
        <alias-all entity-alias="tx"/>
        <alias entity-alias="prio" name="prioritySequence" field="sequenceNum"/>
        <alias entity-alias="aparty" name="applicantPartyName" field="partyName"/>
        <alias entity-alias="bparty" name="beneficiaryPartyName" field="partyName"/>
        <alias entity-alias="app_role" name="applicantPartyId" field="partyId"/>
        <alias entity-alias="ben_role" name="beneficiaryPartyId" field="partyId"/>
    </view-entity>
```

- [ ] **Step 5: Commit**

```bash
git add runtime/component/TradeFinance/entity/ImportLcEntities.xml
git commit -m "feat: remove flat BIC fields from ImportLC, refactor view entity

- Remove 10 flat party fields from ImportLetterOfCredit
- Add availableWithEnumId for explicit Available With choice
- Remove presentingBankBic from TradeDocumentPresentation
- Refactor ImportLetterOfCreditView to join via TradeInstrumentParty"
```

---

## Task 3: Party CRUD, Assignment, and Eligibility Services

**BDD Scenarios:** SC-01 through SC-11
**BRD Requirements:** FR-TP-10, FR-TP-11, FR-TP-12, FR-TP-17
**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/service/trade/TradeCommonServices.xml`
- Test: `runtime/component/TradeFinance/src/test/groovy/trade/TradePartySpec.groovy`

- [ ] **Step 1: Write failing test — create commercial party (SC-01)**

Create `src/test/groovy/trade/TradePartySpec.groovy`:

```groovy
// ABOUTME: Tests for TradeParty CRUD, role assignment, and bank eligibility validation.
// ABOUTME: Covers BDD scenarios SC-01 through SC-11 from the TradeParty refactor BDD spec.
package trade

import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

@Stepwise
class TradePartySpec extends Specification {
    @Shared ExecutionContext ec

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.user.loginUser("trade.admin", "moqui")
        ec.artifactExecution.disableAuthz()
    }

    def cleanupSpec() {
        ec.artifactExecution.enableAuthz()
        ec.destroy()
    }

    // SC-01: Create a commercial trade party
    def "create commercial party with valid data"() {
        when:
        def result = ec.service.sync().name("trade.TradeCommonServices.create#TradeParty").parameters([
            partyTypeEnumId: "PARTY_COMMERCIAL",
            partyName: "Vietnam Textiles JSC",
            registeredAddress: "123 Le Loi\nDistrict 1\nHo Chi Minh City\nVietnam",
            accountNumber: "VN12VCOM01234567890123",
            kycStatus: "Active",
            sanctionsStatus: "SANCTION_CLEAR",
            countryOfRisk: "VNM"
        ]).call()

        then:
        result.partyId != null
        def party = ec.entity.find("trade.TradeParty").condition("partyId", result.partyId).one()
        party.partyTypeEnumId == "PARTY_COMMERCIAL"
        party.partyName == "Vietnam Textiles JSC"
        party.accountNumber == "VN12VCOM01234567890123"
        // No bank extension record
        ec.entity.find("trade.TradePartyBank").condition("partyId", result.partyId).one() == null
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd /Users/me/myprojects/moqui-trade && ./gradlew :runtime:component:TradeFinance:test --tests "trade.TradePartySpec.create commercial party*"
```

Expected: FAIL — service `create#TradeParty` does not exist yet.

- [ ] **Step 3: Implement create#TradeParty service**

Add to `TradeCommonServices.xml`:

```xml
    <service verb="create" noun="TradeParty" authenticate="false">
        <in-parameters>
            <parameter name="partyTypeEnumId" required="true"/>
            <parameter name="partyName" required="true"/>
            <parameter name="registeredAddress"/>
            <parameter name="accountNumber"/>
            <parameter name="kycStatus" default-value="Active"/>
            <parameter name="kycExpiryDate" type="Date"/>
            <parameter name="sanctionsStatus" default-value="SANCTION_CLEAR"/>
            <parameter name="countryOfRisk"/>
            <!-- Bank extension fields (ignored for PARTY_COMMERCIAL) -->
            <parameter name="swiftBic"/>
            <parameter name="clearingCode"/>
            <parameter name="hasActiveRMA"/>
            <parameter name="nostroAccountRef"/>
            <parameter name="fiLimitAvailable" type="BigDecimal"/>
            <parameter name="fiLimitCurrencyUomId"/>
        </in-parameters>
        <out-parameters><parameter name="partyId"/></out-parameters>
        <actions>
            <script>ec.artifactExecution.disableAuthz()</script>
            <!-- SWIFT X charset validation on partyName and registeredAddress -->
            <script><![CDATA[
                def swiftXPattern = ~/^[A-Za-z0-9\/ \-\?\:\(\)\.\,\'\+\n\r]*$/
                if (partyName && !swiftXPattern.matcher(partyName).matches()) {
                    ec.message.addError("partyName contains invalid SWIFT characters")
                }
                if (registeredAddress && !swiftXPattern.matcher(registeredAddress).matches()) {
                    ec.message.addError("registeredAddress contains invalid SWIFT characters")
                }
                if (ec.message.hasError()) return
            ]]></script>

            <service-call name="create#trade.TradeParty" out-map="partyOut"
                in-map="[partyTypeEnumId:partyTypeEnumId, partyName:partyName,
                         registeredAddress:registeredAddress, accountNumber:accountNumber,
                         kycStatus:kycStatus, kycExpiryDate:kycExpiryDate,
                         sanctionsStatus:sanctionsStatus, countryOfRisk:countryOfRisk]"/>
            <set field="partyId" from="partyOut.partyId"/>

            <if condition="partyTypeEnumId == 'PARTY_BANK'">
                <service-call name="create#trade.TradePartyBank"
                    in-map="[partyId:partyId, swiftBic:swiftBic, clearingCode:clearingCode,
                             hasActiveRMA:hasActiveRMA, nostroAccountRef:nostroAccountRef,
                             fiLimitAvailable:fiLimitAvailable, fiLimitCurrencyUomId:fiLimitCurrencyUomId]"/>
            </if>
        </actions>
    </service>
```

- [ ] **Step 4: Run test to verify it passes**

- [ ] **Step 5: Add remaining tests — SC-02 (bank), SC-03 (SWIFT reject), SC-04 through SC-07 (assignment)**

Add to `TradePartySpec.groovy`:

```groovy
    // SC-02: Create bank party with extension
    def "create bank party with extension fields"() {
        when:
        def result = ec.service.sync().name("trade.TradeCommonServices.create#TradeParty").parameters([
            partyTypeEnumId: "PARTY_BANK", partyName: "Citibank London",
            registeredAddress: "25 Canada Square\nLondon E14 5LB",
            kycStatus: "Active", sanctionsStatus: "SANCTION_CLEAR", countryOfRisk: "GBR",
            swiftBic: "CITIGB2L", clearingCode: "185008", hasActiveRMA: "Y",
            nostroAccountRef: "NOSTRO-USD-CITI-001",
            fiLimitAvailable: 10000000.00, fiLimitCurrencyUomId: "USD"
        ]).call()

        then:
        result.partyId != null
        def bank = ec.entity.find("trade.TradePartyBank").condition("partyId", result.partyId).one()
        bank != null
        bank.swiftBic == "CITIGB2L"
        bank.hasActiveRMA == "Y"
    }

    // SC-03: Reject invalid SWIFT characters
    def "reject party with invalid SWIFT characters in partyName"() {
        when:
        def result = ec.service.sync().name("trade.TradeCommonServices.create#TradeParty").parameters([
            partyTypeEnumId: "PARTY_COMMERCIAL",
            partyName: "Acme & Sons Trading @Corp",
            registeredAddress: "123 Main St"
        ]).call()

        then:
        ec.message.hasError()
    }
```

- [ ] **Step 6: Implement assign#InstrumentParty service**

Add to `TradeCommonServices.xml`:

```xml
    <service verb="assign" noun="InstrumentParty" authenticate="false">
        <in-parameters>
            <parameter name="instrumentId" required="true"/>
            <parameter name="roleEnumId" required="true"/>
            <parameter name="partyId" required="true"/>
        </in-parameters>
        <actions>
            <script>ec.artifactExecution.disableAuthz()</script>
            <!-- Validate party exists -->
            <entity-find-one entity-name="trade.TradeParty" value-field="party"/>
            <if condition="!party"><return error="true" message="Party ${partyId} not found"/></if>

            <!-- Validate party type matches role category -->
            <script><![CDATA[
                def bankRoles = ['TP_ISSUING_BANK','TP_APPLICANT_BANK','TP_ADVISING_BANK',
                    'TP_ADVISE_THROUGH_BANK','TP_CONFIRMING_BANK','TP_REIMBURSING_BANK',
                    'TP_NEGOTIATING_BANK','TP_DRAWEE_BANK','TP_PRESENTING_BANK',
                    'TP_INTERMEDIARY_BANK','TP_SENDERS_CORRESPONDENT','TP_RECEIVERS_CORRESPONDENT']
                def isBankRole = bankRoles.contains(roleEnumId)
                if (isBankRole && party.partyTypeEnumId != 'PARTY_BANK') {
                    ec.message.addError("Role ${roleEnumId} requires a Bank party.")
                    return
                }
                if (!isBankRole && party.partyTypeEnumId != 'PARTY_COMMERCIAL') {
                    ec.message.addError("Role ${roleEnumId} requires a Commercial party.")
                    return
                }
            ]]></script>
            <if condition="ec.message.hasError()"><return/></if>

            <!-- Bank eligibility checks (FR-TP-12) -->
            <if condition="party.partyTypeEnumId == 'PARTY_BANK'">
                <entity-find-one entity-name="trade.TradePartyBank" value-field="bankExt">
                    <field-map field-name="partyId"/>
                </entity-find-one>

                <!-- RMA check for Advising Bank -->
                <if condition="roleEnumId == 'TP_ADVISING_BANK' &amp;&amp; bankExt?.hasActiveRMA != 'Y'">
                    <entity-find entity-name="trade.TradeInstrumentParty" list="throughList">
                        <econdition field-name="instrumentId"/>
                        <econdition field-name="roleEnumId" value="TP_ADVISE_THROUGH_BANK"/>
                    </entity-find>
                    <if condition="!throughList">
                        <return error="true" message="Advising Bank requires active RMA. Assign an Advise Through Bank or select a bank with active RMA."/>
                    </if>
                </if>

                <!-- Nostro check for Reimbursing Bank -->
                <if condition="roleEnumId == 'TP_REIMBURSING_BANK' &amp;&amp; !bankExt?.nostroAccountRef">
                    <return error="true" message="Cannot designate as Reimbursing Bank: No active Nostro account found."/>
                </if>

                <!-- FI limit check for Confirming Bank -->
                <if condition="roleEnumId == 'TP_CONFIRMING_BANK'">
                    <entity-find-one entity-name="trade.TradeInstrument" value-field="instrument"/>
                    <set field="instAmount" from="instrument?.amount ?: 0"/>
                    <set field="tolerance" from="0"/>
                    <entity-find-one entity-name="trade.importlc.ImportLetterOfCredit" value-field="lc">
                        <field-map field-name="instrumentId"/>
                    </entity-find-one>
                    <if condition="lc?.tolerancePositive"><set field="tolerance" from="lc.tolerancePositive"/></if>
                    <set field="liability" from="instAmount * (1 + tolerance)"/>
                    <if condition="(bankExt?.fiLimitAvailable ?: 0) &lt; liability">
                        <return error="true" message="Confirming Bank's FI limit (${bankExt?.fiLimitAvailable ?: 0} ${bankExt?.fiLimitCurrencyUomId ?: ''}) is insufficient for instrument liability (${liability} ${instrument?.currencyUomId ?: ''})."/>
                    </if>
                </if>
            </if>

            <!-- Upsert junction record (PK handles uniqueness) -->
            <entity-find-one entity-name="trade.TradeInstrumentParty" value-field="existing"/>
            <if condition="existing">
                <set field="existing.partyId" from="partyId"/>
                <entity-update value-field="existing"/>
            </if>
            <else>
                <service-call name="create#trade.TradeInstrumentParty"
                    in-map="[instrumentId:instrumentId, roleEnumId:roleEnumId, partyId:partyId]"/>
            </else>
        </actions>
    </service>
```

- [ ] **Step 7: Add assignment + eligibility tests (SC-04 through SC-11)**

Add to `TradePartySpec.groovy` — tests for: multi-role assignment (SC-04, SC-05), duplicate role upsert (SC-06), type mismatch rejection (SC-07), RMA check (SC-08), RMA with advise-through (SC-09), Nostro check (SC-10), FI limit check (SC-11). Each test follows Given-When-Then from the BDD spec.

- [ ] **Step 8: Run all tests, verify pass**

```bash
cd /Users/me/myprojects/moqui-trade && ./gradlew :runtime:component:TradeFinance:test --tests "trade.TradePartySpec"
```

- [ ] **Step 9: Commit**

```bash
git add runtime/component/TradeFinance/service/trade/TradeCommonServices.xml runtime/component/TradeFinance/src/test/groovy/trade/TradePartySpec.groovy
git commit -m "feat: add party CRUD, assignment, and eligibility services

- create#TradeParty with SWIFT charset validation and bank extension
- assign#InstrumentParty with type enforcement and bank eligibility
- RMA, Nostro, FI limit validation per FR-TP-12
- Tests: SC-01 through SC-11"
```

---

## Task 4: Refactor LC Services to Use Junction

**BDD Scenarios:** SC-12, SC-13, SC-14, SC-15, SC-16
**BRD Requirements:** FR-TP-09, FR-TP-13
**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/service/trade/importlc/ImportLcServices.xml`
- Test: `runtime/component/TradeFinance/src/test/groovy/trade/TradePartyLcIntegrationSpec.groovy`

- [ ] **Step 1: Write failing test — create LC with parties (SC-12)**

Create `src/test/groovy/trade/TradePartyLcIntegrationSpec.groovy`:

```groovy
// ABOUTME: Integration tests for LC services with the party-role junction pattern.
// ABOUTME: Covers BDD scenarios SC-12 through SC-17 from the TradeParty refactor BDD spec.
package trade

import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

@Stepwise
class TradePartyLcIntegrationSpec extends Specification {
    @Shared ExecutionContext ec

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.user.loginUser("trade.admin", "moqui")
        ec.artifactExecution.disableAuthz()
    }

    def cleanupSpec() {
        ec.artifactExecution.enableAuthz()
        ec.destroy()
    }

    // SC-12: Create LC with party role assignments
    def "create LC assigns parties via junction"() {
        when:
        def result = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            lcAmount: 100000.00, lcCurrencyUomId: "USD", expiryDate: "2027-12-31",
            customerFacilityId: "FAC-ACME-001",
            parties: [
                [roleEnumId: "TP_APPLICANT", partyId: "ACME_CORP_001"],
                [roleEnumId: "TP_BENEFICIARY", partyId: "GLOBAL_EXP_002"],
                [roleEnumId: "TP_ADVISING_BANK", partyId: "ADVISING_BANK_001"]
            ]
        ]).call()

        then:
        result.instrumentId != null
        def junctions = ec.entity.find("trade.TradeInstrumentParty")
            .condition("instrumentId", result.instrumentId).list()
        junctions.size() == 3
    }
}
```

- [ ] **Step 2: Run test — verify failure**

- [ ] **Step 3: Refactor create#ImportLetterOfCredit to accept parties**

In `ImportLcServices.xml`, add a `parties` parameter and loop to assign via junction after creating the instrument:

```xml
        <in-parameters>
            <!-- ...existing params... -->
            <parameter name="parties" type="List">
                <parameter name="party" type="Map">
                    <parameter name="roleEnumId" required="true"/>
                    <parameter name="partyId" required="true"/>
                </parameter>
            </parameter>
            <parameter name="availableWithEnumId"/>
        </in-parameters>
```

After creating ImportLetterOfCredit, add junction assignment loop:

```xml
            <!-- Assign parties via junction -->
            <iterate list="parties" entry="partyAssignment">
                <service-call name="trade.TradeCommonServices.assign#InstrumentParty"
                    in-map="[instrumentId:instrumentId, roleEnumId:partyAssignment.roleEnumId, partyId:partyAssignment.partyId]"/>
            </iterate>
```

Also remove the `applicantPartyId` reference from the create call (line 68).

- [ ] **Step 4: Run test — verify pass**

- [ ] **Step 5: Add Available With tests (SC-13, SC-14)**

```groovy
    // SC-13: ANY BANK selection
    def "select ANY BANK for Available With"() {
        when:
        def result = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            lcAmount: 50000.00, lcCurrencyUomId: "USD", expiryDate: "2027-12-31",
            customerFacilityId: "FAC-ACME-001",
            availableWithEnumId: "AVAIL_ANY_BANK",
            parties: [[roleEnumId: "TP_APPLICANT", partyId: "ACME_CORP_001"],
                       [roleEnumId: "TP_BENEFICIARY", partyId: "GLOBAL_EXP_002"]]
        ]).call()

        then:
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit")
            .condition("instrumentId", result.instrumentId).one()
        lc.availableWithEnumId == "AVAIL_ANY_BANK"
        // No negotiating bank junction
        def negBank = ec.entity.find("trade.TradeInstrumentParty")
            .condition("instrumentId", result.instrumentId)
            .condition("roleEnumId", "TP_NEGOTIATING_BANK").one()
        negBank == null
    }
```

- [ ] **Step 6: Add mandatory role validation test (SC-15)**

- [ ] **Step 7: Run all tests, verify pass**

- [ ] **Step 8: Commit**

```bash
git add runtime/component/TradeFinance/service/trade/importlc/ImportLcServices.xml runtime/component/TradeFinance/src/test/groovy/trade/TradePartyLcIntegrationSpec.groovy
git commit -m "feat: refactor LC services to use party-role junction

- create#ImportLetterOfCredit accepts parties array
- availableWithEnumId for explicit ANY BANK vs specific bank
- Junction-based party assignment replaces flat BIC fields
- Tests: SC-12 through SC-16"
```

---

## Task 5: Refactor SWIFT Generation Services

**BDD Scenarios:** SC-17
**BRD Requirements:** FR-TP-06, FR-TP-16
**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/service/trade/SwiftGenerationServices.xml`
- Modify: `runtime/component/TradeFinance/service/trade/importlc/ImportLcValidationServices.xml`
- Test: `runtime/component/TradeFinance/src/test/groovy/trade/SwiftGenerationSpec.groovy`

- [ ] **Step 1: Write failing test (SC-17) for MT700 tag generation**

In `TradePartyLcIntegrationSpec.groovy` (or `SwiftGenerationSpec.groovy`), add test to verify `generate#Mt700` correctly resolves parties via junction and formats tags (e.g. 50, 59, 41a, 42a).

- [ ] **Step 2: Update ImportLcValidationServices**

In `ImportLcValidationServices.xml`, remove flat field validation (beneficiaryName, advisingBankBic). Replace with junction-based checks:

```xml
        <!-- Replace flat validations with junction lookup -->
        <entity-find entity-name="trade.TradeInstrumentParty" list="parties">
            <econdition field-name="instrumentId" from="lc.instrumentId"/>
        </entity-find>
        <script><![CDATA[
            boolean hasApplicant = false
            boolean hasBeneficiary = false
            for (def p : parties) {
                if (p.roleEnumId == 'TP_APPLICANT') hasApplicant = true
                if (p.roleEnumId == 'TP_BENEFICIARY') hasBeneficiary = true
            }
            if (!hasApplicant) ec.message.addError("Applicant party is required")
            if (!hasBeneficiary) ec.message.addError("Beneficiary party is required")
        ]]></script>
```

- [ ] **Step 3: Refactor SwiftGenerationServices.xml (MT700)**

Change `generate#Mt700` (lines 56+):
1. Look up parties via `TradeInstrumentParty` -> `TradeParty` -> `TradePartyBank`.
2. Format Tag 50 (Applicant) using `partyName` and `registeredAddress`. Include `accountNumber` ONLY if format requirements allow it.
3. Format Tag 59 (Beneficiary) similarly. Account number is allowed in 59.
4. Replace `lc.advisingBankBic` with lookup for `TP_ADVISING_BANK` swiftBic.
5. Format Tag 41a using `lc.availableWithEnumId` (if `AVAIL_ANY_BANK` -> "ANY BANK", else lookup `TP_NEGOTIATING_BANK` swiftBic/name).
6. Update Tag 42a using `TP_DRAWEE_BANK`.

- [ ] **Step 4: Refactor SwiftGenerationServices.xml (MT730, MT750, MT752)**

Update `setReceiverBic` logic in other services to resolve `TP_ADVISING_BANK` or `TP_PRESENTING_BANK` instead of flat fields.

- [ ] **Step 5: Run SWIFT specs, verify pass**

- [ ] **Step 6: Commit**

```bash
git add runtime/component/TradeFinance/service/trade/SwiftGenerationServices.xml runtime/component/TradeFinance/service/trade/importlc/ImportLcValidationServices.xml
git commit -m "feat: refactor SWIFT generation to use party junction

- MT700/MT730/MT750 resolve parties via TradeInstrumentParty
- Tag 50/59 formatted with partyName, address, accountNumber
- Tag 41a supports AVAIL_ANY_BANK logic
- Tag 42a resolved via TP_DRAWEE_BANK
- Tests: SC-17 passing"
```

---

## Task 6: Refactor Accounting Services

**BRD Requirements:** FR-TP-16
**User-Facing:** NO

**Files:**
- Modify: `runtime/component/TradeFinance/service/trade/TradeAccountingServices.xml`

- [ ] **Step 1: Refactor TradeAccountingServices**

In `TradeAccountingServices.xml`, remove reference to `instrument.applicantPartyId` (line 31). Look it up via the junction:

```xml
            <entity-find-one entity-name="trade.TradeInstrumentParty" value-field="appParty">
                <field-map field-name="instrumentId" from="instrument.instrumentId"/>
                <field-map field-name="roleEnumId" value="TP_APPLICANT"/>
            </entity-find-one>
            
            <service-call name="mantle.ledger.LedgerServices.post#AcctgTrans"
                          in-map="[acctgTransTypeEnumId: 'AttInternal',
                                   organizationPartyId: (appParty?.partyId ?: 'InternalOrganization'), ..."/>
```

- [ ] **Step 2: Commit**

```bash
git add runtime/component/TradeFinance/service/trade/TradeAccountingServices.xml
git commit -m "fix: resolve applicant via junction in accounting services"
```

---

## Verification Plan

### Automated Tests
1. Run `TradePartySpec`: `./gradlew :runtime:component:TradeFinance:test --tests "trade.TradePartySpec"`
2. Run `TradePartyLcIntegrationSpec`: `./gradlew :runtime:component:TradeFinance:test --tests "trade.TradePartyLcIntegrationSpec"`
3. Run all tests to ensure no regressions: `./gradlew :runtime:component:TradeFinance:test`

### Manual Verification
1. N/A for this phase (Backend schema/services only). UI mapping will follow in a separate plan.
