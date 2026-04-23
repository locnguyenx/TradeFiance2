package moqui.trade.finance

import spock.lang.Specification
import org.moqui.Moqui
import org.moqui.context.ExecutionContext

// ABOUTME: TradeCommonEntitiesSpec validates core entity structures for instruments and facilities.

class TradeCommonEntitiesSpec extends Specification {
    protected ExecutionContext ec
    
    def setup() {
        ec = Moqui.getExecutionContext()
        ec.artifactExecution.disableAuthz()
    }
    
    def cleanup() {
        ec.destroy()
    }
    
    def "Test CREATE TradeInstrument implicitly checks structures"() {
        when:
        ec.entity.makeValue("moqui.trade.instrument.TradeInstrument")
            .setAll([instrumentId:"RESTORE-1", transactionRef:"TF-TEST-01"]).create()
            
        then:
        ec.entity.find("moqui.trade.instrument.TradeInstrument")
            .condition("instrumentId", "RESTORE-1").one() != null
            
        cleanup:
        ec.entity.find("moqui.trade.instrument.TradeInstrument")
            .condition("instrumentId", "RESTORE-1").deleteAll()
    }
    def "TradeInstrument persists transaction management fields"() {
        when:
        ec.service.sync().name("create#moqui.trade.instrument.TradeInstrument").parameters([
            instrumentId: "TF-MGMT-TEST",
            transactionRef: "TF-IMP-26-TEST",
            lifecycleStatusId: "INST_PRE_ISSUE",
            transactionStatusId: "TRANS_DRAFT",
            transactionDate: ec.user.nowTimestamp,
            transactionTypeEnumId: "NEW_ISSUANCE",
            makerUserId: ec.user.userId,
            makerTimestamp: ec.user.nowTimestamp,
            versionNumber: 1,
            priorityEnumId: "NORMAL",
            productEnumId: "IMP_LC",
            amount: 100000,
            currencyUomId: "USD",
            outstandingAmount: 100000,
            applicantPartyId: "ACME_CORP_001",
            beneficiaryPartyId: "BENEFICIARY_001",
            issueDate: "2026-06-01",
            expiryDate: "2026-12-31"
        ]).call()
        def instruments = ec.entity.find("moqui.trade.instrument.TradeInstrument")
                .condition("instrumentId", "TF-MGMT-TEST").list()
        def inst = instruments[0]

        then:
        inst != null
        inst.transactionStatusId == "TRANS_DRAFT"
        inst.transactionTypeEnumId == "NEW_ISSUANCE"
        inst.makerUserId == ec.user.userId
        inst.makerTimestamp != null
        inst.versionNumber == 1
        inst.priorityEnumId == "NORMAL"
        inst.checkerUserId == null
        inst.checkerTimestamp == null
        inst.rejectionReason == null

        cleanup:
        ec.entity.find("moqui.trade.instrument.TradeInstrument")
            .condition("instrumentId", "TF-MGMT-TEST").deleteAll()
    }

    def "TradeParty persists compliance and SWIFT fields"() {
        when:
        ec.service.sync().name("create#moqui.trade.instrument.TradeParty").parameters([
            partyId: "PARTY_TEST",
            partyName: "Test Party Ltd",
            kycStatus: "Active",
            kycExpiryDate: "2027-01-01",
            sanctionsStatus: "CLEAR",
            countryOfRisk: "SGP",
            swiftBic: "TESTSGSGXXX",
            registeredAddress: "123 Marina Bay, Singapore",
            partyRoleEnumId: "APPLICANT"
        ]).call()
        def party = ec.entity.find("moqui.trade.instrument.TradeParty")
                .condition("partyId", "PARTY_TEST").one()

        then:
        party != null
        party.sanctionsStatus == "CLEAR"
        party.swiftBic == "TESTSGSGXXX"
        party.countryOfRisk == "SGP"
        party.registeredAddress == "123 Marina Bay, Singapore"
        party.partyRoleEnumId == "APPLICANT"

        cleanup:
        ec.entity.find("moqui.trade.instrument.TradeParty")
            .condition("partyId", "PARTY_TEST").deleteAll()
    }

