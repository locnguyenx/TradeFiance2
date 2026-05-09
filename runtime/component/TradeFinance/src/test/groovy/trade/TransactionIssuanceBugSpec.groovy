package trade

import spock.lang.Specification
import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import java.sql.Date

// ABOUTME: Reproduction spec for the transaction issuance bug.
class TransactionIssuanceBugSpec extends Specification {
    protected ExecutionContext ec
    
    def setup() {
        ec = Moqui.getExecutionContext()
        ec.artifactExecution.disableAuthz()
        
        // Ensure users exist
        if (ec.entity.find("moqui.security.UserAccount").condition("username", "trade.admin").count() == 0) {
            ec.entity.makeValue("moqui.security.UserAccount")
                .setAll([userId: "trade.admin", username: "trade.admin", currentPassword: "trade123", firstName: "Trade", lastName: "Admin"])
                .create()
        }
        if (ec.entity.find("trade.UserAuthorityProfile").condition("userId", "trade.admin").count() == 0) {
            ec.entity.makeValue("trade.UserAuthorityProfile")
                .setAll([userAuthorityId: "T1-BUG", userId: "trade.admin", delegationTierId: "TIER_1", customLimit: 10000000.00, currencyUomId: "USD", makerCheckerFlag: "MAKER_CHECKER"])
                .create()
        }
        if (ec.entity.find("moqui.security.UserAccount").condition("username", "trade.checker").count() == 0) {
            ec.entity.makeValue("moqui.security.UserAccount")
                .setAll([userId: "trade.checker", username: "trade.checker", currentPassword: "trade123", firstName: "Trade", lastName: "Checker"])
                .create()
        }
        if (ec.entity.find("trade.UserAuthorityProfile").condition("userId", "trade.checker").count() == 0) {
            ec.entity.makeValue("trade.UserAuthorityProfile")
                .setAll([userAuthorityId: "T2-BUG", userId: "trade.checker", delegationTierId: "TIER_2", customLimit: 10000000.00, currencyUomId: "USD", makerCheckerFlag: "CHECKER"])
                .create()
        }
        
        ec.user.loginUser("trade.admin", "trade123")
    }
    
    def cleanup() {
        ec.destroy()
    }
    
    def "should NOT create new IMP_NEW transaction for an already issued LC during update"() {
        given:
        // 1. Create LC
        def createResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            lcAmount: 100000.0, lcCurrencyUomId: 'USD',
            instrumentParties: [[partyId: 'ACME_CORP_001', roleEnumId: 'TP_APPLICANT'], [partyId: 'GLOBAL_EXP_002', roleEnumId: 'TP_BENEFICIARY']]
        ]).call()
        String instrumentId = createResult.instrumentId
        
        // 2. Approve LC (status becomes LC_ISSUED)
        ec.service.sync().name("trade.importlc.ImportLcServices.approve#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, approverUserId: 'trade.checker']).call()
            
        def lcAfterApprove = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        assert lcAfterApprove.businessStateId == "LC_ISSUED"
        
        def txCountBefore = ec.entity.find("trade.TradeTransaction")
            .condition([instrumentId: instrumentId, transactionTypeEnumId: 'IMP_NEW']).count()
        assert txCountBefore == 1
        
        def instrumentBefore = ec.entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId).one()
        assert instrumentBefore.versionNumber >= 1
        def versionBefore = instrumentBefore.versionNumber
        
        when:
        // 3. Call update with skipImmutabilityGuard (simulating amendment or internal update)
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, skipImmutabilityGuard: true]).call()
            
        then:
        def instrumentAfter = ec.entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId).one()
        instrumentAfter.versionNumber == versionBefore + 1
        
        def txCountAfter = ec.entity.find("trade.TradeTransaction")
            .condition([instrumentId: instrumentId, transactionTypeEnumId: 'IMP_NEW']).count()
        
        // If the bug is present, txCountAfter will be 2
        txCountAfter == 1
    }

    def "should NOT allow concurrent in-progress transactions for the same LC"() {
        given:
        // 1. Create LC (creates one IMP_NEW in TX_DRAFT)
        def createResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            lcAmount: 100000.0, lcCurrencyUomId: 'USD',
            instrumentParties: [[partyId: 'ACME_CORP_001', roleEnumId: 'TP_APPLICANT'], [partyId: 'GLOBAL_EXP_002', roleEnumId: 'TP_BENEFICIARY']]
        ]).call()
        String instrumentId = createResult.instrumentId
        
        when:
        // 2. Attempt to create a second transaction for the same instrument
        ec.service.sync().name("create#trade.TradeTransaction").parameters([
            instrumentId: instrumentId,
            transactionTypeEnumId: "IMP_AMENDMENT",
            transactionStatusId: "TX_DRAFT"
        ]).call()
        
        then:
        ec.message.hasError()
        ec.message.getErrorsString().contains("already has a transaction in progress")
    }

    def "should NOT allow creating a second IMP_NEW transaction even if the first is approved"() {
        given:
        // 1. Create and Approve LC
        def createResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            lcAmount: 100000.0, lcCurrencyUomId: 'USD',
            instrumentParties: [[partyId: 'ACME_CORP_001', roleEnumId: 'TP_APPLICANT'], [partyId: 'GLOBAL_EXP_002', roleEnumId: 'TP_BENEFICIARY']]
        ]).call()
        String instrumentId = createResult.instrumentId
        ec.service.sync().name("trade.importlc.ImportLcServices.approve#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, approverUserId: 'trade.checker']).call()
            
        when:
        // 2. Attempt to create a second IMP_NEW transaction
        ec.service.sync().name("create#trade.TradeTransaction").parameters([
            instrumentId: instrumentId,
            transactionTypeEnumId: "IMP_NEW",
            transactionStatusId: "TX_DRAFT"
        ]).call()
        
        then:
        ec.message.hasError()
        ec.message.getErrorsString().contains("already been initiated")
    }
}
