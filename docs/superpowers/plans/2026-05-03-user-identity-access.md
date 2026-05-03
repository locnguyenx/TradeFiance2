# User Identity & Access Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a secure, session-based authentication system with user self-service and administrative user management.

**Architecture:** Leverages Moqui's native server-side sessions via cookies (`JSESSIONID`). Frontend uses a React `AuthContext` to manage state and route guards. Admin features are consolidated into a unified "User Administration" master-detail view.

**Tech Stack:** Moqui Framework (Java/Groovy), Next.js (React), Lucide React (Icons).

---

### Task 1: Backend Auth Services & REST Mapping

**BDD Scenarios:** 
- S.1: Successful Login (Backend)
- S.2: Failed Login (Backend)
- S.3: Account Lockout (Moqui Default)
- S.6: User changes own password (Backend)

**BRD Requirements:** FR-SVC-01, FR-SVC-02, FR-SVC-10

**User-Facing:** NO

**Files:**
- Create: `runtime/component/TradeFinance/service/trade/UserAccountServices.xml`
- Modify: `runtime/component/TradeFinance/service/trade.rest.xml`
- Test: `runtime/component/TradeFinance/src/test/groovy/trade/UserAccountServicesSpec.groovy`

- [ ] **Step 1: Write failing Spock test for `get#CurrentUser` and `logout`**
- [ ] **Step 2: Run test to verify it fails**
- [ ] **Step 3: Implement `UserAccountServices.xml` with `get#CurrentUser` and `logout#User`**
- [ ] **Step 4: Update `trade.rest.xml` to expose login, logout, and current-user endpoints**
- [ ] **Step 5: Run tests to verify success**
- [ ] **Step 6: Commit**

---

### Task 2: Frontend Auth Infrastructure (Context & API)

**BDD Scenarios:** 
- Infrastructure for all Authentication scenarios.

**BRD Requirements:** FR-AUTH-03, FR-AUTH-06

**User-Facing:** YES

**Files:**
- Modify: `frontend/src/api/tradeApi.ts`
- Create: `frontend/src/context/AuthContext.tsx`
- Modify: `frontend/src/app/layout.tsx`

- [ ] **Step 1: Update `tradeApi.ts` to support `credentials: 'include'` and new auth endpoints**
- [ ] **Step 2: Create `AuthContext.tsx` to handle session initialization and user state**
- [ ] **Step 3: Wrap `layout.tsx` with `AuthProvider`**
- [ ] **Step 4: Verify session restoration in browser console**
- [ ] **Step 5: Commit**

---

### Task 3: Login Page & Route Guards

**BDD Scenarios:** 
- S.1: Successful Login
- S.2: Failed Login
- S.4: Unauthorized Access Redirect

**BRD Requirements:** FR-AUTH-01, FR-AUTH-02, FR-AUTH-04

**User-Facing:** YES

**Files:**
- Create: `frontend/src/app/login/page.tsx`
- Modify: `frontend/src/app/layout.tsx`
- Modify: `frontend/src/components/GlobalShell.tsx`

- [ ] **Step 1: Create `/login` page with high-fidelity "Blue Premium" form**
- [ ] **Step 2: Implement route guard in `layout.tsx` to redirect to `/login` if unauthenticated**
- [ ] **Step 3: Update `GlobalShell` to show loading state during auth check**
- [ ] **Step 4: Verify login flow and redirect to dashboard**
- [ ] **Step 5: Commit**

---

### Task 4: User Profile & Self-Service (Change Password)

**BDD Scenarios:** 
- S.5: View own profile
- S.6: User changes own password
- S.7: User logs out

**BRD Requirements:** FR-PROF-01 to FR-PROF-05, FR-SVC-10

**User-Facing:** YES

**Files:**
- Create: `frontend/src/components/ProfilePanel.tsx`
- Modify: `frontend/src/components/GlobalShell.tsx`

- [ ] **Step 1: Create `ProfilePanel` with identity display and "Change Password" form**
- [ ] **Step 2: Integrate `ProfilePanel` into `GlobalShell` sidebar footer**
- [ ] **Step 3: Implement logout button and verify session destruction**
- [ ] **Step 4: Verify "Change Password" validation and success**
- [ ] **Step 5: Commit**

---

### Task 5: User Administration (Account & Role Management)

**BDD Scenarios:** 
- S.8 to S.12 (Admin operations)

**BRD Requirements:** FR-ADM-01 to FR-ADM-10, FR-SVC-03 to FR-SVC-09

**User-Facing:** YES

**Files:**
- Create: `runtime/component/TradeFinance/service/trade/AdminUserServices.xml`
- Create: `frontend/src/app/admin/users/page.tsx`
- Create: `frontend/src/components/UserAdminManager.tsx` (consolidating `UserAuthorityManager`)
- Modify: `frontend/src/components/GlobalShell.tsx` (navigation update)

- [ ] **Step 1: Implement Admin-level backend services for user CRUD, roles, and status**
- [ ] **Step 2: Create `UserAdminManager` Master-Detail component**
- [ ] **Step 3: Create `/admin/users` page and update sidebar navigation**
- [ ] **Step 4: Verify Admin flows: Create User -> Assign Roles -> Set Limits -> Disable Account**
- [ ] **Step 5: Commit**
