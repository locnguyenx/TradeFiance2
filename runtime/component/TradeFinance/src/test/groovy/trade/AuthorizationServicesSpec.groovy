package trade


import spock.lang.Specification
import org.moqui.Moqui
import org.moqui.context.ExecutionContext

// ABOUTME: AuthorizationServicesSpec verifies the Maker/Checker risk matrix, ensuring Maker cannot also be Checker.

class AuthorizationServicesSpec extends Specification {
    protected ExecutionContext ec
    
    def setupSpec() {
        ExecutionContext ec = Moqui.getExecutionContext()
        ec.artifactExecution.disableAuthz()
        ec.transaction.begin(null)
        try {
            def ids = ["AUTH-1", "AUTH-AMD", "PRIO-LOW", "PRIO-HIGH"]
            for (id in ids) {
                ec.entity.find("trade.TradeInstrument").condition("instrumentId", id).updateAll([latestTransactionId: null])
                ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", id).deleteAll()
                ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", id).deleteAll()
                ec.entity.find("trade.TradeTransaction").condition("instrumentId", id).deleteAll()
                ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", id).deleteAll()
                ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", id).deleteAll()
                ec.entity.find("trade.TradeInstrument").condition("instrumentId", id).deleteAll()
            }
            ec.entity.find("trade.UserAuthorityProfile").condition("userAuthorityId", "LIKE", "T1-%").deleteAll()
            ec.transaction.commit()
        } catch (Exception e) {
            ec.transaction.rollback(e.message, e)
            throw e
        } finally {
            ec.destroy()
        }
    }
    
    def setup() {
        ec = Moqui.getExecutionContext()
        ec.user.internalLoginUser("trade.maker")
        ec.artifactExecution.disableAuthz()
        // Load mandatory seed data for priority enums
        ec.entity.makeDataLoader().location("component://TradeFinance/data/TradeFinanceSeedData.xml").load()
    }
    
    def cleanup() {
        ec.destroy()
    }
    
