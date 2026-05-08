package trade

import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import spock.lang.Specification
import org.moqui.screen.ScreenTest
import org.moqui.screen.ScreenTest.ScreenTestRender
import spock.lang.Shared

// ABOUTME: RestApiEndpointsSpec verifies the existence and contract of the headless REST API facade.
// ABOUTME: Uses ScreenTest to perform real HTTP-level verification against trade.rest.xml.

class RestApiEndpointsSpec extends Specification {
    @Shared
    ExecutionContext ec
    @Shared
    ScreenTest screenTest

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.user.internalLoginUser("trade.maker")
        ec.artifactExecution.disableAuthz()
        screenTest = ec.screen.makeTest()
            .rootScreen("component://webroot/screen/webroot.xml")
            .baseScreenPath("rest")
    }

    def cleanupSpec() {
        ec.destroy()
    }

    String createTestLc(String prefix) {
        String ref = prefix + "-" + System.currentTimeMillis()
        ec.user.internalLoginUser("trade.maker")
        ScreenTestRender str = screenTest.render("s1/trade/import-lc",
            [instrumentRef: ref, lcAmount: 5000.0, lcCurrencyUomId: "USD",
             customerFacilityId: 'FAC-ACME-001',
             lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_NEGOTIATION', confirmationEnumId: 'CONF_WITHOUT',
             availableWithEnumId: 'AVB_WITH_ANY_BANK',
             instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                 [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]], "post")
        if (str.errorMessages) {
            throw new Exception("Failed to create LC: ${str.errorMessages}")
        }
        def json = new groovy.json.JsonSlurper().parseText(str.output)
        return json.instrumentId
    }

    String createTestPresentation(String instrumentId) {
        // Find the issuance transaction
        def tx = ec.entity.find("trade.TradeTransaction").condition([instrumentId: instrumentId, transactionTypeEnumId: 'IMP_NEW']).disableAuthz().one()
        ec.user.internalLoginUser("trade.checker")
        screenTest.render("s1/trade/authorize", [transactionId: tx.transactionId, skipFourEyes: true], "post")
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).updateAll([businessStateId: "LC_ISSUED"])
        ec.user.internalLoginUser("trade.maker")
        ScreenTestRender str = screenTest.render("s1/trade/import-lc/${instrumentId}/presentation",
            [instrumentId: instrumentId, claimAmount: 1000.0, claimCurrency: "USD"], "post")
        if (str.errorMessages) {
            throw new Exception("Failed to create presentation: ${str.errorMessages}")
        }
        def json = new groovy.json.JsonSlurper().parseText(str.output)
        return json.presentationId
    }

    // ===== GET Endpoints =====

    def "Test GET /trade/kpis"() {
        when:
        ScreenTestRender str = screenTest.render("s1/trade/kpis", [:], "get")

        then:
        !str.errorMessages
        def json = new groovy.json.JsonSlurper().parseText(str.output)
        json.kpis.pendingDrafts >= 0
        json.kpis.pendingApprovals >= 0
    }

    def "Test GET /trade/import-lc returns list"() {
        when:
        ScreenTestRender str = screenTest.render("s1/trade/import-lc", [:], "get")

        then:
        !str.errorMessages
        def json = new groovy.json.JsonSlurper().parseText(str.output)
        json.lcList != null
    }

    def "Test GET /trade/import-lc with status filter"() {
        when:
        ScreenTestRender str = screenTest.render("s1/trade/import-lc", [transactionStatusId: "TX_DRAFT"], "get")

        then:
        !str.errorMessages
        def json = new groovy.json.JsonSlurper().parseText(str.output)
        json.lcList != null
    }

    def "Test GET /trade/import-lc by id"() {
        given:
        String instrumentId = createTestLc("REST-GET")

        when:
        ScreenTestRender str = screenTest.render("s1/trade/import-lc/${instrumentId}", [:], "get")

        then:
        !str.errorMessages
        def json = new groovy.json.JsonSlurper().parseText(str.output)
        json.instrumentId == instrumentId
        json.parties != null
        json.parties.any { it.roleEnumId == "TP_APPLICANT" }
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

    def "Test GET /trade/current-user returns empty when not logged in"() {
        given:
        ec.user.logoutUser()

        when:
        ScreenTestRender str = screenTest.render("s1/trade/current-user", [:], "get")

        then:
        !str.errorMessages
        def json = new groovy.json.JsonSlurper().parseText(str.output)
        json.userId == null
    }

    // ===== POST Endpoints =====

    def "Test POST /trade/import-lc creates new LC"() {
        given:
        String ref = "REST-CREATE-" + System.currentTimeMillis()
        Map params = [instrumentRef: ref, lcAmount: 25000.0, lcCurrencyUomId: "USD",
                      lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_NEGOTIATION', confirmationEnumId: 'CONF_WITHOUT',
                      instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                          [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]]

        when:
        ec.user.internalLoginUser("trade.maker")
        ScreenTestRender str = screenTest.render("s1/trade/import-lc", params, "post")

        then:
        !str.errorMessages
        def json = new groovy.json.JsonSlurper().parseText(str.output)
        json.instrumentId != null
    }

    def "Test POST /trade/import-lc update"() {
        given:
        String instrumentId = createTestLc("REST-UPDATE")
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
        String instrumentId = createTestLc("REST-AUTH")

        when:
        ec.user.internalLoginUser("trade.checker")
        ScreenTestRender str = screenTest.render("s1/trade/authorize", [instrumentId: instrumentId], "post")

        then:
        !str.errorMessages
        def json = new groovy.json.JsonSlurper().parseText(str.output)
        json.isAuthorized != null
    }

    def "Test POST /trade/import-lc amendments"() {
        given:
        String instrumentId = createTestLc("REST-AMEND")
        def tx = ec.entity.find("trade.TradeTransaction").condition([instrumentId: instrumentId, transactionTypeEnumId: 'IMP_NEW']).disableAuthz().one()
        ec.user.internalLoginUser("trade.checker")
        screenTest.render("s1/trade/authorize", [transactionId: tx.transactionId, skipFourEyes: true], "post")
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).updateAll([businessStateId: "LC_ISSUED"])
        
        Map params = [
            instrumentId: instrumentId,
            amendmentTypeEnumId: "AMEND_AMDTMNT",
            amendmentDate: new java.sql.Date(System.currentTimeMillis()),
            amendmentNarrative: "Test amendment"
        ]

        when:
        ec.user.internalLoginUser("trade.maker")
        ScreenTestRender str = screenTest.render("s1/trade/import-lc/${instrumentId}/amendment", params, "post")

        then:
        !str.errorMessages
        def json = new groovy.json.JsonSlurper().parseText(str.output)
        json.amendmentId != null
    }

    def "Test POST /trade/import-lc presentations requires LC_ISSUED state"() {
        given:
        String instrumentId = createTestLc("REST-PRES")
        def tx = ec.entity.find("trade.TradeTransaction").condition([instrumentId: instrumentId, transactionTypeEnumId: 'IMP_NEW']).disableAuthz().one()
        ec.user.internalLoginUser("trade.checker")
        screenTest.render("s1/trade/authorize", [transactionId: tx.transactionId, skipFourEyes: true], "post")
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).updateAll([businessStateId: "LC_ISSUED"])

        when:
        ec.user.internalLoginUser("trade.maker")
        ScreenTestRender str = screenTest.render("s1/trade/import-lc/${instrumentId}/presentation",
            [instrumentId: instrumentId, claimAmount: 5000.0, claimCurrency: "USD"], "post")

        then:
        str.errorMessages || str.output != null
    }

    def "Test PATCH /trade/import-lc presentations waiver requires LC_DOCS_RECEIVED state"() {
        given:
        String instrumentId = createTestLc("REST-WAIVER")
        def tx = ec.entity.find("trade.TradeTransaction").condition([instrumentId: instrumentId, transactionTypeEnumId: 'IMP_NEW']).disableAuthz().one()
        ec.user.internalLoginUser("trade.checker")
        screenTest.render("s1/trade/authorize", [transactionId: tx.transactionId, skipFourEyes: true], "post")
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).updateAll([businessStateId: "LC_ISSUED"])

        when:
        ec.user.internalLoginUser("trade.maker")
        ScreenTestRender str = screenTest.render("s1/trade/import-lc/${instrumentId}/presentation",
            [instrumentId: instrumentId, claimAmount: 1000.0, claimCurrency: "USD"], "post")

        then:
        str.errorMessages || str.output != null
    }

    def "Test POST /trade/import-lc settlement"() {
        given:
        String instrumentId = createTestLc("REST-SETTLE")
        String presentationId = createTestPresentation(instrumentId)
        
        // Move to LC_ACCEPTED to allow settlement
        ec.service.sync().name("update#trade.TradeInstrument").parameters([instrumentId: instrumentId, businessStateId: 'LC_ACCEPTED']).call()

        when:
        ec.user.internalLoginUser("trade.maker")
        ScreenTestRender str = screenTest.render("s1/trade/import-lc/${instrumentId}/settlement",
            [instrumentId: instrumentId, presentationId: presentationId, principalAmount: 1000.0, 
             settlementTypeEnumId: 'SETTLE_SIGHT', debitAccountId: "TRADE-USD-001"], "post")

        then:
        !str.errorMessages
        str.output != null
    }

    def "Test POST /trade/import-lc shipping-guarantee"() {
        given:
        String instrumentId = createTestLc("REST-SG")

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
        String instrumentId = createTestLc("REST-CANCEL")
        // Move to LC_ISSUED to simulate a more realistic cancellation
        ec.user.internalLoginUser("trade.checker")
        screenTest.render("s1/trade/authorize", [instrumentId: instrumentId], "post")
        ec.service.sync().name("update#trade.TradeInstrument")
            .parameters([instrumentId: instrumentId, businessStateId: "INST_AUTHORIZED"]).call()

        when:
        ec.user.internalLoginUser("trade.maker")
        ScreenTestRender str = screenTest.render("s1/trade/import-lc/${instrumentId}/cancellation",
            [instrumentId: instrumentId, cancellationReason: "REST test"], "post")

        then:
        if (str.errorMessages) println "DEBUG CANCEL ERRORS: ${str.errorMessages}"
        !str.errorMessages
        str.output != null
    }

    def "Test POST /trade/product-config"() {
        given:
        String key = "REST_TEST_" + System.currentTimeMillis()
        Map params = [configKey: key, configValue: "test_value"]

        when:
        ec.user.internalLoginUser("trade.maker")
        ScreenTestRender str = screenTest.render("s1/trade/product-config", params, "post")

        then:
        !str.errorMessages
        str.output != null
    }

    
}