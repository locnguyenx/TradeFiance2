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
- Modify: `runtime/component/TradeFinance/entity/TradeCommonEntities.xml`

- [ ] **Step 1: Read current TradeCommonEntities.xml to find TradeInstrument definition**

- [ ] **Step 2: Add TradeTransaction entity**

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

- [ ] **Step 3: Remove migrated fields from TradeInstrument**

Remove these fields from TradeInstrument entity (lines 24-34):
- transactionDate
- transactionTypeEnumId  
- transactionStatusId
- makerUserId
- makerTimestamp
- checkerUserId
- checkerTimestamp
- rejectionReason
- versionNumber
- lastUpdateTimestamp
- priorityEnumId

Keep: previousBusinessStateId (for Hold/Release)

- [ ] **Step 4: Update TradeApprovalRecord to link to TradeTransaction**

Check existing TradeApprovalRecord - add transactionId relationship:

```xml
    <relationship type="one" related="trade.TradeTransaction">
        <key-map field-name="transactionId"/>
    </relationship>
```

- [ ] **Step 5: Update TradeTransactionAudit to link to TradeTransaction**

Check existing TradeTransactionAudit - add transactionId field/references if missing

- [ ] **Step 6: Commit**

```bash
git add runtime/component/TradeFinance/entity/TradeCommonEntities.xml
git commit -m "feat: add TradeTransaction entity for authorization processing"
```

---

### Task 2: Add TransactionType Enumerations

**Files:**
- Modify: `runtime/component/TradeFinance/data/TradeFinanceSeedData.xml`

- [ ] **Step 1: Add TradeTransactionType enum type**

Read file to find where to add (around line 140 after StatusType definitions):

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
    
    <!-- Future: Collections -->
    <moqui.basic.Enumeration enumId="COL_IMP_NEW" enumTypeId="TradeTransactionType" description="Import Collection" sequenceNum="21"/>
    <moqui.basic.Enumeration enumId="COL_EXP_NEW" enumTypeId="TradeTransactionType" description="Export Collection" sequenceNum="22"/>
```

- [ ] **Step 2: Commit**

```bash
git add runtime/component/TradeFinance/data/TradeFinanceSeedData.xml
git commit -m "feat: add TradeTransactionType enumerations"
```

---

### Task 3: Update create#ImportLetterOfCredit to Create TradeTransaction

**Files:**
- Modify: `runtime/component/TradeFinance/service/trade/importlc/ImportLcServices.xml`

- [ ] **Step 1: Read ImportLcServices.xml to find create#ImportLetterOfCredit (lines 6-70)**

- [ ] **Step 2: Update create#ImportLetterOfCredit service**

Find the create section (after creating TradeInstrument around line 31-44) and add TradeTransaction creation:

Replace existing TradeInstrument creation block, remove makerUserId/makerTimestamp from it:

```xml
            <!-- 1. Create TradeInstrument -->
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

- [ ] **Step 3: Remove old field assignments from ImportLetterOfCredit creation if any reference to maker/checker fields**

- [ ] **Step 4: Add transactionId to output-parameters** (if not already)

- [ ] **Step 5: Test by running: verify service runs without errors**

- [ ] **Step 6: Commit**

```bash
git add runtime/component/TradeFinance/service/trade/importlc/ImportLcServices.xml
git commit -m "feat: create TradeTransaction for LC issuance"
```

---

### Task 4: Update submit#ForApproval Logic

**Files:**
- Modify: `runtime/component/TradeFinance/service/trade/importlc/ImportLcServices.xml`

- [ ] **Step 1: Find or create submit service for Import LC**

Check if there's an existing "submit for approval" service. If not, this may be inline in the create or a separate service.

- [ ] **Step 2: When user submits LC for approval, update TradeTransaction status**

Find where transactionStatusId changes to TX_PENDING and ensure it updates TradeTransaction:

