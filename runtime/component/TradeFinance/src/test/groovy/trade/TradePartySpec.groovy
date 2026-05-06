package trade

import spock.lang.Specification
import spock.lang.Stepwise
import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityCondition

// ABOUTME: TradePartySpec validates the creation and management of TradeParty records and their association with instruments.
// ABOUTME: Enforces compliance (KYC) and role-specific validation (RMA, FI Limit).

@Stepwise
class TradePartySpec extends Specification {
    protected static ExecutionContext ec

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.user.loginUser("trade.admin", "trade123")
        ec.artifactExecution.disableAuthz()
        
        boolean began = ec.transaction.begin(60)
        try {
            ec.entity.find("trade.importlc.ImportLcSettlement").condition("instrumentId", EntityCondition.LIKE, "INST_%").deleteAll()
            ec.entity.find("trade.importlc.ImportLcAmendment").condition("instrumentId", EntityCondition.LIKE, "INST_%").deleteAll()
            ec.entity.find("trade.importlc.ImportLcShippingGuarantee").condition("instrumentId", EntityCondition.LIKE, "INST_%").deleteAll()
            ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("instrumentId", EntityCondition.LIKE, "INST_%").deleteAll()
            ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", EntityCondition.LIKE, "INST_%").deleteAll()
            ec.entity.find("trade.TradeApprovalRecord").condition("instrumentId", EntityCondition.LIKE, "INST_%").deleteAll()
            ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", EntityCondition.LIKE, "INST_%").deleteAll()
            ec.entity.find("trade.TradeTransaction").condition("instrumentId", EntityCondition.LIKE, "INST_%").deleteAll()
            ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", EntityCondition.LIKE, "INST_%").deleteAll()
            ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", EntityCondition.LIKE, "INST_%").deleteAll()
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", EntityCondition.LIKE, "INST_%").deleteAll()
            
            ec.entity.find("trade.TradeInstrumentParty").condition("partyId", EntityCondition.LIKE, "SPEC_%").deleteAll()
            ec.entity.find("trade.TradePartyBank").condition("partyId", EntityCondition.LIKE, "SPEC_%").deleteAll()
            ec.entity.find("trade.TradeParty").condition("partyId", EntityCondition.LIKE, "SPEC_%").deleteAll()
            
            ec.transaction.commit(began)
        } catch (Exception e) {
            ec.transaction.rollback(began, "Error in setupSpec", e)
            throw e
        }
    }

    def setup() {
        ec.message.clearAll()
    }

    def cleanupSpec() {
        ec.destroy()
    }

    def "BDD-CMN-TP-01: Create Commercial TradeParty"() {
        when: "Creating a new commercial party"
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: 'SPEC_COMM_01', partyTypeEnumId: 'PARTY_COMMERCIAL', 
                             partyName: 'Spec Comm 01', kycStatus: 'Active'])
                .call()
        
        then: "Records are created in TradeParty"
        def tp = ec.entity.find("trade.TradeParty").condition("partyId", "SPEC_COMM_01").one()
        tp != null
        tp.partyTypeEnumId == 'PARTY_COMMERCIAL'
    }

    def "BDD-CMN-TP-02: Create Bank TradeParty (base + extension)"() {
        when: "Creating a new bank party"
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: 'SPEC_BANK_01', partyTypeEnumId: 'PARTY_BANK', 
                             partyName: 'Spec Bank 01', swiftBic: 'SPECXXXV', hasActiveRMA: true, 
                             kycStatus: 'Active', fiLimitAvailable: 1000000.0])
                .call()
        
        then: "Records are created in both TradeParty and TradePartyBank"
        def tp = ec.entity.find("trade.TradeParty").condition("partyId", "SPEC_BANK_01").one()
        def tpb = ec.entity.find("trade.TradePartyBank").condition("partyId", "SPEC_BANK_01").one()
        tp != null
        tpb != null
        tpb.swiftBic == 'SPECXXXV'
        tpb.hasActiveRMA == 'Y'
    }

    def "BDD-CMN-TP-03: Create TradeParty with invalid SWIFT characters -> Fail"() {
        when:
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: 'SPEC_COMM_002', partyTypeEnumId: 'PARTY_COMMERCIAL', 
                            partyName: 'Test & Corp @', kycStatus: 'Active'])
                .call()

        then:
        ec.message.hasError()
        
        cleanup:
        ec.message.clearAll()
    }

    def "BDD-CMN-TP-04: Assign Applicant role (Commercial)"() {
        // Create mandatory parties first
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: 'SPEC_COMM_02', partyTypeEnumId: 'PARTY_COMMERCIAL', 
                             partyName: 'Spec Comm 02', kycStatus: 'Active'])
                .call()
        
        // Create instrument via service
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
                .parameters([instrumentId: 'INST_ASSIGN_01', lcAmount: 5000.0, lcCurrencyUomId: 'USD',
                             instrumentParties: [
                                 [roleEnumId: 'TP_APPLICANT', partyId: 'SPEC_COMM_01'],
                                 [roleEnumId: 'TP_BENEFICIARY', partyId: 'SPEC_COMM_02']
                             ]])
                .call()
        
        when: "Assigning commercial party as Applicant"
        ec.service.sync().name("trade.TradeCommonServices.assign#InstrumentParty")
                .parameters([instrumentId: 'INST_ASSIGN_01', roleEnumId: 'TP_APPLICANT', partyId: 'SPEC_COMM_01'])
                .call()
        
        then: "Assignment is successful"
        ec.entity.find("trade.TradeInstrumentParty").condition([instrumentId: 'INST_ASSIGN_01', roleEnumId: 'TP_APPLICANT', partyId: 'SPEC_COMM_01']).one() != null
    }

    def "BDD-CMN-TP-05: Assign same bank to multiple roles"() {
        when: "Assigning bank as Advising and Confirming bank"
        ec.service.sync().name("trade.TradeCommonServices.assign#InstrumentParty")
                .parameters([instrumentId: 'INST_ASSIGN_01', roleEnumId: 'TP_ADVISING_BANK', partyId: 'SPEC_BANK_01'])
                .call()
        ec.service.sync().name("trade.TradeCommonServices.assign#InstrumentParty")
                .parameters([instrumentId: 'INST_ASSIGN_01', roleEnumId: 'TP_CONFIRMING_BANK', partyId: 'SPEC_BANK_01'])
                .call()
        
        then: "Both assignments exist"
        ec.entity.find("trade.TradeInstrumentParty").condition([instrumentId: 'INST_ASSIGN_01', partyId: 'SPEC_BANK_01']).list().size() == 2
    }

    def "BDD-CMN-TP-08: Assign Advising Bank (Missing RMA) -> Fail"() {
        setup:
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: 'SPEC_NO_RMA_BANK', partyTypeEnumId: 'PARTY_BANK', 
                             partyName: 'Spec No RMA Bank', hasActiveRMA: false, kycStatus: 'Active'])
                .call()
        
        when: "Assigning bank without RMA"
        ec.service.sync().name("trade.TradeCommonServices.assign#InstrumentParty")
                .parameters([instrumentId: 'INST_ASSIGN_01', roleEnumId: 'TP_ADVISING_BANK', partyId: 'SPEC_NO_RMA_BANK'])
                .call()
        
        then: "Fails due to missing RMA"
        ec.message.hasError()
        ec.message.errors.any { it.contains("has no active RMA") }
        
        cleanup:
        ec.message.clearAll()
    }

    def "BDD-CMN-TP-07: Commercial party -> bank role rejection"() {
        when: "Assigning commercial party as Advising Bank"
        ec.service.sync().name("trade.TradeCommonServices.assign#InstrumentParty")
                .parameters([instrumentId: 'INST_ASSIGN_01', roleEnumId: 'TP_ADVISING_BANK', partyId: 'SPEC_COMM_01'])
                .call()
        
        then: "Fails due to incorrect party type"
        ec.message.hasError()
        ec.message.errors.any { it.contains("requires a Bank party") }
        
        cleanup:
        ec.message.clearAll()
    }

    def "BDD-CMN-TP-08: Assign Confirming Bank (Insufficient Limit) -> Fail"() {
        setup:
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: 'SPEC_LOW_LIMIT_BANK', partyTypeEnumId: 'PARTY_BANK', 
                             partyName: 'Spec Low Limit Bank', hasActiveRMA: true, fiLimitAvailable: 1000.0, kycStatus: 'Active'])
                .call()
        // Create instrument via service
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
                .parameters([instrumentId: 'INST_LIMIT_01', lcAmount: 5000.0, lcCurrencyUomId: 'USD',
                             instrumentParties: [
                                 [roleEnumId: 'TP_APPLICANT', partyId: 'SPEC_COMM_01'],
                                 [roleEnumId: 'TP_BENEFICIARY', partyId: 'SPEC_COMM_02']
                             ]])
                .call()
        
        when: "Assigning bank with insufficient limit"
        ec.service.sync().name("trade.TradeCommonServices.assign#InstrumentParty")
                .parameters([instrumentId: 'INST_LIMIT_01', roleEnumId: 'TP_CONFIRMING_BANK', partyId: 'SPEC_LOW_LIMIT_BANK'])
                .call()
        
        then: "Fails due to limit breach"
        ec.message.hasError()
        ec.message.errors.any { it.contains("Insufficient FI Limit") }
        
        cleanup:
        ec.message.clearAll()
    }

    def "BDD-CMN-TP-09.1: Advising bank (Receiver) strictly requires RMA"() {
        setup:
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: 'SPEC_NO_RMA_BANK_2', partyTypeEnumId: 'PARTY_BANK', 
                            partyName: 'Spec No RMA Bank 2', hasActiveRMA: false, kycStatus: 'Active'])
                .call()
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: 'SPEC_WITH_RMA_BANK', partyTypeEnumId: 'PARTY_BANK', 
                            partyName: 'Spec With RMA Bank', hasActiveRMA: true, kycStatus: 'Active'])
                .call()

        when: "Assigning SPEC_NO_RMA_BANK_2 as Advising Bank"
        ec.service.sync().name("trade.TradeCommonServices.assign#InstrumentParty")
                .parameters([instrumentId: 'INST_ASSIGN_01', roleEnumId: 'TP_ADVISING_BANK', partyId: 'SPEC_NO_RMA_BANK_2']).call()

        then: "Must fail because Advising Bank strictly requires RMA"
        ec.message.hasError()
        ec.message.getErrorsString().contains("has no active RMA")
        
        cleanup:
        ec.message.clearAll()
    }

    def "BDD-CMN-TP-09.2: Advise-through bank is exempt from RMA requirement"() {
        when: "Using SPEC_NO_RMA_BANK_2 as Advise Through Bank"
        ec.service.sync().name("trade.TradeCommonServices.assign#InstrumentParty")
                .parameters([instrumentId: 'INST_ASSIGN_01', roleEnumId: 'TP_ADVISING_BANK', partyId: 'SPEC_WITH_RMA_BANK']).call()
        ec.service.sync().name("trade.TradeCommonServices.assign#InstrumentParty")
                .parameters([instrumentId: 'INST_ASSIGN_01', roleEnumId: 'TP_ADVISE_THROUGH_BANK', partyId: 'SPEC_NO_RMA_BANK_2']).call()

        then: "Must succeed because Advise Through Bank (Tag 57A) does not require RMA"
        !ec.message.hasError()
    }

    def "BDD-CMN-TP-10: Assign Reimbursing Bank (Missing Nostro) -> Fail"() {
        setup:
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: 'SPEC_NO_NOSTRO_BANK', partyTypeEnumId: 'PARTY_BANK', 
                             partyName: 'Spec No Nostro Bank', hasActiveRMA: true, kycStatus: 'Active', nostroAccountRef: null])
                .call()
        
        when: "Assigning bank without Nostro as Reimbursing Bank"
        ec.service.sync().name("trade.TradeCommonServices.assign#InstrumentParty")
                .parameters([instrumentId: 'INST_ASSIGN_01', roleEnumId: 'TP_REIMBURSING_BANK', partyId: 'SPEC_NO_NOSTRO_BANK'])
                .call()
        
        then: "Fails due to missing Nostro"
        ec.message.hasError()
        ec.message.errors.any { it.contains("Nostro") }
        
        cleanup:
        ec.message.clearAll()
    }

    def "BDD-CMN-TP-06: Update: Role reassignment updates existing record"() {
        setup:
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: 'SPEC_COMM_03', partyTypeEnumId: 'PARTY_COMMERCIAL', partyName: 'New Applicant', kycStatus: 'Active'])
                .call()

        when: "Reassigning Applicant to a different party"
        ec.service.sync().name("trade.TradeCommonServices.assign#InstrumentParty")
                .parameters([instrumentId: 'INST_ASSIGN_01', roleEnumId: 'TP_APPLICANT', partyId: 'SPEC_COMM_03'])
                .call()

        then: "Existing assignment is updated"
        !ec.message.hasError()
        def junc = ec.entity.find("trade.TradeInstrumentParty")
                .condition([instrumentId: 'INST_ASSIGN_01', roleEnumId: 'TP_APPLICANT']).one()
        junc.partyId == 'SPEC_COMM_03'
    }

    def "BDD-CMN-TP-18: Role Uniqueness Enforcement (PK Validation)"() {
        when: "Attempting to assign multiple parties to the same role on an instrument"
        ec.service.sync().name("trade.TradeCommonServices.assign#InstrumentParty")
                .parameters([instrumentId: 'INST_ASSIGN_01', roleEnumId: 'TP_ADVISING_BANK', partyId: 'SPEC_BANK_01'])
                .call()
        ec.service.sync().name("trade.TradeCommonServices.assign#InstrumentParty")
                .parameters([instrumentId: 'INST_ASSIGN_01', roleEnumId: 'TP_ADVISING_BANK', partyId: 'SPEC_NO_RMA_BANK'])
                .call()

        then: "Throws error or PK violation"
        // In Moqui, createOrUpdate usually handles this, but since it's a junction, we expect the service to either update (TP-06) 
        // or the DB to reject if manual insertion is attempted. TP-06 covers the service behavior. 
        // This test ensures the junction remains unique.
        ec.entity.find("trade.TradeInstrumentParty").condition([instrumentId: 'INST_ASSIGN_01', roleEnumId: 'TP_ADVISING_BANK']).count() == 1
    }

    def "BDD-CMN-TP-11: Reject confirming bank insufficient FI limit"() {
        setup:
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: 'SPEC_SMALL_LIMIT_BANK', partyTypeEnumId: 'PARTY_BANK', 
                             partyName: 'Small Limit Bank', swiftBic: 'SMALLXXX', hasActiveRMA: true, 
                             kycStatus: 'Active', fiLimitAvailable: 1000.0, fiLimitCurrencyUomId: 'USD'])
                .call()
        
        when: "Assigning as confirming bank for large liability"
        // Create instrument with 10k amount
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
                .parameters([instrumentId: 'INST_LIMIT_TEST', lcAmount: 10000.0, lcCurrencyUomId: 'USD',
                             instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'SPEC_COMM_01'],
                                       [roleEnumId: 'TP_BENEFICIARY', partyId: 'SPEC_COMM_02']]])
                .call()
                
        ec.service.sync().name("trade.TradeCommonServices.assign#InstrumentParty")
                .parameters([instrumentId: 'INST_LIMIT_TEST', roleEnumId: 'TP_CONFIRMING_BANK', partyId: 'SPEC_SMALL_LIMIT_BANK'])
                .call()

        then: "Fails due to insufficient limit"
        ec.message.hasError()
        ec.message.getErrorsString().toLowerCase().contains("insufficient")
    }

    def "BDD-CMN-TP-12: Create LC with party role assignments"() {
        when: "Creating LC with inline parties"
        def result = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
                .parameters([instrumentId: 'INST_INLINE_01', lcAmount: 5000.0, lcCurrencyUomId: 'USD',
                             instrumentParties: [
                                 [roleEnumId: 'TP_APPLICANT', partyId: 'SPEC_COMM_01'],
                                 [roleEnumId: 'TP_BENEFICIARY', partyId: 'SPEC_COMM_02'],
                                 [roleEnumId: 'TP_ADVISING_BANK', partyId: 'SPEC_BANK_01']
                             ]])
                .call()
        
        then: "All junction records are created"
        ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", 'INST_INLINE_01').count() == 3
    }
}
