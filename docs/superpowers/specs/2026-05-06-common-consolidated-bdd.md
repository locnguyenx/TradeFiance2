# Consolidated Behavior-Driven Development (BDD) Specification
**ABOUTME:** Consolidated BDD scenarios for the Common Trade Finance Module.
This document merges all source BDD specs into one traceable document aligned with the consolidated BRD.

**Project Name:** Digital Trade Finance Platform
**Module:** Common Trade Finance (Cross-Functional Core)
**Document Version:** 2.0
**Date:** May 06, 2026

**Superseded BDDs:**
*   `2026-04-21-common-module-bdd.md`
*   `2026-04-30-tradeparty-refactor-bdd.md`
*   `2026-04-28-trade-transaction-tracking-bdd.md`
*   `2026-05-03-user-identity-access-bdd.md`

---

## 1. Traceability Matrix

| Feature | Scenario ID | Title | Type | Source BRD Req | User Story |
|---|---|---|---|---|---|
| **1: Trade Party** | BDD-CMN-TP-01 | Create commercial trade party | Happy | US-TP-01 | US-TP-01 |
| **1: Trade Party** | BDD-CMN-TP-02 | Create bank party with extension | Happy | US-TP-01 | US-TP-01 |
| **1: Trade Party** | BDD-CMN-TP-03 | Reject commercial party invalid SWIFT chars | Edge | FR-TP-01 | US-TP-01 |
| **1: Trade Party** | BDD-CMN-TP-04 | Assign parties to instrument roles | Happy | FR-TP-03 | US-TP-02 |
| **1: Trade Party** | BDD-CMN-TP-05 | Same bank multiple roles on one instrument | Happy | FR-TP-03 | US-TP-02 |
| **1: Trade Party** | BDD-CMN-TP-06 | Reject duplicate role replaces existing | Edge | FR-TP-03 | US-TP-02 |
| **1: Trade Party** | BDD-CMN-TP-07 | Reject commercial party assigned to bank role | Edge | FR-TP-03 | US-TP-02 |
| **1: Trade Party** | BDD-CMN-TP-08 | Reject advising bank without RMA | Edge | FR-TP-12 | US-TP-03 |
| **1: Trade Party** | BDD-CMN-TP-09 | Allow advise through bank without RMA | Happy | FR-TP-12 | US-TP-03 |
| **1: Trade Party** | BDD-CMN-TP-10 | Reject reimbursing bank without Nostro | Edge | FR-TP-12 | US-TP-03 |
| **1: Trade Party** | BDD-CMN-TP-11 | Reject confirming bank insufficient FI limit | Edge | FR-TP-12 | US-TP-04 |
| **1: Trade Party** | BDD-CMN-TP-12 | Create LC with party role assignments | Happy | FR-TP-13 | US-TP-05 |
| **1: Trade Party** | BDD-CMN-TP-13 | Select "ANY BANK" for Available With | Happy | US-TP-05 | US-TP-05 |
| **1: Trade Party** | BDD-CMN-TP-14 | Switch from specific bank to ANY BANK | Edge | FR-TP-09 | US-TP-05 |
| **1: Trade Party** | BDD-CMN-TP-15 | Submit LC validates mandatory roles | Edge | FR-TP-13 | US-TP-05 |
| **1: Trade Party** | BDD-CMN-TP-16 | Submit LC validates party eligibility | Edge | FR-TP-13 | US-TP-05 |
| **1: Trade Party** | BDD-CMN-TP-17 | View entity resolves party names from junction | Happy | FR-TP-14 | US-TP-04 |
| **2: Credit Facility** | BDD-CMN-FAC-01 | Facility limit earmark | Happy | US-LIM-01 | US-LIM-01 |
| **2: Credit Facility** | BDD-CMN-FAC-02 | Expired facility block | Edge | REQ-COM-ENT-03 | US-LIM-01 |
| **2: Credit Facility** | BDD-CMN-FAC-03 | Facility 95% threshold warning | Happy | US-LIM-03 | US-LIM-03 |
| **3: Transaction Lifecycle** | BDD-CMN-TXN-01 | Transaction base attributes and maker tracking | Happy | REQ-COM-ENT-01 | US-TXN-01 |
| **3: Transaction Lifecycle** | BDD-CMN-TXN-02 | Version increment on authorization | Happy | REQ-COM-ENT-01 | US-TXN-01 |
| **3: Transaction Lifecycle** | BDD-CMN-TXN-03 | Valid party KYC acceptance | Happy | REQ-COM-ENT-02 | US-TXN-01 |
| **3: Transaction Lifecycle** | BDD-CMN-TXN-04 | Expired party KYC rejection | Edge | REQ-COM-ENT-02 | US-TXN-01 |
| **3: Transaction Lifecycle** | BDD-CMN-TXN-05 | Processing flow: Draft to Submitted dual-status | Happy | REQ-COM-WF-01 | US-TXN-01 |
| **3: Transaction Lifecycle** | BDD-CMN-TXN-06 | Transaction-primary: New LC creates issuance | Happy | US-TXN-01 | US-TXN-01 |
| **3: Transaction Lifecycle** | BDD-CMN-TXN-07 | Checker authorizes by transactionId | Happy | US-TXN-02 | US-TXN-02 |
| **3: Transaction Lifecycle** | BDD-CMN-TXN-08 | Decoupled data: View pending amendment snapshot | Happy | US-TXN-01 | US-TXN-01 |
| **3: Transaction Lifecycle** | BDD-CMN-TXN-09 | Dual-status clarity: Workflow vs lifecycle | Happy | US-TXN-03 | US-TXN-03 |
| **4: Authority & Access** | BDD-CMN-AUTH-01 | Tier enforcement by equivalent amount | Happy | REQ-COM-AUTH-01 | US-UIA-01 |
| **4: Authority & Access** | BDD-CMN-AUTH-02 | Tier 4 dual checker enforcement | Edge | REQ-COM-AUTH-02 | US-UIA-02 |
| **4: Authority & Access** | BDD-CMN-AUTH-03 | Amendment total liability route determination | Happy | REQ-COM-AUTH-03A | US-UIA-01 |
| **4: Authority & Access** | BDD-CMN-AUTH-04 | Compliance route overrides financial route | Edge | REQ-COM-AUTH-03C | US-UIA-01 |
| **4: Authority & Access** | BDD-CMN-AUTH-05 | Priority queue ordering | Happy | REQ-COM-AUTH-01 | US-UIA-01 |
| **4: Authority & Access** | BDD-CMN-AUTH-06 | Segregation of duties prevention | Edge | REQ-COM-VAL-02 | US-UIA-03 |
| **4: Authority & Access** | BDD-CMN-AUTH-07 | Suspended account exclusion | Edge | REQ-COM-MAS-02 | US-UIA-04 |
| **4: Authority & Access** | BDD-CMN-AUTH-08 | Successful login | Happy | FR-AUTH-02 | US-AUTH-01 |
| **4: Authority & Access** | BDD-CMN-AUTH-09 | Failed login generic error | Edge | FR-AUTH-04 | US-AUTH-01 |
| **4: Authority & Access** | BDD-CMN-AUTH-10 | Account lockout after 3 failures | Edge | FR-AUTH-05 | US-AUTH-01 |
| **4: Authority & Access** | BDD-CMN-AUTH-11 | Unauthorized access redirect | Edge | FR-AUTH-01 | US-AUTH-01 |
| **4: Authority & Access** | BDD-CMN-AUTH-12 | View own profile | Happy | FR-PROF-01 | US-AUTH-02 |
| **4: Authority & Access** | BDD-CMN-AUTH-13 | User changes own password | Happy | FR-PROF-03 | US-AUTH-02 |
| **4: Authority & Access** | BDD-CMN-AUTH-14 | User logs out | Happy | FR-AUTH-06 | US-AUTH-02 |
| **4: Authority & Access** | BDD-CMN-AUTH-15 | Admin creates new user | Happy | FR-ADM-04 | US-AUTH-03 |
| **4: Authority & Access** | BDD-CMN-AUTH-16 | Admin assigns roles to user | Happy | FR-ADM-07 | US-AUTH-03 |
| **4: Authority & Access** | BDD-CMN-AUTH-17 | Admin resets user password | Happy | FR-ADM-06 | US-AUTH-03 |
| **4: Authority & Access** | BDD-CMN-AUTH-18 | Admin disables account | Happy | FR-ADM-09 | US-AUTH-03 |
| **4: Authority & Access** | BDD-CMN-AUTH-19 | Admin cannot disable own account | Edge | FR-ADM-10 | US-AUTH-03 |
| **4: Authority & Access** | BDD-CMN-AUTH-20 | Immutability prevents active record mod | Edge | REQ-COM-VAL-02 | US-TXN-01 |
| **5: Product Config** | BDD-CMN-PRD-01 | Active component verification | Happy | REQ-COM-PRD-01 | US-PRD-01 |
| **5: Product Config** | BDD-CMN-PRD-02 | Allowed tenor sight restriction | Edge | REQ-COM-PRD-01 | US-PRD-01 |
| **5: Product Config** | BDD-CMN-PRD-03 | Tolerance limit ceiling check | Edge | REQ-COM-PRD-01 | US-PRD-02 |
| **5: Product Config** | BDD-CMN-PRD-04 | Display revolving fields rule | Happy | REQ-COM-PRD-01 | US-PRD-01 |
| **5: Product Config** | BDD-CMN-PRD-05 | Advance payment document avoidance | Happy | REQ-COM-PRD-01 | US-PRD-01 |
| **5: Product Config** | BDD-CMN-PRD-06 | Standby routing path rule | Happy | REQ-COM-PRD-01 | US-PRD-01 |
| **5: Product Config** | BDD-CMN-PRD-07 | Transferable instructions render | Happy | REQ-COM-PRD-01 | US-PRD-01 |
| **5: Product Config** | BDD-CMN-PRD-08 | Islamic ledger classification | Happy | REQ-COM-PRD-01 | US-PRD-01 |
| **5: Product Config** | BDD-CMN-PRD-09 | Mandatory margin prerequisite | Edge | REQ-COM-PRD-01 | US-PRD-02 |
| **5: Product Config** | BDD-CMN-PRD-10 | Custom SLA deadline formula | Happy | REQ-COM-PRD-01 | US-PRD-01 |
| **5: Product Config** | BDD-CMN-PRD-11 | Default SWIFT MT generation | Happy | REQ-COM-PRD-01 | US-PRD-01 |
| **5: Product Config** | BDD-CMN-PRD-12 | Tariff matrix priority overrides | Happy | REQ-COM-MAS-01 | US-FEE-01 |
| **5: Product Config** | BDD-CMN-PRD-13 | Tariff matrix minimum floor fee | Edge | REQ-COM-MAS-01 | US-FEE-01 |
| **6: Audit & Timeline** | BDD-CMN-AUD-01 | Full narrative rendering | Happy | REQ-UTN-01 | US-UTN-01 |
| **6: Audit & Timeline** | BDD-CMN-AUD-02 | Timeline: Checker authorizes pending node | Happy | US-UTN-02 | US-UTN-02 |
| **6: Audit & Timeline** | BDD-CMN-AUD-03 | Timeline: Rejection with reason display | Edge | US-UTN-02 | US-UTN-02 |
| **6: Audit & Timeline** | BDD-CMN-AUD-04 | Delta analysis: Amendment differences | Happy | US-UTN-03 | US-UTN-03 |
| **6: Audit & Timeline** | BDD-CMN-AUD-05 | Mandatory transaction delta audit log | Happy | REQ-COM-MAS-03 | US-UTN-01 |
| **6: Audit & Timeline** | BDD-CMN-AUD-06 | Sanctions check triggers hold | Edge | REQ-COM-NOT-02 | US-UTN-01 |
| **7: FX, SLA & Notifs** | BDD-CMN-FX-01 | Precision: Zero decimal JPY | Edge | REQ-COM-FX-01 | US-FX-01 |
| **7: FX, SLA & Notifs** | BDD-CMN-FX-02 | Precision: 2 decimals USD | Happy | REQ-COM-FX-01 | US-FX-01 |
| **7: FX, SLA & Notifs** | BDD-CMN-FX-03 | Daily board rate for limit consumption | Happy | REQ-COM-FX-02 | US-FX-01 |
| **7: FX, SLA & Notifs** | BDD-CMN-FX-04 | Live FX spread for settlement | Happy | REQ-COM-FX-02 | US-FX-01 |
| **7: FX, SLA & Notifs** | BDD-CMN-SLA-01 | SLA timer skips holidays | Happy | REQ-COM-SLA-01 | US-SLA-01 |
| **7: FX, SLA & Notifs** | BDD-CMN-SLA-02 | Timer exhaustion generates block | Edge | REQ-COM-SLA-02 | US-SLA-02 |
| **8: Fee & Charge** | *(covered by BDD-CMN-PRD-12/13 above)* | | | | |
| **9: Navigation** | BDD-CMN-NAV-01 | Global transaction log | Happy | REQ-NAV-01.3 | US-NAV-02 |
| **9: Navigation** | BDD-CMN-NAV-02 | Contextual search toggle | Happy | REQ-SRH-01.1 | US-SRH-01 |
| **9: Navigation** | BDD-CMN-NAV-03 | Checker queue modal interaction | Happy | REQ-UI-CMN-02 | US-NAV-02 |
| **9: Navigation** | BDD-CMN-SRH-01 | Cross-reference indexing | Happy | REQ-SRH-01.2 | US-SRH-01 |
| **1: Trade Party** | BDD-CMN-TP-18 | Role uniqueness enforcement | Edge | FR-TP-18 | US-TP-02 |
| **8: Fees** | BDD-CMN-FEE-01 | Customer exception rates | Happy | US-FEE-02 | US-FEE-02 |

