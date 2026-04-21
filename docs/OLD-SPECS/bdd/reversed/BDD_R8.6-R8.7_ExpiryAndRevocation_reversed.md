---
Document ID: BDD-R8.6-R8.7-Lifecycle
Version: 0.1
System: Trade Finance
Module: Letter of Credit (LC)
BRD ID: BRD-002
Status: DRAFT
Last Updated: 2026-03-12
Author: [Antigravity]
---

# BDD Scenarios: LC Expiry & Revocation (R8.6-R8.7) - Reversed

**Requirement:** R8.6 (LC Expiry), R8.7 (LC Revocation)

This document contains BDD scenarios reversed from the implemented test cases in `TradeFinanceLifecycleSpec.groovy`.

## Feature: LC Expiry & Revocation

**As a** Trade Finance System
**I want to** handle the closure of LCs due to expiration or revocation
**So that** liabilties are released and parties are notified correctly

### Background
The system manages active LCs with specific expiry dates and forms of credit (Revocable/Irrevocable).

### Scenario: Automated LC Expiry (Happy Path)
**Given** an active LC has reached its **expiryDate**
**When** the scheduled system job **"check#LcExpiry"** is executed
**Then** the system should automatically transition the **lcStatusId** to **Expired** (`LcLfExpired`)
**And** the system should release any remaining provision holds (Mock CBS release).

### Scenario: Revoking a Revocable LC (Happy Path)
**Given** an LC is configured as **Revocable** (`LC_FORM_REVOCABLE`)
**When** the authorized operator calls the **"revoke#LetterOfCredit"** service
**Then** the system should transition the **lcStatusId** to **Revoked** (`LcLfRevoked`)
**And** the system should release any active provisions
**And** the system should generate a SWIFT **MT799** (Free Format Message) to notify the relevant parties.

### Scenario: Preventing Revocation of Irrevocable LC (Business Rule)
**Given** an LC is configured as **Irrevocable** (`LC_FORM_IRREVOCABLE`)
**When** a user attempts to call the **"revoke#LetterOfCredit"** service
**Then** the system should reject the request with an error message
**And** the LC status should remain unchanged.

---

## Business Rules & Validation
1. **Automation:** Expiry MUST be processed by a scheduled job without manual intervention.
2. **Irrevocability:** Most LCs under UCP 600 are irrevocable and cannot be revoked without the consent of all parties. The system must enforce this strictly.
3. **Provision Release:** Any termination of the LC (Expiry or Revocation) MUST trigger the release of held funds in the CBS.
4. **Notification:** Termination events should generate appropriate SWIFT messages (e.g., MT799) or system notifications to stakeholders.
