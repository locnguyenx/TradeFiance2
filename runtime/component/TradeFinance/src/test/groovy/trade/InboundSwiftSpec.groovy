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
    @Shared long idOffset
    @Shared String testPrefix

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        long now = System.currentTimeMillis()
        idOffset = (now % 10000) * 1000 // Dynamic range
        testPrefix = "T" + (now % 100000)
        ec.artifactExecution.disableAuthz()
        // Force load seed data
        ec.entity.makeDataLoader().location("component://TradeFinance/data/InboundSwiftSeedData.xml").load()
        ec.entity.makeDataLoader().location("component://TradeFinance/data/TradeFinanceSeedData.xml").load()
        
        // Ensure USD exists
        if (!ec.entity.find("moqui.basic.Uom").condition("uomId", "USD").one()) {
            ec.entity.makeValue("moqui.basic.Uom").setAll([uomId: "USD", uomTypeEnumId: "UT_CURRENCY_MEASURE", description: "US Dollar"]).create()
        }
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrument", 60000000 + idOffset, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit", 60000000 + idOffset, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLcAmendment", 60000000 + idOffset, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeTransaction", 60000000 + idOffset, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.TradeDocumentPresentation", 60000000 + idOffset, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.swift.InboundSwiftRaw", 60000000 + idOffset, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.swift.TradeInboxItem", 60000000 + idOffset, 1000)
    }

    def cleanupSpec() {
        ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrument")
        ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit")
        ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLcAmendment")
        ec.entity.tempResetSequencedIdPrimary("trade.TradeTransaction")
        ec.entity.tempResetSequencedIdPrimary("trade.importlc.TradeDocumentPresentation")
        ec.destroy()
    }

    def setup() {
        ec.message.clearAll()
        ec.artifactExecution.disableAuthz()
    }

    def "ingest#SwiftMessage creates InboundSwiftRaw with SHA-256 hash"() {
        given:
        def sampleMt730 = """{1:F01VIETBANK1XXX0000000000}{2:O7301200260515VIETBANK1XXX0000000000N}{4:
:20:APP-REF-001
:21:ADV-REF-001
:30:260515
:71B:CHARGES DETS
-}"""

        when:
        def result = ec.service.sync().name("trade.swift.InboundSwiftServices.ingest#SwiftMessage")
            .parameters([
                rawContent: sampleMt730,
                sourceChannel: "SRC_MANUAL_UPLOAD",
                sourceFileName: "MT730_test.txt",
                skipCorrelation: true
            ]).call()

        then:
        result.rawMessageId != null
        def raw = ec.entity.find("trade.swift.InboundSwiftRaw")
            .condition("rawMessageId", result.rawMessageId).one()
        if (raw.parseStatusEnumId != "PARSE_SUCCESS") println "DEBUG: Parse failure: ${raw.parseStatusEnumId} - ${raw.parseErrorText}"
        raw.contentHash != null
        raw.contentHash.length() == 64 // SHA-256 hex
        raw.parseStatusEnumId == "PARSE_SUCCESS"
        raw.messageType == "730"
    }

    def "correlate#InboxItem matches MT730 to existing LC"() {
        given:
        ec.artifactExecution.disableAuthz()

        // Create parties
        def advisingBankId = "${testPrefix}-ADV-BANK-730"
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
            .parameters([
                partyId: advisingBankId, partyTypeEnumId: 'PTY_BANK', partyName: 'Advising Bank 730',
                kycStatus: 'KYC_ACTIVE', hasActiveRMA: true, swiftBic: 'VIETBANK1XXX'
            ]).call()

        // Create an LC
        def lcResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([
                externalId: "${testPrefix}-EXT-730",
                instrumentRef: "${testPrefix}-LC-REF-730",
                businessStateId: "LC_ISSUED",
                lcAmount: 100000,
                lcCurrencyUomId: "USD",
                instrumentParties: [
                    [roleEnumId: 'TP_APPLICANT', partyId: '_NA_'],
                    [roleEnumId: 'TP_BENEFICIARY', partyId: '_NA_'],
                    [roleEnumId: 'TP_ADVISING_BANK', partyId: advisingBankId]
                ]
            ]).call()
        def instrumentId = lcResult?.instrumentId
        
        def sampleMt730 = """{1:F01VIETBANK1XXX0000000000}{2:O7301200260515VIETBANK1XXX0000000000N}{4:
:20:${testPrefix}-REF-730
:21:${testPrefix}-LC-REF-730
:30:260515
:71B:CHARGES DETS
-}"""

        when:
        // 1. Ingest
        def ingestResult = ec.service.sync().name("trade.swift.InboundSwiftServices.ingest#SwiftMessage")
            .parameters([rawContent: sampleMt730, sourceChannel: "SRC_MANUAL", skipCorrelation: true]).call()
        def rawId = ingestResult?.rawMessageId
        // 2. Correlate
        def corrResult = ec.service.sync().name("trade.swift.InboundCorrelationServices.correlate#SwiftMessage")
            .parameter("rawMessageId", rawId).call()

        then:
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        lc.isAdvised == 'Y'
        lc.advisedDate != null
        
        def item = ec.entity.find("trade.swift.TradeInboxItem").condition("rawMessageId", rawId).one()

        item.correlationStatusEnumId == 'CORR_AUTO_MATCH'
        item.inboxStatusEnumId == 'INBOX_PROCESSED'
        item.actionTaken == "LC_DELIVERY_CONFIRMED"
    }


}
