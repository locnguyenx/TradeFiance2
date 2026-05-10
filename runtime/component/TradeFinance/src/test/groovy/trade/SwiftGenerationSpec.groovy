package trade

import org.moqui.entity.EntityCondition
import org.moqui.context.ExecutionContext
import spock.lang.Specification
import spock.lang.Shared
import org.slf4j.Logger
import org.slf4j.LoggerFactory

// ABOUTME: SwiftGenerationSpec verifies the generation of SWIFT MT messages and their lifecycle.
// ABOUTME: Covers MT700, MT701, MT707, and MT750 with DRAFT/ACTIVE state transitions.

class SwiftGenerationSpec extends Specification {
    @Shared protected ExecutionContext ec
    @Shared String testPrefix

    def setupSpec() {
        ec = org.moqui.Moqui.getExecutionContext()
        ec.artifactExecution.disableAuthz()
        ec.user.loginUser("trade.admin", "trade123")
        testPrefix = "SWG-" + System.currentTimeMillis()
        cleanData()
    }

    def cleanupSpec() {
        try {
            if (ec != null) cleanData()
        } finally {
            if (ec != null) ec.destroy()
        }
    }

    private void cleanData() {
        boolean begun = ec.transaction.begin(60)
        try {
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").updateAll([latestTransactionId: null])
            ec.entity.find("trade.importlc.ImportLcInternalAmendment").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.ImportLcAmendment").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.ImportLcSettlement").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeApprovalRecord").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeTransaction").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.transaction.commit(begun)
        } catch (Exception e) {
            ec.transaction.rollback(begun, "Error in cleanData", e)
        }
    }

    def setup() {
        ec.message.clearAll()
        ec.artifactExecution.disableAuthz()
    }

    def cleanup() {
        ec.message.clearAll()
    }

    // BDD-SWG-LCY-01: Lifecycle — DRAFT Message on Creation
    def "LCY-01: Lifecycle - DRAFT message generated on creation"() {
        given: "A new Import LC that is not yet approved"
        def instrumentId = testPrefix + "_LCY01"
        def ref = instrumentId + "_REF"
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, instrumentRef: ref, lcAmount: 100000.0, lcCurrencyUomId: "USD",
                         lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_SIGHT_PAYMENT',
                         availableWithEnumId: 'AW_ANY_BANK', confirmationEnumId: 'CONF_WITHOUT',
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002'],
                                   [roleEnumId: 'TP_ADVISING_BANK', partyId: 'ADVISING_BANK_001']]]).call()
        assert !ec.message.hasError()

        when: "generate#Mt700 is called"
        def genRes = ec.service.sync().name("trade.SwiftGenerationServices.generate#Mt700")
            .parameters([instrumentId: instrumentId, forceRegenerate: true]).call()

        then: "A SWIFT message is created with DRAFT status"
        !ec.message.hasError()
        def swiftMsg = ec.entity.find("trade.importlc.SwiftMessage").condition("swiftMessageId", genRes.swiftMessageId).one()
        swiftMsg.messageStatusId == "SWIFT_MSG_DRAFT"
        swiftMsg.messageContent.contains("{1:F01")
        swiftMsg.messageContent.contains(":20:" + ref)
    }

    // BDD-SWG-LCY-02: Lifecycle — ACTIVE Message on Approval
    def "LCY-02: Lifecycle - ACTIVE message generated on approval"() {
        given: "An Import LC that is approved"
        def instrumentId = testPrefix + "_LCY02"
        def ref = instrumentId + "_REF"
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, instrumentRef: ref, lcAmount: 100000.0, lcCurrencyUomId: "USD",
                         lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_SIGHT_PAYMENT',
                         availableWithEnumId: 'AW_ANY_BANK', confirmationEnumId: 'CONF_WITHOUT',
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002'],
                                   [roleEnumId: 'TP_ADVISING_BANK', partyId: 'ADVISING_BANK_001']]]).call()
        assert !ec.message.hasError()
        
        // Simulate approval
        def tx = ec.entity.find("trade.TradeTransaction").condition("instrumentId", instrumentId).one()
        ec.service.sync().name("update#trade.TradeTransaction")
            .parameters([transactionId: tx.transactionId, transactionStatusId: "TX_APPROVED"]).call()

        when: "generate#Mt700 is called"
        def genRes = ec.service.sync().name("trade.SwiftGenerationServices.generate#Mt700")
            .parameters([instrumentId: instrumentId, forceRegenerate: true]).call()

        then: "The message is created with ACTIVE status"
        !ec.message.hasError()
        def swiftMsg = ec.entity.find("trade.importlc.SwiftMessage")
                .condition("swiftMessageId", genRes.swiftMessageId).useCache(false).one()
        swiftMsg.messageStatusId == "SWIFT_MSG_ACTIVE"
    }

    // BDD-SWG-LCY-05: Lifecycle — Immutability of ACTIVE Messages
    def "LCY-05: Lifecycle - Immutability of ACTIVE messages"() {
        given: "An active MT700 message"
        def instrumentId = testPrefix + "_LCY05"
        def ref = instrumentId + "_REF"
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, instrumentRef: ref, lcAmount: 100000.0, lcCurrencyUomId: "USD",
                         lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_SIGHT_PAYMENT',
                         availableWithEnumId: 'AW_ANY_BANK', confirmationEnumId: 'CONF_WITHOUT',
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]]).call()
        
        ec.entity.find("trade.TradeTransaction").condition("instrumentId", instrumentId).one()
            .set("transactionStatusId", "TX_APPROVED").update()
        
        def gen1 = ec.service.sync().name("trade.SwiftGenerationServices.generate#Mt700")
            .parameters([instrumentId: instrumentId, forceRegenerate: true]).call()
        assert gen1.swiftMessageId != null

        when: "generate#Mt700 is called again"
        def gen2 = ec.service.sync().name("trade.SwiftGenerationServices.generate#Mt700")
            .parameters([instrumentId: instrumentId, forceRegenerate: true]).call()

        then: "Regeneration is blocked and existing message is returned"
        gen2.swiftMessageId == null || gen2.swiftMessageId == gen1.swiftMessageId
    }

    // BDD-SWG-CON-01: Continuation — MT701 generated for large fields
    def "CON-01: Continuation - MT701 generated for large text"() {
        given: "An LC with very large goods description"
        def largeText = "STEEL PIPES " + ("X" * 7000)
        def instrumentId = testPrefix + "_CON01"
        def ref = instrumentId + "_REF"
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, instrumentRef: ref, lcAmount: 50000.0, lcCurrencyUomId: "USD",
                         goodsDescription: largeText,
                         lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_SIGHT_PAYMENT',
                         availableWithEnumId: 'AW_ANY_BANK', confirmationEnumId: 'CONF_WITHOUT',
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]]).call()

        when: "generate#Mt701 is called"
        def genRes = ec.service.sync().name("trade.SwiftGenerationServices.generate#Mt701")
            .parameters([instrumentId: instrumentId, forceRegenerate: true]).call()

        then: "MT701 content exists with Tag 45B containing overflow"
        !ec.message.hasError()
        genRes.messageContent != null
        genRes.messageContent.contains("{2:I701")
        genRes.messageContent.contains(":45B:")
    }

    // BDD-SWG-AMD-01: Amendment — MT707 generated correctly (SRG 2024)
    def "AMD-01: Amendment - MT707 generated correctly (SRG 2024)"() {
        given: "An Import LC Amendment"
        def instrumentId = testPrefix + "_AMD01"
        def ref = instrumentId + "_REF"
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, instrumentRef: ref, lcAmount: 100000.0, lcCurrencyUomId: "USD",
                         lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_SIGHT_PAYMENT',
                         availableWithEnumId: 'AW_ANY_BANK', confirmationEnumId: 'CONF_WITHOUT',
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]]).call()
        
        // Add a SRG 2024 amendment
        def amRes = ec.service.sync().name("create#trade.importlc.ImportLcAmendment")
            .parameters([instrumentId: instrumentId, amendmentNumber: 1, 
                         amountIncrease: 5000.0, goodsActionEnumId: 'AMA_ADD', goodsDeltaText: "INCREASE AMOUNT"]).call()

        when: "generate#Mt707 is called"
        def genRes = ec.service.sync().name("trade.SwiftGenerationServices.generate#Mt707")
            .parameters([amendmentId: amRes.amendmentId]).call()

        then: "MT707 content exists with Tag 32B and Smart Delta Tag 45B"
        !ec.message.hasError()
        genRes.messageContent.contains("{2:I707")
        genRes.messageContent.contains(":32B:USD5000,00")
        genRes.messageContent.contains(":45B:/ADD/INCREASE AMOUNT")
    }

    def "MT700 contains Tags 40A, 41a, 49"() {
        given: "An LC with new mandatory fields"
        def instrumentId = testPrefix + "_TAGS"
        def ref = instrumentId + "_REF"
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, instrumentRef: ref, lcAmount: 100000.0, lcCurrencyUomId: "USD",
                         lcTypeEnumId: "LCT_IRREVOCABLE",
                         availableByEnumId: "AVB_BY_NEGOTIATION",
                         availableWithEnumId: "AW_ANY_BANK",
                         confirmationEnumId: "CONF_CONFIRMED",
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002'],
                                   [roleEnumId: 'TP_ADVISING_BANK', partyId: 'ADVISING_BANK_001']]]).call()

        when: "generate#Mt700 is called"
        def genRes = ec.service.sync().name("trade.SwiftGenerationServices.generate#Mt700")
            .parameters([instrumentId: instrumentId, forceRegenerate: true]).call()

        then: "Generated content contains 40A, 41D, 49 mappings"
        !ec.message.hasError()
        genRes.messageContent != null
        genRes.messageContent.contains(":40A:IRREVOCABLE")
        genRes.messageContent.contains(":41D:ANY BANK")
        genRes.messageContent.contains("NEGOTIATION")
    }

    def "MT734 contains Tag 32A Value Date"() {
        given: "A presentation for an LC"
        def instrumentId = testPrefix + "_734"
        def ref = instrumentId + "_REF"
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, instrumentRef: ref, lcAmount: 100000.0, lcCurrencyUomId: "USD",
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002'],
                                   [roleEnumId: 'TP_ADVISING_BANK', partyId: 'ADVISING_BANK_001']]]).call()
        
        def presRes = ec.service.sync().name("create#trade.importlc.TradeDocumentPresentation")
            .parameters([instrumentId: instrumentId, claimAmount: 50000.0, claimCurrency: "USD",
                         presentationDate: new java.sql.Date(System.currentTimeMillis())]).call()

        when: "generate#Mt734 is called"
        def genRes = ec.service.sync().name("trade.SwiftGenerationServices.generate#Mt734")
            .parameters([presentationId: presRes.presentationId]).call()

        then: "Generated content contains Tag 32A"
        !ec.message.hasError()
        genRes.messageContent != null
        genRes.messageContent.contains(":32A:")
    }

    def "MT700 includes Tags 49G, 49H, and 40E when populated"() {
        given: "An LC with payment conditions and applicable rules"
        def instrumentId = testPrefix + "_GAPS"
        def ref = instrumentId + "_REF"
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, instrumentRef: ref, lcAmount: 100000.0, lcCurrencyUomId: "USD",
                         lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_SIGHT_PAYMENT',
                         availableWithEnumId: 'AW_ANY_BANK', confirmationEnumId: 'CONF_WITHOUT',
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]]).call()

        ec.entity.find("trade.importlc.ImportLetterOfCredit")
            .condition("instrumentId", instrumentId)
            .updateAll([paymentCondBeneText: "Payment upon receipt of clean BL",
                        paymentCondBankText: "Reimburse via MT 202 only",
                        applicableRulesEnumId: "APR_UCP_LATEST"])

        when: "generate#Mt700 is called"
        def genRes = ec.service.sync().name("trade.SwiftGenerationServices.generate#Mt700")
            .parameters([instrumentId: instrumentId, forceRegenerate: true]).call()

        then: "Generated content contains 49G, 49H, and 40E"
        !ec.message.hasError()
        genRes.messageContent.contains(":49G:Payment upon receipt of clean BL")
        genRes.messageContent.contains(":49H:Reimburse via MT 202 only")
        genRes.messageContent.contains(":40E:UCP LATEST VERSION")
    }

    def "MT707 includes Tag 23S CANCEL when isCancellationRequest is Y"() {
        given: "An amendment with cancellation request"
        def instrumentId = testPrefix + "_CANCEL"
        def ref = instrumentId + "_REF"
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, instrumentRef: ref, lcAmount: 100000.0, lcCurrencyUomId: "USD",
                         lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_SIGHT_PAYMENT',
                         availableWithEnumId: 'AW_ANY_BANK', confirmationEnumId: 'CONF_WITHOUT',
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]]).call()
        
        def amRes = ec.service.sync().name("create#trade.importlc.ImportLcAmendment")
            .parameters([instrumentId: instrumentId, amendmentNumber: 1, 
                         isCancellationRequest: "Y"]).call()

        when: "generate#Mt707 is called"
        def genRes = ec.service.sync().name("trade.SwiftGenerationServices.generate#Mt707")
            .parameters([amendmentId: amRes.amendmentId, forceRegenerate: true]).call()

        then: "MT707 content contains 23S:CANCEL"
        !ec.message.hasError()
        genRes.messageContent.contains(":23S:CANCEL")
    }
}
