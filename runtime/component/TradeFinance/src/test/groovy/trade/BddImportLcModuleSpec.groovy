package trade

import spock.lang.*
import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import java.sql.Date
import org.moqui.entity.EntityCondition
import spock.lang.Shared

// ABOUTME: BddImportLcModuleSpec provides backend parity for Import LC lifecycle scenarios.
// ABOUTME: Covers Issuance, Amendment, Shipping Guarantee, Drawing, and Cancellation.

class BddImportLcModuleSpec extends Specification {
    @Shared protected ExecutionContext ec
    @Shared String testPrefix

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.artifactExecution.disableAuthz()
        ec.user.loginUser("trade.admin", "trade123")
        testPrefix = "BDD-IMP-" + System.currentTimeMillis()
        cleanData()
        
        // Ensure test parties exist
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
            .parameters([partyId: testPrefix + '_APP_01', partyName: 'Acme Corp', 
                         partyTypeEnumId: 'PTY_COMMERCIAL', kycStatus: 'KYC_ACTIVE']).call()
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
            .parameters([partyId: testPrefix + '_BEN_01', partyName: 'Global Exp', 
                         partyTypeEnumId: 'PTY_COMMERCIAL', kycStatus: 'KYC_ACTIVE']).call()
    }

    def cleanupSpec() {
        try {
            if (ec != null) cleanData()
        } finally {
            if (ec != null) ec.destroy()
        }
    }

    def setup() {
        ec.message.clearAll()
        ec.artifactExecution.disableAuthz()
    }

    def cleanup() {
        ec.message.clearAll()
    }

    private void cleanData() {
        boolean begun = ec.transaction.begin(60)
        try {
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").updateAll([latestTransactionId: null])
            ec.entity.find("trade.importlc.ImportLcInternalAmendment").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.ImportLcAmendment").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.ImportLcSettlement").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.ImportLcShippingGuarantee").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeApprovalRecord").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeTransaction").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            
            ec.entity.find("trade.CustomerFacility").condition("facilityId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeParty").condition("partyId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            
            ec.transaction.commit(begun)
        } catch (Exception e) {
            ec.transaction.rollback(begun, "Error in cleanData", e)
        }
    }

    private String createIssuedLc(String idSuffix, BigDecimal amount = 50000.0) {
        String instrumentId = testPrefix + idSuffix
        String ref = instrumentId + "_REF"
        
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, instrumentRef: ref, lcAmount: amount, lcCurrencyUomId: "USD",
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: testPrefix + '_APP_01'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: testPrefix + '_BEN_01']],
                         lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_NEGOTIATION', confirmationEnumId: 'CONF_WITHOUT']).call()
        
        def tx = ec.entity.find("trade.TradeTransaction").condition([instrumentId: instrumentId, transactionTypeEnumId: 'IMP_NEW']).one()
        ec.user.loginUser("trade.checker", "trade123")
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument")
            .parameters([transactionId: tx.transactionId, skipFourEyes: true]).call()
        ec.user.loginUser("trade.admin", "trade123")
        
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).updateAll([businessStateId: "LC_ISSUED"])
        return instrumentId
    }

    def "BDD-IMP-ISS-01: State Transition: Save to Draft"() {
        given:
        def instrumentId = testPrefix + "_DRAFT_01"
        
        when:
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, instrumentRef: instrumentId + "_REF", lcAmount: 1000.0, lcCurrencyUomId: "USD",
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: testPrefix + '_APP_01'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: testPrefix + '_BEN_01']]]).call()
            
        then:
        !ec.message.hasError()
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        lc.businessStateId == "LC_DRAFT"
    }

    def "BDD-IMP-ISS-03: State Transition: Authorize to Issued"() {
        given:
        def instrumentId = testPrefix + "_AUTH_01"
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, instrumentRef: instrumentId + "_REF", lcAmount: 1000.0, lcCurrencyUomId: "USD",
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: testPrefix + '_APP_01'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: testPrefix + '_BEN_01']]]).call()
        
        when:
        def tx = ec.entity.find("trade.TradeTransaction").condition([instrumentId: instrumentId, transactionTypeEnumId: 'IMP_NEW']).one()
        ec.user.loginUser("trade.checker", "trade123")
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument")
            .parameters([transactionId: tx.transactionId, skipFourEyes: true]).call()
        ec.user.loginUser("trade.admin", "trade123")
            
        then:
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        lc.businessStateId == "LC_ISSUED"
    }

    def "BDD-IMP-DRW-01: State Transition: Receive Docs"() {
        given:
        def instrumentId = createIssuedLc("_DRW_01")
            
        when:
        ec.service.sync().name("trade.TradeCommonServices.create#Presentation")
            .parameters([instrumentId: instrumentId, claimAmount: 500.0]).call()
            
        then:
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        lc.businessStateId == "LC_DOC_RECEIVED"
    }

    def "BDD-IMP-FLOW-07: State Transition: Settled terminates instrument"() {
        given:
        def instrumentId = createIssuedLc("_SETTLE_01")
        def presRes = ec.service.sync().name("trade.TradeCommonServices.create#Presentation")
            .parameters([instrumentId: instrumentId, claimAmount: 50000.0]).call()
        def presentationId = presRes.presentationId

        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).updateAll([businessStateId: "LC_ACCEPTED"])
        
        when:
        ec.service.sync().name("trade.importlc.ImportLcServices.settle#Presentation")
            .parameters([presentationId: presentationId, principalAmount: 50000.0, 
                         settlementTypeEnumId: 'SIGHT_PAYMENT']).call()
            
        then:
        !ec.message.hasError()
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        lc.businessStateId == "LC_CLOSED"
    }

    def "BDD-IMP-VAL-01: Drawn Tolerance Over-Draw Block"() {
        given:
        def instrumentId = testPrefix + "_TOL_01"
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, instrumentRef: instrumentId + "_REF", lcAmount: 10000.0, lcCurrencyUomId: "USD", tolerancePositive: 0.10,
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: testPrefix + '_APP_01'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: testPrefix + '_BEN_01']],
                          availableWithEnumId: 'AW_ANY_BANK']).call()
        
        def tx = ec.entity.find("trade.TradeTransaction").condition([instrumentId: instrumentId, transactionTypeEnumId: 'IMP_NEW']).one()
        ec.user.loginUser("trade.checker", "trade123")
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument")
            .parameters([transactionId: tx.transactionId, skipFourEyes: true]).call()
        ec.user.loginUser("trade.admin", "trade123")
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).updateAll([businessStateId: "LC_ISSUED"])
            
        when:
        ec.service.sync().name("trade.TradeCommonServices.create#Presentation")
            .parameters([instrumentId: instrumentId, claimAmount: 11500.0]).call()
            
        then:
        ec.message.hasError()
        ec.message.getErrorsString().contains("exceeds tolerance limit")
    }

    def "BDD-IMP-VAL-02: Specific Rule: Late Presentation Expiry Block"() {
        given:
        def yesterday = new Date(System.currentTimeMillis() - 86400000)
        def instrumentId = testPrefix + "_LATE_01"
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, instrumentRef: instrumentId + "_REF", lcAmount: 1000.0, lcCurrencyUomId: "USD", expiryDate: yesterday,
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: testPrefix + '_APP_01'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: testPrefix + '_BEN_01']]]).call()
        
        def tx = ec.entity.find("trade.TradeTransaction").condition([instrumentId: instrumentId, transactionTypeEnumId: 'IMP_NEW']).one()
        ec.user.loginUser("trade.checker", "trade123")
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument")
            .parameters([transactionId: tx.transactionId, skipFourEyes: true]).call()
        ec.user.loginUser("trade.admin", "trade123")
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).updateAll([businessStateId: "LC_ISSUED"])
            
        when:
        ec.service.sync().name("trade.TradeCommonServices.create#Presentation")
            .parameters([instrumentId: instrumentId, claimAmount: 500.0]).call()
        
        then:
        ec.message.hasError()
        ec.message.getErrorsString().contains("after LC expiry date")
    }

    def "BDD-IMP-AMD-05: Amendment: Effective values update after Beneficiary Consent"() {
        given:
        def instrumentId = createIssuedLc("_AMD_05")

        when: "A financial amendment of +20000 is created and authorized"
        def amdRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#Amendment")
            .parameters([instrumentId: instrumentId, amendmentTypeEnumId: "AMEND_INCREASE", isFinancial: "Y", amountAdjustment: 20000.0, amendmentDate: new Date(System.currentTimeMillis())]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.authorize#Amendment")
            .parameters([amendmentId: amdRes.amendmentId, skipFourEyes: true]).call()

        then: "Effective values are NOT yet updated"
        def lcBefore = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        lcBefore.effectiveAmount == 50000.0

        when: "Beneficiary Consent is logged as ACCEPTED"
        ec.service.sync().name("trade.importlc.ImportLcServices.accept#Amendment")
            .parameters([amendmentId: amdRes.amendmentId]).call()

        then: "Effective values are updated"
        def lcAfter = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        lcAfter.effectiveAmount == 70000.0
    }

    def "BDD-IMP-SG-01: Shipping Guarantee Issuance with multiplier"() {
        given:
        def instrumentId = createIssuedLc("_SG_01")
            
        when:
        def sgRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#ShippingGuarantee")
            .parameters([instrumentId: instrumentId, invoiceAmount: 1000.0, liabilityMultiplierRequired: 110.0]).call()
            
        then:
        !ec.message.hasError()
        def sg = ec.entity.find("trade.importlc.ImportLcShippingGuarantee").condition("guaranteeId", sgRes.guaranteeId).one()
        sg.earmarkAmount == 1100.0
    }

    def "BDD-IMP-CAN-01: LC Cancellation"() {
        given:
        def instrumentId = createIssuedLc("_CAN_01")
            
        when:
        def cancelRes = ec.service.sync().name("trade.TradeCommonServices.create#Cancellation")
            .parameters([instrumentId: instrumentId, cancellationReason: "User Request"]).call()
        ec.user.loginUser("trade.checker", "trade123")
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument")
            .parameters([transactionId: cancelRes.transactionId, skipFourEyes: true]).call()
        ec.user.loginUser("trade.admin", "trade123")
            
        then:
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        lc.businessStateId == "LC_CANCELLED"
    }

    @Unroll
    def "BDD-IMP-SWT-02/03/04: MT700 Tag Formats (#tag)"() {
        given:
        def instrumentId = createIssuedLc("_TAG_" + tag)
        
        when:
        def result = ec.service.sync().name("trade.SwiftGenerationServices.format#Tag")
            .parameters([tag: tag, instrumentId: instrumentId]).call()
        
        then:
        result.swText == expected
        
        where:
        tag   | expected
        "32B" | "USD50000,00"
        "39A" | "10/10"
        "59"  | "/" + testPrefix + "_BEN_01"
    }
}