---

## 2. Detailed BDD Scenarios

### Feature 1: Trade Party Management

#### Scenario BDD-CMN-TP-01: Create commercial trade party
**US-TP-01 | FR-TP-01, FR-TP-10**
*Type: Happy Path*

* **Given** no `TradeParty` with partyId "TEST_COMM_001" exists
* **When** `create#TradeParty` is called with:
  - `partyTypeEnumId = PARTY_COMMERCIAL`
  - `partyName = "Vietnam Textiles JSC"`
  - `registeredAddress = "123 Le Loi, District 1, Ho Chi Minh City, Vietnam"`
  - `accountNumber = "VN12VCOM01234567890123"`
  - `kycStatus = "Active"`
  - `sanctionsStatus = "SANCTION_CLEAR"`
  - `countryOfRisk = "VNM"`
* **Then** a `TradeParty` record is created with all provided fields
* **And** `partyTypeEnumId = PARTY_COMMERCIAL`
* **And** no `TradePartyBank` record exists for this partyId
* **And** SWIFT X Character Set validation is applied to `partyName` and `registeredAddress`

#### Scenario BDD-CMN-TP-02: Create bank party with extension fields
**US-TP-01 | FR-TP-02, FR-TP-10**
*Type: Happy Path*

* **Given** no `TradeParty` with partyId "TEST_BANK_001" exists
* **When** `create#TradeParty` is called with:
  - `partyTypeEnumId = PARTY_BANK`
  - `partyName = "Citibank London"`
  - `registeredAddress = "25 Canada Square, London E14 5LB"`
  - `kycStatus = "Active"`
  - `sanctionsStatus = "SANCTION_CLEAR"`
  - `countryOfRisk = "GBR"`
  - `swiftBic = "CITIGB2L"`
  - `clearingCode = "185008"`
  - `hasActiveRMA = "Y"`
  - `nostroAccountRef = "NOSTRO-USD-CITI-001"`
  - `fiLimitAvailable = 10000000.00`
  - `fiLimitCurrencyUomId = "USD"`
