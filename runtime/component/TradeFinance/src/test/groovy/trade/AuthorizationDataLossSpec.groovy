package trade

import org.moqui.context.ExecutionContext
import org.moqui.Moqui
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Specification
import spock.lang.Shared
import org.moqui.entity.EntityCondition

/**
 * ABOUTME: AuthorizationDataLossSpec verifies that narrative fields are preserved during instrument authorization.
 * Ensures that authorization triggers do not inadvertently overwrite instrument fields with null values.
 */
class AuthorizationDataLossSpec extends Specification {
    @Shared protected ExecutionContext ec
    @Shared String testPrefix

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.transaction.runUseOrBegin(null, null) {
            ec.artifactExecution.disableAuthz()
            ec.user.loginUser("trade.admin", "trade123")
            testPrefix = "TEST-AUTH-" + System.currentTimeMillis()

        // Set isolated ID generation ranges - use 4400000
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrument", 38000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit", 38000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeTransaction", 38000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrumentParty", 38000000, 1000)
        }
    }

    def cleanupSpec() {
        if (ec != null) {
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrument")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeTransaction")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrumentParty")
            ec.destroy()
        }
    }

    def setup() {
        if (ec.transaction.isTransactionInPlace()) ec.transaction.rollback("Cleanup from previous state", null)
        ec.message.clearAll()
        ec.artifactExecution.disableAuthz()
    }
    
    def cleanup() {
        ec.message.clearAll()
    }

    def "Verify Narrative Field Preservation during Authorization"() {
        given:
        def uniqueRef = testPrefix + "_REF"
        
        // 0. Ensure Parties exist with RMA if bank
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
            .parameters([partyId: testPrefix + "_APP", partyName: "Applicant", partyTypeEnumId: "PTY_COMMERCIAL", kycStatus: "KYC_ACTIVE"]).call()
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
            .parameters([partyId: testPrefix + "_BEN", partyName: "Beneficiary", partyTypeEnumId: "PTY_COMMERCIAL", kycStatus: "KYC_ACTIVE"]).call()
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
            .parameters([partyId: testPrefix + "_ADVBANK", partyName: "Advising Bank", partyTypeEnumId: "PTY_BANK", kycStatus: "KYC_ACTIVE", hasActiveRMA: "Y"]).call()

        // 1. Create LC with narrative data
        Map createRes = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit").parameters([
            lcAmount: 10000.0,
            lcCurrencyUomId: "USD",
            expiryDate: new java.sql.Timestamp(System.currentTimeMillis() + 86400000 * 30),
            goodsDescription: "INITIAL GOODS",
            documentsRequired: "INITIAL DOCUMENTS",
            instrumentRef: uniqueRef,
            instrumentParties: [
                [roleEnumId: "TP_APPLICANT", partyId: testPrefix + "_APP"],
                [roleEnumId: "TP_BENEFICIARY", partyId: testPrefix + "_BEN"],
                [roleEnumId: "TP_ADVISING_BANK", partyId: testPrefix + "_ADVBANK"]
            ]
        ]).call()
        assert !ec.message.hasError()
        String instrumentId = createRes.instrumentId
        String transactionId = createRes.transactionId

        // Verify initial state
        def lcInit = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        assert lcInit != null
        assert lcInit.goodsDescription == "INITIAL GOODS"

        // 2. Submit for Approval (Transition to PENDING)
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit").parameters([
            instrumentId: instrumentId,
            businessStateId: "LC_PENDING"
        ]).call()
        assert !ec.message.hasError()

        // 3. Authorize the Transaction
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument").parameters([
            transactionId: transactionId,
            skipFourEyes: true
        ]).call()
        assert !ec.message.hasError()

        when: "Fetching post-authorization state"
        def lcPost = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()

        then: "Narrative fields must be preserved"
        lcPost.goodsDescription == "INITIAL GOODS"
        lcPost.documentsRequired == "INITIAL DOCUMENTS"
    }
}
