# BDD Specification: Charges Management

**Version:** 1.0  
**Date:** 2026-03-21  
**Status:** Draft  
**Module:** Charges Management (`com.banking.charges`)  
**BRD Reference:** `docs/superpowers/brds/charges/brd.md`

Each BDD scenario is tagged with exactly one `@US` (User Story) and one `@FR` (Functional Requirement) for atomic traceability.

**Scenario Types:**
- **Happy Path** — Standard successful flow
- **Edge Case** — Boundary, override, or unusual valid conditions
- **Negative** — Expected rejection or error

---

## 1. Charge Definition

```gherkin
Feature: Charge Definition

  @US-1 @FR-1.1
  Scenario: Create a monthly maintenance charge (Happy Path)
    When Admin creates a charge with name "Monthly Maintenance", type "MONTHLY_MAINTENANCE", currency "USD"
    Then the charge is created with status "ACTIVE"

  @US-1 @FR-1.1
  Scenario: Create a transaction fee charge (Happy Path)
    When Admin creates a charge with name "Wire Transfer Fee", type "TRANSACTION_FEE", currency "USD"
    Then the charge is created with status "ACTIVE"

  @US-1 @FR-1.1
  Scenario: Create duplicate charge name (Negative)
    Given charge "Monthly Maintenance" exists
    When Admin creates a charge with name "Monthly Maintenance"
    Then the creation is rejected with error "Charge name already exists"

  @US-1 @FR-1.2
  Scenario: Deactivate a charge (Happy Path)
    Given charge "Monthly Maintenance" is ACTIVE
    When Admin deactivates the charge
    Then the charge status is "INACTIVE"

  @US-1 @FR-1.2
  Scenario: Activate an inactive charge (Happy Path)
    Given charge "Monthly Maintenance" is INACTIVE
    When Admin activates the charge
    Then the charge status is "ACTIVE"
```

---

## 2. Fee Calculation Methods

```gherkin
Feature: Flat Fee Calculation

  @US-2 @FR-2.1
  Scenario: Configure flat fee (Happy Path)
    Given charge "Monthly Maintenance" exists
    When Admin configures flat fee of 25.00 USD
    Then the fee is calculated as 25.00 USD

  @US-2 @FR-2.1
  Scenario: Calculate flat fee (Happy Path)
    Given charge "Monthly Maintenance" has flat fee 25.00 USD
    When the system calculates the fee for an account
    Then the result is 25.00 USD
```

```gherkin
Feature: Percentage Fee Calculation

  @US-2 @FR-2.2
  Scenario: Configure percentage fee (Happy Path)
    Given charge "Wire Transfer Fee" exists
    When Admin configures percentage fee of 0.15%, min 5.00 USD, max 50.00 USD
    Then the fee configuration is saved

  @US-2 @FR-2.2
  Scenario: Calculate percentage fee above min (Happy Path)
    Given charge "Wire Transfer Fee" has percentage 0.15%, min 5.00 USD, max 50.00 USD
    When the system calculates fee for 5000 USD transaction
    Then the result is 7.50 USD

  @US-2 @FR-2.2
  Scenario: Percentage fee capped at minimum (Edge Case)
    Given charge "Wire Transfer Fee" has percentage 0.15%, min 5.00 USD, max 50.00 USD
    When the system calculates fee for 100 USD transaction
    Then the result is 5.00 USD (minimum applied)

  @US-2 @FR-2.2
  Scenario: Percentage fee capped at maximum (Edge Case)
    Given charge "Wire Transfer Fee" has percentage 0.15%, min 5.00 USD, max 50.00 USD
    When the system calculates fee for 100000 USD transaction
    Then the result is 50.00 USD (maximum applied)

  @US-2 @FR-2.2
  Scenario: Percentage fee on zero amount (Edge Case)
    Given charge "Wire Transfer Fee" has percentage 0.15%, min 5.00 USD
    When the system calculates fee for 0 USD transaction
    Then the result is 5.00 USD (minimum applied)
```

