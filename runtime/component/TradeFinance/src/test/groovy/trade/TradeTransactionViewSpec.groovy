package trade

import spock.lang.Specification
import spock.lang.Shared
import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityCondition

// ABOUTME: TradeTransactionViewSpec verifies the hydration of business references in the unified transaction view.
// ABOUTME: Ensures consistency between list-view data sources and detail-view entities.

class TradeTransactionViewSpec extends Specification {
    @Shared protected ExecutionContext ec
    @Shared String testPrefix

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.artifactExecution.disableAuthz()
        ec.user.loginUser("trade.admin", "trade123")
        testPrefix = "TX-VW-SPEC-" + System.currentTimeMillis()

        // Set isolated ID generation ranges
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrument", 51000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit", 51000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeTransaction", 51000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeParty", 51000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrumentParty", 51000000, 1000)
    }

    def cleanupSpec() {
        if (ec != null) {
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrument")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeTransaction")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeParty")
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
    
    def "TradeTransactionView correctly hydrates instrument and transaction references"() {
        given:
        def instrumentId = testPrefix + "_INST_001"
        def instRef = testPrefix + "-REF-001"
        def transactionId = testPrefix + "_TXN_001"
        def txRef = testPrefix + "-TX-REF-001"

        ec.entity.makeValue("trade.TradeInstrument").setAll([
            instrumentId: instrumentId, instrumentRef: instRef, instrumentTypeEnumId: "IMPORT_LC"
        ]).create()
        
        ec.entity.makeValue("trade.TradeTransaction").setAll([
            transactionId: transactionId, instrumentId: instrumentId, transactionRef: txRef,
            transactionTypeEnumId: "IMP_NEW", transactionStatusId: "TX_DRAFT",
            transactionDate: ec.user.nowTimestamp, makerUserId: "trade.admin"
        ]).create()
        
        when:
        def view = ec.entity.find("trade.TradeTransactionView").condition("transactionId", transactionId).one()
        
        then:
        view != null
        view.transactionId == transactionId
        view.transactionRef == txRef
        view.instrumentId == instrumentId
        view.instrumentRef == instRef
    }

    def "get#TradeTransactions service returns correct reference fields"() {
        given:
        def instrumentId = testPrefix + "_INST_002"
        def instRef = testPrefix + "-REF-002"
        def transactionId = testPrefix + "_TXN_002"
        def txRef = testPrefix + "-TX-REF-002"

        ec.entity.makeValue("trade.TradeInstrument").setAll([
            instrumentId: instrumentId, instrumentRef: instRef, instrumentTypeEnumId: "IMPORT_LC"
        ]).create()
        
        ec.entity.makeValue("trade.TradeTransaction").setAll([
            transactionId: transactionId, instrumentId: instrumentId, transactionRef: txRef,
            transactionTypeEnumId: "IMP_NEW", transactionStatusId: "TX_DRAFT",
            transactionDate: ec.user.nowTimestamp, makerUserId: "trade.admin"
        ]).create()

        when:
        def result = ec.service.sync().name("trade.TradeCommonServices.get#TradeTransactions")
            .parameters([transactionStatusId: "TX_DRAFT", pageSize: 100]).call()
        def txn = result.transactionList.find { it.transactionId == transactionId }

        then:
        txn != null
        txn.transactionRef == txRef
        txn.instrumentRef == instRef
    }

    def "get#TradeTransaction service hydrates LC details and parties"() {
        given:
        def instrumentId = testPrefix + "_INST_003"
        def transactionId = testPrefix + "_TXN_003"
        def bankPartyId = testPrefix + "_BANK_HYD"

        ec.entity.makeValue("trade.TradeInstrument").setAll([
            instrumentId: instrumentId, instrumentRef: testPrefix + "-HYD-001",
            instrumentTypeEnumId: "IMPORT_LC", amount: 100000, currencyUomId: "USD"
        ]).create()
        
        ec.entity.makeValue("trade.importlc.ImportLetterOfCredit").setAll([
            instrumentId: instrumentId, availableWithEnumId: "AW_SPECIFIC_BANK",
            availableByEnumId: "AVB_BY_SIGHT_PAYMENT", goodsDescription: "TEST GOODS", businessStateId: "LC_DRAFT"
        ]).create()

        ec.entity.makeValue("trade.TradeParty").setAll([partyId: bankPartyId, partyName: "Hydration Bank", partyTypeEnumId: 'PTY_BANK', kycStatus: 'KYC_ACTIVE']).create()
        ec.entity.makeValue("trade.TradePartyBank").setAll([partyId: bankPartyId, swiftBic: "HYDBANKXXX", hasActiveRMA: 'Y']).create()
        ec.entity.makeValue("trade.TradeInstrumentParty").setAll([
            instrumentId: instrumentId, partyId: bankPartyId, roleEnumId: "TP_NEGOTIATING_BANK"
        ]).create()

        ec.entity.makeValue("trade.TradeTransaction").setAll([
            transactionId: transactionId, instrumentId: instrumentId, transactionRef: testPrefix + "-REF-HYD-001",
            transactionTypeEnumId: "IMP_NEW", transactionStatusId: "TX_DRAFT",
            transactionDate: ec.user.nowTimestamp, makerUserId: "trade.admin"
        ]).create()

        when:
        def result = ec.service.sync().name("trade.TradeCommonServices.get#TradeTransaction")
            .parameter("transactionId", transactionId).call()

        then:
        result.transactionId == transactionId
        result.instrument != null
        result.instrument.instrumentId == instrumentId
        result.instrument.availableWithEnumId == "AW_SPECIFIC_BANK"
        result.instrument.availableByEnumId == "AVB_BY_SIGHT_PAYMENT"
        result.instrument.goodsDescription == "TEST GOODS"
        
        result.instrument.parties != null
        def negBank = result.instrument.parties.find { it.roleEnumId == "TP_NEGOTIATING_BANK" }
        negBank != null
        negBank.partyName == "Hydration Bank"
        negBank.swiftBic == "HYDBANKXXX"
    }

    def "get#TradeTransactions search by instrument"() {
        given:
        def instrumentId = testPrefix + "_INST_004"
        def instRef = testPrefix + "-SEARCH-001"
        def transactionId = testPrefix + "_TXN_004"

        ec.entity.makeValue("trade.TradeInstrument").setAll([
            instrumentId: instrumentId, instrumentRef: instRef, instrumentTypeEnumId: "IMPORT_LC"
        ]).create()
        
        ec.entity.makeValue("trade.TradeTransaction").setAll([
            transactionId: transactionId, instrumentId: instrumentId, transactionRef: testPrefix + "-TX-SEARCH-001",
            transactionTypeEnumId: "IMP_NEW", transactionStatusId: "TX_DRAFT",
            transactionDate: ec.user.nowTimestamp, makerUserId: "trade.admin"
        ]).create()

        when:
        def result = ec.service.sync().name("trade.TradeCommonServices.get#TradeTransactions")
            .parameter("instrumentSearch", instRef).call()
        def txnByRef = result.transactionList.find { it.transactionId == transactionId }

        def result2 = ec.service.sync().name("trade.TradeCommonServices.get#TradeTransactions")
            .parameter("instrumentSearch", instrumentId).call()
        def txnById = result2.transactionList.find { it.transactionId == transactionId }

        then:
        txnByRef != null
        txnById != null
    }

    def "get#TradeTransactions search by transaction"() {
        given:
        def instrumentId = testPrefix + "_INST_005"
        def transactionId = testPrefix + "_TXN_005"
        def txRef = testPrefix + "-TX-SEARCH-002"

        ec.entity.makeValue("trade.TradeInstrument").setAll([
            instrumentId: instrumentId, instrumentRef: testPrefix + "-REF-005", instrumentTypeEnumId: "IMPORT_LC"
        ]).create()
        
        ec.entity.makeValue("trade.TradeTransaction").setAll([
            transactionId: transactionId, instrumentId: instrumentId, transactionRef: txRef,
            transactionTypeEnumId: "IMP_NEW", transactionStatusId: "TX_DRAFT",
            transactionDate: ec.user.nowTimestamp, makerUserId: "trade.admin"
        ]).create()

        when:
        def result = ec.service.sync().name("trade.TradeCommonServices.get#TradeTransactions")
            .parameter("transactionSearch", txRef).call()
        def txnByRef = result.transactionList.find { it.transactionId == transactionId }

        def result2 = ec.service.sync().name("trade.TradeCommonServices.get#TradeTransactions")
            .parameter("transactionSearch", transactionId).call()
        def txnById = result2.transactionList.find { it.transactionId == transactionId }

        then:
        txnByRef != null
        txnById != null
    }

    def "get#TradeTransactions supports pagination"() {
        given:
        def testMaker = testPrefix + "_PAG_USR"
        def testInstId = testPrefix + "_PAG_INST"
        
        ec.entity.makeValue("trade.TradeInstrument").setAll([
            instrumentId: testInstId, instrumentRef: testInstId + "-REF", instrumentTypeEnumId: "IMPORT_LC"
        ]).create()
        
        6.times { i ->
            ec.entity.makeValue("trade.TradeTransaction").setAll([
                transactionId: testPrefix + "_PAG_TX_${i}", instrumentId: testInstId, 
                transactionTypeEnumId: "IMP_NEW", transactionStatusId: "TX_DRAFT",
                transactionDate: ec.user.nowTimestamp, makerUserId: testMaker
            ]).create()
        }

        when: "Request first page with size 2"
        def result = ec.service.sync().name("trade.TradeCommonServices.get#TradeTransactions")
            .parameters([pageSize: 2, pageIndex: 0, makerUserId: testMaker]).call()

        then: "Page 1 should have 2 records and correct total"
        result.transactionList.size() == 2
        result.transactionCount >= 6
        result.pageIndex == 0
        result.pageSize == 2

        when: "Request second page"
        def res2 = ec.service.sync().name("trade.TradeCommonServices.get#TradeTransactions")
            .parameters([pageSize: 2, pageIndex: 1, makerUserId: testMaker]).call()

        then: "Page 2 should have 2 records and correct total"
        res2.transactionList.size() == 2
        res2.transactionCount >= 6
        res2.pageIndex == 1
        res2.pageSize == 2
    }
}
