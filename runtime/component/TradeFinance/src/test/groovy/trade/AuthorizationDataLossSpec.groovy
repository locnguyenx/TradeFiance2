package trade

import org.moqui.context.ExecutionContext
import org.moqui.Moqui
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Specification
import spock.lang.Shared
import org.moqui.entity.EntityCondition

// ABOUTME: AuthorizationDataLossSpec verifies that narrative fields are preserved during instrument authorization.
// ABOUTME: Ensures that authorization triggers do not inadvertently overwrite instrument fields with null values.

class AuthorizationDataLossSpec extends Specification {
    @Shared protected ExecutionContext ec
    @Shared String testPrefix

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.artifactExecution.disableAuthz()
        ec.user.loginUser("trade.admin", "trade123")
        testPrefix = "TEST-AUTH-" + System.currentTimeMillis()
        cleanData()
    }

    def cleanupSpec() {
        try {
            if (ec != null) cleanData()
        } finally {
            if (ec != null) ec.destroy()
        }
    }

    private void cleanData() {
        boolean begun = ec.transaction.begin(60)
        try {
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").updateAll([latestTransactionId: null])
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
    }
    
    def cleanup() {
        ec.message.clearAll()
    }

    def "Verify Narrative Field Preservation during Authorization"() {
        given:
        def instrumentId = testPrefix + "_ID"
        def uniqueRef = testPrefix + "_REF"
        
        // 1. Create LC with narrative data
        Map createRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            instrumentId: instrumentId,
            lcAmount: 10000.0,
            lcCurrencyUomId: "USD",
            expiryDate: new java.sql.Timestamp(System.currentTimeMillis() + 86400000 * 30),
            goodsDescription: "INITIAL GOODS",
            documentsRequired: "INITIAL DOCUMENTS",
            instrumentRef: uniqueRef,
            instrumentParties: [
                [roleEnumId: "TP_APPLICANT", partyId: "ACME_CORP_001"],
                [roleEnumId: "TP_BENEFICIARY", partyId: "GLOBAL_EXP_002"],
                [roleEnumId: "TP_ADVISING_BANK", partyId: "ADVISING_BANK_001"]
            ]
        ]).call()
        assert !ec.message.hasError()
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
        assert !ec.message.hasError()

        // 3. Authorize the Transaction
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument").parameters([
            transactionId: transactionId,
            skipFourEyes: true
        ]).call()
        assert !ec.message.hasError()

        when: "Fetching post-authorization state"
        def lcPost = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()

        then: "Narrative fields must be preserved"
        lcPost.goodsDescription == "INITIAL GOODS"
        lcPost.documentsRequired == "INITIAL DOCUMENTS"
    }
}
