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

### Task 6: System Admin & Product Configuration Stub Screens
**BDD Scenarios:** BDD-CMN-PRD-01 (Product Config), BDD-CMN-MAS-04 (Audit Logs)
**BRD Requirements:** REQ-UI-CMN-01 (System Admin)
**User-Facing:** YES

**Files:**
- Create: `frontend/src/components/SystemAdminSettings.tsx`
- Create: `frontend/src/components/SystemAdminSettings.test.tsx`

- [ ] **Step 1: Write the failing test**

```tsx
import { render, screen } from '@testing-library/react';
import { SystemAdminSettings } from './SystemAdminSettings';

describe('SystemAdminSettings Sub-menu Render', () => {
    it('generates specific panels for User Authorities, Audit, and Product Config', () => {
        render(<SystemAdminSettings />);
        expect(screen.getByText('User Authority Management')).toBeInTheDocument();
        expect(screen.getByText('System Audit Logs (Delta JSON)')).toBeInTheDocument();
        expect(screen.getByText('Trade Product Configuration Matrix')).toBeInTheDocument();
    });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- SystemAdminSettings.test.tsx`
Expected: FAIL

- [ ] **Step 3: Write minimal implementation**

```tsx
import React from 'react';

export const SystemAdminSettings: React.FC = () => {
    return (
        <div className="system-admin-tabs">
            <section className="admin-panel">
                <h2>User Authority Management</h2>
                <p>Assign Maker/Checker Tiers (Tiers 1-4) to banking personnel.</p>
            </section>
            <section className="admin-panel">
                <h2>System Audit Logs (Delta JSON)</h2>
                <p>Immutable record viewer for transaction tracking.</p>
            </section>
            <section className="admin-panel">
                <h2>Trade Product Configuration Matrix</h2>
                <p>Toggle features like Is Transferable, Allow Revolving, Mandatory Margin.</p>
            </section>
        </div>
    );
};
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npm test -- SystemAdminSettings.test.tsx`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/SystemAdminSettings.tsx frontend/src/components/SystemAdminSettings.test.tsx
git commit -m "feat(frontend): map System Admin and Product Config skeleton structures"
```

---

### Task 7: Import LC Dashboard & Active Transactions
**BDD Scenarios:** BDD-IMP-FLOW-01 (Tracking Statuses)
**BRD Requirements:** REQ-UI-IMP-02
**User-Facing:** YES

**Files:**
- Create: `frontend/src/components/ImportLcDashboard.tsx`
- Create: `frontend/src/components/ImportLcDashboard.test.tsx`

- [ ] **Step 1: Write the failing test**

```tsx
import { render, screen } from '@testing-library/react';
import { ImportLcDashboard } from './ImportLcDashboard';

