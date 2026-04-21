# BDD Specification: Master Data Management

**Version:** 1.1  
**Date:** 2026-03-21  
**Status:** Draft  
**Module:** Master Data Management (`com.banking.masterdata`)  
**BRD Reference:** `docs/superpowers/brds/master-data/brd.md`

Each BDD scenario is tagged with exactly one `@US` (User Story) and one `@FR` (Functional Requirement) for atomic traceability.

**Scenario Types:**
- **Happy Path** — Standard successful flow
- **Edge Case** — Boundary, override, or unusual valid conditions
- **Negative** — Expected rejection or error

---

## 1. Currency Management

```gherkin
Feature: Currency Management

  @US-1 @FR-1.1
  Scenario: Create a new currency (Happy Path)
    Given currency SGD does not exist
    When Admin creates currency with code SGD, name "Singapore Dollar", symbol "S$", decimals 2
    Then currency SGD exists with those attributes

  @US-1 @FR-1.1
  Scenario: Create currency with minimal fields (Edge Case)
    Given currency TWD does not exist
    When Admin creates currency with code TWD, name "Taiwan Dollar", symbol null, decimals 2
    Then currency TWD exists with symbol null

  @US-1 @FR-1.4
  Scenario: Currency code is immutable (Negative)
    Given currency USD exists
    When Admin attempts to change currency code to "USD2"
    Then the update is rejected with error "Currency code cannot be changed"

  @US-1 @FR-1.2
  Scenario: List all active currencies (Happy Path)
    Given currencies USD, EUR, GBP are active
    And currency ABC is inactive
    When Admin requests the list of active currencies
    Then the result contains USD, EUR, GBP
    And the result does not contain ABC

  @US-1 @FR-1.2
  Scenario: List all currencies including inactive (Edge Case)
    Given currencies USD, EUR are active
    And currency ABC is inactive
    When Admin requests the list of all currencies
    Then the result contains USD, EUR, ABC

  @US-1 @FR-1.3
  Scenario: Update a currency (Happy Path)
    Given currency USD exists with name "US Dollar"
    When Admin updates currency USD name to "United States Dollar"
    Then currency USD has name "United States Dollar"

  @US-1 @FR-1.3
  Scenario: Deactivate a currency (Happy Path)
    Given currency JPY is active
    When Admin deactivates currency JPY
    Then currency JPY is inactive

  @US-1 @FR-1.3
  Scenario: Deactivate an already inactive currency (Edge Case)
    Given currency JPY is inactive
    When Admin deactivates currency JPY
    Then currency JPY remains inactive

  @US-1 @FR-1.3
  Scenario: Create duplicate currency code (Negative)
    Given currency USD exists
    When Admin creates currency with code USD
    Then the creation is rejected with error "Currency code already exists"

  @US-9 @FR-1.2
  Scenario: System queries active currencies (Happy Path)
    Given currencies USD, EUR are active
    When the Payment module queries active currencies
    Then USD and EUR are returned

  @US-9 @FR-1.2
  Scenario: System queries when no currencies active (Edge Case)
    Given no currencies are active
    When the Payment module queries active currencies
    Then an empty list is returned
```

---

## 2. Country Management

```gherkin
Feature: Country Management

  @US-2 @FR-2.1
  Scenario: Create a country (Happy Path)
    When Admin creates country with isoCode SG, name "Singapore", region "ASIA"
    Then country SG exists with those attributes

  @US-2 @FR-2.1
  Scenario: Create duplicate country code (Negative)
    Given country US exists
    When Admin creates country with isoCode US
    Then the creation is rejected with error "Country code already exists"

  @US-2 @FR-2.2
  Scenario: List active countries (Happy Path)
    Given countries US, UK, SG are active
    And country XX is inactive
    When Admin requests the list of active countries
    Then the result contains US, UK, SG

  @US-2 @FR-2.2
  Scenario: List all countries including inactive (Edge Case)
    Given countries US, UK are active
    And country XX is inactive
    When Admin requests the list of all countries
    Then the result contains US, UK, XX
```

---

## 3. Industry Management

