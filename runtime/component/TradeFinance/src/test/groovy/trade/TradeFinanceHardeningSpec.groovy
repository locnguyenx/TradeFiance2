package trade

import spock.lang.Specification
import spock.lang.Shared
import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import java.sql.Date
import java.util.Calendar
import org.moqui.entity.EntityCondition

/**
 * ABOUTME: TradeFinanceHardeningSpec verifies the backend improvements made during the hardening phase.
 * Tests cover business date logic, fee configuration, accounting integration, and limit enforcement.
 */
class TradeFinanceHardeningSpec extends Specification {
    @Shared protected ExecutionContext ec
    @Shared String testPrefix
    
    @Shared String applicantId
    @Shared String beneficiaryId
    @Shared String facilityId

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.transaction.runUseOrBegin(null, null) {
            ec.artifactExecution.disableAuthz()
            ec.user.loginUser("trade.admin", "trade123")
            testPrefix = "TFH-" + System.currentTimeMillis()
        
        applicantId = testPrefix + "-APP"
        beneficiaryId = testPrefix + "-BEN"

        // Set isolated ID generation ranges - use 12000000
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrument", 12000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit", 12000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeTransaction", 12000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrumentParty", 12000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.CustomerFacility", 12000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.FeeConfiguration", 12000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.TradeDocumentPresentation", 12000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLcSettlement", 12000000, 1000)



        // Setup unique TradeConfig
        ec.entity.makeValue("trade.common.TradeConfig")
            .setAll([configId: testPrefix + "_CONF", configKey: testPrefix + "_GL_CASH", configValue: "110101"]).create()
            
        // Setup unique Facility and Party
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
            .parameters([partyId: applicantId, partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'App Hardening', kycStatus: 'KYC_ACTIVE']).call()
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
            .parameters([partyId: beneficiaryId, partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'Ben Hardening', kycStatus: 'KYC_ACTIVE']).call()
            
        // Setup unique FeeConfiguration
        ec.entity.makeValue("trade.FeeConfiguration")
            .setAll([
                feeEventEnumId: "ISSUANCE_FEE", calculationTypeEnumId: "PERCENTAGE",
                baseValue: 0.005, minFloorAmount: 100.0, statusId: "FEE_ACTIVE", 
                partyId: applicantId,
                effectiveDate: new Date(System.currentTimeMillis() - 86400000)
            ]).setSequencedIdPrimary().create()
            
        def facVal = ec.entity.makeValue("trade.CustomerFacility")
            .setAll([
                ownerPartyId: applicantId, 
                totalApprovedLimit: 1000000.0, utilizedAmount: 0.0, currencyUomId: "USD", statusId: "FAC_ACTIVE"
            ]).setSequencedIdPrimary().create()
        facilityId = facVal.facilityId
        }
    }

    def cleanupSpec() {
        if (ec != null) {
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrument")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeTransaction")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrumentParty")
            ec.entity.tempResetSequencedIdPrimary("trade.CustomerFacility")
            ec.entity.tempResetSequencedIdPrimary("trade.FeeConfiguration")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.TradeDocumentPresentation")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLcSettlement")
            ec.destroy()
        }
    }

    def setup() {
        if (ec.transaction.isTransactionInPlace()) ec.transaction.rollback("Cleanup", null)
        ec.message.clearAll()
        ec.user.loginUser("trade.admin", "trade123")
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
                        .parameters([baseAmount: 100000.0, feeEventEnumId: "ISSUANCE_FEE", partyId: applicantId]).call()

        then:
        result.totalFee == 500.0
    }

    def "should enforce facility limit and update utilization on approval"() {
        given:
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            instrumentRef: testPrefix + "_LIM_01_REF",
            instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId], [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]],
            lcAmount: 500000.0, lcCurrencyUomId: "USD",
            lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_NEGOTIATION', confirmationEnumId: 'CONF_WITHOUT',
            customerFacilityId: facilityId
        ]).call()
        assert !ec.message.hasError()
        String instrumentId = res.instrumentId

        when: "approving LC"
        def tx = ec.entity.find("trade.TradeTransaction").condition("instrumentId", instrumentId).one()
        ec.user.loginUser("trade.checker", "trade123")
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument").parameters([transactionId: tx.transactionId, skipFourEyes: true]).call()
        
        def facility = ec.entity.find("trade.CustomerFacility").condition("facilityId", facilityId).one()

        then:
        facility.utilizedAmount == 500000.0

        when: "trying to approve another LC that exceeds limit"
        ec.user.loginUser("trade.admin", "trade123")
        def res2 = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            instrumentRef: testPrefix + "_LIM_02_REF",
            instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId], [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]],
            lcAmount: 600000.0, lcCurrencyUomId: "USD",
            lcTypeEnumId: 'LCT_IRREVOCABLE', availableByByEnumId: 'AVB_BY_NEGOTIATION', confirmationEnumId: 'CONF_WITHOUT',
            customerFacilityId: facilityId
        ]).call()
        String instrumentId2 = res2.instrumentId
        
        def tx2 = ec.entity.find("trade.TradeTransaction").condition("instrumentId", instrumentId2).one()
        ec.user.loginUser("trade.checker", "trade123")
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument").parameters([transactionId: tx2.transactionId, skipFourEyes: true]).call()

        then:
        ec.message.hasError()
        ec.message.getErrorsString().contains("Insufficient limit")
    }

    def "should persist ImportLcSettlement on settlement"() {
        given:
        ec.user.loginUser("trade.admin", "trade123")
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            instrumentRef: testPrefix + "_SETTLE_REF",
            instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId], [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]],
            lcAmount: 100000.0, lcCurrencyUomId: "USD"
        ]).call()
        String instrumentId = res.instrumentId
        
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
