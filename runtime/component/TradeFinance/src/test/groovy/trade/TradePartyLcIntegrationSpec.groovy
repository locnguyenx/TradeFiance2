package trade

import spock.lang.Specification
import spock.lang.Stepwise
import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityCondition

@Stepwise
class TradePartyLcIntegrationSpec extends Specification {
    protected static ExecutionContext ec

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.user.loginUser("trade.admin", "trade123")
        ec.artifactExecution.disableAuthz()
        cleanData()
        ec.message.clearAll()
        
        // Setup parties for tests using entity calls
        ec.entity.makeValue("trade.TradeParty").setAll([partyId: 'INT_APP_001', partyTypeEnumId: 'PARTY_COMMERCIAL', partyName: 'Integration Applicant', kycStatus: 'Active']).createOrUpdate()
        ec.entity.makeValue("trade.TradeParty").setAll([partyId: 'INT_BEN_001', partyTypeEnumId: 'PARTY_COMMERCIAL', partyName: 'Integration Beneficiary', kycStatus: 'Active']).createOrUpdate()
        
        ec.entity.makeValue("trade.TradeParty").setAll([partyId: 'INT_BANK_ADV', partyTypeEnumId: 'PARTY_BANK', partyName: 'Adv Bank', kycStatus: 'Active']).createOrUpdate()
        ec.entity.makeValue("trade.TradePartyBank").setAll([partyId: 'INT_BANK_ADV', hasActiveRMA: 'Y']).createOrUpdate()
        
        ec.entity.makeValue("trade.TradeParty").setAll([partyId: 'INT_BANK_CONF', partyTypeEnumId: 'PARTY_BANK', partyName: 'Conf Bank', kycStatus: 'Active']).createOrUpdate()
        ec.entity.makeValue("trade.TradePartyBank").setAll([partyId: 'INT_BANK_CONF', hasActiveRMA: 'Y', fiLimitAvailable: 1000000]).createOrUpdate()
        
