package trade

import spock.lang.Specification
import org.moqui.Moqui
import org.moqui.context.ExecutionContext

/**
 * ABOUTME: TradeSeedDataSpec verifies that the mandatory seed data (Enums, Status) is correctly loaded.
 */
class TradeSeedDataSpec extends Specification {
    protected ExecutionContext ec
    
    def setup() {
        ec = Moqui.getExecutionContext()
        ec.artifactExecution.disableAuthz()
        ec.user.loginUser("trade.admin", "trade123")
        
        // Set isolated ID generation ranges - use 4800000
        ec.entity.tempSetSequencedIdPrimary("trade.TradeInstrument", 47000000, 1000)

        // Manually load seed data for verification
        ec.entity.makeDataLoader().location("component://TradeFinance/data/TradeFinanceSeedData.xml").load()
        ec.entity.makeDataLoader().location("component://TradeFinance/data/TradeClauseSeedData.xml").load()
        ec.entity.makeDataLoader().location("component://TradeFinance/entity/TradeCommonEntities.xml").load()
        ec.entity.makeDataLoader().location("component://TradeFinance/entity/ImportLcEntities.xml").load()
    }
    
    def cleanup() {
        if (ec != null) {
            ec.entity.tempResetSequencedIdPrimary("trade.TradeInstrument")
            ec.destroy()
        }
    }
    
    def "verifies mandatory enumerations exist"() {
        expect:
        ec.entity.find("moqui.basic.Enumeration").condition("enumId", "SANCTION_PENDING").one() != null
        ec.entity.find("moqui.basic.Enumeration").condition("enumId", "ALLOWED").one() != null
        ec.entity.find("moqui.basic.Enumeration").condition("enumId", "CONFIRMED").one() != null
        ec.entity.find("moqui.basic.Enumeration").condition("enumId", "LCT_IRREVOCABLE").one() != null
        ec.entity.find("moqui.basic.Enumeration").condition("enumId", "AMEND_INCREASE").one() != null
        ec.entity.find("moqui.basic.Enumeration").condition("enumId", "TIER_4").one() != null
        ec.entity.find("moqui.basic.Enumeration").condition("enumId", "ISSUANCE_FEE").one() != null
        ec.entity.find("moqui.basic.Enumeration").condition("enumId", "SIGHT_PAYMENT").one() != null
    }

    def "verifies mandatory status items exist"() {
        expect:
        ec.entity.find("moqui.basic.StatusItem").condition("statusId", "AMEND_DRAFT").one() != null
        ec.entity.find("moqui.basic.StatusItem").condition("statusId", "PRES_EXAMINING").one() != null
        ec.entity.find("moqui.basic.StatusItem").condition("statusId", "SG_ISSUED").one() != null
    }
}
