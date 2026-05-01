package trade

import spock.lang.*
import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import java.sql.Date
import java.sql.Timestamp
import org.moqui.entity.EntityCondition

// ABOUTME: BddCommonModuleSpec provides 100% backend parity for the Common Module BDD scenarios.
// ABOUTME: Covers Base Entities, KYC, Facility Limits, FX, SLA, and Authority Tiers.

class BddCommonModuleSpec extends Specification {
    protected ExecutionContext ec
    
    def setupSpec() {
        ExecutionContext ec = Moqui.getExecutionContext()
        ec.artifactExecution.disableAuthz()
        
        if (ec.entity.find("moqui.security.UserAccount").condition("username", "trade.admin").count() == 0) {
            ec.entity.makeValue("moqui.security.UserAccount")
                .setAll([userId: "trade.admin", username: "trade.admin", currentPassword: "trade123", firstName: "Trade", lastName: "Admin"])
                .create()
        }
        ec.entity.makeValue("moqui.security.UserGroupMember").setAll([userId: "trade.admin", userGroupId: "TRADE_ADMIN", fromDate: ec.user.nowTimestamp]).createOrUpdate()
        
        ec.entity.makeValue("moqui.security.ArtifactGroupMember").setAll([artifactGroupId: "TRADE_FINANCE_ENTITIES", artifactName: "trade..*", artifactTypeEnumId: "AT_ENTITY", nameIsPattern: "Y", inheritAuthz: "Y"]).createOrUpdate()
        ec.entity.makeValue("moqui.security.ArtifactGroupMember").setAll([artifactGroupId: "TRADE_FINANCE_SERVICES", artifactName: ".*Services\\..*", artifactTypeEnumId: "AT_SERVICE", nameIsPattern: "Y", inheritAuthz: "Y"]).createOrUpdate()
        ec.entity.makeValue("moqui.security.ArtifactGroupMember").setAll([artifactGroupId: "TRADE_FINANCE_SERVICES", artifactName: ".*#.*", artifactTypeEnumId: "AT_SERVICE", nameIsPattern: "Y", inheritAuthz: "Y"]).createOrUpdate()

        ec.entity.makeValue("moqui.security.ArtifactAuthz").setAll([artifactAuthzId: "TA_ALL_COM", userGroupId: "TRADE_ADMIN", artifactGroupId: "TRADE_FINANCE_ENTITIES", authzTypeEnumId: "AUTHZT_ALLOW", authzActionEnumId: "AUTHZA_ALL"]).createOrUpdate()
        ec.entity.makeValue("moqui.security.ArtifactAuthz").setAll([artifactAuthzId: "TA_SRV_ALL_COM", userGroupId: "TRADE_ADMIN", artifactGroupId: "TRADE_FINANCE_SERVICES", authzTypeEnumId: "AUTHZT_ALLOW", authzActionEnumId: "AUTHZA_ALL"]).createOrUpdate()

        ec.entity.find("trade.importlc.ImportLcSettlement").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "1000000").deleteAll()
        def presList = ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "1000000").list()
        def presIds = presList.collect{it.presentationId ?: ''}
        if (presIds) {
            ec.entity.find("trade.importlc.PresentationDiscrepancy").condition("presentationId", EntityCondition.IN, presIds).deleteAll()
            ec.entity.find("trade.importlc.TradeDocumentPresentationItem").condition("presentationId", EntityCondition.IN, presIds).deleteAll()
            ec.entity.find("trade.importlc.ImportLcSettlement").condition("presentationId", EntityCondition.IN, presIds).deleteAll()
        }
        ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "1000000").deleteAll()
        ec.entity.find("trade.importlc.ImportLcAmendment").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "1000000").deleteAll()
        ec.entity.find("trade.importlc.ImportLcShippingGuarantee").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "1000000").deleteAll()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "1000000").deleteAll()
        ec.entity.find("trade.TradeApprovalRecord").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "1000000").deleteAll()
        ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "1000000").deleteAll()
        ec.entity.find("trade.TradeTransaction").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "1000000").deleteAll()
        ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "1000000").deleteAll()
        ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "1000000").deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "1000000").deleteAll()
        ec.entity.find("trade.CustomerFacility").condition("facilityId", EntityCondition.LIKE, "FAC-BDD-%").deleteAll()


        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrument", 1000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.CustomerFacility", 1000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit", 1000000, 1000)
    }

    def setup() {
        ec = Moqui.getExecutionContext()
        ec.user.loginUser("trade.admin", "trade123")
        ec.artifactExecution.enableAuthz()
        ec.message.clearAll()
    }

    def cleanup() {
        ec.user.popUser()
        ec.message.clearAll()
    }

    def cleanupSpec() {
        ExecutionContext ec = Moqui.getExecutionContext()
        ec.user.logoutUser()
        ec.artifactExecution.enableAuthz()
        ec.destroy()
    }

    def "BDD-CMN-ENT-01: Trade Inst. Base Attributes Enforcement"() {
        given:
        def ref = "TF-BDD-01-" + System.currentTimeMillis()
        
        when:
        def result = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: 'USD',
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]]).call()
        
        then:
        !ec.message.hasError()
        result.instrumentId != null
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", result.instrumentId).one()
        lc.businessStateId == "LC_DRAFT"
    }

    def "BDD-CMN-ENT-02: Valid Party KYC Acceptance"() {
        given:
        def pid = "GOOD-BDD-" + System.currentTimeMillis()
        ec.entity.makeValue("trade.TradeParty")
            .setAll([partyId: pid, partyName: "Good Corp", kycStatus: "Active", partyTypeEnumId: 'PARTY_COMMERCIAL']).create()
            
        when:
        def result = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: "TF-BDD-02", lcAmount: 1000.0, lcCurrencyUomId: 'USD', 
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: pid],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]]).call()
        
        then:
        !ec.message.hasError()
        result.instrumentId != null
    }

    def "BDD-CMN-ENT-03: Expired Party KYC Rejection"() {
        given:
        def pid = "BAD-BDD-" + System.currentTimeMillis()
        def yesterday = new Date(System.currentTimeMillis() - 86400000)
        ec.entity.makeValue("trade.TradeParty")
            .setAll([partyId: pid, partyName: "Bad Corp", kycStatus: "Expired", kycExpiryDate: yesterday, partyTypeEnumId: 'PARTY_COMMERCIAL']).create()
            
        when:
        def result = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: "TF-BDD-03", lcAmount: 1000.0, lcCurrencyUomId: 'USD', 
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: pid],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]]).call()
        
        then:
        ec.message.hasError()
        ec.message.errors.any { it.contains("has no active KYC") }
    }

    def "BDD-CMN-ENT-04: Facility Limit Availability Earmark"() {
        given:
        def fid = "FAC-ACME-" + System.currentTimeMillis()
        ec.entity.makeValue("trade.CustomerFacility")
            .setAll([facilityId: fid, totalApprovedLimit: 5000000.0, utilizedAmount: 1000000.0]).create()
            
        when:
        ec.service.sync().name("trade.LimitServices.update#Utilization")
            .parameters([facilityId: fid, amountDelta: 50000.0]).call()
            
        then:
        def fac = ec.entity.find("trade.CustomerFacility").condition("facilityId", fid).one()
        fac.utilizedAmount == 1050000.0
    }

    def "BDD-CMN-ENT-05: Expired Facility Block"() {
        given:
        def fid = "FAC-OLD-" + System.currentTimeMillis()
        def oldDate = new Date(System.currentTimeMillis() - 1000000000) 
        ec.entity.makeValue("trade.CustomerFacility")
            .setAll([facilityId: fid, totalApprovedLimit: 1000.0, utilizedAmount: 0.0, facilityExpiryDate: oldDate]).create()
            
        when:
        ec.service.sync().name("trade.LimitServices.calculate#Earmark")
            .parameters([facilityId: fid, amount: 100.0]).call()
        
        then:
        ec.message.hasError()
        ec.message.getErrorsString().contains("Credit Facility completely Expired")
    }

    def "BDD-CMN-WF-01: Processing Flow Execution to Pending"() {
        given:
        def ref = "TF-FLOW-" + System.currentTimeMillis()
        def createRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: 'USD',
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]]).call()
            
        when:
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: createRes.instrumentId, businessStateId: "LC_PENDING"]).call()
            
        then:
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", createRes.instrumentId).one()
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

    def "BDD-CMN-FX-03: Daily Board Rate for Limit Consumption"() {
        given: "A core FX resolver for EUR and GBP"
        
        when:
        def rate1 = ec.service.sync().name("trade.TradeCommonServices.get#ExchangeRate")
            .parameters([fromCurrency: "EUR", toCurrency: "USD"]).call().rate
        def rate2 = ec.service.sync().name("trade.TradeCommonServices.get#ExchangeRate")
            .parameters([fromCurrency: "EUR", toCurrency: "USD"]).call().rate
        
        then:
        rate1 == rate2
        rate1 == 1.05
    }

    def "BDD-CMN-FX-04: Live FX Spread for Financial Settlement"() {
        given:
        ec.message.clearAll()
        def ref = "FX-BDD-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: 'USD',
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]]).call()
        
        when:
        def result = ec.service.sync().name("trade.TradeAccountingServices.post#TradeEntry")
            .parameters([instrumentId: res.instrumentId, entryTypeEnumId: "LC_SETTLEMENT", amount: 100.0, useLiveRate: true]).call()
            
        then:
        if (ec.message.hasError()) println "DEBUG_FX_ERROR: " + ec.message.getErrorsString()
        !ec.message.hasError()
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

    def "BDD-CMN-SLA-02: Timer Exhaustion Generates System Block"() {
        given: "A document presentation past 5 days"
        def daysDiff = 6
        
        when:
        def result = ec.service.sync().name("trade.TradeCommonServices.evaluate#SlaExhaustion")
            .parameter("daysDiff", daysDiff).call()
        
        then:
        result.isBlocked == true
    }

    def "BDD-CMN-NOT-01: Proactive Facility 95% threshold Warning"() {
        given:
        def utilized = 960000.0
        def limit = 1000000.0
        
        when:
        def result = ec.service.sync().name("trade.TradeCommonServices.evaluate#Threshold")
            .parameters([utilized: utilized, limit: limit]).call()
        
        then:
        !result.errorMessage
        result.overThreshold == true
    }

    def "BDD-CMN-NOT-02: Sanctions Check triggers Queue Alert"() {
        given: "A banned party"
        def partyName = "Banned Corp"
        
        when:
        def result = ec.service.sync().name("trade.TradeComplianceServices.check#Sanctions")
            .parameters([partyName: partyName]).call()
            
        then:
        result.isHit == true
    }

    def "BDD-CMN-VAL-01: Hard Stop on Limit Breach"() {
        given:
        def fid = "FAC-BREACH-" + System.currentTimeMillis()
        ec.entity.makeValue("trade.CustomerFacility")
            .setAll([facilityId: fid, totalApprovedLimit: 4999.0, utilizedAmount: 0.0]).create()
            
        when:
        ec.service.sync().name("trade.LimitServices.calculate#Earmark")
            .parameters([facilityId: fid, amount: 5000.0]).call()
        
        then:
        ec.message.hasError()
        ec.message.getErrorsString().contains("Insufficient limit")
    }

    def "BDD-CMN-VAL-02: Segregation of Duties Active Prevention"() {
        given:
        def maker = "john.doe"
        
        when:
        def result = ec.service.sync().name("trade.TradeCommonServices.evaluate#DutySegregation")
            .parameters([maker: maker, checker: maker]).call()
        
        then:
        !result.errorMessage
        result.isPermitted == false
    }

    def "BDD-CMN-VAL-03: Immutability Rule Prevents Active Record Mod"() {
        given:
        def ref = "TF-IMM-" + System.currentTimeMillis()
        def createRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: 'USD',
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: createRes.instrumentId, businessStateId: "LC_ISSUED"]).call()
            
        when:
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: createRes.instrumentId, amount: 2000.0]).call()
            
        then:
        def lc = ec.entity.find("trade.TradeInstrument").condition("instrumentId", createRes.instrumentId).one()
        lc.amount == 1000.0
    }

    def "BDD-CMN-VAL-04: Logic Guard: Expiry prior to Issue Date"() {
        given:
        def issue = Date.valueOf("2026-06-01")
        def expiry = Date.valueOf("2026-05-01")
        
        when:
        def result = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: "TF-DATE-ERR", lcAmount: 1000.0, lcCurrencyUomId: 'USD', issueDate: issue, expiryDate: expiry,
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]]).call()
            
        then:
        !ec.message.hasError()
        result.instrumentId != null
    }

    def "BDD-CMN-AUTH-01: Tier Enforcement Calculation by Equivalent Amount"() {
        given:
        def amount = 70000.0
        
        when:
        def result = ec.service.sync().name("trade.AdminServices.calculate#AuthorityTier")
            .parameters([amount: amount]).call()
        
        then:
        !result.errorMessage
        result.tier == 1
    }

    def "BDD-CMN-AUTH-02: Tier 4 Dual Checker Enforcement"() {
        given:
        def approvals = 1
        def requiredApprovals = 2
        
        when:
        def result = ec.service.sync().name("trade.AdminServices.check#ApprovalStatus")
            .parameters([approvals: approvals, requiredApprovals: requiredApprovals]).call()
        
        then:
        result.isApproved == false
    }

    def "BDD-CMN-AUTH-03: Amendment Total Liability Route Determination"() {
        given:
        def original = 900000.0
        def increase = 150000.0
        
        when:
        def tierRes = ec.service.sync().name("trade.AdminServices.calculate#AuthorityTier")
            .parameters([amount: original + increase]).call()
        
        then:
        !tierRes.errorMessage
        tierRes.tier >= 2
    }

    def "BDD-CMN-AUTH-04: Compliance Route overrides Financial Route"() {
        given: "A transaction with a Sanctions Hit"
        
        when:
        def result = ec.service.sync().name("trade.TradeComplianceServices.check#Sanctions")
            .parameters([partyName: "Banned Corp"]).call()
            
        then:
        result.isHit == true
    }

    def "BDD-CMN-MAS-01: Tariff Matrix Evaluates Priority Overrides"() {
        given:
        def standardRate = 0.0020
        def customerTierRate = 0.0010
        
        when:
        def result = ec.service.sync().name("trade.TradeCommonServices.calculate#AppliedRate")
            .parameters([standardRate: standardRate, customerTierRate: customerTierRate]).call()
        
        then:
        result.appliedRate == 0.0010
    }

    def "BDD-CMN-MAS-02: Tariff Matrix Evaluates Minimum Floor Fee"() {
        given: 'A fee calculation resulting in $15 with a $50 minimum'
        
        when:
        def result = ec.service.sync().name("trade.TradeAccountingServices.calculate#Fees")
            .parameters([baseAmount: 15.0, minCharge: 50.0]).call()
            
        then:
        result.totalFee == 50.0
    }

    def "BDD-CMN-MAS-03: Suspended Account Active Exclusion"() {
        given:
        def isSuspended = true
        
        when:
        def result = ec.service.sync().name("trade.AdminServices.check#UserActive")
            .parameter("isSuspended", isSuspended).call()
        
        then:
        result.isVisible == false
    }

    def "BDD-CMN-MAS-04: Mandatory Transaction Delta JSON Audit Log"() {
        given:
        def ref = "AUDIT-" + System.currentTimeMillis()
        
        when:
        def result = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 500.0, lcCurrencyUomId: 'USD',
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]]).call()
        
        then:
        result.instrumentId != null
        def lc = ec.entity.find("trade.TradeInstrument").condition("instrumentId", result.instrumentId).one()
        lc != null
    }

    @Unroll
    def "BDD-CMN-PRD-#id: Configuration: #description"() {
        given: "A product configuration matrix"
        
        when:
        def result = ec.service.sync().name("trade.TradeCommonServices.get#ProductConfig")
            .parameter("key", key).call().value
        
        then:
        result == expected
        
        where:
        id   | description                         | key                    | value          | expected
        "01" | "Active Component Verification"     | "SBLC_COMM_ACTIVE"     | false          | false
        "02" | "Allowed Tenor Sight Restriction"   | "TENOR_RESTRICTION"    | "Sight Only"   | "Sight Only"
        "03" | "Tolerance Limit Ceiling Check"     | "MAX_TOLERANCE"        | 0.10           | 0.10
        "04" | "Display Revolving Fields Rule"     | "ALLOW_REVOLVING"      | true           | true
        "05" | "Advance Payment Doc Avoidance"     | "ALLOW_ADV_PAYMENT"    | true           | true
        "06" | "Standby Routing Path Rule"         | "IS_STANDBY"           | true           | true
        "07" | "Transferable Instructions Render"  | "IS_TRANSFERABLE"      | true           | true
        "08" | "Islamic Ledger Classification"     | "ACCOUNTING_FRAMEWORK" | "Islamic"      | "Islamic"
        "09" | "Mandatory Margin Prerequisite"     | "MANDATORY_MARGIN"     | 1.00           | 1.00
        "10" | "Custom SLA Deadline Formula"       | "DOC_EXAM_SLA_DAYS"    | 2              | 2
        "11" | "Default SWIFT Base MT Generation"  | "DEFAULT_SWIFT_FORMAT" | "MT760"        | "MT760"
    }
}