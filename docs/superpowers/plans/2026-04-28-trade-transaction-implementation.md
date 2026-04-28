# TradeTransaction Entity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create TradeTransaction entity to properly separate Authorization Processing (Transaction) from Instrument Data (LC/Collection), fixing the conceptual confusion between "Transaction State" and "Business State" per BRD REQ-IMP-DEF-01 and REQ-IMP-DEF-02.

**Architecture:** 
- New `TradeTransaction` entity holds authorization processing fields (maker/checker, status, versioning)
- Instrument data stays in `TradeInstrument` and product-specific entities (ImportLetterOfCredit, etc.)
- Transaction types defined as enums with product prefix (IMP_NEW, IMP_AMENDMENT, etc.)
- Services updated to create/manage TradeTransaction for each processing flow

**Tech Stack:** Moqui Framework (XML entities/services), existing TradeFinance component

---

## Task Breakdown

### Task 1: Create TradeTransaction Entity

**Files:**
- Create: `runtime/component/TradeFinance/entity/TradeCommonEntities.xml` (add new entity)
- Modify: `runtime/component/TradeFinance/entity/TradeCommonEntities.xml:6-43` (remove migrated fields from TradeInstrument)

- [ ] **Step 1: Add TradeTransaction entity to TradeCommonEntities.xml**

Add after line 169 (after NumberSequence entity):

```xml
    <entity entity-name="TradeTransaction" package="trade">
        <field name="transactionId" type="id" is-pk="true"/>
        <field name="instrumentId" type="id"/>
        <field name="transactionTypeEnumId" type="id"/>
        <field name="transactionStatusId" type="id"/>
        <field name="transactionDate" type="date"/>
        
        <!-- Maker Info -->
        <field name="makerUserId" type="id"/>
        <field name="makerTimestamp" type="date-time"/>
        
        <!-- Checker Info -->
        <field name="checkerUserId" type="id"/>
        <field name="checkerTimestamp" type="date-time"/>
        
        <!-- Authorization -->
        <field name="rejectionReason" type="text-long"/>
        <field name="versionNumber" type="number-integer" default="1"/>
        <field name="priorityEnumId" type="id"/>
        
        <!-- Payload Link -->
        <field name="relatedRecordId" type="id"/>
        <field name="relatedRecordType" type="id"/>
        
        <relationship type="one" related="trade.TradeInstrument">
            <key-map field-name="instrumentId"/>
        </relationship>
    </entity>
```

- [ ] **Step 2: Remove migrated fields from TradeInstrument**
- Remove lines 24-34 from TradeInstrument entity:
  - transactionDate, transactionTypeEnumId, transactionStatusId
  - makerUserId, makerTimestamp, checkerUserId, checkerTimestamp
  - rejectionReason, versionNumber, lastUpdateTimestamp, priorityEnumId

- [ ] **Step 3: Keep previousBusinessStateId**
- Verify it's still in TradeInstrument (for Hold/Release)

- [ ] **Step 4: Commit**

```bash
git add runtime/component/TradeFinance/entity/TradeCommonEntities.xml
git commit -m "feat: add TradeTransaction entity for authorization processing"
```

---

### Task 2: Add TransactionType Enumerations

**Files:**
- Modify: `runtime/component/TradeFinance/data/TradeFinanceSeedData.xml:140-160`

- [ ] **Step 1: Add TransactionType enum type**

Add after StatusType definitions (around line 140):

