package trade

import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import spock.lang.Specification
import org.moqui.screen.ScreenTest
import org.moqui.screen.ScreenTest.ScreenTestRender
import spock.lang.Shared
import org.moqui.entity.EntityCondition

// ABOUTME: RestApiEndpointsSpec verifies the existence and contract of the headless REST API facade.
// ABOUTME: Uses ScreenTest to perform real HTTP-level verification against trade.rest.xml.

class RestApiEndpointsSpec extends Specification {
    @Shared protected ExecutionContext ec
    @Shared ScreenTest screenTest
    @Shared String testPrefix

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.artifactExecution.disableAuthz()
        ec.user.loginUser("trade.admin", "trade123")
        testPrefix = "REST-API-" + System.currentTimeMillis()
        
        screenTest = ec.screen.makeTest()
            .rootScreen("component://webroot/screen/webroot.xml")
            .baseScreenPath("rest")
            
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
            ec.entity.find("trade.importlc.ImportLcInternalAmendment").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.ImportLcAmendment").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.ImportLcShippingGuarantee").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.ImportLcSettlement").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
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

    String createTestLc(String suffix) {
        String instrumentId = testPrefix + "_" + suffix
        String ref = instrumentId + "_REF"
        ec.user.internalLoginUser("trade.maker")
        ScreenTestRender str = screenTest.render("s1/trade/import-lc",
            [instrumentId: instrumentId, instrumentRef: ref, lcAmount: 5000.0, lcCurrencyUomId: "USD",
             customerFacilityId: 'FAC-ACME-001',
             lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_NEGOTIATION', confirmationEnumId: 'CONF_WITHOUT',
             availableWithEnumId: 'AW_ANY_BANK',
             instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                 [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]], "post")
        if (str.errorMessages) {
            throw new Exception("Failed to create LC: ${str.errorMessages}")
        }
        def json = new groovy.json.JsonSlurper().parseText(str.output)
        return json.instrumentId
    }

    void approveIssuance(String instrumentId) {
        def tx = ec.entity.find("trade.TradeTransaction").condition([instrumentId: instrumentId, transactionTypeEnumId: 'IMP_NEW']).disableAuthz().one()
        if (tx && tx.transactionStatusId != 'TX_APPROVED') {
            ec.user.internalLoginUser("trade.checker")
            screenTest.render("s1/trade/authorize", [transactionId: tx.transactionId, skipFourEyes: true], "post")
            ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).updateAll([businessStateId: "LC_ISSUED"])
            ec.user.internalLoginUser("trade.maker")
        }
    }

    String createTestPresentation(String instrumentId) {
        approveIssuance(instrumentId)
        ec.user.internalLoginUser("trade.maker")
        ScreenTestRender str = screenTest.render("s1/trade/import-lc/${instrumentId}/presentation",
            [instrumentId: instrumentId, claimAmount: 1000.0, claimCurrency: "USD"], "post")
        if (str.errorMessages) {
            throw new Exception("Failed to create presentation: ${str.errorMessages}")
        }
        def json = new groovy.json.JsonSlurper().parseText(str.output)
        
        // Also approve the presentation transaction to allow settlement
        def txPres = ec.entity.find("trade.TradeTransaction")
            .condition([instrumentId: instrumentId, transactionTypeEnumId: 'IMP_PRESENTATION', transactionStatusId: 'TX_DRAFT'])
            .disableAuthz().one()
            
        if (txPres) {
            ec.user.internalLoginUser("trade.checker")
            screenTest.render("s1/trade/authorize", [transactionId: txPres.transactionId, skipFourEyes: true], "post")
        }
        ec.user.internalLoginUser("trade.maker")
        
        return json.presentationId
    }

    // ===== GET Endpoints =====

    def "Test GET /trade/kpis"() {
        when:
        ScreenTestRender str = screenTest.render("s1/trade/kpis", [:], "get")

        then:
        !str.errorMessages
        def json = new groovy.json.JsonSlurper().parseText(str.output)
        json.kpis != null
    }

    def "Test GET /trade/import-lc returns list"() {
        when:
        ScreenTestRender str = screenTest.render("s1/trade/import-lc", [:], "get")

        then:
        !str.errorMessages
        def json = new groovy.json.JsonSlurper().parseText(str.output)
        json.lcList != null
    }

    def "Test GET /trade/import-lc by id"() {
        given:
        String instrumentId = createTestLc("GET_ID")

        when:
        ScreenTestRender str = screenTest.render("s1/trade/import-lc/${instrumentId}", [:], "get")

        then:
        !str.errorMessages
        def json = new groovy.json.JsonSlurper().parseText(str.output)
        json.instrumentId == instrumentId
    }

    def "Test GET /trade/standard-clauses"() {
        when:
        ScreenTestRender str = screenTest.render("s1/trade/standard-clauses", [:], "get")

        then:
        !str.errorMessages
        def json = new groovy.json.JsonSlurper().parseText(str.output)
        json.clauseList != null
    }

    def "Test GET /trade/audit-logs"() {
        when:
        ScreenTestRender str = screenTest.render("s1/trade/common/audit-logs", [:], "get")

        then:
        !str.errorMessages
        def json = new groovy.json.JsonSlurper().parseText(str.output)
        json.auditLogList != null
    }

    // ===== POST Endpoints =====

    def "Test POST /trade/import-lc creates new LC"() {
        given:
        String instrumentId = testPrefix + "_POST_CREATE"
        String ref = instrumentId + "_REF"
        Map params = [instrumentId: instrumentId, instrumentRef: ref, lcAmount: 25000.0, lcCurrencyUomId: "USD",
                      lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_NEGOTIATION', confirmationEnumId: 'CONF_WITHOUT',
                      instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                          [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]]

        when:
        ec.user.internalLoginUser("trade.maker")
        ScreenTestRender str = screenTest.render("s1/trade/import-lc", params, "post")

        then:
        !str.errorMessages
        def json = new groovy.json.JsonSlurper().parseText(str.output)
        json.instrumentId == instrumentId
    }

    def "Test POST /trade/import-lc update"() {
        given:
        String instrumentId = createTestLc("POST_UPDATE")
        Map params = [instrumentId: instrumentId, lcAmount: 200.0]

        when:
        ec.user.internalLoginUser("trade.maker")
        ScreenTestRender str = screenTest.render("s1/trade/import-lc/${instrumentId}", params, "post")

        then:
        !str.errorMessages
        str.output != null
    }

    def "Test POST /trade/authorize"() {
        given:
        String instrumentId = createTestLc("POST_AUTH")

        when:
        ec.user.internalLoginUser("trade.checker")
        ScreenTestRender str = screenTest.render("s1/trade/authorize", [instrumentId: instrumentId], "post")

        then:
        !str.errorMessages
        def json = new groovy.json.JsonSlurper().parseText(str.output)
        json.isAuthorized != null
    }

    def "Test POST /trade/import-lc amendments (External)"() {
        given:
        String instrumentId = createTestLc("POST_AMEND")
        approveIssuance(instrumentId)
        
        Map params = [
            instrumentId: instrumentId,
            amendmentTypeEnumId: "AMEND_OTHER",
            amendmentDate: new java.sql.Date(System.currentTimeMillis()),
            goodsActionEnumId: "AMA_REPLACE",
            goodsDeltaText: "Revised goods description via REST"
        ]

        when:
        ec.user.internalLoginUser("trade.maker")
        ScreenTestRender str = screenTest.render("s1/trade/import-lc/${instrumentId}/amendment/external", params, "post")

        then:
        !str.errorMessages
        def json = new groovy.json.JsonSlurper().parseText(str.output)
        json.amendmentId != null
    }

    def "Test POST /trade/import-lc amendments (Internal)"() {
        given:
        String instrumentId = createTestLc("POST_INT_AMEND")
        approveIssuance(instrumentId)
        
        Map params = [
            instrumentId: instrumentId,
            amendmentTypeEnumId: "AMD_TYPE_INTERNAL",
            newFacilityId: "FAC-INTERNAL-001"
        ]

        when:
        ec.user.internalLoginUser("trade.maker")
        ScreenTestRender str = screenTest.render("s1/trade/import-lc/${instrumentId}/amendment/internal", params, "post")

        then:
        !str.errorMessages
        def json = new groovy.json.JsonSlurper().parseText(str.output)
        json.internalAmendmentId != null
    }

    def "Test POST /trade/import-lc settlement"() {
        given:
        String instrumentId = createTestLc("POST_SETTLE")
        String presentationId = createTestPresentation(instrumentId)
        
        // Move to LC_ACCEPTED to allow settlement
        ec.service.sync().name("update#trade.TradeInstrument").parameters([instrumentId: instrumentId, businessStateId: 'LC_ACCEPTED']).call()
        
        when:
        ec.user.internalLoginUser("trade.maker")
        ScreenTestRender str = screenTest.render("s1/trade/import-lc/${instrumentId}/settlement",
            [instrumentId: instrumentId, presentationId: presentationId, principalAmount: 1000.0, 
             settlementTypeEnumId: 'SIGHT_PAYMENT', debitAccountId: "TRADE-USD-001"], "post")

        then:
        !str.errorMessages
        str.output != null
    }

    def "Test POST /trade/import-lc shipping-guarantee"() {
        given:
        String instrumentId = createTestLc("POST_SG")
        approveIssuance(instrumentId)

        when:
        ec.user.internalLoginUser("trade.maker")
        ScreenTestRender str = screenTest.render("s1/trade/import-lc/${instrumentId}/shipping-guarantee",
            [instrumentId: instrumentId, invoiceAmount: 1000.0], "post")

        then:
        !str.errorMessages
        def json = new groovy.json.JsonSlurper().parseText(str.output)
        json.guaranteeId != null
    }

    def "Test POST /trade/import-lc cancellation"() {
        given:
        String instrumentId = createTestLc("POST_CANCEL")
        approveIssuance(instrumentId)
        
        // Move to LC_ISSUED to simulate a more realistic cancellation
        ec.user.internalLoginUser("trade.checker")
        screenTest.render("s1/trade/authorize", [instrumentId: instrumentId], "post")
        ec.service.sync().name("update#trade.TradeInstrument")
            .parameters([instrumentId: instrumentId, businessStateId: "LC_ISSUED"]).call()

        when:
        ec.user.internalLoginUser("trade.maker")
        ScreenTestRender str = screenTest.render("s1/trade/import-lc/${instrumentId}/cancellation",
            [instrumentId: instrumentId, cancellationReason: "REST test"], "post")

        then:
        !str.errorMessages
        str.output != null
    }
}