        ec.message.clearAll()
        println "SetupSpec finished, message has error: ${ec.message.hasError()}"
    }

    def cleanupSpec() {
        cleanData()
        ec.destroy()
    }

    def setup() {
        ec.message.clearAll()
    }

    private void cleanData() {
        ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", EntityCondition.LIKE, "INT_LC_%").deleteAll()
        ec.entity.find("trade.TradeInstrumentParty").condition("partyId", EntityCondition.LIKE, "INT_%").deleteAll()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", EntityCondition.LIKE, "INT_LC_%").deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", EntityCondition.LIKE, "INT_LC_%").deleteAll()
        ec.entity.find("trade.TradePartyBank").condition("partyId", EntityCondition.LIKE, "INT_%").deleteAll()
        ec.entity.find("trade.TradeParty").condition("partyId", EntityCondition.LIKE, "INT_%").deleteAll()
        ec.entity.find("mantle.party.Party").condition("partyId", EntityCondition.LIKE, "INT_%").deleteAll()
    }

    static String lc001Id
    static String lc002Id

    def "SC-12: Create LC with 4 Normalized Parties"() {
        setup:
        ec.message.clearAll()
        
        when:
        def result = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
                .parameters([
                    instrumentId: 'INT_LC_001',
                    lcAmount: 50000,
                    lcCurrencyUomId: 'USD',
                    instrumentParties: [
                        [roleEnumId: 'TP_APPLICANT', partyId: 'INT_APP_001'],
                        [roleEnumId: 'TP_BENEFICIARY', partyId: 'INT_BEN_001'],
                        [roleEnumId: 'TP_ADVISING_BANK', partyId: 'INT_BANK_ADV'],
                        [roleEnumId: 'TP_CONFIRMING_BANK', partyId: 'INT_BANK_CONF']
                    ]
                ])
                .call()
        lc001Id = result.instrumentId

        then:
        if (ec.message.hasError()) {
            println "Service Errors: " + ec.message.errorsString
        }
        !ec.message.hasError()
        def juncs = ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", lc001Id).list()
        juncs.size() == 4
        juncs.any { it.roleEnumId == 'TP_APPLICANT' && it.partyId == 'INT_APP_001' }
        juncs.any { it.roleEnumId == 'TP_CONFIRMING_BANK' && it.partyId == 'INT_BANK_CONF' }
    }

    def "SC-13: Select ANY BANK -> verify availableWithEnumId and no TP_NEGOTIATING_BANK"() {
        when:
        def result = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
                .parameters([
                    instrumentId: 'INT_LC_002', lcAmount: 10000, lcCurrencyUomId: 'USD',
                    availableWithEnumId: 'AVAIL_ANY_BANK', availableByEnumId: 'NEGOTIATION',
                    instrumentParties: [
                        [roleEnumId: 'TP_APPLICANT', partyId: 'INT_APP_001'],
                        [roleEnumId: 'TP_BENEFICIARY', partyId: 'INT_BEN_001']
                    ]
                ])
                .call()
        lc002Id = result.instrumentId

        then:
        !ec.message.hasError()
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", lc002Id).one()
        lc.availableWithEnumId == 'AVAIL_ANY_BANK'
        
        def junc = ec.entity.find("trade.TradeInstrumentParty")
                .condition([instrumentId: lc002Id, roleEnumId: 'TP_NEGOTIATING_BANK']).one()
        junc == null
    }

    def "SC-17: Query ImportLetterOfCreditView -> verify applicantPartyName resolved from junction"() {
        when:
        def view = ec.entity.find("trade.importlc.ImportLetterOfCreditView")
                .condition("instrumentId", lc001Id).one()

        then:
        view != null
        view.applicantPartyName == 'Integration Applicant'
        view.beneficiaryPartyName == 'Integration Beneficiary'
    }

    def "SC-14: Switch from specific bank to ANY BANK -> verify junction record removed"() {
        when: "Updating LC001 to ANY BANK"
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
                .parameters([
                    instrumentId: lc001Id,
                    availableWithEnumId: 'AVAIL_ANY_BANK',
                    instrumentParties: [
                        [roleEnumId: 'TP_APPLICANT', partyId: 'INT_APP_001'],
                        [roleEnumId: 'TP_BENEFICIARY', partyId: 'INT_BEN_001']
                    ]
                ])
                .call()

        then:
        !ec.message.hasError()
        def junc = ec.entity.find("trade.TradeInstrumentParty")
                .condition([instrumentId: lc001Id, roleEnumId: 'TP_CONFIRMING_BANK']).one()
        junc == null
    }

    def "SC-15: Submit LC missing mandatory role (no beneficiary) -> validation error"() {
        when:
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
                .parameters([
                    lcAmount: 5000, lcCurrencyUomId: 'USD',
                    instrumentParties: [
                        [roleEnumId: 'TP_APPLICANT', partyId: 'INT_APP_001']
                    ]
                ])
                .call()

        then:
        ec.message.hasError()
        ec.message.errors.any { it.contains("Beneficiary are mandatory") }
    }

    def "SC-16: Submit LC with expired KYC on advising bank -> validation error"() {
        setup:
        def expiredDate = new java.sql.Date(System.currentTimeMillis() - 86400000)
        ec.entity.makeValue("trade.TradeParty").setAll([
            partyId: 'EXPIRED_BANK', partyTypeEnumId: 'PARTY_BANK', 
            partyName: 'Expired KYC Bank', kycStatus: 'Active', kycExpiryDate: expiredDate
        ]).createOrUpdate()
        ec.entity.makeValue("trade.TradePartyBank").setAll([partyId: 'EXPIRED_BANK', hasActiveRMA: 'Y']).createOrUpdate()

        when:
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
                .parameters([
                    lcAmount: 5000, lcCurrencyUomId: 'USD',
                    instrumentParties: [
                        [roleEnumId: 'TP_APPLICANT', partyId: 'INT_APP_001'],
                        [roleEnumId: 'TP_BENEFICIARY', partyId: 'INT_BEN_001'],
                        [roleEnumId: 'TP_ADVISING_BANK', partyId: 'EXPIRED_BANK']
                    ]
                ])
                .call()

        then:
        ec.message.hasError()
        ec.message.errors.any { it.contains("KYC has expired") }
    }
}