```gherkin
Feature: Tiered Volume Fee Calculation

  @US-2 @FR-2.3
  Scenario: Configure tiered volume fee (Happy Path)
    Given charge "Transaction Fee" exists
    When Admin configures tiers:
      | from | to  | rate |
      | 0    | 100 | 0.00 |
      | 101  | 500 | 0.50 |
      | 501  |     | 0.25 |
    Then the tiered fee configuration is saved

  @US-2 @FR-2.3
  Scenario: Calculate fee in first tier (free) (Happy Path)
    Given charge "Transaction Fee" has tiered fees as configured
    When the system calculates fee for 50 transactions
    Then the result is 0.00 USD

  @US-2 @FR-2.3
  Scenario: Calculate fee spanning tiers (Edge Case)
    Given charge "Transaction Fee" has tiered fees as configured
    When the system calculates fee for 200 transactions
    Then the result is 50.00 USD (100 free + 100 * 0.50)

  @US-2 @FR-2.3
  Scenario: Calculate fee in top tier (Edge Case)
    Given charge "Transaction Fee" has tiered fees as configured
    When the system calculates fee for 1000 transactions
    Then the result is 325.00 USD (100 free + 400 * 0.50 + 500 * 0.25)
```

---

## 3. Charge Assignment

```gherkin
Feature: Product-Level Charge Assignment

  @US-3 @FR-3.1
  Scenario: Assign charge to a product (Happy Path)
    Given charge "Monthly Maintenance" with flat fee 25.00 USD exists
    And product "SAVINGS" exists
    When Admin assigns the charge to product "SAVINGS"
    Then product "SAVINGS" has charge "Monthly Maintenance" of 25.00 USD

  @US-3 @FR-3.1
  Scenario: Multiple charges on same product (Edge Case)
    Given charges "Monthly Maintenance" and "Transaction Fee" exist
    And product "SAVINGS" exists
    When Admin assigns both charges to product "SAVINGS"
    Then product "SAVINGS" has 2 charges
```

```gherkin
Feature: Customer-Level Charge Assignment

  @US-4 @FR-3.2
  Scenario: Assign charge override to a customer (Happy Path)
    Given charge "Monthly Maintenance" exists with flat fee 25.00 USD
    And customer "CORP-001" exists
    When Admin assigns charge override to customer "CORP-001" with fee 15.00 USD
    Then customer "CORP-001" has charge "Monthly Maintenance" at 15.00 USD

  @US-4 @FR-3.2
  Scenario: Customer-level charge overrides product-level (Edge Case)
    Given product "SAVINGS" has "Monthly Maintenance" of 25.00 USD
    And customer "CORP-001" has "Monthly Maintenance" override of 15.00 USD
    When the system resolves charge for customer "CORP-001" on product "SAVINGS"
    Then the effective charge is 15.00 USD
```

---

## 4. Fee Waivers

```gherkin
Feature: Fee Waivers

  @US-5 @FR-4.1
  Scenario: Create a customer-level waiver (Happy Path)
    Given customer "CORP-001" exists
    When Admin creates a 100% waiver for customer "CORP-001" on charge type "MONTHLY_MAINTENANCE"
    Then the waiver is created

  @US-5 @FR-4.2
  Scenario: Create a time-limited waiver (Edge Case)
    Given customer "CORP-001" exists
    When Admin creates a waiver valid from 2026-01-01 to 2026-06-30
    Then the waiver is active within that date range only

  @US-5 @FR-4.3
  Scenario: Create a partial waiver (Edge Case)
    Given customer "CORP-001" exists
    When Admin creates a 50% waiver for customer "CORP-001" on charge type "TRANSACTION_FEE"
    Then only 50% of the fee is charged

  @US-5 @FR-4.1
  Scenario: Create account-level waiver (Happy Path)
    Given account "ACC-001" exists
    When Admin creates a 100% waiver for account "ACC-001" on charge type "OVERDRAFT_FEE"
    Then the waiver is created

  @US-9 @FR-4.4
  Scenario: Waiver applied before fee calculation (Happy Path)
    Given customer "CORP-001" has 100% waiver on "MONTHLY_MAINTENANCE"
    When the system calculates monthly maintenance for customer "CORP-001"
    Then the result is 0.00 USD

  @US-9 @FR-4.4
  Scenario: No waiver applies full fee (Happy Path)
    Given customer "CORP-002" has no waivers
    And "Monthly Maintenance" is 25.00 USD
    When the system calculates monthly maintenance for customer "CORP-002"
    Then the result is 25.00 USD

  @US-9 @FR-4.4
  Scenario: Expired waiver does not apply (Edge Case)
    Given customer "CORP-001" had a waiver valid 2025-01-01 to 2025-12-31
    When the system calculates fee on 2026-03-01
    Then the waiver is not applied and full fee is charged

  @US-5 @FR-4.1
  Scenario: Duplicate waiver rejected (Negative)
    Given customer "CORP-001" has waiver on "MONTHLY_MAINTENANCE"
    When Admin tries to create another waiver for same customer and charge type
    Then the creation is rejected with error "Waiver already exists"
```

