# Consolidated BDD: Inbound SWIFT Processing Engine
**ABOUTME:** BDD scenarios for the Inbound SWIFT Correlation Engine and Trade Inbox.
**Source BRD:** `2026-05-15-inbound-swift-processing-brd.md`

**Project Name:** Digital Trade Finance Platform
**Module:** Inbound SWIFT Processing (Import LC)
**Date:** May 15, 2026

---

## 1. Traceability Matrix

| Feature (BRD) | Scenario ID | Title | Type | Source Rule/Req | User Story |
|---|---|---|---|---|---|
| **2. Ingestion** | BDD-INB-ING-01 | Directory polling reads and archives files | Happy | ING-01..04 | US-SEC-02 |
| **2. Ingestion** | BDD-INB-ING-02 | Manual upload with SHA-256 audit stamp | Happy | SEC-03 | US-SEC-02 |
| **2. Ingestion** | BDD-INB-ING-03 | Duplicate detected by content hash | Edge | DDP-01 | US-SEC-01 |
| **2. Ingestion** | BDD-INB-ING-04 | Duplicate detected by SWIFT header composite key | Edge | DDP-01 | US-SEC-01 |
| **2. Ingestion** | BDD-INB-ING-05 | Corrupt file routed to error archive | Edge | ING-05 | US-SEC-02 |
| **2. Ingestion** | BDD-INB-ING-06 | Batch file split into individual messages | Happy | ING-06 | US-SEC-02 |
| **3. Correlation** | BDD-INB-COR-01 | Tag 21 auto-match to LC | Happy | COR-01..02 | US-INB-01 |
| **3. Correlation** | BDD-INB-COR-02 | Orphan queue on missing Tag 21 | Edge | COR-03 | US-INB-01 |
| **3. Correlation** | BDD-INB-COR-03 | Manual orphan linking | Happy | COR-04 | US-INB-01 |
| **3. Correlation** | BDD-INB-COR-04 | Sub-correlation: MT 730 matched to amendment | Happy | COR-05 | US-MT730-01 |
| **4. Trade Inbox** | BDD-INB-TIX-01 | Inbox shows unread messages with badge count | Happy | §4.2 | US-INB-01, US-INB-02 |
| **4. Trade Inbox** | BDD-INB-TIX-02 | MT 999 renders security warning banner | Edge | SEC-04..06 | US-INB-03 |
| **5.1 MT 730** | BDD-INB-730-01 | Acknowledge issuance sets advised flag | Happy | MT730-01 | US-MT730-01 |
| **5.1 MT 730** | BDD-INB-730-02 | Acknowledge amendment sets advised flag | Happy | MT730-02 | US-MT730-01 |
| **5.2 MT 799** | BDD-INB-799-01 | Accept amendment merges delta into master LC | Happy | MT799-01..02 | US-MT799-01 |
| **5.2 MT 799** | BDD-INB-799-02 | Reject amendment releases earmarked limits | Happy | MT799-03 | US-MT799-01 |
| **5.2 MT 799** | BDD-INB-799-03 | MT 799 narrative never auto-evaluated | Edge | MT799-01 | US-MT799-01 |
| **5.3 MT 750** | BDD-INB-750-01 | Auto-spawn discrepant presentation | Happy | MT750-01..03 | US-MT750-01 |
| **5.3 MT 750** | BDD-INB-750-02 | Inbound presentation bypasses validation SECA | Edge | §7A.3.B | US-MT750-01 |
| **5.4 MT 754** | BDD-INB-754-01 | Clean presentation skips examination | Happy | MT754-01..02 | US-MT754-01 |
| **5.5 MT 742** | BDD-INB-742-01 | Auto-match reimbursement within limit | Happy | MT742-01..02 | US-MT742-01 |
| **5.5 MT 742** | BDD-INB-742-02 | Over-limit claim routed to exception queue | Edge | MT742-03 | US-MT742-01 |
| **6. Discrepancy** | BDD-INB-DSC-01 | Waiver with mandatory upload and Checker auth | Happy | DISC-01..03 | US-DISC-01 |
| **6. Discrepancy** | BDD-INB-DSC-02 | Refusal generates MT 734 | Happy | DISC-01..03 | US-DISC-02 |
| **6. Discrepancy** | BDD-INB-DSC-03 | Submit blocked without uploaded evidence | Edge | DISC-01 | US-DISC-01 |
| **6. Discrepancy** | BDD-INB-DSC-04 | UCP 600 5-day deadline alert | Edge | DISC-04..06 | US-DISC-03 |

