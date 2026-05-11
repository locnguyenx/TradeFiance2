package trade

import spock.lang.Specification
import spock.lang.Shared
import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityCondition
import java.sql.Date

/**
 * ABOUTME: TradeSearchSpec validates contextual search and cross-reference indexing.
 * Verifies REQ-SRH-01.2: Instrument-to-Transaction traceability.
 */
class TradeSearchSpec extends Specification {
    @Shared protected ExecutionContext ec
    @Shared String testPrefix
    
    @Shared String applicantId
    @Shared String beneficiaryId

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.artifactExecution.disableAuthz()
        ec.user.loginUser("trade.maker", "trade123")
        testPrefix = "SRCH-SPEC-" + System.currentTimeMillis()
        
        applicantId = testPrefix + "-APP"
        beneficiaryId = testPrefix + "-BEN"

        // Set isolated ID generation ranges - use 5200000
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrument", 52000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit", 52000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeTransaction", 52000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrumentParty", 52000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLcAmendment", 52000000, 1000)

        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
            .parameters([partyId: applicantId, partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'ACME Corp Search', kycStatus: 'KYC_ACTIVE']).call()
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
            .parameters([partyId: beneficiaryId, partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'Global Exports Search', kycStatus: 'KYC_ACTIVE']).call()
    }

    def cleanupSpec() {
        if (ec != null) {
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrument")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeTransaction")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrumentParty")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLcAmendment")
            ec.destroy()
        }
    }

    def setup() {
        if (ec.transaction.isTransactionInPlace()) ec.transaction.rollback("Cleanup", null)
        ec.message.clearAll()
        ec.user.loginUser("trade.admin", "trade123")
        ec.artifactExecution.disableAuthz()
    }

    def cleanup() {
        ec.message.clearAll()
    }

    def "BDD-CMN-SRH-01: Cross-Reference Indexing (REQ-SRH-01.2)"() {
        given: "An instrument with multiple transactions"
        def ref = testPrefix + "-REF-01"
        
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: ref, lcAmount: 1000.0, lcCurrencyUomId: 'USD',
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]]]).call()
        def instrumentId = res.instrumentId
        
        // Approve issuance to allow amendment
        def txIss = ec.entity.find("trade.TradeTransaction")
            .condition([instrumentId: instrumentId, transactionTypeEnumId: 'IMP_NEW']).disableAuthz().one()
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument")
            .parameters([transactionId: txIss.transactionId, skipFourEyes: true]).call()
        ec.entity.find("trade.importlc.ImportLetterOfCredit")
            .condition("instrumentId", instrumentId).updateAll([businessStateId: "LC_ISSUED"])

        // Add an amendment transaction
        ec.service.sync().name("trade.importlc.ImportLcServices.create#Amendment")
            .parameters([instrumentId: instrumentId, amendmentTypeEnumId: "AMEND_INCREASE", amountAdjustment: 100.0, isFinancial: 'Y']).call()

        when: "Searching for transactions by instrumentId"
        def result = ec.service.sync().name("trade.TradeCommonServices.get#InstrumentTransactions")
            .parameter("instrumentId", instrumentId).call()
        
        then: "Both Issuance and Amendment transactions are surfaced"
        !ec.message.hasError()
        result.transactionList != null
        result.transactionList.size() >= 2
        result.transactionList.any { it.transactionTypeEnumId == 'IMP_NEW' }
        result.transactionList.any { it.transactionTypeEnumId == 'IMP_AMENDMENT' }
    }
}
