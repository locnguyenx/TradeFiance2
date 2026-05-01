package trade


import spock.lang.Specification
import org.moqui.Moqui
import org.moqui.context.ExecutionContext

// ABOUTME: ShippingGuaranteeSpec verifies the mandatory 110% credit earmarking for Shipping Guarantees.
// ABOUTME: Compares invoice amount against facility availability with the required multiplier.

class ShippingGuaranteeSpec extends Specification {
    protected ExecutionContext ec
    
    def setup() {
        ec = Moqui.getExecutionContext()
        ec.user.internalLoginUser("trade.maker")
        ec.artifactExecution.disableAuthz()
    }
    
    def cleanup() {
        ec.destroy()
    }
    
    def "Test Shipping Guarantee 110% Earmarking"() {
        setup: "Initialize Facility and instrument"
        ec.entity.makeValue("trade.CustomerFacility")
            .setAll([facilityId:"SG-FAC-1", totalApprovedLimit: 120.0, utilizedAmount: 0.0]).create()
        ec.entity.makeValue("trade.TradeInstrument")
            .setAll([instrumentId:"SG-LC-1", customerFacilityId: "SG-FAC-1", amount: 1000.0, currencyUomId: 'USD']).create()
        ec.entity.makeValue("trade.importlc.ImportLetterOfCredit")
            .setAll([instrumentId:"SG-LC-1", effectiveAmount: 1000.0, effectiveCurrencyUomId: 'USD']).create()
            
        when: "1. Create SG within limit (Invoice 100 * 110% = 110, limit 120)"
        def resultOk = ec.service.sync().name("trade.importlc.ImportLcServices.create#ShippingGuarantee").parameters([
            instrumentId: "SG-LC-1",
            invoiceAmount: 100.0,
            liabilityMultiplierRequired: 110,
            transportDocReference: "BOL-001"
        ]).call()
        
        then: "Success and utilization updated"
        !ec.message.hasError()
        resultOk.guaranteeId != null
        ec.entity.find("trade.CustomerFacility").condition("facilityId", "SG-FAC-1").one().utilizedAmount == 110.0
        
        when: "2. Create SG exceeding limit (remaining 10, need 22)"
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ShippingGuarantee").parameters([
            instrumentId: "SG-LC-1",
            invoiceAmount: 20.0,
            liabilityMultiplierRequired: 110
        ]).call()
        
        then: "Failure"
        ec.message.hasError()
        
        cleanup:
        ec.entity.find("trade.importlc.ImportLcShippingGuarantee").condition("instrumentId", "SG-LC-1").deleteAll()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", "SG-LC-1").deleteAll()
        ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", "SG-LC-1").deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", "SG-LC-1").deleteAll()
        ec.entity.find("trade.CustomerFacility").condition("facilityId", "SG-FAC-1").deleteAll()
    }
}
