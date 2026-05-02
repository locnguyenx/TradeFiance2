# Moqui UI & Screen Patterns

> Verified against Moqui Framework and SimpleScreens component

## 1. Form-List Pattern

### Standard Find Screen List
```xml
<form-list name="ListExamples" list="exampleList" skip-form="true" header-dialog="true">
    <entity-find entity-name="moqui.example.Example" list="exampleList">
        <search-form-inputs default-order-by="-date"/>
    </entity-find>
    
    <field name="exampleName">
        <header-field title="Name" show-order-by="case-insensitive">
            <text-find size="15" hide-options="true"/>
        </header-field>
        <default-field>
            <link url="../ExampleDetails" text="${exampleName}"/>
        </default-field>
    </field>
    
    <field name="statusId">
        <header-field title="Status">
            <drop-down allow-empty="true">
                <entity-options key="${statusId}" text="${description}">
                    <entity-find entity-name="moqui.basic.StatusItem">
                        <econdition field-name="statusTypeId" value="ExampleStatus"/>
                    </entity-find>
                </entity-options>
            </drop-down>
        </header-field>
        <default-field>
            <display-entity entity-name="moqui.basic.StatusItem"/>
        </default-field>
    </field>
</form-list>
```

### Advanced List (from SimpleScreens)
```xml
<form-list name="ListTasks" list="taskList" skip-form="true" header-dialog="true"
           saved-finds="true" select-columns="true" show-page-size="true"
           show-csv-button="true" show-xlsx-button="true">
    <row-actions>
        <entity-find entity-name="mantle.work.effort.WorkEffortAndPartyDetail" list="weapdList">
            <date-filter/><econdition field-name="workEffortId"/>
        </entity-find>
    </row-actions>
    
    <row-selection id-field="workEffortId">
        <action>
            <dialog button-text="Assign" title="Assign Selected..."/>
            <form-single name="AssignTaskSelected" transition="addWorkEffortParty">
                <!-- form fields -->
            </form-single>
        </action>
    </row-selection>
</form-list>
```

### Form-List Column Layout
```xml
<form-list-column>
    <field-ref name="name"/><field-ref name="description"/>
</form-list-column>
<form-list-column>
    <field-ref name="statusId"/><field-ref name="dateField"/>
</form-list-column>
```
**CRITICAL**: Use `<field-ref>` NOT `<field>` inside `<form-list-column>`

## 2. Screen Hierarchy Pattern

### Parent Shell Screen
```xml
<actions>
    <set field="exampleId" from="exampleId ?: exampleSeqId"/>
    <if condition="exampleId">
        <entity-find-one entity-name="moqui.example.Example" value-field="example"/>
    </if>
</actions>

<widgets>
    <subscreens-panel type="tab" parent-name="Example" if-active="true"/>
</widgets>
```

### Details Sub-screen
```xml
<parameter name="exampleId" required="true"/>

<actions>
    <entity-find-one entity-name="moqui.example.Example" value-field="example"/>
</actions>
```

### Menu Clearing
```xml
<subscreens-item name="Example" location="." parameter-map="[exampleId:null]"/>
```

### Visibility Regex Guard
```xml
<section name="DetailHeader" 
         condition="example &amp;&amp; sri.screenUrlInfo.extraPathNameList">
    <!-- Only shows when entity exists AND sub-path present -->
</section>
```

## 3. Form-Single (Create/Edit)

### Standard Create Form
```xml
<form-single name="CreateExample" transition="createExample">
    <field name="exampleName">
        <default-field title="Name"><text-line required="true"/></default-field>
    </field>
    <field name="submitButton">
        <default-field title="Create"><submit/></default-field>
    </field>
</form-single>
```

### Container Dialog Pattern
```xml
<container-dialog id="CreateDialog" button-text="Create New">
    <form-single name="CreateRecord" transition="createRecord">
        <field name="name"><default-field><text-line required="true"/></default-field></field>
        <field name="submitButton"><default-field><submit/></default-field></field>
    </form-single>
</container-dialog>
```

## 4. Transition Patterns

### Relative Redirect
```xml
<default-response url="./Details" parameter-map="[id:id]"/>
```

### Delete with Confirmation
```xml
<transition name="deleteRecord">
    <service-call name="moqui.example.ExampleServices.delete#Example"/>
    <default-response url="."/>
</transition>

<!-- In form-list -->
<field name="actions">
    <default-field title="">
        <link url="deleteRecord" text="Delete" style="text-negative"
              parameter-map="[id:id]"
              confirmation="Confirm deletion?"/>
    </default-field>
</field>
```

## 5. Quasar Styling & Widgets

### Container Styling
Use `style=` not `class=` for Quasar classes in XML widgets.
```xml
<container style="q-card shadow-2 q-pa-md">
    <label style="q-chip bg-primary text-white">Status</label>
</container>
```

### Button Styling
Use `style="text-negative"` for dangerous actions.

### widget-templates
Use `statusDropDown` and `enumDropDown` for consistent UI.

## 6. XML Validation
Always validate before testing:
```bash
xmllint --noout path/to/Screen.xml
```

## 7. Stale UI Cache Fix
```bash
./gradlew cleanAll
# Browser: Cmd+Shift+R (hard refresh)
```

## 8. Field Layout Rules
| Rule | Implementation |
|------|----------------|
| Links in list | Wrap in `<field>`, use `<field-ref>` |
| Quasar classes | Use `style=` not `class=` |
| Buttons | `Create`, `Save`, `Delete` (text-negative) |
| Back navigation | `${lastScreenUrl ?: '.'}` |

## 9. Shared Fragments (transition-include)
```xml
<transition-include name="createRecord" location="component://.../template/SharedTransitions.xml"/>
```

## 10. Conditional Logic
**CRITICAL**: `<else>` must be INSIDE `<if>`, not as sibling.
```xml
<if condition="active">
    <set field="label" value="success"/>
    <else>
        <set field="label" value="danger"/>
    </else>
</if>
```

## 11. Additional UI Patterns

### Screen Rendering (formInstance was null)
**Symptom**: `expression 'formInstance' was null`
**Cause**: Using `<include-screen>` for a dialog or form with missing context parameters.
**Solution**: Inline the dialogs within the screen or ensure the parent context passes all required parameters to the `<include-screen>` tag.

### Form Submit Issues
- Static `<option>` tags may not submit selected values correctly in some Moqui versions.
- **Solution**: Use `<entity-options>` or dynamic `<list-options>` for reliable behavior.
- Use `<text-line>` for manual text input instead of complex dropdowns if validation is simple.
```xml
<!-- Reliable Pattern -->
<drop-down>
    <entity-options key="${uomId}" text="${uomId}">
        <entity-find entity-name="moqui.basic.Uom"/>
    </entity-options>
</drop-down>
```

