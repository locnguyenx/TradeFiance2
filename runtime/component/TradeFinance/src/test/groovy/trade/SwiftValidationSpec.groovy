package trade

import org.moqui.entity.EntityCondition
import org.moqui.context.ExecutionContext
import spock.lang.*
import org.moqui.Moqui

// ABOUTME: SwiftValidationSpec tests Layer 1 SWIFT field validation.
// ABOUTME: Covers character sets, slash rules, BIC, line format, mutual exclusion, and conditional rules.

class SwiftValidationSpec extends Specification {
    protected ExecutionContext ec

    def setupSpec() {
        ExecutionContext ec = Moqui.getExecutionContext()
        ec.user.loginUser("john.doe", "moqui")
        ec.artifactExecution.disableAuthz()
        try {
            ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrument", 9000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit", 9000000, 1000)
        } finally {
            ec.artifactExecution.enableAuthz()
            ec.destroy()
        }
    }

    def setup() {
        ec = Moqui.getExecutionContext()
        ec.user.loginUser("john.doe", "moqui")
        ec.artifactExecution.disableAuthz()
        ec.message.clearAll()
    }

    def cleanup() {
        ec.user.popUser()
        ec.message.clearAll()
    }

    def cleanupSpec() {
        ExecutionContext ec = Moqui.getExecutionContext()
        ec.user.loginUser("john.doe", "moqui")
        ec.artifactExecution.disableAuthz()
        try {
            ec.entity.find("trade.importlc.ImportLcAmendment").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "9000000").deleteAll()
            ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "9000000").deleteAll()
            ec.entity.find("trade.importlc.ImportLcSettlement").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "9000000").deleteAll()
            ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "9000000").deleteAll()
            ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "9000000").deleteAll()
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "9000000").deleteAll()
        } finally {
            ec.artifactExecution.enableAuthz()
            ec.destroy()
        }
    }

    // BDD-SWV-XCS-01: X Character Set — Invalid Characters Blocked
    def "XCS-01: X charset - invalid characters blocked"() {
        given: "An LC with invalid X charset characters"
        def ref = "TF-XCS-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD",
                         goodsDescription: "STEEL PIPES @100MM & FITTINGS",
                         portOfLoading: "HO CHI MINH CITY #1 PORT"]).call()
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
    def "ZCS-01: Z charset validation for Amendment Narrative"() {
        given: "An amendment with both valid and invalid Z charset characters"
        def ref = "TF-AMD-" + System.currentTimeMillis()
        def lcRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD"]).call()
        
        def amdRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#Amendment")
            .parameters([instrumentId: lcRes.instrumentId, amendmentNarrative: "Valid Z characters: @#=!\nInvalid: ^",
                         amendmentTypeEnumId: "AMEND_NON_FINANCIAL", amendmentDate: ec.user.nowTimestamp]).call()

        when: "validate#SwiftFields is called on the Amendment"
        def result = ec.service.sync().name("trade.importlc.ImportLcValidationServices.validate#SwiftFields")
            .parameters([entityType: "ImportLcAmendment", entityId: amdRes.amendmentId]).call()

        then: "Error is returned for the invalid character ^"
        result.errors != null
        result.errors.any { it.fieldName == "amendmentNarrative" && it.message.contains("^") }
        !result.errors.any { it.message.contains("@") }
    }

    // BDD-SWV-REF-02: Reference field length check
    def "REF-02: Reference length exceeds 16 chars"() {
        given: "A TradeInstrument with a very long reference"
        def longRef = "TF-REF-TOO-LONG-REFERENCE-12345"
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: longRef, lcAmount: 1000.0, lcCurrencyUomId: "USD"]).call()

        when: "validate#SwiftFields is called"
        def result = ec.service.sync().name("trade.importlc.ImportLcValidationServices.validate#SwiftFields")
            .parameters([entityType: "TradeInstrument", entityId: res.instrumentId]).call()

        then: "Error for length"
        result.errors != null
        result.errors.any { it.fieldName == "transactionRef" && it.message.contains("16-character maximum") }
    }

    // BDD-SWV-BIC-02: Valid BIC for Advising Bank
    def "BIC-02: Advising Bank BIC - invalid length"() {
        given: "An LC with invalid BIC length"
        def ref = "TF-BIC2-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD",
                         advisingBankBic: "BAN1"]).call()

        when: "validate#SwiftFields is called"
        def result = ec.service.sync().name("trade.importlc.ImportLcValidationServices.validate#SwiftFields")
            .parameters([entityType: "ImportLetterOfCredit", entityId: res.instrumentId]).call()

        then: "Error for BIC length"
        result.errors != null
        result.errors.any { it.fieldName == "advisingBankBic" && it.message.contains("8 or 11") }
    }

    // BDD-SWV-LIN-02: Multiline format violation for Name field
    def "LIN-02: Name field line format violation"() {
        given: "An LC with a Name field exceeding 4 lines"
        def longName = "LINE 1\nLINE 2\nLINE 3\nLINE 4\nLINE 5"
        def ref = "TF-LIN2-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD",
                         applicantName: longName]).call()

        when: "validate#SwiftFields is called"
        def result = ec.service.sync().name("trade.importlc.ImportLcValidationServices.validate#SwiftFields")
            .parameters([entityType: "ImportLetterOfCredit", entityId: res.instrumentId]).call()

        then: "Error for too many lines"
        result.errors != null
        result.errors.any { it.fieldName == "applicantName" && it.message.contains("maximum 4 lines") }
    }

    // Existing test cases preserved and hardened...
    def "XCS-02: X charset - valid characters accepted"() {
        given: "An LC with valid X charset characters"
        def ref = "TF-XCS-V-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD",
                         goodsDescription: "STEEL PIPES 100MM, GRADE A (STANDARD)",
                         portOfLoading: "HO CHI MINH CITY"]).call()
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
            .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD"]).call()

        when: "validate#SwiftFields is called on the TradeInstrument"
        def result = ec.service.sync().name("trade.importlc.ImportLcValidationServices.validate#SwiftFields")
            .parameters([entityType: "TradeInstrument", entityId: res.instrumentId]).call()

        then: "Error for slash rule violation"
        result.errors != null
        result.errors.any { it.fieldName == "transactionRef" && it.message.contains("/") }
    }

    def "MEX-01: Mutual exclusion - tolerance vs max credit amount"() {
        given: "An LC with both tolerance and maxCreditAmountFlag set"
        def ref = "TF-MEX-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD",
                         tolerancePositive: 0.10, toleranceNegative: 0.05]).call()
        
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
            .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD"]).call()
        
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", res.instrumentId).one()
        lc.setAll([tenorTypeId: "USANCE", usanceDays: null, usanceBaseDate: null]).update()

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
            .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD",
                         goodsDescription: "PIPES @100MM"]).call()
        
        def lcEntity = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", res.instrumentId).one()
        lcEntity.setAll([maxCreditAmountFlag: "Y", tolerancePositive: 0.10, tenorTypeId: "USANCE", usanceDays: null]).update()

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
            .parameters([transactionRef: ref, lcAmount: 500000.0, lcCurrencyUomId: "USD",
                         goodsDescription: "STEEL PIPES, GRADE A, AS PER PROFORMA INV 2026-001",
                         documentsRequired: "FULL SET OF CLEAN ON BOARD BILL OF LADING",
                         portOfLoading: "HO CHI MINH CITY",
                         portOfDischarge: "TOKYO, JAPAN",
                         expiryPlace: "VIETNAM",
                         applicantName: "VIETNAM IMPORT EXPORT CORP\nNO 1 LE LOI STREET\nHO CHI MINH CITY",
                         beneficiaryName: "GLOBAL STEEL TRADING LTD\n123 MAIN ROAD\nLONDON, UK"]).call()

        when: "validate#SwiftFields is called"
        def result = ec.service.sync().name("trade.importlc.ImportLcValidationServices.validate#SwiftFields")
            .parameters([entityType: "ImportLetterOfCredit", entityId: res.instrumentId]).call()

        then: "No errors"
        result.errors == null || result.errors.size() == 0
    }
}
