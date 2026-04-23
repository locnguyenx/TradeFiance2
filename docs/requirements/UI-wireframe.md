Transitioning from business requirements to user interface (UI) design in Trade Finance requires a specific focus on **cognitive load reduction**. Trade operations involve massive amounts of text, strict validation rules, and high financial risk. 

To build this in Moqui, your XML screens (whether rendering standard HTML or a modern frontend like Vue/React) should utilize specific enterprise UX patterns: split-screens for document checking, stepper components for heavy data entry, and distinct visual modes for Makers vs. Checkers.

---

## Common Module UI

The Common Module UI is the central nervous system of your Trade Finance platform. Because it handles the overarching governance—credit limits, KYC/AML, and cross-product authorizations—its screens must be designed for **high-level visibility and exception management**.

Here are the UI wireframe requirements for the Common Module, seamlessly integrating with the global shell we defined earlier.

***

### 1. Updated Left Navigation Menu (Global Shell Integration)
To accommodate the Common Module, the persistent navigation menu expands as follows:

* **Dashboard** (User's daily workspace)
* **My Approvals** (The Global Checker Queue)
* **Trade Modules**
    * `Import LC`
    * `Export LC` *(Phase 2)*
    * `Collections` *(Phase 2)*
* **Master Data (Common Module)**
    * `Party & KYC Directory`
    * `Credit Facilities (Limits)`
    * `Tariff & Fee Configuration`
    * `Product Configuration`
* **System Admin**
    * `User Authority Tiers`
    * `Audit Logs`

---

### 2. Wireframe: Global Checker Queue ("My Approvals")
**Purpose:** A unified inbox for Checkers. Instead of hunting through different product modules, Checkers come here to see every transaction requiring their specific authority tier.
**Layout Pattern:** High-Density Data Grid with Quick Filters.

* **Top KPI Banner:**
    * *SLA Warnings:* "2 items approaching 5-day UCP deadline."
    * *Tier Indicator:* "Logged in as Tier 3 Approver (Up to $5,000,000)."
* **Main Data Table:**
    * **Filters:** Product Type (`Import LC`, `Export Doc`, `SG`), Action Type (`Issuance`, `Amendment`, `Settlement`), Priority.
    * **Columns:**
        * `Module` (e.g., Badge: [IMP LC] or [SG])
        * `Reference No.`
        * `Action` (e.g., "Authorize Issuance", "Authorize Discrepancy Waiver")
        * `Maker`
        * `Base Equivalent Amount`
        * `Time in Queue`
    * **Row Behavior:** Clicking a row does *not* open a new tab. It opens a **Full-Screen Overlay** (Modal) displaying the "Checker Authorization Screen" we defined in the Import LC wireframes, tailored to that specific transaction.

---

### 3. Wireframe: Party & KYC Directory (Mantle `Party` Facade)
**Purpose:** Centralized management of all Applicants, Beneficiaries, and Correspondent Banks to ensure clean data for SWIFT messaging and compliance screening.
**Layout Pattern:** Master-Detail View.

* **Left Pane (Master List):**
    * Search bar (Name, SWIFT BIC, National ID/Tax ID).
    * List of parties with a color-coded dot for KYC Status (Green = Clear, Red = Expired, Yellow = Pending).
* **Right Pane (Detail View - Tabbed):**
    * **Header:** Legal Name, SWIFT BIC (if applicable), and large bold KYC / Sanctions Status badges.
    * **Tab 1: General Info:** Registered address, operational country, primary contacts.
    * **Tab 2: Roles:** Toggle switches dictating what this party is allowed to do (e.g., `Is Applicant? [On]`, `Is Advising Bank? [Off]`).
    * **Tab 3: Compliance (AML/Sanctions):** Read-only log of the latest automated screening results, Next KYC Review Date, and upload widgets for corporate governance documents.

---

### 4. Wireframe: Credit Facility & Limit Dashboard (Mantle `FinancialAccount` Facade)
**Purpose:** For Credit Risk Officers and Trade Operations to monitor the bank's exposure to specific customers in real-time.
**Layout Pattern:** Analytics Dashboard with Drill-Down capabilities.

* **Search & Select Banner:**
    * Dropdown to select an `Applicant`.
* **Top Visual Summary (The Exposure Widget):**
    * A large, horizontal, color-coded progress bar.
    * *Total Length:* Approved Facility Limit (e.g., $10,000,000).
    * *Dark Blue Segment:* Firm Liabilities (e.g., Settled Sight LCs pending payment, Accepted Usance LCs).
    * *Light Blue Segment:* Contingent Liabilities (e.g., Unutilized Issued LCs).
    * *Orange Segment:* Reserved / Earmarks (e.g., LCs in *Pending Approval* state).
    * *Grey Segment:* Available Balance.
* **Bottom Data Table (Utilization Breakdown):**
    * A live list of every single active transaction currently consuming this specific facility.
    * **Columns:** `Transaction Ref`, `Module`, `Current State`, `Expiry Date`, `Utilized Amount`.
    * **Feature:** Hyperlinked Reference Numbers that navigate directly to that specific transaction's read-only view.

---

### 5. Wireframe: Tariff & Fee Configuration
**Purpose:** Allows business administrators to update trade finance fee structures without requiring code changes or database scripts.
**Layout Pattern:** Matrix / Rules Grid.

* **Left Navigation:** List of Fee Types (e.g., `Issuance Commission`, `Amendment Fee`, `Discrepancy Fee`, `SWIFT Cable Charge`).
* **Main Configuration Area (Example: Issuance Commission):**
    * **Base Rule Set:**
        * `Calculation Method:` [Percentage per Quarter]
        * `Default Rate:` [0.125 %]
        * `Minimum Charge:` [$50.00 USD]
    * **Exception / Tier Pricing Grid:**
        * A table allowing admins to set overrides for specific Customer Tiers (e.g., VIP Corporate gets 0.100%) or specific LC Amount thresholds (e.g., LCs over $1M get a flat rate).
    * **Action Bar:** **[Save Draft]** / **[Publish New Tariff]** (Requires Maker/Checker approval to alter the bank's fee income rules).

***

## Import LC Module UI
Here is the UI wireframe specification for the Import LC Module.

### 1. Global UI Shell (Persistent Elements)
These elements must be visible on every screen to maintain system context.

* **Top Navigation Bar:**
    * **System Context:** Current Business Date (critical for value dating), Logged-in User Profile, and Active Role (e.g., `Maker` or `Checker`).
    * **Global Search:** A persistent omni-search bar that accepts LC Reference Numbers, SWIFT references, or Applicant Names.
* **Left Navigation Menu (Collapsible):**
    * `Dashboard`
    * `Import LC` (Expands to: *New Application, Active LCs, Presentations, Settled/Closed*)
    * `Export LC` (Locked for Phase 2)
    * `Collections` (Locked for Phase 2)
    * `My Approvals` (Displays badge count of pending items for Checkers)

---

### 2. Wireframe: Import LC Dashboard (The Workspace)
**Purpose:** The landing page for operations staff to see their daily actionable items.

* **Top Area: KPI "Widget" Cards**
    * *Card 1:* Drafts Awaiting My Submission (Count).
    * *Card 2:* LCs Expiring within 7 Days (Count - Red text).
    * *Card 3:* Discrepant Presentations Awaiting Applicant Waiver (Count - Orange text).
* **Main Area: Active Transaction Data Table**
    * **Filters:** Status dropdown (*Draft, Issued, Docs Received*), Issue Date range, Applicant Name.
    * **Columns:** `Transaction Ref`, `Applicant`, `Beneficiary`, `CCY`, `Amount`, `Expiry Date`, `Status`, `SLA Timer` (Shows days left for document checking).
    * **Row Action Menu (Three dots):** Context-aware actions. For an *Issued* LC, options are `Initiate Amendment`, `Log Presentation`, or `Cancel`.

---

### 3. Wireframe: LC Issuance Data Entry Form (The Maker View)
**Purpose:** Capturing the MT 700 data. Because there are over 40 fields, a single long-scrolling page will cause user fatigue and errors.
**Layout Pattern:** Horizontal Stepper or Accordion Tabs.

* **Header Banner (Sticky):**
    * Displays `Draft Reference`, `Status: DRAFT`, and a dynamic `Calculated Base Equivalent Amount` that updates as the user enters the LC amount.
* **Step 1: Basic Information**
    * **LC Info**
        * *LC Type:* [Import LC] / [Export LC] / [Collections]
        * *LC Number:* [Auto-generated]
        * *LC Reference:* [Auto-generated]
        * *LC Status:* [Draft] / [Pending Approval] / [Issued] / [Docs Received] / [Discrepant] / [Settled] / [Closed] No input. System automactically updates the status based on the actions taken by the user.
        * *LC Product:* [Dropdown: Sight LC, Usance LC, Standby LC, Revolving LC]
    * **Parties**
        * *Applicant Field:* Auto-complete search. Once selected, a read-only widget appears showing their `Available Facility Limit` and `KYC Status`.
        * *Beneficiary Field:* Multi-line text area (4x35 chars max).
        ...
* **Step 2: Main LC Information**
    * **Financials & Dates**
        * *Amount & Currency:* Dropdown for ISO currency, numeric input for amount.
        * *Tolerance:* Two small numeric inputs for `+ %` and `- %`.
        * *Dates:* Date-pickers for `Issue Date` and `Expiry Date`. System throws an inline warning if Expiry is in the past.
    * **Terms & Shipping**
        * *Radio Buttons:* Partial Shipments (`Allowed`/`Not Allowed`), Transhipment (`Allowed`/`Not Allowed`).
        * *Ports:* Text inputs for Port of Loading / Discharge.
    * **Narratives (The heavy text)**
        * *Layout:* Large, expandable text areas for `Description of Goods`, `Documents Required`, and `Additional Conditions`.
        * *Feature:* A "Standard Clauses" button next to each text area allowing users to insert pre-approved legal text blocks.
    * ...
* **Step 3: Margin & Charges**
    * **Margin**
        * *Margin Type:* [Cash] / [Lombard] / [None]
        * *Margin Percentage:* [100%] (Auto-calculated based on LC Product & LC risk profile)
        * *Margin Amount:* (Auto-calculated based on LC Product & LC risk profile)
        * *Debit Account:* [Account Number] (Auto-calculated based on LC risk profile)
    * **Charges**
        * *Charge Type:* [Issuance Commission] / [Amendment Fee] / [Discrepancy Fee] / [SWIFT Cable Charge]
        * *Charge Rate:* [0.125 %] (Depend on Charge Type & Charge Configuration)
        * *Charge Amount:* (Auto-calculated based on Charge Type & Charge Configuration)
        * *Debit Account:* [Account Number] (Auto-calculated based on LC risk profile)
* **Step 4: Review & Submit**
    * A read-only summary of all entered data.
    * A "System Validations" panel showing green checkmarks for Limit Check, Sanctions Check, and KYC Check.
    * Primary Action Button: **[Submit for Approval]**.

---

### 4. Wireframe: Document Examination (The Split-Screen View)
**Purpose:** Allowing operations to check presented documents against the original LC terms without constantly switching tabs.
**Layout Pattern:** 50/50 Vertical Split-Pane.

* **Left Pane (Read-Only Context):**
    * Displays the *Issued LC Terms*.
    * Accordion sections for `Financials`, `Shipping Terms`, and `Required Documents`. The user can scroll this independently to reference the rules.
* **Right Pane (Action/Entry Area):**
    * *Top:* Presentation Header (Claim Amount, Date Received).
    * *Middle - Document Grid:* A dynamic table where the user adds rows for each document received (e.g., [Invoice] [3 Originals] [0 Copies]).
    * *Bottom - Discrepancy Logger:* * A prominent toggle: `[Clean] / [Discrepant]`.
        * If `Discrepant` is selected, a dynamic list appears. The user selects an ISBP Code from a dropdown and types the specific detail (e.g., "Code 12 - Late Shipment: B/L dated 12-Apr, latest shipment was 10-Apr").
* **Sticky Footer:**
    * Action Buttons: **[Save Draft]** or **[Submit Examination]**.

---

### 5. Wireframe: The Checker Authorization Screen
**Purpose:** Designed for speed, risk assessment, and the "Four-Eyes Principle."

* **Top Action Bar (Highly Visible):**
    * Buttons: **[AUTHORIZE]** (Green), **[REJECT TO MAKER]** (Red), **[SEND TO COMPLIANCE]** (Yellow).
* **Left Column: Risk & Compliance Summary (30% width)**
    * *Maker Details:* "Submitted by [User] at [Time]".
    * *Limit Widget:* A visual progress bar showing the Applicant's total limit, current utilization, and how *this specific transaction* impacts the remaining bar.
    * *Screening Status:* Clear visual badges for AML and Sanctions.
* **Right Column: Transaction Data (70% width)**
    * Read-only view of the data.
    * *Crucial UX Feature for Amendments:* If this is an amendment approval, the UI **must highlight the delta**. (e.g., The original amount is struck through, and the new amount is highlighted in a distinct color, so the Checker immediately sees what changed without reading the whole document).
* **Rejection Modal:**
    * If the Checker clicks **[REJECT TO MAKER]**, a modal window forces them to type a `Rejection Reason` before the system routes it back to the *Draft* state.
***

## **NOTES FOR THE UI WIREFRAME DOCUMENT:**

1. This document is a wireframe, not a final design.
2. The UI wireframes of each module are for illustration of the UI patterns for key features, not a complete UI design, thus the wireframe may not cover all fields required for a function. The completed requirement and function list are described in the BRDs. REMEMBER this point when implement the UI.
