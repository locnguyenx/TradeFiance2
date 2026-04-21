---
Document ID: BDD-SWIFT-NOTIF
Version: 0.1
System: Trade Finance
Module: Letter of Credit (LC)
BRD ID: BRD-002
Status: DRAFT
Last Updated: 2026-03-12
Author: [Antigravity]
---

# BDD Scenarios: SWIFT Messaging & Notifications - Reversed

**Requirement:** SWIFT Integration (MT700, MT707), System Notifications (R8.3-UC6)

This document contains BDD scenarios reversed from the implemented test cases in `TradeFinanceSwiftSpec.groovy` and `TradeFinanceNotificationSpec.groovy`.

## Feature: SWIFT Messaging & Notifications

**As a** Trade Finance System
**I want to** generate and parse high-quality SWIFT messages and notify users of critical events
**So that** communication with international banks and internal staff is efficient and error-free

### Scenario: Generating and Validation of MT700 (Happy Path)
**Given** an issued LC exists
**When** the system generates a SWIFT **MT700** message
**Then** the message should contain the correct mandatory tags (e.g., `:20:`, `:32B:`)
**And** the data in the tags must accurately reflect the values stored in the **LetterOfCredit** record.

### Scenario: Parsing an MT700 Message (Data Integrity)
**Given** a valid SWIFT MT700 message string
**When** the system parses the message using the **"parse#SwiftMt700"** service
**Then** the resulting data map should contain the correct field values (e.g., LC Number, Amount) to allow for system ingestion or comparison.

### Scenario: System Notifications for LC Events (Happy Path)
**Given** an LC is being processed (e.g., Created or Issued)
**When** a significant event occurs
**Then** the system should automatically record a **Notification** entry in **LcHistory**
**And** the system should generate a **NotificationMessage** for the target users (e.g., CSR, Applicant).

---

## Business Rules & Validation
1. **SWIFT Tags:** Generated messages MUST conform to bank-standard SWIFT formatting rules for Tag 20 (Reference), Tag 32B (Amount), etc.
2. **Read-Only Trail:** SWIFT messages are logged as read-only **LcDocument** records for audit trail purposes.
3. **User-Linked Notifications:** System notifications MUST be delivered to the correct `userId` based on their party role associated with the LC.
4. **Data Accuracy:** Parsing a message and then re-generating it should yield consistent results for core financial fields.
