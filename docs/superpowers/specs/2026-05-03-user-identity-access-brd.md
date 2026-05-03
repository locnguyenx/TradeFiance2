# Business Requirements Document (BRD)

**Project Name:** Digital Trade Finance Platform
**Module:** User Identity & Access Management
**Document Version:** 1.0
**Date:** May 3, 2026

## 1. Document Control & Scope

This module introduces authentication, user self-service, and administrative user management to the Digital Trade Finance platform. Currently the platform has no login screen, no session management, and user identity is hardcoded in the UI shell.

### Scope

**In Scope:**
1. **Authentication & Session Management** — Login page, Moqui session cookies, session validation, logout
2. **User Self-Service** — Profile view (read-only), change password
3. **Admin User Management** — Create/edit users, reset passwords, assign roles, manage authority tiers, enable/disable accounts

**Out of Scope:**
- MFA / 2FA
- LDAP / SSO / Active Directory integration
- Forced password change on first login
- Email-based password reset
- Branch-level access restrictions
- Role-based route hiding (sidebar filtering by role)
- Self-service profile editing (name, email changes by the user)

### Design Decisions

| Decision | Choice | Rationale |
|:---------|:-------|:----------|
| Auth enforcement | Hard redirect to `/login` for all unauthenticated access | Banking application — no data visible without authentication |
| Session mechanism | Moqui session cookies (server-side sessions) | Native to Moqui, persistent across page refreshes, secure |
| Admin UI location | Unified "User Administration" page replacing existing UserAuthorityManager | Avoids fragmenting related admin tasks across multiple pages |
| User profile scope | View-only + change password + logout | Banking compliance — identity fields managed by admin only |
| Initial password | Admin sets directly during user creation | No email infrastructure; simplest approach for v1 |

---

## 2. User Stories

| ID | Role | Story | Acceptance Criteria |
|:---|:-----|:------|:-------------------|
| **US-01** | Any User | I want to log in with my credentials so I can access the platform | Valid credentials → redirect to dashboard; invalid → error message; session persists across page refresh |
| **US-02** | Any User | I want to view my profile so I can see my account details and role | Profile shows username, name, email, assigned roles, delegation tier |
| **US-03** | Any User | I want to change my password so I can maintain account security | Must provide current password + new password; Moqui password policy enforced |
| **US-04** | Any User | I want to log out so I can end my session securely | Session destroyed server-side; redirect to login page; subsequent API calls return 401 |
| **US-05** | System Admin | I want to create new user accounts so new staff can access the platform | Set username, name, email, initial password; assign one or more roles |
| **US-06** | System Admin | I want to edit user details so I can keep records accurate | Edit name, email; username is immutable after creation |
| **US-07** | System Admin | I want to reset a user's password so I can restore locked-out access | Admin sets new password directly; no email required |
| **US-08** | System Admin | I want to assign/remove roles so users have correct permissions | Assign/remove from: TRADE_MAKER, TRADE_CHECKER, TRADE_BACKOFFICE, TRADE_ADMIN |
| **US-09** | System Admin | I want to disable/enable user accounts for staff changes | Disabled users cannot log in; their pending queue items are flagged |

---

## 3. Functional Requirements — Authentication & Session

| ID | Requirement | Priority |
|:---|:-----------|:---------|
| **FR-AUTH-01** | All routes except `/login` require an active Moqui session. Unauthenticated requests redirect to `/login`. | Must |
| **FR-AUTH-02** | Login form accepts username + password, calls `POST /rest/s1/trade/login`. On success, Moqui session cookie is established. | Must |
| **FR-AUTH-03** | On app mount, `GET /rest/s1/trade/current-user` validates the session. If valid, returns user info (userId, username, firstName, lastName, email, roles). If invalid, returns 401. | Must |
| **FR-AUTH-04** | Failed login displays a generic error: "Invalid username or password." No information leakage about whether the username exists. | Must |
| **FR-AUTH-05** | Moqui's built-in account lockout policy applies (3 failures → 5-minute disable). The login form displays: "Account temporarily locked. Try again later." | Must |
| **FR-AUTH-06** | Logout calls a backend endpoint to invalidate the server session, clears any client-side auth state, and redirects to `/login`. | Must |

---

## 4. Functional Requirements — User Self-Service

| ID | Requirement | Priority |
|:---|:-----------|:---------|
| **FR-PROF-01** | Clicking the user avatar/name in the sidebar opens a profile dropdown or panel showing: full name, username, email, assigned roles, delegation tier. | Must |
| **FR-PROF-02** | Profile panel includes a "Change Password" action that opens an inline form. | Must |
| **FR-PROF-03** | Change password requires: current password, new password, confirm new password. Calls `org.moqui.impl.UserServices.update#Password`. | Must |
| **FR-PROF-04** | Password validation enforces Moqui's configured policy (min 8 chars, 1 digit, 1 special char). Errors are displayed inline. | Must |
| **FR-PROF-05** | Profile panel includes a "Logout" button. | Must |

---

## 5. Functional Requirements — Admin User Management

