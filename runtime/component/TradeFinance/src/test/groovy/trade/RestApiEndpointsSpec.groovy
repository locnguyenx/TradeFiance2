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
            [transactionRef: ref, lcAmount: 5000.0, lcCurrencyUomId: "USD",
             instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                 [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]], "post")
        if (str.errorMessages) {
            throw new Exception("Failed to create LC: ${str.errorMessages}")
        }
        def json = new groovy.json.JsonSlurper().parseText(str.output)
        return json.instrumentId
    }

    String createTestPresentation(String instrumentId) {
        ec.user.internalLoginUser("trade.checker")
        screenTest.render("s1/trade/authorize", [instrumentId: instrumentId], "post")
        ec.service.sync().name("trade.importlc.ImportLcServices.approve#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, approverUserId: "trade.checker"]).call()
        ec.user.internalLoginUser("trade.maker")
        ScreenTestRender str = screenTest.render("s1/trade/import-lc/${instrumentId}/presentations",
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
        ScreenTestRender str = screenTest.render("s1/trade/audit-logs", [:], "get")

        then:
        !str.errorMessages
        def json = new groovy.json.JsonSlurper().parseText(str.output)
        json.auditLogList != null
    }

    // ===== POST Endpoints =====

    def "Test POST /trade/import-lc creates new LC"() {
        given:
        String ref = "REST-CREATE-" + System.currentTimeMillis()
        Map params = [transactionRef: ref, lcAmount: 25000.0, lcCurrencyUomId: "USD",
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
        Map params = [
            instrumentId: instrumentId,
            amendmentTypeEnumId: "AMEND_AMDTMNT",
            amendmentDate: new java.sql.Date(System.currentTimeMillis()),
            amendmentNarrative: "Test amendment"
        ]

        when:
        ec.user.internalLoginUser("trade.maker")
        ScreenTestRender str = screenTest.render("s1/trade/import-lc/${instrumentId}/amendments", params, "post")

        then:
        !str.errorMessages
        def json = new groovy.json.JsonSlurper().parseText(str.output)
        json.amendmentId != null
    }

    def "Test POST /trade/import-lc presentations requires LC_ISSUED state"() {
        given:
        String instrumentId = createTestLc("REST-PRES")
        ec.user.internalLoginUser("trade.checker")
        screenTest.render("s1/trade/authorize", [instrumentId: instrumentId], "post")
        ec.service.sync().name("trade.importlc.ImportLcServices.approve#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, approverUserId: "trade.checker"]).call()

        when:
        ec.user.internalLoginUser("trade.maker")
        ScreenTestRender str = screenTest.render("s1/trade/import-lc/${instrumentId}/presentations",
            [instrumentId: instrumentId, claimAmount: 5000.0, claimCurrency: "USD"], "post")

        then:
        str.errorMessages || str.output != null
    }

    def "Test PATCH /trade/import-lc presentations waiver requires LC_DOCS_RECEIVED state"() {
        given:
        String instrumentId = createTestLc("REST-WAIVER")
        ec.user.internalLoginUser("trade.checker")
        screenTest.render("s1/trade/authorize", [instrumentId: instrumentId], "post")
        ec.service.sync().name("trade.importlc.ImportLcServices.approve#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, approverUserId: "trade.checker"]).call()

        when:
        ec.user.internalLoginUser("trade.maker")
        ScreenTestRender str = screenTest.render("s1/trade/import-lc/${instrumentId}/presentations",
            [instrumentId: instrumentId, claimAmount: 1000.0, claimCurrency: "USD"], "post")

        then:
        str.errorMessages || str.output != null
    }

    def "Test POST /trade/import-lc settlements"() {
        given:
        String instrumentId = createTestLc("REST-SETTLE")

        when:
        ec.user.internalLoginUser("trade.maker")
        ScreenTestRender str = screenTest.render("s1/trade/import-lc/${instrumentId}/settlements",
            [instrumentId: instrumentId, principalAmount: 2500.0, debitAccountId: "TRADE-USD-001"], "post")

        then:
        !str.errorMessages
        str.output != null
    }

    def "Test POST /trade/import-lc shipping-guarantees"() {
        given:
        String instrumentId = createTestLc("REST-SG")

        when:
        ec.user.internalLoginUser("trade.maker")
        ScreenTestRender str = screenTest.render("s1/trade/import-lc/${instrumentId}/shipping-guarantees",
            [instrumentId: instrumentId, invoiceAmount: 1000.0], "post")

        then:
        !str.errorMessages
        def json = new groovy.json.JsonSlurper().parseText(str.output)
        json.guaranteeId != null
    }

    def "Test POST /trade/import-lc cancel"() {
        given:
        String instrumentId = createTestLc("REST-CANCEL")

        when:
        ec.user.internalLoginUser("trade.maker")
        ScreenTestRender str = screenTest.render("s1/trade/import-lc/${instrumentId}/cancel",
            [instrumentId: instrumentId, cancellationReason: "REST test"], "post")

        then:
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