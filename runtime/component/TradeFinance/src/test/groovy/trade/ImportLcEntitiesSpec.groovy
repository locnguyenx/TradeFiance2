package trade


import spock.lang.Specification
import org.moqui.Moqui
import org.moqui.context.ExecutionContext

// ABOUTME: ImportLcEntitiesSpec verifies the entity relationships and extended fields for Letters of Credit.

class ImportLcEntitiesSpec extends Specification {
    protected ExecutionContext ec
    
    def setupSpec() {
        ExecutionContext ec = Moqui.getExecutionContext()
        ec.artifactExecution.disableAuthz()
        // Ensure mandatory seed data is loaded for entity tests
        long count1 = ec.entity.makeDataLoader().location("component://TradeFinance/data/TradeFinanceSeedData.xml").load()
        long count2 = ec.entity.makeDataLoader().location("component://TradeFinance/data/TradeClauseSeedData.xml").load()
        println "DEBUG_SEED: Loaded ${count1} records from TradeFinanceSeedData.xml"
        println "DEBUG_SEED: Loaded ${count2} records from TradeClauseSeedData.xml"
        // Load inline entity seed-data (moved from TradeFinanceSeedData.xml)
        long count3 = ec.entity.makeDataLoader().location("component://TradeFinance/entity/TradeCommonEntities.xml").load()
        long count4 = ec.entity.makeDataLoader().location("component://TradeFinance/entity/ImportLcEntities.xml").load()
        println "DEBUG_SEED: Loaded ${count3} records from TradeCommonEntities.xml"
        println "DEBUG_SEED: Loaded ${count4} records from ImportLcEntities.xml"
        
        def ids = ["LC-ENT-1", "LC-ENT-PERSIST", "LC-AMEND-TEST", "LC-PRES-TEST", "LC-SG-TEST", "LC-AMEND-EXT", "LC-PRES-EXT", "LC-SG-EXT", "LC_AMD_DELTA", "LC_INT_AMD"]
        for (id in ids) {
            ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", id).deleteAll()
            ec.entity.find("trade.importlc.ImportLcSettlement").condition("instrumentId", id).deleteAll()
            ec.entity.find("trade.importlc.ImportLcAmendment").condition("instrumentId", id).deleteAll()
            ec.entity.find("trade.importlc.ImportLcInternalAmendment").condition("instrumentId", id).deleteAll()
            ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("instrumentId", id).deleteAll()
            ec.entity.find("trade.importlc.ImportLcShippingGuarantee").condition("instrumentId", id).deleteAll()
            ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", id).deleteAll()
            ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", id).deleteAll()
            ec.entity.find("trade.TradeTransaction").condition("instrumentId", id).deleteAll()
            ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", id).deleteAll()
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", id).deleteAll()
        }
        ec.destroy()
    }
    
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
        ec.service.sync().name("create#trade.TradeInstrument")
            .parameters([instrumentId:"LC-ENT-1", instrumentRef:"TF-LC-01"]).call()
        ec.service.sync().name("create#trade.importlc.ImportLetterOfCredit")
            .parameters([instrumentId:"LC-ENT-1", businessStateId:"LC_DRAFT"]).call()
            
        when:
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit")
            .condition("instrumentId", "LC-ENT-1").one()
            
        then:
        lc != null
        lc.instrumentId == "LC-ENT-1"
        
