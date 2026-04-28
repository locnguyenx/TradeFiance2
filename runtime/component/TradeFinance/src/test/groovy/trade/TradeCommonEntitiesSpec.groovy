package trade


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
        ec.entity.makeValue("trade.TradeInstrument")
            .setAll([instrumentId:"RESTORE-1", transactionRef:"TF-TEST-01"]).create()
            
        then:
        ec.entity.find("trade.TradeInstrument")
            .condition("instrumentId", "RESTORE-1").one() != null
            
        cleanup:
        ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", "RESTORE-1").deleteAll()
        ec.entity.find("trade.TradeTransaction").condition("instrumentId", "RESTORE-1").deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", "RESTORE-1").deleteAll()
    }
    // Removed test: TradeInstrument persists transaction management fields
    // Fields moved to TradeTransaction entity. See TradeTransactionSpec.groovy

    def "TradeParty persists compliance and SWIFT fields"() {
        when:
        ec.service.sync().name("create#trade.TradeParty").parameters([
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
        def party = ec.entity.find("trade.TradeParty")
                .condition("partyId", "PARTY_TEST").one()

        then:
        party != null
        party.sanctionsStatus == "CLEAR"
        party.swiftBic == "TESTSGSGXXX"
        party.countryOfRisk == "SGP"
        party.registeredAddress == "123 Marina Bay, Singapore"
        party.partyRoleEnumId == "APPLICANT"

        cleanup:
        ec.entity.find("trade.TradeParty")
            .condition("partyId", "PARTY_TEST").deleteAll()
    }

    def "CustomerFacility persists owner and currency fields"() {
        when:
        ec.service.sync().name("create#trade.CustomerFacility").parameters([
            facilityId: "FAC_TEST_001",
            ownerPartyId: "ACME_CORP_001",
            totalApprovedLimit: 1000000,
            utilizedAmount: 0,
            currencyUomId: "USD",
            facilityExpiryDate: "2027-12-31"
        ]).call()
        def fac = ec.entity.find("trade.CustomerFacility")
                .condition("facilityId", "FAC_TEST_001").one()

        then:
        fac != null
        fac.ownerPartyId == "ACME_CORP_001"
        fac.currencyUomId == "USD"
        fac.totalApprovedLimit == 1000000

        cleanup:
        ec.entity.find("trade.CustomerFacility")
            .condition("facilityId", "FAC_TEST_001").deleteAll()
    }

    def "TradeTransactionAudit persists additional tracking fields"() {
        when:
        ec.service.sync().name("create#trade.TradeTransactionAudit").parameters([
            instrumentId: "AUDIT-TEST",
            transactionId: "TX-AUDIT-TEST",
            auditId: "1",
            userId: "USER_001",
            actionEnumId: "UPDATE",
            requestIpAddress: "192.168.1.1",
            changedFieldName: "amount",
            oldValue: "400000",
            newValue: "500000",
            justificationRootText: "Increased amount per request"
        ]).call()
        def audit = ec.entity.find("trade.TradeTransactionAudit")
                .condition("instrumentId", "AUDIT-TEST").one()

        then:
        audit != null
        audit.requestIpAddress == "192.168.1.1"
        audit.changedFieldName == "amount"
        audit.oldValue == "400000"
        audit.newValue == "500000"

        cleanup:
        ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", "AUDIT-TEST").deleteAll()
        ec.entity.find("trade.TradeTransaction").condition("instrumentId", "AUDIT-TEST").deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", "AUDIT-TEST").deleteAll()
    }

    def "FeeConfiguration persists correctly"() {
        when:
        ec.service.sync().name("create#trade.FeeConfiguration").parameters([
            feeConfigurationId: "FEE_TEST_001",
            feeEventEnumId: "ISSUANCE_FEE",
            calculationTypeEnumId: "PERCENTAGE",
            baseValue: 0.25,
            ratePercent: 0.25,
            minFloorAmount: 100,
            currencyUomId: "USD",
            isActive: "Y",
            statusId: "FEE_ACTIVE"
        ]).call()
        def fee = ec.entity.find("trade.FeeConfiguration")
                .condition("feeConfigurationId", "FEE_TEST_001").one()

        then:
        fee != null
        fee.feeEventEnumId == "ISSUANCE_FEE"
        fee.baseValue == 0.25

        cleanup:
        ec.entity.find("trade.FeeConfiguration")
            .condition("feeConfigurationId", "FEE_TEST_001").deleteAll()
    }

    def "TradeProductCatalog persists correctly"() {
        when:
        ec.service.sync().name("create#trade.TradeProductCatalog").parameters([
            productId: "PROD_TEST_001",
            productTypeEnumId: "PROD_IMP_LC",
            productName: "Test Product",
            isActive: "Y",
            statusId: "PROD_ACTIVE",
            allowRevolving: "Y",
            documentExamSlaDays: 5
        ]).call()
        def prod = ec.entity.find("trade.TradeProductCatalog")
                .condition("productId", "PROD_TEST_001").one()

        then:
        prod != null
        prod.productName == "Test Product"
        prod.allowRevolving == "Y"
        prod.documentExamSlaDays == 5

        cleanup:
        ec.entity.find("trade.TradeProductCatalog")
            .condition("productId", "PROD_TEST_001").deleteAll()
    }

    def "UserAuthorityProfile persists correctly"() {
        when:
        ec.service.sync().name("create#trade.UserAuthorityProfile").parameters([
            userAuthorityId: "AUTH_TEST_001",
            userId: "USER_001",
            delegationTierId: "TIER_4",
            customLimit: 5000000,
            currencyUomId: "USD",
            isSuspended: "N"
        ]).call()
        def auth = ec.entity.find("trade.UserAuthorityProfile")
                .condition("userAuthorityId", "AUTH_TEST_001").one()

        then:
        auth != null
        auth.delegationTierId == "TIER_4"
        auth.customLimit == 5000000

        cleanup:
        ec.entity.find("trade.UserAuthorityProfile")
            .condition("userAuthorityId", "AUTH_TEST_001").deleteAll()
    }
}
