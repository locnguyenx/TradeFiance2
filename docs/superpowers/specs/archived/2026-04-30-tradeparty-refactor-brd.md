# Business Requirements Document (BRD)
**Project Name:** Digital Trade Finance Platform
**Module:** TradeParty Refactoring — Party-Role Junction Pattern
**Document Version:** 1.0
**Date:** April 30, 2026
**Source Requirements:** `docs/requirements/20260429-MT-special-tags-TradeParty.md`

---

## 1. Overview & Business Need

The current system stores bank party identifiers as flat fields scattered across `TradeInstrument` (e.g., `reimbursingBankBic`, `adviseThroughBankBic`) and `ImportLetterOfCredit` (e.g., `advisingBankBic`, `availableWithBic`, `draweeBankBic`). This approach has fundamental limitations:

1. **Cannot model multi-role parties:** In real-world trade, one bank frequently plays Advising + Confirming + Negotiating Bank on the same LC. Flat fields cannot express this without duplicating data.
2. **Missing SWIFT format alternatives:** Only BICs are stored. SWIFT requires Option D (Name/Address) and Option C (Clearing Code) fallbacks when BICs are unavailable.
3. **Unscalable for new MT messages:** Adding MT202 settlement routing (Intermediary, Sender's/Receiver's Correspondent) would require yet more flat fields.
4. **No bank eligibility enforcement:** The system cannot validate whether a bank has the required relationships (RMA, Nostro, FI Limits) to serve in a given role.

**Goal:** Replace flat party fields with a structured Party-Role junction pattern:
- **TradeParty** (base) + **TradePartyBank** (extension) for type-specific master data
- **TradeInstrumentParty** junction linking parties to instruments by role
- Backend services refactored to read party data from the junction
- Full-stack changes including entity, service, REST API, and frontend

---

## 2. User Stories

### US-TP-01: Party Master Data with Type-Specific Fields
**As a** system administrator,
**I want** to manage trade parties with dedicated field sets based on party type (Commercial vs Bank),
**So that** commercial parties store KYC/address data and bank parties additionally store relationship attributes (RMA, Nostro, FI Limits).

**Acceptance Criteria:**
- Commercial parties: partyName, registeredAddress, KYC status, sanctions status, country of risk
- Bank parties: all commercial fields PLUS swiftBic, clearingCode, hasActiveRMA, nostroAccountRef, fiLimitAvailable
- Party type (COMMERCIAL/BANK) is set at creation and determines which fields are applicable

### US-TP-02: Instrument Party Role Assignment
**As a** Trade Operations Maker,
**I want** to assign parties to specific roles on an LC instrument,
**So that** each role (Applicant, Beneficiary, Advising Bank, etc.) is filled by exactly one party per instrument.

**Acceptance Criteria:**
- Each role on an instrument maps to exactly one party
- A single party can be assigned to multiple different roles on the same instrument
- Assignment creates a `TradeInstrumentParty` junction record with (instrumentId, roleEnumId, partyId)
- Removing a role assignment deletes the junction record

### US-TP-03: Bank Eligibility Validation per Role
**As a** Trade Operations system,
**I want** to validate that a bank meets the relationship requirements for its assigned role,
**So that** compliance and operational requirements are enforced before LC submission.

**Acceptance Criteria:**
- Advising Bank: requires `hasActiveRMA = Y` (mandatory, no exceptions)
  - Advise Through Bank: does NOT require RMA with Issuing Bank (the Advising Bank handles RMA relay via MT 710)
- Reimbursing Bank: requires `nostroAccountRef` populated
- Confirming Bank: requires `fiLimitAvailable >= instrument liability amount`
- All bank roles: require `kycStatus = Active`, `sanctionsStatus = SANCTION_CLEAR`
- Validation errors return specific, actionable messages

### US-TP-04: Structured Party Review for Checker
**As a** Trade Operations Checker,
**I want** to see all parties and their roles in a structured view when reviewing an LC,
**So that** I can verify the party assignments are correct before authorization.

**Acceptance Criteria:**
- Parties displayed grouped by category: Commercial Parties, Banking Parties
- Each party shows: role label, party name, BIC (if bank), address, eligibility status
- Multi-role assignments clearly indicated (e.g., "JP Morgan — Advising, Confirming, Negotiating")

