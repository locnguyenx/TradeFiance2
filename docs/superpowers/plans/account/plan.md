# Account Management Module - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the Account Management module for corporate and SME banking, supporting account opening, holder management, balance inquiries, and statements.

**Architecture:** Backend-first approach using Spring Boot modular monolith. Customer module integration via shared database with proper module partitioning.

**Tech Stack:** Java 17, Spring Boot 3.2, PostgreSQL, JPA, Gradle, React 18 + TypeScript frontend

---

### Task 1: Project Setup

**Estimated steps:** 4

- [ ] 1.1 Create `build.gradle.kts` with Spring Boot 3.2, Java 17
  - Use same structure as customer module
  - Include dependencies: spring-boot-starter-data-jpa, spring-boot-starter-web, flyway, h2 (test), lombok
- [ ] 1.2 Create `settings.gradle.kts` with project name: `account-module`
- [ ] 1.3 Create `src/main/resources/application.yml` with H2 config (for development) and PostgreSQL config (commented)
- [ ] 1.4 Create `AccountModuleApplication.java` with `@SpringBootApplication`

**Verification:** `./gradlew build` compiles without errors

---

### Task 2: Account Domain Enums

**Estimated steps:** 5

- [ ] 2.1 Create `AccountType.java` enum (CURRENT, SAVINGS, FIXED_DEPOSIT, LOAN, ESCROW)
- [ ] 2.2 Create `AccountStatus.java` enum (PENDING, ACTIVE, DORMANT, FROZEN, CLOSED)
- [ ] 2.3 Create `Currency.java` enum (USD, EUR, GBP, SGD, JPY, CAD, AUD, CHF)
- [ ] 2.4 Create `AccountHolderRole.java` enum (PRIMARY, JOINT, AUTHORIZED_SIGNATORY, NOMINEE)
- [ ] 2.5 Create `AccountHolderStatus.java` enum (ACTIVE, INACTIVE, REMOVED)

**Verification:** All enums compile; no tests required

---

### Task 3: Account Embeddables

**Estimated steps:** 3

- [ ] 3.1 Create `AccountBalance.java` embeddable with fields: availableBalance (BigDecimal), ledgerBalance (BigDecimal), holdAmount (BigDecimal), currency (Currency enum)
- [ ] 3.2 Add proper JPA annotations (`@Embeddable`, `@Embedded`)
- [ ] 3.3 Create unit tests for AccountBalance (getters/setters, equals/hashCode)

**Verification:** All embeddables compile; unit tests pass

---

### Task 4: Account Entity

**Estimated steps:** 6

- [ ] 4.1 Create `Account.java` abstract entity with:
  - id (auto-increment), accountNumber (unique), type, status, currency
  - balance (BigDecimal), customer (ManyToOne), productId (Long - reference to Product module)
  - openedAt (LocalDateTime), closedAt (LocalDateTime nullable)
  - AuditFields embedded, optimistic locking with @Version
  - JPA inheritance: `@Inheritance(strategy = InheritanceType.JOINED)`, `@DiscriminatorColumn`
- [ ] 4.2 Create `CurrentAccount.java` concrete entity (extends Account)
  - Add overdraftLimit (BigDecimal), interestRate (BigDecimal)
- [ ] 4.3 Create `SavingsAccount.java` concrete entity (extends Account)
  - Add minimumBalance (BigDecimal), interestRate (BigDecimal), lastInterestPosted (LocalDate)
- [ ] 4.4 Create `FixedDepositAccount.java` concrete entity (extends Account)
  - Add depositTerm (Integer, months), maturityDate (LocalDate), maturityAmount (BigDecimal)
- [ ] 4.5 Create `LoanAccount.java` concrete entity (extends Account)
  - Add loanAmount (BigDecimal), interestRate (BigDecimal), term (Integer, months), outstandingBalance (BigDecimal), nextPaymentDate (LocalDate)
- [ ] 4.6 Create `AccountRepository.java` extending JpaRepository<Account, Long> with methods:
  - `Optional<Account> findByAccountNumber(String accountNumber)`
  - `List<Account> findByCustomer_Id(Long customerId)`
  - `List<Account> findByStatus(AccountStatus status)`

