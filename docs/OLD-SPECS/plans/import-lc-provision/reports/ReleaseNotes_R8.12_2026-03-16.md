# Release Notes: R8.12 LC Provision Collection

**Version:** 1.0.0
**Date:** 2026-03-16
**Feature:** Import LC Multi-Account Provision Collection
**Status:** RELEASED

---

## Overview

This release implements the LC Provision Collection feature (R8.12), enabling multi-currency provision collection from multiple accounts for Import Letters of Credit.

---

## Features Implemented

### 1. Entity Definitions
- **LcProvisionCollection**: Main entity for tracking provision collections
  - Fields: collectionId, lcId, targetProvisionAmount, targetCurrencyUomId, collectedAmount, collectionStatusId, collectionDate, collectedByUserId, remarks
- **LcProvisionCollectionEntry**: Individual account entries within a collection
  - Fields: collectionId, entrySeqId, partyId, accountId, sourceCurrencyUomId, sourceAmount, exchangeRate, convertedAmount, convertedCurrencyUomId, rateFetchedDate, entryStatusId, cbsHoldReference, cbsHoldDate, remarks

### 2. Status Enumerations
- **Collection Statuses**: Draft, Complete, Collected, Released, Failed
- **Entry Statuses**: Pending, Collected, Released, Failed
- StatusFlow transitions configured for lifecycle management

### 3. Services
| Service | Description |
|---------|-------------|
| `create#LcProvisionCollection` | Initialize a new provision collection for an LC |
| `add#CollectionEntry` | Add account entry with currency conversion |
| `validate#CollectionTotal` | Validate total against target with ±0.01 tolerance |
| `collect#ProvisionFunds` | Execute CBS holds for all entries with rollback on failure |
| `release#ProvisionCollection` | Release CBS holds for collected entries |

### 4. Multi-Currency Support
- Exchange rates fetched from CBS Integration Service
- Supported currencies: USD, EUR, GBP
- Real-time conversion calculation

### 5. CBS Integration
- Extended exchange rate service with EUR, GBP rates
- Simulator support for testing
- Hold/Release functionality with transaction safety

### 6. User Interface
- **Location**: `ImportLc/Lc/Financials.xml` (Provision Collection Section)
- **Components**:
  - "Collect Provision" button to initiate collection
  - Collection summary panel (Target, Collected, Status)
  - Collection entries table with account/currency/amount details
  - Add Entry dialog with account dropdown and currency selection
  - Action buttons: Validate, Collect Funds, Release Funds
- **Status Indicators**: Color-coded chips (Grey=Draft, Yellow=Complete, Green=Collected, Blue=Released, Red=Failed)

---

## Technical Details

### Database Changes
- Added 2 new entities to `TradeFinanceEntities.xml`
- Added 2 new status types and 9 status items to `10_TradeFinanceData.xml`

### Service Changes
- Created `ProvisionCollectionServices.xml` (5 services)
- Updated `CbsIntegrationServices.xml` (exchange rates)
- Updated `CbsSimulatorServices.xml` (authentication)

### Screen Changes
- Modified `ImportLc/Lc/Financials.xml` - Added Provision Collection section
- Added 5 transitions for collection operations

### Test Coverage
- **Backend Tests**: 10 Spock tests (100% passing)
- **Screen Tests**: 12 test scenarios created
- Coverage: Core BDD scenarios (Groups 1-6, 16)

---

## Breaking Changes
None.

---

## Known Limitations
- CBS real integration not tested (uses Simulator)
- Partial failure scenarios not fully tested

---

## Dependencies
- TradeFinance Component
- Mantle Framework
- CBS Simulator

---

## Rollback Instructions
1. Remove entity definitions from `TradeFinanceEntities.xml`
2. Remove status enumerations from `10_TradeFinanceData.xml`
3. Delete `ProvisionCollectionServices.xml`
4. Revert changes to `CbsIntegrationServices.xml` and `CbsSimulatorServices.xml`

---

**Release Manager:** OpenCode Agent
**Approved:** 2026-03-16
