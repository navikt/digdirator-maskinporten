package tokenxcanary

import com.fasterxml.jackson.module.kotlin.readValue
import com.nimbusds.jwt.JWTParser
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.ktor.util.KtorExperimentalAPI
import org.amshove.kluent.shouldBeEqualTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import tokenxcanary.http.objectMapper
import tokenxcanary.token.AccessTokenResponse
import kotlin.test.fail

@KtorExperimentalAPI
class MaskinportenClientDigdiratorTest {

    @Test
    fun `Generate and sign an JWT and make a successful request on token_endpoint to configured IDP`() {
        withMockOAuth2Server {
            val mockOAuth2Server = this
            val environment = testEnvironment(this)
            withTestApplication({
                testApplication(mockOAuth2Server, environment)
            }) {
                with(handleRequest(HttpMethod.Get, "/token")) {
                    assertThat("Should return OK response", response.status() == HttpStatusCode.OK)
                    response.content?.let {
                        val body = objectMapper.readValue<AccessTokenResponse>(it)
                        val claimsSet = JWTParser.parse(body.accessToken).jwtClaimsSet
                        claimsSet.getStringClaim("iss") shouldBeEqualTo mockOAuth2Server.issuerUrl("maskinporten").toString()
                        claimsSet.getStringClaim("scope") shouldBeEqualTo environment.maskinporten.scopes
                    } ?: fail("No body returned")
                }
            }
        }
    }
}
