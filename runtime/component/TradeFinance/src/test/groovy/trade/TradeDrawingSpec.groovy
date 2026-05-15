package trade

import spock.lang.*
import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import java.sql.Date
import java.sql.Timestamp
import org.moqui.entity.EntityCondition

/**
 * ABOUTME: TradeDrawingSpec handles complex drawing scenarios, shipping guarantees, and settlement reconciliation.
 * Consolidates Nostro reconciliation API and Shipping Guarantee limit management.
 */
@Stepwise
class TradeDrawingSpec extends Specification {
    @Shared protected ExecutionContext ec
    @Shared String testPrefix
    @Shared String facilityId
    @Shared String instrumentId
    @Shared String reconciliationId

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.transaction.runUseOrBegin(null, null) {
            ec.artifactExecution.disableAuthz()
            ec.user.loginUser("trade.admin", "trade123")
            testPrefix = "DRW-" + System.currentTimeMillis()

            // Set isolated ID generation ranges - use 95000000 (Module 3)
            ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrument", 95000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit", 95000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.TradeTransaction", 95000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrumentParty", 95000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.importlc.NostroReconciliation", 95000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLcShippingGuarantee", 95000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.CustomerFacility", 95000000, 1000)

            // Create test data
            ec.service.sync().name("trade.TradeCommonServices.create#TradeParty").parameters([
                partyId: testPrefix + "_BANK", partyName: "Reimbursing Bank",
                partyTypeEnumId: 'PTY_BANK', kycStatus: 'KYC_ACTIVE'
            ]).call()

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

            facilityId = testPrefix + "_FAC"
            ec.entity.makeValue("trade.CustomerFacility")
                .setAll([facilityId: facilityId, totalApprovedLimit: 120.0, utilizedAmount: 0.0, 
                         currencyUomId: 'USD', statusId: 'FAC_ACTIVE']).create()
        }
    }

    def cleanupSpec() {
        if (ec != null) {
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrument")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeTransaction")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrumentParty")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.NostroReconciliation")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLcShippingGuarantee")
            ec.entity.tempResetSequencedIdPrimary("trade.CustomerFacility")
            ec.destroy()
        }
    }

    def setup() {
        if (ec.transaction.isTransactionInPlace()) ec.transaction.rollback("Cleanup", null)
        ec.user.loginUser("trade.admin", "trade123")
        ec.message.clearAll()
        ec.artifactExecution.disableAuthz()
    }

    // --- SHIPPING GUARANTEE ---

    def "should enforce 110% earmarking for Shipping Guarantee"() {
        given: "An LC associated with a facility"
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId).updateAll([customerFacilityId: facilityId])
            
        when: "Create SG within limit (Invoice 100 * 110% = 110, limit 120)"
        def resultOk = ec.service.sync().name("trade.importlc.ImportLcServices.create#ShippingGuarantee").parameters([
            instrumentId: instrumentId,
            invoiceAmount: 100.0,
            liabilityMultiplierRequired: 110,
            transportDocReference: testPrefix + "-BOL-001"
        ]).call()
        
        then: "Success and utilization updated"
        !ec.message.hasError()
        resultOk.guaranteeId != null
        ec.entity.find("trade.CustomerFacility").condition("facilityId", facilityId).one().utilizedAmount == 110.0
        
        when: "Create SG exceeding limit (remaining 10, need 22)"
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ShippingGuarantee").parameters([
            instrumentId: instrumentId,
            invoiceAmount: 20.0,
            liabilityMultiplierRequired: 110
        ]).call()
        
        then: "Failure due to limit"
        ec.message.hasError()
    }

    // --- NOSTRO RECONCILIATION ---

    def "should list pending Nostro reconciliation records"() {
        given: "A pending reconciliation record"
        def recVal = ec.entity.makeValue("trade.importlc.NostroReconciliation").setAll([
            instrumentId: instrumentId,
            reimbursingBankPartyId: testPrefix + "_BANK",
            expectedCurrency: "USD",
            expectedAmount: 5000.00,
            matchStatusEnumId: "RECON_PENDING"
        ]).setSequencedIdPrimary().create()
        reconciliationId = recVal.reconciliationId

        when: "Fetching reconciliation list"
        def result = ec.service.sync().name("trade.TradeAccountingServices.get#NostroReconciliationList").call()
        def list = result.reconciliationList

        then: "The record is found"
        list != null
        list.any { it.reconciliationId == reconciliationId }
    }

    def "should manually match Nostro reconciliation record"() {
        given: "A pending reconciliation record from previous test"
        assert reconciliationId != null

        when: "Matching the record"
        ec.service.sync().name("trade.TradeAccountingServices.match#NostroReconciliation").parameters([
            reconciliationId: reconciliationId,
            nostroDebitDate: ec.user.nowTimestamp,
            nostroDebitAmount: 5000.00,
            nostroStatementRef: "STMT/MATCH/" + testPrefix,
            remarks: "Manual match verified"
        ]).call()

        then: "Status is matched"
        !ec.message.hasError()
        def rec = ec.entity.find("trade.importlc.NostroReconciliation").condition("reconciliationId", reconciliationId).one()
        rec.matchStatusEnumId == "RECON_MATCHED"
    }
}
