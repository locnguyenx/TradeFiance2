package trade

import org.moqui.entity.EntityCondition
import org.moqui.context.ExecutionContext
import spock.lang.*
import org.moqui.Moqui

// ABOUTME: SwiftValidationSpec tests Layer 1 SWIFT field validation.
// ABOUTME: Covers character sets, slash rules, BIC, line format, mutual exclusion, and conditional rules.

class SwiftValidationSpec extends Specification {
    @Shared protected ExecutionContext ec
    @Shared String testPrefix
    
    @Shared String applicantId
    @Shared String beneficiaryId
    @Shared String advisingBankId

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.transaction.runUseOrBegin(null, null) {
            println "DEBUG: setupSpec SwiftValidationSpec starting"
            ec.user.loginUser("trade.maker", "trade123")
        println "DEBUG: setupSpec logged in"
        ec.artifactExecution.disableAuthz()
        testPrefix = "SWV-SPEC-" + System.currentTimeMillis()
        println "DEBUG: testPrefix: " + testPrefix
        
        applicantId = testPrefix + "-APP"
        beneficiaryId = testPrefix + "-BEN"
        advisingBankId = testPrefix + "-ADV-BANK"

        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
            .parameters([partyId: applicantId, partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'Acme Corporation Ltd', kycStatus: 'KYC_ACTIVE']).call()
        println "DEBUG: setupSpec created applicant"
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
            .parameters([partyId: beneficiaryId, partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'Global Exports Inc', kycStatus: 'KYC_ACTIVE']).call()
        println "DEBUG: setupSpec created beneficiary"
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
            .parameters([partyId: advisingBankId, partyTypeEnumId: 'PTY_BANK', partyName: 'Overseas Banking Corp', 
                         swiftBic: 'OBCSGSGX', hasActiveRMA: true, kycStatus: 'KYC_ACTIVE']).call()
        println "DEBUG: setupSpec created advising bank"

        // Set isolated ID generation ranges - use 7000000 to avoid conflicts with 9000000
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrument", 31000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit", 31000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeTransaction", 31000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrumentParty", 31000000, 1000)
        }
        println "DEBUG: setupSpec SwiftValidationSpec complete"
    }

    def cleanupSpec() {
        if (ec != null) {
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrument")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit")
            ec.destroy()
        }
    }

    def setup() {
        ec.message.clearAll()
        ec.user.loginUser("trade.admin", "trade123")
        ec.artifactExecution.disableAuthz()
    }

    def cleanup() {
        ec.message.clearAll()
    }

    // BDD-SWV-XCS-01: X Character Set — Invalid Characters Blocked
    def "XCS-01: X charset - invalid characters blocked"() {
        given: "An LC with invalid X charset characters"
        def ref = "TF-XCS-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD",
                         goodsDescription: "STEEL PIPES @100MM & FITTINGS",
                         portOfLoading: "HO CHI MINH CITY #1 PORT",
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]],
                         availableWithEnumId: 'AW_ANY_BANK']).call()
        assert res.instrumentId != null

        when: "validate#SwiftFields is called"
        def result = ec.service.sync().name("trade.importlc.ImportLcValidationServices.validate#SwiftFields")
            .parameters([entityType: "ImportLetterOfCredit", entityId: res.instrumentId]).call()

        then: "Errors are returned for invalid characters"
        result.errors != null
        result.errors.size() >= 2
        result.errors.any { it.fieldName == "goodsDescription" && it.message.contains("@") }
        result.errors.any { it.fieldName == "portOfLoading" && it.message.contains("#") }
    }

    // BDD-SWV-ZCS-01: Z Character Set — Valid Characters vs Invalid Characters
    def "BDD-IMP-VAL-05: Z charset validation for Amendment Narrative"() {
        given: "An amendment with both valid and invalid Z charset characters"
        def ref = "TF-AMD-" + System.currentTimeMillis()
        def lcRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD",
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]],
                         availableWithEnumId: 'AW_ANY_BANK']).call()
        
        // Approve issuance to allow amendment
        def txIss = ec.entity.find("trade.TradeTransaction")
                .condition([instrumentId: lcRes.instrumentId.toString(), transactionTypeEnumId: 'IMP_NEW']).disableAuthz().one()
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument")
                .parameters([transactionId: txIss.transactionId, skipFourEyes: true]).call()
        ec.artifactExecution.disableAuthz()
        ec.entity.find("trade.importlc.ImportLetterOfCredit")
                .condition("instrumentId", lcRes.instrumentId).updateAll([businessStateId: "LC_ISSUED"])
        
        def amdRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#Amendment")
            .parameters([instrumentId: lcRes.instrumentId, amendmentNarrative: "Valid Z: @#=!\"%_\nInvalid: ^",
                         amendmentTypeEnumId: "AMEND_OTHER", amendmentDate: ec.user.nowTimestamp]).call()
        assert amdRes?.amendmentId != null

        when: "validate#SwiftFields is called on the Amendment"
        def result = ec.service.sync().name("trade.importlc.ImportLcValidationServices.validate#SwiftFields")
            .parameters([entityType: "ImportLcAmendment", entityId: amdRes.amendmentId]).call()

        then: "Error is returned for the invalid character ^ and not for valid Z chars"
        result.errors != null
        result.errors.any { it.fieldName == "amendmentNarrative" && it.message.contains("^") }
        !result.errors.any { it.message.contains("@") }
        !result.errors.any { it.message.contains("\"") }
        !result.errors.any { it.message.contains("%") }
        !result.errors.any { it.message.contains("_") }
    }

    // BDD-SWV-REF-02: Reference field length check
    def "REF-02: Reference length exceeds 16 chars"() {
        given: "A TradeInstrument with a very long reference"
        def longRef = "TF-REF-TOO-LONG-REFERENCE-12345"
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: longRef, lcAmount: 1000.0, lcCurrencyUomId: "USD",
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]],
                         availableWithEnumId: 'AW_ANY_BANK']).call()

        when: "validate#SwiftFields is called"
        def result = ec.service.sync().name("trade.importlc.ImportLcValidationServices.validate#SwiftFields")
            .parameters([entityType: "TradeInstrument", entityId: res.instrumentId]).call()

        then: "Error for length"
        result.errors != null
        result.errors.any { it.fieldName == "instrumentRef" && it.message.contains("16-character maximum") }
    }

    // BDD-SWV-BIC-02: Valid BIC for Advising Bank
    def "BIC-02: Advising Bank BIC - invalid length"() {
        given: "An LC with invalid BIC length"
        def ref = "TF-BIC2-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD",
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId],
                                   [roleEnumId: 'TP_ADVISING_BANK', partyId: advisingBankId]]]).call()
        // Override BIC to test length validation on the extension
        ec.entity.find("trade.TradePartyBank").condition("partyId", advisingBankId).one().set("swiftBic", "BAN1").update()

        when: "validate#SwiftFields is called"
        def result = ec.service.sync().name("trade.importlc.ImportLcValidationServices.validate#SwiftFields")
            .parameters([entityType: "ImportLetterOfCredit", entityId: res.instrumentId]).call()

        then: "Error for BIC length"
        result.errors != null
        result.errors.any { it.fieldName == "advisingBankBic" && it.message.contains("8 or 11") }

        cleanup: "Revert the BIC back to original"
        ec.entity.find("trade.TradePartyBank").condition("partyId", advisingBankId).one().set("swiftBic", "OBCSGSGX").update()
    }

    // BDD-SWV-LIN-02: Multiline format violation for Name field
    def "LIN-02: Name field line format violation"() {
        given: "An LC with a Name field exceeding 4 lines"
        def longName = "LINE 1\nLINE 2\nLINE 3\nLINE 4\nLINE 5"
        def ref = "TF-LIN2-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD",
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]],
                         availableWithEnumId: 'AW_ANY_BANK']).call()
        // Update applicant name on TradeParty entity for line count test
        ec.entity.find("trade.TradeParty").condition("partyId", applicantId).one().set("partyName", longName).update()

        when: "validate#SwiftFields is called"
        def result = ec.service.sync().name("trade.importlc.ImportLcValidationServices.validate#SwiftFields")
            .parameters([entityType: "ImportLetterOfCredit", entityId: res.instrumentId]).call()

        then: "Error for too many lines"
        result.errors != null
        result.errors.any { it.fieldName == "applicantName" && it.message.contains("maximum 4 lines") }

        cleanup:
        ec.entity.find("trade.TradeParty").condition("partyId", applicantId).one().set("partyName", "Acme Corporation Ltd").update()
    }

    // Existing test cases preserved and hardened...
    def "XCS-02: X charset - valid characters accepted"() {
        given: "An LC with valid X charset characters"
        def ref = "TF-XCS-V-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD",
                         goodsDescription: "STEEL PIPES 100MM, GRADE A (STANDARD)",
                         portOfLoading: "HO CHI MINH CITY",
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]],
                         availableWithEnumId: 'AW_ANY_BANK']).call()
        assert res.instrumentId != null

        when: "validate#SwiftFields is called"
        def result = ec.service.sync().name("trade.importlc.ImportLcValidationServices.validate#SwiftFields")
            .parameters([entityType: "ImportLetterOfCredit", entityId: res.instrumentId]).call()

        then: "No charset errors for these fields"
        def charsetErrors = result.errors?.findAll { it.fieldName in ["goodsDescription", "portOfLoading"] && it.message.contains("charset") }
        charsetErrors == null || charsetErrors.size() == 0
    }

    def "REF-01: Reference slash rules - violations blocked"() {
        given: "A TradeInstrument with slash rule violations"
        def ref = "/TF-REF-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD",
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]],
                         availableWithEnumId: 'AW_ANY_BANK']).call()

        when: "validate#SwiftFields is called on the TradeInstrument"
        def result = ec.service.sync().name("trade.importlc.ImportLcValidationServices.validate#SwiftFields")
            .parameters([entityType: "TradeInstrument", entityId: res.instrumentId]).call()

        then: "Error for slash rule violation"
        result.errors != null
        result.errors.any { it.fieldName == "instrumentRef" && it.message.contains("/") }
    }

    def "MEX-01: Mutual exclusion - tolerance vs max credit amount"() {
        given: "An LC with both tolerance and max credit amount flag"
        def ref = "TF-MEX-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD",
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]],
                         tolerancePositive: 10,
                         availableWithEnumId: 'AW_ANY_BANK']).call()
        
        if (ec.message.hasError()) {
            println "CREATE ERROR (MEX-01): " + ec.message.getMessages().join(", ")
        }
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", res.instrumentId).one()
        lc.set("maxCreditAmountFlag", "Y").update()

        when: "validate#SwiftFields is called"
        def result = ec.service.sync().name("trade.importlc.ImportLcValidationServices.validate#SwiftFields")
            .parameters([entityType: "ImportLetterOfCredit", entityId: res.instrumentId]).call()

        then: "Error for mutual exclusion"
        result.errors != null
        result.errors.any { it.fieldName == "maxCreditAmountFlag" && it.message.contains("mutually exclusive") }
    }

    def "CND-01: Conditional - usance fields required"() {
        given: "An LC with tenor USANCE but missing usanceDays/usanceBaseDate"
        def ref = "TF-CND-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD",
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]],
                         availableWithEnumId: 'AW_ANY_BANK']).call()
        
        if (ec.message.hasError()) {
            println "CREATE ERROR (CND-01): " + ec.message.getMessages().join(", ")
        }
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", res.instrumentId).one()
        lc.setAll([tenorTypeId: "LCT_USANCE", usanceDays: null, usanceBaseDate: null]).update()

        when: "validate#SwiftFields is called"
        def result = ec.service.sync().name("trade.importlc.ImportLcValidationServices.validate#SwiftFields")
            .parameters([entityType: "ImportLetterOfCredit", entityId: res.instrumentId]).call()

        then: "Errors for missing conditional fields"
        result.errors != null
        result.errors.any { it.fieldName == "usanceDays" && it.message.contains("Required") }
        result.errors.any { it.fieldName == "usanceBaseDate" && it.message.contains("Required") }
    }

    def "SVC-01: Validation returns ALL errors, not fail-fast"() {
        given: "An LC with multiple violations"
        def ref = "TF-SVC-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD",
                         goodsDescription: "PIPES @100MM",
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]],
                         availableWithEnumId: 'AW_ANY_BANK']).call()
        
        def lcEntity = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", res.instrumentId).one()
        lcEntity.setAll([maxCreditAmountFlag: "Y", tolerancePositive: 0.10, tenorTypeId: "LCT_USANCE", usanceDays: null]).update()

        when: "validate#SwiftFields is called"
        def result = ec.service.sync().name("trade.importlc.ImportLcValidationServices.validate#SwiftFields")
            .parameters([entityType: "ImportLetterOfCredit", entityId: res.instrumentId]).call()

        then: "At least 3 distinct field-level errors returned"
        result.errors != null
        result.errors.size() >= 3
    }

    def "SVC-02: Clean data returns no errors"() {
        given: "An LC with valid SWIFT-compliant data"
        def ref = "TF-CLEAN-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: ref, lcAmount: 500000.0, lcCurrencyUomId: "USD",
                         goodsDescription: "STEEL PIPES, GRADE A, AS PER PROFORMA INV 2026-001",
                         documentsRequired: "FULL SET OF CLEAN ON BOARD BILL OF LADING",
                         portOfLoading: "HO CHI MINH CITY",
                         portOfDischarge: "TOKYO, JAPAN",
                         expiryPlace: "VIETNAM",
                         lcTypeEnumId: "LCT_IRREVOCABLE",
                         availableByEnumId: "AVB_BY_SIGHT_PAYMENT",
                         confirmationEnumId: "CONF_WITHOUT",
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]],
                         availableWithEnumId: 'AW_ANY_BANK']).call()

        when: "validate#SwiftFields is called"
        def result = ec.service.sync().name("trade.importlc.ImportLcValidationServices.validate#SwiftFields")
            .parameters([entityType: "ImportLetterOfCredit", entityId: res.instrumentId]).call()

        then: "No errors"
        result.errors == null || result.errors.size() == 0
    }

    def "Validate mandatory SWIFT fields (40A, 41a, 49)"() {
        given: "An LC missing mandatory SWIFT fields"
        def ref = "TF-MAND-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD",
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]],
                         availableWithEnumId: 'AW_ANY_BANK']).call()
        
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", res.instrumentId).one()
        lc.setAll([lcTypeEnumId: null, availableByEnumId: null, confirmationEnumId: null]).update()

        when: "validate#SwiftFields is called"
        def result = ec.service.sync().name("trade.importlc.ImportLcValidationServices.validate#SwiftFields")
            .parameters([entityType: "ImportLetterOfCredit", entityId: res.instrumentId]).call()

        then: "Errors for 40A, 41a, 49"
        result.errors != null
        result.errors.any { it.fieldName == "lcTypeEnumId" && it.message.contains("Required") }
        result.errors.any { it.fieldName == "availableByEnumId" && it.message.contains("Required") }
        result.errors.any { it.fieldName == "confirmationEnumId" && it.message.contains("Required") }
    }

    def "MEX-02: Mutual exclusion - shipment period vs latest shipment date"() {
        given: "An LC with both shipmentPeriodText and latestShipmentDate"
        def ref = "TF-MEX2-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD",
                         latestShipmentDate: "2026-12-31", shipmentPeriodText: "DURING DECEMBER",
                         lcTypeEnumId: "LCT_IRREVOCABLE", availableByEnumId: "AVB_BY_SIGHT_PAYMENT", confirmationEnumId: "CONF_WITHOUT",
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]],
                         availableWithEnumId: 'AW_ANY_BANK']).call()

        when: "validate#SwiftFields is called"
        def result = ec.service.sync().name("trade.importlc.ImportLcValidationServices.validate#SwiftFields")
            .parameters([entityType: "ImportLetterOfCredit", entityId: res.instrumentId]).call()

        then: "Error for mutual exclusion"
        result.errors != null
        result.errors.any { it.message.contains("mutually exclusive") }
    }

    def "LIN-03: Presentation chargesDeducted exceeds 6 lines (Tag 73)"() {
        given: "A presentation with 7 lines in chargesDeducted"
        def ref = "TF-PRES-LIN-" + System.currentTimeMillis()
        def lcRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD",
                         lcTypeEnumId: "LCT_IRREVOCABLE", availableByEnumId: "AVB_BY_SIGHT_PAYMENT", confirmationEnumId: "CONF_WITHOUT",
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]],
                         availableWithEnumId: 'AW_ANY_BANK']).call()
        
        // Authorize LC
        def txIss = ec.entity.find("trade.TradeTransaction")
                .condition([instrumentId: lcRes.instrumentId.toString(), transactionTypeEnumId: 'IMP_NEW']).disableAuthz().one()
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument")
                .parameters([transactionId: txIss.transactionId, skipFourEyes: true]).call()
        ec.artifactExecution.disableAuthz()
        ec.entity.find("trade.importlc.ImportLetterOfCredit")
                .condition("instrumentId", lcRes.instrumentId).updateAll([businessStateId: "LC_ISSUED"])

        def longText = "LINE 1\nLINE 2\nLINE 3\nLINE 4\nLINE 5\nLINE 6\nLINE 7"
        def presRes = ec.service.sync().name("trade.TradeCommonServices.create#Presentation")
            .parameters([instrumentId: lcRes.instrumentId, claimAmount: 1000]).call()
        def presId = presRes.presentationId
        ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("presentationId", presId).one().set("chargesDeducted", longText).update()

        when: "validate#SwiftFields is called for TradeDocumentPresentation"
        def result = ec.service.sync().name("trade.importlc.ImportLcValidationServices.validate#SwiftFields")
            .parameters([entityType: "TradeDocumentPresentation", entityId: presId]).call()

        then: "Error for exceeding 6 lines"
        result.errors != null
        result.errors.any { it.fieldName == "chargesDeducted" && it.message.contains("6 lines") }
    }

    def "LIN-04: Presentation senderToReceiverPresentationInfo exceeds 6 lines (Tag 72Z)"() {
        given: "A presentation with 7 lines in senderToReceiverPresentationInfo"
        def ref = "TF-PRES-LIN2-" + System.currentTimeMillis()
        def lcRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD",
                         lcTypeEnumId: "LCT_IRREVOCABLE", availableByEnumId: "AVB_BY_SIGHT_PAYMENT", confirmationEnumId: "CONF_WITHOUT",
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]],
                         availableWithEnumId: 'AW_ANY_BANK']).call()

        // Authorize LC
        def txIss = ec.entity.find("trade.TradeTransaction")
                .condition([instrumentId: lcRes.instrumentId.toString(), transactionTypeEnumId: 'IMP_NEW']).disableAuthz().one()
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument")
            .parameters([transactionId: txIss.transactionId, skipFourEyes: true]).call()
        ec.artifactExecution.disableAuthz()
        ec.entity.find("trade.importlc.ImportLetterOfCredit")
                .condition("instrumentId", lcRes.instrumentId).updateAll([businessStateId: "LC_ISSUED"])

        def longText = "LINE 1\nLINE 2\nLINE 3\nLINE 4\nLINE 5\nLINE 6\nLINE 7"
        def presRes = ec.service.sync().name("trade.TradeCommonServices.create#Presentation")
            .parameters([instrumentId: lcRes.instrumentId, claimAmount: 1000]).call()
        def presId = presRes.presentationId
        ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("presentationId", presId).one().set("senderToReceiverPresentationInfo", longText).update()

        when: "validate#SwiftFields is called for TradeDocumentPresentation"
        def result = ec.service.sync().name("trade.importlc.ImportLcValidationServices.validate#SwiftFields")
            .parameters([entityType: "TradeDocumentPresentation", entityId: presId]).call()

        then: "Error for exceeding 6 lines"
        result.errors != null
        result.errors.any { it.fieldName == "senderToReceiverPresentationInfo" && it.message.contains("6 lines") }
    }
}
