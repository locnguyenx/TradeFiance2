# TradeTransaction Entity Design Specification

## Version: 1.1 (Comprehensive)
## Date: April 28, 2026

---

## Table of Contents

1. [Conceptual Model](#1-conceptual-model)
2. [Entity Definitions](#2-entity-definitions)
3. [Business Rules](#3-business-rules)
4. [Processing Flows](#4-processing-flows)
5. [Transaction State Machine](#5-transaction-state-machine)
6. [Data Relationships](#6-data-relationships)
7. [Migration Strategy](#7-migration-strategy)
8. [Impact Analysis](#8-impact-analysis)

---

## 1. Conceptual Model

### 1.1 Core Distinction: Instrument vs Transaction

Based on BRD requirements (REQ-IMP-NOTE-01 through REQ-IMP-NOTE-03, REQ-IMP-DEF-01, REQ-IMP-DEF-02):

| Concept | BRD Term | Entity | Description |
|---------|----------|--------|-------------|
| **Trade Instrument** | Business Object / Financial Product | `TradeInstrument` + Product Entity (ImportLetterOfCredit, etc.) | The financial product itself - LC, Collection - containing terms, amounts, parties |
| **Trade Transaction** | Processing Flow / Transaction | `TradeTransaction` | Each authorization processing execution that goes through Maker/Checker workflow |

### 1.2 Two Parallel Status Flows

| Status Flow | Field | Entity | Tracks | Values |
|-------------|-------|--------|--------|--------|
| **Transaction Status** | `transactionStatusId` | `TradeTransaction` | Authorization workflow (Maker/Checker) | TX_DRAFT, TX_PENDING, TX_APPROVED, TX_REJECTED |
| **Business State** | `businessStateId` | Product Entity (ImportLetterOfCredit) | Product lifecycle | LC_DRAFT, LC_PENDING, LC_ISSUED, LC_DOCS_RECEIVED, LC_DISCREPANT, LC_ACCEPTED, LC_SETTLED, LC_CLOSED, LC_CANCELLED, LC_AMENDMENT_PENDING, LC_HOLD |

### 1.3 Transaction Type Taxonomy

Transaction types follow the pattern `{PRODUCT}_{ACTION}`:

| Transaction Type | Description | Payload Entity | Business State Impact |
|-----------------|-------------|----------------|----------------------|
| `IMP_NEW` | Import LC Issuance | (none - instrument is the payload) | LC_DRAFT → LC_ISSUED |
| `IMP_AMENDMENT` | Import LC Amendment | ImportLcAmendment | Stays at LC_ISSUED or LC_AMENDMENT_PENDING |
| `IMP_PRESENTATION` | Document Presentation | TradeDocumentPresentation | LC_ISSUED → LC_DOCS_RECEIVED |
| `IMP_SETTLEMENT` | Payment/Settlement | ImportLcSettlement | LC_ACCEPTED → LC_SETTLED |
| `IMP_CANCELLATION` | Early Cancellation | (embedded) | LC_ISSUED → LC_CANCELLED |
| `EXP_NEW` | Export LC Issuance | (Future) | - |
| `EXP_AMENDMENT` | Export LC Amendment | (Future) | - |
| `COL_IMP_NEW` | Import Collection | (Future) | - |
| `COL_EXP_NEW` | Export Collection | (Future) | - |

---

## 2. Entity Definitions

### 2.1 TradeInstrument (Revised)

**Purpose:** The financial product record - holds the core terms and current state of the trade instrument.

```xml
<entity entity-name="TradeInstrument" package="trade">
    <!-- Primary Key -->
    <field name="instrumentId" type="id" is-pk="true"/>
    
    <!-- Reference -->
    <field name="transactionRef" type="text-short"/>  <!-- TF-IMP-2026-0001 -->
    
    <!-- Instrument Type -->
    <field name="instrumentTypeEnumId" type="id"/>    <!-- INST_LC, INST_COLLECTION -->
    <field name="productEnumId" type="id"/>           <!-- IMPORT_LC, etc. -->
    
    <!-- Financial Terms (Original Issuance Snapshot) -->
    <field name="amount" type="number-decimal"/>      <!-- Original LC amount -->
    <field name="currencyUomId" type="id"/>            <!-- ISO currency -->
    <field name="outstandingAmount" type="number-decimal"/>  <!-- Remaining balance -->
    <field name="baseEquivalentAmount" type="number-decimal"/>  <!-- Local currency -->
    
    <!-- Dates -->
    <field name="issueDate" type="date"/>             <!-- Original issue date -->
    <field name="expiryDate" type="date"/>            <!-- Expiry date -->
    
    <!-- Parties -->
    <field name="applicantPartyId" type="id"/>        <!-- FK to TradeParty -->
    <field name="beneficiaryPartyId" type="id"/>      <!-- FK to TradeParty -->
    
    <!-- Facility -->
    <field name="customerFacilityId" type="id"/>     <!-- FK to CustomerFacility -->
    
    <!-- Business State (LC/Collection lifecycle) -->
    <field name="businessStateId" type="id"/>         <!-- LC_DRAFT, LC_ISSUED, etc. -->
    <field name="previousBusinessStateId" type="id"/>  <!-- For Hold/Release -->
    
    <!-- SWIFT Fields -->
    <field name="reimbursingBankBic" type="text-short"/>
    <field name="adviseThroughBankBic" type="text-short"/>
    <field name="beneficiaryName" type="text-long"/>
    <field name="senderToReceiverInfo" type="text-long"/>
    <!-- ... other SWIFT fields ... -->
    
    <!-- Audit -->
    <field name="createdStamp" type="date-time"/>
    <field name="lastUpdatedStamp" type="date-time"/>
</entity>
```

### 2.2 TradeTransaction (NEW)

**Purpose:** Authorization processing record - each transaction that goes through Maker/Checker workflow.

```xml
<entity entity-name="TradeTransaction" package="trade">
    <!-- Primary Key -->
    <field name="transactionId" type="id" is-pk="true"/>
    
    <!-- Relationship to Instrument -->
    <field name="instrumentId" type="id"/>             <!-- FK to TradeInstrument -->
    <field name="transactionTypeEnumId" type="id"/>    <!-- IMP_NEW, IMP_AMENDMENT, etc. -->
    
    <!-- Transaction State (Authorization Workflow) -->
    <field name="transactionStatusId" type="id"/>      <!-- TX_DRAFT, TX_PENDING, TX_APPROVED -->
    <field name="transactionDate" type="date"/>       <!-- Business date -->
    
    <!-- Maker (Initiator) -->
    <field name="makerUserId" type="id"/>             <!-- User who created/submitted -->
    <field name="makerTimestamp" type="date-time"/>    <!-- When created -->
    
    <!-- Checker (Approver) -->
    <field name="checkerUserId" type="id"/>            <!-- User who approved -->
    <field name="checkerTimestamp" type="date-time"/>  <!-- When approved -->
    
    <!-- Authorization Details -->
    <field name="rejectionReason" type="text-long"/>  <!-- If rejected -->
    <field name="versionNumber" type="number-integer" default="1"/>  <!-- For dual-checker -->
    <field name="priorityEnumId" type="id"/>          <!-- NORMAL, URGENT, EXPRESS -->
    
    <!-- Payload Link (for Amendment, Presentation, Settlement) -->
    <field name="relatedRecordId" type="id"/>         <!-- AmendmentId, PresentationId, etc. -->
    <field name="relatedRecordType" type="id"/>        <!-- AMENDMENT, PRESENTATION, SETTLEMENT -->
    
    <!-- Audit -->
    <field name="createdStamp" type="date-time"/>
    <field name="lastUpdatedStamp" type="date-time"/>
    
    <relationship type="one" related="trade.TradeInstrument">
        <key-map field-name="instrumentId"/>
    </relationship>
</entity>
```

### 2.3 TradeApprovalRecord

**Purpose:** Track individual approval actions for dual-checker (Tier 4) transactions.

```xml
<entity entity-name="TradeApprovalRecord" package="trade">
    <field name="approvalRecordId" type="id" is-pk="true"/>
    <field name="transactionId" type="id"/>           <!-- FK to TradeTransaction -->
    <field name="instrumentId" type="id"/>
    <field name="approverUserId" type="id"/>
    <field name="approvalTimestamp" type="date-time"/>
    <field name="approvalComments" type="text-long"/>
    <field name="versionSnapshot" type="number-integer"/>
    <field name="approvalDecision" type="id"/>         <!-- APPROVED, REJECTED -->
    
    <relationship type="one" related="trade.TradeTransaction">
        <key-map field-name="transactionId"/>
    </relationship>
    <relationship type="one" related="trade.TradeInstrument">
        <key-map field-name="instrumentId"/>
    </relationship>
</entity>
```

### 2.4 TradeTransactionAudit

**Purpose:** Immutable audit log for all transaction actions.

```xml
<entity entity-name="TradeTransactionAudit" package="trade">
    <field name="auditId" type="id" is-pk="true"/>
    <field name="transactionId" type="id"/>
    <field name="instrumentId" type="id"/>
    <field name="timestamp" type="date-time"/>
    <field name="userId" type="id"/>
    <field name="actionEnumId" type="id"/>             <!-- SUBMIT, APPROVE, REJECT, etc. -->
    <field name="requestIpAddress" type="text-short"/>
    <field name="changedFieldName" type="text-medium"/>
    <field name="oldValue" type="text-long"/>
    <field name="newValue" type="text-long"/>
    <field name="justificationRootText" type="text-long"/>
    <field name="snapshotDeltaJSON" type="text-very-long"/>
    
    <relationship type="one" related="trade.TradeTransaction">
        <key-map field-name="transactionId"/>
    </relationship>
</entity>
```

### 2.5 Existing Product Entities (Unchanged Structure)

These entities remain as-is, storing the domain-specific data:

| Entity | Purpose |
|--------|---------|
| `ImportLetterOfCredit` | Import LC specific fields (terms, tenor, documents, effective values) |
| `ImportLcAmendment` | Amendment details (amount adjustment, new expiry, beneficiary consent) |
| `TradeDocumentPresentation` | Document presentation (claim amount, presenting bank, discrepancies) |
| `ImportLcSettlement` | Settlement details (value date, FX rate, debit account) |
| `ImportLcShippingGuarantee` | Shipping guarantee details |

### 2.6 Supporting Entities (Reference)

| Entity | Purpose |
|--------|---------|
| `TradeParty` | Customer/counterparty with KYC, sanctions status |
| `CustomerFacility` | Credit limit with utilized/available amounts |
| `TradeProductCatalog` | Product configuration matrix |
| `FeeConfiguration` | Tariff rules for fees |
| `UserAuthorityProfile` | User authority tiers and limits |
| `NumberSequence` | Reference number generation |

---

## 3. Business Rules

### 3.1 Authorization Rules

#### 3.1.1 Four-Eyes Principle (REQ-COM-VAL-02)

**Rule:** The user who initiates (Maker) cannot be the same user who approves (Checker).

**Implementation:**
```groovy
// In AuthorizationServices.evaluate#MakerCheckerMatrix
if (tx.makerUserId == userId) {
    return [isAuthorized: false, reason: "Four-Eyes violation: Maker cannot be Checker"]
}
```

#### 3.1.2 Approval Authority Tiers (REQ-COM-AUTH-01)

| Tier | User Group | Maximum Approval Limit (Base Equivalent) |
|------|-----------|------------------------------------------|
| Tier 1 | Senior Trade Operations Officer | Up to $100,000 |
| Tier 2 | Trade Finance Team Lead | Up to $1,000,000 |
| Tier 3 | Head of Trade Operations | Up to $5,000,000 |
| Tier 4 | Credit Risk Committee / Board | Above $5,000,000 |

**Implementation:** UserAuthorityProfile.delegationTierId + customLimit determines tier.

#### 3.1.3 Dual-Checker for Tier 4 (REQ-COM-AUTH-02)

**Rule:** Transactions requiring Tier 4 authority require TWO distinct Checkers to approve.

**Implementation:**
```groovy
// When evaluating Tier 4 transaction:
def existingApprovals = findTradeApprovalRecord where 
    transactionId = tx.transactionId and 
    versionSnapshot = tx.versionNumber

if (existingApprovals.size() >= 1) {
    // First approval already exists
    // Must be different user from first approver
    if (existingApprovals[0].approverUserId == currentUser) {
        return [isAuthorized: false, reason: "Dual approval requires different checker"]
    }
    // This is second approval - mark as final
    tx.transactionStatusId = TX_APPROVED
} else {
    // First approval - keep pending for second
    tx.transactionStatusId = TX_PENDING
}
```

#### 3.1.4 Downward Delegation Restriction (REQ-COM-AUTH-02)

**Rule:** A Tier 1 user cannot approve a transaction requiring Tier 2 authority.

**Implementation:**
```groovy
def requiredTier = getRequiredTier(evaluationAmount)
def userTier = getUserTier(userId)

if (userTier < requiredTier) {
    return [isAuthorized: false, reason: "Insufficient authority tier"]
}
```

### 3.2 Risk & Compliance Rules

#### 3.2.1 KYC Validation (REQ-COM-VAL-01)

**Rule:** Block transaction if Applicant's KYC status is "Expired".

**Implementation:**
```groovy
def applicant = findTradeParty(partyId: instrument.applicantPartyId)
if (applicant?.kycStatus == 'Expired') {
    return [isValid: false, reason: "Applicant KYC expired"]
}
```

#### 3.2.2 Sanctions Hold (REQ-COM-VAL-01)

**Rule:** If any party matches sanctions watchlist, route to Compliance.

**Implementation:**
```groovy
def sanctionsCheck = performSanctionsScreening(instrument)
if (sanctionsCheck.isHit) {
    instrument.businessStateId = LC_HOLD
    sendComplianceAlert(instrument)
    return [isHold: true]
}
```

#### 3.2.3 Limit Availability (REQ-COM-VAL-01)

**Rule:** Block if Base Equivalent Amount exceeds Available Earmark.

**Implementation:**
```groovy
def facility = findCustomerFacility(facilityId: instrument.customerFacilityId)
def available = facility.totalApprovedLimit - facility.utilizedAmount
if (baseEquivalentAmount > available) {
    return [isValid: false, reason: "Insufficient facility limit"]
}
```

### 3.3 Business Logic Rules

#### 3.3.1 Date Sequence (REQ-COM-VAL-03)

**Rule:** Expiry Date must be >= Issue Date.

**Implementation:** Enforced at entity level + validation service.

#### 3.3.2 Back-Valuation Restriction (REQ-COM-VAL-03)

**Rule:** Issue Date cannot be in the past without admin override.

**Implementation:**
```groovy
if (issueDate < today && !hasAdminOverride(userId)) {
    return [isValid: false, reason: "Issue date cannot be in past"]
}
```

#### 3.3.3 Amendment Financial vs Non-Financial (REQ-COM-AUTH-03)

**Rule:** Financial amendments use new total liability for tier calculation; non-financial default to Tier 1.

**Implementation:**
```groovy
if (amendment.isFinancial == 'Y') {
    def newTotal = originalAmount + amountAdjustment
    requiredTier = getRequiredTier(newTotal)
} else {
    requiredTier = 'TIER_1'  // Non-financial defaults to Tier 1
}
```

### 3.4 SLA Rules (REQ-COM-SLA-02)

#### 3.4.1 Document Examination Window

**Rule:** Maximum 5 banking days for document examination (UCP 600).

**Implementation:**
```groovy
def presentationDate = presentation.presentationDate
def regulatoryDeadline = calculateBusinessDate(presentationDate, 5, 'ADD')
def daysElapsed = calculateBusinessDays(presentationDate, today)

if (daysElapsed >= 3) {
    setSlaWarningFlag(presentation, true)
    sendSlaAlert(presentation)
}
if (daysElapsed >= 5) {
    setSlaBlockedFlag(presentation, true)
    blockFurtherAction(presentation)
}
```

---

## 4. Processing Flows

### 4.1 Standardized Processing Flow (REQ-COM-WF-01)

Each processing flow consists of these steps:

| Step | Transaction State | Actions |
|------|-------------------|---------|
| 1. Initiation | TX_DRAFT | Create TradeTransaction with payload, save data |
| 2. Pre-Validation | TX_DRAFT | Run validate#Submission (KYC, limits, sanctions) |
| 3. Submit | TX_PENDING | Move to Checker queue |
| 4. Review | TX_PENDING | Checker reviews, validates authorization |
| 5. Approve/Reject | TX_APPROVED / TX_DRAFT | Execute or return to Maker |
| 6. Post-Execution | TX_APPROVED | Update limits, generate SWIFT, notify |

### 4.2 LC Issuance Flow (IMP_NEW)

```
User Action              TradeTransaction          ImportLetterOfCredit
    |                         |                           |
Create LC -----> TX_DRAFT, IMP_NEW -----> LC_DRAFT
    |                         |                           |
Submit --------> TX_PENDING ---> LC_PENDING
    |                         |                           |
Checker Approves -> TX_APPROVED -----+
    |                         |               |
    |                         |               v
    |                    (limit update) ----> LC_ISSUED
    |                         |               |
    |                         |          (MT700 sent)
    v                         v               v
                  COMPLETE                 COMPLETE
```

### 4.3 Amendment Flow (IMP_AMENDMENT)

```
User Action            TradeTransaction          ImportLetterOfCredit    ImportLcAmendment
    |                       |                           |                        |
Initiate Amendment --> TX_DRAFT, IMP_AMENDMENT -> LC_AMENDMENT_PENDING      AMEND_DRAFT
    |                       |                           |                        |
Submit ---------> TX_PENDING -------------------------+------------------------+
    |                       |                           |                        |
Checker Approves -> TX_APPROVED ---------------------+                        |
    |                       |                           |                        |
Beneficiary Consent -> (update amendment)                                      AMEND_COMMITTED
    |                       |                           |                        |
    |                       |                   LC_ISSUED <---------------------+
    v                       v                           v
                  COMPLETE                       COMPLETE
```

### 4.4 Presentation Flow (IMP_PRESENTATION)

```
User Action            TradeTransaction          ImportLetterOfCredit    TradeDocumentPresentation
    |                       |                           |                        |
Log Documents ----> TX_DRAFT, IMP_PRESENTATION -> LC_DOCS_RECEIVED         PRES_RECEIVED
    |                       |                           |                        |
Examine ---------> TX_PENDING -------------------------+-----------------------> PRES_EXAMINED
    |                       |                           |                        |
    |                       |                           |                        |
    v                       |                           |                        v
                  TX_APPROVED                          |                 (clean/discrepant)
    |                       |                           |                        |
    |                       |                   LC_ACCEPTED or LC_DISCREPANT
    v                       v                           v
                  COMPLETE                       COMPLETE
```

### 4.5 Settlement Flow (IMP_SETTLEMENT)

```
User Action            TradeTransaction          ImportLetterOfCredit    ImportLcSettlement
    |                       |                           |                        |
Initiate Settlement -> TX_DRAFT, IMP_SETTLEMENT -> LC_SETTLING (internal)    SETTLE_PENDING
    |                       |                           |                        |
Submit ---------> TX_PENDING -------------------------+------------------------+
    |                       |                           |                        |
Checker Approves -> TX_APPROVED ---------------------+                        |
    |                       |                           |                        |
    |                   (limit release)                                         |
    |                   (account debit) ------------------------->              |
    |                   (MT202/103) ----------------------------------------->
    |                       |                           |                        |
    |                       |                   (revolving if applicable)       |
    |                       |                           |               SETTLE_EXECUTED
    v                       v                           v
                  COMPLETE              LC_SETTLED or LC_ISSUED (partial)
```

---

## 5. Transaction State Machine

### 5.1 Transaction Status Values (TX_*)

| Status | Description | Transitions To |
|--------|-------------|----------------|
| TX_DRAFT | Initial creation, can be edited | TX_PENDING (submit), TX_CANCELLED (discard) |
| TX_PENDING | Submitted for approval | TX_APPROVED (approve), TX_DRAFT (reject/return) |
| TX_APPROVED | Authorized, execution complete | TX_CANCELLED (reverse), (new TX for next action) |
| TX_REJECTED | Declined by Checker | TX_DRAFT (resubmit after fix) |

### 5.2 Business State Values (LC_*)

| Status | Description | Instrument Lifecycle |
|--------|-------------|---------------------|
| LC_DRAFT | Initial data entry | Active |
| LC_PENDING | Awaiting approval | Active |
| LC_ISSUED | Approved and active | Active |
| LC_AMENDMENT_PENDING | Amendment awaiting beneficiary consent | Active |
| LC_DOCS_RECEIVED | Documents received, pending examination | Active |
| LC_DISCREPANT | Documents have discrepancies | Active |
| LC_ACCEPTED | Clean documents or waived | Active |
| LC_SETTLED | Payment completed | Active |
| LC_CLOSED | Terminal - fully drawn/expired | Terminated |
| LC_CANCELLED | Terminal - rejected/early close | Terminated |
| LC_HOLD | Compliance hold | Active |

### 5.3 Transaction Status Transitions

| Current State | Event | New State | Actions |
|--------------|-------|-----------|---------|
| TX_DRAFT | User submits | TX_PENDING | Send to checker queue |
| TX_DRAFT | User discards | TX_CANCELLED | Soft delete |
| TX_PENDING | Checker approves | TX_APPROVED | Execute post-auth, update business state |
| TX_PENDING | Checker rejects | TX_DRAFT | Return to maker with reason |
| TX_PENDING | Checker returns to maker | TX_DRAFT | Same as reject |
| TX_APPROVED | (New action starts) | TX_DRAFT | New transaction for next action |

---

## 6. Data Relationships

### 6.1 Entity Relationship Diagram

```
┌─────────────────────┐         1:n         ┌─────────────────────┐
│   TradeInstrument  │─────────────────────│  TradeTransaction   │
├─────────────────────┤                     ├─────────────────────┤
│ - instrumentId (PK)│                     │ - transactionId (PK)│
│ - transactionRef   │                     │ - instrumentId (FK) │
│ - businessStateId  │                     │ - transactionType   │
│ - amount, currency │                     │ - transactionStatus │
│ - applicantPartyId │                     │ - makerUserId        │
│ - beneficiaryPartyId                     │ - checkerUserId      │
│ - customerFacilityId│                     │ - versionNumber     │
└─────────────────────┘                     │ - relatedRecordId   │
       │                                    └─────────────────────┘
       │ 1:1                                         │ n:1
       │                                    ┌────────┴─────────┐
       ▼                                    ▼                 ▼
┌─────────────────────┐         ┌─────────────────┐   ┌───────────────┐
│ImportLetterOfCredit │         │TradeApprovalRecord   │TradeTransaction│
├─────────────────────┤         ├─────────────────┤    Audit      │
│ - instrumentId (PK)│         │- approvalRecordId    │               │
│ - businessStateId   │         │- transactionId (FK) │               │
│ - effectiveAmount   │         │- approverUserId     │               │
│ - tenorTypeId       │         │- versionSnapshot    │               │
│ - beneficiaryName   │         └─────────────────┘               │
└─────────────────────┘
       │
       │ 1:n
       ▼
┌──────────────┐  ┌──────────────────┐  ┌─────────────────┐
│ImportLcAmendmt│  │TradeDocumentPres │  │ImportLcSettlement
├──────────────┤  ├──────────────────┤  ├─────────────────┤
│- amendmentId  │  │- presentationId  │  │- settlementId   │
│- instrumentId│  │- instrumentId    │  │- instrumentId  │
│- amountAdj   │  │- claimAmount     │  │- principalAmt  │
│- beneficiary │  │- discrepancies[] │  │- settlementType │
│  Consent     │  │                  │  │                 │
└──────────────┘  └──────────────────┘  └─────────────────┘
```

### 6.2 Foreign Key Relationships

| Parent Entity | Child Entity | Relationship | On Delete |
|--------------|--------------|--------------|------------|
| TradeInstrument | TradeTransaction | 1:N | CASCADE |
| TradeInstrument | ImportLetterOfCredit | 1:1 | CASCADE |
| TradeTransaction | TradeApprovalRecord | 1:N | CASCADE |
| TradeTransaction | TradeTransactionAudit | 1:N | CASCADE |
| ImportLetterOfCredit | ImportLcAmendment | 1:N | CASCADE |
| ImportLetterOfCredit | TradeDocumentPresentation | 1:N | CASCADE |
| ImportLetterOfCredit | ImportLcSettlement | 1:N | CASCADE |
| TradeParty | TradeInstrument | 1:N | RESTRICT |
| CustomerFacility | TradeInstrument | 1:N | RESTRICT |

---

## 7. Migration Strategy

### 7.1 Data Migration Script

```sql
-- Step 1: Insert TradeTransaction records for existing instruments
INSERT INTO trade_transaction (
    transaction_id, instrument_id, transaction_type_enum_id,
    transaction_status_id, transaction_date,
    maker_user_id, maker_timestamp,
    checker_user_id, checker_timestamp,
    rejection_reason, version_number, priority_enum_id,
    created_stamp, last_updated_stamp
)
SELECT 
    ti.instrument_id,
    ti.instrument_id,
    ti.instrument_type_enum_id,  -- Will map to IMP_NEW, etc.
    ti.transaction_status_id,
    ti.transaction_date,
    ti.maker_user_id,
    ti.maker_timestamp,
    ti.checker_user_id,
    ti.checker_timestamp,
    ti.rejection_reason,
    COALESCE(ti.version_number, 1),
    ti.priority_enum_id,
    ti.created_stamp,
    ti.last_updated_stamp
FROM trade_instrument ti
WHERE ti.instrument_type_enum_id = 'IMPORT_LC';

-- Step 2: Map instrument_type to transaction_type
UPDATE trade_transaction 
SET transaction_type_enum_id = 'IMP_NEW'
WHERE instrument_id IN (
    SELECT instrument_id FROM trade_instrument 
    WHERE instrument_type_enum_id = 'IMPORT_LC'
);
```

### 7.2 Post-Migration Validation

```sql
-- Verify migration
SELECT 
    (SELECT COUNT(*) FROM trade_instrument) as instrument_count,
    (SELECT COUNT(*) FROM trade_transaction) as transaction_count,
    (SELECT COUNT(*) FROM trade_instrument ti 
     LEFT JOIN trade_transaction tt ON ti.instrument_id = tt.instrument_id 
     WHERE tt.transaction_id IS NULL) as missing_transactions;

-- Verify authorization still works
SELECT tt.transaction_status_id, COUNT(*) 
FROM trade_transaction tt 
GROUP BY tt.transaction_status_id;
```

### 7.3 Rollback Plan

1. Keep backup of trade_instrument fields before migration
2. If issues, reverse: update instrument fields from transaction, then drop transactions
3. Use feature flag to toggle between old (direct instrument) and new (transaction-based) paths

---

## 8. Impact Analysis

### 8.1 Services to Update

| Service File | Changes Required |
|-------------|------------------|
| ImportLcServices.xml | Create TradeTransaction on LC creation, amendment, presentation, settlement |
| ImportLcValidationServices.xml | Validate against TradeTransaction status |
| AuthorizationServices.xml | Accept transactionId, check maker/checker from TradeTransaction |
| LimitServices.xml | Update utilization based on approved transactions only |
| SwiftGenerationServices.xml | Generate SWIFT only when TradeTransaction is TX_APPROVED |
| TradeFinanceSeca.xml | Trigger on TradeTransaction status change |
| TradeAccountingServices.xml | Post accounting entries only for TX_APPROVED |

### 8.2 Enums to Add

| Enum Type | Values to Add |
|-----------|---------------|
| TradeTransactionType | IMP_NEW, IMP_AMENDMENT, IMP_PRESENTATION, IMP_SETTLEMENT, IMP_CANCELLATION, EXP_NEW, EXP_AMENDMENT, COL_IMP_NEW, COL_EXP_NEW |

### 8.3 API Impact

| Endpoint | Impact |
|----------|--------|
| GET /import-lcs | Add filter by transactionStatusId |
| POST /import-lc/approve | Now requires transactionId |
| GET /approvals | Query TradeTransaction instead of TradeInstrument |

---

## 9. Validation Checklist

Before marking implementation complete:

- [ ] TradeTransaction entity created with all fields
- [ ] TransactionType enums seeded
- [ ] TradeInstrument migration fields removed (lifecycleStatusId, maker/checker fields)
- [ ] ImportLcServices creates transaction on each flow
- [ ] AuthorizationServices uses transaction for maker/checker
- [ ] Dual-checker still works (versionNumber on transaction)
- [ ] Limit checking works with new model
- [ ] SWIFT generation triggers on TX_APPROVED
- [ ] Audit logging still captures all actions
- [ ] All existing tests pass

---

## Appendix A: TransactionType Mapping

| Product | Transaction Type | Business State Before | Business State After |
|---------|------------------|----------------------|---------------------|
| Import LC | IMP_NEW | LC_DRAFT | LC_ISSUED |
| Import LC | IMP_AMENDMENT | LC_ISSUED/LC_AMENDMENT_PENDING | LC_ISSUED |
| Import LC | IMP_PRESENTATION | LC_ISSUED | LC_DOCS_RECEIVED → LC_DISCREPANT/LC_ACCEPTED |
| Import LC | IMP_SETTLEMENT | LC_ACCEPTED | LC_SETTLED |
| Import LC | IMP_CANCELLATION | LC_ISSUED | LC_CANCELLED |

---

**Document Version:** 1.1
**Date:** April 28, 2026
**Status:** Comprehensive - Ready for Implementation