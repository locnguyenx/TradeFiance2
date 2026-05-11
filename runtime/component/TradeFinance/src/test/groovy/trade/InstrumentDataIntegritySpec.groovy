package trade

import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import spock.lang.Specification
import spock.lang.Shared
import org.moqui.entity.EntityCondition

/**
 * ABOUTME: InstrumentDataIntegritySpec verifies that instrument-level data retrieval is correctly isolated.
 * Specifically validates that party records from different instruments do not leak into each other.
 */
class InstrumentDataIntegritySpec extends Specification {
    @Shared protected ExecutionContext ec
    @Shared String testPrefix

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.artifactExecution.disableAuthz()
        ec.user.loginUser("trade.admin", "trade123")
        testPrefix = "IDI-" + System.currentTimeMillis()

        // Set isolated ID generation ranges - use 12500000
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrument", 12500000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit", 12500000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeTransaction", 12500000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrumentParty", 12500000, 1000)
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
        if (ec.transaction.isTransactionInPlace()) ec.transaction.rollback("Cleanup", null)
        ec.message.clearAll()
        ec.artifactExecution.disableAuthz()
    }

    def cleanup() {
        ec.message.clearAll()
    }

    def "Verify Party Record Isolation between Instruments"() {
        given:
        // 1. Create Instrument A with both mandatory parties
        def resA = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: testPrefix + "_REF_A", lcAmount: 1000.0, lcCurrencyUomId: "USD",
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: testPrefix + '_APP_A'],
                                             [roleEnumId: 'TP_BENEFICIARY', partyId: testPrefix + '_BEN_A']]]).call()
        assert !ec.message.hasError()
        String idA = resA.instrumentId

        // 2. Create Instrument B with different parties
        def resB = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: testPrefix + "_REF_B", lcAmount: 2000.0, lcCurrencyUomId: "USD",
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: testPrefix + '_APP_B'],
                                             [roleEnumId: 'TP_BENEFICIARY', partyId: testPrefix + '_BEN_B']]]).call()
        assert !ec.message.hasError()
        String idB = resB.instrumentId

        when:
        // 3. Fetch Instrument A
        def outA = ec.service.sync().name("trade.importlc.ImportLcServices.get#ImportLetterOfCredit")
            .parameters([instrumentId: idA]).call()
        
        // 4. Fetch Instrument B
        def outB = ec.service.sync().name("trade.importlc.ImportLcServices.get#ImportLetterOfCredit")
            .parameters([instrumentId: idB]).call()

        then:
        // Instrument A should ONLY have its own parties
        outA.parties.size() == 2
        outA.parties.any { it.roleEnumId == 'TP_APPLICANT' && it.partyId == testPrefix + '_APP_A' }
        outA.parties.any { it.roleEnumId == 'TP_BENEFICIARY' && it.partyId == testPrefix + '_BEN_A' }

        // Instrument B should ONLY have its own parties
        outB.parties.size() == 2
        outB.parties.any { it.roleEnumId == 'TP_APPLICANT' && it.partyId == testPrefix + '_APP_B' }
        outB.parties.any { it.roleEnumId == 'TP_BENEFICIARY' && it.partyId == testPrefix + '_BEN_B' }
    }

    def "Verify Instrument View handles Alphanumeric IDs"() {
        given:
        String testId = testPrefix + "_ALPHA_123"
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentId: testId, instrumentRef: testPrefix + "_ALPHA_REF", lcAmount: 5000.0, lcCurrencyUomId: "USD",
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: testPrefix + '_APP_ALPHA'],
                                             [roleEnumId: 'TP_BENEFICIARY', partyId: testPrefix + '_BEN_ALPHA']]]).call()
        assert !ec.message.hasError()

        when:
        def out = ec.service.sync().name("trade.importlc.ImportLcServices.get#ImportLetterOfCredit")
            .parameters([instrumentId: testId]).call()

        then:
        out != null
        out.instrumentId == testId
        out.instrumentRef == testPrefix + "_ALPHA_REF"
    }
}
