# Trade Finance Backend User Guide

Welcome to the Trade Finance backend system. This guide provides technical documentation for the core entities and services implemented within the `TradeFinance` Moqui component.

## Architecture Overview
The system is built as a native Moqui component located at `runtime/component/TradeFinance`. It follows a decoupled architecture, separating common trade operations from specialized product logic (like Import LC).

## Core Components

### 1. Common Framework
The common framework manages shared trade entities and cross-cutting services.

#### Entities
- **TradeInstrument**: Base entity for all trade products (LCs, Guarantees, etc.).
  - `instrumentId`: Unique identifier (Primary Key).
  - `transactionRef`: External reference number.
  - `baseEquivalentAmount`: Value in base currency.
- **CustomerFacility**: Tracks approved limits and utilization per customer.
  - `facilityId`: Unique ID.
  - `totalApprovedLimit`: Maximum allowed limit.
  - `utilizedAmount`: Currently used amount.

#### Services
- **`trade.finance.LimitServices.calculate#Earmark`**:
  - Validates if a requested amount exceeds the customer's available facility limit.
  - Automatically updates the `utilizedAmount` upon success.
  - Throws a standard Moqui error if limits are exceeded.

### 2. Import Letter of Credit (LC) Module
Specialized logic for managing Import Letters of Credit.

#### Entities
- **ImportLetterOfCredit**: Extends `TradeInstrument` with LC-specific metadata.
  - `tolerancePositive`/`toleranceNegative`: Percentage bounds for drawings.
  - `businessStateId`: Lifecycle stage (e.g., DRAFT, ISSUED).

#### Services
- **`trade.finance.ImportLcValidationServices.evaluate#Drawing`**:
  - Enforces tolerance percentage rules during the drawing process.
  - Checks `claimAmount` against `lcAmount` + `tolerancePositive`.

### 3. Authorization System
Implements a strict Maker/Checker (Four-eyes principle) matrix.

#### services
- **`trade.finance.AuthorizationServices.evaluate#MakerCheckerMatrix`**:
  - Enforces that the `makerUserId` cannot be the same as `checkerUserId`.
  - Determines if additional approvals are needed based on customer security tiers.

## Integration & Extension
- **REST API**: Services are configured for headless consumption via Moqui's REST facade.
- **SWIFT**: State transitions are ready for mapping to SWIFT MX (CBPR+) messages (MT700/MT707).

## Verification
To run the full suite of backend tests:
```bash
./gradlew :runtime:component:TradeFinance:test
```

## Error Handling & Logging
The system uses standard Moqui error reporting. Instead of raw Java stack traces, business validation failures returning `error="true"` will manifest in the console/logs as:
- An `ERROR` entry from `o.moqui.i.c.MessageFacadeImpl` containing the business error message.
- A `WARN` entry from `o.moqui.i.c.TransactionFacadeImpl` indicating a transaction rollback.

This is the **intended behavior** to ensure traceability of business rule violations while keeping the logs free of unhandled script exception noise.
