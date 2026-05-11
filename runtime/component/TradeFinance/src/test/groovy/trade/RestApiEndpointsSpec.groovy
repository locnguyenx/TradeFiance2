package trade

import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import spock.lang.Specification
import org.moqui.screen.ScreenTest
import org.moqui.screen.ScreenTest.ScreenTestRender
import spock.lang.Shared
import org.moqui.entity.EntityCondition

// ABOUTME: RestApiEndpointsSpec verifies the existence and contract of the headless REST API facade.
// ABOUTME: Uses ScreenTest to perform real HTTP-level verification against trade.rest.xml.

class RestApiEndpointsSpec extends Specification {
    @Shared protected ExecutionContext ec
    @Shared ScreenTest screenTest
    @Shared String testPrefix
    
    @Shared String applicantId
    @Shared String beneficiaryId
    @Shared String facilityId

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.transaction.runUseOrBegin(null, null) {
            ec.artifactExecution.disableAuthz()
            ec.user.loginUser("trade.maker", "trade123")
            testPrefix = "REST-API-" + System.currentTimeMillis()
            
            applicantId = testPrefix + "-APP"
            beneficiaryId = testPrefix + "-BEN"

            // Ensure test parties exist
            ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: applicantId, partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'ACME Corp REST', kycStatus: 'KYC_ACTIVE']).call()
            ec.service.sync().name("trade.TradeCommonServices.create#TradeParty")
                .parameters([partyId: beneficiaryId, partyTypeEnumId: 'PTY_COMMERCIAL', partyName: 'Global Exports REST', kycStatus: 'KYC_ACTIVE']).call()

            // Set isolated ID generation ranges - use 11500000
            ec.entity.tempSetSequencedIdPrimary("trade.CustomerFacility", 11500000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrument", 11500000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit", 11500000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.TradeTransaction", 11500000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrumentParty", 11500000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.importlc.TradeDocumentPresentation", 11500000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLcSettlement", 11500000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLcAmendment", 11500000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLcShippingGuarantee", 11500000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLcInternalAmendment", 11500000, 1000)

            def facRes = ec.entity.makeValue("trade.CustomerFacility")
                .setAll([ownerPartyId: applicantId, totalApprovedLimit: 1000000.0, utilizedAmount: 0.0, currencyUomId: "USD", statusId: "FAC_ACTIVE"])
                .setSequencedIdPrimary().create()
            facilityId = facRes.facilityId
        }
            
        screenTest = ec.screen.makeTest()
            .rootScreen("component://webroot/screen/webroot.xml")
            .baseScreenPath("rest")
        println "DEBUG: setupSpec RestApiEndpointsSpec complete"
    }

    def cleanupSpec() {
        if (ec != null) {
            ec.entity.tempResetSequencedIdPrimary("trade.CustomerFacility")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrument")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeTransaction")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrumentParty")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.TradeDocumentPresentation")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLcSettlement")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLcAmendment")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLcShippingGuarantee")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLcInternalAmendment")
            ec.destroy()
        }
    }

    def setup() {
        ec.message.clearAll()
        ec.artifactExecution.disableAuthz()
    }

    def cleanup() {
        ec.message.clearAll()
    }

    String createTestLc(String suffix) {
        String ref = testPrefix + "_" + suffix + "_REF"
        ec.user.internalLoginUser("trade.maker")
        ScreenTestRender str = screenTest.render("s1/trade/import-lc",
            [instrumentRef: ref, lcAmount: 5000.0, lcCurrencyUomId: "USD",
             customerFacilityId: facilityId,
             lcTypeEnumId: 'LCT_IRREVOCABLE', availableByEnumId: 'AVB_BY_NEGOTIATION', confirmationEnumId: 'CONF_WITHOUT',
             availableWithEnumId: 'AW_ANY_BANK',
             instrumentParties: [[roleEnumId: 'TP_APPLICANT', partyId: applicantId],
                                 [roleEnumId: 'TP_BENEFICIARY', partyId: beneficiaryId]]], "post")
        if (str.errorMessages) {
            throw new Exception("Failed to create LC: ${str.errorMessages}")
        }
        def json = new groovy.json.JsonSlurper().parseText(str.output)
        return json.instrumentId
    }

    void approveIssuance(String instrumentId) {
        def tx = ec.entity.find("trade.TradeTransaction").condition([instrumentId: instrumentId, transactionTypeEnumId: 'IMP_NEW']).disableAuthz().one()
        if (tx && tx.transactionStatusId != 'TX_APPROVED') {
            ec.user.internalLoginUser("trade.checker")
            screenTest.render("s1/trade/authorize", [transactionId: tx.transactionId, skipFourEyes: true], "post")
            ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).updateAll([businessStateId: "LC_ISSUED"])
            ec.user.internalLoginUser("trade.maker")
        }
    }

    String createTestPresentation(String instrumentId) {
        approveIssuance(instrumentId)
        ec.user.internalLoginUser("trade.maker")
        ScreenTestRender str = screenTest.render("s1/trade/import-lc/${instrumentId}/presentation",
            [instrumentId: instrumentId, claimAmount: 1000.0, claimCurrency: "USD"], "post")
        if (str.errorMessages) {
            println "Error creating presentation: " + str.errorMessages
            throw new Exception("Failed to create presentation: ${str.errorMessages}")
        }
        if (!str.output) {
            println "Empty output for presentation creation. Error messages: " + str.errorMessages
            throw new Exception("Empty output for presentation creation")
        }
        def json = new groovy.json.JsonSlurper().parseText(str.output)
        
        // Also approve the presentation transaction to allow settlement
        def txPres = ec.entity.find("trade.TradeTransaction")
                .condition([instrumentId: instrumentId, transactionTypeEnumId: 'IMP_PRESENTATION', transactionStatusId: 'TX_DRAFT']).one()
        if (txPres) {
            ec.user.internalLoginUser("trade.checker")
            screenTest.render("s1/trade/authorize", [transactionId: txPres.transactionId, skipFourEyes: true], "post")
            ec.user.internalLoginUser("trade.maker")
        }
        
        return json.presentationId
    }

    def "should create an Import LC via POST"() {
        when:
        String instrumentId = createTestLc("C1")

        then:
        instrumentId != null
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instrumentId).one()
        lc != null
        lc.businessStateId == "LC_DRAFT"
    }

    def "should fetch LC details via GET"() {
        given:
        String instrumentId = createTestLc("G1")

        when:
        ScreenTestRender str = screenTest.render("s1/trade/import-lc/${instrumentId}", [:], "get")
        def json = new groovy.json.JsonSlurper().parseText(str.output)

        then:
        str.errorMessages.size() == 0
        json.instrumentId == instrumentId
        json.amount == 5000.0
    }

    def "should update an Import LC via PATCH"() {
        given:
        String instrumentId = createTestLc("U1")

        when:
        ScreenTestRender str = screenTest.render("s1/trade/import-lc/${instrumentId}", [lcAmount: 7500.0], "post")
        def json = new groovy.json.JsonSlurper().parseText(str.output)

        then:
        str.errorMessages.size() == 0
        json.instrumentId == instrumentId
        def lc = ec.entity.find("trade.TradeInstrument").condition("instrumentId", instrumentId).one()
        lc.amount == 7500.0
    }

    def "should create a Presentation via POST"() {
        given:
        String instrumentId = createTestLc("P1")

        when:
        String presentationId = createTestPresentation(instrumentId)

        then:
        presentationId != null
        def pres = ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("presentationId", presentationId).one()
        pres.instrumentId == instrumentId
    }

    def "should create a Settlement via POST"() {
        given:
        String instrumentId = createTestLc("S1")
        String presentationId = createTestPresentation(instrumentId)

        when:
        ScreenTestRender str = screenTest.render("s1/trade/import-lc/${instrumentId}/settlement",
            [presentationId: presentationId, principalAmount: 1000.0, settlementAmount: 1000.0, 
             settlementCurrencyUomId: "USD", settlementTypeEnumId: 'SIGHT_PAYMENT'], "post")
        def json = new groovy.json.JsonSlurper().parseText(str.output)

        then:
        str.errorMessages.size() == 0
        json.settlementId != null
        def sett = ec.entity.find("trade.importlc.ImportLcSettlement").condition("settlementId", json.settlementId).one()
        sett.presentationId == presentationId
    }

    def "should create an Amendment via POST"() {
        given:
        String instrumentId = createTestLc("A1")
        approveIssuance(instrumentId)

        when:
        ScreenTestRender str = screenTest.render("s1/trade/import-lc/${instrumentId}/amendment/external",
            [amendmentTypeEnumId: "AMEND_INCREASE", amountIncrease: 500.0], "post")
        def json = new groovy.json.JsonSlurper().parseText(str.output)

        then:
        str.errorMessages.size() == 0
        json.amendmentId != null
        def amend = ec.entity.find("trade.importlc.ImportLcAmendment").condition("amendmentId", json.amendmentId).one()
        amend.instrumentId == instrumentId
    }

    def "should create a Shipping Guarantee via POST"() {
        given:
        String instrumentId = createTestLc("SG1")
        approveIssuance(instrumentId)

        when:
        ScreenTestRender str = screenTest.render("s1/trade/import-lc/${instrumentId}/shipping-guarantee",
            [invoiceAmount: 1000.0, transportDocReference: "BOL-123"], "post")
        def json = new groovy.json.JsonSlurper().parseText(str.output)

        then:
        str.errorMessages.size() == 0
        json.guaranteeId != null
        def sg = ec.entity.find("trade.importlc.ImportLcShippingGuarantee").condition("guaranteeId", json.guaranteeId).one()
        sg.instrumentId == instrumentId
    }

    def "should create an Internal Amendment via POST"() {
        given:
        String instrumentId = createTestLc("IA1")
        approveIssuance(instrumentId)

        when:
        ScreenTestRender str = screenTest.render("s1/trade/import-lc/${instrumentId}/amendment/internal",
            [internalNotes: "Bank internal note"], "post")
        def json = new groovy.json.JsonSlurper().parseText(str.output)

        then:
        str.errorMessages.size() == 0
        json.internalAmendmentId != null
        def ia = ec.entity.find("trade.importlc.ImportLcInternalAmendment").condition("internalAmendmentId", json.internalAmendmentId).one()
        ia.instrumentId == instrumentId
    }
}