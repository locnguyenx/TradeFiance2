package trade

import spock.lang.*
import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import java.sql.Date
import java.sql.Timestamp
import org.moqui.entity.EntityCondition

/**
 * ABOUTME: SwiftComplianceSpec consolidates all SWIFT generation, field validation, and compliance hold scenarios.
 * Covers MT700/701/707/734/740/747 generation, character set validation (X/Z), and SECA-based auto-triggers.
 */
class SwiftComplianceSpec extends Specification {
    @Shared protected ExecutionContext ec
    @Shared String testPrefix
    @Shared String applicantId
    @Shared String beneficiaryId
    @Shared String advisingBankId
    @Shared String reimbursingBankId

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.transaction.runUseOrBegin(null, null) {
            ec.artifactExecution.disableAuthz()
            ec.user.loginUser("trade.admin", "trade123")
            testPrefix = "SWC-" + System.currentTimeMillis()

            // Set isolated ID generation ranges - use 96000000 (Module 4)
            ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrument", 96000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit", 96000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.TradeTransaction", 96000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrumentParty", 96000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.importlc.SwiftMessage", 96000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLcAmendment", 96000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.importlc.NostroReconciliation", 96000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.importlc.TradeDocumentPresentation", 96000000, 1000)

            applicantId = testPrefix + "_APP"
            beneficiaryId = testPrefix + "_BEN"
            advisingBankId = testPrefix + "_ADVB"
            reimbursingBankId = testPrefix + "_REIMB"

