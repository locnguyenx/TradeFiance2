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
        ec.user.loginUser("trade.admin", "trade123")
        ec.artifactExecution.disableAuthz()
        testPrefix = "TRP-INT-" + System.currentTimeMillis()
        cleanData()
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
        
        ec.message.clearAll()
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
            ec.entity.find("trade.importlc.ImportLcSettlement").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.ImportLcAmendment").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.ImportLcShippingGuarantee").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeApprovalRecord").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeTransaction").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            
            ec.entity.find("trade.TradeInstrumentParty").condition("partyId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradePartyBank").condition("partyId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeParty").condition("partyId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            
            ec.transaction.commit(begun)
        } catch (Exception e) {
            ec.transaction.rollback(begun, "Error in cleanData", e)
        }
    }

    @Shared String lc001Id
    @Shared String lc002Id

    def "SC-12: Create LC with 4 Normalized Parties"() {
        given:
        def instrumentId = testPrefix + '_LC_001'

        when:
        def result = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
                .parameters([
                    instrumentId: instrumentId,
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
        given:
        def instrumentId = testPrefix + '_LC_002'

        when:
        def result = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
                .parameters([
                    instrumentId: instrumentId, lcAmount: 10000, lcCurrencyUomId: 'USD',
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
                    availableWithEnumId: 'AW_ANY_BANK',
                    instrumentParties: [
                        [roleEnumId: 'TP_APPLICANT', partyId: testPrefix + '_APP_001'],
                        [roleEnumId: 'TP_BENEFICIARY', partyId: testPrefix + '_BEN_001']
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
                        [roleEnumId: 'TP_APPLICANT', partyId: testPrefix + '_APP_001']
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
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: testPrefix + '_EXPIRED_BANK', partyTypeEnumId: 'PTY_BANK', 
                             partyName: 'Expired KYC Bank', kycStatus: 'KYC_ACTIVE', 
                             kycExpiryDate: expiredDate, hasActiveRMA: true]).call()

        when:
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
                .parameters([
                    lcAmount: 5000, lcCurrencyUomId: 'USD',
                    instrumentParties: [
                        [roleEnumId: 'TP_APPLICANT', partyId: testPrefix + '_APP_001'],
                        [roleEnumId: 'TP_BENEFICIARY', partyId: testPrefix + '_BEN_001'],
                        [roleEnumId: 'TP_ADVISING_BANK', partyId: testPrefix + '_EXPIRED_BANK']
                    ]
                ])
                .call()

        then:
        ec.message.hasError()
        ec.message.errors.any { it.contains("KYC has expired") }
    }
}
