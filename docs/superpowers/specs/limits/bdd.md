# BDD Specification: Limits Management

**Version:** 1.0  
**Date:** 2026-03-21  
**Status:** Draft  
**Module:** Limits Management (`com.banking.limits`)  
**BRD Reference:** `docs/superpowers/brds/limits/brd.md`

Each BDD scenario is tagged with exactly one `@US` (User Story) and one `@FR` (Functional Requirement) for atomic traceability.

**Scenario Types:**
- **Happy Path** — Standard successful flow
- **Edge Case** — Boundary, override, or unusual valid conditions
- **Negative** — Expected rejection or error

---

## 1. Limit Definition

```gherkin
Feature: Limit Definition

  @US-1 @FR-1.1
  Scenario: Create a daily limit (Happy Path)
    When Admin creates a limit with name "Daily Transfer Limit", type "DAILY", amount 100000, currency "USD"
    Then the limit is created with status "ACTIVE"

  @US-1 @FR-1.2
  Scenario: Create a per-transaction limit (Happy Path)
    When Admin creates a limit with name "Per Transaction Limit", type "PER_TRANSACTION", amount 50000, currency "USD"
    Then the limit is created with status "ACTIVE"

  @US-1 @FR-1.2
  Scenario: Invalid limit type is rejected (Negative)
    When Admin creates a limit with type "HOURLY"
    Then the creation is rejected with error "Invalid limit type"

  @US-6 @FR-1.3
  Scenario: Deactivate a limit (Happy Path)
    Given a limit "Daily Transfer Limit" is ACTIVE
    When Admin deactivates the limit
    Then the limit status is "INACTIVE"

  @US-6 @FR-1.3
  Scenario: Activate a limit (Happy Path)
    Given a limit "Daily Transfer Limit" is INACTIVE
    When Admin activates the limit
    Then the limit status is "ACTIVE"
```

---

## 2. Product-Level Limits

```gherkin
Feature: Product-Level Limit Assignment

  @US-2 @FR-2.1
  Scenario: Assign limit to a product (Happy Path)
    Given a limit "Daily Transfer Limit" exists with amount 100000 USD
    And product "SAVINGS" exists
    When Admin assigns the limit to product "SAVINGS"
    Then product "SAVINGS" has limit "Daily Transfer Limit" of 100000 USD

  @US-2 @FR-2.2
  Scenario: Multiple limits on same product (Edge Case)
    Given limits "Daily Transfer Limit" and "Monthly Transfer Limit" exist
    And product "SAVINGS" exists
    When Admin assigns both limits to product "SAVINGS"
    Then product "SAVINGS" has 2 limits
```

---

## 3. Customer-Level Limits

```gherkin
Feature: Customer-Level Limit Assignment

  @US-3 @FR-3.1
  Scenario: Assign limit to a customer (Happy Path)
    Given a limit "Daily Transfer Limit" exists with amount 200000 USD
    And customer "CORP-001" exists
    When Admin assigns the limit to customer "CORP-001"
    Then customer "CORP-001" has limit "Daily Transfer Limit" of 200000 USD

  @US-3 @FR-3.1
  Scenario: Customer-level limit overrides product-level (Edge Case)
    Given product "SAVINGS" has "Daily Transfer Limit" of 100000 USD
    And customer "CORP-001" has "Daily Transfer Limit" of 200000 USD
    When the system resolves limits for customer "CORP-001" on product "SAVINGS"
    Then the effective daily limit is 200000 USD
```

---

## 4. Account-Level Limits

```gherkin
Feature: Account-Level Limit Assignment

  @US-4 @FR-4.1
  Scenario: Assign limit to an account (Happy Path)
    Given a limit "Daily Transfer Limit" exists with amount 150000 USD
    And account "ACC-001" exists
    When Admin assigns the limit to account "ACC-001"
    Then account "ACC-001" has limit "Daily Transfer Limit" of 150000 USD

  @US-4 @FR-4.1
  Scenario: Account-level limit overrides customer and product (Edge Case)
    Given product "SAVINGS" has "Daily Transfer Limit" of 100000 USD
    And customer "CORP-001" has "Daily Transfer Limit" of 200000 USD
    And account "ACC-001" has "Daily Transfer Limit" of 150000 USD
    When the system resolves limits for account "ACC-001"
    Then the effective daily limit is 150000 USD
```

---

## 5. Limit Enforcement

```gherkin
Feature: Limit Enforcement

  @US-7 @FR-6.1
  Scenario: Transaction within limit is allowed (Happy Path)
    Given account "ACC-001" has daily limit 100000 USD
    And cumulative daily usage is 50000 USD
    When Payment module checks a transaction of 30000 USD
    Then the result is "ALLOWED"

  @US-7 @FR-6.1
  Scenario: Transaction exceeding limit is rejected (Negative)
    Given account "ACC-001" has daily limit 100000 USD
    And cumulative daily usage is 80000 USD
    When Payment module checks a transaction of 30000 USD
    Then the result is "REJECTED" with error "Daily limit exceeded"

  @US-7 @FR-6.1
  Scenario: Per-transaction limit is enforced (Negative)
    Given account "ACC-001" has per-transaction limit 50000 USD
    When Payment module checks a transaction of 60000 USD
    Then the result is "REJECTED" with error "Per-transaction limit exceeded"

  @US-9 @FR-6.2
  Scenario: Usage is aggregated within period (Happy Path)
    Given account "ACC-001" has daily limit 100000 USD
    And cumulative daily usage is 40000 USD
    When a transaction of 20000 USD is processed
    Then the cumulative daily usage becomes 60000 USD

  @US-10 @FR-6.3
  Scenario: Usage resets when period expires (Edge Case)
    Given account "ACC-001" has daily limit 100000 USD
    And cumulative daily usage was 90000 USD on 2026-03-20
    When the system processes a transaction on 2026-03-21
    Then the cumulative daily usage resets to 0

  @US-7 @FR-6.1
  Scenario: Transaction exactly at limit boundary is allowed (Edge Case)
    Given account "ACC-001" has daily limit 100000 USD
    And cumulative daily usage is 90000 USD
    When Payment module checks a transaction of 10000 USD
    Then the result is "ALLOWED"

  @US-7 @FR-6.1
  Scenario: Transaction exactly over limit boundary is rejected (Edge Case)
    Given account "ACC-001" has daily limit 100000 USD
    And cumulative daily usage is 90000 USD
    When Payment module checks a transaction of 10001 USD
    Then the result is "REJECTED" with error "Daily limit exceeded"

  @US-7 @FR-6.1
  Scenario: No applicable limit allows all transactions (Edge Case)
    Given no limits are configured for account "ACC-001"
    When Payment module checks a transaction of 999999999 USD
    Then the result is "ALLOWED"
```

