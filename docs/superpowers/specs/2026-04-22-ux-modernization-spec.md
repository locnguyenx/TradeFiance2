# BRD: Frontend UX Modernization (Phase 14)

## 1. Overview
The goal is to modernize the Trade Finance platform's UX/UI, transitioning from a basic functional layout to a premium, high-density enterprise interface. This phase specifically targets the global navigation shell and core design system.

## 2. Success Criteria
- **SC1**: Global shell utilizes a minimalist, high-density sidebar.
- **SC2**: UI follows "Flat Premium Light" aesthetic (No gradients/glows).
- **SC3**: Modern typography (Inter) is used globally.
- **SC4**: Sidebar supports nested navigation and collapsible states (Optional/TBD).

## 3. Requirements (REQ-UI-MOD-*)
| ID | Requirement | Priority |
|---|---|---|
| REQ-UI-MOD-01 | Implement Clean Slate 50 Sidebar with 1px border-right separator. | P0 |
| REQ-UI-MOD-02 | Map all modules (Trade, Audit, Facilities, Admin) with sharp 1.5px stroke icons. | P0 |
| REQ-UI-MOD-03 | Active menu item MUST have a flat Soft blue (#5373fb) background with white text. | P0 |
| REQ-UI-MOD-04 | Standardize Card surfaces with 12px border-radius and shadow-sm. | P1 |
| REQ-UI-MOD-05 | Implement smooth transitions (150ms) for hover and active states. | P1 |

---

# BDD: Frontend UX Modernization (Phase 14)

## Scenario 1: Sidebar Layout Verification
**REQ-UI-MOD-01, REQ-UI-MOD-02**
- **Given** I am logged into the Trade Finance platform
- **When** I view the global navigation menu
- **Then** I should see a solid sidebar with background hex #031e88 (Dark blue)
- **And** I should see specific icons for "Trade Lifecycle", "Facilities", and "Administration"
- **And** there should be a `border-right` of 1px with color #8da2fc

## Scenario 2: Active Navigation State
**REQ-UI-MOD-03**
- **Given** I am on the "Facilities" dashboard
- **When** I look at the sidebar
- **Then** the "Facilities" menu item should have a background color of #5373fb (Soft blue)
- **And** the text color should be `#ffffff`
- **And** there should be NO outer glow or gradient effects

## Scenario 3: Global Typography & Surface Standards
**REQ-UI-MOD-04**
- **Given** any operational dashboard (e.g. Import LC)
- **When** content is displayed in a Card component
- **Then** the card should have a `border-radius` of 12px
- **And** it should have a subtle `shadow-sm` depth
