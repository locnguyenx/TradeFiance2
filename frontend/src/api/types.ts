// ABOUTME: TypeScript interfaces for all Trade Finance entity shapes.
// ABOUTME: Aligned with Design Spec v3.0 entity definitions.

export interface TradeInstrument {
  instrumentId: string;
  transactionRef: string;
  lifecycleStatusId: string;
  transactionStatusId: string;
  productEnumId: string;
  amount: number;
  currencyUomId: string;
  outstandingAmount: number;
  baseEquivalentAmount: number;
  applicantPartyId: string;
  beneficiaryPartyId: string;
  issueDate: string;
  expiryDate: string;
  customerFacilityId: string;
  // Transaction management
  transactionDate: string;
  transactionTypeEnumId: string;
  makerUserId: string;
  makerTimestamp: string;
  checkerUserId?: string;
  checkerTimestamp?: string;
  rejectionReason?: string;
  versionNumber: number;
  lastUpdateTimestamp?: string;
  priorityEnumId: string;
  // v3.0 Amendment Snapshot Fields
  snapshotAmount?: number;
  effectiveAmount?: number;
  snapshotExpiryDate?: string;
  effectiveExpiryDate?: string;
}

export interface ImportLetterOfCredit {
  instrumentId: string;
  businessStateId: string;
  beneficiaryPartyId?: string;
  tolerancePositive?: number;
  toleranceNegative?: number;
  tenorTypeId?: string;
  usanceDays?: number;
  portOfLoading?: string;
  portOfDischarge?: string;
  expiryPlace?: string;
  goodsDescription?: string;
  documentsRequired?: string;
  additionalConditions?: string;
  // LC-specific new fields
  chargeAllocationEnumId?: string;
  partialShipmentEnumId?: string;
  transhipmentEnumId?: string;
  latestShipmentDate?: string;
  confirmationEnumId?: string;
  lcTypeEnumId?: string;
  productCatalogId?: string;
  // UI Friendly fields
  applicantName?: string;
  beneficiaryName?: string;
  currency?: string;
  slaDaysRemaining?: number;
  // v3.0 effective values
  effectiveAmount: number;
  effectiveCurrencyUomId?: string;
  effectiveExpiryDate: string;
  effectiveTolerancePositive?: number;
  effectiveToleranceNegative?: number;
  effectiveOutstandingAmount: number;
  cumulativeDrawnAmount: number;
  totalAmendmentCount: number;
}

export interface TradeParty {
  partyId: string;
  partyName: string;
  roleTypeId: string;
  kycStatusEnumId?: string;
  sanctionsStatusEnumId?: string;
  lastKycUpdate?: string;
  riskRating?: string;
  description?: string;
  countryOfRisk?: string;
  swiftBic?: string;
  registeredAddress?: string;
  partyRoleEnumId?: string;
}

export interface TradeProductCatalog {
  productId: string;
  productName: string;
  isActive: string;
  allowedTenorEnumId?: string;
  maxToleranceLimit?: number;
  allowRevolving: string;
  allowAdvancePayment: string;
  isStandby: string;
  isTransferable: string;
  accountingFrameworkEnumId?: string;
  mandatoryMarginPercent?: number;
  documentExamSlaDays: number;
  defaultSwiftFormatEnumId?: string;
}

export interface FeeConfiguration {
  feeConfigurationId: string;
  feeEventEnumId: string;
  calculationTypeEnumId: string;
  ratePercent?: number;
  flatAmount?: number;
  minFloorAmount?: number;
  maxCeilingAmount?: number;
  currencyUomId?: string;
  isActive: string;
  description?: string;
}

export interface ImportLcSettlement {
  settlementId: string;
  presentationId: string;
  instrumentId: string;
  principalAmount: number;
  settlementCurrencyUomId: string;
  fxRate?: number;
  localEquivalent?: number;
  valueDate: string;
  debitAccountId?: string;
  marginApplied?: number;
  netDebitAmount?: number;
  chargesDetailEnumId?: string;
  maturityDate?: string;
}

export interface UserAuthorityProfile {
  userAuthorityId: string;
  userId: string;
  delegationTierId: string;
  customLimit: number;
  currencyUomId: string;
  isSuspended: string;
}

export interface PresentationDiscrepancy {
  discrepancyId: string;
  presentationId: string;
  discrepancyCode: string;
  discrepancyDescription: string;
  isWaived: string;
  waivedByUserId?: string;
  waivedTimestamp?: string;
}

export interface QueueItem {
  instrumentId: string;
  transactionRef: string;
  module: string;
  action: string;
  makerUserId: string;
  baseEquivalentAmount: number;
  timeInQueue: string;
  priorityEnumId: string;
  lifecycleStatusId: string;
}
