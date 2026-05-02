package trade

import org.junit.jupiter.api.AfterAll
import org.junit.platform.suite.api.SelectClasses
import org.junit.platform.suite.api.Suite
import org.moqui.Moqui

@Suite
@SelectClasses([
    SwiftValidationSpec.class,
    SwiftGenerationSpec.class,
    ComplianceServicesSpec.class,
    DualApprovalSpec.class,
    EndToEndImportLcSpec.class,
    BddImportLcModuleSpec.class,
    BddCommonModuleSpec.class,
    AuthVerificationSpec.class,
    AuthorizationServicesSpec.class,
    DraftLcSpec.class,
    ImportLcEntitiesSpec.class,
    ImportLcServicesSpec.class,
    ImportLcValidationServicesSpec.class,
    LimitServicesSpec.class,
    RestApiEndpointsSpec.class,
    ShippingGuaranteeSpec.class,
    TradePartySpec.class,
    TradePartyLcIntegrationSpec.class,
    TradeCommonEntitiesSpec.class,
    TradeSeedDataSpec.class
])
class TradeFinanceMoquiSuite {
    @AfterAll
    static void destroyMoqui() {
        Moqui.destroyActiveExecutionContextFactory()
    }
}