**Verification:** Entities compile; repository integration tests pass

---

### Task 5: Account Holder Entity

**Estimated steps:** 4

- [ ] 5.1 Create `AccountHolder.java` entity with:
  - id, account (ManyToOne), customer (ManyToOne)
  - role (AccountHolderRole), status (AccountHolderStatus)
  - effectiveFrom (LocalDate), effectiveTo (LocalDate nullable)
  - AuditFields embedded
- [ ] 5.2 Create `AccountHolderRepository.java` extending JpaRepository<AccountHolder, Long> with methods:
  - `List<AccountHolder> findByAccount_Id(Long accountId)`
  - `List<AccountHolder> findByCustomer_Id(Long customerId)`
  - `Optional<AccountHolder> findByAccount_IdAndCustomer_IdAndStatus(Long accountId, Long customerId, AccountHolderStatus status)`
- [ ] 5.3 Add JPA constraints and indexes for performance
- [ ] 5.4 Write repository integration tests

**Verification:** Repository tests pass

---

### Task 6: Account Number Generator

**Estimated steps:** 3

- [ ] 6.1 Create `AccountNumberGenerator.java` service
  - Generate format: ACC-YYYYMMDD-XXXXXX (e.g., ACC-20240115-300001)
  - Use timestamp-based approach with counter for uniqueness
- [ ] 6.2 Add unit tests for AccountNumberGenerator (test format, uniqueness, daily resets)
- [ ] 6.3 Ensure thread-safe implementation

**Verification:** Unit tests pass (≥ 90% coverage)

---

### Task 7: Account Service

**Estimated steps:** 8

- [ ] 7.1 Create `AccountService.java` with methods:
  - `openAccount(AccountOpeningRequest request)` - create new account with holders
  - `closeAccount(String accountNumber)` - close account with validation
  - `freezeAccount(String accountNumber)` - freeze account
  - `unfreezeAccount(String accountNumber)` - unfreeze account
  - `addAccountHolder(String accountNumber, AccountHolderRequest request)`
  - `removeAccountHolder(Long holderId)` - soft delete (set status REMOVED)
  - `updateAccountHolderRole(Long holderId, AccountHolderRole newRole)`
- [ ] 7.2 Create `AccountOpeningRequest.java` DTO with fields:
  - customerId (Long), productCode (String), type (AccountType), currency (Currency)
  - initialDeposit (BigDecimal), holders (List<AccountHolderRequest>)
- [ ] 7.3 Create `AccountHolderRequest.java` DTO with fields:
  - customerId (Long), role (AccountHolderRole)
- [ ] 7.4 Implement validation: customer exists, product exists (mock for now), initial deposit meets minimum requirement
- [ ] 7.5 Create `DuplicateAccountException.java` and `InvalidAccountStateException.java`
- [ ] 7.6 Create `AccountMapper.java` to convert entities to `AccountResponse.java` DTO
- [ ] 7.7 Write unit tests for AccountService (≥80% coverage)
  - Test happy paths: open account, close account, add holder
  - Test validation: duplicate account number, invalid customer, insufficient deposit
  - Test state transitions: cannot close frozen account, etc.
- [ ] 7.8 Write integration tests with test database

**Verification:** All unit tests pass; integration tests pass

---

### Task 8: Account Inquiry Service

**Estimated steps:** 5

- [ ] 8.1 Create `AccountInquiryService.java` with methods:
  - `getAccountDetails(String accountNumber)` - returns full account with holders
  - `getAccountBalance(String accountNumber)` - returns AccountBalance
  - `getAccountStatement(String accountNumber, StatementFilter filter, Pageable pageable)` - returns Page<AccountStatement>
- [ ] 8.2 Create `AccountStatementFilter.java` DTO with fields:
  - fromDate (LocalDate), toDate (LocalDate), transactionType (String nullable)
- [ ] 8.3 Create `AccountStatement.java` DTO (for now, empty transactions list)
- [ ] 8.4 Implement pagination for statements
- [ ] 8.5 Write unit tests for AccountInquiryService

**Verification:** All unit tests pass

---

### Task 9: Account REST API

**Estimated steps:** 7