| ID | Requirement | Priority |
|:---|:-----------|:---------|
| **FR-ADM-01** | The "User Authority Tiers" page is replaced by a unified "User Administration" page at `/admin/users`. Only users with `TRADE_ADMIN` role can access it. | Must |
| **FR-ADM-02** | The page layout: left pane lists all users (username, name, role badges), right pane shows the selected user's details. Follows the existing `PartyDirectory` layout pattern. | Must |
| **FR-ADM-03** | Left pane includes a "+ New User" button and a search filter for username/name. | Must |
| **FR-ADM-04** | **Create User:** Form captures: username (required, unique), first name, last name, email, initial password, confirm password. On submit, calls `create#UserAccount`. | Must |
| **FR-ADM-05** | **Edit User:** Admin can edit first name, last name, email address. Username is immutable after creation. Calls `update#UserAccount`. | Must |
| **FR-ADM-06** | **Reset Password:** Admin enters a new password + confirm. Calls a service that updates the user's password without requiring the old password. | Must |
| **FR-ADM-07** | **Role Assignment:** Detail pane shows checkboxes for each role: TRADE_MAKER, TRADE_CHECKER, TRADE_BACKOFFICE, TRADE_ADMIN. Toggling creates/removes `UserGroupMember` records. A user can hold multiple roles simultaneously. | Must |
| **FR-ADM-08** | **Authority Tier Config:** The existing delegation tier, custom limit, and currency fields remain in the detail pane (migrated from `UserAuthorityManager`). | Must |
| **FR-ADM-09** | **Enable/Disable Account:** A toggle switch in the detail pane. Disabled accounts cannot log in. Calls `enable#UserAccount` / `disable#UserAccount`. | Must |
| **FR-ADM-10** | An admin cannot disable their own account. The toggle is hidden or greyed out for the currently logged-in admin. | Must |

---

## 6. Backend Service Requirements

| ID | REST Endpoint | Method | Service | Access |
|:---|:-------------|:-------|:--------|:-------|
| **FR-SVC-01** | `/current-user` | GET | `trade.UserAccountServices.get#CurrentUser` — returns session user info + roles + authority tier | Authenticated |
| **FR-SVC-02** | `/logout` | POST | Wraps `ec.user.logoutUser()` — invalidates server session | Authenticated |
| **FR-SVC-03** | `/users` | GET | `trade.UserAccountServices.get#UserList` — lists all trade finance users with roles | Admin only |
| **FR-SVC-04** | `/users/{userId}` | GET | `trade.UserAccountServices.get#UserDetail` — single user full profile + roles + authority | Admin only |
| **FR-SVC-05** | `/users` | POST | `trade.UserAccountServices.create#TradeUser` — wraps `org.moqui.impl.UserServices.create#UserAccount` + role assignment | Admin only |
| **FR-SVC-06** | `/users/{userId}` | POST | `trade.UserAccountServices.update#TradeUser` — wraps `org.moqui.impl.UserServices.update#UserAccount` | Admin only |
| **FR-SVC-07** | `/users/{userId}/reset-password` | POST | `trade.UserAccountServices.reset#UserPassword` — admin resets password without old password | Admin only |
| **FR-SVC-08** | `/users/{userId}/roles` | POST | `trade.UserAccountServices.update#UserRoles` — sets role assignments (add/remove `UserGroupMember`) | Admin only |
| **FR-SVC-09** | `/users/{userId}/status` | POST | `trade.UserAccountServices.update#UserStatus` — enable/disable account | Admin only |
| **FR-SVC-10** | `/change-password` | POST | `trade.UserAccountServices.change#OwnPassword` — wraps `org.moqui.impl.UserServices.update#Password` | Authenticated |

---

## 7. Traceability Matrix

### User Stories → Functional Requirements

| User Story | Functional Requirements |
|:-----------|:-----------------------|
| US-01 (Login) | FR-AUTH-01, FR-AUTH-02, FR-AUTH-03, FR-AUTH-04, FR-AUTH-05 |
| US-02 (View Profile) | FR-PROF-01, FR-SVC-01 |
| US-03 (Change Password) | FR-PROF-02, FR-PROF-03, FR-PROF-04, FR-SVC-10 |
| US-04 (Logout) | FR-AUTH-06, FR-PROF-05, FR-SVC-02 |
| US-05 (Create User) | FR-ADM-01, FR-ADM-03, FR-ADM-04, FR-SVC-03, FR-SVC-05 |
| US-06 (Edit User) | FR-ADM-02, FR-ADM-05, FR-SVC-04, FR-SVC-06 |
| US-07 (Reset Password) | FR-ADM-06, FR-SVC-07 |
| US-08 (Assign Roles) | FR-ADM-07, FR-SVC-08 |
| US-09 (Enable/Disable) | FR-ADM-09, FR-ADM-10, FR-SVC-09 |

### Existing Requirement Cross-References

| This Module | Relates To | Impact |
|:-----------|:-----------|:-------|
| FR-AUTH-01 (Session required) | REQ-COM-VAL-02 (Four-Eyes Principle) | Enables user identity tracking for Maker/Checker segregation |
| FR-ADM-07 (Role Assignment) | REQ-COM-AUTH-01 (Delegation Tiers) | Roles determine which tier a user operates in |
| FR-ADM-08 (Authority Tier) | REQ-COM-MAS-02 (User Authority Tiers) | Direct migration of existing functionality |
| FR-ADM-09 (Disable Account) | REQ-COM-MAS-02 (Suspended Accounts) | Implements the "remove from routing matrix" rule |
