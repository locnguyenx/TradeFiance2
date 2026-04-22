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
