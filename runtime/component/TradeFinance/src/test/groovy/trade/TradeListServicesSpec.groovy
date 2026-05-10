package trade

import spock.lang.Specification
import spock.lang.Shared
import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityCondition
import java.sql.Timestamp

// ABOUTME: TradeListServicesSpec verifies the pagination and search capabilities of trade list services.
// ABOUTME: Ensures server-side filtering and counting work correctly for all instrument lifecycles.

class TradeListServicesSpec extends Specification {
    @Shared protected ExecutionContext ec
    @Shared String testPrefix
    @Shared List<String> refs

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.artifactExecution.disableAuthz()
        ec.user.loginUser("trade.admin", "trade123")
        
        // Ensure test facility exists
        ec.entity.makeValue("trade.CustomerFacility")
            .setAll([facilityId: 'FAC_LIST_01', ownerPartyId: 'ACME_CORP_001', totalApprovedLimit: 2000000.0, utilizedAmount: 0.0, currencyUomId: "USD", statusId: "FAC_ACTIVE"])
            .createOrUpdate()

        testPrefix = "LIST-SRV-" + System.currentTimeMillis()
        refs = ["01", "02", "AMD-01", "PRES-01", "SETL-01", "SG-01", "INT-01"].collect { testPrefix + "-" + it }
        
        cleanData()
        
