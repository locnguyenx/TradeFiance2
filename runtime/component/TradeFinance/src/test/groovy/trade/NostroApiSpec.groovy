package trade

import spock.lang.Specification
import spock.lang.Shared
import org.moqui.Moqui
import org.moqui.context.ExecutionContext

class NostroApiSpec extends Specification {
    @Shared ExecutionContext ec
    @Shared String testId

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.user.loginUser("john.doe", "moqui")
        ec.artifactExecution.disableAuthz()
        testId = System.currentTimeMillis().toString()
        // Create parent records
        ec.service.sync().name("create#trade.TradeInstrument").parameters([
            instrumentId: "INST_" + testId, instrumentRef: "ITEST" + testId,
            instrumentTypeEnumId: "IMPORT_LC", amount: 10000.00,
            currencyUomId: "USD", issueDate: new java.sql.Date(System.currentTimeMillis()),
            expiryDate: java.sql.Date.valueOf("2026-12-31")
        ]).call()
        ec.service.sync().name("create#trade.importlc.ImportLetterOfCredit").parameters([
            instrumentId: "INST_" + testId, businessStateId: "LC_ISSUED"
        ]).call()

        // Create a test reconciliation record
        ec.entity.makeValue("trade.importlc.NostroReconciliation").setAll([
            reconciliationId: "TEST_REC_" + testId,
            instrumentId: "INST_" + testId,
            reimbursingBankPartyId: "RBANK_" + testId,
            expectedCurrency: "USD",
            expectedAmount: 5000.00,
            matchStatusEnumId: "RECON_PENDING"
        ]).create()
    }

    def cleanupSpec() {
        ec.artifactExecution.disableAuthz()
        ec.entity.find("trade.importlc.NostroReconciliation").condition("instrumentId", "INST_" + testId).deleteAll()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", "INST_" + testId).deleteAll()
        ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", "INST_" + testId).deleteAll()
        ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", "INST_" + testId).deleteAll()
        ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", "INST_" + testId).deleteAll()
        ec.entity.find("trade.TradeApprovalRecord").condition("instrumentId", "INST_" + testId).deleteAll()
        ec.entity.find("trade.TradeTransaction").condition("instrumentId", "INST_" + testId).deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", "INST_" + testId).deleteAll()
        ec?.destroy()
    }

    def "get#NostroReconciliationList returns records"() {
        when:
        def result = ec.service.sync().name("trade.TradeAccountingServices.get#NostroReconciliationList").call()
        def list = result.reconciliationList

        then:
        list != null
        list.any { it.reconciliationId == "TEST_REC_" + testId }
    }

    def "match#NostroReconciliation updates record status"() {
        when:
        ec.service.sync().name("trade.TradeAccountingServices.match#NostroReconciliation").parameters([
            reconciliationId: "TEST_REC_" + testId,
            nostroDebitDate: new java.sql.Date(System.currentTimeMillis()),
            nostroDebitAmount: 5000.00,
            nostroStatementRef: "STMT/TEST/" + testId,
            remarks: "Manual match verified"
        ]).call()

        def rec = ec.entity.find("trade.importlc.NostroReconciliation").condition("reconciliationId", "TEST_REC_" + testId).one()

        then:
        rec.matchStatusEnumId == "RECON_MATCHED"
        rec.nostroStatementRef == "STMT/TEST/" + testId
        rec.matchedByUserId == "EX_JOHN_DOE"
    }
}
