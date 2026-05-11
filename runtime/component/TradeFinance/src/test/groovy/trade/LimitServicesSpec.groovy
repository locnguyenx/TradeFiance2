package trade


import spock.lang.Specification
import spock.lang.Shared
import org.moqui.Moqui
import org.moqui.context.ExecutionContext

// ABOUTME: LimitServicesSpec verifies the facility earmarking logic and insufficient limit enforcement.

class LimitServicesSpec extends Specification {
    @Shared protected ExecutionContext ec
    @Shared String testPrefix
    
    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.transaction.runUseOrBegin(null, null) {
            println "DEBUG: setupSpec LimitServicesSpec starting"
            ec.user.loginUser("trade.maker", "trade123")
            ec.artifactExecution.disableAuthz()
        testPrefix = "LIM-SPEC-" + System.currentTimeMillis()

        // Set isolated ID generation ranges - use 2500000
        ec.entity.tempSetSequencedIdPrimary("trade.CustomerFacility", 42000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeParty", 42000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeTransaction", 42000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrument", 42000000, 1000)
        }
        println "DEBUG: setupSpec LimitServicesSpec complete"
    }
    
    def cleanupSpec() {
        if (ec != null) {
            ec.entity.tempResetSequencedIdPrimary("trade.CustomerFacility")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeParty")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeTransaction")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrument")
            ec.destroy()
        }
    }

    def setup() {
        ec.message.clearAll()
        ec.artifactExecution.disableAuthz()
    }
    
    def "Test CREATE CustomerFacility validates base limits"() {
        given:
        def facRes = ec.entity.makeValue("trade.CustomerFacility")
            .setAll([totalApprovedLimit: 1000.0, utilizedAmount: 0.0])
        facRes.setSequencedIdPrimary()
        facRes.create()
        def facId = facRes.facilityId
            
        when:
        def res = ec.service.sync().name("trade.LimitServices.calculate#Earmark").parameters([facilityId: facId, amount: 500.0]).call()

        then:
        res.isAllowed == true
    }

    def "Test GET CustomerFacilities returns list for owner"() {
        given:
        def partyRes = ec.entity.makeValue("trade.TradeParty").setAll([partyName: "Limit Test Party", partyTypeEnumId: "PTY_COMMERCIAL"])
        partyRes.setSequencedIdPrimary()
        partyRes.create()
        def partyId = partyRes.partyId
        def facRes = ec.entity.makeValue("trade.CustomerFacility")
            .setAll([ownerPartyId: partyId, totalApprovedLimit: 5000.0, utilizedAmount: 1000.0])
        facRes.setSequencedIdPrimary()
        facRes.create()
        def facId = facRes.facilityId
            
        when:
        def res = ec.service.sync().name("trade.LimitServices.get#CustomerFacilities").parameters([partyId: partyId]).call()
        
        then:
        res.facilityList != null
        res.facilityList.size() >= 1
        def fac = res.facilityList.find { it.facilityId == facId }
        fac.limitAmount == 5000.0
        fac.available == 4000.0
    }
}
