package trade


import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import spock.lang.Specification
import spock.lang.Shared
import org.moqui.entity.EntityCondition

/**
 * ABOUTME: ImportLcValidationServicesSpec validates SWIFT character set compliance and instrument-specific business rules.
 */
class ImportLcValidationServicesSpec extends Specification {
    @Shared ExecutionContext ec
    @Shared String testPrefix
    @Shared String applicantId
    @Shared String beneficiaryId

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        println "DEBUG: setupSpec ImportLcValidationServicesSpec starting"
        ec.user.loginUser("trade.maker", "trade123")
        ec.artifactExecution.disableAuthz()
        testPrefix = "VAL-SRV-" + System.currentTimeMillis()
        
        applicantId = testPrefix + "-APP"
        beneficiaryId = testPrefix + "-BEN"
        
        // Ensure test parties exist
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
            .parameters([partyId: applicantId, partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'VAL ACME Corp', kycStatus: 'KYC_ACTIVE']).call()
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
            .parameters([partyId: beneficiaryId, partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'VAL Global Exports', kycStatus: 'KYC_ACTIVE']).call()

        // Set isolated ID generation ranges - use 2300000
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrument", 41000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit", 41000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeTransaction", 41000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrumentParty", 41000000, 1000)
        println "DEBUG: setupSpec ImportLcValidationServicesSpec complete"
    }

    def cleanupSpec() {
        if (ec != null) {
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrument")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeTransaction")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrumentParty")
            ec.destroy()
        }
    }

    def setup() {
        ec.message.clearAll()
        ec.artifactExecution.disableAuthz()
    }

    def cleanup() {
        ec.message.clearAll()
    }

    def "validate#SwiftFields rejects invalid X-Character in goodsDescription"() {
        given:
        def ref = testPrefix + "-BAD"
        def createRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([
                instrumentRef: ref, 
                goodsDescription: "Steel Rods @ 50mm diameter", 
                lcAmount: 1000.0, 
                lcCurrencyUomId: 'USD',
                lcTypeEnumId: 'LCT_IRREVOCABLE',
                availableByEnumId: 'AVB_BY_SIGHT_PAYMENT',
                confirmationEnumId: 'CONF_WITHOUT',
                availableWithEnumId: 'AW_ANY_BANK',
                productCatalogId: 'PROD_IMP_LC',
                expiryDate: '2026-12-31',
                instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId],
                                    [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]]
            ]).call()
        if (ec.message.hasError()) throw new Exception("LC creation failed: " + ec.message.errorsString)
        def instrumentId = createRes.instrumentId

        when:
        def result = ec.service.sync().name("trade.importlc.ImportLcValidationServices.validate#SwiftFields")
            .parameters([entityType: 'ImportLetterOfCredit', entityId: instrumentId]).call()

        then:
        result.errors != null
        result.errors.any { it.fieldName == "goodsDescription" && it.message.contains("@") }
    }

    def "validate#SwiftFields accepts valid characters"() {
        given:
        def ref = testPrefix + "-GOOD"
        def createRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([
                instrumentRef: ref, 
                goodsDescription: "Steel Rods - 50mm diameter (standard)", 
                lcAmount: 1000.0, 
                lcCurrencyUomId: 'USD',
                lcTypeEnumId: 'LCT_IRREVOCABLE',
                availableByEnumId: 'AVB_BY_SIGHT_PAYMENT',
                confirmationEnumId: 'CONF_WITHOUT',
                availableWithEnumId: 'AW_ANY_BANK',
                productCatalogId: 'PROD_IMP_LC',
                expiryDate: '2026-12-31',
                instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId],
                                    [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]]
            ]).call()
        if (ec.message.hasError()) throw new Exception("LC creation failed: " + ec.message.errorsString)
        def instrumentId = createRes.instrumentId

        when:
        def result = ec.service.sync().name("trade.importlc.ImportLcValidationServices.validate#SwiftFields")
            .parameters([entityType: 'ImportLetterOfCredit', entityId: instrumentId]).call()

        then:
        result.errors == null || result.errors.size() == 0
    }
}