            // Setup parties
            ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: applicantId, partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'SWC Applicant', kycStatus: 'KYC_ACTIVE']).call()
            ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: beneficiaryId, partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'SWC Beneficiary', kycStatus: 'KYC_ACTIVE']).call()
            ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: advisingBankId, partyTypeEnumId: 'PTY_BANK', partyName: 'SWC Adv Bank', swiftBic: 'SWCADVXX', hasActiveRMA: true, kycStatus: 'KYC_ACTIVE']).call()
            ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: reimbursingBankId, partyTypeEnumId: 'PTY_BANK', partyName: 'SWC Reimb Bank', swiftBic: 'SWCREIXX', kycStatus: 'KYC_ACTIVE']).call()
        }
    }

    def cleanupSpec() {
        if (ec != null) {
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrument")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeTransaction")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrumentParty")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.SwiftMessage")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLcAmendment")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.NostroReconciliation")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.TradeDocumentPresentation")
            ec.destroy()
        }
    }

    def setup() {
        if (ec.transaction.isTransactionInPlace()) ec.transaction.rollback("Cleanup", null)
        ec.user.loginUser("trade.admin", "trade123")
        ec.message.clearAll()
        ec.artifactExecution.disableAuthz()
    }

    private String createIssuedLc(BigDecimal amount = 50000.0) {
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: testPrefix + "_REF_" + System.nanoTime(), lcAmount: amount, lcCurrencyUomId: "USD",
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId],
                                             [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId],
                                             [roleEnumId: 'TP_ADVISING_BANK', partyId: advisingBankId]],
                         lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_SIGHT_PAYMENT', 
                         availableWithEnumId: 'AW_ANY_BANK', confirmationEnumId: 'CONF_WITHOUT']).call()
        String instrumentId = res.instrumentId
        
        def tx = ec.entity.find("trade.TradeTransaction").condition([instrumentId: instrumentId, transactionTypeEnumId: 'IMP_NEW']).one()
        ec.user.loginUser("trade.checker", "trade123")
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument")
            .parameters([transactionId: tx.transactionId, skipFourEyes: true]).call()
        ec.user.loginUser("trade.admin", "trade123")
        
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).updateAll([businessStateId: "LC_ISSUED"])
        return instrumentId
    }

    // --- CHARACTER SET VALIDATION ---

    def "should block invalid X charset characters"() {
        given: "An LC with invalid X charset characters (@, #)"
        def instrumentId = createIssuedLc()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId)
            .updateAll([goodsDescription: "Steel Pipes @100mm", portOfLoading: "Port #1"])

        when: "validate#SwiftFields is called"
        def result = ec.service.sync().name("trade.importlc.ImportLcValidationServices.validate#SwiftFields")
            .parameters([entityType: "ImportLetterOfCredit", entityId: instrumentId]).call()

        then: "Errors are returned"
        result.errors != null
        result.errors.any { it.fieldName == "goodsDescription" && it.message.contains("@") }
        result.errors.any { it.fieldName == "portOfLoading" && it.message.contains("#") }
    }

    def "should block invalid Z charset characters in Amendment Narrative"() {
        given: "An amendment with invalid Z charset character (^)"
        def instrumentId = createIssuedLc()
        def amdRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#Amendment")
            .parameters([instrumentId: instrumentId, amendmentNarrative: "Valid Z: @#=!\"%_\nInvalid: ^",
                         amendmentTypeEnumId: "AMEND_OTHER", amendmentDate: ec.user.nowTimestamp]).call()
        assert amdRes?.amendmentId != null : "Amendment creation failed: ${ec.message.errorsString}"

        when: "validate#SwiftFields is called on the Amendment"
        def result = ec.service.sync().name("trade.importlc.ImportLcValidationServices.validate#SwiftFields")
            .parameters([entityType: "ImportLcAmendment", entityId: amdRes.amendmentId]).call()
        
        if (result.errors == null || !result.errors.any { it.fieldName == "amendmentNarrative" && it.message.contains("^") }) {
            println "DEBUG: Amendment ID: ${amdRes.amendmentId}"
            println "DEBUG: Result Errors: ${result.errors}"
            def amdEntity = ec.entity.find("trade.importlc.ImportLcAmendment").condition("amendmentId", amdRes.amendmentId).one()
            println "DEBUG: Amendment Entity conditionsDeltaText: ${amdEntity?.conditionsDeltaText}"
        }

        then: "Error for ^ but not for valid Z chars"
        result.errors != null
        result.errors.any { it.fieldName == "amendmentNarrative" && it.message.contains("^") }
        !result.errors.any { it.message.contains("@") }
    }

    // --- SWIFT GENERATION ---

    def "should generate MT700 with DRAFT/ACTIVE status transitions"() {
        given: "A new LC draft"
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: testPrefix + "_LIFECYCLE", lcAmount: 100000.0, lcCurrencyUomId: "USD",
                         lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_SIGHT_PAYMENT',
                         availableWithEnumId: 'AW_ANY_BANK', confirmationEnumId: 'CONF_WITHOUT',
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId],
                                             [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]]]).call()
        def instrumentId = res.instrumentId

        when: "Generating MT700 for draft"
        def genDraft = ec.service.sync().name("trade.SwiftGenerationServices.generate#Mt700")
            .parameters([instrumentId: instrumentId, forceRegenerate: true]).call()

        then: "Status is SWIFT_MSG_DRAFT"
        def swiftDraft = ec.entity.find("trade.importlc.SwiftMessage").condition("swiftMessageId", genDraft.swiftMessageId).one()
        swiftDraft.messageStatusId == "SWIFT_MSG_DRAFT"

        when: "Approving LC and regenerating"
        def tx = ec.entity.find("trade.TradeTransaction").condition("instrumentId", instrumentId).one()
        ec.service.sync().name("update#trade.TradeTransaction")
            .parameters([transactionId: tx.transactionId, transactionStatusId: "TX_APPROVED"]).call()
        def genActive = ec.service.sync().name("trade.SwiftGenerationServices.generate#Mt700")
            .parameters([instrumentId: instrumentId, forceRegenerate: true]).call()

        then: "Status is SWIFT_MSG_ACTIVE"
        def swiftActive = ec.entity.find("trade.importlc.SwiftMessage").condition("swiftMessageId", genActive.swiftMessageId).one()
        swiftActive.messageStatusId == "SWIFT_MSG_ACTIVE"
    }

    def "should generate MT701 for large narratives"() {
        given: "An LC with large goods description"
        def largeText = "PIPES " + ("X" * 7000)
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: testPrefix + "_MT701", lcAmount: 50000.0, lcCurrencyUomId: "USD",
                         goodsDescription: largeText,
                         lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_SIGHT_PAYMENT',
                         availableWithEnumId: 'AW_ANY_BANK', confirmationEnumId: 'CONF_WITHOUT',
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId],
                                             [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]]]).call()

        when: "Generating MT701"
        def genRes = ec.service.sync().name("trade.SwiftGenerationServices.generate#Mt701")
            .parameters([instrumentId: res.instrumentId, forceRegenerate: true]).call()

        then: "MT701 generated with Tag 45B"
        genRes.messageContent.contains("{2:I701")
        genRes.messageContent.contains(":45B:")
    }

    def "should generate MT707 for amendments with Smart Delta"() {
        given: "An amendment with amount increase"
        def instrumentId = createIssuedLc()
        def amRes = ec.service.sync().name("create#trade.importlc.ImportLcAmendment")
            .parameters([instrumentId: instrumentId, amendmentNumber: 1, 
                         amountIncrease: 5000.0, goodsActionEnumId: 'AMA_ADD', goodsDeltaText: "NEW GOODS"]).call()

        when: "Generating MT707"
        def genRes = ec.service.sync().name("trade.SwiftGenerationServices.generate#Mt707")
            .parameters([amendmentId: amRes.amendmentId]).call()

        then: "MT707 generated with Tag 32B and Delta Tag 45B"
        genRes.messageContent.contains("{2:I707")
        genRes.messageContent.contains(":32B:USD5000,00")
        genRes.messageContent.contains(":45B:/ADD/NEW GOODS")
    }

    def "should generate MT740 and NostroReconciliation for reimbursing bank"() {
        given: "An LC with a reimbursing bank"
        def instrumentId = createIssuedLc()
        ec.service.sync().name("create#trade.TradeInstrumentParty").parameters([
            instrumentId: instrumentId, partyId: reimbursingBankId, roleEnumId: "TP_REIMBURSING_BANK"
        ]).call()

        when: "Generating MT740"
        def result = ec.service.sync().name("trade.SwiftGenerationServices.generate#Mt740")
            .parameters([instrumentId: instrumentId]).call()
        
        then: "MT740 and NostroReconciliation record created"
        result.messageContent.contains("SWCREIXX")
        def recon = ec.entity.find("trade.importlc.NostroReconciliation").condition("instrumentId", instrumentId).one()
        recon != null
        recon.matchStatusEnumId == "RECON_PENDING"
    }

    def "should generate MT734 with Tag 32A Value Date"() {
        given: "A presentation for an LC"
        def instrumentId = createIssuedLc()
        def presRes = ec.service.sync().name("create#trade.importlc.TradeDocumentPresentation")
            .parameters([instrumentId: instrumentId, claimAmount: 50000.0, claimCurrency: "USD",
                         presentationDate: new java.sql.Date(System.currentTimeMillis())]).call()

        when: "Generating MT734"
        def genRes = ec.service.sync().name("trade.SwiftGenerationServices.generate#Mt734")
            .parameters([presentationId: presRes.presentationId]).call()

        then: "MT734 contains Tag 32A"
        genRes.messageContent.contains(":32A:")
    }

    def "should include Tags 49G, 49H, and 40E in MT700 when populated"() {
        given: "An LC with payment conditions and applicable rules"
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: testPrefix + "_TAGS_" + System.nanoTime(), lcAmount: 10000.0, lcCurrencyUomId: 'USD',
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: testPrefix + '_APP'],
                                             [roleEnumId: 'TP_BENEFICIARY', partyId: testPrefix + '_BEN']]]).call()
        def instrumentId = res.instrumentId

        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, 
                         paymentCondBeneText: "Payment upon receipt of clean BL",
                         paymentCondBankText: "Reimburse via MT 202 only",
                         applicableRulesEnumId: "APR_UCP_LATEST"]).call()

        when: "Generating MT700"
        def genRes = ec.service.sync().name("trade.SwiftGenerationServices.generate#Mt700")
            .parameters([instrumentId: instrumentId]).call()

        then: "MT700 contains 49G, 49H, and 40E"
        genRes.messageContent != null
        genRes.messageContent.contains(":49G:Payment upon receipt of clean BL")
        genRes.messageContent.contains(":49H:Reimburse via MT 202 only")
        genRes.messageContent.contains(":40E:UCP LATEST VERSION")
    }

    def "should include Tag 23S CANCEL in MT707 for cancellation requests"() {
        given: "An amendment with cancellation request"
        def instrumentId = createIssuedLc()
        def amRes = ec.service.sync().name("create#trade.importlc.ImportLcAmendment")
            .parameters([instrumentId: instrumentId, amendmentNumber: 1, isCancellationRequest: "Y"]).call()

        when: "Generating MT707"
        def genRes = ec.service.sync().name("trade.SwiftGenerationServices.generate#Mt707")
            .parameters([amendmentId: amRes.amendmentId]).call()

        then: "MT707 contains 23S:CANCEL"
        genRes.messageContent != null
        genRes.messageContent.contains(":23S:CANCEL")
    }

    def "should generate MT700 with junction-based party data (:50, :59, 41A)"() {
        given: "A draft LC with junction parties"
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: testPrefix + "_JUNC_" + System.nanoTime(), lcAmount: 10000.0, lcCurrencyUomId: 'USD',
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: testPrefix + '_APP'],
                                             [roleEnumId: 'TP_BENEFICIARY', partyId: testPrefix + '_BEN']]]).call()
        def instrumentId = res.instrumentId
        
        // Negotiating Bank for 41A
        def negBankId = testPrefix + "_NEG"
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
            .parameters([partyId: negBankId, partyTypeEnumId: 'PTY_BANK', partyName: 'Neg Bank', swiftBic: 'NEGOTXXX', hasActiveRMA: true, kycStatus: 'KYC_ACTIVE']).call()
        ec.service.sync().name("trade.TradeCommonServices.assign#InstrumentParty")
            .parameters([instrumentId: instrumentId, roleEnumId: 'TP_NEGOTIATING_BANK', partyId: negBankId]).call()
        
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId)
            .updateAll([availableWithEnumId: 'AW_SPECIFIC_BANK', availableByEnumId: 'AVB_BY_NEGOTIATION'])

        when: "Generating MT700"
        def genRes = ec.service.sync().name("trade.SwiftGenerationServices.generate#Mt700")
            .parameters([instrumentId: instrumentId]).call()

        then: "SWIFT tags contain data from junction parties"
        genRes.messageContent != null
        def content = genRes.messageContent.replaceAll("\\s+", " ").toUpperCase()
        content.contains(":50:SWC APPLICANT")
        content.contains(":59:SWC BENEFICIARY")
        content.contains("NEGOTXXX") // Tag 41A
    }

    // --- SWIFT VALIDATION GAPS ---

    def "should enforce mandatory SWIFT fields (40A, 41a, 49)"() {
        given: "An LC missing mandatory SWIFT fields"
        def instrumentId = createIssuedLc()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId)
            .updateAll([lcTypeEnumId: null, availableByEnumId: null, confirmationEnumId: null])

        when: "validate#SwiftFields is called"
        def result = ec.service.sync().name("trade.importlc.ImportLcValidationServices.validate#SwiftFields")
            .parameters([entityType: "ImportLetterOfCredit", entityId: instrumentId]).call()

        then: "Errors for 40A, 41a, 49"
        result.errors != null
        result.errors.any { it.fieldName == "lcTypeEnumId" && it.message.contains("Required") }
        result.errors.any { it.fieldName == "availableByEnumId" && it.message.contains("Required") }
        result.errors.any { it.fieldName == "confirmationEnumId" && it.message.contains("Required") }
    }

    def "should block mutually exclusive shipment period and latest shipment date"() {
        given: "An LC with both shipmentPeriodText and latestShipmentDate"
        def instrumentId = createIssuedLc()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId)
            .updateAll([latestShipmentDate: ec.user.nowTimestamp, shipmentPeriodText: "DURING DECEMBER"])

        when: "validate#SwiftFields is called"
        def result = ec.service.sync().name("trade.importlc.ImportLcValidationServices.validate#SwiftFields")
            .parameters([entityType: "ImportLetterOfCredit", entityId: instrumentId]).call()

        then: "Error for mutual exclusion"
        result.errors.any { it.message.contains("mutually exclusive") }
    }

    def "should enforce 6-line limit for Tag 73 and 72Z"() {
        given: "A presentation with 7 lines in chargesDeducted"
        def instrumentId = createIssuedLc()
        def presRes = ec.service.sync().name("trade.TradeCommonServices.create#Presentation")
            .parameters([instrumentId: instrumentId, claimAmount: 1000]).call()
        def longText = "1\n2\n3\n4\n5\n6\n7"
        ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("presentationId", presRes.presentationId)
            .updateAll([chargesDeducted: longText, senderToReceiverPresentationInfo: longText])

        when: "validate#SwiftFields is called"
        def result = ec.service.sync().name("trade.importlc.ImportLcValidationServices.validate#SwiftFields")
            .parameters([entityType: "TradeDocumentPresentation", entityId: presRes.presentationId]).call()

        then: "Error for exceeding 6 lines"
        result.errors.any { it.fieldName == "chargesDeducted" && it.message.contains("6 lines") }
        result.errors.any { it.fieldName == "senderToReceiverPresentationInfo" && it.message.contains("6 lines") }
    }

    // --- AUTO-TRIGGERS (SECAs) ---

    def "should auto-generate MT700 on LC Authorization"() {
        given: "A new Import LC draft"
        def instrumentId = testPrefix + "_AUTO_700"
        def createRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, instrumentRef: instrumentId + "_REF", lcAmount: 50000.0, lcCurrencyUomId: "USD",
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId],
                                             [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId],
                                             [roleEnumId: 'TP_ADVISING_BANK', partyId: advisingBankId]],
                         lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_NEGOTIATION', confirmationEnumId: 'CONF_WITHOUT']).call()
        
        when: "The transaction is authorized"
        ec.user.loginUser("trade.checker", "trade123")
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument")
            .parameters([transactionId: createRes.transactionId, skipFourEyes: true]).call()

        then: "The authorization should succeed"
        !ec.message.hasError()
        
        when: "Verifying generated message (with retry)"
        ec.artifactExecution.disableAuthz()
        def swiftMsg = null
        long start = System.currentTimeMillis()
        while (swiftMsg == null && (System.currentTimeMillis() - start) < 5000) {
            swiftMsg = ec.entity.find("trade.importlc.SwiftMessage")
                .condition([instrumentId: instrumentId, messageType: 'MT700']).one()
            if (swiftMsg == null) Thread.sleep(200)
        }
        
        then: "The message should exist and be ACTIVE"
        swiftMsg != null
        swiftMsg.messageStatusId == 'SWIFT_MSG_ACTIVE'
    }

    def "should auto-generate MT707 on Amendment Authorization"() {
        given: "An issued LC"
        def instrumentId = createIssuedLc()
        
        when: "An amendment is created and authorized"
        def amRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#ExternalAmendment")
            .parameters([instrumentId: instrumentId, amendmentTypeEnumId: 'AMEND_INCREASE', amountIncrease: 1000.0, 
                         goodsActionEnumId: 'AMA_ADD', goodsDeltaText: "AUTO TRIGGER", amendmentDate: ec.user.nowTimestamp]).call()
        ec.user.loginUser("trade.checker", "trade123")
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument")
            .parameters([transactionId: amRes.transactionId, skipFourEyes: true]).call()

        then: "MT707 is generated automatically"
        ec.user.loginUser("trade.admin", "trade123")
        def swiftMsg = ec.entity.find("trade.importlc.SwiftMessage")
            .condition([instrumentId: instrumentId, messageType: 'MT707']).one()
        swiftMsg != null
        swiftMsg.messageContent.contains("AUTO TRIGGER")
    }

    // --- COMPLIANCE HOLD ---

    def "should block transitions while on Compliance Hold"() {
        given: "An issued LC"
        def instrumentId = createIssuedLc()

        when: "Applying Compliance Hold"
        ec.service.sync().name("trade.importlc.ImportLcServices.hold#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId]).call()
        
        then: "Status is LC_HOLD"
        def inst = ec.entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId).one()
        inst.businessStateId == "LC_HOLD"

        when: "Attempting transition"
        ec.service.sync().name("trade.importlc.ImportLcValidationServices.validate#BusinessStateTransition")
            .parameters([instrumentId: instrumentId, toStateId: 'LC_DOC_RECEIVED']).call()

        then: "Transition is blocked"
        ec.message.hasError()
        ec.message.errors.any { it.contains("Compliance Hold") }

        when: "Releasing hold"
        ec.message.clearAll()
        ec.service.sync().name("trade.importlc.ImportLcServices.release#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId]).call()

        then: "Status is restored to LC_ISSUED"
        def instRestored = ec.entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId).one()
        instRestored.businessStateId == "LC_ISSUED"
    }
}
