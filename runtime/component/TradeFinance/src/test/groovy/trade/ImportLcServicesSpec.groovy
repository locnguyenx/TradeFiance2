package trade

import spock.lang.Specification
import spock.lang.Shared
import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityCondition
import java.sql.Date

// ABOUTME: ImportLcServicesSpec validates the business logic of Import Letter of Credit services.
// ABOUTME: Covers issuance, amendment (external/internal), and settlement lifecycles.

class ImportLcServicesSpec extends Specification {
    @Shared protected ExecutionContext ec
    @Shared String testPrefix
    
    @Shared static final String APPLICANT_ID = 'ACME_CORP_001'
    @Shared static final String BENEFICIARY_ID = 'GLOBAL_EXP_002'
    @Shared static final String ISSUING_BANK_ID = 'UTB_SG_001'

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.artifactExecution.disableAuthz()
        ec.user.loginUser("trade.admin", "trade123")
        testPrefix = "IMP-SRV-" + System.currentTimeMillis()
        
        // Ensure test parties and facilities exist
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
            .parameters([partyId: APPLICANT_ID, partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'ACME Corp', kycStatus: 'KYC_ACTIVE']).call()
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
            .parameters([partyId: BENEFICIARY_ID, partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'Global Exports', kycStatus: 'KYC_ACTIVE']).call()

        ec.entity.makeValue("trade.CustomerFacility")
            .setAll([facilityId: 'FAC_001', ownerPartyId: APPLICANT_ID, totalApprovedLimit: 1000000.0, utilizedAmount: 0.0, currencyUomId: "USD", statusId: "FAC_ACTIVE"])
            .createOrUpdate()
        ec.entity.makeValue("trade.CustomerFacility")
            .setAll([facilityId: 'FAC_002', ownerPartyId: APPLICANT_ID, totalApprovedLimit: 1000000.0, utilizedAmount: 0.0, currencyUomId: "USD", statusId: "FAC_ACTIVE"])
            .createOrUpdate()
            
