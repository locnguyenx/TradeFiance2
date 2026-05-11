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

## 10. Latest Transaction Pointer Pattern (Dual-Status Visibility)

### Problem
In complex lifecycles (like Trade Finance), a master instrument (e.g., LC) can have a stable legal state (Issued) while an active transaction (e.g., Amendment) is in progress. Standard joins to "the last transaction" are expensive or unreliable when multiple types exist.

### Solution
Add a `latestTransactionId` field to the master entity and use an **EECA** (Entity ECA) to maintain it.

```xml
<!-- Master Entity -->
<entity entity-name="TradeInstrument">
    <field name="instrumentId" type="id" is-pk="true"/>
    <field name="latestTransactionId" type="id"/>
    <relationship type="one" related="trade.TradeTransaction">
        <key-map field-name="latestTransactionId" related-field-name="transactionId"/>
    </relationship>
</entity>

<!-- EECA to maintain the pointer -->
<eeca entity-name="trade.TradeTransaction" get-entire-entity="true">
    <actions>
        <service-call name="update#trade.TradeInstrument" 
                      in-map="[instrumentId:instrumentId, latestTransactionId:transactionId]"/>
    </actions>
</eeca>

<!-- High-Performance View -->
<view-entity entity-name="InstrumentView">
    <member entity-alias="inst" entity-name="TradeInstrument"/>
    <member entity-alias="tx" entity-name="TradeTransaction" join-from-alias="inst">
        <key-map field-name="latestTransactionId" related-field-name="transactionId"/>
    </member>
    <!-- View aliases now show both Business State (inst) and Tx Status (tx) -->
</view-entity>
```

### Benefits
- **Performance**: O(1) lookup of the "current action" on the instrument.
- **Reporting**: Enables dashboards to show "Issued / Amendment Pending" in a single row.
- **Audit**: Decouples historical logs from the "current active workflow" pointer.

## 11. Entity Definition Best Practices (Framework Features Reference)

> Researched 2026-05-09 from `framework/entity/BasicEntities.xml`, `framework/entity/SecurityEntities.xml`,
> `mantle-udm/AccountingAccountEntities.xml`, `mantle-udm/OrderEntities.xml`,
> and `moqui-documentation/moqui-framework/Data+and+Resources_Data+Model+Definition.md`.

### 11.1 Relationship Conventions

Every `type="id"` field MUST have an explicit `<relationship>` definition. This enables:
- Automatic form dropdown generation in XML screens
- Entity graph navigation (`getRelated()`)
- REST API nested expansion (`?dependents=true`)
- Data integrity via foreign key constraints

#### StatusItem Pattern
The `title` attribute MUST match the `statusTypeId` value in seed data:
```xml
<!-- Field definition -->
<field name="statusId" type="id" enable-audit-log="true"/>

<!-- Relationship: title matches statusTypeId -->
<relationship type="one" title="FinancialAccount" related="moqui.basic.StatusItem" short-alias="status"/>
```
Source: `mantle-udm/AccountingAccountEntities.xml:105` (FinancialAccount entity)

#### Enumeration Pattern
The `title` attribute MUST match the `enumTypeId` value in seed data:
```xml
<field name="dataSourceTypeEnumId" type="id"/>

<relationship type="one" title="DataSourceType" related="moqui.basic.Enumeration" short-alias="type">
    <key-map field-name="dataSourceTypeEnumId"/>
</relationship>
```
Source: `framework/BasicEntities.xml` (DataSource entity), `Data+Model+Definition.md:46`

The framework auto-filters Enumeration dropdown options based on the title→enumTypeId match.

#### Uom (Currency) Pattern
```xml
<field name="currencyUomId" type="id"/>

<relationship type="one" title="Currency" related="moqui.basic.Uom" short-alias="currencyUom">
    <key-map field-name="currencyUomId" related="uomId"/>
</relationship>
```
Source: `mantle-udm/AccountingAccountEntities.xml:112`

#### UserAccount Pattern
Always `type="one-nofk"` (no FK constraint, cross-package):
```xml
<field name="performedByUserId" type="id"/>

<relationship type="one-nofk" title="PerformedBy" related="moqui.security.UserAccount" short-alias="performedByUser">
    <key-map field-name="performedByUserId" related="userId"/>
</relationship>
```
Source: `mantle-udm/AccountingAccountEntities.xml:220`