* **Then** a `TradeParty` record is created with base fields
* **And** a `TradePartyBank` record is created with `swiftBic`, `clearingCode`, `hasActiveRMA`, `nostroAccountRef`, `fiLimitAvailable`, `fiLimitCurrencyUomId`
* **And** BIC validation confirms "CITIGB2L" is valid 8-char format

#### Scenario BDD-CMN-TP-03: Reject commercial party with invalid SWIFT characters
**US-TP-01 | FR-TP-01, FR-TP-10**
*Type: Edge Case*

* **Given** no existing party
* **When** `create#TradeParty` is called with:
  - `partyTypeEnumId = PARTY_COMMERCIAL`
  - `partyName = "Acme & Sons Trading @Corp"`
  - `registeredAddress = "123 Main St"`
* **Then** the service returns a validation error
* **And** the error identifies invalid characters `&` and `@` in `partyName`
* **And** no `TradeParty` record is created

#### Scenario BDD-CMN-TP-04: Assign parties to instrument roles
**US-TP-02 | FR-TP-03, FR-TP-11**
*Type: Happy Path*

* **Given** a `TradeInstrument` with `instrumentId = "LC_TEST_001"`
* **And** a commercial `TradeParty` "ACME_CORP_001" (PARTY_COMMERCIAL)
* **And** a commercial `TradeParty` "GLOBAL_EXP_002" (PARTY_COMMERCIAL)
* **And** a bank `TradeParty` "CITI_BANK_001" (PARTY_BANK) with `hasActiveRMA = "Y"`
* **When** `assign#InstrumentParty` is called for:
  - `(LC_TEST_001, TP_APPLICANT, ACME_CORP_001)`
  - `(LC_TEST_001, TP_BENEFICIARY, GLOBAL_EXP_002)`
  - `(LC_TEST_001, TP_ADVISING_BANK, CITI_BANK_001)`
* **Then** three `TradeInstrumentParty` junction records are created
* **And** each record has the correct `instrumentId`, `roleEnumId`, `partyId`
* **And** `get#InstrumentParties(LC_TEST_001)` returns all three assignments

#### Scenario BDD-CMN-TP-05: Same bank multiple roles on one instrument
**US-TP-02, US-TP-05 | FR-TP-03, FR-TP-11**
*Type: Happy Path*

* **Given** a `TradeInstrument` with `instrumentId = "LC_TEST_002"`
* **And** a bank `TradeParty` "JPM_BANK_001" (PARTY_BANK) with `hasActiveRMA = "Y"`, `nostroAccountRef = "NOSTRO-JPM-001"`, `fiLimitAvailable = 5000000.00`
* **When** `assign#InstrumentParty` is called for:
  - `(LC_TEST_002, TP_ADVISING_BANK, JPM_BANK_001)`
  - `(LC_TEST_002, TP_CONFIRMING_BANK, JPM_BANK_001)`
  - `(LC_TEST_002, TP_REIMBURSING_BANK, JPM_BANK_001)`
* **Then** three `TradeInstrumentParty` junction records are created
* **And** all three records reference the same `partyId = JPM_BANK_001`
* **And** each record has a distinct `roleEnumId`

#### Scenario BDD-CMN-TP-06: Reject duplicate role replaces existing
**US-TP-02 | FR-TP-03, FR-TP-11**
*Type: Edge Case*

* **Given** a `TradeInstrument` with `instrumentId = "LC_TEST_003"`
* **And** a `TradeInstrumentParty` record exists: `(LC_TEST_003, TP_ADVISING_BANK, CITI_BANK_001)`
* **When** `assign#InstrumentParty` is called with `(LC_TEST_003, TP_ADVISING_BANK, JPM_BANK_001)`
* **Then** the existing junction record is **updated** to `partyId = JPM_BANK_001`
* **And** only ONE `TradeInstrumentParty` record exists for `(LC_TEST_003, TP_ADVISING_BANK)`

#### Scenario BDD-CMN-TP-07: Reject commercial party assigned to bank role
**US-TP-02 | FR-TP-03, FR-TP-11**
*Type: Edge Case*

* **Given** a `TradeInstrument` with `instrumentId = "LC_TEST_004"`
* **And** a commercial `TradeParty` "ACME_CORP_001" (PARTY_COMMERCIAL)
* **When** `assign#InstrumentParty` is called with `(LC_TEST_004, TP_ADVISING_BANK, ACME_CORP_001)`
* **Then** the service returns a validation error: "Role TP_ADVISING_BANK requires a Bank party."
* **And** no junction record is created

#### Scenario BDD-CMN-TP-08: Reject advising bank without RMA
**US-TP-03 | FR-TP-12**
*Type: Edge Case*

* **Given** a bank `TradeParty` "NO_RMA_BANK" with `hasActiveRMA = "N"`
* **And** a `TradeInstrument` "LC_TEST_005"
* **When** `assign#InstrumentParty` is called with `(LC_TEST_005, TP_ADVISING_BANK, NO_RMA_BANK)`
* **Then** the service returns a validation error: "Advising Bank (MT700 Receiver) must have active RMA with the Issuing Bank."
* **And** no junction record is created

#### Scenario BDD-CMN-TP-09: Allow advise through bank without RMA
**US-TP-03 | FR-TP-12**
*Type: Happy Path*

* **Given** a bank `TradeParty` "NO_RMA_BANK" with `hasActiveRMA = "N"`, `kycStatus = "Active"`
* **And** a `TradeInstrument` "LC_TEST_006"
* **When** `assign#InstrumentParty` is called with `(LC_TEST_006, TP_ADVISE_THROUGH_BANK, NO_RMA_BANK)`
* **Then** the junction record is created successfully
* **And** no validation error is returned

#### Scenario BDD-CMN-TP-10: Reject reimbursing bank without Nostro account
**US-TP-03 | FR-TP-12**
*Type: Edge Case*

* **Given** a bank `TradeParty` "NO_NOSTRO_BANK" with `nostroAccountRef = null`
* **When** `assign#InstrumentParty` is called with `(LC_TEST_007, TP_REIMBURSING_BANK, NO_NOSTRO_BANK)`
* **Then** the service returns a validation error: "Cannot designate as Reimbursing Bank: No active Nostro account found."

#### Scenario BDD-CMN-TP-11: Reject confirming bank with insufficient FI limit
**US-TP-04 | FR-TP-12**
*Type: Edge Case*