```gherkin
Feature: Industry Classification

  @US-3 @FR-3.1
  Scenario: Create a top-level industry code (Happy Path)
    When Admin creates industry with code "64", name "Financial Service Activities"
    Then industry "64" exists

  @US-3 @FR-3.1
  Scenario: Create a child industry with parent (Edge Case)
    Given industry "64" exists with name "Financial Service Activities"
    When Admin creates industry with code "64.1", name "Central banking", parentCode "64"
    Then industry "64.1" has parent "64"

  @US-3 @FR-3.1
  Scenario: Create child with non-existent parent (Negative)
    Given industry "99" does not exist
    When Admin creates industry with code "99.1", parentCode "99"
    Then the creation is rejected with error "Parent industry not found"
```

---

## 4. Exchange Rate Management

```gherkin
Feature: Exchange Rate Management

  @US-4 @FR-4.1
  Scenario: Create an exchange rate (Happy Path)
    Given currencies USD and EUR are active
    When Admin creates exchange rate USD->EUR with rate 0.92 effective 2026-03-20
    Then exchange rate USD->EUR 0.92 exists effective 2026-03-20

  @US-4 @FR-4.2
  Scenario: Multiple rates with different effective dates (Happy Path)
    Given exchange rate USD->EUR 0.90 effective 2026-03-15 exists
    When Admin creates exchange rate USD->EUR with rate 0.92 effective 2026-03-20
    Then both rates exist: 0.90 (Mar 15) and 0.92 (Mar 20)

  @US-4 @FR-4.3
  Scenario: Exchange rate with non-existent base currency (Negative)
    Given currency ZZZ does not exist
    When Admin tries to create exchange rate ZZZ->EUR
    Then the creation is rejected with error "Base currency ZZZ not found"

  @US-4 @FR-4.3
  Scenario: Exchange rate with non-existent target currency (Negative)
    Given currency JPY does not exist
    When Admin tries to create exchange rate USD->JPY
    Then the creation is rejected with error "Target currency JPY not found"

  @US-4 @FR-4.3
  Scenario: Exchange rate with same base and target (Negative)
    Given currency USD exists
    When Admin tries to create exchange rate USD->USD
    Then the creation is rejected with error "Base and target currency must differ"

  @US-10 @FR-4.1
  Scenario: System queries latest exchange rate (Happy Path)
    Given exchange rate USD->EUR has rates: 0.90 (Mar 15), 0.91 (Mar 18), 0.92 (Mar 20)
    When the system queries latest rate for USD->EUR
    Then the result is rate 0.92 effective 2026-03-20

  @US-10 @FR-4.1
  Scenario: System queries rate with no data (Edge Case)
    Given no exchange rates exist for USD->GBP
    When the system queries latest rate for USD->GBP
    Then the result is null

  @US-10 @FR-4.1
  Scenario: Convert amount between currencies (Happy Path)
    Given exchange rate USD->EUR is 0.92 effective 2026-03-20
    When the system converts 100 USD to EUR
    Then the result is 92.00 EUR

  @US-10 @FR-4.1
  Scenario: Convert zero amount (Edge Case)
    Given exchange rate USD->EUR is 0.92
    When the system converts 0 USD to EUR
    Then the result is 0.00 EUR
```

---

## 5. Holiday Calendar

```gherkin
Feature: Holiday Management

  @US-5 @FR-5.1
  Scenario: Create a holiday (Happy Path)
    Given country US exists
    When Admin creates holiday for US on 2026-01-01 with description "New Year's Day"
    Then holiday for US on 2026-01-01 exists

  @US-5 @FR-5.3
  Scenario: Holiday with non-existent country (Negative)
    Given country XX does not exist
    When Admin tries to create holiday for XX on 2026-01-01
    Then the creation is rejected with error "Country XX not found"

  @US-5 @FR-5.1
  Scenario: Duplicate holiday for same country and date (Negative)
    Given holiday for US on 2026-01-01 exists
    When Admin tries to create another holiday for US on 2026-01-01
    Then the creation is rejected with error "Holiday already exists"

  @US-5 @FR-5.2
  Scenario: Get holidays for a year (Happy Path)
    Given US has holidays on Jan 1, Jul 4, Dec 25 in 2026
    When Admin requests holidays for US in 2026
    Then the result contains 3 holidays

  @US-5 @FR-5.2
  Scenario: Get holidays for year with no data (Edge Case)
    Given no holidays exist for UK in 2027
    When Admin requests holidays for UK in 2027
    Then an empty list is returned

  @US-11 @FR-5.2
  Scenario: System checks date is a holiday (Happy Path)
    Given 2026-01-01 is a holiday in country US
    When the system checks if 2026-01-01 is a holiday in US
    Then the result is true

  @US-11 @FR-5.2
  Scenario: System checks non-holiday date (Happy Path)
    Given 2026-01-02 is not a holiday in country US
    When the system checks if 2026-01-02 is a holiday in US
    Then the result is false

  @US-11 @FR-5.2
  Scenario: System checks date for country with no holidays (Edge Case)
    Given no holidays exist for country XX
    When the system checks if 2026-01-01 is a holiday in XX
    Then the result is false
```

