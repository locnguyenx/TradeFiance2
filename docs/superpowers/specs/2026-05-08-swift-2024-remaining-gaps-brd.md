# BRD Supplement: Remaining SWIFT SRG 2024 Gaps
**ABOUTME:**
Supplement BRD for SWIFT SRG 2024 gaps not covered by the Amendment BRD.
Covers MT 700 enhancements, MT 740/747 reimbursement, Nostro reconciliation, Tag 77J, Tag 23S, MT 730 reference.

**Project Name:** Digital Trade Finance Platform
**Module:** Import Letter of Credit (LC)
**Document Version:** 1.0
**Date:** May 08, 2026

**Parent BRDs (not superseded — supplemented):**
*   [2026-05-05-import-lc-consolidated-brd.md](file:///Users/me/myprojects/moqui-trade/docs/superpowers/specs/2026-05-05-import-lc-consolidated-brd.md)
*   [2026-05-05-common-consolidated-brd.md](file:///Users/me/myprojects/moqui-trade/docs/superpowers/specs/2026-05-05-common-consolidated-brd.md)

**Related (already implemented — not in scope):**
*   [2026-05-08-import-lc-amendment-srg2024-brd.md](file:///Users/me/myprojects/moqui-trade/docs/superpowers/specs/2026-05-08-import-lc-amendment-srg2024-brd.md) — MT 707 SRG 2024 amendments

**Source Requirements:**
*   [20260504-MT_all_SWIFT_ver2024.md](file:///Users/me/myprojects/moqui-trade/docs/requirements/20260504-MT_all_SWIFT_ver2024.md) — SWIFT SRG 2024 tag lists, gap analysis, and URR 725 reimbursement process

---

## 1. Feature 1: MT 700 Issuance Enhancements (Tags 49G, 49H, 40E)

### 1.1 Revision Scope
Three SWIFT tags introduced in SRG 2018 are missing from the current Issuance data dictionary. Tags 49G/49H were added to give structured Payment Conditions fields (previously banks crammed these into Tag 47A or 78). Tag 40E specifies Applicable Rules (e.g., `UCP LATEST VERSION`).

### 1.2 New Entity Fields on `ImportLetterOfCredit`

| Field Name | Req/Opt | Data Type | SWIFT Tag | Validation Rules |
| :--- | :--- | :--- | :--- | :--- |
| `paymentCondBeneText` | Opt | text-long | Tag 49G | Max 4×35, X charset. Beneficiary-specific payment instructions. |
| `paymentCondBankText` | Opt | text-long | Tag 49H | Max 4×35, X charset. Receiving Bank-specific payment instructions. |
| `applicableRulesEnumId` | Opt | id | Tag 40E | `UCP_LATEST`, `EUCP_LATEST`, `UCPDC_600`, `ISP_LATEST`. |

### 1.3 SWIFT Generation Impact
- MT 700: Tags 49G and 49H appended after Tag 47A in Text Block 4 if populated.
- MT 700: Tag 40E appended in the Contract Base block if populated.
- MT 707: Tags 49M/49N (amendment B-tags for 49G/49H) already covered by amendment BRD.

### 1.4 SWIFT Validation Rules

| Rule ID | Rule | Tags Affected |
| :--- | :--- | :--- |
| ISS-SWV-18 | `paymentCondBeneText`: X charset, max 4×35 lines | Tag 49G |
| ISS-SWV-19 | `paymentCondBankText`: X charset, max 4×35 lines | Tag 49H |
| ISS-SWV-20 | `applicableRulesEnumId` must map to valid SWIFT code | Tag 40E |

### 1.5 User Stories

#### US-ISS-04: Payment Condition Fields
**As a** Trade Operations Maker,
**I want** dedicated input fields for Beneficiary and Bank payment conditions,
**So that** these instructions are transmitted in the correct SWIFT tags instead of being buried in Additional Conditions or Bank-to-Bank Instructions.

### 1.6 Grounding Info
*   **Tags 49G/49H:** From SRG 2024 §1 Gap Analysis — MT 700 tags added post-2018, previously crammed into 47A/78.
*   **Tag 40E:** Present in amendment BRD §7.2 (MT 707) but missing from issuance data dictionary.

---

## 2. Feature 2: Reimbursement Authorization & Nostro Reconciliation (MT 740, MT 747, URR 725)

### 2.1 MT 740 Auto-Generation (Alongside Issuance)

**Trigger:** When Checker authorizes LC issuance AND a `TP_REIMBURSING_BANK` party role is assigned to the instrument, the system generates MT 740 alongside MT 700.

#### A. New Entity Fields on `ImportLetterOfCredit`

| Field Name | Req/Opt | Data Type | SWIFT Tag | Validation Rules |
| :--- | :--- | :--- | :--- | :--- |
| `authExpiryDate` | Opt | Date | Tag 31D (MT740) | Usually LC Expiry Date + grace period (e.g., 30 days). Formatted YYMMDD. |
| `reimbursingChargesEnumId` | Cond | id | Tag 71D (MT740) | `REIMB_OUR`, `REIMB_BEN`. **Required if `TP_REIMBURSING_BANK` assigned.** |
| `applicableReimbRulesText` | Opt | text-medium | Tag 40F (MT740) | Default: `URR LATEST VERSION`. Max 4×35, X charset. |

#### B. MT 740 Exhaustive Tag Dictionary (SRG 2024)

Merging the SRG 2024 spec (§2) with the URR 725 section. The 2024 version takes precedence.

| Tag | Field Name | Status | System Source / Mapping |
| :--- | :--- | :--- | :--- |
| **20** | Documentary Credit No. | **M** | `TradeInstrument.transactionRef`. Must match MT 700 Tag 20 exactly. |
| **21** | Receiver's Reference | O | Reimbursing Bank's ref (if known). Default `NONREF`. |
| **25** | Account Identification | O | `TradePartyBank.nostroAccountRef` from `TP_REIMBURSING_BANK` party. |
| **31D** | Date and Place of Expiry | **M** | `ImportLetterOfCredit.authExpiryDate`. Expiry of the *reimbursement authorization*, not the LC. |
| **32B** | Credit Amount | **M** | `TradeInstrument.currencyUomId` + `TradeInstrument.amount`. Must match MT 700. |
| **39A** | Percentage Tolerance | O | `ImportLetterOfCredit.tolerancePositive` / `toleranceNegative`. Mutually exclusive with 39B. |
| **39B** | Maximum Credit Amount | O | `ImportLetterOfCredit.maxCreditAmountFlag`. Mutually exclusive with 39A. |
| **40F** | Applicable Rules | O | `ImportLetterOfCredit.applicableReimbRulesText`. Default `URR LATEST VERSION`. |
| **58a** | Negotiating Bank | **M** | `TP_NEGOTIATING_BANK` junction party BIC, or `ANY BANK` if `availableWithEnumId = AVAIL_ANY_BANK`. |
| **59** | Beneficiary | **M** | `TP_BENEFICIARY` junction party. Matches MT 700 Tag 59. |
| **71D** | Charges | O | `ImportLetterOfCredit.reimbursingChargesEnumId` mapped to text. |
| **72Z** | Sender to Receiver Info | O | `ImportLetterOfCredit.senderToReceiverInfo`. Max 6×35, X charset. |

#### C. Validation Rules

| Rule ID | Rule | Tags |
| :--- | :--- | :--- |
| RMB-SWV-01 | MT 740 only generated if `TP_REIMBURSING_BANK` is assigned | Generation gate |
| RMB-SWV-02 | `TP_REIMBURSING_BANK` must have `nostroAccountRef` populated | Tag 25 |
| RMB-SWV-03 | `authExpiryDate` must be ≥ LC `expiryDate` | Tag 31D |
| RMB-SWV-04 | Tag 39A/39B mutual exclusion (inherited from MT 700) | Tags 39A, 39B |
| RMB-SWV-05 | Tag 58a must resolve to valid BIC or `ANY BANK` | Tag 58a |

#### D. URR 725 Business Rules

*   **Independent Undertaking:** The reimbursement authorization is independent of the commercial LC terms. The Reimbursing Bank verifies only that the claim amount matches the authorized amount.
*   **Expiry Tracking:** The system must track `authExpiryDate` separately from LC expiry. Claims arriving after this date will be rejected by the Reimbursing Bank.
*   **Amendment Dependency:** A Reimbursing Bank is not bound by an LC amendment until they receive a specific MT 747.

---

### 2.2 MT 747 Auto-Generation (Alongside Amendment)

**Trigger:** When an External Amendment is authorized AND `TP_REIMBURSING_BANK` is assigned AND the amendment changes amount or expiry.

#### A. New Entity Field on `ImportLcAmendment`

| Field Name | Req/Opt | Data Type | SWIFT Tag | Validation Rules |
| :--- | :--- | :--- | :--- | :--- |
| `newAuthExpiryDate` | Opt | Date | Tag 31E (MT747) | New reimbursement authorization expiry. Formatted YYMMDD. |

#### B. MT 747 Tag Dictionary

| Tag | Field Name | Status | System Source / Mapping |
| :--- | :--- | :--- | :--- |
| **20** | Documentary Credit No. | **M** | `TradeInstrument.transactionRef`. |
| **21** | Reimbursing Bank's Ref | O | Their reference if provided. Default `NONREF`. |
| **30** | Date of Amendment | **M** | `ImportLcAmendment.amendmentDate`. Formatted YYMMDD. |
| **32B** | Increase of Amount | O | `ImportLcAmendment.amountIncrease`. CCY + Amount. Mutually exclusive with 33B. |
| **33B** | Decrease of Amount | O | `ImportLcAmendment.amountDecrease`. CCY + Amount. Mutually exclusive with 32B. |
| **34B** | New Authorized Amount | O | Computed: original + increase - decrease. Required if 32B or 33B present. |
| **31E** | New Expiry Date | O | `ImportLcAmendment.newAuthExpiryDate`. Formatted YYMMDD. |

> **Note:** Tags 34B and 31E were deleted from MT 707 (SRG 2018), but the SRG 2024 requirements doc does not indicate the same deletion for MT 747. These tags are retained for MT 747 pending SWIFT standards verification.

#### C. Generation Rules

| Rule ID | Rule |
| :--- | :--- |
| RMB-SWV-06 | MT 747 generated only if amendment contains `amountIncrease`, `amountDecrease`, OR `newAuthExpiryDate` |
| RMB-SWV-07 | MT 747 generation blocked if no `TP_REIMBURSING_BANK` assigned to parent LC |

---

### 2.3 Manual Nostro Reconciliation

**Scope:** Allow operations to manually mark the Nostro debit (from the Reimbursing Bank honoring a claim) as matched against the LC settlement. Auto-reconciliation via inbound MT 742/MT 900 parsing is deferred to Phase 2 (Inbound SWIFT Processing).

#### A. New Entity: `NostroReconciliation`

| Field Name | Req/Opt | Data Type | Description |
| :--- | :--- | :--- | :--- |
| `reconciliationId` | Req (PK) | id | System-generated. |
| `instrumentId` | Req | id | Parent LC. |
| `reimbursingBankPartyId` | Req | id | The `TP_REIMBURSING_BANK` party. |
| `expectedCurrency` | Req | id | ISO 4217. Inherited from LC. |
| `expectedAmount` | Req | number-decimal | The authorized amount (LC amount + tolerance). |
| `nostroDebitDate` | Opt | Date | Date the Nostro account was debited (entered manually). |
| `nostroDebitAmount` | Opt | number-decimal | Actual debit amount. |
| `nostroStatementRef` | Opt | text-short | Reference from the bank statement. Max 35 chars. |
| `matchStatusEnumId` | Req | id | `RECON_PENDING`, `RECON_MATCHED`, `RECON_UNMATCHED`, `RECON_PARTIAL`. |
| `matchedByUserId` | Opt | id | User who performed matching. |
| `matchedDate` | Opt | Date | When matching was performed. |
| `remarks` | Opt | text-long | Free-text notes. |

#### B. Business Rules

| Rule ID | Rule |
| :--- | :--- |
| RMB-REC-01 | `NostroReconciliation` record auto-created with status `RECON_PENDING` when MT 740 is generated. |
| RMB-REC-02 | Matching requires Maker/Checker approval (Maker enters debit details, Checker confirms match). |
| RMB-REC-03 | If `nostroDebitAmount` ≠ `expectedAmount`, status → `RECON_UNMATCHED`. Requires manual investigation. |
| RMB-REC-04 | LC cannot transition to `Closed` while any linked `NostroReconciliation` is in `RECON_PENDING`. |

### 2.4 User Stories

#### US-RMB-01: Automatic Reimbursement Authorization
**As a** Trade Operations system,
**I want** to auto-generate MT 740 when an LC with a Reimbursing Bank is authorized,
**So that** the Reimbursing Bank is formally authorized to honor claims.

#### US-RMB-02: Reimbursement Amendment Propagation
**As a** Trade Operations system,
**I want** to auto-generate MT 747 when a financial amendment is authorized on an LC with a Reimbursing Bank,
**So that** the Reimbursing Bank's authorized ledger stays in sync.

#### US-RMB-03: Manual Nostro Matching
**As a** Trade Operations Maker,
**I want** to manually log Nostro debit details and mark them as matched against LC settlements,
**So that** the bank can reconcile reimbursement claims until automated inbound processing is built.

### 2.5 Grounding Info
*   **MT 740 Tags:** From SRG 2024 §2 (MT 740 tag list) merged with URR 725 §F (system mapping).
*   **MT 747 Tags:** From URR 725 §F (MT 747 tag list). SRG 2024 does not modify MT 747.
*   **URR 725 Rules:** From URR 725 §E (guidelines to enforce).
*   **Reimbursing Bank Eligibility:** From `FR-TP-12` — `nostroAccountRef` required (common-consolidated-brd.md).

---

## 3. Feature 3: Presentation Validation Tightening (MT 750/734 Tag 77J)

### 3.1 Revision Scope
The SRG 2024 standard enforces strict truncation on Tag 77J (Discrepancies). The SWIFT gateway will aggressively reject any payload where Tag 77J exceeds **70 lines of 50 characters**. The current BRD specifies per-entry limits (35 chars code, 50 chars description) but lacks aggregate line-count enforcement.

### 3.2 Updated Validation Rules

| Rule ID | Rule | Tags Affected | Status |
| :--- | :--- | :--- | :--- |
| PRE-SWV-08 | **Aggregate Tag 77J limit:** Total concatenated discrepancy text must not exceed 70 lines of 50 characters. System must calculate total line count at submission and block if exceeded. | Tag 77J (MT 750, MT 734) | New |
| PRE-SWV-09 | **UI enforcement:** Discrepancy entry form must display a running line-count indicator (e.g., "42/70 lines used") and prevent adding new entries that would breach the 70-line limit. | — | New |
| PRE-SWV-10 | **Generation-time safety net:** `SwiftMessageBuilder` must validate assembled Tag 77J content against 70×50 limit before dispatch. If exceeded, abort and return error. | Tag 77J | New |

### 3.3 Impact on Existing Rules
- PRE-SWV-04 (at least one discrepancy if `discrepancyFound = true`) — unchanged.
- PRE-SWV-06 (X charset, max 35/50 per entry) — unchanged but now supplemented by aggregate limit.

### 3.4 User Stories

#### US-PRE-03: Discrepancy Line Count Enforcement
**As a** Trade Operations Maker,
**I want** the system to show me how many of the 70 allowed discrepancy lines I've used and block me from exceeding the limit,
**So that** the MT 750/734 message is never rejected by the SWIFT network.

### 3.5 Grounding Info
*   **Source:** `20260504-MT_all_SWIFT_ver2024.md` §3 (MT 750) and §4 Gap Analysis (Tag 77J strict truncation).
*   **Applies to:** MT 734 (Notice of Refusal) which uses the same Tag 77J.

---

## 4. Feature 4: MT 707 Cancellation Request (Tag 23S)

### 4.1 Revision Scope
The SRG 2024 MT 707 spec includes Tag 23S (`CANCEL`) for requesting full LC cancellation via amendment message, as an alternative to the MT 799 free-format approach currently implemented.

### 4.2 New Entity Field on `ImportLcAmendment`

| Field Name | Req/Opt | Data Type | SWIFT Tag | Validation Rules |
| :--- | :--- | :--- | :--- | :--- |
| `isCancellationRequest` | Opt | text-indicator | Tag 23S | Y/N. If `Y`, Tag 23S is populated with `CANCEL`. |

### 4.3 Business Rules

| Rule ID | Rule |
| :--- | :--- |
| CAN-SWV-04 | If `isCancellationRequest = Y`, the MT 707 must include Tag 23S with value `CANCEL`. |
| CAN-SWV-05 | If `isCancellationRequest = Y`, no other amendable fields should be changed. System blocks mixed cancellation + amendment. |
| CAN-SWV-06 | Cancellation-via-amendment follows the same Beneficiary Consent workflow as standard amendments (UCP 600 Article 10). |

### 4.4 Relationship to Existing MT 799 Cancellation
- MT 799 (free-format cancellation request) remains available for early cancellation negotiations.
- Tag 23S on MT 707 is the formal, structured alternative once both parties agree.
- Operations chooses the appropriate method based on the stage of negotiation.

### 4.5 User Stories

#### US-CAN-03: Structured Cancellation via Amendment
**As a** Trade Operations Maker,
**I want** to request LC cancellation via a structured MT 707 with Tag 23S,
**So that** the Advising Bank receives a machine-readable cancellation instruction instead of a free-text MT 799.

### 4.6 Grounding Info
*   **Source:** `2026-05-08-import-lc-amendment-srg2024-brd.md` §7.1 — Tag 23S defined but no process coverage.
*   **Cross-reference:** Feature 2.6 (Cancellations) in `import-lc-consolidated-brd.md`.

---

## 5. Feature 5: Inbound MT 730 Data Dictionary (Reference Only)

### 5.1 Scope
This section documents the MT 730 (Acknowledgment) message structure for future inbound SWIFT processing. **No implementation is required now.** The manual consent logging workflow in the amendment BRD covers the current need.

### 5.2 MT 730 Tag Dictionary (SRG 2024)

| Tag | Field Name | Status | Inbound Mapping (Future) |
| :--- | :--- | :--- | :--- |
| **20** | Sender's Reference | **M** | Advising Bank's reference. Maps to `ImportLcAmendment.consentSwiftRef`. |
| **21** | Receiver's Reference | **M** | Our LC Number (`TradeInstrument.transactionRef`). Used to locate parent LC. |
| **25** | Account Identification | O | Nostro account for fee settlement. Informational. |
| **30** | Date of Message Being Ack'd | **M** | Date of Issue (Tag 31C) of the MT 700/707 being acknowledged. |
| **57a** | Account With Bank | O | Bank where advising fees should be remitted. Informational. |
| **71D** | Charges | O | Advising fees owed. Could trigger fee booking in future. |
| **72Z** | Sender to Receiver Info | O | Free-text confirmation (e.g., "LC safely advised to beneficiary"). |

### 5.3 Future Inbound Processing Rules (Phase 2 — Deferred)

| Rule ID | Rule |
| :--- | :--- |
| MT730-IN-01 | Match Tag 21 against `TradeInstrument.transactionRef` to locate the parent LC. |
| MT730-IN-02 | If a pending External Amendment exists, auto-update `beneficiaryDecisionEnumId` to `ACCEPTED` and populate `consentSwiftRef` with Tag 20. |
| MT730-IN-03 | If Tag 30 matches the amendment's `amendmentDate`, confirm the ack is for the correct amendment. |
| MT730-IN-04 | Auto-logged consent still requires Checker approval before merge (per amendment BRD state machine). |

### 5.4 Current Workaround
Per the amendment BRD (FR-AMD-07): Operations manually logs Beneficiary consent by entering the MT 730 reference and selecting `ACCEPTED`/`REJECTED`.

### 5.5 Grounding Info
*   **Source:** `20260504-MT_all_SWIFT_ver2024.md` §4 (MT 730 Acknowledgment).
*   **Cross-reference:** `2026-05-08-import-lc-amendment-srg2024-brd.md` §3 Step 7 and §6.

---

## 6. Traceability Matrix

### Requirements → Features

| Requirement ID | Description | Component |
| :--- | :--- | :--- |
| **FR-SWG-20** | Add Tags 49G, 49H, 40E to MT 700 entity and generation | Entity + SWIFT Gen |
| **FR-SWG-21** | Auto-generate MT 740 alongside MT 700 when Reimbursing Bank assigned | SWIFT Gen |
| **FR-SWG-22** | Auto-generate MT 747 alongside MT 707 when financials change | SWIFT Gen |
| **FR-RMB-01** | Manual Nostro Reconciliation with Maker/Checker matching | Business Logic + UI |
| **FR-RMB-02** | Block LC closure while Nostro reconciliation is PENDING | Business Logic |
| **FR-PRE-08** | Enforce 70×50 aggregate limit on Tag 77J (MT 750/734) | Validation + UI |
| **FR-CAN-04** | Support Tag 23S (CANCEL) on MT 707 for structured cancellation | SWIFT Gen |
| **FR-MT730-REF** | MT 730 inbound data dictionary (reference only) | Documentation |

### User Stories → Features

| User Story | Feature |
| :--- | :--- |
| US-ISS-04 | Feature 1: MT 700 Issuance Enhancements |
| US-RMB-01, US-RMB-02, US-RMB-03 | Feature 2: Reimbursement & Nostro Reconciliation |
| US-PRE-03 | Feature 3: Presentation Validation Tightening |
| US-CAN-03 | Feature 4: MT 707 Cancellation Request |
| *(none — reference only)* | Feature 5: Inbound MT 730 Data Dictionary |

### SWIFT Message Coverage (Updated)

| MT Message | Lifecycle Trigger | Feature | Status |
| :--- | :--- | :--- | :--- |
| MT 700 | LC Issuance authorized | Feature 1 (Tags 49G/49H/40E) | **This BRD** |
| MT 707 | Amendment authorized (cancellation) | Feature 4 (Tag 23S) | **This BRD** |
| MT 740 | LC Issuance (with Reimbursing Bank) | Feature 2.1 | **This BRD** |
| MT 747 | Amendment (financial, with Reimbursing Bank) | Feature 2.2 | **This BRD** |
| MT 750/734 | Presentation discrepant/refused | Feature 3 (77J enforcement) | **This BRD** |
| MT 730 | Inbound acknowledgment | Feature 5 (reference only) | **Deferred** |
| MT 742/900 | Inbound reimbursement claim/debit | *(Phase 2)* | **Deferred** |
