package tokenxcanary.token

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import io.ktor.util.KtorExperimentalAPI
import tokenxcanary.common.parseToRsaKey
import tokenxcanary.config.Environment
import tokenxcanary.http.defaultHttpClient
import tokenxcanary.http.token
import tokenxcanary.token.ClientAuthentication.Companion.SCOPE
import mu.KotlinLogging
import java.time.Instant
import java.util.UUID
import java.util.Date

private val log = KotlinLogging.logger { }

@KtorExperimentalAPI
class ClientAuthentication(
    private val maskinporten: Environment.Maskinporten
) {
    private val rsaKey = maskinporten.clientJwk.parseToRsaKey()

    suspend fun tokenRequest() =
        defaultHttpClient.token(
            maskinporten.metadata.tokenEndpoint,
            clientAssertion()
        )

    fun clientAssertion(): String {
        return clientAssertion(
            maskinporten.clientId,
            maskinporten.metadata.issuer,
            maskinporten.scopes,
            rsaKey
        ).also {
            log.info {
                "JWK with keyID: ${rsaKey.keyID} used to sign " +
                    "generated JWT for integration with: ${maskinporten.metadata.issuer}"
            }
        }
    }

    companion object {
        const val SCOPE = "scope"
        const val GRANT_TYPE = "urn:ietf:params:oauth:grant-type:jwt-bearer"
        const val PARAMS_GRANT_TYPE = "grant_type"
        const val PARAMS_CLIENT_ASSERTION = "assertion"
    }
}

@KtorExperimentalAPI
internal fun clientAssertion(clientId: String, audience: String, scopes: String, rsaKey: RSAKey): String {
    val now = Date.from(Instant.now())
    return JWTClaimsSet.Builder()
        .issuer(clientId)
        .audience(audience)
        .issueTime(now)
        .expirationTime(Date.from(Instant.now().plusSeconds(120)))
        .jwtID(UUID.randomUUID().toString())
        .claim(SCOPE, scopes)
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
