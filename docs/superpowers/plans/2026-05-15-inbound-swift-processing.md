# Inbound SWIFT Processing Engine — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a secure inbound SWIFT message ingestion, correlation, and Trade Inbox system for Import LC lifecycle automation.

**Architecture:** Directory-polling + manual-upload dual-channel ingestion → Prowide Core parsing → SHA-256 dedup → Tag 21 correlation → Trade Inbox operator UI → message-type-specific SECA triggers. Flag-based design (no new LC statuses). Future-proof for SAA/JMS gateway swap.

**Tech Stack:** Moqui Framework (XML entities/services/SECAs), Prowide Core (`pw-swift-core`), Spock (Groovy tests)

**Source BRD:** `docs/superpowers/specs/2026-05-15-inbound-swift-processing-brd.md`
**Source BDD:** `docs/superpowers/specs/2026-05-15-inbound-swift-processing-bdd.md`

---

## File Structure

### New Files

| File | Responsibility |
|:---|:---|
| `entity/InboundSwiftEntities.xml` | `InboundSwiftRaw`, `TradeInboxItem` entities + view entities + seed data |
| `service/trade/swift/InboundSwiftServices.xml` | Ingestion: `poll#InboundDirectory`, `upload#SwiftFile`, `ingest#SwiftMessage` |
| `service/trade/swift/InboundCorrelationServices.xml` | Correlation: `correlate#SwiftMessage`, `link#OrphanMessage` |
| `service/trade/swift/InboundActionServices.xml` | Actions: `acknowledge#Mt730`, `processConsent#Mt799`, `spawnPresentation#Mt750`, `spawnPresentation#Mt754`, `matchReimbursement#Mt742` |
| `service/InboundSwift.secas.xml` | SECA triggers for inbound auto-spawn and auto-match |
| `src/test/groovy/trade/InboundSwiftSpec.groovy` | Spock tests for ingestion, dedup, correlation |
| `src/test/groovy/trade/InboundActionSpec.groovy` | Spock tests for MT-specific business actions |

### Modified Files

| File | Change |
|:---|:---|
| `entity/ImportLcEntities.xml` | Add advised flag fields to LC + Amendment. Add `messageDirection` to `SwiftMessage`. Add `sourceChannel` to `TradeDocumentPresentation`. |
| `service/ImportLc.secas.xml` | Add `sourceChannel != INBOUND` condition to `validate#Presentation` SECA |
| `service/trade/TradeCommonServices.xml` | Add `sourceChannel` bypass to inline `validate#Presentation` call in `create#Presentation` (line 50) |
| `data/TradeFinanceSeedData.xml` | Inbox/correlation status enumerations |
| `src/test/groovy/trade/TradeFinanceMoquiSuite.groovy` | Register new spec classes |

---

## Task Overview