        cleanData()
    }

    def cleanupSpec() {
        try {
            if (ec != null) {
                ec.artifactExecution.disableAuthz()
                cleanData()
            }
        } finally {
            if (ec != null) ec.destroy()
        }
    }

    private void cleanData() {
        boolean begun = ec.transaction.begin(60)
        try {
            ec.artifactExecution.disableAuthz()
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").updateAll([latestTransactionId: null])
            ec.entity.find("trade.importlc.ImportLcInternalAmendment").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.ImportLcAmendment").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.ImportLcShippingGuarantee").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.ImportLcSettlement").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeApprovalRecord").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeTransaction").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.transaction.commit(begun)
        } catch (Exception e) {
            ec.transaction.rollback(begun, "Error in cleanData", e)
        }
    }

    def setup() {
        ec.message.clearAll()
        ec.artifactExecution.disableAuthz()
        ec.user.loginUser("trade.admin", "trade123")
    }

    def cleanup() {
        ec.message.clearAll()
    }

    def "should create Import Letter of Credit in DRAFT status"() {
        given:
        def instrumentId = testPrefix + "_DRAFT_01"
        
        when:
        def result = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            instrumentId: instrumentId, instrumentRef: instrumentId + "_REF",
            instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: APPLICANT_ID], [roleEnumId: 'TP_BENEFICIARY', partyId: BENEFICIARY_ID]],
            lcAmount: 100000.0, lcCurrencyUomId: "USD",
            lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_NEGOTIATION', confirmationEnumId: 'CONF_WITHOUT',
            businessStateId: "LC_DRAFT"
        ]).call()

        then:
        !ec.message.hasError()
        result.instrumentId != null
        ec.artifactExecution.disableAuthz() || true
        def lcLookup = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", result.instrumentId).one()
        lcLookup.businessStateId == "LC_DRAFT"
    }

    def "should transition Import LC to ISSUED upon approval"() {
        given:
        def instrumentId = testPrefix + "_APPROVE_01"
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            instrumentId: instrumentId, instrumentRef: instrumentId + "_REF",
            instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: APPLICANT_ID], [roleEnumId: 'TP_BENEFICIARY', partyId: BENEFICIARY_ID]],
            lcAmount: 200000.0, lcCurrencyUomId: "USD",
            lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_NEGOTIATION', confirmationEnumId: 'CONF_WITHOUT',
            businessStateId: "LC_DRAFT"
        ]).call()

        when:
        ec.service.sync().name("trade.importlc.ImportLcServices.approve#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, approverUserId: 'trade.checker']).call()

        then:
        !ec.message.hasError()
        ec.artifactExecution.disableAuthz() || true
        def lcLookup = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        lcLookup.businessStateId == "LC_ISSUED"
    }

    def "should authorize Import LC and create approval record"() {
        given:
        def instrumentId = testPrefix + "_AUTH_01"
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            instrumentId: instrumentId, instrumentRef: instrumentId + "_REF",
            instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: APPLICANT_ID], [roleEnumId: 'TP_BENEFICIARY', partyId: BENEFICIARY_ID]],
            lcAmount: 300000.0, lcCurrencyUomId: "USD",
            lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_NEGOTIATION', confirmationEnumId: 'CONF_WITHOUT',
            businessStateId: "LC_DRAFT"
        ]).call()
        ec.artifactExecution.disableAuthz()
        def txIss = ec.entity.find("trade.TradeTransaction").condition([instrumentId: instrumentId, transactionTypeEnumId: 'IMP_NEW']).one()

        when:
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument")
            .parameters([transactionId: txIss.transactionId, skipFourEyes: true]).call()

        then:
        !ec.message.hasError()
        ec.artifactExecution.disableAuthz() || true
        def txLookup = ec.entity.find("trade.TradeTransaction").condition("transactionId", txIss.transactionId).one()
        txLookup.transactionStatusId == "TX_APPROVED"
        def approvalRecord = ec.entity.find("trade.TradeApprovalRecord").condition("transactionId", txIss.transactionId).one()
        approvalRecord != null
    }

    def "should create Amendment draft"() {
        given:
        def instrumentId = testPrefix + "_AMD_01"
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            instrumentId: instrumentId, instrumentRef: instrumentId + "_REF",
            instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: APPLICANT_ID], [roleEnumId: 'TP_BENEFICIARY', partyId: BENEFICIARY_ID]],
            lcAmount: 500000.0, lcCurrencyUomId: "USD",
            lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_NEGOTIATION', confirmationEnumId: 'CONF_WITHOUT',
            businessStateId: "LC_DRAFT"
        ]).call()

        ec.artifactExecution.disableAuthz()
        def txIss = ec.entity.find("trade.TradeTransaction").condition([instrumentId: instrumentId, transactionTypeEnumId: 'IMP_NEW']).one()
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument").parameters([transactionId: txIss.transactionId, skipFourEyes: true]).call()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).updateAll([businessStateId: "LC_ISSUED"])

        when:
        def amdResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#ExternalAmendment").parameters([
            instrumentId: instrumentId, amendmentTypeEnumId: "AMEND_INCREASE", amountIncrease: 50000.0,
            amendmentDate: new java.sql.Date(System.currentTimeMillis()), goodsActionEnumId: 'AMA_ADD', goodsDeltaText: 'New certificates required'
        ]).call()
        def amendmentId = amdResult?.amendmentId

        then:
        !ec.message.hasError()
        amendmentId != null
        ec.artifactExecution.disableAuthz() || true
        def amdLookup = ec.entity.find("trade.importlc.ImportLcAmendment").condition("amendmentId", amendmentId).one()
        amdLookup.goodsActionEnumId == 'AMA_ADD'
        amdLookup.amountIncrease == 50000.0
        amdLookup.amendmentBusinessStateId == "AMEND_DRAFT"
    }

    def "should create Internal Amendment"() {
        given:
        def instrumentId = testPrefix + "_IA_01"
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            instrumentId: instrumentId, instrumentRef: instrumentId + "_REF",
            instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: APPLICANT_ID], [roleEnumId: 'TP_BENEFICIARY', partyId: BENEFICIARY_ID]],
            lcAmount: 500000.0, lcCurrencyUomId: "USD",
            lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_NEGOTIATION', confirmationEnumId: 'CONF_WITHOUT',
            businessStateId: "LC_DRAFT"
        ]).call()

        ec.artifactExecution.disableAuthz()
        def txIss = ec.entity.find("trade.TradeTransaction").condition([instrumentId: instrumentId, transactionTypeEnumId: 'IMP_NEW']).one()
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument").parameters([transactionId: txIss.transactionId, skipFourEyes: true]).call()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).updateAll([businessStateId: "LC_ISSUED"])

        when:
        def amdResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#InternalAmendment").parameters([
            instrumentId: instrumentId, newFeeDebitAccountId: 'ACC_INT_001', newFacilityId: 'FAC_001'
        ]).call()
        def internalAmendmentId = amdResult?.internalAmendmentId

        then:
        !ec.message.hasError()
        internalAmendmentId != null
        ec.artifactExecution.disableAuthz() || true
        def iaLookup = ec.entity.find("trade.importlc.ImportLcInternalAmendment").condition("internalAmendmentId", internalAmendmentId).one()
        iaLookup.newFeeDebitAccountId == 'ACC_INT_001'
        // Internal Amendment status is tracked in TradeTransaction
        def txAmd = ec.entity.find("trade.TradeTransaction").condition([relatedRecordId: internalAmendmentId, transactionTypeEnumId: 'IMP_AMENDMENT']).one()
        txAmd.transactionStatusId == "TX_DRAFT"
    }

    def "should settle Presentation and drawdown effective outstanding"() {
        given:
        def instrumentId = testPrefix + "_SETTLE_01"
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            instrumentId: instrumentId, instrumentRef: instrumentId + "_REF",
            instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: APPLICANT_ID], [roleEnumId: 'TP_BENEFICIARY', partyId: BENEFICIARY_ID]],
            lcAmount: 100000.0, lcCurrencyUomId: "USD",
            lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_NEGOTIATION', confirmationEnumId: 'CONF_WITHOUT',
            businessStateId: "LC_DRAFT"
        ]).call()
        
        ec.artifactExecution.disableAuthz()
        def txIss = ec.entity.find("trade.TradeTransaction").condition([instrumentId: instrumentId, transactionTypeEnumId: 'IMP_NEW']).one()
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument").parameters([transactionId: txIss.transactionId, skipFourEyes: true]).call()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).updateAll([businessStateId: "LC_ISSUED"])

        def presResult = ec.service.sync().name("trade.TradeCommonServices.create#Presentation").parameters([
            instrumentId: instrumentId, claimAmount: 40000.0
        ]).call()
        def presId = presResult?.presentationId
        ec.artifactExecution.disableAuthz()
        ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("presentationId", presId).updateAll([presentationStatusId: "PRES_COMPLIANT"])

        when:
        ec.service.sync().name("trade.importlc.ImportLcServices.settle#Presentation").parameters([
            presentationId: presId, principalAmount: 40000.0, settlementTypeEnumId: "SIGHT_PAYMENT"
        ]).call()

        then:
        !ec.message.hasError()
        ec.artifactExecution.disableAuthz() || true
        def lcLookup = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        Math.abs(lcLookup.effectiveOutstandingAmount - 60000.0) < 0.001
        Math.abs(lcLookup.cumulativeDrawnAmount - 40000.0) < 0.001
    }

    def "should enforce Maker/Checker on Amendment authorization"() {
        setup:
        def instrumentId = testPrefix + "_MC_01"
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            instrumentId: instrumentId, instrumentRef: instrumentId + "_REF",
            lcAmount: 100000.0, lcCurrencyUomId: 'USD',
            instrumentParties: [[partyId: APPLICANT_ID, roleEnumId: 'TP_APPLICANT'], [partyId: BENEFICIARY_ID, roleEnumId: 'TP_BENEFICIARY']]
        ]).call()
        
        ec.artifactExecution.disableAuthz()
        def txIss = ec.entity.find("trade.TradeTransaction").condition([instrumentId: instrumentId, transactionTypeEnumId: 'IMP_NEW']).one()
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument").parameters([transactionId: txIss.transactionId, skipFourEyes: true]).call()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).updateAll([businessStateId: "LC_ISSUED"])

        ec.user.loginUser("trade.maker", "trade123")
        def amdResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#ExternalAmendment").parameters([
            instrumentId: instrumentId, amendmentTypeEnumId: "AMEND_INCREASE", amountIncrease: 10000.0,
            amendmentDate: new java.sql.Date(System.currentTimeMillis())
        ]).call()
        def amendmentId = amdResult?.amendmentId

        when:
        ec.service.sync().name("trade.importlc.ImportLcServices.authorize#Amendment")
            .parameters([amendmentId: amendmentId, approverUserId: 'trade.maker']).call()

        then:
        ec.message.hasError()
        ec.message.getErrorsString().contains("Authorization failed: Maker/Checker validation")
    }

    def "should allow Checker to authorize Amendment"() {
        setup:
        def instrumentId = testPrefix + "_MC_02"
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            instrumentId: instrumentId, instrumentRef: instrumentId + "_REF",
            lcAmount: 100000.0, lcCurrencyUomId: 'USD',
            instrumentParties: [[partyId: APPLICANT_ID, roleEnumId: 'TP_APPLICANT'], [partyId: BENEFICIARY_ID, roleEnumId: 'TP_BENEFICIARY']]
        ]).call()
        
        ec.artifactExecution.disableAuthz()
        def txIss = ec.entity.find("trade.TradeTransaction").condition([instrumentId: instrumentId, transactionTypeEnumId: 'IMP_NEW']).one()
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument").parameters([transactionId: txIss.transactionId, skipFourEyes: true]).call()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).updateAll([businessStateId: "LC_ISSUED"])

        ec.user.loginUser("trade.maker", "trade123")
        def amdResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#ExternalAmendment").parameters([
            instrumentId: instrumentId, amendmentTypeEnumId: "AMEND_INCREASE", amountIncrease: 10000.0,
            amendmentDate: new java.sql.Date(System.currentTimeMillis())
        ]).call()
        def amendmentId = amdResult?.amendmentId

        when:
        ec.user.loginUser("trade.checker", "trade123")
        ec.service.sync().name("trade.importlc.ImportLcServices.authorize#Amendment")
            .parameters([amendmentId: amendmentId, approverUserId: 'trade.checker']).call()
        ec.user.loginUser("trade.admin", "trade123")

        then:
        !ec.message.hasError()
        ec.artifactExecution.disableAuthz() || true
        def amdLookup = ec.entity.find("trade.importlc.ImportLcAmendment").condition("amendmentId", amendmentId).one()
        amdLookup.amendmentBusinessStateId == "AMEND_APPROVED"
    }

    def "should authorize Internal Amendment and immediately update Master LC (Scenario 3)"() {
        given:
        def instrumentId = testPrefix + "_IA_SC3"
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            instrumentId: instrumentId, instrumentRef: instrumentId + "_REF",
            instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: APPLICANT_ID], [roleEnumId: 'TP_BENEFICIARY', partyId: BENEFICIARY_ID]],
            lcAmount: 500000.0, lcCurrencyUomId: "USD",
            lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_NEGOTIATION', confirmationEnumId: 'CONF_WITHOUT',
            businessStateId: "LC_DRAFT"
        ]).call()

        ec.artifactExecution.disableAuthz()
        def txIss = ec.entity.find("trade.TradeTransaction").condition([instrumentId: instrumentId, transactionTypeEnumId: 'IMP_NEW']).one()
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument").parameters([transactionId: txIss.transactionId, skipFourEyes: true]).call()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).updateAll([businessStateId: "LC_ISSUED"])

        def iaResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#InternalAmendment").parameters([
            instrumentId: instrumentId, newFeeDebitAccountId: 'ACC_INT_002', newFacilityId: 'FAC_002'
        ]).call()
        def internalAmendmentId = iaResult?.internalAmendmentId

        when:
        ec.service.sync().name("trade.importlc.ImportLcServices.authorize#Amendment")
            .parameters([amendmentId: internalAmendmentId, approverUserId: 'trade.checker', skipFourEyes: true]).call()

        then:
        !ec.message.hasError()
        ec.artifactExecution.disableAuthz() || true
        def lcLookup = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        lcLookup.feeDebitAccountId == 'ACC_INT_002'
        def instLookup = ec.entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId).one()
        instLookup.customerFacilityId == 'FAC_002'
    }
}
