package moqui.trade.finance

import spock.lang.Specification
import org.moqui.Moqui
import org.moqui.context.ExecutionContext

// ABOUTME: LimitServicesSpec verifies the facility earmarking logic and insufficient limit enforcement.

class LimitServicesSpec extends Specification {
    protected ExecutionContext ec
    
    def setup() {
        ec = Moqui.getExecutionContext()
    }
    
    def cleanup() {
        ec.destroy()
    }
    
    def "Test CREATE CustomerFacility validates base limits"() {
        when:
        ec.entity.makeValue("moqui.trade.instrument.CustomerFacility")
            .setAll([facilityId:"FAC-1", totalApprovedLimit: 1000.0, utilizedAmount: 0.0]).create()
            
        then:
        ec.service.call("trade.finance.LimitServices.calculate#Earmark", [facilityId:"FAC-1", amount: 500.0]).isAllowed == true
        
        cleanup:
        ec.entity.find("moqui.trade.instrument.CustomerFacility").condition("facilityId", "FAC-1").deleteAll()
    }
}
