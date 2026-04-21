# Business Requirements Template

**Created**: 2026-03-15
**Phase**: 1 - Business Understanding
**Agent**: @reasoning
**Feature**: Import LC Provision Collection

---

## 1. Problem Statement

**Current State:** The LC provision mechanism handles a single provision amount held against the LC, typically in the LC's base currency (USD).

**Business Problem:** Applicants often need to fulfill provision obligations by drawing funds from multiple accounts holding various currencies (e.g., EUR, GBP, JPY). There's no mechanism to aggregate these multi-currency contributions, convert them to the LC's base currency using real-time exchange rates, and validate that the total collected equals the required provision amount $X$.

**Solution Value:** Enables flexible fund sourcing for provision fulfillment, improves operational efficiency, and ensures accurate fund verification across multiple accounts and currencies.

---

## 2. Stakeholders

| Role | Description |
|------|-------------|
| **Initiator** | Applicant (Customer) - Requests to fulfill provision obligation via multiple accounts |
| **Approver** | Trade Finance Officer / Back-office Operator - Validates collection entries and finalizes provision status |
| **User** | Relationship Manager / Account Officer - Inputs collection details from applicant's instructions |

---

## 3. Business Rules

| # | Rule | Constraint |
|---|------|------------|
| 1 | **Total Provision Match** | Sum of all collected amounts (converted to USD) must equal target provision amount $X$ defined for the LC |
| 2 | **Multi-Account Collection** | System must support specifying multiple source accounts for a single provision obligation. Each entry must capture: Account ID, Currency, Amount, Exchange Rate |
| 3 | **Real-Time Currency Conversion** | Conversion from source currency to USD must use current exchange rate fetched from CBS at the time of collection entry |
| 4 | **Account Eligibility** | Only accounts owned by the LC applicant (Party ID associated with LC) can be used for provision collection |
| 5 | **Precision Handling** | Calculations must maintain high precision (6 decimal places for exchange rates) to prevent rounding errors. Tolerance threshold (±$0.01$ USD) must be defined for total match validation |
| 6 | **Status Management** | Each collection entry must have status (Pending, Collected, Verified). Overall provision status should reflect aggregation of individual collections |

---

## 4. Success Criteria

| Criteria | Validation Method |
|----------|-------------------|
| **Multi-Currency Entry** | User can successfully add collection entries from accounts with different currencies (e.g., EUR, GBP) |
| **Accurate Conversion** | System calculates USD equivalent for each entry using CBS exchange rate fetched at entry creation |
| **Total Validation** | System validates sum of converted USD amounts equals target provision amount $X$ (within defined tolerance) |
| **UI Clarity** | UI displays breakdown showing original currency amounts, converted USD amounts, and running total |
| **Status Transition** | Provision status transitions to "Fully Collected" only when total collected amount matches $X$ |

---

## 5. Edge Cases

| Case | Handling |
|------|----------|
| **Exchange Rate Fluctuation** | If rate changes between viewing screen and submission, system must recalculate and validate (or warn user) before finalizing |
| **Rounding Discrepancies** | If sum of converted amounts differs slightly from $X$ due to floating-point arithmetic, system should accept if difference is within tolerance (±$0.01$ USD) |
| **Partial Collections** | User may submit collection that doesn't fulfill full amount $X$. System should allow if status is "Partial" but block "Complete" status until $X$ is met |
| **Unsupported Currency** | If account currency not supported by CBS exchange rate service, entry must be rejected with clear error message |
| **Concurrent Updates** | If two operators attempt collection for same LC simultaneously, system should handle locking or versioning to prevent data inconsistency |
| **CBS Service Failure** | If CBS exchange rate service unavailable, system should prevent new collections (rates cannot be verified) and queue entries for retry or manual override |

---

## 6. Questions for Next Phase

- What is the specific tolerance for rounding discrepancies (e.g., $0.01$ USD)?
- Should the exchange rate be locked at LC creation or fetched dynamically for every collection entry?
- What is the process for handling failed CBS exchange rate fetches?
- Are there specific compliance rules (e.g., AML checks) required for multi-account provision collections?
- How should the system handle historical exchange rates if a collection is edited after a period of time?

---

**Last Updated**: YYYY-MM-DD
