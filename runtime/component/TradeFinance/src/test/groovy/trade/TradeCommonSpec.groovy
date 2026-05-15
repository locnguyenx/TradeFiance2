package trade

import spock.lang.*
import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import java.sql.Date
import java.sql.Timestamp
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * ABOUTME: TradeCommonSpec provides backend parity for the Common Module BDD scenarios.
 * Covers Base Entities, Trade Parties, KYC, Facility Limits, FX, SLA, Authority Tiers, and User Accounts.
 * Consolidates TradePartySpec, BddCommonModuleSpec, LimitServicesSpec, UserAccountServicesSpec, TradePartyLcIntegrationSpec.
 */
@Stepwise
class TradeCommonSpec extends Specification {
    protected final static Logger logger = LoggerFactory.getLogger(TradeCommonSpec.class)
    
    @Shared protected ExecutionContext ec
    @Shared String testPrefix
    @Shared protected String testUserId
    @Shared protected String testUsername
    @Shared protected String testEmail
    @Shared String lc001Id
    @Shared String lc002Id

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.transaction.runUseOrBegin(null, null) {
            ec.user.loginUser("trade.admin", "trade123")
            ec.artifactExecution.disableAuthz()
            testPrefix = "CMN-" + System.currentTimeMillis()

            // Set isolated ID generation ranges - use 91000000 (Module 1)
            ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrument", 91000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit", 91000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.TradeTransaction", 91000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrumentParty", 91000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.CustomerFacility", 91000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.FeeConfiguration", 91000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.TradeParty", 91000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("moqui.security.UserAccount", 91000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.UserAuthorityProfile", 91000000, 1000)

            // Ensure test parties exist
            ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                    .parameters([partyId: testPrefix + '_CUST_01', partyTypeEnumId: 'PTY_COMMERCIAL', 
                                 partyName: 'BDD Customer 01', kycStatus: 'KYC_ACTIVE']).call()
            ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                    .parameters([partyId: testPrefix + '_APP_001', partyTypeEnumId: 'PTY_COMMERCIAL', 
                                 partyName: 'Integration Applicant', kycStatus: 'KYC_ACTIVE']).call()
            ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                    .parameters([partyId: testPrefix + '_BEN_001', partyTypeEnumId: 'PTY_COMMERCIAL', 
                                 partyName: 'Integration Beneficiary', kycStatus: 'KYC_ACTIVE']).call()
            ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                    .parameters([partyId: testPrefix + '_BANK_ADV', partyTypeEnumId: 'PTY_BANK', 
                                 partyName: 'Adv Bank', kycStatus: 'KYC_ACTIVE', hasActiveRMA: true]).call()
            ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                    .parameters([partyId: testPrefix + '_BANK_CONF', partyTypeEnumId: 'PTY_BANK', 
                                 partyName: 'Conf Bank', kycStatus: 'KYC_ACTIVE', hasActiveRMA: true, 
                                 fiLimitAvailable: 1000000]).call()

            // Ensure holiday exists
            def holidayDate = java.sql.Date.valueOf("2026-04-27")
            if (ec.entity.find("trade.PublicHoliday").condition("holidayDate", holidayDate).count() == 0) {
                ec.entity.makeValue("trade.PublicHoliday").setAll([holidayDate: holidayDate, description: "SLA Test Holiday"]).create()
            }
            
            // User Setup
            long ts = System.currentTimeMillis()
            testUsername = "test.user." + ts
            testEmail = "test." + ts + "@example.com"
            Timestamp pastDate = new Timestamp(ec.l10n.parseTimestamp("2020-01-01 00:00:00", null).getTime())

            def group = ec.entity.find("moqui.security.UserGroup").condition("userGroupId", "TRADE_MAKER").one()
            if (!group) {
                ec.entity.makeValue("moqui.security.UserGroup")
                    .setAll([userGroupId: "TRADE_MAKER", description: "Trade Maker"]).create()
            }