* **Given** a bank `TradeParty` "SMALL_LIMIT_BANK" with `fiLimitAvailable = 100000.00`, `fiLimitCurrencyUomId = "USD"`
* **And** a `TradeInstrument` "LC_TEST_008" with `amount = 500000.00`, `tolerancePositive = 0.10`
* **When** `assign#InstrumentParty` is called with `(LC_TEST_008, TP_CONFIRMING_BANK, SMALL_LIMIT_BANK)`
* **Then** the service returns a validation error: "Confirming Bank's FI limit (100,000.00 USD) is insufficient for instrument liability (550,000.00 USD)."

#### Scenario BDD-CMN-TP-12: Create LC with party role assignments
**US-TP-05 | FR-TP-13**
*Type: Happy Path*

* **Given** the following parties exist:
  - "ACME_CORP_001" (PARTY_COMMERCIAL)
  - "GLOBAL_EXP_002" (PARTY_COMMERCIAL)
  - "ADV_BANK_001" (PARTY_BANK, hasActiveRMA=Y)
* **When** `create#ImportLetterOfCredit` is called with:
  - Standard LC fields (amount, currency, dates, etc.)
  - `parties: [{roleEnumId: "TP_APPLICANT", partyId: "ACME_CORP_001"}, {roleEnumId: "TP_BENEFICIARY", partyId: "GLOBAL_EXP_002"}, {roleEnumId: "TP_ADVISING_BANK", partyId: "ADV_BANK_001"}]`
* **Then** a `TradeInstrument` is created
* **And** an `ImportLetterOfCredit` is created
* **And** three `TradeInstrumentParty` junction records are created
* **And** no flat BIC or party name fields exist on the instrument or LC records

#### Scenario BDD-CMN-TP-13: Select "ANY BANK" for Available With
**US-TP-05 | FR-TP-09**
*Type: Happy Path*

* **Given** an `ImportLetterOfCredit` "LC_TEST_010" exists
* **When** the Maker selects "ANY BANK" for the Available With field
* **Then** `ImportLetterOfCredit.availableWithEnumId = "AVAIL_ANY_BANK"`
* **And** no `TP_NEGOTIATING_BANK` junction record exists for "LC_TEST_010"
* **And** SWIFT Tag 41a generates as: `:41a:ANY BANK`

#### Scenario BDD-CMN-TP-14: Switch from specific negotiating bank to "ANY BANK"
**US-TP-05 | FR-TP-09**
*Type: Edge Case*

* **Given** an `ImportLetterOfCredit` "LC_TEST_011" exists
* **And** `availableWithEnumId = "AVAIL_SPECIFIC_BANK"`
* **And** a `TradeInstrumentParty` record exists: `(LC_TEST_011, TP_NEGOTIATING_BANK, CITI_BANK_001)`
* **When** the Maker changes Available With to "ANY BANK"
* **Then** `ImportLetterOfCredit.availableWithEnumId` is updated to `"AVAIL_ANY_BANK"`
* **And** the `TP_NEGOTIATING_BANK` junction record is **removed**

#### Scenario BDD-CMN-TP-15: Submit LC validates mandatory roles
**US-TP-05 | FR-TP-13**
*Type: Edge Case*

* **Given** an `ImportLetterOfCredit` "LC_TEST_012" exists
* **And** only `TP_APPLICANT` is assigned (no beneficiary)
* **When** `submit#ForApproval` is called for "LC_TEST_012"
* **Then** the service returns a validation error listing missing mandatory roles
* **And** the error includes: "Mandatory role TP_BENEFICIARY is not assigned."
* **And** the instrument status is NOT changed to submitted

#### Scenario BDD-CMN-TP-16: Submit LC validates all party eligibility
**US-TP-05 | FR-TP-13**
*Type: Edge Case*

* **Given** an `ImportLetterOfCredit` "LC_TEST_013" with all mandatory parties assigned
* **And** the `TP_ADVISING_BANK` party has `kycStatus = "Expired"`
* **When** `submit#ForApproval` is called for "LC_TEST_013"
* **Then** the service returns a validation error: "Party KYC status is expired." for the Advising Bank
* **And** the instrument status is NOT changed to submitted

*   **And** party names are resolved through `TradeInstrumentParty` junction joins, not flat fields

#### Scenario BDD-CMN-TP-18: Role uniqueness enforcement
**US-TP-02 | FR-TP-18**
*Type: Edge Case*

* **Given** an `ImportLetterOfCredit` "LC_TEST_UNIQUE_01"
* **And** a `TradeInstrumentParty` record already exists for `(LC_TEST_UNIQUE_01, TP_APPLICANT, ACME_CORP_001)`
* **When** a user attempts to manually insert a SECOND record for `(LC_TEST_UNIQUE_01, TP_APPLICANT, GLOBAL_EXP_002)`
* **Then** the database must throw a Primary Key constraint violation
* **And** only ONE party remains assigned to the `TP_APPLICANT` role for this instrument

---

### Feature 2: Credit Facility & Limit Dashboard

#### Scenario BDD-CMN-FAC-01: Facility limit earmark
**US-LIM-01 | REQ-COM-ENT-03**
*Type: Happy Path*

* **Given** a `CustomerFacility` entity designated "FAC-ACME-001"
* **And** the recorded stats evaluate to:
  | Field | Value |
  | Total Approved Limit | 5,000,000 USD |
  | Previous Utilized Amount | 1,000,000 USD |
* **When** an application executes a synchronous limit earmark for exactly 50,000 USD
* **Then** the facility instantly resolves to the new metrics:
  | Field | Evaluated Result |
  | Available Earmark | 3,950,000 USD |
  | Utilized Amount | 1,050,000 USD |

#### Scenario BDD-CMN-FAC-02: Expired facility block
**US-LIM-01 | REQ-COM-ENT-03**
*Type: Edge Case*

* **Given** a `CustomerFacility` entity designated "FAC-OLD-001"
* **And** the Facility Expiry Date is recorded as "2023-12-01"
* **When** an application requests an earmark confirmation, regardless of Available Balance
* **Then** the Facility Manager rejects the request:
  | Target Limit Engine Response | Status |
  | Allocation Permitted | False |
  | Exception Detail | 'Credit Facility completely Expired' |

#### Scenario BDD-CMN-FAC-03: Facility 95% threshold warning
**US-LIM-03 | REQ-COM-NOT-01**
*Type: Happy Path*

* **Given** a transaction actively deducts from `FAC-ACME-002` (Total Limit: `1,000,000 USD`)
* **When** the final limit offset places the `Utilized Amount` squarely at `960,000 USD`
* **Then** the system triggers the alert payload module logically for the risk manager group:
  | Condition (960k / 1M) | Active Trigger Result Metric |
  | Evaluated Coverage > 95% | True |
  | Alert Queued | Facility Overutilization Group Email |

---

### Feature 3: Unified Transaction Lifecycle

#### Scenario BDD-CMN-TXN-01: Trade transaction base attributes and maker tracking
**US-TXN-01 | REQ-COM-ENT-01**
*Type: Happy Path*

* **Given** a new trade transaction is initialized by the Maker
* **When** the service `create#ImportLetterOfCredit` executes
* **Then** the `TradeInstrument` record is created with transaction management fields populated:
  | Field | Expected Value |
  | `transactionRef` | Not Null (auto-generated `TF-IMP-YY-NNNN`) |
  | `lifecycleStatusId` | `INST_PRE_ISSUE` |
  | `transactionStatusId` | `TRANS_DRAFT` |
  | `makerUserId` | Current authenticated user ID |
  | `makerTimestamp` | Current system timestamp |
  | `versionNumber` | `1` |
  | `transactionDate` | Current business date |
  | `checkerUserId` | Null |
  | `checkerTimestamp` | Null |

