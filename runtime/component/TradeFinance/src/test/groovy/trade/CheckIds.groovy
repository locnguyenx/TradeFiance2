
import org.moqui.Moqui
def ec = Moqui.getExecutionContext()
def list = ec.entity.find("trade.TradeInstrument").selectFields(["instrumentId"]).orderBy("-instrumentId").list()
println "Max Instrument IDs:"
list.take(10).each { println it.instrumentId }
ec.destroy()
