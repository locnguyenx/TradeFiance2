# [UX Modernization] Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Establish a high-density, modern "Flat Premium Light" UI shell and global design system for the Trade Finance platform.

**Architecture:** Transition from basic list-based navigation to a structured, solid sidebar shell. Implement a centralized CSS token system for Slate-based colors and emerald accents.

**Tech Stack:** React, Next.js, Vanilla CSS

---

### Task 1: Global Design Tokens & Typography [DONE]
**BDD Scenarios:** Scenario 1, Scenario 3
**BRD Requirements:** REQ-UI-MOD-01, REQ-UI-MOD-04
**User-Facing:** YES

**Files:**
- Modify: `frontend/src/app/globals.css`
- Modify: `frontend/src/app/layout.tsx` (Add Inter Font)

- [ ] **Step 1: Define Global CSS Variables**

```css
:root {
  --app-bg: #f8fafc;
  --card-bg: #ffffff;
  --nav-bg: #f8fafc;
  --nav-active-bg: #10b981;
  --nav-active-text: #ffffff;
  --border-main: #e2e8f0;
  --text-primary: #0f172a;
  --text-secondary: #64748b;
  --radius-card: 12px;
  --shadow-card: 0 1px 3px 0 rgb(0 0 0 / 0.1), 0 1px 2px -1px rgb(0 0 0 / 0.1);
}
```

- [ ] **Step 2: Apply Global Font and Background**

```css
body {
  background-color: var(--app-bg);
  color: var(--text-primary);
  font-family: 'Inter', system-ui, sans-serif;
}
```

- [ ] **Step 3: Commit**
```bash
git add frontend/src/index.css frontend/src/layout.tsx
git commit -m "style: establish flat premium light design tokens"
```

---

### Task 2: Modern Navigation Sidebar (GlobalShell Restructure) [DONE]
**BDD Scenarios:** Scenario 1, Scenario 2
**BRD Requirements:** REQ-UI-MOD-01, REQ-UI-MOD-02, REQ-UI-MOD-03
**User-Facing:** YES

**Files:**
- Modify: `frontend/src/components/GlobalShell.tsx`

- [ ] **Step 1: Implement solid sidebar layout**

```tsx
// Inside GlobalShell component
<aside className="global-sidebar">
  <div className="brand-logo">TRADEFINANCE</div>
  <nav className="nav-menu">
     {/* Modules mapped with Lucide icons */}
  </nav>
</aside>
```

- [ ] **Step 2: Implement Active State Logic**

```tsx
const isActive = (path: string) => router.pathname === path;
// ...
<Link href="/facilities">
  <a className={isActive('/facilities') ? 'nav-item active' : 'nav-item'}>
    <IconFacilities /> Facilities
  </a>
</Link>
```

- [ ] **Step 3: Update CSS for Sidebar**

```css
.global-sidebar {
  background-color: var(--nav-bg);
  border-right: 1px solid var(--border-main);
  width: 260px;
  /* ... */
}
.nav-item.active {
  background-color: var(--nav-active-bg);
  color: var(--nav-active-text);
}
```

- [ ] **Step 4: Commit**
```bash
git add frontend/src/components/GlobalShell.tsx
git commit -m "feat(ux): modernize global shell navigation bar"
```

---

### Task 3: Standardize Card Surfaces [DONE]
**BDD Scenarios:** Scenario 3
**BRD Requirements:** REQ-UI-MOD-04
**User-Facing:** YES

**Files:**
- Modify: `frontend/src/index.css` (Add utility class)

- [ ] **Step 1: Add `.modern-card` utility**

```css
.modern-card {
  background-color: var(--card-bg);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
  border: 1px solid var(--border-main);
}
```

- [ ] **Step 2: Commit**
```bash
git add frontend/src/index.css
git commit -m "style: add modern card utility class"
```

## Verification Summary
- **Visual Check**: Sidebar solid Slate 50, Active Emerald.
- **TDD**: Update `GlobalShell.test.tsx` to match the new structure.