```xml
            <!-- Find the IMP_NEW transaction for this instrument -->
            <entity-find entity-name="trade.TradeTransaction" list="txList">
                <econdition field-name="instrumentId"/>
                <econdition field-name="transactionTypeEnumId" value="IMP_NEW"/>
                <order-by field-name="-transactionId"/>
            </entity-find>
            <if condition="txList">
                <service-call name="update#trade.TradeTransaction"
                    in-map="[transactionId: txList[0].transactionId, transactionStatusId: 'TX_PENDING']"/>
            </if>
```

- [ ] **Step 3: Also update ImportLetterOfCredit businessStateId to LC_PENDING**

- [ ] **Step 4: Commit**

---

### Task 5: Update approve#ImportLetterOfCredit to Use TradeTransaction

**Files:**
- Modify: `runtime/component/TradeFinance/service/trade/importlc/ImportLcServices.xml` (approve service around lines 131-195)

- [ ] **Step 1: Update approve service to use TradeTransaction for authorization**

Replace the approval logic to:

```xml
    <service verb="approve" noun="ImportLetterOfCredit" authenticate="false">
        <in-parameters>
            <parameter name="instrumentId" required="true"/>
            <parameter name="approverUserId"/>
            <parameter name="approvalComments"/>
        </in-parameters>
        <actions>
            <script>ec.artifactExecution.disableAuthz()</script>
            <entity-find-one entity-name="trade.TradeInstrument" value-field="instrument" auto-field-map="true"/>
            <if condition="!instrument"><return error="true" message="Letter of Credit not found with ID ${instrumentId}"/></if>

            <!-- 1. Find the IMP_NEW transaction -->
            <entity-find entity-name="trade.TradeTransaction" list="txList">
                <econdition field-name="instrumentId"/>
                <econdition field-name="transactionTypeEnumId" value="IMP_NEW"/>
                <order-by field-name="-transactionId"/>
            </entity-find>
            <set field="tx" from="txList ? txList[0] : null"/>
            <if condition="!tx"><return error="true" message="No issuance transaction found"/></if>

            <!-- 2. Validate Authority using transaction -->
            <service-call name="trade.AuthorizationServices.evaluate#MakerCheckerMatrix" 
                          in-map="[transactionId: tx.transactionId, userId: approverUserId]" out-map="authOut"/>
            <if condition="!authOut.isAuthorized">
                <return error="true" message="User ${approverUserId} is not authorized: ${authOut.reason ?: 'Limit exceeded or Maker/Checker conflict'}"/>
            </if>

            <!-- 3. Dual Checker Logic for Tier 4 -->
            <set field="isFinalApproval" from="true"/>
            <if condition="authOut.authorityTierEnumId == 'TIER_4'">
                <script>
                    def existing = ec.entity.find("trade.TradeApprovalRecord")
                        .condition("transactionId", tx.transactionId)
                        .condition("versionSnapshot", tx.versionNumber)
                        .list()
                    if (existing.isEmpty()) {
                        context.isFinalApproval = false
                    } else {
                        for (prevAppr in existing) {
                            if (prevAppr.approverUserId == approverUserId) {
                                throw new org.moqui.service.ServiceException("Dual approval required: Current user has already approved this version.")
                            }
                        }
                    }
                </script>
            </if>

            <!-- 4. Create Approval Record -->
            <service-call name="create#trade.TradeApprovalRecord"
                in-map="[
                    transactionId: tx.transactionId,
                    instrumentId: instrumentId,
                    approverUserId: approverUserId,
                    approvalTimestamp: ec.user.nowTimestamp,
                    approvalComments: approvalComments,
                    versionSnapshot: tx.versionNumber,
                    approvalDecision: 'APPROVED'
                ]"/>

            <!-- 5. Update Transaction Status -->
            <if condition="isFinalApproval">
                <service-call name="update#trade.TradeTransaction"
                    in-map="[transactionId: tx.transactionId, 
                             transactionStatusId: 'TX_APPROVED',
                             checkerUserId: approverUserId, 
                             checkerTimestamp: ec.user.nowTimestamp]"/>
                
                <!-- 6. Update Business State to LC_ISSUED -->
                <entity-find-one entity-name="trade.importlc.ImportLetterOfCredit" value-field="lc" for-update="true">
                    <field-map field-name="instrumentId"/>
                </entity-find-one>
                <if condition="lc">
                    <set field="lc.businessStateId" value="LC_ISSUED"/>
                    <entity-update value-field="lc"/>
                </if>
            </if>
            <else>
                <message>First approval recorded for Tier 4 transaction. Waiting for second checker.</message>
            </else>
        </actions>
    </service>
```

