package trade

import spock.lang.Specification
import spock.lang.Shared
import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityCondition

// ABOUTME: Reproduction spec for the transaction issuance bug.
// ABOUTME: Ensures that only one issuance transaction can exist and concurrent transactions are blocked.

class TransactionIssuanceBugSpec extends Specification {
    @Shared ExecutionContext ec
    @Shared String testPrefix

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.user.loginUser("trade.admin", "trade123")
        ec.artifactExecution.disableAuthz()
        testPrefix = "TX-ISS-BUG-" + System.currentTimeMillis()
        
        // Setup unique Party for this spec
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
            .parameters([partyId: testPrefix + '_APP', partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'App Bug', kycStatus: 'KYC_ACTIVE']).call()
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
            .parameters([partyId: testPrefix + '_BEN', partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'Ben Bug', kycStatus: 'KYC_ACTIVE']).call()
        cleanData()
    }

    def cleanupSpec() {
        try {
            if (ec != null) cleanData()
        } finally {
            if (ec != null) ec.destroy()
        }
    }

    def setup() {
        ec.message.clearAll()
        ec.artifactExecution.disableAuthz()
    }

    def cleanup() {
        ec.message.clearAll()
    }

    private void cleanData() {
        boolean begun = ec.transaction.begin(60)
        try {
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").updateAll([latestTransactionId: null])
            ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeApprovalRecord").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeTransaction").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.transaction.commit(begun)
        } catch (Exception e) {
            ec.transaction.rollback(begun, "Error in cleanData", e)
        }
    }
    
    def "should NOT create new IMP_NEW transaction for an already issued LC during update"() {
        given:
        def instrumentId = testPrefix + "_LC_01"
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            instrumentId: instrumentId, instrumentRef: instrumentId + "-REF",
            lcAmount: 100000.0, lcCurrencyUomId: 'USD',
            instrumentParties: [[partyId: testPrefix + '_APP', roleEnumId: 'TP_APPLICANT'], [partyId: testPrefix + '_BEN', roleEnumId: 'TP_BENEFICIARY']]
        ]).call()
        
        ec.service.sync().name("trade.importlc.ImportLcServices.approve#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, approverUserId: 'trade.checker']).call()
            
        def txCountBefore = ec.entity.find("trade.TradeTransaction")
            .condition([instrumentId: instrumentId, transactionTypeEnumId: 'IMP_NEW']).count()
        assert txCountBefore == 1
        
        def instrumentBefore = ec.entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId).one()
        def versionBefore = instrumentBefore.versionNumber ?: 1
        
        when: "Update is called"
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, skipImmutabilityGuard: true]).call()
            
        then: "No new transaction was created"
        def txCountAfter = ec.entity.find("trade.TradeTransaction")
            .condition([instrumentId: instrumentId, transactionTypeEnumId: 'IMP_NEW']).count()
        txCountAfter == 1
    }

    def "should NOT allow concurrent in-progress transactions for the same LC"() {
        given:
        def instrumentId = testPrefix + "_LC_02"
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            instrumentId: instrumentId, instrumentRef: instrumentId + "-REF",
            lcAmount: 100000.0, lcCurrencyUomId: 'USD',
            instrumentParties: [[partyId: testPrefix + '_APP', roleEnumId: 'TP_APPLICANT'], [partyId: testPrefix + '_BEN', roleEnumId: 'TP_BENEFICIARY']]
        ]).call()
        
        when: "Attempting to create a second transaction for the same instrument"
        ec.service.sync().name("create#trade.TradeTransaction").requireNewTransaction(true).parameters([
            instrumentId: instrumentId, transactionRef: instrumentId + "-TX2",
            transactionTypeEnumId: "IMP_AMENDMENT", transactionStatusId: "TX_DRAFT"
        ]).call()
        
        then: "Fails due to concurrent transaction guard"
        ec.message.hasError()
        ec.message.getErrorsString().contains("already has a transaction in progress")
    }

    def "should NOT allow creating a second IMP_NEW transaction even if the first is approved"() {
        given:
        def instrumentId = testPrefix + "_LC_03"
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            instrumentId: instrumentId, instrumentRef: instrumentId + "-REF",
            lcAmount: 100000.0, lcCurrencyUomId: 'USD',
            instrumentParties: [[partyId: testPrefix + '_APP', roleEnumId: 'TP_APPLICANT'], [partyId: testPrefix + '_BEN', roleEnumId: 'TP_BENEFICIARY']]
        ]).call()
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
        errors.contains("already been initiated") || errors.contains("marked for rollback")
    }
}
