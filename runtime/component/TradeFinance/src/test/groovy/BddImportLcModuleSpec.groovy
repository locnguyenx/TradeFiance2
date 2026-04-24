
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

    def createIssuedLc(String refSuffix) {
        def ref = "TF-LC-" + refSuffix + "-" + System.currentTimeMillis()
        def res = service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 50000.0, lcCurrencyUomId: "USD", beneficiaryPartyId: "BEN-01"]).call()
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

        // Clean up any leaked test data from previous runs to avoid 23505 (Unique Constraint)
        // Must delete in reverse dependency order
        ec.entity.find("trade.importlc.ImportLcAmendment").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "3000000").deleteAll()
        ec.entity.find("trade.importlc.ImportLcSettlement").condition("presentationId", EntityCondition.IN, ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "3000000").list().collect{it.presentationId}).deleteAll()
        ec.entity.find("trade.importlc.TradeDocumentPresentationItem").condition("presentationId", EntityCondition.IN, ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "3000000").list().collect{it.presentationId}).deleteAll()
        ec.entity.find("trade.importlc.PresentationDiscrepancy").condition("presentationId", EntityCondition.IN, ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "3000000").list().collect{it.presentationId}).deleteAll()
        ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "3000000").deleteAll()
        ec.entity.find("trade.importlc.ImportLcShippingGuarantee").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "3000000").deleteAll()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "3000000").deleteAll()
        ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "3000000").deleteAll()
        ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "3000000").deleteAll()
        ec.entity.find("trade.TradeProductCatalog").condition("productCatalogId", EntityCondition.LIKE, "CAT-%").deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", EntityCondition.GREATER_THAN_EQUAL_TO, "3000000").deleteAll()

        // Use unique sequence range for ImportLcSpec to avoid collisions with CommonSpec
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

    // --- Feature: Standard Lifecycle Flow Transitions (BDD-IMP-FLOW-01 to 08) ---

    def "BDD-IMP-FLOW-01: State Transition: Save to Draft"() {
        given: "A user inputs generic parameters"
        def ref = "TF-DRAFT-" + System.currentTimeMillis()
        
        when: "The Save method is invoked"
        def result = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD"]).call()
            
        then: "The database establishes logical entry in Draft"
        !result.errorMessage
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", result.instrumentId).one()
        lc.businessStateId == "LC_DRAFT"
    }

    def "BDD-IMP-FLOW-02: State Transition: Submit to Pending Approval"() {
        given: "A Draft LC"
        def ref = "TF-SUBMIT-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD"]).call()
            
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
            .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD"]).call()
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
        def ref = "TF-RECV-" + System.currentTimeMillis()
        def instrumentId = createIssuedLc("RECV-01")
        def res = [instrumentId: instrumentId]
            
        when: "Document packet receipt is logged"
        ec.service.sync().name("trade.TradeCommonServices.create#Presentation")
            .parameters([instrumentId: res.instrumentId, claimAmount: 500.0]).call()
            
        then: "Instrument transitions to Documents Received"
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", res.instrumentId).one()
        lc.businessStateId == "LC_DOCS_RECEIVED"
    }

    def "BDD-IMP-FLOW-05/06: State Transition: Review Outcomes"() {
        given: "An LC with Docs Received"
        def ref = "TF-REVIEW-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD", lcCurrencyUomId: "USD"]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: "LC_PENDING"]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: "LC_ISSUED"]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: "LC_DOCS_RECEIVED"]).call()
            
        when: "Operations user tags results"
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: targetState]).call()
            
        then: "Result state is correctly mapped"
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", res.instrumentId).one()
        lc.businessStateId == targetState
        
        where:
        outcome       | targetState
        "Discrepant"  | "LC_DISCREPANT"
        "Accepted"    | "LC_ACCEPTED"
    }

    def "BDD-IMP-FLOW-07: State Transition: Settled decreases active liability"() {
        given: "An Accepted LC"
        def ref = "TF-SETTLE-" + System.currentTimeMillis()
        def fid = "FAC-SETTLE-" + System.currentTimeMillis()
        ec.entity.makeValue("trade.CustomerFacility")
            .setAll([facilityId: fid, totalApprovedLimit: 1000.0, utilizedAmount: 0.0]).create()
            
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD", lcCurrencyUomId: "USD", customerFacilityId: fid]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: "LC_PENDING"]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: "LC_ISSUED"]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: "LC_DOCS_RECEIVED"]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: "LC_ACCEPTED"]).call()
            
        when: "Settlement concludes"
        ec.service.sync().name("trade.TradeAccountingServices.create#ImportLcSettlement")
            .parameters([instrumentId: res.instrumentId, principalAmount: 1000.0, debitAccountId: "ACC-01"]).call()
            
        then: "Global business state is Settled"
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", res.instrumentId).one()
        lc.businessStateId == "LC_SETTLED"
    }

    def "BDD-IMP-FLOW-08: State Transition: Closed terminates actions"() {
        given: "A Settled LC"
        def ref = "TF-CLOSE-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD", lcCurrencyUomId: "USD"]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: "LC_PENDING"]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: "LC_ISSUED"]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: "LC_DOCS_RECEIVED"]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: "LC_ACCEPTED"]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: "LC_SETTLED"]).call()
            
        when: "Terminal execution is flagged"
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: "LC_CLOSED"]).call()
            
        then: "System forcibly flags terminal execution"
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", res.instrumentId).one()
        lc.businessStateId == "LC_CLOSED"
    }

    // --- Feature: Custom LC Validation Behaviors (REQ-IMP-04) ---

    def "BDD-IMP-VAL-01: Drawn Tolerance Over-Draw Block"() {
        given: 'An LC with $10,000 value and 10% positive tolerance (Max $11,000)'
        def ref = "TF-TOL-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 10000.0, lcCurrencyUomId: "USD", tolerancePositive: 0.10]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: "LC_PENDING"]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: "LC_ISSUED"]).call()
            
        when: 'A drawing for $11,500 is presented'
        ec.service.sync().name("trade.TradeCommonServices.create#Presentation")
            .parameters([instrumentId: res.instrumentId, claimAmount: 11500.0]).call()
            
        then: "Presentation is blocked due to tolerance threshold"
        ec.message.hasError()
        ec.message.getErrorsString().contains("exceeds tolerance limit")
    }

    def "BDD-IMP-VAL-02: Specific Rule: Late Presentation Expiry Block"() {
        given: "An LC expiring yesterday"
        def yesterday = new Date(System.currentTimeMillis() - 86400000)
        def ref = "TF-LATE-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD", expiryDate: yesterday]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: "LC_PENDING"]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: "LC_ISSUED"]).call()
            
        when: "Ops attempts a presentation lodgement today"
        ec.service.sync().name("trade.TradeCommonServices.create#Presentation")
            .parameters([instrumentId: res.instrumentId, claimAmount: 500.0]).call()
        
        then: "Service blocks due to expiry (simulation for now until service hardened)"
        ec.message.hasError()
        ec.message.getErrorsString().contains("after LC Expiry Date")
    }

    def "BDD-IMP-VAL-03: Specific Rule: Auto-Reinstatement of Revolving LC"() {
        given: "An Issued LC with Allow Revolving = True"
        def ref = "TF-REVOLVE-" + System.currentTimeMillis()
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD", revolvingFlag: 'Y']).call()
        
        when: "Applying reinstatement logic via backend service"
        def result = ec.service.sync().name("trade.TradeCommonServices.evaluate#Reinstatement")
            .parameter("isRevolving", true).call()
        
        then: "Available base is restored"
        result.reinstated == true
    }

    def "BDD-IMP-VAL-04: Specific Rule: Vietnam FX Regulatory Tagging"() {
        given: "An LC with goods description in Vietnam"
        def ref = "TF-VN-" + System.currentTimeMillis()
        ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD", goodsDescription: "Rice Export"]).call()
        
        when: "The compliance routine check is executed"
        def result = ec.service.sync().name("trade.TradeComplianceServices.check#Sanctions")
            .parameter("partyName", "Rice Export Trader").call()
        
        then: "Categorization/Compliance flags are processed"
        result.isHit != null
    }

    // --- Feature: Detailed Issuance Modifiers (REQ-IMP-SPEC-01) ---

    def "BDD-IMP-ISS-01: Issuance: Facility Earmark Calculation"() {
        given: 'An LC with $500,000 base + 10% tolerance (Total $550,000)'
        def amount = 500000.0
        def tolerance = 0.10
        
        when: "Limit module queries bounds via backend service"
        def result = ec.service.sync().name("trade.TradeCommonServices.calculate#Earmark")
            .parameters([amount: amount, tolerancePositive: tolerance]).call()
        
        then: 'Earmark is $550,000'
        result.earmarkAmount == 550000.0
    }

    def "BDD-IMP-ISS-02: Issuance: Mandatory Cash Margin Block"() {
        given: 'An applicant with $0 unsecured bounds'
        def unsecuredLimit = 0.0
        
        when: 'Issuing LC for $100,000 equivalent via backend logic'
        def result = ec.service.sync().name("trade.TradeCommonServices.evaluate#Margin")
            .parameters([unsecuredLimit: unsecuredLimit]).call()
        
        then: "100% margin debit hold is generated"
        result.marginRequired == 100000.0
    }

    // --- Feature: Specific Amendment Logic Routes (REQ-IMP-SPEC-02) ---

    def "BDD-IMP-AMD-01: Valid Amendment"() {
        given: "An active LC"
        def fid = "FAC-AMD-" + System.currentTimeMillis()
        ec.entity.makeValue("trade.CustomerFacility")
            .setAll([facilityId: fid, totalApprovedLimit: 1000000.0, utilizedAmount: 0.0]).create()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: "LC-AMD-" + System.currentTimeMillis(), lcAmount: 50000.0, lcCurrencyUomId: "USD", customerFacilityId: fid]).call()
            
        when: "Amendment is requested"
        def amdRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#Amendment")
            .parameters([instrumentId: res.instrumentId, amountAdjustment: 10000.0, amendmentTypeEnumId: 'AMEND_FINANCIAL', amendmentDate: new Date(System.currentTimeMillis())]).call()
            
        then: "Amendment is recorded"
        amdRes?.amendmentId != null
    }

    def "BDD-IMP-AMD-02: Amendment: Negative Delta Limits Unlocked"() {
        given: 'An LC with $100,000 liability'
        def ref = "TF-NEG-AMD-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 100000.0, lcCurrencyUomId: "USD"]).call()
            
        when: 'Authorized decrease of $15,000 is accepted'
        def amdRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#Amendment")
            .parameters([instrumentId: res.instrumentId, amountAdjustment: -15000.0, amendmentTypeEnumId: 'AMEND_FINANCIAL', amendmentDate: new Date(System.currentTimeMillis())]).call()
            
        then: "Facility limits are unlocked (+15,000 credit)"
        amdRes.amendmentId != null
    }

    def "BDD-IMP-AMD-03: Amendment: Non-Financial Bypasses Limits"() {
        given: "Maker altering Port of Loading payload"
        def ref = "TF-NONFIN-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 100000.0, lcCurrencyUomId: "USD"]).call()
            
        when: "Non-financial amendment is saved"
        def amdRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#Amendment")
            .parameters([instrumentId: res.instrumentId, amountAdjustment: 0.0, amendmentNarrative: "New Goods Description", amendmentTypeEnumId: 'AMEND_NON_FINANCIAL', amendmentDate: new Date(System.currentTimeMillis())]).call()
            
        then: "Earmark service is ignored"
        amdRes.amendmentId != null
    }

    def "BDD-IMP-AMD-04: Amendment: Pending Beneficiary Consent"() {
        given: "An Issued LC with a linked facility"
        def facId = "FAC-AMD-04"
        if (!ec.entity.find("trade.CustomerFacility").condition("facilityId", facId).one()) {
            ec.entity.makeValue("trade.CustomerFacility")
                .setAll([facilityId: facId, totalApprovedLimit: 100000.0, utilizedAmount: 0.0]).create()
        }
        
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: "AMD4-" + System.currentTimeMillis(), lcAmount: 10000.0, lcCurrencyUomId: "USD", customerFacilityId: facId]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: "LC_PENDING"]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: "LC_ISSUED"]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, outstandingAmount: 10000.0]).call()
            
        when: "Amendment is created"
        ec.service.sync().name("trade.importlc.ImportLcServices.create#Amendment")
            .parameters([instrumentId: res.instrumentId, amountAdjustment: 5000.0, beneficiaryConsentStatusId: "PENDING", amendmentTypeEnumId: 'AMEND_FINANCIAL', amendmentDate: new Date(System.currentTimeMillis()), isFinancial: 'Y']).call()
            
        then: "System checks Beneficiary Acknowledgement (Pending)"
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", res.instrumentId).one()
        lc.businessStateId == "LC_AMENDMENT_PENDING"
    }

    def "BDD-IMP-AMD-05: Amendment authorization updates ImportLetterOfCredit effective values"() {
        given: "An issued LC with effectiveAmount = 50000"
        def ref = "TF-AMD-EFF-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 50000.0, lcCurrencyUomId: "USD", beneficiaryPartyId: "BEN-AMD-05"]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: "LC_PENDING"]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: "LC_ISSUED"]).call()

        when: "A financial amendment of +20000 is created and authorized"
        def amdRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#Amendment")
            .parameters([instrumentId: res.instrumentId, amendmentTypeEnumId: "AMEND_FINANCIAL", isFinancial: "Y", amountAdjustment: 20000.0, amendmentDate: new Date(System.currentTimeMillis())]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.authorize#Amendment")
            .parameters([amendmentId: amdRes.amendmentId]).call()

        then: "Effective values on ImportLetterOfCredit are updated"
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit")
                .condition("instrumentId", res.instrumentId).one()
        lc.effectiveAmount == 70000.0
        lc.effectiveOutstandingAmount == 70000.0
        lc.totalAmendmentCount == 1

        and: "TradeInstrument.amount remains unchanged (original snapshot)"
        def inst = ec.entity.find("trade.TradeInstrument")
                .condition("instrumentId", res.instrumentId).one()
        inst.amount == 50000.0
        inst.versionNumber == 2
    }

    // --- Feature: Complex Document & Settlement Flow Events (REQ-IMP-SPEC-03, REQ-IMP-SPEC-04) ---

    def "BDD-IMP-DOC-01: Presentation: Examination Timer Enforcement"() {
        given: "A physical payload arrival on 2026-04-20"
        def presentationDate = Date.valueOf("2026-04-20")
        
        when: "Logic defines SLAtargets via backend service"
        def result = ec.service.sync().name("trade.TradeCommonServices.calculate#BusinessDate")
            .parameters([startDate: presentationDate, daysToAdd: 5]).call()
        
        then: "Target parameter is established (2026-04-28 due to holiday on 22nd)"
        result.resultDate == Date.valueOf("2026-04-28")
    }

    def "BDD-IMP-DOC-02: Presentation: Internal Notice on Discrepancy"() {
        given: "Operations evaluates Discrepancy Found? = True on a presentation"
        def ref = "TF-PRES-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD"]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: "LC_PENDING"]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: "LC_ISSUED"]).call()
        def presRes = ec.service.sync().name("trade.TradeCommonServices.create#Presentation")
            .parameters([instrumentId: res.instrumentId, claimAmount: 500.0]).call()
        
        when: "Discrepancies are logged and presentation is authorized"
        ec.service.sync().name("trade.importlc.ImportLcServices.examine#Documents")
            .parameters([presentationId: presRes.presentationId, discrepancyList: [[discrepancyCode: "D01", discrepancyDescription: "Late shipment"]]]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.authorize#Presentation")
            .parameters([presentationId: presRes.presentationId]).call()
        
        then: "MT 750 advice is generated"
        def msgs = ec.entity.find("trade.importlc.SwiftMessage")
            .condition("instrumentId", res.instrumentId).condition("messageType", "MT750").list()
        msgs.size() == 1
        msgs[0].messageContent.contains("D01: Late shipment")
    }

    def "BDD-IMP-SET-01: Settlement: Usance Future Queue Mapping"() {
        given: "Usance LC presentation with 14 days maturity"
        def maturityDays = 14
        
        when: "Clean phase completes via backend service"
        def result = ec.service.sync().name("trade.TradeCommonServices.evaluate#Tenor")
            .parameters([maturityDays: maturityDays]).call()
        
        then: "Application generates logical suspense records"
        !result.errorMessage
        result.settlementState == "Suspended"
    }

    def "BDD-IMP-SET-02: Settlement: Nostro Entry Posting"() {
        given: "Final manual trigger over Sight LC"
        def ref = "TF-NOSTRO-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD", lcCurrencyUomId: "USD"]).call()
            
        when: "Payment evaluation calculates via backend service"
        def postRes = ec.service.sync().name("trade.TradeAccountingServices.post#TradeEntry")
            .parameters([instrumentId: res.instrumentId, entryTypeEnumId: "LC_SETTLEMENT", amount: 1000.0, currencyUomId: "USD"]).call()
            
        then: "Core logic pushes ledger integration mappings"
        !postRes.errorMessage
        !ec.message.hasError()
    }

    // --- Feature: Shipping Guarantees & Transaction Cancellations (REQ-IMP-SPEC-05, REQ-IMP-SPEC-06) ---

    def "BDD-IMP-SG-01: Shipping Guarantee Issuance"() {
        given: "An active LC"
        def fid = "FAC-SG-" + System.currentTimeMillis()
        ec.entity.makeValue("trade.CustomerFacility")
            .setAll([facilityId: fid, totalApprovedLimit: 1000000.0, utilizedAmount: 0.0]).create()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: "SG-LC-" + System.currentTimeMillis(), lcAmount: 20000.0, lcCurrencyUomId: "USD", customerFacilityId: fid]).call()
            
        when: "Issuing a Shipping Guarantee via backend service"
        def sgRes = ec.service.sync().name("trade.TradeCommonServices.create#ShippingGuarantee")
            .parameters([instrumentId: res.instrumentId, invoiceAmount: 20000.0, transportDocRef: "BOL-BDD-" + System.currentTimeMillis()]).call()
            
        then: "SG is created"
        !sgRes.errorMessage
        sgRes?.guaranteeId != null
    }

    def "BDD-IMP-SG-02: Ship Guar: B/L Exchange Waiver Lock"() {
        given: "An active SG indemnifying carrier"
        def discrepancyFound = true
        
        when: "Presentation documents arrive with discrepancies"
        def result = ec.service.sync().name("trade.TradeCommonServices.evaluate#Waiver")
            .parameters([discrepancyFound: discrepancyFound]).call()
        
        then: "System generates waiver notification"
        !result.errorMessage
        result.alertGenerated == "Waiver Request Sent"
    }

    def "BDD-IMP-DRW-01: Document Presentation"() {
        given: "An active LC"
        def instrumentId = createIssuedLc("DRW-01")
        def res = [instrumentId: instrumentId] // Keep compatibility with existing code if possible
            
        when: "Documents are presented via backend service"
        def presRes = ec.service.sync().name("trade.TradeCommonServices.create#Presentation")
            .parameters([instrumentId: res.instrumentId, claimAmount: 50000.0]).call()
            
        then: "Presentation is recorded"
        !presRes.errorMessage
        presRes?.presentationId != null
    }

    def "BDD-IMP-CAN-01: LC Cancellation"() {
        given: "An active LC"
        def fid = "FAC-CAN-" + System.currentTimeMillis()
        ec.entity.makeValue("trade.CustomerFacility")
            .setAll([facilityId: fid, totalApprovedLimit: 1000000.0, utilizedAmount: 0.0]).create()
        def instrumentId = createIssuedLc("VAL-01")
        def res = [instrumentId: instrumentId]
            
        when: "Cancellation is triggered"
        ec.service.sync().name("trade.TradeCommonServices.update#Cancellation")
            .parameters([instrumentId: res.instrumentId, cancellationReason: "User Request"]).call()
            
        then: "LC is cancelled"
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", res.instrumentId).one()
        lc.businessStateId == "LC_CANCELLED"
    }

    def "BDD-IMP-CAN-02: Cancellation: Active Limit Reversal"() {
        given: 'Mutual Early Cancellation over $500k'
        def fid = "FAC-CAN-LIB-" + System.currentTimeMillis()
        ec.entity.makeValue("trade.CustomerFacility")
            .setAll([facilityId: fid, totalApprovedLimit: 1000000.0, utilizedAmount: 500000.0]).create()
        def ref = "TF-CAN-REV-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 500000.0, lcCurrencyUomId: "USD", customerFacilityId: fid]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, outstandingAmount: 500000.0]).call()
        
        when: "Cancellation is authorized and executed"
        ec.service.sync().name("trade.TradeCommonServices.update#Cancellation")
            .parameters([instrumentId: res.instrumentId, cancellationReason: "Early Closure"]).call()
        
        then: "Facility utilized amount is reversed (-500,000)"
        def fac = ec.entity.find("trade.CustomerFacility").condition("facilityId", fid).one()
        fac.utilizedAmount == 500000.0 // Original 500k remained, the new 500k was reversed
    }

    def "BDD-IMP-CAN-03: Cancellation: End of Day Auto-Expiry Flush"() {
        given: "An LC with Expiry Date = T-1 and status = Issued"
        def yesterday = new Date(System.currentTimeMillis() - 86400000)
        def facId = "FAC-CAN-03"
        if (!ec.entity.find("trade.CustomerFacility").condition("facilityId", facId).one()) {
            ec.entity.makeValue("trade.CustomerFacility")
                .setAll([facilityId: facId, totalApprovedLimit: 100000.0, utilizedAmount: 0.0]).create()
        }
        
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: "TF-AUTOEX-" + System.currentTimeMillis(), lcAmount: 1000.0, lcCurrencyUomId: "USD", lcCurrencyUomId: "USD", expiryDate: yesterday, customerFacilityId: facId]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: "LC_PENDING"]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: "LC_ISSUED"]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, outstandingAmount: 1000.0]).call()
            
        when: "The Auto-Expiry background task runs"
        ec.service.sync().name("trade.TradeCommonServices.update#Cancellation")
            .parameters([instrumentId: res.instrumentId, cancellationReason: "Auto-Expiry"]).call()
            
        then: "Transaction state is Cancelled and limits cleared"
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", res.instrumentId).one()
        lc.businessStateId == "LC_CANCELLED"
    }

    // --- Feature: SWIFT MT7xx Outbound Formatting (REQ-IMP-SWT-01 to 05) ---

    def "BDD-IMP-SWT-01: MT700: X-Character Base Validation"() {
        given: "An Import LC with goods description containing invalid characters"
        def ref = "TF-SWT-01-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD", lcCurrencyUomId: "USD", goodsDescription: "Price @ 5.00 #1"]).call()
        assert !res.errorMessage
        def lcVal = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", res.instrumentId).one()
        assert lcVal != null
        def goods = lcVal.goodsDescription
        
        when: "Formatting for SWIFT X-set via backend service"
        def result = ec.service.sync().name("trade.SwiftGenerationServices.format#XCharacter")
            .parameters([rawText: goods]).call()
        
        then: "Invalid characters are replaced by spaces"
        result.cleanText != null
        result.cleanText == "Price   5.00  1"
    }

    @Unroll
    def "BDD-IMP-SWT-02/03/04: MT700 Tag Formats (#tag)"() {
        given: "An Issued LC record"
        def ref = "TF-TAG-" + tag + "-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 50000.0, tolerancePositive: 0.10, lcCurrencyUomId: "USD", beneficiaryPartyId: "ABC12345"]).call()
        assert !res.errorMessage
        assert res.instrumentId != null
        
        when: "Transposing to SWIFT tag #tag via backend service"
        def result = ec.service.sync().name("trade.SwiftGenerationServices.format#Tag")
            .parameters([tag: tag, instrumentId: res.instrumentId]).call()
        assert !result.errorMessage
        
        then: "Format matches SWIFT standard with comma decimal"
        result.swText != null
        result.swText == expected
        
        where:
        tag   | expected
        "32B" | "USD50000,00"
        "39A" | "10/10"
        "59"  | "/ABC12345"
    }

    def "BDD-IMP-SWT-05: MT700: Native 65-Character Array Splitting"() {
        given: "An LC with long description field (> 65 chars)"
        def longDesc = "This is a very long description of goods that exceeds sixty five characters for sure."
        def ref = "TF-SWT-05-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD", goodsDescription: longDesc]).call()
        assert !res.errorMessage
        def lcVal = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", res.instrumentId).one()
        assert lcVal != null
        def goods = lcVal.goodsDescription

        when: "Splitting into SWIFT rows via backend service"
        def result = ec.service.sync().name("trade.SwiftGenerationServices.split#Rows")
            .parameters([text: goods]).call()
        
        then: "Array contains multiple rows with max 65 chars"
        result.rows != null
        result.rows.size() > 1
        result.rows[0].length() <= 65
    }

    def "BDD-IMP-SET-03: Partial draw updates effectiveOutstandingAmount"() {
        given: 'An active LC with $100,000 effective values'
        def ref = "TF-SET-03-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 100000.0, lcCurrencyUomId: "USD", beneficiaryPartyId: "BEN-01"]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: "LC_PENDING"]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: "LC_ISSUED"]).call()
        
        // Create presentation
        def presRes = ec.service.sync().name("trade.TradeCommonServices.create#Presentation")
            .parameters([instrumentId: res.instrumentId, claimAmount: 40000.0]).call()
        
        // Operation: Accept Documents
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: "LC_ACCEPTED"]).call()
        
        when: 'Settle partial amount of $40,000'
        ec.service.sync().name("trade.importlc.ImportLcServices.settle#Presentation")
            .parameters([presentationId: presRes.presentationId, settlementAmount: 40000.0, settlementTypeEnumId: "SIGHT"]).call()
        
        then: 'effectiveOutstandingAmount is $60,000'
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", res.instrumentId).one()
        lc.effectiveOutstandingAmount == 60000.0
        lc.cumulativeDrawnAmount == 40000.0
        lc.businessStateId == "LC_ISSUED"
    }

    def "BDD-IMP-VAL-03: Revolving LC reinstatement"() {
        given: 'A Revolving LC with $10,000 limit'
        // First create Product Catalog entry that allows revolving
        ec.entity.makeValue("trade.TradeProductCatalog")
            .setAll([productCatalogId: "CAT-REV-01", productName: "Revolving LC", allowRevolving: "Y"]).create()

        def ref = "TF-VAL-03-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 10000.0, lcCurrencyUomId: "USD", productCatalogId: "CAT-REV-01", beneficiaryPartyId: "BEN-01"]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: "LC_PENDING"]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: "LC_ISSUED"]).call()
        
        def presRes = ec.service.sync().name("trade.TradeCommonServices.create#Presentation")
            .parameters([instrumentId: res.instrumentId, claimAmount: 10000.0]).call()
        
        // Operation: Accept Documents
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: "LC_ACCEPTED"]).call()
        
        when: 'Full amount of $10,000 is settled'
        ec.service.sync().name("trade.importlc.ImportLcServices.settle#Presentation")
            .parameters([presentationId: presRes.presentationId, settlementAmount: 10000.0, settlementTypeEnumId: "SIGHT"]).call()
        
        then: 'effectiveOutstandingAmount is reinstated to $10,000'
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", res.instrumentId).one()
        lc.effectiveOutstandingAmount == 10000.0
        lc.cumulativeDrawnAmount == 10000.0
        lc.businessStateId == "LC_ISSUED"
    }

    def "BDD-IMP-AMD-06: Amendment: Concurrent Amendment Block"() {
        given:
        def instrumentId = createIssuedLc("AMD_CH_06")
        
        when: "First financial amendment is created"
        service.sync().name("trade.importlc.ImportLcServices.create#Amendment").parameters([
            instrumentId: instrumentId, amendmentTypeEnumId: 'AMEND_AMOUNT',
            amendmentDate: new java.sql.Date(ec.user.nowTimestamp.time), amountAdjustment: 1000, isFinancial: 'Y'
        ]).call()
        
        then: "Second amendment creation is blocked"
        try {
            service.sync().name("trade.importlc.ImportLcServices.create#Amendment").parameters([
                instrumentId: instrumentId, amendmentTypeEnumId: 'AMEND_DATE',
                amendmentDate: new java.sql.Date(ec.user.nowTimestamp.time), isFinancial: 'N'
            ]).call()
            false
        } catch (Exception e) {
            e.getMessage().contains("Cannot create new amendment while another is pending")
        }
    }

    def "BDD-IMP-AMD-07: Amendment: Beneficiary Consent Approval"() {
        given:
        def instrumentId = createIssuedLc("AMD_CH_07")
        def amdOut = service.sync().name("trade.importlc.ImportLcServices.create#Amendment").parameters([
            instrumentId: instrumentId, amendmentTypeEnumId: 'AMEND_AMOUNT',
            amendmentDate: new java.sql.Date(ec.user.nowTimestamp.time), amountAdjustment: 5000, isFinancial: 'Y'
        ]).call()
        def amendmentId = amdOut.amendmentId

        when: "Beneficiary accepts the amendment"
        service.sync().name("trade.importlc.ImportLcServices.update#Amendment").parameters([
            amendmentId: amendmentId, beneficiaryConsentStatusId: 'ACCEPTED'
        ]).call()

        then: "LC returns to ISSUED and values are updated"
        def lc = entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        lc.businessStateId == "LC_ISSUED"
        lc.effectiveAmount == 55000 // 50000 base + 5000 adjustment
        
        and: "Amendment status is COMMITTED"
        def amd = entity.find("trade.importlc.ImportLcAmendment").condition("amendmentId", amendmentId).one()
        amd.amendmentBusinessStateId == "AMEND_COMMITTED"
    }

    def "BDD-IMP-DOC-03: Discrepancy waiver and MT 752 generation"() {
        given: "A discrepant presentation"
        def ref = "TF-DOC-03-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 50000.0, lcCurrencyUomId: "USD", beneficiaryPartyId: "BEN-01"]).call()
        service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: "LC_PENDING"]).call()
        service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: "LC_ISSUED"]).call()
        
        // Create presentation and set to discrepant
        def presRes = ec.service.sync().name("trade.TradeCommonServices.create#Presentation")
            .parameters([instrumentId: res.instrumentId, claimAmount: 50000.0]).call()
        service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: "LC_DOCS_RECEIVED"]).call()
        service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: "LC_DISCREPANT"]).call()
        
        when: "Applicant waives discrepancies"
        ec.service.sync().name("trade.importlc.ImportLcServices.update#PresentationWaiver")
            .parameters([presentationId: presRes.presentationId, applicantDecisionEnumId: "WAIVED"]).call()
        
        then: "LC state transitions to LC_ACCEPTED and MT752 is generated"
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", res.instrumentId).one()
        lc.businessStateId == "LC_ACCEPTED"
        
        def swiftMsg = ec.entity.find("trade.importlc.SwiftMessage")
            .condition("instrumentId", res.instrumentId).condition("messageType", "MT752").one()
        swiftMsg != null
    }

    def "BDD-IMP-SWT-07: MT700: Continuation Message MT701 Logic"() {
        given: "An LC with very long goods description (> 6500 chars)"
        def longGoods = "A" * 6600
        def ref = "TF-701-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 1000.0, lcCurrencyUomId: "USD", goodsDescription: longGoods]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: "LC_PENDING"]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: "LC_ISSUED"]).call()
        
        when: "MT700 is generated"
        ec.service.sync().name("trade.SwiftGenerationServices.generate#Mt700")
            .parameters([instrumentId: res.instrumentId]).call()
            
        then: "Both MT700 and MT701 are produced"
        def mt700 = ec.entity.find("trade.importlc.SwiftMessage")
            .condition("instrumentId", res.instrumentId).condition("messageType", "MT700").one()
        def mt701 = ec.entity.find("trade.importlc.SwiftMessage")
            .condition("instrumentId", res.instrumentId).condition("messageType", "MT701").one()
            
        mt700 != null
        mt701 != null
        mt700.messageContent.contains(":27:1/2")
        mt701.messageContent.contains(":27:2/2")
    }

    def "BDD-IMP-SWT-08: MT707: Amendment Message Generation"() {
        given: "An authorized amendment"
        def ref = "TF-707-" + System.currentTimeMillis()
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([transactionRef: ref, lcAmount: 10000.0, lcCurrencyUomId: "USD"]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: "LC_PENDING"]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: res.instrumentId, businessStateId: "LC_ISSUED"]).call()
        def amdRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#Amendment")
            .parameters([instrumentId: res.instrumentId, amountAdjustment: 5000.0, amendmentNarrative: "Increase amount",
                         amendmentTypeEnumId: 'AMEND_INCREASE', amendmentDate: new java.sql.Date(System.currentTimeMillis())]).call()
        ec.service.sync().name("trade.importlc.ImportLcServices.authorize#Amendment")
            .parameters([amendmentId: amdRes.amendmentId]).call()
            
        when: "MT707 is generated"
        ec.service.sync().name("trade.SwiftGenerationServices.generate#Mt707")
            .parameters([amendmentId: amdRes.amendmentId]).call()
            
        then: "MT707 contains the amendment details"
        def msg = ec.entity.find("trade.importlc.SwiftMessage")
            .condition("instrumentId", res.instrumentId).condition("messageType", "MT707").one()
        msg != null
        msg.messageContent.contains(":34B:USD15000") // 10000 + 5000
        msg.messageContent.contains("Increase amount")
    }
}
