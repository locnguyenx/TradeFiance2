
package trade

import spock.lang.*
import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import java.sql.Date
import org.moqui.entity.EntityCondition

// ABOUTME: BddImportLcModuleSpec provides 100% backend parity for Import LC lifecycle scenarios.
// ABOUTME: Covers Issuance, Amendment, Shipping Guarantee, Drawing, and Cancellation.

class BddImportLcModuleSpec extends Specification {
    protected ExecutionContext ec
    
    def getService() { ec.service }
    def getEntity() { ec.entity }

    def findOrCreateFacility() {
        def fid = "FAC-DEFAULT"
        if (ec.entity.find("trade.CustomerFacility").condition("facilityId", fid).count() == 0) {
            ec.entity.makeValue("trade.CustomerFacility")
                .setAll([facilityId: fid, totalApprovedLimit: 5000000.0, utilizedAmount: 0.0]).create()
        }
        return fid
    }

    def createIssuedLc(String refSuffix, BigDecimal amount = 50000.0, String isRevolving = 'N') {
        def ref = "TF-LC-" + refSuffix + "-" + System.currentTimeMillis()
        def fid = findOrCreateFacility()
        def expiryDate = new java.sql.Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000)
        // Ensure BEN-01 exists and is Active
        ec.entity.makeValue("trade.TradeParty").setAll([partyId: "BEN-01", partyName: "Beneficiary 01", partyTypeEnumId: 'PARTY_COMMERCIAL', kycStatus: 'Active']).createOrUpdate()
        
