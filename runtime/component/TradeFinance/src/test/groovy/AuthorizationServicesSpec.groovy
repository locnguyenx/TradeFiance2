
import spock.lang.Specification
import org.moqui.Moqui
import org.moqui.context.ExecutionContext

// ABOUTME: AuthorizationServicesSpec verifies the Maker/Checker risk matrix, ensuring Maker cannot also be Checker.

class AuthorizationServicesSpec extends Specification {
    protected ExecutionContext ec
    
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
        ec.entity.makeValue("trade.TradeInstrument")
            .setAll([instrumentId:"AUTH-1", transactionRef:"TF-AUTH-01", instrumentTypeEnumId: 'IMPORT_LC']).create()
        ec.entity.makeValue("trade.TradeTransactionAudit")
            .setAll([instrumentId:"AUTH-1", auditId:"1", actionEnumId:"MAKER_COMMIT", userId:"john.doe"]).create()
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
        ec.entity.find("trade.UserAuthorityProfile").condition("userAuthorityId", "T1-02").deleteAll()
        ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", "AUTH-1").deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", "AUTH-1").deleteAll()
    }
    
    def "BDD-CMN-AUTH-03: Tier Routing uses effectiveAmount for amendments"() {
        given: "An LC with effectiveAmount reflecting total new value"
        ec.entity.makeValue("trade.TradeInstrument")
            .setAll([instrumentId:"AUTH-AMD", transactionRef:"TF-AUTH-03", amount:100000.0, instrumentTypeEnumId: 'IMPORT_LC']).create()
        ec.entity.makeValue("trade.importlc.ImportLetterOfCredit")
            .setAll([instrumentId:"AUTH-AMD", effectiveAmount:150000.0]).create()
        
        and: "Audit record for maker"
        ec.entity.makeValue("trade.TradeTransactionAudit")
            .setAll([instrumentId:"AUTH-AMD", auditId:"2", actionEnumId:"MAKER_COMMIT", userId:"maker.user"]).create()
            
        and: "Checker with Tier 1 limit (100k)"
        ec.entity.makeValue("trade.UserAuthorityProfile")
            .setAll([userAuthorityId:"T1-03", userId:"tier1.user", delegationTierId:"TIER_1", customLimit:100000.0, makerCheckerFlag: "CHECKER"]).create()

        when: "Tier 1 user tries to authorize a 150k effective amount"
        def result = ec.service.sync().name("trade.AuthorizationServices.evaluate#MakerCheckerMatrix")
            .parameters([instrumentId:"AUTH-AMD", userId:"tier1.user"]).call()
            
        then: "isAuthorized should be false because 150k > 100k"
        result.isAuthorized == false
        
        cleanup:
        ec.entity.find("trade.UserAuthorityProfile").condition("userAuthorityId", "T1-03").deleteAll()
        ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", "AUTH-AMD").deleteAll()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", "AUTH-AMD").deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", "AUTH-AMD").deleteAll()
    }

    def "BDD-CMN-AUTH-05: Priority Queue ordering sorts Urgent before Low"() {
        given: "Two instruments with different priorities"
        ec.entity.makeValue("trade.TradeInstrument").setAll([instrumentId:"PRIO-LOW", transactionRef:"TF-PRIO-01", priorityEnumId:"PRIO_LOW", instrumentTypeEnumId:'IMPORT_LC']).create()
        ec.entity.makeValue("trade.importlc.ImportLetterOfCredit").setAll([instrumentId:"PRIO-LOW"]).create()
        
        ec.entity.makeValue("trade.TradeInstrument").setAll([instrumentId:"PRIO-HIGH", transactionRef:"TF-PRIO-02", priorityEnumId:"PRIO_URGENT", instrumentTypeEnumId:'IMPORT_LC']).create()
        ec.entity.makeValue("trade.importlc.ImportLetterOfCredit").setAll([instrumentId:"PRIO-HIGH"]).create()
        
        when: "Retrieving the list"
        def result = ec.service.sync().name("trade.importlc.ImportLcServices.get#ImportLetterOfCreditList").call()
        def list = result.lcList
        list.each { println "DEBUG_LIST: ID=${it.instrumentId}, Prio=${it.priorityEnumId}, Seq=${it.prioritySequence}" }
        
        then: "Urgent (PRIO-HIGH) should be before Low (PRIO-LOW)"
        int highIdx = list.findIndexOf { it.instrumentId == "PRIO-HIGH" }
        int lowIdx = list.findIndexOf { it.instrumentId == "PRIO-LOW" }
        highIdx != -1
        lowIdx != -1
        highIdx < lowIdx
        
        cleanup:
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", "PRIO-LOW").deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", "PRIO-LOW").deleteAll()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", "PRIO-HIGH").deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", "PRIO-HIGH").deleteAll()
    }
}