- [ ] **Step 2: Commit**

```bash
git add runtime/component/TradeFinance/service/trade/importlc/ImportLcServices.xml
git commit -m "feat: update approval to use TradeTransaction and support dual-checker"
```

---

### Task 6: Update AuthorizationServices to Use TradeTransaction

**Files:**
- Modify: `runtime/component/TradeFinance/service/trade/AuthorizationServices.xml`

- [ ] **Step 1: Update evaluate#MakerCheckerMatrix**

Read current service (lines 6-61), update to accept transactionId:

Replace the entire service with:

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
            
            <!-- Resolve instrument from transaction or direct -->
            <set field="localInstrumentId" from="instrumentId"/>
            <if condition="transactionId">
                <entity-find-one entity-name="trade.TradeTransaction" value-field="tx">
                    <field-map field-name="transactionId"/>
                </entity-find-one>
                <if condition="tx">
                    <set field="localInstrumentId" from="tx.instrumentId"/>
                </if>
            </if>
            
            <entity-find-one entity-name="trade.TradeInstrument" value-field="instrument">
                <field-map field-name="instrumentId" from="localInstrumentId"/>
            </entity-find-one>
            <if condition="!instrument">
                <return error="true" message="Instrument not found"/>
            </if>

            <!-- Get evaluation amount -->
            <set field="evaluationAmount" from="instrument.amount ?: 0"/>
            <if condition="instrument.instrumentTypeEnumId == 'IMPORT_LC'">
                <entity-find-one entity-name="trade.importlc.ImportLetterOfCredit" value-field="lc">
                    <field-map field-name="instrumentId"/>
                </entity-find-one>
                <if condition="lc?.effectiveAmount">
                    <set field="evaluationAmount" from="lc.effectiveAmount"/>
                </if>
            </if>
            
            <!-- Check Four-Eyes: Cannot approve own submission -->
            <if condition="transactionId">
                <entity-find-one entity-name="trade.TradeTransaction" value-field="makerTx">
                    <field-map field-name="transactionId"/>
                </entity-find-one>
                <if condition="makerTx?.makerUserId == userId">
                    <set field="isAuthorized" from="false" type="Boolean"/>
                    <return/>
                </if>
            </if>
            <if condition="!transactionId">
                <entity-find entity-name="trade.TradeTransactionAudit" list="makerAudits">
                    <econdition field-name="instrumentId" from="localInstrumentId"/>
                    <econdition field-name="actionEnumId" value="MAKER_COMMIT"/>
                </entity-find>
                <if condition="makerAudits &amp;&amp; makerAudits[0].userId == userId">
                    <set field="isAuthorized" from="false" type="Boolean"/>
                    <return/>
                </if>
            </if>
            
            <!-- Check User Authority Tier -->
            <entity-find entity-name="trade.UserAuthorityProfile" list="profiles">
                <econdition field-name="userId"/>
                <econdition field-name="isSuspended" operator="not-equals" value="Y"/>
                <order-by field-name="customLimit"/>
            </entity-find>
            <if condition="!profiles">
                <set field="isAuthorized" from="false" type="Boolean"/>
                <return/>
            </if>
            
            <set field="isAuthorized" value="false" type="Boolean"/>
            <iterate list="profiles" entry="profile">
                <if condition="evaluationAmount &lt;= (profile.customLimit ?: BigDecimal.ZERO)">
                    <set field="isAuthorized" value="true" type="Boolean"/>
                    <set field="authorityTierEnumId" from="profile.delegationTierId"/>
                    <break/>
                </if>
            </iterate>
        </actions>
    </service>