---

## 5. Interest Configuration

```gherkin
Feature: Interest Rate Configuration

  @US-6 @FR-5.1
  Scenario: Configure fixed interest rate (Happy Path)
    Given charge "Savings Interest" exists with type "INTEREST"
    When Admin configures fixed interest rate of 3.5% per annum
    Then the interest rate is saved

  @US-6 @FR-5.1
  Scenario: Configure tiered interest rate by balance (Edge Case)
    Given charge "Savings Interest" exists with type "INTEREST"
    When Admin configures tiered rates:
      | balance_from | balance_to | rate  |
      | 0            | 10000      | 1.00% |
      | 10001        | 50000      | 2.50% |
      | 50001        |            | 3.50% |
    Then the tiered interest rates are saved

  @US-6 @FR-5.2
  Scenario: Configure negative (overdraft) interest (Edge Case)
    Given charge "Overdraft Interest" exists with type "INTEREST"
    When Admin configures interest rate of -8.5% per annum
    Then the interest rate is saved as negative
```

```gherkin
Feature: Interest Accrual

  @US-7 @FR-5.3
  Scenario: Configure daily accrual schedule (Happy Path)
    Given charge "Savings Interest" has interest rate 3.5%
    When Admin sets accrual to daily and application to monthly
    Then the schedule is saved

  @US-10 @FR-6.2
  Scenario: Calculate daily interest accrual (Happy Path)
    Given account "ACC-001" has balance 10000 USD with 3.5% interest
    When the system calculates daily accrual
    Then the daily accrual is approximately 0.96 USD (10000 * 0.035 / 365)

  @US-10 @FR-6.2
  Scenario: Calculate accrual for zero balance (Edge Case)
    Given account "ACC-001" has balance 0 USD with 3.5% interest
    When the system calculates daily accrual
    Then the daily accrual is 0.00 USD

  @US-10 @FR-6.2
  Scenario: Calculate accrual for period with balance changes (Edge Case)
    Given account "ACC-001" has balance 10000 USD on Mar 1
    And balance changed to 20000 USD on Mar 15
    When the system calculates accrual for March
    Then accrual is calculated per-day based on daily balances

  @US-11 @FR-6.3
  Scenario: Apply accrued interest to account (Happy Path)
    Given account "ACC-001" has accrued interest of 28.77 USD
    When the system applies interest on the scheduled date
    Then account balance increases by 28.77 USD

  @US-11 @FR-6.3
  Scenario: Apply negative interest (overdraft) (Edge Case)
    Given account "ACC-001" has accrued overdraft interest of -12.50 USD
    When the system applies interest on the scheduled date
    Then account balance decreases by 12.50 USD

  @US-11 @FR-6.3
  Scenario: Interest application resets accrual counter (Edge Case)
    Given account "ACC-001" has accrued interest of 28.77 USD
    When the system applies interest
    Then the accrued interest resets to 0.00 USD
```

---

## 6. Charge Calculation Integration