---

## 2. Detailed BDD Scenarios

### Feature 2: Ingestion & Deduplication

#### Scenario BDD-INB-ING-01: Directory polling reads and archives files
**US-SEC-02 | ING-01..04**
*Type: Happy Path*

* **Given** a SWIFT `.txt` file named `MT730_20260515.txt` exists in the configured `/inbound_swift/` directory
* **And** the file size has been stable for at least 2 seconds (write-complete check)
* **When** the scheduled `poll#InboundDirectory` service executes
* **Then** the system performs the Read & Move protocol:
  | Step | Result |
  | File read to memory | Content loaded |
  | File moved | From `/inbound_swift/` to `/archive_swift/processed/` |
  | `InboundSwiftRaw` created | `contentHash` = SHA-256 of content, `sourceChannel` = `SRC_DIRECTORY_POLL` |
  | `parseStatusEnumId` | `PARSE_SUCCESS` |
* **And** the original file no longer exists in the `/inbound_swift/` directory

#### Scenario BDD-INB-ING-02: Manual upload with SHA-256 audit stamp
**US-SEC-02 | SEC-03**
*Type: Happy Path*

* **Given** a Trade Operations Maker with `TRADE_OPS_MAKER` role
* **When** the Maker uploads a file `MT799_consent.txt` via the `SwiftManualUpload` screen
* **Then** the system creates an `InboundSwiftRaw` record:
  | Field | Value |
  | `contentHash` | SHA-256 of the uploaded content |
  | `sourceChannel` | `SRC_MANUAL_UPLOAD` |
  | `sourceFileName` | `MT799_consent.txt` |
  | `uploadedByUserId` | Current user ID |
* **And** the file content is passed to the same parsing pipeline as the directory poller

#### Scenario BDD-INB-ING-03: Duplicate detected by content hash
**US-SEC-01 | DDP-01**
*Type: Edge Case*

* **Given** an `InboundSwiftRaw` record exists with `contentHash = 'abc123def456'`
* **When** a Maker manually uploads a file with identical content (same SHA-256 hash)
* **Then** the system rejects the upload with message: "This message has already been processed on [date]. Inbox Item: [link]."
* **And** no new `InboundSwiftRaw` record is created
* **And** no new `TradeInboxItem` record is created

#### Scenario BDD-INB-ING-04: Duplicate detected by SWIFT header composite key
**US-SEC-01 | DDP-01**
*Type: Edge Case*

* **Given** an `InboundSwiftRaw` record exists with `swiftHeaderKey = 'BKCHCNBJ|730|REF001|20260515'`
* **When** the directory poller reads a file with a different byte sequence but the same SWIFT header composite key
* **Then** the system detects the duplicate via `swiftHeaderKey` match
* **And** the file is moved to `/archive_swift/duplicate/`
* **And** no downstream processing occurs

#### Scenario BDD-INB-ING-05: Corrupt file routed to error archive
**US-SEC-02 | ING-05**
*Type: Edge Case*

* **Given** a file `corrupt_message.txt` in the `/inbound_swift/` directory containing invalid SWIFT formatting
* **When** `poll#InboundDirectory` attempts to parse via Prowide Core
* **Then** `InboundSwiftRaw` is created with `parseStatusEnumId = 'PARSE_FAILED'` and `parseErrorText` populated
* **And** the file is moved to `/archive_swift/error/`
* **And** a system notification is sent to the IT Admin
* **And** no `TradeInboxItem` record is created

#### Scenario BDD-INB-ING-06: Batch file split into individual messages
**US-SEC-02 | ING-06**
*Type: Happy Path*

* **Given** a batch file containing 3 concatenated SWIFT messages (MT 730, MT 799, MT 750) delimited by `{1:` block starts
* **When** `poll#InboundDirectory` processes the file
* **Then** 3 separate `InboundSwiftRaw` records are created, each with its own `contentHash`
* **And** 3 separate `TradeInboxItem` records are created
* **And** the single batch file is moved to `/archive_swift/processed/`