    def "Test Maker/Checker matrix prohibits self-approval"() {
        setup:
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
                .parameters([instrumentId: "AUTH-1", instrumentRef: "TF-AUTH-01", businessStateId: "LC_DRAFT",
                             lcAmount: 10000.0, lcCurrencyUomId: 'USD',
                             instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                                 [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]]).call()
        
        // Find the transaction created by the service and update it for the test case
        def tx = ec.entity.find("trade.TradeTransaction").condition("instrumentId", "AUTH-1").one()
        tx.setAll([transactionStatusId: 'TX_PENDING', makerUserId: 'john.doe']).update()
        
        ec.entity.makeValue("trade.TradeTransactionAudit")
            .setAll([transactionId:tx.transactionId, instrumentId:"AUTH-1", auditId:"1", actionEnumId:"MAKER_COMMIT", userId:"john.doe"]).create()
        ec.entity.makeValue("trade.UserAuthorityProfile")
            .setAll([userAuthorityId:"T1-02", userId:"jane.doe", delegationTierId:"TIER_1", customLimit:100000.0, makerCheckerFlag: "CHECKER"]).create()
            
        when: "Checker is the same user"
        def resultSame = ec.service.sync().name("trade.AuthorizationServices.evaluate#MakerCheckerMatrix")
            .parameters([instrumentId:"AUTH-1", userId:"john.doe"]).call()
            
        then:
        resultSame.isAuthorized == false
        
        when: "Checker is a different user"
        def resultDiff = ec.service.sync().name("trade.AuthorizationServices.evaluate#MakerCheckerMatrix")
            .parameters([instrumentId:"AUTH-1", userId:"jane.doe"]).call()
            
        then:
        resultDiff.isAuthorized == true
        
        cleanup:
        if (ec != null) ec.artifactExecution.disableAuthz()
        ec.entity.find("trade.UserAuthorityProfile").condition("userAuthorityId", "T1-02").deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", "AUTH-1").updateAll([latestTransactionId: null])
        ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", "AUTH-1").deleteAll()
        ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", "AUTH-1").deleteAll()
        ec.entity.find("trade.TradeTransaction").condition("instrumentId", "AUTH-1").deleteAll()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", "AUTH-1").deleteAll()
        ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", "AUTH-1").deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", "AUTH-1").deleteAll()
    }
    
    def "BDD-CMN-AUTH-03: Tier Routing uses effectiveAmount for amendments"() {
        given: "An LC with amount exceeding limit"
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
                .parameters([instrumentId: "AUTH-AMD", instrumentRef: "TF-AUTH-03", businessStateId: "LC_ISSUED", 
                             lcAmount: 150000.0, lcCurrencyUomId: 'USD',
                             instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                                 [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]]).call()
        
        def tx = ec.entity.find("trade.TradeTransaction").condition("instrumentId", "AUTH-AMD").one()
        tx.setAll([transactionStatusId: 'TX_PENDING', makerUserId: 'maker.user']).update()
            
        and: "Audit record for maker"
        ec.entity.makeValue("trade.TradeTransactionAudit")
            .setAll([transactionId:tx.transactionId, instrumentId:"AUTH-AMD", auditId:"2", actionEnumId:"MAKER_COMMIT", userId:"maker.user"]).create()
            
        and: "Checker with Tier 1 limit (100k)"
        ec.entity.makeValue("trade.UserAuthorityProfile")
            .setAll([userAuthorityId:"T1-03", userId:"tier1.user", delegationTierId:"TIER_1", customLimit:100000.0, makerCheckerFlag: "CHECKER"]).create()

        when: "Tier 1 user tries to authorize a 150k effective amount"
        def result = ec.service.sync().name("trade.AuthorizationServices.evaluate#MakerCheckerMatrix")
            .parameters([instrumentId:"AUTH-AMD", userId:"tier1.user"]).call()
            
        then: "isAuthorized should be false because 150k > 100k"
        result.isAuthorized == false
        
        cleanup:
        if (ec != null) ec.artifactExecution.disableAuthz()
        ec.entity.find("trade.UserAuthorityProfile").condition("userAuthorityId", "T1-03").deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", "AUTH-AMD").updateAll([latestTransactionId: null])
        ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", "AUTH-AMD").deleteAll()
        ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", "AUTH-AMD").deleteAll()
        ec.entity.find("trade.TradeTransaction").condition("instrumentId", "AUTH-AMD").deleteAll()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", "AUTH-AMD").deleteAll()
        ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", "AUTH-AMD").deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", "AUTH-AMD").deleteAll()
    }

    def "BDD-CMN-AUTH-05: Priority Queue ordering sorts Urgent before Low"() {
        given: "Two instruments with different priorities"
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
                .parameters([instrumentId: "PRIO-LOW", instrumentRef: "TF-PRIO-01", businessStateId: "LC_DRAFT", priorityEnumId: "PRIO_LOW",
                             lcAmount: 1000.0, lcCurrencyUomId: 'USD',
                             instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                                 [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]]).call()
        
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
                .parameters([instrumentId: "PRIO-HIGH", instrumentRef: "TF-PRIO-02", businessStateId: "LC_DRAFT", priorityEnumId: "PRIO_URGENT",
                             lcAmount: 1000.0, lcCurrencyUomId: 'USD',
                             instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                                 [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]]).call()
        
        when: "Retrieving the list"
        def result = ec.service.sync().name("trade.importlc.ImportLcServices.get#ImportLetterOfCreditList").call()
        def list = result.lcList
        
        then: "Urgent (PRIO-HIGH) should be before Low (PRIO-LOW)"
        int highIdx = list.findIndexOf { it.instrumentId == "PRIO-HIGH" }
        int lowIdx = list.findIndexOf { it.instrumentId == "PRIO-LOW" }
        highIdx != -1
        lowIdx != -1
        highIdx < lowIdx
        
        cleanup:
        if (ec != null) ec.artifactExecution.disableAuthz()
        def ids = ["PRIO-LOW", "PRIO-HIGH", "AUTH-TEST"]
        for (id in ids) {
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", id).updateAll([latestTransactionId: null])
            ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", id).deleteAll()
            ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", id).deleteAll()
            ec.entity.find("trade.TradeTransaction").condition("instrumentId", id).deleteAll()
            ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", id).deleteAll()
            ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", id).deleteAll()
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", id).deleteAll()
        }
    }

    def "BDD-CMN-AUTH-06: Pending Approvals shows transaction amount, not parent amount"() {
        given: "An LC and an amendment"
        def uniqueId = "AUTH-" + System.currentTimeMillis()
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
                .parameters([instrumentId: uniqueId, instrumentRef: "TF-" + uniqueId, 
                             lcAmount: 5000.0, lcCurrencyUomId: 'USD',
                             instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                                 [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]]).call()
        
        // Approve issuance to allow amendment
        def txIss = ec.entity.find("trade.TradeTransaction")
                .condition([instrumentId: uniqueId, transactionTypeEnumId: 'IMP_NEW']).disableAuthz().one()
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument")
                .parameters([transactionId: txIss.transactionId, skipFourEyes: true]).call()
        ec.entity.find("trade.importlc.ImportLetterOfCredit")
                .condition("instrumentId", uniqueId).updateAll([businessStateId: "LC_ISSUED"])
        
        def amdOut = ec.service.sync().name("trade.importlc.ImportLcServices.create#Amendment")
                .parameters([instrumentId: uniqueId, amendmentTypeEnumId: 'AMEND_TYPE_AMOUNT', amountAdjustment: 1200.0, isFinancial: 'Y']).call()
        
        ec.service.sync().name("trade.importlc.ImportLcServices.submit#Amendment")
                .parameters([amendmentId: amdOut.amendmentId]).call()
        
        when: "Retrieving pending approvals"
        def result = ec.service.sync().name("trade.AuthorizationServices.get#PendingApprovals").call()
        def amdItem = result.approvalsList.find { it.instrumentId == uniqueId && it.action == "AMENDMENT" }
        
        then: "Amount should be the adjustment (1200), not the parent amount (5000)"
        amdItem != null
        amdItem.transactionAmount == 1200.0
    }
}
