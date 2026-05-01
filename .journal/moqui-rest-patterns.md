# Moqui REST API Patterns & Best Practices

## 1. Registration (`*.rest.xml`)
Files should be placed in the `service/` directory of a component and matched via the `rest.xml` facade.

### Core Elements
- **`<resource>`**: Defines a URL path segment.
- **`<id>`**: Defines a parameterized path segment (e.g., `/items/{itemId}`).
- **`<method>`**: Defines the HTTP verb (lowercase).
- **`<service>`**: Maps to a Moqui service.
- **`<entity>`**: Maps directly to an entity operation.

### Example Pattern
```xml
<resource name="customers">
    <method type="get"><service name="CustomerServices.get#CustomerList"/></method>
    <id name="customerId">
        <method type="get"><service name="CustomerServices.get#Customer"/></method>
        <resource name="validate">
            <method type="post"><service name="CustomerServices.validate#Customer"/></method>
        </resource>
    </id>
</resource>
```

## 2. REST API Routing & Setup
* **Method Normalization**: `RestApi.groovy` normalizes HTTP methods to lowercase. In `rest.xml`, the `type` attribute MUST be lowercase (e.g., `<method type="post">`) for reliable matching.
* **Path Parameter Extraction**: The standard `{parameter}` syntax for path parameters in `rest.xml` can fail. Use the `<id name="parameterName">` wrapper tag around the `<method>` instead. This is the more robust pattern for path parameter extraction in Moqui.
* **Declarative Routing**: Favor declarative `<service>` mappings in `rest.xml` over custom `<actions>` blocks to align with Mantle architecture and reduce complexity.

## 3. Service Implementation
- **Authentication**: Use `require-authentication="true"` on the service or method level.
- **Identity**: Derive user identity from `ec.user.userId` rather than passing it in the payload.
- **Parameters**: Use declarative `in-parameters` and `out-parameters`.

## 3. Contract Testing (`ScreenTest`)
### Setup Pattern
```groovy
screenTest = ec.screen.makeTest()
    .rootScreen("component://webroot/screen/webroot.xml")
    .baseScreenPath("rest")
```

### Verification Pattern
- Use `JsonSlurper` to parse response.
- Check `str.errorMessages`.

## 4. Troubleshooting
- **405 Method Not Supported**: Ensure `type` is lowercase.
- **404 Not Found**: Verify resource hierarchy and `baseScreenPath`.

## 5. Security & Authentication
### Password Hashing
Demo users often use SHA-256 hashes.

## 6. Integration Patterns
### Mock External Services
```xml
<service verb="external" noun="SendNotification" location="moqui.example.MockServices">
    <script>
        result.reference = "MOCK-" + System.currentTimeMillis()
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
```
