package trade

import spock.lang.Specification
import org.moqui.Moqui
import org.moqui.context.ExecutionContext

// ABOUTME: TradeTransactionSpec validates the separation of transaction processing from instrument data.

class TradeTransactionSpec extends Specification {
    protected ExecutionContext ec
    
    def setup() {
        ec = Moqui.getExecutionContext()
        ec.artifactExecution.disableAuthz()
    }
    
    def cleanup() {
        ec.destroy()
    }
    
    def "Test create TradeTransaction linked to TradeInstrument"() {
        when:
        // 1. Create Instrument (minimal)
        def instOut = ec.service.sync().name("create#trade.TradeInstrument").parameters([
            instrumentId: "TX-TEST-INST-01",
            transactionRef: "TF-TEST-01",
            businessStateId: "LC_DRAFT",
            instrumentTypeEnumId: "IMPORT_LC"
        ]).call()
        
        // 2. Create Transaction linked to Instrument
        def txOut = ec.service.sync().name("create#trade.TradeTransaction").parameters([
            transactionId: "TX-TEST-01",
            instrumentId: "TX-TEST-INST-01",
            transactionTypeEnumId: "IMP_NEW",
            transactionStatusId: "TX_DRAFT",
            transactionDate: ec.user.nowTimestamp,
            makerUserId: "TEST_USER",
            makerTimestamp: ec.user.nowTimestamp,
            versionNumber: 1
        ]).call()
        
        then:
        def inst = ec.entity.find("trade.TradeInstrument").condition("instrumentId", "TX-TEST-INST-01").one()
        def tx = ec.entity.find("trade.TradeTransaction").condition("transactionId", "TX-TEST-01").one()
        
        inst != null
        tx != null
        tx.instrumentId == inst.instrumentId
        tx.transactionTypeEnumId == "IMP_NEW"
        tx.transactionStatusId == "TX_DRAFT"
        
        cleanup:
        ec.entity.find("trade.TradeTransaction").condition("transactionId", "TX-TEST-01").deleteAll()
        ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", "TX-TEST-INST-01").deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", "TX-TEST-INST-01").deleteAll()
    }

    def "TradeTransactionAudit should include transactionId"() {
        when:
        ec.service.sync().name("create#trade.TradeTransactionAudit").parameters([
            transactionId: "TX-AUDIT-01",
            instrumentId: "INST-AUDIT-01",
            auditId: "1",
            userId: "USER_001",
            actionEnumId: "MAKER_COMMIT",
            oldValue: "TX_DRAFT",
            newValue: "TX_PENDING"
        ]).call()
        
        then:
        def audit = ec.entity.find("trade.TradeTransactionAudit")
            .condition("transactionId", "TX-AUDIT-01")
            .condition("instrumentId", "INST-AUDIT-01")
            .condition("auditId", "1").one()
            
        audit != null
        audit.transactionId == "TX-AUDIT-01"
        
        cleanup:
        ec.entity.find("trade.TradeTransactionAudit")
            .condition("transactionId", "TX-AUDIT-01")
            .condition("instrumentId", "INST-AUDIT-01")
            .condition("auditId", "1").deleteAll()
    }
}
