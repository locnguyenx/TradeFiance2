package trade

import spock.lang.Specification
import spock.lang.Shared
import org.moqui.Moqui
import org.moqui.context.ExecutionContext

/**
 * ABOUTME: EndToEndImportLcSpec verifies the complete lifecycle of an Import LC from creation to SWIFT generation.
 * Covers Instrument creation, Limit updates, and MT700 block presence.
 */
class EndToEndImportLcSpec extends Specification {
    @Shared protected ExecutionContext ec
    @Shared String testPrefix
    
    @Shared String applicantId
    @Shared String beneficiaryId
    @Shared String advisingBankId

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.user.loginUser("trade.maker", "trade123")
        ec.artifactExecution.disableAuthz()
        testPrefix = "E2E-SPEC-" + System.currentTimeMillis()
        
        applicantId = testPrefix + "-APP"
        beneficiaryId = testPrefix + "-BEN"
        advisingBankId = testPrefix + "-ADV-BANK"

        // Set isolated ID generation ranges - use 98000000
        ec.entity.tempSetSequencedIdPrimary("trade.CustomerFacility", 98000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrument", 98000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit", 98000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.SwiftMessage", 98000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeTransaction", 98000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrumentParty", 98000000, 1000)

        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
            .parameters([partyId: applicantId, partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'App E2E', kycStatus: 'KYC_ACTIVE']).call()
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
            .parameters([partyId: beneficiaryId, partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'Ben E2E', kycStatus: 'KYC_ACTIVE']).call()
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
            .parameters([partyId: advisingBankId, partyTypeEnumId: 'PTY_BANK', partyName: 'Adv Bank E2E', kycStatus: 'KYC_ACTIVE', hasActiveRMA: 'Y']).call()
    }
    
    def cleanupSpec() {
        if (ec != null) {
            ec.entity.tempResetSequencedIdPrimary("trade.CustomerFacility")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrument")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.SwiftMessage")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeTransaction")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrumentParty")
            ec.destroy()
        }
    }

    def setup() {
        if (ec.transaction.isTransactionInPlace()) ec.transaction.rollback("Cleanup", null)
        ec.message.clearAll()
        ec.artifactExecution.disableAuthz()
    }
    
    def "Full Flow: Create LC -> Update Limit -> Generate SWIFT"() {
        given: "Initialize Facility"
        def facId = testPrefix + "-FAC"
        ec.entity.makeValue("trade.CustomerFacility")
            .setAll([facilityId: facId, totalApprovedLimit: 100000.0, utilizedAmount: 0.0, currencyUomId: 'USD', statusId: 'FAC_ACTIVE']).create()
        def transRef = testPrefix + "-REF-001"
            
        when: "1. Create Import LC"
        def createResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            instrumentRef: transRef,
            lcAmount: 50000.0,
            lcCurrencyUomId: "USD",
            customerFacilityId: facId,
            instrumentParties: [
                [roleEnumId: 'TP_APPLICANT', partyId: applicantId],
                [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId],
                [roleEnumId: 'TP_ADVISING_BANK', partyId: advisingBankId]
            ],
            lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_NEGOTIATION', confirmationEnumId: 'CONF_WITHOUT'
        ]).call()
        if (ec.message.hasError()) println "DEBUG: create#ImportLetterOfCredit errors: " + ec.message.errorsString
        def instrumentId = createResult?.instrumentId
        
        then: "Instrument exists"
        instrumentId != null
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one() != null
        
        when: "2. Update Limit Utilization"
        ec.service.sync().name("trade.LimitServices.update#Utilization").parameters([facilityId: facId, amountDelta: 50000.0]).call()
        
        then: "Facility utilization is updated"
        ec.entity.find("trade.CustomerFacility").condition("facilityId", facId).one().utilizedAmount == 50000.0
        
        when: "3. Generate SWIFT MT700"
        def swiftResult = ec.service.sync().name("trade.SwiftGenerationServices.generate#Mt700").parameters([instrumentId: instrumentId]).call()
        
        then: "SWIFT message is created with correct content"
        swiftResult.swiftMessageId != null
        def message = ec.entity.find("trade.importlc.SwiftMessage").condition("swiftMessageId", swiftResult.swiftMessageId).one()
        message.messageContent.contains(transRef)
        message.messageType == "MT700"
    }
}
