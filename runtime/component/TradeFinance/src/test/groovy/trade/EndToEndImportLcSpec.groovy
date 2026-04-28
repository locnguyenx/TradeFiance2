package trade

import spock.lang.Specification
import org.moqui.Moqui
import org.moqui.context.ExecutionContext

// ABOUTME: EndToEndImportLcSpec verifies the complete lifecycle of an Import LC from creation to SWIFT generation.
// ABOUTME: Covers Instrument creation, Limit updates, and MT700 block presence.

class EndToEndImportLcSpec extends Specification {
    protected ExecutionContext ec
    
    // Test constants to avoid hardcoding
    static final String TEST_MAKER = "trade.maker"
    static final String TEST_FACILITY_ID = "E2E-FAC-1"
    static final String TEST_TRANS_REF = "TF-E2E-001"
    
    def setup() {
        ec = Moqui.getExecutionContext()
        ec.user.internalLoginUser(TEST_MAKER)
        ec.artifactExecution.disableAuthz()
        
        if (ec.entity.find("moqui.security.UserAccount").condition("username", TEST_MAKER).count() == 0) {
            ec.entity.makeValue("moqui.security.UserAccount")
                .setAll([userId: TEST_MAKER, username: TEST_MAKER, currentPassword: "trade123", firstName: "Trade", lastName: "Maker"])
                .create()
        }
        if (ec.entity.find("trade.UserAuthorityProfile").condition("userId", TEST_MAKER).count() == 0) {
            ec.entity.makeValue("trade.UserAuthorityProfile")
                .setAll([userAuthorityId: "T1-E2E", userId: TEST_MAKER, delegationTierId: "TIER_1", customLimit: 1000000.00, currencyUomId: "USD", makerCheckerFlag: "MAKER_CHECKER"])
                .create()
        }
    }
    
    def cleanup() {
        ec.destroy()
    }
    
    def "Full Flow: Create LC -> Update Limit -> Generate SWIFT"() {
        setup: "Initialize Facility"
        ec.entity.makeValue("trade.CustomerFacility")
            .setAll([facilityId: TEST_FACILITY_ID, totalApprovedLimit: 100000.0, utilizedAmount: 0.0]).create()
            
        when: "1. Create Import LC"
        def createResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            transactionRef: TEST_TRANS_REF,
            lcAmount: 50000.0,
            lcCurrencyUomId: "USD",
            customerFacilityId: TEST_FACILITY_ID,
            businessStateId: "LC_DRAFT"
        ]).call()
        def instrumentId = createResult.instrumentId
        
        then: "Instrument exists"
        instrumentId != null
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one() != null
        
        when: "2. Update Limit Utilization"
        ec.service.sync().name("trade.LimitServices.update#Utilization").parameters([facilityId: TEST_FACILITY_ID, amountDelta: 50000.0]).call()
        
        then: "Facility utilization is updated"
        ec.entity.find("trade.CustomerFacility").condition("facilityId", TEST_FACILITY_ID).one().utilizedAmount == 50000.0
        
        when: "3. Generate SWIFT MT700"
        def swiftResult = ec.service.sync().name("trade.SwiftGenerationServices.generate#Mt700").parameters([instrumentId: instrumentId]).call()
        
        then: "SWIFT message is created with correct content"
        swiftResult.swiftMessageId != null
        def message = ec.entity.find("trade.importlc.SwiftMessage").condition("swiftMessageId", swiftResult.swiftMessageId).one()
        message.messageContent.contains(TEST_TRANS_REF)
        message.messageType == "MT700"
        
        cleanup:
        if (instrumentId) {
            ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.TradeTransaction").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId).deleteAll()
        }
        ec.entity.find("trade.CustomerFacility").condition("facilityId", TEST_FACILITY_ID).deleteAll()
    }
}