#### Geo (Country) Pattern
```xml
<relationship type="one-nofk" title="CountryOfRisk" related="moqui.basic.Geo" short-alias="country">
    <key-map field-name="countryOfRisk" related="geoId"/>
</relationship>
```

#### When to use `one-nofk` vs `one`
- `one` = creates DB foreign key constraint. Use when seed data exists and values are controlled.
- `one-nofk` = relationship for navigation only, no DB FK. Use for:
  - Cross-package references (e.g., security.UserAccount)
  - Fields with ad-hoc/optional values (e.g., nullable enum fields)
  - Fields where data loading order could cause constraint violations

### 11.2 `enable-audit-log` Attribute

Automatically tracks field changes in the `EntityAuditLog` entity. Two modes:

| Value | Behavior | Use For |
|---|---|---|
| `"true"` | Logs initial value AND all changes | Status/state fields, critical business fields |
| `"update"` | Logs changes only (not initial) | Mutable fields that change frequently (lighter weight) |

```xml
<field name="statusId" type="id" enable-audit-log="true"/>
<field name="amount" type="currency-amount" enable-audit-log="update"/>
<field name="ownerPartyId" type="id" enable-audit-log="update"/>
```
Source: `mantle-udm/AccountingAccountEntities.xml:83,89,95` (FinancialAccount), `Data+Model+Patterns.md:90-95`

**Apply to all status/state lifecycle fields** — this replaces manual audit trail implementations.

### 11.3 `short-alias` Attribute

Used on both entities AND relationships for cleaner REST API paths and nested expansion.

#### Entity-level short-alias
Defines the REST API collection path: `/rest/s1/{package}/{short-alias}`
```xml
<entity entity-name="FinancialAccount" package="mantle.account.financial"
        short-alias="financialAccounts" cache="never">
```
Source: `mantle-udm/AccountingAccountEntities.xml:80`

Convention: **plural camelCase** of the entity name.

#### Relationship-level short-alias
Defines the nested expansion key in REST responses:
```xml
<relationship type="one" title="Owner" related="mantle.party.Party" short-alias="owner">
    <key-map field-name="ownerPartyId"/>
</relationship>
<relationship type="many" related="mantle.account.financial.FinancialAccountTrans" short-alias="transactions">
    <key-map field-name="finAccountId"/>
</relationship>
```
Source: `mantle-udm/AccountingAccountEntities.xml:108,119`

Convention: **singular camelCase** for `type="one"`, **plural camelCase** for `type="many"`.

### 11.4 `cache` Attribute

| Value | Meaning | Use For |
|---|---|---|
| `"true"` | Cache finds (code can override) | Configuration/reference data (catalogs, clauses, fee configs) |
| `"false"` | No cache (code can override) | Default for most entities |
| `"never"` | No cache (code CANNOT override) | Transactional entities with high write frequency |

```xml
<!-- Configuration entity -->
<entity entity-name="DataSource" package="moqui.basic" cache="true">

<!-- Transactional entity -->
<entity entity-name="Invoice" package="mantle.account.invoice"
        short-alias="invoices" cache="never" optimistic-lock="true">
```
Source: `framework/BasicEntities.xml:11`, `mantle-udm/AccountingAccountEntities.xml:328`

### 11.5 `optimistic-lock` Attribute

Compares `lastUpdatedStamp` before updates to detect concurrent modifications:
```xml
<entity entity-name="Invoice" package="mantle.account.invoice"
        short-alias="invoices" cache="never" optimistic-lock="true">
```
Source: `mantle-udm/AccountingAccountEntities.xml:328`

**Apply to entities where concurrent updates are a business risk** (e.g., instruments, LCs, orders).

### 11.6 `master` Definitions

Defines a pre-built graph of related data for `?dependents=true` REST expansion:
```xml
<entity entity-name="OrderHeader" ...>
    <!-- fields and relationships... -->
    <master>
        <detail relationship="type"/>
        <detail relationship="status"/>
        <detail relationship="parts">
            <detail relationship="items"/>
            <detail relationship="party" use-master="contact"/>
            <detail relationship="contactMech" use-master="default"/>
        </detail>
        <detail relationship="vendor" use-master="basic"/>
        <detail relationship="customer" use-master="basic"/>
        <detail relationship="payments" use-master="default"/>
    </master>
</entity>
```
Source: `mantle-udm/OrderEntities.xml:253-293`

