package trade

import spock.lang.Specification
import spock.lang.Shared
import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityCondition

// ABOUTME: DraftLcSpec validates the creation and visibility of draft Import LCs on the dashboard.
// ABOUTME: Ensures that auto-generated transaction references and status tracking work as expected.

class DraftLcSpec extends Specification {
    @Shared protected ExecutionContext ec
    @Shared String testPrefix

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.artifactExecution.disableAuthz()
        ec.user.loginUser("trade.admin", "trade123")
        testPrefix = "DRAFT-LC-" + System.currentTimeMillis()
        
        // Setup unique Party and Facility for this spec
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
            .parameters([partyId: testPrefix + '_APP', partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'App Draft', kycStatus: 'KYC_ACTIVE']).call()
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
            .parameters([partyId: testPrefix + '_BEN', partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'Ben Draft', kycStatus: 'KYC_ACTIVE']).call()
            
        ec.entity.makeValue("trade.CustomerFacility")
            .setAll([
                facilityId: testPrefix + "_FAC", ownerPartyId: testPrefix + '_APP', 
                totalApprovedLimit: 1000000.0, utilizedAmount: 0.0, currencyUomId: "USD", statusId: "FAC_ACTIVE"
            ]).create()

        cleanData()
    }

    def cleanupSpec() {
        try {
            if (ec != null) {
                cleanData()
                ec.entity.find("trade.CustomerFacility").condition("facilityId", testPrefix + "_FAC").deleteAll()
                ec.entity.find("trade.TradeParty").condition("partyId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            }
        } finally {
            if (ec != null) ec.destroy()
        }
    }

    private void cleanData() {
        boolean begun = ec.transaction.begin(60)
        try {
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").updateAll([latestTransactionId: null])
            ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeApprovalRecord").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeTransaction").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.transaction.commit(begun)
        } catch (Exception e) {
            ec.transaction.rollback(begun, "Error in cleanData", e)
        }
    }

    def setup() {
        ec.message.clearAll()
        ec.artifactExecution.disableAuthz()
    }
    
    def cleanup() {
        ec.message.clearAll()
    }
    
    def "Create Draft LC and Verify Visibility on Dashboard"() {
        given:
        def instrumentId1 = testPrefix + "_ID_01"
        def instrumentId2 = testPrefix + "_ID_02"
        def ref2 = testPrefix + "_REF_02"

        when: "Creating a draft LC without instrumentRef"
        def result = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([
                instrumentId: instrumentId1,
                lcAmount: 50000,
                lcCurrencyUomId: 'USD',
                instrumentParties: [
                    [roleEnumId: 'TP_APPLICANT', partyId: testPrefix + '_APP'],
                    [roleEnumId: 'TP_BENEFICIARY', partyId: testPrefix + '_BEN']
                ],
                customerFacilityId: testPrefix + '_FAC',
                productCatalogId: 'PROD_IMP_LC'
            ]).call()
            
        then: "It should succeed and auto-generate transactionRef on the draft transaction"
        !ec.message.hasError()
        result.instrumentId == instrumentId1
        def draftTx = ec.entity.find("trade.TradeTransaction").condition("instrumentId", instrumentId1).one()
        draftTx.transactionRef != null
        
        when: "Creating a draft LC with explicit instrumentRef"
        def createResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([
                instrumentId: instrumentId2,
                instrumentRef: ref2,
                lcAmount: 50000,
                lcCurrencyUomId: 'USD',
                instrumentParties: [
                    [roleEnumId: 'TP_APPLICANT', partyId: testPrefix + '_APP'],
                    [roleEnumId: 'TP_BENEFICIARY', partyId: testPrefix + '_BEN']
                ],
                customerFacilityId: testPrefix + '_FAC',
                productCatalogId: 'PROD_IMP_LC'
            ]).call()
        
        then: "LC is created in TX_DRAFT status"
        !ec.message.hasError()
        createResult.instrumentId == instrumentId2
        def tx = ec.entity.find("trade.TradeTransaction").condition("instrumentId", instrumentId2).one()
        tx.transactionStatusId == 'TX_DRAFT'
        
        when: "Fetching LC list for dashboard"
        def listResult = ec.service.sync().name("trade.importlc.ImportLcServices.get#ImportLetterOfCreditList").call()
        def found = listResult.lcList.find { it.instrumentId == instrumentId2 }
        
        then: "Draft LC should be in the list"
        found != null
        found.instrumentRef == ref2
    }
}
