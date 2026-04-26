package trade


import spock.lang.Specification
import org.moqui.Moqui
import org.moqui.context.ExecutionContext

// ABOUTME: TradeSeedDataSpec verifies that the mandatory seed data (Enums, Status) is correctly loaded.

class TradeSeedDataSpec extends Specification {
    protected ExecutionContext ec
    
    def setup() {
        ec = Moqui.getExecutionContext()
        ec.artifactExecution.disableAuthz()
        ec.user.loginUser("tf-admin", "moqui")
        // Manually load seed data for verification
        long count1 = ec.entity.makeDataLoader().location("component://TradeFinance/data/TradeFinanceSeedData.xml").load()
        long count2 = ec.entity.makeDataLoader().location("component://TradeFinance/data/TradeClauseSeedData.xml").load()
        ec.logger.info("DEBUG_SEED: Loaded ${count1} records from TradeFinanceSeedData.xml")
        ec.logger.info("DEBUG_SEED: Loaded ${count2} records from TradeClauseSeedData.xml")
    }
    
    def cleanup() {
        ec.destroy()
    }
    
    def "verifies mandatory enumerations exist"() {
        expect:
        ec.entity.find("moqui.basic.Enumeration").condition("enumId", "SANCTION_PENDING").one() != null
        ec.entity.find("moqui.basic.Enumeration").condition("enumId", "ALLOWED").one() != null
        ec.entity.find("moqui.basic.Enumeration").condition("enumId", "CONFIRMED").one() != null
        ec.entity.find("moqui.basic.Enumeration").condition("enumId", "IRREVOCABLE").one() != null
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
