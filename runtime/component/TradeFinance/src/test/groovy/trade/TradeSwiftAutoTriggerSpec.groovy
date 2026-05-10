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
        testPrefix = "SWT-AUTO-" + System.currentTimeMillis()
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
        ec.artifactExecution.disableAuthz()
        boolean begun = ec.transaction.begin(60)
        try {
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").updateAll([latestTransactionId: null])
            ec.entity.find("trade.importlc.ImportLcInternalAmendment").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.ImportLcAmendment").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.ImportLcSettlement").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.ImportLcShippingGuarantee").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
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
