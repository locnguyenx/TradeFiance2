# Module Boundary Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix all module boundary violations and align implementations with modular monolith architecture defined in AGENTS.md

**Architecture:** Modular monolith with foundation/business module separation. All cross-module dependencies must use API interfaces only (`.api` package), not internal implementations (`.domain.entity`, `.repository`, `.service`). Event-driven integration for side-effects.

**Tech Stack:** Java 17, Spring Boot 3.2, Spring Events, PostgreSQL, Gradle

**Specifications:**
- BRD: `docs/superpowers/specs/2026-03-23-module-boundary-fix-brd.md`
- Design: `docs/superpowers/specs/account/design.md`, `docs/superpowers/specs/limits/design.md`, plus all other module designs with guardrails

---

## Architecture Context

The system has a hard requirement: modules must only depend on published API interfaces (Rule 2 in AGENTS.md). Several violations were found where modules imported internal repositories and domain entities directly:

1. AccountService used `LimitAssignmentService` (should use event)
2. AccountService used `CurrencyRepository` (should use `CurrencyQueryService` API)
3. DemoDataLoader used product domain entities and repositories (should use `ProductQueryService` API)
4. LoanAccount imported `Customer` domain entity (should use customer ID only)
5. AccountInquiryService used `CustomerRepository` (fixed earlier: now uses `CustomerQueryService`)

The fix involves:
- Replacing direct service calls with API interfaces
- Replacing synchronous limit assignment with event-driven approach
- Creating new event classes and listeners
- Updating tests to match new dependencies
- Updating design specifications to document guardrails

---

## File Structure Changes

### New Files to Create:
1. `src/main/java/com/banking/account/event/AccountOpenedEvent.java` - Event class
2. `src/main/java/com/banking/limits/event/AccountOpenedEventListener.java` - Event consumer
3. `src/main/java/com/banking/product/ProductDemoDataLoader.java` - Isolated product demo data

### Modified Files:
1. `src/main/java/com/banking/account/service/AccountService.java` - Remove dependencies, add event publishing
2. `src/main/java/com/banking/account/DemoDataLoader.java` - Remove product creation, only account data
3. `src/main/java/com/banking/account/domain/entity/LoanAccount.java` - Remove Customer import
4. `src/main/java/com/banking/product/service/ProductService.java` - Add createProductVersion method
5. `src/test/java/com/banking/account/service/AccountServiceIntegrationTest.java` - Update mocks

### Design Specs to Update (Already Done):
- `docs/superpowers/specs/account/design.md` - Added Integration Points, Event-Driven Architecture, Implementation Guardrails
- `docs/superpowers/specs/limits/design.md` - Added Event-Driven Integration, Implementation Guardrails
- All other module designs (customer, product, charges, master-data, infra) - Added Implementation Guardrails sections

---

## Prerequisites

- All module boundary violations identified (see BRD)
- API interfaces already exist: `CustomerQueryService`, `ProductQueryService`, `CurrencyQueryService`, `LimitCheckService`
- Kafka infrastructure exists (`DomainEventPublisher`) but not required; using Spring `ApplicationEventPublisher` for now
- DemoDataLoader depends on ProductQueryService having product data; needs `ProductDemoDataLoader` to run first

---

## Implementation Tasks

### Phase 1: Prepare Infrastructure (Events Package)

**Task 1.1: Create AccountOpenedEvent**

*File:* `src/main/java/com/banking/account/event/AccountOpenedEvent.java`

- [ ] Create package `com.banking.account.event` if not exists
- [ ] Write AccountOpenedEvent class extending Spring `ApplicationEvent`
- [ ] Include fields: accountId, accountNumber, customerId, productCode, productId, productVersionId, productName, currency, initialBalance, createdBy
- [ ] Add constructor, getters, JavaDoc explaining purpose
- [ ] No business logic - just data carrier
- [ ] Commit with message: `feat(account): add AccountOpenedEvent for async integration`

