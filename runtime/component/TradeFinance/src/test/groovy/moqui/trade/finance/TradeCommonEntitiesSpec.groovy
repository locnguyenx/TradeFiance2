package moqui.trade.finance

import spock.lang.Specification
import org.moqui.Moqui
import org.moqui.context.ExecutionContext

// ABOUTME: TradeCommonEntitiesSpec validates core entity structures for instruments and facilities.

class TradeCommonEntitiesSpec extends Specification {
    protected ExecutionContext ec
    
    def setup() {
        ec = Moqui.getExecutionContext()
    }
    
    def cleanup() {
        ec.destroy()
    }
    
    def "Test CREATE TradeInstrument implicitly checks structures"() {
        when:
        ec.entity.makeValue("moqui.trade.instrument.TradeInstrument")
            .setAll([instrumentId:"RESTORE-1", transactionRef:"TF-TEST-01"]).create()
            
        then:
        ec.entity.find("moqui.trade.instrument.TradeInstrument")
            .condition("instrumentId", "RESTORE-1").one() != null
            
        cleanup:
        ec.entity.find("moqui.trade.instrument.TradeInstrument")
            .condition("instrumentId", "RESTORE-1").deleteAll()
    }
}
