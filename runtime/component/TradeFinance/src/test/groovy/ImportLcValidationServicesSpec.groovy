
import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import spock.lang.Specification
import spock.lang.Shared

/**
 * ABOUTME: ImportLcValidationServicesSpec validates SWIFT character set compliance and instrument-specific business rules.
 */
class ImportLcValidationServicesSpec extends Specification {
    @Shared
    ExecutionContext ec

    def setupSpec() {
        ec = Moqui.getExecutionContext()
    }

    def cleanupSpec() {
        ec.destroy()
    }

    def "validate#SwiftFields rejects invalid X-Character in goodsDescription"() {
        when:
        ec.service.sync().name("trade.importlc.ImportLcValidationServices.validate#SwiftFields")
            .parameters([
                goodsDescription: "Steel Rods @ 50mm diameter",
                transactionRef: "TF-IMP-26-0001",
                portOfLoading: "Ho Chi Minh City"
            ]).call()

        then:
        ec.message.hasError()
        ec.message.errorsString.contains("goodsDescription")
        ec.message.errorsString.contains("@")
        
        cleanup:
        ec.message.clearAll()
    }

    def "validate#SwiftFields accepts valid characters"() {
        when:
        ec.service.sync().name("trade.importlc.ImportLcValidationServices.validate#SwiftFields")
            .parameters([
                goodsDescription: "Steel Rods - 50mm diameter (standard)",
                transactionRef: "TF-IMP-26-0002",
                portOfLoading: "HO CHI MINH CITY"
            ]).call()

        then:
        !ec.message.hasError()
        
        cleanup:
        ec.message.clearAll()
    }

    def "transition#BusinessState rejects invalid transition"() {
        given:
        ec.artifactExecution.disableAuthz()

        when:
        ec.service.sync().name("trade.importlc.ImportLcValidationServices.transition#BusinessState")
            .parameters([
                instrumentId: "123", 
                fromStateId: "LC_DRAFT",
                toStateId: "LC_CLOSED"
            ]).call()

        then:
        ec.message.hasError()
        ec.message.errorsString.contains("Transition from LC_DRAFT to LC_CLOSED is not allowed")

        cleanup:
        ec.artifactExecution.enableAuthz()
        ec.message.clearAll()
    }
}
