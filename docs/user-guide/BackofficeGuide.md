# Backoffice & System Admin Guide: Digital Trade Finance

This guide is intended for Administrative and Backoffice users responsible for system configuration, user management, and audit integrity.

---

## 1. Access Credentials (Development/UAT)

| Role | Username | Password | Purpose |
|------|----------|----------|---------|
| **Maker** | `trade.maker` | `trade123` | Transaction Issuance |
| **Checker** | `trade.checker` | `trade123` | Transaction Authorization |
| **Backoffice** | `trade.backoffice` | `trade123` | Facility Management |
| **System Admin** | `trade.admin` | `trade123` | User/Audit Management |

---

Backoffice users manage the core ecosystem through the **Master Data** module.

### Party & KYC Directory
- **KYC Status Monitoring**: Track compliance tiers and AML clearance for all parties.
- **Role Assignment**: Define parties as Applicants, Beneficiaries, or Advising Banks.

### Credit Facilities Workspace
- **Limit Configuration**: Define `totalApprovedLimit` and monitoring thresholds.
- **Exposure Dashboard**: Access real-time visualization of `utilizedAmount` (Firm vs Contingent) across the bank's trade portfolio.

### Tariff & Fee Mapping
- **MT610 SWIFT Mapping**: Align system charge codes with international SWIFT standards.
- **Pricing Matrix**: Configure multi-tier fee structures with automated approval workflows.

---

## 3. System Administration

### User & Security Management
System Admins manage the Four-Eyes principle enforcement:
- Assigning users to `TRADE_MAKER`, `TRADE_CHECKER`, or `TRADE_BACKOFFICE` groups.
- Monitoring for "Self-Authorization" attempts in security logs.

### Audit Trail Review
The platform maintains an immutable audit trail for every state change:
- **Location**: `moqui.trade.instrument.TradeTransactionAudit` entity.
- **Traceability**: Every record captures `userId`, `actionEnumId` (e.g., `MAKER_COMMIT`, `CHECKER_APPROVE`), and the `auditTimestamp`.
- **Purpose**: Ensuring compliance with international trade regulations and internal bank policies.

### Technical Health
System Admins access the standard Moqui toolset (`/vroot/tools`) for:
- Cache management.
- Service job scheduling (e.g., for end-of-day batch limit resets).
- Entity data exports for regulatory reporting.
