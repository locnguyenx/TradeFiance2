package trade

import spock.lang.*
import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityCondition

/**
 * ABOUTME: PortfolioServicesSpec validates the searching, listing, and cross-referencing of instruments and transactions.
 * Consolidates TradeSearchSpec, TradeListServicesSpec, and TradeTransactionViewSpec.
 */
@Stepwise
class PortfolioServicesSpec extends Specification {
    @Shared protected ExecutionContext ec
    @Shared String testPrefix
    @Shared String applicantId
    @Shared String beneficiaryId

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.transaction.runUseOrBegin(null, null) {
            ec.user.loginUser("trade.admin", "trade123")
            ec.artifactExecution.disableAuthz()
            testPrefix = "PORT-" + System.currentTimeMillis()

            // Set isolated ID generation ranges - use 94000000 (Module 2 - Portfolio)
            ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrument", 94000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit", 94000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.TradeTransaction", 94000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.TradeParty", 94000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrumentParty", 94000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLcAmendment", 94000000, 1000)

            applicantId = testPrefix + "_APP"
            beneficiaryId = testPrefix + "_BEN"

            ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: applicantId, partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'Port Applicant', kycStatus: 'KYC_ACTIVE']).call()
            ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: beneficiaryId, partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'Port Beneficiary', kycStatus: 'KYC_ACTIVE']).call()
        }
    }

    def cleanupSpec() {
        if (ec != null) {
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrument")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeTransaction")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeParty")
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

    def "should surface instrument and transaction references in Unified View"() {
        given: "An instrument and its transaction"
        def instRef = testPrefix + "-REF-01"
        def txRef = testPrefix + "-TX-01"

        def instRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: instRef, lcAmount: 10000.0, lcCurrencyUomId: 'USD',
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId],
                                             [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]]]).call()
        
        def tx = ec.entity.find("trade.TradeTransaction").condition("instrumentId", instRes.instrumentId).one()
        tx.set("transactionRef", txRef).update()

        when: "Querying the TradeTransactionView"
        def view = ec.entity.find("trade.TradeTransactionView").condition("transactionId", tx.transactionId).one()

        then: "Both references are hydrated"
        view != null
        view.instrumentRef == instRef
        view.transactionRef == txRef
    }

    def "should provide instrument-to-transaction traceability"() {
        given: "An instrument with multiple transactions (Issuance + Amendment)"
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: testPrefix + "_TRACE", lcAmount: 1000.0, lcCurrencyUomId: 'USD',
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId],
                                             [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]]]).call()
        def instrumentId = res.instrumentId
        
        // Authorize to allow amendment
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument")
            .parameters([transactionId: res.transactionId, skipFourEyes: true]).call()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).updateAll([businessStateId: "LC_ISSUED"])

        // Create Amendment
        ec.service.sync().name("trade.importlc.ImportLcServices.create#Amendment")
            .parameters([instrumentId: instrumentId, amendmentTypeEnumId: "AMEND_INCREASE", amountAdjustment: 500.0]).call()

        when: "Fetching transactions for the instrument"
        def result = ec.service.sync().name("trade.TradeCommonServices.get#InstrumentTransactions")
            .parameter("instrumentId", instrumentId).call()

        then: "Traceability list contains 2 transactions"
        result.transactionList.size() >= 2
        result.transactionList.any { it.transactionTypeEnumId == 'IMP_NEW' }
        result.transactionList.any { it.transactionTypeEnumId == 'IMP_AMENDMENT' }
    }

    def "should support paginated instrument list searching"() {
        given: "Multiple instruments created"
        def baseRef = testPrefix + "_LIST_"
        5.times { i ->
            ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
                .parameters([instrumentRef: baseRef + i, lcAmount: 1000.0, lcCurrencyUomId: 'USD',
                             instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId],
                                                 [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]]]).call()
        }

        when: "Searching for instruments with prefix"
        def result = ec.service.sync().name("trade.importlc.ImportLcServices.get#ImportLetterOfCreditList")
            .parameters([instrumentSearch: baseRef, pageSize: 2, pageIndex: 0]).call()

        then: "Pagination metadata is correct"
        result.lcList.size() == 2
        result.lcCount >= 5
    }
}
