package trade

import spock.lang.Specification
import spock.lang.Shared
import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import java.sql.Date
import java.util.Calendar
import org.moqui.entity.EntityCondition

// ABOUTME: TradeFinanceHardeningSpec verifies the backend improvements made during the hardening phase.
// ABOUTME: Tests cover business date logic, fee configuration, accounting integration, and limit enforcement.

class TradeFinanceHardeningSpec extends Specification {
    @Shared protected ExecutionContext ec
    @Shared String testPrefix

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.artifactExecution.disableAuthz()
        ec.user.loginUser("trade.admin", "trade123")
        testPrefix = "HARDEN-" + System.currentTimeMillis()
        
        // Setup unique FeeConfiguration
        ec.entity.makeValue("trade.FeeConfiguration")
            .setAll([
                feeConfigurationId: testPrefix + "_FEE", feeEventEnumId: "ISSUANCE_FEE", calculationTypeEnumId: "PERCENTAGE",
                baseValue: 0.005, minFloorAmount: 100.0, statusId: "FEE_ACTIVE", 
                effectiveDate: new Date(System.currentTimeMillis() - 86400000)
            ]).create()

        // Setup unique TradeConfig
        ec.entity.makeValue("trade.common.TradeConfig")
            .setAll([configId: testPrefix + "_CONF", configKey: testPrefix + "_GL_CASH", configValue: "110101"]).create()
            
        // Setup unique Facility and Party
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
            .parameters([partyId: testPrefix + '_APP', partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'App Hardening', kycStatus: 'KYC_ACTIVE']).call()
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
            .parameters([partyId: testPrefix + '_BEN', partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'Ben Hardening', kycStatus: 'KYC_ACTIVE']).call()
            
        ec.entity.makeValue("trade.CustomerFacility")
            .setAll([
                facilityId: testPrefix + "_FAC", ownerPartyId: testPrefix + '_APP', 
                totalApprovedLimit: 1000000.0, utilizedAmount: 0.0, currencyUomId: "USD", statusId: "FAC_ACTIVE"
            ]).create()
            
