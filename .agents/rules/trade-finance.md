---
paths:
  - "runtime/component/TradeFinance/**/*"
---

# TradeFinance Component-Specific Rules

## 1. Package & Directory Structure

### Service Package
- **Package Name**: `moqui.trade.finance`
- **Location**: `service/moqui/trade/finance/*.xml`
- The directory path must match the package structure

### Entity Package
- **Package Name**: `moqui.trade.finance`
- **Location**: `entity/TradeFinanceEntities.xml` (single consolidated file)

### Test Package
- **Package Name**: `moqui.trade.finance`
- **Location**: `src/test/groovy/moqui/trade/finance/`

---

## 2. Entity Naming Conventions

### Lc Prefix Pattern
All TradeFinance entities MUST use the `Lc` prefix:
- `LetterOfCredit` - Master LC entity
- `LcAmendment` - LC amendments
- `LcDrawing` - LC drawings
- `LcHistory` - Audit trail
- `LcCharge` - Fees and charges
- `LcProvision` - Collateral/guarantees
- `LcDocument` - Required documents
- `LcProduct` - LC product configuration

### SWIFT Field Naming
Use SWIFT MT700 field codes as suffixes:
- `formOfCredit_40A` - Form of Credit (40A)
- `applicableRules_40E` - Applicable Rules (40E)
- `expiryDate_31D` - Expiry Date (31D)
- `portOfLoading_44E` - Port of Loading (44E)
- `descriptionOfGoods_45A` - Description of Goods (45A)

---

## 3. Status Flow Pattern

TradeFinance uses **two parallel status flows**:

### LcLifecycle (Instrument State)
Tracks the actual LC state:
- `LcDraft`, `LcIssued`, `LcAmended`, `LcCancelled`, `LcExpired`

### LcTransaction (Processing State)
Tracks internal workflow:
- `LcTxDraft`, `LcTxSubmitted`, `LcTxApproved`, `LcTxRejected`

Both must be defined as separate relationships with different titles:
```xml
<relationship type="one" title="LcLifecycle" related="moqui.basic.StatusItem">
    <key-map field-name="lcStatusId"/></relationship>
<relationship type="one" title="LcTransaction" related="moqui.basic.StatusItem">
    <key-map field-name="transactionStatusId"/></relationship>
```

---

## 4. Data File Organization

Use numbered prefixes for load order:
- `10_TradeFinanceData.xml` - Enumerations, StatusItems
- `20_TradeFinanceSecurityData.xml` - Security, permissions
- `30_TradeFinanceDemoData.xml` - Demo/test data

---

## 5. Service Organization

Group services by domain:
- `TradeFinanceServices.xml` - Core LC operations
- `AmendmentServices.xml` - Amendment logic
- `DrawingServices.xml` - Drawing/disbursement
- `LifecycleServices.xml` - Status transitions
- `SwiftServices.xml` - SWIFT message generation
- `FinancialServices.xml` - Charges, accounting
- `AccountingServices.xml` - GL integration
- `NotificationServices.xml` - Alerts/emails

---

## 6. Screen Organization

TradeFinance screens are organized by LC type:
```
screen/TradeFinance/
├── ImportLc/
│   ├── Lc/MainLC.xml
│   ├── Lc/FindLc.xml
│   ├── Lc/Amendments.xml
│   ├── Lc/Drawings.xml
│   ├── Lc/Financials.xml
│   └── Amendment/
├── ExportLc/
│   └── ...
├── ExportCollection/
├── ImportCollection/
└── Home.xml
```

---

## 7. Test Naming Conventions

Test specs MUST follow this pattern:
- `TradeFinanceSuite.groovy` - Test suite aggregator
- `TradeFinanceScreensSpec.groovy` - Screen rendering tests
- `TradeFinanceServicesSpec.groovy` - Service unit tests
- `TradeFinanceLifecycleSpec.groovy` - Status transition tests
- `TradeFinanceAmendmentSpec.groovy` - Amendment flow tests
- `TradeFinanceDrawingFlowSpec.groovy` - Drawing flow tests

---

## 8. Key Business Rules

### Shadow Copy Pattern
Amendments MUST create a shadow copy of the current LC state before modification.

### Two-Phase Commit
LC issuance requires:
1. Create LC record with `LcTxDraft` status
2. Call CBS service to issue
3. Update status to `LcIssued` on success

### Forced Transactions
Financial side effects (charges, accounting) must use `transaction="force"` to persist even if main transaction rolls back.

---

## 9. References

- General Moqui rules: `.agents/rules/moqui-*.md`
- Testing patterns: `.agents/knowledge/moqui-testing.md`
- Entity patterns: `.agents/knowledge/moqui-entity-patterns.md`
- UI patterns: `.agents/knowledge/moqui-ui-patterns.md`
