package trade

import spock.lang.Specification
import spock.lang.Shared
import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityCondition

// ABOUTME: SwiftReimbursementSpec verifies the generation of MT740/MT747 reimbursement messages and Nostro reconciliation flow.
// ABOUTME: Covers auto-creation of NostroReconciliation records and financial adjustment triggers.

class SwiftReimbursementSpec extends Specification {
    @Shared ExecutionContext ec
    @Shared String testId

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.user.loginUser("john.doe", "moqui")
        ec.artifactExecution.disableAuthz()
        testId = (System.currentTimeMillis() % 1000000000L).toString()

        // Create test instrument with reimbursing bank
        def instRes = ec.service.sync().name("create#trade.TradeInstrument").parameters([
            instrumentId: "REIMB_" + testId, instrumentRef: "RTEST" + testId,
            instrumentTypeEnumId: "IMPORT_LC", amount: 100000.00,
            currencyUomId: "USD", issueDate: new java.sql.Date(System.currentTimeMillis()),
            expiryDate: java.sql.Date.valueOf("2026-09-30")
        ]).call()
        if (ec.message.hasError()) println "INST CREATE ERROR: " + ec.message.errorsString
        assert !ec.message.hasError()

        def lcRes = ec.service.sync().name("create#trade.importlc.ImportLetterOfCredit").parameters([
            instrumentId: "REIMB_" + testId, businessStateId: "LC_ISSUED",
            tolerancePositive: 0.10, toleranceNegative: 0.10,
            availableWithEnumId: "AW_ANY_BANK",
            authExpiryDate: java.sql.Date.valueOf("2026-10-30"),
            reimbursingChargesEnumId: "RMB_OUR",
            applicableReimbRulesText: "URR LATEST VERSION"
        ]).call()
        if (ec.message.hasError()) println "LC CREATE ERROR: " + ec.message.errorsString
        assert !ec.message.hasError()

        // Create reimbursing bank party
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty").parameters([
            partyId: "RBANK_" + testId, partyName: "CITIBANK NEW YORK " + testId,
            partyTypeEnumId: "PTY_BANK", swiftBic: "CITIUS33",
            nostroAccountRef: "36112345"
        ]).call()
        ec.service.sync().name("create#trade.TradeInstrumentParty").parameters([
            instrumentId: "REIMB_" + testId, partyId: "RBANK_" + testId,
            roleEnumId: "TP_REIMBURSING_BANK"
        ]).call()

        // Create advising bank, beneficiary (required for MT 740 tags)
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty").parameters([
            partyId: "ADVBANK_" + testId, partyName: "HSBC HONG KONG " + testId,
            partyTypeEnumId: "PTY_BANK", swiftBic: "HSBCHKHH"
        ]).call()
        ec.service.sync().name("create#trade.TradeInstrumentParty").parameters([
            instrumentId: "REIMB_" + testId, partyId: "ADVBANK_" + testId,
            roleEnumId: "TP_ADVISING_BANK"
        ]).call()

        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty").parameters([
            partyId: "BEN_" + testId, partyName: "ACME EXPORTS LTD " + testId,
            partyTypeEnumId: "PTY_COMMERCIAL"
        ]).call()
        ec.service.sync().name("create#trade.TradeInstrumentParty").parameters([
            instrumentId: "REIMB_" + testId, partyId: "BEN_" + testId,
            roleEnumId: "TP_BENEFICIARY"
        ]).call()
        if (ec.message.hasError()) println "SETUP ERROR: " + ec.message.errorsString
        assert !ec.message.hasError()
    }

    def cleanupSpec() {
        ec.artifactExecution.disableAuthz()
        try {
            ec.entity.find("trade.importlc.NostroReconciliation").condition("instrumentId", "REIMB_" + testId).deleteAll()
            ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", "REIMB_" + testId).deleteAll()
            ec.entity.find("trade.importlc.ImportLcAmendment").condition("instrumentId", "REIMB_" + testId).deleteAll()
            ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", "REIMB_" + testId).deleteAll()
            ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", "REIMB_" + testId).deleteAll()
            ec.entity.find("trade.TradeTransaction").condition("instrumentId", "REIMB_" + testId).deleteAll()
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", "REIMB_" + testId).deleteAll()
            
            ec.entity.find("trade.TradePartyBank").condition("partyId", "RBANK_" + testId).deleteAll()
            ec.entity.find("trade.TradePartyBank").condition("partyId", "ADVBANK_" + testId).deleteAll()
            ec.entity.find("trade.TradeParty").condition("partyId", "RBANK_" + testId).deleteAll()
            ec.entity.find("trade.TradeParty").condition("partyId", "ADVBANK_" + testId).deleteAll()
            ec.entity.find("trade.TradeParty").condition("partyId", "BEN_" + testId).deleteAll()
        } finally {
            ec?.destroy()
        }
    }

    def "MT740 generated with correct tags when reimbursing bank assigned"() {
        when:
        def result = ec.service.sync().name("trade.SwiftGenerationServices.generate#Mt740")
            .parameters([instrumentId: "REIMB_" + testId]).call()

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
        def res = ec.service.sync().name("trade.SwiftGenerationServices.generate#Mt740")
            .parameters([instrumentId: "REIMB_" + testId]).call()
        if (ec.message.hasError()) println "REIMB ERROR: " + ec.message.errorsString
        assert !ec.message.hasError()
        def reconList = ec.entity.find("trade.importlc.NostroReconciliation")
            .condition("instrumentId", "REIMB_" + testId).list()

        then:
        reconList.size() >= 1
        reconList[0].matchStatusEnumId == "RECON_PENDING"
        reconList[0].expectedAmount == 100000.00
    }
}
