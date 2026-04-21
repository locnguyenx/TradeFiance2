# Runtime Components Symbol Overview

## Components Overview

| Component | Entities | Services | Screens |
|-----------|----------|----------|---------|
| **TradeFinance** | 16 | 69 | 23 |
| **mantle-usl** | View Entities | 790+ | - |
| **mantle-udm** | 422 | - | - |
| **SimpleScreens** | - | - | 367 |
| **base-component** | - | - | 110 |
| **moqui-fop** | - | - | - |
| **MarbleERP** | - | - | 5 |

## 1. TradeFinance Component

### Entities (16)
| Entity | Description |
|--------|-------------|
| `LetterOfCredit` | Master LC record |
| `LcAmendment` | Amendment records |
| `LcAmendmentBeneficiaryResponse` | Beneficiary responses |
| `LcAmendmentLock` | Amendment locking |
| `LcCharge` | LC charges |
| `LcDiscrepancy` | Discrepancy tracking |
| `LcDocument` | Documents |
| `LcDrawing` | Drawing records |
| `LcDrawingDocument` | Drawing documents |
| `LcHistory` | Audit history |
| `LcProduct` | LC products |
| `LcProductCharge` | Product charges |
| `LcProvision` | Provisions |
| `LcProvisionCollection` | Provision collection |
| `LcProvisionCollectionEntry` | Collection entries |
| `CbsSimulatorState` | CBS mock state |

### Services (69 across 14 files)

| File | Services |
|------|---------|
| `TradeFinanceServices.xml` | `create/validate/update/submit/return/delete#LetterOfCredit`, `approve#LcBy*`, `transition#LcStatus/TransactionStatus`, `check#CustomerCreditLimit`, `create/update/delete#LcCharge` |
| `AmendmentServices.xml` | `create/submit/approve/confirm/review#LcAmendment`, `transition#AmendmentStatus`, `acquire/release/forceRelease#AmendmentLock`, `check#AmendmentLockStatus`, `get#AmendmentHistory/EffectiveTerms`, `adjust#ProvisionsForAmendment`, `get#AllActiveLocks/LockedLcNumbers`, `expire#Locks` |
| `SwiftServices.xml` | `generate#SwiftMt700/Mt707/Mt799/Mt734`, `parse#SwiftMt700/Mt707` |
| `CbsIntegrationServices.xml` | `hold/release#Funds`, `check#CreditLimit`, `get#ExchangeRate` |
| `CbsMockServices.xml` | `hold/release#FundsMock`, `check#CreditLimitMock` |
| `CbsSimulatorServices.xml` | `hold/release#FundsSimulator`, `check#CreditLimitSimulator` |
| `LifecycleServices.xml` | `issue/revoke#LetterOfCredit` |
| `DrawingServices.xml` | `create/examine#LcDrawing`, `record/resolve#LcDiscrepancy`, `transition#DrawingStatus` |
| `FinancialServices.xml` | `calculate#LcCharges/LcChargesAndProvisions`, `hold/release#LcProvision` |
| `NotificationServices.xml` | `send#LcNotification` |
| `DocumentServices.xml` | `attach#LcDocument`, `generate#LcPdf` |
| `ProvisionCollectionServices.xml` | `create#LcProvisionCollection`, `add/validate#CollectionEntry`, `collect#ProvisionFunds`, `release#ProvisionCollection` |
| `ScheduledServices.xml` | `check#LcExpiry` |
| `AccountingServices.xml` | `post#LcChargesToInvoice` |

### Screens (23)
- `Home.xml`, `ImportLc.xml`, `ExportLc.xml`, `ImportCollection.xml`, `ExportCollection.xml`
- `ImportLc/Dashboard.xml`, `ImportLc/Lc/MainLC.xml`, `ImportLc/Lc/FindLc.xml`
- `ImportLc/Lc/Drawings.xml`, `ImportLc/Lc/Financials.xml`, `ImportLc/Lc/History.xml`
- `ImportLc/Drawing/FindDrawing.xml`, `ImportLc/Drawing/DrawingDetail.xml`
- `ImportLc/Amendment/AmendmentDetail.xml`, `ImportLc/Amendment/FindAmendment.xml`
- `ImportLc/Amendment/LockManagement.xml`, `ImportLc/Amendment/Financials.xml`