---

## 6. Branch Management

```gherkin
Feature: Branch Management

  @US-6 @FR-6.1
  Scenario: Create a branch (Happy Path)
    Given country US exists
    When Admin creates branch with code NYC-001, name "New York Main", country US
    Then branch NYC-001 exists

  @US-6 @FR-6.1
  Scenario: Create duplicate branch code (Negative)
    Given branch NYC-001 exists
    When Admin creates branch with code NYC-001
    Then the creation is rejected with error "Branch code already exists"

  @US-6 @FR-6.1
  Scenario: Create branch with non-existent country (Negative)
    Given country XX does not exist
    When Admin creates branch with code XX-001, country XX
    Then the creation is rejected with error "Country XX not found"

  @US-6 @FR-6.2
  Scenario: List active branches (Happy Path)
    Given branches NYC-001 and LON-001 are active, BER-002 is inactive
    When Admin requests active branches
    Then the result contains NYC-001 and LON-001

  @US-6 @FR-6.2
  Scenario: List branches filtered by country (Edge Case)
    Given branches NYC-001 (US) and LON-001 (UK) are active
    When Admin requests active branches for country US
    Then the result contains NYC-001 only

  @US-13 @FR-6.1
  Scenario: System queries active branches (Happy Path)
    Given branches NYC-001 and LON-001 are active
    When the Account module queries active branches
    Then NYC-001 and LON-001 are returned
```

---

## 7. Channel Management

```gherkin
Feature: Channel Management

  @US-7 @FR-7.1
  Scenario: Create a banking channel (Happy Path)
    When Admin creates channel with code MOBILE, name "Mobile Banking"
    Then channel MOBILE exists

  @US-7 @FR-7.1
  Scenario: Create duplicate channel code (Negative)
    Given channel MOBILE exists
    When Admin creates channel with code MOBILE
    Then the creation is rejected with error "Channel code already exists"

  @US-7 @FR-7.2
  Scenario: List active channels (Happy Path)
    Given channels BRANCH, ONLINE, MOBILE are active
    And channel FAX is inactive
    When Admin requests active channels
    Then the result contains BRANCH, ONLINE, MOBILE

  @US-7 @FR-7.2
  Scenario: List channels when all inactive (Edge Case)
    Given all channels are inactive
    When Admin requests active channels
    Then an empty list is returned
```

---

## 8. Document Type Management

```gherkin
Feature: Document Type Management

  @US-8 @FR-8.1
  Scenario: Create a document type (Happy Path)
    When Admin creates document type with code PASSPORT, name "Passport", category "IDENTITY"
    Then document type PASSPORT exists with category "IDENTITY"

  @US-8 @FR-8.1
  Scenario: Create duplicate document type code (Negative)
    Given document type PASSPORT exists
    When Admin creates document type with code PASSPORT
    Then the creation is rejected with error "Document type code already exists"

  @US-8 @FR-8.2
  Scenario: List document types filtered by category (Happy Path)
    Given document types PASSPORT (IDENTITY), UTILITY_BILL (ADDRESS), INC_CERT (CORPORATE) exist
    When Admin requests document types filtered by category "IDENTITY"
    Then the result contains PASSPORT only

  @US-8 @FR-8.2
  Scenario: List document types with no match (Edge Case)
    Given document types PASSPORT (IDENTITY) exists
    When Admin requests document types filtered by category "FINANCIAL"
    Then an empty list is returned

  @US-12 @FR-8.1
  Scenario: System queries active document types (Happy Path)
    Given document types PASSPORT and DRIVERS_LICENSE are active
    And document type EXPIRED_TYPE is inactive
    When the KYC module queries active document types
    Then PASSPORT and DRIVERS_LICENSE are returned

  @US-12 @FR-8.1
  Scenario: System queries active document types filtered by category (Edge Case)
    Given document types PASSPORT (IDENTITY) and UTILITY_BILL (ADDRESS) are active
    When the KYC module queries active document types for category "IDENTITY"
    Then only PASSPORT is returned
```