#### Scenario BDD-CMN-TXN-02: Transaction version increment on authorization
**US-TXN-01 | REQ-COM-ENT-01**
*Type: Happy Path*

* **Given** a `TradeInstrument` with `versionNumber = 1` and `transactionStatusId = TRANS_SUBMITTED`
* **When** a Checker authorizes the transaction via `authorize#Instrument`
* **Then** the transaction management fields are updated:
  | Field | Expected Value |
  | `checkerUserId` | The Checker's authenticated user ID |
  | `checkerTimestamp` | Current system timestamp |
  | `transactionStatusId` | `TRANS_APPROVED` |
  | `versionNumber` | Unchanged (`1`) |
* **And** when a subsequent Amendment is authorized against this LC
* **Then** the `versionNumber` increments to `2`

#### Scenario BDD-CMN-TXN-03: Valid party KYC acceptance
**US-TXN-01 | REQ-COM-ENT-02**
*Type: Happy Path*

* **Given** a `TradeParty` Directory query against Applicant "Acme Corp"
* **And** the KYC Status logic evaluator returns "Active"
* **When** the Maker links the Applicant ID to the transaction
* **Then** the Entity mapper accepts the link without error flags:
  | Target Entity Link | System Response |
  | Party Role assigned Applicant | Success |

#### Scenario BDD-CMN-TXN-04: Expired party KYC rejection
**US-TXN-01 | REQ-COM-ENT-02**
*Type: Edge Case*

* **Given** a `TradeParty` Directory query against Applicant "Bad Corp"
* **And** the Party's KYC Expiry Date is "2026-01-01" (in the past)
* **When** a user actively attempts to associate this party to a target transaction
* **Then** the transaction enforces the Directory constraints natively:
  | Constraint Target | System Action |
  | Maker Save Request | Blocked - Exception Thrown |
  | Exception Alert Value | 'Party KYC status is expired.' |

#### Scenario BDD-CMN-TXN-05: Processing flow: Draft to Submitted dual-status
**US-TXN-01 | REQ-COM-WF-01**
*Type: Happy Path*

* **Given** a transaction with `transactionStatusId = TRANS_DRAFT` and `lifecycleStatusId = INST_PRE_ISSUE`
* **And** all pre-processing Validations have sequentially evaluated to True
* **When** the Maker explicitly activates submission via `submit#ForApproval`
* **Then** the dual-status model updates independently:
  | Status Dimension | Before | After |
  | `transactionStatusId` (processing) | `TRANS_DRAFT` | `TRANS_SUBMITTED` |
  | `lifecycleStatusId` (system) | `INST_PRE_ISSUE` | `INST_PENDING_APPROVAL` |
  | `makerTimestamp` | Updated to current timestamp |
* **And** the `ImportLetterOfCredit.businessStateId` remains `LC_DRAFT`

#### Scenario BDD-CMN-TXN-06: Transaction-primary: New LC creates issuance transaction
**US-TXN-01 | REQ-TXN-01.1**
*Type: Happy Path*

* **Given** a user is completing the "New Import LC" form
* **When** they click "Submit for Approval"
* **Then** the frontend makes a POST request to `/rest/s1/trade/transactions`
* **And** the request payload includes:
  | Field | Value |
  | `transactionTypeEnumId` | `TXNT_ISSUANCE` |
  | `instrumentTypeEnumId` | `INST_IMPORT_LC` |
  | `priorityEnumId` | (User selected priority) |
* **And** the backend returns a `transactionId` which is then used to redirect the user

#### Scenario BDD-CMN-TXN-07: Checker authorizes by transactionId
**US-TXN-02 | REQ-TXN-01.2**
*Type: Happy Path*

* **Given** a Checker is reviewing an approval item with `transactionId = TXN-001`
* **When** the Checker clicks "Authorize"
* **Then** the frontend calls `tradeApi.authorize(transactionId: "TXN-001")`
* **And** the call hits the backend service `authorize#TradeTransaction`
* **And** the `TradeInstrument` state is updated by the backend as a cross-entity side effect

#### Scenario BDD-CMN-TXN-08: Decoupled data: View pending amendment proposed snapshot
**US-TXN-01 | REQ-TXN-02**
*Type: Happy Path*

* **Given** an LC exists with `amount = 500,000`
* **And** a pending Amendment transaction (v.2) exists with `amount = 550,000`
* **When** a Checker views the transaction details for the Amendment
* **Then** the UI displays the "Proposed Snapshot":
  | Display Field | Value | Style |
  | `Current Amount` | `500,000.00` | Standard |
  | `Proposed Amount`| `550,000.00` | Highlighted (Blue) |

#### Scenario BDD-CMN-TXN-09: Dual-status clarity: Workflow vs lifecycle states
**US-TXN-03 | REQ-UTN-05**
*Type: Happy Path*

* **Given** an Import LC is already `Issued` (Lifecycle State)
* **And** a partial Drawing presentation is currently `Pending Approval` (Workflow State)
* **When** a user views the Instrument Header
* **Then** the header displays both states:
  | UI Element | Value |
  | `Business State` | `Issued` |
  | `Action Status` | `Pending Approval` |
* **And** the visual "Global Status" badge uses the color for `Pending Approval`

---

### Feature 4: Authority Tiers & Data Access

#### Scenario BDD-CMN-AUTH-01: Tier enforcement by equivalent amount
**US-UIA-01 | REQ-COM-AUTH-01**
*Type: Happy Path*

* **Given** an input parameter targets an instrument transaction containing 70,000 USD Base Equivalent
* **When** the queue determines proper visibility logic
* **Then** the system enforces visibility solely to mapped Tier 1 Officers or strictly higher.
  | Required Matrix Limit Mapping | Condition Passed |
  | Tier 1 Limit Map ($100K) | Valid |

#### Scenario BDD-CMN-AUTH-02: Tier 4 dual checker enforcement
**US-UIA-02 | REQ-COM-AUTH-02**
*Type: Edge Case*

* **Given** the `TradeInstrument.baseEquivalentAmount` computes at `8,000,000 USD` (mapped Tier 4 equivalent)
* **When** the Maker pushes to the Authorization gateway and one Tier 4 member executes `Approve`
* **Then** the workflow transitions to `INST_PARTIAL_APPROVAL` (not `INST_AUTHORIZED`)
* **And** a `TradeApprovalRecord` is created with `approvalSequence = 1`
* **And** the transaction reappears in the Checker queue for a second distinct Tier 4 user
* **And** the second Checker must be different from both the Maker (`makerUserId`) and the first Checker
* **And** only after the second approval does `lifecycleStatusId` transition to `INST_AUTHORIZED`

#### Scenario BDD-CMN-AUTH-03: Amendment total liability route determination
**US-UIA-01 | REQ-COM-AUTH-03A**
*Type: Happy Path*

* **Given** an `ImportLetterOfCredit` with `effectiveAmount = 900,000 USD`
* **And** an Amendment increases the `effectiveAmount` by `150,000 USD` (new total: `1,050,000 USD`)
* **When** the system recalculates the limit bounds against the Maker-Checker matrix tier framework
* **Then** it uses the **new `effectiveAmount` (1,050,000 USD)** for tier routing — not the isolated $150k delta
* **And** this routes to Tier 3 (>$1M) instead of Tier 1

