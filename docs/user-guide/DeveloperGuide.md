# Getting Started Guide: Digital Trade Finance

This guide provides the necessary steps to set up, run, and access the Digital Trade Finance platform for development and local evaluation.

---

## 1. Prerequisites
Ensure the following are installed on your system:
- **OpenJDK 17+**: Required for Moqui Framework.
- **Node.js 18+ & npm**: Required for the React SPA.
- **Git**: For repository management.

---

## 2. Backend Setup (Moqui)
The backend is a headless service providing the `/rest/s1/trade` API.

### Environment Preparation
1. **Load Seed Data**: Provision standard roles, users, and trade templates.
   ```bash
   ./gradlew load
   ```
2. **Start the Server**:
   ```bash
   ./gradlew run
   ```
   - The backend will be available at `http://localhost:8080`.
   - Access Moqui Admin Tools at `http://localhost:8080/vroot`.

---

## 3. Frontend Setup (React SPA)
The frontend is a modern React application located in the `/frontend` directory.

### Installation & Execution
1. **Install Dependencies**:
   ```bash
   cd frontend
   npm install
   ```
2. **Start the Development Server**:
   ```bash
   npm run dev
   ```
   - The application will be available at `http://localhost:3000`.

---

## 4. Navigation & Layout
The platform utilizes a premium, unified navigation shell:
- **Workspace**: Access your module dashboards and the Global Checker Queue.
- **Trade Modules**: Context-sensitive menus for Import and Export workflows.
- **Master Data**: Administrative access to Parties, Facilities, and Tariffs.

---

## 5. Initial Access & Login
Use the following credentials to explore the different personas:

| persona | Username | Password |
|---------|----------|----------|
| **Maker** | `trade.maker` | `trade123` |
| **Checker** | `trade.checker` | `trade123` |
| **Backoffice** | `trade.backoffice` | `trade123` |
| **System Admin**| `trade.admin` | `trade123` |

---

## 5. Verification
The platform includes a comprehensive test suite covering backend logic, frontend components, and full E2E journeys.

### Run All Tests
1. **Backend (Spock)**: `./gradlew test` (Moqui runtime)
2. **Frontend (Jest)**: `cd frontend && npm test` (Component level)
3. **E2E (Playwright)**: `cd frontend && npx playwright test` (Full-stack)

For detailed technical patterns and test strategies, refer to the [Developer Guide](file:///Users/me/myprojects/moqui-trade/docs/user-guide/DeveloperGuide.md).

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
  # Fresh database before tests
  ./gradlew cleanDb reloadSave
  
  # Run all TradeFinance tests
  ./gradlew :runtime:component:TradeFinance:test
  ```
- **Test Specs**:
  - `BddCommonModuleSpec`: Common framework (37 tests) - entity CRUD, FX, SLA, authority tiers
  - `BddImportLcModuleSpec`: LC lifecycle (40 tests) - issuance, amendments, SWIFT
  - `ImportLcServicesSpec`: Service-specific tests (4 tests)
  - `EndToEndImportLcSpec`: Full E2E flow
  - `RestApiEndpointsSpec`: REST API contracts
  - `AuthorizationServicesSpec`: Maker/Checker matrix

### 2. Test Infrastructure Conventions

#### Database Refresh
Always use `reloadSave` (not `loadSeed`) for fresh test data:
```bash
./gradlew cleanDb reloadSave
```

#### Service Naming in Tests
Use full service paths with package prefix:
```groovy
// Correct
ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
    .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: 'USD']).call()

// Incorrect (missing package prefix)
ec.service.sync().name("ImportLcServices.create#ImportLetterOfCredit")
```

#### FK Cleanup Order in Tests
When cleaning up test data, delete child entities before parents:
```groovy
// Order: Settlement → PresentationItem → Discrepancy → Presentation → Amendment → LC → Instrument
ec.entity.find("trade.importlc.ImportLcSettlement").condition("instrumentId", id).deleteAll()
ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("instrumentId", id).deleteAll()
ec.entity.find("trade.importlc.ImportLcAmendment").condition("instrumentId", id).deleteAll()
ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", id).deleteAll()
ec.entity.find("trade.TradeInstrument").condition("instrumentId", id).deleteAll()
```

#### Test ID Ranges
Each spec uses a unique ID range to avoid conflicts:
- `BddCommonModuleSpec`: 1000000+
- `BddImportLcModuleSpec`: 3000000+
- `AuthorizationServicesSpec`: AUTH-* prefixed IDs

#### Seed Data Conflicts
Avoid creating test records with IDs that exist in seed data (e.g., `T1-01` for `UserAuthorityProfile`). Use unique IDs:
```groovy
// Use unique ID like T1-03 instead of T1-01 (which exists in TradeFinanceMasterData.xml)
ec.entity.makeValue("trade.UserAuthorityProfile")
    .setAll([authorityProfileId:"T1-03", ...]).create()
```

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
