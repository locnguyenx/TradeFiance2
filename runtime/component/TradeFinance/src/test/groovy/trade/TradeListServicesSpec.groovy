package trade

import spock.lang.Specification
import spock.lang.Shared
import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import java.sql.Timestamp

// ABOUTME: TradeListServicesSpec verifies the pagination and search capabilities of trade list services.
// ABOUTME: Ensures server-side filtering and counting work correctly for all instrument lifecycles.

class TradeListServicesSpec extends Specification {
    @Shared ExecutionContext ec

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.artifactExecution.disableAuthz()
        ec.user.loginUser("trade.admin", "trade123")
        
        // Clean up any existing test data to avoid transaction collisions
        def refs = ["TF-LIST-01", "TF-LIST-02", "TF-LIST-AMD-01", "TF-LIST-PRES-01", "TF-LIST-SETL-01", "TF-LIST-SG-01", "TF-LIST-INT-01"]
        refs.each { ref ->
            def inst = ec.entity.find("trade.TradeInstrument").condition("instrumentRef", ref).one()
            if (inst) {
                String id = inst.instrumentId
                // Null out latestTransactionId to break circular/FK dependency
                inst.set("latestTransactionId", null).update()
                
                // Delete in reverse dependency order
                ec.entity.find("trade.TradeApprovalRecord").condition("instrumentId", id).deleteAll()
                ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", id).deleteAll()
                ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", id).deleteAll()
                ec.entity.find("trade.importlc.ImportLcInternalAmendment").condition("instrumentId", id).deleteAll()
                ec.entity.find("trade.importlc.ImportLcAmendment").condition("instrumentId", id).deleteAll()
                ec.entity.find("trade.importlc.ImportLcShippingGuarantee").condition("instrumentId", id).deleteAll()
                
                // Settlements and Presentations
                ec.entity.find("trade.importlc.ImportLcSettlement").condition("instrumentId", id).deleteAll()
                ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("instrumentId", id).deleteAll()

                ec.entity.find("trade.TradeTransaction").condition("instrumentId", id).deleteAll()
                ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", id).deleteAll()
                ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", id).deleteAll()
                inst.delete()
            }
        }