#### Scenario BDD-CMN-AUTH-04: Compliance route overrides financial route
**US-UIA-01 | REQ-COM-AUTH-03C**
*Type: Edge Case*

* **Given** an instrument has triggered `check#Sanctions` returning `isHit = true`
* **And** `lifecycleStatusId` has been set to `INST_HOLD`
* **When** the Checker matrix evaluates conditional checks
* **Then** standard Financial Queue authorization is blocked until `release#ComplianceHold` is invoked by a `TRADE_COMPLIANCE_OFFICER`

#### Scenario BDD-CMN-AUTH-05: Priority queue ordering
**US-UIA-01 | REQ-COM-AUTH-01**
*Type: Happy Path*

* **Given** two transactions in the Checker queue: TX-A with `priorityEnumId = NORMAL` and TX-B with `priorityEnumId = URGENT`
* **When** the Checker queue list is rendered
* **Then** TX-B appears above TX-A regardless of submission timestamp

#### Scenario BDD-CMN-AUTH-06: Segregation of duties prevention
**US-UIA-03 | REQ-COM-VAL-02**
*Type: Edge Case*

* **Given** a specific authenticated user `USER_MAKER_XX` was logged as the `makerUserId` of `TradeInstrument` for transaction `TF-IMP-001`
* **When** identical authenticated subject `USER_MAKER_XX` subsequently opens the Authorization view for `TF-IMP-001`
* **Then** the visual context disables manual progress vectors entirely:
  | Target UI Logic Vector | Security Outcome Applied |
  | Authorization Interface Buttons | Read-only / disabled |
  | Endpoint Direct Auth Call Payload | Refused via Auth Middleware |

#### Scenario BDD-CMN-AUTH-07: Suspended account exclusion
**US-UIA-04 | REQ-COM-MAS-02**
*Type: Edge Case*

* **Given** a user agent identity natively exists inside a Tier 3 authorization matrix in `UserAuthorityProfile`
* **When** an HR automation explicitly updates `UserAuthorityProfile.isSuspended` to `Y`
* **Then** global lists explicitly eliminate user visibility from Maker queues and authorization assignments

#### Scenario BDD-CMN-AUTH-08: Successful login
**US-AUTH-01 | FR-AUTH-02**
*Type: Happy Path*

* **Given** the user is on the Login page
* **When** they enter a valid username "trade.maker" and password "trade123"
* **And** click "Sign In"
* **Then** they should be redirected to the "Operations Dashboard"
* **And** the sidebar should display "Trade Maker" (firstName + lastName)

#### Scenario BDD-CMN-AUTH-09: Failed login generic error
**US-AUTH-01 | FR-AUTH-04**
*Type: Edge Case*

* **Given** the user is on the Login page
* **When** they enter an invalid username or wrong password
* **And** click "Sign In"
* **Then** they should remain on the Login page
* **And** see an error message "Invalid username or password"

#### Scenario BDD-CMN-AUTH-10: Account lockout after 3 failures
**US-AUTH-01 | FR-AUTH-05**
*Type: Edge Case*

* **Given** the user has entered a wrong password 3 times
* **When** they attempt a 4th login
* **Then** they should see an error message "Account temporarily locked. Try again later."

#### Scenario BDD-CMN-AUTH-11: Unauthorized access redirect
**US-AUTH-01 | FR-AUTH-01**
*Type: Edge Case*

* **Given** the user is not logged in
* **When** they try to navigate directly to "/transactions"
* **Then** they should be redirected to "/login"

#### Scenario BDD-CMN-AUTH-12: View own profile
**US-AUTH-02 | FR-PROF-01**
*Type: Happy Path*

* **Given** the user "trade.maker" is logged in
* **When** they click their avatar in the sidebar footer
* **Then** a profile overlay/panel should appear
* **And** it should show "Trade Maker (trade.maker)"
* **And** it should show roles: "Trade Maker"
* **And** it should show Delegation Tier: "Tier 1"

#### Scenario BDD-CMN-AUTH-13: User changes own password
**US-AUTH-02 | FR-PROF-03**
*Type: Happy Path*

* **Given** the user is logged in and viewing their profile
* **When** they enter their current password, a valid new password, and confirm it
* **And** click "Update Password"
* **Then** they should see a success notification "Password updated successfully"
* **And** their server-side password should be updated

#### Scenario BDD-CMN-AUTH-14: User logs out
**US-AUTH-02 | FR-AUTH-06**
*Type: Happy Path*

* **Given** a user is logged in
* **When** they click "Logout" in the profile panel
* **Then** their server session should be invalidated
* **And** they should be redirected to the Login page

#### Scenario BDD-CMN-AUTH-15: Admin creates new user
**US-AUTH-03 | FR-ADM-04**
*Type: Happy Path*

* **Given** an administrator is on the "User Administration" page
* **When** they click "+ New User"
* **And** enter "john.doe", "John", "Doe", "john@bank.com", and an initial password
* **And** click "Create User"
* **Then** the new user "John Doe" should appear in the user list
* **And** they should have no roles assigned by default

#### Scenario BDD-CMN-AUTH-16: Admin assigns roles to user
**US-AUTH-03 | FR-ADM-07**
*Type: Happy Path*

* **Given** the Admin is editing user "john.doe"
* **When** they check the "Trade Maker" and "Trade Checker" roles
* **And** click "Save Changes"
* **Then** the user "john.doe" should now be a member of "TRADE_MAKER" and "TRADE_CHECKER" groups

#### Scenario BDD-CMN-AUTH-17: Admin resets user password
**US-AUTH-03 | FR-ADM-06**
*Type: Happy Path*

* **Given** the Admin is editing user "trade.maker"
* **When** they enter a new password in the "Reset Password" section
* **And** click "Reset Password"
* **Then** the user's password should be updated immediately without requiring their old password

#### Scenario BDD-CMN-AUTH-18: Admin disables account
**US-AUTH-03 | FR-ADM-09**
*Type: Happy Path*

* **Given** an active user "trade.maker" exists
* **When** the Admin toggles the "Account Status" to Disabled for "trade.maker"
* **Then** "trade.maker" should be immediately disconnected
* **And** unable to log in again until re-enabled

#### Scenario BDD-CMN-AUTH-19: Admin cannot disable own account
**US-AUTH-03 | FR-ADM-10**
*Type: Edge Case*

* **Given** "trade.admin" is managing users
* **When** they view the details for "trade.admin"
* **Then** the "Account Status" toggle should be disabled (preventing self-lockout)

#### Scenario BDD-CMN-AUTH-20: Immutability prevents active record modification
**US-TXN-01 | REQ-COM-VAL-02**
*Type: Edge Case*

* **Given** an `ImportLetterOfCredit` evaluates `businessStateId = LC_ISSUED`
* **When** a user agent initiates a raw `PUT` standard core document update targeting a financial parameter (e.g., `effectiveAmount`)
* **Then** the system natively intercepts the action and demands an explicit event payload:
  | Update Evaluation Route | Exception Status |
  | Modification Method Evaluated | Bypassed. Formal Amendment Process Requested. |

---

### Feature 5: Product Configuration

#### Scenario BDD-CMN-PRD-01: Active component verification
**US-PRD-01 | REQ-COM-PRD-01**
*Type: Happy Path*

* **Given** an administrator effectively configured `"SBLC_COMM"` `isActive` property to `N` in `TradeProductCatalog`
* **When** a user hits the New Application rendering sequence view
* **Then** the component completely ignores rendering the target definition options over dropdown inputs

