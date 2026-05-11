package trade

import spock.lang.Specification
import spock.lang.Shared
import org.moqui.Moqui
import org.moqui.context.ExecutionContext

/**
 * ABOUTME: TradeCommonEntitiesSpec validates core entity structures for instruments and facilities.
 */
class TradeCommonEntitiesSpec extends Specification {
    @Shared protected ExecutionContext ec
    @Shared String testPrefix
    
    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.transaction.runUseOrBegin(null, null) {
            ec.artifactExecution.disableAuthz()
            ec.user.loginUser("trade.maker", "trade123")
            testPrefix = "COM-ENT-" + System.currentTimeMillis()

        // Set isolated ID generation ranges - use 4700000
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrument", 46000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit", 46000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeTransaction", 46000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrumentParty", 46000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.CustomerFacility", 46000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeTransactionAudit", 46000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.FeeConfiguration", 46000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeProductCatalog", 46000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.UserAuthorityProfile", 46000000, 1000)
        }
    }
    
    def cleanupSpec() {
        if (ec != null) {
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrument")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeTransaction")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrumentParty")
            ec.entity.tempResetSequencedIdPrimary("trade.CustomerFacility")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeTransactionAudit")
            ec.entity.tempResetSequencedIdPrimary("trade.FeeConfiguration")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeProductCatalog")
            ec.entity.tempResetSequencedIdPrimary("trade.UserAuthorityProfile")
            ec.destroy()
        }
    }

    def setup() {
        if (ec.transaction.isTransactionInPlace()) ec.transaction.rollback("Cleanup from previous state", null)
        ec.message.clearAll()
        ec.artifactExecution.disableAuthz()
    }
    
    def "Test CREATE TradeInstrument implicitly checks structures"() {
        when:
        def res = ec.entity.makeValue("trade.TradeInstrument")
            .setAll([instrumentRef: testPrefix + "-REF-01"]).setSequencedIdPrimary().create()
        String instrumentId = res.instrumentId
            
        then:
        ec.entity.find("trade.TradeInstrument")
            .condition("instrumentId", instrumentId).one() != null
    }

    def "TradeParty persists compliance and SWIFT fields"() {
        given:
        def partyId = testPrefix + "-PARTY-1"
        
        when:
        ec.service.sync().name("create#trade.TradeParty").parameters([
            partyId: partyId,
            partyName: "Test Party Ltd",
            kycStatus: "KYC_ACTIVE",
            kycExpiryDate: "2027-01-01",
            sanctionsStatus: "CLEAR",
            countryOfRisk: "SGP",
            registeredAddress: "123 Marina Bay, Singapore"
        ]).call()
        def party = ec.entity.find("trade.TradeParty")
                .condition("partyId", partyId).one()

        then:
        party != null
        party.sanctionsStatus == "CLEAR"
        party.countryOfRisk == "SGP"
        party.registeredAddress == "123 Marina Bay, Singapore"
    }

    def "CustomerFacility persists owner and currency fields"() {
        given:
        def partyId = testPrefix + "-OWNER-1"
        ec.service.sync().name("create#trade.TradeParty").parameters([partyId: partyId, partyName: "Owner", partyTypeEnumId: "PTY_COMMERCIAL"]).call()
        
        when:
        def res = ec.service.sync().name("create#trade.CustomerFacility").parameters([
            ownerPartyId: partyId,
            totalApprovedLimit: 1000000,
            utilizedAmount: 0,
            currencyUomId: "USD",
            facilityExpiryDate: "2027-12-31"
        ]).call()
        String facId = res.facilityId
        def fac = ec.entity.find("trade.CustomerFacility")
                .condition("facilityId", facId).one()

        then:
        fac != null
        fac.ownerPartyId == partyId
        fac.currencyUomId == "USD"
        fac.totalApprovedLimit == 1000000
    }

    def "TradeTransactionAudit persists additional tracking fields"() {
        given: "A parent instrument and transaction"
        def instRes = ec.service.sync().name("create#trade.TradeInstrument").parameters([instrumentRef: testPrefix + "-REF-02"]).call()
        String instId = instRes.instrumentId
        def txRes = ec.service.sync().name("create#trade.TradeTransaction").parameters([instrumentId: instId, transactionTypeEnumId: "IMP_NEW"]).call()
        String txId = txRes.transactionId

        when:
        def auditId = ec.entity.sequencedIdPrimary("trade.TradeTransactionAudit", null, null)
        ec.entity.makeValue("trade.TradeTransactionAudit").setAll([
            auditId: auditId,
            instrumentId: instId,
            transactionId: txId,
            userId: "trade.maker",
            actionEnumId: "MAKER_UPDATE",
            requestIpAddress: "192.168.1.1",
            changedFieldName: "amount",
            oldValue: "400000",
            newValue: "500000",
            justificationRootText: "Increased amount per request"
        ]).create()
        def audit = ec.entity.find("trade.TradeTransactionAudit")
                .condition("auditId", auditId).one()

        then:
        audit != null
        audit.requestIpAddress == "192.168.1.1"
        audit.changedFieldName == "amount"
        audit.oldValue == "400000"
        audit.newValue == "500000"
    }

    def "FeeConfiguration persists correctly"() {
        when:
        def res = ec.service.sync().name("create#trade.FeeConfiguration").parameters([
            feeEventEnumId: "ISSUANCE_FEE",
            calculationTypeEnumId: "PERCENTAGE",
            baseValue: 0.25,
            ratePercent: 0.25,
            minFloorAmount: 100,
            currencyUomId: "USD",
            isActive: "Y",
            statusId: "FEE_ACTIVE"
        ]).call()
        String feeId = res.feeConfigurationId
        def fee = ec.entity.find("trade.FeeConfiguration")
                .condition("feeConfigurationId", feeId).one()

        then:
        fee != null
        fee.feeEventEnumId == "ISSUANCE_FEE"
        fee.baseValue == 0.25
    }

    def "TradeProductCatalog persists correctly"() {
        when:
        def res = ec.service.sync().name("create#trade.TradeProductCatalog").parameters([
            productTypeEnumId: "PROD_IMP_LC",
            productName: "Test Product " + testPrefix,
            isActive: "Y",
            statusId: "PROD_ACTIVE",
            allowRevolving: "Y",
            documentExamSlaDays: 5
        ]).call()
        String prodId = res.productId
        def prod = ec.entity.find("trade.TradeProductCatalog")
                .condition("productId", prodId).one()

        then:
        prod != null
        prod.productName.contains("Test Product")
        prod.allowRevolving == "Y"
        prod.documentExamSlaDays == 5
    }

    def "UserAuthorityProfile persists correctly"() {
        given:
        def userId = testPrefix + "-USER-1"
        when:
        def authId = null
        ec.transaction.runUseOrBegin(null, null) {
            ec.entity.makeValue("moqui.security.UserAccount").setAll([userId: userId, username: userId]).create()
            def res = ec.service.sync().name("create#trade.UserAuthorityProfile").parameters([
                userId: userId,
                delegationTierId: "TIER_4",
                customLimit: 5000000,
                currencyUomId: "USD",
                isSuspended: "N"
            ]).call()
            authId = res.userAuthorityId
        }
        def auth = ec.entity.find("trade.UserAuthorityProfile")
                .condition("userAuthorityId", authId).one()

        then:
        auth != null
        auth.delegationTierId == "TIER_4"
        auth.customLimit == 5000000
    }
}