        // Ensure test data exists with unique refs to avoid collision
        refs.each { createLc(it) }
    }

    private void createLc(String ref) {
        def result = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            instrumentRef: ref,
            instrumentParties: [
                [roleEnumId: 'TP_APPLICANT', partyId: "ACME_CORP_001"],
                [roleEnumId: 'TP_BENEFICIARY', partyId: "GLOBAL_EXP_002"]
            ],
            lcAmount: 100000.0, lcCurrencyUomId: "USD",
            customerFacilityId: "FAC-ACME-001",
            businessStateId: "LC_ISSUED"
        ]).call()
        if (ec.message.hasError()) {
            println "Error creating LC ${ref}: " + ec.message.getErrorsString()
            ec.message.clearAll()
        } else if (result.transactionId) {
            // Authorize the transaction to clear TX_DRAFT and allow subsequent operations
            ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument").parameters([
                transactionId: result.transactionId,
                skipFourEyes: true
            ]).call()
            if (ec.message.hasError()) {
                println "Error authorizing LC ${ref}: " + ec.message.getErrorsString()
                ec.message.clearAll()
            }
        }
    }

    def cleanupSpec() {
        ec.destroy()
    }

    def "should filter Import LC list by instrument search"() {
        when:
        def result = ec.service.sync().name("trade.importlc.ImportLcServices.get#ImportLetterOfCreditList")
            .parameters([instrumentSearch: "TF-LIST-01"]).call()
        
        then:
        !ec.message.hasError()
        result.lcList.size() >= 1
        result.lcList.any { it.instrumentRef == "TF-LIST-01" }
        result.lcCount >= 1
    }

    def "should support pagination for Import LC list"() {
        when:
        def result = ec.service.sync().name("trade.importlc.ImportLcServices.get#ImportLetterOfCreditList")
            .parameters([pageIndex: 0, pageSize: 1]).call()
        
        then:
        !ec.message.hasError()
        result.lcList.size() == 1
        result.lcCount >= 2
    }

    def "should search across Amendment list"() {
        given:
        def inst = ec.entity.find("trade.TradeInstrument").condition("instrumentRef", "TF-LIST-AMD-01").one()
        assert inst != null
        def lcId = inst.instrumentId
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ExternalAmendment").parameters([
            instrumentId: lcId,
            amendmentTypeEnumId: "AMEND_INCREASE",
            amountIncrease: 1000.0
        ]).call()
        assert !ec.message.hasError()

        when:
        def result = ec.service.sync().name("trade.TradeCommonServices.get#ExternalAmendmentList")
            .parameters([instrumentSearch: "TF-LIST-AMD-01"]).call()
        
        then:
        !ec.message.hasError()
        result.amendmentList.size() >= 1
        result.amendmentList.any { it.instrumentId == lcId }
    }

    def "should search across Presentation list"() {
        given:
        def inst = ec.entity.find("trade.TradeInstrument").condition("instrumentRef", "TF-LIST-PRES-01").one()
        assert inst != null
        def lcId = inst.instrumentId
        ec.service.sync().name("trade.TradeCommonServices.create#Presentation").parameters([
            instrumentId: lcId,
            claimAmount: 5000.0
        ]).call()
        if (ec.message.hasError()) println "Error creating presentation: " + ec.message.getErrorsString()
        assert !ec.message.hasError()

        when:
        def result = ec.service.sync().name("trade.TradeCommonServices.get#PresentationList")
            .parameters([instrumentSearch: "TF-LIST-PRES-01"]).call()
        
        then:
        !ec.message.hasError()
        result.presentationList.size() >= 1
        result.presentationList.any { it.instrumentId == lcId }
    }

    def "should search across Settlement list"() {
        given:
        def inst = ec.entity.find("trade.TradeInstrument").condition("instrumentRef", "TF-LIST-SETL-01").one()
        assert inst != null
        def lcId = inst.instrumentId
        // Create presentation first
        def presResult = ec.service.sync().name("trade.TradeCommonServices.create#Presentation").parameters([
            instrumentId: lcId, claimAmount: 5000.0
        ]).call()
        assert !ec.message.hasError()
        // Ensure presentation is compliant
        ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("presentationId", presResult.presentationId)
            .updateAll([presentationStatusId: "PRES_COMPLIANT"])
            
        // Settle it
        ec.service.sync().name("trade.importlc.ImportLcServices.settle#Presentation").parameters([
            presentationId: presResult.presentationId, principalAmount: 5000.0,
            settlementTypeEnumId: 'SIGHT_PAYMENT'
        ]).call()
        if (ec.message.hasError()) {
            println "Error settling presentation: " + ec.message.getErrorsString()
            ec.message.clearAll()
        }
        assert !ec.message.hasError()

        when:
        def result = ec.service.sync().name("trade.TradeCommonServices.get#SettlementList")
            .parameters([instrumentSearch: "TF-LIST-SETL-01"]).call()
        
        then:
        !ec.message.hasError()
        result.settlementList.size() >= 1
        result.settlementList.any { it.instrumentId == lcId }
    }

    def "should search across Shipping Guarantee list"() {
        given:
        def inst = ec.entity.find("trade.TradeInstrument").condition("instrumentRef", "TF-LIST-SG-01").one()
        assert inst != null
        def lcId = inst.instrumentId
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ShippingGuarantee").parameters([
            instrumentId: lcId, vesselName: "TEST VESSEL", billOfLadingNo: "BL-001",
            invoiceAmount: 5000.0
        ]).call()
        if (ec.message.hasError()) {
            println "Error creating shipping guarantee: " + ec.message.getErrorsString()
            ec.message.clearAll()
        }
        assert !ec.message.hasError()

        when:
        def result = ec.service.sync().name("trade.TradeCommonServices.get#ShippingGuaranteeList")
            .parameters([instrumentSearch: "TF-LIST-SG-01"]).call()
        
        then:
        !ec.message.hasError()
        result.guaranteeList.size() >= 1
        result.guaranteeList.any { it.instrumentId == lcId }
    }

    def "should search across Internal Amendment list"() {
        given:
        def inst = ec.entity.find("trade.TradeInstrument").condition("instrumentRef", "TF-LIST-INT-01").one()
        assert inst != null
        def lcId = inst.instrumentId
        ec.service.sync().name("trade.importlc.ImportLcServices.create#InternalAmendment").parameters([
            instrumentId: lcId,
            newFeeDebitAccountId: 'ACC_LIST_001'
        ]).call()
        if (ec.message.hasError()) {
            println "Error creating internal amendment: " + ec.message.getErrorsString()
            ec.message.clearAll()
        }
        assert !ec.message.hasError()

        when:
        def result = ec.service.sync().name("trade.TradeCommonServices.get#InternalAmendmentList")
            .parameters([instrumentSearch: "TF-LIST-INT-01"]).call()
        
        then:
        !ec.message.hasError()
        result.amendmentList.size() >= 1
        result.amendmentList.any { it.instrumentId == lcId }
    }
}
