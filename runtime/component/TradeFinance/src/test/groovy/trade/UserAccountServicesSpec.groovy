package trade

import org.moqui.context.ExecutionContext
import org.moqui.Moqui
import spock.lang.Specification
import spock.lang.Shared
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Timestamp

/*
 * ABOUTME: UserAccountServicesSpec tests the authentication and profile services for trade users.
 */
class UserAccountServicesSpec extends Specification {
    protected final static Logger logger = LoggerFactory.getLogger(UserAccountServicesSpec.class)
    
    @Shared protected ExecutionContext ec
    @Shared protected String testUserId
    @Shared protected String testUsername
    @Shared protected String testEmail

    def setupSpec() {
        ec = Moqui.getExecutionContext()
        ec.artifactExecution.disableAuthz()
        ec.message.clearAll()
        
        long ts = System.currentTimeMillis()
        testUsername = "test.user." + ts
        testEmail = "test." + ts + "@example.com"
        Timestamp pastDate = new Timestamp(ec.l10n.parseTimestamp("2020-01-01 00:00:00", null).getTime())

        boolean suspended = ec.transaction.begin(null)
        try {
            // Ensure UserGroup exists
            def group = ec.entity.find("moqui.security.UserGroup").condition("userGroupId", "TRADE_MAKER").one()
            if (!group) {
                def newGroup = ec.entity.makeValue("moqui.security.UserGroup")
                newGroup.set("userGroupId", "TRADE_MAKER")
                newGroup.set("description", "Trade Maker")
                newGroup.create()
            }

            // Create UserAccount (generates system userId)
            def uaResult = ec.service.sync().name("org.moqui.impl.UserServices.create#UserAccount")
                .parameters([username: testUsername, 
                            newPassword: "Password123!", newPasswordVerify: "Password123!",
                            emailAddress: testEmail, userFullName: "Test User"])
                .call()
            testUserId = uaResult.userId

            // Ensure Party & Person linked to generated userId
            def party = ec.entity.find("mantle.party.Party").condition("partyId", testUserId).one()
            if (!party) {
                party = ec.entity.makeValue("mantle.party.Party")
                party.set("partyId", testUserId)
                party.set("partyTypeEnumId", "PtyPerson")
                party.create()
            }
            def person = ec.entity.find("mantle.party.Person").condition("partyId", testUserId).one()
            if (!person) {
                person = ec.entity.makeValue("mantle.party.Person")
                person.set("partyId", testUserId)
                person.set("firstName", "Test")
                person.set("lastName", "User")
                person.create()
            }

            // Assign Roles
            def ugm = ec.entity.makeValue("moqui.security.UserGroupMember")
            ugm.set("userId", testUserId)
            ugm.set("userGroupId", "TRADE_MAKER")
            ugm.set("fromDate", pastDate)
            ugm.create()

            // Create Authority Profile
            def profile = ec.entity.makeValue("trade.UserAuthorityProfile")
            profile.set("userAuthorityId", testUserId)
            profile.set("userId", testUserId)
            profile.set("delegationTierId", "TIER_1")
            profile.set("customLimit", 50000.0)
            profile.set("currencyUomId", "USD")
            profile.create()
            
            ec.transaction.commit()
        } catch (Exception e) {
            ec.transaction.rollback("Failed to setup test user", e)
            throw e
        } finally {
            if (suspended) ec.transaction.resume()
        }
        
        ec.artifactExecution.enableAuthz()
    }

    def cleanupSpec() {
        if (ec) {
            ec.artifactExecution.disableAuthz()
            ec.user.logoutUser()
            ec.destroy()
        }
    }

    def setup() {
        if (!ec) ec = Moqui.getExecutionContext()
        ec.user.logoutUser()
        ec.message.clearAll()
    }

    def "Get current user fails when not logged in"() {
        when:
        ec.artifactExecution.disableAuthz()
        def result = [:]
        try {
            result = ec.service.sync().name("trade.UserAccountServices.get#CurrentUser").call()
        } catch (Exception e) {
            logger.info("Caught expected failure: ${e.message}")
        }

        then:
        ec.message.hasError() || result.userId == null
    }

    def "Login and get current user returns profile"() {
        given:
        boolean loggedIn = ec.user.loginUser(testUsername, "Password123!")
        assert loggedIn == true
        ec.message.clearAll()
        
        when:
        ec.artifactExecution.disableAuthz()
        def result = ec.service.sync().name("trade.UserAccountServices.get#CurrentUser").call()
        ec.artifactExecution.enableAuthz()

        then:
        !ec.message.hasError()
        result.userId == testUserId
        result.username == testUsername
        result.firstName == "Test"
        result.lastName == "User"
        result.emailAddress == testEmail
        result.roles.contains("TRADE_MAKER")
        result.delegationTierId == "TIER_1"
        result.customLimit == 50000.00
    }

    def "Logout invalidates the session"() {
        given:
        boolean loggedIn = ec.user.loginUser(testUsername, "Password123!")
        assert loggedIn == true

        when:
        ec.artifactExecution.disableAuthz()
        ec.service.sync().name("trade.UserAccountServices.logout#User").call()
        ec.artifactExecution.enableAuthz()

        then:
        ec.user.userId == null
    }

    def "Change password updates the user record"() {
        given:
        boolean loggedIn = ec.user.loginUser(testUsername, "Password123!")
        assert loggedIn == true

        when:
        ec.artifactExecution.disableAuthz()
        ec.service.sync().name("trade.UserAccountServices.change#OwnPassword")
            .parameters([oldPassword: "Password123!", newPassword: "NewPassword123!", newPasswordVerify: "NewPassword123!"])
            .call()
        ec.artifactExecution.enableAuthz()

        then:
        !ec.message.hasError()
        
        // Verify new password works
        and:
        ec.user.logoutUser()
        boolean loggedInWithNew = ec.user.loginUser(testUsername, "NewPassword123!")
        assert loggedInWithNew == true
        
        // Change back for other tests
        cleanup:
        ec.artifactExecution.disableAuthz()
        ec.service.sync().name("org.moqui.impl.UserServices.update#Password")
            .parameters([userId: testUserId, oldPassword: "NewPassword123!", newPassword: "Password123!", newPasswordVerify: "Password123!"])
            .call()
    }
}