---

## 9. Audit Trail

```gherkin
Feature: Master Data Audit Trail

  @US-1 @FR-9.1
  Scenario: Currency creation is audited (Happy Path)
    When Admin creates currency with code SGD
    Then an audit record is created with action "CREATE", actor "admin", entity "Currency", entityId "SGD"

  @US-1 @FR-9.1
  Scenario: Currency update is audited (Happy Path)
    Given currency USD exists
    When Admin updates currency USD name
    Then an audit record is created with action "UPDATE", entity "Currency", entityId "USD"

  @US-1 @FR-9.1
  Scenario: Currency deactivation is audited (Happy Path)
    Given currency JPY is active
    When Admin deactivates currency JPY
    Then an audit record is created with action "UPDATE", entity "Currency", entityId "JPY", from "ACTIVE", to "INACTIVE"
```

---

## 10. Traceability Matrix

### User Story → BDD Scenario Coverage

| User Story | FR(s) | BDD Scenario(s) |
|------------|-------|-----------------|
| US-1 | FR-1.1, FR-1.2, FR-1.3, FR-1.4 | S1.1, S1.2, S1.3, S1.4, S1.5, S1.6, S1.7, S1.8, S1.9, S1.10, S1.11 |
| US-2 | FR-2.1, FR-2.2 | S2.1, S2.2, S2.3, S2.4 |
| US-3 | FR-3.1 | S3.1, S3.2, S3.3 |
| US-4 | FR-4.1, FR-4.2, FR-4.3 | S4.1, S4.2, S4.3, S4.4, S4.5, S4.6, S4.7, S4.8, S4.9 |
| US-5 | FR-5.1, FR-5.2, FR-5.3 | S5.1, S5.2, S5.3, S5.4, S5.5 |
| US-6 | FR-6.1, FR-6.2 | S6.1, S6.2, S6.3, S6.4, S6.5, S6.6 |
| US-7 | FR-7.1, FR-7.2 | S7.1, S7.2, S7.3, S7.4 |
| US-8 | FR-8.1, FR-8.2 | S8.1, S8.2, S8.3, S8.4 |
| US-9 | FR-1.1, FR-1.2 | S1.10, S1.11 |
| US-10 | FR-4.1 | S4.6, S4.7, S4.8, S4.9 |
| US-11 | FR-5.2 | S5.6, S5.7, S5.8 |
| US-12 | FR-8.1 | S8.5, S8.6 |
| US-13 | FR-6.1 | S6.6 |

### Requirement Coverage Summary

| Requirement | Covered By Scenario(s) |
|-------------|----------------------|
| FR-1.1 | S1.1, S1.2 |
| FR-1.2 | S1.4, S1.5, S1.10, S1.11 |
| FR-1.3 | S1.6, S1.7, S1.8 |
| FR-1.4 | S1.3, S1.9 |
| FR-2.1 | S2.1, S2.2 |
| FR-2.2 | S2.3, S2.4 |
| FR-3.1 | S3.1, S3.2, S3.3 |
| FR-4.1 | S4.1, S4.6, S4.7, S4.8, S4.9 |
| FR-4.2 | S4.2 |
| FR-4.3 | S4.3, S4.4, S4.5 |
| FR-5.1 | S5.1, S5.3 |
| FR-5.2 | S5.4, S5.5, S5.6, S5.7, S5.8 |
| FR-5.3 | S5.2 |
| FR-6.1 | S6.1, S6.2, S6.3, S6.6 |
| FR-6.2 | S6.4, S6.5 |
| FR-7.1 | S7.1, S7.2 |
| FR-7.2 | S7.3, S7.4 |
| FR-8.1 | S8.1, S8.2, S8.5, S8.6 |
| FR-8.2 | S8.3, S8.4 |
| FR-9.1 | S9.1, S9.2, S9.3 |
