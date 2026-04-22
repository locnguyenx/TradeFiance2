package moqui.trade.finance

import spock.lang.Specification
import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import java.sql.Timestamp

// ABOUTME: RestApiEndpointsSpec verifies the existence and contract of the headless REST API facade.
// ABOUTME: Covers Import LC, Checker Auth, and KPI dashboards.

class RestApiEndpointsSpec extends Specification {
    protected ExecutionContext ec
    
    def setup() {
        ec = Moqui.getExecutionContext()
    }
    
    def cleanup() {
        ec.destroy()
    }
    
    def "Test Import LC endpoint presence"() {
        expect:
        ec.screen.makeRender().rootScreen("component://TradeFinance/screen/TradeFinanceRoot/rest.xml")
            .transition("import-lc").render() != null
    }

    def "Test Checker authorization and KPI endpoints validate presence"() {
        expect:
        ec.screen.makeRender().rootScreen("component://TradeFinance/screen/TradeFinanceRoot/rest.xml")
            .transition("authorize").render() != null
        ec.screen.makeRender().rootScreen("component://TradeFinance/screen/TradeFinanceRoot/rest.xml")
            .transition("kpis").render() != null
    }
}
