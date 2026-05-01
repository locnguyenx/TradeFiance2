package trade

import org.moqui.entity.EntityCondition
import org.moqui.context.ExecutionContext
import spock.lang.Specification
import spock.lang.Stepwise
import org.slf4j.Logger
import org.slf4j.LoggerFactory

// ABOUTME: SwiftPartyGenerationSpec verifies SWIFT generation using the normalized TradeInstrumentParty junction.
@Stepwise
class SwiftPartyGenerationSpec extends Specification {
    protected final static Logger logger = LoggerFactory.getLogger(SwiftPartyGenerationSpec.class)
    private static ExecutionContext ec

    def setupSpec() {
        ec = org.moqui.Moqui.getExecutionContext()
        ec.user.loginUser("trade.admin", "trade123")
        ec.artifactExecution.disableAuthz()
        cleanData()
    }

    def cleanupSpec() {
        ec.destroy()
    }

    private static void cleanData() {
        boolean began = ec.transaction.begin(60)
        try {
            ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", EntityCondition.LIKE, "SWTP_%").deleteAll()
            ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", EntityCondition.LIKE, "SWTP_%").deleteAll()
            ec.entity.find("trade.TradeInstrumentParty").condition("partyId", EntityCondition.LIKE, "SWTP_%").deleteAll()
            ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", EntityCondition.LIKE, "SWTP_%").deleteAll()
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", EntityCondition.LIKE, "SWTP_%").deleteAll()
            ec.entity.find("trade.TradePartyBank").condition("partyId", EntityCondition.LIKE, "SWTP_%").deleteAll()
            ec.entity.find("trade.TradeParty").condition("partyId", EntityCondition.LIKE, "SWTP_%").deleteAll()
            ec.entity.find("mantle.party.Party").condition("partyId", EntityCondition.LIKE, "SWTP_%").deleteAll()
            ec.transaction.commit(began)
        } catch (Exception e) {
            ec.transaction.rollback(began, "Error in cleanData", e)
            throw e
        }
    }

    def "SWTP-01: MT700 generates correctly with Junction Parties"() {
        given: "Parties and an LC instrument with junction records"
        ec.entity.makeValue("trade.TradeParty").setAll([partyId: 'SWTP_APP_001', partyTypeEnumId: 'PARTY_COMMERCIAL', partyName: 'SWIFT Applicant', registeredAddress: '123 App Lane\nNew York', kycStatus: 'Active']).createOrUpdate()
        ec.entity.makeValue("trade.TradeParty").setAll([partyId: 'SWTP_BEN_001', partyTypeEnumId: 'PARTY_COMMERCIAL', partyName: 'SWIFT Beneficiary', registeredAddress: '456 Ben Blvd\nLondon', kycStatus: 'Active']).createOrUpdate()
        ec.entity.makeValue("trade.TradeParty").setAll([partyId: 'SWTP_ADV_001', partyTypeEnumId: 'PARTY_BANK', partyName: 'Advising Bank', kycStatus: 'Active']).createOrUpdate()
        ec.entity.makeValue("trade.TradePartyBank").setAll([partyId: 'SWTP_ADV_001', swiftBic: 'ADVISXXX', hasActiveRMA: 'Y']).createOrUpdate()

        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([
                instrumentId: 'SWTP_LC_001',
                lcAmount: 75000.0,
                lcCurrencyUomId: 'USD',
                instrumentParties: [
                    [roleEnumId: 'TP_APPLICANT', partyId: 'SWTP_APP_001'],
                    [roleEnumId: 'TP_BENEFICIARY', partyId: 'SWTP_BEN_001'],
                    [roleEnumId: 'TP_ADVISING_BANK', partyId: 'SWTP_ADV_001']
                ]
            ]).call()
        assert !ec.message.hasError()
        def instrumentId = res.instrumentId

        when: "MT700 is generated"
        def genRes = ec.service.sync().name("trade.SwiftGenerationServices.generate#Mt700")
            .parameters([instrumentId: instrumentId]).call()

        then: "SWIFT tags contain data from junction parties"
        !ec.message.hasError()
        def content = genRes.messageContent
        content != null
        // Normalize whitespace for comparison
        def cleanContent = content.replaceAll("\\s+", " ").toUpperCase()
        cleanContent.contains(":50:SWIFT APPLICANT 123 APP LANE NEW YORK")
        cleanContent.contains(":59:SWIFT BENEFICIARY 456 BEN BLVD LONDON")
        content.contains("ADVISXXX")
    }