#### Scenario BDD-CMN-PRD-02: Allowed tenor sight restriction
**US-PRD-01 | REQ-COM-PRD-01**
*Type: Edge Case*

* **Given** a `TradeProductCatalog` entry maps `allowedTenorEnumId` exclusively to `SIGHT_ONLY`
* **When** an application input passes a parameter payload asserting `tenorTypeId: USANCE`
* **Then** the core logic throws a validation assertion denying progress against the product configuration

#### Scenario BDD-CMN-PRD-03: Tolerance limit ceiling check
**US-PRD-02 | REQ-COM-PRD-01**
*Type: Edge Case*

* **Given** a `TradeProductCatalog` specifies `maxToleranceLimit` explicitly to `10`
* **When** the user explicitly attempts to input a positive tolerance value of `25%`
* **Then** the system automatically generates an explicit UI exception denying values exceeding the product limit matrix configuration

#### Scenario BDD-CMN-PRD-04: Display revolving fields rule
**US-PRD-01 | REQ-COM-PRD-01**
*Type: Happy Path*

* **Given** a `TradeProductCatalog` specifies `allowRevolving = Y`
* **When** the framework generates the application entry screen
* **Then** the module injects form objects supporting "Reinstatement parameters" directly to the client screen output dynamically

#### Scenario BDD-CMN-PRD-05: Advance payment document avoidance
**US-PRD-01 | REQ-COM-PRD-01**
*Type: Happy Path*

* **Given** the `TradeProductCatalog` records `allowAdvancePayment = Y` (typically Red Clause LCs)
* **When** the Beneficiary invokes a pre-shipment documentation presentation against the system workflow
* **Then** the logic evaluator bypasses typical standard UCP transportation document validations natively to accept a simple receipt input matrix automatically

#### Scenario BDD-CMN-PRD-06: Standby routing path rule
**US-PRD-01 | REQ-COM-PRD-01**
*Type: Happy Path*

* **Given** the `TradeProductCatalog` evaluates `isStandby = Y`
* **When** the core evaluates normal presentation behaviors and standard 5-day checks
* **Then** the system natively switches workflow tracks utilizing local Guarantee processing mechanics

#### Scenario BDD-CMN-PRD-07: Transferable instructions render
**US-PRD-01 | REQ-COM-PRD-01**
*Type: Happy Path*

* **Given** a `TradeProductCatalog` maps `isTransferable = Y`
* **When** the user actively parses the data
* **Then** the application triggers the inclusion of a specific "Transfer Instructions" tab inherently visible across Maker interface vectors

#### Scenario BDD-CMN-PRD-08: Islamic ledger classification
**US-PRD-01 | REQ-COM-PRD-01**
*Type: Happy Path*

* **Given** the `TradeProductCatalog.accountingFrameworkEnumId` is set to `ISLAMIC`
* **When** the backend prepares physical accounting vouchers effectively for fee allocations
* **Then** the system forcibly routes computations relying on "Profit Rates" bypassing "Interest Rates" entirely mapping to distinct Islamic GL arrays

#### Scenario BDD-CMN-PRD-09: Mandatory margin prerequisite
**US-PRD-02 | REQ-COM-PRD-01**
*Type: Edge Case*

* **Given** the `TradeProductCatalog.mandatoryMarginPercent` is set to `100`
* **When** the Maker pushes submission mechanics towards the system
* **Then** the framework absolutely denies validation assertions if evaluating standard unsecured credit facilities, natively forcing identical 100% equivalent holds over local user deposits

#### Scenario BDD-CMN-PRD-10: Custom SLA deadline formula
**US-PRD-01 | REQ-COM-PRD-01**
*Type: Happy Path*

* **Given** the `TradeProductCatalog.documentExamSlaDays` is configured to `2` instead of global standard 5
* **When** a document logs to the system successfully
* **Then** the background timer establishes the hard escalation limit calculated around the `2` custom mapped property days dynamically

#### Scenario BDD-CMN-PRD-11: Default SWIFT MT generation
**US-PRD-01 | REQ-COM-PRD-01**
*Type: Happy Path*

* **Given** the `TradeProductCatalog.defaultSwiftFormatEnumId` is set to `MT760`
* **When** the standard process triggers the authorization routines dispatching automated message frameworks
* **Then** the engine automatically routes the payload data against the MT760 standard definitions completely ignoring default MT700 structures

#### Scenario BDD-CMN-PRD-12: Tariff matrix priority overrides
**US-FEE-01 | REQ-COM-MAS-01**
*Type: Happy Path*

* **Given** a designated standard configuration calculates product issuance effectively at `0.20%`
* **And** the specialized customer explicitly possesses a Customer Tier rule assigning `0.10%` to Issuance
* **When** the Tariff engine collects fee structure data points upon action termination
* **Then** the engine overrides the default natively honoring specific customer tier mappings primarily before applying math

| Target Value Applied Against Ledgers |
| $50 USD |

#### Scenario BDD-CMN-FEE-01: Customer exception rates
**US-FEE-02 | US-FEE-02**
*Type: Happy Path*

* **Given** a standard fee configuration for `ISSUANCE_FEE` at `0.25%`
* **And** a customer "TEST_CUSTOMER_001" has a negotiated override rate at `0.10%`
* **When** `calculate#Fees` is called for "TEST_CUSTOMER_001" with a base amount of `$100,000`
* **Then** the result evaluates to `$100.00` (0.10%)
* **And** the standard rate (0.25% = $250) is ignored

---

### Feature 6: Audit Logs & Narrative Timeline

#### Scenario BDD-CMN-AUD-01: Full narrative rendering
**US-UTN-01 | REQ-UTN-01**
*Type: Happy Path*

* **Given** a Trade Instrument (Import LC) has the following history:
  1. Issuance Transaction (Authorized)
  2. SWIFT MT700 (ACKed by Network)
  3. Amendment Transaction (Pending Approval)
* **When** a user views the "Unified Narrative" page for this instrument
* **Then** the timeline renders 4 nodes in reverse chronological order:
  | Node Type | Source | Status |
  | `Transaction` | Amendment | `Pending Approval` (Highlighted) |
  | `Audit` | SWIFT MT700 | `ACKed` |
  | `System` | Issuance Advice | `Sent` |
  | `Transaction` | Issuance | `Authorized` |

#### Scenario BDD-CMN-AUD-02: Timeline: Checker authorizes pending node
**US-UTN-02 | REQ-UTN-02.1**
*Type: Happy Path*

* **Given** an Amendment transaction exists in `Pending Approval`
* **And** the current user has the `Checker` role with a sufficient financial tier
* **When** the Checker clicks the "Authorize" button directly on the timeline node
* **Then** the `authorize#TradeTransaction` service is invoked with the `transactionId`
* **And** the timeline node updates its status to `Authorized` (Green)
* **And** a new `System` event for "SWIFT MT707 Generation" appears above it

#### Scenario BDD-CMN-AUD-03: Timeline: Rejection with reason display
**US-UTN-02 | REQ-UTN-02.3**
*Type: Edge Case*

* **Given** an Amendment transaction in `Pending Approval`
* **When** a Checker clicks "Reject" on the timeline node and enters "Invalid Expiry Date"
* **Then** the transaction state shifts to `Draft` (or `Rejected`)
* **And** the timeline node displays a sub-item: "Rejection Reason: Invalid Expiry Date"
* **And** an "Edit" button becomes available on the node for the Maker