    def "CustomerFacility persists owner and currency fields"() {
        when:
        ec.service.sync().name("create#moqui.trade.instrument.CustomerFacility").parameters([
            facilityId: "FAC_TEST_001",
            ownerPartyId: "ACME_CORP_001",
            totalApprovedLimit: 1000000,
            utilizedAmount: 0,
            currencyUomId: "USD",
            facilityExpiryDate: "2027-12-31"
        ]).call()
        def fac = ec.entity.find("moqui.trade.instrument.CustomerFacility")
                .condition("facilityId", "FAC_TEST_001").one()

        then:
        fac != null
        fac.ownerPartyId == "ACME_CORP_001"
        fac.currencyUomId == "USD"
        fac.totalApprovedLimit == 1000000

        cleanup:
        ec.entity.find("moqui.trade.instrument.CustomerFacility")
            .condition("facilityId", "FAC_TEST_001").deleteAll()
    }

    def "TradeTransactionAudit persists additional tracking fields"() {
        when:
        ec.service.sync().name("create#moqui.trade.instrument.TradeTransactionAudit").parameters([
            instrumentId: "AUDIT-TEST",
            auditId: "1",
            userId: "USER_001",
            actionEnumId: "UPDATE",
            requestIpAddress: "192.168.1.1",
            changedFieldName: "amount",
            oldValue: "400000",
            newValue: "500000",
            justificationRootText: "Increased amount per request"
        ]).call()
        def audit = ec.entity.find("moqui.trade.instrument.TradeTransactionAudit")
                .condition("instrumentId", "AUDIT-TEST").one()

        then:
        audit != null
        audit.requestIpAddress == "192.168.1.1"
        audit.changedFieldName == "amount"
        audit.oldValue == "400000"
        audit.newValue == "500000"

        cleanup:
        ec.entity.find("moqui.trade.instrument.TradeTransactionAudit")
            .condition("instrumentId", "AUDIT-TEST").deleteAll()
    }

    def "FeeConfiguration persists correctly"() {
        when:
        ec.service.sync().name("create#moqui.trade.instrument.FeeConfiguration").parameters([
            feeConfigId: "FEE_TEST_001",
            feeTypeEnumId: "ISSUANCE_FEE",
            calculationMethodEnumId: "PERCENTAGE",
            ratePercent: 0.25,
            minFloorAmount: 100,
            currencyUomId: "USD",
            isActive: "Y"
        ]).call()
        def fee = ec.entity.find("moqui.trade.instrument.FeeConfiguration")
                .condition("feeConfigId", "FEE_TEST_001").one()

        then:
        fee != null
        fee.feeTypeEnumId == "ISSUANCE_FEE"
        fee.ratePercent == 0.25

        cleanup:
        ec.entity.find("moqui.trade.instrument.FeeConfiguration")
            .condition("feeConfigId", "FEE_TEST_001").deleteAll()
    }

    def "TradeProductCatalog persists correctly"() {
        when:
        ec.service.sync().name("create#moqui.trade.instrument.TradeProductCatalog").parameters([
            productCatalogId: "PROD_TEST_001",
            productName: "Test Product",
            isActive: "Y",
            allowRevolving: "Y",
            documentExamSlaDays: 5
        ]).call()
        def prod = ec.entity.find("moqui.trade.instrument.TradeProductCatalog")
                .condition("productCatalogId", "PROD_TEST_001").one()

        then:
        prod != null
        prod.productName == "Test Product"
        prod.allowRevolving == "Y"
        prod.documentExamSlaDays == 5

        cleanup:
        ec.entity.find("moqui.trade.instrument.TradeProductCatalog")
            .condition("productCatalogId", "PROD_TEST_001").deleteAll()
    }

    def "UserAuthorityProfile persists correctly"() {
        when:
        ec.service.sync().name("create#moqui.trade.instrument.UserAuthorityProfile").parameters([
            authorityProfileId: "AUTH_TEST_001",
            userId: "USER_001",
            authorityTierEnumId: "TIER_4",
            maxApprovalAmount: 5000000,
            currencyUomId: "USD",
            isSuspended: "N"
        ]).call()
        def auth = ec.entity.find("moqui.trade.instrument.UserAuthorityProfile")
                .condition("authorityProfileId", "AUTH_TEST_001").one()

        then:
        auth != null
        auth.authorityTierEnumId == "TIER_4"
        auth.maxApprovalAmount == 5000000

        cleanup:
        ec.entity.find("moqui.trade.instrument.UserAuthorityProfile")
            .condition("authorityProfileId", "AUTH_TEST_001").deleteAll()
    }
}
