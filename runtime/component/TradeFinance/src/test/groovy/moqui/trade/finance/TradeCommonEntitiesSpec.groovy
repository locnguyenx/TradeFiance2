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
}
