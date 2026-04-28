# TradeTransaction Entity Design Specification

## 1. Conceptual Model

### 1.1 Instrument vs Transaction Separation

Based on BRD analysis (REQ-IMP-NOTE-01, REQ-IMP-NOTE-03, REQ-IMP-DEF-01, REQ-IMP-DEF-02):

| Concept | BRD Term | Description | Example |
|---------|----------|-------------|---------|
| **Instrument** | Trade Instrument / Business State | The financial product data record; lifecycle of the LC/Collection | Import LC with amount, beneficiary, terms |
| **Transaction** | Transaction / Processing Flow | Each processing execution that goes through Maker/Checker authorization | Issuance, Amendment, Presentation, Settlement |

### 1.2 Two Parallel Status Flows

| Status Flow | Field | Tracks | Values |
|-------------|-------|--------|--------|
| **Transaction Status** | `TradeTransaction.transactionStatusId` | Maker/Checker workflow | TX_DRAFT, TX_PENDING, TX_APPROVED |
| **Business State** | `ImportLetterOfCredit.businessStateId` | Product lifecycle | LC_DRAFT, LC_ISSUED, LC_DOCS_RECEIVED... |

### 1.3 Relationship

```
TradeInstrument (1) ──── (many) TradeTransaction
  - product (Import LC)           - transaction type (AMENDMENT, PRESENTATION...)
  - financial terms               - authorization status (TX_*)
  - business state (LC_*)        - maker/checker info
```

---

## 2. TradeTransaction Entity

### 2.1 Fields

```xml
<entity entity-name="TradeTransaction" package="trade">
    <field name="transactionId" type="id" is-pk="true"/>
    <field name="instrumentId" type="id"/>
    <field name="transactionTypeEnumId" type="id"/>  <!-- IMP_NEW, IMP_AMENDMENT, etc. -->
    <field name="transactionStatusId" type="id"/>    <!-- TX_DRAFT, TX_PENDING, TX_APPROVED -->
    
    <!-- Maker Info -->
    <field name="makerUserId" type="id"/>
    <field name="makerTimestamp" type="date-time"/>
    
    <!-- Checker Info -->
    <field name="checkerUserId" type="id"/>
    <field name="checkerTimestamp" type="date-time"/>
    
    <!-- Additional Authorization -->
    <field name="rejectionReason" type="text-long"/>
    <field name="versionNumber" type="number-integer"/>
    <field name="priorityEnumId" type="id"/>
    <field name="transactionDate" type="date"/>
    
    <!-- Links to payload entity -->
    <field name="relatedRecordId" type="id"/>        <!-- AmendmentId, PresentationId, etc. -->
    <field name="relatedRecordType" type="id"/>      <!-- Amendment, Presentation, Settlement -->
    
    <relationship type="one" related="trade.TradeInstrument">
        <key-map field-name="instrumentId"/>
    </relationship>
</entity>
```

### 2.2 Transaction Type Enumeration

```xml
<!-- TradeTransactionType -->
<moqui.basic.EnumerationType enumTypeId="TradeTransactionType" description="Type of Trade Transaction"/>

<!-- Import LC Transactions -->
<moqui.basic.Enumeration enumId="IMP_NEW" enumTypeId="TradeTransactionType" description="Import LC Issuance"/>
<moqui.basic.Enumeration enumId="IMP_AMENDMENT" enumTypeId="TradeTransactionType" description="Import LC Amendment"/>
<moqui.basic.Enumeration enumId="IMP_PRESENTATION" enumTypeId="TradeTransactionType" description="Import LC Document Presentation"/>
<moqui.basic.Enumeration enumId="IMP_SETTLEMENT" enumTypeId="TradeTransactionType" description="Import LC Settlement"/>
<moqui.basic.Enumeration enumId="IMP_CANCELLATION" enumTypeId="TradeTransactionType" description="Import LC Cancellation"/>

<!-- Future: Export LC -->
<moqui.basic.Enumeration enumId="EXP_NEW" enumTypeId="TradeTransactionType" description="Export LC Issuance"/>
<moqui.basic.Enumeration enumId="EXP_AMENDMENT" enumTypeId="TradeTransactionType" description="Export LC Amendment"/>

<!-- Future: Collections -->
<moqui.basic.Enumeration enumId="COL_IMP_NEW" enumTypeId="TradeTransactionType" description="Import Collection"/>
```

---

## 3. TradeInstrument Cleanup

### 3.1 Fields to Remove from TradeInstrument

| Field | Action | Reason |
|-------|--------|--------|
| `transactionStatusId` | **MOVE** → TradeTransaction | Authorization workflow status |
| `lifecycleStatusId` | **REMOVE** | Redundant; use `businessStateId` |
| `makerUserId` | **MOVE** → TradeTransaction | Maker information |
| `makerTimestamp` | **MOVE** → TradeTransaction | |
| `checkerUserId` | **MOVE** → TradeTransaction | |
| `checkerTimestamp` | **MOVE** → TradeTransaction | |
| `rejectionReason` | **MOVE** → TradeTransaction | |
| `versionNumber` | **MOVE** → TradeTransaction | For dual-checker |
| `priorityEnumId` | **MOVE** → TradeTransaction | |

