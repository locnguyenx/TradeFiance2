package trade

import spock.lang.*
import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityCondition

/**
 * ABOUTME: CommonEntitiesSpec validates core entities, seed data, and basic CRUD operations.
 * Covers TradeInstrument, ImportLetterOfCredit, TradeTransaction, and TradeParty entities.
 */
@Stepwise
class CommonEntitiesSpec extends Specification {
    @Shared protected ExecutionContext ec
    @Shared String testPrefix

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.transaction.runUseOrBegin(null, null) {
            ec.user.loginUser("trade.admin", "trade123")
            ec.artifactExecution.disableAuthz()
            testPrefix = "ENT-" + System.currentTimeMillis()

            // Set isolated ID generation ranges - use 92000000 (Module 1 - Entities)
            ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrument", 92000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit", 92000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.TradeTransaction", 92000000, 1000)
            ec.entity.tempSetSequencedIdPrimary("trade.TradeParty", 92000000, 1000)
        }
    }

    def cleanupSpec() {
        if (ec != null) {
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrument")
            ec.entity.tempResetSequencedIdPrimary("trade.importlc.ImportLetterOfCredit")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeTransaction")
            ec.entity.tempResetSequencedIdPrimary("trade.TradeParty")
            ec.destroy()
        }
    }

    def "should verify Seed Data presence"() {
        expect: "Status and Enum seeds are loaded"
        ec.entity.find("moqui.basic.StatusItem").condition("statusId", "LC_DRAFT").one() != null
        ec.entity.find("moqui.basic.Enumeration").condition("enumId", "IMPORT_LC").one() != null
        ec.entity.find("moqui.basic.Enumeration").condition("enumId", "PTY_BANK").one() != null
    }

    def "should support CRUD for TradeInstrument and ImportLetterOfCredit"() {
        given: "A new LC record"
        def instId = testPrefix + "_INST"
        def inst = ec.entity.makeValue("trade.TradeInstrument").setAll([
            instrumentId: instId, instrumentRef: testPrefix + "_REF", instrumentTypeEnumId: "IMPORT_LC",
            amount: 10000.0, currencyUomId: "USD", businessStateId: "LC_DRAFT"
        ])
        
        when: "Creating entities"
        inst.create()
        ec.entity.makeValue("trade.importlc.ImportLetterOfCredit").setAll([
            instrumentId: instId, effectiveAmount: 10000.0, lcTypeEnumId: "LCT_IRREVOCABLE"
        ]).create()

        then: "Records exist and are retrievable"
        def lc = ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instId).one()
        lc != null
        lc.effectiveAmount == 10000.0

        when: "Updating LC"
        lc.set("effectiveAmount", 12000.0).update()

        then: "Update is persisted"
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", instId).one().effectiveAmount == 12000.0
    }

    def "should enforce Referential Integrity for TradeTransaction"() {
        given: "A transaction without a valid instrument"
        def tx = ec.entity.makeValue("trade.TradeTransaction").setAll([
            instrumentId: "NON_EXISTENT", transactionTypeEnumId: "IMP_NEW", transactionStatusId: "TX_DRAFT"
        ])

        when: "Attempting to create"
        tx.setSequencedIdPrimary().create()

        then: "Foreign key violation occurs"
        thrown(Exception)
    }
}
