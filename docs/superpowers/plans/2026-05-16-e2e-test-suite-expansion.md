# Trade Finance E2E Coverage Expansion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close critical E2E testing gaps for Inbound SWIFT, Amendments, Presentations, and Settlements by implementing the missing "Trade Inbox" UI and extending Playwright coverage to full-lifecycle process validation.

**Architecture:** Dual-status verification (Transaction vs Instrument State). The "Trade Inbox" acts as the entry point for inbound SWIFT events, which are then correlated to instruments. E2E tests will simulate external events via API ingestion and verify UI responses.

**Tech Stack:** Next.js (Frontend), Moqui (Backend), Playwright (E2E), Lucide React (Icons).

---

### Task 1: Trade Inbox Infrastructure (API & REST)

**BDD Scenarios:** 
- BDD-INB-TIX-01: Inbox shows unread messages
- BDD-INB-TIX-02: Message detail view

**User-Facing:** NO (Internal Infrastructure)

**Files:**
- Modify: `runtime/component/TradeFinance/service/trade.rest.xml`
- Modify: `frontend/src/api/tradeApi.ts`
- Modify: `frontend/src/api/types.ts`

- [x] **Step 1: Add Inbox resources to trade.rest.xml**
- [x] **Step 2: Update frontend types.ts**
- [x] **Step 3: Update tradeApi.ts with Inbox methods**
- [ ] **Step 4: Commit Infrastructure**

---

### Task 2: Trade Inbox Dashboard & Sidebar Integration

**BDD Scenarios:** 
- BDD-INB-TIX-01: Inbox shows unread messages with badge count

**BRD Requirements:** 
- REQ-INB-01: Centralized Trade Inbox

**User-Facing:** YES

**Files:**
- Create: `frontend/src/app/import-lc/inbox/page.tsx`
- Modify: `frontend/src/components/GlobalShell.tsx`

- [ ] **Step 1: Create Inbox Dashboard Page**
Implement a high-density grid showing `TradeInboxItem` list, filtering by `inboxStatusEnumId` (Unread/Processed).

- [ ] **Step 2: Update GlobalShell.tsx Sidebar**
Add "Trade Inbox" to the "OPERATIONS" group in the sidebar.

- [ ] **Step 3: Verify Navigation**
Run the dev server and confirm "Trade Inbox" appears and opens the empty dashboard.

- [ ] **Step 4: Commit UI**
`git add . && git commit -m "feat(ui): implement trade inbox dashboard and sidebar entry"`

---

### Task 3: Inbound SWIFT E2E Test (TradeInbox.spec.ts)

**BDD Scenarios:** 
- BDD-INB-ING-01: Ingestion of MT 730
- BDD-INB-TIX-01: Inbox shows unread messages

**User-Facing:** YES

**Files:**
- Create: `frontend/e2e/TradeInbox.spec.ts`

- [ ] **Step 1: Write TradeInbox.spec.ts**
Test flow: 
1. Call `tradeApi.ingestSwiftMessage` via `page.evaluate` with a raw MT 730 payload.
2. Navigate to `/import-lc/inbox`.
3. Verify the message appears as "UNREAD".
4. Click "Acknowledge" and verify the instrument state updates to "Advised" in the dashboard.

- [ ] **Step 2: Run E2E Test**
`npx playwright test frontend/e2e/TradeInbox.spec.ts`

- [ ] **Step 3: Commit Test**
`git add . && git commit -m "test(e2e): add inbound swift and trade inbox validation"`

---

### Task 4: Presentation Attachment & Discrepancy Resolution UI

**BDD Scenarios:** 
- BDD-IMP-DOC-02: Applicant waiver with document upload

**User-Facing:** YES

**Files:**
- Modify: `frontend/src/components/PresentationDetails.tsx`

- [ ] **Step 1: Implement Attachment Upload/View**
Add logic to `PresentationDetails.tsx` to handle `DbResourceFile` storage (`parentResourceId='PRES_{id}'`) as specified in the coverage matrix.

- [ ] **Step 2: Add "Waive Discrepancy" Action**
Ensure the button triggers a document upload and then calls `waiveDiscrepancy`.

- [ ] **Step 3: Commit UI Enhancements**
`git add . && git commit -m "feat(ui): implement presentation attachments and discrepancy waiver flow"`

---

### Task 5: Comprehensive Lifecycle E2E Tests

**BDD Scenarios:** 
- BDD-IMP-AMD-01: Financial amendment
- BDD-IMP-DOC-01: Lodge presentation
- BDD-IMP-SET-01: Settlement

**User-Facing:** YES

**Files:**
- Create: `frontend/e2e/AmendmentFlow.spec.ts`
- Create: `frontend/e2e/PresentationFlow.spec.ts`
- Create: `frontend/e2e/SettlementGuarantee.spec.ts`

- [ ] **Step 1: Implement AmendmentFlow.spec.ts**
Verify: Maker creates -> Checker approves -> Dashboard shows "Amended" status.

- [ ] **Step 2: Implement PresentationFlow.spec.ts**
Verify: External ingestion (MT 750) -> Inbox -> Auto-spawn Presentation -> Waiver with Upload -> Status to "Examining".

- [ ] **Step 3: Implement SettlementGuarantee.spec.ts**
Verify: Presentation -> Settlement Initiation -> Final status "Settled".

- [ ] **Step 4: Run full suite**
`npx playwright test frontend/e2e/`

- [ ] **Step 5: Final Commit**
`git add . && git commit -m "test(e2e): complete full-lifecycle coverage for amendments, presentations, and settlements"`
