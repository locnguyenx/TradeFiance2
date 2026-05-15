package trade

import spock.lang.*
import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import java.sql.Date
import java.sql.Timestamp
import org.moqui.entity.EntityCondition

/**
 * ABOUTME: ImportLcLifecycleSpec provides comprehensive coverage of the Import Letter of Credit lifecycle.
 * Consolidates BDD scenarios, service logic validation, and bug regression tests for issuance, 
 * amendment, drawing, and cancellation.
 */
@Stepwise
class ImportLcLifecycleSpec extends Specification {
    @Shared protected ExecutionContext ec
    @Shared String testPrefix
    @Shared String facilityId
    
    @Shared String applicantId
    @Shared String beneficiaryId

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.transaction.runUseOrBegin(null, null) {
            ec.artifactExecution.disableAuthz()
            ec.user.loginUser("trade.admin", "trade123")
            testPrefix = "IMP-LC-" + System.currentTimeMillis()

            // Set isolated ID generation ranges - use 93100000 (Module 2)
            ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrument", 93100000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit", 93100000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.TradeTransaction", 93100000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrumentParty", 93100000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLcAmendment", 93100000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLcInternalAmendment", 93100000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.importlc.TradeDocumentPresentation", 93100000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLcShippingGuarantee", 93100000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.importlc.NostroReconciliation", 93100000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.importlc.SwiftMessage", 93100000, 1000)

            applicantId = testPrefix + "_APP_01"
            beneficiaryId = testPrefix + "_BEN_01"
            facilityId = testPrefix + "_FAC_01"

