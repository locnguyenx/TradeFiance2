# Business Requirements Document: Master Data Management

**Version:** 1.0  
**Date:** 2026-03-21  
**Status:** Draft  
**Module:** Master Data Management (`com.banking.masterdata`)

---

## 1. Business Goals & Success Criteria

### Business Goals

1. Provide a single source of truth for all reference data used across banking operations
2. Enable admin-manageable reference data without code deployments
3. Support effective-dated data (exchange rates) for accurate historical calculations
4. Maintain holiday calendars for correct value date computations

### Success Criteria

- All foundation modules can query master data via clean read interfaces
- Exchange rates support effective-date versioning (latest + historical lookup)
- Holiday calendar supports country + year range queries
- All changes audit-logged

---

## 2. User Roles & Stories

### Roles

| Role | Responsibilities |
|------|-----------------|
| **Bank Admin** | Manages all master data entities (CRUD) |
| **System** | Queries reference data during business operations |

### User Stories

#### Bank Admin

- **US-1** As an Admin, I can manage currencies (create, update, deactivate) so the system supports a global set of ISO 4217 currencies
- **US-2** As an Admin, I can manage countries (create, update, deactivate) so compliance validates jurisdiction
- **US-3** As an Admin, I can manage industry classification codes so customer risk profiling uses standard categories
- **US-4** As an Admin, I can manage exchange rates with effective dates so historical currency conversions are accurate
- **US-5** As an Admin, I can maintain a holiday calendar per country so value dates skip non-business days
- **US-6** As an Admin, I can manage bank branches so customers are assigned to the correct banking unit
- **US-7** As an Admin, I can manage banking channels (branch, online, mobile, ATM) so transaction routing works correctly
- **US-8** As an Admin, I can manage document types so KYC captures the correct document categories

#### System / Downstream Modules

- **US-9** As the Payment module, I can query active currencies so payment validation succeeds
- **US-10** As the system, I can look up the latest exchange rate between two currencies for real-time conversion
- **US-11** As the system, I can check if a date is a holiday in a country so value dates skip holidays
- **US-12** As the KYC module, I can retrieve active document types so customer onboarding shows the correct upload options
- **US-13** As the Account module, I can query active branches so account opening assigns the correct branch

### User Story to Functional Requirement Mapping

| User Story | Functional Requirements |
|------------|------------------------|
| US-1 | FR-1.1, FR-1.2, FR-1.3, FR-1.4 |
| US-2 | FR-2.1, FR-2.2 |
| US-3 | FR-3.1 |
| US-4 | FR-4.1, FR-4.2, FR-4.3 |
| US-5 | FR-5.1, FR-5.2 |
| US-6 | FR-6.1, FR-6.2 |
| US-7 | FR-7.1, FR-7.2 |
| US-8 | FR-8.1, FR-8.2 |
| US-9 | FR-1.1, FR-1.2 |
| US-10 | FR-4.1 |
| US-11 | FR-5.2 |
| US-12 | FR-8.1 |
| US-13 | FR-6.1 |

---

## 3. Functional Requirements

### FR-1: Currency

- **FR-1.1** Currencies have: code (ISO 4217, primary key), name, symbol, decimalPlaces, isActive
- **FR-1.2** Admin can list/filter active currencies
- **FR-1.3** Admin can create, update, and deactivate a currency
- **FR-1.4** Currency code is immutable once created

### FR-2: Country

- **FR-2.1** Countries have: isoCode (ISO 3166-1, primary key), name, region, isActive
- **FR-2.2** Admin can list/filter active countries

### FR-3: Industry

- **FR-3.1** Industries have: code, name, parentCode (for hierarchical classification), isActive

### FR-4: Exchange Rate

- **FR-4.1** Exchange rates have: baseCurrency, targetCurrency, rate, effectiveDate
- **FR-4.2** Rates are effective-dated — multiple rates can exist for the same currency pair with different dates
- **FR-4.3** Base and target currencies must reference existing active Currency records

### FR-5: Holiday

- **FR-5.1** Holidays have: countryCode, date, description
- **FR-5.2** System can query holidays by country and year range
- **FR-5.3** Country must reference an existing Country record

### FR-6: Branch

- **FR-6.1** Branches have: code (unique), name, countryCode, address, isActive
- **FR-6.2** Admin can list/filter active branches, optionally by country

### FR-7: Channel

- **FR-7.1** Channels have: code (unique), name, isActive
- **FR-7.2** Admin can list active channels

### FR-8: Document Type

- **FR-8.1** Document types have: code (unique), name, category (e.g., IDENTITY, ADDRESS, CORPORATE), isActive
- **FR-8.2** Admin can list active document types, optionally filtered by category

### FR-9: Audit & Data Integrity

- **FR-9.1** All entities include audit fields: createdAt, createdBy, updatedAt, updatedBy
- **FR-9.2** Seed data provided for ISO 4217 currencies, ISO 3166 countries, NACE industries

---

## 4. Non-Functional Requirements

### NFR-1: Performance

- Lookup by code: < 20ms
- List with filters: < 100ms (paginated)

### NFR-2: Audit

- All CRUD operations audit-logged
- Audit records are immutable

### NFR-3: Data Integrity

- Referential integrity enforced (exchange rates → currencies, holidays → countries, branches → countries)

---

## 5. Constraints & Assumptions

- Module package: `com.banking.masterdata`
- Database migrations via Flyway
- Seed data loaded via migration scripts
- No external data feeds for now (manual admin entry)
