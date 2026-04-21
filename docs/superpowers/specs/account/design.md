# Account Management Module Design

**Created:** 2026-03-19  
**Status:** Draft  
**Module:** Account Management  

## Overview

The Account Management module handles the creation, maintenance, and lifecycle of bank accounts for corporate and SME customers. It supports various account types including current accounts, savings accounts, fixed deposits, and loan accounts. The module provides comprehensive account holder management, including joint accounts and authorized signatories.

## Architecture

### Module Boundaries
- **Domain:** Core account entities and business logic
- **Application Services:** Account operations, holder management, inquiry services
- **Infrastructure:** Data persistence (JPA repositories), external integrations
- **Interface:** RESTful API for account operations

### Key Components

1. **Account Entity Hierarchy**
   - Abstract `Account` base class with common fields
   - Concrete implementations: `CurrentAccount`, `SavingsAccount`, `FixedDepositAccount`, `LoanAccount`
   - Uses JOINED inheritance strategy for flexible querying

2. **Account Holder Management**
   - `AccountHolder` entity linking customers to accounts with roles
   - Support for multiple holders per account with different permissions
   - Role-based access: PRIMARY, JOINT, AUTHORIZED_SIGNATORY, NOMINEE

3. **Account Balance & Financials**
   - `AccountBalance` embeddable tracking available, ledger, and hold amounts
   - Currency support with proper money handling (BigInteger for minor units)
   - Interest calculation hooks for savings and fixed deposit accounts

4. **Account Lifecycle**
   - Account opening with initial deposit validation
   - Status transitions: PENDING → ACTIVE → DORMANT/FROZEN → CLOSED
   - Freeze/unfreeze functionality for regulatory compliance
   - Closure with balance settlement procedures

5. **Account Inquiry & Statements**
   - Balance inquiries (available vs ledger)
   - Transaction history with filtering and pagination
   - Statement generation in multiple formats (PDF, CSV, Excel)
   - Date-range filtering for statement periods

### Module Dependency Rules

```
Account Management Module
    ├── Depends On: Master Data (currencies), Customer, Product, Limits, Charges
    └── Used By: Transaction, Cash Management, Trade Finance, Payments
```
Refer to section "Integration Points" for more details

## Data Model

### Core Entities

```
Account (abstract)
├── id (PK, BIGINT auto-increment)
├── accountNumber (UNIQUE, VARCHAR)
├── type (AccountType enum)
├── status (AccountStatus enum)
├── currency (Currency enum)
├── balance (BigDecimal - current balance)
├── customer (FK to Customer)
├── productId (FK to Product)
├── openedAt (Timestamp)
├── closedAt (Timestamp, nullable)
└── auditFields (createdAt, createdBy, updatedAt, updatedBy)

AccountBalance (embeddable)
├── availableBalance (BigDecimal)
├── ledgerBalance (BigDecimal)
├── holdAmount (BigDecimal)
├── currency (Currency enum)

AccountHolder
├── id (PK, BIGINT auto-increment)
├── account (FK to Account)
├── customer (FK to Customer)
├── role (AccountHolderRole enum)
├── effectiveFrom (Date)
├── effectiveTo (Date, nullable)
├── status (AccountHolderStatus enum)
└── auditFields

AccountType: CURRENT, SAVINGS, FIXED_DEPOSIT, LOAN, ESCROW
AccountStatus: PENDING, ACTIVE, DORMANT, FROZEN, CLOSED
AccountHolderRole: PRIMARY, JOINT, AUTHORIZED_SIGNATORY, NOMINEE
AccountHolderStatus: ACTIVE, INACTIVE, REMOVED
Currency: USD, EUR, GBP, SGD, JPY, CAD, AUD, CHF
```

### Relationships
- Customer 1:N Account (via AccountHolder)
- Account 1:N AccountHolder
- Product 1:N Account (reference only in Phase 2)
- Account 1:1 AccountBalance (embedded)

## Key Features

### Account Operations
- Open new account with initial deposit validation
- Close account with balance settlement
- Freeze/unfreeze account for regulatory holds
- Reactivate dormant accounts
- Update account information (contact details, preferences)

