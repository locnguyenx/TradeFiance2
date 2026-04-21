# TradeFinance Component

## Entities (16)

| Entity | Description |
|--------|-------------|
| `LetterOfCredit` | Master LC record |
| `LcAmendment` | Amendment records |
| `LcAmendmentLock` | Amendment locking |
| `LcCharge` | LC charges |
| `LcDrawing` | Drawing records |
| `LcDiscrepancy` | Discrepancy tracking |
| `LcDocument` | Documents |
| `LcProvision` | Provisions |
| `LcProduct` | LC products |

## Key Services

| Service | Description |
|---------|-------------|
| `create#LetterOfCredit` | Create LC |
| `issue#LetterOfCredit` | Issue LC |
| `submit#LetterOfCredit` | Submit for approval |
| `approve#LcByTradeSupervisor` | Supervisor approval |
| `transition#LcStatus` | Status transitions |
| `create#LcAmendment` | Create amendment |
| `approve#LcAmendment` | Approve amendment |
| `acquire#AmendmentLock` | Lock for amendment |
| `generate#SwiftMt700` | SWIFT MT700 message |
| `generate#SwiftMt707` | SWIFT MT707 (amendment) |

## Status Flows

- `LcLifecycle`: Draft → Issued → Amended → Expired
- `LcTransaction`: Draft → Submitted → Approved → Active

## Test Specs (20)

- `TradeFinanceServicesSpec` - Service layer
- `TradeFinanceAmendmentSpec` - Amendment workflow
- `TradeFinanceSwiftSpec` - SWIFT messages
- `TradeFinanceCbsSpec` - CBS integration