```xml
    <moqui.basic.EnumerationType enumTypeId="TradeTransactionType" description="Type of Trade Transaction"/>
    
    <!-- Import LC Transactions -->
    <moqui.basic.Enumeration enumId="IMP_NEW" enumTypeId="TradeTransactionType" description="Import LC Issuance" sequenceNum="1"/>
    <moqui.basic.Enumeration enumId="IMP_AMENDMENT" enumTypeId="TradeTransactionType" description="Import LC Amendment" sequenceNum="2"/>
    <moqui.basic.Enumeration enumId="IMP_PRESENTATION" enumTypeId="TradeTransactionType" description="Import LC Document Presentation" sequenceNum="3"/>
    <moqui.basic.Enumeration enumId="IMP_SETTLEMENT" enumTypeId="TradeTransactionType" description="Import LC Settlement" sequenceNum="4"/>
    <moqui.basic.Enumeration enumId="IMP_CANCELLATION" enumTypeId="TradeTransactionType" description="Import LC Cancellation" sequenceNum="5"/>
    
    <!-- Future: Export LC Transactions -->
    <moqui.basic.Enumeration enumId="EXP_NEW" enumTypeId="TradeTransactionType" description="Export LC Issuance" sequenceNum="11"/>
    <moqui.basic.Enumeration enumId="EXP_AMENDMENT" enumTypeId="TradeTransactionType" description="Export LC Amendment" sequenceNum="12"/>
```

- [ ] **Step 2: Commit**

```bash
git add runtime/component/TradeFinance/data/TradeFinanceSeedData.xml
git commit -m "feat: add TradeTransactionType enumerations"
```

---

### Task 3: Update create#ImportLetterOfCredit to Create TradeTransaction

**Files:**
- Modify: `runtime/component/TradeFinance/service/trade/importlc/ImportLcServices.xml:6-70`

- [ ] **Step 1: Update create#ImportLetterOfCredit service**

Find the create#ImportLetterOfCredit service (lines 6-70) and add TradeTransaction creation after creating TradeInstrument:

Replace existing logic (after line 44 where instrument is created):

```xml
            <!-- 1. Create TradeInstrument (existing) -->
            <service-call name="create#trade.TradeInstrument" out-map="createInstOut"
                in-map="[
                    transactionRef: transactionRef,
                    instrumentTypeEnumId: 'IMPORT_LC',
                    businessStateId: 'LC_DRAFT',
                    amount: lcAmount,
                    currencyUomId: lcCurrencyUomId,
                    expiryDate: expiryDate,
                    baseEquivalentAmount: lcAmount
                ]"/>
            <set field="instrumentId" from="createInstOut.instrumentId"/>

            <!-- 2. Create TradeTransaction for Issuance -->
            <service-call name="create#trade.TradeTransaction" out-map="txOut"
                in-map="[
                    instrumentId: instrumentId,
                    transactionTypeEnumId: 'IMP_NEW',
                    transactionStatusId: 'TX_DRAFT',
                    transactionDate: ec.user.nowTimestamp,
                    makerUserId: ec.user.userId,
                    makerTimestamp: ec.user.nowTimestamp,
                    versionNumber: 1
                ]"/>
            <set field="transactionId" from="txOut.transactionId"/>
```

- [ ] **Step 2: Remove old field assignments from TradeInstrument creation**
- Remove any makerUserId, makerTimestamp from TradeInstrument creation
- Keep only instrument data

- [ ] **Step 3: Update output parameters**
- Add transactionId to output-parameters if needed for reference

- [ ] **Step 4: Commit**

```bash
git add runtime/component/TradeFinance/service/trade/importlc/ImportLcServices.xml
git commit -m "feat: create TradeTransaction for LC issuance"
```

---

### Task 4: Update approve#ImportLetterOfCredit to Use TradeTransaction

**Files:**
- Modify: `runtime/component/TradeFinance/service/trade/importlc/ImportLcServices.xml:131-195`

- [ ] **Step 1: Update approve#ImportLetterOfCredit**

Find approve service and update to use TradeTransaction:

Replace existing approval logic to update TradeTransaction instead of TradeInstrument:

```xml
    <!-- Find the active IMP_NEW transaction for this instrument -->
    <entity-find entity-name="trade.TradeTransaction" list="txList">
        <econdition field-name="instrumentId"/>
        <econdition field-name="transactionTypeEnumId" value="IMP_NEW"/>
        <order-by field-name="-transactionId"/>
    </entity-find>
    <set field="transactionId" from="txList ? txList[0].transactionId : null"/>
    
    <if condition="!transactionId">
        <return error="true" message="No issuance transaction found for instrument ${instrumentId}"/>
    </if>
    
    <!-- Validate Authority using transaction amount -->
    <service-call name="trade.AuthorizationServices.evaluate#MakerCheckerMatrix" 
                  in-map="[transactionId: transactionId, userId: approverUserId]" out-map="authOut"/>
```

