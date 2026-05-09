package trade

import spock.lang.Specification
import org.moqui.Moqui
import org.moqui.context.ExecutionContext

// ABOUTME: TradeTransactionViewSpec verifies the hydration of business references in the unified transaction view.
// ABOUTME: Ensures consistency between list-view data sources and detail-view entities.

class TradeTransactionViewSpec extends Specification {
    protected ExecutionContext ec
    
    def setup() {
        ec = Moqui.getExecutionContext()
        ec.artifactExecution.disableAuthz()
        ec.user.internalLoginUser("trade.maker")
    }
    
    def cleanup() {
        if (ec != null) ec.destroy()
    }
    
    def "TradeTransactionView correctly hydrates instrument and transaction references"() {
        given:
        def instId = "VIEW-INST-001"
        def instRef = "TF-VIEW-INST-001"
        def txId = "VIEW-TXN-001"
        def txRef = "TF-VIEW-TXN-001"

        // Create Instrument
        ec.entity.makeValue("trade.TradeInstrument").setAll([
            instrumentId: instId, 
            instrumentRef: instRef,
            instrumentTypeEnumId: "IMPORT_LC"
        ]).createOrUpdate()
        
        // Create Transaction
        ec.entity.makeValue("trade.TradeTransaction").setAll([
            transactionId: txId, 
            instrumentId: instId, 
            transactionRef: txRef,
            transactionTypeEnumId: "IMP_NEW",
            transactionStatusId: "TX_DRAFT",
            transactionDate: ec.user.nowTimestamp,
            makerUserId: "trade.maker"
        ]).createOrUpdate()
        
        when:
        def view = ec.entity.find("trade.TradeTransactionView").condition("transactionId", txId).one()
        
        then:
        view != null
        view.transactionId == txId
        view.transactionRef == txRef
        view.instrumentId == instId
        view.instrumentRef == instRef
        
        cleanup:
        ec.artifactExecution.disableAuthz()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", instId).updateAll([latestTransactionId: null])
        ec.entity.find("trade.importlc.NostroReconciliation").condition("instrumentId", instId).deleteAll()
        ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", instId).deleteAll()
        ec.entity.find("trade.TradeTransactionAudit").condition("transactionId", txId).deleteAll()
        ec.entity.find("trade.TradeApprovalRecord").condition("transactionId", txId).deleteAll()
        ec.entity.find("trade.TradeTransaction").condition("transactionId", txId).deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", instId).deleteAll()
    }

    def "get#TradeTransactions service returns correct reference fields"() {
        given:
        def instId = "SVC-INST-001"
        def instRef = "TF-SVC-INST-001"
        def txId = "SVC-TXN-001"
        def txRef = "TF-SVC-TXN-001"

        ec.entity.makeValue("trade.TradeInstrument").setAll([
            instrumentId: instId, 
            instrumentRef: instRef,
            instrumentTypeEnumId: "IMPORT_LC"
        ]).createOrUpdate()
        
        ec.entity.makeValue("trade.TradeTransaction").setAll([
            transactionId: txId, 
            instrumentId: instId, 
            transactionRef: txRef,
            transactionTypeEnumId: "IMP_NEW",
            transactionStatusId: "TX_DRAFT",
            transactionDate: ec.user.nowTimestamp,
            makerUserId: "trade.maker"
        ]).createOrUpdate()

        when:
        def result = ec.service.sync().name("trade.TradeCommonServices.get#TradeTransactions")
            .parameters([transactionStatusId: "TX_DRAFT", pageSize: 100]).call()
        def txn = result.transactionList.find { it.transactionId == txId }

        then:
        txn != null
        txn.transactionRef == txRef
        txn.instrumentRef == instRef

        cleanup:
        ec.artifactExecution.disableAuthz()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", instId).updateAll([latestTransactionId: null])
        ec.entity.find("trade.importlc.NostroReconciliation").condition("instrumentId", instId).deleteAll()
        ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", instId).deleteAll()
        ec.entity.find("trade.TradeTransactionAudit").condition("transactionId", txId).deleteAll()
        ec.entity.find("trade.TradeApprovalRecord").condition("transactionId", txId).deleteAll()
        ec.entity.find("trade.TradeTransaction").condition("transactionId", txId).deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", instId).deleteAll()
    }

