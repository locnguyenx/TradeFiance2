# Test Specification: Trade Finance Platform
**Component:** TradeFinance  
**Module:** Import Letter of Credit / Common Module  
**Version:** 1.1 (Production Baseline)  
**Update Date:** 2026-04-23  
**Status:** Active  

---

## 1. Test Architecture & Strategy

The platform utilizes a structured, 5-layer testing strategy designed for 100% BDD scenario traceability. Every business requirement is verified at its most appropriate technical level to maximize isolation and deterministic outcomes.

### 1.1 Strategy by Layer

| Level | Layer | Framework | Focus |
|---|---|---|---|
| **L1** | **Backend Unit/Entity Logic** | Moqui + Spock | Base data models, constraints, and relational integrity. |
| **L2** | **Backend Service Logic** | Moqui + Spock | ACID integrity, mathematical accuracy, error boundary guards. |
| **L3** | **API & Orchestration** | Moqui + Spock | REST compliance, auth permissions, and state-machine flows. |
| **L4** | **Frontend Component Logic** | Jest / RTL | Interaction states, client validation, and local rules. |
| **L5** | **UI/UX End-to-End** | Playwright | Full browser workflows and critical path navigation. |

---

## 2. Test Suite Catalog

### 2.1 Layers 1-3: Backend Service & Integration Tests (9 Suites)
**Location:** `runtime/component/TradeFinance/src/test/groovy/moqui/trade/finance/*.groovy`  
**Execution:** `./gradlew test` (Triggers Moqui Spock harness)

| Target | Test Specification Class | Layer |
|---|---|---|
| **Entities** | `TradeCommonEntitiesSpec.groovy` | **L1** |
| **Entities** | `ImportLcEntitiesSpec.groovy` | **L1** |
| **Services** | `ImportLcValidationServicesSpec.groovy` | **L2** |
| **Services** | `AuthorizationServicesSpec.groovy` | **L2** |
| **Services** | `LimitServicesSpec.groovy` | **L2** |
| **API** | `RestApiEndpointsSpec.groovy` | **L3** |
| **Orchestration** | `EndToEndImportLcSpec.groovy` | **L3** |
| **Orchestration** | `ShippingGuaranteeSpec.groovy` | **L3** |

### 2.2 Layer 4: Frontend Component Tests (29 Suites)
**Location:** `frontend/src/components/*.test.tsx`  
**Execution:** `npm test` (Triggers Jest with `jsdom`)

| Test Suite (Logical) | Verification Focus |
|---|---|
| `IssuanceStepper.test.tsx` | 5-Step horizontal movement & validation triggers. |
| `SwiftValidation.test.tsx` | X-Character set filtering and MT700/707 block logic. |
| `LimitEnforcement.test.tsx` | UI-side over-draw blocks & facility threshold warnings. |
| `RegulatoryVietnam.test.tsx` | Regional regulatory tagging logic specifically for Branch VN. |
| `CommonEntities.test.tsx` | Base Instrument attribute enforcement and KYC rules. |

### 2.3 Layer 5: UI/UX E2E Tests (4 Suites)
**Location:** `frontend/e2e/*.spec.ts`  
**Execution:** `npm run test:e2e` (Triggers Playwright)

| Test Script | Target Requirement |
|---|---|
| `NavigationIntegrity.spec.ts`| Verifies all 14 sidebar routes render correct "Clean Slate 50" headers. |
| `AuthorizationsFlow.spec.ts`| Verifies Checker Dashboard highlighting of Delta JSON amendments. |
| `IssuanceFlow.spec.ts` | Complete browser-driven Issuance to Submit flow. |

---

## 3. Data Strategy

The testing framework uses a dedicated set of seed and demo data to ensure repeatability.

| Data Type | Source File | Purpose |
|---|---|---|
| **Seed** | `TradeClauseSeedData.xml` | Standard UCP 600 clauses. |
| **User/Auth** | `TradeFinanceAuthData.xml` | Authorizer Tiers and Group memberships. |
| **Users** | `TradeFinanceUsers.xml` | 100% trace of Tiers 1-4 for Checker testing. |
| **Mock Logic** | `src/components/NotificationLogic.test.tsx` | Localized notification event triggers for limit breaches. |

---

## 4. Execution Protocol

```bash
# Backend Full Verification (Moqui)
./gradlew cleanTest :runtime:component:TradeFinance:test

# Frontend Logic Tests (Jest)
cd frontend && npm test

# Frontend E2E Tests (Playwright)
cd frontend && npm run test:e2e
```

## 5. Traceability Matrix Highlights
As verified in the `VerificationReport.md` (2026-04-22), the platform maintains **100% BDD Coverage** across all core Import LC and Common Framework scenarios.
- **Import LC Lifecycle**: Verified via `ImportLcFlow.test.tsx` and `EndToEndImportLcSpec.groovy`.
- **Goverance & Audit**: Verified via `AuthorizationRoles.test.tsx` and `AuthVerificationSpec.groovy`.
- **Limit/Margin**: Verified via `IssuanceLogic.test.tsx` and `LimitEnforcement.test.tsx`.