### US-TP-05: Service Layer Party Data Integration
**As a** backend service,
**I want** to read party data from the `TradeInstrumentParty` junction instead of flat fields,
**So that** all downstream processing (validation, generation, reporting) uses a single consistent party data source.

**Acceptance Criteria:**
- Import LC services (create, update, submit) accept party role assignments as structured input
- All services that previously read flat BIC fields now query the junction by role
- Flat BIC fields removed from `TradeInstrument` and `ImportLetterOfCredit`

---

## 3. Functional Requirements

### FR-TP-01: TradeParty Entity Schema (Modified)

Remove `partyRoleEnumId` (roles are now per-instrument via junction). Remove `swiftBic` (moves to `TradePartyBank`). Add `partyTypeEnumId` and `accountNumber`.

| Field | Type | Change | Notes |
|:---|:---|:---|:---|
| `partyId` | `id` (PK) | Keep | Auto-sequenced |
| `partyTypeEnumId` | `id` | **Add** | `PARTY_COMMERCIAL` or `PARTY_BANK` |
| `partyName` | `text-medium` | Keep | Official legal name (used in SWIFT tags) |
| `accountNumber` | `text-short` | **Add** | Party's account number (IBAN or domestic). Optional in MT700, mandatory in MT103 for beneficiary. See source requirements §"Explain accountNumber field" for tag-specific rules |
| `registeredAddress` | `text-long` | Keep | 4x35 SWIFT format |
| `kycStatus` | `text-short` | Keep | Active, Expired, Pending |
| `kycExpiryDate` | `date` | Keep | Next review date |
| `sanctionsStatus` | `text-short` | Keep | SANCTION_CLEAR, SANCTION_PENDING, SANCTION_BLOCKED |
| `countryOfRisk` | `id` | Keep | ISO country code |
| `swiftBic` | `text-short` | **Remove** | Moves to `TradePartyBank` — only banks have SWIFT BICs |
| `partyRoleEnumId` | `id` | **Remove** | Replaced by per-instrument junction |

### FR-TP-02: TradePartyBank Entity Schema (New)

Extension entity for bank-specific relationship attributes. Joined to `TradeParty` via `partyId`. Only created for parties where `partyTypeEnumId = PARTY_BANK`.

| Field | Type | Notes |
|:---|:---|:---|
| `partyId` | `id` (PK) | FK → `TradeParty`. One-to-one relationship |
| `swiftBic` | `text-short` | Valid 8/11 char SWIFT BIC. Bank-only field (moved from `TradeParty` base) |
| `clearingCode` | `text-short` | National routing code (e.g., ABA number, UK Sort Code). Used for SWIFT Option C |
| `hasActiveRMA` | `text-indicator` | Y/N. Required for Advising Bank role |
| `nostroAccountRef` | `text-medium` | Our Nostro account reference at this bank. Required for Reimbursing Bank role |
| `fiLimitAvailable` | `number-decimal` | Available FI credit limit for this bank. Required for Confirming Bank role |
| `fiLimitCurrencyUomId` | `id` | Currency of the FI limit |

**Relationship:** `<relationship type="one" related="trade.TradeParty"><key-map field-name="partyId"/></relationship>`

### FR-TP-03: TradeInstrumentParty Junction Entity (New)

Links a party to an instrument with a specific role. Primary key is `(instrumentId, roleEnumId)` — this PK constraint enforces that each role is filled by exactly one party per instrument. A party may appear in multiple junction records with different roles on the same instrument.

| Field | Type | Notes |
|:---|:---|:---|
| `instrumentId` | `id` (PK) | FK → `TradeInstrument` |
| `roleEnumId` | `id` (PK) | FK → `Enumeration` (TradePartyRole type). PK ensures no duplicate roles per instrument |
| `partyId` | `id` | FK → `TradeParty`. The party filling this role |

**Note:** `accountNumber` is NOT on this junction — it belongs on the `TradeParty` master record. The account number is a party attribute, not an instrument-role attribute. SWIFT generation services read it from `TradeParty` and apply tag-context rules (forbidden on Tag 41a/42a, optional on most MT700 tags, mandatory on MT103 Tag 59). See source requirements §"Explain accountNumber field".

**Relationships:**
- `<relationship type="one" related="trade.TradeInstrument"><key-map field-name="instrumentId"/></relationship>`
- `<relationship type="one" related="trade.TradeParty"><key-map field-name="partyId"/></relationship>`