- [ ] **Step 2: Update dual-checker logic to check TradeTransaction versionNumber**

- [ ] **Step 3: Update TradeTransaction status on approval**

After successful approval:
```xml
            <service-call name="update#trade.TradeTransaction"
                in-map="[transactionId: transactionId, 
                         transactionStatusId: 'TX_APPROVED',
                         checkerUserId: approverUserId, 
                         checkerTimestamp: ec.user.nowTimestamp]"/>
```

- [ ] **Step 4: Commit**

```bash
git add runtime/component/TradeFinance/service/trade/importlc/ImportLcServices.xml
git commit -m "feat: update approval to use TradeTransaction"
```

---

### Task 5: Update AuthorizationServices to Use TradeTransaction

**Files:**
- Modify: `runtime/component/TradeFinance/service/trade/AuthorizationServices.xml`

- [ ] **Step 1: Update evaluate#MakerCheckerMatrix**

Update the service to accept either instrumentId or transactionId, preferring transaction:

```xml
    <service verb="evaluate" noun="MakerCheckerMatrix" authenticate="false">
        <in-parameters>
            <parameter name="instrumentId"/>
            <parameter name="transactionId"/>
            <parameter name="userId" default="ec.user.userId ?: 'EXOR_SERVICE_USER'"/>
        </in-parameters>
        <out-parameters>
            <parameter name="isAuthorized" type="Boolean"/>
            <parameter name="authorityTierEnumId"/>
            <parameter name="evaluationAmount" type="BigDecimal"/>
        </out-parameters>
        <actions>
            <script>ec.artifactExecution.disableAuthz()</script>
            
            <!-- Get transaction and instrument info -->
            <if condition="transactionId">
                <entity-find-one entity-name="trade.TradeTransaction" value-field="tx" for-update="true">
                    <field-map field-name="transactionId"/>
                </entity-find-one>
                <set field="localInstrumentId" from="tx?.instrumentId"/>
            </if>
            <if condition="!transactionId &amp;&amp; instrumentId">
                <set field="localInstrumentId" from="instrumentId"/>
            </if>
            
            <entity-find-one entity-name="trade.TradeInstrument" value-field="instrument">
                <field-map field-name="instrumentId" from="localInstrumentId"/>
            </entity-find-one>
            <if condition="!instrument"><return error="true" message="Instrument not found"/></if>

            <!-- Use amount from transaction if available, else instrument -->
            <set field="evaluationAmount" from="instrument.amount ?: 0"/>
            <if condition="instrument.instrumentTypeEnumId == 'IMPORT_LC'">
                <entity-find-one entity-name="trade.importlc.ImportLetterOfCredit" value-field="lc">
                    <field-map field-name="instrumentId"/>
                </entity-find-one>
                <if condition="lc?.effectiveAmount"><set field="evaluationAmount" from="lc.effectiveAmount"/></if>
            </if>
            
            <!-- Check Four-Eyes: Maker cannot be Checker -->
            <if condition="transactionId">
                <entity-find-one entity-name="trade.TradeTransaction" value-field="makerTx">
                    <field-map field-name="transactionId" from="transactionId"/>
                </entity-find-one>
                <if condition="makerTx?.makerUserId == userId">
                    <set field="isAuthorized" from="false" type="Boolean"/>
                    <return/>
                </if>
            </if>
            
            <!-- Rest of tier evaluation logic remains the same -->
```
- Keep rest of existing logic for tier evaluation and custom limit checking

- [ ] **Step 2: Update get#PendingApprovals to query TradeTransaction**

Update get#PendingApprovals to use TradeTransaction.transactionStatusId:

```xml
            <entity-find entity-name="trade.TradeTransaction" list="pendingList">
                <econdition field-name="transactionStatusId" value="TX_PENDING"/>
                <order-by field-name="-makerTimestamp"/>
            </entity-find>
```

