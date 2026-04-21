# Technical Discovery - TradeFinance Component

**Created**: 2026-03-16
**Phase**: 4 - Technical Discovery
**Agent**: @tech-designer + @plan
**Feature**: TradeFinance Component Implementation
**Input**: 01-business-requirements.md, 02-current-state.md, 03-future-state.md

---

## 1. New Entities

Based on the TSD and BRD analysis, the following entities need to be created or are already present:

### 1.1 Core Entities (Already Present)
- LetterOfCredit (master entity)
- LcProduct (configuration)
- LcProductCharge (product charges)
- LcHistory (audit trail)
- LcAmendment (shadow record model)
- LcDrawing (document presentations)
- LcDrawingDocument (drawing documents)
- LcDiscrepancy (discrepancy tracking)
- LcCharge (charges and fees)
- LcProvision (provision holds)
- LcDocument (attachments)
- CbsSimulatorState (CBS simulation)

### 1.2 View Entities (Already Present)
- LcAmendmentDetailView
- LcDrawingDetailView

### 1.3 Status Item Entities (Need to be added to data seed files)
- LcLifecycle statuses
- LcTransaction statuses
- LcDrawingStatus statuses
- LcProvisionStatus statuses
- LcAmendmentConfirmation statuses
- Collection statuses (for provision collection)
- Entry statuses (for provision collection)

---

## 2. Modified Entities

### 2.1 Entities Requiring Modifications
- **LcProvision**: Add `collectionId` field (FK to LcProvisionCollection) - already implemented
- **CbsSimulatorState**: Add multi-currency support (accountId as PK) - already implemented
- **LcProvisionCollection**: New entity for multi-account provision collection - already implemented
- **LcProvisionCollectionEntry**: New entity for individual account entries - already implemented

---

## 3. New Services

### 3.1 Core Services (Already Present)
- TradeFinanceServices.xml
- AmendmentServices.xml
- DrawingServices.xml
- LifecycleServices.xml
- FinancialServices.xml
- SwiftServices.xml
- CbsIntegrationServices.xml
- CbsSimulatorServices.xml
- NotificationServices.xml
- ScheduledServices.xml
- DocumentServices.xml
- AccountingServices.xml
- ProvisionCollectionServices.xml

### 3.2 Key Service Functions

#### TradeFinanceServices
- `validate#LetterOfCredit` - SWIFT charset and format validation
- `create#LetterOfCredit` - Create LC with initial charges and request
- `update#LetterOfCredit` - Update LC with history tracking
- `delete#LetterOfCredit` - Draft-only deletion with cascade
- `transition#LcStatus` - LC lifecycle status transitions
- `transition#TransactionStatus` - Internal processing status transitions
- `update#LcApplicationDetail` - Update application details
- `check#CustomerCreditLimit` - CBS credit limit check
- `submit#LetterOfCredit` - Submit for review
- `approve#LcBySupervisor` - Supervisor approval
- `approve#LcByTradeOperator` - Trade operator approval with CBS holds
- `calculate#LcChargesAndProvisions` - Calculate charges and provisions
- `approve#LcByTradeSupervisor` - Final approval and LC issuance
- `return#LetterOfCredit` - Return for correction

#### AmendmentServices
- `create#LcAmendment` - Create amendment shadow record
- `confirm#LcAmendment` - Apply amendment and generate MT707

#### DrawingServices
- `register#LcDrawing` - Register document presentation
- `examine#LcDrawing` - Examine documents for compliance
- `record#LcDiscrepancy` - Record discrepancies
- `resolve#LcDiscrepancy` - Resolve discrepancies

#### LifecycleServices
- `issue#LetterOfCredit` - Issue LC with contingent accounting
- `revoke#LetterOfCredit` - Revoke LC with MT799

#### FinancialServices
- `calculate#LcCharge` - Calculate individual charges
- `calculate#LcProvision` - Calculate and hold provisions
- `release#LcProvision` - Release provisions

#### SwiftServices
- `generate#SwiftMt700` - Generate MT700 for LC issuance
- `generate#SwiftMt707` - Generate MT707 for amendments
- `generate#SwiftMt734` - Generate MT734 for discrepancies
- `generate#SwiftMt799` - Generate MT799 for revocations
- `parse#SwiftMt700` - Parse incoming MT700 messages
- `parse#SwiftMt707` - Parse incoming MT707 messages

