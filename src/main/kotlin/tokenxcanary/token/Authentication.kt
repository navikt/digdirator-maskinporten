package tokenxcanary.token

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import io.ktor.util.KtorExperimentalAPI
import mu.KotlinLogging
import tokenxcanary.common.parseToRsaKey
import tokenxcanary.config.ClientProperties
import tokenxcanary.token.Authentication.Companion.SCOPE
import java.time.Instant
import java.util.Date
import java.util.UUID

private val log = KotlinLogging.logger { }

@KtorExperimentalAPI
class Authentication(
    private val clientProperties: ClientProperties
) {
    private val rsaKey = clientProperties.clientJwk.parseToRsaKey()

    fun assertion(scopes: String?) = assertion(
        clientProperties.clientId,
        clientProperties.metadata.issuer,
        rsaKey,
        scopes
    ).also {
        log.info {
            "JWK with keyID: ${rsaKey.keyID} used to sign " +
                "generated JWT for integration with: ${clientProperties.metadata.issuer}"
        }
    }

    companion object {
        const val SCOPE = "scope"
    }
}

@KtorExperimentalAPI
internal fun assertion(clientId: String, audience: String, rsaKey: RSAKey, scopes: String?): String {
    val now = Date.from(Instant.now())
    val builder = JWTClaimsSet.Builder()

    scopes?.let {
        builder.claim(SCOPE, it)
    }

    return builder
        .issuer(clientId)
        .audience(audience)
        .issueTime(now)
        .expirationTime(Date.from(Instant.now().plusSeconds(120)))
        .jwtID(UUID.randomUUID().toString())
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
