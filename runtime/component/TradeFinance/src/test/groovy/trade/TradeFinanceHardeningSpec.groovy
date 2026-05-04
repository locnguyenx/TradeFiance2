package trade

import spock.lang.Specification
import spock.lang.Shared
import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import java.sql.Date
import java.util.Calendar

// ABOUTME: TradeFinanceHardeningSpec verifies the backend improvements made during the hardening phase.
// Tests cover business date logic, fee configuration, accounting integration, and limit enforcement.

class TradeFinanceHardeningSpec extends Specification {
    @Shared ExecutionContext ec

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.artifactExecution.disableAuthz()
        ec.user.loginUser("trade.admin", "trade123")
        
        // Ensure necessary users and profiles exist (simplified from ImportLcServicesSpec)
        if (!ec.entity.find("moqui.security.UserAccount").condition("username", "trade.checker").one()) {
            ec.entity.makeValue("moqui.security.UserAccount")
                .setAll([userId: "trade.checker", username: "trade.checker", currentPassword: "trade123", firstName: "Trade", lastName: "Checker"])
                .create()
        }
        if (!ec.entity.find("trade.UserAuthorityProfile").condition("userId", "trade.checker").one()) {
            ec.entity.makeValue("trade.UserAuthorityProfile")
                .setAll([userAuthorityId: "T2-HARDEN", userId: "trade.checker", delegationTierId: "TIER_2", customLimit: 10000000.00, currencyUomId: "USD", makerCheckerFlag: "CHECKER"])
                .create()
        }

        // Setup a test FeeConfiguration
        ec.entity.find("trade.FeeConfiguration").condition("feeEventEnumId", "ISSUANCE_FEE").deleteAll()
        ec.entity.makeValue("trade.FeeConfiguration")
            .setAll([
                feeEventEnumId: "ISSUANCE_FEE",
                calculationTypeEnumId: "PERCENTAGE",
                baseValue: 0.005, // 0.5%
                minFloorAmount: 100.0,
                statusId: "FEE_ACTIVE",
                effectiveDate: new Date(System.currentTimeMillis() - 86400000)
            ]).setSequencedIdPrimary().create()

        // Setup a test TradeConfig for accounting
        ec.entity.find("trade.common.TradeConfig").condition("configKey", "GL_CASH_ACCOUNT").deleteAll()
        ec.entity.makeValue("trade.common.TradeConfig")
            .setAll([configKey: "GL_CASH_ACCOUNT", configValue: "110101"])
            .setSequencedIdPrimary().create()
            
        // Setup a test Facility
        ec.entity.find("trade.CustomerFacility").condition("facilityId", "FAC_TEST_001").deleteAll()
        ec.entity.makeValue("trade.CustomerFacility")
            .setAll([
                facilityId: "FAC_TEST_001",
                ownerPartyId: "ACME_CORP_001",
                totalApprovedLimit: 1000000.0,
                utilizedAmount: 0.0,
                currencyUomId: "USD",
                statusId: "FAC_ACTIVE"
            ]).create()
    }

    def setup() {
        ec.message.clearAll()
        ec.user.loginUser("trade.admin", "trade123")
        ec.artifactExecution.disableAuthz()
    }

    def cleanupSpec() {
        ec.destroy()
    }

    def "should calculate business date skipping weekends"() {
        given:
        // Friday May 1st, 2026.
        Calendar cal = Calendar.getInstance()
        cal.set(2026, Calendar.MAY, 1, 12, 0)
        Date friday = new Date(cal.getTimeInMillis())

        when:
        // Add 1 business day -> should be Monday May 4th
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
        def txRef = "HARDEN-LIM-" + System.currentTimeMillis()
        def createResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            transactionRef: txRef,
            instrumentParties: [
                [roleEnumId: 'TP_APPLICANT', partyId: "ACME_CORP_001"],
                [roleEnumId: 'TP_BENEFICIARY', partyId: "GLOBAL_EXP_002"]
            ],
            lcAmount: 500000.0, // 500k
            lcCurrencyUomId: "USD",
            customerFacilityId: "FAC_TEST_001"
        ]).call()
        def instrumentId = createResult.instrumentId

        when:
        ec.user.loginUser("trade.checker", "trade123")
        ec.service.sync().name("trade.importlc.ImportLcServices.approve#ImportLetterOfCredit").parameters([
            instrumentId: instrumentId,
            approverUserId: "trade.checker"
        ]).call()
        
        def facility = ec.entity.find("trade.CustomerFacility").condition("facilityId", "FAC_TEST_001").one()

        then:
        !ec.message.hasError()
        facility.utilizedAmount == 500000.0

        when: "trying to approve another LC that exceeds limit"
        def createResult2 = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            transactionRef: txRef + "-2",
            instrumentParties: [
                [roleEnumId: 'TP_APPLICANT', partyId: "ACME_CORP_001"],
                [roleEnumId: 'TP_BENEFICIARY', partyId: "GLOBAL_EXP_002"]
            ],
            lcAmount: 600000.0, // Total 1.1M, limit is 1M
            lcCurrencyUomId: "USD",
            customerFacilityId: "FAC_TEST_001"
        ]).call()
        def instrumentId2 = createResult2.instrumentId
        
        // Second approval should fail due to limit
        ec.service.sync().name("trade.importlc.ImportLcServices.approve#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId2, approverUserId: "trade.checker"]).call()
        
        ec.message.getErrorsString().contains("Insufficient limit")

        then:
        ec.message.hasError()

        cleanup:
        ec.entity.find("trade.TradeApprovalRecord").condition("instrumentId", instrumentId).deleteAll()
        ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", instrumentId).deleteAll()
        ec.entity.find("trade.TradeTransaction").condition("instrumentId", instrumentId).deleteAll()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).deleteAll()
        ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", instrumentId).deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId).deleteAll()
    }

    def "should persist ImportLcSettlement on settlement"() {
        given:
        def txRef = "HARDEN-SET-" + System.currentTimeMillis()
        def createResult = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            transactionRef: txRef,
            instrumentParties: [
                [roleEnumId: 'TP_APPLICANT', partyId: "ACME_CORP_001"],
                [roleEnumId: 'TP_BENEFICIARY', partyId: "GLOBAL_EXP_002"]
            ],
            lcAmount: 100000.0,
            lcCurrencyUomId: "USD"
        ]).call()
        def instrumentId = createResult.instrumentId
        
        // Transition to issued
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).updateAll([businessStateId: "LC_ISSUED"])

        // Create presentation
        def presValue = ec.entity.makeValue("trade.importlc.TradeDocumentPresentation")
            .setAll([instrumentId: instrumentId, claimAmount: 20000.0, presentationStatusId: "PRES_COMPLIANT"])
            .setSequencedIdPrimary().create()
        def presId = presValue.presentationId

        when:
        ec.service.sync().name("trade.importlc.ImportLcServices.settle#Presentation").parameters([
            presentationId: presId,
            principalAmount: 20000.0,
            settlementTypeEnumId: "SIGHT_PAYMENT",
            valueDate: new Date(System.currentTimeMillis())
        ]).call()

        def settlement = ec.entity.find("trade.importlc.ImportLcSettlement").condition("presentationId", presId).one()

        then:
        !ec.message.hasError()
        settlement != null
        settlement.principalAmount == 20000.0
        settlement.instrumentId == instrumentId
    }
}