---

### Feature 3: Tag 21 Correlation Engine

#### Scenario BDD-INB-COR-01: Tag 21 auto-match to LC
**US-INB-01 | COR-01..02**
*Type: Happy Path*

* **Given** a `TradeInstrument` exists with `instrumentRef = 'TF-IMP-26-0001'`
* **And** a parsed inbound MT 730 has Tag 21 = `TF-IMP-26-0001`
* **When** `correlate#SwiftMessage` executes
* **Then** a `TradeInboxItem` is created:
  | Field | Value |
  | `instrumentId` | The matched `TradeInstrument.instrumentId` |
  | `relatedReference` | `TF-IMP-26-0001` |
  | `correlationStatusEnumId` | `CORR_AUTO_MATCH` |
  | `inboxStatusEnumId` | `INBOX_UNREAD` |

#### Scenario BDD-INB-COR-02: Orphan queue on missing Tag 21
**US-INB-01 | COR-03**
*Type: Edge Case*

* **Given** a parsed inbound MT 799 has Tag 21 = `UNKNOWN-REF-123`
* **And** no `TradeInstrument` exists with `instrumentRef = 'UNKNOWN-REF-123'`
* **When** `correlate#SwiftMessage` executes
* **Then** a `TradeInboxItem` is created:
  | Field | Value |
  | `instrumentId` | null |
  | `correlationStatusEnumId` | `CORR_ORPHANED` |
  | `inboxStatusEnumId` | `INBOX_ORPHANED` |
  | `orphanReason` | `NO_MATCH_FOUND` |
* **And** the message appears in the Orphan Queue UI

#### Scenario BDD-INB-COR-03: Manual orphan linking
**US-INB-01 | COR-04**
*Type: Happy Path*

* **Given** a `TradeInboxItem` with `correlationStatusEnumId = 'CORR_ORPHANED'` and `instrumentId = null`
* **When** an operator searches for LC `TF-IMP-26-0001` in the Orphan Queue and clicks "Link to LC"
* **Then** `TradeInboxItem.instrumentId` is updated to the matched instrument
* **And** `correlationStatusEnumId` changes to `CORR_MANUAL_MATCH`
* **And** `inboxStatusEnumId` changes to `INBOX_UNREAD`

#### Scenario BDD-INB-COR-04: Sub-correlation: MT 730 matched to amendment
**US-MT730-01 | COR-05**
*Type: Happy Path*

* **Given** an Import LC `TF-IMP-26-0001` with `issueDate = 2026-04-01`
* **And** an `ImportLcAmendment` linked to this LC with `amendmentDate = 2026-05-10`
* **And** an inbound MT 730 with Tag 21 = `TF-IMP-26-0001` and Tag 30 = `260510`
* **When** sub-correlation executes
* **Then** `TradeInboxItem.amendmentId` is populated with the matching amendment
* **Because** Tag 30 matches the amendment date, not the issuance date

---

### Feature 4: Trade Inbox

#### Scenario BDD-INB-TIX-01: Inbox shows unread messages with badge count
**US-INB-01, US-INB-02 | §4.2**
*Type: Happy Path*

* **Given** 3 `TradeInboxItem` records with `inboxStatusEnumId = 'INBOX_UNREAD'`
* **And** 2 `TradeInboxItem` records with `inboxStatusEnumId = 'INBOX_PROCESSED'`
* **When** the operator opens the Trade Inbox dashboard
* **Then** the default filter shows the 3 unread items
* **And** the navigation badge displays "3"
* **And** records are sorted by `receivedTimestamp` descending

#### Scenario BDD-INB-TIX-02: MT 999 renders security warning banner
**US-INB-03 | SEC-04..06**
*Type: Edge Case*

* **Given** a `TradeInboxItem` with `messageType = '999'` and `securityWarningFlag = 'Y'`
* **When** the operator opens this inbox item
* **Then** a red security banner is rendered: "⚠️ SECURITY WARNING: This message lacks SWIFT cryptographic authentication. Do NOT execute financial actions based on this message."
* **And** no Accept/Reject/Waive action buttons are available — only "Acknowledge" (informational)

---

### Feature 5.1: Inbound MT 730 (Acknowledgment)