```

- [ ] **Step 2: Update get#PendingApprovals**

Replace the query to use TradeTransaction:

```xml
            <entity-find entity-name="trade.TradeTransaction" list="pendingList">
                <econdition field-name="transactionStatusId" value="TX_PENDING"/>
                <order-by field-name="-makerTimestamp"/>
            </entity-find>
            <set field="approvalsList" from="[]"/>
            <iterate list="pendingList" entry="tx">
                <entity-find-one entity-name="trade.TradeInstrument" value-field="inst">
                    <field-map field-name="instrumentId" from="tx.instrumentId"/>
                </entity-find-one>
                <set field="item" from="[
                    instrumentId: tx.instrumentId,
                    transactionRef: inst?.transactionRef,
                    transactionId: tx.transactionId,
                    module: inst?.instrumentTypeEnumId == 'IMPORT_LC' ? 'IMPORT LC' : inst?.instrumentTypeEnumId,
                    action: 'Authorization',
                    makerUserId: tx.makerUserId,
                    baseEquivalentAmount: inst?.baseEquivalentAmount,
                    priorityEnumId: tx.priorityEnumId ?: 'NORMAL',
                    transactionStatusId: tx.transactionStatusId,
                    transactionTypeEnumId: tx.transactionTypeEnumId,
                    timeInQueue: '0h'
                ]"/>
                <if condition="tx.makerTimestamp">
                    <set field="diffMs" from="ec.user.nowTimestamp.time - tx.makerTimestamp.time"/>
                    <set field="hours" from="(diffMs / (1000 * 60 * 60)).toInteger()"/>
                    <set field="item.timeInQueue" from="hours + 'h'"/>
                </if>
                <script>approvalsList.add(item)</script>
            </iterate>
```

- [ ] **Step 3: Commit**

```bash
git add runtime/component/TradeFinance/service/trade/AuthorizationServices.xml
git commit -m "feat: update AuthorizationServices to use TradeTransaction"
```

---

### Task 7: Update reject#Instrument Service

**Files:**
- Modify: `runtime/component/TradeFinance/service/trade/AuthorizationServices.xml`

- [ ] **Step 1: Update reject#Instrument (lines 116-127)**

Replace to use TradeTransaction:

```xml
    <service verb="reject" noun="Instrument" authenticate="false">
        <in-parameters>
            <parameter name="instrumentId" required="true"/>
            <parameter name="rejectionReason"/>
            <parameter name="userId" default="ec.user.userId ?: 'EXOR_SERVICE_USER'"/>
        </in-parameters>
        <actions>
            <script>ec.artifactExecution.disableAuthz()</script>
            
            <!-- Find pending transaction -->
            <entity-find entity-name="trade.TradeTransaction" list="txList">
                <econdition field-name="instrumentId"/>
                <econdition field-name="transactionStatusId" value="TX_PENDING"/>
                <order-by field-name="-transactionId"/>
            </entity-find>
            <if condition="txList">
                <service-call name="update#trade.TradeTransaction" 
                              in-map="[transactionId: txList[0].transactionId, 
                                       transactionStatusId: 'TX_DRAFT', 
                                       rejectionReason: rejectionReason,
                                       checkerUserId: userId,
                                       checkerTimestamp: ec.user.nowTimestamp]"/>
                
                <!-- Create audit record -->
                <service-call name="create#trade.TradeTransactionAudit"
                    in-map="[transactionId: txList[0].transactionId,
                             instrumentId: instrumentId,
                             userId: userId,
                             actionEnumId: 'CHECKER_REJECT',
                             oldValue: 'TX_PENDING',
                             newValue: 'TX_DRAFT',
                             justificationRootText: rejectionReason]"/>
            </if>
            
            <!-- Update business state to LC_DRAFT -->
            <entity-find-one entity-name="trade.importlc.ImportLetterOfCredit" value-field="lc" for-update="true">
                <field-map field-name="instrumentId"/>
            </entity-find-one>
            <if condition="lc &amp;&amp; lc.businessStateId != 'LC_DRAFT'">
                <set field="oldBusinessState" from="lc.businessStateId"/>
                <set field="lc.businessStateId" value="LC_DRAFT"/>
                <entity-update value-field="lc"/>
                
                <service-call name="create#trade.TradeTransactionAudit"
                    in-map="[instrumentId: instrumentId,
                             userId: userId,
                             actionEnumId: 'BUSINESS_STATE_CHANGE',
                             oldValue: oldBusinessState,
                             newValue: 'LC_DRAFT']"/>
            </if>
        </actions>
    </service>
