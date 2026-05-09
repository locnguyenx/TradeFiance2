package trade

import org.junit.jupiter.api.AfterAll
import org.junit.platform.suite.api.SelectClasses
import org.junit.platform.suite.api.Suite
import org.moqui.Moqui

@Suite
@SelectClasses([
    SwiftValidationSpec.class,
    SwiftGenerationSpec.class,
    SwiftPartyGenerationSpec.class,
    SwiftReimbursementSpec.class,
    ComplianceServicesSpec.class,
    DualApprovalSpec.class,
    EndToEndImportLcSpec.class,
    BddImportLcModuleSpec.class,
    BddCommonModuleSpec.class,
    AuthorizationServicesSpec.class,
    AuthorizationDataLossSpec.class,
    DraftLcSpec.class,
    ImportLcEntitiesSpec.class,
    ImportLcServicesSpec.class,
    ImportLcValidationServicesSpec.class,
    LimitServicesSpec.class,
    NostroApiSpec.class,
    RestApiEndpointsSpec.class,
    ShippingGuaranteeSpec.class,
    TradePartySpec.class,
    TradePartyLcIntegrationSpec.class,
    TradeCommonEntitiesSpec.class,
    TradeSeedDataSpec.class,
    UserAccountServicesSpec.class,
    InstrumentDataIntegritySpec.class,
    TradeFinanceHardeningSpec.class,
    TradeSwiftTriggerSpec.class,
    TradeSwiftAutoTriggerSpec.class,
    TradeTransactionSpec.class,
    TradeTransactionViewSpec.class,
    TradeSearchSpec.class,
    TransactionIssuanceBugSpec.class,
    TradeListServicesSpec.class
])
class TradeFinanceMoquiSuite {
    @AfterAll
    static void destroyMoqui() {
        Moqui.destroyActiveExecutionContextFactory()
    }
}
