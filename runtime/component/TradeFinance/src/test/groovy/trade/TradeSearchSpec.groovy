package trade

import spock.lang.Specification
import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import java.sql.Date

// ABOUTME: TradeSearchSpec validates contextual search and cross-reference indexing.
// ABOUTME: Verifies REQ-SRH-01.2: Instrument-to-Transaction traceability.

class TradeSearchSpec extends Specification {
    protected ExecutionContext ec

    def setupSpec() {
        ExecutionContext ec = Moqui.getExecutionContext()
        ec.artifactExecution.disableAuthz()
        ec.user.loginUser("trade.admin", "trade123")
    }

    def setup() {
        ec = Moqui.getExecutionContext()
        ec.user.loginUser("trade.admin", "trade123")
        ec.artifactExecution.disableAuthz()
        ec.message.clearAll()
    }

    def cleanup() {
        ec.user.popUser()
        ec.message.clearAll()
    }

    def cleanupSpec() {
        Moqui.getExecutionContext().destroy()
    }

    def "GAP-04: Cross-Reference Indexing (REQ-SRH-01.2)"() {
        given: "An instrument with multiple transactions"
        def ref = "SRH-TEST-" + System.currentTimeMillis()
        def createRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: 'USD',
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]]).call()
        def instrumentId = createRes.instrumentId

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
        println "DEBUG_TX_LIST: " + result.transactionList
        result.transactionList.size() >= 2
        result.transactionList.any { it.transactionTypeEnumId == 'IMP_NEW' }
        result.transactionList.any { it.transactionTypeEnumId == 'IMP_AMENDMENT' }

        cleanup:
        ec.artifactExecution.disableAuthz()
        if (instrumentId) {
            ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.importlc.ImportLcAmendment").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.TradeTransaction").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId).deleteAll()
        }
    }
}
