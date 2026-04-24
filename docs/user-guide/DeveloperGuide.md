# Developer Guide: Digital Trade Finance

Welcome to the Digital Trade Finance platform development guide. This document provides core architectural details and comprehensive testing protocols for maintainers and contributors.

---

## Architecture Overview
The platform follows a headless Moqui backend architecture with a React-based Single Page Application (SPA). The primary domain logic resides in the `TradeFinance` component (`runtime/component/TradeFinance`).

---

## Core Components
// ... (omitting for brevity in this thought, but I'll write the full content in the tool call)

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
- **`trade.LimitServices.calculate#Earmark`**:
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
- **`trade.ImportLcValidationServices.evaluate#Drawing`**:
  - Enforces tolerance percentage rules during the drawing process.
  - Checks `claimAmount` against `lcAmount` + `tolerancePositive`.

### 3. Authorization System
Implements a strict Maker/Checker (Four-eyes principle) matrix.

#### Services
- **`trade.AuthorizationServices.authorize#Instrument`**:
  - Enforces that the `makerUserId` cannot be the same as `checkerUserId`.
  - Determines if additional approvals are needed based on customer security tiers.

### 4. Advanced Domain Services (Phase 4 Delivery)

#### Shipping Guarantees
- **`trade.ImportLcServices.create#ShippingGuarantee`**:
  - Handles 110% earmarking of invoice amount against facility.
  - Generates `ImportLcShippingGuarantee` records.

#### Document Examination
- **`trade.ImportLcValidationServices.validate#Presentation`**:
  - Checks for Expiry Date and Drawing Amount discrepancies.
  - Flags `TradeDocumentPresentation` records as `isDiscrepant`.

#### SWIFT Generation
- **`trade.SwiftGenerationServices.generate#Mt700`**:
  - Constructs functional MT700 blocks for instrument issuance.
  - Stores output in the `SwiftMessage` entity.

#### Accounting (Conventional)
- **`trade.TradeAccountingServices.post#TradeEntry`**:
  - Posts atomic GL entries to Moqui's Mantle ledger.
  - Supports standard LC Fee and Commission debit/credit legs.

#### Compliance
- **`trade.TradeComplianceServices.check#Sanctions`**:
  - Standardized proxy interface for external sanctions screening.

## Integration & Extension
- **REST API**: Services are exposed via `/rest/s1/trade` using the `<id>` tag pattern for robust path parameter matching.
- **Data Access**: Complex joins are implemented via **Manual Groovy Joins** (e.g., `ImportLcServices.xml`) to bypass brittle ORM registry resolution failures.
- **Headless Facade**: Located at `service/trade.rest.xml`.

## Testing & Verification Protocols
The platform implements a three-layered defense strategy to ensure functional integrity.

### 1. Backend Integration Tests (Spock)
Focuses on service-level transactional integrity and database state.
- **Execution**:
  ```bash
  ./gradlew :runtime:component:TradeFinance:test
  ```
- **Key Specs**:
  - `EndToEndImportLcSpec`: Full Issuance lifecycle.
  - `LimitServicesSpec`: Facility earmarking logic.
  - `ShippingGuaranteeSpec`: 110% multiplier logic.

### 2. Frontend Component Tests (Jest)
Mocks API responses to verify complex UI states and error handling.
- **Location**: `frontend/src/components/*.test.tsx`
- **Execution**:
  ```bash
  cd frontend
  npm test
  ```

### 3. End-to-End (E2E) Integration Tests (Playwright)
Simulates real user behavior in a browser against the live Moqui API.
- **Location**: `frontend/e2e/IssuanceFlow.spec.ts`
- **Execution**:
  1. Ensure backend is running (`./gradlew run`).
  2. Ensure frontend is running (`npm run dev`).
  3. Run tests:
     ```bash
     cd frontend
     npx playwright test
     ```

## Error Handling & Logging
The system uses standard Moqui error reporting. Instead of raw Java stack traces, business validation failures returning `error="true"` will manifest in the console/logs as:
- An `ERROR` entry from `o.moqui.i.c.MessageFacadeImpl` containing the business error message.
- A `WARN` entry from `o.moqui.i.c.TransactionFacadeImpl` indicating a transaction rollback.

This is the **intended behavior** to ensure traceability of business rule violations while keeping the logs free of unhandled script exception noise.
