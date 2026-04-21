# Moqui Service Patterns

> Verified against Moqui Framework (174+ service definitions)

## 1. Service Naming Convention

### verb#noun Pattern
| Verb | Purpose | Example |
|------|---------|---------|
| `create` | New record | `create#LetterOfCredit` |
| `update` | Modify record | `update#LetterOfCredit` |
| `delete` | Remove record | `delete#LetterOfCredit` |
| `get` | Retrieve data | `get#LcDetails` |
| `find` | Search records | `find#LcByStatus` |
| `transition` | Status change | `transition#Status` |
| `validate` | Business rule check | `validate#Amendment` |

### Full Path
```xml
<service verb="create" noun="LetterOfCredit" 
         type="interface"
         location="moqui.service.sec.SimpleServices">
```
- Use full package path: `moqui.trade.finance.TradeFinanceServices.create#LcDrawing`

## 2. Service Definition Structure

### Standard CRUD Service
```xml
<service verb="create" noun="LetterOfCredit">
    <description>Create a new Letter of Credit</description>
    <in-parameters>
        <parameter name="lcNumber" type="String" required="true"/>
        <parameter name="lcAmount" type="BigDecimal" required="false"/>
        <parameter name="lcStatusId" type="String" default-value="LcDraft"/>
    </in-parameters>
    <out-parameters>
        <parameter name="lcId" type="String"/>
    </out-parameters>
    <actions>
        <!-- Validation -->
        <if condition="!lcNumber">
            <return error="true" message="LC Number is required"/>
        </if>
        
        <!-- Create -->
        <make-value entity-name="moqui.trade.finance.LetterOfCredit" value-field="lc"/>
        <set field="lc.lcId" from="ec.entity.generatePk('moqui.trade.finance.LetterOfCredit')"/>
        <set field="lc.lcNumber" from="lcNumber"/>
        <set field="lc.lcAmount" from="lcAmount ?: 0"/>
        <set field="lc.lcStatusId" from="lcStatusId"/>
        
        <entity-create value-field="lc"/>
        
        <return from="lc.lcId"/>
    </actions>
</service>
```

### Interface + Implementation Pattern
```xml
<!-- Interface (in entity package) -->
<service verb="transition" noun="Status" type="interface"/>

<!-- Implementation -->
<service verb="transition" noun="Status" location="moqui.service.facility.FacilityServices">
    <in-parameters>
        <parameter name="lcId" required="true"/>
        <parameter name="toStatusId"/>
    </in-parameters>
    <actions>
        <!-- Implementation -->
    </actions>
</service>
```

## 3. Error Handling

### Correct Error Pattern (use script)
```xml
<actions>
    <if condition="!validRecord">
        <script>ec.message.addError("Invalid record state")</script>
        <return/>
    </if>
</actions>
```

### AVOID
```xml
<!-- DON'T USE - doesn't set error state -->
<message error="true">Invalid record</message>
```

### Return with Error
```xml
<return error="true" message="Status transition not allowed"/>
```

## 4. Service Call Patterns

### Sync Call
```xml
<service-call name="moqui.trade.finance.TradeFinanceServices.create#LcDrawing"
             in-map="[lcId:lcId, amount:drawingAmount]"
             out-map="/result"/>
```

### Async Call (Background)
```xml
<service-call name="moqui.trade.finance.TradeFinanceServices.processAmendment"
             transaction="async"/>
```

### Require New Transaction (Force Commit)
```xml
<service-call name="moqui.trade.finance.AuditServices.log#StatusChange"
             transaction="force"/>
```

### Groovy Service Call
```groovy
def result = ec.service.sync()
    .name("moqui.trade.finance.TradeFinanceServices.create#LcDrawing")
    .parameters([lcId: lcId, amount: amount])
    .call()

if (result.success) {
    // handle success
}
```

## 5. Transaction Patterns

### Default (join existing)
```xml
<service ... transaction-timeout="300">
```

### New Transaction (isolated)
```xml
<service ... transaction="force-new">
```

### Async (non-blocking)
```xml
<service ... transaction="async">
```

## 6. Status Transition Service