    def "get#TradeTransaction service hydrates LC details and parties"() {
        given:
        def instId = "HYD-LC-001"
        def txId = "HYD-TXN-001"
        def bankPartyId = "HYD-BANK-001"

        // Create Instrument and LC
        ec.entity.makeValue("trade.TradeInstrument").setAll([
            instrumentId: instId, instrumentRef: "TF-HYD-001",
            instrumentTypeEnumId: "IMPORT_LC", amount: 100000, currencyUomId: "USD"
        ]).createOrUpdate()
        
        ec.entity.makeValue("trade.importlc.ImportLetterOfCredit").setAll([
            instrumentId: instId, availableWithEnumId: "AVAIL_SPECIFIC_BANK",
            availableByEnumId: "BY_SIGHT", goodsDescription: "TEST GOODS"
        ]).createOrUpdate()

        // Create Bank Party and link
        ec.entity.makeValue("trade.TradeParty").setAll([partyId: bankPartyId, partyName: "Hydration Bank"]).createOrUpdate()
        ec.entity.makeValue("trade.TradePartyBank").setAll([partyId: bankPartyId, swiftBic: "HYDBANKXXX"]).createOrUpdate()
        ec.entity.makeValue("trade.TradeInstrumentParty").setAll([
            instrumentId: instId, partyId: bankPartyId, roleEnumId: "TP_NEGOTIATING_BANK"
        ]).createOrUpdate()

        // Create Transaction
        ec.entity.makeValue("trade.TradeTransaction").setAll([
            transactionId: txId, instrumentId: instId, transactionRef: "REF-HYD-001",
            transactionTypeEnumId: "IMP_NEW", transactionStatusId: "TX_DRAFT",
            transactionDate: ec.user.nowTimestamp, makerUserId: "trade.maker"
        ]).createOrUpdate()

        when:
        def result = ec.service.sync().name("trade.TradeCommonServices.get#TradeTransaction")
            .parameter("transactionId", txId).call()

        then:
        result.transactionId == txId
        result.instrument != null
        result.instrument.instrumentId == instId
        result.instrument.availableWithEnumId == "AVAIL_SPECIFIC_BANK"
        result.instrument.goodsDescription == "TEST GOODS"
        
        // Verify parties hydration
        result.instrument.parties != null
        result.instrument.parties.size() > 0
        def negBank = result.instrument.parties.find { it.roleEnumId == "TP_NEGOTIATING_BANK" }
        negBank != null
        negBank.partyName == "Hydration Bank"
        negBank.swiftBic == "HYDBANKXXX"

        cleanup:
        ec.artifactExecution.disableAuthz()
        ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", instId).deleteAll()
        ec.entity.find("trade.TradePartyBank").condition("partyId", bankPartyId).deleteAll()
        ec.entity.find("trade.TradeParty").condition("partyId", bankPartyId).deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", instId).updateAll([latestTransactionId: null])
        ec.entity.find("trade.importlc.NostroReconciliation").condition("instrumentId", instId).deleteAll()
        ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", instId).deleteAll()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instId).deleteAll()
        ec.entity.find("trade.TradeTransactionAudit").condition("transactionId", txId).deleteAll()
        ec.entity.find("trade.TradeApprovalRecord").condition("transactionId", txId).deleteAll()
        ec.entity.find("trade.TradeTransaction").condition("transactionId", txId).deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", instId).deleteAll()
    }

    def "get#TradeTransactions search by instrument"() {
        given:
        def instId = "SEARCH-INST-001"
        def instRef = "TF-SEARCH-001"
        def txId = "SEARCH-TXN-001"

        ec.entity.makeValue("trade.TradeInstrument").setAll([
            instrumentId: instId, instrumentRef: instRef, instrumentTypeEnumId: "IMPORT_LC"
        ]).createOrUpdate()
        
        ec.entity.makeValue("trade.TradeTransaction").setAll([
            transactionId: txId, instrumentId: instId, transactionRef: "TX-SEARCH-001",
            transactionTypeEnumId: "IMP_NEW", transactionStatusId: "TX_DRAFT",
            transactionDate: ec.user.nowTimestamp, makerUserId: "trade.maker"
        ]).createOrUpdate()

        when:
        def result = ec.service.sync().name("trade.TradeCommonServices.get#TradeTransactions")
            .parameter("instrumentSearch", "SEARCH-001").call()
        def txnByRef = result.transactionList.find { it.transactionId == txId }

        def result2 = ec.service.sync().name("trade.TradeCommonServices.get#TradeTransactions")
            .parameter("instrumentSearch", instId).call()
        def txnById = result2.transactionList.find { it.transactionId == txId }

        then:
        txnByRef != null
        txnById != null

        cleanup:
        ec.artifactExecution.disableAuthz()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", instId).updateAll([latestTransactionId: null])
        ec.entity.find("trade.importlc.NostroReconciliation").condition("instrumentId", instId).deleteAll()
        ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", instId).deleteAll()
        ec.entity.find("trade.TradeTransactionAudit").condition("transactionId", txId).deleteAll()
        ec.entity.find("trade.TradeApprovalRecord").condition("transactionId", txId).deleteAll()
        ec.entity.find("trade.TradeTransaction").condition("transactionId", txId).deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", instId).deleteAll()
    }