#### ProvisionCollectionServices
- `create#LcProvisionCollection` - Initialize multi-account collection
- `add#CollectionEntry` - Add account entry with currency conversion
- `fetch#ExchangeRate` - Get CBS exchange rate
- `validate#CollectionTotal` - Validate collection total matches target
- `collect#ProvisionFunds` - Execute CBS holds with rollback
- `release#ProvisionCollection` - Release all CBS holds

#### AccountingServices
- `post#LcChargesToInvoice` - Post charges to invoice and generate GL entries

#### NotificationServices
- `send#LcNotification` - Send LC-related notifications

#### ScheduledServices
- `check#LcExpiry` - Daily job to expire overdue LCs

#### DocumentServices
- `attach#LcDocument` - Attach documents to LC records

---

## 4. Modified Services

### 4.1 Services Requiring Modifications
- **FinancialServices.xml**: Enhanced provision calculation and release
- **CbsIntegrationServices.xml**: Enhanced hold/release functions for multi-account support
- **ProvisionCollectionServices.xml**: New service for provision collection management
- **AccountingServices.xml**: Enhanced charge posting to invoices

---

## 5. Screen Changes

### 5.1 Screen Structure (Already Defined in TSD)
- **Root Screen**: `screen/TradeFinance.xml`
- **Import LC Module**: `screen/TradeFinance/ImportLc.xml`
  - Dashboard
  - Task Queue
  - Letter of Credit wrapper (`Lc.xml`)
    - Find LC (`FindLc.xml`)
    - Main LC Detail (`MainLC.xml`)
    - Financials (`Financials.xml`)
    - Amendments (`Amendments.xml`)
    - Drawings (`Drawings.xml`)
    - History (`History.xml`)
  - Amendment wrapper (`Amendment.xml`)
    - Find Amendment (`FindAmendment.xml`)
    - Amendment Detail (`AmendmentDetail.xml`)
    - Financials (`Financials.xml`)
    - History (`History.xml`)
  - Drawing wrapper (`Drawing.xml`)
    - Find Drawing (`FindDrawing.xml`)
    - Drawing Detail (`DrawingDetail.xml`)

### 5.2 Key Screen Patterns
- **Wrapper Pattern**: Used for LC, Amendment, and Drawing screens with read-only support
- **Find/List Pattern**: Standard entity-find with form-list for navigation
- **Detail Form Pattern**: Conditional fields for edit/display modes
- **Status Chip Pattern**: Visual status indicators using Quasar q-chip
- **Cross-Module Navigation**: Relative paths with read-only=true for viewing
- **Provision Collection Screen**: New screen for multi-account provision collection

### 5.3 Specific Screen Modifications
- **Financials.xml**: Add provision collection link and summary
- **MainLC.xml**: Add provision collection button and status display
- **ProvisionCollection.xml**: New screen for managing multi-account collections

---

## 6. Implementation Roadmap

### Phase 1: Foundation Setup (Week 1)
1. Verify entity definitions in TradeFinanceEntities.xml
2. Verify service definitions in all service XML files
3. Verify screen structure matches TSD specifications
4. Create initial data seed files for status items and enumerations
5. Set up CBS simulator for testing

### Phase 2: Core LC Functionality (Weeks 2-3)
1. Implement and test LC creation, validation, and basic transitions
2. Implement and test LC application workflow (submit, approve, return)
3. Implement and test LC issuance with contingent accounting
4. Implement and test basic SWIFT message generation (MT700)
5. Implement and test notification services

### Phase 3: Amendment and Drawing Functionality (Weeks 4-5)
1. Implement and test LC amendment creation and confirmation
2. Implement and test SWIFT MT707 generation for amendments
3. Implement and test document presentation and examination
4. Implement and test discrepancy handling and MT734 generation
5. Implement and test payment processing (sight, usance, deferred, negotiation)

### Phase 4: Provision and Financial Management (Weeks 6-7)
1. Implement and test provision calculation and CBS holds
2. Implement and test charge calculation and invoicing
3. Implement and test provision release on LC closure/expiry/revocation
4. Implement and test multi-account provision collection
5. Implement and test CBS integration simulators

