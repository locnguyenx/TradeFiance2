package trade

import spock.lang.Specification
import spock.lang.Shared
import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityCondition

/**
 * ABOUTME: SwiftReimbursementSpec verifies the generation of MT740/MT747 reimbursement messages and Nostro reconciliation flow.
 * Covers auto-creation of NostroReconciliation records and financial adjustment triggers.
 */
class SwiftReimbursementSpec extends Specification {
    @Shared ExecutionContext ec
    @Shared String testPrefix
    @Shared String instrumentId

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.user.loginUser("trade.maker", "trade123")
        ec.artifactExecution.disableAuthz()
        testPrefix = "REIMB-" + System.currentTimeMillis()

        // Set isolated ID generation ranges - use 4000000
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrument", 34000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit", 34000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeTransaction", 34000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrumentParty", 34000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.NostroReconciliation", 34000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.SwiftMessage", 34000000, 1000)

        // Create test instrument with reimbursing bank
        def instRes = ec.service.sync().name("create#trade.TradeInstrument").parameters([
            instrumentRef: testPrefix + "-LC-REF",
            instrumentTypeEnumId: "IMPORT_LC", amount: 100000.00,
            currencyUomId: "USD", issueDate: ec.user.nowTimestamp,
            expiryDate: java.sql.Date.valueOf("2026-09-30")
        ]).call()
        instrumentId = instRes.instrumentId

        ec.service.sync().name("create#trade.importlc.ImportLetterOfCredit").parameters([
            instrumentId: instrumentId, businessStateId: "LC_ISSUED",
            tolerancePositive: 0.10, toleranceNegative: 0.10,
            availableWithEnumId: "AW_ANY_BANK",
            authExpiryDate: java.sql.Date.valueOf("2026-10-30"),
            reimbursingChargesEnumId: "RMB_OUR",
            applicableReimbRulesText: "URR LATEST VERSION"
        ]).call()

        // Create reimbursing bank party
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty").parameters([
            partyId: testPrefix + "-RBANK", partyName: "CITIBANK NEW YORK",
            partyTypeEnumId: "PTY_BANK", swiftBic: "CITIUS33",
            nostroAccountRef: "36112345"
        ]).call()
        ec.service.sync().name("create#trade.TradeInstrumentParty").parameters([
            instrumentId: instrumentId, partyId: testPrefix + "-RBANK",
            roleEnumId: "TP_REIMBURSING_BANK"
        ]).call()

        // Create advising bank, beneficiary (required for MT 740 tags)
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty").parameters([
            partyId: testPrefix + "-ADVBANK", partyName: "HSBC HONG KONG",
            partyTypeEnumId: "PTY_BANK", swiftBic: "HSBCHKHH"
        ]).call()
        ec.service.sync().name("create#trade.TradeInstrumentParty").parameters([
            instrumentId: instrumentId, partyId: testPrefix + "-ADVBANK",
            roleEnumId: "TP_ADVISING_BANK"
        ]).call()

        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty").parameters([
            partyId: testPrefix + "-BEN", partyName: "ACME EXPORTS LTD",
            partyTypeEnumId: "PTY_COMMERCIAL"
        ]).call()
        ec.service.sync().name("create#trade.TradeInstrumentParty").parameters([
            instrumentId: instrumentId, partyId: testPrefix + "-BEN",
            roleEnumId: "TP_BENEFICIARY"
        ]).call()
    }

    def cleanupSpec() {
        if (ec != null) {
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrument")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeTransaction")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrumentParty")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.NostroReconciliation")
            ec.entity.tempResetSequencedIdPrimary("trade.SwiftMessage")
            ec.destroy()
        }
    }

    def "MT740 generated with correct tags when reimbursing bank assigned"() {
        when:
        def result = ec.service.sync().name("trade.SwiftGenerationServices.generate#Mt740")
            .parameters([instrumentId: instrumentId]).call()

        then:
        result.messageContent != null
        result.messageContent.contains("CITIUS33")  // Receiver BIC
        result.messageContent.contains("36112345")  // Tag 25: Nostro account
        result.messageContent.contains("USD100000,00")  // Tag 32B
        result.messageContent.contains("ANY BANK")  // Tag 58a
        result.messageContent.contains("URR LATEST VERSION")  // Tag 40F
        result.swiftMessageId != null
    }

    def "NostroReconciliation record created when MT740 generated"() {
        when:
        ec.message.clearAll()
        def res = ec.service.sync().name("trade.SwiftGenerationServices.generate#Mt740")
            .parameters([instrumentId: instrumentId]).call()
        
        def reconList = ec.entity.find("trade.importlc.NostroReconciliation")
            .condition("instrumentId", instrumentId).list()

        then:
        !ec.message.hasError()
        reconList.size() >= 1
        reconList[0].matchStatusEnumId == "RECON_PENDING"
        reconList[0].expectedAmount == 100000.00
    }
}
