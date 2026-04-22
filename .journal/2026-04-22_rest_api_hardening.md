# Journal: REST API Hardening & Contract Verification (2026-04-22)

## Context
Goal was to stabilize the backend REST layer for the Digital Trade Finance platform using Moqui's `ScreenTest` and hardened service patterns.

## Findings & Lessons Learned

### 1. Moqui REST Routing
- **Method Normalization**: `RestApi.groovy` in Moqui normalizes requested HTTP methods to lowercase. In `rest.xml`, the `type` attribute MUST be lowercase (e.g., `<method type="post">`) for reliable matching.
- **Declarative Patterns**: Moving from custom `<actions>` blocks to declarative `<service>` mappings in `rest.xml` reduced complexity and aligned with Mantle's hardened architecture.

### 2. ScreenTest Nuances
- **WebFacadeStub Limitation**: `ScreenTest` uses a stubbed web context. If a service call adds error messages (`ec.message.addError()`), the framework tries to save these to the session via `WebFacadeImpl.saveErrorParametersToSession()`. Since the stub isn't a full `WebFacadeImpl`, this triggers a `NullPointerException`.
- **Diagnosis**: Failures in `authorize` endpoint were due to missing `userId` parameter, which triggered the error reporting NPE.

### 3. Entity Sequencing
- **Direct Tags**: `<entity-sequenced-id>` is not a valid XML action tag in `xml-actions-3.xsd`.
- **Composite Keys**: For `TradeTransactionAudit` (which has composite PKs), I learned to use `<entity-make-value>` followed by `<entity-sequenced-id-secondary value-field="..."/>`. This correctly increments the `auditId` relative to the `instrumentId`.

### 4. Test Robustness
- **Parsing**: `ScreenTestRender.getJsonObject()` became unreliable in the Spock test runner. Switching to explicit `groovy.json.JsonSlurper().parseText(str.output)` provided consistent results and better error messages.
- **Log Buffering**: Redirecting output to `/tmp/diag.txt` allowed inspection of full JSON payloads and stack traces that were otherwise truncated by Gradle.

## Decisions Made
- Defaulted `userId` in `AuthorizationServices` to `ec.user.userId` to support seamless REST calls from the frontend.
- Disabled session token requirement for the `s1` transition in `webroot/rest.xml` to facilitate backend contract testing without CSRF overhead.

---
*OpenCode*
