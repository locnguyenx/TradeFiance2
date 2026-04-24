
import spock.lang.Specification
import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import org.moqui.impl.context.ArtifactExecutionFacadeImpl

class AuthVerificationSpec extends Specification {
    protected ExecutionContext ec
    
    def setup() {
        ec = Moqui.getExecutionContext()
    }
    
    def cleanup() {
        ec.destroy()
    }
    
    def "Deep verify TRADE_MAKER permissions"() {
        expect:
        def authzList = ec.entity.find("moqui.security.ArtifactAuthz").condition("userGroupId", "TRADE_MAKER").disableAuthz().list()
        println "DEEP_AUTH: Found ${authzList.size()} authz records for TRADE_MAKER"
        
        authzList.each { authz ->
            println "  - AuthzId: ${authz.artifactAuthzId}, GroupId: ${authz.artifactGroupId}, Action: ${authz.authzActionEnumId}"
            def members = ec.entity.find("moqui.security.ArtifactGroupMember").condition("artifactGroupId", authz.artifactGroupId).disableAuthz().list()
            members.each { m ->
                println "    - Member: ${m.artifactName}, Type: ${m.artifactTypeEnumId}, Pattern: ${m.nameIsPattern}"
            }
        }
        
        and: "Checking View Entity Definition"
        def ed = ec.entity.getEntityDefinition("trade.importlc.ImportLetterOfCredit")
        println "DEEP_AUTH: Entity ImportLetterOfCredit defined=${ed != null}"
        if (ed != null) println "DEEP_AUTH: Entity full name=${ed.getFullEntityName()}"

        and: "Simulating a View check"
        def user = "trade.maker"
        ec.user.internalLoginUser(user)
        // Format: "typeEnumId:actionEnumId:name"
        boolean canView = ArtifactExecutionFacadeImpl.isPermitted("AT_ENTITY:AUTHZA_VIEW:trade.TradeInstrument", ec as org.moqui.impl.context.ExecutionContextImpl)
        println "DEEP_AUTH: user=${user}, canView TradeInstrument=${canView}"
        
        authzList.size() > 0
    }
}
