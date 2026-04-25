import org.moqui.context.ExecutionContext
import org.moqui.Moqui
import spock.lang.Specification

class DraftLcSpec extends Specification {
    ExecutionContext ec
    
    def setup() {
        ec = Moqui.getExecutionContext()
        ec.user.loginUser("john.doe", "moqui")
    }
    
    def cleanup() {
        ec.destroy()
    }
    
    def "Create Draft LC and Verify Visibility on Dashboard"() {
        when: "Creating a draft LC without transactionRef (simulating UI behavior)"
        def result = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([
                lcAmount: 50000,
                lcCurrencyUomId: 'USD',
                applicantPartyId: 'ACME_CORP',
                beneficiaryPartyId: 'GLOBAL_TRADE',
                customerFacilityId: 'FAC_001',
                productCatalogId: 'IMP_LC_USANCE'
            ]).call()
            
        then: "It should fail because transactionRef is required"
        thrown(org.moqui.service.ServiceException)
        
        when: "Creating a draft LC with random transactionRef"
        def ref = "DRAFT-" + System.currentTimeMillis()
        def createResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([
                transactionRef: ref,
                lcAmount: 50000,
                lcCurrencyUomId: 'USD',
                applicantPartyId: 'ACME_CORP',
                beneficiaryPartyId: 'GLOBAL_TRADE',
                customerFacilityId: 'FAC_001',
                productCatalogId: 'IMP_LC_USANCE'
            ]).call()
        def instrumentId = createResult.instrumentId
        
        then: "LC is created in TX_DRAFT status"
        instrumentId != null
        def inst = ec.entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId).one()
        inst.transactionStatusId == 'TX_DRAFT'
        
        when: "Fetching LC list for dashboard"
        def listResult = ec.service.sync().name("trade.importlc.ImportLcServices.get#ImportLetterOfCreditList").call()
        def found = listResult.lcList.find { it.instrumentId == instrumentId }
        
        then: "Draft LC should be in the list"
        found != null
        found.transactionRef == ref
    }
}