### Account Holder Management
- Add/remove account holders with role assignment
- Update holder information and effective dates
- Transfer primary holder status
- Holder status tracking (active/inactive/removed)

### Inquiry & Reporting
- Real-time balance inquiries (available vs ledger)
- Account details retrieval (type, status, customer info)
- Transaction history with filtering (date, type, amount)
- Statement generation (PDF, CSV, Excel formats)
- Interest calculation and accrual tracking

### Validation & Business Rules
- Initial deposit meets product minimum requirements
- Customer eligibility for account type (via Product module)
- Unique account number generation (ACC-YYYYMMDD-XXXXXX)
- Holder role validation (only one PRIMARY per account)
- Status transition validation (e.g., cannot freeze closed account)

## API Design

### Account Endpoints
```
POST   /api/accounts                          # Open new account
GET    /api/accounts/{accountNumber}          # Get account details
GET    /api/accounts/{accountNumber}/balance  # Get account balance
GET    /api/accounts/{accountNumber}/statement # Get account statement
PUT    /api/accounts/{accountNumber}/close    # Close account
PUT    /api/accounts/{accountNumber}/freeze   # Freeze account
PUT    /api/accounts/{accountNumber}/unfreeze # Unfreeze account
```

### Account Holder Endpoints
```
POST   /api/accounts/{accountNumber}/holders          # Add account holder
GET    /api/accounts/{accountNumber}/holders          # Get all holders
PUT    /api/accounts/{accountNumber}/holders/{holderId} # Update holder
DELETE /api/accounts/{accountNumber}/holders/{holderId} # Remove holder
```

### Response Formats
All responses follow standard structure:
```json
{
  "success": true,
  "data": { /* resource data */ },
  "errorCode": null,
  "message": null,
  "timestamp": "ISO 8601 datetime"
}
```

Error responses include appropriate HTTP status codes and error codes (ACCT-XXX prefix).

## Integration Points

### Architectural Compliance

The Account module strictly follows the **Module Boundary Rules** defined in `AGENTS.md`:

| Rule | Compliance in this Module |
|------|---------------------------|
| **Rule 1: No domain entity sharing** | ✅ Only imports DTOs from other modules (`CustomerDTO`, `ProductVersionDTO`, `CurrencyDTO`) |
| **Rule 2: Only api/dto packages public** | ✅ All cross-module imports are from `<module>.api` packages only |
| **Rule 3: JPA relationships within module** | ✅ Account entities only reference other Account entities; cross-module refs use `Long id` |
| **Rule 4: Async for side-effects** | ✅ Limit assignment is async via `AccountOpenedEvent`; validation (limit check) is sync |

**Reference:** See `AGENTS.md` section "## Architecture Enforcement" for complete rules.

### Customer Module
- **Dependency:** Uses `CustomerQueryService` API (read-only)
- **Purpose:** Validates customer exists, retrieves customer details for account holders
- **Architectural Compliance:** ✅ Only depends on `com.banking.customer.api` package (Rule 2)
- **Rationale:** Maintains loose coupling; customer internal implementation can change without affecting account module

### Product Module
- **Dependency:** Uses `ProductQueryService` API (read-only)
- **Purpose:** Validates product code, retrieves product version details, applies product-specific rules
- **Architectural Compliance:** ✅ Only depends on `com.banking.product.api` package
- **Rationale:** Product module owns product data; account module queries through published interface

### Limits Module
- **Dependency:** Event-driven via `AccountOpenedEvent` publication
- **Purpose:** Assigns default product limits to new accounts (e.g., transaction limits, daily withdrawal limits)
- **Architectural Compliance:** ✅ No direct dependency; uses Spring ApplicationEventPublisher
- **Rationale:** 
  - **Loose Coupling:** Limits module subscribes to events; account module doesn't know consumers exist
  - **Resilience:** Limit assignment failures don't block account creation (fire-and-forget)
  - **Scalability:** Can be distributed to separate JVMs via Kafka (future)
  - **Change Isolation:** Limits module can change assignment logic without account module changes
