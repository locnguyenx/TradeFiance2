
import spock.lang.Specification
import org.moqui.Moqui
import org.moqui.context.ExecutionContext

// ABOUTME: ImportLcEntitiesSpec verifies the entity relationships and extended fields for Letters of Credit.

class ImportLcEntitiesSpec extends Specification {
    protected ExecutionContext ec
    
    def setup() {
        ec = Moqui.getExecutionContext()
        ec.user.internalLoginUser("trade.maker")
        ec.artifactExecution.disableAuthz()
    }
    
    def cleanup() {
        ec.destroy()
    }
    
    def "Test LC Relationship maps to TradeInstrument"() {
        setup:
        ec.entity.makeValue("trade.TradeInstrument")
            .setAll([instrumentId:"LC-ENT-1", transactionRef:"TF-LC-01"]).create()
        ec.entity.makeValue("trade.importlc.ImportLetterOfCredit")
            .setAll([instrumentId:"LC-ENT-1", businessStateId:"LC_DRAFT"]).create()
            
        when:
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit")
            .condition("instrumentId", "LC-ENT-1").one()
            
        then:
        lc != null
        lc.instrumentId == "LC-ENT-1"
        
        cleanup:
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", "LC-ENT-1").deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", "LC-ENT-1").deleteAll()
    }

    def "ImportLetterOfCredit persists effective and new fields"() {
        setup:
        ec.entity.makeValue("trade.TradeInstrument")
                .setAll([instrumentId: "LC-MGMT-TEST", transactionRef: "TF-IMP-TEST-01"]).create()

        when:
        ec.service.sync().name("create#trade.importlc.ImportLetterOfCredit").parameters([
            instrumentId: "LC-MGMT-TEST",
            businessStateId: "LC_DRAFT",
            effectiveAmount: 500000,
            effectiveCurrencyUomId: "USD",
            effectiveExpiryDate: "2026-12-31",
            effectiveOutstandingAmount: 500000,
            cumulativeDrawnAmount: 0,
            totalAmendmentCount: 0,
            chargeAllocationEnumId: "SHA",
            partialShipmentEnumId: "ALLOWED",
            transhipmentEnumId: "NOT_ALLOWED",
            latestShipmentDate: "2026-12-15",
            confirmationEnumId: "CONFIRMED",
            lcTypeEnumId: "IRREVOCABLE",
            productCatalogId: "PROD_IMP_LC"
        ]).call()
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit")
                .condition("instrumentId", "LC-MGMT-TEST").one()

        then:
        lc != null
        lc.effectiveAmount == 500000
        lc.effectiveOutstandingAmount == 500000
        lc.cumulativeDrawnAmount == 0
        lc.totalAmendmentCount == 0
        lc.chargeAllocationEnumId == "SHA"
        lc.latestShipmentDate == java.sql.Date.valueOf("2026-12-15")

        cleanup:
        ec.entity.find("trade.importlc.ImportLetterOfCredit")
            .condition("instrumentId", "LC-MGMT-TEST").deleteAll()
        ec.entity.find("trade.TradeInstrument")
            .condition("instrumentId", "LC-MGMT-TEST").deleteAll()
    }

    def "PresentationDiscrepancy persists correctly"() {
        setup:
        ec.entity.makeValue("trade.TradeInstrument")
                .setAll([instrumentId: "LC-MGMT-TEST", transactionRef: "TF-IMP-TEST-01"]).create()
        ec.entity.makeValue("trade.importlc.ImportLetterOfCredit")
                .setAll([instrumentId: "LC-MGMT-TEST", businessStateId: "LC_DRAFT"]).create()
        ec.entity.makeValue("trade.importlc.TradeDocumentPresentation")
                .setAll([presentationId: "PRES_TEST_01", instrumentId: "LC-MGMT-TEST", claimAmount: 1000]).create()

        when:
        ec.service.sync().name("create#trade.importlc.PresentationDiscrepancy").parameters([
            discrepancyId: "DISC_TEST_01",
            presentationId: "PRES_TEST_01",
            discrepancyCode: "DOC_MISSING",
            discrepancyDescription: "Bill of Lading missing original #3",
            isWaived: "N"
        ]).call()
        def disc = ec.entity.find("trade.importlc.PresentationDiscrepancy")
                .condition("discrepancyId", "DISC_TEST_01").one()

        then:
        disc != null
        disc.discrepancyCode == "DOC_MISSING"
        disc.isWaived == "N"

        cleanup:
        ec.entity.find("trade.importlc.PresentationDiscrepancy")
            .condition("discrepancyId", "DISC_TEST_01").deleteAll()
        ec.entity.find("trade.importlc.TradeDocumentPresentation")
            .condition("presentationId", "PRES_TEST_01").deleteAll()
        ec.entity.find("trade.importlc.ImportLetterOfCredit")
            .condition("instrumentId", "LC-MGMT-TEST").deleteAll()
        ec.entity.find("trade.TradeInstrument")
            .condition("instrumentId", "LC-MGMT-TEST").deleteAll()
    }

    def "ImportLcSettlement persists correctly"() {
        setup:
        ec.entity.makeValue("trade.TradeInstrument")
                .setAll([instrumentId: "LC-MGMT-TEST", transactionRef: "TF-IMP-TEST-01"]).create()
        ec.entity.makeValue("trade.importlc.ImportLetterOfCredit")
                .setAll([instrumentId: "LC-MGMT-TEST", businessStateId: "LC_DRAFT"]).create()
        ec.entity.makeValue("trade.importlc.TradeDocumentPresentation")
                .setAll([presentationId: "PRES_TEST_01", instrumentId: "LC-MGMT-TEST", claimAmount: 1000]).create()

        when:
        def result = ec.service.sync().name("create#trade.importlc.ImportLcSettlement").parameters([
            settlementId: "SETTLE_TEST_01",
            presentationId: "PRES_TEST_01",
            instrumentId: "LC-MGMT-TEST",
            principalAmount: 250000.0G,
            valueDate: ec.user.nowTimestamp,
            settlementTypeEnumId: "SIGHT_PAYMENT"
        ]).call()
        if (ec.message.hasError()) {
            System.err.println("DEBUG ERRORS: " + ec.message.getErrorsString())
        }
        def settle = ec.entity.find("trade.importlc.ImportLcSettlement")
                .condition("settlementId", "SETTLE_TEST_01").one()

        then:
        !ec.message.hasError()
        settle != null
        (settle.principalAmount as BigDecimal) == 250000.0G

        cleanup:
        ec.entity.find("trade.importlc.ImportLcSettlement")
            .condition("settlementId", "SETTLE_TEST_01").deleteAll()
        ec.entity.find("trade.importlc.TradeDocumentPresentation")
            .condition("presentationId", "PRES_TEST_01").deleteAll()
        ec.entity.find("trade.importlc.ImportLetterOfCredit")
            .condition("instrumentId", "LC-MGMT-TEST").deleteAll()
        ec.entity.find("trade.TradeInstrument")
            .condition("instrumentId", "LC-MGMT-TEST").deleteAll()
    }

    def "ImportLcAmendment persists extended fields"() {
        setup:
        ec.entity.makeValue("trade.TradeInstrument")
                .setAll([instrumentId: "LC-AMEND-TEST", transactionRef: "TF-LC-AM-01"]).create()
        ec.entity.makeValue("trade.importlc.ImportLetterOfCredit")
                .setAll([instrumentId: "LC-AMEND-TEST", businessStateId: "LC_ISSUED"]).create()

        when:
        ec.service.sync().name("create#trade.importlc.ImportLcAmendment").parameters([
            amendmentId: "AMEND_01",
            instrumentId: "LC-AMEND-TEST",
            amendmentBusinessStateId: "AMEND_DRAFT",
            amendmentTypeEnumId: "AMEND_INCREASE",
            isBeneficiaryAcceptanceRequired: "Y"
        ]).call()
        def am = ec.entity.find("trade.importlc.ImportLcAmendment")
                .condition("amendmentId", "AMEND_01").one()

        then:
        am != null
        am.amendmentBusinessStateId == "AMEND_DRAFT"
        am.isBeneficiaryAcceptanceRequired == "Y"

        cleanup:
        ec.entity.find("trade.importlc.ImportLcAmendment")
            .condition("amendmentId", "AMEND_01").deleteAll()
        ec.entity.find("trade.importlc.ImportLetterOfCredit")
            .condition("instrumentId", "LC-AMEND-TEST").deleteAll()
        ec.entity.find("trade.TradeInstrument")
            .condition("instrumentId", "LC-AMEND-TEST").deleteAll()
    }

    def "TradeDocumentPresentation persists status field"() {
        setup:
        ec.entity.makeValue("trade.TradeInstrument")
                .setAll([instrumentId: "LC-PRES-TEST", transactionRef: "TF-LC-PR-01"]).create()
        ec.entity.makeValue("trade.importlc.ImportLetterOfCredit")
                .setAll([instrumentId: "LC-PRES-TEST", businessStateId: "LC_ISSUED"]).create()

        when:
        ec.service.sync().name("create#trade.importlc.TradeDocumentPresentation").parameters([
            presentationId: "PRES_01",
            instrumentId: "LC-PRES-TEST",
            presentationStatusId: "PRES_DRAFT",
            claimAmount: 50000
        ]).call()
        def pr = ec.entity.find("trade.importlc.TradeDocumentPresentation")
                .condition("presentationId", "PRES_01").one()

        then:
        pr != null
        pr.presentationStatusId == "PRES_DRAFT"

        cleanup:
        ec.entity.find("trade.importlc.TradeDocumentPresentation")
            .condition("presentationId", "PRES_01").deleteAll()
        ec.entity.find("trade.importlc.ImportLetterOfCredit")
            .condition("instrumentId", "LC-PRES-TEST").deleteAll()
        ec.entity.find("trade.TradeInstrument")
            .condition("instrumentId", "LC-PRES-TEST").deleteAll()
    }

    def "ImportLcShippingGuarantee persists status field"() {
        setup:
        ec.entity.makeValue("trade.TradeInstrument")
                .setAll([instrumentId: "LC-SG-TEST", transactionRef: "TF-LC-SG-01"]).create()
        ec.entity.makeValue("trade.importlc.ImportLetterOfCredit")
                .setAll([instrumentId: "LC-SG-TEST", businessStateId: "LC_ISSUED"]).create()

        when:
        ec.service.sync().name("create#trade.importlc.ImportLcShippingGuarantee").parameters([
            guaranteeId: "SG_01",
            instrumentId: "LC-SG-TEST",
            guaranteeStatusId: "SG_DRAFT",
            invoiceAmount: 20000
        ]).call()
        def sg = ec.entity.find("trade.importlc.ImportLcShippingGuarantee")
                .condition("guaranteeId", "SG_01").one()

        then:
        sg != null
        sg.guaranteeStatusId == "SG_DRAFT"

        cleanup:
        ec.entity.find("trade.importlc.ImportLcShippingGuarantee")
            .condition("guaranteeId", "SG_01").deleteAll()
        ec.entity.find("trade.importlc.ImportLetterOfCredit")
            .condition("instrumentId", "LC-SG-TEST").deleteAll()
        ec.entity.find("trade.TradeInstrument")
            .condition("instrumentId", "LC-SG-TEST").deleteAll()
    }

    def "ImportLcAmendment persists amendmentNumber, newTolerance, chargeAllocationEnumId"() {
        setup:
        ec.entity.makeValue("trade.TradeInstrument")
                .setAll([instrumentId: "LC-AMEND-EXT", transactionRef: "TF-LC-AM-EXT"]).create()
        ec.entity.makeValue("trade.importlc.ImportLetterOfCredit")
                .setAll([instrumentId: "LC-AMEND-EXT", businessStateId: "LC_ISSUED"]).create()

        when:
        ec.service.sync().name("create#trade.importlc.ImportLcAmendment").parameters([
            amendmentId: "AMEND_EXT_01",
            instrumentId: "LC-AMEND-EXT",
            amendmentNumber: 2,
            newTolerance: 5.0,
            chargeAllocationEnumId: "SHA"
        ]).call()
        def am = ec.entity.find("trade.importlc.ImportLcAmendment")
                .condition("amendmentId", "AMEND_EXT_01").one()

        then:
        am != null
        am.amendmentNumber == 2
        am.newTolerance == 5.0
        am.chargeAllocationEnumId == "SHA"

        cleanup:
        ec.entity.find("trade.importlc.ImportLcAmendment")
            .condition("amendmentId", "AMEND_EXT_01").deleteAll()
        ec.entity.find("trade.importlc.ImportLetterOfCredit")
            .condition("instrumentId", "LC-AMEND-EXT").deleteAll()
        ec.entity.find("trade.TradeInstrument")
            .condition("instrumentId", "LC-AMEND-EXT").deleteAll()
    }

    def "TradeDocumentPresentation persists presentingBankBic, presentingBankRef, claimCurrency, regulatoryDeadline"() {
        setup:
        ec.entity.makeValue("trade.TradeInstrument")
                .setAll([instrumentId: "LC-PRES-EXT", transactionRef: "TF-LC-PR-EXT"]).create()
        ec.entity.makeValue("trade.importlc.ImportLetterOfCredit")
                .setAll([instrumentId: "LC-PRES-EXT", businessStateId: "LC_ISSUED"]).create()

        when:
        ec.service.sync().name("create#trade.importlc.TradeDocumentPresentation").parameters([
            presentationId: "PRES_EXT_01",
            instrumentId: "LC-PRES-EXT",
            presentingBankBic: "DEUTDEFF",
            presentingBankRef: "BANKREF123",
            claimCurrency: "USD",
            regulatoryDeadline: "2026-06-30",
            claimAmount: 50000
        ]).call()
        def pr = ec.entity.find("trade.importlc.TradeDocumentPresentation")
                .condition("presentationId", "PRES_EXT_01").one()

        then:
        pr != null
        pr.presentingBankBic == "DEUTDEFF"
        pr.presentingBankRef == "BANKREF123"
        pr.claimCurrency == "USD"
        pr.regulatoryDeadline == java.sql.Date.valueOf("2026-06-30")

        cleanup:
        ec.entity.find("trade.importlc.TradeDocumentPresentation")
            .condition("presentationId", "PRES_EXT_01").deleteAll()
        ec.entity.find("trade.importlc.ImportLetterOfCredit")
            .condition("instrumentId", "LC-PRES-EXT").deleteAll()
        ec.entity.find("trade.TradeInstrument")
            .condition("instrumentId", "LC-PRES-EXT").deleteAll()
    }

    def "ImportLcShippingGuarantee persists sgStatusId, waiverLockFlag, redemptionDate, issuanceFee"() {
        setup:
        ec.entity.makeValue("trade.TradeInstrument")
                .setAll([instrumentId: "LC-SG-EXT", transactionRef: "TF-LC-SG-EXT"]).create()
        ec.entity.makeValue("trade.importlc.ImportLetterOfCredit")
                .setAll([instrumentId: "LC-SG-EXT", businessStateId: "LC_ISSUED"]).create()

        when:
        ec.service.sync().name("create#trade.importlc.ImportLcShippingGuarantee").parameters([
            guaranteeId: "SG_EXT_01",
            instrumentId: "LC-SG-EXT",
            sgStatusId: "SG_ISSUED",
            waiverLockFlag: "Y",
            redemptionDate: "2026-07-15",
            issuanceFee: 150.00
        ]).call()
        def sg = ec.entity.find("trade.importlc.ImportLcShippingGuarantee")
                .condition("guaranteeId", "SG_EXT_01").one()

        then:
        sg != null
        sg.sgStatusId == "SG_ISSUED"
        sg.waiverLockFlag == "Y"
        sg.redemptionDate == java.sql.Date.valueOf("2026-07-15")
        sg.issuanceFee == 150.00

        cleanup:
        ec.entity.find("trade.importlc.ImportLcShippingGuarantee")
            .condition("guaranteeId", "SG_EXT_01").deleteAll()
        ec.entity.find("trade.importlc.ImportLetterOfCredit")
            .condition("instrumentId", "LC-SG-EXT").deleteAll()
        ec.entity.find("trade.TradeInstrument")
            .condition("instrumentId", "LC-SG-EXT").deleteAll()
    }
}
