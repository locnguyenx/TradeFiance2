/* ABOUTME: TradeSwiftAutoTriggerSpec verifies the just-in-time SWIFT generation via SECAs. */
package trade

import org.moqui.context.ExecutionContext
import org.moqui.Moqui
import spock.lang.Specification

class TradeSwiftAutoTriggerSpec extends Specification {
    protected ExecutionContext ec

    def setup() {
        ec = Moqui.getExecutionContext()
        ec.user.internalLoginUser("trade.maker")
    }

    def cleanup() {
        ec.destroy()
    }

    def "should auto-generate MT700 DRAFT on detail view"() {
        setup:
        def instId = "AUTO-GEN-LC-01"
        // Ensure no existing record
        ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", instId).deleteAll()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instId).deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", instId).deleteAll()

        // Create a basic LC draft manually (to avoid triggering other SECAs if they already exist, but here we want to test the 'get' trigger)
        ec.service.sync().name("create#trade.TradeInstrument").parameters([
            instrumentId: instId, transactionRef: "AUTO-01", instrumentTypeEnumId: 'IMPORT_LC', businessStateId: 'LC_DRAFT', amount: 1000.0, currencyUomId: 'USD'
        ]).call()
        ec.service.sync().name("create#trade.importlc.ImportLetterOfCredit").parameters([
            instrumentId: instId, businessStateId: 'LC_DRAFT', lcAmount: 1000.0, lcCurrencyUomId: 'USD'
        ]).call()

        when: "Calling the detail get service"
        ec.service.sync().name("trade.importlc.ImportLcServices.get#ImportLetterOfCredit").parameters([instrumentId: instId]).call()

        then: "MT700 draft should have been generated"
        def msgs = ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", instId).list()
        msgs.size() >= 1
        msgs.any { it.messageType == 'MT700' && it.messageStatusId == 'SWIFT_MSG_DRAFT' }

        cleanup:
        ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", instId).deleteAll()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instId).deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", instId).deleteAll()
    }
}
