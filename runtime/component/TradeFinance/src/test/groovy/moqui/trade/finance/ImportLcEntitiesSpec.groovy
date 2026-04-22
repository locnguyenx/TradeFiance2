package moqui.trade.finance

import spock.lang.Specification
import org.moqui.Moqui
import org.moqui.context.ExecutionContext

// ABOUTME: ImportLcEntitiesSpec verifies the entity relationships and extended fields for Letters of Credit.

class ImportLcEntitiesSpec extends Specification {
    protected ExecutionContext ec
    
    def setup() {
        ec = Moqui.getExecutionContext()
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
}
