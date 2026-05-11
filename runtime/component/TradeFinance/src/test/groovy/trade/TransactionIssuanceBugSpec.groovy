package trade

import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import spock.lang.Specification
import spock.lang.Shared
import org.moqui.entity.EntityCondition

/**
 * ABOUTME: TransactionIssuanceBugSpec verifies the fix for the duplicate IMP_NEW transaction bug.
 * Ensures that an instrument cannot have multiple issuance transactions and that state transitions are guarded.
 */
class TransactionIssuanceBugSpec extends Specification {
    @Shared ExecutionContext ec
    @Shared String testPrefix

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.transaction.runUseOrBegin(null, null) {
            println "DEBUG: setupSpec TransactionIssuanceBugSpec starting"
            ec.user.loginUser("trade.maker", "trade123")
            ec.artifactExecution.disableAuthz()
            testPrefix = "TX-ISS-BUG-" + System.currentTimeMillis()

            // Set isolated ID generation ranges - use 9900000
            ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrument", 53000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit", 53000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.TradeTransaction", 53000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrumentParty", 53000000, 1000)
            
            // Ensure test parties exist
            ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: testPrefix + '_APP', partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'Maker Corp', kycStatus: 'KYC_ACTIVE']).call()
            ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: testPrefix + '_BEN', partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'Beneficiary Corp', kycStatus: 'KYC_ACTIVE']).call()
            
            println "DEBUG: setupSpec TransactionIssuanceBugSpec complete"
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
        if (ec.transaction.isTransactionInPlace()) ec.transaction.rollback("Cleanup from previous failed state", null)
        ec.user.loginUser("trade.maker", "trade123")
        ec.message.clearAll()
        ec.artifactExecution.disableAuthz()
    }

    def cleanup() {
        if (ec.transaction.isTransactionInPlace()) ec.transaction.rollback("End of test isolation", null)
    }

    def "should NOT create new IMP_NEW transaction for an already issued LC during update"() {
        given: "An issued LC"
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            instrumentRef: testPrefix + "_LC_01_REF",
            lcAmount: 50000.0, lcCurrencyUomId: 'USD',
            instrumentParties: [[partyId: testPrefix + '_APP', roleEnumId: 'TP_APPLICANT'], [partyId: testPrefix + '_BEN', roleEnumId: 'TP_BENEFICIARY']]
        ]).call()
        def instrumentId = res.instrumentId
        
        // Advance to ISSUED
        ec.service.sync().name("trade.importlc.ImportLcServices.approve#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, approverUserId: 'trade.checker']).call()
        
        when: "Maker attempts to update LC (triggering re-issuance guard)"
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, lcAmount: 60000.0]).call()
        
        then: "Fails because LC is already issued"
        ec.message.hasError()
        ec.message.getErrorsString().contains("Issued LC")
    }

    def "should NOT allow concurrent in-progress transactions for the same LC"() {
        given: "A draft LC"
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            instrumentRef: testPrefix + "_LC_02_REF",
            lcAmount: 75000.0, lcCurrencyUomId: 'USD',
            instrumentParties: [[partyId: testPrefix + '_APP', roleEnumId: 'TP_APPLICANT'], [partyId: testPrefix + '_BEN', roleEnumId: 'TP_BENEFICIARY']]
        ]).call()
        def instrumentId = res.instrumentId

        when: "Maker attempts to create a second transaction while the first is TX_DRAFT"
        ec.service.sync().name("create#trade.TradeTransaction").requireNewTransaction(true).parameters([
            instrumentId: instrumentId, transactionRef: instrumentId + "-TX2",
            transactionTypeEnumId: "IMP_NEW", transactionStatusId: "TX_DRAFT"
        ]).call()
        
        then: "Fails due to concurrent transaction guard"
        ec.message.hasError()
        ec.message.getErrorsString().contains("already has a transaction in progress")
    }

    def "should NOT allow creating a second IMP_NEW transaction even if the first is approved"() {
        given:
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            instrumentRef: testPrefix + "_LC_03_REF",
            lcAmount: 100000.0, lcCurrencyUomId: 'USD',
            instrumentParties: [[partyId: testPrefix + '_APP', roleEnumId: 'TP_APPLICANT'], [partyId: testPrefix + '_BEN', roleEnumId: 'TP_BENEFICIARY']]
        ]).call()
        if (res == null) {
            throw new Exception("res is null! errors: " + ec.message.errorsString)
        }
        def instrumentId = res.instrumentId
        ec.service.sync().name("trade.importlc.ImportLcServices.approve#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, approverUserId: 'trade.checker']).call()
            
        when: "Attempting to create a second IMP_NEW transaction"
        ec.service.sync().name("create#trade.TradeTransaction").requireNewTransaction(true).parameters([
            instrumentId: instrumentId, transactionRef: instrumentId + "-TX2",
            transactionTypeEnumId: "IMP_NEW", transactionStatusId: "TX_DRAFT"
        ]).call()
        
        then: "Fails due to duplication guard"
        ec.message.hasError()
        def errors = ec.message.getErrorsString()
        println "DEBUG: TransactionIssuanceBugSpec errors: ${errors}"
        errors.contains("already been initiated") || errors.contains("marked for rollback")
    }
}
