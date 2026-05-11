package trade

import spock.lang.*
import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import java.sql.Date
import org.moqui.entity.EntityCondition
import spock.lang.Shared

/**
 * ABOUTME: BddImportLcModuleSpec provides backend parity for Import LC lifecycle scenarios.
 * Covers Issuance, Amendment, Shipping Guarantee, Drawing, and Cancellation.
 */
class BddImportLcModuleSpec extends Specification {
    @Shared protected ExecutionContext ec
    @Shared String testPrefix
    @Shared String facilityId

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.transaction.runUseOrBegin(null, null) {
            ec.artifactExecution.disableAuthz()
            ec.user.loginUser("trade.admin", "trade123")
            testPrefix = "BIM-" + System.currentTimeMillis()

        // Set isolated ID generation ranges - use 17000000
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrument", 17000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit", 17000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeTransaction", 17000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrumentParty", 17000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.TradeDocumentPresentation", 17000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLcAmendment", 17000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLcShippingGuarantee", 17000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.NostroReconciliation", 17000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.SwiftMessage", 17000000, 1000)
        
        // Ensure test parties exist
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
            .parameters([partyId: testPrefix + '_APP_01', partyName: 'Acme Corp', 
                         partyTypeEnumId: 'PTY_COMMERCIAL', kycStatus: 'KYC_ACTIVE']).call()
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
            .parameters([partyId: testPrefix + '_BEN_01', partyName: 'Global Exp', 
                         partyTypeEnumId: 'PTY_COMMERCIAL', kycStatus: 'KYC_ACTIVE']).call()

        // Setup unique Facility for limit tests
        facilityId = testPrefix + "_FAC"
        ec.entity.makeValue("trade.CustomerFacility")
            .setAll([facilityId: facilityId, ownerPartyId: testPrefix + '_APP_01', 
                     totalApprovedLimit: 1000000.0, utilizedAmount: 0.0, currencyUomId: "USD"]).create()
        }
    }

    def cleanupSpec() {
        if (ec != null) {
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrument")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeTransaction")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrumentParty")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.TradeDocumentPresentation")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLcAmendment")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLcShippingGuarantee")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.NostroReconciliation")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.SwiftMessage")
            ec.destroy()
        }
    }

    def setup() {
        if (ec.transaction.isTransactionInPlace()) ec.transaction.rollback("Cleanup from previous state", null)
        ec.user.loginUser("trade.admin", "trade123")
        ec.message.clearAll()
        ec.artifactExecution.disableAuthz()
    }
    private String createIssuedLc(BigDecimal amount = 50000.0) {
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: testPrefix + "_REF_" + System.nanoTime(), lcAmount: amount, lcCurrencyUomId: "USD",
                         tolerancePositive: 0.10, toleranceNegative: 0.10, customerFacilityId: facilityId,
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: testPrefix + '_APP_01'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: testPrefix + '_BEN_01']],
                         lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_NEGOTIATION', 
                         availableWithEnumId: 'AW_ANY_BANK', confirmationEnumId: 'CONF_WITHOUT']).call()
        String instrumentId = res.instrumentId
        
        def tx = ec.entity.find("trade.TradeTransaction").condition([instrumentId: instrumentId, transactionTypeEnumId: 'IMP_NEW']).one()
        ec.user.loginUser("trade.checker", "trade123")
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument")
            .parameters([transactionId: tx.transactionId, skipFourEyes: true]).call()
        ec.user.loginUser("trade.admin", "trade123")
        
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).updateAll([businessStateId: "LC_ISSUED"])
        return instrumentId
    }

    def "BDD-IMP-ISS-01: State Transition: Save to Draft"() {
        when:
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: testPrefix + "_DRAFT_REF", lcAmount: 1000.0, lcCurrencyUomId: "USD",
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: testPrefix + '_APP_01'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: testPrefix + '_BEN_01']]]).call()
        String instrumentId = res.instrumentId
            
        then:
        !ec.message.hasError()
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        lc.businessStateId == "LC_DRAFT"
    }

    def "BDD-IMP-ISS-03: State Transition: Authorize to Issued"() {
        given:
        def createRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: testPrefix + "_AUTH_REF", lcAmount: 1000.0, lcCurrencyUomId: "USD",
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: testPrefix + '_APP_01'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: testPrefix + '_BEN_01']]]).call()
        String instrumentId = createRes.instrumentId
        
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
        def instrumentId = createIssuedLc()
            
        when:
        ec.service.sync().name("trade.TradeCommonServices.create#Presentation")
            .parameters([instrumentId: instrumentId, claimAmount: 500.0]).call()
            
        then:
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        lc.businessStateId == "LC_DOC_RECEIVED"
    }

    def "BDD-IMP-FLOW-07: State Transition: Settled terminates instrument"() {
        given:
        def instrumentId = createIssuedLc(50000.0)
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
        def createRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: testPrefix + "_TOL_REF", lcAmount: 10000.0, lcCurrencyUomId: "USD", tolerancePositive: 0.10,
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: testPrefix + '_APP_01'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: testPrefix + '_BEN_01']],
                          availableWithEnumId: 'AW_ANY_BANK']).call()
        String instrumentId = createRes.instrumentId
        
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
        def createRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: testPrefix + "_LATE_REF", lcAmount: 1000.0, lcCurrencyUomId: "USD", expiryDate: yesterday,
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: testPrefix + '_APP_01'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: testPrefix + '_BEN_01']]]).call()
        String instrumentId = createRes.instrumentId
        
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
        def instrumentId = createIssuedLc(50000.0)

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
        def instrumentId = createIssuedLc()
            
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
        def instrumentId = createIssuedLc()
            
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
        def instrumentId = createIssuedLc()
        
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
