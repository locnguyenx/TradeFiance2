package trade

import spock.lang.Specification
import spock.lang.Shared
import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityCondition

/**
 * ABOUTME: DraftLcSpec validates the creation and visibility of draft Import LCs on the dashboard.
 * Ensures that auto-generated transaction references and status tracking work as expected.
 */
class DraftLcSpec extends Specification {
    @Shared protected ExecutionContext ec
    @Shared String testPrefix
    @Shared String applicantId
    @Shared String beneficiaryId

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.transaction.runUseOrBegin(null, null) {
            ec.artifactExecution.disableAuthz()
            ec.user.loginUser("trade.maker", "trade123")
            testPrefix = "DFT-LC-" + System.currentTimeMillis()
            
            applicantId = testPrefix + "-APP"
            beneficiaryId = testPrefix + "-BEN"

            // Set isolated ID generation ranges - use 10500000
            ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrument", 10500000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit", 10500000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.TradeTransaction", 10500000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrumentParty", 10500000, 1000)

            // Ensure test parties exist
            ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: applicantId, partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'Draft ACME Corp', kycStatus: 'KYC_ACTIVE']).call()
            ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: beneficiaryId, partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'Draft Global Exports', kycStatus: 'KYC_ACTIVE']).call()
        }
    }

    def cleanupSpec() {
        if (ec != null) {
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrument")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeTransaction")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrumentParty")
            ec.destroy()
        }
    }

    def setup() {
        if (ec.transaction.isTransactionInPlace()) ec.transaction.rollback("Cleanup", null)
        ec.message.clearAll()
        ec.artifactExecution.disableAuthz()
    }

    def "should successfully create a draft Import LC and verify its existence"() {
        given: "Standard LC parameters"
        def ref1 = testPrefix + "-REF-01"
        def params = [
            instrumentRef: ref1, lcAmount: 100000.0, lcCurrencyUomId: "USD",
            instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId], [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]],
            lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_NEGOTIATION', confirmationEnumId: 'CONF_WITHOUT'
        ]

        when: "Calling create#ImportLetterOfCredit"
        def result = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters(params).call()
        def instrumentId = result.instrumentId

        then: "LC is created in DRAFT status"
        !ec.message.hasError()
        instrumentId != null
        def lcLookup = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        lcLookup.businessStateId == "LC_DRAFT"
        lcLookup.effectiveAmount == 100000.0
    }

    def "should show Draft LCs in the dashboard/list view"() {
        given: "A draft LC is created"
        def ref2 = testPrefix + "-REF-02"
        def createRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            instrumentRef: ref2, lcAmount: 50000.0, lcCurrencyUomId: "USD",
            instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId], [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]],
            lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_SIGHT_PAYMENT', confirmationEnumId: 'CONF_WITHOUT'
        ]).call()
        def instrumentId2 = createRes.instrumentId
        
        // Ensure Transaction is created in DRAFT
        def tx = ec.entity.find("trade.TradeTransaction").condition("instrumentId", instrumentId2).one()
        assert tx != null
        assert tx.transactionStatusId == 'TX_DRAFT'
        
        when: "Fetching LC list for dashboard"
        def listResult = ec.service.sync().name("trade.importlc.ImportLcServices.get#ImportLetterOfCreditList")
            .parameters([instrumentId: instrumentId2]).call()
        def found = listResult.lcList.find { it.instrumentId == instrumentId2 }
        
        then: "Draft LC should be in the list"
        found != null
        found.instrumentRef == ref2
    }
}
