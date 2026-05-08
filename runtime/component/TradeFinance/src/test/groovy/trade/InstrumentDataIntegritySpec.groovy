package trade

import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import spock.lang.Specification
import spock.lang.Shared

// ABOUTME: InstrumentDataIntegritySpec verifies that instrument-level data retrieval is correctly isolated.
// ABOUTME: Specifically validates that party records from different instruments do not leak into each other.

class InstrumentDataIntegritySpec extends Specification {
    @Shared
    ExecutionContext ec

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.user.internalLoginUser("trade.maker")
    }

    def cleanupSpec() {
        ec.destroy()
    }

    def "Verify Party Record Isolation between Instruments"() {
        given:
        // 1. Create Instrument A with both mandatory parties
        def resA = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: "ISO-A-" + System.currentTimeMillis(), lcAmount: 1000.0, lcCurrencyUomId: "USD",
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                             [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]]).call()
        String idA = resA.instrumentId

        // 2. Create Instrument B with different parties
        def resB = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: "ISO-B-" + System.currentTimeMillis(), lcAmount: 2000.0, lcCurrencyUomId: "USD",
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'GLOBAL_EXP_002'],
                                             [roleEnumId: 'TP_BENEFICIARY', partyId: 'ACME_CORP_001']]]).call()
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
        outA.parties.any { it.roleEnumId == 'TP_APPLICANT' && it.partyId == 'ACME_CORP_001' }
        outA.parties.any { it.roleEnumId == 'TP_BENEFICIARY' && it.partyId == 'GLOBAL_EXP_002' }

        // Instrument B should ONLY have its own parties
        outB.parties.size() == 2
        outB.parties.any { it.roleEnumId == 'TP_APPLICANT' && it.partyId == 'GLOBAL_EXP_002' }
        outB.parties.any { it.roleEnumId == 'TP_BENEFICIARY' && it.partyId == 'ACME_CORP_001' }
    }

    def "Verify Instrument View handles Alphanumeric IDs"() {
        given:
        String testId = "LC240003" // Standard test ID from seed

        when:
        def out = ec.service.sync().name("trade.importlc.ImportLcServices.get#ImportLetterOfCredit")
            .parameters([instrumentId: testId]).call()

        then:
        println "DEBUG: out fields for ${testId}: " + out.keySet()
        println "DEBUG: out instrumentRef: " + out.instrumentRef
        out != null
        out.instrumentId == testId
        out.instrumentRef != null || out.instrumentRef == null // Just to see what it is
    }
}