| Task | Component | BDD Scenarios | Depends On |
|:---|:---|:---|:---|
| 1 | [IN_PROGRESS] Entity definitions + seed data | — (foundation) | — |
| 2 | Ingestion services (poll + upload + dedup) | ING-01..06 | Task 1 |
| 3 | Correlation engine (Tag 21 + orphan) | COR-01..04 | Task 2 |
| 4 | MT 730 acknowledge (flag-based) | 730-01, 730-02 | Task 3 |
| 5 | MT 799 amendment consent (reuses accept/reject#Amendment) | 799-01..03 | Task 3 |
| 6 | MT 750 discrepant presentation auto-spawn | 750-01, 750-02 | Task 3 |
| 7 | MT 754 clean presentation + MT 742 reimbursement | 754-01, 742-01..02 | Task 6 |
| 8 | Discrepancy resolution Maker/Checker | DSC-01..04 | Task 6 |
| 9 | SECA wiring + integration test | All | Tasks 4-8 |

> **Self-Review Notes:**
> - **BDD-INB-TIX-01/02** (Trade Inbox UI badge + MT 999 security banner) are **UI-only** scenarios. They require a frontend task (out of scope for this backend plan). The backend provides the data — `TradeInboxItemView` query for badge count and `securityWarningFlag` field for MT 999.
> - **BDD-INB-DSC-04** (UCP 600 deadline monitoring) requires a scheduled batch job (`monitor#PresentationDeadlines`). This is deferred to a follow-up plan to keep scope focused.
> - Test setup blocks use `// ... create LC ...` shorthand. The executing agent MUST reference `EndToEndImportLcSpec.groovy` for the full entity creation pattern.
>
> **Investigation Findings (verified against codebase):**
> - **Amendment merge: REUSE existing services.** `accept#Amendment` (ImportLcServices.xml:680) already handles `BENE_ACCEPTED` + `AMEND_COMMITTED` + calls `merge#AmendmentDeltas` (line 699). Task 5's `commit#Amendment` is removed — call `accept#Amendment` instead.
> - **Dual validation on presentations:** `create#Presentation` (TradeCommonServices.xml:50) calls `validate#Presentation` **inline** in addition to the SECA (ImportLc.secas.xml:28). Both need the `sourceChannel` bypass. Task 6 updated to modify both.
> - **Attachment entity:** `moqui.resource.DbResourceFile` is not currently used for presentation attachments. Task 8's upload gate pattern needs implementation-time design — the executing agent should check how existing file uploads work in the frontend before choosing the entity.

---

## Detailed Tasks

### Task 1: Entity Definitions + Seed Data

**BDD Scenarios:** Foundation for all scenarios (no direct BDD, enables BDD-INB-ING-01..06, BDD-INB-COR-01..04)
**BRD Requirements:** §2.1 InboundSwiftRaw, §4.1 TradeInboxItem, §7.1 entity gaps
**User-Facing:** NO

**Files:**
- Create: `entity/InboundSwiftEntities.xml`
- Modify: `entity/ImportLcEntities.xml` (add advised flags + messageDirection)
- Modify: `data/TradeFinanceSeedData.xml` (add enumerations)

- [ ] **Step 1: Create InboundSwiftEntities.xml with InboundSwiftRaw entity**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<entities xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:noNamespaceSchemaLocation="http://moqui.org/xsd/entity-definition-3.0.xsd">

    <!-- ABOUTME: Entities for inbound SWIFT message processing pipeline. -->
    <!-- ABOUTME: Handles raw message audit, parsing, correlation, and Trade Inbox workflow. -->

    <entity entity-name="InboundSwiftRaw" package="trade.swift" short-alias="InboundSwiftRaw"
            comment="Immutable audit record for every inbound SWIFT message. Stores raw content with SHA-256 hash for deduplication and tamper-proof audit trail.">
        <field name="rawMessageId" type="id" is-pk="true"/>
        <field name="contentHash" type="text-medium" not-null="true"
               comment="SHA-256 hex digest of rawContent. Unique constraint prevents duplicate ingestion."/>
        <field name="swiftHeaderKey" type="text-medium"
               comment="Composite key from SWIFT headers: senderBIC|messageType|tag20|date. Secondary dedup check."/>
        <field name="messageType" type="text-short"
               comment="SWIFT message type parsed from Block 2 (e.g., 730, 799, 750, 754, 742, 999)."/>
        <field name="rawContent" type="text-very-long" not-null="true"
               comment="Original SWIFT message text. Immutable after creation — no update service exposed."/>
        <field name="sourceChannel" type="id" not-null="true"
               comment="Ingestion channel: SRC_DIRECTORY_POLL or SRC_MANUAL_UPLOAD."/>
        <field name="sourceFileName" type="text-medium"
               comment="Original filename for audit trail."/>
        <field name="uploadedByUserId" type="id"
               comment="User who uploaded (manual channel only)."/>
        <field name="receivedTimestamp" type="date-time" not-null="true"/>
        <field name="parseStatusEnumId" type="id" not-null="true"
               comment="PARSE_SUCCESS or PARSE_FAILED."/>
        <field name="parseErrorText" type="text-long"
               comment="Error details if parsing failed."/>
        <index name="IDX_INBOUND_HASH" unique="true">
            <index-field name="contentHash"/>
        </index>
        <index name="IDX_INBOUND_HEADER_KEY" unique="true">
            <index-field name="swiftHeaderKey"/>
        </index>
        <relationship type="one-nofk" related="moqui.security.UserAccount" fk-name="FK_ISR_USER">
            <key-map field-name="uploadedByUserId" related="userId"/>
        </relationship>
    </entity>

    <entity entity-name="TradeInboxItem" package="trade.swift" short-alias="TradeInboxItem"
            comment="Operator-facing inbox record for each parsed inbound SWIFT message. Pre-linked to parent LC via Tag 21 correlation. Supports Maker/Checker workflow actions.">
        <field name="inboxItemId" type="id" is-pk="true"/>
        <field name="rawMessageId" type="id" not-null="true"/>
        <field name="instrumentId" type="id" comment="FK to TradeInstrument. Null if orphaned."/>
        <field name="amendmentId" type="id" comment="FK to ImportLcAmendment. Set by sub-correlation."/>
        <field name="messageType" type="text-short" not-null="true"/>
        <field name="senderBic" type="text-short" not-null="true"/>
        <field name="senderReference" type="text-medium" comment="Tag 20."/>
        <field name="relatedReference" type="text-medium" comment="Tag 21 — correlation key."/>
        <field name="narrativeText" type="text-very-long" comment="Tag 79 (MT 799/999) or Tag 77J (MT 750)."/>
        <field name="claimAmount" type="number-decimal" comment="Tag 32B amount."/>
        <field name="claimCurrency" type="id" comment="Tag 32B currency."/>
        <field name="receivedTimestamp" type="date-time" not-null="true"/>
        <field name="inboxStatusEnumId" type="id" not-null="true"/>
        <field name="correlationStatusEnumId" type="id" not-null="true"/>
        <field name="orphanReason" type="text-medium"/>
        <field name="processedByUserId" type="id"/>
        <field name="processedTimestamp" type="date-time"/>
        <field name="actionTaken" type="text-medium"/>
        <field name="securityWarningFlag" type="text-indicator"/>
        <relationship type="one" related="trade.swift.InboundSwiftRaw" fk-name="FK_TII_RAW">
            <key-map field-name="rawMessageId"/>
        </relationship>
        <relationship type="one-nofk" related="trade.TradeInstrument" fk-name="FK_TII_INST">
            <key-map field-name="instrumentId"/>
        </relationship>
        <relationship type="one-nofk" related="trade.importlc.ImportLcAmendment" fk-name="FK_TII_AMD">
            <key-map field-name="amendmentId"/>
        </relationship>
    </entity>

    <view-entity entity-name="TradeInboxItemView" package="trade.swift"
                 comment="Joins TradeInboxItem with InboundSwiftRaw and TradeInstrument for dashboard queries.">
        <member-entity entity-alias="TII" entity-name="trade.swift.TradeInboxItem"/>
        <member-entity entity-alias="ISR" entity-name="trade.swift.InboundSwiftRaw" join-from-alias="TII">
            <key-map field-name="rawMessageId"/>
        </member-entity>
        <member-entity entity-alias="TI" entity-name="trade.TradeInstrument" join-from-alias="TII" join-optional="true">
            <key-map field-name="instrumentId"/>
        </member-entity>
        <alias-all entity-alias="TII"/>
        <alias entity-alias="TI" name="instrumentRef" field="instrumentRef"/>
        <alias entity-alias="ISR" name="sourceChannel" field="sourceChannel"/>
        <alias entity-alias="ISR" name="sourceFileName" field="sourceFileName"/>
    </view-entity>

</entities>
```

- [ ] **Step 2: Add advised flag fields to ImportLcEntities.xml**

Add to `ImportLetterOfCredit` entity (after existing fields):
```xml
<field name="isAdvised" type="text-indicator" comment="Y when MT 730 confirms issuance delivery to Beneficiary."/>
<field name="advisedDate" type="date" comment="Date from inbound MT 730 Tag 30."/>
<field name="advisingBankReference" type="text-medium" comment="Advising Bank ref from MT 730 Tag 20."/>
```

Add to `ImportLcAmendment` entity (after existing fields):
```xml
<field name="isAdvisedToBeneficiary" type="text-indicator" comment="Y when MT 730 confirms amendment delivery."/>
<field name="advisedToBeneficiaryDate" type="date" comment="Date from MT 730 Tag 30 for amendment."/>
<field name="consentSwiftRef" type="text-medium" comment="MT 799 Tag 20 — audit ref for consent decision."/>
```

Add to `SwiftMessage` entity (after existing fields):
```xml
<field name="messageDirection" type="text-short" default="'OUTBOUND'"
       comment="OUTBOUND (generated) or INBOUND (received). Existing records default to OUTBOUND."/>
```

Add to `TradeDocumentPresentation` entity (after existing fields):
```xml
<field name="sourceChannel" type="id"
       comment="INBOUND if auto-created from inbound SWIFT. Used to bypass validation SECA for MT 750/754."/>
```

- [ ] **Step 3: Add seed data enumerations to TradeFinanceSeedData.xml**

```xml
<!-- Inbound SWIFT: Inbox Status -->
<moqui.basic.Enumeration enumId="INBOX_UNREAD" enumTypeId="InboxStatus" description="Unread"/>
<moqui.basic.Enumeration enumId="INBOX_IN_PROGRESS" enumTypeId="InboxStatus" description="In Progress"/>
<moqui.basic.Enumeration enumId="INBOX_PROCESSED" enumTypeId="InboxStatus" description="Processed"/>
<moqui.basic.Enumeration enumId="INBOX_ORPHANED" enumTypeId="InboxStatus" description="Orphaned"/>

<!-- Inbound SWIFT: Correlation Status -->
<moqui.basic.Enumeration enumId="CORR_AUTO_MATCH" enumTypeId="CorrelationStatus" description="Auto-Matched"/>
<moqui.basic.Enumeration enumId="CORR_MANUAL_MATCH" enumTypeId="CorrelationStatus" description="Manual Match"/>
<moqui.basic.Enumeration enumId="CORR_ORPHANED" enumTypeId="CorrelationStatus" description="Orphaned"/>

<!-- Inbound SWIFT: Source Channel -->
<moqui.basic.Enumeration enumId="SRC_DIRECTORY_POLL" enumTypeId="SourceChannel" description="Directory Polling"/>
<moqui.basic.Enumeration enumId="SRC_MANUAL_UPLOAD" enumTypeId="SourceChannel" description="Manual Upload"/>

<!-- Inbound SWIFT: Parse Status -->
<moqui.basic.Enumeration enumId="PARSE_SUCCESS" enumTypeId="ParseStatus" description="Parse Success"/>
<moqui.basic.Enumeration enumId="PARSE_FAILED" enumTypeId="ParseStatus" description="Parse Failed"/>
```

- [ ] **Step 4: Build and verify entities load**

Run: `./gradlew :runtime:component:TradeFinance:test --tests "trade.CommonEntitiesSpec" -x jar`
Expected: PASS — new entities recognized by Moqui entity facade.

- [ ] **Step 5: Commit**

```bash
git add entity/InboundSwiftEntities.xml entity/ImportLcEntities.xml data/TradeFinanceSeedData.xml
git commit -m "feat(inbound): add InboundSwiftRaw, TradeInboxItem entities and seed data"
```

---

### Task 2: Ingestion Services (Poll + Upload + Dedup)

**BDD Scenarios:** BDD-INB-ING-01 (poll), BDD-INB-ING-02 (upload), BDD-INB-ING-03 (hash dedup), BDD-INB-ING-04 (header dedup), BDD-INB-ING-05 (corrupt file), BDD-INB-ING-06 (batch split)
**BRD Requirements:** §1.1 Directory Polling, §1.2 Manual Upload, §2.1 InboundSwiftRaw, DDP-01..03, SEC-01..03
**User-Facing:** NO

**Files:**
- Create: `service/trade/swift/InboundSwiftServices.xml`
- Test: `src/test/groovy/trade/InboundSwiftSpec.groovy`

- [ ] **Step 1: Write failing test — ingest creates InboundSwiftRaw with hash**

```groovy
// BDD-INB-ING-02: Manual upload with SHA-256 audit stamp
def "ingest#SwiftMessage creates InboundSwiftRaw with SHA-256 hash"() {
    when:
    def result = ec.service.sync().name("trade.swift.InboundSwiftServices.ingest#SwiftMessage")
        .parameters([
            rawContent: sampleMt730Content,
            sourceChannel: "SRC_MANUAL_UPLOAD",
            sourceFileName: "MT730_test.txt"
        ]).call()
    then:
    result.rawMessageId != null
    def raw = ec.entity.find("trade.swift.InboundSwiftRaw")
        .condition("rawMessageId", result.rawMessageId).one()
    raw.contentHash != null
    raw.contentHash.length() == 64 // SHA-256 hex
    raw.parseStatusEnumId == "PARSE_SUCCESS"
    raw.messageType == "730"
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :runtime:component:TradeFinance:test --tests "trade.InboundSwiftSpec.ingest*" -x jar`
Expected: FAIL — service not defined.

- [ ] **Step 3: Implement ingest#SwiftMessage service**

```xml
<service verb="ingest" noun="SwiftMessage" authenticate="false"
         comment="Core ingestion: hash, dedup-check, parse via Prowide, persist InboundSwiftRaw.">
    <in-parameters>
        <parameter name="rawContent" type="String" required="true"/>
        <parameter name="sourceChannel" required="true"/>
        <parameter name="sourceFileName"/>
    </in-parameters>
    <out-parameters>
        <parameter name="rawMessageId"/>
        <parameter name="isDuplicate" type="Boolean"/>
        <parameter name="parseSuccess" type="Boolean"/>
    </out-parameters>
    <actions>
        <script>ec.artifactExecution.disableAuthz()</script>
        <script><![CDATA[
            import java.security.MessageDigest

            // SHA-256 hash
            def digest = MessageDigest.getInstance("SHA-256")
            def hashBytes = digest.digest(rawContent.getBytes("UTF-8"))
            context.contentHash = hashBytes.collect { String.format("%02x", it) }.join()

            // Dedup check: content hash
            def existing = ec.entity.find("trade.swift.InboundSwiftRaw")
                .condition("contentHash", context.contentHash).one()
            if (existing) {
                context.isDuplicate = true
                context.rawMessageId = existing.rawMessageId
                context.parseSuccess = false
                return
            }

            // Parse via Prowide Core
            try {
                def sm = com.prowidesoftware.swift.model.SwiftMessage.parse(rawContent)
                context.messageType = sm.getType()
                context.tag20 = sm.getBlock4()?.getTagValue("20")
                context.tag21 = sm.getBlock4()?.getTagValue("21")
                context.senderBic = sm.getBlock1()?.getLogicalTerminal()?.substring(0, 8)

                // Build composite header key for secondary dedup
                def dateStr = sm.getBlock2()?.getReceiverInputTime() ?: ""
                context.swiftHeaderKey = "${context.senderBic}|${context.messageType}|${context.tag20 ?: ''}|${dateStr}"

                // Dedup check: header key
                if (context.swiftHeaderKey) {
                    def existingByKey = ec.entity.find("trade.swift.InboundSwiftRaw")
                        .condition("swiftHeaderKey", context.swiftHeaderKey).one()
                    if (existingByKey) {
                        context.isDuplicate = true
                        context.rawMessageId = existingByKey.rawMessageId
                        context.parseSuccess = false
                        return
                    }
                }

                context.parseSuccess = true
            } catch (Exception e) {
                context.parseSuccess = false
                context.parseErrorText = e.getMessage()
            }

            // Persist
            def rawOut = ec.service.sync().name("create#trade.swift.InboundSwiftRaw")
                .parameters([
                    contentHash: context.contentHash,
                    swiftHeaderKey: context.swiftHeaderKey,
                    messageType: context.messageType,
                    rawContent: rawContent,
                    sourceChannel: sourceChannel,
                    sourceFileName: sourceFileName,
                    uploadedByUserId: ec.user?.userId,
                    receivedTimestamp: ec.user.nowTimestamp,
                    parseStatusEnumId: context.parseSuccess ? "PARSE_SUCCESS" : "PARSE_FAILED",
                    parseErrorText: context.parseErrorText
                ]).call()
            context.rawMessageId = rawOut.rawMessageId
            context.isDuplicate = false
        ]]></script>
    </actions>
</service>
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :runtime:component:TradeFinance:test --tests "trade.InboundSwiftSpec.ingest*" -x jar`
Expected: PASS

- [ ] **Step 5: Write failing test — duplicate rejection**

```groovy
// BDD-INB-ING-03: Duplicate detected by content hash
def "ingest rejects duplicate by content hash"() {
    when: "first ingestion"
    def first = ec.service.sync().name("trade.swift.InboundSwiftServices.ingest#SwiftMessage")
        .parameters([rawContent: sampleMt730Content, sourceChannel: "SRC_MANUAL_UPLOAD"]).call()
    and: "second ingestion with same content"
    def second = ec.service.sync().name("trade.swift.InboundSwiftServices.ingest#SwiftMessage")
        .parameters([rawContent: sampleMt730Content, sourceChannel: "SRC_MANUAL_UPLOAD"]).call()
    then:
    first.isDuplicate == false
    second.isDuplicate == true
    second.rawMessageId == first.rawMessageId
}
```

- [ ] **Step 6: Run test — should pass (dedup already implemented in Step 3)**

- [ ] **Step 7: Implement poll#InboundDirectory service**

```xml
<service verb="poll" noun="InboundDirectory" authenticate="false"
         comment="Scheduled service: scans inbound directory, reads files, calls ingest, moves to archive.">
    <actions>
        <script>ec.artifactExecution.disableAuthz()</script>
        <entity-find-one entity-name="trade.common.TradeConfig" value-field="inboundDirConfig">
            <field-map field-name="configKey" value="SWIFT_INBOUND_DIR"/>
        </entity-find-one>
        <entity-find-one entity-name="trade.common.TradeConfig" value-field="archiveDirConfig">
            <field-map field-name="configKey" value="SWIFT_ARCHIVE_DIR"/>
        </entity-find-one>
        <set field="inboundDir" from="inboundDirConfig?.configValue ?: 'runtime/swift/inbound'"/>
        <set field="archiveDir" from="archiveDirConfig?.configValue ?: 'runtime/swift/archive'"/>
        <script><![CDATA[
            def inDir = new File(inboundDir)
            if (!inDir.exists()) { ec.logger.warn("Inbound dir not found: ${inboundDir}"); return }

            def processedDir = new File("${archiveDir}/processed")
            def errorDir = new File("${archiveDir}/error")
            def duplicateDir = new File("${archiveDir}/duplicate")
            [processedDir, errorDir, duplicateDir].each { if (!it.exists()) it.mkdirs() }

            inDir.listFiles({ f -> f.name.endsWith(".txt") } as FileFilter)?.each { file ->
                // Write-complete check: file size stable for 2 seconds
                long size1 = file.length()
                Thread.sleep(2000)
                if (file.length() != size1) {
                    ec.logger.info("Skipping ${file.name} — still being written")
                    return
                }

                def content = file.text
                // Support batch files: split on {1: block starts
                def messages = content.contains("{1:") ?
                    content.split("(?=\\{1:)").findAll { it.trim() } : [content]

                messages.each { msgContent ->
                    def result = ec.service.sync()
                        .name("trade.swift.InboundSwiftServices.ingest#SwiftMessage")
                        .parameters([rawContent: msgContent.trim(),
                                     sourceChannel: "SRC_DIRECTORY_POLL",
                                     sourceFileName: file.name]).call()

                    if (result.isDuplicate) {
                        // Move to duplicate archive
                    } else if (result.parseSuccess) {
                        // Proceed to correlation
                        ec.service.sync()
                            .name("trade.swift.InboundCorrelationServices.correlate#SwiftMessage")
                            .parameters([rawMessageId: result.rawMessageId]).call()
                    }
                }

                // Move file to processed (or error if all failed)
                def targetDir = processedDir
                file.renameTo(new File(targetDir, file.name))
            }
        ]]></script>
    </actions>
</service>
```

- [ ] **Step 8: Commit**

```bash
git add service/trade/swift/InboundSwiftServices.xml src/test/groovy/trade/InboundSwiftSpec.groovy
git commit -m "feat(inbound): ingestion services with SHA-256 dedup and directory polling"
```

---

### Task 3: Correlation Engine (Tag 21 + Sub-Correlation + Orphan)

**BDD Scenarios:** BDD-INB-COR-01 (auto-match), BDD-INB-COR-02 (orphan), BDD-INB-COR-03 (manual link), BDD-INB-COR-04 (sub-correlation)
**BRD Requirements:** §3.1 Golden Rule, §3.2 Correlation Logic, §3.3 Sub-Correlation, §3.4 Orphan Queue
**User-Facing:** NO

**Files:**
- Create: `service/trade/swift/InboundCorrelationServices.xml`
- Test: `src/test/groovy/trade/InboundSwiftSpec.groovy` (add correlation tests)

- [ ] **Step 1: Write failing test — Tag 21 auto-match**

```groovy
// BDD-INB-COR-01: Tag 21 auto-match to LC
def "correlate#SwiftMessage matches Tag 21 to TradeInstrument"() {
    setup: "create LC with known instrumentRef"
    def lcRef = testPrefix + "LC-COR-01"
    // ... create TradeInstrument with instrumentRef = lcRef ...
    // ... ingest a sample MT 730 with Tag 21 = lcRef ...

    when:
    ec.service.sync().name("trade.swift.InboundCorrelationServices.correlate#SwiftMessage")
        .parameters([rawMessageId: rawMessageId]).call()

    then:
    def inbox = ec.entity.find("trade.swift.TradeInboxItem")
        .condition("rawMessageId", rawMessageId).one()
    inbox.instrumentId == instrumentId
    inbox.correlationStatusEnumId == "CORR_AUTO_MATCH"
    inbox.inboxStatusEnumId == "INBOX_UNREAD"
    inbox.relatedReference == lcRef
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :runtime:component:TradeFinance:test --tests "trade.InboundSwiftSpec.correlate*" -x jar`
Expected: FAIL — service not defined.

- [ ] **Step 3: Implement correlate#SwiftMessage**

```xml
<service verb="correlate" noun="SwiftMessage" authenticate="false"
         comment="Tag 21 correlation: links parsed inbound message to parent LC, creates TradeInboxItem.">
    <in-parameters>
        <parameter name="rawMessageId" required="true"/>
    </in-parameters>
    <out-parameters>
        <parameter name="inboxItemId"/>
    </out-parameters>
    <actions>
        <script>ec.artifactExecution.disableAuthz()</script>
        <entity-find-one entity-name="trade.swift.InboundSwiftRaw" value-field="raw"/>
        <if condition="!raw || raw.parseStatusEnumId != 'PARSE_SUCCESS'"><return/></if>

        <script><![CDATA[
            def sm = com.prowidesoftware.swift.model.SwiftMessage.parse(raw.rawContent)
            def tag21 = sm.getBlock4()?.getTagValue("21")?.trim()
            def tag20 = sm.getBlock4()?.getTagValue("20")?.trim()
            def tag30 = sm.getBlock4()?.getTagValue("30")?.trim()
            def tag32b = sm.getBlock4()?.getTagValue("32B")
            def tag77j = sm.getBlock4()?.getTagValue("77J")
            def tag79 = sm.getBlock4()?.getTagValue("79")
            def senderBic = sm.getBlock1()?.getLogicalTerminal()?.substring(0, 8) ?: "UNKNOWN"
            def msgType = sm.getType()

            // Parse claim amount from Tag 32B (format: CCYAMOUNT e.g. "USD45000,00")
            def claimAmount = null; def claimCurrency = null
            if (tag32b) {
                claimCurrency = tag32b.substring(0, 3)
                claimAmount = new BigDecimal(tag32b.substring(3).replace(",", "."))
            }

            // Correlation: find LC by Tag 21
            def correlationStatus = "CORR_ORPHANED"
            def inboxStatus = "INBOX_ORPHANED"
            def orphanReason = null
            def instrumentId = null
            def amendmentId = null

            if (tag21) {
                def instruments = ec.entity.find("trade.TradeInstrument")
                    .condition("instrumentRef", tag21).list()
                if (instruments.size() == 1) {
                    instrumentId = instruments[0].instrumentId
                    correlationStatus = "CORR_AUTO_MATCH"
                    inboxStatus = "INBOX_UNREAD"

                    // Sub-correlation: MT 730/799 — match to amendment via Tag 30
                    if (msgType in ["730", "799"] && tag30 && instrumentId) {
                        def amendments = ec.entity.find("trade.importlc.ImportLcAmendment")
                            .condition("instrumentId", instrumentId).list()
                        def parsedDate = tag30 // YYMMDD format
                        def matched = amendments.find { amd ->
                            def amdDate = amd.amendmentDate?.format("yyMMdd")
                            return amdDate == parsedDate
                        }
                        if (matched) amendmentId = matched.amendmentId
                    }
                } else if (instruments.size() == 0) {
                    orphanReason = "NO_MATCH_FOUND"
                } else {
                    orphanReason = "AMBIGUOUS_REF"
                }
            } else {
                orphanReason = "NO_TAG21"
            }

            // Security flag for MT 999
            def securityWarning = (msgType == "999") ? "Y" : "N"

            // Narrative: Tag 79 for MT 799/999, Tag 77J for MT 750
            def narrative = tag79 ?: tag77j

            def createResult = ec.service.sync().name("create#trade.swift.TradeInboxItem")
                .parameters([
                    rawMessageId: rawMessageId, instrumentId: instrumentId,
                    amendmentId: amendmentId, messageType: msgType,
                    senderBic: senderBic, senderReference: tag20,
                    relatedReference: tag21, narrativeText: narrative,
                    claimAmount: claimAmount, claimCurrency: claimCurrency,
                    receivedTimestamp: ec.user.nowTimestamp,
                    inboxStatusEnumId: inboxStatus,
                    correlationStatusEnumId: correlationStatus,
                    orphanReason: orphanReason,
                    securityWarningFlag: securityWarning
                ]).call()
            context.inboxItemId = createResult.inboxItemId
        ]]></script>
    </actions>
</service>
```

- [ ] **Step 4: Run test to verify it passes**

- [ ] **Step 5: Write failing test — orphan on missing Tag 21**

```groovy
// BDD-INB-COR-02: Orphan queue on missing Tag 21
def "correlate routes to orphan queue when Tag 21 has no match"() {
    setup: "ingest MT 799 with unknown Tag 21"
    // ... ingest sample with Tag 21 = "UNKNOWN-REF-999" ...

    when:
    ec.service.sync().name("trade.swift.InboundCorrelationServices.correlate#SwiftMessage")
        .parameters([rawMessageId: rawMessageId]).call()

    then:
    def inbox = ec.entity.find("trade.swift.TradeInboxItem")
        .condition("rawMessageId", rawMessageId).one()
    inbox.instrumentId == null
    inbox.correlationStatusEnumId == "CORR_ORPHANED"
    inbox.inboxStatusEnumId == "INBOX_ORPHANED"
    inbox.orphanReason == "NO_MATCH_FOUND"
}
```

- [ ] **Step 6: Run test — should pass (orphan logic in Step 3)**

- [ ] **Step 7: Implement link#OrphanMessage for manual linking**

```xml
<service verb="link" noun="OrphanMessage" authenticate="true"
         comment="Manual orphan resolution: operator links an orphaned inbox item to an LC.">
    <in-parameters>
        <parameter name="inboxItemId" required="true"/>
        <parameter name="instrumentId" required="true"/>
    </in-parameters>
    <actions>
        <entity-find-one entity-name="trade.swift.TradeInboxItem" value-field="item"/>
        <set field="item.instrumentId" from="instrumentId"/>
        <set field="item.correlationStatusEnumId" value="CORR_MANUAL_MATCH"/>
        <set field="item.inboxStatusEnumId" value="INBOX_UNREAD"/>
        <set field="item.orphanReason" from="null"/>
        <entity-update value-field="item"/>
    </actions>
</service>
```

- [ ] **Step 8: Commit**

```bash
git add service/trade/swift/InboundCorrelationServices.xml src/test/groovy/trade/InboundSwiftSpec.groovy
git commit -m "feat(inbound): Tag 21 correlation engine with orphan queue and manual linking"
```

---

### Task 4: MT 730 Acknowledge (Flag-Based)

**BDD Scenarios:** BDD-INB-730-01 (issuance advised), BDD-INB-730-02 (amendment advised)
**BRD Requirements:** §5.1 MT 730, MT730-01..03
**User-Facing:** NO (backend service only; UI in future task)

**Files:**
- Create: `service/trade/swift/InboundActionServices.xml`
- Test: `src/test/groovy/trade/InboundActionSpec.groovy`

- [ ] **Step 1: Write failing test — MT 730 sets isAdvised flag on LC**

```groovy
// BDD-INB-730-01: Acknowledge issuance sets advised flag
def "acknowledge#Mt730 sets isAdvised flag without changing businessStateId"() {
    setup: "create LC in LC_ISSUED state"
    // ... create TradeInstrument + ImportLetterOfCredit with businessStateId = 'LC_ISSUED' ...
    // ... create TradeInboxItem for MT 730 linked to this LC ...

    when:
    ec.service.sync().name("trade.swift.InboundActionServices.acknowledge#Mt730")
        .parameters([inboxItemId: inboxItemId]).call()

    then:
    def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit")
        .condition("instrumentId", instrumentId).one()
    lc.isAdvised == "Y"
    lc.advisedDate != null
    lc.advisingBankReference != null
    lc.businessStateId == "LC_ISSUED" // unchanged

    def inbox = ec.entity.find("trade.swift.TradeInboxItem")
        .condition("inboxItemId", inboxItemId).one()
    inbox.inboxStatusEnumId == "INBOX_PROCESSED"
    inbox.actionTaken == "ACKNOWLEDGE"
}
```

- [ ] **Step 2: Run test to verify it fails**

- [ ] **Step 3: Implement acknowledge#Mt730**

```xml
<service verb="acknowledge" noun="Mt730" authenticate="true"
         comment="Operator acknowledges MT 730 delivery receipt. Sets advised flag on LC or amendment.">
    <in-parameters>
        <parameter name="inboxItemId" required="true"/>
    </in-parameters>
    <actions>
        <entity-find-one entity-name="trade.swift.TradeInboxItem" value-field="item"/>
        <entity-find-one entity-name="trade.swift.InboundSwiftRaw" value-field="raw">
            <field-map field-name="rawMessageId" from="item.rawMessageId"/>
        </entity-find-one>

        <script><![CDATA[
            def sm = com.prowidesoftware.swift.model.SwiftMessage.parse(raw.rawContent)
            def tag20 = sm.getBlock4()?.getTagValue("20")?.trim()
            def tag30 = sm.getBlock4()?.getTagValue("30")?.trim()

            // Parse Tag 30 date (YYMMDD → java date)
            def advisedDate = null
            if (tag30) {
                advisedDate = java.time.LocalDate.parse(tag30,
                    java.time.format.DateTimeFormatter.ofPattern("yyMMdd"))
            }
        ]]></script>

        <if condition="item.amendmentId">
            <!-- Amendment acknowledged -->
            <entity-find-one entity-name="trade.importlc.ImportLcAmendment" value-field="amendment">
                <field-map field-name="amendmentId" from="item.amendmentId"/>
            </entity-find-one>
            <set field="amendment.isAdvisedToBeneficiary" value="Y"/>
            <set field="amendment.advisedToBeneficiaryDate" from="advisedDate"/>
            <entity-update value-field="amendment"/>
        <else>
            <!-- Issuance acknowledged -->
            <entity-find-one entity-name="trade.importlc.ImportLetterOfCredit" value-field="lc">
                <field-map field-name="instrumentId" from="item.instrumentId"/>
            </entity-find-one>
            <set field="lc.isAdvised" value="Y"/>
            <set field="lc.advisedDate" from="advisedDate"/>
            <set field="lc.advisingBankReference" from="tag20"/>
            <entity-update value-field="lc"/>
        </else>
        </if>

        <!-- Mark inbox item processed -->
        <set field="item.inboxStatusEnumId" value="INBOX_PROCESSED"/>
        <set field="item.actionTaken" value="ACKNOWLEDGE"/>
        <set field="item.processedByUserId" from="ec.user.userId"/>
        <set field="item.processedTimestamp" from="ec.user.nowTimestamp"/>
        <entity-update value-field="item"/>
    </actions>
</service>
```

- [ ] **Step 4: Run test to verify it passes**

- [ ] **Step 5: Write failing test — MT 730 amendment advised flag**

```groovy
// BDD-INB-730-02: Acknowledge amendment sets advised flag
def "acknowledge#Mt730 for amendment sets isAdvisedToBeneficiary"() {
    setup: "create LC + amendment in BENE_PENDING"
    // ... create amendment with beneficiaryConsentStatusId = 'BENE_PENDING' ...
    // ... create TradeInboxItem with amendmentId set ...

    when:
    ec.service.sync().name("trade.swift.InboundActionServices.acknowledge#Mt730")
        .parameters([inboxItemId: inboxItemId]).call()

    then:
    def amd = ec.entity.find("trade.importlc.ImportLcAmendment")
        .condition("amendmentId", amendmentId).one()
    amd.isAdvisedToBeneficiary == "Y"
    amd.advisedToBeneficiaryDate != null
    amd.beneficiaryConsentStatusId == "BENE_PENDING" // unchanged
}
```

- [ ] **Step 6: Run test — should pass**

- [ ] **Step 7: Commit**

```bash
git add service/trade/swift/InboundActionServices.xml src/test/groovy/trade/InboundActionSpec.groovy
git commit -m "feat(inbound): MT 730 acknowledge service with flag-based advised tracking"
```

---

### Task 5: MT 799 Amendment Consent (Reuses Existing Services)

**BDD Scenarios:** BDD-INB-799-01 (accept merges), BDD-INB-799-02 (reject releases), BDD-INB-799-03 (no auto-eval)
**BRD Requirements:** §5.2 MT 799, MT799-01..04, §7A.4 commit#Amendment gap
**User-Facing:** NO

**Files:**
- Modify: `service/trade/swift/InboundActionServices.xml` (add processConsent#Mt799)
- Test: `src/test/groovy/trade/InboundActionSpec.groovy`

> **IMPORTANT:** No new `commit#Amendment` service needed. The existing `accept#Amendment` (ImportLcServices.xml:680) already sets `BENE_ACCEPTED`, `AMEND_COMMITTED`, transitions LC to `LC_AMENDED`, and calls `merge#AmendmentDeltas`. For reject, existing `reject#Amendment` (ImportLcServices.xml:703) sets `BENE_REJECTED` + `AMEND_REJECTED`.

- [ ] **Step 1: Write failing test — accept amendment merges delta**

```groovy
// BDD-INB-799-01: Accept amendment merges delta into master LC
def "processConsent#Mt799 accept merges amendment delta into master LC"() {
    setup: "create LC with effectiveAmount = 100000 and pending amendment +25000"
    // ... create LC, amendment with amountAdjustment = 25000 ...
    // ... create inbox item for MT 799 ...

    when:
    ec.service.sync().name("trade.swift.InboundActionServices.processConsent#Mt799")
        .parameters([inboxItemId: inboxItemId, decision: "ACCEPT"]).call()

    then:
    def amd = ec.entity.find("trade.importlc.ImportLcAmendment")
        .condition("amendmentId", amendmentId).one()
    amd.beneficiaryConsentStatusId == "BENE_ACCEPTED"
    amd.amendmentBusinessStateId == "AMEND_COMMITTED"
    amd.consentSwiftRef != null

    def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit")
        .condition("instrumentId", instrumentId).one()
    lc.effectiveAmount == 125000
    lc.effectiveOutstandingAmount == 125000
}
```

- [ ] **Step 2: Run test to verify it fails**

- [ ] **Step 3: Implement processConsent#Mt799 in InboundActionServices.xml**

> **NOTE:** Uses existing `accept#Amendment` and `reject#Amendment` services — no new merge service needed.

```xml
<service verb="processConsent" noun="Mt799" authenticate="true"
         comment="Operator accepts or rejects a pending amendment based on MT 799 narrative.">
    <in-parameters>
        <parameter name="inboxItemId" required="true"/>
        <parameter name="decision" required="true">
            <description>ACCEPT or REJECT</description>
        </parameter>
    </in-parameters>
    <actions>
        <entity-find-one entity-name="trade.swift.TradeInboxItem" value-field="item"/>

        <!-- Find the pending amendment -->
        <if condition="!item.amendmentId">
            <!-- If no sub-correlation, find latest pending amendment for this LC -->
            <entity-find entity-name="trade.importlc.ImportLcAmendment" list="amendments">
                <econdition field-name="instrumentId" from="item.instrumentId"/>
                <econdition field-name="beneficiaryConsentStatusId" value="BENE_PENDING"/>
                <order-by field-name="-amendmentDate"/>
            </entity-find>
            <set field="amendmentId" from="amendments ? amendments[0].amendmentId : null"/>
        <else>
            <set field="amendmentId" from="item.amendmentId"/>
        </else>
        </if>

        <if condition="!amendmentId">
            <return error="true" message="No pending amendment found for this LC."/>
        </if>

        <entity-find-one entity-name="trade.importlc.ImportLcAmendment" value-field="amendment">
            <field-map field-name="amendmentId"/>
        </entity-find-one>

        <!-- Parse Tag 20 for audit ref -->
        <entity-find-one entity-name="trade.swift.InboundSwiftRaw" value-field="raw">
            <field-map field-name="rawMessageId" from="item.rawMessageId"/>
        </entity-find-one>
        <script><![CDATA[
            def sm = com.prowidesoftware.swift.model.SwiftMessage.parse(raw.rawContent)
            context.consentRef = sm.getBlock4()?.getTagValue("20")?.trim()
        ]]></script>

        <set field="amendment.consentSwiftRef" from="consentRef"/>

        <if condition="decision == 'ACCEPT'">
            <!-- Set consent ref before calling existing service -->
            <entity-update value-field="amendment"/>
            <!-- Reuse existing accept#Amendment: sets BENE_ACCEPTED, AMEND_COMMITTED, merges deltas -->
            <service-call name="trade.importlc.ImportLcServices.accept#Amendment"
                          in-map="[amendmentId: amendmentId]"/>
            <set field="actionTaken" value="ACCEPT_AMENDMENT"/>
        <else>
            <!-- Reuse existing reject#Amendment: sets BENE_REJECTED, AMEND_REJECTED -->
            <service-call name="trade.importlc.ImportLcServices.reject#Amendment"
                          in-map="[amendmentId: amendmentId]"/>
        </else>
        </if>

        <!-- Mark inbox item processed -->
        <set field="item.inboxStatusEnumId" value="INBOX_PROCESSED"/>
        <set field="item.actionTaken" from="actionTaken"/>
        <set field="item.processedByUserId" from="ec.user.userId"/>
        <set field="item.processedTimestamp" from="ec.user.nowTimestamp"/>
        <entity-update value-field="item"/>
    </actions>
</service>
```

- [ ] **Step 5: Run test to verify it passes**

- [ ] **Step 6: Write failing test — reject releases earmarked limits**

```groovy
// BDD-INB-799-02: Reject amendment releases earmarked limits
def "processConsent#Mt799 reject releases earmarked limits"() {
    setup: "create LC + amendment with +25000 increase"
    // ... create LC with effectiveAmount = 100000 ...
    // ... create amendment with amountAdjustment = 25000 ...

    when:
    ec.service.sync().name("trade.swift.InboundActionServices.processConsent#Mt799")
        .parameters([inboxItemId: inboxItemId, decision: "REJECT"]).call()

    then:
    def amd = ec.entity.find("trade.importlc.ImportLcAmendment")
        .condition("amendmentId", amendmentId).one()
    amd.beneficiaryConsentStatusId == "BENE_REJECTED"
    amd.amendmentBusinessStateId == "AMEND_REJECTED"

    def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit")
        .condition("instrumentId", instrumentId).one()
    lc.effectiveAmount == 100000 // unchanged
}
```

- [ ] **Step 7: Run test — should pass**

- [ ] **Step 8: Commit**

```bash
git add service/trade/swift/InboundActionServices.xml
git add src/test/groovy/trade/InboundActionSpec.groovy
git commit -m "feat(inbound): MT 799 consent using existing accept/reject#Amendment services"
```

### Task 6: MT 750 Discrepant Presentation Auto-Spawn

**BDD Scenarios:** BDD-INB-750-01 (auto-spawn), BDD-INB-750-02 (bypasses validation SECA)
**BRD Requirements:** §5.3 MT 750, MT750-01..04, §7A.3.B SECA bypass
**User-Facing:** NO

**Files:**
- Modify: `service/trade/swift/InboundActionServices.xml` (add spawnPresentation#Mt750)
- Modify: `service/ImportLc.secas.xml` (add sourceChannel bypass)
- Test: `src/test/groovy/trade/InboundActionSpec.groovy`

- [ ] **Step 1: Write failing test — MT 750 auto-spawns discrepant presentation**

```groovy
// BDD-INB-750-01: Auto-spawn discrepant presentation
def "spawnPresentation#Mt750 creates presentation with parsed discrepancies"() {
    setup: "create LC in LC_ISSUED, ingest MT 750 with 2 discrepancy lines"
    // ... create LC ...
    // ... create inbox item for MT 750 with claimAmount=45000, narrativeText="DOC1\nDOC2" ...

    when:
    ec.service.sync().name("trade.swift.InboundActionServices.spawnPresentation#Mt750")
        .parameters([inboxItemId: inboxItemId]).call()

    then:
    def presentations = ec.entity.find("trade.importlc.TradeDocumentPresentation")
        .condition("instrumentId", instrumentId).list()
    presentations.size() >= 1
    def pres = presentations[0]
    pres.presentationStatusId == "PRES_RECEIVED"
    pres.isDiscrepant == "Y"
    pres.applicantDecisionEnumId == "PENDING"
    pres.claimAmount == 45000

    def discs = ec.entity.find("trade.importlc.PresentationDiscrepancy")
        .condition("presentationId", pres.presentationId).list()
    discs.size() >= 2

    def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit")
        .condition("instrumentId", instrumentId).one()
    lc.businessStateId == "LC_DOC_RECEIVED"
}
```

- [ ] **Step 2: Run test to verify it fails**

- [ ] **Step 3: Implement spawnPresentation#Mt750**

```xml
<service verb="spawnPresentation" noun="Mt750" authenticate="false"
         comment="Auto-creates a discrepant TradeDocumentPresentation from inbound MT 750 data.">
    <in-parameters>
        <parameter name="inboxItemId" required="true"/>
    </in-parameters>
    <out-parameters>
        <parameter name="presentationId"/>
    </out-parameters>
    <actions>
        <entity-find-one entity-name="trade.swift.TradeInboxItem" value-field="item"/>

        <!-- Create presentation with sourceChannel = INBOUND to bypass validation SECA -->
        <service-call name="trade.TradeCommonServices.create#Presentation"
                      in-map="[instrumentId: item.instrumentId,
                               claimAmount: item.claimAmount,
                               claimCurrency: item.claimCurrency,
                               presentingBankRef: item.senderReference,
                               isDiscrepant: 'Y',
                               applicantDecisionEnumId: 'PENDING',
                               presentationStatusId: 'PRES_RECEIVED',
                               sourceChannel: 'INBOUND']"
                      out-map="presResult"/>
        <set field="presentationId" from="presResult.presentationId"/>

        <!-- Parse Tag 77J discrepancies into individual records -->
        <if condition="item.narrativeText">
            <script><![CDATA[
                def lines = item.narrativeText.split("\n").findAll { it.trim() }
                int seq = 1
                lines.each { line ->
                    ec.service.sync().name("create#trade.importlc.PresentationDiscrepancy")
                        .parameters([
                            presentationId: presentationId,
                            sequenceNum: seq++,
                            discrepancyText: line.trim()
                        ]).call()
                }
            ]]></script>
        </if>

        <!-- Transition LC to LC_DOC_RECEIVED -->
        <service-call name="trade.importlc.ImportLcServices.update#ImportLetterOfCredit"
                      in-map="[instrumentId: item.instrumentId,
                               businessStateId: 'LC_DOC_RECEIVED']"/>

        <!-- Mark inbox item processed -->
        <set field="item.inboxStatusEnumId" value="INBOX_PROCESSED"/>
        <set field="item.actionTaken" value="SPAWN_PRESENTATION"/>
        <set field="item.processedByUserId" from="ec.user.userId"/>
        <set field="item.processedTimestamp" from="ec.user.nowTimestamp"/>
        <entity-update value-field="item"/>
    </actions>
</service>
```

- [ ] **Step 4: Add sourceChannel bypass in TWO locations**

> **CRITICAL:** `create#Presentation` validates presentations in TWO places. Both need the bypass.

**Location 1: SECA bypass** — `service/ImportLc.secas.xml` line 28:

```xml
<!-- BEFORE (line 28-30): -->
<seca service="trade.TradeCommonServices.create#Presentation" when="pre-service">
    <service-call name="trade.importlc.ImportLcValidationServices.validate#Presentation" in-map="context"/>
</seca>

<!-- AFTER: Add condition to skip for inbound -->
<seca service="trade.TradeCommonServices.create#Presentation" when="pre-service">
    <condition>
        <condition field-name="sourceChannel" operator="not-equals" value="INBOUND" or-null="true"/>
    </condition>
    <service-call name="trade.importlc.ImportLcValidationServices.validate#Presentation" in-map="context"/>
</seca>
```

**Location 2: Inline validation** — `service/trade/TradeCommonServices.xml` line 50:

```xml
<!-- BEFORE (line 50-53): -->
<service-call name="trade.importlc.ImportLcValidationServices.validate#Presentation" in-map="context" out-map="valOut"/>
<if condition="valOut.isDiscrepant">
    <return error="true" message="Presentation blocked: ${valOut.discrepancyMessage}"/>
</if>

<!-- AFTER: Wrap in sourceChannel check -->
<if condition="sourceChannel != 'INBOUND'">
    <service-call name="trade.importlc.ImportLcValidationServices.validate#Presentation" in-map="context" out-map="valOut"/>
    <if condition="valOut.isDiscrepant">
        <return error="true" message="Presentation blocked: ${valOut.discrepancyMessage}"/>
    </if>
</if>
```

- [ ] **Step 5: Run test to verify it passes**

- [ ] **Step 6: Write failing test — inbound bypasses SECA validation**

```groovy
// BDD-INB-750-02: Inbound presentation bypasses validation SECA
def "spawnPresentation#Mt750 bypasses tolerance check for over-limit claim"() {
    setup: "create LC with effectiveAmount=50000, tolerance=0.10 (max=55000)"
    // ... create LC ...
    // ... create inbox item with claimAmount=60000 (exceeds tolerance) ...

    when: "spawn should succeed despite tolerance breach"
    ec.service.sync().name("trade.swift.InboundActionServices.spawnPresentation#Mt750")
        .parameters([inboxItemId: inboxItemId]).call()

    then: "presentation created — SECA bypassed"
    def pres = ec.entity.find("trade.importlc.TradeDocumentPresentation")
        .condition("instrumentId", instrumentId).list()
    pres.size() >= 1
    pres[0].isDiscrepant == "Y"
}
```

- [ ] **Step 7: Run test — should pass**

- [ ] **Step 8: Commit**

```bash
git add service/trade/swift/InboundActionServices.xml service/ImportLc.secas.xml
git add src/test/groovy/trade/InboundActionSpec.groovy
git commit -m "feat(inbound): MT 750 auto-spawn with SECA validation bypass"
```

---

### Task 7: MT 754 Clean Presentation + MT 742 Reimbursement

**BDD Scenarios:** BDD-INB-754-01 (clean fast-track), BDD-INB-742-01 (auto-match), BDD-INB-742-02 (over-limit exception)
**BRD Requirements:** §5.4 MT 754, MT754-01..03, §5.5 MT 742, MT742-01..04
**User-Facing:** NO

**Files:**
- Modify: `service/trade/swift/InboundActionServices.xml` (add spawnPresentation#Mt754, matchReimbursement#Mt742)
- Test: `src/test/groovy/trade/InboundActionSpec.groovy`

- [ ] **Step 1: Write failing test — MT 754 creates compliant presentation**

```groovy
// BDD-INB-754-01: Clean presentation skips examination
def "spawnPresentation#Mt754 creates PRES_COMPLIANT and transitions to LC_ACCEPTED"() {
    setup:
    // ... create LC in LC_ISSUED ...
    // ... create inbox item for MT 754, claimAmount=50000 ...

    when:
    ec.service.sync().name("trade.swift.InboundActionServices.spawnPresentation#Mt754")
        .parameters([inboxItemId: inboxItemId]).call()

    then:
    def pres = ec.entity.find("trade.importlc.TradeDocumentPresentation")
        .condition("instrumentId", instrumentId).list()
    pres.size() >= 1
    pres[0].presentationStatusId == "PRES_COMPLIANT"
    pres[0].isDiscrepant == "N"

    def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit")
        .condition("instrumentId", instrumentId).one()
    lc.businessStateId == "LC_ACCEPTED"
}
```

- [ ] **Step 2: Run test to verify it fails**

- [ ] **Step 3: Implement spawnPresentation#Mt754**

```xml
<service verb="spawnPresentation" noun="Mt754" authenticate="false"
         comment="Auto-creates a clean/compliant presentation from MT 754. Skips examination queue.">
    <in-parameters>
        <parameter name="inboxItemId" required="true"/>
    </in-parameters>
    <actions>
        <entity-find-one entity-name="trade.swift.TradeInboxItem" value-field="item"/>

        <service-call name="trade.TradeCommonServices.create#Presentation"
                      in-map="[instrumentId: item.instrumentId,
                               claimAmount: item.claimAmount,
                               claimCurrency: item.claimCurrency,
                               presentingBankRef: item.senderReference,
                               isDiscrepant: 'N',
                               applicantDecisionEnumId: 'WAIVED',
                               presentationStatusId: 'PRES_COMPLIANT',
                               sourceChannel: 'INBOUND']"/>

        <!-- Transition directly to LC_ACCEPTED -->
        <service-call name="trade.importlc.ImportLcServices.update#ImportLetterOfCredit"
                      in-map="[instrumentId: item.instrumentId,
                               businessStateId: 'LC_ACCEPTED']"/>

        <set field="item.inboxStatusEnumId" value="INBOX_PROCESSED"/>
        <set field="item.actionTaken" value="SPAWN_PRESENTATION"/>
        <set field="item.processedByUserId" from="ec.user.userId"/>
        <set field="item.processedTimestamp" from="ec.user.nowTimestamp"/>
        <entity-update value-field="item"/>
    </actions>
</service>
```

- [ ] **Step 4: Run test to verify it passes**

- [ ] **Step 5: Write failing test — MT 742 auto-match within limit**

```groovy
// BDD-INB-742-01: Auto-match reimbursement within limit
def "matchReimbursement#Mt742 auto-matches when claim within authorization"() {
    setup:
    // ... create NostroReconciliation with expectedAmount=50000, RECON_PENDING ...
    // ... create inbox item for MT 742, claimAmount=48000 ...

    when:
    ec.service.sync().name("trade.swift.InboundActionServices.matchReimbursement#Mt742")
        .parameters([inboxItemId: inboxItemId]).call()

    then:
    def recon = ec.entity.find("trade.importlc.NostroReconciliation")
        .condition("reconciliationId", reconId).one()
    recon.matchStatusEnumId == "RECON_MATCHED"
}
```

- [ ] **Step 6: Implement matchReimbursement#Mt742**

```xml
<service verb="matchReimbursement" noun="Mt742" authenticate="false"
         comment="Auto-matches MT 742 reimbursement claim against NostroReconciliation authorization.">
    <in-parameters>
        <parameter name="inboxItemId" required="true"/>
    </in-parameters>
    <actions>
        <entity-find-one entity-name="trade.swift.TradeInboxItem" value-field="item"/>

        <!-- Find pending reconciliation for this LC -->
        <entity-find entity-name="trade.importlc.NostroReconciliation" list="recons">
            <econdition field-name="instrumentId" from="item.instrumentId"/>
            <econdition field-name="matchStatusEnumId" value="RECON_PENDING"/>
        </entity-find>

        <if condition="!recons">
            <set field="item.inboxStatusEnumId" value="INBOX_ORPHANED"/>
            <set field="item.orphanReason" value="NO_PENDING_RECON"/>
            <entity-update value-field="item"/>
            <return/>
        </if>

        <set field="recon" from="recons[0]"/>

        <if condition="item.claimAmount &lt;= recon.expectedAmount">
            <!-- Auto-match: within authorization -->
            <set field="recon.matchStatusEnumId" value="RECON_MATCHED"/>
            <set field="recon.matchedAmount" from="item.claimAmount"/>
            <entity-update value-field="recon"/>
            <set field="item.actionTaken" value="AUTO_MATCH"/>
        <else>
            <!-- Over-limit: route to exception -->
            <set field="recon.matchStatusEnumId" value="RECON_EXCEPTION"/>
            <entity-update value-field="recon"/>
            <set field="item.actionTaken" value="EXCEPTION_OVER_LIMIT"/>
        </else>
        </if>

        <set field="item.inboxStatusEnumId" value="INBOX_PROCESSED"/>
        <set field="item.processedTimestamp" from="ec.user.nowTimestamp"/>
        <entity-update value-field="item"/>
    </actions>
</service>
```

- [ ] **Step 7: Run tests, commit**

```bash
git add service/trade/swift/InboundActionServices.xml src/test/groovy/trade/InboundActionSpec.groovy
git commit -m "feat(inbound): MT 754 clean presentation + MT 742 reimbursement matching"
```

---

### Task 8: Discrepancy Resolution (Maker/Checker)

**BDD Scenarios:** BDD-INB-DSC-01 (waiver flow), BDD-INB-DSC-02 (refusal MT 734), BDD-INB-DSC-03 (upload gate), BDD-INB-DSC-04 (UCP 600 deadline)
**BRD Requirements:** §6.1–6.5, DISC-01..06
**User-Facing:** YES (but UI is Phase 3 — backend services only here)

**Files:**
- Modify: `service/trade/swift/InboundActionServices.xml` (add resolve#Discrepancy)
- Test: `src/test/groovy/trade/InboundActionSpec.groovy`

> **Note:** The existing `TradeDocumentPresentation` entity and Maker/Checker `TradeTransaction` pipeline are reused. This task adds the resolution service that enforces the mandatory upload gate (DISC-01).

- [ ] **Step 1: Write failing test — waiver blocked without uploaded document**

```groovy
// BDD-INB-DSC-03: Submit blocked without uploaded evidence
def "resolve#Discrepancy rejects waiver without uploaded document"() {
    setup: "create discrepant presentation without attachment"
    // ... create TradeDocumentPresentation with isDiscrepant='Y', applicantDecisionEnumId='PENDING' ...
    // ... DO NOT attach a document ...

    when:
    ec.service.sync().name("trade.swift.InboundActionServices.resolve#Discrepancy")
        .parameters([presentationId: presentationId, decision: "WAIVE"]).call()

    then:
    thrown(Exception) || ec.message.hasError()
    // Error: "Applicant waiver/refusal document is mandatory before submission."
}
```

- [ ] **Step 2: Run test to verify it fails**

- [ ] **Step 3: Implement resolve#Discrepancy**

```xml
<service verb="resolve" noun="Discrepancy" authenticate="true"
         comment="Maker submits discrepancy resolution (waive/refuse) with mandatory document upload gate.">
    <in-parameters>
        <parameter name="presentationId" required="true"/>
        <parameter name="decision" required="true">
            <description>WAIVE or REFUSE</description>
        </parameter>
        <parameter name="documentDisposal">
            <description>HOLDING_DOCUMENTS or RETURNING_DOCUMENTS (for refusal only)</description>
        </parameter>
    </in-parameters>
    <actions>
        <entity-find-one entity-name="trade.importlc.TradeDocumentPresentation" value-field="pres"/>

        <!-- DISC-01: Mandatory upload gate -->
        <entity-find entity-name="moqui.resource.DbResourceFile" list="attachments">
            <econdition field-name="parentResourceId" from="'PRES_' + presentationId"/>
        </entity-find>
        <if condition="!attachments">
            <return error="true"
                    message="Applicant waiver/refusal document is mandatory before submission."/>
        </if>

        <if condition="decision == 'WAIVE'">
            <set field="pres.applicantDecisionEnumId" value="WAIVED"/>
        <else>
            <set field="pres.applicantDecisionEnumId" value="REFUSED"/>
            <set field="pres.documentDisposal" from="documentDisposal"/>
        </else>
        </if>

        <set field="pres.presentationStatusId" value="PRES_EXAMINING"/>
        <entity-update value-field="pres"/>
    </actions>
</service>
```

- [ ] **Step 4: Run test to verify it passes**

- [ ] **Step 5: Write failing test — successful waiver with document**

```groovy
// BDD-INB-DSC-01: Waiver with mandatory upload and Checker auth
def "resolve#Discrepancy waiver succeeds with uploaded document"() {
    setup: "create discrepant presentation WITH attachment"
    // ... create presentation, attach a DbResourceFile ...

    when:
    ec.service.sync().name("trade.swift.InboundActionServices.resolve#Discrepancy")
        .parameters([presentationId: presentationId, decision: "WAIVE"]).call()

    then:
    def pres = ec.entity.find("trade.importlc.TradeDocumentPresentation")
        .condition("presentationId", presentationId).one()
    pres.applicantDecisionEnumId == "WAIVED"
    pres.presentationStatusId == "PRES_EXAMINING"
}
```

- [ ] **Step 6: Run test — should pass**

- [ ] **Step 7: Commit**

```bash
git add service/trade/swift/InboundActionServices.xml src/test/groovy/trade/InboundActionSpec.groovy
git commit -m "feat(inbound): discrepancy resolution with mandatory upload gate"
```

---

### Task 9: SECA Wiring + Integration Test

**BDD Scenarios:** All scenarios (integration validation)
**BRD Requirements:** §7.2 SECA gaps, full pipeline verification
**User-Facing:** NO

**Files:**
- Create: `service/InboundSwift.secas.xml`
- Modify: `src/test/groovy/trade/TradeFinanceMoquiSuite.groovy` (register new specs)
- Test: Full suite run

- [ ] **Step 1: Create InboundSwift.secas.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<secas xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:noNamespaceSchemaLocation="http://moqui.org/xsd/service-eca-3.0.xsd">

    <!-- ABOUTME: Service ECA rules for inbound SWIFT message processing. -->
    <!-- ABOUTME: Triggers auto-spawn (MT 750/754) and auto-match (MT 742) after correlation. -->

    <!-- After successful correlation, auto-trigger message-type-specific actions -->
    <seca service="trade.swift.InboundCorrelationServices.correlate#SwiftMessage"
          when="post-service" run-on-error="false">
        <condition>
            <condition field-name="inboxItemId" operator="is-not-empty"/>
        </condition>
        <actions>
            <entity-find-one entity-name="trade.swift.TradeInboxItem" value-field="inboxItem">
                <field-map field-name="inboxItemId"/>
            </entity-find-one>
            <if condition="inboxItem?.instrumentId &amp;&amp; inboxItem?.correlationStatusEnumId == 'CORR_AUTO_MATCH'">
                <!-- MT 750: Auto-spawn discrepant presentation -->
                <if condition="inboxItem.messageType == '750'">
                    <service-call name="trade.swift.InboundActionServices.spawnPresentation#Mt750"
                                  in-map="[inboxItemId: inboxItemId]"/>
                </if>
                <!-- MT 754: Auto-spawn clean presentation -->
                <if condition="inboxItem.messageType == '754'">
                    <service-call name="trade.swift.InboundActionServices.spawnPresentation#Mt754"
                                  in-map="[inboxItemId: inboxItemId]"/>
                </if>
                <!-- MT 742: Auto-match reimbursement -->
                <if condition="inboxItem.messageType == '742'">
                    <service-call name="trade.swift.InboundActionServices.matchReimbursement#Mt742"
                                  in-map="[inboxItemId: inboxItemId]"/>
                </if>
            </if>
        </actions>
    </seca>

</secas>
```

- [ ] **Step 2: Register new spec classes in TradeFinanceMoquiSuite.groovy**

```groovy
@Suite.SuiteClasses([
    // ... existing specs ...
    InboundSwiftSpec,
    InboundActionSpec
])
```

- [ ] **Step 3: Run full suite**

```bash
./gradlew :runtime:component:TradeFinance:test --tests "trade.TradeFinanceMoquiSuite" -x jar
```

Expected: ALL PASS

- [ ] **Step 4: Commit**

```bash
git add service/InboundSwift.secas.xml src/test/groovy/trade/TradeFinanceMoquiSuite.groovy
git commit -m "feat(inbound): SECA wiring for auto-spawn/auto-match + full suite registration"
```

---

## Verification Plan

### Automated Tests
- `InboundSwiftSpec`: Ingestion (hash, dedup, parse), Correlation (Tag 21, orphan, sub-correlation)
- `InboundActionSpec`: MT 730 (flag set), MT 799 (accept/reject/merge), MT 750 (auto-spawn + SECA bypass), MT 754 (clean fast-track), MT 742 (auto-match + exception), Discrepancy resolution (upload gate)
- Full `TradeFinanceMoquiSuite` regression

### Manual Verification
- Drop sample `.txt` files in `/runtime/swift/inbound/` and verify polling + archive
- Upload a file via manual upload and verify dedup
- Verify dashboard badge count (after UI task)