#### Scenario BDD-CMN-AUD-04: Delta analysis: Amendment differences
**US-UTN-03 | REQ-UTN-03.1**
*Type: Happy Path*

* **Given** an Amendment (v.2) exists for an LC where the `amount` was increased from `500,000` to `550,000`
* **When** a user clicks "View Diff" on the Amendment node in the timeline
* **Then** a side-panel or modal appears showing:
  | Field | Old Value | New Value | Change |
  | `LC Amount` | `500,000.00` | `550,000.00` | `+ 50,000.00` |

#### Scenario BDD-CMN-AUD-05: Mandatory transaction delta audit log
**US-UTN-01 | REQ-COM-MAS-03**
*Type: Happy Path*

* **Given** a save request commits a state-modifying delta against the physical DB
* **When** the save commit formally finalizes inside the application structure layer
* **Then** an explicit Append-Only `TradeTransactionAudit` record logs:
  | Target Audit Key Attributes | Existence |
  | `snapshotDeltaJSON` (Before/After) | Checked |
  | `userId` | Checked |
  | `timestamp` | Checked |
  | `ipAddress` | Checked |
  | `fieldChanged` | Checked |

#### Scenario BDD-CMN-AUD-06: Sanctions check triggers hold
**US-UTN-01 | REQ-COM-NOT-02**
*Type: Edge Case*

* **Given** an operation applies a new Target Party to the instrument ("Banned Corp")
* **When** the validation gateway invokes instantaneous sanctions analysis and returns `Hit`
* **Then** standard execution routes are replaced immediately:
  | Event Hook Route | Outcome Target |
  | `lifecycleStatusId` | `INST_HOLD` |
  | `transactionStatusId` | Unchanged (remains current processing state) |
  | Compliance Queue Alert | Dispatched True |

---

### Feature 7: FX, SLA & Notifications

#### Scenario BDD-CMN-FX-01: Precision: Zero decimal JPY
**US-FX-01 | REQ-COM-FX-01**
*Type: Edge Case*

* **Given** an amount evaluation sequence targeting `10050.50`
* **And** the mapped Currency base is designated as `JPY` (ISO standard 0 decimals)
* **When** the universal rounding routine is invoked over the transaction array
* **Then** the result strictly aligns with zero-decimal precision standards:
  | Raw Amount | Truncated Legal Value (or rounded per param) |
  | 10050.50 | 10051 (rounded standard) |

#### Scenario BDD-CMN-FX-02: Precision: 2 decimals USD
**US-FX-01 | REQ-COM-FX-01**
*Type: Happy Path*

* **Given** an amount evaluation sequence targeting `5200.125`
* **And** the mapped Currency base is designated as `USD` (ISO standard 2 decimals)
* **When** the standard rounding routine applies scaling parameters
* **Then** the system maintains exactly two decimal nodes unconditionally:
  | Raw Amount | Evaluated Legal Value |
  | 5200.125 | 5200.13 |

#### Scenario BDD-CMN-FX-03: Daily board rate for limit consumption
**US-FX-01 | REQ-COM-FX-02**
*Type: Happy Path*

* **Given** a base equivalent query required to allocate limits in USD
* **And** the working instrument specifies an original payload in EUR
* **When** the core FX resolver asks for identical conversions throughout the working day
* **Then** the system locks consumption conversions to the cached Board Rate values:
  | Board Rate Param Context | Resolution Value Example |
  | Selected Source Table | DAILY_CACHE |
  | Result for 100 EUR Query 1 | 105 USD |
  | Result for 100 EUR Query 2 | 105 USD |

#### Scenario BDD-CMN-FX-04: Live FX spread for settlement
**US-FX-01 | REQ-COM-FX-02**
*Type: Happy Path*

* **Given** the system is generating a Nostro Account funding sequence required to process an active cash settlement
* **And** the settlement bridges EUR into local USD equivalent physically
* **When** the FX resolver builds the remittance payload
* **Then** the resolver forces a live Treasury REST look-up exclusively:
  | Live Spot Target Table | Evaluation Result Rules |
  | Selected Source Context | LIVE_API_TREASURY_PROXY |
  | Uses Daily Cache? | False |

#### Scenario BDD-CMN-SLA-01: SLA timer skips holidays
**US-SLA-01 | REQ-COM-SLA-01**
*Type: Happy Path*

* **Given** the system generates an operational deadline set for purely `5 Banking Days`
* **And** the start time is a `Monday`, and a global Head-Office holiday strictly exists on the subsequent `Wednesday`
* **When** the global banking calendar formula applies
* **Then** the logic determines that `Wednesday`, `Saturday`, and `Sunday` do NOT compute against the total duration:
  | Day Additions Evaluated | M(1) T(2) W(0) Th(3) F(4) S(0) Su(0) M(5) |
  | Result Deadline Date | The following Monday |

#### Scenario BDD-CMN-SLA-02: Timer exhaustion generates block
**US-SLA-02 | REQ-COM-SLA-02**
*Type: Edge Case*

* **Given** a Document presentation is active and unassessed
* **And** the exact runtime spans past 5 Universal Banking Days since the start trigger
* **When** the continuous batch queue checks the document timer metrics
* **Then** the system forcibly applies an escalation label against the primary parent document to alert management:
  | Computed Day Difference | Enforced Evaluation Output Status |
  | 5 | Critical Blocking Exception Raised |

---

### Feature 9: Navigation & Search

#### Scenario BDD-CMN-NAV-01: Global transaction log
**US-NAV-02 | REQ-NAV-01.3**
*Type: Happy Path*

* **Given** three different instruments (LC-1, LC-2, SG-1) have had recent activity
* **When** a user navigates to the "Global Transaction Log"
* **Then** they see a table containing:
  | Row | Transaction ID | Instrument Ref | User | Status |
  | 1 | TXN-999 | LC-1 | Maker-A | `Pending` |
  | 2 | TXN-998 | SG-1 | Checker-B| `Executed` |
  | 3 | TXN-997 | LC-2 | Maker-A | `Draft` |

#### Scenario BDD-CMN-NAV-02: Contextual search toggle
**US-SRH-01 | REQ-SRH-01.1**
*Type: Happy Path*

* **Given** an LC exists with reference `TF-IMP-26-0001`
* **And** it has an active amendment `TXN-5561`
* **When** a user searches for `TF-IMP-26` in the "Instruments" context
* **Then** the search results show the `TF-IMP-26-0001` asset record.
* **When** the user clicks "Switch to Transactions"
* **Then** the search results show `TXN-5561` (Amendment).

#### Scenario BDD-CMN-NAV-03: Checker queue modal interaction
**US-NAV-02 | REQ-UI-CMN-02**
*Type: Happy Path*

* **Given** a Checker is viewing the global transaction log
* **When** the Checker clicks on an "URGENT" issuance transaction
* **Then** the application opens a full-screen modal overlay
* **And** the background dashboard remains visible but inactive
* **And** the modal displays all instrument details for authorization

#### Scenario BDD-CMN-SRH-01: Cross-reference indexing
**US-SRH-01 | REQ-SRH-01.2**
*Type: Happy Path*

* **Given** an Import LC "LC_SEARCH_001" exists
* **And** multiple transactions (Issuance, Amendment) are associated with "LC_SEARCH_001"
* **When** searching for "LC_SEARCH_001" with context "Transactions"
* **Then** the result set includes both the Issuance and the Amendment transaction IDs
* **And** the "Instrument" context still shows the legal master record