### Test Specs (20)
| Spec | Description |
|------|-------------|
| `TradeFinanceSuite` | Test suite runner |
| `TradeFinanceServicesSpec` | Service layer tests |
| `TradeFinanceAmendmentSpec` | Amendment workflow |
| `TradeFinanceAmendmentTddSpec` | Amendment TDD |
| `TradeFinanceAmendmentLockManagementSpec` | Lock management |
| `TradeFinanceAmendmentLockScreenSpec` | Lock screens |
| `TradeFinanceSwiftSpec` | SWIFT messages |
| `TradeFinanceCbsSpec` | CBS integration |
| `TradeFinanceDrawingFlowSpec` | Drawing flow |
| `TradeFinanceIssuanceSpec` | LC issuance |
| `TradeFinanceLifecycleSpec` | Lifecycle |
| `TradeFinanceNotificationSpec` | Notifications |
| `TradeFinanceAccountingSpec` | Accounting |
| `TradeFinanceProvisionChargeSpec` | Provisions/charges |
| `TradeFinanceLcProvisionCollectionSpec` | Provision collection |
| `TradeFinanceProvisionCollectionScreenSpec` | Screen tests |
| `TradeFinanceScreensSpec` | Screen integration |
| `TradeFinanceApplicationSpec` | Application |
| `TradeFinanceUiSpec` | UI tests |
| `TradeFinanceWorkflowSpec` | Workflow tests |

## 2. Mantle-USL Component

### Service Packages (12)
| Package | Description |
|---------|-------------|
| `mantle.account` | Payments, Invoicing, Reconciliation |
| `mantle.facility` | Facilities, Inventory |
| `mantle.humanres` | Payroll, Employment, Positions |
| `mantle.ledger` | Accounting, Assets |
| `mantle.order` | Sales Orders, Returns |
| `mantle.party` | Party Management, Contact |
| `mantle.product` | Products, Pricing, Promotions |
| `mantle.request` | Requests |
| `mantle.sales` | Sales, Accounts |
| `mantle.shipment` | Shipping, Carriers |
| `mantle.work` | Projects, Tasks, Manufacturing |
| `mantle.other` | Budgets, Tax |

### Top Service Files (by count)
| File | Count | Description |
|------|-------|-------------|
| `OrderServices.xml` | 52 | Order management |
| `InvoiceServices.xml` | 48 | Invoice processing |
| `PaymentServices.xml` | 46 | Payment handling |
| `AssetServices.xml` | 45 | Asset management |
| `ShipmentServices.xml` | 39 | Shipping |
| `PartyServices.xml` | 33 | Party operations |
| `LedgerServices.xml` | 33 | Ledger operations |
| `ReturnServices.xml` | 26 | Returns |
| `ProductServices.xml` | 26 | Products |
| `PayrollServices.xml` | 24 | Payroll |
| `CarrierServices.xml` | 24 | Carriers |

### View Entities (17 files)
- `OrderViewEntities.xml` - Order views
- `PartyViewEntities.xml` - Party views
- `ProductAssetViewEntities.xml` - Product/Asset views
- `AccountingLedgerViewEntities.xml` - Ledger views
- `AccountingAccountViewEntities.xml` - Account views
- `AccountingOtherViewEntities.xml` - Other accounting
- `FacilityViewEntities.xml` - Facility views
- `HumanResourcesViewEntities.xml` - HR views
- `WorkEffortViewEntities.xml` - Work effort views
- `ProductDefinitionViewEntities.xml` - Product views
- `ProductStoreViewEntities.xml` - Store views
- `RequestViewEntities.xml` - Request views
- `ShipmentViewEntities.xml` - Shipment views

### ECAS Files (12)
- `OrderReturn.secas.xml` - Order/Return events
- `Party.secas.xml` - Party events
- `ProductAsset.secas.xml` - Product/Asset events
- `AccountingLedger.secas.xml` - Ledger events
- `AccountingInvoice.secas.xml` - Invoice events
- `AccountingPayment.secas.xml` - Payment events
- `AccountingFinancial.secas.xml` - Financial events
- `Shipment.secas.xml` - Shipment events
- `WorkEffort.secas.xml` - Work effort events
- `ProductSubscription.secas.xml` - Subscription events

## 3. Mantle-UDM Component