    def "get#TradeTransactions search by transaction"() {
        given:
        def instId = "TSEARCH-INST-001"
        def txId = "TSEARCH-TXN-001"
        def txRef = "TF-TX-SEARCH-001"

        ec.entity.makeValue("trade.TradeInstrument").setAll([
            instrumentId: instId, instrumentTypeEnumId: "IMPORT_LC"
        ]).createOrUpdate()
        
        ec.entity.makeValue("trade.TradeTransaction").setAll([
            transactionId: txId, instrumentId: instId, transactionRef: txRef,
            transactionTypeEnumId: "IMP_NEW", transactionStatusId: "TX_DRAFT",
            transactionDate: ec.user.nowTimestamp, makerUserId: "trade.maker"
        ]).createOrUpdate()

        when:
        def result = ec.service.sync().name("trade.TradeCommonServices.get#TradeTransactions")
            .parameter("transactionSearch", "TX-SEARCH").call()
        def txnByRef = result.transactionList.find { it.transactionId == txId }

        def result2 = ec.service.sync().name("trade.TradeCommonServices.get#TradeTransactions")
            .parameter("transactionSearch", txId).call()
        def txnById = result2.transactionList.find { it.transactionId == txId }

        then:
        txnByRef != null
        txnById != null

        cleanup:
        ec.artifactExecution.disableAuthz()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", instId).updateAll([latestTransactionId: null])
        ec.entity.find("trade.importlc.NostroReconciliation").condition("instrumentId", instId).deleteAll()
        ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", instId).deleteAll()
        ec.entity.find("trade.TradeTransactionAudit").condition("transactionId", txId).deleteAll()
        ec.entity.find("trade.TradeApprovalRecord").condition("transactionId", txId).deleteAll()
        ec.entity.find("trade.TradeTransaction").condition("transactionId", txId).deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", instId).deleteAll()
    }
    def "get#TradeTransactions supports pagination"() {
        setup:
        ec.artifactExecution.disableAuthz()
        def testMaker = "pagination.test.user"
        def testInstId = "INST-PAG"
        // Ensure no leftover data
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", testInstId).updateAll([latestTransactionId: null])
        ec.entity.find("trade.TradeTransaction").condition("makerUserId", testMaker).deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", testInstId).deleteAll()

        ec.entity.makeValue("trade.TradeInstrument").setAll([
            instrumentId: testInstId, instrumentTypeEnumId: "IMPORT_LC"
        ]).createOrUpdate()
        
        6.times { i ->
            ec.entity.makeValue("trade.TradeTransaction").setAll([
                transactionId: "PAG-TX-${i}", instrumentId: testInstId, 
                transactionTypeEnumId: "IMP_NEW", transactionStatusId: "TX_DRAFT",
                transactionDate: ec.user.nowTimestamp, makerUserId: testMaker
            ]).createOrUpdate()
        }

        when: "Request first page with size 2"
        def result = ec.service.sync().name("trade.TradeCommonServices.get#TradeTransactions")
            .parameters([pageSize: 2, pageIndex: 0, makerUserId: testMaker]).call()

        then: "Page 1 should have 2 records and correct total"
        result.transactionList.size() == 2
        result.transactionCount == 6
        result.pageIndex == 0
        result.pageSize == 2

        when: "Request second page"
        def res2 = ec.service.sync().name("trade.TradeCommonServices.get#TradeTransactions")
            .parameters([pageSize: 2, pageIndex: 1, makerUserId: testMaker]).call()

        then: "Page 2 should have 2 records and correct total"
        res2.transactionList.size() == 2
        res2.transactionCount == 6
        res2.pageIndex == 1
        res2.pageSize == 2

        when: "Request third page"
        def result2 = ec.service.sync().name("trade.TradeCommonServices.get#TradeTransactions")
            .parameters([pageSize: 2, pageIndex: 2, makerUserId: testMaker]).call()

        then: "Page 3 should have 2 records and correct total"
        result2.transactionList.size() == 2
        result2.transactionCount == 6

        cleanup:
        ec.artifactExecution.disableAuthz()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", testInstId).updateAll([latestTransactionId: null])
        ec.entity.find("trade.TradeTransaction").condition("makerUserId", testMaker).deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", testInstId).deleteAll()
    }
}
