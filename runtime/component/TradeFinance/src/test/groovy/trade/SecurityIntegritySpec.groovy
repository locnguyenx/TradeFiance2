package trade

import spock.lang.*
import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import java.sql.Date
import java.sql.Timestamp
import java.util.Calendar
import org.moqui.entity.EntityCondition

/**
 * ABOUTME: SecurityIntegritySpec consolidates Maker/Checker logic, Dual Approval, Data Integrity, and Hardening features.
 * Covers delegation tiers, self-approval prevention, narrative preservation, and facility limit enforcement.
 */
@Stepwise
class SecurityIntegritySpec extends Specification {
    @Shared protected ExecutionContext ec
    @Shared String testPrefix
    @Shared String applicantId
    @Shared String beneficiaryId
    @Shared String applicant2Id
    @Shared String beneficiary2Id
    @Shared String facilityId

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.transaction.runUseOrBegin(null, null) {
            ec.artifactExecution.disableAuthz()
            ec.user.loginUser("trade.admin", "trade123")
            testPrefix = "SEC-" + System.currentTimeMillis()
            ec.logger.info("DEBUG: SecurityIntegritySpec testPrefix=${testPrefix}")

            // Set isolated ID generation ranges - use 99000000 (Module 5)
            ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrument", 99000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit", 99000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.TradeTransaction", 99000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrumentParty", 99000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.TradeApprovalRecord", 99000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.TradeTransactionAudit", 99000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.UserAuthorityProfile", 99000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.CustomerFacility", 99000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.FeeConfiguration", 99000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.importlc.TradeDocumentPresentation", 99000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLcSettlement", 99000000, 1000)

            applicantId = testPrefix + "_APP"
            beneficiaryId = testPrefix + "_BEN"
            applicant2Id = testPrefix + "_APP2"
            beneficiary2Id = testPrefix + "_BEN2"

            // Setup parties
            ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: applicantId, partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'SEC Applicant', kycStatus: 'KYC_ACTIVE']).call()
            ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: beneficiaryId, partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'SEC Beneficiary', kycStatus: 'KYC_ACTIVE']).call()
            ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: applicant2Id, partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'SEC Applicant 2', kycStatus: 'KYC_ACTIVE']).call()
            ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: beneficiary2Id, partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'SEC Beneficiary 2', kycStatus: 'KYC_ACTIVE']).call()

            // Setup Facility
            facilityId = testPrefix + "_FAC"
            ec.logger.info("DEBUG: Creating CustomerFacility ${facilityId}")
            ec.entity.makeValue("trade.CustomerFacility")
                .setAll([facilityId: facilityId, ownerPartyId: applicantId, totalApprovedLimit: 1000000.0, utilizedAmount: 0.0, currencyUomId: "USD", statusId: "FAC_ACTIVE"])
                .create()

            // Setup Fee
            ec.logger.info("DEBUG: Creating FeeConfiguration")
            ec.entity.makeValue("trade.FeeConfiguration")
                .setAll([feeConfigurationId: testPrefix + "_FEE", feeEventEnumId: "ISSUANCE_FEE", calculationTypeEnumId: "PERCENTAGE", baseValue: 0.005, statusId: "FEE_ACTIVE", partyId: applicantId,
                         effectiveDate: new Date(System.currentTimeMillis() - 86400000)]).create()

            // Setup Checker Users
            ["CK1", "CK2"].each { suffix ->
                def userId = testPrefix + "_" + suffix
                ec.logger.info("DEBUG: Creating UserAccount ${userId}")
                ec.entity.makeValue("moqui.security.UserAccount").setAll([userId: userId, username: userId, emailAddress: userId + "@example.com"]).create()
                ec.logger.info("DEBUG: Creating UserAuthorityProfile for ${userId}")
                ec.entity.makeValue("trade.UserAuthorityProfile").setAll([
                    userAuthorityId: userId + "_AUTH",
                    userId: userId, delegationTierId: "TIER_4", customLimit: 5000000.0, makerCheckerFlag: "CHECKER"
                ]).create()
            }
        }
    }

    def cleanupSpec() {
        if (ec != null) {
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrument")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeTransaction")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrumentParty")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeApprovalRecord")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeTransactionAudit")
            ec.entity.tempResetSequencedIdPrimary("trade.UserAuthorityProfile")
            ec.entity.tempResetSequencedIdPrimary("trade.CustomerFacility")
            ec.entity.tempResetSequencedIdPrimary("trade.FeeConfiguration")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.TradeDocumentPresentation")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLcSettlement")
            ec.destroy()
        }
    }

    def setup() {
        ec.transaction.begin(null)
        ec.user.loginUser("trade.admin", "trade123")
        ec.message.clearAll()
        ec.artifactExecution.disableAuthz()
    }

    def cleanup() {
        ec.transaction.commit()
    }

    // --- MAKER / CHECKER & DUAL APPROVAL ---

    def "should prohibit Maker from self-approving"() {
        given: "An LC created by a maker"
        def makerId = testPrefix + "_MAKER"
        ec.logger.info("DEBUG: Creating Maker UserAccount ${makerId}")
        ec.entity.makeValue("moqui.security.UserAccount").setAll([userId: makerId, username: makerId, emailAddress: makerId + "@example.com"]).create()
        
        ec.user.internalLoginUser(makerId)
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: testPrefix + "_SELF", lcAmount: 10000.0, lcCurrencyUomId: 'USD',
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId],
                                             [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]]]).call()
        if (ec.message.hasError()) ec.logger.error("DEBUG: create#ImportLetterOfCredit errors: " + ec.message.getMessagesString())
        def instrumentId = res?.instrumentId
        ec.logger.info("DEBUG: instrumentId=${instrumentId}, transactionId=${res.transactionId}, makerId=${makerId}")

        when: "Maker tries to authorize"
        def result = ec.service.sync().name("trade.AuthorizationServices.evaluate#MakerCheckerMatrix")
            .parameters([instrumentId: instrumentId, transactionId: res.transactionId, userId: makerId]).call()
        ec.logger.info("DEBUG: MakerCheckerMatrix result=${result}")

        then: "isAuthorized is false"
        result.isAuthorized == false
    }

    def "should enforce Dual Checker for high-value Tier 4 transactions"() {
        given: "A Tier 4 transaction (2M USD)"
        def ck1 = testPrefix + "_CK1"
        def ck2 = testPrefix + "_CK2"
        
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: testPrefix + "_DUAL", lcAmount: 2000000.0, lcCurrencyUomId: 'USD',
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId],
                                             [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]]]).call()
        def instrumentId = res.instrumentId
        
        // Submit for approval
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, businessStateId: 'LC_PENDING']).call()

        when: "First checker approves"
        ec.user.internalLoginUser(ck1)
        ec.service.sync().name("trade.importlc.ImportLcServices.approve#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, approvalComments: "First OK"]).call()

        then: "Transaction is still PENDING"
        def tx1 = ec.entity.find("trade.TradeTransaction").condition("instrumentId", instrumentId).one()
        tx1.transactionStatusId == "TX_PENDING"

        when: "Second unique checker approves"
        ec.user.internalLoginUser(ck2)
        ec.service.sync().name("trade.importlc.ImportLcServices.approve#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, approvalComments: "Second OK"]).call()

        then: "Transaction is APPROVED"
        def tx2 = ec.entity.find("trade.TradeTransaction").condition("instrumentId", instrumentId).one()
        tx2.transactionStatusId == "TX_APPROVED"
    }

    // --- DATA INTEGRITY ---

    def "should preserve narrative fields during authorization"() {
        given: "An LC with narrative data"
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: testPrefix + "_DATA_LOSS", lcAmount: 10000.0, lcCurrencyUomId: 'USD',
                         goodsDescription: "PRECIOUS CARGO", documentsRequired: "GOLDEN CERT",
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId],
                                             [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]]]).call()
        def instrumentId = res.instrumentId

        when: "Authorizing instrument"
        ec.user.loginUser("trade.checker", "trade123")
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument")
            .parameters([transactionId: res.transactionId, skipFourEyes: true]).call()

        then: "Narratives are preserved"
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        lc.goodsDescription == "PRECIOUS CARGO"
        lc.documentsRequired == "GOLDEN CERT"
    }

    def "should isolate party records between different instruments"() {
        given: "Two instruments with different parties"
        def resA = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: testPrefix + "_A", lcAmount: 1000.0, lcCurrencyUomId: 'USD',
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId],
                                             [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiary2Id]]]).call()
        def resB = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: testPrefix + "_B", lcAmount: 1000.0, lcCurrencyUomId: 'USD',
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicant2Id],
                                             [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]]]).call()

        when: "Fetching both"
        def outA = ec.service.sync().name("trade.importlc.ImportLcServices.get#ImportLetterOfCredit")
            .parameters([instrumentId: resA.instrumentId]).call()
        def outB = ec.service.sync().name("trade.importlc.ImportLcServices.get#ImportLetterOfCredit")
            .parameters([instrumentId: resB.instrumentId]).call()

        then: "Parties do not leak"
        outA.parties.any { it.partyId == applicantId }
        !outA.parties.any { it.partyId == applicant2Id }
        outB.parties.any { it.partyId == beneficiaryId }
        !outB.parties.any { it.partyId == beneficiary2Id }
    }

    // --- HARDENING & CALCULATIONS ---

    def "should calculate business date skipping weekends"() {
        given: "A Friday"
        Calendar cal = Calendar.getInstance()
        cal.set(2026, Calendar.MAY, 1, 12, 0) // May 1, 2026 is Friday
        Date friday = new Date(cal.getTimeInMillis())

        when: "Adding 1 business day"
        def result = ec.service.sync().name("trade.TradeCommonServices.calculate#BusinessDate")
                        .parameters([startDate: friday, daysToAdd: 1]).call()
        Calendar resCal = Calendar.getInstance()
        resCal.setTime(result.resultDate)

        then: "Result is Monday May 4"
        resCal.get(Calendar.DAY_OF_MONTH) == 4
        resCal.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY
    }

    def "should calculate fees correctly"() {
        when: "Calculating 0.5% fee on 100k"
        def result = ec.service.sync().name("trade.TradeAccountingServices.calculate#Fees")
                        .parameters([baseAmount: 100000.0, feeEventEnumId: "ISSUANCE_FEE", partyId: applicantId]).call()

        then: "Fee is 500"
        result.totalFee == 500.0
    }

    def "should enforce facility limit and update utilization"() {
        given: "An LC with facility ID"
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: testPrefix + "_FAC", lcAmount: 200000.0, lcCurrencyUomId: "USD",
                         customerFacilityId: facilityId,
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId],
                                             [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]]]).call()

        when: "Approving LC"
        ec.user.loginUser("trade.checker", "trade123")
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument")
            .parameters([transactionId: res.transactionId, skipFourEyes: true]).call()
        
        then: "Utilization is updated"
        def facility = ec.entity.find("trade.CustomerFacility").condition("facilityId", facilityId).one()
        facility.utilizedAmount == 200000.0

        when: "Trying to exceed limit (1M limit, 200k used, try 900k)"
        ec.user.loginUser("trade.admin", "trade123")
        def resExceed = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: testPrefix + "_EXCEED", lcAmount: 900000.0, lcCurrencyUomId: "USD",
                         customerFacilityId: facilityId,
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId],
                                             [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]]]).call()
        
        ec.user.loginUser("trade.checker", "trade123")
        ec.service.sync().name("trade.AuthorizationServices.authorize#Instrument")
            .parameters([transactionId: resExceed.transactionId, skipFourEyes: true]).call()

        then: "Fails with insufficient limit"
        ec.message.hasError()
        ec.message.errors.any { it.contains("Insufficient limit") }
    }
}