- [ ] **Step 3: Commit**

```bash
git add runtime/component/TradeFinance/service/trade/AuthorizationServices.xml
git commit -m "feat: update AuthorizationServices for TradeTransaction"
```

---

### Task 6: Update reject#Instrument Service

**Files:**
- Modify: `runtime/component/TradeFinance/service/trade/AuthorizationServices.xml:116-127`

- [ ] **Step 1: Update reject#Instrument**

Update to update TradeTransaction instead of TradeInstrument:

```xml
    <service verb="reject" noun="Instrument" authenticate="false">
        <in-parameters>
            <parameter name="instrumentId" required="true"/>
            <parameter name="rejectionReason"/>
            <parameter name="userId" default="ec.user.userId ?: 'EXOR_SERVICE_USER'"/>
        </in-parameters>
        <actions>
            <script>ec.artifactExecution.disableAuthz()</script>
            
            <!-- Find the pending transaction for this instrument -->
            <entity-find entity-name="trade.TradeTransaction" list="txList">
                <econdition field-name="instrumentId"/>
                <econdition field-name="transactionStatusId" value="TX_PENDING"/>
                <order-by field-name="-transactionId"/>
            </entity-find>
            <if condition="txList">
                <service-call name="update#trade.TradeTransaction" 
                              in-map="[transactionId: txList[0].transactionId, 
                                       transactionStatusId: 'TX_DRAFT', 
                                       rejectionReason: rejectionReason]"/>
            </if>
            
            <!-- Also update the business state if needed -->
            <entity-find-one entity-name="trade.importlc.ImportLetterOfCredit" value-field="lc" for-update="true">
                <field-map field-name="instrumentId"/>
            </entity-find-one>
            <if condition="lc">
                <set field="lc.businessStateId" value="LC_DRAFT"/>
                <entity-update value-field="lc"/>
            </if>
        </actions>
    </service>
```

- [ ] **Step 2: Commit**

```bash
git add runtime/component/TradeFinance/service/trade/AuthorizationServices.xml
git commit -m "feat: update reject to use TradeTransaction"
```

---

### Task 7: Update Amendment Services for TradeTransaction

**Files:**
- Modify: `runtime/component/TradeFinance/service/trade/importlc/ImportLcServices.xml:197-352`

- [ ] **Step 1: Update create#Amendment service**

After creating ImportLcAmendment entity, also create TradeTransaction:

```xml
            <!-- Save amendment first -->
            <service-call name="create#trade.importlc.ImportLcAmendment" out-map="amdOut"
                in-map="context + [amendmentBusinessStateId:amendmentBusinessStateId, amendmentNumber:amendmentNumber, beneficiaryConsentStatusId:beneficiaryConsentStatusId]"/>
            
            <!-- Create TradeTransaction for the amendment -->
            <service-call name="create#trade.TradeTransaction"
                in-map="[
                    instrumentId: instrumentId,
                    transactionTypeEnumId: 'IMP_AMENDMENT',
                    transactionStatusId: 'TX_DRAFT',  -- or TX_PENDING if auto-submit
                    transactionDate: ec.user.nowTimestamp,
                    makerUserId: ec.user.userId,
                    makerTimestamp: ec.user.nowTimestamp,
                    versionNumber: 1,
                    relatedRecordId: amdOut.amendmentId,
                    relatedRecordType: 'AMENDMENT'
                ]"/>
```

- [ ] **Step 2: Update authorize#Amendment**

Update to transition TradeTransaction status on approval:

```xml
            <!-- Update TradeTransaction -->
            <entity-find entity-name="trade.TradeTransaction" list="txList">
                <econdition field-name="instrumentId"/>
                <econdition field-name="transactionTypeEnumId" value="IMP_AMENDMENT"/>
                <order-by field-name="-transactionId"/>
            </entity-find>
            <if condition="txList">
                <service-call name="update#trade.TradeTransaction"
                    in-map="[transactionId: txList[0].transactionId,
                             transactionStatusId: 'TX_APPROVED',
                             checkerUserId: ec.user.userId,
                             checkerTimestamp: ec.user.nowTimestamp]"/>
            </if>
```