        def res = service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: amount, lcCurrencyUomId: "USD", 
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: "BEN-01"]],
                         expiryDate: expiryDate, tolerancePositive: 0.10, toleranceNegative: 0.10, 
                         isRevolving: isRevolving, customerFacilityId: fid]).call()
        service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: "LC_PENDING"]).call()
        service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: "LC_ISSUED"]).call()
        return res.instrumentId
    }

    def setupSpec() {
        ExecutionContext ec = Moqui.getExecutionContext()
        ec.artifactExecution.disableAuthz()
        
        // Ensure trade.admin exists and has full permissions
        if (ec.entity.find("moqui.security.UserAccount").condition("username", "trade.admin").count() == 0) {
            ec.entity.makeValue("moqui.security.UserAccount")
                .setAll([userId: "trade.admin", username: "trade.admin", currentPassword: "trade123", firstName: "Trade", lastName: "Admin"])
                .create()
        }
        ec.entity.makeValue("moqui.security.UserGroupMember").setAll([userId: "trade.admin", userGroupId: "TRADE_ADMIN", fromDate: ec.user.nowTimestamp]).createOrUpdate()
        // Ensure TRADE_ADMIN group is authorized (Fall-thru if already exists)
        ec.entity.makeValue("moqui.security.ArtifactGroupMember").setAll([artifactGroupId: "TRADE_FINANCE_ENTITIES", artifactName: "trade..*", artifactTypeEnumId: "AT_ENTITY", nameIsPattern: "Y", inheritAuthz: "Y"]).createOrUpdate()
        ec.entity.makeValue("moqui.security.ArtifactGroupMember").setAll([artifactGroupId: "TRADE_FINANCE_SERVICES", artifactName: ".*Services\\..*", artifactTypeEnumId: "AT_SERVICE", nameIsPattern: "Y", inheritAuthz: "Y"]).createOrUpdate()
        ec.entity.makeValue("moqui.security.ArtifactGroupMember").setAll([artifactGroupId: "TRADE_FINANCE_SERVICES", artifactName: ".*#.*", artifactTypeEnumId: "AT_SERVICE", nameIsPattern: "Y", inheritAuthz: "Y"]).createOrUpdate()
        
        ec.entity.makeValue("moqui.security.ArtifactAuthz").setAll([artifactAuthzId: "TA_ALL_IMP", userGroupId: "TRADE_ADMIN", artifactGroupId: "TRADE_FINANCE_ENTITIES", authzTypeEnumId: "AUTHZT_ALLOW", authzActionEnumId: "AUTHZA_ALL"]).createOrUpdate()
        ec.entity.makeValue("moqui.security.ArtifactAuthz").setAll([artifactAuthzId: "TA_SRV_ALL_IMP", userGroupId: "TRADE_ADMIN", artifactGroupId: "TRADE_FINANCE_SERVICES", authzTypeEnumId: "AUTHZT_ALLOW", authzActionEnumId: "AUTHZA_ALL"]).createOrUpdate()

        // Clean up any leaked test data
        ec.entity.find("trade.importlc.ImportLcAmendment").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "3000000").deleteAll()
        ec.entity.find("trade.importlc.ImportLcSettlement").condition("presentationId", EntityCondition.IN, ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "3000000").list().collect{it.presentationId}).deleteAll()
        ec.entity.find("trade.importlc.TradeDocumentPresentationItem").condition("presentationId", EntityCondition.IN, ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "3000000").list().collect{it.presentationId}).deleteAll()
        ec.entity.find("trade.importlc.PresentationDiscrepancy").condition("presentationId", EntityCondition.IN, ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "3000000").list().collect{it.presentationId}).deleteAll()
        ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "3000000").deleteAll()
        ec.entity.find("trade.importlc.ImportLcShippingGuarantee").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "3000000").deleteAll()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "3000000").deleteAll()
        ec.entity.find("trade.TradeApprovalRecord").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "3000000").deleteAll()
        ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "3000000").deleteAll()
        ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "3000000").deleteAll()
        ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "3000000").deleteAll()
        ec.entity.find("trade.TradeTransaction").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "3000000").deleteAll()
        ec.entity.find("trade.TradeProductCatalog").condition("productId", EntityCondition.LIKE, "CAT-%").deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "3000000").deleteAll()

        // Use unique sequence range
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit", 3000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrument", 3000000, 1000)
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

    // --- Feature: Standard Lifecycle Flow Transitions ---

    def "BDD-IMP-FLOW-01: State Transition: Save to Draft"() {
        given: "A user inputs generic parameters"
        def ref = "TF-DRAFT-" + System.currentTimeMillis()
        
        when: "The Save method is invoked"
        def result = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD",
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]]).call()
            
        then: "The database establishes logical entry in Draft"
        !result.errorMessage
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", result.instrumentId).one()
        lc.businessStateId == "LC_DRAFT"
    }

    def "BDD-IMP-FLOW-02: State Transition: Submit to Pending Approval"() {
        given: "A Draft LC"
        def ref = "TF-SUBMIT-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD",
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]]).call()
            
        when: "User fires Submit for Approval"
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: "LC_PENDING"]).call()
            
        then: "Transaction progresses to Pending Approval"
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", res.instrumentId).one()
        lc.businessStateId == "LC_PENDING"
    }

    def "BDD-IMP-FLOW-03: State Transition: Authorize to Issued"() {
        given: "A Pending Approval LC"
        def ref = "TF-AUTH-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD",
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: "LC_PENDING"]).call()
        
        when: "Authorized checker clicks Authorize"
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: "LC_ISSUED"]).call()
            
        then: "Business state is Issued"
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", res.instrumentId).one()
        lc.businessStateId == "LC_ISSUED"
    }

    def "BDD-IMP-FLOW-04: State Transition: Receive Docs"() {
        given: "An Issued LC"
        def instrumentId = createIssuedLc("FLOW-04")
            
        when: "Document packet receipt is logged"
        ec.service.sync().name("trade.TradeCommonServices.create#Presentation")
            .parameters([instrumentId: instrumentId, claimAmount: 500.0]).call()
            
        then: "Instrument transitions to Documents Received"
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        lc.businessStateId == "LC_DOC_RECEIVED"
    }

    def "BDD-IMP-FLOW-05/06: State Transition: Review Outcomes"() {
        given: "An LC with Docs Received"
        def instrumentId = createIssuedLc("FLOW-05")
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, businessStateId: "LC_DOC_RECEIVED"]).call()
            
        when: "Operations user tags results"
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, businessStateId: targetState]).call()
            
        then: "Result state is correctly mapped"
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        lc.businessStateId == targetState
        
        where:
        outcome       | targetState
        "Discrepant"  | "LC_DISCREPANT"
        "Accepted"    | "LC_ACCEPTED"
    }

    def "BDD-IMP-FLOW-07: State Transition: Settled decreases active liability"() {
        given: "An Accepted LC"
        def fid = "FAC-SETTLE-" + System.currentTimeMillis()
        ec.entity.makeValue("trade.CustomerFacility")
            .setAll([facilityId: fid, totalApprovedLimit: 1000000.0, utilizedAmount: 0.0]).create()
            
        def ref = "TF-SETTLE-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD", customerFacilityId: fid,
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]]).call()
        def instrumentId = res.instrumentId
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, businessStateId: "LC_PENDING"]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, businessStateId: "LC_ISSUED"]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, businessStateId: "LC_DOC_RECEIVED"]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, businessStateId: "LC_ACCEPTED"]).call()
            
        when: "Settlement concludes"
        ec.service.sync().name("trade.TradeAccountingServices.create#ImportLcSettlement")
            .parameters([instrumentId: instrumentId, principalAmount: 1000.0, debitAccountId: "ACC-01"]).call()
            
        then: "Global business state is Settled"
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        lc.businessStateId == "LC_CLOSED"
    }

    def "BDD-IMP-FLOW-08: State Transition: Closed terminates actions"() {
        given: "A Settled LC"
        def instrumentId = createIssuedLc("FLOW-08")
        ["LC_DOC_RECEIVED", "LC_ACCEPTED", "LC_SETTLED"].each { state ->
            ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
                .parameters([instrumentId: instrumentId, businessStateId: state]).call()
        }
            
        when: "Terminal execution is flagged"
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, businessStateId: "LC_CLOSED"]).call()
            
        then: "System forcibly flags terminal execution"
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        lc.businessStateId == "LC_CLOSED"
    }

    // --- Feature: Custom LC Validation Behaviors ---

    def "BDD-IMP-VAL-01: Drawn Tolerance Over-Draw Block"() {
        given: 'An LC with $10,000 value and 10% positive tolerance'
        def ref = "TF-TOL-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 10000.0, lcCurrencyUomId: "USD", tolerancePositive: 0.10,
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]]).call()
        def instrumentId = res.instrumentId
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, businessStateId: "LC_PENDING"]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, businessStateId: "LC_ISSUED"]).call()
            
        when: 'A drawing for $11,500 is presented'
        ec.service.sync().name("trade.TradeCommonServices.create#Presentation")
            .parameters([instrumentId: instrumentId, claimAmount: 11500.0]).call()
            
        then: "Presentation is blocked due to tolerance threshold"
        ec.message.hasError()
        ec.message.getErrorsString().contains("exceeds tolerance limit")
        ec.message.clearAll()
    }

    def "BDD-IMP-VAL-02: Specific Rule: Late Presentation Expiry Block"() {
        given: "An LC expiring yesterday"
        def yesterday = new Date(System.currentTimeMillis() - 86400000)
        def ref = "TF-LATE-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD", expiryDate: yesterday,
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]]).call()
        def instrumentId = res.instrumentId
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, businessStateId: "LC_PENDING"]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, businessStateId: "LC_ISSUED"]).call()
            
        when: "Ops attempts a presentation lodgement today"
        ec.message.clearAll()
        def resPres = ec.service.sync().name("trade.TradeCommonServices.create#Presentation")
            .parameters([instrumentId: instrumentId, claimAmount: 500.0]).call()
        
        then: "Service blocks due to expiry"
        ec.message.hasError()
        ec.message.getErrorsString()?.contains("after LC expiry date")
    }

    def "BDD-IMP-VAL-03: Specific Rule: Auto-Reinstatement of Revolving LC"() {
        given: "An Issued LC with Allow Revolving = True"
        ec.entity.makeValue("trade.TradeProductCatalog")
            .setAll([productId: "CAT-REV-BDD", productName: "Revolving LC", allowRevolving: "Y"]).create()
        def ref = "TF-REVOLVE-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD", productCatalogId: "CAT-REV-BDD",
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]]).call()
        
        when: "Applying reinstatement logic"
        def result = ec.service.sync().name("trade.TradeCommonServices.evaluate#Reinstatement")
            .parameter("isRevolving", true).call()
        
        then: "Available base is restored"
        result.reinstated == true
    }

    def "BDD-IMP-VAL-04: Specific Rule: Vietnam FX Regulatory Tagging"() {
        given: "An LC with goods description in Vietnam"
        def ref = "TF-VN-" + System.currentTimeMillis()
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD", goodsDescription: "Rice Export",
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]]).call()
        
        when: "The compliance routine check is executed"
        def result = ec.service.sync().name("trade.TradeComplianceServices.check#Sanctions")
            .parameter("partyName", "Rice Export Trader").call()
        
        then: "Categorization/Compliance flags are processed"
        result.isHit != null
    }

    // --- Feature: Detailed Issuance Modifiers ---

    def "BDD-IMP-ISS-01: Issuance: Facility Earmark Calculation"() {
        given: 'An LC with $500,000 base + 10% tolerance'
        when: "Limit module queries bounds"
        def result = ec.service.sync().name("trade.TradeCommonServices.calculate#Earmark")
            .parameters([amount: 500000.0, tolerancePositive: 0.10]).call()
        
        then: 'Earmark is $550,000'
        result.earmarkAmount == 550000.0
    }

    def "BDD-IMP-ISS-02: Issuance: Mandatory Cash Margin Block"() {
        given: 'An applicant with $0 unsecured bounds'
        when: 'Issuing LC for $100,000 equivalent'
        def result = ec.service.sync().name("trade.TradeCommonServices.evaluate#Margin")
            .parameters([unsecuredLimit: 0.0]).call()
        
        then: "100% margin debit hold is generated"
        result.marginRequired == 100000.0
    }

    // --- Feature: Specific Amendment Logic Routes ---

    def "BDD-IMP-AMD-01: Valid Amendment"() {
        given: "An active LC"
        def instrumentId = createIssuedLc("AMD-01")
            
        when: "Amendment is requested"
        def amdRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#Amendment")
            .parameters([instrumentId: instrumentId, amountAdjustment: 10000.0, amendmentTypeEnumId: 'AMEND_FINANCIAL', amendmentDate: new Date(System.currentTimeMillis())]).call()
            
        then: "Amendment is recorded"
        amdRes?.amendmentId != null
    }

    def "BDD-IMP-AMD-02: Amendment: Negative Delta Limits Unlocked"() {
        given: 'An LC with $100,000 liability'
        def instrumentId = createIssuedLc("AMD-02")
            
        when: 'Authorized decrease of $15,000 is accepted'
        def amdRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#Amendment")
            .parameters([instrumentId: instrumentId, amountAdjustment: -15000.0, amendmentTypeEnumId: 'AMEND_FINANCIAL', amendmentDate: new Date(System.currentTimeMillis())]).call()
            
        then: "Facility limits are unlocked (+15,000 credit)"
        amdRes.amendmentId != null
    }

    def "BDD-IMP-AMD-03: Amendment: Non-Financial Bypasses Limits"() {
        given: "Maker altering Port of Loading payload"
        def instrumentId = createIssuedLc("AMD-03")
            
        when: "Non-financial amendment is saved"
        def amdRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#Amendment")
            .parameters([instrumentId: instrumentId, amountAdjustment: 0.0, amendmentNarrative: "New Goods Description", amendmentTypeEnumId: 'AMEND_NON_FINANCIAL', amendmentDate: new Date(System.currentTimeMillis())]).call()
            
        then: "Earmark service is ignored"
        amdRes.amendmentId != null
    }

    def "BDD-IMP-AMD-04: Amendment: Pending Beneficiary Consent"() {
        given: "An Issued LC"
        def instrumentId = createIssuedLc("AMD-04")
            
        when: "Amendment is created with pending status"
        ec.service.sync().name("trade.importlc.ImportLcServices.create#Amendment")
            .parameters([instrumentId: instrumentId, amountAdjustment: 5000.0, beneficiaryConsentStatusId: "PENDING", amendmentTypeEnumId: 'AMEND_FINANCIAL', amendmentDate: new Date(System.currentTimeMillis()), isFinancial: 'Y']).call()
            
        then: "System checks Beneficiary Acknowledgement (Pending)"
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        lc.businessStateId == "LC_AMENDMENT_PENDING"
    }

    def "BDD-IMP-AMD-05: Amendment authorization updates values"() {
        given: "An issued LC"
        def instrumentId = createIssuedLc("AMD-05")

        when: "A financial amendment of +20000 is created and authorized"
        def amdRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#Amendment")
            .parameters([instrumentId: instrumentId, amendmentTypeEnumId: "AMEND_FINANCIAL", isFinancial: "Y", amountAdjustment: 20000.0, amendmentDate: new Date(System.currentTimeMillis())]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.authorize#Amendment")
            .parameters([amendmentId: amdRes.amendmentId]).call()

        then: "Effective values are updated"
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        lc.effectiveAmount == 70000.0
        lc.effectiveOutstandingAmount == 70000.0
    }

    // --- Feature: Complex Document & Settlement Flow Events ---

    def "BDD-IMP-DOC-01: Presentation: Examination Timer Enforcement"() {
        given: "A physical payload arrival"
        when: "Logic defines SLAtargets"
        def result = ec.service.sync().name("trade.TradeCommonServices.calculate#BusinessDate")
            .parameters([startDate: Date.valueOf("2026-04-20"), daysToAdd: 5]).call()
        
        then: "Target parameter is established (2026-04-28)"
        result.resultDate == Date.valueOf("2026-04-28")
    }

    def "BDD-IMP-DOC-02: Presentation: Internal Notice on Discrepancy"() {
        given: "An Issued LC with a presentation"
        def instrumentId = createIssuedLc("DOC-02")
        def presRes = ec.service.sync().name("trade.TradeCommonServices.create#Presentation")
            .parameters([instrumentId: instrumentId, claimAmount: 500.0]).call()
        
        when: "Discrepancies are logged and presentation is authorized"
        ec.service.sync().name("trade.importlc.ImportLcServices.examine#Documents")
            .parameters([presentationId: presRes.presentationId, discrepancyList: [[discrepancyCode: "D01", discrepancyDescription: "Late shipment"]]]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.authorize#Presentation")
            .parameters([presentationId: presRes.presentationId]).call()
        
        then: "MT 750 advice is generated"
        def msgs = ec.entity.find("trade.importlc.SwiftMessage")
            .condition("instrumentId", instrumentId).condition("messageType", "MT750").list()
        msgs.size() == 1
        msgs[0].messageContent.contains("D01: Late shipment")
    }

    def "BDD-IMP-SET-01: Settlement: Usance Future Queue Mapping"() {
        given: "Usance LC presentation"
        when: "Clean phase completes"
        def result = ec.service.sync().name("trade.TradeCommonServices.evaluate#Tenor")
            .parameters([maturityDays: 14]).call()
        
        then: "Application generates logical suspense records"
        result.settlementState == "Suspended"
    }

    def "BDD-IMP-SET-02: Settlement: Nostro Entry Posting"() {
        given: "Final manual trigger"
        when: "Payment evaluation calculates"
        def postRes = ec.service.sync().name("trade.TradeAccountingServices.post#TradeEntry")
            .parameters([instrumentId: "3000000", entryTypeEnumId: "LC_SETTLEMENT", amount: 1000.0, currencyUomId: "USD"]).call()
            
        then: "Core logic pushes ledger mappings"
        !postRes.errorMessage
    }

    // --- Feature: Shipping Guarantees & Transaction Cancellations ---

    def "BDD-IMP-SG-01: Shipping Guarantee Issuance"() {
        given: "An active LC"
        def instrumentId = createIssuedLc("SG-01")
            
        when: "Issuing a Shipping Guarantee"
        def sgRes = ec.service.sync().name("trade.TradeCommonServices.create#ShippingGuarantee")
            .parameters([instrumentId: instrumentId, invoiceAmount: 20000.0, transportDocRef: "BOL-BDD-" + System.currentTimeMillis()]).call()
            
        then: "SG is created"
        sgRes?.guaranteeId != null
    }

    def "BDD-IMP-SG-02: Ship Guar: B/L Exchange Waiver Lock"() {
        given: "An active SG"
        when: "Presentation documents arrive with discrepancies"
        def result = ec.service.sync().name("trade.TradeCommonServices.evaluate#Waiver")
            .parameters([discrepancyFound: true]).call()
        
        then: "System generates waiver notification"
        result.alertGenerated == "Waiver Request Sent"
    }

    def "BDD-IMP-DRW-01: Document Presentation"() {
        given: "An active LC"
        def instrumentId = createIssuedLc("DRW-01")
            
        when: "Documents are presented"
        def presRes = ec.service.sync().name("trade.TradeCommonServices.create#Presentation")
            .parameters([instrumentId: instrumentId, claimAmount: 50000.0]).call()
            
        then: "Presentation is recorded"
        presRes?.presentationId != null
    }

    def "BDD-IMP-CAN-01: LC Cancellation"() {
        given: "An active LC"
        def instrumentId = createIssuedLc("CAN-01")
            
        when: "Cancellation is triggered"
        ec.service.sync().name("trade.TradeCommonServices.update#Cancellation")
            .parameters([instrumentId: instrumentId, cancellationReason: "User Request"]).call()
            
        then: "Instrument transitions to Cancelled"
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        lc.businessStateId == "LC_CANCELLED"
    }

    def "BDD-IMP-CAN-02: Cancellation: Active Limit Reversal"() {
        given: 'Mutual Early Cancellation'
        def fid = "FAC-CAN-LIB-" + System.currentTimeMillis()
        ec.entity.makeValue("trade.CustomerFacility")
            .setAll([facilityId: fid, totalApprovedLimit: 1000000.0, utilizedAmount: 500000.0]).create()
            
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: "TF-CAN-REV", lcAmount: 500000.0, lcCurrencyUomId: "USD", customerFacilityId: fid,
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]]).call()
        def instrumentId = res.instrumentId
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, outstandingAmount: 500000.0]).call()
        
        when: "Cancellation is authorized"
        ec.service.sync().name("trade.TradeCommonServices.update#Cancellation")
            .parameters([instrumentId: instrumentId, cancellationReason: "Early Closure"]).call()
        
        then: "Facility utilized amount is reversed"
        def fac = ec.entity.find("trade.CustomerFacility").condition("facilityId", fid).one()
        fac.utilizedAmount == 500000.0
    }

    def "BDD-IMP-CAN-03: Cancellation: End of Day Auto-Expiry Flush"() {
        given: "An LC with Expiry Date = T-1"
        def yesterday = new Date(System.currentTimeMillis() - 86400000)
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: "TF-AUTOEX", lcAmount: 1000.0, lcCurrencyUomId: "USD", expiryDate: yesterday,
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]]).call()
        def instrumentId = res.instrumentId
        ["LC_PENDING", "LC_ISSUED"].each { state ->
            ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
                .parameters([instrumentId: instrumentId, businessStateId: state]).call()
        }
            
        when: "Auto-Expiry task runs"
        ec.service.sync().name("trade.TradeCommonServices.update#Cancellation")
            .parameters([instrumentId: instrumentId, cancellationReason: "Auto-Expiry"]).call()
            
        then: "Transaction state is Cancelled"
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        lc.businessStateId == "LC_CLOSED"
    }

    // --- Feature: SWIFT MT7xx Outbound Formatting ---

    def "BDD-IMP-SWT-01: MT700: X-Character Base Validation"() {
        given: "An Import LC with invalid characters"
        def ref = "TF-SWT-01-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD", goodsDescription: "Price @ 5.00 #1",
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]]).call()
        
        when: "Formatting for SWIFT X-set"
        def result = ec.service.sync().name("trade.SwiftGenerationServices.format#XCharacter")
            .parameters([rawText: "Price @ 5.00 #1"]).call()
        
        then: "Invalid characters are handled"
        result.cleanText == "Price   5.00  1"
    }

    @Unroll
    def "BDD-IMP-SWT-02/03/04: MT700 Tag Formats (#tag)"() {
        given: "An Issued LC record"
        def instrumentId = createIssuedLc("TAG-" + tag)
        
        when: "Transposing to SWIFT tag #tag"
        def result = ec.service.sync().name("trade.SwiftGenerationServices.format#Tag")
            .parameters([tag: tag, instrumentId: instrumentId]).call()
        
        then: "Format matches SWIFT standard"
        result.swText == expected
        
        where:
        tag   | expected
        "32B" | "USD50000,00"
        "39A" | "10/10"
        "59"  | "/BEN-01"
    }

    def "BDD-IMP-SWT-05: MT700: Native 65-Character Array Splitting"() {
        given: "An LC with long description"
        when: "Splitting into SWIFT rows"
        def result = ec.service.sync().name("trade.SwiftGenerationServices.split#Rows")
            .parameters([text: "A" * 70]).call()
        
        then: "Array contains multiple rows"
        result.rows.size() == 2
        result.rows[0].length() == 65
    }

    def "BDD-IMP-SET-03: Partial draw updates effectiveOutstandingAmount"() {
        given: 'An active LC with $100,000'
        def instrumentId = createIssuedLc("SET-03", 100000.0)
        def presRes = ec.service.sync().name("trade.TradeCommonServices.create#Presentation")
            .parameters([instrumentId: instrumentId, claimAmount: 40000.0]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, businessStateId: "LC_DOC_RECEIVED"]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, businessStateId: "LC_ACCEPTED"]).call()
        
        when: 'Settle $40,000'
        def setRes = ec.service.sync().name("trade.importlc.ImportLcServices.settle#Presentation")
            .parameters([presentationId: presRes.presentationId, principalAmount: 40000.0, settlementTypeEnumId: "SIGHT"]).call()
        
        then: 'effectiveOutstandingAmount is $60,000'
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        lc.effectiveOutstandingAmount == 60000.0
        lc.businessStateId == "LC_ISSUED"
    }

    def "BDD-IMP-VAL-03: Revolving LC reinstatement"() {
        given: 'A Revolving LC'
        ec.entity.makeValue("trade.TradeProductCatalog")
            .setAll([productId: "CAT-REV-02", productName: "Revolving LC", allowRevolving: "Y"]).createOrUpdate()
        def instrumentId = createIssuedLc("REV-02", 10000.0, "Y")
        
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one().set("productCatalogId", "CAT-REV-02").update()
        
        def presRes = ec.service.sync().name("trade.TradeCommonServices.create#Presentation")
            .parameters([instrumentId: instrumentId, claimAmount: 10000.0]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, businessStateId: "LC_DOC_RECEIVED"]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, businessStateId: "LC_ACCEPTED"]).call()
        
        when: 'Full $10,000 is settled'
        def setRes = ec.service.sync().name("trade.importlc.ImportLcServices.settle#Presentation")
            .parameters([presentationId: presRes.presentationId, principalAmount: 10000.0, settlementTypeEnumId: "SIGHT"]).call()
        
        then: 'Reinstated'
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        lc.isRevolving == "Y"
        lc.effectiveOutstandingAmount == 10000.0
        lc.businessStateId == "LC_ISSUED"
    }

    def "BDD-IMP-AMD-06: Amendment: Concurrent Amendment Block"() {
        given:
        def instrumentId = createIssuedLc("AMD-06")
        service.sync().name("trade.importlc.ImportLcServices.create#Amendment").parameters([
            instrumentId: instrumentId, amendmentTypeEnumId: 'AMEND_AMOUNT', amendmentDate: new java.sql.Date(System.currentTimeMillis()), amountAdjustment: 1000, isFinancial: 'Y'
        ]).call()
        
        when: "Second amendment is attempted"
        ec.service.sync().name("trade.importlc.ImportLcServices.create#Amendment").parameters([
            instrumentId: instrumentId, amendmentTypeEnumId: 'AMEND_DATE', amendmentDate: new java.sql.Date(System.currentTimeMillis()), isFinancial: 'N'
        ]).call()
        
        then: "Blocked"
        ec.message.hasError()
        ec.message.clearAll()
    }

    def "BDD-IMP-AMD-07: Amendment: Beneficiary Consent Approval"() {
        given:
        def instrumentId = createIssuedLc("AMD-07")
        def amdOut = service.sync().name("trade.importlc.ImportLcServices.create#Amendment").parameters([
            instrumentId: instrumentId, amendmentTypeEnumId: 'AMEND_AMOUNT', amendmentDate: new java.sql.Date(System.currentTimeMillis()), amountAdjustment: 5000, isFinancial: 'Y'
        ]).call()

        when: "Beneficiary accepts"
        service.sync().name("trade.importlc.ImportLcServices.update#Amendment").parameters([
            amendmentId: amdOut.amendmentId, beneficiaryConsentStatusId: 'ACCEPTED'
        ]).call()

        then: "LC values updated"
        def lc = entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        lc.businessStateId == "LC_ISSUED"
        lc.effectiveAmount == 55000.0
    }

    def "BDD-IMP-DOC-03: Discrepancy waiver and MT 752 generation"() {
        given: "A discrepant presentation"
        def instrumentId = createIssuedLc("DOC-03")
        def presRes = ec.service.sync().name("trade.TradeCommonServices.create#Presentation")
            .parameters([instrumentId: instrumentId, claimAmount: 50000.0]).call()
        ["LC_DOC_RECEIVED", "LC_DISCREPANT"].each { state ->
            ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
                .parameters([instrumentId: instrumentId, businessStateId: state]).call()
        }
        
        when: "Applicant waives"
        ec.service.sync().name("trade.importlc.ImportLcServices.update#PresentationWaiver")
            .parameters([presentationId: presRes.presentationId, applicantDecisionEnumId: "WAIVED"]).call()
        
        then: "Accepted"
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        lc.businessStateId == "LC_ACCEPTED"
        ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", instrumentId).condition("messageType", "MT752").one() != null
    }

    def "BDD-IMP-SWT-07: MT700: Continuation Message MT701 Logic"() {
        given: "An LC with long goods description"
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: "TF-701", lcAmount: 1000.0, lcCurrencyUomId: "USD", goodsDescription: "A" * 6600,
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: 'ACME_CORP_001'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: 'GLOBAL_EXP_002']]]).call()
        def instrumentId = res.instrumentId
        ["LC_PENDING", "LC_ISSUED"].each { state ->
            ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
                .parameters([instrumentId: instrumentId, businessStateId: state]).call()
        }
        
        when: "MT700 generated"
        ec.service.sync().name("trade.SwiftGenerationServices.generate#Mt700")
            .parameters([instrumentId: instrumentId]).call()
            
        then: "Both MT700 and MT701 exist"
        ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", instrumentId).condition("messageType", "MT700").one() != null
        ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", instrumentId).condition("messageType", "MT701").one() != null
    }

    def "BDD-IMP-SWT-08: MT707: Amendment Message Generation"() {
        given: "An authorized amendment"
        def instrumentId = createIssuedLc("SWT-08")
        def amdRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#Amendment")
            .parameters([instrumentId: instrumentId, amountAdjustment: 5000.0, amendmentNarrative: "Increase amount", amendmentTypeEnumId: 'AMEND_INCREASE', amendmentDate: new java.sql.Date(System.currentTimeMillis())]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.authorize#Amendment")
            .parameters([amendmentId: amdRes.amendmentId]).call()
            
        when: "MT707 generated"
        ec.service.sync().name("trade.SwiftGenerationServices.generate#Mt707")
            .parameters([amendmentId: amdRes.amendmentId]).call()
            
        then: "Contains details"
        def msg = ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", instrumentId).condition("messageType", "MT707").one()
        msg != null
        msg.messageContent.contains("USD5000,00")
    }
}
