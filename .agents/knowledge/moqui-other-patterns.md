# Moqui Integration & Integration Patterns

## 1. XML Screen Patterns

### XML Prolog (REQUIRED)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!-- Missing prolog causes: SAXParseException: Content is not allowed in prolog -->
```

### Validation (MANDATORY)
```bash
xmllint --noout runtime/component/TradeFinance/screen/.../Screen.xml
```

### Common XML Errors
| Error | Cause |
|-------|-------|
| Duplicate field | Two `<field name="x">` |
| Unclosed tag | Missing `</form-single>` |
| Extra closing | `</field></field>` |
| Whitespace before prolog | ` \n<?xml...` |

## 2. Security

### Password Hashing
Demo users need SHA-256 hashes:
```
moqui -> d72023cb602fa4815410631f9d45a995
```

### Service Authentication
```xml
<!-- Require auth (default) -->
<service verb="update" noun="Order">

<!-- Public -->
<service verb="calculate" noun="ShippingRate" authenticate="false">
```

## 3. Integration Patterns

### Mock External Services
```xml
<service verb="cbs" noun="SendPayment" location="moqui.trade.finance.CbsServices">
    <!-- Mock implementation -->
    <script>
        // Return simulated response
        result.cbsReference = "MOCK-" + System.currentTimeMillis()
        result.success = true
    </script>
</service>
```

### REST Call Pattern
```groovy
import org.moqui.util.RestClient

def rc = ec.service.rest()
    .url("https://api.example.com/endpoint")
    .method(RestClient.METHOD_POST)
    .body([key: value], RestClient.JSON_CONTENT_TYPE)
    .call()

if (rc.statusCode == 200) {
    def response = rc.jsonObject
}
```

## 4. Screen Rendering

### XML vs FTL
- Screen XML parsed first
- Then rendered via FreeMarker templates
- Errors can occur at either stage

### Form Template Error
```
expression 'formInstance' was null
```
**Cause**: Using `<include-screen>` for dialog with missing context

**Solution**: Inline dialogs or ensure context parameters exist

## 5. Database Operations

### Entity Condition
```groovy
// LIKE query
import org.moqui.entity.EntityCondition
ec.entity.find("moqui.trade.finance.LetterOfCredit")
    .condition("lcNumber", EntityCondition.LIKE, "DEMO%")
    .list()

// IN query
import org.moqui.entity.EntityCondition
ec.entity.find("moqui.trade.finance.LetterOfCredit")
    .condition("statusId", EntityCondition.IN, ["LcDraft", "LcApproved"])
    .list()
```

### Cascade Delete
```groovy
// Delete children first
def children = ec.entity.find("moqui.trade.finance.LcHistory")
    .condition("lcId", lcId)
    .list()
children.each { it.delete() }

// Then parent
def lc = ec.entity.find("moqui.trade.finance.LetterOfCredit")
    .condition("lcId", lcId)
    .one()
lc.delete()
```

## 6. JSON/XML Responses

### Transition JSON Response
```xml
<transition name="getLcDetails" method="GET">
    <actions>
        <entity-find-one entity-name="moqui.trade.finance.LetterOfCredit" value-field="lc"/>
        <script>
            response."LcId" = lc.lcId
            response."LcNumber" = lc.lcNumber
            response."LcStatusId" = lc.lcStatusId
        </script>
    </actions>
    <response type="json"/>
</transition>
```

## 7. Stale UI Cache

### Symptom
Tabs persist after changing `subscreen-panel` type

### Fix
```bash
./gradlew cleanAll
# Browser: Cmd+Shift+R
```

## 8. Form Submit Issues

### Dropdown Behavior
- Static `<option>` tags may not submit correctly
- Use `entity-options` for reliable behavior
- `<text-line>` for manual input

```xml
<!-- Reliable -->
<drop-down>
    <entity-options key="${uomId}" text="${uomId}">
        <entity-find entity-name="moqui.basic.Uom"/>
    </entity-options>
</drop-down>

<!-- May fail -->
<drop-down>
    <option text="USD" value="USD"/>
</drop-down>
```

## 9. Conditional Logic

### If-Else Structure
```xml
<if condition="status == 'Active'">
    <set field="label" value="label-success"/>
    <log message="Status is active"/>
    <else>
        <set field="label" value="label-danger"/>
    </else>
</if>
```

**CRITICAL**: `<else>` must be INSIDE `<if>`, not as sibling

## 10. Double Test Execution

### Problem
JUnit Suite + Spock discovery = tests run twice

### Solution (build.gradle)
```gradle
test {
    useJUnitPlatform {
        filter {
            includeTestsMatching '*Suite'
        }
    }
}
```