### Entity Files (422 entities total)
| File | Entities |
|------|----------|
| `PartyEntities.xml` | Party, ContactMech, PostalAddress, TelecomNumber |
| `ProductDefinitionEntities.xml` | Product, InventoryItem |
| `OrderEntities.xml` | OrderHeader, OrderItem, OrderItemShipGroup |
| `AccountingLedgerEntities.xml` | AcctgTrans, AcctgTransEntry |
| `AccountingAccountEntities.xml` | GlAccount, GlAccountType |
| `AccountingOtherEntities.xml` | Invoice, Payment |
| `FacilityEntities.xml` | Facility, FacilityLocation |
| `ProductAssetEntities.xml` | Asset, AssetMaintenance |
| `ProductStoreEntities.xml` | ProductStore, StoreKeywordSearch |
| `HumanResourcesEntities.xml` | Employment, Position, Payroll |
| `WorkEffortEntities.xml` | WorkEffort, Project, Task |
| `ShipmentEntities.xml` | Shipment, ShipmentPackage |
| `SalesEntities.xml` | SalesOpportunity, SalesForecast |
| `RequestEntities.xml` | Request, RequestItem |
| `MarketingEntities.xml` | Marketing campaigns |

## 4. SimpleScreens Component

### Screens (367 total)
| Category | Count | Examples |
|----------|-------|----------|
| Accounting | ~80 | `Accounting.xml`, `BalanceSheet.xml`, `Aging.xml` |
| Asset | ~30 | `Asset.xml`, `AssetOnHand.xml`, `AssetMaintenance.xml` |
| Order | ~40 | `AddOrder.xml`, `OrderView.xml`, `OrderShipments.xml` |
| Product | ~50 | `Product.xml`, `PriceHistory.xml`, `Inventory.xml` |
| Facility | ~30 | `Facility.xml`, `Inventory.xml`, `Shipment.xml` |
| Party | ~40 | `Party.xml`, `FindParty.xml`, `Communication.xml` |
| WorkEffort | ~40 | `Project.xml`, `Task.xml`, `Calendar.xml` |
| Report | ~50 | Various report screens |

## 5. Moqui-FOP Component

### Classes
| Class | Description |
|-------|-------------|
| `FopToolFactory` | FOP PDF generation tool |
| `HtmlRenderer` | HTML to PDF rendering |
| `HtmlRenderServlet` | Servlet for HTML rendering |

## 6. MarbleERP Component

### Screens
- `marble.xml` - Main ERP screen
- `marble/dashboard.xml` - Dashboard

## 7. Base Component Tools

### Tools Screens (110)
| Category | Screens |
|----------|---------|
| Entity Tools | `Entity.xml`, `DataEdit.xml`, `DataImport.xml`, `DataExport.xml`, `SqlRunner.xml`, `SqlScriptRunner.xml`, `SpeedTest.xml`, `QueryStats.xml`, `TableStats.xml` |
| Service Tools | `Service.xml`, `ServiceRun.xml`, `ServiceDetail.xml`, `ServiceLoadRunner.xml` |
| Auto Screens | `AutoScreen.xml`, `AutoFind.xml`, `AutoEdit.xml`, `AutoEditDetail.xml`, `AutoEditMaster.xml` |
| System | `System.xml`, `Cache.xml`, `Security.xml`, `ArtifactGroup.xml`, `Visit.xml`, `LogViewer.xml`, `ThreadList.xml` |
| Print | `Print.xml`, `Printer.xml`, `PrintJob.xml` |
| Data Document | `DataDocument.xml`, `Index.xml`, `Search.xml`, `Export.xml` |
| Service Job | `ServiceJob.xml`, `Jobs.xml`, `JobRuns.xml` |
| System Message | `SystemMessage.xml`, `Message.xml`, `Remote.xml`, `Type.xml` |
| Instance | `Instance.xml`, `InstanceList.xml`, `InstanceDetail.xml` |
| Localization | `Localization.xml`, `EntityFields.xml`, `Messages.xml` |
| Security | `UserAccount.xml`, `UserGroup.xml`, `ActiveUsers.xml`, `VerifyTotp.xml`, `VerifySms.xml`, `VerifyEmail.xml` |
| Entity Sync | `EntitySync.xml`, `EntitySyncList.xml`, `EntitySyncDetail.xml` |
| Resources | `Resource.xml`, `ElFinder.xml` |

## Summary Statistics

| Component | Services | Entities | Screens | Test Specs |
|-----------|----------|----------|---------|------------|
| TradeFinance | 69 | 16 | 23 | 20 |
| mantle-usl | 790+ | View Entities | - | - |
| mantle-udm | - | 422 | - | - |
| SimpleScreens | - | - | 367 | - |
| base-component | - | - | 110 | - |
| Framework | 145 | 152 | 200+ | 18 |
| **TOTAL** | **1000+** | **600+** | **700+** | **38+** |