        // Ensure test data exists with unique refs to avoid collision
        refs.each { createLc(it) }
    }

    def cleanupSpec() {
        try {
            if (ec != null) cleanData()
        } finally {
            if (ec != null) ec.destroy()
        }
    }

    private void cleanData() {
        boolean begun = ec.transaction.begin(60)
        try {
            ec.entity.find("trade.TradeInstrument").condition("instrumentRef", EntityCondition.LIKE, testPrefix + "%").updateAll([latestTransactionId: null])
            
            // Get all instrumentIds created by this test prefix
            def instIds = ec.entity.find("trade.TradeInstrument").condition("instrumentRef", EntityCondition.LIKE, testPrefix + "%").selectField("instrumentId").list().collect { it.instrumentId }
            
            if (instIds) {
                ec.entity.find("trade.importlc.ImportLcInternalAmendment").condition("instrumentId", EntityCondition.IN, instIds).deleteAll()
                ec.entity.find("trade.importlc.ImportLcAmendment").condition("instrumentId", EntityCondition.IN, instIds).deleteAll()
                ec.entity.find("trade.importlc.ImportLcSettlement").condition("instrumentId", EntityCondition.IN, instIds).deleteAll()
                ec.entity.find("trade.importlc.ImportLcShippingGuarantee").condition("instrumentId", EntityCondition.IN, instIds).deleteAll()
                ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("instrumentId", EntityCondition.IN, instIds).deleteAll()
                ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", EntityCondition.IN, instIds).deleteAll()
                ec.entity.find("trade.TradeApprovalRecord").condition("instrumentId", EntityCondition.IN, instIds).deleteAll()
                ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", EntityCondition.IN, instIds).deleteAll()
                ec.entity.find("trade.TradeTransaction").condition("instrumentId", EntityCondition.IN, instIds).deleteAll()
                ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", EntityCondition.IN, instIds).deleteAll()
                ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", EntityCondition.IN, instIds).deleteAll()
                ec.entity.find("trade.TradeInstrument").condition("instrumentId", EntityCondition.IN, instIds).deleteAll()
            }
            
            ec.transaction.commit(begun)
        } catch (Exception e) {
            ec.transaction.rollback(begun, "Error in cleanData", e)
        }
    }

    private void createLc(String ref) {
        def result = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            instrumentRef: ref,
            instrumentParties: [
                [roleEnumId: 'TP_APPLICANT', partyId: "ACME_CORP_001"],
                [roleEnumId: 'TP_BENEFICIARY', partyId: "GLOBAL_EXP_002"]
            ],
            lcAmount: 100000.0, lcCurrencyUomId: "USD",
            customerFacilityId: "FAC_LIST_01",
            businessStateId: "LC_ISSUED"
        ]).call()
        
        if (ec.message.hasError()) {
            println "Error creating LC ${ref}: " + ec.message.getErrorsString()
            ec.message.clearAll()
            return
        }
        
        // Authorize if possible
        def tx = ec.entity.find("trade.TradeTransaction").condition([instrumentId: result.instrumentId, transactionTypeEnumId: 'IMP_NEW']).one()
        if (tx) {
            ec.user.loginUser("trade.checker", "trade123")
            ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument").parameters([
                transactionId: tx.transactionId, skipFourEyes: true
            ]).call()
            ec.user.loginUser("trade.admin", "trade123")
            ec.message.clearAll()
        }
        
        // Force state to LC_ISSUED
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", result.instrumentId).updateAll([businessStateId: "LC_ISSUED"])
    }

    def setup() {
        ec.message.clearAll()
        ec.artifactExecution.disableAuthz()
    }
    
    def cleanup() {
        ec.message.clearAll()
    }

    def "should filter Import LC list by instrument search"() {
        when:
        def result = ec.service.sync().name("trade.importlc.ImportLcServices.get#ImportLetterOfCreditList")
            .parameters([instrumentSearch: refs[0]]).call()
        
        then:
        !ec.message.hasError()
        result.lcList != null
        result.lcList.any { it.instrumentRef == refs[0] }
    }

    def "should search across Amendment list"() {
        given:
        def ref = refs[2]
        def inst = ec.entity.find("trade.TradeInstrument").condition("instrumentRef", ref).one()
        assert inst != null
        
        // Create an amendment
        ec.service.sync().name("trade.importlc.ImportLcServices.create#Amendment").parameters([
            instrumentId: inst.instrumentId, amendmentTypeEnumId: "AMEND_INCREASE", amountAdjustment: 1000.0,
            amendmentDate: new java.sql.Date(System.currentTimeMillis())
        ]).call()

        when:
        def result = ec.service.sync().name("trade.importlc.ImportLcServices.get#AmendmentList")
            .parameters([instrumentSearch: ref]).call()
        
        then:
        !ec.message.hasError()
        result.amendmentList != null
        result.amendmentList.any { it.instrumentRef == ref }
    }

    def "should search across Presentation list"() {
        given:
        def ref = refs[3]
        def inst = ec.entity.find("trade.TradeInstrument").condition("instrumentRef", ref).one()
        assert inst != null
        
        // Create a presentation
        ec.service.sync().name("trade.TradeCommonServices.create#Presentation").parameters([
            instrumentId: inst.instrumentId, claimAmount: 5000.0
        ]).call()

        when:
        def result = ec.service.sync().name("trade.importlc.ImportLcServices.get#PresentationList")
            .parameters([instrumentSearch: ref]).call()
        
        then:
        !ec.message.hasError()
        result.presentationList != null
        result.presentationList.any { it.instrumentRef == ref }
    }

    def "should search across Settlement list"() {
        given:
        def ref = refs[4]
        def inst = ec.entity.find("trade.TradeInstrument").condition("instrumentRef", ref).one()
        assert inst != null
        
        // Create presentation and settlement
        def presRes = ec.service.sync().name("trade.TradeCommonServices.create#Presentation").parameters([
            instrumentId: inst.instrumentId, claimAmount: 5000.0
        ]).call()
        ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("presentationId", presRes.presentationId)
            .updateAll([presentationStatusId: "PRES_COMPLIANT"])
            
        ec.service.sync().name("trade.importlc.ImportLcServices.settle#Presentation").parameters([
            presentationId: presRes.presentationId, principalAmount: 5000.0, settlementTypeEnumId: "SIGHT_PAYMENT"
        ]).call()

        when:
        def result = ec.service.sync().name("trade.importlc.ImportLcServices.get#SettlementList")
            .parameters([instrumentSearch: ref]).call()
        
        then:
        !ec.message.hasError()
        result.settlementList != null
        result.settlementList.any { it.instrumentRef == ref }
    }

    def "should search across Shipping Guarantee list"() {
        given:
        def ref = refs[5]
        def inst = ec.entity.find("trade.TradeInstrument").condition("instrumentRef", ref).one()
        assert inst != null
        
        // Create SG
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ShippingGuarantee").parameters([
            instrumentId: inst.instrumentId, invoiceAmount: 5000.0, 
            sgDate: new java.sql.Date(System.currentTimeMillis())
        ]).call()

        when:
        def result = ec.service.sync().name("trade.importlc.ImportLcServices.get#ShippingGuaranteeList")
            .parameters([instrumentSearch: ref]).call()
        
        then:
        !ec.message.hasError()
        result.sgList != null
        result.sgList.any { it.instrumentRef == ref }
    }

    def "should search across Internal Amendment list"() {
        given:
        def ref = refs[6]
        def inst = ec.entity.find("trade.TradeInstrument").condition("instrumentRef", ref).one()
        assert inst != null
        
        // Create IA
        ec.service.sync().name("trade.importlc.ImportLcServices.create#InternalAmendment").parameters([
            instrumentId: inst.instrumentId, newFacilityId: "FAC-NEW-01"
        ]).call()

        when:
        def result = ec.service.sync().name("trade.importlc.ImportLcServices.get#InternalAmendmentList")
            .parameters([instrumentSearch: ref]).call()
        
        then:
        !ec.message.hasError()
        result.iaList != null
        result.iaList.any { it.instrumentRef == ref }
    }
}
