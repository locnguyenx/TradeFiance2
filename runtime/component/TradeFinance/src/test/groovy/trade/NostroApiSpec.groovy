package trade

import spock.lang.Specification
import spock.lang.Shared
import org.moqui.Moqui
import org.moqui.context.ExecutionContext

/**
 * ABOUTME: NostroApiSpec verifies the Nostro reconciliation API and manual matching flow.
 */
class NostroApiSpec extends Specification {
    @Shared ExecutionContext ec
    @Shared String testPrefix
    @Shared String reconciliationId
    @Shared String instrumentId

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.transaction.runUseOrBegin(null, null) {
            ec.user.loginUser("trade.maker", "trade123")
            ec.artifactExecution.disableAuthz()
            testPrefix = "NOS-" + System.currentTimeMillis()

        // Set isolated ID generation ranges - use 10400000
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrument", 10400000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit", 10400000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.NostroReconciliation", 10400000, 1000)

        // Create parent records
        def instRes = ec.service.sync().name("create#trade.TradeInstrument").parameters([
            instrumentRef: testPrefix + "-REF",
            instrumentTypeEnumId: "IMPORT_LC", amount: 10000.00,
            currencyUomId: "USD", issueDate: ec.user.nowTimestamp,
            expiryDate: java.sql.Date.valueOf("2026-12-31")
        ]).call()
        instrumentId = instRes.instrumentId

        ec.service.sync().name("create#trade.importlc.ImportLetterOfCredit").parameters([
            instrumentId: instrumentId, businessStateId: "LC_ISSUED"
        ]).call()

        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty").parameters([
            partyId: testPrefix + "_RBANK", partyName: "Reimbursing Bank",
            partyTypeEnumId: 'PTY_BANK', kycStatus: 'KYC_ACTIVE'
        ]).call()

        // Create a test reconciliation record
        def recVal = ec.entity.makeValue("trade.importlc.NostroReconciliation").setAll([
            instrumentId: instrumentId,
            reimbursingBankPartyId: testPrefix + "_RBANK",
            expectedCurrency: "USD",
            expectedAmount: 5000.00,
            matchStatusEnumId: "RECON_PENDING"
        ]).setSequencedIdPrimary().create()
        reconciliationId = recVal.reconciliationId
        }
    }

    def cleanupSpec() {
        if (ec != null) {
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrument")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.NostroReconciliation")
            ec.destroy()
        }
    }

    def setup() {
        if (ec.transaction.isTransactionInPlace()) ec.transaction.rollback("Cleanup from previous state", null)
        ec.user.loginUser("trade.maker", "trade123")
        ec.message.clearAll()
        ec.artifactExecution.disableAuthz()
    }

    def "get#NostroReconciliationList returns records"() {
        when:
        def result = ec.service.sync().name("trade.TradeAccountingServices.get#NostroReconciliationList").call()
        def list = result.reconciliationList

        then:
        list != null
        list.any { it.reconciliationId == reconciliationId }
    }

    def "match#NostroReconciliation updates record status"() {
        when:
        ec.service.sync().name("trade.TradeAccountingServices.match#NostroReconciliation").parameters([
            reconciliationId: reconciliationId,
            nostroDebitDate: ec.user.nowTimestamp,
            nostroDebitAmount: 5000.00,
            nostroStatementRef: "STMT/TEST/" + testPrefix,
            remarks: "Manual match verified"
        ]).call()

        def rec = ec.entity.find("trade.importlc.NostroReconciliation").condition("reconciliationId", reconciliationId).one()

        then:
        rec.matchStatusEnumId == "RECON_MATCHED"
        rec.nostroStatementRef == "STMT/TEST/" + testPrefix
    }
}
