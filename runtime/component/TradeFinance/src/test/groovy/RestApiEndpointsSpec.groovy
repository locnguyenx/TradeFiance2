
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

    def "Test GET /trade/kpis"() {
        given:
        def diagFile = new File("/tmp/diag.txt")
        
        when:
        ScreenTestRender str = screenTest.render("s1/trade/kpis", [:], "get")
        diagFile << "s1/trade/kpis output: ${str.output}\n"
        diagFile << "s1/trade/kpis errors: ${str.errorMessages}\n"
        
        then:
        !str.errorMessages
        def json = new groovy.json.JsonSlurper().parseText(str.output)
        json.kpis.pendingDrafts >= 0
    }

    def "Test POST /trade/import-lc"() {
        given:
        def diagFile = new File("/tmp/diag.txt")
        Map params = [transactionRef: "REST-TEST-001", amount: 25000.0]
        
        when:
        ec.user.internalLoginUser("trade.maker")
        ScreenTestRender str = screenTest.render("s1/trade/import-lc", params, "post")
        diagFile << "s1/trade/import-lc output: ${str.output}\n"
        diagFile << "s1/trade/import-lc errors: ${str.errorMessages}\n"
        
        then:
        !str.errorMessages
        def json = new groovy.json.JsonSlurper().parseText(str.output)
        json.instrumentId != null
    }

    def "Test POST /trade/authorize"() {
        given:
        def diagFile = new File("/tmp/diag.txt")
        // Create an instrument first to authorize
        def createOut = ec.service.sync().name("ImportLcServices.create#ImportLetterOfCredit")
                          .parameters([amount: 100.0]).call()
        String instrumentId = createOut.instrumentId
        diagFile << "Created instrument for auth test: ${instrumentId}\n"
        
        when:
        ec.user.internalLoginUser("trade.checker")
        ScreenTestRender str = screenTest.render("s1/trade/authorize", [instrumentId: instrumentId], "post")
        diagFile << "s1/trade/authorize output: ${str.output}\n"
        diagFile << "s1/trade/authorize errors: ${str.errorMessages}\n"
        
        then:
        !str.errorMessages
        def json = new groovy.json.JsonSlurper().parseText(str.output)
        json.isAuthorized == true
    }
}
