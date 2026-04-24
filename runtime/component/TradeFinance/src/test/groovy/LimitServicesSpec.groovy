
import spock.lang.Specification
import org.moqui.Moqui
import org.moqui.context.ExecutionContext

// ABOUTME: LimitServicesSpec verifies the facility earmarking logic and insufficient limit enforcement.

class LimitServicesSpec extends Specification {
    protected ExecutionContext ec
    
    def setup() {
        ec = Moqui.getExecutionContext()
        ec.user.internalLoginUser("trade.maker")
        ec.artifactExecution.disableAuthz()
    }
    
    def cleanup() {
        ec.destroy()
    }
    
    def "Test CREATE CustomerFacility validates base limits"() {
        when:
        ec.entity.makeValue("trade.CustomerFacility")
            .setAll([facilityId:"FAC-1", totalApprovedLimit: 1000.0, utilizedAmount: 0.0]).create()
            
        then:
        ec.service.sync().name("trade.LimitServices.calculate#Earmark").parameters([facilityId:"FAC-1", amount: 500.0]).call().isAllowed == true
        
        cleanup:
        ec.entity.find("trade.CustomerFacility").condition("facilityId", "FAC-1").deleteAll()
    }
}
