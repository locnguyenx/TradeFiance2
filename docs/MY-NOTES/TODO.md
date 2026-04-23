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