            def uaResult = ec.service.sync().name("org.moqui.impl.UserServices.create#UserAccount")
                .parameters([username: testUsername, 
                            newPassword: "Password123!", newPasswordVerify: "Password123!",
                            emailAddress: testEmail, userFullName: "Test User"])
                .call()
            testUserId = uaResult.userId

            ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: testUserId, partyTypeEnumId: 'PTY_PERSON', partyName: 'Test User', kycStatus: 'KYC_ACTIVE']).call()
            
            ec.entity.makeValue("mantle.party.Person")
                .setAll([partyId: testUserId, firstName: "Test", lastName: "User"])
                .createOrUpdate()

            ec.entity.makeValue("moqui.security.UserGroupMember")
                .setAll([userId: testUserId, userGroupId: "TRADE_MAKER", fromDate: pastDate]).create()

            ec.entity.makeValue("trade.UserAuthorityProfile")
                .setAll([userId: testUserId, delegationTierId: "TIER_1", customLimit: 50000.0, currencyUomId: "USD"])
                .setSequencedIdPrimary().create()
        }
    }

    def cleanupSpec() {
        if (ec != null) {
            ec.user.logoutUser()
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrument")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeTransaction")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrumentParty")
            ec.entity.tempResetSequencedIdPrimary("trade.CustomerFacility")
            ec.entity.tempResetSequencedIdPrimary("trade.FeeConfiguration")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeParty")
            ec.entity.tempResetSequencedIdPrimary("moqui.security.UserAccount")
            ec.entity.tempResetSequencedIdPrimary("trade.UserAuthorityProfile")
            ec.destroy()
        }
    }

    def setup() {
        if (ec.transaction.isTransactionInPlace()) ec.transaction.rollback("Cleanup from previous state", null)
        ec.user.loginUser("trade.admin", "trade123")
        ec.message.clearAll()
        ec.artifactExecution.disableAuthz()
    }

    def cleanup() {
        ec.message.clearAll()
    }

    // --- FROM BddCommonModuleSpec ---

    def "BDD-CMN-ENT-01: Trade Inst. Base Attributes Enforcement"() {
        when:
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: testPrefix + "_REF_01", lcAmount: 1000.0, lcCurrencyUomId: 'USD',
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: testPrefix + '_CUST_01'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: testPrefix + '_BEN_001']]]).call()
        String instrumentId = res.instrumentId
        
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
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: testPrefix + "_REF_02", lcAmount: 1000.0, lcCurrencyUomId: 'USD', 
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: pid],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: testPrefix + '_BEN_001']]]).call()
        String instrumentId = res.instrumentId
        
        then:
        !ec.message.hasError()
        instrumentId != null
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
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: testPrefix + '_BEN_001']]]).call()
        
        then:
        ec.message.hasError()
        ec.message.getErrorsString().contains("has no active KYC")
    }

    def "BDD-CMN-ENT-04: Facility Limit Availability Earmark"() {
        given:
        def res = ec.entity.makeValue("trade.CustomerFacility")
            .setAll([totalApprovedLimit: 5000000.0, utilizedAmount: 1000000.0]).setSequencedIdPrimary().create()
        String fid = res.facilityId
            
        when:
        ec.service.sync().name("trade.LimitServices.update#Utilization")
            .parameters([facilityId: fid, amountDelta: 50000.0]).call()
            
        then:
        def fac = ec.entity.find("trade.CustomerFacility").condition("facilityId", fid).one()
        fac.utilizedAmount == 1050000.0
    }

    def "BDD-CMN-WF-01: Processing Flow Execution to Pending"() {
        given:
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
            .parameters([instrumentRef: testPrefix + "_FLOW_REF", lcAmount: 1000.0, lcCurrencyUomId: 'USD',
                         instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: testPrefix + '_CUST_01'],
                                   [roleEnumId: 'TP_BENEFICIARY', partyId: testPrefix + '_BEN_001']]]).call()
        String instrumentId = res.instrumentId
            
        when:
        ec.service.sync().name("trade.importlc.ImportLcServices.update#ImportLetterOfCredit")
            .parameters([instrumentId: instrumentId, businessStateId: "LC_PENDING"]).call()
            
        then:
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        lc.businessStateId == "LC_PENDING"
    }

    @Unroll
    def "BDD-CMN-FX-01/02: Precision: #currency decimal format"() {
        when:
        def result = ec.service.sync().name("trade.TradeCommonServices.round#Amount")
            .parameters([amount: rawAmount, currencyUomId: currency]).call()
        
        then:
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
        def res = ec.entity.makeValue("trade.CustomerFacility")
            .setAll([totalApprovedLimit: 4999.0, utilizedAmount: 0.0]).setSequencedIdPrimary().create()
        String fid = res.facilityId
            
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

    // --- FROM TradePartySpec ---

    def "BDD-CMN-TP-01: Create Commercial TradeParty"() {
        given:
        String tpId = testPrefix + "_C1"
        
        when:
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: tpId, partyTypeEnumId: 'PTY_COMMERCIAL', 
                             partyName: 'Spec Comm 01', kycStatus: 'KYC_ACTIVE']).call()
        
        then:
        def tp = ec.entity.find("trade.TradeParty").condition("partyId", tpId).one()
        tp != null
        tp.partyTypeEnumId == 'PTY_COMMERCIAL'
    }

    def "BDD-CMN-TP-02: Create Bank TradeParty (base + extension)"() {
        given:
        String tpId = testPrefix + "_B1"
        
        when:
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: tpId, partyTypeEnumId: 'PTY_BANK', 
                             partyName: 'Spec Bank 01', swiftBic: 'SPECXXXV', hasActiveRMA: true, 
                             kycStatus: 'KYC_ACTIVE', fiLimitAvailable: 1000000.0]).call()
        
        then:
        def tp = ec.entity.find("trade.TradeParty").condition("partyId", tpId).one()
        def tpb = ec.entity.find("trade.TradePartyBank").condition("partyId", tpId).one()
        tp != null
        tpb != null
        tpb.swiftBic == 'SPECXXXV'
        tpb.hasActiveRMA == 'Y'
    }

    def "BDD-CMN-TP-03: Create TradeParty with invalid SWIFT characters -> Fail"() {
        when:
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: testPrefix + '_C2', partyTypeEnumId: 'PTY_COMMERCIAL', 
                            partyName: 'Test & Corp @', kycStatus: 'KYC_ACTIVE']).call()

        then:
        ec.message.hasError()
    }

    def "BDD-CMN-TP-04: Assign Applicant role (Commercial)"() {
        given:
        String commId1 = testPrefix + "-C1-TP4"
        String commId2 = testPrefix + "-C2-TP4"
        
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: commId1, partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'Spec Comm 01', kycStatus: 'KYC_ACTIVE']).call()
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: commId2, partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'Spec Comm 02', kycStatus: 'KYC_ACTIVE']).call()
        
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
                .parameters([lcAmount: 5000.0, lcCurrencyUomId: 'USD',
                             instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: commId1], [roleEnumId: 'TP_BENEFICIARY', partyId: commId2]]]).call()
        String instId = res.instrumentId
        
        when:
        ec.service.sync().name("trade.TradeCommonServices.assign#InstrumentParty")
                .parameters([instrumentId: instId, roleEnumId: 'TP_APPLICANT', partyId: commId1]).call()
        
        then:
        ec.entity.find("trade.TradeInstrumentParty").condition([instrumentId: instId, roleEnumId: 'TP_APPLICANT', partyId: commId1]).one() != null
    }

    def "BDD-CMN-TP-05: Assign same bank to multiple roles"() {
        given:
        def instVal = ec.entity.makeValue("trade.TradeInstrument").setAll([instrumentTypeEnumId: "IMPORT_LC"]).setSequencedIdPrimary().create()
        String instId = instVal.instrumentId
        String bankId = testPrefix + "_BANK_05"
        ec.entity.makeValue("trade.TradeParty").setAll([partyId: bankId, partyTypeEnumId: "PTY_BANK", partyName: "Bank 05", kycStatus: "KYC_ACTIVE"]).create()
        ec.entity.makeValue("trade.TradePartyBank").setAll([partyId: bankId, hasActiveRMA: "Y"]).create()

        when:
        ec.service.sync().name("trade.TradeCommonServices.assign#InstrumentParty")
                .parameters([instrumentId: instId, roleEnumId: 'TP_ADVISING_BANK', partyId: bankId]).call()
        ec.service.sync().name("trade.TradeCommonServices.assign#InstrumentParty")
                .parameters([instrumentId: instId, roleEnumId: 'TP_CONFIRMING_BANK', partyId: bankId]).call()
        
        then:
        def list = ec.entity.find("trade.TradeInstrumentParty").condition([instrumentId: instId, partyId: bankId]).list()
        list.any { it.roleEnumId == 'TP_ADVISING_BANK' }
        list.any { it.roleEnumId == 'TP_CONFIRMING_BANK' }
    }

    def "BDD-CMN-TP-08: Assign Advising Bank (Missing RMA) -> Fail"() {
        setup:
        def instVal = ec.entity.makeValue("trade.TradeInstrument").setAll([instrumentTypeEnumId: "IMPORT_LC"]).setSequencedIdPrimary().create()
        String instId = instVal.instrumentId
        def bankId = testPrefix + "_NO_RMA_08"
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: bankId, partyTypeEnumId: 'PTY_BANK', 
                             partyName: 'Spec No RMA Bank', hasActiveRMA: false, kycStatus: 'KYC_ACTIVE']).call()
        
        when:
        ec.service.sync().name("trade.TradeCommonServices.assign#InstrumentParty")
                .parameters([instrumentId: instId, roleEnumId: 'TP_ADVISING_BANK', partyId: bankId]).call()
        
        then:
        ec.message.hasError()
        ec.message.errors.any { it.contains("has no active RMA") }
    }

    def "BDD-CMN-TP-07: Commercial party -> bank role rejection"() {
        given:
        def instVal = ec.entity.makeValue("trade.TradeInstrument").setAll([instrumentTypeEnumId: "IMPORT_LC"]).setSequencedIdPrimary().create()
        String instId = instVal.instrumentId
        def commId = testPrefix + "_COMM_07"
        ec.entity.makeValue("trade.TradeParty").setAll([partyId: commId, partyTypeEnumId: "PTY_COMMERCIAL", partyName: "Comm 07", kycStatus: "KYC_ACTIVE"]).create()

        when:
        ec.service.sync().name("trade.TradeCommonServices.assign#InstrumentParty")
                .parameters([instrumentId: instId, roleEnumId: 'TP_ADVISING_BANK', partyId: commId]).call()
        
        then:
        ec.message.hasError()
        ec.message.errors.any { it.contains("requires a Bank party") }
    }

    def "BDD-CMN-TP-09.1: Advising bank (Receiver) strictly requires RMA"() {
        given:
        def instVal = ec.entity.makeValue("trade.TradeInstrument").setAll([instrumentTypeEnumId: "IMPORT_LC"]).setSequencedIdPrimary().create()
        String instId = instVal.instrumentId
        def bankId = testPrefix + "_NO_RMA_09_1"
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: bankId, partyTypeEnumId: 'PTY_BANK', 
                            partyName: 'Spec No RMA Bank 2', hasActiveRMA: false, kycStatus: 'KYC_ACTIVE']).call()

        when:
        ec.service.sync().name("trade.TradeCommonServices.assign#InstrumentParty")
                .parameters([instrumentId: instId, roleEnumId: 'TP_ADVISING_BANK', partyId: bankId]).call()

        then:
        ec.message.hasError()
        ec.message.getErrorsString().contains("has no active RMA")
    }

    def "BDD-CMN-TP-09.2: Advise-through bank is exempt from RMA requirement"() {
        given:
        def instVal = ec.entity.makeValue("trade.TradeInstrument").setAll([instrumentTypeEnumId: "IMPORT_LC"]).setSequencedIdPrimary().create()
        String instId = instVal.instrumentId
        def advBankId = testPrefix + "_ADV_09_2"
        def thrBankId = testPrefix + "_THR_09_2"
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: advBankId, partyTypeEnumId: 'PTY_BANK', partyName: 'Adv Bank', hasActiveRMA: true, kycStatus: 'KYC_ACTIVE']).call()
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: thrBankId, partyTypeEnumId: 'PTY_BANK', partyName: 'Thr Bank', hasActiveRMA: false, kycStatus: 'KYC_ACTIVE']).call()

        when:
        ec.service.sync().name("trade.TradeCommonServices.assign#InstrumentParty")
                .parameters([instrumentId: instId, roleEnumId: 'TP_ADVISING_BANK', partyId: advBankId]).call()
        ec.service.sync().name("trade.TradeCommonServices.assign#InstrumentParty")
                .parameters([instrumentId: instId, roleEnumId: 'TP_ADVISE_THROUGH_BANK', partyId: thrBankId]).call()

        then:
        !ec.message.hasError()
    }

    def "BDD-CMN-TP-10: Assign Reimbursing Bank (Missing Nostro) -> Fail"() {
        given:
        def instVal = ec.entity.makeValue("trade.TradeInstrument").setAll([instrumentTypeEnumId: "IMPORT_LC"]).setSequencedIdPrimary().create()
        String instId = instVal.instrumentId
        def bankId = testPrefix + "_NO_NOSTRO_10"
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: bankId, partyTypeEnumId: 'PTY_BANK', 
                             partyName: 'Spec No Nostro Bank', hasActiveRMA: true, kycStatus: 'KYC_ACTIVE', nostroAccountRef: null]).call()
        
        when:
        ec.service.sync().name("trade.TradeCommonServices.assign#InstrumentParty")
                .parameters([instrumentId: instId, roleEnumId: 'TP_REIMBURSING_BANK', partyId: bankId]).call()
        
        then:
        ec.message.hasError()
        ec.message.errors.any { it.contains("Nostro") }
    }

    def "BDD-CMN-TP-06: Update: Role reassignment updates existing record"() {
        given:
        def instVal = ec.entity.makeValue("trade.TradeInstrument").setAll([instrumentTypeEnumId: "IMPORT_LC"]).setSequencedIdPrimary().create()
        String instId = instVal.instrumentId
        def commId1 = testPrefix + "_COMM_06_1"
        def commId2 = testPrefix + "_COMM_06_2"
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: commId1, partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'Old Applicant', kycStatus: 'KYC_ACTIVE']).call()
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: commId2, partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'New Applicant', kycStatus: 'KYC_ACTIVE']).call()

        ec.service.sync().name("trade.TradeCommonServices.assign#InstrumentParty")
                .parameters([instrumentId: instId, roleEnumId: 'TP_APPLICANT', partyId: commId1]).call()

        when:
        ec.service.sync().name("trade.TradeCommonServices.assign#InstrumentParty")
                .parameters([instrumentId: instId, roleEnumId: 'TP_APPLICANT', partyId: commId2]).call()

        then:
        !ec.message.hasError()
        def junc = ec.entity.find("trade.TradeInstrumentParty").condition([instrumentId: instId, roleEnumId: 'TP_APPLICANT']).one()
        junc.partyId == commId2
    }

    def "BDD-CMN-TP-18: Role Uniqueness Enforcement (PK Validation)"() {
        given:
        def instVal = ec.entity.makeValue("trade.TradeInstrument").setAll([instrumentTypeEnumId: "IMPORT_LC"]).setSequencedIdPrimary().create()
        String instId = instVal.instrumentId
        def bankId1 = testPrefix + "_BANK_18_1"
        def bankId2 = testPrefix + "_BANK_18_2"
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: bankId1, partyTypeEnumId: 'PTY_BANK', partyName: 'Bank 1', hasActiveRMA: true, kycStatus: 'KYC_ACTIVE']).call()
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: bankId2, partyTypeEnumId: 'PTY_BANK', partyName: 'Bank 2', hasActiveRMA: true, kycStatus: 'KYC_ACTIVE']).call()

        when:
        ec.service.sync().name("trade.TradeCommonServices.assign#InstrumentParty")
                .parameters([instrumentId: instId, roleEnumId: 'TP_ADVISING_BANK', partyId: bankId1]).call()
        ec.service.sync().name("trade.TradeCommonServices.assign#InstrumentParty")
                .parameters([instrumentId: instId, roleEnumId: 'TP_ADVISING_BANK', partyId: bankId2]).call()

        then:
        ec.entity.find("trade.TradeInstrumentParty").condition([instrumentId: instId, roleEnumId: 'TP_ADVISING_BANK']).count() == 1
    }

    def "BDD-CMN-TP-11: Reject confirming bank insufficient FI limit"() {
        setup:
        def bankId = testPrefix + "_SMALL_BANK_11"
        def commId1 = testPrefix + "_APP_11"
        def commId2 = testPrefix + "_BEN_11"
        
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: bankId, partyTypeEnumId: 'PTY_BANK', 
                             partyName: 'Small Limit Bank', swiftBic: 'SMALLXXX', hasActiveRMA: true, 
                             kycStatus: 'KYC_ACTIVE', fiLimitAvailable: 1000.0]).call()
        
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
                .parameters([lcAmount: 10000.0, lcCurrencyUomId: 'USD',
                             instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: commId1], [roleEnumId: 'TP_BENEFICIARY', partyId: commId2]]]).call()
        String instId = res.instrumentId
                
        when:
        ec.service.sync().name("trade.TradeCommonServices.assign#InstrumentParty")
                .parameters([instrumentId: instId, roleEnumId: 'TP_CONFIRMING_BANK', partyId: bankId]).call()

        then:
        ec.message.hasError()
        ec.message.getErrorsString().toLowerCase().contains("insufficient")
    }

    def "BDD-CMN-TP-12: Create LC with party role assignments"() {
        given:
        def commId1 = testPrefix + "_APP_12"
        def commId2 = testPrefix + "_BEN_12"
        def bankId = testPrefix + "_ADV_12"
        
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: commId1, partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'App 12', kycStatus: 'KYC_ACTIVE']).call()
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: commId2, partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'Ben 12', kycStatus: 'KYC_ACTIVE']).call()
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: bankId, partyTypeEnumId: 'PTY_BANK', partyName: 'Bank 12', hasActiveRMA: true, kycStatus: 'KYC_ACTIVE']).call()

        when:
        def res = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
                .parameters([lcAmount: 5000.0, lcCurrencyUomId: 'USD',
                             instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: commId1], [roleEnumId: 'TP_BENEFICIARY', partyId: commId2], [roleEnumId: 'TP_ADVISING_BANK', partyId: bankId]]]).call()
        String instId = res.instrumentId
        
        then:
        def juncs = ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", instId).list()
        juncs.any { it.roleEnumId == 'TP_APPLICANT' }
        juncs.any { it.roleEnumId == 'TP_BENEFICIARY' }
        juncs.any { it.roleEnumId == 'TP_ADVISING_BANK' }
    }

    // --- FROM LimitServicesSpec ---

    def "Test CREATE CustomerFacility validates base limits"() {
        given:
        def facRes = ec.entity.makeValue("trade.CustomerFacility")
            .setAll([totalApprovedLimit: 1000.0, utilizedAmount: 0.0])
        facRes.setSequencedIdPrimary()
        facRes.create()
        def facId = facRes.facilityId
            
        when:
        def res = ec.service.sync().name("trade.LimitServices.calculate#Earmark").parameters([facilityId: facId, amount: 500.0]).call()

        then:
        res.isAllowed == true
    }

    def "Test GET CustomerFacilities returns list for owner"() {
        given:
        def partyId = testPrefix + "_LIMIT_TEST"
        ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
            .parameters([partyId: partyId, partyName: "Limit Test Party", partyTypeEnumId: "PTY_COMMERCIAL", kycStatus: "KYC_ACTIVE"]).call()
        def facRes = ec.entity.makeValue("trade.CustomerFacility")
            .setAll([ownerPartyId: partyId, totalApprovedLimit: 5000.0, utilizedAmount: 1000.0])
        facRes.setSequencedIdPrimary()
        facRes.create()
        def facId = facRes.facilityId
            
        when:
        def res = ec.service.sync().name("trade.LimitServices.get#CustomerFacilities").parameters([partyId: partyId]).call()
        
        then:
        res.facilityList != null
        res.facilityList.size() >= 1
        def fac = res.facilityList.find { it.facilityId == facId }
        fac.limitAmount == 5000.0
        fac.available == 4000.0
    }

    // --- FROM UserAccountServicesSpec ---

    def "Get current user fails when not logged in"() {
        when:
        ec.user.logoutUser()
        ec.artifactExecution.disableAuthz()
        def result = [:]
        try {
            result = ec.service.sync().name("trade.UserAccountServices.get#CurrentUser").call()
        } catch (Exception e) {
            logger.info("Caught expected failure: ${e.message}")
        }

        then:
        ec.message.hasError() || result.userId == null
    }

    def "Login and get current user returns profile"() {
        given:
        boolean loggedIn = ec.user.loginUser(testUsername, "Password123!")
        assert loggedIn == true
        ec.message.clearAll()
        
        when:
        ec.artifactExecution.disableAuthz()
        def result = ec.service.sync().name("trade.UserAccountServices.get#CurrentUser").call()
        ec.artifactExecution.enableAuthz()

        then:
        !ec.message.hasError()
        result.userId == testUserId
        result.username == testUsername
        result.firstName == "Test"
        result.lastName == "User"
        result.emailAddress == testEmail
        result.roles.contains("TRADE_MAKER")
        result.delegationTierId == "TIER_1"
        result.customLimit == 50000.00
    }

    def "Logout invalidates the session"() {
        given:
        boolean loggedIn = ec.user.loginUser(testUsername, "Password123!")
        assert loggedIn == true

        when:
        ec.artifactExecution.disableAuthz()
        ec.service.sync().name("trade.UserAccountServices.logout#User").call()
        ec.artifactExecution.enableAuthz()

        then:
        ec.user.userId == "trade.admin"
    }

    def "Change password updates the user record"() {
        given:
        boolean loggedIn = ec.user.loginUser(testUsername, "Password123!")
        assert loggedIn == true

        when:
        ec.artifactExecution.disableAuthz()
        ec.service.sync().name("trade.UserAccountServices.change#OwnPassword")
            .parameters([oldPassword: "Password123!", newPassword: "NewPassword123!", newPasswordVerify: "NewPassword123!"])
            .call()
        ec.artifactExecution.enableAuthz()

        then:
        !ec.message.hasError()
        
        // Verify new password works
        and:
        ec.user.logoutUser()
        boolean loggedInWithNew = ec.user.loginUser(testUsername, "NewPassword123!")
        assert loggedInWithNew == true
    }

    // --- FROM TradePartyLcIntegrationSpec ---

    def "SC-12: Create LC with 4 Normalized Parties"() {
        when:
        def result = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
                .parameters([
                    lcAmount: 50000,
                    lcCurrencyUomId: 'USD',
                    instrumentParties: [
                        [roleEnumId: 'TP_APPLICANT', partyId: testPrefix + '_APP_001'],
                        [roleEnumId: 'TP_BENEFICIARY', partyId: testPrefix + '_BEN_001'],
                        [roleEnumId: 'TP_ADVISING_BANK', partyId: testPrefix + '_BANK_ADV'],
                        [roleEnumId: 'TP_CONFIRMING_BANK', partyId: testPrefix + '_BANK_CONF']
                    ]
                ])
                .call()
        lc001Id = result.instrumentId

        then:
        !ec.message.hasError()
        def juncs = ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", lc001Id).list()
        juncs.any { it.roleEnumId == 'TP_APPLICANT' && it.partyId == testPrefix + '_APP_001' }
        juncs.any { it.roleEnumId == 'TP_BENEFICIARY' && it.partyId == testPrefix + '_BEN_001' }
        juncs.any { it.roleEnumId == 'TP_ADVISING_BANK' && it.partyId == testPrefix + '_BANK_ADV' }
        juncs.any { it.roleEnumId == 'TP_CONFIRMING_BANK' && it.partyId == testPrefix + '_BANK_CONF' }
    }

    def "SC-13: Select ANY BANK -> verify availableWithEnumId and no TP_NEGOTIATING_BANK"() {
        when:
        def result = ec.service.sync().name("trade.importlc.ImportLcServices.create#ImportLetterOfCredit")
                .parameters([
                    lcAmount: 10000, lcCurrencyUomId: 'USD',
                    availableWithEnumId: 'AW_ANY_BANK', availableByEnumId: 'AVB_BY_NEGOTIATION',
                    instrumentParties: [
                        [roleEnumId: 'TP_APPLICANT', partyId: testPrefix + '_APP_001'],
                        [roleEnumId: 'TP_BENEFICIARY', partyId: testPrefix + '_BEN_001']
                    ]
                ])
                .call()
        lc002Id = result.instrumentId

        then:
        !ec.message.hasError()
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", lc002Id).one()
        lc.availableWithEnumId == 'AW_ANY_BANK'
    }
}
