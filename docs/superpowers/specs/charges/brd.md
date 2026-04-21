# Business Requirements Document: Charges Management

**Version:** 1.0  
**Date:** 2026-03-21  
**Status:** Draft  
**Module:** Charges Management (`com.banking.charges`)

---

## 1. Business Goals & Success Criteria

### Business Goals

1. Provide a flexible fee and interest engine for all banking products
2. Support complex tiered pricing common in corporate banking
3. Enable fee waivers and promotional schedules without code deployments
4. Accurate interest accrual and periodic application

### Success Criteria

- All product fees calculated accurately based on configured rules
- Fee waivers applied correctly based on eligibility criteria
- Interest accrued daily and applied per schedule
- All charge calculations auditable

---

## 2. User Roles & Stories

### Roles

| Role | Responsibilities |
|------|-----------------|
| **Bank Admin** | Manages charge definitions, fee schedules, and waivers |
| **System** | Calculates charges during transactions and account operations |

### User Stories

#### Bank Admin

- **US-1** As an Admin, I can define charge types (MONTHLY_MAINTENANCE, TRANSACTION_FEE, OVERDRAFT_FEE, INTEREST) so different fees are tracked separately
- **US-2** As an Admin, I can configure flat fees, percentage-based fees, and tiered volume fees
- **US-3** As an Admin, I can assign charges to products so each product has its own fee schedule
- **US-4** As an Admin, I can assign charges to specific customers (overrides product-level)
- **US-5** As an Admin, I can create fee waivers for specific customers, accounts, or time periods
- **US-6** As an Admin, I can configure interest rates (fixed or tiered by balance) for savings and overdraft
- **US-7** As an Admin, I can set up interest accrual schedules (daily, monthly) and application dates

#### System / Downstream Modules

- **US-8** As the Transaction module, I can calculate the fee for a transaction based on configured rules
- **US-9** As the system, I can check if a fee waiver applies before charging
- **US-10** As the system, I can calculate interest accrual for a given period
- **US-11** As the system, I can apply accrued interest to accounts on schedule

### User Story to Functional Requirement Mapping

| User Story | Functional Requirements |
|------------|------------------------|
| US-1 | FR-1.1, FR-1.2 |
| US-2 | FR-2.1, FR-2.2, FR-2.3 |
| US-3 | FR-3.1 |
| US-4 | FR-3.2 |
| US-5 | FR-4.1, FR-4.2, FR-4.3 |
| US-6 | FR-5.1, FR-5.2 |
| US-7 | FR-5.3 |
| US-8 | FR-6.1 |
| US-9 | FR-4.4 |
| US-10 | FR-6.2 |
| US-11 | FR-6.3 |

---

## 3. Functional Requirements

### FR-1: Charge Definition

- **FR-1.1** Charges have: name, chargeType (MONTHLY_MAINTENANCE, TRANSACTION_FEE, OVERDRAFT_FEE, INTEREST, etc.), currency, status
- **FR-1.2** Charges can be activated or deactivated

### FR-2: Fee Calculation Methods

- **FR-2.1** Flat fee: Fixed amount per occurrence
- **FR-2.2** Percentage fee: Percentage of transaction amount with optional min/max caps
- **FR-2.3** Tiered volume fee: Different rates based on volume ranges (0-100 free, 101-500 at $0.50, etc.)

### FR-3: Charge Assignment

- **FR-3.1** Charges can be assigned to a product code (all accounts of that product use these charges)
- **FR-3.2** Charges can be assigned to a specific customer (overrides product-level)

### FR-4: Fee Waivers

- **FR-4.1** Waivers have: scope (CUSTOMER, ACCOUNT, PRODUCT), reference ID, chargeType, validFrom, validTo
- **FR-4.2** Waivers can be time-limited or permanent (validTo = null)
- **FR-4.3** Waivers can waive 100% or a percentage of the fee
- **FR-4.4** System checks applicable waivers before calculating final charge

### FR-5: Interest Configuration

- **FR-5.1** Interest rates can be fixed or tiered by account balance
- **FR-5.2** Interest can be positive (savings) or negative (overdraft/loan)
- **FR-5.3** Accrual schedule: daily accrual, periodic application (monthly, quarterly)

### FR-6: Charge Calculation

- **FR-6.1** System calculates the applicable charge for a transaction or account operation
- **FR-6.2** System calculates interest accrual for a given period
- **FR-6.3** System applies accrued interest to account balance on schedule

### FR-7: Audit

- **FR-7.1** All charge definitions and changes are audit-logged
- **FR-7.2** All fee calculations produce an audit trail

---

## 4. Non-Functional Requirements

### NFR-1: Performance

- Fee calculation: < 20ms
- Interest accrual batch: < 5 minutes for all accounts

### NFR-2: Accuracy

- Interest calculations accurate to 6 decimal places
- Banker's rounding (HALF_UP) for all monetary calculations

---

## 5. Constraints & Assumptions

- Module package: `com.banking.charges`
- Account and Product modules provide ID references (no JPA joins)
- Currency support uses existing Currency enum
- Interest accrual initially supports daily calculation
