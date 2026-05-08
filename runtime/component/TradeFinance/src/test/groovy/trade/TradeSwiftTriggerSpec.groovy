package trade

import org.moqui.context.ExecutionContext
import spock.lang.Specification
import org.slf4j.Logger
import org.slf4j.LoggerFactory

// ABOUTME: TradeSwiftTriggerSpec verifies that SWIFT messages are automatically generated via SECAs.
// ABOUTME: Ensures the end-to-end integration between transaction authorization and message creation.

class TradeSwiftTriggerSpec extends Specification {
    protected final static Logger logger = LoggerFactory.getLogger(TradeSwiftTriggerSpec.class)
    private ExecutionContext ec

    def setup() {
        ec = org.moqui.Moqui.getExecutionContext()
        ec.artifactExecution.disableAuthz()
        
        // Setup Maker
        ec.user.internalLoginUser("trade.maker")
        if (ec.entity.find("moqui.security.UserAccount").condition("username", "trade.maker").count() == 0) {
            ec.entity.makeValue("moqui.security.UserAccount")
                .setAll([userId: "trade.maker", username: "trade.maker", firstName: "Trade", lastName: "Maker"])
                .create()
        }
        
        // Setup Checker
        if (ec.entity.find("moqui.security.UserAccount").condition("username", "trade.checker").count() == 0) {
            ec.entity.makeValue("moqui.security.UserAccount")
                .setAll([userId: "trade.checker", username: "trade.checker", firstName: "Trade", lastName: "Checker"])
                .create()
        }
        if (ec.entity.find("trade.UserAuthorityProfile").condition("userId", "trade.checker").count() == 0) {
            ec.entity.makeValue("trade.UserAuthorityProfile")
                .setAll([userAuthorityId: "TRG-CHECK-AUTH", userId: "trade.checker", delegationTierId: "TIER_1", 
                         customLimit: 10000000.0, currencyUomId: "USD", makerCheckerFlag: "MAKER_CHECKER"])
                .create()
        }
    }

    def cleanup() {
        ec.artifactExecution.enableAuthz()
        ec.destroy()
    }

    def "MT700 is generated automatically on LC Authorization"() {
        given: "A new Import LC in Pending state"
        def ref = "TF-TRG-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: ref, lcAmount: 50000.0, lcCurrencyUomId: "USD",
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002'],
                                   [roleEnumId: 'TP_ADVISING_BANK', partyId: 'ADVISING_BANK_001']],
                         lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_NEGOTIATION', confirmationEnumId: 'CONF_WITHOUT']).call()
        if (ec.message.hasError()) {
            logger.error("Create LC failed: " + ec.message.getErrorsString())
            throw new Exception("Create LC failed: " + ec.message.getErrorsString())
        }
        def instrumentId = res.instrumentId
        
        // Find the transaction created
        def tx = ec.entity.find("trade.TradeTransaction")
            .condition([instrumentId: instrumentId, transactionTypeEnumId: 'IMP_NEW']).disableAuthz().one()
        assert tx != null
        
        // Transition to Pending for authorization
        ec.service.sync().name("update#trade.TradeTransaction")
            .parameters([transactionId: tx.transactionId, transactionStatusId: "TX_PENDING"]).call()
        tx.refresh()
        assert tx.transactionStatusId == 'TX_PENDING'

        when: "The transaction is authorized via the Authorization Service"
        ec.user.internalLoginUser("trade.checker")
        ec.artifactExecution.disableAuthz()
        def authRes = ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument")
            .parameters([transactionId: tx.transactionId]).call()
        assert authRes.isAuthorized == true

        then: "An MT700 SWIFT message should exist in ACTIVE status"
        def txR = ec.entity.find("trade.TradeTransaction").condition("transactionId", tx.transactionId).disableAuthz().one()
        txR.transactionStatusId == 'TX_APPROVED'
        
        def swiftMsg = ec.entity.find("trade.importlc.SwiftMessage")
            .condition([instrumentId: instrumentId, messageType: 'MT700']).disableAuthz().one()
        
        if (swiftMsg == null) {
            def allMsgs = ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", instrumentId).disableAuthz().list()
            logger.error("MT700 not found for instrument ${instrumentId}. Found messages: ${allMsgs}")
        }
        assert swiftMsg != null
        swiftMsg.messageStatusId == 'SWIFT_MSG_ACTIVE'
        swiftMsg.messageContent.contains(ref)
    }

    def "MT707 is generated automatically on Amendment Authorization"() {
        given: "An issued LC and a pending Amendment"
        def ref = "TF-AMD-TRG-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: ref, lcAmount: 50000.0, lcCurrencyUomId: "USD",
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002'],
                                   [roleEnumId: 'TP_ADVISING_BANK', partyId: 'ADVISING_BANK_001']],
                         lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_NEGOTIATION', confirmationEnumId: 'CONF_WITHOUT']).call()
        def instrumentId = res.instrumentId
        
        // Approve the issuance first
        def txIss = ec.entity.find("trade.TradeTransaction").condition([instrumentId: instrumentId, transactionTypeEnumId: 'IMP_NEW']).disableAuthz().one()
        ec.service.sync().name("update#trade.TradeTransaction")
            .parameters([transactionId: txIss.transactionId, transactionStatusId: "TX_PENDING"]).call()
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument").parameters([transactionId: txIss.transactionId]).call()
        
        // Ensure LC is in LC_ISSUED state to allow amendment
        ec.service.sync().name("update#trade.importlc.ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, businessStateId: "LC_ISSUED"]).call()
        ec.service.sync().name("update#trade.TradeInstrument")
            .parameters([instrumentId: instrumentId, businessStateId: "LC_ISSUED"]).call()

        // Create amendment
        ec.user.internalLoginUser("trade.maker")
        ec.artifactExecution.disableAuthz()
        def amRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#Amendment")
            .parameters([instrumentId: instrumentId, amendmentDate: ec.user.nowTimestamp, 
                         amendmentTypeEnumId: 'AMD_TYPE_AMOUNT', amountAdjustment: 1000.0,
                         amendmentNarrative: "TEST TRIGGER"]).call()
        if (ec.message.hasError()) {
            logger.error("Create Amendment failed: " + ec.message.getErrorsString())
            // Don't throw yet, let Spock show it
        }
        
        def txAmd = ec.entity.find("trade.TradeTransaction")
            .condition([instrumentId: instrumentId, transactionTypeEnumId: 'IMP_AMENDMENT']).disableAuthz().one()
        assert txAmd != null
        ec.service.sync().name("update#trade.TradeTransaction")
            .parameters([transactionId: txAmd.transactionId, transactionStatusId: "TX_PENDING"]).call()

        when: "The amendment transaction is authorized"
        ec.user.internalLoginUser("trade.checker")
        ec.artifactExecution.disableAuthz()
        def authResAmd = ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument")
            .parameters([transactionId: txAmd.transactionId]).call()
        assert authResAmd.isAuthorized == true

        then: "An MT707 SWIFT message should exist in ACTIVE status"
        def txAR = ec.entity.find("trade.TradeTransaction").condition("transactionId", txAmd.transactionId).disableAuthz().one()
        txAR.transactionStatusId == 'TX_APPROVED'
        def swiftMsg = ec.entity.find("trade.importlc.SwiftMessage")
            .condition([instrumentId: instrumentId, messageType: 'MT707']).disableAuthz().one()
        
        swiftMsg != null
        swiftMsg.messageStatusId == 'SWIFT_MSG_ACTIVE'
        swiftMsg.messageContent.contains("TEST TRIGGER")
    }
}