    def "SWTP-02: MT700 Available With Bank (Tag 41A) from junction"() {
        given: "An LC with Negotiating Bank in junction"
        ec.entity.makeValue("trade.TradeParty").setAll([partyId: 'SWTP_APP_002', partyTypeEnumId: 'PARTY_COMMERCIAL', partyName: 'App', kycStatus: 'Active']).createOrUpdate()
        ec.entity.makeValue("trade.TradeParty").setAll([partyId: 'SWTP_BEN_002', partyTypeEnumId: 'PARTY_COMMERCIAL', partyName: 'Ben', kycStatus: 'Active']).createOrUpdate()
        ec.entity.makeValue("trade.TradeParty").setAll([partyId: 'SWTP_NEG_001', partyTypeEnumId: 'PARTY_BANK', partyName: 'Neg Bank', kycStatus: 'Active']).createOrUpdate()
        ec.entity.makeValue("trade.TradePartyBank").setAll([partyId: 'SWTP_NEG_001', swiftBic: 'NEGOTXXX', hasActiveRMA: 'Y']).createOrUpdate()

        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([
                instrumentId: 'SWTP_LC_002',
                lcAmount: 50000.0,
                lcCurrencyUomId: 'USD',
                availableWithEnumId: 'BANK',
                availableByEnumId: 'NEGOTIATION',
                instrumentParties: [
                    [roleEnumId: 'TP_APPLICANT', partyId: 'SWTP_APP_002'],
                    [roleEnumId: 'TP_BENEFICIARY', partyId: 'SWTP_BEN_002'],
                    [roleEnumId: 'TP_NEGOTIATING_BANK', partyId: 'SWTP_NEG_001']
                ]
            ]).call()
        def instrumentId = res.instrumentId

        when: "MT700 is generated"
        def genRes = ec.service.sync().name("trade.SwiftGenerationServices.generate#Mt700")
            .parameters([instrumentId: instrumentId]).call()

        then: "Tag 41A contains Negotiating Bank BIC"
        genRes.messageContent.contains(":41A:NEGOTXXX\r\nBY NEGOTIATION")
    }

    def "SWTP-03: MT700 Available With ANY BANK (Tag 41D) logic"() {
        given: "An LC with Available With = ANY_BANK"
        ec.entity.makeValue("trade.TradeParty").setAll([partyId: 'SWTP_APP_003', partyTypeEnumId: 'PARTY_COMMERCIAL', partyName: 'App', kycStatus: 'Active']).createOrUpdate()
        ec.entity.makeValue("trade.TradeParty").setAll([partyId: 'SWTP_BEN_003', partyTypeEnumId: 'PARTY_COMMERCIAL', partyName: 'Ben', kycStatus: 'Active']).createOrUpdate()

        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([
                instrumentId: 'SWTP_LC_003',
                lcAmount: 50000.0,
                lcCurrencyUomId: 'USD',
                availableWithEnumId: 'AVAIL_ANY_BANK',
                availableByEnumId: 'NEGOTIATION',
                instrumentParties: [
                    [roleEnumId: 'TP_APPLICANT', partyId: 'SWTP_APP_003'],
                    [roleEnumId: 'TP_BENEFICIARY', partyId: 'SWTP_BEN_003']
                ]
            ]).call()
        def instrumentId = res.instrumentId

        when: "MT700 is generated"
        def genRes = ec.service.sync().name("trade.SwiftGenerationServices.generate#Mt700")
            .parameters([instrumentId: instrumentId]).call()

        then: "Tag 41D contains ANY BANK"
        genRes.messageContent.contains(":41D:ANY BANK\r\nBY NEGOTIATION")
    }
}
