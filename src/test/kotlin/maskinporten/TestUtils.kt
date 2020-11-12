package maskinporten

import com.nimbusds.jose.Algorithm
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTParser
import io.ktor.application.Application
import io.ktor.util.KtorExperimentalAPI
import maskinporten.config.Environment
import maskinporten.http.objectMapper
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.text.ParseException
import java.util.UUID
import kotlin.test.fail

@KtorExperimentalAPI
internal fun Application.testApplication(
    mockOAuth2Server: MockOAuth2Server,
    environment: Environment,
    running: Boolean = true,
    initialized: Boolean = false
) {
    val applicationStatus = ApplicationStatus(
        running = running,
        initialized = initialized
    )
    mockOAuth2Server.enqueueCallback(
        DefaultOAuth2TokenCallback(
            issuerId = environment.maskinporten.clientId,
            audience = null,
        )
    )
    setupHttpServer(environment, applicationStatus)
}

internal fun <R> withMockOAuth2Server(
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

internal fun generateRsaKey(keyId: String = UUID.randomUUID().toString(), keySize: Int = 2048): RSAKey =
    KeyPairGenerator.getInstance("RSA").apply { initialize(keySize) }.generateKeyPair()
        .let {
            RSAKey.Builder(it.public as RSAPublicKey)
                .privateKey(it.private as RSAPrivateKey)
                .keyID(keyId)
                .algorithm(Algorithm("RS256"))
                .keyUse(KeyUse.SIGNATURE)
                .build()
        }

internal fun testEnvironment(mockOAuth2Server: MockOAuth2Server) = Environment(
    maskinporten = Environment.Maskinporten(
        wellKnownUrl = mockOAuth2Server.wellKnownUrl(issuerId = "maskinporten").toString(),
        privateJwk = objectMapper.writeValueAsString(generateRsaKey().toJSONObject()),
        scopes = "scope1 scope2",
        clientId = "clientId"
    )
)

internal fun parse(jwt: String) =
    try {
        JWTParser.parse(jwt)
    } catch (p: ParseException) {
        fail("Parsing of jwt fails", p)
    }