        cleanup:
        ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", "LC-ENT-1").deleteAll()
        ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", "LC-ENT-1").deleteAll()
        ec.entity.find("trade.TradeTransaction").condition("instrumentId", "LC-ENT-1").deleteAll()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", "LC-ENT-1").deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", "LC-ENT-1").deleteAll()
    }

    def "ImportLetterOfCredit persists effective and new fields"() {
        setup:
        ec.service.sync().name("create#trade.TradeInstrument")
                .parameters([instrumentId: "LC-ENT-PERSIST", instrumentRef: "TF-IMP-TEST-01"]).call()

        when:
        ec.service.sync().name("create#trade.importlc.ImportLetterOfCredit").parameters([
            instrumentId: "LC-ENT-PERSIST",
            businessStateId: "LC_DRAFT",
            effectiveAmount: 500000,
            effectiveCurrencyUomId: "USD",
            effectiveExpiryDate: "2026-12-31",
            effectiveOutstandingAmount: 500000,
            cumulativeDrawnAmount: 0,
            totalAmendmentCount: 0,
            chargeAllocationEnumId: "CHG_SHARED",
            partialShipmentEnumId: "ALLOWED",
            transhipmentEnumId: "NOT_ALLOWED",
            latestShipmentDate: "2026-12-15",
            confirmationEnumId: "CONF_CONFIRMED",
            lcTypeEnumId: "LCT_IRREVOCABLE",
            productCatalogId: "PROD_IMP_LC"
        ]).call()
        if (ec.message.hasError()) ec.logger.info("TEST ERRORS: " + ec.message.getErrorsString())
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit")
                .condition("instrumentId", "LC-ENT-PERSIST").one()

        then:
        lc != null
        lc.effectiveAmount == 500000
        lc.effectiveOutstandingAmount == 500000
        lc.cumulativeDrawnAmount == 0
        lc.totalAmendmentCount == 0
        lc.chargeAllocationEnumId == "CHG_SHARED"
        lc.latestShipmentDate == java.sql.Date.valueOf("2026-12-15")

        cleanup:
        ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", "LC-ENT-PERSIST").deleteAll()
        ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", "LC-ENT-PERSIST").deleteAll()
        ec.entity.find("trade.TradeTransaction").condition("instrumentId", "LC-ENT-PERSIST").deleteAll()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", "LC-ENT-PERSIST").deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", "LC-ENT-PERSIST").deleteAll()
    }

    def "PresentationDiscrepancy persists correctly"() {
        setup:
        ec.service.sync().name("create#trade.TradeInstrument")
                .parameters([instrumentId: "LC-ENT-DISC", instrumentRef: "TF-IMP-TEST-01"]).call()
        ec.service.sync().name("create#trade.importlc.ImportLetterOfCredit")
                .parameters([instrumentId: "LC-ENT-DISC", businessStateId: "LC_DRAFT"]).call()
        ec.service.sync().name("create#trade.importlc.TradeDocumentPresentation")
                .parameters([presentationId: "PRES_TEST_01", instrumentId: "LC-ENT-DISC", claimAmount: 1000]).call()

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
        ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", "LC-ENT-DISC").deleteAll()
        ec.entity.find("trade.importlc.PresentationDiscrepancy").condition("presentationId", "PRES_TEST_01").deleteAll()
        ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("instrumentId", "LC-ENT-DISC").deleteAll()
        ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", "LC-ENT-DISC").deleteAll()
        ec.entity.find("trade.TradeTransaction").condition("instrumentId", "LC-ENT-DISC").deleteAll()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", "LC-ENT-DISC").deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", "LC-ENT-DISC").deleteAll()
    }

    def "ImportLcSettlement persists correctly"() {
        setup:
        ec.service.sync().name("create#trade.TradeInstrument")
                .parameters([instrumentId: "LC-ENT-SETTLE", instrumentRef: "TF-IMP-TEST-01"]).call()
        ec.service.sync().name("create#trade.importlc.ImportLetterOfCredit")
                .parameters([instrumentId: "LC-ENT-SETTLE", businessStateId: "LC_DRAFT"]).call()
        ec.service.sync().name("create#trade.importlc.TradeDocumentPresentation")
                .parameters([presentationId: "PRES_TEST_01", instrumentId: "LC-ENT-SETTLE", claimAmount: 1000]).call()

        when:
        def result = ec.service.sync().name("create#trade.importlc.ImportLcSettlement").parameters([
            settlementId: "SETTLE_TEST_01",
            presentationId: "PRES_TEST_01",
            instrumentId: "LC-ENT-SETTLE",
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
        ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", "LC-ENT-SETTLE").deleteAll()
        ec.entity.find("trade.importlc.ImportLcSettlement").condition("instrumentId", "LC-ENT-SETTLE").deleteAll()
        ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("instrumentId", "LC-ENT-SETTLE").deleteAll()
        ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", "LC-ENT-SETTLE").deleteAll()
        ec.entity.find("trade.TradeTransaction").condition("instrumentId", "LC-ENT-SETTLE").deleteAll()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", "LC-ENT-SETTLE").deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", "LC-ENT-SETTLE").deleteAll()
    }

    def "ImportLcAmendment persists extended fields"() {
        setup:
        ec.entity.makeValue("trade.TradeInstrument").setAll([instrumentId: "LC-AMEND-TEST", instrumentRef: "TF-LC-AM-01"]).create()
        ec.entity.makeValue("trade.importlc.ImportLetterOfCredit").setAll([instrumentId: "LC-AMEND-TEST", businessStateId: "LC_ISSUED"]).create()

        when:
        def amValue = ec.entity.makeValue("trade.importlc.ImportLcAmendment").setAll([
            amendmentId: "AMEND_01",
            instrumentId: "LC-AMEND-TEST",
            amendmentBusinessStateId: "AMEND_DRAFT",
            amendmentTypeEnumId: "AMEND_INCREASE",
            beneficiaryConsentStatusId: "BENE_PENDING"
        ]).create()
        def am = ec.entity.find("trade.importlc.ImportLcAmendment")
                .condition("amendmentId", "AMEND_01").one()

        then:
        am != null
        am.amendmentBusinessStateId == "AMEND_DRAFT"
        am.beneficiaryConsentStatusId == "BENE_PENDING"

        cleanup:
        ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", "LC-AMEND-TEST").deleteAll()
        ec.entity.find("trade.importlc.ImportLcAmendment").condition("instrumentId", "LC-AMEND-TEST").deleteAll()
        ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", "LC-AMEND-TEST").deleteAll()
        ec.entity.find("trade.TradeTransaction").condition("instrumentId", "LC-AMEND-TEST").deleteAll()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", "LC-AMEND-TEST").deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", "LC-AMEND-TEST").deleteAll()
    }

    def "TradeDocumentPresentation persists status field"() {
        setup:
        ec.service.sync().name("create#trade.TradeInstrument")
                .parameters([instrumentId: "LC-PRES-TEST", instrumentRef: "TF-LC-PR-01"]).call()
        ec.service.sync().name("create#trade.importlc.ImportLetterOfCredit")
                .parameters([instrumentId: "LC-PRES-TEST", businessStateId: "LC_ISSUED"]).call()

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
        ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", "LC-PRES-TEST").deleteAll()
        ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("instrumentId", "LC-PRES-TEST").deleteAll()
        ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", "LC-PRES-TEST").deleteAll()
        ec.entity.find("trade.TradeTransaction").condition("instrumentId", "LC-PRES-TEST").deleteAll()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", "LC-PRES-TEST").deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", "LC-PRES-TEST").deleteAll()
    }

    def "ImportLcShippingGuarantee persists status field"() {
        setup:
        ec.service.sync().name("create#trade.TradeInstrument")
                .parameters([instrumentId: "LC-SG-TEST", instrumentRef: "TF-LC-SG-01"]).call()
        ec.service.sync().name("create#trade.importlc.ImportLetterOfCredit")
                .parameters([instrumentId: "LC-SG-TEST", businessStateId: "LC_ISSUED"]).call()

        when:
        ec.service.sync().name("create#trade.importlc.ImportLcShippingGuarantee").parameters([
            guaranteeId: "SG_01",
            instrumentId: "LC-SG-TEST",
            sgStatusId: "SG_DRAFT",
            invoiceAmount: 20000
        ]).call()
        def sg = ec.entity.find("trade.importlc.ImportLcShippingGuarantee")
                .condition("guaranteeId", "SG_01").one()

        then:
        sg != null
        sg.sgStatusId == "SG_DRAFT"

        cleanup:
        ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", "LC-SG-TEST").deleteAll()
        ec.entity.find("trade.importlc.ImportLcShippingGuarantee").condition("instrumentId", "LC-SG-TEST").deleteAll()
        ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", "LC-SG-TEST").deleteAll()
        ec.entity.find("trade.TradeTransaction").condition("instrumentId", "LC-SG-TEST").deleteAll()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", "LC-SG-TEST").deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", "LC-SG-TEST").deleteAll()
    }

    def "ImportLcAmendment persists amendmentNumber, newTolerance, chargeAllocationEnumId"() {
        setup:
        ec.entity.makeValue("trade.TradeInstrument").setAll([instrumentId: "LC-AMEND-EXT", instrumentRef: "TF-LC-AM-EXT"]).create()
        ec.entity.makeValue("trade.importlc.ImportLetterOfCredit").setAll([instrumentId: "LC-AMEND-EXT", businessStateId: "LC_ISSUED"]).create()

        when:
        ec.entity.makeValue("trade.importlc.ImportLcAmendment").setAll([
            amendmentId: "AMEND_EXT_01",
            instrumentId: "LC-AMEND-EXT",
            amendmentNumber: 2,
            newTolerance: 5.0,
            chargeAllocationEnumId: "CHG_SHARED"
        ]).create()
        def am = ec.entity.find("trade.importlc.ImportLcAmendment")
                .condition("amendmentId", "AMEND_EXT_01").one()

        then:
        am != null
        am.amendmentNumber == 2
        am.newTolerance == 5.0
        am.chargeAllocationEnumId == "CHG_SHARED"

        cleanup:
        ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", "LC-AMEND-EXT").deleteAll()
        ec.entity.find("trade.importlc.ImportLcAmendment").condition("instrumentId", "LC-AMEND-EXT").deleteAll()
        ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", "LC-AMEND-EXT").deleteAll()
        ec.entity.find("trade.TradeTransaction").condition("instrumentId", "LC-AMEND-EXT").deleteAll()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", "LC-AMEND-EXT").deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", "LC-AMEND-EXT").deleteAll()
    }

    def "TradeDocumentPresentation persists claimCurrency, regulatoryDeadline and uses junction for presenting bank"() {
        setup:
        ec.service.sync().name("create#trade.TradeInstrument")
                .parameters([instrumentId: "LC-PRES-EXT", instrumentRef: "TF-LC-PR-EXT", instrumentTypeEnumId: "IMPORT_LC"]).call()
        ec.service.sync().name("create#trade.importlc.ImportLetterOfCredit")
                .parameters([instrumentId: "LC-PRES-EXT", businessStateId: "LC_ISSUED"]).call()
        
        // Setup presenting bank junction
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: "PRES_BANK_1", partyName: "Presenting Bank", partyTypeEnumId: 'PTY_BANK', 
                             swiftBic: "PRESBANK", hasActiveRMA: true, kycStatus: 'KYC_ACTIVE']).call()
        ec.service.sync().name("trade.TradeCommonServices.assign#InstrumentParty")
                .parameters([instrumentId: "LC-PRES-EXT", roleEnumId: "TP_PRESENTING_BANK", partyId: "PRES_BANK_1"]).call()

        when:
        ec.service.sync().name("create#trade.importlc.TradeDocumentPresentation").parameters([
            presentationId: "PRES_EXT_01",
            instrumentId: "LC-PRES-EXT",
            presentingBankRef: "BANKREF123",
            claimCurrency: "USD",
            regulatoryDeadline: "2026-06-30",
            claimAmount: 50000
        ]).call()
        def pr = ec.entity.find("trade.importlc.TradeDocumentPresentation")
                .condition("presentationId", "PRES_EXT_01").one()

        then:
        pr != null
        pr.presentingBankRef == "BANKREF123"
        pr.claimCurrency == "USD"
        pr.regulatoryDeadline == java.sql.Date.valueOf("2026-06-30")
        
        def junc = ec.entity.find("trade.TradeInstrumentParty").condition(["instrumentId": "LC-PRES-EXT", "roleEnumId": "TP_PRESENTING_BANK"]).one()
        junc != null
        junc.partyId == "PRES_BANK_1"

        cleanup:
        ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", "LC-PRES-EXT").deleteAll()
        ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("instrumentId", "LC-PRES-EXT").deleteAll()
        ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", "LC-PRES-EXT").deleteAll()
        ec.entity.find("trade.TradePartyBank").condition("partyId", "PRES_BANK_1").deleteAll()
        ec.entity.find("trade.TradeParty").condition("partyId", "PRES_BANK_1").deleteAll()
        ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", "LC-PRES-EXT").deleteAll()
        ec.entity.find("trade.TradeTransaction").condition("instrumentId", "LC-PRES-EXT").deleteAll()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", "LC-PRES-EXT").deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", "LC-PRES-EXT").deleteAll()
    }

    def "ImportLcShippingGuarantee persists sgStatusId, waiverLockFlag, redemptionDate, issuanceFee"() {
        setup:
        ec.service.sync().name("create#trade.TradeInstrument")
                .parameters([instrumentId: "LC-SG-EXT", instrumentRef: "TF-LC-SG-EXT"]).call()
        ec.service.sync().name("create#trade.importlc.ImportLetterOfCredit")
                .parameters([instrumentId: "LC-SG-EXT", businessStateId: "LC_ISSUED"]).call()

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
        ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", "LC-SG-EXT").deleteAll()
        ec.entity.find("trade.importlc.ImportLcShippingGuarantee").condition("instrumentId", "LC-SG-EXT").deleteAll()
        ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", "LC-SG-EXT").deleteAll()
        ec.entity.find("trade.TradeTransaction").condition("instrumentId", "LC-SG-EXT").deleteAll()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", "LC-SG-EXT").deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", "LC-SG-EXT").deleteAll()
    }
    
    def "should persist external amendment with smart delta fields"() {
        setup:
        ec.entity.makeValue("trade.TradeInstrument").setAll([instrumentId: "LC_AMD_DELTA", instrumentRef: "TF-LC-AMD-DELTA"]).create()
        ec.entity.makeValue("trade.importlc.ImportLetterOfCredit").setAll([instrumentId: "LC_AMD_DELTA", businessStateId: "LC_ISSUED"]).create()

        when:
        def amd = ec.entity.makeValue("trade.importlc.ImportLcAmendment")
        amd.setAll([
            amendmentId: 'AMD_TEST_01', instrumentId: 'LC_AMD_DELTA', amendmentNumber: 1, 
            amendmentDate: ec.user.nowTimestamp, transactionRef: 'TX_1',
            amountIncrease: 50000.0, goodsActionEnumId: 'AMA_ADD', goodsDeltaText: 'Cert required',
            amendmentBusinessStateId: 'AMEND_DRAFT'
        ])
        amd.create()
        def fetched = ec.entity.find("trade.importlc.ImportLcAmendment").condition("amendmentId", "AMD_TEST_01").one()

        then:
        fetched != null
        fetched.goodsActionEnumId == 'AMA_ADD'
        fetched.amountIncrease == 50000.0

        cleanup:
        ec.entity.find("trade.importlc.ImportLcAmendment").condition("amendmentId", "AMD_TEST_01").deleteAll()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", "LC_AMD_DELTA").deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", "LC_AMD_DELTA").deleteAll()
    }

    def "should persist internal amendment"() {
        setup:
        ec.entity.makeValue("trade.TradeInstrument").setAll([instrumentId: "LC_INT_AMD", instrumentRef: "TF-LC-INT-AMD"]).create()
        ec.entity.makeValue("trade.importlc.ImportLetterOfCredit").setAll([instrumentId: "LC_INT_AMD", businessStateId: "LC_ISSUED"]).create()

        when:
        def intAmd = ec.entity.makeValue("trade.importlc.ImportLcInternalAmendment")
        intAmd.setAll([
            internalAmendmentId: 'INT_TEST_01', instrumentId: 'LC_INT_AMD', 
            amendmentDate: ec.user.nowTimestamp, transactionRef: 'TX_2',
            newFeeDebitAccountId: 'ACC_001'
        ])
        intAmd.create()
        def fetched = ec.entity.find("trade.importlc.ImportLcInternalAmendment").condition("internalAmendmentId", "INT_TEST_01").one()

        then:
        fetched != null
        fetched.newFeeDebitAccountId == 'ACC_001'

        cleanup:
        ec.entity.find("trade.importlc.ImportLcInternalAmendment").condition("internalAmendmentId", "INT_TEST_01").deleteAll()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", "LC_INT_AMD").deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", "LC_INT_AMD").deleteAll()
    }
}
