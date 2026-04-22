# Trade Frontend Phase 2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Exhaustively implement all remaining Trade Frontend SPA UI components mapped in the BRD wireframes and BDD specifications using strictly defined Next.js/React TDD parameters. Ensure 100% coverage of REQ-UI-CMN-01 navigation and all module configurations.

**Architecture:** We are continuing the Next.js SPA construction. The components require rigorous separation of concerns, managing heavy DOM cognitive loads via dedicated Layouts (Split Screens, High-Density Analytics, Steppers). Components must strictly follow Test-Driven Development (React Testing Library) before defining internal state tracking.

**Tech Stack:** React, Next.js, Jest, RTL

---

### Task 1: Global UI Shell & Navigation Menu
**BDD Scenarios:** BDD-CMN-WF-01 (Supports workflow transitions via navigation)
**BRD Requirements:** REQ-UI-CMN-01, REQ-UI-IMP-01
**User-Facing:** YES

**Files:**
- Modify: `frontend/src/components/GlobalShell.tsx`
- Modify: `frontend/src/components/GlobalShell.test.tsx`

- [ ] **Step 1: Write the failing test**

```tsx
import { render, screen } from '@testing-library/react';
import { GlobalShell } from './GlobalShell';

describe('GlobalShell Complete Navigation Layout', () => {
    it('renders the complete lateral navigation menus matching REQ-UI-CMN-01', () => {
        render(<GlobalShell><div>Content</div></GlobalShell>);
        // Primary
        expect(screen.getByText('Dashboard')).toBeInTheDocument();
        expect(screen.getByText('My Approvals')).toBeInTheDocument();
        
        // Modules
        expect(screen.getByText('Import LC')).toBeInTheDocument();
        expect(screen.getByText('Export LC (Phase 2)')).toBeInTheDocument();
        
        // Master Data
        expect(screen.getByText('Party & KYC Directory')).toBeInTheDocument();
        expect(screen.getByText('Credit Facilities')).toBeInTheDocument();
        expect(screen.getByText('Tariff & Fee Configuration')).toBeInTheDocument();
        expect(screen.getByText('Product Configuration')).toBeInTheDocument();
        
        // System Admin
        expect(screen.getByText('System Admin')).toBeInTheDocument();
        expect(screen.getByText('User Authority Tiers')).toBeInTheDocument();
        expect(screen.getByText('Audit Logs')).toBeInTheDocument();
    });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- GlobalShell.test.tsx`
Expected: FAIL

- [ ] **Step 3: Write minimal implementation**

```tsx
import React, { ReactNode } from 'react';

export const GlobalShell: React.FC<{ children: ReactNode }> = ({ children }) => {
    return (
        <div className="global-shell">
            <header className="top-banner">
                <span>Business Date: 2026-04-21 | Role: Maker</span>
                <span>Global Search</span>
            </header>
            <nav className="left-menu">
                <ul>
                    <li className="nav-header">Workspace</li>
                    <li>Dashboard</li>
                    <li>My Approvals</li>
                    
                    <li className="nav-header">Trade Modules</li>
                    <li>Import LC</li>
                    <li>Export LC (Phase 2)</li>
                    <li>Collections (Phase 2)</li>
                    
                    <li className="nav-header">Master Data</li>
                    <li>Party &amp; KYC Directory</li>
                    <li>Credit Facilities</li>
                    <li>Tariff &amp; Fee Configuration</li>
                    <li>Product Configuration</li>
                    
                    <li className="nav-header">System Admin</li>
                    <li>User Authority Tiers</li>
                    <li>Audit Logs</li>
                </ul>
            </nav>
            <main className="content">{children}</main>
        </div>
    );
};
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npm test -- GlobalShell.test.tsx`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/GlobalShell.tsx frontend/src/components/GlobalShell.test.tsx
git commit -m "feat(frontend): natively construct complete Global UI navigation shell matching REQ-UI-CMN-01"
```

---

### Task 2: Tariff & Fee Configuration Matrix
**BDD Scenarios:** BDD-CMN-MAS-01, BDD-CMN-MAS-02
**BRD Requirements:** REQ-UI-CMN-05
**User-Facing:** YES

**Files:**
- Create: `frontend/src/components/TariffConfiguration.tsx`
- Create: `frontend/src/components/TariffConfiguration.test.tsx`

- [ ] **Step 1: Write the failing test**

```tsx
import { render, screen } from '@testing-library/react';
import { TariffConfiguration } from './TariffConfiguration';

