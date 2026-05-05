# Behavior Driven Development (BDD) Spec

**Project Name:** Digital Trade Finance Platform
**Module:** User Identity & Access Management
**Date:** May 3, 2026

## 1. Traceability Matrix

| Story ID | Requirement ID | BDD Scenarios |
|:---------|:---------------|:--------------|
| **US-01** | FR-AUTH-01, 02, 04, 05 | Successful Login, Failed Login, Account Lockout |
| **US-02** | FR-PROF-01, FR-SVC-01 | View own profile |
| **US-03** | FR-PROF-03, FR-SVC-10 | User changes own password |
| **US-04** | FR-AUTH-06, FR-PROF-05 | User logs out |
| **US-05** | FR-ADM-04, FR-SVC-05 | Admin creates new user |
| **US-06** | FR-ADM-05, FR-SVC-06 | Admin edits user details |
| **US-07** | FR-ADM-06, FR-SVC-07 | Admin resets user password |
| **US-08** | FR-ADM-07, FR-SVC-08 | Admin assigns/removes roles |
| **US-09** | FR-ADM-09, FR-SVC-09 | Admin disables/enables account |

---

## 2. Scenarios

### Feature: Authentication & Session Management

**Scenario: Successful Login** (Happy Path)
* **Given** the user is on the Login page
* **When** they enter a valid username "trade.maker" and password "trade123"
* **And** click "Sign In"
* **Then** they should be redirected to the "Operations Dashboard"
* **And** the sidebar should display "Trade Maker" (firstName + lastName)

**Scenario: Failed Login with Invalid Credentials** (Edge Case)
* **Given** the user is on the Login page
* **When** they enter an invalid username or wrong password
* **And** click "Sign In"
* **Then** they should remain on the Login page
* **And** see an error message "Invalid username or password"

**Scenario: Account Lockout** (Edge Case)
* **Given** the user has entered a wrong password 3 times
* **When** they attempt a 4th login
* **Then** they should see an error message "Account temporarily locked. Try again later."

**Scenario: Unauthorized Access Redirect** (Edge Case)
* **Given** the user is not logged in
* **When** they try to navigate directly to "/transactions"
* **Then** they should be redirected to "/login"

---

### Feature: User Self-Service

**Scenario: View Own Profile** (Happy Path)
* **Given** the user "trade.maker" is logged in
* **When** they click their avatar in the sidebar footer
* **Then** a profile overlay/panel should appear
* **And** it should show "Trade Maker (trade.maker)"
* **And** it should show roles: "Trade Maker"
* **And** it should show Delegation Tier: "Tier 1"

**Scenario: User Changes Own Password** (Happy Path)
* **Given** the user is logged in and viewing their profile
* **When** they enter their current password, a valid new password, and confirm it
* **And** click "Update Password"
* **Then** they should see a success notification "Password updated successfully"
* **And** their server-side password should be updated

**Scenario: User Logs Out** (Happy Path)
* **Given** a user is logged in
* **When** they click "Logout" in the profile panel
* **Then** their server session should be invalidated
* **And** they should be redirected to the Login page
* **And** the sidebar should no longer be visible

---

### Feature: Admin User Administration

**Scenario: Admin Creates New User** (Happy Path)
* **Given** an administrator is on the "User Administration" page
* **When** they click "+ New User"
* **And** enter "john.doe", "John", "Doe", "john@bank.com", and an initial password
* **And** click "Create User"
* **Then** the new user "John Doe" should appear in the user list
* **And** they should have no roles assigned by default

**Scenario: Admin Assigns Roles to User** (Happy Path)
* **Given** the Admin is editing user "john.doe"
* **When** they check the "Trade Maker" and "Trade Checker" roles
* **And** click "Save Changes"
* **Then** the user "john.doe" should now be a member of "TRADE_MAKER" and "TRADE_CHECKER" groups

**Scenario: Admin Resets User Password** (Happy Path)
* **Given** the Admin is editing user "trade.maker"
* **When** they enter a new password in the "Reset Password" section
* **And** click "Reset Password"
* **Then** the user's password should be updated immediately without requiring their old password

**Scenario: Admin Disables a User Account** (Happy Path)
* **Given** an active user "trade.maker" exists
* **When** the Admin toggles the "Account Status" to Disabled for "trade.maker"
* **Then** "trade.maker" should be immediately disconnected
* **And** unable to log in again until re-enabled

**Scenario: Admin Cannot Disable Own Account** (Edge Case)
* **Given** "trade.admin" is managing users
* **When** they view the details for "trade.admin"
* **Then** the "Account Status" toggle should be disabled (preventing self-lockout)
