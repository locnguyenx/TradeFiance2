package trade

import spock.lang.Specification
import spock.lang.Shared
import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityCondition

// ABOUTME: TradeSwiftAutoTriggerSpec verifies the just-in-time SWIFT generation via SECAs.
// ABOUTME: Ensures that MT700 drafts are generated when accessing instrument details.

class TradeSwiftAutoTriggerSpec extends Specification {
    @Shared protected ExecutionContext ec
    @Shared String testPrefix

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.user.loginUser("trade.admin", "trade123")
        ec.artifactExecution.disableAuthz()
        testPrefix = "STA-" + System.currentTimeMillis()

        // Set isolated ID generation ranges - use 49000000
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrument", 49000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit", 49000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.SwiftMessage", 49000000, 1000)
    }

    def cleanupSpec() {
        if (ec != null) {
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrument")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.SwiftMessage")
            ec.destroy()
        }
    }

    def setup() {
        ec.message.clearAll()
        ec.artifactExecution.disableAuthz()
    }

    def cleanup() {
        ec.message.clearAll()
    }

    def "should auto-generate MT700 DRAFT on detail view"() {
        given:
        def instrumentId = testPrefix + "-LC-01"
        
        // Create mandatory parties first to allow ImportLetterOfCredit creation if needed (though we create manually here)
        ec.entity.makeValue("trade.TradeInstrument").setAll([
            instrumentId: instrumentId, instrumentRef: instrumentId + "-REF", 
            instrumentTypeEnumId: 'IMPORT_LC', businessStateId: 'LC_DRAFT', amount: 1000.0, currencyUomId: 'USD'
        ]).create()
        ec.entity.makeValue("trade.importlc.ImportLetterOfCredit").setAll([
            instrumentId: instrumentId, businessStateId: 'LC_DRAFT', effectiveAmount: 1000.0, effectiveCurrencyUomId: 'USD'
        ]).create()

        when: "Calling the detail get service"
        ec.service.sync().name("trade.importlc.ImportLcServices.get#ImportLetterOfCredit").parameters([instrumentId: instrumentId]).call()

        then: "The service should succeed"
        !ec.message.hasError()

        when: "Verifying generated messages (with retry for async SECA)"
        ec.artifactExecution.disableAuthz()
        def msgs = []
        long start = System.currentTimeMillis()
        while (msgs.isEmpty() && (System.currentTimeMillis() - start) < 5000) {
            msgs = ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", instrumentId).list()
            if (msgs.isEmpty()) Thread.sleep(200)
        }
        
        then: "MT700 draft should have been generated"
        msgs.size() >= 1
        msgs.any { it.messageType == 'MT700' && it.messageStatusId == 'SWIFT_MSG_DRAFT' }
    }
}
