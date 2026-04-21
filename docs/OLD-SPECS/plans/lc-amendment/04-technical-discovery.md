# Technical Discovery - LC Amendment

**Created**: 2026-03-16
**Phase**: 4 - Technical Discovery
**Agent**: @tech-designer + @plan
**Feature**: LC Amendment
**Input**: 01-business-requirements.md, 02-current-state.md, 03-future-state.md

---

## 1. New Entities

### 1.1 LcAmendmentBeneficiaryResponse
```xml
<entity entity-name="LcAmendmentBeneficiaryResponse" package="moqui.trade.finance">
    <field name="responseId" type="id" is-pk="true"/>
    <field name="lcId" type="id"/>
    <field name="amendmentSeqId" type="id"/>
    <field name="responseDate" type="date-time"/>
    <field name="responseTypeEnumId" type="id"/>
    <field name="beneficiaryPartyId" type="id"/>
    <field name="contactName" type="text-medium"/>
    <field name="contactMethod" type="id"/>
    <field name="responseReference" type="text-short"/>
    <field name="reason" type="text-medium"/>
    <field name="receivedByUserId" type="id"/>
    
    <relationship type="one" related="moqui.trade.finance.LcAmendment">
        <key-map field-name="lcId"/><key-map field-name="amendmentSeqId"/></relationship>
    <relationship type="one" title="ResponseType" related="moqui.basic.Enumeration">
        <key-map field-name="responseTypeEnumId"/></relationship>
    <relationship type="one" title="Beneficiary" related="mantle.party.Party">
        <key-map field-name="beneficiaryPartyId"/></relationship>
    <relationship type="one" related="moqui.security.UserAccount">
        <key-map field-name="receivedByUserId" related="userId"/></relationship>
</entity>
```

### 1.2 LcAmendmentLock
```xml
<entity entity-name="LcAmendmentLock" package="moqui.trade.finance">
    <field name="lcId" type="id" is-pk="true"/>
    <field name="lockType" type="id" is-pk="true"/>
    <field name="lockedByUserId" type="id"/>
    <field name="lockedAt" type="date-time"/>
    <field name="lockExpiry" type="date-time"/>
    <field name="amendmentSeqId" type="id"/>
    
    <relationship type="one" related="moqui.trade.finance.LetterOfCredit"/>
    <relationship type="one" related="moqui.security.UserAccount">
        <key-map field-name="lockedByUserId" related="userId"/></relationship>
</entity>
```

### 1.3 LcAmendmentFullDetailView
```xml
<view-entity entity-name="LcAmendmentFullDetailView" package="moqui.trade.finance">
    <member-entity entity-alias="AMND" entity-name="moqui.trade.finance.LcAmendment"/>
    <member-entity entity-alias="LC" entity-name="moqui.trade.finance.LetterOfCredit" join-from-alias="AMND">
        <key-map field-name="lcId"/></member-entity>
    <member-entity entity-alias="ST" entity-name="moqui.basic.StatusItem" join-from-alias="AMND">
        <key-map field-name="amendmentStatusId" related="statusId"/></member-entity>
    <member-entity entity-alias="CST" entity-name="moqui.basic.StatusItem" join-from-alias="AMND">
        <key-map field-name="confirmationStatusId" related="statusId"/></member-entity>
    <member-entity entity-alias="RESP" entity-name="moqui.trade.finance.LcAmendmentBeneficiaryResponse" join-from-alias="AMND">
        <key-map field-name="lcId"/><key-map field-name="amendmentSeqId"/></member-entity>
    
    <alias-all entity-alias="AMND"/>
    <alias entity-alias="LC" name="lcNumber"/>
    <alias entity-alias="LC" name="applicantPartyId"/>
    <alias entity-alias="LC" name="beneficiaryPartyId" name="lcBeneficiaryPartyId"/>
    <alias entity-alias="ST" name="statusDescription" field="description"/>
    <alias entity-alias="CST" name="confirmationStatusDescription" field="description"/>
</view-entity>
```

