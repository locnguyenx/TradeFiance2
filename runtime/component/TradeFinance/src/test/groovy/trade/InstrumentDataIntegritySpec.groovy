package trade

import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import spock.lang.Specification
import spock.lang.Shared
import org.moqui.entity.EntityCondition

// ABOUTME: InstrumentDataIntegritySpec verifies that instrument-level data retrieval is correctly isolated.
// ABOUTME: Specifically validates that party records from different instruments do not leak into each other.

class InstrumentDataIntegritySpec extends Specification {
    @Shared protected ExecutionContext ec
    @Shared String testPrefix

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.artifactExecution.disableAuthz()
        ec.user.loginUser("trade.admin", "trade123")
        testPrefix = "INST-INT-" + System.currentTimeMillis()
        cleanData()
    }

    def cleanupSpec() {
        try {
            if (ec != null) cleanData()
        } finally {
            if (ec != null) ec.destroy()
        }
    }

    private void cleanData() {
        boolean begun = ec.transaction.begin(60)
        try {
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").updateAll([latestTransactionId: null])
            ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
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

    def "Verify Party Record Isolation between Instruments"() {
        given:
        def idA = testPrefix + "_ID_A"
        def idB = testPrefix + "_ID_B"

        // 1. Create Instrument A with both mandatory parties
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentId: idA, instrumentRef: idA + "_REF", lcAmount: 1000.0, lcCurrencyUomId: "USD",
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                             [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]]).call()
        assert !ec.message.hasError()

        // 2. Create Instrument B with different parties
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentId: idB, instrumentRef: idB + "_REF", lcAmount: 2000.0, lcCurrencyUomId: "USD",
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'GLOBAL_EXP_002'],
                                             [roleEnumId: 'TP_BENEFICIARY', partyId: 'ACME_CORP_001']]]).call()
        assert !ec.message.hasError()

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
        String testId = testPrefix + "_ALPHA_123"
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentId: testId, instrumentRef: testPrefix + "_ALPHA_REF", lcAmount: 5000.0, lcCurrencyUomId: "USD",
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                             [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]]).call()
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