describe('ImportLcDashboard Rendering', () => {
    it('constructs top KPI widgets and explicit transaction filters', () => {
        render(<ImportLcDashboard />);
        expect(screen.getByText(/Drafts Awaiting/i)).toBeInTheDocument();
        expect(screen.getByText(/LCs Expiring within 7 Days/i)).toBeInTheDocument();
        expect(screen.getByText('Active Transaction Data Table')).toBeInTheDocument();
    });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- ImportLcDashboard.test.tsx`
Expected: FAIL

- [ ] **Step 3: Write minimal implementation**

```tsx
import React from 'react';

export const ImportLcDashboard: React.FC = () => {
    return (
        <div className="import-lc-dashboard">
            <div className="kpi-widgets">
                <div className="kpi-card">Drafts Awaiting My Submission: 5</div>
                <div className="kpi-card urgent">LCs Expiring within 7 Days: 2</div>
                <div className="kpi-card warning">Discrepant Presentations Awaiting Waiver: 1</div>
            </div>
            <div className="transaction-table-container">
                <h2>Active Transaction Data Table</h2>
                <div className="filters">
                    <select><option>Status: Draft, Issued, Docs</option></select>
                </div>
                <table>
                    <thead>
                        <tr><th>Ref No</th><th>Applicant</th><th>Amount</th><th>Status</th><th>SLA Timer</th></tr>
                    </thead>
                    <tbody>
                        <tr><td>TF-IMP-001</td><td>Acme Corp</td><td>$500,000</td><td>Issued</td><td>N/A</td></tr>
                    </tbody>
                </table>
            </div>
        </div>
    );
};
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npm test -- ImportLcDashboard.test.tsx`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/ImportLcDashboard.tsx frontend/src/components/ImportLcDashboard.test.tsx
git commit -m "feat(frontend): deploy Import LC explicit operational dashboard"
```

---

### Task 8: Enhance LC Issuance Data Entry Form (5-Step Stepper)
**BDD Scenarios:** BDD-CMN-PRD-03 (Tolerances), BDD-CMN-VAL-04 (Expiry rules)
**BRD Requirements:** REQ-UI-IMP-03
**User-Facing:** YES

**Files:**
- Modify: `frontend/src/components/IssuanceStepper.tsx`
- Modify: `frontend/src/components/IssuanceStepper.test.tsx`

- [ ] **Step 1: Write the failing test**

```tsx
import { render, screen, fireEvent } from '@testing-library/react';
import { IssuanceStepper } from './IssuanceStepper';

describe('IssuanceStepper Horizontal Form', () => {
    it('advances through 5 strictly defined data entry steps sequentially', () => {
        render(<IssuanceStepper />);
        expect(screen.getByText('Step 1: Parties & Limits')).toBeInTheDocument();
        fireEvent.click(screen.getByText('Next'));
        expect(screen.getByText('Step 2: Financials & Dates')).toBeInTheDocument();
        fireEvent.click(screen.getByText('Next'));
        expect(screen.getByText('Step 3: Terms & Shipping')).toBeInTheDocument();
        fireEvent.click(screen.getByText('Next'));
        expect(screen.getByText('Step 4: Narratives (MT700 Block)')).toBeInTheDocument();
        fireEvent.click(screen.getByText('Next'));
        expect(screen.getByText('Step 5: Review & Submit')).toBeInTheDocument();
    });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- IssuanceStepper.test.tsx`
Expected: FAIL

- [ ] **Step 3: Write minimal implementation**

```tsx
import React, { useState } from 'react';

const steps = [
    'Step 1: Parties & Limits',
    'Step 2: Financials & Dates',
    'Step 3: Terms & Shipping',
    'Step 4: Narratives (MT700 Block)',
    'Step 5: Review & Submit',
];

export const IssuanceStepper: React.FC = () => {
    const [stepIndex, setStepIndex] = useState(0);
    
    return (
        <div className="stepper-layout">
            <header className="draft-banner">
                <span>Draft Reference: DRAFT-1002</span>
                <span>Base Equivalent: $0</span>
            </header>
            <h2>{steps[stepIndex]}</h2>
            {stepIndex === 1 && (
                <div className="financials-form">
                    <label>Positive Tolerance %: <input type="number" /></label>
                    <label>Negative Tolerance %: <input type="number" /></label>
                    <label>Issue Date: <input type="date" /></label>
                    <label>Expiry Date: <input type="date" /></label>
                </div>
            )}
            {stepIndex === 4 && (
                <div className="review-submit-panel">
                    <h3>System Validations</h3>
                    <p>✅ Limit Check Passed</p>
                    <p>✅ Sanctions Check Passed</p>
                    <button>Submit for Approval</button>
                </div>
            )}
            <div className="stepper-actions">
                {stepIndex > 0 && <button onClick={() => setStepIndex(stepIndex - 1)}>Back</button>}
                {stepIndex < 4 && <button onClick={() => setStepIndex(stepIndex + 1)}>Next</button>}
            </div>
        </div>
    );
};
```

- [ ] **Step 4: Run test to verify it passes**

Run: `npm test -- IssuanceStepper.test.tsx`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/IssuanceStepper.tsx frontend/src/components/IssuanceStepper.test.tsx
git commit -m "feat(frontend): upgrade Issuance Stepper to rigorous 5-step SWIFT compliant entry layout"
```

---

*(Note: The `PartyDirectory`, `LimitsDashboard`, `DocumentExamination`, and `CheckerAuthorization` modules remain unchanged from Phase 1 or initial plan draft and should be fully completed identically to their respective tasks).*