### 1.4 LcAmendmentChangesView
```xml
<view-entity entity-name="LcAmendmentChangesView" package="moqui.trade.finance">
    <member-entity entity-alias="HIST" entity-name="moqui.trade.finance.LcHistory"/>
    <member-entity entity-alias="LC" entity-name="moqui.trade.finance.LetterOfCredit" join-from-alias="HIST">
        <key-map field-name="lcId"/></member-entity>
    
    <alias-all entity-alias="HIST"/>
    <alias entity-alias="LC" name="lcNumber"/>
</view-entity>
```

---

## 2. Modified Entities

| Entity | Changes |
|--------|---------|
| **LcAmendment** | Add fields for beneficiary acceptance window tracking: `beneficiaryAcceptanceWindowDays` (Integer), `beneficiaryAcceptanceExpiry` (date-time) |
| **LetterOfCredit** | No changes needed (already has `amendmentNumber` field) |
| **LcHistory** | No changes needed (already tracks amendments via `amendmentSeqId`) |

---

## 3. New Services

| Service | Purpose | Parameters |
|---------|---------|------------|
| create#LcAmendmentDraft | Creates amendment draft with validation and locking | lcId (id), remarks (text-medium) → amendmentSeqId (id), requestId (id) |
| route#AmendmentForApproval | Routes amendment through approval workflow | lcId (id), amendmentSeqId (id) |
| review#LcAmendmentBySupervisor | Supervisor review (approve/reject) | lcId (id), amendmentSeqId (id), approvalDecision (text-short), reviewComments (text-medium) |
| approve#LcAmendmentByIpc | IPC final approval (approve/reject) | lcId (id), amendmentSeqId (id), approvalDecision (text-short), ipcComments (text-medium) |
| record#BeneficiaryResponse | Records beneficiary acceptance/rejection | lcId (id), amendmentSeqId (id), responseTypeEnumId (id), beneficiaryPartyId (id), contactName (text-medium), contactMethod (id), responseReference (text-short), reason (text-medium) → responseId (id) |
| confirm#AmendmentApplication | Applies confirmed amendment to master LC | lcId (id), amendmentSeqId (id) |
| track#BeneficiaryAcceptanceWindow | Tracks UCP 600 Article 10 acceptance window | lcId (id), amendmentSeqId (id), windowDays (Integer) |
| adjust#ProvisionsForAmendment | Adjusts provisions if LC amount changed | lcId (id), amendmentSeqId (id) |
| acquire#AmendmentLock | Acquires pessimistic lock for concurrency control | lcId (id), lockTimeoutMinutes (Integer) |
| release#AmendmentLock | Releases amendment lock | lcId (id) |
| check#AmendmentLockStatus | Checks lock status | lcId (id) → isLocked (Boolean), lockedByUserId (id), lockExpiry (date-time) |
| forceRelease#AmendmentLock | Administrative force release of lock | lcId (id), reason (text-medium) |
| record#AmendmentAction | Records amendment action for audit trail | lcId (id), amendmentSeqId (id), actionType (text-short), actionResult (text-short), comments (text-medium) |
| get#AmendmentHistory | Retrieves full amendment history | lcId (id), amendmentSeqId (id) → historyList (List) |
| get#AmendmentFieldChanges | Gets field changes between original and amendment | lcId (id), amendmentSeqId (id) → changes (List) |

---

## 4. Modified Services

| Service | Changes |
|---------|---------|
| **AmendmentServices.create#LcAmendment** | Add validation that LC status is "Issued" before creating draft |
| **AmendmentServices.confirm#LcAmendment** | Add parameter to indicate if amendment was accepted by beneficiary |
| **SwiftServices.generate#SwiftMt707** | Enhance to generate full SWIFT MT707 message per standards |
| **FinancialServices.calculate#LcCharges** | Ensure amendment-specific charge calculation |
| **FinancialServices.create#LcProvisions** | Add amendmentSeqId parameter for provision tracking |

---

## 5. Screen Changes

| Screen | Changes |
|--------|---------|
| **AmendmentList.xml** (NEW) | List all amendments with filtering by status, LC number, dates |
| **AmendmentDetail.xml** (NEW) | View amendment details, show proposed changes, action buttons based on status/role |
| **AmendmentEdit.xml** (NEW) | Edit amendment draft (only in Draft status) |
| **AmendmentSupervisorReview.xml** (NEW) | Supervisor review interface with approve/reject options |
| **AmendmentHistory.xml** (NEW) | Display amendment history and field changes |
| **Financials.xml** (existing) | Add amendment-specific charges and provisions display |
| **MainLC.xml** (existing) | Add "Amendments" tab showing linked amendments |
| **Amendment.xml** (existing) | Update to include new subscreens |

