# Test Coverage Matrix
This document tracks the testing coverage across all domains within the Trade Finance module.

## Backend BDD Integration Tests
Refer to `docs/technical/BackendBDDReport.md` for a comprehensive breakdown of the 66 integration tests and their BDD traceability.

## Inbound SWIFT Processing (InboundActionSpec)
The `InboundActionSpec` provides isolated unit test coverage for the `InboundActionServices.xml` suite, verifying each message type's operational payload:

| Message Type | Target Service | Purpose | Status |
|---|---|---|---|
| **MT 730** | `InboundActionServices.acknowledge#Mt730` | Validates LC status update and attachment of advising/confirming bank details to the core instrument upon successful acknowledgment. | ✅ PASS |
| **MT 799** | `InboundActionServices.resolve#AmendmentConsent` | Validates automated transition of pending `ImportLcAmendment` records to Approved or Rejected based on consent narrative parsing. | ✅ PASS |
| **MT 750** | `InboundActionServices.spawn#Presentation750` | Validates the automatic creation of a discrepant `TradeDocumentPresentation` and the generation of internal review tasks for the operations team. | ✅ PASS |
| **MT 754** | `InboundActionServices.process#Mt754` | Validates fast-tracking compliant presentations, auto-accepting the claim, and staging the financial settlement transaction. | ✅ PASS |
| **MT 742** | `InboundActionServices.reconcile#Mt742` | Validates matching reimbursement claims against existing pending Nostro Reconciliation entries. | ✅ PASS |

## Frontend UI Verification
- The `TradeDocumentPresentation` attachments mechanism currently assumes `DbResourceFile` storage (`parentResourceId='PRES_{id}'`). This architecture is defined in the backend `InboundActionServices.xml` but remains unimplemented in the frontend UI (`PresentationDetails.tsx`). Ensure future UI implementation aligns with this backend storage strategy.

## E2E Integration Verification
- **Playwright Suite (`TradeInbox.spec.ts`)**: 
  - **MT 730 Acknowledge Flow**: Full-stack integration successfully verified. End-to-end traversal confirming Swift correlation polling, backend propagation of `isAdvised='Y'`, and dynamic UI state synchronization without race conditions. | ✅ PASS