#### Scenario BDD-INB-730-01: Acknowledge issuance sets advised flag
**US-MT730-01 | MT730-01**
*Type: Happy Path*

* **Given** an Import LC with `instrumentId = 'LC001'` and `businessStateId = 'LC_ISSUED'` and `isAdvised = 'N'`
* **And** a `TradeInboxItem` of `messageType = '730'` linked to this LC with `amendmentId = null`
* **When** the operator clicks "Acknowledge Receipt"
* **Then** the system updates:
  | Entity | Field | Value |
  | `ImportLetterOfCredit` | `isAdvised` | `Y` |
  | `ImportLetterOfCredit` | `advisedDate` | Tag 30 date |
  | `ImportLetterOfCredit` | `advisingBankReference` | Tag 20 value |
  | `ImportLetterOfCredit` | `businessStateId` | `LC_ISSUED` (unchanged) |
  | `TradeInboxItem` | `inboxStatusEnumId` | `INBOX_PROCESSED` |
  | `TradeInboxItem` | `actionTaken` | `ACKNOWLEDGE` |

#### Scenario BDD-INB-730-02: Acknowledge amendment sets advised flag
**US-MT730-01 | MT730-02**
*Type: Happy Path*

* **Given** an Import LC with an `ImportLcAmendment` (amendmentId = 'AMD001') in `beneficiaryConsentStatusId = 'BENE_PENDING'` and `isAdvisedToBeneficiary = 'N'`
* **And** a `TradeInboxItem` of `messageType = '730'` linked to this amendment
* **When** the operator clicks "Acknowledge Receipt"
* **Then** the system updates:
  | Entity | Field | Value |
  | `ImportLcAmendment` | `isAdvisedToBeneficiary` | `Y` |
  | `ImportLcAmendment` | `advisedToBeneficiaryDate` | Tag 30 date |
  | `ImportLcAmendment` | `beneficiaryConsentStatusId` | `BENE_PENDING` (unchanged) |
  | `TradeInboxItem` | `inboxStatusEnumId` | `INBOX_PROCESSED` |

---

### Feature 5.2: Inbound MT 799 (Amendment Consent)

#### Scenario BDD-INB-799-01: Accept amendment merges delta into master LC
**US-MT799-01 | MT799-01..02**
*Type: Happy Path*

* **Given** an Import LC with `effectiveAmount = 100,000` and `effectiveOutstandingAmount = 100,000`
* **And** an `ImportLcAmendment` with `amountAdjustment = +25,000` and `beneficiaryConsentStatusId = 'BENE_PENDING'`
* **And** a `TradeInboxItem` of `messageType = '799'` with Tag 79 narrative containing "We accept the amendment"
* **When** the operator reads the narrative and clicks "Accept Amendment"
* **Then** the system executes:
  | Entity | Field | Value |
  | `ImportLcAmendment` | `beneficiaryConsentStatusId` | `BENE_ACCEPTED` |
  | `ImportLcAmendment` | `amendmentBusinessStateId` | `AMEND_COMMITTED` |
  | `ImportLcAmendment` | `consentSwiftRef` | Tag 20 from MT 799 |
  | `ImportLetterOfCredit` | `effectiveAmount` | 125,000 (merged) |
  | `ImportLetterOfCredit` | `effectiveOutstandingAmount` | 125,000 (merged) |
  | `TradeInboxItem` | `inboxStatusEnumId` | `INBOX_PROCESSED` |
  | `TradeInboxItem` | `actionTaken` | `ACCEPT_AMENDMENT` |

#### Scenario BDD-INB-799-02: Reject amendment releases earmarked limits
**US-MT799-01 | MT799-03**
*Type: Happy Path*

* **Given** an Import LC with `effectiveAmount = 100,000`
* **And** an `ImportLcAmendment` with `amountAdjustment = +25,000` (extra 25K was earmarked on dispatch)
* **And** a `TradeInboxItem` of `messageType = '799'` with narrative "Beneficiary rejects the amendment"
* **When** the operator clicks "Reject Amendment"
* **Then** the system executes:
  | Entity | Field | Value |
  | `ImportLcAmendment` | `beneficiaryConsentStatusId` | `BENE_REJECTED` |
  | `ImportLcAmendment` | `amendmentBusinessStateId` | `AMEND_REJECTED` |
  | `ImportLetterOfCredit` | `effectiveAmount` | 100,000 (unchanged) |
  | `TradeInboxItem` | `actionTaken` | `REJECT_AMENDMENT` |