### 3.2 Fields to Keep on TradeInstrument

| Field | Description |
|-------|-------------|
| `instrumentId` | Primary key |
| `transactionRef` | Human-readable reference (TF-IMP-2026-0001) |
| `businessStateId` | LC/Collection lifecycle state |
| `previousBusinessStateId` | For Hold/Release scenarios |
| `instrumentTypeEnumId` | INST_LC, INST_COLLECTION |
| `productEnumId` | Product type |
| `amount` | Original issuance amount |
| `currencyUomId` | Currency code |
| `outstandingAmount` | Remaining balance |
| `baseEquivalentAmount` | Local currency equivalent |
| `applicantPartyId` | Customer |
| `beneficiaryPartyId` | Supplier |
| `issueDate` | Issue date |
| `expiryDate` | Expiry date |
| `customerFacilityId` | Credit facility |
| Other SWIFT-related fields | |

---

## 4. Transaction Workflow Pattern

### 4.1 Processing Flow per Transaction Type

| Step | Action | Transaction Status |
|------|--------|-------------------|
| 1 | User starts processing (e.g., create Amendment) | TX_DRAFT |
| 2 | User submits for approval | TX_PENDING |
| 3 | Checker reviews and approves | TX_APPROVED |

### 4.2 Service Pattern

Each transaction type requires:
1. **Create Service** → Creates TradeTransaction (TX_DRAFT) + payload entity
2. **Submit Service** → Transitions to TX_PENDING
3. **Approve Service** → Evaluates Maker/Checker matrix, transitions to TX_APPROVED
4. **Reject Service** → Returns to TX_DRAFT with rejection reason

---

## 5. Data Migration

### 5.1 Migration Strategy

For existing TradeInstrument records, create corresponding TradeTransaction records:

```sql
-- For Issuance (original instrument creation)
INSERT INTO trade_transaction (
    transaction_id, instrument_id, transaction_type_enum_id,
    transaction_status_id, transaction_date,
    maker_user_id, maker_timestamp,
    checker_user_id, checker_timestamp,
    rejection_reason, version_number, priority_enum_id
)
SELECT 
    instrument_id, instrument_id, 'IMP_NEW',
    transaction_status_id, transaction_date,
    maker_user_id, maker_timestamp,
    checker_user_id, checker_timestamp,
    rejection_reason, version_number, priority_enum_id
FROM trade_instrument;
```

### 5.2 Post-Migration Cleanup

1. Remove migrated fields from TradeInstrument entity definition
2. Update all service references to use TradeTransaction
3. Update seed data to not include migrated fields in TradeInstrument

---

## 6. Implementation Scope (Phase 1: Import LC Focus)

### 6.1 Transaction Types to Implement

| Type | Description |
|------|-------------|
| `IMP_NEW` | Initial Import LC creation |
| `IMP_AMENDMENT` | Terms modification |
| `IMP_PRESENTATION` | Document presentation |
| `IMP_SETTLEMENT` | Payment |

### 6.2 Products with Transaction Support

- Import Letter of Credit (Import LC)

---

## 7. Backward Compatibility

### 7.1 Transition Period

During transition, services should:
1. First check TradeTransaction for authorization data
2. Fall back to TradeInstrument fields if no transaction record exists (for legacy data)
3. Create transaction records for all new operations

### 7.2 Deprecation Timeline

- Phase 1: Dual read (transaction + instrument fields)
- Phase 2: Transaction-first, warn if no transaction record
- Phase 3: Require transaction record for all operations

---

## 8. Files to Modify

| File | Changes |
|------|---------|
| `entity/TradeCommonEntities.xml` | Add TradeTransaction, remove migrated fields from TradeInstrument |
| `data/TradeFinanceSeedData.xml` | Add TransactionType enums |
| `service/trade/AuthorizationServices.xml` | Update to use TradeTransaction |
| `service/trade/importlc/ImportLcServices*.xml` | Update to use TradeTransaction |
| `service/trade/LimitServices.xml` | Update references |
| `service/trade/SwiftGenerationServices.xml` | Update references |
| `service/TradeFinanceSeca.xml` | Update trigger conditions |
| `data/TradeFinanceMasterData.xml` | Clean up migrated fields |
| `data/TradeFinanceAuthData.xml` | Clean up |

---

## 9. Validation Checklist

- [ ] TradeTransaction created with all required fields
- [ ] TransactionType enums defined and-seeded
- [ ] TradeInstrument migrated fields removed
- [ ] All services updated to use TradeTransaction
- [ ] Dual-checker logic still works
- [ ] Audit logging still works
- [ ] Limit checking still works
- [ ] SWIFT generation still works
- [ ] Tests pass

---

**Document Version:** 1.0
**Date:** April 28, 2026
**Author:** Design Review
**Status:** Approved for Implementation