### Phase 5: UI Implementation and Refinement (Weeks 8-9)
1. Implement all screens following Moqui XML patterns
2. Implement conditional rendering for read-only modes
3. Implement status chips and visual indicators
4. Implement cross-module navigation with proper read-only handling
5. Implement provision collection UI with real-time validation

### Phase 6: Testing and Validation (Weeks 10-12)
1. Unit test all services with Spock framework
2. Integration test screen flows and transitions
3. Test end-to-end LC lifecycle scenarios
4. Test provision collection workflows with multiple currencies
5. Test SWIFT message generation and parsing
6. Performance testing with simulated loads
7. Security testing with role-based access control

---

## 7. Files to Create/Modify

### 7.1 Entity Files
| Type | Path | Action |
|------|------|--------|
| Entity | runtime/component/TradeFinance/entity/TradeFinanceEntities.xml | Verify - Already complete |
| Data | runtime/component/TradeFinance/data/10_TradeFinanceData.xml | Modify - Add status enumerations |
| Data | runtime/component/TradeFinance/data/20_TradeFinanceSecurityData.xml | Modify - Add roles and permissions |
| Data | runtime/component/TradeFinance/data/30_TradeFinanceDemoData.xml | Modify - Add demo LCs, parties, users |

### 7.2 Service Files
| Type | Path | Action |
|------|------|--------|
| Service | runtime/component/TradeFinance/service/moqui/trade/finance/TradeFinanceServices.xml | Verify - Already complete |
| Service | runtime/component/TradeFinance/service/moqui/trade/finance/AmendmentServices.xml | Verify - Already complete |
| Service | runtime/component/TradeFinance/service/moqui/trade/finance/DrawingServices.xml | Verify - Already complete |
| Service | runtime/component/TradeFinance/service/moqui/trade/finance/LifecycleServices.xml | Verify - Already complete |
| Service | runtime/component/TradeFinance/service/moqui/trade/finance/FinancialServices.xml | Verify - Already complete |
| Service | runtime/component/TradeFinance/service/moqui/trade/finance/SwiftServices.xml | Verify - Already complete |
| Service | runtime/component/TradeFinance/service/moqui/trade/finance/CbsIntegrationServices.xml | Verify - Already complete |
| Service | runtime/component/TradeFinance/service/moqui/trade/finance/CbsSimulatorServices.xml | Verify - Already complete |
| Service | runtime/component/TradeFinance/service/moqui/trade/finance/NotificationServices.xml | Verify - Already complete |
| Service | runtime/component/TradeFinance/service/moqui/trade/finance/ScheduledServices.xml | Verify - Already complete |
| Service | runtime/component/TradeFinance/service/moqui/trade/finance/DocumentServices.xml | Verify - Already complete |
| Service | runtime/component/TradeFinance/service/moqui/trade/finance/AccountingServices.xml | Verify - Already complete |
| Service | runtime/component/TradeFinance/service/moqui/trade/finance/ProvisionCollectionServices.xml | Verify - Already complete |

