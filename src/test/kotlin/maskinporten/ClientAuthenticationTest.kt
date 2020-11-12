package maskinporten

import com.nimbusds.jwt.JWT
import io.ktor.util.KtorExperimentalAPI
import maskinporten.token.ClientAuthentication
import org.amshove.kluent.`should be instance of`
import org.amshove.kluent.shouldBeEqualTo
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Date

@KtorExperimentalAPI
class ClientAuthenticationTest {

    @Test
    fun `Generate, sign and Validate JWT`() {
        withMockOAuth2Server {
            val environment = testEnvironment(this)
            val assertion = ClientAuthentication(environment).clientAssertion()
            val parsedToken = parse(assertion)
            val claimsSet = parsedToken.jwtClaimsSet
            parsedToken `should be instance of` JWT::class
            claimsSet.getStringClaim("scope") shouldBeEqualTo environment.maskinporten.scopes
            claimsSet.getStringClaim("iss") shouldBeEqualTo environment.maskinporten.clientId
            claimsSet.getStringListClaim("aud").singleOrNull() shouldBeEqualTo this.issuerUrl("maskinporten").toString()
            val now = Date.from(Instant.now())
            MatcherAssert.assertThat("Claim `iat` is not greater", now.after(Date(claimsSet.getDateClaim("iat").time)))
            MatcherAssert.assertThat("Claim `exp` is not less", now.before(Date(claimsSet.getDateClaim("exp").time)))
        }
    }
}
