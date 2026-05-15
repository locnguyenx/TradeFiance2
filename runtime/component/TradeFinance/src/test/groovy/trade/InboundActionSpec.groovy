package trade

import spock.lang.Specification
import org.moqui.context.ExecutionContext
import org.moqui.Moqui
import spock.lang.Shared

/**
 * ABOUTME: InboundActionSpec tests the business actions triggered by specific MT message types (730, 799, 750, etc.).
 */
class InboundActionSpec extends Specification {
    @Shared protected ExecutionContext ec

    def setupSpec() {
        ec = Moqui.getExecutionContext()
    }

    def cleanupSpec() {
        ec.destroy()
    }

    def "Placeholder test"() {
        expect: true
    }
}