---

## 6. Implementation Roadmap

### Phase 1: Foundation Setup (Week 1)
1. Create new entities: LcAmendmentBeneficiaryResponse, LcAmendmentLock
2. Create view entities: LcAmendmentFullDetailView, LcAmendmentChangesView
3. Add new fields to LcAmendment for beneficiary acceptance window tracking
4. Create enumeration types for beneficiary response (LcAmndAccept, LcAmndReject)
5. Create AmendmentServices.xml with core services (create, submit, approve, confirm)
6. Set up basic service infrastructure and transaction boundaries

### Phase 2: Workflow Implementation (Week 2)
1. Implement AmendmentWorkflowServices.xml:
   - create#LcAmendmentDraft with validation and locking
   - route#AmendmentForApproval (CSR → Supervisor → IPC)
   - review#LcAmendmentBySupervisor
   - approve#LcAmendmentByIpc
2. Implement AmendmentConcurrencyServices.xml:
   - acquire#AmendmentLock, release#AmendmentLock, check#AmendmentLockStatus
   - forceRelease#AmendmentLock (admin function)
3. Implement basic notification service integration
4. Create unit tests for workflow services

### Phase 3: Beneficiary Acceptance & History (Week 3)
1. Implement AmendmentHistoryServices.xml:
   - record#AmendmentAction (audit trail)
   - get#AmendmentHistory
   - get#AmendmentFieldChanges
2. Implement beneficiary response handling:
   - record#BeneficiaryResponse service
   - track#BeneficiaryAcceptanceWindow service
   - Beneficiary acceptance/rejection UI components
3. Enhance SwiftServices.generate#SwiftMt707 for full MT707 compliance
4. Create integration tests for beneficiary workflow

### Phase 4: Financial Integration & UI (Week 4)
1. Implement adjust#ProvisionsForAmendment service
2. Update FinancialServices for amendment-specific charge/provision calculation
3. Create UI screens:
   - AmendmentList.xml
   - AmendmentDetail.xml
   - AmendmentEdit.xml
   - AmendmentSupervisorReview.xml
   - AmendmentHistory.xml
4. Update existing screens:
   - Financials.xml (amendment charges/provisions)
   - MainLC.xml (amendments tab)
   - Amendment.xml (subscreen navigation)
5. Implement read-only context enforcement for LC viewing from amendment

### Phase 5: Testing & Validation (Week 5)
1. Create comprehensive unit tests for all new services
2. Create integration tests for end-to-end amendment workflow
3. Create UI tests for all amendment screens
4. Test edge cases:
   - Concurrent amendment prevention
   - Beneficiary acceptance/rejection
   - Lock expiration and cleanup
   - SWIFT MT707 generation
   - Provision adjustment on amount change
   - Audit trail completeness
5. Performance testing and optimization
6. Security validation (role-based access control)

### Phase 6: Deployment Preparation (Week 6)
1. Create data seed files for:
   - Beneficiary response enumerations
   - Sample amendment records
   - Test data for various scenarios
2. Update documentation:
   - Technical specifications
   - User guides for amendment functionality
   - API documentation for new services
3. Conduct user acceptance testing with business stakeholders
4. Final bug fixing and polish
5. Prepare release notes and deployment instructions

---

## 7. Files to Create/Modify