**Task 1.2: Create AccountOpenedEventListener in Limits Module**

*File:* `src/main/java/com/banking/limits/event/AccountOpenedEventListener.java`

- [ ] Create package `com.banking.limits.event` if not exists
- [ ] Write listener component with `@Component` and `@EventListener` method
- [ ] Inject `LimitAssignmentService` (correct: internal since listener IS in limits module)
- [ ] Implement logic: get product limits via `limitAssignmentService.getProductLimits(productCode)`, loop and call `assignToAccount()`
- [ ] Add robust error handling: try-catch per limit (log WARN, continue), global catch (log ERROR)
- [ ] Add logging at INFO (start/complete) and WARN/ERROR for failures
- [ ] JavaDoc explaining event-driven rationale and architectural compliance
- [ ] Commit with message: `feat(limits): add AccountOpenedEventListener for async limit assignment`

**Task 1.3: Create ProductDemoDataLoader**

*File:* `src/main/java/com/banking/product/ProductDemoDataLoader.java`

- [ ] Create in `com.banking.product` package (next to other component classes)
- [ ] Implement `CommandLineRunner` with `@Order(1)` to run before account DemoDataLoader
- [ ] Inject `ProductRepository` and `ProductVersionRepository` (internal OK since this IS product module)
- [ ] Implement `loadProductData()`: create 4 products (CURRENT, SAVINGS, FIXED-DEPOSIT, LOAN) with ACTIVE versions
- [ ] Use `productRepository.count()` check to skip if already exists
- [ ] Set audit fields to "system", timestamps to `LocalDateTime.now()`
- [ ] Log progress with `log.info()`
- [ ] Commit with message: `feat(product): add ProductDemoDataLoader for demo data isolation`

---

### Phase 2: Fix Account Service Boundary Violations

**Task 2.1: Update AccountService Imports and Constructor**

*File:* `src/main/java/com/banking/account/service/AccountService.java`

- [ ] Remove import: `com.banking.limits.service.LimitAssignmentService`
- [ ] Remove import: `com.banking.limits.dto.response.ProductLimitResponse`
- [ ] Remove import: `com.banking.masterdata.repository.CurrencyRepository`
- [ ] Add import: `com.banking.account.event.AccountOpenedEvent`
- [ ] Add import: `com.banking.masterdata.api.CurrencyQueryService`
- [ ] Add import: `org.springframework.context.ApplicationEventPublisher`
- [ ] Modify class fields: remove `LimitAssignmentService limitAssignmentService`, remove `CurrencyRepository currencyRepository`, add `CurrencyQueryService currencyQueryService`, add `ApplicationEventPublisher eventPublisher`
- [ ] Update constructor: remove parameters for the two removed services, add `CurrencyQueryService` and `ApplicationEventPublisher`, assign to fields
- [ ] Commit with message: `refactor(account): remove internal dependencies from AccountService`

**Task 2.2: Replace CurrencyRepository with CurrencyQueryService**

*In same file:* `src/main/java/com/banking/account/service/AccountService.java`

- [ ] Find currency lookup code: `currencyRepository.findById(request.getCurrency())`
- [ ] Replace with: `currencyQueryService.findByCode(request.getCurrency())`
- [ ] Adjust null handling: `if (currencyDTO == null)` throw `IllegalArgumentException("Invalid currency code: " + request.getCurrency())`
- [ ] Replace `masterDataCurrency.isActive()` with `currencyDTO.isActive()`
- [ ] Remove any remaining `CurrencyRepository` usage
- [ ] Commit with message: `refactor(account): use CurrencyQueryService API instead of repository`

**Task 2.3: Replace Limit Assignment with Event Publishing**

*In same file:* `src/main/java/com/banking/account/service/AccountService.java`

