package trade

import spock.lang.Specification
import spock.lang.Stepwise
import spock.lang.Shared
import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityCondition

/**
 * ABOUTME: TradePartySpec validates the creation and management of TradeParty records and their association with instruments.
 * Enforces compliance (KYC) and role-specific validation (RMA, FI Limit).
 */
class TradePartySpec extends Specification {
    @Shared protected ExecutionContext ec
    @Shared String testPrefix

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.transaction.runUseOrBegin(null, null) {
            ec.user.loginUser("trade.admin", "trade123")
            ec.artifactExecution.disableAuthz()
            testPrefix = "INST-TP-" + System.currentTimeMillis()

        // Set isolated ID generation ranges - use 4600000
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrument", 44000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit", 44000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeTransaction", 44000000, 1000)
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrumentParty", 44000000, 1000)
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
        ec.entity.find("trade.TradeInstrumentParty").condition([instrumentId: instId, partyId: bankId]).list().size() == 2
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
        ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", instId).count() == 3
    }
}
