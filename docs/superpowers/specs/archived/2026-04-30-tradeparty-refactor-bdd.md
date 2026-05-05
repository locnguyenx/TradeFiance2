# Behavior-Driven Development (BDD) Specification
**Project Name:** Digital Trade Finance Platform
**Module:** TradeParty Refactoring — Party-Role Junction Pattern
**Document Version:** 1.0
**Date:** April 30, 2026
**Source BRD:** `docs/superpowers/specs/2026-04-30-tradeparty-refactor-brd.md`

---

## Traceability Matrix

| Scenario | Type | User Story | Functional Requirement(s) |
|:---|:---|:---|:---|
| SC-01 | Happy | US-TP-01 | FR-TP-01, FR-TP-05, FR-TP-10 |
| SC-02 | Happy | US-TP-01 | FR-TP-02, FR-TP-05, FR-TP-10 |
| SC-03 | Edge | US-TP-01 | FR-TP-01, FR-TP-10 |
| SC-04 | Happy | US-TP-02 | FR-TP-03, FR-TP-04, FR-TP-11 |
| SC-05 | Happy | US-TP-02 | FR-TP-03, FR-TP-04, FR-TP-11 |
| SC-06 | Edge | US-TP-02 | FR-TP-03, FR-TP-11 |
| SC-07 | Edge | US-TP-02 | FR-TP-03, FR-TP-11 |
| SC-08 | Happy | US-TP-03 | FR-TP-12 |
| SC-09 | Edge | US-TP-03 | FR-TP-12 |
| SC-10 | Edge | US-TP-03 | FR-TP-12 |
| SC-11 | Edge | US-TP-03 | FR-TP-12 |
| SC-12 | Happy | US-TP-05 | FR-TP-13, FR-TP-06, FR-TP-07 |
| SC-13 | Happy | US-TP-05 | FR-TP-09 |
| SC-14 | Edge | US-TP-05 | FR-TP-09 |
| SC-15 | Happy | US-TP-05 | FR-TP-13 |
| SC-16 | Edge | US-TP-05 | FR-TP-13 |
| SC-17 | Happy | US-TP-04 | FR-TP-14 |

---

## Scenarios

### SC-01: Create a commercial trade party
**US-TP-01 | FR-TP-01, FR-TP-05, FR-TP-10**

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

---

### SC-02: Create a bank trade party with extension fields
**US-TP-01 | FR-TP-02, FR-TP-05, FR-TP-10**

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

---

### SC-03: Reject commercial party with invalid SWIFT characters
**US-TP-01 | FR-TP-01, FR-TP-10**

* **Given** no existing party
* **When** `create#TradeParty` is called with:
  - `partyTypeEnumId = PARTY_COMMERCIAL`
  - `partyName = "Acme & Sons Trading @Corp"`
  - `registeredAddress = "123 Main St"`
* **Then** the service returns a validation error
* **And** the error identifies invalid characters `&` and `@` in `partyName`
* **And** no `TradeParty` record is created

---

### SC-04: Assign parties to instrument roles
**US-TP-02 | FR-TP-03, FR-TP-04, FR-TP-11**

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

---

### SC-05: Assign same bank to multiple roles on one instrument
**US-TP-02 | FR-TP-03, FR-TP-04, FR-TP-11**

* **Given** a `TradeInstrument` with `instrumentId = "LC_TEST_002"`
* **And** a bank `TradeParty` "JPM_BANK_001" (PARTY_BANK) with `hasActiveRMA = "Y"`, `nostroAccountRef = "NOSTRO-JPM-001"`, `fiLimitAvailable = 5000000.00`
* **When** `assign#InstrumentParty` is called for:
  - `(LC_TEST_002, TP_ADVISING_BANK, JPM_BANK_001)`
  - `(LC_TEST_002, TP_CONFIRMING_BANK, JPM_BANK_001)`
  - `(LC_TEST_002, TP_REIMBURSING_BANK, JPM_BANK_001)`
* **Then** three `TradeInstrumentParty` junction records are created
* **And** all three records reference the same `partyId = JPM_BANK_001`
* **And** each record has a distinct `roleEnumId`

---

### SC-06: Reject duplicate role assignment on same instrument
**US-TP-02 | FR-TP-03, FR-TP-11**

* **Given** a `TradeInstrument` with `instrumentId = "LC_TEST_003"`
* **And** a `TradeInstrumentParty` record exists: `(LC_TEST_003, TP_ADVISING_BANK, CITI_BANK_001)`
* **When** `assign#InstrumentParty` is called with `(LC_TEST_003, TP_ADVISING_BANK, JPM_BANK_001)`
* **Then** the existing junction record is **updated** to `partyId = JPM_BANK_001`
* **And** only ONE `TradeInstrumentParty` record exists for `(LC_TEST_003, TP_ADVISING_BANK)`
* **And** no duplicate records are created

---

### SC-07: Reject assigning commercial party to bank role
**US-TP-02 | FR-TP-03, FR-TP-11**

* **Given** a `TradeInstrument` with `instrumentId = "LC_TEST_004"`
* **And** a commercial `TradeParty` "ACME_CORP_001" (PARTY_COMMERCIAL)
* **When** `assign#InstrumentParty` is called with `(LC_TEST_004, TP_ADVISING_BANK, ACME_CORP_001)`
* **Then** the service returns a validation error: "Role TP_ADVISING_BANK requires a Bank party."
* **And** no junction record is created

---

### SC-08: Reject advising bank without RMA
**US-TP-03 | FR-TP-12**

