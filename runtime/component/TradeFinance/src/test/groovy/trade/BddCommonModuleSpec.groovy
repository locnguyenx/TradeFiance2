package trade

import spock.lang.*
import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import java.sql.Date
import java.sql.Timestamp
import org.moqui.entity.EntityCondition

// ABOUTME: BddCommonModuleSpec provides backend parity for the Common Module BDD scenarios.
// ABOUTME: Covers Base Entities, KYC, Facility Limits, FX, SLA, and Authority Tiers.

class BddCommonModuleSpec extends Specification {
    @Shared protected ExecutionContext ec
    @Shared String testPrefix

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.artifactExecution.disableAuthz()
        ec.user.loginUser("trade.admin", "trade123")
        testPrefix = "BDD-CMN-" + System.currentTimeMillis()
        cleanData()
        
        // Ensure test parties exist
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: testPrefix + '_CUST_01', partyTypeEnumId: 'PTY_COMMERCIAL', 
                             partyName: 'BDD Customer 01', kycStatus: 'KYC_ACTIVE']).call()

        // Ensure holiday exists
        def holidayDate = java.sql.Date.valueOf("2026-04-27")
        if (ec.entity.find("trade.PublicHoliday").condition("holidayDate", holidayDate).count() == 0) {
            ec.entity.makeValue("trade.PublicHoliday").setAll([holidayDate: holidayDate, description: "SLA Test Holiday"]).create()
        }
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
            ec.entity.find("trade.importlc.ImportLcInternalAmendment").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.ImportLcAmendment").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.ImportLcSettlement").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeApprovalRecord").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeTransaction").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeInstrument").condition("instrumentId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            
            ec.entity.find("trade.CustomerFacility").condition("facilityId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            ec.entity.find("trade.TradeParty").condition("partyId", EntityCondition.LIKE, testPrefix + "%").deleteAll()
            
            ec.transaction.commit(begun)
        } catch (Exception e) {
            ec.transaction.rollback(begun, "Error in cleanData", e)
        }
    }

    def "BDD-CMN-ENT-01: Trade Inst. Base Attributes Enforcement"() {
        given:
        def instrumentId = testPrefix + "_ENT_01"
        def ref = instrumentId + "_REF"
        
        when:
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, instrumentRef: ref, lcAmount: 1000.0, lcCurrencyUomId: 'USD',
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]]).call()
        
        then:
        !ec.message.hasError()
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        lc.businessStateId == "LC_DRAFT"
    }

    def "BDD-CMN-ENT-02: Valid Party KYC Acceptance"() {
        given:
        def pid = testPrefix + "_GOOD_CORP"
        ec.entity.makeValue("trade.TradeParty")
            .setAll([partyId: pid, partyName: "Good Corp", kycStatus: "KYC_ACTIVE", partyTypeEnumId: 'PTY_COMMERCIAL']).create()
            
        when:
        def result = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentId: testPrefix + "_ENT_02", instrumentRef: testPrefix + "_REF_02", lcAmount: 1000.0, lcCurrencyUomId: 'USD', 
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: pid],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]]).call()
        
        then:
        !ec.message.hasError()
        result.instrumentId != null
    }

    def "BDD-CMN-ENT-03: Expired Party KYC Rejection"() {
        given:
        def pid = testPrefix + "_BAD_CORP"
        def yesterday = new Date(System.currentTimeMillis() - 86400000)
        ec.entity.makeValue("trade.TradeParty")
            .setAll([partyId: pid, partyName: "Bad Corp", kycStatus: "KYC_EXPIRED", kycExpiryDate: yesterday, partyTypeEnumId: 'PTY_COMMERCIAL']).create()
            
        when:
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: testPrefix + "_REF_03", lcAmount: 1000.0, lcCurrencyUomId: 'USD', 
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: pid],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]]).call()
        
        then:
        ec.message.hasError()
        ec.message.getErrorsString().contains("has no active KYC")
    }

    def "BDD-CMN-ENT-04: Facility Limit Availability Earmark"() {
        given:
        def fid = testPrefix + "_FAC_01"
        ec.entity.makeValue("trade.CustomerFacility")
            .setAll([facilityId: fid, totalApprovedLimit: 5000000.0, utilizedAmount: 1000000.0]).create()
            
        when:
        ec.service.sync().name("trade.LimitServices.update#Utilization")
            .parameters([facilityId: fid, amountDelta: 50000.0]).call()
            
        then:
        def fac = ec.entity.find("trade.CustomerFacility").condition("facilityId", fid).one()
        fac.utilizedAmount == 1050000.0
    }

    def "BDD-CMN-WF-01: Processing Flow Execution to Pending"() {
        given:
        def instrumentId = testPrefix + "_FLOW_01"
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, instrumentRef: instrumentId + "_REF", lcAmount: 1000.0, lcCurrencyUomId: 'USD',
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]]).call()
            
        when:
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, businessStateId: "LC_PENDING"]).call()
            
        then:
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        lc.businessStateId == "LC_PENDING"
    }

    @Unroll
    def "BDD-CMN-FX-01/02: Precision: #currency decimal format"() {
        given: "A currency #currency with decimals #decimals"
        
        when:
        def result = ec.service.sync().name("trade.TradeCommonServices.round#Amount")
            .parameters([amount: rawAmount, currencyUomId: currency]).call()
        
        then:
        !result.errorMessage
        result.roundedAmount == expected
        
        where:
        currency | rawAmount | expected
        "JPY"    | 10050.50  | 10051
        "USD"    | 5200.125  | 5200.13
    }

    def "BDD-CMN-SLA-01: SLA Timer Skips Head Office Holidays"() {
        given: "A start date on Monday"
        def start = java.sql.Date.valueOf("2026-04-20")
        
        when:
        def result = ec.service.sync().name("trade.TradeCommonServices.calculate#BusinessDate")
            .parameters([startDate: start, daysToAdd: 5]).call()
        
        then:
        !result.errorMessage
        result.resultDate == java.sql.Date.valueOf("2026-04-28")
    }

    def "BDD-CMN-VAL-01: Hard Stop on Limit Breach"() {
        given:
        def fid = testPrefix + "_FAC_BREACH"
        ec.entity.makeValue("trade.CustomerFacility")
            .setAll([facilityId: fid, totalApprovedLimit: 4999.0, utilizedAmount: 0.0]).create()
            
        when:
        ec.service.sync().name("trade.LimitServices.calculate#Earmark")
            .parameters([facilityId: fid, amount: 5000.0]).call()
        
        then:
        ec.message.hasError()
        ec.message.getErrorsString().contains("Insufficient limit")
    }

    def "BDD-CMN-FEE-01: Customer Exception Rate Overrides Standard Rate"() {
        given: "A standard fee configuration at 0.25% and a customer-specific override at 0.10%"
        def partyId = testPrefix + "_CUST_FEE"
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: partyId, partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'Fee Test Customer', kycStatus: 'KYC_ACTIVE']).call()
        
        def feeEvent = "ISSUANCE_FEE"

        // Create standard rate: 0.25%
        ec.entity.makeValue("trade.FeeConfiguration")
            .setAll([feeEventEnumId: feeEvent, calculationTypeEnumId: "PERCENTAGE",
                     baseValue: 0.0025, statusId: "FEE_ACTIVE",
                     effectiveDate: new Date(System.currentTimeMillis() - 86400000)])
            .setSequencedIdPrimary().create()

        // Create customer-specific rate: 0.10%
        ec.entity.makeValue("trade.FeeConfiguration")
            .setAll([feeEventEnumId: feeEvent, calculationTypeEnumId: "PERCENTAGE",
                     baseValue: 0.0010, partyId: partyId, statusId: "FEE_ACTIVE",
                     effectiveDate: new Date(System.currentTimeMillis() - 86400000)])
            .setSequencedIdPrimary().create()

        when: "Calculating fees with customer override"
        def result = ec.service.sync().name("trade.TradeAccountingServices.calculate#Fees")
            .parameters([baseAmount: 100000.0, feeEventEnumId: feeEvent, partyId: partyId]).call()

        then: "Customer rate (0.10% of 100k = 100) is applied instead of standard rate (0.25% = 250)"
        result.totalFee == 100.0
    }
}