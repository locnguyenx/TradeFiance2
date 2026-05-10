package trade

import spock.lang.Specification
import spock.lang.Shared
import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityCondition
import java.sql.Date

// ABOUTME: TradeSearchSpec validates contextual search and cross-reference indexing.
// ABOUTME: Verifies REQ-SRH-01.2: Instrument-to-Transaction traceability.

class TradeSearchSpec extends Specification {
    @Shared protected ExecutionContext ec
    @Shared String testPrefix

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.artifactExecution.disableAuthz()
        ec.user.loginUser("trade.admin", "trade123")
        testPrefix = "SRCH-" + System.currentTimeMillis()
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
            ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.ImportLcAmendment").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeTransaction").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.transaction.commit(begun)
        } catch (Exception e) {
            ec.transaction.rollback(begun, "Error in cleanData", e)
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

    def "BDD-CMN-SRH-01: Cross-Reference Indexing (REQ-SRH-01.2)"() {
        given: "An instrument with multiple transactions"
        def ref = testPrefix + "-REF"
        def createRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentId: testPrefix + "_ID", instrumentRef: ref, lcAmount: 1000.0, lcCurrencyUomId: 'USD',
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]]).call()
        def instrumentId = createRes.instrumentId
        
        // Approve issuance to allow amendment
        def txIss = ec.entity.find("trade.TradeTransaction")
            .condition([instrumentId: instrumentId, transactionTypeEnumId: 'IMP_NEW']).disableAuthz().one()
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument")
            .parameters([transactionId: txIss.transactionId, skipFourEyes: true]).call()
        ec.entity.find("trade.importlc.ImportLetterOfCredit")
            .condition("instrumentId", instrumentId).updateAll([businessStateId: "LC_ISSUED"])

        // Add an amendment transaction
        def amdRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#Amendment")
            .parameters([instrumentId: instrumentId, amendmentTypeEnumId: "AMEND_INCREASE", amountAdjustment: 100.0, isFinancial: 'Y']).call()
        if (ec.message.hasError()) println "DEBUG_AMD_ERROR: " + ec.message.getErrorsString()

        when: "Searching for transactions by instrumentId"
        def result = ec.service.sync().name("trade.TradeCommonServices.get#InstrumentTransactions")
            .parameter("instrumentId", instrumentId).call()
        
        then: "Both Issuance and Amendment transactions are surfaced"
        if (ec.message.hasError()) println "DEBUG_SEARCH_ERROR: " + ec.message.getErrorsString()
        !ec.message.hasError()
        result.transactionList != null
        result.transactionList.size() >= 2
        result.transactionList.any { it.transactionTypeEnumId == 'IMP_NEW' }
        result.transactionList.any { it.transactionTypeEnumId == 'IMP_AMENDMENT' }
    }
}