* **And** the earmarked 25,000 is released back to the customer facility

#### Scenario BDD-INB-799-03: MT 799 narrative never auto-evaluated
**US-MT799-01 | MT799-01**
*Type: Edge Case*

* **Given** a `TradeInboxItem` of `messageType = '799'` with Tag 79 = "Beneficiary does NOT accept the amendment"
* **When** the system processes this inbox item
* **Then** the system does NOT auto-set `beneficiaryConsentStatusId`
* **And** the item remains in `INBOX_UNREAD` status until a human operator manually clicks Accept or Reject
* **Because** free-text narratives are ambiguous and must never be machine-evaluated for consent decisions

---

### Feature 5.3: Inbound MT 750 (Advice of Discrepancy)

#### Scenario BDD-INB-750-01: Auto-spawn discrepant presentation
**US-MT750-01 | MT750-01..03**
*Type: Happy Path*

* **Given** an Import LC with `instrumentId = 'LC001'` and `businessStateId = 'LC_ISSUED'`
* **And** an inbound MT 750 with Tag 21 = matching LC ref, Tag 32B = `USD 45,000.00`, Tag 77J containing 2 discrepancy lines
* **When** the MT 750 is correlated and auto-spawn executes
* **Then** a `TradeDocumentPresentation` is created:
  | Field | Value |
  | `instrumentId` | `LC001` |
  | `presentationStatusId` | `PRES_RECEIVED` |
  | `isDiscrepant` | `Y` |
  | `applicantDecisionEnumId` | `PENDING` |
  | `claimAmount` | 45,000 |
  | `presentingBankRef` | Tag 20 value |
* **And** 2 `PresentationDiscrepancy` records are created from Tag 77J
* **And** `ImportLetterOfCredit.businessStateId` transitions to `LC_DOC_RECEIVED`
* **And** the UCP 600 5-banking-day examination countdown starts

#### Scenario BDD-INB-750-02: Inbound presentation bypasses validation SECA
**US-MT750-01 | §7A.3.B**
*Type: Edge Case*

* **Given** an Import LC with `effectiveAmount = 50,000` and `effectiveTolerancePositive = 0.10` (max drawable = 55,000)
* **And** an inbound MT 750 with Tag 32B = `USD 60,000.00` (exceeds tolerance)
* **When** `spawnPresentation#Mt750` executes with `sourceChannel = 'INBOUND'`
* **Then** the presentation is created successfully despite the tolerance breach
* **And** the `validate#Presentation` SECA is bypassed because `sourceChannel = 'INBOUND'`
* **And** `TradeDocumentPresentation.isDiscrepant = 'Y'`
* **Because** inbound MT 750 presentations are discrepant by definition — the Applicant decides to waive or refuse

---

### Feature 5.4: Inbound MT 754 (Clean Presentation)

#### Scenario BDD-INB-754-01: Clean presentation skips examination
**US-MT754-01 | MT754-01..02**
*Type: Happy Path*

* **Given** an Import LC with `businessStateId = 'LC_ISSUED'`
* **And** an inbound MT 754 with Tag 21 = matching LC ref, Tag 32A = `USD 50,000.00`
* **When** `spawnPresentation#Mt754` executes
* **Then** a `TradeDocumentPresentation` is created:
  | Field | Value |
  | `presentationStatusId` | `PRES_COMPLIANT` |
  | `isDiscrepant` | `N` |
  | `claimAmount` | 50,000 |
* **And** `ImportLetterOfCredit.businessStateId` transitions to `LC_ACCEPTED`
* **And** the presentation appears directly in the "Ready for Settlement" queue, skipping examination

---

### Feature 5.5: Inbound MT 742 (Reimbursement Claim)

#### Scenario BDD-INB-742-01: Auto-match reimbursement within limit
**US-MT742-01 | MT742-01..02**
*Type: Happy Path*