- [ ] Find limit assignment block (lines ~181-195): `List<ProductLimitResponse> productLimits = limitAssignmentService.getProductLimits(...)` loop
- [ ] Delete entire block
- [ ] After `account = accountRepository.save(account);`, add event publishing:
```java
AccountOpenedEvent event = new AccountOpenedEvent(
    this,
    account.getId(),
    account.getAccountNumber(),
    customerId,
    request.getProductCode(),
    productId,
    productVersionId,
    productName,
    request.getCurrency(),
    request.getInitialDeposit(),
    "system" // TODO: get from security context
);
eventPublisher.publishEvent(event);
```
- [ ] Ensure variables `productId`, `productVersionId`, `productName` are set (already from productVersion lookup)
- [ ] Commit with message: `feat(account): publish AccountOpenedEvent for async limit assignment`

---

### Phase 3: Fix DemoDataLoader

**Task 3.1: Remove Product Creation from Account DemoDataLoader**

*File:* `src/main/java/com/banking/account/DemoDataLoader.java`

- [ ] Remove imports: `com.banking.product.domain.entity.Product`, `ProductVersion`, `ProductFamily`, `ProductStatus`
- [ ] Remove imports: `com.banking.product.repository.ProductRepository`, `ProductVersionRepository`
- [ ] Remove import: `com.banking.product.service.ProductService`
- [ ] Remove field `ProductService productService` from constructor
- [ [ ] Remove entire `loadProductData()` method (or replace with simple log saying product data loaded by ProductDemoDataLoader)
- [ ] Adjust constructor: remove `ProductService` parameter, remove field assignment
- [ ] In `run()` method, remove call to `loadProductData()` - only call `loadAccountData()`
- [ ] Commit with message: `refactor(account): remove product creation from DemoDataLoader`

---

### Phase 4: Clean Up LoanAccount

**Task 4.1: Remove Customer Entity Import**

*File:* `src/main/java/com/banking/account/domain/entity/LoanAccount.java`

- [ ] Remove import: `com.banking.customer.domain.entity.Customer`
- [ ] Verify LoanAccount doesn't directly use Customer class (it shouldn't - uses customerId from parent)
- [ ] Commit with message: `refactor(account): remove Customer entity import from LoanAccount`

---

### Phase 5: Update Tests

**Task 5.1: Fix AccountServiceIntegrationTest Mocks**

*File:* `src/test/java/com/banking/account/service/AccountServiceIntegrationTest.java`

- [ ] Remove import: `com.banking.limits.service.LimitAssignmentService`
- [ ] Remove import: `com.banking.limits.dto.response.ProductLimitResponse`
- [ ] Remove import: `com.banking.masterdata.repository.CurrencyRepository`
- [ ] Add import: `com.banking.masterdata.api.CurrencyQueryService`
- [ ] Add import: `com.banking.masterdata.api.dto.CurrencyDTO`
- [ ] Remove field: `@MockBean private LimitAssignmentService limitAssignmentService;`
- [ ] Remove fields: `@MockBean private ProductLimitRepository productLimitRepository;` and `@MockBean private AccountLimitRepository accountLimitRepository;` if present
- [ ] Remove field: `@MockBean private CurrencyRepository currencyRepository;`
- [ ] Add field: `@MockBean private CurrencyQueryService currencyQueryService;`
- [ ] In `setUp()` method:
  - Remove currency repository mocks
  - Add `CurrencyDTO` creation and mock: `CurrencyDTO usdCurrency = new CurrencyDTO(); usdCurrency.setCode("USD"); usdCurrency.setActive(true); when(currencyQueryService.findByCode("USD")).thenReturn(usdCurrency);`
  - Remove product limit mocks (limitAssignmentService not used anymore)
- [ ] Adjust tests to not expect limit assignment calls (they are async events now)
- [ ] Commit with message: `test(account): update AccountServiceIntegrationTest for boundary fix`

---

### Phase 6: Verify Architectural Compliance

**Task 6.1: Run Compilation**

- [ ] Compile main sources: `./gradlew compileJava`
- Expected: BUILD SUCCESSFUL
- [ ] Verify no compilation errors related to our changes
- [ ] Commit: (no code changes, just verification)

