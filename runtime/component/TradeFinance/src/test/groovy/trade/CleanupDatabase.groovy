package trade
import org.moqui.Moqui
import org.moqui.context.ExecutionContext
ExecutionContext ec = Moqui.getExecutionContext()
try {
    ec.artifactExecution.disableAuthz()
    def ids = ["LC-ENT-1", "LC-MGMT-TEST", "LC-AMEND-TEST", "LC-PRES-TEST", "LC-SG-TEST", "LC-AMEND-EXT", "LC-PRES-EXT", "LC-SG-EXT"]
    for (id in ids) {
        ec.entity.find("trade.importlc.SwiftMessage").condition("instrumentId", id).deleteAll()
        ec.entity.find("trade.importlc.ImportLcSettlement").condition("instrumentId", id).deleteAll()
        ec.entity.find("trade.importlc.ImportLcAmendment").condition("instrumentId", id).deleteAll()
        ec.entity.find("trade.importlc.TradeDocumentPresentation").condition("instrumentId", id).deleteAll()
        ec.entity.find("trade.importlc.ImportLcShippingGuarantee").condition("instrumentId", id).deleteAll()
        ec.entity.find("trade.TradeInstrumentParty").condition("instrumentId", id).deleteAll()
        ec.entity.find("trade.TradeTransactionAudit").condition("instrumentId", id).deleteAll()
        ec.entity.find("trade.TradeTransaction").condition("instrumentId", id).deleteAll()
        ec.entity.find("trade.importlc.ImportLetterOfCredit").condition("instrumentId", id).deleteAll()
        ec.entity.find("trade.TradeInstrument").condition("instrumentId", id).deleteAll()
    }
    println "Cleanup successful"
} finally {
    ec.destroy()
}
