package trade

import org.moqui.context.ExecutionContext
import org.moqui.Moqui
import org.moqui.entity.EntityCondition
import spock.lang.Specification
import spock.lang.Shared

/**
 * ABOUTME: DualApprovalSpec verifies the multi-step approval workflow for Tier 4 transactions.
 * It ensures that high-value transactions require two unique checkers before being finalized.
 */
class DualApprovalSpec extends Specification {
    @Shared protected ExecutionContext ec
    @Shared String testPrefix
    
    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.transaction.runUseOrBegin(null, null) {
            ec.artifactExecution.disableAuthz()
            ec.user.loginUser("trade.maker", "trade123")
            testPrefix = "DUAL-APP-" + System.currentTimeMillis()
            
            // Ensure test users exist with TIER_4 authority
            ["CK1", "CK2"].each { suffix ->
                def userId = testPrefix + "-" + suffix
                if (ec.entity.find("moqui.security.UserAccount").condition("userId", userId).count() == 0) {
                    ec.entity.makeValue("moqui.security.UserAccount").setAll([userId: userId, username: userId]).create()
                }
                ec.entity.makeValue("trade.UserAuthorityProfile").setAll([
                    userId: userId, delegationTierId: "TIER_4", customLimit: 10000000.0, makerCheckerFlag: "CHECKER"
                ]).setSequencedIdPrimary().create()
            }

            // Set isolated ID generation ranges - use 11000000
            ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrument", 11000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit", 11000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.TradeTransaction", 11000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrumentParty", 11000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.TradeApprovalRecord", 11000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.TradeTransactionAudit", 11000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.UserAuthorityProfile", 11000000, 1000)
        }
    }

    def cleanupSpec() {
        if (ec != null) {
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrument")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeTransaction")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrumentParty")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeApprovalRecord")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeTransactionAudit")
            ec.destroy()
        }
    }

    def setup() {
        if (ec.transaction.isTransactionInPlace()) ec.transaction.rollback("Cleanup", null)
        ec.message.clearAll()
        ec.artifactExecution.disableAuthz()
    }
    
    def cleanup() {
        ec.message.clearAll()
    }
    
    def "BDD-CMN-AUTH-02: Dual Checker Enforcement for Tier 4"() {
        given: "A Tier 4 transaction (2M USD LC)"
        def ck1 = testPrefix + "-CK1"
        def ck2 = testPrefix + "-CK2"

        def instRes = ec.entity.makeValue("trade.TradeInstrument").setAll([
            instrumentRef: testPrefix + "-REF-01", instrumentTypeEnumId: 'IMPORT_LC',
            amount: 2000000, currencyUomId: 'USD', versionNumber: 1
        ]).setSequencedIdPrimary().create()
        def instId = instRes.instrumentId

        ec.entity.makeValue("trade.importlc.ImportLetterOfCredit").setAll([
            instrumentId: instId, effectiveAmount: 2000000, businessStateId: 'LC_DRAFT',
            lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_NEGOTIATION', confirmationEnumId: 'CONF_WITHOUT'
        ]).create()
        
        // Create parties
        def applicantId = testPrefix + '_APP'
        def beneficiaryId = testPrefix + '_BEN'
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
            .parameters([partyId: applicantId, partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'Dual ACME Corp', kycStatus: 'KYC_ACTIVE']).call()
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
            .parameters([partyId: beneficiaryId, partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'Dual Global Exports', kycStatus: 'KYC_ACTIVE']).call()

        // Add parties
        ec.entity.makeValue("trade.TradeInstrumentParty").setAll([instrumentId: instId, partyId: applicantId, roleEnumId: 'TP_APPLICANT']).create()
        ec.entity.makeValue("trade.TradeInstrumentParty").setAll([instrumentId: instId, partyId: beneficiaryId, roleEnumId: 'TP_BENEFICIARY']).create()
        
        // Record TradeTransaction
        def txRes = ec.entity.makeValue("trade.TradeTransaction").setAll([
            instrumentId: instId, transactionStatusId: 'TX_PENDING',
            transactionTypeEnumId: 'IMP_NEW',
            makerUserId: "trade.maker", versionNumber: 1
        ]).setSequencedIdPrimary().create()
        def txId = txRes.transactionId

        // Record maker audit
        ec.entity.makeValue("trade.TradeTransactionAudit").setAll([
            transactionId: txId, instrumentId: instId, actionEnumId: "MAKER_COMMIT", userId: "trade.maker"
        ]).set("auditId", ec.entity.sequencedIdPrimary("trade.TradeTransactionAudit", null, null)).create()

        when: "First checker approves"
        ec.user.internalLoginUser(ck1)
        ec.service.sync().name("trade.importlc.ImportLcServices.approve#ImportLetterOfCredit")
            .parameters([instrumentId: instId, approvalComments: "First checker look good"]).call()

        then: "Status remains TX_PENDING and approval record is created"
        !ec.message.hasError()
        def trans1 = ec.entity.find("trade.TradeTransaction").condition("instrumentId", instId).one()
        trans1.transactionStatusId == "TX_PENDING"
        def approvals1 = ec.entity.find("trade.TradeApprovalRecord").condition("instrumentId", instId).list()
        approvals1.size() == 1
        approvals1[0].approverUserId == ck1

        when: "First checker tries to approve again"
        ec.message.clearAll()
        ec.service.sync().name("trade.importlc.ImportLcServices.approve#ImportLetterOfCredit")
            .parameters([instrumentId: instId, approvalComments: "Trying again"]).call()

        then: "Fails with error"
        ec.message.hasError()
        ec.message.getErrorsString().contains("already approved this version")
        ec.message.clearAll()

        when: "Second unique checker approves"
        ec.user.internalLoginUser(ck2)
        ec.service.sync().name("trade.importlc.ImportLcServices.approve#ImportLetterOfCredit")
            .parameters([instrumentId: instId, approvalComments: "Second checker OK"]).call()

        then: "Status transitions to TX_APPROVED and business state to LC_ISSUED"
        !ec.message.hasError()
        def trans2 = ec.entity.find("trade.TradeTransaction").condition("instrumentId", instId).one()
        trans2.transactionStatusId == "TX_APPROVED"
        def lc2 = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instId).one()
        lc2.businessStateId == "LC_ISSUED"
    }
}