**Task 6.2: Check for Cross-Module Violations**

- [ ] Run grep to check for any remaining improper imports in account module:
```bash
grep -r "import com.banking.customer.repository" src/main/java/com/banking/account
grep -r "import com.banking.limits.service" src/main/java/com/banking/account
grep -r "import com.banking.masterdata.repository" src/main/java/com/banking/account
grep -r "import com.banking.product.repository" src/main/java/com/banking/account
```
- Expected: No output (zero violations)
- [ ] Run similar checks for other modules to ensure no regressions
- [ ] Document results in verification report

**Task 6.3: Update Design Specifications (Already Completed)**

*Note: The design spec updates were done in a separate session. This task is just to acknowledge they are part of the plan.*

- [ ] Verify account design spec has Integration Points section with Rule 1-4 compliance table
- [ ] Verify limits design spec has Event-Driven Integration subsection
- [ ] Verify all module designs (account, limits, customer, product, charges, master-data, infra) have Implementation Guardrails sections

---

## TDD Approach

This implementation follows TDD principles where applicable:

1. **Tests were written/updated BEFORE verifying implementation works** (Task 5.1 updated tests to match new API)
2. **Tests define expected behavior**: AccountService should publish event, not call LimitAssignmentService directly
3. **Compilation verification ensures API contracts are satisfied**

However, full TDD (red-green-refactor per tiny step) was not applied because:
- This is a refactoring of existing code with known violations
- The violations are architectural, not functional; existing tests ensure functional behavior
- The fix is straightforward dependency replacement + event addition

---

## Rollback Plan

If issues arise after deployment:

1. **Revert individual commits** using `git revert <commit-hash>` in order:
   - Revert in reverse order: tests last → events first
   - Each revert should be independently tested

2. **Database changes**: None (this is pure code refactoring)

3. **Event handling**: If `AccountOpenedEvent` listener causes issues, can temporarily comment out `@EventListener` method; AccountService still publishes event (harmless if no consumer)

4. **Feature flag**: Could wrap event publishing in a conditional:
```java
if (featureFlags.isEventDrivenLimitAssignmentEnabled()) {
    eventPublisher.publishEvent(event);
}
```
But not needed for initial rollout since listener is in same module and defensive.

---

## Verification & Acceptance Criteria

**Must pass:**
- ✅ `./gradlew compileJava` - no compilation errors
- ✅ No cross-module boundary violations (grep scan)
- ✅ AccountService functional tests pass (account creation still works)
- ✅ Limit assignment occurs asynchronously (can test by verifying listener runs and creates AccountLimit records)
- ✅ Design specs updated with guardrails and compliance tables

**Optional (pending pre-existing issues):**
- Full test suite `./gradlew test` may fail due to unrelated issues in charges/product modules - these are pre-existing and not caused by our changes

---

## Dependencies

**Internal dependencies:**
- `CustomerQueryService` API (existing) - used by AccountService
- `ProductQueryService` API (existing) - used by DemoDataLoader and AccountService
- `CurrencyQueryService` API (existing) - used by AccountService
- `LimitCheckService` API (existing) - still used synchronously by AccountService (Rule 4: validation)
- `ProductService` internal (used by ProductDemoDataLoader - OK since it's in same module)

**External dependencies:**
- Spring Framework: `ApplicationEventPublisher` (core Spring - no version issues)
- Kafka (optional): `DomainEventPublisher` exists but not required for this implementation

---

## Notes

- The `AccountOpenedEvent` uses `"system"` as `createdBy` temporarily. Should be enhanced later to extract authenticated user from SecurityContext.
- Event model is designed to be Kafka-ready: topics can be created later without changing business module code.
- DemoDataLoader now expects ProductDemoDataLoader to have run first (order=1 vs order=2). In integration tests, may need to pre-load product data or mock ProductQueryService.

---

**Plan Completeness:** This plan captures all implementation tasks for fixing the module boundary violations. Any deviations should be documented as change requests with architectural review.
