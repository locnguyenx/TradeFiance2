# Mantle-USL Service Packages

## Package Structure

| Package | Description | Key Services |
|---------|-------------|--------------|
| `mantle.account` | Payments, Invoicing | Payment, Invoice, Reconciliation |
| `mantle.facility` | Facilities, Inventory | Facility, Contact |
| `mantle.humanres` | Payroll, Employment | Employment, Payroll, Position |
| `mantle.ledger` | Accounting, Assets | Asset, Ledger, Depreciation |
| `mantle.order` | Sales Orders, Returns | Order, Return |
| `mantle.party` | Party Management | Party, Communication |
| `mantle.product` | Products, Pricing | Product, Price, Promotion |
| `mantle.request` | Requests | Request, Content |
| `mantle.sales` | Sales, Accounts | Account, Contact |
| `mantle.shipment` | Shipping, Carriers | Shipment, Carrier |
| `mantle.work` | Projects, Tasks | Project, Task, Manufacturing |

## Service File Organization

```
service/mantle/{package}/
├── OrderServices.xml
├── InvoiceServices.xml
├── PaymentServices.xml
└── ...
```

## Top Service Files by Count

| File | Count |
|------|-------|
| `OrderServices.xml` | 52 |
| `InvoiceServices.xml` | 48 |
| `PaymentServices.xml` | 46 |
| `AssetServices.xml` | 45 |
| `ShipmentServices.xml` | 39 |
| `PartyServices.xml` | 33 |
| `LedgerServices.xml` | 33 |
