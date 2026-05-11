package trade

import org.moqui.context.ExecutionContext
import org.moqui.Moqui
import spock.lang.Specification
import spock.lang.Shared
import org.moqui.entity.EntityCondition

/**
 * ABOUTME: ComplianceServicesSpec verifies the manual hold and release workflows for trade instruments.
 * It ensures that compliance holds prevent all business state transitions until explicitly released.
 */
class ComplianceServicesSpec extends Specification {
    @Shared protected ExecutionContext ec
    @Shared String testPrefix

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.user.loginUser("trade.maker", "trade123")
        ec.artifactExecution.disableAuthz()
        testPrefix = "COMP-SPEC-" + System.currentTimeMillis()

        // Set isolated ID generation ranges - use 4100000
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrument", 35000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit", 35000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeTransaction", 35000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrumentParty", 35000000, 1000)
    }

    def cleanupSpec() {
        if (ec != null) {
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrument")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeTransaction")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrumentParty")
            ec.destroy()
        }
    }

    def setup() {
        if (ec.transaction.isTransactionInPlace()) ec.transaction.rollback("Cleanup from previous state", null)
        ec.user.loginUser("trade.maker", "trade123")
        ec.message.clearAll()
        ec.artifactExecution.disableAuthz()
    }

    def "BDD-CMN-AUTH-04: Compliance Hold blocks LC lifecycle"() {
        given:
        // Create instrument via service
        def createRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
                .parameters([lcAmount: 10000, lcCurrencyUomId: 'USD',
                             instrumentParties: [
                                 [roleEnumId: 'TP_APPLICANT', partyId: testPrefix + '_APP'],
                                 [roleEnumId: 'TP_BENEFICIARY', partyId: testPrefix + '_BEN']
                             ]])
                .call()
        String lcId = createRes.instrumentId

        // Move to ISSUED state for testing hold on active LC
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
                .parameters([instrumentId: lcId, businessStateId: 'LC_ISSUED']).call()

        // Approve issuance to clear concurrent transaction block
        def txIss = ec.entity.find("trade.TradeTransaction").condition([instrumentId: lcId, transactionTypeEnumId: 'IMP_NEW']).disableAuthz().one()
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument")
            .parameters([transactionId: txIss.transactionId, skipFourEyes: true]).call()
        
        // Create a draft transaction
        ec.service.sync().name("create#trade.TradeTransaction")
                .parameters([instrumentId: lcId, 
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
