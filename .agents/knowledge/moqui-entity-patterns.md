# Moqui Entity & Data Patterns

> Verified against Moqui Framework and SimpleScreens component

## 1. Entity Structure & Conventions

### Naming
- **Entity names**: PascalCase (e.g., `LetterOfCredit`, `LcAmendment`)
- **Field names**: snake_case (e.g., `lc_id`, `lc_status_id`)
- **Package path**: Must match directory structure

### Primary Key
- Use exactly ONE `<field is-pk="true">` with `type="id"`
- Set `primary-key-sequence="true"` for auto-increment:
  ```xml
  <entity entity-name="TradeFinance.LetterOfCredit" package="moqui.trade.finance"
          primary-key-sequence="true">
      <field name="lcId" type="id" is-pk="true"/>
  ```

### Audit Stamps (Auto-injected)
- DO NOT manually add `lastUpdatedStamp` or `createdTxStamp`
- Framework injects these automatically

## 2. Relationships

### One-to-One / One-to-Many
```xml
<relationship type="one" related="moqui.basic.StatusItem">
    <key-map field-name="statusId"/>
</relationship>
```

### Many-to-One (Foreign Key)
```xml
<relationship type="many" related="moqui.trade.finance.LcAmendment">
    <key-map field-name="lcId"/>
</relationship>
```

### View Entities (No Raw SQL)
```xml
<view-entity entity-name="OrderAndParty" package="moqui.order">
    <member entity-alias="oh" entity-name="OrderHeader"/>
    <member entity-alias="pa" entity-name="PartyIdentification" join-from-alias="oh">
        <key-map field-name="partyId"/>
    </member>
    <alias name="orderId" entity-alias="oh"/>
    <alias name="partyId" entity-alias="oh"/>
</view-entity>
```

## 3. Caching

| Entity Type | Cache Setting |
|-------------|---------------|
| Static config (Status, Enums) | `cache="true"` |
| Dynamic transactional | No cache |

## 4. Data Loading Order

### Sequence (CRITICAL)
1. Seed Data (Enumerations, StatusItems)
2. Initial Data (Configuration)
3. Demo Data (Test records with explicit PKs)

### Demo Data Strategy
Use predictable IDs for testing:
```xml
<LetterOfCredit lcId="DEMO_LC_01" lcStatusId="LcDraft" .../>
```

## 5. Shadow Record Pattern (Amendments)

### Concept
Create a shadow entity to hold proposed changes before committing.

### Implementation
```xml
<!-- Master Entity -->
<entity entity-name="LetterOfCredit">
    <field name="lcId" type="id" is-pk="true"/>
    <field name="lcStatusId" type="id"/>
    <!-- All amendable fields -->
</entity>

<!-- Shadow Entity (LcAmendment) -->
<entity entity-name="LcAmendment">
    <field name="amendmentId" type="id" is-pk="true"/>
    <field name="lcId" type="id"/>
    <!-- Mirror of amendable fields with _NEW suffix -->
    <field name="lcStatusIdNew" type="id"/>
</entity>

<!-- Transaction Entity (LcTransaction) -->
<entity entity-name="LcTransaction">
    <field name="txId" type="id" is-pk="true"/>
    <field name="amendmentId" type="id"/>
    <field name="txTypeEnumId" type="id"/>
</entity>
```

### Workflow
1. Create `LcAmendment` record (status: Draft)
2. User edits shadow record
3. On approval, create `LcTransaction` 
4. Apply changes to master via service
5. Delete shadow record

## 6. Idempotent Service Pattern

### Problem
Services creating records can cause duplicates on retries.

### Solution
```xml
<service verb="create" noun="LcProvision">
    <in-parameters>
        <parameter name="lcId" required="true"/>
    </in-parameters>
    <actions>
        <!-- Check if already exists -->
        <entity-find entity-name="moqui.trade.finance.LcProvision" list="existing">
            <econdition field-name="lcId"/>
        </entity-find>
        <if condition="existing">
            <return/>
        </if>
        <!-- Create new record -->
        <make-value entity-name="moqui.trade.finance.LcProvision" value-field="prov"/>
        <set field="prov.lcId" from="lcId"/>
        <entity-create value-field="prov"/>
    </actions>
</service>
```

## 7. Status Guard Pattern

### Problem
Modifying children after parent advances breaks audit trails.

### Solution
```xml
<service verb="transition" noun="TransactionStatus">
    <in-parameters>
        <parameter name="lcId" required="true"/>
        <parameter name="toStatusId"/>
    </in-parameters>
    <actions>
        <entity-find-one entity-name="moqui.trade.finance.LetterOfCredit" value-field="lc"/>
        
        <!-- Status Guard: Only allow changes in Draft state -->
        <if condition="lc.transactionStatusId != 'LcTxDraft'">
            <return error="true" message="Cannot modify in ${lc.transactionStatusId} state"/>
        </if>
        
        <!-- Validate transition -->
        <entity-find entity-name="moqui.basic.StatusFlowTransition" list="validTransitions">
            <econdition field-name="statusFlowId" value="LcTransaction"/>
            <econdition field-name="statusId" from="lc.transactionStatusId"/>
            <econdition field-name="toStatusId" from="toStatusId"/>
        </entity-find>
        
        <if condition="!validTransitions">
            <return error="true" message="Invalid status transition"/>
        </if>
        
        <!-- Apply -->
        <set field="lc.transactionStatusId" from="toStatusId"/>
        <entity-update value-field="lc"/>
    </actions>
</service>
```

## 8. Read-Refresh-Update Pattern

### Problem
Calling child service modifies record, parent EntityValue becomes stale.

### Solution
```xml
<service verb="update" noun="LetterOfCredit">
    <actions>
        <entity-find-one entity-name="moqui.trade.finance.LetterOfCredit" value-field="lc"/>
        
        <!-- Call service that modifies lc -->
        <service-call name="moqui.trade.finance.TradeFinanceServices.transition#Status"
                      parameter-map="[lcId:lcId, toStatusId:'LcApproved']"/>
        
        <!-- RE-FETCH after child service call -->
        <entity-find-one entity-name="moqui.trade.finance.LetterOfCredit" value-field="lc"/>
        
        <!-- Now safe to update -->
        <set field="lc.lastAmendmentDate" from="ec.user.nowTimestamp"/>
        <entity-update value-field="lc"/>
    </actions>
</service>
```