- `<detail relationship="..."/>` references a relationship `short-alias`
- `use-master="..."` inherits the master definition of the related entity
- Nested `<detail>` defines sub-graph expansion

### 11.7 `type="many"` Reverse Relationships

Parent entities should define reverse relationships to their children:
```xml
<!-- On FinancialAccount (parent) -->
<relationship type="many" related="mantle.account.financial.FinancialAccountAuth" short-alias="authorizations">
    <key-map field-name="finAccountId"/>
</relationship>
<relationship type="many" related="mantle.account.financial.FinancialAccountTrans" short-alias="transactions">
    <key-map field-name="finAccountId"/>
</relationship>
<relationship type="many" related="mantle.account.financial.FinancialAccountParty" short-alias="parties">
    <key-map field-name="finAccountId"/>
</relationship>
```
Source: `mantle-udm/AccountingAccountEntities.xml:117-121`

These enable:
- `master` definitions to expand child collections
- REST API to return nested arrays
- UI lists to load child records automatically

### 11.8 Inline `seed-data` Element

Configuration data tightly coupled to the entity can be declared inline:
```xml
<entity entity-name="DataSource" package="moqui.basic" cache="true">
    <field name="dataSourceId" type="id" is-pk="true"/>
    <field name="dataSourceTypeEnumId" type="id"/>
    <field name="description" type="text-medium"/>
    <relationship type="one" title="DataSourceType" related="Enumeration">
        <key-map field-name="dataSourceTypeEnumId"/>
    </relationship>
    <seed-data>
        <moqui.basic.EnumerationType description="Data Source Type" enumTypeId="DataSourceType"/>
        <moqui.basic.Enumeration description="Purchased Data" enumId="DST_PURCHASED_DATA" enumTypeId="DataSourceType"/>
    </seed-data>
</entity>
```
Source: `framework/BasicEntities.xml`, `Data+Model+Definition.md:18-21`

Loaded with `./gradlew load` along with type=seed data.

### 11.9 Entity Extension (Cross-Component)

Extend entities from other components without modifying original files:
```xml
<extend-entity entity-name="Example" package="moqui.example">
    <field name="auditedField" type="text-medium" enable-audit-log="true"/>
    <field name="encryptedField" type="text-medium" encrypt="true"/>
</extend-entity>
```
Source: `Data+Model+Definition.md:136-140`

Useful for adding Trade Finance fields to Mantle entities without forking.

### 11.10 Complete Entity Definition Checklist

When defining or auditing an entity, verify:

- [ ] All `type="id"` fields have explicit `<relationship>` definitions
- [ ] `title` attributes match `enumTypeId`/`statusTypeId` for auto-filtering
- [ ] `short-alias` on entity (plural camelCase) for REST paths
- [ ] `short-alias` on all relationships for REST expansion
- [ ] `enable-audit-log` on status fields and key mutable business fields
- [ ] `cache="true"` on configuration/reference entities
- [ ] `optimistic-lock="true"` on concurrency-sensitive master entities
- [ ] `type="many"` reverse relationships from parent to children
- [ ] `master` definition on key master entities for API graph expansion
- [ ] Seed data in `<seed-data>` or separate seed data files for all EnumerationType/StatusType values

## 12. Frontend-Backend Enum Synchronization Pattern

### Problem
Referential integrity violations (`23506`) occurring during REST API calls (e.g., `update#ImportLetterOfCredit`) because the frontend submits enum IDs that either use incorrect prefixes or are missing from the backend `Enumeration` table.

### Solution
1. **Frontend Standardization**: All hardcoded enum constants in React components MUST use the prefixes defined in the backend seed data.
2. **Relationship Title Matching**: Ensure the `<relationship title="...">` in the entity definition matches the `enumTypeId` in the seed data.
3. **Seed Data Integrity**: Any new enum values added to the UI MUST be explicitly declared in `TradeFinanceSeedData.xml` (or inline `<seed-data>`).

### Checklist
- [ ] Frontend constants use correct prefixes (e.g., `LCT_`, `CHG_`, `AVB_`, `AW_`, `APR_`, `RMB_`, `MARG_`).
- [ ] Backend `moqui.basic.Enumeration` records exist for all UI-visible options.
- [ ] UI dropdown labels are derived from the same source of truth as the backend `description` fields where possible.
- [ ] Use `reloadSave` or `loadData` to synchronize the database after XML changes.
