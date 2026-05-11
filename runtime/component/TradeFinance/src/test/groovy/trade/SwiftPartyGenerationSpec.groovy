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
        println "DEBUG: setupSpec SwiftPartyGenerationSpec starting"
        ec.user.loginUser("trade.maker", "trade123")
        ec.artifactExecution.disableAuthz()
        testPrefix = "SWTP-" + System.currentTimeMillis()

        // Set isolated ID generation ranges - use 1500000
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrument", 33000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit", 33000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeTransaction", 33000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrumentParty", 33000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.SwiftMessage", 33000000, 1000)
        println "DEBUG: setupSpec SwiftPartyGenerationSpec complete"
    }

    def cleanupSpec() {
        if (ec != null) {
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrument")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeTransaction")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrumentParty")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.SwiftMessage")
            ec.destroy()
        }
    }

    def "SWTP-01: MT700 generates correctly with Junction Parties"() {
        given: "Parties and an LC instrument with junction records"
        String ts = System.currentTimeMillis()
        String appPartyId = testPrefix + "_APP_" + ts
        String benPartyId = testPrefix + "_BEN_" + ts
        String advPartyId = testPrefix + "_ADV_" + ts

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
                lcAmount: 75000.0,
                lcCurrencyUomId: 'USD',
                instrumentParties: [
                    [roleEnumId: 'TP_APPLICANT', partyId: appPartyId],
                    [roleEnumId: 'TP_BENEFICIARY', partyId: benPartyId],
                    [roleEnumId: 'TP_ADVISING_BANK', partyId: advPartyId]
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
        def instrumentId = res.instrumentId

        when: "MT700 is generated"
        def genRes = ec.service.sync().name("trade.SwiftGenerationServices.generate#Mt700")
            .parameters([instrumentId: instrumentId]).call()

        then: "MT700 contains Negotiating Bank BIC"
        !ec.message.hasError()
        def content = genRes.messageContent
        content.contains("NEGOTXXX")
    }
}
