package moqui.trade.finance

import spock.lang.Specification
import org.moqui.Moqui
import org.moqui.context.ExecutionContext

// ABOUTME: ImportLcValidationServicesSpec verifies the claim amount tolerance enforcement during drawing.

class ImportLcValidationServicesSpec extends Specification {
    protected ExecutionContext ec
    
    def setup() {
        ec = Moqui.getExecutionContext()
        ec.user.internalLoginUser("trade.maker")
        ec.artifactExecution.disableAuthz()
    }
    
    def cleanup() {
        ec.destroy()
    }
    
    def "Test evaluate#Drawing enforces tolerance"() {
        setup:
        ec.entity.makeValue("moqui.trade.instrument.TradeInstrument")
            .setAll([instrumentId:"LC-1", amount: 1000.0, baseEquivalentAmount: 1000.0]).create()
        ec.entity.makeValue("moqui.trade.importlc.ImportLetterOfCredit")
            .setAll([instrumentId:"LC-1", tolerancePositive: 0.10]).create()
            
        when: "Drawing within tolerance"
        def result = ec.service.sync().name("ImportLcValidationServices.evaluate#Drawing").parameters([instrumentId:"LC-1", claimAmount: 1100.0]).call()
        
        then:
        !ec.message.hasError()
        
        when: "Drawing above tolerance"
        ec.service.sync().name("ImportLcValidationServices.evaluate#Drawing").parameters([instrumentId:"LC-1", claimAmount: 1200.0]).call()
        
        then:
        ec.message.hasError()
        
        cleanup:
        ec.entity.find("moqui.trade.importlc.ImportLetterOfCredit").condition("instrumentId", "LC-1").deleteAll()
        ec.entity.find("moqui.trade.instrument.TradeInstrument").condition("instrumentId", "LC-1").deleteAll()
    }
}
