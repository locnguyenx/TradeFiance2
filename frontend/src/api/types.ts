// ABOUTME: TypeScript interfaces for all Trade Finance entity shapes.
// ABOUTME: Aligned with Design Spec v3.0 entity definitions.

export interface TradeTransaction {
  transactionId: string;
  instrumentId: string;
  transactionTypeEnumId: string;
  transactionStatusId: string;
  priorityEnumId?: string;
  rejectionReason?: string;
  
  // v3.0 snapshot/delta fields
  transactionRef: string;
  instrumentRef?: string;
  proposedAmount?: number;
  proposedCurrencyUomId?: string;
  proposedExpiryDate?: string;
  transactionDate: string;
  makerUserId: string;
  makerTimestamp: string;
  checkerUserId?: string;
  checkerTimestamp?: string;
  versionNumber: number;
  relatedRecordId?: string;
  relatedRecordType?: string;
  relatedRecord?: any;
}

export interface TradeInstrument {
  instrumentId: string;
  instrumentRef: string;
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

export interface TradeInstrumentParty {
  instrumentId: string;
  roleEnumId: string;
  partyId: string;
  partyName?: string;
  swiftBic?: string;
  partyTypeEnumId?: string;
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
  
  receiptPlace?: string;
  finalDeliveryPlace?: string;
  shipmentPeriodText?: string;
  maxCreditAmountFlag?: string;
  additionalAmountsText?: string;
  mixedPaymentDetails?: string;
  deferredPaymentDetails?: string;
  usanceBaseDate?: string;
  bankToBankInstructions?: string;
  presentationPeriodDays?: number;
  chargeAllocationText?: string;
  
  // NEW Party Junction Fields
  parties?: TradeInstrumentParty[];
  availableWithEnumId?: string;
  availableByEnumId?: string;

  issuingBankBic?: string;
  advisingBankBic?: string;
  advisingThroughBankBic?: string;
  availableWithBic?: string;
  draweeBankBic?: string;
  marginType?: string;
  marginPercentage?: string;
  marginAmount?: string;
  marginDebitAccount?: string;
  // UI Friendly fields
  applicantName?: string;
  applicantPartyName?: string;
  beneficiaryName?: string;
  beneficiaryPartyName?: string;
  reimbursingBankPartyId?: string;
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
  
  // SRG 2024 Compliance
  paymentCondBeneText?: string;
  paymentCondBankText?: string;
  applicableRulesEnumId?: string;
  applicableRulesText?: string;
  authExpiryDate?: string;
  reimbursingChargesEnumId?: string;
  applicableReimbRulesText?: string;
  isAdvised?: string;
  advisedDate?: string;
  isAdvisedToBeneficiary?: string;
  advisedToBeneficiaryDate?: string;
}


export interface TradeParty {
  partyId: string;
  partyTypeEnumId: string;
  partyName: string;
  swiftBic?: string;
  registeredAddress?: string;
  accountNumber?: string;
  countryOfRisk?: string;
  kycStatus?: string;
  sanctionsStatus?: string;
  hasActiveRMA?: string | boolean;
  clearingCode?: string;
  nostroAccountRef?: string;
  riskRating?: string;
  lastKycUpdate?: string;
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
  baseValue?: number;
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
  amendmentDate: string;
  amountAdjustment?: number;
  newExpiryDate?: string;

  // SRG 2024 Compliance
  isCancellationRequest?: string;
  newAuthExpiryDate?: string;
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
  instrumentRef: string;
  transactionRef: string;
  module: string;
  action: string;
  makerUserId: string;
  baseEquivalentAmount: number;
  transactionAmount: number;
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

export interface NostroReconciliation {
    reconciliationId: string;
    instrumentId: string;
    reimbursingBankPartyId: string;
    expectedCurrency: string;
    expectedAmount: number;
    nostroDebitDate?: string;
    nostroDebitAmount?: number;
    nostroStatementRef?: string;
    matchStatusEnumId: string;
    matchedByUserId?: string;
    matchedDate?: string;
    remarks?: string;
}

export interface SwiftMessage {
  swiftMessageId: string;
  instrumentId: string;
  messageType: string;
  messageStatusId: string;
  messageContent: string;
  generatedDate: string;
}

export interface TradeInboxItem {
  inboxItemId: string;
  rawMessageId: string;
  messageType: string;
  instrumentId?: string;
  instrumentRef?: string;
  amendmentId?: string;
  presentationId?: string;
  senderBic?: string;
  senderReference?: string;
  relatedReference?: string;
  narrativeText?: string;
  claimAmount?: number;
  claimCurrency?: string;
  receivedTimestamp: string;
  inboxStatusEnumId: string;
  actionTaken?: string;
  processedByUserId?: string;
  processedTimestamp?: string;
  correlationStatusEnumId?: string;
  orphanReason?: string;
  securityWarningFlag?: string;
}