### Pattern for Custom Status Fields
```xml
<service verb="transition" noun="TransactionStatus">
    <in-parameters>
        <parameter name="lcId" required="true"/>
        <parameter name="toStatusId"/>
    </in-parameters>
    <actions>
        <entity-find-one entity-name="moqui.trade.finance.LetterOfCredit" value-field="lc"/>
        
        <!-- Auto-detect single valid transition -->
        <if condition="!toStatusId">
            <entity-find entity-name="moqui.basic.StatusFlowTransition" list="transitions">
                <econdition field-name="statusFlowId" value="LcTransaction"/>
                <econdition field-name="statusId" from="lc.transactionStatusId"/>
            </entity-find>
            <if condition="transitions.size() == 1">
                <set field="toStatusId" from="transitions[0].toStatusId"/>
            </if>
        </if>
        
        <!-- Validate -->
        <entity-find entity-name="moqui.basic.StatusFlowTransition" list="valid">
            <econdition field-name="statusFlowId" value="LcTransaction"/>
            <econdition field-name="statusId" from="lc.transactionStatusId"/>
            <econdition field-name="toStatusId" from="toStatusId"/>
        </entity-find>
        
        <if condition="!valid">
            <script>ec.message.addError("Invalid transition from ${lc.transactionStatusId} to ${toStatusId}")</script>
            <return/>
        </if>
        
        <!-- Apply -->
        <set field="lc.transactionStatusId" from="toStatusId"/>
        <entity-update value-field="lc"/>
    </actions>
</service>
```

## 7. Authorization

### Default (authenticate required)
```xml
<service verb="update" noun="LetterOfCredit">
    <!-- authenticate="true" is default -->
</service>
```

### Public Service
```xml
<service verb="calculate" noun="ShippingRate" authenticate="false">
```

### Permission-based
```xml
<sec-permission service-permission="TRADE_FINANCE -LC_UPDATE"/>
```

## 8. Entity Auto Services

### Auto CRUD
```xml
<!-- Framework generates: create, update, delete, find for entity -->
<service verb="create" noun="LetterOfCredit" type="interface"/>
<service verb="update" noun="LetterOfCredit" type="interface"/>
<service verb="delete" noun="LetterOfCredit" type="interface"/>
<service verb="find" noun="LetterOfCredit" type="interface"/>
```

### Auto Service Runner
- Only works for entities named exactly in service noun
- Only validates `statusId` field name
- Custom status fields need dedicated transition services

## 9. Common Patterns

### Find-or-Create
```xml
<entity-find entity-name="moqui.trade.finance.LcCharge" list="existing">
    <econdition field-name="lcId"/>
    <econdition field-name="chargeTypeEnumId"/>
</entity-find>

<if condition="existing">
    <set field="charge" from="existing[0]"/>
<else>
    <make-value entity-name="moqui.trade.finance.LcCharge" value-field="charge"/>
    <set field="charge.lcId" from="lcId"/>
    <entity-create value-field="charge"/>
</else>
```

### Cascade Update
```xml
<entity-find entity-name="moqui.trade.finance.LcCharge" list="charges">
    <econdition field-name="lcId"/>
</entity-find>
<iterate list="charges" entry="charge">
    <set field="charge.lcStatusId" from="toStatusId"/>
    <entity-update value-field="charge"/>
</iterate>
```

### Read-Refresh-Update (CRITICAL)
```xml
<actions>
    <!-- Fetch -->
    <entity-find-one entity-name="moqui.trade.finance.LetterOfCredit" value-field="lc"/>
    
    <!-- Call service that modifies lc -->
    <service-call name="moqui.trade.finance.TradeFinanceServices.transition#Status"
                  in-map="[lcId:lcId, toStatusId:'LcApproved']"/>
    
    <!-- RE-FETCH after child service -->
    <entity-find-one entity-name="moqui.trade.finance.LetterOfCredit" value-field="lc"/>
    
    <!-- Now safe to update -->
    <entity-update value-field="lc"/>
</actions>
```

## 10. Service Location Organization

```
service/
├── moqui.service.ExampleServices.xml        # Framework services
├── moqui.service.facility.FacilityServices.xml
└── TradeFinance/
    ├── TradeFinanceServices.xml              # Main services
    ├── LcAmendmentServices.xml
    └── LcDrawingServices.xml
```

## 11. Testing Services

### Full Path Required
```groovy
// CORRECT
ec.service.sync().name("moqui.trade.finance.TradeFinanceServices.create#LetterOfCredit")

// MAY FAIL
ec.service.sync().name("create#LetterOfCredit")
```

### Check for Errors
```groovy
if (ec.message.hasError()) {
    logger.info("Errors: ${ec.message.errors}")
    return
}
```