* **Given** a `NostroReconciliation` record with `expectedAmount = 50,000` and `matchStatusEnumId = 'RECON_PENDING'`
* **And** an inbound MT 742 with Tag 32B = `USD 48,000.00` (≤ expected)
* **When** `matchReimbursement#Mt742` executes
* **Then** `NostroReconciliation.matchStatusEnumId` transitions to `RECON_MATCHED`
* **And** an outbound MT 202 payment is queued for Maker/Checker authorization

#### Scenario BDD-INB-742-02: Over-limit claim routed to exception queue
**US-MT742-01 | MT742-03**
*Type: Edge Case*

* **Given** a `NostroReconciliation` record with `expectedAmount = 50,000`
* **And** an inbound MT 742 with Tag 32B = `USD 55,000.00` (exceeds authorization)
* **When** `matchReimbursement#Mt742` executes
* **Then** the claim is NOT auto-matched
* **And** an exception record is created with reason "CLAIM_EXCEEDS_AUTHORIZATION"
* **And** the operator is alerted to investigate

---

### Feature 6: Discrepancy Resolution (Maker/Checker)

#### Scenario BDD-INB-DSC-01: Waiver with mandatory upload and Checker auth
**US-DISC-01 | DISC-01..03**
*Type: Happy Path*

* **Given** a `TradeDocumentPresentation` with `isDiscrepant = 'Y'` and `applicantDecisionEnumId = 'PENDING'`
* **And** 2 `PresentationDiscrepancy` records linked to it
* **When** the Maker:
  1. Marks each discrepancy as `isWaived = Y`
  2. Uploads an Applicant waiver PDF
  3. Clicks "Submit for Authorization"
* **Then** `applicantDecisionEnumId` → `WAIVED` and `presentationStatusId` → `PRES_EXAMINING`
* **When** the Checker reviews and clicks "Authorize"
* **Then** the system executes:
  | Action | Result |
  | `presentationStatusId` | `PRES_COMPLIANT` |
  | `businessStateId` | `LC_ACCEPTED` |
  | Outbound MT 752 | Generated (Discrepancy Waiver Advice) |

#### Scenario BDD-INB-DSC-02: Refusal generates MT 734
**US-DISC-02 | DISC-01..03**
*Type: Happy Path*

* **Given** a `TradeDocumentPresentation` with `isDiscrepant = 'Y'` and `applicantDecisionEnumId = 'PENDING'`
* **When** the Maker:
  1. Selects document disposal = `RETURNING_DOCUMENTS`
  2. Uploads the Applicant's refusal instruction
  3. Clicks "Submit for Authorization"
* **Then** `applicantDecisionEnumId` → `REFUSED` and `presentationStatusId` → `PRES_EXAMINING`
* **When** the Checker authorizes
* **Then** the system executes:
  | Action | Result |
  | `presentationStatusId` | `PRES_REJECTED` |
  | Outbound MT 734 | Generated with Tag 77B = `RETURNING_DOCUMENTS` |
  | LC balance | Unchanged (no funds deducted) |

#### Scenario BDD-INB-DSC-03: Submit blocked without uploaded evidence
**US-DISC-01 | DISC-01**
*Type: Edge Case*

* **Given** a `TradeDocumentPresentation` with `isDiscrepant = 'Y'`
* **When** the Maker marks discrepancies as `isWaived = Y` but does NOT upload an Applicant waiver document
* **Then** the "Submit for Authorization" button remains disabled
* **And** the system displays: "Applicant waiver/refusal document is mandatory before submission."

#### Scenario BDD-INB-DSC-04: UCP 600 5-banking-day deadline alert
**US-DISC-03 | DISC-04..06**
*Type: Edge Case*

* **Given** a `TradeDocumentPresentation` created on Monday 2026-05-11 with `applicantDecisionEnumId = 'PENDING'`
* **And** `TradeProductCatalog.documentExamSlaDays = 5`
* **When** the daily deadline monitoring job runs on Thursday 2026-05-14 (day 3 of 5)
* **Then** the presentation is flagged with an amber warning: "2 banking days remaining"
* **When** the job runs on Monday 2026-05-18 (day 5 exceeded)
* **Then** the presentation is flagged with a red alert: "UCP 600 deadline exceeded — implicit acceptance may apply"
* **And** a compliance alert is generated for the Operations Manager
