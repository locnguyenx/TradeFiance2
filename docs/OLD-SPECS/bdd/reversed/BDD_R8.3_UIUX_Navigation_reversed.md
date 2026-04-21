---
Document ID: BDD-R8.3-UIUX
Version: 0.1
System: Trade Finance
Module: Letter of Credit (LC)
BRD ID: BRD-002
Status: DRAFT
Last Updated: 2026-03-12
Author: [Antigravity]
---

# BDD Scenarios: UI/UX & Navigation Pattern (R8.3) - Reversed

**Requirement:** R8.3 UI/UX Requirements (Grouped Layout, Status Tracking, Navigation Pattern)

This document contains BDD scenarios reversed from the implemented test cases in `TradeFinanceScreensSpec.groovy`.

## Feature: UI/UX & Navigation Pattern

**As a** Trade Finance User
**I want to** navigate through a consistent and well-organized interface
**So that** I can efficiently manage LC records and track their status visually

### Background
The system is populated with demo data representing various LC states.

### Scenario: Find LC Screen contains Dual-Status tracking
**Given** the user is on the **Import Letters of Credit** list screen (`ImportLc/Lc/FindLc`)
**When** the screen renders the list of LCs
**Then** it should display both **LC Status** (Lifecycle) and **Processing Status** (Transaction) columns
**And** it should display the **Create New LC** action button.

### Scenario: Grouped Layout in LC Detail Screen (Happy Path)
**Given** the user is viewing the detail of an existing LC (e.g., `DEMO_LC_01`)
**When** the **MainLC** screen renders
**Then** the information should be organized into logical sections:
  - **General Info** (LC Number, Amount)
  - **Parties** (Applicant, Beneficiary, Banks)
  - **Shipment** (Partial/Transhipment rules)
  - **Sub-Tabs** (Amendments, Drawings, Financials, History).

### Scenario: Visual Status Tracking (Premium Status Chips)
**Given** the user is viewing an LC in a specific state (e.g., **Closed**)
**When** the screen renders
**Then** the status should be clearly visible in the header/sidebar as a "Premium Status Chip" (e.g., displaying **Closed** for `DEMO_LC_01`).

### Scenario: Hierarchical Navigation Pattern
**Given** the user is on the **Find LC** list screen
**When** the user clicks on an LC record (e.g., `ILC-2026-0001`)
**Then** the system should navigate to the **MainLC** detail screen for that specific `lcId`.

### Scenario: Secure Read-Only Access
**Given** a user is viewing an LC record in read-only mode (e.g., from a linked amendment)
**When** the screen renders
**Then** any "Save" or "Update" actions should be hidden
**And** a **Back** link should be provided to return to the previous context.

---

## Business Rules & Validation
1. **Consistency:** All detail screens MUST follow the `Find -> Detail` pattern.
2. **Standardized Identity:** Statuses MUST be displayed using standardized labels (not raw IDs).
3. **Data Linkage:** Sub-tabs (Financials, History, etc.) MUST be filtered to show only data related to the currently viewed `lcId`.
4. **UI Performance:** Screens should render within a reasonable time frame (verified in tests).
5. **Accessibility:** Form fields should be clearly labeled with their corresponding SWIFT tag numbers (e.g., "Applicant (50)").
