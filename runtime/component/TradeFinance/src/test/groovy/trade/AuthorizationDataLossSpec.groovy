package trade

import org.moqui.context.ExecutionContext
import org.moqui.Moqui
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Specification

class AuthorizationDataLossSpec extends Specification {
    protected final static Logger logger = LoggerFactory.getLogger(AuthorizationDataLossSpec.class)

    def "Verify Narrative Field Preservation during Authorization"() {
        setup:
        ExecutionContext ec = Moqui.getExecutionContext()
        ec.user.internalLoginUser("trade.admin")
        ec.artifactExecution.disableAuthz()

        String uniqueRef = "TEST-AUTH-" + System.currentTimeMillis()
        
        // 1. Create LC with narrative data
        Map createRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            lcAmount: 10000.0,
            lcCurrencyUomId: "USD",
            expiryDate: ec.user.nowTimestamp + 30,
            goodsDescription: "INITIAL GOODS",
            documentsRequired: "INITIAL DOCUMENTS",
            instrumentRef: uniqueRef,
            instrumentParties: [
                [roleEnumId: "TP_APPLICANT", partyId: "ACME_CORP_001"],
                [roleEnumId: "TP_BENEFICIARY", partyId: "GLOBAL_EXP_002"],
                [roleEnumId: "TP_ADVISING_BANK", partyId: "ADVISING_BANK_001"]
            ]
        ]).call()
        String instrumentId = createRes.instrumentId
        String transactionId = createRes.transactionId

        // Verify initial state
        def lcInit = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        assert lcInit != null
        assert lcInit.goodsDescription == "INITIAL GOODS"

        // 2. Submit for Approval (Transition to PENDING)
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit").parameters([
            instrumentId: instrumentId,
            businessStateId: "LC_PENDING"
        ]).call()

        // 3. Authorize the Transaction
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument").parameters([
            transactionId: transactionId
        ]).call()

        // 4. Verify post-authorization state
        def lcPost = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        
        logger.info(" POST-AUTH GOODS: ${lcPost?.goodsDescription}")

        expect:
        lcPost.goodsDescription == "INITIAL GOODS"
        lcPost.documentsRequired == "INITIAL DOCUMENTS"
        
        cleanup:
        if (instrumentId) {
            ec.artifactExecution.disableAuthz()
            // Null out latestTransactionId on the instrument before deleting transaction to avoid FK constraint violation
            def inst = ec.entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId).one()
            if (inst) {
                inst.latestTransactionId = null
                inst.update()
            }
            ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.TradeTransaction").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId).deleteAll()
        }
    }
}
