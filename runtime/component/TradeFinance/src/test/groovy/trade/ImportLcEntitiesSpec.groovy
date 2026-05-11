package trade


import spock.lang.Specification
import spock.lang.Shared
import org.moqui.Moqui
import org.moqui.context.ExecutionContext

// ABOUTME: ImportLcEntitiesSpec verifies the entity relationships and extended fields for Letters of Credit.

class ImportLcEntitiesSpec extends Specification {
    @Shared protected ExecutionContext ec
    @Shared String testPrefix
    
    def setupSpec() {
        ec = Moqui.getExecutionContext()
        println "DEBUG: setupSpec ImportLcEntitiesSpec starting"
        ec.user.loginUser("trade.maker", "trade123")
        ec.artifactExecution.disableAuthz()
        testPrefix = "LC-ENT-" + System.currentTimeMillis()

        // Set isolated ID generation ranges - use 6500000
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrument", 39000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit", 39000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.TradeDocumentPresentation", 39000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.PresentationDiscrepancy", 39000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLcSettlement", 39000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLcAmendment", 39000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLcShippingGuarantee", 39000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLcInternalAmendment", 39000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeTransaction", 39000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrumentParty", 39000000, 1000)
        println "DEBUG: setupSpec ImportLcEntitiesSpec complete"
    }
    
    def cleanupSpec() {
        if (ec != null) {
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrument")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.TradeDocumentPresentation")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.PresentationDiscrepancy")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLcSettlement")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLcAmendment")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLcShippingGuarantee")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLcInternalAmendment")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeTransaction")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrumentParty")
            ec.destroy()
        }
    }
    
    def setup() {
        ec.message.clearAll()
        ec.artifactExecution.disableAuthz()
    }
    
    def cleanup() {
        ec.message.clearAll()
    }
    
    def "Test LC Relationship maps to TradeInstrument"() {
        when:
        def instRes = ec.service.sync().name("create#trade.TradeInstrument")
            .parameters([instrumentRef: testPrefix + "-REF-01"]).call()
        def lcId = instRes.instrumentId
        ec.service.sync().name("create#trade.importlc.ImportLetterOfCredit")
            .parameters([instrumentId: lcId, businessStateId: "LC_DRAFT"]).call()
            
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit")
            .condition("instrumentId", lcId).one()
            
        then:
        lc != null
        lc.instrumentId == lcId
    }

    def "ImportLetterOfCredit persists effective and new fields"() {
        given:
        def instRes = ec.service.sync().name("create#trade.TradeInstrument")
                .parameters([instrumentRef: testPrefix + "-REF-02"]).call()
        def lcId = instRes.instrumentId

        when:
        ec.service.sync().name("create#trade.importlc.ImportLetterOfCredit").parameters([
            instrumentId: lcId,
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
                .condition("instrumentId", lcId).one()

        then:
        lc != null
        lc.effectiveAmount == 500000
        lc.effectiveOutstandingAmount == 500000
        lc.cumulativeDrawnAmount == 0
        lc.totalAmendmentCount == 0
        lc.chargeAllocationEnumId == "CHG_SHARED"
        lc.latestShipmentDate == java.sql.Date.valueOf("2026-12-15")
    }

    def "PresentationDiscrepancy persists correctly"() {
        given:
        def instRes = ec.service.sync().name("create#trade.TradeInstrument")
                .parameters([instrumentRef: testPrefix + "-REF-03"]).call()
        def lcId = instRes.instrumentId
        ec.service.sync().name("create#trade.importlc.ImportLetterOfCredit")
                .parameters([instrumentId: lcId, businessStateId: "LC_DRAFT"]).call()
        def presRes = ec.service.sync().name("create#trade.importlc.TradeDocumentPresentation")
                .parameters([instrumentId: lcId, claimAmount: 1000]).call()
        def presId = presRes.presentationId

        when:
        def discRes = ec.service.sync().name("create#trade.importlc.PresentationDiscrepancy").parameters([
            presentationId: presId,
            discrepancyCode: "DOC_MISSING",
            discrepancyDescription: "Bill of Lading missing original #3",
            isWaived: "N"
        ]).call()
        def discId = discRes.discrepancyId
        def disc = ec.entity.find("trade.importlc.PresentationDiscrepancy")
                .condition("discrepancyId", discId).one()

        then:
        disc != null
        disc.discrepancyCode == "DOC_MISSING"
        disc.isWaived == "N"
    }

    def "ImportLcSettlement persists correctly"() {
        given:
        def instRes = ec.service.sync().name("create#trade.TradeInstrument")
                .parameters([instrumentRef: testPrefix + "-REF-04"]).call()
        def lcId = instRes.instrumentId
        ec.service.sync().name("create#trade.importlc.ImportLetterOfCredit")
                .parameters([instrumentId: lcId, businessStateId: "LC_DRAFT"]).call()
        def presRes = ec.service.sync().name("create#trade.importlc.TradeDocumentPresentation")
                .parameters([instrumentId: lcId, claimAmount: 1000]).call()
        def presId = presRes.presentationId

        when:
        def settleRes = ec.service.sync().name("create#trade.importlc.ImportLcSettlement").parameters([
            presentationId: presId,
            instrumentId: lcId,
            principalAmount: 250000.0G,
            valueDate: ec.user.nowTimestamp,
            settlementTypeEnumId: "SIGHT_PAYMENT"
        ]).call()
        def settleId = settleRes.settlementId
        if (ec.message.hasError()) {
            System.err.println("DEBUG ERRORS: " + ec.message.getErrorsString())
        }
        def settle = ec.entity.find("trade.importlc.ImportLcSettlement")
                .condition("settlementId", settleId).one()

        then:
        !ec.message.hasError()
        settle != null
        (settle.principalAmount as BigDecimal) == 250000.0G
    }

    def "ImportLcAmendment persists extended fields"() {
        given:
        def instRes = ec.service.sync().name("create#trade.TradeInstrument")
                .parameters([instrumentRef: testPrefix + "-REF-05"]).call()
        def lcId = instRes.instrumentId
        ec.entity.makeValue("trade.importlc.ImportLetterOfCredit").setAll([instrumentId: lcId, businessStateId: "LC_ISSUED"]).create()

        when:
        def amRes = ec.service.sync().name("create#trade.importlc.ImportLcAmendment").parameters([
            instrumentId: lcId,
            amendmentBusinessStateId: "AMEND_DRAFT",
            amendmentTypeEnumId: "AMEND_INCREASE",
            beneficiaryConsentStatusId: "BENE_PENDING"
        ]).call()
        def amdId = amRes.amendmentId
        def am = ec.entity.find("trade.importlc.ImportLcAmendment")
                .condition("amendmentId", amdId).one()

        then:
        am != null
        am.amendmentBusinessStateId == "AMEND_DRAFT"
        am.beneficiaryConsentStatusId == "BENE_PENDING"
    }

    def "TradeDocumentPresentation persists status field"() {
        given:
        def instRes = ec.service.sync().name("create#trade.TradeInstrument")
                .parameters([instrumentRef: testPrefix + "-REF-06"]).call()
        def lcId = instRes.instrumentId
        ec.service.sync().name("create#trade.importlc.ImportLetterOfCredit")
                .parameters([instrumentId: lcId, businessStateId: "LC_ISSUED"]).call()

        when:
        def presRes = ec.service.sync().name("create#trade.importlc.TradeDocumentPresentation").parameters([
            instrumentId: lcId,
            presentationStatusId: "PRES_DRAFT",
            claimAmount: 50000
        ]).call()
        def presId = presRes.presentationId
        def pr = ec.entity.find("trade.importlc.TradeDocumentPresentation")
                .condition("presentationId", presId).one()

        then:
        pr != null
        pr.presentationStatusId == "PRES_DRAFT"
    }

    def "ImportLcShippingGuarantee persists status field"() {
        given:
        def instRes = ec.service.sync().name("create#trade.TradeInstrument")
                .parameters([instrumentRef: testPrefix + "-REF-07"]).call()
        def lcId = instRes.instrumentId
        ec.service.sync().name("create#trade.importlc.ImportLetterOfCredit")
                .parameters([instrumentId: lcId, businessStateId: "LC_ISSUED"]).call()

        when:
        def sgRes = ec.service.sync().name("create#trade.importlc.ImportLcShippingGuarantee").parameters([
            instrumentId: lcId,
            sgStatusId: "SG_DRAFT",
            invoiceAmount: 20000
        ]).call()
        def sgId = sgRes.guaranteeId
        def sg = ec.entity.find("trade.importlc.ImportLcShippingGuarantee")
                .condition("guaranteeId", sgId).one()

        then:
        sg != null
        sg.sgStatusId == "SG_DRAFT"
    }

    def "ImportLcAmendment persists amendmentNumber, newTolerance, chargeAllocationEnumId"() {
        given:
        def instRes = ec.service.sync().name("create#trade.TradeInstrument")
                .parameters([instrumentRef: testPrefix + "-REF-08"]).call()
        def lcId = instRes.instrumentId
        ec.entity.makeValue("trade.importlc.ImportLetterOfCredit").setAll([instrumentId: lcId, businessStateId: "LC_ISSUED"]).create()

        when:
        def amRes = ec.service.sync().name("create#trade.importlc.ImportLcAmendment").parameters([
            instrumentId: lcId,
            amendmentNumber: 2,
            newTolerance: 5.0,
            chargeAllocationEnumId: "CHG_SHARED"
        ]).call()
        def amdId = amRes.amendmentId
        def am = ec.entity.find("trade.importlc.ImportLcAmendment")
                .condition("amendmentId", amdId).one()

        then:
        am != null
        am.amendmentNumber == 2
        am.newTolerance == 5.0
        am.chargeAllocationEnumId == "CHG_SHARED"
    }

    def "TradeDocumentPresentation persists claimCurrency, regulatoryDeadline and uses junction for presenting bank"() {
        given:
        def instRes = ec.service.sync().name("create#trade.TradeInstrument")
                .parameters([instrumentRef: testPrefix + "-REF-09", instrumentTypeEnumId: "IMPORT_LC"]).call()
        def lcId = instRes.instrumentId
        ec.service.sync().name("create#trade.importlc.ImportLetterOfCredit")
                .parameters([instrumentId: lcId, businessStateId: "LC_ISSUED"]).call()
        
        // Setup presenting bank junction
        def bankId = testPrefix + "-BANK-1"
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: bankId, partyName: "Presenting Bank", partyTypeEnumId: 'PTY_BANK', 
                             swiftBic: "PRESBANK", hasActiveRMA: 'Y', kycStatus: 'KYC_ACTIVE']).call()
        ec.service.sync().name("trade.TradeCommonServices.assign#InstrumentParty")
                .parameters([instrumentId: lcId, roleEnumId: "TP_PRESENTING_BANK", partyId: bankId]).call()

        when:
        def presRes = ec.service.sync().name("create#trade.importlc.TradeDocumentPresentation").parameters([
            instrumentId: lcId,
            presentingBankRef: "BANKREF123",
            claimCurrency: "USD",
            regulatoryDeadline: "2026-06-30",
            claimAmount: 50000
        ]).call()
        def presId = presRes.presentationId
        def pr = ec.entity.find("trade.importlc.TradeDocumentPresentation")
                .condition("presentationId", presId).one()

        then:
        pr != null
        pr.presentingBankRef == "BANKREF123"
        pr.claimCurrency == "USD"
        pr.regulatoryDeadline == java.sql.Date.valueOf("2026-06-30")
        
        def junc = ec.entity.find("trade.TradeInstrumentParty").condition(["instrumentId": lcId, "roleEnumId": "TP_PRESENTING_BANK"]).one()
        junc != null
        junc.partyId == bankId
    }

    def "ImportLcShippingGuarantee persists sgStatusId, waiverLockFlag, redemptionDate, issuanceFee"() {
        given:
        def instRes = ec.service.sync().name("create#trade.TradeInstrument")
                .parameters([instrumentRef: testPrefix + "-REF-10"]).call()
        def lcId = instRes.instrumentId
        ec.service.sync().name("create#trade.importlc.ImportLetterOfCredit")
                .parameters([instrumentId: lcId, businessStateId: "LC_ISSUED"]).call()

        when:
        def sgRes = ec.service.sync().name("create#trade.importlc.ImportLcShippingGuarantee").parameters([
            instrumentId: lcId,
            sgStatusId: "SG_ISSUED",
            waiverLockFlag: "Y",
            redemptionDate: "2026-07-15",
            issuanceFee: 150.00
        ]).call()
        def sgId = sgRes.guaranteeId
        def sg = ec.entity.find("trade.importlc.ImportLcShippingGuarantee")
                .condition("guaranteeId", sgId).one()

        then:
        sg != null
        sg.sgStatusId == "SG_ISSUED"
        sg.waiverLockFlag == "Y"
        sg.redemptionDate == java.sql.Date.valueOf("2026-07-15")
        sg.issuanceFee == 150.00
    }
    
    def "should persist external amendment with smart delta fields"() {
        given:
        def instRes = ec.service.sync().name("create#trade.TradeInstrument")
                .parameters([instrumentRef: testPrefix + "-REF-11"]).call()
        def lcId = instRes.instrumentId
        ec.entity.makeValue("trade.importlc.ImportLetterOfCredit").setAll([instrumentId: lcId, businessStateId: "LC_ISSUED"]).create()

        when:
        def amRes = ec.service.sync().name("create#trade.importlc.ImportLcAmendment").parameters([
            instrumentId: lcId, amendmentNumber: 1, 
            amendmentDate: ec.user.nowTimestamp, transactionRef: 'TX_1',
            amountIncrease: 50000.0, goodsActionEnumId: 'AMA_ADD', goodsDeltaText: 'Cert required',
            amendmentBusinessStateId: 'AMEND_DRAFT'
        ]).call()
        def amdId = amRes.amendmentId
        def fetched = ec.entity.find("trade.importlc.ImportLcAmendment").condition("amendmentId", amdId).one()

        then:
        fetched != null
        fetched.goodsActionEnumId == 'AMA_ADD'
        fetched.amountIncrease == 50000.0
    }

    def "should persist internal amendment"() {
        given:
        def instRes = ec.service.sync().name("create#trade.TradeInstrument")
                .parameters([instrumentRef: testPrefix + "-REF-12"]).call()
        def lcId = instRes.instrumentId
        ec.entity.makeValue("trade.importlc.ImportLetterOfCredit").setAll([instrumentId: lcId, businessStateId: "LC_ISSUED"]).create()

        when:
        def iaRes = ec.service.sync().name("create#trade.importlc.ImportLcInternalAmendment").parameters([
            instrumentId: lcId, 
            amendmentDate: ec.user.nowTimestamp, transactionRef: 'TX_2',
            newFeeDebitAccountId: 'ACC_001'
        ]).call()
        def intAmdId = iaRes.internalAmendmentId
        def fetched = ec.entity.find("trade.importlc.ImportLcInternalAmendment").condition("internalAmendmentId", intAmdId).one()

        then:
        fetched != null
        fetched.newFeeDebitAccountId == 'ACC_001'
    }
}
