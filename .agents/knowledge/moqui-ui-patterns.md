# Moqui UI & Screen Patterns

> Verified against SimpleScreens (811 form-list patterns) and TradeFinance component

## 1. Form-List Pattern

### Standard Find Screen List
```xml
<form-list name="FindEntityList" list="entityList" skip-form="true" header-dialog="true">
    <entity-find entity-name="moqui.trade.finance.LetterOfCredit" list="entityList" use-clone="true">
        <search-form-inputs default-order-by="-date"/>
        <econdition field-name="lcStatusId" operator="not-equals" value="LcCancelled"/>
    </entity-find>
    
    <field name="lcNumber">
        <header-field title="Ref #" show-order-by="case-insensitive">
            <text-find size="15" hide-options="true"/>
        </header-field>
        <default-field>
            <link url="../MainLC" text="${lcNumber}" style="text-weight-bold"/>
        </default-field>
    </field>
    
    <field name="lcStatusId">
        <header-field title="Status">
            <drop-down allow-empty="true">
                <entity-options key="${statusId}" text="${description}">
                    <entity-find entity-name="moqui.basic.StatusItem">
                        <econdition field-name="statusId" operator="in" value="LcDraft,LcApproved,LcIssued"/>
                    </entity-find>
                </entity-options>
            </drop-down>
        </header-field>
        <default-field>
            <display-entity entity-name="moqui.basic.StatusItem"/>
        </default-field>
    </field>
    
    <field name="lcAmount">
        <header-field title="Amount" show-order-by="true"><range-find/></header-field>
        <default-field><display format="#,##0.00"/></default-field>
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
    
    <!-- Field definitions -->
</form-list>
```

### Form-List Column Layout (SIMPLE SCREENS STANDARD)
```xml
<form-list-column>
    <field-ref name="fieldName1"/><field-ref name="fieldName2"/>
</form-list-column>
<form-list-column>
    <field-ref name="statusId"/><field-ref name="dateField"/>
</form-list-column>
```

**CRITICAL**: Use `<field-ref>` NOT `<field>` inside `<form-list-column>`

## 2. Screen Hierarchy Pattern (Clean Parent)

### Parent Shell Screen (No Parameters)
```xml
<!-- Lc.xml (Parent) -->
<actions>
    <set field="lcId" from="lcId ?: lcSeqId"/>
    <if condition="lcId">
        <entity-find-one entity-name="moqui.trade.finance.LetterOfCredit" value-field="lc"/>
    </if>
</actions>

<widgets>
    <!-- Visibility handled by entity presence -->
    <section name="LcHeader" condition="lc">
        <widgets>
            <!-- Status chips, routing buttons -->
        </widgets>
    </section>
    
    <subscreens-panel type="tab" parent-name="Lc" if-active="true"/>
</widgets>
```

### Detail Sub-screen (REQUIRED Parameters)
```xml
<!-- MainLC.xml -->
<parameter name="lcId" required="true"/>

<actions>
    <entity-find-one entity-name="moqui.trade.finance.LetterOfCredit" value-field="lc"/>
</actions>
```

### Menu Clearing (Entry Point)
```xml
<!-- ImportLc.xml (menu) -->
<subscreens-item name="Lc" location="." parameter-map="[lcId:null]"/>
```

### Visibility Regex Guard
```xml
<section name="DetailHeader" 
         condition="lc &amp;&amp; sri.screenUrlInfo.extraPathNameList">
    <!-- Only shows when entity exists AND sub-path present -->
</section>
```

## 3. Form-Single (Create/Edit)

### Standard Create Form
```xml
<form-single name="CreateEntity" transition="createEntity">
    <field name="lcNumber">
        <default-field title="LC Number">
            <text-line required="true" size="20"/>
        </default-field>
    </field>
    
    <field name="lcStatusId">
        <default-field title="Status">
            <drop-down>
                <entity-options key="${statusId}" text="${description}">
                    <entity-find entity-name="moqui.basic.StatusItem">
                        <econdition field-name="statusId" operator="like" value="Lc%"/>
                    </entity-find>
                </entity-options>
            </drop-down>
        </default-field>
    </field>
    
    <field name="submitButton">
        <default-field title="Create">
            <submit/>
        </default-field>
    </field>
</form-single>
```

