package trade

import spock.lang.Specification
import org.moqui.context.ExecutionContext
import org.moqui.Moqui
import spock.lang.Shared

/**
 * ABOUTME: InboundActionSpec tests the business actions triggered by specific MT message types (730, 799, 750, etc.).
 */
class InboundActionSpec extends Specification {
    @Shared protected ExecutionContext ec

    @Shared long idOffset
    @Shared String testPrefix

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        long now = System.currentTimeMillis()
        idOffset = (now % 10000) * 1000
        testPrefix = "A" + (now % 100000)
        ec.artifactExecution.disableAuthz()
        
        ec.entity.makeDataLoader().location("component://TradeFinance/data/InboundSwiftSeedData.xml").load()
        ec.entity.makeDataLoader().location("component://TradeFinance/data/TradeFinanceSeedData.xml").load()
        if (!ec.entity.find("moqui.basic.Uom").condition("uomId", "USD").one()) {
            ec.entity.makeValue("moqui.basic.Uom").setAll([uomId: "USD", uomTypeEnumId: "UT_CURRENCY_MEASURE", description: "US Dollar"]).create()
        }
        
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrument", 70000000 + idOffset, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit", 70000000 + idOffset, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLcAmendment", 70000000 + idOffset, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeTransaction", 70000000 + idOffset, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.TradeDocumentPresentation", 70000000 + idOffset, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.swift.InboundSwiftRaw", 70000000 + idOffset, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.swift.TradeInboxItem", 70000000 + idOffset, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.NostroReconciliation", 70000000 + idOffset, 1000)
    }

    def cleanupSpec() {
        ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrument")
        ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit")
        ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLcAmendment")
        ec.entity.tempResetSequencedIdPrimary("trade.TradeTransaction")
        ec.entity.tempResetSequencedIdPrimary("trade.importlc.TradeDocumentPresentation")
        ec.entity.tempResetSequencedIdPrimary("trade.swift.InboundSwiftRaw")
        ec.entity.tempResetSequencedIdPrimary("trade.swift.TradeInboxItem")
        ec.entity.tempResetSequencedIdPrimary("trade.importlc.NostroReconciliation")
        ec.destroy()
    }

    def setup() {
        ec.message.clearAll()
        ec.artifactExecution.disableAuthz()
    }

    def "InboundActionServices.acknowledge#Mt730 updates LC with advice details"() {
        given:
        def advisingBankId = "${testPrefix}-ADV-BANK-730"
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
            .parameters([partyId: advisingBankId, partyTypeEnumId: 'PTY_BANK', partyName: 'Advising Bank 730', kycStatus: 'KYC_ACTIVE', swiftBic: 'VIETBANK1XXX', hasActiveRMA: true]).call()

        def lcResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: "${testPrefix}-LC-REF-730", lcAmount: 100000, lcCurrencyUomId: "USD", businessStateId: "LC_ISSUED", instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: '_NA_'], [roleEnumId: 'TP_BENEFICIARY', partyId: '_NA_'], [roleEnumId: 'TP_ADVISING_BANK', partyId: advisingBankId]]]).call()
        def instId = lcResult?.instrumentId

        def sampleMt730 = """{1:F01VIETBANK1XXX0000000000}{2:O7301200260515VIETBANK1XXX0000000000N}{4:\n:20:${testPrefix}-REF-730\n:21:${testPrefix}-LC-REF-730\n:30:260515\n:71B:CHARGES DETS\n-}"""
        def rawId = ec.entity.makeValue("trade.swift.InboundSwiftRaw").setAll([rawContent: sampleMt730, messageType: "730", parseStatusEnumId: "PARSE_SUCCESS", contentHash: "hash730", swiftHeaderKey: "hdr730", sourceChannel: "SRC_MANUAL_UPLOAD"]).setSequencedIdPrimary().create().rawMessageId
        def itemId = ec.entity.makeValue("trade.swift.TradeInboxItem").setAll([rawMessageId: rawId, instrumentId: instId, messageType: "730", inboxStatusEnumId: "INBOX_UNREAD", correlationStatusEnumId: "CORR_AUTO_MATCH"]).setSequencedIdPrimary().create().inboxItemId

        when:
        ec.service.sync().name("trade.swift.InboundActionServices.acknowledge#Mt730").parameters([inboxItemId: itemId]).call()

        then:
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instId).one()
        lc.isAdvised == 'Y'
        lc.advisedDate != null
        
        def item = ec.entity.find("trade.swift.TradeInboxItem").condition("inboxItemId", itemId).one()
        item.actionTaken == "LC_DELIVERY_CONFIRMED"
        item.inboxStatusEnumId == "INBOX_PROCESSED"
    }

    def "InboundActionServices.resolve#AmendmentConsent accepts amendment for MT799"() {
        given:
        def advisingBankId = "${testPrefix}-ADV-BANK-799"
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
            .parameters([partyId: advisingBankId, partyTypeEnumId: 'PTY_BANK', partyName: 'Advising Bank 799', kycStatus: 'KYC_ACTIVE', hasActiveRMA: true]).call()

        def lcResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: "${testPrefix}-LC-REF-799", lcAmount: 100000, lcCurrencyUomId: "USD", businessStateId: "LC_ISSUED", instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: '_NA_'], [roleEnumId: 'TP_BENEFICIARY', partyId: '_NA_']]]).call()
        def instId = lcResult?.instrumentId
        def txnId = lcResult?.transactionId
        if (txnId) ec.service.sync().name("update#trade.TradeTransaction").parameters([transactionId: txnId, transactionStatusId: 'TX_APPROVED']).call()
        
        def amdResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#Amendment")
            .parameters([instrumentId: instId, amendmentRef: "${testPrefix}-AMD-REF-799", transactionRef: "${testPrefix}-AMD-REF-799"]).call()
        def amdId = amdResult?.amendmentId

        def sampleMt799 = """{1:F01VIETBANK1XXX0000000000}{2:O7991200260515VIETBANK1XXX0000000000N}{4:\n:20:${testPrefix}-REF-799\n:21:${testPrefix}-AMD-REF-799\n:79:WE ACKNOWLEDGE AND ACCEPT THE AMENDMENT\n-}"""
        def rawId = ec.entity.makeValue("trade.swift.InboundSwiftRaw").setAll([rawContent: sampleMt799, messageType: "799", parseStatusEnumId: "PARSE_SUCCESS", contentHash: "hash799", swiftHeaderKey: "hdr799", sourceChannel: "SRC_MANUAL_UPLOAD"]).setSequencedIdPrimary().create().rawMessageId
        def itemId = ec.entity.makeValue("trade.swift.TradeInboxItem").setAll([rawMessageId: rawId, instrumentId: instId, amendmentId: amdId, messageType: "799", inboxStatusEnumId: "INBOX_UNREAD", correlationStatusEnumId: "CORR_AUTO_MATCH"]).setSequencedIdPrimary().create().inboxItemId

        when:
        ec.service.sync().name("trade.swift.InboundActionServices.resolve#AmendmentConsent").parameters([inboxItemId: itemId]).call()

        then:
        def item = ec.entity.find("trade.swift.TradeInboxItem").condition("inboxItemId", itemId).one()
        item.actionTaken == "AMENDMENT_AUTO_ACCEPTED_799"
        item.inboxStatusEnumId == "INBOX_PROCESSED"
    }

    def "InboundActionServices.spawn#Presentation750 spawns discrepant presentation"() {
        given:
        def lcResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: "${testPrefix}-LC-REF-750", lcAmount: 100000, lcCurrencyUomId: "USD", businessStateId: "LC_ISSUED", instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: '_NA_'], [roleEnumId: 'TP_BENEFICIARY', partyId: '_NA_']]]).call()
        def instId = lcResult?.instrumentId
        if (lcResult?.transactionId) ec.service.sync().name("update#trade.TradeTransaction").parameters([transactionId: lcResult.transactionId, transactionStatusId: 'TX_APPROVED']).call()

        def sampleMt750 = "dummy"
        def rawId = ec.entity.makeValue("trade.swift.InboundSwiftRaw").setAll([rawContent: sampleMt750, messageType: "750", parseStatusEnumId: "PARSE_SUCCESS", contentHash: "hash750", swiftHeaderKey: "hdr750", sourceChannel: "SRC_MANUAL_UPLOAD"]).setSequencedIdPrimary().create().rawMessageId
        def itemId = ec.entity.makeValue("trade.swift.TradeInboxItem").setAll([rawMessageId: rawId, instrumentId: instId, messageType: "750", inboxStatusEnumId: "INBOX_UNREAD", correlationStatusEnumId: "CORR_AUTO_MATCH", claimAmount: 5000.0, claimCurrency: "USD"]).setSequencedIdPrimary().create().inboxItemId

        when:
        ec.service.sync().name("trade.swift.InboundActionServices.spawn#Presentation750").parameters([inboxItemId: itemId]).call()

        then:
        def item = ec.entity.find("trade.swift.TradeInboxItem").condition("inboxItemId", itemId).one()
        item.presentationId != null
        def pres = ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("presentationId", item.presentationId).one()
        pres.claimAmount == 5000.0
        pres.isDiscrepant == 'Y'
        pres.sourceChannel == 'INBOUND'
        item.actionTaken == 'SPAWN_PRESENTATION_750'
    }

    def "InboundActionServices.process#Mt754 fast-tracks clean presentation"() {
        given:
        def lcResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: "${testPrefix}-LC-REF-754", lcAmount: 100000, lcCurrencyUomId: "USD", businessStateId: "LC_ISSUED", instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: '_NA_'], [roleEnumId: 'TP_BENEFICIARY', partyId: '_NA_']]]).call()
        def instId = lcResult?.instrumentId
        if (lcResult?.transactionId) ec.service.sync().name("update#trade.TradeTransaction").parameters([transactionId: lcResult.transactionId, transactionStatusId: 'TX_APPROVED']).call()

        def sampleMt754 = "dummy"
        def rawId = ec.entity.makeValue("trade.swift.InboundSwiftRaw").setAll([rawContent: sampleMt754, messageType: "754", parseStatusEnumId: "PARSE_SUCCESS", contentHash: "hash754", swiftHeaderKey: "hdr754", sourceChannel: "SRC_MANUAL_UPLOAD"]).setSequencedIdPrimary().create().rawMessageId
        def itemId = ec.entity.makeValue("trade.swift.TradeInboxItem").setAll([rawMessageId: rawId, instrumentId: instId, messageType: "754", inboxStatusEnumId: "INBOX_UNREAD", correlationStatusEnumId: "CORR_AUTO_MATCH", claimAmount: 8000.0, claimCurrency: "USD"]).setSequencedIdPrimary().create().inboxItemId

        when:
        ec.service.sync().name("trade.swift.InboundActionServices.process#Mt754").parameters([inboxItemId: itemId]).call()

        then:
        def item = ec.entity.find("trade.swift.TradeInboxItem").condition("inboxItemId", itemId).one()
        def pres = ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("presentationId", item.presentationId).one()
        pres.presentationStatusId == 'PRES_COMPLIANT'
        pres.isDiscrepant == 'N'

        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instId).one()
        lc.businessStateId == 'LC_ACCEPTED'
    }

    def "InboundActionServices.reconcile#Mt742 matches reimbursement"() {
        given:
        def lcResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: "${testPrefix}-LC-REF-742", lcAmount: 100000, lcCurrencyUomId: "USD", businessStateId: "LC_ISSUED", instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: '_NA_'], [roleEnumId: 'TP_BENEFICIARY', partyId: '_NA_']]]).call()
        def instId = lcResult?.instrumentId

        def reconResult = ec.service.sync().name("create#trade.importlc.NostroReconciliation")
            .parameters([instrumentId: instId, expectedAmount: 10000, matchStatusEnumId: 'RECON_PENDING']).call()
        def reconId = reconResult.reconciliationId

        def sampleMt742 = "dummy"
        def rawId = ec.entity.makeValue("trade.swift.InboundSwiftRaw").setAll([rawContent: sampleMt742, messageType: "742", parseStatusEnumId: "PARSE_SUCCESS", contentHash: "hash742", swiftHeaderKey: "hdr742", sourceChannel: "SRC_MANUAL_UPLOAD"]).setSequencedIdPrimary().create().rawMessageId
        def itemId = ec.entity.makeValue("trade.swift.TradeInboxItem").setAll([rawMessageId: rawId, instrumentId: instId, messageType: "742", inboxStatusEnumId: "INBOX_UNREAD", correlationStatusEnumId: "CORR_AUTO_MATCH", claimAmount: 9000.0, claimCurrency: "USD"]).setSequencedIdPrimary().create().inboxItemId

        when:
        ec.service.sync().name("trade.swift.InboundActionServices.reconcile#Mt742").parameters([inboxItemId: itemId]).call()

        then:
        def item = ec.entity.find("trade.swift.TradeInboxItem").condition("inboxItemId", itemId).one()
        item.actionTaken == 'REIMBURSEMENT_AUTO_MATCHED'

        def recon = ec.entity.find("trade.importlc.NostroReconciliation").condition("reconciliationId", reconId).one()
        recon.matchStatusEnumId == 'RECON_MATCHED'
        recon.matchedAmount == 9000.0
    }
}