---

## 6. Approval Thresholds

```gherkin
Feature: Approval Thresholds

  @US-5 @FR-5.1
  Scenario: Configure absolute approval threshold (Happy Path)
    Given a limit "Daily Transfer Limit" exists with amount 100000 USD
    When Admin sets approval threshold at 50000 USD
    Then transactions > 50000 USD require approval

  @US-5 @FR-5.2
  Scenario: Configure percentage-based threshold (Happy Path)
    Given a limit "Daily Transfer Limit" exists with amount 100000 USD
    When Admin sets approval threshold at 80% of limit
    Then transactions > 80000 USD require approval

  @US-8 @FR-5.3
  Scenario: Transaction below threshold does not require approval (Happy Path)
    Given account "ACC-001" has daily limit 100000 USD with threshold 80000 USD
    When Payment module checks a transaction of 60000 USD
    Then the result is "ALLOWED" with approvalRequired = false

  @US-8 @FR-5.3
  Scenario: Transaction above threshold requires approval (Edge Case)
    Given account "ACC-001" has daily limit 100000 USD with threshold 80000 USD
    When Payment module checks a transaction of 85000 USD
    Then the result is "ALLOWED" with approvalRequired = true

  @US-8 @FR-5.3
  Scenario: Transaction exactly at threshold does not require approval (Edge Case)
    Given account "ACC-001" has daily limit 100000 USD with threshold 80000 USD
    When Payment module checks a transaction of 80000 USD
    Then the result is "ALLOWED" with approvalRequired = false
```

---

## 7. Approval Workflow

```gherkin
Feature: Approval Workflow

  @US-11 @FR-7.1
  Scenario: View pending approval requests (Happy Path)
    Given 3 transactions require approval
    When Approver views pending approvals
    Then 3 approval requests are returned

  @US-11 @FR-7.1
  Scenario: View pending approvals when none exist (Edge Case)
    Given no transactions require approval
    When Approver views pending approvals
    Then an empty list is returned

  @US-12 @FR-7.2
  Scenario: Approve a pending transaction (Happy Path)
    Given transaction "TXN-001" has approval status "PENDING"
    When Approver approves "TXN-001"
    Then approval status changes to "APPROVED"

  @US-12 @FR-7.2
  Scenario: Reject a pending transaction (Happy Path)
    Given transaction "TXN-001" has approval status "PENDING"
    When Approver rejects "TXN-001" with reason "Exceeds risk tolerance"
    Then approval status changes to "REJECTED"
    And the rejection reason is stored

  @US-12 @FR-7.2
  Scenario: Approve an already-approved transaction is rejected (Negative)
    Given transaction "TXN-001" has approval status "APPROVED"
    When Approver attempts to approve "TXN-001"
    Then the action is rejected with error "Approval request is not pending"
```

---

## 8. Traceability Matrix

### User Story → BDD Scenario Coverage

| User Story | FR(s) | BDD Scenario(s) |
|------------|-------|-----------------|
| US-1 | FR-1.1, FR-1.2 | S1.1, S1.2, S1.3 |
| US-2 | FR-2.1, FR-2.2 | S2.1, S2.2 |
| US-3 | FR-3.1 | S3.1, S3.2 |
| US-4 | FR-4.1 | S4.1, S4.2 |
| US-5 | FR-5.1, FR-5.2 | S6.1, S6.2 |
| US-6 | FR-1.3 | S1.4, S1.5 |
| US-7 | FR-6.1 | S5.1, S5.2, S5.3, S5.6, S5.7, S5.8 |
| US-8 | FR-5.3 | S6.3, S6.4, S6.5 |
| US-9 | FR-6.2 | S5.4 |
| US-10 | FR-6.3 | S5.5 |
| US-11 | FR-7.1 | S7.1, S7.2 |
| US-12 | FR-7.2 | S7.3, S7.4, S7.5 |

### Requirement Coverage Summary

| Requirement | Covered By Scenario(s) |
|-------------|----------------------|
| FR-1.1 | S1.1, S1.2 |
| FR-1.2 | S1.3 |
| FR-1.3 | S1.4, S1.5 |
| FR-2.1 | S2.1 |
| FR-2.2 | S2.2 |
| FR-3.1 | S3.1, S3.2 |
| FR-4.1 | S4.1, S4.2 |
| FR-5.1 | S6.1 |
| FR-5.2 | S6.2 |
| FR-5.3 | S6.3, S6.4, S6.5 |
| FR-6.1 | S5.1, S5.2, S5.3, S5.6, S5.7, S5.8 |
| FR-6.2 | S5.4 |
| FR-6.3 | S5.5 |
| FR-7.1 | S7.1, S7.2 |
| FR-7.2 | S7.3, S7.4, S7.5 |
