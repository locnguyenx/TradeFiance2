# Business Requirements Document: Limits Management

**Version:** 1.0  
**Date:** 2026-03-21  
**Status:** Draft  
**Module:** Limits Management (`com.banking.limits`)

---

## 1. Business Goals & Success Criteria

### Business Goals

1. Configure and enforce transaction limits to mitigate operational and fraud risk
2. Support multi-level limits (customer, account, product) for granular control
3. Provide approval workflows when transactions exceed configured thresholds
4. Enable admins to manage limits without code deployments

### Success Criteria

- Real-time limit checking integrated into payment and transaction flows
- Approval workflow triggered automatically when transactions exceed thresholds
- Admin can configure limits at customer, account, and product levels
- All limit changes audit-logged

---

## 2. User Roles & Stories

### Roles

| Role | Responsibilities |
|------|-----------------|
| **Bank Admin** | Manages limit definitions and rules |
| **Approver** | Reviews and approves/rejects transactions that exceed limits |
| **System** | Enforces limits during transaction processing |

### User Stories

#### Bank Admin

- **US-1** As an Admin, I can define limit types (DAILY, WEEKLY, MONTHLY, PER_TRANSACTION) so different periods are enforced
- **US-2** As an Admin, I can set limits at the product level (e.g., all Savings Accounts have a $10,000 daily limit)
- **US-3** As an Admin, I can set limits at the customer level (e.g., corporate customers get $100,000 daily)
- **US-4** As an Admin, I can set limits at the account level (e.g., specific account has $50,000 daily)
- **US-5** As an Admin, I can configure approval thresholds (e.g., transactions > $50,000 need approval)
- **US-6** As an Admin, I can activate or deactivate limits

#### System / Downstream Modules

- **US-7** As the Payment module, I can check if a transaction amount exceeds the applicable limit before processing
- **US-8** As the system, I can determine if a transaction requires approval based on configured thresholds
- **US-9** As the system, I can track cumulative usage against period-based limits (daily, weekly, monthly)
- **US-10** As the system, I can reset usage counters when periods expire

#### Approver

- **US-11** As an Approver, I can view pending approval requests
- **US-12** As an Approver, I can approve or reject a transaction that exceeded limits

### User Story to Functional Requirement Mapping

| User Story | Functional Requirements |
|------------|------------------------|
| US-1 | FR-1.1, FR-1.2 |
| US-2 | FR-2.1, FR-2.2 |
| US-3 | FR-3.1 |
| US-4 | FR-4.1 |
| US-5 | FR-5.1, FR-5.2 |
| US-6 | FR-1.3 |
| US-7 | FR-6.1 |
| US-8 | FR-5.3 |
| US-9 | FR-6.2 |
| US-10 | FR-6.3 |
| US-11 | FR-7.1 |
| US-12 | FR-7.2 |

---

## 3. Functional Requirements

### FR-1: Limit Definition

- **FR-1.1** Limits have: name, limitType (DAILY, WEEKLY, MONTHLY, PER_TRANSACTION), currency, amount, status
- **FR-1.2** LimitType determines how usage is tracked and reset
- **FR-1.3** Limits can be activated or deactivated

### FR-2: Product-Level Limits

- **FR-2.1** Limits can be assigned to a product code (e.g., all Savings accounts share a limit)
- **FR-2.2** Multiple limits can apply to the same product

### FR-3: Customer-Level Limits

- **FR-3.1** Limits can be assigned to a customer ID (overrides product-level for that customer)

### FR-4: Account-Level Limits

- **FR-4.1** Limits can be assigned to a specific account number (overrides customer and product level)

### FR-5: Approval Thresholds

- **FR-5.1** Approval thresholds define a minimum amount above which a transaction requires manual approval
- **FR-5.2** Thresholds can be configured per limit (e.g., 80% of daily limit triggers review)
- **FR-5.3** System returns approvalRequired = true when transaction would exceed threshold

### FR-6: Limit Enforcement

- **FR-6.1** System checks applicable limits for a transaction (account → customer → product, most specific wins)
- **FR-6.2** System tracks cumulative usage per limit per period
- **FR-6.3** System resets usage counters when period expires

### FR-7: Approval Workflow

- **FR-7.1** System returns pending approval requests for approvers
- **FR-7.2** Approver can approve or reject; rejection returns reason
- **FR-7.3** All approval actions are audit-logged

### FR-8: Audit

- **FR-8.1** All limit changes audit-logged
- **FR-8.2** All approval actions audit-logged

---

## 4. Non-Functional Requirements

### NFR-1: Performance

- Limit check: < 10ms (critical path for payment processing)
- Usage aggregation: < 50ms

### NFR-2: Data Integrity

- No concurrent modification of limit definitions (optimistic locking)

---

## 5. Constraints & Assumptions

- Module package: `com.banking.limits`
- Account and Customer modules provide ID references (no JPA joins)
- Product module provides product code references
- Limits are currency-specific (same limit may differ per currency)
- Initial implementation supports DAILY, WEEKLY, MONTHLY, PER_TRANSACTION periods
