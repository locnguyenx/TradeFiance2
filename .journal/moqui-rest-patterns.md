# Moqui REST API Patterns & Best Practices

## 1. Registration (`*.rest.xml`)
Files should be placed in the `service/` directory of a component and matched via the `rest.xml` facade.

### Core Elements
- **`<resource>`**: Defines a URL path segment.
- **`<id>`**: Defines a parameterized path segment (e.g., `/items/{itemId}`). The value is passed as a parameter with the name of the `id` element.
- **`<method>`**: Defines the HTTP verb. **Note:** Moqui normalizes methods to lowercase.
- **`<service>`**: Maps the method to a Moqui service.
- **`<entity>`**: Maps the method directly to an entity operation (`one`, `list`, `create`, etc.).

### Example Pattern
```xml
<resource name="import-lc">
    <method type="get"><service name="ImportLcServices.get#ImportLetterOfCreditList"/></method>
    <id name="instrumentId">
        <method type="get"><service name="ImportLcServices.get#ImportLetterOfCredit"/></method>
        <resource name="authorize">
            <method type="post"><service name="AuthorizationServices.authorize#Instrument"/></method>
        </resource>
    </id>
</resource>
```

## 2. Service Implementation
Services for REST APIs should follow standard Moqui patterns:
- **Authentication**: Use `require-authentication="true"` on the service or method level.
- **Identity**: Derive user identity from `ec.user.userId` rather than passing it in the payload.
- **Parameters**: Use declarative `in-parameters` and `out-parameters`.
- **Audit**: Use `entity-sequenced-id-secondary` for composite primary keys (e.g., audit trails).

## 3. Contract Testing (`ScreenTest`)
Testing should be done at the HTTP level using `ScreenTest` to verify the facade mapping.

### Setup Pattern
```groovy
screenTest = ec.screen.makeTest()
    .rootScreen("component://webroot/screen/webroot.xml")
    .baseScreenPath("rest") // Mount point
```

### Verification Pattern
- Use `JsonSlurper` to parse `str.output` for robust assertions.
- Check `str.errorMessages` for engine-level failures.
- Verify status codes and JSON structure.

## 4. Troubleshooting
- **NPE in ScreenTest**: Often caused by errors in the service being reported to the `WebFacadeStub` which isn't fully session-aware.
- **405 Method Not Supported**: Double-check that the `type` in `rest.xml` is lowercase.
- **404 Not Found**: Ensure the `resource` hierarchy matches the URI and that `baseScreenPath` is correct.

---
*OpenCode Moqui Knowledge Base*

## 5. REST API Routing & Setup (From patterns)
* **Method Normalization**: `RestApi.groovy` normalizes HTTP methods to lowercase. In `rest.xml`, the `type` attribute MUST be lowercase (e.g., `<method type="post">`) for reliable matching.
* **Path Parameter Extraction**: The standard `{parameter}` syntax for path parameters in `rest.xml` can fail. Use the `<id name="parameterName">` wrapper tag around the `<method>` instead. This is the more robust pattern for path parameter extraction in Moqui.
* **Declarative Routing**: Favor declarative `<service>` mappings in `rest.xml` over custom `<actions>` blocks to align with Mantle architecture and reduce complexity.

## 6. Security & Authentication
### Password Hashing
Demo users need SHA-256 hashes: `moqui -> d72023cb602fa4815410631f9d45a995`

### Service Authentication
```xml
<!-- Require auth (default) -->
<service verb="update" noun="Order">

<!-- Public -->
<service verb="calculate" noun="ShippingRate" authenticate="false">
```

## 7. Integration Patterns
### Mock External Services
```xml
<service verb="cbs" noun="SendPayment" location="trade.CbsServices">
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

## 8. JSON/XML Responses
### Transition JSON Response
```xml
<transition name="getLcDetails" method="GET">
    <actions>
        <entity-find-one entity-name="trade.LetterOfCredit" value-field="lc"/>
        <script>
            response."LcId" = lc.lcId
            response."LcNumber" = lc.lcNumber
            response."LcStatusId" = lc.lcStatusId
        </script>
    </actions>
    <response type="json"/>
</transition>
```
