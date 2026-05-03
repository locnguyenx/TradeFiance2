# Phase 1 - Import LC
## TODO: 
- rework UI using docs/superpowers/specs/2026-04-21-ui-wireframes.md
- check all page to comply UI requirements
- check ImportLC entity for completeness

Next Phases:
- All MT messages for Import LC
- Enhancement for common module

## Enhancement

this is my suggestion related to SWIFT message generation and validation: 
* Because SWIFT has strict constraints regarding character limits, specific allowed character sets (the SWIFT "X" character set), and line breaks. We should implement the data formatting, validation, constraint in domain entity and data capturing for early prevention of SWIFT data/format violation. The checking in SWIFT generation services is still needed. But by do checking during data capturing we bring better user experience and operations.
* **ImportLetterOfCredit:** While a lot of data are available in TradeInstrument, but TradeInstrument has meaning of a snapshot or historical data. Hence, we should replicate these data in ImportLetterOfCredit to have latest effective LC values 
  - lifecycleStatusId
  - productEnumId:  LC
  - amount
  - outstandingAmount
  - baseEquivalentAmount
  - applicantPartyId
  - beneficiaryPartyId
  - issueDate
  - expiryDate
  - customerFacilityId

* **TradeInstrument:** 
  - Add field `transactionDate`, `transactionTypeEnumId`, `makerPartyId`, `checkerPartyId` , `makerTimestamp`, `checkerTimestamp`, `lastUpdateTimestamp`, `transactionStatusId`
  - more BDD cases for REQ-COM-WF-01

* **Common Module**
    - RBAC

## MT700 tags fixing
* Problematic document: docs/requirements/20260429-MT-special-tags-TradeParty.md
| Incorrect    | Correct     |
| :----------- | :---------- |
| Advising Bank (Tag 57a)  | Advise Through Bank (Tag 57A)     |
| RMA required for Advise Through Bank in Tag 57A| Not required RMA   |

58A is used to input confirming bank or Negotiating Bank

* docs/superpowers/specs/2026-04-30-tradeparty-refactor-brd.md
FR-TP-04: Role Enumeration

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

* Frontend issue
Advising Bank (Tag 57A) -> Advising Bank (Receiver): mandatory
Tag 57a (Advise Through Bank): optional, not required RMA with Issuing Bank