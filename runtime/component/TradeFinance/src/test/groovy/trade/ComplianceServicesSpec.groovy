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

    def setup() {
        ec = Moqui.getExecutionContext()
        ec.user.internalLoginUser("trade.maker")
        ec.artifactExecution.disableAuthz()
        // Load mandatory seed data for priority enums and hold actions
        ec.entity.makeDataLoader().location("component://TradeFinance/data/TradeFinanceSeedData.xml").load()
        
        // Clean up before each test
        String lcId = "COMP-HOLD-LC-01"
        ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", lcId).deleteAll()
        ec.entity.find("trade.TradeTransaction").condition("instrumentId", lcId).deleteAll()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", lcId).deleteAll()
        ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", lcId).deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", lcId).deleteAll()
    }

    def cleanup() {
        ec.destroy()
    }

    def "BDD-CMN-AUTH-04: Compliance Hold blocks LC lifecycle"() {
        given:
        String lcId = "COMP-HOLD-LC-01"
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

        ec.service.sync().name("create#trade.TradeTransaction")
                .parameters([transactionId: "TX-COMP-HOLD-01", instrumentId: lcId, 
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

        cleanup:
        ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", lcId).deleteAll()
        ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", lcId).deleteAll()
        ec.entity.find("trade.TradeTransaction").condition("instrumentId", lcId).deleteAll()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", lcId).deleteAll()
        ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", lcId).deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", lcId).deleteAll()
    }
}
