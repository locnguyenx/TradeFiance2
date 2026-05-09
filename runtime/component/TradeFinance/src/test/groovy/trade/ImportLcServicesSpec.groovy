package trade


import spock.lang.Specification
import spock.lang.Shared
import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityCondition
import java.sql.Date

// ABOUTME: ImportLcServicesSpec tests the core lifecycle services for Import Letters of Credit.
// Verifies create (maker initialization), update (versioning/draft edit), and approve (checker finalization).

class ImportLcServicesSpec extends Specification {
    @Shared ExecutionContext ec
    @Shared String APPLICANT_ID = "ACME_CORP_001"
    @Shared String BENEFICIARY_ID = "GLOBAL_EXP_002"
    @Shared String ISSUING_BANK_ID = "ISSUING_BANK_001"

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.artifactExecution.disableAuthz()
        ec.user.loginUser("trade.admin", "trade123")
        
        if (ec.entity.find("moqui.security.UserAccount").condition("username", "trade.admin").count() == 0) {
            ec.entity.makeValue("moqui.security.UserAccount")
                .setAll([userId: "trade.admin", username: "trade.admin", currentPassword: "trade123", firstName: "Trade", lastName: "Admin"])
                .create()
        }
        if (ec.entity.find("trade.UserAuthorityProfile").condition("userId", "trade.admin").count() == 0) {
            ec.entity.makeValue("trade.UserAuthorityProfile")
                .setAll([userAuthorityId: "T1-01", userId: "trade.admin", delegationTierId: "TIER_1", customLimit: 10000000.00, currencyUomId: "USD", makerCheckerFlag: "MAKER_CHECKER"])
                .create()
        }
        if (ec.entity.find("moqui.security.UserAccount").condition("username", "trade.checker").count() == 0) {
            ec.entity.makeValue("moqui.security.UserAccount")
                .setAll([userId: "trade.checker", username: "trade.checker", currentPassword: "trade123", firstName: "Trade", lastName: "Checker"])
                .create()
        }
        if (ec.entity.find("trade.UserAuthorityProfile").condition("userId", "trade.checker").count() == 0) {
            ec.entity.makeValue("trade.UserAuthorityProfile")
                .setAll([userAuthorityId: "T2-01", userId: "trade.checker", delegationTierId: "TIER_2", customLimit: 10000000.00, currencyUomId: "USD", makerCheckerFlag: "CHECKER"])
                .create()
        }
        
        def testPrefix = "TF-TEST-" + System.currentTimeMillis()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").updateAll([latestTransactionId: null])
        ec.entity.find("trade.importlc.TradeDocumentPresentationItem").condition("presentationId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
        ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
        ec.entity.find("trade.importlc.ImportLcSettlement").condition("presentationId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
        ec.entity.find("trade.importlc.ImportLcAmendment").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
        ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
    }

    def setup() {
        ec.message.clearAll()
        ec.user.loginUser("trade.admin", "trade123")
        ec.artifactExecution.disableAuthz()
    }

    def cleanup() {
        ec.user.popUser()
        ec.message.clearAll()
    }

    def cleanupSpec() {
        ec.destroy()
    }

    def "should create LetterOfCredit with transaction management fields"() {
        when:
        def result = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            instrumentRef: "TF-IMP-SERV-01",
            instrumentParties: [
                [roleEnumId: 'TP_APPLICANT', partyId: APPLICANT_ID],
                [roleEnumId: 'TP_BENEFICIARY', partyId: BENEFICIARY_ID],
                [roleEnumId: 'TP_ISSUING_BANK', partyId: ISSUING_BANK_ID]
            ],
            lcAmount: 500000.0,
            lcCurrencyUomId: "USD",
            expiryDate: new Date(System.currentTimeMillis() + (90L * 24 * 60 * 60 * 1000)), 
            latestShipmentDate: new Date(System.currentTimeMillis() + (60L * 24 * 60 * 60 * 1000)),
            lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_NEGOTIATION', confirmationEnumId: 'CONF_WITHOUT',
            businessStateId: "LC_DRAFT"
        ]).call()
        def instrumentId = result.instrumentId

        then:
        !ec.message.hasError()
        instrumentId != null

