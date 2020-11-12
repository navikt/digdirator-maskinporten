package maskinporten

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.ktor.util.KtorExperimentalAPI
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

@KtorExperimentalAPI
class ObservabilityTest {

    @Test
    fun `isReady should answer OK`() {
        withMockOAuth2Server {
            val mockOAuth2Server = this
            withTestApplication({
                testApplication(
                    mockOAuth2Server = mockOAuth2Server,
                    environment = testEnvironment(mockOAuth2Server = mockOAuth2Server)
                )
            }) {
                with(handleRequest(HttpMethod.Get, "/isReady")) {
                    response.status() shouldBeEqualTo HttpStatusCode.OK
                }
            }
        }
    }

    @Test
    fun `isAlive should answer OK application is up and running`() {
        withMockOAuth2Server {
            val mockOAuth2Server = this
            withTestApplication({
                testApplication(
                    mockOAuth2Server = mockOAuth2Server,
                    environment = testEnvironment(mockOAuth2Server = mockOAuth2Server)
                )
            }) {
                with(handleRequest(HttpMethod.Get, "/isAlive")) {
                    response.status() shouldBeEqualTo HttpStatusCode.OK
                }
            }
        }
    }

    @Test
    fun `isAlive should fail if application did not start`() {
        withMockOAuth2Server {
            val mockOAuth2Server = this
            withTestApplication({
                testApplication(
                    mockOAuth2Server = mockOAuth2Server,
                    environment = testEnvironment(mockOAuth2Server),
                    running = false
                )
            }) {
                with(handleRequest(HttpMethod.Get, "/isAlive")) {
                    response.status() shouldBeEqualTo HttpStatusCode.InternalServerError
                }
            }
        }
    }
}
