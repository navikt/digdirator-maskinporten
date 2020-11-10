package maskinporten

import com.fasterxml.jackson.module.kotlin.readValue
import com.nimbusds.jwt.JWTParser
import io.ktor.application.Application
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.ktor.util.KtorExperimentalAPI
import maskinporten.config.Environment
import maskinporten.http.objectMapper
import maskinporten.token.AccessTokenResponse
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import kotlin.test.fail

class MaskinportenDigdiratorTest {

    @KtorExperimentalAPI
    @Test
    fun `Generate, sign the JWT and make a request on token_endpoint to configured IDP`() {
        withMockOAuth2Server {
            val issuerId = "maskinporten"
            val environment = Environment(
                maskinporten = Environment.Maskinporten(
                    wellKnownUrl = this.wellKnownUrl(issuerId = issuerId).toString()
                )
            )
            withTestApplication({
                setupMockApplication(this@withMockOAuth2Server, environment)
            }) {
                with(handleRequest(HttpMethod.Get, "/token")) {
                    assertThat("Should return OK response", response.status() == HttpStatusCode.OK)
                    response.content?.let {
                        val body = objectMapper.readValue<AccessTokenResponse>(it)
                        val claimsSet = JWTParser.parse(body.accessToken).jwtClaimsSet
                        assertThat("Token claims should have scope", claimsSet.claims["scope"] == environment.maskinporten.scopes)
                    } ?: fail("No body returned")
                }
            }
        }
    }
}

@KtorExperimentalAPI
internal fun Application.setupMockApplication(mockOAuth2Server: MockOAuth2Server, environment: Environment) {
    val applicationStatus = ApplicationStatus(
        running = true,
        initialized = false
    )
    mockOAuth2Server.enqueueCallback(
        DefaultOAuth2TokenCallback(
            issuerId = environment.maskinporten.clientId,
            audience = null,
        )
    )
    setupHttpServer(environment, applicationStatus)
}

fun <R> withMockOAuth2Server(
    test: MockOAuth2Server.() -> R
): R {
    val server = MockOAuth2Server()
    server.start()
    try {
        return server.test()
    } finally {
        server.shutdown()
    }
}