        def instrumentLookup = ec.entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId).useCache(false).one()
        instrumentLookup != null
        instrumentLookup.instrumentTypeEnumId == "IMPORT_LC"

        def txLookup = ec.entity.find("trade.TradeTransaction").condition("instrumentId", instrumentId).one()
        txLookup != null
        txLookup.transactionStatusId == "TX_DRAFT"
        txLookup.versionNumber == 1

        cleanup:
        if (instrumentId) {
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId).updateAll([latestTransactionId: null])
            ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.TradeTransaction").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId).deleteAll()
        }
    }

    def "should update LetterOfCredit draft and maintain versioning"() {
        given:
        def createResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            instrumentRef: "TF-IMP-UPD-01",
            instrumentParties: [
                [roleEnumId: 'TP_APPLICANT', partyId: APPLICANT_ID],
                [roleEnumId: 'TP_BENEFICIARY', partyId: BENEFICIARY_ID],
                [roleEnumId: 'TP_ISSUING_BANK', partyId: ISSUING_BANK_ID]
            ],
            lcAmount: 500000.0,
            lcCurrencyUomId: "USD",
            expiryDate: new Date(System.currentTimeMillis() + (90L * 24 * 60 * 60 * 1000)),
            latestShipmentDate: new Date(System.currentTimeMillis() + (60L * 24 * 60 * 60 * 1000)),
            lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_NEGOTIATION', confirmationEnumId: 'CONF_WITHOUT',
            businessStateId: "LC_DRAFT"
        ]).call()
        def instrumentId = createResult.instrumentId

        when:
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit").parameters([
            instrumentId: instrumentId,
            lcAmount: 600000.0,
            latestShipmentDate: new Date(System.currentTimeMillis() + (70L * 24 * 60 * 60 * 1000))
        ]).call()
        def instrumentLookup = ec.entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId).useCache(false).one()

        then:
        !ec.message.hasError()
        instrumentLookup != null
        instrumentLookup.amount == 600000.0
        
        def txLookup = ec.entity.find("trade.TradeTransaction").condition("instrumentId", instrumentId).one()
        txLookup != null
        txLookup.versionNumber == 1

        cleanup:
        if (instrumentId) {
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId).updateAll([latestTransactionId: null])
            ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.TradeTransaction").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId).deleteAll()
        }
    }

    def "should approve LetterOfCredit and track approval record"() {
        given:
        def createResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            instrumentRef: "TF-IMP-APP-01",
            instrumentParties: [
                [roleEnumId: 'TP_APPLICANT', partyId: APPLICANT_ID],
                [roleEnumId: 'TP_BENEFICIARY', partyId: BENEFICIARY_ID],
                [roleEnumId: 'TP_ISSUING_BANK', partyId: ISSUING_BANK_ID]
            ],
            lcAmount: 500000.0,
            lcCurrencyUomId: "USD",
            lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_NEGOTIATION', confirmationEnumId: 'CONF_WITHOUT',
            businessStateId: "LC_DRAFT"
        ]).call()
        def instrumentId = createResult.instrumentId

        when:
        ec.user.loginUser("trade.checker", "trade123")
        ec.service.sync().name("trade.importlc.ImportLcServices.approve#ImportLetterOfCredit").parameters([
            instrumentId: instrumentId,
            approverUserId: "trade.checker",
            approvalComments: "Standard approval"
        ]).call()
        def instrumentLookup = ec.entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId).useCache(false).one()
        def approvalRecord = ec.entity.find("trade.TradeApprovalRecord").condition("instrumentId", instrumentId).one()
        def txLookup = ec.entity.find("trade.TradeTransaction").condition("instrumentId", instrumentId).one()

        then:
        !ec.message.hasError()
        txLookup != null
        txLookup.transactionStatusId == "TX_APPROVED"
        approvalRecord != null
        approvalRecord.approverUserId == "trade.checker"

        cleanup:
        if (instrumentId) {
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId).updateAll([latestTransactionId: null])
            ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.TradeApprovalRecord").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.TradeTransaction").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId).deleteAll()
        }
    }

    def "should create Amendment draft"() {
        given:
        def createResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            instrumentRef: "TF-IMP-AMD-01",
            instrumentParties: [
                [roleEnumId: 'TP_APPLICANT', partyId: APPLICANT_ID],
                [roleEnumId: 'TP_BENEFICIARY', partyId: BENEFICIARY_ID],
                [roleEnumId: 'TP_ISSUING_BANK', partyId: ISSUING_BANK_ID]
            ],
            lcAmount: 500000.0,
            lcCurrencyUomId: "USD",
            lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_NEGOTIATION', confirmationEnumId: 'CONF_WITHOUT',
            businessStateId: "LC_DRAFT"
        ]).call()
        if (ec.message.hasError()) println "ERROR in create (Amendment Test): " + ec.message.getErrorsString()
        assert createResult != null
        def instrumentId = createResult.instrumentId

        // Approve the issuance first to allow amendment
        def txIss = ec.entity.find("trade.TradeTransaction")
            .condition([instrumentId: instrumentId, transactionTypeEnumId: 'IMP_NEW']).disableAuthz().one()
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument")
            .parameters([transactionId: txIss.transactionId, skipFourEyes: true]).call()
        ec.entity.find("trade.importlc.ImportLetterOfCredit")
            .condition("instrumentId", instrumentId).updateAll([businessStateId: "LC_ISSUED"])

        when:
        def amdResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#ExternalAmendment").parameters([
            instrumentId: instrumentId,
            amendmentTypeEnumId: "AMEND_INCREASE",
            amountIncrease: 50000.0,
            amendmentDate: new Date(System.currentTimeMillis()),
            goodsActionEnumId: 'ADD',
            goodsDeltaText: 'New certificates required'
        ]).call()
        def amendmentId = amdResult.amendmentId

        then:
        !ec.message.hasError()

        amendmentId != null
        def amdLookup = ec.entity.find("trade.importlc.ImportLcAmendment").condition("amendmentId", amendmentId).one()
        amdLookup != null
        amdLookup.goodsActionEnumId == 'ADD'
        amdLookup.amountIncrease == 50000.0
        amdLookup.amendmentBusinessStateId == "AMEND_DRAFT"

        cleanup:
        ec.artifactExecution.disableAuthz()
        if (instrumentId) {
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId).updateAll([latestTransactionId: null])
            ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.importlc.ImportLcAmendment").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.TradeTransaction").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId).deleteAll()
        }
    }

    def "should create Internal Amendment"() {
        given:
        def createResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            instrumentRef: "TF-IMP-INT-AMD-01",
            instrumentParties: [
                [roleEnumId: 'TP_APPLICANT', partyId: APPLICANT_ID],
                [roleEnumId: 'TP_BENEFICIARY', partyId: BENEFICIARY_ID],
                [roleEnumId: 'TP_ISSUING_BANK', partyId: ISSUING_BANK_ID]
            ],
            lcAmount: 500000.0,
            lcCurrencyUomId: "USD",
            lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_NEGOTIATION', confirmationEnumId: 'CONF_WITHOUT',
            businessStateId: "LC_DRAFT"
        ]).call()
        def instrumentId = createResult.instrumentId

        // Approve the issuance first to allow amendment
        def txIss = ec.entity.find("trade.TradeTransaction")
            .condition([instrumentId: instrumentId, transactionTypeEnumId: 'IMP_NEW']).disableAuthz().one()
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument")
            .parameters([transactionId: txIss.transactionId, skipFourEyes: true]).call()

        when:
        def amdResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#InternalAmendment").parameters([
            instrumentId: instrumentId,
            newFeeDebitAccountId: 'ACC_INT_001',
            newFacilityId: 'FAC_001'
        ]).call()
        def internalAmendmentId = amdResult.internalAmendmentId

        then:
        !ec.message.hasError()
        internalAmendmentId != null
        def amdLookup = ec.entity.find("trade.importlc.ImportLcInternalAmendment").condition("internalAmendmentId", internalAmendmentId).one()
        amdLookup != null
        amdLookup.newFeeDebitAccountId == 'ACC_INT_001'

        cleanup:
        ec.artifactExecution.disableAuthz()
        if (instrumentId) {
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId).updateAll([latestTransactionId: null])
            ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.importlc.ImportLcInternalAmendment").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.TradeTransaction").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId).deleteAll()
        }
    }

    def "should settle Presentation and drawdown effective outstanding"() {
        given:
        def createResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            instrumentRef: "TF-IMP-SET-01",
            instrumentParties: [
                [roleEnumId: 'TP_APPLICANT', partyId: APPLICANT_ID],
                [roleEnumId: 'TP_BENEFICIARY', partyId: BENEFICIARY_ID],
                [roleEnumId: 'TP_ISSUING_BANK', partyId: ISSUING_BANK_ID]
            ],
            lcAmount: 100000.0,
            lcCurrencyUomId: "USD",
            lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_NEGOTIATION', confirmationEnumId: 'CONF_WITHOUT',
            businessStateId: "LC_DRAFT"
        ]).call()
        if (ec.message.hasError()) println "ERROR in create: " + ec.message.getErrorsString()
        assert createResult != null
        def instrumentId = createResult.instrumentId
        
        // Approve the issuance first to allow amendment
        def txIss = ec.entity.find("trade.TradeTransaction")
            .condition([instrumentId: instrumentId, transactionTypeEnumId: 'IMP_NEW']).disableAuthz().one()
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument")
            .parameters([transactionId: txIss.transactionId, skipFourEyes: true]).call()
        ec.entity.find("trade.importlc.ImportLetterOfCredit")
            .condition("instrumentId", instrumentId).updateAll([businessStateId: "LC_ISSUED"])

        // Create a presentation first
        def presResult = ec.service.sync().name("trade.TradeCommonServices.create#Presentation").parameters([
            instrumentId: instrumentId,
            claimAmount: 40000.0
        ]).call()
        assert !ec.message.hasError()
        def presId = presResult.presentationId

        // Ensure presentation is compliant for settlement
        ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("presentationId", presId)
            .updateAll([presentationStatusId: "PRES_COMPLIANT"])

        when:
        def setlResult = ec.service.sync().name("trade.importlc.ImportLcServices.settle#Presentation").parameters([
            presentationId: presId,
            principalAmount: 40000.0,
            settlementTypeEnumId: "SIGHT_PAYMENT"
        ]).call()

        then:
        if (ec.message.hasError()) {
            println "ERROR in settle: " + ec.message.getErrorsString()
        }
        !ec.message.hasError()
        def lcLookup = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        lcLookup != null
        Math.abs(lcLookup.effectiveOutstandingAmount - 60000.0) < 0.001
        Math.abs(lcLookup.cumulativeDrawnAmount - 40000.0) < 0.001

        cleanup:
        ec.artifactExecution.disableAuthz()
        if (instrumentId) {
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId).updateAll([latestTransactionId: null])
            ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.importlc.ImportLcSettlement").condition("presentationId", presId).deleteAll()
            ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("presentationId", presId).deleteAll()
            ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.TradeTransaction").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId).deleteAll()
        }
    }

    def "should enforce Maker/Checker on Amendment authorization"() {
        setup:
        ec.artifactExecution.disableAuthz()
        // 1. Create LC as trade.maker
        def lcResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            lcAmount: 100000.0, lcCurrencyUomId: 'USD',
            instrumentParties: [[partyId: 'ACME_CORP_001', roleEnumId: 'TP_APPLICANT'], [partyId: 'GLOBAL_EXP_002', roleEnumId: 'TP_BENEFICIARY']]
        ]).call()
        String instrumentId = lcResult.instrumentId
        
        // Approve LC as trade.checker to make it ISSUED
        ec.service.sync().name("trade.importlc.ImportLcServices.approve#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, approverUserId: 'trade.checker']).call()

        // 2. Create Amendment as trade.maker
        ec.user.pushUser("trade.maker")
        def amdResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#ExternalAmendment").parameters([
            instrumentId: instrumentId, amendmentTypeEnumId: 'AMD_TYPE_GEN',
            amountIncrease: 20000.0
        ]).call()
        String amendmentId = amdResult.amendmentId

        when:
        // 3. Attempt to authorize as the same user (trade.maker) -> should fail
        ec.service.sync().name("trade.importlc.ImportLcServices.authorize#Amendment")
            .parameters([amendmentId: amendmentId, approverUserId: 'trade.maker']).call()
        ec.artifactExecution.disableAuthz()

        then:
        ec.message.hasError()
        def amdLookup = ec.entity.find("trade.importlc.ImportLcAmendment").condition("amendmentId", amendmentId).one()
        amdLookup.amendmentBusinessStateId == "AMEND_DRAFT"
        
        cleanup:
        ec.user.popUser()
    }

    def "should allow Checker to authorize Amendment"() {
        setup:
        ec.artifactExecution.disableAuthz()
        // 1. Create LC
        def lcResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            lcAmount: 100000.0, lcCurrencyUomId: 'USD',
            instrumentParties: [[partyId: 'ACME_CORP_001', roleEnumId: 'TP_APPLICANT'], [partyId: 'GLOBAL_EXP_002', roleEnumId: 'TP_BENEFICIARY']]
        ]).call()
        String instrumentId = lcResult.instrumentId
        ec.service.sync().name("trade.importlc.ImportLcServices.approve#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, approverUserId: 'trade.checker']).call()

        // 2. Create Amendment as trade.maker
        ec.user.pushUser("trade.maker")
        def amdResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#ExternalAmendment").parameters([
            instrumentId: instrumentId, amendmentTypeEnumId: 'AMD_TYPE_GEN',
            amountIncrease: 20000.0
        ]).call()
        String amendmentId = amdResult.amendmentId
        ec.user.popUser()

        when:
        // 3. Authorize as trade.checker
        ec.service.sync().name("trade.importlc.ImportLcServices.authorize#Amendment")
            .parameters([amendmentId: amendmentId, approverUserId: 'trade.checker']).call()
        ec.artifactExecution.disableAuthz()

        then:
        !ec.message.hasError()
        def amdLookup = ec.entity.find("trade.importlc.ImportLcAmendment").condition("amendmentId", amendmentId).one()
        amdLookup.amendmentBusinessStateId == "AMEND_APPROVED"
        
        // Transaction should be approved
        def tx = ec.entity.find("trade.TradeTransaction").condition("relatedRecordId", amendmentId).one()
        tx.transactionStatusId == "TX_APPROVED"
        tx.checkerUserId == "trade.checker"
    }

    def "should authorize Internal Amendment and immediately update Master LC (Scenario 3)"() {
        setup:
        ec.artifactExecution.disableAuthz()
        // 1. Create and Approve LC
        def lcResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            lcAmount: 100000.0, lcCurrencyUomId: 'USD',
            instrumentParties: [[partyId: 'ACME_CORP_001', roleEnumId: 'TP_APPLICANT'], [partyId: 'GLOBAL_EXP_002', roleEnumId: 'TP_BENEFICIARY']]
        ]).call()
        String instrumentId = lcResult.instrumentId
        ec.service.sync().name("trade.importlc.ImportLcServices.approve#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, approverUserId: 'trade.checker']).call()

        // 2. Create Internal Amendment
        ec.user.pushUser("trade.maker")
        def amdResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#InternalAmendment").parameters([
            instrumentId: instrumentId,
            newFeeDebitAccountId: 'ACC_INT_TEST',
            newFacilityId: 'FAC_TEST_001'
        ]).call()
        String internalAmendmentId = amdResult.internalAmendmentId
        ec.user.popUser()

        when:
        // 3. Authorize Internal Amendment
        ec.service.sync().name("trade.importlc.ImportLcServices.authorize#Amendment")
            .parameters([amendmentId: internalAmendmentId, approverUserId: 'trade.checker']).call()
        ec.artifactExecution.disableAuthz()

        then:
        !ec.message.hasError()
        ec.artifactExecution.disableAuthz()
        def lcLookup = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).useCache(false).one()
        lcLookup.feeDebitAccountId == 'ACC_INT_TEST'
        def instLookup = ec.entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId).useCache(false).one()
        instLookup.customerFacilityId == 'FAC_TEST_001'
        lcLookup.totalAmendmentCount == 1
    }

    def "should merge Smart Delta changes upon Beneficiary Acceptance (Scenario 4)"() {
        given:
        // 0. Setup Instrument
        ec.user.pushUser("trade.maker")
        def lcResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            lcTypeEnumId: 'LC_SIGHT',
            lcAmount: 100000,
            lcCurrencyUomId: 'USD',
            issueDate: ec.user.nowTimestamp,
            expiryDate: ec.user.nowTimestamp + 90,
            goodsDescription: 'ORIGINAL GOODS',
            instrumentParties: [
                [partyId: 'ACME_CORP_001', roleEnumId: 'TP_APPLICANT'],
                [partyId: 'GLOBAL_EXP_002', roleEnumId: 'TP_BENEFICIARY']
            ]
        ]).call()
        String instrumentId = lcResult.instrumentId
        ec.user.popUser()
        
        // Approve it first
        ec.service.sync().name("trade.importlc.ImportLcServices.approve#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, approverUserId: 'trade.checker']).call()

        // 1. Create External Amendment with Deltas
        ec.user.pushUser("trade.maker")
        def amdResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#ExternalAmendment").parameters([
            instrumentId: instrumentId,
            amendmentTypeEnumId: 'STANDARD_OUT',
            amountIncrease: 50000,
            goodsActionEnumId: 'REPLACE',
            goodsDeltaText: 'MODIFIED GOODS DESCRIPTION'
        ]).call()
        String amendmentId = amdResult.amendmentId
        
        // 2. Authorize it
        ec.user.popUser()
        ec.service.sync().name("trade.importlc.ImportLcServices.authorize#Amendment")
            .parameters([amendmentId: amendmentId, approverUserId: 'trade.checker']).call()
        
        when:
        // 3. Beneficiary Accepts
        ec.service.sync().name("trade.importlc.ImportLcServices.accept#Amendment")
            .parameters([amendmentId: amendmentId]).call()
        ec.artifactExecution.disableAuthz()
        
        then:
        !ec.message.hasError()
        def lcLookup = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).useCache(false).one()
        lcLookup.effectiveAmount == 150000
        lcLookup.goodsDescription == 'MODIFIED GOODS DESCRIPTION'
        
        def amdLookup = ec.entity.find("trade.importlc.ImportLcAmendment").condition("amendmentId", amendmentId).one()
        amdLookup.beneficiaryConsentStatusId == 'ACCEPTED'
        amdLookup.amendmentBusinessStateId == 'AMEND_COMMITTED'
    }
}