| Type | Path | Action |
|------|------|--------|
| Entity | entity/TradeFinanceEntities.xml | Modify - Add new entities and fields |
| Service | service/moqui/trade/finance/AmendmentServices.xml | Modify - Enhance existing services |
| Service | service/moqui/trade/finance/AmendmentWorkflowServices.xml | Create |
| Service | service/moqui/trade/finance/AmendmentConcurrencyServices.xml | Create |
| Service | service/moqui/trade/finance/AmendmentHistoryServices.xml | Create |
| Screen | screen/TradeFinance/ImportLc/Lc/AmendmentList.xml | Create |
| Screen | screen/TradeFinance/ImportLc/Lc/AmendmentDetail.xml | Create |
| Screen | screen/TradeFinance/ImportLc/Lc/AmendmentEdit.xml | Create |
| Screen | screen/TradeFinance/ImportLc/Lc/AmendmentSupervisorReview.xml | Create |
| Screen | screen/TradeFinance/ImportLc/Lc/AmendmentHistory.xml | Create |
| Screen | screen/TradeFinance/ImportLc/Lc/Financials.xml | Modify |
| Screen | screen/TradeFinance/ImportLc/Lc/MainLC.xml | Modify |
| Screen | screen/TradeFinance/ImportLc/Lc/Amendment.xml | Modify |
| Data | data/10_TradeFinanceData.xml | Modify - Add status enumerations and seed data |
| Data | data/seed/TradeFinanceSeedData.xml | Create - Add enumeration seed data |
| Test | test/groovy/moqui/trade/finance/AmendmentServicesSpec.java | Create |
| Test | test/groovy/moqui/trade/finance/AmendmentWorkflowServicesSpec.java | Create |
| Test | test/groovy/moqui/trade/finance/AmendmentHistoryServicesSpec.java | Create |
| Test | test/groovy/moqui/trade/finance/AmendmentUISpec.java | Create |

---

## 8. Status Enumerations

| Status ID | Status Type | Description |
|-----------|-------------|-------------|
| LcAmndAccept | BeneficiaryResponse | Beneficiary accepted the amendment |
| LcAmndReject | BeneficiaryResponse | Beneficiary rejected the amendment |
| LcAmndPendingBen | BeneficiaryResponse | Awaiting beneficiary response |
| LcTxSupervisorApproved | TransactionStatus | Approved by Branch Supervisor |
| LcTxIpcApproved | TransactionStatus | Approved by IPC (awaiting beneficiary) |
| LcTxDraft | TransactionStatus | Amendment in draft state |
| LcTxSubmitted | TransactionStatus | Submitted for review |
| LcTxRejected | TransactionStatus | Rejected at any approval stage |

---

## 9. Dependencies & Integration Points

### Internal Dependencies
- **Mantle Request Workflow**: Uses RqtLcAmendment request type for approval routing
- **LcHistory**: Tracks all amendment actions for audit trail
- **LetterOfCredit**: Main LC entity that gets updated upon confirmation
- **FinancialServices**: Calculates charges and provisions
- **SwiftServices**: Generates SWIFT MT707 messages
- **NotificationService**: Sends status updates to stakeholders
- **Security**: Role-based access control for different workflow stages

### External Integration Points
- **CBS (Core Banking)**:
  - Exchange rate lookup for multi-currency amendments
  - Fund hold/release for provision adjustments
  - Accounting entry posting for charges and provisions
- **SWIFT Network**:
  - MT707 transmission to Advising Bank
  - MT799 for beneficiary notifications (if implemented)
- **Notification Systems**:
  - Email/SMS alerts for status changes
  - Internal system notifications
- **Document Management**:
  - Storage of SWIFT MT707 messages
  - Attachment of supporting documents to amendments

---

## 10. Non-Functional Requirements Addressed

### Security
- Role-based access control (CSR, Supervisor, IPC, Beneficiary)
- Pessimistic locking prevents race conditions
- Input validation and sanitization
- Audit trail for all amendment actions
- Secure handling of beneficiary responses

### Performance
- Efficient entity locking with automatic expiry
- Minimal database queries through proper indexing
- Caching of reference data (statuses, enumerations)
- Asynchronous notification processing
- Pagination for large amendment lists

### Scalability
- Stateless service design
- Horizontal scaling capable
- Database connection pooling
- Message queue integration ready for notifications
- Partitioning strategy ready for high-volume LCs

### Maintainability
- Modular service design (workflow, concurrency, history)
- Clear separation of concerns
- Comprehensive unit and integration test coverage
- Detailed logging and monitoring points
- Documentation of business rules and assumptions

### Compliance
- UCP 600 Article 10 beneficiary acceptance workflow
- SWIFT MT707 message standards compliance
- Audit trail for regulatory reporting
- Data retention and archiving capabilities
- Role segregation of duties (CSR vs Supervisor vs IPC)

---

**Last Updated**: 2026-03-16
