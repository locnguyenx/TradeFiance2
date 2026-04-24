
import spock.lang.Specification
import spock.lang.Shared
import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import java.sql.Date

// ABOUTME: ImportLcServicesSpec tests the core lifecycle services for Import Letters of Credit.
// Verifies create (maker initialization), update (versioning/draft edit), and approve (checker finalization).

class ImportLcServicesSpec extends Specification {
    @Shared ExecutionContext ec

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.artifactExecution.disableAuthz()
        ec.user.loginUser("trade.admin", "trade123")
    }

    def cleanupSpec() {
        ec.destroy()
    }

    def "should create LetterOfCredit with transaction management fields"() {
        when:
        def result = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            transactionRef: "TF-IMP-SERV-01",
            applicantPartyId: "ACME_CORP_001",
            beneficiaryPartyId: "GLOBAL_EXP_002",
            issuingBankPartyId: "ISSUING_BANK_001",
            lcAmount: 500000.0,
            lcCurrencyUomId: "USD",
            expiryDate: new Date(System.currentTimeMillis() + (90L * 24 * 60 * 60 * 1000)), 
            latestShipmentDate: new Date(System.currentTimeMillis() + (60L * 24 * 60 * 60 * 1000)),
            businessStateId: "LC_DRAFT"
        ]).call()
        def instrumentId = result.instrumentId

        then:
        !ec.message.hasError()
        instrumentId != null

        def instrumentLookup = ec.entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId).useCache(false).one()
        instrumentLookup != null
        instrumentLookup.transactionStatusId == "TX_DRAFT"
        instrumentLookup.instrumentTypeEnumId == "IMPORT_LC"
        instrumentLookup.versionNumber == 1

        cleanup:
        if (instrumentId) {
            ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId).deleteAll()
        }
    }

    def "should update LetterOfCredit draft and maintain versioning"() {
        given:
        def createResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            transactionRef: "TF-IMP-UPD-01",
            applicantPartyId: "ACME_CORP_001",
            beneficiaryPartyId: "GLOBAL_EXP_002",
            issuingBankPartyId: "ISSUING_BANK_001",
            lcAmount: 500000.0,
            lcCurrencyUomId: "USD",
            expiryDate: new Date(System.currentTimeMillis() + (90L * 24 * 60 * 60 * 1000)),
            latestShipmentDate: new Date(System.currentTimeMillis() + (60L * 24 * 60 * 60 * 1000)),
            businessStateId: "LC_DRAFT"
        ]).call()
        def instrumentId = createResult.instrumentId

        when:
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit").parameters([
            instrumentId: instrumentId,
            lcAmount: 600000.0,
            latestShipmentDate: new Date(System.currentTimeMillis() + (70L * 24 * 60 * 60 * 1000))
        ]).call()
        def instrumentLookup = ec.entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId).useCache(false).one()

        then:
        !ec.message.hasError()
        instrumentLookup != null
        instrumentLookup.amount == 600000.0
        instrumentLookup.versionNumber == 1

        cleanup:
        if (instrumentId) {
            ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId).deleteAll()
        }
    }

    def "should approve LetterOfCredit and track approval record"() {
        given:
        def createResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            transactionRef: "TF-IMP-APP-01",
            applicantPartyId: "ACME_CORP_001",
            beneficiaryPartyId: "GLOBAL_EXP_002",
            issuingBankPartyId: "ISSUING_BANK_001",
            lcAmount: 500000.0,
            lcCurrencyUomId: "USD",
            businessStateId: "LC_DRAFT"
        ]).call()
        def instrumentId = createResult.instrumentId

        when:
        ec.service.sync().name("trade.importlc.ImportLcServices.approve#ImportLetterOfCredit").parameters([
            instrumentId: instrumentId,
            approverUserId: "trade.admin",
            approvalComments: "Standard approval"
        ]).call()
        def instrumentLookup = ec.entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId).useCache(false).one()
        def approvalRecord = ec.entity.find("trade.TradeApprovalRecord").condition("instrumentId", instrumentId).one()

        then:
        !ec.message.hasError()
        instrumentLookup.transactionStatusId == "TX_APPROVED"
        approvalRecord != null
        approvalRecord.approverUserId == "trade.admin"

        cleanup:
        if (instrumentId) {
            ec.entity.find("trade.TradeApprovalRecord").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId).deleteAll()
        }
    }

    def "should create Amendment draft"() {
        given:
        def createResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            transactionRef: "TF-IMP-AMD-01",
            applicantPartyId: "ACME_CORP_001",
            beneficiaryPartyId: "GLOBAL_EXP_002",
            issuingBankPartyId: "ISSUING_BANK_001",
            lcAmount: 500000.0,
            lcCurrencyUomId: "USD",
            businessStateId: "LC_DRAFT"
        ]).call()
        if (ec.message.hasError()) println "ERROR in create (Amendment Test): " + ec.message.getErrorsString()
        assert createResult != null
        def instrumentId = createResult.instrumentId

        when:
        def amdResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#Amendment").parameters([
            instrumentId: instrumentId,
            amendmentTypeEnumId: "AMEND_INCREASE",
            amountAdjustment: 50000.0,
            amendmentDate: new Date(System.currentTimeMillis()),
            amendmentNarrative: "Increase for additional cargo"
        ]).call()
        def amendmentId = amdResult.amendmentId

        then:
        !ec.message.hasError()
        amendmentId != null
        def amdLookup = ec.entity.find("trade.importlc.ImportLcAmendment").condition("amendmentId", amendmentId).one()
        amdLookup != null
        Math.abs(amdLookup.amountAdjustment - 50000.0) < 0.001
        amdLookup.amendmentBusinessStateId == "AMEND_DRAFT"

        cleanup:
        if (instrumentId) {
            ec.entity.find("trade.importlc.ImportLcAmendment").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId).deleteAll()
        }
    }

    def "should settle Presentation and drawdown effective outstanding"() {
        given:
        def createResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            transactionRef: "TF-IMP-SET-01",
            applicantPartyId: "ACME_CORP_001",
            beneficiaryPartyId: "GLOBAL_EXP_002",
            issuingBankPartyId: "ISSUING_BANK_001",
            lcAmount: 100000.0,
            lcCurrencyUomId: "USD",
            businessStateId: "LC_DRAFT"
        ]).call()
        if (ec.message.hasError()) println "ERROR in create: " + ec.message.getErrorsString()
        assert createResult != null
        def instrumentId = createResult.instrumentId

        // Manually create a presentation record
        def presId = ec.entity.makeValue("trade.importlc.TradeDocumentPresentation", [
            instrumentId: instrumentId,
            claimAmount: 40000.0,
            presentationStatusId: "PRES_COMPLIANT"
        ]).setSequencedIdPrimary().create().presentationId

        when:
        def setlResult = ec.service.sync().name("trade.importlc.ImportLcServices.settle#Presentation").parameters([
            presentationId: presId,
            settlementAmount: 40000.0,
            settlementTypeEnumId: "SIGHT_PAYMENT"
        ]).call()

        then:
        !ec.message.hasError()
        def lcLookup = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        lcLookup != null
        Math.abs(lcLookup.effectiveOutstandingAmount - 60000.0) < 0.001
        Math.abs(lcLookup.cumulativeDrawnAmount - 40000.0) < 0.001

        cleanup:
        if (instrumentId) {
            ec.entity.find("trade.importlc.ImportLcSettlement").condition("presentationId", presId).deleteAll()
            ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("presentationId", presId).deleteAll()
            ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId).deleteAll()
        }
    }
}
