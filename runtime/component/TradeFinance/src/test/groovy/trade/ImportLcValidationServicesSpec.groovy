package trade


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
        ec.artifactExecution.disableAuthz()
    }

    def cleanupSpec() {
        ec.destroy()
    }

    def "validate#SwiftFields rejects invalid X-Character in goodsDescription"() {
        given:
        def ref = "TF-SWIFT-BAD-" + System.currentTimeMillis()
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([
                transactionRef: ref, 
                goodsDescription: "Steel Rods @ 50mm diameter", 
                lcAmount: 1000.0, 
                lcCurrencyUomId: 'USD',
                lcTypeEnumId: 'LC_TYPE_IRREVOCABLE',
                availableByEnumId: 'AVAIL_BY_SIGHT',
                confirmationEnumId: 'CONFIRM_WITHOUT'
            ]).call()
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("goodsDescription", "Steel Rods @ 50mm diameter").one()

        when:
        ec.service.sync().name("trade.importlc.ImportLcValidationServices.validate#SwiftFields")
            .parameters([entityType: 'ImportLetterOfCredit', entityId: lc.instrumentId]).call()

        then:
        ec.message.hasError()
        ec.message.errorsString.contains("goodsDescription")
        ec.message.errorsString.contains("@")
        
        cleanup:
        ec.message.clearAll()
    }

    def "validate#SwiftFields accepts valid characters"() {
        given:
        def ref = "TF-SWIFT-GOOD-" + System.currentTimeMillis()
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([
                transactionRef: ref, 
                goodsDescription: "Steel Rods - 50mm diameter (standard)", 
                lcAmount: 1000.0, 
                lcCurrencyUomId: 'USD',
                lcTypeEnumId: 'LC_TYPE_IRREVOCABLE',
                availableByEnumId: 'AVAIL_BY_SIGHT',
                confirmationEnumId: 'CONFIRM_WITHOUT'
            ]).call()
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("goodsDescription", "Steel Rods - 50mm diameter (standard)").one()

        when:
        ec.service.sync().name("trade.importlc.ImportLcValidationServices.validate#SwiftFields")
            .parameters([entityType: 'ImportLetterOfCredit', entityId: lc.instrumentId]).call()

        then:
        !ec.message.hasError()
        
        cleanup:
        ec.message.clearAll()
    }

    def "validate#BusinessStateTransition rejects invalid transition"() {
        given:
        ec.artifactExecution.disableAuthz()

        when:
        ec.service.sync().name("trade.importlc.ImportLcValidationServices.validate#BusinessStateTransition")
            .parameters([
                instrumentId: "123", 
                fromStateId: "LC_DRAFT",
                toStateId: "LC_CLOSED"
            ]).call()

        then:
        ec.message.hasError()
        ec.message.errorsString.contains("Invalid Business State transition from LC_DRAFT to LC_CLOSED")

        cleanup:
        ec.artifactExecution.enableAuthz()
        ec.message.clearAll()
    }
}
