package trade

import org.moqui.context.ExecutionContext
import spock.lang.Specification
import spock.lang.Shared
import org.moqui.entity.EntityCondition

// ABOUTME: TradeSwiftTriggerSpec verifies that SWIFT messages are automatically generated via SECAs.
// ABOUTME: Ensures the end-to-end integration between transaction authorization and message creation.

class TradeSwiftTriggerSpec extends Specification {
    @Shared protected ExecutionContext ec
    @Shared String testPrefix

    def setupSpec() {
        ec = org.moqui.Moqui.getExecutionContext()
        ec.artifactExecution.disableAuthz()
        ec.user.loginUser("trade.admin", "trade123")
        testPrefix = "SW-TRG-" + System.currentTimeMillis()
        cleanData()
        
        // Setup parties
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
            .parameters([partyId: testPrefix + '_APP', partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'App', kycStatus: 'KYC_ACTIVE']).call()
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
            .parameters([partyId: testPrefix + '_BEN', partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'Ben', kycStatus: 'KYC_ACTIVE']).call()
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
            .parameters([partyId: testPrefix + '_ADV', partyTypeEnumId: 'PTY_BANK', partyName: 'Adv Bank', hasActiveRMA: true, kycStatus: 'KYC_ACTIVE']).call()
    }

    def cleanupSpec() {
        try {
            if (ec != null) cleanData()
        } finally {
            if (ec != null) ec.destroy()
        }
    }

    private void cleanData() {
        ec.artifactExecution.disableAuthz()
        boolean begun = ec.transaction.begin(60)
        try {
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").updateAll([latestTransactionId: null])
            ec.entity.find("trade.importlc.ImportLcInternalAmendment").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.ImportLcAmendment").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.ImportLcSettlement").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.ImportLcShippingGuarantee").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeApprovalRecord").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeTransaction").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            
            ec.entity.find("trade.TradeInstrumentParty").condition("partyId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradePartyBank").condition("partyId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeParty").condition("partyId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            
            ec.transaction.commit(begun)
        } catch (Exception e) {
            ec.transaction.rollback(begun, "Error in cleanData", e)
        }
    }

    def setup() {
        ec.message.clearAll()
        ec.artifactExecution.disableAuthz()
    }

    def cleanup() {
        ec.message.clearAll()
    }

    def "MT700 is generated automatically on LC Authorization"() {
        given: "A new Import LC"
        def instrumentId = testPrefix + "_MT700"
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, instrumentRef: instrumentId + "_REF", lcAmount: 50000.0, lcCurrencyUomId: "USD",
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: testPrefix + '_APP'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: testPrefix + '_BEN'],
                                   [roleEnumId: 'TP_ADVISING_BANK', partyId: testPrefix + '_ADV']],
                         lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_NEGOTIATION', confirmationEnumId: 'CONF_WITHOUT']).call()
        
        def tx = ec.entity.find("trade.TradeTransaction").condition([instrumentId: instrumentId, transactionTypeEnumId: 'IMP_NEW']).one()
        
        when: "The transaction is authorized"
        ec.user.loginUser("trade.checker", "trade123")
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument")
            .parameters([transactionId: tx.transactionId, skipFourEyes: true]).call()

        then: "The authorization should succeed"
        !ec.message.hasError()
        
        when: "Verifying generated message"
        ec.artifactExecution.disableAuthz()
        def swiftMsg = ec.entity.find("trade.importlc.SwiftMessage")
            .condition([instrumentId: instrumentId, messageType: 'MT700']).one()
        
        then: "The message should exist and be ACTIVE"
        swiftMsg != null
        swiftMsg.messageStatusId == 'SWIFT_MSG_ACTIVE'
        swiftMsg.messageContent.contains(instrumentId + "_REF")
    }

    def "MT707 is generated automatically on Amendment Authorization"() {
        given: "An issued LC"
        def instrumentId = testPrefix + "_MT707"
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, instrumentRef: instrumentId + "_REF", lcAmount: 50000.0, lcCurrencyUomId: "USD",
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: testPrefix + '_APP'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: testPrefix + '_BEN'],
                                   [roleEnumId: 'TP_ADVISING_BANK', partyId: testPrefix + '_ADV']],
                         lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_NEGOTIATION', confirmationEnumId: 'CONF_WITHOUT']).call()
        
        def txIss = ec.entity.find("trade.TradeTransaction").condition([instrumentId: instrumentId, transactionTypeEnumId: 'IMP_NEW']).one()
        ec.user.loginUser("trade.checker", "trade123")
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument")
            .parameters([transactionId: txIss.transactionId, skipFourEyes: true]).call()
        
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).updateAll([businessStateId: "LC_ISSUED"])

        ec.user.loginUser("trade.maker", "trade123")
        def amRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#ExternalAmendment")
            .parameters([instrumentId: instrumentId, amendmentTypeEnumId: 'AMEND_INCREASE', amountIncrease: 1000.0, 
                         goodsActionEnumId: 'AMA_ADD', goodsDeltaText: "TEST TRIGGER", amendmentDate: new java.sql.Date(System.currentTimeMillis())]).call()
        
        def txAmd = ec.entity.find("trade.TradeTransaction").condition([instrumentId: instrumentId, transactionTypeEnumId: 'IMP_AMENDMENT']).one()

        when: "The amendment transaction is authorized"
        ec.user.loginUser("trade.checker", "trade123")
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument")
            .parameters([transactionId: txAmd.transactionId, skipFourEyes: true]).call()

        then: "The authorization should succeed"
        !ec.message.hasError()
        
        when: "Verifying generated message"
        ec.artifactExecution.disableAuthz()
        def swiftMsg = ec.entity.find("trade.importlc.SwiftMessage")
            .condition([instrumentId: instrumentId, messageType: 'MT707']).one()
        
        then: "The message should exist and be ACTIVE"
        swiftMsg != null
        swiftMsg.messageStatusId == 'SWIFT_MSG_ACTIVE'
        swiftMsg.messageContent.contains("TEST TRIGGER")
    }
}
