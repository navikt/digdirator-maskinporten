package maskinporten.token

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.http.parametersOf
import io.ktor.util.KtorExperimentalAPI
import maskinporten.config.Environment
import mu.KotlinLogging
import java.time.Instant
import java.util.Date
import java.util.UUID

private val log = KotlinLogging.logger { }
internal const val SCOPES = "scopes"
internal const val GRANT_TYPE = "urn:ietf:params:oauth:grant-type:jwt-bearer"
internal const val PARAMS_GRANT_TYPE = "grant_type"
internal const val PARAMS_CLIENT_ASSERTION = "client_assertion"

class ClientAuthentication(
    private val env: Environment
) {
    private val rsaKey = RSAKey.parse(env.maskinporten.privateJwk)

    @KtorExperimentalAPI
    fun clientAssertion(): String {
        return clientAssertion(
            env.maskinporten.clientId,
            env.maskinporten.metadata.issuer,
            env.maskinporten.scopes,
            rsaKey
        ).also {
            log.info {
                "Keys with keyID: ${rsaKey.keyID}. " +
                    "Generating JWT token for integration with: ${env.maskinporten.metadata.issuer}"
            }
        }
    }
}

suspend fun HttpClient.token(url: String, assertion: String) =
    this.submitForm<AccessTokenResponse>(
        url = url,
        formParameters = parametersOf(
            PARAMS_CLIENT_ASSERTION to listOf(assertion),
            PARAMS_GRANT_TYPE to listOf(GRANT_TYPE)
        )
    )

fun clientAssertion(clientId: String, audience: String, scopes: String, rsaKey: RSAKey): String {
    val now = Date.from(Instant.now())
    return JWTClaimsSet.Builder()
        .issuer(clientId)
        .audience(audience)
        .issueTime(now)
        .expirationTime(Date.from(Instant.now().plusSeconds(60)))
        .jwtID(UUID.randomUUID().toString())
        .claim(SCOPES, scopes)
        .build()
        .sign(rsaKey)
        .serialize()
}

internal fun JWTClaimsSet.sign(rsaKey: RSAKey): SignedJWT =
    SignedJWT(
        JWSHeader.Builder(JWSAlgorithm.RS256)
            .keyID(rsaKey.keyID)
            .type(JOSEObjectType.JWT).build(),
        this
    ).apply {
        sign(RSASSASigner(rsaKey.toPrivateKey()))
    }
