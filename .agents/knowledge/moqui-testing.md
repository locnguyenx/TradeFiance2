# Moqui Testing Patterns

> Verified patterns for Spock/Groovy testing in TradeFinance

## 1. Test Environment

### Always Use reloadSave (MANDATORY)
```bash
# Before EVERY test run
./gradlew reloadSave :runtime:component:TradeFinance:test

# For specific test
./gradlew reloadSave :runtime:component:TradeFinance:test --tests moqui.trade.finance.TradeFinanceScreensSpec
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
package moqui.trade.finance

import moqui.Moqui
import spock.lang.Specification

class TradeFinanceServicesSpec extends Specification {
    
    @Shared ExecutionContext ec
    
    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.artifactExecution.disableAuthz()
        ec.entity.makeDataLoader().load()
        ec.user.loginUser("tf-admin", "moqui")
    }
    
    def setup() {
        ec.message.clearAll()
        ec.artifactExecution.enableAuthz()
    }
    
    def cleanup() {
        // cleanup test data
    }
    
    def cleanupSpec() {
        ec.destroy()
    }
}
```

### Test Naming
```groovy
def "should create LetterOfCredit when valid data provided"() {
    when:
    def result = ec.service.sync()
        .name("moqui.trade.finance.TradeFinanceServices.create#LetterOfCredit")
        .parameters([lcNumber: "DEMO-001", lcAmount: 10000])
        .call()
    
    then:
    result.lcId
    result.success == true || result.success == "true"
}
```

## 3. Common Assertions

### Type Coercion Trap
```groovy
// STRING "true" vs BOOLEAN true
assert result.success == true || result.success == "true"

// Avoid exact size checks
assert history.size() >= 5
assert history.find { it.field == value }
```

### Screen Test
```groovy
def "should render LC list screen"() {
    when:
    ScreenTestRender str = screenTest.render("TradeFinance/ImportLc/Lc/FindLc", [:], null)
    
    then:
    !str.errorMessages
    !str.output.contains("Error rendering")
    str.assertContains("Letter of Credit")
}
```

### Parent Screen Visibility
```groovy
def "parent screen with ID does NOT render detail tabs"() {
    when:
    ScreenTestRender str = screenTest.render("TradeFinance/ImportLc/Lc", [lcId: "DEMO_01"], null)
    
    then:
    !str.errorMessages
    str.assertContains("Find LC") // List header
    !str.output.contains("Detail Header") // No detail tabs
}
```

## 4. Data Setup

### Sequence Safety (Auto-increment PKs)
```groovy
def setupSpec() {
    // ...
    // Set sequence to avoid collisions
    ec.entity.tempSetSequencedIdPrimary("moqui.trade.finance.LetterOfCredit", 960000, 100)
}
```

### Authorization Bypass (Setup Only)
```groovy
def setupSpec() {
    ec = Moqui.getExecutionContext()
    ec.artifactExecution.disableAuthz() // Only for setup
    ec.entity.makeDataLoader().load()
    ec.user.loginUser("tf-admin", "moqui")
}

def setup() {
    ec.artifactExecution.enableAuthz() // Re-enable for tests
}
```

### Cleanup Pattern
```groovy
def cleanupProvision(String lcId) {
    def provisions = ec.entity.find("moqui.trade.finance.LcProvision")
        .condition("lcId", lcId)
        .list()
    provisions.each { it.delete() }
}
```

## 5. Service Testing

### Full Service Path
```groovy
// Use full path
ec.service.sync()
    .name("moqui.trade.finance.TradeFinanceServices.create#LetterOfCredit")
    .call()

// NOT short name
ec.service.sync().name("create#LcDrawing") // May fail
```

### Check Errors
```groovy
if (ec.message.hasError()) {
    logger.info("Errors: ${ec.message.errors}")
}
```

### Verify Database State
```groovy
// After service call, verify persisted state
def lc = ec.entity.find("moqui.trade.finance.LetterOfCredit")
    .condition("lcId", lcId)
    .one()
assert lc.lcStatusId == "LcApproved"
```

### Read-Refresh-Update Pattern
```groovy
def "status transition updates parent record"() {
    when:
    // Create LC
    def createResult = ec.service.sync()
        .name("moqui.trade.finance.TradeFinanceServices.create#LetterOfCredit")
        .parameters([lcNumber: "DEMO-001"])
        .call()
    
    def lcId = createResult.lcId
    
    // Transition
    ec.service.sync()
        .name("moqui.trade.finance.TradeFinanceServices.transition#Status")
        .parameters([lcId: lcId, toStatusId: "LcApproved"])
        .call()
    
    then:
    // CRITICAL: Query fresh from DB, not from cache
    def lc = ec.entity.find("moqui.trade.finance.LetterOfCredit")
        .condition("lcId", lcId)
        .one()
    lc.lcStatusId == "LcApproved"
}
```

## 6. TDD Workflow

### RED: Test the Workflow
```groovy
def "adding entry updates collected amount"() {
    when:
    // Create collection
    def createResult = ec.service.sync()
        .name("moqui.trade.finance.ProvisionCollectionServices.create#Collection")
        .call()
    def collectionId = createResult.collectionId
    
    // Add entry (this SHOULD populate collectedAmount)
    ec.service.sync()
        .name("moqui.trade.finance.ProvisionCollectionServices.add#CollectionEntry")
        .parameters([collectionId: collectionId, amount: 5000])
        .call()
    
    then:
    // CRITICAL: Verify DB state, not service output
    def coll = ec.entity.find("moqui.trade.finance.LcProvisionCollection")
        .condition("collectionId", collectionId)
        .one()
    coll.collectedAmount == 5000
}
```

## 7. UI Test (ScreenTestRender)

### Strict Assertions
```groovy
expect:
!str.errorMessages
!str.output.contains("Error rendering")
!str.output.contains("EntityException")
!str.output.contains("Freemarker Error")
str.assertContains("Expected UI Text")
```

### @Ignore Legacy Tests
```groovy
@Ignore("Quasar lazy loading causes false failures")
def "should show all fields"() {
    // Legacy screen rendering test
}
```

## 8. Common Errors

### NoClassDefFoundError
```bash
./gradlew cleanAll loadSave
```

### Data Integrity Violation
```bash
./gradlew cleanDb loadSave
```

### Sequence Collision
```groovy
ec.entity.tempSetSequencedIdPrimary("EntityName", 960000, 100)
```

## 9. Debugging

### Log Variables
```groovy
logger.info("lcId: ${lcId}, status: ${lc?.lcStatusId}")
```

### Check Messages
```groovy
if (ec.message.hasError()) {
    logger.info("Service errors: ${ec.message.errors}")
}
```

### Check Log File
```bash
tail -f runtime/log/moqui.log
```

## 10. Mantle GL Configuration (Required for Financial Tests)

### Prerequisites for Invoice/Payment
1. Party has `OrgInternal` role
2. GL Mappings configured (`mantle.ledger.config.ItemTypeGlAccount`)
3. Internal Account Links (`mantle.ledger.account.GlAccountOrganization`)
4. Accounting Preference (`mantle.ledger.config.PartyAcctgPreference`)
5. Open FiscalMonth for organization
6. InvoiceTypeTransType mapping exists
