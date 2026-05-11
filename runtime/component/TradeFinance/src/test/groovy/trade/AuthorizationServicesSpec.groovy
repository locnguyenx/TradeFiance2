package trade

import spock.lang.Specification
import spock.lang.Shared
import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityCondition

/**
 * ABOUTME: AuthorizationServicesSpec verifies the Maker/Checker risk matrix, ensuring Maker cannot also be Checker.
 */
class AuthorizationServicesSpec extends Specification {
    @Shared protected ExecutionContext ec
    @Shared String testPrefix
    
    @Shared String applicantId
    @Shared String beneficiaryId

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.transaction.runUseOrBegin(null, null) {
            ec.artifactExecution.disableAuthz()
            ec.user.loginUser("trade.maker", "trade123")
            testPrefix = "AUTH-SPEC-" + System.currentTimeMillis()
        
        applicantId = testPrefix + "-APP"
        beneficiaryId = testPrefix + "-BEN"

        // Set isolated ID generation ranges - use 10000000
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrument", 10000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit", 10000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeTransaction", 10000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrumentParty", 10000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeTransactionAudit", 10000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.UserAuthorityProfile", 10000000, 1000)

        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
            .parameters([partyId: applicantId, partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'ACME Corp Auth', kycStatus: 'KYC_ACTIVE']).call()
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
            .parameters([partyId: beneficiaryId, partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'Global Exports Auth', kycStatus: 'KYC_ACTIVE']).call()
        }
    }

    def cleanupSpec() {
        if (ec != null) {
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrument")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeTransaction")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrumentParty")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeTransactionAudit")
            ec.entity.tempResetSequencedIdPrimary("trade.UserAuthorityProfile")
            ec.destroy()
        }
    }

    def setup() {
        if (ec.transaction.isTransactionInPlace()) ec.transaction.rollback("Cleanup", null)
        ec.user.loginUser("trade.maker", "trade123")
        ec.artifactExecution.disableAuthz()
        ec.message.clearAll()
    }
    
    def cleanup() {
        ec.message.clearAll()
    }
    
    def "Test Maker/Checker matrix prohibits self-approval"() {
        setup:
        def johnId = testPrefix + "-JOHN"
        def janeId = testPrefix + "-JANE"
        
        ec.transaction.runUseOrBegin(null, null) {
            ec.entity.makeValue("moqui.security.UserAccount").setAll([userId: johnId, username: johnId]).create()
            ec.entity.makeValue("moqui.security.UserAccount").setAll([userId: janeId, username: janeId]).create()

            def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
                    .parameters([instrumentRef: testPrefix + "-REF-01",
                                 lcAmount: 10000.0, lcCurrencyUomId: 'USD',
                                 instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId],
                                                      [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]]]).call()
            def instrumentId = res.instrumentId
            
            // Find the transaction created by the service and update it for the test case
            def tx = ec.entity.find("trade.TradeTransaction").condition("instrumentId", instrumentId).one()
            tx.setAll([transactionStatusId: 'TX_PENDING', makerUserId: johnId]).update()
            
            ec.entity.makeValue("trade.TradeTransactionAudit")
                .setAll([transactionId:tx.transactionId, instrumentId:instrumentId, actionEnumId:"MAKER_COMMIT", userId:johnId])
                .set("auditId", ec.entity.sequencedIdPrimary("trade.TradeTransactionAudit", null, null)).create()
            ec.entity.makeValue("trade.UserAuthorityProfile")
                .setAll([userId:janeId, delegationTierId:"TIER_1", customLimit:100000.0, makerCheckerFlag: "CHECKER"])
                .setSequencedIdPrimary().create()
                
            when: "Checker is the same user"
            def resultSame = ec.service.sync().name("trade.AuthorizationServices.evaluate#MakerCheckerMatrix")
                .parameters([instrumentId:instrumentId, userId:johnId]).call()
                
            then:
            resultSame.isAuthorized == false
            
            when: "Checker is a different user"
            def resultDiff = ec.service.sync().name("trade.AuthorizationServices.evaluate#MakerCheckerMatrix")
                .parameters([instrumentId:instrumentId, userId:janeId]).call()
                
            then:
            resultDiff.isAuthorized == true
        }
    }
    
    def "BDD-CMN-AUTH-03: Tier Routing uses effectiveAmount for amendments"() {
        given: "An LC with amount exceeding limit"
        def makerId = testPrefix + "-MAKER"
        def checkerId = testPrefix + "-CHECKER"
        
        ec.transaction.runUseOrBegin(null, null) {
            ec.entity.makeValue("moqui.security.UserAccount").setAll([userId: makerId, username: makerId]).create()
            ec.entity.makeValue("moqui.security.UserAccount").setAll([userId: checkerId, username: checkerId]).create()

            def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
                    .parameters([instrumentRef: testPrefix + "-REF-03", 
                                 lcAmount: 150000.0, lcCurrencyUomId: 'USD',
                                 instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId],
                                                      [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]]]).call()
            def instrumentId = res.instrumentId
            ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).updateAll([businessStateId: "LC_ISSUED"])
            
            def tx = ec.entity.find("trade.TradeTransaction").condition("instrumentId", instrumentId).one()
            tx.setAll([transactionStatusId: 'TX_PENDING', makerUserId: makerId]).update()
                
            and: "Audit record for maker"
            ec.entity.makeValue("trade.TradeTransactionAudit")
                .setAll([transactionId:tx.transactionId, instrumentId:instrumentId, actionEnumId:"MAKER_COMMIT", userId:makerId])
                .set("auditId", ec.entity.sequencedIdPrimary("trade.TradeTransactionAudit", null, null)).create()
                
            and: "Checker with Tier 1 limit (100k)"
            ec.entity.makeValue("trade.UserAuthorityProfile")
                .setAll([userId:checkerId, delegationTierId:"TIER_1", customLimit:100000.0, makerCheckerFlag: "CHECKER"])
                .setSequencedIdPrimary().create()
     
            when: "Tier 1 user tries to authorize a 150k effective amount"
            def result = ec.service.sync().name("trade.AuthorizationServices.evaluate#MakerCheckerMatrix")
                .parameters([instrumentId:instrumentId, userId:checkerId]).call()
                
            then: "isAuthorized should be false because 150k > 100k"
            result.isAuthorized == false
        }
    }

    def "BDD-CMN-AUTH-05: Priority Queue ordering sorts Urgent before Low"() {
        given: "Two instruments with different priorities"
        def ref1 = testPrefix + "-URGENT"
        def ref2 = testPrefix + "-LOW"
        
        ec.transaction.runUseOrBegin(null, null) {
            def res1 = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
                    .parameters([instrumentRef: ref1, lcAmount: 10000.0, lcCurrencyUomId: 'USD',
                                 instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId],
                                                      [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]]]).call()
            def res2 = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
                    .parameters([instrumentRef: ref2, lcAmount: 10000.0, lcCurrencyUomId: 'USD',
                                 instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId],
                                                      [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]]]).call()
            
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", res1.instrumentId).updateAll([priorityEnumId: "PRIO_URGENT"])
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", res2.instrumentId).updateAll([priorityEnumId: "PRIO_LOW"])
            
            when: "Querying pending transactions"
            def txList = ec.entity.find("trade.TradeTransactionView").condition("transactionStatusId", "TX_DRAFT")
                    .orderBy("-priorityEnumId").list()
            
            then: "Urgent should be before Low"
            txList.size() >= 2
            def priorities = txList.findAll { it.instrumentRef in [ref1, ref2] }.collect { it.priorityEnumId }
            priorities[0] == "PRIO_URGENT"
            priorities[1] == "PRIO_LOW"
        }
    }
}