- [ ] 9.1 Create `AccountController.java` with endpoints:
  - `POST /api/accounts` - open account (use @Valid)
  - `GET /api/accounts/{accountNumber}` - get details
  - `GET /api/accounts/{accountNumber}/balance` - get balance
  - `GET /api/accounts/{accountNumber}/statement` - get statement with optional fromDate, toDate params
  - `PUT /api/accounts/{accountNumber}/close` - close account
  - `PUT /api/accounts/{accountNumber}/freeze` - freeze account
  - `PUT /api/accounts/{accountNumber}/unfreeze` - unfreeze account
- [ ] 9.2 Create `AccountHolderController.java` with endpoints:
  - `POST /api/accounts/{accountNumber}/holders` - add holder
  - `GET /api/accounts/{accountNumber}/holders` - get all holders
  - `DELETE /api/accounts/{accountNumber}/holders/{holderId}` - remove holder
- [ ] 9.3 Create `AccountExceptionHandler.java` (or use GlobalExceptionHandler from customer module by updating CustomerModuleApplication to scan account exceptions)
- [ ] 9.4 Add request validation with Jakarta Bean Validation (`@NotNull`, `@Positive`, etc.)
- [ ] 9.5 Write controller integration tests using MockMvc
  - Use `@SpringBootTest(classes = AccountModuleApplication.class)`
  - Mock service layers
  - Test all endpoints with various scenarios
- [ ] 9.6 Create `AccountNotFoundException.java`, `InvalidAccountStateException.java` with error codes (ACCT-002, ACCT-003)

**Verification:** All controller tests pass; API contracts validated

---

### Task 10: Cross-Module Integration

**Estimated steps:** 3

- [ ] 10.1 Update `CustomerModuleApplication.java` to scan account module packages:
  - Add `@EnableJpaRepositories(basePackages = {"com.banking.customer.repository", "com.banking.account.repository"})`
  - Add `@ComponentScan(basePackages = {"com.banking.customer", "com.banking.account"})`
  - Add `@EntityScan(basePackages = {"com.banking.customer.domain", "com.banking.account.domain"})`
- [ ] 10.2 Create `CustomerAccountService.java` in account module:
  - `getCustomerAccounts(Long customerId)` - returns all accounts for customer
  - `getCustomerAccountSummary(Long customerId)` - returns aggregated balance info
- [ ] 10.3 Add integration tests verifying customer module can access account data

**Verification:** All modules load together in test context; integration tests pass

---

### Task 11: Database Migrations

**Estimated steps:** 3

- [ ] 11.1 Create Flyway migration `V1__create_account_schema.sql` with tables:
  - `accounts` (with discriminator column)
  - `current_account`, `savings_account`, `fixed_deposit_account`, `loan_account` (subclass tables)
  - `account_holders`
  - Add proper foreign keys, indexes, constraints
- [ ] 11.2 Create `V2__add_account_balance_constraints.sql` for check constraints
- [ ] 11.3 Test migrations with `./gradlew bootRun` and verify tables created

**Verification:** Flyway migrations run successfully on H2; backend starts without errors

---

### Task 12: Frontend - Account Pages

**Estimated steps:** 6

- [ ] 12.1 Create `AccountListPage.tsx` with:
  - Table showing accounts (accountNumber, type, status, balance, customer)
  - Search by account number or customer name
  - Filter by account type and status
  - Pagination
  - Link to account details page
- [ ] 12.2 Create `AccountDetailPage.tsx` with:
  - Tabs: Account Info, Account Holders, Transactions (placeholder)
  - Account info: balance, status, product, opened date
  - Holders section with add/remove buttons
  - Action buttons: Freeze, Unfreeze, Close
- [ ] 12.3 Create `AccountOpeningForm.tsx` for creating new accounts with fields:
  - Customer selector (search from customer module)
  - Product selection (dropdown - currently static)
  - Account type, currency, initial deposit
  - Account holders multi-select
- [ ] 12.4 Update account Redux slice with async thunks:
  - `fetchAccounts`, `fetchAccountById`, `openAccount`, `closeAccount`, `freezeAccount`, `unfreezeAccount`
  - `fetchAccountHolders`, `addAccountHolder`, `removeAccountHolder`