- **Implementation:** See "Event-Driven Architecture" section below

### Master Data Module
- **Dependency:** Uses `CurrencyQueryService` API (read-only)
- **Purpose:** Validates currency code and active status during account opening
- **Architectural Compliance:** ✅ Only depends on `com.banking.masterdata.api` package
- **Rationale:** Ensures only active currencies are used; master data module owns currency definitions

### Transaction Module (Future)
- Will consume `AccountCreatedEvent` for account provisioning in transaction ledger
- Will publish `TransactionPostedEvent` for balance updates
- Expected to use async event-driven integration

### Notification/Event System
- Publishes domain events for cross-module communication:
  - `AccountOpenedEvent` → triggers limit assignment, notifications, audit logging
  - `AccountClosedEvent` → triggers cleanup, notifications
  - `AccountFrozenEvent` → triggers compliance actions
- Uses Spring's `ApplicationEventPublisher` (in-process) with optional Kafka bridge
- All events contain minimal data needed for processing (IDs, codes, timestamps)

## Event-Driven Architecture

### Why Events?
The Account module uses an event-driven architecture for cross-module integration to achieve:
1. **Loose Coupling:** Modules don't call each other directly; they publish/subscribe to events
2. **Resilience:** Event consumers can fail without affecting the primary transaction
3. **Scalability:** Events can be distributed via Kafka for async processing across JVMs
4. **Change Isolation:** Adding/removing consumers doesn't require changes to the publisher

### Event Publishing Pattern
```java
// In AccountService.openAccount()
Account account = accountRepository.save(account);

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
    "system" // from security context
);
eventPublisher.publishEvent(event);
```

### Event Consumption Pattern in Other Modules
```java
@Component
public class AccountOpenedEventListener {
    
    @EventListener
    public void handleAccountOpened(AccountOpenedEvent event) {
        // Perform side effects (e.g., assign limits, send notifications)
        // Errors are logged but don't propagate back to publisher
    }
}
```

### Current Event Types
| Event | Purpose | Consumers |
|-------|---------|-----------|
| `AccountOpenedEvent` | Notify that account is created | Limits module (assign product limits), Notification module (welcome email), Audit module |
| `AccountClosedEvent` | Notify that account is closed | Compliance module, Notification module |
| `AccountFrozenEvent` | Notify account freeze | Risk module, Compliance module |

### Kafka Readiness
- Events extend Spring's `ApplicationEvent` (in-process)
- Can be bridged to Kafka by adding `@KafkaListener` and `KafkaTemplate` without changing publishers
- Future: Publish directly to Kafka using `DomainEventPublisher` for distributed consumers

---

## Security & Authorization

### Access Controls
- Account holders can only access their own accounts
- Joint holders have equal access (subject to bank policies)
- Authorized signatories have limited transaction rights
- Nominees have view-only access (typically for inheritance)

### Validation & Sanitization
- Input validation for all API requests
- SQL injection prevention via JPA/Criteria API
- XSS prevention in response rendering
- Rate limiting on sensitive operations (freeze/unfreeze)

### Audit Trail
- All account operations logged with user context
- Holder additions/removals tracked
- Status change history maintained
- Regulatory reporting data preserved

## Technology Implementation

### Backend (Java/Spring Boot)
- Java 17, Spring Boot 3.2
- Spring Data JPA for data access
- Hibernate as JPA provider
- Jakarta Validation for request validation
- Spring Transaction management
- Spring Events for domain events
- SLF4J with Logback for logging

### Database (PostgreSQL)
- Proper indexing for query performance
- Foreign key constraints for referential integrity
- Check constraints for enum validation
- Partitioning strategy for large tables (considered for future)

### Testing Strategy

#### Standards

Follows [Test Strategy Template](../templates/test-strategy.md) for:
- Merge failure prevention (stale cache, duplicate classes, Gradle syntax, mock conventions)
- Test conventions (error code constants, HTTP status verification, mock patterns, DTO types)
- Pre-merge checklist
- CI pipeline requirements
- Test data isolation
- Coverage requirements

#### Module-Specific Exception Constants

