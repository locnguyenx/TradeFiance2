# Moqui Entity & Data Patterns

> Verified against Moqui Framework and SimpleScreens component

## 1. Entity Structure & Conventions

### Naming
- **Entity names**: PascalCase (e.g., `OrderHeader`, `OrderItem`)
- **Field names**: snake_case (e.g., `order_id`, `status_id`)
- **Package path**: Must match directory structure

### Primary Key
- Use exactly ONE `<field is-pk="true">` with `type="id"`
- Set `primary-key-sequence="true"` for auto-increment:
  ```xml
  <entity entity-name="moqui.example.Example" package="moqui.example"
          primary-key-sequence="true">
      <field name="exampleId" type="id" is-pk="true"/>
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
<relationship type="many" related="moqui.example.ExampleItem">
    <key-map field-name="exampleId"/>
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
<Example exampleId="DEMO_01" statusId="Draft" .../>
```

## 5. Shadow Record Pattern (Draft/Proposed Changes)

### Concept
Create a shadow entity to hold proposed changes before committing.

### Implementation
```xml
<!-- Master Entity -->
<entity entity-name="WorkEffort">
    <field name="workEffortId" type="id" is-pk="true"/>
    <field name="statusId" type="id"/>
    <!-- All amendable fields -->
</entity>

<!-- Shadow Entity (Proposed Changes) -->
<entity entity-name="WorkEffortProposed">
    <field name="proposalId" type="id" is-pk="true"/>
    <field name="workEffortId" type="id"/>
    <!-- Mirror of fields with _NEW suffix -->
    <field name="statusIdNew" type="id"/>
</entity>
```

### Workflow
1. Create shadow record
2. User edits shadow record
3. On approval, apply changes to master via service
4. Delete shadow record

## 6. Idempotent Service Pattern

### Problem
Services creating records can cause duplicates on retries.

### Solution
```xml
<service verb="create" noun="Example">
    <in-parameters>
        <parameter name="exampleId" required="true"/>
    </in-parameters>
    <actions>
        <!-- Check if already exists -->
        <entity-find-one entity-name="moqui.example.Example" value-field="existing"/>
        <if condition="existing">
            <return/>
        </if>
        <!-- Create new record -->
        <make-value entity-name="moqui.example.Example" value-field="newValue"/>
        <set field="newValue.exampleId" from="exampleId"/>
        <entity-create value-field="newValue"/>
    </actions>
</service>
```

## 7. Status Guard Pattern

### Problem
Modifying records after they advance to a terminal state.

### Solution
```xml
<service verb="transition" noun="Status">
    <in-parameters>
        <parameter name="exampleId" required="true"/>
        <parameter name="toStatusId"/>
    </in-parameters>
    <actions>
        <entity-find-one entity-name="moqui.example.Example" value-field="example"/>
        
        <!-- Status Guard -->
        <if condition="example.statusId == 'Finalized'">
            <return error="true" message="Cannot modify in ${example.statusId} state"/>
        </if>
        
        <!-- Validate transition -->
        <entity-find entity-name="moqui.basic.StatusFlowTransition" list="validTransitions">
            <econdition field-name="statusId" from="example.statusId"/>
            <econdition field-name="toStatusId" from="toStatusId"/>
        </entity-find>
        
        <if condition="!validTransitions">
            <return error="true" message="Invalid status transition"/>
        </if>
        
        <!-- Apply -->
        <set field="example.statusId" from="toStatusId"/>
        <entity-update value-field="example"/>
    </actions>
</service>
```

## 8. Read-Refresh-Update Pattern

### Problem
Calling child service modifies record, parent EntityValue becomes stale.

### Solution
```xml
<service verb="update" noun="Example">
    <actions>
        <entity-find-one entity-name="moqui.example.Example" value-field="example"/>
        
        <!-- Call service that modifies example -->
        <service-call name="moqui.example.ExampleServices.transition#Status"
                      parameter-map="[exampleId:exampleId, toStatusId:'Approved']"/>
        
        <!-- RE-FETCH after child service call -->
        <entity-find-one entity-name="moqui.example.Example" value-field="example"/>
        
        <!-- Now safe to update -->
        <set field="example.lastUpdatedDate" from="ec.user.nowTimestamp"/>
        <entity-update value-field="example"/>
    </actions>
</service>
```

## 9. Entity & Database Operations

* **View-Entity Brittleness**: Moqui's `view-entity` engine can be brittle when resolving joined fields across packages. For high-stakes API services, use **Manual Groovy Joins** (sequential lookups and merging maps) for predictability and avoiding registry resolution errors.
* **Composite Keys & Sequencing**: `<entity-sequenced-id>` is not valid in XML actions directly for composite keys. Use `<entity-make-value>` followed by `<entity-sequenced-id-secondary value-field="..."/>` to correctly increment secondary IDs relative to a primary key.
* **Data Mapping**: Ensure explicit mapping between UI fields and backend entity fields in service actions to avoid silent failure of `auto-parameters`.
* **XML Fragility**: Be careful with XML syntax. Accidental removal of `<actions>` or unclosed `<in-parameters>` tags can lead to services that load successfully but execute no logic.

### Entity Condition
```groovy
// LIKE query
import org.moqui.entity.EntityCondition
ec.entity.find("moqui.example.Example")
    .condition("exampleName", EntityCondition.LIKE, "DEMO%")
    .list()

// IN query
import org.moqui.entity.EntityCondition
ec.entity.find("moqui.example.Example")
    .condition("statusId", EntityCondition.IN, ["Draft", "Approved"])
    .list()
```

### Cascade Delete
```groovy
// Delete children first
def children = ec.entity.find("moqui.example.Child")
    .condition("parentId", parentId)
    .list()
children.each { it.delete() }

// Then parent
def parent = ec.entity.find("moqui.example.Parent")
    .condition("parentId", parentId)
    .one()
parent.delete()
```