- [ ] 12.5 Add routes in `App.tsx`: `/accounts`, `/accounts/:id`, `/accounts/new`
- [ ] 12.6 Write component tests for all pages

**Verification:** Frontend builds without errors; component tests pass

---

### Task 13: Frontend - Account Statement Page

**Estimated steps:** 4

- [ ] 13.1 Create `AccountStatementPage.tsx` with:
  - Date range picker (fromDate, toDate)
  - Table showing transactions (placeholder data for now)
  - Export buttons (CSV, Excel) - implement CSV export
  - Summary: opening balance, closing balance, total credits, total debits
- [ ] 13.2 Add statement fetching to Redux slice
- [ ] 13.3 Implement CSV export using file-saver library
- [ ] 13.4 Write component tests

**Verification:** Page renders; CSV export works; tests pass

---

### Task 14: Integration Testing

**Estimated steps:** 3

- [ ] 14.1 Write integration test: `AccountCustomerIntegrationTest.java`
  - Verify account opening links to existing customer
  - Verify customer's accounts can be queried
- [ ] 14.2 Write integration test: `AccountProductIntegrationTest.java` (product module mock)
  - Verify account respects product configuration (minimum deposit, currency)
- [ ] 14.3 Run full integration test suite with both modules loaded

**Verification:** All integration tests pass

---

### Task 15: Performance & Optimization

**Estimated steps:** 3

- [ ] 15.1 Add database indexes:
  - `idx_accounts_account_number` (unique)
  - `idx_accounts_customer_id`
  - `idx_accounts_status`
  - `idx_account_holders_account_id`
  - `idx_account_holders_customer_id`
- [ ] 15.2 Implement caching for frequently accessed accounts:
  - Use Spring Cache with Caffeine or Redis (configure in application.yml)
  - Cache account details and balances
- [ ] 15.3 Optimize N+1 queries in account holder fetching

**Verification:** Performance tests show < 100ms for account lookups; indexes created via Flyway

---

### Task 16: Demo Data & Documentation

**Estimated steps:** 3

- [ ] 16.1 Create `DemoDataLoader.java` (CommandLineRunner) to populate:
  - Sample accounts for testing/demo purposes
  - Sample account holders
  - Sample transactions (placeholder)
- [ ] 16.2 Add API documentation: update `docs/api/accounts-api.md` with endpoints, request/response examples
- [ ] 16.3 Update `README.md` with account module features, how to run, API examples

**Verification:** Demo data loads on startup; API docs complete

---

### Definition of Done

1. All 16 tasks completed with all steps checked off
2. Backend: `./gradlew build` passes with all tests green (≥80% coverage)
3. Frontend: `npm run build` passes with no TypeScript errors
4. API contracts match specification; Postman collection updated
5. Database migrations complete and tested
6. Integration tests verify customer-account linking
7. Documentation complete (API docs, README updates)
8. All code committed to `feature/phase2-core-banking` branch
9. Demo data available for manual testing
10. No linting errors (backend or frontend)

---

## Notes

- **Module Independence:** Account module should be testable independently, but will integrate with customer module via shared database
- **ID Strategy:** Auto-increment BIGINT for all entities
- **Account Numbers:** Use ACC-YYYYMMDD-XXXXXX format; ensure uniqueness
- **Error Codes:** Use ACCT-XXX prefix
- **Events:** Publish `AccountOpenedEvent`, `AccountClosedEvent`, `AccountFrozenEvent` for future integration
- **Product References:** For now, productId is a Long reference; Product module will provide product details later
- **Transactions:** Transaction module not implemented yet; statements will show placeholder data

---

## Troubleshooting

**Customer module tests failing in phase2 worktree:**
- Ensure `CustomerModuleApplication.java` does NOT scan account module packages
- Account module tests should use `AccountModuleApplication.java` (create a separate test configuration if needed)
- Keep module boundaries clean: customer tests only load customer beans

**Frontend API calls 404:**
- Verify backend is running on port 8080
- Check Vite proxy configuration (`vite.config.ts`) points to correct backend URL
- Ensure CORS is configured in backend to allow frontend origin

**Account number duplication:**
- AccountNumberGenerator must be thread-safe
- Consider using database sequence if concurrency issues arise
