# Comprehensive Moqui Technical Design Specification (Live Spec)
**Project Name:** Digital Trade Finance Platform
**Date:** April 23, 2026
**Version:** 2.1 (Live Production Spec - Incorporates Modernized UX)

## 1. Architectural Overview & Integration
This technical design fully maps the Business Requirements (BRD), Behavior Driven Development tests (BDD), and UI specifications directly into Moqui framework data structures, service logic, and persistence patterns. 

### 1.1 Architecture Pattern (Hybrid Headless)
* **Moqui Engine Backend:** Operates explicitly as a headless REST API (`/rest/s1/trade`). It serves as the secure core, strictly executing state-machine constraint validation, Limit/Facility manipulation, Tariff logic, SWIFT building, and database persistence.
* **Separated Frontend SPA:** A Next.js/React Single Page Application disconnected from standard Moqui XML Form rendering. It consumes the REST APIs and resolves cognitive load requirements using a modern, flat-premium "Blue Premium" design system.

### 1.2 Maker/Checker Authorization Engine Framework
* **OOTB Baseline Access Migration:** Tiers 1 through 4 correlate strictly to standard Moqui `UserGroup`s (e.g., `TRADE_APP_TIER_1` down to `TRADE_APP_TIER_4`). REST API endpoints are protected using standard `UserPermission` assignments natively attached to these groups, causing generic `403 Forbidden` limits dynamically.
* **Dual Approval & Compliance Suspensions:** Executed within `trade.finance.AuthorizationServices.evaluateMakerCheckerMatrix`. Since specific logic exceeds simple boolean routing, this service directly parses `Shadow Records` determining validity natively.
* **Segregation By Identity:** Explicit Four-Eyes principle evaluates `Instrument.createdBy` directly prohibiting UI Action execution for identical user identities inherently.
* **Sanctions Interruptions:** If compliance calls output `True` during Pre-processing validations, standard structural Maker/Checker assignments logically disconnect routing targets directly toward an isolated internal `COMPLIANCE_REVIEW_QUEUE`.

---

## 2. COMMON MODULE A: Base Framework & Master Configurations
Covers the cross-cutting global operations universally applicable for all underlying Trade Operations (Letters of Credit, Shipping Guarantees, Collections).

### 2.1 Core Business Entity Relational Schema
Extending standard Moqui structures dynamically.

* **`TradeInstrument` (Base Extensibility Parent):**
  * `instrumentId` (`id`, PK, auto-sequential)
  * `transactionRef` (`text-short`) [TF-IMP-YY-0001 System generated standard utilizing NumberSequence logic].
  * `lifecycleStatusId` (`id`) [Mapped directly into `TradeTransactionFlow`]
  * `productEnumId` (`id`) [Derived via `TradeProductType`]
  * `baseEquivalentAmount` (`number-decimal`) [Pre-computed using designated local Daily Exchange Rates].
  * `issueDate` (`date`), `expiryDate` (`date`)
  * `customerFacilityId` (`id`) [FK mapping directly into Limits Engines API].

* **`CustomerFacility` (Core Limits):**
  * `facilityId` (`id`, PK)
  * `totalApprovedLimit` (`number-decimal`)
  * `utilizedAmount` (`number-decimal`)
  * `facilityExpiryDate` (`date`)

* **`TradePartyExtent` (KYC/AML Extrapolations):**
  * Extends standard Moqui `mantle.party.Party` inserting specific explicit logic arrays capturing formal fields:
  * `isKycCleared` (`boolean`), `kycExpirationDate` (`date`), `sanctionsWarningActive` (`boolean`), `partyRoleEnumId` (`id`).

* **`TradeInstrumentAmendment` (Shadow Versioning):**
  * Inherently buffers modified states explicitly avoiding physical writes over `TradeInstrument` limits until final formal Checker approval natively updates parameters securely.

### 2.2 Currency, SLA Calendars & Notifications
* **Dual-Rate FX Mapping Strategies:**
  * `Daily Board Rate Cache:` Cached end-of-day variables natively retrieved during Pre-Processing calculations evaluating `CustomerFacility` earmarks reliably (protecting inputs uniformly).
  * `Live Spot Integration API:` Settlement components hitting explicit REST API (`TreasuryProxyServices`) natively evaluating accounting cash generation matrices explicitly mapping real-time numbers independently.
* **Single Global Banking Calendar Engine:**
  * Service explicit structure computing logic (`trade.finance.DateServices.computeSlaTime`). Inherently bypasses universal weekends and Head-Office recognized structured holiday arrays evaluating maximum SLA durations strictly returning `Target End Date` explicitly.

### 2.3 Comprehensive Catalog Master Data
Ensuring dynamic configurability structurally without system deployments.

#### 1. Tariff & Fee Mathematical Engine (`FeeConfiguration` Schema)
* **Fields:** `feeConfigurationId`, `targetEventEnumId`, `calculationTypeEnumId`, `baseValue`, `minFloorAmount`, `maxCeilingAmount`, `customerOverrideTierId`.
* **Service Executor (`trade.finance.TariffServices.calculateFee`)**: Uses logical extraction assessing standard `Base Value` metrics checking conditional outputs cleanly against `minFloorAmount`.

