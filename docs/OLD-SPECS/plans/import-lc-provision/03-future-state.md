# Future State Definition - Import LC Provision Collection

**Created**: 2026-03-15
**Phase**: 3 - Future State Definition
**Agent**: @plan + @reasoning
**Feature**: Import LC Provision Collection
**Input**: 01-business-requirements.md, 02-current-state.md

---

## 1. Process Flow

### End-to-End Process

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    PROVISION COLLECTION WORKFLOW                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  [Bank Staff]                                                               │
│       │                                                                     │
│       ▼                                                                     │
│  ┌─────────────────┐                                                       │
│  │ Select LC       │                                                       │
│  │ Enter X USD     │                                                       │
│  └────────┬────────┘                                                       │
│           │                                                                 │
│           ▼                                                                 │
│  ┌─────────────────────────────────────┐                                  │
│  │ Provision Collection Screen         │                                  │
│  │ ┌─────────────────────────────────┐ │                                  │
│  │ │ Account 1: [Select] Currency    │ │                                  │
│  │ │ Amount: [________] Converted:   │ │                                  │
│  │ │ USD: [Display] Rate: [Display] │ │                                  │
│  │ └─────────────────────────────────┘ │                                  │
│  │ [+ Add Another Account]            │                                  │
│  │                                     │                                  │
│  │ Total Collected: [XXX USD]          │                                  │
│  │ Provision Required: [X USD]        │                                  │
│  │ Status: [INCOMPLETE/COMPLETE]      │                                  │
│  └────────┬────────────────────────────┘                                  │
│           │                                                                 │
│           ▼ (When Total = X USD)                                           │
│  ┌─────────────────┐                                                       │
│  │ [Collect Funds]│ ──► CBS: Hold Funds (per account)                    │
│  └────────┬────────┘                                                       │
│           │                                                                 │
│           ▼                                                                 │
│  ┌─────────────────────────────┐                                           │
│  │ Provision Record Created    │                                           │
│  │ - Multiple collection items │                                           │
│  │ - CBS hold references       │                                           │
│  │ - Status: LcPrvHeld         │                                           │
│  └─────────────────────────────┘                                           │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Step-by-Step Process

| Step | Actor | Action | System Response |
|------|-------|--------|-----------------|
| 1 | Bank Staff | Select LC for provision collection | Load LC details, existing provision status |
| 2 | System | Display provision amount (X USD) | From LC or manual entry |
| 3 | Bank Staff | Add account #1 | - Fetch CBS account balance |
| 4 | System | Display account currency | - |
| 5 | Bank Staff | Enter amount in account currency | - |
| 6 | System | Call CBS `get#ExchangeRate` | Return rate |
| 7 | System | Display converted amount in USD | Calculate: Amount × Rate |
| 8 | Bank Staff | Add more accounts (repeat 3-7) | Accumulate total |
| 9 | System | Validate total = X USD | Enable "Collect" button when equal |
| 10 | Bank Staff | Click "Collect Funds" | - |
| 11 | System | For each account: CBS `hold#Funds` | Get hold reference per account |
| 12 | System | Create `LcProvisionCollection` with items | Save all data |
| 13 | System | Display success | Show CBS references |

---

## 2. Feature Prioritization

| Priority | Feature | Description | Phase |
|----------|---------|-------------|-------|
| **P0 - MVP** | Single collection, multiple accounts | Collect from multiple accounts in one transaction | 1 |
| **P0 - MVP** | Exchange rate conversion | Convert non-USD to USD | 1 |
| **P0 - MVP** | Total validation | Ensure total = X USD | 1 |
| **P1** | CBS hold per account | Track individual hold references | 1 |
| **P1** | Reversal/Refund | Full reversal workflow | 2 |
| **P2** | Partial collection (Phase 2) | Multiple partial collections over time | 3 |
| **P2** | Historical tracking | View past collections | 3 |

---

## 3. Data Requirements

| Data | Type | Source |
|------|------|--------|
| Provision amount (X USD) | Currency-amount | LC entity or manual entry |
| Account ID | Party ID | Applicant's accounts |
| Account currency | Uom ID | CBS account lookup |
| Collection amount | Currency-amount | User input |
| Exchange rate | Decimal | CBS `get#ExchangeRate` |
| Converted USD amount | Currency-amount | Calculated |
| CBS hold reference | Text | CBS `hold#Funds` response |
| Collection status | Status ID | System-generated |

---

## 4. Integration Points

| System | Integration | Method |
|--------|-------------|--------|
| CBS (Core Banking) | Exchange rate lookup | `get#ExchangeRate` |
| CBS (Core Banking) | Fund hold | `hold#Funds` (per account) |
| CBS (Core Banking) | Fund release | `release#Funds` |
| TradeFinance | LC entity | Link provision to LC |
| TradeFinance | LcProvision entity | Create/update provision records |

---

## 5. Risk Assessment

| Risk | Severity | Mitigation |
|------|----------|------------|
| **Exchange rate fluctuation** between entry and collection | Medium | Use rate at collection time, not entry time; store rate with timestamp |
| **CBS hold failure** on one account | High | Rollback all holds if any fails; use transaction rollback |
| **Race condition** - concurrent collections | Medium | Lock LC during collection; use optimistic locking |
| **Account not found** in CBS | Low | Validate account before allowing selection |
| **Over-collection** (total > X) | High | Prevent submission, show warning |
| **Under-collection** (total < X) | High | Disable collect button until equal |
| **Exchange rate service unavailable** | Medium | Cache rates; allow manual override with approval |
| **Rounding errors** in conversion | Low | Use high precision (6 decimal places); define tolerance (±$0.01) |

---

**Last Updated**: 2026-03-15
