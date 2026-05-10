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

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.user.internalLoginUser("trade.maker")
        ec.artifactExecution.disableAuthz()
        testPrefix = "VAL-SRV-" + System.currentTimeMillis()
        cleanData()
    }

    def cleanupSpec() {
        if (ec != null) {
            cleanData()
            ec.destroy()
        }
    }

    private void cleanData() {
        boolean begun = ec.transaction.begin(60)
        try {
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").updateAll([latestTransactionId: null])
            ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.transaction.commit(begun)
        } catch (Exception e) {
            ec.transaction.rollback(begun, "Error in cleanData", e)
        }
    }

    def "validate#SwiftFields rejects invalid X-Character in goodsDescription"() {
        given:
        def ref = testPrefix + "-BAD"
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([
                instrumentId: testPrefix + "_LC_BAD",
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
                instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                    [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]
            ]).call()
        if (ec.message.hasError()) throw new Exception("LC creation failed: " + ec.message.errorsString)
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", testPrefix + "_LC_BAD").one()

        when:
        def result = ec.service.sync().name("trade.importlc.ImportLcValidationServices.validate#SwiftFields")
            .parameters([entityType: 'ImportLetterOfCredit', entityId: lc.instrumentId]).call()

        then:
        result.errors != null
        result.errors.any { it.fieldName == "goodsDescription" && it.message.contains("@") }
        
        cleanup:
        ec.message.clearAll()
    }

    def "validate#SwiftFields accepts valid characters"() {
        given:
        def ref = testPrefix + "-GOOD"
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([
                instrumentId: testPrefix + "_LC_GOOD",
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
                instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                    [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]
            ]).call()
        if (ec.message.hasError()) throw new Exception("LC creation failed: " + ec.message.errorsString)
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", testPrefix + "_LC_GOOD").one()

        when:
        def result = ec.service.sync().name("trade.importlc.ImportLcValidationServices.validate#SwiftFields")
            .parameters([entityType: 'ImportLetterOfCredit', entityId: lc.instrumentId]).call()

        then:
        result.errors == []
        
        cleanup:
        ec.message.clearAll()
    }

    def "validate#BusinessStateTransition rejects invalid transition"() {
        given:
        ec.artifactExecution.disableAuthz()

        when:
        ec.service.sync().name("trade.importlc.ImportLcValidationServices.validate#BusinessStateTransition")
            .parameters([
                instrumentId: testPrefix + "_123", 
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
