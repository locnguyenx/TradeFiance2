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
            ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", EntityCondition.LIKE, "INST_%").deleteAll()
            ec.entity.find("trade.TradePartyBank").condition("partyId", EntityCondition.LIKE, "SPEC_%").deleteAll()
            ec.entity.find("trade.TradeParty").condition("partyId", EntityCondition.LIKE, "SPEC_%").deleteAll()
            ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", EntityCondition.LIKE, "INST_%").deleteAll()
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", EntityCondition.LIKE, "INST_%").deleteAll()
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

    def "SC-01: Create Commercial TradeParty"() {
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

    def "SC-02: Create Bank TradeParty (base + extension)"() {
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

    def "SC-03: Create TradeParty with invalid SWIFT characters -> Fail"() {
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

    def "SC-04: Assign Applicant role (Commercial)"() {
        setup:
        ec.entity.makeValue("trade.TradeInstrument").setAll([instrumentId: 'INST_ASSIGN_01', instrumentTypeEnumId: 'INST_IMPORT_LC', amount: 5000.0]).create()
        ec.entity.makeValue("trade.importlc.ImportLetterOfCredit").setAll([instrumentId: 'INST_ASSIGN_01', lcAmount: 5000.0]).create()
        
        when: "Assigning commercial party as Applicant"
        ec.service.sync().name("trade.TradeCommonServices.assign#InstrumentParty")
                .parameters([instrumentId: 'INST_ASSIGN_01', roleEnumId: 'TP_APPLICANT', partyId: 'SPEC_COMM_01'])
                .call()
        
        then: "Assignment is successful"
        ec.entity.find("trade.TradeInstrumentParty").condition([instrumentId: 'INST_ASSIGN_01', roleEnumId: 'TP_APPLICANT', partyId: 'SPEC_COMM_01']).one() != null
    }

    def "SC-05: Assign same bank to multiple roles"() {
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

    def "SC-06: Assign Advising Bank (Missing RMA) -> Fail"() {
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

    def "SC-07: Commercial party -> bank role rejection"() {
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

    def "SC-08: Assign Confirming Bank (Insufficient Limit) -> Fail"() {
        setup:
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: 'SPEC_LOW_LIMIT_BANK', partyTypeEnumId: 'PARTY_BANK', 
                             partyName: 'Spec Low Limit Bank', hasActiveRMA: true, fiLimitAvailable: 1000.0, kycStatus: 'Active'])
                .call()
        // Create a dedicated instrument for this test with high amount
        ec.entity.makeValue("trade.TradeInstrument").setAll([instrumentId: 'INST_LIMIT_01', instrumentTypeEnumId: 'INST_IMPORT_LC', amount: 5000.0]).create()
        ec.entity.makeValue("trade.importlc.ImportLetterOfCredit").setAll([instrumentId: 'INST_LIMIT_01', lcAmount: 5000.0]).create()
        
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

    def "SC-09: Advising bank without RMA allowed when advise-through exists"() {
        setup:
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: 'SPEC_NO_RMA_BANK_2', partyTypeEnumId: 'PARTY_BANK', 
                            partyName: 'Spec No RMA Bank 2', hasActiveRMA: false, kycStatus: 'Active'])
                .call()
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: 'SPEC_THROUGH_BANK', partyTypeEnumId: 'PARTY_BANK', 
                            partyName: 'Spec Through Bank', hasActiveRMA: true, kycStatus: 'Active'])
                .call()

        when: "Assigning Through Bank first"
        ec.service.sync().name("trade.TradeCommonServices.assign#InstrumentParty")
                .parameters([instrumentId: 'INST_ASSIGN_01', roleEnumId: 'TP_ADVISE_THROUGH_BANK', partyId: 'SPEC_THROUGH_BANK'])
                .call()
        
        then: "Now Advising Bank without RMA should be allowed"
        def result = ec.service.sync().name("trade.TradeCommonServices.assign#InstrumentParty")
                .parameters([instrumentId: 'INST_ASSIGN_01', roleEnumId: 'TP_ADVISING_BANK', partyId: 'SPEC_NO_RMA_BANK_2'])
                .call()
        
        result != null
        !ec.message.hasError()
    }

    def "SC-10: Assign Reimbursing Bank (Missing Nostro) -> Fail"() {
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

    def "SC-11: Update: Role reassignment updates existing record"() {
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
}