### 7.3 Screen Files
| Type | Path | Action |
|------|------|--------|
| Screen | runtime/component/TradeFinance/screen/TradeFinance.xml | Verify - Root screen |
| Screen | runtime/component/TradeFinance/screen/TradeFinance/ImportLc.xml | Verify - Module screen |
| Screen | runtime/component/TradeFinance/screen/TradeFinance/ImportLc/Lc.xml | Verify - LC wrapper |
| Screen | runtime/component/TradeFinance/screen/TradeFinance/ImportLc/Lc/FindLc.xml | Verify - Find LC |
| Screen | runtime/component/TradeFinance/screen/TradeFinance/ImportLc/Lc/MainLC.xml | Verify - Main LC detail |
| Screen | runtime/component/TradeFinance/screen/TradeFinance/ImportLc/Lc/Financials.xml | Modify - Add provision collection link |
| Screen | runtime/component/TradeFinance/screen/TradeFinance/ImportLc/Lc/Amendments.xml | Verify - Amendments list |
| Screen | runtime/component/TradeFinance/screen/TradeFinance/ImportLc/Lc/Drawings.xml | Verify - Drawings list |
| Screen | runtime/component/TradeFinance/screen/TradeFinance/ImportLc/Lc/History.xml | Verify - History list |
| Screen | runtime/component/TradeFinance/screen/TradeFinance/ImportLc/Amendment.xml | Verify - Amendment wrapper |
| Screen | runtime/component/TradeFinance/screen/TradeFinance/ImportLc/Amendment/FindAmendment.xml | Verify - Find Amendment |
| Screen | runtime/component/TradeFinance/screen/TradeFinance/ImportLc/Amendment/AmendmentDetail.xml | Verify - Amendment detail |
| Screen | runtime/component/TradeFinance/screen/TradeFinance/ImportLc/Amendment/Financials.xml | Verify - Amendment financials |
| Screen | runtime/component/TradeFinance/screen/TradeFinance/ImportLc/Amendment/History.xml | Verify - Amendment history |
| Screen | runtime/component/TradeFinance/screen/TradeFinance/ImportLc/Drawing.xml | Verify - Drawing wrapper |
| Screen | runtime/component/TradeFinance/screen/TradeFinance/ImportLc/Drawing/FindDrawing.xml | Verify - Find Drawing |
| Screen | runtime/component/TradeFinance/screen/TradeFinance/ImportLc/Drawing/DrawingDetail.xml | Verify - Drawing detail |
| Screen | runtime/component/TradeFinance/screen/TradeFinance/ImportLc/Lc/ProvisionCollection.xml | Create - New provision collection screen |

### 7.4 Test Files
| Type | Path | Action |
|------|------|--------|
| Test | runtime/component/TradeFinance/src/test/groovy/moqui/trade/finance/TradeFinanceServicesSpec.groovy | Verify - Service tests |
| Test | runtime/component/TradeFinance/src/test/groovy/moqui/trade/finance/TradeFinanceScreensSpec.groovy | Verify - Screen tests |
| Test | runtime/component/TradeFinance/src/test/groovy/moqui/trade/finance/TradeFinanceDrawingFlowSpec.groovy | Verify - Drawing flow tests |
| Test | runtime/component/TradeFinance/src/test/groovy/moqui/trade/finance/TradeFinanceCbsSpec.groovy | Verify - CBS integration tests |
| Test | runtime/component/TradeFinance/src/test/groovy/moqui/trade/finance/TradeFinancePhase2Spec.groovy | Verify - Phase 2 tests |
| Test | runtime/component/TradeFinance/src/test/groovy/moqui/trade/finance/TradeFinancePhase3Spec.groovy | Verify - Phase 3 tests |
| Test | runtime/component/TradeFinance/src/test/groovy/moqui/trade/finance/TradeFinancePhase4Spec.groovy | Verify - Phase 4 tests |

### 7.5 Configuration Files
| Type | Path | Action |
|------|------|--------|
| Config | runtime/component/TradeFinance/component.xml | Verify - Component definition |
| Config | runtime/component/TradeFinance/MoquiConf.xml | Verify - Moqui configuration |

---

## 8. Status Enumerations (To be added to data seed)

### 8.1 LC Lifecycle Statuses
| Status ID | Description | Sequence |
|-----------|-------------|----------|
| LcLfDraft | Draft | 1 |
| LcLfApplied | Applied | 2 |
| LcLfIssued | Issued | 3 |
| LcLfAdvised | Advised | 4 |
| LcLfAmended | Amended | 5 |
| LcLfNegotiated | Negotiated | 6 |
| LcLfRevoked | Revoked | 7 |
| LcLfExpired | Expired | 8 |
| LcLfClosed | Closed | 9 |

### 8.2 Transaction Statuses
| Status ID | Description | Sequence |
|-----------|-------------|----------|
| LcTxDraft | Draft | 1 |
| LcTxSubmitted | Submitted | 2 |
| LcTxPendingReview | Pending Review | 3 |
| LcTxPendingProcessing | Pending Processing | 4 |
| LcTxPendingApproval | Pending Approval | 5 |
| LcTxApproved | Approved | 6 |
| LcTxRejected | Rejected | 7 |
| LcTxReturned | Returned | 8 |
| LcTxCancelled | Cancelled | 9 |
| LcTxClosed | Closed | 10 |

