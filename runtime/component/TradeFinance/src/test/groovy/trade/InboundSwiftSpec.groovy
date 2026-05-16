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

    def "correlate#InboxItem matches MT799 and accepts amendment"() {
        given:
        ec.artifactExecution.disableAuthz()

        // Create party
        def advisingBankId = "${testPrefix}-ADV-BANK-799"
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
            .parameters([
                partyId: advisingBankId, partyTypeEnumId: 'PTY_BANK', partyName: 'Advising Bank 799',
                kycStatus: 'KYC_ACTIVE', hasActiveRMA: true, swiftBic: 'VIETBANK1XXX'
            ]).call()

        // 1. Create LC and Amendment
        def lcResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([
                instrumentRef: "${testPrefix}-LC-REF-799", lcAmount: 100000, lcCurrencyUomId: "USD",
                businessStateId: "LC_ISSUED",
                instrumentParties: [
                    [roleEnumId: 'TP_APPLICANT', partyId: '_NA_'],
                    [roleEnumId: 'TP_BENEFICIARY', partyId: '_NA_'],
                    [roleEnumId: 'TP_ADVISING_BANK', partyId: advisingBankId]
                ]
            ]).call()
        def instId = lcResult?.instrumentId
        def txnId = lcResult?.transactionId
        if (txnId) ec.service.sync().name("update#trade.TradeTransaction").parameters([transactionId: txnId, transactionStatusId: 'TX_APPROVED']).call()
        
        def amdResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#Amendment")
            .parameters([
                instrumentId: instId, 
                amendmentRef: "${testPrefix}-AMD-REF-799", 
                transactionRef: "${testPrefix}-AMD-REF-799"
            ]).call()
        def amdId = amdResult?.amendmentId

        def sampleMt799 = """{1:F01VIETBANK1XXX0000000000}{2:O7991200260515VIETBANK1XXX0000000000N}{4:
:20:${testPrefix}-REF-799
:21:${testPrefix}-AMD-REF-799
:79:WE ACKNOWLEDGE AND ACCEPT THE AMENDMENT
-}"""

        when:
        def ingestResult = ec.service.sync().name("trade.swift.InboundSwiftServices.ingest#SwiftMessage")
            .parameters([rawContent: sampleMt799, sourceChannel: "SRC_MANUAL", skipCorrelation: true]).call()
        
        ec.service.sync().name("trade.swift.InboundCorrelationServices.correlate#SwiftMessage")
            .parameter("rawMessageId", ingestResult?.rawMessageId).call()

        then:
        def item = ec.entity.find("trade.swift.TradeInboxItem").condition("rawMessageId", ingestResult?.rawMessageId).one()
        
        item != null
        item.correlationStatusEnumId == 'CORR_AUTO_MATCH'
        item.actionTaken == "AMENDMENT_AUTO_ACCEPTED_799"
    }

    def "correlate#InboxItem matches MT750 and spawns presentation"() {
        given:
        ec.artifactExecution.disableAuthz()

        // Create party
        def advisingBankId = "${testPrefix}-ADV-BANK-750"
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
            .parameters([
                partyId: advisingBankId, partyTypeEnumId: 'PTY_BANK', partyName: 'Advising Bank 750',
                kycStatus: 'KYC_ACTIVE', hasActiveRMA: true, swiftBic: 'VIETBANK1XXX'
            ]).call()

        def lcResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([
                instrumentRef: "${testPrefix}-LC-REF-750", lcAmount: 100000, lcCurrencyUomId: "USD",
                businessStateId: "LC_ISSUED",
                instrumentParties: [
                    [roleEnumId: 'TP_APPLICANT', partyId: '_NA_'],
                    [roleEnumId: 'TP_BENEFICIARY', partyId: '_NA_'],
                    [roleEnumId: 'TP_ADVISING_BANK', partyId: advisingBankId]
                ]
            ]).call()
        def instId = lcResult?.instrumentId
        def txnId = lcResult?.transactionId
        
        // Approve the issuance transaction so we can lodge a presentation
        if (txnId) ec.service.sync().name("update#trade.TradeTransaction").parameters([transactionId: txnId, transactionStatusId: 'TX_APPROVED']).call()

        def sampleMt750 = """{1:F01VIETBANK1XXX0000000000}{2:O7501200260515VIETBANK1XXX0000000000N}{4:
:20:${testPrefix}-REF-750
:21:${testPrefix}-LC-REF-750
:32B:USD5000,00
:77A:DISCREPANCIES FOUND...
-}"""

        when:
        def ingestResult = ec.service.sync().name("trade.swift.InboundSwiftServices.ingest#SwiftMessage")
            .parameters([rawContent: sampleMt750, sourceChannel: "SRC_MANUAL", skipCorrelation: true]).call()

        ec.service.sync().name("trade.swift.InboundCorrelationServices.correlate#SwiftMessage")
            .parameter("rawMessageId", ingestResult?.rawMessageId).call()

        then:
        def item = ec.entity.find("trade.swift.TradeInboxItem").condition("rawMessageId", ingestResult?.rawMessageId).one()
        
        item != null
        item.presentationId != null
        def pres = ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("presentationId", item.presentationId).one()
        
        pres.claimAmount == 5000.0
        pres.sourceChannel == 'INBOUND'
        item.actionTaken == 'SPAWN_PRESENTATION_750'
    }

    def "correlate#SwiftMessage matches MT754 and fast-tracks to LC_ACCEPTED"() {
        given:
        ec.artifactExecution.disableAuthz()

        def lcResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([
                instrumentRef: "${testPrefix}-LC-REF-754", lcAmount: 100000, lcCurrencyUomId: "USD",
                businessStateId: "LC_ISSUED",
                instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: '_NA_'], [roleEnumId: 'TP_BENEFICIARY', partyId: '_NA_']]
            ]).call()
        def instId = lcResult?.instrumentId
        if (lcResult?.transactionId) ec.service.sync().name("update#trade.TradeTransaction").parameters([transactionId: lcResult.transactionId, transactionStatusId: 'TX_APPROVED']).call()

        def sampleMt754 = """{1:F01VIETBANK1XXX0000000000}{2:O7541200260515VIETBANK1XXX0000000000N}{4:
:20:${testPrefix}-REF-754
:21:${testPrefix}-LC-REF-754
:32B:USD8000,00
-}"""

        when:
        def ingestResult = ec.service.sync().name("trade.swift.InboundSwiftServices.ingest#SwiftMessage")
            .parameters([rawContent: sampleMt754, sourceChannel: "SRC_MANUAL", skipCorrelation: true]).call()

        ec.service.sync().name("trade.swift.InboundCorrelationServices.correlate#SwiftMessage")
            .parameter("rawMessageId", ingestResult?.rawMessageId).call()

        then:
        def item = ec.entity.find("trade.swift.TradeInboxItem").condition("rawMessageId", ingestResult?.rawMessageId).one()
        item.actionTaken == 'PRESENTATION_AUTO_SPAWNED_754'
        
        def pres = ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("presentationId", item.presentationId).one()
        pres.presentationStatusId == 'PRES_COMPLIANT'
        pres.isDiscrepant == 'N'

        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instId).one()
        lc.businessStateId == 'LC_ACCEPTED'
    }

    def "correlate#SwiftMessage matches MT742 and reconciles reimbursement"() {
        given:
        ec.artifactExecution.disableAuthz()

        def lcResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([
                instrumentRef: "${testPrefix}-LC-REF-742", lcAmount: 100000, lcCurrencyUomId: "USD",
                businessStateId: "LC_ISSUED",
                instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: '_NA_'], [roleEnumId: 'TP_BENEFICIARY', partyId: '_NA_']]
            ]).call()
        def instId = lcResult?.instrumentId

        // Create a pending reconciliation
        def reconResult = ec.service.sync().name("create#trade.importlc.NostroReconciliation")
            .parameters([instrumentId: instId, expectedAmount: 10000, matchStatusEnumId: 'RECON_PENDING']).call()
        def reconId = reconResult.reconciliationId

        def sampleMt742 = """{1:F01VIETBANK1XXX0000000000}{2:O7421200260515VIETBANK1XXX0000000000N}{4:
:20:${testPrefix}-REF-742
:21:${testPrefix}-LC-REF-742
:32B:USD9000,00
-}"""

        when:
        def ingestResult = ec.service.sync().name("trade.swift.InboundSwiftServices.ingest#SwiftMessage")
            .parameters([rawContent: sampleMt742, sourceChannel: "SRC_MANUAL", skipCorrelation: true]).call()

        ec.service.sync().name("trade.swift.InboundCorrelationServices.correlate#SwiftMessage")
            .parameter("rawMessageId", ingestResult?.rawMessageId).call()

        then:
        def item = ec.entity.find("trade.swift.TradeInboxItem").condition("rawMessageId", ingestResult?.rawMessageId).one()
        item.actionTaken == 'REIMBURSEMENT_AUTO_MATCHED'

        def recon = ec.entity.find("trade.importlc.NostroReconciliation").condition("reconciliationId", reconId).one()
        recon.matchStatusEnumId == 'RECON_MATCHED'
        recon.matchedAmount == 9000.0
    }
}