Each exception class must define `ERROR_CODE` as a public constant:

| Exception | Error Code |
|-----------|-----------|
| `AccountNotFoundException` | ACCT-001 |
| `InvalidAccountStateException` | ACCT-002 |
| `DuplicateAccountException` | ACCT-003 |

#### Module-Specific Tests

- Unit tests for service layer (80%+ coverage)
- Integration tests for repositories
- Contract tests for REST APIs
- Test data builders for consistent test setup
- Performance tests for critical operations

## Non-Functional Requirements

### Performance
- Account lookup: < 100ms
- Balance inquiry: < 50ms
- Statement generation: < 2s for 1-year history
- Concurrent users: 1000+ simultaneous sessions

### Scalability
- Horizontal scaling via stateless services
- Database read replicas for inquiry-heavy workloads
- Caching layer for frequently accessed reference data
- Asynchronous processing for non-critical operations

### Reliability
- Automated failover for database connections
- Circuit breaker pattern for external dependencies
- Graceful degradation for non-core features
- Comprehensive health checks and monitoring

### Compliance
- GDPR/CCPA data privacy controls
- SOX financial reporting controls
- Basel III/IV capital adequacy tracking
- AML/KYC integration points
- Audit trail retention (7+ years)

## Implementation Notes

### Assumptions
- Product module provides basic product configurations
- Customer module handles customer KYC and onboarding
- Transaction module will be implemented in subsequent phases
- Initial implementation focuses on core account functionality

### Future Enhancements
- Advanced interest calculation algorithms
- Fee scheduling and automation
- Account closure workflows with checks
- Dormant account escheatment procedures
- Multi-currency account support
- Sweep account functionality
- Overdraft protection and lines of credit

---

## Implementation Guardrails

**For Future Developers:**
When adding new features to the Account module, always verify compliance with the Module Boundary Rules (AGENTS.md) and the Communication Pattern Guidance (System Design, Section 7.1).

### Communication Patterns Reference

The Account module uses the following communication patterns as defined in `docs/superpowers/architecture/system-design.md` Section 7.1:

| Pattern | Use Case in Account Module | Example |
|---------|---------------------------|---------|
| **Direct Interface Call** | Synchronous validation that affects immediate response | `limitCheckService.checkLimit()` before account opening |
| **Event Publishing** | Asynchronous side-effects (fire-and-forget) | `AccountOpenedEvent` for limit assignment |

**Key Principle (from System Design):**
- Use **Direct Interface Call** for: Validation that needs immediate result for response
- Use **Event Publishing** for: Side-effects that can happen asynchronously

See System Design Section 7.1 "Communication Pattern Guidance" for complete patterns.

### Cross-Module Integration Checklist

Before implementing cross-module integration:

1. **Check for existing API** in the target module (`<module>.api` package)
2. **If API doesn't exist**, request the target module team to create one (do NOT import internal packages)
3. **Use events for side-effects** (notifications, cache updates, async processing)
4. **Use direct API calls for validation** that affects the immediate response
5. **Never import** from: `<module>.domain.entity`, `<module>.repository`, `<module>.service`, `<module>.config`

**Example - Adding New Integration:**
```java
// ✅ CORRECT: Using Master Data API
@Autowired
private CurrencyQueryService currencyQueryService;

CurrencyDTO currency = currencyQueryService.findByCode("USD");

// ❌ WRONG: Direct repository access
@Autowired
private CurrencyRepository currencyRepository; // Violates Rule 2!
```

**Code Review Checklist:**
- [ ] All cross-module imports are from `.api` or `.dto` packages only
- [ ] No JPA relationships to entities in other modules
- [ ] Side-effects use events (unless synchronous validation required)
- [ ] New dependencies don't create circular module dependencies
- [ ] Communication pattern matches System Design Section 7.1 guidance

See `AGENTS.md` for the complete architecture enforcement rules.
See System Design Section 7.1 for communication pattern guidance.

### Dependencies
- Customer module (for customer data and validation)
- Product module (for account product references)
- Shared kernel (for common enums, exceptions, utilities)