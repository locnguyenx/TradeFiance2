# Trade Finance Platform v3.0 — Frontend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Update the existing Next.js frontend to reflect the v3.0 entity architecture (effective values, transaction management fields, dual-status model) and implement all remaining UI wireframe requirements.

**Architecture:** Next.js 16 App Router + React 19 SPA. All data fetched from Moqui REST API (`/rest/s1/trade/*`) via `tradeApi.ts`. Components are in `src/components/`, pages in `src/app/`. Tests: Jest + React Testing Library for unit, Playwright for E2E.

**Tech Stack:** Next.js 16, React 19, TypeScript, CSS Modules + globals.css, Lucide React (icons), Jest 30, Playwright 1.59

**Depends on:** [Backend Plan](file:///Users/me/myprojects/moqui-trade/docs/superpowers/plans/2026-04-23-trade-finance-v3.md) — all API endpoints must exist before frontend integration tests can pass.

**References:**
- UI Wireframes: [2026-04-21-ui-wireframes.md](file:///Users/me/myprojects/moqui-trade/docs/superpowers/specs/2026-04-21-ui-wireframes.md)
- Design Spec §5: [2026-04-21-moqui-trade-design.md](file:///Users/me/myprojects/moqui-trade/docs/superpowers/specs/2026-04-21-moqui-trade-design.md) (lines 882-991)
- Common BDD: [2026-04-21-common-module-bdd.md](file:///Users/me/myprojects/moqui-trade/docs/superpowers/specs/2026-04-21-common-module-bdd.md)
- Import LC BDD: [2026-04-21-import-lc-bdd.md](file:///Users/me/myprojects/moqui-trade/docs/superpowers/specs/2026-04-21-import-lc-bdd.md)

**Constraints:**
- All paths relative to `frontend/`
- Follow existing patterns: `src/api/tradeApi.ts` for API, `src/components/*.tsx` for components, `src/app/**/page.tsx` for pages
- Tests: `src/components/*.test.tsx` (Jest), `e2e/*.spec.ts` (Playwright)
- CSS: Use `globals.css` design tokens + CSS Modules — no Tailwind

---

## Existing Frontend State

**Already implemented (78 source files):**
| Area | Files | Status |
|------|-------|--------|
| API Client | `tradeApi.ts` (12 methods) | Needs extension for new endpoints |
| GlobalShell | `GlobalShell.tsx` + layout | Left nav, top bar — working |
| Import LC Dashboard | `ImportLcDashboard.tsx` + page | KPI cards + table — needs effective values |
| Issuance Stepper | `IssuanceStepper.tsx` + page | 4-step form — needs new fields |
| Checker Queue | `CheckersQueue.tsx` + page | Data grid — needs priority ordering |
| Checker Auth | `CheckerAuthorization.tsx` + page | Auth screen — needs dual-checker UX |
| Doc Examination | `DocumentExamination.tsx` + page | Split pane — needs discrepancy codes |
| Amendment Stepper | `AmendmentStepper.tsx` + page | Amendment form — needs effective value display |
| Cancellation | `CancellationRequest.tsx` + page | Cancel form — working |
| Limits Dashboard | `LimitsDashboard.test.tsx` | Tests exist — component may be partial |
| Admin Pages | `admin/logs`, `admin/product`, `admin/tiers` | Pages exist — need product catalog UI |
| Party Directory | `parties/page.tsx` | Page exists — needs SWIFT BIC, sanctions status |
| Tariff Config | `tariffs/page.tsx` | Page exists — needs FeeConfiguration grid |
| Standard Clauses | `ClauseSelector.tsx` | Insert clause button — working |

---

## Phase FE-1: API Client & TypeScript Types

> Update the API layer to support all new backend endpoints and entity shapes.

---

### Task FE-1.1: Extend TypeScript Interfaces for v3.0 Entities

**BDD Scenarios:** All BDD scenarios — types drive correct data rendering
**BRD Requirements:** REQ-COM-ENT-01, REQ-IMP-02

**User-Facing:** NO (infrastructure)

**Files:**
- Modify: `src/api/tradeApi.ts`
- Create: `src/api/types.ts`
- Test: `src/api/tradeApi.integration.test.ts`

- [ ] **Step 1: Write the failing test**

```typescript
// In tradeApi.integration.test.ts
import { TradeInstrument, ImportLetterOfCredit } from '../api/types';

describe('v3.0 Type Contracts', () => {
  it('TradeInstrument includes transaction management fields', () => {
    const inst: TradeInstrument = {
      instrumentId: 'test',
      transactionRef: 'TF-IMP-26-0001',
      lifecycleStatusId: 'INST_PRE_ISSUE',
      transactionStatusId: 'TRANS_DRAFT',
      makerUserId: 'USER_001',
      makerTimestamp: '2026-06-01T10:00:00Z',
      versionNumber: 1,
      priorityEnumId: 'NORMAL',
      amount: 500000,
      currencyUomId: 'USD',
    } as TradeInstrument;
    expect(inst.transactionStatusId).toBe('TRANS_DRAFT');
    expect(inst.makerUserId).toBe('USER_001');
  });

  it('ImportLetterOfCredit includes effective values', () => {
    const lc: ImportLetterOfCredit = {
      instrumentId: 'test',
      businessStateId: 'LC_DRAFT',
      effectiveAmount: 500000,
      effectiveExpiryDate: '2026-12-31',
      effectiveOutstandingAmount: 500000,
      cumulativeDrawnAmount: 0,
      totalAmendmentCount: 0,
    } as ImportLetterOfCredit;
    expect(lc.effectiveAmount).toBe(500000);
    expect(lc.effectiveOutstandingAmount).toBe(500000);
  });
});
```

- [ ] **Step 2: Run test — expect FAIL**

```bash
cd frontend && npx jest --testPathPattern="tradeApi.integration" --no-cache 2>&1 | tail -20
```

- [ ] **Step 3: Create `src/api/types.ts` with all v3.0 interfaces**

```typescript
// ABOUTME: TypeScript interfaces for all Trade Finance entity shapes.
// ABOUTME: Aligned with Design Spec v3.0 entity definitions.

export interface TradeInstrument {
  instrumentId: string;
  transactionRef: string;
  lifecycleStatusId: string;
  transactionStatusId: string;
  productEnumId: string;
  amount: number;
  currencyUomId: string;
  outstandingAmount: number;
  baseEquivalentAmount: number;
  applicantPartyId: string;
  beneficiaryPartyId: string;
  issueDate: string;
  expiryDate: string;
  customerFacilityId: string;
  // Transaction management
  transactionDate: string;
  transactionTypeEnumId: string;
  makerUserId: string;
  makerTimestamp: string;
  checkerUserId?: string;
  checkerTimestamp?: string;
  rejectionReason?: string;
  versionNumber: number;
  lastUpdateTimestamp?: string;
  priorityEnumId: string;
}

export interface ImportLetterOfCredit {
  instrumentId: string;
  businessStateId: string;
  beneficiaryPartyId?: string;
  tolerancePositive?: number;
  toleranceNegative?: number;
  tenorTypeId?: string;
  usanceDays?: number;
  portOfLoading?: string;
  portOfDischarge?: string;
  expiryPlace?: string;
  goodsDescription?: string;
  documentsRequired?: string;
  additionalConditions?: string;
  // LC-specific new fields
  chargeAllocationEnumId?: string;
  partialShipmentEnumId?: string;
  transhipmentEnumId?: string;
  latestShipmentDate?: string;
  confirmationEnumId?: string;
  lcTypeEnumId?: string;
  productCatalogId?: string;
  // Effective values
  effectiveAmount: number;
  effectiveCurrencyUomId?: string;
  effectiveExpiryDate: string;
  effectiveTolerancePositive?: number;
  effectiveToleranceNegative?: number;
  effectiveOutstandingAmount: number;
  cumulativeDrawnAmount: number;
  totalAmendmentCount: number;
}

export interface TradeParty {
  partyId: string;
  partyName: string;
  kycStatus: string;
  kycExpiryDate?: string;
  sanctionsStatus?: string;
  countryOfRisk?: string;
  swiftBic?: string;
  registeredAddress?: string;
  partyRoleEnumId?: string;
}

export interface TradeProductCatalog {
  productCatalogId: string;
  productName: string;
  isActive: string;
  allowedTenorEnumId?: string;
  maxToleranceLimit?: number;
  allowRevolving: string;
  allowAdvancePayment: string;
  isStandby: string;
  isTransferable: string;
  accountingFrameworkEnumId?: string;
  mandatoryMarginPercent?: number;
  documentExamSlaDays: number;
  defaultSwiftFormatEnumId?: string;
}

export interface FeeConfiguration {
  feeConfigId: string;
  feeTypeEnumId: string;
  calculationMethodEnumId: string;
  ratePercent?: number;
  flatAmount?: number;
  minFloorAmount?: number;
  maxCeilingAmount?: number;
  currencyUomId?: string;
  isActive: string;
}

export interface UserAuthorityProfile {
  authorityProfileId: string;
  userId: string;
  authorityTierEnumId: string;
  maxApprovalAmount: number;
  currencyUomId: string;
  isSuspended: string;
}

export interface PresentationDiscrepancy {
  discrepancyId: string;
  presentationId: string;
  discrepancyCode: string;
  discrepancyDescription: string;
  isWaived: string;
  waivedByUserId?: string;
  waivedTimestamp?: string;
}

export interface QueueItem {
  instrumentId: string;
  transactionRef: string;
  module: string;
  action: string;
  makerUserId: string;
  baseEquivalentAmount: number;
  timeInQueue: string;
  priorityEnumId: string;
  lifecycleStatusId: string;
}
```

Update `tradeApi.ts` to import and use these types instead of inline interfaces.

- [ ] **Step 4: Run test — expect PASS**
- [ ] **Step 5: Commit**

```bash
git add src/api/types.ts src/api/tradeApi.ts src/api/tradeApi.integration.test.ts
git commit -m "feat(frontend): add v3.0 TypeScript interfaces for all entities

Extracts types to dedicated types.ts file. Adds TradeInstrument with
transaction management fields, ImportLetterOfCredit with effective values,
TradeParty, TradeProductCatalog, FeeConfiguration, UserAuthorityProfile,
PresentationDiscrepancy, QueueItem. Ref: Design Spec v3.0"
```

---

### Task FE-1.2: Extend tradeApi with New Endpoints

**BDD Scenarios:** Enables all UI scenarios
**BRD Requirements:** All UI requirements

**User-Facing:** NO

**Files:**
- Modify: `src/api/tradeApi.ts`
- Test: `src/api/tradeApi.integration.test.ts`

- [ ] **Step 1: Write the failing test**

```typescript
describe('v3.0 API Methods', () => {
  it('getApprovals accepts priority filter', () => {
    expect(typeof tradeApi.getApprovals).toBe('function');
  });
  it('getProductCatalog returns TradeProductCatalog', () => {
    expect(typeof tradeApi.getProductCatalog).toBe('function');
  });
  it('getFeeConfigurations returns FeeConfiguration[]', () => {
    expect(typeof tradeApi.getFeeConfigurations).toBe('function');
  });
  it('getParties returns TradeParty[]', () => {
    expect(typeof tradeApi.getParties).toBe('function');
  });
  it('getUserAuthorityProfiles returns profiles', () => {
    expect(typeof tradeApi.getUserAuthorityProfiles).toBe('function');
  });
  it('rejectToMaker requires rejection reason', () => {
    expect(typeof tradeApi.rejectToMaker).toBe('function');
  });
  it('waiveDiscrepancy submits waiver', () => {
    expect(typeof tradeApi.waiveDiscrepancy).toBe('function');
  });
});
```

- [ ] **Step 2: Run test — expect FAIL**

- [ ] **Step 3: Add new API methods**

```typescript
async getApprovals(params?: { tier?: string; productType?: string; actionType?: string; priority?: string }): Promise<{ approvalsList: QueueItem[] }> {
  const query = params ? '?' + new URLSearchParams(params as Record<string, string>).toString() : '';
  const res = await this._fetch(`${API_BASE}/approvals${query}`);
  return res.json();
},

async getProductCatalog(): Promise<{ productList: TradeProductCatalog[] }> {
  const res = await this._fetch(`${API_BASE}/product-catalog`);
  return res.json();
},

async updateProductCatalog(catalogId: string, data: Partial<TradeProductCatalog>): Promise<any> {
  const res = await this._fetch(`${API_BASE}/product-catalog/${catalogId}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  return res.json();
},

async getFeeConfigurations(): Promise<{ feeList: FeeConfiguration[] }> {
  const res = await this._fetch(`${API_BASE}/fee-configurations`);
  return res.json();
},

async updateFeeConfiguration(feeConfigId: string, data: Partial<FeeConfiguration>): Promise<any> {
  const res = await this._fetch(`${API_BASE}/fee-configurations/${feeConfigId}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  return res.json();
},

async getParties(search?: string): Promise<{ partyList: TradeParty[] }> {
  const query = search ? `?search=${encodeURIComponent(search)}` : '';
  const res = await this._fetch(`${API_BASE}/parties${query}`);
  return res.json();
},

async getParty(partyId: string): Promise<TradeParty> {
  const res = await this._fetch(`${API_BASE}/parties/${partyId}`);
  return res.json();
},

async getUserAuthorityProfiles(): Promise<{ profileList: UserAuthorityProfile[] }> {
  const res = await this._fetch(`${API_BASE}/authority-profiles`);
  return res.json();
},

async rejectToMaker(instrumentId: string, rejectionReason: string): Promise<any> {
  const res = await this._fetch(`${API_BASE}/reject`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ instrumentId, rejectionReason }),
  });
  return res.json();
},

async waiveDiscrepancy(presentationId: string): Promise<any> {
  const res = await this._fetch(`${API_BASE}/import-lc/presentation/${presentationId}/waive`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ applicantDecisionEnumId: 'WAIVED' }),
  });
  return res.json();
},

async settleLcPresentation(instrumentId: string, presentationId: string, data: any): Promise<any> {
  const res = await this._fetch(`${API_BASE}/import-lc/${instrumentId}/settlement`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ presentationId, ...data }),
  });
  return res.json();
},
```

- [ ] **Step 4: Run test — expect PASS**
- [ ] **Step 5: Commit**

---

## Phase FE-2: Component Updates for v3.0 Entity Model

> Update existing components to display effective values and transaction management fields.

---

### Task FE-2.1: Update IssuanceStepper for New Fields and SWIFT Validation

**BDD Scenarios:** BDD-IMP-FLOW-01 (effective value init), BDD-CMN-VAL-05 (SWIFT Layer 1)
**BRD Requirements:** REQ-UI-IMP-03

**User-Facing:** YES

**Files:**
- Modify: `src/components/IssuanceStepper.tsx`
- Modify: `src/components/IssuanceStepper.test.tsx`

- [ ] **Step 1: Write the failing test**

```typescript
describe('IssuanceStepper v3.0', () => {
  it('renders product catalog dropdown on Step 1', () => {
    render(<IssuanceStepper />);
    expect(screen.getByLabelText('LC Product')).toBeInTheDocument();
  });

  it('renders charge allocation field on Step 3', () => {
    render(<IssuanceStepper />);
    // Navigate to step 3
    fireEvent.click(screen.getByText('Margin & Charges'));
    expect(screen.getByLabelText(/charge allocation/i)).toBeInTheDocument();
  });

  it('validates SWIFT characters on goodsDescription blur', async () => {
    render(<IssuanceStepper />);
    // Navigate to step 2
    fireEvent.click(screen.getByText('Main LC Information'));
    const goodsInput = screen.getByLabelText(/description of goods/i);
    fireEvent.change(goodsInput, { target: { value: 'Steel Rods @ 50mm' } });
    fireEvent.blur(goodsInput);
    await waitFor(() => {
      expect(screen.getByText(/invalid SWIFT character/i)).toBeInTheDocument();
    });
  });

  it('renders right-navigation section anchors on Step 2', () => {
    render(<IssuanceStepper />);
    fireEvent.click(screen.getByText('Main LC Information'));
    expect(screen.getByText('Financials & Dates')).toBeInTheDocument();
    expect(screen.getByText('Terms & Shipping')).toBeInTheDocument();
    expect(screen.getByText('Narratives')).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Run test — expect FAIL**
- [ ] **Step 3: Update IssuanceStepper**

Key changes:
1. **Step 1:** Add `LC Product` dropdown populated from `tradeApi.getProductCatalog()`. Product selection drives downstream field visibility (revolving fields, transferable tab, etc.)
2. **Step 2:** Add `chargeAllocationEnumId`, `partialShipmentEnumId`, `transhipmentEnumId`, `confirmationEnumId`, `latestShipmentDate`. Add right-nav section anchors. Add inline SWIFT X-Character validation on blur for `goodsDescription`, `documentsRequired`, `additionalConditions`.
3. **Step 3:** Add margin fields and fee auto-calculation from `FeeConfiguration`
4. **Step 4 (Review):** Display `effectiveAmount`, `effectiveExpiryDate`, `effectiveOutstandingAmount` as computed preview

- [ ] **Step 4: Run test — expect PASS**
- [ ] **Step 5: Commit**

---

### Task FE-2.2: Update CheckersQueue with Priority Ordering and Dual-Status

**BDD Scenarios:** BDD-CMN-AUTH-05 (priority queue), BDD-CMN-AUTH-02 (dual checker)
**BRD Requirements:** REQ-UI-CMN-02

**User-Facing:** YES

**Files:**
- Modify: `src/components/CheckersQueue.tsx`
- Modify: `src/components/CheckersQueue.test.tsx`

- [ ] **Step 1: Write the failing test**

```typescript
describe('CheckersQueue v3.0', () => {
  it('displays priority column and sorts URGENT above NORMAL', () => {
    const items = [
      { ...mockItem, transactionRef: 'TX-A', priorityEnumId: 'NORMAL' },
      { ...mockItem, transactionRef: 'TX-B', priorityEnumId: 'URGENT' },
    ];
    render(<CheckersQueue items={items} />);
    const rows = screen.getAllByRole('row');
    // Header + 2 data rows. URGENT should appear first.
    expect(within(rows[1]).getByText('TX-B')).toBeInTheDocument();
    expect(within(rows[2]).getByText('TX-A')).toBeInTheDocument();
  });

  it('shows PARTIAL APPROVAL badge for Tier 4 pending second checker', () => {
    const items = [
      { ...mockItem, lifecycleStatusId: 'INST_PARTIAL_APPROVAL' },
    ];
    render(<CheckersQueue items={items} />);
    expect(screen.getByText(/partial approval/i)).toBeInTheDocument();
  });

  it('renders tier indicator in KPI banner', () => {
    render(<CheckersQueue items={[]} userTier="TIER_3" />);
    expect(screen.getByText(/tier 3/i)).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Run test — expect FAIL**
- [ ] **Step 3: Update CheckersQueue**

Key changes:
1. Add `Priority` column with color badges (URGENT = red, EXPRESS = orange, NORMAL = grey)
2. Default sort: priority DESC, then `timeInQueue` DESC
3. Add KPI banner with SLA warning count and Tier indicator
4. Show `PARTIAL APPROVAL` status badge for Tier 4 dual-checker items
5. Use `tradeApi.getApprovals()` with filters

- [ ] **Step 4: Run test — expect PASS**
- [ ] **Step 5: Commit**

---

### Task FE-2.3: Update CheckerAuthorization with Rejection Reason and Amendment Delta

**BDD Scenarios:** BDD-CMN-VAL-02 (segregation of duties), BDD-CMN-AUTH-02 (dual checker)
**BRD Requirements:** REQ-UI-IMP-05

**User-Facing:** YES

**Files:**
- Modify: `src/components/CheckerAuthorization.tsx`
- Modify: `src/components/CheckerAuthorization.test.tsx`

- [ ] **Step 1: Write the failing test**

```typescript
describe('CheckerAuthorization v3.0', () => {
  it('shows rejection modal with mandatory reason field', async () => {
    render(<CheckerAuthorization instrumentId="TEST" />);
    fireEvent.click(screen.getByText(/reject to maker/i));
    const modal = screen.getByRole('dialog');
    expect(within(modal).getByLabelText(/rejection reason/i)).toBeInTheDocument();
    // Reject button disabled until reason is entered
    expect(within(modal).getByRole('button', { name: /confirm reject/i })).toBeDisabled();
  });

  it('highlights amendment delta with struck-through old values', () => {
    const amendment = {
      oldAmount: 50000,
      newEffectiveAmount: 70000,
      amountAdjustment: 20000,
    };
    render(<CheckerAuthorization instrumentId="TEST" amendment={amendment} />);
    expect(screen.getByText('50,000')).toHaveClass('strikethrough');
    expect(screen.getByText('70,000')).toHaveClass('highlight-new');
  });

  it('disables AUTHORIZE when current user is the maker', () => {
    render(<CheckerAuthorization instrumentId="TEST" makerUserId="USER_001" currentUserId="USER_001" />);
    expect(screen.getByRole('button', { name: /authorize/i })).toBeDisabled();
  });
});
```

- [ ] **Step 2: Run test — expect FAIL**
- [ ] **Step 3: Update CheckerAuthorization**

Key changes:
1. Add rejection modal with mandatory `rejectionReason` textarea. Calls `tradeApi.rejectToMaker()`.
2. For amendments: display a diff panel. Old values (from `TradeInstrument.amount`) are struck-through. New values (from `ImportLetterOfCredit.effectiveAmount`) are highlighted green.
3. Disable AUTHORIZE button if `currentUserId === makerUserId` (Four-Eyes enforcement)
4. Show Maker details: `makerUserId`, `makerTimestamp` in the risk/compliance left panel

- [ ] **Step 4: Run test — expect PASS**
- [ ] **Step 5: Commit**

---

### Task FE-2.4: Update DocumentExamination with Discrepancy Codes and Waiver

**BDD Scenarios:** BDD-IMP-DOC-02 (discrepancy), BDD-IMP-DOC-03 (waiver → MT 752)
**BRD Requirements:** REQ-UI-IMP-04

**User-Facing:** YES

**Files:**
- Modify: `src/components/DocumentExamination.tsx`
- Modify: `src/components/DocumentExamination.test.tsx`

- [ ] **Step 1: Write the failing test**

```typescript
describe('DocumentExamination v3.0', () => {
  it('renders ISBP discrepancy code dropdown', () => {
    render(<DocumentExamination instrumentId="TEST" presentationId="PRES-1" isDiscrepant={true} />);
    expect(screen.getByLabelText(/isbp code/i)).toBeInTheDocument();
  });

  it('renders regulatory deadline from product SLA', () => {
    render(<DocumentExamination instrumentId="TEST" regulatoryDeadline="2026-07-05" />);
    expect(screen.getByText(/2026-07-05/)).toBeInTheDocument();
  });

  it('shows waiver button when status is LC_DISCREPANT', () => {
    render(<DocumentExamination instrumentId="TEST" businessStateId="LC_DISCREPANT" />);
    expect(screen.getByRole('button', { name: /waive discrepancies/i })).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Run test — expect FAIL**
- [ ] **Step 3: Update DocumentExamination**

Key changes:
1. Discrepancy logger: Add ISBP code dropdown (predefined list) + free-text description per discrepancy row
2. Show `regulatoryDeadline` from presentation (computed by backend: `presentationDate + documentExamSlaDays`)
3. In `LC_DISCREPANT` state, show "Waive Discrepancies" button calling `tradeApi.waiveDiscrepancy()`
4. Left pane: show `effectiveAmount`, `effectiveExpiryDate` from `ImportLetterOfCredit` (not snapshot values)

- [ ] **Step 4: Run test — expect PASS**
- [ ] **Step 5: Commit**

---

### Task FE-2.5: Update AmendmentStepper to Show Effective vs Snapshot Values

**BDD Scenarios:** BDD-IMP-AMD-01 (effective amount), BDD-IMP-AMD-04 (beneficiary consent)
**BRD Requirements:** REQ-IMP-SPEC-02

**User-Facing:** YES

**Files:**
- Modify: `src/components/AmendmentStepper.tsx`
- Modify: `src/components/AmendmentStepper.test.tsx`

- [ ] **Step 1: Write the failing test**

```typescript
describe('AmendmentStepper v3.0', () => {
  it('displays current effectiveAmount as baseline', () => {
    render(<AmendmentStepper instrumentId="TEST" currentEffectiveAmount={50000} />);
    expect(screen.getByText(/current.*50,000/i)).toBeInTheDocument();
  });

  it('previews new effectiveAmount when adjustment entered', () => {
    render(<AmendmentStepper instrumentId="TEST" currentEffectiveAmount={50000} />);
    const adjustInput = screen.getByLabelText(/amount adjustment/i);
    fireEvent.change(adjustInput, { target: { value: '20000' } });
    expect(screen.getByText(/new.*70,000/i)).toBeInTheDocument();
  });

  it('shows beneficiary consent status badge', () => {
    render(<AmendmentStepper instrumentId="TEST" beneficiaryConsentStatus="PENDING" />);
    expect(screen.getByText(/pending consent/i)).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Run test — expect FAIL**
- [ ] **Step 3: Update AmendmentStepper**

Key changes:
1. Header shows: "Original Amount: {TradeInstrument.amount}" + "Current Effective: {ImportLetterOfCredit.effectiveAmount}"
2. Live preview: as user enters `amountAdjustment`, display "New Effective: {effectiveAmount + adjustment}"
3. Tier routing preview: dynamically show which authorization tier the new effective amount will trigger
4. Beneficiary consent status badge: PENDING (yellow), ACCEPTED (green), REJECTED (red)

- [ ] **Step 4: Run test — expect PASS**
- [ ] **Step 5: Commit**

---

### Task FE-2.6: Update ImportLcDashboard to Show Effective Values

**BDD Scenarios:** BDD-IMP-FLOW-07 (settled decreases outstanding)
**BRD Requirements:** REQ-UI-IMP-02

**User-Facing:** YES

**Files:**
- Modify: `src/components/ImportLcDashboard.tsx`
- Modify: `src/components/ImportLcDashboard.test.tsx`

- [ ] **Step 1: Write the failing test**

```typescript
describe('ImportLcDashboard v3.0', () => {
  it('renders effectiveAmount column instead of amount', () => {
    const mockLcs = [{ instrumentId: '1', effectiveAmount: 500000, amount: 400000, businessStateId: 'LC_ISSUED' }];
    render(<ImportLcDashboard lcList={mockLcs} />);
    expect(screen.getByText('500,000')).toBeInTheDocument();
    expect(screen.queryByText('400,000')).not.toBeInTheDocument();
  });

  it('renders effectiveExpiryDate in expiry column', () => {
    const mockLcs = [{ instrumentId: '1', effectiveExpiryDate: '2027-06-30', expiryDate: '2026-12-31' }];
    render(<ImportLcDashboard lcList={mockLcs} />);
    expect(screen.getByText('2027-06-30')).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Run test — expect FAIL**
- [ ] **Step 3: Update ImportLcDashboard columns:** Amount → `effectiveAmount`, Expiry Date → `effectiveExpiryDate`. Add `effectiveOutstandingAmount` column. Add `cumulativeDrawnAmount` in row expandable detail.
- [ ] **Step 4: Run test — expect PASS**
- [ ] **Step 5: Commit**

---

## Phase FE-3: New Admin Screens

> Implement the admin management UIs from the Common Module wireframes.

---

### Task FE-3.1: Implement Product Configuration Matrix Screen

**BDD Scenarios:** BDD-CMN-PRD-01 to PRD-11
**BRD Requirements:** REQ-UI-CMN-06

**User-Facing:** YES

**Files:**
- Modify: `src/app/admin/product/page.tsx`
- Create: `src/components/ProductCatalogManager.tsx`
- Create: `src/components/ProductCatalogManager.test.tsx`

- [ ] **Step 1: Write the failing test**

```typescript
describe('ProductCatalogManager', () => {
  it('renders product list in left navigation', () => {
    const products = [
      { productCatalogId: 'IMP_LC_STANDARD', productName: 'Standard Import LC', isActive: 'Y' },
    ];
    render(<ProductCatalogManager products={products} />);
    expect(screen.getByText('Standard Import LC')).toBeInTheDocument();
  });

  it('renders all configuration toggles for selected product', () => {
    render(<ProductCatalogManager products={[mockProduct]} selectedId="IMP_LC_STANDARD" />);
    expect(screen.getByLabelText(/allow revolving/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/allow advance payment/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/is standby/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/is transferable/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/accounting framework/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/document exam sla days/i)).toBeInTheDocument();
  });

  it('shows Save Draft and Publish buttons', () => {
    render(<ProductCatalogManager products={[mockProduct]} selectedId="IMP_LC_STANDARD" />);
    expect(screen.getByRole('button', { name: /save draft/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /publish/i })).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Run test — expect FAIL**
- [ ] **Step 3: Implement ProductCatalogManager** — Left nav lists products, main area shows configuration toggle switches + number inputs per field from `TradeProductCatalog`. Save Draft / Publish via `tradeApi.updateProductCatalog()`.
- [ ] **Step 4: Run test — expect PASS**
- [ ] **Step 5: Commit**

---

### Task FE-3.2: Implement Tariff & Fee Configuration Screen

**BDD Scenarios:** BDD-CMN-MAS-01, BDD-CMN-MAS-02
**BRD Requirements:** REQ-UI-CMN-05

**User-Facing:** YES

**Files:**
- Modify: `src/app/tariffs/page.tsx`
- Create: `src/components/TariffManager.tsx`
- Create: `src/components/TariffManager.test.tsx`

- [ ] **Step 1: Write the failing test**

```typescript
describe('TariffManager', () => {
  it('renders fee type list in left navigation', () => {
    const fees = [
      { feeConfigId: '1', feeTypeEnumId: 'ISSUANCE_FEE', calculationMethodEnumId: 'PERCENTAGE' },
    ];
    render(<TariffManager fees={fees} />);
    expect(screen.getByText(/issuance/i)).toBeInTheDocument();
  });

  it('renders minFloorAmount field', () => {
    render(<TariffManager fees={[mockFee]} selectedId="1" />);
    expect(screen.getByLabelText(/minimum charge/i)).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Run test — expect FAIL**
- [ ] **Step 3: Implement TariffManager** — Left nav: fee types. Main area: base rule set (calculation method, rate, floor/ceiling) + exception grid for customer tier overrides. Calls `tradeApi.updateFeeConfiguration()`.
- [ ] **Step 4: Run test — expect PASS**
- [ ] **Step 5: Commit**

---

### Task FE-3.3: Update Party & KYC Directory with SWIFT BIC and Sanctions

**BDD Scenarios:** BDD-CMN-ENT-02, BDD-CMN-ENT-03
**BRD Requirements:** REQ-UI-CMN-03

**User-Facing:** YES

**Files:**
- Modify: `src/app/parties/page.tsx`
- Create: `src/components/PartyDirectory.tsx`
- Create: `src/components/PartyDirectory.test.tsx`

- [ ] **Step 1: Write the failing test**

```typescript
describe('PartyDirectory', () => {
  it('renders SWIFT BIC in detail view', () => {
    const party = { partyId: '1', partyName: 'Test Bank', swiftBic: 'TESTUSXX', kycStatus: 'Active' };
    render(<PartyDirectory parties={[party]} selectedId="1" />);
    expect(screen.getByText('TESTUSXX')).toBeInTheDocument();
  });

  it('shows sanctions status badge', () => {
    const party = { partyId: '1', partyName: 'Test', sanctionsStatus: 'Clear', kycStatus: 'Active' };
    render(<PartyDirectory parties={[party]} selectedId="1" />);
    expect(screen.getByText(/clear/i)).toBeInTheDocument();
  });

  it('renders KYC status dot colors', () => {
    const parties = [
      { partyId: '1', partyName: 'Good', kycStatus: 'Active' },
      { partyId: '2', partyName: 'Bad', kycStatus: 'Expired' },
    ];
    render(<PartyDirectory parties={parties} />);
    const dots = screen.getAllByTestId('kyc-status-dot');
    expect(dots[0]).toHaveClass('status-active');
    expect(dots[1]).toHaveClass('status-expired');
  });
});
```

- [ ] **Step 2: Run test — expect FAIL**
- [ ] **Step 3: Implement PartyDirectory** — Master-Detail split view. Left pane: searchable list with KYC color dots. Right pane tabbed: General Info (address, country, SWIFT BIC), Roles (toggle switches), Compliance (sanctions badge + screening log). Calls `tradeApi.getParties()` + `tradeApi.getParty()`.
- [ ] **Step 4: Run test — expect PASS**
- [ ] **Step 5: Commit**

---

### Task FE-3.4: Implement Credit Facility Dashboard with Exposure Widget

**BDD Scenarios:** BDD-CMN-ENT-04, BDD-CMN-NOT-01
**BRD Requirements:** REQ-UI-CMN-04

**User-Facing:** YES

**Files:**
- Modify: `src/app/facilities/page.tsx`
- Create: `src/components/FacilityDashboard.tsx`
- Create: `src/components/FacilityDashboard.test.tsx`

- [ ] **Step 1: Write the failing test**

```typescript
describe('FacilityDashboard', () => {
  it('renders horizontal exposure progress bar', () => {
    const facility = {
      facilityId: 'FAC-001',
      totalApprovedLimit: 10000000,
      utilizedAmount: 2500000,
    };
    render(<FacilityDashboard facility={facility} />);
    expect(screen.getByTestId('exposure-bar')).toBeInTheDocument();
    expect(screen.getByText(/7,500,000/)).toBeInTheDocument(); // Available
  });

  it('renders utilization breakdown table', () => {
    const transactions = [
      { transactionRef: 'TF-IMP-001', businessStateId: 'LC_ISSUED', effectiveOutstandingAmount: 500000 },
    ];
    render(<FacilityDashboard facility={mockFacility} transactions={transactions} />);
    expect(screen.getByText('TF-IMP-001')).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Run test — expect FAIL**
- [ ] **Step 3: Implement FacilityDashboard** — Applicant selector dropdown. Exposure widget: horizontal segmented bar (Firm / Contingent / Reserved / Available). Utilization breakdown table with hyperlinked transaction refs.
- [ ] **Step 4: Run test — expect PASS**
- [ ] **Step 5: Commit**

---

### Task FE-3.5: Update User Authority Tiers Admin

**BDD Scenarios:** BDD-CMN-AUTH-01, BDD-CMN-MAS-03
**BRD Requirements:** REQ-UI-CMN-01 (System Admin section)

**User-Facing:** YES

**Files:**
- Modify: `src/app/admin/tiers/page.tsx`
- Create: `src/components/UserAuthorityManager.tsx`
- Create: `src/components/UserAuthorityManager.test.tsx`

- [ ] **Step 1: Write the failing test**

```typescript
describe('UserAuthorityManager', () => {
  it('renders user profiles with tier assignments', () => {
    const profiles = [
      { authorityProfileId: '1', userId: 'USER_001', authorityTierEnumId: 'TIER_3', maxApprovalAmount: 5000000, isSuspended: 'N' },
    ];
    render(<UserAuthorityManager profiles={profiles} />);
    expect(screen.getByText('USER_001')).toBeInTheDocument();
    expect(screen.getByText(/tier 3/i)).toBeInTheDocument();
  });

  it('shows suspended badge for suspended users', () => {
    const profiles = [
      { authorityProfileId: '1', userId: 'SUSPENDED_USER', authorityTierEnumId: 'TIER_2', isSuspended: 'Y' },
    ];
    render(<UserAuthorityManager profiles={profiles} />);
    expect(screen.getByText(/suspended/i)).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Run test — expect FAIL**
- [ ] **Step 3: Implement UserAuthorityManager** — Table of user authority profiles. Columns: User, Tier, Max Amount, Currency, Suspended (toggle). Calls `tradeApi.getUserAuthorityProfiles()`.
- [ ] **Step 4: Run test — expect PASS**
- [ ] **Step 5: Commit**

---

## Phase FE-4: Settlement & Shipping Guarantee Screens

---

### Task FE-4.1: Implement Settlement Screen with Effective Outstanding Tracking

**BDD Scenarios:** BDD-IMP-FLOW-07, BDD-IMP-SET-03 (partial draw)
**BRD Requirements:** REQ-IMP-SPEC-04

**User-Facing:** YES

**Files:**
- Modify: `src/app/import-lc/settlement/[id]/page.tsx`
- Create: `src/components/SettlementForm.tsx`
- Create: `src/components/SettlementForm.test.tsx`

- [ ] **Step 1: Write the failing test**

```typescript
describe('SettlementForm', () => {
  it('displays effectiveOutstandingAmount as max settleable', () => {
    render(<SettlementForm instrumentId="TEST" effectiveOutstandingAmount={60000} />);
    expect(screen.getByText(/outstanding.*60,000/i)).toBeInTheDocument();
  });

  it('shows partial draw result preview', () => {
    render(<SettlementForm instrumentId="TEST" effectiveOutstandingAmount={100000} />);
    const amountInput = screen.getByLabelText(/settlement amount/i);
    fireEvent.change(amountInput, { target: { value: '40000' } });
    expect(screen.getByText(/remaining.*60,000/i)).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Run test — expect FAIL**
- [ ] **Step 3: Implement SettlementForm** — Shows current `effectiveOutstandingAmount`, `cumulativeDrawnAmount`. Settlement amount input with live preview of remaining balance. Calls `tradeApi.settleLcPresentation()`.
- [ ] **Step 4: Run test — expect PASS**
- [ ] **Step 5: Commit**

---

## Phase FE-5: Navigation & Left Menu Updates

---

### Task FE-5.1: Update GlobalShell Left Navigation for Common Module

**BDD Scenarios:** All admin screens need navigation access
**BRD Requirements:** REQ-UI-CMN-01

**User-Facing:** YES

**Files:**
- Modify: `src/components/GlobalShell.tsx`
- Modify: `src/components/GlobalShell.test.tsx`

- [ ] **Step 1: Write the failing test**

```typescript
describe('GlobalShell v3.0 Navigation', () => {
  it('renders Master Data section with all sub-items', () => {
    render(<GlobalShell><div>test</div></GlobalShell>);
    expect(screen.getByText('Party & KYC Directory')).toBeInTheDocument();
    expect(screen.getByText('Credit Facilities (Limits)')).toBeInTheDocument();
    expect(screen.getByText('Tariff & Fee Configuration')).toBeInTheDocument();
    expect(screen.getByText('Product Configuration')).toBeInTheDocument();
  });

  it('renders System Admin section', () => {
    render(<GlobalShell><div>test</div></GlobalShell>);
    expect(screen.getByText('User Authority Tiers')).toBeInTheDocument();
    expect(screen.getByText('Audit Logs')).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Run test — expect FAIL**
- [ ] **Step 3: Update GlobalShell** — Add all navigation items from REQ-UI-CMN-01 wireframe. Groups: Dashboard, My Approvals, Trade Modules (Import LC + locked Phase 2), Master Data (Party, Facilities, Tariffs, Product Config), System Admin (Tiers, Audit Logs).
- [ ] **Step 4: Run test — expect PASS**
- [ ] **Step 5: Commit**

---

## Phase FE-6: E2E Integration Tests

---

### Task FE-6.1: Update Playwright E2E Tests for v3.0 Flows

**BDD Scenarios:** Full E2E coverage
**BRD Requirements:** All UI requirements

**User-Facing:** NO

**Files:**
- Modify: `e2e/IssuanceFlow.spec.ts`
- Modify: `e2e/AuthorizationsFlow.spec.ts`
- Modify: `e2e/DashboardKPIs.spec.ts`
- Modify: `e2e/NavigationIntegrity.spec.ts`

- [ ] **Step 1: Update `NavigationIntegrity.spec.ts`** — verify all new nav items from REQ-UI-CMN-01 are present and route correctly
- [ ] **Step 2: Update `IssuanceFlow.spec.ts`** — verify product dropdown, SWIFT validation inline errors, effective value preview on review step
- [ ] **Step 3: Update `AuthorizationsFlow.spec.ts`** — verify rejection modal, amendment delta highlighting, priority badge display
- [ ] **Step 4: Update `DashboardKPIs.spec.ts`** — verify effectiveAmount column, effectiveExpiryDate column
- [ ] **Step 5: Run all E2E tests**

```bash
cd frontend && npx playwright test 2>&1 | tail -30
```

- [ ] **Step 6: Fix any failures and commit**

---

## Verification Plan

### Unit Tests (Jest)

```bash
cd frontend && npx jest --coverage 2>&1 | tail -30
```

**Expected:** All component tests pass. Coverage > 80% on modified files.

### E2E Tests (Playwright)

```bash
cd frontend && npx playwright test --reporter=html 2>&1 | tail -30
# Then view report:
npx playwright show-report
```

**Expected:** All 4 E2E specs pass.

### Manual Verification

After automated tests pass:

1. **Start the dev server:** `cd frontend && npm run dev`
2. **Navigation check:** Verify all nav items from REQ-UI-CMN-01 are present and link correctly
3. **Issuance flow:** Create a new LC, verify product dropdown drives field visibility, check SWIFT char validation triggers on `@` input
4. **Amendment flow:** Create an amendment, verify the effective vs original amount display, confirm the Checker auth screen highlights the delta
5. **Checker queue:** Verify URGENT items appear above NORMAL items
6. **Admin screens:** Navigate to Product Config, Tariff Config, Authority Tiers — verify CRUD operations work
