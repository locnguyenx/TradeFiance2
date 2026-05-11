package trade


import spock.lang.Specification
import spock.lang.Shared
import org.moqui.Moqui
import org.moqui.context.ExecutionContext

// ABOUTME: ShippingGuaranteeSpec verifies the mandatory 110% credit earmarking for Shipping Guarantees.
// ABOUTME: Compares invoice amount against facility availability with the required multiplier.

class ShippingGuaranteeSpec extends Specification {
    @Shared protected ExecutionContext ec
    @Shared String testPrefix
    
    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.transaction.runUseOrBegin(null, null) {
            println "DEBUG: setupSpec ShippingGuaranteeSpec starting"
            ec.user.loginUser("trade.maker", "trade123")
            ec.artifactExecution.disableAuthz()
            testPrefix = "SG-SPEC-" + System.currentTimeMillis()

        // Set isolated ID generation ranges - use 3000000
        ec.entity.tempSetSequencedIdPrimary("trade.CustomerFacility", 43000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrument", 43000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit", 43000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLcShippingGuarantee", 43000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeTransaction", 43000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrumentParty", 43000000, 1000)
        }
        println "DEBUG: setupSpec ShippingGuaranteeSpec complete"
    }
    
    def cleanupSpec() {
        if (ec != null) {
            ec.entity.tempResetSequencedIdPrimary("trade.CustomerFacility")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrument")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLcShippingGuarantee")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeTransaction")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrumentParty")
            ec.destroy()
        }
    }

    def setup() {
        ec.message.clearAll()
        ec.artifactExecution.disableAuthz()
    }
    
    def "Test Shipping Guarantee 110% Earmarking"() {
        given: "Initialize Facility and instrument"
        def facRes = ec.entity.makeValue("trade.CustomerFacility")
            .setAll([totalApprovedLimit: 120.0, utilizedAmount: 0.0, currencyUomId: 'USD', statusId: 'FAC_ACTIVE'])
            .setSequencedIdPrimary().create()
        def facId = facRes.facilityId
        def instRes = ec.entity.makeValue("trade.TradeInstrument")
            .setAll([customerFacilityId: facId, amount: 1000.0, currencyUomId: 'USD', instrumentTypeEnumId: 'IMPORT_LC'])
            .setSequencedIdPrimary().create()
        def lcId = instRes.instrumentId
        ec.entity.makeValue("trade.importlc.ImportLetterOfCredit")
            .setAll([instrumentId: lcId, effectiveAmount: 1000.0, effectiveCurrencyUomId: 'USD']).create()
            
        when: "1. Create SG within limit (Invoice 100 * 110% = 110, limit 120)"
        def resultOk = ec.service.sync().name("trade.importlc.ImportLcServices.create#ShippingGuarantee").parameters([
            instrumentId: lcId,
            invoiceAmount: 100.0,
            liabilityMultiplierRequired: 110,
            transportDocReference: testPrefix + "-BOL-001"
        ]).call()
        
        then: "Success and utilization updated"
        !ec.message.hasError()
        resultOk.guaranteeId != null
        ec.entity.find("trade.CustomerFacility").condition("facilityId", facId).one().utilizedAmount == 110.0
        
        when: "2. Create SG exceeding limit (remaining 10, need 22)"
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ShippingGuarantee").parameters([
            instrumentId: lcId,
            invoiceAmount: 20.0,
            liabilityMultiplierRequired: 110
        ]).call()
        
        then: "Failure"
        ec.message.hasError()
    }
}
