package moqui.trade.finance

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
        ec.entity.makeValue("moqui.trade.instrument.TradeInstrument")
            .setAll([instrumentId:"LC-ENT-1", transactionRef:"TF-LC-01"]).create()
        ec.entity.makeValue("moqui.trade.importlc.ImportLetterOfCredit")
            .setAll([instrumentId:"LC-ENT-1", businessStateId:"LC_DRAFT"]).create()
            
        when:
        def lc = ec.entity.find("moqui.trade.importlc.ImportLetterOfCredit")
            .condition("instrumentId", "LC-ENT-1").one()
            
        then:
        lc != null
        lc.instrumentId == "LC-ENT-1"
        
        cleanup:
        ec.entity.find("moqui.trade.importlc.ImportLetterOfCredit").condition("instrumentId", "LC-ENT-1").deleteAll()
        ec.entity.find("moqui.trade.instrument.TradeInstrument").condition("instrumentId", "LC-ENT-1").deleteAll()
    }

    def "ImportLetterOfCredit persists effective and new fields"() {
        setup:
        ec.entity.makeValue("moqui.trade.instrument.TradeInstrument")
                .setAll([instrumentId: "LC-MGMT-TEST", transactionRef: "TF-IMP-TEST-01"]).create()

        when:
        ec.service.sync().name("create#moqui.trade.importlc.ImportLetterOfCredit").parameters([
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
            productCatalogId: "IMP_LC_STD"
        ]).call()
        def lc = ec.entity.find("moqui.trade.importlc.ImportLetterOfCredit")
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
        ec.entity.find("moqui.trade.importlc.ImportLetterOfCredit")
            .condition("instrumentId", "LC-MGMT-TEST").deleteAll()
        ec.entity.find("moqui.trade.instrument.TradeInstrument")
            .condition("instrumentId", "LC-MGMT-TEST").deleteAll()
    }
}