### FR-TP-04: Role Enumeration

`EnumerationType: TradePartyRole`

| Enum ID | Description | Applicable Party Type | Primary MT700 Tag |
|:---|:---|:---|:---|
| `TP_APPLICANT` | Applicant (Ordering Customer) | Commercial | Tag 50 |
| `TP_BENEFICIARY` | Beneficiary | Commercial | Tag 59 |
| `TP_ISSUING_BANK` | Issuing Bank (Our Bank) | Bank | Header Block 2 (sender) |
| `TP_APPLICANT_BANK` | Applicant Bank | Bank | Tag 51a |
| `TP_ADVISING_BANK` | Advising Bank | Bank | Header Block 2 (receiver) |
| `TP_ADVISE_THROUGH_BANK` | Advise Through Bank | Bank | Tag 57a |
| `TP_CONFIRMING_BANK` | Confirming/Requested Confirming Bank | Bank | Tag 58a |
| `TP_REIMBURSING_BANK` | Reimbursing Bank | Bank | Tag 53a |
| `TP_NEGOTIATING_BANK` | Negotiating/Available With Bank | Bank | Tag 41a |
| `TP_DRAWEE_BANK` | Drawee Bank | Bank | Tag 42a |
| `TP_PRESENTING_BANK` | Presenting Bank | Bank | MT750/734/752 |
| `TP_INTERMEDIARY_BANK` | Intermediary Bank (Settlement) | Bank | Tag 56a (MT202) |
| `TP_SENDERS_CORRESPONDENT` | Sender's Correspondent (Settlement) | Bank | Tag 53a (MT202) |
| `TP_RECEIVERS_CORRESPONDENT` | Receiver's Correspondent (Settlement) | Bank | Tag 54a (MT202) |

**Note:** Settlement-specific roles (INTERMEDIARY, SENDERS_CORRESPONDENT, RECEIVERS_CORRESPONDENT) are added to the instrument during Process 3.4 (Settlement), not at issuance time.

#### Tag Coverage Verification

The following table verifies that all party-related SWIFT tags across all MT message types are served by the role enumeration:

