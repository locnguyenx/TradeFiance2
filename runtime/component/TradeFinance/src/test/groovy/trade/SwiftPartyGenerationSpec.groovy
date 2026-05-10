package trade

import org.moqui.entity.EntityCondition
import org.moqui.context.ExecutionContext
import spock.lang.Specification
import spock.lang.Stepwise
import spock.lang.Shared
import org.slf4j.Logger
import org.slf4j.LoggerFactory

// ABOUTME: SwiftPartyGenerationSpec verifies SWIFT generation using the normalized TradeInstrumentParty junction.
@Stepwise
class SwiftPartyGenerationSpec extends Specification {
    protected final static Logger logger = LoggerFactory.getLogger(SwiftPartyGenerationSpec.class)
    @Shared ExecutionContext ec
    @Shared String testPrefix

    def setupSpec() {
        ec = org.moqui.Moqui.getExecutionContext()
        ec.user.loginUser("trade.admin", "trade123")
        ec.artifactExecution.disableAuthz()
        testPrefix = "SWTP-" + System.currentTimeMillis()
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
            ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.ImportLcAmendment").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.ImportLcShippingGuarantee").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeSettlement").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeDraft").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeTransaction").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            
            ec.entity.find("trade.TradePartyBank").condition("partyId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeParty").condition("partyId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            
            ec.transaction.commit(begun)
        } catch (Exception e) {
            ec.transaction.rollback(begun, "Error in cleanData", e)
            logger.warn("Cleanup failed: " + e.message)
        }
    }

    def "SWTP-01: MT700 generates correctly with Junction Parties"() {
        given: "Parties and an LC instrument with junction records"
        String ts = System.currentTimeMillis()
        String appPartyId = testPrefix + "_APP_" + ts
        String benPartyId = testPrefix + "_BEN_" + ts
        String advPartyId = testPrefix + "_ADV_" + ts
        String instrumentId = testPrefix + "_LC_" + ts

        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: appPartyId, partyTypeEnumId: 'PTY_COMMERCIAL', 
                             partyName: 'SWIFT Applicant', registeredAddress: '123 App Lane', 
                             kycStatus: 'KYC_ACTIVE']).call()
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: benPartyId, partyTypeEnumId: 'PTY_COMMERCIAL', 
                             partyName: 'SWIFT Beneficiary', registeredAddress: '456 Ben Blvd', 
                             kycStatus: 'KYC_ACTIVE']).call()
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: advPartyId, partyTypeEnumId: 'PTY_BANK', 
                             partyName: 'Advising Bank', kycStatus: 'KYC_ACTIVE', 
                             swiftBic: 'ADVISXXX', hasActiveRMA: true]).call()

        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([
                instrumentId: instrumentId,
                lcAmount: 75000.0,
                lcCurrencyUomId: 'USD',
                instrumentParties: [
                    [roleEnumId: 'TP_APPLICANT', partyId: appPartyId],
                    [roleEnumId: 'TP_BENEFICIARY', partyId: benPartyId],
                    [roleEnumId: 'TP_ADVISING_BANK', partyId: advPartyId]
                ]
            ]).call()
        assert !ec.message.hasError()
        def instrumentIdRes = res.instrumentId

        when: "MT700 is generated"
        def genRes = ec.service.sync().name("trade.SwiftGenerationServices.generate#Mt700")
            .parameters([instrumentId: instrumentId]).call()

        then: "SWIFT tags contain data from junction parties"
        !ec.message.hasError()
        def content = genRes.messageContent
        content != null
        // Normalize whitespace for comparison
        def cleanContent = content.replaceAll("\\s+", " ").toUpperCase()
        cleanContent.contains(":50:SWIFT APPLICANT 123 APP LANE")
        cleanContent.contains(":59:SWIFT BENEFICIARY 456 BEN BLVD")
        content.contains("ADVISXXX")
    }

    def "SWTP-02: MT700 Available With Bank (Tag 41A) from junction"() {
        given: "An LC with Negotiating Bank in junction"
        String ts = System.currentTimeMillis()
        String appPartyId = testPrefix + "_APP_2_" + ts
        String benPartyId = testPrefix + "_BEN_2_" + ts
        String negPartyId = testPrefix + "_NEG_" + ts
        String instrumentId = testPrefix + "_LC_2_" + ts

        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: appPartyId, partyTypeEnumId: 'PTY_COMMERCIAL', 
                             partyName: 'App', kycStatus: 'KYC_ACTIVE']).call()
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: benPartyId, partyTypeEnumId: 'PTY_COMMERCIAL', 
                             partyName: 'Ben', kycStatus: 'KYC_ACTIVE']).call()
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: negPartyId, partyTypeEnumId: 'PTY_BANK', 
                             partyName: 'Neg Bank', kycStatus: 'KYC_ACTIVE', 
                             swiftBic: 'NEGOTXXX', hasActiveRMA: true]).call()

        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([
                instrumentId: instrumentId,
                lcAmount: 50000.0,
                lcCurrencyUomId: 'USD',
                availableByEnumId: 'AVB_BY_NEGOTIATION',
                instrumentParties: [
                    [roleEnumId: 'TP_APPLICANT', partyId: appPartyId],
                    [roleEnumId: 'TP_BENEFICIARY', partyId: benPartyId],
                    [roleEnumId: 'TP_NEGOTIATING_BANK', partyId: negPartyId]
                ],
                availableWithEnumId: 'AW_SPECIFIC_BANK'
            ]).call()
        instrumentId = res.instrumentId

        when: "MT700 is generated"
        def genRes = ec.service.sync().name("trade.SwiftGenerationServices.generate#Mt700")
            .parameters([instrumentId: instrumentId]).call()

        then: "Tag 41A contains Negotiating Bank BIC"
        genRes.messageContent.contains(":41A:NEGOTXXX\r\nBY NEGOTIATION")
    }

    def "SWTP-03: MT700 Available With ANY BANK (Tag 41D) logic"() {
        given: "An LC with Available With = ANY_BANK"
        String ts = System.currentTimeMillis()
        String appPartyId = testPrefix + "_APP_3_" + ts
        String benPartyId = testPrefix + "_BEN_3_" + ts
        String instrumentId = testPrefix + "_LC_3_" + ts

        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: appPartyId, partyTypeEnumId: 'PTY_COMMERCIAL', 
                             partyName: 'App', kycStatus: 'KYC_ACTIVE']).call()
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: benPartyId, partyTypeEnumId: 'PTY_COMMERCIAL', 
                             partyName: 'Ben', kycStatus: 'KYC_ACTIVE']).call()

        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([
                instrumentId: instrumentId,
                lcAmount: 50000.0,
                lcCurrencyUomId: 'USD',
                availableWithEnumId: 'AW_ANY_BANK',
                availableByEnumId: 'AVB_BY_NEGOTIATION',
                instrumentParties: [
                    [roleEnumId: 'TP_APPLICANT', partyId: appPartyId],
                    [roleEnumId: 'TP_BENEFICIARY', partyId: benPartyId]
                ]
            ]).call()
        instrumentId = res.instrumentId

        when: "MT700 is generated"
        def genRes = ec.service.sync().name("trade.SwiftGenerationServices.generate#Mt700")
            .parameters([instrumentId: instrumentId]).call()

        then: "Tag 41D contains ANY BANK"
        genRes.messageContent.contains(":41D:ANY BANK\r\nBY NEGOTIATION")
    }
}