#### 2. The Product Configuration Catalog Matrix (`TradeProductCatalog`)
* **Fields & Attributes:** `productId`, `isTransferable`, `allowRevolving`, `allowAdvancePayment`, `maxToleranceThreshold`, `documentExamSlaRuleDays`, `defaultSwiftMtTypeEnumId`, `accountingFrameworkEnumId`, `mandatoryMarginPercent`.
* **Execution Logic Integration:** Modifies structural behavior inherently across components conditionally bypassing hard rules universally.

#### 3. Delta JSON Transaction Audit Logging
* **`TradeTransactionAudit` (Immutable Layer):**
  Every modification specifically creating immutable records mapping `timestamp` (e.g. `ec.user.nowTimestamp`), `auditId`, `userId`, `actionEnumId`, `justificationRootText`, and `snapshotDeltaJSON`.

---

## 3. IMPORT LC MODULE B: Structured Lifecycle Domain

### 3.1 Domain Extensibility Engine Data Schemas

#### `ImportLetterOfCredit` (Product Direct Inherit)
* `instrumentId` (`id`, PK) [Relational binding]
* `businessStateId` (`id`) [StatusFlow tracking UCP logic directly: `LcDraft`, `LcIssued`, `LcDocsReceived`, `LcSettled`].
* `beneficiaryPartyId` (`id`) [Foreign Link mapping `TradePartyExtent`]
* `tolerancePositive` (`number-decimal`), `toleranceNegative` (`number-decimal`)

#### `ImportLcShippingGuarantee`
Tracks 110% over-earmarked independent legal claims implicitly locking applicant capabilities.
* `guaranteeId` (`id`, PK), `instrumentId` (`id`, FK LC), `invoiceAmount` (`decimal`), `liabilityMultiplierRequired` (`integer` default 110%), `transportDocReference` (`string`).

#### `TradeDocumentPresentation`
* `presentationId` (`id`, PK), `instrumentId` (`id`), `presentationDate` (`date`).
* `claimAmount` (`number-decimal`), `isDiscrepant` (`boolean`), `applicantDecisionEnumId` (`id` [PENDING, WAIVED, REFUSED]).

### 3.2 Key Process Operations Enforcement
* **`ImportLcValidationServices.xml` `evaluateDrawingLimits`**: Restricts mathematical output parameters implicitly demanding `ClaimAmount` evaluates mathematically `<` `(LC Amount * (1 + PositiveTolerance))`.

---

## 4. SWIFT Messaging Formatter Validations
Executes strict conversion parsing ensuring data fields properly mutate out of standard generic entities flawlessly.

### 4.1 Native Output Format Services
* **`trade.finance.SwiftGenerationServices.generateMt700`:** Binds output parameters actively executing generation logics logically resolving raw entities definitively inside Prowide WIFE mapping structures natively.

---

## 5. DEDICATED FRONTEND SPA APPLICATION DESIGN (LIVE SPEC)
The Next.js/React frontend leverages a modernized UI configuration meeting REQ-UI-MOD priorities using the "Blue Premium" format.

### 5.1 Design System & Typography
- **Typography:** Uses strictly `Inter` (Google Font) natively via Next.js optimizations.
- **Aesthetic:** "Flat Premium Light" explicitly denying gradients/glows. Uses solid surfaces over var(--app-bg).
- **Core Token Set:** 
  - Nav/Sidebar Background: `#031e88`
  - Active Nav Background: `#5373fb` (Soft Blue)
  - Active Text: `#ffffff`
  - Nav Border Right Separator: `1px solid #8da2fc`
  - Primary Component Cards: `12px border-radius` with a strict `shadow-sm` depth utilizing `1px solid #e2e8f0` mapping.

### 5.2 Common & Master Data Component Architectures
- **The Global Shell (`components/GlobalShell.tsx`):**
  - Architecture: Operates a minimalist, high-density left-sidebar shell segmenting operations dynamically (`Dashboard`, `Approvals`, `Issuance`, `Document Exam`) integrating standard `lucide-react` icons matching 1.5px stroke constraints. Active menu routes evaluate automatically, turning flat soft-blue seamlessly. 
- **The Global Checker Queue:** 
  - *Pattern:* High-Density Data Grid utilizing Quick Filters nested within `12px` modern cards.
  - Interaction does not invoke hard page navigation; row clicks trigger Full-Screen Overlays contextually.
- **Party & KYC Directory:**
  - *Pattern:* Master-Detail Split View utilizing `350px` localized inner sidebars wrapping isolated component tabs.
- **Credit Limits Analytics Dashboard:**
  - *Pattern:* Live Exposure Graphic Indicators utilizing flat minimal charts natively bound to `LimitServices`.

### 5.3 Import LC Specialized Views
- **LC Issuance (MT700 Data Entry):**
  - *Pattern:* Horizontal Client-Side Stepper.
  - Divides 40+ SWIFT input fields across isolated React contexts. Local validation constraints check API limit bounds synchronously before enabling Step 5 "Submission" triggers using explicit primary blue action triggers.
- **Document Examination Workspace:**
  - *Pattern:* 50/50 Vertical Split-Pane. Left read-only wrapper representing the `ImportLetterOfCredit`. Right writable `TradeDocumentPresentation` form grid mapping ISBP codes.
- **Checker Authorization Dashboard:**
  - *Pattern:* Distinct visual comparison overlay highlighting `TradeInstrumentAmendment` differences dynamically over `shadow-sm` card surfaces mathematically mapping the DOM accurately.
