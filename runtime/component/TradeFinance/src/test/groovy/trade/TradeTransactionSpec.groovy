package trade

import spock.lang.Specification
import org.moqui.Moqui
import org.moqui.context.ExecutionContext

// ABOUTME: TradeTransactionSpec validates the separation of transaction processing from instrument data.

class TradeTransactionSpec extends Specification {
    protected ExecutionContext ec
    protected String testPrefix
    
    def setup() {
        ec = Moqui.getExecutionContext()
        ec.artifactExecution.disableAuthz()
        testPrefix = "TX-SPEC-" + System.currentTimeMillis()
    }
    
    def cleanup() {
        ec.destroy()
    }
    
    def "Test create TradeTransaction linked to TradeInstrument"() {
        given:
        def instrumentId = testPrefix + "-INST-01"
        def transactionId = testPrefix + "-TX-01"

        when:
        // 1. Create Instrument (minimal)
        def instOut = ec.service.sync().name("create#trade.TradeInstrument").parameters([
            instrumentId: instrumentId,
            instrumentRef: testPrefix + "-REF-01",
            businessStateId: "LC_DRAFT",
            instrumentTypeEnumId: "IMPORT_LC"
        ]).call()
        
        // 2. Create Transaction linked to Instrument
        def txOut = ec.service.sync().name("create#trade.TradeTransaction").parameters([
            transactionId: transactionId,
            instrumentId: instrumentId,
            transactionTypeEnumId: "IMP_NEW",
            transactionStatusId: "TX_DRAFT",
            transactionDate: ec.user.nowTimestamp,
            makerUserId: "TEST_USER",
            makerTimestamp: ec.user.nowTimestamp,
            versionNumber: 1
        ]).call()
        
        then:
        def inst = ec.entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId).one()
        def tx = ec.entity.find("trade.TradeTransaction").condition("transactionId", transactionId).one()
        
        inst != null
        tx != null
        tx.instrumentId == inst.instrumentId
        tx.transactionTypeEnumId == "IMP_NEW"
        tx.transactionStatusId == "TX_DRAFT"
        
        cleanup:
        if (ec != null) {
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId)
                .updateAll([latestTransactionId: null])
            ec.entity.find("trade.TradeTransaction").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId).deleteAll()
        }
    }

    def "TradeTransactionAudit should include transactionId"() {
        given: "A parent instrument and transaction"
        def instrumentId = testPrefix + "-INST-AUDIT-01"
        def transactionId = testPrefix + "-TX-AUDIT-01"
        def auditId = testPrefix + "-AUDIT-01"
        
        ec.service.sync().name("create#trade.TradeInstrument").parameters([instrumentId: instrumentId, instrumentRef: testPrefix + "-REF-AUDIT-01"]).call()
        ec.service.sync().name("create#trade.TradeTransaction").parameters([transactionId: transactionId, instrumentId: instrumentId, transactionTypeEnumId: "IMP_NEW"]).call()

        when:
        ec.service.sync().name("create#trade.TradeTransactionAudit").parameters([
            transactionId: transactionId,
            instrumentId: instrumentId,
            auditId: auditId,
            userId: "USER_001",
            actionEnumId: "MAKER_COMMIT",
            oldValue: "TX_DRAFT",
            newValue: "TX_PENDING"
        ]).call()
        
        then:
        def audit = ec.entity.find("trade.TradeTransactionAudit")
            .condition("transactionId", transactionId)
            .condition("instrumentId", instrumentId)
            .condition("auditId", auditId).one()
            
        audit != null
        audit.transactionId == transactionId
        
        cleanup:
        if (ec != null) {
            ec.entity.find("trade.TradeTransactionAudit")
                .condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.TradeTransaction").condition("instrumentId", instrumentId).deleteAll()
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId).deleteAll()
        }
    }
    def "TradeTransaction persists transactionRef"() {
        given:
        def testInstId = testPrefix + "-REF-TEST-001"
        def testTxId = testPrefix + "-TX-REF-TEST-001"

        when:
        // Create Instrument first
        ec.service.sync().name("create#trade.TradeInstrument").parameters([
            instrumentId: testInstId,
            instrumentRef: testPrefix + "-TF-INST-01",
            instrumentTypeEnumId: "IMPORT_LC"
        ]).call()

        ec.service.sync().name("create#trade.TradeTransaction").parameters([
            transactionId: testTxId,
            instrumentId: testInstId,
            transactionRef: testPrefix + "-TF-TXN-0001",
            transactionTypeEnumId: "IMP_NEW",
            transactionStatusId: "TX_DRAFT"
        ]).call()

        def tx = ec.entity.find("trade.TradeTransaction")
            .condition("transactionId", testTxId).one()

        then:
        if (ec.message.hasError()) println "DEBUG: Moqui Errors: ${ec.message.errorsString}"
        tx != null
        tx.transactionRef == testPrefix + "-TF-TXN-0001"

        cleanup:
        if (ec != null) {
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", testInstId)
                .updateAll([latestTransactionId: null])
            ec.entity.find("trade.TradeTransaction").condition("instrumentId", testInstId).deleteAll()
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", testInstId).deleteAll()
        }
    }
}




