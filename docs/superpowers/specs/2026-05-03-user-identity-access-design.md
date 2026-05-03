# Technical Design Spec: User Identity & Access Management

**Project Name:** Digital Trade Finance Platform
**Module:** User Identity & Access Management
**Date:** May 3, 2026

## 1. System Architecture

The module follows a standard Client-Server architecture, leveraging Moqui's native session-based authentication.

### 1.1 Backend Component (Moqui)

#### Entity Model Extensions
While we primarily use Moqui's internal `UserAccount` and `UserGroupMember`, we maintain the `UserAuthorityProfile` to store trade-specific metadata.

| Entity | Fields Used |
|:-------|:------------|
| `moqui.security.UserAccount` | `userId`, `username`, `firstName`, `lastName`, `emailAddress`, `disabled` |
| `moqui.security.UserGroupMember` | `userId`, `userGroupId`, `fromDate` |
| `trade.UserAuthorityProfile` | `userId`, `delegationTierId`, `customLimit`, `currencyUomId`, `isSuspended` |

#### Service Layer (`trade.UserAccountServices`)
New services implemented in `runtime/component/TradeFinance/service/trade/UserAccountServices.xml`:

- **`get#CurrentUser`**: 
  - Retrieves data from `ec.user.userAccount`.
  - Joins with `UserGroupMember` and `UserGroup` to get roles.
  - Joins with `UserAuthorityProfile` to get limits.
- **`get#UserList`**: 
  - Admin only.
  - Returns all users with `TRADE_*` roles.
- **`create#TradeUser`**: 
  - Wraps `org.moqui.impl.UserServices.create#UserAccount`.
  - Automatically creates a default `UserAuthorityProfile`.
- **`update#UserRoles`**: 
  - Input: `userId`, `roleList` (Array of group IDs).
  - Logic: Compares current roles with input, calls `create#UserGroupMember` or `update#UserGroupMember` (thruDate) as needed.
- **`reset#UserPassword`**:
  - Admin only.
  - Calls `org.moqui.impl.UserServices.update#PasswordInternal` with `requireOldPassword=false`.

#### REST API (`trade.rest.xml`)
New endpoints added to the `/trade` resource path:

```xml
<resource name="login">
    <method type="post"><service name="org.moqui.impl.UserServices.login#UserAccount"/></method>
</resource>
<resource name="logout">
    <method type="post"><service name="trade.UserAccountServices.logout#User"/></method>
</resource>
<resource name="current-user">
    <method type="get"><service name="trade.UserAccountServices.get#CurrentUser"/></method>
</resource>
<resource name="users">
    <method type="get"><service name="trade.UserAccountServices.get#UserList"/></method>
    <method type="post"><service name="trade.UserAccountServices.create#TradeUser"/></method>
    <id name="userId">
        <method type="get"><service name="trade.UserAccountServices.get#UserDetail"/></method>
        <method type="post"><service name="trade.UserAccountServices.update#TradeUser"/></method>
        <resource name="roles">
            <method type="post"><service name="trade.UserAccountServices.update#UserRoles"/></method>
        </resource>
        <resource name="reset-password">
            <method type="post"><service name="trade.UserAccountServices.reset#UserPassword"/></method>
        </resource>
    </id>
</resource>
```

---

### 1.2 Frontend Component (Next.js)

#### Authentication Management (`AuthContext.tsx`)
A Global React Context providing authentication state:

```typescript
interface AuthContextType {
    user: UserProfile | null;
    isAuthenticated: boolean;
    loading: boolean;
    login: (username, password) => Promise<void>;
    logout: () => Promise<void>;
    refresh: () => Promise<void>;
}
```

- **Persistence**: Relies on Moqui's `JSESSIONID` cookie. `tradeApi.ts` will be updated to use `credentials: 'include'`.
- **Initialization**: Calls `getCurrentUser()` on mount. If it receives a 401, sets `isAuthenticated` to false.

#### UI Structure
- **`/login` Page**: Standalone page using a minimal layout. High-fidelity "Blue Premium" login form.
- **`GlobalShell`**:
  - Displays `loading` state during initial session check.
  - Contextually displays User Full Name and Avatar.
  - `ProfilePanel`: A popover or sidebar extension showing roles, tier, "Change Password" form, and "Logout" button.
- **`UserAdminPage` (`/admin/users`)**:
  - Master-detail view.
  - Left: User list with status (Active/Disabled) and role badges.
  - Right: Tabs for "Account Info", "Roles & Authority", "Security (Password)".

---

## 2. Security & Compliance

- **Password Policy**: Enforced by Moqui's `UserFacade` configuration (min 8 chars, 1 digit, 1 special).
- **Session Security**: `HttpOnly` and `Secure` flags on the `JSESSIONID` cookie (if SSL is enabled).
- **Audit Logging**: Every user management action (create, edit, role change, password reset) will be captured by Moqui's entity-audit mechanism (`moqui.entity.EntityAuditLog`).
- **Segregation of Duties**: The login system enables the "Four-Eyes Principle" by providing verifiable `makerUserId` and `checkerUserId` for every transaction.