            // Ensure test parties exist
            ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: applicantId, partyTypeEnumId: 'PTY_COMMERCIAL', 
                             partyName: 'Module 2 Applicant', kycStatus: 'KYC_ACTIVE']).call()
            ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: beneficiaryId, partyTypeEnumId: 'PTY_COMMERCIAL', 
                             partyName: 'Module 2 Beneficiary', kycStatus: 'KYC_ACTIVE']).call()

            // Setup facility
            ec.entity.makeValue("trade.CustomerFacility")
                .setAll([facilityId: facilityId, ownerPartyId: applicantId, 
                         totalApprovedLimit: 2000000.0, utilizedAmount: 0.0, 
                         currencyUomId: "USD", statusId: "FAC_ACTIVE"]).create()
        }
    }

    def cleanupSpec() {
        if (ec != null) {
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrument")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeTransaction")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrumentParty")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLcAmendment")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLcInternalAmendment")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.TradeDocumentPresentation")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLcShippingGuarantee")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.NostroReconciliation")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.SwiftMessage")
            ec.destroy()
        }
    }

    def setup() {
        if (ec.transaction.isTransactionInPlace()) ec.transaction.rollback("Cleanup", null)
        ec.user.loginUser("trade.admin", "trade123")
        ec.message.clearAll()
        ec.artifactExecution.disableAuthz()
    }

    private String createIssuedLc(BigDecimal amount = 50000.0) {
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: testPrefix + "_REF_" + System.nanoTime(), lcAmount: amount, lcCurrencyUomId: "USD",
                         tolerancePositive: 0.10, toleranceNegative: 0.10, customerFacilityId: facilityId,
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]],
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

    // --- ISSUANCE SCENARIOS ---

    def "should create a draft Import LC and verify dashboard visibility"() {
        given: "Standard LC parameters"
        def ref = testPrefix + "-DRAFT-01"
        def params = [
            instrumentRef: ref, lcAmount: 100000.0, lcCurrencyUomId: "USD",
            instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId], [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]],
            lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_NEGOTIATION', confirmationEnumId: 'CONF_WITHOUT'
        ]

        when: "Calling create#ImportLetterOfCredit"
        def result = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters(params).call()
        def instrumentId = result.instrumentId

        then: "LC is created in DRAFT status"
        !ec.message.hasError()
        instrumentId != null
        def lcLookup = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        lcLookup.businessStateId == "LC_DRAFT"

        when: "Fetching LC list for dashboard"
        def listResult = ec.service.sync().name("trade.importlc.ImportLcServices.get#ImportLetterOfCreditList")
            .parameters([instrumentId: instrumentId]).call()
        def found = listResult.lcList.find { it.instrumentId == instrumentId }
        
        then: "Draft LC should be in the list"
        found != null
        found.instrumentRef == ref
    }

    def "should transition Import LC to ISSUED upon approval"() {
        given:
        def createRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            instrumentRef: testPrefix + "_AUTH_REF",
            instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId], [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]],
            lcAmount: 200000.0, lcCurrencyUomId: "USD",
            lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_NEGOTIATION', confirmationEnumId: 'CONF_WITHOUT',
            businessStateId: "LC_DRAFT"
        ]).call()
        def instrumentId = createRes.instrumentId

        when:
        def tx = ec.entity.find("trade.TradeTransaction").condition([instrumentId: instrumentId, transactionTypeEnumId: 'IMP_NEW']).one()
        ec.user.loginUser("trade.checker", "trade123")
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument")
            .parameters([transactionId: tx.transactionId, skipFourEyes: true]).call()
        ec.user.loginUser("trade.admin", "trade123")

        then:
        !ec.message.hasError()
        def lcLookup = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        lcLookup.businessStateId == "LC_ISSUED"
    }

    // --- AMENDMENT SCENARIOS ---

    def "should create and authorize External Amendment (Scenario 1)"() {
        given:
        def instrumentId = createIssuedLc(50000.0)

        when: "A financial amendment of +20000 is created and authorized"
        def amdRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#ExternalAmendment")
            .parameters([instrumentId: instrumentId, amendmentTypeEnumId: "AMEND_INCREASE", amountIncrease: 20000.0, amendmentDate: new Date(System.currentTimeMillis())]).call()
        def amendmentId = amdRes.amendmentId
        
        ec.user.loginUser("trade.checker", "trade123")
        ec.service.sync().name("trade.importlc.ImportLcServices.authorize#Amendment")
            .parameters([amendmentId: amendmentId, approverUserId: 'trade.checker', skipFourEyes: true]).call()
        ec.user.loginUser("trade.admin", "trade123")

        then: "Effective values are NOT yet updated until consent"
        def lcBefore = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        lcBefore.effectiveAmount == 50000.0

        when: "Beneficiary Consent is logged as ACCEPTED"
        ec.service.sync().name("trade.importlc.ImportLcServices.accept#Amendment")
            .parameters([amendmentId: amendmentId]).call()

        then: "Effective values are updated"
        def lcAfter = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        lcAfter.effectiveAmount == 70000.0
    }

    def "should authorize Internal Amendment and immediately update Master LC (Scenario 3)"() {
        given:
        def instrumentId = createIssuedLc(50000.0)
        def newAcc = testPrefix + '_ACC_INT_001'

        when: "An internal amendment is created and authorized"
        def iaResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#InternalAmendment").parameters([
            instrumentId: instrumentId, newFeeDebitAccountId: newAcc, newFacilityId: facilityId
        ]).call()
        def internalAmendmentId = iaResult.internalAmendmentId

        ec.service.sync().name("trade.importlc.ImportLcServices.authorize#Amendment")
            .parameters([amendmentId: internalAmendmentId, approverUserId: 'trade.checker', skipFourEyes: true]).call()

        then: "Master LC is updated immediately without beneficiary consent"
        !ec.message.hasError()
        def lcLookup = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        lcLookup.feeDebitAccountId == newAcc
    }

    // --- DRAWING AND SETTLEMENT ---

    def "should transition to DOC_RECEIVED on presentation and then to CLOSED on settlement"() {
        given:
        def instrumentId = createIssuedLc(50000.0)
            
        when: "Presentation is received"
        def presRes = ec.service.sync().name("trade.TradeCommonServices.create#Presentation")
            .parameters([instrumentId: instrumentId, claimAmount: 50000.0]).call()
        def presentationId = presRes.presentationId
            
        then: "State is DOC_RECEIVED"
        def lcDoc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        lcDoc.businessStateId == "LC_DOC_RECEIVED"

        when: "Settle the presentation"
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).updateAll([businessStateId: "LC_ACCEPTED"])
        ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("presentationId", presentationId).updateAll([presentationStatusId: "PRES_COMPLIANT"])
        
        ec.service.sync().name("trade.importlc.ImportLcServices.settle#Presentation")
            .parameters([presentationId: presentationId, principalAmount: 50000.0, 
                         settlementTypeEnumId: 'SIGHT_PAYMENT']).call()
            
        then: "Instrument is closed"
        !ec.message.hasError()
        def lcClosed = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        lcClosed.businessStateId == "LC_CLOSED"
        lcClosed.effectiveOutstandingAmount == 0.0
    }

    // --- VALIDATIONS AND CONSTRAINTS ---

    def "should block drawing exceeding tolerance"() {
        given:
        def instrumentId = createIssuedLc(10000.0) // Tolerance is 10%
            
        when: "Drawing exceeds tolerance (11000)"
        ec.service.sync().name("trade.TradeCommonServices.create#Presentation")
            .parameters([instrumentId: instrumentId, claimAmount: 11500.0]).call()
            
        then:
        ec.message.hasError()
        ec.message.getErrorsString().contains("exceeds tolerance limit")
    }

    def "should block late presentation after LC expiry"() {
        given:
        def yesterday = new Date(System.currentTimeMillis() - 86400000)
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: testPrefix + "_EXP_REF", lcAmount: 1000.0, lcCurrencyUomId: "USD", expiryDate: yesterday,
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]]]).call()
        String instrumentId = res.instrumentId
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).updateAll([businessStateId: "LC_ISSUED"])
            
        when:
        ec.service.sync().name("trade.TradeCommonServices.create#Presentation")
            .parameters([instrumentId: instrumentId, claimAmount: 500.0]).call()
        
        then:
        ec.message.hasError()
        ec.message.getErrorsString().contains("after LC expiry date")
    }

    // --- BUG REGRESSIONS ---

    def "should NOT allow duplicate issuance transactions"() {
        given: "An issued LC"
        def instrumentId = createIssuedLc()
        
        when: "Maker attempts to update LC which would trigger re-issuance guard"
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, lcAmount: 60000.0]).call()
        
        then: "Fails because LC is already issued"
        ec.message.hasError()
        ec.message.getErrorsString().contains("Issued LC")
    }

    def "should NOT allow concurrent in-progress transactions"() {
        given: "A draft LC"
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            instrumentRef: testPrefix + "_CONC_REF",
            lcAmount: 75000.0, lcCurrencyUomId: 'USD',
            instrumentParties: [[partyId: applicantId, roleEnumId: 'TP_APPLICANT'], [partyId: beneficiaryId, roleEnumId: 'TP_BENEFICIARY']]
        ]).call()
        def instrumentId = res.instrumentId

        when: "Creating a second transaction while the first is TX_DRAFT"
        ec.service.sync().name("create#trade.TradeTransaction").requireNewTransaction(true).parameters([
            instrumentId: instrumentId, transactionRef: instrumentId + "-TX2",
            transactionTypeEnumId: "IMP_NEW", transactionStatusId: "TX_DRAFT"
        ]).call()
        
        then: "Fails due to concurrent transaction guard"
        ec.message.hasError()
    }

    // --- MISC / REMAINING ---

    def "should handle Shipping Guarantee with liability multiplier"() {
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

    def "should successfully cancel an issued LC"() {
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
    def "should format SWIFT MT700 tags correctly: #tag"() {
        given:
        def instrumentId = createIssuedLc(50000.0)
        
        when:
        def result = ec.service.sync().name("trade.SwiftGenerationServices.format#Tag")
            .parameters([tag: tag, instrumentId: instrumentId]).call()
        
        then:
        result.swText == expected
        
        where:
        tag   | expected
        "32B" | "USD50000,00"
        "39A" | "10/10"
        "59"  | "/" + applicantId.replace("_APP_01", "_BEN_01") // Matching test party in setupSpec
    }
}
