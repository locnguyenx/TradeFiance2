# Consolidated BRD: Inbound SWIFT Processing & Trade Inbox (Phase 2)
**ABOUTME:**
BRD for inbound SWIFT message ingestion, correlation, Trade Inbox UI, and automated LC lifecycle triggers.
Covers file polling, manual upload, deduplication, Tag 21 correlation, operator action workflows, and Maker/Checker discrepancy resolution.

**Project Name:** Digital Trade Finance Platform
**Module:** Import Letter of Credit (LC) — Inbound Processing
**Document Version:** 1.0
**Date:** May 15, 2026

**Parent BRDs (supplemented — not superseded):**
*   [2026-05-05-import-lc-consolidated-brd.md](file:///Users/me/myprojects/moqui-trade/docs/superpowers/specs/2026-05-05-import-lc-consolidated-brd.md) — Core LC lifecycle, state machine, outbound SWIFT generation
*   [2026-05-05-common-consolidated-brd.md](file:///Users/me/myprojects/moqui-trade/docs/superpowers/specs/2026-05-05-common-consolidated-brd.md) — Common entities, Maker/Checker, TradeParty
*   [2026-05-08-import-lc-amendment-srg2024-brd.md](file:///Users/me/myprojects/moqui-trade/docs/superpowers/specs/2026-05-08-import-lc-amendment-srg2024-brd.md) — MT 707 amendments, consent tracking
*   [2026-05-08-swift-2024-remaining-gaps-brd.md](file:///Users/me/myprojects/moqui-trade/docs/superpowers/specs/2026-05-08-swift-2024-remaining-gaps-brd.md) — MT 740/747 reimbursement, Nostro reconciliation, MT 730 reference

**Source Requirements:**
*   [20260507-INCOMMING COMMUNICATION.md](file:///Users/me/myprojects/moqui-trade/docs/requirements/20260507-INCOMMING%20COMMUNICATION.md) — Inbound message correlation engine, Trade Inbox, discrepancy resolution workflows

---

## Architectural Overview

### The Problem: The Missing Inbound Half

The current system (Phase 1) is entirely **outbound and maker-driven**. All LC lifecycle transitions — issuance, amendments, presentations, settlements — are initiated by human operators who manually enter data and generate outbound SWIFT messages (MT 700, MT 707, MT 734, MT 750, MT 202).

When the outside world responds (e.g., the Advising Bank acknowledges the LC, or the Beneficiary accepts an amendment), operators currently update the system manually based on information received through a separate legacy SWIFT terminal or email. This creates:

*   **Operational lag:** Hours or days between receiving a SWIFT response and updating the Moqui state machine.
*   **Data integrity risk:** Manual re-keying of SWIFT reference numbers and amounts.
*   **Audit gaps:** No system-of-record linking between the inbound message and the state change it triggered.

### The Solution: Inbound Message Correlation Engine

This BRD defines the **Phase 2 Inbound Processing** layer that closes the feedback loop. The system will:

1.  **Ingest** raw SWIFT `.txt` files via two channels: automated directory polling (primary) and manual UI upload (fallback).
2.  **Parse** the raw text using Prowide Core to extract structured SWIFT fields.
3.  **Correlate** inbound messages to existing LC records using **Tag 21 (Related Reference)** as the foreign key against `TradeInstrument.instrumentRef`.
4.  **Stage** parsed messages in a **Trade Inbox** for operator review and action.
5.  **Trigger** LC state machine transitions when operators click action buttons (Accept, Reject, Acknowledge, Waive, Refuse).

### Future-Proofing Principle

The ingestion layer is designed as a **pluggable channel**. Phase 1 implements file-based ingestion (directory polling + manual upload). The parsing, correlation, and Trade Inbox layers are completely channel-agnostic. When the bank obtains SWIFT Alliance Access (SAA) connectivity, the file-based channel is replaced with a JMS/MQ listener or Apache Camel route — **zero changes to business logic, entities, or UI**.

```
Phase 1 (This BRD):           Phase 2 (Future):
┌──────────────────┐           ┌──────────────────┐
│  Directory Poll  │           │  JMS/MQ Listener  │
│  + Manual Upload │           │  (SAA Gateway)    │
└────────┬─────────┘           └────────┬─────────┘
         │                              │
         ▼                              ▼
┌──────────────────────────────────────────────┐
│         Ingestion Service Interface          │
│  (accepts raw SWIFT string, returns parsed)  │
├──────────────────────────────────────────────┤
│         Prowide Core Parsing Engine          │
├──────────────────────────────────────────────┤
│         Tag 21 Correlation Engine            │
├──────────────────────────────────────────────┤
│         Trade Inbox (TradeInboxItem)         │
├──────────────────────────────────────────────┤
│         Operator Action → State Machine      │
└──────────────────────────────────────────────┘
```

### Scope of Inbound Messages (Phase 1)

| MT Type | Description | Lifecycle Process | Auto-Action |
| :--- | :--- | :--- | :--- |
| **MT 730** | Acknowledgment | Issuance (3.1), Amendment (3.2) | Operator 1-click → state update |
| **MT 799** | Free Format (Consent/Query) | Amendment (3.2), Cancellation (3.6) | Operator reads + decides |
| **MT 750** | Advice of Discrepancy | Presentation (3.3) | Auto-spawn `TradeDocumentPresentation` |
| **MT 754** | Advice of Clean Presentation | Presentation (3.3) | Auto-spawn `TradeDocumentPresentation` |
| **MT 742** | Reimbursement Claim | Settlement (3.4) | Auto-match → STP or exception queue |
| **MT 999** | Free Format (Unauthenticated) | Any | Security warning + manual only |
| **MT 720** | Transfer Notification | Transfer (info only) | Operator reads + internal update |

---

## 1. Feature 1: Ingestion Pipeline

### 1.1 Channel A — Automated Directory Polling (Primary)

A Moqui Scheduled Service Job polls a designated server directory for inbound `.txt` files on a configurable cron schedule.

#### A. Configuration

| Config Key | Default Value | Description |
| :--- | :--- | :--- |
| `SWIFT_INBOUND_DIR` | `/data/swift/inbound/` | Hot directory where the cron job or SWIFT gateway drops `.txt` files. |
| `SWIFT_ARCHIVE_DIR` | `/data/swift/archive/processed/` | Successfully parsed files are moved here. |
| `SWIFT_ERROR_DIR` | `/data/swift/archive/error/` | Files that fail parsing are moved here. |
| `SWIFT_POLL_CRON` | `0 */5 * * * ?` | Cron expression (default: every 5 minutes). |
| `SWIFT_FILE_STABLE_SECONDS` | `2` | Minimum seconds since last modification before reading (prevents partial-write reads). |

#### B. The "Read & Move" Protocol

The polling service executes this strict sequence for every `.txt` file found:

1.  **Stability Check:** Verify the file's last-modified timestamp is at least `SWIFT_FILE_STABLE_SECONDS` ago. If not, skip to next poll cycle.
2.  **Read to Memory:** Load the entire `.txt` file content into a string variable.
3.  **Move Immediately:** Move the physical file from `SWIFT_INBOUND_DIR` to `SWIFT_ARCHIVE_DIR` (append timestamp to filename to prevent collisions). If the file fails parsing later, it is moved to `SWIFT_ERROR_DIR`.
4.  **Batch Split:** If the file contains multiple concatenated SWIFT messages (delimited by `{1:` block starts), use Prowide Core's `SwiftParser` iterator to split into individual message strings.
5.  **Process Each:** For each individual message string, call the shared **Ingestion Service** (Feature 1.3).

#### C. Batch File Handling

Banks commonly output multiple SWIFT messages concatenated in a single `.txt` file. The system must detect and split these before parsing:

*   **Detection:** Check if the file content contains more than one `{1:` block start pattern.
*   **Splitting:** Use Prowide Core's `SwiftParser` to iterate over individual messages.
*   **Per-Message Processing:** Each split message is processed independently. A parse failure on message #3 of 10 must not block messages #1, #2, #4–#10.

#### D. User Stories

##### US-ING-01: Automated SWIFT File Polling
**As a** Trade Operations system,
**I want** to automatically detect and ingest SWIFT `.txt` files from a server directory on a configurable schedule,
**So that** inbound messages are processed without manual intervention.

##### US-ING-02: Batch File Splitting
**As a** Trade Operations system,
**I want** to split concatenated batch files into individual SWIFT messages,
**So that** each message is processed independently and a single parse failure does not block the entire batch.

---

### 1.2 Channel B — Manual Upload UI (Fallback)

A Moqui screen allows authorized operators to manually upload `.txt` files containing one or more SWIFT messages. This covers scenarios where the automated directory is unavailable or when files arrive via alternative channels (email, USB).

#### A. UI Requirements

*   **Screen:** `SwiftManualUpload.xml` — accessible to users with `TRADE_OPS_MAKER` role.
*   **Input:** Standard Moqui file upload widget accepting `.txt` files only.
*   **Validation:** File must not be empty. File extension must be `.txt`.
*   **Processing:** Upon upload, the system reads the file content and calls the same shared **Ingestion Service** (Feature 1.3) used by the automated poller.
*   **Feedback:** The UI displays a summary: `N messages parsed, M correlated, K orphaned, J duplicates skipped`.

#### B. User Stories

##### US-ING-03: Manual SWIFT File Upload
**As a** Trade Operations Maker,
**I want** to manually upload a SWIFT `.txt` file through the Moqui UI,
**So that** I can process inbound messages when the automated directory polling is unavailable.

---

### 1.3 Shared Ingestion Service Interface

Both channels converge on a single Moqui service that accepts a raw SWIFT string and executes the full parse → deduplicate → correlate → stage pipeline. This is the **channel-agnostic boundary** that enables future SAA/JMS integration with zero business logic changes.

#### A. Service Signature

```
Service: trade.swift.InboundSwiftServices.ingest#SwiftMessage
In:  rawContent (String, required) — raw SWIFT message text
     sourceChannel (String, required) — "DIRECTORY_POLL" or "MANUAL_UPLOAD"
     sourceFileName (String, optional) — original filename for audit
Out: inboxItemId (String) — ID of the created TradeInboxItem, or null if duplicate
     correlationStatus (String) — "MATCHED", "ORPHANED", or "DUPLICATE"
```

#### B. Processing Steps

1.  **Parse** the raw string using Prowide Core (`SwiftMessage.parse(rawContent)`).
2.  **Extract** message type, Tag 20 (Sender's Reference), Tag 21 (Related Reference), and SWIFT Basic Header fields (Sender BIC, Session Number, Input Sequence Number).
3.  **Compute content hash** (SHA-256 of the raw content) for deduplication.
4.  **Check for duplicates** (see Feature 2).
5.  **Correlate** Tag 21 against `TradeInstrument.instrumentRef` (see Feature 3).
6.  **Create** `InboundSwiftRaw` audit record and `TradeInboxItem` record.
7.  **Return** the result.

---

## 2. Feature 2: Deduplication & Security Gate

### 2.1 The Deduplication Problem

With two ingestion channels (directory polling + manual upload) and the possibility of re-processing archived files, the system must guarantee that the same SWIFT message is never processed twice.

### 2.2 Deduplication Strategy: Content Hash + SWIFT Header Composite Key

Every inbound SWIFT message is fingerprinted using two complementary keys:

*   **Content Hash:** SHA-256 of the full raw message text. Catches exact byte-level duplicates from any source.
*   **SWIFT Header Composite Key:** `Sender BIC` + `Message Type` + `Tag 20 (Sender's Reference)` + `Date` from Basic Header Block 2. Catches logically equivalent messages even if whitespace or trailing characters differ.

#### A. New Entity: `InboundSwiftRaw`

| Field Name | Req/Opt | Data Type | Description |
| :--- | :--- | :--- | :--- |
| `rawMessageId` | Req (PK) | id | System-generated. |
| `contentHash` | Req | text-medium | SHA-256 hex digest of the raw message text. **Unique index.** |
| `swiftHeaderKey` | Req | text-medium | Composite key: `{senderBic}:{messageType}:{tag20}:{date}`. **Unique index.** |
| `rawContent` | Req | text-very-long | Original raw SWIFT text (immutable audit copy). |
| `sourceChannel` | Req | id | Enum: `SRC_DIRECTORY_POLL`, `SRC_MANUAL_UPLOAD`. |
| `sourceFileName` | Opt | text-medium | Original filename (for audit trail). |
| `receivedTimestamp` | Req | date-time | When the system ingested the message. |
| `parseStatusEnumId` | Req | id | `PARSE_SUCCESS`, `PARSE_FAILED`. |
| `parseErrorText` | Opt | text-long | Error details if parsing failed. |

#### B. Deduplication Rules

| Rule ID | Rule |
| :--- | :--- |
| DDP-01 | Before creating any `InboundSwiftRaw` record, query for existing records matching either `contentHash` OR `swiftHeaderKey`. If found, mark as `DUPLICATE` and skip all downstream processing. |
| DDP-02 | The duplicate check must be atomic (database-level unique constraint) to prevent race conditions between concurrent polling and manual upload. |
| DDP-03 | When a duplicate is detected via manual upload, the UI must display: "This message has already been processed on [date]. Inbox Item: [link]." |

### 2.3 The "No-Touch" Security Rule

Once the raw content is persisted to `InboundSwiftRaw`, operators must never interact with the raw text file again. All subsequent work happens through the parsed `TradeInboxItem` entity.

| Rule ID | Rule |
| :--- | :--- |
| SEC-01 | The raw `.txt` file is read once, hashed, persisted to DB, then moved to archive. Operators interact only with parsed database records. |
| SEC-02 | `InboundSwiftRaw.rawContent` is immutable after creation. No update service is exposed. |
| SEC-03 | Manual upload requires `TRADE_OPS_MAKER` role. The uploaded filename and user ID are logged in `InboundSwiftRaw`. |

### 2.4 User Stories

##### US-SEC-01: Duplicate Message Prevention
**As a** Trade Operations system,
**I want** to detect and reject duplicate SWIFT messages across both automated and manual channels,
**So that** the same message is never processed twice, preventing double state transitions or double payments.

##### US-SEC-02: Immutable Audit Trail
**As a** Compliance Officer,
**I want** every inbound SWIFT message stored with its original content and a SHA-256 hash,
**So that** I can prove exactly what was received and when, without risk of tampering.

---

## 3. Feature 3: Tag 21 Correlation Engine

### 3.1 The Golden Rule of SWIFT Routing

When our system sends an outbound message (MT 700, MT 707), it places the LC Number (`TradeInstrument.instrumentRef`) in **Tag 20 (Sender's Reference)**. When the foreign bank replies, they are universally mandated to place that exact number in **Tag 21 (Related Reference)**.

The correlation engine uses this convention to link every inbound message back to its parent LC.

### 3.2 Correlation Logic

```
1. Extract Tag 21 value from the parsed SWIFT message.
2. Query: SELECT instrumentId FROM TradeInstrument WHERE instrumentRef = [Tag 21 Value]
3. If exactly one match → MATCHED. Link to this LC.
4. If zero matches → ORPHANED. Route to Orphan Queue.
5. If multiple matches → ORPHANED with reason "AMBIGUOUS_REF". Route to Orphan Queue.
```

### 3.3 Sub-Correlation: Amendment vs Issuance

When an MT 730 or MT 799 is correlated to an LC, the system must determine *which specific event* it is acknowledging:

*   **MT 730:** Extract Tag 30 (Date of Message Acknowledged). Compare against the `issueDate` of the LC and the `amendmentDate` of any pending amendments. If Tag 30 matches an amendment date, link to that specific `ImportLcAmendment` record.
*   **MT 799:** Always linked to the LC at the top level. The operator determines which amendment (if any) it pertains to by reading the free-text narrative (Tag 79).

### 3.4 The Orphan Queue

When Tag 21 is missing, blank, contains a typo, or matches zero/multiple LCs, the message must not be discarded.

#### A. Orphan Queue Entity Fields (on `TradeInboxItem`)

When a message is orphaned, the `TradeInboxItem` record is created with:
*   `instrumentId` = null
*   `correlationStatusEnumId` = `CORR_ORPHANED`
*   `orphanReason` = descriptive text (e.g., "Tag 21 value 'LC-2026-XYZ' not found in system", "Tag 21 missing", "Multiple LCs match Tag 21")

#### B. Manual Linking UI

An operations manager opens the Orphan Queue, reads the parsed narrative, manually searches the Moqui database for the correct LC, and clicks **"Link to LC"**. This updates the `TradeInboxItem.instrumentId` and changes `correlationStatusEnumId` to `CORR_MANUAL_MATCH`.

#### C. Correlation Rules

| Rule ID | Rule |
| :--- | :--- |
| COR-01 | Tag 21 is the primary correlation key. System queries `TradeInstrument.instrumentRef` for exact match. |
| COR-02 | If Tag 21 is missing or blank, route to Orphan Queue. Do not reject the message. |
| COR-03 | If Tag 21 matches multiple LCs, route to Orphan Queue with reason `AMBIGUOUS_REF`. |
| COR-04 | For MT 730: Sub-correlate using Tag 30 against `ImportLcAmendment.amendmentDate` to identify the specific amendment being acknowledged. |
| COR-05 | For MT 799: No sub-correlation. Operator determines context from Tag 79 narrative. |
| COR-06 | Manual linking in Orphan Queue requires `TRADE_OPS_MANAGER` role. |

### 3.5 User Stories

##### US-COR-01: Automatic Tag 21 Correlation
**As a** Trade Operations system,
**I want** to automatically match inbound SWIFT messages to LC records using Tag 21,
**So that** operators see inbound messages pre-linked to the correct LC without manual searching.

##### US-COR-02: Orphan Queue for Unmatched Messages
**As a** Trade Operations Manager,
**I want** unmatched messages routed to an Orphan Queue where I can manually link them to an LC,
**So that** no inbound message is ever lost due to a typo or missing reference.

---

## 4. Feature 4: Trade Inbox Entity & Operator Dashboard

### 4.1 Core Entity: `TradeInboxItem`

Every successfully parsed inbound message creates a record in this entity, regardless of correlation status.

| Field Name | Req/Opt | Data Type | Description |
| :--- | :--- | :--- | :--- |
| `inboxItemId` | Req (PK) | id | System-generated. |
| `rawMessageId` | Req | id | FK → `InboundSwiftRaw`. Links to the raw audit record. |
| `instrumentId` | Opt | id | FK → `TradeInstrument`. Null if orphaned. |
| `amendmentId` | Opt | id | FK → `ImportLcAmendment`. Populated when sub-correlated to a specific amendment. |
| `messageType` | Req | text-short | SWIFT message type (e.g., `730`, `799`, `750`, `754`, `742`, `999`, `720`). |
| `senderBic` | Req | text-short | Parsed from SWIFT Basic Header Block 1. |
| `senderReference` | Opt | text-medium | Tag 20 — the foreign bank's own reference. |
| `relatedReference` | Opt | text-medium | Tag 21 — our LC number (the correlation key). |
| `narrativeText` | Opt | text-very-long | Tag 79 (MT 799/999) or Tag 77J (MT 750) — free-text content. |
| `claimAmount` | Opt | number-decimal | Tag 32B (MT 750/754/742) — the claimed payment amount. |
| `claimCurrency` | Opt | id | Tag 32B currency code. |
| `receivedTimestamp` | Req | date-time | When the message was ingested. |
| `inboxStatusEnumId` | Req | id | `INBOX_UNREAD`, `INBOX_IN_PROGRESS`, `INBOX_PROCESSED`, `INBOX_ORPHANED`. |
| `correlationStatusEnumId` | Req | id | `CORR_AUTO_MATCH`, `CORR_MANUAL_MATCH`, `CORR_ORPHANED`. |
| `orphanReason` | Opt | text-medium | Why correlation failed (if orphaned). |
| `processedByUserId` | Opt | id | FK → `UserAccount`. The operator who actioned this item. |
| `processedTimestamp` | Opt | date-time | When the operator actioned this item. |
| `actionTaken` | Opt | text-medium | The operator action (e.g., `ACKNOWLEDGE`, `ACCEPT_AMENDMENT`, `REJECT_AMENDMENT`, `WAIVE`, `REFUSE`). |
| `securityWarningFlag` | Opt | text-indicator | `Y` if MT 999 (unauthenticated). UI renders severe warning. |

#### Relationships

| Relationship | Target Entity | Type |
| :--- | :--- | :--- |
| `rawMessage` | `InboundSwiftRaw` | one |
| `instrument` | `TradeInstrument` | one (optional) |
| `amendment` | `ImportLcAmendment` | one (optional) |
| `processedBy` | `moqui.security.UserAccount` | one-nofk (optional) |

#### Seed Data: Enumerations & Statuses

```
InboxStatus: INBOX_UNREAD, INBOX_IN_PROGRESS, INBOX_PROCESSED, INBOX_ORPHANED
CorrelationStatus: CORR_AUTO_MATCH, CORR_MANUAL_MATCH, CORR_ORPHANED
SourceChannel: SRC_DIRECTORY_POLL, SRC_MANUAL_UPLOAD
ParseStatus: PARSE_SUCCESS, PARSE_FAILED
```

### 4.2 Operator Dashboard

The Trade Inbox acts like an email inbox for the operations team.

#### A. Dashboard Requirements

*   **List View:** Shows all `TradeInboxItem` records filtered by `inboxStatusEnumId`. Default filter: `INBOX_UNREAD`.
*   **Columns:** Received Date, Message Type, Sender BIC, Related LC Number, Claim Amount, Status, Security Warning icon.
*   **Sorting:** Default sort by `receivedTimestamp` descending (newest first).
*   **Filtering:** By message type, by status, by LC number, by date range.
*   **Badge Count:** The main navigation shows an unread count badge (count of `INBOX_UNREAD` items).
*   **LC Detail Integration:** When viewing an LC detail screen, show an alert: *"N New Inbound Messages"* with a link to the filtered inbox.

#### B. MT 999 Security Warning

| Rule ID | Rule |
| :--- | :--- |
| SEC-04 | When `messageType = '999'`, set `securityWarningFlag = 'Y'`. |
| SEC-05 | UI renders a prominent red banner: **"⚠️ SECURITY WARNING: This message lacks SWIFT cryptographic authentication. Do NOT execute financial actions based on this message."** |
| SEC-06 | Operators must never execute financial amendments, payments, or cancellations based on an MT 999. It is valid only for non-financial queries. |

### 4.3 User Stories

##### US-INB-01: Trade Inbox Dashboard
**As a** Trade Operations Maker,
**I want** to see all inbound SWIFT messages in a unified inbox pre-linked to their parent LCs,
**So that** I can review and action inbound communications without switching between systems.

##### US-INB-02: Unread Message Badge
**As a** Trade Operations Maker,
**I want** to see an unread message count on the main navigation,
**So that** I am immediately aware when new inbound messages arrive.

##### US-INB-03: MT 999 Security Warning
**As a** Trade Operations Maker,
**I want** unauthenticated MT 999 messages to display a prominent security warning,
**So that** I never accidentally execute a financial action based on an unauthenticated message.

---

## 5. Feature 5: Message-Specific Business Process Automations

When an operator clicks a row in the Trade Inbox, the UI reads `messageType` and dynamically renders the appropriate action screen with message-type-specific buttons. This section defines the exact data mapping, operator actions, and state machine impact for each supported inbound message type.

### 5.1 Inbound MT 730 (Acknowledgment)

The Advising Bank sends this to confirm they received and delivered our MT 700 (Issuance) or MT 707 (Amendment) to the Beneficiary.

#### A. Data Mapping (MT 730 → Moqui)

| SWIFT Tag | Description | Moqui Mapping |
| :--- | :--- | :--- |
| **20** | Sender's Reference | Saved as `senderReference` on `TradeInboxItem`. Stored as `advisingBankReference` on the LC record. |
| **21** | Related Reference | Used by Correlation Engine to find the LC. |
| **30** | Date of Msg Acknowledged | Used for sub-correlation: matches against LC `issueDate` or `ImportLcAmendment.amendmentDate`. |
| **71D / 72Z** | Charges / Notes | Displayed as read-only text on the Inbox action screen. |

#### B. Operator Action: "Acknowledge Receipt"

The MT 730 is purely administrative. The operator has a single action button.

*   **If acknowledging MT 700 Issuance:** Sets `ImportLetterOfCredit.isAdvised = 'Y'`, `advisedDate = Tag 30`, and `advisingBankReference = Tag 20`. The LC **stays in `LC_ISSUED`** — no state change, because "advised" does not change what operations are allowed.
*   **If acknowledging MT 707 Amendment:** Sets `ImportLcAmendment.isAdvisedToBeneficiary = 'Y'` and `advisedToBeneficiaryDate = Tag 30`. The amendment **stays in `BENE_PENDING`** — delivery receipt is informational, not behavioral.
*   **System Action:** Marks the `TradeInboxItem` as `INBOX_PROCESSED`. If Tag 71D contains advising fees, the system logs a fee note for the Accounting module.

#### C. Flag Fields (Not Status Changes)

> **Design Decision:** `LC_ADVISED` and `BENE_ADVISED` are **not** separate statuses. Analysis of the existing state machine shows that "advised" has zero behavioral difference from "issued"/"pending" — all downstream transitions and processing are identical. Using separate statuses would require duplicating 6 `StatusFlowTransition` entries and modifying the `create#Presentation` guard for no processing benefit. Instead, we use flag fields:

| Entity | New Field | Type | Description |
| :--- | :--- | :--- | :--- |
| `ImportLetterOfCredit` | `isAdvised` | text-indicator | `Y` when MT 730 confirms issuance delivery. Default `N`. |
| `ImportLetterOfCredit` | `advisedDate` | date | Date from MT 730 Tag 30. |
| `ImportLetterOfCredit` | `advisingBankReference` | text-medium | MT 730 Tag 20 (Advising Bank's own reference). |
| `ImportLcAmendment` | `isAdvisedToBeneficiary` | text-indicator | `Y` when MT 730 confirms amendment delivery. Default `N`. |
| `ImportLcAmendment` | `advisedToBeneficiaryDate` | date | Date from MT 730 Tag 30. |

#### D. Rules

| Rule ID | Rule |
| :--- | :--- |
| MT730-01 | MT 730 acknowledgment does not constitute Beneficiary acceptance of an amendment. It only proves delivery. |
| MT730-02 | If Tag 30 matches an amendment date, the acknowledgment is linked to that specific amendment record. |
| MT730-03 | If Tag 71D contains fee information, the system creates a fee note for manual review by the Accounting team. |

#### E. User Stories

##### US-MT730-01: Acknowledge LC Advised
**As a** Trade Operations Maker,
**I want** to acknowledge an inbound MT 730 with a single click,
**So that** the LC status reflects that the Beneficiary has been formally advised.

---

### 5.2 Inbound MT 799 (Free Format — Amendment Consent)

The Beneficiary's bank sends this free-text message to explicitly accept or reject a pending MT 707 amendment.

#### A. Data Mapping (MT 799 → Moqui)

| SWIFT Tag | Description | Moqui Mapping |
| :--- | :--- | :--- |
| **20** | Sender's Reference | Saved as `senderReference` on `TradeInboxItem`. Logged for audit trail. |
| **21** | Related Reference | Used by Correlation Engine to find the LC. |
| **79** | Narrative | **Critical:** Displayed prominently in the UI. Contains the actual consent text. |

#### B. Operator Actions & State Machine

The operator reads Tag 79 and makes a human decision to click one of two buttons.

**Action 1: Click "Accept Amendment"**
*   **Business Impact:** The amendment becomes legally binding. The new terms are the master contract.
*   **State Changes:**
    *   `ImportLcAmendment.beneficiaryConsentStatusId` → `BENE_ACCEPTED`
    *   `ImportLcAmendment.consentSwiftRef` ← Tag 20 from the MT 799
    *   `ImportLcAmendment.amendmentBusinessStateId` → `AMEND_COMMITTED`
*   **System Actions (SECA-triggered):**
    *   **The Merge:** Overwrite `ImportLetterOfCredit` master fields with the amendment delta (per existing amendment BRD §4 State 2).
    *   **Liability Release (Decrease):** If the amendment decreased the LC amount, release excess facility limits.
    *   Marks the `TradeInboxItem` as `INBOX_PROCESSED`.

**Action 2: Click "Reject Amendment"**
*   **Business Impact:** The amendment is dead. The original LC terms remain in force.
*   **State Changes:**
    *   `ImportLcAmendment.beneficiaryConsentStatusId` → `BENE_REJECTED`
    *   `ImportLcAmendment.amendmentBusinessStateId` → `AMEND_REJECTED`
*   **System Actions:**
    *   **The Rollback:** Master LC untouched.
    *   **Liability Release (Increase):** If the rejected amendment increased the amount, release the extra earmarked limit.
    *   Marks the `TradeInboxItem` as `INBOX_PROCESSED`.

**Implied Consent (UCP 600):**
In reality, ~80% of Beneficiaries never send an explicit response. They simply present documents matching the amended terms. The amendment BRD already includes a **"Force Accept (Implied Consent)"** manual override button on the pending amendment record. This existing mechanism is retained — it is not triggered from the Trade Inbox but from the Amendment UI directly.

#### C. Rules

| Rule ID | Rule |
| :--- | :--- |
| MT799-01 | MT 799 narrative (Tag 79) must NEVER be auto-evaluated for consent. Free text is ambiguous (e.g., "Beneficiary does *not* accept"). Only human operators make the Accept/Reject decision. |
| MT799-02 | Accept triggers the existing amendment merge SECA and liability release logic (per amendment BRD §4). |
| MT799-03 | Reject triggers liability un-earmarking for increases (per amendment BRD §4 State 3). |
| MT799-04 | The `consentSwiftRef` field on the amendment is populated with the MT 799 Tag 20 for audit traceability. |

#### D. User Stories

##### US-MT799-01: Amendment Consent via Trade Inbox
**As a** Trade Operations Maker,
**I want** to read the MT 799 narrative and click Accept or Reject to finalize a pending amendment,
**So that** the amendment consent is logged with a SWIFT reference and the LC master record is updated accordingly.

---

### 5.3 Inbound MT 750 (Advice of Discrepancy)

The Negotiating Bank sends this when documents do not match the LC terms and asks whether the Applicant will waive the errors.

#### A. Data Mapping (MT 750 → Moqui)

When the system correlates an MT 750, it **automatically spawns** a new child `TradeDocumentPresentation` record linked to the LC.

| SWIFT Tag | Description | Moqui Mapping |
| :--- | :--- | :--- |
| **20** | Sender's Reference | Saved as `presentingBankRef` on `TradeDocumentPresentation`. |
| **21** | Related Reference | Used to find the LC. |
| **32B** | Principal Claimed | Mapped to `TradeDocumentPresentation.claimAmount` and `claimCurrency`. |
| **33a / 73** | Additional Fees | Mapped to `TradeDocumentPresentation.chargesDeducted`. |
| **77J** | Discrepancies | Each line parsed and mapped to individual `PresentationDiscrepancy` records. |

#### B. Auto-Spawn Logic

Upon successful correlation of an MT 750:

1.  Create `TradeDocumentPresentation` with:
    *   `instrumentId` from correlation
    *   `presentationStatusId` = `PRES_RECEIVED`
    *   `isDiscrepant` = `Y`
    *   `applicantDecisionEnumId` = `PENDING`
    *   `presentingBankRef` = Tag 20
    *   `claimAmount` / `claimCurrency` from Tag 32B
2.  Parse Tag 77J into individual `PresentationDiscrepancy` records linked to the presentation.
3.  Update `ImportLetterOfCredit.businessStateId` → `LC_DOC_RECEIVED`.
4.  Start the UCP 600 5-banking-day examination countdown.

#### C. Operator Actions (Discrepancy Resolution)

See **Feature 6** for the full Maker/Checker discrepancy resolution workflow.

#### D. Rules

| Rule ID | Rule |
| :--- | :--- |
| MT750-01 | Upon MT 750 correlation, system auto-creates a `TradeDocumentPresentation` record in `PRES_RECEIVED` status with `isDiscrepant = Y`. |
| MT750-02 | Tag 77J discrepancies are parsed line-by-line and stored as individual `PresentationDiscrepancy` records. |
| MT750-03 | The 5-banking-day UCP 600 examination deadline starts from the presentation creation date. |
| MT750-04 | Claim amount (Tag 32B) must not exceed the LC's remaining balance + tolerance. If exceeded, the presentation is flagged for investigation. |

#### E. User Stories

##### US-MT750-01: Auto-Spawn Presentation from MT 750
**As a** Trade Operations system,
**I want** an inbound MT 750 to automatically create a Presentation record with parsed discrepancies,
**So that** the operations team can begin the waiver/refusal workflow immediately without manual data entry.

---

### 5.4 Inbound MT 754 (Advice of Clean Presentation)

The Negotiating Bank sends this when documents are perfect — no discrepancies. The bank is legally bound to pay.

#### A. Data Mapping (MT 754 → Moqui)

| SWIFT Tag | Description | Moqui Mapping |
| :--- | :--- | :--- |
| **20** | Sender's Reference | Saved as `presentingBankRef` on `TradeDocumentPresentation`. |
| **21** | Related Reference | Used to find the LC. |
| **32A/B** | Amount Claimed | Mapped to `claimAmount` and `claimCurrency`. |

#### B. Auto-Spawn Logic

Upon successful correlation of an MT 754:

1.  Create `TradeDocumentPresentation` with:
    *   `presentationStatusId` = `PRES_COMPLIANT` (skips examination queue entirely)
    *   `isDiscrepant` = `N`
    *   `applicantDecisionEnumId` = `WAIVED` (not applicable, but set to indicate no waiver needed)
2.  Update `ImportLetterOfCredit.businessStateId` → `LC_ACCEPTED`.

#### C. Operator Action

The operator does **not** need to contact the Applicant for a waiver. The Maker simply:
1.  Verifies the claim amount (Tag 32A/B).
2.  Inputs the Value Date.
3.  Submits for Checker authorization of the outbound MT 202 payment.

#### D. Rules

| Rule ID | Rule |
| :--- | :--- |
| MT754-01 | Upon MT 754 correlation, system auto-creates `TradeDocumentPresentation` in `PRES_COMPLIANT` status, skipping the examination queue. |
| MT754-02 | LC business state transitions directly to `LC_ACCEPTED`. |
| MT754-03 | Settlement (MT 202 generation) follows the existing Settlement process (import-lc-consolidated-brd §2.4) with Maker/Checker authorization. |

#### E. User Stories

##### US-MT754-01: Clean Presentation Fast-Track
**As a** Trade Operations system,
**I want** an inbound MT 754 to create a Presentation record that bypasses examination and goes directly to settlement readiness,
**So that** clean presentations are processed faster without unnecessary examination steps.

---

### 5.5 Inbound MT 742 (Reimbursement Claim)

The claiming bank sends this to request reimbursement under a prior MT 740 authorization.

#### A. Data Mapping (MT 742 → Moqui)

| SWIFT Tag | Description | Moqui Mapping |
| :--- | :--- | :--- |
| **20** | Sender's Reference | Logged for audit. |
| **21** | Related Reference | Used to find the LC. Then sub-correlated to the active `NostroReconciliation` record. |
| **32B** | Claim Amount | Compared against `NostroReconciliation.expectedAmount`. |

#### B. Auto-Match Logic (Straight-Through Processing)

1.  Correlate Tag 21 to the LC.
2.  Find the active `NostroReconciliation` record (status `RECON_PENDING`) linked to the LC.
3.  **If `claimAmount` ≤ `expectedAmount`:** Auto-match. Update `NostroReconciliation.matchStatusEnumId` → `RECON_MATCHED`. Queue the outbound MT 202 payment for Maker/Checker authorization.
4.  **If `claimAmount` > `expectedAmount`:** Route to a `PRES_EXCEPTION` queue. The claim exceeds the authorization — an operator must investigate.

#### C. Rules

| Rule ID | Rule |
| :--- | :--- |
| MT742-01 | MT 742 bypasses the Trade Inbox operator workflow and routes directly to the Settlement/Reimbursement queue. |
| MT742-02 | If claim amount ≤ authorized amount, system auto-matches the `NostroReconciliation` record. |
| MT742-03 | If claim amount > authorized amount, system creates an exception record and alerts the operator. |
| MT742-04 | MT 742 settlement still requires Maker/Checker authorization before the MT 202 is dispatched. |

#### D. User Stories

##### US-MT742-01: Automated Reimbursement Matching
**As a** Trade Operations system,
**I want** inbound MT 742 reimbursement claims to auto-match against MT 740 authorizations,
**So that** conforming claims are settled via straight-through processing without manual intervention.

---

## 6. Feature 6: Discrepancy Resolution (Maker/Checker)

When an MT 750 has auto-spawned a discrepant presentation, the operations team must resolve the discrepancies by contacting the Applicant for a waiver or refusal decision. This section defines the Maker/Checker workflow.

### 6.1 The "Resolve Discrepancies" Workflow

```
                  ┌────────────────────────────┐
 Inbound MT 750 → │ TradeDocumentPresentation   │
                  │ status = PRES_RECEIVED      │
                  │ isDiscrepant = Y            │
                  └─────────────┬──────────────┘
                                │
                      Maker contacts Applicant
                      (offline: phone/email/letter)
                                │
                ┌───────────────┴───────────────┐
                │                               │
          Applicant WAIVES               Applicant REFUSES
                │                               │
    ┌───────────┴───────────┐        ┌──────────┴──────────┐
    │ Maker uploads Waiver  │        │ Maker uploads Refusal│
    │ PDF/scan (mandatory)  │        │ PDF/scan (mandatory) │
    └───────────┬───────────┘        └──────────┬──────────┘
                │                               │
    ┌───────────┴───────────┐        ┌──────────┴──────────┐
    │ Checker authorizes    │        │ Checker authorizes   │
    │ → PRES_COMPLIANT      │        │ → PRES_REJECTED      │
    │ → Generate MT 752     │        │ → Generate MT 734    │
    │ → Proceed to Settle   │        │ → Close presentation │
    └───────────────────────┘        └─────────────────────┘
```

### 6.2 Mandatory Upload Gate

The Applicant's waiver or refusal is received offline (phone call, signed letter, email). The system enforces that this evidence is uploaded before submission.

| Rule ID | Rule |
| :--- | :--- |
| DISC-01 | The Maker MUST upload an Applicant waiver/refusal document (PDF/scan) before submitting the discrepancy resolution. The Submit button is disabled until a file is attached. |
| DISC-02 | The uploaded document is stored as a Moqui content attachment linked to the `TradeDocumentPresentation` record. |
| DISC-03 | The Checker sees the uploaded document inline and must confirm they reviewed it before authorizing. |

### 6.3 Maker Actions

**Waive Discrepancies:**
1.  Maker opens the presentation.
2.  Marks each `PresentationDiscrepancy` as `isWaived = Y`.
3.  Uploads the Applicant's waiver document (mandatory).
4.  Clicks "Submit for Authorization".
5.  `TradeDocumentPresentation.applicantDecisionEnumId` → `WAIVED`.
6.  `TradeDocumentPresentation.presentationStatusId` → `PRES_EXAMINING`.

**Refuse Discrepancies:**
1.  Maker opens the presentation.
2.  Marks each `PresentationDiscrepancy` as `isWaived = N` (the default).
3.  Selects document disposal option (`HOLDING_DOCUMENTS` or `RETURNING_DOCUMENTS`).
4.  Uploads the Applicant's refusal document (mandatory).
5.  Clicks "Submit for Authorization".
6.  `TradeDocumentPresentation.applicantDecisionEnumId` → `REFUSED`.
7.  `TradeDocumentPresentation.presentationStatusId` → `PRES_EXAMINING`.

### 6.4 Checker Authorization

The Checker reviews the Maker's decision, uploaded evidence, and parsed discrepancies.

**If Waived:**
1.  `TradeDocumentPresentation.presentationStatusId` → `PRES_COMPLIANT`.
2.  System generates outbound **MT 752** (Discrepancy Waiver Advice) — uses the existing SECA.
3.  `ImportLetterOfCredit.businessStateId` → `LC_ACCEPTED`.
4.  Proceed to Settlement workflow (per import-lc-consolidated-brd §2.4).

**If Refused:**
1.  `TradeDocumentPresentation.presentationStatusId` → `PRES_REJECTED`.
2.  System generates outbound **MT 734** (Refusal) with Tag 77B disposal instructions.
3.  `ImportLetterOfCredit.businessStateId` remains at `LC_DOC_RECEIVED` (or reverts to `LC_ISSUED` if this was the only pending presentation).

### 6.5 UCP 600 5-Banking-Day Countdown

| Rule ID | Rule |
| :--- | :--- |
| DISC-04 | The 5-banking-day examination deadline countdown begins from the `TradeDocumentPresentation.presentationDate`. |
| DISC-05 | A scheduled job runs daily to flag presentations approaching the deadline (3 days remaining: amber warning, 5 days reached: red alert). |
| DISC-06 | If the deadline passes without action, the system creates a compliance alert. Under UCP 600, failure to refuse within 5 banking days constitutes implicit acceptance. |

### 6.6 User Stories

##### US-DISC-01: Mandatory Waiver Upload
**As a** Trade Operations Maker,
**I want** the Submit button to be disabled until I upload the Applicant's waiver or refusal document,
**So that** every discrepancy resolution has auditable evidence of the Applicant's decision.

##### US-DISC-02: Checker Discrepancy Authorization
**As a** Trade Operations Checker,
**I want** to review the Maker's waiver/refusal decision and the uploaded Applicant evidence,
**So that** I can authorize or reject the resolution with full context.

##### US-DISC-03: UCP 600 Deadline Alert
**As a** Trade Operations Manager,
**I want** the system to alert me when a presentation approaches its 5-banking-day examination deadline,
**So that** we comply with UCP 600 timing requirements.

---

## 7. Gap Analysis: Current System vs This BRD

This section identifies the gaps between the existing Moqui implementation and the requirements of this BRD.

### 7.1 Entity Gaps

| Gap | Current State | Required Change |
| :--- | :--- | :--- |
| `InboundSwiftRaw` entity | Does not exist. | **[NEW]** Create entity in new `InboundSwiftEntities.xml`. |
| `TradeInboxItem` entity | Does not exist. | **[NEW]** Create entity in new `InboundSwiftEntities.xml`. |
| `SwiftMessage.messageDirection` | Not present. Existing `SwiftMessage` is implicitly outbound-only. | **[MODIFY]** Add `messageDirection` field (`OUTBOUND`, `INBOUND`). Existing records default to `OUTBOUND`. |
| Advised flag fields | `ImportLetterOfCredit` has no advised tracking. `ImportLcAmendment` has no advised/consent tracking. | **[ADD]** LC: `isAdvised`, `advisedDate`, `advisingBankReference`. Amendment: `isAdvisedToBeneficiary`, `advisedToBeneficiaryDate`, `consentSwiftRef`. |
| `TradeConfig` entity | Exists (`trade.common.TradeConfig`). | **[REUSE]** Use existing entity for `SWIFT_INBOUND_DIR`, `SWIFT_ARCHIVE_DIR`, etc. |

### 7.2 Service Gaps

| Gap | Current State | Required Change |
| :--- | :--- | :--- |
| `InboundSwiftServices` | Does not exist. | **[NEW]** New service file `trade/swift/InboundSwiftServices.xml` with `ingest#SwiftMessage`, `poll#InboundDirectory`, `upload#SwiftFile`. |
| `InboundCorrelationServices` | Does not exist. | **[NEW]** New service file with `correlate#SwiftMessage`, `link#OrphanMessage`. |
| `InboundActionServices` | Does not exist. | **[NEW]** Service file with `acknowledge#Mt730`, `processConsent#Mt799`, `spawnPresentation#Mt750`, `matchReimbursement#Mt742`. |
| SECA for inbound actions | Only outbound SECAs exist (`ImportLc.secas.xml`). | **[NEW]** New `InboundSwift.secas.xml` for triggers (e.g., MT 750 → auto-spawn presentation, MT 742 → auto-match). |
| Prowide Core dependency | Already present (`pw-swift-core:SRU2023-10.1.1`). | **[REUSE]** No change needed. |

### 7.3 UI/Screen Gaps

| Gap | Current State | Required Change |
| :--- | :--- | :--- |
| Trade Inbox screen | Does not exist. | **[NEW]** `TradeInbox.xml` list screen with filtering, sorting, badge count. |
| Inbox Detail/Action screen | Does not exist. | **[NEW]** `TradeInboxDetail.xml` with message-type-specific action panels. |
| Manual Upload screen | Does not exist. | **[NEW]** `SwiftManualUpload.xml` with file upload widget. |
| Orphan Queue screen | Does not exist. | **[NEW]** `OrphanQueue.xml` with LC search and manual linking. |
| LC Detail panel | Existing `LcDetail.xml`. | **[MODIFY]** Add "Inbound Messages" alert/badge linking to filtered Trade Inbox. |

### 7.4 Existing Infrastructure to Reuse

| Component | Location | How It's Reused |
| :--- | :--- | :--- |
| Prowide Core library | `build.gradle` dependency | Parsing inbound messages uses the same library as outbound generation. |
| `SwiftMessage` entity | `ImportLcEntities.xml:220` | Outbound messages are already stored here. Add `messageDirection` field. |
| `TradeConfig` entity | `TradeCommonEntities.xml:145` | Store all polling configuration keys. |
| `TradeTransaction` + Maker/Checker | `TradeCommonEntities.xml:442` | Inbound actions flow through the same `TradeTransaction` authorization pipeline. |
| `PresentationDiscrepancy` entity | `ImportLcEntities.xml:492` | MT 750 discrepancies map directly to existing entity fields. |
| `NostroReconciliation` entity | `ImportLcEntities.xml:559` | MT 742 auto-matching uses the existing reconciliation entity. |
| SECA architecture | `ImportLc.secas.xml` | Same pattern used for inbound triggers. |
| Existing settlement services | `ImportLcServices.xml` | MT 754 clean presentations flow into existing settlement workflow. |

---

## 7A. Impact Analysis: Changes to Existing Import LC Business Processes

This section documents the specific modifications required to existing state machine, services, SECAs, and UI components. These are not new features — they are **breaking or constraining changes** to code that already works.

### 7A.1 State Machine: No Changes Required (Flag-Based Design)

The existing `ImportLcDefault` status flow (lines 199–216 of `ImportLcEntities.xml`) requires **no modifications**.

> **Design Decision:** The original analysis proposed two new statuses (`LC_ADVISED`, `BENE_ADVISED`), which would have required 6+ duplicated `StatusFlowTransition` entries and modifications to the `create#Presentation` guard. Upon closer analysis, "advised" has zero behavioral difference from "issued"/"pending" — all downstream transitions and processing are identical.
>
> Instead, we use **flag fields** on the existing entities (see §5.1.C). This means:
> - No new `StatusItem` records needed
> - No new `StatusFlowTransition` records needed
> - No changes to `validate#BusinessStateTransition` guard
> - No changes to the `create#Presentation` state guard (line 46 of TradeCommonServices.xml)
> - The existing consent flow (`BENE_PENDING → BENE_ACCEPTED / BENE_REJECTED`) remains unchanged

#### Entity Field Additions

| Entity | Field | Type | Purpose |
| :--- | :--- | :--- | :--- |
| `ImportLetterOfCredit` | `isAdvised` | text-indicator | `Y` when MT 730 confirms issuance delivery |
| `ImportLetterOfCredit` | `advisedDate` | date | MT 730 Tag 30 date |
| `ImportLetterOfCredit` | `advisingBankReference` | text-medium | MT 730 Tag 20 |
| `ImportLcAmendment` | `isAdvisedToBeneficiary` | text-indicator | `Y` when MT 730 confirms amendment delivery |
| `ImportLcAmendment` | `advisedToBeneficiaryDate` | date | MT 730 Tag 30 date |

---

### 7A.2 Service Guard Modifications

#### A. `validate#BusinessStateTransition` (ImportLcValidationServices.xml:72)

**No code change needed** — with the flag-based design, the LC stays in `LC_ISSUED` when advised. The existing transitions from `LC_ISSUED` cover all cases. ✅

#### B. `create#Presentation` Guard (TradeCommonServices.xml:46) — **NO CHANGE NEEDED**

**Current code:**
```groovy
if (!lc || !(lc.businessStateId in ['LC_ISSUED', 'LC_AMENDED', 'LC_DOC_RECEIVED']))
```

**With flag-based design:** Since the LC stays in `LC_ISSUED` when advised (only `isAdvised` flag changes), the existing guard works without modification. ✅

#### C. `validate#Presentation` Drawing Check (ImportLcValidationServices.xml:33)

**Current behavior:** Checks `claimAmount` against tolerance and expiry date. Uses `ec.message.addError()` to block the presentation entirely if discrepant.

**Problem for MT 750:** An inbound MT 750 is **by definition discrepant**. The current validation service will block the presentation creation because the amount may exceed tolerance or the presentation is past expiry. But the whole point of MT 750 is to present **despite** discrepancies — the Applicant decides to waive or refuse.

**Required change:** The MT 750 auto-spawn service must bypass the `validate#Presentation` SECA or call a new `create#InboundPresentation` service that skips the pre-check validation. Alternatively, the SECA on `create#Presentation` (line 28 of `ImportLc.secas.xml`) must detect inbound-source presentations and skip validation.

#### D. Amendment Commit Logic — **CURRENTLY MISSING**

The BRD §5.2 requires that when an operator clicks "Accept Amendment" on an MT 799, the system must:
1.  Update `ImportLcAmendment.amendmentBusinessStateId` → `AMEND_COMMITTED`
2.  Merge the amendment delta into `ImportLetterOfCredit` master fields (effective amount, expiry, etc.)

**Current state:** There is no `commit#Amendment` or `merge#Amendment` service. The `AMEND_APPROVED → AMEND_COMMITTED` transition exists in seed data, but the merge logic (overwriting `effectiveAmount`, `effectiveExpiryDate`, etc.) must be **implemented as a new service**. This is not an inbound-specific gap — it's an existing Phase 1 gap that the inbound processing will now depend on.

---

### 7A.3 SECA Conflict Analysis

#### A. Outbound SWIFT Regeneration SECA (ImportLc.secas.xml:42–54)

**Current behavior:** When `update#trade.importlc.ImportLetterOfCredit` is called and the LC is in `LC_DRAFT` or `LC_PENDING`, the SECA regenerates the MT 700.

**Risk:** When the inbound MT 730 triggers `update#ImportLetterOfCredit` to change state from `LC_ISSUED` to `LC_ADVISED`, the SECA condition (`LC_DRAFT` or `LC_PENDING`) will NOT fire — **no conflict**. ✅

#### B. Presentation Validation SECA (ImportLc.secas.xml:28–30)

**Current behavior:** `create#Presentation` pre-service calls `validate#Presentation`.

**Conflict:** As described in §7A.2.C, this SECA will block MT 750 auto-spawned presentations. The inbound service must either:
1.  Call `create#trade.importlc.TradeDocumentPresentation` directly (bypassing the wrapper service and its SECA), OR
2.  Add a context flag (`sourceChannel = 'INBOUND'`) and modify the SECA to skip validation when the flag is set.

**Recommendation:** Option 2 is cleaner. It preserves the audit trail and transaction creation from the wrapper service.

#### C. Post-Authorization SWIFT Generation SECA (ImportLc.secas.xml:87–105)

**Current behavior:** When a `TradeTransaction` is approved (`TX_APPROVED`), the SECA regenerates MT 700/MT 740 if the transaction type is `IMP_NEW`.

**Risk:** Inbound actions will create `TradeTransaction` records of new types (e.g., `IMP_INBOUND_ACTION`). The existing SECA only checks for `IMP_NEW` — **no conflict** as long as we use a distinct transaction type. ✅

---

### 7A.4 UI Integration Points

#### A. LC Dashboard Status Filters

**Current state:** The LC dashboard filters include statuses: `LC_DRAFT`, `LC_PENDING`, `LC_ISSUED`, `LC_DOC_RECEIVED`, `LC_DISCREPANT`, `LC_ACCEPTED`, `LC_SETTLED`, `LC_EXPIRED`, `LC_CLOSED`, `LC_CANCELLED`, `LC_AMENDMENT_PENDING`.

**Required change:** Add `LC_ADVISED` to the status filter list and assign it an appropriate color tag (suggested: blue, indicating "active but acknowledged").

#### B. LC Detail Screen — Amendment Consent Panel

**Current state:** The amendment section shows `beneficiaryConsentStatusId` with values: `BENE_PENDING`, `BENE_ACCEPTED`, `BENE_REJECTED`.

**Required change:** Add `BENE_ADVISED` rendering (suggested: amber tag "Delivered — Awaiting Consent").

#### C. LC Detail Screen — Inbound Messages Tab/Alert

**Required change:** New section on the LC detail screen showing a count of unread `TradeInboxItem` records linked to this LC, with a link to the filtered Trade Inbox.



---

## 8. Traceability Matrix: Business Processes → System Artifacts

This matrix maps each business process from the [source requirements §3.1–3.6](file:///Users/me/myprojects/moqui-trade/docs/requirements/20260507-INCOMMING%20COMMUNICATION.md) to the concrete system artifacts that implement it, identifying what exists, what needs modification, and what is new.

---

### 8.1 Process 3.1: Import LC Issuance (MT 730 Acknowledgment)

**Requirements summary:** MT 730 arrives confirming the Advising Bank delivered our MT 700 to the Beneficiary. Operator clicks "Acknowledge" → `isAdvised = 'Y'` flag is set. LC **stays in `LC_ISSUED`**.

| Aspect | Existing Artifact | Required Change |
| :--- | :--- | :--- |
| **State: LC lifecycle** | [ImportLcEntities.xml:186–196](file:///Users/me/myprojects/moqui-trade/runtime/component/TradeFinance/entity/ImportLcEntities.xml#L186-L196) — `TradeLcBusinessState` | **[NO CHANGE]** — flag-based design, LC stays in `LC_ISSUED` |
| **State: Transition guard** | [ImportLcValidationServices.xml:72–103](file:///Users/me/myprojects/moqui-trade/runtime/component/TradeFinance/service/trade/importlc/ImportLcValidationServices.xml#L72-L103) — queries `StatusFlowTransition` | **[NO CHANGE]** |
| **Service: Update LC** | [ImportLcServices.xml](file:///Users/me/myprojects/moqui-trade/runtime/component/TradeFinance/service/trade/importlc/ImportLcServices.xml) — `update#ImportLetterOfCredit` | **[REUSE]** — called with `isAdvised: 'Y'`, `advisedDate`, `advisingBankReference` |
| **Service: Inbox action** | Does not exist | **[NEW]** `InboundActionServices.acknowledge#Mt730` |
| **SECA: Outbound regen** | [ImportLc.secas.xml](file:///Users/me/myprojects/moqui-trade/runtime/component/TradeFinance/service/ImportLc.secas.xml) — fires on `LC_DRAFT`/`LC_PENDING` only | **[NO CONFLICT]** — MT 730 action happens in `LC_ISSUED` state |
| **Entity: Advised fields** | [ImportLcEntities.xml](file:///Users/me/myprojects/moqui-trade/runtime/component/TradeFinance/entity/ImportLcEntities.xml) — `ImportLetterOfCredit` has no advised tracking fields | **[ADD]** New fields: `isAdvised`, `advisedDate`, `advisingBankReference` |
| **UI: Dashboard** | Frontend LC detail screen | **[MODIFY]** Show "Advised ✓" indicator when `isAdvised = 'Y'` (no new filter — same status) |

---

### 8.2 Process 3.2: Import LC Amendments (MT 730 + MT 799)

**Requirements summary:** MT 730 confirms amendment delivery → `Advised to Beneficiary`. MT 799 carries explicit accept/reject → triggers merge or rollback via SECA.

| Aspect | Existing Artifact | Required Change |
| :--- | :--- | :--- |
| **State: Consent status** | [ImportLcEntities.xml:461–468](file:///Users/me/myprojects/moqui-trade/runtime/component/TradeFinance/entity/ImportLcEntities.xml#L461-L468) — `BeneficiaryConsentStatus` has `BENE_PENDING`, `BENE_ACCEPTED`, `BENE_REJECTED` | **[NO CHANGE]** — flag-based design, amendment stays in `BENE_PENDING` until explicit accept/reject |
| **State: Amendment lifecycle** | [ImportLcEntities.xml:454–473](file:///Users/me/myprojects/moqui-trade/runtime/component/TradeFinance/entity/ImportLcEntities.xml#L454-L473) — `AMEND_APPROVED → AMEND_COMMITTED` transition exists | **[NO CHANGE]** to transition, but merge service is missing (see below) |
| **Service: Amendment merge** | Does not exist — `AMEND_COMMITTED` seed data exists but no service implements the delta merge | **[NEW]** `commit#Amendment` service that overwrites `ImportLetterOfCredit` master fields with amendment delta. This is a **Phase 1 gap** (not inbound-specific) |
| **Entity: Advised + consent fields** | [ImportLcEntities.xml](file:///Users/me/myprojects/moqui-trade/runtime/component/TradeFinance/entity/ImportLcEntities.xml) — `ImportLcAmendment` has no advised/consent tracking fields | **[ADD]** New fields: `isAdvisedToBeneficiary`, `advisedToBeneficiaryDate`, `consentSwiftRef` |
| **Service: Inbox action** | Does not exist | **[NEW]** `InboundActionServices.processConsent#Mt799` |
| **SECA: Liability release** | Existing earmark logic in TradeCommonServices | **[REUSE]** — triggered by the new `processConsent#Mt799` service |
| **UI: Amendment consent** | Frontend amendment detail panel shows `BENE_PENDING/ACCEPTED/REJECTED` | **[MODIFY]** Show "Delivered to Beneficiary ✓" indicator when `isAdvisedToBeneficiary = 'Y'` |

> **⚠️ CRITICAL DEPENDENCY:** The amendment merge service (`commit#Amendment`) does not exist yet. The inbound MT 799 "Accept" action depends on it. This must be implemented *before* the inbound processing can function for amendments.

---

### 8.3 Process 3.3: Document Presentation & Examination (MT 750 + MT 754)

**Requirements summary:** MT 750 auto-spawns a discrepant `LcPresentation`. MT 754 auto-spawns a clean presentation that skips examination.

| Aspect | Existing Artifact | Required Change |
| :--- | :--- | :--- |
| **Service: create#Presentation** | [TradeCommonServices.xml:33–77](file:///Users/me/myprojects/moqui-trade/runtime/component/TradeFinance/service/trade/TradeCommonServices.xml#L33-L77) — state guard restricts to `['LC_ISSUED', 'LC_AMENDED', 'LC_DOC_RECEIVED']` | **[NO CHANGE]** — LC stays in `LC_ISSUED` when advised, guard works as-is |
| **SECA: validate#Presentation** | [ImportLc.secas.xml:28–30](file:///Users/me/myprojects/moqui-trade/runtime/component/TradeFinance/service/ImportLc.secas.xml#L28-L30) — pre-service calls drawing check | **[MODIFY]** Add `sourceChannel` context flag check. Skip validation when `sourceChannel = 'INBOUND'` for MT 750 (discrepant by definition). See §7A.3.B |
| **Entity: TradeDocumentPresentation** | [ImportLcEntities.xml](file:///Users/me/myprojects/moqui-trade/runtime/component/TradeFinance/entity/ImportLcEntities.xml) — entity exists with `presentationStatusId`, `isDiscrepant`, `claimAmount` fields | **[REUSE]** — MT 750/754 auto-spawn maps directly |
| **Entity: PresentationDiscrepancy** | [ImportLcEntities.xml](file:///Users/me/myprojects/moqui-trade/runtime/component/TradeFinance/entity/ImportLcEntities.xml) — child entity for per-line discrepancies | **[REUSE]** — MT 750 Tag 77J lines map to individual records |
| **Service: Inbox action** | Does not exist | **[NEW]** `InboundActionServices.spawnPresentation#Mt750` and `spawnPresentation#Mt754` |
| **State: LC transition** | [ImportLcEntities.xml:203](file:///Users/me/myprojects/moqui-trade/runtime/component/TradeFinance/entity/ImportLcEntities.xml#L203) — `LC_ISSUED → LC_DOC_RECEIVED` exists | **[NO CHANGE]** — LC stays in `LC_ISSUED`, existing transition works |
| **State: Direct to ACCEPTED** | [ImportLcEntities.xml:207](file:///Users/me/myprojects/moqui-trade/runtime/component/TradeFinance/entity/ImportLcEntities.xml#L207) — `LC_DOC_RECEIVED → LC_ACCEPTED` exists | **[REUSE]** for MT 754 (skips to ACCEPTED) |

> **✅ NO BREAKING CHANGE:** With the flag-based design, the LC stays in `LC_ISSUED` when advised, so the existing `create#Presentation` guard works without modification.

---

### 8.4 Process 3.4: Settlement & Payment (MT 742 + MT 999/799 Chasers)

**Requirements summary:** MT 742 reimbursement claim auto-matches against MT 740 authorization. MT 999/799 chasers drop into Trade Inbox for manual reply.

| Aspect | Existing Artifact | Required Change |
| :--- | :--- | :--- |
| **Entity: NostroReconciliation** | [ImportLcEntities.xml](file:///Users/me/myprojects/moqui-trade/runtime/component/TradeFinance/entity/ImportLcEntities.xml) — exists with `expectedAmount`, `matchStatusEnumId` | **[REUSE]** — MT 742 compares `claimAmount` against `expectedAmount` |
| **Service: Settlement** | Existing settlement workflow in ImportLcServices | **[REUSE]** — MT 742 auto-match queues existing MT 202 generation |
| **Service: Inbox action** | Does not exist | **[NEW]** `InboundActionServices.matchReimbursement#Mt742` |
| **State: Reconciliation** | `RECON_PENDING → RECON_MATCHED` exists | **[REUSE]** |
| **MT 999 Security** | No entity or UI support | **[NEW]** `securityWarningFlag` on `TradeInboxItem` + UI red banner |

---

### 8.5 Process 3.5: Shipping Guarantees

**Requirements summary:** *"Almost ZERO"* SWIFT impact. Rare MT 999 with courier tracking number.

| Aspect | Existing Artifact | Required Change |
| :--- | :--- | :--- |
| **SWIFT processing** | N/A | **[NONE]** — MT 999 drops into Trade Inbox as informational only |
| **Entity: ShippingGuarantee** | [ImportLcEntities.xml](file:///Users/me/myprojects/moqui-trade/runtime/component/TradeFinance/entity/ImportLcEntities.xml) — `ImportLcShippingGuarantee` exists | **[NO CHANGE]** |

---

### 8.6 Process 3.6: Cancellations, Expiry, Limit Reversal (MT 799 Consent to Cancel)

**Requirements summary:** MT 799 stating "Beneficiary agrees to cancellation." Operator clicks "Process Cancellation" → LC moves to `Cancelled - Closed`, limits released.

| Aspect | Existing Artifact | Required Change |
| :--- | :--- | :--- |
| **Service: create#Cancellation** | [TradeCommonServices.xml:263–284](file:///Users/me/myprojects/moqui-trade/runtime/component/TradeFinance/service/trade/TradeCommonServices.xml#L263-L284) — creates `IMP_CANCELLATION` transaction | **[REUSE]** — MT 799 cancellation consent triggers existing cancellation flow |
| **State: LC_CANCELLED** | [ImportLcEntities.xml:195](file:///Users/me/myprojects/moqui-trade/runtime/component/TradeFinance/entity/ImportLcEntities.xml#L195) — exists, with transitions from `LC_ISSUED` and `LC_DRAFT` | **[NO CHANGE]** — LC stays in `LC_ISSUED`, existing transition works |
| **Service: Inbox action** | Does not exist | **[NEW]** `InboundActionServices.processCancellationConsent#Mt799` |
| **Limit release** | Existing earmark release logic | **[REUSE]** — triggered by Checker authorization of cancellation transaction |

---

### 8.7 Summary: Critical Modification Hotspots

These are the existing files that require code changes (not just new files):

| File | Line(s) | Change | Priority |
| :--- | :--- | :--- | :--- |
| [ImportLc.secas.xml](file:///Users/me/myprojects/moqui-trade/runtime/component/TradeFinance/service/ImportLc.secas.xml) | 28–30 | Add `sourceChannel` bypass for inbound MT 750 presentation validation | **P0 — BLOCKING** |
| [ImportLcEntities.xml](file:///Users/me/myprojects/moqui-trade/runtime/component/TradeFinance/entity/ImportLcEntities.xml) | LC entity | Add `isAdvised`, `advisedDate`, `advisingBankReference` fields | **P1** |
| [ImportLcEntities.xml](file:///Users/me/myprojects/moqui-trade/runtime/component/TradeFinance/entity/ImportLcEntities.xml) | Amendment entity | Add `isAdvisedToBeneficiary`, `advisedToBeneficiaryDate`, `consentSwiftRef` fields | **P1** |
| Frontend | LC detail + amendment panel | Show "Advised ✓" / "Delivered to Beneficiary ✓" indicators | **P2** |

---

## 9. Technical Dependencies

### 9.1 Prowide Core Library

Already present in the project. The inbound processing uses the same parsing classes as outbound generation but in reverse:

*   **Outbound (existing):** Build `SwiftMessage` → `msg.message()` → Store as string.
*   **Inbound (new):** `SwiftMessage.parse(rawString)` → Extract fields → Create entities.

### 9.2 Moqui Service Jobs

The directory polling service is implemented as a Moqui Scheduled Service Job:

```xml
<moqui.service.job.ServiceJob jobName="PollInboundSwift"
    serviceName="trade.swift.InboundSwiftServices.poll#InboundDirectory"
    cronExpression="0 */5 * * * ?"
    description="Polls the inbound SWIFT directory for new .txt files"/>
```

### 9.3 Future Gateway Integration Point

When SAA connectivity is acquired, the ingestion architecture changes at exactly one point:

| Phase | Ingestion Source | Entry Point |
| :--- | :--- | :--- |
| Phase 1 (this BRD) | File system polling + manual upload | `InboundSwiftServices.poll#InboundDirectory` and `upload#SwiftFile` |
| Phase 2 (future) | JMS/MQ queue listener or Apache Camel route | A new `InboundSwiftServices.receive#FromQueue` service that calls the same `ingest#SwiftMessage` |

The `ingest#SwiftMessage` service, the correlation engine, the Trade Inbox, and all operator action services remain **completely unchanged**. Only the channel adapter is swapped.

---

## 10. Summary of New Artifacts

### Entities (New)
*   `InboundSwiftRaw` — immutable audit record of raw SWIFT text
*   `TradeInboxItem` — parsed message with correlation and action tracking

### Entities (Modified)
*   `SwiftMessage` — add `messageDirection` field
*   `ImportLetterOfCredit` seed data — add `LC_ADVISED` status + flow transition
*   `ImportLcAmendment` seed data — add `BENE_ADVISED` consent status

### Services (New)
*   `InboundSwiftServices.xml` — `ingest#SwiftMessage`, `poll#InboundDirectory`, `upload#SwiftFile`
*   `InboundCorrelationServices.xml` — `correlate#SwiftMessage`, `link#OrphanMessage`
*   `InboundActionServices.xml` — `acknowledge#Mt730`, `processConsent#Mt799`, `spawnPresentation#Mt750`, `spawnPresentation#Mt754`, `matchReimbursement#Mt742`

### SECAs (New)
*   `InboundSwift.secas.xml` — post-ingestion triggers for auto-spawning presentations and reimbursement matching

### Screens (New)
*   `TradeInbox.xml` — operator inbox list
*   `TradeInboxDetail.xml` — message detail with action buttons
*   `SwiftManualUpload.xml` — file upload widget
*   `OrphanQueue.xml` — manual linking for unmatched messages

### Configuration (TradeConfig)
*   `SWIFT_INBOUND_DIR`, `SWIFT_ARCHIVE_DIR`, `SWIFT_ERROR_DIR`, `SWIFT_POLL_CRON`, `SWIFT_FILE_STABLE_SECONDS`

