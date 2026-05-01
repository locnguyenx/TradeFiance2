# Moqui Service Patterns

> Verified against Moqui Framework

## 1. Service Naming Convention

### verb#noun Pattern
| Verb | Purpose | Example |
|------|---------|---------|
| `create` | New record | `create#Order` |
| `update` | Modify record | `update#Order` |
| `delete` | Remove record | `delete#Order` |
| `get` | Retrieve data | `get#OrderDetails` |
| `find` | Search records | `find#OrderByStatus` |
| `transition` | Status change | `transition#Status` |
| `validate` | Business rule check | `validate#Record` |

### Full Path
```xml
<service verb="create" noun="Order" 
         type="interface"
         location="moqui.service.ExampleServices">
```
- Use full package path: `moqui.example.ExampleServices.create#Example`

## 2. Service Definition Structure

### Standard CRUD Service
```xml
<service verb="create" noun="Example">
    <description>Create a new Example record</description>
    <in-parameters>
        <parameter name="exampleName" type="String" required="true"/>
        <parameter name="statusId" type="String" default-value="Draft"/>
    </in-parameters>
    <out-parameters>
        <parameter name="exampleId" type="String"/>
    </out-parameters>
    <actions>
        <!-- Validation -->
        <if condition="!exampleName">
            <return error="true" message="Name is required"/>
        </if>
        
        <!-- Create -->
        <make-value entity-name="moqui.example.Example" value-field="newValue"/>
        <set field="newValue.exampleId" from="ec.entity.generatePk('moqui.example.Example')"/>
        <set field="newValue.exampleName" from="exampleName"/>
        <set field="newValue.statusId" from="statusId"/>
        
        <entity-create value-field="newValue"/>
        
        <return from="newValue.exampleId"/>
    </actions>
</service>
```

### Interface + Implementation Pattern
```xml
<!-- Interface (in entity package) -->
<service verb="transition" noun="Status" type="interface"/>

<!-- Implementation -->
<service verb="transition" noun="Status" location="moqui.service.ExampleServices">
    <in-parameters>
        <parameter name="exampleId" required="true"/>
        <parameter name="toStatusId"/>
    </in-parameters>
    <actions>
        <!-- Implementation logic -->
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

### Return with Error
```xml
<return error="true" message="Action not allowed"/>
```

## 4. Service Call Patterns

### Sync Call
```xml
<service-call name="moqui.example.ExampleServices.create#Example"
             in-map="[name:name, type:type]"
             out-map="/result"/>
```

### Async Call (Background)
```xml
<service-call name="moqui.example.ExampleServices.processData"
             transaction="async"/>
```

### Require New Transaction (Force Commit)
```xml
<service-call name="moqui.AuditServices.log#Change"
             transaction="force"/>
```

### Groovy Service Call
```groovy
def result = ec.service.sync()
    .name("moqui.example.ExampleServices.create#Example")
    .parameters([name: name])
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
<service verb="transition" noun="Status">
    <in-parameters>
        <parameter name="exampleId" required="true"/>
        <parameter name="toStatusId"/>
    </in-parameters>
    <actions>
        <entity-find-one entity-name="moqui.example.Example" value-field="example"/>
        
        <!-- Auto-detect single valid transition -->
        <if condition="!toStatusId">
            <entity-find entity-name="moqui.basic.StatusFlowTransition" list="transitions">
                <econdition field-name="statusId" from="example.statusId"/>
            </entity-find>
            <if condition="transitions.size() == 1">
                <set field="toStatusId" from="transitions[0].toStatusId"/>
            </if>
        </if>
        
        <!-- Validate -->
        <entity-find entity-name="moqui.basic.StatusFlowTransition" list="valid">
            <econdition field-name="statusId" from="example.statusId"/>
            <econdition field-name="toStatusId" from="toStatusId"/>
        </entity-find>
        
        <if condition="!valid">
            <script>ec.message.addError("Invalid transition")</script>
            <return/>
        </if>
        
        <!-- Apply -->
        <set field="example.statusId" from="toStatusId"/>
        <entity-update value-field="example"/>
    </actions>
</service>
```

## 7. Authorization

### Default (authenticate required)
```xml
<service verb="update" noun="Example">
    <!-- authenticate="true" is default -->
</service>
```

### Public Service
```xml
<service verb="calculate" noun="Rate" authenticate="false">
```

### Permission-based
```xml
<sec-permission service-permission="EXAMPLE_MODULE -RECORD_UPDATE"/>
```

## 8. Entity Auto Services

### Auto CRUD
```xml
<service verb="create" noun="Example" type="interface"/>
<service verb="update" noun="Example" type="interface"/>
<service verb="delete" noun="Example" type="interface"/>
<service verb="find" noun="Example" type="interface"/>
```

## 9. Common Patterns

### Find-or-Create
```xml
<entity-find entity-name="moqui.example.Example" list="existing">
    <econdition field-name="exampleId"/>
</entity-find>

<if condition="existing">
    <set field="example" from="existing[0]"/>
<else>
    <make-value entity-name="moqui.example.Example" value-field="example"/>
    <entity-create value-field="example"/>
</else>
```

### Cascade Update
```xml
<entity-find entity-name="moqui.example.Child" list="children">
    <econdition field-name="parentId"/>
</entity-find>
<iterate list="children" entry="child">
    <set field="child.statusId" from="toStatusId"/>
    <entity-update value-field="child"/>
</iterate>
```

### Read-Refresh-Update (CRITICAL)
```xml
<actions>
    <!-- Fetch -->
    <entity-find-one entity-name="moqui.example.Example" value-field="example"/>
    
    <!-- Call service that modifies record -->
    <service-call name="moqui.example.ExampleServices.transition#Status"
                  in-map="[id:id, toStatusId:'Approved']"/>
    
    <!-- RE-FETCH after child service -->
    <entity-find-one entity-name="moqui.example.Example" value-field="example"/>
    
    <!-- Now safe to update -->
    <entity-update value-field="example"/>
</actions>
```