```

- [ ] **Step 2: Commit**

---

### Task 8: Update Amendment Services for TradeTransaction

**Files:**
- Modify: `runtime/component/TradeFinance/service/trade/importlc/ImportLcServices.xml` (create#Amendment around line 197)

- [ ] **Step 1: Update create#Amendment to create TradeTransaction**

Find the create#Amendment service and update to create TradeTransaction after creating ImportLcAmendment:

```xml
            <!-- Existing: Create ImportLcAmendment -->
            <service-call name="create#trade.importlc.ImportLcAmendment" out-map="amdOut"
                in-map="context + [amendmentBusinessStateId:amendmentBusinessStateId, amendmentNumber:amendmentNumber, beneficiaryConsentStatusId:beneficiaryConsentStatusId]"/>
            
            <!-- NEW: Create TradeTransaction for Amendment -->
            <service-call name="create#trade.TradeTransaction"
                in-map="[
                    instrumentId: instrumentId,
                    transactionTypeEnumId: 'IMP_AMENDMENT',
                    transactionStatusId: 'TX_DRAFT',
                    transactionDate: ec.user.nowTimestamp,
                    makerUserId: ec.user.userId,
                    makerTimestamp: ec.user.nowTimestamp,
                    versionNumber: 1,
                    relatedRecordId: amdOut.amendmentId,
                    relatedRecordType: 'AMENDMENT'
                ]"/>
```

- [ ] **Step 2: Update authorize#Amendment**

Find authorize#Amendment (around line 267) and update to:

```xml
            <!-- Find the IMP_AMENDMENT transaction -->
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

- [ ] **Step 3: Update update#Amendment for beneficiary consent**

Ensure when beneficiary consent is received, the transaction is properly transitioned

- [ ] **Step 4: Commit**

---

### Task 9: Update Presentation Services for TradeTransaction

**Files:**
- Modify: `runtime/component/TradeFinance/service/trade/importlc/ImportLcServices.xml`

- [ ] **Step 1: Find/create presentation creation service**

