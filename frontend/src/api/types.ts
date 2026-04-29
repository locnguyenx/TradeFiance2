// ABOUTME: TypeScript interfaces for all Trade Finance entity shapes.
// ABOUTME: Aligned with Design Spec v3.0 entity definitions.

export interface TradeTransaction {
  transactionId: string;
  instrumentId: string;
  transactionTypeEnumId: string;
  transactionStatusId: string;
  priorityEnumId: string;
  transactionDate: string;
  makerUserId: string;
  makerTimestamp: string;
  checkerUserId?: string;
  checkerTimestamp?: string;
  rejectionReason?: string;
  versionNumber: number;
  relatedRecordId?: string;
  relatedRecordType?: string;
  // Proposed value hints for simple diffs
  proposedAmount?: number;
  proposedExpiryDate?: string;
}

export interface TradeInstrument {
  instrumentId: string;
  transactionRef: string;
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
  versionNumber: number;
  lastUpdateTimestamp?: string;
  makerUserId?: string;
  priorityEnumId?: string;
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
  issuingBankBic?: string;
  advisingBankBic?: string;
  advisingThroughBankBic?: string;
  availableWithBic?: string;
  draweeBankBic?: string;
  marginType?: string;
  marginPercentage?: string;
  marginAmount?: string;
  marginDebitAccount?: string;
  availableByEnumId?: string;
  availableWithName?: string;
  shipmentPeriodText?: string;
  // UI Friendly fields
  applicantName?: string;
  applicantPartyName?: string;
  beneficiaryName?: string;
  beneficiaryPartyName?: string;
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

export interface ImportLcAmendment {
  amendmentId: string;
  instrumentId: string;
  transactionId: string;
  amount?: number;
  expiryDate?: string;
  beneficiaryPartyId?: string;
  amendmentDate: string;
}

export interface TradePresentation {
  presentationId: string;
  instrumentId: string;
  transactionId: string;
  presentationDate: string;
  presentationAmount: number;
  currencyUomId: string;
  expiryDate?: string;
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
  transactionId: string;
  instrumentId: string;
  transactionRef: string;
  module: string;
  action: string;
  makerUserId: string;
  baseEquivalentAmount: number;
  timeInQueue: string;
  priorityEnumId: string;
  transactionStatusId: string;
  businessStateId: string;
}

export interface ExposureData {
  totalLimit: number;
  totalExposure: number;
  totalFirm: number;
  totalContingent: number;
  totalReserved: number;
  utilizationPercent: number;
  facilityBreakdown: any[];
  facilityList?: any[];
}
