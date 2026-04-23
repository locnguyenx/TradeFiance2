package moqui.trade.finance

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
    }
    
    def cleanup() {
        ec.destroy()
    }
    
    def "Test Maker/Checker matrix prohibits self-approval"() {
        setup:
        ec.entity.makeValue("moqui.trade.instrument.TradeInstrument")
            .setAll([instrumentId:"AUTH-1", transactionRef:"TF-AUTH-01"]).create()
        ec.entity.makeValue("moqui.trade.instrument.TradeTransactionAudit")
            .setAll([instrumentId:"AUTH-1", auditId:"1", actionEnumId:"MAKER_COMMIT", userId:"john.doe"]).create()
            
        when: "Checker is the same user"
        def resultSame = ec.service.sync().name("AuthorizationServices.evaluate#MakerCheckerMatrix")
            .parameters([instrumentId:"AUTH-1", userId:"john.doe"]).call()
            
        then:
        resultSame.isAuthorized == false
        
        when: "Checker is a different user"
        def resultDiff = ec.service.sync().name("AuthorizationServices.evaluate#MakerCheckerMatrix")
            .parameters([instrumentId:"AUTH-1", userId:"jane.doe"]).call()
            
        then:
        resultDiff.isAuthorized == true
        
        cleanup:
        ec.entity.find("moqui.trade.instrument.TradeTransactionAudit").condition("instrumentId", "AUTH-1").deleteAll()
        ec.entity.find("moqui.trade.instrument.TradeInstrument").condition("instrumentId", "AUTH-1").deleteAll()
    }
}