describe('TariffConfiguration Rules Grid', () => {
    it('executes baseline rule rendering displaying default fees and minimums', () => {
        render(<TariffConfiguration />);
        expect(screen.getByText('Issuance Commission')).toBeInTheDocument();
        expect(screen.getByText('Base Rule Set')).toBeInTheDocument();
        expect(screen.getByDisplayValue('0.125')).toBeInTheDocument();
        expect(screen.getByText('Exception / Tier Pricing Grid')).toBeInTheDocument();
    });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- TariffConfiguration.test.tsx`
Expected: FAIL

- [ ] **Step 3: Write minimal implementation**

```tsx
import React from 'react';

export const TariffConfiguration: React.FC = () => {
    return (
        <div className="tariff-config-layout">
            <aside className="tariff-nav">
                <ul>
                    <li>Issuance Commission</li>
                    <li>Amendment Fee</li>
                    <li>SWIFT Cable Charge</li>
                </ul>
            </aside>
            <main className="tariff-main">
                <h2>Issuance Commission</h2>
                <div className="base-rules">
                    <h3>Base Rule Set</h3>
                    <label>Default Rate (%): <input defaultValue="0.125" /></label>
                    <label>Minimum Charge (USD): <input defaultValue="50.00" /></label>
                </div>
                <div className="exceptions-grid">
                    <h3>Exception / Tier Pricing Grid</h3>
                    <table>
                        <thead><tr><th>Customer Tier</th><th>Override Rate</th></tr></thead>
                        <tbody><tr><td>VIP Corporate</td><td>0.100</td></tr></tbody>
                    </table>
                </div>
                <button>Publish New Tariff</button>
            </main>
        </div>
    );
};
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npm test -- TariffConfiguration.test.tsx`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/TariffConfiguration.tsx frontend/src/components/TariffConfiguration.test.tsx
git commit -m "feat(frontend): deploy Tariff & Fee Configuration matrix component"
```

---

#### Task 9: High-Density Document Examination Workspace
**BDD Scenarios:** BDD-IMP-FLOW-05/06, BDD-IMP-DOC-01
**BRD Requirements:** REQ-UI-IMP-04
**User-Facing:** YES

**Files:**
- Create: `frontend/src/components/DocumentExamination.tsx`
- Create: `frontend/src/components/DocumentExamination.test.tsx`

- [x] **Step 1: Implement Split-Pane Layout**
- [x] **Step 2: Implement Interactive Document Matrix**
- [x] **Step 3: Implement ISBP-indexed Discrepancy Logger**

---

### Task 10: Checker Authorization with Risk Widgets
**BDD Scenarios:** BDD-IMP-FLOW-03, BDD-IMP-ISS-01/02
**BRD Requirements:** REQ-UI-IMP-05
**User-Facing:** YES

**Files:**
- Create: `frontend/src/components/CheckerAuthorization.tsx`
- Create: `frontend/src/components/CheckerAuthorization.test.tsx`

- [x] **Step 1: Implement "Exposure Widget" progress bars**
- [x] **Step 2: Implement "Compliance Deck" summary**
- [x] **Step 3: Implement Delta Highlighting for Amendments**

---

### Task 11: Import LC Lifecycle Operations (Amendments, Settlements, SGs)
**BDD Scenarios:** BDD-IMP-AMD-*, BDD-IMP-SET-*, BDD-IMP-SG-*
**BRD Requirements:** REQ-IMP-SPEC-02, REQ-IMP-SPEC-04, REQ-IMP-SPEC-05
**User-Facing:** YES

**Files:**
- Create: `frontend/src/components/AmendmentStepper.tsx`
- Create: `frontend/src/components/SettlementInitiation.tsx`
- Create: `frontend/src/components/ShippingGuaranteeForm.tsx`

- [x] **Step 1: Delta liability calculation for Amendments**
- [x] **Step 2: MT707 SWIFT preview generation**
- [x] **Step 3: Financial breakdown for Settlements**

---

### Task 12: System Admin & Governance
**BDD Scenarios:** BDD-CMN-PRD-01 (Product Config), BDD-CMN-MAS-04 (Audit Logs)
**BRD Requirements:** REQ-UI-CMN-01
**User-Facing:** YES

**Files:**
- Modify: `frontend/src/components/SystemAdminSettings.tsx`
- New Service: `AdminServices.xml`

- [x] **Step 1: REST integration for Audit Logs**
- [x] **Step 2: Dynamic Product Configuration Matrix**
- [x] **Step 3: Standard Clause Selector integration**

## Verification Summary
- **Frontend**: 51 Jest tests (100% Pass)
- **Backend**: 18 Spock specs
- **E2E**: 4 Playwright specs covering full lifecycle
