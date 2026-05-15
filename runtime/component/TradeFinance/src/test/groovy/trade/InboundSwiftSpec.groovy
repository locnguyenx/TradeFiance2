package trade

import spock.lang.Specification
import org.moqui.context.ExecutionContext
import org.moqui.Moqui
import spock.lang.Shared

/**
 * ABOUTME: InboundSwiftSpec tests the technical ingestion, deduplication, and correlation of SWIFT messages.
 */
class InboundSwiftSpec extends Specification {
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
