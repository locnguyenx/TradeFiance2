package trade

import spock.lang.Specification
import spock.lang.Stepwise
import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityCondition

@Stepwise
class TradePartySpec extends Specification {
    protected static ExecutionContext ec

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.user.loginUser("trade.admin", "trade123")
        ec.artifactExecution.disableAuthz()
        ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", EntityCondition.LIKE, "INST_%").deleteAll()
        ec.entity.find("trade.TradePartyBank").condition("partyId", EntityCondition.LIKE, "TEST_%").deleteAll()
        ec.entity.find("trade.TradePartyBank").condition("partyId", EntityCondition.LIKE, "NO_%").deleteAll()
        ec.entity.find("trade.TradePartyBank").condition("partyId", EntityCondition.LIKE, "LOW_%").deleteAll()
        ec.entity.find("trade.TradeParty").condition("partyId", EntityCondition.LIKE, "TEST_%").deleteAll()
        ec.entity.find("trade.TradeParty").condition("partyId", EntityCondition.LIKE, "NO_%").deleteAll()
        ec.entity.find("trade.TradeParty").condition("partyId", EntityCondition.LIKE, "LOW_%").deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", EntityCondition.LIKE, "INST_%").deleteAll()
    }

    def setup() {
        ec.message.clearAll()
    }

    def cleanupSpec() {
        ec.destroy()
    }

    def "SC-01: Create Commercial TradeParty"() {
        when:
        def result = ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: 'TEST_COMM_001', partyTypeEnumId: 'PARTY_COMMERCIAL', 
                            partyName: 'Test Corp', countryOfRisk: 'USA', 
                            registeredAddress: '123 Test St', accountNumber: 'ACC123'])
                .call()

        then:
        result.partyId == 'TEST_COMM_001'
        def party = ec.entity.find("trade.TradeParty").condition("partyId", 'TEST_COMM_001').one()
        party.partyName == 'Test Corp'
        party.partyTypeEnumId == 'PARTY_COMMERCIAL'
    }

    def "SC-02: Create Bank TradeParty (base + extension)"() {
        when:
        def result = ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: 'TEST_BANK_001', partyTypeEnumId: 'PARTY_BANK', 
                            partyName: 'Test Bank', swiftBic: 'TBANKUS33', 
                            hasActiveRMA: true, nostroAccountRef: 'NOSTRO-001'])
                .call()

        then:
        result.partyId == 'TEST_BANK_001'
        def bank = ec.entity.find("trade.TradePartyBank").condition("partyId", 'TEST_BANK_001').one()
        bank.swiftBic == 'TBANKUS33'
        bank.hasActiveRMA == 'Y'
        bank.nostroAccountRef == 'NOSTRO-001'
    }

    def "SC-04: Assign Applicant role (Commercial)"() {
        setup:
        ec.entity.makeValue("trade.TradeInstrument").setAll([instrumentId: 'INST_ASSIGN_01', transactionRef: 'REF01']).create()
        
        when:
        ec.service.sync().name("trade.TradeCommonServices.assign#InstrumentParty")
                .parameters([instrumentId: 'INST_ASSIGN_01', roleEnumId: 'TP_APPLICANT', partyId: 'TEST_COMM_001'])
                .call()

        then:
        def junc = ec.entity.find("trade.TradeInstrumentParty")
                .condition([instrumentId: 'INST_ASSIGN_01', roleEnumId: 'TP_APPLICANT']).one()
        junc.partyId == 'TEST_COMM_001'
    }

    def "SC-06: Assign Advising Bank (Missing RMA) -> Fail"() {
        setup:
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: 'NO_RMA_BANK', partyTypeEnumId: 'PARTY_BANK', 
                            partyName: 'No RMA Bank', hasActiveRMA: false])
                .call()

        when:
        ec.service.sync().name("trade.TradeCommonServices.assign#InstrumentParty")
                .parameters([instrumentId: 'INST_ASSIGN_01', roleEnumId: 'TP_ADVISING_BANK', partyId: 'NO_RMA_BANK'])
                .call()

        then:
        ec.message.hasError()
        ec.message.errors.any { it.contains("has no active RMA") }
    }

    def "SC-08: Assign Confirming Bank (Insufficient Limit) -> Fail"() {
        setup:
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: 'LOW_LIMIT_BANK', partyTypeEnumId: 'PARTY_BANK', 
                            partyName: 'Low Limit Bank', hasActiveRMA: true,
                            fiLimitAvailable: 100.00])
                .call()
        ec.entity.makeValue("trade.TradeInstrument").setAll([instrumentId: 'INST_HIGH_AMT', amount: 5000.00]).create()

        when:
        ec.service.sync().name("trade.TradeCommonServices.assign#InstrumentParty")
                .parameters([instrumentId: 'INST_HIGH_AMT', roleEnumId: 'TP_CONFIRMING_BANK', partyId: 'LOW_LIMIT_BANK'])
                .call()

        then:
        ec.message.hasError()
        ec.message.errors.any { it.contains("Insufficient FI Limit") }
    }

    def "SC-10: Assign Reimbursing Bank (Missing Nostro) -> Fail"() {
        setup:
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: 'NO_NOSTRO_BANK', partyTypeEnumId: 'PARTY_BANK', 
                            partyName: 'No Nostro Bank'])
                .call()

        when:
        ec.service.sync().name("trade.TradeCommonServices.assign#InstrumentParty")
                .parameters([instrumentId: 'INST_ASSIGN_01', roleEnumId: 'TP_REIMBURSING_BANK', partyId: 'NO_NOSTRO_BANK'])
                .call()

        then:
        ec.message.hasError()
        ec.message.errors.any { it.contains("has no Nostro account configured") }
    }

    def "SC-11: Duplicate Role Assignment -> Fail"() {
        when:
        // Try assigning beneficiary again to INST_ASSIGN_01 (PK constraint should trigger)
        ec.service.sync().name("trade.TradeCommonServices.assign#InstrumentParty")
                .parameters([instrumentId: 'INST_ASSIGN_01', roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'])
                .call()

        then:
        ec.message.hasError()
    }
}
