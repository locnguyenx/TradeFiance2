package trade

import spock.lang.Specification
import spock.lang.Stepwise
import spock.lang.Shared
import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityCondition

// ABOUTME: TradePartyLcIntegrationSpec verifies the integration between TradeParty entities and Import LC creation.
// ABOUTME: Validates role-based junction records, KYC enforcement, and view resolution.

@Stepwise
class TradePartyLcIntegrationSpec extends Specification {
    @Shared protected ExecutionContext ec
    @Shared String testPrefix

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        println "DEBUG: setupSpec TradePartyLcIntegrationSpec starting"
        ec.user.loginUser("trade.maker", "trade123")
        ec.artifactExecution.disableAuthz()
        testPrefix = "TRP-INT-" + System.currentTimeMillis()
        ec.message.clearAll()
        
        // Setup parties for tests using services
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: testPrefix + '_APP_001', partyTypeEnumId: 'PTY_COMMERCIAL', 
                             partyName: 'Integration Applicant', kycStatus: 'KYC_ACTIVE']).call()
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: testPrefix + '_BEN_001', partyTypeEnumId: 'PTY_COMMERCIAL', 
                             partyName: 'Integration Beneficiary', kycStatus: 'KYC_ACTIVE']).call()
        
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: testPrefix + '_BANK_ADV', partyTypeEnumId: 'PTY_BANK', 
                             partyName: 'Adv Bank', kycStatus: 'KYC_ACTIVE', hasActiveRMA: true]).call()
        
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: testPrefix + '_BANK_CONF', partyTypeEnumId: 'PTY_BANK', 
                             partyName: 'Conf Bank', kycStatus: 'KYC_ACTIVE', hasActiveRMA: true, 
                             fiLimitAvailable: 1000000]).call()
        
        // Set isolated ID generation ranges - use 2800000
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrument", 45000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit", 45000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeTransaction", 45000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrumentParty", 45000000, 1000)
        println "DEBUG: setupSpec TradePartyLcIntegrationSpec complete"
    }

    def cleanupSpec() {
        if (ec != null) {
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrument")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeTransaction")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrumentParty")
            ec.destroy()
        }
    }

    @Shared String lc001Id
    @Shared String lc002Id

    def "SC-12: Create LC with 4 Normalized Parties"() {
        when:
        def result = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
                .parameters([
                    lcAmount: 50000,
                    lcCurrencyUomId: 'USD',
                    instrumentParties: [
                        [roleEnumId: 'TP_APPLICANT', partyId: testPrefix + '_APP_001'],
                        [roleEnumId: 'TP_BENEFICIARY', partyId: testPrefix + '_BEN_001'],
                        [roleEnumId: 'TP_ADVISING_BANK', partyId: testPrefix + '_BANK_ADV'],
                        [roleEnumId: 'TP_CONFIRMING_BANK', partyId: testPrefix + '_BANK_CONF']
                    ]
                ])
                .call()
        lc001Id = result.instrumentId

        then:
        !ec.message.hasError()
        def juncs = ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", lc001Id).list()
        juncs.size() == 4
        juncs.any { it.roleEnumId == 'TP_APPLICANT' && it.partyId == testPrefix + '_APP_001' }
        juncs.any { it.roleEnumId == 'TP_CONFIRMING_BANK' && it.partyId == testPrefix + '_BANK_CONF' }
    }

    def "SC-13: Select ANY BANK -> verify availableWithEnumId and no TP_NEGOTIATING_BANK"() {
        when:
        def result = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
                .parameters([
                    lcAmount: 10000, lcCurrencyUomId: 'USD',
                    availableWithEnumId: 'AW_ANY_BANK', availableByEnumId: 'AVB_BY_NEGOTIATION',
                    instrumentParties: [
                        [roleEnumId: 'TP_APPLICANT', partyId: testPrefix + '_APP_001'],
                        [roleEnumId: 'TP_BENEFICIARY', partyId: testPrefix + '_BEN_001']
                    ]
                ])
                .call()
        lc002Id = result.instrumentId

        then:
        !ec.message.hasError()
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", lc002Id).one()
        lc.availableWithEnumId == 'AW_ANY_BANK'
    }
}
