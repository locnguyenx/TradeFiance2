package trade

import org.junit.jupiter.api.AfterAll
import org.junit.platform.suite.api.SelectClasses
import org.junit.platform.suite.api.Suite
import org.moqui.Moqui

/**
 * ABOUTME: TradeFinanceMoquiSuite aggregates all consolidated Trade Finance test specifications.
 * Organized by BRD modules: Common, Lifecycle, Drawing, SWIFT, and Security.
 */
@Suite
@SelectClasses([
    // Module 1: Common & Entities
    TradeCommonSpec.class,
    CommonEntitiesSpec.class,
    
    // Module 2: Issuance & Amendment (Lifecycle)
    ImportLcLifecycleSpec.class,
    PortfolioServicesSpec.class,
    
    // Module 3: Drawing & Settlement
    TradeDrawingSpec.class,
    
    // Module 4: SWIFT & Compliance
    SwiftComplianceSpec.class,
    
    // Module 5: Security & Integrity
    SecurityIntegritySpec.class,
    
    // Module 6: API & Integration
    RestApiIntegrationSpec.class,
    
    // Regression / E2E
    EndToEndImportLcSpec.class,
    
    // Module 7: Inbound SWIFT
    InboundSwiftSpec.class,
    InboundActionSpec.class
])
class TradeFinanceMoquiSuite {
    @AfterAll
    static void destroyMoqui() {
        Moqui.destroyActiveExecutionContextFactory()
    }
}