### 8.3 Drawing Statuses
| Status ID | Description | Sequence |
|-----------|-------------|----------|
| LcDrReceived | Received | 1 |
| LcDrCompliant | Compliant | 2 |
| LcDrDiscrepant | Discrepant | 3 |
| LcDrAccepted | Accepted | 4 |
| LcDrUnderTrust | Under Trust | 5 |
| LcDrPaid | Paid | 6 |
| LcDrRejected | Rejected | 7 |

### 8.4 Provision Statuses
| Status ID | Description |
|-----------|-------------|
| LcPrvDraft | Draft |
| LcPrvHeld | Held |
| LcPrvActive | Active |
| LcPrvReleased | Released |

### 8.5 Amendment Confirmation Statuses
| Status ID | Description |
|-----------|-------------|
| LcAmndPending | Pending |
| LcAmndConfirmed | Confirmed |
| LcAmndRejected | Rejected |

### 8.6 Provision Collection Statuses
| Status ID | Description |
|-----------|-------------|
| LcPrvColDraft | Draft |
| LcPrvColComplete | Complete |
| LcPrvColCollected | Collected |
| LcPrvColReleased | Released |

### 8.7 Provision Collection Entry Statuses
| Status ID | Description |
|-----------|-------------|
| LcPrvEntryPending | Pending |
| LcPrvEntryCollected | Collected |
| LcPrvEntryReleased | Released |
| LcPrvEntryFailed | Failed |

---

## 9. Dependencies and Integration Points

### 9.1 Internal Dependencies
- Moqui Framework Core
- Mantle UDM (User Data Model)
- Mantle USL (User Security Layer)
- Mantle Request (Workflow)
- Mantle Party (Parties and Contacts)
- Mantle Account (Invoicing and Accounting)
- Moqui Security (Users and Permissions)

### 9.2 External Integration Points
- **Core Banking System (CBS)**: 
  - Credit limit checking
  - Funds holding/releasing for provisions
  - Accounting entry posting
  - Exchange rate retrieval
  - Payment processing
- **SWIFT Network**:
  - MT700 generation for LC issuance
  - MT707 generation for amendments
  - MT734 generation for discrepancies
  - MT799 generation for revocations
  - MT750/MT754/MT756 for payment advice
- **Email/SMS Gateway**: For notifications
- **Document Management System**: For LC document storage and retrieval

### 9.3 Data Flow
1. LC Application → Validation → Request Creation → Workflow Routing
2. Approval → Provision Calculation → CBS Hold → Charge Calculation
3. Issuance → Contingent Accounting → SWIFT MT700 → LC Activation
4. Amendments → Shadow Record → Approval → SWIFT MT707 → LC Update
5. Document Presentation → Examination → Discrepancy Handling → Payment Processing
6. LC Closure/Expiry/Revocation → Provision Release → Accounting Reversal → Notifications

---

## 10. Non-Functional Requirements Addressed

### 10.1 Security
- Role-based access control (TF_ADMIN, TF_MAKER, TF_CHECKER, TF_VIEWER)
- Granular permissions on entities, services, and screens
- Audit trails for all LC modifications via LcHistory
- Secure handling of sensitive data (account information, collateral details)

### 10.2 Performance
- Efficient entity relationships with proper indexing
- Caching enabled for static data (enumerations, status items)
- Asynchronous processing for SWIFT generation and notifications
- Database connection pooling through Moqui framework

### 10.3 Scalability
- Modular design allowing independent scaling of services
- Stateless services where possible for horizontal scaling
- Efficient querying with view entities for search screens
- Batch processing capabilities for scheduled jobs

### 10.4 Maintainability
- Clear separation of concerns (entities, services, screens)
- Consistent naming conventions and patterns
- Comprehensive documentation in BRD and TSD
- Test-driven development approach with Spock specifications

### 10.5 Compliance
- UCP 600 rules enforcement through validation and workflows
- SWIFT MTxxx message format compliance
- Audit trail completeness for regulatory requirements
- Data integrity constraints and validation rules

---

**Last Updated**: 2026-03-16
