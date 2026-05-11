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
    
    @Shared String applicantId
    @Shared String beneficiaryId
    @Shared String facilityId1
    @Shared String facilityId2

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.transaction.runUseOrBegin(null, null) {
            println "DEBUG: setupSpec ImportLcServicesSpec starting"
            ec.artifactExecution.disableAuthz()
            ec.user.loginUser("trade.maker", "trade123")
        testPrefix = "IMP-SRV-" + System.currentTimeMillis()
        
        applicantId = testPrefix + "-APP"
        beneficiaryId = testPrefix + "-BEN"
        facilityId1 = testPrefix + "-FAC-1"
        facilityId2 = testPrefix + "-FAC-2"

        // Ensure test parties and facilities exist
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
            .parameters([partyId: applicantId, partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'ACME Corp', kycStatus: 'KYC_ACTIVE']).call()
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
            .parameters([partyId: beneficiaryId, partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'Global Exports', kycStatus: 'KYC_ACTIVE']).call()

        ec.entity.makeValue("trade.CustomerFacility")
            .setAll([facilityId: facilityId1, ownerPartyId: applicantId, totalApprovedLimit: 1000000.0, utilizedAmount: 0.0, currencyUomId: "USD", statusId: "FAC_ACTIVE"])
            .create()
        ec.entity.makeValue("trade.CustomerFacility")
            .setAll([facilityId: facilityId2, ownerPartyId: applicantId, totalApprovedLimit: 1000000.0, utilizedAmount: 0.0, currencyUomId: "USD", statusId: "FAC_ACTIVE"])
            .create()

        // Set isolated ID generation ranges - use 8500000
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrument", 40000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit", 40000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLcAmendment", 40000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLcInternalAmendment", 40000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.TradeDocumentPresentation", 40000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeTransaction", 40000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrumentParty", 40000000, 1000)
        }
        println "DEBUG: setupSpec ImportLcServicesSpec complete"
    }

    def cleanupSpec() {
        if (ec != null) {
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrument")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLcAmendment")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLcInternalAmendment")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.TradeDocumentPresentation")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeTransaction")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrumentParty")
            ec.destroy()
        }
    }

    def setup() {
        ec.message.clearAll()
        ec.user.loginUser("trade.admin", "trade123")
        ec.artifactExecution.disableAuthz()
    }

    def cleanup() {
        ec.message.clearAll()
    }

    def "should create Import Letter of Credit in DRAFT status"() {
        when:
        def result = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            instrumentRef: testPrefix + "_REF_01",
            instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId], [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]],
            lcAmount: 100000.0, lcCurrencyUomId: "USD",
            lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_NEGOTIATION', confirmationEnumId: 'CONF_WITHOUT',
            businessStateId: "LC_DRAFT"
        ]).call()
        def instrumentId = result.instrumentId

        then:
        !ec.message.hasError()
        instrumentId != null
        def lcLookup = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        lcLookup.businessStateId == "LC_DRAFT"
    }

    def "should transition Import LC to ISSUED upon approval"() {
        given:
        def createRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            instrumentRef: testPrefix + "_REF_02",
            instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId], [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]],
            lcAmount: 200000.0, lcCurrencyUomId: "USD",
            lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_NEGOTIATION', confirmationEnumId: 'CONF_WITHOUT',
            businessStateId: "LC_DRAFT"
        ]).call()
        def instrumentId = createRes.instrumentId

        when:
        ec.service.sync().name("trade.importlc.ImportLcServices.approve#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, approverUserId: 'trade.checker']).call()

        then:
        !ec.message.hasError()
        def lcLookup = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        lcLookup.businessStateId == "LC_ISSUED"
    }

    def "should authorize Import LC and create approval record"() {
        given:
        def createRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            instrumentRef: testPrefix + "_REF_03",
            instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId], [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]],
            lcAmount: 300000.0, lcCurrencyUomId: "USD",
            lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_NEGOTIATION', confirmationEnumId: 'CONF_WITHOUT',
            businessStateId: "LC_DRAFT"
        ]).call()
        def instrumentId = createRes.instrumentId
        
        ec.artifactExecution.disableAuthz()
        def txIss = ec.entity.find("trade.TradeTransaction").condition([instrumentId: instrumentId, transactionTypeEnumId: 'IMP_NEW']).one()

        when:
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument")
            .parameters([transactionId: txIss.transactionId, skipFourEyes: true]).call()

        then:
        !ec.message.hasError()
        def txLookup = ec.entity.find("trade.TradeTransaction").condition("transactionId", txIss.transactionId).one()
        txLookup.transactionStatusId == "TX_APPROVED"
        def approvalRecord = ec.entity.find("trade.TradeApprovalRecord").condition("transactionId", txIss.transactionId).one()
        approvalRecord != null
    }

    def "should create Amendment draft"() {
        given:
        def createRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            instrumentRef: testPrefix + "_REF_04",
            instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId], [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]],
            lcAmount: 500000.0, lcCurrencyUomId: "USD",
            lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_NEGOTIATION', confirmationEnumId: 'CONF_WITHOUT',
            businessStateId: "LC_DRAFT"
        ]).call()
        def instrumentId = createRes.instrumentId

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
        def amdLookup = ec.entity.find("trade.importlc.ImportLcAmendment").condition("amendmentId", amendmentId).one()
        amdLookup.goodsActionEnumId == 'AMA_ADD'
        amdLookup.amountIncrease == 50000.0
        amdLookup.amendmentBusinessStateId == "AMEND_DRAFT"
    }

    def "should create Internal Amendment"() {
        given:
        def createRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            instrumentRef: testPrefix + "_REF_05",
            instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId], [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]],
            lcAmount: 500000.0, lcCurrencyUomId: "USD",
            lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_NEGOTIATION', confirmationEnumId: 'CONF_WITHOUT',
            businessStateId: "LC_DRAFT"
        ]).call()
        def instrumentId = createRes.instrumentId

        ec.artifactExecution.disableAuthz()
        def txIss = ec.entity.find("trade.TradeTransaction").condition([instrumentId: instrumentId, transactionTypeEnumId: 'IMP_NEW']).one()
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument").parameters([transactionId: txIss.transactionId, skipFourEyes: true]).call()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).updateAll([businessStateId: "LC_ISSUED"])

        when:
        def amdResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#InternalAmendment").parameters([
            instrumentId: instrumentId, newFeeDebitAccountId: testPrefix + '_ACC_INT_001', newFacilityId: facilityId1
        ]).call()
        def internalAmendmentId = amdResult?.internalAmendmentId

        then:
        !ec.message.hasError()
        internalAmendmentId != null
        def iaLookup = ec.entity.find("trade.importlc.ImportLcInternalAmendment").condition("internalAmendmentId", internalAmendmentId).one()
        iaLookup.newFeeDebitAccountId == testPrefix + '_ACC_INT_001'
        // Internal Amendment status is tracked in TradeTransaction
        def txAmd = ec.entity.find("trade.TradeTransaction").condition([relatedRecordId: internalAmendmentId, transactionTypeEnumId: 'IMP_AMENDMENT']).one()
        txAmd.transactionStatusId == "TX_DRAFT"
    }

    def "should settle Presentation and drawdown effective outstanding"() {
        given:
        def createRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            instrumentRef: testPrefix + "_REF_06",
            instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId], [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]],
            lcAmount: 100000.0, lcCurrencyUomId: "USD",
            lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_NEGOTIATION', confirmationEnumId: 'CONF_WITHOUT',
            businessStateId: "LC_DRAFT"
        ]).call()
        def instrumentId = createRes.instrumentId
        
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
        def lcLookup = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        Math.abs(lcLookup.effectiveOutstandingAmount - 60000.0) < 0.001
        Math.abs(lcLookup.cumulativeDrawnAmount - 40000.0) < 0.001
    }

    def "should enforce Maker/Checker on Amendment authorization"() {
        setup:
        def createRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            instrumentRef: testPrefix + "_REF_07",
            lcAmount: 100000.0, lcCurrencyUomId: 'USD',
            instrumentParties: [[partyId: applicantId, roleEnumId: 'TP_APPLICANT'], [partyId: beneficiaryId, roleEnumId: 'TP_BENEFICIARY']]
        ]).call()
        def instrumentId = createRes.instrumentId
        
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
        def createRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            instrumentRef: testPrefix + "_REF_08",
            lcAmount: 100000.0, lcCurrencyUomId: 'USD',
            instrumentParties: [[partyId: applicantId, roleEnumId: 'TP_APPLICANT'], [partyId: beneficiaryId, roleEnumId: 'TP_BENEFICIARY']]
        ]).call()
        def instrumentId = createRes.instrumentId
        
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
        def amdLookup = ec.entity.find("trade.importlc.ImportLcAmendment").condition("amendmentId", amendmentId).one()
        amdLookup.amendmentBusinessStateId == "AMEND_APPROVED"
    }

    def "should authorize Internal Amendment and immediately update Master LC (Scenario 3)"() {
        given:
        def createRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            instrumentRef: testPrefix + "_REF_09",
            instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId], [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]],
            lcAmount: 500000.0, lcCurrencyUomId: "USD",
            lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_NEGOTIATION', confirmationEnumId: 'CONF_WITHOUT',
            businessStateId: "LC_DRAFT"
        ]).call()
        def instrumentId = createRes.instrumentId

        ec.artifactExecution.disableAuthz()
        def txIss = ec.entity.find("trade.TradeTransaction").condition([instrumentId: instrumentId, transactionTypeEnumId: 'IMP_NEW']).one()
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument").parameters([transactionId: txIss.transactionId, skipFourEyes: true]).call()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).updateAll([businessStateId: "LC_ISSUED"])

        def iaResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#InternalAmendment").parameters([
            instrumentId: instrumentId, newFeeDebitAccountId: testPrefix + '_ACC_INT_002', newFacilityId: facilityId2
        ]).call()
        def internalAmendmentId = iaResult?.internalAmendmentId

        when:
        ec.service.sync().name("trade.importlc.ImportLcServices.authorize#Amendment")
            .parameters([amendmentId: internalAmendmentId, approverUserId: 'trade.checker', skipFourEyes: true]).call()

        then:
        !ec.message.hasError()
        def lcLookup = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        lcLookup.feeDebitAccountId == testPrefix + '_ACC_INT_002'
        def instLookup = ec.entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId).one()
        instLookup.customerFacilityId == facilityId2
    }
}
