# System Context & Constraints

## Overview
This project is to build a TradeFinance system

## Project dir
- **Project Root:** `~/myprojects/moqui-trade/`
- **Working Component (TradeFinance component):**: `<Project Root>/runtime/component/TradeFinance/`

## SCOPE LOCKDOWN (CRITICAL)
You are allowed to edit files only in the `TradeFinance` component.
**FORBIDDEN**: You are STRICTLY FORBIDDEN from modifying any files outside of the `TradeFinance` component, by any file manipulation command/tool, regardless of mode

**Exception**:
- Use gradle tasks for database operations

**Git Boundary**: All commits must target only the TradeFinance component directory.

## GIT WORKFLOW

### Pre-commit Checks
- Run `<Project Root>/gradlew test` for affected components
- Validate XML files with xmllint
- Check for TODO/FIXME comments that should be addressed
- Verify no debug logging statements remain in production code

# TradeFinance Architecture Guide

This document provides essential information for developing, testing, and troubleshooting the TradeFinance component.

---

## 1. Framework & Tech Stack

| Component | Version/Detail |
|-----------|----------------|
| **Moqui Framework** | v4.0.0 (Feb 2026) |
| **Java** | Version 21 |
| **Groovy** | Version 5 |
| **Build** | Gradle |
| **Test** | JUnit 5 + Spock |
| **Database** | H2 (dev), PostgreSQL (prod) |

### Component Dependencies

```xml
<depends-on name="mantle-usl"/>
<depends-on name="SimpleScreens"/>
```

---

## 2. Project Structure

### Root Project (`~/myprojects/moqui-trade/`)

```
root/
├── framework/                  # Moqui Framework 4.0.0 (Java/Groovy)
├── runtime/
│   ├── base-component/       # webroot, tools (shared infrastructure)
│   ├── component/
│   │   ├── mantle-udm       # Universal Data Model (Party, Contact, etc.)
│   │   ├── mantle-usl       # Universal Service Layer (core services)
│   │   ├── SimpleScreens   # UI framework
│   │   └── TradeFinance    # THIS COMPONENT
```

### TradeFinance Component

```
TradeFinance/
├── component.xml             # Component definition (version 1.0.0)
├── build.gradle             # Build config (depends on framework)
├── MoquiConf.xml           # Screen registration
├── entity/
│   └── TradeFinanceEntities.xml    # All entities (single file)
├── service/
│   └── moqui/trade/finance/     # All services
│       ├── TradeFinanceServices.xml
│       ├── AmendmentServices.xml
│       ├── DrawingServices.xml
│       ├── LifecycleServices.xml
│       ├── SwiftServices.xml
│       ├── FinancialServices.xml
│       ├── ProvisionCollectionServices.xml
│       └── ...
├── screen/
│   └── TradeFinance/
│       ├── ImportLc/
│       │   ├── Lc/           # Main LC screens
│       │   ├── Amendment/    # Amendment screens
│       │   └── Drawing/      # Drawing screens
│       ├── ExportLc/
│       ├── ImportCollection/
│       └── ExportCollection/
├── data/
│   ├── 10_TradeFinanceData.xml       # Enumerations, StatusItems
│   ├── 20_TradeFinanceSecurityData.xml # Security, permissions
│   └── 30_TradeFinanceDemoData.xml # Demo/test data
├── template/
│   └── lc/
│       ├── CreateLc.xml
│       ├── CreateAmendment.xml
│       └── ...
└── src/test/groovy/
    └── moqui/trade/finance/
        ├── TradeFinanceSuite.groovy
        ├── TradeFinanceServicesSpec.groovy
        ├── TradeFinanceScreensSpec.groovy
        ├── TradeFinanceLifecycleSpec.groovy
        └── ...
```

---

## 3. Three-Layer Architecture

```
┌─────────────────────────────────────┐
│ Screen Layer (UI)                    │
│ XML screens with form-list,           │
│ entity-find, transitions          │
├─────────────────────────────────────┤
│ Service Layer                     │
│ XML definitions + Groovy scripts   │
│ verb#noun service actions          │
├─────────────────────────────────────┤
│ Entity Layer                     │
│ XML entity definitions             │
│ CRUD via ec.entity              │
└─────────────────────────────────────┘
```

---

## 4. Key Entities

### Master Entity

| Entity | Purpose |
|--------|---------|
| `LetterOfCredit` | Master LC record with ~50 SWIFT MT700 fields |

### Detail Entities

| Entity | Purpose |
|--------|---------|
| `LcAmendment` | Amendment requests and changes |
| `LcDrawing` | Document presentations |
| `LcCharge` | Fees and commissions |
| `LcProvision` | Collateral/guarantees |
| `LcHistory` | Audit trail |
| `LcDocument` | Attachments |

### Support Entities

| Entity | Purpose |
|--------|---------|
| `LcProduct` | LC product configuration |
| `LcProductCharge` | Product-specific charges |
| `LcProvisionCollection` | Multi-account provision collection |
| `CbsSimulatorState` | CBS simulation state |

---

## 5. Dual Status Architecture

TradeFinance uses **two parallel status flows**:

| Status Flow | Field | Tracks |
|-----------|-------|--------|
| `LcLifecycle` | `lcStatusId` | What the LC is (Draft, Issued, Amended, Cancelled) |
| `LcTransaction` | `transactionStatusId` | Where it is in processing (Draft, Submitted, Approved, Rejected) |

---

## 6. Key Integration Points

| System | Integration Method | Key Services |
|--------|-----------------|--------------|
| **CBS** | Adapter pattern (Mock/Simulator) | `CbsIntegrationServices` |
| **SWIFT** | MT700/MT734 message generation | `SwiftServices.generate#SwiftMt700` |
| **Mantle USL** | Entity relationships | Party, Request, Invoice |

### CBS Adapter Pattern

Routes to `CbsMockServices` or `CbsSimulatorServices` based on system property `cbs.integration.impl`.

```groovy
def cbsImpl = ec.conf.getString('cbs.integration.impl', 'mock')
ec.service.sync().name("Cbs${cbsImpl}#${action}").call([...])
```

---

## 7. Service Organization

| Service File | Purpose |
|--------------|---------|
| `TradeFinanceServices.xml` | Core CRUD, validation |
| `AmendmentServices.xml` | Amendment workflow |
| `DrawingServices.xml` | Document presentation |
| `LifecycleServices.xml` | Status transitions |
| `SwiftServices.xml` | SWIFT messages |
| `FinancialServices.xml` | Charges, provisions |
| `CbsIntegrationServices.xml` | CBS adapter |

---

## 8. Key Development Commands

### Run Tests

```bash
cd /Users/me/myprojects/moqui-trade
./gradlew test
```

### Validate XML

```bash
xmllint --noout runtime/component/TradeFinance/screen/TradeFinance/ImportLc.xml
```

---

## 9. Naming Conventions

### Entities

- Use `Lc` prefix: `LetterOfCredit`, `LcAmendment`, `LcDrawing`
- SWIFT fields use suffix: `formOfCredit_40A`, `expiryDate_31D`

### Services

- Follow `verb#noun` pattern: `create#LetterOfCredit`, `update#LcAmendment`

### Screens

- Relative paths: `../../Lc/MainLC` (no leading double-slash)
- Back link: `${lastScreenUrl ?: '.'}`

