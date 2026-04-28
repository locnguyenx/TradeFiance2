package trade

import org.moqui.context.ExecutionContext
import org.moqui.Moqui
import spock.lang.Specification

/*
ABOUTME: DualApprovalSpec verifies the multi-step approval workflow for Tier 4 transactions.
It ensures that high-value transactions require two unique checkers before being finalized.
*/
class DualApprovalSpec extends Specification {
    ExecutionContext ec

    def setup() {
        ec = Moqui.getExecutionContext()
        ec.artifactExecution.disableAuthz()
        
        // Load mandatory seed data
        ec.entity.makeDataLoader().location("component://TradeFinance/data/TradeFinanceSeedData.xml").load()
        
        ec.transaction.begin(null)
        try {
            // Clean up dependencies
            String instId = "DUAL-APP-LC-01"
            ec.entity.find("trade.TradeApprovalRecord").condition("instrumentId", instId).deleteAll()
            ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", instId).deleteAll()
            ec.entity.find("trade.TradeTransaction").condition("instrumentId", instId).deleteAll()
            ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instId).deleteAll()
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", instId).deleteAll()

            // Setup Users and Authority Profiles
            ec.entity.find("trade.UserAuthorityProfile").condition("userId", "in", ["trade.maker", "trade.checker1", "trade.checker2"]).deleteAll()
            ec.entity.find("moqui.security.UserAccount").condition("userId", "in", ["trade.maker", "trade.checker1", "trade.checker2"]).deleteAll()
            
            ec.entity.makeValue("moqui.security.UserAccount").setAll([userId: "trade.maker", username: "trade.maker"]).create()
            ec.entity.makeValue("moqui.security.UserAccount").setAll([userId: "trade.checker1", username: "trade.checker1"]).create()
            ec.entity.makeValue("moqui.security.UserAccount").setAll([userId: "trade.checker2", username: "trade.checker2"]).create()

            ec.entity.makeValue("trade.UserAuthorityProfile").setAll([
                userAuthorityId: "PROFILE_MAKER", userId: "trade.maker", delegationTierId: "TIER_1", customLimit: 100000.0, makerCheckerFlag: "MAKER"
            ]).create()
            ec.entity.makeValue("trade.UserAuthorityProfile").setAll([
                userAuthorityId: "PROFILE_CK1", userId: "trade.checker1", delegationTierId: "TIER_4", customLimit: 10000000.0, makerCheckerFlag: "CHECKER"
            ]).create()
            ec.entity.makeValue("trade.UserAuthorityProfile").setAll([
                userAuthorityId: "PROFILE_CK2", userId: "trade.checker2", delegationTierId: "TIER_4", customLimit: 10000000.0, makerCheckerFlag: "CHECKER"
            ]).create()
            
            ec.transaction.commit()
        } catch (Exception e) {
            ec.transaction.rollback("Error in setup", e)
            throw e
        }
    }

    def cleanup() {
        ec.destroy()
    }

    def "BDD-CMN-AUTH-02: Dual Checker Enforcement for Tier 4"() {
        given: "A Tier 4 transaction (2M USD LC)"
        String instId = "DUAL-APP-LC-01"
        ec.entity.makeValue("trade.TradeInstrument").setAll([
            instrumentId: instId, transactionRef: "DUAL-01", instrumentTypeEnumId: 'IMPORT_LC',
            transactionStatusId: 'TX_PENDING', businessStateId: 'LC_DRAFT',
            amount: 2000000, currencyUomId: 'USD', versionNumber: 1
        ]).create()
        ec.entity.makeValue("trade.importlc.ImportLetterOfCredit").setAll([
            instrumentId: instId, effectiveAmount: 2000000, businessStateId: 'LC_DRAFT'
        ]).create()
        
        // Record TradeTransaction
        ec.entity.makeValue("trade.TradeTransaction").setAll([
            transactionId: "TX-DUAL-01", instrumentId: instId, transactionStatusId: 'TX_PENDING',
            transactionTypeEnumId: 'IMP_NEW',
            makerUserId: "trade.maker", versionNumber: 1
        ]).create()

        // Record maker audit
        ec.entity.makeValue("trade.TradeTransactionAudit").setAll([
            transactionId: "TX-DUAL-01", instrumentId: instId, auditId: "AUDIT-M1", actionEnumId: "MAKER_COMMIT", userId: "trade.maker"
        ]).create()

        when: "First checker approves"
        ec.user.internalLoginUser("trade.checker1")
        def res1 = ec.service.sync().name("trade.importlc.ImportLcServices.approve#ImportLetterOfCredit")
            .parameters([instrumentId: instId, approvalComments: "First checker look good"]).call()

        then: "Status remains TX_PENDING and approval record is created"
        if (ec.message.hasError()) {
            println "ERROR in first approval: " + ec.message.getErrorsString()
            ec.message.clearAll()
            assert false, "First approval failed"
        }
        def trans1 = ec.entity.find("trade.TradeTransaction").condition("instrumentId", instId).one()
        trans1.transactionStatusId == "TX_PENDING"
        def approvals1 = ec.entity.find("trade.TradeApprovalRecord").condition("instrumentId", instId).list()
        approvals1.size() == 1
        approvals1[0].approverUserId == "trade.checker1"

        when: "First checker tries to approve again"
        boolean caught = false
        try {
            ec.service.sync().name("trade.importlc.ImportLcServices.approve#ImportLetterOfCredit")
                .parameters([instrumentId: instId, approvalComments: "Trying again"]).call()
        } catch (org.moqui.service.ServiceException e) {
            caught = true
        }

        then: "Fails with error"
        caught || ec.message.hasError()
        ec.message.clearAll()

        when: "Second checker (different user) approves"
        ec.user.internalLoginUser("trade.checker2")
        ec.service.sync().name("trade.importlc.ImportLcServices.approve#ImportLetterOfCredit")
            .parameters([instrumentId: instId, approvalComments: "Second checker approved"]).call()

        then: "Status becomes TX_APPROVED"
        def trans2 = ec.entity.find("trade.TradeTransaction").condition("instrumentId", instId).useCache(false).one()
        trans2.refresh()
        if (trans2.transactionStatusId != "TX_APPROVED") {
            ec.logger.info("VERIFY_FAIL: Expected TX_APPROVED, got ${trans2.transactionStatusId} for ${trans2.instrumentId}")
            ec.logger.info("VERIFY_FAIL_MAP: ${trans2.getMap()}")
        }
        trans2.transactionStatusId == "TX_APPROVED"
        trans2.checkerUserId == "trade.checker2"
        def approvals2 = ec.entity.find("trade.TradeApprovalRecord").condition("instrumentId", instId).list()
        approvals2.size() == 2
        approvals2.any { it.approverUserId == "trade.checker1" }
        approvals2.any { it.approverUserId == "trade.checker2" }
        
        cleanup:
        ec.entity.find("trade.TradeApprovalRecord").condition("instrumentId", instId).deleteAll()
        ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", instId).deleteAll()
        ec.entity.find("trade.TradeTransaction").condition("instrumentId", instId).deleteAll()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instId).deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", instId).deleteAll()
    }
}
