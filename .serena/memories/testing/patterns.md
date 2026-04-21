# Testing Patterns

## Framework Tests (18)

Location: `framework/src/test/groovy/`

| Test | Description |
|------|-------------|
| `MoquiSuite` | Test suite runner |
| `EntityCrud` | CRUD operations |
| `ServiceFacadeTests` | Service calls |
| `UserFacadeTests` | User authentication |

## TradeFinance Tests (20)

Location: `runtime/component/TradeFinance/src/test/groovy/`

Pattern: `*Spec.groovy` (Spock Framework)

### Test Structure

```groovy
class TradeFinanceServicesSpec extends Specification {
    @Shared ExecutionContext ec
    
    def setup() {
        // Setup before each feature
    }
    
    def "test name"() {
        when:
        def result = ec.service.sync().name("create#LetterOfCredit")
        
        then:
        result.success == true || result.success == "true"
    }
}
```

## Running Tests

```bash
./gradlew test
```

## Key Patterns

1. Use `@Stepwise` for ordered tests
2. Use `@Shared` for shared state
3. Mock collaborators, test behaviors
4. Assert with `result.success == true || result.success == "true"`