| MT Message | Tag | Party Role Used | Account Number Rule |
|:---|:---|:---|:---|
| MT700 | 50 (Applicant) | `TP_APPLICANT` | Optional |
| MT700 | 59 (Beneficiary) | `TP_BENEFICIARY` | Optional |
| MT700 | 51a (Applicant Bank) | `TP_APPLICANT_BANK` | Optional |
| MT700 | 41a (Available With) | `TP_NEGOTIATING_BANK` or "ANY BANK" | **Forbidden** |
| MT700 | 42a (Drawee) | `TP_DRAWEE_BANK` | **Forbidden** |
| MT700 | 53a (Reimbursing Bank) | `TP_REIMBURSING_BANK` | Optional |
| MT700 | 57a (Advise Through) | `TP_ADVISE_THROUGH_BANK` | Optional |
| MT700 | 58a (Confirming Bank) | `TP_CONFIRMING_BANK` | Optional |
| MT707 | 50 (Applicant) | `TP_APPLICANT` | **Forbidden** (flat text only in MT707) |
| MT707 | 59 (Beneficiary) | `TP_BENEFICIARY` | Optional |
| MT740 | 59/59A (Beneficiary) | `TP_BENEFICIARY` | Optional |
| MT740 | 58a (Negotiating Bank) | `TP_NEGOTIATING_BANK` | Optional |
| MT750/734/752 | 20/21 (References) | `TP_PRESENTING_BANK` (via `presentingBankRef`) | N/A |
| MT202 | 52a (Ordering Institution) | `TP_ISSUING_BANK` | Optional |
| MT202 | 53a (Sender's Correspondent) | `TP_SENDERS_CORRESPONDENT` | Highly Recommended |
| MT202 | 54a (Receiver's Correspondent) | `TP_RECEIVERS_CORRESPONDENT` | Optional |
| MT202 | 56a (Intermediary) | `TP_INTERMEDIARY_BANK` | Optional |
| MT202 | 58a (Beneficiary Institution) | `TP_PRESENTING_BANK` | Optional |
| MT103 | 50a (Ordering Customer) | `TP_APPLICANT` | Mandatory |
| MT103 | 59a (Beneficiary) | `TP_BENEFICIARY` | **Mandatory** (IBAN/account required) |
| MT103 | 56a (Intermediary) | `TP_INTERMEDIARY_BANK` | Optional |
| MT103 | 57a (Account With Institution) | `TP_ADVISING_BANK` | Optional |

### FR-TP-05: Party Type Enumeration

`EnumerationType: TradePartyType`

| Enum ID | Description |
|:---|:---|
| `PARTY_COMMERCIAL` | Commercial entity (importer, exporter, trading company) |
| `PARTY_BANK` | Financial institution (bank, FI) |

### FR-TP-06: Fields Removed from TradeInstrument

These flat fields are replaced by `TradeInstrumentParty` junction records:

| Field Removed | Replacement Junction Role | Notes |
|:---|:---|:---|
| `applicantPartyId` | `TP_APPLICANT` | FK moves to junction |
| `beneficiaryPartyId` | `TP_BENEFICIARY` | FK moves to junction |
| `reimbursingBankBic` | `TP_REIMBURSING_BANK` | Party's `swiftBic` via junction |
| `reimbursingBankName` | `TP_REIMBURSING_BANK` | Party's `partyName` + `registeredAddress` via junction |
| `adviseThroughBankBic` | `TP_ADVISE_THROUGH_BANK` | Party's `swiftBic` via junction |
| `adviseThroughBankName` | `TP_ADVISE_THROUGH_BANK` | Party's `partyName` + `registeredAddress` via junction |
| `beneficiaryName` | `TP_BENEFICIARY` | Party's `partyName` via junction |

### FR-TP-07: Fields Removed from ImportLetterOfCredit

| Field Removed | Replacement Junction Role | Notes |
|:---|:---|:---|
| `applicantPartyId` | `TP_APPLICANT` | FK moves to junction |
| `beneficiaryPartyId` | `TP_BENEFICIARY` | FK moves to junction |
| `applicantName` | `TP_APPLICANT` | Party's `partyName` via junction |
| `beneficiaryName` | `TP_BENEFICIARY` | Party's `partyName` via junction |
| `advisingBankBic` | `TP_ADVISING_BANK` | Party's `swiftBic` via junction |
| `advisingThroughBankBic` | `TP_ADVISE_THROUGH_BANK` | Party's `swiftBic` via junction |
| `issuingBankBic` | `TP_ISSUING_BANK` | Party's `swiftBic` via junction (or system config) |
| `availableWithBic` | `TP_NEGOTIATING_BANK` | Replaced by junction + `availableWithEnumId` |
| `availableWithName` | `TP_NEGOTIATING_BANK` | Party's `partyName` via junction |
| `draweeBankBic` | `TP_DRAWEE_BANK` | Party's `swiftBic` via junction |

### FR-TP-08: Fields Removed from TradeDocumentPresentation

| Field Removed | Replacement | Notes |
|:---|:---|:---|
| `presentingBankBic` | `TP_PRESENTING_BANK` junction record | Added to instrument when presentation is created |

**Note:** `presentingBankRef` (the presenting bank's own reference number) stays on `TradeDocumentPresentation` — it is a presentation attribute, not a party attribute.

### FR-TP-09: "Available With" — Explicit User Choice

Tag 41a can be either a specific bank or "ANY BANK". This is an **explicit user choice**, not inferred from data presence.

**New field on `ImportLetterOfCredit`:** `availableWithEnumId` — values: `AVAIL_ANY_BANK` or `AVAIL_SPECIFIC_BANK`

**Rules:**
- The Maker explicitly selects either "ANY BANK" or a specific negotiating bank in the UI
- If the Maker selects a specific bank: set `availableWithEnumId = AVAIL_SPECIFIC_BANK` and create a `TP_NEGOTIATING_BANK` junction record
- If the Maker selects "ANY BANK": set `availableWithEnumId = AVAIL_ANY_BANK` and remove any existing `TP_NEGOTIATING_BANK` junction record
- The Maker can switch between these choices at any time before submission, regardless of prior selection
- SWIFT generation reads `availableWithEnumId` to determine Tag 41a content
- The `availableByEnumId` field (BY PAYMENT, BY NEGOTIATION, etc.) remains on `ImportLetterOfCredit` as an instrument attribute
- **Account numbers are forbidden on Tag 41a** — even if the negotiating bank's `TradeParty` has an `accountNumber`, the SWIFT builder must suppress it

---

### FR-TP-10: Party CRUD Services

**Service:** `TradeCommonServices.create#TradeParty`
- Input: `partyTypeEnumId`, `partyName`, `registeredAddress`, `accountNumber`, KYC/sanctions fields
- If `partyTypeEnumId = PARTY_BANK`: also accepts and creates `TradePartyBank` extension fields (`swiftBic`, `clearingCode`, `hasActiveRMA`, `nostroAccountRef`, `fiLimitAvailable`, `fiLimitCurrencyUomId`)
- Output: `partyId`

**Service:** `TradeCommonServices.update#TradeParty`
- Same field set. Updates base and extension records as applicable.

### FR-TP-11: Instrument Party Assignment Service

**Service:** `TradeCommonServices.assign#InstrumentParty`
- Input: `instrumentId`, `roleEnumId`, `partyId`, optional `accountNumber`
- Validates:
  - Party exists and `partyTypeEnumId` matches role's applicable type (FR-TP-04)
  - If bank role: party has `TradePartyBank` extension record
  - Role-specific eligibility (FR-TP-12)
  - KYC and sanctions status
- Creates `TradeInstrumentParty` record
- If role already assigned: updates the existing record with new partyId

**Service:** `TradeCommonServices.remove#InstrumentParty`
- Input: `instrumentId`, `roleEnumId`
- Deletes the junction record

**Service:** `TradeCommonServices.get#InstrumentParties`
- Input: `instrumentId`
- Output: List of all party-role assignments with full party details (joined)

### FR-TP-12: Bank Eligibility Validation Rules

Validated during `assign#InstrumentParty` and during `submit#ForApproval`:

| Role | Validation Rule | Error Message |
|:---|:---|:---|
| `TP_ADVISING_BANK` | `hasActiveRMA = Y` (mandatory, no exceptions) | "Advising Bank (MT700 Receiver) must have active RMA with the Issuing Bank." |
| `TP_ADVISE_THROUGH_BANK` | No RMA check required | N/A — RMA between Advising Bank and Advise Through Bank is outside Issuing Bank's scope |
| `TP_REIMBURSING_BANK` | `nostroAccountRef` is not null | "Cannot designate as Reimbursing Bank: No active Nostro account found." |
| `TP_CONFIRMING_BANK` | `fiLimitAvailable >= instrument max liability` | "Confirming Bank's FI limit (X) is insufficient for instrument liability (Y)." |
| All bank roles | `kycStatus = Active` | "Party KYC status is expired." |
| All bank roles | `sanctionsStatus = SANCTION_CLEAR` | "Party is under sanctions review/block." |
| All roles | `partyTypeEnumId` matches role's applicable type | "Role X requires a [Commercial/Bank] party." |

### FR-TP-13: Import LC Services Refactor

**Service:** `ImportLcServices.create#ImportLetterOfCredit`
- Accepts party assignments as structured input: list of `{roleEnumId, partyId, accountNumber}`
- Creates `TradeInstrumentParty` records alongside the instrument
- Minimum required roles for submission: `TP_APPLICANT`, `TP_BENEFICIARY`
- Removes direct `applicantPartyId`, `beneficiaryPartyId` from input parameters

**Service:** `ImportLcServices.update#ImportLetterOfCredit`
- Accepts party assignment updates in same structure
- Handles add/update/remove of role assignments

**Service:** `ImportLcServices.submit#ForApproval`
- Validates all assigned parties via FR-TP-12 eligibility rules
- Validates mandatory roles are assigned

### FR-TP-14: View Entity Refactor

`ImportLetterOfCreditView` currently joins to `TradeParty` via `applicantPartyId` and `beneficiaryPartyId`. Refactor to join through `TradeInstrumentParty`:

```xml
<!-- Applicant via junction -->
<member-entity entity-alias="app_role" entity-name="trade.TradeInstrumentParty" 
               join-from-alias="lc" join-optional="true">
    <key-map field-name="instrumentId"/>
    <entity-condition><econdition field-name="roleEnumId" value="TP_APPLICANT"/></entity-condition>
</member-entity>
<member-entity entity-alias="aparty" entity-name="trade.TradeParty" 
               join-from-alias="app_role" join-optional="true">
    <key-map field-name="partyId"/>
</member-entity>
```

**Aliases:** `applicantPartyName` → `aparty.partyName`, `beneficiaryPartyName` → `bparty.partyName`

### FR-TP-15: Master Data Migration

Existing `TradeFinanceMasterData.xml` must be updated:

1. **Add `partyTypeEnumId`** to all existing `TradeParty` records:
   - Commercial: ACME_CORP_001, GLOBAL_EXP_002, RISKY_BIZ_003, BANNED_ENTITY_004, ORG_ZIZI_CORP
   - Bank: ISSUING_BANK_001, ADVISING_BANK_001

2. **Create `TradePartyBank`** records for bank parties with relationship attributes

3. **Create `TradeInstrumentParty`** junction records for existing sample instruments (LC240001, LC240002, LC240003)

4. **Add enumeration data** for `TradePartyRole` and `TradePartyType`

5. **Remove deprecated flat fields** from sample TradeInstrument/ImportLetterOfCredit data

### FR-TP-16: REST API Contract Changes

The REST API must expose party assignments as structured data:

**Create/Update LC — Input:**
```json
{
  "amount": 500000,
  "currencyUomId": "USD",
  "parties": [
    { "roleEnumId": "TP_APPLICANT", "partyId": "ACME_CORP_001" },
    { "roleEnumId": "TP_BENEFICIARY", "partyId": "GLOBAL_EXP_002" },
    { "roleEnumId": "TP_ADVISING_BANK", "partyId": "ADVISING_BANK_001" },
    { "roleEnumId": "TP_REIMBURSING_BANK", "partyId": "ADVISING_BANK_001" }
  ]
}
```

**Get LC — Output:**
Party data returned as a `parties` array grouped by role, with full party details resolved.

### FR-TP-17: Backend Validation Rules Summary

All validation is enforced at the backend service layer. Frontend validation is for UX convenience only.

| Rule | Enforcement Point | Logic |
|:---|:---|:---|
| Party type matches role | `assign#InstrumentParty` | Commercial roles → PARTY_COMMERCIAL; Bank roles → PARTY_BANK |
| One party per role per instrument | Entity PK constraint | PK = (instrumentId, roleEnumId) |
| Mandatory roles for submission | `submit#ForApproval` | TP_APPLICANT and TP_BENEFICIARY must be assigned |
| Bank eligibility per role | `assign#InstrumentParty`, `submit#ForApproval` | Per FR-TP-12 matrix |
| KYC/Sanctions on all parties | `submit#ForApproval` | All assigned parties must pass compliance checks |
| SWIFT tag format selection | SWIFT generation services | Determined at runtime from party data (see existing SWIFT Generation BRD) |

---

## 4. Traceability Matrix

| User Story | Functional Requirement(s) |
|:---|:---|
| US-TP-01 | FR-TP-01 (TradeParty), FR-TP-02 (TradePartyBank), FR-TP-05 (Party Types), FR-TP-10 (CRUD) |
| US-TP-02 | FR-TP-03 (Junction), FR-TP-04 (Roles), FR-TP-11 (Assignment Service) |
| US-TP-03 | FR-TP-12 (Eligibility), FR-TP-17 (Validation Rules) |
| US-TP-04 | FR-TP-14 (View Entity) |
| US-TP-05 | FR-TP-06/07/08 (Field Removals), FR-TP-13 (LC Services), FR-TP-16 (REST API) |

---

## 5. Out of Scope

The following are explicitly out of scope for this BRD and covered by existing specifications:

- **SWIFT tag format selection logic** (Option A vs D vs C) — covered by SWIFT Generation BRD (`2026-04-25-swift-generation-brd.md`). This BRD only changes the data source (junction instead of flat fields).
- **RMA/Nostro/FI Limit master data management UI** — future enhancement. This BRD uses simple flags; a full relationship management module is deferred.

**In scope (clarification):**
- **SWIFT field validation on TradeParty data** IS in scope. When capturing party data (`partyName`, `registeredAddress`, `accountNumber`, `swiftBic`), the existing Layer 1 validation rules (X Character Set, 4x35 line limits, BIC format) from the SWIFT Validation BRD (`2026-04-25-swift-validation-brd.md`) must be applied at the `create#TradeParty` / `update#TradeParty` service layer.