        cleanData()
    }

    def cleanupSpec() {
        try {
            if (ec != null) {
                cleanData()
                ec.entity.find("trade.CustomerFacility").condition("facilityId", testPrefix + "_FAC").deleteAll()
                ec.entity.find("trade.common.TradeConfig").condition("configKey", testPrefix + "_GL_CASH").deleteAll()
                ec.entity.find("trade.FeeConfiguration").condition("feeConfigurationId", testPrefix + "_FEE").deleteAll()
            }
        } finally {
            if (ec != null) ec.destroy()
        }
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
            
            ec.entity.find("trade.TradeInstrumentParty").condition("partyId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradePartyBank").condition("partyId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeParty").condition("partyId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            
            ec.entity.find("trade.CustomerFacility").condition("facilityId", testPrefix + "_FAC").updateAll([utilizedAmount: 0.0])
            
            ec.transaction.commit(begun)
        } catch (Exception e) {
            ec.transaction.rollback(begun, "Error in cleanData", e)
        }
    }

    def setup() {
        ec.message.clearAll()
        ec.artifactExecution.disableAuthz()
    }

    def cleanup() {
        ec.message.clearAll()
    }

    def "should calculate business date skipping weekends"() {
        given:
        // Friday May 1st, 2026.
        Calendar cal = Calendar.getInstance()
        cal.set(2026, Calendar.MAY, 1, 12, 0)
        Date friday = new Date(cal.getTimeInMillis())

        when:
        def result = ec.service.sync().name("trade.TradeCommonServices.calculate#BusinessDate")
                        .parameters([startDate: friday, daysToAdd: 1]).call()
        Calendar resCal = Calendar.getInstance()
        resCal.setTime(result.resultDate)

        then:
        resCal.get(Calendar.DAY_OF_MONTH) == 4
        resCal.get(Calendar.MONTH) == Calendar.MAY
        resCal.get(Calendar.YEAR) == 2026
    }

    def "should calculate fees based on FeeConfiguration"() {
        when:
        // 100,000 * 0.5% = 500
        def result = ec.service.sync().name("trade.TradeAccountingServices.calculate#Fees")
                        .parameters([baseAmount: 100000.0, feeEventEnumId: "ISSUANCE_FEE"]).call()

        then:
        result.totalFee == 500.0
    }

    def "should enforce facility limit and update utilization on approval"() {
        given:
        def instrumentId = testPrefix + "_LIM_01"
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            instrumentId: instrumentId, instrumentRef: instrumentId + "_REF",
            instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: testPrefix + '_APP'], [roleEnumId: 'TP_BENEFICIARY', partyId: testPrefix + '_BEN']],
            lcAmount: 500000.0, lcCurrencyUomId: "USD",
            lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_NEGOTIATION', confirmationEnumId: 'CONF_WITHOUT',
            customerFacilityId: testPrefix + "_FAC"
        ]).call()
        assert !ec.message.hasError()

        when: "approving LC"
        def tx = ec.entity.find("trade.TradeTransaction").condition("instrumentId", instrumentId).one()
        ec.user.loginUser("trade.checker", "trade123")
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument").parameters([transactionId: tx.transactionId, skipFourEyes: true]).call()
        
        def facility = ec.entity.find("trade.CustomerFacility").condition("facilityId", testPrefix + "_FAC").one()

        then:
        facility.utilizedAmount == 500000.0

        when: "trying to approve another LC that exceeds limit"
        def instrumentId2 = testPrefix + "_LIM_02"
        ec.user.loginUser("trade.admin", "trade123")
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            instrumentId: instrumentId2, instrumentRef: instrumentId2 + "_REF",
            instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: testPrefix + '_APP'], [roleEnumId: 'TP_BENEFICIARY', partyId: testPrefix + '_BEN']],
            lcAmount: 600000.0, lcCurrencyUomId: "USD",
            lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_NEGOTIATION', confirmationEnumId: 'CONF_WITHOUT',
            customerFacilityId: testPrefix + "_FAC"
        ]).call()
        
        def tx2 = ec.entity.find("trade.TradeTransaction").condition("instrumentId", instrumentId2).one()
        ec.user.loginUser("trade.checker", "trade123")
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument").parameters([transactionId: tx2.transactionId, skipFourEyes: true]).call()

        then:
        ec.message.hasError()
        ec.message.getErrorsString().contains("Insufficient limit")
    }

    def "should persist ImportLcSettlement on settlement"() {
        given:
        def instrumentId = testPrefix + "_SETTLE"
        ec.user.loginUser("trade.admin", "trade123")
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            instrumentId: instrumentId, instrumentRef: instrumentId + "_REF",
            instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: testPrefix + '_APP'], [roleEnumId: 'TP_BENEFICIARY', partyId: testPrefix + '_BEN']],
            lcAmount: 100000.0, lcCurrencyUomId: "USD"
        ]).call()
        
        def tx = ec.entity.find("trade.TradeTransaction").condition([instrumentId: instrumentId, transactionTypeEnumId: 'IMP_NEW']).one()
        ec.user.loginUser("trade.checker", "trade123")
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument").parameters([transactionId: tx.transactionId, skipFourEyes: true]).call()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).updateAll([businessStateId: "LC_ISSUED"])

        def presValue = ec.entity.makeValue("trade.importlc.TradeDocumentPresentation")
            .setAll([instrumentId: instrumentId, claimAmount: 20000.0, presentationStatusId: "PRES_COMPLIANT"])
            .setSequencedIdPrimary().create()
        def presId = presValue.presentationId

        when: "settling presentation"
        ec.service.sync().name("trade.importlc.ImportLcServices.settle#Presentation").parameters([
            presentationId: presId, principalAmount: 20000.0, settlementTypeEnumId: "SIGHT_PAYMENT", valueDate: new Date(System.currentTimeMillis())
        ]).call()

        def settlement = ec.entity.find("trade.importlc.ImportLcSettlement").condition("presentationId", presId).one()

        then:
        !ec.message.hasError()
        settlement != null
        settlement.principalAmount == 20000.0
    }
}
