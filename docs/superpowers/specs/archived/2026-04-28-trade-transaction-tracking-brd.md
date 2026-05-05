# Business Requirements Document (BRD)
**Project Name:** Digital Trade Finance Platform
**Module:** Common / Unified Traceability
**Topic:** Trade Transaction & Instrument Narrative
**Document Version:** 1.0
**Date:** April 28, 2026

---

## REQ-TXN-01: Transaction-Primary Lifecycle Management
The frontend must transition to a model where all instrument modifications are originated and tracked via the `TradeTransaction` entity.

- **REQ-TXN-01.1: Unified Initiation**: All actions (New LC, Amendment, Presentation, Settlement) must create a `TradeTransaction` record first.
- **REQ-TXN-01.2: Transaction-Based Authorization**: Checkers must authorize by `transactionId`. The frontend MUST NOT perform direct instrument updates for authorized actions.
- **REQ-TXN-01.3: Priority and Urgency**: The `priorityEnumId` (Low, Medium, High, Urgent) is a property of the `TradeTransaction`, allowing checkers to prioritize their queue based on transaction urgency rather than just instrument size.

## REQ-TXN-02: Decoupled Data Capture (Proposed vs. Current)
The frontend must distinguish between the data being *proposed* in an active transaction and the *current* state of the instrument.

## REQ-NAV-01: Enhanced Navigation Structure
The frontend must provide a clear separation between operational workflow and asset management.

- **REQ-NAV-01.1: Dashboard**: KPI-driven summary of current exposure and queue health.
- **REQ-NAV-01.2: Instrument Management**: Vertical-specific lists (Import LCs, SGs, etc.) for browsing the current legal state of all trade assets.
- **REQ-NAV-01.3: Global Transaction Log**: A cross-instrument audit view of all Maker/Checker activity, sorted by priority and date.

## REQ-SRH-01: Contextual Global Search
The system must provide a unified search interface that understands the context of the user's intent.

- **REQ-SRH-01.1: Context Toggling**: Users must be able to toggle search results between "Instruments" (linking to the legal state) and "Transactions" (linking to the workflow/timeline).
- **REQ-SRH-01.2: Cross-Reference Indexing**: Searching by an `instrumentId` must also surface all associated `transactionId`s in the transaction context.

## REQ-UTN-01: Unified Narrative Timeline
The system must provide a chronological "storytelling" view of an instrument's entire lifecycle. This view merges business-level transactions with technical audit events.

- **REQ-UTN-01.1: Business Transaction Events**: Every `TradeTransaction` (Issuance, Amendment, Presentation, Settlement) must appear as a major anchor in the timeline.
- **REQ-UTN-01.2: Technical Audit Events**: System-generated events such as SWIFT message dispatch (MT700, MT707), Network ACKs, Email notifications, and Compliance screening results must be interleaved chronologically.
- **REQ-UTN-01.3: Visual Hierarchy**: Active/Pending transactions must be visually highlighted (e.g., larger nodes or distinct colors) to indicate the "Current Focus".

## REQ-UTN-02: In-Timeline Actionability
To improve operational efficiency, the timeline must be interactive, allowing users to perform workflow actions without navigating away.

- **REQ-UTN-02.1: Contextual Workflow Actions**: For transactions in `Pending Approval`, users with Checker authority must be able to "Authorize" or "Reject" directly from the timeline node.
- **REQ-UTN-02.2: Draft Resumption**: Users must be able to "Resume" draft transactions directly from the timeline.
- **REQ-UTN-02.3: Rejection Feedback**: If a transaction was rejected, the "Rejection Reason" must be visible as a child-event under the rejected node.

## REQ-UTN-03: Delta & Version Analysis
Users must be able to understand exactly what changed between iterations of a Trade Instrument.

- **REQ-UTN-03.1: Amendment Delta**: For every Amendment transaction, the system must provide a "View Diff" function that highlights modified fields (e.g., Old Amount vs. New Amount).
- **REQ-UTN-03.2: Snapshot Preservation**: Every transaction node in the timeline must link to the specific data snapshot of the instrument as it existed at that version number.

## REQ-UTN-04: Cross-Module Traceability
The timeline must support all trade modules (Import LC, Export LC, Guarantees) in a consistent format.

- **REQ-UTN-04.1: Module Tagging**: Each event must be tagged with its origin (e.g., [ISSUANCE], [AMENDMENT], [SWIFT-OUT]).
- **REQ-UTN-04.2: Reference Integrity**: Events must be linked via `transactionId` to ensure that even if multiple amendments are pending, they are tracked independently.

## REQ-UTN-05: Status Convergence
The UI must clearly distinguish between the **Transaction Status** (where we are in the workflow) and the **Business State** (the legal status of the instrument).

- **REQ-UTN-05.1: Transaction Status Display**: Shows "Draft", "Pending Approval", "Executing", "Completed".
- **REQ-UTN-05.2: Business State Display**: Shows "Issued", "Documents Received", "Settled".
- **REQ-UTN-05.3: Dual Status Header**: The main instrument header must display BOTH statuses when a transaction is active. (e.g., "Instrument: Issued | Action: Pending Approval").
