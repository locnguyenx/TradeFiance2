
import spock.lang.Specification
import org.moqui.Moqui
import org.moqui.context.ExecutionContext

// ABOUTME: EndToEndImportLcSpec verifies the complete lifecycle of an Import LC from creation to SWIFT generation.
// ABOUTME: Covers Instrument creation, Limit updates, and MT700 block presence.

class EndToEndImportLcSpec extends Specification {
    protected ExecutionContext ec
    
    def setup() {
        ec = Moqui.getExecutionContext()
        ec.user.internalLoginUser("trade.maker")
        ec.artifactExecution.disableAuthz()
    }
    
    def cleanup() {
        ec.destroy()
    }
    
    def "Full Flow: Create LC -> Update Limit -> Generate SWIFT"() {
        setup: "Initialize Facility"
        ec.entity.makeValue("trade.CustomerFacility")
            .setAll([facilityId:"E2E-FAC-1", totalApprovedLimit: 100000.0, utilizedAmount: 0.0]).create()
            
        when: "1. Create Import LC"
        def createResult = ec.service.sync().name("ImportLcServices.create#ImportLetterOfCredit").parameters([
            transactionRef: "TF-E2E-001",
            amount: 50000.0,
            baseEquivalentAmount: 50000.0,
            customerFacilityId: "E2E-FAC-1",
            businessStateId: "LC_DRAFT"
        ]).call()
        def instrumentId = createResult.instrumentId
        
        then: "Instrument exists"
        instrumentId != null
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one() != null
        
        when: "2. Update Limit Utilization"
        ec.service.sync().name("LimitServices.update#Utilization").parameters([facilityId:"E2E-FAC-1", amountDelta: 50000.0]).call()
        
        then: "Facility utilization is updated"
        ec.entity.find("trade.CustomerFacility").condition("facilityId", "E2E-FAC-1").one().utilizedAmount == 50000.0
        
        when: "3. Generate SWIFT MT700"
        def swiftResult = ec.service.sync().name("SwiftGenerationServices.generate#Mt700").parameters([instrumentId: instrumentId]).call()
        
        then: "SWIFT message is created with correct content"
        swiftResult.swiftMessageId != null
        def message = ec.entity.find("trade.importlc.SwiftMessage").condition("swiftMessageId", swiftResult.swiftMessageId).one()
        message.messageContent.contains("TF-E2E-001")
        message.messageType == "MT700"
        
        cleanup:
        ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", instrumentId).deleteAll()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId).deleteAll()
        ec.entity.find("trade.CustomerFacility").condition("facilityId", "E2E-FAC-1").deleteAll()
    }
}
