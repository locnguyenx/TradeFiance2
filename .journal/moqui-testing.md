# Moqui Testing Patterns

> Verified patterns for Spock/Groovy testing in Moqui

## 1. Test Environment

### Always Use reloadSave (MANDATORY)
```bash
# Before EVERY test run
./gradlew reloadSave :runtime:component:Example:test

# For specific test
./gradlew reloadSave :runtime:component:Example:test --tests moqui.example.ExampleSpec
```

### Reset Patterns

#### Data Issue
```bash
./gradlew cleanDb loadSave
```

#### System Hang / Corruption
```bash
./gradlew cleanAll
./gradlew loadSave
```

## 2. Spock Test Structure

### Standard Spec
```groovy
package moqui.example

import org.moqui.context.ExecutionContext
import moqui.Moqui
import spock.lang.Specification
import spock.lang.Shared

class ExampleServicesSpec extends Specification {
    
    @Shared ExecutionContext ec
    
    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.artifactExecution.disableAuthz()
        ec.entity.makeDataLoader().load()
        ec.user.loginUser("admin", "moqui")
    }
    
    def setup() {
        ec.message.clearAll()
        ec.artifactExecution.enableAuthz()
    }
    
    def cleanupSpec() {
        ec.destroy()
    }
}
```

### Test Naming
```groovy
def "should create Example when valid data provided"() {
    when:
    def result = ec.service.sync()
        .name("moqui.example.ExampleServices.create#Example")
        .parameters([name: "DEMO-001", amount: 10000])
        .call()
    
    then:
    result.id
    result.success == true || result.success == "true"
}
```

## 3. Common Assertions

### Type Coercion Trap
```groovy
// STRING "true" vs BOOLEAN true
assert result.success == true || result.success == "true"

// Avoid exact size checks
assert list.size() >= 1
assert list.find { it.field == value }
```

### Screen Test
```groovy
def "should render list screen"() {
    when:
    ScreenTestRender str = screenTest.render("Example/FindExample", [:], null)
    
    then:
    !str.errorMessages
    !str.output.contains("Error rendering")
    str.assertContains("Expected Header")
}
```

## 4. Data Setup

### Sequence Safety (Auto-increment PKs)
```groovy
ec.entity.tempSetSequencedIdPrimary("moqui.example.Example", 960000, 100)
```

### Authorization Bypass (Setup Only)
```groovy
ec.artifactExecution.disableAuthz() 
```

#### Transactional Robust Cleanup
Used in `TradePartySpec.groovy` to handle complex dependency chains safely.
```groovy
def setupSpec() {
    ec = Moqui.getExecutionContext()
    // ... setup ...
    boolean began = ec.transaction.begin(60)
    try {
        // Order matters for FK constraints
        ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", EntityCondition.LIKE, "SPEC_%").deleteAll()
        ec.entity.find("trade.TradePartyBank").condition("partyId", EntityCondition.LIKE, "SPEC_%").deleteAll()
        ec.entity.find("trade.TradeParty").condition("partyId", EntityCondition.LIKE, "SPEC_%").deleteAll()
        ec.transaction.commit(began)
    } catch (Exception e) {
        ec.transaction.rollback(began, "Error in setupSpec", e)
        throw e
    }
}
```

## 5. Service Testing

### Full Service Path
```groovy
ec.service.sync()
    .name("moqui.example.ExampleServices.create#Example")
    .call()
```

### Check Errors
```groovy
if (ec.message.hasError()) {
    logger.info("Service errors: ${ec.message.errors}")
}
```

### Verify Persisted State
```groovy
def record = ec.entity.find("moqui.example.Example")
    .condition("id", id)
    .one()
assert record.statusId == "Approved"
```

## 6. UI Test (ScreenTestRender)

### Strict Assertions
```groovy
expect:
!str.errorMessages
!str.output.contains("Error rendering")
!str.output.contains("EntityException")
!str.output.contains("Freemarker Error")
str.assertContains("Expected UI Text")
```

## 7. Common Errors

### NoClassDefFoundError
```bash
./gradlew cleanAll loadSave
```

## 8. Debugging

### Log Variables
```groovy
logger.info("id: ${id}, status: ${status}")
```

### Check Log File
```bash
tail -f runtime/log/moqui.log
```

## 9. ScreenTest & Testing Limitations
* **WebFacadeStub Limitations**: `ScreenTest` uses a stubbed web context. If a service call triggers `ec.message.addError()`, the framework attempts to save it to the session. The stub is not a full `WebFacadeImpl` and this triggers a `NullPointerException`. Always check if errors are originating from missing parameters causing error reporting NPEs.
* **JSON Parsing in Spock**: `ScreenTestRender.getJsonObject()` is unreliable in the Spock test runner. Use `groovy.json.JsonSlurper().parseText(str.output)` for consistent results and better error messages.
* **Log Buffering**: Redirect test output to a temp file (like `/tmp/diag.txt`) to inspect full JSON payloads and stack traces that Gradle truncates.
* **Namespace Consistency**: Moving specs to the correct package is critical for accurate discovery by the Moqui test runner.

## 10. Master Data & Referential Integrity
* **Test Isolation**: Integration tests will fail on a blank environment due to missing core enumerations (`InternalOrganization`, `AcctgTransType`, etc.). Consolidate these into a master seed file (e.g. `MasterData.xml`) to ensure a "bootstrappable" testing environment.
* **Data Dependency**: Always seed master data (`reloadSave`) before test runs to ensure referential integrity.

## 11. Double Test Execution
### Problem
JUnit Suite + Spock discovery = tests run twice.
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