* **Given** a bank `TradeParty` "NO_RMA_BANK" with `hasActiveRMA = "N"`
* **And** a `TradeInstrument` "LC_TEST_005"
* **When** `assign#InstrumentParty` is called with `(LC_TEST_005, TP_ADVISING_BANK, NO_RMA_BANK)`
* **Then** the service returns a validation error: "Advising Bank (MT700 Receiver) must have active RMA with the Issuing Bank."
* **And** no junction record is created
* **Note:** Unlike the old rules, the presence of an Advise Through Bank does NOT waive this requirement. The Advising Bank is the direct message receiver and always needs RMA.

---

### SC-09: Allow advise through bank without RMA
**US-TP-03 | FR-TP-12**

* **Given** a bank `TradeParty` "NO_RMA_BANK" with `hasActiveRMA = "N"`, `kycStatus = "Active"`
* **And** a `TradeInstrument` "LC_TEST_006"
* **When** `assign#InstrumentParty` is called with `(LC_TEST_006, TP_ADVISE_THROUGH_BANK, NO_RMA_BANK)`
* **Then** the junction record is created successfully
* **And** no validation error is returned
* **Note:** The Advise Through Bank (Tag 57a) does not need RMA with the Issuing Bank. The relay RMA between the Advising Bank and the Advise Through Bank is outside the Issuing Bank's scope.

---

### SC-10: Reject reimbursing bank without Nostro account
**US-TP-03 | FR-TP-12**

* **Given** a bank `TradeParty` "NO_NOSTRO_BANK" with `nostroAccountRef = null`
* **When** `assign#InstrumentParty` is called with `(LC_TEST_007, TP_REIMBURSING_BANK, NO_NOSTRO_BANK)`
* **Then** the service returns a validation error: "Cannot designate as Reimbursing Bank: No active Nostro account found."

---

### SC-11: Reject confirming bank with insufficient FI limit
**US-TP-03 | FR-TP-12**

* **Given** a bank `TradeParty` "SMALL_LIMIT_BANK" with `fiLimitAvailable = 100000.00`, `fiLimitCurrencyUomId = "USD"`
* **And** a `TradeInstrument` "LC_TEST_008" with `amount = 500000.00`, `tolerancePositive = 0.10`
* **When** `assign#InstrumentParty` is called with `(LC_TEST_008, TP_CONFIRMING_BANK, SMALL_LIMIT_BANK)`
* **Then** the service returns a validation error: "Confirming Bank's FI limit (100,000.00 USD) is insufficient for instrument liability (550,000.00 USD)."

---

### SC-12: Create Import LC with party role assignments
**US-TP-05 | FR-TP-13, FR-TP-06, FR-TP-07**

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
* **And** the applicant's name is retrieved via junction: `TradeInstrumentParty(TP_APPLICANT) → TradeParty.partyName`

---

### SC-13: Select "ANY BANK" for Available With
**US-TP-05 | FR-TP-09**

* **Given** an `ImportLetterOfCredit` "LC_TEST_010" exists
* **When** the Maker selects "ANY BANK" for the Available With field
* **Then** `ImportLetterOfCredit.availableWithEnumId = "AVAIL_ANY_BANK"`
* **And** no `TP_NEGOTIATING_BANK` junction record exists for "LC_TEST_010"
* **And** SWIFT Tag 41a generates as: `:41a:ANY BANK` + the `availableByEnumId` method

---

### SC-14: Switch from specific negotiating bank to "ANY BANK"
**US-TP-05 | FR-TP-09**

* **Given** an `ImportLetterOfCredit` "LC_TEST_011" exists
* **And** `availableWithEnumId = "AVAIL_SPECIFIC_BANK"`
* **And** a `TradeInstrumentParty` record exists: `(LC_TEST_011, TP_NEGOTIATING_BANK, CITI_BANK_001)`
* **When** the Maker changes Available With to "ANY BANK"
* **Then** `ImportLetterOfCredit.availableWithEnumId` is updated to `"AVAIL_ANY_BANK"`
* **And** the `TP_NEGOTIATING_BANK` junction record is **removed**
* **And** no stale party reference remains

---

### SC-15: Submit LC validates mandatory roles
**US-TP-05 | FR-TP-13**

* **Given** an `ImportLetterOfCredit` "LC_TEST_012" exists
* **And** only `TP_APPLICANT` is assigned (no beneficiary)
* **When** `submit#ForApproval` is called for "LC_TEST_012"
* **Then** the service returns a validation error listing missing mandatory roles
* **And** the error includes: "Mandatory role TP_BENEFICIARY is not assigned."
* **And** the instrument status is NOT changed to submitted

---

### SC-16: Submit LC validates all party eligibility
**US-TP-05 | FR-TP-13**

* **Given** an `ImportLetterOfCredit` "LC_TEST_013" with all mandatory parties assigned
* **And** the `TP_ADVISING_BANK` party has `kycStatus = "Expired"`
* **When** `submit#ForApproval` is called for "LC_TEST_013"
* **Then** the service returns a validation error: "Party KYC status is expired." for the Advising Bank
* **And** the instrument status is NOT changed to submitted

---

### SC-17: View entity resolves party names from junction
**US-TP-04 | FR-TP-14**

* **Given** an `ImportLetterOfCredit` "LC_TEST_014" with:
  - `TP_APPLICANT → "ACME_CORP_001"` (partyName = "Acme Corp")
  - `TP_BENEFICIARY → "GLOBAL_EXP_002"` (partyName = "Global Exports Ltd")
  - `TP_ADVISING_BANK → "CITI_BANK_001"` (partyName = "Citibank London")
* **When** querying `ImportLetterOfCreditView` for "LC_TEST_014"
* **Then** the view returns `applicantPartyName = "Acme Corp"`
* **And** `beneficiaryPartyName = "Global Exports Ltd"`
* **And** party names are resolved through `TradeInstrumentParty` junction joins, not flat fields

