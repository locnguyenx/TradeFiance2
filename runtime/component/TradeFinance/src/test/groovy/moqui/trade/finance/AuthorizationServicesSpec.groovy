package moqui.trade.finance

import spock.lang.Specification
import org.moqui.Moqui
import org.moqui.context.ExecutionContext

// ABOUTME: AuthorizationServicesSpec verifies the Maker/Checker risk matrix, ensuring Maker cannot also be Checker.

class AuthorizationServicesSpec extends Specification {
    protected ExecutionContext ec
    
    def setup() {
        ec = Moqui.getExecutionContext()
    }
    
    def cleanup() {
        ec.destroy()
    }
    
    def "Test Maker/Checker matrix prohibits self-approval"() {
        setup:
        ec.entity.makeValue("moqui.trade.instrument.TradeInstrument")
            .setAll([instrumentId:"AUTH-1", transactionRef:"TF-AUTH-01"]).create()
        ec.entity.makeValue("moqui.trade.instrument.TradeTransactionAudit")
            .setAll([instrumentId:"AUTH-1", actionEnumId:"MAKER_COMMIT", userId:"john.doe"]).create()
            
        when: "Checker is the same user"
        def resultSame = ec.service.call("trade.finance.AuthorizationServices.evaluate#MakerCheckerMatrix", 
            [instrumentId:"AUTH-1", userId:"john.doe"])
            
        then:
        resultSame.isAuthorized == false
        
        when: "Checker is a different user"
        def resultDiff = ec.service.call("trade.finance.AuthorizationServices.evaluate#MakerCheckerMatrix", 
            [instrumentId:"AUTH-1", userId:"jane.doe"])
            
        then:
        resultDiff.isAuthorized == true
        
        cleanup:
        ec.entity.find("moqui.trade.instrument.TradeTransactionAudit").condition("instrumentId", "AUTH-1").deleteAll()
        ec.entity.find("moqui.trade.instrument.TradeInstrument").condition("instrumentId", "AUTH-1").deleteAll()
    }
}