```gherkin
Feature: Fee Calculation for Transactions

  @US-8 @FR-6.1
  Scenario: Calculate fee for wire transfer (Happy Path)
    Given product "CURRENT" has "Wire Transfer Fee" of 0.15% (min 5, max 50)
    When Transaction module calculates fee for 10000 USD wire transfer
    Then the fee is 15.00 USD

  @US-8 @FR-6.1
  Scenario: Calculate fee with waiver applied (Happy Path)
    Given product "CURRENT" has "Wire Transfer Fee" of 15.00 USD
    And customer "CORP-001" has 100% waiver on "TRANSACTION_FEE"
    When Transaction module calculates fee for customer "CORP-001"
    Then the fee is 0.00 USD

  @US-8 @FR-6.1
  Scenario: Calculate fee with partial waiver (Edge Case)
    Given product "CURRENT" has "Wire Transfer Fee" of 20.00 USD
    And customer "CORP-001" has 50% waiver on "TRANSACTION_FEE"
    When Transaction module calculates fee for customer "CORP-001"
    Then the fee is 10.00 USD

  @US-8 @FR-6.1
  Scenario: No charge configured returns zero (Edge Case)
    Given no charges are configured for product "NEW-PRODUCT"
    When Transaction module calculates fee for "NEW-PRODUCT"
    Then the fee is 0.00 USD
```

---

## 7. Audit Trail

```gherkin
Feature: Charges Audit Trail

  @US-7 @FR-7.1
  Scenario: Charge creation is audited (Happy Path)
    When Admin creates charge "Monthly Maintenance"
    Then an audit record is created with action "CREATE", entity "Charge"

  @US-7 @FR-7.1
  Scenario: Fee calculation is audited (Happy Path)
    When the system calculates a fee of 25.00 USD for account "ACC-001"
    Then an audit record is created with action "CALCULATE", amount 25.00, account "ACC-001"

  @US-7 @FR-7.2
  Scenario: Interest application is audited (Happy Path)
    When the system applies interest of 28.77 USD to account "ACC-001"
    Then an audit record is created with action "APPLY_INTEREST", amount 28.77, account "ACC-001"
```

---

## 8. Traceability Matrix

### User Story → BDD Scenario Coverage

| User Story | FR(s) | BDD Scenario(s) |
|------------|-------|-----------------|
| US-1 | FR-1.1, FR-1.2 | S1.1, S1.2, S1.3, S1.4, S1.5 |
| US-2 | FR-2.1, FR-2.2, FR-2.3 | S2.1, S2.2, S2.3, S2.4, S2.5, S2.6, S2.7, S2.8, S2.9, S2.10 |
| US-3 | FR-3.1 | S3.1, S3.2 |
| US-4 | FR-3.2 | S4.1, S4.2 |
| US-5 | FR-4.1, FR-4.2, FR-4.3 | S5.1, S5.2, S5.3, S5.4, S5.8 |
| US-6 | FR-5.1, FR-5.2 | S5a.1, S5a.2, S5a.3 |
| US-7 | FR-5.3, FR-7.1, FR-7.2 | S5a.4, S7.1, S7.2, S7.3 |
| US-8 | FR-6.1 | S6.1, S6.2, S6.3, S6.4 |
| US-9 | FR-4.4 | S5.5, S5.6, S5.7 |
| US-10 | FR-6.2 | S5a.5, S5a.6, S5a.7 |
| US-11 | FR-6.3 | S5a.8, S5a.9, S5a.10 |

### Requirement Coverage Summary

| Requirement | Covered By Scenario(s) |
|-------------|----------------------|
| FR-1.1 | S1.1, S1.2, S1.3 |
| FR-1.2 | S1.4, S1.5 |
| FR-2.1 | S2.1, S2.2 |
| FR-2.2 | S2.3, S2.4, S2.5, S2.6 |
| FR-2.3 | S2.7, S2.8, S2.9, S2.10 |
| FR-3.1 | S3.1, S3.2 |
| FR-3.2 | S4.1, S4.2 |
| FR-4.1 | S5.1, S5.4, S5.8 |
| FR-4.2 | S5.2 |
| FR-4.3 | S5.3 |
| FR-4.4 | S5.5, S5.6, S5.7 |
| FR-5.1 | S5a.1, S5a.2 |
| FR-5.2 | S5a.3 |
| FR-5.3 | S5a.4 |
| FR-6.1 | S6.1, S6.2, S6.3, S6.4 |
| FR-6.2 | S5a.5, S5a.6, S5a.7 |
| FR-6.3 | S5a.8, S5a.9, S5a.10 |
| FR-7.1 | S7.1, S7.2 |
| FR-7.2 | S7.3 |
