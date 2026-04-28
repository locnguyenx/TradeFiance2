# TradeTransaction Frontend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor the Next.js frontend to align with the new `TradeTransaction` backend architecture, moving from an `instrumentId`-centric authorization model to a `transactionId`-centric model.

---

## Task Breakdown

### Task 1: Update TypeScript Interfaces (Transaction-Primary)
**Files:**
- Modify: `frontend/src/api/types.ts`

- [ ] **Step 1: Add TradeTransaction interface** (as defined in superpowers/specs)
- [ ] **Step 2: Update TradeInstrument** (Remove legacy fields now handled by Transaction)
- [ ] **Step 3: Update QueueItem** (Shift from instrumentId to transactionId as primary identifier)

### Task 2: Refactor tradeApi Layer (Service Decoupling)
**Files:**
- Modify: `frontend/src/api/tradeApi.ts`

- [ ] **Step 1: Implement createTransaction(payload) helper**
  - All lifecycle actions (Issuance, Amendment) must now call this first.
- [ ] **Step 2: Update authorize and rejectToMaker**
  - Shift signature from `(instrumentId)` to `(transactionId)`.
- [ ] **Step 3: Add getTransaction(transactionId) and getInstrumentByTransaction(transactionId)**
  - Support for dual-data loading in checkers view.

### Task 3: Refactor CheckersQueue (Priority-Driven)
**Files:**
- Modify: `frontend/src/components/CheckersQueue.tsx`

- [ ] **Step 1: Update Table State to Transaction-Centric**
  - Use `transactionId` for table row keys.
  - Display `transactionType` and `priorityEnumId` columns.
- [ ] **Step 2: Implement Priority-Based Sorting**
  - Ensure 'Urgent' transactions appear at the top regardless of amount.

### Task 4: Refactor CheckerAuthorization (Proposed vs Current)
**Files:**
- Modify: `frontend/src/components/CheckerAuthorization.tsx`

- [ ] **Step 1: Dual-Data Loading**
  - Fetch both the base `TradeInstrument` and the active `TradeTransaction`.
- [ ] **Step 2: "Target Snapshot" UI**
  - Display fields as they will be after the transaction (Proposed) vs how they are now (Current).
- [ ] **Step 3: Signature Alignment**
  - Ensure "Approve/Reject" calls use the `transactionId`.

### Task 5: Implement Unified Instrument Traceability (Timeline)
**Files:**
- [NEW] `frontend/src/components/InstrumentTimeline.tsx`
- Modify: `frontend/src/components/ImportLcDetails.tsx` (or equivalent details page)

- [ ] **Step 1: Create InstrumentTimeline component**
  - Implement a chronological feed merging `TradeTransaction` and `TradeAuditLog` events.
  - Implement visual icons for different event types (Transaction, SWIFT, Compliance).
- [ ] **Step 2: Implement In-Timeline Actions**
  - Add "Authorize/Reject" buttons for pending transaction nodes.
  - Integrate with `tradeApi.authorize` and `tradeApi.rejectToMaker`.
- [ ] **Step 3: Integrate Timeline into Details Page**
  - Replace or augment the current static data view with the interactive timeline.

### Task 6: Implement Delta Analysis (Amendment Diff)
**Files:**
- [NEW] `frontend/src/components/AmendmentDeltaView.tsx`

- [ ] **Step 1: Create AmendmentDeltaView component**
  - Build a visual "Side-by-Side" or "Unified Diff" view comparison for instrument fields.
  - Implement logic to fetch "Previous Version" snapshot to compute deltas.
- [ ] **Step 2: Add "View Diff" trigger to Timeline**
  - Ensure every Amendment node has a "View Diff" button that opens this view.

### Task 7: Global Navigation & Contextual Search
**Files:**
- Modify: `frontend/src/components/Sidebar.tsx` (or Sidebar navigation component)
- [NEW] `frontend/src/components/GlobalSearch.tsx`
- [NEW] `frontend/src/components/TransactionLog.tsx`

- [ ] **Step 1: Refactor Sidebar Navigation**
  - Implement the three pillars: Dashboard, Instrument Management, and Workflow/Audit.
- [ ] **Step 2: Implement Global Transaction Log**
  - Cross-instrument view of all `TradeTransaction` records.
- [ ] **Step 3: Implement Contextual Search**
  - Build the search modal with the "Instruments vs. Transactions" toggle.
  - Implement the redirection logic based on context.

### Task 8: Verification & E2E Testing
- [ ] **Step 1: Run Jest unit tests for API and components**
- [ ] **Step 2: Run Playwright E2E tests for Issuance, Approval, and Amendment flows**
- [ ] **Step 3: Verify the "Contextual Search" correctly toggles between asset and workflow views**
- [ ] **Step 4: Verify the "Full Audit Trail" accurately reflects both business and system events**
- [ ] **Step 5: Verify the "Blue Premium" UI remains high-fidelity across new components**