- [ ] **Step 3: Commit**

```bash
git add runtime/component/TradeFinance/service/trade/importlc/ImportLcServices.xml
git commit -m "feat: update Amendment services for TradeTransaction"
```

---

### Task 8: Update Presentation Services for TradeTransaction

**Files:**
- Modify: `runtime/component/TradeFinance/service/trade/importlc/ImportLcServices.xml`

- [ ] **Step 1: Create TradeTransaction for presentations**

In presentation creation flow (check create#Presentation or similar), add:

```xml
            <!-- Create TradeTransaction for document presentation -->
            <service-call name="create#trade.TradeTransaction"
                in-map="[
                    instrumentId: instrumentId,
                    transactionTypeEnumId: 'IMP_PRESENTATION',
                    transactionStatusId: 'TX_DRAFT',
                    transactionDate: ec.user.nowTimestamp,
                    makerUserId: ec.user.userId,
                    makerTimestamp: ec.user.nowTimestamp,
                    versionNumber: 1,
                    relatedRecordId: presentationId,
                    relatedRecordType: 'PRESENTATION'
                ]"/>
```

- [ ] **Step 2: Update authorize#Presentation to update transaction**

- [ ] **Step 3: Commit**

```bash
git add runtime/component/TradeFinance/service/trade/importlc/ImportLcServices.xml
git commit -m "feat: update Presentation services for TradeTransaction"
```

---

### Task 9: Update Settlement Services for TradeTransaction

**Files:**
- Modify: `runtime/component/TradeFinance/service/trade/importlc/ImportLcServices.xml:354-375`

- [ ] **Step 1: Create TradeTransaction for settlements**

Add to settlement flow:

```xml
            <!-- Create TradeTransaction for settlement -->
            <service-call name="create#trade.TradeTransaction"
                in-map="[
                    instrumentId: instrumentId,
                    transactionTypeEnumId: 'IMP_SETTLEMENT',
                    transactionStatusId: 'TX_DRAFT',
                    transactionDate: ec.user.nowTimestamp,
                    makerUserId: ec.user.userId,
                    makerTimestamp: ec.user.nowTimestamp,
                    versionNumber: 1,
                    relatedRecordId: settlementId,
                    relatedRecordType: 'SETTLEMENT'
                ]"/>
```

- [ ] **Step 2: Update to use TX_APPROVED on successful settlement**

- [ ] **Step 3: Commit**

```bash
git add runtime/component/TradeFinance/service/trade/importlc/ImportLcServices.xml
git commit -m "feat: update Settlement services for TradeTransaction"
```

---

### Task 10: Update LimitServices References

**Files:**
- Modify: `runtime/component/TradeFinance/service/trade/LimitServices.xml`

- [ ] **Step 1: Update limit calculation to use TradeTransaction**

Search for references to transactionStatusId on TradeInstrument and update to check TradeTransaction instead

- [ ] **Step 2: Commit**

```bash
git add runtime/component/TradeFinance/service/trade/LimitServices.xml
git commit -m "feat: update LimitServices for TradeTransaction"
```

---

### Task 11: Update SwiftGenerationServices

**Files:**
- Modify: `runtime/component/TradeFinance/service/trade/SwiftGenerationServices.xml`

- [ ] **Step 1: Update SWIFT message generation to check TradeTransaction**

- [ ] **Step 2: Commit**

```bash
git add runtime/component/TradeFinance/service/trade/SwiftGenerationServices.xml
git commit -m "feat: update SwiftGenerationServices for TradeTransaction"
```

---

### Task 12: Update SECA (Security Condition)

**Files:**
- Modify: `runtime/component/TradeFinance/service/TradeFinanceSeca.xml`

- [ ] **Step 1: Update SECA triggers**

Change from checking TradeInstrument.transactionStatusId to TradeTransaction:

```xml
    <eca-service service-name="create#trade.TradeTransaction" if-service="importlc.ImportLcServices.create#ImportLetterOfCredit">
        <expression>transactionStatusId == 'TX_APPROVED' &amp;&amp; oldValueMap?.transactionStatusId != 'TX_APPROVED'</expression>
```

- [ ] **Step 2: Commit**

```bash
git add runtime/component/TradeFinance/service/TradeFinanceSeca.xml
git commit -m "feat: update SECA for TradeTransaction"
```

---

### Task 13: Remove lifecycleStatusId Usage

**Files:**
- Modify: Multiple service files

- [ ] **Step 1: Find all lifecycleStatusId usages**

```bash
grep -r "lifecycleStatusId" runtime/component/TradeFinance/
```

- [ ] **Step 2: Replace with businessStateId where appropriate**

In services that set lifecycleStatusId = businessStateId, remove the redundant assignment

- [ ] **Step 3: Commit**

```bash
git add runtime/component/TradeFinance/
git commit -m "refactor: remove redundant lifecycleStatusId usage"
```

---

### Task 14: Update Seed Data

**Files:**
- Modify: `runtime/component/TradeFinance/data/TradeFinanceMasterData.xml`

- [ ] **Step 1: Remove migrated fields from seed data**

Remove lifecycleStatusId, transactionStatusId, makerUserId, etc. from TradeInstrument seed data

- [ ] **Step 2: Add TradeTransaction seed records**

For existing LCs, seed corresponding TradeTransaction records:

```xml
    <!-- Existing LC: LC240001 - should have IMP_NEW transaction -->
    <trade.TradeTransaction transactionId="TX240001" instrumentId="LC240001" 
        transactionTypeEnumId="IMP_NEW" transactionStatusId="TX_APPROVED" 
        makerUserId="EXOR_SERVICE_USER" makerTimestamp="2026-04-20 10:00:00"
        checkerUserId="EXOR_SERVICE_USER" checkerTimestamp="2026-04-20 11:00:00"
        versionNumber="1" transactionDate="2026-04-20"/>
```

- [ ] **Step 3: Commit**

```bash
git add runtime/component/TradeFinance/data/TradeFinanceMasterData.xml
git commit -m "refactor: update seed data for TradeTransaction"
```

---

### Task 15: Update Test Data

**Files:**
- Modify: `runtime/component/TradeFinance/data/TradeFinanceUsers.xml`

- [ ] **Step 1: Check if user auth data needs updates**

- [ ] **Step 2: Commit if needed**

---

### Task 16: Run Tests and Verify

**Files:**
- Test: Existing Spock tests in `src/test/groovy/`

- [ ] **Step 1: Run existing tests**

```bash
./gradlew test --tests "*ImportLc*" 2>&1 | head -100
```

- [ ] **Step 2: Fix any failures**

- [ ] **Step 3: Commit test fixes**

---

## Implementation Notes

1. **Dual-read during transition:** Services should check TradeTransaction first, fall back to TradeInstrument fields for legacy data

2. **Transaction type validation:** Each transaction type (IMP_NEW, IMP_AMENDMENT, etc.) maps to specific payload entities

3. **Version tracking:** versionNumber stays on TradeTransaction, incremented on each approval cycle (for dual-checker)

4. **Business state vs transaction state:** 
   - businessStateId on ImportLetterOfCredit = LC lifecycle (LC_ISSUED, LC_DOCS_RECEIVED, etc.)
   - transactionStatusId on TradeTransaction = Authorization flow (TX_DRAFT, TX_PENDING, TX_APPROVED)

---

## Self-Review Checklist

- [ ] All services updated to create TradeTransaction for new flows
- [ ] AuthorizationServices handles both transactionId and instrumentId
- [ ] Dual-checker logic works with versionNumber on TradeTransaction
- [ ] No orphaned maker/checker fields on TradeInstrument
- [ ] Tests pass with new entity
- [ ] Seed data correctly populates both entities

---

**Plan Version:** 1.0
**Created:** April 28, 2026
**Status:** Ready for Execution