### Container Dialog Pattern
```xml
<container-dialog id="CreateDialog" button-text="Create LC">
    <form-single name="CreateLc" transition="createLc">
        <field name="lcNumber"><default-field><text-line required="true"/></default-field></field>
        <field name="submitButton"><default-field><submit/></default-field></field>
    </form-single>
</container-dialog>
```

## 4. Transition Patterns

### Create Transition
```xml
<transition name="createLc">
    <service-call name="moqui.trade.finance.TradeFinanceServices.create#LetterOfCredit"/>
    <default-response url="."/>
</transition>
```

### Delete with Confirmation
```xml
<transition name="deleteLc">
    <service-call name="moqui.trade.finance.TradeFinanceServices.delete#LetterOfCredit"/>
    <default-response url="."/>
</transition>

<!-- In form-list -->
<field name="actions">
    <default-field title="">
        <link url="deleteLc" text="Delete" style="text-negative"
              parameter-map="[lcId:lcSeqId]"
              confirmation="Confirm deletion?"/>
    </default-field>
</field>
```

### Relative Redirect (Quasar-safe)
```xml
<default-response url="./MainLC" parameter-map="[lcId:lcId]"/>
```

## 5. Quasar Styling

### Container Styling (NOT class attribute)
```xml
<container style="q-card shadow-2 q-pa-md">
    <label style="q-chip bg-primary text-white">Status</label>
</container>
```

### Button Text Conventions
| Action | Text |
|--------|------|
| Primary | `Create`, `Save` |
| Danger | `Delete` (style: `text-negative`) |
| Navigation | `text-weight-bold` |

## 6. Common Widgets

### Status Dropdown
```xml
<widget-template-include location="component://webroot/template/screen/BasicWidgetTemplates.xml#statusDropDown">
    <set field="statusTypeId" value="WorkEffort"/>
    <set field="allowMultiple" value="true"/>
</widget-template-include>
```

### Enum Dropdown
```xml
<widget-template-include location="component://webroot/template/screen/BasicWidgetTemplates.xml#enumDropDown">
    <set field="enumTypeId" value="LcType"/>
    <set field="allowEmpty" value="true"/>
</widget-template-include>
```

### Dynamic Options (AJAX)
```xml
<drop-down>
    <dynamic-options transition="searchPartyList" server-search="true" min-length="2"/>
</drop-down>
```

## 7. XML Validation (MANDATORY)

```bash
# Always validate before testing
xmllint --noout runtime/component/TradeFinance/screen/.../Screen.xml
```

### Common Errors
- Duplicate field definitions
- Unclosed tags (`</form-single>`, `</widgets>`)
- Extra closing tags
- Whitespace before XML prolog

## 8. Stale UI Cache Fix

```bash
./gradlew cleanAll
# Browser: Cmd+Shift+R (hard refresh)
```

## 9. Field Layout Rules

| Rule | Implementation |
|------|----------------|
| Links in list | Wrap in `<field>`, use `<field-ref>` |
| Quasar classes | Use `style=` not `class=` |
| Buttons | `Create`, `Save`, `Delete` (text-negative) |
| Back navigation | `${lastScreenUrl ?: '.'}` |

## 10. Shared Fragments (transition-include)

### Shared Transition File
```xml
<!-- template/lc/LcTransitions.xml -->
<transition name="createLc">
    <service-call name="moqui.trade.finance.TradeFinanceServices.create#LetterOfCredit"/>
    <default-response url="../MainLC" parameter-map="[lcId:lcId]"/>
</transition>
```

### Include in Screen
```xml
<transition-include name="createLc" location="component://.../template/lc/LcTransitions.xml"/>
```
