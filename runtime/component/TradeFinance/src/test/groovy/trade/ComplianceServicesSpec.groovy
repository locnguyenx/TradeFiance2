package trade

import org.moqui.context.ExecutionContext
import org.moqui.Moqui
import spock.lang.Specification
import spock.lang.Shared

/*
ABOUTME: ComplianceServicesSpec verifies the manual hold and release workflows for trade instruments.
It ensures that compliance holds prevent all business state transitions until explicitly released.
*/
class ComplianceServicesSpec extends Specification {
    ExecutionContext ec
    String testPrefix

    def setup() {
        ec = Moqui.getExecutionContext()
        ec.user.internalLoginUser("trade.maker")
        ec.artifactExecution.disableAuthz()
        testPrefix = "COMP-SPEC-" + System.currentTimeMillis()
        
        // Load mandatory seed data for priority enums and hold actions
        ec.entity.makeDataLoader().location("component://TradeFinance/data/TradeFinanceSeedData.xml").load()
        ec.entity.makeDataLoader().location("component://TradeFinance/entity/TradeCommonEntities.xml").load()
        ec.entity.makeDataLoader().location("component://TradeFinance/entity/ImportLcEntities.xml").load()
        
        // Clean up before each test
        cleanData()
    }

    def cleanup() {
        if (ec != null) {
            cleanData()
            ec.destroy()
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

    def "BDD-CMN-AUTH-04: Compliance Hold blocks LC lifecycle"() {
        given:
        String lcId = testPrefix + "-LC-01"
        // Create instrument via service
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
                .parameters([instrumentId: lcId, lcAmount: 10000, lcCurrencyUomId: 'USD',
                             instrumentParties: [
                                 [roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                 [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']
                             ]])
                .call()
        // Move to ISSUED state for testing hold on active LC
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
                .parameters([instrumentId: lcId, businessStateId: 'LC_ISSUED']).call()

        // Approve issuance to clear concurrent transaction block
        def txIss = ec.entity.find("trade.TradeTransaction").condition([instrumentId: lcId, transactionTypeEnumId: 'IMP_NEW']).disableAuthz().one()
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument")
            .parameters([transactionId: txIss.transactionId, skipFourEyes: true]).call()
        
        def txHoldId = testPrefix + "-TX-HOLD-01"
        ec.service.sync().name("create#trade.TradeTransaction")
                .parameters([transactionId: txHoldId, instrumentId: lcId, 
                             transactionTypeEnumId: 'IMP_AMENDMENT',
                             transactionStatusId: 'TX_DRAFT', versionNumber: 1]).call()

        when: "Apply Compliance Hold"
        ec.service.sync().name("trade.importlc.ImportLcServices.hold#ImportLetterOfCredit")
            .parameters([instrumentId: lcId]).call()
        
        then: "Status is LC_HOLD and previous status is remembered"
        def inst = ec.entity.find("trade.TradeInstrument").condition("instrumentId", lcId).one()
        inst.businessStateId == "LC_HOLD"
        inst.previousBusinessStateId == "LC_ISSUED"

        when: "Try to transition (e.g., Receive Documents)"
        ec.service.sync().name("trade.importlc.ImportLcValidationServices.validate#BusinessStateTransition")
            .parameters([instrumentId: lcId, toStateId: 'LC_DOC_RECEIVED']).call()

        then: "Transition fails with error message"
        ec.message.hasError()
        ec.message.errors.any { it.contains("Compliance Hold") }
        ec.message.clearAll()

        when: "Release Compliance Hold"
        ec.service.sync().name("trade.importlc.ImportLcServices.release#ImportLetterOfCredit")
            .parameters([instrumentId: lcId]).call()

        then: "Status is restored to LC_ISSUED"
        def instReleased = ec.entity.find("trade.TradeInstrument").condition("instrumentId", lcId).one()
        instReleased.businessStateId == "LC_ISSUED"
        instReleased.previousBusinessStateId == null

        when: "Try to transition now (Receive Documents)"
        ec.service.sync().name("trade.importlc.ImportLcValidationServices.validate#BusinessStateTransition")
            .parameters([instrumentId: lcId, toStateId: 'LC_DOC_RECEIVED']).call()

        then: "Transition succeeds"
        !ec.message.hasError()
    }
}