Look for service that creates TradeDocumentPresentation (around line 376, examine#Documents or similar)

- [ ] **Step 2: Add TradeTransaction creation**

After creating TradeDocumentPresentation:

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

- [ ] **Step 3: Update submit for examination (transition to TX_PENDING)**

- [ ] **Step 4: Update authorize#Presentation to update transaction**

- [ ] **Step 5: Commit**

---

### Task 10: Update Settlement Services for TradeTransaction

**Files:**
- Modify: `runtime/component/TradeFinance/service/trade/importlc/ImportLcServices.xml` (settle#Presentation around line 354)

- [ ] **Step 1: Add TradeTransaction creation**

In the settle service, create transaction:

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

- [ ] **Step 2: Update approval to mark TX_APPROVED and execute settlement**

- [ ] **Step 3: Commit**

---

### Task 11: Update LimitServices

**Files:**
- Modify: `runtime/component/TradeFinance/service/trade/LimitServices.xml`

- [ ] **Step 1: Find all references to transactionStatusId on TradeInstrument**

- [ ] **Step 2: Update to check TradeTransaction instead**

For limit calculation (checking utilization):

```groovy
// Only count towards utilization if transaction is TX_APPROVED
def approvedTransactions = findTradeTransaction where 
    instrumentId = instrumentId and 
    transactionStatusId = 'TX_APPROVED'
```

- [ ] **Step 3: Commit**

---

### Task 12: Update SwiftGenerationServices

**Files:**
- Modify: `runtime/component/TradeFinance/service/trade/SwiftGenerationServices.xml`

- [ ] **Step 1: Find where SWIFT generation is triggered**

- [ ] **Step 2: Update to check TradeTransaction for final status**

```xml
            <entity-find entity-name="trade.TradeTransaction" list="txList">
                <econdition field-name="instrumentId"/>
                <econdition field-name="transactionStatusId" value="TX_APPROVED"/>
                <order-by field-name="-transactionId"/>
            </entity-find>
            <if condition="!txList">
                <return error="true" message="Cannot generate SWIFT - transaction not approved"/>
            </if>
```

- [ ] **Step 3: Commit**

---

### Task 12b: Update TradeAccountingServices

**Files:**
- Modify: `runtime/component/TradeFinance/service/trade/TradeAccountingServices.xml`

- [ ] **Step 1: Check for transaction status references**
- [ ] **Step 2: Update to verify via TradeTransaction**

---

### Task 13: Update SECA (Security Condition)

**Files:**
- Modify: `runtime/component/TradeFinance/service/TradeFinanceSeca.xml`

- [ ] **Step 1: Read SECA file**

- [ ] **Step 2: Update triggers from TradeInstrument to TradeTransaction**

Change:
```xml
<expression>transactionStatusId == 'TX_APPROVED' &amp;&amp; oldValueMap?.transactionStatusId != 'TX_APPROVED'</expression>
```

To check TradeTransaction:
```xml
        <entity-find-one entity-name="trade.TradeTransaction" value-field="tx">
            <field-map field-name="transactionId" from="relatedRecordId"/>
        </entity-find-one>
        <if condition="tx?.transactionStatusId == 'TX_APPROVED'">
```

- [ ] **Step 3: Commit**

---

### Task 14: Remove lifecycleStatusId Usage

**Files:**
- Modify: Multiple service files

- [ ] **Step 1: Find all lifecycleStatusId usages**

```bash
grep -r "lifecycleStatusId" runtime/component/TradeFinance/
```

- [ ] **Step 2: Update code to use businessStateId**

In services that set both:
```xml
<!-- Remove this: -->
lifecycleStatusId: businessStateId
```

- [ ] **Step 3: Remove lifecycleStatusId field from TradeInstrument entity definition**

- [ ] **Step 4: Commit**

---

### Task 15: Update Seed Data

**Files:**
- Modify: `runtime/component/TradeFinance/data/TradeFinanceMasterData.xml`

- [ ] **Step 1: Remove migrated fields from TradeInstrument seed data**

Remove: lifecycleStatusId, transactionStatusId, makerUserId, etc.

- [ ] **Step 2: Add TradeTransaction seed records**

```xml
    <!-- Existing LC: LC240001 -->
    <trade.TradeTransaction transactionId="TX240001" instrumentId="LC240001" 
        transactionTypeEnumId="IMP_NEW" transactionStatusId="TX_APPROVED" 
        makerUserId="EXOR_SERVICE_USER" makerTimestamp="2026-04-20 10:00:00"
        checkerUserId="EXOR_SERVICE_USER" checkerTimestamp="2026-04-20 11:00:00"
        versionNumber="1" transactionDate="2026-04-20"/>
    
    <!-- LC240002 - still pending -->
    <trade.TradeTransaction transactionId="TX240002" instrumentId="LC240002" 
        transactionTypeEnumId="IMP_NEW" transactionStatusId="TX_PENDING" 
        makerUserId="EXOR_SERVICE_USER" makerTimestamp="2026-04-21 10:00:00"
        versionNumber="1" transactionDate="2026-04-21"/>
```

- [ ] **Step 3: Commit**

---

### Task 16: Run Tests and Verify

**Files:**
- Test: Existing specs in `src/test/groovy/`

- [ ] **Step 1: Run all TradeFinance tests**

```bash
cd /Users/me/myprojects/moqui-trade
./gradlew test --tests "*Trade*" 2>&1 | head -150
```

- [ ] **Step 2: Fix any failures**

- [ ] **Step 3: Commit test fixes if any**

---

### Task 17: Create Migration Script

**Files:**
- Create: `runtime/component/TradeFinance/data/TradeTransactionMigration.xml`

- [ ] **Step 1: Create data migration file**

Creates TradeTransaction records for existing instruments:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<data xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:noNamespaceSchemaLocation="http://moqui.org/xsd/entity-data-3.0.xsd">
    
    <!-- Migration: Create TradeTransaction for existing Import LCs -->
    <!-- This data is only loaded once during migration -->
    
    <!-- Load existing TradeInstrument records and create corresponding TradeTransaction -->
    <!-- Run as SQL for bulk migration:
    INSERT INTO trade_transaction (transaction_id, instrument_id, transaction_type_enum_id,
        transaction_status_id, transaction_date, maker_user_id, maker_timestamp,
        checker_user_id, checker_timestamp, version_number, priority_enum_id,
        created_stamp, last_updated_stamp)
    SELECT ti.instrument_id, ti.instrument_id, 
        CASE ti.instrument_type_enum_id 
            WHEN 'IMPORT_LC' THEN 'IMP_NEW'
            ELSE ti.instrument_type_enum_id 
        END,
        ti.transaction_status_id, ti.transaction_date, ti.maker_user_id, ti.maker_timestamp,
        ti.checker_user_id, ti.checker_timestamp, COALESCE(ti.version_number, 1), ti.priority_enum_id,
        ti.created_stamp, ti.last_updated_stamp
    FROM trade_instrument ti
    WHERE ti.instrument_type_enum_id IN ('IMPORT_LC');
    -->
    
</data>
```

- [ ] **Step 2: Document migration steps in README or migration guide**

---

## Implementation Notes

1. **Sequence:**
   - Task 1-2: Entity and enum setup
   - Task 3-4: LC Issuance flow
   - Task 5-7: Authorization services
   - Task 8-10: Amendment, Presentation, Settlement
   - Task 11-13: Integration services
   - Task 14-15: Cleanup
   - Task 16-17: Verification

2. **Dual-read during transition:** Services should check TradeTransaction first, fall back to TradeInstrument fields for legacy data during migration period

3. **Test after each task:** Run existing tests to ensure nothing breaks

---

## Self-Review Checklist

- [ ] Task 1: TradeTransaction entity created
- [ ] Task 2: Transaction type enums seeded
- [ ] Task 3: LC creation creates transaction
- [ ] Task 4: Submit moves to TX_PENDING
- [ ] Task 5: Approval uses transaction for auth
- [ ] Task 6: AuthorizationServices handles both
- [ ] Task 7: Reject updates transaction
- [ ] Task 8-10: Amendment/Presentation/Settlement all use transaction
- [ ] Task 11-13: Limit/SWIFT/Accounting use transaction
- [ ] Task 14: lifecycleStatusId removed
- [ ] Task 15: Seed data updated
- [ ] Task 16: Tests pass
- [ ] Task 17: Migration documented

---

**Plan Version:** 1.1 (Comprehensive)
**Created:** April 28, 2026
**Status:** Ready for Execution